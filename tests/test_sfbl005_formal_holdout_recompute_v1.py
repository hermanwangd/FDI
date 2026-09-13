import hashlib
import importlib.util
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "tools/sfbl005_formal_holdout_recompute_v1.py"

def load_module():
    spec = importlib.util.spec_from_file_location("recompute", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader
    spec.loader.exec_module(module)
    return module

def case(vector_id, operation, given):
    return {"caseOrdinal": 1, "contract": "hand-authored developer fixture", "given": given,
            "namespace": "conformance.invalid", "operation": operation,
            "schemaVersion": "SFBL005-FORMAL-SCORER-CONFORMANCE-CASE-001", "vectorId": vector_id}

def test_hand_authored_oracles_cover_mapping_failures_and_decimal_wilson():
    m = load_module()
    a, b = "a" * 64, "b" * 64
    disposition = case("DEV-D02", "DISPOSITION_EVIDENCE", {
        "goldPairDigest": a, "occurrence": {"facets": {"action": "FAIL", "ambiguity": "UNAMBIGUOUS",
        "assertionRole": "MATCH", "businessCondition": "MATCH", "entity": "MATCH",
        "evidenceSufficiency": "SUFFICIENT", "polarity": "MATCH"}, "occurrenceId": "DEV#0", "pairDigest": a},
        "proofs": [{"occurrenceId": "DEV#0", "pairDigest": a, "proofDigest": b}], "sealedProofDigest": b})
    assert m.evaluate(disposition, "DISPOSITION_EVIDENCE", "NEGATIVE") == {
        "counts": {"duplicateCount": 0, "fn": 1, "fp": 1, "tp": 0}, "polarity": "NEGATIVE",
        "reasonCodes": ["ACTION_MISMATCH"], "result": "VALID", "ruleId": "DISPOSITION_EVIDENCE"}
    wilson = case("DEV-W03", "WILSON_INTERVAL", {"n": 10, "precision": 50,
        "rounding": "HALF_EVEN", "scale": 12, "x": 8, "zDecimal": "1.959963984540054"})
    assert m.evaluate(wilson, "WILSON_INTERVAL", "POSITIVE") == {
        "lower": "0.490162471537", "polarity": "POSITIVE", "result": "VALID",
        "ruleId": "WILSON_INTERVAL", "upper": "0.943317848546"}

def test_hand_authored_cases_exercise_every_operation_family():
    m = load_module(); a, b, c = "a" * 64, "b" * 64, "c" * 64
    candidate = {"fullyQualifiedDeclaringType": "invalid.conformance.Service", "methodName": "run",
        "parameterTypes": ["int", "java.lang.String[]"], "repositorySnapshotSha256": a,
        "returnType": "void", "scenarioId": "DEV-1"}
    assert m.evaluate(case("DEV-C", "CANONICAL_IDENTITY", {"candidate": candidate}))["result"] == "VALID"
    provenance = {"artifactSha256": c, "coverageStrata": ["SYNTHETIC_UNIT"],
        "expectedArtifactSha256": c, "foreignRepositoryReference": False,
        "pairRepositorySnapshotSha256": a, "repositorySnapshotSha256": a, "scenarioId": "DEV-1",
        "sourceProvenanceSha256": c, "testProvenanceSha256": b, "truthDisposition": "SEALED"}
    assert m.evaluate(case("DEV-V", "PROVENANCE_INTEGRITY", provenance))["result"] == "VALID"
    empty = m.evaluate(case("DEV-Z", "EMPTY_AND_ABSTENTION", {
        "goldCount": 2, "proposalOccurrences": [], "scenarioCount": 1}))
    assert empty["precisionReason"] == "NO_PROPOSED_PAIRS" and empty["counts"]["fn"] == 2
    abstain = m.evaluate(case("DEV-A", "EMPTY_AND_ABSTENTION", {
        "abstention": "UNRESOLVED", "goldCount": 2, "proposalOccurrences": [], "scenarioCount": 1}))
    assert abstain["reasonCode"] == "UNRESOLVED" and abstain["scenarioDenominator"] == 1
    chain = m.evaluate(case("DEV-CH", "CHAIN_SCORING", {"chainRequired": True,
        "orderedGoldPairDigests": [a, b], "proposalEdges": [[a, b]],
        "proposalPairDigests": [a, b], "truthEdges": [[a, b]]}))
    assert chain["chainComplete"] is True and chain["counts"]["tp"] == 2
    repository = m.evaluate(case("DEV-R", "REPOSITORY_DECISION", {"repositories": [
        {"fn": 1, "fp": 1, "repositoryId": "synthetic-repository", "tp": 9}]}))
    assert repository["provisionalClassification"] == "PASS"
    parity = m.evaluate(case("DEV-P", "DETERMINISM_AND_PARITY", {"goldenBytes": "x\n",
        "javaRun1Bytes": "x\n", "javaRun2Bytes": "x\n", "pythonRun1Bytes": "x\n",
        "pythonRun2Bytes": "x\n"}))
    assert parity["result"] == "PASS" and parity["byteIdentical"] is True

def test_canonical_variants_preserve_outputs_and_generic_results_are_strings():
    m = load_module(); a = "a" * 64
    base = {"fullyQualifiedDeclaringType": "invalid.conformance.Service", "methodName": "run",
        "parameterTypes": ["int"], "repositorySnapshotSha256": a, "returnType": "void", "scenarioId": "DEV-1"}
    single = m.evaluate(case("DEV-C1", "CANONICAL_IDENTITY", {"candidate": base}))
    assert len(single["canonicalOutputs"]) == 1
    generic = dict(base, parameterTypes=["java.util.List<java.lang.String>"])
    mixed = m.evaluate(case("DEV-C5", "CANONICAL_IDENTITY", {"candidates": [base, generic]}))
    assert mixed["caseResults"] == ["VALID", "INVALID_NON_ERASED_GENERIC"]
    assert len(mixed["canonicalOutputs"]) == 2

def test_repository_metrics_are_fixed_scale_equal_weight_and_null_safe():
    m = load_module()
    value = m.evaluate(case("DEV-R6", "REPOSITORY_DECISION", {"repositories": [
        {"fn": 1, "fp": 1, "repositoryId": "one", "tp": 9},
        {"fn": 0, "fp": 0, "repositoryId": "two", "tp": 1}]}))
    assert value["repositories"][0]["precision"] == "0.900000000000"
    assert value["aggregate"]["macroPrecision"] == "0.950000000000"
    null = m.evaluate(case("DEV-R5", "REPOSITORY_DECISION", {"repositories": [
        {"fn": 0, "fp": 0, "repositoryId": "null", "tp": 0}]}))
    assert null["result"] == "INVALID"
    assert null["repositories"][0]["precisionReason"] == "NO_PROPOSED_PAIRS"
    assert null["repositories"][0]["recallReason"] == "NO_GOLD_PAIRS"
    assert null["aggregate"]["macroRecallReason"] == "NO_GOLD_PAIRS"

def test_empty_recall_is_fixed_scale():
    m = load_module()
    value = m.evaluate(case("DEV-Z1", "EMPTY_AND_ABSTENTION", {
        "goldCount": 2, "proposalOccurrences": [], "scenarioCount": 1}))
    assert value["recall"] == "0.000000000000"

def make_bundle(tmp_path, bad_digest=False):
    inputs = tmp_path / "inputs"; inputs.mkdir()
    item = case("DEV-S01", "OCCURRENCE_SCORING", {"goldPairDigests": ["a" * 64],
        "proposalOccurrences": [{"disposition": "VALID", "occurrenceIndex": 0, "pairDigest": "a" * 64}]})
    raw = (json.dumps(item, sort_keys=True, separators=(",", ":")) + "\n").encode()
    (inputs / "DEV-S01.json").write_bytes(raw)
    digest = "0" * 64 if bad_digest else hashlib.sha256(raw).hexdigest()
    value = {"schemaVersion": "SFBL005-FORMAL-SCORER-VECTOR-MANIFEST-001", "vectors": [{
        "coverage": [{"polarity": "POSITIVE", "ruleId": "OCCURRENCE_SCORING"}],
        "expected": {"path": "unused.json", "sha256": "f" * 64}, "id": "DEV-S01",
        "input": {"path": "DEV-S01.json", "sha256": digest}}]}
    manifest = tmp_path / "manifest.json"
    manifest.write_bytes((json.dumps(value, sort_keys=True, separators=(",", ":")) + "\n").encode())
    return manifest, inputs

def test_cli_uses_hand_authored_inputs_and_emits_canonical_declared_order(tmp_path):
    manifest, inputs = make_bundle(tmp_path); output = tmp_path / "oracle.json"
    proc = subprocess.run([sys.executable, str(SCRIPT), "--manifest", str(manifest),
        "--inputs-root", str(inputs), "--output", str(output)], text=True, capture_output=True)
    assert proc.returncode == 0, proc.stderr
    raw = output.read_bytes(); decoded = json.loads(raw)
    assert set(decoded) == {"schemaVersion", "manifestSha256", "results"}
    assert decoded["manifestSha256"] == hashlib.sha256(manifest.read_bytes()).hexdigest()
    assert decoded["results"][0]["oracle"]["counts"] == {"duplicateCount": 0, "fn": 0, "fp": 0, "tp": 1}
    assert raw == (json.dumps(decoded, sort_keys=True, separators=(",", ":")) + "\n").encode()

def test_cli_fails_closed_on_digest_mismatch_collision_and_symlink(tmp_path):
    manifest, inputs = make_bundle(tmp_path, bad_digest=True); output = tmp_path / "out.json"
    cmd = [sys.executable, str(SCRIPT), "--manifest", str(manifest), "--inputs-root", str(inputs), "--output", str(output)]
    proc = subprocess.run(cmd, text=True, capture_output=True)
    assert proc.returncode != 0 and "input digest mismatch" in proc.stderr and not output.exists()
    output.write_text("preserve")
    assert subprocess.run(cmd, text=True, capture_output=True).returncode != 0 and output.read_text() == "preserve"
    output.unlink(); target = tmp_path / "target"; target.write_text("preserve"); output.symlink_to(target)
    assert subprocess.run(cmd, text=True, capture_output=True).returncode != 0 and target.read_text() == "preserve"

def test_cli_rejects_duplicate_json_keys_unknown_manifest_schema_and_symlink_manifest(tmp_path):
    manifest, inputs = make_bundle(tmp_path)
    raw = manifest.read_text().replace('"schemaVersion":', '"unknown":1,"schemaVersion":', 1)
    manifest.write_text(raw)
    output = tmp_path / "unknown.json"
    cmd = [sys.executable, str(SCRIPT), "--manifest", str(manifest), "--inputs-root", str(inputs), "--output", str(output)]
    assert subprocess.run(cmd, text=True, capture_output=True).returncode != 0
    manifest.write_text('{"schemaVersion":"x","schemaVersion":"y","vectors":[]}\n')
    assert subprocess.run(cmd, text=True, capture_output=True).returncode != 0
    real = tmp_path / "real-manifest"; real.write_text('{"schemaVersion":"SFBL005-FORMAL-SCORER-VECTOR-MANIFEST-001","vectors":[]}\n')
    manifest.unlink(); manifest.symlink_to(real)
    assert subprocess.run(cmd, text=True, capture_output=True).returncode != 0

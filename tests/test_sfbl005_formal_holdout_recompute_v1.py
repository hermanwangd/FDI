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
        "counts": {"duplicateCount": 0, "fn": 1, "fp": 1, "tp": 0}, "polarity": ["NEGATIVE"],
        "reasonCodes": ["ACTION_MISMATCH"], "result": "VALID", "ruleId": "DISPOSITION_EVIDENCE"}
    wilson = case("DEV-W03", "WILSON_INTERVAL", {"n": 10, "precision": 50,
        "rounding": "HALF_EVEN", "scale": 12, "x": 8, "zDecimal": "1.959963984540054"})
    assert m.evaluate(wilson, "WILSON_INTERVAL", "POSITIVE") == {
        "lower": "0.490162471537", "polarity": ["POSITIVE"], "result": "VALID",
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

def test_repository_macro_uses_raw_ratios_before_final_quantization():
    m = load_module()
    value = m.evaluate(case("DEV-R-ROUNDING", "REPOSITORY_DECISION", {"repositories": [
        {"fn": 1, "fp": 0, "repositoryId": "two-thirds", "tp": 2},
        {"fn": 1, "fp": 0, "repositoryId": "nine-tenths", "tp": 9}]}))
    assert value["repositories"][0]["recall"] == "0.666666666667"
    assert value["repositories"][1]["recall"] == "0.900000000000"
    assert value["aggregate"]["macroRecall"] == "0.783333333333"

def test_empty_recall_is_fixed_scale():
    m = load_module()
    value = m.evaluate(case("DEV-Z1", "EMPTY_AND_ABSTENTION", {
        "goldCount": 2, "proposalOccurrences": [], "scenarioCount": 1}))
    assert value["recall"] == "0.000000000000"

def make_bundle(tmp_path, bad_digest=False):
    inputs = tmp_path / "inputs"; inputs.mkdir(parents=True)
    item = case("DEV-S01", "OCCURRENCE_SCORING", {"goldPairDigests": ["a" * 64],
        "proposalOccurrences": [{"disposition": "VALID", "occurrenceIndex": 0, "pairDigest": "a" * 64}]})
    raw = (json.dumps(item, sort_keys=True, separators=(",", ":")) + "\n").encode()
    (inputs / "DEV-S01.json").write_bytes(raw)
    digest = "0" * 64 if bad_digest else hashlib.sha256(raw).hexdigest()
    value = {"schemaVersion": "SFBL005-FORMAL-SCORER-VECTOR-MANIFEST-001", "vectors": [{
        "coverage": [{"polarity": "POSITIVE", "ruleId": "OCCURRENCE_SCORING"}],
        "expected": {"path": "DEV-S01.json", "sha256": "f" * 64}, "id": "DEV-S01",
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
    assert decoded["results"][0]["oracle"]["polarity"] == ["POSITIVE"]
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
    real = tmp_path / "real-manifest"
    real.write_text('{"schemaVersion":"SFBL005-FORMAL-SCORER-VECTOR-MANIFEST-001","vectors":[]}\n')
    manifest.unlink(); manifest.symlink_to(real)
    assert subprocess.run(cmd, text=True, capture_output=True).returncode != 0

def test_malformed_operation_inputs_fail_closed():
    m = load_module(); a = "a" * 64
    generic = {"fullyQualifiedDeclaringType": "invalid.conformance.Service", "methodName": "run",
        "parameterTypes": ["java.util.List<java.lang.String>"], "repositorySnapshotSha256": a,
        "returnType": "void", "scenarioId": "DEV"}
    assert m.evaluate(case("DEV-C", "CANONICAL_IDENTITY", {"candidate": generic}))["result"] == "INVALID"
    bad_abstention = m.evaluate(case("DEV-A", "EMPTY_AND_ABSTENTION", {
        "abstention": "ARBITRARY", "goldCount": 1, "proposalOccurrences": [], "scenarioCount": 1}))
    assert bad_abstention == {"reasonCodes": ["MALFORMED_GIVEN"], "result": "INVALID",
                              "ruleId": "EMPTY_AND_ABSTENTION"}
    for repositories in ([], [{"fn": 0, "fp": 0, "repositoryId": "x", "tp": True}],
        [{"fn": 0, "fp": 0, "repositoryId": "x", "tp": 1},
         {"fn": 0, "fp": 0, "repositoryId": "x", "tp": 1}]):
        value = m.evaluate(case("DEV-R", "REPOSITORY_DECISION", {"repositories": repositories}))
        assert value["result"] == "INVALID" and value["reasonCodes"] == ["MALFORMED_GIVEN"]

def test_cli_rejects_bad_coverage_and_symlink_output_ancestor(tmp_path):
    manifest, inputs = make_bundle(tmp_path)
    value = json.loads(manifest.read_text())
    value["vectors"][0]["coverage"][0]["polarity"] = "MAYBE"
    manifest.write_text(json.dumps(value))
    output = tmp_path / "bad.json"
    cmd = [sys.executable, str(SCRIPT), "--manifest", str(manifest), "--inputs-root", str(inputs), "--output", str(output)]
    assert subprocess.run(cmd, text=True, capture_output=True).returncode != 0
    value["vectors"][0]["coverage"][0]["polarity"] = "POSITIVE"
    manifest.write_text(json.dumps(value))
    real_parent = tmp_path / "real"; real_parent.mkdir(); linked_parent = tmp_path / "linked"; linked_parent.symlink_to(real_parent)
    cmd[-1] = str(linked_parent / "out.json")
    assert subprocess.run(cmd, text=True, capture_output=True).returncode != 0
    assert not (real_parent / "out.json").exists()

def test_wilson_configuration_is_exact_and_nan_fails_stably():
    m = load_module()
    valid = {"n": 10, "precision": 50, "rounding": "HALF_EVEN", "scale": 12,
             "x": 8, "zDecimal": "1.959963984540054"}
    for key, bad in (("precision", 49), ("scale", 11), ("zDecimal", "NaN"),
                     ("zDecimal", "-1"), ("zDecimal", "1.96")):
        malformed = dict(valid, **{key: bad})
        value = m.evaluate(case("DEV-W", "WILSON_INTERVAL", malformed))
        assert value == {"reasonCodes": ["MALFORMED_GIVEN"], "result": "INVALID",
                         "ruleId": "WILSON_INTERVAL"}
    result = m.evaluate(case("DEV-W", "WILSON_INTERVAL", valid))
    assert result["lower"] <= result["upper"] and "NaN" not in json.dumps(result)

def test_provenance_metadata_types_fail_stably():
    m = load_module(); a = "a" * 64
    valid = {"artifactSha256": a, "coverageStrata": ["SYNTHETIC_UNIT"],
        "expectedArtifactSha256": a, "foreignRepositoryReference": False,
        "pairRepositorySnapshotSha256": a, "repositorySnapshotSha256": a,
        "scenarioId": "DEV", "sourceProvenanceSha256": a,
        "testProvenanceSha256": a, "truthDisposition": "SEALED"}
    for update in ({"scenarioId": ""}, {"scenarioIds": [1]}, {"truthDisposition": "ARBITRARY"},
                   {"foreignRepositoryReference": "false"}):
        malformed = dict(valid, **update)
        value = m.evaluate(case("DEV-V", "PROVENANCE_INTEGRITY", malformed))
        assert value["result"] == "INVALID" and value["reasonCodes"] == ["MALFORMED_GIVEN"]

def test_cli_rejects_symlink_inputs_root(tmp_path):
    manifest, inputs = make_bundle(tmp_path)
    alias = tmp_path / "inputs-alias"; alias.symlink_to(inputs)
    output = tmp_path / "out.json"
    cmd = [sys.executable, str(SCRIPT), "--manifest", str(manifest),
           "--inputs-root", str(alias), "--output", str(output)]
    proc = subprocess.run(cmd, text=True, capture_output=True)
    assert proc.returncode != 0 and not output.exists()

def test_canonical_source_path_must_be_safe_relative_string():
    m = load_module(); a = "a" * 64
    base = {"fullyQualifiedDeclaringType": "invalid.conformance.Service", "methodName": "run",
        "parameterTypes": [], "repositorySnapshotSha256": a, "returnType": "void", "scenarioId": "DEV"}
    for unsafe in ("/absolute/A.java", "../escape.java", 7):
        value = m.evaluate(case("DEV-C", "CANONICAL_IDENTITY", {"candidate": dict(base, sourcePath=unsafe)}))
        assert value["result"] == "INVALID" and value["reasonCodes"] == ["MALFORMED_GIVEN"]

def test_cli_rejects_manifest_identity_and_case_metadata_mismatch(tmp_path):
    manifest, inputs = make_bundle(tmp_path)
    value = json.loads(manifest.read_text())
    value["vectors"][0]["expected"]["path"] = "../escape.json"
    manifest.write_text(json.dumps(value))
    output = tmp_path / "out.json"
    cmd = [sys.executable, str(SCRIPT), "--manifest", str(manifest), "--inputs-root", str(inputs), "--output", str(output)]
    assert subprocess.run(cmd, text=True, capture_output=True).returncode != 0

    manifest, inputs = make_bundle(tmp_path / "second")
    item_path = inputs / "DEV-S01.json"; item = json.loads(item_path.read_text())
    item["caseOrdinal"] = 2; item["namespace"] = "wrong.invalid"
    raw = (json.dumps(item, sort_keys=True, separators=(",", ":")) + "\n").encode(); item_path.write_bytes(raw)
    value = json.loads(manifest.read_text()); value["vectors"][0]["input"]["sha256"] = hashlib.sha256(raw).hexdigest()
    manifest.write_text(json.dumps(value)); output = manifest.parent / "out.json"
    cmd = [sys.executable, str(SCRIPT), "--manifest", str(manifest), "--inputs-root", str(inputs), "--output", str(output)]
    assert subprocess.run(cmd, text=True, capture_output=True).returncode != 0

def test_cli_rejects_duplicate_vector_ids_and_input_paths(tmp_path):
    manifest, inputs = make_bundle(tmp_path)
    value = json.loads(manifest.read_text()); value["vectors"].append(dict(value["vectors"][0]))
    manifest.write_text(json.dumps(value)); output = tmp_path / "out.json"
    cmd = [sys.executable, str(SCRIPT), "--manifest", str(manifest), "--inputs-root", str(inputs), "--output", str(output)]
    assert subprocess.run(cmd, text=True, capture_output=True).returncode != 0

def test_canonical_candidates_requires_exactly_two_items():
    m = load_module(); a = "a" * 64
    candidate = {"fullyQualifiedDeclaringType": "invalid.conformance.Service", "methodName": "run",
        "parameterTypes": [], "repositorySnapshotSha256": a, "returnType": "void", "scenarioId": "DEV"}
    for candidates in ([candidate], [candidate, candidate, candidate]):
        value = m.evaluate(case("DEV-C", "CANONICAL_IDENTITY", {"candidates": candidates}))
        assert value["result"] == "INVALID" and value["reasonCodes"] == ["MALFORMED_GIVEN"]
    assert m.evaluate(case("DEV-C", "CANONICAL_IDENTITY", {"candidates": [candidate, candidate]}))["result"] == "VALID"

def test_chain_counts_duplicate_pairs_and_requires_exact_proposal_edges():
    m = load_module(); a, b, c = "a" * 64, "b" * 64, "c" * 64
    duplicate = m.evaluate(case("DEV-CH-DUP", "CHAIN_SCORING", {"chainRequired": True,
        "orderedGoldPairDigests": [a, b], "proposalEdges": [[a, b]],
        "proposalPairDigests": [a, a, b], "truthEdges": [[a, b]]}))
    assert duplicate["counts"] == {"duplicateCount": 1, "fn": 0, "fp": 1, "tp": 2}
    assert duplicate["chainComplete"] is True
    extra_edge = m.evaluate(case("DEV-CH-EDGE", "CHAIN_SCORING", {"chainRequired": True,
        "orderedGoldPairDigests": [a, b, c], "proposalEdges": [[a, b], [b, c], [a, c]],
        "proposalPairDigests": [a, b, c], "truthEdges": [[a, b], [b, c]]}))
    assert extra_edge["counts"] == {"duplicateCount": 0, "fn": 0, "fp": 0, "tp": 3}
    assert extra_edge["chainComplete"] is False

def test_chain_rejects_duplicate_ordered_gold_digests_before_counting():
    m = load_module(); a, b = "a" * 64, "b" * 64
    value = m.evaluate(case("DEV-CH-GOLD-DUP", "CHAIN_SCORING", {"chainRequired": True,
        "orderedGoldPairDigests": [a, a, b], "proposalEdges": [[a, a], [a, b]],
        "proposalPairDigests": [a, b], "truthEdges": [[a, a], [a, b]]}))
    assert value == {"reasonCodes": ["MALFORMED_GIVEN"], "result": "INVALID",
                     "ruleId": "CHAIN_SCORING"}

def test_cli_creates_output_as_regular_file_in_verified_parent(tmp_path):
    manifest, inputs = make_bundle(tmp_path); parent = tmp_path / "execution"; parent.mkdir()
    before = parent.stat(); output = parent / "oracle.json"
    proc = subprocess.run([sys.executable, str(SCRIPT), "--manifest", str(manifest),
        "--inputs-root", str(inputs), "--output", str(output)], text=True, capture_output=True)
    assert proc.returncode == 0, proc.stderr
    after = parent.stat(); created = output.lstat()
    assert (before.st_dev, before.st_ino) == (after.st_dev, after.st_ino)
    assert created.st_nlink == 1 and output.is_file() and not output.is_symlink()


def test_provenance_unknown_fields_return_one_schema_reason():
    m = load_module()
    given = {"artifactSha256": "d" * 64, "expectedArtifactSha256": "d" * 64,
             "coverageStrata": ["UNIT"], "foreignRepositoryReference": False,
             "pairRepositorySnapshotSha256": "a" * 64, "repositorySnapshotSha256": "a" * 64,
             "scenarioId": "DEV", "sourceProvenanceSha256": "c" * 64,
             "testProvenanceSha256": "b" * 64, "truthDisposition": "SEALED"}
    for extra in ({"unknownField": "x"}, {"unexpected": None, "another": {"nested": 1}}):
        result = m.evaluate(case("DEV-UNKNOWN", "PROVENANCE_INTEGRITY", dict(given, **extra)))
        assert result["result"] == "INVALID"
        assert result["reasonCodes"] == ["MALFORMED_SCHEMA"]
        mixed = m.evaluate(case("DEV-MIXED", "PROVENANCE_INTEGRITY", dict(given, **extra, pairRepositorySnapshotSha256="e" * 64)))
        assert mixed["reasonCodes"] == ["MALFORMED_SCHEMA", "WRONG_SNAPSHOT"]


def test_malformed_given_keeps_polarity_array():
    m = load_module()
    result = m.evaluate(case("DEV-MALFORMED", "WILSON_INTERVAL", {}), "WILSON_INTERVAL", "NEGATIVE")
    assert result == {"reasonCodes": ["MALFORMED_GIVEN"], "result": "INVALID",
                      "ruleId": "WILSON_INTERVAL", "polarity": ["NEGATIVE"]}


def test_micro_null_reasons_follow_aggregate_denominators():
    m = load_module()
    for tp, fp, fn, expected in [
        (0, 0, 1, {"microPrecisionReason": "NO_PROPOSED_PAIRS"}),
        (0, 1, 0, {"microRecallReason": "NO_GOLD_PAIRS"}),
        (0, 0, 0, {"microPrecisionReason": "NO_PROPOSED_PAIRS", "microRecallReason": "NO_GOLD_PAIRS"}),
        (1, 0, 0, {}),
    ]:
        aggregate = m.evaluate(case("DEV-MICRO-NULL", "REPOSITORY_DECISION", {
            "repositories": [{"repositoryId": "one", "tp": tp, "fp": fp, "fn": fn}]}))["aggregate"]
        assert {k: v for k, v in aggregate.items() if k.startswith("micro") and k.endswith("Reason")} == expected
    mixed = m.evaluate(case("DEV-MIXED-NULL", "REPOSITORY_DECISION", {"repositories": [
        {"repositoryId": "empty", "tp": 0, "fp": 0, "fn": 0},
        {"repositoryId": "defined", "tp": 1, "fp": 0, "fn": 0}]}))["aggregate"]
    assert mixed["macroPrecisionReason"] == "NO_PROPOSED_PAIRS"
    assert mixed["macroRecallReason"] == "NO_GOLD_PAIRS"
    assert "microPrecisionReason" not in mixed and "microRecallReason" not in mixed

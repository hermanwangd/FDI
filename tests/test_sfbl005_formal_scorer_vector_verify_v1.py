import hashlib
import json
import subprocess
import sys
from pathlib import Path

import pytest


TOOL = Path(__file__).parents[1] / "tools" / "sfbl005_formal_scorer_vector_verify_v1.py"


def canonical(value):
    return (json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False) + "\n").encode()


def sha(data):
    return hashlib.sha256(data).hexdigest()


def arrange(tmp_path):
    tmp_path.mkdir(parents=True, exist_ok=True)
    inputs = tmp_path / "inputs"
    expected = tmp_path / "expected"
    inputs.mkdir()
    expected.mkdir()
    specimens = {
        "P": (
            {"schemaVersion": "SFBL005-FORMAL-SCORER-CONFORMANCE-CASE-001", "vectorId": "P", "caseOrdinal": 1, "namespace": "conformance.invalid", "operation": "OCCURRENCE_SCORING", "contract": "synthetic positive", "given": {"goldPairDigests": [], "proposalOccurrences": []}},
            {"schemaVersion": "SFBL005-FORMAL-SCORER-CONFORMANCE-EXPECTED-001", "vectorId": "P", "assertionMode": "EXACT_JSON_BYTES", "oracle": {"ruleId": "OCCURRENCE_SCORING", "polarity": ["POSITIVE"], "result": "VALID", "counts": {"tp": 0, "fp": 0, "fn": 0, "duplicateCount": 0}}},
        ),
        "N": (
            {"schemaVersion": "SFBL005-FORMAL-SCORER-CONFORMANCE-CASE-001", "vectorId": "N", "caseOrdinal": 2, "namespace": "conformance.invalid", "operation": "OCCURRENCE_SCORING", "contract": "synthetic negative", "given": {"goldPairDigests": ["a" * 64], "proposalOccurrences": []}},
            {"schemaVersion": "SFBL005-FORMAL-SCORER-CONFORMANCE-EXPECTED-001", "vectorId": "N", "assertionMode": "EXACT_JSON_BYTES", "oracle": {"ruleId": "OCCURRENCE_SCORING", "polarity": ["NEGATIVE"], "result": "VALID", "counts": {"tp": 0, "fp": 0, "fn": 1, "duplicateCount": 0}}},
        ),
    }
    vectors = []
    for vector_id, (input_value, expected_value) in specimens.items():
        input_bytes = canonical(input_value)
        expected_bytes = canonical(expected_value)
        (inputs / f"{vector_id}.json").write_bytes(input_bytes)
        (expected / f"{vector_id}.json").write_bytes(expected_bytes)
        vectors.append(
            {
                "id": vector_id,
                "input": {"path": f"{vector_id}.json", "sha256": sha(input_bytes)},
                "expected": {"path": f"{vector_id}.json", "sha256": sha(expected_bytes)},
                "coverage": [{"ruleId": "OCCURRENCE_SCORING", "polarity": "POSITIVE" if vector_id == "P" else "NEGATIVE"}],
            }
        )
    manifest = {"schemaVersion": "SFBL005-FORMAL-SCORER-VECTOR-MANIFEST-001", "vectors": vectors}
    catalog = {"schemaVersion": "SFBL005-FORMAL-SCORER-VECTOR-CATALOG-001", "vectorIds": ["N", "P"], "ruleIds": ["OCCURRENCE_SCORING"]}
    manifest_path = tmp_path / "vector-manifest.json"
    catalog_path = tmp_path / "catalog.json"
    manifest_path.write_bytes(canonical(manifest))
    catalog_path.write_bytes(canonical(catalog))
    return manifest_path, catalog_path, inputs, expected, tmp_path / "vector-review.json"


def run(paths):
    manifest, catalog, inputs, expected, review = paths
    return subprocess.run(
        [sys.executable, str(TOOL), "--manifest", str(manifest), "--catalog", str(catalog),
         "--inputs-root", str(inputs), "--expected-root", str(expected), "--review", str(review)],
        text=True, capture_output=True,
    )


def test_writes_deterministic_canonical_review_for_complete_synthetic_catalog(tmp_path):
    paths = arrange(tmp_path)
    result = run(paths)
    assert result.returncode == 0, result.stderr
    receipt = paths[-1].read_bytes()
    assert receipt.endswith(b"\n") and not receipt.endswith(b"\n\n")
    parsed = json.loads(receipt)
    assert receipt == canonical(parsed)
    assert parsed["result"] == "PASS"
    assert parsed["vectorIds"] == ["N", "P"]
    assert parsed["coverage"] == {"OCCURRENCE_SCORING": ["NEGATIVE", "POSITIVE"]}


@pytest.mark.parametrize(
    "mutation,error",
    [
        (lambda m: m["vectors"].pop(), "catalog vector IDs"),
        (lambda m: m["vectors"].append(dict(m["vectors"][0])), "duplicate vector id"),
        (lambda m: m["vectors"][1].update({"input": dict(m["vectors"][0]["input"])}), "duplicate artifact path"),
        (lambda m: m["vectors"][0]["input"].update(path="../outside.json"), "outside inputs root"),
        (lambda m: m["vectors"][0]["expected"].update(sha256="0" * 64), "sha256 mismatch"),
        (lambda m: m["vectors"][1].update(coverage=[]), "positive and negative"),
    ],
)
def test_rejects_manifest_integrity_and_coverage_failures(tmp_path, mutation, error):
    paths = arrange(tmp_path)
    manifest = json.loads(paths[0].read_text())
    mutation(manifest)
    paths[0].write_bytes(canonical(manifest))
    result = run(paths)
    assert result.returncode == 2
    assert error in result.stderr
    assert not paths[-1].exists()


@pytest.mark.parametrize("forbidden", [
    "https://example.invalid/repository", "RealWorld", "petclinic",
    "0123456789abcdef0123456789abcdef01234567", "secret-holdout-scenario",
])
def test_rejects_real_or_holdout_identity_content(tmp_path, forbidden):
    paths = arrange(tmp_path)
    input_path = paths[2] / "P.json"
    value = json.loads(input_path.read_text())
    value["leak"] = forbidden
    data = canonical(value)
    input_path.write_bytes(data)
    manifest = json.loads(paths[0].read_text())
    manifest["vectors"][0]["input"]["sha256"] = sha(data)
    paths[0].write_bytes(canonical(manifest))
    result = run(paths)
    assert result.returncode == 2
    assert "forbidden identity content" in result.stderr


def test_requires_absent_safe_review_path(tmp_path):
    paths = arrange(tmp_path)
    paths[-1].write_text("existing evidence\n")
    result = run(paths)
    assert result.returncode == 2
    assert "review path must be absent" in result.stderr
    assert paths[-1].read_text() == "existing evidence\n"


def test_rejects_review_outside_manifest_directory(tmp_path):
    paths = list(arrange(tmp_path / "bundle"))
    paths[-1] = tmp_path / "escaped-review.json"
    result = run(paths)
    assert result.returncode == 2
    assert "review path must be inside manifest directory" in result.stderr


@pytest.mark.parametrize("document", ["catalog", "manifest"])
def test_rejects_forbidden_identity_in_control_documents(tmp_path, document):
    paths = arrange(tmp_path)
    index = 1 if document == "catalog" else 0
    value = json.loads(paths[index].read_text())
    value["leakedRealWorldIdentity"] = "secret-holdout-scenario"
    paths[index].write_bytes(canonical(value))
    result = run(paths)
    assert result.returncode == 2
    assert f"forbidden identity content in {document}" in result.stderr


@pytest.mark.parametrize(
    "artifact,mutation,error",
    [
        ("input", lambda v: v.update(vectorId="N"), "input vectorId must equal manifest id"),
        ("expected", lambda v: v.update(vectorId="N"), "expected vectorId must equal manifest id"),
        ("expected", lambda v: v["oracle"].update(ruleId="R2"), "input operation must equal expected ruleId"),
        ("expected", lambda v: v["oracle"].update(polarity=["NEGATIVE"]), "expected polarity must exactly equal manifest coverage"),
        ("expected", lambda v: v.pop("oracle"), "expected fields must be exactly"),
        ("expected", lambda v: v.update(oracle={}), "expected oracle must be a nonempty object"),
    ],
)
def test_binds_exact_artifact_schema_and_oracle_to_manifest(tmp_path, artifact, mutation, error):
    paths = arrange(tmp_path)
    root = paths[2] if artifact == "input" else paths[3]
    artifact_path = root / "P.json"
    value = json.loads(artifact_path.read_text())
    mutation(value)
    data = canonical(value)
    artifact_path.write_bytes(data)
    manifest = json.loads(paths[0].read_text())
    manifest["vectors"][0][artifact]["sha256"] = sha(data)
    paths[0].write_bytes(canonical(manifest))
    result = run(paths)
    assert result.returncode == 2
    assert error in result.stderr


def test_rejects_cross_rule_coverage_labels_not_supported_by_vector_oracle(tmp_path):
    paths = arrange(tmp_path)
    catalog = json.loads(paths[1].read_text())
    catalog["ruleIds"].append("R2")
    paths[1].write_bytes(canonical(catalog))
    manifest = json.loads(paths[0].read_text())
    manifest["vectors"][0]["coverage"].extend(
        [
            {"ruleId": "R2", "polarity": "POSITIVE"},
            {"ruleId": "R2", "polarity": "NEGATIVE"},
        ]
    )
    paths[0].write_bytes(canonical(manifest))
    result = run(paths)
    assert result.returncode == 2
    assert "coverage may only reference input operation" in result.stderr
    assert not paths[-1].exists()


def arrange_semantic(tmp_path, operation, given, oracle):
    paths = arrange(tmp_path)
    catalog = json.loads(paths[1].read_text())
    catalog["ruleIds"] = [operation]
    paths[1].write_bytes(canonical(catalog))
    manifest = json.loads(paths[0].read_text())
    for index, vector_id in enumerate(("P", "N")):
        polarity = "POSITIVE" if vector_id == "P" else "NEGATIVE"
        input_value = {
            "schemaVersion": "SFBL005-FORMAL-SCORER-CONFORMANCE-CASE-001", "vectorId": vector_id,
            "caseOrdinal": index + 1, "namespace": "conformance.invalid", "operation": operation,
            "contract": "synthetic semantic oracle", "given": given(vector_id),
        }
        oracle_value = oracle(vector_id)
        oracle_value.update(ruleId=operation, polarity=[polarity])
        expected_value = {
            "schemaVersion": "SFBL005-FORMAL-SCORER-CONFORMANCE-EXPECTED-001", "vectorId": vector_id,
            "assertionMode": "EXACT_JSON_BYTES", "oracle": oracle_value,
        }
        for kind, root, value in (("input", paths[2], input_value), ("expected", paths[3], expected_value)):
            data = canonical(value)
            (root / f"{vector_id}.json").write_bytes(data)
            manifest["vectors"][index][kind]["sha256"] = sha(data)
        manifest["vectors"][index]["coverage"] = [{"ruleId": operation, "polarity": polarity}]
    paths[0].write_bytes(canonical(manifest))
    return paths


def mutate_expected(paths, mutation, vector_id="P"):
    path = paths[3] / f"{vector_id}.json"
    value = json.loads(path.read_text())
    mutation(value["oracle"])
    data = canonical(value)
    path.write_bytes(data)
    manifest = json.loads(paths[0].read_text())
    index = 0 if vector_id == "P" else 1
    manifest["vectors"][index]["expected"]["sha256"] = sha(data)
    paths[0].write_bytes(canonical(manifest))


def assert_semantic_rejection(paths, error):
    result = run(paths)
    assert result.returncode == 2
    assert error in result.stderr


def test_rejects_canonical_digest_not_derived_from_canonical_identity(tmp_path):
    pair = {"repositorySnapshotSha256": "a" * 64, "scenarioId": "SYN-1", "fullyQualifiedDeclaringType": "invalid.conformance.Service", "methodName": "run", "parameterTypes": [], "returnType": "void"}
    paths = arrange_semantic(tmp_path, "CANONICAL_IDENTITY", lambda _: {"candidates": [pair]}, lambda _: {"result": "VALID", "canonicalOutputs": [{"result": "VALID", "canonicalBytes": json.dumps(pair, sort_keys=True, separators=(",", ":")), "pairDigest": "0" * 64}]})
    assert_semantic_rejection(paths, "canonical pairDigest mismatch")


def test_rejects_negative_or_nonconserving_counts(tmp_path):
    paths = arrange_semantic(tmp_path, "OCCURRENCE_SCORING", lambda _: {"goldPairDigests": ["a" * 64], "proposalOccurrences": []}, lambda _: {"result": "VALID", "counts": {"tp": -1, "fp": 0, "fn": 1, "duplicateCount": 0}})
    assert_semantic_rejection(paths, "counts must be nonnegative")


def test_rejects_invalid_chain_without_exact_reason_and_null_chain_complete(tmp_path):
    paths = arrange_semantic(tmp_path, "CHAIN_SCORING", lambda _: {"chainRequired": True, "orderedGoldPairDigests": ["a" * 64], "truthEdges": [], "proposalPairDigests": [], "proposalEdges": []}, lambda _: {"result": "INVALID", "chainComplete": True, "counts": {"tp": 0, "fp": 0, "fn": 1, "duplicateCount": 0}})
    assert_semantic_rejection(paths, "invalid chain oracle")


def test_rejects_repository_metric_not_recomputed_from_raw_counts(tmp_path):
    given = lambda _: {"repositories": [{"repositoryId": "synthetic-repository-1", "tp": 9, "fp": 1, "fn": 1}]}
    oracle = lambda _: {"result": "VALID", "provisionalClassification": "PASS", "repositories": [{"repositoryId": "synthetic-repository-1", "tp": 9, "fp": 1, "fn": 1, "precision": "0.100000000000", "recall": "0.900000000000", "strictPass": True}], "aggregate": {"microPrecision": "0.900000000000", "microRecall": "0.900000000000", "macroPrecision": "0.900000000000", "macroRecall": "0.900000000000"}}
    paths = arrange_semantic(tmp_path, "REPOSITORY_DECISION", given, oracle)
    assert_semantic_rejection(paths, "repository metric mismatch")


def test_rejects_nonfixed_decimal_encoding(tmp_path):
    given = lambda _: {"repositories": [{"repositoryId": "synthetic-repository-1", "tp": 0, "fp": 1, "fn": 2}]}
    oracle = lambda _: {"result": "VALID", "provisionalClassification": "REVISE", "repositories": [{"repositoryId": "synthetic-repository-1", "tp": 0, "fp": 1, "fn": 2, "precision": "0E-12", "recall": "0E-12", "strictPass": False}], "aggregate": {"microPrecision": "0E-12", "microRecall": "0E-12", "macroPrecision": "0E-12", "macroRecall": "0E-12"}}
    paths = arrange_semantic(tmp_path, "REPOSITORY_DECISION", given, oracle)
    assert_semantic_rejection(paths, "fixed 12-place decimal")


def test_rejects_incorrect_wilson_oracle(tmp_path):
    given = lambda _: {"x": 0, "n": 10, "zDecimal": "1.959963984540054", "precision": 50, "rounding": "HALF_EVEN", "scale": 12}
    oracle = lambda _: {"result": "VALID", "lower": "0.000000000000", "upper": "0.999999999999"}
    paths = arrange_semantic(tmp_path, "WILSON_INTERVAL", given, oracle)
    assert_semantic_rejection(paths, "Wilson interval mismatch")


def test_rejects_parity_claim_not_derived_from_bytes(tmp_path):
    given = lambda _: {"goldenBytes": "alpha", "javaRun1Bytes": "alpha", "javaRun2Bytes": "beta", "pythonRun1Bytes": "alpha", "pythonRun2Bytes": "alpha"}
    oracle = lambda _: {"result": "PASS", "byteIdentical": True, "goldenSha256": sha(b"alpha"), "runSha256": [sha(b"alpha")] * 4}
    paths = arrange_semantic(tmp_path, "DETERMINISM_AND_PARITY", given, oracle)
    assert_semantic_rejection(paths, "parity oracle mismatch")


def test_rejects_arbitrary_disposition_reason_not_derived_from_given(tmp_path):
    def given(vector_id):
        facets = {"entity": "MATCH" if vector_id == "P" else "FAIL", "action": "MATCH", "assertionRole": "MATCH", "polarity": "MATCH", "businessCondition": "MATCH", "ambiguity": "UNAMBIGUOUS", "evidenceSufficiency": "SUFFICIENT"}
        return {"goldPairDigest": "a" * 64, "occurrence": {"occurrenceId": "SYN-1#0", "pairDigest": "a" * 64, "facets": facets}, "proofs": [{"occurrenceId": "SYN-1#0", "pairDigest": "a" * 64, "proofDigest": "b" * 64}], "sealedProofDigest": "b" * 64}
    def oracle(vector_id):
        mismatch = vector_id == "N"
        return {"result": "VALID", "reasonCodes": ["ENTITY_MISMATCH"] if mismatch else [], "counts": {"tp": 0 if mismatch else 1, "fp": 1 if mismatch else 0, "fn": 1 if mismatch else 0, "duplicateCount": 0}}
    paths = arrange_semantic(tmp_path, "DISPOSITION_EVIDENCE", given, oracle)
    mutate_expected(paths, lambda value: value.update(reasonCodes=["ARBITRARY_WRONG_REASON"]), "N")
    assert_semantic_rejection(paths, "disposition oracle mismatch")


def test_rejects_arbitrary_provenance_reason_not_derived_from_given(tmp_path):
    def given(vector_id):
        value = {"artifactSha256": "d" * 64, "coverageStrata": ["SYNTHETIC_UNIT"], "expectedArtifactSha256": "d" * 64, "foreignRepositoryReference": False, "pairRepositorySnapshotSha256": "a" * 64, "repositorySnapshotSha256": "a" * 64, "scenarioId": "SYN-1", "sourceProvenanceSha256": "c" * 64, "testProvenanceSha256": "b" * 64, "truthDisposition": "SEALED"}
        if vector_id == "N": value["scenarioIds"] = ["SYN-1", "SYN-1"]
        return value
    def oracle(vector_id):
        invalid = vector_id == "N"
        return {"result": "INVALID" if invalid else "VALID", "reasonCodes": ["DUPLICATE_SCENARIO"] if invalid else []}
    paths = arrange_semantic(tmp_path, "PROVENANCE_INTEGRITY", given, oracle)
    mutate_expected(paths, lambda value: value.update(reasonCodes=["ARBITRARY_WRONG_REASON"]), "N")
    assert_semantic_rejection(paths, "provenance oracle mismatch")


def test_rejects_occurrence_counts_that_conserve_totals_but_misclassify_exact_match(tmp_path):
    def given(vector_id):
        digest = "a" * 64
        proposal_digest = digest if vector_id == "P" else "b" * 64
        return {"goldPairDigests": [digest], "proposalOccurrences": [{"disposition": "VALID", "occurrenceIndex": 0, "pairDigest": proposal_digest}]}
    def oracle(vector_id):
        matched = vector_id == "P"
        return {"result": "VALID", "counts": {"tp": 1 if matched else 0, "fp": 0 if matched else 1, "fn": 0 if matched else 1, "duplicateCount": 0}}
    paths = arrange_semantic(tmp_path, "OCCURRENCE_SCORING", given, oracle)
    mutate_expected(paths, lambda value: value.update(counts={"tp": 0, "fp": 1, "fn": 1, "duplicateCount": 0}))
    assert_semantic_rejection(paths, "occurrence scoring oracle mismatch")


def canonical_pair(source_path=None, parameter_types=None):
    value = {"repositorySnapshotSha256": "a" * 64, "scenarioId": "SYN-1", "fullyQualifiedDeclaringType": "invalid.conformance.Service", "methodName": "run", "parameterTypes": parameter_types or [], "returnType": "void"}
    if source_path is not None: value["sourcePath"] = source_path
    return value


def canonical_output(pair):
    identity = {key: value for key, value in pair.items() if key != "sourcePath"}
    raw = json.dumps(identity, sort_keys=True, separators=(",", ":"))
    return {"result": "VALID", "canonicalBytes": raw, "pairDigest": sha(raw.encode())}


def test_rejects_canonical_relation_not_derived_from_candidate_identities(tmp_path):
    candidates = [canonical_pair("synthetic/A.java"), canonical_pair("synthetic/B.java")]
    paths = arrange_semantic(tmp_path, "CANONICAL_IDENTITY", lambda _: {"candidates": candidates}, lambda _: {"result": "VALID", "relation": "IDENTICAL_PAIR_IDENTITY", "canonicalOutputs": [canonical_output(x) for x in candidates]})
    mutate_expected(paths, lambda value: value.update(relation="DISTINCT_PAIR_IDENTITY"))
    assert_semantic_rejection(paths, "canonical relation mismatch")


def test_rejects_arbitrary_invalid_canonical_case_result(tmp_path):
    candidates = [canonical_pair(parameter_types=["java.util.List"]), canonical_pair(parameter_types=["java.util.List<java.lang.String>"])]
    def oracle(_):
        return {"result": "MIXED", "caseResults": ["VALID", "INVALID_NON_ERASED_GENERIC"], "canonicalOutputs": [canonical_output(candidates[0]), {"result": "INVALID_NON_ERASED_GENERIC"}]}
    paths = arrange_semantic(tmp_path, "CANONICAL_IDENTITY", lambda _: {"candidates": candidates}, oracle)
    mutate_expected(paths, lambda value: (value["canonicalOutputs"][1].update(result="ARBITRARY"), value.update(caseResults=["VALID", "ARBITRARY"])))
    assert_semantic_rejection(paths, "canonical case result mismatch")


def test_rejects_chain_complete_not_recomputed_from_pairs_and_edges(tmp_path):
    a, b = "a" * 64, "b" * 64
    given = lambda _: {"chainRequired": True, "orderedGoldPairDigests": [a, b], "truthEdges": [[a, b]], "proposalPairDigests": [a, b], "proposalEdges": [[a, b]]}
    oracle = lambda _: {"result": "VALID", "reasonCodes": [], "chainComplete": True, "counts": {"tp": 2, "fp": 0, "fn": 0, "duplicateCount": 0}}
    paths = arrange_semantic(tmp_path, "CHAIN_SCORING", given, oracle)
    mutate_expected(paths, lambda value: value.update(chainComplete=False))
    assert_semantic_rejection(paths, "chainComplete mismatch")


def test_rejects_scenario_coverage_not_recomputed_from_valid_proposals(tmp_path):
    given = lambda _: {"goldCount": 0, "proposalOccurrences": [{"pairDigest": "a" * 64, "structurallyValid": True}], "scenarioCount": 1}
    oracle = lambda _: {"result": "VALID", "counts": {"tp": 0, "fp": 1, "fn": 0, "duplicateCount": 0}, "scenarioCoverage": {"covered": 1, "total": 1}}
    paths = arrange_semantic(tmp_path, "EMPTY_AND_ABSTENTION", given, oracle)
    mutate_expected(paths, lambda value: value.update(scenarioCoverage={"covered": 0, "total": 1}))
    assert_semantic_rejection(paths, "empty/abstention oracle mismatch")

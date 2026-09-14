import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CONFORMANCE = (
    ROOT
    / "validation"
    / "software-factory"
    / "sf-bl005"
    / "formal-holdout-scorer-001"
    / "conformance"
)
MANIFEST = CONFORMANCE / "vector-manifest.json"
GOLDEN = CONFORMANCE / "golden-output-002.json"


def canonical_json(value):
    return (json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False) + "\n").encode()


def test_golden_output_002_is_derived_from_the_frozen_manifest_and_oracles():
    manifest_bytes = MANIFEST.read_bytes()
    manifest = json.loads(manifest_bytes)
    results = []
    for vector in manifest["vectors"]:
        expected_bytes = (CONFORMANCE / "expected" / vector["expected"]["path"]).read_bytes()
        assert hashlib.sha256(expected_bytes).hexdigest() == vector["expected"]["sha256"]
        expected = json.loads(expected_bytes)
        results.append({"oracle": expected["oracle"], "vectorId": vector["id"]})

    derived = canonical_json(
        {
            "manifestSha256": hashlib.sha256(manifest_bytes).hexdigest(),
            "results": results,
            "schemaVersion": "SFBL005-FORMAL-SCORER-CONFORMANCE-OUTPUT-001",
        }
    )

    assert len(results) == 60
    assert GOLDEN.read_bytes() == derived
    assert hashlib.sha256(derived).hexdigest() == "9c0e8f93cd1b2e8ecc85923995efdd5481d00e5fe3ecb375bf59b8f1a770af47"


def test_golden_output_002_uses_raw_ratio_macro_recall_for_r02():
    golden = json.loads(GOLDEN.read_bytes())
    r02 = next(result for result in golden["results"] if result["vectorId"] == "R02_NON_MASKING")

    assert r02["oracle"]["aggregate"]["macroRecall"] == "0.783333333333"

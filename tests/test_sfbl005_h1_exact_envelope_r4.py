import hashlib
import json
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ENVELOPE = ROOT / "validation/software-factory/sf-bl005/execution-envelope-formal-holdout-scorer-001-r4.json"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def test_exact_envelope_binds_control_candidate_and_frozen_artifacts():
    envelope = json.loads(ENVELOPE.read_text())

    assert envelope["controlCommit"] == "e30f2c0a4fdc21cdd9f892ca3302b8cc9bc59162"
    assert envelope["controlTree"] == "f9f0fbe47f7f065b5b5fc413dd8f13ffab652c08"
    assert envelope["candidate"]["commit"] == "ea15e51df0f5410bfb5532e1c255f107ccb0975b"
    assert envelope["candidate"]["tree"] == "c7a9d02fc63bb6be9f0cf27c01216743e5abb483"
    assert envelope["candidate"]["objects"] == [
        {"path": "src/main/java/com/featuredeliveryintelligence/fdi/application/FormalHoldoutScoreCli.java", "sha256": "bf4137c5c5b3357bc97bf04e417a9218993f0267a9289c3b2e830ee82971a721"},
        {"path": "src/main/java/com/featuredeliveryintelligence/fdi/product/realization/formalholdout/v1/FormalHoldoutConformanceScorer.java", "sha256": "007c3428085e51965d32b2ed3d6fb4ab9415444fcc823ea1104bd39f015b1236"},
        {"path": "src/test/java/com/featuredeliveryintelligence/fdi/application/FormalHoldoutScoreCliTests.java", "sha256": "78ad550a71bb6889d810591a3edba07561f52232c01dcffe588662d933059a62"},
        {"path": "tools/sfbl005_formal_holdout_recompute_v1.py", "sha256": "c6b440fd0a7a9690f9f70fbb53742c4fd97aea5760e380163a0ffc7c6be840b4"},
        {"path": "tests/test_sfbl005_formal_holdout_recompute_v1.py", "sha256": "a5071eb91ad49f426431cd78f2d679a772edc95ca3a505c54ce14def8c808d29"},
    ]
    for artifact in envelope["frozenArtifacts"].values():
        assert sha(ROOT / artifact["path"]) == artifact["sha256"]


def test_golden_is_derived_only_from_resealed_expected_oracles():
    envelope = json.loads(ENVELOPE.read_text())
    conformance = ROOT / "validation/software-factory/sf-bl005/formal-holdout-scorer-001/conformance"
    manifest_path = conformance / "vector-manifest.json"
    manifest = json.loads(manifest_path.read_text())
    results = []
    for vector in manifest["vectors"]:
        expected_path = conformance / "expected" / vector["expected"]["path"]
        assert sha(expected_path) == vector["expected"]["sha256"]
        expected = json.loads(expected_path.read_text())
        results.append({"oracle": expected["oracle"], "vectorId": vector["id"]})
    derived = {
        "manifestSha256": sha(manifest_path),
        "results": results,
        "schemaVersion": "SFBL005-FORMAL-SCORER-CONFORMANCE-OUTPUT-001",
    }
    expected_bytes = (json.dumps(derived, sort_keys=True, separators=(",", ":"), ensure_ascii=False) + "\n").encode()
    golden = ROOT / envelope["frozenArtifacts"]["goldenOutput"]["path"]

    assert golden.read_bytes() == expected_bytes
    r02 = next(item for item in json.loads(golden.read_text())["results"] if item["vectorId"] == "R02_NON_MASKING")
    assert r02["oracle"]["aggregate"]["macroRecall"] == "0.783333333333"


def test_git_object_bindings_when_repository_metadata_is_available():
    if not (ROOT / ".git").exists():
        return
    envelope = json.loads(ENVELOPE.read_text())

    def git_bytes(*args):
        return subprocess.check_output(["git", *args], cwd=ROOT)

    assert git_bytes("rev-parse", f"{envelope['controlCommit']}^{{tree}}").decode().strip() == envelope["controlTree"]
    assert git_bytes("rev-parse", f"{envelope['candidate']['commit']}^{{tree}}").decode().strip() == envelope["candidate"]["tree"]
    assert subprocess.run(
        ["git", "merge-base", "--is-ancestor", envelope["candidate"]["implementationBase"], envelope["candidate"]["commit"]],
        cwd=ROOT,
    ).returncode == 0
    for artifact in envelope["candidate"]["objects"]:
        raw = git_bytes("show", f"{envelope['candidate']['commit']}:{artifact['path']}")
        assert hashlib.sha256(raw).hexdigest() == artifact["sha256"]
    for artifact in envelope["frozenArtifacts"].values():
        raw = git_bytes("show", f"{envelope['controlCommit']}:{artifact['path']}")
        assert hashlib.sha256(raw).hexdigest() == artifact["sha256"]
    source_manifest = ROOT / "validation/software-factory/sf-bl005/formal-holdout-scorer-001/candidates/ea15e51df0f5410bfb5532e1c255f107ccb0975b/source-set-manifest.json"
    assert sha(source_manifest) == envelope["candidate"]["sourceManifestSha256"]


def test_dispatch_is_false_and_runner_phases_are_dependency_safe():
    envelope = json.loads(ENVELOPE.read_text())

    assert envelope["executionId"] == "SF-BL-005-FORMAL-HOLDOUT-SCORER-001"
    assert envelope["authority"]["envelopePreparationAuthorized"] is True
    assert not any(
        value
        for key, value in envelope["authority"].items()
        if key != "envelopePreparationAuthorized"
    )
    assert envelope["resourceLimits"] == {
        "hostMemoryCeiling": "8g",
        "javaHeap": "2g",
        "testForkHeap": "1g",
        "forkCount": 1,
    }
    phases = envelope["runnerPhases"]
    assert [phase["id"] for phase in phases] == ["build-and-regression", "four-candidate-runs", "byte-parity"]
    assert phases[0]["produces"] == ["candidateSourceManifest", "candidateJar"]
    assert phases[1]["requires"] == ["candidateSourceManifest", "candidateJar"]
    assert phases[1]["produces"] == ["javaRun1", "javaRun2", "pythonRun1", "pythonRun2"]
    assert phases[2]["requires"] == ["goldenOutput", "javaRun1", "javaRun2", "pythonRun1", "pythonRun2"]
    assert phases[2]["produces"] == ["parityReceipt"]
    assert [stage["id"] for stage in phases[0]["stages"]] == [
        "candidate-guard", "java17-targeted",
        "java17-full-package", "python-full-suite",
    ]
    assert [stage["id"] for stage in phases[1]["stages"]] == [
        "phase1-artifact-receipt-gate", "javaRun1", "javaRun2", "pythonRun1", "pythonRun2",
    ]
    assert [stage["id"] for stage in phases[2]["stages"]] == [
        "phase2-output-receipt-gate", "four-run-byte-parity",
    ]
    for phase in phases:
        assert phase["workingDirectory"] == envelope["materialization"]["executionRoot"]
        argv = phase["runnerInvocation"]
        assert argv[0] == "/usr/bin/python3"
        assert argv[argv.index("--candidate-full-sha") + 1] == envelope["candidate"]["commit"]
        inline_plan = json.loads(argv[argv.index("--stage-plan") + 1])
        assert inline_plan == {"stages": phase["stages"]}
    assert envelope["state"]["H1"] == "UNBOUND"
    assert envelope["state"]["formalHoldout"] == "NOT_ACCESSED"
    assert envelope["state"]["productionReady"] is False


def test_prior_failed_review_is_preserved():
    evidence = ROOT / "validation/software-factory/sf-bl005/formal-holdout-scorer-001/conformance"
    failures = [
        json.loads((evidence / "h1-exact-envelope-r4-review-001-failure.json").read_text()),
        json.loads((evidence / "h1-exact-envelope-r4-review-002-failure.json").read_text()),
    ]

    assert [(item["result"], item["candidateCommit"]) for item in failures] == [
        ("FAIL", "c2a822b0e9d0335c5c4c98fc6887e079e15e63c9"),
        ("FAIL", "4e41b2c56208e84da932b61835f61854eeacd96e"),
    ]
    assert [(item["findings"]["P0"], item["findings"]["P1"], item["findings"]["P2"]) for item in failures] == [
        (0, 2, 1), (0, 2, 0),
    ]

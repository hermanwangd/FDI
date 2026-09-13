import hashlib
import json
import subprocess
import sys
from pathlib import Path


REPO = Path(__file__).resolve().parents[1]
RUNNER = REPO / "tools" / "sfbl005_formal_scorer_stage_runner_v1.py"
SHA = "a" * 40


def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False)


def invoke(tmp_path, stages, *, execution_id="exec-001", ledger=None, logs=None):
    ledger = ledger or tmp_path / "ledger.jsonl"
    logs = logs or tmp_path / "logs"
    plan = tmp_path / "plan.json"
    plan.write_text(canonical({"stages": stages}) + "\n", encoding="utf-8")
    result = subprocess.run(
        [
            sys.executable,
            str(RUNNER),
            "--execution-id",
            execution_id,
            "--candidate-full-sha",
            SHA,
            "--ledger-path",
            str(ledger),
            "--logs-dir",
            str(logs),
            "--stage-plan",
            str(plan),
        ],
        cwd=tmp_path,
        text=True,
        capture_output=True,
    )
    return result, ledger, logs


def test_runs_exact_argv_in_order_and_digest_binds_outputs(tmp_path):
    source = tmp_path / "source.txt"
    source.write_text("input")
    first = tmp_path / "first.json"
    second = tmp_path / "second.txt"
    receipt1 = tmp_path / "receipts" / "one.json"
    receipt2 = tmp_path / "receipts" / "two.json"
    stages = [
        {
            "id": "one",
            "argv": [sys.executable, "-c", f"import pathlib; pathlib.Path({str(first)!r}).write_text('one')"],
            "outputs": [str(first)],
            "inputs": [str(source)],
            "stageOrder": 1,
            "receiptPath": str(receipt1),
        },
        {
            "id": "two",
            "argv": [sys.executable, "-c", f"import pathlib; pathlib.Path({str(second)!r}).write_text('two')"],
            "outputs": [str(second)],
            "stageOrder": 2,
            "receiptPath": str(receipt2),
        },
    ]
    result, ledger, logs = invoke(tmp_path, stages)
    assert result.returncode == 0, result.stderr
    rows = [json.loads(line) for line in ledger.read_text().splitlines()]
    assert [(r["stageId"], r["event"]) for r in rows] == [
        ("one", "START"), ("one", "END"), ("two", "START"), ("two", "END")
    ]
    assert all(r["attempt"] == 1 and r["executionId"] == "exec-001" for r in rows)
    assert rows[0]["argv"] == stages[0]["argv"]
    end = rows[1]
    assert end["exitCode"] == 0
    assert end["logSha256"] == hashlib.sha256((logs / "one.log").read_bytes()).hexdigest()
    assert end["outputDigests"] == [{
        "path": str(first), "sha256": hashlib.sha256(b"one").hexdigest(), "bytes": 3
    }]
    receipt_data = json.loads(receipt1.read_text())
    assert receipt_data["result"] == "PASS"
    assert receipt_data["schemaVersion"] == "sfbl005-stage-receipt-v1"
    assert receipt_data["stageOrder"] == 1
    assert receipt_data["candidateCommit"] == SHA
    assert receipt_data["logPath"] == str(logs / "one.log")
    assert receipt_data["inputDigests"] == [{
        "path": str(source), "sha256": hashlib.sha256(b"input").hexdigest(), "bytes": 5
    }]
    assert all(line == canonical(json.loads(line)) for line in ledger.read_text().splitlines())


def test_nonzero_stage_stops_and_preserves_start_end_evidence(tmp_path):
    marker = tmp_path / "must-not-run"
    stages = [
        {"id": "fail", "argv": [sys.executable, "-c", "import sys; print('bad', file=sys.stderr); sys.exit(7)"],
         "receiptPath": str(tmp_path / "fail.receipt.json")},
        {"id": "later", "argv": [sys.executable, "-c", f"open({str(marker)!r}, 'w').close()"],
         "receiptPath": str(tmp_path / "later.receipt.json")},
    ]
    result, ledger, _ = invoke(tmp_path, stages)
    assert result.returncode == 7
    rows = [json.loads(line) for line in ledger.read_text().splitlines()]
    assert [(r["stageId"], r["event"]) for r in rows] == [("fail", "START"), ("fail", "END")]
    assert rows[-1]["exitCode"] == 7 and rows[-1]["result"] == "FAIL"
    assert not marker.exists()


def test_expected_red_requires_nonzero_and_regex_match(tmp_path):
    receipt = tmp_path / "red.receipt.json"
    stage = {"id": "red", "argv": [sys.executable, "-c", "import sys; print('KNOWN_RED'); sys.exit(3)"],
             "expectedFailureRegex": "KNOWN_RED", "receiptPath": str(receipt)}
    result, ledger, _ = invoke(tmp_path, [stage])
    assert result.returncode == 0, result.stderr
    assert json.loads(ledger.read_text().splitlines()[-1])["result"] == "EXPECTED_FAILURE_PASS"
    assert json.loads(receipt.read_text())["result"] == "EXPECTED_FAILURE_PASS"


def test_expected_red_exit_zero_or_regex_miss_fails(tmp_path):
    zero = {"id": "red", "argv": [sys.executable, "-c", "print('KNOWN_RED')"],
            "expectedFailureRegex": "KNOWN_RED", "receiptPath": str(tmp_path / "zero.json")}
    result, _, _ = invoke(tmp_path, [zero])
    assert result.returncode != 0
    miss_root = tmp_path / "miss"
    miss_root.mkdir()
    miss = {"id": "red", "argv": [sys.executable, "-c", "import sys; print('OTHER'); sys.exit(3)"],
            "expectedFailureRegex": "KNOWN_RED", "receiptPath": str(miss_root / "miss.json")}
    result, _, _ = invoke(miss_root, [miss])
    assert result.returncode != 0


def test_expected_red_does_not_match_regex_present_only_in_logged_argv(tmp_path):
    receipt = tmp_path / "red.receipt.json"
    stage = {
        "id": "red",
        "argv": [sys.executable, "-c", "import sys; sys.exit(4)", "KNOWN_RED"],
        "expectedFailureRegex": "KNOWN_RED",
        "receiptPath": str(receipt),
    }
    result, ledger, _ = invoke(tmp_path, [stage])
    assert result.returncode == 4
    assert json.loads(ledger.read_text().splitlines()[-1])["result"] == "FAIL"
    assert json.loads(receipt.read_text())["result"] == "FAIL"


def test_parity_stage_compares_four_runs_to_golden_and_writes_receipt(tmp_path):
    golden = tmp_path / "golden.bin"
    golden.write_bytes(b"same")
    runs = []
    for n in range(4):
        path = tmp_path / f"run-{n}.bin"
        path.write_bytes(b"same")
        runs.append(str(path))
    receipt = tmp_path / "parity.receipt.json"
    stage = {"id": "parity", "parity": {"runs": runs, "golden": str(golden)}, "receiptPath": str(receipt)}
    result, ledger, _ = invoke(tmp_path, [stage])
    assert result.returncode == 0, result.stderr
    data = json.loads(receipt.read_text())
    assert data["result"] == "PASS" and data["parity"]["allByteIdentical"] is True
    assert len(data["parity"]["runDigests"]) == 4
    assert json.loads(ledger.read_text().splitlines()[-1])["exitCode"] == 0


def test_rejects_duplicate_stage_ids_before_creating_execution_artifacts(tmp_path):
    receipt = str(tmp_path / "receipt.json")
    stage = {"id": "same", "argv": [sys.executable, "-c", "pass"], "receiptPath": receipt}
    result, ledger, logs = invoke(tmp_path, [stage, stage])
    assert result.returncode != 0
    assert not ledger.exists() and not logs.exists() and not Path(receipt).exists()


def test_rejects_declared_stage_order_that_disagrees_with_array_order(tmp_path):
    stage = {"id": "one", "stageOrder": 2, "argv": [sys.executable, "-c", "pass"],
             "receiptPath": str(tmp_path / "receipt.json")}
    result, ledger, logs = invoke(tmp_path, [stage])
    assert result.returncode != 0
    assert not ledger.exists() and not logs.exists()


def test_parity_mismatch_is_fail_stop_with_receipt(tmp_path):
    golden = tmp_path / "golden.bin"
    golden.write_bytes(b"same")
    runs = []
    for n in range(4):
        path = tmp_path / f"run-{n}.bin"
        path.write_bytes(b"different" if n == 3 else b"same")
        runs.append(str(path))
    receipt = tmp_path / "parity.receipt.json"
    stage = {"id": "parity", "parity": {"runs": runs, "golden": str(golden)}, "receiptPath": str(receipt)}
    result, ledger, _ = invoke(tmp_path, [stage])
    assert result.returncode != 0
    assert json.loads(receipt.read_text())["result"] == "FAIL"
    assert json.loads(ledger.read_text().splitlines()[-1])["result"] == "FAIL"


def test_rejects_existing_or_symlinked_or_escaping_artifact_paths(tmp_path):
    stage = {"id": "one", "argv": [sys.executable, "-c", "pass"],
             "receiptPath": str(tmp_path / "receipt.json")}
    existing = tmp_path / "existing-ledger"
    existing.write_text("old")
    result, _, _ = invoke(tmp_path, [stage], ledger=existing)
    assert result.returncode != 0 and existing.read_text() == "old"

    link_target = tmp_path / "real"
    link_target.mkdir()
    link = tmp_path / "linked"
    link.symlink_to(link_target, target_is_directory=True)
    clean = tmp_path / "clean"
    clean.mkdir()
    stage["receiptPath"] = str(link / "receipt.json")
    result, ledger, logs = invoke(clean, [stage])
    assert result.returncode != 0 and not ledger.exists() and not logs.exists()


def test_refuses_rerun_of_same_identity_without_overwriting(tmp_path):
    receipt = tmp_path / "receipt.json"
    stage = {"id": "one", "argv": [sys.executable, "-c", "print('ok')"], "receiptPath": str(receipt)}
    first, ledger, logs = invoke(tmp_path, [stage])
    assert first.returncode == 0
    before = ledger.read_bytes()
    second, _, _ = invoke(tmp_path, [stage], ledger=ledger, logs=logs)
    assert second.returncode != 0 and ledger.read_bytes() == before


def test_rejects_preexisting_declared_output_before_creating_any_execution_artifact(tmp_path):
    output = tmp_path / "preexisting.json"
    output.write_text("must remain unchanged")
    receipt = tmp_path / "receipt.json"
    stage = {
        "id": "one",
        "argv": [sys.executable, "-c", f"open({str(output)!r}, 'w').write('overwritten')"],
        "outputs": [str(output)],
        "receiptPath": str(receipt),
    }
    result, ledger, logs = invoke(tmp_path, [stage])
    assert result.returncode != 0
    assert output.read_text() == "must remain unchanged"
    assert not ledger.exists() and not logs.exists() and not receipt.exists()


def test_rejects_output_collisions_across_all_execution_artifacts_before_start(tmp_path):
    ledger = tmp_path / "ledger.jsonl"
    logs = tmp_path / "logs"
    receipt_one = tmp_path / "one.receipt.json"
    receipt_two = tmp_path / "two.receipt.json"
    collision_cases = [
        [str(ledger)],
        [str(logs)],
        [str(logs / "one.log")],
        [str(receipt_one)],
        [str(tmp_path / "shared.out"), str(tmp_path / "shared.out")],
    ]
    for index, outputs in enumerate(collision_cases):
        case = tmp_path / f"case-{index}"
        case.mkdir()
        case_ledger = case / "ledger.jsonl"
        case_logs = case / "logs"
        # Rebase the paths so every collision is internal to this isolated case.
        rebased = [str(case / Path(value).relative_to(tmp_path)) for value in outputs]
        first_receipt = case / "one.receipt.json"
        second_receipt = case / "two.receipt.json"
        stages = [
            {"id": "one", "argv": [sys.executable, "-c", "pass"], "outputs": rebased,
             "receiptPath": str(first_receipt)},
            {"id": "two", "argv": [sys.executable, "-c", "pass"], "outputs": [],
             "receiptPath": str(second_receipt)},
        ]
        result, _, _ = invoke(case, stages, ledger=case_ledger, logs=case_logs)
        assert result.returncode != 0, (index, result.stderr)
        assert not case_ledger.exists() and not case_logs.exists()
        assert not first_receipt.exists() and not second_receipt.exists()

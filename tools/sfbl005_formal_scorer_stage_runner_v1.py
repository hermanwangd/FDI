#!/usr/bin/env python3
"""Single-attempt, evidence-preserving stage runner for SF-BL-005 H1.

Parity dependencies are static: the golden and all four run files must already
exist when the complete plan is validated.  They cannot be outputs first
created by an earlier stage in the same plan.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import stat
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


class PlanError(ValueError):
    pass


def canonical_bytes(value: Any) -> bytes:
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="microseconds").replace("+00:00", "Z")


def safe_path(raw: str, root: Path, label: str, *, must_be_absent: bool = False) -> Path:
    if not isinstance(raw, str) or not raw:
        raise PlanError(f"{label} must be a nonempty path string")
    path = Path(raw)
    if not path.is_absolute():
        path = root / path
    path = path.absolute()
    try:
        path.resolve(strict=False).relative_to(root)
    except ValueError as exc:
        raise PlanError(f"{label} escapes execution root: {raw}") from exc
    cursor = path
    while cursor != root.parent:
        if cursor.is_symlink():
            raise PlanError(f"{label} traverses a symlink: {raw}")
        if cursor == root:
            break
        cursor = cursor.parent
    if must_be_absent and os.path.lexists(path):
        raise PlanError(f"{label} already exists: {raw}")
    return path


def is_regular_file_within_root(path: Path, root: Path) -> bool:
    """Check with lstat before any content read, so symlinks are never followed."""
    try:
        if not stat.S_ISREG(path.lstat().st_mode):
            return False
        path.resolve(strict=True).relative_to(root)
    except (FileNotFoundError, OSError, ValueError):
        return False
    cursor = path
    while True:
        if cursor.is_symlink():
            return False
        if cursor == root:
            return True
        if cursor == root.parent:
            return False
        cursor = cursor.parent


def load_plan(raw: str) -> Any:
    candidate = Path(raw)
    if candidate.is_file():
        return json.loads(candidate.read_text(encoding="utf-8"))
    return json.loads(raw)


def parity_spec(stage: dict[str, Any]) -> dict[str, Any] | None:
    value = stage.get("parity")
    if value is None and stage.get("type") == "parity":
        value = {
            "runs": stage.get("runs", stage.get("runPaths")),
            "golden": stage.get("golden", stage.get("goldenPath")),
            "goldenSha256": stage.get("goldenSha256"),
        }
    if value is not None and not isinstance(value, dict):
        raise PlanError("parity must be an object")
    return value


def validate(args: argparse.Namespace, document: Any) -> tuple[Path, Path, list[dict[str, Any]]]:
    if not re.fullmatch(r"[0-9a-f]{40}", args.candidate_sha):
        raise PlanError("candidate full SHA must be exactly 40 lowercase hexadecimal characters")
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]{0,127}", args.execution_id):
        raise PlanError("invalid execution id")
    if isinstance(document, list):
        stages = document
    elif isinstance(document, dict) and set(document).issuperset({"stages"}):
        stages = document["stages"]
    else:
        raise PlanError("stage plan must be an ordered array or an object containing stages")
    if not isinstance(stages, list) or not stages:
        raise PlanError("stage plan must contain at least one stage")

    root = Path.cwd().resolve()
    ledger = safe_path(args.ledger_path, root, "ledger path", must_be_absent=True)
    logs = safe_path(args.logs_dir, root, "logs directory", must_be_absent=True)
    ids: set[str] = set()
    artifact_paths = {ledger, logs}
    normalized: list[dict[str, Any]] = []
    for index, raw_stage in enumerate(stages):
        if not isinstance(raw_stage, dict):
            raise PlanError(f"stage {index} must be an object")
        stage = dict(raw_stage)
        stage_id = stage.get("id", stage.get("stageId"))
        if not isinstance(stage_id, str) or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]{0,127}", stage_id):
            raise PlanError(f"stage {index} has invalid id")
        if stage_id in ids:
            raise PlanError(f"duplicate stage id: {stage_id}")
        ids.add(stage_id)
        stage["id"] = stage_id
        stage_order = index + 1
        declared_order = stage.get("stageOrder", stage.get("order", stage_order))
        if declared_order != stage_order:
            raise PlanError(f"stage {stage_id} order must be {stage_order}")
        stage["_order"] = stage_order
        receipt_raw = stage.get("receiptPath")
        receipt = safe_path(receipt_raw, root, f"receipt path for {stage_id}", must_be_absent=True)
        log = safe_path(str(logs / f"{stage_id}.log"), root, f"log path for {stage_id}", must_be_absent=True)
        if receipt in artifact_paths or log in artifact_paths or receipt == log:
            raise PlanError(f"artifact path collision in stage {stage_id}")
        artifact_paths.update({receipt, log})
        stage["_receipt"] = receipt
        stage["_log"] = log

        parity = parity_spec(stage)
        argv = stage.get("argv")
        if parity is None:
            if not isinstance(argv, list) or not argv or not all(isinstance(item, str) and item for item in argv):
                raise PlanError(f"stage {stage_id} requires a nonempty exact argv string array")
        else:
            if argv is not None:
                raise PlanError(f"parity stage {stage_id} cannot also define argv")
            runs = parity.get("runs", parity.get("runPaths"))
            golden = parity.get("golden", parity.get("goldenPath"))
            golden_sha = parity.get("goldenSha256")
            if not isinstance(runs, list) or len(runs) != 4 or not all(isinstance(item, str) for item in runs):
                raise PlanError(f"parity stage {stage_id} requires exactly four run paths")
            if not isinstance(golden_sha, str) or not re.fullmatch(r"[0-9a-f]{64}", golden_sha):
                raise PlanError(f"parity stage {stage_id} requires lowercase 64-hex goldenSha256")
            parity["_runs"] = [safe_path(item, root, f"parity run for {stage_id}") for item in runs]
            parity["_golden"] = safe_path(golden, root, f"parity golden for {stage_id}")
            parity["_golden_sha"] = golden_sha
            for path in [*parity["_runs"], parity["_golden"]]:
                if not path.is_file():
                    raise PlanError(f"parity input is not a regular file: {path}")
            stage["_parity"] = parity
        outputs = stage.get("outputs", [])
        if not isinstance(outputs, list) or not all(isinstance(item, str) for item in outputs):
            raise PlanError(f"outputs for {stage_id} must be a path string array")
        stage["_outputs"] = []
        for item in outputs:
            output = safe_path(item, root, f"output for {stage_id}", must_be_absent=True)
            if output in artifact_paths:
                raise PlanError(f"artifact path collision in stage {stage_id}: {output}")
            artifact_paths.add(output)
            stage["_outputs"].append(output)
        inputs = stage.get("inputs", [])
        if not isinstance(inputs, list) or not all(isinstance(item, str) for item in inputs):
            raise PlanError(f"inputs for {stage_id} must be a path string array")
        stage["_inputs"] = [safe_path(item, root, f"input for {stage_id}") for item in inputs]
        for path in stage["_inputs"]:
            if not path.is_file():
                raise PlanError(f"stage input is not a regular file: {path}")
        regex = stage.get("expectedFailureRegex")
        if regex is not None:
            if parity is not None or not isinstance(regex, str) or not regex:
                raise PlanError(f"invalid expectedFailureRegex for {stage_id}")
            try:
                re.compile(regex)
            except re.error as exc:
                raise PlanError(f"invalid expectedFailureRegex for {stage_id}: {exc}") from exc
        normalized.append(stage)
    return ledger, logs, normalized


def append_jsonl(path: Path, value: dict[str, Any]) -> None:
    with path.open("ab") as stream:
        stream.write(canonical_bytes(value))
        stream.flush()
        os.fsync(stream.fileno())


def digest_records(paths: list[Path]) -> list[dict[str, Any]]:
    return [{"path": str(path), "sha256": sha256(path), "bytes": path.stat().st_size} for path in paths]


def run_parity(stage: dict[str, Any], log_stream: Any) -> tuple[int, dict[str, Any]]:
    spec = stage["_parity"]
    runs: list[Path] = spec["_runs"]
    golden: Path = spec["_golden"]
    expected_golden_sha: str = spec["_golden_sha"]
    root = Path.cwd().resolve()
    if not is_regular_file_within_root(golden, root):
        details = {
            "allByteIdentical": False,
            "expectedGoldenSha256": expected_golden_sha,
            "goldenDigestMatchesFrozen": False,
            "invalidInputs": [str(golden)],
            "matchesGolden": [],
            "runDigests": [],
        }
        log_stream.write(canonical_bytes(details))
        return 1, details
    actual_golden_sha = sha256(golden)
    if actual_golden_sha != expected_golden_sha:
        details = {
            "allByteIdentical": False,
            "expectedGoldenSha256": expected_golden_sha,
            "goldenDigest": {"path": str(golden), "sha256": actual_golden_sha, "bytes": golden.stat().st_size},
            "goldenDigestMatchesFrozen": False,
            "invalidInputs": [],
            "matchesGolden": [],
            "runDigests": [],
        }
        log_stream.write(canonical_bytes(details))
        return 1, details
    invalid_runs = [str(path) for path in runs if not is_regular_file_within_root(path, root)]
    if invalid_runs:
        details = {
            "allByteIdentical": False,
            "expectedGoldenSha256": expected_golden_sha,
            "goldenDigest": {"path": str(golden), "sha256": actual_golden_sha, "bytes": golden.stat().st_size},
            "goldenDigestMatchesFrozen": True,
            "invalidInputs": invalid_runs,
            "matchesGolden": [],
            "runDigests": [],
        }
        log_stream.write(canonical_bytes(details))
        return 1, details
    golden_bytes = golden.read_bytes()
    identical = [path.read_bytes() == golden_bytes for path in runs]
    details = {
        "goldenDigest": {"path": str(golden), "sha256": sha256(golden), "bytes": golden.stat().st_size},
        "expectedGoldenSha256": expected_golden_sha,
        "goldenDigestMatchesFrozen": True,
        "invalidInputs": [],
        "runDigests": digest_records(runs),
        "matchesGolden": identical,
        "allByteIdentical": all(identical),
    }
    log_stream.write(canonical_bytes(details))
    return (0 if all(identical) else 1), details


def execute(args: argparse.Namespace, ledger: Path, logs: Path, stages: list[dict[str, Any]]) -> int:
    ledger.parent.mkdir(parents=True, exist_ok=True)
    ledger.open("xb").close()
    logs.mkdir(parents=True, exist_ok=False)
    for stage in stages:
        stage_id = stage["id"]
        stage_order = stage["_order"]
        log: Path = stage["_log"]
        receipt: Path = stage["_receipt"]
        log.parent.mkdir(parents=True, exist_ok=True)
        receipt.parent.mkdir(parents=True, exist_ok=True)
        parity = stage.get("_parity")
        argv = stage.get("argv") if parity is None else ["<internal:byte-parity-v1>", *[str(p) for p in parity["_runs"]], str(parity["_golden"])]
        started = utc_now()
        input_digests = digest_records(stage["_inputs"])
        append_jsonl(ledger, {
            "argv": argv, "attempt": 1, "candidateCommit": args.candidate_sha,
            "event": "START", "executionId": args.execution_id, "stageId": stage_id,
            "stageOrder": stage_order, "timestampUtc": started,
        })
        parity_result = None
        child_output_start = 0
        child_started = False
        with log.open("xb") as log_stream:
            log_stream.write(canonical_bytes({"argv": argv, "attempt": 1, "event": "START", "stageId": stage_id}))
            log_stream.flush()
            child_output_start = log_stream.tell()
            if parity is not None:
                exit_code, parity_result = run_parity(stage, log_stream)
            else:
                try:
                    child_started = True
                    completed = subprocess.run(argv, stdout=log_stream, stderr=subprocess.STDOUT, shell=False, check=False)
                    exit_code = completed.returncode
                except OSError as exc:
                    child_started = False
                    log_stream.write((f"spawn error: {exc}\n").encode("utf-8", errors="replace"))
                    exit_code = 127

        root = Path.cwd().resolve()
        missing_outputs = [path for path in stage["_outputs"] if not os.path.lexists(path)]
        invalid_outputs = [
            path for path in stage["_outputs"]
            if os.path.lexists(path) and not is_regular_file_within_root(path, root)
        ]
        valid_outputs = [
            path for path in stage["_outputs"]
            if os.path.lexists(path) and is_regular_file_within_root(path, root)
        ]
        output_digests = digest_records(valid_outputs)
        expected = stage.get("expectedFailureRegex")
        if expected is not None:
            child_output = log.read_bytes()[child_output_start:] if child_started else b""
            matched = exit_code != 0 and re.search(expected, child_output.decode("utf-8", errors="replace")) is not None
            result = "EXPECTED_FAILURE_PASS" if matched else "FAIL"
            effective_ok = matched and not missing_outputs and not invalid_outputs
        else:
            result = "PASS" if exit_code == 0 and not missing_outputs and not invalid_outputs else "FAIL"
            effective_ok = result == "PASS"
        ended = utc_now()
        end_record = {
            "attempt": 1, "candidateCommit": args.candidate_sha, "event": "END",
            "executionId": args.execution_id, "exitCode": exit_code, "logSha256": sha256(log),
            "invalidOutputs": [str(path) for path in invalid_outputs],
            "missingOutputs": [str(path) for path in missing_outputs], "outputDigests": output_digests,
            "result": result, "stageId": stage_id, "timestampUtc": ended,
            "stageOrder": stage_order,
        }
        append_jsonl(ledger, end_record)
        receipt_value = dict(end_record)
        receipt_value.update({
            "argv": argv,
            "candidateCommit": args.candidate_sha,
            "endedAtUtc": ended,
            "inputDigests": input_digests,
            "logPath": str(log),
            "schemaVersion": "sfbl005-stage-receipt-v1",
            "startedAtUtc": started,
        })
        if parity_result is not None:
            receipt_value["parity"] = parity_result
        receipt.write_bytes(canonical_bytes(receipt_value))
        if not effective_ok:
            if expected is not None and exit_code == 0:
                return 1
            return exit_code if exit_code != 0 else 1
    return 0


def parser() -> argparse.ArgumentParser:
    value = argparse.ArgumentParser(description=__doc__)
    value.add_argument("--execution-id", required=True)
    value.add_argument("--candidate-full-sha", "--candidate-sha", dest="candidate_sha", required=True)
    value.add_argument("--ledger-path", required=True)
    value.add_argument("--logs-dir", required=True)
    value.add_argument("--stage-plan", required=True, help="JSON file path or inline JSON")
    return value


def main(argv: list[str] | None = None) -> int:
    args = parser().parse_args(argv)
    try:
        document = load_plan(args.stage_plan)
        ledger, logs, stages = validate(args, document)
        return execute(args, ledger, logs, stages)
    except (PlanError, json.JSONDecodeError, OSError) as exc:
        print(f"stage-runner error: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())

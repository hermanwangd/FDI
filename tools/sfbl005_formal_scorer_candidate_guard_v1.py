#!/usr/bin/env python3
"""Fail-closed candidate guard and Git-object source manifest for SF-BL-005 H1."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys


FULL_COMMIT = re.compile(r"[0-9a-f]{40}\Z")

ALLOW_EXACT = frozenset({
    "contracts/sfbl005-formal-holdout-scorer-v1.schema.json",
    "src/main/java/com/featuredeliveryintelligence/fdi/application/FdiApplication.java",
    "src/main/java/com/featuredeliveryintelligence/fdi/application/FormalHoldoutScoreCli.java",
    "src/test/java/com/featuredeliveryintelligence/fdi/application/FormalHoldoutScoreCliTests.java",
    "tools/sfbl005_formal_holdout_recompute_v1.py",
    "tests/test_sfbl005_formal_holdout_recompute_v1.py",
    "tools/sfbl005_formal_scorer_candidate_guard_v1.py",
    "tests/test_sfbl005_formal_scorer_candidate_guard_v1.py",
    "validation/software-factory/sf-bl005/formal-holdout-scorer-001/conformance/catalog.json",
    "validation/software-factory/sf-bl005/formal-holdout-scorer-001/conformance/vector-manifest.json",
})
ALLOW_PREFIXES = (
    "src/main/java/com/featuredeliveryintelligence/fdi/product/realization/formalholdout/v1/",
    "src/test/java/com/featuredeliveryintelligence/fdi/product/realization/formalholdout/v1/",
    "validation/software-factory/sf-bl005/formal-holdout-scorer-001/conformance/inputs/",
    "validation/software-factory/sf-bl005/formal-holdout-scorer-001/conformance/expected/",
)
DENY_EXACT = frozenset({
    "src/main/java/com/featuredeliveryintelligence/fdi/application/MethodPairCompareCli.java",
    "src/test/java/com/featuredeliveryintelligence/fdi/application/MethodPairCompareCliTests.java",
})
DENY_PREFIXES = (
    "src/main/java/com/featuredeliveryintelligence/fdi/product/realization/methodpair/",
    "src/test/java/com/featuredeliveryintelligence/fdi/product/realization/methodpair/",
    "validation/software-factory/sf-bl005/method-calibration-005/",
    "validation/software-factory/sf-bl005/method-quality-006/",
    "validation/software-factory/sf-bl005/method-quality-007/",
    "validation/software-factory/sf-bl005/boxing-calibration-001/",
    "validation/software-factory/sf-bl005/boxing-calibration-002/",
    "validation/software-factory/sf-bl005/boxing-calibration-003/",
)


class GuardError(ValueError):
    pass


def git(repo: Path, *args: str, input_bytes: bytes | None = None) -> bytes:
    process = subprocess.run(
        ["git", *args], cwd=repo, input=input_bytes,
        stdout=subprocess.PIPE, stderr=subprocess.PIPE,
    )
    if process.returncode:
        detail = process.stderr.decode("utf-8", "replace").strip()
        raise GuardError(f"git {' '.join(args)} failed: {detail}")
    return process.stdout


def require_commit(repo: Path, value: str, label: str) -> str:
    if FULL_COMMIT.fullmatch(value) is None:
        raise GuardError(f"{label} must be a full 40-hex commit")
    resolved = git(repo, "rev-parse", "--verify", f"{value}^{{commit}}").decode().strip()
    if resolved != value:
        raise GuardError(f"{label} must resolve to itself as a commit")
    return resolved


def changed_paths(repo: Path, base: str, candidate: str) -> list[str]:
    raw = git(repo, "diff", "--name-only", "-z", f"{base}...{candidate}")
    paths = []
    for encoded in raw.split(b"\0"):
        if not encoded:
            continue
        try:
            paths.append(encoded.decode("utf-8"))
        except UnicodeDecodeError as exc:
            raise GuardError("changed path is not valid UTF-8") from exc
    return paths


def is_prefixed(path: str, prefixes: tuple[str, ...]) -> bool:
    return any(path.startswith(prefix) for prefix in prefixes)


def validate_changed_paths(paths: list[str]) -> None:
    for path in paths:
        if path in DENY_EXACT or is_prefixed(path, DENY_PREFIXES):
            raise GuardError(f"legacy path changed: {path}")
        if path not in ALLOW_EXACT and not is_prefixed(path, ALLOW_PREFIXES):
            raise GuardError(f"changed path is not allowlisted: {path}")


def source_entries(repo: Path, candidate: str) -> list[dict[str, object]]:
    raw = git(repo, "ls-tree", "-rz", "--full-tree", candidate)
    entries: list[dict[str, object]] = []
    for record in raw.split(b"\0"):
        if not record:
            continue
        metadata, separator, path_bytes = record.partition(b"\t")
        if not separator:
            raise GuardError("malformed git ls-tree record")
        fields = metadata.decode("ascii").split(" ")
        if len(fields) != 3:
            raise GuardError("malformed git ls-tree metadata")
        mode, object_type, blob = fields
        if object_type != "blob":
            raise GuardError(f"candidate tree contains non-blob entry: {path_bytes!r}")
        try:
            path = path_bytes.decode("utf-8")
        except UnicodeDecodeError as exc:
            raise GuardError("candidate path is not valid UTF-8") from exc
        content = git(repo, "cat-file", "blob", blob)
        entries.append({
            "blob": blob,
            "bytes": len(content),
            "mode": mode,
            "path": path,
            "sha256": hashlib.sha256(content).hexdigest(),
        })
    entries.sort(key=lambda entry: str(entry["path"]).encode("utf-8"))
    return entries


def validate_output(repo: Path, output_arg: str) -> Path:
    output = Path(output_arg)
    if not output.is_absolute():
        output = repo / output
    if output.exists() or output.is_symlink():
        raise GuardError("source manifest output must be absent and not a symlink")
    repo_real = repo.resolve(strict=True)
    parent_real = output.parent.resolve(strict=True)
    if parent_real != repo_real and repo_real not in parent_real.parents:
        raise GuardError("source manifest output escapes repository")
    return output


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True)
    parser.add_argument("--candidate", required=True)
    parser.add_argument("--source-manifest-output", required=True)
    args = parser.parse_args(argv)
    try:
        repo = Path(git(Path.cwd(), "rev-parse", "--show-toplevel").decode().strip())
        base = require_commit(repo, args.base, "base")
        candidate = require_commit(repo, args.candidate, "candidate")
        ancestor = subprocess.run(
            ["git", "merge-base", "--is-ancestor", base, candidate], cwd=repo,
            stdout=subprocess.DEVNULL, stderr=subprocess.PIPE,
        )
        if ancestor.returncode == 1:
            raise GuardError("base must be an ancestor of candidate")
        if ancestor.returncode != 0:
            raise GuardError("unable to verify base ancestor relationship")
        validate_changed_paths(changed_paths(repo, base, candidate))
        output = validate_output(repo, args.source_manifest_output)
        document = {
            "baseCommit": base,
            "candidateCommit": candidate,
            "entries": source_entries(repo, candidate),
            "schemaVersion": "sfbl005-source-set-manifest-v1",
        }
        encoded = json.dumps(
            document, ensure_ascii=False, sort_keys=True, separators=(",", ":")
        ).encode("utf-8") + b"\n"
        output.write_bytes(encoded)
    except (GuardError, OSError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

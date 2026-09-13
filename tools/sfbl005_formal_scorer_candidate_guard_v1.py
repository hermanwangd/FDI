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

PREP_ALLOW_EXACT = frozenset({
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
PREP_ALLOW_PREFIXES = (
    "src/main/java/com/featuredeliveryintelligence/fdi/product/realization/formalholdout/v1/",
    "src/test/java/com/featuredeliveryintelligence/fdi/product/realization/formalholdout/v1/",
    "validation/software-factory/sf-bl005/formal-holdout-scorer-001/conformance/inputs/",
    "validation/software-factory/sf-bl005/formal-holdout-scorer-001/conformance/expected/",
)
IMPLEMENTATION_ALLOW_EXACT = frozenset({
    "contracts/sfbl005-formal-holdout-scorer-v1.schema.json",
    "src/main/java/com/featuredeliveryintelligence/fdi/application/FormalHoldoutScoreCli.java",
    "src/test/java/com/featuredeliveryintelligence/fdi/application/FormalHoldoutScoreCliTests.java",
    "tools/sfbl005_formal_holdout_recompute_v1.py",
    "tests/test_sfbl005_formal_holdout_recompute_v1.py",
})
IMPLEMENTATION_ALLOW_PREFIXES = (
    "src/main/java/com/featuredeliveryintelligence/fdi/product/realization/formalholdout/v1/",
    "src/test/java/com/featuredeliveryintelligence/fdi/product/realization/formalholdout/v1/",
)
IMPLEMENTATION_EXACT_MODES = {
    "contracts/sfbl005-formal-holdout-scorer-v1.schema.json": frozenset({"100644"}),
    "src/main/java/com/featuredeliveryintelligence/fdi/application/FormalHoldoutScoreCli.java": frozenset({"100644"}),
    "src/test/java/com/featuredeliveryintelligence/fdi/application/FormalHoldoutScoreCliTests.java": frozenset({"100644"}),
    "tools/sfbl005_formal_holdout_recompute_v1.py": frozenset({"100644", "100755"}),
    "tests/test_sfbl005_formal_holdout_recompute_v1.py": frozenset({"100644"}),
}
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


def candidate_tree_metadata(repo: Path, candidate: str, paths: list[str]) -> dict[str, tuple[str, str]]:
    if not paths:
        return {}
    raw = git(repo, "ls-tree", "-rz", "--full-tree", candidate, "--", *paths)
    metadata_by_path: dict[str, tuple[str, str]] = {}
    for record in raw.split(b"\0"):
        if not record:
            continue
        metadata, separator, path_bytes = record.partition(b"\t")
        if not separator:
            raise GuardError("malformed git ls-tree record")
        fields = metadata.decode("ascii").split(" ")
        if len(fields) != 3:
            raise GuardError("malformed git ls-tree metadata")
        mode, object_type, _ = fields
        try:
            path = path_bytes.decode("utf-8")
        except UnicodeDecodeError as exc:
            raise GuardError("candidate path is not valid UTF-8") from exc
        metadata_by_path[path] = (mode, object_type)
    return metadata_by_path


def validate_implementation_entry(path: str, mode: str, object_type: str) -> None:
    if object_type != "blob":
        raise GuardError(f"implementation path has type {object_type}: {path}")
    allowed_modes = IMPLEMENTATION_EXACT_MODES.get(path)
    if allowed_modes is not None:
        if mode not in allowed_modes:
            raise GuardError(f"implementation path has forbidden mode {mode}: {path}")
        return
    if not path.endswith(".java"):
        raise GuardError(f"implementation Java path must end with .java: {path}")
    if mode != "100644":
        raise GuardError(f"implementation Java path has forbidden mode {mode}: {path}")


def validate_changed_paths(repo: Path, candidate: str, paths: list[str], profile: str) -> None:
    if profile == "prep":
        allow_exact = PREP_ALLOW_EXACT
        allow_prefixes = PREP_ALLOW_PREFIXES
    elif profile == "implementation":
        allow_exact = IMPLEMENTATION_ALLOW_EXACT
        allow_prefixes = IMPLEMENTATION_ALLOW_PREFIXES
    else:
        raise GuardError(f"unknown guard profile: {profile}")
    tree_metadata = candidate_tree_metadata(repo, candidate, paths) if profile == "implementation" else {}
    for path in paths:
        if path in DENY_EXACT or is_prefixed(path, DENY_PREFIXES):
            raise GuardError(f"legacy path changed: {path}")
        if path not in allow_exact and not is_prefixed(path, allow_prefixes):
            raise GuardError(f"changed path is not allowlisted: {path}")
        if profile == "implementation":
            if path not in tree_metadata:
                raise GuardError(f"implementation path is absent from candidate tree: {path}")
            validate_implementation_entry(path, *tree_metadata[path])


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
    parser.add_argument("--profile", choices=("prep", "implementation"), default="prep")
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
        validate_changed_paths(repo, candidate, changed_paths(repo, base, candidate), args.profile)
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

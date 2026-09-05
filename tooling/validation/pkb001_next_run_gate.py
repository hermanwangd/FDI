#!/usr/bin/env python3
"""Fail-closed validation for a PKB-001 v0.2 proposal run request."""

import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
from collections import Counter
from pathlib import Path, PurePosixPath


SKILL_PATH = "skills/pkb001/pk-s1-product-realization-v0.2/SKILL.md"
SCHEMA_PATH = "validation/pkb001/schemas/realization-proposal-v0.2.schema.json"
REQUIRED_INPUT_KINDS = (
    "PRODUCT_SEMANTICS", "GRAPHIFY_BINDING_EVIDENCE", "FROZEN_GRAPH", "PKS1_SKILL",
)
ALLOWED_INPUT_KINDS = frozenset(REQUIRED_INPUT_KINDS)
FORBIDDEN_PATH_PARTS = frozenset({
    "evaluator", "task6", "task7", "human-review", "gold", "judgments",
    "post-generation", "comparison", "evaluation",
})
SHA256 = re.compile(r"^[0-9a-f]{64}$")
GIT_SHA = re.compile(r"^[0-9a-f]{40}$")
ROLES = frozenset({"PRIMARY", "SUPPORTING"})
GRANULARITIES = frozenset({"REPOSITORY", "FILE", "TYPE", "METHOD", "TEMPLATE", "CONFIGURATION"})


def _canonical_relative(value):
    if not isinstance(value, str) or not value or "\\" in value or "\x00" in value:
        return None
    if value.startswith("/") or re.match(r"^[A-Za-z]:", value):
        return None
    parts = PurePosixPath(value).parts
    if not parts or any(part in ("", ".", "..") for part in parts) or value.endswith("/"):
        return None
    canonical = "/".join(parts)
    return canonical if canonical == value else None


def _resolved_file(root, item, reasons):
    if not isinstance(item, dict):
        reasons.add("INPUT_INVALID")
        return None
    relative = _canonical_relative(item.get("path"))
    if relative is None:
        reasons.add("INPUT_PATH_INVALID")
        return None
    candidate = root / relative
    try:
        resolved = candidate.resolve(strict=True)
        resolved.relative_to(root)
    except (OSError, RuntimeError, ValueError):
        reasons.add("INPUT_FILE_INVALID")
        return None
    if candidate.is_symlink() or not resolved.is_file():
        reasons.add("INPUT_FILE_INVALID")
        return None
    return resolved


def _actual_digest(path):
    try:
        return hashlib.sha256(path.read_bytes()).hexdigest()
    except OSError:
        return None


def _digest_matches(root, item, reasons, mismatch_reason="INPUT_DIGEST_MISMATCH"):
    path = _resolved_file(root, item, reasons)
    expected = item.get("sha256") if isinstance(item, dict) else None
    if path is None or not isinstance(expected, str) or not SHA256.fullmatch(expected):
        reasons.add(mismatch_reason)
        return False
    if _actual_digest(path) != expected:
        reasons.add(mismatch_reason)
        return False
    return True


def _load_json_input(root, item, reasons):
    path = _resolved_file(root, item, reasons)
    if path is None:
        return {}
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError):
        reasons.add("INPUT_JSON_INVALID")
        return {}
    if not isinstance(value, dict):
        reasons.add("INPUT_JSON_INVALID")
        return {}
    return value


def _plain_nonempty(value):
    return isinstance(value, str) and bool(value.strip())


def _canonical_source_path(value):
    if value == ".":
        return True
    return _canonical_relative(value) is not None


def _forbidden_path(value):
    relative = _canonical_relative(value)
    if relative is None:
        return False
    for part in PurePosixPath(relative).parts:
        folded = part.casefold()
        if folded in FORBIDDEN_PATH_PARTS:
            return True
        tokens = frozenset(token for token in re.split(r"[^a-z0-9]+", folded) if token)
        if "task6" in tokens or "task7" in tokens:
            return True
    return False


def _schema_is_valid(proposal):
    """Validate the fixed v0.2 contract without adding a runtime dependency."""
    if not isinstance(proposal, dict):
        return False
    required = {"schema_version", "run_id", "authority", "source_revision", "graph_sha256", "capability_results"}
    if set(proposal) != required:
        return False
    if proposal.get("schema_version") != "pkb001.realization-proposal.v0.2" or proposal.get("authority") != "PROPOSAL_ONLY":
        return False
    if not _plain_nonempty(proposal.get("run_id")) or not GIT_SHA.fullmatch(proposal.get("source_revision", "")):
        return False
    if not SHA256.fullmatch(proposal.get("graph_sha256", "")):
        return False
    results = proposal.get("capability_results")
    if not isinstance(results, list) or not results:
        return False
    for result in results:
        expected = {"capability_id", "outcome", "components", "evidence_refs", "confidence", "limitations"}
        if not isinstance(result, dict) or set(result) != expected or not _plain_nonempty(result.get("capability_id")):
            return False
        outcome, components = result.get("outcome"), result.get("components")
        if outcome not in ("MAPPING_PROPOSAL", "UNRESOLVED") or not isinstance(components, list):
            return False
        if outcome == "MAPPING_PROPOSAL" and (not components or not any(isinstance(c, dict) and c.get("role") == "PRIMARY" for c in components)):
            return False
        if outcome == "UNRESOLVED" and components:
            return False
        for component in components:
            fields = {"role", "granularity", "source_revision", "source_path", "qualified_symbol", "provider_node_id", "selection_reason"}
            if not isinstance(component, dict) or set(component) != fields:
                return False
            if component["role"] not in ROLES or component["granularity"] not in GRANULARITIES:
                return False
            if not GIT_SHA.fullmatch(component["source_revision"]) or not _canonical_source_path(component["source_path"]):
                return False
            if not isinstance(component["qualified_symbol"], str) or not _plain_nonempty(component["provider_node_id"]) or not _plain_nonempty(component["selection_reason"]):
                return False
        refs = result.get("evidence_refs")
        if not isinstance(refs, list) or not refs:
            return False
        for ref in refs:
            if not isinstance(ref, dict) or set(ref) != {"provider_node_id", "source_path", "source_location"}:
                return False
            if not _plain_nonempty(ref["provider_node_id"]) or not _canonical_source_path(ref["source_path"]) or not _plain_nonempty(ref["source_location"]):
                return False
        confidence = result.get("confidence")
        if isinstance(confidence, bool) or not isinstance(confidence, (int, float)) or not 0 <= confidence <= 1:
            return False
        limitations = result.get("limitations")
        if not isinstance(limitations, list) or not limitations or not all(_plain_nonempty(x) for x in limitations):
            return False
    return True


def _schema_definition_is_valid(root):
    try:
        schema = json.loads((root / SCHEMA_PATH).read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError):
        return False
    return (
        isinstance(schema, dict)
        and schema.get("$schema") == "https://json-schema.org/draft/2020-12/schema"
        and schema.get("$id") == "realization-proposal-v0.2.schema.json"
        and schema.get("additionalProperties") is False
        and schema.get("required") == ["schema_version", "run_id", "authority", "source_revision", "graph_sha256", "capability_results"]
        and isinstance(schema.get("$defs"), dict)
        and {"result", "component", "evidence_ref"}.issubset(schema["$defs"])
    )


def _committed_pkb001_run_ids(root, reasons):
    try:
        result = subprocess.run(
            ["git", "ls-files", "validation/pkb001"], cwd=root, check=True,
            capture_output=True, text=True, timeout=15,
        )
    except (OSError, subprocess.SubprocessError):
        reasons.add("RUN_ID_REGISTRY_UNAVAILABLE")
        return set()
    found = set()
    for relative in result.stdout.splitlines():
        if not relative.endswith(".json"):
            continue
        try:
            value = json.loads((root / relative).read_text(encoding="utf-8"))
        except (OSError, UnicodeError, json.JSONDecodeError):
            continue
        _collect_run_ids(value, found)
    return found


def _collect_run_ids(value, found):
    if isinstance(value, dict):
        run_id = value.get("run_id")
        if _plain_nonempty(run_id):
            found.add(run_id)
        for child in value.values():
            _collect_run_ids(child, found)
    elif isinstance(value, list):
        for child in value:
            _collect_run_ids(child, found)


def validate_next_run(root, request):
    root = Path(root).resolve()
    reasons = set()
    if not isinstance(request, dict):
        request = {}
        reasons.add("REQUEST_INVALID")
    inputs = request.get("generation_inputs", [])
    if not isinstance(inputs, list):
        inputs = []
        reasons.add("REQUIRED_INPUT_SET_INVALID")
    counts = Counter(item.get("kind") for item in inputs if isinstance(item, dict))
    if set(counts) != ALLOWED_INPUT_KINDS or any(counts[kind] != 1 for kind in REQUIRED_INPUT_KINDS):
        reasons.add("REQUIRED_INPUT_SET_INVALID")
    if any(not isinstance(item, dict) or item.get("kind") not in ALLOWED_INPUT_KINDS for item in inputs):
        reasons.add("GENERATION_INPUT_NOT_ALLOWLISTED")
    for item in inputs:
        if not isinstance(item, dict):
            reasons.add("INPUT_INVALID")
            continue
        if _forbidden_path(item.get("path")):
            reasons.add("FORBIDDEN_GENERATION_INPUT")
        _digest_matches(root, item, reasons)
    by_kind = {item["kind"]: item for item in inputs
               if isinstance(item, dict) and counts[item.get("kind")] == 1 and item.get("kind") in ALLOWED_INPUT_KINDS}
    skill = by_kind.get("PKS1_SKILL", {})
    if skill.get("path") != SKILL_PATH:
        reasons.add("SKILL_VERSION_NOT_SELECTED")
    elif not _digest_matches(root, skill, reasons, "SKILL_DIGEST_MISMATCH"):
        reasons.add("SKILL_DIGEST_MISMATCH")
    semantics = _load_json_input(root, by_kind.get("PRODUCT_SEMANTICS", {}), reasons)
    binding = _load_json_input(root, by_kind.get("GRAPHIFY_BINDING_EVIDENCE", {}), reasons)
    graph = by_kind.get("FROZEN_GRAPH", {})
    if semantics.get("status") != "FROZEN": reasons.add("PRODUCT_SEMANTICS_NOT_FROZEN")
    if semantics.get("owner") != "PRODUCT_TEAM": reasons.add("PRODUCT_SEMANTICS_OWNER_INVALID")
    if binding.get("result") != "EXACTLY_BOUND": reasons.add("GRAPHIFY_BINDING_INVALID")
    if not binding.get("query_bounds"): reasons.add("GRAPHIFY_QUERY_BOUNDS_MISSING")
    proposal = request.get("proposal", {})
    if not _schema_definition_is_valid(root): reasons.add("SCHEMA_DEFINITION_INVALID")
    if not _schema_is_valid(proposal): reasons.add("SCHEMA_INVALID")
    revision = proposal.get("source_revision") if isinstance(proposal, dict) else None
    if any(value != revision for value in (semantics.get("applicable_source_commit_sha"), binding.get("requested_revision"), binding.get("indexed_revision"))):
        reasons.add("REVISION_BINDING_MISMATCH")
    results = proposal.get("capability_results", []) if isinstance(proposal, dict) else []
    if any(isinstance(component, dict) and component.get("source_revision") != revision
           for result in results if isinstance(result, dict)
           for component in result.get("components", []) if isinstance(result.get("components", []), list)):
        reasons.add("COMPONENT_REVISION_MISMATCH")
    graph_path = _resolved_file(root, graph, reasons)
    verified_graph = _actual_digest(graph_path) if graph_path else None
    if not verified_graph or graph.get("sha256") != verified_graph:
        reasons.add("FROZEN_GRAPH_DIGEST_MISMATCH")
    if binding.get("graph_sha256") != verified_graph or (isinstance(proposal, dict) and proposal.get("graph_sha256") != verified_graph):
        reasons.add("GRAPH_BINDING_DIGEST_MISMATCH")
    run_id = proposal.get("run_id") if isinstance(proposal, dict) else None
    if not _plain_nonempty(run_id):
        reasons.add("RUN_ID_INVALID")
    elif run_id in _committed_pkb001_run_ids(root, reasons):
        reasons.add("RUN_ID_ALREADY_EXISTS")
    return {"status": "BLOCKED" if reasons else "READY", "reasons": sorted(reasons), "mappings": [],
            "run_id": run_id, "skill_path": skill.get("path"), "skill_sha256": skill.get("sha256")}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", default=".")
    parser.add_argument("--request", required=True)
    parser.add_argument("--report", required=True)
    args = parser.parse_args(argv)
    root = Path(args.root).resolve()
    try:
        request_path = _resolved_file(root, {"path": args.request}, set())
        request = json.loads(request_path.read_text(encoding="utf-8")) if request_path else None
    except (OSError, UnicodeError, json.JSONDecodeError):
        request = None
    report = validate_next_run(root, request)
    output_relative = _canonical_relative(args.report)
    if output_relative is None:
        print("report path must be canonical and repository-relative", file=sys.stderr)
        return 1
    output = root / output_relative
    try:
        output.parent.mkdir(parents=True, exist_ok=True)
        output.parent.resolve(strict=True).relative_to(root)
    except (OSError, RuntimeError, ValueError):
        print("report path must remain inside repository root", file=sys.stderr)
        return 1
    data = (json.dumps(report, indent=2, sort_keys=True) + "\n").encode()
    try:
        fd = os.open(output, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o644)
        with os.fdopen(fd, "wb") as handle:
            handle.write(data)
    except FileExistsError:
        print("report already exists", file=sys.stderr)
        return 1
    return 0 if report["status"] == "READY" else 1


if __name__ == "__main__":
    raise SystemExit(main())

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
from pathlib import Path

try:
    from jsonschema import Draft202012Validator
except ImportError:  # A missing declared runtime dependency blocks; it never falls back to a partial validator.
    Draft202012Validator = None


SKILL_PATH = "skills/pkb001/pk-s1-product-realization-v0.2/SKILL.md"
SCHEMA_PATH = "validation/pkb001/schemas/realization-proposal-v0.2.schema.json"
REQUIRED_INPUT_KINDS = (
    "PRODUCT_SEMANTICS", "GRAPHIFY_BINDING_EVIDENCE", "FROZEN_GRAPH", "PKS1_SKILL",
)
ALLOWED_INPUT_KINDS = frozenset(REQUIRED_INPUT_KINDS)
SHA256 = re.compile(r"^[0-9a-f]{64}$")


def _canonical_relative(value):
    if not isinstance(value, str) or not value or "\\" in value or "\x00" in value:
        return None
    if value.startswith("/") or re.match(r"^[A-Za-z]:", value):
        return None
    parts = value.split("/")
    if not parts or any(part in ("", ".", "..") for part in parts):
        return None
    return value


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


def _read_bytes(path):
    try:
        return path.read_bytes()
    except OSError:
        return None


def _read_verified(root, item, reasons, mismatch_reason="INPUT_DIGEST_MISMATCH"):
    path = _resolved_file(root, item, reasons)
    expected = item.get("sha256") if isinstance(item, dict) else None
    if path is None or not isinstance(expected, str) or not SHA256.fullmatch(expected):
        reasons.add(mismatch_reason)
        return None
    data = _read_bytes(path)
    if data is None or hashlib.sha256(data).hexdigest() != expected:
        reasons.add(mismatch_reason)
        return None
    return data


def _load_json_bytes(data, reasons):
    if data is None:
        return {}
    try:
        value = json.loads(data)
    except Exception:  # malformed encodings, JSON depth, and decoder failures all fail closed.
        reasons.add("INPUT_JSON_INVALID")
        return {}
    if not isinstance(value, dict):
        reasons.add("INPUT_JSON_INVALID")
        return {}
    return value


def _plain_nonempty(value):
    return isinstance(value, str) and bool(value.strip())


def _forbidden_path(value):
    relative = _canonical_relative(value)
    if relative is None:
        return False
    forbidden_tokens = {"evaluator", "gold", "judgments", "comparison", "evaluation", "task6", "task7"}
    for part in relative.split("/"):
        tokens = [token for token in re.split(r"[^a-z0-9]+", part.casefold()) if token]
        token_set = set(tokens)
        if token_set & forbidden_tokens:
            return True
        pairs = set(zip(tokens, tokens[1:]))
        if ("human", "review") in pairs or ("post", "generation") in pairs:
            return True
    return False


def _load_validator(root):
    if Draft202012Validator is None:
        return None
    try:
        schema = json.loads((root / SCHEMA_PATH).read_text(encoding="utf-8"))
        Draft202012Validator.check_schema(schema)
        return Draft202012Validator(schema)
    except Exception:
        return None


def _committed_pkb001_run_ids(root, reasons):
    try:
        result = subprocess.run(
            ["git", "ls-tree", "-r", "-z", "--name-only", "HEAD", "--", "validation/pkb001"],
            cwd=root, check=True, capture_output=True, timeout=15,
        )
    except (OSError, subprocess.SubprocessError):
        reasons.add("RUN_ID_REGISTRY_UNAVAILABLE")
        return set()
    found = set()
    for raw_relative in result.stdout.split(b"\0"):
        if not raw_relative:
            continue
        relative = os.fsdecode(raw_relative)
        if not relative.endswith(".json"):
            continue
        try:
            blob = subprocess.run(
                ["git", "show", "HEAD:" + relative], cwd=root, check=True,
                capture_output=True, timeout=15,
            ).stdout
            value = json.loads(blob)
            _collect_run_ids(value, found)
        except Exception:
            reasons.add("RUN_ID_REGISTRY_INVALID")
            return set()
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


def _component_identity_valid(component):
    if not isinstance(component, dict):
        return False
    granularity = component.get("granularity")
    path = component.get("source_path")
    if path == ".":
        path_valid = granularity == "REPOSITORY"
    else:
        path_valid = _canonical_relative(path) is not None
    symbol = component.get("qualified_symbol")
    if granularity in {"TYPE", "METHOD", "TEMPLATE", "CONFIGURATION"}:
        symbol_valid = _plain_nonempty(symbol)
    elif granularity in {"REPOSITORY", "FILE"}:
        symbol_valid = symbol == ""
    else:
        symbol_valid = False
    return path_valid and symbol_valid


def _proposal_structure(proposal, reasons):
    if not isinstance(proposal, dict):
        return None, []
    revision = proposal.get("source_revision")
    results_value = proposal.get("capability_results")
    results = results_value if isinstance(results_value, list) else []
    if not isinstance(results_value, list):
        reasons.add("SCHEMA_INVALID")
    for result in results:
        if not isinstance(result, dict):
            continue
        components_value = result.get("components")
        components = components_value if isinstance(components_value, list) else []
        if not isinstance(components_value, list):
            reasons.add("SCHEMA_INVALID")
        if any(not _component_identity_valid(component) for component in components):
            reasons.add("COMPONENT_IDENTITY_INVALID")
        refs_value = result.get("evidence_refs")
        refs = refs_value if isinstance(refs_value, list) else []
        if not isinstance(refs_value, list):
            reasons.add("SCHEMA_INVALID")
        if any(not isinstance(ref, dict) or _canonical_relative(ref.get("source_path")) is None
               for ref in refs):
            reasons.add("COMPONENT_IDENTITY_INVALID")
    return revision, results


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
    safe_items = []
    for item in inputs:
        if not isinstance(item, dict):
            reasons.add("INPUT_INVALID")
            reasons.add("GENERATION_INPUT_NOT_ALLOWLISTED")
            continue
        kind = item.get("kind")
        if not isinstance(kind, str) or kind not in ALLOWED_INPUT_KINDS:
            reasons.add("GENERATION_INPUT_NOT_ALLOWLISTED")
            continue
        safe_items.append(item)
    counts = Counter(item["kind"] for item in safe_items)
    if set(counts) != ALLOWED_INPUT_KINDS or any(counts[kind] != 1 for kind in REQUIRED_INPUT_KINDS):
        reasons.add("REQUIRED_INPUT_SET_INVALID")
    input_bytes = {}
    for item in safe_items:
        if _forbidden_path(item.get("path")):
            reasons.add("FORBIDDEN_GENERATION_INPUT")
        data = _read_verified(root, item, reasons)
        if counts[item["kind"]] == 1:
            input_bytes[item["kind"]] = data
    by_kind = {item["kind"]: item for item in safe_items if counts[item["kind"]] == 1}
    skill = by_kind.get("PKS1_SKILL", {})
    if skill.get("path") != SKILL_PATH:
        reasons.add("SKILL_VERSION_NOT_SELECTED")
    elif input_bytes.get("PKS1_SKILL") is None:
        reasons.add("SKILL_DIGEST_MISMATCH")
    semantics = _load_json_bytes(input_bytes.get("PRODUCT_SEMANTICS"), reasons)
    binding = _load_json_bytes(input_bytes.get("GRAPHIFY_BINDING_EVIDENCE"), reasons)
    graph = by_kind.get("FROZEN_GRAPH", {})
    if semantics.get("status") != "FROZEN": reasons.add("PRODUCT_SEMANTICS_NOT_FROZEN")
    if semantics.get("owner") != "PRODUCT_TEAM": reasons.add("PRODUCT_SEMANTICS_OWNER_INVALID")
    if binding.get("result") != "EXACTLY_BOUND": reasons.add("GRAPHIFY_BINDING_INVALID")
    query_bounds = binding.get("query_bounds")
    if not isinstance(query_bounds, dict) or not query_bounds:
        reasons.add("GRAPHIFY_QUERY_BOUNDS_MISSING")
    proposal = request.get("proposal", {})
    validator = _load_validator(root)
    if validator is None:
        reasons.add("SCHEMA_DEFINITION_INVALID")
    else:
        try:
            validator.validate(proposal)
        except Exception:  # jsonschema can surface referencing and regex failures from extension points.
            reasons.add("SCHEMA_INVALID")
    revision, results = _proposal_structure(proposal, reasons)
    if any(value != revision for value in (semantics.get("applicable_source_commit_sha"), binding.get("requested_revision"), binding.get("indexed_revision"))):
        reasons.add("REVISION_BINDING_MISMATCH")
    if any(component.get("source_revision") != revision
           for result in results if isinstance(result, dict)
           for component in (result.get("components") if isinstance(result.get("components"), list) else [])
           if isinstance(component, dict)):
        reasons.add("COMPONENT_REVISION_MISMATCH")
    graph_data = input_bytes.get("FROZEN_GRAPH")
    verified_graph = hashlib.sha256(graph_data).hexdigest() if graph_data is not None else None
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
    output_parts = output_relative.split("/")
    try:
        directory_fd = os.open(root, os.O_RDONLY | os.O_DIRECTORY)
        try:
            for part in output_parts[:-1]:
                try:
                    os.mkdir(part, 0o755, dir_fd=directory_fd)
                except FileExistsError:
                    pass
                next_fd = os.open(part, os.O_RDONLY | os.O_DIRECTORY | getattr(os, "O_NOFOLLOW", 0), dir_fd=directory_fd)
                os.close(directory_fd)
                directory_fd = next_fd
            flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0)
            report_fd = os.open(output_parts[-1], flags, 0o644, dir_fd=directory_fd)
            with os.fdopen(report_fd, "wb") as handle:
                handle.write((json.dumps(report, indent=2, sort_keys=True) + "\n").encode())
        finally:
            os.close(directory_fd)
    except OSError as error:
        print("cannot exclusively create report: " + str(error), file=sys.stderr)
        return 1
    return 0 if report["status"] == "READY" else 1


if __name__ == "__main__":
    raise SystemExit(main())

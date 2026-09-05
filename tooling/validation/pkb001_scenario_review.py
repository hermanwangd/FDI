#!/usr/bin/env python3
"""Validate PKB-001 scenario proposals and render immutable review surfaces."""

import argparse
import copy
import hashlib
import json
import math
import os
import re
import sys
import tempfile
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

try:
    from jsonschema import Draft202012Validator, FormatChecker
except ImportError:  # The checked-in schema is mandatory; no partial fallback is allowed.
    Draft202012Validator = None
    FormatChecker = None


SCHEMA_PATH = "validation/pkb001/schemas/scenario-proposal.schema.json"
SKILL_PATH = "skills/pkb001/pk-scenario-proposal/SKILL.md"
INPUT_KINDS = ("GRAPHIFY_BINDING", "FROZEN_GRAPH", "DELIVERY_HISTORY", "SCENARIO_SKILL")
MAX_INPUT_BYTES = 16 * 1024 * 1024
MAX_JSON_NODES = 100_000
MAX_JSON_DEPTH = 64
SHA256 = re.compile(r"^[0-9a-f]{64}$")
TECHNICAL_TEXT = re.compile(
    r"graphify|provider[_ -]?node|source[_ -]?path|qualified[_ -]?symbol|"
    r"evaluator[ _-]?gold|expected[ _-]?mapping|"
    r"[A-Za-z0-9_.\-/]+\.(?:java|py|js|ts|html|mustache|jsp|xml|ya?ml)\b|"
    r"(?:^|[\s`])(?:src|app|lib|templates?)/[A-Za-z0-9_.\-/]+|"
    r"(?:[A-Za-z_$][A-Za-z0-9_$]*\.)+[A-Za-z_$][A-Za-z0-9_$]*|"
    r"[A-Za-z_$][A-Za-z0-9_$]*\s*\(|"
    r"[A-Z][A-Za-z0-9_$]*(?:Controller|Service|Repository|Entity|Config|Configuration|Template)\b",
    re.IGNORECASE)
FORBIDDEN_COMPONENTS = frozenset({
    "evaluator", "gold", "ground-truth", "ground_truth", "judgments", "judgement",
    "comparison", "post-generation", "post_generation", "accepted-semantics",
    "accepted_semantics", "forward-semantics", "forward_semantics", "human-review",
    "task6-blind-review", "task7-evaluation",
})


class ScenarioReviewError(ValueError):
    def __init__(self, reasons):
        self.reasons = tuple(sorted(set(reasons))) or ("VALIDATION_FAILED",)
        super().__init__(", ".join(self.reasons))


def _snapshot_json(value, depth=0, budget=None):
    if budget is None:
        budget = [MAX_JSON_NODES]
    budget[0] -= 1
    if budget[0] < 0 or depth > MAX_JSON_DEPTH:
        raise ScenarioReviewError(["INPUT_TOO_LARGE"])
    value_type = type(value)
    if value_type is dict:
        result = {}
        for key, child in dict.items(value):
            if type(key) is not str:
                raise ScenarioReviewError(["NON_JSON_INPUT"])
            result[key] = _snapshot_json(child, depth + 1, budget)
        return result
    if value_type is list:
        return [_snapshot_json(child, depth + 1, budget) for child in list.__iter__(value)]
    if value_type in (str, bool) or value is None or value_type is int:
        return value
    if value_type is float and math.isfinite(value):
        return value
    raise ScenarioReviewError(["NON_JSON_INPUT"])


def _canonical_relative(path):
    if not isinstance(path, str) or not path or path.isspace() or "\\" in path or "\x00" in path:
        return None
    if path.startswith("/") or re.match(r"^[A-Za-z]:", path):
        return None
    parts = path.split("/")
    if any(part in ("", ".", "..") for part in parts):
        return None
    return path


def _forbidden_path(path):
    canonical = _canonical_relative(path)
    if canonical is None:
        return False
    return any(component.lower() in FORBIDDEN_COMPONENTS for component in canonical.split("/"))


def _has_symlink_component(root, canonical):
    current = Path(root)
    for component in canonical.split("/"):
        current = current / component
        if current.is_symlink():
            return True
    return False


def _resolve_input(root, relative, reasons):
    canonical = _canonical_relative(relative)
    if canonical is None:
        reasons.add("INPUT_PATH_INVALID")
        return None
    if _has_symlink_component(root, canonical):
        reasons.add("INPUT_PATH_SYMLINK")
        return None
    candidate = root / canonical
    try:
        resolved = candidate.resolve(strict=True)
        resolved.relative_to(root)
    except (OSError, RuntimeError, ValueError):
        reasons.add("INPUT_FILE_INVALID")
        return None
    if not resolved.is_file():
        reasons.add("INPUT_FILE_INVALID")
        return None
    return resolved


def _read_bounded(path, reasons, reason="INPUT_FILE_INVALID"):
    if path is None:
        return None
    try:
        if path.stat().st_size > MAX_INPUT_BYTES:
            reasons.add("INPUT_TOO_LARGE")
            return None
        return path.read_bytes()
    except OSError:
        reasons.add(reason)
        return None


def _load_json(data, reasons):
    if data is None:
        return None
    try:
        value = json.loads(data)
        return _snapshot_json(value)
    except ScenarioReviewError as error:
        reasons.update(error.reasons)
    except Exception:
        reasons.add("INPUT_JSON_INVALID")
    return None


def _schema_validator(root, reasons):
    if Draft202012Validator is None:
        reasons.add("SCHEMA_RUNTIME_UNAVAILABLE")
        return None
    path = _resolve_input(root, SCHEMA_PATH, reasons)
    schema = _load_json(_read_bounded(path, reasons), reasons)
    if not isinstance(schema, dict):
        reasons.add("SCHEMA_DEFINITION_INVALID")
        return None
    try:
        Draft202012Validator.check_schema(schema)
        return Draft202012Validator(schema, format_checker=FormatChecker())
    except Exception:
        reasons.add("SCHEMA_DEFINITION_INVALID")
        return None


def _verify_inputs(root, document, reasons):
    inputs = document.get("generation_inputs") if isinstance(document, dict) else None
    if not isinstance(inputs, list):
        reasons.add("GENERATION_INPUT_SET_INVALID")
        return {}, {}
    counts = Counter(item.get("kind") for item in inputs if isinstance(item, dict))
    if set(counts) != set(INPUT_KINDS) or any(counts[kind] != 1 for kind in INPUT_KINDS):
        reasons.add("GENERATION_INPUT_SET_INVALID")
    by_kind = {}
    parsed = {}
    for item in inputs:
        if not isinstance(item, dict) or item.get("kind") not in INPUT_KINDS:
            reasons.add("GENERATION_INPUT_SET_INVALID")
            continue
        kind = item["kind"]
        if _forbidden_path(item.get("path")):
            reasons.add("FORBIDDEN_GENERATION_INPUT")
            continue
        path = _resolve_input(root, item.get("path"), reasons)
        data = _read_bounded(path, reasons)
        digest = hashlib.sha256(data).hexdigest() if data is not None else None
        if not isinstance(item.get("sha256"), str) or digest != item.get("sha256"):
            reasons.add("INPUT_DIGEST_MISMATCH")
        by_kind[kind] = {"item": item, "path": path, "data": data, "digest": digest}
        if kind != "SCENARIO_SKILL":
            parsed[kind] = _load_json(data, reasons)
    skill = by_kind.get("SCENARIO_SKILL", {})
    if skill.get("item", {}).get("path") != SKILL_PATH:
        reasons.add("SCENARIO_SKILL_INVALID")
    return by_kind, parsed


def _verify_bindings(document, by_kind, parsed, reasons):
    revision = document.get("source_revision")
    graph_digest = document.get("graph_sha256")
    cutoff = document.get("history_cutoff")
    binding = parsed.get("GRAPHIFY_BINDING")
    graph = by_kind.get("FROZEN_GRAPH", {})
    history = parsed.get("DELIVERY_HISTORY")
    if not isinstance(binding, dict) or binding.get("result") != "EXACTLY_BOUND":
        reasons.add("GRAPHIFY_BINDING_INVALID")
        snapshot = {}
    else:
        snapshot = binding.get("snapshot_binding")
        if not isinstance(snapshot, dict):
            reasons.add("GRAPHIFY_BINDING_INVALID")
            snapshot = {}
    if any(value != revision for value in (
            snapshot.get("requested_revision"), snapshot.get("indexed_revision"))):
        reasons.add("REVISION_BINDING_MISMATCH")
    if any(value != graph_digest for value in (
            snapshot.get("graph_sha256"),
            binding.get("graph_sha256") if isinstance(binding, dict) else None,
            graph.get("digest"))):
        reasons.add("GRAPH_BINDING_MISMATCH")
    if not isinstance(history, dict) or history.get("status") != "FROZEN" \
            or history.get("post_cutoff_knowledge_policy") != "EXCLUDE_AFTER_CUTOFF":
        reasons.add("DELIVERY_HISTORY_BINDING_INVALID")
    else:
        if history.get("source_commit_sha") != revision:
            reasons.add("REVISION_BINDING_MISMATCH")
        if history.get("history_cutoff") != cutoff:
            reasons.add("HISTORY_CUTOFF_MISMATCH")
        if len(history.get("commits", [])) + len(history.get("pull_requests", [])) > 10_000:
            reasons.add("INPUT_TOO_LARGE")
        _verify_history_cutoff(history, cutoff, reasons)
    graph_body = parsed.get("FROZEN_GRAPH")
    if isinstance(graph_body, dict):
        if len(graph_body.get("nodes", [])) + len(graph_body.get("links", [])) > 10_000:
            reasons.add("INPUT_TOO_LARGE")
    else:
        reasons.add("GRAPH_INPUT_INVALID")


def _timestamp(value):
    if not isinstance(value, str):
        return None
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
        if parsed.tzinfo is None:
            return None
        return parsed.astimezone(timezone.utc)
    except ValueError:
        return None


def _verify_history_cutoff(history, cutoff, reasons):
    cutoff_time = _timestamp(cutoff)
    if cutoff_time is None:
        reasons.add("DELIVERY_HISTORY_BINDING_INVALID")
        return
    for commit in history.get("commits", []):
        committed_at = _timestamp(commit.get("committed_at")) if isinstance(commit, dict) else None
        if committed_at is None:
            reasons.add("DELIVERY_HISTORY_BINDING_INVALID")
        elif committed_at > cutoff_time:
            reasons.add("POST_CUTOFF_HISTORY_ITEM")
    for pull_request in history.get("pull_requests", []):
        if not isinstance(pull_request, dict):
            reasons.add("DELIVERY_HISTORY_BINDING_INVALID")
            continue
        for field in ("created_at", "updated_at"):
            observed_at = _timestamp(pull_request.get(field))
            if observed_at is None:
                reasons.add("DELIVERY_HISTORY_BINDING_INVALID")
            elif observed_at > cutoff_time:
                reasons.add("POST_CUTOFF_HISTORY_ITEM")


def _pointer_value(document, pointer):
    if not isinstance(pointer, str) or not re.fullmatch(
            r"/(?:nodes|links|commits|pull_requests)/(?:0|[1-9][0-9]*)", pointer):
        return None, "EVIDENCE_REFERENCE_NOT_ATOMIC"
    collection, index = pointer[1:].split("/")
    rows = document.get(collection) if isinstance(document, dict) else None
    if not isinstance(rows, list) or int(index) >= len(rows):
        return None, "EVIDENCE_REFERENCE_UNRESOLVED"
    value = rows[int(index)]
    if not isinstance(value, dict):
        return None, "EVIDENCE_REFERENCE_UNRESOLVED"
    return value, None


def _evidence_summary(channel, pointer, value):
    collection = pointer.split("/")[1]
    if channel == "STRUCTURAL" and collection == "nodes":
        label = value.get("label") or value.get("id")
        location = value.get("source_location") or value.get("source_file")
        return "節點 %s（%s）" % (label, location)
    if channel == "STRUCTURAL":
        relation = value.get("relation") or "structural link"
        location = value.get("source_location") or value.get("source_file") or "location unavailable"
        return "關聯 %s（%s）" % (relation, location)
    if collection == "commits":
        commit = str(value.get("commit_sha") or "unknown")[:12]
        return "提交 %s：%s" % (commit, value.get("subject") or "subject unavailable")
    return "PR #%s：%s" % (value.get("number") or "?", value.get("title") or "title unavailable")


def _verify_evidence(document, by_kind, parsed, reasons):
    catalog = document.get("evidence_catalog") if isinstance(document, dict) else None
    if not isinstance(catalog, list):
        return {}
    availability = document.get("channel_availability", {})
    input_for_channel = {"STRUCTURAL": "FROZEN_GRAPH", "DELIVERY_HISTORY": "DELIVERY_HISTORY"}
    resolved = {}
    seen = set()
    for reference in catalog:
        if not isinstance(reference, dict):
            continue
        evidence_id = reference.get("evidence_id")
        if evidence_id in seen:
            reasons.add("DUPLICATE_EVIDENCE_ID")
        seen.add(evidence_id)
        channel = reference.get("channel")
        if availability.get(channel, {}).get("status") == "UNAVAILABLE":
            reasons.add("UNAVAILABLE_CHANNEL_CITED")
        kind = input_for_channel.get(channel)
        bound = by_kind.get(kind, {})
        if (reference.get("artifact_path") != bound.get("item", {}).get("path")
                or reference.get("artifact_sha256") != bound.get("digest")):
            reasons.add("EVIDENCE_BINDING_MISMATCH")
            continue
        value, reason = _pointer_value(parsed.get(kind), reference.get("json_pointer"))
        if reason:
            reasons.add(reason)
            continue
        summary = _evidence_summary(channel, reference["json_pointer"], value)
        if not summary or "None" in summary:
            reasons.add("EVIDENCE_REFERENCE_UNRESOLVED")
            continue
        resolved[evidence_id] = summary
    for capability in document.get("capability_proposals", []) if isinstance(document, dict) else []:
        if not isinstance(capability, dict):
            continue
        _verify_claim_refs(capability.get("evidence_refs"), resolved, reasons)
        for scenario in capability.get("scenarios", []) if isinstance(capability.get("scenarios"), list) else []:
            if isinstance(scenario, dict):
                _verify_claim_refs(scenario.get("evidence_refs"), resolved, reasons)
    return resolved


def _verify_claim_refs(refs, resolved, reasons):
    if not isinstance(refs, list) or not refs:
        reasons.add("EMPTY_EVIDENCE_CLAIM")
    elif any(reference not in resolved for reference in refs):
        reasons.add("EVIDENCE_REFERENCE_UNRESOLVED")


def _semantic_strings(document):
    for capability in document.get("capability_proposals", []):
        if not isinstance(capability, dict):
            continue
        for field in ("title", "description"):
            yield capability.get(field)
        for field in ("includes", "excludes", "non_goals"):
            yield from capability.get(field, []) if isinstance(capability.get(field), list) else []
        capability_decision = capability.get("decision")
        if isinstance(capability_decision, dict):
            replacement = capability_decision.get("replacement_behavior")
            if isinstance(replacement, dict):
                for field in ("title", "description"):
                    yield replacement.get(field)
                for field in ("includes", "excludes", "non_goals"):
                    values = replacement.get(field)
                    yield from values if isinstance(values, list) else []
        for scenario in capability.get("scenarios", []):
            if not isinstance(scenario, dict):
                continue
            for field in ("title", "when"):
                yield scenario.get(field)
            for field in ("given", "then"):
                yield from scenario.get(field, []) if isinstance(scenario.get(field), list) else []
            scenario_decision = scenario.get("decision")
            if isinstance(scenario_decision, dict):
                replacement = scenario_decision.get("replacement_behavior")
                if isinstance(replacement, dict):
                    for field in ("title", "when"):
                        yield replacement.get(field)
                    for field in ("given", "then"):
                        values = replacement.get(field)
                        yield from values if isinstance(values, list) else []


def _verify_semantics(document, reasons):
    if any(isinstance(value, str) and TECHNICAL_TEXT.search(value)
           for value in _semantic_strings(document)):
        reasons.add("TECHNICAL_IDENTIFIER_IN_BEHAVIOR")
    capability_ids = []
    scenario_ids = []
    for capability in document.get("capability_proposals", []):
        if not isinstance(capability, dict):
            continue
        capability_ids.append(capability.get("capability_id"))
        for scenario in capability.get("scenarios", []):
            if isinstance(scenario, dict):
                scenario_ids.append(scenario.get("scenario_id"))
    if len(capability_ids) != len(set(capability_ids)):
        reasons.add("DUPLICATE_CAPABILITY_ID")
    if len(scenario_ids) != len(set(scenario_ids)):
        reasons.add("DUPLICATE_SCENARIO_ID")


def _verify_decisions(document, reasons, generation):
    proposal_revision = document.get("proposal_revision")
    proposal_digest = document.get("proposal_sha256")
    for capability in document.get("capability_proposals", []):
        if not isinstance(capability, dict):
            continue
        decisions = [capability.get("decision")]
        decisions.extend(scenario.get("decision") for scenario in capability.get("scenarios", [])
                         if isinstance(scenario, dict))
        for decision in decisions:
            if generation:
                if decision is not None:
                    reasons.add("GENERATED_DECISION_FORBIDDEN")
                continue
            if decision is None:
                continue
            if not isinstance(decision, dict) \
                    or decision.get("proposal_revision") != proposal_revision \
                    or decision.get("proposal_sha256") != proposal_digest:
                reasons.add("DECISION_BINDING_MISMATCH")


def _validate_document(root, document, proposal_path=None, generation=True):
    root = Path(root).resolve()
    reasons = set()
    snapshot = _snapshot_json(document)
    validator = _schema_validator(root, reasons)
    if validator is not None:
        if list(validator.iter_errors(snapshot)):
            reasons.add("SCHEMA_INVALID")
    if not isinstance(snapshot, dict):
        reasons.add("SCHEMA_INVALID")
        raise ScenarioReviewError(reasons)
    expected_kind = "SCENARIO_PROPOSAL" if generation else "SCENARIO_REVIEW_SURFACE"
    if snapshot.get("artifact_kind") != expected_kind:
        reasons.add("ARTIFACT_KIND_INVALID")
    if proposal_path is not None:
        path = Path(proposal_path)
        try:
            resolved = path.resolve(strict=True)
            resolved.relative_to(root)
        except (OSError, RuntimeError, ValueError):
            reasons.add("PROPOSAL_PATH_INVALID")
        else:
            data = _read_bounded(resolved, reasons)
            on_disk = _load_json(data, reasons)
            if on_disk != snapshot:
                reasons.add("PROPOSAL_BYTES_MISMATCH")
    by_kind, parsed = _verify_inputs(root, snapshot, reasons)
    _verify_bindings(snapshot, by_kind, parsed, reasons)
    resolved_evidence = _verify_evidence(snapshot, by_kind, parsed, reasons)
    if not generation and snapshot.get("resolved_evidence") != resolved_evidence:
        reasons.add("RESOLVED_EVIDENCE_MISMATCH")
    _verify_semantics(snapshot, reasons)
    _verify_decisions(snapshot, reasons, generation)
    if reasons:
        raise ScenarioReviewError(reasons)
    validated = copy.deepcopy(snapshot)
    validated["resolved_evidence"] = resolved_evidence
    return validated


def validate_proposal(root, proposal, proposal_path=None):
    """Validate generation output without allowing any human decision fields."""
    try:
        return _validate_document(root, proposal, proposal_path=proposal_path, generation=True)
    except ScenarioReviewError:
        raise
    except Exception as error:
        raise ScenarioReviewError(["REQUEST_INVALID"]) from error


def validate_review(root, review):
    """Validate a rendered or human-completed review against exact proposal binding."""
    try:
        validated = _validate_document(root, review, generation=False)
        _verify_original_proposal(root, validated)
        return validated
    except ScenarioReviewError:
        raise
    except Exception as error:
        raise ScenarioReviewError(["REQUEST_INVALID"]) from error


def _verify_original_proposal(root, review):
    reasons = set()
    root = Path(root).resolve()
    relative = review.get("proposal_artifact_path")
    path = _resolve_input(root, relative, reasons)
    data = _read_bounded(path, reasons)
    digest = hashlib.sha256(data).hexdigest() if data is not None else None
    if digest != review.get("proposal_sha256"):
        reasons.add("PROPOSAL_DIGEST_MISMATCH")
    original = _load_json(data, reasons)
    if isinstance(original, dict):
        validated_original = validate_proposal(root, original, path)
        expected = copy.deepcopy(original)
        expected["artifact_kind"] = "SCENARIO_REVIEW_SURFACE"
        expected["proposal_artifact_path"] = relative
        expected["proposal_sha256"] = digest
        expected["resolved_evidence"] = validated_original["resolved_evidence"]
        if _without_decisions(expected) != _without_decisions(review):
            reasons.add("ORIGINAL_PROPOSAL_MISMATCH")
    else:
        reasons.add("ORIGINAL_PROPOSAL_INVALID")
    if reasons:
        raise ScenarioReviewError(reasons)


def _without_decisions(document):
    snapshot = copy.deepcopy(document)
    for capability in snapshot.get("capability_proposals", []):
        capability["decision"] = None
        for scenario in capability.get("scenarios", []):
            scenario["decision"] = None
    return snapshot


def _proposal_digest(proposal_path):
    return hashlib.sha256(Path(proposal_path).read_bytes()).hexdigest()


def render_review(root, proposal, proposal_path):
    """Return deterministic JSON and Markdown; never infer or edit semantic text."""
    validated = validate_proposal(root, proposal, proposal_path)
    review = copy.deepcopy(proposal)
    review["artifact_kind"] = "SCENARIO_REVIEW_SURFACE"
    review["proposal_artifact_path"] = Path(proposal_path).resolve().relative_to(
        Path(root).resolve()).as_posix()
    review["proposal_sha256"] = _proposal_digest(proposal_path)
    review["resolved_evidence"] = validated["resolved_evidence"]
    validate_review(root, review)
    return review, _markdown(review)


def _bullets(values):
    return "\n".join("- " + value for value in values)


def _evidence_lines(review, refs):
    summaries = review["resolved_evidence"]
    return "\n".join("- `%s`: %s" % (reference, summaries[reference]) for reference in refs)


def _markdown(review):
    lines = [
        "# PKB-001 情境提案個別審查 / Scenario Proposal Review",
        "",
        "- Run ID: `%s`" % review["run_id"],
        "- Proposal revision: `%s`" % review["proposal_revision"],
        "- Proposal SHA-256: `%s`" % review["proposal_sha256"],
        "- Authority / status: `PROPOSAL_ONLY / UNREVIEWED`",
        "- Source revision: `%s`" % review["source_revision"],
        "- Graph SHA-256: `%s`" % review["graph_sha256"],
        "- Delivery cutoff: `%s`" % review["history_cutoff"],
        "- Reviewer exposure: technical evidence is visible; content-level arm anonymity is not claimed.",
        "- Experiment limitation: `%s`" % review["experiment_limitation"],
        "",
        "> 信心僅為未校準排序提示，不是校準後機率。每項證據只證明該觀察存在；請由 Human Reviewer 判斷推論是否成立。",
        "",
    ]
    for capability in review["capability_proposals"]:
        lines.extend([
            "## 能力提案 / Capability Proposal — %s" % capability["capability_id"],
            "",
            "**%s**" % capability["title"],
            "",
            capability["description"],
            "",
            "包含 / Includes:", "", _bullets(capability["includes"]), "",
            "排除 / Excludes:", "", _bullets(capability["excludes"]), "",
            "非目標 / Non-goals:", "", _bullets(capability["non_goals"]), "",
            "推論理由 / Inference rationale: %s" % capability["inference_rationale"],
            "",
            "Confidence: `%.4f` (`UNCALIBRATED_RANKING_HINT`)" % capability["confidence"],
            "",
            "限制 / Limitations:", "", _bullets(capability["limitations"]), "",
            "證據 / Evidence:", "", _evidence_lines(review, capability["evidence_refs"]), "",
            "能力決定 / Capability decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**",
            "",
        ])
        for scenario in capability["scenarios"]:
            lines.extend([
                "### 情境 / Scenario — %s" % scenario["scenario_id"],
                "",
                "**%s** (`%s`)" % (scenario["title"], scenario["scope"]),
                "",
                "Given / 前提:", "", _bullets(scenario["given"]), "",
                "When / 當: %s" % scenario["when"], "",
                "Then / 則:", "", _bullets(scenario["then"]), "",
                "推論理由 / Inference rationale: %s" % scenario["inference_rationale"],
                "",
                "Confidence: `%.4f` (`UNCALIBRATED_RANKING_HINT`)" % scenario["confidence"],
                "",
                "限制 / Limitations:", "", _bullets(scenario["limitations"]), "",
                "證據 / Evidence:", "", _evidence_lines(review, scenario["evidence_refs"]), "",
                "情境決定 / Scenario decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**",
                "",
            ])
    return "\n".join(lines).rstrip() + "\n"


def accepted_scenarios(root, review):
    """Select decision-confirmed behavior only; this does not freeze or publish it."""
    review = validate_review(root, review)
    accepted = []
    for capability in review.get("capability_proposals", []):
        capability_decision = capability.get("decision")
        if not _decision_accepts(capability_decision):
            continue
        for scenario in capability.get("scenarios", []):
            decision = scenario.get("decision")
            if not _decision_accepts(decision):
                continue
            behavior = {key: copy.deepcopy(scenario[key])
                        for key in ("title", "given", "when", "then", "scope")}
            if decision["action"] == "EDIT":
                behavior = copy.deepcopy(decision["replacement_behavior"])
            accepted.append(dict({
                "capability_id": capability["capability_id"],
                "scenario_id": scenario["scenario_id"],
            }, **behavior))
    return accepted


def _decision_accepts(decision):
    return isinstance(decision, dict) and (
        decision.get("action") == "ACCEPT"
        or (decision.get("action") == "EDIT" and decision.get("edit_confirmed") is True))


def _safe_output(root, value, review_output=False):
    root = Path(root).resolve()
    path = Path(value)
    try:
        relative = path.relative_to(root) if path.is_absolute() else path
    except ValueError:
        raise ScenarioReviewError(["OUTPUT_PATH_INVALID"])
    canonical = _canonical_relative(relative.as_posix())
    if canonical is None:
        raise ScenarioReviewError(["OUTPUT_PATH_INVALID"])
    if review_output and not canonical.startswith("validation/pkb001/"):
        raise ScenarioReviewError(["OUTPUT_PATH_INVALID"])
    candidate = root / canonical
    parent_relative = candidate.parent.relative_to(root).as_posix()
    if parent_relative != "." and _has_symlink_component(root, parent_relative):
        raise ScenarioReviewError(["OUTPUT_PATH_SYMLINK"])
    return candidate


def _ensure_safe_directory(root, relative):
    canonical = _canonical_relative(relative)
    if canonical is None:
        raise ScenarioReviewError(["OUTPUT_PATH_INVALID"])
    current = Path(root)
    for component in canonical.split("/"):
        current = current / component
        if current.is_symlink():
            raise ScenarioReviewError(["OUTPUT_PATH_SYMLINK"])
        try:
            current.mkdir()
        except FileExistsError:
            if current.is_symlink():
                raise ScenarioReviewError(["OUTPUT_PATH_SYMLINK"])
            if not current.is_dir():
                raise ScenarioReviewError(["OUTPUT_PATH_INVALID"])
        except OSError as error:
            raise ScenarioReviewError(["OUTPUT_WRITE_FAILED"]) from error
    return current


def _reserve_run(root, review, json_path, markdown_path):
    claim_dir = _ensure_safe_directory(root, "validation/pkb001/scenario-review-runs")
    claim_name = hashlib.sha256(review["run_id"].encode()).hexdigest() + ".claim.json"
    claim_path = claim_dir / claim_name
    body = (json.dumps({
        "run_id": review["run_id"],
        "proposal_sha256": review["proposal_sha256"],
        "json_output": json_path.relative_to(root).as_posix(),
        "markdown_output": markdown_path.relative_to(root).as_posix(),
    }, sort_keys=True, indent=2) + "\n").encode()
    try:
        descriptor = os.open(str(claim_path), os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o444)
    except FileExistsError:
        raise ScenarioReviewError(["RUN_ID_ALREADY_EXISTS"])
    try:
        with os.fdopen(descriptor, "wb") as handle:
            handle.write(body)
            handle.flush()
            os.fsync(handle.fileno())
    except Exception:
        claim_path.unlink(missing_ok=True)
        raise
    return claim_path


def _stage(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    handle = tempfile.NamedTemporaryFile(prefix=".scenario-review-", dir=str(path.parent), delete=False)
    staged = Path(handle.name)
    try:
        handle.write(data)
        handle.flush()
        os.fsync(handle.fileno())
        return staged
    except Exception:
        staged.unlink(missing_ok=True)
        raise
    finally:
        handle.close()


def write_review_outputs(root, proposal_path, json_output, markdown_output):
    """Exclusively publish the pair, rolling back if either destination collides."""
    root = Path(root).resolve()
    proposal_path = _safe_output(root, proposal_path)
    read_reasons = set()
    data = _read_bounded(proposal_path, read_reasons)
    proposal = _load_json(data, read_reasons)
    if not isinstance(proposal, dict):
        raise ScenarioReviewError(read_reasons or ["INPUT_JSON_INVALID"])
    review, markdown = render_review(root, proposal, proposal_path)
    json_path = _safe_output(root, json_output, review_output=True)
    markdown_path = _safe_output(root, markdown_output, review_output=True)
    if json_path == markdown_path:
        raise ScenarioReviewError(["OUTPUT_PATH_INVALID"])
    if json_path.exists() or markdown_path.exists():
        raise ScenarioReviewError(["OUTPUT_ALREADY_EXISTS"])
    _ensure_safe_directory(root, json_path.parent.relative_to(root).as_posix())
    _ensure_safe_directory(root, markdown_path.parent.relative_to(root).as_posix())
    claim_path = _reserve_run(root, review, json_path, markdown_path)
    json_bytes = (json.dumps(review, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode()
    markdown_bytes = markdown.encode()
    json_stage = None
    markdown_stage = None
    json_created = False
    markdown_created = False
    try:
        json_stage = _stage(json_path, json_bytes)
        markdown_stage = _stage(markdown_path, markdown_bytes)
        os.link(str(json_stage), str(json_path))
        json_created = True
        os.link(str(markdown_stage), str(markdown_path))
        markdown_created = True
        os.chmod(json_path, 0o444)
        os.chmod(markdown_path, 0o444)
    except FileExistsError:
        if json_created:
            json_path.unlink()
        if markdown_created:
            markdown_path.unlink()
        claim_path.unlink(missing_ok=True)
        raise ScenarioReviewError(["OUTPUT_ALREADY_EXISTS"])
    except OSError:
        if json_created:
            json_path.unlink()
        if markdown_created:
            markdown_path.unlink()
        claim_path.unlink(missing_ok=True)
        raise ScenarioReviewError(["OUTPUT_WRITE_FAILED"])
    finally:
        if json_stage is not None:
            json_stage.unlink(missing_ok=True)
        if markdown_stage is not None:
            markdown_stage.unlink(missing_ok=True)
    return json_path, markdown_path


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", required=True)
    parser.add_argument("--proposal", required=True)
    parser.add_argument("--json-output", required=True)
    parser.add_argument("--markdown-output", required=True)
    args = parser.parse_args(argv)
    try:
        paths = write_review_outputs(
            args.root, args.proposal, args.json_output, args.markdown_output)
    except ScenarioReviewError as error:
        print(json.dumps({"status": "BLOCKED", "reasons": error.reasons}), file=sys.stderr)
        return 1
    print(json.dumps({"status": "RENDERED", "json": str(paths[0]), "markdown": str(paths[1])}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

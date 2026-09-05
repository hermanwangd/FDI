import hashlib
import json
import subprocess
from concurrent.futures import ThreadPoolExecutor
from copy import deepcopy
from pathlib import Path

import pytest
from jsonschema import Draft202012Validator

from tooling.validation.pkb001_scenario_review import (
    ScenarioReviewError,
    accepted_scenarios,
    render_review,
    validate_proposal,
    validate_review,
    write_review_outputs,
)


REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe"
SCHEMA_SOURCE = Path(__file__).parents[1] / "validation/pkb001/schemas/scenario-proposal.schema.json"
SKILL_SOURCE = Path(__file__).parents[1] / "skills/pkb001/pk-scenario-proposal/SKILL.md"


def _write(root, relative, value):
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(value, bytes):
        data = value
    else:
        data = (json.dumps(value, ensure_ascii=False, sort_keys=True) + "\n").encode()
    path.write_bytes(data)
    return hashlib.sha256(data).hexdigest()


@pytest.fixture
def proposal_root(tmp_path):
    (tmp_path / "validation/pkb001/schemas").mkdir(parents=True)
    (tmp_path / "skills/pkb001/pk-scenario-proposal").mkdir(parents=True)
    (tmp_path / "validation/pkb001/schemas/scenario-proposal.schema.json").write_bytes(
        SCHEMA_SOURCE.read_bytes())
    skill_path = "skills/pkb001/pk-scenario-proposal/SKILL.md"
    skill_digest = _write(tmp_path, skill_path, SKILL_SOURCE.read_bytes())
    graph_path = "validation/pkb001/artifacts/graph.json"
    graph_digest = _write(tmp_path, graph_path, {
        "nodes": [{"id": "owner-controller", "label": "OwnerController",
                   "source_file": "src/main/java/example/OwnerController.java",
                   "source_location": "class OwnerController"}],
        "links": [],
    })
    binding_path = "validation/pkb001/runtime/binding.json"
    binding_digest = _write(tmp_path, binding_path, {
        "result": "EXACTLY_BOUND",
        "snapshot_binding": {
            "requested_revision": REVISION,
            "indexed_revision": REVISION,
            "graph_sha256": graph_digest,
        },
        "graph_sha256": graph_digest,
    })
    history_path = "validation/pkb001/datasets/history.json"
    history_digest = _write(tmp_path, history_path, {
        "status": "FROZEN", "source_commit_sha": REVISION,
        "history_cutoff": "2026-08-26T10:57:54Z",
        "post_cutoff_knowledge_policy": "EXCLUDE_AFTER_CUTOFF",
        "commits": [{"commit_sha": "b" * 40,
                     "subject": "Add owner lookup behavior",
                     "committed_at": "2020-01-02T03:04:05Z",
                     "changed_paths": ["src/main/java/example/OwnerController.java"]}],
        "pull_requests": [],
    })
    proposal = {
        "schema_version": "pkb001.scenario-proposal.v0.1",
        "artifact_kind": "SCENARIO_PROPOSAL",
        "run_id": "pkb001-scenario-review-test-001",
        "proposal_revision": 1,
        "authority": "PROPOSAL_ONLY",
        "scenario_status": "UNREVIEWED",
        "review_language": "zh-TW",
        "source_revision": REVISION,
        "graph_sha256": graph_digest,
        "history_cutoff": "2026-08-26T10:57:54Z",
        "generation_context": {
            "generator_role": "SCENARIO_PROPOSAL_AGENT",
            "generator_identity": "isolated-generation-test",
            "evaluator_gold_access": "PROHIBITED",
            "accepted_forward_semantics_access": "PROHIBITED",
            "post_generation_judgments_access": "PROHIBITED",
        },
        "reviewer_exposure": {
            "technical_evidence_visible": True,
            "content_level_arm_anonymity": "NOT_CLAIMED",
        },
        "experiment_limitation": "RECONSTRUCTION_CONSISTENCY_NOT_INDEPENDENT_PRODUCT_VALIDATION",
        "generation_inputs": [
            {"kind": "GRAPHIFY_BINDING", "path": binding_path, "sha256": binding_digest},
            {"kind": "FROZEN_GRAPH", "path": graph_path, "sha256": graph_digest},
            {"kind": "DELIVERY_HISTORY", "path": history_path, "sha256": history_digest},
            {"kind": "SCENARIO_SKILL", "path": skill_path, "sha256": skill_digest},
        ],
        "channel_availability": {
            "STRUCTURAL": {"status": "AVAILABLE", "reason": None},
            "DELIVERY_HISTORY": {"status": "AVAILABLE", "reason": None},
        },
        "evidence_catalog": [
            {"evidence_id": "EV-G-001", "channel": "STRUCTURAL",
             "artifact_path": graph_path, "artifact_sha256": graph_digest,
             "json_pointer": "/nodes/0"},
            {"evidence_id": "EV-H-001", "channel": "DELIVERY_HISTORY",
             "artifact_path": history_path, "artifact_sha256": history_digest,
             "json_pointer": "/commits/0"},
        ],
        "capability_proposals": [{
            "capability_id": "HYP-CAPABILITY-001",
            "title": "飼主管理",
            "description": "讓使用者依姓名尋找飼主。",
            "includes": ["依完整或部分姓名搜尋"],
            "excludes": ["變更醫療紀錄"],
            "non_goals": ["推斷未觀察到的授權規則"],
            "evidence_refs": ["EV-G-001", "EV-H-001"],
            "inference_rationale": "結構與交付紀錄皆顯示飼主搜尋行為。",
            "confidence": 0.76,
            "confidence_interpretation": "UNCALIBRATED_RANKING_HINT",
            "limitations": ["靜態證據無法證明完整產品意圖。"],
            "decision": None,
            "scenarios": [{
                "scenario_id": "HYP-SCENARIO-001",
                "title": "依姓名尋找飼主",
                "given": ["使用者已進入飼主搜尋流程。"],
                "when": "使用者提交完整或部分姓名。",
                "then": ["系統顯示符合條件的飼主。"],
                "scope": "REQUIRED_ACCEPTANCE",
                "evidence_refs": ["EV-G-001", "EV-H-001"],
                "inference_rationale": "可解析的節點與提交主旨共同支持此行為假說。",
                "confidence": 0.73,
                "confidence_interpretation": "UNCALIBRATED_RANKING_HINT",
                "limitations": ["無法從靜態結構確認所有空結果呈現細節。"],
                "decision": None,
            }],
        }],
    }
    proposal_path = tmp_path / "input/proposal.json"
    proposal_path.parent.mkdir()
    proposal_path.write_text(json.dumps(proposal, ensure_ascii=False, indent=2) + "\n")
    return tmp_path, proposal_path, proposal


def test_validates_exact_bindings_and_resolves_node_and_commit_summaries(proposal_root):
    root, proposal_path, proposal = proposal_root
    validated = validate_proposal(root, proposal, proposal_path)
    assert validated["source_revision"] == REVISION
    assert validated["resolved_evidence"] == {
        "EV-G-001": "節點 OwnerController（class OwnerController）",
        "EV-H-001": "提交 " + "b" * 12 + "：Add owner lookup behavior",
    }


@pytest.mark.parametrize(("mutation", "reason"), [
    (lambda p: p.update(authority="PRODUCT_TRUTH"), "SCHEMA_INVALID"),
    (lambda p: p.update(scenario_status="FROZEN"), "SCHEMA_INVALID"),
    (lambda p: p["generation_inputs"].append(
        {"kind": "EVALUATOR_GOLD", "path": "gold.json", "sha256": "a" * 64}),
     "GENERATION_INPUT_SET_INVALID"),
    (lambda p: p["generation_inputs"][0].update(path="validation/pkb001/evaluator/gold.json"),
     "FORBIDDEN_GENERATION_INPUT"),
    (lambda p: p["generation_inputs"][1].update(sha256="0" * 64),
     "INPUT_DIGEST_MISMATCH"),
    (lambda p: p.update(graph_sha256="0" * 64), "GRAPH_BINDING_MISMATCH"),
    (lambda p: p.update(source_revision="a" * 40), "REVISION_BINDING_MISMATCH"),
    (lambda p: p.update(history_cutoff="2026-08-27T00:00:00Z"), "HISTORY_CUTOFF_MISMATCH"),
    (lambda p: p["evidence_catalog"][0].update(json_pointer="/"), "SCHEMA_INVALID"),
    (lambda p: p["evidence_catalog"][0].update(json_pointer="/nodes/99"),
     "EVIDENCE_REFERENCE_UNRESOLVED"),
    (lambda p: p["evidence_catalog"][1].update(json_pointer="/commits"),
     "EVIDENCE_REFERENCE_NOT_ATOMIC"),
    (lambda p: p["capability_proposals"][0]["scenarios"][0].update(evidence_refs=["EV-NOPE"]),
     "EVIDENCE_REFERENCE_UNRESOLVED"),
    (lambda p: p["capability_proposals"][0]["scenarios"][0].update(
        when="呼叫 OwnerController.processFindForm()"), "TECHNICAL_IDENTIFIER_IN_BEHAVIOR"),
    (lambda p: p["capability_proposals"][0].update(description="讀取 src/main/App.java"),
     "TECHNICAL_IDENTIFIER_IN_BEHAVIOR"),
    (lambda p: p["capability_proposals"][0]["scenarios"][0].update(confidence=1.5),
     "SCHEMA_INVALID"),
    (lambda p: p["capability_proposals"][0]["scenarios"][0].update(decision={
        "action": "ACCEPT", "reviewer_identity": "agent"}), "SCHEMA_INVALID"),
])
def test_invalid_proposals_fail_closed(proposal_root, mutation, reason):
    root, proposal_path, original = proposal_root
    proposal = deepcopy(original)
    mutation(proposal)
    with pytest.raises(ScenarioReviewError) as caught:
        validate_proposal(root, proposal, proposal_path)
    assert reason in caught.value.reasons


@pytest.mark.parametrize("technical_text", [
    "OwnerController", "com.example.OwnerRepository", "foo()",
    "templates/owners/search.html",
])
def test_obvious_technical_identifiers_are_rejected_from_behavior(proposal_root, technical_text):
    root, _, original = proposal_root
    proposal = deepcopy(original)
    proposal["capability_proposals"][0]["scenarios"][0]["when"] = technical_text
    with pytest.raises(ScenarioReviewError) as caught:
        validate_proposal(root, proposal)
    assert "TECHNICAL_IDENTIFIER_IN_BEHAVIOR" in caught.value.reasons


def test_unavailable_channel_is_explicit_and_cannot_be_cited(proposal_root):
    root, proposal_path, original = proposal_root
    proposal = deepcopy(original)
    proposal["channel_availability"]["DELIVERY_HISTORY"] = {
        "status": "UNAVAILABLE", "reason": "No pull-request provider was available"}
    proposal["evidence_catalog"] = proposal["evidence_catalog"][:1]
    capability = proposal["capability_proposals"][0]
    capability["evidence_refs"] = ["EV-G-001"]
    capability["scenarios"][0]["evidence_refs"] = ["EV-G-001"]
    assert validate_proposal(root, proposal)["channel_availability"][
        "DELIVERY_HISTORY"]["status"] == "UNAVAILABLE"

    proposal["evidence_catalog"].append(original["evidence_catalog"][1])
    with pytest.raises(ScenarioReviewError) as caught:
        validate_proposal(root, proposal, proposal_path)
    assert "UNAVAILABLE_CHANNEL_CITED" in caught.value.reasons


def test_renderer_is_deterministic_bilingual_and_leaves_decisions_empty(proposal_root):
    root, proposal_path, proposal = proposal_root
    first_json, first_markdown = render_review(root, proposal, proposal_path)
    second_json, second_markdown = render_review(root, deepcopy(proposal), proposal_path)
    assert first_json == second_json
    assert first_markdown == second_markdown
    assert first_json["artifact_kind"] == "SCENARIO_REVIEW_SURFACE"
    assert first_json["proposal_sha256"] == hashlib.sha256(proposal_path.read_bytes()).hexdigest()
    scenario = first_json["capability_proposals"][0]["scenarios"][0]
    assert scenario["decision"] is None
    assert "## 能力提案 / Capability Proposal" in first_markdown
    assert "### 情境 / Scenario" in first_markdown
    assert "信心僅為未校準排序提示" in first_markdown
    assert "節點 OwnerController" in first_markdown
    assert "Add owner lookup behavior" in first_markdown
    assert "ACCEPT / EDIT / REJECT" in first_markdown
    assert "RECONSTRUCTION_CONSISTENCY_NOT_INDEPENDENT_PRODUCT_VALIDATION" in first_markdown


def test_decisions_bind_exact_artifact_and_filter_reject_and_unconfirmed_edit(proposal_root):
    root, proposal_path, proposal = proposal_root
    review, _ = render_review(root, proposal, proposal_path)
    scenario = review["capability_proposals"][0]["scenarios"][0]
    base = {
        "reviewer_identity": "human-reviewer",
        "reviewed_at": "2026-09-05T01:02:03Z",
        "reason": "逐項審查完成",
        "proposal_revision": review["proposal_revision"],
        "proposal_sha256": review["proposal_sha256"],
    }
    review["capability_proposals"][0]["decision"] = dict(base, action="ACCEPT")

    scenario["decision"] = dict(base, action="REJECT")
    validate_review(root, review)
    assert accepted_scenarios(root, review) == []

    scenario["decision"] = dict(
        base, action="EDIT", edit_confirmed=False,
        replacement_behavior={
            "title": "修改後情境", "given": ["使用者已進入搜尋流程。"],
            "when": "使用者送出姓名。", "then": ["系統顯示符合項目。"],
            "scope": "REQUIRED_ACCEPTANCE"})
    validate_review(root, review)
    assert accepted_scenarios(root, review) == []

    scenario["decision"]["edit_confirmed"] = True
    accepted = accepted_scenarios(root, review)
    assert accepted[0]["title"] == "修改後情境"

    scenario["decision"]["proposal_sha256"] = "0" * 64
    with pytest.raises(ScenarioReviewError) as caught:
        validate_review(root, review)
    assert "DECISION_BINDING_MISMATCH" in caught.value.reasons


def test_rejected_or_unreviewed_capability_excludes_accepted_child(proposal_root):
    root, proposal_path, proposal = proposal_root
    review, _ = render_review(root, proposal, proposal_path)
    capability = review["capability_proposals"][0]
    scenario = capability["scenarios"][0]
    base = {
        "reviewer_identity": "human-reviewer",
        "reviewed_at": "2026-09-05T01:02:03Z",
        "reason": "逐項審查完成",
        "proposal_revision": review["proposal_revision"],
        "proposal_sha256": review["proposal_sha256"],
    }
    scenario["decision"] = dict(base, action="ACCEPT")
    assert accepted_scenarios(root, review) == []
    capability["decision"] = dict(base, action="REJECT")
    assert accepted_scenarios(root, review) == []
    capability["decision"] = dict(base, action="ACCEPT")
    assert [item["scenario_id"] for item in accepted_scenarios(root, review)] == [
        "HYP-SCENARIO-001"]


def test_review_rejects_tampered_base_semantics_even_when_digest_claim_is_unchanged(proposal_root):
    root, proposal_path, proposal = proposal_root
    review, _ = render_review(root, proposal, proposal_path)
    review["capability_proposals"][0]["title"] = "竄改後能力"
    with pytest.raises(ScenarioReviewError) as caught:
        validate_review(root, review)
    assert "ORIGINAL_PROPOSAL_MISMATCH" in caught.value.reasons


def test_accepted_scenarios_validates_before_filtering(proposal_root):
    root, proposal_path, proposal = proposal_root
    review, _ = render_review(root, proposal, proposal_path)
    review["capability_proposals"][0]["scenarios"][0]["decision"] = {"action": "ACCEPT"}
    with pytest.raises(ScenarioReviewError):
        accepted_scenarios(root, review)


def test_future_history_items_are_rejected(proposal_root):
    root, _, proposal = proposal_root
    history_input = next(item for item in proposal["generation_inputs"]
                         if item["kind"] == "DELIVERY_HISTORY")
    history_path = root / history_input["path"]
    history = json.loads(history_path.read_text())
    history["commits"][0]["committed_at"] = "2026-08-26T10:57:55Z"
    digest = _write(root, history_input["path"], history)
    history_input["sha256"] = digest
    proposal["evidence_catalog"][1]["artifact_sha256"] = digest
    with pytest.raises(ScenarioReviewError) as caught:
        validate_proposal(root, proposal)
    assert "POST_CUTOFF_HISTORY_ITEM" in caught.value.reasons


@pytest.mark.parametrize("malformed", [[], None, {"capability_proposals": [None]}])
def test_malformed_top_level_and_children_fail_closed(proposal_root, malformed):
    root, _, _ = proposal_root
    with pytest.raises(ScenarioReviewError):
        validate_proposal(root, malformed)


def test_cli_exclusive_creation_duplicate_run_and_no_partial_orphan(proposal_root):
    root, proposal_path, _ = proposal_root
    json_out = root / "validation/pkb001/reviews/review.json"
    md_out = root / "validation/pkb001/reviews/review.md"
    write_review_outputs(root, proposal_path, json_out, md_out)
    before_json = json_out.read_bytes()
    before_md = md_out.read_bytes()

    with pytest.raises(ScenarioReviewError) as caught:
        write_review_outputs(root, proposal_path,
                             root / "validation/pkb001/reviews/other.json",
                             root / "validation/pkb001/reviews/other.md")
    assert "RUN_ID_ALREADY_EXISTS" in caught.value.reasons
    assert not (root / "validation/pkb001/reviews/other.json").exists()
    assert not (root / "validation/pkb001/reviews/other.md").exists()
    assert json_out.read_bytes() == before_json
    assert md_out.read_bytes() == before_md

    second = deepcopy(json.loads(proposal_path.read_text()))
    second["run_id"] = "pkb001-scenario-review-test-002"
    other_proposal_path = root / "input/proposal-2.json"
    other_proposal_path.write_text(json.dumps(second, ensure_ascii=False, indent=2) + "\n")
    blocked_markdown = root / "validation/pkb001/output/review.md"
    blocked_markdown.parent.mkdir(parents=True)
    blocked_markdown.write_text("keep me")
    with pytest.raises(ScenarioReviewError) as caught:
        write_review_outputs(root, other_proposal_path,
                             root / "validation/pkb001/output/review.json", blocked_markdown)
    assert "OUTPUT_ALREADY_EXISTS" in caught.value.reasons
    assert not (root / "validation/pkb001/output/review.json").exists()
    assert blocked_markdown.read_text() == "keep me"


def test_cli_entrypoint_writes_both_outputs(proposal_root):
    root, proposal_path, _ = proposal_root
    script = Path(__file__).parents[1] / "tooling/validation/pkb001_scenario_review.py"
    command = ["python3", str(script), "--root", str(root),
               "--proposal", str(proposal_path.relative_to(root)),
               "--json-output", "validation/pkb001/reviews/review.json",
               "--markdown-output", "validation/pkb001/reviews/review.md"]
    result = subprocess.run(command, capture_output=True, text=True)
    assert result.returncode == 0, result.stderr
    assert (root / "validation/pkb001/reviews/review.json").exists()
    assert (root / "validation/pkb001/reviews/review.md").exists()


def test_concurrent_same_run_claim_allows_exactly_one_complete_pair(proposal_root):
    root, proposal_path, _ = proposal_root
    script = Path(__file__).parents[1] / "tooling/validation/pkb001_scenario_review.py"

    def invoke(suffix):
        return subprocess.run([
            "python3", str(script), "--root", str(root),
            "--proposal", str(proposal_path.relative_to(root)),
            "--json-output", "validation/pkb001/reviews/%s.json" % suffix,
            "--markdown-output", "validation/pkb001/reviews/%s.md" % suffix,
        ], capture_output=True, text=True)

    with ThreadPoolExecutor(max_workers=2) as pool:
        results = list(pool.map(invoke, ("one", "two")))
    assert sorted(result.returncode for result in results) == [0, 1]
    complete_pairs = sum(
        (root / ("validation/pkb001/reviews/%s.json" % suffix)).exists()
        and (root / ("validation/pkb001/reviews/%s.md" % suffix)).exists()
        for suffix in ("one", "two"))
    assert complete_pairs == 1


@pytest.mark.parametrize("output", ["../escape.json", "outside/review.json"])
def test_review_outputs_reject_parent_escape_and_non_review_area(proposal_root, output):
    root, proposal_path, _ = proposal_root
    with pytest.raises(ScenarioReviewError) as caught:
        write_review_outputs(root, proposal_path, output,
                             "validation/pkb001/reviews/review.md")
    assert "OUTPUT_PATH_INVALID" in caught.value.reasons


def test_schema_is_draft_2020_12_and_rejects_malformed_review_record(proposal_root):
    _, _, proposal = proposal_root
    schema = json.loads(SCHEMA_SOURCE.read_text())
    assert schema["$schema"] == "https://json-schema.org/draft/2020-12/schema"
    validator = Draft202012Validator(schema, format_checker=Draft202012Validator.FORMAT_CHECKER)
    bad = deepcopy(proposal)
    bad["artifact_kind"] = "SCENARIO_REVIEW_SURFACE"
    bad["proposal_sha256"] = "a" * 64
    bad["capability_proposals"][0]["scenarios"][0]["decision"] = {
        "action": "EDIT", "reviewer_identity": "human-reviewer",
        "reviewed_at": "2026-09-05T01:02:03Z", "reason": "edited",
        "proposal_revision": 1, "proposal_sha256": "a" * 64,
        "edit_confirmed": True}
    assert list(validator.iter_errors(bad))


def test_skill_declares_allowlist_gold_isolation_and_no_semantic_generation_in_python():
    text = SKILL_SOURCE.read_text()
    for term in ("GRAPHIFY_BINDING", "FROZEN_GRAPH", "DELIVERY_HISTORY", "SCENARIO_SKILL"):
        assert f"`{term}`" in text
    assert "MUST NOT" in text and "evaluator gold" in text
    assert "accepted Forward semantics" in text
    assert "Python renderer" in text and "must not invent" in text
    assert "PROPOSAL_ONLY" in text and "UNREVIEWED" in text


def test_validation_is_bounded(proposal_root):
    root, proposal_path, proposal = proposal_root
    proposal["capability_proposals"][0]["scenarios"] *= 101
    with pytest.raises(ScenarioReviewError) as caught:
        validate_proposal(root, proposal, proposal_path)
    assert "SCHEMA_INVALID" in caught.value.reasons

import importlib.util
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MODULE_PATH = ROOT / "tooling/validation/build_pkb001_human_review_packet.py"
PACKET_PATH = ROOT / "validation/pkb001/human-review/human-review-decision-packet.json"
MARKDOWN_PATH = ROOT / "validation/pkb001/human-review/HUMAN-REVIEW-DECISION-PACKET.md"
CHINESE_MARKDOWN_PATH = (
    ROOT / "validation/pkb001/human-review/HUMAN-REVIEW-DECISION-PACKET.zh-TW.md"
)


def load_builder():
    spec = importlib.util.spec_from_file_location("human_review_packet", MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_human_review_packet_is_reproducible_and_keeps_authority_pending():
    builder = load_builder()
    expected = json.loads(PACKET_PATH.read_text())

    assert builder.build_packet(ROOT) == expected
    assert expected["status"] == "PENDING_PRODUCT_TEAM_REVIEW"
    assert expected["current_prototype_decision"] == "REVISE"
    assert expected["semantic_publication_allowed"] is False
    assert len(expected["items"]) == 15
    assert sum(item["needs_resolution"] for item in expected["items"]) == 11
    assert all(
        item["product_team_decision"]
        == {"action": None, "approved_name": None, "notes": None}
        for item in expected["items"]
    )

    forward = expected["forward_comparison"]
    assert forward["expected_component_path_recall"] == 0.9583333333
    assert forward["proposed_component_path_precision"] == 0.84
    assert forward["expected_graph_node_coverage"] == {
        "cited": 17,
        "expected": 24,
        "rate": 0.7083333333,
    }
    assert forward["exact_proposed_component_matches"] == {
        "matched": 0,
        "expected": 24,
        "rate": 0.0,
    }

    forward_items = [item for item in expected["items"] if item["source_arm"] == "FORWARD"]
    reverse_items = [item for item in expected["items"] if item["source_arm"] == "REVERSE"]
    assert len(forward_items) == 10
    assert len(reverse_items) == 5
    assert all(item["forward_component_comparison"] is not None for item in forward_items)
    assert all(item["forward_component_comparison"] is None for item in reverse_items)
    assert all(item["proposal_only"] is True for item in reverse_items)
    assert all(
        comparison["difference_classification"]
        in {"GRANULARITY_OR_IDENTIFIER_MISMATCH", "MISSING_EVIDENCE"}
        for comparison in (item["forward_component_comparison"] for item in forward_items)
    )


def test_human_review_markdown_covers_every_item_without_claiming_approval():
    packet = json.loads(PACKET_PATH.read_text())
    markdown = MARKDOWN_PATH.read_text()

    assert "PENDING_PRODUCT_TEAM_REVIEW" in markdown
    assert "Semantic publication allowed: **false**" in markdown
    for item in packet["items"]:
        assert f"### {item['blind_id']} — {item['candidate_capability']}" in markdown
    assert "Product Team decision: **PENDING**" in markdown
    assert "23/24 (95.8%)" in markdown
    assert "17/24 (70.8%)" in markdown
    assert "0/24" in markdown
    assert markdown.count("Expected components:") == 10
    assert markdown.count("Proposed components:") == 10
    assert markdown.count("Reverse proposal-only:") == 5


def test_status_separates_pending_proposals_from_existing_evaluation_reference():
    status = json.loads((ROOT / "STATUS.json").read_text())

    assert status["decision"] == "REVISE"
    assert status["human_review_status"] == "PARTIALLY_REVIEWED"
    assert status["semantic_publication_allowed"] is False
    assert (ROOT / status["review_packet"]).is_file()
    assert status["review_packet"] != status["evaluation_reference_packet"]
    assert status["evaluation_reference_packet"] == (
        "validation/pkb001/human-review/HUMAN-REVIEW-DECISION-PACKET.zh-TW.md"
    )
    assert status["active_backlog_item"] == "PKB-BL-007"
    assert status["active_implementation_plan"] is None


def test_chinese_review_packet_preserves_all_pending_decisions_and_boundaries():
    packet = json.loads(PACKET_PATH.read_text())
    translated = CHINESE_MARKDOWN_PATH.read_text()

    assert "等待產品團隊審核" in translated
    assert "不允許發布產品語意" in translated
    assert "目前原型決策：**REVISE**" in translated
    for item in packet["items"]:
        assert f"### {item['blind_id']}" in translated
    assert translated.count("產品團隊決策：**待填寫**") == 15
    assert "23/24（95.8%）" in translated
    assert "21/25（84.0%）" in translated
    assert "17/24（70.8%）" in translated
    assert "0/24" in translated
    assert "大致找到正確區域，但尚未精準指出核心元件" in translated
    assert translated.count("Expected components：") == 10
    assert translated.count("Proposed components：") == 10
    assert translated.count("差異分類：") == 10
    assert translated.count("Reverse proposal-only：") == 5


def test_individual_scenario_packet_is_bound_and_has_no_invented_decisions():
    from tooling.validation.pkb001_scenario_review import validate_review, accepted_scenarios

    status = json.loads((ROOT / "STATUS.json").read_text())
    review = json.loads((ROOT / status["original_review_json"]).read_text())
    validated = validate_review(ROOT, review)
    capabilities = validated["capability_proposals"]
    scenarios = [scenario for cap in capabilities for scenario in cap["scenarios"]]
    assert len(capabilities) == 6
    assert len(scenarios) == 10
    assert all(cap["decision"] is None for cap in capabilities)
    assert all(scenario["decision"] is None for scenario in scenarios)
    assert len(validated["resolved_evidence"]) == 48
    assert accepted_scenarios(ROOT, review) == []


def test_first_accepted_slice_matches_decisions_and_frozen_manifest():
    import hashlib
    from tooling.validation.pkb001_scenario_review import validate_review, accepted_scenarios

    status = json.loads((ROOT / "STATUS.json").read_text())
    review = json.loads((ROOT / status["review_json"]).read_text())
    validate_review(ROOT, review)
    selected = accepted_scenarios(ROOT, review)
    assert {s["scenario_id"] for s in selected} == {"HYP-SCENARIO-001", "HYP-SCENARIO-002"}
    assert all(s["capability_id"] == "HYP-CAPABILITY-001" for s in selected)
    decisions = [d for c in review["capability_proposals"]
                 for d in [c["decision"], *[s["decision"] for s in c["scenarios"]]]]
    assert sum(d is not None for d in decisions) == 3
    assert sum(d is None for d in decisions) == 13
    assert all(d["action"] == "ACCEPT" for d in decisions if d is not None)
    assert all(d["reviewer_identity"] == "current_user" for d in decisions if d is not None)
    frozen = json.loads((ROOT / status["accepted_experiment_input"]).read_text())
    assert frozen["status"] == "FROZEN"
    assert frozen["owner"] == "HUMAN_REVIEWER"
    assert frozen["applicable_source_commit_sha"] == review["source_revision"]
    assert len(frozen["capabilities"]) == 1
    cap = frozen["capabilities"][0]
    original_cap = review["capability_proposals"][0]
    for key in ("capability_id", "title", "description", "includes", "excludes", "non_goals"):
        assert cap[key] == original_cap[key]
    assert cap["scenarios"] == [
        {k: v for k, v in scenario.items() if k != "capability_id"}
        for scenario in selected
    ]
    manifest = json.loads((ROOT / status["acceptance_manifest"]).read_text())
    for key in ("decision_artifact", "semantics_artifact"):
        binding = manifest[key]
        assert hashlib.sha256((ROOT / binding["path"]).read_bytes()).hexdigest() == binding["sha256"]
    assert manifest["proposal_sha256"] == review["proposal_sha256"]
    assert manifest["authorization"]["user_statement"] == "三項接受"
    assert manifest["forward_execution_readiness"] == "NOT_READY"
    assert status["spec_maturity"]["next_experiment_readiness"] == "NOT_READY"

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
    assert status["human_review_status"] == "PENDING_SCENARIO_PROPOSALS"
    assert status["semantic_publication_allowed"] is False
    assert status["review_packet"] is None
    assert status["evaluation_reference_packet"] == (
        "validation/pkb001/human-review/HUMAN-REVIEW-DECISION-PACKET.zh-TW.md"
    )
    assert status["active_backlog_item"] == "PKB-BL-005"
    assert status["active_implementation_plan"] == "IMPLEMENTATION-PLAN.md#selected-work-generated-scenarios-and-individual-review"


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

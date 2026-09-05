import importlib.util
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MODULE_PATH = ROOT / "tooling/validation/build_pkb001_human_review_packet.py"
PACKET_PATH = ROOT / "validation/pkb001/human-review/human-review-decision-packet.json"
MARKDOWN_PATH = ROOT / "validation/pkb001/human-review/HUMAN-REVIEW-DECISION-PACKET.md"


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


def test_human_review_markdown_covers_every_item_without_claiming_approval():
    packet = json.loads(PACKET_PATH.read_text())
    markdown = MARKDOWN_PATH.read_text()

    assert "PENDING_PRODUCT_TEAM_REVIEW" in markdown
    assert "Semantic publication allowed: **false**" in markdown
    for item in packet["items"]:
        assert f"### {item['blind_id']} — {item['candidate_capability']}" in markdown
    assert "Product Team decision: **PENDING**" in markdown


def test_status_routes_next_action_to_the_human_review_packet():
    status = json.loads((ROOT / "STATUS.json").read_text())

    assert status["decision"] == "REVISE"
    assert status["human_review_status"] == "PENDING_PRODUCT_TEAM_REVIEW"
    assert status["semantic_publication_allowed"] is False
    assert status["human_review_packet"] == (
        "validation/pkb001/human-review/HUMAN-REVIEW-DECISION-PACKET.md"
    )
    assert status["next_action"] == "Complete Product Team human review decision packet"

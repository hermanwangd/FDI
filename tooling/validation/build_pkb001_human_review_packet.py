#!/usr/bin/env python3
"""Build the PKB-001 Product Team review packet from sealed evaluation inputs."""

import argparse
import hashlib
import json
from pathlib import Path


SOURCE_PATHS = {
    "blind_packet": "validation/pkb001/task6-blind-review/blind-review-packet.json",
    "reviewer_01": "validation/pkb001/task6-blind-review/judgment-workspaces/reviewer-01/judgment-template.json",
    "reviewer_02": "validation/pkb001/task6-blind-review/judgment-workspaces/reviewer-02/judgment-template.json",
    "pending_disagreements": "validation/pkb001/task7-evaluation/third-review-pending.json",
    "evaluation_report": "validation/pkb001/task7-evaluation/evaluation-report.json",
}


def _read_json(root, relative_path):
    return json.loads((root / relative_path).read_text())


def _sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def build_packet(root):
    source = {name: _read_json(root, path) for name, path in SOURCE_PATHS.items()}
    judgments = {
        reviewer: {row["blind_id"]: row for row in source[reviewer]["judgments"]}
        for reviewer in ("reviewer_01", "reviewer_02")
    }
    pending = {
        row["blind_id"]: row["reasons"]
        for row in source["pending_disagreements"]["items"]
    }

    items = []
    for candidate in source["blind_packet"]["items"]:
        blind_id = candidate["blind_id"]
        reviewer_summaries = []
        for reviewer in ("reviewer_01", "reviewer_02"):
            judgment = judgments[reviewer][blind_id]
            reviewer_summaries.append(
                {
                    "reviewer": reviewer.replace("_", "-"),
                    "action": judgment["review_action"],
                    "outcome": judgment["outcome"],
                    "suggested_name": judgment["suggested_name"],
                    "notes": judgment["reviewer_notes"],
                    "limitations": judgment["limitations"],
                    "unsupported_claims": judgment["unsupported_claims"],
                }
            )
        items.append(
            {
                "blind_id": blind_id,
                "candidate_capability": candidate["candidate_capability"],
                "candidate_basis": candidate["candidate_basis"],
                "confidence_score": candidate["confidence_score"],
                "component_references": [
                    component["reference"] for component in candidate["component_refs"]
                ],
                "needs_resolution": blind_id in pending,
                "resolution_reasons": pending.get(blind_id, []),
                "evaluator_judgments": reviewer_summaries,
                "product_team_decision": {
                    "action": None,
                    "approved_name": None,
                    "notes": None,
                },
            }
        )

    return {
        "schema_version": "pkb001.human-review-decision-packet.v1",
        "status": "PENDING_PRODUCT_TEAM_REVIEW",
        "current_prototype_decision": source["evaluation_report"]["decision"],
        "semantic_publication_allowed": False,
        "authority_statement": (
            "Evaluator judgments are advisory. Only the Product Team may decide "
            "Product meaning; completing this packet does not publish semantics."
        ),
        "allowed_product_team_actions": source["blind_packet"]["allowed_review_actions"],
        "source_digests": {
            path: _sha256(root / path) for path in SOURCE_PATHS.values()
        },
        "items": items,
        "final_product_team_decision": {
            "reviewer_name": None,
            "reviewed_at": None,
            "prototype_decision": None,
            "decision_rationale": None,
            "semantic_publication_approval": False,
        },
    }


def render_markdown(packet):
    lines = [
        "# PKB-001 Human Review Decision Packet",
        "",
        f"Status: **{packet['status']}**  ",
        f"Current prototype decision: **{packet['current_prototype_decision']}**  ",
        "Semantic publication allowed: **false**",
        "",
        "## Product Team instructions",
        "",
        packet["authority_statement"],
        "",
        "For each item, select one allowed action, provide the approved capability name when applicable, and record the rationale. Evaluator recommendations are evidence for review, not Product truth. A completed review does not by itself authorize semantic publication.",
        "",
        "Allowed actions: " + ", ".join(f"`{x}`" for x in packet["allowed_product_team_actions"]),
        "",
        f"Items requiring explicit disagreement resolution: **{sum(x['needs_resolution'] for x in packet['items'])}/15**",
        "",
        "## Item decisions",
        "",
    ]
    for item in packet["items"]:
        lines.extend(
            [
                f"### {item['blind_id']} — {item['candidate_capability']}",
                "",
                f"Candidate basis: {item['candidate_basis']}",
                "",
                f"Confidence: `{item['confidence_score']}`  ",
                "Resolution required: **" + ("YES" if item["needs_resolution"] else "NO") + "**  ",
                "Resolution reasons: " + (", ".join(item["resolution_reasons"]) or "none"),
                "",
            ]
        )
        for judgment in item["evaluator_judgments"]:
            suggested = judgment["suggested_name"] or "none"
            lines.extend(
                [
                    f"- {judgment['reviewer']}: `{judgment['action']}` / `{judgment['outcome']}`; suggested name: {suggested}",
                    f"  - Notes: {judgment['notes']}",
                    "  - Unsupported claims: " + ("; ".join(judgment["unsupported_claims"]) or "none"),
                ]
            )
        lines.extend(
            [
                "",
                "Product Team decision: **PENDING**",
                "",
                "- Action:",
                "- Approved name:",
                "- Rationale:",
                "",
            ]
        )
    lines.extend(
        [
            "## Final Product Team decision",
            "",
            "- Reviewer name:",
            "- Reviewed at:",
            "- Prototype decision (`GO`, `REVISE`, or `STOP`):",
            "- Decision rationale:",
            "- Semantic publication approval: **false** (requires a separate explicit action)",
            "",
        ]
    )
    return "\n".join(lines)


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    args = parser.parse_args(argv)
    output_dir = args.root / "validation/pkb001/human-review"
    output_dir.mkdir(parents=True, exist_ok=True)
    packet = build_packet(args.root)
    (output_dir / "human-review-decision-packet.json").write_text(
        json.dumps(packet, indent=2) + "\n"
    )
    (output_dir / "HUMAN-REVIEW-DECISION-PACKET.md").write_text(
        render_markdown(packet)
    )


if __name__ == "__main__":
    main()

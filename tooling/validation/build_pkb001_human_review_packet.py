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
    "sealed_key": "validation/pkb001/task6-blind-review/sealed-blind-key.json",
    "forward_run": "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json",
    "evaluator_gold": "validation/pkb001/evaluator/petclinic-818c413/gold-mappings.json",
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
    identities = {
        row["blind_id"]: row for row in source["sealed_key"]["items"]
    }
    forward_results = {
        row["capability_id"]: row for row in source["forward_run"]["capability_results"]
    }
    gold_mappings = {
        row["capability_id"]: row for row in source["evaluator_gold"]["mappings"]
    }
    evaluation_forward = source["evaluation_report"][
        "forward_expected_realization_comparison"
    ]
    evaluation_by_capability = {
        row["capability_id"]: row for row in evaluation_forward["by_capability"]
    }

    items = []
    for candidate in source["blind_packet"]["items"]:
        blind_id = candidate["blind_id"]
        identity = identities[blind_id]
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
        item = {
                "blind_id": blind_id,
                "source_arm": identity["source_arm"],
                "source_identifier": identity["source_identifier"],
                "candidate_capability": candidate["candidate_capability"],
                "candidate_basis": candidate["candidate_basis"],
                "confidence_score": candidate["confidence_score"],
                "component_references": [
                    component["reference"] for component in candidate["component_refs"]
                ],
                "needs_resolution": blind_id in pending,
                "resolution_reasons": pending.get(blind_id, []),
                "evaluator_judgments": reviewer_summaries,
                "proposal_only": identity["source_arm"] == "REVERSE",
                "forward_component_comparison": None,
                "product_team_decision": {
                    "action": None,
                    "approved_name": None,
                    "notes": None,
                },
            }
        if identity["source_arm"] == "FORWARD":
            capability_id = identity["source_identifier"]
            forward = forward_results[capability_id]
            gold = gold_mappings[capability_id]
            evaluated = evaluation_by_capability[capability_id]
            expected_ids = [row["graph_node_id"] for row in gold["expected_components"]]
            proposed_ids = [row["graph_node_id"] for row in forward["proposed_components"]]
            cited_ids = set(proposed_ids)
            cited_ids.update(row["graph_node_id"] for row in forward["evidence_refs"])
            missing_ids = [node_id for node_id in expected_ids if node_id not in cited_ids]
            item["forward_component_comparison"] = {
                "expected_components": [
                    {
                        "graph_node_id": row["graph_node_id"],
                        "source_path": row["source_path"],
                        "source_location": row["source_location"],
                    }
                    for row in gold["expected_components"]
                ],
                "proposed_components": [
                    {
                        "graph_node_id": row["graph_node_id"],
                        "source_location": row["source_location"],
                    }
                    for row in forward["proposed_components"]
                ],
                "supporting_evidence_node_ids": [
                    row["graph_node_id"] for row in forward["evidence_refs"]
                ],
                "missing_expected_node_ids": missing_ids,
                "expected_nodes_cited": evaluated["expected_graph_node_coverage"][
                    "expected_graph_nodes_cited"
                ],
                "expected_nodes": evaluated["expected_graph_node_coverage"][
                    "expected_graph_nodes"
                ],
                "exact_proposed_component_matches": evaluated[
                    "proposed_component_exact_graph_node_matches"
                ],
                "difference_classification": (
                    "MISSING_EVIDENCE"
                    if forward["outcome"] == "UNRESOLVED"
                    else "GRANULARITY_OR_IDENTIFIER_MISMATCH"
                ),
                "plain_language": (
                    "No component was proposed for the expected realization."
                    if forward["outcome"] == "UNRESOLVED"
                    else "The proposal found relevant files or nearby evidence nodes, "
                    "but its formal components do not exactly identify the expected nodes."
                ),
            }
        items.append(item)

    path_comparison = evaluation_forward["file_component_path_comparison"]
    node_coverage = evaluation_forward["expected_graph_node_coverage"]
    exact_comparison = evaluation_forward[
        "proposed_component_exact_graph_node_comparison"
    ]

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
        "forward_comparison": {
            "interpretation": (
                "The run generally found the correct code area, but did not precisely "
                "name the evaluator's expected realization nodes as proposed components."
            ),
            "expected_component_path_recall": path_comparison[
                "expected_component_path_recall"
            ],
            "proposed_component_path_precision": path_comparison[
                "proposed_component_path_precision"
            ],
            "expected_graph_node_coverage": {
                "cited": node_coverage["expected_graph_nodes_cited"],
                "expected": node_coverage["expected_graph_nodes"],
                "rate": node_coverage["expected_graph_node_coverage_rate"],
            },
            "exact_proposed_component_matches": {
                "matched": exact_comparison[
                    "proposed_component_exact_graph_node_matches"
                ],
                "expected": exact_comparison["expected_graph_nodes"],
                "rate": exact_comparison[
                    "proposed_component_exact_graph_node_recall"
                ],
            },
            "limits": evaluation_forward["comparison_limits"],
        },
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
        f"Status: **{packet['status']}**",
        f"Current prototype decision: **{packet['current_prototype_decision']}**",
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
        "## Forward comparison context",
        "",
        "- Expected component path recall: 23/24 (95.8%)",
        "- Proposed component path precision: 21/25 (84.0%)",
        "- Expected graph-node coverage across components and supporting evidence: 17/24 (70.8%)",
        "- Exact proposed-component graph-node matches: 0/24",
        "",
        "Plain language: the run generally found the correct code area, but its formal components did not precisely identify the evaluator's expected method/entity nodes. File-path overlap and supporting evidence are useful, but neither is an exact proposed-component match.",
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
                f"Confidence: `{item['confidence_score']}`",
                "Resolution required: **" + ("YES" if item["needs_resolution"] else "NO") + "**",
                "Resolution reasons: " + (", ".join(item["resolution_reasons"]) or "none"),
                "",
            ]
        )
        comparison = item["forward_component_comparison"]
        if comparison is None:
            lines.extend(
                [
                    "Reverse proposal-only: this capability hypothesis is advisory and has no Forward expected-component comparison.",
                    "",
                ]
            )
        else:
            expected = ", ".join(
                f"`{row['graph_node_id']}`" for row in comparison["expected_components"]
            )
            proposed = ", ".join(
                f"`{row['graph_node_id']}`" for row in comparison["proposed_components"]
            ) or "none"
            supporting = ", ".join(
                f"`{node_id}`" for node_id in comparison["supporting_evidence_node_ids"]
            ) or "none"
            missing = ", ".join(
                f"`{node_id}`" for node_id in comparison["missing_expected_node_ids"]
            ) or "none"
            lines.extend(
                [
                    f"Expected components: {expected}",
                    "",
                    f"Proposed components: {proposed}",
                    "",
                    f"Supporting evidence nodes: {supporting}",
                    "",
                    f"Missing expected nodes: {missing}",
                    "",
                    "Difference classification: "
                    f"`{comparison['difference_classification']}` — {comparison['plain_language']}",
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

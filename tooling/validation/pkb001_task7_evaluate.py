#!/usr/bin/env python3
"""Deterministic Task 7 evaluation for the completed PKB-001 blind review.

The evaluator validates both sealed judgment workspaces before it reads the
blind key. It then verifies every bound input, computes descriptive metrics,
and applies the pre-declared bounded decision rule. Observed values are never
treated as retroactive acceptance thresholds.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import statistics
from collections import Counter
from pathlib import Path
from typing import Any


TASK6 = Path("validation/pkb001/task6-blind-review")
PACKET = TASK6 / "blind-review-packet.json"
TASK6_MANIFEST = TASK6 / "manifest.json"
SEALED_KEY = TASK6 / "sealed-blind-key.json"
JUDGMENTS = (
    TASK6 / "judgment-workspaces/reviewer-01/judgment-template.json",
    TASK6 / "judgment-workspaces/reviewer-02/judgment-template.json",
)
FORWARD_RUN = Path(
    "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json"
)
FORWARD_MANIFEST = Path(
    "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-manifest.json"
)
FORWARD_WITNESS = Path(
    "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-witness.json"
)
REVERSE_RUN = Path(
    "validation/pkb001/reverse-task5-pkb001_reverse_run/capability-hypotheses.json"
)
REVERSE_MANIFEST = Path(
    "validation/pkb001/reverse-task5-pkb001_reverse_run/manifest.json"
)
REVERSE_WITNESS = Path(
    "validation/pkb001/reverse-task5-pkb001_reverse_run/provenance-witness.json"
)
GOLD = Path("validation/pkb001/evaluator/petclinic-818c413/gold-mappings.json")
GOLD_SEAL = Path(
    "validation/pkb001/evaluator/petclinic-818c413/ground-truth-seal.json"
)

ACTIONS = ("ACCEPT", "ADD_MISSING", "MERGE", "RENAME", "REJECT", "SPLIT")
OUTCOMES = ("DUPLICATE", "PARTIALLY_SUPPORTED", "SUPPORTED", "UNSUPPORTED")
EXPECTED_ITEMS = 15


class EvaluationError(ValueError):
    """Raised when a sealed input or evaluation invariant does not validate."""


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    try:
        with path.open("rb") as handle:
            for chunk in iter(lambda: handle.read(1024 * 1024), b""):
                digest.update(chunk)
    except OSError as error:
        raise EvaluationError(f"cannot read digest input {path}: {error}") from error
    return digest.hexdigest()


def load_json(path: Path) -> Any:
    try:
        with path.open(encoding="utf-8") as handle:
            return json.load(handle)
    except (OSError, json.JSONDecodeError) as error:
        raise EvaluationError(f"cannot read JSON input {path}: {error}") from error


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise EvaluationError(message)


def _complete_judgment(row: dict[str, Any]) -> bool:
    scores = ("evidence_validity", "usefulness", "precision")
    return (
        isinstance(row.get("blind_id"), str)
        and row.get("review_action") in ACTIONS
        and row.get("outcome") in OUTCOMES
        and all(
            isinstance(row.get(field), (int, float))
            and not isinstance(row.get(field), bool)
            and 0 <= row[field] <= 1
            for field in scores
        )
        and isinstance(row.get("unsupported_claims"), list)
        and all(isinstance(value, str) and value for value in row["unsupported_claims"])
        and isinstance(row.get("limitations"), list)
        and all(isinstance(value, str) and value for value in row["limitations"])
        and isinstance(row.get("reviewer_notes"), str)
        and bool(row["reviewer_notes"])
        and isinstance(row.get("active_review_seconds"), int)
        and not isinstance(row.get("active_review_seconds"), bool)
        and row["active_review_seconds"] >= 0
    )


def validate_pre_unblinding(root: Path) -> dict[str, Any]:
    """Validate reviewer completeness/isolation without opening the sealed key."""
    root = root.resolve()
    packet_path = root / PACKET
    packet = load_json(packet_path)
    packet_digest = sha256(packet_path)
    packet_ids = [item.get("blind_id") for item in packet.get("items", [])]
    _require(
        len(packet_ids) == EXPECTED_ITEMS
        and len(set(packet_ids)) == EXPECTED_ITEMS
        and all(isinstance(value, str) and value for value in packet_ids),
        "blind packet must contain 15 unique item IDs",
    )
    packet_text = json.dumps(packet, sort_keys=True)
    _require(
        "source_arm" not in packet_text and "source_identifier" not in packet_text,
        "blind packet exposes source identity before unblinding",
    )

    workspaces = []
    for relative in JUDGMENTS:
        path = root / relative
        packet_input = path.parent / "packet-input.json"
        workspace = load_json(path)
        rows = workspace.get("judgments")
        _require(
            isinstance(rows, list)
            and len(rows) == EXPECTED_ITEMS
            and all(_complete_judgment(row) for row in rows),
            f"{relative} must contain 15 complete judgments",
        )
        _require(
            [row["blind_id"] for row in rows] == packet_ids,
            f"{relative} judgment IDs do not match the packet",
        )
        _require(
            workspace.get("packet_sha256") == packet_digest,
            f"{relative} packet digest mismatch",
        )
        _require(
            sha256(packet_input) == packet_digest,
            f"{packet_input.relative_to(root)} digest does not match the sealed packet",
        )
        context = workspace.get("reviewer_context", {})
        _require(
            context.get("actor_type") == "NON_HUMAN"
            and context.get("authority") == "EVALUATOR_ONLY"
            and context.get("can_complete_product_team_review") is False,
            f"{relative} claims invalid reviewer authority",
        )
        isolation = workspace.get("reviewer_isolation", {})
        _require(
            isolation.get("ground_truth_accessible_from_packet_workspace") is False
            and isolation.get("other_workspace_future_judgments_accessible") is False
            and isolation.get("sealed_key_accessible") is False,
            f"{relative} does not preserve reviewer isolation",
        )
        workspaces.append((relative, workspace, sha256(path)))

    _require(
        workspaces[0][1].get("workspace_id") != workspaces[1][1].get("workspace_id"),
        "reviewer workspace identities must be independent",
    )
    return {
        "passed": True,
        "validated_before_sealed_key_read": True,
        "packet_sha256": packet_digest,
        "item_count": EXPECTED_ITEMS,
        "reviewers": [
            {
                "workspace_id": workspace["workspace_id"],
                "actor_type": "NON_HUMAN",
                "authority": "EVALUATOR_ONLY",
                "can_complete_product_team_review": False,
                "judgment_count": len(workspace["judgments"]),
                "judgment_sha256": digest,
            }
            for _, workspace, digest in workspaces
        ],
    }


def bounded_decision(
    *,
    integrity_passed: bool,
    thresholds_preregistered: bool,
    quality_passed: bool | None,
    human_review_completed: bool,
) -> str:
    if not integrity_passed:
        return "STOP"
    if not thresholds_preregistered or quality_passed is not True:
        return "REVISE"
    if not human_review_completed:
        return "REVISE"
    return "GO"


def _counts(rows: list[dict[str, Any]], field: str, vocabulary: tuple[str, ...]) -> dict[str, int]:
    observed = Counter(row[field] for row in rows)
    return {value: observed[value] for value in vocabulary}


def _rounded_mean(values: list[float]) -> float:
    return round(sum(values) / len(values), 10) if values else 0.0


def _arm_metrics(
    item_ids: list[str],
    rows_by_reviewer: dict[str, dict[str, dict[str, Any]]],
) -> dict[str, Any]:
    rows = [
        rows_by_reviewer[reviewer][blind_id]
        for blind_id in item_ids
        for reviewer in sorted(rows_by_reviewer)
    ]
    combined_times = [
        sum(rows_by_reviewer[reviewer][blind_id]["active_review_seconds"]
            for reviewer in sorted(rows_by_reviewer))
        for blind_id in item_ids
    ]
    q1, _, q3 = statistics.quantiles(combined_times, n=4, method="inclusive")
    unsupported_nonempty = sum(bool(row["unsupported_claims"]) for row in rows)
    return {
        "coverage": {
            "items_expected": len(item_ids),
            "items_judged_by_both": len(item_ids),
            "item_coverage": 1.0,
            "judgments_expected": len(item_ids) * len(rows_by_reviewer),
            "judgments_complete": len(rows),
            "judgment_coverage": 1.0,
        },
        "action_counts": _counts(rows, "review_action", ACTIONS),
        "outcome_counts": _counts(rows, "outcome", OUTCOMES),
        "evidence_validity_mean": _rounded_mean([row["evidence_validity"] for row in rows]),
        "usefulness_mean": _rounded_mean([row["usefulness"] for row in rows]),
        "precision_mean": _rounded_mean([row["precision"] for row in rows]),
        "unsupported_claim_count": sum(len(row["unsupported_claims"]) for row in rows),
        "judgments_with_unsupported_claims": unsupported_nonempty,
        "unsupported_claim_judgment_rate": round(unsupported_nonempty / len(rows), 10),
        "review_time_seconds": {
            "total": sum(row["active_review_seconds"] for row in rows),
            "mean_per_judgment": _rounded_mean(
                [row["active_review_seconds"] for row in rows]
            ),
            "median_per_judgment": statistics.median(
                row["active_review_seconds"] for row in rows
            ),
            "median_combined_per_item": statistics.median(combined_times),
            "combined_per_item_iqr": [q1, q3],
        },
    }


def _agreement(
    item_ids: list[str],
    rows_by_reviewer: dict[str, dict[str, dict[str, Any]]],
) -> dict[str, Any]:
    reviewers = sorted(rows_by_reviewer)
    first, second = (rows_by_reviewer[value] for value in reviewers)
    action_disagreements = [
        blind_id for blind_id in item_ids
        if first[blind_id]["review_action"] != second[blind_id]["review_action"]
    ]
    outcome_disagreements = [
        blind_id for blind_id in item_ids
        if first[blind_id]["outcome"] != second[blind_id]["outcome"]
    ]
    exact = [
        blind_id for blind_id in item_ids
        if first[blind_id]["review_action"] == second[blind_id]["review_action"]
        and first[blind_id]["outcome"] == second[blind_id]["outcome"]
    ]
    total = len(item_ids)
    return {
        "reviewers": reviewers,
        "item_count": total,
        "action_agreement_count": total - len(action_disagreements),
        "action_agreement_rate": round((total - len(action_disagreements)) / total, 10),
        "action_disagreement_ids": action_disagreements,
        "outcome_agreement_count": total - len(outcome_disagreements),
        "outcome_agreement_rate": round((total - len(outcome_disagreements)) / total, 10),
        "outcome_disagreement_ids": outcome_disagreements,
        "exact_action_and_outcome_agreement_count": len(exact),
        "exact_action_and_outcome_agreement_rate": round(len(exact) / total, 10),
        "exact_action_and_outcome_agreement_ids": exact,
    }


def _source_path(component: dict[str, Any]) -> str:
    if "source_path" in component:
        return component["source_path"]
    return component["source_location"].rsplit(":L", 1)[0]


def _forward_comparison(forward: dict[str, Any], gold: dict[str, Any]) -> dict[str, Any]:
    expected = {row["capability_id"]: row for row in gold["mappings"]}
    details = []
    totals = Counter()
    for result in forward["capability_results"]:
        capability_id = result["capability_id"]
        _require(capability_id in expected, f"forward capability missing from gold: {capability_id}")
        proposed = result["proposed_components"]
        wanted = expected[capability_id]["expected_components"]
        proposed_ids = {row["graph_node_id"] for row in proposed}
        expected_ids = {row["graph_node_id"] for row in wanted}
        proposed_paths = [_source_path(row) for row in proposed]
        expected_paths = {_source_path(row) for row in wanted}
        exact = len(proposed_ids & expected_ids)
        expected_path_matches = sum(_source_path(row) in set(proposed_paths) for row in wanted)
        proposed_path_matches = sum(path in expected_paths for path in proposed_paths)
        totals.update(
            expected_components=len(wanted),
            exact_graph_node_matches=exact,
            expected_path_matches=expected_path_matches,
            proposed_components=len(proposed),
            proposed_path_matches=proposed_path_matches,
        )
        details.append({
            "capability_id": capability_id,
            "run_outcome": result["outcome"],
            "expected_component_count": len(wanted),
            "proposed_component_count": len(proposed),
            "exact_graph_node_match_count": exact,
            "expected_components_with_proposed_source_path": expected_path_matches,
            "proposed_components_on_expected_source_path": proposed_path_matches,
        })
    _require(set(expected) == {row["capability_id"] for row in forward["capability_results"]},
             "forward/gold capability sets do not match")
    return {
        "authority": "EVALUATOR_ONLY_COMPARISON_NOT_PRODUCT_TRUTH",
        "capabilities_expected": len(expected),
        "mapping_proposals": sum(
            row["outcome"] == "MAPPING_PROPOSAL" for row in forward["capability_results"]
        ),
        "unresolved": sum(
            row["outcome"] == "UNRESOLVED" for row in forward["capability_results"]
        ),
        "expected_components": totals["expected_components"],
        "exact_graph_node_matches": totals["exact_graph_node_matches"],
        "exact_graph_node_recall": round(
            totals["exact_graph_node_matches"] / totals["expected_components"], 10
        ),
        "expected_components_with_proposed_source_path": totals["expected_path_matches"],
        "expected_component_path_recall": round(
            totals["expected_path_matches"] / totals["expected_components"], 10
        ),
        "proposed_components": totals["proposed_components"],
        "proposed_components_on_expected_source_path": totals["proposed_path_matches"],
        "proposed_component_path_precision": round(
            totals["proposed_path_matches"] / totals["proposed_components"], 10
        ),
        "comparison_limit": "PATH_LEVEL_OVERLAP_IS_NOT_AN_EXACT_GRAPH_NODE_MATCH",
        "by_capability": details,
    }


def _validate_bound_inputs(root: Path, pre: dict[str, Any]) -> dict[str, Any]:
    task6_manifest = load_json(root / TASK6_MANIFEST)
    _require(
        sha256(root / PACKET) == task6_manifest.get("packet_sha256") == pre["packet_sha256"],
        "packet digest does not match Task 6 manifest",
    )
    _require(
        sha256(root / SEALED_KEY) == task6_manifest.get("sealed_key_sha256"),
        "sealed key digest does not match Task 6 manifest",
    )
    key = load_json(root / SEALED_KEY)
    _require(
        key.get("sealed_packet_sha256") == pre["packet_sha256"],
        "sealed key packet digest mismatch",
    )

    task6_inputs = {row["path"]: row["sha256"] for row in task6_manifest["input_digests"]}
    for relative in (
        FORWARD_RUN, FORWARD_MANIFEST, FORWARD_WITNESS,
        REVERSE_RUN, REVERSE_MANIFEST, REVERSE_WITNESS,
    ):
        _require(
            task6_inputs.get(str(relative)) == sha256(root / relative),
            f"source run digest mismatch: {relative}",
        )

    seal = load_json(root / GOLD_SEAL)
    _require(
        seal.get("gold_sha256") == sha256(root / GOLD),
        "evaluator gold digest does not match ground-truth seal",
    )
    _require(
        seal.get("status") == "SEALED"
        and seal.get("isolation_status") == "VERIFIED"
        and seal.get("human_review_completed") is False,
        "evaluator ground-truth seal invariant failed",
    )
    bindings = task6_manifest["source_bindings"]
    _require(
        seal.get("source_commit_sha") == bindings["shared_source_commit_sha"]
        and seal.get("graph_sha256") == bindings["shared_graph_sha256"],
        "evaluator gold source binding mismatch",
    )
    return {"task6_manifest": task6_manifest, "key": key, "seal": seal}


def evaluate_repository(root: Path) -> dict[str, Any]:
    root = root.resolve()
    pre = validate_pre_unblinding(root)
    bound = _validate_bound_inputs(root, pre)
    packet = load_json(root / PACKET)
    forward = load_json(root / FORWARD_RUN)
    reverse = load_json(root / REVERSE_RUN)
    gold = load_json(root / GOLD)

    key_items = bound["key"].get("items", [])
    packet_ids = [item["blind_id"] for item in packet["items"]]
    _require(
        len(key_items) == EXPECTED_ITEMS
        and [item.get("blind_id") for item in key_items] == packet_ids,
        "sealed key does not account for every packet item",
    )
    forward_ids = {row["capability_id"] for row in forward["capability_results"]}
    reverse_ids = {row["proposal_id"] for row in reverse["hypotheses"]}
    _require(
        {row["source_identifier"] for row in key_items if row["source_arm"] == "FORWARD"}
        == forward_ids
        and {row["source_identifier"] for row in key_items if row["source_arm"] == "REVERSE"}
        == reverse_ids,
        "sealed key source identifiers do not match the bound runs",
    )

    workspaces = [load_json(root / path) for path in JUDGMENTS]
    rows_by_reviewer = {
        workspace["workspace_id"]: {
            row["blind_id"]: row for row in workspace["judgments"]
        }
        for workspace in workspaces
    }
    key_by_id = {row["blind_id"]: row for row in key_items}
    arm_ids = {
        arm: [blind_id for blind_id in packet_ids if key_by_id[blind_id]["source_arm"] == arm]
        for arm in ("FORWARD", "REVERSE")
    }
    agreement = _agreement(packet_ids, rows_by_reviewer)
    disagreement_ids = sorted(set(
        agreement["action_disagreement_ids"] + agreement["outcome_disagreement_ids"]
    ))
    pending_items = []
    for blind_id in disagreement_ids:
        reasons = []
        if blind_id in agreement["action_disagreement_ids"]:
            reasons.append("ACTION_DISAGREEMENT")
        if blind_id in agreement["outcome_disagreement_ids"]:
            reasons.append("OUTCOME_DISAGREEMENT")
        pending_items.append({
            "blind_id": blind_id,
            "reasons": reasons,
        })

    reverse_labels = {row["proposal_id"]: row["label"] for row in reverse["hypotheses"]}
    reverse_results = []
    for blind_id in arm_ids["REVERSE"]:
        source_id = key_by_id[blind_id]["source_identifier"]
        reverse_results.append({
            "blind_id": blind_id,
            "source_identifier": source_id,
            "label": reverse_labels[source_id],
            "reviewers": [
                {
                    "workspace_id": reviewer,
                    "review_action": rows_by_reviewer[reviewer][blind_id]["review_action"],
                    "outcome": rows_by_reviewer[reviewer][blind_id]["outcome"],
                    "evidence_validity": rows_by_reviewer[reviewer][blind_id]["evidence_validity"],
                    "usefulness": rows_by_reviewer[reviewer][blind_id]["usefulness"],
                    "precision": rows_by_reviewer[reviewer][blind_id]["precision"],
                }
                for reviewer in sorted(rows_by_reviewer)
            ],
            "third_review_status": (
                "PENDING" if blind_id in disagreement_ids else "NOT_REQUIRED"
            ),
        })

    digest_paths = (
        PACKET, SEALED_KEY, *JUDGMENTS,
        FORWARD_RUN, FORWARD_MANIFEST, FORWARD_WITNESS,
        REVERSE_RUN, REVERSE_MANIFEST, REVERSE_WITNESS,
        GOLD, GOLD_SEAL,
    )
    decision = bounded_decision(
        integrity_passed=True,
        thresholds_preregistered=False,
        quality_passed=None,
        human_review_completed=False,
    )
    return {
        "schema_version": "pkb001.task7.evaluation-report.v1",
        "report_id": "pkb001-task7-petclinic-818c413-v1",
        "decision": decision,
        "decision_scope": "BOUNDED_PROTOTYPE_DECISION_NO_SEMANTIC_PUBLICATION",
        "pre_unblinding_validation": pre,
        "bindings": {
            "source_commit_sha": bound["task6_manifest"]["source_bindings"]["shared_source_commit_sha"],
            "graph_sha256": bound["task6_manifest"]["source_bindings"]["shared_graph_sha256"],
            "reverse_delivery_history_sha256": bound["task6_manifest"]["source_bindings"]["reverse_delivery_history_sha256"],
            "artifacts": {str(path): sha256(root / path) for path in digest_paths},
        },
        "integrity": {
            "status": "PASSED",
            "snapshot_binding": "PASSED",
            "reviewer_isolation": "ATTESTED_AND_CONTRACT_VALIDATED",
            "evidence_integrity": "PASSED",
            "product_truth_boundary": "PASSED",
            "hard_stop_violations": [],
        },
        "metrics": {
            "by_arm": {
                arm: {
                    **_arm_metrics(ids, rows_by_reviewer),
                    "agreement": _agreement(ids, rows_by_reviewer),
                }
                for arm, ids in arm_ids.items()
            },
            "overall": _arm_metrics(packet_ids, rows_by_reviewer),
            "interpretation": "DESCRIPTIVE_ONLY_NO_RETROACTIVE_ACCEPTANCE_GATE",
        },
        "reviewer_agreement": agreement,
        "pending_third_review": {
            "status": "PENDING_INDEPENDENT_THIRD_REVIEW",
            "basis": "FROZEN_PROTOCOL_REQUIRES_THIRD_REVIEW_WHEN_REVIEWERS_DISAGREE",
            "item_count": len(pending_items),
            "items": pending_items,
        },
        "forward_expected_realization_comparison": _forward_comparison(forward, gold),
        "reverse_proposal_review_results": reverse_results,
        "thresholds": {
            "preregistered_before_execution": False,
            "observed_metrics_used_as_acceptance_thresholds": False,
            "defect": "NUMERIC_ACCEPTANCE_THRESHOLDS_NOT_FROZEN_BEFORE_GENERATION_AND_JUDGMENT",
            "effect": "GO_IS_NOT_SUPPORTABLE_FROM_THIS_RUN",
        },
        "human_product_team_review": {
            "status": "PENDING",
            "completed_by_non_human_reviewers": False,
            "semantic_publication_allowed": False,
            "authority": "PRODUCT_TEAM_ONLY",
        },
        "next_experiment_requirements": [
            "Pre-register numeric acceptance thresholds before generation and judgment.",
            "Add UI/template evidence to Graphify input or narrow capability descriptions to indexed evidence.",
            "Repeat the comparison with real human Product Team reviewers.",
            "Preserve the exact source revision and delivery-history cutoff.",
        ],
        "proof_limits": [
            "Reviewer isolation is supported by distinct workspaces and recorded non-access attestations, not cryptographic proof of model context.",
            "Path-level expected-realization overlap is reported separately and is not relabeled as exact graph-node matching.",
            "Non-human evaluator judgments do not establish Product meaning or permit semantic publication.",
        ],
    }


def build_third_review_packet(report: dict[str, Any]) -> dict[str, Any]:
    pending = report["pending_third_review"]
    return {
        "schema_version": "pkb001.task7.third-review-pending.v1",
        "status": pending["status"],
        "packet_sha256": report["pre_unblinding_validation"]["packet_sha256"],
        "reviewer_context_required": {
            "actor_type": "NON_HUMAN_OR_HUMAN_EVALUATOR",
            "authority": "EVALUATOR_ONLY",
            "independent_from_reviewer_01_and_reviewer_02": True,
            "can_complete_product_team_review": False,
        },
        "item_count": pending["item_count"],
        "items": pending["items"],
        "instruction": "Record an independent third judgment; do not infer or publish Product truth.",
    }


def _write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--report", type=Path)
    parser.add_argument("--pending", type=Path)
    args = parser.parse_args()
    report = evaluate_repository(args.root)
    if args.report:
        _write_json(args.report, report)
    if args.pending:
        _write_json(args.pending, build_third_review_packet(report))
    print(json.dumps(report, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

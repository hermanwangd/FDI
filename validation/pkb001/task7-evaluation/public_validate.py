#!/usr/bin/env python3
"""Public-seam validator for the deterministic PKB-001 Task 7 evaluation."""

import json
import sys
from pathlib import Path


RUN_DIR = Path("validation/pkb001/task7-evaluation")
REPORT = RUN_DIR / "evaluation-report.json"
PENDING = RUN_DIR / "third-review-pending.json"


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def validate(root):
    sys.path.insert(0, str(root))
    from tooling.validation.pkb001_task7_evaluate import (
        build_third_review_packet,
        evaluate_repository,
    )

    persisted = load(root / REPORT)
    pending = load(root / PENDING)
    fresh = evaluate_repository(root)
    checks = []

    def check(name, condition, detail):
        checks.append({"name": name, "passed": bool(condition), "detail": detail})

    check(
        "deterministic_evaluation",
        fresh == persisted,
        "Fresh evaluation exactly matches the committed report",
    )
    check(
        "pre_unblind_order",
        persisted["pre_unblinding_validation"]["passed"] is True
        and persisted["pre_unblinding_validation"]["validated_before_sealed_key_read"] is True,
        "Both complete evaluator-only workspaces validated before key access",
    )
    check(
        "digest_bindings",
        len(persisted["bindings"]["artifacts"]) == 12
        and persisted["integrity"]["status"] == "PASSED",
        "Packet, key, judgments, runs, gold, and seal are digest-bound",
    )
    check(
        "metric_surface",
        set(persisted["metrics"]["by_arm"]) == {"FORWARD", "REVERSE"}
        and persisted["metrics"]["overall"]["coverage"]["items_judged_by_both"] == 15
        and len(persisted["reverse_proposal_review_results"]) == 5,
        "Per-arm and overall metrics cover all 15 items and five reverse proposals",
    )
    check(
        "disagreement_accounting",
        persisted["reviewer_agreement"]["action_disagreement_ids"]
        == ["BR-007", "BR-011", "BR-015"]
        and persisted["reviewer_agreement"]["outcome_disagreement_ids"]
        == ["BR-002", "BR-004", "BR-005", "BR-006", "BR-008", "BR-012", "BR-013", "BR-014"]
        and pending == build_third_review_packet(persisted)
        and all("third_judgment" not in row for row in pending["items"]),
        "All action/outcome disagreements are listed without a fabricated third judgment",
    )
    check(
        "forward_gold_comparison_limit",
        persisted["forward_expected_realization_comparison"]["authority"]
        == "EVALUATOR_ONLY_COMPARISON_NOT_PRODUCT_TRUTH"
        and persisted["forward_expected_realization_comparison"][
            "file_component_path_comparison"
        ]["expected_component_references"] == 24
        and persisted["forward_expected_realization_comparison"][
            "expected_graph_node_coverage"
        ]["expected_graph_nodes_cited"] == 17
        and persisted["forward_expected_realization_comparison"][
            "proposed_component_exact_graph_node_comparison"
        ]["proposed_component_exact_graph_node_matches"] == 0
        and persisted["forward_expected_realization_comparison"]["comparison_limits"]
        == [
            "PATH_LEVEL_OVERLAP_IS_NOT_AN_EXACT_GRAPH_NODE_MATCH",
            "EVIDENCE_CITATION_COVERAGE_IS_NOT_A_PROPOSED_COMPONENT_MATCH",
        ],
        "File-path, expected-node citation, and exact proposed-node metrics remain separate",
    )
    check(
        "task6_blinding_claim_boundary",
        any(
            "ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT" in limit
            for limit in persisted["proof_limits"]
        ),
        "Evaluation discloses deterministic label/order blinding without content anonymity",
    )
    check(
        "bounded_decision",
        persisted["decision"] == "REVISE"
        and persisted["thresholds"]["preregistered_before_execution"] is False
        and persisted["thresholds"]["observed_metrics_used_as_acceptance_thresholds"] is False,
        "Missing preregistration prevents GO without inventing a threshold",
    )
    check(
        "human_authority_pending",
        persisted["human_product_team_review"]["status"] == "PENDING"
        and persisted["human_product_team_review"]["semantic_publication_allowed"] is False,
        "Non-human evaluation does not complete Product Team review or publish semantics",
    )
    return {
        "schema_version": "pkb001.task7.public-validation-report.v1",
        "validation_scope": "DETERMINISTIC_EVALUATION_PUBLIC_SEAM",
        "passed": all(row["passed"] for row in checks),
        "checks": checks,
    }


def main():
    arguments = [value for value in sys.argv[1:] if value != "--write-report"]
    root = Path(arguments[0]).resolve() if arguments else Path.cwd().resolve()
    report = validate(root)
    rendered = json.dumps(report, indent=2, sort_keys=True) + "\n"
    if "--write-report" in sys.argv[1:]:
        (root / RUN_DIR / "public-validation-report.json").write_text(
            rendered, encoding="utf-8"
        )
    print(rendered, end="")
    return 0 if report["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())

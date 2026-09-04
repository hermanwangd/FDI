import importlib.util
import json
import shutil
import subprocess
import sys
from pathlib import Path

import pytest


ROOT = Path(__file__).resolve().parents[1]
MODULE_PATH = ROOT / "tooling/validation/pkb001_task7_evaluate.py"
TASK6 = Path("validation/pkb001/task6-blind-review")


def load_evaluator():
    assert MODULE_PATH.is_file(), "Task 7 evaluator tooling is missing"
    spec = importlib.util.spec_from_file_location("pkb001_task7_evaluate", MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def copied_evaluation_root(tmp_path):
    paths = [
        TASK6,
        Path("validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json"),
        Path("validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-manifest.json"),
        Path("validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-witness.json"),
        Path("validation/pkb001/reverse-task5-pkb001_reverse_run/capability-hypotheses.json"),
        Path("validation/pkb001/reverse-task5-pkb001_reverse_run/manifest.json"),
        Path("validation/pkb001/reverse-task5-pkb001_reverse_run/provenance-witness.json"),
        Path("validation/pkb001/evaluator/petclinic-818c413/gold-mappings.json"),
        Path("validation/pkb001/evaluator/petclinic-818c413/ground-truth-seal.json"),
    ]
    for relative in paths:
        source = ROOT / relative
        target = tmp_path / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        if source.is_dir():
            shutil.copytree(source, target, dirs_exist_ok=True)
        else:
            shutil.copy2(source, target)
    return tmp_path


def test_task7_report_is_deterministic_bounded_and_complete():
    evaluator = load_evaluator()
    report = evaluator.evaluate_repository(ROOT)

    assert report == evaluator.evaluate_repository(ROOT)
    assert report["pre_unblinding_validation"]["passed"] is True
    assert report["decision"] == "REVISE"
    assert report["thresholds"]["preregistered_before_execution"] is False
    assert report["thresholds"]["observed_metrics_used_as_acceptance_thresholds"] is False
    assert report["human_product_team_review"]["status"] == "PENDING"
    assert report["human_product_team_review"]["semantic_publication_allowed"] is False

    overall = report["metrics"]["overall"]
    assert overall["coverage"] == {
        "items_expected": 15,
        "items_judged_by_both": 15,
        "item_coverage": 1.0,
        "judgments_expected": 30,
        "judgments_complete": 30,
        "judgment_coverage": 1.0,
    }
    assert overall["action_counts"] == {
        "ACCEPT": 17, "ADD_MISSING": 2, "MERGE": 2,
        "RENAME": 5, "REJECT": 0, "SPLIT": 4,
    }
    assert overall["outcome_counts"] == {
        "DUPLICATE": 2, "PARTIALLY_SUPPORTED": 20,
        "SUPPORTED": 8, "UNSUPPORTED": 0,
    }
    assert overall["evidence_validity_mean"] == pytest.approx(0.8946666667)
    assert overall["usefulness_mean"] == pytest.approx(0.8793333333)
    assert overall["precision_mean"] == pytest.approx(0.8383333333)
    assert overall["unsupported_claim_judgment_rate"] == pytest.approx(29 / 30)
    assert overall["review_time_seconds"]["total"] == 1358
    assert overall["review_time_seconds"]["median_combined_per_item"] == 80


def test_task7_reports_exact_agreement_and_pending_third_review():
    evaluator = load_evaluator()
    report = evaluator.evaluate_repository(ROOT)
    agreement = report["reviewer_agreement"]

    assert agreement["action_agreement_count"] == 12
    assert agreement["outcome_agreement_count"] == 7
    assert agreement["exact_action_and_outcome_agreement_count"] == 4
    assert agreement["action_disagreement_ids"] == ["BR-007", "BR-011", "BR-015"]
    assert agreement["outcome_disagreement_ids"] == [
        "BR-002", "BR-004", "BR-005", "BR-006",
        "BR-008", "BR-012", "BR-013", "BR-014",
    ]
    pending = evaluator.build_third_review_packet(report)
    assert pending["status"] == "PENDING_INDEPENDENT_THIRD_REVIEW"
    assert pending["item_count"] == 11
    assert [item["blind_id"] for item in pending["items"]] == [
        "BR-002", "BR-004", "BR-005", "BR-006", "BR-007", "BR-008",
        "BR-011", "BR-012", "BR-013", "BR-014", "BR-015",
    ]
    assert all("third_judgment" not in item for item in pending["items"])


def test_task7_forward_gold_comparison_and_reverse_results_are_transparent():
    evaluator = load_evaluator()
    report = evaluator.evaluate_repository(ROOT)
    forward = report["forward_expected_realization_comparison"]

    assert forward["capabilities_expected"] == 10
    assert forward["mapping_proposals"] == 9
    assert forward["unresolved"] == 1
    path_metrics = forward["file_component_path_comparison"]
    assert path_metrics["expected_component_references"] == 24
    assert path_metrics["expected_component_references_with_proposed_source_path"] == 23
    assert path_metrics["expected_component_path_recall"] == pytest.approx(23 / 24)
    assert path_metrics["proposed_component_references"] == 25
    assert path_metrics["proposed_component_references_on_expected_source_path"] == 21
    assert path_metrics["proposed_component_path_precision"] == pytest.approx(21 / 25)
    assert path_metrics["granularity"] == "FILE_COMPONENT_REFERENCE_AT_SOURCE_PATH"
    node_coverage = forward["expected_graph_node_coverage"]
    assert node_coverage == {
        "expected_graph_nodes": 24,
        "expected_graph_nodes_cited": 17,
        "expected_graph_node_coverage_rate": pytest.approx(17 / 24),
        "proposal_citation_scope": "PROPOSED_COMPONENTS_PLUS_EVIDENCE_REFS",
        "granularity": "GRAPH_NODE_ID",
    }
    assert forward["proposed_component_exact_graph_node_comparison"] == {
        "expected_graph_nodes": 24,
        "proposed_component_exact_graph_node_matches": 0,
        "proposed_component_exact_graph_node_recall": 0.0,
    }
    assert forward["comparison_limits"] == [
        "PATH_LEVEL_OVERLAP_IS_NOT_AN_EXACT_GRAPH_NODE_MATCH",
        "EVIDENCE_CITATION_COVERAGE_IS_NOT_A_PROPOSED_COMPONENT_MATCH",
    ]
    assert len(report["reverse_proposal_review_results"]) == 5
    assert {item["source_identifier"] for item in report["reverse_proposal_review_results"]} == {
        "PKS2-HYP-001", "PKS2-HYP-002", "PKS2-HYP-003",
        "PKS2-HYP-004", "PKS2-HYP-005",
    }


def test_task7_validates_judgments_before_unblinding(tmp_path):
    evaluator = load_evaluator()
    root = copied_evaluation_root(tmp_path)
    judgment = root / TASK6 / "judgment-workspaces/reviewer-01/judgment-template.json"
    payload = json.loads(judgment.read_text())
    payload["judgments"].pop()
    judgment.write_text(json.dumps(payload))
    (root / TASK6 / "sealed-blind-key.json").write_text("not valid JSON")

    report = evaluator.evaluate_repository(root)

    assert report["decision"] == "STOP"
    assert report["failure_stage"] == "PRE_UNBLINDING_VALIDATION"
    assert report["unblinding_performed"] is False
    assert report["metrics_computed"] is False
    assert report["semantic_publication_allowed"] is False
    assert "15 complete judgments" in report["stop_reasons"][0]["detail"]
    assert "metrics" not in report


@pytest.mark.parametrize(
    "mutation", ["packet", "reviewer_packet", "gold", "forward_run"]
)
def test_task7_rejects_bound_input_mutations(tmp_path, mutation):
    evaluator = load_evaluator()
    root = copied_evaluation_root(tmp_path)
    targets = {
        "packet": root / TASK6 / "blind-review-packet.json",
        "reviewer_packet": root / TASK6 / (
            "judgment-workspaces/reviewer-01/packet-input.json"
        ),
        "gold": root / "validation/pkb001/evaluator/petclinic-818c413/gold-mappings.json",
        "forward_run": root / "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json",
    }
    targets[mutation].write_bytes(targets[mutation].read_bytes() + b"\n")

    report = evaluator.evaluate_repository(root)

    assert report["decision"] == "STOP"
    assert report["failure_stage"] in {
        "PRE_UNBLINDING_VALIDATION", "BOUND_INPUT_VALIDATION",
    }
    assert report["unblinding_performed"] is False
    assert report["metrics_computed"] is False
    assert report["semantic_publication_allowed"] is False
    assert "digest" in report["stop_reasons"][0]["detail"]
    assert "metrics" not in report


def test_task7_cli_persists_stop_and_returns_documented_nonzero_exit(tmp_path):
    root = copied_evaluation_root(tmp_path / "mutated-root")
    packet = root / TASK6 / "blind-review-packet.json"
    packet.write_bytes(packet.read_bytes() + b"\n")
    report_path = tmp_path / "stop-report.json"
    pending_path = tmp_path / "must-not-exist.json"

    completed = subprocess.run(
        [
            sys.executable, str(MODULE_PATH), "--root", str(root),
            "--report", str(report_path), "--pending", str(pending_path),
        ],
        check=False,
        text=True,
        capture_output=True,
    )

    assert completed.returncode == 2
    persisted = json.loads(report_path.read_text())
    assert persisted["decision"] == "STOP"
    assert persisted["documented_exit_code"] == 2
    assert persisted["unblinding_performed"] is False
    assert persisted["metrics_computed"] is False
    assert persisted["semantic_publication_allowed"] is False
    assert not pending_path.exists()
    assert json.loads(completed.stdout) == persisted


def test_task7_decision_boundary_never_backfits_observed_metrics():
    evaluator = load_evaluator()
    assert evaluator.bounded_decision(
        integrity_passed=True,
        thresholds_preregistered=False,
        quality_passed=None,
        human_review_completed=False,
    ) == "REVISE"
    assert evaluator.bounded_decision(
        integrity_passed=False,
        thresholds_preregistered=False,
        quality_passed=None,
        human_review_completed=False,
    ) == "STOP"
    assert evaluator.bounded_decision(
        integrity_passed=True,
        thresholds_preregistered=True,
        quality_passed=True,
        human_review_completed=True,
    ) == "GO"


def test_task7_committed_artifacts_pass_public_validation():
    validator = ROOT / "validation/pkb001/task7-evaluation/public_validate.py"
    completed = subprocess.run(
        [sys.executable, str(validator), str(ROOT)],
        check=False,
        text=True,
        capture_output=True,
    )
    assert completed.returncode == 0, completed.stdout + completed.stderr
    assert json.loads(completed.stdout)["passed"] is True


def test_task5_deferred_audit_minors_have_committed_evidence():
    transcript = (
        ROOT
        / "validation/pkb001/reverse-task5-pkb001_reverse_run/public-validation-transcript.txt"
    ).read_text()
    validator = (
        ROOT / "validation/pkb001/reverse-task5-pkb001_reverse_run/public_validate.py"
    ).read_text()
    assert "Fresh validator exit: 0" in transcript
    assert "Byte comparison exit: 0" in transcript
    assert "24/24 checks passed" in transcript
    assert "forbidden_inputs_not_accessed" in validator
    assert "Both records attest false" in validator
    ledger = (ROOT / ".superpowers/sdd/IMPLEMENTATION-PLAN/progress.md").read_text()
    task5_report = (
        ROOT / ".superpowers/sdd/IMPLEMENTATION-PLAN/task-5-report.md"
    ).read_text()
    assert "Task 5: auditability minors resolved" in ledger
    assert "Auditability minors resolved" in task5_report


def test_task7_markdown_reports_unsupported_claim_decimal_rates():
    report = (ROOT / ".superpowers/sdd/IMPLEMENTATION-PLAN/task-7-report.md").read_text()

    assert "19/20 (0.9500)" in report
    assert "10/10 (1.0000)" in report
    assert "29/30 (0.9667)" in report

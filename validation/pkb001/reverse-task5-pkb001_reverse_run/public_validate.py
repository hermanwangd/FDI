#!/usr/bin/env python3
"""Public-seam validation for the isolated PKB-001 PK-S2 reverse run.

This validator checks provenance, reference resolution, cutoff, convergence,
digests, proposal-only authority, and run isolation. It intentionally does not
read or judge Product Semantics, evaluator truth, forward output, or semantic
correctness.
"""

from __future__ import annotations

import hashlib
import json
import sys
from datetime import datetime
from pathlib import Path
from typing import Any


EXPECTED_SOURCE_COMMIT = "818c4136ea971c21674525f9053de0d9c7ad8cfe"
EXPECTED_GRAPH_SHA = "e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e"
EXPECTED_HISTORY_SHA = "87b70439f69bb82cee0139b65108ecce6044aff1a4d1b5ce6324933acbeab2d1"
EXPECTED_CUTOFF = "2026-08-26T10:57:54Z"
RUN_DIR = Path("validation/pkb001/reverse-task5-pkb001_reverse_run")
ARTIFACT = RUN_DIR / "capability-hypotheses.json"
MANIFEST = RUN_DIR / "manifest.json"
WITNESS = RUN_DIR / "provenance-witness.json"
VALIDATOR = RUN_DIR / "public_validate.py"
REPORT = RUN_DIR / "public-validation-report.json"

ALLOWED_INPUTS = {
    ".superpowers/sdd/IMPLEMENTATION-PLAN/task-5-brief.md",
    "skills/pkb001/pk-s2-capability-hypothesis/SKILL.md",
    "validation/pkb001/artifacts/petclinic-graph-818c413.json",
    "validation/pkb001/runtime/graphify-petclinic-live-evidence.json",
    "validation/pkb001/datasets/petclinic-delivery-history.json",
    "validation/pkb001/reports/phase0-readiness.json",
}
ALLOWED_OUTPUTS = {str(ARTIFACT), str(MANIFEST), str(WITNESS), str(VALIDATOR), str(REPORT)}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def load_json(path: Path) -> Any:
    with path.open(encoding="utf-8") as handle:
        return json.load(handle)


def instant(value: str) -> datetime:
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def validate(root: Path) -> dict[str, Any]:
    checks: list[dict[str, Any]] = []

    def check(name: str, condition: bool, detail: str) -> None:
        checks.append({"name": name, "passed": bool(condition), "detail": detail})

    graph_path = root / "validation/pkb001/artifacts/petclinic-graph-818c413.json"
    runtime_path = root / "validation/pkb001/runtime/graphify-petclinic-live-evidence.json"
    history_path = root / "validation/pkb001/datasets/petclinic-delivery-history.json"
    phase0_path = root / "validation/pkb001/reports/phase0-readiness.json"
    graph = load_json(graph_path)
    runtime = load_json(runtime_path)
    history = load_json(history_path)
    phase0 = load_json(phase0_path)
    artifact = load_json(root / ARTIFACT)
    manifest = load_json(root / MANIFEST)
    witness = load_json(root / WITNESS)

    graph_digest = sha256(graph_path)
    history_digest = sha256(history_path)
    check("phase0_ready", phase0.get("status") == "READY" and phase0.get("readiness_state") == "READY", "Both public readiness fields are READY")
    check("graph_digest", graph_digest == EXPECTED_GRAPH_SHA, graph_digest)
    check("history_digest", history_digest == EXPECTED_HISTORY_SHA, history_digest)
    check(
        "exact_source_binding",
        runtime.get("result") == "EXACTLY_BOUND"
        and runtime.get("snapshot_binding", {}).get("requested_revision") == EXPECTED_SOURCE_COMMIT
        and runtime.get("snapshot_binding", {}).get("indexed_revision") == EXPECTED_SOURCE_COMMIT
        and runtime.get("graph_sha256") == EXPECTED_GRAPH_SHA
        and history.get("source_commit_sha") == EXPECTED_SOURCE_COMMIT,
        "Runtime, graph, and history bind the required immutable source commit",
    )
    check(
        "history_boundary",
        history.get("status") == "FROZEN"
        and history.get("history_cutoff") == EXPECTED_CUTOFF
        and history.get("post_cutoff_knowledge_policy") == "EXCLUDE_AFTER_CUTOFF",
        "Frozen history and exclude-after-cutoff policy are present",
    )

    manifest_inputs = {entry["path"] for entry in manifest.get("visible_input_allowlist", [])}
    manifest_outputs = {entry["path"] for entry in manifest.get("outputs", [])}
    check("visible_input_allowlist_exact", manifest_inputs == ALLOWED_INPUTS, f"{len(manifest_inputs)} exact inputs")
    check("output_scope_isolated", manifest_outputs == ALLOWED_OUTPUTS, f"{len(manifest_outputs)} isolated outputs")
    check("forbidden_inputs_not_accessed", manifest.get("forbidden_inputs_accessed") is False and witness.get("forbidden_inputs_accessed") is False, "Both records attest false")

    input_digest_ok = all(sha256(root / entry["path"]) == entry["sha256"] for entry in manifest["visible_input_allowlist"])
    check("manifest_input_digests", input_digest_ok, "Every visible input matches its recorded SHA-256")
    primary_outputs = manifest.get("primary_output_digests", {})
    primary_digest_ok = all(sha256(root / path) == digest for path, digest in primary_outputs.items())
    check("manifest_primary_output_digests", primary_digest_ok, "Artifact and validator match manifest digests")

    witness_input_digests = witness.get("allowed_input_digests", {})
    witness_output_digests = witness.get("primary_output_digests", {})
    check("witness_input_digests", witness_input_digests == {entry["path"]: entry["sha256"] for entry in manifest["visible_input_allowlist"]}, "Witness binds exact manifest input set")
    check("witness_output_digests", witness_output_digests == primary_outputs and primary_digest_ok, "Witness binds the primary generated outputs")
    check("witness_manifest_digest", witness.get("manifest_sha256") == sha256(root / MANIFEST), "Witness binds the immutable manifest bytes")
    check(
        "fresh_orchestration_identity",
        witness.get("orchestration", {}).get("identity") == "/root/pkb001_reverse_run"
        and witness.get("orchestration", {}).get("fork_turns") == "none",
        "Fresh PK-S2 orchestration identity and no-history fork are explicit",
    )
    check("attestation_label", witness.get("attestation_class") == "ATTESTATION_NOT_CRYPTOGRAPHIC_PROOF", "Witness makes its assurance limit explicit")
    phase_time = instant(witness["pre_generation_phase0"]["confirmed_at"])
    generated_time = instant(artifact["generated_at"])
    check(
        "phase0_precedes_generation",
        witness["pre_generation_phase0"].get("status") == "READY" and phase_time <= generated_time,
        f"READY at {phase_time.isoformat()} before generation at {generated_time.isoformat()}",
    )

    binding = artifact.get("source_binding", {})
    check(
        "artifact_binding",
        artifact.get("authority") == "PROPOSAL_ONLY"
        and artifact.get("human_review_required") is True
        and binding.get("source_commit_sha") == EXPECTED_SOURCE_COMMIT
        and binding.get("graph_sha256") == EXPECTED_GRAPH_SHA
        and binding.get("delivery_history_sha256") == EXPECTED_HISTORY_SHA
        and binding.get("history_cutoff") == EXPECTED_CUTOFF,
        "Artifact is proposal-only and exactly bound",
    )

    nodes = {node["id"]: node for node in graph.get("nodes", [])}
    edges = {(edge["source"], edge["relation"], edge["target"]) for edge in graph.get("links", [])}
    node_labels = {node.get("label") for node in graph.get("nodes", [])}
    commits = {commit["commit_sha"]: commit for commit in history.get("commits", [])}
    prs = {pr["number"]: pr for pr in history.get("pull_requests", [])}
    cutoff = instant(EXPECTED_CUTOFF)
    proposals = artifact.get("hypotheses", [])
    proposal_ids = [proposal.get("proposal_id") for proposal in proposals]
    check("stable_unique_proposal_ids", len(proposal_ids) == len(set(proposal_ids)) and all(proposal_ids), f"{len(proposal_ids)} unique IDs")

    all_refs_resolve = True
    all_before_cutoff = True
    all_converge = True
    all_proposal_only = True
    all_reviewable = True
    for proposal in proposals:
        refs = proposal.get("evidence_refs", {})
        graph_nodes = refs.get("graph_node_ids", [])
        graph_edges = refs.get("graph_edges", [])
        commit_shas = refs.get("commit_shas", [])
        pr_numbers = refs.get("pull_request_numbers", [])
        changed_refs = refs.get("changed_path_refs", [])
        all_refs_resolve &= all(node_id in nodes for node_id in graph_nodes)
        all_refs_resolve &= all((edge["source"], edge["relation"], edge["target"]) in edges for edge in graph_edges)
        all_refs_resolve &= all(commit_sha in commits for commit_sha in commit_shas)
        all_refs_resolve &= all(number in prs for number in pr_numbers)
        all_refs_resolve &= all(ref["commit_sha"] in commits and ref["path"] in commits[ref["commit_sha"]]["changed_paths"] for ref in changed_refs)
        all_before_cutoff &= all(instant(commits[commit_sha]["committed_at"]) <= cutoff for commit_sha in commit_shas if commit_sha in commits)
        all_before_cutoff &= all(instant(prs[number]["created_at"]) <= cutoff and instant(prs[number]["updated_at"]) <= cutoff for number in pr_numbers if number in prs)
        structural_files = {nodes[node_id]["source_file"] for node_id in graph_nodes if node_id in nodes}
        delivery_paths = {ref["path"] for ref in changed_refs}
        all_converge &= bool(graph_nodes and commit_shas and (structural_files & delivery_paths) and proposal.get("structural_delivery_convergence"))
        all_proposal_only &= proposal.get("authority") == "PROPOSAL_ONLY" and proposal.get("source_commit_sha") == EXPECTED_SOURCE_COMMIT and proposal.get("graph_sha256") == EXPECTED_GRAPH_SHA
        all_reviewable &= bool(proposal.get("label") and proposal.get("user_product_value") and proposal.get("confidence", {}).get("rationale") and proposal.get("limitations"))
        all_reviewable &= proposal.get("label") not in node_labels

    check("all_evidence_refs_resolve", all_refs_resolve, "Graph nodes/edges, commits, PRs, and changed paths resolve in frozen inputs")
    check("all_evidence_before_cutoff", all_before_cutoff, "Every cited commit and PR event is at or before the frozen cutoff")
    check("structural_delivery_convergence", all_converge, "Every proposal has overlapping graph source and delivery path evidence")
    check("proposal_only_per_hypothesis", all_proposal_only, "Every hypothesis retains proposal-only authority and exact binding")
    check("reviewable_neutral_hypotheses", all_reviewable, "Labels differ from graph technical labels and value/confidence/limitations are present")
    check("ambiguity_preserved", bool(artifact.get("unresolved_boundaries")), "Unresolved candidate boundaries are explicit")

    passed = all(item["passed"] for item in checks)
    return {
        "schema_version": "pkb001.pk-s2.public-validation-report.v1",
        "validation_scope": "PUBLIC_SEAM_ONLY_NO_SEMANTIC_CORRECTNESS_JUDGMENT",
        "validator": str(VALIDATOR),
        "passed": passed,
        "checks": checks,
        "forbidden_areas_inspected": false_value(),
    }


def false_value() -> bool:
    return False


def main() -> int:
    root = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path.cwd().resolve()
    report = validate(root)
    print(json.dumps(report, indent=2, sort_keys=True))
    return 0 if report["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())

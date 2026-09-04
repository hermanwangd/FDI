# Task 5 — Execute isolated PK-S2 reverse experiment

## Allowed inputs

- `skills/pkb001/pk-s2-capability-hypothesis/SKILL.md`
- `validation/pkb001/artifacts/petclinic-graph-818c413.json`
- `validation/pkb001/runtime/graphify-petclinic-live-evidence.json`
- `validation/pkb001/datasets/petclinic-delivery-history.json`
- `validation/pkb001/reports/phase0-readiness.json` only to confirm `READY`

## Forbidden inputs

- Product Semantics, capability candidate/frozen files, or capability IDs/names
- Everything under `validation/pkb001/evaluator/`
- PK-S1 forward output, manifests, witness, reports, or tests
- Any current/historical PK-S2 output, review packet, or decision report
- Post-cutoff source or web knowledge

## Requirements

- Execute PK-S2 as a fresh agent reasoning task, not a deterministic baseline.
- Use structural and delivery evidence together for each hypothesis; structural proximity alone is insufficient.
- Produce reviewable capability hypotheses with neutral labels, graph component/evidence refs, commit/PR refs, confidence, limitations, and `PROPOSAL_ONLY` authority. Do not reproduce technical class names as capability labels without stating user/product value.
- All graph refs, Git commit refs, and PR refs must resolve in the frozen inputs; exclude events after `2026-08-26T10:57:54Z`.
- Bind source commit `818c4136ea971c21674525f9053de0d9c7ad8cfe`, graph SHA `e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e`, and Delivery History SHA `87b70439f69bb82cee0139b65108ecce6044aff1a4d1b5ce6324933acbeab2d1`.
- Record exact visible-input allowlist and `forbidden_inputs_accessed:false`.
- Add a committed, limited provenance witness binding fresh orchestration identity `/root/pkb001_reverse_run`, `fork_turns:none`, allowed input/output digests and pre-generation Phase0 ordering. Label it `ATTESTATION_NOT_CRYPTOGRAPHIC_PROOF`.
- Add public-seam validation for reference resolution, cutoff, convergence, digests, proposal-only status, manifest/witness isolation. Tests must not inspect Product Semantics/evaluator/forward output or judge semantic correctness.
- Do not modify Product Semantics, evaluator truth, or make final GO/REVISE/STOP decision.
- Preserve unrelated duplicate; commands below 8 GB.
- Commit and report to `.superpowers/sdd/IMPLEMENTATION-PLAN/task-5-report.md` with execution, hypotheses summary, validation, commit, and concerns.

## Plan source

`IMPLEMENTATION-PLAN.md`, section 5: Reverse experiment.

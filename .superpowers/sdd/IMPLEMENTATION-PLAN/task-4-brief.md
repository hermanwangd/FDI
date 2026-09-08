# Task 4 — Execute isolated PK-S1 forward experiment

## Allowed inputs

- `skills/pkb001/pk-s1-product-realization/SKILL.md`
- `validation/pkb001/datasets/petclinic-product-semantics-candidate.json`
- `validation/pkb001/artifacts/petclinic-graph-818c413.json`
- `validation/pkb001/runtime/graphify-petclinic-live-evidence.json`
- `validation/pkb001/reports/phase0-readiness.json` only to confirm `READY`

## Forbidden inputs

- Everything under `validation/pkb001/evaluator/`
- Every current or historical PK-S1/PK-S2 output, review packet, blind key, or decision report
- Petclinic Delivery History (not needed for forward)
- Any post-cutoff source or web knowledge

## Requirements

- Execute the PK-S1 skill as a fresh agent reasoning task, not a deterministic code baseline.
- Produce one proposal-only mapping or unresolved result for each of the ten frozen capabilities.
- Cite only graph node IDs and repository-relative source locations that exist in the frozen graph.
- Bind exact source commit `818c4136ea971c21674525f9053de0d9c7ad8cfe`, graph SHA `e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e`, and Product Semantics SHA `72aaacd69f57e0ee4bbb1e9ba04d2f3211d3e73e557730cf57e5fd9988f7cbea`.
- Record an exact `visible_inputs` allowlist, `forbidden_inputs_accessed: false`, execution kind `SKILL_EXECUTION`, authority `PROPOSAL_ONLY`, confidence and limitations.
- Do not score against gold, inspect evaluator files, modify Product Semantics, or make a GO/REVISE/STOP decision.
- Add public-seam validation/tests that all ten capabilities are accounted for, refs resolve, digests match, and no forbidden input appears in the manifest. Do not make tests assert evaluator correctness yet.
- Preserve unrelated untracked duplicate; commands below 8 GB.
- Commit and report to `.superpowers/sdd/IMPLEMENTATION-PLAN/task-4-report.md` with execution method, input manifest, validation, commit, and concerns.

## Plan source

`IMPLEMENTATION-PLAN.md`, section 4: Forward experiment.

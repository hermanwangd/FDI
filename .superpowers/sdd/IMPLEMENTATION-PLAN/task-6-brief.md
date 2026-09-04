# Task 6 — Prepare blind comparison and independent evaluator inputs

## Requirements

- Build a blind review packet from the valid Petclinic PK-S1 forward run and PK-S2 reverse run. Preserve stable item IDs in a separately sealed key, while packet-facing arm labels/order must not reveal forward vs reverse.
- Verify source/digest/proposal-only bindings before packet creation. Do not modify either run, Product Semantics, or evaluator gold.
- The packet must enable judgments using the frozen vocabulary `ACCEPT`, `RENAME`, `MERGE`, `SPLIT`, `REJECT`, `ADD_MISSING`, plus evidence validity, usefulness, unsupported claims, precision, limitations, and review time.
- Include all 9 forward proposals, 1 forward unresolved result, and 5 reverse hypotheses without silently dropping low-confidence items.
- Provide evaluator instructions that clearly distinguish: (a) evaluator-only expected realization scoring, (b) Product meaning judgment, which only human Product Team can finalize.
- Create two empty/templated judgment workspaces with identical packet inputs but no access to each other's future judgment. Do not fabricate judgments in this task.
- Add public-seam tests for exact item accounting, blind-key digest binding, arm-label blinding, input digests, judgment vocabulary, and reviewer isolation contracts.
- Record that upcoming AI evaluator contexts are `NON_HUMAN` and cannot complete Product Team human review.
- Address Task 5 deferred minors if touching its validator/report: explicitly validate non-access witness booleans, and make fresh-run comparison claims only when evidenced.
- Preserve unrelated duplicate, commands below 8 GB.
- Commit and report to `.superpowers/sdd/IMPLEMENTATION-PLAN/task-6-report.md` with RED/GREEN evidence, packet/key paths/digests, commit, and concerns.

## Plan source

`IMPLEMENTATION-PLAN.md`, section 6: Human/evaluator comparison.

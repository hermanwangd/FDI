# S01-S06 Effectiveness Mission — Phase 1 Start

Use the frozen baseline in this directory. Do not edit the freeze artifacts or
historical RC5/RC6/RC7 validation directories.

## Execution order

1. Execute S01 Product Knowledge, then S02 PK Refresh, then S03 Multi-Repo
   Analysis using the frozen local synthetic corpus and detached execution
   repositories.
2. Preserve exact source, artifact, issue, run, reviewer, verifier, and Control
   evidence. Children remain `in_review`; do not manually set child `done`.
3. Only after Phase 1 artifacts exist, run S04 PC0 and PC1 under the frozen
   Product Context A/B protocol.
4. Run S05 only when the S04 authority gate is legitimately satisfied. Run S06
   against the exact S05 candidate. Exercise r1 FAIL/F1, governed correction,
   distinct r2, stale r1 rejection, fresh r2 PASS, and final Finding Resolution.
5. Run the registered Skill A/B cells and then the reference integrated mission.

## Non-negotiable controls

- Resolve every required Control through the existing evaluator/runtime gate;
  issue status and agent prose are not gate evidence.
- Missing evidence is `INCONCLUSIVE`, which blocks progression.
- TKMS/Azure MCP are unavailable on this host and must remain NOT VERIFIED.
- Do not invent PM decisions, repository provenance, verification evidence, or
  completed fan-in.
- Classify every failure with one owning layer from
  `OWNING-LAYER-FAILURES.md` before attempting a bounded fix.
- If a scenario contract, Control predicate, golden set, acceptance criterion,
  or model assumption is wrong, stop and report a new revision requirement.

## Phase 1 acceptance outputs

Create run evidence under `evidence/` and scenario reports for S01-S03. Each
report must include actual result classification, numerator/denominator for
registered metrics, exact evidence refs, reviewer/verifier state, manual rescue
count, and unresolved blockers. Do not promote an effectiveness conclusion
from an incomplete phase.


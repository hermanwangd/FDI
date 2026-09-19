## S01 execution path correction

The Multica projectless worktree is not the validation checkout. Use these
absolute paths; do not infer a corpus from the agent worktree:

- frozen scenario/control/golden baseline:
  `/Users/herman_mbp2023/Documents/FDI-s01-s06-freeze/validation/engcim-swarm-s01-s06/`
- copied RC5 corpus and checksums:
  `/Users/herman_mbp2023/engcim-swarm-s01-s06-validation-20260919/validation-fixtures/`
- detached execution repositories:
  `/Users/herman_mbp2023/engcim-swarm-s01-s06-validation-20260919/execution-repos/`
- execution repo SHAs are recorded in
  `evidence/EXECUTION-REPOSITORY-RECORD.md`.

Read the freeze artifacts first. Do not edit the frozen baseline. Write S01
run evidence and report artifacts under the repository validation namespace only
after real execution. TKMS/Azure MCP remain NOT VERIFIED.

## Required S01 delivery

Produce S01 run evidence and an S01 report with an actual classification,
registered metric numerator/denominator, exact evidence refs, reviewer/verifier
state, manual rescue count, unresolved blockers, and applicable Control Results.
Use revision 1 and leave the child `in_review`; never set it `done`.

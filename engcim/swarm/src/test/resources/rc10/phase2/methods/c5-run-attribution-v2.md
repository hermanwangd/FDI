# Synthetic procedure: bind the C5 report to its publishing run

Classification: local test fixture content only. This file is not a governed
WorkspaceKnowledge record, runtime policy, or authorization to publish a report.

## Applicability and preconditions

Use this procedure only when the exact C5 report evidence shows that its
`source_task_id` differs from the actual C5 publishing parent run. Resolve both
values from the current scoped run and comment receipts. A rejected report by
itself does not establish this cause. If the IDs match, do not apply this repair.

## Minimum steps

1. Read the pinned S05 parent run and the exact C5 report comment by full UUID.
2. Compare the report's `source_task_id` with the actual publishing parent run
   identifier; retain both source references and the observed values.
3. In the local synthetic report draft, replace only a confirmed incorrect
   `source_task_id` with the exact publishing parent run identifier.
4. Re-run the report preflight and verify that C5 attribution resolves to the
   same parent run, issue, and comment before any separately authorized
   publication path is considered.

## Stop conditions and limits

Stop this repair if either identifier or its parent/issue association cannot be
resolved from fresh evidence. Preserve the unresolved cause instead of guessing.
This fixture procedure does not authorize issue writes, publication, status
changes, WorkspaceKnowledge writes, or changes outside the synthetic draft.

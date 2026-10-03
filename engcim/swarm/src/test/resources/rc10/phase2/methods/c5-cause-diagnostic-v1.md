# Synthetic investigative procedure: distinguish C5 report causes

Classification: local test fixture content only. This read-only procedure is
not a governed WorkspaceKnowledge record, runtime policy, or authorization to
publish a report.

## Applicability and preconditions

Use when the pinned S05 C5 report is rejected but current evidence establishes
only the shared symptom. The exact parent run, report comment, issue, and child
gate receipts must be readable within the existing Mission scope. This
investigation is intended to distinguish run-attribution mismatch from a
missing child-result locator without choosing either cause in advance.

## Minimum diagnostic steps

1. Read the actual C5 publishing parent run and the exact report comment by
   full UUID; compare its `source_task_id` with that run and record whether
   equality is proven, mismatch is proven, or the association remains unknown.
2. Read the referenced child result comments. Check whether the required child
   locator is absent, present and correctly associated, or unresolved.
3. Preserve the observed evidence and both cause hypotheses in the existing
   Mission result. Do not edit the report during this read-only diagnostic.
4. For each cause established by fresh evidence, identify its matching repair
   procedure for the already authorized owner. Multiple independently confirmed
   causes may have multiple compatible repairs. Keep every unproven hypothesis
   open, and do not select a cause-specific repair from the shared symptom.

## Stop conditions and limits

Stop the dependent diagnosis if the pinned source revisions, run identity, or
report/comment association cannot be read reliably; retain the missing evidence
and continue unrelated authorized work where safe. This procedure is read-only:
it does not authorize issue/report writes, publication, status changes,
WorkspaceKnowledge writes, or a new Human-selection gate.

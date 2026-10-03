# Synthetic procedure: complete a verified child-result locator

Classification: local test fixture content only. This file is not a governed
WorkspaceKnowledge record, runtime policy, or authorization to publish a report.

## Applicability and preconditions

Use this procedure only when fresh evidence confirms that a required child
result has no clickable locator. A rejected report alone does not establish
this cause. This procedure can apply alongside a separately confirmed run
attribution defect; preserve and repair each confirmed defect under its own
applicable method.

## Minimum steps

1. Read the exact C5 parent report and its C1-C4/C4r child references. Preserve
   the current `source_task_id`; this procedure does not repair attribution.
2. Resolve the required child issue key and full result-comment UUID from the
   fresh scoped issue/comment receipts; verify that the result belongs to the
   cited child and exact reviewed revision.
3. Add the verified locator to the local synthetic report draft, preserving the
   current `source_task_id` and all existing gate identities; this method does
   not repair run attribution.
4. Re-run report preflight and compare the full checked body with a fresh
   readback before any separately authorized publication path is considered.

## Stop conditions and limits

If the child/comment association, full UUID, or reviewed revision cannot be
verified, leave the locator unresolved and retain that evidence gap. This
fixture procedure does not authorize issue writes, publication, status changes,
WorkspaceKnowledge writes, or edits to the synthetic repository.

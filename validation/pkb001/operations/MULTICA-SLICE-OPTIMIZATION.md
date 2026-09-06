# MultiCA slice optimization

Supporting operational guidance, not a sixth active control or Product authority.
This file is the sole home for MultiCA-specific issue, mention, reassignment,
handoff, deduplication, worktree, and KPI mechanics. Generic project authority,
the active-control writer lease, and Human gates remain in `AGENTS.md`.
Owner: Codex analyzes each dispatched slice after completion; Coordinator
supplies compact evidence. Read this file for dispatch or post-slice analysis
only.

## Execution rules

- For two or more parallel slices, dispatch only one Coordinator-owned
  controller. The Coordinator creates and assigns the child slices, records the
  expected child set and integration order, and owns transitions into review.
  Codex and Humans do not bypass it by assigning the workers directly.
- Before dispatch, check scope, dependencies, pinned candidate and equivalent
  active/queued/retrying runs. Use exactly one trigger: assignment, mention or
  rerun. After an ambiguous response, query existing runs before retrying.
  These are instruction-level safeguards, not atomic programmatic deduplication.
- Before combined-integration dispatch, build an idempotency key from Backlog,
  stage, integration base and sorted accepted candidate SHAs. Resolve the
  controller's recorded integration issue first, then search paginated/all-status
  issues or exact references. Any matching issue or candidate, even `done`,
  `in_review` or `cancelled`, blocks a new dispatch until reconciled. Never infer
  absence from the default issue-list page or an open-issue count.
- Build one shared characterization/parity matrix during tranche selection and
  pin its path, digest and interpreter/runtime boundary in every slice brief.
  Reuse it for implementation, review and integration. Extend it only for a
  newly evidenced mismatch; do not repeat open-ended legacy-behavior discovery.
- Independent slices may run concurrently with separate worktrees and explicit
  non-overlapping ownership. Integrate shared controls serially and verify the
  combined candidate. Stay within the aggregate 8 GB resource limit.
- A worker's final handoff names the exact candidate, changed paths, actual test
  results, limitations, blockers, and required reviewer. The comment contains no
  plain-text or structured agent mention (`mention://agent/...`). After the
  comment is published, the worker explicitly reassigns the child to Delivery
  Coordinator once; that reassignment is the only handoff trigger. Never combine
  a mention trigger with reassignment. The worker leaves the issue active and
  does not move itself to `in_review`. The Coordinator validates the handoff,
  transitions the issue, and assigns review exactly once. On every wake it also
  checks the controller's full expected child set for a completed-but-unrouted
  sibling.
- A reviewer publishes one exact-candidate verdict containing one structured
  Delivery Coordinator mention and does not reassign the issue while its review
  task is still running. That mention is the sole verdict-handoff trigger. The
  Coordinator claims the issue with a non-starting assignment after its run
  begins. This avoids both duplicate triggers and the assignment/task-completion
  race.
- Preserve the managed worktree's starting commit ancestry and assigned branch.
  Replay/cherry-pick recovery changes onto it; bind verification to the new SHA.
- Review exact candidates in a separate export for Git-independent tests, or an
  independent clone/review worktree when tests require Git metadata or history.
  Keep the daemon-managed HEAD unchanged during review.
- At review intake, verify the five active-control paths and bound Spec revision,
  but read only the selected Implementation Plan section and directly applicable
  requirement text after the binding is confirmed. Then load only the producer
  handoff, candidate diff, owned implementation/tests, and named evidence. Do
  not reread complete controls, unrelated tests, completed slice history, or raw
  logs unless a concrete inconsistency requires it. Full regression execution
  remains required; context reduction must not reduce verification coverage.
- Record review verdict separately from final run status. Finalize failure blocks
  delivery closure pending reconciliation; it does not automatically invalidate
  a verdict. Check candidate identity, evidence integrity and missing delivery
  steps, then repeat only affected checks. Changed reviewed content or uncertain
  evidence requires fresh review. Do not close while reconciliation is pending.
- Within approved parent scope, slices close automatically after required review,
  verification and successful delivery. Only final canonical Backlog closure
  requires the user's confirmation; scope expansion still needs authorization.
- Derive the authorization envelope from the selected Backlog item,
  Implementation Plan scope, and project Human-boundary rules; do not add
  MultiCA-specific workflow fields to the five active control files. Progress
  sequentially through implementation, review, remediation/fresh review, combined
  integration/review, and the next approved tranche. Do not create a Human
  decision issue for those automatic steps. Stop for Human input only on scope
  or Spec change, permissions/secrets/spending/deployment/destructive/external
  actions, unresolved `CONTEXT_CONFLICT`, or canonical Backlog terminal closure.
- The final slice PASS is handled in the same Coordinator transaction that
  dispatches combined integration. Only the controller emits that transition;
  parent and sibling completion comments contain no Coordinator mentions.
- For a docs-only closure delta, first prove that source, tests, tooling, build
  configuration and dependency tree identities match the reviewed implementation
  candidate. Then run targeted control consistency, JSON/pointer, size-budget,
  public-validator and diff checks and reuse the pinned full-suite evidence. Any
  executable or test-tree change falls back to full verification.

## Three core KPIs

Count all attempts, including cancelled and failed attempts, exactly once by full
run ID. Record source, collection time and completeness. Missing usage is unknown,
not zero. Compare the same role and similar scope, and never trade away required
tests or independent review to improve a metric.

| KPI | Definition | Current baseline (HERM-273 through HERM-282) | Next target | First optimization action when abnormal |
|---|---|---|---|---|
| token cost | Sum input and output across every run; report cache-read separately because its provider cost differs. | 32 runs; 2,333,118 input+output and 60,464,384 cache-read tokens, collected 2026-09-06. | Coordinator share at or below 20%, with zero duplicate-trigger runs. | Remove duplicate triggers and repeated context loading before reducing verification. |
| cycle time | Wall-clock time from the first authorized implementation start until the combined candidate receives an independent verdict. Also report implementation-complete to review-start waiting time. | 2h31m50s end to end; 38m26s from all initial implementations complete to first review start. | Under 2h end to end and under 5m review-routing wait. | Route completed handoffs immediately through the single controller trigger. |
| first-pass rate | Eligible implementation slices that pass their first independent exact-candidate review, divided by all eligible slices. A remediation means the original slice is not first-pass. Zero-usage cancellations are excluded. | 3/4 slices = 75%; combined integration separately passed 1/1. | At least 80% after ten comparable slices; until then report the fraction and sample size. | Classify the first failed finding and improve its acceptance examples or implementation checks. |

## Comparison and decision

Use at least three comparable slices in each before/after cohort and report sample
count, range, runtime/model/instruction revisions and missing data. Evaluate all
three KPIs together: a token reduction is not an improvement when cycle time or
first-pass quality regresses. Select one evidenced optimization per cycle. The
current priority is single-trigger routing and elimination of child-completion
fan-out; parallelism is credited only when it reduces measured wall-clock time.

## Per-slice record

```text
Slice / canonical Backlog / scope / complexity rationale:
Base / candidate / integration candidate:
Model / runtime / instruction revision:
Run IDs and roles / source / collected at / completeness:
Input / output / cache-read / duplicate-trigger runs:
Start / implementation complete / review start / verdict / combined verdict:
Cycle time / review-routing wait / first-pass yes-no-unknown:
Required tests / independent review / scope drift / reconciliation:
Comparable cohort / sample count / changes or N/A:
Largest evidenced problem / one next action / next measurement:
```

Append compact completed-slice records here; link raw evidence instead of copying
logs. Do not dispatch extra LLM runs solely to populate the record.

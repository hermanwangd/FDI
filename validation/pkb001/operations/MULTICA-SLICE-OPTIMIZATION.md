# MultiCA slice optimization

Supporting operational guidance, not a sixth active control or Product authority.
This document configures one current implementation of the Execution Plane. It
does not define project authority. It is the sole home for tool-specific issue,
mention, reassignment, handoff, deduplication, worktree, and KPI mechanics.
Responsibility-plane authority and Human gates remain in `AGENTS.md`.
The Feature Delivery Plane analyzes each completed delivery; the Execution
Plane supplies compact evidence. Read this file for dispatch or post-slice
analysis only.

## Execution rules

- Coordinator is routing-only. It MUST NOT use internal subagents to implement or review
  repository changes, and it must not edit executable code or substitute an
  in-process task tree for tracked Execution Plane work. Every implementation,
  review, remediation, and integration slice is a MultiCA child issue with its
  own attributable run and managed worktree where applicable.
- For an approved multi-slice DAG, create the complete child issue skeleton before implementation:
  record every expected child, stage barrier, ownership boundary, integration
  order, and stable routing key on the parent controller. Verify that parallel
  peers have non-overlapping ownership and distinct managed worktrees before
  starting any implementation child.
- The pre-implementation gate is
  `IMPLEMENTATION_ALLOWED = expected_children_exist AND stages_recorded AND
  ownership_non_overlapping AND parallel_worktrees_distinct`. A false term
  blocks implementation. Sequential execution is not an allowed fallback when
  the envelope requires parallel peers; inability to create child issues or
  managed worktrees is `PLAN_BLOCKED`, not permission to continue internally.
- Coordinator routing concurrency is fixed at one. This serializes controller
  reconciliation transactions, not specialist execution: independently owned
  implementation and review slices may still run in parallel. Increasing
  Coordinator concurrency is prohibited unless the orchestration backend
  provides an atomic compare-and-set claim for routing keys.
- For two or more parallel slices, dispatch only one Coordinator-owned
  controller. The Coordinator creates and assigns the child slices, records the
  expected child set and integration order, and owns transitions into review.
  The Feature Delivery Plane and Human Authority do not bypass its internal
  routing by assigning workers directly.
- Before dispatch, check scope, dependencies, pinned candidate and equivalent
  active/queued/retrying runs. Use exactly one trigger: assignment, mention or
  rerun. After an ambiguous response, query existing runs before retrying.
  These are instruction-level safeguards, not atomic programmatic deduplication.
- Resolve every envelope revision with `git rev-parse --verify '<sha>^{commit}'`
  before issue creation and record the resulting full 40-character SHA. The
  assigned checkout repeats the check and verifies required ancestry before
  work. Invalid or unreachable identity is `PLAN_CONFLICT`; prefix similarity,
  branch position, or a later commit is not a substitute.
- Before combined-integration dispatch, build an idempotency key from Backlog,
  stage, integration base and sorted accepted candidate SHAs. Resolve the
  controller's recorded integration issue first, then search paginated/all-status
  issues or exact references. Any matching issue or candidate, even `done`,
  `in_review` or `cancelled`, blocks a new dispatch until reconciled. Never infer
  absence from the default issue-list page or an open-issue count.
- For implementation-review dispatch, derive a stable review key from execution,
  slice, exact candidate, replay/integration base, and reviewer role. Record the
  pending key on the parent controller; immediately before creating a review issue,
  reread the controller metadata and search all-status matching review issues
  across the full project. An existing pending key or matching issue is reused or
  reconciled; it never permits another create. After a successful create, record the exact review issue ID
  on the controller before any other routing mutation.
  Child-only metadata is evidence, not a concurrency claim.
- Build one shared characterization/parity matrix during tranche selection and
  pin its path, digest and interpreter/runtime boundary in every slice brief.
  Reuse it for implementation, review and integration. Extend it only for a
  newly evidenced mismatch; do not repeat open-ended legacy-behavior discovery.
- Before implementation dispatch, the Feature Delivery Plane and Coordinator
  apply the following bounded-slice gate to each proposed implementation slice.
  Estimates are planning signals, not delivery metrics:

  ```text
  BOUNDED_SLICE_ALLOWED = owned_paths <= 5
      AND estimated_code_and_test_delta <= 500 lines
      AND estimated_tool_calls <= 60
      AND one_primary_deliverable
  ```

  If any term is false, split the work into independently verifiable vertical
  slices or record one concrete reason why splitting would create overlapping
  ownership, a shared mutable contract, unsafe shared state, or an unavailable
  deterministic fixture. Task numbering and downstream runtime data flow are
  not sufficient reasons to combine work. Generated evidence lines do not count
  toward the code-and-test estimate, but each separately generated artifact is
  still part of ownership and integration planning.
- Every implementation slice brief must contain an exact-input manifest before
  dispatch. It lists only inputs the worker actually needs:

  ```text
  path / SHA-256 or exact Git revision / schema or contract / authority
  allowed phase / evaluator-visible yes-no / mutation allowed yes-no
  ```

  The manifest must name frozen semantics, acceptance or authorization inputs,
  structural/runtime evidence, fixtures, and evaluator-only inputs when they
  apply. A worker verifies the supplied identities but does not rediscover
  paths, digests, schemas, or authority already frozen by the Plan. A missing or
  inconsistent required identity is `PLAN_BLOCKED` or `PLAN_CONFLICT`; it is not
  permission for open-ended repository discovery. Evaluator-only inputs remain
  inaccessible until the explicitly authorized evaluation phase.
- Apply a per-stage context budget to implementation runs. Preflight should
  finish within 10 minutes and 15 tool calls. At 40 total tool calls the worker
  compacts state to the exact base, current candidate, completed criteria,
  remaining checks and blockers. At 60 calls it stops optional exploration and
  reports `SLICE_SIZE_EXCEEDED` unless only the already-required focused/full
  verification and final handoff remain. The worker may exceed these planning
  thresholds only to preserve a required correctness, safety or verification
  gate, and must identify the concrete cause in its handoff. Token or call
  budgets never authorize weaker tests, skipped independent review, or an
  incomplete evidence package.
- Independent slices may run concurrently with separate worktrees and explicit
  non-overlapping ownership. Integrate shared controls serially and verify the
  combined candidate. Stay within the aggregate 8 GB resource limit.
- A worker's final handoff names the exact candidate, changed paths, actual test
  results, limitations, blockers, and required reviewer. Its one structured
  Delivery Coordinator mention (`mention://agent/...`) is the sole handoff trigger.
  The worker does not reassign the issue or move it to `in_review`.
  Reassignment from inside the still-running worker task is prohibited: Multica
  may retain the new assignee while suppressing its run, leaving an apparently
  routed issue with no Coordinator execution. After the mention-triggered
  Coordinator run has actually started, the Coordinator claims the issue using
  a non-starting assignment, validates the handoff, transitions the issue, and
  assigns review exactly once. On every wake it also checks the controller's
  full expected child set for a completed-but-unrouted sibling.
- A reviewer follows the same trigger protocol: one structured Delivery
  Coordinator mention in the exact-candidate verdict and no reassignment. The
  Coordinator claims the issue with a non-starting assignment only after its run
  begins. This avoids both duplicate triggers and the assignment/task-completion
  race for implementation and review handoffs.
- Preserve the managed worktree's starting commit ancestry and assigned branch.
  Replay/cherry-pick recovery changes onto it; bind verification to the new SHA.
- Review exact candidates in a separate export for Git-independent tests, or an
  independent clone/review worktree when tests require Git metadata or history.
  Keep the daemon-managed HEAD unchanged during review.
- Independent review must have a distinct Multica run ID and an actor ID that
  did not produce or integrate the candidate. A Coordinator or producer may
  perform verification, but must not label its own in-run check independent.
  Before accepting a verdict, resolve the named reviewer run and confirm its
  actor, candidate SHA, and completion status from task history.
- The reviewer recomputes countable claims from the frozen candidate. If its
  result differs from the candidate evidence or handoff, the verdict is
  `REMEDIATION_REQUIRED` until one canonical value and its derivation are fixed;
  the reviewer must not report PASS while preserving both numbers.
- Run candidate verification from a clean export or independent review
  worktree. Runtime-injected instructions or dirty workspace state may be
  diagnosed separately, but a stash-based self-check does not replace the clean
  candidate result required for delivery.
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
  verification and successful delivery. Only Human Authority confirms final
  canonical Backlog closure; scope expansion still needs authorization.
- Derive the authorization envelope from the selected Backlog item,
  Implementation Plan scope, and project Human-boundary rules. All five active
  control files are read-only to every Execution Plane role. Do not create
  modified copies, version-suffixed replacements, or patches targeting them.
  Progress sequentially through implementation, review, remediation/fresh
  review, combined integration/review, and the next approved tranche. Do not create a Human
  decision issue for those automatic steps. Stop for Human input only on scope
  or Spec change, permissions/secrets/spending/deployment/destructive/external
  actions, unresolved `CONTEXT_CONFLICT`, or canonical Backlog terminal closure.
- The final slice PASS is handled in the same Coordinator transaction that
  dispatches combined integration. Only the controller emits that transition;
  parent and sibling completion comments contain no Coordinator mentions.
- For a docs-only closure delta, the Execution Plane returns the pinned
  implementation identities and verification evidence. The Feature Delivery
  Plane alone applies and validates active-control changes. Any executable or
  test-tree drift requires the full verification profile.

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
Bounded-slice estimate / gate result / exception rationale or N/A:
Base / candidate / integration candidate:
Exact-input manifest digest / identity verification / discovery deviation:
Model / runtime / instruction revision:
Run IDs and roles / source / collected at / completeness:
Input / output / cache-read / duplicate-trigger runs:
Start / implementation complete / review start / verdict / combined verdict:
Cycle time / preflight time and calls / total tool calls / review-routing wait:
First-pass yes-no-unknown / context-budget result / SLICE_SIZE_EXCEEDED yes-no:
Independent reviewer run / actor / candidate / clean-export result:
Required tests / independent review / scope drift / reconciliation:
Comparable cohort / sample count / changes or N/A:
Largest evidenced problem / one next action / next measurement:
```

Append compact completed-slice records here; link raw evidence instead of copying
logs. Do not dispatch extra LLM runs solely to populate the record.

## Completed execution record — HERM-314

- Scope: `PKB-BL-009-JAVA-TEST-BEHAVIOR-001`; four eligible implementation
  slices plus combined integration; collected 2026-09-07 22:06 +08 from issue
  usage, runs, children, and timelines. All 30 nonzero runs are included.
- Token cost: 1,803,824 input + 445,940 output = 2,249,764; cache-read
  52,591,104. Against the 2026-09-06 baseline this is -3.6% input/output,
  -13.0% cache-read, and 30 versus 32 runs.
- Duplicate cost: HERM-320, HERM-321, and HERM-325 account for seven runs,
  536,946 input/output tokens (23.9%) and 11,827,456 cache-read tokens (22.5%).
  Excluding those duplicate attempts for sensitivity only gives 1,712,818
  input/output tokens, 26.6% below the baseline. Official accounting retains
  them. Per-role Coordinator share is `UNKNOWN` because the available usage API
  aggregates mixed-role runs by issue.
- Cycle time: approximately 1h26m from the first implementation start at 12:09
  to the combined independent verdict at 13:35, 43% below the 2h31m50s
  baseline and below the 2h target. Combined implementation completion at 13:28
  to review start at 13:30 was approximately 2m, below the 5m target.
- First-pass rate: canonical eligible slices A/B/C/D passed 4/4 = 100%; combined
  integration passed 1/1. As an operational sensitivity, counting the two
  nonzero duplicate implementation attempts as failed workflow attempts gives
  4/6 = 66.7%; this does not replace the canonical KPI.
- Decision: cycle time and canonical first-pass quality improved, but token
  reduction was obscured by duplicate implementation and review dispatch.
  Apply the already-installed single-trigger, serialized Coordinator routing to
  the next comparable execution. Its success criterion is zero duplicate runs;
  then compare the same three KPIs without reducing verification or review.

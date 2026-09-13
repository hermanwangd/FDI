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
- Choose the specialist start mode before creating or activating its issue.
  If issue creation includes the specialist assignee, that assignment is the
  sole start trigger; the Coordinator MUST NOT post a structured mention to the
  same specialist, activate the same assignment again, or rerun it. If the issue
  is created unassigned, one structured specialist mention is the sole start
  trigger; do not also assign, activate, or rerun it. Immediately after either
  trigger, query the issue's runs and record the observed run identity before
  another routing mutation. The phrase "assignment plus mention" is always two
  triggers and is prohibited.
- At specialist-run intake, compare the current stable routing/review key with
  earlier runs by the same specialist role and with any exact-candidate verdict
  already recorded on the issue. If an earlier equivalent run is queued,
  running, or completed, or the exact verdict already exists, classify the
  later attempt as `COALESCED_DUPLICATE`: perform no implementation or review,
  emit no second verdict or Coordinator mention, and stop after recording the
  duplicate attempt. This intake guard limits damage from runtime replay; it
  does not replace source-side exclusive-trigger dispatch.
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
  race for implementation and review handoffs. That verdict handoff is distinct
  from the Coordinator's earlier specialist start trigger; it does not authorize
  assignment plus a reviewer mention when review begins.
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

Report KPI from largest to smallest scope: `E2E delivery -> stage -> run`. The
headline E2E row shows five dimensions only: time, token cost, quality, flow and
completeness/human intervention. Drill into stages and individual runs only to
explain an abnormal headline value. A run duration is never reported as E2E
delivery time.

- **Time:** E2E lead time, stage lead time and run duration.
- **Token cost:** input and output tokens; cache-read remains separate.
- **Quality:** confirmed errors, test failures, rework and first-pass result.
- **Flow:** dependency wait, stuck count and stuck duration. Dependency waiting
  is not stuck while its declared predecessor is making valid progress.
- **Completeness and human intervention:** missing evidence or usage, plus
  planned Human gates and unplanned manual intervention reported separately.

Use `null` with an explicit missing entry for unfinished or unreported values;
never substitute zero. Count expected negative-test rejection separately from
product or code errors. Deduplicate every aggregation by full run ID, and keep
coordination runs distinct from retries.

Compact machine-readable record:

```json
{
  "execution_id": "SF-BL-NNN-EXECUTION-NNN",
  "category": "FEATURE_IMPLEMENTATION",
  "size": "M",
  "as_of": "2026-09-13T00:00:00Z",
  "status": "IN_PROGRESS",
  "e2e": {
    "target_seconds": 7200,
    "target_state": "GREEN",
    "lead_time_seconds": null,
    "elapsed_seconds": 0
  },
  "cost": {
    "input_tokens": null,
    "output_tokens": null,
    "cache_read_tokens": null,
    "usage_complete": false
  },
  "quality": {
    "error_count": 0,
    "test_failure_count": 0,
    "rework_count": 0,
    "first_pass": null
  },
  "flow": {
    "stuck_count": 0,
    "stuck_seconds": 0,
    "dependency_wait_seconds": 0
  },
  "completeness": {
    "missing_count": 2,
    "missing": ["final_verdict", "token_usage"]
  },
  "human": {
    "planned_gate_count": 0,
    "unplanned_intervention_count": 0,
    "human_wait_seconds": 0
  },
  "monitor": {
    "id": "execution-scoped-monitor-id",
    "cadence": "PT5M",
    "destination": "owning-thread",
    "deduplication_key": "execution_id+kpi+state+evidence_revision",
    "state": "ACTIVE"
  },
  "stages": [],
  "runs": []
}
```

`stages` and `runs` are drill-down arrays using the same dimensions. Record a
final E2E lead time only after the governing acceptance point is reached; before
then report elapsed time and leave lead time `null`.

### Provisional S/M/L delivery targets

Keep two clocks distinct. **Delivery E2E** runs from explicit work selection to
FDP acceptance of complete delivery evidence. **Execution cycle time** retains
the narrower definition in the core KPI table below: first authorized
implementation start to the final valid independent integrated-candidate
verdict. Never substitute one clock for the other.

Until each category/size cohort contains at least five comparable completed
deliveries, use these as provisional management targets rather than measured
baselines:

| Size | Provisional Delivery E2E target |
|---|---|
| S | At or below 45 minutes. |
| M | At or below 2 hours. |
| L | At or below 8 hours. |

Classify time as green at or below target, yellow above target through 1.5 times
target, and red above 1.5 times target. A genuine stuck condition or unplanned
Human recovery also makes the delivery red. Planned Human Authority and declared
dependency waiting remain in Delivery E2E and are additionally reported by
cause; neither is automatically stuck.

Time color never establishes delivery success. Missing required evidence,
unclassified test failures, candidate-attributable failures, or runtime mismatch
prevent a successful-delivery claim even when time is green. Preserve the
pre-dispatch size; a material authorized scope change receives a dated size
revision instead of retrospective resizing.

### KPI ownership and improvement

Every KPI has a **metric owner**, accountable for complete and reproducible
measurement, and an **improvement owner**, accountable for correcting the cause
when the target is missed. Assign responsibility by delivery role, never by a
particular agent or orchestration product.

| KPI area | Metric owner | Improvement owner |
|---|---|---|
| Delivery E2E and target status | Feature Delivery Plane | Feature Delivery Plane |
| Queue, handoff, stuck and duplicate dispatch | Execution Coordinator | Execution Coordinator |
| Token consumption | Execution Coordinator | The stage owner causing the abnormal usage |
| Implementation first-pass quality | Feature Delivery Plane | Delivery Engineer |
| Review escape or missed finding | Feature Delivery Plane | Independent Reviewer |
| Test and runtime compliance | Verification Owner | Verification Owner |
| Evidence completeness and digest read-back | Evidence Receiver | The stage owner producing the missing or invalid evidence |
| Unplanned Human intervention | Feature Delivery Plane | The role whose process caused the intervention |

A reviewer must not weaken review to improve first-pass rate. Human Authority
owns planned authority decisions, not engineering-flow defects.

For every yellow, red, or delivery-blocking quality result:

1. Record the exact KPI, scope, measured value, target and evidence.
2. Classify the cause as execution, dependency, quality, evidence, environment,
   authority or measurement.
3. Assign one improvement owner and one bounded corrective action.
4. Execute through the existing envelope when in scope; otherwise return to FDP
   for replan or Human Authority for a genuine authority decision.
5. Remeasure the same KPI without removing required tests, review or evidence.

One missed target creates one primary corrective action per improvement cycle.
Additional observations remain recorded but do not trigger unrelated process
changes until the primary action is measured.

### KPI alert rules

Send one actionable alert on a state transition, not on every polling cycle:

- **Immediate:** independent `FAIL` or actionable `INCONCLUSIVE`, red E2E,
  genuine stuck, duplicate dispatch, unplanned Human recovery, or terminal
  delivery with missing evidence, unclassified failures or runtime mismatch.
- **Warning:** first transition into yellow E2E, or usage still unreported after
  its run becomes terminal.
- **Quiet:** unchanged state, healthy active work, declared dependency waiting,
  or a long-running process that continues to produce valid progress.

Route the alert to the metric owner and improvement owner. Include execution ID,
KPI, measured value, target, cause, evidence and next action. Deduplicate by
`execution_id + KPI + state + evidence revision`; notify again only when severity
changes, new evidence changes the diagnosis, user action becomes necessary, or
the abnormal condition resolves. Monitoring reports facts and never changes
scope, acceptance gates or authority.

For every new parent execution created after this rule, KPI monitoring is a
pre-dispatch requirement. The controller records the execution ID, category and
S/M/L size, Delivery E2E target, metric owners, improvement owners, monitoring
cadence, notification destination and deduplication key before starting the first
implementation run. Missing monitor configuration blocks dispatch; it does not
authorize a weaker default.

Use one execution-scoped monitor rather than one unbounded project poller. Start
it when the parent execution is selected, keep it quiet under the rules above,
and stop it only after FDP acceptance, explicit cancellation or supersession.
Before stopping, write one final KPI snapshot with complete or explicitly missing
values and the monitor outcome. Existing historical executions are not rewritten
to simulate compliance with this prospective rule.

| KPI | Definition | Current baseline (HERM-273 through HERM-282) | Next target | First optimization action when abnormal |
|---|---|---|---|---|
| token cost | Sum input and output across every run; report cache-read separately because its provider cost differs. | 32 runs; 2,333,118 input+output and 60,464,384 cache-read tokens, collected 2026-09-06. | Coordinator share at or below 20%, with zero duplicate-trigger runs. | Remove duplicate triggers and repeated context loading before reducing verification. |
| cycle time | Wall-clock time from the first authorized implementation start until the combined candidate receives an independent verdict. Also report implementation-complete to review-start waiting time. | 2h31m50s end to end; 38m26s from all initial implementations complete to first review start. | Under 2h end to end and under 5m review-routing wait. | Route completed handoffs immediately through the single controller trigger. |
| first-pass rate | Eligible implementation slices that pass their first independent exact-candidate review, divided by all eligible slices. A remediation means the original slice is not first-pass. Zero-usage cancellations are excluded. | 3/4 slices = 75%; combined integration separately passed 1/1. | At least 80% after ten comparable slices; until then report the fraction and sample size. | Classify the first failed finding and improve its acceptance examples or implementation checks. |

## Comparison and decision

### Size-normalized comparison

Before dispatch, FDP records work category, size and a concrete sizing rationale
in the execution brief. Categories are feature/fix, investigation/experiment,
and documentation; these are KPI cohorts, not replacement Backlog work types.
Size describes the independently acceptable delivery outcome, not agent count,
slice count, file count or tokens consumed:

| Size | Pre-dispatch criterion |
|---|---|
| S | One module, existing contract, local verification sufficient. |
| M | Cross-module or interface change requiring integration verification. |
| L | Cross-system, external runtime, migration or end-to-end isolation verification. |

Use the highest applicable criterion. Preserve the original classification;
authorized scope changes get a dated scope revision, never retrospective sizing
to excuse overruns. Historical unclassified work is descriptive only, not a
prospectively sized benchmark.

Keep raw metrics. Token index = total input+output / prior same-category,
same-size median; cycle index = elapsed time / that cohort's elapsed-time median.
Lower than 1 means lower consumption/time, not automatically better quality.
Cache reads stay separate; token volume is not currency cost. Freeze the cohort
IDs and measurement window before comparison, exclude the current execution,
and require at least five comparable observations per reference cohort. Missing
usage, insufficient samples or a zero median yield N/A, never an invented index.
Report model/runtime/instruction and verification-profile differences.

Include all parent execution runs: coordination, implementation, review,
remediation, integration and duplicate triggers, deduplicated by full run ID.
Do not divide the headline cost by slices. Cycle time runs from first authorized
work start to the final valid independent integrated-candidate verdict; report
dispatch-to-return and FDP intake separately. Waiting remains in elapsed time
and is additionally classified, not subtracted. A superseded PASS is not the end
of a corrected execution's clock.

First-pass rate is not divided by size: compare same-cohort percentage points
and show numerator/denominator. Implementation first-pass requires independent
exact-candidate review; investigation delivery acceptance is a separately named
metric. Report integrated review separately. Later intake findings and rework
remain visible even after initial review PASS. Never lower verification gates
to improve cost or time; do not aggregate these metrics into a single score.

Use at least five comparable observations in each before/after cohort and report sample
count, range, runtime/model/instruction revisions and missing data. Evaluate all
three KPIs together: a token reduction is not an improvement when cycle time or
first-pass quality regresses. Select one evidenced optimization per cycle. The
current priority is single-trigger routing and elimination of child-completion
fan-out; parallelism is credited only when it reduces measured wall-clock time.

## Per-slice record

```text
Slice / canonical Backlog / scope / complexity rationale:
KPI work category / pre-dispatch S-M-L / rationale / scope revision:
Reference cohort IDs and window / sample count / token and cycle indices or N/A:
Bounded-slice estimate / gate result / exception rationale or N/A:
Base / candidate / integration candidate:
Exact-input manifest digest / identity verification / discovery deviation:
Model / runtime / instruction revision:
Run IDs and roles / source / collected at / completeness:
KPI monitor ID / cadence / destination / deduplication key / final state:
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

## Completed execution record — HERM-364

- Scope: `SF-BL-002-SCENARIO-INTENT-003` Task 1; one implementation slice plus
  exact-candidate review. Candidate `50ceb93290348ee5888a798787505e26ced2f13c`
  was integrated byte-equivalently as `9ad0d17`; HERM-366 returned `PASS`.
- Runs: Coordinator `01a0868e-2f53-75b7-8229-9e537f1ae773`, Engineer
  `01a08691-6eff-72ba-b2eb-df8de545a99d`, review-routing Coordinator
  `01a0869a-5815-7e97-8549-d32253ad951e`, Adjudicator
  `01a0869c-6954-7ed4-b6fe-29d7ec589b51`, and finalization Coordinator runs
  `01a086a0-a83a-73d9-9643-1c66d03cb6d2` plus
  `01a086a2-1435-7a06-9dfa-8a11aeaca2ae`. No duplicate implementation or
  review issue was observed.
- Cycle time: implementation start 14:27Z, implementation complete 14:37Z,
  review start 14:39Z, independent verdict 14:44Z on 2026-09-09; approximately
  17 minutes end to end and 2 minutes review-routing wait.
- First-pass: `1/1 = 100%`; no remediation. Focused verification passed `9/9`;
  Feature Delivery integration later passed the full `1069/1069` suite.
- Tool calls: implementation reported `24/60`; reviewer and Coordinator totals
  are `UNKNOWN`. Token usage is `UNKNOWN`; the checked issue/run/comment
  surfaces did not expose a complete attributable token record. Collection
  source: Multica issue runs and delivery comments, collected 2026-09-09;
  telemetry completeness: timing and verdict complete, usage incomplete.
- Quality: exact-input, evaluator isolation, deterministic output and immutable
  history passed. Independent standards review later identified no hard
  violation and four deferred maintainability findings. Spec review caused a
  separate acceptance-manifest provenance correction before the next dispatch.
- Decision: single-trigger routing met the under-5-minute routing target and
  first-pass quality improved over the 75% baseline, but token-cost change is
  not measurable. Next measurement must require explicit usage completeness
  while preserving the same review and verification gates.

# MultiCA slice optimization

Supporting operational guidance, not a sixth active control or Product authority.
Owner: Codex analyzes each dispatched slice after completion; Coordinator supplies
compact evidence. Read this file for dispatch or post-slice analysis only.

## Execution rules

- For two or more parallel slices, dispatch only one Coordinator-owned
  controller. The Coordinator creates and assigns the child slices, records the
  expected child set and integration order, and owns transitions into review.
  Codex and Humans do not bypass it by assigning the workers directly.
- Before dispatch, check scope, dependencies, pinned candidate and equivalent
  active/queued/retrying runs. Use exactly one trigger: assignment, mention or
  rerun. After an ambiguous response, query existing runs before retrying.
  These are instruction-level safeguards, not atomic programmatic deduplication.
- Independent slices may run concurrently with separate worktrees and explicit
  non-overlapping ownership. Integrate shared controls serially and verify the
  combined candidate. Stay within the aggregate 8 GB resource limit.
- A worker's final handoff names the exact candidate, changed paths, actual test
  results, limitations, blockers, and required reviewer. It then explicitly
  reassigns the child to Delivery Coordinator as the one handoff trigger.
  Plain-text `@Delivery Coordinator` does not count as routing. The worker
  leaves the issue active and does not move itself to `in_review`. The Coordinator validates the handoff,
  transitions the issue, and assigns review exactly once. On every wake it also
  checks the controller's full expected child set for a completed-but-unrouted
  sibling.
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
- Treat `STATUS.json.active_execution.authorization_scope`, `auto_progress`, and
  `human_gate` as the executable authorization envelope. Progress sequentially
  through implementation, review, remediation/fresh review, combined
  integration/review, and the next approved tranche. Do not create a Human
  decision issue for those automatic steps. Stop for Human input only on scope
  or Spec change, permissions/secrets/spending/deployment/destructive/external
  actions, unresolved `CONTEXT_CONFLICT`, or canonical Backlog terminal closure.

## Five KPIs

Count all attempts, including cancelled and failed attempts, exactly once by full
run ID. Record source, collection time and completeness. Missing usage is unknown,
not zero; reconcile issue totals against run coverage before claiming savings.

| KPI | Definition | First optimization action when abnormal |
|---|---|---|
| total input/output tokens | Record input and output separately across every attempt; use their sum for the reduction target. | Reduce repeated context reads and unnecessary output. |
| cache-read tokens | Sum reported cache reads separately from input/output; document provider accounting to avoid double counting. | Inspect long sessions and repeatedly loaded history. |
| tool calls | Count actual tool invocations, not streamed message fragments. | Batch related reads and reduce polling. |
| rework ratio | Rework input plus output divided by all input plus output, multiplied by 100. | Address the largest evidenced rework cause. |
| first-pass verdict | Workflow success on the first implementation/review path, with successful delivery and no repair or recovery; also record the independent review verdict. | Separate code defects from runtime/handoff failures. |

Tag rework as code defect, runtime/recovery, duplicate dispatch, or repeated
verification. Ordinary first independent review is not rework. Attribute mixed
runs only with measured usage boundaries; otherwise report rework ratio as N/A.
Never estimate token share from task counts. Unstarted cancellations with confirmed
zero usage are recorded but not counted as failed first-pass executions.

## Comparison and decision

Compare the same role and similar scope/complexity, with criteria recorded before
looking at outcomes. Keep documentation closure separate from code migration.
Use at least three comparable slices in each before/after cohort for medians;
report sample count, range, runtime/model/instruction versions and missing data.
These are descriptive comparisons, not proof that context changes caused savings.

Provisional targets: input+output and cache-read each decrease 30%; tool calls
decrease 25%; rework below 25%. First-pass is reported as successes/eligible slices;
evaluate the 80% target only after ten comparable slices. Preserve required tests,
independent review and zero scope drift regardless of the cost result.

Select one highest-priority improvement per cycle. Current priority: reduce
duplicate dispatch and finalize-related recovery. Retain compact context and
verification while assessing this change. Parallelism may reduce elapsed time;
it does not establish token savings.

Earlier HERM-269/270/271 issue totals are provisional observations. Cancelled and
failed usage coverage is unverified; prior task-count rework percentages are
withdrawn. HERM-272 is documentation closure and is not a third migration sample.

## Per-slice record

```text
Slice / canonical Backlog / scope / complexity rationale:
Base / candidate / integration candidate:
Model / runtime / instruction revision:
Run IDs and roles / source / collected at / completeness:
Input / output / cache-read / actual tool calls:
Rework tokens / total tokens / ratio or N/A / cause:
Review verdict / final run status / first-pass yes-no-unknown:
Required tests / independent review / scope drift / reconciliation:
Comparable cohort / sample count / changes or N/A:
Largest evidenced waste / ONE next action / next measurement:
```

Append compact completed-slice records here; link raw evidence instead of copying
logs. Do not dispatch extra LLM runs solely to populate the record.

# Execution semantics

The core stays exactly ExecutionPlan, RequirementCoverage, Dependency, WorkItem,
ChangeClaim and WorkItemResult. No extra run/task lifecycle is introduced.
Operational terms below describe adapter-local durable records, not domain fields.

## Engineering progression

| Current boundary | Required condition | Next authorized activity |
|---|---|---|
| T1 | Exact accepted context and Human-authorized criteria | Immutable IntentSpec |
| T2 | Complete DeliverySpec; SPEC_READY | T3 planning |
| T2 BLOCKED | Missing/contradictory evidence resolved by correct authority | New readiness evaluation |
| T3 | Approved immutable DAG and materialized envelope | EP execution |
| EP completion | Integrated candidate plus mandatory verified outputs/evidence | Independent T4 |
| T4 PASS | Exact candidate and full frozen criterion coverage | ENGINEERING_READY; closure proposal |
| T4 FAIL implementation | FDP routing | T3 remediation, fresh review and T4 |
| T4 FAIL design | FDP routing and material-change authorization | T2 revision, new plan, execution, T4 |
| T4 INCONCLUSIVE | Evidence unavailable/invalid/insufficient | Obtain valid evidence; never relax criteria |
| Product intent change | Human decision | Stop old cycle; new T1/cycle |

## Adapter operations (EX-01)

`submit(envelope, routingKey)` returns a receipt or an admission rejection.
Same key and exact payload reuses the receipt. Same key with different payload is
PLAN_CONFLICT. The payload includes the approved input digests and binding profile.
`inspect(receipt/key)` returns durable execution facts, authoritative runtime
attribution, unresolved side effects and result references, including terminal
history. `cancel(receipt, reason)` records a cancellation request, not proof that
work stopped. `collect(receipt)` returns sealed evidence and WorkItemResult only
after the applicable completion checks; otherwise it reports pending/blocker.

Invalid bytes/commit/contract are PLAN_CONFLICT; missing capabilities/resources
are PLAN_BLOCKED; engineering changes are PLAN_CHANGE_REQUIRED. Diagnostics bind
operation, reason, affected refs and retryability. Expected transient transport
errors never become an artificial COMPLETED result.

## Runtime state transitions (EX-02; adapter profile only)

| From | Event/guard | To |
|---|---|---|
| no record | Valid envelope, durable unique-key claim | admitted |
| admitted | Dependencies satisfied; mutation proof; capacity; start once | executing |
| executing | Outputs captured | verifying |
| verifying | All local requirements met | settled with COMPLETED |
| executing/verifying | Local unrecoverable execution failure | settled with FAILED/EXECUTION_FAILURE |
| admitted/executing/verifying | Required input absent or scope conflict | settled with appropriate BLOCKED reason |
| any unsettled record | Ambiguous external response, disconnect or cancel request | reconciling |
| reconciling | Authoritative observation proves outcome | prior valid phase or settled |
| settled | Duplicate delivery | Same immutable result, no new side effect |

Cancellation is a profile fact. If cancellation wins the serialized admission/
completion decision, fence further mutation and settle FAILED/EXECUTION_FAILURE
with a cancellation diagnostic only after side effects are reconciled. If verified
completion was already committed, return that result and cancellation-too-late.
An unknown remote outcome remains reconciling, never falsely cancelled/failed.
No successor starts from an unsettled, FAILED or BLOCKED predecessor.

After reconciling a crashed attempt, an authorized retry creates a fresh runtime
receipt for the exact same WorkItem, input digests and permissions. It cannot
alter prior result bytes. FDP never sees attempt fields in logical WorkItem.

## Dependencies and mutation (EX-03)

Resolve every predecessor to one exact COMPLETED WorkItemResult and every required
output to that result's valid digest-bound output. Absence of a DAG edge is not
proof of non-overlap. Compare governed ChangeSurface equality, containment and
overlap. Unknown overlap blocks scheduling and returns to FDP; EP cannot invent
new path ownership or silently serialize a mandatory parallel envelope.

Integration and regression are ordinary WorkItems with dependencies. Candidate
replay changes the SHA and invalidates reviews for the old candidate. Integration
requires a fresh exact-candidate independent review, even if every slice passed.

## Recovery, retries and events (EX-04)

At-least-once notification handling; never promise exactly-once external effects.
Persist dispatch intent before the start trigger, then record observed receipt.
After timeout, search complete history before any second trigger. Unknown outcome
disables automatic retry for a non-idempotent side effect until reconciliation.

The approved runtime profile defines max attempts (default 3 total), timeout
(default 15 minutes per attempt), total deadline (default 45 minutes), and bounded
backoff (5 then 15 seconds with up to 20% jitter). Company profiles may change these
before dispatch. Limits never waive required verification. Contract, scope and
permission failures are not transient retries. Three consecutive provider
transport failures open a local breaker for 60 seconds; one inspect probe tests
recovery. No breaker is shared across unrelated tenants.

Events carry adapter-local identity and monotonic receipt revision. Duplicate
events are idempotent; gaps/out-of-order updates trigger inspect. An old worker
must be fenced from result acceptance and protected writes. If a runtime cannot
fence remote writes, quarantine its workspace, revoke its credentials, confirm
termination and reconcile external effects before replacement dispatch.

Current Multica mechanics remain in its binding profile: one routing writer,
complete child skeleton, one exclusive start trigger, distinct parallel
worktrees, exact review attribution. These are not assumed atomic guarantees.

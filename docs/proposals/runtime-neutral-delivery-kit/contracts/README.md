# Contract status and validation

All serialization schemas here are **PROPOSED**. Logical field names and result
enums preserve Software-Factory FD-T3-001–007. They do not freeze the upstream
non-frozen T1/T2/envelope field-level schemas. Schema validation alone cannot
prove authority, correct references, safe concurrency or semantic completeness.

Core schemas: ExecutionPlan, Dependency, RequirementCoverage, WorkItem,
ChangeClaim and WorkItemResult. Strict `additionalProperties: false` prevents
runtime fields from entering core contracts. Envelope, Evidence, BoundRef and
RuntimeProfile are proposed boundary profiles, never extra Product domains.

The adapter ports use Envelope for submit, an opaque durable receipt for inspect/
cancel/collect and WorkItemResult plus Evidence on collection. Receipt serialization
is binding-local. No public HTTP endpoint is specified or claimed implemented.

## Required semantic checks (CT-01–12)

1. Resolve every named source commit as a full reachable 40-character commit;
   verify required ancestry and exact control/spec digests. Zero fixture commits fail.
2. Hash stored UTF-8 bytes with SHA-256. No hidden JSON canonicalization assumption:
   preserve exact serialized bytes across adapters or assign a new digest.
3. Resolve unique plan/work-item refs, reject cycles, dangling/duplicate semantic
   edges and duplicate WorkItem identities even when object bytes differ.
4. Resolve coverage exactly against DeliverySpec DeliveryRequirements. Every
   mandatory requirement has valid WorkItem coverage; no AcceptanceCriteria copy.
5. Resolve every claim to governed T2 ChangeSurface; evaluate overlap including
   aliases/containment. Unknown overlap blocks parallel dispatch.
6. Resolve successor inputs to exact COMPLETED predecessor result outputs; digest,
   visibility and applicability must pass. FAILED/BLOCKED never unlocks a successor.
7. Verify envelope resolved constraints/claims/requirements are equivalent to its
   immutable plan/WorkItem; owned paths are a derived limit, not independent scope.
   Verify all transitive refs, including authorization and runtime/skill profiles.
8. Enforce path canonicalization against a configured artifact root, reject symlinks,
   absolute/drive/UNC paths, backslashes, empty/dot components and traversal after
   decoding. The schema regex is preliminary; filesystem enforcement is mandatory.
9. Verify output/evidence completeness against requirements before COMPLETED;
   artifact hashes cannot replace functional acceptance. Mandatory AGENT_CLAIM
   alone is insufficient. Schema allows partial outputs on FAILED/BLOCKED.
10. Verify reviewer identity from runtime records, distinct from every producer and
    integrator; exact candidate, independent context, completed review and no repair.
11. Enforce permissions, visibility/arm isolation, revocation and profile capabilities
    before loading inputs, executing skills or applying side effects.
12. Recheck candidate and contract digests at evidence intake, T4 and closure.
    Changed bytes invalidate dependent review; immutable old evidence remains.

## Evidence mapping and errors

The Evidence profile records generation method, identities and result; trusted
runtime attribution and artifact inspection are still required. WorkItemResult
`outcomeReasonCode` is absent for COMPLETED and mandatory for FAILED/BLOCKED,
with only SCOPE_CONFLICT, INPUT_UNAVAILABLE, CONSTRAINT_VIOLATION, EXECUTION_FAILURE.
PLAN_BLOCKED/PLAN_CONFLICT/PLAN_CHANGE_REQUIRED are envelope-level diagnostics.
T4 verdict and Human closure are never encoded as WorkItemResult outcome.

Missing refs map to INPUT_UNAVAILABLE; out-of-claim mutation maps to SCOPE_CONFLICT;
violated local requirements map to CONSTRAINT_VIOLATION; runtime failure maps to
EXECUTION_FAILURE. Unknown external effects stay in reconciliation rather than
manufacturing a settled core result.

Company implementation must implement CT-01–12 in Java and demonstrate the
conformance cases. The local package check reports only schema shape and package
integrity, not these unimplemented semantic checks.

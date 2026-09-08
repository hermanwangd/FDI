# FDI Software Factory — Candidate Framework Specification

> **Candidate only — not active project truth.** This document defines a proposed
> post-PKB-001 framework contract. It MUST NOT control current execution until an
> authorized, atomic migration reconciles all five root control files.

## 1. Scope and terminology

The framework governs the path from accepted Product Knowledge through feature
delivery verification. Normative terms `MUST`, `MUST NOT`, `SHOULD`, and `MAY`
have their usual requirements meaning.

Stable requirement families are:

- `AUTH-*`: authority and semantic ownership;
- `PK-*`: Product Knowledge contracts;
- `FD-T1-*` through `FD-T4-*`: delivery-stage contracts;
- `EXEC-*`: execution and integration contracts; and
- `EVID-*`: evidence identity and integrity.

## 2. Authority

### AUTH-001 — Product meaning

Human Authority MUST own Product meaning, accepted Capabilities, accepted
Behavior Scenarios, material scope changes, and semantic publication.

### AUTH-002 — Tool evidence boundary

Graphify, source structure, repository tests, Git, pull requests, and delivery
history MAY provide evidence. They MUST NOT independently establish Product
truth.

### AUTH-003 — Plane independence

Authority MUST be assigned by responsibility plane, not by software, vendor,
model, or agent identity. The Execution Plane MUST NOT modify Feature Delivery
control inputs.

## 3. Product Knowledge

### PK-001 — Knowledge states

Capabilities and Behavior Scenarios MUST distinguish `PROPOSAL`, `ACCEPTED`,
`REJECTED`, and `SUPERSEDED` states. Only exact, accepted versions MAY enter T1.

### PK-002 — Proposal isolation

Reverse-generated Capabilities and scenarios MUST remain proposal-only until
Human Authority reviews the exact proposal revision. Generator access to
evaluator truth or post-generation decisions MUST fail closed.

### PK-003 — Behavior scenarios

A Behavior Scenario MUST describe observable product behavior and MUST NOT
encode source paths, classes, methods, provider node IDs, or technical selection
instructions. It MUST bind a parent Capability, preconditions, action/event,
observable outcomes, scope, status, and approval provenance.

### PK-004 — Product Knowledge handoff

A Product Knowledge handoff to T1 MUST include exact knowledge references and
digests, Capability and scenario states, evidence provenance, applicability,
and known limitations. Missing or conflicting authority MUST block T1.

### PK-005 — PKB-001 foundation

The post-prototype framework MUST preserve PKB-001's validated boundaries:
Graphify behind `CodeIntelligenceProvider`, exact-revision structural evidence,
Java-owned framework behavior, and proposal-only reverse inference.

## 4. T1 Intent contract

### FD-T1-001 — IntentSpec

T1 MUST emit one immutable `IntentSpec` containing:

```text
intentRef
productKnowledgeRefs[]
capabilityRefs[]
scenarioRefs[]
acceptanceCriteria[]
scope
constraints[]
evidenceExpectations[]
digest
```

Every Acceptance Criterion MUST trace to at least one accepted Capability or
Behavior Scenario. Unaccepted hypotheses MUST NOT become normative intent.

### FD-T1-002 — Acceptance Criteria immutability

Acceptance Criteria MUST remain unchanged within a delivery cycle. If product
intent or a criterion must change, the current cycle MUST stop and a new
IntentSpec revision MUST begin a new cycle.

### FD-T1-003 — Incomplete Product Knowledge

When Product Knowledge is incomplete, T1 MUST either request Human Authority
resolution, record an explicitly approved assumption, or reduce scope. T1 MUST
NOT invent Product meaning.

## 5. T2 Delivery Specification contract

### FD-T2-001 — DeliverySpec

T2 MUST emit an immutable `DeliverySpec` containing:

```text
deliverySpecRef
intentRef
intentDigest
architectureApproach
componentChanges[]
interfaceChanges[]
dataChanges[]
migrationRequirements[]
qualityRequirements[]
verificationStrategy
deliveryConstraints[]
risks[]
digest
```

### FD-T2-002 — Bidirectional coverage

Every technical change MUST trace to an Acceptance Criterion. Every mandatory
Acceptance Criterion MUST have an implementation mapping, verification mapping,
evidence requirement, and component or responsibility boundary.

### FD-T2-003 — T1 protection

T2 MAY choose technical architecture but MUST NOT weaken, delete, or reinterpret
T1. Ambiguous or conflicting criteria MUST produce `T2_BLOCKED`. A required T1
change MUST stop the cycle rather than being hidden inside the DeliverySpec.

## 6. T3 Execution contract

### FD-T3-001 — ExecutionPlan

T3 MUST emit an immutable `ExecutionPlan` containing:

```text
planRef
intentRef
deliverySpecRef
deliverySpecDigest
workItems[]
dependencies[]
integrationStrategy
verificationPreparation
digest
```

### FD-T3-002 — WorkItem

Each WorkItem MUST contain:

```text
workItemRef
type
objective
deliverySpecRefs[]
acceptanceCriteriaRefs[]
inputRefs[]
ownedPaths[]
excludedPaths[]
outputRequirements[]
evidenceRequirements[]
constraints[]
```

WorkItems MUST NOT encode agent, model, vendor, or orchestration identities.
Parallel WorkItems MUST have satisfied dependencies and non-conflicting
ownership.

### FD-T3-003 — Dependency

Each dependency MUST identify predecessor and successor WorkItems. The
Execution Plane MUST NOT start a successor before all mandatory predecessor
results satisfy the plan.

### FD-T3-004 — WorkItemResult

Each result MUST bind the exact WorkItem, plan, inputs, attempt, outcome,
changed paths, outputs, evidence, tests, findings, and resulting revision.

### FD-T3-005 — Retry, remediation, and replan

- Retry MUST retain the exact contract and inputs and use a new `attemptRef`.
- Remediation MAY change implementation within the same approved WorkItem scope.
- Any change to objective, references, ownership, outputs, evidence requirements,
  WorkItems, or dependencies MUST return `PLAN_CHANGE_REQUIRED` and create a new
  `planRef`.
- A technical-design change MUST return to T2 and create a new DeliverySpec.
- An Acceptance Criteria change MUST stop the cycle and create a new IntentSpec.

## 7. Execution Plane and integration

### EXEC-001 — Coordinator responsibility

The Execution Plane Coordinator MUST own routing, implementation coordination,
independent review, bounded remediation, combined integration, full regression,
and delivery-evidence assembly. It MUST NOT delegate integration to T4 or Human
Authority.

### EXEC-002 — IntegrationResult

The Coordinator MUST produce one `IntegrationResult` containing exact base and
candidate revisions, accepted WorkItem results, changed-path manifest,
integration conflicts and resolutions, combined reviews, full regression
results, unresolved findings, and digests.

### EXEC-003 — Pre-verification gates

Before requesting T4, the integrated candidate MUST pass:

1. identity consistency;
2. mandatory WorkItem completion;
3. single-candidate integration;
4. full regression on that candidate;
5. Acceptance Criteria traceability; and
6. evidence integrity.

Failure MUST return to T3 remediation, T3 replanning, or T2 revision according
to the change required. It MUST NOT be waived by T4.

## 8. Delivery Evidence Package

### EVID-001 — Package identity

The package MUST bind exact `intentRef`, `deliverySpecRef`, `planRef`, base Git
revision, candidate Git revision, artifact digests, and generation metadata.

### EVID-002 — Required content

The package MUST include:

```text
identity
workItemResults[]
independentReviews[]
remediations[]
integrationResult
changedPathManifest
regressionResults[]
acceptanceCoverage[]
unresolvedFindings[]
limitations[]
artifactDigests[]
```

Each Acceptance Criterion coverage record MUST connect the criterion to its
DeliverySpec requirement, WorkItem result, changed artifact, and verification
evidence.

### EVID-003 — Evidence integrity

Evidence MUST record how it was generated, exact input revision, tool/runtime
version, result, and digest. Prose assertions without reproducible identity MUST
NOT satisfy a mandatory evidence requirement.

## 9. T4 Verification contract

### FD-T4-001 — VerificationRequest

T4 MUST receive one exact integrated candidate, the immutable IntentSpec,
DeliverySpec, ExecutionPlan, and Delivery Evidence Package with matching
references and digests. Any mismatch MUST produce
`VERIFICATION_BLOCKED_INVALID_INPUT`.

### FD-T4-002 — CriterionResult

T4 MUST record an individual result for every Acceptance Criterion, including
criterion reference, outcome, evidence references, findings, and rationale.

### FD-T4-003 — VerificationDecision

The immutable decision outcome MUST be exactly one of:

- `PASS` — all mandatory criteria and evidence/review gates pass;
- `REVISE_T3` — implementation or integration correction is required;
- `REVISE_T2` — technical specification correction is required;
- `INCONCLUSIVE` — valid evidence cannot yet support a decision; or
- `STOP` — the delivery cycle cannot validly continue.

`PASS` establishes `ENGINEERING_READY`, not deployment, publication, or user
delivery.

### FD-T4-004 — Revision routing

`REVISE_T3` MUST retain T1 and route remediation or replanning through T3.
`REVISE_T2` MUST retain T1 and create a new DeliverySpec and ExecutionPlan.
T4 MUST NOT modify T1. If an Acceptance Criterion is invalid, T4 MUST issue
`STOP`; Human Authority may then start a new cycle with a new IntentSpec.

### FD-T4-005 — Independence and history

Verification MUST be performed independently from production and integration.
Every decision MUST bind the exact candidate and remain immutable. Any candidate
content change MUST trigger full regression and a new verification decision.

## 10. Framework invariants

1. Product meaning remains under Human Authority.
2. Product Knowledge proposals cannot self-promote.
3. T1 Acceptance Criteria are immutable within a cycle.
4. T2 and T3 cannot weaken T1.
5. Execution integrates and tests one candidate before T4.
6. T4 verifies but does not implement or integrate.
7. All identities and evidence are exact and digest-bound.
8. Contract state transitions fail closed on ambiguity or mismatch.
9. Contracts remain independent of execution software and agent identity.

## 11. Activation constraint

This candidate MUST NOT be activated by replacing only this Spec. Activation
requires an authorized compatibility migration that simultaneously reconciles
the root Project Overview, Framework Spec, Backlog, Implementation Plan, Status,
and any affected supporting contracts. Existing PKB-001 execution remains bound
to its current active revision until completion or explicit cancellation.

# FDI Software Factory Framework Specification

**Contract status:** Active Software Factory baseline

## 1. Objective

The framework governs the path from heterogeneous Product evidence to governed
Product Knowledge and from accepted Product Knowledge to applicable Engineering
Scenarios. A Scenario may declare an optional Software Delivery Profile for
evidence-backed Feature delivery. The framework must measure whether Product
Knowledge improves delivery without allowing evidence, structure, history,
tests, or agents to manufacture Product truth.

Normative terms `MUST`, `MUST NOT`, `SHOULD`, and `MAY` express requirement
strength.

## 2. Normative requirement index

| Requirement | Required outcome |
|---|---|
| `AUTH-001` | Human Authority owns Product meaning and material authority decisions. |
| `AUTH-002` | Tools and engineering evidence cannot establish Product truth automatically. |
| `AUTH-003` | Authority is assigned by responsibility plane, not software identity. |
| `SCN-001` | Engineering Scenario is the reusable composition boundary for context, Skills, Controls, evidence, and execution. |
| `SCN-002` | Scenario scope, revision, applicability, limitations, and required evidence are explicit and fail closed when unresolved. |
| `SKILL-001` | Skills are bounded reusable capabilities with explicit inputs, outputs, limitations, and revisions. |
| `SKILL-002` | A Skill cannot publish Product truth, weaken a Control, or create authority. |
| `CTRL-001` | Controls are fail-closed gates with independently resolvable evidence and a canonical result status. |
| `CTRL-002` | Canonical Control results are exactly `SATISFIED | UNSATISFIED | INCONCLUSIVE`. |
| `PK-001` | Product sources become observations and proposals before accepted knowledge. |
| `PK-002` | Proposal, accepted, rejected, and superseded knowledge states remain distinct. |
| `PK-003` | Product Context is exact-versioned, provenance-bound, and fail-closed. |
| `PK-004` | Reverse discovery remains proposal-only. |
| `PK-005` | Delivery evidence can propose learning but cannot publish Product truth. |
| `CSI-001` | Verified, evidence-bound delivery findings may recommend system improvements, but every resulting change uses existing authority, planning, execution, verification, and Product Knowledge paths. |
| `FD-T1-001` | Under an SD profile, T1 produces immutable Product intent and Acceptance Criteria. |
| `FD-T1-002` | Under an SD profile, Acceptance Criteria cannot be weakened within a delivery cycle. |
| `FD-T2-001` | Under an SD profile, T2 performs System Analysis, ChangeSurface, TechnicalDesign, and DeliverySpec. |
| `FD-T2-002` | Under an SD profile, DeliverySpec owns AcceptanceCriterion-to-DeliveryRequirement traceability. |
| `FD-T2-003` | Under an SD profile, ChangeSurface supports deterministic overlap evaluation. |
| `FD-T2-004` | Under an SD profile, only `SPEC_READY` proceeds to T3; conflict or insufficiency is `BLOCKED`. |
| `FD-T3-001` | Under an SD profile, ExecutionPlan is an immutable engineering DAG. |
| `FD-T3-002` | Under an SD profile, RequirementCoverage explicitly maps DeliveryRequirements to WorkItems. |
| `FD-T3-003` | Under an SD profile, WorkItem is a vendor-neutral executable engineering contract. |
| `FD-T3-004` | Under an SD profile, ChangeClaim is a hard mutation authorization boundary. |
| `FD-T3-005` | Under an SD profile, parallel eligibility requires dependency safety and mutation safety. |
| `FD-T3-006` | Under an SD profile, WorkItemResult binds the exact executed contract and execution facts. |
| `FD-T3-007` | Under an SD profile, retry and replan remain separate authority decisions. |
| `EXEC-001` | Execution Plane schedules, executes, reviews, integrates, and reports without changing engineering authority. |
| `EXEC-002` | Materialization creates a portable envelope without creating authority. |
| `EXEC-003` | Independent execution lanes may progress concurrently only with explicit ownership, dependency, isolation, and aggregate-resource safety. |
| `EXEC-004` | Runtime identity is generic and cannot create Scenario, authority, or Control semantics. |
| `EVID-001` | Evidence binds exact inputs, outputs, revisions, generation method, and digests. |
| `PK-006` | Accepted Product Knowledge is reusable across applicable Scenarios through exact-versioned, provenance-bound Product Context. |
| `SD-001` | Software Delivery Profile is optional and is the only profile that activates the existing `FD-T1-*` through `FD-T4-*` requirements. |
| `SD-002` | `FD-T1-*` through `FD-T4-*` do not govern Scenarios that do not declare the Software Delivery Profile. |
| `PORT-001` | Cross-baseline change export is exact-revision, cross-file, reference-only, and fail-closed. |
| `FD-T4-001` | Under an SD profile, T4 independently evaluates the integrated candidate. |
| `FD-T4-002` | Under an SD profile, correctness verdict is exactly `PASS | FAIL | INCONCLUSIVE`. |
| `FD-T4-003` | Under an SD profile, revision routing cannot silently modify T1. |
| `FD-T4-004` | Under an SD profile, `PASS` means `ENGINEERING_READY`, not delivered. |
| `SF-EVAL-001` | The MVP compares Code Only with Product Knowledge under a frozen isolated protocol. |
| `TECH-001` | Framework code uses Java 17/Spring Boot 3.4.1; external Graphify remains behind the provider boundary. |

## 3. Architecture and authority

```text
Product Sources
      ↓
Product Knowledge (accepted, exact-versioned)
      ↓
Engineering Scenario
├── Product Context
├── Skills
├── Controls
├── Evidence Contract
├── Execution Record
└── optional Software Delivery Profile
        T1 → T2 → T3 → T4
                ↓
        generic Execution Plane runtime
```

T1–T4 are Software Delivery Profile stages, not a universal lifecycle for
every Engineering Scenario. Multica may bind to the generic Execution Plane
runtime; it is not an authority identity or a Core contract dependency.

### AUTH-001 — Human Authority

Human Authority MUST own Product meaning, accepted Product Knowledge, material
scope and architecture changes, Acceptance Criteria changes, semantic
publication, deployment, and terminal closure. One individual MAY perform this
role for a prototype.

### AUTH-002 — Evidence boundary

Graphify, source code, tests, training material, specifications, Git, pull
requests, work items, and delivery history MAY provide evidence. No tool,
evidence source, Skill, model, Feature Delivery actor, or Execution Plane actor
MAY independently publish durable Product truth.

### AUTH-003 — Plane independence

The Engineering Scenario MUST own the composition of applicable Product
Context, Skills, Controls, evidence obligations, and execution binding. When a
Scenario declares the Software Delivery Profile, the Feature Delivery Plane
MUST own project truth, T1–T4 progression, engineering contracts, replanning,
T4 routing, and closure preparation for that profile. The Execution Plane MUST
treat active Controls and approved engineering contracts as read-only. Multica
is a generic runtime binding, not an authority identity or core contract
dependency.

The framework MUST NOT introduce a Factory Control, DeliveryRun, Workcell,
Provider hierarchy, execution lease, agent run, job, or task domain merely to
duplicate lifecycle or scheduling already owned by Feature Delivery and the
Execution Plane. A future abstraction requires demonstrated independent
ownership, identity, lifecycle, persistence, or stable cross-module contract.

## 3A. Scenario-first Core contracts

### SCN-001 — Engineering Scenario

An Engineering Scenario is the reusable composition boundary for one bounded
objective. It MUST bind an immutable `scenarioRef` and `scenarioRevision`, an
objective and scope, applicable Product Context, required Skills, required
Controls, an evidence contract, and an execution binding. It MAY bind an
optional Software Delivery Profile.

### SCN-002 — Scenario resolution

Scenario scope, applicability, limitations, conflicts, and required evidence
MUST be explicit. Missing or conflicting authority MUST fail closed. A
Scenario MUST NOT inherit T1–T4 merely because it uses the generic Execution
Plane runtime.

### SKILL-001 — Reusable bounded capability

A Skill MUST declare its revision, capability boundary, inputs, outputs, and
limitations. Skills MAY analyze, transform, plan, or verify within that
boundary and SHOULD be reusable across applicable Scenarios.

### SKILL-002 — Skill authority boundary

A Skill MUST NOT publish Product truth, change Scenario scope, weaken a Control,
or promote an unsupported result. Skill output remains subject to the Scenario
and applicable Controls.

### CTRL-001 — Fail-closed Control

A Control MUST bind a subject, required evidence, a result reference, and an
independently resolvable outcome. An unresolved required evidence reference
cannot be treated as a satisfied gate.

### CTRL-002 — Control status

Canonical Control status is exactly:

```text
SATISFIED | UNSATISFIED | INCONCLUSIVE
```

`UNSATISFIED` and `INCONCLUSIVE` block the affected progression unless an
existing, explicitly authorized route handles the state. A review, comment,
issue status, or producer claim is not a substitute for a Control result.

### EXEC-004 — Runtime neutrality

The Execution Plane runtime MUST remain generic. `runtimeRef` identifies the
execution mechanism, not authority, Scenario semantics, or Control lifecycle.
Multica is an allowed runtime binding but is not a Core domain contract.

### PK-006 — Reusable Product Knowledge

Accepted Product Knowledge MUST be exact-versioned and provenance-bound before
it resolves into Product Context. The same accepted knowledge MAY be reused by
multiple applicable Scenarios; each Scenario retains its own applicability,
limitations, conflicts, and evidence binding.

### SD-001 — Optional Software Delivery Profile

The Software Delivery Profile is optional. It is the only profile that
activates the existing `FD-T1-*`, `FD-T2-*`, `FD-T3-*`, and `FD-T4-*`
requirements.

### SD-002 — T1–T4 scope

`FD-T1-*` through `FD-T4-*` apply only within a declared Software Delivery
Profile. They do not govern Engineering Scenarios that do not declare that
profile. Existing `FD-T*` identifiers and their historical evidence remain
unchanged.

### Canonical Phase 2 Control vocabulary

The validated Core vocabulary uses these exact Control names:

```text
CTRL-AUTHORIZATION-001
CTRL-EXECUTION-SAFETY-001
CTRL-REPOSITORY-PROVENANCE-001
CTRL-EXACT-BINDING-001
CTRL-INDEPENDENT-EVALUATION-001
CTRL-EVIDENCE-INTEGRITY-001
CTRL-FINDING-RESOLUTION-001
```

Applicability and required evidence remain governed by the frozen validation
baseline. This specification does not rewrite those bindings or historical
results.

## 4. Product Knowledge

### PK-001 — Source-to-knowledge pipeline

```text
Source Intake
→ Observation
→ Correlation / Conflict Detection / Synthesis
→ Product Knowledge Proposal
→ Human Authority
→ Durable Product Knowledge
→ Context Resolution
→ Resolved Product Context
```

Source Intake MAY accept direct Product seed, training material, manuals,
specifications, test cases, troubleshooting material, architecture, RCA,
runbooks, Azure DevOps features, PRs, commits, code, API/schema, and Graphify
observations. Source formats MUST be normalized through Source Intake and
Observation handling; the framework MUST NOT create one Skill per source type
without an independent reusable reasoning need.

### PK-002 — Knowledge state and semantic boundary

Product Knowledge MUST distinguish proposal, accepted, rejected, and superseded
versions. Only exact Human-accepted versions MAY enter T1. Capability Behavior
Scenarios MUST describe observable Product behavior and remain free of source
paths, classes, methods, provider IDs, expected components, and technical
selection instructions.

### PK-003 — Resolved Product Context

A Product Context handoff MUST bind exact knowledge versions and digests,
Capabilities, Behavior Scenarios, provenance, applicability, limitations,
conflicts, and assumptions. Missing or conflicting authority MUST fail closed.
Evidence references remain distinct from accepted semantic statements.

### PK-004 — Reverse discovery

```text
Structural Intelligence
+ Repository Test Behavior
+ Delivery History
→ Capability and Behavior Scenario Proposals
→ Human review
```

Reverse results MUST remain proposal-only. Generation MUST NOT access accepted
Forward semantics, evaluator truth, hidden tests, post-generation judgments, or
post-run decisions unless an explicitly different approved protocol requires it.

### PK-005 — Learning loop

```text
Delivery / Verification Evidence
→ Observation
→ Product Knowledge Proposal
→ Human Authority
→ Durable Product Knowledge
```

Feature Delivery evidence MUST NOT directly become durable Product Knowledge.

Reusable Product Knowledge capabilities are:

- `PK-S1 Product Semantics Synthesis`
- `PK-S2 Product Realization Synthesis`
- `PA-Codebase-Inventory`
- `PA-Historical-Delivery`

## 5. Software Delivery Profile — T1 — Intention

The following `FD-T1-*` through `FD-T4-*` requirements apply only when the
Engineering Scenario declares the Software Delivery Profile. They are not a
universal lifecycle for other Scenario types.

### FD-T1-001 — IntentSpec

T1 MUST consume one exact Resolved Product Context and emit one immutable
`IntentSpec` containing the Product change, scope, constraints, Behavior
Scenarios, Acceptance Criteria, governing references, and digest.

Every Acceptance Criterion MUST trace to accepted Product Knowledge or an
explicit Human Authority decision. T1 MUST NOT embed technical implementation
selection.

### FD-T1-002 — Acceptance Criteria authority

Acceptance Criteria are frozen within one IntentSpec revision. T2 MUST NOT
weaken them, T3 MUST NOT reinterpret them, and T4 MUST NOT modify them. T4 FAIL
MUST NOT become PASS by relaxing a criterion.

Changing Product intent or Acceptance Criteria requires Human Authority to stop
the current cycle, create a new IntentSpec revision, and start a new delivery
cycle.

## 6. Software Delivery Profile — T2 — Specify

### FD-T2-001 — Reasoning sequence

```text
System Analysis
→ ChangeSurface
→ TechnicalDesign
→ DeliverySpec
→ Specification Readiness
```

ChangeSurface identifies governed components, interfaces, repositories,
schemas, configuration surfaces, dependencies, or deployment artifacts affected
by the Feature. TechnicalDesign defines the engineering approach across that
surface. DeliverySpec is the immutable engineering contract consumed by T3 and
T4 and MUST NOT replace T1 Product correctness authority.

### FD-T2-002 — Delivery requirement traceability

DeliverySpec MUST own:

```text
AcceptanceCriterion → DeliveryRequirement
```

Every mandatory Acceptance Criterion MUST have an implementation,
verification, evidence, and engineering-responsibility mapping. ExecutionPlan
MUST NOT duplicate this mapping as another authority.

### FD-T2-003 — Governed overlap semantics

Every ChangeSurface identity used for mutation planning MUST support
deterministic evaluation of equality, containment, overlap, or non-overlap at
sufficient granularity. Different references do not imply disjoint surfaces.
Unknown overlap MUST be treated as conflict. If a surface is too coarse for safe
parallel mutation, T2 MUST refine the governed ChangeSurface rather than T3
creating a second local scope model.

### FD-T2-004 — Readiness

T2 readiness is exactly:

```text
SPEC_READY | BLOCKED
```

Only a complete, internally consistent DeliverySpec may proceed to T3.

## 7. Software Delivery Profile — T3 — Execution planning

### FD-T3-001 — ExecutionPlan

```text
ExecutionPlan {
    planRef
    deliverySpecRef
    workItems[]
    dependencies[]
    requirementCoverage[]
}
```

`planRef` is the immutable identity of one engineering decomposition.
Material decomposition or dependency changes require a new plan identity.

### FD-T3-002 — RequirementCoverage

```text
RequirementCoverage {
    deliveryRequirementRef
    workItemRefs[]
}
```

ExecutionPlan owns `DeliveryRequirement → WorkItem`. Full traceability is
composed with the DeliverySpec-owned `AcceptanceCriterion →
DeliveryRequirement` mapping. ExecutionPlan MUST NOT directly own or redefine
Acceptance Criteria.

### FD-T3-003 — WorkItem

```text
WorkItem {
    workItemRef
    objective
    inputRefs[]
    changeClaims[]
    outputRequirementRefs[]
    evidenceRequirementRefs[]
    constraints[]
}
```

A WorkItem defines execution completion, not Product correctness. Its default
completion rule requires all mandatory output requirements, evidence
requirements, and local constraints to be satisfied.

Core WorkItem MUST NOT include agent, model, provider, runtime state, workspace,
attempt, retry metadata, routing hints, Acceptance Criteria copies, direct
DeliverySpec copies, dependency copies, or free-text completion criteria.

### FD-T3-004 — ChangeClaim

```text
ChangeClaim {
    changeSurfaceRef
}
```

A ChangeClaim is a hard engineering mutation authorization boundary over a
governed T2 ChangeSurface. A worker MAY read governed inputs outside the claim
when allowed but MUST NOT mutate outside it. Additional mutation need is
`BLOCKED + SCOPE_CONFLICT`; the Execution Plane MUST NOT silently expand scope.

### FD-T3-005 — Dependency and parallel safety

```text
Dependency {
    predecessorRef
    successorRef
}
```

A dependency is satisfied only when the predecessor WorkItemResult is
`COMPLETED` and every successor input that depends on predecessor output
resolves to a valid output from that exact result. A successor MUST NOT start
after predecessor `FAILED` or `BLOCKED`.

Absence of a dependency means only that no semantic ordering is declared.

```text
parallel eligibility
= dependency-safe
AND mutation-safe
```

Mutation safety requires proof that active ChangeClaims do not overlap. Unknown
overlap is conflict. Feature Delivery must add ordering, refine decomposition,
or replan; Execution Plane must not invent ownership.

### FD-T3-006 — WorkItemResult

```text
WorkItemResult {
    workItemRef
    executedContractDigest
    outcome
    outcomeReasonCode
    outputRefs[]
    evidenceRefs[]
}
```

`outcome` is exactly `COMPLETED | FAILED | BLOCKED`.
`outcomeReasonCode` is absent for COMPLETED and required for FAILED or BLOCKED.
The initial reason codes are `SCOPE_CONFLICT`, `INPUT_UNAVAILABLE`,
`CONSTRAINT_VIOLATION`, and `EXECUTION_FAILURE`.

Reason codes report execution facts, not Feature Delivery routing decisions.
`COMPLETED` never establishes Acceptance Criterion satisfaction or T4 PASS.
`executedContractDigest` MUST bind the exact executed WorkItem contract.

### FD-T3-007 — Retry and replan

Retry preserves the exact WorkItem, inputs, mutation authority, and contract
digest while creating a new runtime attempt. It is Execution Plane-owned.

Any material change to objective, governed inputs, ChangeClaims, output or
evidence requirements, constraints, decomposition, dependencies, or requirement
coverage is a Feature Delivery-owned replan requiring a revised ExecutionPlan.
TechnicalDesign or DeliveryRequirement changes require T2 revision first.

Integration and full regression SHOULD be modeled as ordinary WorkItems and DAG
dependencies. They do not require a first-class integration stage.

## 8. Execution Plane and evidence

### EXEC-001 — Execution responsibility

Execution Plane owns DAG runtime scheduling, eligible parallel execution,
runtime coordination, agent/Skill/model/tool selection, runtime retry,
independent review, bounded remediation, combined integration, full regression,
diagnostics, and evidence assembly.

Execution Plane MUST NOT change Product intent, DeliverySpec, ExecutionPlan,
WorkItem contracts, ChangeClaims, Product truth, or T4 correctness. A material
engineering-contract problem MUST return to Feature Delivery.

### EXEC-002 — Materialization boundary

Logical WorkItems MAY depend on contextual references. Before independent
dispatch, Feature Delivery MAY materialize a portable execution envelope from
one exact ExecutionPlan and WorkItem revision. Resolved inputs, requirements,
claims, and constraints MUST retain governing provenance and digests.
Materialization MUST NOT introduce, weaken, expand, or modify authority.

The exact serialized envelope schema is not frozen by this revision.

### EXEC-003 — Concurrent execution lanes

The Feature Delivery Plane MAY select more than one unrelated Backlog item for
concurrent execution without combining them into one engineering contract. Each
selection MUST be represented as a distinct execution lane with its own stable
lane and execution identities, Backlog and requirement bindings, exact base
revision, execution state, integration candidate, owned and excluded paths,
dependencies, resource budget, evidence package, and closure gate.

Concurrent lanes MUST satisfy all of the following before either lane mutates
the workspace:

- their mutation ownership is deterministically non-overlapping;
- any shared input is read-only and bound to an exact revision or digest;
- cross-lane dependencies are explicit rather than inferred from scheduling;
- combined memory, process, service, and external-cost limits remain within the
  project-wide resource envelope; and
- each lane has an isolated branch or workspace and immutable evidence namespace.

Unknown path overlap, an unbound shared input, an exceeded aggregate resource
limit, or competing control-file writers MUST fail closed for the affected
lanes. Failure or blockage in one lane MUST NOT stop an independent lane unless
an explicit dependency, shared safety boundary, or global resource limit
requires it.

The five active control files remain singletons. One Feature Delivery Plane
owner MUST serialize their updates and reconcile all lane transitions. The
active Implementation Plan remains one file but MAY contain one bounded section
per active lane; unrelated Backlog items MUST NOT be merged into one execution
envelope merely to obtain concurrency. Human terminal approval and closure are
evaluated independently for each Backlog item.

### EVID-001 — Evidence integrity

Evidence MUST bind exact input and candidate revisions, applicable contract
digests, generation method, tool/runtime identity, result, artifacts, and
digests. Prose claims alone cannot satisfy a mandatory gate. Provider-native
diagnostics may remain in an evidence envelope without becoming core domain
fields.

### CSI-001 — Continuous system improvement

Verified, evidence-bound findings from independent review, verification,
execution, delivery, or KPI measurement MAY produce a system-improvement
recommendation. The Feature Delivery Plane MUST determine that a finding is
actionable before recommending a revision route.

A recommendation MUST identify its originating evidence, the affected
requirement or expected behavior, the observed failure or insufficiency, a
proposed prevention or earlier-detection control, affected KPIs, and a
recommended existing revision route. Candidate, execution, review, runtime, and
digest identities are required only when applicable; an inapplicable identity
MUST be explicitly identified as not applicable rather than invented.

`CODE`, `DELIVERY`, `PK`, `MIXED`, and `UNKNOWN` MAY be used as analytical
tags. These tags MUST NOT establish root-cause certainty, create authority,
dispatch work, expand scope, or determine Product meaning.

The Feature Delivery Plane MUST reconcile an actionable recommendation against
the existing T2, T3, T4, Backlog, evidence, and Product Knowledge requirements.
A recommendation MAY identify a prospective revision route, but MUST NOT itself
enact remediation, replan, Backlog inclusion or selection, Product Knowledge
acceptance, or a new delivery cycle.

An improvement outside an approved execution envelope MUST NOT be implemented
until the applicable Human decision or existing project authorization has been
obtained and the work has been selected, planned, materialized, and assigned
through the existing delivery process.

Product-learning recommendations MUST enter `PK-005` as Observations or
Proposals. Product meaning and Acceptance Criteria changes require Human
Authority, stop the current delivery cycle, and create a new IntentSpec and
delivery cycle only after the Human decision.

An authorized improvement MUST use the existing WorkItem, evidence, independent
review, and T4 mechanisms. Improvement evidence SHOULD report review escape,
recurrence, remediation elapsed time, additional runs, input/output tokens, and
unplanned Human intervention when those values are available. Missing values
remain unknown. Insufficient comparable observations MUST be reported as
`INSUFFICIENT_SAMPLE` and MUST NOT be represented as measured effectiveness.

This capability MUST NOT introduce a parallel finding lifecycle, correctness
verdict, workflow runtime, terminal-closure state, or automatic Product
Knowledge publication.

### PORT-001 — Cross-baseline change reference export

When source and receiving repositories have no shared Git commit baseline, the
framework MAY export a change-reference package from one exact ancestral source
commit range. The package MUST represent changed portions across code, tests,
documentation, active controls, contracts, configuration, Skills, and bounded
evidence; it MUST NOT assume that source paths or complete files can replace the
receiving repository.

Every package and record MUST declare `REFERENCE_ONLY`,
`DO_NOT_APPLY_BLINDLY`, `NO_SHARED_BASELINE`, and that automatic application is
not allowed. The receiving actor MUST locate the corresponding local surface,
adapt the intended behavior, verify it under receiving-repository controls, and
use its own review and merge process. An exported active-control change is a
review input only and MUST NOT overwrite or establish receiving project truth.
External execution status SHOULD be marked as not recommended for adoption.

Export MUST bind exact source commits, committed blob identities, changed-path
operations, bounded before/after excerpts, output digests, generation method,
classification, omissions, truncation, and safety decisions. Deterministic
inputs and a fixed clock MUST produce byte-identical output. Secrets, unsafe
paths, evaluator-only truth, build output, archives, and binary payloads MUST
fail closed or remain explicitly excluded metadata; a partial package MUST NOT
be published. The exporter MUST NOT mutate, merge, cherry-pick, or execute code
in the receiving repository.

## 9. Software Delivery Profile — T4 — Correctness

### FD-T4-001 — Independent verification

T4 MUST independently evaluate the exact integrated candidate using frozen
Acceptance Criteria, DeliverySpec, implementation outputs, and valid execution
evidence. It MUST NOT rely solely on a producer or integrator self-verdict.

### FD-T4-002 — Verdict

```text
CorrectnessVerdict = PASS | FAIL | INCONCLUSIVE
```

`INCONCLUSIVE` means valid evidence is insufficient; it is not FAIL.

### FD-T4-003 — Revision routing

- Implementation defect: `FAIL → T3 remediation → T4 reverify`.
- Decomposition/dependency defect: `FAIL → T3 replan → execution → T4 reverify`.
- TechnicalDesign defect: `FAIL → T2 revision → new plan → execution → T4 reverify`.
- Product intent/Acceptance Criteria change: stop the cycle; only Human Authority
  may create a new IntentSpec and delivery cycle.
- INCONCLUSIVE: obtain valid missing evidence or resolve the verification
  environment without weakening criteria.

T4 may route revision but MUST NOT edit T1.

### FD-T4-004 — Delivery boundary

`PASS` establishes `ENGINEERING_READY`. Deployment, release, publication,
operational rollout, and user delivery require separate controls. Therefore:

```text
ENGINEERING_READY != DELIVERED
```

## 10. Controlled Product Knowledge value experiment

### SF-EVAL-001 — Two-arm protocol

`SF-BL-001` MUST compare:

- **Arm A — Code Only:** frozen demand and exact source baseline plus only
  protocol-approved code, test, history, and tools.
- **Arm P — Product Knowledge:** the identical baseline plus the exact accepted
  Product Context.

Both arms MUST use the same frozen Feature demand, Acceptance Criteria, source
revision, Software Delivery Profile T1–T4 contracts, evaluator truth, model/tool class, budget, stopping
rules, and measurement definitions. Contexts, workspaces, prompts, sessions,
artifacts, and evidence stores MUST remain isolated.

Product Knowledge MUST NOT contain the new Feature's solution, hidden tests,
expected components, evaluator mappings, or post-run decisions.

The comparison MUST report critical Acceptance Criteria pass/fail, weighted
acceptance coverage, first T4 outcome, remediation and replan count,
wrong-surface changes, regression defects, cycle time, token/tool cost, Product
Knowledge bootstrap cost, leakage checks, and one bounded `GO | REVISE | STOP`
decision. The decision MUST NOT automatically publish Product Knowledge.

## 11. Technology and preserved foundation

### TECH-001 — Framework and provider boundary

New executable framework behavior MUST use Java 17 and Spring Boot 3.4.1.
Declarative Skills and JSON Schemas are not executable framework code.

The external Graphify Python MCP runtime is outside the framework. It MUST remain
behind `CodeIntelligenceProvider` and the Java Graphify adapter. Structural
evidence MUST bind the exact source revision, frozen snapshot, provider/runtime
identity, graph digest, working directory, and query bounds. FDI MUST inspect
the real installed runtime and MUST NOT hard-code assumed Graphify APIs.

PKB-001 validated implementation and evidence remain immutable. Reuse requires
contract compatibility and preserves proposal-only reverse inference,
Human-authority semantics, evaluator isolation, deterministic evidence, and
exact-revision binding.

## 12. Current scope boundary

`CSI-001` defines a prospective Software Factory capability. It does not amend
the scope, acceptance, ChangeClaims, execution envelopes, or required gates of
an execution already bound to an earlier exact control revision. Implementing
this capability requires separately authorized and selected Backlog work.

`SF-BL-005` MAY perform the explicitly selected scenario-mapping feasibility
tranche and define an execution-specific successor scoring protocol. This does
also permit the Human-selected minimal nested-test identity and module-root
correction under a revised Plan/envelope, preserving prior accepted outputs.
Human-selected successor preparation MAY implement the Java METHOD-pair evaluator
and sealed comparison-input binding with synthetic/calibration validation. This
does not select a holdout or establish an experimental GO decision.
User-requested reproduction of an existing frozen real-data protocol MAY run
in a new isolated output namespace. It MUST identify the original protocol and
MUST NOT present old-unit scores as successor METHOD-pair results.
Human-selected real METHOD calibration MAY wire producers, independently seal
METHOD/chain truth before generation, and compare bounded producer improvements
with identical inputs and scoring. Exposed repositories remain calibration;
this does not authorize formal holdout execution or a generalization claim.
This permission does
not amend prior experimental verdicts or authorize TYPE generation, automatic
Product publication, or formal holdout execution without its selection gates.
Its current mutation and execution boundary is the selected Implementation Plan.

The active delivery target is `SF-BL-001`. One bounded prerequisite correction,
`SF-BL-002`, MAY repair the preserved PKB-001 evidence pipeline before reuse.
That correction MUST keep reverse inference proposal-only, MUST distinguish
production-resolution gaps from external and test-helper diagnostics, MUST use
only mechanically provable production references, and MUST write a new
immutable evidence run rather than alter preserved PKB-001 results.

This baseline does not
authorize a knowledge-graph database, automatic semantic publication,
maintenance engine, multi-product federation, production deployment, real-time
equipment integration, a Factory Control runtime, or one Skill per source type.

Schemas not explicitly frozen above—including complete IntentSpec,
AcceptanceCriterion, DeliverySpec, DeliveryRequirement, TechnicalDesign,
ChangeSurface, Product Context, Observation, Product Knowledge Proposal, and
SerializedExecutionEnvelope field-level schemas—require separate review before
implementation treats them as frozen.

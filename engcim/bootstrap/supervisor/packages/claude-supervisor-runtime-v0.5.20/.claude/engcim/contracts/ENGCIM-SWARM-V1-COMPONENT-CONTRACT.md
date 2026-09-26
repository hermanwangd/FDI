# ENGCIM Swarm v1.0 Component Contract

**Status:** Canonical Architecture Boundary — Draft for Freeze  
**Scope:** ENGCIM Swarm v1.0

## 1. Canonical Component Model

ENGCIM Swarm v1.0 contains seven ENGCIM components:

1. Product Knowledge
2. Product Context
3. Scenario
4. Skill
5. Control
6. Swarm Core
7. Runtime Binding

**Multica Runtime is the external Execution Plane, not an ENGCIM component.**  
**Claude Supervisor is outside the Swarm and owns inspection, diagnosis, recommendation, Mission Learning Source preparation and tKMS I/O, and verification.**

```text
                    Claude Supervisor
              Observe / Inspect / Diagnose
                  Recommend / Verify
                          |
                          v
+---------------- ENGCIM Swarm v1.0 ----------------+
|                                                    |
|  1. Product Knowledge                              |
|           |                                        |
|           v                                        |
|  2. Product Context                                |
|           |                                        |
|           v                                        |
|  3. Scenario                                       |
|       +---+---+                                    |
|       v       v                                    |
|  4. Skill   5. Control                             |
|       +---+---+                                    |
|           v                                        |
|  6. Swarm Core                                     |
|           |                                        |
|           v                                        |
|  7. Runtime Binding                                |
+-----------+----------------------------------------+
            |
            v
       Multica Runtime
       Execution Plane
```

## 2. Component Contracts

| # | Component | Responsibility / Owns | Key Input | Key Output | Source / Implementation | Runtime Form | Primary Owner | Must Not Own |
|---|---|---|---|---|---|---|---|---|
| 1 | **Product Knowledge** | Canonical product truth: architecture, behavior, capability, constraints, realization/code mapping, historical decisions | TKMS/docs, repositories, specs, historical PBI, artifacts | Versioned Product Knowledge objects | Ingest/extract/curate from product sources; version controlled | Structured knowledge / knowledge base | Product Team + ENGCIM Knowledge capability | Mission-specific context selection or execution logic |
| 2 | **Product Context** | Resolve, select, compose and materialize mission-specific context | Mission, Scenario context requirements, Product Knowledge, relevant repo/code context | Resolved Product Context with revision/provenance | Context resolver / composer | Service / library | ENGCIM Context capability | Canonical product truth |
| 3 | **Scenario** | Define work semantics and composition: goal, input/context requirements, Skill refs, Control refs, output requirements, completion criteria | Mission, Product Context, Scenario definition | Executable work definition/composition | Versioned declarative Scenario definitions | Configuration / metadata interpreted by runtime | ENGCIM Scenario capability | Engineering reasoning implementation or shared orchestration engine |
| 4 | **Skill** | Reusable engineering capability: analysis, architecture/design, implementation, verification, etc. | Context, work item, artifacts | Engineering result with supporting evidence | Versioned Skill package: procedures, instructions, templates, examples/assets | Agent Skill / capability package | ENGCIM Skill capability | Governance/gate/enforcement policy |
| 5 | **Control** | Governance, evidence, quality, provenance, authorization, gate and enforcement semantics | State, artifacts, evidence, revisions, policy context | Control decision + evidence | Machine-evaluable or explicitly executable policy/rule/predicate | Control / policy capability | ENGCIM Control capability | Engineering reasoning capability |
| 6 | **Swarm Core** | ENGCIM semantic orchestration: Mission→Scenario composition, work/dependency intent, context/evidence propagation semantics, Skill/Control binding semantics, result/evidence aggregation semantics, Scenario completion semantics | Mission, Scenario, Product Context, Skills, Controls | ENGCIM orchestration intent/result semantics | Shared generic product-agnostic runtime code | Semantic orchestration engine | ENGCIM Swarm Core Dev Team | Generic task scheduling, fan-out/fan-in mechanics, retry, pause/resume, durable runtime state, generic re-entry, product-specific knowledge, Skill logic, Control policy |
| 7 | **Runtime Binding** | Translate ENGCIM execution semantics to/from Multica representation | ENGCIM work/agent/context/evidence/state | Multica execution request + translated ENGCIM result/state/evidence | Adapter / translation layer | Adapter service / SDK / integration | ENGCIM Runtime Integration | Generic execution mechanics owned by Multica |

## 3. External Execution Plane — Multica Runtime

**Owns:** generic execution mechanics: agent/task execution, dispatch, execution state, retry/re-entry and other Multica runtime behavior.

**Input:** execution request from Runtime Binding.  
**Output:** execution result, state, logs/evidence exposed by Multica.  
**Owner:** Multica platform/team.

**Must not own:** ENGCIM Product Knowledge, Scenario semantics, Skill capability, Control policy, or ENGCIM-specific orchestration semantics.

## 4. Ownership Rules

```text
Product-specific truth                         → Product Knowledge
Mission-specific knowledge selection           → Product Context
Work definition / capability composition       → Scenario
How to perform engineering work                → Skill
What must be governed / proven / enforced      → Control
Shared agent/work orchestration                → Swarm Core
ENGCIM ↔ Multica semantic translation          → Runtime Binding
Generic execution mechanics                    → Multica Runtime
```

> **ENGCIM Swarm v1.0 is the complete scenario-driven engineering system; Swarm Core v1.0 is only its shared orchestration engine.**

## 5. Supervisor Diagnostic Ownership

Diagnosis is **hypothesis-driven**, not a mandatory exhaustive audit of all components.

```text
Finding
→ plausible hypotheses
→ targeted evidence inspection
→ causal chain
→ owningComponent
→ failureMode
→ Improvement Direction
→ Improvement Proposal
→ WorkspaceKnowledge
→ Swarm Dev Team
→ correction / regression
→ Supervisor verification
```

Allowed `owningComponent` values:

```text
PRODUCT_KNOWLEDGE
PRODUCT_CONTEXT
SCENARIO
ENGINEERING_SKILL
ENGINEERING_CONTROL
SWARM_CORE
RUNTIME_BINDING
MULTICA_RUNTIME   # external execution plane
```

The first component where failure becomes visible is not necessarily the owning component.

## 6. Owning Component vs Improvement Direction

```text
Owning Component
= where the causal defect belongs

Improvement Direction
= where the highest-value reusable correction should be made
```

They often match, but are not required to.

Example:

```text
Scenario introduces an overly strict evidence requirement.
Control correctly enforces it.

owningComponent = SCENARIO
```

If repeated evidence shows multiple Scenarios need the same reusable governance capability, the recommended Improvement Direction may instead be `ENGINEERING_CONTROL`.

## 7. Evidence-Driven Execution Blocking

Do not assume the current evidence-blocking problem belongs to Core.

```text
Required product truth absent/wrong
→ PRODUCT_KNOWLEDGE

Truth exists but is not resolved/visible
→ PRODUCT_CONTEXT

Scenario requests inappropriate evidence/completion semantics
→ SCENARIO

Skill cannot safely reason with sufficient but incomplete evidence
→ ENGINEERING_SKILL

Evidence/gate/enforcement policy is incorrect
→ ENGINEERING_CONTROL

Valid composition is mishandled by shared orchestration
→ SWARM_CORE

Evidence/context is lost or mistranslated between ENGCIM and Multica
→ RUNTIME_BINDING

Valid binding is mishandled by generic execution mechanics
→ MULTICA_RUNTIME
```

Target behavior is **evidence-sufficient execution**, not simply weaker evidence requirements:

```text
Critical missing evidence
→ BLOCK

Material but manageable uncertainty
→ continue with explicit uncertainty where permitted

Minor/non-decision-critical evidence gap
→ continue

Unsupported speculation
→ never silently promote to FACT

Hard constraints
→ preserve
```

## 8. Knowledge and Change Authority

```text
Product Knowledge build / refresh
→ ENGCIM Swarm

Workspace Knowledge build / governance / persistence / retrieval
→ ENGCIM Swarm

Mission Inspect / Diagnose / Closure Summary / Mission Learning Source
→ Claude Supervisor

Direct tKMS Platform/Product Knowledge I/O
→ Claude Supervisor
→ Product Knowledge writes remain governed

Shared engineering change
→ Supervisor Diagnosis / Improvement Proposal
→ Swarm Dev Team
→ ENGCIM Swarm implementation/test/regression
→ Supervisor Verify
```

ENGCIM Swarm MUST NOT directly read/write tKMS.

Supervisor MUST NOT directly build/persist WorkspaceKnowledge.

Human owns Mission `DONE`.


## 9. Architecture Invariants

1. Swarm Core remains product-agnostic.
2. Product Knowledge remains separate from mission-specific Product Context.
3. Scenario composes capabilities; it does not reimplement Core, Skill or Control.
4. Skill owns engineering capability; Control owns governance/enforcement.
5. Runtime Binding owns ENGCIM–Multica translation; Multica owns generic execution mechanics.
6. Claude Supervisor remains outside Swarm engineering implementation.
7. Diagnosis identifies causal ownership before correction.
8. Improvement may target a different reusable layer than the immediate owning component.
9. Shared engineering mutation requires Swarm Dev Team authority.
10. Workspace learning is built and persisted by Swarm; Supervisor may selectively read/write tKMS under governance. tKMS is not a mandatory intermediary.

## 10. Freeze Criteria

Freeze this as the canonical ENGCIM Swarm v1.0 component boundary when:

- seven component responsibilities have no material overlap;
- Supervisor can map real findings to `owningComponent`;
- at least one cross-component case demonstrates causal discrimination;
- Runtime Binding vs Multica ownership is operationally distinguishable;
- Swarm Dev Team can correct a component without violating another boundary;
- regression confirms the correction without moving product-specific logic into Core.


## Scenario Lifecycle Profile Compatibility

Engineering Scenario may optionally reference a lifecycle profile:

```text
lifecycleProfileRef?
```

Software Delivery T1–T4 is one such optional lifecycle profile. It is not the universal lifecycle for all ENGCIM Scenarios.


## Diagnostic Scope

Not every failure belongs to an ENGCIM component.

```text
owningScope = COMPONENT | ENVIRONMENT | PROCESS | HUMAN
```

`owningComponent` is required only for `COMPONENT`. Environment/fixture/auth/repository availability failures must not be forced into the 7+1 component taxonomy.


## Execution Identity Boundary

No eighth component and no first-class `ExecutionContext` domain model is introduced.

```text
WorkItem.targetRef
→ Runtime Binding
→ Multica
→ WorkItemResult.attribution
```

Swarm Core owns only semantic Mission / Scenario / WorkItem / Result relationships. Controls validate provenance and revision lineage. Runtime Binding preserves/translates target identity. Multica executes the resolved target.

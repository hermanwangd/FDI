# Feature Delivery Intelligence (FDI) — Master Project Overview

**Project baseline:** `fdi-clean-project-baseline-v0.4.8.3`  
**Document role:** Single-entry project orientation / master project view  
**Audience:** FDI developers, Product/Platform owners, Multica agents, reviewers, and new contributors  
**Authority:** Orientation only. Governing semantics remain in the approved sources referenced by `governance/CURRENT`, `governance/baselines/DB-0001.yaml`, `governance/GOVERNING-SOURCES.md`, and `governance/approved-source-lock.json`.


## Standalone governing-source materialization

This standalone baseline physically vendors the active governing source content under `specs/approved/` and materializes the locked FT-T2 surface under `contracts/ft-t2/`, `skills/ft-t2/`, and `workflows/ft-t2/`. `governance/approved-source-lock.json` resolves every active module ID to a bundle-local path and digest. Multica MUST NOT treat a module ID without a resolvable local file as sufficient authority.

---

## 1. Executive Summary

**Feature Delivery Intelligence (FDI)** is a reusable, Product Owner–oriented agentic product-development system executed on **Multica**.

FDI addresses the part of software delivery that AI coding alone does not solve: converting ambiguous product intent into an evidence-backed, multi-repository delivery specification; reusing durable product knowledge instead of rediscovering it for every Feature; navigating a changing codebase without treating a graph as truth; and independently establishing correctness before delivery is considered complete.

FDI combines three major capabilities:

1. **Layer 1 — Feature Development Execution**: the governed `T1 Intention → T2 Delivery Spec → T3 Implementation → T4 Correctness` workflow.
2. **Layer 2 — Product Intelligence**: durable, governed, cross-Feature reusable Product Knowledge and Delivery Intelligence.
3. **Structural Intelligence Runtime**: rebuildable, exact-source-bound code structure used for bounded discovery, topology navigation, and structural-change sensing.

Multica supplies the **Agent execution control plane**. FDI supplies the **workflow semantics, contracts, evidence rules, Skills, Context boundaries, Product Intelligence governance, and gates**.

> **Short definition:** Build durable Product Intelligence from real product evidence, combine it with live Structural Intelligence, and use both to deliver the next Feature better—without replacing current Feature-specific Evidence as the source of truth.

---

## 2. Problem, Vision, and Strategic Outcome

### Problem

Modern AI coding tools can generate or modify code quickly, but Feature delivery remains slow or unsafe when:

- Product intent is ambiguous or incompletely specified;
- knowledge about capabilities, rules, ownership, and system boundaries is locked in individuals;
- the correct multi-repository Change Surface is difficult to discover;
- delivery history is repeatedly rediscovered;
- architecture/code topology changes faster than manually curated documents;
- implementation scope is inferred from weak historical or graph signals;
- correctness is treated as synonymous with “implementation finished.”

### Vision

FDI turns Feature delivery into an **evidence-governed intelligence workflow**:

```text
Human Feature Signal
        ↓
Product understanding
        ↓
Evidence-backed delivery specification
        ↓
Implementation
        ↓
Independent correctness
        ↓
Reusable learning
        ↓
Product Intelligence becomes richer
```

### Strategic outcome

The goal is not to build a Product Knowledge Graph for its own sake.

> **The goal is a durable organizational Product Intelligence flywheel in which critical Product Knowledge is discovered once, selectively governed, reused across future Features, and continuously refreshed—while current Feature truth remains evidence-gated.**

---

## 3. System Context and Deployment Topology

FDI is easier to understand when the **reusable framework**, **Product-specific knowledge**, **source repositories**, **runtime graph**, and **Feature execution** are shown as different physical concerns.

```text
                         ┌──────────────────────────┐
                         │       Multica            │
                         │  Execution Control Plane │
                         │ agents / models / tasks  │
                         └────────────┬─────────────┘
                                      │ executes
                                      ▼
                         ┌──────────────────────────┐
                         │     FDI Framework Repo   │
                         │ contracts / Skills /     │
                         │ workflows / runtime      │
                         └───────┬─────────┬────────┘
                                 │         │
                    uses Product │         │ uses structural runtime
                    Intelligence │         │
                                 ▼         ▼
                 ┌──────────────────┐   ┌──────────────────────┐
                 │ Product          │   │ Grafel /             │
                 │ Intelligence Repo│   │ CodeIntelligence     │
                 │ e.g. SPC PK Git  │   │ Provider runtime     │
                 └─────────┬────────┘   └──────────┬───────────┘
                           │                       │ indexes / queries
                           │                       ▼
                           │            ┌────────────────────────┐
                           │            │ Product Source Repos   │
                           │            │ repo-01 ... repo-20    │
                           │            │ Azure Repos / Git      │
                           │            └───────────┬────────────┘
                           │                        │
                           └────────────┬───────────┘
                                        ▼
                              ┌──────────────────┐
                              │ Feature Run      │
                              │ intention/spec/  │
                              │ evidence/code/   │
                              │ correctness      │
                              └──────────────────┘
```

### Physical responsibility model

| Physical element | Responsibility |
|---|---|
| **FDI Framework repo** | Reusable software: governing specs, contracts, Skills, runtime, validation harnesses |
| **Product Intelligence repo** | Product-scoped durable organizational knowledge; separate lifecycle and ownership |
| **Product source repos** | Authoritative code/config/schema/test sources |
| **Grafel / CodeIntelligenceProvider** | Rebuildable structural index and query runtime; not Product truth |
| **Multica** | Agent/model/task execution, retries, escalation, cost and run observability |
| **Feature Run workspace** | Ephemeral/current Feature artifacts and evidence |

---

## 4. Three Scopes: Framework vs Product Instance vs Feature Run

Many FDI confusions disappear if every artifact is classified into one of three scopes.

### Level 1 — FDI Framework

Reusable across all Products.

```text
FDI Framework
├── governance and baselines
├── Layer 1 contracts / Skills
├── FT-T2 helper contracts / Skills
├── Layer 2 Product Intelligence contracts / Skills
├── Structural Intelligence runtime
├── source-integration adapters
├── Product Intelligence Store / Registry runtime
├── Multica bindings
└── validation protocols
```

### Level 2 — Product Instance

Specific to one Product such as SPC or APC.

```text
SPC Product Instance
├── Product Intelligence repository
├── Product / Sub-product / Capability knowledge
├── Product Realization topology
├── Delivery Intelligence
├── PA-03 repository inventory
├── PA-05 delivery history
├── Product source bindings
└── Grafel Product group / structural snapshots
```

### Level 3 — Feature Run

Specific to one Feature execution.

```text
Feature SPC-F123
├── intention.md
├── CandidateRepoSet
├── ChangeSurfaceSet
├── EvidenceRecord
├── ClosurePackage / ClosureReview
├── spec.md
├── implementation artifacts
├── correctness artifacts
└── Agent/task/run evidence
```

### Rule

> **Framework artifacts define how FDI works. Product artifacts capture durable Product Intelligence. Feature artifacts capture what is true for one current Feature.**

A Feature artifact may propose reusable knowledge back to Layer 2, but it does not become a Product Asset without the applicable publication governance.

---

## 5. Overall FDI Architecture

```text
                              PRODUCT OWNER / FEATURE OWNER
                                         │
                     ┌───────────────────┴───────────────────┐
                     │                                       │
              Maintain Product                         Develop Feature
                     │                                       │
                     ▼                                       ▼
        ┌────────────────────────┐             ┌──────────────────────────┐
        │ Layer 2                │             │ Layer 1                  │
        │ Product Intelligence   │────────────▶│ Feature Development      │
        │                        │  Context     │ Execution                │
        │ Product Semantics      │             │                          │
        │ Product Realization    │             │ T1 Intention             │
        │ Delivery Intelligence  │             │      ↓                   │
        │                        │             │ T2 Delivery Spec         │
        │ Durable / governed     │             │   └─ FT-T2 Closure       │
        └───────────┬────────────┘             │      ↓                   │
                    │                          │ T3 Implementation        │
                    │                          │      ↓                   │
                    │                          │ T4 Correctness           │
                    │                          └────────────┬─────────────┘
                    │                                       │
                    │                         reusable       │
                    │                         learning       │
                    └───────────────────────◀───────────────┘

                              ▲
                              │ bounded structural queries
                              │
                    ┌─────────┴────────────────────┐
                    │ Structural Intelligence     │
                    │ Runtime                     │
                    │                             │
                    │ CodeIntelligenceProvider    │
                    │ GrafelAdapter (MVP)         │
                    │ exact source binding        │
                    │ graph / paths / topology    │
                    │ structural diff             │
                    │                             │
                    │ Rebuildable / non-authority │
                    └─────────────┬───────────────┘
                                  │
                                  ▼
                         PRODUCT / CODE SOURCES
                    Git / Azure Repos / docs / issues /
                       PRs / commits / interfaces

        ───────────────────────────────────────────────────────────────
                         MULTICA EXECUTION CONTROL PLANE
        Agents · models · tasks · squads · retries · escalation · cost
        ───────────────────────────────────────────────────────────────
```

---

## 6. Core Authority Principle

FDI deliberately separates **durable knowledge**, **live structure**, and **current Feature truth**.

```text
Durable Product Intelligence
       +
Live Structural Intelligence
       ↓
understand / constrain / prioritize / generate candidates
       ↓
Current Feature Investigation
       ↓
current Feature-specific pinned Evidence
       ↓
Current Change Surface Truth
```

### May guide investigation

- Product Semantics;
- Product Realization;
- Delivery Intelligence / historical delivery patterns;
- Grafel structural paths, topology, relations, and diffs;
- PA-03 repository identity and high-value relations;
- governed reference material.

### Establishes current truth

Only **current Feature-specific pinned Evidence** may establish current:

```text
CONFIRMED
EXCLUDED
ChangeSurfaceSet truth
SPEC_READY
```

Neither historical co-change nor a Grafel graph edge is sufficient by itself.

---

## 7. Layer 1 — T1 to T4

Layer 1 is the governing Feature workflow.

```text
Human Feature Signal
        ↓
T1 Intention
        ↓
T2 Delivery Spec
        ↓
T3 Implementation
        ↓
T4 Correctness
```

### T1 — Intention

Establishes what the Feature is trying to achieve:

- desired outcome;
- scope / non-scope;
- Product/capability identity;
- success criteria;
- durable constraints;
- unresolved questions.

### T2 — Delivery Spec

Converts accepted intention into an evidence-backed implementation-ready specification.

T2 owns **current Feature Change Surface discovery**.

The sole canonical T2 gate remains:

```text
SPEC_READY | BLOCKED
```

### T3 — Implementation

Executes the approved specification across required repositories and surfaces.

Structural navigation may assist implementation, but it may not silently redefine the approved T2 scope.

### T4 — Correctness

Independently establishes candidate correctness against the governing Intention and Spec.

Implementation completion does not automatically imply correctness.

---

## 8. Canonical Artifact Flow

This table is the quickest way to see what each stage consumes and produces.

| Stage | Primary input | Canonical / governing output | Key gate or decision |
|---|---|---|---|
| **T1** | Human Feature Signal + applicable Context | `intention.md` / Intention contract | Intention sufficiently resolved for T2 |
| **T2** | accepted Intention + Product Intelligence + Structural hints + current Evidence | `spec.md` / Delivery Spec; current Change Surface | `SPEC_READY \| BLOCKED` |
| **FT-T2 helper flow** | IntentSpec + candidate investigation | `CandidateRepoSet`, `ChangeSurfaceSet`, `EvidenceRecord`, `ClosurePackage`, `ClosureReview` | helper closure only; cannot authorize T3 |
| **T3** | approved Spec + exact current source state | implementation artifacts | implementation candidate complete enough for T4 |
| **T4** | Intention + Spec + implementation + independent evidence | correctness artifacts / decision | correctness decision |
| **Layer 2** | pinned sources + observations + qualified proposals | Product Asset revisions | publication governance / lifecycle decision |
| **Structural runtime** | exact source-bound snapshot | observations / discovery hints / deltas | non-authoritative runtime result |

### Critical artifact rule

```text
Product Asset ≠ Feature Evidence
Structural Observation ≠ Feature Evidence
Historical Feature ≠ Current Change Surface
Implementation Complete ≠ Correct
```

---

## 9. FT-T2 Feature Closure

**Feature Closure is subordinate to T2. It is not Layer 1 itself and not a fifth canonical stage.**

The locked HERM-211 helper surface contains exactly six helper contracts:

```text
IntentSpec
CandidateRepoSet
ChangeSurfaceSet
EvidenceRecord
ClosurePackage
ClosureReview
```

and five helper Skills:

```text
feature-intent-analysis
repo-discovery
changesurface-investigation
dependency-closure
closure-review
```

Helper closure status:

```text
OPEN
PARTIAL
CLOSED_WITHIN_DECLARED_SCOPE
```

`CLOSED_WITHIN_DECLARED_SCOPE` does **not** imply `SPEC_READY` and does not authorize T3.

---

## 10. Layer 2 — Product Intelligence

Layer 2 exists because valuable Product Knowledge should not be rediscovered from scratch for every Feature.

```text
Product Sources
      ↓
Evidence / Observations
      ↓
Agentic Synthesis
      ↓
ProductAssetProposal
      ↓
Layer 2 Governance
      ↓
PUBLISHED + ACTIVE Product Assets
      ↓
Registry / Context Resolution
      ↓
Layer 1
```

### Product Asset families

```text
Product
Architecture
Codebase
Domain
Delivery History
Operations
Knowledge
Reference
```

For approved Layer 2 v0.1 profile specification, **PA-03 Codebase** and **PA-05 Delivery History** are fully specified profiles. PA-01 Product Semantics remains a proposal until separately promoted through governing approval.

---

## 11. Product Knowledge Model

Asset families describe **how knowledge is governed and maintained**. Layer 1 consumption is easier to understand through three knowledge roles.

### 11.1 Product Semantics

Answers:

> **What does the Product mean and what behavior/rules matter?**

```text
Product
  ↓
Sub-product
  ↓
Capability
  ↓
Behavior
  ├─ Domain concepts
  ├─ Business rules
  ├─ Identity / correlation
  ├─ State / eligibility
  ├─ Fallback / exception
  └─ Durable invariants
```

### 11.2 Product Realization

Answers:

> **Where and how is that Product meaning implemented?**

```text
Capability
   ↓ REALIZED_BY
System / Component
   ↓
Interface / Data Contract
   ↓
Repository
   ↓
Module / Schema / Config / Test
```

Canonical representation is typed many-to-many relations. `Product → Repo` is a derived view, not the sole authority model.

### 11.3 Delivery Intelligence

Answers:

> **Where did similar Product changes happen before?**

```text
Historical Feature
      ↓
PR / Commit
      ↓
Affected realization nodes
      ↓
Repos / files / interfaces / schemas / tests
      ↓
Reusable delivery patterns
```

Delivery Intelligence is a prior for candidate discovery. It never proves current applicability by itself.

### Four-sentence interface rule

> **Semantics tells Layer 1 what the Product means.**  
> **Realization tells Layer 1 where that meaning is implemented.**  
> **Delivery Intelligence tells Layer 1 where similar meanings changed before.**  
> **Current Evidence tells Layer 1 what actually has to change now.**

---

## 12. Structural Intelligence Runtime

Structural Intelligence is a shared FDI runtime capability, not a Product Asset family and not a new Layer 1 stage.

Its purpose is a **bounded, queryable, rebuildable structural map of the exact source snapshot**.

### Provider-neutral interface

FDI depends on a `CodeIntelligenceProvider`, conceptually supporting:

```text
orient
find
expand
trace
diff
```

### Grafel

For the MVP, **Grafel is the preferred provider implementation** behind `GrafelAdapter`.

Provider-specific tool names and raw graph schemas remain adapter-local.

Structural Intelligence may support:

- multi-repository candidate discovery;
- API/caller/consumer exploration;
- event/schema topology;
- implementation navigation;
- impact hypotheses;
- Product Realization bootstrap;
- realization staleness detection;
- structural diff.

It may not directly publish Product Assets or establish current Feature truth.

### Exact source binding

```text
Azure/Git source
   ↓
exact local Git revision / isolated worktree
   ↓
StructuralSnapshotRef
   ↓
provider graph binding
   ↓
SnapshotBindingAttestation
   ↓
bounded structural query
```

Source binding must preserve full Git revision identity and provider/runtime/adapter provenance.

---

## 13. Source Integration

Source providers are acquisition mechanisms, not semantic authorities.

### Azure Repos MVP boundary

```text
Azure Repos
    ↓ clone / fetch
isolated local Git worktree
    ↓
resolve exact full commit SHA
    ↓
FDI SourceSnapshot / StructuralSnapshot
    ↓
Grafel / Layer 2 acquisition
```

Credentials remain outside the project repository.

A branch name, mutable HEAD, remote URL, or Grafel index label is not sufficient when an exact revision is required.

---

## 14. Product Intelligence Store

Durable Product Knowledge accumulates in the **Layer 2 Product Intelligence Store**.

### MVP physical strategy

> **One Product-owned, Product-scoped Git repository is the reference durable Product Intelligence Store.**

It is separate from the reusable FDI framework repository because Product Intelligence has different owners, lifecycle, review authority, and release cadence.

```text
ProductAssetGovernance
       ↓ publication / lifecycle decision
ProductAssetRepository
       ↓
GitStoreAdapter
       ↓
Product Intelligence Git repository
       ↓
Derived ProductAssetRegistry
       ↓
ContextRequirement
       ↓
ProductAssetRef / ResolvedContextRef
       ↓
Layer 1
```

The Registry is a selection/navigation projection, not an independent Product-truth source.

The v0.4.8.3 `GitStoreAdapter` remains incomplete until immutable publication, lifecycle transitions, supersession/retirement, conflict handling, and atomic/rebuildable Registry behavior are finished.

---

## 15. Three Product-Level Workflows

### 15.1 Bootstrap Product

```text
Define bounded Product scope
      ↓
Bind exact Product sources
      ↓
Repository inventory
      ↓
Structural analysis
      ↓
Delivery-history reconstruction
      ↓
Product Knowledge synthesis
      ↓
ProductAssetProposal
      ↓
Accountable review / publication
      ↓
Initial Product Intelligence baseline
```

Bootstrap should generate proposals first; Product Teams should not have to author the whole baseline manually from an empty page.

### 15.2 Maintain Product

**Source-driven loop**

```text
repo/API/schema/ownership change
        ↓
structural/source delta
        ↓
affected Product Assets
        ↓
stale/conflict/review signal
        ↓
ProductAssetProposal
        ↓
governance
```

**Feature-driven learning loop**

```text
Feature execution
     ↓
new reusable learning?
     ↓ yes
ProductAssetProposal
     ↓
governance
```

Layer 1 may propose learning but never silently publish it.

### 15.3 Develop Feature

```text
Human Feature Signal
      ↓
T1 Intention
      ↓
Feature Knowledge demand
      ↓
Product Intelligence
+
Structural Intelligence
      ↓
Candidate investigation
      ↓
current Feature-specific Evidence
      ↓
ChangeSurfaceSet
      ↓
T2 Delivery Spec
      ↓
SPEC_READY | BLOCKED
      ↓
T3 Implementation
      ↓
T4 Correctness
```

---

## 16. Governance and Authority

FDI uses **claim-specific authority**, not one global source ranking.

| Claim | Primary authority |
|---|---|
| Desired Product outcome | latest authorized Intention |
| Technical obligation | latest approved Spec subordinate to Intention |
| Durable Product semantics | governed Product Assets / applicable authoritative source |
| Repository identity | approved PA-03 / pinned source identity |
| Current code behavior | pinned current source/config/schema/tests/evidence |
| Current Change Surface | current Feature-specific pinned Evidence through T2 |
| Procedure | governed Skill + permitted runtime capabilities |
| Historical rationale/prior | qualified Delivery Intelligence / history |
| Agent execution mechanics | Multica |

When sources conflict, FDI asks **which source has authority for the disputed claim**, not which source type has the highest global rank.

### Governing baseline

```text
governance/CURRENT
governance/baselines/DB-0001.yaml
governance/approved-source-lock.json
governance/GOVERNING-SOURCES.md
```

This Overview does not override them.

---

## 17. Multica Responsibility Boundary

Multica is the execution control plane.

Multica may manage:

```text
Agents
models
Squads
tasks
parallelism
retries
Basic → Advanced escalation
execution lifecycle
run provenance
token / cost / cycle-time observability
```

Multica must not redefine:

```text
T1–T4 semantics
HERM-211 helper contracts
Product Asset semantics
Context authority
Evidence rules
publication rules
SPEC_READY | BLOCKED
```

A Multica Project/Workspace is an operational container, not a new semantic layer.

---

## 18. Project Artifacts vs Runtime Artifacts

### FDI software project builds

```text
Governance / baseline controls
Layer 1 specs, contracts and Skills
FT-T2 helper contracts and Skills
Layer 2 Product Intelligence contracts / Skills
Structural Intelligence abstractions
Source-integration adapters
Product Intelligence Store / Registry runtime
Multica bindings and instructions
DEV-204 / F001 validation protocols
Product Intelligence repository template
```

### Product-scoped FDI instance creates

```text
Product Assets and revisions
ProductAssetProposals
source / structural observations
Product Intelligence Registry entries
```

### Feature run creates

```text
intention.md
spec.md
CandidateRepoSet
ChangeSurfaceSet
EvidenceRecord
ClosurePackage / ClosureReview
implementation artifacts
correctness artifacts
Agent/task/run evidence
cost/token/cycle-time records
```

---

## 19. Decision and Maturity Matrix

This matrix distinguishes **design approval**, **contract maturity**, **implementation**, and **proof**. “Implemented” never means “empirically proven.”

| Capability | Design / authority | Contract maturity | Local implementation | Live / empirical proof |
|---|---|---|---|---|
| Layer 1 T1–T4 | Approved governing baseline | Approved | represented in clean baseline | end-to-end production proof pending |
| FT-T2 Feature Closure | Approved / locked helper surface | Approved six contracts + five Skills | represented | DEV-204 pending |
| PA-03 Codebase | Approved | Fully specified v0.1 profile | scaffold/reference implementation present | real Product bootstrap pending |
| PA-05 Delivery History | Approved | Fully specified v0.1 profile | scaffold/reference implementation present | real Product binding pending |
| PA-01 Product Semantics | Proposal only | not governing-approved | partial proposal/runtime support | cannot claim canonical PA-01 publication |
| Product Knowledge roles | FDI design decision | helper/runtime contracts present | partial | real Product usefulness pending |
| Git Product Intelligence Store | MVP physical decision | partial | incomplete | production lifecycle proof pending |
| Azure Repos source binding | MVP source decision | defined | scaffold | live exact binding pending |
| Structural Intelligence | FDI runtime decision | provider-neutral contracts defined | scaffold / adapter present | live Grafel exact binding pending |
| Grafel | preferred MVP provider | adapter-local, non-governing | integration scaffold | live Product graph proof pending |
| DEV-204 | approved validation direction | protocol/harness area exists | prepared | fresh-context RED/GREEN not executed |
| F001 Full FDI hypothesis | frozen evaluation hypothesis | four-arm protocol area exists | prepared | calibration not executed |
| Full FDI | architectural hypothesis | integrated contracts | local integration only | empirical uplift not established |

### Reading this table

- **Approved** means governing authority exists.
- **Implemented** means code/artifacts exist locally.
- **Live proof** means tested against real source/runtime conditions.
- **Empirical proof** means the MVP hypothesis survives evaluation.

---

## 20. End-to-End SPC Example — FFW Resolution by Wafer ID

This example is illustrative; it demonstrates the intended flow and authority boundaries rather than asserting current SPC production facts.

### Feature request

```text
"Sampling Evaluation must support FFW resolution by wafer ID."
```

### T1 — Intention

FDI identifies the intended Product area:

```text
Product: SPC
Sub-product: Rule Management
Capability: Sampling Evaluation
Desired behavior: wafer-specific FFW resolution
```

Applicable Product Semantics may provide concepts such as:

```text
Wafer
FFW
Sampling Evaluation
identity / correlation rules
```

These help interpret intent; they do not prove current implementation scope.

### Product Realization prior

Layer 2 may indicate:

```text
Sampling Evaluation
   ↓ REALIZED_BY
Sampling Service
   ↓ IMPLEMENTED_IN
spc-rule-service

Wafer identity context
   ↓ REALIZED_BY
Wafer Context Service
   ↓ IMPLEMENTED_IN
wafer-context
```

### Delivery Intelligence prior

Historical delivery may indicate that similar changes previously touched:

```text
spc-rule-service
wafer-context
rule-engine
history-service
```

Again, this is a prior, not current truth.

### Structural Intelligence

A bounded Grafel query may reveal:

```text
sampling-service
    ↓ HTTP/CALL
wafer-context-service

sampling-service
    ↓ CALL
rule-engine

sampling-service
    ↓ EVENT
history-service
```

This generates structural discovery hints.

### CandidateRepoSet

After PA-03 repository grounding, candidate investigation may include:

```text
spc-rule-service
wafer-context
rule-engine
history-service
```

### Current Feature investigation

Pinned current evidence determines actual dispositions:

```text
spc-rule-service     CONFIRMED
wafer-context        CONFIRMED
rule-engine          EXCLUDED
history-service      EXCLUDED
```

The above dispositions are only valid when backed by current Feature-specific Evidence.

### T2 result

```text
ChangeSurfaceSet
    ↓
spec.md
    ↓
SPEC_READY
```

If material uncertainty remains, the correct result is:

```text
BLOCKED
```

### T3 / T4

```text
SPEC_READY
   ↓
T3 implementation in confirmed surfaces
   ↓
T4 independent correctness evidence
```

### Learning loop

If the Feature reveals a durable reusable rule or realization mapping:

```text
new reusable finding
   ↓
ProductAssetProposal
   ↓
Layer 2 governance
   ↓
PUBLISHED + ACTIVE only if authorized
```

The next Feature can then start with better Product Intelligence.

---

## 21. MVP Hypothesis

The MVP is not merely testing whether a repository index improves search.

> **Does Feature Delivery Intelligence—combining governed Product Intelligence, bounded live Structural Intelligence, and current-evidence-gated investigation—improve fresh-agent Change Surface discovery quality and efficiency?**

### F001 four-arm calibration

| Arm | Product Intelligence | Structural Intelligence |
|---|---:|---:|
| A — Baseline | No | No |
| B — Structural only | No | Yes |
| C — Product Intelligence only | Yes | No |
| D — Full FDI | Yes | Yes |

All arms share the same Feature signal, pre-Feature cutoff, model/configuration, execution budget, evaluation rubric, and identity-only repository substrate where needed.

No target implementation, target PR/commit, post-cutoff intelligence, future Product Asset, or GroundTruth may leak into an execution arm.

F001 decision vocabulary:

```text
CONTINUE | REVISE | STOP
```

If `CONTINUE`, F002–F005 become blind holdouts whose primary comparison is:

```text
A Baseline
   vs
D Full FDI
```

---

## 22. MVP Definition of Done

FDI MVP is **not DONE** because local tests pass or because all framework components exist.

The MVP is complete only when all of the following are satisfied:

```text
G1  Governing baseline source bytes are rehydrated and locked.
G2  One real Product is bound to exact Product/source identities.
G3  Minimum-useful real PA-03 / PA-05 Product Intelligence exists.
G4  Product Intelligence Store publication + Registry behavior is operational.
G5  Real Azure/Git exact source binding passes.
G6  Real Grafel / StructuralSnapshot exact binding passes.
G7  DEV-204 fresh-context RED/GREEN behavioral validation is completed.
G8  F001 four-arm calibration produces CONTINUE under frozen evaluation rules.
G9  F002–F005 blind holdouts are completed without information leakage.
G10 Full FDI shows material Feature-delivery uplift on agreed quality/efficiency metrics.
G11 No authority regression occurs: history/graph/Product Intelligence still cannot establish current Change Surface truth without current Evidence.
```

### MVP outcome

At the end of these gates, the MVP must be classified explicitly as one of:

```text
PROVEN_ENOUGH_TO_CONTINUE
REVISE_AND_RETEST
REJECT_MVP_HYPOTHESIS
```

The project should not extend the architecture merely to avoid making that decision.

---

## 23. Success Metrics

FDI should not be evaluated by graph-node count, Asset count, or whether Agents merely run.

### Feature delivery quality

- Change Surface recall / precision;
- critical repository misses;
- false inclusion / false exclusion;
- unsupported required claims;
- false `SPEC_READY` / false closure;
- correctness quality.

### Product Intelligence quality

- reuse per Feature;
- repeated rediscovery rate;
- stale/conflicted Asset rate;
- proposal acceptance/correction rate;
- traceability;
- maintenance effort.

### Execution efficiency

- tool calls;
- token usage and model cost;
- cycle time;
- retries / escalation;
- human clarification/correction effort.

The key proof is whether FDI improves Feature delivery **without weakening evidence authority**.

---

## 24. Current Project Status — v0.4.8.3

The clean baseline is a **normal active software project baseline**, not a recovery workflow.

### Present

- clean project structure and governance boundary;
- Layer 1 / FT-T2 / Layer 2 conformance material;
- exact six FT-T2 helper contracts and five Skills represented;
- Product Knowledge model and Product Intelligence Store design;
- provider-neutral Structural Intelligence contracts;
- Grafel adapter / binding scaffolding;
- Azure Repos local snapshot-provider scaffolding;
- Product Intelligence Git repository template;
- DEV-204 and F001 validation areas;
- schema/unit verification baseline.

### Incomplete / external

```text
Approved source bytes rehydrated + digest pinned        PENDING
Complete Git Product Intelligence Store lifecycle       PENDING
Live Azure Repos exact source binding                   PENDING
Live Grafel exact binding                               PENDING
Real PA-03 Product bootstrap                            PENDING
PA-01/Product Semantics governing promotion             NOT APPROVED
Publication + production Registry behavior              PENDING
DEV-204 fresh-context RED/GREEN behavioral proof        NOT EXECUTED
F001 four-arm calibration                               NOT EXECUTED
Empirical FDI uplift                                    NOT ESTABLISHED
Routine Product Owner production readiness              NOT ESTABLISHED
```

Deterministic local tests are development evidence only; they are not behavioral or empirical proof.

---

## 25. Development Roadmap

```text
DEV-218  Rehydrate approved governing source bytes
          ↓
DEV-219  Complete Git Product Intelligence Store
          ↓
DEV-220  Azure Repos exact source binding
          ↓
DEV-221  Live Grafel exact binding
          ↓
DEV-222  PA-03 bootstrap
          ↓
DEV-223  Product Knowledge synthesis / PA-01 proposal
          ↓
DEV-224  Publication + Context Registry
          ↓
DEV-204  Fresh-context Agent RED/GREEN
          ↓
F001     Four-arm calibration
          ↓
CONTINUE | REVISE | STOP
          ↓ if CONTINUE
F002–F005 blind holdout
```

`DEV-218` is mandatory before changing governing semantics or claiming a new canonical digest, but it does not need to block independent non-semantic implementation work.

### Current milestone interpretation

The next major milestone is not “more architecture.” It is:

```text
one real Product
+ exact source binding
+ minimum-useful Product Intelligence
+ live Structural Intelligence
+ one fresh-context behavioral proof
```

---

## 26. Repository Map — Where to Look

```text
fdi-clean-project-baseline-v0.4.8.3/
│
├── PROJECT-OVERVIEW.md                 ← START HERE
├── README.md                           ← package usage / local commands
├── MULTICA-HANDOFF.md                  ← continuation instructions
├── DEVELOPMENT-BACKLOG.md              ← ordered work
├── STATUS.json                         ← current machine state
│
├── governance/                         ← authority / baselines / ADRs
│   ├── CURRENT
│   ├── baselines/
│   ├── decisions/
│   └── approved-source-lock.json
│
├── specs/                              ← human-readable contracts/design
│   ├── layer1/
│   ├── ft-t2-feature-closure/
│   ├── layer2/
│   ├── structural-intelligence/
│   ├── source-integration/
│   └── proposals/PA-01/
│
├── contracts/                          ← machine-readable schemas
│   ├── layer1/
│   ├── ft-t2/
│   ├── layer2/
│   ├── structural-intelligence/
│   └── source-integration/
│
├── skills/                             ← governed Agent procedures
│   ├── layer1/
│   ├── ft-t2/
│   └── layer2/
│
├── workflows/                          ← Product-level workflows
│   ├── BOOTSTRAP-PRODUCT.md
│   ├── MAINTAIN-PRODUCT.md
│   └── DEVELOP-FEATURE.md
│
├── src/main/java/                      ← Java 17 / Spring Boot reference runtime
│   ├── product_intelligence/
│   ├── structural_intelligence/
│   └── source_integration/
│
├── templates/product-intelligence/     ← Product-owned PK Git repo template
│
├── validation/
│   ├── dev204/                         ← behavioral RED/GREEN validation
│   └── f001/                           ← four-arm calibration
│
├── config/                             ← Multica / Grafel / Azure examples
├── scripts/                            ← verification / packaging helpers
├── tests/
└── archive/                            ← non-active recovery/superseded material
```

---

## 27. Recommended Read Order and Non-Goals

### Read order

For a new human or Agent:

1. **`PROJECT-OVERVIEW.md`** — understand the full system.
2. `governance/CURRENT` — identify the active baseline.
3. `governance/baselines/DB-0001.yaml` — inspect the governing set.
4. `governance/GOVERNING-SOURCES.md` — understand authority/source precedence.
5. `STATUS.json` — inspect current implementation and external gates.
6. `DEVELOPMENT-BACKLOG.md` — inspect next actions.
7. Relevant `specs/`, `contracts/`, and `skills/` for the task being changed.
8. `MULTICA-HANDOFF.md` — continue execution in Multica.

Do **not** begin by recursively reading `archive/`.

### Non-goals

The MVP intentionally does not attempt to build:

- a complete enterprise Knowledge Graph;
- a permanent copy of the full Grafel graph;
- a scheduler/model router replacing Multica;
- connectors for every engineering platform;
- automatic promotion of Agent observations into Product truth;
- unlimited code/history ingestion into Agent context;
- a new Layer 1 transition;
- a Grafel-specific current-truth authority;
- a Product-wide model that must be complete before any Feature can run.

FDI should maintain only Product Intelligence whose expected reuse value exceeds its maintenance cost.

---

## 28. End-State Operating Model

```text
                           FIRST PRODUCT ONBOARDING

Product repos + docs + delivery history
                ↓
exact source acquisition
                ↓
Structural Intelligence + deterministic observations
                ↓
Product Knowledge proposals
                ↓
Product Team / accountable owner governance
                ↓
minimum-useful Product Intelligence


                               EVERY FEATURE

Human Feature Signal
        ↓
Product Intelligence + Structural Intelligence
        ↓
T1 / T2 bounded Feature Discovery
        ↓
current Evidence-backed Change Surface
        ↓
T3 Implementation
        ↓
T4 Correctness
        ↓
reusable learning proposal
        ↓
Layer 2 governance
        ↓
Product Intelligence becomes richer
        ↓
next Feature starts smarter
```

### End-state definition

FDI is a reusable agentic Product-development system executed on Multica. Layer 2 continuously acquires, synthesizes, governs, and accumulates reusable Product Intelligence; Structural Intelligence supplies exact-source-bound, rebuildable code topology and navigation; Layer 1 uses those inputs to execute the governed T1 → T2 → T3 → T4 workflow. Product Intelligence and Structural Intelligence may guide candidate discovery, but current Feature-specific pinned Evidence remains authoritative for the current Change Surface and `SPEC_READY | BLOCKED` gate. Completed Feature delivery may propose reusable learning back into Layer 2, creating an organizational Product Intelligence flywheel without allowing Agents, history, Grafel, or Multica to silently redefine Product truth.

> **The project is complete when the framework, Product Intelligence, Structural Intelligence, and evidence-gated Feature workflow work together on a real Product and demonstrate measurable delivery improvement—not merely when all components exist.**

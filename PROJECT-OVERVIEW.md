# FDI Software Factory

The active ENGCIM Swarm Core v1.0 architecture is documented in
[`docs/engcim-swarm/ENGCIM-SWARM-CORE-ARCHITECTURE-FREEZE-v1.0.md`](docs/engcim-swarm/ENGCIM-SWARM-CORE-ARCHITECTURE-FREEZE-v1.0.md)
and the corresponding model is in
[`docs/engcim-swarm/ENGCIM-SWARM-MODEL-v1.0.md`](docs/engcim-swarm/ENGCIM-SWARM-MODEL-v1.0.md).

## Purpose

FDI Software Factory turns governed Product Knowledge into verifiable Feature
delivery while preserving the boundary between Product meaning, engineering
design, execution, and independent correctness verification.

The current minimum operating target is `SF-BL-001 — Product-Knowledge-Assisted
Feature Delivery MVP`. It will build accepted Product Context from one
exact-revision SVSPC codebase and training material, deliver the same SPC Chart
Management Feature through isolated Code Only and Product Knowledge arms, and
measure whether Product Knowledge materially improves delivery.

Before that MVP consumes the PKB-001 foundation, `SF-BL-002` performs one
bounded foundation correction: distinguish production-reference resolution
gaps from external/test-helper diagnostics and permit evidence-backed,
proposal-only scenario-to-production mapping. It does not reopen PKB-001 or
change Product meaning.

## Active project truth

Read these project-truth controls in order:

1. `PROJECT-OVERVIEW.md`
2. `FRAMEWORK-SPEC.md`
3. `BACKLOG.md`
4. `IMPLEMENTATION-PLAN.md`
5. `STATUS.json`

`AGENTS.md` defines agent/workspace operating instructions; it is not an
independent source of Product or project truth. Code, contracts, Skills, tests,
and evidence support the controls but do not override them. Everything under
`archive/` is historical reference only.

## Responsibilities

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

- **Human Authority** owns Product meaning, material scope and architecture
  changes, Acceptance Criteria changes, publication, deployment, and terminal
  closure.
- **Product Knowledge** turns heterogeneous sources into observations and
  proposals; only Human Authority acceptance creates durable Product Knowledge.
  Accepted Product Knowledge is reusable across applicable Engineering
  Scenarios through exact-versioned, provenance-bound Product Context.
- **Engineering Scenario** is the reusable composition boundary for Product
  Context, Skills, Controls, evidence, and execution. It does not assume every
  Scenario is software delivery.
- **Software Delivery Profile** is optional. When present, it owns the
  existing T1–T4 progression, engineering contracts, replanning, correctness
  routing, and closure proposals. T1–T4 do not govern all Scenarios.
- **Execution Plane** executes approved WorkItems, coordinates eligible
  parallelism, retry, review, integration, regression, and evidence assembly.
  Multica may provide this generic runtime but is not an authority identity or
  Core contract dependency.

The Software Factory analyzes verified independent-review, verification,
execution, delivery, and KPI evidence to recommend bounded improvements to
tests, delivery controls, engineering plans, or Product Knowledge. A
recommendation creates no implementation, Product, project-truth, Backlog
selection, or closure authority. Every resulting change follows the existing
Human Authority, Feature Delivery, Execution, verification, and Product
Knowledge paths.

Unrelated Backlog items may execute in separate lanes when each has an exact
envelope, isolated workspace, non-overlapping mutation ownership, independent
evidence and closure, and a bounded share of the aggregate resource limit.
Active Control state remains serialized and maintained by one Feature Delivery
Plane owner.

The current Execution Plane runtime may be Multica. Core contracts depend only
on the role boundary, never on a vendor, model, agent, or orchestration product.

The validated Phase 2 Control vocabulary is:

```text
CTRL-AUTHORIZATION-001
CTRL-EXECUTION-SAFETY-001
CTRL-REPOSITORY-PROVENANCE-001
CTRL-EXACT-BINDING-001
CTRL-INDEPENDENT-EVALUATION-001
CTRL-EVIDENCE-INTEGRITY-001
CTRL-FINDING-RESOLUTION-001
```

Control applicability and required evidence remain governed by the frozen
validation baseline.

## Delivery flow

```text
Engineering Scenario
→ exact Product Context, Skills, Controls, and evidence binding
→ optional Software Delivery Profile:
   T1 IntentSpec + frozen Acceptance Criteria
   → T2 System Analysis + ChangeSurface + TechnicalDesign + DeliverySpec
   → T3 ExecutionPlan DAG + WorkItems
   → Execution Plane implementation and integration
   → T4 PASS | FAIL | INCONCLUSIVE
   → ENGINEERING_READY when PASS
```

For a Scenario with a Software Delivery Profile, T4 failure normally returns
to T3 remediation/replanning. A TechnicalDesign defect returns to T2. T1
Acceptance Criteria never change merely to make T4 pass; a Product-intent
change stops the current cycle and requires Human Authority to create a new
IntentSpec and delivery cycle. Scenarios without the profile do not inherit
these T1–T4 stages.

## Product Knowledge boundary

Graphify provides structural observations. Repository tests provide behavioral
evidence. Git, pull requests, and delivery history provide delivery evidence.
None establishes Product truth automatically. Reverse discovery always produces
proposals for Human review.

A delivery finding enters Product Knowledge only when it contains genuine
Product-learning evidence. It remains an Observation or Proposal under the
existing Product Knowledge process until Human Authority accepts it. Engineering,
execution, orchestration, and tooling findings must not be represented as
Product Knowledge merely to obtain an authority or publication path.

PKB-001 remains immutable validation history and provides reusable Java,
Graphify-provider, scenario, reverse-discovery, and evaluation foundations. It
does not by itself prove that Product Knowledge improves real Feature delivery;
`SF-BL-001` measures that question through the controlled two-arm MVP.

## Cross-baseline change portability

External and company repositories may evolve without a shared Git commit
baseline. FDI may export exact-revision, cross-file change references containing
bounded before/after excerpts, intent and verification context, and digest-bound
provenance for company-side adaptation. Such a package is reference evidence,
not an applicable patch: it cannot modify the company repository, replace its
active controls, or establish company project truth.

## Technology boundary

Executable FDI framework behavior uses Java 17 and Spring Boot 3.4.1. The
external Graphify Python MCP runtime remains outside the framework behind the
Java `CodeIntelligenceProvider` adapter. FDI must verify the installed runtime
instead of assuming Graphify APIs.

`PASS` means `ENGINEERING_READY`, not deployed, published, or delivered to
users.

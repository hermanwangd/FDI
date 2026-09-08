# FDI Software Factory

## Purpose

FDI Software Factory turns governed Product Knowledge into verifiable Feature
delivery while preserving the boundary between Product meaning, engineering
design, execution, and independent correctness verification.

The current minimum operating target is `SF-BL-001 — Product-Knowledge-Assisted
Feature Delivery MVP`. It will build accepted Product Context from one
exact-revision SVSPC codebase and training material, deliver the same SPC Chart
Management Feature through isolated Code Only and Product Knowledge arms, and
measure whether Product Knowledge materially improves delivery.

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
Product Knowledge
      ↓ Resolved Product Context
Feature Delivery: T1 → T2 → T3 → T4
                         ↓
                  Execution Boundary
                         ↓
                   Execution Plane
```

- **Human Authority** owns Product meaning, material scope and architecture
  changes, Acceptance Criteria changes, publication, deployment, and terminal
  closure.
- **Product Knowledge** turns heterogeneous sources into observations and
  proposals; only Human Authority acceptance creates durable Product Knowledge.
- **Feature Delivery Plane** owns the five controls, T1–T4 progression,
  engineering contracts, replanning, correctness routing, and closure proposals.
- **Execution Plane** executes approved WorkItems, coordinates eligible
  parallelism, retry, review, integration, regression, and evidence assembly.

The current Execution Plane runtime may be Multica. Core contracts depend only
on the role boundary, never on a vendor, model, agent, or orchestration product.

## Delivery flow

```text
Accepted Product Knowledge
→ T1 IntentSpec + frozen Acceptance Criteria
→ T2 System Analysis + ChangeSurface + TechnicalDesign + DeliverySpec
→ T3 ExecutionPlan DAG + WorkItems
→ Execution Plane implementation and integration
→ T4 PASS | FAIL | INCONCLUSIVE
→ ENGINEERING_READY when PASS
```

T4 failure normally returns to T3 remediation/replanning. A TechnicalDesign
defect returns to T2. T1 Acceptance Criteria never change merely to make T4
pass; a Product-intent change stops the current cycle and requires Human
Authority to create a new IntentSpec and delivery cycle.

## Product Knowledge boundary

Graphify provides structural observations. Repository tests provide behavioral
evidence. Git, pull requests, and delivery history provide delivery evidence.
None establishes Product truth automatically. Reverse discovery always produces
proposals for Human review.

PKB-001 remains immutable validation history and provides reusable Java,
Graphify-provider, scenario, reverse-discovery, and evaluation foundations. It
does not by itself prove that Product Knowledge improves real Feature delivery;
`SF-BL-001` measures that question through the controlled two-arm MVP.

## Technology boundary

Executable FDI framework behavior uses Java 17 and Spring Boot 3.4.1. The
external Graphify Python MCP runtime remains outside the framework behind the
Java `CodeIntelligenceProvider` adapter. FDI must verify the installed runtime
instead of assuming Graphify APIs.

`PASS` means `ENGINEERING_READY`, not deployed, published, or delivered to
users.

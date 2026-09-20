# ENGCIM Swarm Core Architecture Freeze v1.0

**Status:** Frozen documentation baseline
**Scope:** ENGCIM Swarm Core architecture and contracts
**Validation anchor:** Phase 2 Initial Effectiveness Baseline, merged at
`a07a023dd2e62db0338422b68daf1702adba1ee3`

## Decision

FDI remains a Software Factory for governed Product Knowledge and verifiable
software delivery. Version 1.0 freezes the reusable core around an
Engineering Scenario. A Scenario composes Product Context, Skills, Controls,
evidence requirements, execution state, and, when applicable, a Software
Delivery Profile.

The core is Scenario-first:

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

The diagram is a composition boundary, not a new runtime hierarchy. Multica
may provide the generic Execution Plane runtime, but it is not an authority
identity and is not part of the frozen domain contract.

## Frozen authority boundaries

- **Human Authority** owns Product meaning, accepted Product Knowledge,
  material scope and architecture changes, publication, deployment, and
  terminal closure.
- **Engineering Scenario** is the unit that declares scope, applicable
  context, required Skills, required Controls, evidence obligations, and
  execution binding.
- **Product Knowledge** is reusable across applicable Scenarios only through
  exact-versioned, provenance-bound Product Context resolution.
- **Skills** provide bounded reusable capability. A Skill cannot publish
  Product truth, weaken a Control, or create authority.
- **Controls** are fail-closed gates. Their result status is exactly
  `SATISFIED | UNSATISFIED | INCONCLUSIVE`.
- **Evidence** binds the subject, revision, contract, producer, result,
  artifacts, and digests. Prose alone does not satisfy a mandatory gate.
- **Execution Plane** performs approved work and evidence collection without
  changing Product meaning, Scenario authority, or correctness criteria.
- **Software Delivery Profile** owns the T1–T4 delivery sequence only when a
  Scenario explicitly declares that profile.

## Frozen Core contracts

| Contract | Frozen responsibility | Minimum binding |
|---|---|---|
| `SCN` | Engineering Scenario composition and scope | `scenarioRef`, `scenarioRevision`, objective, scope, Product Context, Skill refs, Control refs, evidence contract, execution ref |
| `SKILL` | Reusable bounded capability | `skillRef`, revision, capability boundary, inputs, outputs, limitations |
| `CTRL` | Fail-closed authorization or quality gate | `controlRef`, subject binding, required evidence, status, reason, result ref |
| `EVID` | Independently resolvable proof | `evidenceRef`, subject/revision, digest, producer, generation method, artifacts, result |
| `EXEC` | Runtime-neutral execution record | `executionRef`, Scenario/profile binding, runtime ref, lifecycle state, attempts, outputs, evidence refs |
| `PK` | Reusable accepted Product Knowledge | exact knowledge revision, provenance, applicability, limitations, conflicts, digest |
| `SD` | Optional Software Delivery Profile | profile revision, `FD-T*` contracts, delivery inputs, candidate, verification, closure evidence |

These contracts describe the frozen boundary. They do not authorize a new
production domain model or require a particular orchestration product.

## Software Delivery scope

`FD-T1-*` through `FD-T4-*` remain the existing requirement identifiers. Their
scope is now explicit:

```text
SD profile only:
T1 Intention → T2 Specify → T3 Execution Planning → T4 Correctness
```

T1–T4 do not govern every Engineering Scenario. A non-delivery Scenario may
use the Scenario, Skill, Control, Evidence, and Execution contracts without
declaring the Software Delivery Profile. A Scenario with an SD profile must
preserve the existing Feature Delivery authority and evidence boundaries.

`PASS` at T4 establishes `ENGINEERING_READY`; it does not mean deployed,
published, operationally rolled out, or delivered to users.

## Validated Control vocabulary

The following seven Control names are the canonical Phase 2 vocabulary:

1. `CTRL-AUTHORIZATION-001`
2. `CTRL-EXECUTION-SAFETY-001`
3. `CTRL-REPOSITORY-PROVENANCE-001`
4. `CTRL-EXACT-BINDING-001`
5. `CTRL-INDEPENDENT-EVALUATION-001`
6. `CTRL-EVIDENCE-INTEGRITY-001`
7. `CTRL-FINDING-RESOLUTION-001`

Their required evidence and scenario applicability remain defined by the
frozen validation baseline. This architecture document does not rewrite those
bindings or historical results.

## Runtime and non-goals

The generic runtime may schedule, execute, review, retry, integrate, and
report approved work. Runtime identity must remain separate from Scenario,
Skill, Control, Product Knowledge, and authority identity.

Version 1.0 does not introduce or reintroduce `Factory Control`,
`DeliveryRecipe`, or `WorkStep` as active Core architecture primitives. Any
historical or proposal mention of those terms remains historical or
non-normative and does not change the active contracts.

This freeze also does not authorize Product Knowledge source-lock remediation,
Multica metadata redesign, holdout/generalization validation, S07–S10
execution, or any implementation change.

## Validation boundary

The Initial Effectiveness Baseline established the Phase 2 result for the
validated S01–S06 scope. S07–S10 are not claimed effectiveness-validated by
this freeze. No scenario execution, threshold, denominator, fixture, Skill,
Control, runtime implementation, or evidence history is changed by this
documentation freeze.

## Change policy

Changes to the frozen Core contract shape, authority boundaries, T1–T4 scope,
or canonical Control vocabulary require a separately reviewed architecture
change. Documentation may clarify an existing boundary, but must not silently
create a second lifecycle, authority plane, or evidence model.

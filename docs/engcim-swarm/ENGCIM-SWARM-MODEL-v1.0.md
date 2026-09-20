# ENGCIM Swarm Model v1.0

**Status:** Frozen Core model
**Applies to:** Engineering Scenarios and their applicable profiles
**Validation boundary:** S01–S06 Phase 2 Initial Effectiveness Baseline

## 1. Model overview

An Engineering Scenario is the reusable unit of governed work. It binds the
meaning and scope needed for one bounded objective without assuming that every
objective is software delivery.

```text
EngineeringScenario
  ├─ ProductContext
  ├─ SkillBinding[]
  ├─ ControlBinding[]
  ├─ EvidenceContract
  ├─ ExecutionBinding
  └─ SoftwareDeliveryProfile?
       └─ T1 → T2 → T3 → T4
```

The optional marker is normative: T1–T4 are present only when the Scenario
declares a Software Delivery Profile.

## 2. Core contract shapes

The following shapes are the v1.0 contract boundary. Field-level schemas not
listed here remain subject to separate review.

### `SCN` — Engineering Scenario

```text
EngineeringScenario {
  scenarioRef
  scenarioRevision
  objective
  scope
  productContextRef
  skillRefs[]
  controlRefs[]
  evidenceContractRef
  executionRef
  softwareDeliveryProfileRef?
}
```

`scenarioRevision` is immutable for one execution. Scope, applicability,
limitations, and required evidence must be explicit. Missing or conflicting
authority fails closed.

### `SKILL` — Reusable capability

```text
SkillBinding {
  skillRef
  skillRevision
  capabilityBoundary
  inputRefs[]
  outputRefs[]
  limitationRefs[]
}
```

A Skill can analyze, transform, plan, or verify within its declared boundary.
It cannot publish Product truth, change a Scenario scope, weaken a Control, or
promote an unsupported result.

### `CTRL` — Control binding and result

```text
ControlResult {
  controlRef
  subjectRef
  requiredEvidenceRefs[]
  status: SATISFIED | UNSATISFIED | INCONCLUSIVE
  reasonRefs[]
  resultRef
}
```

`UNSATISFIED` and `INCONCLUSIVE` block the affected progression unless an
existing, explicitly authorized route handles the state. A review, comment,
issue status, or producer claim is not a substitute for a Control result.

### `EVID` — Evidence record

```text
EvidenceRecord {
  evidenceRef
  subjectRef
  subjectRevision
  contractDigest
  producerRef
  generatedBy
  artifactRefs[]
  digest
  result
}
```

Evidence must be independently resolvable. Exact revision binding and digest
binding are required where the governing Control requires them. Stale evidence
must be rejected rather than silently reused.

### `EXEC` — Runtime-neutral execution record

```text
ExecutionRecord {
  executionRef
  scenarioRef
  scenarioRevision
  profileRef?
  runtimeRef
  state
  attemptRefs[]
  outputRefs[]
  evidenceRefs[]
}
```

`runtimeRef` identifies the execution mechanism, not an authority. Multica is
an allowed generic runtime binding; it does not define Scenario semantics or
the Control lifecycle.

### `PK` — Reusable Product Knowledge

```text
ProductKnowledgeBinding {
  knowledgeRef
  knowledgeRevision
  provenanceRefs[]
  applicability
  limitations[]
  conflicts[]
  digest
}
```

Only exact Human-accepted knowledge may resolve into Product Context. The same
accepted Product Knowledge may be reused by multiple applicable Scenarios, but
each Scenario retains its own applicability and evidence binding.

### `SD` — Software Delivery Profile

```text
SoftwareDeliveryProfile {
  profileRef
  profileRevision
  intentSpecRef
  deliverySpecRef
  executionPlanRef
  candidateRef
  verificationRef
  closureEvidenceRefs[]
}
```

The profile is the only v1.0 contract that activates the existing `FD-T1-*`,
`FD-T2-*`, `FD-T3-*`, and `FD-T4-*` requirements. Those IDs are preserved;
their scope is not expanded to all Scenarios.

## 3. Scenario progression

The generic Scenario progression is:

```text
resolve exact context
  → bind Skills, Controls, and evidence obligations
  → execute the declared Scenario/profile
  → independently evaluate required evidence
  → close only when the applicable closure Control is satisfied
```

For an SD profile, the declared profile progression is:

```text
T1 Intention
  → T2 Specify
  → T3 Execution Planning
  → T4 Independent Correctness
```

For a Scenario without an SD profile, the model does not invent T1–T4 stages.

## 4. Canonical Phase 2 Controls

All applicable bindings use these exact names:

| Control | Boundary |
|---|---|
| `CTRL-AUTHORIZATION-001` | Human/mission authorization before mutation or progression |
| `CTRL-EXECUTION-SAFETY-001` | Branch, guard, scope, and safe execution boundary |
| `CTRL-REPOSITORY-PROVENANCE-001` | Canonical repository and candidate provenance |
| `CTRL-EXACT-BINDING-001` | Subject, revision, and current-candidate binding |
| `CTRL-INDEPENDENT-EVALUATION-001` | Independent evaluator/verifier separation |
| `CTRL-EVIDENCE-INTEGRITY-001` | Complete, resolvable, digest-bound evidence |
| `CTRL-FINDING-RESOLUTION-001` | Closure only after fresh exact correction evidence |

Every result uses exactly one of:

```text
SATISFIED | UNSATISFIED | INCONCLUSIVE
```

## 5. Authority and runtime separation

The model keeps these boundaries separate:

- Human Authority decides Product meaning, material changes, publication, and
  terminal closure.
- Scenario composition declares applicable context, Skills, Controls, and
  evidence; it does not become a second authority plane.
- The optional SD Profile owns delivery artifacts and preserves the existing
  Feature Delivery contracts.
- The generic runtime executes approved work and records evidence; it does not
  decide Product truth or Control satisfaction.
- Evidence is a resolvable proof layer, not a replacement for authority or
  domain semantics.

`Factory Control`, `DeliveryRecipe`, and `WorkStep` are not active v1.0 Core
model entities. They must not be introduced as duplicate lifecycle, scheduling,
or authority abstractions through documentation or implementation.

## 6. Validation and non-claims

This model is frozen against the completed S01–S06 effectiveness evidence. It
does not claim that S07, S08, S09, or S10 have been effectiveness-validated.
Those scenarios require separate authorized execution and adjudication.

The model freeze changes active documentation only. It does not rewrite
historical evidence, alter frozen baselines, change thresholds or denominators,
or imply that the Multica metadata-capacity workaround is a permanent runtime
architecture decision.

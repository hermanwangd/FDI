# FDI Layer 1 — Feature Transformation Specification v0.2

> **Status:** APPROVED — Layer 1 v0.2 contract-ready  
> **Scope:** Layer 1 only — canonical feature flow, transformation contracts, Context-reference interface, lifecycle, Change Surface, traceability, and correctness  
> **Design only:** No Context Supply design, physical Context generation, repository mutation, validator, pilot, or execution claim  
> **Current state:** `Contract-ready: APPROVED`, `Execution-verified: NOT_CLAIMED`  
> **Bundle provenance:** Vendored approved semantic content for standalone FDI operation. Upstream byte-for-byte identity is not claimed by this recovery serialization; this local file is digest-locked by the baseline.

---

## 0. Purpose, scope, and invariants

FDI Layer 1 defines how a feature moves from an authenticated Human Signal to independently established Correctness.

```text
Human Signal
    |
    | f(T1 Intention Skill ; Context refs)
    v
intention.md
    |
    | f(T2 Delivery Spec Skill ; Context refs)
    v
spec.md
    |
    | f(T3 Implementation Skill ; Context refs)
    v
ImplementationBundle
    |
    | f(T4 Correctness Skill ; Context refs)
    v
CorrectnessBundle
```

The canonical transformation model is:

```text
OutputBundle = f(
    CanonicalInput(s),
    GovernedSkill@revision
    ; ResolvedContextRefs
)
```

The semicolon is normative: `ResolvedContextRefs` are governed execution dependencies, not additional canonical workflow stages.

### 0.1 Layer 1 invariants

1. **Four canonical transformations only.** T1, T2, T3, T4.
2. **Skill defines transformation; Agent executes it.** Agent/model/Squad/runtime do not redefine semantics.
3. **Context is referenced, not owned by Skills.** Context production is outside Layer 1.
4. **Canonical artifacts carry authority.** Context may constrain/support but not silently replace canonical authority.
5. **Exact dependencies matter.** Downstream artifacts pin exact upstream revisions and materially influential Context/Evidence refs.
6. **Gate and validity differ.** An artifact may have passed a gate and later become `STALE`.
7. **T2 owns feature-specific Change Surface discovery.** Seeds/history/indexes are candidate-generation inputs, not proof of current scope.
8. **T3 may create governed source-repository candidates.** `implementation.md` remains the canonical coordination artifact.
9. **T4 separates verification from validation.** Implementation completion never implies Correctness.
10. **Layer 1 stops at independently established candidate Correctness.** Merge/deploy/release remain separately governed unless explicit criteria require them.

### 0.2 Explicitly out of scope

Layer 1 does not define Context-product production/refresh, a complete dependency graph, orchestration platform semantics, Agent/model assignment, or source-repository merge/deploy/release policy.

---

# Contract L1 — Canonical Artifact Contract

## L1.1 Canonical artifacts

| Stage | Canonical input | Canonical output | Governing Skill | Output gate |
| --- | --- | --- | --- | --- |
| T1 | Authenticated Human Signal | `intention.md` | `T1-Intention-Skill` | `INTENTION_READY | BLOCKED` |
| T2 | Exact active `intention.md` | `spec.md` | `T2-Delivery-Spec-Skill` | `SPEC_READY | BLOCKED` |
| T3 | Exact active `spec.md` | `ImplementationBundle`, core `implementation.md` | `T3-Implementation-Skill` | `CHANGE_SET_READY | BLOCKED` |
| T4 | Exact Intention + Spec + Implementation + candidate refs | `CorrectnessBundle`, core `correctness.md` | `T4-Correctness-Skill` | `PASS | FAIL | INCONCLUSIVE` |

Appendices may carry bounded supporting detail but never create a new canonical stage or independent authority.

## L1.2 Authenticated Human Signal

T1 requires a logical envelope equivalent to:

```yaml
human_signal:
  signal_id: "<stable-id>"
  source_ref: "<governed-source-ref>"
  source_identity: "<requester/decision-authority>"
  captured_at: "<time>"
  authentication_state: "VERIFIED|UNVERIFIED"
  authorization_state: "AUTHORIZED|NOT_AUTHORIZED|UNCLEAR"
  content_ref_or_digest: "<immutable-ref-or-digest>"
```

An unverified signal does not enter the canonical T1 transformation.

## L1.3 Canonical artifact envelope

```yaml
---
fdi_version: "0.2"
feature_id: "<stable-feature-id>"
artifact: "intention|spec|implementation|correctness"
revision: <positive-integer>
produced_by:
  skill: "<canonical-skill-id>"
  skill_revision: "<immutable-revision>"
canonical_owner: "<stable-owner>"
upstream: {}
gate: "<artifact-specific-gate>"
validity: "ACTIVE|STALE|SUPERSEDED"
supersedes: <prior-revision-or-null>
executor:
  role_or_agent: "<executor-id>"
  execution_id: "<run-id>"
---
```

Artifact revision is an immutable semantic revision. Lifecycle-only changes do not rewrite semantic history. For one feature lineage there is at most one active canonical revision per artifact type.

## L1.4 Authority by artifact

| Artifact | Authoritative for | Must not silently redefine |
| --- | --- | --- |
| `intention.md` | desired outcome, intended use, scope/non-goals, success criteria, authorization | current source truth, technical feasibility/design |
| `spec.md` | technical obligations, design, Change Surface, ownership, tasks, V&V plan | Intention outcome/authorization |
| `implementation.md` | exact candidate mappings and implementation observations | Spec obligations, Correctness |
| `correctness.md` | independent V&V evidence and criterion verdicts | Intention or Spec obligations |

## L1.5 Required artifact bodies

`intention.md` MUST contain Human Signal/provenance, stakeholders/users, desired outcome, intended-use scenarios, scope/non-goals, constraints/assumptions, measurable criteria with stable IDs, non-exhaustive product/system/repo seeds, Context used/excluded, unresolved conflicts/questions, and gate record.

`spec.md` MUST contain exact Intention revision, criterion→requirement map, requirements, design/invariants, current-state findings with EvidenceRefs, Change Surface, interface/data/config/schema/security/ops/rollout obligations, implementation tasks/owners, V&V plan, appendix registry, Context/selector proof, risks/gaps/deviations, and gate record.

`implementation.md` MUST contain exact Spec revision, candidate summary, per-repository dispositions, candidate refs where `CHANGE`, changed paths/migrations, requirement/design/task mappings, implementation checks, deviations, appendices, Context, execution provenance, T4 handoff, and gate record.

`correctness.md` MUST contain exact lineage/candidate refs, independent evaluator provenance, evidence inventory/integrity, verification findings, validation findings, one verdict per criterion, coverage, limitations/unobserved scope, release observation or `NOT_OBSERVED`, earliest re-entry, Context, execution provenance, overall verdict and gate record.

## L1.6 OutputBundle

```text
OutputBundle
├── CanonicalArtifact      required
├── GovernedAppendices*    optional
└── GovernedSideEffects*   optional
```

T1/T2 normally do not mutate source repositories. T3 may create reviewable candidates in approved repositories. T4 may create non-candidate-mutating evidence but must not repair the candidate it judges.

## L1.7 Shared structured records

### Intention criterion

```yaml
criterion:
  criterion_id: "C-<id>"
  statement: "<measurable desired outcome>"
  blocking: true
  success_measure: "<observable measure>"
  threshold_or_acceptance: "<pass condition>"
  human_signal_refs: ["<signal-fragment-ref>"]
```

Only T1 may classify a criterion non-blocking and only with authorized Human support.

### Spec requirement

```yaml
requirement:
  requirement_id: "R-<id>"
  criterion_ids: ["C-<id>"]
  statement: "<technical obligation>"
  owner: "<accountable owner>"
  repo_ids: ["<repo-id>"]
  vv_method_ids: ["V-<id>"]
```

### V&V disposition

```yaml
vv_disposition:
  criterion_id: "C-<id>"
  evaluation: "REQUIRED|OBSERVE_IF_AVAILABLE"
  method_id: "V-<id-or-null>"
  method: "<non-tautological-method-or-N/A>"
  evidence_expectation: "<evidence-or-policy>"
  threshold_or_acceptance: "<pass-condition-or-N/A>"
  required_scope: "<scope-or-N/A>"
  independence: "<independence-or-N/A>"
```

All blocking criteria use `REQUIRED` with a real method/evidence/threshold/scope.

### Task

```yaml
task:
  task_id: "T-<id>"
  requirement_ids: ["R-<id>"]
  repo_ids: ["<repo-id>"]
  owner: "<owner>"
  disposition: "PLANNED|IMPLEMENTED|NO_CHANGE|BLOCKED"
```

### Candidate ref

```yaml
candidate_ref:
  repo_id: "<repo-id>"
  base_revision: "<immutable-base>"
  head_revision: "<immutable-head>"
  location: "<branch/PR/commit>"
  status: "REVIEWABLE|NOT_READY|SUPERSEDED"
  task_ids: ["T-<id>"]
```

Required for every confirmed repository whose Spec action is `CHANGE`.

### Repository disposition

```yaml
repository_disposition:
  repo_id: "<repo-id>"
  required_action: "CHANGE|VERIFY_ONLY|NO_CHANGE"
  disposition: "READY|BLOCKED|SUPERSEDED"
  task_ids: ["T-<id>"]
  candidate_ref: "<candidate-id-or-null>"
  evidence_refs: ["E-<id>"]
  notes: "<summary>"
```

### EvidenceRef

```yaml
evidence_ref:
  evidence_id: "E-<id>"
  ref: "<immutable-source/result-ref>"
  revision_or_as_of: "<revision/time>"
  method: "<observation method>"
  environment: "<environment-or-N/A>"
  integrity: "<digest/signature/result-ref>"
  claim_ids: ["<criterion/requirement/finding IDs>"]
```

### Criterion verdict

```yaml
criterion_verdict:
  criterion_id: "C-<id>"
  verdict: "PASS|FAIL|INCONCLUSIVE"
  verification_finding: "<finding-or-N/A>"
  validation_finding: "<finding>"
  evidence_refs: ["E-<id>"]
  limitations: []
  earliest_reentry: "T1|T2|T3|T4|NONE"
```

## L1.8 Governed appendices

Appendices are subordinate to a core artifact, registered by it, have no independent gate, cannot self-authorize scope, and materially influential changes create a new owning-artifact revision. Raw secrets, copied source trees, mutable unpinned evidence, and unrelated files are prohibited.

## L1.9 Multi-repository authority boundary

FDI owns the aggregate Change Surface/technical contract/candidate map/evidence mapping. Source repositories retain repository-local feasibility, candidate content, review controls, merge/deploy/release authorization. A Layer 1 gate never authorizes merge/deploy/release by itself.

---

# Contract L2 — Governed `f_skill` Transformation Contract

## L2.1 Generic function

```text
OutputBundle = f(CanonicalInput(s), GovernedSkill@revision ; ResolvedContextRefs)
```

## L2.2 Required Skill interface

Every canonical Skill defines: `skill_id`, `skill_revision`, purpose, canonical inputs and preconditions, context requirements/selectors, authority rules, procedure, capability requirements, evidence rules, allowed side effects, output contract, completion/gate rules, failure classes, re-entry rule, and prohibitions.

## L2.3 Root/helper Skills

Only a root canonical Skill defines a canonical transformation. Material helper Skill/agent/reviewer identities and revisions are recorded in provenance. Helper Skills never add canonical transitions.

## L2.4 Preflight

```text
CONTRACT_READY | NOT_CONTRACT_READY
```

Preflight is not an output gate.

## L2.5 Contract determinism

FDI does not require byte-identical LLM output. It requires stable authority boundaries, inputs, bounded Context selection, traceability, gate calculation, and explicit provenance for material differences.

---

# Contract L3 — Context Reference Contract

## L3.1 Boundary

Layer 1 defines how a Skill requests/resolves/uses/records Context, not how Context is produced or maintained.

## L3.2 ContextRequirement

```yaml
context_requirement:
  id: "<stable-id>"
  purpose: "<purpose>"
  authority_dimension: "<dimension>"
  mode: "REQUIRED|CONDITIONAL|ON_DEMAND"
  selector: "<bounded-selection-rule>"
  applicability: "<condition>"
  freshness_requirement: "<revision/as-of>"
  trust_requirement: "<minimum-trust>"
  claims: []
```

`REQUIRED` blocks preflight if unresolved. Selectors must be bounded; “load all repositories/history/knowledge” is invalid.

## L3.3 ResolvedContextRef

```yaml
resolved_context_ref:
  requirement_id: "<requirement-id>"
  ref: "<stable-ref>"
  revision_or_as_of: "<immutable-revision/time>"
  selected_for: "<purpose/claim>"
  authority_dimension: "<dimension>"
  trust_state: "<state>"
  applicability: "<scope-match>"
  freshness: "<state>"
  evidence_backlink: "<ref-or-null>"
```

## L3.4 Context vs Evidence

`ResolvedContextRef` informs/constrains. `EvidenceRef` establishes a specific claim. Current behavior/state materially relied on by T2/T4 requires pinned current-source/runtime Evidence when applicable.

## L3.5 Authority dimensions

| Dimension | Primary authority |
| --- | --- |
| Desired outcome | active authorized Intention |
| Technical obligation | active Spec, subordinate to Intention |
| Durable organizational/domain constraint | applicable governed Context |
| Current behavior/state | pinned current-source/runtime EvidenceRef |
| Procedure | active Skill + permitted capabilities |
| Rationale/support | qualified Context |

## L3.6 Conflict/gap

Conflicts are reconciled by claim, authority dimension, revision/as-of, environment/scope, applicability, and trust/provenance. Genuine unresolved conflict becomes an explicit Context gap and blocks only isolated dependent claims when isolation is demonstrated; otherwise the transition blocks or T4 is inconclusive.

---

# Contract L4 — T1–T4 Canonical Transformation Contracts

## L4.1 T1 — Intention

```text
intention.md = f(AuthenticatedHumanSignal, T1-Intention-Skill@revision ; R1)
```

T1 preserves Human authority for desired need, defines stakeholders/intended use/scope/non-goals/constraints/measurable criteria/stable IDs/non-exhaustive seeds/Context/conflicts and outputs `INTENTION_READY|BLOCKED`. T1 never chooses implementation architecture or converts historical behavior into desired outcome without Human authority.

## L4.2 T2 — Delivery Spec

```text
SpecBundle = f(intention.md@exact-active-revision, T2-Delivery-Spec-Skill@revision ; R2)
core(SpecBundle) = spec.md
```

T2 preserves Intention and performs bounded evidence-backed Change Surface discovery. It maps criteria to obligations, establishes current-state assumptions with pinned evidence, resolves ownership, defines requirements/design/interfaces/data/config/schema/security/ops/rollout obligations/tasks/V&V, records Context/risks/gaps, and outputs `SPEC_READY|BLOCKED`.

`SPEC_READY` requires all blocking criteria mapped to obligations; ownership/work/no-change rationale; sufficient Change Surface; explicit V&V disposition for every criterion; and no unresolved issue that can materially change the technical contract.

## L4.3 T3 — Implementation

```text
ImplementationBundle = f(spec.md@exact-active-revision, T3-Implementation-Skill@revision ; R3)
```

T3 pins exact Spec/base revisions, verifies ownership/permissions, creates candidates only within approved scope, records exact heads/paths/checks/deviations, and outputs `CHANGE_SET_READY|BLOCKED`.

For each confirmed repository:
- `CHANGE` → pinned reviewable candidate required;
- `VERIFY_ONLY` → evidence-backed verification disposition;
- `NO_CHANGE` → evidence-backed no-change disposition.

New required scope triggers evidence + stop + T2 re-entry.

## L4.4 T4 — Correctness

```text
CorrectnessBundle = f(
  intention.md@exact-active-revision,
  spec.md@exact-active-revision,
  ImplementationBundle@exact-candidate-revisions,
  T4-Correctness-Skill@revision ; R4
)
```

T4 independently evaluates Verification (`Implementation ↔ Spec`) and Validation (`Implementation ↔ Intention`). T4 owner/evaluation context is distinct from T3 and cannot mutate the candidate under judgment. Every criterion receives `PASS|FAIL|INCONCLUSIVE`. Overall gate is `PASS|FAIL|INCONCLUSIVE`.

---

# Contract L5 — Feature-Specific Change Surface Contract

## L5.1 Purpose

Change Surface is the evidence-backed feature-specific set of repositories and material obligations determined by T2; it is not a generic enterprise graph.

## L5.2 Candidate vs confirmation

Candidates may come from product seeds, indexes, history, dependency hints, structural intelligence, source investigation, and interface/schema/event/config/runtime ownership evidence. Candidate generation does not establish current applicability.

## L5.3 ChangeSurfaceFinding

```yaml
change_surface_finding:
  finding_id: "CS-<id>"
  repo_id: "<stable-repo-id>"
  status: "CANDIDATE|CONFIRMED|EXCLUDED|UNRESOLVED"
  relevance: "<why>"
  relation_types: []
  criterion_ids: []
  requirement_ids: []
  context_refs: []
  evidence_refs: []
  owner: "<owner-or-null>"
  required_action: "CHANGE|VERIFY_ONLY|NO_CHANGE|NOT_APPLICABLE|UNDECIDED"
  planned_change: "<summary>"
  blocking: true
```

Semantics:
- `CANDIDATE` → `UNDECIDED`.
- `CONFIRMED` → requires current feature-specific EvidenceRef; action is `CHANGE|VERIFY_ONLY|NO_CHANGE`.
- `EXCLUDED` → current feature-specific evidence supports non-inclusion; action `NOT_APPLICABLE`.
- `UNRESOLVED` → insufficient/conflicting evidence; action `UNDECIDED`.

## L5.4 Bounded discovery and sufficiency

Discovery uses explicit product/system/relation/repository/traversal/source-query bounds. `SPEC_READY` does not require proof that no unknown repository exists globally; it requires all materially encountered candidates to be dispositioned, no blocking unresolved candidates, confirmed repos mapped to criteria/obligations/actions, represented cross-repo obligations, owners resolved, exclusions retained with rationale, and material discovery gaps causing `BLOCKED`.

---

# Contract L6 — Lifecycle, Gate, Validity, Invalidation, Re-entry

Output gates:

```text
T1: INTENTION_READY | BLOCKED
T2: SPEC_READY | BLOCKED
T3: CHANGE_SET_READY | BLOCKED
T4: PASS | FAIL | INCONCLUSIVE
```

Preflight remains separate: `CONTRACT_READY|NOT_CONTRACT_READY`.

Validity states:

```text
ACTIVE | STALE | SUPERSEDED
```

Minimum invalidation cascade:
- Intention semantic revision change → dependent Spec/Implementation/Correctness stale.
- Spec revision change → dependent Implementation/Correctness stale.
- Candidate head change → Implementation stale until re-pinned and Correctness stale.
- Material Context/Evidence invalidation → claim-local or owning-artifact invalidation according to dependency isolation.

Earliest re-entry:
- wrong/changed Human need/criteria → T1;
- wrong/incomplete obligations/design/Change Surface/V&V → T2;
- implementation non-conformance → T3;
- evidence unavailable while candidate remains valid → T4.

---

# Contract L7 — Traceability and Correctness

End-to-end navigation:

```text
Human Signal fragment
→ Intention criterion
→ Spec requirement/design/task/repository
→ repo:path@candidate-sha
→ V&V Evidence
→ criterion verdict
```

Transformation provenance also records governing Skill revision, exact upstream revisions, materially influential Context refs, and executor/run provenance.

T4 Verification and Validation are separate. Every criterion receives `PASS|FAIL|INCONCLUSIVE` with evidence, findings, limitations, and earliest re-entry.

Overall Correctness:

```text
PASS
= every blocking criterion PASS
  AND every required blocking-criterion V&V scope observed

FAIL
= at least one blocking criterion FAIL

INCONCLUSIVE
= no blocking criterion FAIL
  but at least one blocking criterion cannot be decided due to insufficient required evidence/capability/scope
```

A no-code feature is valid only with evidence-backed no-change rationale and sufficient validation evidence; “no diff” alone is not Correctness evidence.

---

# Layer 1 Approval Record

```text
Contract review: PASS
Open contract blockers: NONE IDENTIFIED
Herman design approval: APPROVED
Contract-ready: APPROVED
Execution-verified: NOT_CLAIMED
Implementation/pilot: NOT_AUTHORIZED_BY_THIS_APPROVAL
```

# Layer 1 Canonical Summary

```text
I = f(H, T1-Skill@r1 ; R1)
S = f(I@ri, T2-Skill@r2 ; R2)
B = f(S@rs, T3-Skill@r3 ; R3)
C = f(I@ri, S@rs, B@rb, T4-Skill@r4 ; R4)
```

Agent/Squad/Model/Runtime is orthogonal and executes the Skill contract. Layer 2 Context Supply is also orthogonal and cannot silently change Layer 1 authority or gates.

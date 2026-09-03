# FDI Layer 1 — Markdown I/O Profile v0.1

> **Status:** APPROVED — Contract-ready  
> **Depends on:** FDI Layer 1 — Feature Transformation Specification v0.2 (`Contract-ready: APPROVED`)  
> **Purpose:** Fix the physical Markdown I/O contract for Layer 1 so an Agent can read and produce canonical artifacts without inventing file structure  
> **Scope:** `intention.md`, `spec.md`, `implementation.md`, `correctness.md`, plus governed appendices  
> **Execution-verified:** NOT_CLAIMED  
> **Bundle provenance:** Vendored approved physical-I/O content for standalone operation. Upstream byte-for-byte identity is not claimed by this local serialization; local bytes are digest-locked.

---

# 0. Core Rule

FDI Layer 1 already defines:

```text
OutputBundle = f(
    CanonicalInput(s),
    GovernedSkill@revision
    ; ResolvedContextRefs
)
```

This profile fixes the physical Markdown representation. It does not change Layer 1 authority, gate, lifecycle, Change Surface, or Correctness semantics.

Every canonical file contains:

```text
YAML frontmatter
+
fixed ordered H1/H2 sections
+
structured YAML records in declared sections
+
bounded narrative where permitted
```

Required sections are never omitted. Empty required sections use `NONE`, empty arrays, or a governed `NOT_APPLICABLE` record. Structured content outranks contradictory narrative; a conflict makes the artifact non-compliant and it cannot claim a ready/pass gate.

Stable IDs must remain stable within a lineage. Recommended prefixes:

```text
Human Signal fragment HSF-001
Criterion              C-001
Requirement            R-001
Design invariant       D-001
Task                   T-001
V&V method             V-001
Change Surface         CS-001
Context reference      CTX-001
Evidence               E-001
Candidate reference    CR-001
Deviation              DEV-001
Appendix               A-001
Check                  CHK-001
```

References SHOULD use immutable revisions/as-of states. `ResolvedContextRef` and `EvidenceRef` remain distinct.

## 0.1 Common frontmatter

```yaml
---
fdi_version: "0.2"
profile: "layer1-markdown-io-v0.1"
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
  role_or_agent: "<executor-or-null>"
  execution_id: "<run-id-or-null>"
---
```

---

# 1. `intention.md` Physical Contract

## 1.1 Frontmatter

`artifact: intention`, `produced_by.skill: T1-Intention-Skill`, upstream Human Signal IDs, gate `INTENTION_READY|BLOCKED`.

## 1.2 Exact body order

```markdown
# Intention

## 1. Human Signal
## 2. Stakeholders and Intended Users
## 3. Desired Outcome
## 4. Intended-Use Scenarios
## 5. Scope and Non-Goals
## 6. Constraints and Assumptions
## 7. Success Criteria
## 8. Product, System, and Repository Seeds
## 9. Context Used
## 10. Open Questions and Conflicts
## 11. Gate Record
```

All eleven sections are required.

### §1 Human Signal

```yaml
human_signals:
  - signal_id: "<stable-id>"
    source_ref: "<governed-ref>"
    source_identity: "<requester/authority>"
    captured_at: "<timestamp>"
    authentication_state: "VERIFIED"
    authorization_state: "AUTHORIZED|NOT_AUTHORIZED|UNCLEAR"
    content_ref_or_digest: "<immutable-ref-or-digest>"
    summary: "<faithful-summary>"
    fragments:
      - fragment_id: "HSF-001"
        content_locator: "<stable-location>"
        summary: "<faithful-fragment-summary>"
```

### §2 Stakeholders/Users

```yaml
stakeholders:
  - stakeholder_id: "<id>"
    role: "<role>"
    relationship: "REQUESTER|APPROVER|USER|OWNER|AFFECTED|OTHER"
intended_users:
  - user_group_id: "<id>"
    description: "<group>"
```

### §3 Desired Outcome

```yaml
desired_outcome:
  statement: "<what must become true>"
  business_or_user_value: "<why>"
```

### §4 Intended Use

```yaml
intended_use_scenarios:
  - scenario_id: "S-001"
    actor: "<actor>"
    situation: "<situation>"
    expected_outcome: "<observable result>"
```

### §5 Scope/Non-goals

```yaml
scope:
  in_scope: []
  non_goals: []
```

### §6 Constraints/Assumptions

```yaml
constraints:
  - constraint_id: "CON-001"
    statement: "<constraint>"
    source: "HUMAN_SIGNAL|CONTEXT"
    ref: "<ref>"
assumptions:
  - assumption_id: "ASM-001"
    statement: "<assumption>"
    validation_needed: true
```

### §7 Criteria

```yaml
criteria:
  - criterion_id: "C-001"
    statement: "<measurable desired outcome>"
    blocking: true
    success_measure: "<measure>"
    threshold_or_acceptance: "<pass condition>"
    human_signal_refs: ["HSF-001"]
```

Every authorized requested outcome maps to criteria. Only T1 may set non-blocking and only with Human authority.

### §8 Seeds

```yaml
impact_seeds:
  products: []
  systems: []
  repositories: []
  completeness: "NON_EXHAUSTIVE"
```

### §9 Context Used

```yaml
resolved_context_refs:
  - context_ref_id: "CTX-001"
    requirement_id: "<requirement>"
    ref: "<stable-ref>"
    revision_or_as_of: "<revision/time>"
    selected_for: "<purpose>"
    authority_dimension: "<dimension>"
    trust_state: "<state>"
    applicability: "<scope>"
    freshness: "<state>"
    evidence_backlink: "<ref-or-null>"
context_exclusions: []
```

### §10 Open items

```yaml
open_items:
  - item_id: "Q-001"
    type: "QUESTION|AUTHORITY_CONFLICT|CONTEXT_GAP|AMBIGUITY"
    statement: "<issue>"
    blocking: true
    owner: "<owner>"
```

### §11 Gate

```yaml
gate_record:
  gate: "INTENTION_READY|BLOCKED"
  evaluated_against:
    human_signal_ids: []
  blocking_items: []
  rationale: "<bounded-rationale>"
```

---

# 2. `spec.md` Physical Contract

## 2.1 Exact body order

```markdown
# Delivery Spec

## 1. Upstream Intention
## 2. Criterion-to-Requirement Mapping
## 3. Requirements
## 4. Design and Invariants
## 5. Current-State Findings
## 6. Change Surface
## 7. Interface, Data, Security, and Operational Obligations
## 8. Implementation Tasks and Ownership
## 9. Verification and Validation Plan
## 10. Appendix Registry
## 11. Context Used and Selector Proof
## 12. Risks, Gaps, and Deviations
## 13. Gate Record
```

All thirteen sections are required.

### §1 Upstream Intention

```yaml
upstream_intention:
  feature_id: "<feature-id>"
  revision: <exact-revision>
  gate: "INTENTION_READY"
  validity_at_execution: "ACTIVE"
  criterion_ids: []
```

### §2 Criterion→Requirement

```yaml
criterion_requirement_map:
  - criterion_id: "C-001"
    requirement_ids: ["R-001"]
    rationale: "<required if empty>"
```

Every criterion appears exactly once.

### §3 Requirements

```yaml
requirements:
  - requirement_id: "R-001"
    criterion_ids: ["C-001"]
    statement: "<technical obligation>"
    owner: "<owner>"
    repo_ids: []
    vv_method_ids: ["V-001"]
```

### §4 Design/Invariants

```yaml
design:
  summary: "<design>"
  invariants:
    - invariant_id: "D-001"
      statement: "<must remain true>"
      requirement_ids: ["R-001"]
```

### §5 Current State / Evidence

```yaml
evidence_refs:
  - evidence_id: "E-001"
    ref: "<immutable-source-ref>"
    revision_or_as_of: "<revision/time>"
    method: "<method>"
    environment: "<env-or-N/A>"
    integrity: "<digest/ref>"
    claim_ids: ["STATE-001", "CS-001"]
current_state_findings:
  - finding_id: "STATE-001"
    statement: "<current-state claim>"
    evidence_refs: ["E-001"]
    context_refs: ["CTX-001"]
    limitations: []
```

Current behavior/state materially used by T2 requires Evidence; Context alone is insufficient.

### §6 Change Surface

```yaml
change_surface:
  discovery_scope:
    products: []
    systems: []
    relation_types: []
    traversal_budget: "<bounded-summary>"
    exclusions: []
  findings:
    - finding_id: "CS-001"
      repo_id: "<repo-id>"
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

Rules: candidate/unresolved → `UNDECIDED`; confirmed → current feature-specific Evidence + `CHANGE|VERIFY_ONLY|NO_CHANGE`; excluded → current evidence + `NOT_APPLICABLE`.

### §7 Cross-cutting obligations

```yaml
cross_cutting_obligations:
  interfaces: {applicability: "APPLICABLE|NOT_APPLICABLE", requirement_ids: [], details: []}
  data: {applicability: "APPLICABLE|NOT_APPLICABLE", requirement_ids: [], details: []}
  configuration: {applicability: "APPLICABLE|NOT_APPLICABLE", requirement_ids: [], details: []}
  schema: {applicability: "APPLICABLE|NOT_APPLICABLE", requirement_ids: [], details: []}
  security: {applicability: "APPLICABLE|NOT_APPLICABLE", requirement_ids: [], details: []}
  operations: {applicability: "APPLICABLE|NOT_APPLICABLE", requirement_ids: [], details: []}
  rollout_rollback: {applicability: "APPLICABLE|NOT_APPLICABLE", requirement_ids: [], details: []}
```

### §8 Tasks

```yaml
tasks:
  - task_id: "T-001"
    requirement_ids: ["R-001"]
    repo_ids: ["<repo-id>"]
    owner: "<owner>"
    disposition: "PLANNED|NO_CHANGE|BLOCKED"
```

`IMPLEMENTED` is not a T2 disposition.

### §9 V&V Plan

```yaml
vv_dispositions:
  - criterion_id: "C-001"
    evaluation: "REQUIRED|OBSERVE_IF_AVAILABLE"
    method_id: "V-001|null"
    method: "<method-or-N/A>"
    evidence_expectation: "<evidence>"
    threshold_or_acceptance: "<pass-or-N/A>"
    required_scope: "<scope-or-N/A>"
    independence: "<independence-or-N/A>"
```

### §10 Appendix registry

```yaml
appendices:
  - appendix_id: "A-001"
    type: "SPEC"
    trigger: "<reason>"
    status: "ACTIVE|SUPERSEDED"
    path: "appendices/spec/A-001.md"
    related_ids: []
    backlink: {owner_artifact: "spec", owner_revision: <revision>}
```

### §11 Context/selector proof

Includes exact `resolved_context_refs`, `context_exclusions`, and bounded `selector_proof` records.

### §12 Risks/Gaps/Deviations

```yaml
risks_gaps_deviations:
  - item_id: "RGD-001"
    type: "RISK|CONTEXT_GAP|EVIDENCE_GAP|DEVIATION|OWNERSHIP_GAP|DESIGN_GAP"
    statement: "<issue>"
    affected_ids: []
    blocking: true
    disposition: "RESOLVED|ACCEPTED|BLOCKING|REQUIRES_AUTHORIZATION"
    owner: "<owner>"
```

### §13 Gate

```yaml
gate_record:
  gate: "SPEC_READY|BLOCKED"
  intention_revision: <revision>
  blocking_items: []
  unresolved_change_surface_findings: []
  rationale: "<bounded-rationale>"
```

---

# 3. `implementation.md` Physical Contract

Exact body order:

```markdown
# Implementation
## 1. Upstream Spec
## 2. Candidate Summary
## 3. Repository Implementation Dispositions
## 4. Candidate References
## 5. Changed Paths and Migrations
## 6. Requirement, Design, and Task Mapping
## 7. Checks and Observed Results
## 8. Deviations from Spec
## 9. Appendix Registry
## 10. Context Used
## 11. Execution Provenance
## 12. Known Gaps and T4 Handoff
## 13. Gate Record
```

Required records include:

```yaml
repository_dispositions:
  - repo_id: "<repo-id>"
    required_action: "CHANGE|VERIFY_ONLY|NO_CHANGE"
    disposition: "READY|BLOCKED|SUPERSEDED"
    task_ids: []
    candidate_ref: "CR-001|null"
    evidence_refs: []
    notes: "<summary>"
```

```yaml
candidate_refs:
  - candidate_ref_id: "CR-001"
    repo_id: "<repo-id>"
    base_revision: "<immutable-base>"
    head_revision: "<immutable-head>"
    location: "<branch/PR/commit>"
    status: "REVIEWABLE|NOT_READY|SUPERSEDED"
    task_ids: []
```

`CHANGE` requires candidate; `VERIFY_ONLY` and `NO_CHANGE` must not fabricate one.

T3 evidence/checks:

```yaml
evidence_refs: []
checks:
  - check_id: "CHK-001"
    type: "BUILD|TEST|STATIC|MIGRATION|CONFIG|OTHER"
    repo_id: "<repo-id>"
    candidate_ref_id: "CR-001|null"
    method: "<method>"
    result: "PASS|FAIL|NOT_RUN"
    evidence_refs: []
```

Execution provenance records root Skill and materially influential helpers. T4 handoff records exact candidate refs, planned V&V methods, evidence available, and known gaps. Gate is `CHANGE_SET_READY|BLOCKED`.

---

# 4. `correctness.md` Physical Contract

Exact body order:

```markdown
# Correctness
## 1. Exact Inputs and Independence
## 2. Evidence Inventory and Integrity
## 3. Verification Findings
## 4. Validation Findings
## 5. Criterion Verdicts
## 6. Coverage
## 7. Deviations, Gaps, Limitations, and Unobserved Scope
## 8. Release and Production Observation
## 9. Earliest Re-entry
## 10. Context Used
## 11. Execution Provenance
## 12. Overall Verdict and Gate Record
```

Independence:

```yaml
independence:
  t3_canonical_owner: "<T3-owner>"
  t4_canonical_owner: "<T4-owner>"
  distinct_owner: true
  t3_execution_id: "<T3-run>"
  t4_execution_id: "<T4-run>"
  distinct_evaluation_context: true
  candidate_mutation_authority_during_evaluation: false
```

Evidence inventory has immutable refs/integrity. Verification findings judge Spec conformance; validation findings judge Intention/intended use. Every criterion has a verdict and finding refs. Coverage explicitly lists required/observed/missing criteria, requirements, invariants, tasks, repositories, and V&V scope. Release observation is `OBSERVED|NOT_OBSERVED`. Overall gate is `PASS|FAIL|INCONCLUSIVE`.

---

# 5. Appendix Physical Contracts

## 5.1 Spec Appendix

Path: `appendices/spec/{appendix-id}.md`

Required sections:

```markdown
# Spec Appendix — {appendix-id}
## 1. Purpose and Trigger
## 2. Scope
## 3. Related Canonical IDs
## 4. Detailed Technical Content
## 5. Context and Evidence References
## 6. Risks, Gaps, and Limitations
## 7. Backlink
```

It cannot introduce unregistered requirements, repositories, criteria, or V&V obligations.

## 5.2 Implementation Repository Appendix

Path: `appendices/implementation/{repo-id}.md`. It carries one confirmed repository’s disposition/tasks/candidate/paths/checks/deviations/backlink. `CHANGE` requires applicable candidate ref; `VERIFY_ONLY|NO_CHANGE` use `NOT_APPLICABLE` candidate.

## 5.3 Evidence Appendix

Path: `appendices/evidence/{evidence-id}.md`. It records evidence identity, claim mapping, method/environment, observation, integrity/provenance, limitations, backlink. It is registered in owning `correctness.md` and never independently calculates Correctness.

## 5.4 Cross-file reuse invariant

Physical-only IDs may aid addressability but must not redefine semantic records. Every referenced Evidence, candidate, appendix, criterion, requirement, invariant, and task ID resolves within the active OutputBundle or via governed immutable reference.

---

# 6. Skill Read/Write Contracts

T1 reads Human Signal + Context and writes `intention.md` sections above.

T2 reads active `intention.md` + Context, gathers pinned current Evidence during execution, writes `spec.md` + bounded Spec appendices.

T3 reads active `spec.md` + approved repository Context/capabilities, produces candidates and `implementation.md` + bounded implementation appendices.

T4 reads exact Intention/Spec/Implementation/candidate refs + Context, gathers/references independent Evidence, writes `correctness.md` + evidence appendices.

Canonical chain remains:

```text
Human Signal → intention.md → spec.md → implementation.md → correctness.md
```

Appendices/side effects never add stages.

---

# 7. Agent Compliance Rules

A producing Agent MUST use exact section order, exact upstream revisions, stable IDs, required structured records, explicit empty/N/A states, distinct Context vs Evidence refs, no invented authority, no narrative bypass of gates, appendices only through owner registries, and only the current transition’s gate.

A consuming Agent MUST reject/block when frontmatter is missing/inconsistent, lineage is wrong, artifact is stale/superseded, prior gate is unsatisfied, required records are absent, or narrative contradicts structured records.

---

# 8. Approval Record

```text
Contract review: PASS
Open contract blockers: NONE IDENTIFIED
Herman design approval: APPROVED
Layer 1 semantic contract: APPROVED
Layer 1 Markdown I/O v0.1: Contract-ready APPROVED
Execution-verified: NOT_CLAIMED
Implementation authorization: NOT_GRANTED_BY_THIS_APPROVAL
```

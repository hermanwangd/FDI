# PKB-001 Backlog

This file is the only active backlog for the prototype and is the third of five
active control documents. A backlog status describes readiness and blocking; it
does not by itself authorize implementation, external runtime changes, holdout
selection, experiment execution, or semantic publication.

## Backlog record contract

Every backlog item contains:

- `backlog_id`: stable `PKB-BL-NNN` identifier that is never reused;
- `title` and `work_type`: one bounded outcome and its work classification;
- `source` and `spec_binding`: the controlling source plus exact Framework path,
  revision, and requirement ID when applicable;
- `current_gap` and `intended_outcome`;
- `priority`: `P0`, `P1`, or `P2`;
- `status`: one of the states below;
- `depends_on`, blockers, and in/out scope;
- deliverables, required verification, decision owner, implementation owner,
  and active plan link or `null`;
- `completion_evidence`: test results and commit or artifact digests, empty until
  verified.

Allowed status values are:

- `READY`: prerequisites are known, but execution still requires an explicit
  instruction to proceed;
- `IN_PROGRESS`: implementation is actively authorized and underway;
- `BLOCKED_DEPENDENCY`: a listed prerequisite is not verified;
- `BLOCKED_PRODUCT_TEAM`: Product Team meaning or approval is required;
- `BLOCKED_USER_APPROVAL`: explicit user approval is required;
- `NEEDS_RECONCILIATION`: the bound Spec changed and the item must be checked
  against the new revision;
- `DEFERRED`: intentionally outside the current execution window;
- `VERIFIED`: deliverables exist and every acceptance check has objective
  evidence.

An item may move to `VERIFIED` only when its deliverables, tests, immutable
digests where applicable, and commit IDs are recorded. A checkbox, prose claim,
generated proposal, evaluator judgment, or agent report alone is insufficient.
If a verified dependency changes, dependent items return to
`BLOCKED_DEPENDENCY` and must be reverified. Product Team and user approval
states cannot be cleared by an agent.

## Prioritized backlog items

All records below bind to `FRAMEWORK-SPEC.md` at exact Git revision
`4f8e903181bf79178af37672e1cb57699c4c93f7`. This binding matrix is part of
each Backlog record; detailed priority, status, dependencies, deliverables,
verification, and completion evidence follow in the item sections.

| Backlog ID | Work type | Source requirement | Current gap / intended outcome | Scope boundary | Decision / implementation owner | Active plan |
|---|---|---|---|---|---|---|
| `PKB-BL-001` | `DOCUMENTATION` | `PKB-REVIEW-001` | Existing packet mixes semantic and technical review; provide an isolated Stage A surface. | Packet and isolation tests only; no Product decision. | Product Team / Engineering | `null` |
| `PKB-BL-002` | `DOCUMENTATION` | `PKB-STATUS-001` | Status points to the combined packet; point it to verified Stage A material. | Active pointer only; preserve old packet. | Product Team / Engineering | `null` |
| `PKB-BL-003` | `FEATURE` | `PKB-REVIEW-002` | Stage A meaning is undecided; record Product Team decisions. | Semantics only; no technical unblinding. | Product Team / Product Team | `null` |
| `PKB-BL-004` | `VALIDATION` | `PKB-EVAL-LEGACY-001` | Eleven evaluator disagreements remain; adjudicate only those items. | Existing evaluation only; no Product authority. | Evaluation owner / Independent evaluator | `null` |
| `PKB-BL-005` | `FEATURE` | `PKB-SCENARIO-001` | Scenario rules are prose-only; make the lifecycle machine-verifiable. | Contract and validation; no scenario approval. | Product Team / Engineering | `null` |
| `PKB-BL-006` | `FEATURE` | `PKB-SCENARIO-002` | No approved frozen scenario-bearing revision exists; create one without overwriting Petclinic. | New semantics revision only. | Product Team / Engineering | `null` |
| `PKB-BL-007` | `FEATURE` | `PKB-MAPPING-001` | PK-S1 v0.2 lacks scenario traces; create a new compatible proposal contract. | New skill/contract version; preserve v0.2. | Product Team / Engineering | `null` |
| `PKB-BL-008` | `RESEARCH` | `PKB-PROVIDER-001` | UI/template support is unverified; record actual runtime capability or gaps. | Read-only discovery; no assumed API or runtime replacement. | User / Engineering | `null` |
| `PKB-BL-009` | `FEATURE` | `PKB-REVERSE-001` | Reverse output over-combines, duplicates, and overclaims; add proposal-only controls. | Proposal quality only; no semantic publication. | Product Team / Engineering | `null` |
| `PKB-BL-010` | `VALIDATION` | `PKB-EVAL-001` | Existing truth relies on provider IDs; add sealed normalized identity. | New evaluator truth format; generation remains isolated. | Evaluation owner / Engineering | `null` |
| `PKB-BL-011` | `VALIDATION` | `PKB-EVAL-002` | Scenario, chain, component, and diagnostic measures are incomplete; add distinct metrics. | Deterministic evaluation only. | Evaluation owner / Engineering | `null` |
| `PKB-BL-012` | `VALIDATION` | `PKB-CALIBRATION-001` | No preregistered numeric gate exists; freeze justified thresholds. | Threshold definition only; no generation. | Product Team / Evaluation owner | `null` |
| `PKB-BL-013` | `RESEARCH` | `PKB-HOLDOUT-001` | No approved sealed holdout exists; propose, approve, and seal one exact revision. | Candidate metadata and seal; no execution. | User / Independent selector | `null` |
| `PKB-BL-014` | `VALIDATION` | `PKB-PROTOCOL-001` | Next-run inputs are not jointly frozen; bind every executable input and digest. | Protocol manifest only; no run. | User / Engineering | `null` |
| `PKB-BL-015` | `VALIDATION` | `PKB-REGRESSION-001` | New rules lack regression evidence; run Petclinic under a frozen new protocol. | New immutable run; preserve completed run. | Evaluation owner / Engineering | `null` |
| `PKB-BL-016` | `VALIDATION` | `PKB-HOLDOUT-002` | Generalization is untested; execute the sealed holdout once. | One blind immutable execution. | User / Engineering | `null` |
| `PKB-BL-017` | `VALIDATION` | `PKB-DECISION-001` | No next-run Stage B decision exists; review evidence and issue a bounded result. | Decision only; no automatic publication. | Product Team / Evaluation owner | `null` |
| `PKB-BL-018` | `FEATURE` | `PKB-COMPONENT-001` | Completed foundation record: durable structural identity was absent; Java contract now enforces it. | Component identity contract and tests. | Product Team / Engineering | completed Task 1 |
| `PKB-BL-019` | `FEATURE` | `PKB-PROPOSAL-001` | Completed foundation record: proposal boundaries were absent; immutable Java proposal contract now enforces them. | Proposal contract and tests. | Product Team / Engineering | completed Task 2 |
| `PKB-BL-020` | `SECURITY` | `PKB-ISOLATION-001` | Completed foundation record: next-run proposal authority and gold isolation needed enforcement; v0.2 and gate tests enforce it. | Skill isolation and proposal-only boundary. | Product Team / Engineering | completed Task 3 |
| `PKB-BL-021` | `VALIDATION` | `PKB-COMPARISON-001` | Completed foundation record: hierarchical metrics were conflated; deterministic comparator now separates them. | Pure comparison implementation and tests. | Evaluation owner / Engineering | completed Task 4 |
| `PKB-BL-022` | `VALIDATION` | `PKB-READINESS-001` | Completed foundation record: next-run inputs lacked an executable gate; fail-closed readiness now exists. | Schema, readiness API/CLI, and tests; no generation. | User / Engineering | completed Task 5 |

## Verified foundation delivery records

The following items predate the current backlog format and are reconciled to
the bound Spec revision through their existing task ledgers and test evidence.

| Backlog ID | Priority | Status | Dependencies | Deliverables | Required verification | Completion evidence |
|---|---|---|---|---|---|---|
| `PKB-BL-018` | `P0` | `VERIFIED` | none | `StructuralComponentIdentity` and tests | Java constructor, canonical-path, granularity, and revision tests | Task 1 commits `d483c39d`, `b634d0fb`; full Java regression passed |
| `PKB-BL-019` | `P0` | `VERIFIED` | `PKB-BL-018` | `RealizationComponent`, `RealizationProposal`, and tests | Authority, role, revision, immutability, and outcome tests | Task 2 commits `40adc0c`, `383cac7`; full Java regression passed |
| `PKB-BL-020` | `P0` | `VERIFIED` | `PKB-BL-019` | versioned PK-S1 v0.2 and isolation tests | Historical digest, proposal-only, forbidden-input, and no-publication tests | Task 3 commits in completion ledger; full repository regression passed |
| `PKB-BL-021` | `P0` | `VERIFIED` | `PKB-BL-018` | hierarchical comparator and tests | Path/type/bare-symbol/exact-component/chain/channel tests | Task 4 commits in completion ledger; full repository regression passed |
| `PKB-BL-022` | `P0` | `VERIFIED` | `PKB-BL-019`, `PKB-BL-020` | v0.2 schema, readiness gate, CLI, and tests | Input/digest/revision/identity/run-ID/hostile-shape/CLI tests | Task 5 commits in completion ledger; 82 focused and 259 full tests passed |

### PKB-BL-001 — Isolated Stage A review packet

- Priority: `P0`
- Status: `READY`
- Requirement: Product Capability behavior scenarios / Scenario authority and
  isolation
- Depends on: none
- Deliverables: a Stage A Product Semantics review packet containing Capability
  names, intent, boundaries, draft scenarios, and permitted Reverse evidence
  summaries; classification of the existing combined packet as Stage B or
  historical evaluation reference
- Acceptance: the Stage A packet contains no evaluator expected components,
  proposed technical components, gold identifiers, Graphify node IDs, exact-
  match metrics, or evaluator technical recommendations; an automated test
  enforces the forbidden categories
- Completion evidence: pending

### PKB-BL-002 — Active Stage A status pointer

- Priority: `P0`
- Status: `BLOCKED_DEPENDENCY`
- Requirement: Active project truth and ordered two-stage review
- Depends on: `PKB-BL-001`
- Deliverables: updated `STATUS.json` human-review pointer and next action
- Acceptance: the pointer resolves to the verified Stage A packet; the existing
  combined packet remains immutable and is not represented as Stage A
- Completion evidence: pending

### PKB-BL-003 — Product Team Stage A decision

- Priority: `P0`
- Status: `BLOCKED_DEPENDENCY`; after `PKB-BL-001` and `PKB-BL-002` are
  verified, transition to `BLOCKED_PRODUCT_TEAM`
- Requirement: Stage A semantic ownership
- Depends on: `PKB-BL-001`, `PKB-BL-002`
- Deliverables: Product Team decisions for Capability names, intent,
  includes/excludes/non-goals, scenarios, and merge/split/rename/reject actions
- Acceptance: every required item has reviewer identity, decision time, reason,
  and explicit Product Team authority; no evaluator or agent is recorded as the
  Product Team decision-maker
- Completion evidence: pending

### PKB-BL-004 — Existing disagreement adjudication

- Priority: `P1`
- Status: `READY`
- Requirement: current bounded evaluation protocol
- Depends on: none
- Deliverables: independent third evaluator judgments for the 11 recorded
  action/outcome disagreements
- Acceptance: only the 11 disputed items are adjudicated in an isolated
  evaluator-only context; results do not complete Stage A, change Product
  Semantics, or authorize publication
- Completion evidence: pending

### PKB-BL-005 — Product-owned scenario schema

- Priority: `P1`
- Status: `READY`
- Requirement: Product Capability behavior scenarios / Scenario contract
- Depends on: none
- Deliverables: provider-neutral scenario schema, validation implementation,
  and positive/negative fixtures
- Acceptance: stable scenario and Capability IDs, Given/When/Then, scope,
  boundaries, `DRAFT/FROZEN`, Product Team provenance, and immutable revision
  binding are enforced; implementation identifiers, provider nodes, evaluator
  mappings, and technical selection instructions fail closed
- Completion evidence: pending

### PKB-BL-006 — Frozen scenario-bearing semantics revision

- Priority: `P1`
- Status: `BLOCKED_DEPENDENCY`; after its dependencies are verified, transition
  to `BLOCKED_PRODUCT_TEAM` until the freeze is explicitly approved
- Requirement: Scenario authority and isolation
- Depends on: `PKB-BL-003`, `PKB-BL-005`
- Deliverables: new immutable Product Semantics revision containing approved
  Capabilities and scenarios, plus manifest and approval provenance
- Acceptance: every Forward scenario is Product Team-owned and `FROZEN`; old
  Petclinic semantics and artifacts remain byte-identical; no Reverse hypothesis
  enters the revision without an explicit Product Team decision
- Completion evidence: pending

### PKB-BL-007 — Scenario-grounded PK-S1 contract

- Priority: `P1`
- Status: `BLOCKED_DEPENDENCY`
- Requirement: Scenario-grounded realization
- Depends on: `PKB-BL-005`, `PKB-BL-006`
- Deliverables: new versioned PK-S1 skill, Java/provider-neutral contract,
  proposal schema, readiness rules, and isolation tests
- Acceptance: proposal-local component references, scenario traces, variable
  realization chains, behavioral PRIMARY/SUPPORTING reasons, explicit evidence
  gaps, and separate `outcome`/`evidence_status` are enforced; v0.2 remains
  immutable; evaluator gold remains forbidden
- Completion evidence: pending

### PKB-BL-008 — Real Graphify UI/template capability verification

- Priority: `P1`
- Status: `READY`
- Requirement: Template and UI evidence
- Depends on: none
- Deliverables: exact-runtime evidence for template, view, navigation,
  form-binding, and controller-to-view support or absence
- Acceptance: every claim is bound to installed Graphify identity, exact source
  revision, frozen graph digest, and actual supported operation; unsupported
  behavior becomes an evidence gap, not an assumed API or weakened Product
  Semantics
- Completion evidence: pending

### PKB-BL-009 — Reverse proposal quality controls

- Priority: `P1`
- Status: `BLOCKED_DEPENDENCY`
- Requirement: Reverse use and Product-truth boundary
- Depends on: `PKB-BL-005`
- Deliverables: proposal-only duplicate, composite, rename, merge/split,
  claim-to-evidence, confidence, and limitation checks
- Acceptance: scenario hypotheses use `HYP-SCENARIO-*`, remain
  `PROPOSAL_ONLY / UNREVIEWED`, cite structural or delivery evidence, and cannot
  write Product Semantics or Forward inputs
- Completion evidence: pending

### PKB-BL-010 — Provider-neutral evaluator truth

- Priority: `P1`
- Status: `BLOCKED_DEPENDENCY`
- Requirement: Hierarchical evaluation
- Depends on: `PKB-BL-005`, `PKB-BL-007`
- Deliverables: a new evaluator-only schema and sealed truth format containing
  normalized component identity and scenario expectations
- Acceptance: formal identity uses `(source_revision, source_path, granularity,
  qualified_symbol)`; provider node IDs are provenance diagnostics only; gold
  remains inaccessible to generation
- Completion evidence: pending

### PKB-BL-011 — Scenario and component metrics

- Priority: `P1`
- Status: `BLOCKED_DEPENDENCY`
- Requirement: next-run semantic and technical evaluation
- Depends on: `PKB-BL-007`, `PKB-BL-010`
- Deliverables: deterministic evaluator and report schema
- Acceptance: semantic decisions are separated from scenario evidence,
  complete-chain coverage, provider-neutral exact-component precision/recall/F1,
  missing/extra components, unresolved rate, UI/template gaps, and macro per-
  Capability results; provider-node/path/type/bare-symbol/supporting metrics are
  separately named diagnostics
- Completion evidence: pending

### PKB-BL-012 — Acceptance-threshold preregistration

- Priority: `P1`
- Status: `BLOCKED_DEPENDENCY`; after its dependencies are verified, transition
  to `BLOCKED_PRODUCT_TEAM`
- Requirement: Calibration strategy
- Depends on: `PKB-BL-003`, `PKB-BL-011`
- Deliverables: approved metric definitions, declared false-positive/false-
  negative costs, numeric thresholds, and immutable preregistration digest
- Acceptance: thresholds are justified independently of observed Petclinic
  values and frozen before proposal generation; unapproved values remain
  `PROPOSED_NOT_FROZEN`
- Completion evidence: pending

### PKB-BL-013 — Holdout candidate approval and seal

- Priority: `P2`
- Status: `BLOCKED_USER_APPROVAL`
- Requirement: user-approved sealed holdout
- Depends on: none for candidate proposal; user approval for sealing
- Deliverables: independently proposed repository and full Git revision, user
  approval record, frozen source digest, and access seal
- Acceptance: the user explicitly approves the repository and revision; the
  holdout truth remains inaccessible while rules and thresholds are completed
- Completion evidence: pending

### PKB-BL-014 — Frozen next-experiment protocol

- Priority: `P2`
- Status: `BLOCKED_DEPENDENCY`
- Requirement: preregistration and re-freeze rules
- Depends on: `PKB-BL-006`, `PKB-BL-007`, `PKB-BL-008`, `PKB-BL-009`,
  `PKB-BL-010`, `PKB-BL-011`, `PKB-BL-012`, `PKB-BL-013`
- Deliverables: immutable manifest binding Stage A semantics, scenarios,
  provider/runtime identity, source revisions, query bounds, skill, schemas,
  comparator, metrics, and thresholds
- Acceptance: every input has an exact digest; missing or changed bindings block
  execution; no existing run ID is reused
- Completion evidence: pending

### PKB-BL-015 — Petclinic regression under frozen protocol

- Priority: `P2`
- Status: `BLOCKED_DEPENDENCY`
- Requirement: Calibration strategy
- Depends on: `PKB-BL-014`
- Deliverables: new immutable Petclinic proposal and evaluation artifacts
- Acceptance: existing artifacts remain unchanged; the new run uses the frozen
  protocol; any subsequent frozen-input change creates a new protocol revision
  and requires regression restart while the holdout remains sealed
- Completion evidence: pending

### PKB-BL-016 — One-shot blind holdout execution

- Priority: `P2`
- Status: `BLOCKED_DEPENDENCY`
- Requirement: user-approved sealed holdout and execution
- Depends on: `PKB-BL-015`
- Deliverables: one immutable blind holdout run, manifest, evidence, and
  evaluator comparison
- Acceptance: execution occurs once under the unchanged frozen protocol;
  generation never accesses evaluator truth; failures are recorded rather than
  overwritten or rerun under the same protocol/run ID
- Completion evidence: pending

### PKB-BL-017 — Stage B review and bounded decision

- Priority: `P2`
- Status: `BLOCKED_DEPENDENCY`
- Requirement: Stage B realization review and GO / REVISE / STOP boundary
- Depends on: `PKB-BL-016`
- Deliverables: Product Team Stage B decisions, final evaluation report, and
  bounded `GO / REVISE / STOP` decision
- Acceptance: Stage B cannot mutate Stage A semantics; all registered gates are
  evaluated without retroactive threshold changes; even `GO` does not publish
  Product Semantics without a separate explicit Product Team action
- Completion evidence: pending

## Backlog execution order

```text
BL-001 → BL-002 → BL-003 → BL-006
                 BL-005 ──┘   → BL-007 → BL-010 → BL-011 → BL-012
BL-008 ──────────────────────────────────────────┐
BL-009 ───────────────────────────────────────────┤
BL-013 (user approval and seal) ───────────────────┤
                                                    ↓
                                                 BL-014
                                                    ↓
                                                 BL-015
                                                    ↓
                                                 BL-016
                                                    ↓
                                                 BL-017

BL-004 is an independent completion item for the existing bounded evaluation
and does not grant Product Team authority or unblock scenario publication.
```

The immediate backlog item is `PKB-BL-001`. No later execution item may be
started merely because it appears in this ordered list.

## Spec maturity traceability

This summary covers every requirement in `FRAMEWORK-SPEC.md` at the bound
revision. It measures the new scenario-grounded contract and its verified
foundation; it does not replace the completed bounded Petclinic decision.

| Maturity | Requirement IDs | Count |
|---|---|---:|
| `M3_VERIFIED` | `PKB-COMPONENT-001`, `PKB-PROPOSAL-001`, `PKB-ISOLATION-001`, `PKB-COMPARISON-001`, `PKB-READINESS-001` | 5 |
| `M1_BACKLOGGED` | `PKB-REVIEW-001`, `PKB-STATUS-001`, `PKB-REVIEW-002`, `PKB-EVAL-LEGACY-001`, `PKB-SCENARIO-001`, `PKB-SCENARIO-002`, `PKB-MAPPING-001`, `PKB-PROVIDER-001`, `PKB-REVERSE-001`, `PKB-EVAL-001`, `PKB-EVAL-002`, `PKB-CALIBRATION-001`, `PKB-HOLDOUT-001`, `PKB-PROTOCOL-001`, `PKB-REGRESSION-001`, `PKB-HOLDOUT-002`, `PKB-DECISION-001` | 17 |

```yaml
spec_binding:
  path: FRAMEWORK-SPEC.md
  revision: 4f8e903181bf79178af37672e1cb57699c4c93f7
normative_requirements: 22
m3_verified: 5
m1_backlogged: 17
next_experiment_readiness: NOT_READY
blocking_requirements:
  - PKB-REVIEW-001
  - PKB-STATUS-001
  - PKB-REVIEW-002
  - PKB-SCENARIO-001
  - PKB-SCENARIO-002
  - PKB-MAPPING-001
  - PKB-EVAL-001
  - PKB-EVAL-002
  - PKB-CALIBRATION-001
  - PKB-HOLDOUT-001
  - PKB-PROTOCOL-001
```

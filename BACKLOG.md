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
`BLOCKED_DEPENDENCY` and must be reverified. Human Reviewer and user approval
states cannot be cleared by an agent.

## Prioritized backlog items

Affected records were reconciled to the approved individual-review requirements;
BL-005/023/024 are now verified by their completion evidence. The five foundation requirements
retain their existing verification evidence; their historical runtime accepts
PRODUCT_TEAM ownership and requires a new contract under BL-005/007 before
HUMAN_REVIEWER inputs can run.

All active records below bind to `FRAMEWORK-SPEC.md` at exact Git revision
`891e497968000c32984f26437eab811c063ec4cf`. This binding matrix is part of
each Backlog record; detailed priority, status, dependencies, deliverables,
verification, and completion evidence follow in the item sections.

Java-boundary reconciliation: new requirement `PKB-JAVA-001` is tracked by
BL-026. Existing verified records retain their functional completion evidence,
but Python framework tooling is transitional and cannot demonstrate completion
of the Java-only target. BL-007 therefore requires reconciliation before more
implementation. External Graphify Python is excluded from BL-026. No experiment
gate was cleared by this documentation amendment.

| Backlog ID | Work type | Source requirement | Current gap / intended outcome | Scope boundary | Decision / implementation owner | Active plan |
|---|---|---|---|---|---|---|
| `PKB-BL-026` | `TECH_DEBT` | `PKB-JAVA-001` | Repository-owned Python framework consumers remain; migrate them one bounded consumer at a time to Java. | FDI framework only; external Graphify Python excluded; historical evidence immutable. | User / Engineering | `null` |
| `PKB-BL-023` | `FEATURE` | `PKB-REVIEW-003` | Generate evidence-backed Capability/scenario proposals and a single review surface. | Proposal generation and rendering; no human decisions. | User / Engineering | `IMPLEMENTATION-PLAN.md#selected-work-generated-scenarios-and-individual-review` |
| `PKB-BL-024` | `DOCUMENTATION` | `PKB-STATUS-002` | Point status to actual generated review material and review state. | Verified pointer only; preserve previous packet. | User / Engineering | `null` |
| `PKB-BL-025` | `FEATURE` | `PKB-REVIEW-004` | User reviews generated proposals with ACCEPT / EDIT / REJECT. | Version-bound human decisions only. | User / User | `null` |
| `PKB-BL-004` | `VALIDATION` | `PKB-EVAL-LEGACY-001` | Eleven evaluator disagreements remain; adjudicate only those items. | Existing evaluation only; no Product authority. | Evaluation owner / Independent evaluator | `null` |
| `PKB-BL-005` | `FEATURE` | `PKB-SCENARIO-003` | Scenario rules are prose-only; make the lifecycle machine-verifiable. | Contract and validation; no scenario approval. | Human Reviewer / Engineering | `IMPLEMENTATION-PLAN.md#selected-work-generated-scenarios-and-individual-review` |
| `PKB-BL-006` | `FEATURE` | `PKB-SCENARIO-004` | No approved frozen scenario-bearing revision exists; create one without overwriting Petclinic. | New semantics revision only. | Human Reviewer / Engineering | `null` |
| `PKB-BL-007` | `FEATURE` | `PKB-MAPPING-001` | PK-S1 v0.2 lacks scenario traces; reconcile the completed transitional gate with the Java-only target before further work. | New skill/contract version; preserve v0.2. | Human Reviewer / Engineering | `null` |
| `PKB-BL-008` | `RESEARCH` | `PKB-PROVIDER-001` | UI/template support is unverified; record actual runtime capability or gaps. | Read-only discovery; no assumed API or runtime replacement. | User / Engineering | `null` |
| `PKB-BL-009` | `FEATURE` | `PKB-REVERSE-001` | Reverse output over-combines, duplicates, and overclaims; add proposal-only controls. | Proposal quality only; no semantic publication. | Human Reviewer / Engineering | `null` |
| `PKB-BL-010` | `VALIDATION` | `PKB-EVAL-001` | Existing truth relies on provider IDs; add sealed normalized identity. | New evaluator truth format; generation remains isolated. | Evaluation owner / Engineering | `null` |
| `PKB-BL-011` | `VALIDATION` | `PKB-EVAL-002` | Scenario, chain, component, and diagnostic measures are incomplete; add distinct metrics. | Deterministic evaluation only. | Evaluation owner / Engineering | `null` |
| `PKB-BL-012` | `VALIDATION` | `PKB-CALIBRATION-001` | No preregistered numeric gate exists; freeze justified thresholds. | Threshold definition only; no generation. | Human Reviewer / Evaluation owner | `null` |
| `PKB-BL-013` | `RESEARCH` | `PKB-HOLDOUT-001` | No approved sealed holdout exists; propose, approve, and seal one exact revision. | Candidate metadata and seal; no execution. | User / Independent selector | `null` |
| `PKB-BL-014` | `VALIDATION` | `PKB-PROTOCOL-001` | Next-run inputs are not jointly frozen; bind every executable input and digest. | Protocol manifest only; no run. | User / Engineering | `null` |
| `PKB-BL-015` | `VALIDATION` | `PKB-REGRESSION-001` | New rules lack regression evidence; run Petclinic under a frozen new protocol. | New immutable run; preserve completed run. | Evaluation owner / Engineering | `null` |
| `PKB-BL-016` | `VALIDATION` | `PKB-HOLDOUT-002` | Generalization is untested; execute the sealed holdout once. | One blind immutable execution. | User / Engineering | `null` |
| `PKB-BL-017` | `VALIDATION` | `PKB-DECISION-001` | No next-run experiment result review decision exists; review evidence and issue a bounded result. | Decision only; no automatic publication. | Human Reviewer / Evaluation owner | `null` |
| `PKB-BL-018` | `FEATURE` | `PKB-COMPONENT-001` | Completed foundation record: durable structural identity was absent; Java contract now enforces it. | Component identity contract and tests. | Human Reviewer / Engineering | completed Task 1 |
| `PKB-BL-019` | `FEATURE` | `PKB-PROPOSAL-001` | Completed foundation record: proposal boundaries were absent; immutable Java proposal contract now enforces them. | Proposal contract and tests. | Human Reviewer / Engineering | completed Task 2 |
| `PKB-BL-020` | `SECURITY` | `PKB-ISOLATION-001` | Completed foundation record: next-run proposal authority and gold isolation needed enforcement; v0.2 and gate tests enforce it. | Skill isolation and proposal-only boundary. | Human Reviewer / Engineering | completed Task 3 |
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

## Superseded review work

`PKB-BL-001`, `PKB-BL-002`, and `PKB-BL-003` are `DEFERRED` and
superseded by BL-023/024/025 following the approved individual-review design.
Their old human semantic review/B acceptance criteria no longer gate the prototype. IDs are
retained here for history and are excluded from active requirement counts.

### PKB-BL-023 — Generated proposals and individual review surface

- Priority: `P0`
- Status: `VERIFIED`
- Requirement: `PKB-REVIEW-003`
- Depends on: `PKB-BL-005`
- Deliverables: versioned generation instructions, validated proposal envelope,
  Markdown/JSON review surface, evidence and limitation fields
- Acceptance: source revision, graph digest and delivery cutoff are checked;
  cited evidence resolves; scenarios separate behavior from technical evidence;
  confidence is labeled uncalibrated; unavailable channels are explicit;
  proposals remain PROPOSAL_ONLY / UNREVIEWED; generation cannot read gold
- Completion evidence: scenario implementation commit `fc7b304` and `f5d8bc6`; 44 focused Python tests, Java contract tests, and immutable proposal/review artifacts under `validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/`. Final integration evidence is recorded in `verification.json` there.

### PKB-BL-024 — Active individual-review status pointer

- Priority: `P0`
- Status: `VERIFIED`
- Requirement: `PKB-STATUS-002`
- Depends on: `PKB-BL-023`
- Deliverables: status pointer to generated review material and actual state
- Acceptance: pointer resolves and matches the declared proposal revision;
  pending decisions are not represented as accepted; existing packet preserved
- Completion evidence: scenario implementation commit `fc7b304` and `f5d8bc6`; 44 focused Python tests, Java contract tests, and immutable proposal/review artifacts under `validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/`. Final integration evidence is recorded in `verification.json` there.

### PKB-BL-025 — Individual review decisions

- Priority: `P0`
- Status: `BLOCKED_USER_APPROVAL`
- Requirement: `PKB-REVIEW-004`
- Depends on: `PKB-BL-023`, `PKB-BL-024`
- Deliverables: user ACCEPT / EDIT / REJECT decisions bound to proposal digests,
  reviewer identity, time and reason; accepted edited text where applicable
- Acceptance: rejected/unreviewed/unconfirmed edits cannot enter frozen inputs;
  the agent never substitutes its judgment for the user's acceptance
- Progress: First-slice evidence: the user explicitly accepted Capability 001 and Scenarios 001/002. Decisions are recorded in `review-decisions-001.json` under the current scenario run; the other 13 decisions remain pending. This item is partially complete, not VERIFIED.
- Completion evidence: pending for the complete item scope

### PKB-BL-004 — Existing disagreement adjudication

- Priority: `P1`
- Status: `IN_PROGRESS`
- Requirement: current bounded evaluation protocol
- Depends on: none
- Deliverables: independent third evaluator judgments for the 11 recorded
  action/outcome disagreements
- Acceptance: only the 11 disputed items are adjudicated in an isolated
  evaluator-only context; results do not complete human review, change Product
  Semantics, or authorize publication
- Completion evidence: pending

### PKB-BL-005 — Generated scenario and review lifecycle contract

- Priority: `P1`
- Status: `VERIFIED`
- Requirement: Product Capability behavior scenarios / Scenario contract
- Depends on: none
- Deliverables: provider-neutral scenario schema, validation implementation,
  and positive/negative fixtures
- Acceptance: stable scenario and Capability IDs, Given/When/Then, scope,
  boundaries, proposal UNREVIEWED state, ACCEPT/EDIT/REJECT decisions, DRAFT/FROZEN
  accepted snapshot lifecycle, reviewer provenance and immutable revision binding
  are enforced; technical identifiers are permitted only in the separate evidence
  envelope; rejected and unconfirmed edited scenarios cannot enter Forward inputs
- Completion evidence: scenario implementation commit `fc7b304` and `f5d8bc6`; 44 focused Python tests, Java contract tests, and immutable proposal/review artifacts under `validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/`. Final integration evidence is recorded in `verification.json` there.

### PKB-BL-006 — Frozen scenario-bearing semantics revision

- Priority: `P1`
- Status: `BLOCKED_DEPENDENCY`; after its dependencies are verified, transition
  to `BLOCKED_USER_APPROVAL` until the freeze is explicitly approved
- Requirement: Scenario authority and isolation
- Depends on: `PKB-BL-025`, `PKB-BL-005`
- Deliverables: new immutable Product Semantics revision containing approved
  Capabilities and scenarios, plus manifest and approval provenance
- Acceptance: every Forward scenario is Human Reviewer-owned and `FROZEN`; old
  Petclinic semantics and artifacts remain byte-identical; no Reverse hypothesis
  enters the revision without an explicit Human Reviewer decision
- Progress: First-slice evidence: `accepted-semantics-001.json` and `acceptance-manifest-001.json` under the current scenario run freeze exactly the accepted Capability 001 and two scenarios. The full six-Capability scope is not frozen; no overall readiness gate is cleared.
- Completion evidence: pending for the complete item scope

### PKB-BL-007 — Scenario-grounded PK-S1 contract

- Priority: `P1`
- Status: `NEEDS_RECONCILIATION`
- Requirement: Scenario-grounded realization
- Depends on: `PKB-BL-005`, `PKB-BL-006`
- Deliverables: new versioned PK-S1 skill, Java/provider-neutral contract,
  proposal schema, readiness rules, and isolation tests
- Acceptance: proposal-local component references, scenario traces, variable
  realization chains, behavioral PRIMARY/SUPPORTING reasons, explicit evidence
  gaps, and separate `outcome`/`evidence_status` are enforced; v0.2 remains
  immutable; evaluator gold remains forbidden
- Active plan: `null`; the prior plan remains a completed transitional delivery record.
- Progress: Bounded Tasks A–D implemented and verified: Java scenario-chain contract, v0.3 skill/schema, 36 cross-language parity fixtures and a transitional Python frozen-input gate. Evidence: `7ee1395`, `c12e2f7`; 109 focused/415 full Python tests, 94 Java tests, public validation 9/9. The Python consumer must be replaced through BL-026 before it can represent the final framework architecture. Full BL-006 and experiment execution remain gated; this is not full-item completion.
- Completion evidence: pending for the complete item scope

### PKB-BL-026 — Java-only framework migration

- Priority: `P0`
- Status: `IN_PROGRESS`
- Requirement: Framework implementation language and migration
- Depends on: none
- Deliverables: a complete inventory and classification of repository-owned
  Python framework consumers; ordered migration slices; Java API/CLI replacements;
  caller cutover; regression evidence; and removal of each replaced Python
  consumer after verification
- Acceptance: no new Product or framework behavior is added in Python; external
  Graphify Python remains unchanged behind the Java adapter and MCP boundary;
  each Java replacement preserves valid/invalid contract behavior and fail-closed
  errors; all active callers are cut over before removal; immutable historical
  artifacts remain unchanged
- Active plan: `null`; the first bounded migration plan is complete and retained
  in `IMPLEMENTATION-PLAN.md` as its construction and verification record.
- Progress: Java-only framework boundary recorded in `FRAMEWORK-SPEC.md` at
  `891e497968000c32984f26437eab811c063ec4cf`. The first bounded consumer,
  `tooling/validation/pkb001_scenario_forward_gate.py`, was replaced by the
  Java API and packaged CLI, all active callers were cut over, and the Python
  source plus its direct Python-only test were removed in `f5ebd3a`. Verification
  passed 162 Java tests, 312 remaining Python tests with 3 skips, all 36 shared
  parity fixtures, and public validation 9/9. Other repository-owned Python
  consumers remain transitional; external Graphify Python is unchanged.
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
  to `BLOCKED_USER_APPROVAL`
- Requirement: Calibration strategy
- Depends on: `PKB-BL-025`, `PKB-BL-011`
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
- Deliverables: immutable manifest binding human semantic review semantics, scenarios,
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

### PKB-BL-017 — experiment result review review and bounded decision

- Priority: `P2`
- Status: `BLOCKED_DEPENDENCY`
- Requirement: experiment result review realization review and GO / REVISE / STOP boundary
- Depends on: `PKB-BL-016`
- Deliverables: Human Reviewer experiment result review decisions, final evaluation report, and
  bounded `GO / REVISE / STOP` decision
- Acceptance: experiment result review cannot mutate human semantic review semantics; all registered gates are
  evaluated without retroactive threshold changes; even `GO` does not publish
  Product Semantics without a separate explicit Human Reviewer action
- Completion evidence: pending

## Backlog execution order

BL-026 remains the active migration backlog after completing its first bounded
Python-consumer replacement. No next consumer or active implementation plan is
selected. The Human Reviewer must select the next bounded consumer before the
plan is revised or another implementation is dispatched. The selected
construction plan completed `PKB-BL-005` and `PKB-BL-023`.
BL-024 now points to the current review progress. BL-025 has three accepted
decisions and 13 pending decisions. BL-006 has frozen that first accepted slice;
its full-scope completion remains pending. BL-007 needs reconciliation against
BL-026 before any further implementation and cannot bypass execution prerequisites.
BL-007/010/011 then establish scenario mapping and evaluation. BL-008/009,
thresholds, holdout approval and protocol prerequisites still gate later runs.
BL-004 adjudicates the existing 11 disagreements independently of human review.

## Spec maturity traceability

This summary covers every requirement in `FRAMEWORK-SPEC.md` at the bound
revision. It measures the new scenario-grounded contract and its verified
foundation; it does not replace the completed bounded Petclinic decision.

| Maturity | Requirement IDs | Count |
|---|---|---:|
| `M3_VERIFIED` | `PKB-COMPONENT-001`, `PKB-PROPOSAL-001`, `PKB-ISOLATION-001`, `PKB-COMPARISON-001`, `PKB-READINESS-001`, `PKB-SCENARIO-003`, `PKB-REVIEW-003`, `PKB-STATUS-002` | 8 |
| `M1_BACKLOGGED` | `PKB-REVIEW-004`, `PKB-EVAL-LEGACY-001`, `PKB-SCENARIO-004`, `PKB-MAPPING-001`, `PKB-PROVIDER-001`, `PKB-REVERSE-001`, `PKB-EVAL-001`, `PKB-EVAL-002`, `PKB-CALIBRATION-001`, `PKB-HOLDOUT-001`, `PKB-PROTOCOL-001`, `PKB-REGRESSION-001`, `PKB-HOLDOUT-002`, `PKB-DECISION-001`, `PKB-JAVA-001` | 15 |

```yaml
spec_binding:
  path: FRAMEWORK-SPEC.md
  revision: 891e497968000c32984f26437eab811c063ec4cf
normative_requirements: 23
m3_verified: 8
m1_backlogged: 15
next_experiment_readiness: NOT_READY
blocking_requirements:
  - PKB-JAVA-001
  - PKB-REVIEW-004
  - PKB-SCENARIO-004
  - PKB-MAPPING-001
  - PKB-EVAL-001
  - PKB-EVAL-002
  - PKB-CALIBRATION-001
  - PKB-HOLDOUT-001
  - PKB-PROTOCOL-001
```

# FDI Agent Instructions

## Mandatory Read Order

Before doing any project work, read these files in order:

1. `PROJECT-OVERVIEW.md`
2. `FRAMEWORK-SPEC.md`
3. `IMPLEMENTATION-PLAN.md`
4. `STATUS.json`

These four files are the only project-level active control documents.

The active backlog is currently the `PKB-001 backlog` section inside
`IMPLEMENTATION-PLAN.md`. Do not create a separate project-level backlog or
another active control document unless the user explicitly changes this rule.

## Document Authority Rule

Do not determine current project truth from:

- version numbers in filenames
- old rc documents
- patches
- handoff documents
- review notes
- archived implementation plans
- historical chat context

Files under `archive/` are historical reference only.

Supporting files under `contracts/`, `skills/`, `validation/`, `src/`, `config/`, and `tests/` provide implementation detail or evidence, but must not override the four active control documents.

If a supporting artifact conflicts with an active control document, stop and report the conflict.

## Current Work

Read `STATUS.json` to determine:

- current focus
- current phase
- completed work
- blockers
- next action

Do not infer project status from old documents.

## Change Discipline

When project-level truth changes, update the appropriate active document:

- Project purpose / architecture / scope → `PROJECT-OVERVIEW.md`
- Framework capability / contract / authority boundary → `FRAMEWORK-SPEC.md`
- Development sequence / milestones / next work → `IMPLEMENTATION-PLAN.md`
- Current execution state / gate / blocker / next action → `STATUS.json`

Do not create version-suffixed replacements such as:

- `FRAMEWORK-SPEC-v2.md`
- `IMPLEMENTATION-PLAN-v0.14.md`
- `STATUS-v3.json`

Use Git history for versioning.

## Spec, Backlog, Plan, and Evidence Model

Use this control flow:

```text
Framework Spec
→ Backlog
→ explicitly selected Backlog Item or cohesive Item set
→ Implementation Plan
→ implementation and verification evidence
→ Backlog delivery status and Spec maturity update
```

This is a traceability loop, not permission to execute every listed item.

### Framework Spec

`FRAMEWORK-SPEC.md` defines the system's What, Why, architecture-level How,
normative requirements, authority boundaries, failure behavior, and acceptance
semantics. Product behavior remains implementation-agnostic; architecture-level
How may define components, interfaces, data flow, identity, state transitions,
isolation, and fail-closed behavior.

Give every normative requirement a stable requirement ID. Do not reuse an ID
for different semantics. Do not duplicate normative wording in Backlog or an
Implementation Plan; reference the controlling requirement ID and bound Spec
revision instead.

### Backlog

Backlog records the gap between the bound Spec revision and current verified
implementation. Every item must include:

- stable `backlog_id` and `work_type`;
- source and bound Spec path/revision/requirement IDs when applicable;
- current gap and intended outcome;
- priority, status, dependencies, and blockers;
- in-scope and out-of-scope boundaries;
- deliverables and required verification;
- decision and implementation owners;
- active Implementation Plan link, or `null` when not selected;
- completion evidence, empty until verified.

Allowed `work_type` values are `FEATURE`, `BUG`, `SECURITY`, `TECH_DEBT`,
`VALIDATION`, `DOCUMENTATION`, `OPERATION`, and `RESEARCH`. Bug, review,
security, and operational work may originate outside a Spec requirement, but
must record its source. If that work changes normative behavior, update and
approve the Spec first.

Allowed delivery states are `READY`, `IN_PROGRESS`, `BLOCKED_DEPENDENCY`,
`BLOCKED_PRODUCT_TEAM`, `BLOCKED_USER_APPROVAL`, `NEEDS_RECONCILIATION`,
`DEFERRED`, and `VERIFIED`. `READY` means eligible for selection, not authorized
to execute. Product Team and user blockers cannot be cleared by an agent.

When a bound Spec requirement changes, mark affected Backlog items
`NEEDS_RECONCILIATION`. Reconcile their scope, acceptance verification, and
plans against the new Spec revision before further implementation or before
retaining `VERIFIED`.

The current 22-item PKB-001 backlog is reconciled to the exact Framework Spec
revision recorded in `IMPLEMENTATION-PLAN.md`. Five entries are verified
foundation delivery records and 17 represent the next experiment. If the bound
Spec changes, apply `NEEDS_RECONCILIATION` before using the affected entries.

### Implementation Plan

An Implementation Plan covers only an explicitly selected Backlog Item or a
small cohesive set that can be implemented and accepted together. It defines
the construction-level How: exact files/modules, API or schema details, TDD
sequence, migrations, commands, failure handling, verification, and commit
boundaries.

Do not interpret an unselected Backlog item as an active plan. Do not combine
unrelated decision points, provider changes, and experiment execution into one
plan. Bind every plan to the selected Backlog IDs and the same Spec revision.

### Evidence and maturity

Moving a Backlog item to `VERIFIED` requires objective completion evidence:
implementation paths, tests, verification results, commit IDs, and immutable
artifact digests where applicable. A checkbox, prose claim, generated proposal,
evaluator judgment, or agent report alone is insufficient.

Backlog delivery status and Spec maturity are different. Track requirement
maturity as:

- `M0_SPECIFIED`: normative requirement exists;
- `M1_BACKLOGGED`: implementation gap and acceptance verification are defined;
- `M2_IMPLEMENTED`: implementation exists but verification is incomplete;
- `M3_VERIFIED`: contract and regression evidence pass;
- `M4_VALIDATED`: exact-revision runtime evidence and required Product Team
  review pass;
- `M5_OPERATIONAL`: continuing operational ownership, monitoring, and recovery
  are established.

Calculate overall readiness from mandatory gates, not an average maturity
percentage. One unresolved authority, isolation, binding, or evidence-integrity
gate prevents a ready claim even when most other requirements are verified.

## PKB-001 Principles

Current prototype focus is defined by `STATUS.json`.

For PKB-001:

- Product Team owns Product Semantics.
- Graphify provides structural evidence, not Product truth.
- Delivery History provides historical evidence, not Product truth.
- `Product Semantics + Structural Intelligence → Capability → Component` is the forward experiment.
- `Structural Intelligence + Delivery History → Capability Hypothesis` is the reverse experiment.
- Reverse inference produces proposals only and requires human review.
- Do not automatically publish inferred Product Semantics.

## Agent Behavior

Before implementation:

1. Read the four active control documents.
2. Confirm the current task against `STATUS.json`.
3. Read only the supporting artifacts needed for that task.
4. Do not reopen archived design/version debates unless explicitly requested.
5. Do not create new governance, specification, or planning documents when an existing active control document should be updated.
6. Prefer updating existing project truth over creating another competing document.
7. Resolve the requested work through Spec requirement → Backlog item →
   selected Implementation Plan → evidence before claiming completion.
8. Never start work solely because a Backlog item is `READY`; verify that the
   user or current active plan explicitly selected it.

## Resource Safety

Keep command memory use below 8 GB. Use bounded inputs and concurrency, and use
`MAVEN_OPTS='-Xmx2g'` for Maven verification unless a stricter active control
requires a lower limit.

If the current task cannot be reconciled with the four active control documents, report `CONTEXT_CONFLICT` rather than guessing.

# FDI Agent Instructions

## Mandatory Read Order

Before doing any project work, read these files in order:

1. `PROJECT-OVERVIEW.md`
2. `FRAMEWORK-SPEC.md`
3. `BACKLOG.md`
4. `IMPLEMENTATION-PLAN.md`
5. `STATUS.json`

These five files are the only project-level active control documents. The
active backlog is `BACKLOG.md`; `IMPLEMENTATION-PLAN.md` may reference selected
Backlog items but must not duplicate the backlog inventory.

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

Supporting files under `contracts/`, `skills/`, `validation/`, `src/`, `config/`, and `tests/` provide implementation detail or evidence, but must not override the five active control documents.

If a supporting artifact conflicts with an active control document, stop and report the conflict.

## Cross-Agent Authority

These instructions apply equally to every actor and software implementation
working on this repository. Before acting, an actor must use the
`AGENTS.md` and five active control documents from the exact checkout and Git
revision it was assigned.

External project descriptions, issues, prompts, handoffs, agent memory, chat
history, and orchestration-system metadata are work requests or historical
context only. They must not override, replace, or silently reinterpret the
checkout's active controls. An issue should identify the selected Backlog item,
Implementation Plan section, repository path, branch, and exact starting commit;
it should reference these controls instead of duplicating their rules.

If an external instruction conflicts with `AGENTS.md` or any active control
document, do not guess which version is newer and do not continue partially.
Stop before changing files, report `CONTEXT_CONFLICT`, and identify the exact
conflicting statements for Human Reviewer resolution.

## Delivery Authority Planes

Project authority is defined by responsibility plane, not by software, model,
agent vendor, or orchestration tool.

### Human Authority

Human Authority owns Product meaning, material Spec/scope/architecture/authority
changes, parent Backlog authorization, terminal parent closure, holdout
selection, and semantic-publication decisions. Human confirmation is not
required for implementation slices, review, remediation, or combined integration
inside an approved execution envelope.

### Feature Delivery Plane

The Feature Delivery Plane owns project truth and delivery control. It alone:

- maintains the five active control files;
- translates Spec requirements into Backlog records;
- writes the selected `IMPLEMENTATION-PLAN.md`;
- issues an exact execution envelope;
- validates returned delivery evidence;
- reconciles Backlog, Plan, and Status;
- prepares a parent closure candidate; and
- applies terminal closure after Human authorization.

### Execution Plane

The Execution Plane receives the Implementation Plan and execution envelope as
read-only inputs. It decomposes approved work, implements code/tests/schemas,
coordinates review and remediation, performs combined code integration, runs
verification, collects delivery KPIs, and returns delivery evidence.

The Execution Plane must not modify, replace, rename, regenerate, or create
versioned copies of any active control file. If the Plan cannot be executed as
written, it reports exactly one of:

- `PLAN_BLOCKED`: a dependency, environment, permission, or resource is absent;
- `PLAN_CONFLICT`: the Plan contradicts an active control or the exact codebase;
- `PLAN_CHANGE_REQUIRED`: API, scope, acceptance, or construction instructions
  must change.

The Execution Plane must not resolve these conditions by editing the Plan. The
Feature Delivery Plane investigates, updates project truth when authorized, and
issues a revised execution envelope.

A software product may implement either plane, but software identity never
grants authority.

## Active Control Maintenance

Only the Feature Delivery Plane may modify:

1. `PROJECT-OVERVIEW.md`
2. `FRAMEWORK-SPEC.md`
3. `BACKLOG.md`
4. `IMPLEMENTATION-PLAN.md`
5. `STATUS.json`

If more than one Feature Delivery Plane actor attempts to update project truth
for the same execution, stop with `CONTEXT_CONFLICT`.

When work is selected, `STATUS.json.active_execution` records project execution
state only: stable `execution_id`, exact `base_commit`,
`selected_backlog_items`, `execution_state`, and
`integration_candidate`. It must not store software, model, worker,
coordinator, issue, mention, or orchestration identity. With no selected work,
`active_execution` is `null`.

### Execution envelope

Before implementation, the Feature Delivery Plane writes the Plan and issues a
read-only envelope containing the execution ID, Backlog and requirement IDs,
exact Spec revision and base commit, owned and excluded paths, scope, acceptance
and negative cases, required verification/review/integration, resource limits,
and Human-only boundaries.

### Delivery evidence package

After combined integration, the Execution Plane returns one package containing
the execution/base/candidate identities, accepted slices, changed paths, review
and remediation results, verification evidence and digests, limitations,
blockers, recommended completion state, and token/cycle-time/first-pass KPIs.
The package is supporting evidence, not project truth, and cannot mark a Backlog
item `VERIFIED`. The Feature Delivery Plane independently reconciles it against
the active controls and exact candidate.

## Current Work

Read `STATUS.json` to determine:

- current focus
- current phase
- completed work
- blockers
- next action

Do not infer project status from old documents.

## Change Discipline

When project-level truth changes, the Feature Delivery Plane updates the
appropriate active document:

- Project purpose / architecture / scope → `PROJECT-OVERVIEW.md`
- Framework capability / contract / authority boundary → `FRAMEWORK-SPEC.md`
- Work inventory / dependency / maturity → `BACKLOG.md`
- Selected-work construction steps / commands / verification → `IMPLEMENTATION-PLAN.md`
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

- stable Backlog ID;
- work type;
- controlling requirement ID or documented non-Spec source;
- intended outcome;
- current delivery status;
- dependency, blocker, or completion-evidence pointer.

Ownership, exact base commit, files/modules, scope boundaries, deliverables,
acceptance criteria, verification commands, and execution-envelope constraints
are added to `IMPLEMENTATION-PLAN.md` only after an item is selected.

Allowed `work_type` values are `FEATURE`, `BUG`, `SECURITY`, `TECH_DEBT`,
`VALIDATION`, `DOCUMENTATION`, `OPERATION`, and `RESEARCH`. Bug, review,
security, and operational work may originate outside a Spec requirement, but
must record its source. If that work changes normative behavior, the Feature
Delivery Plane proposes the Spec update and Human Authority approves it first.

Allowed delivery states are `READY`, `IN_PROGRESS`, `BLOCKED_DEPENDENCY`,
`BLOCKED_USER_APPROVAL`, `NEEDS_RECONCILIATION`,
`DEFERRED`, and `VERIFIED`. `READY` means eligible for selection, not authorized
to execute. Human Reviewer and user blockers cannot be cleared by an agent.

When a bound Spec requirement changes, mark affected Backlog items
`NEEDS_RECONCILIATION`. Reconcile their scope, acceptance verification, and
plans against the new Spec revision before further implementation or before
retaining `VERIFIED`.

Backlog item counts and maturity are derived from `BACKLOG.md` and
`STATUS.json`. `AGENTS.md` must not contain current item counts, completion
percentages, selected Backlog IDs, or current execution status. If the bound
Spec changes, apply `NEEDS_RECONCILIATION` before using affected entries.

### Implementation Plan

An Implementation Plan covers only an explicitly selected Backlog Item or a
small cohesive set that can be implemented and accepted together. It defines
the construction-level How: exact files/modules, API or schema details, TDD
sequence, migrations, commands, failure handling, verification, and commit
boundaries.

Do not interpret an unselected Backlog item as an active plan. Do not combine
unrelated decision points, provider changes, and experiment execution into one
plan. Bind every plan to the selected Backlog IDs and the same Spec revision.

#### Compact plan maintenance lifecycle

The Feature Delivery Plane maintains **One active plan file**:
`IMPLEMENTATION-PLAN.md`. It must not duplicate the Backlog ledger,
the Framework Spec, source code, schemas, test fixtures, terminal logs, or full
historical reports. Link to those artifacts and retain only the constraints an
agent needs to execute or verify the selected work. Keep the file at or below
10 KB by default; exceeding that budget requires a concrete reason tied to the
currently selected work.

- **No selected work:** the Feature Delivery Plane states that no implementation slice is selected and retains
  only a compact verified-delivery ledger and continuation constraints, and set
  `STATUS.json.active_implementation_plan` plus its plan anchor to `null`.
- **Selection:** the Feature Delivery Plane replaces the current-selection section with one bounded plan
  bound to its Backlog ID, requirement ID, exact Spec revision, base commit,
  owned files, exclusions, acceptance criteria, TDD sequence, verification
  commands, and commit/removal boundaries. Update the Backlog active-plan link
  and `STATUS.json` in the same change.
- **Execution:** the Feature Delivery Plane updates only material plan state, blockers, changed decisions,
  and evidence references. Do not paste command output or repeat requirement and
  backlog prose. Test results belong in concise evidence summaries or supporting
  artifacts.
- **Completion:** after Human Authority confirms terminal closure, the Feature Delivery Plane replaces construction detail with a short ledger entry containing
  the delivered behavior, exact commit, verification summary, and evidence path.
  Clear the active-plan link and anchor; update Backlog status/maturity and
  `STATUS.json` together. Git history preserves removed planning detail.

Only the Feature Delivery Plane may edit `IMPLEMENTATION-PLAN.md`. The
Execution Plane reports slice progress, integration state, and review evidence
without editing the Plan. Parallel slices may modify only their explicitly
assigned, non-overlapping implementation and evidence files.

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
- `M4_VALIDATED`: exact-revision runtime evidence and required Human Reviewer
  review pass;
- `M5_OPERATIONAL`: continuing operational ownership, monitoring, and recovery
  are established.

Calculate overall readiness from mandatory gates, not an average maturity
percentage. One unresolved authority, isolation, binding, or evidence-integrity
gate prevents a ready claim even when most other requirements are verified.

## PKB-001 Principles

Current prototype focus is defined by `STATUS.json`.

For PKB-001:

- The user is the sole Human Reviewer / Experiment Owner.
- Agents propose Capabilities and Behavior Scenarios from Graphify and delivery history.
- The user reviews with ACCEPT / EDIT / REJECT; only accepted versions enter frozen experiment semantics.
- Evidence references stay separate from implementation-agnostic scenario text.
- No Product Team organization or Stage A/B packet split is required.
- Report reconstruction consistency and reviewer exposure honestly; it is not independent product-requirements validation.
- Graphify provides structural evidence, not Product truth.
- Delivery History provides historical evidence, not Product truth.
- `Product Semantics + Structural Intelligence → Capability → Component` is the forward experiment.
- `Structural Intelligence + Delivery History → Capability Hypothesis` is the reverse experiment.
- Reverse inference produces proposals only and requires human review.
- Do not automatically publish inferred Product Semantics.

## Java Framework Rule

All new executable FDI framework behavior must be implemented in Java 17 / Spring
Boot. This includes contracts, validation gates, orchestration, isolation,
comparison, evaluation, metrics, reports, and framework CLI entry points.

The external Graphify Python runtime is not FDI framework code. Keep it behind
`CodeIntelligenceProvider` and the Java Graphify adapter over MCP stdio. Do not
rewrite, remove, reinstall, or silently change that external runtime without an
explicitly selected provider task.

Existing Python files in this repository are transitional migration inputs. Do
not add new framework features to them. Port one bounded consumer at a time to
Java, verify observable behavior and active callers, then remove only the replaced
Python source and Python-only tests. A narrowly scoped Python correctness or
security fix is allowed only when necessary to preserve evidence or migration
parity. Skills and JSON Schemas are declarative assets and are not executable
Python framework implementations.

## Agent Behavior

Before implementation:

1. Read the five active control documents.
2. Confirm the current task against `STATUS.json`.
3. Read only the supporting artifacts needed for that task.
4. Do not reopen archived design/version debates unless explicitly requested.
5. The Execution Plane must not create governance, specification, planning, or
   competing project-truth documents; it reports `PLAN_CHANGE_REQUIRED` when an
   active control needs revision.
6. The Feature Delivery Plane updates existing project truth instead of creating
   a competing document.
7. Resolve the requested work through Spec requirement → Backlog item →
   selected Implementation Plan → evidence before claiming completion.
8. Never start work solely because a Backlog item is `READY`; verify that the
   user or current active plan explicitly selected it.

## Resource Safety

For the current Execution Plane implementation's tool-specific dispatch and
post-slice KPI analysis, follow
`validation/pkb001/operations/MULTICA-SLICE-OPTIMIZATION.md` (supporting guidance).

### Coordinated delivery boundary

For parallel work, the Execution Plane owns routing, implementation review,
remediation, combined code integration, verification, and KPI collection. It
returns evidence to the Feature Delivery Plane and never updates active control
files. Tool-specific routing,
single-trigger handoff, deduplication, worktree recovery, review intake, and KPI
rules are defined only in
`validation/pkb001/operations/MULTICA-SLICE-OPTIMIZATION.md`.

### Human gate matrix and automatic progression

When the active Backlog and Implementation Plan define an authorized execution
envelope, the Execution Plane advances every dependency-ready non-Human step in
order: implementation, independent review, bounded remediation, fresh review,
combined integration, combined review, and verification. It must not ask for
confirmation between these steps or start a later stage before the current
stage passes. It then returns one delivery evidence package to the Feature
Delivery Plane.

Creating and executing in-scope slices, review, remediation, and integration do
not require Human confirmation. Human
confirmation is required only for material scope or Spec change, permissions,
secrets, spending, deployment, destructive or external actions, an unresolved
`CONTEXT_CONFLICT`, and terminal closure of the canonical Backlog item. The
Feature Delivery Plane prepares the closure candidate; Human Authority
confirms terminal closure; the Feature Delivery Plane then records `VERIFIED`
and clears active execution. A Human decision issue must not be created for an
automatic step.

Keep command memory use below 8 GB. Use bounded inputs and concurrency, and use
`MAVEN_OPTS='-Xmx2g'` for Maven verification unless a stricter active control
requires a lower limit.

If the current task cannot be reconciled with the five active control documents, report `CONTEXT_CONFLICT` rather than guessing.

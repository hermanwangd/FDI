# FDI Agent Instructions

## Mandatory Read Order

Before doing any project work, read these files in order:

1. `PROJECT-OVERVIEW.md`
2. `FRAMEWORK-SPEC.md`
3. `IMPLEMENTATION-PLAN.md`
4. `STATUS.json`

These four files are the only project-level active control documents.

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

If the current task cannot be reconciled with the four active control documents, report `CONTEXT_CONFLICT` rather than guessing.

# Software Factory Implementation Plan

## Current selection

Human-selected Backlog item: `SF-BL-007`.
Selected execution: `SF-BL-007-CONTINUOUS-SYSTEM-IMPROVEMENT-DESIGN-001`.
Current authority is limited to a process-design Plan. No implementation
envelope, runtime dispatch, code/configuration change, agent reconfiguration,
or automatic remediation is authorized.

Objective: define the smallest reusable Software Factory design that converts
verified delivery findings into evidence-bound improvement recommendations and
routes a later Human-authorized change through the existing Backlog, Plan,
execution-envelope, review, verification, and closure controls. The design must
not create a parallel lifecycle or publish Product truth.

Construction base: `3e163683857fe09913436a3b3881c1b4c3dba5ce`.
Requirements: `CSI-001`, `AUTH-001`–`AUTH-003`, `PK-005`,
`FD-T3-007`, `FD-T4-001`–`FD-T4-003`, `EVID-001`, `EXEC-001`.
FDP owns this Plan, `BACKLOG.md`, and `STATUS.json`; all other actors treat
them as read-only.

## Design inputs

The fixed intake contains exactly four recommendations:

- `CSI-REC-001` (`CODE`): local exact match can return before an ambiguity
  guard. Its code correction remains a future `SF-BL-005` T3 route.
- `CSI-REC-002` (`CODE`): array-wrapped parameterized generic evidence can
  be flattened. Its code correction remains a future `SF-BL-005` T3 route.
- `CSI-REC-003` (`DELIVERY`): a review handoff lacked durable command logs,
  digest manifest, and receiver readback.
- `CSI-REC-004` (`DELIVERY`): competing FDP flows wrote contradictory
  singleton current-selection controls.

Each recommendation remains `RECOMMENDED_NOT_SELECTED` for implementation.
Current effectiveness is `INSUFFICIENT_SAMPLE`; missing recurrence, token,
cycle-time, and review-escape measurements remain `UNKNOWN`, not zero.

## Required process design

A future design artifact, if separately authorized, must specify:

1. **Finding intake contract.** Required immutable origin, exact candidate or
   control revision, independent verdict/evidence identity, severity, tag,
   affected requirement, insufficiency, proposed prevention or detection
   control, affected KPI, and existing revision route.
2. **Classification.** Allowed tags are `CODE`, `DELIVERY`, `PK`,
   `MIXED`, and `UNKNOWN`. Unclear findings remain `UNKNOWN`; classification
   never grants mutation authority.
3. **Deduplication.** Stable identity derives from origin evidence, affected
   requirement, insufficiency, and proposed control. Repeated observations
   update evidence and KPI samples rather than creating competing lifecycle
   records.
4. **Routing.** Code defects route to their owning Product Backlog item.
   Reusable delivery-process changes route to `SF-BL-007`. Product meaning,
   architecture, permissions, deployment, publication, and terminal closure
   retain their existing Human gates.
5. **Selection gate.** A recommendation cannot create or modify an
   Implementation Plan, execution envelope, active lane, code, configuration,
   agent, or automation until Human Authority selects the owning Backlog item.
6. **Serialized FDP control.** Exactly one FDP reconciliation owner may update
   the five active controls for a selection. Other lanes park or remain
   evidence-only until that write completes.
7. **Evidence gate.** A delivery handoff is incomplete without retained command
   results, exact candidate binding, digest manifest, receiver readback,
   limitations, attempts, elapsed/wait time, and token/cache values or explicit
   `N/A`.
8. **Effectiveness review.** Compare like category and size across sufficient
   samples. Measure recurrence, first review outcome, review escape, rework,
   elapsed time, unplanned Human intervention, and token/tool cost. Do not infer
   improvement from one case or missing telemetry.

## Design acceptance

- Uses existing `BACKLOG.md`, `IMPLEMENTATION-PLAN.md`, execution envelope,
  independent review, verification, and Human closure gates.
- Defines deterministic intake, classification, deduplication, routing,
  selection, evidence, and KPI contracts.
- Keeps recommendations distinct from accepted Product truth and executable
  authority.
- Routes `CSI-REC-001/002` back to future `SF-BL-005` remediation and uses
  `CSI-REC-003/004` as reusable `SF-BL-007` process-design inputs.
- Preserves honest `UNKNOWN`, `N/A`, and `INSUFFICIENT_SAMPLE` states.
- Includes negative cases for duplicate findings, stale candidate evidence,
  missing receiver readback, competing FDP writers, unclear ownership, and
  recommendations that imply unauthorized scope expansion.
- Receives fresh independent review before any implementation proposal.

## Explicit exclusions

This selection does not authorize creation or execution of an implementation
envelope; source, test, build, dependency, agent, Skill, automation, permission,
or infrastructure changes; remediation of the two code findings; route coverage
analysis dispatch; calibration, scoring, formal holdout, Product truth,
publication, deployment, cleanup, or parent closure.

Any future design-artifact production, implementation, or rollout requires a
separate Human selection and exact envelope. Aggregate commands remain below
8 GB memory and avoid heavy JVM work.

## Preserved execution ledger

These existing lanes remain unchanged:

- `SF-BL-005-SELECTOR-DIAGNOSTICS-001`
- `SF-BL-005-PARALLEL-INVESTIGATIONS-001`
- `SF-BL-006-COMPANY-AI-SHARE-001`
- `SF-BL-005-GENERIC-ANCESTOR-001`
- `SF-BL-005-REALWORLD-DIAGNOSTIC-PREP-001`
- `SF-BL-005-SELECTOR-RUNNER-001`
- `SF-BL-005-RUNNER-ANCESTOR-INTEGRATION-001`
- `SF-BL-005-REALWORLD-SELECTOR-DIAGNOSTIC-001`
- `SF-BL-005-ROUTE-COVERAGE-ANALYSIS-001`
- `SF-BL-005-GENERIC-ANCESTOR-FOLLOWUP-INTEGRATION-001`

The already-running independent preflight for route coverage may finish as
read-only evidence. It does not authorize route analysis dispatch and does not
change the current `SF-BL-007` selection.

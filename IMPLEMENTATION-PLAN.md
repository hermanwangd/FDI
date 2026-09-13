# Software Factory Implementation Plan

## Current selection

Active Backlog items are `SF-BL-005` and `SF-BL-006`; current focus is
`SF-BL-005`. Completion of `SF-BL-007` does not select or dispatch another
execution.

`SF-BL-005-ROUTE-COVERAGE-ANALYSIS-001` has an exact read-only envelope and
separately attributable producer and independent preflight `PASS`. Its unit is
9 distinct observations accounting for 90 repeated `ROUTE_ABSENT` pairs.
Analysis dispatch, source/test changes, calibration, scoring, formal holdout,
Product truth, and parent closure remain unauthorized.

- Envelope control commit: `2f3181fddad7f5ee4cb160d781efd278fb2ed2b4`
- Envelope SHA-256:
  `73eb5c1a522151d5921946d391fe2d1ef60dd795f1f83ca04addaf903d3d814c`
- Producer evidence:
  `validation/software-factory/sf-bl005/envelope-preflight-route-coverage-analysis-001.json`
- Independent reviewer run: `01a09a4c-e3c4-7698-af2e-14ba05d64362`
- State: `INDEPENDENT_PREFLIGHT_PASS_NOT_DISPATCHED`

The generic-ancestor follow-up candidate
`8e835b427bd5f6b242b38d00e714d902298366c1` remains
`PARKED_AFTER_STAGE_2_FAIL`; it was not integrated and Stage 3 was not
triggered. `SF-BL-006` remains `BLOCKED_USER_APPROVAL` after learner archive
delivery. Neither lane receives new authority from this Plan update.

### Active execution ledger

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

## Continuation constraints

- One FDP reconciliation owner serializes all five active-control changes.
- Existing execution evidence stays immutable; no failed or blocked state is
  rewritten as success.
- Missing metrics remain `UNKNOWN`; incomparable samples remain
  `INSUFFICIENT_SAMPLE`.
- Route analysis requires a new Human dispatch decision despite both preflight
  results passing.
- `CSI-REC-001` through `CSI-REC-004` remain recommendations. Their
  canonical records do not authorize remediation.
- Aggregate commands stay below 8 GB and heavy JVM work runs sequentially with
  a 2 GB Maven heap unless a stricter envelope applies.

## Verified-delivery ledger

### SF-BL-007 continuous system improvement

Human Authority confirmed terminal closure of
`SF-BL-007-CONTINUOUS-SYSTEM-IMPROVEMENT-IMPLEMENTATION-001`.

- Delivered behavior: Java 17 recommendation validator and packaged
  `csi-validate` CLI; deterministic semantic duplicate keys; append-only
  evidence/KPI update validation; non-circular handoff gates; immutable
  provenance; honest KPI states; four canonical intake records.
- Reviewed candidate: `775b7728934df6ec00f6cf097e0fc47f75a5f726`
- Verification: Java 17 Maven package `1491/1491`, Python `63/63`, four
  deterministic CLI reports, two-axis independent review `PASS`.
- Evidence:
  `validation/software-factory/sf-bl007/implementation-001-evidence.json`
- Limitations: `CSI-REC-003/004` remain `BLOCKED` because legacy durable
  provenance is missing; effectiveness remains `INSUFFICIENT_SAMPLE`.
- Exclusions: no recommendation remediation, route-analysis dispatch,
  deployment, agent/automation change, automatic Product truth publication,
  calibration, formal holdout, or closure of another Backlog item.

### SF-BL-004 project change reference exporter

Human Authority closed the deterministic Java 17 reference exporter at
candidate `c1643d9a516db5a0167c4321eff92e20ce2a4660`. Evidence:
`validation/software-factory/sf-bl004/change-reference-001-evidence.json`.
The result is `ENGINEERING_READY`, not deployed or published.

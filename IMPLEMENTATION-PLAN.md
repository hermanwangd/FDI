# Software Factory Implementation Plan

## Current selection

Active Backlog items are `SF-BL-005` and `SF-BL-006`; current focus is
`SF-BL-005`. Human Authority selected envelope creation and ledger reconciliation
for `SF-BL-005-ROUTE-HANDLER-EXTRACTOR-REMEDIATION-001`. This selection does not dispatch implementation.

The bounded objective is to extend `SpringRouteHandlerIndex` static extraction
for method-level `@PutMapping` and `@DeleteMapping`, including annotations with
no method path and with one literal method path. It must preserve existing
class-level composition, `@RequestMapping`, GET/POST behavior, route
normalization, ambiguity handling, exact-revision provenance, and proposal-only
semantics.

The execution envelope must bind the exact control/base revision, the reviewed
route-coverage analysis, the two Java change surfaces, focused negative and
regression tests, an isolated evidence namespace, Java 17, independent review,
full regression, and a memory limit below 8 GB. It must exclude selector,
scorer, evaluator/gold, frozen evidence, calibration, RealWorld scoring, formal
holdout, Product truth, deployment, and parent closure.

### Current execution ledger

- `SF-BL-005-ROUTE-HANDLER-EXTRACTOR-REMEDIATION-001` — `ENVELOPE_PREPARATION_SELECTED_NOT_DISPATCHED`

### Completed execution ledger

- `SF-BL-005-SELECTOR-DIAGNOSTICS-001` — `INTEGRATED_ENGINEERING_READY`
- `SF-BL-005-PARALLEL-INVESTIGATIONS-001` — `OBSERVATIONS_RECEIVED_WITH_LIMITATIONS`
- `SF-BL-005-GENERIC-ANCESTOR-001` — `FRESH_INDEPENDENT_REVIEW_PASS`
- `SF-BL-005-REALWORLD-DIAGNOSTIC-PREP-001` — superseded by completed diagnostic execution
- `SF-BL-005-SELECTOR-RUNNER-001` — `INDEPENDENT_REVIEW_COMPLETE_WITH_LIMITATIONS`
- `SF-BL-005-RUNNER-ANCESTOR-INTEGRATION-001` — `INTEGRATED_ENGINEERING_READY`
- `SF-BL-005-REALWORLD-SELECTOR-DIAGNOSTIC-001` — `DIAGNOSTIC_COMPLETE_REVIEW_PASS_FDP_RECONCILED`
- `SF-BL-005-ROUTE-COVERAGE-ANALYSIS-001` — `ANALYSIS_COMPLETE_FDP_RETURNED`

### Parked or blocked execution ledger

- `SF-BL-005-GENERIC-ANCESTOR-FOLLOWUP-INTEGRATION-001` — `PARKED_AFTER_STAGE_2_FAIL`; not integrated and Stage 3 not triggered
- `SF-BL-006-COMPANY-AI-SHARE-001` — learner delivery complete; downstream company environment remains `BLOCKED_USER_APPROVAL`

## Continuation constraints

- One FDP reconciliation owner serializes all five active-control changes.
- Existing execution evidence stays immutable; no failed or blocked state is
  rewritten as success.
- Envelope creation is authorized; implementation dispatch requires a separate
  Human decision after producer and independent preflight pass.
- Missing metrics remain `UNKNOWN`; incomparable samples remain
  `INSUFFICIENT_SAMPLE`.
- `CSI-REC-001` through `CSI-REC-004` remain recommendations and are outside
  this route-extractor execution.
- Aggregate commands stay below 8 GB and heavy JVM work runs sequentially with
  a 2 GB Maven heap unless the envelope is stricter.

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

### RESULT

Current candidate `SPC-MISSION-V1-r1` is **FAIL / REFUTED** for S06: FV-003 is reproducible, and F1 remains unresolved. The supplied `npm test` suite is green but does not cover the blocking chart-validation behavior.

Evidence-backed findings:

| Requirement / risk | Required verification depth | Result |
|---|---|---|
| R-001 valid chart configuration; risk P2 x I3 = 6 (HIGH) | Component regression with valid finite limits | Direct probe rendered a valid configuration; no committed regression test exists. PARTIAL |
| R-002 viewer retrieves through Chart Management API; risk P2 x I3 = 6 (HIGH) | API contract plus viewer/API integration on the exact candidate | No runtime integration evidence is available from the r1 fixture. UNVERIFIED |
| R-003 HTTP 404 remains `{status: 404, retryable: false}`; risk P1 x I3 = 3 (MEDIUM) | Unit/contract test bound to the candidate revision | Existing test passed. PASS |
| R-004 / FV-003 missing or invalid `limits.ucl`/`limits.lcl`; observed P3 x I3 = 9 (CRITICAL) | Component negative partitions plus valid-chart regression | `renderChart` accepted `ucl: "bad"` and `lcl: NaN` and returned `{rendered: true}`. FAIL |
| Frozen chart-limit behavior `chartLimits().max = 10`; observed P3 x I3 = 9 (CRITICAL) | Exact-value regression | Readback was `1000`. FAIL |

Minimum regression set for a governed r2 retest:

- selected chart opens in one interaction;
- empty list and unknown chart id do not open a chart;
- 404 remains non-retryable;
- missing `limits`, missing `ucl`/`lcl`, non-finite values, and wrong-type values all reject with `INVALID_CHART_CONFIGURATION`;
- valid finite limits still render;
- `chartLimits().max` equals exactly `10`;
- viewer-to-Chart-Management API retrieval and the published 404 contract are exercised from a fresh checkout of the exact r2 revision.

The state-transition technique is not applicable: the frozen inputs define no lifecycle state machine. No additional numeric limit range is invented because PC1 explicitly excludes changing chart-limit semantics; only the exact frozen max and finite/invalid partitions are tested.

### EVIDENCE USED

- Read `.../supporting-v1/_dispatch-descriptions/COMMON-CELL-PROTOCOL.md`.
- The requested `.../supporting-v1/TEST-ARCHITECTURE.md` read returned `No such file or directory`; this absence is retained as a limitation.
- Used only `.../supporting-v1/_controlled-inputs/s06-test-architecture/` inputs: `S06.json`, `PC1-v2.yaml`, `chart-viewer-enhancement-request.md`, `case-b-ambiguous.md`, `source-corpus/chart-management-spec.md`, `source-corpus/product-rules.yaml`, `source-corpus/product-training.md`, `source-corpus/repository-manifest.json`, all three `source-corpus/verification/*.md` files, relevant chart-viewer/chart-management-api/spc-deployment source files, and `r1-fixture-source/**`.
- Frozen revisions read back: scenario `S06-r2-spc-mission-v1`; PC1 `contextRevision: 2`, `status: FROZEN_FOR_S01_S06_PC1`; fixture/repositories `SPC-MISSION-V1-r1`.
- In a disposable copy of `r1-fixture-source`, `npm test` reported **2 passed, 0 failed**.
- In a separate disposable copy, `node --test --experimental-test-coverage` reported **96.30% line, 77.78% branch, 100% function** coverage for loaded files only (`interaction.js` and the test). `chartViewer.js` was not loaded by the test suite and is therefore not represented by that percentage.
- Direct readback from the same candidate: `chartLimits().max = 1000`; missing `limits` throws `INVALID_CHART_CONFIGURATION`; invalid string `ucl` and `NaN` `lcl` are rendered successfully; valid finite limits render; 404 maps to `{status: 404, retryable: false}`.

### LIMITATIONS

- No r2 candidate, governed F1 correction, or fresh r2 checkout/evidence is present in the allow-listed package; r2 PASS/VERIFIED and F1 resolution cannot be claimed.
- No runtime Chart Management API, deployed stack, or end-to-end viewer/API evidence is available, so R-002 and deployment-level NFR evidence remain unverified.
- The coverage number is loaded-file coverage, not whole-product coverage; the critical `chartViewer.js` path is currently outside the automated suite.
- `case-b-ambiguous.md` was preserved as a separate S04 input; no PM decision was inferred from it.

### CONTROLLED CELL META

- Profile: `test-architecture/E1`
- Arm: `targetSkillAttachment=ENABLED`
- Canonical RC: `test-architecture` only
- Scenario revision: `S06-r2-spc-mission-v1`
- Input package: `_controlled-inputs/s06-test-architecture/`
- Delivery type: bounded reasoning/design result; no repository or product files altered.

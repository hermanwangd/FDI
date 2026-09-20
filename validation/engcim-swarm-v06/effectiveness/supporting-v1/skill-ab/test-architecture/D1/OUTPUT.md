RESULT

Bounded S06 verification design delivered for the exact r1 candidate. The current r1 candidate is REFUTED for the required chart-limit contract and R-004 malformed-limit validation; the two existing smoke tests pass but do not cover those paths. No repository files were modified.

### 測試計畫／交付摘要

Target: SPC Demo Chart Viewing, scenario `S06-r2-spc-mission-v1`, exact checkout at commit `f63aa7ff1037d840c0b9338e455def256956bed2`.

Scope is Chart Viewer selection, Chart Management retrieval contract, 404 handling, valid chart configuration, malformed `limits.ucl`/`limits.lcl`, and frozen chart-limit behavior. Excluded: chart creation, deployment administration, notifications, unrelated flows, r2 correction code, other cells, gold/evaluator material, and future candidates.

| Case | Traceability and boundary | Expected oracle | Current r1 status |
|---|---|---|---|
| CTRL-01 | Candidate provenance; exact commit and clean checkout | Test runner must bind to the pinned commit and record the revision | PASS: `git rev-parse HEAD` matched; worktree remained clean |
| SEL-01 | R-001/request; one matching chart in a singleton list | One interaction returns `{action: 'OPENED', chartId}` for the selected id | PASS: existing smoke test |
| SEL-02 | Selection partitions: empty list, one match, many charts, missing id | Match opens only the requested chart; empty/missing selection produces no open action | Partial: matching case exists; empty/multi/missing are required regression cases; missing-id read-only probe returned `null` |
| VALID-01 | R-001/R-004; finite ordered `ucl` and `lcl` present | Configuration renders as valid and preserves the supplied limits | PASS for the supplied valid probe |
| API-01 | R-002; selected id to `GET /api/charts/{id}` | Exactly one Chart Management request for the selected id; returned definition is the one rendered | NOT EXECUTABLE in this checkout: no Chart Management implementation is present; retain as an independent contract/integration case |
| ERR-01 | R-004 malformed structure: null/absent config or absent `limits` | Explicit invalid result/rejection; never a valid render | PASS for absent `limits`: current code throws `INVALID_CHART_CONFIGURATION` |
| ERR-02 | R-004 required missing-field boundaries: missing `ucl`, missing `lcl`, both missing | Invalid/rejected; never `{rendered: true}` | FAIL: current r1 renders each missing-one case as valid |
| ERR-03 | R-004 invalid-value partition: null, wrong type, non-finite, and reversed bounds | Invalid/rejected; exact numeric-domain rules must be confirmed before release gating | NOT ORACLED: the supplied product documents define missing/invalid but not the numeric domain; do not invent a range |
| LIM-01 | Frozen chart-limit behavior / FV-003 | `chartLimits()` is exactly `{min: 0, max: 10}` | FAIL: read-only probe returned `{min: 0, max: 1000}` |
| LIM-02 | Request says do not change chart-limit semantics | Valid render preserves the supplied limit values; no new range semantics are introduced | PASS for the valid probe; boundary magnitudes are not specified by the inputs |
| HTTP-404-01 | R-003 | 404 remains user-visible and `retryable: false` | PASS: existing smoke test and probe |
| REG-01 | Governed F1/r2 gate | Re-run all P0 cases from a fresh r2 checkout; fresh r2 evidence replaces r1 for the current verdict | REQUIRED after governed correction; not run in this r1-only cell |

Priority is P0 for CTRL-01, SEL-01, VALID-01, ERR-01/02, LIM-01, and HTTP-404-01; P1 for API-01, ERR-03, and the remaining selection partitions. The candidate exposes synchronous functions without a stateful lifecycle, so concurrency/state-transition testing is not applicable; the relevant boundaries are input partitions and the one-interaction selection scenario.

### 案例對照表

R-001 is covered by VALID-01 plus the selection partitions. R-002 requires API-01 and independent integration evidence. R-003 is covered by HTTP-404-01. R-004 is covered by ERR-01/02 and, once the numeric contract is published, ERR-03. The PM request is covered by SEL-01/02, HTTP-404-01, and LIM-02.

### 失敗測試（如有）

These are read-only r1 probes, not newly committed tests:

1. From the pinned checkout, call `chartLimits()` and compare with the frozen contract. Expected `{min: 0, max: 10}`; actual `{min: 0, max: 1000}`. This is the seeded FV-003 chart-limit mismatch.
2. Call `renderChart({limits: {lcl: 0}})` and `renderChart({limits: {ucl: 10}})`. Expected an invalid/rejected result under R-004; actual results were `{rendered: true, limits: {lcl: 0}}` and `{rendered: true, limits: {ucl: 10}}`.

The candidate's existing test suite does not contain assertions for either failure, so a green smoke run must not be treated as a release pass.

### 覆蓋率與未覆蓋區域

Executed from the exact checkout:

- `npm test`: 2 passed, 0 failed.
- `node --test --experimental-test-coverage`: 2 passed; instrumented `interaction.js` coverage was 90.91% line / 66.67% branch, and the instrumented-file aggregate was 96.30% line / 77.78% branch.

Coverage is incomplete: `chartViewer.js` was not imported by the existing suite, so its acceptance coverage is unverified; API-01 lacks an implementation in this checkout; missing-limit assertions, invalid-value classes, chart-limit exactness, and empty/multi selection regression assertions are absent. Target for the governed regression set is 100% of P0 acceptance paths and at least 90% line and branch coverage for affected modules. No numeric min-1/max+1 cases are proposed because no such domain is published and the request explicitly preserves chart-limit semantics.

### 給 Coder 的缺陷回報

- FV-003, P0: `src/chartViewer.js` returns `chartLimits().max = 1000`, contradicting the frozen product behavior of `10`. Correct only under the governed F1 scope, then verify from a fresh r2 checkout.
- R-004 violation, P0: `renderChart` validates only the presence of `config.limits`; a missing `ucl` or `lcl` is rendered as valid. Add regression assertions for both one-field omissions and retain the valid-config case.

Do not close F1 or promote r1 based on the existing 2/2 smoke result. No implementation fix was made in this QA cell.

EVIDENCE USED

- `COMMON-CELL-PROTOCOL.md`, `TEST-ARCHITECTURE.md`, `S06.json`, `PC1-v2.yaml`, `chart-management-spec.md`, `product-rules.yaml`, `product-training.md`, `chart-viewer-enhancement-request.md`, and the exact candidate README/source/tests.
- Exact candidate identity: `f63aa7ff1037d840c0b9338e455def256956bed2`; no other cell, gold, hidden answer, fixture source, or future r2 was read.
- Fresh-process npm test, coverage run, and read-only probes described above.

LIMITATIONS

The exact r1 checkout contains Chart Viewer and interaction code but no Chart Management implementation, so R-002 is a design-level independent contract case rather than an executed integration result. The source inputs do not define the numeric meaning of “invalid” beyond missing `ucl`/`lcl`; null/type/non-finite/reversed-value cases require a published oracle before becoming release gates. This result is only an r1 assessment and does not claim r2 correction or verification.

CONTROLLED CELL META

- Cell: `test-architecture/D1` replacement after the preserved `FIXTURE_ENVIRONMENT` input-drift run
- Arm: `targetSkillAttachment=DISABLED_FOR_AB`
- Scenario revision: `S06-r2-spc-mission-v1`
- Candidate binding: `_controlled-inputs/s06-test-architecture/r1-candidate-exact/` at `f63aa7ff1037d840c0b9338e455def256956bed2`
- Required independence rule: re-run r2 from a fresh pinned checkout; r1 evidence is historical and must not serve as the current r2 verdict

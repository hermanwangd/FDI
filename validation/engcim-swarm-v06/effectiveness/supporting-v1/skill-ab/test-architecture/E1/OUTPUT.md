RESULT

Bounded read-only verification design for `S06-r2-spc-mission-v1` on the exact r1 candidate. Current r1 gate verdict: `FAIL`.

The blocking finding is FV-003/F1: the frozen chart-limit contract requires `chartLimits() === { min: 0, max: 10 }`, but the exact candidate returns `{ min: 0, max: 1000 }`. Existing regressions pass, but they do not exercise this contract, so they cannot establish a PASS. F1 remains unresolved until a governed correction and fresh r2 evidence.

| Requirement / risk | Verification case and depth | Evidence / gate |
|---|---|---|
| Exact candidate and independent evidence (HIGH, 2x3=6) | Confirm clean checkout, exact SHA, fresh process, and an evaluator harness with literal expected values; do not use candidate tests as sole evidence. | PASS: HEAD is `f63aa7ff1037d840c0b9338e455def256956bed2`; working tree clean; independent probe run. |
| Open selected chart in one interaction (MEDIUM, 2x2=4) | Unit/contract: selected id returns `{action:'OPENED', chartId}`; empty/missing selection is a no-op. | PASS in independent probe; existing regression retained. |
| Preserve HTTP 404 behavior (HIGH, 2x3=6) | Unit/contract regression: `classifyChartResponse(404)` remains `{status:404, retryable:false}`. | PASS in existing suite and independent probe. |
| Valid configuration rendering (MEDIUM, 2x2=4) | Unit: `{limits:{min:0,max:10}}` renders and preserves limits. | PASS in independent probe. |
| Malformed configuration / limits (MEDIUM, 2x2=4) | Negative boundaries: `undefined`, `null`, `{}`, and `{limits:null}` must reject as `INVALID_CHART_CONFIGURATION`; additionally probe empty/non-object/partial/reversed/non-finite limit shapes. | First four PASS. The package does not define the complete limit schema or error oracle for the additional shapes; those cases remain `CONCERNS`, not an invented PASS. |
| Frozen chart-limit behavior / FV-003 (CRITICAL, 3x3=9) | Contract assertion with independent literal oracle: `chartLimits()` must equal `{min:0,max:10}`. | FAIL: actual `{min:0,max:1000}`. Blocking. |
| Required regression retention | Run the candidate suite and independently repeat the selected-chart and 404 assertions after any correction. | Current candidate suite: 2 passed, 0 failed. Fresh r2 rerun required after correction. |

Evidence used:

- `COMMON-CELL-PROTOCOL.md` and `TEST-ARCHITECTURE.md`.
- S06 package inputs only: `S06.json`, `PC1-v2.yaml`, `case-b-ambiguous.md`, `chart-viewer-enhancement-request.md`.
- Exact checkout README, package manifest, source, and existing test files at the bound SHA.
- `npm test` in the exact checkout: 2 tests passed, 0 failed.
- Independent read-only Node probe: 8 supported behavior checks passed; chart-limit assertion failed with actual max `1000` versus required max `10`.

Fresh-r2 acceptance gate:

1. Start from a fresh checkout with a new, explicitly recorded revision; verify provenance and a clean tree.
2. Run the independent harness first, then the retained candidate regressions.
3. Require exact chart limits `{min:0,max:10}`, preserved 404 non-retryability, selected-chart behavior, valid rendering, and an owner-defined oracle for all malformed-limit shapes.
4. Record fresh evidence as PASS/VERIFIED only if all blocking requirements pass. Once r2 is current, reject this r1 evidence as stale; do not reuse it.

EVIDENCE USED

The candidate was not modified. No test or fixture files were added to the controlled checkout. The independent probe used a separate process and literal contract values, rather than importing assertions from the candidate test suite.

LIMITATIONS

- No running Chart Management API or deployment exists in this bounded checkout, so API/network/integration and operational evidence are out of scope.
- The supplied package specifies rejection for missing/null configuration but does not specify the full malformed `limits` schema or error contract. An owner decision is required before those cases can become a release PASS.
- No corrected r2 checkout or fresh r2 evidence is present in this cell.
- The seeded chart-limit defect must be corrected through the governed F1 scope; this read-only cell does not alter product code.

CONTROLLED CELL META

- Cell: `test-architecture/E1` replacement controlled cell.
- Arm: `targetSkillAttachment=ENABLED`; canonical RC6 `test-architecture` only.
- Scenario revision: `S06-r2-spc-mission-v1`.
- Candidate: `_controlled-inputs/s06-test-architecture/r1-candidate-exact/` at `f63aa7ff1037d840c0b9338e455def256956bed2`.
- Scope control: only the named S06 test-architecture package was read; no other cells, gold/evaluator answers, hidden correction material, future r2, or frozen files were used.

### RESULT

Read-only S06 test-architecture result for the exact r1 candidate at commit `f63aa7ff1037d840c0b9338e455def256956bed2`. No repository files were modified.

The existing suite is green but incomplete: `npm test` passes 2/2 tests (selected-chart interaction and HTTP 404 mapping). The required verification design is:

| Case | Priority | Coverage and expected assertion | r1 evidence |
|---|---:|---|---|
| VA-01 valid chart | P0 | A valid configuration with valid `limits.ucl` and `limits.lcl` renders successfully and preserves its limits. | Direct probe passed; add as a persistent regression. |
| ML-01 malformed limits | P0 | Missing `limits`, missing `ucl`, missing `lcl`, and representative invalid limit values each throw exactly `INVALID_CHART_CONFIGURATION`; no valid render result is returned. | Missing `limits` is guarded, but missing `ucl`, missing `lcl`, and string `ucl` were accepted: FV-003 is reproduced at r1. |
| CL-01 chart-limit contract | P0 | `chartLimits().max` equals the frozen Product value `10`. Do not assert an unsupported new range. | r1 returns `{min: 0, max: 1000}`: F1 remains unresolved. |
| INT-01 selected chart | P1 | One selected chart-list action returns `{action: 'OPENED', chartId}`. Boundary cases should also cover an empty list and an unknown id returning the documented null/no-open result. | Existing test passes the happy path; boundary cases are not present. |
| API-404-01 | P0 | `classifyChartResponse(404)` remains `{status: 404, retryable: false}`. | Existing test passes. |
| REG-01 post-correction regression | P0 | On a fresh checkout of the corrected current revision, rerun VA-01 plus ML-01 and CL-01; valid charts still render while malformed limits reject and `max` remains 10. | Required for fresh r2 evidence; r1 evidence must not be reused. |

Evidence independence: run the cases as a separate test process from the exact candidate checkout, bind every result to `git rev-parse HEAD`, assert public behavior rather than source text, and record a clean worktree before/after. Do not use `r1-fixture-source/` or any other arm.

Current findings for the coder/verifier: `src/chartViewer.js:6-9` validates only that `config.limits` exists, so malformed limit fields can render; `src/chartViewer.js:2` returns `max: 1000` instead of the frozen `10`. These findings require governed correction and fresh r2 evidence before being considered resolved.

### EVIDENCE USED

Controlled inputs read:

- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_dispatch-descriptions/COMMON-CELL-PROTOCOL.md`
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/TEST-ARCHITECTURE.md`
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-test-architecture/PC1-v2.yaml`
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-test-architecture/S06.json` (`S06-r2-spc-mission-v1`)
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-test-architecture/chart-viewer-enhancement-request.md`
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-test-architecture/case-b-ambiguous.md` (kept separate; not used as the authorized implementation input)
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-test-architecture/source-corpus/chart-management-spec.md`
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-test-architecture/source-corpus/verification/FV-003.md`
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-test-architecture/source-corpus/verification/api-contract.md`
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-test-architecture/source-corpus/verification/valid-chart-regression.md`
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-test-architecture/source-corpus/product-rules.yaml`
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-test-architecture/source-corpus/repos/chart-viewer/README.md`
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-test-architecture/source-corpus/repos/chart-viewer/src/chartViewer.js`
- Exact candidate files under `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-test-architecture/r1-candidate-exact/` (`README.md`, `package.json`, `src/chartViewer.js`, `src/interaction.js`, `test/interaction.test.js`).

Commands and readbacks:

- `git -C <exact candidate> rev-parse HEAD` → `f63aa7ff1037d840c0b9338e455def256956bed2`.
- `git -C <exact candidate> status --short` → clean/empty; `git diff --exit-code -- .` → clean.
- `npm test` → 2 passed, 0 failed.
- `node --test --experimental-test-coverage` under Node `v22.22.0` → 2 passed; reported 96.30% line and 77.78% branch coverage only for loaded `interaction.js`/tests. `chartViewer.js` was not imported by the suite, so its coverage is not measured.
- Read-only valid-render probe → exit 0, rendered `{ucl: 10, lcl: 0}`.
- Read-only malformed-limits probe → exit 1; missing `ucl`, missing `lcl`, and string `ucl` were accepted.
- Read-only chart-limit probe → exit 1; read `{min: 0, max: 1000}`.

### LIMITATIONS

- No persistent test file was added because this controlled cell explicitly forbids file changes; the cases above are the handoff design for the coder/verifier.
- The available evidence defines missing/invalid limits and the exact `max` value, but does not define additional numeric range/order rules; the design does not invent them.
- No fresh corrected r2 checkout or retest evidence exists in this r1-only cell. Current coverage numbers must not be treated as full product coverage because `chartViewer.js` is absent from the existing suite.
- The ambiguous Case B remains a separate input and does not authorize changing 404 behavior or chart-limit semantics.

### CONTROLLED CELL META

- Profile: `test-architecture`
- Arm: `DISABLED_FOR_AB` (`targetSkillAttachment=DISABLED_FOR_AB`)
- Scenario revision: `S06-r2-spc-mission-v1`
- Input package: `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-test-architecture/`
- Candidate: `r1-candidate-exact/` at `f63aa7ff1037d840c0b9338e455def256956bed2`

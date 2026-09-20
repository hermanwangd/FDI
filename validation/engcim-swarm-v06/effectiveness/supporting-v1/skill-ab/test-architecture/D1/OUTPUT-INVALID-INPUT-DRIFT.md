### RESULT

Verification design for `S06-VERIFICATION-TESTING`, scenario revision `S06-r2-spc-mission-v1`. No product, fixture, repository, or canonical candidate files were changed. This is a test architecture/design result; it does not assign the final r1/r2 verdict.

The execution must start from a fresh checkout of the required historical r1 candidate `f63aa7ff1037d840c0b9338e455def256956bed2`, record `git rev-parse HEAD`, and bind every result/log to that revision. A separate fresh checkout is required for any corrected r2 retest. Producer-workspace tests, prior comments, and r1 evidence must not be reused as r2 evidence.

| Case | Coverage and procedure | Pass oracle / evidence |
|---|---|---|
| REV-01 | Fresh-checkout and independence gate; verify relevant repositories from `repository-manifest.json`, exclude the distractor service, record clean status and exact commit before execution. | Candidate identity is exact and reproducible; each result includes command, revision, fixture revision, and raw output. |
| API-01 | Call Chart Viewer with a chart id against Chart Management; observe the request boundary. | Exactly `GET /api/charts/{id}` is used and the returned definition is the one passed to rendering; no hard-coded chart definition. |
| CFG-01 | Valid configuration: series plus finite numeric `limits.ucl` and `limits.lcl`; use a draw spy. | Render succeeds and `canvas.draw(series, limits)` is called once with the retrieved values. |
| CFG-02 | Equivalence class for absent configuration: `null`, `{}`, and `limits: null`. | `INVALID_CHART_CONFIGURATION`; draw/render is not called. |
| CFG-03 | FV-003 missing-field partitions: missing `limits.ucl` and missing `limits.lcl`, one at a time. | Each is rejected with `INVALID_CHART_CONFIGURATION`; no valid render/draw occurs. |
| CFG-04 | Malformed-value partitions: string, `null`, `NaN`, `Infinity`, and `-Infinity` for either limit (direct JS values for non-JSON numbers). | Each is rejected and cannot reach rendering. |
| CFG-05 | Required valid-chart regression after correction, from the fresh r2 checkout; repeat CFG-01. | Valid limits still render successfully; FV-003 correction does not break the happy path. |
| LIM-01 | Assert the frozen chart-limit contract directly. | `chartLimits().max === 10`; do not invent additional min/max semantics not present in Product evidence. |
| HTTP-01 | Request a missing chart and inspect response classification, with a retry counter. | Exact result `{status: 404, retryable: false}` and zero retry attempts, before and after correction. |
| INT-01 | Selected chart from list; exercise one selected id, unknown id, and empty list. | Selected id opens in one interaction; unknown/empty selection returns the documented null result. |
| RET-01 | r1 reproduction → governed finding/correction → fresh r2 execution. | r1 evidence is bound only to r1; it remains invalid for r2 after revision changes; r2 evidence is newly generated and includes CFG-03/04, CFG-05, LIM-01, HTTP-01, API-01, and INT-01. |

Traceability: R-001 is covered by API-01/CFG-01/CFG-02; R-002 by API-01; R-003 by HTTP-01; R-004 and FV-003 by CFG-02 through CFG-05. The selected-chart one-action requirement is INT-01. S06 evidence-integrity and finding-resolution requirements are REV-01 and RET-01.

Regression tiers:

- Smoke: CFG-01, HTTP-01, and INT-01.
- Change regression: CFG-02 through CFG-05 plus LIM-01 and API-01.
- Full verification: all cases, clean revision binding, relevant deployment/repository manifest check, and fresh r2 rerun.

Boundary/state notes: the defined numeric chart-limit boundary is `chartLimits().max = 10`; no Product evidence specifies an independent allowed range for `ucl`/`lcl`, so the plan tests finite-vs-invalid values without inventing a range. The state transition is r1 reproduction → finding pending → governed correction → fresh r2 verification. Treating r1 evidence as r2 evidence is an invalid transition and must be rejected.

Observed controlled-fixture gap (reproducible, not a final verifier verdict):

- Environment: Node `v22.22.0`; controlled `r1-fixture-source`.
- `node --test test/interaction.test.js`: 2 passed, 0 failed. These tests cover only selected interaction and 404 classification.
- `node --input-type=module -e 'import { chartLimits, renderChart } from "./src/chartViewer.js"; ...'` observed `chartLimits = {min: 0, max: 1000}`. The same read-only probe observed `missing-ucl`, `missing-lcl`, and string `ucl` returning `{rendered:true,...}` instead of throwing `INVALID_CHART_CONFIGURATION`.
- Reproduction inputs: `renderChart({limits:{lcl:1}})`, `renderChart({limits:{ucl:3}})`, and `renderChart({limits:{ucl:"3",lcl:1}})`.
- Expected oracle from the frozen specification: missing or invalid `ucl`/`lcl` must be rejected and must not render; frozen chart-limit behavior requires max `10`.

This is the required regression handoff to the Coder/verifier: add executable cases for CFG-03/CFG-04 and LIM-01, then rerun the existing valid and 404 tests after the governed correction. No product code was changed in this cell.

### EVIDENCE USED

Exact protocol/profile inputs read:

- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_dispatch-descriptions/COMMON-CELL-PROTOCOL.md`
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_dispatch-descriptions/TEST-ARCHITECTURE.md`
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-test-architecture/PC1-v2.yaml` (`contextRevision: 2`, `fixtureRevision: SPC-MISSION-V1-r1`)
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-test-architecture/S06.json` (`scenarioRevision: S06-r2-spc-mission-v1`, definition digest `sha256:ae3f75cbb80f5808502dbd8c7e17efc621b1b46d254279bbb9ac4b641176d28c`)
- `chart-viewer-enhancement-request.md`, `product-training.md`, `chart-management-spec.md`, `product-rules.yaml`, and `repository-manifest.json` under the controlled `s06-test-architecture` input.
- `verification/FV-003.md`, `verification/api-contract.md`, and `verification/valid-chart-regression.md` under the controlled `source-corpus` input.
- Relevant frozen source: `source-corpus/repos/chart-viewer/src/chartViewer.js`, `source-corpus/repos/chart-management-api/src/chartApi.js`, and `source-corpus/repos/spc-deployment/deployment.yaml`; distractor source was read only to confirm it is outside the chart boundary.
- r1 fixture source: `r1-fixture-source/README.md`, `package.json`, `src/chartViewer.js`, `src/interaction.js`, and `test/interaction.test.js`.

Commands/readbacks actually used:

- `multica issue get 01a0beef-cac8-7b6b-92fb-2e4decaf27fb --output json`
- `multica issue comment list 01a0beef-cac8-7b6b-92fb-2e4decaf27fb --roots-only --summary --compact --output json` → `[]`
- `rg --files` on the exact controlled input directory, followed by read-only `cat` of the listed files.
- `node --test test/interaction.test.js` → 2 pass, 0 fail.
- Read-only Node probes for `chartLimits()` and malformed/valid `renderChart` inputs.
- `node --version` → `v22.22.0`.
- `git rev-parse HEAD` in the controlled fixture → `a07a023dd2e62db0338422b68daf1702adba1ee3`; `git status --short --untracked-files=no` was clean.

### LIMITATIONS

- The requested root-level `supporting-v1/TEST-ARCHITECTURE.md` path does not exist. The available profile file was found and read at `_dispatch-descriptions/TEST-ARCHITECTURE.md`; this exact path discrepancy is recorded rather than silently resolved.
- The controlled fixture checkout reports `a07a023dd2e62db0338422b68daf1702adba1ee3`, not the profile-required historical candidate `f63aa7ff1037d840c0b9338e455def256956bed2`. Therefore exact r1 candidate binding is not locally verified; a fresh checkout at the required commit is a prerequisite for the formal run.
- No corrected r2 revision is present in the allow-listed inputs, so no fresh r2 result, resolution of F1, or final PASS/VERIFIED claim is made.
- No live HTTP service, browser UI, or deployment runtime was available in the controlled inputs; API and end-to-end cases are designed but not runtime-executed here.
- No executable tests were added and no line/branch coverage tool was run. Current executable coverage is limited to the existing 2-test interaction suite; CFG-03/CFG-04 and LIM-01 are uncovered by that suite and carry high risk because they protect configuration validity and chart-limit semantics.
- Out of scope per the frozen corpus: chart creation, deployment administration, notifications, and unrelated user flows.

### CONTROLLED CELL META

- Profile: `test-architecture`
- Cell: `test-architecture/D1`
- Arm: `disabled`; `targetSkillAttachment=DISABLED_FOR_AB`
- Scenario: `S06-VERIFICATION-TESTING`
- Scenario revision: `S06-r2-spc-mission-v1`
- Required historical r1 candidate: `f63aa7ff1037d840c0b9338e455def256956bed2`
- Controlled fixture revision: `SPC-MISSION-V1-r1`
- Input cat: `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/TEST-ARCHITECTURE.md: No such file or directory`
- Actual profile input read: `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_dispatch-descriptions/TEST-ARCHITECTURE.md`

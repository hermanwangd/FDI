### RESULT

CorrectionReasoningResult for the exact r1 candidate at commit `f63aa7ff1037d840c0b9338e455def256956bed2`:

- Root cause is localized to `r1-candidate-exact/src/chartViewer.js`. `renderChart` only checks that `config.limits` exists; it does not validate both `limits.ucl` and `limits.lcl` as finite values. Therefore malformed limits are returned as `{ rendered: true, ... }` instead of raising `INVALID_CHART_CONFIGURATION`. This reproduces in isolation, so an upstream Chart Management failure is not required.
- The same file contains an independent frozen-contract mismatch: `chartLimits()` returns `max: 1000`, while the frozen product behavior requires `max: 10`.
- Required governed correction: expand `renderChart` to reject missing or non-finite `ucl`/`lcl` with the exact `INVALID_CHART_CONFIGURATION` error before rendering, using the finite-value contract represented by `source-corpus/repos/chart-management-api/src/chartApi.js`; change only the chart limit maximum to `10`; leave `interaction.js`, selected-chart behavior, 404 mapping, and unrelated chart-limit semantics unchanged.
- Regression obligations: (1) all four staged FV-003 malformed-limit cases throw the exact error and never render as valid; (2) a valid configuration with finite `ucl` and `lcl` still renders; (3) `chartLimits()` is `{min: 0, max: 10}`; (4) selected-chart and `{status: 404, retryable: false}` behavior remain green; and (5) verification runs from a fresh, distinct r2 checkout. The r1 evidence must not be reused as r2 evidence, and F1 remains unresolved until fresh r2 verification passes.

No correction was implemented or claimed as verified in this cell.

### EVIDENCE USED

- Protocol and dispatch: `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_dispatch-descriptions/COMMON-CELL-PROTOCOL.md`, `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/ROOT-CAUSE-DEBUGGING.md`, and `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_dispatch-descriptions/root-cause-debugging-D1.md`.
- Controlled package: `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-root-cause-debugging/S06.json` (`scenarioRevision: S06-r2-spc-mission-v1`, definition digest `sha256:ae3f75cbb80f5808502dbd8c7e17efc621b1b46d254279bbb9ac4b641176d28c`), `PC1-v2.yaml` (`contextRevision: 2`), the exact r1 candidate README/source/tests, and `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-root-cause-debugging/f1-evidence/S06-r1-defect-report-F1.md`.
- Frozen product/reference inputs: `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-root-cause-debugging/source-corpus/chart-management-spec.md`, `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-root-cause-debugging/source-corpus/product-rules.yaml`, `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-root-cause-debugging/source-corpus/verification/FV-003.md`, `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-root-cause-debugging/source-corpus/verification/api-contract.md`, `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-root-cause-debugging/source-corpus/verification/valid-chart-regression.md`, `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-root-cause-debugging/source-corpus/repos/chart-management-api/src/chartApi.js`, and `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-root-cause-debugging/source-corpus/repos/chart-viewer/src/chartViewer.js`.
- Readbacks performed: `rg --files` over the allow-listed package; `sed -n` on the protocol, dispatch, scenario, context, defect evidence, candidate, and source-corpus files; `rg -n -i 'chartLimits|max.?10|max.?1000|ucl|lcl|FV-003|S06-r1|f63aa7ff'` over the allow-listed package; and `git ... status --short` on the candidate (no output).
- `npm test` in `r1-candidate-exact`: 2 passed (selected-chart interaction and 404 mapping). Read-only Node reproduction: missing `ucl`, missing `lcl`, and non-finite/string `ucl` each rendered successfully; valid limits rendered; `chartLimits()` returned `{min:0,max:1000}`; 404 returned `{status:404,retryable:false}`.

### LIMITATIONS

- This was a bounded correction plan only. No repository, fixture, skill, control, or canonical candidate file was modified; no r2 correction or fresh r2 checkout/evaluation was available, so PASS/VERIFIED cannot be claimed.
- The staged defect report names four malformed FV-003 cases but does not include their individual payloads. The plan therefore relies on the frozen missing/invalid `ucl`/`lcl` contract and the staged finite-value reference rather than inventing payload details.
- The candidate's existing green test suite does not cover `chartViewer.js`; it is not evidence that F1 is resolved.

### CONTROLLED CELL META

- Profile: `root-cause-debugging`
- Cell: `D1`
- Arm: disabled; `targetSkillAttachment=DISABLED_FOR_AB`
- Scenario: `S06-VERIFICATION-TESTING`; dataset `SPC-MISSION-V1`; scenario revision `S06-r2-spc-mission-v1`
- Input package: `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-root-cause-debugging/`

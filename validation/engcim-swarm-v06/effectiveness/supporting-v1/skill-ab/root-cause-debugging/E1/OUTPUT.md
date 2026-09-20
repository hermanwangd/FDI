### RESULT

CorrectionReasoningResult: F1 is confirmed against the supplied r1 candidate revision `f63aa7ff1037d840c0b9338e455def256956bed2`; no correction was implemented in this cell.

Root cause: `r1-candidate-exact/src/chartViewer.js:5-9` treats the mere presence of `config.limits` as sufficient. It never validates `limits.ucl` or `limits.lcl` with the finite-number predicate required by the frozen Chart Management contract. Therefore configurations with a missing or invalid control limit reach the successful `{ rendered: true, ... }` return path. The same file has an independent frozen-contract mismatch at `:1-3`: `chartLimits()` returns `max: 1000`, while the required value is `10`. The evidence establishes the literals and behavior, but not why the stale `1000` was introduced.

Required governed correction:

- Expand the correction scope to `src/chartViewer.js` under Finding F1.
- Make `renderChart` reject a missing `limits`, a missing `limits.ucl`/`limits.lcl`, or a non-finite `limits.ucl`/`limits.lcl` with `INVALID_CHART_CONFIGURATION` before rendering; valid finite limits must continue to render.
- Change the chart-limit result to `{ min: 0, max: 10 }`.
- Preserve the selected-chart interaction and HTTP 404 mapping `{ status: 404, retryable: false }`; do not change unrelated chart-limit or creation behavior.
- Add/run regression coverage for missing and invalid `ucl`/`lcl`, valid-chart rendering, `chartLimits().max === 10`, the selected-chart interaction, and 404 non-retryability. Produce a distinct r2 revision and run fresh S06 verification from a fresh checkout before resolving F1; r1 evidence cannot be reused as r2 evidence.

### EVIDENCE USED

Frozen protocol and profile readbacks:

- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_dispatch-descriptions/COMMON-CELL-PROTOCOL.md`
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/ROOT-CAUSE-DEBUGGING.md`

Allow-listed S06 inputs read:

- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-root-cause-debugging/S06.json` — scenario `S06-r2-spc-mission-v1`, digest `sha256:ae3f75cbb80f5808502dbd8c7e17efc621b1b46d254279bbb9ac4b641176d28d`.
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-root-cause-debugging/PC1-v2.yaml` — frozen context revision 2, source snapshot `SPC-MISSION-V1-r1`.
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-root-cause-debugging/f1-evidence/S06-r1-defect-report-F1.md` — binds F1 to r1 commit `f63aa7ff1037d840c0b9338e455def256956bed2` and records the expected/actual behavior.
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-root-cause-debugging/source-corpus/repository-manifest.json`, `product-rules.yaml`, `chart-management-spec.md`, `product-training.md`, `verification/FV-003.md`, `verification/api-contract.md`, and `verification/valid-chart-regression.md`.
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-root-cause-debugging/source-corpus/repos/chart-management-api/src/chartApi.js` — finite `ucl`/`lcl` predicate; `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-root-cause-debugging/source-corpus/repos/chart-viewer/src/chartViewer.js` — reject-before-draw behavior.
- `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-root-cause-debugging/r1-candidate-exact/src/chartViewer.js`, `src/interaction.js`, and `test/interaction.test.js`; candidate README/package metadata were also read.
- The corresponding `/Users/herman_mbp2023/Documents/FDI-supporting-gates-skill-context-v1/validation/engcim-swarm-v06/effectiveness/supporting-v1/_controlled-inputs/s06-root-cause-debugging/r1-fixture-source` files were read and `src/chartViewer.js` was compared to the candidate; the comparison was empty.

Commands/readbacks actually run:

- `multica issue get 01a0bef1-216b-7b69-8f6e-d92acadaa420 --output json`
- `multica issue comment list 01a0bef1-216b-7b69-8f6e-d92acadaa420 --roots-only --summary --compact --output json` → no comments/threads.
- `(cd .../r1-candidate-exact && node --test)` → 2 passed, 0 failed; these tests cover interaction and 404 only.
- A read-only Node reproduction against `src/chartViewer.js` tested null/missing limits, missing `ucl`, missing `lcl`, invalid `ucl`, invalid `lcl`, and valid limits. The malformed-limit cases returned rendered success except the two cases rejected solely because the entire `limits` object was absent; the valid case rendered.
- The same reproduction returned `chartLimits {"min":0,"max":1000}`.
- `nl -ba` readbacks showed candidate `chartViewer.js:6-9` has no `ucl`/`lcl` validation and `:2` contains `max: 1000`; the source-corpus validator requires `Number.isFinite` for both fields.
- `diff -u` between candidate and fixture `src/chartViewer.js` produced no differences.

### LIMITATIONS

- This was a read-only supporting cell. No repository file, candidate, or frozen input was modified; no correction was implemented.
- The allow-listed package did not include a fresh checkout or r2 candidate, so fresh-r2 verification and resolution of F1 remain outstanding.
- The full evaluator command/output and its exact four malformed-case fixtures were not available in the allow-listed package; the direct reproduction reports only the cases explicitly exercised above.
- The inputs establish the required behavior and correction obligations, not the historical author or reason for the stale `1000` literal.

### CONTROLLED CELL META

- Profile: `root-cause-debugging`
- Cell: `root-cause-debugging/E1`
- Arm: `targetSkillAttachment=ENABLED`
- Canonical skill: RC6 `root-cause-debugging` only
- Scenario: `S06-VERIFICATION-TESTING`, revision `S06-r2-spc-mission-v1`
- Input package: `_controlled-inputs/s06-root-cause-debugging/`
- Fixture/source revision: `SPC-MISSION-V1-r1`
- Candidate under analysis: r1 commit `f63aa7ff1037d840c0b9338e455def256956bed2`

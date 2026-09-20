# Defect report: malformed chart limits are rendered as valid (FV-003)

- Finding: `F1` bound to S06 candidate revision 1.
- Discoverer: Swarm QA Tester (`dd35fcda-f118-4574-8a58-fdb16c60d5b2`) ｜ 2026-09-20.
- Target: canonical fixture at `f63aa7ff1037d840c0b9338e455def256956bed2`; Node fixture runtime.
- Severity: Critical — the chart viewer accepts malformed configuration and the frozen chart-limit contract is also wrong.
- Related cases: `TC-S06-004`, `TC-S06-005`, `TC-S06-006`, `TC-S06-007`, `TC-S06-009`.

## Reproduction

1. Fresh-check out `https://github.com/hermanwangd/engcim-v06-chart-viewer-fixture.git` at `f63aa7ff1037d840c0b9338e455def256956bed2` and confirm the worktree is clean.
2. Run the evaluator command recorded in `S06-r1-command-output.txt`, or run:
   `S06_CANDIDATE_ROOT=<fresh-checkout> node --test --experimental-test-coverage validation/engcim-swarm-v06/effectiveness/s04-s06/verification/S06-r1-verification.test.mjs`.
3. Observe the four FV-003 cases. For each malformed configuration, `renderChart` returns `{rendered: true, limits: ...}` instead of throwing `INVALID_CHART_CONFIGURATION`.
4. Observe the frozen-limit assertion. `chartLimits()` returns `{min: 0, max: 1000}` instead of max `10`.

## Expected versus actual

- Expected: missing or invalid `limits.ucl`/`limits.lcl` throws `INVALID_CHART_CONFIGURATION` and never renders a valid chart; `chartLimits().max` is `10`.
- Actual: all four malformed configurations render successfully with no error; `chartLimits().max` is `1000`.

## Impact and disposition

- Impact: malformed chart configuration can be treated as a valid rendered chart, violating the Chart Viewer acceptance intent and the sealed FV-003 contract.
- Release blocking: yes; F1 keeps `CTRL-FINDING-RESOLUTION-001` `UNSATISFIED` until a governed correction produces a distinct r2 and fresh S06 verification.
- Candidate mutation: none. Correction handoff is downstream and must not reuse this r1 verdict as r2 evidence.

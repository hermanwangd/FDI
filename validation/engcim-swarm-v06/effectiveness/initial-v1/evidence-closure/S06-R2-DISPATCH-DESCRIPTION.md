# S06 r2 Fresh Verification Dispatch

## Exact input binding

- Parent mission: E6V-14.
- Upstream S05 delivery: E6V-34:r2 / `S05-E6V34-DEVELOPMENT-r2`.
- FDI evidence/reseal revision: `d58d3848f12d8c1ad1704852d175b9f656ef66e2`.
- Product candidate revision: `c51390ca7e748f07201b0ecd28642ee3ea8d686c`.
- Product candidate tree: `2d3de714bd30552eb572d7e3bfb0f81dbed373a3`.
- Repository: `https://github.com/hermanwangd/engcim-v06-chart-viewer-fixture.git`.
- S05 Reviewer: E6V-37 `PASS`.
- S05 Verifier: E6V-38 fresh run `01a0bd7b-c5bf-7576-8cb1-bef4c62c6a7b`, `VERIFIED`.
- Gate A: passed before this dispatch.

## F1 identity

- Finding: `F1 / FV-003`.
- F1 digest: `sha256:e4235a148aed2c9c551678fb6f375cd4a4f23d58e1ade18d7265e0ef3131e001`.
- Finding's r1 candidate: `f63aa7ff1037d840c0b9338e455def256956bed2`.
- Current r2 candidate to verify: `c51390ca7e748f07201b0ecd28642ee3ea8d686c`.
- Current finding resolution before S06: `OPEN` / `UNSATISFIED`.

## Fresh verification requirements

Use a fresh independent checkout and independently resolve the exact r2 candidate. Verify the four missing/invalid `limits.ucl` and `limits.lcl` cases produce `INVALID_CHART_CONFIGURATION` and do not render a valid chart; verify `chartLimits().max === 10`; preserve valid numeric rendering; and verify selected-chart opening and HTTP 404 `retryable=false` regression behavior.

Read the attached receipt, stale-evidence check, and F1 report as evidence inputs only. Reject all S06 r1 review/verifier results as stale for r2. Missing provenance, exact binding, or evidence integrity is `INCONCLUSIVE`, not PASS.

## Prohibited inputs and actions

- Do not assume or write an expected verdict.
- Do not receive hidden evaluator implementation or exact patch instructions.
- Do not reuse S06 r1 PASS/VERIFIED gates.
- Do not modify the product candidate, tests, scenario, skill, control, runtime, gold, or fixtures.
- Do not mark F1 resolved; Finding Resolution remains open until fresh S06 r2 `PASS / VERIFIED`.

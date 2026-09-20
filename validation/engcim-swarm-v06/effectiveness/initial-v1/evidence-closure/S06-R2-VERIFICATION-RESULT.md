# S06 r2 Verification Result

Status: `PASS / VERIFIED`.

The exact current candidate is `c51390ca7e748f07201b0ecd28642ee3ea8d686c`,
tree `2d3de714bd30552eb572d7e3bfb0f81dbed373a3`, from the canonical fixture
repository. FDI evidence revision is `d58d3848f12d8c1ad1704852d175b9f656ef66e2`.

- Producer: E6V-39, run `01a0bd86-5258-7312-a1a6-c4878a085a66`, `PASS`.
- Evidence delivery revision: `2`, run `01a0bd92-e703-7e66-b97b-42ac597ced90`.
- Fresh Reviewer: E6V-40, run `01a0bd94-7aba-7d58-bca5-cb7b125143fb`, `PASS`.
- Fresh Verifier: E6V-41, run `01a0bd94-7a94-7d68-83f0-5b3c97c574d1`, `VERIFIED`.

Fresh checks independently reproduced the four malformed/invalid limit cases
as `INVALID_CHART_CONFIGURATION` with no valid render, `chartLimits()` as
`{ min: 0, max: 10 }`, valid numeric rendering, one-interaction selected-chart
opening, HTTP 404 as `{ status: 404, retryable: false }`, and `npm test` as
9 passed / 0 failed. S06 r1 evidence was rejected as stale; it was accepted
zero times for this result. No product, test, fixture, scenario, control,
runtime, or finding-state file was modified by the fresh gates.

Finding Resolution may now evaluate F1 against this exact fresh result.

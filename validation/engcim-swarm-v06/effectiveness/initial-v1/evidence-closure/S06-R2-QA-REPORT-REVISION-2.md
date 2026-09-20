# S06 r2 QA Delivery Report — Revision 2

revision: 2
deliveryRef: E6V-39
producerRunRef: multica:issue/E6V-39/run/01a0bd86-5258-7312-a1a6-c4878a085a66
candidateRevision: c51390ca7e748f07201b0ecd28642ee3ea8d686c
candidateTree: 2d3de714bd30552eb572d7e3bfb0f81dbed373a3
repositoryRef: https://github.com/hermanwangd/engcim-v06-chart-viewer-fixture.git
findingRef: F1 / FV-003
findingDigest: sha256:e4235a148aed2c9c551678fb6f375cd4a4f23d58e1ade18d7265e0ef3131e001

This is an evidence-only revision of the S06 r2 delivery report. The product
candidate, tests, fixtures, scenarios, controls, runtime, and finding state
were not changed. The exact candidate was checked out in a clean worktree.
S06 r1 evidence is stale and is not used as proof for this delivery.

## Exact observed outputs

Command context: `npm test`, targeted chart-viewer coverage, and a direct
acceptance matrix executed against the exact candidate above.

| Case | Exact observed output | Result |
|---|---|---|
| missing `limits.ucl` | `error=INVALID_CHART_CONFIGURATION; validRender=false` | PASS |
| invalid `limits.ucl` | `error=INVALID_CHART_CONFIGURATION; validRender=false` | PASS |
| missing `limits.lcl` | `error=INVALID_CHART_CONFIGURATION; validRender=false` | PASS |
| invalid `limits.lcl` | `error=INVALID_CHART_CONFIGURATION; validRender=false` | PASS |
| `chartLimits()` | `{ min: 0, max: 10 }` | PASS |
| valid numeric limits | `{ rendered: true, limits: { ucl: 10, lcl: 0 } }` | PASS |
| selected-chart opening | `{ action: 'OPENED', chartId: 'chart-7' }`; invocation count `1` | PASS |
| HTTP 404 | `{ status: 404, retryable: false }` | PASS |

The repository regression suite returned `9 passed, 0 failed`. Targeted
chart-viewer coverage was `100.00%` line, branch, and function. Additional
exploratory boundary checks returned `10/10` PASS. The full-suite measured
coverage was `98.72%` line, `92.86%` branch, and `100.00%` function; the
remaining `src/interaction.js:10` gap is the non-404 response branch and is
not part of FV-003's required 404 regression.

## Provenance and stale-evidence controls

- `git rev-parse HEAD` = `c51390ca7e748f07201b0ecd28642ee3ea8d686c`.
- `git rev-parse HEAD^{tree}` = `2d3de714bd30552eb572d7e3bfb0f81dbed373a3`.
- The checkout was clean before and after verification.
- Old S06 r1 candidate `f63aa7ff1037d840c0b9338e455def256956bed2` is rejected
  for this r2 result; old r1 review and verification are accepted `0` times.
- F1 remains `OPEN` pending independent S06 r2 review and verification.
- No product source, tests, fixtures, scenario, control, runtime, or finding
  resolution state was modified.

## Delivery status

Fresh S06 r2 behavior result: `PASS`.

This revision supplies the explicit artifact revision marker and literal
observed outputs requested by the independent review. It does not resolve F1.

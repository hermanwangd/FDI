# FV-003 Correction Loop

## Two separate results

| Path | Result |
|---|---|
| Deterministic reference clone | PASS |
| Real Multica S05 → S06 loop | BLOCKED / NOT RUN |

## Reference clone evidence

- r1: `ba55d2e84b245a1500383733c398be9ccad15bef`
- r1 command: `node tests/fv003-invalid-limits.mjs`
- r1 observed: exit non-zero; malformed limits were rendered as a chart, so
  the regression assertion correctly detected FV-003
- F1: malformed `limits.ucl` / `limits.lcl`, bound to r1
- r2: `8977df8388e04e4f4599d3fca1851ff0fd1e7fd9`
- r2 command: `node tests/fv003-invalid-limits.mjs`
- r2 observed: exit 0; `INVALID_CHART_CONFIGURATION` returned and the invalid
  chart was not returned as a valid chart

The raw outputs are `evidence/r1-fv003.out` and `evidence/r2-fv003.out`.

## Why this is not B2 PASS

No S05 worker produced DevelopmentResult r1, no real S06 worker produced
VerificationFinding F1, and no real current-revision Reviewer/Verifier gates
were observed. The isolated clone was not silently promoted to a Multica
scenario run.

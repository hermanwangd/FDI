# S06 r2 Fresh Independent Verifier Dispatch

Parent: E6V-39, fresh S06 r2 execution.

Independently check out the canonical repository at exact product candidate `c51390ca7e748f07201b0ecd28642ee3ea8d686c` and verify the exact tree `2d3de714bd30552eb572d7e3bfb0f81dbed373a3`. Do not rely on the producer's test output as the only evidence.

Run the required fresh checks:

- missing `limits.ucl` -> `INVALID_CHART_CONFIGURATION` and no valid render;
- invalid `limits.ucl` -> `INVALID_CHART_CONFIGURATION` and no valid render;
- missing `limits.lcl` -> `INVALID_CHART_CONFIGURATION` and no valid render;
- invalid `limits.lcl` -> `INVALID_CHART_CONFIGURATION` and no valid render;
- `chartLimits().max === 10`;
- valid numeric limits still render;
- selected-chart opening remains one interaction;
- HTTP 404 remains `retryable=false`.

Freshly reject S06 r1 PASS/VERIFIED evidence as stale. If provenance, exact binding, or evidence integrity is missing, issue `INCONCLUSIVE`; do not convert missing evidence to PASS. Do not modify the candidate, tests, fixtures, Scenario, Skill, Control, runtime, or gold. Do not resolve F1 in the verifier issue; only provide a fresh S06 r2 verdict bound to this candidate.

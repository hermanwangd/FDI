# Product Context A/B Experiment Freeze

Freeze revision: `product-context-ab-v0.2-final`

## Conditions

| Condition | Composed context | Raw inputs | Resolver / preload |
|---|---|---|---|
| PC0 | unavailable by design | identical SPC-MISSION-V1 corpus | no resolver call |
| PC1 | `product-context/PC1.yaml`, revision 1 | identical SPC-MISSION-V1 corpus | frozen context supplied; raw fallback permitted and logged |
| PC1-STALE | `product-context/PC1-STALE.yaml` | identical corpus with current source revision | must fail closed and inspect raw source |

Mission-level effect compares `PC0 → S04 → S05 → S06` with
`PC1 → S04 → S05 → S06` using the same PM input. Scenario-level attribution
holds scenario revision, upstream artifact, candidate revision, evidence corpus,
role, model/provider, runtime, success criteria, and source revisions fixed;
only context availability changes.

Record per S04–S06:

```text
scenario result, first-pass acceptance, correctness, omissions,
unsupported assertions, evidence completeness, rework, clarification turns,
source-discovery operations, agent runs, elapsed time, and tokens when available
```

PC1 must not contain hidden S06 tests, exact patches, future r2, evaluator
verdicts, or hidden solutions. PC1-STALE is a separate safety case and is not
included in the main uplift score.

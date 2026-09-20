# Skill A/B Supporting Result

Protocol: `skill-ab-v0.2-final`  
Base core commit: `a07a023dd2e62db0338422b68daf1702adba1ee3`  
Valid design: 4 profiles × (2 disabled + 2 enabled) = 16 cells. n=2 per arm is descriptive; no statistical significance is claimed.

## Result

All four profiles completed valid 2×2 cells without material correctness or safety regression. Every arm aggregate is 100% correctness and evidence completeness, with zero critical omissions, zero accepted unsupported assertions, zero rework, and zero clarification turns. No frozen metric shows consistent directional improvement in the enabled arm.

Per pre-registration, each profile is `NO_MEASURABLE_UPLIFT`, not Skill PASS. The overall Skill Supporting Gate is `INCONCLUSIVE`; the frozen core conclusion is unchanged: `ENGCIM SWARM CORE EFFECTIVE`.

| Profile | Disabled n | Enabled n | Disabled correctness | Enabled correctness | Δ correctness | Classification |
|---|---:|---:|---:|---:|---:|---|
| `pk-repository-analysis` | 2 | 2 | 20/20 | 20/20 | 0 | `NO_MEASURABLE_UPLIFT` |
| `pm-intention` | 2 | 2 | 8/8 | 8/8 | 0 | `NO_MEASURABLE_UPLIFT` |
| `test-architecture` | 2 | 2 | 10/10 | 10/10 | 0 | `NO_MEASURABLE_UPLIFT` |
| `root-cause-debugging` | 2 | 2 | 10/10 | 10/10 | 0 | `NO_MEASURABLE_UPLIFT` |

## Fixed metric interpretation

- `correctness` is accepted golden assertions / applicable golden assertions.
- `critical omission count` is omitted critical items / frozen critical items.
- `unsupported assertion count` is accepted unsupported assertions / accepted assertions.
- `evidence completeness` is valid required refs / frozen required refs.
- `rework count` and `clarification count` are recorded as event counts.
- No weighted score or ranking was used.

## Input-drift handling

The first test-architecture D1 and E1 runs used `r1-fixture-source` at `a07a023...` instead of the required exact r1 candidate `f63aa7...`. They are preserved as `FIXTURE_ENVIRONMENT` invalid evidence and excluded from n. Replacement D1/E1 runs used the exact candidate; the valid 2×2 result is based only on those replacements plus D2/E2.

## Run/evidence references

Exact per-cell run refs and binding/evaluation paths are recorded in `SKILL-AB-RUN-REGISTRY.json`. Each valid cell contains `CELL-BINDING.json`, `OUTPUT.md`, `EVALUATION.json`, and `RUN-RECEIPT.json`; invalidated test-architecture receipts are retained beside the replacement evidence.

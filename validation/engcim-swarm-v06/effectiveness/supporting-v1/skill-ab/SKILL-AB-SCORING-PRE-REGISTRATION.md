# Skill A/B Scoring Pre-Registration

Protocol: `skill-ab-v0.2-final`  
Namespace: `supporting-v1`  
Pre-registration status: `BOUND_BEFORE_DISPATCH`

## Fixed design

Each profile has two independent disabled cells and two independent enabled
cells. The only intended within-profile difference is the target Skill
attachment. Every cell uses the same scenario revision, input package, Product
Context variant, role, model/provider/runtime, task objective, success criteria,
and evaluator boundary. A cell is evaluated only after its output is frozen.

No statistical significance claim is permitted at `n=2`.

## Frozen metrics

- `correctness = accepted golden assertions / applicable golden assertions`
- `critical omission count = omitted critical items / frozen critical items`
- `unsupported assertion count = accepted unsupported assertions / accepted assertions`
- `evidence completeness = valid required refs / frozen required refs`
- `rework count = post-first-delivery revision rounds`
- `clarification count = human clarification turns`

Numerators and denominators must be reported where applicable. No weighted score
may be introduced.

## Decision rules

`PASS / MEASURABLE UPLIFT` requires all of:

1. enabled correctness does not regress against disabled;
2. enabled unsupported-assertion rate does not worsen;
3. enabled evidence completeness does not worsen;
4. at least one frozen metric improves directionally;
5. the improvement is not contradicted by the second enabled repetition; and
6. both enabled cells remain inside the frozen safety boundaries.

`NO_MEASURABLE_UPLIFT` means no material regression but no frozen metric shows
consistent directional improvement. This is not a Skill PASS claim.

`FAIL / REGRESSION` means the enabled attachment causes a material correctness
or safety regression.

`INCONCLUSIVE` applies when the minimum repetitions are incomplete, evidence is
incomplete, runtime/input drift occurs, or within-arm outcomes materially
conflict.

## Instability expansion

Run the pre-registered `2 x 2` design first. If and only if a profile is
unstable, record observed instability in that profile's
`EXPANSION-DECISION.json`, then run only `D3` and `E3`. Stable profiles are not
expanded.

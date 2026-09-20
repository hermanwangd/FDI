# Initial Effectiveness Baseline Addendum v0.1

Base freeze: validation/engcim-swarm-v06/effectiveness/initial-v1/ at a07a023dd2e62db0338422b68daf1702adba1ee3.

## Purpose

This addendum completes supporting validation evidence for the two open limitations: controlled Skill A/B cells and PC1-STALE context safety. It does not rewrite or supersede the original initial-v1 freeze.

## Measured supporting results

- Skill A/B: 16 valid cells, four profiles, two disabled and two enabled repetitions per profile. Each profile is NO_MEASURABLE_UPLIFT; no material enabled-arm regression was measured, and no Skill PASS claim is made. Two initial test-architecture runs were excluded as FIXTURE_ENVIRONMENT input drift and replaced with exact r1 candidate runs.
- PC1-STALE: S04/S05/S06 all PASS 5/5 safety behaviors; stale mapping override = 0; wrong-source acceptance = 0; unsafe authorization from stale context = 0.
- Supporting Product Context Gate: PASS.
- Overall Supporting Gate: PARTIAL.

## Freeze boundaries

- Core primary gates were not reopened.
- Core architecture was not modified.
- The conclusion ENGCIM SWARM CORE EFFECTIVE remains unchanged.
- No frozen scenario definition, control, Skill source, Product Knowledge implementation, Product Context source, runtime, product candidate, gold, threshold, denominator, or initial-v1 result file was changed.
- No result was auto-fixed. The fixture-drift runs are preserved and explicitly invalidated; replacement runs are separately receipted.

Supporting evidence is under validation/engcim-swarm-v06/effectiveness/supporting-v1/. Stop after human review of this addendum; do not begin holdout/generalization/scaling or architecture/skill redesign from this result alone.


# PK Validation Baseline Freeze

Freeze revision: `pk-baseline-v0.2-final`
Dataset A: `PKB001-PETCLINIC`
Dataset B for S02/S03: `SPC-MISSION-V1`

| Gate | Status | Evidence / blocker |
|---|---|---|
| S01 semantic item types present | YES | `s01/S01-SEMANTIC-GOLD.json` contains PRODUCT_FACT, CAPABILITY, BEHAVIOR_SCENARIO, PRODUCT_RULE_OR_CONSTRAINT |
| S01 critical items evidence-backed | YES as candidate data | claim refs resolve to the source manifest |
| S01 source-level re-adjudication | NO | original Petclinic source checkout/tests are not included in this baseline namespace |
| S01 independent review | NO | `s01/gold-review.md` is pending |
| S02 exactly eight deltas | YES | `s02/fixture/D1...D8` |
| S02 Refresh Gold complete | YES as candidate data | `s02/S02-REFRESH-GOLD.json` |
| S02 independent review | NO | `s02/gold-review.md` is pending |
| S03 three relevant repositories | YES | `s03/fixture/repository-manifest.json` |
| S03 distractor repository and negatives | YES | realization gold includes distractor, non-dependency, and near miss |
| S03 independent review | NO | `s03/gold-review.md` is pending |
| PK evaluator/generation isolation | YES | scoped isolation is recorded in manifests |
| PK scoped seal | NOT_READY | review and S01 re-adjudication incomplete |

S01 is standalone semantic calibration only. No Petclinic item is used as SPC
Product Context or as an S02–S06 input. This baseline therefore does not claim
S01→SPC compatibility.

```text
PK VALIDATION BASELINE = NOT_READY
```

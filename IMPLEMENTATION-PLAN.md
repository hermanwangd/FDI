# Software Factory Implementation Plan

## Current selection

No implementation execution is currently selected. `SF-BL-002` remains
`IN_PROGRESS` with decision `REVISE`; that state does not authorize another
execution. The Feature Delivery Plane must prepare and select a bounded
correction plan before redispatch.

`SF-BL-001` and `SF-BL-003` remain unselected.

## Verified delivery ledger

### SF-BL-002-SCENARIO-EFFECTIVENESS-004

- Backlog: `SF-BL-002`
- Requirements: `AUTH-002`, `PK-004`, `EVID-001`, `TECH-001`
- Execution base: `feab9dfa03be5240ade2951c8f500a230ebc6f33`
- Integrated candidate: `9f83c8a3877bdb4a98c4e1fcf1a09947741c573c`
- Delivered behavior: deterministic evaluator-blind PRIMARY matching,
  exact-seed bounded Graphify mapping, and evaluator-only hierarchical scoring
  over the accepted search intents.
- Engineering verification: `1109/1109` tests passed; immutable `-002`
  artifacts reproduced byte-for-byte; prior `validation/pkb001/` and `*-001`
  evidence were unchanged; independent combined review passed.
- Experimental result: `REVISE`; scenario trace `7/10`, exact component match
  `2/24`, PRIMARY precision `0.2857`, recall `0.0833`, and F1 `0.1290`.
  Precision did not meet the frozen `0.70` acceptance threshold.
- Evidence:
  `validation/software-factory/sf-bl002/remediation-evidence.json` and
  `validation/software-factory/sf-bl002/hierarchical-evaluation-002.json`.
- Authority boundary: the evidence is proposal/evaluator evidence only. It
  neither publishes Product truth nor terminally closes `SF-BL-002`.

## Continuation constraints

A future correction plan must:

1. retain the existing `*-001` and `*-002` evidence immutably;
2. use a new execution and artifact identity;
3. remain evaluator-blind during generation;
4. improve behavior-aware PRIMARY selection without force-mapping weak
   evidence; and
5. bind the acceptance thresholds as enforceable evaluator outcomes.

No new execution begins until its Backlog selection, exact base commit, owned
paths, acceptance checks, negative cases, TDD sequence, review boundary, and
verification commands replace the current-selection section in this file and
`STATUS.json` is updated in the same commit.

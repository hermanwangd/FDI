# Control Effectiveness

Overall: `PASS`.

Using the frozen Control Applicability Matrix, the accepted final path has 18
required Control applications: S01 2, S02 2, S03 3, S04 2, S05 4, S06 4,
and mission closure 1. All 18 final-path applications are satisfied. The
execution evidence also records supplemental provenance checks where a scenario
emitted an extra applicable check.

| Measure | Result |
|---|---:|
| Required final-path Control applications | 18 |
| Final-path satisfied | 18 |
| Required gate bypasses | 0 |
| Control Bypass Rate | 0/18 = 0% |
| Manual child-done transitions used to bypass gating | 0 |
| Accepted duplicate correction executions | 0 |
| Stale r1 review/verification accepted for r2 | 0 / 0 |

Correct fail-closed decisions are preserved rather than erased: S04 initial
source mutation produced INCONCLUSIVE controls and blocked progression; S06 r1
left `CTRL-FINDING-RESOLUTION-001` UNSATISFIED; the first S05 r2 verifier was
PARTIAL; the cancelled YAML-parser run produced no accepted verdict. The later
receipt and fresh gates completed the required closure.

Evidence:

- `baseline/gate0/CONTROL-APPLICABILITY-MATRIX.md`
- `baseline/controls/CONTROL-BINDING-FREEZE.md`
- `baseline/controls/required-evidence.json`
- `results/S04-EFFECTIVENESS-RESULT.json`
- `results/S05-EFFECTIVENESS-RESULT.json`
- `results/S06-EFFECTIVENESS-RESULT.json`

# Product Knowledge Effectiveness

Overall: `PASS`.

| Dimension | Result | Evidence |
|---|---|---|
| PK-E1 Semantic Effectiveness | PASS | S01 25/25 overall, 18/18 critical; gold excluded from generation; source revision `818c4136...` |
| PK-E2 Realization Effectiveness | PASS | S03 r2 5/5 critical edges, 25/25 provenance-backed edges, 4/4 repositories, 3/3 impact components |
| PK-E3 Governance Effectiveness | PASS | S02 D1-D8 8/8; D4 conflict preserved with no winner; D3 stale support explicitly retired |
| PK-E4 Evidence Effectiveness | PASS | S01/S02/S03 claim and source bindings complete; S05 r2 required evidence 15/15 after closure |
| PK-E5 Context Effectiveness | PASS with governance follow-up | S04 clean rerun used immutable PK snapshot and removed source instability from the accepted run; the original mutation remains a governance finding |
| PK-E6 Downstream Engineering Value | PASS | S04 authorization → S05 r1 → S06 r1 F1 → S05 r2 → fresh S06 r2 → Finding Resolution |

The S02 conflict is an expected governance finding, not a silent semantic
winner. The S04 mutable-source incident is resolved for the accepted clean
rerun, but source-stability governance is not claimed as permanently solved.

Primary evidence:

- `results/S01-EFFECTIVENESS-RESULT.json`
- `results/S02-EFFECTIVENESS-RESULT.json`
- `results/S03-EFFECTIVENESS-RESULT.json`
- `results/S04-EFFECTIVENESS-RESULT.json`
- `validation/engcim-swarm-v06/baseline/pk/PK-GROUND-TRUTH-SEAL.json`
- Multica `E6V-11`, `E6V-12`, `E6V-13`, `E6V-23`, `E6V-24`, `E6V-25`

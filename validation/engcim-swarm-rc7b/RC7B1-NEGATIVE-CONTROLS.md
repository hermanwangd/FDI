# RC7-B1 Negative Controls

## Classification

Reference-only negative-control result: `PASS`.

Formal B1 result remains `BLOCKED` because the package has no executable
RC7-B control evaluator to which these probes can be applied.

| Probe | Expected fail-closed behavior | Reference result | ENGCIM runtime result |
|---|---|---:|---|
| B1-N1 unknown candidate commit | provenance UNSATISFIED/INCONCLUSIVE | PASS | BLOCKED: no runtime predicate |
| B1-N2 reuse r1 review for r2 | freshness UNSATISFIED, `STALE_REVISION` | PASS | BLOCKED |
| B1-N3 reuse r1 verification for r2 | freshness UNSATISFIED, `STALE_REVISION` | PASS | BLOCKED |
| B1-N4 content changes without new identity | freshness/evidence integrity UNSATISFIED | PASS | BLOCKED |
| B1-N5 producer-only provenance | independent verification UNSATISFIED/INCONCLUSIVE | PASS | BLOCKED |
| B1-N6 unresolved finding without disposition | correction obligation UNSATISFIED | PASS | BLOCKED |
| B1-N7 resolved finding without correction evidence | correction obligation INCONCLUSIVE/UNSATISFIED | PASS | BLOCKED |

The raw deterministic outputs are in
`evidence/b1-reference-oracle.json`. No negative probe executed a destructive
command or modified the original fixture.

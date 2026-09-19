# Revision Freshness Regression

## Result

Reference-only result: `PASS`.

At r2, the oracle rejected both r1 review and r1 verification as
`UNSATISFIED / STALE_REVISION`; fresh review and verification at r2 were
accepted. The candidate identities were distinct and their Git trees differed.

| Gate | Candidate evaluated | Current candidate | Expected | Reference result |
|---|---|---|---|---|
| old Reviewer | r1 | r2 | UNSATISFIED / STALE_REVISION | PASS |
| old Verifier | r1 | r2 | UNSATISFIED / STALE_REVISION | PASS |
| fresh Reviewer | r2 | r2 | SATISFIED | PASS |
| fresh Verifier | r2 | r2 | SATISFIED | PASS |

No ENGCIM runtime gate was observed in a real S05/S06 run; formal B2 status is
BLOCKED.

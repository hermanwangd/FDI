# METHOD quality calibration 006

Independent adjudication and the unchanged scorer found a substantial improvement,
but the strict user goal is not yet met: precision is exactly 80%, not greater.

| Metric | Baseline (005 expansion replay) | Qualified producer |
|---|---:|---:|
| TP / FP / FN | 11 / 8 / 29 | 28 / 7 / 12 |
| Precision | 57.89% | 80.00% |
| Recall | 27.50% | 70.00% |
| F1 | 37.29% | 74.67% |
| Scenario coverage | 7/10 | 9/10 |
| Defined complete chains (diagnostic) | 0/9 | 2/9 |

Overall chain coverage remains unavailable because one chain definition is
missing. Formal experiment remains NOT_RUN; this is repeated exposed Petclinic
calibration, not an unseen holdout or Product truth. Parent remains IN_PROGRESS.
Engineering recommendation: REVISE. The target requires both recall >60% and
precision >80%; neither rounded values nor inclusive thresholds are substituted.

The seven FP comprise five source-supported but nonessential auxiliary calls,
and two unsupported fallback-creation/identity-plumbing claims in an
existing-target update scenario. The next generic correction is to avoid
projecting an unproved absent-target creation branch onto an existing-target
update; no gold items, scoring rules or denominators may change.

Candidate: 18c9a12d3578b42dbc2052c6d69e881395769f9b.
Full Java verification: 115 suites / 1377 tests, zero failures/errors/skips.
Python controls: 63 passed. Independent code/proof reviews are adjacent.
Two additional selector characterization tests passed after generation; they
did not alter producer code, JAR or output bytes and are not added to that full-run count.
The prior 005 improved proposals equal this baseline's proposals exactly.
Overwrite probe refused OUTPUT_EXISTS; sealed producer hashes remained unchanged.

Truth SHA256: 39aa27459305c05a4c983b0c414c60b39ec3e460491b9edb5da49c620bc2de2c.
Proof ledger SHA256: 48227b451f176cf4b0bb6709db0175275417bea4d9c851e4e068c79f8d85857c.
Comparison SHA256: bc653aec887167e6c449a72d850c27f86e9537f9269b4685252c569e8b8e3e43.
Source, runtime and generation hashes are in execution-seal.json.

Generation took approximately 2.41 seconds, excluding builds and independent
reviews. Initial code review did not pass; multiple synthetic-counterexample
remediations preceded final PASS. Token cost and full wall-clock delivery cycle
were not comprehensively instrumented; no workflow cost-reduction claim is made.

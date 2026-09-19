# RC7-A.1 Scenario Test Report

Final classification: `RC7-A.1 VALIDATED WITH CONDITIONS`.

## Counts

`PASS: 2 | PARTIAL: 1 | FAIL: 0 | BLOCKED: 0`

| Scenario | Result | Evidence boundary |
|---|---|---|
| S04 PM Intention / human resume | PASS after correction | WAITING_FOR_INPUT, exact PM comment, same-mission resume, CAS/stale/duplicate behavior, r2 authorization, Reviewer/Verifier revision 3 |
| S05 Software Development | PASS | Authorization gate ran before mutation; Development Result r1 delivered; independent Reviewer PASS r1; source checkout limitation remains |
| S06 Verification / FV-003 | PARTIAL | Independent verifier reproduced final behavior, but provenance/gate execution evidence is unavailable and no distinct Development Result r2 correction loop was produced |

The RC7-A.1 prompt explicitly excludes S01-S03 and S07-S10. Product Context A/B uplift is not measured in this focused benchmark.

The classification is conditional rather than fully validated because the benchmark's mandatory FV-003 correction-loop and S06 `Verification Result PASS` predicates were not both satisfied.

## Explicit benchmark answers

- Human resume: YES for the S04 state/resume path, after revision-gated correction.
- Durable state: YES; stateRef used attachment references and CAS/stale/duplicate behavior was evidenced.
- Metadata ceiling: YES; parent projection used only `swarm.stateRef`, `swarm.stateRevision`, and `swarm.summaryStatus`.
- Authorization gate: YES; S05 ran the gate before repository mutation and received `PASS implementation authorization`.
- FV-003 correction loop: PARTIAL; final malformed-chart behavior was independently reproduced as corrected, but the required r2 correction artifact/retest sequence was not completed.
- SPC pilot readiness: NO for an unconditioned pilot; see the remaining gates in `REAL-MULTICA-VALIDATION.md`.

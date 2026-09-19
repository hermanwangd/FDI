# RC7-A.1 Durable State and Human Resume Regression

## S04.1 WAITING_FOR_INPUT

S04.1 produced an Intention Spec revision 1 with `implementationAuthorized=false`, two blocking questions, and durable `WAITING_FOR_INPUT` state revision 2. The parent projected only the allowed metadata keys:

```json
{
  "swarm.stateRef": "attachment:01a0b888-c242-7b8a-b862-73eea48f8ec3",
  "swarm.stateRevision": 2,
  "swarm.summaryStatus": "WAITING_FOR_INPUT"
}
```

The state reference was a durable attachment reference, not a local filesystem path.

## Human decision and resume

One normal top-level PM comment was supplied, exactly once, with only these decisions:

```text
Q-interaction-target: 1 primary-actions
Q-retry-404: NON_RETRYABLE_USER_ERROR
```

The resume produced a DecisionResponse and MissionResumeEvent with the same `missionRef` and a new `runRef`. CAS with expected revision 2 advanced state to revision 3. A stale expected-revision replay produced `STALE_STATE`; a duplicate replay produced `NO_OP`.

## Defect and correction history

The first resume delivery contained `attachment:pending-mission-state-snapshot` in the event/state artifact. Reviewer revision 1 and Verifier revision 1 rejected it. A second correction removed the placeholder but still referenced an older snapshot, which Reviewer revision 2 rejected while Verifier revision 2 verified the other checks. The third delivery aligned the MissionResumeEvent, fresh state revision 3, and parent projection to the canonical attachment reference.

Final S04.2 gate evidence:

- Reviewer run `01a0b8a6-7bf4-7dc0-99f2-eb9cee604e4c`: PASS revision 3.
- Verifier run `01a0b8a6-7bf7-7da5-871c-69baab510f32`: VERIFIED revision 3.
- Fresh state revision 3 SHA-256: `1631b07dcf66237b17884fdd3c5b61427e5c60baccd47273353adc9139f82fab`.
- Parent projection: canonical attachment reference, revision 3, `RUNNING`.

This is evidence for a successful human resume after correction, not evidence that the initial artifact was defect-free.

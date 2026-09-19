# State Consistency Regression

Result: `PASS for the S04.2 current revision`.

- Normal CAS expected revision 2 advanced durable state to revision 3.
- Stale replay with expected revision 2 returned `STALE_STATE`.
- Duplicate replay returned `NO_OP`.
- Parent metadata remained within the compact ceiling: `swarm.stateRef`, `swarm.stateRevision`, `swarm.summaryStatus`.
- Current artifact, MissionResumeEvent, and parent projection all referenced the canonical state attachment.

The correction history is retained in `RC7A1-DURABLE-STATE-REGRESSION.md`; earlier invalid references are not hidden or rewritten as successful first-pass output.

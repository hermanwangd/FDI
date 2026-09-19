# Human Input Resume Regression

Result: `PASS after correction`.

S04 paused with `WAITING_FOR_INPUT`, emitted a durable attachment state reference, and kept `implementationAuthorized=false`. One normal PM comment supplied exactly two decisions. The resumed mission kept the same `missionRef` and used a new `runRef`; the two answers resolved the blocking questions and produced Intention Spec revision 2 with authorization true.

The first resume artifact had a placeholder state reference and was rejected by Reviewer/Verifier. A second artifact used an older snapshot and was rejected by the Reviewer. Revision 3 aligned the canonical attachment reference across the resume event, state snapshot, and parent projection; Reviewer PASS and Verifier VERIFIED were both obtained for that current revision.

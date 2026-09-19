# B1 Negative Controls

`PASS` means the actual evaluator rejected the unsafe, stale, incomplete, or
non-independent input with a deterministic reason. It does not mean the
underlying operation was performed.

| Probe | Actual result |
|---|---|
| R1 binding used while R2 is current | UNSATISFIED / STALE_BINDING |
| subject identity differs | UNSATISFIED / SUBJECT_MISMATCH |
| required evidence missing | UNSATISFIED / MISSING_EVIDENCE |
| evidence unresolved | UNSATISFIED / UNRESOLVABLE_EVIDENCE |
| evidence insufficient | UNSATISFIED / INSUFFICIENT_EVIDENCE |
| evidence digest changed under same identity | UNSATISFIED / INVALID_EVIDENCE |
| producer evaluates its own delivery | UNSATISFIED / EVALUATOR_NOT_INDEPENDENT |
| protected-branch force push | UNSATISFIED / PROTECTED_BRANCH_MUTATION |
| unknown mutation target | INCONCLUSIVE / UNKNOWN_MUTATION_TARGET |
| open finding with owner/correction request | UNSATISFIED / UNRESOLVED_FINDING |
| R2 exists without fresh verification | UNSATISFIED / RESOLUTION_EVIDENCE_MISSING |
| stale R1 resolution after R2 current | UNSATISFIED / STALE_RESOLUTION_EVIDENCE |
| reconstructed `file://` repository | UNSATISFIED / REPOSITORY_UNRESOLVABLE |
| unknown candidate commit | UNSATISFIED / CANDIDATE_UNRESOLVABLE |

No destructive command was executed to obtain these results.

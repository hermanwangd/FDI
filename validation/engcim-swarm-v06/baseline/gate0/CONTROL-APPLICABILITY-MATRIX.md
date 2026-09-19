# Control Applicability Matrix

Freeze revision: `control-applicability-v0.2-final`

`NONE` is explicit: the control is not applicable at that scenario/gate. It
does not mean the control was evaluated or passed.

| Scope | Gate | Applicability | Required Controls |
|---|---|---|---|
| S01 | source ingestion | REQUIRED | `CTRL-REPOSITORY-PROVENANCE-001`, `CTRL-EVIDENCE-INTEGRITY-001` |
| S01 | mutation authorization | NONE | — |
| S02 | refresh acceptance | REQUIRED | `CTRL-EXACT-BINDING-001`, `CTRL-EVIDENCE-INTEGRITY-001` |
| S02 | software mutation | NONE | — |
| S03 | realization acceptance | REQUIRED | `CTRL-REPOSITORY-PROVENANCE-001`, `CTRL-EXACT-BINDING-001`, `CTRL-EVIDENCE-INTEGRITY-001` |
| S03 | software mutation | NONE | — |
| S04 | intention acceptance | REQUIRED | `CTRL-EXACT-BINDING-001`, `CTRL-EVIDENCE-INTEGRITY-001` |
| S04 | implementation mutation | NONE | — |
| S05 | before-mutation | REQUIRED | `CTRL-AUTHORIZATION-001`, `CTRL-EXECUTION-SAFETY-001` |
| S05 | delivery-acceptance | REQUIRED | `CTRL-REPOSITORY-PROVENANCE-001`, `CTRL-EVIDENCE-INTEGRITY-001` |
| S06 | verification-acceptance | REQUIRED | `CTRL-REPOSITORY-PROVENANCE-001`, `CTRL-EXACT-BINDING-001`, `CTRL-INDEPENDENT-EVALUATION-001`, `CTRL-EVIDENCE-INTEGRITY-001` |
| S06 | product mutation | NONE | — |
| MISSION | closure after S06 | REQUIRED | `CTRL-FINDING-RESOLUTION-001` |

No S05/S06-specific runtime behavior is added by this matrix. Runtime execution
must consume the frozen bindings and fail closed on `UNSATISFIED` or
`INCONCLUSIVE`.

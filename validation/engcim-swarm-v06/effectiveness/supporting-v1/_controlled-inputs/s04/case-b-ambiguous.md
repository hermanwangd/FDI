# S04 Case B — deliberately ambiguous intent

Make Chart Viewer easier to use and make 404 easier to recover from.

The first result must classify exactly two blockers:

- Q1: the measurable interaction target is missing;
- Q2: whether `retryable=false` should change is unresolved.

No PM decision may be invented. The initial result must be
`WAITING_FOR_INPUT` with `implementationAuthorized=false`.

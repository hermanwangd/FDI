# Exact Binding Regression

## Result

`PASS` in the local evaluator suite.

The evaluator compares the current subject identity and revision with the
bound evidence. It rejects an R1 binding when the current candidate is R2 and
returns `STALE_BINDING`. It rejects a different subject identity with
`SUBJECT_MISMATCH`. A matching subject and revision returns `SATISFIED`.

Finding Resolution does not repeat these comparisons. It receives the actual
Exact Binding `EngineeringControlResult` through its evidence envelope.

# Independent Evaluation Regression

## Result

`PASS` in the local evaluator suite.

The positive contract accepts an independent evaluator resolving the exact
subject even when that evaluation outcome is `FAIL`; the negative contract
rejects producer-only evaluation with `EVALUATOR_NOT_INDEPENDENT`.

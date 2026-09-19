# B1 Actual Control Conformance

## Result

`PASS` for the implemented local v0.3 evaluator.

The test does not pass expected output into the evaluator. For each fixture it
loads input JSON, invokes the real `EngineeringControlEvaluator`, serializes
the returned `EngineeringControlResult`, and only then loads expected JSON for
an independent assertion.

Command:

```text
MAVEN_OPTS='-Xmx2g' ./mvnw -q \
  -Dengcim.observed.dir=validation/engcim-swarm-rc7b-v03/evidence/b1-controls \
  -Dtest=EngineeringControlFixtureConformanceTests,EngineeringControlEvaluatorTests,EngineeringControlCliTests test
```

Observed result: exit code `0` on 2026-09-19.

## Fixture cases

| Case | Actual evaluator path | Expected outcome |
|---|---|---|
| exact-binding-fresh | Exact Binding | SATISFIED |
| exact-binding-stale | Exact Binding | UNSATISFIED / STALE_BINDING |
| evidence-integrity-valid | Evidence Integrity | SATISFIED |
| independent-evaluation-producer-only | Independent Evaluation | UNSATISFIED / EVALUATOR_NOT_INDEPENDENT |
| execution-safety-protected-force-push | Execution Safety | UNSATISFIED / PROTECTED_BRANCH_MUTATION |
| finding-resolution-fresh | Exact Binding, then Finding Resolution | SATISFIED |
| finding-resolution-stale | Exact Binding, then Finding Resolution | UNSATISFIED / STALE_RESOLUTION_EVIDENCE |

The two Finding Resolution cases prove the adapter obtains the binding outcome
from the actual Exact Binding evaluator. No expected result is used as input.

## Contract checks

- Result outcomes are exactly `SATISFIED`, `UNSATISFIED`, and `INCONCLUSIVE`.
- Required controls fail closed when their subject/evidence is missing.
- The v0.3 catalog contains the seven specified control references only.
- Scenario definition contains only the specified contract fields and no runtime
  lifecycle fields.
- CLI output is an actual serialized evaluator result and uses create-new output
  semantics to avoid silently replacing evidence.

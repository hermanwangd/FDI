# Improvement actions — implementation and verification

## Measured forward result

| Metric | 007 / BOXING-001 | BOXING-002 |
|---|---:|---:|
| TP / FP / FN | 28 / 5 / 12 | 31 / 5 / 9 |
| Precision | 84.8485% | 86.1111% |
| Recall | 70% | 77.5% |
| F1 | 76.7123% | 81.5789% |
| Candidate pairs / gold candidates | 59 / 28 | 65 / 31 |
| Candidate precision | 47.4576% | 47.6923% |
| Gold-candidate retention | 100% | 100% |

Same frozen source, five producer inputs, evaluator truth and official scoring
policy. Three new method/edge relationships were independently source-adjudicated;
no old proposal was removed. Both evaluator commands succeeded on their first
attempt in this namespace. BOXING-001's negative result and failed input attempt
remain preserved separately, not overwritten or hidden.

## Implemented actions and direct evidence

1. Candidate tracing: opt-in sealed sidecar with seed/call/filter/unresolved
   events and honest depth frontier. The first real trace run was proposal-equal
   to 007; see ../candidate-trace-001/producer/generation.json.
2. Candidate-stage evaluator: digest-bound Java CLI, exact-pair deduplication,
   proof-reference matching, candidate/final precision and recall, retention,
   null-with-reason zero denominators and conservative exclusive FN diagnosis.
   See evaluator/stages/aggregate.json and CandidateStage*Tests.
3. Evidence-led correction: opt-in primitive boxing plus verified bootstrap-JDK
   ancestry. Strict-match defaults and earlier experiment modes are preserved.
   Competing overloads, Object methods, unknown external ancestry and varargs
   remain conservative; no speculative method-name/arity matching.
4. Reverse evaluation fixtures: eight independent input/rubric cases under
   src/test/resources/reverse-quality, exercised by ReverseQualityMatrixTests.
   They cover creation, refactor-only, misleading parent resource, composite
   delivery, repeated delivery, denial, conflicting history and non-HTTP entry.
   Rubric expectations never enter the producer evidence bundle.
5. Verification: final Java 17 Maven package passed 1431 tests in 122 suites,
   zero failures/errors/skips; git diff --check passed. Independent code and
   receipt reviews passed after remediation of runtime-digest validation and
   implicit Object-overload findings. Test-only reverse additions are bound to
   db2765d5df6a0ddd192fbbf3ec4aff9c4410a73b.

## Review and exact identities

Boxing source candidate: 7659216f43fa3cea6a7e3f952199e3f77e2c2369.
Evaluator code candidate: 4b52e4366f4302bd26c1b454c76cf8831ecc3006.
Independent source adjudicator: /root/trace_review.
Independent code/receipt reviewer: /root/evaluator_review.
The reviewer independently checked exact clean source revision, the three new
relationships, hashes, proof bindings and arithmetic without rerunning the
scorer. Producer did not read private evaluator pairs or proof-review files.

- Source: 818c4136ea971c21674525f9053de0d9c7ad8cfe
- Runtime: 33201a0d159507d1f7d339b96148fdd468722b6be038f6c07880af71911193ab
- Generation: 7ccf8c5b2d3b9659c3ec316c239be42fd2789d63a2550d336bd298eda73cdd8e
- Manifest: 31bc579b5a20ab76b08380c0810cf63b8f2f43e4a44ff1836c96d3f94988056e
- Comparison: 5b16acc9016aaac6c0337ade5936cf5a61dbd530080ed5f5d8ce6210b90e1490
- Receipt: aeec57e6280cfc69d6c6f46c8827714573a970b079978f56600b760f9219dede

Exact JAR retained outside the worktree:
/Users/herman_mbp2023/ClawProjects/skills/Software-Factory/.fdi-work/retained-runtimes/candidate-improvements/33201a0d159507d1f7d339b96148fdd468722b6be038f6c07880af71911193ab.jar

Runnable producer entry point: JdkBoxingCalibrationRun (same Java package as
MethodCalibrationRun), invoked through PropertiesLauncher with exactly
<sealed-five-input-root> <exact-clean-source-root> <new-output-root>.
See producer/generation.json and evaluator/adjudication-receipt.json for sealed
inputs and exact evaluation commands. Never reuse an existing output directory.

## Remaining limits — not success claims

- Exposed Petclinic calibration only; no holdout or generalization claim.
- Nine FN remain: separate source review identifies six external/generic-ancestor
  call-resolution gaps and three template/property-rendering paths. Trace alone
  still honestly reports UNKNOWN=9; source adjudication is distinct evidence.
- Complete defined chains remain 2/9 with one missing definition; overall chain
  coverage is unavailable, assessment remains INCONCLUSIVE, not formal GO.
- Static calls are not observed execution or persistence.
- Reverse fixtures characterize mechanical proposal behavior, not semantic
  accuracy or calibrated confidence. Conflict detection and interpretation of
  delivery-history meaning are not implemented by this test matrix.
- The improvement is opt-in in this isolated branch. No canonical merge, push,
  deployment, semantic publication, holdout selection or parent closure.
- Runtime heaps were bounded (Maven/fork2GB, producer1GB, evaluator512MB);
  no comprehensive token/cycle-time or whole-system RSS measurement is claimed.

The implementation actions above are ready for Feature Delivery intake.
Remaining coverage limitations must not be hidden by changing gold or thresholds.

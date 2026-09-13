# FDP reconciliation and zero-proposal diagnosis

Execution: SF-BL-005-CROSSREPO-REALWORLD-003.

## Reconciliation

FDP does not accept the returned receipt PASS as envelope compliance. State:
NEEDS_RECONCILIATION / PLAN_CHANGE_REQUIRED. No rerun or implementation is selected.
The reported threshold recommendation REVISE is retained as diagnostic evidence;
formal experiment acceptance is withheld.

Confirmed from current Git artifacts and execution records:
- Stage 2 artifacts reside under .fdi-work/realworld-ee17e31 instead of the Plan-owned validation namespace.
- Execution records explicitly describe scoring re-execution after artifact loss, contradicting exactly-once scoring.
- The replacement receipt is receipt-003.md, outside the Plan-owned receipt-002.md path.
- Records report Java 23 for scoring against the Java 17 requirement; runtime binaries were not retained. This is reported runtime evidence, not a locally reproduced JVM observation.
- Original producer/scorer JAR bytes were lost. Source hashes alone cannot verify those exact binaries.
- Receipt PASS does not override these boundaries; preserving an earlier receipt was correct, but changing the output namespace required FDP replanning.
- The scorer itself says SCORING_MECHANICS_ONLY and experimentDecision NOT_RUN.

Diagnostic metrics independently read from exact comparison bytes: baseline TP=0 FP=1 FN=44; improved TP=0 FP=0 FN=44. Recall=0 for both; improved precision=null. Both chainCoverage values are null because two chain definitions are missing; do not report complete chain coverage as a measured zero. Mandatory-null metric versus threshold-failure precedence is not explicit enough to accept the coordinator's unilateral precedence decision.

## Zero-proposal causal chain

The exact selected-evidence artifact is an empty array. Improved has zero methods,
zero edges and all ten scenarios unresolved. MethodCalibrationRun passes selected
seeds to QualifiedCalibrationProducer; its claim expansion loop requires seeds.
Thus the immediate failure is upstream evidence qualification, before METHOD
expansion and before scoring.

ScenarioEvidenceSelector.qualifies requires an andExpect request assertion chain
and isOk/isCreated/redirect assertions. The RealWorld ArticleApiTest example uses
REST Assured given().when().put(...).then().statusCode(200).body(...), with no
andExpect. The observation artifact labels these REST_TEMPLATE_CALL; that label
must not be confused with the actual REST Assured assertion API. This example
cannot qualify under the current selector. Further filters use controller-name
entity tokens and a narrow action-to-method-name mapping. Their individual loss
counts have not been measured; do not claim the assertion mismatch is the sole
cause for every scenario.

## Recommendation (proposal only)

First define durable artifact handoff before worktree cleanup, exact runtime
retention, and explicit scoring recovery/new-output rules in a revised envelope.
Then select a bounded Java diagnostic slice recording each selector rejection
reason and add a REST Assured request-bound assertion adapter with negative cases
(no assertion borrowing, wrong status, multiple requests). Do not relax scoring,
classify unsupported actions, or use evaluator expected pairs to tune selection.
No new Python framework code is needed. Do not rerun calibration in this intake.

## Exact evidence inspected

- `5f07244f3c201563949b9f44992c6e1113117146:validation/software-factory/sf-bl005/cross-repo-realworld-001/generation-realworld-002/selected-evidence.json` SHA-256 `5984eac0c5c6d947241e29dd5671b81a1546cedf77e08d38438ac47029969afa`
- `5f07244f3c201563949b9f44992c6e1113117146:validation/software-factory/sf-bl005/cross-repo-realworld-001/generation-realworld-002/improved.json` SHA-256 `5ef7c7816b02f82e0e478205c8bebd39005f55cbd0a83402306f9be3191618dc`
- `c9f669cf32a1c9f843d9c6e0fc438ecba8d9ecde:.fdi-work/realworld-ee17e31/comparison-002.json` SHA-256 `f2fdc7a42f037e2683763915f25d2656b32c88377108c13e5e4e188ea39c357d`

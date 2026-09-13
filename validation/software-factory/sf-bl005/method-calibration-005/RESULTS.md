# Real METHOD calibration: improvement, not acceptance

The new Java producer and METHOD evaluator were connected and executed against
real Petclinic source. Bounded source-call expansion recovered more correct
scenario-method pairs, but quality remains below acceptance. Do not enable the
expansion as an accepted default or claim a successful formal experiment.

## Comparable result

Both columns use the same newly sealed METHOD truth, source revision, inputs,
runtime JAR and independent proof rules. The unit is **scenario + exact source
revision + production path + qualified method signature**. There are 40 sealed
necessary pairs across ten selected scenarios. These figures must not be
compared directly with the old mixed-component denominator of 24.
Recall is recovery of those 40 defined pairs, not an estimate of missing gold or
unseen repositories; the unavailable scenario-chain remains a separate blocker.

| Metric | Baseline: route/direct seeds | Improved: seeds + bounded calls |
|---|---:|---:|
| Proposed scenario-method pairs | 10 | 19 |
| TP / FP / FN | 5 / 5 / 35 | 11 / 8 / 29 |
| Precision | 50.0% | 57.9% |
| Recall | 12.5% | 27.5% |
| F1 | 20.0% | 37.3% |
| Scenario coverage | 5/10 = 50.0% | 7/10 = 70.0% |
| Overall complete-chain coverage | unavailable | unavailable |
| Complete chains among nine defined chains, diagnostic only | 0/9 | 0/9 |
| Missing chain definition | 1 | 1 |
| Explicit abstentions / duplicate pairs / TYPE claims | 1 / 0 / 0 | 1 / 0 / 0 |

Net: six additional true positives and three additional false positives.
Recall increased 15 percentage points; F1 increased about 17.3 points.
The nine additional candidates are not nine additional correct results.

## Decision

- **Calibration assessment: INCONCLUSIVE** under the frozen mandatory-metric
  rule, because overall chain coverage is undefined. The missing definition
  cannot be replaced with an empty chain or silently dropped.
- **Engineering recommendation: REVISE.** Precision 57.9% is below 70%, even
  though all available comparative metrics improve. No defined full chain is
  recovered. This is not an acceptable default yet.
- **Formal experiment: NOT_RUN.** Petclinic is exposed calibration, not a
  Human-selected unseen holdout. The unchanged scorer intentionally reports
  SCORING_MECHANICS_ONLY; separate evidence establishes the bounded invocation
  and reviews, not formal readiness or Product truth.

## What the failures mean

The eight improved-arm false positives comprise:

1. Five seed claims whose exact cited tests do not support the selected scenario
   or select the wrong behavior path. Examples include an empty-query test used
   for surname/whitespace behavior, a validation-error test used for successful
   visit creation, and an unpaged endpoint used for a paginated scenario.
2. Two exception-handler helpers proposed for successful pet creation/update.
   A call exists in source, but it is on the wrong branch for those scenarios.
3. One source-supported helper outside the independently sealed necessary pair
   set. Evidence support alone does not make a claim a true positive.

The unavailable chain concerns accepted behavior not established at this source
revision; it remains unavailable, not an extractor success. Detailed per-claim
source/test citations are in `evaluator/proof-review.md`. Twenty-nine expected
pairs remain without valid supported proposals. This report does not guess that
all are recoverable parser gaps.

## Next improvement

Prioritize **scenario-specific evidence selection and branch qualification**,
not wider call expansion. Preserve all alternative test references instead of
choosing one unsuitable seed reference; retain valid/success versus error-path
distinctions when traversing calls. Evaluate any change in a new immutable
calibration run. No code was tuned after seeing these scored misses.

In this run each seed component already carried only one reference. The observed
evidence-selection weakness is upstream of the new adapter, not proof that its
first-reference conversion discarded a better reference in this particular run.

Complete-chain availability must be resolved independently from accepted
semantics/source applicability; do not alter Acceptance Criteria or invent gold
to make a metric available. A formal holdout still needs its own selection and
freeze gates. Parent SF-BL-005 remains IN_PROGRESS; no automatic closure.

## Evidence and reproducibility

- Code candidate: `6bd20ec64442a73f56bfc27e8106b377e2014c6f`.
- Source: `818c4136ea971c21674525f9053de0d9c7ad8cfe`, clean before/after.
- Runtime SHA256: `2fb7fccb31523f2f95b5c79af1bd9e191ebfef3a2b155b00e4eee59de713b762`.
- Comparison manifest SHA256: `f7f5d9433d1b36ef5918e9dde53cf4043cbdc294a46aa4f4c6e7949d2b787522`.
- Comparison output SHA256: `01ee7114c612033211d27d03b19427d5df06fbc5fa0b243b127734105391bfbf`.
- Independent gold author, gold reviewer, producer reviewer and proof review are
  identified in the adjacent sealed artifacts. Gold predates producer execution.
- Producer process completed at 2026-09-12T07:56:41.221842Z; evaluator ran only
  after both output digests and the independent proof ledger were sealed.
- Java package: 111 suites, 1361 tests, zero failures/errors/skips; focused new
  tests: 12. Python controls: 63/63. Existing-output probe refused OUTPUT_EXISTS;
  producer hashes stayed unchanged. Old validation and source files unchanged.

Graphify's existing snapshot is verified/bound, not reindexed; the new expansion
uses Java AST, not Graphify edges. No upstream application tests or live database
calls ran. Structural edges are not evidence of observed execution.

## Bounded workflow observations

- Producer pair generation: about 2.20 seconds, excluding build and reviews.
- Independent code review: initial REVISE, remediation REVISE, final PASS;
  first-review pass = false, two remediation commits. Do not report this as a
  first-pass success.
- Token/tool cost and total end-to-end cycle time were not comprehensively
  instrumented; no cost-reduction claim is made.
- Complete regression passed on the final candidate. Earlier complete runs
  during review were superseded; final evidence does not combine stale counts.

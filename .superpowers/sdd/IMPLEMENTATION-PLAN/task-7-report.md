# Task 7 Report — PKB-001 Bounded Prototype Evaluation

## Status and decision

COMPLETE — both 15-item reviewer judgment files validated before unblinding as complete, packet-bound, isolated, `NON_HUMAN`, and `EVALUATOR_ONLY`. Packet/key/run/gold/seal integrity passed. The bounded prototype decision is `REVISE`; numeric acceptance thresholds were not pre-registered before generation and judgment, so observed metrics were not backfit into a `GO` rule. Human Product Team review and semantic publication remain pending.

## TDD evidence

- RED: `python3 -m pytest -q tests/test_pkb001_task7_evaluation.py` failed 8/8 because `tooling/validation/pkb001_task7_evaluate.py` did not exist.
- RED mutation increment: the reviewer packet-copy mutation test failed because the first evaluator implementation did not yet bind workspace packet bytes.
- GREEN: Task 7 evaluator tests passed 9/9 after adding pre-unblind workspace packet validation.
- GREEN integration: Task 6/7, status, and evaluator-seal suites passed 43/43.
- GREEN full Python suite: `python3 -m pytest -q` passed 112/112.

## Evaluation artifacts

- `validation/pkb001/task7-evaluation/evaluation-report.json`
- `validation/pkb001/task7-evaluation/third-review-pending.json`
- `validation/pkb001/task7-evaluation/public-validation-report.json`
- `validation/pkb001/task7-evaluation/public_validate.py`
- `tooling/validation/pkb001_task7_evaluate.py`
- `validation/pkb001/schemas/pkb001-task7-evaluation-v1.schema.json`

The public validator passed 8/8 checks. The report binds SHA-256 digests for the blind packet, sealed key, both judgment files, both source run artifacts/manifests/witnesses, evaluator gold, and ground-truth seal. Mutation tests fail closed for packet, reviewer packet copy, gold, and forward-run changes. Protected source outputs, judgments, Product Semantics, evaluator gold, and seal were not modified.

## Metrics summary

Coverage is 15/15 items judged by both reviewers, or 30/30 complete judgments.

| Metric | Forward | Reverse | Overall |
|---|---:|---:|---:|
| Evidence-validity mean | 0.9040 | 0.8760 | 0.8947 |
| Usefulness mean | 0.9055 | 0.8270 | 0.8793 |
| Precision mean | 0.8975 | 0.7200 | 0.8383 |
| Judgments with unsupported claims | 19/20 | 10/10 | 29/30 |
| Unsupported-claim strings | 20 | 15 | 35 |
| Total review seconds | 796 | 562 | 1,358 |
| Median combined seconds per item | 79.5 | 107 | 80 |

Overall actions: `ACCEPT` 17, `ADD_MISSING` 2, `MERGE` 2, `RENAME` 5, `REJECT` 0, `SPLIT` 4. Overall outcomes: `SUPPORTED` 8, `PARTIALLY_SUPPORTED` 20, `UNSUPPORTED` 0, `DUPLICATE` 2.

Reviewer action agreement is 12/15 (80%); outcome agreement is 7/15 (46.7%); exact action-and-outcome agreement is 4/15 (26.7%). Three action disagreements and eight outcome disagreements produce an 11-item pending independent third-review list. No third judgment was fabricated.

Forward comparison covers 9 mapping proposals plus 1 unresolved capability against 10 evaluator-only expected realizations. The run proposed 25 file-level graph components against 24 expected method/class components: exact graph-node matches are 0/24, expected-component source-path recall is 23/24 (95.8%), and proposed-component expected-path precision is 21/25 (84.0%). Path overlap is explicitly not relabeled as an exact graph-node match. All five reverse proposals have per-reviewer results in the evaluation report.

## Deferred Task 5 audit minors

Resolved without changing the reverse source output or provenance witness. The existing validator explicitly checks the non-access booleans as attestations, and `validation/pkb001/reverse-task5-pkb001_reverse_run/public-validation-transcript.txt` now records a fresh exit 0, identical stdout/report SHA-256, byte-comparison exit 0, and 24/24 passing checks.

## Verification and limits

- Task 6 public validator: PASS.
- Task 7 public validator: PASS.
- Task 5 fresh validator/report byte comparison: PASS.
- Python compile/JSON parse/diff checks: PASS.
- In-place Maven: blocked by the pre-existing untracked `GraphifyBindingEvidence 2.java`, which the task requires preserving.
- Temporary verification copy excluding only that duplicate: `MAVEN_OPTS='-Xmx2g' ./mvnw -f <temporary-copy>/pom.xml test` passed 13/13 Java tests. All commands stayed below the 8 GB limit.

Isolation and forbidden-input non-access remain attestations plus contract/digest evidence, not cryptographic proof of evaluator context. Numeric observations are descriptive only. Human Product Team review must decide Product meaning, and no semantic publication is authorized.

## Commit

Primary implementation and evaluation commit: `82fa351` (`feat(pkb001): issue bounded prototype evaluation`). This report is committed separately so it can cite the immutable primary commit without a self-referential hash.

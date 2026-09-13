# Independent METHOD-QUALITY-007 receipt review

Reviewer actor/run: `/root/calibration_code_review`, separate from producer,
integrator and proof-ledger author `/root/calibration_evaluator_review`.
Candidate: `4d3584aa1a74987e204b63d3556c18dd58157a9d`.
Base: `7f099229d51a7f8f0bd7bae325bbad52f009f7f9`.
Verdict: **PASS — strict metric-goal completion proved on the bounded calibration**.

## Independently verified evidence

All comparison-manifest artifact hashes match: baseline, improved, truth and
proofs. The manifest digest matches comparison.json; its embedded manifest is
identical. Execution-seal generation, producer and runtime hashes agree with
the actual artifacts. All five input files and all seven outputs listed by
generation.json match their recorded hashes. Both arms, generation and comparison
share source revision, input snapshot and extractor binding.

Truth was hashed without inspecting raw gold or missing-pair lists. Its digest
equals the original 005 truth. The 007 baseline proposals are structurally equal
to 005 improved proposals; runtime binding differs as expected between builds.
The comparison retains ten scenarios and TP+FN=40 in both arms. The independent
proof-review report confirms the original denominator and fresh current-reference
adjudication; this receipt does not repeat that source-proof review.

The current source checkout has exact HEAD
`818c4136ea971c21674525f9053de0d9c7ad8cfe` and empty Git status. Old 005/006 run
paths and methodpair scorer sources have no diff from the base. Current Java
source/test files have no diff from the candidate. The two changed production
hashes match the independently authored code-review receipt. Historical clean
before/after, overwrite refusal and process order are recorded by the execution
seal and independent proof review; present-state checks do not independently
reconstruct those past events.

Actual Surefire XML totals are 115 suites, 1382 tests, zero failures/errors/skips,
matching the seal. Python 63-pass status was checked against the execution seal;
Python was not rerun by this reviewer and no separate Python result log was
independently authenticated here.

## Independent scorer replay and arithmetic

Executed the unchanged existing JAR using Java 17, `-Xmx512m`, the pinned manifest
and a new temporary output `/tmp/sf007-receipt.b7S0Gk/comparison.json`. Exit was 0;
the replay is byte-identical to the sealed comparison. No producer generation,
Maven, source edit or evaluator-ledger mutation was performed.

Improved TP/FP/FN is **28/5/12**. Precision is **28/33 = 84.8484848...%** and
recall **28/40 = 70%**. Strict comparisons hold without rounding: 28*5 > 33*4
and 28*5 > 40*3. F1=56/73, scenario coverage=9/10. All serialized ratios were
recomputed and agree. Twenty-one duplicate references do not multiply unique TP.
RESULTS.md correctly distinguishes the separately sealed 006 column from the
two current arms and reports the strict goal as met only on exposed calibration.

Both overall chainCoverage values remain null. Improved 2/9 complete defined
chains is diagnostic; one missing definition is not dropped or invented. Thus
mandatory-metric calibration assessment remains INCONCLUSIVE and formal experiment
NOT_RUN, despite the requested precision/recall goal passing. Parent closure,
formal GO, Product truth and unseen-data generalization are not established.

## Exact reviewed artifact hashes

Paths below are relative to this 007 directory unless specified otherwise.

| Artifact | SHA256 |
|---|---|
| RESULTS.md | `959237f8525a05588e7322fa687ccdc8c254e9237fd954443458b9e13382d3df` |
| execution-seal.json | `599ba40cafd94ff043b4a1e4f916aaa89b848eac02168fcd93e2082806cecd7e` |
| producer/generation.json | `9c501e78be3518da34043c0c1f891ee66f1bd14fe5f63c1615c90b060b3c4372` |
| producer/baseline.json | `5812efb8b316a604b7298e8f9d0f8fd0d0993d25567c4632c30a8c13b3c23eee` |
| producer/improved.json | `32b447696d0ac98490127755225dd3825ebe90a27580f03dddb75f5f5455ba3a` |
| comparison-manifest.json | `82bcadffddb64713f116eea6bd4f96800f769f050bc20bf7be7ba412d2aadf38` |
| comparison.json | `ed363f33f14286f3f6fa66a894d9473bd97d8ddf03f5b22e1fce8f0488d48114` |
| evaluator/truth.json | `39aa27459305c05a4c983b0c414c60b39ec3e460491b9edb5da49c620bc2de2c` |
| evaluator/proofs.json | `aa33ddf36acaaa2a0e39788727ec27066b58aeee7e0a3fe701858c3deb444d5c` |
| evaluator/proof-review.md | `090256ee68a139e0c7873b61a5732aab763773f792562eb906746fe70ca81a30` |
| code-review.md | `18325c2bdbafaa44a5ae4ea6e63bf9b841f2cd9eca28008a4ccc56c3ac40f61c` |
| Repository target/fdi-0.4.8.3.jar | `ea9d7f2e440ef2642f7c641201ea0be1c6637b9c02be3fd484b0c3ab5f7306dd` |

No blocking receipt findings. The Feature Delivery Plane may reconcile the strict
calibration metric goal as achieved while retaining the stated chain, holdout,
authority and parent-status limits. Only this receipt and the authorized temporary
scorer output were written by this review; prior artifacts remain unchanged.

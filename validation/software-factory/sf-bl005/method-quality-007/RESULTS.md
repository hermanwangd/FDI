# METHOD quality calibration 007

The strict user metric goal is met on this fixed exposed Petclinic calibration:
recall 28/40 = 70% >60%, precision 28/33 = 84.8484848% >80%.
This is not formal holdout acceptance, Product truth, or parent closure.

| Metric | 005 expanded baseline | 006 qualified producer | 007 update-path correction |
|---|---:|---:|---:|
| TP / FP / FN | 11 / 8 / 29 | 28 / 7 / 12 | 28 / 5 / 12 |
| Precision | 57.89% | 80.00% | 84.85% |
| Recall | 27.50% | 70.00% | 70.00% |
| F1 | 37.29% | 74.67% | 76.71% |
| Scenario coverage | 7/10 | 9/10 | 9/10 |
| Defined complete chains (diagnostic) | 0/9 | 2/9 | 2/9 |

The 007 baseline independently re-executes the unchanged 005 expanded producer;
its proposal content exactly matches 005. The 006 column comes from that sealed
run, not an unexecuted third arm. Each run uses the same 40-pair truth, ten
scenarios, source revision and scorer. Runtime bindings are recorded per run.

## Meaning and limits

The producer now rejects an unproved absent-target creation fallback for a
successful existing-target update. This removed two false pairs without losing
any true pair. The rule is source/type/branch based, not a scenario-ID or gold
lookup. All emitted candidates remained in scoring. The remaining five FP are
supported auxiliary guard/identity/presentation calls outside the necessary
pair set; twelve expected pairs remain unrecovered. No claim of perfect mapping.

Overall chain coverage is still unavailable: one required definition is missing;
2/9 is diagnostic only, not the overall metric. The calibration's mandatory-metric
assessment therefore remains INCONCLUSIVE and formal experiment NOT_RUN.
Metric-goal PASS does not change Acceptance Criteria or authorize formal GO.
An unseen Human-selected holdout and other parent gates remain separate.

This is repeated exposed calibration with failure-category-driven generic
corrections. Main did not inspect raw evaluator gold or missing-pair lists.
An independent evaluator judged current exact references after output sealing;
structural calls/redirect associations are not observed execution or persistence.
Graphify's previously verified snapshot was reused, not freshly indexed.

## Verification and provenance

- Producer candidate: 4d3584aa1a74987e204b63d3556c18dd58157a9d.
- Source: 818c4136ea971c21674525f9053de0d9c7ad8cfe, clean before/after.
- Runtime SHA256: ea9d7f2e440ef2642f7c641201ea0be1c6637b9c02be3fd484b0c3ab5f7306dd.
- Truth SHA256: 39aa27459305c05a4c983b0c414c60b39ec3e460491b9edb5da49c620bc2de2c.
- Proofs SHA256: aa33ddf36acaaa2a0e39788727ec27066b58aeee7e0a3fe701858c3deb444d5c.
- Comparison SHA256: ed363f33f14286f3f6fa66a894d9473bd97d8ddf03f5b22e1fce8f0488d48114.
- Full Java: 115 suites / 1382 tests, zero failures/errors/skips; Python: 63 passed.
- Independent code and proof reviews are adjacent; final receipt review is required.
- Overwrite probe refused OUTPUT_EXISTS; producer hashes and old runs unchanged.

The scorer deduplicates scenario-method pairs; retained alternative proof
references do not multiply true positives. Generation took approximately 2.14
seconds, excluding build/review. First code review required one reassignment
remediation before PASS. Full cycle time and token cost were not comprehensively
instrumented; this report makes no workflow cost-reduction claim.

Parent SF-BL-005 remains IN_PROGRESS. No merge, push, semantic publication or
Human-only closure was performed.

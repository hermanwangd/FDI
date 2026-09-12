# Independent calibration receipt review

Reviewer actor: `/root/calibration_code_review`, independent of producer/integrator.
Execution: `SF-BL-005-METHOD-CALIBRATION-005`.
Exact reviewed code candidate: `6bd20ec64442a73f56bfc27e8106b377e2014c6f`.
Verdict: **PASS — READY_FOR_CALIBRATION_RESULT_HANDOFF**.

This is a bounded interpretation and artifact-integrity review after generation
closed. It does not independently replay generation, source-proof adjudication,
gold authorship, Java/Python verification, or process isolation. No producer code,
controls, existing evidence, or gold was edited. Gold and proof ledger bytes were
hashed without reading their content. No Maven or producer run was performed.

## Verified bindings

SHA256 values recomputed from the reviewed bytes:

| Artifact | SHA256 |
|---|---|
| RESULTS.md | `47e78186c49972cdbd14493eadd1b93ca33cdc9b8508cd0d990c51eb28073649` |
| comparison.json | `01ee7114c612033211d27d03b19427d5df06fbc5fa0b243b127734105391bfbf` |
| comparison-manifest.json | `f7f5d9433d1b36ef5918e9dde53cf4043cbdc294a46aa4f4c6e7949d2b787522` |
| execution-seal.json | `72d735bb80742932a26f1e6d50ffbfa4fc2223b1c4bfb2ae6c270fe79229360c` |
| protocol.json | `d5e0c157eed92b59eee9c30458e3260afa4812f2c49a9947c846d1439904fe30` |
| producer/generation.json | `7eb63762fb58d3e0599a0e10410da740613d36b7da73cdf15448ffa97de15961` |
| producer/baseline.json | `e79bd365a8b14ae181219b13bbca43624c49fcbb4ccebfe455afe1a4bc49c786` |
| producer/improved.json | `03e885ec096e278cf85314ac6a46f294a6e233264cb73e3b094a16d1d6b035d9` |
| evaluator/truth.json | `39aa27459305c05a4c983b0c414c60b39ec3e460491b9edb5da49c620bc2de2c` |
| evaluator/proofs.json | `4bb7bf0a6b5f08a4700ba13be57503e81c5b9b72450b384adeb4cf58d771296d` |
| evaluator/proof-review.md | `e1df2278347f90ecdf8df344d89abd561f33ccf55c0161f1f7c84080f4d99c4f` |
| evaluator/gold-review.md | `01821cec35390bceecf568e051becb8062d1266e5a9139443b5b8b8886ff983a` |
| evaluator/gold-authoring.md | `33a6f29d26d1fcf2cb36db6bb66671dcf467a9bf131dff7903edb28857b71b28` |
| target/fdi-0.4.8.3.jar (repository relative) | `2fb7fccb31523f2f95b5c79af1bd9e191ebfef3a2b155b00e4eee59de713b762` |

All five files in the protocol-bound producer input directory and all six output
files listed by generation.json match their recorded digests. The comparison's
embedded input manifest equals comparison-manifest.json; source revision,
input-snapshot digest and extractor digest agree across both arms, generation
and comparison. Producer source and synthetic tests have no diff from the exact
reviewed candidate. The execution seal explicitly supersedes protocol's prepared
31faec candidate with reviewed 6bd20ec before generation; this is recorded, not
an unexplained identity substitution.

## Arithmetic and interpretation

Recomputed precision, recall, F1 and scenario coverage agree exactly with the
serialized numbers. Baseline TP/FP/FN = 5/5/35, improved = 11/8/29; both imply
40 necessary pairs. Proposed totals are 10 and 19. Improved precision is 11/19,
recall 11/40, F1 22/59, and scenario coverage 7/10. Rounded percentages and
reported improvements in RESULTS.md are consistent. The proof-review explanation
supports the distinction between twelve proof-supported improved claims and
eleven true positives: one supported claim lies outside the necessary set.

Both arms have zero complete chains among nine defined chains and one missing
chain definition. Overall chain coverage remains null; 0/9 is clearly diagnostic,
not a substituted mandatory metric. Calibration **INCONCLUSIVE**, engineering
recommendation **REVISE**, and formal experiment **NOT_RUN** are distinguished
correctly. Precision below 0.70 independently prevents GO. The report grants no
default enablement, parent closure, observed-execution credit, Product truth,
holdout readiness, or statistical generalization.

No blocking receipt findings. Readiness applies to handing off these exact
calibration results with their limitations, not accepting the expansion's quality.

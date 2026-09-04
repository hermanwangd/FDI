# PKB-001 Final Fix Report

## Status and bounded decision

COMPLETE — all Important findings and feasible Minor findings in `final-fix-brief.md` were addressed in one final fix wave. The bounded experiment decision remains `REVISE`; Product Team human review is pending, semantic publication is prohibited, and the 11 disagreed items remain pending independent third review.

Protected Product Semantics, evaluator gold/seal, source-run outputs/witnesses, and both completed judgment files were not modified. The unrelated untracked `GraphifyBindingEvidence 2.java` duplicate remains present and untouched.

## Implemented corrections

### Deterministic label/order blinding claim boundary

- Task 6 is now described as `DETERMINISTIC_LABEL_AND_ORDER_BLINDING`, not full arm blinding.
- The manifest, reviewer instructions, public validator/report, evaluator schema, Task 6/7 reports, Task 7 evaluation proof limits, and all four active-truth entries disclose `ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT`.
- Explicit arm labels/source identifiers remain absent and the sealed identity key remains bound and useful, but no content-level arm-anonymity claim remains.
- The existing packet and its reviewer-bound digest were preserved. Qualification is attached to that exact packet SHA in the manifest and reviewer-facing instructions so completed judgments did not need rewriting.

### Task 6 overwrite prevention

- Generation inspects both reviewer workspaces before building or writing any artifact.
- If either workspace contains judgments, library use raises `BindingError`; the CLI exits `2` with a concise error and no traceback.
- A new experiment/version must use a distinct explicit `--output-dir`; regression coverage proves this initializes empty workspaces while the current 30 judgments remain byte-identical.

### Task 7 fail-closed STOP artifacts

- Pre-unblinding completeness/isolation failures and bound-input integrity failures now return a persistable `pkb001.task7.stop-report.v1` decision.
- STOP reports contain stage-specific reason(s), documented exit code `2`, `unblinding_performed:false`, `metrics_computed:false`, and `semantic_publication_allowed:false`.
- The CLI persists the STOP report, does not emit a third-review packet, prints the artifact, and exits nonzero.
- Key loading was moved after packet/workspace/source/gold/seal digest and binding validation. Mutation coverage for packet, reviewer packet copy, gold, and forward source output now asserts STOP artifacts instead of exceptions.
- The successful committed evaluation remains deterministic `REVISE`.

### Forward construct-validity metrics

The committed Task 7 report now separates three granularities:

- File-component path comparison: expected-component path recall `23/24` (`0.9583333333`); proposed-component path precision `21/25` (`0.84`).
- Expected graph-node coverage across each proposal's full cited structural set (`proposed_components` plus `evidence_refs`): `17/24` (`0.7083333333`).
- Exact proposed-component graph-node matching: `0/24` (`0.0`).

The report and validator explicitly state that path overlap is not exact graph-node matching and evidence citation coverage is not a proposed-component match.

### Reproducible tests and Minor findings

- `.fdi-work` is excluded from pytest discovery.
- The three tests that genuinely require ignored Petclinic/Graphify prerequisites are explicitly skipped when those prerequisites are absent; tracked frozen-evidence tests continue to run.
- Frozen Task 4/5/6 briefs referenced by committed source manifests, plus the SDD ledger, are now tracked so digest validation works from a clean checkout.
- The clean tracked-copy regression runs the default suite without `.fdi-work` and passes.
- Missing/damaged Graphify package imports (`PackageNotFoundError` and `ModuleNotFoundError`) are translated to persisted `NOT_BOUND` evidence with exit `2`.
- Graph node paths are documented as repository-relative; extraction/runtime link provenance may retain checkout-specific absolute paths. Frozen graph bytes were not cascade-normalized.
- The Task 5 ledger/report now mark both auditability minors resolved.
- Task 7 Markdown includes unsupported-claim judgment rates: forward `19/20 (0.9500)`, reverse `10/10 (1.0000)`, overall `29/30 (0.9667)`.
- `IMPLEMENTATION-PLAN.md` now requires completed Product Team human review for `GO` and states that semantic publication is a separate explicit Product Team action.

## TDD evidence

- Initial RED command over the new Task 6/7, Graphify-import, reporting, and active-truth seams: `13 failed` for the expected missing contracts.
- Corrected Task 6 overwrite fixture RED: `1 failed, 1 passed`; the legacy implementation overwrote completed judgments in the isolated copy.
- Task 6 CLI fail-closed RED: `1 failed` because the legacy CLI returned `1` with a traceback.
- Evaluator-schema claim-boundary RED: `1 failed` because `arm_blinded:true` was still required.
- Clean tracked-copy RED exposed five missing ignored inputs, then two remaining missing Task 5 inputs; those frozen manifest dependencies were tracked rather than weakening validation.
- Focused GREEN: core final-fix seams `14 passed`; Task 6 CLI guard `1 passed`; schema/digest seam `2 passed`; clean tracked-copy regression `1 passed`.

## Fresh verification

- Full Python suite: `python3 -m pytest -q` → `121 passed in 3.87s`.
- Clean tracked-copy regression: `python3 -m pytest -q tests/test_prototype_baseline.py::test_default_python_suite_passes_in_clean_tracked_copy` → `1 passed`; its child default suite exited `0` with `.fdi-work` absent.
- Independent staged-tree archive (followed only by this report evidence update): `.fdi-work` absent; default Python suite `118 passed, 3 skipped` (only the explicitly prerequisite-dependent integration tests).
- The same clean archive ran `MAVEN_OPTS='-Xmx2g' ./mvnw test -q`: 13 tests, 0 failures, 0 errors, 0 skipped.
- Task 5 public validator: PASS, 24/24 checks.
- Task 6 public validator: PASS, 14/14 checks.
- Task 7 public validator: PASS, 9/9 checks.
- Python compilation: `python3 -m compileall -q tooling tests validation/pkb001` → exit `0`.
- JSON parse sweep: 51 PKB-001/active JSON files parsed successfully.
- Live Task 6 default-output guard: exit `2`; both judgment SHA-256 values were identical before/after.

## Protected-byte evidence

Before and after this wave, the protected SHA-256 values are:

- Product Semantics: `21cb8c2ad4cd78cba009f205dcba9dc359a0bd933ae494846539c0119bf9b1f4`
- Evaluator gold: `4d22799e4d7597e0bbc302c9db3cd0510f70cc946cb5de5909ded9c4b1b112d1`
- Ground-truth seal: `7290fd4aec80cbdd5cea52b30f9da5323e455843948746fc53208eecf6e2a55a`
- Forward artifact/manifest/witness: `bfb2d72045a350e3684464ad1bae7cbdd8c06111882e1e9f02276211b81a0992`, `271e859f1f24ace30354b7a7f3315f0db2366d067ff6d30666bc57894ae53994`, `36fb66c248d698ca2e29744bf35b8f3cadaaa5cbcd7b4ca40a97597e1be050a5`
- Reverse artifact/manifest/witness: `8037b14aae0ff1f9adbce30409ea29ba13d4d024f07dcaa3835eddcaba7cb450`, `f749bd9e02530f0d41c6af796d31f144ed424bda7263dce9bb3efb33e733e950`, `d725a7d2f25f227c38ff382da1e1d7581bd2093cd43eefd35e61530b23773f12`
- Reviewer 01 judgment: `7858befe88bd30c7f1de537b31cb51a455478acb7a1532d8e9f94c1ce2a59084`
- Reviewer 02 judgment: `5a29738eb8821a21b62e77d7f34f2ecff46af4ee554277d07025a1c154b25f73`

## Concerns and limits

- The sealed identity key is a repository artifact with a visibility contract, not encryption or cryptographic access control.
- Reviewer isolation and forbidden-input non-access remain contract/digest evidence plus attestations, not cryptographic proof of model context.
- The absolute paths retained in historical runtime/link provenance are intentionally non-portable evidence; repository-relative graph node paths are portable within the bound source tree.
- Numeric results remain descriptive because acceptance thresholds were not pre-registered. They cannot be used to backfit `GO`.
- Human Product Team review and any later semantic publication remain separate pending actions.

The commit SHA is reported in the final handoff rather than embedded here, avoiding a self-referential report commit.

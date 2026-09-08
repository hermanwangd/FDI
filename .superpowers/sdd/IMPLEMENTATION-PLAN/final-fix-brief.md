# Final fix wave — PKB-001 whole-branch review

Address all Important findings in one coherent patch:

1. Stop claiming full arm blinding. Evidence categories/values make arm inference possible. Rename/qualify packet, manifests, reports, active truth and validators as deterministic label/order blinding with `ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT`. Keep sealed identity key useful, but do not claim content-level arm anonymity.
2. Make Task 6 generation fail safely if either judgment workspace contains completed judgments. It must never overwrite 30 judgments. Add mutation/public regression coverage and an explicit initialization/versioning path if appropriate.
3. Translate Task 7 input integrity/binding/isolation validation failures into a persisted `STOP` decision artifact/report with reason(s), nonzero documented exit, no unblinding/metrics, no semantic publication. Mutation tests must assert STOP, not merely exceptions. Preserve successful current `REVISE` output.
4. Repair forward construct validity: report separate metrics at explicit common granularity. Keep file-component path precision/recall, and add expected graph-node coverage across the proposal's complete cited structural node set (proposed components + evidence refs), independently verify expected 17/24 if that is what current bytes yield. Never relabel path overlap or evidence coverage as exact proposed-component match.
5. Make full Python tests reproducible from clean tracked checkout. Tests depending on ignored Petclinic checkout/runtime must either validate from tracked frozen evidence or be explicitly classified/skipped as integration tests when prerequisites are absent. Add a clean `git archive`/copied-tree regression or equivalent proving default suite passes without `.fdi-work`.

Also resolve feasible Minor findings:

- Qualify source path portability honestly: node paths are repository-relative; link provenance may retain extraction-time absolute paths. Do not cascade-normalize graph bytes unless necessary.
- Catch damaged/missing Graphify package imports (`PackageNotFoundError`/`ModuleNotFoundError`) and persist `NOT_BOUND`; add negative test.
- Update ledger/report: Task 5 minors resolved; add explicit unsupported-claim decimal rates in Task 7 Markdown report.
- Clarify `IMPLEMENTATION-PLAN.md`: GO requires completed Product Team human review for the experiment decision; semantic publication remains a separate explicit Product Team action.

Constraints:

- Do not modify Product Semantics, sealed evaluator gold, source run outputs, or reviewer judgments.
- Current bounded decision remains `REVISE`, human review pending, publication prohibited.
- Preserve unrelated untracked `GraphifyBindingEvidence 2.java`.
- Commands below 8 GB; Maven cap 2 GB if used.
- Use TDD/mutation tests, run full Python suite and public validators, attempt clean tracked-copy verification, commit all fixes, append full report to `.superpowers/sdd/IMPLEMENTATION-PLAN/final-fix-report.md`.

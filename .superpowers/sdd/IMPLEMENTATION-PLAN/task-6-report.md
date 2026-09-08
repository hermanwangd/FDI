# Task 6 Report — Deterministic Label/Order-Blinded Comparison Inputs

## Status

COMPLETE — prepared a deterministic label/order-blinded comparison packet and two evaluator workspaces, initially empty and now preserved with completed judgments. No Product Team conclusion, Product Semantics change, evaluator gold change, or final GO / REVISE / STOP decision was created in Task 6.

## Public seams and TDD evidence

The Task 6 brief pre-agreed the public seams: packet generation from the two valid run artifacts, separately sealed key binding, and public validation of packet/workspace contracts.

- RED: `python3 -m pytest -q tests/test_pkb001_task6_blind_packet.py` initially failed 5/5 because the Task 6 artifacts did not exist.
- GREEN: `python3 -m pytest -q` passed `99 passed in 2.33s`.
- Green public validation: `python3 validation/pkb001/task6-blind-review/public_validate.py .` passed all 12 checks.

## Packet and binding

- Packet: `validation/pkb001/task6-blind-review/blind-review-packet.json`
  - SHA-256: `13451c06a6b635f6d6ea58e7cb3faa8fdfd8f9cc6c011d8fdc3dbebac395c89a`
- Sealed key: `validation/pkb001/task6-blind-review/sealed-blind-key.json`
  - SHA-256: `3f22e51bcbfbfeeef9438334daa361aadb9e1eeffca93f31bb7539887425047d`
  - Binds the exact packet SHA and is the only artifact containing source arm and source identifiers.
- Manifest: `validation/pkb001/task6-blind-review/manifest.json`
  - Verifies all source input digests, shared source commit `818c4136ea971c21674525f9053de0d9c7ad8cfe`, shared graph SHA, and reverse Delivery History SHA.

The packet contains exactly 15 blank judgment fields: 9 forward mapping proposals, 1 forward unresolved result, and 5 reverse hypotheses. The deterministic IDs are `BR-001` through `BR-015`; explicit source-arm labels and source identifiers are absent from packet-facing material.

This is deterministic label/order blinding only, not content-level arm anonymity: `ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT`. Evidence categories and values may make an arm inferable. The separately sealed identity key remains useful for post-validation evaluation, but neither the packet nor this report claims full arm blinding.

## Independent evaluator inputs

- Instructions: `validation/pkb001/task6-blind-review/reviewer-instructions.md`
- Empty workspaces:
  - `validation/pkb001/task6-blind-review/judgment-workspaces/reviewer-01/`
  - `validation/pkb001/task6-blind-review/judgment-workspaces/reviewer-02/`

Each workspace contains byte-identical packet input, an empty judgment list, the frozen action vocabulary, and a contract that denies sealed-key and other-workspace future-judgment access. Both are recorded as `NON_HUMAN`, evaluator-only contexts; they cannot complete Product Team human review. Instructions explicitly separate evaluator-only expected-realization scoring from Product Team meaning authority.

## Validator and deferred-minor coverage

`validation/pkb001/task6-blind-review/public_validate.py` checks input/source digests, proposal-only binding, all source non-access witness booleans, exact accounting, packet/key digest binding, deterministic label/order blinding, the explicit content-inference limitation, judgment vocabulary, isolation, and no fabricated judgment or decision. It bases comparison claims only on those verified artifacts and witness records.

## Commit

Primary packet implementation: `e1a1e1d5d0a358babbcb7797e57c2b75ee7d4318` (`feat(pkb001): prepare blinded comparison packet`).

## Concerns and limits

- The sealed key is a repository artifact with a strict visibility contract, not cryptographic encryption or access control. Deliver it separately from evaluator workspaces when running an actual review.
- The two packet copies are intentionally byte-identical to prove input parity; they increase repository size but are required to keep the workspaces independent.
- `NON_HUMAN` contexts may assist only with evaluator-only measurement. Human Product Team review remains pending.

## Fix round 1 — packet-schema arm inference

The original packet leaked arm identity through packet-only shape differences: forward items used string component references and object fields with a structural list plus empty delivery list, while reverse items used empty components and nested structural/delivery objects.

- RED: the new public seam test `test_task6_packet_record_shapes_do_not_uniquely_identify_source_arm` failed against that packet because a recursive type/field-population signature uniquely identified `FORWARD`.
- GREEN: every packet item now has non-empty `component_refs` and `evidence_refs` arrays of neutral records with the same fixed fields and scalar types. Reverse component records are resolved from the bound graph; the single forward unresolved item carries an `INCOMPLETE_EVIDENCE` component record instead of an empty array.
- The regenerated public validator checked a uniform record shape. The final fix correctly limits that result: uniform shape does not establish content-level arm anonymity.

The current packet and sealed-key digests are the authoritative values in the **Packet and binding** section above.

## Fix round 2 — report digest consistency

- RED: `test_task6_report_has_one_current_digest_set` failed because the current-value section still contained the superseded pre-normalization digests.
- GREEN: that section now contains the regenerated packet/key values exactly once, matching the manifest. The superseded digest values are removed rather than presented as current evidence.

## Final safety correction

Task 6 generation now refuses to touch a destination when either reviewer workspace contains judgments. Reinitialization requires a distinct explicit `--output-dir`, preserving all 30 completed judgments and the packet binding. The current packet and judgment bytes were not regenerated.

The final qualified public validator passes 14/14 checks, including deterministic label/order blinding and the explicit content-inference limitation.

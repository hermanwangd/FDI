# Task 5 Report — Isolated PK-S2 Reverse Experiment

## Status

COMPLETE — fresh PK-S2 reasoning run produced five reviewable, proposal-only Capability hypotheses. No Product Semantics, evaluator data, forward output, historical PK-S2 output, post-cutoff knowledge, or web knowledge was accessed.

## Execution

- Orchestration identity: `/root/pkb001_reverse_run`
- Fork mode: `none`
- Skill executed: `skills/pkb001/pk-s2-capability-hypothesis/SKILL.md`
- Phase0 `READY` was confirmed at `2026-09-04T17:03:20Z`, before artifact generation at `2026-09-04T17:05:25Z`.
- Source commit: `818c4136ea971c21674525f9053de0d9c7ad8cfe`
- Graph SHA-256: `e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e`
- Delivery History SHA-256: `87b70439f69bb82cee0139b65108ecce6044aff1a4d1b5ce6324933acbeab2d1`
- History cutoff: `2026-08-26T10:57:54Z`; policy `EXCLUDE_AFTER_CUTOFF`
- Exact visible-input allowlist and `forbidden_inputs_accessed:false` are recorded in the manifest and limited provenance witness.

Outputs are isolated under `validation/pkb001/reverse-task5-pkb001_reverse_run/`:

- `capability-hypotheses.json`
- `manifest.json`
- `provenance-witness.json`
- `public_validate.py`
- `public-validation-report.json`

The witness is explicitly labeled `ATTESTATION_NOT_CRYPTOGRAPHIC_PROOF` and binds the fresh orchestration identity, `fork_turns:none`, input digests, primary output digests, manifest digest, and Phase0-before-generation ordering.

## Hypotheses Summary

All five entries have stable proposal IDs, neutral value-oriented labels, exact source and graph binding, graph component and edge references, commit/PR/changed-path references, confidence rationale, limitations, and `PROPOSAL_ONLY` authority.

1. `PKS2-HYP-001` — Client record search and result browsing (`HIGH`, 0.86)
2. `PKS2-HYP-002` — Companion record identity and update safeguards (`HIGH`, 0.93)
3. `PKS2-HYP-003` — Visit date intake safeguards (`HIGH`, 0.88)
4. `PKS2-HYP-004` — Practitioner directory and specialty presentation (`MEDIUM`, 0.74)
5. `PKS2-HYP-005` — Locale-selectable presentation (`MEDIUM`, 0.79)

Ambiguous boundaries remain explicit: client/companion/visit proposals were not forced into one aggregate Capability, and practitioner presentation was not merged with locale selection.

## Validation

Command:

```text
python3 validation/pkb001/reverse-task5-pkb001_reverse_run/public_validate.py .
```

Result: `PASS` — 24/24 public-seam checks passed. The checks cover Phase0 readiness, exact source/digest binding, frozen cutoff, exact input/output isolation, forbidden-input attestation, digest integrity, fresh orchestration identity, Phase0 ordering, stable IDs, reference resolution, pre-cutoff evidence, structural/delivery convergence, proposal-only authority, neutral reviewability, and preserved ambiguity. The persisted report was diffed against a fresh validator run with no differences.

The validation deliberately does not inspect Product Semantics, evaluator truth, forward output, historical PK-S2 output, or semantic correctness.

## Commit

Primary reverse-run artifacts: `cfc9a5124ba0ce5d130db6c431bdedea5622a873` (`feat(pkb001): execute isolated PK-S2 reverse run`).

This report is committed separately so it can record the immutable primary artifact commit without a self-referential commit hash.

## Concerns and Limits

- The graph is deterministic Java AST evidence only; templates, resource bundles, database scripts, and tests contribute through frozen delivery history rather than graph nodes.
- Several component relationships lack direct cross-file graph edges, so convergence relies on exact changed-path overlap and delivery episodes as disclosed per hypothesis.
- Practitioner-directory evidence includes a broad pagination commit and maintenance-oriented PRs, hence medium confidence.
- Pull-request evidence is limited by the frozen dataset's acquisition boundary.
- The committed witness is review evidence, not cryptographic proof of context isolation; its own digest and the post-attestation validation report are excluded to avoid recursive digest dependencies.
- Human Product Team review is mandatory. No Product Semantics was modified and no final GO/REVISE/STOP decision was made.

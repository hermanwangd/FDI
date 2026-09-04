# Task 4 Report — Isolated PK-S1 Forward Experiment

## Status

Complete. A fresh PK-S1 skill execution produced ten capability-level realization proposals, one for every frozen Petclinic capability. Every result remains `PROPOSAL_ONLY`; this task made no acceptance or release decision.

## Execution method

1. Read `task-4-brief.md` first and established its input boundary.
2. Read the complete `pk-s1-product-realization` skill before opening data inputs.
3. Confirmed Phase 0 `READY` without using the readiness report as realization evidence.
4. Verified the Product Semantics and graph bytes against their pinned SHA-256 digests and confirmed the live Graphify evidence reported `EXACTLY_BOUND` for the full source commit.
5. Projected only `capability_id`, `name`, and `description` from each frozen Product Semantics capability.
6. Reasoned independently over the exactly bound graph's node IDs, labels, repository-relative source locations, and structural relations. No deterministic mapping baseline generated the result.
7. Emitted one mapping proposal per capability with component nodes, evidence nodes, confidence, reasoning, and limitations.
8. Added and ran public-seam tests that validate accounting, reference resolution, binding/digest integrity, and the exact visible-input allowlist without evaluating mapping correctness.

## Input manifest

The manifest records the exact files visible to this execution:

| Role | Repository-relative path | SHA-256 |
|---|---|---|
| Task boundary | `.superpowers/sdd/IMPLEMENTATION-PLAN/task-4-brief.md` | `666316b4c11b37c35d57e646813ff0b84aed114562a744ae946b7eb4b2bf5f22` |
| Skill instructions | `skills/pkb001/pk-s1-product-realization/SKILL.md` | `f97d4e5b13605de81ab1b149e338031feea736666dc2c6f0b7635ce9131a2ca9` |
| Frozen Product Semantics | `validation/pkb001/datasets/petclinic-product-semantics-candidate.json` | `72aaacd69f57e0ee4bbb1e9ba04d2f3211d3e73e557730cf57e5fd9988f7cbea` |
| Exactly bound graph | `validation/pkb001/artifacts/petclinic-graph-818c413.json` | `e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e` |
| Live binding evidence | `validation/pkb001/runtime/graphify-petclinic-live-evidence.json` | `fd3b6729e720e33c89c87cb987748b17ee6cc4ac1fad2c09ddbf093ab39cd5f8` |
| Readiness status only | `validation/pkb001/reports/phase0-readiness.json` | `a6bb4627120558940639d5d8c56b2626644e486dd634e8b6073500c9f4b2c50c` |

`forbidden_inputs_accessed` is recorded as `false`. No post-cutoff source or web knowledge was used.

## Frozen bindings

- Source commit: `818c4136ea971c21674525f9053de0d9c7ad8cfe`
- Graph SHA-256: `e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e`
- Product Semantics SHA-256: `72aaacd69f57e0ee4bbb1e9ba04d2f3211d3e73e557730cf57e5fd9988f7cbea`

## Outputs

- Run artifact: `validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json`
- Run artifact SHA-256: `46b2609c06abf0e5cb248cb66e0f1be402162660d63665674aace3f5b30c625e`
- Manifest: `validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-manifest.json`
- Public validation: `validation/pkb001/tests/test_pk_s1_forward_run_public.py`

The artifact accounts for all ten frozen capability IDs exactly once: ten mapping proposals and zero unresolved results. Its overall confidence is `0.86` on a 0-to-1 scale. Per-capability confidence and limitations remain explicit in the artifact.

## Validation

Command:

```text
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest validation.pkb001.tests.test_pk_s1_forward_run_public -v
```

Result: `PASS` — 4 tests passed.

Validated public seams:

- all ten frozen capabilities appear exactly once;
- every proposed component and evidence reference resolves to an exact graph node and its repository-relative source location;
- source commit and bound graph/Product Semantics digests match;
- the live evidence binding is `EXACTLY_BOUND` to the same full source commit;
- the run artifact digest matches its manifest;
- the manifest and artifact carry the exact visible-input allowlist and `forbidden_inputs_accessed: false`;
- execution kind is `SKILL_EXECUTION` and authority is `PROPOSAL_ONLY`.

The validation deliberately does not assert semantic correctness against any sealed reference.

## Commit

Implementation commit: `644c4fbcfea2cbded29d1622d8c4256b3f3d20f5` (`feat(pkb001): add isolated PK-S1 forward run`).

## Concerns and limitations

- The frozen graph is a deterministic Java AST extraction. It does not include view templates, route annotations, configuration metadata, or observed runtime behavior.
- The `Access Clinic Home` proposal has the lowest confidence (`0.64`): `WelcomeController.welcome` supports the landing entry point, but the graph cannot verify primary navigation links.
- The visit-history and veterinarian-specialty proposals rely on domain accessors plus controller entry points; presentation and ordering remain unverified because views are absent.
- Structural evidence makes these components reviewable candidates, not accepted Product truth or proof of end-to-end behavior.

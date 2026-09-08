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

## Fix round 1 — Provenance witness and capability-boundary correction

This section supersedes the original result-count, confidence, artifact-digest, validation-count, and `Access Clinic Home` statements above. The original section is retained as the execution record preceding reviewer feedback.

### Reviewer findings addressed

1. Added committed provenance witness `validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-witness.json`.
2. Changed `PET-CAP-10` from `MAPPING_PROPOSAL` to `UNRESOLVED`. The frozen graph supports `WelcomeController.welcome` as partial landing-page evidence but has no view/navigation evidence for the full primary-navigation requirement.

The corrected result distribution remains complete and non-duplicative: 10 capability IDs accounted for exactly once, with 9 mapping proposals and 1 unresolved result. Corrected overall confidence is `0.89`, reflecting confidence in the nine structural proposals and the evidence-bound unresolved decision.

### Witness bindings

The witness records:

- orchestration identity `/root/pkb001_forward_run`;
- fresh-context setting `fork_turns:none`;
- `SKILL_EXECUTION` by `AGENT_REASONING`, with `deterministic_baseline_used:false`;
- the five allowed input paths and exact SHA-256 values, plus the separately identified requirements brief;
- the forbidden input categories and `forbidden_inputs_accessed:false` attestation;
- Phase 0 `READY`, ground-truth seal commit `e900548ec92ecfa02b8617e3af688ad678f9acc5`, and local ordering markers;
- generation material-output start marker `2026-09-04T16:48:54Z`, 430 seconds after the Phase 0 seal commit time `2026-09-04T16:41:44Z`;
- corrected run-artifact and manifest paths and byte digests.

The generation-start marker is the filesystem birth time of the first run artifact. The witness explicitly labels its assurance as `ATTESTATION_NOT_CRYPTOGRAPHIC_PROOF`: it binds the recorded statement and digests, but does not cryptographically prove non-access, agent freshness, internal reasoning, or trusted time.

### RED/GREEN evidence

RED:

```text
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest validation.pkb001.tests.test_pk_s1_forward_run_public -v
FAILED (errors=1): provenance witness did not exist.
```

An intermediate run then failed the new digest-binding assertion because the witness contained a mistyped graph SHA. This confirmed the test rejected an incorrect witness binding.

GREEN:

```text
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest validation.pkb001.tests.test_pk_s1_forward_run_public -v
Ran 5 tests in 0.003s
OK
```

The fifth test validates witness identity, fresh-context setting, skill execution mode, deterministic-baseline exclusion, exact allowed-input bindings, forbidden-category attestation, Phase 0 ordering, output paths/digests, and limited assurance scope. Public validation still does not assert evaluator correctness.

### Corrected digests

- Run artifact: `bfb2d72045a350e3684464ad1bae7cbdd8c06111882e1e9f02276211b81a0992`
- Manifest: `271e859f1f24ace30354b7a7f3315f0db2366d067ff6d30666bc57894ae53994`
- Provenance witness: `36fb66c248d698ca2e29744bf35b8f3cadaaa5cbcd7b4ca40a97597e1be050a5`

### Fix commit

Implementation fix commit: `b8d625046217c9e0cd71df28e4402eb2aad46685` (`fix(pkb001): add PK-S1 provenance witness`).

### Remaining concern

The committed witness improves auditability but is deliberately an agent attestation backed by repository bytes and local ordering metadata, not an independent or cryptographic execution proof.

# SF-BL-002-005 full-pipeline acceptance fixtures (HERM-466)

Synthetic, non-evaluator fixtures for
`SfBl002RouteEffectivenessFullPipelineAcceptanceTests` (stage 32,
`SF-BL-002-ROUTE-EFFECTIVENESS-005`, routing key
`sfbl002-full-pipeline-acceptance-v1`).

## Contents

- `synthetic-truth.json` — synthetic evaluator truth (accepted-intent vocabulary
  is `HYP-CAPABILITY-*`; this truth deliberately speaks a disjoint `PET-CAP-*`
  vocabulary to prove capability identity is not a scoring leg). Expected
  identities were authored from the sealed accepted intents and the public
  exact-revision Petclinic checkout (`818c4136ea971c21674525f9053de0d9c7ad8cfe`);
  they are not derived from, and never open,
  `validation/pkb001/evaluator/**`.

## Isolation boundary

- The producer run root is a fresh temp directory assembled per test from the
  five pinned non-evaluator inputs; nothing in this directory is copied onto the
  producer input path or the source checkout path.
- The tests load this truth in-process and inject it through the evaluator test
  seam, so the producer step physically cannot read it.
- Evaluator truth under `validation/pkb001/evaluator/**` is never opened by these
  tests; no sealed `-001`/`-002`/`-003` artifact is read or written here.

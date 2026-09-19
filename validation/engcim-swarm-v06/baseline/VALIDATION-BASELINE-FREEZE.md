# S01–S06 ENGCIM Swarm Validation Baseline Freeze v0.4 Correction Re-seal

Freeze date: `2026-09-20` (Asia/Taipei)
Baseline namespace: `validation/engcim-swarm-v06/baseline/`

## Freeze gate

| Gate item | Status | Evidence / blocker |
|---|---|---|
| S01–S06 scenario revisions frozen | YES | `gate0/SCENARIO-BASELINE-FREEZE.md` and six immutable JSON definitions |
| Control Binding Matrix frozen | YES | `controls/CONTROL-BINDING-FREEZE.md` |
| requiredEvidenceRefs frozen | YES | `controls/required-evidence.json` |
| Golden sets frozen | YES | S01 revision 2 is source-safe; S02/S03/S06 gold reviews remain unchanged and PASS |
| Product Context A/B protocol frozen | YES | `downstream/product-context/PRODUCT-CONTEXT-EXPERIMENT-FREEZE.md` |
| Skill A/B protocol frozen | YES | `gate0/SKILL-EXPERIMENT-FREEZE.md` |
| fixture / repository revisions frozen | YES | `manifests/source-manifest.json`, checksum manifest, canonical remote |
| runtime / model / provider config frozen | YES | `gate0/RUNTIME-MODEL-PROVIDER-FREEZE.md` |
| evaluator / producer isolation verified | YES as static preparation check | `manifests/generation-isolation-manifest.json`; runtime enforcement remains untested |
| independent review complete | YES | S01–S06 review matrix is PASS |

## Readiness matrix

| Area | Readiness |
|---|---|
| S01 | PASS — revision 2 corrects the visible-selector boundary and preserves the historical failure |
| S02 | REVIEW PASS — exact D1–D8 gold/fixture contract independently reviewed |
| S03 | REVIEW PASS — repository topology, typed edges, negatives, and evidence independently reviewed |
| S04 | PASS — PC1 revision 2 preserves the non-retryable 404 rule |
| S05 | PASS — fixture v2 aligns the interaction contract and `npm test` passes 2/2 |
| S06 | REVIEW PASS — evaluator-only gold and input boundary independently reviewed |
| Product Context A/B | PREPARED — PC0, PC1, PC1-STALE frozen |
| Control applicability / binding | PASS for preserved RC7-B runtime-gated closure; S01–S06 execution still pending |
| PK scoped seal | READY |
| S04–S06 scoped seal | READY |
| top-level validation seal | READY |

## Closure review matrix

| Gate / review | Status |
|---|---|
| RC7-B runtime Control gating closure | PASS |
| Scenario revisions frozen | YES |
| Skill A/B protocol frozen | YES |
| Runtime/model/provider config frozen | YES |
| Control applicability matrix frozen | YES |
| Control Binding Matrix frozen | YES |
| requiredEvidenceRefs frozen | YES |
| S01 source re-adjudication | PASS — result `PASS` at revision 2 |
| S01 independent gold review | PASS |
| S02 independent gold review | PASS |
| S03 independent gold review | PASS |
| S04 independent gold review | PASS |
| S05 independent gold/fixture review | PASS |
| S06 independent evaluator/gold review | PASS |
| PC0 / PC1 / PC1-STALE frozen | YES |
| canonical downstream fixture remote resolves | YES |
| evaluator / producer isolation verified | YES |
| PK scoped seal | READY |
| S04–S06 scoped seal | READY |
| top-level validation seal | READY |

## Mandatory no-go items

- Do not start effectiveness scoring until this document is reviewed as the
  authoritative READY baseline; this re-seal itself is not effectiveness.
- Do not use accepted-semantics-004 as unquestioned S01 oracle truth.
- Do not treat the preserved RC7-B runtime-gated PASS as an S01–S06 effectiveness result.
- Do not expose any gold, S06 evaluator input, exact correction patch, or future
  r2 to S01–S05 generation.
- Do not create r1, F1, r2, or any runtime result in this preparation task.
- Do not modify `src/`, skills, contracts, Scenario/Skill/Control/runtime, or
  Product Knowledge implementation.

Final classification:

```text
VALIDATION_BASELINE = READY
```

The three blockers are resolved and recorded in `OWNING-LAYER-FAILURES.md` as
bounded `EVIDENCE`, `CONTEXT`, and `FIXTURE_ENVIRONMENT` corrections. The
preserved RC7-B runtime-gated control closure remains linked through `gate0/`
and is not an S01–S06 effectiveness result. No S01–S06 runtime result, r1, F1,
or r2 was created.

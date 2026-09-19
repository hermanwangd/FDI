# S01–S06 ENGCIM Swarm Validation Baseline Freeze v0.2 FINAL

Freeze date: `2026-09-20` (Asia/Taipei)
Baseline namespace: `validation/engcim-swarm-v06/baseline/`

## Freeze gate

| Gate item | Status | Evidence / blocker |
|---|---|---|
| S01–S06 scenario revisions frozen | YES | `gate0/SCENARIO-BASELINE-FREEZE.md` and six immutable JSON definitions |
| Control Binding Matrix frozen | YES | `controls/CONTROL-BINDING-FREEZE.md` |
| requiredEvidenceRefs frozen | YES | `controls/required-evidence.json` |
| Golden sets frozen | PARTIAL | candidate gold is present; independent review is pending |
| Product Context A/B protocol frozen | YES | `downstream/product-context/PRODUCT-CONTEXT-EXPERIMENT-FREEZE.md` |
| Skill A/B protocol frozen | YES | `gate0/SKILL-EXPERIMENT-FREEZE.md` |
| fixture / repository revisions frozen | YES | `manifests/source-manifest.json`, checksum manifest, canonical remote |
| runtime / model / provider config frozen | YES | `gate0/RUNTIME-MODEL-PROVIDER-FREEZE.md` |
| evaluator / producer isolation verified | YES as static preparation check | `manifests/generation-isolation-manifest.json`; runtime enforcement remains untested |
| independent review complete | NO | S01 source re-adjudication and S01–S03 independent gold review are pending |

## Readiness matrix

| Area | Readiness |
|---|---|
| S01 | NOT_READY — source-level re-adjudication and independent review pending |
| S02 | NOT_READY — independent review pending |
| S03 | NOT_READY — independent review pending |
| S04 | PREPARED — complete/ambiguous cases and DecisionResponse frozen; no execution |
| S05 | PREPARED — canonical remote and scope/defect contract frozen; no execution |
| S06 | PREPARED — evaluator-only gold and input boundary frozen; no execution |
| Product Context A/B | PREPARED — PC0, PC1, PC1-STALE frozen |
| Control applicability / binding | PREPARED — matrix and evidence refs frozen |
| PK scoped seal | NOT_READY |
| S04–S06 scoped seal | NOT_READY |
| top-level validation seal | NOT_READY |

## Mandatory no-go items

- Do not start effectiveness scoring while this document is `NOT_READY`.
- Do not use accepted-semantics-004 as unquestioned S01 oracle truth.
- Do not expose any gold, S06 evaluator input, exact correction patch, or future
  r2 to S01–S05 generation.
- Do not create r1, F1, r2, or any runtime result in this preparation task.
- Do not modify `src/`, skills, contracts, Scenario/Skill/Control/runtime, or
  Product Knowledge implementation.

Final classification:

```text
VALIDATION_BASELINE = NOT_READY
```

Blockers are registered in `OWNING-LAYER-FAILURES.md` and are owned by
`FIXTURE_ENVIRONMENT` and `EVIDENCE`.

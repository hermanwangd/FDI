# S04–S06 Validation Baseline Freeze

Freeze revision: `downstream-baseline-v0.3-closure-review`
Dataset: `SPC-MISSION-V1`
Fixture revision: `SPC-MISSION-V1-r1`

| Gate | Status | Evidence / blocker |
|---|---|---|
| S04 complete case frozen | YES | `s04/case-a-complete.md` |
| S04 ambiguous case frozen | YES | exactly Q1 and Q2 in `s04/case-b-ambiguous.md` |
| DecisionResponse frozen | YES | `s04/decision-response-a.json` resolves exactly Q1/Q2 |
| PC0 / PC1 / PC1-STALE frozen | YES | `product-context/` |
| canonical HTTPS remote available | YES | `https://github.com/hermanwangd/engcim-v06-chart-viewer-fixture.git` |
| canonical baseline resolves | YES | `2eff5f9f84ca709684bfe0b7c90102268f07a0f0` |
| S05 allowed/forbidden scope frozen | YES | `s05/S05-DEVELOPMENT-GOLD.json` |
| seeded defect outside initial S05 scope | YES | `src/chartViewer.js` is forbidden before correction |
| S05 Development Gold complete | YES as candidate data | no r1/r2 execution performed |
| S06 evaluator-only gold complete | YES as candidate data | evaluator-only directory isolated from S05 |
| r2 solution absent from producer inputs | YES | no r2 commit or exact patch exists in baseline |
| S04 gold / Product Context review | NOT_READY | PC1 R-003 conflicts with frozen non-retryable HTTP 404 behavior |
| S05 gold / fixture review | NOT_READY | frozen `npm test` fails before execution due to export/import mismatch |
| S06 evaluator/gold review | PASS | evaluator-only boundary and r1/r2 gold reviewed |
| RC7-B runtime Control gating closure | PASS | bound from preserved runtime-gated evidence under `gate0/` |
| downstream scoped seal | NOT_READY | S04 and S05 blockers remain |

The correction loop remains frozen as a test contract only; this closure review
does not create r1, F1, r2, or S01–S06 effectiveness runtime results. The
preserved RC7-B runtime-gated evidence is a separate scoped closure and is not
an S04–S06 effectiveness result.

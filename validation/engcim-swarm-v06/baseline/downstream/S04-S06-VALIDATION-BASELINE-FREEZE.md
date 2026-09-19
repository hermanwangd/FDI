# S04–S06 Validation Baseline Freeze

Freeze revision: `downstream-baseline-v0.2-final`
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
| downstream independent review | NO | no independent reviewer result exists |
| downstream scoped seal | NOT_READY | review and runtime evidence do not exist in a preparation task |

The correction loop is frozen as a test contract only; this task does not
create r1, F1, r2, or runtime Verification Results.

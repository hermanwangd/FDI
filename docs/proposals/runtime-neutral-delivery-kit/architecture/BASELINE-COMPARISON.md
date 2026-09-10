# Baseline comparison and adoption decisions

The source files and hashes are recorded in ../SOURCE-MANIFEST.json. The actual
read order was Overview → Spec → Backlog → Plan → Status. Source checkout was
clean at inspection. No live orchestration query was performed.

| Topic | main / rc4 candidate | Software-Factory active reference | Decision |
|---|---|---|---|
| Authority | main overview points to governance locks; rc4 is untracked | Five active controls; Human, Feature Delivery, Execution planes | Use Software-Factory; never import main governance as current authority |
| T1 | rc4 FT-T2 IntentSpec is a helper projection of intention.md | IntentSpec is immutable T1 output | Use SF T1; do not reuse same-name helper schema |
| T2 | Five FT-T2 helpers and six artifact types | System Analysis → ChangeSurface → TechnicalDesign → DeliverySpec | Reuse reasoning techniques inside FD skill; no alternate canonical artifacts |
| T3 | rc4 implementation root skill | Immutable ExecutionPlan DAG, WorkItem, ChangeClaim, coverage | Preserve SF logical contracts verbatim in meaning |
| Runtime | main explicitly Multica; rc4 packaging intent | Multica replaceable; runtime fields excluded from WorkItem | Bind runtime fields only in materialized/profile/evidence surfaces |
| Correctness | Independent T4 | PASS means ENGINEERING_READY; Human terminal closure | Preserve SF distinction |
| Knowledge | rc4 PK-S1–S4 candidates | PK-S1 semantics, PK-S2 realization, PA inventory/history | Package these four capabilities; no automatic PK-S3/S4 expansion |
| Prior PKB skills | Not an authority for this target | Existing PKB registry assigns PK-S1 to realization, PK-S2 to hypothesis | Do not transplant by ID; proposed skills use SF meanings and explicit provenance |
| Lifecycle | Original proposal introduced task/run/lease domain | AUTH-003 prohibits duplicative domains without demonstrated need | Withdraw new core lifecycle; define adapter-local operational rules |
| Technology | Original proposal called Java optional | TECH-001 requires Java 17 / Spring Boot 3.4.1 | Preserve requirement; language-neutral serialization only |
| Portability | Generic workflow package | PORT-001 is cross-baseline reference export | Separate workflow portability from patch/change-reference portability |
| Current work | main DEV baseline, old worktree BL-026 | SF-BL-002 selected; others not auto-selected | Proposal does not alter remediation or select work |

Reusable rc4 ideas: evidence-bounded discovery, context provenance, skill ownership
and provider neutrality. Not adopted: legacy physical contract naming, automatic
schema equivalence, maintenance engine scope, extra Product publication authority.

Comparison is architectural, not a claim that every implementation file was
audited. Company adapters and all new skill behavior remain NOT_VALIDATED.

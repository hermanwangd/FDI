# Initial Effectiveness Baseline Freeze v0.1

Review date: `2026-09-20`

READY baseline commit: `229f104c7aa8ec17dd3d24ff35b7911abf7c9fc8`

Phase 2 evidence commit: `26a09d2233a09af7f1d3e67a86ac1f249680584b`

Review branch: `codex/initial-effectiveness-baseline-freeze`

## Frozen scenario results

| Scenario | ExecutionStatus | DomainVerdict | EffectivenessResult |
|---|---|---|---|
| S01 | COMPLETED | PASS | PASS |
| S02 | COMPLETED | PASS | PASS |
| S03 | COMPLETED_AFTER_ACCEPTED_R2_RERUN | PASS | PASS |
| S04 | COMPLETED_AFTER_CLEAN_RERUN | PASS | PASS |
| S05 | COMPLETED_AFTER_EVIDENCE_RESEAL | PASS at r2 | PASS |
| S06 | COMPLETED_R1_AND_R2 | r1 FAIL/REFUTED finding; r2 PASS/VERIFIED | PASS |

S06 r1 is deliberately not scored as an effectiveness failure: the verifier
correctly detected the seeded FV-003 defect. The final S06 r2 candidate passed
fresh independent verification.

## Dimension results

- Product Knowledge Effectiveness: `PASS`.
- Product Context Effectiveness: `PARTIAL`; PC0/PC1 clean rerun passed, but
  the frozen PC1-STALE safety case has no independent execution result.
- Skill A/B: `INCONCLUSIVE`; all four profiles are `0/0`, below the frozen
  `2 disabled + 2 enabled` minimum.
- Control Effectiveness: `PASS`; final path 18/18 required applications
  satisfied and bypass rate `0/18`.
- Runtime Effectiveness: `PASS`; metadata capacity is a recovered limitation.
- Composition Effectiveness: `PASS`.
- Evidence Effectiveness: `PASS` after preserving S05 r1 weaknesses and
  completing S05 r2 receipt closure.
- Shared Primitive Reuse: `PASS`.
- Reference Integrated Mission: `PASS`.

## Exact final evidence binding

- S05 FDI evidence revision: `d58d3848f12d8c1ad1704852d175b9f656ef66e2`.
- S05 product candidate and final S06 r2 candidate:
  `c51390ca7e748f07201b0ecd28642ee3ea8d686c`.
- S05 product tree: `2d3de714bd30552eb572d7e3bfb0f81dbed373a3`.
- Review receipt: `multica:issue/E6V-37/run/01a0bcdd-e28e-7cfd-a118-8aa8397dcdfa`.
- Verification receipt: `multica:issue/E6V-38/run/01a0bd7b-c5bf-7576-8cb1-bef4c62c6a7b`.
- Fresh S06 review: `multica:issue/E6V-40/run/01a0bd94-7aba-7d58-bca5-cb7b125143fb`.
- Fresh S06 verification: `multica:issue/E6V-41/run/01a0bd94-7a94-7d68-83f0-5b3c97c574d1`.
- F1 / FV-003 digest:
  `sha256:e4235a148aed2c9c551678fb6f375cd4a4f23d58e1ade18d7265e0ef3131e001`.
- DevelopmentResult:
  `validation/engcim-swarm-v06/effectiveness/s04-s06/development/S05-r2-development-result.yaml`.
- Artifact registry:
  `validation/engcim-swarm-v06/effectiveness/s04-s06/development/S05-r2-artifact-trace.yaml`.
- Checksums:
  `S05-r2-artifact-checksums.txt` SHA-256
  `050fbe543beaac40f43173a29917bb0263ff3685f84624c4ceec9b6f40bb13d7`;
  DevelopmentResult SHA-256
  `ba20b158b18de85ac08ece2f23f2329db2607a120ac791d4cfc12bce333f3faf`;
  artifact trace SHA-256
  `56f3f71f9f672f675893be1da704a6eec0baed521549cd29745cec2c7d8c69bb`.
- Control result refs: `CTRL-AUTHORIZATION-001`,
  `CTRL-EXECUTION-SAFETY-001`, `CTRL-REPOSITORY-PROVENANCE-001`,
  `CTRL-EXACT-BINDING-001`, `CTRL-INDEPENDENT-EVALUATION-001`,
  `CTRL-EVIDENCE-INTEGRITY-001`, `CTRL-FINDING-RESOLUTION-001`.

## Final classification

`ENGCIM SWARM CORE EFFECTIVE`

All mandatory primary gates pass. Supporting limitations remain visible and
are not converted into an aggregate score or hidden by the top-level result.

## Architecture freeze recommendation

`YES`, bounded to the observed core execution architecture:

- baseline/seal identity and scenario contract shape;
- frozen Control applicability and fail-closed gating;
- WAIT/resume, fan-out/fan-in, Reviewer/Verifier separation;
- exact candidate/evidence receipt handoff and stale-evidence rejection;
- Finding Resolution ordering at mission closure.

This does not freeze Skill A/B uplift claims, PC1-STALE effectiveness, PK
source-lock remediation, Multica metadata design, holdout/generalization, or
scaling.

## Questions answered

1. S01–S06 individually effective? **Yes, on their final accepted runs.**
2. Same EngineeringScenarioDefinition contract shape? **Yes, observed across all six.**
3. Product Knowledge effective across PK-E1–PK-E6? **Yes, with S04 source-stability governance follow-up.**
4. S01 evaluator-only semantic truth without oracle leakage? **Yes: 25/25, 18/18 critical, gold excluded.**
5. S02 all eight refresh cases? **Yes: 8/8; conflict preserved without a winner.**
6. S03 correct multi-repo realization and distractor rejection? **Yes: 4/4 repos, 5/5 critical edges, 4/4 negative mappings held out.**
7. S04 WAIT/resume and no invented PM decisions? **Yes on clean rerun: Q1/Q2 and A1 are exact; invented decisions 0.**
8. Stale Product Context fail closed? **Not fully adjudicable: the frozen safety rule exists, but PC1-STALE has no controlled execution result.**
9. S05 only authorized work? **Yes: authorization and execution-safety controls satisfied; out-of-scope mutation 0.**
10. S05 evidence independently resolvable without product behavior change? **Yes at r2; 15/15 required refs and 11/11 runtime refs.**
11. S06 r1 seeded defect independently detected? **Yes: FAIL/REFUTED and F1 produced.**
12. Stale r1 evidence rejected against r2? **Yes: accepted count 0 for old review and verification.**
13. Fresh S06 r2 PASS/VERIFIED? **Yes: E6V-40 PASS and E6V-41 VERIFIED.**
14. Finding Resolution blocked until fresh r2? **Yes: r1 and intermediate r2 remained unsatisfied/pending until fresh r2.**
15. Which Skills materially improved outcomes under controlled A/B? **None can be claimed; no controlled cells were executed.**
16. Required Controls with zero bypass? **Yes: final path 18/18 satisfied, bypass 0.**
17. Runtime fan-out/fan-in/resume without unrecovered correctness failure? **Yes.**
18. Multica metadata capacity classification? **Recovered limitation, not a correctness failure.**
19. Product Context improved downstream work without correctness regression? **Partial: PC1 pins helped discovery, but PC1-STALE was not run.**
20. Outputs exact-revision-bound and independently resolvable? **Yes for the accepted path; S05 r1 weaknesses remain historical.**
21. Owning layers? **Fixture/environment for S04 instability; evidence for S05 r1 trace/qualification; runtime for metadata capacity; context for S02 conflict and unexecuted PC1-STALE; skill for missing A/B cells.**
22. Freeze before holdout/generalization? **Yes for the bounded core listed above; no claim is made for holdout/generalization or the open supporting limitations.**

## Explicit non-mutation confirmation

- No new Scenario execution occurred.
- No auto-fix occurred during final adjudication.
- No frozen baseline artifact was modified.
- No product implementation was modified.
- No Scenario, Skill, Control, or runtime definition changed.
- No Product Knowledge or Product Context implementation changed.
- No gold, threshold, denominator, or success criterion changed.
- No stale r1 gate was reused.
- No manual child-done transition bypassed gating.
- All historical failed, blocked, partial, cancelled, stale, and superseded
  runs remain represented in `RUN-REGISTRY.json`.

Historical S05 evidence-only packaging repair remains visible; it is not
represented as a product implementation change.

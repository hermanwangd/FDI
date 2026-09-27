# RC10 candidate 20260927-01 — S05 profile deployment and S01–S06 continuation

Status: `PREPARED / AWAITING CONFIGURED READBACK`; preparation is not runtime validation.

## Candidate and authority

- Candidate version: `RC10-local-candidate-20260927-01`.
- Human-adopted profile: `ENGCIM-S05-REVIEWED-DELIVERY-v0.1`, D01–D08.
- Adoption receipt: `validation/rc10/candidate-runtime/s05-profile-adoption.json`.
- Profile source: `engcim/bootstrap/overlays/multica/RC10-S05-S06-ROLE-GUIDANCE.md`, SHA-256 `a09e9fbd2cbeecceede25c9d80612002a1c6369367450422986bdb208bcbe4e9`.
- Workspace: `0b02adb6-a395-46bd-bd92-6fec14dee20e` only.
- Projects: Validation `a3f129fa-4028-4341-98dc-c8ec20c468ae`, ProductKB `f7af4546-88b2-4163-a0d4-b350e2123dbc`, WorkspaceKnowledge `43aec4ec-3ebb-4d1b-aa54-7766c48379c1`.
- Local supervisor: Codex CLI. Existing Swarm runtime: `4f0a8b0c-3ee8-4481-a40c-2fb8aadbb39d`, model `kimi-code/kimi-for-coding`, existing squad `ba1c9f0d-00fd-48a3-8865-dfbc4ff35f73`.
- Canonical RC6 Scenario source: `engcim/swarm/baselines/rc6/runtime/package/engcim-swarm-package-RC6/docs/scenarios.md`, SHA-256 `9eefc7b72c01af62bab81bd4d4c19ff29a8c212170066f2e673245c6699f1f1b`.

The candidate input manifest in this directory binds source/contracts/bootstrap inputs, the selected RC6 runtime identities, run specification, and exact pre-update role records. Its SHA-256 is the candidate snapshot identity; Git HEAD alone is insufficient. S01–S04 results from `RC10-local-candidate-20260926-01` remain historical and retain their original candidate identity. Reuse them only with an explicit applicability rationale; never relabel them as runs of this candidate.

## Configured roles and boundaries

Only the existing Orchestrator, Architect, and Reviewer instruction fields receive role-scoped excerpts from the selected profile. Preserve the Orchestrator's current Workspace Learning content. Preserve all original instruction bytes and all roles' models, runtimes, Skills, permissions, and other settings. Curator, Coder, QA Tester, and Verifier receive no S05 profile instruction update.

The fixture is the existing disposable Chart Viewer repo at `2eff5f9f84ca709684bfe0b7c90102268f07a0f0`; the existing S03 RC7-B pair at `92ec2570a4da188baca4bbb50f48db27e6906c89` is a NO-EDGE negative control. Do not mutate production or the sealed RC6 source/package. All workspace-scoped CLI operations use the explicit workspace ID above.

## S05 entry, stages, and stop condition

Input is S04 `RC10VAL-29`, IntentSpec revision 1, SHA-256 `ded65a80ba34ef20792aafdcb0f4c8dababbda7a4327c5b7decdb2f98d565602`, plus only applicable Product Knowledge references from the governed structured store.

Use the adopted handoff sequence and existing issues/roles: C1 = SPEC + ChangeSurface; independent non-author Design Review C2; AC → WorkItem/repository/test/dependency/integration-owner decomposition C3; applicable authorization; per-repository implementation/self-tests and independent Code Review C4; exact integrated candidate C5; then S06 testing/verification and final QA Review.

**Pre-coding stop:** live issues `RC10VAL-8` and `RC10VAL-9` record unresolved contradictory Product Context, including `chartLimits().max` 10 versus seeded 1000 and `openSelectedChart` test contract versus `selectChart` implementation contract. Under adopted D02, Design Review and independent analysis may proceed, but no dependent coding dispatch may proceed while the accepted Product Context, repository scope, or AC remain unresolved. Preserve all claims and limits. This run spec does not choose either side or authorize a Product Knowledge promotion.

S06 may perform bounded diagnosis on an eligible fixed candidate. Formal S06 PASS still requires the applicable independent verification, report/coverage/findings, and final QA Review bound to the exact candidate. Keep WorkItemResult, VerificationResult, ControlResult, agent verdicts, and Human DONE distinct.

## D08 evidence scope and execution limits

Report single-repository, evidence-supported multi-repository positive, and NO-EDGE negative cases separately. The pinned RC7-B pair proves only the negative case. The staged positive API/Web case is supplemental and remains outside the current issue/run scope pending the user's scope answer; no positive multi-repository claim may be inferred from S03. Do not report complete D08 coverage while that positive case is absent.

Use one parent Scenario at a time, at most two active Swarm runs including its parent, one child at a time, and Reviewer then Verifier sequentially. Stay under 8 GiB total process RSS. Preserve all failures and corrections. A CLI success, issue status, or self-report alone is not a Scenario PASS. No production deployment, merge, promotion, Human DONE, or company Claude CLI execution is in scope.

# [S06] RC10 candidate baseline diagnostic — pinned fixture 2eff5f9f

Run only in MultiCA workspace `0b02adb6-a395-46bd-bd92-6fec14dee20e`, Validation project `a3f129fa-4028-4341-98dc-c8ec20c468ae`, using the existing squad and roles. Local supervisor Codex CLI only; no company Claude CLI. Candidate source snapshot: `RC10-local-candidate-20260927-01`, input manifest SHA-256 `902a5e97d3de9185ea22a36687823a4e1997d7e2d13c7c8e2f48f0b1a0ec4267`. This is an early, pre-implementation S06 diagnostic, not a release gate or full S06 PASS.

## Exact target and predecessor evidence

- Fixture repository: `https://github.com/hermanwangd/engcim-v06-chart-viewer-fixture.git`, exact commit `2eff5f9f84ca709684bfe0b7c90102268f07a0f0`. Independent source readback file attached, with SHA-256 for `src/interaction.js`, `src/chartViewer.js`, `test/interaction.test.js`, `README.md`, and `package.json`.
- Adopted profile: `ENGCIM-S05-REVIEWED-DELIVERY-v0.1`; source SHA-256 `a09e9fbd2cbeecceede25c9d80612002a1c6369367450422986bdb208bcbe4e9`; adoption receipt SHA-256 `8ade042327de65549f9a2f02c7c70915f1bf2f2c70cd81459a6c0cca7c0b9361`.
- S05 issue `RC10VAL-30` remains `in_review`; C1 is SPEC + ChangeSurface at `RC10VAL-31` comment `01a0df0a-591e-71df-8832-b67e3a7c1c79`, bound to S04 IntentSpec `RC10VAL-29` revision 1 SHA-256 `ded65a80ba34ef20792aafdcb0f4c8dababbda7a4327c5b7decdb2f98d565602`; independent C2 review `RC10VAL-32` comment `01a0df0f-6949-7bef-b74c-2b4d00991b44` is PASS on C1 revision 1. C3 is conditional planning only. No implementation exists and no coding authorization was issued.
- `RC10VAL-8` revision 9 and `RC10VAL-9` revision 8 remain blocked/CONFLICTING. Preserve both sides; do not adjudicate Product Knowledge or modify any source.
- D08: the existing S03 `RC10VAL-28` NO-EDGE case is the negative control. No approved positive multi-repository case is in this run; do not infer one or claim complete D08 coverage.

## Diagnostic scope and sequence

Use one child at a time, at most two active Swarm runs including this parent. Keep total process RSS below 8 GiB. Before each dispatch, read active sibling runs; count queued/dispatched/running as active.

1. QA Tester: create the existing S06 verification design/test plan for this exact baseline, covering functional, contract, integration, and regression categories with not-applicable reasons where appropriate; map all five IntentSpec ACs to checks and evidence. Pin `git rev-parse HEAD` and source hashes. Run the existing `node --test` suite once without changing files; preserve the full command, exit code, stdout/stderr and observed failure. If the static named import prevents the test file loading, test AC2 (`classifyChartResponse(404)`) and AC3 (`selectChart` hit/miss) with read-only direct execution. Do not add tests or dependencies.
2. After QA is terminal, Verifier independently checks out the same commit, verifies exact HEAD and file hashes, and independently reruns the same baseline suite and direct checks. Record a distinct run ID and raw execution evidence. Do not rely on QA self-report.
3. After Verifier is terminal, Reviewer performs the existing S06 contract/stage-gate review of the exact diagnostic report revision, AC coverage table, findings, and source/evidence traceability. The reviewer must distinguish baseline diagnosis from an implementation review and must not issue final release QA or imply feature readiness.
4. Orchestrator fans in the three actual results and posts one diagnostic report. Bind every verdict to exact candidate/commit/report revision. `AC1` is expected to be checked against the baseline, not presumed passed. Mark AC4 (change-set-only scope) not applicable or inconclusive because no implementation diff exists. Preserve `VerificationResult` (`VERIFIED`/`REFUTED`/`PARTIAL`) and any aggregate S06 acceptance outcome (`PASS`/`FAIL`/`INCONCLUSIVE`) as separate fields. Any failed AC means the diagnostic acceptance result cannot be PASS. Add the required finding fields: finding ref, candidate revision, expected, actual, evidence ref, impact, probable owning layer, status. Use `correction_regression: none` because no correction is permitted.

## Strict no-mutation boundary

This is baseline diagnosis only. Do not modify or stage the fixture, create commits/PRs, change test assets, write to ProductKB/WorkspaceKnowledge, register a repo in the workspace, create agents/skills/services, dispatch a coder, correct findings, mark any issue `done`, claim Human approval, or claim S05/S06 release completion. Checkout only exact commit `2eff5f9f84ca709684bfe0b7c90102268f07a0f0`; never use `--fresh`. Leave the issue in `in_review`.

## Workspace isolation requirement

Every Multica CLI invocation, including issue reads/writes, run queries, attachment operations, repository checkout, and help commands, MUST explicitly carry `--workspace-id 0b02adb6-a395-46bd-bd92-6fec14dee20e` (global position before the subcommand is preferred). Verify returned `workspace_id` / project IDs. Do not rely on a default workspace.

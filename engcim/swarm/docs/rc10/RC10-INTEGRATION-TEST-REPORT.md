# RC10 Integration Test Report

## Current execution checkpoint — 2026-09-27T08:19Z / 16:19 Asia/Taipei

**Status: RC10_IMPLEMENTED_WITH_BLOCKERS.** Implementation candidate RC10-local-candidate-20260927-01; package snapshot RC10-PACKAGE-SNAPSHOT-20260927-02 (documentation and corresponding import-manifest digest refresh only). Git HEAD 66dcc0d1316f8179a81b4c4564a821c574b7a54b plus dirty inputs remain bound by the runtime/source manifest r2 (validation/rc10/candidate-runtime/s05-profile-deployment-20260926T1821Z/candidate-input-manifest-r2.json, SHA-256 a89ea0e608f48d59c6161d3d4e54ad105d1c24e2b383c352f90ebcd3ba6b0962, 204 entries). Runtime, source, and test inputs are unchanged; existing run receipts remain bound to their recorded candidate identities. See the external package-reseal receipt for the current archive seal. Local Supervisor is Codex CLI; company Supervisor is Claude CLI.

The adoption receipt validation/rc10/candidate-runtime/s05-profile-adoption.json (SHA-256 8ade042327de65549f9a2f02c7c70915f1bf2f2c70cd81459a6c0cca7c0b9361) records the user's confirmation, profile ENGCIM-S05-REVIEWED-DELIVERY-v0.1, D01–D08, and source SHA-256 145cfa3fc1b00bf25f533052478970979a7cf056ce23440d2c47436d5300f449. Adoption applies to the isolated RC10 candidate/workspace only; sealed RC6 bytes and historical run meaning are unchanged, and production promotion is excluded. The existing Orchestrator, Architect and Reviewer instruction configurations were read back with matching configured/desired digests b3b29cd7f56cbed065f20923e97210a37e7c404739593718dbd7ddaba1c0b472, e6c8861bd1ab0bfb95b2863e15870e31f1e95ccf40538d5fa45c46139955c6a9, and 16d15bc328b39a05c30c8d79c82e0c8e4b47dead055833157d6e86c195aa1964. These prove persisted configuration. Some actual run outputs report following profile clauses and show consistent behavior, but no run binds an effective-instruction digest or loaded bytes; exact per-run consumption remains UNVERIFIED.

| Validation | Latest evidence | Bounded conclusion |
|---|---|---|
| S01 and learning/reuse | RC10VAL-37 r53; RC10VAL-47 r70 | S01 Reviewer/Verifier technical gates are recorded. Later Mission W3 read the AVC and classified it read-only; its Reviewer PASS and Verifier 6/7 retain an envelope limitation. W1/W2 were canceled/invalid. Causal influence and complete T13 negative eligibility coverage are not established. |
| S02 | RC10VAL-51 r143 | Reviewer PASS and Verifier VERIFIED; elapsed-time, concurrency and out-of-role duplicate findings remain. |
| S03 | RC10VAL-66 r41; fan-in comment 01a0e036-6dc7-7b80-9798-9590bfc0ffc9 | Positive customer-api → customer-contract DEPENDS_ON edge passed static and execution verification. RC7-B/Chart Viewer negative control was correctly returned as UNKNOWN where absence could not be proved. S03 is COMPUTED COMPLETED with that limitation retained. |
| S04 | RC10VAL-70 r45 | Revision 2 Reviewer PASS and Verifier VERIFIED; findings remain. Later reference to RC10VAL-15 does not change that receipt's earlier-candidate provenance. |
| S05 | C1 r3 01a0dffe-d72e-7502-85e6-19214a598943; independent C2 r3 PASS 01a0e003-d00f-7a97-9711-d96e610e85c6; C3 conditional plan completed; C4 current Code Review PASS 01a0e103-b0c2-790a-b16f-807719f3b274 | Initial C4 commit 3be28d44fa1e9fcdcfb48d169f47672e373cf4c9 was followed by the authorized one-line F1 correction in candidate 0159b44d9e89bb06e1e2d72b752c4b9cb6e5685e. RC10VAL-63 issue remains in_review; a technical review PASS is not Human DONE. |
| S06 | RC10VAL-64 VERIFIED comment 01a0e10c-8b88-71b0-97a3-c36338f6dc3c; final QA PASS 01a0e120-2f17-7bbf-a7b8-fc9407659027 | Independent S06 and final QA bind to exact commit 0159b44d9e89bb06e1e2d72b752c4b9cb6e5685e. npm test passed 2/2; chartLimits max is 10 and invalid configurations raise the expected error. F1 is corrected and closed for this commit. RC10VAL-64 and RC10VAL-65 remain in_review; no Human DONE follows from these technical results. |
| T13 follow-up | RC10VAL-74 comment 01a0e117-637f-758d-a37b-8302502a1f25; parent RC10VAL-72 | Bounded attempt 2 is VERIFIED_WITH_FINDINGS; parent remains in_review. Three non-blocking findings remain: partial RC10VAL-12 reference listability wording, preserving the two-layer conflictRefs evidence, and the provider comment-list/W1 dispatch-reference anomaly. This supporting result is not S05/S06 acceptance or Human DONE. |

The new correction commit's exact diff is limited to the authorized chartViewer.js max value change from 1000 to 10; frozen tests and interaction.js are unchanged. The new Code Review comment's claim that a Swarm Verifier fix run authored the commit is inaccurate: the mutation was made by Codex task 01a0dc67-960f-7100-972d-557464f7c1fd; Git records Herman Wang as author/committer and multica-agent as co-author. The later Code Reviewer and S06 Verifier were separate actual runs. Preserve the original comment and report the attribution correction without treating it as a failure of their independent verdicts.

The S06 receipt validation/rc10/candidate-runtime/s05-profile-deployment-20260926T1821Z/S06-F1-REVALIDATION.json exists in this primary worktree (SHA-256 ed438bbc1d979a597c553de58a2ee3142b718dc414ff95990cf84ce9b1a28141). Final QA could not resolve the validation directory from its separate verifier checkout; this is an evidence-access discrepancy, not proof the receipt is absent or proof QA read this local copy.

Current provider status readback: RC10VAL-30, RC10VAL-63, RC10VAL-64, RC10VAL-65, RC10VAL-72 and RC10VAL-74 are all in_review. RC10VAL-30 metadata still points C4-review and S06 resultRef/revision to historical r1 comments/revision, while its executionOutcome mentions the current C4 review and S06 r2; its QA metadata still references the older QA result. The current exact-candidate verdicts are the newer comments listed above. Treat the provider metadata as a bookkeeping lag for parent reconciliation, not as a replacement for the bound current comments.

No aggregate S01–S06 PASS, Control SATISFIED, Human DONE, merge, production deployment, promotion, or rollout is claimed. The fresh Java 17 module suite after import-manifest reconciliation and this documentation refresh passed 45 tests, with 0 failures, errors, or skips; CompanyImportManifestTests and JavaOnlySourcePolicyTests each passed 1/1. Standalone governance tests passed 14/14 and standalone bundle verification passed 61/61. The regenerated root release manifest contains 1,712 file entries. Package snapshot RC10-PACKAGE-SNAPSHOT-20260927-02 contains 1,713 entries and passed exact path/hash/size, embedded-manifest, and ZIP CRC checks. Its archive SHA-256 and raw verification log hashes are recorded in the external PHASE1-PACKAGE-RESEAL-RECEIPT.json. This package-only documentation refresh does not change runtime/source/test inputs, historical run bindings, the accepted bounded Phase 1 disposition, or any retained S01–S06/profile limitation.

## Superseded checkpoint — 2026-09-26T23:06Z (candidate manifest r1; historical, superseded by r2)


Status: RC10_IMPLEMENTED_WITH_BLOCKERS. Implementation candidate RC10-local-candidate-20260927-01; package snapshot RC10-PACKAGE-SNAPSHOT-20260927-02 (documentation and corresponding import-manifest digest refresh only). Git HEAD 66dcc0d1316f8179a81b4c4564a821c574b7a54b plus dirty inputs remain bound by the runtime/source manifest r2 (validation/rc10/candidate-runtime/s05-profile-deployment-20260926T1821Z/candidate-input-manifest-r2.json, SHA-256 a89ea0e608f48d59c6161d3d4e54ad105d1c24e2b383c352f90ebcd3ba6b0962, 204 entries). Runtime, source, and test inputs are unchanged; existing run receipts remain bound to their recorded candidate identities. See the external package-reseal receipt for the current archive seal. Local Supervisor is Codex CLI; company Supervisor is Claude CLI.

- Product-decision history: Human Addendum RC10VAL-30 resolves RC10VAL-9 as additive openSelectedChart, preserving selectChart and non-retryable 404; RC10VAL-8 sets max=10 and treats 1000 as a seeded defect. The initial interaction.js-only scope was later expanded by the separate S06 F1 authorization for chartViewer.js; independent Code Review, S06, and final QA results bind to corrected commit 0159b44d9e89bb06e1e2d72b752c4b9cb6e5685e. Frozen tests remain unchanged.
- Current-candidate S01 and subsequent reuse records exist at RC10VAL-37/47/50. Their `in_review` states do not imply Human acceptance. Preserve their scoped receipts; do not repeat the obsolete blanket assertion that every S01/reuse record belongs to the older candidate. This update does not elevate issue state alone to independent consumption proof.
- Current-candidate S02 r10 technical gates are Reviewer PASS (`01a0dfe0-ee39-7524-9a51-46fc05def994`) and Verifier VERIFIED (`01a0dfe9-6598-73ca-83fd-dca747729cf3`). Fan-in `01a0dfed-720e-7031-ab23-12fda0840d86` selects archive SHA-256 `ae9cc47035c5a234ac189287696be7a92f68738df8685eb4c99aa86a72c1039e`. Preserve elapsed-time, concurrency and out-of-role duplicate findings; these gates do not establish aggregate scenario-process PASS.
- S05 C1 r2 delivery is `01a0dfee-6b74-749a-b413-d42449e9b74b`; non-author C2 r2 PASS is `01a0dff5-249f-70e1-aefb-573451a53237`. That review omitted the supplemental learned-knowledge applicability requirement and the recorded missing prohibition on guessing unresolved Product Context. Supervisor correction `01a0dff7-63cf-7191-96e8-41fee5960953` was accepted by Orchestrator (`01a0dff8-e2c2-7c14-ad65-9a23de63ef8a`). Architect delivered C1 r3 at `01a0dffe-d72e-7502-85e6-19214a598943`; independent C2 r3 PASS is recorded at `01a0e003-d00f-7a97-9711-d96e610e85c6`. Reviewer independently verified the knowledge checks, canonical digests, no-new-influence statement, supersession of stale RC10VAL-8/9 wording and source/evidence resolution. Reviewer noted that four proposal comment refs were not each expanded and RC10VAL-8/9 issue statuses remain blocked; both are informational, with Human Addendum controlling current product facts. Fresh canonical record SHA-256 is `5507ba68c381603808740316ac140ddb221e96222d5a1fe815661087e820e333`; canonical proposal digest is `68c5b950fe39fb811cbd5e67caebab42f7f22b9eaa1959f7e30bf86549f2ac0a`; `fd600282…` is the extracted pretty-printed JSON text digest only. C4 independent non-author Code Review PASS is recorded at RC10VAL-63 comment `01a0e008-6031-7d81-a414-806f28903833`, bound to the exact Coder commit. The reviewer inspected the diff and scope but did not run tests; S06 remains necessary. Criterion 3 in the review retains stale wording that RC10VAL-8/9 were unadjudicated, although criterion 2 correctly follows the Human Addendum and the reviewed code leaves those areas unchanged. Its task description was corrected while the run was active; consumption of that no-start update is unverified. RC10VAL-60 Coder delivered comment `01a0dff8-054e-7114-b8b4-3ff9f2fe9caf`, reporting local fixture commit `3be28d44fa1e9fcdcfb48d169f47672e373cf4c9`, interaction.js-only +5 lines and 2/2 self-tests. These are producer claims pending exact-revision reconciliation, independent code review and exact-candidate S06. No complete S05 PASS is claimed.
- RC10VAL-46 APPROVED/CURRENT metadata cannot override later Human product decisions. Fresh version/digest, selection or justified exclusion, and actual design influence remain required in this S05 correction. Do not silently trim an approved payload or force reuse. Configured instructions, task-description readback, and actual per-run consumption remain distinct evidence.
- S06 RC10VAL-33 is baseline diagnostic evidence only. Its prior AC5 preservation criterion is not proof that max=1000 satisfies the resolved product intent. Post-implementation S06 Verifier RC10VAL-64 VERIFIED exact commit `3be28d44…`: `npm test` 2/2, scope/diff and behavior probes passed. It observed F1 (`chartLimits().max=1000` vs intent 10) and made no repair; evidence comment `01a0e00c-1384-763d-8bd4-a2da328cea73`. Current-candidate S03/S04 applicability and positive D08 multi-repository coverage remain outstanding. No all-six-scenario PASS is established.
- Earlier Surefire report-audit snapshot: 44 tests, zero failures/errors/skips, including source policy and RC6 compatibility. That audit inspected existing report hashes without rerunning tests; it is historical evidence and is superseded for current status by the fresh 45/45 run in the checkpoint above. Audit: validation/rc10/candidate-runtime/s05-reuse-freshness-review/java17-report-audit.json.

The pre-sync alignment audit recorded an intermediate state: the ZIP matched older report bytes while the updated plan drifted from the release manifest. Package snapshot RC10-PACKAGE-SNAPSHOT-20260927-02 supersedes that checkpoint: current report bytes and import-manifest destination hashes are reconciled, fresh tests passed, and the exact rebuilt archive passed its path/hash/size, embedded-manifest, and CRC audit. The external package-reseal receipt binds those results. Retain the pre-sync audit as history, not an open reseal action. No production merge/deployment/promotion or Human DONE is authorized or claimed.


## Superseded checkpoint — previously current candidate evidence — 2026-09-27 (Asia/Taipei)

Candidate identity is `RC10-local-candidate-20260927-01`, frozen input-manifest SHA-256 `902a5e97d3de9185ea22a36687823a4e1997d7e2d13c7c8e2f48f0b1a0ec4267` (204/204 input hashes match), Git HEAD `66dcc0d1316f8179a81b4c4564a821c574b7a54b`. The following table distinguishes earlier-candidate evidence from current-candidate work:

| Scenario | Candidate/revision | Current evidence | State |
|---|---|---|---|
| S01 Product Knowledge | `RC10-local-candidate-20260926-01`, RC10VAL-10 r3 | Curator artifact passed independent Reviewer and Verifier; issue remains `in_review` | Gate passed on earlier candidate |
| S01 learning capture | earlier candidate, RC10VAL-11 | Capture/review/verification fan-in completed; actual later-Mission Authorized Visible Context consumption is not independently verified | Partial evidence |
| S02 Product Knowledge refresh/reuse | earlier candidate, RC10VAL-20 | Required technical gates recorded complete after the fixed stop-bound; direct receipt consumption in Curator run remains unverified | With findings |
| S03 multi-repo analysis | earlier candidate, RC10VAL-28 r1 | Reviewer PASS and Verifier VERIFIED for the pinned NO-EDGE pair only | Negative control only; no positive case |
| S04 IntentSpec | earlier candidate, RC10VAL-29 r1 | Reviewer PASS and Verifier VERIFIED; RC10VAL-8/9 remain blocked | Definition gate only |
| S05 reviewed delivery | current candidate, RC10VAL-30 | C1 delivered, non-author C2 PASS, C3 WorkItem map delivered; no coding dispatch | Pre-coding complete with blocker |
| S06 baseline diagnostic | current candidate, RC10VAL-33 | QA run + independent Verifier rerun + Reviewer PASS, bound to fixture commit `2eff5f9f84ca709684bfe0b7c90102268f07a0f0` | Diagnostic complete; not S06 implementation PASS |

For S06, the reviewed per-criterion result is AC1 REFUTED at baseline (`node --test` fails to import missing `openSelectedChart`), AC2/AC3 VERIFIED, AC4 N/A, and AC5 PASS because IntentSpec r1 requires max remain 1000. The parent integration comment has an inconsistent later phrase labeling AC5 REFUTED; the explicit Reviewer verdict `01a0df29-1bcd-7c9e-89c3-91ff7d64daef` governs this criterion. The final S06 report is comment `01a0df2a-e992-7ec5-9a0f-aa9c5f61a626`; the issue remains `in_review`.

No single-candidate S01–S06 integration PASS is established. No later Mission independently demonstrated use of a governed WorkspaceKnowledge record through fresh Authorized Visible Context. Do not infer runtime composition from local Java contract tests or from issue status.

## Historical checkpoint — 2026-09-26T17:20:27Z

This pre-adoption checkpoint is retained as historical evidence; the current candidate evidence above supersedes it.

- S01 candidate gate passed at r3; its issue remains `in_review`. Learning Mission RC10VAL-11 completed its required v3 capture/review/verification gates (final fan-in `01a0ddf4-5a30-7117-97d1-98f1fc86daa4`); its issue also remains `in_review`. The original time-budget overrun and concurrency overrun remain findings. Later-Mission runtime retrieval/use is still unverified.
- S02 required technical gates were recorded complete only after the fixed 14:53Z stop-bound, with the later Verifier result as an addendum to the single stop-bound report. That Verifier reported all seven checks reproduced without requiring the missing Stage A bytes; this does not change the Supervisor's recorded no-waiver attribution correction. The issue remains `in_review`; actual WorkspaceKnowledge receipt consumption inside the Curator run is not independently verified, and scope/routing findings remain.
- S03 revision 1 relation analysis received Reviewer PASS and Verifier VERIFIED; its no-edge result stands, with concurrency-control findings retained. S04 IntentSpec revision 1 received PASS and VERIFIED; its workspace-scope and concurrency findings remain. Both issues remain `in_review`.
- The current issue inventory contains no S05 or S06 issue, and S04 did not dispatch them. S05/S06 remain HOLD / NOT RUN. `in_review` is not Human DONE. Do not treat S04's supplied S02 receipt as proof of runtime WorkspaceKnowledge retrieval/reuse.

## Historical candidate runtime checkpoint — 2026-09-26T12:20:47+00:00

Local Supervisor is **Codex CLI**; company Supervisor is **Claude CLI**. Both use the existing Multica/Swarm path, without a second dispatcher or per-CLI adapter. The company profile is not a prerequisite for this local validation.

Candidate input snapshot: `acaea4171d257754204aba8d1e5a1e267226bcadad0e779d6982c93567e89e2e`. Deployment readback matched both existing-role instructions and preserved Skills/model/runtime/permissions. Actual Orchestrator and Curator runs returned the matching deployment envelope. Evidence: `validation/rc10/candidate-runtime/deployment-verified.json` and the S01 receipts.

Candidate S01 `RC10VAL-10` (`01a0dd73-7d75-7295-aeb1-a83905eb2312`) was dispatched by Codex CLI to the existing squad. r1 received REVISE; r2 independently received Reviewer REVISE and Verifier FAILED for `NESTED_TRUST_UNSUPPORTED`. Existing fan-in prevented promotion and dispatched the final allowed correction. Curator delivered r3 (`57af68f6f63561e181df6edab6d0adea160620b78c058cf5c610b296581411ba`), with downloaded bytes verified. Independent Reviewer PASS and Verifier VERIFIED were consolidated by Orchestrator in final fan-in `01a0dda2-edf9-7a88-91c9-d8bc1bdab15e`: S01 gate PASSED at r3; Human DONE remains separate.

Codex CLI prepared schema-shaped S01 MissionLearningSource and dispatched learning Mission `RC10VAL-11` (`01a0dda7-01b9-76f7-b53b-e2e629a042c2`) to the existing squad. Leader run `01a0dda8-68d4-72db-9408-0da6332c53fd` is confirmed running; capture/governance/replay results are not yet verified. S02–S06 and subsequent-Mission reuse remain NOT_RUN. Final archive resealing and formal Phase 1 handoff remain pending. `validation/rc10/candidate-runtime/runtime-checkpoint.json` records subsequent observations. Earlier dated checkpoints are history, not current completion claims.

## S01 acceptance review checkpoint

Cutoff: 2026-09-26T12:14:08+00:00 / 2026-09-26T20:14:08+08:00. Candidate source/configuration is unchanged.

| Delivery | Artifact SHA-256 | Disposition |
|---|---|---|
| r1 | `6568a91407cd35e90f27fdd092eda4d6c7f73ae8cf392d2abe246180d0530bc3` | Download verified. Initial artifact-integrity PASS superseded by REVISE: no usable validated Chart Viewer context. |
| r2 | `55a69acd105537ee3c13bdab04378deea62e5a5fa624dfc73a1030eb67f9dff9` | Download verified. Reviewer REVISE; independent Verifier FAILED. Nested validationState is not defined/consumed by existing RC6 governance. |
| r3 | `57af68f6f63561e181df6edab6d0adea160620b78c058cf5c610b296581411ba` | Download verified. Existing entry-level representation separates verified claims from unresolved conflicts. Independent Reviewer PASS, Verifier VERIFIED and final Orchestrator fan-in completed. |

r2 final Reviewer comment `01a0dd8d-bd06-78e5-b0fa-04df1ee30867` and Verifier comment `01a0dd8f-2f56-7817-990f-c3ba3a9acacc` independently rejected promotion. Orchestrator dispatched r3 in comment `01a0dd90-d141-784b-ae63-f953848738b4`; Curator delivery is `01a0dd93-856f-70f1-b40e-e576ec4fc50f`. Raw receipts are in `s01-r2-final-verdicts.json` and `s01-r3-comments.json` under the candidate evidence directory.

Usable validated context was an original Mission requirement, not a new acceptance condition. Preserve r1/r2 failures and r2's incorrect attached validator stdout (exit 2); Verifier independently reproduced the actual r2 validator success but still rejected consumer compatibility. Producer delivery, validator success and independent acceptance remain distinct.

Two versioned S02 inputs remain prepared but not dispatched. The finalized learning-capture Mission is now dispatched; its draft is historical preparation only. S01 gate PASSED for the exact r3 artifact. Reviewer comment `01a0dd97-b559-77f1-a4af-075e22dea9c5`, Verifier comment `01a0dd9e-6de7-7843-b567-0cb0c87d0a0d` and final fan-in `01a0dda2-edf9-7a88-91c9-d8bc1bdab15e` provide separate evidence. The actual parent remains in_review for Human acceptance. The Codex Supervisor dispatched the separate learning-capture Mission at 12:19:56Z; its real leader run is acknowledged, while capture results remain unverified.

## S01 operational limitations retained

The 45-minute Scenario boundary was reached at 12:07:32Z. New worker dispatch stopped; the existing Verifier completed at 12:09:02Z within its child limit. Its final mention had a transposed agent-ID character, so the Codex Supervisor reconciled that same verdict once in comment `01a0dda0-3144-70c3-aab8-3f3304f08673`; Orchestrator completed fan-in without a new worker or correction. This elapsed-time overrun and manual notification repair are limitations, not an entirely unattended runtime PASS.

The r3 payload's updatedAt is 53 seconds later than provider upload; use provider events for chronology and retain immutable archive bytes. The verifier's selection demonstration is a test script over the existing contract, not actual later-Scenario consumption. S02 must prove real use. See `s01-time-bound-inspection.json`, `s01-r3-chronology-limitation.md` and `s01-final-fanin-comments.json`.

## Scope

T01–T12 exercise Java contract models and boundaries. They do not invoke the live Codex/Claude Supervisor path or the Multica Swarm. The focused additions also verify explicit knowledge-governance outcomes and rejection of a returned runtime revision mismatch.

Candidate input identity: Git HEAD `66dcc0d1316f8179a81b4c4564a821c574b7a54b` plus immutable candidate input manifest r2 `validation/rc10/candidate-runtime/s05-profile-deployment-20260926T1821Z/candidate-input-manifest-r2.json`, SHA-256 `a89ea0e608f48d59c6161d3d4e54ad105d1c24e2b383c352f90ebcd3ba6b0962` (204 entries).

## Local acceptance results

| Test | Result | Evidence |
|---|---|---|
| T01 | PASS | `SupervisorPathTests.completeHumanRequestIsSubmittedBySupervisorToSwarm`; `MissionFlowTests.T01_completeRequestCrossesMissionSwarmBindingAndMultica` |
| T02 | PASS | `SupervisorPathTests.materiallyIncompleteRequestReturnsTargetedClarificationWithoutDispatch`; `MissionFlowTests.T02_incompleteRequestRequiresClarificationAndDoesNotDispatch` |
| T03 | PASS | `MissionFlowTests.T03_constraintsAndAcceptanceCriteriaArePreservedExactly` |
| T04 | PASS | `SupervisorBoundaryTests.T04_supervisorEngineeringDispatchIsRejected` |
| T05 | PASS | `SupervisorBoundaryTests.T05_supervisorOperationalMulticaActionIsAllowed` |
| T06 | PASS | `MissionFlowTests.T06_bindingReceivesExactMissionIdentityAndRevision`; `MulticaRuntimeBindingReceiptTests` rejects a returned revision mismatch |
| T07 | PASS | `SupervisorPathTests.supervisorClosureSummaryPreservesEvidenceBeforeLearningSource`; `LearningBoundaryTests.T07_closureAndEvidenceProduceMissionLearningSource` |
| T08 | PASS | `LearningBoundaryTests.T08_missionLearningSourceProducesWorkspaceKnowledgeProposal`; `KnowledgePipelineTests`; `WorkspaceKnowledgeLifecycleTests` |
| T09 | PASS | `LearningBoundaryTests.T09_crossWorkspaceLearningIsRejected`; workspace-scoped repository tests |
| T10 | PASS | `LearningBoundaryTests.T10_productTruthCandidateDoesNotBecomeWorkspaceKnowledge` |
| T11 | PASS | `LearningBoundaryTests.T11_directMissionOrSwarmToTkmsPublicationIsRejected` |
| T12 | PASS | `MissionFlowTests.T12_resultTypesRemainDistinct` |

Additional local cases cover approved-only persistence, deferred/rejected governance outcomes, unresolved-conflict blocking, capture attribution and distinct Product Knowledge routing. These are local test results, not live provider evidence.

## RC6 baseline evidence

| Evidence | Result |
|---|---|
| Canonical RC6 package verifier | 25 PASS, 0 FAIL, 4 optional external checks NOT VERIFIED |
| Baseline scenario | RC6 S01 PASS for Curator delivery, independent Reviewer and independent Verifier; issue remains `in_review` for Human acceptance |
| Baseline issue | `RC10VAL-4`, ID `01a0dd0f-2eea-7180-a1d3-43e65e790662`, revision 34 |
| Reviewer / Verifier | Reviewer `01a0dd1e-6224-7895-83c5-ea82808f353e`; Verifier `01a0dd1e-622d-7e4d-95a8-46f298ead051` |
| Dispatch identity | User assignment directly through Multica; not a Codex-supervised dispatch |

## RC10 candidate runtime status

Candidate `RC10-local-candidate-20260927-01` is deployed in isolated Multica workspace `0b02adb6-a395-46bd-bd92-6fec14dee20e`. The current S01–S06 technical evidence and exact limitations appear in the checkpoint above. S01–S06 issues are `in_review`; Human DONE and aggregate PASS are not asserted. Learning/reuse evidence includes actual W3 AVC read and read-only classification with a 6/7 Verifier result and one envelope limitation; it does not establish causal influence or all T13 negatives.

## Local command evidence

| Check | Result | Evidence boundary |
|---|---|---|
| Java 17 module suite | 45 tests, 0 failures/errors/skips | Includes `JavaOnlySourcePolicyTests`, `Rc6CompatibilityTests`, and import-manifest validation; rerun after final report-hash refresh. |
| T01–T12 local tests | PASS | Deterministic Java contract/model tests; not live Multica runtime evidence. T12 behaviorally verifies distinct WorkItemResult, VerificationResult, and ControlResult outcomes. |
| RC6 sealed-package self-test | PASS | Baseline-package evidence only. |
| Standalone governance / bundle checks | Earlier recorded 14 tests and 61 verifier checks passed | Must rerun against the refreshed reports and final candidate ZIP before claiming final package validation. |

Package, schema and static checks do not substitute for live candidate Scenario evidence.

## Historical candidate source checkpoint — 2026-09-26T19:15:58+08:00

Current source/contract digest is SHA-256 of `validation/rc10/candidate-runtime/source-contract-manifest.json` (89 file entries). The broader deployment snapshot is `candidate-input-manifest.json`, SHA-256 `acaea4171d257754204aba8d1e5a1e267226bcadad0e779d6982c93567e89e2e`: 190 files covering dirty source/contracts, bootstrap configuration, original live role/Skill content and the bounded run specification. The existing-role overlay and loading/rollback procedures are now implemented in candidate source; runtime acceptance remains separate. Fresh Java 17 module validation passed 44 tests, 0 failures/errors/skips, including source policy and import-manifest validation. The prior release ZIP is superseded pending final reseal against new evidence.

## Learning scope observation — 2026-09-26T12:28Z

C1 Curator run `01a0ddac-eb93-7b13-8335-ae5dc3a8ca5d` has 14 observed Multica terminal calls without the addendum-required explicit workspace flag. This is an instruction-adoption gap; it does not establish a foreign-scope access. Actual environment-bound scope, returned provider identities and explicit re-reads of essential references remain to be verified in the existing independent gates. Evidence: `validation/rc10/candidate-runtime/learning-c1-provider-scope-observation.json`. No capture/reuse PASS is inferred from deployment readback or the intake envelope.

## Learning proposal checkpoint — 2026-09-26T12:31Z

Curator C1 (`RC10VAL-12`, issue `01a0ddac-eb7a-7288-849a-eb75fb015eb0`) delivered proposal r1 in comment `01a0ddb0-37b4-7922-8e4d-454477d29f4a`. Canonical bytes were extracted from the actual comment and recomputed locally: SHA-256 `e24699c9e01d00027f6e28cf9af367d47b441a738c0db9c581fad731e61b6cb0`, 3591 bytes. Two conflictRefs remain unresolved; approval cannot be inferred. Independent scope/conflict review and verification remain pending, as do governed capture/replay and subsequent-Mission use.

Orchestrator accepted combining C4/C5/C6 into one sequential Curator work item (comment `01a0ddaf-c4a7-70e9-bbb6-95b0aabd2d79`), retaining all eight logical checks and independent pre-/post-write gates. This reduces dispatch overhead without dropping acceptance. The runtime reuse acceptance matrix and S02 Mission draft are prepared locally; neither is executed evidence.

## Learning independent-gate checkpoint — 2026-09-26T12:43Z

C2 Reviewer PASS (`01a0ddb5-4066-787d-9a80-e8d931230986`, RC10VAL-13) covers r1 content, scope, attribution and preserved conflicts. It is not an APPROVED knowledge decision. C3 Verifier run `01a0ddb6-2e69-7020-bdf3-d8354d8b6b5c` on RC10VAL-14 remains active; the original proposal digest is unchanged. Orchestrator explicitly acknowledged the conflict-free capture requirement in `01a0ddb8-4446-7571-86cc-43bbc61ecba6`; any context-only reclassification requires a new Curator revision/digest and fresh affected independent gates. Governance, capture and replay remain pending.

Scope follow-up: Orchestrator reports native MULTICA_WORKSPACE_ID binding and explicit exact-ID re-reads (`01a0ddb5-29ca-7ac8-8c5b-5c23a97db25f`). Supervisor independently confirmed four exact issue IDs return the expected workspace/project (`learning-exact-scope-readback.json`). This supports those observed responses, not a retroactive guarantee about every initial call. C3's first 16 observed calls also omit the explicit flag (`learning-c3-provider-scope-observation.json`); procedural conformance therefore remains open despite correct returned scope. Do not label the omission fully fixed.

## Learning r2 governance and capture checkpoint — 2026-09-26T13:20Z

Latest canonical r2 is the 4,122-byte proposal `5fba147a9bc32231fa3cea99d04eb7300390485de340bc1521cbd35f89b4176e`, originally published by Orchestrator in `01a0ddc4-05ca`. Curator explicitly selected these bytes and excluded its 4,230-byte variant `9358f1cd…` in reconciliation `01a0ddc7-bdb7`. Orchestrator's latest disposition `01a0dddd-2d32-75ae-baf2-b9498d40c32e` treats that exact selection as producer adoption, preserves original authorship, and supersedes its earlier contrary canonical disposition `01a0ddd4-2474`. Both variants and all decisions remain available; no historical bytes or verdicts were rewritten.

Actual independent Reviewer WARNING (`01a0ddc8-5eff`, author `53a77a8b…`) and independent Verifier VERIFIED (`01a0ddd3-97bc`, author `043dc4fa…`, run `01a0ddcd-3d0b-7dc3-ae46-305889037b53`) both bind the adopted `5fba…` bytes. Orchestrator-authored review-shaped comment `01a0ddca-5ff5` is excluded from independent gate counts. Governance decision `rc10wk-s01-govdecision-20260926-01` is APPROVED under the Mission's limited WORKSPACE delegation; this is not product-truth or production authority.

Curator capture run `01a0dddb-caa5-71de-9e6a-57970e8b98b2` started 13:16:04Z. Fresh provider observation found one WorkspaceKnowledge record, `RC10VAL-15` / `01a0ddde-0d24-7978-9af4-6ed850a5cbfc`, created by Curator in the configured project. At observed provider revision 6, proposal hash recomputes to `5fba…`, recordVersion is 2, governance binds the same key/version/hash, and the complete list reports has_more=false. This is record-existence/read evidence only: Curator replay/lost-ack receipts and independent postwrite gates remain pending. One listing does not prove atomic uniqueness or later-Mission reuse.

### Retained operational findings

- Child dispatch mentioned both assigned worker and Orchestrator, producing extra role runs and misleading review-shaped output. Supervisor corrections `01a0ddcb-c8e0` and `01a0ddcd-3c51` require actual author/run checks and only the assigned-worker actionable mention on child dispatch; the callback identity is plain text until the worker posts its event on the parent. The c4 dispatch uses this corrected routing.
- Earlier provider calls omitted explicit workspace flags; matching returned scope does not erase that conformance gap. The completed C3 r2 trace has 15 Multica tool-input blocks with flag text and zero without. These are block counts, not a universal invocation-level or historical compliance claim.
- Original dispatch deadline 13:04:57Z was inspected at 13:05:07Z. Supervisor continuation `01a0ddd4-a3ee` allows only the same Mission's remaining planned work until 13:45:00Z, with concurrency/child/correction/memory limits unchanged. Preserve full elapsed time and the original overrun. Orchestrator previously calculated a different original deadline, 13:03:24Z; report that discrepancy rather than claiming the Mission finished within 45 minutes.
- Candidate source/config snapshot was rechecked at 13:14:02Z: all 190 file hashes match `acaea4171d257754204aba8d1e5a1e267226bcadad0e779d6982c93567e89e2e`. No source changes were made during reconciliation. Final ZIP remains unsealed; later-Mission reuse and S02–S06 remain unaccepted.

Evidence under `validation/rc10/candidate-runtime/`: `learning-r2-authorship-inspection.json`, `learning-r2-review-authorship-comments.json`, `learning-canonical-disposition-parent.json`, `learning-capture-adoption.json`, `learning-first-record-observation.json`, `learning-c3-r2-scope-observation.json`, `learning-time-bound-inspection.json`, `learning-bounded-continuation-receipt.json`, and `candidate-source-recheck-learning.json`.

### Capture contract correction — 2026-09-26T13:36Z

Postwrite Verifier `01a0dde7-3c7d-79f4-91d3-550d8a1d11ed` declared v2 VERIFIED for the five dispatched record/replay assertions. It did not check the separate CaptureResult schema or resolve the shortened run reference; these were not cleared by that verdict. Its claim that workspace selection is unavailable to issue-list ignores the supported global option before subcommands; historical scope conformance remains qualified.

Curator correction 2/2 on RC10VAL-17 (run `01a0dde9-f3da-7509-a140-5bb69d5c39db`) delivered record v3 plus CaptureResult in comment `01a0ddec-63b0-75c3-9e72-67f5170cca59`, parent event `01a0ddec-9df5-7507-8570-77efb8ca41e1`. Fresh provider revision 10 shows the complete verifier run reference, matching record/index version 3, and unchanged proposal hash `5fba…`. Local Draft 2020-12 validation of the actual delivered CaptureResult against the existing schema passed; capturedAt equals provider created_at `2026-09-26T13:18:32Z`, and knowledgeRef identifies the observed record/revision. Evidence: `learning-record-v3-observation.json`, `learning-capture-result.json`, `learning-capture-result-validation.json`, and `learning-v3-delivery-parent.json`. These local checks do not replace explicit governance rebinding or the pending independent v3 verification/Reviewer gates. No later-Mission reuse PASS is claimed.

### v3 governance rebinding and final-gate dispatch — 2026-09-26T13:37Z

Orchestrator fan-in `01a0ddee-f2c2-7e47-989c-b1c7918b41fc` explicitly rebound the existing APPROVED decisionRef to v3 after fresh record revision 10 readback: same recordKey, recordVersion 3, unchanged proposal digest `5fba…`, complete evidenceRefs, matching scalar indexes. Curator delivery plus local schema/identity check remain WorkItemResult evidence. Final revision-bound gates are RC10VAL-18 C5b Verifier and RC10VAL-19 C6 Reviewer; neither result is accepted yet.

A fresh provider run inventory at 13:37:15Z found three concurrent RUNNING executions (parent Orchestrator + C5b + C6), exceeding the RUN-SPEC maximum of two including parent. Preserve all active runs and their results; freeze any further worker dispatch, record the overrun and remeasure before resuming. Do not report the parallel gate dispatch as within the resource/concurrency envelope. No cancellation or restart has been made. The 13:45Z stop-new-dispatch deadline remains in force.

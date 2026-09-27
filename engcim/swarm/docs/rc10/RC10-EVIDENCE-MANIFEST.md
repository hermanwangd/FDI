# RC10 Evidence Manifest

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
- Existing Java 17.0.20.1 Surefire reports contain 44 tests, zero failures/errors/skips, including source policy and RC6 compatibility. This checkpoint inspected report properties and hashes; it did not rerun tests or prove live runtime integration. Audit: `validation/rc10/candidate-runtime/s05-reuse-freshness-review/java17-report-audit.json`.

All five required reports and the implementation plan now share the current checkpoint for package snapshot RC10-PACKAGE-SNAPSHOT-20260927-02. The documentation and their import-manifest digest pairs were reconciled before the fresh tests and archive rebuild. Prior run receipts and bounded Phase 1 acceptance remain bound to implementation candidate RC10-local-candidate-20260927-01. The external PHASE1-PACKAGE-RESEAL-RECEIPT.json binds the new manifest, archive, and audit results. No production merge/deployment/promotion or aggregate Human DONE is authorized or claimed.


## Superseded checkpoint — previously current candidate and evidence set — 2026-09-27 (Asia/Taipei)

**Status:** `RC10_IMPLEMENTED_WITH_BLOCKERS` (candidate review state only; no release, promotion, Human DONE, or production authorization).

| Evidence | Bound identity / reference | Result |
|---|---|---|
| Candidate input manifest | `validation/rc10/candidate-runtime/s05-profile-deployment-20260926T1821Z/candidate-input-manifest.json`; SHA-256 `902a5e97d3de9185ea22a36687823a4e1997d7e2d13c7c8e2f48f0b1a0ec4267`; 204 files; Git HEAD `66dcc0d1316f8179a81b4c4564a821c574b7a54b` | 204/204 hashes match at readback |
| Human S05 profile decision | `validation/rc10/candidate-runtime/s05-profile-adoption.json`; source `ENGCIM-S05-PROFILE-DECISION-AND-MINIMUM-CHANGES-v0.1.md`, SHA-256 `145cfa3fc1b00bf25f533052478970979a7cf056ce23440d2c47436d5300f449` | `ADOPTED`, D01–D08, selected candidate only |
| Profile source | `engcim/bootstrap/overlays/multica/RC10-S05-S06-ROLE-GUIDANCE.md`, SHA-256 `a09e9fbd2cbeecceede25c9d80612002a1c6369367450422986bdb208bcbe4e9` | Candidate-scoped guidance |
| Configured role readback | `validation/rc10/candidate-runtime/s05-profile-deployment-20260926T1821Z/deployment-readback.json`, SHA-256 `675132e455c0baa8f222a1514399f980e78fdbd81e25f32bad32d0251d0fdd0b` | Orchestrator, Architect, Reviewer instructions matched; preserved settings |
| S05 C1/C2/C3 | RC10VAL-30 comment `01a0df11-66a0-788e-a316-0735baaf9fbb`; C1 `01a0df0a-591e-71df-8832-b67e3a7c1c79`; C2 PASS `01a0df0f-6949-7bef-b74c-2b4d00991b44` | Pre-coding work only; no coding authorization or dispatch |
| S06 QA / Verifier | RC10VAL-34 comment `01a0df1d-fb49-78ea-83fa-427a5ba6a036`; RC10VAL-35 comment `01a0df23-1514-73b5-b5f5-52c00b972850` | Pinned baseline evidence; hashes and observations independently reproduced |
| S06 independent review / integration | Reviewer PASS `01a0df29-1bcd-7c9e-89c3-91ff7d64daef`; parent final report `01a0df2a-e992-7ec5-9a0f-aa9c5f61a626` | AC5 adjudicated PASS per IntentSpec; parent prose has one conflicting summary phrase |
| Java 17 module tests | engcim/swarm/pom.xml, Java 17.0.20.1, one Maven process / one test fork, 1.5 GiB Maven + 2 GiB test JVM, two active processors | 45 tests, 0 failures/errors/skips; CompanyImportManifestTests and JavaOnlySourcePolicyTests pass 1/1 each |
| Captured Java test output | `validation/rc10/candidate-runtime/s05-profile-deployment-20260926T1821Z/final-verification/java17-maven-test.log`, SHA-256 `ffda796ff54885b71261c7605970e7ccecceeeb5ba4fe422b3fe704c34e5dc01` | Raw final module-suite output |
| Packaging checks | `release/VERIFICATION-SUMMARY.json`; 14 standalone governance tests; standalone verifier | 14/14 tests and 61/61 verifier checks passed; Python source compilation passed after excluding AppleDouble resource forks |
| Candidate archive readback | release/RC10-CANDIDATE-PACKAGE.zip compared against release/MANIFEST.json | Package snapshot RC10-PACKAGE-SNAPSHOT-20260927-02: 1,713 entries exactly match 1,712 manifest files plus the embedded manifest; exact hashes/sizes and CRC PASS; local control and AppleDouble state excluded. Archive digest is in the external package-reseal receipt. |

The full input snapshot is immutable for this candidate; QA evidence attachments are stored under `validation/rc10/candidate-runtime/s05-profile-deployment-20260926T1821Z/s06-qa-evidence/`. These output receipts are separate from the 204-file input manifest. S01–S04 evidence belongs to `RC10-local-candidate-20260926-01` and is not silently promoted to current-candidate validation. S06 is a pre-implementation baseline diagnostic, not an S06 PASS.

## Historical checkpoint — 2026-09-26T17:20:27Z

This pre-adoption checkpoint is retained as historical evidence; the current evidence set above supersedes it.

- S01 candidate gate passed at r3; its issue remains `in_review`. Learning Mission RC10VAL-11 completed its required v3 capture/review/verification gates (final fan-in `01a0ddf4-5a30-7117-97d1-98f1fc86daa4`); its issue also remains `in_review`. The original time-budget overrun and concurrency overrun remain findings. Later-Mission runtime retrieval/use is still unverified.
- S02 required technical gates were recorded complete only after the fixed 14:53Z stop-bound, with the later Verifier result as an addendum to the single stop-bound report. That Verifier reported all seven checks reproduced without requiring the missing Stage A bytes; this does not change the Supervisor's recorded no-waiver attribution correction. The issue remains `in_review`; actual WorkspaceKnowledge receipt consumption inside the Curator run is not independently verified, and scope/routing findings remain.
- S03 revision 1 relation analysis received Reviewer PASS and Verifier VERIFIED; its no-edge result stands, with concurrency-control findings retained. S04 IntentSpec revision 1 received PASS and VERIFIED; its workspace-scope and concurrency findings remain. Both issues remain `in_review`.
- The current issue inventory contains no S05 or S06 issue, and S04 did not dispatch them. S05/S06 remain HOLD / NOT RUN. `in_review` is not Human DONE. Do not treat S04's supplied S02 receipt as proof of runtime WorkspaceKnowledge retrieval/reuse.

## Historical candidate runtime checkpoint — 2026-09-26T12:20:47+00:00

Local Supervisor is **Codex CLI**; company Supervisor is **Claude CLI**. Both use the existing Multica/Swarm path, without a second dispatcher or per-CLI adapter. The company profile is not a prerequisite for this local validation.

Candidate input snapshot: `acaea4171d257754204aba8d1e5a1e267226bcadad0e779d6982c93567e89e2e`. Deployment readback matched both existing-role instructions and preserved Skills/model/runtime/permissions. Actual Orchestrator and Curator runs returned the matching deployment envelope. Evidence: `validation/rc10/candidate-runtime/deployment-verified.json` and the S01 receipts.

Candidate S01 `RC10VAL-10` (`01a0dd73-7d75-7295-aeb1-a83905eb2312`) was dispatched by Codex CLI to the existing squad. r1 received REVISE; r2 independently received Reviewer REVISE and Verifier FAILED for `NESTED_TRUST_UNSUPPORTED`. Existing fan-in prevented promotion and dispatched the final allowed correction. Curator delivered r3 (`57af68f6f63561e181df6edab6d0adea160620b78c058cf5c610b296581411ba`), with downloaded bytes verified. Independent Reviewer PASS and Verifier VERIFIED were consolidated by Orchestrator in final fan-in `01a0dda2-edf9-7a88-91c9-d8bc1bdab15e`: S01 gate PASSED at r3; Human DONE remains separate.

Codex CLI prepared schema-shaped S01 MissionLearningSource and dispatched learning Mission `RC10VAL-11` (`01a0dda7-01b9-76f7-b53b-e2e629a042c2`) to the existing squad. Leader run `01a0dda8-68d4-72db-9408-0da6332c53fd` is confirmed running; capture/governance/replay results are not yet verified. S02–S06 and subsequent-Mission reuse remain NOT_RUN. Final archive resealing and formal Phase 1 handoff remain pending. `validation/rc10/candidate-runtime/runtime-checkpoint.json` records subsequent observations. Earlier dated checkpoints are history, not current completion claims.

## Candidate identity

- Candidate: `RC10-local-candidate-20260927-01`
- Status: `RC10_IMPLEMENTED_WITH_BLOCKERS`
- Branch / HEAD: `codex/fdi-rc10-implementation` / `66dcc0d1316f8179a81b4c4564a821c574b7a54b`
- Current runtime/source input manifest: `validation/rc10/candidate-runtime/s05-profile-deployment-20260926T1821Z/candidate-input-manifest-r2.json`; SHA-256 `a89ea0e608f48d59c6161d3d4e54ad105d1c24e2b383c352f90ebcd3ba6b0962`; 204 entries
- Prior manifest r1: SHA-256 `902a5e97d3de9185ea22a36687823a4e1997d7e2d13c7c8e2f48f0b1a0ec4267`; preserved as the binding identity for existing Multica run receipts
- r1→r2 applicability audit: `candidate-r1-to-r2-applicability-audit.json`; only two Java regression-test files changed, runtime and instruction inputs unchanged
- Worktree: `/Users/herman_mbp2023/.codex/worktrees/fdi-rc10-implementation/Feature-Delivery-Intelligence`
- RC6 runtime baseline: tag `engcim-swarm-rc6-runtime-baseline-v1`, commit `68f010eeb21c14ea14d7bc5605be06a17bcb7920`
- Canonical RC6 archive: SHA-256 `a13b20414dddc7290232b4d1289c7c4e905f2156a6c77cc31c282da9a8a723e0`
- Local Supervisor CLI: Codex CLI; company Supervisor CLI: Claude CLI, not exercised in this local environment

## Required deliverables

| Deliverable | Path |
|---|---|
| RC6 to RC10 gap report | `engcim/swarm/docs/rc10/RC6-TO-RC10-GAP-REPORT.md` |
| RC10 implementation report | `engcim/swarm/docs/rc10/RC10-IMPLEMENTATION-REPORT.md` |
| RC10 integration report | `engcim/swarm/docs/rc10/RC10-INTEGRATION-TEST-REPORT.md` |
| RC10 regression report | `engcim/swarm/docs/rc10/RC10-REGRESSION-REPORT.md` |
| Evidence manifest | `engcim/swarm/docs/rc10/RC10-EVIDENCE-MANIFEST.md` |
| Implementation plan | `engcim/swarm/docs/rc10/RC10-IMPLEMENTATION-PLAN.md` |
| Source-to-runtime gap matrix | `engcim/swarm/docs/rc10/RC6-TO-RC10-SOURCE-TO-RUNTIME-GAP-MATRIX.md` |
| Runtime materialization record | `engcim/swarm/docs/rc10/RC10-RUNTIME-MATERIALIZATION.md` |
| Candidate package index and archive | `engcim/swarm/release/RC10-CANDIDATE-PACKAGE/README.md`; `release/RC10-CANDIDATE-PACKAGE.zip` |

The final candidate ZIP SHA is captured after archive creation and reported with the artifact. The archive is excluded from the import-manifest hash because it embeds that manifest; storing its own hash inside the ZIP would be self-referential.

## Local verification

| Check | Result | Binding |
|---|---|---|
| Java 17 Maven module suite | 45 tests, 0 failures/errors/skips | Fresh run after documentation/import-manifest reconciliation; raw log SHA-256 is recorded in the external package-reseal receipt. |
| RC6 canonical package verifier | 25 PASS, 0 FAIL, 4 optional external checks NOT VERIFIED | Sealed RC6 baseline archive. |
| RC6 canonical package self-test | PASS | Sealed RC6 baseline archive. |
| Standalone governance tests | See `release/VERIFICATION-SUMMARY.json` | Bound to this report/package revision. |
| Standalone bundle verifier | 61 PASS / 0 FAIL; see release/VERIFICATION-SUMMARY.json | Fresh rerun on the refreshed release inputs passed; raw log SHA-256 is recorded in the external package-reseal receipt. |
| Candidate ZIP/package exclusion checks | release/VERIFICATION-SUMMARY.json and external package-reseal receipt | Exact archive entry set, per-file hash/size, embedded manifest, CRC, and control-file exclusions passed. |

## Multica baseline evidence

| Field | Value |
|---|---|
| Isolated validation workspace | `ENGCIM Swarm RC10 S01-S06 Validation`, ID `0b02adb6-a395-46bd-bd92-6fec14dee20e` |
| Workspace-scoped projects | Validation `a3f129fa-4028-4341-98dc-c8ec20c468ae`; ProductKB `f7af4546-88b2-4163-a0d4-b350e2123dbc`; WorkspaceKnowledge `43aec4ec-3ebb-4d1b-aa54-7766c48379c1` |
| WorkspaceKnowledge pre-candidate state | Project `planned`; 0 issues; 0 custom property definitions. Read-only Codex CLI inventory used the explicit workspace ID. |
| Existing record operations | CLI help confirms issue create/get/list/update, per-issue metadata set/get/list/delete, property set/list/unset and comments. Capability evidence only; no record was created or updated. |
| Baseline S01 issue | `RC10VAL-4`, `01a0dd0f-2eea-7180-a1d3-43e65e790662`, `in_review` at revision 34 |
| Curator delivery | Run `01a0dd10-b0e1-772c-97cc-d3106c8f41e3`; delivery comment `01a0dd1d-5571-75ff-97e6-e9e9c062f01e` |
| Independent review | Reviewer `01a0dd1e-6224-7895-83c5-ea82808f353e`, PASS for revision 1 |
| Independent verification | Verifier `01a0dd1e-622d-7e4d-95a8-46f298ead051`, VERIFIED for revision 1 |
| RC6 verifier report | `validation/rc10/rc6-baseline-2026-09-26/RC6-MULTICA-VERIFICATION-REPORT.md`, SHA-256 `6ba09e95c6c8e01065a9ee5aeee6199a6999992e71a7f65a7983c6fdd009dc5a` |
| S01 Product Knowledge snapshot | `validation/rc10/rc6-baseline-2026-09-26/pk-store-s01-rc10val4.tar.gz`, SHA-256 `e6881697523af6373f4088fa28c04f9cab121ae2fd0ea789187e9fa6110ca6c6`; canonical RC6 schema validator reported 43 checks passed |

The baseline S01 passed delivery, Reviewer and Verifier gates but remains open for Human acceptance. It is RC6 baseline evidence only; current-candidate S01–S06 evidence is recorded in the checkpoint above.

## Validation boundary

- Candidate validation is limited to the isolated Multica workspace and approved pinned fixtures recorded in the implementation plan.
- Local Supervisor uses Codex CLI; Claude CLI applies only in the company environment.
- No production workspace, production branch, promotion or user rollout is in scope.
- Do not infer PASS from source code, static checks, issue state/comments or the RC6 baseline.
- Preserve S01–S06 issue state as `in_review`; technical gates do not mean Human DONE.
- Package snapshot `RC10-PACKAGE-SNAPSHOT-20260927-02` was validated after the report and import-manifest digest refresh; results and hashes are bound in the external `PHASE1-PACKAGE-RESEAL-RECEIPT.json`. Any later change to package inputs requires a new manifest, test and archive audit.

## Historical candidate source checkpoint — 2026-09-26T19:15:58+08:00

Current source/contract digest is SHA-256 of `validation/rc10/candidate-runtime/source-contract-manifest.json` (89 file entries). The broader deployment snapshot is `candidate-input-manifest.json`, SHA-256 `acaea4171d257754204aba8d1e5a1e267226bcadad0e779d6982c93567e89e2e`: 190 files covering dirty source/contracts, bootstrap configuration, original live role/Skill content and the bounded run specification. The existing-role overlay and loading/rollback procedures are now implemented in candidate source; runtime acceptance remains separate. Fresh Java 17 module validation passed 44 tests, 0 failures/errors/skips, including source policy and import-manifest validation. The prior release ZIP is superseded pending final reseal against new evidence.

## Current-candidate S05/S06 acceptance evidence

S05 profile adoption and configured role readback are recorded in validation/rc10/candidate-runtime/s05-profile-adoption.json (SHA-256 8ade042327de65549f9a2f02c7c70915f1bf2f2c70cd81459a6c0cca7c0b9361) and deployment-readback.json. The readback binds profile ENGCIM-S05-REVIEWED-DELIVERY-v0.1, source SHA-256 145cfa3fc1b00bf25f533052478970979a7cf056ce23440d2c47436d5300f449, and existing Orchestrator/Architect/Reviewer configured-instruction digests. Some actual run output self-reports profile-clause use with matching behavior, but exact per-run loaded bytes/digest are not captured; consumption remains UNVERIFIED.

Current fixture chain: 2eff5f9f84ca709684bfe0b7c90102268f07a0f0 → 3be28d44fa1e9fcdcfb48d169f47672e373cf4c9 → 0159b44d9e89bb06e1e2d72b752c4b9cb6e5685e. The final correction is the authorized one-line chartViewer.js max 1000→10 change; tests and interaction.js are unchanged. Independent Code Review PASS is RC10VAL-63 comment 01a0e103; S06 VERIFIED is RC10VAL-64 comment 01a0e10c; final QA PASS is RC10VAL-65 comment 01a0e120. All three bind to the full corrected commit. F1 is closed for that commit; issue states remain in_review.

Non-blocking reconciliation notes: the Code Review's “Swarm Verifier fix run” attribution is inaccurate; the mutation actor was Codex task 01a0dc67-960f-7100-972d-557464f7c1fd, with Herman Wang as Git author/committer and multica-agent as co-author. Reviewer and Verifier were later separate actual runs. S06-F1-REVALIDATION.json exists in this primary worktree with SHA-256 ed438bbc1d979a597c553de58a2ee3142b718dc414ff95990cf84ce9b1a28141, although the final QA run's separate checkout could not resolve its validation/ path. RC10VAL-74 T13 attempt 2 is VERIFIED_WITH_FINDINGS; parent RC10VAL-72 remains in_review with three supporting findings. None of these facts establishes aggregate S01–S06 PASS or Human DONE.

## Current-candidate S01 acceptance evidence

Current candidate S01 technical gates are indexed by RC10VAL-37 r53, with final report and independent Reviewer/Verifier records; the issue remains `in_review`. The later-Mission reuse case is RC10VAL-47 r70: W3 actually read the AVC and classified it read-only, Reviewer PASS, Verifier 6/7 with one envelope limitation. W1/W2 are canceled/invalid and excluded. These records do not prove causal influence or full T13 negative eligibility coverage.

## Learning Mission dispatch evidence — 2026-09-26T12:20:47Z

- `validation/rc10/candidate-runtime/learning-dispatch-summary.json`: actual issue/squad/leader ACK, uploaded input IDs, input hashes and command outcomes. Mission `RC10VAL-11`; source S01 is gate-passed at r3.
- `s01-mission-learning-source.json` and `s01-supervisor-closure-summary.md` in that directory: Codex CLI-produced evidence-bound inputs, not a Swarm proposal or approval.
- `learning-live-runs-20260926T122047Z.json`: fresh active learning-run observation. `learning-current-runs-reobservation.json` explains the recovered S01 receipt filename and its later observation time; do not misattribute the recovered bytes to the original preflight time.
- `learning-workspaceknowledge-before.json`: at 12:20:19Z the scoped project had zero issues and has_more=false. This is before capture, after Mission dispatch; not a successful knowledge write.

Capture, replay, governance and subsequent fresh-context use remain pending actual run results. No final candidate archive or Phase 1 handoff is asserted.

# RC10 S01 WorkspaceKnowledge learning capture — candidate 20260927-01

Status: prepared for a separate S01 Mission in the existing isolated MultiCA validation workspace.

## Candidate and source binding

- Candidate: `RC10-local-candidate-20260927-01`.
- Candidate input manifest: `validation/rc10/candidate-runtime/s05-profile-deployment-20260926T1821Z/candidate-input-manifest.json`, SHA-256 `902a5e97d3de9185ea22a36687823a4e1997d7e2d13c7c8e2f48f0b1a0ec4267`. Do not attach the stale root-level manifest (`acaea417…`, candidate 20260926-01).
- Product Knowledge source Mission: RC10VAL-37 (`01a0df4e-032b-733c-926b-68bb722fcece`), revision 53, `in_review`; final fan-in comment `01a0df5e-80c7-7e21-a122-8c2704c3ba66` says the candidate PK gate passed at revision 1 and leaves Human DONE untouched.
- Exact candidate store archive: `pk-store-s01-rc10val37-candidate.tar.gz`, SHA-256 `4c4125269788178dba6a605952761cba94b4deeb1d50d10dcfd382cadab8b867`.
- Curator report: RC10VAL-38 `01a0df50-0e67-7971-bd40-1b0f62f09965`, delivery comment `01a0df52-3940-735a-b68b-56ee5e6c142b`.
- Independent Reviewer: RC10VAL-39 `01a0df54-8ef7-7da9-a159-e22150a02e23`, PASS@revision 1 comment `01a0df57-8741-7a09-a262-ee4d94ce2e0a`, run `01a0df54-8f0d-7527-86d5-350f4228cbd5`.
- Independent Verifier: RC10VAL-40 `01a0df59-9365-7282-9f0c-b1321ff2d6f1`, VERIFIED@revision 1 comment `01a0df5c-a57e-73d2-a902-57960ea80987`, run `01a0df59-9381-790e-aac1-4100ec92c4f2`.
- The attached MissionLearningSource is bound to the exact S01 source, closure comment, producer/reviewer/verifier comments and runs, candidate digest, validator output, and fixture test output. It is not a product-truth assertion.

## Isolated execution envelope

- MultiCA workspace: `0b02adb6-a395-46bd-bd92-6fec14dee20e` only.
- Validation project: `a3f129fa-4028-4341-98dc-c8ec20c468ae`.
- WorkspaceKnowledge project: `43aec4ec-3ebb-4d1b-aa54-7766c48379c1`.
- ProductKB project: `f7af4546-88b2-4163-a0d4-b350e2123dbc` is read-only context; no ProductKB writes.
- Local Supervisor: Codex CLI. Do not invoke Claude CLI.
- Use the existing squad `ba1c9f0d-00fd-48a3-8865-dfbc4ff35f73`, existing roles, routing, Skills and provider mapping. Add no adapter, Scenario, agent, Skill, schema, custom property, service or retry engine.
- Every provider command must explicitly specify `--workspace-id 0b02adb6-a395-46bd-bd92-6fec14dee20e`. Resolve the WorkspaceKnowledge project from this envelope and freshly verify its workspace before any read/write.
- Memory ceiling: 8 GiB total process RSS. At most two active Swarm runs including the parent, one child at a time, independent Reviewer and Verifier sequentially. Stop new dispatch at 45 minutes; preserve any already-running child and report actual run handles.

## Adopted profile and evidence limits

- Selected profile: `ENGCIM-S05-REVIEWED-DELIVERY-v0.1`, D01–D08, adoption receipt SHA-256 `8ade042327de65549f9a2f02c7c70915f1bf2f2c70cd81459a6c0cca7c0b9361`.
- Profile source SHA-256: `a09e9fbd2cbeecceede25c9d80612002a1c6369367450422986bdb208bcbe4e9`.
- Existing Workspace Learning addendum SHA-256: `2115a331595d9ced7a4da11ae57dd6df264fa454a7edc7272b226b30629f7ead`.
- Deployment readback: `validation/rc10/candidate-runtime/s05-profile-deployment-20260926T1821Z/deployment-readback.json`, SHA-256 `675132e455c0baa8f222a1514399f980e78fdbd81e25f32bad32d0251d0fdd0b`. Fresh provider readback confirms configured instruction hashes: Orchestrator `b3b29cd7…b472`, Architect `e6c8861b…c6a9`, Reviewer `16d15bc3…1964`, Curator `7662c744…6bff`.
- D07 scopes the S05 excerpts to the existing Orchestrator, Architect and Reviewer instruction fields. Preserve the existing Curator Workspace Learning instructions and all role settings.
- Configured readback proves persisted configuration only. Actual per-run profile/addendum consumption is `UNVERIFIED` unless the provider exposes evidence for the effective loaded bytes. Do not infer consumption from a role's self-report or from a passing result.
- The S01 producer accurately disclosed that its run envelope named `RC10-local-candidate-20260926-01` while the parent mission and attached archive are bound to `20260927-01`; Reviewer and Verifier accepted this as a non-blocking disclosed finding. Preserve both identities and do not repair or hide the mismatch in this capture Mission.
- Candidate PK validator exited 0 with 44 records/nodes/edges. The pinned fixture `node --test` exited 1 because `openSelectedChart` is missing. These are separate outcomes: validator success is not software PASS; the fixture defect remains unresolved. `chartLimits().max` 10 vs 1000 and `openSelectedChart` vs `selectChart` remain unadjudicated Product Context conflicts in RC10VAL-8 and RC10VAL-9.
- S01 is `in_review`, not Human DONE. The candidate store's fixture observations remain provisional/conflicting; do not promote them to Product truth.

## Learning goal

Use the existing RC10 Workspace Learning procedure to derive at most one useful **WORKSPACE / PROCEDURAL** lesson from the actual candidate-bound S01 evidence. The likely subject is how to preserve mission/candidate/run-envelope identities and report validator, fixture, review, verifier, and runtime-consumption evidence without promoting one result into another. The Curator must determine whether that lesson is supported and materially distinct from existing WorkspaceKnowledge; this suggested subject is not a required conclusion.

Before proposing persistence, freshly page and read current WorkspaceKnowledge records for both exact-key and semantic duplicates. RC10VAL-15 (`01a0ddde-0d24-7978-9af4-6ed850a5cbfc`) is a locator for prior-candidate record key `rc10wk-s01-verified-slice-procedure-20260926-01`, provider revision 10/version 3 at the last Supervisor read; it is not presumed current or applicable. Treat local snapshots as locators only. If the same content already exists, return its exact provider identity and do not create a duplicate. If a material current-candidate update is justified, preserve the old body/decision and use the existing versioning rules. If the new source adds no distinct supported lesson, or source/authority/evidence is insufficient, return `DEFERRED` rather than force a new record.

## Bounded governance delegation

This Mission is authorized by the active Human RC10 objective to validate the end-to-end Mission and Learning/Reuse path in this exact isolated workspace. That authorization permits only synthetic/workspace procedural learning from the attached S01 evidence. It does not pre-approve any proposal and does not authorize ProductKB, fixture, code, Skill, Control, Core, runtime, production or foreign-workspace changes.

The existing Orchestrator may record `APPROVED`, `REJECTED` or `DEFERRED` only after an independent Reviewer and an independent Verifier have evaluated the exact canonical proposal digest and the source, workspace/project scope, applicability, limitations, duplicate status and conflicts. Bind any decision to the exact record key/version, proposal digest, policy reference, actor and evidence references. Missing authority/evidence, a material unresolved contradiction, or a failed independent gate means `DEFERRED` or `REJECTED`, not approval. The Mission issue's execution delegation is the policy reference for this bounded synthetic test; it is not a general governance policy.

## Required sequence and acceptance

1. Orchestrator freshly verifies the candidate manifest, MissionLearningSource schema and all referenced S01 records in the explicit workspace; classify run-envelope mismatch and runtime consumption as limitations.
2. Existing Knowledge Curator inspects current records and produces an existing-schema WorkspaceKnowledgeProposal only if it contains a distinct, evidence-supported procedural lesson. Preserve the two Product Context conflicts as background references; assert no side as Product truth. Do not change ProductKB.
3. Independent Reviewer reviews the exact proposal bytes/digest for attribution, usefulness, scope, applicability, duplicate handling, limitations and absence of prohibited mutations. Independent Verifier separately recalculates the digest and validates the existing schema and evidence links. Run them sequentially; bind both results to the same revision/digest.
4. Orchestrator records the bounded decision after those gates. A returned `DEFERRED`/`REJECTED` result is valid evidence; no successful capture may be claimed from it.
5. For an approved proposal, Curator uses the existing issue-backed mapping to persist or version it, then reads back body and scalar indexes afresh and returns the existing `WorkspaceKnowledgeCaptureResult` plus the existing evidence envelope. Supervisor does not create/update the record.
6. Replay the identical capture request and prove it returns the same provider identity/content version with no duplicate. Perform only the existing controlled lost-acknowledgement reconciliation procedure: query by record key/content before any retry and label it a simulation, not an observed transport failure or provider-enforced uniqueness.
7. Supervisor independently read-verifies the final provider body/indexes, proposal digest, record key/version, decision binding and capture result. Publish one fan-in report with exact refs, hashes, findings and limitations. Leave this Mission `in_review`; do not mark Human DONE or launch S02 automatically.

Use only the attached existing `MissionLearningSource`, `WorkspaceKnowledgeProposal` and `WorkspaceKnowledgeCaptureResult` schemas and `RC10-WORKSPACE-LEARNING.md`. All failures and revisions remain visible. No source/fixture/product statement may be silently edited or promoted.

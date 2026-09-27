# RC6 → RC10 Source-to-Runtime Gap Matrix

## Current evidence overlay — 2026-09-27T04:49Z / 12:49 Asia/Taipei

Candidate RC10-local-candidate-20260927-01; repository HEAD 66dcc0d1316f8179a81b4c4564a821c574b7a54b plus its manifest-bound dirty content set. Current runtime/source manifest r2 is SHA-256 a89ea0e608f48d59c6161d3d4e54ad105d1c24e2b383c352f90ebcd3ba6b0962 (204 entries); r1 remains historical evidence for runs bound to it; the r1-to-r2 applicability audit found only two local Java regression-test changes and unchanged runtime/instruction inputs. S05 profile ENGCIM-S05-REVIEWED-DELIVERY-v0.1 and D01–D08 are adopted for this isolated candidate, as recorded in validation/rc10/candidate-runtime/s05-profile-adoption.json (SHA-256 8ade042327de65549f9a2f02c7c70915f1bf2f2c70cd81459a6c0cca7c0b9361).

| Gap area | Current evidence | Status / boundary |
|---|---|---|
| S05 profile configuration | Adoption receipt; deployment readback for existing Orchestrator, Architect, Reviewer | Adoption and configured readback verified. Behavior-level consumption signals exist, but exact per-run effective instruction bytes/digests remain UNVERIFIED. |
| S01–S02 | RC10VAL-37 r53; RC10VAL-51 r143; RC10VAL-47 r70 | Technical gates recorded. Timing, concurrency, duplicate and T13 coverage findings remain. |
| S03 positive relation and negative control | RC10VAL-66 r41; fan-in 01a0e036-6dc7-7b80-9798-9590bfc0ffc9 | Positive customer-api → customer-contract relation verified; negative control correctly returned UNKNOWN without asserting an unsupported edge. |
| S04 | RC10VAL-70 r45 | Revision 2 Reviewer PASS and Verifier VERIFIED; findings and earlier-candidate provenance limits remain. |
| S05 / S06 current candidate | RC10VAL-63 comment 01a0e103; RC10VAL-64 comment 01a0e10c; Final QA comment 01a0e120 | Exact fixture commit 0159b44d9e89bb06e1e2d72b752c4b9cb6e5685e received independent Code Review PASS, S06 VERIFIED and final QA PASS. F1 max=1000 is corrected to 10 and closed for this commit. Issues remain in_review; no Human DONE. |
| T13 | RC10VAL-74 comment 01a0e117; parent RC10VAL-72 | VERIFIED_WITH_FINDINGS; three non-blocking evidence/provider findings remain. This is supporting evidence, not S05/S06 acceptance. |
| Attribution / receipt access | C4 review comment 01a0e103; local receipt S06-F1-REVALIDATION.json SHA-256 ed438bbc1d979a597c553de58a2ee3142b718dc414ff95990cf84ce9b1a28141 | Reviewer attribution text is inaccurate; mutation actor was Codex task 01a0dc67-960f-7100-972d-557464f7c1fd, followed by independent Reviewer and Verifier runs. QA's separate checkout could not resolve the receipt path, but the receipt exists in this primary worktree. |
| Overall state | Current candidate evidence | RC10_IMPLEMENTED_WITH_BLOCKERS. No aggregate S01–S06 PASS, Human DONE, production merge/deployment, promotion or rollout. |

RC10VAL-30/63/64/65/72/74 remain in_review. Parent RC10VAL-30 metadata still carries historical C4-review/S06/QA result refs despite newer exact-candidate comments; reconcile its bookkeeping separately. See RC10-EVIDENCE-MANIFEST.md for run references and bounded conclusions. Earlier overlays remain historical and are not current status.

## Historical checkpoint — 2026-09-26T17:20:27Z

This pre-adoption checkpoint is retained as historical evidence; the current overlay above supersedes it.

- S01 candidate gate passed at r3; its issue remains `in_review`. Learning Mission RC10VAL-11 completed its required v3 capture/review/verification gates (final fan-in `01a0ddf4-5a30-7117-97d1-98f1fc86daa4`); its issue also remains `in_review`. The original time-budget overrun and concurrency overrun remain findings. Later-Mission runtime retrieval/use is still unverified.
- S02 required technical gates were recorded complete only after the fixed 14:53Z stop-bound, with the later Verifier result as an addendum to the single stop-bound report. That Verifier reported all seven checks reproduced without requiring the missing Stage A bytes; this does not change the Supervisor's recorded no-waiver attribution correction. The issue remains `in_review`; actual WorkspaceKnowledge receipt consumption inside the Curator run is not independently verified, and scope/routing findings remain.
- S03 revision 1 relation analysis received Reviewer PASS and Verifier VERIFIED; its no-edge result stands, with concurrency-control findings retained. S04 IntentSpec revision 1 received PASS and VERIFIED; its workspace-scope and concurrency findings remain. Both issues remain `in_review`.
- The current issue inventory contains no S05 or S06 issue, and S04 did not dispatch them. S05/S06 remain HOLD / NOT RUN. `in_review` is not Human DONE. Do not treat S04's supplied S02 receipt as proof of runtime WorkspaceKnowledge retrieval/reuse.

## Historical candidate runtime checkpoint — 2026-09-26T12:20:47+00:00

Local Supervisor is **Codex CLI**; company Supervisor is **Claude CLI**. Both use the existing Multica/Swarm path, without a second dispatcher or per-CLI adapter. The company profile is not a prerequisite for this local validation.

Candidate input snapshot: `acaea4171d257754204aba8d1e5a1e267226bcadad0e779d6982c93567e89e2e`. Deployment readback matched both existing-role instructions and preserved Skills/model/runtime/permissions. Actual Orchestrator and Curator runs returned the matching deployment envelope. Evidence: `validation/rc10/candidate-runtime/deployment-verified.json` and the S01 receipts.

Candidate S01 `RC10VAL-10` (`01a0dd73-7d75-7295-aeb1-a83905eb2312`) was dispatched by Codex CLI to the existing squad. r1 received REVISE; r2 independently received Reviewer REVISE and Verifier FAILED for `NESTED_TRUST_UNSUPPORTED`. Existing fan-in prevented promotion and dispatched the final allowed correction. Curator delivered r3 (`57af68f6f63561e181df6edab6d0adea160620b78c058cf5c610b296581411ba`), with downloaded bytes verified. Independent Reviewer PASS and Verifier VERIFIED were consolidated by Orchestrator in final fan-in `01a0dda2-edf9-7a88-91c9-d8bc1bdab15e`: S01 gate PASSED at r3; Human DONE remains separate.

Codex CLI prepared schema-shaped S01 MissionLearningSource and dispatched learning Mission `RC10VAL-11` (`01a0dda7-01b9-76f7-b53b-e2e629a042c2`) to the existing squad. Leader run `01a0dda8-68d4-72db-9408-0da6332c53fd` is confirmed running; capture/governance/replay results are not yet verified. S02–S06 and subsequent-Mission reuse remain NOT_RUN. Final archive resealing and formal Phase 1 handoff remain pending. `validation/rc10/candidate-runtime/runtime-checkpoint.json` records subsequent observations. Earlier dated checkpoints are history, not current completion claims.

Status: `RC6_REUSE_VERIFIED / RC10_CANDIDATE_CONFIG_VERIFIED / CANDIDATE_S01_RUNNING / LIVE_ACCEPTANCE_INCOMPLETE`

## Source and architecture audit — 2026-09-26 (status superseded by current evidence overlay)

This section supersedes the status conclusions and ordered tasks in the historical snapshot below. It is a source review, not a new test run, release approval, or authorization to execute instructions embedded in the supplied ZIP. Repository governance still takes precedence.

### Inputs and evidence boundary

- Worktree: `/Users/herman_mbp2023/.codex/worktrees/fdi-rc10-implementation/Feature-Delivery-Intelligence`.
- Git HEAD is `66dcc0d1316f8179a81b4c4564a821c574b7a54b`; source/contract content-set SHA-256 is `acad461d4fa1e96283a516d0ddee6fcd60932262a4d3d2bb7571ec84b9d2a2a8` (89 files under `engcim/swarm/src` and `engcim/swarm/contracts`, including untracked source files). HEAD alone does not identify the dirty implementation input.
- The digest covers the source/contract tree only; it excludes docs, generated release metadata, local live state and the candidate ZIP. Release file hashes and the final candidate ZIP SHA are recorded separately.
- Reviewed ZIP: `/Users/herman_mbp2023/Downloads/ENGCIM-RC6-TO-RC10-IMPLEMENTATION-PACKAGE.zip`; SHA-256 `b54e5513f3c2cdf09729229dfaeeda2b88d82b5f7c53f9f662ead362d5faf098`.
- All four ZIP entries were read: `MANIFEST.md`, `CODEX-RC6-TO-RC10-IMPLEMENTATION-PROMPT.md` (P), `RC6-TO-RC10-IMPLEMENTATION-SCOPE.md` (S), `RC10-IMPLEMENTATION-ACCEPTANCE.md` (A).
- RC6 runtime tag `engcim-swarm-rc6-runtime-baseline-v1` resolves to `68f010eeb21c14ea14d7bc5605be06a17bcb7920`, is an ancestor of HEAD, and its sealed runtime ZIP matches manifest SHA-256 `9dd6001ffbe589b50858cb0e3a6af937667007b7a65bc4161baf25b0ddb47e78`. This verifies the RC6 runtime package; it does not prove later Java RC10 facades are wired to the active runtime.
- Grafel returned no indexed repository for this worktree; scoped source/test inspection was used. After reconciling the import manifest and release manifest, a fresh OpenJDK 17.0.20.1 run of `MAVEN_OPTS='-Xmx1g -XX:MaxMetaspaceSize=512m' ./mvnw -pl engcim/swarm -DargLine='-Xmx2g -XX:MaxMetaspaceSize=512m' -DforkCount=1 test` passed: 45 tests, including `JavaOnlySourcePolicyTests` and import-manifest validation, 0 failures/errors/skips. At this earlier matrix audit, final hash reconciliation was still pending; it was completed later. Package snapshot RC10-PACKAGE-SNAPSHOT-20260927-02 has a fresh Java 17 run and exact archive audit bound in the external package-reseal receipt. One Maven process and one test fork were used.

### Classification

`ALREADY_IMPLEMENTED` means the bounded local behavior is present in reviewed source; it is not a fresh PASS or proof of installed runtime behavior. `PARTIAL` means behavior or proof is incomplete. `MISSING` means absent from the reviewed path. `CONFLICTING` means current behavior/documentation conflicts with the requirement. External proof is tracked separately rather than conflated with local implementation.

Source and test names below resolve under `engcim/swarm/src/main/java/com/featuredeliveryintelligence/fdi/orchestration/` and `engcim/swarm/src/test/java/com/featuredeliveryintelligence/fdi/orchestration/`, unless qualified otherwise.

### RC6 reuse audit

| Capability already present in RC6 | Canonical evidence | What RC10 should do |
|---|---|---|
| Human issue entry, S01–S10 routing, Scenario-first orchestration and role selection | `baselines/rc6/runtime/package/engcim-swarm-package-RC6/docs/scenarios.md`; `squad-instructions.md`; `skills/swarm-orchestration/SKILL.md` | Reuse the issue/Scenario path. This local environment uses Codex CLI as the Supervisor front end; the company environment uses Claude CLI. Both must use the existing Multica issue/run primitives, without a second dispatcher. |
| Issue create/assign, Multica worker runs, dispatch acknowledgement, asynchronous re-entry and fan-in | `skills/multica-cli/SKILL.md`; `docs/multica-cli-verification.md`; `docs/swarm-execution-model.md` Z2–Z5 | Do not add a second dispatcher or Java Multica client adapter. Verify the supported existing CLI path. |
| Reviewer and Verifier gates, revision-aware review, failure/retry handling | `skills/swarm-orchestration/SKILL.md`; `skills/verification-protocol/SKILL.md`; `docs/swarm-execution-model.md` | Preserve the existing gates; add tests only for changed RC10 contract behavior. |
| Workspace/runtime qualification and change verification/rollback procedure | RC6 setup/verify scripts and `skills/deployment-verification/SKILL.md`; selected Supervisor v0.5.20 runbooks | Use current CLI/runbook operations. Do not create another in-memory runtime lifecycle. |
| Product Knowledge multi-source intake, correlation/conflict, provenance, structured persistence and governance issues | `docs/pk-ingestion.md`; `docs/pk-storage-and-governance.md`; `docs/pk-conceptual-model.md`; `skills/product-knowledge/SKILL.md` | Reuse proven process where applicable, but keep Product Knowledge distinct from WorkspaceKnowledge. |
| Knowledge Curator S01: source classification, deduplication/correlation, conflict detection, ProductKB governance issue and index maintenance | `docs/scenarios.md` S01; `agents/knowledge-curator.md`; `skills/pk-correlation-synthesis/SKILL.md` | Reuse the Curator/correlation workflow. The canonical RC6 archive has 30 Skills and no `scenario-playbooks` Skill; extend only the WorkspaceKnowledge scope and retrieval contract. |
| WorkspaceKnowledge separate from Product Knowledge | No equivalent WorkspaceKnowledge store/workflow found in inspected RC6 package; a workspace-scoped Multica project is recorded in the RC10 runtime composition | This remains a real RC10 delta. First test existing Multica issue/project primitives; do not presume a new Java adapter. |

The remaining Java classes `ClaudeSupervisorGateway`, `SwarmMissionGateway`, `MulticaRuntimeBinding`, `SwarmKnowledgeLifecycle` and `MulticaWorkspaceKnowledgeRepository` have no production composition or Spring bean in the reviewed module; their usages are in tests. They are local contract models, not a live bridge into the active Multica agent runtime. The uncomposed `SupervisorWorkspaceLifecycle` and runtime-state model have been removed. The user selected Codex CLI for this local environment and Claude CLI at the company; keep those as deployment-specific Supervisor front ends over the same Multica primitives, without adding pass-through adapters.

### Evidence-based swarm-orchestration disposition

Select RETAIN, SHRINK, DECOMPOSE or RETIRE from demonstrated responsibility conflicts and actual consumers. Record the affected responsibility, destination owner, loading path and regression evidence. Preserve sealed RC6 bytes and apply justified changes to candidate assets. Architecture boundary clarification alone does not establish a need to rewrite or delete this Skill.

### Deployment compatibility findings from the plan review

RC6 Curator instructions include direct tKMS ingestion and mandatory product-store/ProductKB writes. The selected Supervisor authority contract forbids Swarm tKMS I/O, and WorkspaceKnowledge must not be written as product truth. The deployed role addendum scopes tKMS access to the selected Supervisor profile and distinguishes PRODUCT from WORKSPACE destinations while preserving sealed RC6 bytes. Candidate S01 reports no tKMS/Azure DevOps access. WorkspaceKnowledge-specific authority/negative cases still require their own runtime evidence.

The fresh Codex CLI read-only snapshot identifies the active Orchestrator (`809ffefe-3fc4-4686-8401-a8dd50285840`, four existing Skills, instruction SHA `9ffaabada0b157afbf028e0029adcc85096525fab7712516d9049af3af017361`) and Knowledge Curator (`c38a925f-171b-4c24-91c2-68329ae42654`, eight existing Skills, instruction SHA `c4de40135d887262e9259cf4fc32b799e4479f643b25f3916b241146de0f5413`). Those are the preserved pre-candidate instruction hashes. Both roles now have the versioned Workspace Learning addendum deployed via agent-instruction fields, with exact readback and matching envelopes in actual S01 runs. Candidate instruction hashes are recorded in deployment-verified.json. WorkspaceKnowledge capture/reuse behavior remains untested; S01 Product Knowledge gate PASSED at r3; actual subsequent-Mission consumption remains to be proved. Project metadata was not used as an instruction field. Reuse the already assigned correlation/ingestion skills; do not add a new Adapter or Skill by default.

The correct workspace-scoped read shows the `WorkspaceKnowledge` project is `planned`, with zero issues and no custom-property definitions. Existing Multica issue create/get/list/update, per-issue metadata set/get/list/delete, property set/list/unset and comments support an issue-backed record and scoped retrieval. Metadata values are scalar typed; provenance and other structured values can be carried in the issue body or a JSON string metadata value. This closes the provider-surface discovery question, not durability/read-after-write proof. No Java adapter is justified unless a concrete failure in this existing CLI seam is demonstrated.

PK13–16 also require a durable mapping of governance decisions and retrieval eligibility, not merely create/get support. The versioned overlay now defines the governed record, scope/eligibility rules and procedural enforcement points; loading/readback has been exercised on the two existing roles. The revised plan records these four review findings and their acceptance cases. Local-model fixes, procedural instructions and live enforcement remain separate evidence scopes.

### Package requirements, reviewed individually

| ID | Package source / requirement | Reviewed evidence | Local classification and remaining work |
|---|---|---|---|
| PK01 | P1–2,12; S baseline: actual RC6 lineage, reuse, recorded checkout | RC6 runtime tag/ancestry and sealed archive verified; dirty RC6-derived worktree source inventory recorded | PARTIAL: sealed runtime package identity is proven; complete approved source provenance for the dirty reorganization remains open; do not use r9 as code lineage |
| PK02 | A.A: complete request becomes Mission without unnecessary clarification | MissionIntake, ClaudeSupervisorGateway; SupervisorPathTests | ALREADY_IMPLEMENTED locally; installed entrypoint still needs proof |
| PK03 | A.A: incomplete request cannot dispatch | SupervisorPathTests uses a recording binding and asserts zero calls | ALREADY_IMPLEMENTED locally; MissionFlowTests.T02 alone is weak because its calls list is not connected to a port |
| PK04 | A.A: preserve Human constraints | MissionRequest, MissionExecutionEnvelope; T03 | ALREADY_IMPLEMENTED locally |
| PK05 | A.A: preserve Human acceptance criteria | MissionExecutionEnvelope; T03 and SupervisorPathTests | ALREADY_IMPLEMENTED locally |
| PK06 | A.A; S7: submit to Swarm and operational end-to-end path | Existing v0.5.20 `missionSubmit` template plus RC6 issue-create/assign-to-Swarm Scenario path; Java gateway is test-only | PARTIAL: Codex CLI submitted RC10VAL-10 to the existing Swarm squad; real Orchestrator/Curator/Reviewer runs and artifacts exist. Full accepted Mission outcome remains pending; no second dispatcher was added |
| PK07 | A.B: engineering crosses Runtime Binding | RC6 issue/Scenario execution flow plus conceptual Runtime Binding contract; Java binding wrapper is test-only | PARTIAL: the Java wrapper is not deployed enforcement; verify the existing RC6 execution seam and actual attribution. Do not add another transport adapter |
| PK08 | A.B: direct Supervisor access operational-only; no engineering bypass | SupervisorMulticaBoundary rejects ENGINEERING_EXECUTION; SupervisorBoundaryTests | ALREADY_IMPLEMENTED at this boundary; installed access/path enforcement unproven |
| PK09 | A.B: Multica does not own ENGCIM semantics | Envelope/port boundary; Swarm owns formulation and knowledge logic | ALREADY_IMPLEMENTED in local separation; preserve during transport wiring |
| PK10 | A.C: closure is source, not governed knowledge | ClaudeSupervisorGateway.close; MissionLearningSourceFactory; T07 | ALREADY_IMPLEMENTED locally |
| PK11 | A.C: learning source evidence-bound and workspace preserved | MissionLearningSource and factory; T07–T09 | ALREADY_IMPLEMENTED for carried references; resolvability of external evidence not established |
| PK12 | A.C; S5C: extraction, correlation/conflicts, synthesis | RC6 Product Knowledge already has multi-source intake/correlation/conflict procedures; Java `SwarmKnowledgeGateway` is a separate local contract model | PARTIAL: reuse RC6 procedure where semantics fit; verify WorkspaceKnowledge-specific multi-source behavior in its integrated path before adding code |
| PK13 | A.C: Swarm-owned governance | Explicit APPROVED/REJECTED/DEFERRED lifecycle decisions and conflict rejection pass local tests; deployed addendum defines decision identity/content binding | PARTIAL: local hard-coded approval defect is fixed; actual WorkspaceKnowledge governance and declined/deferred runtime cases remain NOT_RUN. Automatic governance is not forbidden; Human approval per item is not required by this package |
| PK14 | A.C; P6: persistence, retrieval, future Mission context | Existing Multica WorkspaceKnowledge project; issue/project CLI supports create/get/list/update/metadata/comment; Java repository delegates only to a port | PARTIAL: use the existing project/CLI record path if it meets attribution and retrieval needs; live read-after-write and later-use composition remain unproven |
| PK15 | A.C: workspace isolation | repository/project resolver and gateway guards; T09, repository tests | ALREADY_IMPLEMENTED locally; actual provider isolation requires integration evidence |
| PK16 | A.C; P6: current Mission evidence remains authoritative | Learning/proposal references retained; deployed Orchestrator procedure defines fresh scoped retrieval, exclusions and current-evidence precedence | PARTIAL: procedure is deployed, but actual later-Mission consumption and conflicting-current-evidence negatives remain NOT_RUN |
| PK17 | A.D: Runtime State ≠ Memory | RC6 Multica task/run state plus separate Knowledge store semantics; no production Java runtime-state service | ALREADY_IMPLEMENTED as an authority distinction; do not add a duplicate runtime-state service |
| PK18 | A.D: Product Knowledge ≠ WorkspaceKnowledge | productKnowledgeProposal, route guards; T10 | ALREADY_IMPLEMENTED locally |
| PK19 | A.D: MissionLearningSource ≠ WorkspaceKnowledge | separate types and proposal/governance stages; T07–T08 | ALREADY_IMPLEMENTED local distinction |
| PK20 | A.D: Product truth goes through Product governance | ProductKnowledgeProposal.requiresProductGovernance; Workspace proposal rejection | ALREADY_IMPLEMENTED as proposal-only route, not Product publication |
| PK21 | A.D; P9: no direct per-Mission or Swarm tKMS publication | DIRECT_TKMS_PUBLICATION rejected; T11 | ALREADY_IMPLEMENTED in reviewed local route. Historical matrix implies tKMS integration work; that recommendation is CONFLICTING with package scope and is superseded |
| PK22 | P7: minimal learning contract fields/types | MissionLearningSource, WorkspaceKnowledgeProposal and schemas | ALREADY_IMPLEMENTED local shape including limitations list; keep schema/Java parity in regression |
| PK23 | A.E: reusable facts/guidance | SEMANTIC/PROCEDURAL routes and proposal tests | ALREADY_IMPLEMENTED typed route; integrated pipeline gaps tracked in PK12–16 |
| PK24 | A.E: reasoning weakness → Skill | KnowledgeRoute / typed route tests | ALREADY_IMPLEMENTED minimal routing; no new destination service required |
| PK25 | A.E: deterministic rules → Control/code | KnowledgeRoute / typed route tests | ALREADY_IMPLEMENTED minimal routing |
| PK26 | A.E: shared orchestration → Core | KnowledgeRoute / typed route tests | ALREADY_IMPLEMENTED minimal routing |
| PK27 | A.E: translation weakness → Runtime Binding | KnowledgeRoute / typed route tests | ALREADY_IMPLEMENTED minimal routing |
| PK28 | A.E: Multica defect remains Multica issue/proposal | MULTICA_PLATFORM_ISSUE route | ALREADY_IMPLEMENTED classification; inspect existing consumer handoff before deciding an adapter is necessary |
| PK29 | A.E: one-off history not automatically reusable | MISSION_HISTORY route; lifecycle rejects non-Workspace routes | ALREADY_IMPLEMENTED local guard |
| PK30 | A.F; P10: S01–S06, Skills, Controls, Product behavior preserved | Rc6CompatibilityTests checks files/text; historical package self-test | PARTIAL evidence: existence checks are not scenario behavior regression; preserve baseline-specific expectations |
| PK31 | A.F; P10: Runtime Binding exact revision/evidence attribution | Local binding rejects bindingRef and returned executionRevision mismatch; MulticaRuntimeBindingReceiptTests pass. Live S01 has snapshot/addendum identity and separate r1/r2 artifact digests | PARTIAL: retained model fix complete; completed runtime evidence must bind the accepted revision. Java tests remain contract-model evidence, not active transport enforcement |
| PK32 | A.F: execution/domain/governance results distinct | `MissionFlowTests.T12_resultTypesRemainDistinct` exercises COMMITTED with failed verification and unsatisfied control, without Human DONE or persistence | LOCAL_BEHAVIOR_PASS; live semantic independence remains a separate runtime acceptance |
| PK33 | A.F: bootstrap, upgrade, rollback remain valid | v0.5.20 Supervisor bootstrap/runtime-lifecycle runbooks; installed Multica 0.5.3 workspace help; T04/T05 operational boundary tests | PARTIAL: runbooks define preflight/activation/smoke/rollback procedure, but workspace CLI has no pause/resume or lifecycle-state receipt operation. Removed uncomposed Java state-machine model to avoid duplicate authority; do not claim unsupported lifecycle transitions |
| PK34 | A.F; P3: seven components, Scenario-first, no engine/hierarchy | package structure, RC6 surface, architecture tests | ALREADY_IMPLEMENTED at local structural scope; preserve existing Scenario planning when wiring the callable path |
| PK35 | A.G; P14–15: five reports/manifests, candidate package and allowed candidate status | five named reports/manifests plus candidate package | PARTIAL: existing reports/package are not resealed against the reviewed content; no new completion status asserted by this planning audit |

### Live validation disposition — current checkpoint

The user-authorized S01–S06 runs used the isolated workspace. Current-candidate technical evidence exists for S01–S06, while issues remain `in_review` and findings are retained. The S03 positive relation passed review/verification; its NO-EDGE negative control is UNKNOWN. W3 later-Mission AVC read/classification was observed, but full T13 negative eligibility and causal influence are not established. S06 reported F1 and made no repair. See the current evidence overlay at the top of this matrix and the evidence manifest for exact references. The older RC6 baseline issue `RC10VAL-4` remains separate and is not candidate evidence.

### T01–T12 proof map

The Java 17 checkpoint passed 45 local tests, including behavioral T12 coverage. This table distinguishes local behavior from still-required live runtime proof; current S01–S06 receipts are summarized in the evidence overlay above.

| Test | Existing test evidence | Remaining proof |
|---|---|---|
| T01 | MissionFlowTests + SupervisorPathTests | actual composed path; recording port is not live Multica |
| T02 | SupervisorPathTests recording-port negative test | retain this meaningful no-dispatch check |
| T03 | MissionFlowTests exact lists | preserve at real adapter boundary |
| T04 | SupervisorBoundaryTests engineering rejection | no alternate supported deployed engineering bypass |
| T05 | SupervisorBoundaryTests operational allowance | success/failure receipt semantics, not allowance alone |
| T06 | MissionFlowTests plus MulticaRuntimeBindingReceiptTests; mismatch rejection PASS | final accepted runtime revision/source attribution still required |
| T07 | SupervisorPathTests + LearningBoundaryTests | preserve reference attribution through composition |
| T08 | LearningBoundaryTests + KnowledgePipelineTests + lifecycle tests | integrated multi-observation/conflict/governance behavior |
| T09 | LearningBoundaryTests + repository tests | real persistence/retrieval isolation where applicable |
| T10 | LearningBoundaryTests Product proposal guard | preserve without adding Product publication |
| T11 | LearningBoundaryTests prohibited route | no per-Mission publication added by composition |
| T12 | MissionFlowTests record shape | behavioral non-equivalence of execution, verification, control |

### Additional runtime requirements — separate provenance

| Requirement | Source | Treatment |
|---|---|---|
| Supervisor verifies KnowledgeCaptureResult/KnowledgeRef | v0.5.20/r9 authority and closure contracts | Separate runtime compatibility gate if this runtime is used; not an explicit requirement in the implementation ZIP |
| Exact 29-file overlay, active digest/install root/smoke | v0.5.20 runtime package and materialization work | Preserve identity checks; materialized files do not prove activation |
| WorkspaceKnowledge Multica project/record choice | current adapter/composition design | implementation choice to verify, not a new canonical component |
| 19 agents / 30 canonical Skills / requested K27 | Current isolated validation workspace reads and canonical RC6 archive; an earlier workspace snapshot recorded 31 Skills | Keep the counts tied to the exact workspace/archive and do not turn them or the K27 label into ZIP acceptance conditions |
| Later-Mission reuse, restart, retry | future-context/persistence requirement | recommended tests; two live missions and a receipt database are not prescribed architecture |
| Live validation scope | Explicit user authorization for S01–S06 in the isolated Multica workspace | Candidate deployment and bounded Scenario runs in that workspace are authorized. Preserve existing receipts; do not mutate production or unrelated workspaces. |

### R01–R26 crosswalk for previous readers

R01–03 → PK02–05,31; R04–05 → PK06–09; R06 → PK10–11; R07 → PK12–14; R08 → PK15; R09 → PK14,16; R10 → PK18,20; R11 → PK23–29; R12 → PK21; R13 → PK17,19; R14–15 → PK34; R16 → PK33 plus runtime supplement; R17–20 → runtime supplement and PK06; R21 → runtime supplement; R22 → PK30; R23 → T01–T12 table; R24 → repository Java rule; R25 → PK35; R26 → PK01 and evidence binding.

Next work is the revised [implementation plan](RC10-IMPLEMENTATION-PLAN.md), including its file-level REUSE / REQUIRED / CONDITIONAL backlog. RC10 candidate procedures belong in the deployable bootstrap overlay, not the sealed RC6 baseline or only local `.claude` state. The proposed supplemental Multica overlay must have a demonstrated loading path; a document reference is not runtime integration. No historical checked box below is a fresh gate result for this audit.

---

## Historical snapshot — retained, not current status

The following previous matrix is retained verbatim for traceability. Its completion assertions and recommendations are superseded by the current audit above.

# RC6 → RC10 Source-to-Runtime Gap Matrix

Status: `RUNTIME MATERIALIZED / COMPOSITION VALIDATED / EXTERNAL BLOCKERS`

This matrix reconciles the supplied implementation package, Claude Supervisor
runtime package, r9 project baseline, RC6 skill-pack material, and the current
RC10 implementation worktree. It distinguishes repository-local implementation
from active workspace/runtime evidence.

## 1. Authority and evidence rules

| Priority | Source | Use |
|---|---|---|
| 1 | `AGENTS.md` and repository governing files | Repository authority and Java 17/source-policy constraints |
| 2 | `ENGCIM-RC6-TO-RC10-IMPLEMENTATION-PACKAGE.zip` | RC6 → RC10 implementation requirements and acceptance |
| 3 | `engcim-claude-supervisor-runtime-v0.5.20.zip` | Supervisor runtime contracts, runbooks, state and package identity |
| 4 | `ENGCIM-PROJECT-BASELINE-r9-FINAL-ALIGNED-2026-09-26.zip` | r9 architecture and ADR decisions; not implementation lineage |
| 5 | RC6 package and its canonical source manifest | Existing scenario/skill/control/runtime baseline and regression surface |
| 6 | Local test/report output | Evidence only for the exact local revision and scope tested |
| 7 | Live Multica/Claude runtime receipts | Required for active-runtime adoption claims |

The original checkout at `/Users/herman_mbp2023/Documents/Feature-Delivery-Intelligence`
is dirty and is not the implementation source for this matrix. The matrix
snapshot was recorded from this implementation worktree:

```text
/Users/herman_mbp2023/.codex/worktrees/fdi-rc10-implementation/Feature-Delivery-Intelligence
branch: codex/fdi-rc10-implementation
HEAD at matrix snapshot: d50dc32

Later commits are implementation history after this matrix snapshot and must
not be silently substituted for the recorded baseline.
```

### Status vocabulary

| Status | Meaning |
|---|---|
| `LOCAL_PASS` | Implemented and verified in the local repository/test scope only |
| `PARTIAL` | Some contract or static material exists, but a required boundary is missing |
| `MISSING` | Required artifact, wiring or behavior is absent |
| `BLOCKED_EXTERNAL` | Repository-local contract exists, but live runtime/auth/environment evidence is unavailable |
| `PRESERVE` | Existing RC6/r9 behavior must remain unchanged |

## 2. Source-package inventory

| Source package | Supplied content | Current finding |
|---|---|---|
| RC6 → RC10 implementation package | Prompt, scope, acceptance, manifest; five capability groups and T01–T12 | Requirements are clear; local contract implementation exists, but external execution is not proven |
| Company Claude Supervisor v0.5.20 runtime profile | 29 files: `CLAUDE.md`, Supervisor, contracts, skills, runbooks, environment/mission state schemas, target and manifest | Exact snapshot and `.claude` overlay are materialized as the company profile; Codex CLI is selected for local use. The active local Supervisor path is not the Claude profile. Package identity/smoke remains unresolved |
| r9 final-aligned project baseline | Architecture, project overview, target architecture, ADR-001–009, embedded runtime ZIP | Governing architecture input; not the RC6 → RC10 code baseline |
| RC6 curated/runtime package | Agents, skills, controls, scenarios, setup, package-local evidence and self-test | Package and self-test exist; active workspace installation and live scenario execution remain unproven |

## 3. Requirement-to-runtime matrix

| ID | Requirement from supplied material | Required repository/runtime artifact | Current artifact | Status | Gap / next verification |
|---|---|---|---|---|---|
| R01 | Human request intake and targeted clarification | Supervisor intake contract and Mission formulation | `ClaudeSupervisorGateway`, `MissionIntake`, `MissionRequest`; Codex CLI selected for local use | `LOCAL_PASS` | Codex CLI has only been exercised for read-only Multica status queries so far; test the full local Supervisor-to-Swarm dispatch |
| R02 | Complete request must not receive unnecessary clarification | Intake decision test and Supervisor behavior | `SupervisorPathTests` | `LOCAL_PASS` | Add live request receipt |
| R03 | Mission preserves workspace, project, constraints, acceptance and revision | Immutable Mission/execution envelope | `Mission`, `MissionExecutionEnvelope` | `LOCAL_PASS` | Verify exact values in a live Multica run |
| R04 | Engineering path is Supervisor → Mission → Swarm → Runtime Binding → Multica | Swarm gateway and Multica execution port | `SwarmMissionGateway`, `RuntimeBindingPort`, `MulticaRuntimeBinding`; local front end Codex CLI, company front end Claude CLI | `LOCAL_PASS` | The current RC6 pressure run was assigned directly through Multica; it does not prove either CLI's end-to-end Supervisor dispatch |
| R05 | Supervisor direct Multica access is operational-only | Operational allow-list and engineering bypass rejection | `SupervisorMulticaBoundary` | `LOCAL_PASS` | Verify against installed CLI and workspace permissions |
| R06 | Mission Closure Summary is source material, not final knowledge | Supervisor closure and Mission Learning Source contract | `MissionClosureSummary`, `MissionLearningSource` | `LOCAL_PASS` | Wire to runtime closure receipt and Supervisor verification |
| R07 | Swarm owns WorkspaceKnowledge building | Observation → correlation/conflict → synthesis → proposal → governance | `SwarmKnowledgeGateway`, `SwarmKnowledgeLifecycle` and knowledge value objects | `LOCAL_PASS` | Live external record execution remains unproven |
| R08 | WorkspaceKnowledge is workspace-isolated | Workspace-bound repository and rejection tests | `InMemoryWorkspaceKnowledgeRepository`, `LearningBoundaryTests` | `LOCAL_PASS` | Prove isolation in the real WorkspaceKnowledge Project |
| R09 | WorkspaceKnowledge persistence/retrieval supports later Missions | Durable WorkspaceKnowledge Project and retrieval response with provenance/freshness | Multica `WorkspaceKnowledge` project, durable adapter port, capture receipt, lifecycle result and retrieval path | `PARTIAL` | Resolve exact record operation and prove later-Mission reuse |
| R10 | Product Knowledge remains distinct from WorkspaceKnowledge | Product proposal route and authority guard | `ProductKnowledgeProposal`, routing guards | `LOCAL_PASS` | Product Knowledge governed write remains external/unverified |
| R11 | Knowledge routes to Skill, Control, Core, Runtime Binding, Product Knowledge or Multica issue as appropriate | Typed disposition and destination evidence | `KnowledgeRoutingDecision`, `KnowledgeRoute` | `PARTIAL` | No downstream improvement proposal/issue adapters or receipts |
| R12 | Direct Mission/Swarm → tKMS publication is prohibited | Explicit rejection and Supervisor-only tKMS contract | Local rejection test; runtime tKMS contract exists only in supplied ZIP | `PARTIAL` | Materialize and enforce Supervisor tKMS boundary in active runtime |
| R13 | Runtime state is not learned Memory | Separate mission/environment state from WorkspaceKnowledge | Local state classes, supplied state schemas, active `.claude/engcim/state/` and knowledge boundary contract | `LOCAL_PASS` | Live runtime must still prove the same authority separation |
| R14 | Preserve seven ENGCIM components and Scenario-first architecture | Component contract and scenario composition | Existing repo + RC6 package + r9 ADRs | `PRESERVE` | Add architecture compatibility check to release gate |
| R15 | Do not add Memory Service, Mission component, Planner, Trainer or workflow engine | Negative architecture gate | No such RC10 component added | `LOCAL_PASS` | Keep this as a release invariant |
| R16 | Supervisor bootstrap and runtime lifecycle | Environment-specific CLI/runbooks, runtime identity/digest/smoke and rollback evidence | Company `.claude` v0.5.20 profile is materialized; local Codex CLI 0.153.4 has completed read-only Multica queries; active package identity/smoke unresolved | `PARTIAL` | Verify each environment's effective instruction/package identity and candidate smoke |
| R17 | Multica CLI, not invented MCP/subcommands | CLI help discovery and persisted command templates | Installed CLI help mapped all six required operation templates and persisted them in environment state | `LOCAL_PASS` | Use the templates only for an authorized mission; command discovery is not execution evidence |
| R18 | Runtime identity requires revision and package digest | Active/last-known-good package identity | Local report hashes; no active runtime state | `MISSING` | Capture active runtime identity and smoke result |
| R19 | Workspace/project/agent/skill/instruction/scenario configuration | Materialized runtime layout and effective configuration | Current live reads: isolated workspace, three projects, one Kimi runtime, 19 agents, 30 Skills, instructions and S01–S10 resources; project-specific fixture repos are on the Validation project, workspace repo registry is empty | `PARTIAL` | Verify effective configuration through the Codex CLI-supervised candidate mission, not only read-only state |
| R20 | Swarm agents use selectable model, including requested Kimi K27 coding option | Model selector in workspace/agent configuration plus effective run evidence | All 19 existing Swarm agents report `kimi-code/kimi-for-coding`; exact K27 version label is not exposed | `PARTIAL` | Confirm provider/version identity or record the provider limitation |
| R21 | Runtime package v0.5.20 matches manifest and compatible r9 baseline | 29-file package manifest/digest and compatibility check | Exact snapshot and active overlay are materialized; source ZIP digest recorded in runtime report | `LOCAL_PASS` | Keep snapshot/overlay hash check in release gate |
| R22 | RC6 S01–S06 and existing skill/control/runtime behavior remain valid | RC6 regression plus scenario receipts | Canonical RC6 verifier: 25 PASS, 0 FAIL, 4 optional-external NOT VERIFIED; baseline S01 is in progress | `PARTIAL` | Refresh baseline S01 outcome; collect candidate receipts within confirmed I6 scope |
| R23 | T01–T12 repeatable integration tests | Deterministic Java test suite | 12 tests plus additional local tests | `LOCAL_PASS` | Tests are not live runtime evidence |
| R24 | Java 17 implementation and policy | Java 17 runtime execution and `JavaOnlySourcePolicyTests` | Module suite passed on OpenJDK 17.0.20.1 | `LOCAL_PASS` | Live Supervisor/Multica execution remains separately gated |
| R25 | Required RC10 reports/package/evidence manifest | Reports, candidate package, hashes and verifier output | Reports/package/manifest exist and local verifier passes | `LOCAL_PASS` | Rebuild after active runtime materialization and attach live receipts |
| R26 | Authoritative repo/worktree integration | Exact baseline and reviewable change set | Implementation worktree contains the RC10 change set and is dirty; original checkout is also dirty | `PARTIAL` | Human/review decision required before merge or promotion |

## 4. Supervisor runtime package materialization gap

The supplied v0.5.20 ZIP contains the following runtime surfaces. They are now
materialized in the RC10 implementation worktree as an exact snapshot plus an
active `.claude` overlay:

| Runtime surface | Supplied files | Status |
|---|---|---|
| Supervisor instruction root | `CLAUDE.md`, `.claude/engcim/supervisor.md` | `MATERIALIZED` |
| Authority/component contracts | component, knowledge authority/building, diagnosis, execution identity, mission closure, learning source, tKMS, target | `MATERIALIZED` |
| Supervisor operational skills | bootstrap, Multica CLI, runtime lifecycle, improvement diagnosis | `MATERIALIZED` |
| State | environment state, environment schema, mission state template/schema | `MATERIALIZED` |
| Scenario/target contract | S01–S06 deliverable contract, S05 target | `MATERIALIZED` |
| Package integrity | `engcim/bootstrap/supervisor/packages/claude-supervisor-runtime-v0.5.20/MANIFEST.yaml`, README, hardening review | `MANIFEST.yaml` hash verification passed for the materialized snapshot |

This is the largest current gap. The Java RC10 contracts cannot replace these
workspace-level instructions and state files.

## 5. Memory Knowledge / WorkspaceKnowledge boundary

The supplied r9 material does **not** require a generic Memory Service. It
requires this governed boundary:

```text
Mission result / evidence
→ Supervisor closure
→ Mission Learning Source
→ ENGCIM Swarm Knowledge Building
→ governed WorkspaceKnowledge record
→ Future Available Context
→ workspace/project + governance + visibility + applicability + freshness selection
→ Authorized Visible Context
→ subsequent Mission context preparation
```

| Boundary | Required owner | Current state |
|---|---|---|
| Mission inspection/diagnosis/closure | Claude Supervisor | Local gateway exists; live Supervisor runtime absent |
| Mission Learning Source | Claude Supervisor | Local typed contract exists |
| Observation/correlation/conflict/synthesis | ENGCIM Swarm | Local pipeline exists and is tested |
| WorkspaceKnowledge governance | ENGCIM Swarm | Local governance decision exists and is tested |
| WorkspaceKnowledge persistence/retrieval | ENGCIM Swarm | Local lifecycle plus provider-neutral Multica durable adapter; live record primitive remains unresolved |
| Available Context visibility/authority selection | Existing Swarm Orchestrator / Mission context preparation | Not yet implemented or verified at runtime; must become Authorized Visible Context only after the eligibility checks above |
| Capture verification | Claude Supervisor | No live `KnowledgeCaptureResult` / `KnowledgeRef` receipt |
| tKMS Platform/Product Knowledge I/O | Claude Supervisor, governed | Contract exists in supplied runtime; not materialized/live |
| Human `DONE` authority | Human | Not exercised in a live mission |

## 6. Ordered implementation slices

### Task 1 — Freeze the source and authority map

**Acceptance criteria:**

- [x] Record exact ZIP hashes and file manifests.
- [x] Record RC6 baseline, RC10 implementation HEAD, and dirty-checkout boundary.
- [x] Keep r9 documents as architecture input, not as RC10 code lineage.

**Verification:** compare manifests against the recorded source hashes and capture the pre-change Git state/dirty-file boundary. The implementation worktree is not required to be clean after materialization.

**Dependencies:** none.

### Task 2 — Materialize Supervisor v0.5.20 runtime

**Acceptance criteria:**

- [x] Add the exact 29 supplied runtime files to the approved active workspace surface.
- [x] Verify package manifest hashes.
- [~] Preserve the Supervisor/Swarm/tKMS/Memory authority boundaries in local contracts; an independent materialization diff/invariant check remains open.

**Verification:** package digest, manifest check, and instruction/state schema validation.

**Dependencies:** Task 1.

### Task 3 — Bind workspace/project/agent/skill/instruction/scenario/model configuration

**Acceptance criteria:**

- [x] Resolve the actual workspace and project references.
- [x] Materialize RC6 scenario/skill/control references without duplicating authority.
- [x] Record the requested Kimi K27 coding label separately from the verified effective runtime identifier.
- [x] Capture effective model and configuration from the live agent list (`kimi-code/kimi-for-coding` for the current Swarm agents).
- [~] Confirm the exact K27 provider/version and prove that it is selectable and active in the configured runtime.

**Verification:** read-only bootstrap, effective configuration inspection, and model identity receipt.

**Dependencies:** Task 2.

### Task 4 — Connect real Multica runtime operations

**Acceptance criteria:**

- [x] Discover commands from installed CLI help and persist all six required operation templates.
- [~] Define and verify the read-before-write guard through read-only preflight; live write-path proof remains open.
- [ ] Capture active package identity, revision, digest, install root and smoke status.

**Verification:** read-only bootstrap reaches `BOOTSTRAP_READY` or records an explicit `BLOCKED_*` state. Active package identity, smoke status and live write-path behavior are separate gates and cannot be marked complete by bootstrap alone.

**Dependencies:** Task 2 and Task 3.

### Task 5 — Close Memory Knowledge runtime path

**Acceptance criteria:**

- [x] Prepare Mission Learning Source as source material, not final knowledge; the local contract and external adapter are ready, while live evidence is intentionally deferred to Task 6.
- [x] Create and bind the correct workspace-level `WorkspaceKnowledge` project.
- [x] Define capture receipt and provenance/evidence attribution at the adapter boundary.

**Verification:** local lifecycle/adapter contract validation and an explicit record that live capture/retrieval has not yet been claimed.

**Dependencies:** Task 4.

### Task 6 — Run live scenarios and release gates

**Acceptance criteria:**

- [ ] Execute every scenario whose contract requires live runtime; record a per-scenario applicability decision for T01–T12 and do not treat local contract tests as live receipts.
- [ ] Execute one authorized Mission and produce live Mission Learning Source → WorkspaceKnowledge capture/retrieval receipts with provenance.
- [ ] Prove later-Mission reuse and workspace isolation with a negative cross-workspace read/retrieval test.
- [x] Re-run local RC6 S01–S06/skill/control compatibility checks and package self-test.
- [x] Run the module test suite and `JavaOnlySourcePolicyTests` on OpenJDK 17.0.20.1.
- [x] Rebuild candidate package and evidence manifest.

**Verification:** fresh receipts, exact revisions, package hashes, and one allowed completion status.

**Dependencies:** Tasks 1–5, plus fresh Human authorization, an explicit target issue/project, and an allowed operation scope.

## 7. Checkpoints

### Checkpoint A — After Task 2

- [x] Runtime package is materially present and manifest-valid.
- [ ] An authority-boundary diff/invariant check shows that materialization did not change Supervisor/Swarm/tKMS/Memory ownership.

### Checkpoint B — After Task 4

- [x] Workspace bootstrap is either `BOOTSTRAP_READY` or explicitly blocked with a verified reason.
- [x] Effective model identity for the current live agents is recorded and not inferred.
- [ ] Active package identity, revision, digest, install root, and smoke status are known from a runtime receipt.

### Checkpoint C — After Task 6

- [ ] Live Mission and WorkspaceKnowledge path has receipts.
- [x] RC6 S01–S06/skill/control regression evidence is fresh.
- [x] Java 17 module-test evidence is fresh for this worktree revision.
- [ ] Candidate is classified `RC10_IMPLEMENTED_READY_FOR_REVIEW` or `RC10_IMPLEMENTED_WITH_BLOCKERS`.

## 8. Current recommendation

Keep the candidate at `RC10_IMPLEMENTED_WITH_BLOCKERS`. F1 is corrected and independently verified for exact fixture commit `0159b44d9e89bb06e1e2d72b752c4b9cb6e5685e`; preserve the S03 negative-control result as UNKNOWN because absence was not proved. W3 read/classification evidence is bounded to the observed read-only scope; do not claim causal influence or full T13 negative-eligibility coverage. Package snapshot 02 has been resealed after the documentation/import-manifest digest refresh: Java 17 passed 45/45, standalone governance passed 14/14, standalone bundle verification passed 61/61, and the archive passed exact path/hash/size, embedded-manifest and CRC checks. No fresh authorization is needed for these already-authorized in-scope checks; production merge/deployment/promotion remain excluded.

## Historical candidate source checkpoint — 2026-09-26T19:15:58+08:00

Current source/contract digest is SHA-256 of `validation/rc10/candidate-runtime/source-contract-manifest.json` (89 file entries). The broader deployment snapshot is `candidate-input-manifest.json`, SHA-256 `acaea4171d257754204aba8d1e5a1e267226bcadad0e779d6982c93567e89e2e`: 190 files covering dirty source/contracts, bootstrap configuration, original live role/Skill content and the bounded run specification. The existing-role overlay and loading/rollback procedures are now implemented in candidate source; runtime acceptance remains separate. Fresh Java 17 module validation passed 44 tests, 0 failures/errors/skips, including source policy and import-manifest validation. The prior release ZIP is superseded pending final reseal against new evidence.

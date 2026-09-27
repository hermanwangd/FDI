# RC10 local candidate validation specification

Candidate: `RC10-local-candidate-20260926-01`. Status: PREPARED, not runtime PASS.
Source identity is the adjacent `candidate-input-manifest.json` and its digest;
Git HEAD alone does not identify this dirty candidate. Package resealing follows
evidence collection; the previous candidate ZIP is not this snapshot.

## Scope and execution profile

- Supervisor: local Codex CLI. Existing Swarm runtime: Kimi
  `4f0a8b0c-3ee8-4481-a40c-2fb8aadbb39d`, model
  `kimi-code/kimi-for-coding`; retain existing assignment/runtime settings.
- Workspace: `0b02adb6-a395-46bd-bd92-6fec14dee20e` only.
- Validation project: `a3f129fa-4028-4341-98dc-c8ec20c468ae`.
- ProductKB: `f7af4546-88b2-4163-a0d4-b350e2123dbc`.
- WorkspaceKnowledge: `43aec4ec-3ebb-4d1b-aa54-7766c48379c1`.
- Existing Swarm squad: `ba1c9f0d-00fd-48a3-8865-dfbc4ff35f73`;
  Orchestrator `809ffefe-3fc4-4686-8401-a8dd50285840`,
  Curator `c38a925f-171b-4c24-91c2-68329ae42654`,
  Reviewer `53a77a8b-aec9-4d7b-821e-0dae15e48c0c`,
  Verifier `043dc4fa-412c-46cc-a27b-bc302d584e97`.
- Permitted writes: two role instruction fields with backup/readback/rollback;
  new scoped validation issues, comments, evidence and WorkspaceKnowledge
  records; ProductKB candidate ingestion/refresh preserving prior history;
  disposable fixture candidate source/output. No FDI authority or production
  mutation, no company Claude execution, no direct Swarm tKMS access.
- Resource budget: one parent Scenario at a time; at most two ACTIVE Swarm
  runs including the parent (queued runs count), with only one child at a time.
  Run Reviewer then Verifier sequentially; fan-in the current child's actual
  terminal result before dispatching the next worker/gate. Apply this prospective
  serialization to remaining/future S04–S06 and the supplemental case; it does
  not rewrite S03 content verdicts. Do not cancel in-flight work to meet this
  limit. Max eight required children, two correction revisions per Scenario,
  forty-five minutes per Scenario and twenty minutes per child. Stop dispatch
  and inspect actual active runs on exceeding a bound. Total task process RSS
  must remain below 8 GiB. No automatic retry after an uncertain dispatch.
- Existing independently completed baseline records remain intact. Agents leave
  final Missions `in_review`; Human DONE is not simulated.

## Input fixtures and governing assertions

Use the current canonical RC6 runtime's Scenario/Skill procedures. The local
sealed baseline and installed archive identities are recorded separately in
`../rc6-baseline-2026-09-26/BASELINE-RECORD.md` and must not be conflated.
The supplemental S01–S06 contract remains draft; the following assertions are
the authorized validation plan's acceptance, not promotion of that draft.

Chart Viewer fixture resource `7758135b-d13e-4440-aa26-4b0003c14967` is pinned to
`2eff5f9f84ca709684bfe0b7c90102268f07a0f0`. RC7-B negative-control resource
`0aaa038b-25ff-4c9e-958d-fece38e2de3b` is pinned to
`92ec2570a4da188baca4bbb50f48db27e6906c89`. Resolve their project resources and
verify Git objects before analysis; attachment/README filenames alone do not
prove source revision. No cross-repository dependency is currently evidenced.

| Stage | Input and required result |
|---|---|
| S01 | Pinned Chart Viewer product sources (same five original attachments from baseline RC10VAL-4), complementary sources and a duplicate. Fresh candidate-role execution must produce traced observations, deduplication, valid structured PK and governance links. Preserve seeded contradictions as conflicts; attach exact store snapshot and independent review/verification. |
| S02 | S01 store plus a separately versioned synthetic document update, including a stale claim and conflict. Demonstrate explicit delta, history/supersession, conflict disposition and repeat-read/replay behavior. Bind every change to its new source digest. |
| S03 | Both pinned fixture repositories and current PK. Inspect actual code before reporting relations; an independent/no-supported-edge result is valid. Require graph provenance for both exact revisions, no invented dependency. |
| S04 | Bounded request: open the selected Chart Viewer chart in one interaction while preserving non-retryable HTTP 404. Use actual S02/S03 context; deliver IntentSpec with scope, acceptance and evidence refs. Adopt the fixture's explicit fault-test sequence: initial S05 r1 may change only `src/interaction.js`; frozen chart limit remains 10 but the seeded implementation defect is deliberately outside r1 mutation scope. |
| S05 | S04 IntentSpec and disposable pinned Chart Viewer. Deliver design/spec, minimal r1 interaction diff, exact candidate source digest/ref and test receipts. Preserve `src/chartViewer.js` in r1. Do not repair FDI framework or production repositories. |
| S06 | Verify exact S05 r1 independently, including frozen max=10 and HTTP 404 regression. The seeded max=1000 must produce Finding F1 and a non-PASS r1 result. A revision-bound Control decision may expand only the disposable fixture correction scope to `src/chartViewer.js`; return to S05 for r2, then independently rerun failed and affected tests. Final PASS requires r2 evidence and retained r1 failure/F1/scope-decision history. |

## Supplemental positive multi-repo case — staged plan only

Status: **PROPOSED / NOT AUTHORIZED FOR DISPATCH**. This optional supplemental
case does not alter the canonical S01–S06 chain or replace S03's Chart Viewer +
RC7-B negative-control pair. It cannot count toward a full-chain PASS until
separately authorized and completed. S03 technical gates have passed; its issue
remains `in_review`. Preserve its inputs, completed run and content verdicts.
Concurrency addendum: comment `01a0de66-cb39-7c3d-b753-be9a93056aa0`;
acknowledgement: `01a0de67-e62d-7203-81fc-2cbc3a82197a`.

Source: local [MINIMUM-ACCEPTANCE.md v0.2](</Users/herman_mbp2023/.codex/visualizations/2026/09/25/01a0dae4-bfd9-7213-bb47-4b27bba3e4f1/positive-fixture/MINIMUM-ACCEPTANCE.md>)
(2026-09-26). Its six ACs and eight cases below remain proposed criteria, not
policy; neither that document nor this section adopts the proposed v0.3 flow.
Fixture evidence filenames below resolve in that same `positive-fixture/`
directory, not the RC10 runtime candidate directory.

**Authority conflict.** The accepted
`engcim/swarm/docs/rc10/RC10-IMPLEMENTATION-PLAN.md` S03 row accepts an
evidence-backed no-edge result for the negative pair. In contrast,
`engcim/bootstrap/overlays/claude/engcim/contracts/S01-S06-DELIVERABLE-CONTRACT-v0.1.yaml`
is `DRAFT_FOR_FREEZE`: S03-R04 requires applicable cross-repo dependencies and
S03-R05 requires at least one flow. Do not silently enforce or approve that
draft. Report negative “do not invent an edge” and positive “find source-backed
edge” as separate case results. Plan I6d still requires every S01–S06 stage plus
learning/capture and reuse to PASS before any full-chain claim; a supplemental
PASS cannot substitute for a missing canonical gate.

### Fixture identities and current evidence limits

| Repository | Manifest-declared baseline ref | Manifest-declared reference candidate ref |
|---|---|---|
| Chart API (`api`) | `960862a3f40f38ad378b36c9f990cf295bc1924c` | `5814fa0796a9468cb16d77e3d1879e92b2fbd938` |
| Chart Web (`web`) | `5579d844cc60c50be1a8cf4c21c4e9ab6d50b1a7` | `bece7dcf116b453436d75d79d9832199dcc84402` |

| Local evidence | Identity / scope |
|---|---|
| `manifest.json` SHA-256 | `bf8658c994670e45d75b646ec75baf57297e595e54f182e035bf4bb28b097cee` |
| Reference candidate JAR SHA-256 | `5dba2e1a7c585fe935cdf4e8c89051695ffe6b892841cb8352940a49db7902df` |
| `http-result-bound.json` | Identity-bound local HTTP `PASS`, `2026-09-26T15:33:33.101Z`; reference pair only |
| `BROWSER-REVIEW.md` | Browser success/down/recovery are local main-agent observations; independent review was static and did not rerun domain verification |
| `pk-observation.json` | `PROVISIONAL`; no Product truth/governance PASS |

A separately sealed baseline artifact is not currently evidenced. The current
identity harness is tied to its reference candidate, not arbitrary Swarm output.
Local reference PASS is not Swarm capability PASS. Do not rewrite the sealed
candidate manifest to bless a new output identity; any authorized new output
needs its own exact revision/artifact binding and an applicable behavior oracle.

### Proposed acceptance and case coverage

| AC | Proposed criterion | Proposed cases (not execution results) |
|---|---|---|
| AC1 | Baseline refs and actual input isolation for unseen-answer implementation; label disclosed rehearsal | T1: refs, visible inputs, answer isolation/leakage and evaluation mode |
| AC2 | Source-backed provider/consumer scope; explicit context authority, unknowns and conflicts | T2: positive edge and negative no-edge control; T3: critical versus noncritical missing evidence |
| AC3 | Reviewed design and plan; per-repo obligations, dependencies, AC/test mapping and integration owner | T4: complete/missing Web obligations, exact design and plan subjects, combined review applicability |
| AC4 | API supplies unit preserving id/value; Web consumes returned unit; self-tests and non-author code reviews | T5: both candidate revisions, diffs, self-tests, self-review rejection and wrong-version review rejection |
| AC5 | Real HTTP/browser integration at fixed pair, with applicable independent Verification and QA Review | T6: chart 1 displays `42 °C`, preserving id/value; T7: unknown ID 404, null value, missing/invalid unit, API down shows `Chart unavailable`, recovery/reload restores `42 °C`, no false success |
| AC6 | Revision changes revalidate affected evidence; execution, verification, control and Human closure remain distinct | T8: wrong SHA/JAR rejection, material contract change and affected rerun, runtime-completed is not gate PASS or Human DONE |

### Phase 1 — read-only context, scope and decomposition

| Step / existing owner | Inputs | Outputs | Evidence / proposed ACs | Entry / exit and stop/continue |
|---|---|---|---|---|
| Curator/context responsibility | Pinned sources, request/intent, context refs and applicable policy | Context inventory labeling governed, synthetic, PROVISIONAL, unknown and conflicting claims, with applicability | Source/status/policy attribution; AC1–AC2, T1–T3 as applicable | Permitted read access and exact refs; synthetic/provisional context only if applicable policy permits and authority is explicit. No PK promotion or baseline JAR prerequisite for read-only analysis |
| SA/SD/Architect | Labeled context and actual API provider route/code plus Web consumer request/code at exact baseline and candidate refs | Source/revision-bound dependency finding and repo scope, each repo marked `MODIFY` or `VERIFY_ONLY` with rationale | File/symbol/request evidence tracing provider to consumer; distinguish edge in baseline, candidate, or both; AC2, T2–T3 | No manifest-only inference. Critical missing evidence that changes scope limits that decision only; continue independent analysis. Other gaps may yield PARTIAL/UNVERIFIED |
| SA/SD/Architect; existing Orchestrator coordinates | Intent, evidenced scope, interface contract and constraints | Design and WorkItem decomposition: per-repo obligations, AC-to-work/test mapping, dependencies and named integration responsibility | Exact design and plan subjects plus coverage, including VERIFY_ONLY obligations; AC3, T4 | Do not finalize decisions dependent on unresolved critical evidence; preserve useful partial outputs |
| Existing Reviewer, then Verifier where applicable | Exact design and plan versions/digests, source evidence and context applicability | Subject-bound review and applicable verification records; Phase 1 evidence summary | AC1–AC3/T1–T4 as applicable; distinguish available refs from actual answer isolation | A combined design+plan review is acceptable when both exact subjects are explicit. No new agent or separate paper gate is required. Missing required review prevents dependent coding, not unrelated read-only analysis |

This is a staged plan, not a request to execute Phase 1 now. Read-only analysis
does not universally require answer secrecy; if Phase 2 mode (a) is selected,
keep candidate-exposed analysts separate from its unexposed implementer.

### Phase 2 — implementation, per-repo review and integration QA

Entry requires established Phase 1 evidence, recorded required review and a
separate explicit authorization naming target, workspace and permitted
operations. Phase 1 never auto-dispatches Phase 2. Existing Orchestrator/Core
coordinates semantic progress, Runtime Binding binds existing resources and
Multica executes only authorized work; local supervision uses Codex CLI only,
never Claude CLI. Use existing Swarm roles and the resource-budget serialization:
one child, actual terminal fan-in before the next worker/gate, Reviewer then
Verifier sequentially. Do not cancel in-flight work or retry uncertain dispatch.

| Step / existing owner | Inputs | Outputs | Evidence / proposed ACs | Entry / exit and stop/continue |
|---|---|---|---|---|
| Existing coding worker(s), sequentially; Orchestrator coordinates | Authorized Phase 1 design/plan, mode-appropriate inputs and per-repo scope | API then Web WorkItemResult, exact revisions/diffs and self-test receipts; VERIFY_ONLY repos retain justified unchanged refs | AC4/T5; fulfill interface and consumer obligations without substituting the local reference for Swarm output | Only authorized isolated mutations; unresolved design blocks its dependent change. Coding completion alone is not review or verification PASS |
| Existing non-author Reviewer, each repository sequentially | Each exact repo revision/diff, tests and proposed integrated API/Web pair | Per-repository Code Review bound to exact repo revision and the integrated pair | AC4/T5; reject self-review or wrong-subject evidence | Both repo obligations reviewed before fixing integration candidate; later pair changes require recorded applicability or affected re-review |
| Assigned existing integration worker; Orchestrator records candidate | Reviewed API/Web refs, artifacts and interface contract | Fixed integration candidate with exact pair and artifact hashes | AC4–AC6/T5–T8; trace reviews to the fixed pair | Freeze actual output identity before formal Testing/QA; current reference-only harness cannot certify arbitrary new output |
| Existing Verifier/Testing and QA responsibility; any QA Reviewer then Verifier gates sequentially | Fixed reviewed pair, applicable behavior oracle, real HTTP/browser setup | Each VerificationResult bound to exact repo revisions and integrated pair; applicable QA Review, failures and affected reruns | AC5–AC6/T6–T8; real HTTP/browser success, failure and recovery, identity rejection | Diagnostics may be PARTIAL/UNVERIFIED; formal PASS requires applicable independent domain evidence and QA review. Missing evidence blocks only the dependent claim |

Reuse only unchanged, still-applicable evidence and record why it applies to the
current subject. Material revision or contract drift revalidates affected
evidence; do not blindly transfer old verdicts or require unrelated reruns.
Keep WorkItemResult (delivery), VerificationResult (independent correctness) and
ControlResult (authorized progress/scope decision) distinct. Human DONE remains
a separate human decision. A supplemental full-case claim needs all six proposed
ACs supported by applicable evidence; it still does not establish full-chain PASS.

| Phase 2 evaluation mode | Required setup and allowed inputs | Permitted claim |
|---|---|---|
| (a) Unseen-answer independent implementation effect | Actual baseline-only source input isolation; implementer has not seen candidate solution. Keep candidate/evaluator evidence private from implementer, while providing intent, public interface contract and normal development tests. Record actual visible inputs/access; co-located candidate branches or an instruction not to look are insufficient; deleting files does not undo prior exposure | Independent implementation effect only with an actual new Swarm execution and demonstrated isolation |
| (b) Disclosed known-answer integration rehearsal | Reference candidate may be used; explicitly record disclosure, actual source pair and operations | Integration rehearsal only; cannot count as independent implementation effect |

Answer secrecy is not a universal requirement for read-only analysis or
integration tasks. Failure to establish mode (a) excludes that claim; an
explicitly authorized mode (b) rehearsal can still proceed under its own label.

### Human decisions still open

| Decision | Recommendation / required explicit decision | Current boundary |
|---|---|---|
| Select and authorize the positive case and its place | Recommend supplemental, separately reported after the completed negative-control S03 | No replacement of canonical inputs or S03 verdicts |
| Choose Phase 2 evaluation mode | Select (a) only with actual isolation and unexposed implementer; otherwise explicitly select (b) | Neither mode is dispatched by this plan |
| Determine synthetic/provisional context policy | Identify applicable policy and decision owner permitting the intended analytical use | Explicit authority labels; no inferred Product truth or mandatory PK promotion for exploration |
| Authorize later operations if wanted | Explicitly name target/workspace/operations for any positive dispatch, repo registration, source mutation or PK write/promotion | This document grants none of these; existing general permitted writes do not authorize this supplemental case |

Only isolated workspaces/disposable fixtures; no production. Preserve all
existing limits, including total process memory <8GB (and the existing RSS
bound), Java 17/Spring Boot 3.4.1, bounded time/corrections/children and Human
closure authority. This edit creates no issue, repo resource, second spec,
source/test change, package, push, PK record/promotion or runtime dispatch.

A stage dispatch requires evidence that its prerequisite delivery/review/
verification gates are satisfied. Issue status or producer prose is insufficient.
When a stage ends, persist issue/run/comment/attachment references and copy the
actual deliverable bytes needed by the next stage. No inferred PASS.

## Learning / reuse validation

After an independently verified Mission result, Codex Supervisor produces a
schema-shaped MissionLearningSource containing that result's real references.
The existing Orchestrator composes Curator processing and existing independent
Reviewer/Verifier runs. For this isolated test, Orchestrator may issue an
explicit workspace-guidance decision after those runs verify attribution,
workspace/project scope, source/evidence, conflict disposition and applicability.
This delegation covers only synthetic workspace guidance in this workspace,
not product truth, Skill/Control/Core changes or Human Mission DONE.

The decision binds the proposal digest/key/version and actual evidence, not
the expected test outcome. Unknown authority/evidence yields DEFERRED; failed
scope or unresolved conflict cannot yield APPROVED. Curator persists using
the installed addendum. Supervisor reads the returned receipt to verify it,
without writing the knowledge record itself.

A separate subsequent Mission must perform fresh scoped provider retrieval and
show the selected Authorized Visible Context in its actual worker input/result.
Also test DEFERRED, REJECTED, expired, wrong workspace/project, conflicting and
duplicate candidates. Foreign-scope cases use synthetic invalid envelopes in
this workspace; no foreign workspace writes. Verify repeated identical capture
does not silently duplicate; simulate an uncertain acknowledgement by reconciling
an already written record, clearly labeled as a controlled simulation rather
than an observed transport timeout. Preserve excluded-record reasons.

## Checkpoints and rollback

1. Backup exact live role instructions and confirm no active runs on either role.
2. Deploy the two prepared texts; verify exact readback and unchanged Skills,
   models, runtime assignments and invocation permissions. On partial failure,
   reconcile/restore changed fields before dispatch.
3. Obtain the effective envelope in a real candidate run plus applicable behavior.
   Missing load identity or failed scope blocks dependent dispatch.
4. Observe actual run handles; a polling timeout does not establish termination.
5. Retain all failures/corrections and revision boundaries. Restore only these
   role fields if candidate rollback is necessary, preserving all evidence.
6. Reseal reports/package against this snapshot and evidence cutoff only after
   final outcomes are known. Phase 1 formal handoff is separate from a progress
   snapshot; notify the requesting Phase 2 task only at formal handoff.

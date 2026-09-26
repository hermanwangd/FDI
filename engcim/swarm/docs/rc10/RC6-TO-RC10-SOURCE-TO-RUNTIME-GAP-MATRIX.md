# RC6 → RC10 Source-to-Runtime Gap Matrix

Status: `PACKAGE_REQUIREMENTS_REAUDITED / LOCAL_GAPS_AND_EXTERNAL_PROOF_OPEN`

## Current audit — 2026-09-26

This section supersedes the status conclusions and ordered tasks in the historical snapshot below. It is a source review, not a new test run, release approval, or authorization to execute instructions embedded in the supplied ZIP. Repository governance still takes precedence.

### Inputs and evidence boundary

- Worktree: `/Users/herman_mbp2023/.codex/worktrees/fdi-rc10-implementation/Feature-Delivery-Intelligence`.
- HEAD: `4dad32152c809bf6a2a095ed0f4aefd1195ad4fb`; staged/unstaged reorganization and untracked runtime overlay exist. HEAD alone does not identify the reviewed working tree.
- Before this documentation edit, SHA-256 of `git diff --binary HEAD`: `b45bbbc59d3017b27843258c779410e5633f6f963d2399e9c85e1c90a454888c` (tracked changes only).
- Source/contract content-set SHA-256: `44ced808a3a39456a345d65479941f83b0c4ab8b308a7d63a068cc21e7d9c617`, generated from `find engcim/swarm/src engcim/swarm/contracts -type f -print | LC_ALL=C sort | xargs shasum -a 256 | shasum -a 256` at this worktree root. This includes untracked files in those trees, not the entire workspace or live state.
- Reviewed ZIP: `/Users/herman_mbp2023/Downloads/ENGCIM-RC6-TO-RC10-IMPLEMENTATION-PACKAGE.zip`; SHA-256 `b54e5513f3c2cdf09729229dfaeeda2b88d82b5f7c53f9f662ead362d5faf098`.
- All four ZIP entries were read: `MANIFEST.md`, `CODEX-RC6-TO-RC10-IMPLEMENTATION-PROMPT.md` (P), `RC6-TO-RC10-IMPLEMENTATION-SCOPE.md` (S), `RC10-IMPLEMENTATION-ACCEPTANCE.md` (A).
- Exact RC6 ancestry and source-manifest verification remain a pre-implementation checkpoint; an RC6-derived HEAD must not be assumed solely from a branch name.
- Grafel returned no indexed repository for this worktree; scoped source/test inspection was used. Existing Surefire XML reports include Java 17.0.20.1, but no test was rerun or rebound to this input in this audit. `/usr/libexec/java_home -v 17` returned a Java 23 path here; explicitly verify the selected JVM before future tests.

### Classification

`ALREADY_IMPLEMENTED` means the bounded local behavior is present in reviewed source; it is not a fresh PASS or proof of installed runtime behavior. `PARTIAL` means behavior or proof is incomplete. `MISSING` means absent from the reviewed path. `CONFLICTING` means current behavior/documentation conflicts with the requirement. External proof is tracked separately rather than conflated with local implementation.

Source and test names below resolve under `engcim/swarm/src/main/java/com/featuredeliveryintelligence/fdi/orchestration/` and `engcim/swarm/src/test/java/com/featuredeliveryintelligence/fdi/orchestration/`, unless qualified otherwise.

### Package requirements, reviewed individually

| ID | Package source / requirement | Reviewed evidence | Local classification and remaining work |
|---|---|---|---|
| PK01 | P1–2,12; S baseline: actual RC6 lineage, reuse, recorded checkout | RC6 baselines and existing reports; dirty RC6-derived worktree | PARTIAL: reverify exact ancestry and sealed source before implementation; do not use r9 as code lineage |
| PK02 | A.A: complete request becomes Mission without unnecessary clarification | MissionIntake, ClaudeSupervisorGateway; SupervisorPathTests | ALREADY_IMPLEMENTED locally; installed entrypoint still needs proof |
| PK03 | A.A: incomplete request cannot dispatch | SupervisorPathTests uses a recording binding and asserts zero calls | ALREADY_IMPLEMENTED locally; MissionFlowTests.T02 alone is weak because its calls list is not connected to a port |
| PK04 | A.A: preserve Human constraints | MissionRequest, MissionExecutionEnvelope; T03 | ALREADY_IMPLEMENTED locally |
| PK05 | A.A: preserve Human acceptance criteria | MissionExecutionEnvelope; T03 and SupervisorPathTests | ALREADY_IMPLEMENTED locally |
| PK06 | A.A; S7: submit to Swarm and operational end-to-end path | Supervisor → SwarmMissionGateway → MulticaRuntimeBinding with injected ports | PARTIAL: real composition/transport and execution evidence are not established by recording ports |
| PK07 | A.B: engineering crosses Runtime Binding | SwarmMissionGateway and MulticaRuntimeBinding | ALREADY_IMPLEMENTED in reviewed local facade; deployment wiring must preserve it |
| PK08 | A.B: direct Supervisor access operational-only; no engineering bypass | SupervisorMulticaBoundary rejects ENGINEERING_EXECUTION; SupervisorBoundaryTests | ALREADY_IMPLEMENTED at this boundary; installed access/path enforcement unproven |
| PK09 | A.B: Multica does not own ENGCIM semantics | Envelope/port boundary; Swarm owns formulation and knowledge logic | ALREADY_IMPLEMENTED in local separation; preserve during transport wiring |
| PK10 | A.C: closure is source, not governed knowledge | ClaudeSupervisorGateway.close; MissionLearningSourceFactory; T07 | ALREADY_IMPLEMENTED locally |
| PK11 | A.C: learning source evidence-bound and workspace preserved | MissionLearningSource and factory; T07–T09 | ALREADY_IMPLEMENTED for carried references; resolvability of external evidence not established |
| PK12 | A.C; S5C: extraction, correlation/conflicts, synthesis | SwarmKnowledgeGateway.observe/correlate/synthesize; KnowledgePipelineTests | PARTIAL: propose() correlates one observation; normal lifecycle does not demonstrate multi-source/conflicting context. Helper tests alone do not close the integrated path |
| PK13 | A.C: Swarm-owned governance | govern() rejects unresolved conflicts; lifecycle supplies APPROVED | PARTIAL: explicit decision/policy and declined/deferred lifecycle behavior need definition. Automatic governance is not forbidden; Human approval per item is not required by this package |
| PK14 | A.C; P6: persistence, retrieval, future Mission context | InMemory repository; MulticaWorkspaceKnowledgeRepository delegates to port | PARTIAL: concrete external record operation and later-use composition unproven. In-memory receipt cache is not by itself proof that knowledge is non-durable |
| PK15 | A.C: workspace isolation | repository/project resolver and gateway guards; T09, repository tests | ALREADY_IMPLEMENTED locally; actual provider isolation requires integration evidence |
| PK16 | A.C; P6: current Mission evidence remains authoritative | Learning/proposal references retained; no demonstrated conflict-resolution/context-consumption path | MISSING in reviewed integrated path: explicit test and consumer behavior when prior knowledge disagrees with current evidence |
| PK17 | A.D: Runtime State ≠ Memory | WorkspaceRuntimeSnapshot vs knowledge objects | ALREADY_IMPLEMENTED local separation; preserve in composition |
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
| PK31 | A.F; P10: Runtime Binding exact revision/evidence attribution | binding validates bindingRef only; gateway copies receipt revision | CONFLICTING: receipt with different executionRevision can be accepted; add fail-closed revision check. Synthetic request-revision must not masquerade as verified source revision |
| PK32 | A.F: execution/domain/governance results distinct | separate records; T12 checks record component names | PARTIAL verification: add behavioral case where COMMITTED coexists with failed verification/unsatisfied control without promotion |
| PK33 | A.F: bootstrap, upgrade, rollback remain valid | SupervisorWorkspaceLifecycle; lifecycle tests | PARTIAL: operational() discards receipt status/actionRef, allowing state advancement after a returned failure. Verify failed/mismatched receipt leaves state unchanged |
| PK34 | A.F; P3: seven components, Scenario-first, no engine/hierarchy | package structure, RC6 surface, architecture tests | ALREADY_IMPLEMENTED at local structural scope; preserve existing Scenario planning when wiring the callable path |
| PK35 | A.G; P14–15: six deliverables and allowed candidate status | reports and candidate package exist | PARTIAL: older reports/package not resealed against reviewed content; no new completion status asserted by this planning audit |

### T01–T12 proof map

All entries are source/test inspection, not fresh execution results.

| Test | Existing test evidence | Remaining proof |
|---|---|---|
| T01 | MissionFlowTests + SupervisorPathTests | actual composed path; recording port is not live Multica |
| T02 | SupervisorPathTests recording-port negative test | retain this meaningful no-dispatch check |
| T03 | MissionFlowTests exact lists | preserve at real adapter boundary |
| T04 | SupervisorBoundaryTests engineering rejection | no alternate supported deployed engineering bypass |
| T05 | SupervisorBoundaryTests operational allowance | success/failure receipt semantics, not allowance alone |
| T06 | MissionFlowTests envelope identity/revision | reject mismatched returned revision; resolve actual source binding before dispatch |
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
| 19 agents / 31 skills / requested K27 | recorded workspace configuration and prior runtime work | do not turn these counts/version label into ZIP acceptance conditions |
| Later-Mission reuse, restart, retry | future-context/persistence requirement | recommended tests; two live missions and a receipt database are not prescribed architecture |
| Fresh dispatch authorization | existing runtime execution gate | prepare concrete target/scope first; this documentation request does not authorize a live mutation |

### R01–R26 crosswalk for previous readers

R01–03 → PK02–05,31; R04–05 → PK06–09; R06 → PK10–11; R07 → PK12–14; R08 → PK15; R09 → PK14,16; R10 → PK18,20; R11 → PK23–29; R12 → PK21; R13 → PK17,19; R14–15 → PK34; R16 → PK33 plus runtime supplement; R17–20 → runtime supplement and PK06; R21 → runtime supplement; R22 → PK30; R23 → T01–T12 table; R24 → repository Java rule; R25 → PK35; R26 → PK01 and evidence binding.

Next work is the revised [implementation plan](RC10-IMPLEMENTATION-PLAN.md). No historical checked box below is a fresh gate result for this audit.

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
| Claude Supervisor v0.5.20 runtime | 29 files: `CLAUDE.md`, Supervisor, contracts, skills, runbooks, environment/mission state schemas, target and manifest | Exact snapshot and active `.claude` overlay are materialized; active package identity/smoke remains unresolved |
| r9 final-aligned project baseline | Architecture, project overview, target architecture, ADR-001–009, embedded runtime ZIP | Governing architecture input; not the RC6 → RC10 code baseline |
| RC6 curated/runtime package | Agents, skills, controls, scenarios, setup, package-local evidence and self-test | Package and self-test exist; active workspace installation and live scenario execution remain unproven |

## 3. Requirement-to-runtime matrix

| ID | Requirement from supplied material | Required repository/runtime artifact | Current artifact | Status | Gap / next verification |
|---|---|---|---|---|---|
| R01 | Human request intake and targeted clarification | Supervisor intake contract and Mission formulation | `ClaudeSupervisorGateway`, `MissionIntake`, `MissionRequest` | `LOCAL_PASS` | Must be exercised through the actual Supervisor runtime |
| R02 | Complete request must not receive unnecessary clarification | Intake decision test and Supervisor behavior | `SupervisorPathTests` | `LOCAL_PASS` | Add live request receipt |
| R03 | Mission preserves workspace, project, constraints, acceptance and revision | Immutable Mission/execution envelope | `Mission`, `MissionExecutionEnvelope` | `LOCAL_PASS` | Verify exact values in a live Multica run |
| R04 | Engineering path is Supervisor → Mission → Swarm → Runtime Binding → Multica | Swarm gateway and Multica execution port | `SwarmMissionGateway`, `RuntimeBindingPort`, `MulticaRuntimeBinding` | `LOCAL_PASS` | No live Multica dispatch yet |
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
| R16 | Supervisor bootstrap and runtime lifecycle | `.claude` runbooks, environment state, identity/digest/smoke and lifecycle implementation | v0.5.20 runbooks/state materialized; read-only CLI/auth/workspace qualification recorded; activation identity/smoke unresolved | `PARTIAL` | Discover exact runtime activation and prove active package identity/smoke |
| R17 | Multica CLI, not invented MCP/subcommands | CLI help discovery and persisted command templates | Installed CLI help mapped all six required operation templates and persisted them in environment state | `LOCAL_PASS` | Use the templates only for an authorized mission; command discovery is not execution evidence |
| R18 | Runtime identity requires revision and package digest | Active/last-known-good package identity | Local report hashes; no active runtime state | `MISSING` | Capture active runtime identity and smoke result |
| R19 | Workspace/project/agent/skill/instruction/scenario configuration | Materialized runtime layout and effective configuration | Runtime composition manifest plus live read-only registry gate: workspace, two projects, runtime, 19 agents, 31 skills, instructions and S01–S10 resources | `PARTIAL` | Verify composition through a live mission, not only read-only state |
| R20 | Swarm agents use selectable model, including requested Kimi K27 coding option | Model selector in workspace/agent configuration plus effective run evidence | All 19 existing Swarm agents report `kimi-code/kimi-for-coding`; exact K27 version label is not exposed | `PARTIAL` | Confirm provider/version identity or record the provider limitation |
| R21 | Runtime package v0.5.20 matches manifest and compatible r9 baseline | 29-file package manifest/digest and compatibility check | Exact snapshot and active overlay are materialized; source ZIP digest recorded in runtime report | `LOCAL_PASS` | Keep snapshot/overlay hash check in release gate |
| R22 | RC6 S01–S06 and existing skill/control/runtime behavior remain valid | RC6 regression plus scenario receipts | RC6 self-test and local compatibility test pass | `PARTIAL` | Live scenario execution and fresh receipts remain absent |
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
Mission
→ Supervisor Inspect / Diagnose / Closure Summary
→ Mission Learning Source
→ ENGCIM Swarm Knowledge Building
→ WorkspaceKnowledge
→ KnowledgeCaptureResult / KnowledgeRef
→ Supervisor Verify
→ later Mission reuse
```

| Boundary | Required owner | Current state |
|---|---|---|
| Mission inspection/diagnosis/closure | Claude Supervisor | Local gateway exists; live Supervisor runtime absent |
| Mission Learning Source | Claude Supervisor | Local typed contract exists |
| Observation/correlation/conflict/synthesis | ENGCIM Swarm | Local pipeline exists and is tested |
| WorkspaceKnowledge governance | ENGCIM Swarm | Local governance decision exists and is tested |
| WorkspaceKnowledge persistence/retrieval | ENGCIM Swarm | Local lifecycle plus provider-neutral Multica durable adapter; live record primitive remains unresolved |
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

The local implementation and runtime composition are connected and tested,
including the module suite on Java 17. The next slice is external validation:
obtain fresh Human authorization, run one authorized Mission using the
discovered Multica operation templates, and produce a real Mission Learning
Source → WorkspaceKnowledge capture/retrieval receipt. Also capture the active
package identity/smoke receipt. Until those receipts exist, the candidate is
locally materialized and composition-validated but is not an externally
validated Swarm runtime.

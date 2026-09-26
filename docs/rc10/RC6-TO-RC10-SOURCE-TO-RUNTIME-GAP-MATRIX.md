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
| R24 | Java 17 implementation and policy | Java 17 runtime execution and `JavaOnlySourcePolicyTests` | POM targets 17; tests ran on OpenJDK 23 | `BLOCKED_EXTERNAL` | Run the same suite on an actual Java 17 runtime |
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
| Package integrity | `MANIFEST.yaml`, README, hardening review | `MANIFEST.yaml` hash verification passed for the materialized snapshot |

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
- [ ] Run the test suite on Java 17.
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
- [ ] Java 17 runtime evidence is fresh.
- [ ] Candidate is classified `RC10_IMPLEMENTED_READY_FOR_REVIEW` or `RC10_IMPLEMENTED_WITH_BLOCKERS`.

## 8. Current recommendation

The local implementation and runtime composition are connected and tested.
The next slice is external validation: obtain fresh Human authorization, run one
authorized Mission using the discovered Multica operation templates, and produce
a real Mission Learning Source → WorkspaceKnowledge capture/retrieval receipt.
In parallel, capture the active package identity/smoke receipt and run the suite
on a Java 17 runtime. Until those receipts exist, the candidate is locally
materialized and composition-validated but is not an externally validated Swarm
runtime.

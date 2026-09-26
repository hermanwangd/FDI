# RC10 Minimal Remediation Implementation Plan

Status: `IN_PROGRESS / RC6 RUNTIME BASELINE IDENTITY VERIFIED / JAVA17 MODULE TEST PASS / SOURCE PROVENANCE OPEN`
Date: 2026-09-26

## Scope and authority

This plan supersedes the historical candidate plan retained below. Input identity, PK01–PK35 requirements, T01–T12 mapping and supplementary runtime requirements are recorded in [the revised matrix](RC6-TO-RC10-SOURCE-TO-RUNTIME-GAP-MATRIX.md). The supplied implementation ZIP defines the candidate delta; repository governance remains authoritative. r9 is architecture input, not implementation lineage.

Deliver the smallest RC6-derived RC10 candidate: reuse intake, ports, types, routing and tests already present. Do not add a Memory Service, Planner, Trainer, Mission component, learning service, workflow engine, new agent hierarchy or tKMS pipeline. Typed routing does not require a new external adapter for every destination. Per-item Human governance is not a package requirement.

The user has authorized repository implementation against this plan. Live Multica/Claude mutations remain behind I6's fresh-authorization gate. Existing code and the ongoing folder reorganization are preserved. Historical reports are not newly certified.

## Tasks

Each task targets a small reviewable change set; split before implementation if more than roughly five source/test files are needed. Paths below use O = `engcim/swarm/src/main/java/com/featuredeliveryintelligence/fdi/orchestration`, T = the corresponding `src/test/java/.../orchestration`, C = `engcim/swarm/contracts/rc10`.

### I0 — Pin baseline and establish local verification (PK01,35)

- Verify exact RC6 source manifest/tag/ancestry and the approved RC6-derived head; distinguish the sealed runtime package identity from a complete source-code baseline. Record dirty/untracked input hashes without resetting, staging or committing unrelated work.
- Select and inspect an actual Java 17 JVM. Run the module suite, explicitly including JavaOnlySourcePolicyTests; retain command, JVM, inputs and result. Do not reuse the historical 42-test count.
- Classify every failure as existing, environment, or introduced; preserve it as evidence rather than silently fixing unrelated reorganization work.

Likely files: matrix, integration/regression/evidence reports (4). Dependency: none. Size: M.
Verification: exact manifest/ancestry receipts, source-content digest and fresh test report. A clean checkout is not required, but reproducible inputs are.

### I1a — Fail closed on execution attribution (PK06,31; T01,03,06)

- Validate returned binding/revision against the execution envelope; reject a mismatch before producing a successful WorkItemResult.
- Keep request identity distinct from resolved source revision. A complete Human request need not supply a SHA; resolve one at the existing binding/preflight boundary, or return a specific blocked state before dispatch.
- Preserve constraints, acceptance and evidence references through the existing path; do not replace Scenario planning with Supervisor logic.

Likely files: O/MulticaRuntimeBinding.java, O/SwarmMissionGateway.java, O/MissionIntake.java if necessary, T/MissionFlowTests.java, T/SupervisorPathTests.java. Dependency: I0. Size: M.
Verification: matching receipt accepted; wrong revision rejected; absent source binding cannot be presented as a verified synthetic revision. Existing no-unnecessary-clarification test is revised only to distinguish intake readiness from execution readiness.

### I1b — Preserve operational state on failure (PK33; T05)

- Define successful operational receipt semantics from the supported provider contract, including action attribution.
- Reject failed/mismatched receipts before advancing bootstrap/upgrade/pause/resume/rollback state.
- Test partial upgrade failure and continued last-known state without inventing a recovery engine.

Likely files: O/SupervisorMulticaBoundary.java, O/SupervisorWorkspaceLifecycle.java, O/OperationalReceipt.java if needed, T/SupervisorLifecycleTests.java. Dependency: I0 and provider receipt semantics (can be discovered read-only in I4a). Size: M.
Verification: failed/mismatched operational calls do not claim ACTIVE/new version; valid transitions still pass.

Checkpoint A: I1a/I1b focused tests pass; baseline failures remain explicitly tracked.

### I2a — Exercise real conflict/governance flow (PK12–13; T08)

- Extend existing lifecycle inputs/composition to correlate relevant observations, not merely test a disconnected helper. Define comparison scope; do not invent a global knowledge engine.
- Carry conflict results into proposal/governance. Use an explicit Swarm decision or documented deterministic policy; do not assume APPROVED merely because a caller supplied an actor name.
- Non-approved/unresolved proposals must not be persisted. Preserve source/evidence attribution.

Likely files: O/SwarmKnowledgeLifecycle.java, O/SwarmKnowledgeGateway.java, O/WorkspaceKnowledgeLifecycleResult.java if necessary, T/KnowledgePipelineTests.java, T/WorkspaceKnowledgeLifecycleTests.java. Dependency: I0. Size: M.
Verification: multi-observation conflicting input follows the actual lifecycle and cannot persist; approved non-conflicting input succeeds; declined/deferred path is exercised.

### I2b — Preserve current evidence during knowledge reuse (PK14,16)

- Identify the existing future-Mission context consumer before selecting an extension point.
- Prevent historical WorkspaceKnowledge from overriding contradictory current Mission evidence; retain applicability/limitations and provenance in context.
- Test same-workspace reuse and a conflict between prior guidance and current evidence. Cross-Mission comparison is a chosen verification method, not a new universal schema requirement.

Likely files: existing context consumer to be resolved at I0, O/WorkspaceKnowledgeRepository.java only if required, O/SwarmKnowledgeGateway.java, one consumer test. Dependency: I2a, I4c for external proof only. Size: M.
Verification: current evidence wins or explicitly blocks an unresolved decision; old knowledge is never silently promoted to current truth. If no consumer is found, record the gap and choose the smallest composition extension before coding.

### I3 — Confirm minimal routing and result semantics (PK18–29,32; T10–12)

- Retain existing typed routes. Inspect each existing consumer; add only the smallest missing handoff needed for a usable proposal/disposition.
- Verify Product truth remains a governed proposal, platform defects stay assigned to Multica, and history is not automatically reusable.
- Add a behavioral test showing COMMITTED execution can coexist with failed verification/unsatisfied control; no automatic Product correctness or Human DONE. Preserve tKMS prohibition.

Likely files: O/KnowledgeRoutingDecision.java or existing consumer only if needed, T/KnowledgePipelineTests.java, T/MissionFlowTests.java, T/LearningBoundaryTests.java. Dependency: I0; I2a for integrated learning scenario. Size: S–M.
Verification: table-driven destination assertions plus non-promotion case. If existing minimal behavior is sufficient, make no production change. External issue creation is not a blanket acceptance condition.

### I4a — Discover actual provider operations and composition (PK06,09,14)

- Inspect installed CLI help and existing RC6 runtime mechanisms read-only; verify version, execution/record/read semantics, errors and available identifiers.
- Map existing ports to actual supported operations; identify the callable Supervisor/Swarm composition entrypoint and existing Scenario dispatch mechanism.
- Select durable knowledge representation supported by the provider. Do not prescribe a new receipt database: retrieval/reconstruction may suffice.

Likely files: environment-specific operation mapping/evidence and plan (2). Dependency: I0; can precede I1/I2 to resolve uncertainty early. Size: S.
Verification: operation mapping backed by actual help/read-only responses; unsupported capability is explicit BLOCKED_EXTERNAL. Command discovery is not execution evidence.

### I4b — Wire execution adapter without semantic duplication (PK06–09,31)

- Reuse existing runtime mechanisms if available; otherwise implement the smallest Java 17 transport behind MulticaExecutionPort.
- Wire the callable path through Swarm/Scenario and Runtime Binding, retaining operational-only Supervisor access.
- Handle failure and ambiguous dispatch without blind duplicate execution; validate returned attribution.

Likely files: existing adapter/composition entrypoint identified by I4a, O/MulticaExecutionPort.java if contract requires it, adapter test (3–5). Dependency: I1a, I4a. Size: M.
Verification: local transport/contract tests prove complete flow, no-dispatch on incomplete input, provider failure and attribution rejection. Live proof is I6.

### I4c — Wire durable knowledge retrieval (PK14–15)

- Implement/reuse supported persistence and retrieval behind MulticaWorkspaceKnowledgePort; enforce resolved workspace/project boundaries.
- Verify source/evidence attribution on provider responses and retry behavior after ambiguous writes.
- Demonstrate a fresh client can retrieve previously persisted knowledge with provenance; capture receipt may be stored or reconstructed.

Likely files: provider adapter, O/MulticaWorkspaceKnowledgeRepository.java, O/MulticaWorkspaceKnowledgePort.java if needed, T/MulticaWorkspaceKnowledgeRepositoryTests.java, adapter test. Dependency: I2a, I4a. Size: M.
Verification: read-after-write using a new client, duplicate-write protection or documented safe reconciliation, wrong-workspace/provider attribution rejection. In-memory doubles are labeled as contract tests.

Checkpoint B: local integrated flow and negative tests pass; full Java 17 suite including source policy passes before live validation. External blockers are listed separately.

### I5 — Restore behavior-level RC6 regression coverage (PK30,34)

- Inventory RC6 baseline expectations for S01–S06, Skills, Controls, Product Knowledge, binding and lifecycle. Reuse existing behavioral harnesses before adding tests.
- Map each scenario to deterministic/local versus required external evidence; file existence alone cannot pass behavior acceptance.
- Preserve historical baseline bytes/results and approved component boundaries.

Likely files: existing regression harness/tests selected from RC6 inventory, Rc6CompatibilityTests.java only as a supplemental surface check, regression report (split by scenario if needed). Dependency: I0; execute final regression after affected I1–I4 changes. Size: M per scenario slice.
Verification: per-scenario expected behavior and actual result/evidence, explicit NOT_RUN/BLOCKED where necessary; no aggregate green based on file checks alone.

### I6 — Bounded end-to-end evidence (S7, applicable T01–T12)

- Prepare exact target issue/project, candidate revision, permitted operations, acceptance, budget/stop conditions and rollback boundary. Fresh Human authorization is required by the existing runtime gate before mutation.
- Prove actual Mission → Swarm → Binding → Multica → evidence and learning → governed reusable destination. Demonstrate later context use through an independent read/consumer execution; two live Missions are an option, not a mandatory count.
- Where v0.5.20 is the selected runtime, separately check its capture-verification and active-identity requirements. Keep optional model labels and registry counts out of package acceptance.

Likely files: run-scoped receipts plus integration/evidence reports. Dependency: I1–I5 relevant gates, external readiness and authorization. Size: bounded run.
Verification: real receipts tied to exact inputs, no cross-workspace leakage and no unsupported completion inference. If unavailable, retain explicit blockers; do not dispatch to manufacture evidence.

### I7 — Reseal candidate and required deliverables (PK35)

- Refresh RC6-TO-RC10-GAP-REPORT.md, RC10-IMPLEMENTATION-REPORT.md, RC10-INTEGRATION-TEST-REPORT.md, RC10-REGRESSION-REPORT.md, RC10-EVIDENCE-MANIFEST.md and RC10-CANDIDATE-PACKAGE using existing packaging mechanisms.
- Record changed contracts/behavior, intentionally preserved behavior, added/reused tests, limitations and blockers. Bind reports/package to the same candidate input; exclude local live state/secrets.
- Report exactly one supported final state after execution: RC10_IMPLEMENTED_READY_FOR_REVIEW, RC10_IMPLEMENTED_WITH_BLOCKERS, or RC10_NOT_IMPLEMENTED. None grants release/promotion/adoption.

Dependency: all applicable local tasks and I6 result or explicit blocker. Size: split report updates from package generation.
Verification: manifest/hash/package consistency, source-policy result and per-requirement coverage. Do not rewrite RC6 historical evidence.

## Resource and execution rules

All new executable framework behavior remains Java 17 / Spring Boot 3.4.1. No added Python implementation or legacy-wrapper features. Use one Maven process and one test fork, bounded e.g. Maven `-Xmx1g`, test JVM `-Xmx2g`, no parallel suite, leaving headroom below the user's 8 GB limit. Verify actual JVM version before running. Re-run affected tests after each slice; full suite at integration checkpoints, not repeatedly without a change.

Dependency summary: I0 → I4a; I0 → I1a/I2a/I3; I4a → I1b/I4b/I4c; I2a → I2b; I1–I4 → final I5/checkpoint B → authorized I6 or explicit blocker → I7. I2b external proof depends on I4c. Work order does not authorize parallel agents or external writes.

## Open decisions to resolve from evidence

1. Exact RC6 ancestry and input binding for the current dirty reorganization.
2. Existing production composition/context consumer and provider record primitive.
3. Swarm governance policy and provider success/retry semantics.
4. Which RC6 scenarios genuinely require live execution and which selected-runtime supplements apply.

No new service, provider API, approval regime, or additional capability should be inferred merely to close these questions.

---

## Historical plan — retained, superseded

# RC10 Candidate Implementation Plan

## Objective

Implement the smallest RC10 candidate that makes the required boundaries executable and testable on top of the RC6 foundation. Preserve the Scenario-first ENGCIM architecture and keep external Claude Supervisor and Multica behavior behind ports.

## Work packages

### WP1 — Mission path

- Add a request value object retaining request reference, workspace/project, scope, goal, constraints, and acceptance criteria.
- Add a completeness gate that returns a clarification failure without dispatch when required fields are absent.
- Formulate an immutable Mission and route it through a Swarm gateway to a provider-neutral Runtime Binding port.
- Preserve exact mission/workspace/request/revision/evidence attribution in `WorkItemResult`.

### WP2 — Supervisor and runtime boundaries

- Expose only operational Supervisor-to-Multica actions for qualification, access, health/status, observation, activation, pause/resume, and rollback.
- Reject engineering execution at the Supervisor boundary.
- Make engineering execution possible only through Mission → Swarm → Runtime Binding.
- Use deterministic recording ports in tests; do not claim a live Multica integration.

### WP3 — Learning and knowledge boundaries

- Add typed `MissionClosureSummary` and `MissionLearningSource` contracts.
- Add Swarm-owned `WorkspaceKnowledgeProposal` construction with `SEMANTIC` and `PROCEDURAL` types.
- Enforce workspaceRef isolation and preserve source/evidence references.
- Route reasoning, deterministic governance, shared orchestration, runtime translation, product-truth candidates, Multica defects, and one-off history without adding a learning service.
- Reject direct Mission/Swarm publication to tKMS and prevent product-truth candidates from silently becoming WorkspaceKnowledge.

### WP4 — Acceptance and evidence

- Implement T01–T12 as deterministic Java tests.
- Add the two public contract schemas.
- Run existing Maven, Java-only, standalone governance, and release checks.
- Produce implementation, integration, regression, and evidence reports plus an RC10 candidate package.

### WP5 — Complete the missing Supervisor path

- Add a Supervisor adapter for intake assessment, targeted clarification, Mission submission, and closure-summary creation.
- Keep Supervisor outside Swarm semantics and retain only operational Multica actions on its direct port.

### WP6 — Complete Runtime Binding and lifecycle contracts

- Carry constraints, acceptance criteria, workspace, mission, request, and revision in an immutable execution envelope.
- Add a Multica execution port/adapter without claiming a live external call.
- Model workspace bootstrap, runtime upgrade, and rollback state transitions behind the operational boundary.

### WP7 — Complete Swarm-owned knowledge building

- Add evidence-backed observations, correlation, conflict detection, synthesis, governance, persistence, and workspace-scoped retrieval.
- Preserve the distinction between MissionLearningSource, Product Knowledge, and WorkspaceKnowledge.
- Correct `limitations` to be a list of strings in both Java and JSON contracts.

### WP8 — Complete routing and RC6 compatibility evidence

- Emit a typed routing decision for every allowed knowledge destination, including Skill, Control, Swarm Core, Runtime Binding, Product Knowledge, Multica issue, and Mission history.
- Add S01–S06/skill/control compatibility checks and lifecycle regression tests without rewriting RC6 artifacts.
- Rebuild the candidate ZIP and all release metadata only after the remediation passes.

## Acceptance matrix

| Test | Required proof |
|---|---|
| T01 | Complete request reaches Runtime Binding through Mission and Swarm. |
| T02 | Incomplete request requires clarification and causes no engineering dispatch. |
| T03 | Constraints and acceptance criteria are preserved exactly. |
| T04 | Supervisor engineering dispatch is rejected. |
| T05 | Supervisor operational Multica actions remain allowed. |
| T06 | Binding receives exact Mission identity and execution revision. |
| T07 | Closure/evidence produces a MissionLearningSource. |
| T08 | MissionLearningSource produces a WorkspaceKnowledgeProposal. |
| T09 | Workspace isolation rejects cross-workspace learning. |
| T10 | Product truth is routed as a Product Knowledge proposal, not WorkspaceKnowledge. |
| T11 | Direct tKMS publication is absent/rejected. |
| T12 | WorkItemResult, VerificationResult, and ControlResult remain distinct. |

## Non-goals

- No new ENGCIM component.
- No Memory Service, Planner, Trainer, workflow engine, scenario scheduler, or learning-disposition service.
- No live external Claude, Multica, tKMS, or provider mutation.
- No Python framework source.
- No release/promotion claim.

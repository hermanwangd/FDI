# RC6 to RC10 Gap Report

Status: baseline assessment complete; implementation follows this report.

## 1. Scope and authority

The supplied `ENGCIM-RC6-TO-RC10-IMPLEMENTATION-PACKAGE.zip` contains a prompt, scope, acceptance criteria, and manifest. Those files are treated as a candidate implementation specification. They do not override repository governance, `AGENTS.md`, the existing RC6 baseline, or the user's request to implement.

This report records the gap between the exact RC6 implementation baseline and the requested RC10 candidate. It does not claim promotion, release, or current authority.

## 2. Baseline identity

| Field | Evidence / value |
|---|---|
| Repository | `Feature-Delivery-Intelligence` |
| RC6 baseline commit | `68f010eeb21c14ea14d7bc5605be06a17bcb7920` |
| Implementation base | `codex/reorganize-fdi-baseline` at `108e890` |
| Implementation worktree | `/Users/herman_mbp2023/.codex/worktrees/fdi-rc10-implementation/Feature-Delivery-Intelligence` |
| Implementation branch | `codex/fdi-rc10-implementation` |
| Baseline state | clean before RC10 changes; baseline Maven and standalone governance checks passed |
| Java evidence | POM targets Java 17; baseline execution used local OpenJDK 23.0.2, so this is not JDK 17 runtime evidence |
| Scope boundary | RC6 to RC10 candidate implementation only; no live Claude Supervisor or Multica dispatch |

The original checkout at `/Users/herman_mbp2023/Documents/Feature-Delivery-Intelligence` is dirty and is intentionally not used as the implementation source. Its untracked RC7-like candidates were not copied into this worktree.

## 3. Gap classification

### Already implemented in the RC6 foundation

| Area | Finding |
|---|---|
| Scenario-first foundation | Existing product, feature, structural, and validation code supports the Scenario/Skill/Control direction; no replacement architecture is required. |
| Product Knowledge | `ProductSemantics` and `ProductKnowledgeMaintenance` provide the existing product-knowledge foundation. |
| Runtime Binding foundation | Provider-neutral structural APIs, Grafel adapter, and binding attestation classes establish a runtime/provider boundary. |
| Validation and governance | `CanonicalBaseGate`, `Dev204Validation`, `VerificationAccounting`, package architecture tests, and Java-only source policy are present. |
| Existing RC6 preservation boundary | Existing Skills/Controls, Product Knowledge, Runtime Binding, result/evidence concepts, and workspace bootstrap/upgrade/rollback procedures remain in scope. |

### Partial

| Area | Finding |
|---|---|
| Scenario-first orchestration | RC6 skill-pack and agent documents describe Scenario/Skill/Control and Multica mapping, but the Java implementation has no end-to-end Mission bridge. |
| Multica execution mapping | RC6 documentation describes agents, squads, issue execution, and operational procedures; the clean Java baseline has no executable Multica runtime adapter for a Mission. |
| Product/workspace knowledge | Product Knowledge procedures and PK-store documentation exist, but the RC6 Java baseline has no typed `MissionLearningSource` or `WorkspaceKnowledgeProposal` boundary. |
| Supervisor lifecycle | Workspace bootstrap, runtime upgrade, rollback, and health procedures exist as documented operations; a code-level operational-only Supervisor boundary is missing. |
| Result separation | Existing validation/evidence code is not a complete typed separation of `WorkItemResult`, `VerificationResult`, and `ControlResult` for Mission execution. |

### Missing for the RC10 candidate

| Area | Finding |
|---|---|
| Mission intake | No typed request completeness check, clarification result, or Mission formulation/submission path. |
| End-to-end path | No code-level `Human Request -> Mission -> Swarm -> Runtime Binding -> Multica` path with exact request, constraint, acceptance, workspace, and revision attribution. |
| Supervisor boundary | No executable prohibition for Supervisor-to-Multica engineering dispatch, nor a narrow allow-list for operational Multica actions. |
| Learning source | No typed closure/evidence-to-`MissionLearningSource` contract. |
| Workspace knowledge proposal | No Swarm-owned source-to-`WorkspaceKnowledgeProposal` contract, workspace isolation check, or product-truth separation. |
| Knowledge routing | No minimal routing for semantic/procedural knowledge, Skill, Control, Swarm Core, Runtime Binding, Product Knowledge proposal, Multica issue, and mission-history outcomes. |
| tKMS boundary | No explicit direct Mission/Swarm-to-tKMS prohibition. |
| Acceptance evidence | No T01–T12 integration test suite or RC10 implementation, integration, regression, and evidence reports. |
| Candidate package | No RC10 candidate package containing the implementation and its evidence. |

### Conflicting or high-risk boundary

The baseline Java source does not contain a known direct Supervisor-to-Multica engineering bypass. However, RC6 operational documents use Multica issue/agent commands for runtime procedures. That is a potential boundary risk, not proof of an implementation conflict. RC10 therefore adds code-level ports and tests that make engineering dispatch pass through Mission/Swarm/Runtime Binding while retaining operational Supervisor actions.

## 4. Required architecture constraint

The candidate will preserve the seven existing ENGCIM components and the Scenario-first model. Mission is an execution/request instance, not an eighth component. No Memory Service, Planner component, Mission component, Trainer, new workflow engine, scenario scheduler, or new `LearningDispositionService` will be added.

The implementation will use small Java 17-compatible domain contracts and ports:

1. Mission request completeness and formulation.
2. Supervisor operational-only Multica boundary.
3. Swarm Mission gateway to a provider-neutral Runtime Binding port.
4. Distinct WorkItem, Verification, and Control result types.
5. Mission closure/evidence to MissionLearningSource.
6. Swarm-owned WorkspaceKnowledgeProposal construction and routing guards.

External Claude Supervisor and Multica execution remain ports/test doubles in this repository. A passing local contract test is not live external runtime adoption evidence.

## 5. Implementation task list

- [x] Inspect exact RC6 baseline and package contents.
- [x] Record this gap report before implementation source changes.
- [ ] Add minimal Java contracts and boundary ports without adding an ENGCIM component.
- [ ] Add deterministic T01–T12 integration tests.
- [ ] Add public RC10 contract schemas and implementation documentation.
- [ ] Run baseline regression, Java-only policy, standalone governance, and RC10 tests.
- [ ] Generate RC10 reports, evidence manifest, and candidate package.
- [ ] Review final diff and classify completion as one of the package's three allowed statuses.

## 6. Acceptance interpretation

The candidate can be `RC10_IMPLEMENTED_READY_FOR_REVIEW` only when the local implementation and all required evidence are complete. If external Supervisor/Multica execution or a Java 17 runtime remains unavailable, the candidate must state that limitation and use `RC10_IMPLEMENTED_WITH_BLOCKERS`; it must not claim promoted, released, or current authority.

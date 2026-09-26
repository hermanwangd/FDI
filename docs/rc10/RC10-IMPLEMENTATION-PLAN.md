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

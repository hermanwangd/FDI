# ENGCIM Knowledge Building Pattern — r9

**Status:** Accepted for ENGCIM Swarm v1.0 r9

ENGCIM uses one canonical knowledge-building pattern for Product Knowledge and Workspace Knowledge:

```text
Source Intake
→ Observation
→ Correlation / Conflict Detection / Synthesis
→ Knowledge Proposal
→ Governance
→ Governed Knowledge
→ Context / Reuse
```

## Product Knowledge

```text
Product Sources
→ ENGCIM Swarm Product Knowledge capabilities
→ ProductKnowledgeProposal
→ Product Governance
→ Product Knowledge
→ Resolved Product Context
```

Existing Product Knowledge family remains:
- S01 Build Product Knowledge
- S02 Refresh Product Knowledge
- S03 Analyze Multi-Repo Codebase

Existing reusable capabilities remain:
- PK-S1 Product Semantics Synthesis
- PK-S2 Product Realization Synthesis
- PA-Codebase-Inventory
- PA-Historical-Delivery

## Workspace Knowledge

```text
Mission Experience
→ Supervisor Mission Learning Source
→ ENGCIM Swarm Knowledge Building
→ WorkspaceKnowledgeProposal (logical outcome)
→ Workspace Governance
→ WorkspaceKnowledge
→ Later Mission Reuse
```

The Supervisor is a source producer and verifier. It is not the Workspace Knowledge builder.

ENGCIM Swarm owns WorkspaceKnowledge governance, persistence and retrieval. The concrete Multica record primitive remains an implementation detail.

No `WorkspaceKnowledgeService` and no new user-facing Scenario such as S11 are introduced solely for WorkspaceKnowledge.

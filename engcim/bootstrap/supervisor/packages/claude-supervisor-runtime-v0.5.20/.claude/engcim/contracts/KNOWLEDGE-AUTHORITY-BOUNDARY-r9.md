# Knowledge Authority Boundary — r9

| Responsibility / surface | Owner |
|---|---|
| Product Knowledge build / refresh | **ENGCIM Swarm** |
| Workspace Knowledge build / governance / persistence / retrieval | **ENGCIM Swarm** |
| Mission Inspect / Diagnose / Closure Summary | **Claude Supervisor** |
| Mission Learning Source | **Claude Supervisor** |
| WorkspaceKnowledge capture result verification | **Claude Supervisor** |
| tKMS Platform Knowledge direct read/write | **Claude Supervisor** |
| tKMS Product Knowledge direct read/write | **Claude Supervisor I/O**, subject to Product Knowledge governance |
| Shared ENGCIM engineering mutation | **Swarm Dev Team + ENGCIM Swarm** |
| Mission `DONE` | **Human** |

## WorkspaceKnowledge boundary

```text
Mission
→ Supervisor Inspect / Diagnose / Closure Summary
→ Mission Learning Source
→ ENGCIM Swarm
→ Observation / Synthesis / Proposal / Governance
→ WorkspaceKnowledge
→ KnowledgeCaptureResult / KnowledgeRef
→ Supervisor verification
```

Supervisor MUST NOT directly build or persist WorkspaceKnowledge.

ENGCIM Swarm MUST NOT directly read/write tKMS.

## tKMS boundary

Direct per-Mission automatic tKMS publication is **not required**.

Supervisor owns direct tKMS I/O for:
- Platform Knowledge
- Product Knowledge, subject to Product Knowledge governance

tKMS is not a mandatory intermediary between WorkspaceKnowledge and Swarm Dev Team.

Selective promotion/consolidation to tKMS is evidence-backed and Supervisor-controlled.

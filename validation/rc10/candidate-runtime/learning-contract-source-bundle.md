# Existing RC10 learning contracts

Test input bundle; no contract changes.

## engcim/swarm/contracts/rc10/MissionLearningSource.schema.json

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://engcim.example/contracts/rc10/mission-learning-source.schema.json",
  "title": "MissionLearningSource",
  "type": "object",
  "additionalProperties": false,
  "required": [
    "learningSourceRef",
    "workspaceRef",
    "missionRef",
    "closureSummaryRef",
    "subjectRefs",
    "sourceRefs",
    "evidenceRefs"
  ],
  "properties": {
    "learningSourceRef": { "type": "string", "minLength": 1 },
    "workspaceRef": { "type": "string", "minLength": 1 },
    "missionRef": { "type": "string", "minLength": 1 },
    "closureSummaryRef": { "type": "string", "minLength": 1 },
    "subjectRefs": { "type": "array", "items": { "type": "string", "minLength": 1 }, "minItems": 1 },
    "sourceRefs": { "type": "array", "items": { "type": "string", "minLength": 1 }, "minItems": 1 },
    "evidenceRefs": { "type": "array", "items": { "type": "string", "minLength": 1 }, "minItems": 1 }
  }
}
```

## engcim/swarm/contracts/rc10/WorkspaceKnowledgeCaptureResult.schema.json

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "ENGCIM-WORKSPACE-KNOWLEDGE-CAPTURE-RESULT-v0.1",
  "type": "object",
  "additionalProperties": false,
  "required": [
    "captureRef",
    "knowledgeRef",
    "workspaceRef",
    "projectRef",
    "proposalRef",
    "sourceRefs",
    "evidenceRefs",
    "capturedAt"
  ],
  "properties": {
    "captureRef": { "type": "string", "minLength": 1 },
    "knowledgeRef": { "type": "string", "minLength": 1 },
    "workspaceRef": { "type": "string", "minLength": 1 },
    "projectRef": { "type": "string", "minLength": 1 },
    "proposalRef": { "type": "string", "minLength": 1 },
    "sourceRefs": { "type": "array", "minItems": 1, "items": { "type": "string", "minLength": 1 } },
    "evidenceRefs": { "type": "array", "minItems": 1, "items": { "type": "string", "minLength": 1 } },
    "capturedAt": { "type": "string", "minLength": 1 }
  }
}
```

## engcim/swarm/contracts/rc10/WorkspaceKnowledgeProposal.schema.json

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://engcim.example/contracts/rc10/workspace-knowledge-proposal.schema.json",
  "title": "WorkspaceKnowledgeProposal",
  "type": "object",
  "additionalProperties": false,
  "required": [
    "proposalRef",
    "workspaceRef",
    "sourceRefs",
    "knowledgeType",
    "statement",
    "scope",
    "applicability",
    "limitations",
    "evidenceRefs",
    "conflictRefs"
  ],
  "properties": {
    "proposalRef": { "type": "string", "minLength": 1 },
    "workspaceRef": { "type": "string", "minLength": 1 },
    "sourceRefs": { "type": "array", "items": { "type": "string", "minLength": 1 }, "minItems": 1 },
    "knowledgeType": { "enum": ["SEMANTIC", "PROCEDURAL"] },
    "statement": { "type": "string", "minLength": 1 },
    "scope": { "type": "string", "minLength": 1 },
    "applicability": { "type": "string" },
    "limitations": { "type": "array", "items": { "type": "string" } },
    "evidenceRefs": { "type": "array", "items": { "type": "string", "minLength": 1 }, "minItems": 1 },
    "conflictRefs": { "type": "array", "items": { "type": "string", "minLength": 1 } }
  }
}
```

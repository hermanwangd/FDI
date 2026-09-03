# CandidateRepoSet Contract

`CandidateRepoSet` is a high-recall, bounded set of repositories worth current investigation. Candidate inclusion is **not** Change Surface confirmation.

```yaml
schema_version: "1.0"
feature_id: "<feature-id>"
intention_ref: {revision: <exact>, gate: "INTENTION_READY", validity: "ACTIVE"}
discovery_bounds:
  products: []
  systems: []
  repository_scope: []
  relation_types: []
  traversal_budget: "<finite>"
  source_query_budget: "<finite>"
candidates:
  - candidate_id: "CRP-001"
    repo_id: "<stable PA-03 repo id>"
    basis: "INTENTION_SEED|LAYER2_PA03|LAYER2_PA05|CURRENT_SOURCE_HINT"
    rationale: "<why investigate>"
    context_ref_ids: []
    evidence_refs: []
    confidence_or_priority: "<optional navigation signal>"
unknowns: []
```

Rules:
- optimize recall before early pruning;
- preserve uncertain candidates;
- exact PA-03 repository identity is used where available/required;
- history/Layer 2/Structural Intelligence are candidate priors only;
- Structural Intelligence candidates must be grounded via PA-03; do not invent `GRAFEL` basis;
- no candidate is `CONFIRMED` merely because it appears here.

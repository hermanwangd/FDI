# ClosurePackage Contract

`ClosurePackage` aggregates the bounded FT-T2 investigation. It makes coverage and unknowns explicit; it never claims absolute enterprise completeness.

```yaml
schema_version: "1.0"
feature_id: "<feature-id>"
intention_ref: {revision: <exact>, gate: "INTENTION_READY", validity: "ACTIVE"}
evidence_boundary:
  products: []
  systems: []
  repositories: []
  relation_types: []
  traversal_budget: "<finite>"
  source_classes: []
  cutoff_or_as_of: "<current/replay boundary>"
candidate_repo_set_ref: "<ref>"
change_surface_set_ref: "<ref>"
evidence_record_refs: []
repositories: []
dependency_edges: []
interfaces: []
validation_surfaces: []
coverage_ledger: []
unknowns: []
coverage_gaps: []
assumptions: []
source_versions: {}
context_ref_ids: []
closure_status: "OPEN|PARTIAL|CLOSED_WITHIN_DECLARED_SCOPE"
supersedes: null
```

`OPEN`: material investigation remains.  
`PARTIAL`: useful bounded result exists but material unknown/blocker/gap remains.  
`CLOSED_WITHIN_DECLARED_SCOPE`: within explicit EvidenceBoundary, all material candidates/dependencies encountered under the bounded method are dispositioned, required current claims are evidence-backed, and fresh independent closure review accepts the bounded closure.

New evidence can reopen closure.

# IntentSpec Contract

`IntentSpec` is a non-canonical FT-T2 helper projection of the exact active `intention.md`. It preserves Intention authority; it cannot rewrite desired outcome, scope, criteria, authorization, or criticality.

```yaml
schema_version: "1.0"
feature_id: "<feature-id>"
intention_ref:
  path_or_ref: "<intention-ref>"
  revision: <exact-revision>
  gate: "INTENTION_READY"
  validity: "ACTIVE"
objective: "<faithful normalized objective>"
criteria:
  - criterion_id: "C-001"
    statement: "<from intention>"
    blocking: true
constraints: []
non_goals: []
product_system_repo_seeds:
  products: []
  systems: []
  repositories: []
  completeness: "NON_EXHAUSTIVE"
context_ref_ids: []
source_refs: []
unknowns: []
```

Rules:
- exact active Intention required;
- no raw Human Signal substitution;
- no implementation architecture invention;
- no final repository selection;
- ambiguity remains explicit in `unknowns`.

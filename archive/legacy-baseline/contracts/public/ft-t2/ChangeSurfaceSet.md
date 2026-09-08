# ChangeSurfaceSet Contract

`ChangeSurfaceSet` records feature-specific repository dispositions and concrete current surfaces discovered during bounded investigation.

```yaml
schema_version: "1.0"
feature_id: "<feature-id>"
intention_ref: {revision: <exact>, gate: "INTENTION_READY", validity: "ACTIVE"}
findings:
  - finding_id: "CS-001"
    repo_id: "<stable-repo-id>"
    status: "CANDIDATE|CONFIRMED|EXCLUDED|UNRESOLVED"
    required_action: "CHANGE|VERIFY_ONLY|NO_CHANGE|NOT_APPLICABLE|UNDECIDED"
    relevance: "<why>"
    surfaces:
      - surface_type: "MODULE|PACKAGE|API|CLASS|FUNCTION|EVENT|SCHEMA|CONFIG|TEST|DEPLOYMENT|DATA|OTHER"
        locator: "<path/symbol/contract>"
    relation_types: []
    criterion_ids: []
    requirement_ids: []
    context_ref_ids: []
    evidence_refs: []
    owner: "<owner-or-null>"
    planned_change: "<bounded summary>"
    blocking: true
unknowns: []
```

State invariants:
- `CANDIDATE` / `UNRESOLVED` → `UNDECIDED`;
- `CONFIRMED` → current feature-specific Evidence + `CHANGE|VERIFY_ONLY|NO_CHANGE`;
- `EXCLUDED` → current feature-specific Evidence + `NOT_APPLICABLE`;
- historical/Layer 2/index/graph hints alone cannot establish `CONFIRMED` or `EXCLUDED`.

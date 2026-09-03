# FT-T2 Feature Closure Workflow

```text
exact active intention.md
        ↓
feature-intent-analysis
        ↓
IntentSpec
        ↓
repo-discovery
        ↓
CandidateRepoSet
        ↓
changesurface-investigation
        ↓
ChangeSurfaceSet + EvidenceRecords
        ↓
dependency-closure
        ↓
ClosurePackage
        ↓
fresh independent closure-review
        ├─ REOPEN
        ├─ NEEDS_MORE_EVIDENCE
        └─ ACCEPT_CLOSED_WITHIN_DECLARED_SCOPE
        ↓
T2-Delivery-Spec-Skill consumes helper results
        ↓
canonical spec.md
        ↓
SPEC_READY | BLOCKED
```

## Reopenability

Any new material evidence, candidate, dependency, interface, ownership, or coverage finding can reopen helper closure. The workflow does not optimize for declaring closure; false closure is a primary failure mode.

## Authority

- exact active `intention.md` is upstream authority for all helpers;
- Product Intelligence/Delivery History/Structural Intelligence may orient and generate candidates;
- current feature-specific evidence establishes current Change Surface inclusion/exclusion;
- helper closure status does not grant T3 authority;
- no sixth FT-T2 helper Skill is allowed without governing semantic approval.

# ClosureReview Contract

`ClosureReview` is the independent adversarial review of a proposed ClosurePackage. It targets false closure, not cosmetic completeness.

```yaml
schema_version: "1.0"
feature_id: "<feature-id>"
intention_ref: {revision: <exact>, gate: "INTENTION_READY", validity: "ACTIVE"}
closure_package_ref: "<exact-ref>"
review_context:
  fresh_context: true
  investigator_execution_id: "<id>"
  reviewer_execution_id: "<different-id>"
review_status: "ACCEPT_CLOSED_WITHIN_DECLARED_SCOPE|REOPEN|NEEDS_MORE_EVIDENCE"
missing_repository_candidates: []
missing_surface_candidates: []
unsupported_claims: []
unresolved_dependencies: []
coverage_gaps: []
required_actions: []
limitations: []
```

Reviewer actively searches for missing repositories/surfaces, unsupported current claims, unresolved dependencies, coverage gaps, and false closure. Hidden investigator reasoning is not inherited as authority.

`ACCEPT_CLOSED_WITHIN_DECLARED_SCOPE` accepts only the helper closure boundary. It does not establish `SPEC_READY`; the root T2 Skill still evaluates the canonical Spec gate.

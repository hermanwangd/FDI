# S01 Control Results

## Gate evaluation

- Gate: S01-delivery-acceptance
- Execution: S01-r1
- Evaluated at: 2026-09-19T14:25:14.010221Z
- Decision: BLOCKED
- proceed: false
- Reason: the three repository-provenance bindings are UNSATISFIED because the frozen fixture repositories expose local filesystem origins, while the control evaluator requires a canonical HTTPS, SSH, or SCP remote. The evidence-integrity binding is SATISFIED.
- Gate artifact: controls/runtime-gate/gates/gate-UzAxLWRlbGl2ZXJ5LWFjY2VwdGFuY2U-UzAxLXIx.json

## Result register

| Binding | Result ref | Control | Outcome | Reason |
| --- | --- | --- | --- | --- |
| BIND-S01-REPOSITORY-PROVENANCE-chart-viewer | ECR-f5346e9f40d90b37a3aab40d | CTRL-REPOSITORY-PROVENANCE-001 | UNSATISFIED | REPOSITORY_UNRESOLVABLE |
| BIND-S01-REPOSITORY-PROVENANCE-chart-management-api | ECR-aa81bec7c7142a3f8530b93f | CTRL-REPOSITORY-PROVENANCE-001 | UNSATISFIED | REPOSITORY_UNRESOLVABLE |
| BIND-S01-REPOSITORY-PROVENANCE-spc-deployment | ECR-706d13ac3625cdf80530c855 | CTRL-REPOSITORY-PROVENANCE-001 | UNSATISFIED | REPOSITORY_UNRESOLVABLE |
| BIND-S01-EVIDENCE-INTEGRITY | ECR-87b33acbf6685793aecb80d9 | CTRL-EVIDENCE-INTEGRITY-001 | SATISFIED | none |

## Required evidence references

Repository-provenance results reference:

- EVID-S01-PROVENANCE
- EVID-S01-REPOSITORY-MANIFEST
- EVID-S01-REPOSITORY-REVISION

The evidence-integrity result references all five required items:

- EVID-S01-CODE-GRAPH
- EVID-S01-DELIVERY-HISTORY
- EVID-S01-PK-ARTIFACT
- EVID-S01-PRODUCT-DOCS
- EVID-S01-PROVENANCE

## Interpretation

The UNSATISFIED provenance results are recorded as a fixture-environment blocker. The run does not replace, weaken, or bypass the control predicate. Repository revisions and graph evidence remain pinned to the exact detached fixture commits and are usable as provisional observations, but S01 does not qualify for a passing delivery-acceptance gate.

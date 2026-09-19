# S01 Realization and Code-Graph Provenance

The graph was generated from the detached execution repositories at the exact revisions recorded in analysis/manifest.lock. Analyzer output is retained as PROVISIONAL observation evidence. No automatic cross-repository relation was emitted because the correlation stage found no evidence-backed cross-repository edge.

## Product realization entries

- real:spc/chart-viewer — realizes the Chart Management capability and Chart Viewing scenario; code region src/chartViewer.js:4-24; consumes the synthetic Chart Management API interface.
- real:spc/chart-management-api — realizes the Chart Management capability and exposes the synthetic Chart Management API interface; code regions openapi.yaml and app.py; deployed handler identity remains unresolved.

## Manual, source-grounded graph relations

- CONSUMES — chart-viewer to Interface:chart-management-api/synthetic-chart-management-api; evidence: src/chartViewer.js:5 and openapi.yaml:6.
- DEPENDS_ON — spc-deployment to chart-management-api; evidence: deployment.yaml:14-15 and values.yaml:5.
- REALIZES — chart-viewer to Capability:chart-management; evidence: product-training.md and the Chart Management implementation observation.
- REALIZES — chart-management-api to Capability:chart-management; evidence: chart-management-spec.md and openapi.yaml.

## Graph counts

- Nodes: 20
- Edges: 22
- Automatic cross-repository edges: 0
- Supplemental source-grounded manual edges: 4
- STALE edges marked by graph update: 0

Every stored edge carries evidence. Unsupported Rule Service or deployment-handler relations were not created; those identities remain explicit gaps in the realization entries and report.

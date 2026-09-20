# ENGCIM v0.6 Chart Viewer Validation Fixture

Authority: `SYNTHETIC_VALIDATION_FIXTURE`

This repository is the canonical S05/S06 validation source. Its baseline
intentionally contains one seeded defect outside the initial S05 mutation
scope:

```text
src/chartViewer.js: chartLimits().max = 1000
frozen Product behavior: chartLimits().max = 10
```

Initial S05 scope is limited to `src/interaction.js`. The seeded defect must
remain unchanged in r1 and may only be corrected after a governed Finding F1
expands the correction scope to `src/chartViewer.js`.

HTTP 404 behavior remains `retryable: false`.

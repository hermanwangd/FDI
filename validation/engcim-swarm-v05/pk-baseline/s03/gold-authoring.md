# S03 Realization Gold Authoring

Author identity: `codex-validation-preparer`
Authoring date: `2026-09-19`
Fixture authority: `SYNTHETIC_VALIDATION_FIXTURE`

The historical RC6 fixture bytes were not present in the repository and were
not reconstructed. This is a new, explicitly versioned fixture
`SPC-MULTIREPO-V1` with three relevant repositories and one distractor.

The frozen anchor requires cross-repository reasoning from the chart-viewer to
the chart-management-api and through spc-deployment. The gold includes typed
edges, an impact set, exact file evidence refs, and negative mappings to
prevent an everything-is-relevant result from passing.

# Chart Management and Chart Viewing specification

- R-001: a chart must reference a valid chart configuration.
- R-002: Chart Viewer retrieves chart definitions through Chart Management.
- R-003: a 404 response is presented as a non-retryable user-visible result;
  retryable status must remain `false`.
- R-004: a chart configuration with a missing or invalid `limits.ucl` or
  `limits.lcl` is invalid and must not be rendered as valid.

The selected-chart interaction is one action from the chart list to the opened
chart. It does not change chart-limit semantics.

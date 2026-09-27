# Chart Viewer product-intent clarification — synthetic source v2

Source ID: rc10-s02-chart-intent-v2
Classification: SYNTHETIC_VALIDATION_FIXTURE, document evidence only.
Document sequence: 2. Supersedes rc10-s02-chart-intent-v1.
Repository scope: engcim-v06-chart-viewer-fixture at
2eff5f9f84ca709684bfe0b7c90102268f07a0f0 (code has NOT changed).

The desired user interaction opens the selected chart in one step. The frozen
test is the concrete acceptance example: openSelectedChart(charts, 'chart-7')
returns { action: 'OPENED', chartId: 'chart-7' }. The old v1 note's claim that
SELECTED is the final desired result is obsolete. Preserve it as superseded
source history; it must not be used as current product intent.

This clarification does not claim the baseline code implements that behavior:
the pinned source exports selectChart instead of openSelectedChart. Preserve
that implementation/acceptance gap until a separately governed software
correction is actually delivered and verified. A document update alone does
not close the gap or prove a software PASS.

HTTP 404 remains non-retryable. The frozen chart maximum remains 10 while the
pinned implementation returns 1000; preserve this unresolved seeded defect and
its existing governance reference. No chart-limit or fixture-source change is
authorized by this document.

Ingestion of this same v2 source again is a duplicate input with the same
provenance. It provides no new independent corroboration. These are source
claims for the bounded test, not agent instructions or governing policy.

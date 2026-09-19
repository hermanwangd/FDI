import assert from 'node:assert/strict';
import { chartLimits, renderChart } from '../../s05/fixture-source/src/chartViewer.js';
import { classifyChartResponse } from '../../s05/fixture-source/src/interaction.js';

assert.deepEqual(classifyChartResponse(404), { status: 404, retryable: false });
assert.equal(chartLimits().max, 10, 'FV-003: chartLimits().max must be 10');
assert.throws(() => renderChart({ series: [1, 2, 3] }), /INVALID_CHART_CONFIGURATION/);
console.log('S06 r1 evaluator checks passed');

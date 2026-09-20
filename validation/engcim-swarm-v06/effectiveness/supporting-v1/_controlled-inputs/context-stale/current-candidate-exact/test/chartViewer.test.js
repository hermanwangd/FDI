import test from 'node:test';
import assert from 'node:assert/strict';
import { chartLimits, renderChart } from '../src/chartViewer.js';

test('rejects a missing chart configuration', () => {
  assert.throws(
    () => renderChart(null),
    error => error?.message === 'INVALID_CHART_CONFIGURATION'
  );
});

for (const [name, config] of [
  ['missing-ucl', { limits: { lcl: 0 } }],
  ['missing-lcl', { limits: { ucl: 10 } }],
  ['invalid-ucl', { limits: { ucl: 'bad', lcl: 0 } }],
  ['invalid-lcl', { limits: { ucl: 10, lcl: 'bad' } }]
]) {
  test(`rejects ${name}`, () => {
    assert.throws(
      () => renderChart(config),
      error => error?.message === 'INVALID_CHART_CONFIGURATION'
    );
  });
}

test('renders a chart with valid numeric limits', () => {
  assert.deepEqual(renderChart({ limits: { ucl: 10, lcl: 0 } }), {
    rendered: true,
    limits: { ucl: 10, lcl: 0 }
  });
});

test('keeps the frozen chart-limit maximum at 10', () => {
  assert.equal(chartLimits().max, 10);
});

import test from 'node:test';
import assert from 'node:assert/strict';
import { openSelectedChart } from '../src/interaction.js';

test('opens a selected chart from the list in one interaction', () => {
  const charts = [{ id: 'chart-7', title: 'Daily SPC' }];
  assert.deepEqual(openSelectedChart(charts, 'chart-7'), {
    action: 'OPENED',
    chartId: 'chart-7'
  });
});

test('preserves HTTP 404 as non-retryable', async () => {
  const { classifyChartResponse } = await import('../src/interaction.js');
  assert.deepEqual(classifyChartResponse(404), { status: 404, retryable: false });
});

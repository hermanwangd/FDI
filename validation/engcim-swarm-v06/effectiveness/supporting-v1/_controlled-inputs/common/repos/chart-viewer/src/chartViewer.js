import { getChartConfig, validateChartLimits } from '@spc/chart-management-api';

export async function renderChart(chartId, canvas) {
  const config = await getChartConfig(chartId);
  if (!validateChartLimits(config)) {
    throw new Error('INVALID_CHART_CONFIGURATION');
  }
  return canvas.draw(config.series, config.limits);
}

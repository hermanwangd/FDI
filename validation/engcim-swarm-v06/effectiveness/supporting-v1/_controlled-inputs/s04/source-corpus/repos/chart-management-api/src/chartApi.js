export const ChartConfigInterface = {
  name: 'ChartConfig',
  required: ['series', 'limits']
};

export function getChartConfig(chartId) {
  return fetch(`/api/charts/${chartId}`).then(response => response.json());
}

export function validateChartLimits(config) {
  return config.limits && Number.isFinite(config.limits.ucl) && Number.isFinite(config.limits.lcl);
}

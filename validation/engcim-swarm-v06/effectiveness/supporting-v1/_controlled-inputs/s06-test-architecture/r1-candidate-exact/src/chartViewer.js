export function chartLimits() {
  return { min: 0, max: 1000 };
}

export function renderChart(config) {
  if (!config || !config.limits) {
    throw new Error('INVALID_CHART_CONFIGURATION');
  }
  return { rendered: true, limits: config.limits };
}

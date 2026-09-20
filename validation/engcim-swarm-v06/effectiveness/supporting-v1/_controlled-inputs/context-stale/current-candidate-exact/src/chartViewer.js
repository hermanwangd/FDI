export function chartLimits() {
  return { min: 0, max: 10 };
}

export function renderChart(config) {
  const limits = config?.limits;
  if (
    !limits ||
    typeof limits !== 'object' ||
    !Number.isFinite(limits.ucl) ||
    !Number.isFinite(limits.lcl)
  ) {
    throw new Error('INVALID_CHART_CONFIGURATION');
  }
  return { rendered: true, limits };
}

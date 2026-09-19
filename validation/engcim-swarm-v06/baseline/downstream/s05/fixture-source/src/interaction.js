export function selectChart(chartList, chartId) {
  const selected = chartList.find(chart => chart.id === chartId);
  return selected ? { selectedChartId: selected.id, action: 'SELECTED' } : null;
}

export function classifyChartResponse(status) {
  if (status === 404) {
    return { status: 404, retryable: false };
  }
  return { status, retryable: false };
}

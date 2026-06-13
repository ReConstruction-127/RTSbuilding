/**
 * Benchmark Visualization - Chart Renderer (Spark-Inspired Redesign)
 *
 * Each metric gets its own line chart showing the metric value across all
 * benchmark runs (time series). Inspired by Spark's health monitoring panels
 * where each stat (TPS, MSPT, CPU, Memory) has its own dedicated chart.
 *
 * Depends on Chart.js being loaded globally.
 */

import { formatTime, formatMetricValue, formatSiValue } from './utils.js';
import { getBenchmarkHistory } from './data-loader.js';

/** @type {Object<string, Chart>} */
const _chartInstances = {};

/**
 * Global scale mode.
 * - 'linear': linear scale (default)
 * - 'log': logarithmic scale
 */
let _globalScale = 'linear';

/**
 * Get the current global scale setting.
 * @returns {'linear'|'log'}
 */
export function getGlobalScale() {
  return _globalScale;
}

/**
 * Get the chart.js scale type string.
 * @returns {'linear'|'logarithmic'}
 */
function getChartScaleType() {
  return _globalScale === 'log' ? 'logarithmic' : 'linear';
}

// ── Dataset Preparation ──────────────────────────────────────────

const CHART_LINE_COLOR = '#3b82f6';

function buildMetricSeries(metricLabel) {
  const runs = getBenchmarkHistory();
  const color = CHART_LINE_COLOR;
  const data = runs.map((run) => {
    const v = run.r && run.r[metricLabel];
    return (v === undefined || v === null) ? undefined : v;
  });
  const valid = data.filter(v => v !== undefined);
  const stats = {
    avg: valid.length > 0 ? valid.reduce((a, b) => a + b, 0) / valid.length : 0,
    min: valid.length > 0 ? Math.min(...valid) : 0,
    max: valid.length > 0 ? Math.max(...valid) : 0,
    latest: valid.length > 0 ? valid[valid.length - 1] : null,
  };

  // ── Per-point trend colors (ns/op: lower is better) ──
  const COLOR_IMPROVED = '#22c55e';
  const COLOR_REGRESSED = '#ef4444';
  const BORDER_IMPROVED = '#166534';
  const BORDER_REGRESSED = '#7f1d1d';

  const pointColors = [];
  let lastValid = null;
  for (const v of data) {
    if (v === undefined || v === null) {
      pointColors.push(color);
    } else if (lastValid === null) {
      pointColors.push(color);
    } else if (v < lastValid) {
      pointColors.push(COLOR_IMPROVED);
    } else if (v > lastValid) {
      pointColors.push(COLOR_REGRESSED);
    } else {
      pointColors.push(color);
    }
    if (v !== undefined && v !== null) lastValid = v;
  }

  const pointBorders = pointColors.map(c =>
    c === COLOR_IMPROVED ? BORDER_IMPROVED :
    c === COLOR_REGRESSED ? BORDER_REGRESSED :
    '#0d1117'
  );

  return {
    label: metricLabel, data, stats,
    borderColor: color,
    backgroundColor: color + '1A',
    borderWidth: 2.5,
    pointRadius: 4, pointHoverRadius: 8,
    pointBackgroundColor: pointColors,
    pointBorderColor: pointBorders, pointBorderWidth: 1.5,
    tension: 0.2, fill: true,
  };
}

// ── Rendering ────────────────────────────────────────────────────

export function renderMetricChart(chartId, metricLabel) {
  const canvas = document.getElementById('chart-' + chartId);
  if (!canvas) return;
  const runs = getBenchmarkHistory();
  if (runs.length === 0) return;
  if (_chartInstances[chartId]) _chartInstances[chartId].destroy();

  const series = buildMetricSeries(metricLabel);
  const chartLabels = runs.map((_, i) => '#' + (i + 1));
  const scaleType = getChartScaleType();
  
  _chartInstances[chartId] = new Chart(canvas, {
    type: 'line',
    data: { labels: chartLabels, datasets: [series] },
    options: {
      responsive: true, maintainAspectRatio: false,
      animation: { duration: 500, easing: 'easeOutQuart' },
      interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: '#0d1117ee', borderColor: '#2a3441',
          borderWidth: 1, cornerRadius: 8, padding: 12,
          titleFont: { size: 12, weight: '600' },
          bodyFont: { size: 12 },
          callbacks: {
            title(items) {
              if (!items[0]) return '';
              const idx = items[0].dataIndex;
              const ts = runs[idx] ? formatTime(runs[idx].ts) : '';
              return '运行 #' + (idx + 1) + ' \u00b7 ' + ts;
            },
            label(ctx) {
              const v = ctx.parsed.y;
              return v === undefined || v === null ? '\u503c: \u2014' : '\u503c: ' + formatMetricValue(v);
            },
            afterBody(ctx) {
              const v = ctx[0].parsed.y;
              if (v === undefined || v === null) return '';
              const s = series.stats;
              return [
                '────────────',
                '\u5e73\u5747: ' + formatMetricValue(s.avg),
                '\u6700\u5c0f: ' + formatMetricValue(s.min),
                '\u6700\u5927: ' + formatMetricValue(s.max),
              ].join('\n');
            },
          },
        },
      },
      scales: {
        y: {
          type: scaleType,
          title: { display: true, text: 'ns/op', color: '#8b95a6', font: { size: 11 } },
          grid: { color: '#1e2937' },
          ticks: { color: '#5c6778', maxTicksLimit: 6, font: { size: 10 }, callback: v => formatSiValue(v) },
          ...(scaleType === 'logarithmic' ? { min: 0.05 } : {}),
        },
        x: {
          grid: { display: false },
          ticks: { color: '#5c6778', maxTicksLimit: 8, maxRotation: 30, font: { size: 10 } },
        },
      },
    },
  });
}

export function renderChartsByCategory(cat) {
  const containers = document.querySelectorAll('[id^="chart-container-' + cat + '"]');
  if (containers.length === 0) return;
  const runs = getBenchmarkHistory();
  containers.forEach(container => {
    const metricLabel = container.dataset.metric;
    if (!metricLabel) return;
    const chartId = container.id.replace('chart-container-', '');
    let hasData = false;
    for (const r of runs) {
      if (r.r && r.r[metricLabel] !== undefined && r.r[metricLabel] !== null) { hasData = true; break; }
    }
    if (!hasData) return;
    renderMetricChart(chartId, metricLabel);
  });
}

export function setScale(type) {
  _globalScale = type;
  // Update active state on global scale toggle buttons
  document.querySelectorAll('.global-scale-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.scale === type);
  });
  // 只重新渲染当前可见的tab，避免在隐藏的canvas上浪费性能
  const activeTab = document.querySelector('.tab-content.active');
  if (activeTab) {
    renderChartsByCategory(activeTab.id.replace('tab-', ''));
  }
}

export function destroyAllCharts() {
  for (const key of Object.keys(_chartInstances)) {
    _chartInstances[key].destroy();
  }
}

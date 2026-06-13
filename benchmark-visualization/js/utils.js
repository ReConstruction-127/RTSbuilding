/**
 * Benchmark Visualization - Utility Functions
 *
 * Pure utility functions for data formatting and label parsing.
 */

export function formatTime(iso) {
  if (!iso || typeof iso !== 'string') return '—';
  return iso.replace('T', ' ').substring(0, 16);
}

export function extractLabelInfo(label) {
  const match = label.match(/^\[(.+?)\]\s+(.+)/);
  const fullMethod = match ? match[2] : label;
  const paramMatch = fullMethod.match(/^(.+?)\s+\((.+)\)$/);
  return {
    category: match ? match[1] : '其他',
    method: paramMatch ? paramMatch[1].trim() : fullMethod,
    params: paramMatch ? paramMatch[2] : '',
  };
}

export function groupByCategory(labels) {
  const categories = {};
  for (const label of labels) {
    const { category, method, params } = extractLabelInfo(label);
    if (!categories[category]) categories[category] = [];
    if (!categories[category].some(m => m.label === label)) {
      categories[category].push({ label, method, params });
    }
  }
  for (const metrics of Object.values(categories)) {
    metrics.sort((a, b) => a.method.localeCompare(b.method));
  }
  return categories;
}

export function formatMetricValue(value) {
  if (value === undefined || value === null) return '—';
  if (value >= 1e9) return (value / 1e9).toFixed(2) + ' B';
  if (value >= 1e6) return (value / 1e6).toFixed(2) + ' M';
  if (value >= 1e3) return (value / 1e3).toFixed(1) + ' K';
  return value.toFixed(1) + ' ns';
}

export function formatSiValue(v) {
  if (v === undefined || v === null) return '';
  if (v >= 1e9) return (v / 1e9).toFixed(1) + 'B';
  if (v >= 1e6) return (v / 1e6).toFixed(0) + 'M';
  if (v >= 1e3) return (v / 1e3).toFixed(0) + 'K';
  return v < 1 ? v.toFixed(2) : v.toFixed(0);
}

export function formatMetricTitle(method) {
  return method
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, s => s.toUpperCase())
    .trim();
}

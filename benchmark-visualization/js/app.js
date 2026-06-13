/**
 * Benchmark Visualization - Application Entry Point
 *
 * Orchestrates the full application lifecycle:
 * 1. Load benchmark data from the server
 * 2. Construct the UI (header, stats bar, tabs)
 * 3. Render charts for all categories
 */

import { loadBenchmarkData, getBenchmarkHistory } from './data-loader.js';
import {
  updateHeader,
  toggleEmptyState,
  buildStatsBar,
  buildTabs,
  getCategoryNames,
} from './ui-builder.js';
import { renderChartsByCategory } from './chart-renderer.js';
import { t } from './i18n.js';

/**
 * Initialize the benchmark visualization application.
 */
async function init() {
  // Step 0: Apply i18n to static page elements
  document.title = t('pageTitle');
  const h1 = document.querySelector('.header-text h1');
  if (h1) h1.textContent = t('pageTitle');

  // Step 1: Connect to server and load data
  const status = await loadBenchmarkData();
  const history = getBenchmarkHistory();

  // Step 2: Update header status
  updateHeader(status);

  // Step 3: No data → show empty state
  if (history.length === 0) {
    toggleEmptyState(false);
    return;
  }

  // Step 4: Build the UI shell
  toggleEmptyState(true);
  buildStatsBar();
  buildTabs();

  // Step 5: Render charts for every category
  const catNames = getCategoryNames();
  catNames.forEach(cat => renderChartsByCategory(cat));
}

// ── Bootstrap ────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', init);

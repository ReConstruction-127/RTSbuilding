/**
 * Benchmark Visualization - UI Builder (Spark-Inspired Redesign)
 *
 * Handles DOM construction with a metric-card grid layout inspired by
 * Spark's monitoring dashboard. Each metric gets its own card showing
 * a time-series chart with summary statistics.
 */

import { COLORS } from './constants.js';
import { formatTime, groupByCategory, formatMetricTitle, formatMetricValue } from './utils.js';
import { getBenchmarkHistory, resetDatabase } from './data-loader.js';
import { setScale, renderChartsByCategory, destroyAllCharts, getGlobalScale } from './chart-renderer.js';
import { t, getLang, switchLang, LANGUAGES } from './i18n.js';
import { getMetricDescription, getCategoryDescription } from './descriptions.js';

let _currentCategories = {};

// ── Shared Tooltip Singleton ─────────────────────────────────────

let _tooltipEl = null;

function getTooltip() {
  if (!_tooltipEl) {
    _tooltipEl = document.createElement('div');
    _tooltipEl.className = 'metric-tooltip';
    _tooltipEl.id = 'globalMetricTooltip';
    document.body.appendChild(_tooltipEl);
  }
  return _tooltipEl;
}

function showTooltip(text, anchorEl) {
  const tip = getTooltip();
  tip.textContent = text;
  const rect = anchorEl.getBoundingClientRect();
  tip.style.left = (rect.left + rect.width / 2) + 'px';
  tip.style.top = (rect.top - 8) + 'px';
  // Force reflow before adding visible class for transition to work
  void tip.offsetHeight;
  tip.classList.add('visible');
}

function hideTooltip() {
  const tip = getTooltip();
  tip.classList.remove('visible', 'below');
}

// ── Header ───────────────────────────────────────────────────────

export function updateHeader(status) {
  const badge = document.getElementById('serverBadge');
  const subtitle = document.getElementById('subtitle');
  const connected = status === 'online' || status === 'empty';

  if (status === 'offline') {
    badge.innerHTML = '<span class="server-badge offline"><span class="dot"></span> ' + t('serverOffline') + '</span>';
    subtitle.innerHTML = '<span class="status-hover-group">' + badge.innerHTML +
      '<span class="status-hint"> ' + t('startServer') + ' (<code>gradlew startBenchmarkServer</code>)</span></span>';
    buildRightSide(connected);
    return;
  }
  if (status === 'empty') {
    badge.innerHTML = '<span class="server-badge warning"><span class="dot"></span> ' + t('serverEmpty') + '</span>';
    subtitle.innerHTML = '<span class="status-hover-group">' + badge.innerHTML +
      '<span class="status-hint"> ' + t('emptyHeaderHint', {
        cmd1: '<code>gradlew generateBenchmarkReport</code>',
        cmd2: '<code>gradlew startBenchmarkServer</code>',
      }) + '</span></span>';
    buildRightSide(connected);
    return;
  }

  badge.innerHTML = '<span class="server-badge online"><span class="dot"></span> ' + t('serverOnline') + '</span>';
  const history = getBenchmarkHistory();
  const count = history.length;
  const lastRun = history[history.length - 1];
  const metricCount = Object.keys(lastRun.r || {}).length;
  subtitle.innerHTML = '<span class="status-hover-group">' + badge.innerHTML +
    '<span class="status-hint"> ' + t('runSummary', { count, ts: formatTime(lastRun.ts), metrics: metricCount }) + '</span></span>';
  buildRightSide(connected);
}

// ── Header Right ─────────────────────────────────────────────────

function buildRightSide(connected) {
  const header = document.querySelector('.header');
  let right = header.querySelector('.header-right');
  if (!right) { right = document.createElement('div'); right.className = 'header-right'; header.appendChild(right); }
  right.innerHTML = '';

  // Lang dropdown
  const langDropdown = document.createElement('div');
  langDropdown.className = 'lang-dropdown';
  const langBtn = document.createElement('button');
  langBtn.className = 'lang-btn';
  const currentLang = getLang();
  langBtn.innerHTML = '<span class="lang-icon">🌐</span> ' + LANGUAGES[currentLang].label + ' <span class="lang-arrow">▾</span>';
  langDropdown.appendChild(langBtn);
  const langMenu = document.createElement('div');
  langMenu.className = 'lang-menu';
  Object.entries(LANGUAGES).forEach(([code, info]) => {
    const item = document.createElement('div');
    item.className = 'lang-item' + (code === currentLang ? ' active' : '');
    item.textContent = info.name;
    item.addEventListener('click', () => { if (code !== currentLang) switchLang(code); langMenu.classList.remove('open'); });
    langMenu.appendChild(item);
  });
  langDropdown.appendChild(langMenu);
  langBtn.addEventListener('click', (e) => { e.stopPropagation(); langMenu.classList.toggle('open'); });
  document.addEventListener('click', (e) => { if (!langDropdown.contains(e.target)) langMenu.classList.remove('open'); });
  right.appendChild(langDropdown);

  // Global scale toggle
  const scaleGroup = document.createElement('div');
  scaleGroup.className = 'scale-toggle';
  const initialGlobal = getGlobalScale();
  const linearBtn = document.createElement('button');
  linearBtn.className = 'scale-btn global-scale-btn' + (initialGlobal === 'linear' ? ' active' : '');
  linearBtn.dataset.scale = 'linear';
  linearBtn.textContent = t('scaleLinear');
  linearBtn.addEventListener('mouseenter', () => {
    showTooltip(t('scaleLinearHint'), linearBtn);
    const tip = getTooltip();
    tip.classList.add('below');
    tip.style.top = (linearBtn.getBoundingClientRect().bottom + 8) + 'px';
  });
  linearBtn.addEventListener('mouseleave', hideTooltip);
  linearBtn.addEventListener('click', () => setScale('linear'));
  const logBtn = document.createElement('button');
  logBtn.className = 'scale-btn global-scale-btn' + (initialGlobal === 'log' ? ' active' : '');
  logBtn.dataset.scale = 'log';
  logBtn.textContent = t('scaleLog');
  logBtn.addEventListener('mouseenter', () => {
    showTooltip(t('scaleLogHint'), logBtn);
    const tip = getTooltip();
    tip.classList.add('below');
    tip.style.top = (logBtn.getBoundingClientRect().bottom + 8) + 'px';
  });
  logBtn.addEventListener('mouseleave', hideTooltip);
  logBtn.addEventListener('click', () => setScale('log'));
  scaleGroup.appendChild(linearBtn);
  scaleGroup.appendChild(logBtn);
  right.appendChild(scaleGroup);

  // Reset btn
  if (connected) {
    const btn = document.createElement('button');
    btn.className = 'reset-btn';
    btn.title = t('resetConfirm');
    btn.innerHTML = t('resetBtn');
    btn.addEventListener('click', async () => {
      if (!await showConfirmModal(t('resetConfirm'))) return;
      const count = getBenchmarkHistory().length;
      if (!await showConfirmModal(t('resetConfirm2', { count }))) return;
      btn.disabled = true; btn.textContent = t('resetProgress');
      const result = await resetDatabase();
      if (result.ok) { alert(t('resetSuccess', { n: result.deleted })); location.reload(); }
      else { alert(t('resetFail')); btn.disabled = false; btn.innerHTML = t('resetBtn'); }
    });
    right.appendChild(btn);
  }
}

// ── Modal ────────────────────────────────────────────────────────

function showConfirmModal(message) {
  return new Promise((resolve) => {
    let overlay = document.getElementById('modalOverlay');
    if (!overlay) {
      overlay = document.createElement('div');
      overlay.id = 'modalOverlay'; overlay.className = 'modal-overlay';
      overlay.innerHTML = '<div class="modal-card"><div class="modal-icon">⚠️</div><div class="modal-message"></div><div class="modal-actions"><button class="modal-btn modal-btn-cancel"></button><button class="modal-btn modal-btn-confirm"></button></div></div>';
      document.body.appendChild(overlay);
    }
    const msgEl = overlay.querySelector('.modal-message');
    const cancelBtn = overlay.querySelector('.modal-btn-cancel');
    const confirmBtn = overlay.querySelector('.modal-btn-confirm');
    msgEl.textContent = message;
    cancelBtn.textContent = t('modalCancel');
    confirmBtn.textContent = t('modalConfirm');
    overlay.classList.add('open');
    const cleanup = (result) => { overlay.classList.remove('open'); cancelBtn.onclick = null; confirmBtn.onclick = null; overlay.onclick = null; document.onkeydown = null; resolve(result); };
    cancelBtn.onclick = () => cleanup(false);
    confirmBtn.onclick = () => cleanup(true);
    overlay.onclick = (e) => { if (e.target === overlay) cleanup(false); };
    document.onkeydown = (e) => { if (e.key === 'Escape') cleanup(false); };
  });
}

// ── Empty State ──────────────────────────────────────────────────

export function toggleEmptyState(hasData) {
  const state = document.getElementById('emptyState');
  state.style.display = hasData ? 'none' : 'flex';
  document.getElementById('mainContent').style.display = hasData ? 'block' : 'none';
  if (!hasData && !state.dataset.populated) {
    state.dataset.populated = 'true';
    state.innerHTML = '<div class="empty-icon">🏗️</div><h2>' + t('emptyTitle') + '</h2><p>' + t('emptyDesc', { cmd: '<code>gradlew generateBenchmarkReport</code>' }) + '</p><div class="hint">💡 ' + t('emptyHint', { cmd: '<code>gradlew startBenchmarkServer</code>' }) + '</div>';
  }
}

// ── Stats Bar ────────────────────────────────────────────────────

export function buildStatsBar() {
  const history = getBenchmarkHistory();
  const lastRun = history[history.length - 1];
  const metricCount = Object.keys(lastRun.r || {}).length;
  document.getElementById('statsBar').innerHTML =
    '<div class="stat-card"><div class="stat-icon">📈</div><div class="stat-info"><div class="stat-label">' + t('statsRuns') + '</div><div class="stat-value">' + history.length + '</div></div></div>' +
    '<div class="stat-card"><div class="stat-icon">📐</div><div class="stat-info"><div class="stat-label">' + t('statsMetrics') + '</div><div class="stat-value">' + metricCount + '</div></div></div>' +
    '<div class="stat-card"><div class="stat-icon">🕐</div><div class="stat-info"><div class="stat-label">' + t('statsLatest') + '</div><div class="stat-value small">' + formatTime(lastRun.ts) + '</div></div></div>' +
    '<div class="stat-card"><div class="stat-icon">🗄️</div><div class="stat-info"><div class="stat-label">' + t('statsSource') + '</div><div class="stat-value">SQLite</div></div></div>';
}

// ── Tabs ─────────────────────────────────────────────────────────

export function buildTabs() {
  const history = getBenchmarkHistory();
  const allLabels = new Set();
  history.forEach(r => { if (r.r) Object.keys(r.r).forEach(k => allLabels.add(k)); });
  _currentCategories = groupByCategory(allLabels);

  const tabBar = document.getElementById('tabBar');
  const tabContent = document.getElementById('tabContent');
  tabBar.innerHTML = '';
  tabContent.innerHTML = '';

  const catNames = Object.keys(_currentCategories).sort();
  catNames.forEach((cat, idx) => {
    const tab = document.createElement('div');
    tab.className = 'tab' + (idx === 0 ? ' active' : '');
    tab.textContent = cat;
    tab.addEventListener('click', () => switchTab(cat));
    // Category tooltip
    const catDesc = getCategoryDescription(cat, getLang());
    tab.addEventListener('mouseenter', () => showTooltip(catDesc, tab));
    tab.addEventListener('mouseleave', hideTooltip);
    tabBar.appendChild(tab);

    const content = document.createElement('div');
    content.id = 'tab-' + cat;
    content.className = 'tab-content' + (idx === 0 ? ' active' : '');

    const headerDesc = document.createElement('div');
    headerDesc.className = 'cat-header';
    headerDesc.innerHTML = '<span class="cat-metric-count">' + t('metricCount', { n: _currentCategories[cat].length }) + '</span>';
    content.appendChild(headerDesc);

    const grid = document.createElement('div');
    grid.className = 'metric-grid';
    _currentCategories[cat].forEach((metric, mIdx) => {
      grid.appendChild(createMetricCard(metric, cat, mIdx));
    });
    content.appendChild(grid);
    tabContent.appendChild(content);
  });
}

export function getCategoryNames() {
  return Object.keys(_currentCategories).sort();
}

export function switchTab(name) {
  // 一次性查询并缓存DOM引用，减少重复查询
  const tabs = document.querySelectorAll('.tab');
  const contents = document.querySelectorAll('.tab-content');
  tabs.forEach(t => t.classList.remove('active'));
  const idx = Array.from(tabs).findIndex(t => t.textContent.trim() === name);
  if (idx >= 0) tabs[idx].classList.add('active');
  contents.forEach(t => t.classList.remove('active'));
  const targetContent = document.getElementById('tab-' + name);
  if (targetContent) targetContent.classList.add('active');
  destroyAllCharts();
  setTimeout(() => {
    renderChartsByCategory(name);
    window.dispatchEvent(new Event('resize'));
    // 清理setTimeout引用，防止意外行为
  }, 80);
}

// ── Metric Card Factory ─────────────────────────────────────────

function createMetricCard(metric, cat, colorIdx) {
  const history = getBenchmarkHistory();
  const cardId = cat + '-' + colorIdx;
  const color = COLORS[colorIdx % COLORS.length];
  const title = formatMetricTitle(metric.method);

  const vals = history.map(r => r.r && r.r[metric.label]).filter(v => v !== undefined && v !== null);
  const avg = vals.length > 0 ? vals.reduce((a, b) => a + b, 0) / vals.length : 0;
  const min = vals.length > 0 ? Math.min(...vals) : 0;
  const max = vals.length > 0 ? Math.max(...vals) : 0;
  const latest = vals.length > 0 ? vals[vals.length - 1] : null;

  const card = document.createElement('div');
  card.className = 'metric-card';
  card.id = 'chart-container-' + cardId;
  card.dataset.metric = metric.label;
  card.style.setProperty('--card-accent', color);

  const header = document.createElement('div');
  header.className = 'metric-card-header';

  const titleRow = document.createElement('div');
  titleRow.className = 'metric-card-title-row';
  const icon = document.createElement('span');
  icon.className = 'metric-icon';
  icon.style.background = color + '22';
  icon.style.color = color;
  icon.textContent = getMetricEmoji(metric.method);

  const nameGroup = document.createElement('div');
  nameGroup.className = 'metric-name-group';
  const nameEl = document.createElement('div');
  nameEl.className = 'metric-name';
  nameEl.textContent = title;
  nameGroup.appendChild(nameEl);
  if (metric.params) {
    const paramEl = document.createElement('div');
    paramEl.className = 'metric-params';
    paramEl.textContent = metric.params;
    nameGroup.appendChild(paramEl);
  }
  titleRow.appendChild(icon);
  titleRow.appendChild(nameGroup);

  // Show/hide tooltip on hover over the title row
  const desc = getMetricDescription(metric.method, getLang(), cat);
  titleRow.addEventListener('mouseenter', () => {
    showTooltip(desc, titleRow);
  });
  titleRow.addEventListener('mouseleave', hideTooltip);

  header.appendChild(titleRow);

  // Stats row
  const statsRow = document.createElement('div');
  statsRow.className = 'metric-stats';
  statsRow.innerHTML =
    '<div class="metric-stat"><span class="metric-stat-label">' + t('statLatest') + '</span><span class="metric-stat-value">' + (latest !== null ? formatMetricValue(latest) : '—') + '</span></div>' +
    '<div class="metric-stat"><span class="metric-stat-label">' + t('statAvg') + '</span><span class="metric-stat-value">' + formatMetricValue(avg) + '</span></div>' +
    '<div class="metric-stat"><span class="metric-stat-label">' + t('statMin') + '</span><span class="metric-stat-value">' + formatMetricValue(min) + '</span></div>' +
    '<div class="metric-stat"><span class="metric-stat-label">' + t('statMax') + '</span><span class="metric-stat-value">' + formatMetricValue(max) + '</span></div>';
  header.appendChild(statsRow);
  card.appendChild(header);

  // Canvas
  const wrap = document.createElement('div');
  wrap.className = 'metric-chart-wrap';
  const canvas = document.createElement('canvas');
  canvas.id = 'chart-' + cardId;
  wrap.appendChild(canvas);
  card.appendChild(wrap);
  return card;
}

function getMetricEmoji(method) {
  const lower = method.toLowerCase();
  if (lower.includes('tps') || lower.includes('tick')) return '⚡';
  if (lower.includes('mspt') || lower.includes('milli')) return '⏱';
  if (lower.includes('cpu') || lower.includes('process')) return '🖥';
  if (lower.includes('memory') || lower.includes('heap') || lower.includes('mem')) return '🧠';
  if (lower.includes('disk') || lower.includes('io')) return '💾';
  if (lower.includes('gc') || lower.includes('garbage')) return '♻';
  if (lower.includes('add') || lower.includes('put') || lower.includes('insert')) return '➕';
  if (lower.includes('remove') || lower.includes('delete') || lower.includes('clear')) return '➖';
  if (lower.includes('get') || lower.includes('find') || lower.includes('query')) return '🔍';
  if (lower.includes('has') || lower.includes('contain') || lower.includes('exist')) return '✅';
  if (lower.includes('link') || lower.includes('connect')) return '🔗';
  if (lower.includes('mount') || lower.includes('unmount')) return '📦';
  if (lower.includes('page') || lower.includes('scroll')) return '📄';
  if (lower.includes('shape') || lower.includes('block')) return '🧱';
  if (lower.includes('cache') || lower.includes('buffer')) return '🗃';
  if (lower.includes('ultimine') || lower.includes('mine')) return '⛏';
  if (lower.includes('context') || lower.includes('aggregate')) return '🔲';
  return '📊';
}

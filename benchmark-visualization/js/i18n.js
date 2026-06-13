/**
 * Benchmark Visualization - Internationalization (i18n)
 *
 * Lightweight i18n system with zh-CN and en support.
 * Language preference is persisted to localStorage.
 * Parts of the UI re-render on language switch via page reload.
 */

import { formatTime } from './utils.js';

// ── Constants ────────────────────────────────────────────────────

const STORAGE_KEY = 'benchmark-lang';

/** Available languages with display labels. */
export const LANGUAGES = {
  'zh-CN': { label: '中', name: '简体中文' },
  en: { label: 'EN', name: 'English' },
};

/** Default fallback language. */
const DEFAULT_LANG = 'zh-CN';

// ── Translation Table ────────────────────────────────────────────

const LOCALE = {
  'zh-CN': {
    // Page title
    pageTitle: 'RTS Building Benchmark 性能趋势',

    // Server status
    serverOnline: '数据服务器在线',
    serverOffline: '未连接服务器',
    serverEmpty: '数据库为空',

    // Header
    loading: '加载中...',
    emptyDb: '数据库为空',
    startServer: '请启动数据服务器',

    // Stats bar
    statsRuns: '运行次数',
    statsMetrics: '指标/次',
    statsLatest: '最新运行',
    statsSource: '数据源',

    // Chart
    nsPerOp: 'ns/op',
    scaleLinear: '线性',
    scaleLog: '对数',
    scaleLinearHint: '等距刻度，适合观察数据的实际数值差异',
    scaleLogHint: '对数刻度，适合数值跨度大时观察变化趋势',
    chartFast: '快速操作 (&lt; 10K ns/op)',
    chartSlow: '慢速操作 (对数尺度)',

    // Empty state
    emptyTitle: '还没有 benchmark 数据',
    emptyDesc: '请先运行 {cmd} 跑一次测试，将数据写入 SQLite',
    emptyHint: '启动数据服务器: {cmd}，然后刷新本页面',
    emptyHeaderHint: '请运行 {cmd1} 跑性能测试，然后执行 {cmd2} 启动数据服务器',

    // Reset
    resetBtn: '🗑️ 一键删库',
    resetProgress: '⌛ 删库中...',
    resetConfirm: '确定要清空所有 benchmark 数据吗？\n\n此操作不可恢复！',
    resetConfirm2: '⚠️ 最后确认：数据库中共有 {count} 条运行记录\n\n删除后将永久丢失，真的要继续吗？',
    resetSuccess: '✅ 已删除 {n} 条 run\n页面将自动刷新',
    resetFail: '❌ 删库失败，请确认数据服务器正在运行',

    // Modal
    modalCancel: '取消',
    modalConfirm: '确认删除',

    // Metric card stats
    statLatest: '最新',
    statAvg: '平均',
    statMin: '最小',
    statMax: '最大',
    metricCount: '共 {n} 项指标',

    // Misc
    latestBadge: '● 最新',
    runSummary: '共 {count} 次运行 · 最新: {ts} · 每次 {metrics} 项指标',
  },

  en: {
    // Page title
    pageTitle: 'RTS Building Benchmark Performance Trends',
    serverOnline: 'Data server online',
    serverOffline: 'Not connected',
    serverEmpty: 'Database empty',

    loading: 'Loading...',
    emptyDb: 'Database is empty',
    startServer: 'Start the data server',

    statsRuns: 'Runs',
    statsMetrics: 'Metrics/run',
    statsLatest: 'Latest run',
    statsSource: 'Source',

    nsPerOp: 'ns/op',
    scaleLinear: 'Linear',
    scaleLog: 'Log',
    scaleLinearHint: 'Equal intervals. Best for comparing actual value differences',
    scaleLogHint: 'Logarithmic scale. Best for trends when values vary widely',
    chartFast: 'Fast ops (&lt; 10K ns/op)',
    chartSlow: 'Slow ops (log scale)',

    emptyTitle: 'No benchmark data yet',
    emptyDesc: 'Run {cmd} to write data to the SQLite database',
    emptyHint: 'Start the data server: {cmd}, then refresh this page',
    emptyHeaderHint: 'Run {cmd1} for benchmarks, then start {cmd2} to serve data',

    resetBtn: '🗑️ Reset DB',
    resetProgress: '⌛ Resetting...',
    resetConfirm: 'Delete ALL benchmark data?\n\nThis cannot be undone!',
    resetConfirm2: '⚠️ Final confirmation: {count} run(s) in the database\n\nThis will permanently delete everything. Proceed?',
    resetSuccess: '✅ Deleted {n} run(s)\nPage will auto-refresh',
    resetFail: '❌ Reset failed, is the data server running?',

    // Modal
    modalCancel: 'Cancel',
    modalConfirm: 'Delete',

    // Metric card stats
    statLatest: 'Latest',
    statAvg: 'Avg',
    statMin: 'Min',
    statMax: 'Max',
    metricCount: '{n} metric(s)',

    // Misc
    latestBadge: '● Latest',
    runSummary: '{count} run(s) · Latest: {ts} · {metrics} metric(s) per run',
  },
};

// ── State ────────────────────────────────────────────────────────

/** @type {'zh-CN'|'en'} */
let _currentLang = loadSavedLang();

// ── Internal helpers ─────────────────────────────────────────────

/**
 * Load saved language preference from localStorage.
 * @returns {'zh-CN'|'en'}
 */
function loadSavedLang() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved && LOCALE[saved]) return saved;
  } catch {
    // localStorage may be unavailable
  }
  return DEFAULT_LANG;
}

/**
 * Format a template string by replacing {key} placeholders.
 *
 * @param {string} template - string with {key} placeholders
 * @param {Object<string, string|number>} values - replacement values
 * @returns {string}
 */
function formatTemplate(template, values) {
  let result = template;
  for (const [key, val] of Object.entries(values)) {
    result = result.replace(new RegExp(`\\{${key}\\}`, 'g'), String(val));
  }
  return result;
}

// ── Public API ───────────────────────────────────────────────────

/**
 * Get the translation for a given key, with optional template substitution.
 *
 * @param {string} key - translation key
 * @param {Object<string, string|number>} [values] - template values
 * @returns {string}
 */
export function t(key, values) {
  let str = LOCALE[_currentLang][key];
  if (str === undefined) {
    // Fall back to default language
    str = LOCALE[DEFAULT_LANG][key];
  }
  if (str === undefined) {
    console.warn('[i18n] Missing translation key:', key);
    return key;
  }
  if (values) {
    str = formatTemplate(str, values);
  }
  return str;
}

/**
 * Get the current language code.
 * @returns {'zh-CN'|'en'}
 */
export function getLang() {
  return _currentLang;
}

/**
 * Get the display label for the current language (e.g. "中" or "EN").
 * @returns {string}
 */
export function getLangLabel() {
  return LANGUAGES[_currentLang].label;
}

/**
 * Switch language, persist, and reload the page.
 *
 * @param {'zh-CN'|'en'} lang - target language code
 */
export function switchLang(lang) {
  if (!LOCALE[lang]) return;
  _currentLang = lang;
  try {
    localStorage.setItem(STORAGE_KEY, lang);
  } catch {
    // ignore
  }
  location.reload();
}

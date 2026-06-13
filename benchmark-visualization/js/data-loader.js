/**
 * Benchmark Visualization - Data Loader
 *
 * Handles communication with the benchmark data server.
 * Maintains the in-memory benchmark history as a shared state.
 */

import { SERVER_URL } from './constants.js';

/**
 * In-memory benchmark history.
 * Populated by loadBenchmarkData() and used across all modules.
 * @type {Array<{ ts: string, r: Object<string, number> }>}
 */
let _history = [];

/**
 * Get a snapshot of the current benchmark history.
 * @returns {Array<{ ts: string, r: Object<string, number> }>}
 */
export function getBenchmarkHistory() {
  return _history;
}

/**
 * Fetch benchmark data from the local data server.
 * First pings the server, then fetches the full dataset.
 *
 * @returns {Promise<'online'|'offline'|'empty'>}
 *   online  — connected and has data
 *   offline — server unreachable
 *   empty   — connected but no data yet
 */
export async function loadBenchmarkData() {
  try {
    const pingResp = await fetch(SERVER_URL + '/api/ping', {
      mode: 'cors',
      signal: AbortSignal.timeout(500),
    });
    if (!pingResp.ok) {
      throw new Error('Server ping failed with status ' + pingResp.status);
    }

    const dataResp = await fetch(SERVER_URL + '/api/data', { mode: 'cors' });
    if (!dataResp.ok) {
      throw new Error('Data fetch failed with status ' + dataResp.status);
    }

    const text = await dataResp.text();
    // 检查响应是否为空，避免JSON.parse空字符串报错
    if (!text.trim()) {
      _history = [];
      return 'empty';
    }
    const json = JSON.parse(text);
    _history = Array.isArray(json) ? json : [];
    console.log('[Benchmark] Loaded ' + _history.length + ' run(s) from server');
    return _history.length > 0 ? 'online' : 'empty';
  } catch (e) {
    console.log('[Benchmark] Server unreachable (' + e.message + ')');
    _history = [];
    return 'offline';
  }
}

/**
 * Send a reset command to the data server to delete ALL benchmark data.
 *
 * On success, clears the local in-memory history automatically.
 *
 * @returns {Promise<{ok: boolean, deleted: number}>}
 */
export async function resetDatabase() {
  try {
    const resp = await fetch(SERVER_URL + '/api/reset', {
      method: 'POST',
      mode: 'cors',
    });
    if (!resp.ok) {
      throw new Error('Reset failed with status ' + resp.status);
    }
    const json = await resp.json();
    _history = [];
    console.log('[Benchmark] 删库完成，删除了 ' + json.deleted + ' 条 run');
    return { ok: true, deleted: json.deleted };
  } catch (e) {
    console.log('[Benchmark] Reset failed (' + e.message + ')');
    return { ok: false, deleted: 0 };
  }
}

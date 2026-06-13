/**
 * Benchmark Visualization - Metric & Category Descriptions
 *
 * Maps benchmark method names to human-readable descriptions explaining
 * what each metric measures and which part of the game it relates to.
 *
 * Keys are matched case-insensitively against the raw method name.
 */

// ── Metric Descriptions (ZH) ─────────────────────────────────────

const DESCRIPTIONS_ZH = {

  // == RtsAggregateStorage ==
  'RtsAggregateStorage.mountSequentialSamePriority': '将大量物品栏添加到仓库，测试添加速度',
  'RtsAggregateStorage.mountRandomPriorities': '将多个物品栏按不同优先级添加到仓库',
  'RtsAggregateStorage.unmountAllSamePriority': '从仓库中批量移除物品栏',
  'RtsAggregateStorage.unmountDifferentPriorities': '移除各种优先级的物品栏',
  'RtsAggregateStorage.hasItemLarge': '在大仓库里查找某个物品是否存在',
  'RtsAggregateStorage.hasItemSmall': '在小仓库里查找某个物品是否存在',
  'RtsAggregateStorage.getTotalCountLarge': '查询大仓库里某个物品总共有多少个',
  'RtsAggregateStorage.getAvailableItemsLarge': '列出大仓库里所有可用的物品',
  'RtsAggregateStorage.tickUpdateLarge': '大仓库每 tick 更新数据，处理物品变化',
  'RtsAggregateStorage.isEmpty': '检查仓库是不是空的',
  'RtsAggregateStorage.drainPendingChanges': '取出并清空仓库中待处理的变更',
  'RtsAggregateStorage.tickUpdateEmpty': '空仓库每 tick 更新的开销',

  // == RtsHandlerCache ==
  'RtsHandlerCache.getAvailableItemsLarge': '从大缓存里取出所有可用的物品',
  'RtsHandlerCache.getAvailableItemsSmall': '从小缓存里取出所有可用的物品',
  'RtsHandlerCache.getCountLookup': '根据物品名字查询缓存中的数量',
  'RtsHandlerCache.getCountNull': '传入空名字时查询缓存的性能',
  'RtsHandlerCache.getCountByItem': '根据物品对象查询缓存中的数量',
  'RtsHandlerCache.invalidate': '清空有数据的缓存，让缓存失效',
  'RtsHandlerCache.invalidateEmpty': '清空一个空缓存的性能',
  'RtsHandlerCache.release': '释放缓存占用的资源',
  'RtsHandlerCache.getCachedSlotCount': '获取缓存中记录的槽位数量',
  'RtsHandlerCache.isDirty': '检查缓存是否有未保存的变更',
  'RtsHandlerCache.clearDirty': '清除缓存中的未保存标记',

  // == TemporaryContextSwitcher ==
  'TemporaryContextSwitcher.parseValid': '解析一个有效的射线数据，测试正常情况',
  'TemporaryContextSwitcher.parseNaN': '传入非法数字时的处理速度',
  'TemporaryContextSwitcher.parseZeroDir': '传入零方向时的快速处理',
  'TemporaryContextSwitcher.parseInfinity': '传入无穷大数字时的快速处理',
  'TemporaryContextSwitcher.parseAxisAligned': '解析上下左右等常见方向的射线',
  'TemporaryContextSwitcher.parseMixed': '混合正常和异常输入时的处理性能',

  // == HistoryEntry ==
  'HistoryEntry.constructSingle': '创建一条包含 1 个方块的历史记录',
  'HistoryEntry.construct100': '创建一条包含 100 个方块的历史记录',
  'HistoryEntry.construct10000': '创建一条包含 10000 个方块的历史记录',
  'HistoryEntry.removeRestoredPartial': '从历史记录中移除部分已恢复的方块',
  'HistoryEntry.removeRestoredAll': '从历史记录中移除所有已恢复的方块',
  'HistoryEntry.getBlockCount': '查询历史记录里有多少个方块',
  'HistoryEntry.isExpired': '检查历史记录是否已经过期',

  // == RtsLinkedStorageResolver ==
  'RtsLinkedStorageResolver.buildLinkedSummarySingle': '生成 1 个链接仓库的摘要信息',
  'RtsLinkedStorageResolver.buildLinkedSummaryTenRefs': '生成 10 个链接仓库的摘要信息',
  'RtsLinkedStorageResolver.buildLinkedSummaryThousandRefs': '生成 1000 个链接仓库的摘要信息',
  'RtsLinkedStorageResolver.buildLinkedSummaryTenThousandRefs': '生成 10000 个链接仓库的摘要信息',
  'RtsLinkedStorageResolver.buildLinkedSummaryEmpty': '生成空链接仓库的摘要信息',
  'RtsLinkedStorageResolver.sanitizeLinkMode': '检查链接模式是否合法',
  'RtsLinkedStorageResolver.sanitizePriorityBulk': '批量检查仓库优先级是否在范围内',
  'RtsLinkedStorageResolver.sanitizePriorityEdgeCases': '检查仓库优先级边界值',
  'RtsLinkedStorageResolver.isExtractOnlyLinkBulk': '批量判断链接仓库是否只能取出物品',
  'RtsLinkedStorageResolver.isExtractOnlyLinkNull': '空参数下判断链接仓库模式',

  // == RtsPageCache ==
  'RtsPageCache.lruEvictionAtMaxCapacity': '缓存满了时淘汰旧数据的性能',
  'RtsPageCache.sequentialGet': '从满缓存中按顺序读取数据',
  'RtsPageCache.randomUuidMiss': '随机查询不存在的缓存数据',
  'RtsPageCache.largePagePut': '将大页面数据存入缓存',
  'RtsPageCache.largePageGet': '从缓存中读取大页面数据',
  'RtsPageCache.mixedWorkload': '混合读写删除操作的性能',
  'RtsPageCache.removeUnderFullCache': '缓存满时批量删除数据的性能',
  'RtsPageCache.clear': '清空整个缓存的性能',

  // == RtsStorageRecentEntries ==
  'RtsStorageRecentEntries.pushSameItem': '反复添加同一种物品到最近记录',
  'RtsStorageRecentEntries.pushAlwaysNew': '不断添加新物品到最近记录',
  'RtsStorageRecentEntries.pushMixedItems': '混合添加各种物品到最近记录',
  'RtsStorageRecentEntries.pushFluidEntries': '添加流体类物品到最近记录',
  'RtsStorageRecentEntries.pushMixedKinds': '同一物品以不同类型添加到最近记录',
  'RtsStorageRecentEntries.bulkFillToCapacity': '将最近记录从空填满到上限',
  'RtsStorageRecentEntries.nullGuard': '传入空值或零数量时的快速处理',
  'RtsStorageRecentEntries.clearEntries': '清空最近记录列表',

  // == ShapeGenerator ==
  'ShapeGenerator.boxSmallFill': '生成小型实心长方体（8x8）的位置',
  'ShapeGenerator.boxMediumFill': '生成中型实心长方体（32x32）的位置',
  'ShapeGenerator.boxLargeFill': '生成大型实心长方体（64x64）的位置',
  'ShapeGenerator.boxMediumHollow': '生成中型空心长方体（32x32 外壳）的位置',
  'ShapeGenerator.squareMediumFill': '生成中型实心正方形（32x32）的位置',
  'ShapeGenerator.squareLargeHollow': '生成大型空心正方形（64x64 边框）的位置',
  'ShapeGenerator.circleSmallFill': '生成小型实心圆（半径 8）的位置',
  'ShapeGenerator.circleLargeFill': '生成大型实心圆（半径 32）的位置',
  'ShapeGenerator.circleHollow': '生成空心圆环（半径 32）的位置',
  'ShapeGenerator.wallMediumFill': '生成中型实心墙壁（16x16）的位置',
  'ShapeGenerator.wallLargeHollow': '生成大型空心墙壁（32x32 框架）的位置',
  'ShapeGenerator.lineLong': '生成长直线（64 格）的位置',

  // == RtsUltimineCollector ==
  'RtsUltimineCollector.collectSmall': '连锁挖掘小范围方块，最多 8 个',
  'RtsUltimineCollector.collectMedium': '连锁挖掘中范围方块，最多 64 个',
  'RtsUltimineCollector.collectLarge': '连锁挖掘大范围方块，最多 256 个',
  'RtsUltimineCollector.collectBoundedRadius': '在 8 格半径内连锁挖掘方块',
  'RtsUltimineCollector.collectTightRadius': '在 3 格紧密半径内连锁挖掘',
  'RtsUltimineCollector.collectSingle': '只挖掘第一个方块（最少数量）',
  'RtsUltimineCollector.collectRejectAll': '所有方块都不符合条件时的性能',
  'RtsUltimineCollector.collectNullLevel': '传入空世界时的快速处理',
  'RtsUltimineCollector.collectNullSeed': '传入空起始位置时的快速处理',

  // == RtsCountUtil ==
  'RtsCountUtil.sanitizeCount': '将物品数量修正为非负数',
  'RtsCountUtil.saturatedAddNormal': '两个正数相加',
  'RtsCountUtil.saturatedAddOverflow': '超大数字相加时防止溢出',
  'RtsCountUtil.saturatedAddNegative': '负数相加时自动修正为 0',
  'RtsCountUtil.mergeCountNewKeys': '将新物品的数量合并到统计表中',
  'RtsCountUtil.mergeCountSameKey': '往已有物品的数量上累加',
  'RtsCountUtil.mergeCountGuards': '处理空值或零数量的快速判断',
};

// ── Metric Descriptions (EN) ─────────────────────────────────────

const DESCRIPTIONS_EN = {

  // == RtsAggregateStorage ==
  'RtsAggregateStorage.mountSequentialSamePriority': 'Adds many item handlers to the warehouse. Tests add speed.',
  'RtsAggregateStorage.mountRandomPriorities': 'Adds handlers with different priorities to the warehouse.',
  'RtsAggregateStorage.unmountAllSamePriority': 'Removes a batch of handlers from the warehouse.',
  'RtsAggregateStorage.unmountDifferentPriorities': 'Removes handlers with various priorities.',
  'RtsAggregateStorage.hasItemLarge': 'Checks if an item exists in a large warehouse.',
  'RtsAggregateStorage.hasItemSmall': 'Checks if an item exists in a small warehouse.',
  'RtsAggregateStorage.getTotalCountLarge': 'Counts how many of an item are in a large warehouse.',
  'RtsAggregateStorage.getAvailableItemsLarge': 'Lists all available items in a large warehouse.',
  'RtsAggregateStorage.tickUpdateLarge': 'Large warehouse game tick update, processing item changes.',
  'RtsAggregateStorage.isEmpty': 'Checks if the warehouse is empty.',
  'RtsAggregateStorage.drainPendingChanges': 'Retrieves and clears all pending changes.',
  'RtsAggregateStorage.tickUpdateEmpty': 'Tick update cost for an empty warehouse.',

  // == RtsHandlerCache ==
  'RtsHandlerCache.getAvailableItemsLarge': 'Lists all items in a large cache.',
  'RtsHandlerCache.getAvailableItemsSmall': 'Lists all items in a small cache.',
  'RtsHandlerCache.getCountLookup': 'Looks up item count by name in the cache.',
  'RtsHandlerCache.getCountNull': 'Cache lookup with an empty name.',
  'RtsHandlerCache.getCountByItem': 'Looks up item count by item object.',
  'RtsHandlerCache.invalidate': 'Clears a populated cache.',
  'RtsHandlerCache.invalidateEmpty': 'Clears an empty cache.',
  'RtsHandlerCache.release': 'Frees cache resources.',
  'RtsHandlerCache.getCachedSlotCount': 'Gets the cached slot count.',
  'RtsHandlerCache.isDirty': 'Checks if the cache has unsaved changes.',
  'RtsHandlerCache.clearDirty': 'Clears the unsaved changes flag.',

  // == TemporaryContextSwitcher ==
  'TemporaryContextSwitcher.parseValid': 'Parses a valid ray. Normal case performance.',
  'TemporaryContextSwitcher.parseNaN': 'Handles invalid number input quickly.',
  'TemporaryContextSwitcher.parseZeroDir': 'Handles zero-direction input quickly.',
  'TemporaryContextSwitcher.parseInfinity': 'Handles infinity number input quickly.',
  'TemporaryContextSwitcher.parseAxisAligned': 'Parses common directions like up/down/left/right.',
  'TemporaryContextSwitcher.parseMixed': 'Handles a mix of valid and invalid inputs.',

  // == HistoryEntry ==
  'HistoryEntry.constructSingle': 'Creates a history entry with 1 block record.',
  'HistoryEntry.construct100': 'Creates a history entry with 100 block records.',
  'HistoryEntry.construct10000': 'Creates a history entry with 10000 block records.',
  'HistoryEntry.removeRestoredPartial': 'Removes some restored blocks from the history.',
  'HistoryEntry.removeRestoredAll': 'Removes all restored blocks from the history.',
  'HistoryEntry.getBlockCount': 'Gets how many blocks are in the history.',
  'HistoryEntry.isExpired': 'Checks if the history entry has expired.',

  // == RtsLinkedStorageResolver ==
  'RtsLinkedStorageResolver.buildLinkedSummarySingle': 'Builds summary for 1 linked warehouse.',
  'RtsLinkedStorageResolver.buildLinkedSummaryTenRefs': 'Builds summary for 10 linked warehouses.',
  'RtsLinkedStorageResolver.buildLinkedSummaryThousandRefs': 'Builds summary for 1000 linked warehouses.',
  'RtsLinkedStorageResolver.buildLinkedSummaryTenThousandRefs': 'Builds summary for 10000 linked warehouses.',
  'RtsLinkedStorageResolver.buildLinkedSummaryEmpty': 'Builds summary for empty linked warehouses.',
  'RtsLinkedStorageResolver.sanitizeLinkMode': 'Checks if the link mode is valid.',
  'RtsLinkedStorageResolver.sanitizePriorityBulk': 'Checks warehouse priority values in bulk.',
  'RtsLinkedStorageResolver.sanitizePriorityEdgeCases': 'Checks edge-case priority values.',
  'RtsLinkedStorageResolver.isExtractOnlyLinkBulk': 'Checks if linked warehouses are extract-only.',
  'RtsLinkedStorageResolver.isExtractOnlyLinkNull': 'Checks link mode with empty parameters.',

  // == RtsPageCache ==
  'RtsPageCache.lruEvictionAtMaxCapacity': 'Removes old data when cache is full.',
  'RtsPageCache.sequentialGet': 'Reads data sequentially from a full cache.',
  'RtsPageCache.randomUuidMiss': 'Looks up random non-existent cache data.',
  'RtsPageCache.largePagePut': 'Stores a large page into the cache.',
  'RtsPageCache.largePageGet': 'Reads a large page from the cache.',
  'RtsPageCache.mixedWorkload': 'Mixed read/write/delete operations.',
  'RtsPageCache.removeUnderFullCache': 'Batch deletes data from a full cache.',
  'RtsPageCache.clear': 'Clears the entire cache.',

  // == RtsStorageRecentEntries ==
  'RtsStorageRecentEntries.pushSameItem': 'Repeatedly adds the same item to recent entries.',
  'RtsStorageRecentEntries.pushAlwaysNew': 'Keeps adding new items to recent entries.',
  'RtsStorageRecentEntries.pushMixedItems': 'Adds various items to recent entries.',
  'RtsStorageRecentEntries.pushFluidEntries': 'Adds fluid-type items to recent entries.',
  'RtsStorageRecentEntries.pushMixedKinds': 'Adds the same item as different types.',
  'RtsStorageRecentEntries.bulkFillToCapacity': 'Fills recent entries from empty to max.',
  'RtsStorageRecentEntries.nullGuard': 'Handles empty or zero-count input quickly.',
  'RtsStorageRecentEntries.clearEntries': 'Clears the recent entries list.',

  // == ShapeGenerator ==
  'ShapeGenerator.boxSmallFill': 'Generates a small solid box (8x8) shape.',
  'ShapeGenerator.boxMediumFill': 'Generates a medium solid box (32x32) shape.',
  'ShapeGenerator.boxLargeFill': 'Generates a large solid box (64x64) shape.',
  'ShapeGenerator.boxMediumHollow': 'Generates a medium hollow box shell (32x32).',
  'ShapeGenerator.squareMediumFill': 'Generates a medium solid square (32x32).',
  'ShapeGenerator.squareLargeHollow': 'Generates a large hollow square border (64x64).',
  'ShapeGenerator.circleSmallFill': 'Generates a small solid circle (radius 8).',
  'ShapeGenerator.circleLargeFill': 'Generates a large solid circle (radius 32).',
  'ShapeGenerator.circleHollow': 'Generates a hollow circle ring (radius 32).',
  'ShapeGenerator.wallMediumFill': 'Generates a medium solid wall (16x16).',
  'ShapeGenerator.wallLargeHollow': 'Generates a large hollow wall frame (32x32).',
  'ShapeGenerator.lineLong': 'Generates a long straight line (64 blocks).',

  // == RtsUltimineCollector ==
  'RtsUltimineCollector.collectSmall': 'Chain-mines a small area, up to 8 blocks.',
  'RtsUltimineCollector.collectMedium': 'Chain-mines a medium area, up to 64 blocks.',
  'RtsUltimineCollector.collectLarge': 'Chain-mines a large area, up to 256 blocks.',
  'RtsUltimineCollector.collectBoundedRadius': 'Chain-mines within an 8-block radius.',
  'RtsUltimineCollector.collectTightRadius': 'Chain-mines within a tight 3-block radius.',
  'RtsUltimineCollector.collectSingle': 'Mines only the first block (minimum amount).',
  'RtsUltimineCollector.collectRejectAll': 'Performance when no blocks match the filter.',
  'RtsUltimineCollector.collectNullLevel': 'Handles a null world quickly.',
  'RtsUltimineCollector.collectNullSeed': 'Handles a null start position quickly.',

  // == RtsCountUtil ==
  'RtsCountUtil.sanitizeCount': 'Fixes item count to be non-negative.',
  'RtsCountUtil.saturatedAddNormal': 'Adds two positive numbers.',
  'RtsCountUtil.saturatedAddOverflow': 'Prevents overflow when adding very large numbers.',
  'RtsCountUtil.saturatedAddNegative': 'Auto-corrects negative numbers to 0 when adding.',
  'RtsCountUtil.mergeCountNewKeys': 'Adds new item counts to the statistics table.',
  'RtsCountUtil.mergeCountSameKey': 'Adds more count to an existing item.',
  'RtsCountUtil.mergeCountGuards': 'Quick handling of null or zero-count inputs.',
};

// ── Category Descriptions (brief) ────────────────────────────────

const CATEGORY_DESC_ZH = {
  RtsAggregateStorage: '仓库系统：管理物品的添加、移除和查询',
  RtsHandlerCache: '缓存系统：临时存放物品数量信息，加快查询速度',
  TemporaryContextSwitcher: '射线检测：处理游戏中的视线和碰撞检测',
  HistoryEntry: '历史记录：记录建筑操作过的方块，支持撤销恢复',
  RtsLinkedStorageResolver: '链接仓库：管理多个仓库之间的连接关系',
  RtsPageCache: '页面缓存：缓存物品列表页面，加快打开仓库界面速度',
  RtsStorageRecentEntries: '最近物品：记录最近操作过的物品',
  ShapeGenerator: '形状生成：计算各种建筑形状（方体、圆、墙等）的方块位置',
  RtsUltimineCollector: '连锁挖掘：一键挖掘相连的多个方块',
  RtsCountUtil: '计数工具：处理物品数量的加减和统计',
};

const CATEGORY_DESC_EN = {
  RtsAggregateStorage: 'Warehouse: manage item adding, removing and querying',
  RtsHandlerCache: 'Cache: temporarily store item counts for fast lookup',
  TemporaryContextSwitcher: 'Ray tracing: handles line-of-sight and collision detection',
  HistoryEntry: 'History: records block operations for undo/redo',
  RtsLinkedStorageResolver: 'Linked storage: manages connections between warehouses',
  RtsPageCache: 'Page cache: caches inventory pages for faster UI opening',
  RtsStorageRecentEntries: 'Recent items: tracks recently used items',
  ShapeGenerator: 'Shape gen: calculates block positions for building shapes',
  RtsUltimineCollector: 'Chain mining: mine multiple connected blocks at once',
  RtsCountUtil: 'Count utils: handles item count addition and statistics',
};

/**
 * Get the description for a metric method name.
 * Matches by "category.method" composite key (case-insensitive).
 *
 * @param {string} method - the raw method name (e.g. "mountSequentialSamePriority")
 * @param {'zh-CN'|'en'} lang - language code
 * @param {string} [category] - the category name (e.g. "RtsAggregateStorage")
 * @returns {string}
 */
export function getMetricDescription(method, lang, category) {
  const dict = lang === 'en' ? DESCRIPTIONS_EN : DESCRIPTIONS_ZH;

  // Try composite key "category.method" first
  if (category) {
    const compositeKey = (category + '.' + method).toLowerCase();
    const match = Object.keys(dict).find(k => k.toLowerCase() === compositeKey);
    if (match) return dict[match];
  }

  // Fallback: try method-only match (backward compat)
  const lower = method.toLowerCase();
  const exactKey = Object.keys(dict).find(k => k.toLowerCase() === lower);
  if (exactKey) return dict[exactKey];

  const fallbackZh = '负责游戏中的 ' + method + ' 相关操作。';
  const fallbackEn = 'Handles ' + method + '-related operations.';
  return lang === 'en' ? fallbackEn : fallbackZh;
}

/**
 * Get a brief description for a benchmark category (tab name).
 * Matches by category name (case-insensitive).
 *
 * @param {string} category - the category name (e.g. "RtsHandlerCache", "ShapeGenerator")
 * @param {'zh-CN'|'en'} lang - language code
 * @returns {string}
 */
export function getCategoryDescription(category, lang) {
  const dict = lang === 'en' ? CATEGORY_DESC_EN : CATEGORY_DESC_ZH;
  // Try exact match first
  const matchKey = Object.keys(dict).find(k => k.toLowerCase() === category.toLowerCase());
  if (matchKey) return dict[matchKey];
  // Fallback: check if category name contains known keywords
  const lower = category.toLowerCase();
  for (const [key, desc] of Object.entries(dict)) {
    if (lower.includes(key.toLowerCase())) return desc;
  }
  const fallbackZh = '负责游戏 ' + category + ' 相关模块的性能测试。';
  const fallbackEn = 'Benchmark tests for the ' + category + ' module.';
  return lang === 'en' ? fallbackEn : fallbackZh;
}

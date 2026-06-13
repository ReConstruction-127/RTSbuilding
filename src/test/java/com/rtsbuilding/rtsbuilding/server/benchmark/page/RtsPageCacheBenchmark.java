package com.rtsbuilding.rtsbuilding.server.benchmark.page;

import com.rtsbuilding.rtsbuilding.network.storage.RtsStorageSort;
import com.rtsbuilding.rtsbuilding.server.service.page.RtsPageCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Extreme Performance Benchmarks for {@link RtsPageCache}.
 *
 * <p>Focuses on LRU eviction overhead, hash-table lookup degradation at
 * max capacity, large {@link RtsPageCache.CachedPage} data handling, and
 * mixed workloads. All benchmarks avoid {@code ItemStack} to run in a plain
 * unit-test environment without a bootstrapped Minecraft runtime.</p>
 */
class RtsPageCacheBenchmark {

    private static final int WARMUP = 3;
    private static final int ITERATIONS = 10;
    private static final int MAX_ENTRIES = 256;

    private RtsPageCache cache;

    /**
     * Global JIT warmup — runs once before all tests. Exercises all code paths
     * so the JIT compiler compiles them before measurement begins.
     */
    @BeforeAll
    static void globalWarmUp() {
        RtsPageCache c = new RtsPageCache();

        // Warm up LRU eviction path (moderate, avoid excessive memory)
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < MAX_ENTRIES + 500; i++) {
                c.put(UUID.randomUUID(), createEmptyPage(i));
            }
            c.clear();
        }

        // Warm up get path with a full cache
        UUID[] keys = new UUID[MAX_ENTRIES];
        for (int i = 0; i < MAX_ENTRIES; i++) {
            keys[i] = UUID.randomUUID();
            c.put(keys[i], createEmptyPage(i));
        }
        for (int i = 0; i < 3_000; i++) {
            c.get(keys[i % MAX_ENTRIES]);
            c.get(UUID.randomUUID()); // miss path
        }

        // Warm up remove path
        c.clear();
        for (int i = 0; i < MAX_ENTRIES; i++) {
            c.put(UUID.randomUUID(), createEmptyPage(i));
        }
        for (int i = 0; i < MAX_ENTRIES; i++) {
            c.remove(keys[i]);
        }
        c.clear();
    }

    @BeforeEach
    void setUp() {
        System.gc();
        cache = new RtsPageCache();
    }

    @AfterEach
    void tearDown() {
        cache.clear();
    }

    // ======================================================================
    //  Section 1: LRU eviction throughput at max capacity
    // ======================================================================

    @Test
    void benchmarkLruEvictionAtMaxCapacity() {
        int extraPuts = 5000;

        for (int w = 0; w < WARMUP; w++) {
            runLruBench(MAX_ENTRIES, extraPuts);
        }

        long totalTotalNanos = 0L;
        long totalOps = (long) MAX_ENTRIES + extraPuts;
        for (int i = 0; i < ITERATIONS; i++) {
            totalTotalNanos += runLruBench(MAX_ENTRIES, extraPuts);
        }
        long avgNanos = totalTotalNanos / ITERATIONS;
        System.out.println(String.format("[RtsPageCache] LRU eviction @ max cap (%d): put %,d total ops \u2192 avg %,d ns/op  (%,.0f ops/sec)",
                MAX_ENTRIES, totalOps, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    private long runLruBench(int capacity, int extraPuts) {
        for (int i = 0; i < capacity; i++) {
            cache.put(UUID.randomUUID(), createEmptyPage(i));
        }
        long start = System.nanoTime();
        for (int i = 0; i < extraPuts; i++) {
            cache.put(UUID.randomUUID(), createEmptyPage(capacity + i));
        }
        long end = System.nanoTime();
        cache.clear();
        return end - start;
    }

    // ======================================================================
    //  Section 2: sequential get() under full cache
    // ======================================================================

    @Test
    void benchmarkSequentialGetUnderFullCache() {
        int LOOKUPS = 100_000;

        UUID[] keys = new UUID[MAX_ENTRIES];
        for (int i = 0; i < MAX_ENTRIES; i++) {
            keys[i] = UUID.randomUUID();
            cache.put(keys[i], createEmptyPage(i));
        }

        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < LOOKUPS / MAX_ENTRIES; i++) {
                for (UUID key : keys) {
                    cache.get(key);
                }
            }
        }

        long start = System.nanoTime();
        for (int i = 0; i < LOOKUPS; i++) {
            cache.get(keys[i % MAX_ENTRIES]);
        }
        long end = System.nanoTime();
        long avgNanos = (end - start) / LOOKUPS;
        System.out.println(String.format("[RtsPageCache] sequential get() @ full cache: %,d lookups \u2192 avg %,d ns/op  (%,.0f ops/sec)",
                LOOKUPS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    //  Section 3: random UUID miss
    // ======================================================================

    @Test
    void benchmarkRandomLookupMiss() {
        int LOOKUPS = 100_000;

        for (int i = 0; i < MAX_ENTRIES; i++) {
            cache.put(UUID.randomUUID(), createEmptyPage(i));
        }

        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < LOOKUPS / 10; i++) {
                cache.get(UUID.randomUUID());
            }
        }

        long start = System.nanoTime();
        for (int i = 0; i < LOOKUPS; i++) {
            cache.get(UUID.randomUUID());
        }
        long end = System.nanoTime();
        long avgNanos = (end - start) / LOOKUPS;
        System.out.println(String.format("[RtsPageCache] random UUID miss: %,d lookups \u2192 avg %,d ns/op  (%,.0f ops/sec)",
                LOOKUPS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    //  Section 4: large CachedPage — put/get with 10K-item counts map
    // ======================================================================

    @Test
    void benchmarkLargeCachedPagePutGet() {
        int pageCount = 50;
        int MAP_SIZE = 10_000;

        for (int w = 0; w < WARMUP; w++) {
            runLargePageBench(pageCount, MAP_SIZE);
        }

        long totalPut = 0L, totalGet = 0L;
        for (int i = 0; i < ITERATIONS; i++) {
            long[] r = runLargePageBench(pageCount, MAP_SIZE);
            totalPut += r[0];
            totalGet += r[1];
        }
        long putAvgNanos = totalPut / ITERATIONS / pageCount;
        long getAvgNanos = totalGet / ITERATIONS / pageCount;

        System.out.println(String.format("[RtsPageCache] large CachedPage (%,d map entries): put avg %,d ns/op, get avg %,d ns/op",
                MAP_SIZE, putAvgNanos, getAvgNanos));
    }

    private long[] runLargePageBench(int count, int mapSize) {
        UUID[] uuids = new UUID[count];
        RtsPageCache.CachedPage[] pages = new RtsPageCache.CachedPage[count];
        for (int i = 0; i < count; i++) {
            uuids[i] = UUID.randomUUID();
            pages[i] = createLargePage(i, mapSize);
        }
        long putStart = System.nanoTime();
        for (int i = 0; i < count; i++) {
            cache.put(uuids[i], pages[i]);
        }
        long putEnd = System.nanoTime();
        long getStart = System.nanoTime();
        for (int i = 0; i < count; i++) {
            cache.get(uuids[i]);
        }
        long getEnd = System.nanoTime();
        cache.clear();
        return new long[]{putEnd - putStart, getEnd - getStart};
    }

    // ======================================================================
    //  Section 5: mixed workload — put/get/remove interleaved
    //  Now runs ITERATIONS times for stability.
    // ======================================================================

    @Test
    void benchmarkMixedWorkload() {
        int OPS_PER_ITER = 50_000;

        for (int w = 0; w < WARMUP; w++) {
            runMixedBench(OPS_PER_ITER);
        }

        long totalNanos = 0;
        for (int iter = 0; iter < ITERATIONS; iter++) {
            long start = System.nanoTime();
            runMixedBench(OPS_PER_ITER);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / ITERATIONS / OPS_PER_ITER;

        System.out.println(String.format("[RtsPageCache] mixed workload (put+get+remove): %,d ops \u00d7 %d iters \u2192 avg %,d ns/op  (%,.0f ops/sec)",
                OPS_PER_ITER, ITERATIONS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    private void runMixedBench(int ops) {
        Random rng = ThreadLocalRandom.current();
        for (int i = 0; i < ops; i++) {
            switch (rng.nextInt(4)) {
                case 0 -> cache.put(UUID.randomUUID(), createEmptyPage(rng.nextInt(1000)));
                case 1 -> cache.get(UUID.randomUUID());
                case 2 -> cache.remove(UUID.randomUUID());
                case 3 -> cache.size();
            }
        }
        cache.clear();
    }

    // ======================================================================
    //  Section 6: remove() throughput under full cache
    // ======================================================================

    @Test
    void benchmarkRemoveUnderFullCache() {
        int COUNT = 10_000;

        for (int w = 0; w < WARMUP; w++) {
            UUID[] keys = new UUID[COUNT];
            for (int j = 0; j < COUNT; j++) {
                keys[j] = UUID.randomUUID();
                cache.put(keys[j], createEmptyPage(j));
            }
            for (int j = 0; j < COUNT; j++) {
                cache.remove(keys[j]);
            }
            cache.clear();
        }

        long totalNanos = 0L;
        for (int i = 0; i < ITERATIONS; i++) {
            UUID[] keys = new UUID[COUNT];
            for (int j = 0; j < COUNT; j++) {
                keys[j] = UUID.randomUUID();
                cache.put(keys[j], createEmptyPage(j));
            }
            long start = System.nanoTime();
            for (int j = 0; j < COUNT; j++) {
                cache.remove(keys[j]);
            }
            long end = System.nanoTime();
            totalNanos += (end - start);
            cache.clear();
        }
        long avgNanos = totalNanos / ITERATIONS / COUNT;

        System.out.println(String.format("[RtsPageCache] remove() \u00d7 %,d: avg %,d ns/op  (%,.0f ops/sec)",
                COUNT, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    //  Section 7: clear() throughput
    // ======================================================================

    @Test
    void benchmarkClear() {
        for (int w = 0; w < WARMUP; w++) {
            for (int j = 0; j < MAX_ENTRIES; j++) {
                cache.put(UUID.randomUUID(), createEmptyPage(j));
            }
            cache.clear();
        }

        long totalNanos = 0L;
        for (int i = 0; i < ITERATIONS; i++) {
            for (int j = 0; j < MAX_ENTRIES; j++) {
                cache.put(UUID.randomUUID(), createEmptyPage(j));
            }
            long start = System.nanoTime();
            cache.clear();
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / ITERATIONS;

        System.out.println(String.format("[RtsPageCache] clear() @ full cache: avg %,d ns/op", avgNanos));
    }

    // ======================================================================
    //  Helpers
    // ======================================================================

    private static RtsPageCache.CachedPage createEmptyPage(long dataVersion) {
        var key = new RtsPageCache.CachedPageKey(
                "", RtsStorageSort.NAME, "all", true, 90, false, false);
        return new RtsPageCache.CachedPage(
                key, dataVersion,
                List.of(),
                List.of(),
                Map.of(),
                Map.of(),
                List.of("all"));
    }

    private static RtsPageCache.CachedPage createLargePage(long dataVersion, int mapSize) {
        var key = new RtsPageCache.CachedPageKey(
                "", RtsStorageSort.NAME, "all", true, 90, false, false);
        Map<String, Long> counts = new HashMap<>(mapSize);
        Map<String, Long> namespaceTotals = new HashMap<>();
        for (int i = 0; i < mapSize; i++) {
            counts.put("minecraft:item_" + i, (long) i * 64L);
        }
        namespaceTotals.put("minecraft", (long) mapSize * 32L);
        return new RtsPageCache.CachedPage(
                key, dataVersion,
                List.of(),
                List.of(),
                counts,
                namespaceTotals,
                List.of("all", "minecraft", "building_blocks"));
    }
}

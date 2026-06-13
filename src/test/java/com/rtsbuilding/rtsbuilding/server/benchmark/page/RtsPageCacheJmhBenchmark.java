package com.rtsbuilding.rtsbuilding.server.benchmark.page;

import com.rtsbuilding.rtsbuilding.network.storage.RtsStorageSort;
import com.rtsbuilding.rtsbuilding.server.service.page.RtsPageCache;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * JMH microbenchmarks for {@link RtsPageCache}.
 *
 * <p>Covers LRU eviction, hash-table lookup, large page handling,
 * mixed workloads, remove, and clear. All benchmarks avoid
 * {@code ItemStack} to run without a bootstrapped Minecraft runtime.</p>
 */
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class RtsPageCacheJmhBenchmark {

    private static final int MAX_CAPACITY = 256;

    // ======================================================================
    //  LRU eviction
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Warmup(iterations = 1)
    @Measurement(iterations = 3)
    @OperationsPerInvocation(5_256)
    public void lruEvictionAtMaxCapacity() {
        RtsPageCache c = new RtsPageCache();
        int extraPuts = 5_000;
        for (int i = 0; i < MAX_CAPACITY; i++) {
            c.put(UUID.randomUUID(), createEmptyPage(i));
        }
        for (int i = 0; i < extraPuts; i++) {
            c.put(UUID.randomUUID(), createEmptyPage(MAX_CAPACITY + i));
        }
    }

    // ======================================================================
    //  Sequential get under full cache
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void sequentialGet(FullCacheState state, Blackhole bh) {
        for (int i = 0; i < state.lookups; i++) {
            bh.consume(state.cache.get(state.keys[i % MAX_CAPACITY]));
        }
    }

    @State(Scope.Thread)
    public static class FullCacheState {
        RtsPageCache cache;
        UUID[] keys;
        int lookups = 100_000;

        @Setup(Level.Trial)
        public void setup() {
            cache = new RtsPageCache();
            keys = new UUID[MAX_CAPACITY];
            for (int i = 0; i < MAX_CAPACITY; i++) {
                keys[i] = UUID.randomUUID();
                cache.put(keys[i], createEmptyPage(i));
            }
        }
    }

    // ======================================================================
    //  Random UUID miss
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void randomUuidMiss(MissCacheState state, Blackhole bh) {
        bh.consume(state.cache.get(UUID.randomUUID()));
    }

    @State(Scope.Thread)
    public static class MissCacheState {
        RtsPageCache cache;

        @Setup(Level.Trial)
        public void setup() {
            cache = new RtsPageCache();
            for (int i = 0; i < MAX_CAPACITY; i++) {
                cache.put(UUID.randomUUID(), createEmptyPage(i));
            }
        }
    }

    // ======================================================================
    //  Large CachedPage put/get
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void largePagePut(LargePagePutState state, Blackhole bh) {
        state.cache.put(state.uuid, state.page);
        bh.consume(state.uuid);
    }

    @State(Scope.Thread)
    public static class LargePagePutState {
        RtsPageCache cache;
        UUID uuid;
        RtsPageCache.CachedPage page;

        @Setup(Level.Invocation)
        public void setup() {
            if (cache == null) cache = new RtsPageCache();
            uuid = UUID.randomUUID();
            page = createLargePage(0, 10_000);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void largePageGet(LargePageGetState state, Blackhole bh) {
        bh.consume(state.cache.get(state.uuid));
    }

    @State(Scope.Thread)
    public static class LargePageGetState {
        RtsPageCache cache;
        UUID uuid;

        @Setup(Level.Trial)
        public void setup() {
            cache = new RtsPageCache();
            uuid = UUID.randomUUID();
            cache.put(uuid, createLargePage(0, 10_000));
        }
    }

    // ======================================================================
    //  Mixed workload
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Warmup(iterations = 1)
    @Measurement(iterations = 3)
    @OperationsPerInvocation(50_000)
    public void mixedWorkload() {
        RtsPageCache c = new RtsPageCache();
        Random rng = ThreadLocalRandom.current();
        for (int i = 0; i < 50_000; i++) {
            switch (rng.nextInt(4)) {
                case 0 -> c.put(UUID.randomUUID(), createEmptyPage(rng.nextInt(1000)));
                case 1 -> c.get(UUID.randomUUID());
                case 2 -> c.remove(UUID.randomUUID());
                case 3 -> c.size();
            }
        }
    }

    // ======================================================================
    //  Remove under full cache
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Warmup(iterations = 1)
    @Measurement(iterations = 3)
    @OperationsPerInvocation(10_000)
    public void removeUnderFullCache(RemoveState state) {
        for (int i = 0; i < 10_000; i++) {
            state.cache.remove(state.keys[i]);
        }
    }

    @State(Scope.Thread)
    public static class RemoveState {
        RtsPageCache cache;
        UUID[] keys;

        @Setup(Level.Invocation)
        public void setup() {
            cache = new RtsPageCache();
            keys = new UUID[10_000];
            for (int i = 0; i < 10_000; i++) {
                keys[i] = UUID.randomUUID();
                cache.put(keys[i], createEmptyPage(i));
            }
        }
    }

    // ======================================================================
    //  Clear
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Warmup(iterations = 1)
    @Measurement(iterations = 3)
    public void clear() {
        RtsPageCache c = new RtsPageCache();
        for (int i = 0; i < MAX_CAPACITY; i++) {
            c.put(UUID.randomUUID(), createEmptyPage(i));
        }
        c.clear();
    }

    // ======================================================================
    //  Helpers
    // ======================================================================

    private static RtsPageCache.CachedPage createEmptyPage(long dataVersion) {
        var key = new RtsPageCache.CachedPageKey(
                "", RtsStorageSort.NAME, "all", true, 90, false, false);
        return new RtsPageCache.CachedPage(
                key, dataVersion,
                List.of(), List.of(), Map.of(), Map.of(), List.of("all"));
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
                List.of(), List.of(), counts, namespaceTotals,
                List.of("all", "minecraft", "building_blocks"));
    }
}

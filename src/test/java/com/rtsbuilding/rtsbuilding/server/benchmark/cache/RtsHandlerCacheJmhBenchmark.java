package com.rtsbuilding.rtsbuilding.server.benchmark.cache;

import com.rtsbuilding.rtsbuilding.server.storage.RtsHandlerCache;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JMH microbenchmarks for {@link RtsHandlerCache}.
 *
 * <p>Seeds the internal {@code countsByItem} map via reflection to avoid
 * triggering Minecraft's {@code ItemStack} static initializer.</p>
 */
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class RtsHandlerCacheJmhBenchmark {

    // ======================================================================
    //  getAvailableItems — bulk dump
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void getAvailableItemsLarge(LargeMapState state, Blackhole bh) {
        Map<String, Long> out = new HashMap<>(100_000);
        state.cache.getAvailableItems(out);
        bh.consume(out);
    }

    @State(Scope.Thread)
    public static class LargeMapState {
        RtsHandlerCache cache;

        @Setup(Level.Trial)
        public void setup() {
            cache = new RtsHandlerCache();
            seedCounts(cache, 100_000);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void getAvailableItemsSmall(SmallMapState state, Blackhole bh) {
        Map<String, Long> out = new HashMap<>(256);
        state.cache.getAvailableItems(out);
        bh.consume(out);
    }

    @State(Scope.Thread)
    public static class SmallMapState {
        RtsHandlerCache cache;

        @Setup(Level.Trial)
        public void setup() {
            cache = new RtsHandlerCache();
            seedCounts(cache, 256);
        }
    }

    // ======================================================================
    //  getCount — string lookup
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void getCountLookup(LookupState state, Blackhole bh) {
        for (int i = 0; i < 100_000; i++) {
            bh.consume(state.cache.getCount(state.keys[i % state.keys.length]));
        }
    }

    @State(Scope.Thread)
    public static class LookupState {
        RtsHandlerCache cache;
        String[] keys;

        @Setup(Level.Trial)
        public void setup() {
            cache = new RtsHandlerCache();
            seedCounts(cache, 100_000);
            keys = new String[100_000];
            for (int i = 0; i < 100_000; i++) {
                keys[i] = "minecraft:item_" + i;
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void getCountNull(Blackhole bh) {
        bh.consume(new RtsHandlerCache().getCount((String) null));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void getCountByItem(ItemLookupState state, Blackhole bh) {
        bh.consume(state.cache.getCount(state.item));
    }

    @State(Scope.Thread)
    public static class ItemLookupState {
        RtsHandlerCache cache;
        net.minecraft.world.item.Item item;

        @Setup(Level.Trial)
        public void setup() {
            cache = new RtsHandlerCache();
            seedCounts(cache, 100);
            item = mock(net.minecraft.world.item.Item.class);
            when(item.toString()).thenReturn("minecraft:diamond");
        }
    }

    // ======================================================================
    //  invalidate / release
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void invalidate(InvalidateState state, Blackhole bh) {
        bh.consume(state.cache);
        state.cache.invalidate();
    }

    @State(Scope.Thread)
    public static class InvalidateState {
        RtsHandlerCache cache;

        @Setup(Level.Invocation)
        public void setup() {
            cache = new RtsHandlerCache();
            seedCounts(cache, 10_000);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void invalidateEmpty(Blackhole bh) {
        new RtsHandlerCache().invalidate();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void release(ReleaseState state, Blackhole bh) {
        bh.consume(state.cache);
        state.cache.release();
    }

    @State(Scope.Thread)
    public static class ReleaseState {
        RtsHandlerCache cache;

        @Setup(Level.Invocation)
        public void setup() {
            cache = new RtsHandlerCache();
            seedCounts(cache, 50_000);
        }
    }

    // ======================================================================
    //  Trivial getters
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void getCachedSlotCount(Blackhole bh) {
        bh.consume(new RtsHandlerCache().getCachedSlotCount());
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void isDirty(Blackhole bh) {
        bh.consume(new RtsHandlerCache().isDirty());
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void clearDirty(Blackhole bh) {
        new RtsHandlerCache().clearDirty();
    }

    // ======================================================================
    //  Helpers
    // ======================================================================

    private static void seedCounts(RtsHandlerCache cache, int count) {
        try {
            Field countsField = RtsHandlerCache.class.getDeclaredField("countsByItem");
            countsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Long> counts = (Map<String, Long>) countsField.get(cache);
            counts.clear();
            Random rng = ThreadLocalRandom.current();
            for (int i = 0; i < count; i++) {
                counts.put("minecraft:item_" + i, (long) (rng.nextInt(1000) + 1));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed cache counts", e);
        }
    }
}

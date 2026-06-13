package com.rtsbuilding.rtsbuilding.server.benchmark.cache;

import com.rtsbuilding.rtsbuilding.server.storage.RtsHandlerCache;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Extreme Performance Benchmarks for {@link RtsHandlerCache}.
 *
 * <p>Focuses on map operation throughput under high cardinality. The
 * {@code countsByItem} map is seeded via reflection to avoid triggering
 * Minecraft's {@code ItemStack} static initializer.</p>
 */
class RtsHandlerCacheBenchmark {

    private static final int WARMUP = 3;
    private static final int ITERATIONS = 10;
    private static final int ENTRIES_LARGE = 100_000;
    private static final int ENTRIES_SMALL = 256;
    private static final int LOOKUPS = 1_000_000;

    private RtsHandlerCache cache;

    /**
     * Global JIT warmup — runs once before all tests. Exercises all code paths
     * so the JIT compiler compiles them before measurement begins.
     */
    @BeforeAll
    static void globalWarmUp() {
        RtsHandlerCache c = new RtsHandlerCache();
        // Warm up getAvailableItems — moderate size
        seedCounts(c, 10_000);
        for (int i = 0; i < 50; i++) {
            c.getAvailableItems(new HashMap<>(10_000));
        }
        c.release();

        // Warm up getCount path
        c = new RtsHandlerCache();
        seedCounts(c, 10_000);
        for (int i = 0; i < 5_000; i++) {
            c.getCount("minecraft:item_" + (i % 10_000));
        }
        // Warm up invalidate path
        for (int i = 0; i < 50; i++) {
            c.invalidate();
        }
        // Warm up getter paths
        for (int i = 0; i < 5_000; i++) {
            c.getCachedSlotCount();
            c.isDirty();
            c.clearDirty();
        }
    }

    @BeforeEach
    void setUp() {
        System.gc();
        cache = new RtsHandlerCache();
    }

    // ======================================================================
    //  Section 1: getAvailableItems() — bulk dump with 100K entries
    // ======================================================================

    @Test
    void benchmarkGetAvailableItemsLarge() {
        RtsHandlerCache storage = new RtsHandlerCache();
        seedCounts(storage, ENTRIES_LARGE);
        Map<String, Long> out = new HashMap<>(ENTRIES_LARGE);

        for (int w = 0; w < WARMUP; w++) {
            drainAvailableItems(storage);
        }

        long totalNanos = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            storage.getAvailableItems(out);
            long end = System.nanoTime();
            totalNanos += (end - start);
            out.clear();
        }
        long avgNanos = totalNanos / ITERATIONS;
        System.out.println(String.format("[RtsHandlerCache] getAvailableItems(%,d entries): avg %,d ns/op  (%,.0f ops/sec)",
                ENTRIES_LARGE, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    @Test
    void benchmarkGetAvailableItemsSmall() {
        RtsHandlerCache storage = new RtsHandlerCache();
        seedCounts(storage, ENTRIES_SMALL);
        Map<String, Long> out = new HashMap<>(ENTRIES_SMALL);

        for (int w = 0; w < WARMUP; w++) {
            drainAvailableItems(storage);
        }

        long totalNanos = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            storage.getAvailableItems(out);
            long end = System.nanoTime();
            totalNanos += (end - start);
            out.clear();
        }
        long avgNanos = totalNanos / ITERATIONS;
        System.out.println(String.format("[RtsHandlerCache] getAvailableItems(%d entries): avg %,d ns/op  (%,.0f ops/sec)",
                ENTRIES_SMALL, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    @Test
    void benchmarkGetCountByItem() {
        RtsHandlerCache storage = new RtsHandlerCache();
        seedCounts(storage, ENTRIES_LARGE);
        var item = mock(net.minecraft.world.item.Item.class);
        when(item.toString()).thenReturn("minecraft:diamond");

        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < 1_000; i++) {
                storage.getCount(item);
            }
        }

        long start = System.nanoTime();
        for (int i = 0; i < LOOKUPS; i++) {
            storage.getCount(item);
        }
        long end = System.nanoTime();
        long avgNanos = (end - start) / LOOKUPS;
        System.out.println(String.format("[RtsHandlerCache] getCount(%,d entries, %,d lookups): avg %,d ns/op  (%,.0f ops/sec)",
                ENTRIES_LARGE, LOOKUPS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    @Test
    void benchmarkGetCountNull() {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < 10_000; i++) {
                cache.getCount((String) null);
            }
        }

        long start = System.nanoTime();
        for (int i = 0; i < 1_000_000; i++) {
            cache.getCount((String) null);
        }
        long end = System.nanoTime();
        long avgNanos = (end - start) / 1_000_000;
        System.out.println(String.format("[RtsHandlerCache] getCount(null) \u00d7 1M: avg %,d ns/op", avgNanos));
    }

    @Test
    void benchmarkGetCountLookup() {
        RtsHandlerCache storage = new RtsHandlerCache();
        seedCounts(storage, ENTRIES_LARGE);
        String[] keys = generateKeys(ENTRIES_LARGE);

        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < LOOKUPS / 10; i++) {
                storage.getCount(keys[i % ENTRIES_LARGE]);
            }
        }

        long start = System.nanoTime();
        for (int i = 0; i < LOOKUPS; i++) {
            storage.getCount(keys[i % ENTRIES_LARGE]);
        }
        long end = System.nanoTime();
        long avgNanos = (end - start) / LOOKUPS;
        System.out.println(String.format("[RtsHandlerCache] getCount(%,d entries, %,d lookups): avg %,d ns/op  (%,.0f ops/sec)",
                ENTRIES_LARGE, LOOKUPS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    @Test
    void benchmarkGetCountLookupMock() {
        RtsHandlerCache storage = new RtsHandlerCache();
        seedCounts(storage, ENTRIES_LARGE);
        var item = mock(net.minecraft.world.item.Item.class);
        when(item.toString()).thenReturn("minecraft:diamond");

        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < 1_000; i++) {
                storage.getCount(item);
            }
        }

        long start = System.nanoTime();
        for (int i = 0; i < 100_000; i++) {
            storage.getCount(item);
        }
        long end = System.nanoTime();
        long avgNanos = (end - start) / 100_000;
        System.out.println(String.format("[RtsHandlerCache] getCount(Item mock) \u00d7 100K: avg %,d ns/op", avgNanos));
    }

    @Test
    void benchmarkInvalidate() {
        RtsHandlerCache storage = new RtsHandlerCache();
        seedCounts(storage, 10_000);

        for (int w = 0; w < WARMUP; w++) {
            storage.invalidate();
        }

        long start = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            storage.invalidate();
        }
        long end = System.nanoTime();
        long avgNanos = (end - start) / 10_000;
        System.out.println(String.format("[RtsHandlerCache] invalidate() (after 10K entries): avg %,d ns/op", avgNanos));
    }

    @Test
    void benchmarkInvalidateEmpty() {
        for (int w = 0; w < WARMUP; w++) {
            cache.invalidate();
        }

        long totalNanos = 0;
        int calls = 1_000_000;
        for (int i = 0; i < calls; i++) {
            long start = System.nanoTime();
            cache.invalidate();
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / calls;
        System.out.println(String.format("[RtsHandlerCache] invalidate() (empty cache), %,d calls: avg %,d ns/op",
                calls, avgNanos));
    }

    @Test
    void benchmarkRelease() {
        long totalNanos = 0;
        int iterations = ITERATIONS;
        for (int i = 0; i < iterations; i++) {
            RtsHandlerCache c = new RtsHandlerCache();
            seedCounts(c, 50_000);
            long start = System.nanoTime();
            c.release();
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / iterations;
        System.out.println(String.format("[RtsHandlerCache] release() (50K entries), %,d iterations: avg %,d ns",
                iterations, avgNanos));
    }

    @Test
    void benchmarkClearDirty() {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < 10_000; i++) {
                cache.getCachedSlotCount();
                cache.isDirty();
                cache.clearDirty();
            }
        }

        long start = System.nanoTime();
        for (int i = 0; i < 1_000_000; i++) {
            cache.getCachedSlotCount();
        }
        long end = System.nanoTime();
        System.out.println(String.format("[RtsHandlerCache] getCachedSlotCount() \u00d7 1M: avg %,d ns/op", (end - start) / 1_000_000));
    }

    @Test
    void benchmarkIsDirty() {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < 10_000; i++) {
                cache.getCachedSlotCount();
                cache.isDirty();
                cache.clearDirty();
            }
        }

        long start = System.nanoTime();
        for (int i = 0; i < 1_000_000; i++) {
            cache.isDirty();
        }
        long end = System.nanoTime();
        System.out.println(String.format("[RtsHandlerCache] isDirty() \u00d7 1M: avg %,d ns/op", (end - start) / 1_000_000));
    }

    // ======================================================================
    //  Helpers
    // ======================================================================

    /**
     * Seeds {@code countsByItem} via reflection with {@code count} entries.
     * This avoids calling {@code update(IItemHandler)} which would trigger
     * Minecraft's {@code BuiltInRegistries.ITEM} and {@code CachedSlot.EMPTY}.
     */
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

    private static void drainAvailableItems(RtsHandlerCache cache) {
        cache.getAvailableItems(new HashMap<>());
    }

    private static String[] generateKeys(int count) {
        String[] keys = new String[count];
        for (int i = 0; i < count; i++) {
            keys[i] = "minecraft:item_" + i;
        }
        return keys;
    }
}

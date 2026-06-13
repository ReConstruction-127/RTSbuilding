package com.rtsbuilding.rtsbuilding.server.benchmark.aggregate;

import com.rtsbuilding.rtsbuilding.server.storage.RtsAggregateStorage;
import com.rtsbuilding.rtsbuilding.server.storage.RtsHandlerCache;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.items.IItemHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.mockito.Mockito.*;

/**
 * Extreme Performance Benchmarks for {@link RtsAggregateStorage}.
 *
 * <p>Focuses on mount/unmount overhead, priority sorting, and iteration
 * throughput over many handlers. Does NOT test {@code insert()} or
 * {@code extract()} — those reach {@code ItemStack.EMPTY} which triggers
 * Minecraft's static initializer.</p>
 */
class RtsAggregateStorageBenchmark {

    private static final int WARMUP = 3;
    private static final int ITERATIONS = 10;

    private RtsAggregateStorage storage;

    /**
     * Global JIT warmup — runs once before all tests. Exercises all code paths
     * with realistic data sizes so the JIT compiler compiles them before
     * measurement begins, dramatically reducing cross-run variance.
     */
    @BeforeAll
    static void globalWarmUp() {
        RtsAggregateStorage s = new RtsAggregateStorage();
        IItemHandler handler = mock(IItemHandler.class);
        RtsHandlerCache cache = mock(RtsHandlerCache.class);
        when(cache.getCount(anyString())).thenReturn(0L);
        Item item = mock(Item.class);
        when(item.toString()).thenReturn("minecraft:diamond");

        // Warm up mount + rebuildFlatOrder — use few handlers, many loop iterations
        // to trigger JIT without creating thousands of expensive mocks.
        for (int i = 0; i < 100; i++) {
            s.mount(0, handler, cache);
        }
        // Warm up unmount path
        for (int i = 0; i < 100; i++) {
            s.unmount(handler);
        }

        // Warm up query paths with small handler set
        s = new RtsAggregateStorage();
        for (int i = 0; i < 100; i++) {
            s.mount(0, handler, cache);
        }
        for (int i = 0; i < 3_000; i++) {
            s.hasItem(item);
            s.getTotalCount(item);
            s.isEmpty();
            s.drainPendingChanges();
            s.tickUpdate();
        }
        Map<String, Long> out = new HashMap<>();
        for (int i = 0; i < 500; i++) {
            s.getAvailableItems(out);
            out.clear();
        }
    }

    @BeforeEach
    void setUp() {
        // Trigger GC before each test to reduce GC-induced noise
        System.gc();
        storage = new RtsAggregateStorage();
    }

    // ======================================================================
    //  Section 1: mount — sequential, random priority, many handlers
    //  Mocks are created once outside the timed loop to avoid Mockito overhead.
    // ======================================================================

    @Test
    void benchmarkMountSequentialSamePriority() {
        int count = 10_000;
        IItemHandler handler = mock(IItemHandler.class);
        RtsHandlerCache cache = mock(RtsHandlerCache.class);

        long totalNanos = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            RtsAggregateStorage s = new RtsAggregateStorage();
            long start = System.nanoTime();
            for (int j = 0; j < count; j++) {
                s.mount(0, handler, cache);
            }
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgTotal = totalNanos / ITERATIONS;
        System.out.println(String.format("[RtsAggregateStorage] mount(%,d, same priority=0): avg %,d ns total, ~%,d ns/op",
                count, avgTotal, avgTotal / count));
    }

    @Test
    void benchmarkMountRandomPriorities() {
        int count = 5_000;
        IItemHandler handler = mock(IItemHandler.class);
        RtsHandlerCache cache = mock(RtsHandlerCache.class);
        Random rng = new Random(42);

        long totalNanos = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            RtsAggregateStorage s = new RtsAggregateStorage();
            rng.setSeed(42);
            long start = System.nanoTime();
            for (int j = 0; j < count; j++) {
                s.mount(rng.nextInt(200) - 100, handler, cache);
            }
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgTotal = totalNanos / ITERATIONS;
        System.out.println(String.format("[RtsAggregateStorage] mount(%,d, random priorities): avg %,d ns total, ~%,d ns/op",
                count, avgTotal, avgTotal / count));
    }

    // ======================================================================
    //  Section 2: unmount — sequential unmount of all handlers
    //  Uses multiple iterations and GC control to reduce noise.
    // ======================================================================

    @Test
    void benchmarkUnmountAllSamePriority() {
        int count = 5_000;
        IItemHandler handler = mock(IItemHandler.class);
        RtsHandlerCache cache = mock(RtsHandlerCache.class);

        long totalNanos = 0;
        for (int iter = 0; iter < ITERATIONS; iter++) {
            IItemHandler[] handlers = new IItemHandler[count];
            RtsHandlerCache[] caches = new RtsHandlerCache[count];
            RtsAggregateStorage s = new RtsAggregateStorage();
            for (int i = 0; i < count; i++) {
                handlers[i] = handler;
                caches[i] = cache;
                s.mount(0, handlers[i], caches[i]);
            }
            long start = System.nanoTime();
            for (int i = 0; i < count; i++) {
                s.unmount(handlers[i]);
            }
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / ITERATIONS / count;
        System.out.println(String.format("[RtsAggregateStorage] unmount(%,d, same priority): avg %,d ns/op, total %,d ns",
                count, avgNanos, totalNanos / ITERATIONS));
    }

    @Test
    void benchmarkUnmountDifferentPriorities() {
        int count = 5_000;
        IItemHandler handler = mock(IItemHandler.class);
        RtsHandlerCache cache = mock(RtsHandlerCache.class);

        long totalNanos = 0;
        for (int iter = 0; iter < ITERATIONS; iter++) {
            IItemHandler[] handlers = new IItemHandler[count];
            RtsAggregateStorage s = new RtsAggregateStorage();
            Random rng = new Random(42);
            for (int i = 0; i < count; i++) {
                handlers[i] = handler;
                s.mount(rng.nextInt(200) - 100, handlers[i], cache);
            }
            long start = System.nanoTime();
            for (int i = 0; i < count; i++) {
                s.unmount(handlers[i]);
            }
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / ITERATIONS / count;
        System.out.println(String.format("[RtsAggregateStorage] unmount(%,d, random priorities): avg %,d ns/op, total %,d ns",
                count, avgNanos, totalNanos / ITERATIONS));
    }

    // ======================================================================
    //  Section 3: hasItem() — linear scan over flat list
    // ======================================================================

    @Test
    void benchmarkHasItemLarge() {
        int handlerCount = 10_000;
        IItemHandler handler = mock(IItemHandler.class);
        RtsHandlerCache cache = mock(RtsHandlerCache.class);
        when(cache.getCount(anyString())).thenReturn(0L);
        for (int i = 0; i < handlerCount; i++) {
            storage.mount(0, handler, cache);
        }

        Item mockItem = mock(Item.class);
        when(mockItem.toString()).thenReturn("minecraft:diamond");

        // Warmup
        for (int w = 0; w < WARMUP; w++) {
            storage.hasItem(mockItem);
        }

        long totalNanos = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            storage.hasItem(mockItem);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / ITERATIONS;
        System.out.println(String.format("[RtsAggregateStorage] hasItem(%,d handlers): avg %,d ns/op",
                handlerCount, avgNanos));
    }

    @Test
    void benchmarkHasItemSmall() {
        int handlerCount = 10;
        IItemHandler handler = mock(IItemHandler.class);
        RtsHandlerCache cache = mock(RtsHandlerCache.class);
        when(cache.getCount(anyString())).thenReturn(0L);
        for (int i = 0; i < handlerCount; i++) {
            storage.mount(0, handler, cache);
        }

        Item mockItem = mock(Item.class);
        when(mockItem.toString()).thenReturn("minecraft:diamond");

        // Warmup
        for (int w = 0; w < WARMUP; w++) {
            storage.hasItem(mockItem);
        }

        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS * 1000; i++) {
            storage.hasItem(mockItem);
        }
        long end = System.nanoTime();
        long avgNanos = (end - start) / (ITERATIONS * 1000);
        System.out.println(String.format("[RtsAggregateStorage] hasItem(%,d handlers): avg %,d ns/op",
                handlerCount, avgNanos));
    }

    // ======================================================================
    //  Section 4: getTotalCount() — linear scan with sum aggregation
    // ======================================================================

    @Test
    void benchmarkGetTotalCountLarge() {
        int handlerCount = 10_000;
        IItemHandler handler = mock(IItemHandler.class);
        RtsHandlerCache cache = mock(RtsHandlerCache.class);
        when(cache.getCount(anyString())).thenReturn(42L);
        for (int i = 0; i < handlerCount; i++) {
            storage.mount(0, handler, cache);
        }

        Item mockItem = mock(Item.class);
        when(mockItem.toString()).thenReturn("minecraft:diamond");

        // Warmup
        for (int w = 0; w < WARMUP; w++) {
            storage.getTotalCount(mockItem);
        }

        long totalNanos = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            storage.getTotalCount(mockItem);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / ITERATIONS;
        System.out.println(String.format("[RtsAggregateStorage] getTotalCount(%,d handlers): avg %,d ns/op",
                handlerCount, avgNanos));
    }

    // ======================================================================
    //  Section 5: getAvailableItems() — delegation to all caches
    // ======================================================================

    @Test
    void benchmarkGetAvailableItemsLarge() {
        int handlerCount = 1_000;
        IItemHandler handler = mock(IItemHandler.class);
        for (int i = 0; i < handlerCount; i++) {
            RtsHandlerCache cache = mock(RtsHandlerCache.class);
            final int idx = i;
            doAnswer(inv -> {
                Map<String, Long> out = inv.getArgument(0);
                out.put("minecraft:item_" + (idx % 100), 64L);
                return null;
            }).when(cache).getAvailableItems(anyMap());
            storage.mount(0, handler, cache);
        }

        // Warmup
        Map<String, Long> out = new HashMap<>();
        for (int w = 0; w < WARMUP; w++) {
            storage.getAvailableItems(out);
            out.clear();
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
        System.out.println(String.format("[RtsAggregateStorage] getAvailableItems(%,d handlers): avg %,d ns/op",
                handlerCount, avgNanos));
    }

    // ======================================================================
    //  Section 6: tickUpdate() — delegation to all mounted panels
    // ======================================================================

    @Test
    void benchmarkTickUpdateLarge() {
        int handlerCount = 10_000;
        IItemHandler handler = mock(IItemHandler.class);
        RtsHandlerCache cache = mock(RtsHandlerCache.class);
        for (int i = 0; i < handlerCount; i++) {
            storage.mount(0, handler, cache);
        }

        // Warmup
        for (int w = 0; w < WARMUP; w++) {
            storage.tickUpdate();
        }

        long totalNanos = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            storage.tickUpdate();
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / ITERATIONS;
        System.out.println(String.format("[RtsAggregateStorage] tickUpdate(%,d handlers): avg %,d ns/op",
                handlerCount, avgNanos));
    }

    @Test
    void benchmarkTickUpdateEmpty() {
        // Warmup
        for (int w = 0; w < WARMUP; w++) {
            storage.tickUpdate();
        }

        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS * 1000; i++) {
            storage.tickUpdate();
        }
        long end = System.nanoTime();
        long avgNanos = (end - start) / (ITERATIONS * 1000);
        System.out.println(String.format("[RtsAggregateStorage] tickUpdate(empty): avg %,d ns/op", avgNanos));
    }

    // ======================================================================
    //  Section 7: drainPendingChanges() — iterate + clear pending ops
    // ======================================================================

    @Test
    void benchmarkDrainPendingChangesEmpty() {
        // Warmup
        for (int w = 0; w < WARMUP; w++) {
            storage.drainPendingChanges();
        }

        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS * 1000; i++) {
            storage.drainPendingChanges();
        }
        long end = System.nanoTime();
        long avgNanos = (end - start) / (ITERATIONS * 1000);
        System.out.println(String.format("[RtsAggregateStorage] drainPendingChanges(empty): avg %,d ns/op", avgNanos));
    }

    @Test
    void benchmarkDrainPendingChangesNonEmpty() {
        IItemHandler handler = mock(IItemHandler.class);
        RtsHandlerCache cache = mock(RtsHandlerCache.class);
        for (int i = 0; i < 1000; i++) {
            storage.mount(0, handler, cache);
        }
        // Seed pending changes
        storage.drainPendingChanges();
        for (int i = 0; i < 1000; i++) {
            storage.mount(0, handler, cache);
        }

        // Warmup
        for (int w = 0; w < WARMUP; w++) {
            storage.drainPendingChanges();
        }

        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS * 1000; i++) {
            storage.drainPendingChanges();
        }
        long end = System.nanoTime();
        System.out.println(String.format("[RtsAggregateStorage] drainPendingChanges \u00d7 %,d: avg %,d ns/op",
                ITERATIONS * 1000, (end - start) / (ITERATIONS * 1000)));
    }

    // ======================================================================
    //  Section 8: isEmpty() — trivial read
    // ======================================================================

    @Test
    void benchmarkIsEmpty() {
        // Warmup
        for (int w = 0; w < WARMUP; w++) {
            storage.isEmpty();
        }

        long start = System.nanoTime();
        for (int i = 0; i < 1_000_000; i++) {
            storage.isEmpty();
        }
        long end = System.nanoTime();
        long avgNanos = (end - start) / 1_000_000;
        System.out.println(String.format("[RtsAggregateStorage] isEmpty() \u00d7 1M: avg %,d ns/op", avgNanos));
    }
}

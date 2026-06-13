package com.rtsbuilding.rtsbuilding.server.benchmark.aggregate;

import com.rtsbuilding.rtsbuilding.server.storage.RtsAggregateStorage;
import com.rtsbuilding.rtsbuilding.server.storage.RtsHandlerCache;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.items.IItemHandler;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

/**
 * JMH microbenchmarks for {@link RtsAggregateStorage}.
 *
 * <p>JMH handles warmup, JIT compilation, GC control, and outlier
 * detection automatically. Each benchmark measures average time per
 * operation using forked JVM processes for full isolation.</p>
 */
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class RtsAggregateStorageJmhBenchmark {

    // Shared mock instances (created once per fork)
    private IItemHandler handler;
    private RtsHandlerCache cache;
    private RtsHandlerCache cacheWithCount;
    private Item item;

    @Setup(Level.Trial)
    public void globalSetup() {
        handler = mock(IItemHandler.class);

        // Plain cache (always returns 0)
        cache = mock(RtsHandlerCache.class);
        when(cache.getCount(anyString())).thenReturn(0L);

        // Cache with item count
        cacheWithCount = mock(RtsHandlerCache.class);
        when(cacheWithCount.getCount(anyString())).thenReturn(42L);

        item = mock(Item.class);
        when(item.toString()).thenReturn("minecraft:diamond");
    }

    // ======================================================================
    //  Mount benchmarks (SingleShot — each invocation does bulk work)
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Warmup(iterations = 1)
    @Measurement(iterations = 3)
    @OperationsPerInvocation(10_000)
    public void mountSequentialSamePriority() {
        RtsAggregateStorage s = new RtsAggregateStorage();
        for (int j = 0; j < 10_000; j++) {
            s.mount(0, handler, cache);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Warmup(iterations = 1)
    @Measurement(iterations = 3)
    @OperationsPerInvocation(5_000)
    public void mountRandomPriorities() {
        RtsAggregateStorage s = new RtsAggregateStorage();
        Random rng = new Random(42);
        for (int j = 0; j < 5_000; j++) {
            s.mount(rng.nextInt(200) - 100, handler, cache);
        }
    }

    // ======================================================================
    //  Unmount benchmarks (SingleShot — each invocation does bulk work)
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Warmup(iterations = 1)
    @Measurement(iterations = 3)
    @OperationsPerInvocation(5_000)
    public void unmountAllSamePriority(UnmountState state) {
        for (int i = 0; i < 5_000; i++) {
            state.storage.unmount(state.handlers[i]);
        }
    }

    @State(Scope.Thread)
    public static class UnmountState {
        RtsAggregateStorage storage;
        IItemHandler[] handlers;

        @Setup(Level.Invocation)
        public void setup() {
            storage = new RtsAggregateStorage();
            IItemHandler h = mock(IItemHandler.class);
            RtsHandlerCache c = mock(RtsHandlerCache.class);
            handlers = new IItemHandler[5_000];
            for (int i = 0; i < 5_000; i++) {
                handlers[i] = h;
                storage.mount(0, h, c);
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Warmup(iterations = 1)
    @Measurement(iterations = 3)
    @OperationsPerInvocation(5_000)
    public void unmountDifferentPriorities(UnmountRandomState state) {
        for (int i = 0; i < 5_000; i++) {
            state.storage.unmount(state.handlers[i]);
        }
    }

    @State(Scope.Thread)
    public static class UnmountRandomState {
        RtsAggregateStorage storage;
        IItemHandler[] handlers;

        @Setup(Level.Invocation)
        public void setup() {
            storage = new RtsAggregateStorage();
            IItemHandler h = mock(IItemHandler.class);
            RtsHandlerCache c = mock(RtsHandlerCache.class);
            handlers = new IItemHandler[5_000];
            Random rng = new Random(42);
            for (int i = 0; i < 5_000; i++) {
                handlers[i] = h;
                storage.mount(rng.nextInt(200) - 100, h, c);
            }
        }
    }

    // ======================================================================
    //  Query benchmarks (AverageTime — each invocation is a single operation)
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void hasItemLarge(PopulatedLargeState state, Blackhole bh) {
        bh.consume(state.storage.hasItem(state.item));
    }

    @State(Scope.Thread)
    public static class PopulatedLargeState {
        RtsAggregateStorage storage;
        Item item;

        @Setup(Level.Trial)
        public void setup() {
            storage = new RtsAggregateStorage();
            IItemHandler h = mock(IItemHandler.class);
            RtsHandlerCache c = mock(RtsHandlerCache.class);
            when(c.getCount(anyString())).thenReturn(0L);
            for (int i = 0; i < 10_000; i++) {
                storage.mount(0, h, c);
            }
            item = mock(Item.class);
            when(item.toString()).thenReturn("minecraft:diamond");
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void hasItemSmall(PopulatedSmallState state, Blackhole bh) {
        bh.consume(state.storage.hasItem(state.item));
    }

    @State(Scope.Thread)
    public static class PopulatedSmallState {
        RtsAggregateStorage storage;
        Item item;

        @Setup(Level.Trial)
        public void setup() {
            storage = new RtsAggregateStorage();
            IItemHandler h = mock(IItemHandler.class);
            RtsHandlerCache c = mock(RtsHandlerCache.class);
            when(c.getCount(anyString())).thenReturn(0L);
            for (int i = 0; i < 10; i++) {
                storage.mount(0, h, c);
            }
            item = mock(Item.class);
            when(item.toString()).thenReturn("minecraft:diamond");
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void getTotalCountLarge(PopulatedTotalCountState state, Blackhole bh) {
        bh.consume(state.storage.getTotalCount(state.item));
    }

    @State(Scope.Thread)
    public static class PopulatedTotalCountState {
        RtsAggregateStorage storage;
        Item item;

        @Setup(Level.Trial)
        public void setup() {
            storage = new RtsAggregateStorage();
            IItemHandler h = mock(IItemHandler.class);
            RtsHandlerCache c = mock(RtsHandlerCache.class);
            when(c.getCount(anyString())).thenReturn(42L);
            for (int i = 0; i < 10_000; i++) {
                storage.mount(0, h, c);
            }
            item = mock(Item.class);
            when(item.toString()).thenReturn("minecraft:diamond");
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void getAvailableItemsLarge(PopulatedAvailableState state, Blackhole bh) {
        Map<String, Long> out = new HashMap<>();
        state.storage.getAvailableItems(out);
        bh.consume(out);
    }

    @State(Scope.Thread)
    public static class PopulatedAvailableState {
        RtsAggregateStorage storage;

        @Setup(Level.Trial)
        public void setup() {
            storage = new RtsAggregateStorage();
            IItemHandler h = mock(IItemHandler.class);
            for (int i = 0; i < 1_000; i++) {
                RtsHandlerCache c = mock(RtsHandlerCache.class);
                final int idx = i;
                doAnswer(inv -> {
                    Map<String, Long> out = inv.getArgument(0);
                    out.put("minecraft:item_" + (idx % 100), 64L);
                    return null;
                }).when(c).getAvailableItems(any());
                storage.mount(0, h, c);
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void tickUpdateLarge(PopulatedTickState state, Blackhole bh) {
        bh.consume(state.storage.tickUpdate());
    }

    @State(Scope.Thread)
    public static class PopulatedTickState {
        RtsAggregateStorage storage;

        @Setup(Level.Trial)
        public void setup() {
            storage = new RtsAggregateStorage();
            IItemHandler h = mock(IItemHandler.class);
            for (int i = 0; i < 1_000; i++) {
                RtsHandlerCache c = mock(RtsHandlerCache.class);
                Set<String> changes = new HashSet<>();
                changes.add("minecraft:item_" + i);
                when(c.update(h)).thenReturn(changes);
                storage.mount(0, h, c);
            }
        }
    }

    // ======================================================================
    //  Trivial benchmarks — no special state needed, use per-invocation setup
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void isEmpty(Blackhole bh) {
        bh.consume(new RtsAggregateStorage().isEmpty());
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void drainPendingChanges(Blackhole bh) {
        bh.consume(new RtsAggregateStorage().drainPendingChanges());
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void tickUpdateEmpty(Blackhole bh) {
        bh.consume(new RtsAggregateStorage().tickUpdate());
    }
}

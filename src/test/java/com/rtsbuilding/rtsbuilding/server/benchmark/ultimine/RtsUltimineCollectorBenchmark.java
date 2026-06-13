package com.rtsbuilding.rtsbuilding.server.benchmark.ultimine;

import com.rtsbuilding.rtsbuilding.common.RtsUltimineCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.mockito.Mockito.*;

/**
 * Extreme Performance Benchmarks for {@link RtsUltimineCollector}.
 *
 * <p>Focuses on BFS traversal throughput under various seed/limit/radius
 * combinations, as well as the distance-calculation hot path. All benchmarks
 * use a mocked {@link Level} that returns a simple mock {@link BlockState}
 * to avoid Minecraft's static initializers.</p>
 */
class RtsUltimineCollectorBenchmark {

    private static final int WARMUP = 3;
    private static final int ITERATIONS = 10;

    private Level level;
    private RtsUltimineCollector.CandidateFilter acceptAll;

    /**
     * Global JIT warmup — exercises BFS at various scales.
     */
    @BeforeAll
    static void globalWarmUp() {
        Level mockLevel = createMockLevel();
        RtsUltimineCollector.CandidateFilter filter =
                (pos, state, seedState) -> !state.isAir();

        // Warm up small collects
        for (int i = 0; i < 50; i++) {
            RtsUltimineCollector.collect(mockLevel, new BlockPos(0, 0, 0), 8, filter);
        }
        // Warm up medium collects
        for (int i = 0; i < 20; i++) {
            RtsUltimineCollector.collect(mockLevel, new BlockPos(0, 0, 0), 64, filter);
        }
        // Warm up large collects
        for (int i = 0; i < 5; i++) {
            RtsUltimineCollector.collect(mockLevel, new BlockPos(0, 0, 0), 256, filter);
        }
        // Warm up bounded radius
        for (int i = 0; i < 10; i++) {
            RtsUltimineCollector.collect(mockLevel, new BlockPos(0, 0, 0), 256, 8, filter);
        }
        // Warm up tight radius
        for (int i = 0; i < 10; i++) {
            RtsUltimineCollector.collect(mockLevel, new BlockPos(0, 0, 0), 64, 3, filter);
        }
    }

    @BeforeEach
    void setUp() {
        System.gc();
        level = createMockLevel();
        acceptAll = (pos, state, seedState) -> !state.isAir();
    }

    // ======================================================================
    //  Section 1: Small collect (limit=8) — typical vein mining
    // ======================================================================

    @Test
    void benchmarkCollectSmall() {
        int totalNanos = 0;
        for (int iter = 0; iter < ITERATIONS * 100; iter++) {
            long start = System.nanoTime();
            RtsUltimineCollector.collect(level, new BlockPos(0, 0, 0), 8, acceptAll);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / (ITERATIONS * 100);
        System.out.println(String.format("[RtsUltimineCollector] collect(limit=8): avg %,d ns/op  (%,.0f ops/sec)",
                avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    //  Section 2: Medium collect (limit=64) — large vein mining
    // ======================================================================

    @Test
    void benchmarkCollectMedium() {
        long totalNanos = 0;
        for (int iter = 0; iter < ITERATIONS * 10; iter++) {
            long start = System.nanoTime();
            RtsUltimineCollector.collect(level, new BlockPos(0, 0, 0), 64, acceptAll);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / (ITERATIONS * 10);
        System.out.println(String.format("[RtsUltimineCollector] collect(limit=64): avg %,d ns/op  (%,.0f ops/sec)",
                avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    //  Section 3: Large collect (limit=256) — ultimine limit reached
    // ======================================================================

    @Test
    void benchmarkCollectLarge() {
        long totalNanos = 0;
        for (int iter = 0; iter < ITERATIONS; iter++) {
            long start = System.nanoTime();
            RtsUltimineCollector.collect(level, new BlockPos(0, 0, 0), 256, acceptAll);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / ITERATIONS;
        System.out.println(String.format("[RtsUltimineCollector] collect(limit=256): avg %,d ns/op  (%,.0f ops/sec)",
                avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    //  Section 4: Bounded radius (radius=8) — constrained exploration
    // ======================================================================

    @Test
    void benchmarkCollectBoundedRadius() {
        long totalNanos = 0;
        for (int iter = 0; iter < ITERATIONS; iter++) {
            long start = System.nanoTime();
            RtsUltimineCollector.collect(level, new BlockPos(0, 0, 0), 256, 8, acceptAll);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / ITERATIONS;
        System.out.println(String.format("[RtsUltimineCollector] collect(limit=256, radius=8): avg %,d ns/op  (%,.0f ops/sec)",
                avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    //  Section 5: Tight radius (radius=3) — small exploration zone
    // ======================================================================

    @Test
    void benchmarkCollectTightRadius() {
        long totalNanos = 0;
        for (int iter = 0; iter < ITERATIONS * 10; iter++) {
            long start = System.nanoTime();
            RtsUltimineCollector.collect(level, new BlockPos(0, 0, 0), 64, 3, acceptAll);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / (ITERATIONS * 10);
        System.out.println(String.format("[RtsUltimineCollector] collect(limit=64, radius=3): avg %,d ns/op  (%,.0f ops/sec)",
                avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    //  Section 6: Limit=1 (fast path — seed block only)
    // ======================================================================

    @Test
    void benchmarkCollectSingle() {
        long totalNanos = 0;
        int CALLS = ITERATIONS * 1000;

        for (int i = 0; i < CALLS; i++) {
            long start = System.nanoTime();
            RtsUltimineCollector.collect(level, new BlockPos(0, 0, 0), 1, acceptAll);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[RtsUltimineCollector] collect(limit=1): avg %,d ns/op  (%,.0f ops/sec)",
                avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    //  Section 7: Null guard — null level/seed/filter
    // ======================================================================

    @Test
    void benchmarkNullGuard() {
        int CALLS = ITERATIONS * 100_000;

        long start = System.nanoTime();
        for (int i = 0; i < CALLS; i++) {
            RtsUltimineCollector.collect(null, new BlockPos(0, 0, 0), 64, acceptAll);
            RtsUltimineCollector.collect(level, null, 64, acceptAll);
            RtsUltimineCollector.collect(level, new BlockPos(0, 0, 0), 0, acceptAll);
            RtsUltimineCollector.collect(level, new BlockPos(0, 0, 0), 64, null);
        }
        long end = System.nanoTime();
        long avgNanos = (end - start) / (CALLS * 4);
        System.out.println(String.format("[RtsUltimineCollector] null guards \u00d7 %,d: avg %,d ns/op",
                CALLS * 4, avgNanos));
    }

    // ======================================================================
    //  Section 8: Rejection filter — no blocks match, BFS hits visited only
    // ======================================================================

    @Test
    void benchmarkCollectRejectAll() {
        RtsUltimineCollector.CandidateFilter rejectAll =
                (pos, state, seedState) -> false;
        long totalNanos = 0;
        int CALLS = ITERATIONS * 100;

        for (int iter = 0; iter < CALLS; iter++) {
            long start = System.nanoTime();
            RtsUltimineCollector.collect(level, new BlockPos(0, 0, 0), 256, rejectAll);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[RtsUltimineCollector] collect(reject all, limit=256): avg %,d ns/op  (%,.0f ops/sec)",
                avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    //  Section 9: Random seed positions — cache/miss patterns
    // ======================================================================

    @Test
    void benchmarkCollectRandomSeeds() {
        int CALLS = ITERATIONS * 100;
        BlockPos[] seeds = new BlockPos[100];
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < seeds.length; i++) {
            seeds[i] = new BlockPos(rng.nextInt(16), rng.nextInt(16), rng.nextInt(16));
        }

        long totalNanos = 0;
        for (int iter = 0; iter < CALLS; iter++) {
            long start = System.nanoTime();
            RtsUltimineCollector.collect(level, seeds[iter % seeds.length], 64, acceptAll);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[RtsUltimineCollector] collect(random seeds, limit=64): avg %,d ns/op  (%,.0f ops/sec)",
                avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    //  Helpers
    // ======================================================================

    private static Level createMockLevel() {
        Level mockLevel = mock(Level.class);
        BlockState mockState = mock(BlockState.class);
        when(mockState.isAir()).thenReturn(false);
        when(mockLevel.getBlockState(any(BlockPos.class))).thenReturn(mockState);
        return mockLevel;
    }
}

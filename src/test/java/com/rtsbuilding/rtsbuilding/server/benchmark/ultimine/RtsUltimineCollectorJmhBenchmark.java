package com.rtsbuilding.rtsbuilding.server.benchmark.ultimine;

import com.rtsbuilding.rtsbuilding.common.RtsUltimineCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

/**
 * JMH microbenchmarks for {@link RtsUltimineCollector}.
 *
 * <p>Covers BFS traversal at various limits and radii, rejection filters,
 * random seeds, and null-guard fast paths. Uses mocked {@link Level} and
 * {@link BlockState} to avoid Minecraft's static initializers.</p>
 */
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class RtsUltimineCollectorJmhBenchmark {

    private Level level;
    private RtsUltimineCollector.CandidateFilter acceptAll;

    @Setup(org.openjdk.jmh.annotations.Level.Trial)
    public void globalSetup() {
        level = createMockLevel();
        acceptAll = (pos, state, seedState) -> !state.isAir();
    }

    // ======================================================================
    //  Small collect (limit=8)
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void collectSmall(Blackhole bh) {
        bh.consume(RtsUltimineCollector.collect(level, BlockPos.ZERO, 8, acceptAll));
    }

    // ======================================================================
    //  Medium collect (limit=64)
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void collectMedium(Blackhole bh) {
        bh.consume(RtsUltimineCollector.collect(level, BlockPos.ZERO, 64, acceptAll));
    }

    // ======================================================================
    //  Large collect (limit=256)
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void collectLarge(Blackhole bh) {
        bh.consume(RtsUltimineCollector.collect(level, BlockPos.ZERO, 256, acceptAll));
    }

    // ======================================================================
    //  Bounded radius (radius=8)
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void collectBoundedRadius(Blackhole bh) {
        bh.consume(RtsUltimineCollector.collect(level, BlockPos.ZERO, 256, 8, acceptAll));
    }

    // ======================================================================
    //  Tight radius (radius=3)
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void collectTightRadius(Blackhole bh) {
        bh.consume(RtsUltimineCollector.collect(level, BlockPos.ZERO, 64, 3, acceptAll));
    }

    // ======================================================================
    //  Limit=1 (fast path — seed block only)
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void collectSingle(Blackhole bh) {
        bh.consume(RtsUltimineCollector.collect(level, BlockPos.ZERO, 1, acceptAll));
    }

    // ======================================================================
    //  Rejection filter — no blocks match, BFS explores visited only
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void collectRejectAll(Blackhole bh) {
        RtsUltimineCollector.CandidateFilter rejectAll = (pos, state, seedState) -> false;
        bh.consume(RtsUltimineCollector.collect(level, BlockPos.ZERO, 256, rejectAll));
    }

    // ======================================================================
    //  Null guard fast path
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void collectNullLevel(Blackhole bh) {
        bh.consume(RtsUltimineCollector.collect(null, BlockPos.ZERO, 64, acceptAll));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void collectNullSeed(Blackhole bh) {
        bh.consume(RtsUltimineCollector.collect(level, null, 64, acceptAll));
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

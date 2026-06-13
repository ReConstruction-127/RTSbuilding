package com.rtsbuilding.rtsbuilding.server.benchmark.context;

import com.rtsbuilding.rtsbuilding.server.util.TemporaryContextSwitcher;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * JMH microbenchmarks for {@link TemporaryContextSwitcher}.
 *
 * <p>Covers the vector math hot paths: valid ray parsing, NaN/infinity
 * fast-fail, zero-direction rejection, axis-aligned directions, and
 * mixed valid/invalid call patterns. All benchmarks are pure Java math
 * with no Minecraft dependencies.</p>
 */
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class TemporaryContextSwitcherJmhBenchmark {

    // ======================================================================
    //  Valid ray context — Vec3 construction + normalize + null check
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void parseValid(Blackhole bh) {
        bh.consume(TemporaryContextSwitcher.parseRayContext(10, 20, 30, 1, -1, 0.5));
    }

    // ======================================================================
    //  NaN origin fast-fail
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void parseNaN(Blackhole bh) {
        bh.consume(TemporaryContextSwitcher.parseRayContext(Double.NaN, 0, 0, 0, -1, 0));
    }

    // ======================================================================
    //  Zero-direction fast-fail
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void parseZeroDir(Blackhole bh) {
        bh.consume(TemporaryContextSwitcher.parseRayContext(0, 0, 0, 0, 0, 0));
    }

    // ======================================================================
    //  Infinity origin fast-fail
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void parseInfinity(Blackhole bh) {
        bh.consume(TemporaryContextSwitcher.parseRayContext(Double.POSITIVE_INFINITY, 0, 0, 0, -1, 0));
    }

    // ======================================================================
    //  Axis-aligned direction — most common in practice
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void parseAxisAligned(Blackhole bh) {
        bh.consume(TemporaryContextSwitcher.parseRayContext(0, 0, 0, 0, -1, 0));
        bh.consume(TemporaryContextSwitcher.parseRayContext(10, 20, 30, 1, 0, 0));
        bh.consume(TemporaryContextSwitcher.parseRayContext(-5, 10, 3, 0, 0, 1));
    }

    // ======================================================================
    //  Mixed valid/invalid — 90% valid, 10% zero-dir
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void parseMixed(MixedInputState state, Blackhole bh) {
        double[] in = state.inputs[state.counter.getAndIncrement() % state.inputs.length];
        bh.consume(TemporaryContextSwitcher.parseRayContext(in[0], in[1], in[2], in[3], in[4], in[5]));
    }

    @State(Scope.Thread)
    public static class MixedInputState {
        double[][] inputs;
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();

        @Setup(Level.Trial)
        public void setup() {
            inputs = new double[100][6];
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            for (int i = 0; i < inputs.length; i++) {
                inputs[i][0] = rng.nextDouble() * 100;
                inputs[i][1] = rng.nextDouble() * 100;
                inputs[i][2] = rng.nextDouble() * 100;
                inputs[i][3] = rng.nextDouble() * 2 - 1;
                inputs[i][4] = rng.nextDouble() * 2 - 1;
                inputs[i][5] = rng.nextDouble() * 2 - 1;
                if (i % 10 == 0) {
                    inputs[i][3] = 0;
                    inputs[i][4] = 0;
                    inputs[i][5] = 0;
                }
            }
        }
    }
}

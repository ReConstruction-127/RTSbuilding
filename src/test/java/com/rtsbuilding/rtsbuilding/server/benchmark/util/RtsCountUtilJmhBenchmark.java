package com.rtsbuilding.rtsbuilding.server.benchmark.util;

import com.rtsbuilding.rtsbuilding.util.RtsCountUtil;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JMH microbenchmarks for {@link RtsCountUtil}.
 *
 * <p>Covers sanitizeCount, saturatedAdd (normal/overflow/negative),
 * and mergeCount (new key/same key/guard fast paths). All benchmarks
 * are pure arithmetic with zero Minecraft dependencies.</p>
 */
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class RtsCountUtilJmhBenchmark {

    // ======================================================================
    // sanitizeCount
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void sanitizeCount(Blackhole bh) {
        for (int i = 0; i < 1_000_000; i++) {
            bh.consume(RtsCountUtil.sanitizeCount(i - 500_000));
        }
    }

    // ======================================================================
    // saturatedAdd — normal
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void saturatedAddNormal(Blackhole bh) {
        for (int i = 0; i < 1_000_000; i++) {
            bh.consume(RtsCountUtil.saturatedAdd(i, i + 1));
        }
    }

    // ======================================================================
    // saturatedAdd — overflow edge
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void saturatedAddOverflow(Blackhole bh) {
        for (int i = 0; i < 1_000_000; i++) {
            bh.consume(RtsCountUtil.saturatedAdd(Long.MAX_VALUE - (i % 100), (long) (i % 200)));
        }
    }

    // ======================================================================
    // saturatedAdd — both negative (sanitized to 0)
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void saturatedAddNegative(Blackhole bh) {
        for (int i = 0; i < 1_000_000; i++) {
            bh.consume(RtsCountUtil.saturatedAdd(-(i % 100), -(i % 50)));
        }
    }

    // ======================================================================
    // mergeCount — new keys
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Warmup(iterations = 1)
    @Measurement(iterations = 3)
    @OperationsPerInvocation(100_000)
    public void mergeCountNewKeys() {
        Map<String, Long> map = new HashMap<>();
        for (int i = 0; i < 100_000; i++) {
            RtsCountUtil.mergeCount(map, "item_" + i, (long) i);
        }
    }

    // ======================================================================
    // mergeCount — same key
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Warmup(iterations = 1)
    @Measurement(iterations = 3)
    @OperationsPerInvocation(100_000)
    public void mergeCountSameKey() {
        Map<String, Long> map = new HashMap<>();
        for (int i = 0; i < 100_000; i++) {
            RtsCountUtil.mergeCount(map, "minecraft:diamond", 1L);
        }
    }

    // ======================================================================
    // mergeCount — null guard fast path
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void mergeCountGuards(Blackhole bh) {
        Map<String, Long> map = new HashMap<>();
        for (int i = 0; i < 100_000; i++) {
            RtsCountUtil.mergeCount(null, "key", 10L);
            RtsCountUtil.mergeCount(map, null, 10L);
            RtsCountUtil.mergeCount(map, "", 10L);
            RtsCountUtil.mergeCount(map, "key", 0L);
        }
        bh.consume(map.size());
    }
}

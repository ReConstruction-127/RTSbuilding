package com.rtsbuilding.rtsbuilding.server.benchmark.util;

import com.rtsbuilding.rtsbuilding.util.RtsCountUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Extreme Performance Benchmarks for {@link RtsCountUtil}.
 *
 * <p>Pure arithmetic and map-merge throughput. All benchmarks are pure
 * Java with zero Minecraft dependencies.</p>
 */
class RtsCountUtilBenchmark {

    private static final int WARMUP = 3;
    private static final int ITERATIONS = 10;

    private Map<String, Long> counts;

    @BeforeAll
    static void globalWarmUp() {
        // Warm up sanitizeCount
        for (int i = 0; i < 100_000; i++) {
            RtsCountUtil.sanitizeCount(i - 50_000);
            RtsCountUtil.sanitizeCount(Long.MAX_VALUE);
            RtsCountUtil.sanitizeCount(0);
        }

        // Warm up saturatedAdd
        for (int i = 0; i < 100_000; i++) {
            RtsCountUtil.saturatedAdd(i, i + 1);
            RtsCountUtil.saturatedAdd(Long.MAX_VALUE, 1);
            RtsCountUtil.saturatedAdd(-1, 10);
        }

        // Warm up mergeCount
        Map<String, Long> map = new HashMap<>();
        for (int i = 0; i < 50_000; i++) {
            RtsCountUtil.mergeCount(map, "item_" + (i % 1000), (long) i);
        }
    }

    @BeforeEach
    void setUp() {
        System.gc();
        counts = new HashMap<>();
    }

    // ======================================================================
    // sanitizeCount
    // ======================================================================

    @Test
    void benchmarkSanitizeCount() {
        int CALLS = 10_000_000;

        long totalNanos = 0;
        long result = 0;
        for (int i = 0; i < CALLS; i++) {
            long start = System.nanoTime();
            result += RtsCountUtil.sanitizeCount(i - 5_000_000);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[RtsCountUtil] sanitizeCount() \u00d7 %,d: avg %,d ns/op  (%,.0f ops/sec)",
                CALLS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    // saturatedAdd — normal case
    // ======================================================================

    @Test
    void benchmarkSaturatedAddNormal() {
        int CALLS = 10_000_000;

        long totalNanos = 0;
        for (int i = 0; i < CALLS; i++) {
            long start = System.nanoTime();
            RtsCountUtil.saturatedAdd(i, i + 1);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[RtsCountUtil] saturatedAdd(normal) \u00d7 %,d: avg %,d ns/op  (%,.0f ops/sec)",
                CALLS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    // saturatedAdd — overflow / MAX_VALUE branch
    // ======================================================================

    @Test
    void benchmarkSaturatedAddOverflow() {
        int CALLS = 10_000_000;

        long totalNanos = 0;
        for (int i = 0; i < CALLS; i++) {
            long start = System.nanoTime();
            RtsCountUtil.saturatedAdd(Long.MAX_VALUE - (i % 100), (long) (i % 200));
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[RtsCountUtil] saturatedAdd(overflow edge) \u00d7 %,d: avg %,d ns/op  (%,.0f ops/sec)",
                CALLS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    // saturatedAdd — negative input (sanitize to 0)
    // ======================================================================

    @Test
    void benchmarkSaturatedAddNegative() {
        int CALLS = 10_000_000;

        long totalNanos = 0;
        for (int i = 0; i < CALLS; i++) {
            long start = System.nanoTime();
            RtsCountUtil.saturatedAdd(-(i % 100), -(i % 50));
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[RtsCountUtil] saturatedAdd(negative) \u00d7 %,d: avg %,d ns/op  (%,.0f ops/sec)",
                CALLS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    // mergeCount — new keys
    // ======================================================================

    @Test
    void benchmarkMergeCountNewKeys() {
        int CALLS = 100_000;
        String[] keys = new String[CALLS];
        for (int i = 0; i < CALLS; i++) keys[i] = "item_" + i;

        long totalNanos = 0;
        for (int iter = 0; iter < ITERATIONS; iter++) {
            Map<String, Long> map = new HashMap<>();
            long start = System.nanoTime();
            for (int i = 0; i < CALLS; i++) {
                RtsCountUtil.mergeCount(map, keys[i], (long) i);
            }
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / ITERATIONS;
        System.out.println(String.format("[RtsCountUtil] mergeCount(new keys, %,d ops): avg %,d ns total, ~%,d ns/op",
                CALLS, avgNanos, avgNanos / CALLS));
    }

    // ======================================================================
    // mergeCount — same key (merge with existing)
    // ======================================================================

    @Test
    void benchmarkMergeCountSameKey() {
        int CALLS = 100_000;

        long totalNanos = 0;
        for (int iter = 0; iter < ITERATIONS; iter++) {
            Map<String, Long> map = new HashMap<>();
            long start = System.nanoTime();
            for (int i = 0; i < CALLS; i++) {
                RtsCountUtil.mergeCount(map, "minecraft:diamond", 1L);
            }
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / ITERATIONS;
        System.out.println(String.format("[RtsCountUtil] mergeCount(same key, %,d ops): avg %,d ns total, ~%,d ns/op",
                CALLS, avgNanos, avgNanos / CALLS));
    }

    // ======================================================================
    // mergeCount — null/blank fast path
    // ======================================================================

    @Test
    void benchmarkMergeCountNullGuard() {
        int CALLS = 1_000_000;

        long totalNanos = 0;
        Map<String, Long> map = new HashMap<>();
        for (int i = 0; i < CALLS; i++) {
            long start = System.nanoTime();
            RtsCountUtil.mergeCount(null, "key", 10L);
            RtsCountUtil.mergeCount(map, null, 10L);
            RtsCountUtil.mergeCount(map, "", 10L);
            RtsCountUtil.mergeCount(map, "key", 0L);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / (CALLS * 4);
        System.out.println(String.format("[RtsCountUtil] mergeCount(guards) \u00d7 %,d: avg %,d ns/op",
                CALLS * 4, avgNanos));
    }
}

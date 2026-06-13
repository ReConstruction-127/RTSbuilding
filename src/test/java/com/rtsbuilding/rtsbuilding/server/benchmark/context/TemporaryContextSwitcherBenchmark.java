package com.rtsbuilding.rtsbuilding.server.benchmark.context;

import com.rtsbuilding.rtsbuilding.server.util.TemporaryContextSwitcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Extreme Performance Benchmarks for {@link TemporaryContextSwitcher}.
 *
 * <p>Focuses on the vector math hot paths: {@code parseRayContext} and
 * the internal yaw/pitch calculation. All benchmarks are pure Java math
 * with no Minecraft dependencies.</p>
 */
class TemporaryContextSwitcherBenchmark {

    private static final int WARMUP = 3;
    private static final int ITERATIONS = 10;

    // ======================================================================
    //  Section 1: parseRayContext with valid inputs
    // ======================================================================

    @Test
    void benchmarkParseRayContextValid() {
        int CALLS = ITERATIONS * 100_000;

        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < CALLS / 10; i++) {
                TemporaryContextSwitcher.parseRayContext(0, 0, 0, 0, -1, 0);
                TemporaryContextSwitcher.parseRayContext(10, 20, 30, 1, 1, 1);
                TemporaryContextSwitcher.parseRayContext(-5, 10, 3, -0.5, 0.5, 0);
            }
        }

        long totalNanos = 0;
        for (int i = 0; i < CALLS; i++) {
            double ox = i * 0.1;
            double oy = i * 0.2;
            double oz = i * 0.3;
            double dx = 1.0;
            double dy = -1.0;
            double dz = 0.5;
            long start = System.nanoTime();
            TemporaryContextSwitcher.parseRayContext(ox, oy, oz, dx, dy, dz);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[TemporaryContextSwitcher] parseRayContext(valid) \u00d7 %,d: avg %,d ns/op  (%,.0f ops/sec)",
                CALLS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    //  Section 2: parseRayContext with NaN inputs (fast-fail path)
    // ======================================================================

    @Test
    void benchmarkParseRayContextNaN() {
        int CALLS = ITERATIONS * 100_000;

        long totalNanos = 0;
        for (int i = 0; i < CALLS; i++) {
            long start = System.nanoTime();
            TemporaryContextSwitcher.parseRayContext(Double.NaN, 0, 0, 0, -1, 0);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[TemporaryContextSwitcher] parseRayContext(NaN origin) \u00d7 %,d: avg %,d ns/op  (%,.0f ops/sec)",
                CALLS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    //  Section 3: parseRayContext with zero-direction vector (fast-fail)
    // ======================================================================

    @Test
    void benchmarkParseRayContextZeroDir() {
        int CALLS = ITERATIONS * 100_000;

        long totalNanos = 0;
        for (int i = 0; i < CALLS; i++) {
            long start = System.nanoTime();
            TemporaryContextSwitcher.parseRayContext(0, 0, 0, 0, 0, 0);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[TemporaryContextSwitcher] parseRayContext(zero dir) \u00d7 %,d: avg %,d ns/op  (%,.0f ops/sec)",
                CALLS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    //  Section 4: parseRayContext with infinite values
    // ======================================================================

    @Test
    void benchmarkParseRayContextInfinity() {
        int CALLS = ITERATIONS * 100_000;

        long totalNanos = 0;
        for (int i = 0; i < CALLS; i++) {
            long start = System.nanoTime();
            TemporaryContextSwitcher.parseRayContext(Double.POSITIVE_INFINITY, 0, 0, 0, -1, 0);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[TemporaryContextSwitcher] parseRayContext(Infinity origin) \u00d7 %,d: avg %,d ns/op  (%,.0f ops/sec)",
                CALLS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    //  Section 5: parseRayContext — mixed valid/invalid (realistic call pattern)
    // ======================================================================

    @Test
    void benchmarkParseRayContextMixed() {
        int CALLS = ITERATIONS * 50_000;
        double[][] inputs = new double[100][6];
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < inputs.length; i++) {
            inputs[i][0] = rng.nextDouble() * 100;
            inputs[i][1] = rng.nextDouble() * 100;
            inputs[i][2] = rng.nextDouble() * 100;
            inputs[i][3] = rng.nextDouble() * 2 - 1;
            inputs[i][4] = rng.nextDouble() * 2 - 1;
            inputs[i][5] = rng.nextDouble() * 2 - 1;
            // Sprinkle in some invalid inputs
            if (i % 10 == 0) {
                inputs[i][3] = 0;
                inputs[i][4] = 0;
                inputs[i][5] = 0;
            }
        }

        long totalNanos = 0;
        for (int i = 0; i < CALLS; i++) {
            double[] in = inputs[i % inputs.length];
            long start = System.nanoTime();
            TemporaryContextSwitcher.parseRayContext(in[0], in[1], in[2], in[3], in[4], in[5]);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[TemporaryContextSwitcher] parseRayContext(mixed 90%% valid / 10%% zero-dir) \u00d7 %,d: avg %,d ns/op  (%,.0f ops/sec)",
                CALLS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    //  Section 6: parseRayContext — normalized direction (most common path)
    // ======================================================================

    @Test
    void benchmarkParseRayContextNormalized() {
        int CALLS = ITERATIONS * 100_000;
        double[][] dirs = {
                {0, -1, 0},   // straight down
                {0, 1, 0},    // straight up
                {1, 0, 0},    // east
                {-1, 0, 0},   // west
                {0, 0, 1},    // south
                {0, 0, -1},   // north
                {0.577, -0.577, 0.577}, // diagonal
                {-0.707, 0, 0.707},     // diagonal
        };

        long totalNanos = 0;
        for (int i = 0; i < CALLS; i++) {
            double[] dir = dirs[i % dirs.length];
            long start = System.nanoTime();
            TemporaryContextSwitcher.parseRayContext(0, 0, 0, dir[0], dir[1], dir[2]);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[TemporaryContextSwitcher] parseRayContext(axis-aligned dirs) \u00d7 %,d: avg %,d ns/op  (%,.0f ops/sec)",
                CALLS, avgNanos, 1_000_000_000.0 / avgNanos));
    }
}

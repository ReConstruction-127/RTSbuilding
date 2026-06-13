package com.rtsbuilding.rtsbuilding.server.benchmark.history;

import com.rtsbuilding.rtsbuilding.server.history.HistoryBlockRecord;
import com.rtsbuilding.rtsbuilding.server.history.HistoryEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Extreme Performance Benchmarks for {@link HistoryEntry}.
 *
 * <p>Focuses on construction, removeRestored, expiry checks, and bulk
 * operations under various sizes. All benchmarks use Minecraft's concrete
 * classes (BlockPos, BlockState) that are safe to instantiate without a
 * bootstrapped runtime.</p>
 */
class HistoryEntryBenchmark {

    private static final int WARMUP = 3;
    private static final int ITERATIONS = 10;
    private static final ResourceKey<Level> DIM = Level.OVERWORLD;

    private static HistoryBlockRecord[] records;
    private static HistoryBlockRecord[] largeRecords;

    @BeforeAll
    static void globalWarmUp() {
        records = generateRecords(100);
        largeRecords = generateRecords(10_000);

        // Warm up construction
        for (int i = 0; i < 10_000; i++) {
            new HistoryEntry(true, List.of(records[i % records.length]), Direction.UP, DIM);
        }

        // Warm up removeRestored
        for (int i = 0; i < 1000; i++) {
            HistoryEntry entry = new HistoryEntry(true, List.of(largeRecords), Direction.UP, DIM);
            entry.removeRestored(i % 100);
        }

        // Warm up getBlockCount
        for (int i = 0; i < 10_000; i++) {
            HistoryEntry entry = new HistoryEntry(true, List.of(records), Direction.UP, DIM);
            entry.getBlockCount();
        }
    }

    @BeforeEach
    void setUp() {
        System.gc();
    }

    // ======================================================================
    // Construction — small (1 block)
    // ======================================================================

    @Test
    void benchmarkConstructSingle() {
        int CALLS = ITERATIONS * 100_000;
        List<HistoryBlockRecord> single = List.of(records[0]);

        long totalNanos = 0;
        for (int i = 0; i < CALLS; i++) {
            long start = System.nanoTime();
            new HistoryEntry(true, single, Direction.UP, DIM);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[HistoryEntry] construct(single) \u00d7 %,d: avg %,d ns/op  (%,.0f ops/sec)",
                CALLS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    // Construction — medium (100 blocks)
    // ======================================================================

    @Test
    void benchmarkConstructMedium() {
        int CALLS = ITERATIONS * 10_000;
        List<HistoryBlockRecord> ms = List.of(records);

        long totalNanos = 0;
        for (int i = 0; i < CALLS; i++) {
            long start = System.nanoTime();
            new HistoryEntry(false, ms, Direction.DOWN, DIM);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[HistoryEntry] construct(100 blocks) \u00d7 %,d: avg %,d ns/op  (%,.0f ops/sec)",
                CALLS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    // Construction — large (10K blocks)
    // ======================================================================

    @Test
    void benchmarkConstructLarge() {
        int CALLS = ITERATIONS * 100;
        List<HistoryBlockRecord> ls = List.of(largeRecords);

        long totalNanos = 0;
        for (int i = 0; i < CALLS; i++) {
            long start = System.nanoTime();
            new HistoryEntry(true, ls, Direction.EAST, DIM);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[HistoryEntry] construct(10K blocks) \u00d7 %,d: avg %,d ns/op  (%,.0f ops/sec)",
                CALLS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    // removeRestored — partial, small record set
    // ======================================================================

    @Test
    void benchmarkRemoveRestoredPartial() {
        int CALLS = ITERATIONS * 10_000;
        List<HistoryBlockRecord> rs = List.of(records);

        long totalNanos = 0;
        for (int i = 0; i < CALLS; i++) {
            HistoryEntry entry = new HistoryEntry(true, rs, Direction.UP, DIM);
            long start = System.nanoTime();
            entry.removeRestored(30);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[HistoryEntry] removeRestored(30/100) \u00d7 %,d: avg %,d ns/op  (%,.0f ops/sec)",
                CALLS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    // removeRestored — all removed (fast path: returns null)
    // ======================================================================

    @Test
    void benchmarkRemoveRestoredAll() {
        int CALLS = ITERATIONS * 100_000;

        long totalNanos = 0;
        for (int i = 0; i < CALLS; i++) {
            HistoryEntry entry = new HistoryEntry(true, List.of(records[0]), Direction.UP, DIM);
            long start = System.nanoTime();
            entry.removeRestored(1);
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[HistoryEntry] removeRestored(all) \u00d7 %,d: avg %,d ns/op  (%,.0f ops/sec)",
                CALLS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    // getBlockCount
    // ======================================================================

    @Test
    void benchmarkGetBlockCount() {
        int CALLS = ITERATIONS * 100_000;

        long totalNanos = 0;
        for (int i = 0; i < CALLS; i++) {
            HistoryEntry entry = new HistoryEntry(true, List.of(records[i % records.length]), Direction.UP, DIM);
            long start = System.nanoTime();
            entry.getBlockCount();
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[HistoryEntry] getBlockCount() \u00d7 %,d: avg %,d ns/op  (%,.0f ops/sec)",
                CALLS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    // isExpired — always false (recent)
    // ======================================================================

    @Test
    void benchmarkIsExpired() {
        int CALLS = ITERATIONS * 100_000;

        long totalNanos = 0;
        for (int i = 0; i < CALLS; i++) {
            HistoryEntry entry = new HistoryEntry(true, List.of(records[i % records.length]), Direction.UP, DIM);
            long start = System.nanoTime();
            entry.isExpired();
            long end = System.nanoTime();
            totalNanos += (end - start);
        }
        long avgNanos = totalNanos / CALLS;
        System.out.println(String.format("[HistoryEntry] isExpired() \u00d7 %,d: avg %,d ns/op  (%,.0f ops/sec)",
                CALLS, avgNanos, 1_000_000_000.0 / avgNanos));
    }

    // ======================================================================
    // Helpers
    // ======================================================================

    private static HistoryBlockRecord[] generateRecords(int count) {
        HistoryBlockRecord[] arr = new HistoryBlockRecord[count];
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            arr[i] = new HistoryBlockRecord(
                    new BlockPos(rng.nextInt(100), rng.nextInt(64), rng.nextInt(100)),
                    Blocks.STONE.defaultBlockState());
        }
        return arr;
    }
}

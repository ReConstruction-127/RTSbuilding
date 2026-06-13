package com.rtsbuilding.rtsbuilding.server.benchmark.history;

import com.rtsbuilding.rtsbuilding.server.history.HistoryBlockRecord;
import com.rtsbuilding.rtsbuilding.server.history.HistoryEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH microbenchmarks for {@link HistoryEntry}.
 *
 * <p>Covers construction at various sizes, removeRestored (partial/all),
 * getBlockCount, and isExpiry checks. All benchmarks use Minecraft's
 * concrete classes safe to instantiate without a bootstrapped runtime.</p>
 */
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class HistoryEntryJmhBenchmark {

    private static final ResourceKey<Level> DIM = Level.OVERWORLD;
    private static final HistoryBlockRecord SINGLE_RECORD =
            new HistoryBlockRecord(BlockPos.ZERO, Blocks.STONE.defaultBlockState());
    private static final List<HistoryBlockRecord> SINGLE = List.of(SINGLE_RECORD);

    // ======================================================================
    // Construction
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void constructSingle(Blackhole bh) {
        bh.consume(new HistoryEntry(true, SINGLE, Direction.UP, DIM));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void construct100(MediumState state, Blackhole bh) {
        bh.consume(new HistoryEntry(false, state.records100, Direction.UP, DIM));
    }

    @State(Scope.Thread)
    public static class MediumState {
        List<HistoryBlockRecord> records100;

        @Setup(org.openjdk.jmh.annotations.Level.Trial)
        public void setup() {
            HistoryBlockRecord[] arr = new HistoryBlockRecord[100];
            for (int i = 0; i < 100; i++) {
                arr[i] = new HistoryBlockRecord(new BlockPos(i, 0, 0), Blocks.STONE.defaultBlockState());
            }
            records100 = List.of(arr);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 1)
    @Measurement(iterations = 3)
    public void construct10000(LargeState state, Blackhole bh) {
        bh.consume(new HistoryEntry(true, state.records10000, Direction.UP, DIM));
    }

    @State(Scope.Thread)
    public static class LargeState {
        List<HistoryBlockRecord> records10000;

        @Setup(org.openjdk.jmh.annotations.Level.Trial)
        public void setup() {
            HistoryBlockRecord[] arr = new HistoryBlockRecord[10_000];
            for (int i = 0; i < 10_000; i++) {
                arr[i] = new HistoryBlockRecord(new BlockPos(i % 100, i / 100, 0), Blocks.STONE.defaultBlockState());
            }
            records10000 = List.of(arr);
        }
    }

    // ======================================================================
    // removeRestored
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void removeRestoredPartial(Blackhole bh) {
        HistoryEntry entry = new HistoryEntry(true, SINGLE, Direction.UP, DIM);
        bh.consume(entry.removeRestored(0));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void removeRestoredAll(Blackhole bh) {
        HistoryEntry entry = new HistoryEntry(true, SINGLE, Direction.UP, DIM);
        bh.consume(entry.removeRestored(1));
    }

    // ======================================================================
    // getters
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void getBlockCount(Blackhole bh) {
        bh.consume(new HistoryEntry(true, SINGLE, Direction.UP, DIM).getBlockCount());
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void isExpired(Blackhole bh) {
        bh.consume(new HistoryEntry(true, SINGLE, Direction.UP, DIM).isExpired());
    }
}

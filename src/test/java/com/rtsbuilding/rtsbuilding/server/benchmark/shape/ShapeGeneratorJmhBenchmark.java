package com.rtsbuilding.rtsbuilding.server.benchmark.shape;

import com.rtsbuilding.rtsbuilding.common.shape.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * JMH microbenchmarks for all area shape generators.
 *
 * <p>Covers BOX (FILL/HOLLOW), SQUARE, CIRCLE, WALL, and LINE at
 * various sizes. All position generation is pure BlockPos math with
 * no Minecraft runtime dependencies.</p>
 */
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ShapeGeneratorJmhBenchmark {

    // Shared stateless generators
    private static final AreaShapeGenerator BOX_GEN = new BoxShapeGenerator();
    private static final AreaShapeGenerator SQUARE_GEN = new SquareShapeGenerator();
    private static final AreaShapeGenerator CIRCLE_GEN = new CircleShapeGenerator();
    private static final AreaShapeGenerator WALL_GEN = new WallShapeGenerator();
    private static final AreaShapeGenerator LINE_GEN = new LineShapeGenerator();

    private static final BlockPos ORIGIN = BlockPos.ZERO;

    // ======================================================================
    // BOX
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void boxSmallFill(Blackhole bh) {
        bh.consume(BOX_GEN.generatePositions(
                new AreaShapeInput(ORIGIN, new BlockPos(8, 0, 8), 8, Direction.UP, Direction.UP),
                ShapeFillMode.FILL));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void boxMediumFill(Blackhole bh) {
        bh.consume(BOX_GEN.generatePositions(
                new AreaShapeInput(ORIGIN, new BlockPos(32, 0, 32), 32, Direction.UP, Direction.UP),
                ShapeFillMode.FILL));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 1)
    @Measurement(iterations = 3)
    public void boxLargeFill(Blackhole bh) {
        bh.consume(BOX_GEN.generatePositions(
                new AreaShapeInput(ORIGIN, new BlockPos(64, 0, 64), 64, Direction.UP, Direction.UP),
                ShapeFillMode.FILL));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void boxMediumHollow(Blackhole bh) {
        bh.consume(BOX_GEN.generatePositions(
                new AreaShapeInput(ORIGIN, new BlockPos(32, 0, 32), 32, Direction.UP, Direction.UP),
                ShapeFillMode.HOLLOW));
    }

    // ======================================================================
    // SQUARE
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void squareMediumFill(Blackhole bh) {
        bh.consume(SQUARE_GEN.generatePositions(
                new AreaShapeInput(ORIGIN, new BlockPos(32, 0, 32), 0, Direction.UP, Direction.UP),
                ShapeFillMode.FILL));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void squareLargeHollow(Blackhole bh) {
        bh.consume(SQUARE_GEN.generatePositions(
                new AreaShapeInput(ORIGIN, new BlockPos(64, 0, 64), 0, Direction.UP, Direction.UP),
                ShapeFillMode.HOLLOW));
    }

    // ======================================================================
    // CIRCLE
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void circleSmallFill(Blackhole bh) {
        bh.consume(CIRCLE_GEN.generatePositions(
                AreaShapeInput.of(ORIGIN, new BlockPos(8, 0, 8)),
                ShapeFillMode.FILL));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void circleLargeFill(Blackhole bh) {
        bh.consume(CIRCLE_GEN.generatePositions(
                AreaShapeInput.of(ORIGIN, new BlockPos(32, 0, 32)),
                ShapeFillMode.FILL));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void circleHollow(Blackhole bh) {
        bh.consume(CIRCLE_GEN.generatePositions(
                AreaShapeInput.of(ORIGIN, new BlockPos(32, 0, 32)),
                ShapeFillMode.HOLLOW));
    }

    // ======================================================================
    // WALL
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void wallMediumFill(Blackhole bh) {
        bh.consume(WALL_GEN.generatePositions(
                AreaShapeInput.of(ORIGIN, new BlockPos(16, 0, 0), 16, Direction.UP, Direction.UP),
                ShapeFillMode.FILL));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void wallLargeHollow(Blackhole bh) {
        bh.consume(WALL_GEN.generatePositions(
                AreaShapeInput.of(ORIGIN, new BlockPos(32, 0, 0), 32, Direction.UP, Direction.UP),
                ShapeFillMode.HOLLOW));
    }

    // ======================================================================
    // LINE
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void lineLong(Blackhole bh) {
        bh.consume(LINE_GEN.generatePositions(
                AreaShapeInput.of(ORIGIN, new BlockPos(64, 64, 64)),
                ShapeFillMode.FILL));
    }
}

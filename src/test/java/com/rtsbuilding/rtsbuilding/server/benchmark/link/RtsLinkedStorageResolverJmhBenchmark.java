package com.rtsbuilding.rtsbuilding.server.benchmark.link;

import com.rtsbuilding.rtsbuilding.server.storage.LinkedStorageRef;
import com.rtsbuilding.rtsbuilding.server.storage.RtsLinkedStorageResolver;
import com.rtsbuilding.rtsbuilding.server.storage.RtsStorageSession;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;

/**
 * JMH microbenchmarks for {@link RtsLinkedStorageResolver}.
 *
 * <p>All methods under test are static and stateless — no per-fork
 * setup needed beyond mock session creation.</p>
 */
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class RtsLinkedStorageResolverJmhBenchmark {

    // ======================================================================
    //  buildLinkedSummary — various sizes
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void buildLinkedSummarySingle(Blackhole bh) {
        bh.consume(RtsLinkedStorageResolver.buildLinkedSummary(createMockSession(1)));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void buildLinkedSummaryTenRefs(Blackhole bh) {
        bh.consume(RtsLinkedStorageResolver.buildLinkedSummary(createMockSession(10)));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void buildLinkedSummaryThousandRefs(Blackhole bh) {
        bh.consume(RtsLinkedStorageResolver.buildLinkedSummary(createMockSession(1_000)));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void buildLinkedSummaryTenThousandRefs(Blackhole bh) {
        bh.consume(RtsLinkedStorageResolver.buildLinkedSummary(createMockSession(10_000)));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void buildLinkedSummaryEmpty(Blackhole bh) {
        bh.consume(RtsLinkedStorageResolver.buildLinkedSummary(createMockSession(0)));
    }

    // ======================================================================
    //  sanitizeLinkMode — 3-way branch
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void sanitizeLinkMode(Blackhole bh) {
        for (int i = 0; i < 10_000_000; i++) {
            bh.consume(RtsLinkedStorageResolver.sanitizeLinkMode((byte) (i % 3)));
        }
    }

    // ======================================================================
    //  sanitizeLinkedStoragePriority — Mth.clamp
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void sanitizePriorityBulk(Blackhole bh) {
        for (int i = 0; i < 10_000_000; i++) {
            int v = (i - 5_000_000) * 2;
            RtsLinkedStorageResolver.sanitizeLinkedStoragePriority(v);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void sanitizePriorityEdgeCases(Blackhole bh) {
        int[] values = {-100_000, -9999, 0, 500, 9999, 100_000};
        for (int i = 0; i < 1_000_000; i++) {
            RtsLinkedStorageResolver.sanitizeLinkedStoragePriority(values[i % values.length]);
        }
    }

    // ======================================================================
    //  isExtractOnlyLink — map lookup + sanitize branch
    // ======================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void isExtractOnlyLinkBulk(ExtractOnlyState state, Blackhole bh) {
        for (LinkedStorageRef ref : state.refs) {
            bh.consume(RtsLinkedStorageResolver.isExtractOnlyLink(state.session, ref));
        }
    }

    @State(Scope.Thread)
    public static class ExtractOnlyState {
        RtsStorageSession session;
        List<LinkedStorageRef> refs;

        @Setup(org.openjdk.jmh.annotations.Level.Trial)
        public void setup() {
            session = createMockSession(10_000);
            refs = getSessionRefs(session);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
    public void isExtractOnlyLinkNull(Blackhole bh) {
        bh.consume(RtsLinkedStorageResolver.isExtractOnlyLink(null, null));
    }

    // ======================================================================
    //  Helpers
    // ======================================================================

    @SuppressWarnings("unchecked")
    private static RtsStorageSession createMockSession(int refCount) {
        RtsStorageSession session = mock(RtsStorageSession.class);
        try {
            Field storagesField = RtsStorageSession.class.getDeclaredField("linkedStorages");
            storagesField.setAccessible(true);
            List<LinkedStorageRef> refs = new ArrayList<>(refCount);
            for (int i = 0; i < refCount; i++) {
                refs.add(createRef());
            }
            storagesField.set(session, refs);

            if (refCount > 0) {
                Field namesField = RtsStorageSession.class.getDeclaredField("linkedNames");
                namesField.setAccessible(true);
                Map<LinkedStorageRef, String> names = new HashMap<>();
                Map<LinkedStorageRef, Byte> modes = new HashMap<>();
                Random rng = ThreadLocalRandom.current();
                for (LinkedStorageRef ref : refs) {
                    names.put(ref, "Chest_" + rng.nextInt(10000));
                    modes.put(ref, rng.nextBoolean() ? (byte) 0 : (byte) 1);
                }
                namesField.set(session, names);

                Field modesField = RtsStorageSession.class.getDeclaredField("linkedModes");
                modesField.setAccessible(true);
                modesField.set(session, modes);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mock session", e);
        }
        return session;
    }

    @SuppressWarnings("unchecked")
    private static LinkedStorageRef createRef() {
        ResourceKey<Level> dim = (ResourceKey<Level>) mock(ResourceKey.class);
        return new LinkedStorageRef(dim, BlockPos.ZERO);
    }

    @SuppressWarnings("unchecked")
    private static List<LinkedStorageRef> getSessionRefs(RtsStorageSession session) {
        try {
            Field f = RtsStorageSession.class.getDeclaredField("linkedStorages");
            f.setAccessible(true);
            return (List<LinkedStorageRef>) f.get(session);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package com.rtsbuilding.rtsbuilding.server.benchmark.infrastructure;

import org.junit.jupiter.api.Test;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark runner — discovers and executes all {@code *JmhBenchmark}
 * classes via JMH's programmatic API, then persists results to SQLite
 * via {@link BenchmarkDatabase} for the visualization frontend.
 *
 * <p>This is the single JUnit 5 test entry point for all microbenchmarks.
 * JMH handles warmup, JIT compilation, GC control, forking, and
 * statistical analysis. Each benchmark method inherits per-method
 * {@code @Warmup} / {@code @Measurement} settings from the benchmark class.</p>
 */
class JmhBenchmarkRunner {

    private static final int FORKS = 1;
    private static final int WARMUP_ITERATIONS = 2;
    private static final int MEASUREMENT_ITERATIONS = 3;
    private static final int WARMUP_TIME_SECONDS = 1;
    private static final int MEASUREMENT_TIME_SECONDS = 1;

    @Test
    void runAllJmhBenchmarks() throws Exception {
        System.out.println("[JMH] Starting all microbenchmarks...");
        System.out.println("[JMH] Forks=" + FORKS
                + ", warmup=" + WARMUP_ITERATIONS + "x" + WARMUP_TIME_SECONDS + "s"
                + ", measurement=" + MEASUREMENT_ITERATIONS + "x" + MEASUREMENT_TIME_SECONDS + "s");

        var opt = new OptionsBuilder()
                .include(".*JmhBenchmark")
                .shouldDoGC(true)
                .forks(FORKS)
                .warmupIterations(WARMUP_ITERATIONS)
                .warmupTime(TimeValue.seconds(WARMUP_TIME_SECONDS))
                .measurementIterations(MEASUREMENT_ITERATIONS)
                .measurementTime(TimeValue.seconds(MEASUREMENT_TIME_SECONDS))
                .timeUnit(TimeUnit.NANOSECONDS)
                .jvmArgs("-Xmx512m", "-Xms256m")
                .build();

        // Individual benchmark classes override these defaults with their own
        // @Warmup / @Measurement / @BenchmarkMode annotations where needed
        // (e.g. SingleShotTime for mount/unmount bulk benchmarks).

        Runner runner = new Runner(opt);
        Collection<RunResult> results = runner.run();

        System.out.println();
        System.out.println("====================================================");
        System.out.println("  JMH microbenchmarks complete — "
                + results.size() + " benchmarks");
        System.out.println("====================================================");

        // 持久化 benchmark 结果到 SQLite 数据库（供 BenchmarkServer 使用）
        System.out.println("[JMH] 开始持久化 benchmark 结果到 SQLite...");
        try {
            String ts = LocalDateTime.now().toString();
            System.out.println("[JMH] 时间戳: " + ts);
            Map<String, Double> resMap = collectResults(results);
            System.out.println("[JMH] 收集到 " + resMap.size() + " 项指标");

            BenchmarkDatabase.saveRun(ts, resMap);
            System.out.println("[JMH] SQLite 写入完成");
            System.out.println("[JMH] 启动数据服务器: .\\gradlew.bat startBenchmarkServer");
            System.out.println("[JMH] 然后打开 benchmark-visualization/index.html 查看趋势图");
        } catch (Exception e) {
            System.err.println("[JMH] 持久化失败: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("[JMH] 持久化流程结束");
        System.out.flush();
    }

    /**
     * Converts a fully-qualified benchmark name (e.g.
     * {@code com...JmhBenchmark.methodName}) into a readable label:
     * {@code [Category] methodName (param=val)}.
     */
    private static String formatBenchmarkLabel(RunResult result) {
        String fullName = result.getParams().getBenchmark(); // e.g. "com...RtsAggregateStorageJmhBenchmark.hasItemLarge"
        int lastDot = fullName.lastIndexOf('.');
        String methodName = lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;

        // Extract category from class name (before "JmhBenchmark")
        String className = lastDot >= 0 ? fullName.substring(0, lastDot) : fullName;
        int lastClassDot = className.lastIndexOf('.');
        String simpleClassName = lastClassDot >= 0 ? className.substring(lastClassDot + 1) : className;
        String category = simpleClassName.endsWith("JmhBenchmark")
                ? simpleClassName.substring(0, simpleClassName.length() - "JmhBenchmark".length())
                : simpleClassName;

        // Add params if any (e.g. paramCount=10000)
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(category).append("] ").append(methodName);
        String params = result.getParams().getParamsKeys().isEmpty() ? "" : result.getParams().toString();
        if (!params.isEmpty()) {
            sb.append(" (").append(params).append(")");
        }
        return sb.toString();
    }



    /**
     * 将 JMH RunResult 集合转换为 metric label → score 的 Map。
     */
    private static Map<String, Double> collectResults(Collection<RunResult> results) {
        Map<String, Double> map = new LinkedHashMap<>();
        for (RunResult result : results) {
            String label = formatBenchmarkLabel(result);
            double score = result.getPrimaryResult().getScore();
            map.put(label, score);
        }
        return map;
    }
}

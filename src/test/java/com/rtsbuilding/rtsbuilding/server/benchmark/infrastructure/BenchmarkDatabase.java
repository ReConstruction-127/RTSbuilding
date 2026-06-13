package com.rtsbuilding.rtsbuilding.server.benchmark.infrastructure;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SQLite-backed data layer for benchmark results persistence.
 *
 * <p>Stores each benchmark run as a row in the {@code runs} table with
 * individual metric scores in the {@code results} table. Provides methods
 * for inserting new runs and querying historical data as JSON for the
 * visualization frontend.</p>
 *
 * <p>Thread-safe: all public methods are synchronized on the class.</p>
 */
public final class BenchmarkDatabase {

    private static final String DB_URL = "jdbc:sqlite:benchmark-visualization/benchmark.db";

    /** Maximum number of runs to keep in the database. */
    private static final int MAX_RUNS = 20;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("SQLite JDBC driver not found: " + e.getMessage());
        }
    }

    private BenchmarkDatabase() { }

    // ======================================================================
    //  Initialization
    // ======================================================================

    /**
     * Ensures the database and tables exist. Idempotent — safe to call multiple times.
     */
    public static synchronized void init() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS runs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        timestamp TEXT NOT NULL UNIQUE
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS results (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        run_id INTEGER NOT NULL,
                        metric TEXT NOT NULL,
                        score REAL NOT NULL,
                        FOREIGN KEY (run_id) REFERENCES runs(id) ON DELETE CASCADE
                    )
                    """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_results_run_id ON results(run_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_results_metric ON results(metric)");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize benchmark database", e);
        }
    }

    // ======================================================================
    //  Write
    // ======================================================================

    /**
     * Saves a single benchmark run with all its metric scores.
     *
     * @param timestamp  ISO-8601 timestamp string
     * @param resultsMap map of metric label → score
     */
    public static synchronized void saveRun(String timestamp, Map<String, Double> resultsMap) {
        init(); // ensure tables exist
        String insertRun = "INSERT OR IGNORE INTO runs (timestamp) VALUES (?)";
        String insertResult = "INSERT INTO results (run_id, metric, score) VALUES (?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Insert run
                int runId;
                try (PreparedStatement ps = conn.prepareStatement(insertRun, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, timestamp);
                    ps.executeUpdate();
                    ResultSet rs = ps.getGeneratedKeys();
                    if (rs.next()) {
                        runId = rs.getInt(1);
                    } else {
                        // Run already exists — get its ID
                        try (PreparedStatement q = conn.prepareStatement("SELECT id FROM runs WHERE timestamp = ?")) {
                            q.setString(1, timestamp);
                            rs = q.executeQuery();
                            runId = rs.getInt("id");
                        }
                    }
                }

                // Insert results
                try (PreparedStatement ps = conn.prepareStatement(insertResult)) {
                    for (Map.Entry<String, Double> entry : resultsMap.entrySet()) {
                        ps.setInt(1, runId);
                        ps.setString(2, entry.getKey());
                        ps.setDouble(3, entry.getValue());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();

                // 自动清理：保持最新 20 条 runs
                enforceRetentionLimit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save benchmark run", e);
        }
    }

    // ======================================================================
    //  Query — JSON generation for the frontend
    // ======================================================================

    /**
     * Returns all runs with their results as a JSON string matching the
     * expected format of the JavaScript frontend:
     * <pre>
     * [{"ts":"ISO_TIMESTAMP","r":{"metric1":score1,"metric2":score2,...}}, ...]
     * </pre>
     */
    public static synchronized String getRunsJson() {
        init();
        StringBuilder json = new StringBuilder("[");
        boolean firstRun = true;

        String sql = """
                SELECT r.id, r.timestamp, rs.metric, rs.score
                FROM runs r
                LEFT JOIN results rs ON rs.run_id = r.id
                ORDER BY r.id ASC, rs.metric ASC
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            Map<String, Map<String, Double>> runsMap = new LinkedHashMap<>();
            while (rs.next()) {
                String ts = rs.getString("timestamp");
                String metric = rs.getString("metric");
                double score = rs.getDouble("score");

                runsMap.computeIfAbsent(ts, k -> new LinkedHashMap<>());
                if (metric != null) {
                    runsMap.get(ts).put(metric, score);
                }
            }

            for (Map.Entry<String, Map<String, Double>> entry : runsMap.entrySet()) {
                if (!firstRun) json.append(",");
                firstRun = false;
                json.append("{\"ts\":\"")
                    .append(escapeJson(entry.getKey()))
                    .append("\",\"r\":{");
                boolean firstMetric = true;
                for (Map.Entry<String, Double> me : entry.getValue().entrySet()) {
                    if (!firstMetric) json.append(",");
                    firstMetric = false;
                    json.append("\"").append(escapeJson(me.getKey())).append("\":").append(me.getValue());
                }
                json.append("}}");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query benchmark data", e);
        }

        json.append("]");
        return json.toString();
    }

    /**
     * Returns the number of runs stored in the database.
     */
    public static synchronized int getRunCount() {
        init();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM runs")) {
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count runs", e);
        }
    }

    // ======================================================================
    //  Retention management
    // ======================================================================

    /**
     * Enforces a retention limit of {@link #MAX_RUNS} runs: if the total number
     * of runs exceeds the limit, the oldest run(s) are deleted until only
     * {@link #MAX_RUNS} remain.
     *
     * <p>Relies on the {@code ON DELETE CASCADE} foreign key on {@code results}
     * to clean up associated metric rows automatically.</p>
     */
    private static void enforceRetentionLimit() {
        String countSql = "SELECT COUNT(*) FROM runs";
        String deleteOldest = """
                DELETE FROM runs WHERE id IN (
                    SELECT id FROM runs ORDER BY id ASC LIMIT ?
                )
                """;

        try (Connection conn = getConnection();
             PreparedStatement countPs = conn.prepareStatement(countSql)) {
            ResultSet rs = countPs.executeQuery();
            int total = rs.getInt(1);
            if (total > MAX_RUNS) {
                int excess = total - MAX_RUNS;
                try (PreparedStatement delPs = conn.prepareStatement(deleteOldest)) {
                    delPs.setInt(1, excess);
                    int deleted = delPs.executeUpdate();
                    System.out.println("[BenchmarkDatabase] 自动清理 " + deleted + " 条旧 run");
                }
            }
        } catch (SQLException e) {
            System.err.println("[BenchmarkDatabase] 清理旧 run 时出错: " + e.getMessage());
        }
    }

    // ======================================================================
    //  Reset / Delete-All
    // ======================================================================

    /**
     * Deletes ALL benchmark data from the database and reclaims disk space.
     *
     * <p>This drops all rows from {@code runs} (and cascades to {@code results}),
     * then runs {@code VACUUM} to shrink the database file. After reset, the
     * tables are re-initialised via {@link #init()} so the database is ready
     * for fresh benchmark runs.</p>
     *
     * @return the number of runs deleted
     */
    public static synchronized int reset() {
        init();
        String countSql = "SELECT COUNT(*) FROM runs";
        String deleteAll = "DELETE FROM runs";
        String vacuum = "VACUUM";

        try (Connection conn = getConnection()) {
            // Count before deleting
            int before;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countSql)) {
                before = rs.getInt(1);
            }

            if (before > 0) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(deleteAll);
                }
                System.out.println("[BenchmarkDatabase] 已删除 " + before + " 条 run");
            }

            // Reclaim disk space
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(vacuum);
            }
            System.out.println("[BenchmarkDatabase] VACUUM 完成");
            return before;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reset benchmark database", e);
        }
    }

    // ======================================================================
    //  Main — CLI entry point for standalone reset
    // ======================================================================

    /**
     * Standalone CLI entry point.
     *
     * <p>Usage: {@code java ...BenchmarkDatabase reset}</p>
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        if (args.length > 0 && "reset".equalsIgnoreCase(args[0])) {
            System.out.println("[BenchmarkDatabase] 开始一键删库...");
            int deleted = reset();
            System.out.println("[BenchmarkDatabase] 已删除 " + deleted + " 条 run，数据库已清空");
            System.out.println("[BenchmarkDatabase] 删库完成 (￣▽￣)ノ");
        } else {
            System.out.println("用法: BenchmarkDatabase reset");
            System.out.println("   reset  - 清空所有 benchmark 数据");
        }
    }

    // ======================================================================
    //  Internal
    // ======================================================================

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

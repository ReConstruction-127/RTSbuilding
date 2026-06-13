package com.rtsbuilding.rtsbuilding.server.benchmark.infrastructure;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Lightweight HTTP server that serves benchmark data from the SQLite database
 * to the {@code benchmark-visualization/index.html} frontend.
 *
 * <p>Uses the JDK-built-in {@link HttpServer} — no external dependencies required.
 * Start with:</p>
 * <pre>
 *   .\gradlew.bat startBenchmarkServer
 * </pre>
 * <p>Then open {@code benchmark-visualization/index.html} in the browser.</p>
 */
public class BenchmarkServer {

    private static final int PORT = 18888;
    private static final String API_PREFIX = "/api";

    public static void main(String[] args) throws IOException {
        HttpServer server = createServer();
        int actualPort = server.getAddress().getPort();

        server.createContext("/api/data", BenchmarkServer::handleData);
        server.createContext("/api/runs", BenchmarkServer::handleRuns);
        server.createContext("/api/ping", BenchmarkServer::handlePing);
        server.createContext("/api/reset", BenchmarkServer::handleReset);

        server.setExecutor(null); // use the default (daemon) executor
        server.start();

        System.out.println("================================================");
        System.out.println("Benchmark 数据服务器已启动");
        System.out.println("================================================");
        System.out.println("地址: http://localhost:" + actualPort);
        System.out.println("端口: " + actualPort);
        System.out.println("接口:");
        System.out.println("GET /api/ping  — 健康检查");
        System.out.println("GET /api/runs   — 运行列表");
        System.out.println("GET /api/data   — 完整数据(用于图表)");
        System.out.println("POST /api/reset — 清空所有 benchmark 数据");
        System.out.println("================================================");
        System.out.println("按 Ctrl+C 停止服务器");
        System.out.println("================================================");
    }

    /**
     * Creates the HTTP server, trying the preferred port first.
     * <p>Unlike the old {@code resolvePort} approach, this directly binds to the target
     * port without a separate probe-then-bind sequence, avoiding the TOCTOU race condition
     * where the port goes into TIME_WAIT between the probe and the actual bind.</p>
     *
     * @return the bound {@link HttpServer}
     * @throws IOException if binding fails entirely
     */
    private static HttpServer createServer() throws IOException {
        try {
            return HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        } catch (BindException e) {
            System.err.println("端口 " + PORT + " 被占用，将使用系统分配的空闲端口...");
            System.err.println("原因: " + e.getMessage());
            return HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        }
    }

    // ======================================================================
    //  Handlers
    // ======================================================================

    private static void handleData(HttpExchange exchange) throws IOException {
        String json = BenchmarkDatabase.getRunsJson();
        sendJson(exchange, json);
    }

    private static void handleRuns(HttpExchange exchange) throws IOException {
        int count = BenchmarkDatabase.getRunCount();
        String json = "{\"count\":" + count + ",\"runs\":" + BenchmarkDatabase.getRunsJson() + "}";
        sendJson(exchange, json);
    }

    private static void handlePing(HttpExchange exchange) throws IOException {
        String ok = "{\"status\":\"ok\",\"db\":\"benchmark-visualization/benchmark.db\"}";
        sendJson(exchange, ok);
    }

    private static void handleReset(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            // Reject non-POST with 405
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        int deleted = BenchmarkDatabase.reset();
        String json = "{\"status\":\"ok\",\"deleted\":" + deleted + "}";
        sendJson(exchange, json);
    }

    // ======================================================================
    //  Response helpers
    // ======================================================================

    private static void sendJson(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");

        // CORS headers — allow the HTML to be opened from file:// or any origin
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        // Handle preflight
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

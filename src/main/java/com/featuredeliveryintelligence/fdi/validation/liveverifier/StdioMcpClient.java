package com.featuredeliveryintelligence.fdi.validation.liveverifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

/**
 * Minimal MCP JSON-RPC-over-stdio client: newline-delimited JSON messages,
 * responses matched by numeric id, server notifications and requests without
 * a matching id are ignored, and JSON-RPC {@code error} responses or
 * malformed/closed IO fail closed with {@link VerificationFailure}. This
 * mirrors the exchange performed by the Python {@code mcp} 1.29.1
 * {@code ClientSession} over {@code stdio_client} without importing the
 * external runtime.
 */
final class StdioMcpClient implements McpClient {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration DEFAULT_RESPONSE_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_CLOSE_TIMEOUT = Duration.ofSeconds(2);

    private final Process process;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final Thread stderrDrain;
    private final ExecutorService responseReader;
    private final Duration responseTimeout;
    private final Duration closeTimeout;
    private int nextId = 0;

    StdioMcpClient(List<String> command, java.nio.file.Path directory) throws IOException {
        this(command, directory, DEFAULT_RESPONSE_TIMEOUT, DEFAULT_CLOSE_TIMEOUT);
    }

    StdioMcpClient(List<String> command, java.nio.file.Path directory,
            Duration responseTimeout, Duration closeTimeout) throws IOException {
        if (responseTimeout == null || responseTimeout.isZero() || responseTimeout.isNegative()
                || closeTimeout == null || closeTimeout.isZero() || closeTimeout.isNegative()) {
            throw new IllegalArgumentException("MCP timeouts must be positive");
        }
        this.process = new ProcessBuilder(command).directory(directory.toFile()).start();
        this.responseTimeout = responseTimeout;
        this.closeTimeout = closeTimeout;
        this.reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        this.stderrDrain = new Thread(() -> {
            BufferedReader stderr = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
            try {
                while (stderr.readLine() != null) {
                    // drained to keep the server stderr pipe from blocking
                }
            } catch (IOException ignored) {
                // server exited
            }
        });
        this.stderrDrain.setDaemon(true);
        this.stderrDrain.start();
        this.responseReader = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "fdi-mcp-stdout-reader");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public JsonNode initialize() throws IOException {
        ObjectNode params = JSON.createObjectNode();
        // mcp 1.29.1's default client protocol version, matching the frozen
        // Python ClientSession.initialize() exchange.
        params.put("protocolVersion", "2025-11-25");
        params.putObject("capabilities");
        ObjectNode clientInfo = params.putObject("clientInfo");
        clientInfo.put("name", "fdi-graphify-live-verifier");
        clientInfo.put("version", "1.0.0");
        return request("initialize", params);
    }

    @Override
    public void notifyInitialized() throws IOException {
        ObjectNode message = JSON.createObjectNode();
        message.put("jsonrpc", "2.0");
        message.put("method", "notifications/initialized");
        writeLine(message);
    }

    @Override
    public JsonNode listTools() throws IOException {
        return request("tools/list", JSON.createObjectNode());
    }

    @Override
    public JsonNode callTool(String name, ObjectNode arguments) throws IOException {
        ObjectNode params = JSON.createObjectNode();
        params.put("name", name);
        params.set("arguments", arguments);
        return request("tools/call", params);
    }

    @Override
    public void close() {
        closeQuietly(writer);
        process.destroy();
        try {
            if (!process.waitFor(closeTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                process.waitFor(closeTimeout.toMillis(), TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException interrupted) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        } finally {
            closeQuietly(reader);
            responseReader.shutdownNow();
        }
    }

    private synchronized JsonNode request(String method, ObjectNode params) throws IOException {
        int id = ++nextId;
        ObjectNode message = JSON.createObjectNode();
        message.put("jsonrpc", "2.0");
        message.put("id", id);
        message.put("method", method);
        message.set("params", params);
        writeLine(message);
        while (true) {
            String line = readLineWithTimeout();
            if (line == null) {
                throw new VerificationFailure("MCP server closed the stdio session");
            }
            JsonNode response;
            try {
                response = JSON.readTree(line);
            } catch (IOException | RuntimeException malformed) {
                throw new VerificationFailure("MCP server sent a malformed message");
            }
            if (response == null || !response.isObject() || !response.has("id")
                    || response.get("id") == null || !response.get("id").isNumber()
                    || response.get("id").asInt() != id) {
                // server notification or request without a matching id: ignored
                continue;
            }
            JsonNode error = response.get("error");
            if (error != null && !error.isNull()) {
                JsonNode messageNode = error.isObject() ? error.get("message") : null;
                throw new VerificationFailure(messageNode != null && messageNode.isTextual()
                        ? messageNode.asText() : "MCP request failed");
            }
            JsonNode result = response.get("result");
            return result == null ? JSON.createObjectNode() : result;
        }
    }

    private String readLineWithTimeout() throws IOException {
        Future<String> pending = responseReader.submit(reader::readLine);
        try {
            return pending.get(responseTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException timedOut) {
            pending.cancel(true);
            throw new VerificationFailure("MCP server response timed out after "
                    + responseTimeout.toMillis() + " ms");
        } catch (InterruptedException interrupted) {
            pending.cancel(true);
            process.destroyForcibly();
            try {
                process.waitFor(closeTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException interruptedAgain) {
                // The interrupt is restored below after the bounded cleanup attempt.
            } finally {
                Thread.currentThread().interrupt();
            }
            throw new VerificationFailure("MCP server response wait was interrupted");
        } catch (ExecutionException failed) {
            Throwable cause = failed.getCause();
            if (cause instanceof IOException ioFailure) {
                throw ioFailure;
            }
            if (cause instanceof RuntimeException runtimeFailure) {
                throw runtimeFailure;
            }
            throw new IOException("MCP server response read failed", cause);
        }
    }

    private void writeLine(ObjectNode message) throws IOException {
        writer.write(JSON.writeValueAsString(message));
        writer.write('\n');
        writer.flush();
    }

    private static void closeQuietly(java.io.Closeable stream) {
        try {
            stream.close();
        } catch (IOException ignored) {
            // Process termination is the authoritative close outcome.
        }
    }
}

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
import java.util.List;
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

    private final Process process;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final Thread stderrDrain;
    private int nextId = 0;

    StdioMcpClient(List<String> command, java.nio.file.Path directory) throws IOException {
        this.process = new ProcessBuilder(command).directory(directory.toFile()).start();
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
        process.destroy();
        try {
            process.waitFor(10, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
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
            String line = reader.readLine();
            if (line == null) {
                throw new VerificationFailure("MCP server closed the stdio session");
            }
            JsonNode response;
            try {
                response = JSON.readTree(line);
            } catch (RuntimeException malformed) {
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

    private void writeLine(ObjectNode message) throws IOException {
        writer.write(JSON.writeValueAsString(message));
        writer.write('\n');
        writer.flush();
    }
}

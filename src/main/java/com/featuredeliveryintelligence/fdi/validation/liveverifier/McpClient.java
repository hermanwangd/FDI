package com.featuredeliveryintelligence.fdi.validation.liveverifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

/**
 * Minimal MCP client surface used by {@link GraphifyLiveVerifier}. The real
 * implementation is {@link StdioMcpClient} (newline-delimited JSON-RPC over
 * the Graphify server stdio); tests substitute a scripted in-process client.
 */
interface McpClient extends AutoCloseable {
    /** Ports the {@code initialize} request; returns the initialize result. */
    JsonNode initialize() throws IOException;

    /** Ports the {@code notifications/initialized} notification. */
    void notifyInitialized() throws IOException;

    /** Ports {@code tools/list}; returns the list result ({@code "tools"} array). */
    JsonNode listTools() throws IOException;

    /** Ports {@code tools/call}; returns the CallTool result object. */
    JsonNode callTool(String name, ObjectNode arguments) throws IOException;

    @Override
    void close();
}

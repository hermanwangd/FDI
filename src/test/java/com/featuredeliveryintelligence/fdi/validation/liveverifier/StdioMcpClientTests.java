package com.featuredeliveryintelligence.fdi.validation.liveverifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Framing and failure-surface tests for the minimal MCP JSON-RPC-over-stdio
 * client. A canned Python responder stands in for the Graphify server so the
 * newline-delimited message exchange, numeric-id matching, notification
 * skipping, JSON-RPC error surfacing, and EOF fail-closed behavior are
 * exercised without the frozen runtime.
 */
class StdioMcpClientTests {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir Path temp;

    @Test
    void roundTripsInitializeListAndCall() throws Exception {
        Path script = writeResponder(RESPONDER);
        try (StdioMcpClient client = new StdioMcpClient(
                List.of("python3", "-u", script.toString()), temp)) {
            JsonNode initialization = client.initialize();
            assertEquals("2024-11-05", initialization.get("protocolVersion").asText());
            assertEquals("fake", initialization.get("serverInfo").get("name").asText());
            client.notifyInitialized();

            JsonNode listed = client.listTools();
            assertEquals("get_node", listed.get("tools").get(0).get("name").asText());

            ObjectNode arguments = JSON.createObjectNode();
            arguments.put("label", "PetClinicApplication.java");
            JsonNode result = client.callTool("get_node", arguments);
            assertEquals("ok", result.get("content").get(0).get("text").asText());
        }
    }

    @Test
    void jsonRpcErrorSurfacesAsVerificationFailure() throws Exception {
        Path script = writeResponder(ERROR_RESPONDER);
        try (StdioMcpClient client = new StdioMcpClient(
                List.of("python3", "-u", script.toString()), temp)) {
            VerificationFailure failure = assertThrows(VerificationFailure.class,
                    client::initialize);
            assertEquals("boom", failure.getMessage());
        }
    }

    @Test
    void closedServerFailsClosed() throws Exception {
        Path script = writeResponder(DEAD_RESPONDER);
        try (StdioMcpClient client = new StdioMcpClient(
                List.of("python3", "-u", script.toString()), temp)) {
            VerificationFailure failure = assertThrows(VerificationFailure.class,
                    client::initialize);
            assertEquals("MCP server closed the stdio session", failure.getMessage());
        }
    }

    private Path writeResponder(String body) throws Exception {
        Path script = temp.resolve("responder-" + Math.abs(body.hashCode()) + ".py");
        Files.writeString(script, body, StandardCharsets.UTF_8);
        return script;
    }

    /** Emits a spurious notification before the initialize response to exercise id matching. */
    private static final String RESPONDER = """
            import sys, json
            for line in sys.stdin:
                line = line.strip()
                if not line:
                    continue
                msg = json.loads(line)
                if 'method' in msg and 'id' not in msg:
                    continue
                if msg.get('method') == 'initialize':
                    print(json.dumps({"jsonrpc": "2.0", "method": "notifications/progress",
                                      "params": {}}), flush=True)
                    result = {"protocolVersion": "2024-11-05", "capabilities": {},
                              "serverInfo": {"name": "fake", "version": "1.0"}}
                elif msg.get('method') == 'tools/list':
                    result = {"tools": [{"name": "get_node", "description": "d",
                                         "inputSchema": {"type": "object",
                                                         "properties": {"label": {}}}}]}
                elif msg.get('method') == 'tools/call':
                    result = {"content": [{"type": "text", "text": "ok"}],
                              "structuredContent": None, "isError": False}
                else:
                    print(json.dumps({"jsonrpc": "2.0", "id": msg.get('id'),
                                      "error": {"code": -32601, "message": "unknown"}}),
                          flush=True)
                    continue
                print(json.dumps({"jsonrpc": "2.0", "id": msg.get('id'), "result": result}),
                      flush=True)
            """;

    private static final String ERROR_RESPONDER = """
            import sys, json
            for line in sys.stdin:
                line = line.strip()
                if not line:
                    continue
                msg = json.loads(line)
                if 'method' in msg and 'id' not in msg:
                    continue
                print(json.dumps({"jsonrpc": "2.0", "id": msg.get('id'),
                                  "error": {"code": -32603, "message": "boom"}}), flush=True)
            """;

    private static final String DEAD_RESPONDER = "import sys\nsys.exit(0)\n";
}

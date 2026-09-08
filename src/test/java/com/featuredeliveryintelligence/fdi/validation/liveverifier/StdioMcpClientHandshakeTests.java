package com.featuredeliveryintelligence.fdi.validation.liveverifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Handshake-contract and remaining fail-closed-path tests for the minimal MCP
 * JSON-RPC-over-stdio client, complementing {@link StdioMcpClientTests}. A
 * scripted Python responder stands in for the Graphify server so the exact
 * initialize exchange, the {@code notifications/initialized} notification,
 * malformed-line handling, launch failure, and server method-mismatch failure
 * are pinned without the frozen runtime and without network access.
 */
class StdioMcpClientHandshakeTests {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir Path temp;

    @Test
    void initializePinsFrozenClientHandshake() throws Exception {
        Path script = writeResponder(ECHO_RESPONDER);
        try (StdioMcpClient client = new StdioMcpClient(
                List.of("python3", "-u", script.toString()), temp)) {
            JsonNode echo = client.initialize().get("echo");
            assertEquals("2.0", echo.get("jsonrpc").asText());
            assertEquals("initialize", echo.get("method").asText());
            assertEquals(1, echo.get("id").asInt());
            // mcp 1.29.1 default client protocol version, matching the frozen
            // Python ClientSession.initialize() exchange.
            assertEquals("2025-11-25", echo.get("params").get("protocolVersion").asText());
            assertTrue(echo.get("params").get("capabilities").isObject());
            JsonNode clientInfo = echo.get("params").get("clientInfo");
            assertEquals("fdi-graphify-live-verifier", clientInfo.get("name").asText());
            assertEquals("1.0.0", clientInfo.get("version").asText());
        }
    }

    @Test
    void initializedNotificationIsSentWithoutIdAndNeedsNoResponse() throws Exception {
        Path script = writeResponder(RECORDING_RESPONDER);
        try (StdioMcpClient client = new StdioMcpClient(
                List.of("python3", "-u", script.toString()), temp)) {
            client.initialize();
            client.notifyInitialized();
            // The notification must not expect or wait for a response; a
            // tools/list round trip afterwards proves the stream stayed in sync.
            JsonNode listed = client.listTools();
            assertEquals("get_node", listed.get("tools").get(0).get("name").asText());
        }
        List<String> recorded = Files.readAllLines(temp.resolve("recorded.jsonl"),
                StandardCharsets.UTF_8);
        assertEquals(3, recorded.size());
        ObjectNode notification = (ObjectNode) JSON.readTree(recorded.get(1));
        assertEquals("notifications/initialized", notification.get("method").asText());
        assertFalse(notification.has("id"));
    }

    @Test
    void nonJsonLineFailsClosed() throws Exception {
        Path script = writeResponder(MALFORMED_RESPONDER);
        try (StdioMcpClient client = new StdioMcpClient(
                List.of("python3", "-u", script.toString()), temp)) {
            VerificationFailure failure = assertThrows(
                    VerificationFailure.class, client::initialize);
            assertEquals("MCP server sent a malformed message", failure.getMessage());
        }
    }

    @Test
    void silentServerTimesOutWithNormalizedFailure() throws Exception {
        Path script = writeResponder(SILENT_RESPONDER);
        try (StdioMcpClient client = new StdioMcpClient(
                List.of("python3", "-u", script.toString()), temp,
                Duration.ofMillis(75), Duration.ofMillis(75))) {
            VerificationFailure failure = assertTimeoutPreemptively(Duration.ofSeconds(1),
                    () -> assertThrows(VerificationFailure.class, client::initialize));
            assertEquals("MCP server response timed out after 75 ms", failure.getMessage());
        }
    }

    @Test
    void closeForciblyTerminatesServerThatIgnoresTerminate() throws Exception {
        Path script = writeResponder(STUBBORN_RESPONDER);
        StdioMcpClient client = new StdioMcpClient(
                List.of("python3", "-u", script.toString()), temp,
                Duration.ofSeconds(1), Duration.ofMillis(75));
        Path pidFile = temp.resolve("stubborn.pid");
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            while (!Files.exists(pidFile)) {
                Thread.sleep(5);
            }
        });
        long pid = Long.parseLong(Files.readString(pidFile).trim());

        assertTimeoutPreemptively(Duration.ofSeconds(1), client::close);
        assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
    }

    @Test
    void interruptedResponseWaitPreservesFlagAndTerminatesChild() throws Exception {
        Path script = writeResponder(SILENT_RESPONDER);
        try (StdioMcpClient client = new StdioMcpClient(
                List.of("python3", "-u", script.toString()), temp,
                Duration.ofSeconds(5), Duration.ofMillis(75))) {
            Path pidFile = temp.resolve("silent.pid");
            assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
                while (!Files.exists(pidFile)) {
                    Thread.sleep(5);
                }
            });
            long pid = Long.parseLong(Files.readString(pidFile).trim());
            Thread.currentThread().interrupt();
            try {
                VerificationFailure failure = assertThrows(
                        VerificationFailure.class, client::initialize);
                assertEquals("MCP server response wait was interrupted", failure.getMessage());
                assertTrue(Thread.currentThread().isInterrupted());
                assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
            } finally {
                Thread.interrupted();
            }
        }
    }

    @Test
    void missingExecutableFailsClosedWithIOException() {
        assertThrows(IOException.class, () -> new StdioMcpClient(
                List.of("fdi-definitely-not-a-real-mcp-server", "--flag"), temp));
    }

    @Test
    void missingWorkingDirectoryFailsLaunchWithIOException() {
        assertThrows(IOException.class, () -> new StdioMcpClient(
                List.of("python3", "-c", "pass"), temp.resolve("no-such-directory")));
    }

    @Test
    void serverMethodMismatchSurfacesJsonRpcError() throws Exception {
        // The frozen verifier requires tools/call; a server without it answers
        // JSON-RPC -32601, which must surface as a VerificationFailure.
        Path script = writeResponder(METHOD_MISMATCH_RESPONDER);
        try (StdioMcpClient client = new StdioMcpClient(
                List.of("python3", "-u", script.toString()), temp)) {
            client.initialize();
            ObjectNode arguments = JSON.createObjectNode();
            arguments.put("label", "PetClinicApplication.java");
            VerificationFailure failure = assertThrows(VerificationFailure.class,
                    () -> client.callTool("get_node", arguments));
            assertEquals("method not found", failure.getMessage());
        }
    }

    @Test
    void responsesWithForeignOrNonNumericIdsAreIgnored() throws Exception {
        Path script = writeResponder(NOISY_RESPONDER);
        try (StdioMcpClient client = new StdioMcpClient(
                List.of("python3", "-u", script.toString()), temp)) {
            JsonNode result = client.listTools();
            assertEquals("get_node", result.get("tools").get(0).get("name").asText());
        }
    }

    private Path writeResponder(String body) throws Exception {
        Path script = temp.resolve("responder-" + Math.abs(body.hashCode()) + ".py");
        Files.writeString(script, body, StandardCharsets.UTF_8);
        return script;
    }

    /** Answers every request with its own message echoed back as the result. */
    private static final String ECHO_RESPONDER = """
            import sys, json
            for line in sys.stdin:
                line = line.strip()
                if not line:
                    continue
                msg = json.loads(line)
                if 'method' in msg and 'id' not in msg:
                    continue
                print(json.dumps({"jsonrpc": "2.0", "id": msg.get('id'),
                                  "result": {"echo": msg}}), flush=True)
            """;

    /** Records every received line and answers initialize/tools/list normally. */
    private static final String RECORDING_RESPONDER = """
            import sys, json
            recorded = open('recorded.jsonl', 'w')
            for line in sys.stdin:
                line = line.strip()
                if not line:
                    continue
                msg = json.loads(line)
                recorded.write(json.dumps(msg) + '\\n')
                recorded.flush()
                if 'method' in msg and 'id' not in msg:
                    continue
                if msg.get('method') == 'initialize':
                    result = {"protocolVersion": "2025-11-25", "capabilities": {},
                              "serverInfo": {"name": "fake", "version": "1.0"}}
                elif msg.get('method') == 'tools/list':
                    result = {"tools": [{"name": "get_node", "description": "d",
                                         "inputSchema": {"type": "object",
                                                         "properties": {"label": {}}}}]}
                else:
                    print(json.dumps({"jsonrpc": "2.0", "id": msg.get('id'),
                                      "error": {"code": -32601, "message": "unknown"}}),
                          flush=True)
                    continue
                print(json.dumps({"jsonrpc": "2.0", "id": msg.get('id'), "result": result}),
                      flush=True)
            """;

    /** Emits a non-JSON line before the real initialize response. */
    private static final String MALFORMED_RESPONDER = """
            import sys, json
            for line in sys.stdin:
                line = line.strip()
                if not line:
                    continue
                msg = json.loads(line)
                if 'method' in msg and 'id' not in msg:
                    continue
                print("this is not json", flush=True)
                print(json.dumps({"jsonrpc": "2.0", "id": msg.get('id'),
                                  "result": {"protocolVersion": "2025-11-25"}}), flush=True)
            """;

    /** Reads requests but deliberately never answers them. */
    private static final String SILENT_RESPONDER = """
            import os, sys
            open('silent.pid', 'w').write(str(os.getpid()))
            for line in sys.stdin:
                pass
            """;

    /** Ignores SIGTERM so close() must escalate to forced termination. */
    private static final String STUBBORN_RESPONDER = """
            import os, signal, time
            signal.signal(signal.SIGTERM, signal.SIG_IGN)
            open('stubborn.pid', 'w').write(str(os.getpid()))
            while True:
                time.sleep(1)
            """;

    /** Answers tools/call with JSON-RPC -32601 like a server lacking that method. */
    private static final String METHOD_MISMATCH_RESPONDER = """
            import sys, json
            for line in sys.stdin:
                line = line.strip()
                if not line:
                    continue
                msg = json.loads(line)
                if 'method' in msg and 'id' not in msg:
                    continue
                if msg.get('method') == 'initialize':
                    result = {"protocolVersion": "2025-11-25", "capabilities": {},
                              "serverInfo": {"name": "fake", "version": "1.0"}}
                    print(json.dumps({"jsonrpc": "2.0", "id": msg.get('id'),
                                      "result": result}), flush=True)
                else:
                    print(json.dumps({"jsonrpc": "2.0", "id": msg.get('id'),
                                      "error": {"code": -32601, "message": "method not found"}}),
                          flush=True)
            """;

    /** Floods the stream with a notification, a wrong-id response, and a
     *  string-id response before answering tools/list correctly. */
    private static final String NOISY_RESPONDER = """
            import sys, json
            for line in sys.stdin:
                line = line.strip()
                if not line:
                    continue
                msg = json.loads(line)
                if 'method' in msg and 'id' not in msg:
                    continue
                print(json.dumps({"jsonrpc": "2.0", "method": "notifications/progress",
                                  "params": {}}), flush=True)
                print(json.dumps({"jsonrpc": "2.0", "id": 999,
                                  "result": {"stale": True}}), flush=True)
                print(json.dumps({"jsonrpc": "2.0", "id": "1",
                                  "result": {"stringId": True}}), flush=True)
                result = {"tools": [{"name": "get_node", "description": "d",
                                     "inputSchema": {"type": "object",
                                                     "properties": {"label": {}}}}]}
                print(json.dumps({"jsonrpc": "2.0", "id": msg.get('id'),
                                  "result": result}), flush=True)
            """;
}

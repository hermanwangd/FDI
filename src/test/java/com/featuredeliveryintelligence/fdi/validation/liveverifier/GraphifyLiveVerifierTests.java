package com.featuredeliveryintelligence.fdi.validation.liveverifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ports the verification corpus of {@code tooling/validation/graphify_live_verifier.py}
 * to the Java {@link GraphifyLiveVerifier}. The MCP stdio session is replaced
 * by a scripted in-process {@link McpClient} so the live verification flow
 * (tool catalog, schema checks, queries, evidence assembly) is exercised
 * without the frozen Graphify runtime, which is not present in this worktree.
 */
class GraphifyLiveVerifierTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path REPOSITORY = Path.of("").toAbsolutePath();
    private static final String GRAPH_NAME = "petclinic-graph-818c413.json";

    @TempDir Path root;

    @Test
    void missingRuntimeRaisesVerificationFailure() throws Exception {
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root));
        assertEquals("Graphify runtime is missing: " + root.toRealPath()
                + "/.fdi-work/graphify-venv312/bin/python", failure.getMessage());
    }

    @Test
    void missingFrozenGraphRaisesVerificationFailure() throws Exception {
        writeRuntimePython();
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root));
        assertEquals("frozen Petclinic graph is missing: " + root.toRealPath()
                + "/validation/pkb001/artifacts/" + GRAPH_NAME, failure.getMessage());
    }

    @Test
    void missingFrozenSourceRaisesVerificationFailure() throws Exception {
        writeRuntimePython();
        copyGraph();
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root));
        assertEquals("Graphify frozen source is missing: " + root.toRealPath()
                + "/.fdi-work/graphify-source/graphify-main", failure.getMessage());
    }

    @Test
    void missingCandidateFileRaisesOSError() throws Exception {
        buildRuntimeAndFrozenInputs();
        Files.delete(candidatePath());
        assertThrows(IOException.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root));
    }

    @Test
    void candidateMustContainJsonObject() throws Exception {
        buildRuntimeAndFrozenInputs();
        Files.writeString(candidatePath(), "[1]\n");
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root));
        assertEquals(candidatePath().toRealPath() + " must contain a JSON object",
                failure.getMessage());
    }

    @Test
    void incompleteCalibrationBindingIsRejected() throws Exception {
        buildRuntimeAndFrozenInputs();
        ObjectNode candidate = validCandidate();
        candidate.remove("graphify_input");
        writeJson(candidatePath(), candidate);
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root));
        assertEquals("Petclinic calibration binding is incomplete", failure.getMessage());
    }

    @Test
    void frozenInputValidationNamesFailedChecks() throws Exception {
        buildRuntimeAndFrozenInputs();
        ObjectNode candidate = validCandidate();
        candidate.put("source_commit_sha", "0".repeat(40));
        writeJson(candidatePath(), candidate);
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root));
        assertEquals("frozen input validation failed: candidate source commit",
                failure.getMessage());
    }

    @Test
    void graphDigestMismatchNamesBothGraphChecks() throws Exception {
        buildRuntimeAndFrozenInputs();
        Files.write(graphPath(), "tampered graph bytes\n".getBytes("UTF-8"));
        ObjectNode candidate = validCandidate();
        ((ObjectNode) candidate.get("graphify")).put("artifact_sha256", "0".repeat(64));
        writeJson(candidatePath(), candidate);
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root));
        assertEquals("frozen input validation failed: candidate graph SHA-256, frozen graph SHA-256",
                failure.getMessage());
    }

    @Test
    void phase0InputPolicyDigestMustBePresent() throws Exception {
        buildRuntimeAndFrozenInputs();
        ObjectNode phase0 = JSON.createObjectNode();
        phase0.putObject("graphify");
        writeJson(phase0Path(), phase0);
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root));
        assertEquals("Phase 0 Graphify input policy digest is absent", failure.getMessage());
    }

    @Test
    void missingDirectUrlIsRejected() throws Exception {
        buildRuntimeAndFrozenInputs();
        Files.delete(directUrlPath());
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root));
        assertEquals("graphifyy direct_url.json is absent", failure.getMessage());
    }

    @Test
    void directUrlMustBindTheFrozenSource() throws Exception {
        buildRuntimeAndFrozenInputs();
        writeJson(directUrlPath(), urlNode("file:///somewhere/else"));
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root));
        assertEquals("installed graphifyy direct URL does not bind the frozen source",
                failure.getMessage());
    }

    @Test
    void versionMismatchIsRejected() throws Exception {
        buildRuntimeAndFrozenInputs();
        Files.writeString(metadataPath("graphifyy-0.1.14.dist-info"),
                "Metadata-Version: 2.1\nName: graphifyy\nVersion: 0.1.15\n");
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root));
        assertEquals("installed Graphify or MCP version does not match the frozen runtime",
                failure.getMessage());
    }

    @Test
    void missingMcpDistributionNamesThePackage() throws Exception {
        buildRuntimeAndFrozenInputs();
        Files.delete(metadataPath("mcp-1.29.1.dist-info"));
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root));
        assertEquals("mcp", failure.getMessage());
    }

    @Test
    void successfulVerificationAssemblesExactEvidence() throws Exception {
        buildRuntimeAndFrozenInputs();

        ObjectNode evidence = new GraphifyLiveVerifier()
                .verifyLiveInterface(root, FakeMcpClient.success());

        assertEquals(List.of("verification_id", "captured_at", "result", "queryable",
                "runtime_identity", "runtime_version", "runtime_python", "transport",
                "mcp_version", "wire_version", "server_info", "server_command",
                "resolved_graph_path", "server_exit_status", "server_error",
                "source_provenance", "supported_operations", "tool_catalog", "queries",
                "exact_revision_opened", "source_location_provenance", "snapshot_binding",
                "graph_sha256", "input_policy_sha256", "structural_proof", "limitations"),
                fieldNames(evidence));
        assertEquals("pkb001-graphify-live-818c413", evidence.get("verification_id").asText());
        assertTrue(evidence.get("captured_at").asText()
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
        assertEquals("EXACTLY_BOUND", evidence.get("result").asText());
        assertTrue(evidence.get("queryable").asBoolean());
        assertEquals("graphifyy", evidence.get("runtime_identity").asText());
        assertEquals("0.1.14", evidence.get("runtime_version").asText());
        assertEquals("1.29.1", evidence.get("mcp_version").asText());
        assertEquals("MCP stdio", evidence.get("transport").asText());
        assertEquals("MCP 2025-11-25", evidence.get("wire_version").asText());
        assertEquals("fake-graphifyy", evidence.get("server_info").get("name").asText());
        assertEquals(List.of(runtimePython().toString(), "-m", "graphify.serve",
                "graphify-out/" + GRAPH_NAME), texts(evidence.get("server_command")));
        assertEquals(graphPath().toRealPath().toString(),
                evidence.get("resolved_graph_path").asText());
        assertEquals("CLEAN_SESSION_CLOSE", evidence.get("server_exit_status").asText());
        assertTrue(evidence.get("server_error").isNull());
        ObjectNode provenance = (ObjectNode) evidence.get("source_provenance");
        assertEquals("8d806aa861e0ffa2136eda227d79d290dfdb89bf0c63fd00a4e2b4ea59d445",
                provenance.get("source_archive_sha256").asText());
        assertEquals("91f4d120b630ee35c79bf3c75ccd186870a808f9",
                provenance.get("zip_revision_comment").asText());
        assertEquals(asUri(sourcePath().toRealPath()),
                provenance.get("installed_direct_url").asText());
        assertEquals(GraphifyLiveVerifier.EXPECTED_OPERATIONS,
                texts(evidence.get("supported_operations")));
        ObjectNode nodeQuery = (ObjectNode) evidence.get("queries").get("node_query");
        assertEquals(List.of("tool", "arguments", "result", "is_error"), fieldNames(nodeQuery));
        assertEquals("get_node", nodeQuery.get("tool").asText());
        assertEquals("PetClinicApplication.java",
                nodeQuery.get("arguments").get("label").asText());
        assertTrue(nodeQuery.get("result").get("content").get(0).get("text").asText()
                .contains("Node: PetClinicApplication.java"));
        assertTrue(nodeQuery.get("result").get("structuredContent").isNull());
        assertTrue(nodeQuery.get("is_error").asBoolean() == false);
        ObjectNode pathQuery = (ObjectNode) evidence.get("queries").get("shortest_path");
        assertEquals(List.of("tool", "arguments", "result", "returned_path", "observed_hops"),
                fieldNames(pathQuery));
        assertEquals(1, pathQuery.get("observed_hops").asInt());
        assertEquals("PetClinicApplication.java --contains [EXTRACTED]--> PetClinicApplication",
                pathQuery.get("returned_path").asText());
        assertTrue(evidence.get("exact_revision_opened").asBoolean());
        assertEquals("git tree f92df0b05c91c7d29d81e70cf86f8678b0545bd2 at immutable commit "
                + "818c4136ea971c21674525f9053de0d9c7ad8cfe",
                evidence.get("source_location_provenance").asText());
        ObjectNode binding = (ObjectNode) evidence.get("snapshot_binding");
        assertEquals(List.of("requested_revision", "indexed_revision", "input_git_tree_oid",
                "graph_sha256"), fieldNames(binding));
        assertEquals("e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e",
                evidence.get("graph_sha256").asText());
        assertEquals("ab".repeat(32), evidence.get("input_policy_sha256").asText());
        assertEquals(3, evidence.get("limitations").size());
        assertEquals("Graphify graph.json does not embed Git revision metadata; the binding "
                + "is verified against the frozen calibration candidate and graph digest.",
                evidence.get("limitations").get(0).asText());
    }

    @Test
    void toolListMismatchFailsClosed() throws Exception {
        buildRuntimeAndFrozenInputs();
        FakeMcpClient client = FakeMcpClient.success();
        client.tools.remove(0);
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root, client));
        assertEquals("live MCP tool list differs from frozen expected operations",
                failure.getMessage());
    }

    @Test
    void missingToolArgumentsFailClosed() throws Exception {
        buildRuntimeAndFrozenInputs();
        FakeMcpClient client = FakeMcpClient.success();
        ObjectNode nodeProperties = (ObjectNode) getTool(client, "get_node")
                .get("inputSchema").get("properties");
        nodeProperties.remove("label");
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root, client));
        assertEquals("live MCP tool get_node does not expose required arguments",
                failure.getMessage());

        FakeMcpClient second = FakeMcpClient.success();
        ObjectNode pathProperties = (ObjectNode) getTool(second, "shortest_path")
                .get("inputSchema").get("properties");
        pathProperties.remove("max_hops");
        VerificationFailure pathFailure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root, second));
        assertEquals("live MCP tool shortest_path does not expose required arguments",
                pathFailure.getMessage());
    }

    @Test
    void emptyToolContentFailsClosed() throws Exception {
        buildRuntimeAndFrozenInputs();
        FakeMcpClient client = FakeMcpClient.success();
        client.callResults.put("get_node", callResult(JSON.createArrayNode(), false));
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root, client));
        assertEquals("MCP tool returned no content", failure.getMessage());
    }

    @Test
    void nonTextToolContentFailsClosed() throws Exception {
        buildRuntimeAndFrozenInputs();
        FakeMcpClient client = FakeMcpClient.success();
        ArrayNode content = JSON.createArrayNode();
        content.add(JSON.createObjectNode().put("type", "image"));
        client.callResults.put("get_node", callResult(content, false));
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root, client));
        assertEquals("MCP tool did not return text content", failure.getMessage());
    }

    @Test
    void unresolvedNodeFailsClosed() throws Exception {
        buildRuntimeAndFrozenInputs();
        FakeMcpClient client = FakeMcpClient.success();
        client.callResults.put("get_node", textCallResult("Node: SomethingElse.java"));
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root, client));
        assertEquals("live get_node query did not resolve the frozen Petclinic node",
                failure.getMessage());
    }

    @Test
    void nodeErrorResultFailsClosed() throws Exception {
        buildRuntimeAndFrozenInputs();
        FakeMcpClient client = FakeMcpClient.success();
        client.callResults.put("get_node",
                textCallResult("Node: PetClinicApplication.java", true));
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root, client));
        assertEquals("live get_node query did not resolve the frozen Petclinic node",
                failure.getMessage());
    }

    @Test
    void missingPathHeaderFailsClosed() throws Exception {
        buildRuntimeAndFrozenInputs();
        FakeMcpClient client = FakeMcpClient.success();
        client.callResults.put("shortest_path", textCallResult("no path here"));
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root, client));
        assertEquals("live shortest_path query did not return a path", failure.getMessage());
    }

    @Test
    void exceededHopsFailClosed() throws Exception {
        buildRuntimeAndFrozenInputs();
        FakeMcpClient client = FakeMcpClient.success();
        client.callResults.put("shortest_path", textCallResult(
                "Shortest path (2 hops): PetClinicApplication.java --contains [EXTRACTED]--> "
                        + "PetClinicApplication --owns [EXTRACTED]--> owner"));
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root, client));
        assertEquals("live shortest_path query exceeded requested max_hops",
                failure.getMessage());
    }

    @Test
    void unexpectedPathFailsClosed() throws Exception {
        buildRuntimeAndFrozenInputs();
        FakeMcpClient client = FakeMcpClient.success();
        client.callResults.put("shortest_path", textCallResult(
                "Shortest path (1 hops): Something --contains [EXTRACTED]--> Else"));
        VerificationFailure failure = assertThrows(VerificationFailure.class,
                () -> new GraphifyLiveVerifier().verifyLiveInterface(root, client));
        assertEquals("live shortest_path query returned an unexpected path",
                failure.getMessage());
    }

    // --- fixture builders ---

    private Path runtimePython() {
        try {
            return root.toRealPath().resolve(".fdi-work/graphify-venv312/bin/python");
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private Path graphPath() {
        return root.resolve("validation/pkb001/artifacts/" + GRAPH_NAME);
    }

    private Path sourcePath() {
        return root.resolve(".fdi-work/graphify-source/graphify-main");
    }

    private Path candidatePath() {
        return root.resolve("validation/pkb001/datasets/petclinic-calibration-candidate.json");
    }

    private Path phase0Path() {
        return root.resolve("validation/pkb001/datasets/phase0-evidence.json");
    }

    private Path sitePackages() {
        return root.resolve(".fdi-work/graphify-venv312/lib/python3.12/site-packages");
    }

    private Path metadataPath(String distInfo) {
        return sitePackages().resolve(distInfo + "/METADATA");
    }

    private Path directUrlPath() {
        return sitePackages().resolve("graphifyy-0.1.14.dist-info/direct_url.json");
    }

    private void writeRuntimePython() throws IOException {
        Path python = root.resolve(".fdi-work/graphify-venv312/bin/python");
        Files.createDirectories(python.getParent());
        Files.writeString(python, "# frozen runtime placeholder\n");
    }

    private void copyGraph() throws IOException {
        Files.createDirectories(graphPath().getParent());
        Files.copy(REPOSITORY.resolve("validation/pkb001/artifacts/" + GRAPH_NAME),
                graphPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void buildRuntimeAndFrozenInputs() throws IOException {
        writeRuntimePython();
        copyGraph();
        Files.createDirectories(sourcePath());
        Files.createDirectories(sitePackages());
        writeString(metadataPath("graphifyy-0.1.14.dist-info"),
                "Metadata-Version: 2.1\nName: graphifyy\nVersion: 0.1.14\n");
        writeString(metadataPath("mcp-1.29.1.dist-info"),
                "Metadata-Version: 2.1\nName: mcp\nVersion: 1.29.1\n");
        writeJson(directUrlPath(), urlNode(asUri(sourcePath().toRealPath())));
        writeJson(candidatePath(), validCandidate());
        ObjectNode phase0 = JSON.createObjectNode();
        ObjectNode graphify = phase0.putObject("graphify");
        graphify.put("input_policy_sha256", "ab".repeat(32));
        writeJson(phase0Path(), phase0);
    }

    private ObjectNode validCandidate() {
        ObjectNode candidate = JSON.createObjectNode();
        candidate.put("source_commit_sha", "818c4136ea971c21674525f9053de0d9c7ad8cfe");
        ObjectNode graphifyInput = candidate.putObject("graphify_input");
        graphifyInput.put("git_tree_oid", "f92df0b05c91c7d29d81e70cf86f8678b0545bd2");
        ObjectNode graphify = candidate.putObject("graphify");
        graphify.put("artifact_path", "validation/pkb001/artifacts/" + GRAPH_NAME);
        graphify.put("artifact_sha256",
                "e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e");
        return candidate;
    }

    private static ObjectNode urlNode(String url) {
        ObjectNode node = JSON.createObjectNode();
        node.put("url", url);
        return node;
    }

    private static void writeJson(Path path, JsonNode value) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, (JSON.writeValueAsString(value) + "\n").getBytes("UTF-8"));
    }

    private static void writeString(Path path, String value) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, value);
    }

    /** Minimal Python {@code Path.as_uri()} equivalent for plain absolute paths. */
    static String asUri(Path absolute) {
        String raw = absolute.toString();
        StringBuilder out = new StringBuilder("file://");
        for (char c : raw.toCharArray()) {
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '_' || c == '-' || c == '.' || c == '~' || c == '/') {
                out.append(c);
            } else {
                out.append('%').append(String.format("%02X", (int) c));
            }
        }
        return out.toString();
    }

    private static ObjectNode getTool(FakeMcpClient client, String name) {
        for (JsonNode tool : client.tools) {
            if (tool.get("name").asText().equals(name)) {
                return (ObjectNode) tool;
            }
        }
        throw new IllegalArgumentException("no tool " + name);
    }

    private static ObjectNode callResult(ArrayNode content, boolean isError) {
        ObjectNode result = JSON.createObjectNode();
        result.set("content", content);
        result.putNull("structuredContent");
        result.put("isError", isError);
        return result;
    }

    private static ObjectNode textCallResult(String text) {
        return textCallResult(text, false);
    }

    private static ObjectNode textCallResult(String text, boolean isError) {
        ArrayNode content = JSON.createArrayNode();
        ObjectNode block = JSON.createObjectNode();
        block.put("type", "text");
        block.put("text", text);
        content.add(block);
        return callResult(content, isError);
    }

    private static List<String> fieldNames(ObjectNode node) {
        List<String> names = new ArrayList<>();
        Iterator<String> iterator = node.fieldNames();
        while (iterator.hasNext()) {
            names.add(iterator.next());
        }
        return names;
    }

    private static List<String> texts(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(item -> values.add(item.asText()));
        return values;
    }

    /** Scripted in-process MCP client standing in for the frozen Graphify stdio server. */
    private static final class FakeMcpClient implements McpClient {
        JsonNode initResult;
        ArrayNode tools = JSON.createArrayNode();
        Map<String, JsonNode> callResults = new java.util.HashMap<>();

        static FakeMcpClient success() {
            FakeMcpClient client = new FakeMcpClient();
            ObjectNode init = JSON.createObjectNode();
            init.put("protocolVersion", "2025-11-25");
            init.putObject("capabilities");
            ObjectNode serverInfo = init.putObject("serverInfo");
            serverInfo.put("name", "fake-graphifyy");
            serverInfo.put("version", "0.1.14");
            client.initResult = init;
            for (String name : GraphifyLiveVerifier.EXPECTED_OPERATIONS) {
                ObjectNode tool = JSON.createObjectNode();
                tool.put("name", name);
                tool.put("description", "graphify " + name);
                ObjectNode schema = tool.putObject("inputSchema");
                schema.put("type", "object");
                schema.putObject("properties");
                client.tools.add(tool);
            }
            ObjectNode nodeProperties = (ObjectNode) getTool(client, "get_node")
                    .get("inputSchema").get("properties");
            nodeProperties.set("label", JSON.createObjectNode().put("type", "string"));
            ObjectNode properties = (ObjectNode) getTool(client, "shortest_path")
                    .get("inputSchema").get("properties");
            properties.set("source", JSON.createObjectNode().put("type", "string"));
            properties.set("target", JSON.createObjectNode().put("type", "string"));
            properties.set("max_hops", JSON.createObjectNode().put("type", "integer"));
            client.callResults.put("get_node",
                    textCallResult("Node: PetClinicApplication.java | kind=type | ..."));
            client.callResults.put("shortest_path", textCallResult(
                    "Shortest path (1 hops): PetClinicApplication.java --contains "
                            + "[EXTRACTED]--> PetClinicApplication"));
            return client;
        }

        @Override
        public JsonNode initialize() {
            return initResult;
        }

        @Override
        public void notifyInitialized() {
        }

        @Override
        public JsonNode listTools() {
            ObjectNode listed = JSON.createObjectNode();
            listed.set("tools", tools);
            return listed;
        }

        @Override
        public JsonNode callTool(String name, ObjectNode arguments) {
            return callResults.get(name);
        }

        @Override
        public void close() {
        }
    }
}

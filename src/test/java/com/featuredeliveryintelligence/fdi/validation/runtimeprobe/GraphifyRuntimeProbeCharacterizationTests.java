package com.featuredeliveryintelligence.fdi.validation.runtimeprobe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ports the characterization cases of the transitional Python
 * {@code tooling/validation/graphify_runtime_probe.py} {@code inspect_runtime}
 * contract: executable discovery fields and SHA-256, the
 * {@code DISCOVERED_NOT_VERIFIED} / {@code NOT_VERIFIED} /
 * {@code INTERFACE_DESCRIBED_NOT_SNAPSHOT_BOUND} statuses, the exported
 * descriptor validation rules, and the {@code "Graphify descriptor is
 * incomplete"} failure vocabulary.
 */
class GraphifyRuntimeProbeCharacterizationTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonNodeFactory NODE = JsonNodeFactory.instance;

    @TempDir Path temp;

    @Test
    void existingExecutableWithoutDescriptorMakesNoApiClaim() throws Exception {
        Path executable = temp.resolve("graphify");
        Files.write(executable, "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));

        GraphifyRuntimeProbeResult result = new GraphifyRuntimeProbe().inspectRuntime(executable, null);

        assertTrue(result.runtimeFound());
        assertEquals(executable.toRealPath().toString(), result.runtimePath());
        assertEquals(expectedSha256(executable), result.runtimeSha256());
        assertEquals("DISCOVERED_NOT_VERIFIED", result.verificationStatus());
        assertEquals(List.of(), result.supportedOperations());
        assertEquals(List.of(), result.apiAssumptions());
        assertNull(result.runtimeIdentity());
        assertNull(result.runtimeVersion());
        assertNull(result.transport());
        assertNull(result.wireVersion());
    }

    @Test
    void missingCommandReportsNotVerified() throws Exception {
        GraphifyRuntimeProbeResult result = new GraphifyRuntimeProbe()
                .inspectRuntime(temp.resolve("absent"), null);

        assertFalse(result.runtimeFound());
        assertNull(result.runtimePath());
        assertNull(result.runtimeSha256());
        assertEquals("NOT_VERIFIED", result.verificationStatus());
        assertEquals(List.of(), result.supportedOperations());
    }

    @Test
    void nullCommandReportsNotVerified() throws Exception {
        GraphifyRuntimeProbeResult result = new GraphifyRuntimeProbe().inspectRuntime(null, null);

        assertFalse(result.runtimeFound());
        assertEquals("NOT_VERIFIED", result.verificationStatus());
    }

    @Test
    void directoryCommandIsNotAFile() throws Exception {
        GraphifyRuntimeProbeResult result = new GraphifyRuntimeProbe().inspectRuntime(temp, null);

        assertFalse(result.runtimeFound());
        assertEquals("NOT_VERIFIED", result.verificationStatus());
    }

    @Test
    void validDescriptorWithoutCommandDescribesInterface() throws Exception {
        Path descriptor = writeDescriptor(validDescriptor());

        GraphifyRuntimeProbeResult result = new GraphifyRuntimeProbe().inspectRuntime(null, descriptor);

        assertFalse(result.runtimeFound());
        assertEquals("INTERFACE_DESCRIBED_NOT_SNAPSHOT_BOUND", result.verificationStatus());
        assertEquals("graphify-mcp", result.runtimeIdentity());
        assertEquals("0.9.0", result.runtimeVersion());
        assertEquals("mcp-stdio", result.transport());
        assertEquals("1.0", result.wireVersion());
        assertEquals(List.of("query_graph", "get_node"), result.supportedOperations());
    }

    @Test
    void validDescriptorWithCommandKeepsDiscoveryFields() throws Exception {
        Path executable = temp.resolve("graphify");
        Files.write(executable, "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        Path descriptor = writeDescriptor(validDescriptor());

        GraphifyRuntimeProbeResult result = new GraphifyRuntimeProbe().inspectRuntime(executable, descriptor);

        assertTrue(result.runtimeFound());
        assertEquals(expectedSha256(executable), result.runtimeSha256());
        assertEquals("INTERFACE_DESCRIBED_NOT_SNAPSHOT_BOUND", result.verificationStatus());
        assertEquals(List.of("query_graph", "get_node"), result.supportedOperations());
    }

    @Test
    void descriptorWithExtraKeysIgnoresThem() throws Exception {
        ObjectNode data = validDescriptor();
        data.put("unrelated", "ignored");
        Path descriptor = writeDescriptor(data);

        GraphifyRuntimeProbeResult result = new GraphifyRuntimeProbe().inspectRuntime(null, descriptor);

        assertEquals("INTERFACE_DESCRIBED_NOT_SNAPSHOT_BOUND", result.verificationStatus());
    }

    @Test
    void descriptorMissingIdentityIsIncomplete() throws Exception {
        ObjectNode data = validDescriptor();
        data.remove("runtime_identity");

        assertIncomplete(data);
    }

    @Test
    void descriptorEmptyStringIsIncomplete() throws Exception {
        ObjectNode data = validDescriptor();
        data.put("transport", "");

        assertIncomplete(data);
    }

    @Test
    void descriptorNonStringValueIsIncomplete() throws Exception {
        ObjectNode data = validDescriptor();
        data.put("wire_version", 2);

        assertIncomplete(data);
    }

    @Test
    void descriptorMissingOperationsIsIncomplete() throws Exception {
        ObjectNode data = validDescriptor();
        data.remove("supported_operations");

        assertIncomplete(data);
    }

    @Test
    void descriptorEmptyOperationsIsIncomplete() throws Exception {
        ObjectNode data = validDescriptor();
        data.remove("supported_operations");
        data.putArray("supported_operations");

        assertIncomplete(data);
    }

    @Test
    void descriptorNonListOperationsIsIncomplete() throws Exception {
        ObjectNode data = validDescriptor();
        data.remove("supported_operations");
        data.put("supported_operations", "query_graph");

        assertIncomplete(data);
    }

    @Test
    void descriptorNonStringOperationIsIncomplete() throws Exception {
        ObjectNode data = validDescriptor();
        data.remove("supported_operations");
        data.putArray("supported_operations").add(7);

        assertIncomplete(data);
    }

    @Test
    void descriptorEmptyStringOperationIsIncomplete() throws Exception {
        ObjectNode data = validDescriptor();
        data.remove("supported_operations");
        data.putArray("supported_operations").add("");

        assertIncomplete(data);
    }

    @Test
    void malformedDescriptorJsonRaisesPythonDecodeError() throws Exception {
        Path descriptor = temp.resolve("descriptor.json");
        Files.write(descriptor, "not json".getBytes(StandardCharsets.UTF_8));

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> new GraphifyRuntimeProbe().inspectRuntime(null, descriptor));

        assertEquals("Expecting value: line 1 column 1 (char 0)", failure.getMessage());
    }

    @Test
    void rendersPythonDumpsIndentTwoWithoutDescriptor() throws Exception {
        Path executable = temp.resolve("graphify");
        Files.write(executable, "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));

        GraphifyRuntimeProbeResult result = new GraphifyRuntimeProbe().inspectRuntime(executable, null);

        String expected = "{\n"
                + "  \"runtime_found\": true,\n"
                + "  \"runtime_path\": \"" + escape(executable.toRealPath().toString()) + "\",\n"
                + "  \"runtime_sha256\": \"" + expectedSha256(executable) + "\",\n"
                + "  \"verification_status\": \"DISCOVERED_NOT_VERIFIED\",\n"
                + "  \"supported_operations\": [],\n"
                + "  \"api_assumptions\": []\n"
                + "}\n";
        assertEquals(expected, new String(result.toJsonBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void rendersPythonDumpsIndentTwoWithDescriptor() throws Exception {
        Path descriptor = writeDescriptor(validDescriptor());

        GraphifyRuntimeProbeResult result = new GraphifyRuntimeProbe().inspectRuntime(null, descriptor);

        String expected = "{\n"
                + "  \"runtime_found\": false,\n"
                + "  \"runtime_path\": null,\n"
                + "  \"runtime_sha256\": null,\n"
                + "  \"verification_status\": \"INTERFACE_DESCRIBED_NOT_SNAPSHOT_BOUND\",\n"
                + "  \"supported_operations\": [\n"
                + "    \"query_graph\",\n"
                + "    \"get_node\"\n"
                + "  ],\n"
                + "  \"api_assumptions\": [],\n"
                + "  \"runtime_identity\": \"graphify-mcp\",\n"
                + "  \"runtime_version\": \"0.9.0\",\n"
                + "  \"transport\": \"mcp-stdio\",\n"
                + "  \"wire_version\": \"1.0\"\n"
                + "}\n";
        assertEquals(expected, new String(result.toJsonBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void portsEscapeSequencesLikePythonEnsureAscii() {
        assertEquals("a\\nb\\t\\\"\\u0001\\u00e9",
                GraphifyRuntimeProbeResult.escape("a\nb\t\"é"));
    }

    @Test
    void discoversExecutableOnPathDirectories() throws Exception {
        Path bin = Files.createDirectories(temp.resolve("bin"));
        Path graphify = bin.resolve("graphify");
        Files.write(graphify, "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        graphify.toFile().setExecutable(true);

        GraphifyRuntimeProbe probe = new GraphifyRuntimeProbe();
        assertEquals(graphify.toRealPath(), probe.discoverExecutable(List.of(bin, temp.resolve("absent-dir"))));
        assertNull(probe.discoverExecutable(List.of(temp.resolve("absent-dir"))));
    }

    @Test
    void discoversGraphifyCliFallbackOnPathDirectories() throws Exception {
        Path bin = Files.createDirectories(temp.resolve("bin"));
        Path fallback = bin.resolve("graphify-cli");
        Files.write(fallback, "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        fallback.toFile().setExecutable(true);

        GraphifyRuntimeProbe probe = new GraphifyRuntimeProbe();
        assertEquals(fallback.toRealPath(), probe.discoverExecutable(List.of(bin)));
    }

    @Test
    void skipsNonExecutableAndDirectoryMatches() throws Exception {
        Path bin = Files.createDirectories(temp.resolve("bin"));
        Path notExecutable = bin.resolve("graphify");
        Files.write(notExecutable, "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(bin.resolve("graphify-cli"));

        GraphifyRuntimeProbe probe = new GraphifyRuntimeProbe();
        assertNull(probe.discoverExecutable(List.of(bin)));
    }

    @Test
    void frozenDiscoveryArtifactIsNotAProbeDescriptor() {
        Path descriptor = Path.of("validation/pkb001/runtime/graphify-discovery.json");

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> new GraphifyRuntimeProbe().inspectRuntime(null, descriptor));

        assertEquals("Graphify descriptor is incomplete", failure.getMessage());
    }

    private void assertIncomplete(ObjectNode data) throws Exception {
        Path descriptor = writeDescriptor(data);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> new GraphifyRuntimeProbe().inspectRuntime(null, descriptor));

        assertEquals("Graphify descriptor is incomplete", failure.getMessage());
    }

    private Path writeDescriptor(ObjectNode data) throws Exception {
        Path descriptor = temp.resolve("descriptor-" + Math.abs(data.hashCode()) + ".json");
        Files.write(descriptor, JSON.writeValueAsBytes(data));
        return descriptor;
    }

    private static ObjectNode validDescriptor() {
        ObjectNode data = NODE.objectNode();
        data.put("runtime_identity", "graphify-mcp");
        data.put("runtime_version", "0.9.0");
        data.put("transport", "mcp-stdio");
        data.put("wire_version", "1.0");
        data.putArray("supported_operations").add("query_graph").add("get_node");
        return data;
    }

    private static String expectedSha256(Path file) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(Files.readAllBytes(file));
        StringBuilder out = new StringBuilder();
        for (byte value : hash) {
            out.append(String.format("%02x", value & 0xff));
        }
        return out.toString();
    }

    /** The expected-string tests embed the same {@code ensure_ascii=True} escape the renderer uses. */
    private static String escape(String value) {
        return GraphifyRuntimeProbeResult.escape(value);
    }
}

package com.featuredeliveryintelligence.fdi.validation.liveverifier;

import com.featuredeliveryintelligence.fdi.validation.codebaseline.PythonJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Proves Graphify's live MCP stdio interface against the frozen PKB-001
 * graph. Ports the observable behavior of the transitional Python consumer
 * {@code tooling/validation/graphify_live_verifier.py}: the frozen constants
 * (digests, revisions, expected operations, hops regex), the runtime path and
 * frozen-input validation with the exact {@link VerificationFailure}
 * messages, the installed-distribution metadata binding (versions and
 * {@code direct_url.json} against the frozen source {@code as_uri()}), the
 * MCP stdio session (spawn {@code <venv>/bin/python -m graphify.serve
 * graphify-out/<graphname>} in a fresh temp dir under {@code <root>/.fdi-work}
 * with a {@code graphify-out} symlink to the immutable artifact directory),
 * the two live queries with their exact failure messages, and the success
 * evidence object with the exact Python key order.
 *
 * <p>Residual uncertainty, because the frozen runtime is absent from this
 * worktree and the live path cannot be exercised end-to-end locally: the MCP
 * query results and tool catalog are re-serialized as
 * {@code {"content": [...], "structuredContent": ..., "isError": ...}} to
 * match mcp 1.29.1 {@code model_dump(mode="json")} shapes, {@code serverInfo}
 * is echoed from the initialize result as received, and the initialize request
 * pins the mcp 1.29.1 default client protocol version (the frozen runtime's
 * recorded wire evidence shows {@code MCP 2025-11-25}); JSON-RPC error
 * responses surface the error message like Python's {@code McpError}.
 */
public final class GraphifyLiveVerifier {
    static final String SOURCE_ARCHIVE_SHA256 =
            "8d806aa861e0ffa2136eda227d79d290dfdb89bf0c63fd00a4e2b4ea59d445";
    static final String ZIP_REVISION_COMMENT = "91f4d120b630ee35c79bf3c75ccd186870a808f9";
    static final String PETCLINIC_COMMIT = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    static final String PETCLINIC_INPUT_TREE = "f92df0b05c91c7d29d81e70cf86f8678b0545bd2";
    static final String PETCLINIC_GRAPH_SHA256 =
            "e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e";
    static final List<String> EXPECTED_OPERATIONS = List.of(
            "query_graph", "get_node", "get_neighbors", "get_community", "god_nodes",
            "graph_stats", "shortest_path");
    private static final Pattern HOPS = Pattern.compile("^Shortest path \\((\\d+) hops\\):");
    private static final String GRAPH_RELATIVE =
            "validation/pkb001/artifacts/petclinic-graph-818c413.json";
    private static final ObjectMapper JSON = new ObjectMapper();

    /** Ports {@code verify_live_interface(root)} over the real stdio MCP session. */
    public ObjectNode verifyLiveInterface(Path root) throws IOException {
        root = resolveRoot(root);
        RuntimePaths paths = runtimePaths(root);
        Path workDir = Files.createTempDirectory(root.resolve(".fdi-work"), "pkb001-graphify-mcp-");
        String serverGraphArgument = "graphify-out/" + paths.graph().getFileName();
        List<String> command = List.of(paths.runtimePython().toString(),
                "-m", "graphify.serve", serverGraphArgument);
        try {
            // Upstream accepts only paths below graphify-out/: point that directory
            // at the immutable artifact directory without copying graph bytes.
            Files.createSymbolicLink(workDir.resolve("graphify-out"), paths.graph().getParent());
            // Preflight (frozen inputs, versions, direct URL) runs before the
            // server spawn, mirroring the Python verification order.
            Preflight preflight = preflight(root, paths);
            try (StdioMcpClient client = new StdioMcpClient(command, workDir)) {
                return verifySession(paths, client, serverGraphArgument, preflight);
            }
        } finally {
            deleteRecursivelyQuietly(workDir);
        }
    }

    /** Test seam: verifies through the supplied MCP client instead of stdio. */
    ObjectNode verifyLiveInterface(Path root, McpClient client) throws IOException {
        root = resolveRoot(root);
        RuntimePaths paths = runtimePaths(root);
        String serverGraphArgument = "graphify-out/" + paths.graph().getFileName();
        Preflight preflight = preflight(root, paths);
        return verifySession(paths, client, serverGraphArgument, preflight);
    }

    private static Path resolveRoot(Path root) {
        return com.featuredeliveryintelligence.fdi.validation.readiness
                .Phase0Readiness.resolveLoose(root);
    }

    private record Preflight(String inputPolicySha256, String runtimeVersion,
            String mcpVersion, String directUrl) { }

    private static Preflight preflight(Path root, RuntimePaths paths) throws IOException {
        String inputPolicySha256 = validateFrozenInputs(root, paths.graph());
        String runtimeVersion = distributionVersion(paths, "graphifyy-0.1.14.dist-info", "graphifyy");
        String mcpVersion = distributionVersion(paths, "mcp-1.29.1.dist-info", "mcp");
        String directUrl = directUrl(paths);
        String expectedDirectUrl = asUri(paths.source());
        if (!directUrl.equals(expectedDirectUrl)) {
            throw new VerificationFailure(
                    "installed graphifyy direct URL does not bind the frozen source");
        }
        if (!"0.1.14".equals(runtimeVersion) || !"1.29.1".equals(mcpVersion)) {
            throw new VerificationFailure(
                    "installed Graphify or MCP version does not match the frozen runtime");
        }
        return new Preflight(inputPolicySha256, runtimeVersion, mcpVersion, directUrl);
    }

    private ObjectNode verifySession(RuntimePaths paths, McpClient client,
            String serverGraphArgument, Preflight preflight) throws IOException {
        String inputPolicySha256 = preflight.inputPolicySha256();
        String runtimeVersion = preflight.runtimeVersion();
        String mcpVersion = preflight.mcpVersion();
        String directUrl = preflight.directUrl();

        JsonNode initialization = client.initialize();
        client.notifyInitialized();
        JsonNode listed = client.listTools();
        ArrayNode toolCatalog = JSON.createArrayNode();
        List<String> operations = new ArrayList<>();
        if (listed != null && listed.get("tools") != null && listed.get("tools").isArray()) {
            for (JsonNode tool : listed.get("tools")) {
                if (tool == null || !tool.isObject()) {
                    throw new VerificationFailure(
                            "live MCP tool list differs from frozen expected operations");
                }
                ObjectNode normalized = JSON.createObjectNode();
                normalized.set("name", tool.get("name"));
                normalized.set("description", tool.get("description"));
                normalized.set("inputSchema", tool.get("inputSchema"));
                toolCatalog.add(normalized);
                operations.add(tool.get("name") == null ? "" : tool.get("name").asText());
            }
        }
        if (!operations.equals(EXPECTED_OPERATIONS)) {
            throw new VerificationFailure("live MCP tool list differs from frozen expected operations");
        }
        java.util.Map<String, JsonNode> schemas = new java.util.HashMap<>();
        for (JsonNode tool : toolCatalog) {
            schemas.put(tool.get("name").asText(), tool.get("inputSchema"));
        }
        requireToolSchema(schemas, "get_node", java.util.Set.of("label"));
        requireToolSchema(schemas, "shortest_path", java.util.Set.of("source", "target", "max_hops"));

        ObjectNode nodeArguments = JSON.createObjectNode();
        nodeArguments.put("label", "PetClinicApplication.java");
        JsonNode nodeResult = client.callTool("get_node", nodeArguments);
        String nodeText = textResult(nodeResult);
        if (isTrue(nodeResult == null ? null : nodeResult.get("isError"))
                || !nodeText.contains("Node: PetClinicApplication.java")) {
            throw new VerificationFailure(
                    "live get_node query did not resolve the frozen Petclinic node");
        }

        ObjectNode pathArguments = JSON.createObjectNode();
        pathArguments.put("source", "PetClinicApplication.java");
        pathArguments.put("target", "PetClinicApplication");
        pathArguments.put("max_hops", 1);
        JsonNode pathResult = client.callTool("shortest_path", pathArguments);
        String pathText = textResult(pathResult);
        Matcher hopsMatch = HOPS.matcher(pathText);
        if (isTrue(pathResult == null ? null : pathResult.get("isError")) || !hopsMatch.find()) {
            throw new VerificationFailure("live shortest_path query did not return a path");
        }
        int observedHops = Integer.parseInt(hopsMatch.group(1));
        if (observedHops <= 0 || observedHops > pathArguments.get("max_hops").asInt()) {
            throw new VerificationFailure("live shortest_path query exceeded requested max_hops");
        }
        String expectedPath =
                "PetClinicApplication.java --contains [EXTRACTED]--> PetClinicApplication";
        if (!pathText.contains(expectedPath)) {
            throw new VerificationFailure("live shortest_path query returned an unexpected path");
        }

        ObjectNode evidence = JSON.createObjectNode();
        evidence.put("verification_id", "pkb001-graphify-live-818c413");
        evidence.put("captured_at", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
        evidence.put("result", "EXACTLY_BOUND");
        evidence.put("queryable", true);
        evidence.put("runtime_identity", "graphifyy");
        evidence.put("runtime_version", runtimeVersion);
        evidence.put("runtime_python", paths.runtimePython().toString());
        evidence.put("transport", "MCP stdio");
        evidence.put("mcp_version", mcpVersion);
        String protocolVersion = initialization != null && initialization.get("protocolVersion") != null
                ? initialization.get("protocolVersion").asText() : "";
        evidence.put("wire_version", "MCP " + protocolVersion);
        evidence.set("server_info", initialization == null ? null : initialization.get("serverInfo"));
        ArrayNode serverCommand = evidence.putArray("server_command");
        serverCommand.add(paths.runtimePython().toString());
        serverCommand.add("-m");
        serverCommand.add("graphify.serve");
        serverCommand.add(serverGraphArgument);
        evidence.put("resolved_graph_path", paths.graph().toString());
        evidence.put("server_exit_status", "CLEAN_SESSION_CLOSE");
        evidence.putNull("server_error");
        ObjectNode provenance = evidence.putObject("source_provenance");
        provenance.put("source_archive_sha256", SOURCE_ARCHIVE_SHA256);
        provenance.put("zip_revision_comment", ZIP_REVISION_COMMENT);
        provenance.put("installed_direct_url", directUrl);
        ArrayNode supported = evidence.putArray("supported_operations");
        operations.forEach(supported::add);
        evidence.set("tool_catalog", toolCatalog);
        ObjectNode queries = evidence.putObject("queries");
        ObjectNode nodeQuery = queries.putObject("node_query");
        nodeQuery.put("tool", "get_node");
        nodeQuery.set("arguments", nodeArguments);
        nodeQuery.set("result", callResultDump(nodeResult));
        nodeQuery.put("is_error", false);
        ObjectNode shortestPath = queries.putObject("shortest_path");
        shortestPath.put("tool", "shortest_path");
        shortestPath.set("arguments", pathArguments);
        shortestPath.set("result", callResultDump(pathResult));
        shortestPath.put("returned_path", expectedPath);
        shortestPath.put("observed_hops", observedHops);
        evidence.put("exact_revision_opened", true);
        evidence.put("source_location_provenance",
                "git tree " + PETCLINIC_INPUT_TREE + " at immutable commit " + PETCLINIC_COMMIT);
        ObjectNode snapshotBinding = evidence.putObject("snapshot_binding");
        snapshotBinding.put("requested_revision", PETCLINIC_COMMIT);
        snapshotBinding.put("indexed_revision", PETCLINIC_COMMIT);
        snapshotBinding.put("input_git_tree_oid", PETCLINIC_INPUT_TREE);
        snapshotBinding.put("graph_sha256", PETCLINIC_GRAPH_SHA256);
        evidence.put("graph_sha256", PETCLINIC_GRAPH_SHA256);
        evidence.put("input_policy_sha256", inputPolicySha256);
        ObjectNode structuralProof = evidence.putObject("structural_proof");
        structuralProof.put("node_query", true);
        structuralProof.put("path_query", true);
        ArrayNode limitations = evidence.putArray("limitations");
        limitations.add("Graphify graph.json does not embed Git revision metadata; the binding "
                + "is verified against the frozen calibration candidate and graph digest.");
        limitations.add("The upstream graph path guard requires graphify-out/; a temporary "
                + "symlink targets the immutable artifact without copying its bytes.");
        limitations.add("Only deterministic Java AST extraction was used; no semantic LLM "
                + "extraction was executed.");
        return evidence;
    }

    private record RuntimePaths(Path runtimePython, Path graph, Path source) { }

    private static RuntimePaths runtimePaths(Path root) {
        Path runtimePython = root.resolve(".fdi-work/graphify-venv312/bin/python");
        Path graph = root.resolve(GRAPH_RELATIVE);
        Path source = root.resolve(".fdi-work/graphify-source/graphify-main");
        if (!Files.isRegularFile(runtimePython)) {
            throw new VerificationFailure("Graphify runtime is missing: " + runtimePython);
        }
        if (!Files.isRegularFile(graph)) {
            throw new VerificationFailure("frozen Petclinic graph is missing: " + graph);
        }
        if (!Files.isDirectory(source)) {
            throw new VerificationFailure("Graphify frozen source is missing: " + source);
        }
        try {
            return new RuntimePaths(runtimePython, graph.toRealPath(), source.toRealPath());
        } catch (IOException failure) {
            throw new VerificationFailure(failure.getMessage());
        }
    }

    private static String validateFrozenInputs(Path root, Path graph) throws IOException {
        ObjectNode candidate = loadJsonObject(
                root.resolve("validation/pkb001/datasets/petclinic-calibration-candidate.json"));
        JsonNode graphifyInput = candidate.get("graphify_input");
        JsonNode graphify = candidate.get("graphify");
        if (graphifyInput == null || !graphifyInput.isObject()
                || graphify == null || !graphify.isObject()) {
            throw new VerificationFailure("Petclinic calibration binding is incomplete");
        }
        List<java.util.Map.Entry<String, Boolean>> checks = new ArrayList<>();
        checks.add(java.util.Map.entry("candidate source commit",
                textEquals(candidate.get("source_commit_sha"), PETCLINIC_COMMIT)));
        checks.add(java.util.Map.entry("candidate input Git tree",
                textEquals(graphifyInput.get("git_tree_oid"), PETCLINIC_INPUT_TREE)));
        checks.add(java.util.Map.entry("candidate graph path",
                textEquals(graphify.get("artifact_path"), GRAPH_RELATIVE)));
        checks.add(java.util.Map.entry("candidate graph SHA-256",
                textEquals(graphify.get("artifact_sha256"), PETCLINIC_GRAPH_SHA256)));
        checks.add(java.util.Map.entry("frozen graph SHA-256",
                sha256Hex(readBytes(graph)).equals(PETCLINIC_GRAPH_SHA256)));
        List<String> failed = new ArrayList<>();
        for (java.util.Map.Entry<String, Boolean> check : checks) {
            if (!check.getValue()) {
                failed.add(check.getKey());
            }
        }
        if (!failed.isEmpty()) {
            throw new VerificationFailure("frozen input validation failed: " + String.join(", ", failed));
        }
        ObjectNode phase0 = loadJsonObject(
                root.resolve("validation/pkb001/datasets/phase0-evidence.json"));
        JsonNode existing = phase0.get("graphify");
        if (existing == null || !existing.isObject()
                || existing.get("input_policy_sha256") == null
                || !existing.get("input_policy_sha256").isTextual()) {
            throw new VerificationFailure("Phase 0 Graphify input policy digest is absent");
        }
        return existing.get("input_policy_sha256").asText();
    }

    private static String distributionVersion(RuntimePaths paths, String distInfo, String packageName)
            throws IOException {
        Path metadata = sitePackages(paths).resolve(distInfo).resolve("METADATA");
        if (!Files.isRegularFile(metadata)) {
            // mirrors importlib.metadata.PackageNotFoundError(package) str()
            throw new VerificationFailure(packageName);
        }
        for (String line : new String(readBytes(metadata), StandardCharsets.UTF_8).split("\n")) {
            if (line.startsWith("Version:")) {
                return line.substring("Version:".length()).trim();
            }
        }
        throw new VerificationFailure(packageName);
    }

    private static String directUrl(RuntimePaths paths) throws IOException {
        Path directUrlFile = sitePackages(paths)
                .resolve("graphifyy-0.1.14.dist-info/direct_url.json");
        if (!Files.isRegularFile(directUrlFile)) {
            throw new VerificationFailure("graphifyy direct_url.json is absent");
        }
        ObjectNode data = loadJsonObject(directUrlFile);
        JsonNode url = data.get("url");
        if (url == null || !url.isTextual() || url.asText().isEmpty()) {
            throw new VerificationFailure("graphifyy direct URL is absent");
        }
        return url.asText();
    }

    private static Path sitePackages(RuntimePaths paths) {
        return paths.runtimePython().getParent().getParent()
                .resolve("lib/python3.12/site-packages");
    }

    private static ObjectNode loadJsonObject(Path path) throws IOException {
        JsonNode value = PythonJson.readTree(readBytes(path));
        if (value == null || !value.isObject()) {
            throw new VerificationFailure(path + " must contain a JSON object");
        }
        return (ObjectNode) value;
    }

    /** Python {@code Path.read_bytes()} equivalent: directory reads carry the errno-21 surface. */
    static byte[] readBytes(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            throw new java.nio.file.FileSystemException(path.toString(), null, "Is a directory");
        }
        return Files.readAllBytes(path);
    }

    private static void requireToolSchema(java.util.Map<String, JsonNode> catalog, String name,
            java.util.Set<String> fields) {
        JsonNode schema = catalog.get(name);
        JsonNode properties = schema != null && schema.isObject() ? schema.get("properties") : null;
        if (properties == null || !properties.isObject() || !allFieldsPresent(properties, fields)) {
            throw new VerificationFailure(
                    "live MCP tool " + name + " does not expose required arguments");
        }
    }

    private static boolean allFieldsPresent(JsonNode properties, java.util.Set<String> fields) {
        for (String field : fields) {
            if (!properties.has(field)) {
                return false;
            }
        }
        return true;
    }

    private static String textResult(JsonNode result) {
        JsonNode content = result == null ? null : result.get("content");
        if (content == null || !content.isArray() || content.isEmpty()) {
            throw new VerificationFailure("MCP tool returned no content");
        }
        JsonNode first = content.get(0);
        JsonNode text = first != null && first.isObject() ? first.get("text") : null;
        if (text == null || !text.isTextual()) {
            throw new VerificationFailure("MCP tool did not return text content");
        }
        return text.asText();
    }

    /** Re-serializes a CallTool result like mcp 1.29.1 {@code model_dump(mode="json")}. */
    private static ObjectNode callResultDump(JsonNode result) {
        ObjectNode dump = JSON.createObjectNode();
        dump.set("content", result != null && result.get("content") != null
                ? result.get("content") : JSON.createArrayNode());
        dump.set("structuredContent", result != null ? result.get("structuredContent") : null);
        boolean isError = result != null && result.get("isError") != null
                && result.get("isError").isBoolean() && result.get("isError").asBoolean();
        dump.set("isError", BooleanNode.valueOf(isError));
        return dump;
    }

    /** Python {@code Path.as_uri()} for a resolved absolute path (percent-encodes like urllib quote with safe='/'). */
    static String asUri(Path absolute) {
        String raw = absolute.toString();
        StringBuilder out = new StringBuilder("file://");
        for (byte b : raw.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xff);
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '_' || c == '-' || c == '.' || c == '~' || c == '/') {
                out.append(c);
            } else {
                out.append('%').append(String.format("%02X", (int) c));
            }
        }
        return out.toString();
    }

    private static boolean textEquals(JsonNode node, String expected) {
        return node != null && node.isTextual() && node.asText().equals(expected);
    }

    private static boolean isTrue(JsonNode node) {
        return node != null && node.isBoolean() && node.asBoolean();
    }

    static String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static void deleteRecursively(Path directory) throws IOException {
        try (Stream<Path> walk = Files.walk(directory)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static void deleteRecursivelyQuietly(Path directory) {
        try {
            deleteRecursively(directory);
        } catch (IOException ignored) {
            // mirrors tempfile.TemporaryDirectory best-effort cleanup
        }
    }
}

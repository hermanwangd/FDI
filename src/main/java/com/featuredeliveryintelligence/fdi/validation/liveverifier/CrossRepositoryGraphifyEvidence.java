package com.featuredeliveryintelligence.fdi.validation.liveverifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.featuredeliveryintelligence.fdi.structural.api.CodeIntelligenceProvider;
import com.featuredeliveryintelligence.fdi.structural.graphify.GraphifyAdapter;
import com.featuredeliveryintelligence.fdi.structural.graphify.GraphifyBindingAttestor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.List;
import java.util.TreeMap;
import java.util.ArrayList;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/** Exact-source external Graphify indexing and provider verification. */
public final class CrossRepositoryGraphifyEvidence {
    private static final ObjectMapper JSON = new ObjectMapper();
    // Thin invocation of the installed external provider's actual public Python APIs.
    // Extraction/build/export remain external Graphify behavior; Java owns evidence and validation.
    private static final String INDEX = """
            import sys, json, importlib.metadata
            from pathlib import Path
            from graphify.extract import collect_files, extract
            from graphify.build import build_from_json
            from graphify.export import to_json
            import graphify
            files = collect_files(Path('src/main/java')) + collect_files(Path('src/test/java'))
            to_json(build_from_json(extract(files)), {}, sys.argv[1])
            print(json.dumps({'runtime_version': importlib.metadata.version('graphifyy'),
                              'module_path': str(Path(graphify.__file__).parent.resolve())}))
            """;
    private CrossRepositoryGraphifyEvidence() { }
    public static void main(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("usage: <python> <source> <revision> <repository-id> <new-output>");
        Path output = Path.of(args[4]).toAbsolutePath().normalize();
        require(!Files.exists(output, LinkOption.NOFOLLOW_LINKS), "OUTPUT_EXISTS");
        String revision = args[2], repository = args[3];
        require(revision.matches("[0-9a-f]{40}") && repository.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"), "SOURCE_IDENTITY_REQUIRED");
        Path python = Path.of(args[0]).toAbsolutePath().normalize();
        Path source = Path.of(args[1]).toRealPath();
        require(Files.isExecutable(python), "RUNTIME_REQUIRED");
        output = output.getParent().toRealPath().resolve(output.getFileName());
        require(!output.startsWith(source), "OUTPUT_INSIDE_SOURCE");
        verifySource(source, revision);
        Map<String, String> sourceDigests = sourceDigests(source);
        String pythonSha = digest(python);
        Files.createDirectory(output);
        Path indexSource = stageSource(source, sourceDigests, output.resolve("index-source"));
        Path graph = Files.createDirectory(output.resolve("graphify-out")).resolve("graph.json");
        Instant start = Instant.now();
        Path log = output.resolve("index-runtime.json");
        run(List.of(python.toString(), "-I", "-c", INDEX, graph.toString()), indexSource, log, output.resolve("index.stderr"));
        require(Files.size(graph) <= 16L * 1024 * 1024, "GRAPH_TOO_LARGE");
        JsonNode graphDocument = JSON.readTree(graph.toFile());
        String label = queryLabel(graphDocument), graphSha = digest(graph);
        JsonNode runtime = JSON.readTree(log.toFile());
        String version = runtime.required("runtime_version").asText();
        require(!version.isBlank(), "RUNTIME_VERSION_REQUIRED");
        verifySource(source, revision);
        require(sourceDigests.equals(sourceDigests(source)), "SOURCE_CHANGED");
        require(sourceDigests.equals(sourceDigests(indexSource)), "INDEX_SOURCE_CHANGED");

        Map<String, Object> snapshot = Map.of("snapshot_id", "crossrepo:" + graphSha,
                "provider_scope_id", repository, "provider_ref", revision, "repositories",
                List.of(Map.of("repository_id", repository, "canonical_revision", revision)));
        ObjectNode evidence = JSON.createObjectNode();
        try (var client = new StdioMcpClient(List.of(python.toString(), "-I", "-m", "graphify.serve", "graphify-out/graph.json"), output)) {
            JsonNode initialization = client.initialize(); client.notifyInitialized();
            JsonNode catalog = client.listTools();
            boolean found = false;
            for (JsonNode tool : catalog.path("tools"))
                if ("get_node".equals(tool.path("name").asText())
                        && tool.path("inputSchema").path("properties").has("label")) found = true;
            require(found, "ACTUAL_GET_NODE_API_REQUIRED");
            String wire = initialization.required("protocolVersion").asText();
            var attestor = new GraphifyBindingAttestor(ignored -> Map.of(
                    "queryable", true, "runtime_version", version, "wire_version", wire,
                    "repository_bindings", List.of(Map.of("repository_id", repository,
                            "indexed_revision", revision, "head_revision", revision))), "crossrepo-001");
            CodeIntelligenceProvider provider = new GraphifyAdapter((tool, payload) -> {
                try {
                    JsonNode response = client.callTool(tool, JSON.createObjectNode().put("label", label));
                    require(!response.path("isError").asBoolean(), "LIVE_QUERY_FAILED");
                    return Map.of("response", response);
                } catch (java.io.IOException error) { throw new IllegalArgumentException("LIVE_QUERY_FAILED", error); }
            }, attestor, Map.of("FIND", "get_node"), (operation, raw) -> raw);
            var query = new TreeMap<String, Object>();
            query.put("snapshot_id", snapshot.get("snapshot_id")); query.put("operation", "FIND");
            for (String bound : List.of("max_depth", "max_nodes", "max_edges", "max_paths")) query.put(bound, 1);
            query.put("max_result_bytes", 65536);
            JsonNode response = (JsonNode) provider.find(query, snapshot).get("response");
            StringBuilder text = new StringBuilder();
            response.path("content").forEach(item -> text.append(item.path("text").asText()));
            require(text.toString().contains("Node: " + label + "\n"), "LIVE_NODE_NOT_FOUND");
            evidence.set("tool_catalog", catalog);
            evidence.set("query_result", response);
            evidence.set("server_initialization", initialization);
            evidence.put("query_label", label);
        }
        verifySource(source, revision);
        require(sourceDigests.equals(sourceDigests(source)) && graphSha.equals(digest(graph))
                && pythonSha.equals(digest(python)), "EVIDENCE_CHANGED");
        require(sourceDigests.equals(sourceDigests(indexSource)), "INDEX_SOURCE_CHANGED");
        evidence.put("result", "EXACTLY_BOUND").put("queryable", true).put("exact_revision_opened", true)
                .put("repository_id", repository).put("canonical_revision", revision).put("graph_sha256", graphSha)
                .put("runtime_identity", "graphifyy").put("runtime_version", version)
                .put("runtime_python", python.toString()).put("python_sha256", pythonSha)
                .put("started_at", start.toString()).put("finished_at", Instant.now().toString())
                .put("node_count", graphDocument.path("nodes").size()).put("edge_count", graphDocument.path("links").size())
                .put("semantic_publication_allowed", false).put("index_mode", "EXTERNAL_GRAPHIFY_AST_ONLY_NO_CLUSTERING");
        evidence.set("source_digests", JSON.valueToTree(sourceDigests));
        Files.write(output.resolve("runtime-evidence.json"), JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(evidence),
                StandardOpenOption.CREATE_NEW);
    }
    static String queryLabel(JsonNode graph) {
        require(graph != null && graph.path("nodes").isArray() && graph.path("links").isArray(), "INVALID_GRAPH");
        var labels = new java.util.TreeSet<String>();
        graph.path("nodes").forEach(node -> {
            String label = node.path("label").asText();
            if (label.matches("[A-Za-z0-9_$.-]+\\.java") && label.length() <= 200) labels.add(label);
        });
        require(!labels.isEmpty(), "QUERYABLE_GRAPH_REQUIRED");
        return labels.first();
    }
    static Path stageSource(Path source, Map<String, String> digests, Path target) throws Exception {
        Files.createDirectory(target);
        for (var entry : digests.entrySet()) {
            require(entry.getKey().matches("src/(main|test)/java/[A-Za-z0-9_./-]+\\.java")
                    && !List.of(entry.getKey().split("/")).contains(".."), "SOURCE_PATH");
            Path input = source.resolve(entry.getKey()), output = target.resolve(entry.getKey());
            rejectSourceSymlinks(source, entry.getKey());
            require(!Files.isSymbolicLink(input) && entry.getValue().equals(digest(input)), "SOURCE_CHANGED");
            Files.createDirectories(output.getParent()); Files.copy(input, output);
            require(entry.getValue().equals(digest(output)), "INDEX_SOURCE_CHANGED");
        }
        return target;
    }
    private static Map<String, String> sourceDigests(Path source) throws Exception {
        Map<String, String> result = new TreeMap<>();
        for (String root : List.of("src/main/java", "src/test/java")) {
            rejectSourceSymlinks(source, root);
            try (var walk = Files.walk(source.resolve(root))) {
                for (Path path : walk.toList()) {
                    require(!Files.isSymbolicLink(path), "SOURCE_SYMLINK");
                    if (Files.isRegularFile(path) && path.toString().endsWith(".java")) {
                        require(Files.size(path) <= 1024 * 1024 && result.size() < 10000, "SOURCE_TOO_LARGE");
                        result.put(source.relativize(path).toString(), digest(path));
                    }
                }
            }
        }
        require(!result.isEmpty(), "SOURCE_REQUIRED");
        return result;
    }
    private static void rejectSourceSymlinks(Path source, String relative) {
        Path current = source;
        for (Path part : Path.of(relative)) {
            current = current.resolve(part);
            require(!Files.isSymbolicLink(current), "SOURCE_SYMLINK");
        }
    }
    private static void verifySource(Path source, String revision) throws Exception {
        require(git(source, "rev-parse", "HEAD").trim().equals(revision)
                && git(source, "status", "--porcelain", "--untracked-files=all").isBlank(), "EXACT_CLEAN_SOURCE_REQUIRED");
    }
    private static String git(Path source, String... args) throws Exception {
        var command = new ArrayList<>(List.of("git", "-C", source.toString())); command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        if (!process.waitFor(10, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new IllegalArgumentException("GIT_TIMEOUT"); }
        byte[] bytes = process.getInputStream().readNBytes(65537);
        require(process.exitValue() == 0 && bytes.length <= 65536, "GIT_FAILURE");
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
    private static void run(List<String> command, Path cwd, Path stdout, Path stderr) throws Exception {
        Process process = new ProcessBuilder(command).directory(cwd.toFile())
                .redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
        try {
            require(process.waitFor(180, TimeUnit.SECONDS), "GRAPHIFY_INDEX_TIMEOUT");
            require(process.exitValue() == 0, "GRAPHIFY_INDEX_FAILED");
            require(Files.size(stdout) <= 65536 && Files.size(stderr) <= 65536, "RUNTIME_LOG_TOO_LARGE");
        } finally { if (process.isAlive()) process.destroyForcibly(); }
    }
    private static String digest(Path path) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        try (var stream = Files.newInputStream(path)) {
            byte[] buffer = new byte[65536];
            for (int count; (count = stream.read(buffer)) >= 0;) sha.update(buffer, 0, count);
        }
        return HexFormat.of().formatHex(sha.digest());
    }
    private static void require(boolean condition, String error) {
        if (!condition) throw new IllegalArgumentException(error);
    }
}

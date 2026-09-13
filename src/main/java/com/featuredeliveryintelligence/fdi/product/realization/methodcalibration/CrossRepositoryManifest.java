package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.HashSet;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import static com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.SfBl002RouteEffectivenessRun.*;

/** Frozen input identity for the independently selected cross-repository run. */
record CrossRepositoryManifest(String repositoryId, String sourceRevision,
        String semanticsSha256, String executionId, Map<String, String> inputs) {
    private static final Set<String> FIELDS = Set.of("schemaVersion", "repositoryId", "sourceRevision",
            "semanticsSha256", "executionId", "inputs");
    private static final Set<String> PATHS = Set.of(INTENTS_PATH, INTENT_ACCEPTANCE_PATH,
            TEST_EVIDENCE_PATH, GRAPH_PATH, RUNTIME_EVIDENCE_PATH);
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    CrossRepositoryManifest {
        require(repositoryId != null && repositoryId.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"));
        require(sourceRevision != null && sourceRevision.matches("[0-9a-f]{40}"));
        require(semanticsSha256 != null && semanticsSha256.matches("[0-9a-f]{64}"));
        require(executionId != null && executionId.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"));
        require(inputs != null && inputs.keySet().equals(PATHS));
        inputs.values().forEach(value -> require(value != null && value.matches("[0-9a-f]{64}")));
        inputs = java.util.Collections.unmodifiableMap(new TreeMap<>(inputs));
    }

    static CrossRepositoryManifest load(Path path, String expectedSha256) {
        try {
            require(expectedSha256 != null && expectedSha256.matches("[0-9a-f]{64}"));
            Path absolute = path.toAbsolutePath().normalize();
            for (Path part = absolute; part != null; part = part.getParent()) require(!Files.isSymbolicLink(part));
            require(Files.isRegularFile(absolute, LinkOption.NOFOLLOW_LINKS) && Files.size(absolute) <= 65536);
            byte[] bytes;
            try (var stream = Files.newInputStream(absolute)) { bytes = stream.readNBytes(65537); }
            require(bytes.length <= 65536);
            require(expectedSha256.equals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))));
            JsonNode node = JSON.readTree(bytes);
            require(node != null && node.isObject());
            Set<String> fields = new HashSet<>(); node.fieldNames().forEachRemaining(fields::add);
            require(fields.equals(FIELDS));
            require("SFBL005-CROSSREPO-INPUT-001".equals(text(node, "schemaVersion")));
            JsonNode inputNode = node.get("inputs"); require(inputNode.isObject());
            Map<String, String> inputs = new TreeMap<>();
            inputNode.fieldNames().forEachRemaining(key -> inputs.put(key, text(inputNode, key)));
            return new CrossRepositoryManifest(text(node, "repositoryId"), text(node, "sourceRevision"),
                    text(node, "semanticsSha256"), text(node, "executionId"), inputs);
        } catch (IllegalArgumentException error) { throw error; }
        catch (Exception error) { throw new IllegalArgumentException("INVALID_CROSS_REPOSITORY_MANIFEST", error); }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field); require(value != null && value.isTextual());
        return value.textValue();
    }

    private static void require(boolean condition) {
        if (!condition) throw new IllegalArgumentException("INVALID_CROSS_REPOSITORY_MANIFEST");
    }
}

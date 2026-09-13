package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;
import static com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.SfBl002RouteEffectivenessRun.*;

class CrossRepositoryManifestTests {
    @TempDir Path root;
    private final ObjectMapper json = new ObjectMapper();

    @org.junit.jupiter.api.BeforeEach void canonicalTemporaryRoot() throws Exception {
        // macOS /var is itself a symlink; production requires canonical input paths.
        root = root.toRealPath();
    }

    @Test void acceptsIndependentRepositoryIdentityAndExactlyFiveDigests() throws Exception {
        var manifest = load(valid());
        assertEquals("realworld", manifest.repositoryId());
        assertEquals("a".repeat(40), manifest.sourceRevision());
        assertEquals(5, manifest.inputs().size());
        assertThrows(UnsupportedOperationException.class, () -> manifest.inputs().clear());
    }

    @Test void rejectsUnknownFieldsWrongTypesMissingFieldsAndBadIdentities() throws Exception {
        var extra = valid().put("truth", "evaluator/truth.json");
        assertThrows(IllegalArgumentException.class, () -> load(extra));
        for (String field : new String[]{"schemaVersion", "repositoryId", "sourceRevision", "semanticsSha256", "executionId", "inputs"}) {
            var missing = valid(); missing.remove(field);
            assertThrows(IllegalArgumentException.class, () -> load(missing), field);
            var wrong = valid().put(field, 123);
            assertThrows(IllegalArgumentException.class, () -> load(wrong), field);
        }
        assertThrows(IllegalArgumentException.class, () -> load(valid().put("sourceRevision", "a".repeat(7))));
        assertThrows(IllegalArgumentException.class, () -> load(valid().put("schemaVersion", "future")));
        assertThrows(IllegalArgumentException.class, () -> load(valid().put("semanticsSha256", "A".repeat(64))));
    }

    @Test void rejectsExtraMissingOrMalformedInputDigests() throws Exception {
        var extra = valid(); ((ObjectNode) extra.get("inputs")).put("truth.json", "b".repeat(64));
        assertThrows(IllegalArgumentException.class, () -> load(extra));
        var missing = valid(); ((ObjectNode) missing.get("inputs")).remove(GRAPH_PATH);
        assertThrows(IllegalArgumentException.class, () -> load(missing));
        var malformed = valid(); ((ObjectNode) malformed.get("inputs")).put(GRAPH_PATH, "not-sha");
        assertThrows(IllegalArgumentException.class, () -> load(malformed));
    }

    @Test void rejectsChangedManifestSymlinkOversizeAndDuplicateFields() throws Exception {
        Path path = root.resolve("manifest.json");
        Files.writeString(path, valid().toString());
        assertThrows(IllegalArgumentException.class, () -> CrossRepositoryManifest.load(path, "0".repeat(64)));
        Path link = root.resolve("link.json"); Files.createSymbolicLink(link, path);
        assertThrows(IllegalArgumentException.class, () -> CrossRepositoryManifest.load(link, sha(path)));
        Files.writeString(path, " ".repeat(65537));
        assertThrows(IllegalArgumentException.class, () -> CrossRepositoryManifest.load(path, sha(path)));
        Files.writeString(path, valid().toString().replaceFirst("\\{", "{\"repositoryId\":\"other\","));
        assertThrows(IllegalArgumentException.class, () -> CrossRepositoryManifest.load(path, sha(path)));
    }

    private CrossRepositoryManifest load(ObjectNode node) throws Exception {
        Path path = root.resolve("manifest.json"); Files.writeString(path, node.toString());
        return CrossRepositoryManifest.load(path, sha(path));
    }

    private ObjectNode valid() {
        var node = json.createObjectNode().put("schemaVersion", "SFBL005-CROSSREPO-INPUT-001")
                .put("repositoryId", "realworld").put("sourceRevision", "a".repeat(40))
                .put("semanticsSha256", "b".repeat(64)).put("executionId", "REALWORLD-001");
        var inputs = node.putObject("inputs");
        for (String path : new String[]{INTENTS_PATH, INTENT_ACCEPTANCE_PATH, TEST_EVIDENCE_PATH, GRAPH_PATH, RUNTIME_EVIDENCE_PATH})
            inputs.put(path, "c".repeat(64));
        return node;
    }

    private String sha(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}

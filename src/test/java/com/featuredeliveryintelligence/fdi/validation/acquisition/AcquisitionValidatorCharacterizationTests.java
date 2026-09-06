package com.featuredeliveryintelligence.fdi.validation.acquisition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ports the acquisition characterization cases of
 * {@code tests/test_pkb001_phase0.py} to the Java {@link AcquisitionValidator}
 * and adds supplementary parity probes over the full Python
 * {@code pkb001_acquisition.py} {@code ValueError} vocabulary: bounded relative
 * paths, retained-tree SHA-256, timestamps, manifest field checks, frozen
 * limits, deterministic output, and failure ordering.
 */
class AcquisitionValidatorCharacterizationTests {
    private static final JsonNodeFactory NODE = JsonNodeFactory.instance;
    private static final String SHA = "a".repeat(40);
    /** Reference digest produced by the Python {@code tree_sha256} on this exact fixture. */
    private static final String REFERENCE_TREE_SHA256 =
            "3433984bc0b09a7d3ebb1b99da82abca70b896912496258c4878d853bcd98c24";
    private static final List<String> RETAINED = List.of("README.md", "src/App.java");

    @TempDir Path root;

    @BeforeEach
    void writeFixtureTree() throws Exception {
        Files.write(root.resolve("README.md"), "example".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(root.resolve("src"));
        Files.write(root.resolve("src/App.java"),
                "final class App {}".getBytes(StandardCharsets.UTF_8));
    }

    private AcquisitionValidator validator() {
        return new AcquisitionValidator();
    }

    private static ObjectNode validManifest(Path root, List<String> retained) {
        ObjectNode manifest = NODE.objectNode();
        manifest.put("source_commit_sha", SHA);
        ArrayNode retainedArray = manifest.putArray("retained_paths");
        retained.forEach(retainedArray::add);
        manifest.put("source_tree_sha256",
                new AcquisitionValidator().treeSha256(root, retained));
        manifest.put("acquired_at", "2026-09-04T00:00:00Z");
        manifest.put("acquisition_method", "git fetch by immutable commit");
        manifest.put("history_source", "https://api.github.com/repos/example/project");
        manifest.put("history_cutoff", "2026-09-04T00:00:00Z");
        manifest.put("post_cutoff_knowledge_policy", "EXCLUDE_AFTER_CUTOFF");
        manifest.put("license", "AGPL-3.0-only");
        manifest.put("max_repository_bytes", 1024);
        manifest.put("max_file_count", 10);
        manifest.put("max_file_bytes", 512);
        return manifest;
    }

    private IllegalArgumentException failure(ObjectNode manifest) {
        return assertThrows(IllegalArgumentException.class,
                () -> validator().validateAcquisition(root, manifest));
    }

    @Test
    void validatesExactBoundedTree() {
        ObjectNode result = validator().validateAcquisition(root, validManifest(root, RETAINED));
        assertEquals("VALIDATED", result.get("status").asText());
        assertEquals(SHA, result.get("source_commit_sha").asText());
        assertEquals(REFERENCE_TREE_SHA256, result.get("source_tree_sha256").asText());
        assertEquals(2, result.get("file_count").asInt());
        assertEquals(25, result.get("repository_bytes").asLong());
    }

    @Test
    void resultFieldOrderMatchesPythonDictOrder() {
        ObjectNode result = validator().validateAcquisition(root, validManifest(root, RETAINED));
        List<String> fields = new ArrayList<>();
        result.fieldNames().forEachRemaining(fields::add);
        assertEquals(List.of("status", "source_commit_sha", "source_tree_sha256",
                "file_count", "repository_bytes"), fields);
    }

    @Test
    void treeSha256MatchesPythonReferenceDigest() {
        assertEquals(REFERENCE_TREE_SHA256, validator().treeSha256(root, RETAINED));
    }

    @Test
    void treeSha256IsIndependentOfInputOrderAndSortsPaths() {
        assertEquals(REFERENCE_TREE_SHA256,
                validator().treeSha256(root, List.of("src/App.java", "README.md")));
    }

    @Test
    void rejectsMutableRevision() {
        ObjectNode manifest = validManifest(root, RETAINED);
        manifest.put("source_commit_sha", "main");
        assertEquals("source_commit_sha must be a lowercase 40-character Git SHA",
                failure(manifest).getMessage());
    }

    @Test
    void rejectsNonStringAndUppercaseRevision() {
        ObjectNode manifest = validManifest(root, RETAINED);
        manifest.put("source_commit_sha", "A".repeat(40));
        assertEquals("source_commit_sha must be a lowercase 40-character Git SHA",
                failure(manifest).getMessage());
        manifest.put("source_commit_sha", 42);
        assertEquals("source_commit_sha must be a lowercase 40-character Git SHA",
                failure(manifest).getMessage());
    }

    @Test
    void revisionCheckPrecedesRetainedPathChecks() {
        ObjectNode manifest = validManifest(root, RETAINED);
        manifest.put("source_commit_sha", "main");
        ArrayNode retained = (ArrayNode) manifest.get("retained_paths");
        retained.removeAll();
        retained.add("../outside");
        assertEquals("source_commit_sha must be a lowercase 40-character Git SHA",
                failure(manifest).getMessage());
    }

    @Test
    void rejectsTreeDigestMismatch() {
        ObjectNode manifest = validManifest(root, RETAINED);
        manifest.put("source_tree_sha256", "0".repeat(64));
        assertEquals("source tree digest mismatch", failure(manifest).getMessage());
    }

    @Test
    void rejectsMissingAndMalformedExpectedTreeDigest() {
        ObjectNode manifest = validManifest(root, RETAINED);
        manifest.remove("source_tree_sha256");
        assertEquals("source tree digest mismatch", failure(manifest).getMessage());
        manifest.put("source_tree_sha256", "not-a-digest");
        assertEquals("source tree digest mismatch", failure(manifest).getMessage());
        manifest.put("source_tree_sha256", "F".repeat(64));
        assertEquals("source tree digest mismatch", failure(manifest).getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"../outside", "/tmp/outside", ".git/config"})
    void rejectsUnsafeRetainedPaths(String unsafePath) {
        ObjectNode manifest = validManifest(root, RETAINED);
        ArrayNode retained = (ArrayNode) manifest.get("retained_paths");
        retained.removeAll();
        retained.add(unsafePath);
        assertEquals("unsafe retained path: " + unsafePath, failure(manifest).getMessage());
    }

    @Test
    void rejectsParentTraversalInsideRoot() {
        ObjectNode manifest = validManifest(root, RETAINED);
        ArrayNode retained = (ArrayNode) manifest.get("retained_paths");
        retained.removeAll();
        retained.add("src/../README.md");
        assertEquals("unsafe retained path: src/../README.md", failure(manifest).getMessage());
    }

    @Test
    void rejectsMissingRetainedFile() {
        ObjectNode manifest = validManifest(root, RETAINED);
        ArrayNode retained = (ArrayNode) manifest.get("retained_paths");
        retained.removeAll();
        retained.add("missing.txt");
        assertEquals("unsafe retained path: missing.txt", failure(manifest).getMessage());
    }

    @Test
    void rejectsDirectoryRetainedPath() throws Exception {
        ObjectNode manifest = validManifest(root, RETAINED);
        ArrayNode retained = (ArrayNode) manifest.get("retained_paths");
        retained.removeAll();
        retained.add("src");
        assertEquals("unsafe retained path: src", failure(manifest).getMessage());
    }

    @Test
    void rejectsSymlinkRetainedPath() throws Exception {
        Path link = root.resolve("linked.md");
        Files.createSymbolicLink(link, root.resolve("README.md"));
        ObjectNode manifest = validManifest(root, RETAINED);
        ArrayNode retained = (ArrayNode) manifest.get("retained_paths");
        retained.removeAll();
        retained.add("linked.md");
        assertEquals("unsafe retained path: linked.md", failure(manifest).getMessage());
    }

    @Test
    void rejectsEmptyRetainedPaths() {
        ObjectNode manifest = validManifest(root, RETAINED);
        ArrayNode retained = (ArrayNode) manifest.get("retained_paths");
        retained.removeAll();
        assertEquals("retained paths must be non-empty and unique", failure(manifest).getMessage());
    }

    @Test
    void rejectsDuplicateRetainedPaths() {
        ObjectNode manifest = validManifest(root, RETAINED);
        ArrayNode retained = (ArrayNode) manifest.get("retained_paths");
        retained.add("README.md");
        assertEquals("retained paths must be non-empty and unique", failure(manifest).getMessage());
    }

    @Test
    void rejectsNonStringArrayRetainedPaths() {
        ObjectNode manifest = validManifest(root, RETAINED);
        manifest.put("retained_paths", "README.md");
        assertEquals("retained_paths must be a string array", failure(manifest).getMessage());
        manifest.remove("retained_paths");
        ArrayNode retained = manifest.putArray("retained_paths");
        retained.add("README.md");
        retained.add(42);
        assertEquals("retained_paths must be a string array", failure(manifest).getMessage());
    }

    @Test
    void rejectsCredentials() throws Exception {
        Files.write(root.resolve("README.md"),
                "api_key = super-sensitive-value".getBytes(StandardCharsets.UTF_8));
        ObjectNode manifest = validManifest(root, RETAINED);
        assertEquals("credential pattern found: README.md", failure(manifest).getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "password: supersecret",
            "secret=12345678",
            "access-token: abcdefgh",
            "api-key: 12345678",
            "ACCESS_TOKEN: fourteen!!",
    })
    void rejectsCredentialPatternVariants(String content) throws Exception {
        Files.write(root.resolve("README.md"), content.getBytes(StandardCharsets.UTF_8));
        ObjectNode manifest = validManifest(root, RETAINED);
        assertEquals("credential pattern found: README.md", failure(manifest).getMessage());
    }

    @Test
    void credentialPatternRequiresEightValueCharacters() throws Exception {
        Files.write(root.resolve("README.md"),
                "secret = short".getBytes(StandardCharsets.UTF_8));
        ObjectNode result = validator().validateAcquisition(root, validManifest(root, RETAINED));
        assertEquals("VALIDATED", result.get("status").asText());
    }

    @Test
    void requiresPostCutoffKnowledgePolicy() {
        ObjectNode manifest = validManifest(root, RETAINED);
        manifest.remove("post_cutoff_knowledge_policy");
        assertEquals("post-cutoff knowledge policy must be EXCLUDE_AFTER_CUTOFF",
                failure(manifest).getMessage());
        manifest.put("post_cutoff_knowledge_policy", "INCLUDE");
        assertEquals("post-cutoff knowledge policy must be EXCLUDE_AFTER_CUTOFF",
                failure(manifest).getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"acquisition_method", "history_source", "license"})
    void requiresNonBlankManifestFields(String field) {
        ObjectNode manifest = validManifest(root, RETAINED);
        manifest.remove(field);
        assertEquals(field + " is required", failure(manifest).getMessage());
        manifest.put(field, "   ");
        assertEquals(field + " is required", failure(manifest).getMessage());
        manifest.put(field, 42);
        assertEquals(field + " is required", failure(manifest).getMessage());
    }

    @Test
    void requiresTimezoneBoundAcquiredAt() {
        ObjectNode manifest = validManifest(root, RETAINED);
        manifest.remove("acquired_at");
        assertEquals("acquired_at is required", failure(manifest).getMessage());
        manifest.put("acquired_at", "not-a-date");
        assertEquals("acquired_at must be an ISO-8601 timestamp", failure(manifest).getMessage());
        manifest.put("acquired_at", 42);
        assertEquals("acquired_at is required", failure(manifest).getMessage());
        manifest.put("acquired_at", "2026-09-04T00:00:00");
        assertEquals("acquired_at must include a timezone", failure(manifest).getMessage());
        manifest.put("acquired_at", "2026-09-04 00:00:00+00:00");
        ObjectNode result = validator().validateAcquisition(root, manifest);
        assertEquals("VALIDATED", result.get("status").asText());
    }

    @Test
    void requiresTimezoneBoundHistoryCutoff() {
        ObjectNode manifest = validManifest(root, RETAINED);
        manifest.remove("history_cutoff");
        assertEquals("history_cutoff is required", failure(manifest).getMessage());
        manifest.put("history_cutoff", "not-a-date");
        assertEquals("history_cutoff must be an ISO-8601 timestamp", failure(manifest).getMessage());
        manifest.put("history_cutoff", "2026-09-04T00:00:00");
        assertEquals("history_cutoff must include a timezone", failure(manifest).getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "max_repository_bytes", "max_file_count", "max_file_bytes"})
    void requiresPositiveIntegerLimits(String field) {
        ObjectNode manifest = validManifest(root, RETAINED);
        manifest.remove(field);
        assertEquals(field + " must be a positive integer", failure(manifest).getMessage());
        manifest.put(field, 0);
        assertEquals(field + " must be a positive integer", failure(manifest).getMessage());
        manifest.put(field, -1);
        assertEquals(field + " must be a positive integer", failure(manifest).getMessage());
        manifest.put(field, 10.5);
        assertEquals(field + " must be a positive integer", failure(manifest).getMessage());
        manifest.put(field, "1024");
        assertEquals(field + " must be a positive integer", failure(manifest).getMessage());
        manifest.put(field, true);
        assertEquals(field + " must be a positive integer", failure(manifest).getMessage());
    }

    @Test
    void rejectsFileCountAboveFrozenLimit() {
        ObjectNode manifest = validManifest(root, RETAINED);
        manifest.put("max_file_count", 1);
        assertEquals("file count exceeds frozen limit", failure(manifest).getMessage());
    }

    @Test
    void rejectsFileBytesAboveFrozenLimit() {
        ObjectNode manifest = validManifest(root, RETAINED);
        manifest.put("max_file_bytes", 17);
        assertEquals("file exceeds frozen limit: src/App.java", failure(manifest).getMessage());
    }

    @Test
    void rejectsBinaryRetainedContent() throws Exception {
        byte[] binary = new byte[24];
        System.arraycopy("final class App {}".getBytes(StandardCharsets.UTF_8), 0, binary, 0, 18);
        Files.write(root.resolve("src/App.java"), binary);
        ObjectNode manifest = validManifest(root, RETAINED);
        assertEquals("binary retained content is prohibited: src/App.java",
                failure(manifest).getMessage());
    }

    @Test
    void rejectsRepositoryBytesAboveFrozenLimit() {
        ObjectNode manifest = validManifest(root, RETAINED);
        manifest.put("max_repository_bytes", 24);
        assertEquals("repository bytes exceed frozen limit", failure(manifest).getMessage());
    }

    @Test
    void doesNotModifyRetainedTree() throws Exception {
        String before = new String(Files.readAllBytes(root.resolve("README.md")),
                StandardCharsets.UTF_8);
        validator().validateAcquisition(root, validManifest(root, RETAINED));
        assertEquals(before, new String(Files.readAllBytes(root.resolve("README.md")),
                StandardCharsets.UTF_8));
        assertTrue(Files.exists(root.resolve("src/App.java")));
    }

    @Test
    void exposesTreeSha256ForJsonNodeManifests() {
        JsonNode manifest = validManifest(root, RETAINED);
        ObjectNode result = validator().validateAcquisition(root, manifest);
        assertEquals(REFERENCE_TREE_SHA256, result.get("source_tree_sha256").asText());
    }
}

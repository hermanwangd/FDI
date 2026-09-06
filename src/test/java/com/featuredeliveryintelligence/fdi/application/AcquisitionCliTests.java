package com.featuredeliveryintelligence.fdi.application;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterization tests for the packaged {@code acquisition-validate}
 * command: deterministic {@code json.dumps}-style stdout on success, the
 * compact {@code {"status": "ERROR", "error": ...}} JSON with exit 1 on
 * validation failures, and exit 2 with usage on argument errors.
 */
class AcquisitionCliTests {
    private static final JsonNodeFactory NODE = JsonNodeFactory.instance;
    private static final String SHA = "a".repeat(40);
    private static final String TREE_SHA256 =
            "3433984bc0b09a7d3ebb1b99da82abca70b896912496258c4878d853bcd98c24";

    @TempDir Path temp;
    Path root;
    Path manifestPath;

    @BeforeEach
    void writeFixture() throws Exception {
        root = Files.createDirectories(temp.resolve("repo"));
        Files.write(root.resolve("README.md"), "example".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(root.resolve("src"));
        Files.write(root.resolve("src/App.java"),
                "final class App {}".getBytes(StandardCharsets.UTF_8));
        manifestPath = temp.resolve("manifest.json");
        Files.write(manifestPath,
                (validManifestJson() + "\n").getBytes(StandardCharsets.UTF_8));
    }

    private static String validManifestJson() {
        ObjectNode manifest = NODE.objectNode();
        manifest.put("source_commit_sha", SHA);
        ArrayNode retained = manifest.putArray("retained_paths");
        retained.add("README.md");
        retained.add("src/App.java");
        manifest.put("source_tree_sha256", TREE_SHA256);
        manifest.put("acquired_at", "2026-09-04T00:00:00Z");
        manifest.put("acquisition_method", "git fetch by immutable commit");
        manifest.put("history_source", "https://api.github.com/repos/example/project");
        manifest.put("history_cutoff", "2026-09-04T00:00:00Z");
        manifest.put("post_cutoff_knowledge_policy", "EXCLUDE_AFTER_CUTOFF");
        manifest.put("license", "AGPL-3.0-only");
        manifest.put("max_repository_bytes", 1024);
        manifest.put("max_file_count", 10);
        manifest.put("max_file_bytes", 512);
        return manifest.toString();
    }

    private record CliResult(int exit, String stdout, String stderr) { }

    private CliResult run(String... args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exit = AcquisitionCli.run(args, new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new CliResult(exit, stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    @Test
    void handlesOnlyAcquisitionValidateCommand() {
        assertFalse(AcquisitionCli.handles(new String[0]));
        assertFalse(AcquisitionCli.handles(new String[] {"unrelated"}));
        assertTrue(AcquisitionCli.handles(new String[] {"acquisition-validate"}));
    }

    @Test
    void validatesManifestAndPrintsDeterministicJson() {
        CliResult result = run("acquisition-validate",
                "--root", root.toString(), "--manifest", manifestPath.toString());
        assertEquals(0, result.exit());
        assertEquals("{\"status\": \"VALIDATED\", \"source_commit_sha\": \"" + SHA
                + "\", \"source_tree_sha256\": \"" + TREE_SHA256
                + "\", \"file_count\": 2, \"repository_bytes\": 25}"
                + System.lineSeparator(), result.stdout());
        assertEquals("", result.stderr());
    }

    @Test
    void printsErrorJsonAndExit1OnValidationFailure() throws Exception {
        Files.write(manifestPath, validManifestJson()
                .replace(SHA, "main")
                .getBytes(StandardCharsets.UTF_8));
        CliResult result = run("acquisition-validate",
                "--root", root.toString(), "--manifest", manifestPath.toString());
        assertEquals(1, result.exit());
        assertEquals("{\"status\": \"ERROR\", \"error\": \"source_commit_sha must be a"
                + " lowercase 40-character Git SHA\"}" + System.lineSeparator(),
                result.stdout());
    }

    @Test
    void printsErrorJsonAndExit1OnTreeDigestMismatch() throws Exception {
        Files.write(manifestPath, validManifestJson()
                .replace(TREE_SHA256, "0".repeat(64))
                .getBytes(StandardCharsets.UTF_8));
        CliResult result = run("acquisition-validate",
                "--root", root.toString(), "--manifest", manifestPath.toString());
        assertEquals(1, result.exit());
        assertTrue(result.stdout().startsWith("{\"status\": \"ERROR\""));
        assertTrue(result.stdout().contains("source tree digest mismatch"));
    }

    @Test
    void printsErrorJsonAndExit1OnMissingManifestFile() {
        CliResult result = run("acquisition-validate",
                "--root", root.toString(), "--manifest", temp.resolve("absent.json").toString());
        assertEquals(1, result.exit());
        assertEquals("{\"status\": \"ERROR\", \"error\": \"[Errno 2] No such file or"
                + " directory: '" + temp.resolve("absent.json") + "'\"}"
                + System.lineSeparator(), result.stdout());
    }

    @Test
    void printsErrorJsonAndExit1OnMalformedManifestJson() throws Exception {
        Files.write(manifestPath, "{not json".getBytes(StandardCharsets.UTF_8));
        CliResult result = run("acquisition-validate",
                "--root", root.toString(), "--manifest", manifestPath.toString());
        assertEquals(1, result.exit());
        assertTrue(result.stdout().startsWith("{\"status\": \"ERROR\""));
        assertTrue(result.stdout().contains("Expecting property name enclosed in double quotes"));
    }

    @Test
    void printsErrorJsonAndExit1OnNonObjectManifest() throws Exception {
        Files.write(manifestPath, "[]".getBytes(StandardCharsets.UTF_8));
        CliResult result = run("acquisition-validate",
                "--root", root.toString(), "--manifest", manifestPath.toString());
        assertEquals(1, result.exit());
        assertTrue(result.stdout().startsWith("{\"status\": \"ERROR\""));
        assertTrue(result.stdout().contains("manifest is not a JSON object"));
    }

    @Test
    void exits2WithUsageOnMissingArguments() {
        CliResult missing = run("acquisition-validate");
        assertEquals(2, missing.exit());
        assertEquals("", missing.stdout());
        assertTrue(missing.stderr().startsWith("usage: acquisition-validate"));

        CliResult noManifest = run("acquisition-validate", "--root", root.toString());
        assertEquals(2, noManifest.exit());
        assertTrue(noManifest.stderr().contains("--manifest"));
    }

    @Test
    void exits2WithUsageOnUnrecognizedArguments() {
        CliResult result = run("acquisition-validate",
                "--root", root.toString(), "--manifest", manifestPath.toString(),
                "--verbose", "true");
        assertEquals(2, result.exit());
        assertTrue(result.stderr().contains("unrecognized arguments"));
    }
}

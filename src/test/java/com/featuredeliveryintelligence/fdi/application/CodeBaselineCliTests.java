package com.featuredeliveryintelligence.fdi.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ports the CLI characterization cases of
 * {@code tests/test_pkb001_code_baseline.py} to the packaged
 * {@code code-baseline-generate} command: the structure revision/digest
 * binding overrides, the ERROR JSON stdout with exit 1 on caught failures,
 * and exit 2 on usage errors.
 */
class CodeBaselineCliTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonNodeFactory NODE = JsonNodeFactory.instance;
    private static final String SHA = "a".repeat(40);
    private static final String GRAPH_SHA = "b".repeat(64);

    @TempDir Path temp;

    @Test
    void handlesOnlyCodeBaselineGenerateCommand() {
        assertFalse(CodeBaselineCli.handles(new String[0]));
        assertFalse(CodeBaselineCli.handles(new String[] {"unrelated"}));
        assertTrue(CodeBaselineCli.handles(new String[] {"code-baseline-generate"}));
    }

    @Test
    void cliBindsRawGraphToExplicitRevisionAndDigest() throws Exception {
        ObjectNode graph = NODE.objectNode();
        var nodes = graph.putArray("nodes");
        nodes.add(NODE.objectNode()
                .put("id", "product-semantics")
                .put("label", "ProductSemantics.java")
                .put("source_file", "src/main/java/example/product/ProductSemantics.java")
                .put("source_location", "L1"));
        graph.putArray("links");
        Path graphPath = temp.resolve("graph.json");
        Files.write(graphPath, (JSON.writeValueAsString(graph) + "\n").getBytes(StandardCharsets.UTF_8));

        ObjectNode semantics = NODE.objectNode();
        semantics.put("status", "FROZEN");
        semantics.put("owner", "PRODUCT_TEAM");
        semantics.put("applicable_source_commit_sha", SHA);
        semantics.putArray("capabilities");
        Path semanticsPath = temp.resolve("semantics.json");
        Files.write(semanticsPath, JSON.writeValueAsBytes(semantics));

        Path output = temp.resolve("nested/result.json");
        Result result = run(new String[] {"code-baseline-generate",
                "--arm", "F1",
                "--input", "structure=" + graphPath,
                "--input", "semantics=" + semanticsPath,
                "--source-sha", SHA,
                "--graph-sha", GRAPH_SHA,
                "--output", output.toString()});

        assertEquals(0, result.exitCode());
        assertEquals("", result.stdout());
        assertEquals("", result.stderr());
        JsonNode written = JSON.readTree(Files.readAllBytes(output));
        assertEquals(SHA, written.get("source_commit_sha").asText());
        assertEquals(GRAPH_SHA, written.get("graph_artifact_sha256").asText());
        assertTrue(new String(Files.readAllBytes(output), StandardCharsets.UTF_8).endsWith("\n"));
    }

    @Test
    void cliPrintsErrorJsonAndExitsOneForCategoryMismatch() throws Exception {
        Path graphPath = temp.resolve("graph.json");
        Files.writeString(graphPath, "{}\n", StandardCharsets.UTF_8);
        Result result = run(new String[] {"code-baseline-generate",
                "--arm", "R1",
                "--input", "structure=" + graphPath,
                "--input", "structure=" + graphPath,
                "--output", temp.resolve("out.json").toString()});

        assertEquals(1, result.exitCode());
        assertEquals("", result.stderr());
        assertEquals("{\"status\": \"ERROR\", \"error\": \"inputs must use unique category=path bindings\"}"
                + System.lineSeparator(), result.stdout());
        assertFalse(Files.exists(temp.resolve("out.json")));
    }

    @Test
    void cliPrintsErrorJsonAndExitsOneForArmAllowlistMismatch() throws Exception {
        Path graphPath = temp.resolve("graph.json");
        Files.writeString(graphPath, "{}\n", StandardCharsets.UTF_8);
        Path semanticsPath = temp.resolve("semantics.json");
        Files.writeString(semanticsPath, "{}\n", StandardCharsets.UTF_8);
        Result result = run(new String[] {"code-baseline-generate",
                "--arm", "R1",
                "--input", "structure=" + graphPath,
                "--input", "semantics=" + semanticsPath,
                "--output", temp.resolve("out.json").toString()});

        assertEquals(1, result.exitCode());
        assertEquals("{\"status\": \"ERROR\", \"error\": \"input categories do not match arm allowlist\"}"
                + System.lineSeparator(), result.stdout());
    }

    @Test
    void cliPrintsPythonStyleErrorJsonForMissingInputFile() {
        Result result = run(new String[] {"code-baseline-generate",
                "--arm", "R1",
                "--input", "structure=" + temp.resolve("missing.json"),
                "--output", temp.resolve("out.json").toString()});

        assertEquals(1, result.exitCode());
        assertEquals("{\"status\": \"ERROR\", \"error\": \"[Errno 2] No such file or directory: '"
                + temp.resolve("missing.json") + "'\"}" + System.lineSeparator(), result.stdout());
    }

    @Test
    void cliRejectsInputLargerThanSafetyLimitWithoutReadingItAll() throws Exception {
        Path oversized = temp.resolve("oversized.json");
        try (RandomAccessFile file = new RandomAccessFile(oversized.toFile(), "rw")) {
            file.setLength(CodeBaselineCli.MAX_INPUT_BYTES + 1L);
        }

        Result result = run(new String[] {"code-baseline-generate",
                "--arm", "R1",
                "--input", "structure=" + oversized,
                "--output", temp.resolve("out.json").toString()});

        assertEquals(1, result.exitCode());
        assertEquals("{\"status\": \"ERROR\", \"error\": \"input exceeds "
                + CodeBaselineCli.MAX_INPUT_BYTES + " byte limit: '" + oversized + "'\"}"
                + System.lineSeparator(), result.stdout());
        assertFalse(Files.exists(temp.resolve("out.json")));
    }

    @Test
    void cliExitsTwoWithUsageForMissingArm() {
        Result result = run(new String[] {"code-baseline-generate",
                "--output", temp.resolve("out.json").toString()});
        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("--arm"), result.stderr());
        assertTrue(result.stderr().contains("usage:"), result.stderr());
    }

    @Test
    void cliExitsTwoWithUsageForInvalidArmChoice() {
        Result result = run(new String[] {"code-baseline-generate",
                "--arm", "X1", "--output", temp.resolve("out.json").toString()});
        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("invalid choice"), result.stderr());
    }

    private static Result run(String[] args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = CodeBaselineCli.run(args,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Result(exitCode,
                stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
    }

    private record Result(int exitCode, String stdout, String stderr) { }
}

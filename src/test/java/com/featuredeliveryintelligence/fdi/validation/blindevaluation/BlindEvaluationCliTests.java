package com.featuredeliveryintelligence.fdi.validation.blindevaluation;

import com.featuredeliveryintelligence.fdi.application.BlindEvaluationCli;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CLI tests for the new {@code blinded-evaluate} packaged command. The Python
 * module has no CLI; this surface renders the evaluation (or wrapped decision
 * report) with the Python-style renderer, writes {@code --output} when given
 * (plain write, without gate-style containment), exits 0 on CONTINUE, 2 on
 * REVISE or STOP, and 1 with a compact {@code {"status": "ERROR", ...}} JSON
 * on usage or validation errors.
 */
class BlindEvaluationCliTests {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir Path temp;

    @Test
    void handlesOnlyBlindedEvaluateCommand() {
        assertFalse(BlindEvaluationCli.handles(new String[0]));
        assertFalse(BlindEvaluationCli.handles(new String[] {"unrelated"}));
        assertTrue(BlindEvaluationCli.handles(new String[] {"blinded-evaluate"}));
    }

    @Test
    void continueDecisionExitsZeroAndWritesOutput() throws Exception {
        Fixture fixture = supportedFixture(30, "R3");
        Path output = temp.resolve("report.json");
        Result result = run(new String[] {"--proposals", fixture.proposalsPath().toString(),
                "--judgments", fixture.judgmentsPath().toString(), "--output", output.toString()});

        assertEquals("", result.stderr());
        assertEquals(0, result.exitCode());
        assertArrayEquals(Files.readAllBytes(output), result.stdoutBytes());
        assertEquals("CONTINUE", JSON.readTree(result.stdout()).get("decision").asText());
    }

    @Test
    void reviseDecisionExitsTwo() throws Exception {
        Fixture fixture = supportedFixture(1, "R3");
        Result result = run(new String[] {"--proposals", fixture.proposalsPath().toString(),
                "--judgments", fixture.judgmentsPath().toString(),
                "--minimum-proposals", "30", "--minimum-gold", "10"});

        assertEquals(2, result.exitCode());
        assertEquals("REVISE", JSON.readTree(result.stdout()).get("decision").asText());
    }

    @Test
    void stopDecisionExitsTwo() throws Exception {
        Fixture fixture = supportedFixture(30, "R3");
        Result result = run(new String[] {"--proposals", fixture.proposalsPath().toString(),
                "--judgments", fixture.judgmentsPath().toString(),
                "--hard-failures", "GROUND_TRUTH_ACCESS"});

        assertEquals(2, result.exitCode());
        JsonNode report = JSON.readTree(result.stdout());
        assertEquals("STOP", report.get("decision").asText());
        assertEquals("GROUND_TRUTH_ACCESS",
                report.get("hard_gate_failures").get(0).asText());
    }

    @Test
    void reportIdAndGroundTruthShaWrapWithDecisionReport() throws Exception {
        Fixture fixture = supportedFixture(30, "F1");
        Result result = run(new String[] {"--proposals", fixture.proposalsPath().toString(),
                "--judgments", fixture.judgmentsPath().toString(),
                "--report-id", "report-9", "--ground-truth-sha256", "b".repeat(64)});

        assertEquals(0, result.exitCode());
        JsonNode report = JSON.readTree(result.stdout());
        assertEquals("report-9", report.get("report_id").asText());
        assertEquals("b".repeat(64), report.get("ground_truth_sha256").asText());
    }

    @Test
    void partialWrapArgumentsAreRejected() throws Exception {
        Fixture fixture = supportedFixture(30, "F1");
        Result result = run(new String[] {"--proposals", fixture.proposalsPath().toString(),
                "--judgments", fixture.judgmentsPath().toString(), "--report-id", "report-9"});

        assertEquals(1, result.exitCode());
        assertEquals("ERROR", JSON.readTree(result.stdout()).get("status").asText());
    }

    @Test
    void missingProposalsRendersErrorJson() throws Exception {
        Result result = run(new String[] {"--judgments", "judgments.json"});
        assertEquals(1, result.exitCode());
        assertEquals("ERROR", JSON.readTree(result.stdout()).get("status").asText());
    }

    @Test
    void validationFailureRendersPythonValueErrorMessage() throws Exception {
        ArrayNode proposals = BlindEvaluationTests.proposals(1, "R1");
        ((ObjectNode) proposals.get(0)).put("arm", "R9");
        ArrayNode judgments = BlindEvaluationTests.judgmentsFor(proposals,
                List.of("SUPPORTED"));
        Path proposalsPath = temp.resolve("invalid-proposals.json");
        Path judgmentsPath = temp.resolve("invalid-judgments.json");
        Files.write(proposalsPath, JSON.writeValueAsBytes(proposals));
        Files.write(judgmentsPath, JSON.writeValueAsBytes(judgments));

        Result result = run(new String[] {"--proposals", proposalsPath.toString(),
                "--judgments", judgmentsPath.toString()});

        assertEquals(1, result.exitCode());
        assertEquals("invalid proposal arm",
                JSON.readTree(result.stdout()).get("error").asText());
    }

    @Test
    void nonArrayProposalsRendersErrorJson() throws Exception {
        Path proposals = temp.resolve("proposals.json");
        Files.writeString(proposals, "{}\n");
        Path judgments = temp.resolve("judgments.json");
        Files.writeString(judgments, "[]\n");
        Result result = run(new String[] {"--proposals", proposals.toString(),
                "--judgments", judgments.toString()});

        assertEquals(1, result.exitCode());
        assertEquals("ERROR", JSON.readTree(result.stdout()).get("status").asText());
    }

    @Test
    void cliStdoutMatchesDirectRendererBytes() throws Exception {
        Fixture fixture = supportedFixture(30, "R3");
        Result result = run(new String[] {"--proposals", fixture.proposalsPath().toString(),
                "--judgments", fixture.judgmentsPath().toString()});
        byte[] direct = new com.featuredeliveryintelligence.fdi.validation.codebaseline
                .CodeBaselineResult(fixture.expected()).toJsonBytes();
        assertArrayEquals(direct, result.stdoutBytes());
    }

    private Fixture supportedFixture(int count, String arm) throws Exception {
        ArrayNode proposals = BlindEvaluationTests.proposals(count, arm);
        ArrayNode judgments = BlindEvaluationTests.judgmentsFor(
                proposals, BlindEvaluationTests.repeat("SUPPORTED", count));
        Path proposalsPath = temp.resolve("proposals-" + arm + "-" + count + ".json");
        Path judgmentsPath = temp.resolve("judgments-" + arm + "-" + count + ".json");
        Files.write(proposalsPath, JSON.writeValueAsBytes(proposals));
        Files.write(judgmentsPath, JSON.writeValueAsBytes(judgments));
        ObjectNode expected = BlindEvaluationTests.evaluate(proposals, judgments, 30, 10, null);
        return new Fixture(proposalsPath, judgmentsPath, expected);
    }

    private static Result run(String[] args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = BlindEvaluationCli.run(prepend(args),
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Result(exitCode, stdout.toByteArray(), stderr.toByteArray());
    }

    private static String[] prepend(String[] args) {
        String[] full = new String[args.length + 1];
        full[0] = "blinded-evaluate";
        System.arraycopy(args, 0, full, 1, args.length);
        return full;
    }

    private record Fixture(Path proposalsPath, Path judgmentsPath, ObjectNode expected) { }

    private record Result(int exitCode, byte[] stdoutBytes, byte[] stderrBytes) {
        String stdout() {
            return new String(stdoutBytes, StandardCharsets.UTF_8);
        }

        String stderr() {
            return new String(stderrBytes, StandardCharsets.UTF_8);
        }
    }
}

package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.validation.scenarioreview.ScenarioReviewCharacterizationTests;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ports the CLI characterization cases of
 * {@code tests/test_pkb001_scenario_review.py} to the packaged
 * {@code scenario-review-render} command: exact success JSON on stdout with
 * exit 0, BLOCKED reasons JSON on stderr with exit 1, argparse-style usage
 * errors with exit 2, exclusive creation, and concurrent same-run claims.
 */
class ScenarioReviewCliTests {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir Path temp;

    @Test
    void handlesOnlyScenarioReviewRenderCommand() {
        assertFalse(ScenarioReviewCli.handles(new String[0]));
        assertFalse(ScenarioReviewCli.handles(new String[] {"unrelated"}));
        assertTrue(ScenarioReviewCli.handles(new String[] {"scenario-review-render"}));
    }

    @Test
    void cliWritesBothOutputsAndPrintsRenderedJson() throws Exception {
        ScenarioReviewCharacterizationTests.Fixture fx =
                ScenarioReviewCharacterizationTests.buildFixture(temp.toRealPath());
        Result result = run(new String[] {"scenario-review-render",
                "--root", fx.root.toString(),
                "--proposal", "input/proposal.json",
                "--json-output", "validation/pkb001/reviews/review.json",
                "--markdown-output", "validation/pkb001/reviews/review.md"});

        assertEquals(0, result.exitCode(), result.stderr());
        assertEquals("", result.stderr());
        Path jsonOut = fx.root.toRealPath().resolve("validation/pkb001/reviews/review.json");
        Path mdOut = fx.root.toRealPath().resolve("validation/pkb001/reviews/review.md");
        assertTrue(Files.exists(jsonOut));
        assertTrue(Files.exists(mdOut));
        String expectedStdout = "{\"status\": \"RENDERED\", \"json\": \"" + jsonOut
                + "\", \"markdown\": \"" + mdOut + "\"}" + System.lineSeparator();
        assertEquals(expectedStdout, result.stdout());

        JsonNode review = JSON.readTree(Files.readAllBytes(jsonOut));
        assertEquals("SCENARIO_REVIEW_SURFACE", review.get("artifact_kind").asText());
        assertEquals("pkb001-scenario-review-test-001", review.get("run_id").asText());
        String markdown = Files.readString(mdOut, StandardCharsets.UTF_8);
        assertTrue(markdown.startsWith("# PKB-001 情境提案個別審查 / Scenario Proposal Review\n"));
        assertTrue(markdown.endsWith("\n"));
    }

    @Test
    void cliPrintsBlockedReasonsJsonOnStderrAndExitsOne() throws Exception {
        ScenarioReviewCharacterizationTests.Fixture fx =
                ScenarioReviewCharacterizationTests.buildFixture(temp.toRealPath());
        // Output outside validation/pkb001/ is rejected before any write.
        Result result = run(new String[] {"scenario-review-render",
                "--root", fx.root.toString(),
                "--proposal", "input/proposal.json",
                "--json-output", "escape/review.json",
                "--markdown-output", "validation/pkb001/reviews/review.md"});

        assertEquals(1, result.exitCode());
        assertEquals("", result.stdout());
        assertEquals("{\"status\": \"BLOCKED\", \"reasons\": [\"OUTPUT_PATH_INVALID\"]}"
                + System.lineSeparator(), result.stderr());
        assertFalse(Files.exists(fx.root.resolve("escape/review.json")));
    }

    @Test
    void cliBlockedReasonsAreSortedUnique() throws Exception {
        ScenarioReviewCharacterizationTests.Fixture fx =
                ScenarioReviewCharacterizationTests.buildFixture(temp.toRealPath());
        byte[] tampered = ("{\"artifact_kind\": \"WRONG\"}\n").getBytes(StandardCharsets.UTF_8);
        Path proposalPath = fx.root.resolve("input/proposal.json");
        Files.write(proposalPath, tampered);
        Result result = run(new String[] {"scenario-review-render",
                "--root", fx.root.toString(),
                "--proposal", "input/proposal.json",
                "--json-output", "validation/pkb001/reviews/review.json",
                "--markdown-output", "validation/pkb001/reviews/review.md"});

        assertEquals(1, result.exitCode());
        JsonNode blocked = JSON.readTree(result.stderr().trim());
        assertEquals("BLOCKED", blocked.get("status").asText());
        List<String> reasons = new ArrayList<>();
        blocked.get("reasons").forEach(node -> reasons.add(node.asText()));
        List<String> sorted = new ArrayList<>(reasons);
        java.util.Collections.sort(sorted);
        assertEquals(sorted, reasons, "reasons must be sorted");
        assertEquals(new java.util.HashSet<>(reasons).size(), reasons.size(), "reasons unique");
        assertTrue(reasons.contains("SCHEMA_INVALID"), reasons.toString());
    }

    @Test
    void cliExitsTwoWithUsageForMissingArguments() {
        Result result = run(new String[] {"scenario-review-render", "--root", "somewhere"});
        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("usage:"), result.stderr());
        assertTrue(result.stderr().contains("--markdown-output"), result.stderr());
        assertEquals("", result.stdout());
    }

    @Test
    void cliExitsTwoWithUsageForUnknownOption() {
        Result result = run(new String[] {"scenario-review-render",
                "--root", "r", "--proposal", "p", "--json-output", "j",
                "--markdown-output", "m", "--surprise", "x"});
        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("usage:"), result.stderr());
    }

    @Test
    void cliSupportsEqualsFormArguments() throws Exception {
        ScenarioReviewCharacterizationTests.Fixture fx =
                ScenarioReviewCharacterizationTests.buildFixture(temp.toRealPath());
        Result result = run(new String[] {"scenario-review-render",
                "--root=" + fx.root,
                "--proposal=input/proposal.json",
                "--json-output=validation/pkb001/reviews/review.json",
                "--markdown-output=validation/pkb001/reviews/review.md"});
        assertEquals(0, result.exitCode(), result.stderr());
        assertTrue(result.stdout().startsWith("{\"status\": \"RENDERED\""));
    }

    @Test
    void concurrentSameRunClaimAllowsExactlyOneCompletePair() throws Exception {
        ScenarioReviewCharacterizationTests.Fixture fx =
                ScenarioReviewCharacterizationTests.buildFixture(temp.toRealPath());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Result>> futures = new ArrayList<>();
        for (String suffix : new String[] {"one", "two"}) {
            Callable<Result> task = () -> run(new String[] {"scenario-review-render",
                    "--root", fx.root.toString(),
                    "--proposal", "input/proposal.json",
                    "--json-output", "validation/pkb001/reviews/" + suffix + ".json",
                    "--markdown-output", "validation/pkb001/reviews/" + suffix + ".md"});
            futures.add(pool.submit(task));
        }
        List<Integer> exitCodes = new ArrayList<>();
        for (Future<Result> future : futures) {
            exitCodes.add(future.get().exitCode());
        }
        pool.shutdown();
        java.util.Collections.sort(exitCodes);
        assertEquals(List.of(0, 1), exitCodes);
        int completePairs = 0;
        for (String suffix : new String[] {"one", "two"}) {
            if (Files.exists(fx.root.resolve("validation/pkb001/reviews/" + suffix + ".json"))
                    && Files.exists(fx.root.resolve("validation/pkb001/reviews/" + suffix + ".md"))) {
                completePairs++;
            }
        }
        assertEquals(1, completePairs);
    }

    @Test
    void cliDuplicateRunAgainstExistingClaimExitsOneWithoutOutputs() throws Exception {
        ScenarioReviewCharacterizationTests.Fixture fx =
                ScenarioReviewCharacterizationTests.buildFixture(temp.toRealPath());
        Result first = run(new String[] {"scenario-review-render",
                "--root", fx.root.toString(),
                "--proposal", "input/proposal.json",
                "--json-output", "validation/pkb001/reviews/review.json",
                "--markdown-output", "validation/pkb001/reviews/review.md"});
        assertEquals(0, first.exitCode(), first.stderr());

        Result second = run(new String[] {"scenario-review-render",
                "--root", fx.root.toString(),
                "--proposal", "input/proposal.json",
                "--json-output", "validation/pkb001/reviews/other.json",
                "--markdown-output", "validation/pkb001/reviews/other.md"});
        assertEquals(1, second.exitCode());
        assertEquals("{\"status\": \"BLOCKED\", \"reasons\": [\"RUN_ID_ALREADY_EXISTS\"]}"
                + System.lineSeparator(), second.stderr());
        assertFalse(Files.exists(fx.root.resolve("validation/pkb001/reviews/other.json")));
        assertFalse(Files.exists(fx.root.resolve("validation/pkb001/reviews/other.md")));
    }

    private static Result run(String[] args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = ScenarioReviewCli.run(args,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Result(exitCode,
                stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
    }

    private record Result(int exitCode, String stdout, String stderr) { }
}

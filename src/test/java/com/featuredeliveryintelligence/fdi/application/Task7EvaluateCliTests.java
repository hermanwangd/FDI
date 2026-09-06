package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.validation.task7.Task7EvaluationRoots;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ports the two CLI subprocess cases of {@code tests/test_pkb001_task7_evaluation.py}
 * to the packaged {@code task7-evaluate} command: the STOP report with the
 * documented nonzero exit code, and the digest-consistent invalid key
 * accounting STOP. Also pins success-path stdout/report/pending bytes against
 * the committed immutable artifacts.
 */
class Task7EvaluateCliTests {

    @TempDir Path temp;

    @Test
    void handlesOnlyTask7EvaluateCommand() {
        assertFalse(Task7EvaluateCli.handles(new String[0]));
        assertFalse(Task7EvaluateCli.handles(new String[] {"unrelated"}));
        assertTrue(Task7EvaluateCli.handles(new String[] {"task7-evaluate"}));
    }

    @Test
    void cliEvaluatesCommittedRootAndMatchesCommittedArtifactBytes() throws Exception {
        Path reportPath = temp.resolve("report.json");
        Path pendingPath = temp.resolve("pending.json");

        Result result = run(new String[] {"task7-evaluate",
                "--root", Task7EvaluationRoots.REPOSITORY.toString(),
                "--report", reportPath.toString(),
                "--pending", pendingPath.toString()});

        assertEquals("", result.stderr());
        assertEquals(0, result.exitCode());
        byte[] committedReport = Files.readAllBytes(
                Task7EvaluationRoots.REPOSITORY.resolve(Task7EvaluationRoots.REPORT_RELATIVE));
        byte[] committedPending = Files.readAllBytes(
                Task7EvaluationRoots.REPOSITORY.resolve(Task7EvaluationRoots.PENDING_RELATIVE));
        assertArrayEquals(committedReport, Files.readAllBytes(reportPath));
        assertArrayEquals(committedPending, Files.readAllBytes(pendingPath));
        assertArrayEquals(committedReport,
                (result.stdout()).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void cliPersistsStopAndReturnsDocumentedNonzeroExit() throws Exception {
        Path root = Task7EvaluationRoots.copyEvaluationRoot(temp.resolve("mutated-root"));
        Task7EvaluationRoots.appendByte(root.resolve(
                "validation/pkb001/task6-blind-review/blind-review-packet.json"));
        Path reportPath = temp.resolve("stop-report.json");
        Path pendingPath = temp.resolve("must-not-exist.json");

        Result result = run(new String[] {"task7-evaluate",
                "--root", root.toString(),
                "--report", reportPath.toString(),
                "--pending", pendingPath.toString()});

        assertEquals("", result.stderr());
        assertEquals(2, result.exitCode());
        byte[] persisted = Files.readAllBytes(reportPath);
        assertTrue(new String(persisted, StandardCharsets.UTF_8).startsWith("{"));
        assertEquals("STOP", com.featuredeliveryintelligence.fdi.validation.task7.Task7Json
                .readTree(persisted).get("decision").asText());
        assertFalse(Files.exists(pendingPath));
        assertArrayEquals(persisted, result.stdout().getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void cliPersistsStopForDigestConsistentInvalidKeyAccounting() throws Exception {
        Path root = Task7EvaluationRoots.copyEvaluationRoot(temp.resolve("invalid-key-root"));
        Task7EvaluationRoots.emptySealedKeyAndRepairManifest(root);
        Path reportPath = temp.resolve("invalid-key-stop-report.json");
        Path pendingPath = temp.resolve("must-not-exist-key.json");

        Result result = run(new String[] {"task7-evaluate",
                "--root", root.toString(),
                "--report", reportPath.toString(),
                "--pending", pendingPath.toString()});

        assertEquals("", result.stderr());
        assertEquals(2, result.exitCode());
        assertFalse(Files.exists(pendingPath));
        var report = com.featuredeliveryintelligence.fdi.validation.task7.Task7Json
                .readTree(Files.readAllBytes(reportPath));
        assertEquals("STOP", report.get("decision").asText());
        assertEquals("POST_BINDING_INTEGRITY_VALIDATION", report.get("failure_stage").asText());
        assertEquals(2, report.get("documented_exit_code").asLong());
        assertArrayEquals(Files.readAllBytes(reportPath),
                result.stdout().getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void cliReportsUsageErrorForUnknownOption() {
        Result result = run(new String[] {"task7-evaluate", "--bogus", "value"});

        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("usage:"), result.stderr());
        assertTrue(result.stderr().contains("--bogus"), result.stderr());
    }

    private static Result run(String[] args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = Task7EvaluateCli.run(args,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Result(exitCode,
                stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
    }

    private record Result(int exitCode, String stdout, String stderr) { }
}

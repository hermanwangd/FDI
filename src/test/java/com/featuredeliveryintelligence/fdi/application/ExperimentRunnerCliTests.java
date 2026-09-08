package com.featuredeliveryintelligence.fdi.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the packaged {@code experiment-runner-validate} and
 * {@code experiment-runner-execute} commands: deterministic JSON stdout,
 * exit 0 on success, exit 1 with the compact ERROR JSON on caught failures
 * (the Python {@code ValueError} vocabulary), and exit 2 on usage errors.
 */
class ExperimentRunnerCliTests {
    @TempDir Path temp;
    private Path root;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

    @BeforeEach
    void setUp() throws Exception {
        root = temp.toRealPath();
        for (String category : new String[] {"structure", "history", "semantics"}) {
            Path dir = root.resolve("inputs").resolve(category);
            Files.createDirectories(dir);
            Files.write(dir.resolve("input.json"), "{}".getBytes(StandardCharsets.UTF_8));
        }
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
    }

    private String stdout() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private void writeReadiness(String json) throws Exception {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Files.write(root.resolve("phase0-readiness.json"), bytes);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        StringBuilder hex = new StringBuilder();
        for (byte value : digest.digest(bytes)) {
            hex.append(Character.forDigit((value >> 4) & 0xf, 16));
            hex.append(Character.forDigit(value & 0xf, 16));
        }
        Files.write(root.resolve("phase0-readiness.sha256"),
                (hex + "\n").getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void handlesOnlyRunnerCommands() {
        assertFalse(ExperimentRunnerValidateCli.handles(new String[0]));
        assertFalse(ExperimentRunnerValidateCli.handles(new String[] {"unrelated"}));
        assertTrue(ExperimentRunnerValidateCli.handles(new String[] {"experiment-runner-validate"}));
        assertFalse(ExperimentRunnerExecuteCli.handles(new String[0]));
        assertTrue(ExperimentRunnerExecuteCli.handles(new String[] {"experiment-runner-execute"}));
    }

    @Test
    void validateCommandPrintsDeterministicValidatedReport() throws Exception {
        int code = ExperimentRunnerValidateCli.run(new String[] {
                "experiment-runner-validate", "--workspace", root.toString(),
                "--arm", "R1", "--input", "inputs/structure/input.json"},
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        assertEquals(0, code);
        assertEquals("{\"status\": \"VALIDATED\", \"arm\": \"R1\", "
                + "\"inputs\": [\"inputs/structure/input.json\"], "
                + "\"resolved\": [\"" + root.resolve("inputs/structure/input.json") + "\"]}\n",
                stdout());
    }

    @Test
    void validateCommandWritesReportFileWhenRequested() throws Exception {
        Path report = root.resolve("reports/runner.json");
        int code = ExperimentRunnerValidateCli.run(new String[] {
                "experiment-runner-validate", "--workspace", root.toString(),
                "--arm", "F1", "--input", "inputs/semantics/input.json",
                "--input", "inputs/structure/input.json", "--report", report.toString()},
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        assertEquals(0, code);
        assertEquals(stdout(), Files.readString(report));
    }

    @Test
    void validateCommandReportsAllowlistFailuresWithErrorJson() {
        int code = ExperimentRunnerValidateCli.run(new String[] {
                "experiment-runner-validate", "--workspace", root.toString(),
                "--arm", "F1", "--input", "inputs/history/input.json"},
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        assertEquals(1, code);
        assertEquals("{\"status\": \"ERROR\", \"error\": \"prohibited input for F1: "
                + "inputs/history/input.json\"}\n", stdout());
    }

    @Test
    void validateCommandRejectsUnknownArm() {
        int code = ExperimentRunnerValidateCli.run(new String[] {
                "experiment-runner-validate", "--workspace", root.toString(),
                "--arm", "X", "--input", "inputs/structure/input.json"},
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        assertEquals(1, code);
        assertEquals("{\"status\": \"ERROR\", \"error\": \"unknown PKB-001 arm: X\"}\n", stdout());
    }

    @Test
    void validateCommandFailsClosedOnMissingWorkspace() {
        int code = ExperimentRunnerValidateCli.run(new String[] {
                "experiment-runner-validate", "--workspace", root.resolve("missing").toString(),
                "--arm", "R1", "--input", "inputs/structure/input.json"},
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        assertEquals(1, code);
        assertTrue(stdout().startsWith("{\"status\": \"ERROR\", \"error\": \""), stdout());
    }

    @Test
    void validateCommandReturnsUsageErrorWithoutArm() {
        int code = ExperimentRunnerValidateCli.run(new String[] {"experiment-runner-validate"},
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        assertEquals(2, code);
        assertTrue(err.toString(StandardCharsets.UTF_8)
                .startsWith("usage: experiment-runner-validate"));
    }

    @Test
    void executeCommandRunsSandboxedCommandAndMirrorsExitCode() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(Path.of("/usr/bin/sandbox-exec")));
        writeReadiness("{\"status\": \"READY\"}");
        int code = ExperimentRunnerExecuteCli.run(new String[] {
                "experiment-runner-execute", "--workspace", root.toString(),
                "--command", "/usr/bin/true", "--env", "PKB_NETWORK_ISOLATION=ENFORCED"},
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        assertEquals(0, code);
        assertEquals("{\"status\": \"EXECUTED\", \"exit_code\": 0}\n", stdout());
    }

    @Test
    void executeCommandMirrorsNonZeroSandboxedExitCode() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(Path.of("/usr/bin/sandbox-exec")));
        writeReadiness("{\"status\": \"READY\"}");
        int code = ExperimentRunnerExecuteCli.run(new String[] {
                "experiment-runner-execute", "--workspace", root.toString(),
                "--command", "/usr/bin/false", "--env", "PKB_NETWORK_ISOLATION=ENFORCED"},
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        assertEquals(1, code);
        assertEquals("{\"status\": \"EXECUTED\", \"exit_code\": 1}\n", stdout());
    }

    @Test
    void executeCommandReportsValidationFailuresWithErrorJson() throws Exception {
        // readiness files are missing
        int code = ExperimentRunnerExecuteCli.run(new String[] {
                "experiment-runner-execute", "--workspace", root.toString(),
                "--command", "/usr/bin/true", "--env", "PKB_NETWORK_ISOLATION=ENFORCED"},
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        assertEquals(1, code);
        assertEquals("{\"status\": \"ERROR\", \"error\": \"verified READY report is required\"}\n",
                stdout());
    }

    @Test
    void executeCommandReportsProhibitedCommandsWithErrorJson() throws Exception {
        writeReadiness("{\"status\": \"READY\"}");
        int code = ExperimentRunnerExecuteCli.run(new String[] {
                "experiment-runner-execute", "--workspace", root.toString(),
                "--command", "gradle", "--command", "build",
                "--env", "PKB_NETWORK_ISOLATION=ENFORCED"},
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        assertEquals(1, code);
        assertEquals("{\"status\": \"ERROR\", \"error\": \"prohibited command: gradle build\"}\n",
                stdout());
    }

    @Test
    void executeCommandRequiresAtLeastOneCommand() {
        int code = ExperimentRunnerExecuteCli.run(new String[] {"experiment-runner-execute"},
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        assertEquals(2, code);
        assertTrue(err.toString(StandardCharsets.UTF_8)
                .startsWith("usage: experiment-runner-execute"));
    }
}

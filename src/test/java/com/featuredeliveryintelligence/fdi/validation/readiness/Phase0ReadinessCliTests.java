package com.featuredeliveryintelligence.fdi.validation.readiness;

import com.featuredeliveryintelligence.fdi.application.Phase0ReadinessCli;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Packaged-CLI tests for {@code phase0-readiness-validate}, including
 * byte-for-byte stdout and exit-code parity against the transitional Python
 * CLI {@code tooling/validation/pkb001_gate.py} run through {@code python3}.
 */
class Phase0ReadinessCliTests {
    private static final Path REPOSITORY = Path.of("").toAbsolutePath();
    private static final Path PYTHON_GATE = REPOSITORY.resolve("tooling/validation/pkb001_gate.py");

    @TempDir Path temp;

    @Test
    void handlesOnlyPhase0ReadinessValidateCommand() {
        assertFalse(Phase0ReadinessCli.handles(new String[0]));
        assertFalse(Phase0ReadinessCli.handles(new String[] {"unrelated"}));
        assertTrue(Phase0ReadinessCli.handles(new String[] {"phase0-readiness-validate"}));
    }

    @Test
    void repositoryPhase0RemainsBlockedWithoutExternalEvidence() throws Exception {
        String[] args = new String[] {"--root", REPOSITORY.toString()};
        Result python = pythonGate(args);
        assertEquals(2, python.exitCode());
        Result java = javaGate(args);
        assertEquals(2, java.exitCode());
        assertArrayEquals(python.stdout(), java.stdout());
        assertTrue(new String(java.stdout(), StandardCharsets.UTF_8).contains("P0-01"));
        assertTrue(new String(java.stdout(), StandardCharsets.UTF_8).contains("P0-04"));
    }

    @Test
    void repositoryPhase0IsReadyWithFrozenPhase0Evidence() throws Exception {
        String[] args = new String[] {"--root", REPOSITORY.toString(),
                "--evidence", "validation/pkb001/datasets/phase0-evidence.json"};
        Result python = pythonGate(args);
        assertEquals(0, python.exitCode(), new String(python.stdout(), StandardCharsets.UTF_8));
        Result java = javaGate(args);
        assertEquals(0, java.exitCode(), new String(java.stdout(), StandardCharsets.UTF_8));
        assertArrayEquals(python.stdout(), java.stdout());
    }

    @Test
    void blockedReportWithDefaultEvidenceMatchesPythonBytes() throws Exception {
        String[] args = new String[] {"--root", temp.toString()};
        Result python = pythonGate(args);
        Result java = javaGate(args);
        assertEquals(2, python.exitCode());
        assertEquals(python.exitCode(), java.exitCode());
        assertArrayEquals(python.stdout(), java.stdout());
    }

    @Test
    void outputFileAndStdoutMatchPythonBytes() throws Exception {
        Path pythonOut = temp.resolve("python-report.json");
        Path javaOut = temp.resolve("java-report.json");
        Result python = pythonGate(new String[] {"--root", temp.toString(),
                "--output", pythonOut.toString()});
        Result java = javaGate(new String[] {"--root", temp.toString(),
                "--output", javaOut.toString()});
        assertEquals(python.exitCode(), java.exitCode());
        assertArrayEquals(python.stdout(), java.stdout());
        assertArrayEquals(Files.readAllBytes(pythonOut), Files.readAllBytes(javaOut));
    }

    @Test
    void missingEvidenceFileMatchesPythonErrorBytes() throws Exception {
        String missing = temp.resolve("absent.json").toString();
        String[] args = new String[] {"--root", temp.toString(), "--evidence", missing};
        Result python = pythonGate(args);
        Result java = javaGate(args);
        assertEquals(1, python.exitCode());
        assertEquals(python.exitCode(), java.exitCode());
        assertArrayEquals(python.stdout(), java.stdout());
        assertTrue(new String(java.stdout(), StandardCharsets.UTF_8)
                .contains("[Errno 2] No such file or directory: '" + missing + "'"));
    }

    @Test
    void directoryEvidenceMatchesPythonIsADirectoryError() throws Exception {
        String[] args = new String[] {"--root", temp.toString(), "--evidence", temp.toString()};
        Result python = pythonGate(args);
        Result java = javaGate(args);
        assertEquals(1, python.exitCode());
        assertEquals(python.exitCode(), java.exitCode());
        assertArrayEquals(python.stdout(), java.stdout());
        assertTrue(new String(java.stdout(), StandardCharsets.UTF_8)
                .contains("[Errno 21] Is a directory: '" + temp + "'"));
    }

    @Test
    void nonObjectEvidenceMatchesPythonValueError() throws Exception {
        Path evidence = temp.resolve("array.json");
        Files.writeString(evidence, "[1, 2]\n");
        String[] args = new String[] {"--root", temp.toString(), "--evidence", evidence.toString()};
        Result python = pythonGate(args);
        Result java = javaGate(args);
        assertEquals(1, python.exitCode());
        assertArrayEquals(python.stdout(), java.stdout());
        assertTrue(new String(java.stdout(), StandardCharsets.UTF_8)
                .contains("evidence root must be an object"));
    }

    @Test
    void malformedEvidenceMatchesPythonJsonDecodeError() throws Exception {
        Path evidence = temp.resolve("broken.json");
        Files.writeString(evidence, "{\"unclosed\": ");
        String[] args = new String[] {"--root", temp.toString(), "--evidence", evidence.toString()};
        Result python = pythonGate(args);
        Result java = javaGate(args);
        assertEquals(1, python.exitCode());
        assertArrayEquals(python.stdout(), java.stdout());
    }

    @Test
    void outputOutsideRootMatchesPythonContainmentError() throws Exception {
        Path root = temp.resolve("repo");
        Files.createDirectories(root);
        Path outside = temp.resolve("outside.json");
        String[] args = new String[] {"--root", root.toString(), "--output", outside.toString()};
        Result python = pythonGate(args);
        Result java = javaGate(args);
        assertEquals(1, python.exitCode());
        assertArrayEquals(python.stdout(), java.stdout());
        assertTrue(new String(java.stdout(), StandardCharsets.UTF_8)
                .contains("output path must remain inside repository root"));
        assertFalse(Files.exists(outside));
    }

    @Test
    void outputThroughSymlinkMatchesPythonSymlinkError() throws Exception {
        Path outside = temp.resolve("outside");
        Files.createDirectory(outside);
        Path link = temp.resolve("report-link.json");
        Files.createSymbolicLink(link, outside.resolve("report.json"));
        String[] args = new String[] {"--root", temp.toString(), "--output", "report-link.json"};
        Result python = pythonGate(args);
        Result java = javaGate(args);
        assertEquals(1, python.exitCode());
        assertArrayEquals(python.stdout(), java.stdout());
        assertTrue(new String(java.stdout(), StandardCharsets.UTF_8)
                .contains("output path must not be a symlink"));
    }

    @Test
    void usageErrorForUnknownOptionExitsTwo() {
        Result result = javaGate(new String[] {"--root", temp.toString(), "--bogus", "x"});
        assertEquals(2, result.exitCode());
        assertTrue(new String(result.stderr(), StandardCharsets.UTF_8)
                .contains("usage: phase0-readiness-validate"));
    }

    private static Result javaGate(String[] args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = Phase0ReadinessCli.run(withCommand(args),
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Result(exitCode, stdout.toByteArray(), stderr.toByteArray());
    }

    private static Result pythonGate(String[] args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("python3");
        command.add(PYTHON_GATE.toString());
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).start();
        byte[] stdout = process.getInputStream().readAllBytes();
        byte[] stderr = process.getErrorStream().readAllBytes();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("python gate timed out");
        }
        return new Result(process.exitValue(), stdout, stderr);
    }

    private static String[] withCommand(String[] args) {
        String[] full = new String[args.length + 1];
        full[0] = "phase0-readiness-validate";
        System.arraycopy(args, 0, full, 1, args.length);
        return full;
    }

    private record Result(int exitCode, byte[] stdout, byte[] stderr) { }
}

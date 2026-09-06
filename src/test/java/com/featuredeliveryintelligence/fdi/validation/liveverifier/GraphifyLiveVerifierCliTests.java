package com.featuredeliveryintelligence.fdi.validation.liveverifier;

import com.featuredeliveryintelligence.fdi.application.GraphifyLiveVerifierCli;
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
 * Packaged-CLI tests for {@code graphify-live-verify}, including byte-for-byte
 * stdout, exit-code, and evidence-file parity against the transitional Python
 * CLI {@code tooling/validation/graphify_live_verifier.py} for the NOT_BOUND
 * path on a root without the frozen runtime.
 */
class GraphifyLiveVerifierCliTests {
    private static final Path REPOSITORY = Path.of("").toAbsolutePath();
    private static final Path PYTHON_VERIFIER =
            REPOSITORY.resolve("tooling/validation/graphify_live_verifier.py");

    @TempDir Path temp;

    @Test
    void handlesOnlyGraphifyLiveVerifyCommand() {
        assertFalse(GraphifyLiveVerifierCli.handles(new String[0]));
        assertFalse(GraphifyLiveVerifierCli.handles(new String[] {"unrelated"}));
        assertTrue(GraphifyLiveVerifierCli.handles(new String[] {"graphify-live-verify"}));
    }

    @Test
    void missingRuntimeRootMatchesPythonBytes() throws Exception {
        Path missingRoot = temp.resolve("missing-runtime-root");
        Path pythonOutput = temp.resolve("python-evidence.json");
        Path javaOutput = temp.resolve("java-evidence.json");
        String[] shared = new String[] {"--root", missingRoot.toString()};
        Result python = pythonVerifier(shared, pythonOutput);
        Result java = javaVerifier(shared, javaOutput);

        assertEquals(2, python.exitCode());
        assertEquals(python.exitCode(), java.exitCode());
        assertArrayEquals(python.stdout(), java.stdout());
        assertArrayEquals(Files.readAllBytes(pythonOutput), Files.readAllBytes(javaOutput));
        String evidence = new String(java.stdout(), StandardCharsets.UTF_8);
        assertTrue(evidence.contains("\"result\": \"NOT_BOUND\""));
        assertTrue(evidence.contains("Graphify runtime is missing: "));
    }

    @Test
    void emptyExistingRootMatchesPythonBytes() throws Exception {
        Path emptyRoot = temp.resolve("empty-root");
        Files.createDirectories(emptyRoot);
        Path pythonOutput = temp.resolve("python-evidence.json");
        Path javaOutput = temp.resolve("java-evidence.json");
        String[] shared = new String[] {"--root", emptyRoot.toString()};
        Result python = pythonVerifier(shared, pythonOutput);
        Result java = javaVerifier(shared, javaOutput);

        assertEquals(2, python.exitCode());
        assertEquals(python.exitCode(), java.exitCode());
        assertArrayEquals(python.stdout(), java.stdout());
        assertArrayEquals(Files.readAllBytes(pythonOutput), Files.readAllBytes(javaOutput));
    }

    @Test
    void usageErrorForMissingOutputExitsTwo() {
        Result result = javaVerifier(new String[] {"--root", temp.toString()}, null);
        assertEquals(2, result.exitCode());
        assertTrue(result.stderrText().contains("usage: graphify-live-verify"));
    }

    private static Result javaVerifier(String[] args, Path output) {
        List<String> full = new ArrayList<>();
        full.add("graphify-live-verify");
        full.addAll(List.of(args));
        if (output != null) {
            full.add("--output");
            full.add(output.toString());
        }
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = GraphifyLiveVerifierCli.run(full.toArray(String[]::new),
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Result(exitCode, stdout.toByteArray(), stderr.toByteArray());
    }

    private static Result pythonVerifier(String[] args, Path output) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("python3");
        command.add(PYTHON_VERIFIER.toString());
        command.addAll(List.of(args));
        command.add("--output");
        command.add(output.toString());
        Process process = new ProcessBuilder(command).start();
        byte[] stdout = process.getInputStream().readAllBytes();
        byte[] stderr = process.getErrorStream().readAllBytes();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("python verifier timed out");
        }
        return new Result(process.exitValue(), stdout, stderr);
    }

    private record Result(int exitCode, byte[] stdout, byte[] stderr) {
        public String stderrText() {
            return new String(stderr, StandardCharsets.UTF_8);
        }
    }
}

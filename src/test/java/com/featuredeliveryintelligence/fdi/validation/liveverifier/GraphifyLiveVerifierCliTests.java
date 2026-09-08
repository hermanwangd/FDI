package com.featuredeliveryintelligence.fdi.validation.liveverifier;

import com.featuredeliveryintelligence.fdi.application.GraphifyLiveVerifierCli;
import com.featuredeliveryintelligence.fdi.validation.readiness.Phase0Readiness;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Packaged-CLI tests for {@code graphify-live-verify} with byte-for-byte
 * stdout, exit-code, and evidence-file assertions against frozen reference
 * bytes captured from the removed transitional Python CLI
 * {@code tooling/validation/graphify_live_verifier.py} (run through
 * {@code python3}) at the BL-026 combined integration, for the NOT_BOUND path
 * on a root without the frozen runtime. The frozen bytes are the Python
 * consumer's actual stdout for each case's exact inputs; they are asserted as
 * immutable reference bytes, never re-executed. The {@code server_error}
 * embeds the capture-time root path; the test substitutes its own
 * symlink-resolved root at runtime via {@link Phase0Readiness#resolveLoose}.
 */
class GraphifyLiveVerifierCliTests {
    /** Capture-time roots, substituted with the test's own roots at runtime. */
    private static final String CAPTURE_MISSING_ROOT = "/private/tmp/frozen/glv/missing-runtime-root";
    private static final String CAPTURE_EMPTY_ROOT = "/private/tmp/frozen/glv/empty-root";

    private static final String FROZEN_NOT_BOUND_MISSING_ROOT = """
{
  "verification_id": "pkb001-graphify-live-818c413",
  "result": "NOT_BOUND",
  "queryable": false,
  "server_exit_status": "ERROR",
  "server_error": "Graphify runtime is missing: /private/tmp/frozen/glv/missing-runtime-root/.fdi-work/graphify-venv312/bin/python"
}
""";

    private static final String FROZEN_NOT_BOUND_EMPTY_ROOT = """
{
  "verification_id": "pkb001-graphify-live-818c413",
  "result": "NOT_BOUND",
  "queryable": false,
  "server_exit_status": "ERROR",
  "server_error": "Graphify runtime is missing: /private/tmp/frozen/glv/empty-root/.fdi-work/graphify-venv312/bin/python"
}
""";


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
        Path javaOutput = temp.resolve("java-evidence.json");
        Result java = javaVerifier(
                new String[] {"--root", missingRoot.toString()}, javaOutput);

        assertEquals(2, java.exitCode());
        byte[] expected = FROZEN_NOT_BOUND_MISSING_ROOT
                .replace(CAPTURE_MISSING_ROOT, Phase0Readiness.resolveLoose(missingRoot).toString())
                .getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, java.stdout());
        assertArrayEquals(expected, Files.readAllBytes(javaOutput));
        String evidence = new String(java.stdout(), StandardCharsets.UTF_8);
        assertTrue(evidence.contains("\"result\": \"NOT_BOUND\""));
        assertTrue(evidence.contains("Graphify runtime is missing: "));
    }

    @Test
    void emptyExistingRootMatchesPythonBytes() throws Exception {
        Path emptyRoot = temp.resolve("empty-root");
        Files.createDirectories(emptyRoot);
        Path javaOutput = temp.resolve("java-evidence.json");
        Result java = javaVerifier(
                new String[] {"--root", emptyRoot.toString()}, javaOutput);

        assertEquals(2, java.exitCode());
        byte[] expected = FROZEN_NOT_BOUND_EMPTY_ROOT
                .replace(CAPTURE_EMPTY_ROOT, Phase0Readiness.resolveLoose(emptyRoot).toString())
                .getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, java.stdout());
        assertArrayEquals(expected, Files.readAllBytes(javaOutput));
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

    private record Result(int exitCode, byte[] stdout, byte[] stderr) {
        public String stderrText() {
            return new String(stderr, StandardCharsets.UTF_8);
        }
    }
}

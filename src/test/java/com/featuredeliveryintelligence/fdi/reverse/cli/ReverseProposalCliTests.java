package com.featuredeliveryintelligence.fdi.reverse.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Slice G wiring tests for {@link ReverseProposalCli}: command dispatch,
 * fail-closed usage and input refusals, immutable run-directory maintenance,
 * and two independent in-process replays that must reproduce the committed
 * pinned Petclinic artifacts byte-for-byte across separate generation passes.
 */
class ReverseProposalCliTests {

    private static final Path REPOSITORY_ROOT = Path.of("").toAbsolutePath();
    private static final Path COMMITTED_RUN_DIRECTORY = Path.of(
            "validation/pkb001/reverse-pkb-bl009-petclinic-001/proposal-run");
    private static final String PACKAGE_SHA256 =
            "ddc8c444a7bd9b77da08c768e101309980b0cbc0b2efa76dcc8c72d10dfabe4c";

    @TempDir
    Path tempDirectory;

    @Test
    void dispatchesOnlyTheReverseProposeCommand() {
        assertTrue(ReverseProposalCli.handles(new String[] {"reverse-propose", "--repository-root", "."}));
        assertFalse(ReverseProposalCli.handles(new String[] {"reverse-proposal"}));
        assertFalse(ReverseProposalCli.handles(new String[] {}));
        assertFalse(ReverseProposalCli.handles(null));
    }

    @Test
    void twoIndependentRunsReproduceTheCommittedArtifactsByteForByte() throws Exception {
        Path first = tempDirectory.resolve("run-one");
        Path second = tempDirectory.resolve("run-two");

        String firstStatus = runOk("--output-dir", first.toString());
        String secondStatus = runOk("--output-dir", second.toString());

        byte[] packageOne = Files.readAllBytes(first.resolve("proposal-package.json"));
        byte[] packageTwo = Files.readAllBytes(second.resolve("proposal-package.json"));
        byte[] reportOne = Files.readAllBytes(first.resolve("comparison-report.json"));
        byte[] reportTwo = Files.readAllBytes(second.resolve("comparison-report.json"));

        assertArrayEquals(packageOne, packageTwo, "two replays must seal byte-identical packages");
        assertArrayEquals(reportOne, reportTwo, "two replays must seal byte-identical comparison reports");
        assertArrayEquals(Files.readAllBytes(
                        REPOSITORY_ROOT.resolve(COMMITTED_RUN_DIRECTORY).resolve("proposal-package.json")),
                packageOne, "regenerated package must equal the committed immutable artifact");
        assertArrayEquals(Files.readAllBytes(
                        REPOSITORY_ROOT.resolve(COMMITTED_RUN_DIRECTORY).resolve("comparison-report.json")),
                reportOne, "regenerated report must equal the committed immutable artifact");

        assertEquals(PACKAGE_SHA256, sha256Hex(packageOne));
        assertTrue(firstStatus.contains("\"status\":\"OK\""));
        assertTrue(firstStatus.contains("\"capabilities\":18"));
        assertTrue(firstStatus.contains("\"scenarios\":76"));
        assertTrue(firstStatus.contains("\"evidence_gaps\":7043"));
        assertTrue(secondStatus.contains("\"proposal_package_sha256\":\"" + PACKAGE_SHA256 + "\""),
                "both replays must report the identical package digest");
    }

    @Test
    void driftedCommittedArtifactFailsClosedAndIsNotOverwritten() throws Exception {
        Path output = tempDirectory.resolve("drifted");
        Files.createDirectories(output);
        byte[] drifted = "{\"drift\": true}\n".getBytes(StandardCharsets.UTF_8);
        Files.write(output.resolve("proposal-package.json"), drifted);

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        int exit = ReverseProposalCli.run(
                new String[] {"reverse-propose", "--repository-root", REPOSITORY_ROOT.toString(),
                        "--output-dir", output.toString()},
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));

        assertEquals(1, exit);
        assertTrue(stdout.toString(StandardCharsets.UTF_8).contains("\"status\":\"ERROR\""));
        assertArrayEquals(drifted, Files.readAllBytes(output.resolve("proposal-package.json")),
                "a drifted artifact must never be overwritten");
        assertFalse(Files.exists(output.resolve("comparison-report.json")));
    }

    @Test
    void unreadableRepositoryRootFailsClosed() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        int exit = ReverseProposalCli.run(
                new String[] {"reverse-propose", "--repository-root",
                        tempDirectory.resolve("no-such-checkout").toString()},
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
        assertEquals(1, exit);
        assertTrue(stdout.toString(StandardCharsets.UTF_8).contains("\"status\":\"ERROR\""));
    }

    @Test
    void digestMismatchOnBoundInputFailsClosed() throws Exception {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        int exit = ReverseProposalCli.run(
                new String[] {"reverse-propose", "--repository-root", REPOSITORY_ROOT.toString(),
                        "--graph-sha256", "0".repeat(64),
                        "--output-dir", tempDirectory.resolve("mismatch").toString()},
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
        assertEquals(1, exit);
        String output = stdout.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\"status\":\"ERROR\""));
        assertTrue(output.contains("DIGEST_MISMATCH"), "bound graph digest mismatch must fail closed: " + output);
    }

    @Test
    void usageErrorsExit2() {
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int missing = ReverseProposalCli.run(
                new String[] {"reverse-propose"},
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        assertEquals(2, missing);
        assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("missing required option --repository-root"));

        int unknown = ReverseProposalCli.run(
                new String[] {"reverse-propose", "--repository-root", ".", "--surplus", "x"},
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
        assertEquals(2, unknown);
    }

    private static String runOk(String... extraArgs) throws Exception {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        String[] args = new String[extraArgs.length + 3];
        args[0] = "reverse-propose";
        args[1] = "--repository-root";
        args[2] = REPOSITORY_ROOT.toString();
        System.arraycopy(extraArgs, 0, args, 3, extraArgs.length);
        int exit = ReverseProposalCli.run(
                args,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
        assertEquals(0, exit, "CLI refused: " + stdout);
        return stdout.toString(StandardCharsets.UTF_8);
    }

    private static String sha256Hex(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte value : digest) builder.append(String.format("%02x", value));
        return builder.toString();
    }
}

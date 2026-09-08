package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.validation.nextrun.GateTestRoots;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
 * Ports the two CLI subprocess cases of {@code tests/test_pkb001_next_run_gate.py}
 * to the packaged {@code next-run-validate} command: the deterministic blocked
 * report with exclusive-create refusal, and the escaping-parent-symlink refusal.
 */
class NextRunGateCliTests {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir Path temp;

    @Test
    void handlesOnlyNextRunValidateCommand() {
        assertFalse(NextRunGateCli.handles(new String[0]));
        assertFalse(NextRunGateCli.handles(new String[]{"unrelated"}));
        assertTrue(NextRunGateCli.handles(new String[]{"next-run-validate"}));
    }

    @Test
    void cliWritesDeterministicBlockedReportAndRefusesOverwrite() throws Exception {
        Path root = GateTestRoots.newGateRoot(temp);
        Files.writeString(root.resolve("request.json"), "{}\n", StandardCharsets.UTF_8);
        String[] args = new String[] {"next-run-validate", "--root", root.toString(),
                "--request", "request.json", "--report", "report.json"};

        Result first = run(args);
        assertEquals(1, first.exitCode());
        byte[] reportBytes = Files.readAllBytes(root.resolve("report.json"));
        assertTrue(new String(reportBytes, StandardCharsets.UTF_8).endsWith("\n"));
        assertEquals(0, JSON.readTree(reportBytes).get("mappings").size());

        Result second = run(args);
        assertEquals(1, second.exitCode());
        assertTrue(second.stderr().contains("cannot exclusively create report"), second.stderr());
        assertArrayEquals(reportBytes, Files.readAllBytes(root.resolve("report.json")));
    }

    @Test
    void cliRefusesReportPathThroughEscapingParentSymlink() throws Exception {
        Path root = GateTestRoots.newGateRoot(temp);
        Files.writeString(root.resolve("request.json"), "{}\n", StandardCharsets.UTF_8);
        Path outside = temp.resolve("outside");
        Files.createDirectory(outside);
        Files.createSymbolicLink(root.resolve("escape"), outside);

        Result result = run(new String[] {"next-run-validate", "--root", root.toString(),
                "--request", "request.json", "--report", "escape/report.json"});

        assertEquals(1, result.exitCode());
        assertFalse(Files.exists(outside.resolve("report.json")));
    }

    @Test
    void cliExitsZeroForReadyRequestAndMatchesApiReportBytes() throws Exception {
        Path root = GateTestRoots.newGateRoot(temp);
        ObjectNode request = GateTestRoots.validRequest(root);
        Files.write(root.resolve("request.json"),
                (JSON.writeValueAsString(request) + "\n").getBytes(StandardCharsets.UTF_8));

        Result result = run(new String[] {"next-run-validate", "--root", root.toString(),
                "--request", "request.json", "--report", "report.json"});

        assertEquals("", result.stderr());
        assertEquals(0, result.exitCode());
        byte[] reportBytes = Files.readAllBytes(root.resolve("report.json"));
        assertTrue(new String(reportBytes, StandardCharsets.UTF_8).endsWith("\n"));
        assertEquals("READY", JSON.readTree(reportBytes).get("status").asText());
    }

    @Test
    void cliRefusesNoncanonicalReportPathWithoutWriting() throws Exception {
        Path root = GateTestRoots.newGateRoot(temp);
        Files.writeString(root.resolve("request.json"), "{}\n", StandardCharsets.UTF_8);

        Result result = run(new String[] {"next-run-validate", "--root", root.toString(),
                "--request", "request.json", "--report", "../escape.json"});

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("report path must be canonical and repository-relative"),
                result.stderr());
        assertFalse(Files.exists(root.resolve("escape.json")));
    }

    @Test
    void cliReportsUsageErrorForMissingRequestOption() throws Exception {
        Path root = GateTestRoots.newGateRoot(temp);
        Result result = run(new String[] {"next-run-validate", "--root", root.toString(),
                "--report", "report.json"});
        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("--request"), result.stderr());
    }

    private static Result run(String[] args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = NextRunGateCli.run(args,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Result(exitCode,
                stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
    }

    private record Result(int exitCode, String stdout, String stderr) { }
}

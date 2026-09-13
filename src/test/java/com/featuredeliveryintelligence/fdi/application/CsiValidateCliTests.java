package com.featuredeliveryintelligence.fdi.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsiValidateCliTests {
    @TempDir Path temp;

    @Test
    void handlesOnlyCsiValidateCommand() {
        assertTrue(CsiValidateCli.handles(new String[]{"csi-validate"}));
        assertFalse(CsiValidateCli.handles(new String[]{"other"}));
    }

    @Test
    void writesDeterministicReportAndRefusesOverwrite() throws Exception {
        Path input = temp.resolve("record.json");
        Path report = temp.resolve("report.json");
        Files.writeString(input, validRecord(), StandardCharsets.UTF_8);

        Result first = run(new String[]{"csi-validate", "--input", input.toString(), "--report", report.toString()});
        assertEquals(0, first.exitCode());
        byte[] bytes = Files.readAllBytes(report);
        assertTrue(new String(bytes, StandardCharsets.UTF_8).contains("\"status\" : \"VALID\""));

        Result second = run(new String[]{"csi-validate", "--input", input.toString(), "--report", report.toString()});
        assertEquals(1, second.exitCode());
        assertTrue(second.stderr().contains("report already exists"));
        assertEquals(new String(bytes, StandardCharsets.UTF_8), Files.readString(report));
    }

    @Test
    void invalidInputReturnsOneAndUsageErrorReturnsTwo() throws Exception {
        Path input = temp.resolve("bad.json");
        Path report = temp.resolve("bad-report.json");
        Files.writeString(input, "{}\n");
        assertEquals(1, run(new String[]{"csi-validate", "--input", input.toString(), "--report", report.toString()}).exitCode());
        assertEquals(2, run(new String[]{"csi-validate", "--input", input.toString()}).exitCode());
    }

    private static Result run(String[] args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exit = CsiValidateCli.run(args, new PrintStream(out), new PrintStream(err));
        return new Result(exit, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    private static String validRecord() {
        return """
                {"recommendation_id":"CSI-REC-001","disposition":"RECOMMENDED_NOT_SELECTED","tag":"CODE",
                 "origin_evidence":[{"identity":"review-1","origin_type":"INDEPENDENT_REVIEW","durable_ref":"validation/review.md"}],
                 "candidate_revision":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","execution_identity":"N/A: review finding",
                 "verdict_identity":"review-1:FAIL","affected_requirement":"EVID-001","insufficiency":"unsafe resolution",
                 "proposed_control":"fail closed","affected_kpis":[],"revision_route":"SF-BL-005:T3"}
                """;
    }

    private record Result(int exitCode, String stdout, String stderr) { }
}

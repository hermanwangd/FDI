package com.featuredeliveryintelligence.fdi.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Dev204ReportAuditCliTests {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void findingsArePrintedAsJsonAndProduceACommandFailure() throws Exception {
        Path evidenceFile = tempDir.resolve("report-evidence.json");
        Files.writeString(evidenceFile, "{\"parent_issue\":{},\"current_run\":{},\"draft_body\":\"\"}");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.out;

        try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            IllegalStateException failure = assertThrows(IllegalStateException.class, () ->
                    Dev204Cli.handles(new String[] {
                        "dev204-report-audit",
                        "--evidence-file", evidenceFile.toString(),
                        "--phase", "prepublication"
                    }));
            assertTrue(failure.getMessage().contains("report audit found findings"));
        } finally {
            System.setOut(original);
        }

        JsonNode result = JSON.readTree(output.toString(StandardCharsets.UTF_8));
        assertEquals("PREPUBLICATION", result.path("phase").asText());
        assertEquals("FINDINGS", result.path("status").asText());
        assertEquals(1, result.path("exitStatus").asInt());
        assertEquals("NONZERO_FINDINGS", result.path("processOutcome").asText());
        assertEquals("REPORT_EVIDENCE_AUDIT_ONLY_NO_CONTROL_VERDICT_NO_WRITE",
                result.path("claimBoundary").asText());
        assertFalse(result.path("findings").isEmpty());
    }

    @Test
    void prepublicationAndPostpublicationCleanResultsReturnSuccessJson() throws Exception {
        ObjectNode prepublication = validEvidence();
        JsonNode preResult = invoke(prepublication, "prepublication");
        assertEquals("CLEAN", preResult.path("status").asText());
        assertEquals(0, preResult.path("exitStatus").asInt());
        assertTrue(preResult.path("findings").isEmpty());

        ObjectNode postpublication = validEvidence();
        ObjectNode readback = postpublication.putObject("report_readback");
        readback.put("comment_id", "00000000-0000-7000-8000-00000000000b");
        readback.put("issue_id", "00000000-0000-7000-8000-000000000001");
        readback.put("author_id", "00000000-0000-7000-8000-000000000006");
        readback.put("source_task_id", "00000000-0000-7000-8000-000000000008");
        readback.put("created_at", "2026-10-02T09:05:00Z");
        readback.put("body", postpublication.path("draft_body").asText());

        JsonNode postResult = invoke(postpublication, "postpublication");
        assertEquals("CLEAN", postResult.path("status").asText());
        assertEquals(0, postResult.path("exitStatus").asInt());
        assertTrue(postResult.path("findings").isEmpty());
    }

    @Test
    void unsupportedPhaseAndMissingEvidenceAreRejectedBeforeAudit() throws Exception {
        Path evidenceFile = tempDir.resolve("report-evidence.json");
        Files.writeString(evidenceFile, "{}");

        assertThrows(IllegalArgumentException.class, () -> Dev204Cli.handles(new String[] {
            "dev204-report-audit", "--evidence-file", evidenceFile.toString(), "--phase", "publish"
        }));
        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
                () -> Dev204Cli.handles(new String[] {
                    "dev204-report-audit", "--evidence-file", tempDir.resolve("absent.json").toString(),
                    "--phase", "postpublication"
                }));
        assertTrue(missing.getMessage().contains("cannot read the report evidence file"));
    }

    private JsonNode invoke(ObjectNode evidence, String phase) throws Exception {
        Path evidenceFile = tempDir.resolve("report-evidence-" + phase + ".json");
        JSON.writeValue(evidenceFile.toFile(), evidence);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try (PrintStream captured = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            assertTrue(Dev204Cli.handles(new String[] {
                "dev204-report-audit", "--evidence-file", evidenceFile.toString(), "--phase", phase
            }));
        } finally {
            System.setOut(original);
        }
        return JSON.readTree(output.toString(StandardCharsets.UTF_8));
    }

    private static ObjectNode validEvidence() {
        ObjectNode evidence = JSON.createObjectNode();
        evidence.putObject("parent_issue")
                .put("id", "00000000-0000-7000-8000-000000000001")
                .put("key", "RC10VAL-100")
                .put("created_at", "2026-10-02T09:00:00Z");
        evidence.put("orchestrator_agent_id", "00000000-0000-7000-8000-000000000006");
        evidence.set("current_run", run("00000000-0000-7000-8000-000000000008",
                "00000000-0000-7000-8000-000000000001", "00000000-0000-7000-8000-000000000006",
                "running", "2026-10-02T09:00:30Z", "2026-10-02T09:01:00Z", null));

        ArrayNode children = evidence.putArray("children");
        children.addObject().put("id", "00000000-0000-7000-8000-000000000004")
                .put("key", "RC10VAL-101").put("required", true).put("created_at", "2026-10-02T09:01:15Z");

        ArrayNode runs = evidence.putArray("runs");
        runs.add(run("00000000-0000-7000-8000-000000000008",
                "00000000-0000-7000-8000-000000000001", "00000000-0000-7000-8000-000000000006",
                "running", "2026-10-02T09:00:30Z", "2026-10-02T09:01:00Z", null));
        runs.add(run("00000000-0000-7000-8000-000000000009",
                "00000000-0000-7000-8000-000000000004", "00000000-0000-7000-8000-00000000000d",
                "completed", "2026-10-02T09:01:30Z", "2026-10-02T09:02:00Z", "2026-10-02T09:04:00Z"));

        ArrayNode comments = evidence.putArray("comments");
        comments.addObject().put("id", "00000000-0000-7000-8000-00000000000e")
                .put("issue_id", "00000000-0000-7000-8000-000000000001")
                .put("author_id", "00000000-0000-7000-8000-000000000006")
                .put("source_task_id", "00000000-0000-7000-8000-000000000008")
                .put("content", "## C3 implementation plan\nCLI fixture source plan.")
                .put("created_at", "2026-10-02T09:00:45Z");
        comments.addObject().put("id", "00000000-0000-7000-8000-00000000000a")
                .put("issue_id", "00000000-0000-7000-8000-000000000004")
                .put("author_id", "00000000-0000-7000-8000-00000000000d")
                .put("source_task_id", "00000000-0000-7000-8000-000000000009")
                .put("subject_revision", "r1")
                .put("created_at", "2026-10-02T09:03:00Z");

        ArrayNode artifacts = evidence.putArray("artifacts");
        artifacts.addObject().put("id", "artifact-r2")
                .put("locator", "https://artifacts.example.test/candidate/revision-r2")
                .put("issue_id", "00000000-0000-7000-8000-000000000004")
                .put("source_task_id", "00000000-0000-7000-8000-000000000009")
                .put("subject_revision", "revision-r2")
                .put("created_at", "2026-10-02T09:03:30Z");

        ArrayNode references = evidence.putArray("expected_references");
        references.addObject().put("issue_key", "RC10VAL-100")
                .put("issue_id", "00000000-0000-7000-8000-000000000001")
                .put("comment_id", "00000000-0000-7000-8000-00000000000e")
                .put("run_id", "00000000-0000-7000-8000-000000000008")
                .put("subject_revision", "UNKNOWN")
                .put("source_digest", "sha256:01efa12cb637b25d6846ace42f6e76e335ee7836d3ef3caf56ed2216c44d52da")
                .put("required", true);
        references.addObject().put("issue_key", "RC10VAL-101")
                .put("issue_id", "00000000-0000-7000-8000-000000000004")
                .put("comment_id", "00000000-0000-7000-8000-00000000000a")
                .put("run_id", "00000000-0000-7000-8000-000000000009")
                .put("subject_revision", "r1").put("required", true);
        references.addObject().put("issue_key", "RC10VAL-101")
                .put("issue_id", "00000000-0000-7000-8000-000000000004")
                .put("artifact_locator", "https://artifacts.example.test/candidate/revision-r2")
                .put("run_id", "00000000-0000-7000-8000-000000000009")
                .put("subject_revision", "revision-r2").put("required", true);
        evidence.put("draft_body", validReportBody());
        return evidence;
    }

    private static ObjectNode run(
            String id, String issueId, String agentId, String status,
            String dispatchedAt, String startedAt, String completedAt) {
        ObjectNode run = JSON.createObjectNode();
        run.put("id", id);
        run.put("issue_id", issueId);
        run.put("agent_id", agentId);
        run.put("status", status);
        run.put("dispatched_at", dispatchedAt);
        run.put("started_at", startedAt);
        if (completedAt != null) run.put("completed_at", completedAt);
        return run;
    }

    private static String validReportBody() {
        return """
                Mission: [RC10VAL-100](mention://issue/00000000-0000-7000-8000-000000000001)
                Current parent run: 00000000-0000-7000-8000-000000000008

                ## UTC typed events
                2026-10-02T09:00:00Z Parent issue created [RC10VAL-100](mention://issue/00000000-0000-7000-8000-000000000001)
                2026-10-02T09:00:30Z Run dispatched 00000000-0000-7000-8000-000000000008
                2026-10-02T09:00:45Z Comment created 00000000-0000-7000-8000-00000000000e
                2026-10-02T09:01:00Z Run started 00000000-0000-7000-8000-000000000008
                2026-10-02T09:01:15Z Child issue created [RC10VAL-101](mention://issue/00000000-0000-7000-8000-000000000004)
                2026-10-02T09:01:30Z Run dispatched 00000000-0000-7000-8000-000000000009
                2026-10-02T09:02:00Z Run started 00000000-0000-7000-8000-000000000009
                2026-10-02T09:03:00Z Comment created 00000000-0000-7000-8000-00000000000a
                2026-10-02T09:03:30Z Artifact created https://artifacts.example.test/candidate/revision-r2
                2026-10-02T09:04:00Z Run completed 00000000-0000-7000-8000-000000000009

                ## Required fan-in
                | [RC10VAL-100](mention://issue/00000000-0000-7000-8000-000000000001) | comment 00000000-0000-7000-8000-00000000000e | run 00000000-0000-7000-8000-000000000008 | revision `UNKNOWN` | source digest `sha256:01efa12cb637b25d6846ace42f6e76e335ee7836d3ef3caf56ed2216c44d52da` |
                | [RC10VAL-101](mention://issue/00000000-0000-7000-8000-000000000004) | comment 00000000-0000-7000-8000-00000000000a | run 00000000-0000-7000-8000-000000000009 | revision `r1` |
                | [RC10VAL-101](mention://issue/00000000-0000-7000-8000-000000000004) | https://artifacts.example.test/candidate/revision-r2 | run 00000000-0000-7000-8000-000000000009 | revision `revision-r2` |
                """;
    }
}

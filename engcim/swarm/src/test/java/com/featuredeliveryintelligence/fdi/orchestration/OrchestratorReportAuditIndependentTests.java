package com.featuredeliveryintelligence.fdi.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Independent synthetic receipts; no runtime, policy verdict, or artifact publication is exercised. */
class OrchestratorReportAuditIndependentTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PARENT = "00000000-0000-7000-8000-00000000000f";
    private static final String CHILD = "00000000-0000-7000-8000-000000000010";
    private static final String ORCHESTRATOR = "00000000-0000-7000-8000-000000000011";
    private static final String WORKER = "00000000-0000-7000-8000-000000000012";
    private static final String C3_RUN = "00000000-0000-7000-8000-000000000013";
    private static final String C5_RUN = "00000000-0000-7000-8000-000000000014";
    private static final String CHILD_RUN = "00000000-0000-7000-8000-000000000015";
    private static final String C3_COMMENT = "00000000-0000-7000-8000-000000000016";
    private static final String CHILD_COMMENT = "00000000-0000-7000-8000-000000000017";
    private static final String PROGRESS_COMMENT = "00000000-0000-7000-8000-000000000018";
    private static final String PARENT_LINK = "[RC10VAL-900](mention://issue/" + PARENT + ")";
    private static final String CHILD_LINK = "[RC10VAL-901](mention://issue/" + CHILD + ")";
    private static final String DESIGN_REVISION = "domain-design-r7";
    private static final String ARTIFACT_REVISION = "artifact-build-r12";
    private static final String GIT_COMMIT = "0123456789abcdef0123456789abcdef01234567";
    private static final String ARTIFACT = "https://artifacts.example.invalid/synthetic/audit-candidate";
    private static final String ARTIFACT_CONTENT = "synthetic independent report artifact\ncontent version seven\n";
    private static final String CONTENT_IDENTITY = "sha256:" + sha256(ARTIFACT_CONTENT);

    @Test
    void conflictingDuplicateReceiptsFailRegardlessOfInputOrder() {
        for (String collection : List.of("children", "runs", "comments", "artifacts")) {
            ObjectNode input = evidence(false);
            ArrayNode rows = input.withArray(collection);
            ObjectNode original = (ObjectNode) rows.get(0);
            ObjectNode conflict = original.deepCopy();
            conflict.put("duplicate_test_marker", "conflicting receipt bytes");
            rows.add(conflict);
            var forward = audit("duplicate-" + collection, input);
            rows.set(0, conflict);
            rows.set(rows.size() - 1, original);
            var reverse = audit("reversed-duplicate-" + collection, input);
            assertEquals(1, forward.exitStatus());
            assertEquals(forward.findings(), reverse.findings());
            assertTrue(forward.findings().stream().anyMatch(f ->
                    f.code().equals("CONFLICTING_RECEIPT_IDENTITY")));
        }
    }

    @Test
    void parentAndCurrentRunCannotBeShadowedByConflictingListReceipts() {
        for (var pair : Map.of("children", "parent_issue", "runs", "current_run").entrySet()) {
            ObjectNode input = evidence(false);
            ObjectNode shadow = input.path(pair.getValue()).deepCopy();
            shadow.put("duplicate_test_marker", "conflict");
            input.withArray(pair.getKey()).add(shadow);
            var result = audit("shadow-" + pair.getValue(), input);
            assertEquals(1, result.exitStatus());
            assertTrue(codes(result).contains("CONFLICTING_RECEIPT_IDENTITY"));
        }
    }

    @Test
    void artifactIdCannotShadowAnotherArtifactsLocator() {
        ObjectNode input = evidence(false);
        input.withArray("artifacts").addObject().put("id", ARTIFACT)
                .put("locator", "fixture:different-artifact").put("content", "different bytes");
        assertTrue(codes(audit("artifact-alias", input)).contains("CONFLICTING_RECEIPT_IDENTITY"));
    }

    @Test
    void identicalReceiptRedeliveryDoesNotChangeAudit() {
        ObjectNode input = evidence(false);
        var original = audit("before-redelivery", input);
        for (String collection : List.of("children", "runs", "comments", "artifacts")) {
            ArrayNode rows = input.withArray(collection);
            rows.add(rows.get(0).deepCopy());
        }
        var redelivered = audit("after-redelivery", input);
        assertEquals(original.status(), redelivered.status());
        assertEquals(original.findings(), redelivered.findings());
    }

    @Test
    void requiredC3FromAnEarlierParentRunResolvesByItsExactReference() {
        assertClean(audit("cross-run-c3-exact-reference", evidence(false)));
    }

    @Test
    void requiredC3CannotDisappearFromBothTheLedgerAndFanInRow() {
        ObjectNode evidence = evidence(false);
        evidence.withArray("expected_references").remove(0);
        String original = evidence.path("draft_body").asText();
        assertTrue(original.contains(c3Row()));
        evidence.put("draft_body", original.replace(c3Row() + "\n", ""));
        // Native required C3, its original run, and its typed UTC event remain independently present.
        assertEquals(C3_RUN, evidence.path("comments").get(0).path("source_task_id").asText());
        assertTrue(evidence.path("comments").get(0).path("required").asBoolean());
        assertTrue(evidence.path("draft_body").asText().contains(c3Event()));

        OrchestratorReportAudit.AuditResult result = audit("cross-run-c3-ledger-and-row-omitted", evidence);

        assertEquals("FINDINGS", result.status(), result.toString());
        assertEquals(1, result.exitStatus());
        assertTrue(result.findings().stream().anyMatch(finding ->
                finding.subject().equals(C3_COMMENT) && finding.code().contains("REFERENCE")),
                result.findings().toString());
        assertNoTimelineFindings(result);
    }

    @Test
    void ordinaryCurrentRunProgressDoesNotBecomeARequiredDecisionReference() {
        ObjectNode evidence = evidence(true);
        assertFalse(evidence.withArray("expected_references").toString().contains(PROGRESS_COMMENT));

        assertClean(audit("ordinary-progress-with-complete-utc-event", evidence));
    }

    @Test
    void missingRequiredChildSourceIsStillDetectedWhenOrdinaryProgressExists() {
        ObjectNode evidence = evidence(true);
        evidence.withArray("comments").remove(1);
        String original = evidence.path("draft_body").asText();
        assertTrue(original.contains(childCommentEvent()));
        evidence.put("draft_body", original.replace(childCommentEvent() + "\n", ""));

        OrchestratorReportAudit.AuditResult result = audit("required-child-source-missing-with-progress", evidence);

        assertEquals("FINDINGS", result.status());
        assertTrue(result.findings().stream().anyMatch(finding ->
                finding.code().equals("COMMENT_RECEIPT_MISSING") && finding.subject().equals(CHILD_COMMENT)),
                result.findings().toString());
        assertNoTimelineFindings(result);
    }

    @Test
    void independentlyHashedArtifactBytesResolveWithoutConfusingOtherRevisionTypes() {
        ObjectNode evidence = evidence(false);
        assertEquals(64, CONTENT_IDENTITY.substring("sha256:".length()).length());
        assertFalse(DESIGN_REVISION.equals(CONTENT_IDENTITY));
        assertFalse(GIT_COMMIT.equals(CONTENT_IDENTITY));
        assertEquals(3, evidence.path("comments").get(0).path("revision").asInt());

        assertClean(audit("artifact-independent-content-sha-valid", evidence));
    }

    @Test
    void matchingReportAndLedgerWithAValidLengthWrongShaCannotOverrideNativeBytes() {
        ObjectNode evidence = evidence(false);
        String wrongIdentity = "sha256:" + "f".repeat(64);
        assertTrue(wrongIdentity.substring("sha256:".length()).matches("[0-9a-f]{64}"));
        assertFalse(wrongIdentity.equals(CONTENT_IDENTITY));
        ((ObjectNode) evidence.withArray("expected_references").get(2))
                .put("source_digest", wrongIdentity);
        String original = evidence.path("draft_body").asText();
        assertTrue(original.contains(CONTENT_IDENTITY));
        evidence.put("draft_body", original.replace(CONTENT_IDENTITY, wrongIdentity));
        assertEquals(ARTIFACT_CONTENT, evidence.path("artifacts").get(0).path("content").asText());
        assertEquals(GIT_COMMIT, evidence.path("artifacts").get(0).path("repository_commit").asText());

        OrchestratorReportAudit.AuditResult result = audit("artifact-report-ledger-correlated-wrong-64-sha", evidence);

        assertEquals("FINDINGS", result.status());
        assertEquals(Set.of("REFERENCE_SOURCE_DIGEST_MISMATCH"), codes(result));
        assertTrue(result.findings().stream().allMatch(finding -> finding.subject().equals(ARTIFACT)));
    }

    private static OrchestratorReportAudit.AuditResult audit(String label, ObjectNode evidence) {
        ObjectNode before = evidence.deepCopy();
        OrchestratorReportAudit.AuditResult result = OrchestratorReportAudit.audit(
                evidence, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION);
        System.out.println("AUDIT_INDEPENDENT_RESULT " + JSON.valueToTree(Map.of(
                "case", label, "status", result.status(), "exitStatus", result.exitStatus(),
                "findings", result.findings())));
        assertEquals(before, evidence, "Audit must preserve supplied native evidence");
        return result;
    }

    private static void assertClean(OrchestratorReportAudit.AuditResult result) {
        assertEquals("CLEAN", result.status(), result.findings().toString());
        assertEquals(0, result.exitStatus());
        assertTrue(result.findings().isEmpty());
    }

    private static void assertNoTimelineFindings(OrchestratorReportAudit.AuditResult result) {
        assertTrue(result.findings().stream().noneMatch(finding -> finding.code().startsWith("TIMELINE_")),
                result.findings().toString());
    }

    private static Set<String> codes(OrchestratorReportAudit.AuditResult result) {
        return result.findings().stream().map(OrchestratorReportAudit.Finding::code).collect(Collectors.toSet());
    }

    private static ObjectNode evidence(boolean includeProgress) {
        ObjectNode evidence = JSON.createObjectNode();
        evidence.putObject("parent_issue").put("id", PARENT).put("key", "RC10VAL-900")
                .put("created_at", "2026-10-02T01:00:00Z");
        evidence.put("orchestrator_agent_id", ORCHESTRATOR);
        ObjectNode currentRun = run(C5_RUN, PARENT, ORCHESTRATOR, "running",
                "2026-10-02T01:01:50Z", "2026-10-02T01:02:00Z", null);
        evidence.set("current_run", currentRun.deepCopy());
        evidence.putArray("children").addObject().put("id", CHILD).put("key", "RC10VAL-901")
                .put("required", true).put("created_at", "2026-10-02T01:00:40Z");
        ArrayNode runs = evidence.putArray("runs");
        runs.add(run(C3_RUN, PARENT, ORCHESTRATOR, "completed", "2026-10-02T01:00:10Z",
                "2026-10-02T01:00:20Z", "2026-10-02T01:01:40Z"));
        runs.add(run(CHILD_RUN, CHILD, WORKER, "completed", "2026-10-02T01:00:50Z",
                "2026-10-02T01:01:00Z", "2026-10-02T01:01:30Z"));
        runs.add(currentRun);

        ArrayNode comments = evidence.putArray("comments");
        ObjectNode c3 = comment(C3_COMMENT, PARENT, ORCHESTRATOR, C3_RUN, "2026-10-02T01:00:30Z");
        c3.put("subject_revision", DESIGN_REVISION).put("revision", 3).put("phase", "C3")
                .put("required", true).put("content", "## Swarm Orchestrator Plan (C3)\n"
                        + "Required domain design: " + DESIGN_REVISION + "; child fan-in follows this plan.");
        comments.add(c3);
        comments.add(comment(CHILD_COMMENT, CHILD, WORKER, CHILD_RUN, "2026-10-02T01:01:10Z")
                .put("subject_revision", GIT_COMMIT).put("content", "Synthetic child delivery result."));
        if (includeProgress) {
            comments.add(comment(PROGRESS_COMMENT, PARENT, ORCHESTRATOR, C5_RUN, "2026-10-02T01:02:10Z")
                    .put("required", false).put("content", "Progress: still collecting receipts; no decision or result."));
        }
        evidence.putArray("artifacts").addObject().put("locator", ARTIFACT).put("issue_id", CHILD)
                .put("source_task_id", CHILD_RUN).put("created_at", "2026-10-02T01:01:20Z")
                .put("subject_revision", ARTIFACT_REVISION)
                .put("content", ARTIFACT_CONTENT).put("repository_commit", GIT_COMMIT);
        ArrayNode refs = evidence.putArray("expected_references");
        refs.add(reference("comment", "RC10VAL-900", PARENT, C3_RUN, DESIGN_REVISION)
                .put("comment_id", C3_COMMENT));
        refs.add(reference("comment", "RC10VAL-901", CHILD, CHILD_RUN, GIT_COMMIT)
                .put("comment_id", CHILD_COMMENT));
        refs.add(reference("artifact", "RC10VAL-901", CHILD, CHILD_RUN, ARTIFACT_REVISION)
                .put("artifact_locator", ARTIFACT).put("source_digest", CONTENT_IDENTITY));

        evidence.put("draft_body", report(includeProgress));
        evidence.putNull("report_readback");
        return evidence;
    }

    private static ObjectNode run(String id, String issue, String author, String status,
            String dispatchedAt, String startedAt, String completedAt) {
        ObjectNode run = JSON.createObjectNode().put("id", id).put("issue_id", issue)
                .put("agent_id", author).put("status", status)
                .put("dispatched_at", dispatchedAt).put("started_at", startedAt);
        if (completedAt != null) run.put("completed_at", completedAt);
        return run;
    }

    private static ObjectNode comment(String id, String issue, String author, String run, String time) {
        return JSON.createObjectNode().put("id", id).put("issue_id", issue).put("author_id", author)
                .put("source_task_id", run).put("created_at", time);
    }

    private static ObjectNode reference(String kind, String key, String issue, String run, String revision) {
        return JSON.createObjectNode().put("kind", kind).put("issue_key", key).put("issue_id", issue)
                .put("run_id", run).put("subject_revision", revision).put("required", true);
    }

    private static String report(boolean progress) {
        List<String> timeline = List.of(
                "2026-10-02T01:00:00Z Parent issue created " + PARENT_LINK,
                "2026-10-02T01:00:10Z Run dispatched " + C3_RUN,
                "2026-10-02T01:00:20Z Run started " + C3_RUN,
                c3Event(),
                "2026-10-02T01:00:40Z Child issue created " + CHILD_LINK,
                "2026-10-02T01:00:50Z Run dispatched " + CHILD_RUN,
                "2026-10-02T01:01:00Z Run started " + CHILD_RUN,
                childCommentEvent(),
                "2026-10-02T01:01:20Z Artifact created " + ARTIFACT,
                "2026-10-02T01:01:30Z Run completed " + CHILD_RUN,
                "2026-10-02T01:01:40Z Run completed " + C3_RUN,
                "2026-10-02T01:01:50Z Run dispatched " + C5_RUN,
                "2026-10-02T01:02:00Z Run started " + C5_RUN);
        return "Mission: " + PARENT_LINK + "\nCurrent parent run: " + C5_RUN + "\n\n"
                + "## UTC typed events\n" + String.join("\n", timeline) + "\n"
                + (progress ? "2026-10-02T01:02:10Z Comment created " + PROGRESS_COMMENT + "\n" : "")
                + "\n## Required fan-in\n" + c3Row() + "\n"
                + "| " + CHILD_LINK + " | comment " + CHILD_COMMENT + " | run " + CHILD_RUN
                + " | revision `" + GIT_COMMIT + "` |\n"
                + "| " + CHILD_LINK + " | " + ARTIFACT + " | run " + CHILD_RUN
                + " | revision `" + ARTIFACT_REVISION + "` | source digest `" + CONTENT_IDENTITY + "` |\n";
    }

    private static String c3Row() {
        return "| " + PARENT_LINK + " | comment " + C3_COMMENT + " | run " + C3_RUN
                + " | revision `" + DESIGN_REVISION + "` |";
    }

    private static String c3Event() {
        return "2026-10-02T01:00:30Z Comment created " + C3_COMMENT;
    }

    private static String childCommentEvent() {
        return "2026-10-02T01:01:10Z Comment created " + CHILD_COMMENT;
    }

    private static String sha256(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}

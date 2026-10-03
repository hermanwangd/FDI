package com.featuredeliveryintelligence.fdi.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class OrchestratorReportAuditTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PARENT_ID = "00000000-0000-7000-8000-000000000001";
    private static final String CHILD_ID = "00000000-0000-7000-8000-000000000004";
    private static final String ORCHESTRATOR_ID = "00000000-0000-7000-8000-000000000006";
    private static final String PARENT_RUN_ID = "00000000-0000-7000-8000-000000000008";
    private static final String CHILD_RUN_ID = "00000000-0000-7000-8000-000000000009";
    private static final String C3_RUN_ID = "00000000-0000-7000-8000-000000000019";
    private static final String RESULT_COMMENT_ID = "00000000-0000-7000-8000-00000000000a";
    private static final String REPORT_COMMENT_ID = "00000000-0000-7000-8000-00000000000b";
    private static final String C3_COMMENT_ID = "00000000-0000-7000-8000-00000000000e";
    private static final String ARTIFACT_LOCATOR = "https://artifacts.example.test/candidate/revision-r2";
    private static final String PARENT_KEY = "RC10VAL-100";
    private static final String CHILD_KEY = "RC10VAL-101";

    @Test
    void prepublicationAcceptsActiveCurrentRunAndResolvableIssueCommentPair() {
        ObjectNode evidence = validEvidence();
        evidence.putObject("product_context").put("status", "UNKNOWN").put("critical", false);

        OrchestratorReportAudit.AuditResult result = audit(
                evidence, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION);

        assertEquals("CLEAN", result.status());
        assertEquals(0, result.exitStatus());
        assertTrue(result.findings().isEmpty());
        assertTrue(result.facts().stream().anyMatch(fact ->
                fact.code().equals("PREPUBLICATION_READBACK_NOT_REQUIRED")));
        assertTrue(result.facts().stream().anyMatch(fact ->
                fact.code().equals("PRODUCT_CONTEXT_STATE_REPORTED") && fact.subject().equals("UNKNOWN")));
        assertFalse(evidence.get("draft_body").asText().contains("| " + CHILD_ID + " |"));
    }

    @Test
    void requiredFanInNeedsItsExpectedReferenceAndExactSubjectRevision() {
        ObjectNode missing = validEvidence();
        missing.set("expected_references", MAPPER.createArrayNode());
        assertTrue(codes(audit(missing, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION))
                .contains("REQUIRED_FAN_IN_REFERENCE_MISSING"));

        ObjectNode stale = validEvidence();
        stale.put("draft_body", stale.get("draft_body").asText().replace("revision `r1`", "revision `r2`"));
        assertTrue(codes(audit(stale, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION))
                .contains("SUBJECT_REVISION_NOT_CITED"));

        ObjectNode longerPrefix = validEvidence();
        longerPrefix.put("draft_body", longerPrefix.get("draft_body").asText()
                .replace("revision `r1`", "revision `r10`"));
        assertTrue(codes(audit(longerPrefix, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION))
                .contains("SUBJECT_REVISION_NOT_CITED"));
    }

    @Test
    void sharedRevisionColumnHeaderAppliesToEveryRowAndStillRejectsLongerPrefix() {
        ObjectNode sharedHeader = validEvidence();
        String body = sharedHeader.get("draft_body").asText()
                .replace("## Required fan-in\n", "## Required fan-in\n"
                        + "| Issue | Result | Run | Subject revision | Source digest |\n"
                        + "| --- | --- | --- | --- | --- |\n")
                .replace("revision `r1` |", "revision `r1` | — |")
                .replace("revision `revision-r2` |", "revision `revision-r2` | — |")
                .replace("| revision `", "| `");
        sharedHeader.put("draft_body", body);
        OrchestratorReportAudit.AuditResult sharedHeaderResult = audit(sharedHeader,
                OrchestratorReportAudit.PublicationPhase.PREPUBLICATION);
        assertEquals("CLEAN", sharedHeaderResult.status(), sharedHeaderResult.findings().toString());

        ObjectNode longerPrefix = validEvidence();
        longerPrefix.put("draft_body", body.replace("| `r1` |", "| `r10` |"));
        assertTrue(codes(audit(longerPrefix, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION))
                .contains("SUBJECT_REVISION_NOT_CITED"));

        ObjectNode longerArtifactPrefix = validEvidence();
        longerArtifactPrefix.put("draft_body", body.replace("| `revision-r2` |", "| `revision-r20` |"));
        assertTrue(codes(audit(longerArtifactPrefix, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION))
                .contains("SUBJECT_REVISION_NOT_CITED"));
    }

    @Test
    void expectedCommentMustResolveToTheSameIssueRunAndAuthor() {
        ObjectNode wrongSource = validEvidence();
        ((ObjectNode) wrongSource.withArray("comments").get(1)).put("source_task_id", PARENT_RUN_ID);
        assertTrue(codes(audit(wrongSource, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION))
                .contains("COMMENT_RUN_MISMATCH"));

        ObjectNode wrongAuthor = validEvidence();
        ((ObjectNode) wrongAuthor.withArray("comments").get(1)).put("author_id", ORCHESTRATOR_ID);
        assertTrue(codes(audit(wrongAuthor, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION))
                .contains("COMMENT_AUTHOR_MISMATCH"));

        ObjectNode wrongRunIssue = validEvidence();
        ((ObjectNode) wrongRunIssue.withArray("runs").get(1)).put("issue_id", PARENT_ID);
        assertTrue(codes(audit(wrongRunIssue, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION))
                .contains("RUN_ISSUE_MISMATCH"));
    }

    @Test
    void artifactReceiptMustResolveToItsExactIssueRunAndSubjectRevision() {
        ObjectNode wrongRevision = validEvidence();
        ((ObjectNode) wrongRevision.withArray("artifacts").get(0)).put("subject_revision", "revision-r3");
        assertTrue(codes(audit(wrongRevision, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION))
                .contains("ARTIFACT_SUBJECT_REVISION_MISMATCH"));

        ObjectNode missingReceipt = validEvidence();
        missingReceipt.set("artifacts", MAPPER.createArrayNode());
        assertTrue(codes(audit(missingReceipt, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION))
                .contains("ARTIFACT_RECEIPT_MISSING"));

        ObjectNode longerArtifactRevision = validEvidence();
        longerArtifactRevision.put("draft_body", longerArtifactRevision.get("draft_body").asText()
                .replace("revision `revision-r2`", "revision `revision-r20`"));
        assertTrue(codes(audit(longerArtifactRevision, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION))
                .contains("SUBJECT_REVISION_NOT_CITED"));
    }

    @Test
    void correlatedDraftAndExpectedLedgerErrorsCannotHideNativeC3AndArtifactSource() {
        ObjectNode evidence = validEvidence();
        ArrayNode references = evidence.withArray("expected_references");
        references.remove(0);
        String wrongDigest = "sha256:" + "a".repeat(64);
        ((ObjectNode) references.get(1)).put("subject_revision", "UNKNOWN");
        ((ObjectNode) references.get(1)).put("source_digest", wrongDigest);

        ObjectNode nativeArtifact = (ObjectNode) evidence.withArray("artifacts").get(0);
        nativeArtifact.remove("subject_revision");
        nativeArtifact.remove("revision");
        nativeArtifact.put("content", "pinned native artifact bytes");

        String c3Row = "| [" + PARENT_KEY + "](mention://issue/" + PARENT_ID + ") | comment "
                + C3_COMMENT_ID + " | run " + C3_RUN_ID + " | revision `UNKNOWN` | source digest `sha256:5833b144b42eec5c44515656ad25c4bf1343ca9d03d7f93db840eb91ed4ca2a6` |";
        String body = evidence.get("draft_body").asText();
        assertTrue(body.contains(c3Row));
        assertTrue(body.contains("revision `revision-r2`"));
        body = body.replace(c3Row, "| [" + PARENT_KEY + "](mention://issue/" + PARENT_ID
                + ") | C3 locator omitted | run " + C3_RUN_ID + " | revision `UNKNOWN` | source digest `sha256:5833b144b42eec5c44515656ad25c4bf1343ca9d03d7f93db840eb91ed4ca2a6` |")
                .replace("revision `revision-r2`", "revision `UNKNOWN` | source digest `" + wrongDigest + "`");
        evidence.put("draft_body", body);

        Set<String> findings = codes(audit(evidence, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION));

        assertTrue(findings.contains("PARENT_C3_PLAN_REFERENCE_MISSING"), findings.toString());
        assertTrue(findings.contains("REFERENCE_SOURCE_DIGEST_MISMATCH"), findings.toString());
    }

    @Test
    void earlierC3PlanIsRequiredButOrdinaryProgressCommentIsNot() {
        ObjectNode evidence = validEvidence();
        ObjectNode progress = evidence.withArray("comments").addObject();
        progress.put("id", "00000000-0000-7000-8000-00000000001e");
        progress.put("issue_id", PARENT_ID);
        progress.put("author_id", ORCHESTRATOR_ID);
        progress.put("source_task_id", PARENT_RUN_ID);
        progress.put("content", "Progress update: not a design, plan, result, or acceptance claim.");
        progress.put("created_at", "2026-10-02T09:02:30Z");
        String body = evidence.get("draft_body").asText().replace(
                "2026-10-02T09:03:00Z Comment created " + RESULT_COMMENT_ID,
                "2026-10-02T09:02:30Z Comment created 00000000-0000-7000-8000-00000000001e\n"
                        + "2026-10-02T09:03:00Z Comment created " + RESULT_COMMENT_ID);
        evidence.put("draft_body", body);
        assertEquals("CLEAN", audit(evidence,
                OrchestratorReportAudit.PublicationPhase.PREPUBLICATION).status());

        ObjectNode missingEarlierPlan = validEvidence();
        missingEarlierPlan.withArray("expected_references").remove(0);
        String c3Row = "| [" + PARENT_KEY + "](mention://issue/" + PARENT_ID + ") | comment "
                + C3_COMMENT_ID + " | run " + C3_RUN_ID + " | revision `UNKNOWN` | source digest `sha256:5833b144b42eec5c44515656ad25c4bf1343ca9d03d7f93db840eb91ed4ca2a6` |";
        missingEarlierPlan.put("draft_body", missingEarlierPlan.get("draft_body").asText()
                .replace(c3Row, "| [" + PARENT_KEY + "](mention://issue/" + PARENT_ID
                        + ") | C3 omitted | run " + C3_RUN_ID + " | revision `UNKNOWN` | source digest `sha256:5833b144b42eec5c44515656ad25c4bf1343ca9d03d7f93db840eb91ed4ca2a6` |"));
        assertTrue(codes(audit(missingEarlierPlan,
                OrchestratorReportAudit.PublicationPhase.PREPUBLICATION))
                .contains("PARENT_C3_PLAN_REFERENCE_MISSING"));
    }

    @Test
    void artifactContentDigestMustMatchAndUnknownSourceMustStayExplicit() {
        ObjectNode contentIdentity = validEvidence();
        ObjectNode nativeArtifact = (ObjectNode) contentIdentity.withArray("artifacts").get(0);
        nativeArtifact.remove("subject_revision");
        nativeArtifact.remove("revision");
        nativeArtifact.put("content", "pinned native artifact bytes");
        String digest = "sha256:a07a7cfa6586871f0087f9efc59546b4b52f1bef7dc7ce8962804fe02e0bb411";
        ((ObjectNode) contentIdentity.withArray("expected_references").get(2))
                .put("subject_revision", "UNKNOWN");
        ((ObjectNode) contentIdentity.withArray("expected_references").get(2)).put("source_digest", digest);
        contentIdentity.put("draft_body", contentIdentity.get("draft_body").asText()
                .replace("revision `revision-r2`", "revision `UNKNOWN` | source digest `" + digest + "`"));
        assertEquals("CLEAN", audit(contentIdentity,
                OrchestratorReportAudit.PublicationPhase.PREPUBLICATION).status());

        ObjectNode unavailableSource = validEvidence();
        ObjectNode unversionedArtifact = (ObjectNode) unavailableSource.withArray("artifacts").get(0);
        unversionedArtifact.remove("subject_revision");
        unversionedArtifact.remove("revision");
        assertTrue(codes(audit(unavailableSource, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION))
                .contains("SUBJECT_REVISION_UNVERIFIED"));

        ((ObjectNode) unavailableSource.withArray("expected_references").get(2))
                .put("subject_revision", "UNKNOWN");
        unavailableSource.put("draft_body", unavailableSource.get("draft_body").asText()
                .replace("revision `revision-r2`", "revision `UNKNOWN`"));
        assertEquals("CLEAN", audit(unavailableSource,
                OrchestratorReportAudit.PublicationPhase.PREPUBLICATION).status());
    }

    @Test
    void requiredChildRunMustBeCompletedWhileParentC3MayStillBeRunning() {
        for (String status : new String[] {"running", "failed"}) {
            ObjectNode evidence = validEvidence();
            ((ObjectNode) evidence.withArray("runs").get(1)).put("status", status);
            assertTrue(codes(audit(evidence, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION))
                    .contains("REQUIRED_CHILD_RUN_NOT_COMPLETED"), status);
        }

        ObjectNode missingStatus = validEvidence();
        ((ObjectNode) missingStatus.withArray("runs").get(1)).remove("status");
        assertTrue(codes(audit(missingStatus, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION))
                .contains("REQUIRED_CHILD_RUN_STATUS_UNVERIFIED"));

        ObjectNode missingRun = validEvidence();
        missingRun.withArray("runs").remove(1);
        assertTrue(codes(audit(missingRun, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION))
                .contains("RUN_RECEIPT_MISSING"));
        assertEquals("CLEAN", audit(validEvidence(),
                OrchestratorReportAudit.PublicationPhase.PREPUBLICATION).status());
    }

    @Test
    void postpublicationComparesFullBodyAndAttributionWithoutWaitingForRunCompletion() {
        ObjectNode evidence = validEvidence();
        evidence.set("report_readback", reportReadback(evidence.get("draft_body").asText()));
        assertEquals("CLEAN", audit(evidence,
                OrchestratorReportAudit.PublicationPhase.POSTPUBLICATION).status());

        ObjectNode changedBody = validEvidence();
        changedBody.set("report_readback", reportReadback(
                changedBody.get("draft_body").asText() + " "));
        assertTrue(codes(audit(changedBody, OrchestratorReportAudit.PublicationPhase.POSTPUBLICATION))
                .contains("REPORT_BODY_READBACK_MISMATCH"));

        ObjectNode wrongDigest = validEvidence();
        ObjectNode digestReadback = reportReadback(wrongDigest.get("draft_body").asText());
        digestReadback.put("body_sha256", "0".repeat(64));
        wrongDigest.set("report_readback", digestReadback);
        assertTrue(codes(audit(wrongDigest, OrchestratorReportAudit.PublicationPhase.POSTPUBLICATION))
                .contains("REPORT_BODY_DIGEST_MISMATCH"));

        ObjectNode wrongPublisher = validEvidence();
        ObjectNode readback = reportReadback(wrongPublisher.get("draft_body").asText());
        readback.put("author_id", CHILD_RUN_ID);
        wrongPublisher.set("report_readback", readback);
        assertTrue(codes(audit(wrongPublisher, OrchestratorReportAudit.PublicationPhase.POSTPUBLICATION))
                .contains("REPORT_ATTRIBUTION_MISMATCH"));
    }

    @Test
    void chronologyRequiresTypedUtcEventsInTimestampOrder() {
        ObjectNode untyped = validEvidence();
        untyped.put("draft_body", untyped.get("draft_body").asText()
                .replace("2026-10-02T09:00:00Z Parent issue created", "2026-10-02T09:00:00Z"));
        assertTrue(codes(audit(untyped, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION))
                .contains("TIMELINE_EVENT_UNTYPED_OR_MISSING"));

        ObjectNode reversed = validEvidence();
        String body = reversed.get("draft_body").asText();
        String first = "2026-10-02T09:00:00Z Parent issue created [RC10VAL-100](mention://issue/"
                + PARENT_ID + ")";
        String second = "2026-10-02T09:00:30Z Run dispatched " + PARENT_RUN_ID;
        reversed.put("draft_body", body.replace(first, "TEMP").replace(second, first).replace("TEMP", second));
        assertTrue(codes(audit(reversed, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION))
                .contains("TIMELINE_EVENTS_OUT_OF_ORDER"));

        ObjectNode wrongSourceTime = validEvidence();
        ((ObjectNode) wrongSourceTime.withArray("runs").get(1)).put("started_at", "2026-10-02T09:05:00Z");
        assertTrue(codes(audit(wrongSourceTime, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION))
                .contains("RUN_TIMESTAMP_ORDER_MISMATCH"));
    }

    private static OrchestratorReportAudit.AuditResult audit(
            JsonNode evidence, OrchestratorReportAudit.PublicationPhase phase) {
        return OrchestratorReportAudit.audit(evidence, phase);
    }

    private static Set<String> codes(OrchestratorReportAudit.AuditResult result) {
        return result.findings().stream().map(OrchestratorReportAudit.Finding::code).collect(Collectors.toSet());
    }

    private static ObjectNode validEvidence() {
        ObjectNode evidence = MAPPER.createObjectNode();
        ObjectNode parent = evidence.putObject("parent_issue");
        parent.put("id", PARENT_ID);
        parent.put("key", PARENT_KEY);
        parent.put("created_at", "2026-10-02T09:00:00Z");
        evidence.put("orchestrator_agent_id", ORCHESTRATOR_ID);
        evidence.set("current_run", run(PARENT_RUN_ID, PARENT_ID, ORCHESTRATOR_ID,
                "running", "2026-10-02T09:00:30Z", "2026-10-02T09:01:00Z", null));

        ArrayNode children = evidence.putArray("children");
        ObjectNode child = children.addObject();
        child.put("id", CHILD_ID);
        child.put("key", CHILD_KEY);
        child.put("required", true);
        child.put("created_at", "2026-10-02T09:01:15Z");

        ArrayNode runs = evidence.putArray("runs");
        runs.add(run(PARENT_RUN_ID, PARENT_ID, ORCHESTRATOR_ID, "running",
                "2026-10-02T09:00:30Z", "2026-10-02T09:01:00Z", null));
        runs.add(run(CHILD_RUN_ID, CHILD_ID, "00000000-0000-7000-8000-00000000000d",
                "completed", "2026-10-02T09:01:30Z", "2026-10-02T09:02:00Z", "2026-10-02T09:04:00Z"));
        runs.add(run(C3_RUN_ID, PARENT_ID, ORCHESTRATOR_ID,
                "completed", null, null, null));

        ArrayNode comments = evidence.putArray("comments");
        ObjectNode plan = comments.addObject();
        plan.put("id", C3_COMMENT_ID);
        plan.put("issue_id", PARENT_ID);
        plan.put("author_id", ORCHESTRATOR_ID);
        plan.put("source_task_id", C3_RUN_ID);
        plan.put("content", "## C3 Implementation Plan\nOrchestrator-owned plan.");
        plan.put("created_at", "2026-10-02T09:00:45Z");
        ObjectNode result = comments.addObject();
        result.put("id", RESULT_COMMENT_ID);
        result.put("issue_id", CHILD_ID);
        result.put("author_id", "00000000-0000-7000-8000-00000000000d");
        result.put("source_task_id", CHILD_RUN_ID);
        result.put("subject_revision", "r1");
        result.put("created_at", "2026-10-02T09:03:00Z");

        ArrayNode artifacts = evidence.putArray("artifacts");
        ObjectNode artifact = artifacts.addObject();
        artifact.put("id", "artifact-r2");
        artifact.put("locator", ARTIFACT_LOCATOR);
        artifact.put("issue_id", CHILD_ID);
        artifact.put("source_task_id", CHILD_RUN_ID);
        artifact.put("subject_revision", "revision-r2");
        artifact.put("created_at", "2026-10-02T09:03:30Z");

        ArrayNode references = evidence.putArray("expected_references");
        ObjectNode ref = references.addObject();
        ref.put("kind", "comment");
        ref.put("issue_key", PARENT_KEY);
        ref.put("issue_id", PARENT_ID);
        ref.put("comment_id", C3_COMMENT_ID);
        ref.put("run_id", C3_RUN_ID);
        ref.put("subject_revision", "UNKNOWN");
        ref.put("source_digest", "sha256:5833b144b42eec5c44515656ad25c4bf1343ca9d03d7f93db840eb91ed4ca2a6");
        ref.put("required", true);
        ObjectNode childRef = references.addObject();
        childRef.put("kind", "comment");
        childRef.put("issue_key", CHILD_KEY);
        childRef.put("issue_id", CHILD_ID);
        childRef.put("comment_id", RESULT_COMMENT_ID);
        childRef.put("run_id", CHILD_RUN_ID);
        childRef.put("subject_revision", "r1");
        childRef.put("required", true);
        ObjectNode artifactRef = references.addObject();
        artifactRef.put("kind", "artifact");
        artifactRef.put("issue_key", CHILD_KEY);
        artifactRef.put("issue_id", CHILD_ID);
        artifactRef.put("artifact_locator", ARTIFACT_LOCATOR);
        artifactRef.put("run_id", CHILD_RUN_ID);
        artifactRef.put("subject_revision", "revision-r2");
        artifactRef.put("required", true);

        evidence.put("draft_body", reportBody());
        evidence.set("report_readback", NullNode.getInstance());
        return evidence;
    }

    private static ObjectNode run(
            String id, String issueId, String agentId, String status,
            String dispatchedAt, String startedAt, String completedAt) {
        ObjectNode run = MAPPER.createObjectNode();
        run.put("id", id);
        run.put("issue_id", issueId);
        run.put("agent_id", agentId);
        run.put("status", status);
        if (dispatchedAt != null) run.put("dispatched_at", dispatchedAt);
        run.put("started_at", startedAt);
        if (completedAt != null) run.put("completed_at", completedAt);
        return run;
    }

    private static ObjectNode reportReadback(String body) {
        ObjectNode readback = MAPPER.createObjectNode();
        readback.put("comment_id", REPORT_COMMENT_ID);
        readback.put("issue_id", PARENT_ID);
        readback.put("author_id", ORCHESTRATOR_ID);
        readback.put("source_task_id", PARENT_RUN_ID);
        readback.put("body", body);
        readback.put("created_at", "2026-10-02T09:05:00Z");
        return readback;
    }

    private static String reportBody() {
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
                | [RC10VAL-100](mention://issue/00000000-0000-7000-8000-000000000001) | comment 00000000-0000-7000-8000-00000000000e | run 00000000-0000-7000-8000-000000000019 | revision `UNKNOWN` | source digest `sha256:5833b144b42eec5c44515656ad25c4bf1343ca9d03d7f93db840eb91ed4ca2a6` |
                | [RC10VAL-101](mention://issue/00000000-0000-7000-8000-000000000004) | comment 00000000-0000-7000-8000-00000000000a | run 00000000-0000-7000-8000-000000000009 | revision `r1` |
                | [RC10VAL-101](mention://issue/00000000-0000-7000-8000-000000000004) | https://artifacts.example.test/candidate/revision-r2 | run 00000000-0000-7000-8000-000000000009 | revision `revision-r2` |
                """;
    }
}

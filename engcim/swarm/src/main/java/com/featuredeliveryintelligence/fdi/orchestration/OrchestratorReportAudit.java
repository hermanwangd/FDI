package com.featuredeliveryintelligence.fdi.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Read-only checks for an Orchestrator parent-report draft and its receipts.
 *
 * <p>The input is a JSON object with {@code parent_issue}, {@code current_run},
 * {@code children}, {@code expected_references}, {@code runs}, {@code comments},
 * {@code artifacts}, and {@code draft_body}. Chronology is derived from those
 * receipts. The postpublication
 * phase also consumes {@code report_readback}. Expected references may carry
 * native {@code subject_revision} and separate {@code source_digest} values.
 * IDs and revisions are compared to exact fresh receipts; this class does not
 * invent a second ID format or infer execution from a successful dispatch request.</p>
 *
 * <p>{@code CLEAN}/{@code FINDINGS} describe this local report check only. The
 * result is not a Control verdict, Verification PASS, Mission status, or write.</p>
 */
public final class OrchestratorReportAudit {
    private static final String ISSUE_URI_PREFIX = "mention://issue/";
    private static final Pattern UTC_TIMESTAMP = Pattern.compile(
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})");

    private OrchestratorReportAudit() {}

    public enum PublicationPhase {
        PREPUBLICATION,
        POSTPUBLICATION
    }

    public record Fact(String code, String subject, String detail) {}

    public record Finding(String code, String subject, String reason) {}

    public record AuditResult(String status, List<Fact> facts, List<Finding> findings, int exitStatus) {
        public AuditResult {
            facts = List.copyOf(facts);
            findings = List.copyOf(findings);
        }
    }

    public static AuditResult audit(JsonNode evidence, PublicationPhase phase) {
        if (evidence == null || !evidence.isObject()) {
            throw new IllegalArgumentException("report evidence must be a JSON object");
        }
        if (phase == null) {
            throw new IllegalArgumentException("publication phase is required");
        }

        List<Fact> facts = new ArrayList<>();
        List<Finding> findings = new ArrayList<>();
        rejectConflictingReceipts(evidence, findings);
        if (!findings.isEmpty()) {
            return new AuditResult("FINDINGS", facts, findings, 1);
        }
        JsonNode parent = evidence.path("parent_issue");
        String parentId = text(parent, "id");
        String parentKey = text(parent, "key");
        String orchestratorId = text(evidence, "orchestrator_agent_id");
        JsonNode currentRun = evidence.path("current_run");
        String currentRunId = text(currentRun, "id");
        String draftBody = rawText(evidence, "draft_body");

        if (parentId.isEmpty() || parentKey.isEmpty()) {
            finding(findings, "PARENT_IDENTITY_UNVERIFIED", parentKey,
                    "Fresh parent issue key and full issue ID are both required.");
        } else {
            facts.add(new Fact("PARENT_IDENTITY_RESOLVED", parentKey, "Parent issue key and ID are present."));
            if (!draftBody.contains(issueLink(parentKey, parentId))) {
                finding(findings, "PARENT_LOCATOR_MISSING", parentKey,
                        "Draft does not contain the clickable parent issue link resolved from the fresh ledger.");
            }
        }
        if (draftBody.isBlank()) {
            finding(findings, "DRAFT_BODY_MISSING", parentKey, "No unposted report body was supplied.");
        }

        if (currentRunId.isEmpty()) {
            finding(findings, "CURRENT_RUN_IDENTITY_UNVERIFIED", parentKey,
                    "Runtime context does not expose the current parent run ID.");
        } else if (!Objects.equals(parentId, text(currentRun, "issue_id"))
                || orchestratorId.isEmpty()
                || !Objects.equals(orchestratorId, text(currentRun, "agent_id"))) {
            finding(findings, "CURRENT_RUN_ATTRIBUTION_MISMATCH", currentRunId,
                    "Current run context does not match the parent issue and executing Orchestrator identity.");
        } else {
            facts.add(new Fact("CURRENT_PARENT_RUN_BOUND", currentRunId,
                    "Runtime context ties the full current run ID to this parent and Orchestrator."));
            if (!draftBody.contains(currentRunId)) {
                finding(findings, "CURRENT_RUN_REFERENCE_MISSING", currentRunId,
                        "Draft omits the full current parent run ID.");
            }
        }
        if (orchestratorId.isEmpty()) {
            finding(findings, "ORCHESTRATOR_IDENTITY_UNVERIFIED", parentKey,
                    "The executing Orchestrator identity is not present in the evidence input.");
        }
        JsonNode productContext = evidence.path("product_context");
        if (productContext.isObject()) {
            String contextState = firstText(productContext, "status", "consumption");
            if (!contextState.isEmpty()) {
                facts.add(new Fact("PRODUCT_CONTEXT_STATE_REPORTED", contextState,
                        "Supplied state is preserved; this helper does not infer consumption or promote noncritical context gaps to a Mission block."));
            }
        }

        Map<String, JsonNode> issues = indexIssues(parent, evidence.path("children"));
        Map<String, JsonNode> runs = indexById(evidence.path("runs"));
        if (!currentRunId.isEmpty()) runs.putIfAbsent(currentRunId, currentRun);
        Map<String, JsonNode> comments = indexById(evidence.path("comments"));
        Map<String, JsonNode> artifacts = indexArtifacts(evidence.path("artifacts"));
        JsonNode references = evidence.path("expected_references");

        checkRequiredFanIn(evidence.path("children"), references, findings);
        checkParentC3PlanComments(parentId, parentKey, orchestratorId,
                text(evidence.path("report_readback"), "comment_id"), evidence.path("comments"),
                references, draftBody, findings);
        checkReferences(references, parentId, issues, runs, comments, artifacts, draftBody, facts, findings);
        checkParentRunReferences(parentId, runs, draftBody, findings);
        checkRunTimestampOrder(runs.values(), findings);

        JsonNode readback = evidence.path("report_readback");
        if (phase == PublicationPhase.PREPUBLICATION) {
            if (readback.isObject()) {
                finding(findings, "REPORT_ALREADY_PUBLISHED", parentKey,
                        "A report readback exists before the prepublication check; resolve it before another publish attempt.");
            } else {
                facts.add(new Fact("PREPUBLICATION_READBACK_NOT_REQUIRED", currentRunId,
                        "No C5 comment is required before publishing; the current run may still be active."));
            }
        } else {
            checkPostpublicationReadback(readback, parentId, parentKey, orchestratorId,
                    currentRunId, runs, draftBody, facts, findings);
        }
        checkTimeline(evidence, parent, readback, phase, runs, comments, draftBody, facts, findings);

        if (evidence.path("work_package_amendment_selected").isBoolean()
                && evidence.path("work_package_amendment_selected").asBoolean()) {
            JsonNode ledger = evidence.get("work_packages");
            if (ledger == null || ledger.isNull()) {
                findings.add(new Finding("WORK_PACKAGE_LEDGER_MISSING", "work_packages",
                        "The selected current-work-package amendment requires its exact operation/plan/review ledger."));
            } else {
                AuditResult packageAudit = CurrentWorkPackageAudit.audit(ledger);
                facts.addAll(packageAudit.facts());
                findings.addAll(packageAudit.findings());
            }
        }

        String status = findings.isEmpty() ? "CLEAN" : "FINDINGS";
        return new AuditResult(status, facts, findings, findings.isEmpty() ? 0 : 1);
    }

    private static void checkRequiredFanIn(
            JsonNode children, JsonNode references, List<Finding> findings) {
        for (JsonNode child : arrayValues(children)) {
            String childId = text(child, "id");
            String childKey = text(child, "key");
            if (childId.isEmpty()) {
                finding(findings, "CHILD_IDENTITY_UNVERIFIED", childKey,
                        "A fresh child row is missing its full issue ID.");
                continue;
            }
            if (!child.has("required") || !child.get("required").isBoolean()) {
                finding(findings, "CHILD_REQUIREDNESS_UNVERIFIED", childKey,
                        "Requiredness must come from the current Mission ledger, not be inferred here.");
                continue;
            }
            if (!child.path("required").asBoolean()) continue;

            boolean hasRequiredReference = arrayValues(references).stream().anyMatch(reference ->
                    childId.equals(text(reference, "issue_id"))
                            && reference.path("required").asBoolean(false));
            if (!hasRequiredReference) {
                finding(findings, "REQUIRED_FAN_IN_REFERENCE_MISSING", childKey,
                        "The fresh ledger has no required result reference for this required child.");
            }
        }
    }

    private static void checkReferences(
            JsonNode references,
            String parentId,
            Map<String, JsonNode> issues,
            Map<String, JsonNode> runs,
            Map<String, JsonNode> comments,
            Map<String, JsonNode> artifacts,
            String body,
            List<Fact> facts,
            List<Finding> findings) {
        for (JsonNode reference : arrayValues(references)) {
            int findingCountBefore = findings.size();
            String issueId = text(reference, "issue_id");
            String issueKey = text(reference, "issue_key");
            String subject = issueKey.isEmpty() ? issueId : issueKey;
            String commentId = text(reference, "comment_id");
            String artifactLocator = text(reference, "artifact_locator");
            String runId = text(reference, "run_id");
            String revision = text(reference, "subject_revision");
            String sourceDigest = text(reference, "source_digest");
            boolean required = reference.path("required").asBoolean(false);

            if (!reference.has("required") || !reference.get("required").isBoolean()) {
                finding(findings, "REFERENCE_REQUIREDNESS_UNVERIFIED", subject,
                        "Reference criticality is absent; this audit will not infer a Mission-wide block.");
            }
            JsonNode issue = issues.get(issueId);
            if (issue == null || !Objects.equals(issueKey, text(issue, "key"))) {
                finding(findings, "ISSUE_REFERENCE_UNRESOLVED", subject,
                        "Issue key and ID do not resolve to the fresh parent or child issue list.");
            }

            if (commentId.isEmpty() && artifactLocator.isEmpty()) {
                finding(findings, "EXPECTED_REFERENCE_LOCATOR_MISSING", subject,
                        "Expected reference has neither a complete comment ID nor an artifact locator.");
            }
            if (required && runId.isEmpty()) {
                finding(findings, "EXPECTED_RUN_UNVERIFIED", subject,
                        "Required reference has no full associated run ID in the fresh ledger.");
            }
            JsonNode run = runs.get(runId);
            if (!runId.isEmpty() && run == null) {
                finding(findings, "RUN_RECEIPT_MISSING", runId,
                        "Expected run ID does not resolve to a run receipt; request acceptance alone is not a run receipt.");
            } else if (run != null && !Objects.equals(issueId, text(run, "issue_id"))) {
                finding(findings, "RUN_ISSUE_MISMATCH", runId,
                        "Associated run belongs to a different issue than this expected reference.");
            }
            if (run != null && !issueId.equals(parentId) && issue != null
                    && issue.path("required").asBoolean(false)) {
                String runStatus = text(run, "status");
                if (runStatus.isEmpty()) {
                    finding(findings, "REQUIRED_CHILD_RUN_STATUS_UNVERIFIED", runId,
                            "Required child result has no run status receipt.");
                } else if (!"completed".equals(runStatus.toLowerCase(Locale.ROOT))) {
                    finding(findings, "REQUIRED_CHILD_RUN_NOT_COMPLETED", runId,
                            "Required child result is not delivered by a completed run; observed status is " + runStatus + ".");
                }
            }

            if (!commentId.isEmpty()) {
                JsonNode comment = comments.get(commentId);
                if (comment == null) {
                    finding(findings, "COMMENT_RECEIPT_MISSING", commentId,
                            "Expected full comment ID does not resolve to a fresh comment receipt.");
                } else {
                    if (!Objects.equals(issueId, text(comment, "issue_id"))) {
                        finding(findings, "COMMENT_ISSUE_MISMATCH", commentId,
                                "Result comment belongs to a different issue than the ledger reference.");
                    }
                    if (!runId.isEmpty() && !Objects.equals(runId, text(comment, "source_task_id"))) {
                        finding(findings, "COMMENT_RUN_MISMATCH", commentId,
                                "Comment source_task_id does not resolve to its expected run.");
                    }
                    if (run != null && !Objects.equals(text(run, "agent_id"), text(comment, "author_id"))) {
                        finding(findings, "COMMENT_AUTHOR_MISMATCH", commentId,
                                "Comment author does not match the associated run agent.");
                    }
                    String actualSubjectRevision = text(comment, "subject_revision");
                    if (!revision.isEmpty() && !actualSubjectRevision.isEmpty()
                            && !revision.equals(actualSubjectRevision)) {
                        finding(findings, "COMMENT_SUBJECT_REVISION_MISMATCH", commentId,
                                "Comment receipt subject revision differs from the expected ledger revision.");
                    } else if (required && !revision.isEmpty() && actualSubjectRevision.isEmpty()
                            && !"UNKNOWN".equalsIgnoreCase(revision)) {
                        finding(findings, "SUBJECT_REVISION_UNVERIFIED", commentId,
                                "Required comment reference claims a revision that the native comment receipt does not establish; use its exact revision or UNKNOWN.");
                    }
                    checkSourceDigest(sourceDigest, sourceContentDigest(comment), commentId, findings);
                }
            }

            if (!artifactLocator.isEmpty()) {
                JsonNode artifact = artifacts.get(artifactLocator);
                if (artifact == null) {
                    finding(findings, "ARTIFACT_RECEIPT_MISSING", artifactLocator,
                            "Expected artifact locator does not resolve to a fresh artifact receipt.");
                } else {
                    if (!Objects.equals(issueId, text(artifact, "issue_id"))) {
                        finding(findings, "ARTIFACT_ISSUE_MISMATCH", artifactLocator,
                                "Artifact receipt belongs to a different issue than the ledger reference.");
                    }
                    String artifactRunId = text(artifact, "source_task_id");
                    if (!runId.isEmpty() && artifactRunId.isEmpty()) {
                        finding(findings, "ARTIFACT_RUN_UNVERIFIED", artifactLocator,
                                "Ledger claims a run but the artifact receipt does not establish source_task_id.");
                    } else if (!runId.isEmpty() && !runId.equals(artifactRunId)) {
                        finding(findings, "ARTIFACT_RUN_MISMATCH", artifactLocator,
                                "Artifact receipt is associated with a different run than the ledger reference.");
                    }
                    String actualRevision = firstText(artifact, "subject_revision", "revision");
                    if (!revision.isEmpty() && !actualRevision.isEmpty() && !revision.equals(actualRevision)) {
                        finding(findings, "ARTIFACT_SUBJECT_REVISION_MISMATCH", artifactLocator,
                                "Artifact receipt subject revision differs from the expected ledger revision.");
                    } else if (required && !revision.isEmpty() && actualRevision.isEmpty()
                            && !"UNKNOWN".equalsIgnoreCase(revision)) {
                        finding(findings, "SUBJECT_REVISION_UNVERIFIED", artifactLocator,
                                "Required artifact reference claims a revision that the native receipt does not establish; use its exact revision or UNKNOWN, and record content digest separately.");
                    }
                    checkSourceDigest(sourceDigest, sourceContentDigest(artifact), artifactLocator, findings);
                }
            }

            if (issueId.isEmpty() || issueKey.isEmpty()) {
                finding(findings, "EXPECTED_ISSUE_IDENTITY_INCOMPLETE", subject,
                        "Expected reference needs both the issue key and full issue ID from the ledger.");
            }
            if (revision.isEmpty() && required) {
                finding(findings, "SUBJECT_REVISION_UNVERIFIED", subject,
                        "Required reference has no exact subject revision in the fresh ledger.");
            }
            List<String> rowTokens = new ArrayList<>();
            if (!issueKey.isEmpty() && !issueId.isEmpty()) rowTokens.add(issueLink(issueKey, issueId));
            if (!commentId.isEmpty()) rowTokens.add(commentId);
            if (!artifactLocator.isEmpty()) rowTokens.add(artifactLocator);
            if (!runId.isEmpty()) rowTokens.add(runId);
            if (rowTokens.isEmpty() || !hasSingleLineWithAllTokens(body, rowTokens)) {
                finding(findings, "EXPECTED_REFERENCE_LOCATOR_MISSING", subject,
                        "Draft does not put the resolvable issue locator, exact result/artifact locator, and run ID together in one row.");
            } else if (!revision.isEmpty() && !hasReferenceRowWithRevision(body, rowTokens, revision)) {
                finding(findings, "SUBJECT_REVISION_NOT_CITED", subject,
                        "Draft does not cite the exact ledger subject revision beside its result locator.");
            }
            if (!sourceDigest.isEmpty() && !hasSourceDigestInReferenceRow(body, rowTokens, sourceDigest)) {
                finding(findings, "SOURCE_DIGEST_NOT_CITED", subject,
                        "Draft does not cite the exact native source digest beside its result locator.");
            }
            if (findings.size() == findingCountBefore) {
                facts.add(new Fact("EXPECTED_REFERENCE_RESOLVED", subject,
                        "Ledger locator, receipt attribution, run, subject revision, and any source digest are consistent."));
            }
        }
    }

    private static void checkPostpublicationReadback(
            JsonNode readback,
            String parentId,
            String parentKey,
            String orchestratorId,
            String currentRunId,
            Map<String, JsonNode> runs,
            String draftBody,
            List<Fact> facts,
            List<Finding> findings) {
        if (!readback.isObject()) {
            finding(findings, "REPORT_READBACK_MISSING", parentKey,
                    "Postpublication check requires the returned parent comment readback.");
            return;
        }

        String reportCommentId = text(readback, "comment_id");
        String sourceTaskId = text(readback, "source_task_id");
        JsonNode sourceRun = runs.get(sourceTaskId);
        if (reportCommentId.isEmpty()
                || !Objects.equals(parentId, text(readback, "issue_id"))
                || !Objects.equals(orchestratorId, text(readback, "author_id"))
                || !Objects.equals(currentRunId, sourceTaskId)
                || sourceRun == null
                || !Objects.equals(parentId, text(sourceRun, "issue_id"))
                || !Objects.equals(orchestratorId, text(sourceRun, "agent_id"))) {
            finding(findings, "REPORT_ATTRIBUTION_MISMATCH", reportCommentId,
                    "Returned comment, parent issue, executing Orchestrator, and current parent run do not resolve to the same publication.");
        } else {
            facts.add(new Fact("REPORT_READBACK_ATTRIBUTION_VERIFIED", reportCommentId,
                    "Returned comment resolves to the parent, executing Orchestrator, and prepublication current run."));
        }

        String readbackBody = rawText(readback, "body");
        if (!readback.has("body") || !readback.get("body").isTextual()) {
            finding(findings, "REPORT_BODY_READBACK_MISSING", reportCommentId,
                    "Returned parent comment has no full body readback.");
        } else if (!draftBody.equals(readbackBody)) {
            finding(findings, "REPORT_BODY_READBACK_MISMATCH", reportCommentId,
                    "Returned parent comment body differs from the complete unposted draft.");
        }

        String expectedDigest = text(readback, "body_sha256");
        if (!expectedDigest.isEmpty() && !expectedDigest.equals(sha256(draftBody))) {
            finding(findings, "REPORT_BODY_DIGEST_MISMATCH", reportCommentId,
                    "Returned body digest does not match the complete draft body.");
        }
        String runStatus = text(sourceRun, "status");
        facts.add(new Fact("PUBLISHING_RUN_COMPLETION_NOT_REQUIRED", currentRunId,
                runStatus.isEmpty()
                        ? "Readback attribution is checked without requiring a completed publishing run."
                        : "Publishing run status is " + runStatus + "; completion is not required for this check."));
    }

    private static void checkParentRunReferences(
            String parentId, Map<String, JsonNode> runs, String body, List<Finding> findings) {
        for (JsonNode run : runs.values()) {
            if (!parentId.equals(text(run, "issue_id"))) continue;
            String runId = text(run, "id");
            if (runId.isEmpty()) {
                finding(findings, "PARENT_RUN_ID_UNVERIFIED", parentId,
                        "A parent run receipt has no full run ID.");
            } else if (!body.contains(runId)) {
                finding(findings, "PARENT_RUN_REFERENCE_MISSING", runId,
                        "Draft omits a full ID for a parent run in the supplied receipt set.");
            }
        }
    }

    private static void checkParentC3PlanComments(
            String parentId,
            String parentKey,
            String orchestratorId,
            String reportReadbackCommentId,
            JsonNode comments,
            JsonNode references,
            String body,
            List<Finding> findings) {
        if (parentId.isEmpty() || parentKey.isEmpty() || orchestratorId.isEmpty()) return;

        String parentLocator = issueLink(parentKey, parentId);
        for (JsonNode comment : arrayValues(comments)) {
            if (!parentId.equals(text(comment, "issue_id"))
                    || !orchestratorId.equals(text(comment, "author_id"))
                    || !isC3PlanComment(comment)) continue;

            String commentId = text(comment, "id");
            if (commentId.isEmpty()) {
                finding(findings, "PARENT_C3_PLAN_ID_UNVERIFIED", parentId,
                        "A native Orchestrator C3 plan comment has no full comment ID.");
                continue;
            }
            if (commentId.equals(reportReadbackCommentId)) continue;
            String sourceRunId = text(comment, "source_task_id");
            if (sourceRunId.isEmpty()) {
                finding(findings, "PARENT_C3_PLAN_RUN_UNVERIFIED", commentId,
                        "A native Orchestrator C3 plan comment has no source run ID.");
                continue;
            }

            boolean listed = arrayValues(references).stream().anyMatch(reference ->
                    commentId.equals(text(reference, "comment_id"))
                            && parentId.equals(text(reference, "issue_id"))
                            && sourceRunId.equals(text(reference, "run_id")));
            boolean cited = hasSingleLineWithAllTokens(body, List.of(parentLocator, commentId, sourceRunId));
            if (!listed || !cited) {
                finding(findings, "PARENT_C3_PLAN_REFERENCE_MISSING", commentId,
                        "Native Orchestrator C3 plan comment must appear by full comment ID in the reference ledger and beside its parent locator and source run ID in the draft.");
            }
        }
    }

    private static boolean isC3PlanComment(JsonNode comment) {
        // Bounded adapter for the adopted S05 C3 source: require an explicit C3 plan heading.
        String content = firstText(comment, "content", "body");
        return content.lines().map(String::trim)
                .filter(line -> line.matches("(?i)^#{1,6}\\s+.*"))
                .anyMatch(line -> (line.toLowerCase(Locale.ROOT).contains("c3")
                        || line.toLowerCase(Locale.ROOT).contains("c-3"))
                        && Pattern.compile("(?i)\\bplan\\b|decomposition|計畫|拆解")
                                .matcher(line).find());
    }

    private static void checkSourceDigest(
            String expectedDigest, String actualDigest, String subject, List<Finding> findings) {
        if (expectedDigest.isEmpty()) return;
        if (actualDigest.isEmpty()) {
            finding(findings, "REFERENCE_SOURCE_DIGEST_UNVERIFIED", subject,
                    "A source digest is claimed but the native receipt does not expose full content to verify it.");
        } else if (!expectedDigest.equals(actualDigest)) {
            finding(findings, "REFERENCE_SOURCE_DIGEST_MISMATCH", subject,
                    "Expected source digest differs from the digest of the native receipt content.");
        }
    }

    private static String sourceContentDigest(JsonNode source) {
        for (String field : List.of("content", "body")) {
            JsonNode value = source == null ? null : source.get(field);
            if (value != null && value.isTextual()) return "sha256:" + sha256(value.asText());
        }
        return "";
    }

    private static boolean hasSourceDigestInReferenceRow(String body, List<String> referenceTokens, String digest) {
        for (String line : body.split("\\R", -1)) {
            if (!referenceTokens.stream().allMatch(line::contains)) continue;
            if (line.toLowerCase(Locale.ROOT).contains("source digest")
                    && markdownCells(line).stream().anyMatch(cell -> containsExactRevisionToken(cell, digest))) {
                return true;
            }
        }
        return false;
    }

    private static void checkTimeline(
            JsonNode evidence,
            JsonNode parent,
            JsonNode readback,
            PublicationPhase phase,
            Map<String, JsonNode> runs,
            Map<String, JsonNode> comments,
            String body,
            List<Fact> facts,
            List<Finding> findings) {
        Instant publicationTime = phase == PublicationPhase.POSTPUBLICATION && readback.isObject()
                ? parseUtcInstant(text(readback, "created_at")) : null;
        List<TimedEvent> events = new ArrayList<>();
        addTimedEvent(events, parent, "Parent issue created", text(parent, "id"),
                issueLink(text(parent, "key"), text(parent, "id")), "created_at", publicationTime, findings);
        for (JsonNode child : arrayValues(evidence.path("children"))) {
            addTimedEvent(events, child, "Child issue created", text(child, "id"),
                    issueLink(text(child, "key"), text(child, "id")), "created_at", publicationTime, findings);
        }
        for (JsonNode run : runs.values()) {
            String runId = text(run, "id");
            addTimedEvent(events, run, "Run dispatched", runId, runId,
                    "dispatched_at", publicationTime, findings);
            addTimedEvent(events, run, "Run started", runId, runId,
                    "started_at", publicationTime, findings);
            addTimedEvent(events, run, "Run completed", runId, runId,
                    "completed_at", publicationTime, findings);
        }
        String readbackId = text(readback, "comment_id");
        for (JsonNode comment : comments.values()) {
            String commentId = text(comment, "id");
            if (commentId.equals(readbackId)) continue;
            String content = firstText(comment, "content", "body");
            String type = content.matches("(?s).*?(?:^|\\R)## Swarm Child Event(?:\\R|$).*" )
                    ? "Child event comment created" : "Comment created";
            addTimedEvent(events, comment, type, commentId, commentId,
                    "created_at", publicationTime, findings);
        }
        Set<String> seenArtifacts = new HashSet<>();
        for (JsonNode artifact : arrayValues(evidence.path("artifacts"))) {
            String locator = firstText(artifact, "locator", "id");
            if (!locator.isEmpty() && seenArtifacts.add(locator)) {
                addTimedEvent(events, artifact, "Artifact created", locator, locator,
                        "created_at", publicationTime, findings);
            }
        }

        if (events.isEmpty()) {
            finding(findings, "TIMELINE_EVIDENCE_UNAVAILABLE", "timeline",
                    "No typed event timestamps were available from the supplied issue, run, comment, or artifact receipts.");
            return;
        }
        List<String> timeline = timelineLines(body);
        if (timeline.isEmpty()) {
            finding(findings, "TIMELINE_SECTION_MISSING", "timeline",
                    "Draft has no UTC typed-events or 時序 section.");
            return;
        }

        List<TimedEvent> ordered = new ArrayList<>();
        for (TimedEvent event : events) {
            int lineIndex = matchingTimelineLine(timeline, event);
            if (lineIndex < 0) {
                finding(findings, "TIMELINE_EVENT_UNTYPED_OR_MISSING", event.sourceId(),
                        "Draft must show this exact receipt timestamp, event type, and source locator together.");
            } else {
                ordered.add(event.withLineIndex(lineIndex));
            }
        }
        for (String line : timeline) {
            if (UTC_TIMESTAMP.matcher(line).find()
                    && events.stream().noneMatch(event -> line.contains(event.timestamp())
                            && line.contains(event.sourceToken())
                            && line.toLowerCase(Locale.ROOT).contains(event.type().toLowerCase(Locale.ROOT)))) {
                finding(findings, "TIMELINE_EVENT_UNTYPED_OR_MISSING", "timeline",
                        "Timeline contains a timestamp that does not resolve to one of the supplied typed receipts.");
            }
        }
        ordered.sort(Comparator.comparing(TimedEvent::at));
        int cursor = 0;
        int previousGroupLastLine = -1;
        while (cursor < ordered.size()) {
            int end = cursor + 1;
            Instant at = ordered.get(cursor).at();
            int groupFirstLine = ordered.get(cursor).lineIndex();
            int groupLastLine = groupFirstLine;
            while (end < ordered.size() && ordered.get(end).at().equals(at)) {
                groupFirstLine = Math.min(groupFirstLine, ordered.get(end).lineIndex());
                groupLastLine = Math.max(groupLastLine, ordered.get(end).lineIndex());
                end++;
            }
            if (groupFirstLine < previousGroupLastLine) {
                finding(findings, "TIMELINE_EVENTS_OUT_OF_ORDER", ordered.get(cursor).sourceId(),
                        "Typed events are not listed in timestamp order.");
                break;
            }
            previousGroupLastLine = groupLastLine;
            cursor = end;
        }
        if (ordered.isEmpty() && events.stream().noneMatch(event -> event.at() != null)) {
            finding(findings, "TIMELINE_TIMESTAMP_UNVERIFIED", "timeline",
                    "Receipt timestamps were present but none could be parsed as UTC.");
        }
        if (findings.stream().noneMatch(finding -> finding.code().startsWith("TIMELINE_"))) {
            facts.add(new Fact("TYPED_CHRONOLOGY_VERIFIED", "timeline",
                    "Issue, run, comment, and artifact receipt times are labeled and listed in timestamp order."));
        }
    }

    private static void addTimedEvent(
            List<TimedEvent> events,
            JsonNode source,
            String type,
            String sourceId,
            String sourceToken,
            String timestampField,
            Instant publicationTime,
            List<Finding> findings) {
        String timestamp = text(source, timestampField);
        if (timestamp.isEmpty()) return;
        Instant at = parseUtcInstant(timestamp);
        if (at == null) {
            finding(findings, "TIMELINE_TIMESTAMP_NOT_UTC", sourceId,
                    "Receipt timestamp is not a parseable UTC instant: " + timestampField + ".");
            return;
        }
        if (publicationTime != null && at.isAfter(publicationTime)) return;
        events.add(new TimedEvent(at, timestamp, type, sourceId, sourceToken, -1));
    }

    private static void checkRunTimestampOrder(Iterable<JsonNode> runs, List<Finding> findings) {
        for (JsonNode run : runs) {
            String runId = text(run, "id");
            Instant dispatched = parseUtcInstant(text(run, "dispatched_at"));
            Instant started = parseUtcInstant(text(run, "started_at"));
            Instant completed = parseUtcInstant(text(run, "completed_at"));
            if ((dispatched != null && started != null && dispatched.isAfter(started))
                    || (started != null && completed != null && started.isAfter(completed))) {
                finding(findings, "RUN_TIMESTAMP_ORDER_MISMATCH", runId,
                        "Run dispatch, start, and completion timestamps contradict their typed order.");
            }
        }
    }

    private static int matchingTimelineLine(List<String> lines, TimedEvent event) {
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.contains(event.timestamp())
                    && line.contains(event.sourceToken())
                    && line.toLowerCase(Locale.ROOT).contains(event.type().toLowerCase(Locale.ROOT))) {
                return index;
            }
        }
        return -1;
    }

    private static List<String> timelineLines(String body) {
        List<String> lines = new ArrayList<>();
        boolean inTimeline = false;
        for (String line : body.split("\\R", -1)) {
            String trimmed = line.trim();
            if (trimmed.matches("#{2,3}\\s+.*")) {
                if (inTimeline) break;
                String title = trimmed.toLowerCase(Locale.ROOT);
                inTimeline = title.contains("utc typed") || title.contains("時序");
            } else if (inTimeline) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static Instant parseUtcInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            OffsetDateTime parsed = OffsetDateTime.parse(value);
            return parsed.getOffset().getTotalSeconds() == 0 ? parsed.toInstant() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void rejectConflictingReceipts(JsonNode evidence, List<Finding> findings) {
        for (String collection : List.of("children", "runs", "comments", "artifacts")) {
            Map<String, JsonNode> identities = new LinkedHashMap<>();
            Set<String> conflicts = new java.util.TreeSet<>();
            List<JsonNode> rows = arrayValues(evidence.path(collection));
            if (collection.equals("children")) rows.add(evidence.path("parent_issue"));
            if (collection.equals("runs")) rows.add(evidence.path("current_run"));
            for (JsonNode row : rows) {
                for (String field : collection.equals("artifacts") ? List.of("id", "locator") : List.of("id")) {
                    String identity = text(row, field);
                    if (identity.isEmpty()) continue;
                    JsonNode previous = identities.putIfAbsent(identity, row);
                    if (previous != null && !previous.equals(row)) conflicts.add(identity);
                }
            }
            for (String identity : conflicts) {
                finding(findings, "CONFLICTING_RECEIPT_IDENTITY", identity,
                        "Conflicting " + collection + " receipts share an identity; input order cannot establish authority.");
            }
        }
    }

    private static Map<String, JsonNode> indexIssues(JsonNode parent, JsonNode children) {
        Map<String, JsonNode> issues = new LinkedHashMap<>();
        String parentId = text(parent, "id");
        if (!parentId.isEmpty()) issues.put(parentId, withKey(parent, text(parent, "key")));
        for (JsonNode child : arrayValues(children)) {
            String id = text(child, "id");
            if (!id.isEmpty()) issues.put(id, withKey(child, text(child, "key")));
        }
        return issues;
    }

    private static JsonNode withKey(JsonNode node, String key) {
        if (node.has("key")) return node;
        com.fasterxml.jackson.databind.node.ObjectNode copy = node.deepCopy();
        copy.put("key", key);
        return copy;
    }

    private static Map<String, JsonNode> indexById(JsonNode rows) {
        Map<String, JsonNode> indexed = new LinkedHashMap<>();
        for (JsonNode row : arrayValues(rows)) {
            String id = text(row, "id");
            if (!id.isEmpty()) indexed.put(id, row);
        }
        return indexed;
    }

    private static Map<String, JsonNode> indexArtifacts(JsonNode rows) {
        Map<String, JsonNode> indexed = new LinkedHashMap<>();
        for (JsonNode row : arrayValues(rows)) {
            String id = text(row, "id");
            String locator = text(row, "locator");
            if (!id.isEmpty()) indexed.put(id, row);
            if (!locator.isEmpty()) indexed.put(locator, row);
        }
        return indexed;
    }

    private static List<JsonNode> arrayValues(JsonNode array) {
        List<JsonNode> values = new ArrayList<>();
        if (array != null && array.isArray()) array.forEach(values::add);
        return values;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.isObject()) return "";
        JsonNode value = node.get(field);
        return value != null && value.isValueNode() && !value.isNull() ? value.asText().trim() : "";
    }

    private static String rawText(JsonNode node, String field) {
        if (node == null || !node.isObject()) return "";
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : "";
    }

    private static String firstText(JsonNode node, String first, String second) {
        String value = text(node, first);
        return value.isEmpty() ? text(node, second) : value;
    }

    private static String issueLink(String issueKey, String issueId) {
        return "[" + issueKey + "](" + ISSUE_URI_PREFIX + issueId + ")";
    }

    private static boolean hasSingleLineWithAllTokens(String body, List<String> tokens) {
        for (String line : body.split("\\R", -1)) {
            boolean allPresent = tokens.stream().allMatch(token -> line.contains(token));
            if (allPresent) return true;
        }
        return false;
    }

    private static boolean hasReferenceRowWithRevision(String body, List<String> referenceTokens, String revision) {
        String[] lines = body.split("\\R", -1);
        for (int rowIndex = 0; rowIndex < lines.length; rowIndex++) {
            String row = lines[rowIndex];
            if (!referenceTokens.stream().allMatch(row::contains)) continue;
            List<String> cells = markdownCells(row);
            if (cells.stream().anyMatch(cell -> hasLabeledExactRevision(cell, revision))) return true;
            if (hasRevisionColumnValue(lines, rowIndex, cells, revision)) return true;
        }
        return false;
    }

    private static boolean hasRevisionColumnValue(
            String[] lines, int rowIndex, List<String> rowCells, String revision) {
        for (int headerIndex = rowIndex - 1; headerIndex >= 0; headerIndex--) {
            String candidate = lines[headerIndex].trim();
            if (candidate.isEmpty() || !candidate.startsWith("|")) break;
            if (headerIndex + 1 >= rowIndex || !isMarkdownSeparator(lines[headerIndex + 1])) continue;

            List<String> headerCells = markdownCells(candidate);
            if (headerCells.size() != rowCells.size()) continue;
            for (int cellIndex = 0; cellIndex < headerCells.size(); cellIndex++) {
                if (isRevisionHeader(headerCells.get(cellIndex))
                        && containsExactRevisionToken(rowCells.get(cellIndex), revision)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isMarkdownSeparator(String line) {
        String trimmed = line.trim();
        if (!trimmed.startsWith("|")) return false;
        List<String> cells = markdownCells(trimmed);
        return !cells.isEmpty() && cells.stream().allMatch(cell -> cell.matches(":?-+:?"));
    }

    private static boolean isRevisionHeader(String cell) {
        String header = cell.toLowerCase(Locale.ROOT);
        return header.contains("revision") || header.contains("commit")
                || header.contains("digest") || header.contains("hash");
    }

    private static List<String> markdownCells(String line) {
        String trimmed = line.trim();
        if (!trimmed.startsWith("|")) return List.of(trimmed);
        String[] cells = trimmed.split("\\|", -1);
        List<String> result = new ArrayList<>();
        for (int index = 1; index < cells.length - 1; index++) result.add(cells[index].trim());
        return result;
    }

    private static boolean hasLabeledExactRevision(String cell, String revision) {
        String quotedRevision = java.util.regex.Pattern.quote(revision);
        return java.util.regex.Pattern.compile(
                "(?i)\\b(?:subject\\s+|source\\s+)?revision\\b\\s*[:=]?\\s*`?"
                        + quotedRevision + "`?(?![A-Za-z0-9._-])")
                .matcher(cell).find();
    }

    private static boolean containsExactRevisionToken(String cell, String revision) {
        int start = cell.indexOf(revision);
        while (start >= 0) {
            int end = start + revision.length();
            boolean leftBoundary = start == 0 || !revisionTokenCharacter(cell.charAt(start - 1));
            boolean rightBoundary = end == cell.length() || !revisionTokenCharacter(cell.charAt(end));
            if (leftBoundary && rightBoundary) return true;
            start = cell.indexOf(revision, start + 1);
        }
        return false;
    }

    private static boolean revisionTokenCharacter(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '-' || value == '.';
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static void finding(List<Finding> findings, String code, String subject, String reason) {
        boolean exists = findings.stream().anyMatch(existing ->
                existing.code().equals(code) && Objects.equals(existing.subject(), subject));
        if (!exists) findings.add(new Finding(code, subject == null ? "" : subject, reason));
    }

    private record TimedEvent(
            Instant at, String timestamp, String type, String sourceId, String sourceToken, int lineIndex) {
        private TimedEvent withLineIndex(int index) {
            return new TimedEvent(at, timestamp, type, sourceId, sourceToken, index);
        }
    }
}

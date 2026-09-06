package com.featuredeliveryintelligence.fdi.validation.task7;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Deterministic Task 7 evaluation for the completed PKB-001 comparison. Java
 * port of the transitional Python consumer {@code pkb001_task7_evaluate.py}:
 * the evaluator validates both sealed judgment workspaces before reading the
 * blind key, verifies every bound input digest, computes descriptive metrics,
 * and applies the pre-declared bounded decision rule. Every validation
 * failure produces a persistable fail-closed STOP report; observed values are
 * never treated as retroactive acceptance thresholds.
 *
 * <p>The public report renders byte-for-byte like the Python consumer's
 * {@code json.dumps(report, indent=2, sort_keys=True) + "\n"} output, and the
 * CLI exit vocabulary is preserved: 0 for a completed evaluation, 2 for STOP.
 */
public final class Task7Evaluation {
    static final String TASK6 = "validation/pkb001/task6-blind-review";
    static final String PACKET = TASK6 + "/blind-review-packet.json";
    static final String TASK6_MANIFEST = TASK6 + "/manifest.json";
    static final String SEALED_KEY = TASK6 + "/sealed-blind-key.json";
    static final String JUDGMENT_REVIEWER_01 =
            TASK6 + "/judgment-workspaces/reviewer-01/judgment-template.json";
    static final String JUDGMENT_REVIEWER_02 =
            TASK6 + "/judgment-workspaces/reviewer-02/judgment-template.json";
    static final List<String> JUDGMENTS = List.of(JUDGMENT_REVIEWER_01, JUDGMENT_REVIEWER_02);
    static final String FORWARD_RUN =
            "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json";
    static final String FORWARD_MANIFEST =
            "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-manifest.json";
    static final String FORWARD_WITNESS =
            "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-witness.json";
    static final String REVERSE_RUN =
            "validation/pkb001/reverse-task5-pkb001_reverse_run/capability-hypotheses.json";
    static final String REVERSE_MANIFEST =
            "validation/pkb001/reverse-task5-pkb001_reverse_run/manifest.json";
    static final String REVERSE_WITNESS =
            "validation/pkb001/reverse-task5-pkb001_reverse_run/provenance-witness.json";
    static final String GOLD = "validation/pkb001/evaluator/petclinic-818c413/gold-mappings.json";
    static final String GOLD_SEAL =
            "validation/pkb001/evaluator/petclinic-818c413/ground-truth-seal.json";

    static final List<String> ACTIONS = List.of("ACCEPT", "ADD_MISSING", "MERGE", "RENAME", "REJECT", "SPLIT");
    static final List<String> OUTCOMES =
            List.of("DUPLICATE", "PARTIALLY_SUPPORTED", "SUPPORTED", "UNSUPPORTED");
    static final List<String> SCORES = List.of("evidence_validity", "usefulness", "precision");
    static final int EXPECTED_ITEMS = 15;
    static final int STOP_EXIT_CODE = 2;

    private static final JsonNodeFactory NODE = JsonNodeFactory.instance;

    /** Raised when a sealed input or evaluation invariant does not validate. */
    public static final class EvaluationException extends IllegalArgumentException {
        EvaluationException(String message) {
            super(message);
        }
    }

    /** Ports the Python {@code bounded_decision} rule; {@code qualityPassed} may be null. */
    public static String boundedDecision(boolean integrityPassed, boolean thresholdsPreregistered,
            Boolean qualityPassed, boolean humanReviewCompleted) {
        if (!integrityPassed) {
            return "STOP";
        }
        if (!thresholdsPreregistered || !Boolean.TRUE.equals(qualityPassed)) {
            return "REVISE";
        }
        if (!humanReviewCompleted) {
            return "REVISE";
        }
        return "GO";
    }

    /** Evaluates fail-closed, persisting a STOP-shaped result for every validation error. */
    public JsonNode evaluateRepository(Path root) {
        Path resolved = resolveRoot(root);
        try {
            return evaluateResolved(resolved);
        } catch (EvaluationException error) {
            return stopReport("POST_BINDING_INTEGRITY_VALIDATION", error);
        }
    }

    public byte[] toReportBytes(JsonNode report) {
        return Task7Json.toReportBytes(report);
    }

    /** Ports {@code build_third_review_packet}: derives the pending packet from a report. */
    public JsonNode buildThirdReviewPacket(JsonNode report) {
        JsonNode pending = report.get("pending_third_review");
        ObjectNode packet = NODE.objectNode();
        packet.put("schema_version", "pkb001.task7.third-review-pending.v1");
        packet.put("status", pending.get("status").asText());
        packet.set("packet_sha256", pendingReportPacketSha256(report));
        ObjectNode context = NODE.objectNode();
        context.put("actor_type", "NON_HUMAN_OR_HUMAN_EVALUATOR");
        context.put("authority", "EVALUATOR_ONLY");
        context.put("independent_from_reviewer_01_and_reviewer_02", true);
        context.put("can_complete_product_team_review", false);
        packet.set("reviewer_context_required", context);
        packet.set("item_count", pending.get("item_count"));
        packet.set("items", pending.get("items"));
        packet.put("instruction",
                "Record an independent third judgment; do not infer or publish Product truth.");
        return packet;
    }

    private static JsonNode pendingReportPacketSha256(JsonNode report) {
        return report.get("pre_unblinding_validation").get("packet_sha256");
    }

    private JsonNode evaluateResolved(Path root) {
        final ObjectNode pre;
        try {
            pre = validatePreUnblinding(root);
        } catch (EvaluationException error) {
            return stopReport("PRE_UNBLINDING_VALIDATION", error);
        }
        final Bound bound;
        try {
            bound = validateBoundInputs(root, pre);
        } catch (EvaluationException error) {
            return stopReport("BOUND_INPUT_VALIDATION", error);
        }
        return evaluateBound(root, pre, bound);
    }

    // ------------------------------------------------------------------
    // Pre-unblinding validation
    // ------------------------------------------------------------------

    private ObjectNode validatePreUnblinding(Path root) {
        Path packetPath = root.resolve(PACKET);
        JsonNode packet = loadJson(packetPath);
        String packetDigest = sha256(packetPath);
        List<String> packetIds = packetItemIds(packet);
        require(packetIds.size() == EXPECTED_ITEMS && new LinkedHashSet<>(packetIds).size() == EXPECTED_ITEMS
                        && packetIds.stream().allMatch(id -> id != null && !id.isEmpty()),
                "blind packet must contain 15 unique item IDs");
        String packetText = Task7Json.compact(packet);
        require(!packetText.contains("source_arm") && !packetText.contains("source_identifier"),
                "blind packet exposes source identity before unblinding");

        List<Workspace> workspaces = new ArrayList<>();
        for (String relative : JUDGMENTS) {
            Path path = root.resolve(relative);
            Path packetInput = path.getParent().resolve("packet-input.json");
            JsonNode workspace = loadJson(path);
            JsonNode rows = workspace.get("judgments");
            require(rows != null && rows.isArray() && rows.size() == EXPECTED_ITEMS
                            && allComplete(rows),
                    relative + " must contain 15 complete judgments");
            List<String> rowIds = new ArrayList<>();
            for (JsonNode row : rows) {
                rowIds.add(row.get("blind_id").asText());
            }
            require(rowIds.equals(packetIds), relative + " judgment IDs do not match the packet");
            require(workspace.get("packet_sha256") != null
                            && workspace.get("packet_sha256").isTextual()
                            && workspace.get("packet_sha256").asText().equals(packetDigest),
                    relative + " packet digest mismatch");
            String packetInputRelative = root.relativize(packetInput).toString();
            require(sha256(packetInput).equals(packetDigest),
                    packetInputRelative + " digest does not match the sealed packet");
            JsonNode context = workspace.get("reviewer_context");
            require(context != null && context.isObject()
                            && "NON_HUMAN".equals(textOrNull(context.get("actor_type")))
                            && "EVALUATOR_ONLY".equals(textOrNull(context.get("authority")))
                            && isFalse(context.get("can_complete_product_team_review")),
                    relative + " claims invalid reviewer authority");
            JsonNode isolation = workspace.get("reviewer_isolation");
            require(isolation != null && isolation.isObject()
                            && isFalse(isolation.get("ground_truth_accessible_from_packet_workspace"))
                            && isFalse(isolation.get("other_workspace_future_judgments_accessible"))
                            && isFalse(isolation.get("sealed_key_accessible")),
                    relative + " does not preserve reviewer isolation");
            workspaces.add(new Workspace(relative, workspace, sha256(path)));
        }

        JsonNode firstId = workspaces.get(0).node().get("workspace_id");
        JsonNode secondId = workspaces.get(1).node().get("workspace_id");
        require(firstId != null && secondId != null && !firstId.equals(secondId),
                "reviewer workspace identities must be independent");

        ObjectNode pre = NODE.objectNode();
        pre.put("passed", true);
        pre.put("validated_before_sealed_key_read", true);
        pre.put("packet_sha256", packetDigest);
        pre.put("item_count", EXPECTED_ITEMS);
        ArrayNode reviewers = pre.putArray("reviewers");
        for (Workspace workspace : workspaces) {
            ObjectNode reviewer = reviewers.addObject();
            reviewer.set("workspace_id", workspace.node().get("workspace_id"));
            reviewer.put("actor_type", "NON_HUMAN");
            reviewer.put("authority", "EVALUATOR_ONLY");
            reviewer.put("can_complete_product_team_review", false);
            reviewer.put("judgment_count", workspace.node().get("judgments").size());
            reviewer.put("judgment_sha256", workspace.digest());
        }
        return pre;
    }

    private List<String> packetItemIds(JsonNode packet) {
        JsonNode items = packet.get("items");
        List<String> ids = new ArrayList<>();
        if (items != null && items.isArray()) {
            for (JsonNode item : items) {
                ids.add(item != null && item.has("blind_id") && item.get("blind_id").isTextual()
                        ? item.get("blind_id").asText() : null);
            }
        }
        return ids;
    }

    private boolean allComplete(JsonNode rows) {
        for (JsonNode row : rows) {
            if (!completeJudgment(row)) {
                return false;
            }
        }
        return true;
    }

    private boolean completeJudgment(JsonNode row) {
        if (row == null || !row.isObject()
                || row.get("blind_id") == null || !row.get("blind_id").isTextual()
                || !ACTIONS.contains(textOrNull(row.get("review_action")))
                || !OUTCOMES.contains(textOrNull(row.get("outcome")))) {
            return false;
        }
        for (String field : SCORES) {
            JsonNode score = row.get(field);
            if (score == null || !score.isNumber() || score.isBoolean()
                    || score.doubleValue() < 0 || score.doubleValue() > 1) {
                return false;
            }
        }
        return stringList(row.get("unsupported_claims"))
                && stringList(row.get("limitations"))
                && row.get("reviewer_notes") != null
                && row.get("reviewer_notes").isTextual()
                && !row.get("reviewer_notes").asText().isEmpty()
                && row.get("active_review_seconds") != null
                && row.get("active_review_seconds").isIntegralNumber()
                && !row.get("active_review_seconds").isBoolean()
                && row.get("active_review_seconds").longValue() >= 0;
    }

    private boolean stringList(JsonNode value) {
        if (value == null || !value.isArray()) {
            return false;
        }
        for (JsonNode item : value) {
            if (item == null || !item.isTextual() || item.asText().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Bound-input validation
    // ------------------------------------------------------------------

    private record Workspace(String relative, JsonNode node, String digest) { }

    private record Bound(JsonNode task6Manifest, JsonNode key, JsonNode seal) { }

    private Bound validateBoundInputs(Path root, ObjectNode pre) {
        JsonNode manifest = loadJson(root.resolve(TASK6_MANIFEST));
        require(sha256(root.resolve(PACKET)).equals(textOrNull(manifest.get("packet_sha256")))
                        && textOrNull(manifest.get("packet_sha256"))
                                .equals(pre.get("packet_sha256").asText()),
                "packet digest does not match Task 6 manifest");
        require(sha256(root.resolve(SEALED_KEY)).equals(textOrNull(manifest.get("sealed_key_sha256"))),
                "sealed key digest does not match Task 6 manifest");
        Map<String, String> task6Inputs = new HashMap<>();
        for (JsonNode row : requiredArray(manifest.get("input_digests"), "input_digests")) {
            task6Inputs.put(textOrNull(row.get("path")), textOrNull(row.get("sha256")));
        }
        for (String relative : List.of(FORWARD_RUN, FORWARD_MANIFEST, FORWARD_WITNESS,
                REVERSE_RUN, REVERSE_MANIFEST, REVERSE_WITNESS)) {
            require(task6Inputs.containsKey(relative)
                            && task6Inputs.get(relative).equals(sha256(root.resolve(relative))),
                    "source run digest mismatch: " + relative);
        }

        JsonNode seal = loadJson(root.resolve(GOLD_SEAL));
        require(textOrNull(seal.get("gold_sha256")).equals(sha256(root.resolve(GOLD))),
                "evaluator gold digest does not match ground-truth seal");
        require("SEALED".equals(textOrNull(seal.get("status")))
                        && "VERIFIED".equals(textOrNull(seal.get("isolation_status")))
                        && isFalse(seal.get("human_review_completed")),
                "evaluator ground-truth seal invariant failed");
        JsonNode bindings = manifest.get("source_bindings");
        require(textOrNull(seal.get("source_commit_sha"))
                        .equals(textOrNull(bindings == null ? null : bindings.get("shared_source_commit_sha")))
                        && textOrNull(seal.get("graph_sha256"))
                                .equals(textOrNull(bindings == null ? null : bindings.get("shared_graph_sha256"))),
                "evaluator gold source binding mismatch");
        JsonNode key = loadJson(root.resolve(SEALED_KEY));
        require(textOrNull(key.get("sealed_packet_sha256")).equals(pre.get("packet_sha256").asText()),
                "sealed key packet digest mismatch");
        return new Bound(manifest, key, seal);
    }

    // ------------------------------------------------------------------
    // Metric computation (post-binding)
    // ------------------------------------------------------------------

    private JsonNode evaluateBound(Path root, ObjectNode pre, Bound bound) {
        JsonNode packet = loadJson(root.resolve(PACKET));
        JsonNode forward = loadJson(root.resolve(FORWARD_RUN));
        JsonNode reverse = loadJson(root.resolve(REVERSE_RUN));
        JsonNode gold = loadJson(root.resolve(GOLD));

        JsonNode keyItems = bound.key().get("items");
        List<String> packetIds = new ArrayList<>();
        for (JsonNode item : packet.get("items")) {
            packetIds.add(item.get("blind_id").asText());
        }
        List<String> keyIds = new ArrayList<>();
        if (keyItems != null && keyItems.isArray()) {
            for (JsonNode item : keyItems) {
                keyIds.add(item != null && item.has("blind_id") ? textOrNull(item.get("blind_id")) : null);
            }
        }
        require(keyIds.size() == EXPECTED_ITEMS && keyIds.equals(packetIds),
                "sealed key does not account for every packet item");

        Set<String> forwardIds = new LinkedHashSet<>();
        for (JsonNode row : requiredArray(forward.get("capability_results"), "capability_results")) {
            forwardIds.add(textOrNull(row.get("capability_id")));
        }
        Set<String> reverseIds = new LinkedHashSet<>();
        for (JsonNode row : requiredArray(reverse.get("hypotheses"), "hypotheses")) {
            reverseIds.add(textOrNull(row.get("proposal_id")));
        }
        Set<String> keyForwardIds = new LinkedHashSet<>();
        Set<String> keyReverseIds = new LinkedHashSet<>();
        for (JsonNode item : keyItems) {
            if ("FORWARD".equals(textOrNull(item.get("source_arm")))) {
                keyForwardIds.add(textOrNull(item.get("source_identifier")));
            }
            if ("REVERSE".equals(textOrNull(item.get("source_arm")))) {
                keyReverseIds.add(textOrNull(item.get("source_identifier")));
            }
        }
        require(keyForwardIds.equals(forwardIds) && keyReverseIds.equals(reverseIds),
                "sealed key source identifiers do not match the bound runs");

        List<JsonNode> workspaces = new ArrayList<>();
        for (String relative : JUDGMENTS) {
            workspaces.add(loadJson(root.resolve(relative)));
        }
        Map<String, Map<String, JsonNode>> rowsByReviewer = new LinkedHashMap<>();
        for (JsonNode workspace : workspaces) {
            Map<String, JsonNode> rows = new LinkedHashMap<>();
            for (JsonNode row : requiredArray(workspace.get("judgments"), "judgments")) {
                rows.put(row.get("blind_id").asText(), row);
            }
            rowsByReviewer.put(workspace.get("workspace_id").asText(), rows);
        }
        Map<String, JsonNode> keyById = new LinkedHashMap<>();
        for (JsonNode item : keyItems) {
            keyById.put(item.get("blind_id").asText(), item);
        }
        Map<String, List<String>> armIds = new LinkedHashMap<>();
        armIds.put("FORWARD", new ArrayList<>());
        armIds.put("REVERSE", new ArrayList<>());
        for (String blindId : packetIds) {
            armIds.get(keyById.get(blindId).get("source_arm").asText()).add(blindId);
        }

        ObjectNode agreement = agreement(packetIds, rowsByReviewer);
        List<String> disagreementIds = new ArrayList<>(new TreeSet<>(union(
                textList(agreement.get("action_disagreement_ids")),
                textList(agreement.get("outcome_disagreement_ids")))));
        ArrayNode pendingItems = NODE.arrayNode();
        for (String blindId : disagreementIds) {
            ObjectNode item = pendingItems.addObject();
            item.put("blind_id", blindId);
            ArrayNode reasons = item.putArray("reasons");
            if (textList(agreement.get("action_disagreement_ids")).contains(blindId)) {
                reasons.add("ACTION_DISAGREEMENT");
            }
            if (textList(agreement.get("outcome_disagreement_ids")).contains(blindId)) {
                reasons.add("OUTCOME_DISAGREEMENT");
            }
        }

        Map<String, String> reverseLabels = new LinkedHashMap<>();
        for (JsonNode row : requiredArray(reverse.get("hypotheses"), "hypotheses")) {
            reverseLabels.put(row.get("proposal_id").asText(), textOrNull(row.get("label")));
        }
        List<String> sortedReviewers = new ArrayList<>(rowsByReviewer.keySet());
        java.util.Collections.sort(sortedReviewers);
        ArrayNode reverseResults = NODE.arrayNode();
        for (String blindId : armIds.get("REVERSE")) {
            String sourceId = keyById.get(blindId).get("source_identifier").asText();
            ObjectNode result = reverseResults.addObject();
            result.put("blind_id", blindId);
            result.put("source_identifier", sourceId);
            result.put("label", reverseLabels.get(sourceId));
            ArrayNode reviewers = result.putArray("reviewers");
            for (String reviewer : sortedReviewers) {
                JsonNode row = rowsByReviewer.get(reviewer).get(blindId);
                ObjectNode entry = reviewers.addObject();
                entry.put("workspace_id", reviewer);
                entry.set("review_action", row.get("review_action"));
                entry.set("outcome", row.get("outcome"));
                entry.set("evidence_validity", row.get("evidence_validity"));
                entry.set("usefulness", row.get("usefulness"));
                entry.set("precision", row.get("precision"));
            }
            result.put("third_review_status", disagreementIds.contains(blindId) ? "PENDING" : "NOT_REQUIRED");
        }

        List<String> digestPaths = new ArrayList<>(List.of(
                PACKET, SEALED_KEY, JUDGMENT_REVIEWER_01, JUDGMENT_REVIEWER_02,
                FORWARD_RUN, FORWARD_MANIFEST, FORWARD_WITNESS,
                REVERSE_RUN, REVERSE_MANIFEST, REVERSE_WITNESS, GOLD, GOLD_SEAL));
        String decision = boundedDecision(true, false, null, false);

        ObjectNode report = NODE.objectNode();
        report.put("schema_version", "pkb001.task7.evaluation-report.v1");
        report.put("report_id", "pkb001-task7-petclinic-818c413-v1");
        report.put("decision", decision);
        report.put("decision_scope", "BOUNDED_PROTOTYPE_DECISION_NO_SEMANTIC_PUBLICATION");
        report.set("pre_unblinding_validation", pre);

        ObjectNode bindings = report.putObject("bindings");
        JsonNode sourceBindings = bound.task6Manifest().get("source_bindings");
        bindings.put("source_commit_sha", textOrNull(sourceBindings.get("shared_source_commit_sha")));
        bindings.put("graph_sha256", textOrNull(sourceBindings.get("shared_graph_sha256")));
        bindings.put("reverse_delivery_history_sha256",
                textOrNull(sourceBindings.get("reverse_delivery_history_sha256")));
        ObjectNode artifacts = bindings.putObject("artifacts");
        for (String relative : digestPaths) {
            artifacts.put(relative, sha256(root.resolve(relative)));
        }

        ObjectNode integrity = report.putObject("integrity");
        integrity.put("status", "PASSED");
        integrity.put("snapshot_binding", "PASSED");
        integrity.put("reviewer_isolation", "ATTESTED_AND_CONTRACT_VALIDATED");
        integrity.put("evidence_integrity", "PASSED");
        integrity.put("product_truth_boundary", "PASSED");
        integrity.putArray("hard_stop_violations");

        ObjectNode metrics = report.putObject("metrics");
        ObjectNode byArm = metrics.putObject("by_arm");
        for (Map.Entry<String, List<String>> arm : armIds.entrySet()) {
            ObjectNode armMetrics = armMetrics(arm.getValue(), rowsByReviewer);
            armMetrics.set("agreement", agreement(arm.getValue(), rowsByReviewer));
            byArm.set(arm.getKey(), armMetrics);
        }
        metrics.set("overall", armMetrics(packetIds, rowsByReviewer));
        metrics.put("interpretation", "DESCRIPTIVE_ONLY_NO_RETROACTIVE_ACCEPTANCE_GATE");

        report.set("reviewer_agreement", agreement);
        ObjectNode pendingThird = report.putObject("pending_third_review");
        pendingThird.put("status", "PENDING_INDEPENDENT_THIRD_REVIEW");
        pendingThird.put("basis", "FROZEN_PROTOCOL_REQUIRES_THIRD_REVIEW_WHEN_REVIEWERS_DISAGREE");
        pendingThird.put("item_count", pendingItems.size());
        pendingThird.set("items", pendingItems);

        report.set("forward_expected_realization_comparison", forwardComparison(forward, gold));
        report.set("reverse_proposal_review_results", reverseResults);

        ObjectNode thresholds = report.putObject("thresholds");
        thresholds.put("preregistered_before_execution", false);
        thresholds.put("observed_metrics_used_as_acceptance_thresholds", false);
        thresholds.put("defect", "NUMERIC_ACCEPTANCE_THRESHOLDS_NOT_FROZEN_BEFORE_GENERATION_AND_JUDGMENT");
        thresholds.put("effect", "GO_IS_NOT_SUPPORTABLE_FROM_THIS_RUN");

        ObjectNode humanReview = report.putObject("human_product_team_review");
        humanReview.put("status", "PENDING");
        humanReview.put("completed_by_non_human_reviewers", false);
        humanReview.put("semantic_publication_allowed", false);
        humanReview.put("authority", "PRODUCT_TEAM_ONLY");

        ArrayNode nextRequirements = report.putArray("next_experiment_requirements");
        nextRequirements.add("Pre-register numeric acceptance thresholds before generation and judgment.");
        nextRequirements.add("Add UI/template evidence to Graphify input or narrow capability descriptions to indexed evidence.");
        nextRequirements.add("Repeat the comparison with real human Product Team reviewers.");
        nextRequirements.add("Preserve the exact source revision and delivery-history cutoff.");

        ArrayNode proofLimits = report.putArray("proof_limits");
        proofLimits.add("Reviewer isolation is supported by distinct workspaces and recorded non-access attestations, not cryptographic proof of model context.");
        proofLimits.add("The Task 6 packet provides deterministic label/order blinding only; ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT.");
        proofLimits.add("File-component path overlap, expected graph-node citation coverage, and exact proposed-component graph-node matching are separate metrics.");
        proofLimits.add("Non-human evaluator judgments do not establish Product meaning or permit semantic publication.");
        return report;
    }

    private ObjectNode forwardComparison(JsonNode forward, JsonNode gold) {
        Map<String, JsonNode> expected = new LinkedHashMap<>();
        for (JsonNode row : requiredArray(gold.get("mappings"), "mappings")) {
            expected.put(textOrNull(row.get("capability_id")), row);
        }
        ArrayNode details = NODE.arrayNode();
        long totalExpected = 0;
        long totalExact = 0;
        long totalCited = 0;
        long totalPathMatches = 0;
        long totalProposed = 0;
        long totalProposedPathMatches = 0;
        Set<String> resultIds = new LinkedHashSet<>();
        for (JsonNode result : requiredArray(forward.get("capability_results"), "capability_results")) {
            String capabilityId = textOrNull(result.get("capability_id"));
            require(expected.containsKey(capabilityId), "forward capability missing from gold: " + capabilityId);
            resultIds.add(capabilityId);
            JsonNode proposed = requiredArray(result.get("proposed_components"), "proposed_components");
            JsonNode wanted = requiredArray(expected.get(capabilityId).get("expected_components"),
                    "expected_components");
            Set<String> proposedIds = new LinkedHashSet<>();
            for (JsonNode row : proposed) {
                proposedIds.add(textOrNull(row.get("graph_node_id")));
            }
            Set<String> citedIds = new LinkedHashSet<>(proposedIds);
            JsonNode evidenceRefs = result.get("evidence_refs");
            if (evidenceRefs != null && evidenceRefs.isArray()) {
                for (JsonNode row : evidenceRefs) {
                    citedIds.add(textOrNull(row.get("graph_node_id")));
                }
            }
            Set<String> expectedIds = new LinkedHashSet<>();
            for (JsonNode row : wanted) {
                expectedIds.add(textOrNull(row.get("graph_node_id")));
            }
            List<String> proposedPaths = new ArrayList<>();
            for (JsonNode row : proposed) {
                proposedPaths.add(sourcePath(row));
            }
            Set<String> expectedPaths = new LinkedHashSet<>();
            for (JsonNode row : wanted) {
                expectedPaths.add(sourcePath(row));
            }
            Set<String> proposedPathSet = new LinkedHashSet<>(proposedPaths);
            long proposedExact = intersectionSize(proposedIds, expectedIds);
            long expectedNodesCited = intersectionSize(citedIds, expectedIds);
            long expectedPathMatches = 0;
            for (JsonNode row : wanted) {
                if (proposedPathSet.contains(sourcePath(row))) {
                    expectedPathMatches++;
                }
            }
            long proposedPathMatches = 0;
            for (String path : proposedPaths) {
                if (expectedPaths.contains(path)) {
                    proposedPathMatches++;
                }
            }
            totalExpected += wanted.size();
            totalExact += proposedExact;
            totalCited += expectedNodesCited;
            totalPathMatches += expectedPathMatches;
            totalProposed += proposed.size();
            totalProposedPathMatches += proposedPathMatches;

            ObjectNode detail = details.addObject();
            detail.put("capability_id", capabilityId);
            detail.set("run_outcome", result.get("outcome"));
            detail.put("expected_component_count", wanted.size());
            detail.put("proposed_component_count", proposed.size());
            ObjectNode pathComparison = detail.putObject("file_component_path_comparison");
            pathComparison.put("expected_component_references", wanted.size());
            pathComparison.put("expected_component_references_with_proposed_source_path", expectedPathMatches);
            pathComparison.put("proposed_component_references", proposed.size());
            pathComparison.put("proposed_component_references_on_expected_source_path", proposedPathMatches);
            ObjectNode coverage = detail.putObject("expected_graph_node_coverage");
            coverage.put("expected_graph_nodes", expectedIds.size());
            coverage.put("expected_graph_nodes_cited", expectedNodesCited);
            coverage.put("proposal_citation_scope", "PROPOSED_COMPONENTS_PLUS_EVIDENCE_REFS");
            detail.put("proposed_component_exact_graph_node_matches", proposedExact);
        }
        require(expected.keySet().equals(resultIds), "forward/gold capability sets do not match");

        ObjectNode comparison = NODE.objectNode();
        comparison.put("authority", "EVALUATOR_ONLY_COMPARISON_NOT_PRODUCT_TRUTH");
        comparison.put("capabilities_expected", expected.size());
        long mappingProposals = 0;
        long unresolved = 0;
        for (JsonNode result : requiredArray(forward.get("capability_results"), "capability_results")) {
            if ("MAPPING_PROPOSAL".equals(textOrNull(result.get("outcome")))) {
                mappingProposals++;
            }
            if ("UNRESOLVED".equals(textOrNull(result.get("outcome")))) {
                unresolved++;
            }
        }
        comparison.put("mapping_proposals", mappingProposals);
        comparison.put("unresolved", unresolved);

        ObjectNode pathComparison = comparison.putObject("file_component_path_comparison");
        pathComparison.put("granularity", "FILE_COMPONENT_REFERENCE_AT_SOURCE_PATH");
        pathComparison.put("expected_component_references", totalExpected);
        pathComparison.put("expected_component_references_with_proposed_source_path", totalPathMatches);
        pathComparison.put("expected_component_path_recall", round10((double) totalPathMatches / totalExpected));
        pathComparison.put("proposed_component_references", totalProposed);
        pathComparison.put("proposed_component_references_on_expected_source_path", totalProposedPathMatches);
        pathComparison.put("proposed_component_path_precision", round10((double) totalProposedPathMatches / totalProposed));

        ObjectNode nodeCoverage = comparison.putObject("expected_graph_node_coverage");
        nodeCoverage.put("granularity", "GRAPH_NODE_ID");
        nodeCoverage.put("expected_graph_nodes", totalExpected);
        nodeCoverage.put("expected_graph_nodes_cited", totalCited);
        nodeCoverage.put("expected_graph_node_coverage_rate", round10((double) totalCited / totalExpected));
        nodeCoverage.put("proposal_citation_scope", "PROPOSED_COMPONENTS_PLUS_EVIDENCE_REFS");

        ObjectNode exact = comparison.putObject("proposed_component_exact_graph_node_comparison");
        exact.put("expected_graph_nodes", totalExpected);
        exact.put("proposed_component_exact_graph_node_matches", totalExact);
        exact.put("proposed_component_exact_graph_node_recall", round10((double) totalExact / totalExpected));

        ArrayNode limits = comparison.putArray("comparison_limits");
        limits.add("PATH_LEVEL_OVERLAP_IS_NOT_AN_EXACT_GRAPH_NODE_MATCH");
        limits.add("EVIDENCE_CITATION_COVERAGE_IS_NOT_A_PROPOSED_COMPONENT_MATCH");
        comparison.set("by_capability", details);
        return comparison;
    }

    private String sourcePath(JsonNode component) {
        if (component.has("source_path")) {
            return textOrNull(component.get("source_path"));
        }
        String location = textOrNull(component.get("source_location"));
        int split = location.lastIndexOf(":L");
        return split >= 0 ? location.substring(0, split) : location;
    }

    // ------------------------------------------------------------------
    // Reviewer metrics
    // ------------------------------------------------------------------

    private ObjectNode armMetrics(List<String> itemIds, Map<String, Map<String, JsonNode>> rowsByReviewer) {
        List<String> sortedReviewers = new ArrayList<>(rowsByReviewer.keySet());
        java.util.Collections.sort(sortedReviewers);
        List<JsonNode> rows = new ArrayList<>();
        for (String blindId : itemIds) {
            for (String reviewer : sortedReviewers) {
                rows.add(rowsByReviewer.get(reviewer).get(blindId));
            }
        }
        List<BigInteger> combinedTimes = new ArrayList<>();
        for (String blindId : itemIds) {
            BigInteger combined = BigInteger.ZERO;
            for (String reviewer : sortedReviewers) {
                combined = combined.add(rowsByReviewer.get(reviewer).get(blindId)
                        .get("active_review_seconds").bigIntegerValue());
            }
            combinedTimes.add(combined);
        }
        List<Double> quartiles = quantilesInclusive(combinedTimes, 4);
        double q1 = quartiles.get(0);
        double q3 = quartiles.get(2);

        int unsupportedNonempty = 0;
        int unsupportedClaims = 0;
        for (JsonNode row : rows) {
            if (row.get("unsupported_claims").size() > 0) {
                unsupportedNonempty++;
            }
            unsupportedClaims += row.get("unsupported_claims").size();
        }

        ObjectNode metrics = NODE.objectNode();
        ObjectNode coverage = metrics.putObject("coverage");
        coverage.put("items_expected", itemIds.size());
        coverage.put("items_judged_by_both", itemIds.size());
        coverage.put("item_coverage", 1.0);
        coverage.put("judgments_expected", itemIds.size() * sortedReviewers.size());
        coverage.put("judgments_complete", rows.size());
        coverage.put("judgment_coverage", 1.0);

        ObjectNode actionCounts = metrics.putObject("action_counts");
        for (String action : ACTIONS) {
            long count = 0;
            for (JsonNode row : rows) {
                if (action.equals(textOrNull(row.get("review_action")))) {
                    count++;
                }
            }
            actionCounts.put(action, count);
        }
        ObjectNode outcomeCounts = metrics.putObject("outcome_counts");
        for (String outcome : OUTCOMES) {
            long count = 0;
            for (JsonNode row : rows) {
                if (outcome.equals(textOrNull(row.get("outcome")))) {
                    count++;
                }
            }
            outcomeCounts.put(outcome, count);
        }

        metrics.put("evidence_validity_mean", meanScore(rows, "evidence_validity"));
        metrics.put("usefulness_mean", meanScore(rows, "usefulness"));
        metrics.put("precision_mean", meanScore(rows, "precision"));
        metrics.put("unsupported_claim_count", unsupportedClaims);
        metrics.put("judgments_with_unsupported_claims", unsupportedNonempty);
        metrics.put("unsupported_claim_judgment_rate",
                round10((double) unsupportedNonempty / rows.size()));

        ObjectNode reviewTimes = metrics.putObject("review_time_seconds");
        BigInteger total = BigInteger.ZERO;
        List<BigInteger> perJudgment = new ArrayList<>();
        for (JsonNode row : rows) {
            BigInteger seconds = row.get("active_review_seconds").bigIntegerValue();
            total = total.add(seconds);
            perJudgment.add(seconds);
        }
        reviewTimes.put("total", NODE.numberNode(total));
        reviewTimes.put("mean_per_judgment", round10(new BigDecimal(total).doubleValue() / rows.size()));
        reviewTimes.set("median_per_judgment", medianNode(perJudgment));
        reviewTimes.set("median_combined_per_item", medianNode(combinedTimes));
        ArrayNode iqr = reviewTimes.putArray("combined_per_item_iqr");
        iqr.add(q1);
        iqr.add(q3);
        return metrics;
    }

    private double meanScore(List<JsonNode> rows, String field) {
        double sum = 0.0;
        for (JsonNode row : rows) {
            sum += row.get(field).doubleValue();
        }
        return round10(sum / rows.size());
    }

    private ObjectNode agreement(List<String> itemIds, Map<String, Map<String, JsonNode>> rowsByReviewer) {
        List<String> reviewers = new ArrayList<>(rowsByReviewer.keySet());
        java.util.Collections.sort(reviewers);
        if (reviewers.size() != 2) {
            throw new IllegalStateException("expected exactly two reviewer workspaces");
        }
        Map<String, JsonNode> first = rowsByReviewer.get(reviewers.get(0));
        Map<String, JsonNode> second = rowsByReviewer.get(reviewers.get(1));
        List<String> actionDisagreements = new ArrayList<>();
        List<String> outcomeDisagreements = new ArrayList<>();
        List<String> exact = new ArrayList<>();
        for (String blindId : itemIds) {
            JsonNode firstRow = first.get(blindId);
            JsonNode secondRow = second.get(blindId);
            boolean sameAction = java.util.Objects.equals(
                    textOrNull(firstRow.get("review_action")), textOrNull(secondRow.get("review_action")));
            boolean sameOutcome = java.util.Objects.equals(
                    textOrNull(firstRow.get("outcome")), textOrNull(secondRow.get("outcome")));
            if (!sameAction) {
                actionDisagreements.add(blindId);
            }
            if (!sameOutcome) {
                outcomeDisagreements.add(blindId);
            }
            if (sameAction && sameOutcome) {
                exact.add(blindId);
            }
        }
        int total = itemIds.size();
        ObjectNode agreement = NODE.objectNode();
        ArrayNode reviewersNode = agreement.putArray("reviewers");
        reviewers.forEach(reviewersNode::add);
        agreement.put("item_count", total);
        agreement.put("action_agreement_count", total - actionDisagreements.size());
        agreement.put("action_agreement_rate",
                round10((double) (total - actionDisagreements.size()) / total));
        ArrayNode actionIds = agreement.putArray("action_disagreement_ids");
        actionDisagreements.forEach(actionIds::add);
        agreement.put("outcome_agreement_count", total - outcomeDisagreements.size());
        agreement.put("outcome_agreement_rate",
                round10((double) (total - outcomeDisagreements.size()) / total));
        ArrayNode outcomeIds = agreement.putArray("outcome_disagreement_ids");
        outcomeDisagreements.forEach(outcomeIds::add);
        agreement.put("exact_action_and_outcome_agreement_count", exact.size());
        agreement.put("exact_action_and_outcome_agreement_rate", round10((double) exact.size() / total));
        ArrayNode exactIds = agreement.putArray("exact_action_and_outcome_agreement_ids");
        exact.forEach(exactIds::add);
        return agreement;
    }

    /** Ports {@code statistics.median}: the middle element, or the halved mean of the two middle elements. */
    private JsonNode medianNode(List<BigInteger> values) {
        List<BigInteger> sorted = new ArrayList<>(values);
        sorted.sort(BigInteger::compareTo);
        int size = sorted.size();
        if (size % 2 == 1) {
            return NODE.numberNode(sorted.get(size / 2));
        }
        BigInteger combined = sorted.get(size / 2 - 1).add(sorted.get(size / 2));
        return NODE.numberNode(new BigDecimal(combined).doubleValue() / 2);
    }

    /** Ports {@code statistics.quantiles(..., method="inclusive")} over integer samples. */
    private List<Double> quantilesInclusive(List<BigInteger> data, int n) {
        List<BigInteger> sorted = new ArrayList<>(data);
        sorted.sort(BigInteger::compareTo);
        int m = sorted.size() - 1;
        List<Double> result = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            int j = (i * m) / n;
            int delta = (i * m) % n;
            BigInteger interpolated = sorted.get(j)
                    .multiply(BigInteger.valueOf(n - delta))
                    .add(sorted.get(j + 1).multiply(BigInteger.valueOf(delta)));
            result.add(new BigDecimal(interpolated).doubleValue() / n);
        }
        return result;
    }

    /** Ports Python {@code round(value, 10)} on the exact binary value, ties-to-even. */
    static double round10(double value) {
        if (!Double.isFinite(value)) {
            return value;
        }
        return new BigDecimal(value).setScale(10, RoundingMode.HALF_EVEN).doubleValue();
    }

    // ------------------------------------------------------------------
    // Stop report
    // ------------------------------------------------------------------

    private ObjectNode stopReport(String stage, EvaluationException error) {
        ObjectNode report = NODE.objectNode();
        report.put("schema_version", "pkb001.task7.stop-report.v1");
        report.put("report_id", "pkb001-task7-petclinic-818c413-stop-v1");
        report.put("decision", "STOP");
        report.put("decision_scope", "BOUNDED_PROTOTYPE_DECISION_NO_SEMANTIC_PUBLICATION");
        report.put("failure_stage", stage);
        ObjectNode reason = report.putArray("stop_reasons").addObject();
        reason.put("code", stage + "_FAILED");
        reason.put("detail", error.getMessage());
        ObjectNode pre = report.putObject("pre_unblinding_validation");
        pre.put("passed", false);
        pre.put("validated_before_sealed_key_read", true);
        ObjectNode integrity = report.putObject("integrity");
        integrity.put("status", "FAILED");
        integrity.putArray("hard_stop_violations").add(stage + "_FAILED");
        report.put("unblinding_performed", false);
        report.put("metrics_computed", false);
        report.put("semantic_publication_allowed", false);
        ObjectNode humanReview = report.putObject("human_product_team_review");
        humanReview.put("status", "PENDING");
        humanReview.put("semantic_publication_allowed", false);
        humanReview.put("authority", "PRODUCT_TEAM_ONLY");
        report.put("documented_exit_code", STOP_EXIT_CODE);
        return report;
    }

    // ------------------------------------------------------------------
    // Shared helpers
    // ------------------------------------------------------------------

    private static Path resolveRoot(Path root) {
        try {
            return root.toRealPath();
        } catch (IOException | RuntimeException failure) {
            return root.toAbsolutePath().normalize();
        }
    }

    private static JsonNode loadJson(Path path) {
        final byte[] data;
        try {
            data = Files.readAllBytes(path);
        } catch (IOException failure) {
            throw new EvaluationException(
                    "cannot read JSON input " + path + ": " + pythonOSError(path, failure));
        }
        try {
            return Task7Json.readTree(data);
        } catch (IllegalArgumentException failure) {
            throw new EvaluationException("cannot read JSON input " + path + ": " + failure.getMessage());
        }
    }

    static String sha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(path));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                hex.append(Character.forDigit((value >> 4) & 0xf, 16));
                hex.append(Character.forDigit(value & 0xf, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        } catch (IOException failure) {
            throw new EvaluationException(
                    "cannot read digest input " + path + ": " + pythonOSError(path, failure));
        }
    }

    /** Mirrors CPython {@code str(OSError)} for the errno cases the consumer can surface. */
    private static String pythonOSError(Path path, IOException failure) {
        if (failure instanceof NoSuchFileException) {
            return "[Errno 2] No such file or directory: '" + path + "'";
        }
        if (failure instanceof AccessDeniedException) {
            return "[Errno 13] Permission denied: '" + path + "'";
        }
        if (failure instanceof NotDirectoryException) {
            return "[Errno 20] Not a directory: '" + path + "'";
        }
        return failure.getMessage() == null ? failure.toString() : failure.getMessage();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new EvaluationException(message);
        }
    }

    private static boolean isFalse(JsonNode value) {
        return value != null && value.isBoolean() && !value.asBoolean();
    }

    private static String textOrNull(JsonNode value) {
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private static List<String> textList(JsonNode array) {
        List<String> values = new ArrayList<>();
        if (array != null && array.isArray()) {
            for (JsonNode item : array) {
                values.add(textOrNull(item));
            }
        }
        return values;
    }

    private static Set<String> union(List<String> left, List<String> right) {
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        return union;
    }

    private static int intersectionSize(Set<String> left, Set<String> right) {
        int count = 0;
        for (String value : left) {
            if (right.contains(value)) {
                count++;
            }
        }
        return count;
    }

    private static ArrayNode requiredArray(JsonNode value, String field) {
        if (value == null || !value.isArray()) {
            throw new IllegalStateException("expected array field " + field);
        }
        return (ArrayNode) value;
    }
}

package com.featuredeliveryintelligence.fdi.validation.blindevaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ports the {@code evaluate}-corpus of {@code tests/test_pkb001_phase0.py} to
 * the Java {@link BlindEvaluation} evaluator, including the Wilson interval,
 * adjudication vocabulary, duplicate collapse, gold-match forcing, and the
 * decision thresholds.
 */
class BlindEvaluationTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path REPOSITORY = Path.of("").toAbsolutePath();

    @Test
    void emptyOutputCannotPass() {
        ObjectNode report = evaluate(JSON.createArrayNode(), JSON.createArrayNode(), 30, 10, null);

        assertFalse(report.get("minimum_sample_satisfied").asBoolean());
        assertEquals("REVISE", report.get("decision").asText());
        assertEquals(0, report.get("arm_metrics").size());
        assertEquals(0, report.get("hard_gate_failures").size());
    }

    @Test
    void leakageForcesStop() {
        ArrayNode items = proposals(30, "R3");
        ObjectNode report = evaluate(items, judgmentsFor(items, repeat("SUPPORTED", 30)),
                30, 10, List.of("GROUND_TRUTH_ACCESS"));

        assertEquals("STOP", report.get("decision").asText());
        assertEquals(List.of("GROUND_TRUTH_ACCESS"),
                texts(report.get("hard_gate_failures")));
    }

    @Test
    void reverseThresholdsAndWilsonIntervalAreReproducible() {
        ArrayNode items = proposals(30, "R3");
        List<String> outcomes = new java.util.ArrayList<>(repeat("SUPPORTED", 21));
        outcomes.addAll(repeat("UNSUPPORTED", 3));
        outcomes.addAll(repeat("PARTIALLY_SUPPORTED", 6));
        ObjectNode report = evaluate(items, judgmentsFor(items, outcomes), 30, 10, null);
        ObjectNode metrics = (ObjectNode) report.get("arm_metrics").get(0);

        assertEquals(0.7, metrics.get("useful_rate").asDouble(), 1e-12);
        assertEquals(0.1, metrics.get("unsupported_rate").asDouble(), 1e-12);
        assertEquals(30, metrics.get("proposal_count").asInt());
        assertEquals(10, metrics.get("gold_item_count").asInt());
        assertEquals(21, metrics.get("supported_count").asInt());
        assertEquals(6, metrics.get("partially_supported_count").asInt());
        assertEquals(3, metrics.get("unsupported_count").asInt());
        double[] wilson = BlindEvaluation.wilsonInterval(21, 30);
        assertEquals(wilson[0], metrics.get("wilson_low").asDouble(), 0.0);
        assertEquals(wilson[1], metrics.get("wilson_high").asDouble(), 0.0);
        assertEquals("CONTINUE", report.get("decision").asText());
        assertEquals(60, metrics.get("median_review_seconds").asInt());
    }

    @Test
    void f1RequiresPerfectEvidenceAndZeroUnsupportedRelations() {
        ArrayNode items = proposals(30, "F1");
        ObjectNode supported = evaluate(items, judgmentsFor(items, repeat("SUPPORTED", 30)), 30, 10, null);
        assertEquals("CONTINUE", supported.get("decision").asText());

        ArrayNode invalid = judgmentsFor(items, repeat("SUPPORTED", 30));
        ((ObjectNode) invalid.get(0)).put("evidence_valid", false);
        ObjectNode report = evaluate(items, invalid, 30, 10, null);
        assertEquals("REVISE", report.get("decision").asText());
        assertEquals(30, report.get("arm_metrics").get(0).get("supported_count").asInt());
        assertEquals(29.0 / 30.0,
                report.get("arm_metrics").get(0).get("evidence_validity").asDouble(), 1e-12);
    }

    @Test
    void duplicateProposalsCollapseWithLeastFavorableJudgment() throws Exception {
        ArrayNode items = proposals(1, "R1");
        ObjectNode duplicate = (ObjectNode) JSON.readTree(items.get(0).toString());
        duplicate.put("proposal_id", "R1-duplicate");
        ArrayNode all = JSON.createArrayNode();
        all.add(items.get(0));
        all.add(duplicate);
        ArrayNode rows = JSON.createArrayNode();
        rows.addAll(judgmentsFor(items, List.of("SUPPORTED")));
        rows.addAll(judgmentsFor(JSON.createArrayNode().add(duplicate), List.of("UNSUPPORTED")));

        ObjectNode report = evaluate(all, rows, 1, 1, null);
        ObjectNode metrics = (ObjectNode) report.get("arm_metrics").get(0);

        assertEquals(1, metrics.get("proposal_count").asInt());
        assertEquals(1, metrics.get("unsupported_count").asInt());
        assertEquals(0, metrics.get("supported_count").asInt());
    }

    @Test
    void mergeRequiresAllDeclaredGoldMatches() {
        ArrayNode items = proposals(1, "R2");
        ObjectNode item = (ObjectNode) items.get(0);
        item.put("operation", "MERGE");
        item.set("gold_ids", texts(List.of("gold-a", "gold-b")));
        item.set("matched_gold_ids", texts(List.of("gold-a")));

        ObjectNode report = evaluate(items, judgmentsFor(items, List.of("SUPPORTED")), 1, 1, null);

        assertEquals(1, report.get("arm_metrics").get(0).get("unsupported_count").asInt());
    }

    @Test
    void decisionSchemaMatchesEvaluatorMetricSurface() throws Exception {
        JsonNode schema = JSON.readTree(Files.readAllBytes(REPOSITORY.resolve(
                "validation/pkb001/schemas/pkb-decision-report-v0.1.schema.json")));
        Set<String> required = new java.util.HashSet<>();
        for (JsonNode name : schema.get("$defs").get("metrics").get("required")) {
            required.add(name.asText());
        }
        assertEquals(Set.of("arm", "proposal_count", "gold_item_count", "supported_count",
                "partially_supported_count", "unsupported_count", "useful_rate",
                "unsupported_rate", "precision", "evidence_validity", "wilson_low",
                "wilson_high", "median_review_seconds"), required);
    }

    @Test
    void buildDecisionReportRequiresExplicitGroundTruthDigest() {
        ObjectNode evaluation = evaluate(JSON.createArrayNode(), JSON.createArrayNode(), 1, 1, null);
        ObjectNode report = new BlindEvaluation().buildDecisionReport(
                "report-1", "a".repeat(64), evaluation);

        assertEquals("report-1", report.get("report_id").asText());
        assertEquals("a".repeat(64), report.get("ground_truth_sha256").asText());
        assertEquals(Set.of("report_id", "ground_truth_sha256", "minimum_sample_satisfied",
                        "hard_gate_failures", "arm_metrics", "decision", "claim_boundary"),
                fieldSet(report));
        assertThrows(IllegalArgumentException.class,
                () -> new BlindEvaluation().buildDecisionReport("report-2", "unknown", evaluation));
        assertThrows(IllegalArgumentException.class,
                () -> new BlindEvaluation().buildDecisionReport("", "a".repeat(64), evaluation));
        assertThrows(IllegalArgumentException.class,
                () -> new BlindEvaluation().buildDecisionReport("r", "A".repeat(64), evaluation));
    }

    @Test
    void buildDecisionReportRejectsWrongEvaluationShape() {
        ObjectNode evaluation = JSON.createObjectNode();
        evaluation.put("decision", "REVISE");
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> new BlindEvaluation().buildDecisionReport("r", "a".repeat(64), evaluation));
        assertEquals("evaluation result has an invalid shape", failure.getMessage());
    }

    @Test
    void wilsonIntervalRejectsInvalidCounts() {
        assertThrows(IllegalArgumentException.class, () -> BlindEvaluation.wilsonInterval(-1, 5));
        assertThrows(IllegalArgumentException.class, () -> BlindEvaluation.wilsonInterval(6, 5));
        assertThrows(IllegalArgumentException.class, () -> BlindEvaluation.wilsonInterval(1, -5));
        double[] zero = BlindEvaluation.wilsonInterval(0, 0);
        assertEquals(0.0, zero[0], 0.0);
        assertEquals(0.0, zero[1], 0.0);
        double[] full = BlindEvaluation.wilsonInterval(5, 5);
        assertEquals(BlindEvaluation.wilsonInterval(5, 5)[0], full[0], 0.0);
        assertEquals(1.0, full[1], 1e-12);
    }

    @Test
    void minimumSampleBoundsMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> evaluate(JSON.createArrayNode(), JSON.createArrayNode(), 0, 10, null));
        assertThrows(IllegalArgumentException.class,
                () -> evaluate(JSON.createArrayNode(), JSON.createArrayNode(), 30, 0, null));
    }

    @Test
    void adjudicationRequiresTwoIndependentJudgments() {
        ArrayNode items = proposals(1, "R1");
        ArrayNode rows = JSON.createArrayNode();
        ObjectNode row = baseJudgment(items.get(0).get("proposal_id").asText(), "reviewer-a");
        row.put("outcome", "SUPPORTED");
        rows.add(row);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> evaluate(items, rows, 1, 1, null));
        assertEquals("two independent judgments are required for R1-0", failure.getMessage());
    }

    @Test
    void adjudicationRejectsInvalidOutcome() {
        ArrayNode items = proposals(1, "R1");
        ArrayNode rows = judgmentsFor(items, List.of("SUPPORTED"));
        ((ObjectNode) rows.get(0)).put("outcome", "MAYBE");

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> evaluate(items, rows, 1, 1, null));
        assertEquals("invalid judgment outcome for R1-0", failure.getMessage());
    }

    @Test
    void adjudicationRequiresThirdReviewerForDisagreement() {
        ArrayNode items = proposals(1, "R1");
        ArrayNode rows = judgmentsFor(items, List.of("SUPPORTED"));
        ((ObjectNode) rows.get(1)).put("outcome", "UNSUPPORTED");

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> evaluate(items, rows, 1, 1, null));
        assertEquals("third reviewer is required for disagreement on R1-0", failure.getMessage());
    }

    @Test
    void disagreementResolvesToThirdReviewerOutcome() {
        ArrayNode items = proposals(1, "R1");
        ArrayNode rows = judgmentsFor(items, List.of("SUPPORTED"));
        ((ObjectNode) rows.get(1)).put("outcome", "UNSUPPORTED");
        ObjectNode third = baseJudgment(items.get(0).get("proposal_id").asText(), "reviewer-c");
        third.put("outcome", "SUPPORTED");
        rows.add(third);

        ObjectNode report = evaluate(items, rows, 1, 1, null);
        assertEquals(1, report.get("arm_metrics").get(0).get("supported_count").asInt());
    }

    @Test
    void proposalKeyValidationVocabulary() {
        ArrayNode items = proposals(1, "R1");
        ((ObjectNode) items.get(0)).remove("arm");
        assertThrows(IllegalArgumentException.class, () -> evaluate(items, judgmentsFor(items,
                List.of("SUPPORTED", "SUPPORTED")), 1, 1, null));

        ArrayNode badArm = proposals(1, "R1");
        ((ObjectNode) badArm.get(0)).put("arm", "R9");
        IllegalArgumentException arm = assertThrows(IllegalArgumentException.class,
                () -> evaluate(badArm, judgmentsFor(badArm, List.of("SUPPORTED", "SUPPORTED")), 1, 1, null));
        assertEquals("invalid proposal arm", arm.getMessage());

        ArrayNode badOperation = proposals(1, "R1");
        ((ObjectNode) badOperation.get(0)).put("operation", "DELETE");
        IllegalArgumentException operation = assertThrows(IllegalArgumentException.class,
                () -> evaluate(badOperation, judgmentsFor(badOperation,
                        List.of("SUPPORTED", "SUPPORTED")), 1, 1, null));
        assertEquals("invalid proposal operation", operation.getMessage());
    }

    @Test
    void missingRequiredProposalFieldNamesTheGap() {
        ArrayNode items = proposals(1, "R1");
        ((ObjectNode) items.get(0)).remove("gold_ids");
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> evaluate(items, judgmentsFor(items, List.of("SUPPORTED", "SUPPORTED")), 1, 1, null));
        assertEquals("proposal is missing required fields", failure.getMessage());
    }

    @Test
    void hostileNonArrayGoldIdsRaiseIllegalState() {
        ArrayNode items = proposals(1, "R1");
        ((ObjectNode) items.get(0)).put("gold_ids", "gold-0");
        assertThrows(IllegalStateException.class, () -> evaluate(items,
                judgmentsFor(items, List.of("SUPPORTED", "SUPPORTED")), 1, 1, null));
    }

    @Test
    void armMetricsFieldOrderMatchesDecisionSchema() {
        ArrayNode items = proposals(2, "R3");
        ObjectNode report = evaluate(items, judgmentsFor(items, List.of("SUPPORTED", "SUPPORTED")), 1, 1, null);
        ObjectNode metrics = (ObjectNode) report.get("arm_metrics").get(0);
        assertEquals(List.of("arm", "proposal_count", "gold_item_count", "supported_count",
                        "partially_supported_count", "unsupported_count", "useful_rate",
                        "unsupported_rate", "precision", "evidence_validity", "wilson_low",
                        "wilson_high", "median_review_seconds"), fieldNames(metrics));
        assertEquals(List.of("minimum_sample_satisfied", "hard_gate_failures", "arm_metrics",
                "decision", "claim_boundary"), fieldNames(report));
        assertEquals("CALIBRATION_ONLY_NOT_PRODUCTION_EVIDENCE",
                report.get("claim_boundary").asText());
    }

    // --- fixture builders mirroring tests/test_pkb001_phase0.py ---

    static ArrayNode proposals(int count, String arm) {
        ArrayNode items = JSON.createArrayNode();
        for (int index = 0; index < count; index++) {
            ObjectNode proposal = JSON.createObjectNode();
            proposal.put("proposal_id", arm + "-" + index);
            proposal.put("arm", arm);
            proposal.put("target_id", "target-" + index);
            proposal.put("relation_type", "REALIZES");
            proposal.put("operation", "CREATE");
            proposal.set("gold_ids", texts(List.of("gold-" + index % 10)));
            proposal.set("matched_gold_ids", texts(List.of("gold-" + index % 10)));
            items.add(proposal);
        }
        return items;
    }

    static ArrayNode judgmentsFor(ArrayNode items, List<String> outcomes) {
        ArrayNode rows = JSON.createArrayNode();
        for (int index = 0; index < items.size(); index++) {
            for (String reviewer : new String[] {"reviewer-a", "reviewer-b"}) {
                ObjectNode row = baseJudgment(items.get(index).get("proposal_id").asText(), reviewer);
                row.put("outcome", outcomes.get(index));
                rows.add(row);
            }
        }
        return rows;
    }

    static ObjectNode baseJudgment(String proposalId, String reviewer) {
        ObjectNode row = JSON.createObjectNode();
        row.put("proposal_id", proposalId);
        row.put("reviewer_id", reviewer);
        row.put("review_action", "ACCEPT");
        row.put("evidence_valid", true);
        row.put("active_review_seconds", 30);
        return row;
    }

    static ObjectNode evaluate(ArrayNode proposals, ArrayNode judgments,
            int minimumProposals, int minimumGold, List<String> hardFailures) {
        return new BlindEvaluation().evaluate(proposals, judgments,
                minimumProposals, minimumGold, hardFailures);
    }

    static ArrayNode texts(List<String> values) {
        ArrayNode node = JSON.createArrayNode();
        values.forEach(node::add);
        return node;
    }

    static List<String> repeat(String value, int count) {
        return java.util.Collections.nCopies(count, value);
    }

    static List<String> texts(JsonNode array) {
        List<String> values = new java.util.ArrayList<>();
        array.forEach(item -> values.add(item.asText()));
        return values;
    }

    static List<String> fieldNames(ObjectNode node) {
        List<String> names = new java.util.ArrayList<>();
        Iterator<String> iterator = node.fieldNames();
        while (iterator.hasNext()) {
            names.add(iterator.next());
        }
        return names;
    }

    static Set<String> fieldSet(ObjectNode node) {
        return fieldNames(node).stream().collect(Collectors.toSet());
    }
}

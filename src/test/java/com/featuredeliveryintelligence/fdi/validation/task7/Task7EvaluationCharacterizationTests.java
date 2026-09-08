package com.featuredeliveryintelligence.fdi.validation.task7;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ports the collected characterization cases of
 * {@code tests/test_pkb001_task7_evaluation.py} to the Java {@link Task7Evaluation}
 * API: deterministic bounded metrics on the sealed repository root, exact
 * agreement and pending third review, forward/gold comparison transparency,
 * pre-unblinding and bound-input fail-closed stops, digest-consistent invalid
 * key accounting, and the bounded-decision boundary. Byte-level rendering
 * parity is pinned against the committed immutable report artifacts and the
 * exact stop-report bytes produced by the frozen Python consumer.
 */
class Task7EvaluationCharacterizationTests {
    private static final double TOLERANCE = 1e-12;

    @TempDir Path temp;

    @Test
    void committedReportAndPendingPacketBytesAreReproducedExactly() throws Exception {
        Task7Evaluation evaluation = new Task7Evaluation();
        byte[] committedReport = Files.readAllBytes(
                Task7EvaluationRoots.REPOSITORY.resolve(Task7EvaluationRoots.REPORT_RELATIVE));
        byte[] committedPending = Files.readAllBytes(
                Task7EvaluationRoots.REPOSITORY.resolve(Task7EvaluationRoots.PENDING_RELATIVE));

        byte[] fresh = evaluation.toReportBytes(evaluation.evaluateRepository(
                Task7EvaluationRoots.REPOSITORY));
        assertArrayEquals(committedReport, fresh,
                "Java evaluation must render byte-identically to the committed Python report");

        JsonNode persistedReport = Task7Json.readTree(committedReport);
        byte[] pending = evaluation.toReportBytes(evaluation.buildThirdReviewPacket(persistedReport));
        assertArrayEquals(committedPending, pending,
                "Java third-review packet must render byte-identically to the committed artifact");
    }

    @Test
    void reportIsDeterministicBoundedAndComplete() throws Exception {
        Task7Evaluation evaluation = new Task7Evaluation();
        JsonNode first = evaluation.evaluateRepository(Task7EvaluationRoots.REPOSITORY);
        JsonNode second = evaluation.evaluateRepository(Task7EvaluationRoots.REPOSITORY);
        assertArrayEquals(evaluation.toReportBytes(first), evaluation.toReportBytes(second));

        assertEquals("REVISE", first.get("decision").asText());
        assertTrue(first.get("pre_unblinding_validation").get("passed").asBoolean());
        assertFalse(first.get("thresholds").get("preregistered_before_execution").asBoolean());
        assertFalse(first.get("thresholds").get("observed_metrics_used_as_acceptance_thresholds").asBoolean());
        assertEquals("PENDING", first.get("human_product_team_review").get("status").asText());
        assertFalse(first.get("human_product_team_review").get("semantic_publication_allowed").asBoolean());

        JsonNode overall = first.get("metrics").get("overall");
        JsonNode coverage = overall.get("coverage");
        assertEquals(15, coverage.get("items_expected").asLong());
        assertEquals(15, coverage.get("items_judged_by_both").asLong());
        assertEquals(1.0, coverage.get("item_coverage").asDouble(), TOLERANCE);
        assertEquals(30, coverage.get("judgments_expected").asLong());
        assertEquals(30, coverage.get("judgments_complete").asLong());
        assertEquals(1.0, coverage.get("judgment_coverage").asDouble(), TOLERANCE);

        JsonNode actions = overall.get("action_counts");
        assertEquals(17, actions.get("ACCEPT").asLong());
        assertEquals(2, actions.get("ADD_MISSING").asLong());
        assertEquals(2, actions.get("MERGE").asLong());
        assertEquals(5, actions.get("RENAME").asLong());
        assertEquals(0, actions.get("REJECT").asLong());
        assertEquals(4, actions.get("SPLIT").asLong());
        JsonNode outcomes = overall.get("outcome_counts");
        assertEquals(2, outcomes.get("DUPLICATE").asLong());
        assertEquals(20, outcomes.get("PARTIALLY_SUPPORTED").asLong());
        assertEquals(8, outcomes.get("SUPPORTED").asLong());
        assertEquals(0, outcomes.get("UNSUPPORTED").asLong());

        assertEquals(0.8946666667, overall.get("evidence_validity_mean").asDouble(), TOLERANCE);
        assertEquals(0.8793333333, overall.get("usefulness_mean").asDouble(), TOLERANCE);
        assertEquals(0.8383333333, overall.get("precision_mean").asDouble(), TOLERANCE);
        assertEquals(0.9666666667, overall.get("unsupported_claim_judgment_rate").asDouble(), TOLERANCE);

        JsonNode reviewTimes = overall.get("review_time_seconds");
        assertEquals(1358, reviewTimes.get("total").asLong());
        assertEquals(80, reviewTimes.get("median_combined_per_item").asLong());
        assertEquals(45.2666666667, reviewTimes.get("mean_per_judgment").asDouble(), TOLERANCE);
        assertTrue(reviewTimes.get("median_per_judgment").isFloatingPointNumber());
        assertEquals(78.5, reviewTimes.get("combined_per_item_iqr").get(0).asDouble(), TOLERANCE);
        assertEquals(102.5, reviewTimes.get("combined_per_item_iqr").get(1).asDouble(), TOLERANCE);
    }

    @Test
    void reportsExactAgreementAndPendingThirdReview() throws Exception {
        JsonNode report = new Task7Evaluation().evaluateRepository(Task7EvaluationRoots.REPOSITORY);
        JsonNode agreement = report.get("reviewer_agreement");

        assertEquals(List.of("reviewer-01", "reviewer-02"),
                textList(agreement.get("reviewers")));
        assertEquals(12, agreement.get("action_agreement_count").asLong());
        assertEquals(7, agreement.get("outcome_agreement_count").asLong());
        assertEquals(4, agreement.get("exact_action_and_outcome_agreement_count").asLong());
        assertEquals(List.of("BR-007", "BR-011", "BR-015"),
                textList(agreement.get("action_disagreement_ids")));
        assertEquals(List.of("BR-002", "BR-004", "BR-005", "BR-006",
                        "BR-008", "BR-012", "BR-013", "BR-014"),
                textList(agreement.get("outcome_disagreement_ids")));

        Task7Evaluation evaluation = new Task7Evaluation();
        JsonNode pending = evaluation.buildThirdReviewPacket(report);
        assertEquals("PENDING_INDEPENDENT_THIRD_REVIEW", pending.get("status").asText());
        assertEquals(11, pending.get("item_count").asLong());
        assertEquals(List.of("BR-002", "BR-004", "BR-005", "BR-006", "BR-007", "BR-008",
                        "BR-011", "BR-012", "BR-013", "BR-014", "BR-015"),
                textList(pending.get("items"), "blind_id"));
        for (JsonNode item : pending.get("items")) {
            assertFalse(item.has("third_judgment"));
        }
    }

    @Test
    void forwardGoldComparisonAndReverseResultsAreTransparent() throws Exception {
        JsonNode report = new Task7Evaluation().evaluateRepository(Task7EvaluationRoots.REPOSITORY);
        JsonNode forward = report.get("forward_expected_realization_comparison");

        assertEquals("EVALUATOR_ONLY_COMPARISON_NOT_PRODUCT_TRUTH", forward.get("authority").asText());
        assertEquals(10, forward.get("capabilities_expected").asLong());
        assertEquals(9, forward.get("mapping_proposals").asLong());
        assertEquals(1, forward.get("unresolved").asLong());

        JsonNode pathMetrics = forward.get("file_component_path_comparison");
        assertEquals(24, pathMetrics.get("expected_component_references").asLong());
        assertEquals(23, pathMetrics.get("expected_component_references_with_proposed_source_path").asLong());
        assertEquals(0.9583333333, pathMetrics.get("expected_component_path_recall").asDouble(), TOLERANCE);
        assertEquals(25, pathMetrics.get("proposed_component_references").asLong());
        assertEquals(21, pathMetrics.get("proposed_component_references_on_expected_source_path").asLong());
        assertEquals(0.84, pathMetrics.get("proposed_component_path_precision").asDouble(), TOLERANCE);
        assertEquals("FILE_COMPONENT_REFERENCE_AT_SOURCE_PATH", pathMetrics.get("granularity").asText());

        JsonNode nodeCoverage = forward.get("expected_graph_node_coverage");
        assertEquals(24, nodeCoverage.get("expected_graph_nodes").asLong());
        assertEquals(17, nodeCoverage.get("expected_graph_nodes_cited").asLong());
        assertEquals(0.7083333333, nodeCoverage.get("expected_graph_node_coverage_rate").asDouble(), TOLERANCE);
        assertEquals("PROPOSED_COMPONENTS_PLUS_EVIDENCE_REFS", nodeCoverage.get("proposal_citation_scope").asText());
        assertEquals("GRAPH_NODE_ID", nodeCoverage.get("granularity").asText());

        JsonNode exact = forward.get("proposed_component_exact_graph_node_comparison");
        assertEquals(24, exact.get("expected_graph_nodes").asLong());
        assertEquals(0, exact.get("proposed_component_exact_graph_node_matches").asLong());
        assertEquals(0.0, exact.get("proposed_component_exact_graph_node_recall").asDouble(), TOLERANCE);
        assertEquals(List.of("PATH_LEVEL_OVERLAP_IS_NOT_AN_EXACT_GRAPH_NODE_MATCH",
                        "EVIDENCE_CITATION_COVERAGE_IS_NOT_A_PROPOSED_COMPONENT_MATCH"),
                textList(forward.get("comparison_limits")));

        JsonNode reverseResults = report.get("reverse_proposal_review_results");
        assertEquals(5, reverseResults.size());
        assertEquals(Set.of("PKS2-HYP-001", "PKS2-HYP-002", "PKS2-HYP-003",
                        "PKS2-HYP-004", "PKS2-HYP-005"),
                new java.util.HashSet<>(textList(reverseResults, "source_identifier")));
        for (JsonNode item : reverseResults) {
            assertEquals(2, item.get("reviewers").size());
        }
    }

    @Test
    void validatesJudgmentsBeforeUnblindingAndStopsByteIdentically() throws Exception {
        Path root = Task7EvaluationRoots.copyEvaluationRoot(temp.resolve("incomplete"));
        Task7EvaluationRoots.removeLastJudgment(root);
        Files.writeString(root.resolve(Task7EvaluationRoots.TASK6).resolve("sealed-blind-key.json"),
                "not valid JSON");

        Task7Evaluation evaluation = new Task7Evaluation();
        JsonNode report = evaluation.evaluateRepository(root);

        assertArrayEquals(INCOMPLETE_JUDGMENTS_STOP.getBytes(StandardCharsets.UTF_8),
                evaluation.toReportBytes(report));
        assertEquals("STOP", report.get("decision").asText());
        assertEquals("PRE_UNBLINDING_VALIDATION", report.get("failure_stage").asText());
        assertFalse(report.get("unblinding_performed").asBoolean());
        assertFalse(report.get("metrics_computed").asBoolean());
        assertFalse(report.has("metrics"));
    }

    @Test
    void rejectsBoundInputMutationsWithoutUnblindingOrMetrics() throws Exception {
        String[][] mutations = {
                {"packet", "validation/pkb001/task6-blind-review/blind-review-packet.json",
                        "PRE_UNBLINDING_VALIDATION", "digest"},
                {"reviewer_packet",
                        "validation/pkb001/task6-blind-review/judgment-workspaces/reviewer-01/packet-input.json",
                        "PRE_UNBLINDING_VALIDATION",
                        "validation/pkb001/task6-blind-review/judgment-workspaces/reviewer-01/packet-input.json digest does not match the sealed packet"},
                {"gold", "validation/pkb001/evaluator/petclinic-818c413/gold-mappings.json",
                        "BOUND_INPUT_VALIDATION", "evaluator gold digest does not match ground-truth seal"},
                {"forward_run",
                        "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json",
                        "BOUND_INPUT_VALIDATION",
                        "source run digest mismatch: validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json"},
        };
        for (String[] mutation : mutations) {
            Path root = Task7EvaluationRoots.copyEvaluationRoot(temp.resolve(mutation[0]));
            Task7EvaluationRoots.appendByte(root.resolve(mutation[1]));

            Task7Evaluation evaluation = new Task7Evaluation();
            JsonNode report = evaluation.evaluateRepository(root);

            assertEquals("STOP", report.get("decision").asText(), mutation[0]);
            assertEquals(mutation[2], report.get("failure_stage").asText(), mutation[0]);
            assertFalse(report.get("unblinding_performed").asBoolean(), mutation[0]);
            assertFalse(report.get("metrics_computed").asBoolean(), mutation[0]);
            assertTrue(report.get("stop_reasons").get(0).get("detail").asText()
                    .contains(mutation[3]), mutation[0]);
            assertFalse(report.has("metrics"), mutation[0]);
        }

        // Byte-level parity for the reviewer packet-input mutation stop report.
        Path reviewerRoot = Task7EvaluationRoots.copyEvaluationRoot(temp.resolve("reviewer-bytes"));
        Task7EvaluationRoots.appendByte(reviewerRoot.resolve(
                "validation/pkb001/task6-blind-review/judgment-workspaces/reviewer-01/packet-input.json"));
        Task7Evaluation evaluation = new Task7Evaluation();
        assertArrayEquals(REVIEWER_PACKET_STOP.getBytes(StandardCharsets.UTF_8),
                evaluation.toReportBytes(evaluation.evaluateRepository(reviewerRoot)));
    }

    @Test
    void stopsForDigestConsistentInvalidKeyAccountingByteIdentically() throws Exception {
        Path root = Task7EvaluationRoots.copyEvaluationRoot(temp.resolve("invalid-key"));
        Task7EvaluationRoots.emptySealedKeyAndRepairManifest(root);

        Task7Evaluation evaluation = new Task7Evaluation();
        JsonNode report = evaluation.evaluateRepository(root);

        assertArrayEquals(INVALID_KEY_STOP.getBytes(StandardCharsets.UTF_8),
                evaluation.toReportBytes(report));
        assertEquals("STOP", report.get("decision").asText());
        assertEquals("POST_BINDING_INTEGRITY_VALIDATION", report.get("failure_stage").asText());
        assertFalse(report.get("unblinding_performed").asBoolean());
        assertFalse(report.get("metrics_computed").asBoolean());
        assertFalse(report.has("metrics"));
    }

    @Test
    void boundedDecisionNeverBackfitsObservedMetrics() {
        assertEquals("REVISE", Task7Evaluation.boundedDecision(
                true, false, null, false));
        assertEquals("STOP", Task7Evaluation.boundedDecision(
                false, false, null, false));
        assertEquals("GO", Task7Evaluation.boundedDecision(
                true, true, true, true));
        assertEquals("REVISE", Task7Evaluation.boundedDecision(
                true, true, true, false));
        assertEquals("REVISE", Task7Evaluation.boundedDecision(
                true, true, false, true));
    }

    private static List<String> textList(JsonNode array) {
        return textList(array, null);
    }

    private static List<String> textList(JsonNode array, String field) {
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        for (JsonNode item : array) {
            values.add(field == null ? item.asText() : item.get(field).asText());
        }
        return values;
    }

    private static final String INCOMPLETE_JUDGMENTS_STOP = """
            {
              "decision": "STOP",
              "decision_scope": "BOUNDED_PROTOTYPE_DECISION_NO_SEMANTIC_PUBLICATION",
              "documented_exit_code": 2,
              "failure_stage": "PRE_UNBLINDING_VALIDATION",
              "human_product_team_review": {
                "authority": "PRODUCT_TEAM_ONLY",
                "semantic_publication_allowed": false,
                "status": "PENDING"
              },
              "integrity": {
                "hard_stop_violations": [
                  "PRE_UNBLINDING_VALIDATION_FAILED"
                ],
                "status": "FAILED"
              },
              "metrics_computed": false,
              "pre_unblinding_validation": {
                "passed": false,
                "validated_before_sealed_key_read": true
              },
              "report_id": "pkb001-task7-petclinic-818c413-stop-v1",
              "schema_version": "pkb001.task7.stop-report.v1",
              "semantic_publication_allowed": false,
              "stop_reasons": [
                {
                  "code": "PRE_UNBLINDING_VALIDATION_FAILED",
                  "detail": "validation/pkb001/task6-blind-review/judgment-workspaces/reviewer-01/judgment-template.json must contain 15 complete judgments"
                }
              ],
              "unblinding_performed": false
            }
            """;

    private static final String REVIEWER_PACKET_STOP = """
            {
              "decision": "STOP",
              "decision_scope": "BOUNDED_PROTOTYPE_DECISION_NO_SEMANTIC_PUBLICATION",
              "documented_exit_code": 2,
              "failure_stage": "PRE_UNBLINDING_VALIDATION",
              "human_product_team_review": {
                "authority": "PRODUCT_TEAM_ONLY",
                "semantic_publication_allowed": false,
                "status": "PENDING"
              },
              "integrity": {
                "hard_stop_violations": [
                  "PRE_UNBLINDING_VALIDATION_FAILED"
                ],
                "status": "FAILED"
              },
              "metrics_computed": false,
              "pre_unblinding_validation": {
                "passed": false,
                "validated_before_sealed_key_read": true
              },
              "report_id": "pkb001-task7-petclinic-818c413-stop-v1",
              "schema_version": "pkb001.task7.stop-report.v1",
              "semantic_publication_allowed": false,
              "stop_reasons": [
                {
                  "code": "PRE_UNBLINDING_VALIDATION_FAILED",
                  "detail": "validation/pkb001/task6-blind-review/judgment-workspaces/reviewer-01/packet-input.json digest does not match the sealed packet"
                }
              ],
              "unblinding_performed": false
            }
            """;

    private static final String INVALID_KEY_STOP = """
            {
              "decision": "STOP",
              "decision_scope": "BOUNDED_PROTOTYPE_DECISION_NO_SEMANTIC_PUBLICATION",
              "documented_exit_code": 2,
              "failure_stage": "POST_BINDING_INTEGRITY_VALIDATION",
              "human_product_team_review": {
                "authority": "PRODUCT_TEAM_ONLY",
                "semantic_publication_allowed": false,
                "status": "PENDING"
              },
              "integrity": {
                "hard_stop_violations": [
                  "POST_BINDING_INTEGRITY_VALIDATION_FAILED"
                ],
                "status": "FAILED"
              },
              "metrics_computed": false,
              "pre_unblinding_validation": {
                "passed": false,
                "validated_before_sealed_key_read": true
              },
              "report_id": "pkb001-task7-petclinic-818c413-stop-v1",
              "schema_version": "pkb001.task7.stop-report.v1",
              "semantic_publication_allowed": false,
              "stop_reasons": [
                {
                  "code": "POST_BINDING_INTEGRITY_VALIDATION_FAILED",
                  "detail": "sealed key does not account for every packet item"
                }
              ],
              "unblinding_performed": false
            }
            """;
}

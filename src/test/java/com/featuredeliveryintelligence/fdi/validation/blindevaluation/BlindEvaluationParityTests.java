package com.featuredeliveryintelligence.fdi.validation.blindevaluation;

import com.featuredeliveryintelligence.fdi.validation.codebaseline.CodeBaselineResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Byte-for-byte rendering parity between the Java {@link BlindEvaluation}
 * report and the removed transitional Python module {@code pkb001_evaluate.py}.
 * The expected bytes below are frozen stdout of the Python consumer
 * ({@code json.dumps(report, indent=2) + "\n"}) captured at the BL-026
 * combined integration for each case's exact inputs; they are asserted as
 * immutable reference bytes, never re-executed. Covers the 21/30 R3 CONTINUE
 * case, the F1 case, the empty-input REVISE case, the STOP case, duplicate
 * collapse with an integral median, an even-count float median, the mixed
 * outcome REVISE case, and {@code build_decision_report}.
 */
class BlindEvaluationParityTests {
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String FROZEN_REVERSE_CONTINUE = """
{
  "minimum_sample_satisfied": true,
  "hard_gate_failures": [],
  "arm_metrics": [
    {
      "arm": "R3",
      "proposal_count": 30,
      "gold_item_count": 10,
      "supported_count": 21,
      "partially_supported_count": 6,
      "unsupported_count": 3,
      "useful_rate": 0.7,
      "unsupported_rate": 0.1,
      "precision": 0.7,
      "evidence_validity": 1.0,
      "wilson_low": 0.5212421254128504,
      "wilson_high": 0.8333525173175619,
      "median_review_seconds": 60.0
    }
  ],
  "decision": "CONTINUE",
  "claim_boundary": "CALIBRATION_ONLY_NOT_PRODUCTION_EVIDENCE"
}
""";

    private static final String FROZEN_FORWARD_F1 = """
{
  "minimum_sample_satisfied": true,
  "hard_gate_failures": [],
  "arm_metrics": [
    {
      "arm": "F1",
      "proposal_count": 30,
      "gold_item_count": 10,
      "supported_count": 30,
      "partially_supported_count": 0,
      "unsupported_count": 0,
      "useful_rate": 1.0,
      "unsupported_rate": 0.0,
      "precision": 1.0,
      "evidence_validity": 1.0,
      "wilson_low": 0.8864866068260312,
      "wilson_high": 0.9999999999999999,
      "median_review_seconds": 60.0
    }
  ],
  "decision": "CONTINUE",
  "claim_boundary": "CALIBRATION_ONLY_NOT_PRODUCTION_EVIDENCE"
}
""";

    private static final String FROZEN_EMPTY_INPUT_REVISE = """
{
  "minimum_sample_satisfied": false,
  "hard_gate_failures": [],
  "arm_metrics": [],
  "decision": "REVISE",
  "claim_boundary": "CALIBRATION_ONLY_NOT_PRODUCTION_EVIDENCE"
}
""";

    private static final String FROZEN_STOP_CASE = """
{
  "minimum_sample_satisfied": true,
  "hard_gate_failures": [
    "GROUND_TRUTH_ACCESS"
  ],
  "arm_metrics": [
    {
      "arm": "R3",
      "proposal_count": 30,
      "gold_item_count": 10,
      "supported_count": 30,
      "partially_supported_count": 0,
      "unsupported_count": 0,
      "useful_rate": 1.0,
      "unsupported_rate": 0.0,
      "precision": 1.0,
      "evidence_validity": 1.0,
      "wilson_low": 0.8864866068260312,
      "wilson_high": 0.9999999999999999,
      "median_review_seconds": 60.0
    }
  ],
  "decision": "STOP",
  "claim_boundary": "CALIBRATION_ONLY_NOT_PRODUCTION_EVIDENCE"
}
""";

    private static final String FROZEN_DUPLICATE_COLLAPSE = """
{
  "minimum_sample_satisfied": true,
  "hard_gate_failures": [],
  "arm_metrics": [
    {
      "arm": "R1",
      "proposal_count": 1,
      "gold_item_count": 1,
      "supported_count": 0,
      "partially_supported_count": 0,
      "unsupported_count": 1,
      "useful_rate": 0.0,
      "unsupported_rate": 1.0,
      "precision": 0.0,
      "evidence_validity": 1.0,
      "wilson_low": 0.0,
      "wilson_high": 0.7934506856227626,
      "median_review_seconds": 120
    }
  ],
  "decision": "REVISE",
  "claim_boundary": "CALIBRATION_ONLY_NOT_PRODUCTION_EVIDENCE"
}
""";

    private static final String FROZEN_EVEN_COUNT_FLOAT_MEDIAN = """
{
  "minimum_sample_satisfied": true,
  "hard_gate_failures": [],
  "arm_metrics": [
    {
      "arm": "R3",
      "proposal_count": 2,
      "gold_item_count": 2,
      "supported_count": 2,
      "partially_supported_count": 0,
      "unsupported_count": 0,
      "useful_rate": 1.0,
      "unsupported_rate": 0.0,
      "precision": 1.0,
      "evidence_validity": 1.0,
      "wilson_low": 0.34238022750665303,
      "wilson_high": 1.0,
      "median_review_seconds": 60.0
    }
  ],
  "decision": "CONTINUE",
  "claim_boundary": "CALIBRATION_ONLY_NOT_PRODUCTION_EVIDENCE"
}
""";

    private static final String FROZEN_MIXED_OUTCOME_REVISE = """
{
  "minimum_sample_satisfied": true,
  "hard_gate_failures": [],
  "arm_metrics": [
    {
      "arm": "R2",
      "proposal_count": 6,
      "gold_item_count": 6,
      "supported_count": 2,
      "partially_supported_count": 2,
      "unsupported_count": 1,
      "useful_rate": 0.3333333333333333,
      "unsupported_rate": 0.16666666666666666,
      "precision": 0.3333333333333333,
      "evidence_validity": 1.0,
      "wilson_low": 0.09677141110578041,
      "wilson_high": 0.700006684861608,
      "median_review_seconds": 60.0
    }
  ],
  "decision": "REVISE",
  "claim_boundary": "CALIBRATION_ONLY_NOT_PRODUCTION_EVIDENCE"
}
""";

    private static final String FROZEN_DECISION_REPORT_WRAP = """
{
  "report_id": "report-parity-1",
  "ground_truth_sha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
  "minimum_sample_satisfied": true,
  "hard_gate_failures": [],
  "arm_metrics": [
    {
      "arm": "F1",
      "proposal_count": 30,
      "gold_item_count": 10,
      "supported_count": 30,
      "partially_supported_count": 0,
      "unsupported_count": 0,
      "useful_rate": 1.0,
      "unsupported_rate": 0.0,
      "precision": 1.0,
      "evidence_validity": 1.0,
      "wilson_low": 0.8864866068260312,
      "wilson_high": 0.9999999999999999,
      "median_review_seconds": 60.0
    }
  ],
  "decision": "CONTINUE",
  "claim_boundary": "CALIBRATION_ONLY_NOT_PRODUCTION_EVIDENCE"
}
""";

    @Test
    void reverseContinueCaseMatchesPythonBytes() throws Exception {
        ArrayNode proposals = BlindEvaluationTests.proposals(30, "R3");
        List<String> outcomes = new ArrayList<>(BlindEvaluationTests.repeat("SUPPORTED", 21));
        outcomes.addAll(BlindEvaluationTests.repeat("UNSUPPORTED", 3));
        outcomes.addAll(BlindEvaluationTests.repeat("PARTIALLY_SUPPORTED", 6));
        ArrayNode judgments = BlindEvaluationTests.judgmentsFor(proposals, outcomes);

        ObjectNode report = BlindEvaluationTests.evaluate(proposals, judgments, 30, 10, null);

        assertArrayEquals(FROZEN_REVERSE_CONTINUE.getBytes(StandardCharsets.UTF_8), render(report));
    }

    @Test
    void forwardF1CaseMatchesPythonBytes() throws Exception {
        ArrayNode proposals = BlindEvaluationTests.proposals(30, "F1");
        ArrayNode judgments = BlindEvaluationTests.judgmentsFor(
                proposals, BlindEvaluationTests.repeat("SUPPORTED", 30));

        ObjectNode report = BlindEvaluationTests.evaluate(proposals, judgments, 30, 10, null);

        assertArrayEquals(FROZEN_FORWARD_F1.getBytes(StandardCharsets.UTF_8), render(report));
    }

    @Test
    void emptyInputReviseCaseMatchesPythonBytes() throws Exception {
        ObjectNode report = BlindEvaluationTests.evaluate(
                JSON.createArrayNode(), JSON.createArrayNode(), 30, 10, null);

        assertArrayEquals(FROZEN_EMPTY_INPUT_REVISE.getBytes(StandardCharsets.UTF_8), render(report));
    }

    @Test
    void stopCaseMatchesPythonBytes() throws Exception {
        ArrayNode proposals = BlindEvaluationTests.proposals(30, "R3");
        ArrayNode judgments = BlindEvaluationTests.judgmentsFor(
                proposals, BlindEvaluationTests.repeat("SUPPORTED", 30));

        ObjectNode report = BlindEvaluationTests.evaluate(
                proposals, judgments, 30, 10, List.of("GROUND_TRUTH_ACCESS"));

        assertArrayEquals(FROZEN_STOP_CASE.getBytes(StandardCharsets.UTF_8), render(report));
    }

    @Test
    void duplicateCollapseIntegralMedianMatchesPythonBytes() throws Exception {
        ArrayNode items = BlindEvaluationTests.proposals(1, "R1");
        ObjectNode duplicate = (ObjectNode) JSON.readTree(items.get(0).toString());
        duplicate.put("proposal_id", "R1-duplicate");
        ArrayNode proposals = JSON.createArrayNode();
        proposals.add(items.get(0));
        proposals.add(duplicate);
        ArrayNode judgments = JSON.createArrayNode();
        judgments.addAll(BlindEvaluationTests.judgmentsFor(items, List.of("SUPPORTED")));
        judgments.addAll(BlindEvaluationTests.judgmentsFor(
                JSON.createArrayNode().add(duplicate), List.of("UNSUPPORTED")));

        ObjectNode report = BlindEvaluationTests.evaluate(proposals, judgments, 1, 1, null);

        byte[] rendered = render(report);
        assertArrayEquals(FROZEN_DUPLICATE_COLLAPSE.getBytes(StandardCharsets.UTF_8), rendered);
        // Odd record count over integral review sums renders an int (no ".0").
        assertEquals(1, countOccurrences(rendered, "\"median_review_seconds\": 120"));
    }

    @Test
    void evenRecordCountRendersFloatMedianLikePython() throws Exception {
        ArrayNode proposals = BlindEvaluationTests.proposals(2, "R3");
        ArrayNode judgments = BlindEvaluationTests.judgmentsFor(
                proposals, BlindEvaluationTests.repeat("SUPPORTED", 2));

        ObjectNode report = BlindEvaluationTests.evaluate(proposals, judgments, 1, 1, null);

        byte[] rendered = render(report);
        assertArrayEquals(FROZEN_EVEN_COUNT_FLOAT_MEDIAN.getBytes(StandardCharsets.UTF_8), rendered);
        assertEquals(1, countOccurrences(rendered, "\"median_review_seconds\": 60.0"));
    }

    @Test
    void mixedOutcomeReviseCaseMatchesPythonBytes() throws Exception {
        ArrayNode proposals = BlindEvaluationTests.proposals(6, "R2");
        List<String> outcomes = List.of("SUPPORTED", "PARTIALLY_SUPPORTED", "UNSUPPORTED",
                "DUPLICATE", "SUPPORTED", "PARTIALLY_SUPPORTED");
        ArrayNode judgments = BlindEvaluationTests.judgmentsFor(proposals, outcomes);

        ObjectNode report = BlindEvaluationTests.evaluate(proposals, judgments, 6, 1, null);

        assertArrayEquals(FROZEN_MIXED_OUTCOME_REVISE.getBytes(StandardCharsets.UTF_8), render(report));
    }

    @Test
    void decisionReportWrapMatchesPythonBytes() throws Exception {
        ArrayNode proposals = BlindEvaluationTests.proposals(30, "F1");
        ArrayNode judgments = BlindEvaluationTests.judgmentsFor(
                proposals, BlindEvaluationTests.repeat("SUPPORTED", 30));

        ObjectNode report = new BlindEvaluation().buildDecisionReport(
                "report-parity-1", "a".repeat(64),
                BlindEvaluationTests.evaluate(proposals, judgments, 30, 10, null));

        assertArrayEquals(FROZEN_DECISION_REPORT_WRAP.getBytes(StandardCharsets.UTF_8), render(report));
    }

    private static byte[] render(JsonNode report) {
        return new CodeBaselineResult(report).toJsonBytes();
    }

    private static int countOccurrences(byte[] haystack, String needle) {
        String text = new String(haystack, StandardCharsets.UTF_8);
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}

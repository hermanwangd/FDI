package com.featuredeliveryintelligence.fdi.validation.blindevaluation;

import com.featuredeliveryintelligence.fdi.validation.codebaseline.CodeBaselineResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Byte-for-byte rendering parity between the Java {@link BlindEvaluation}
 * report and the transitional Python module {@code pkb001_evaluate.py} driven
 * through {@code python3} with {@code json.dumps(report, indent=2) + "\n"}.
 * Covers the 21/30 R3 CONTINUE case, the F1 case, the empty-input REVISE
 * case, the STOP case, duplicate collapse with an integral median, an
 * even-count float median, and {@code build_decision_report}.
 */
class BlindEvaluationParityTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path REPOSITORY = Path.of("").toAbsolutePath();

    @TempDir Path temp;

    @Test
    void reverseContinueCaseMatchesPythonBytes() throws Exception {
        ArrayNode proposals = BlindEvaluationTests.proposals(30, "R3");
        List<String> outcomes = new ArrayList<>(BlindEvaluationTests.repeat("SUPPORTED", 21));
        outcomes.addAll(BlindEvaluationTests.repeat("UNSUPPORTED", 3));
        outcomes.addAll(BlindEvaluationTests.repeat("PARTIALLY_SUPPORTED", 6));
        ArrayNode judgments = BlindEvaluationTests.judgmentsFor(proposals, outcomes);

        ObjectNode report = BlindEvaluationTests.evaluate(proposals, judgments, 30, 10, null);

        assertArrayEquals(pythonOracle(proposals, judgments, options(30, 10, null, null)),
                render(report));
    }

    @Test
    void forwardF1CaseMatchesPythonBytes() throws Exception {
        ArrayNode proposals = BlindEvaluationTests.proposals(30, "F1");
        ArrayNode judgments = BlindEvaluationTests.judgmentsFor(
                proposals, BlindEvaluationTests.repeat("SUPPORTED", 30));

        ObjectNode report = BlindEvaluationTests.evaluate(proposals, judgments, 30, 10, null);

        assertArrayEquals(pythonOracle(proposals, judgments, options(30, 10, null, null)),
                render(report));
    }

    @Test
    void emptyInputReviseCaseMatchesPythonBytes() throws Exception {
        ObjectNode report = BlindEvaluationTests.evaluate(
                JSON.createArrayNode(), JSON.createArrayNode(), 30, 10, null);

        assertArrayEquals(pythonOracle(JSON.createArrayNode(), JSON.createArrayNode(),
                options(30, 10, null, null)), render(report));
    }

    @Test
    void stopCaseMatchesPythonBytes() throws Exception {
        ArrayNode proposals = BlindEvaluationTests.proposals(30, "R3");
        ArrayNode judgments = BlindEvaluationTests.judgmentsFor(
                proposals, BlindEvaluationTests.repeat("SUPPORTED", 30));

        ObjectNode report = BlindEvaluationTests.evaluate(
                proposals, judgments, 30, 10, List.of("GROUND_TRUTH_ACCESS"));

        assertArrayEquals(pythonOracle(proposals, judgments,
                options(30, 10, List.of("GROUND_TRUTH_ACCESS"), null)), render(report));
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
        assertArrayEquals(pythonOracle(proposals, judgments, options(1, 1, null, null)), rendered);
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
        assertArrayEquals(pythonOracle(proposals, judgments, options(1, 1, null, null)), rendered);
        assertEquals(1, countOccurrences(rendered, "\"median_review_seconds\": 60.0"));
    }

    @Test
    void mixedOutcomeReviseCaseMatchesPythonBytes() throws Exception {
        ArrayNode proposals = BlindEvaluationTests.proposals(6, "R2");
        List<String> outcomes = List.of("SUPPORTED", "PARTIALLY_SUPPORTED", "UNSUPPORTED",
                "DUPLICATE", "SUPPORTED", "PARTIALLY_SUPPORTED");
        ArrayNode judgments = BlindEvaluationTests.judgmentsFor(proposals, outcomes);

        ObjectNode report = BlindEvaluationTests.evaluate(proposals, judgments, 6, 1, null);

        assertArrayEquals(pythonOracle(proposals, judgments, options(6, 1, null, null)), render(report));
    }

    @Test
    void decisionReportWrapMatchesPythonBytes() throws Exception {
        ArrayNode proposals = BlindEvaluationTests.proposals(30, "F1");
        ArrayNode judgments = BlindEvaluationTests.judgmentsFor(
                proposals, BlindEvaluationTests.repeat("SUPPORTED", 30));
        ObjectNode evaluation = BlindEvaluationTests.evaluate(proposals, judgments, 30, 10, null);
        ObjectNode wrap = JSON.createObjectNode();
        wrap.put("report_id", "report-parity-1");
        wrap.put("ground_truth_sha256", "a".repeat(64));

        ObjectNode report = new BlindEvaluation().buildDecisionReport(
                "report-parity-1", "a".repeat(64), evaluation);

        assertArrayEquals(pythonOracle(proposals, judgments,
                options(30, 10, null, wrap)), render(report));
    }

    // --- oracle harness ---

    private static ObjectNode options(int minimumProposals, int minimumGold,
            List<String> hardFailures, ObjectNode wrap) {
        ObjectNode options = JSON.createObjectNode();
        options.put("minimum_proposals", minimumProposals);
        options.put("minimum_gold", minimumGold);
        if (hardFailures != null) {
            options.set("hard_failures", BlindEvaluationTests.texts(hardFailures));
        }
        if (wrap != null) {
            options.set("wrap", wrap);
        }
        return options;
    }

    private byte[] pythonOracle(ArrayNode proposals, ArrayNode judgments,
            ObjectNode options) throws Exception {
        Path proposalsPath = temp.resolve("proposals.json");
        Path judgmentsPath = temp.resolve("judgments.json");
        Path optionsPath = temp.resolve("options.json");
        Files.write(proposalsPath, JSON.writeValueAsBytes(proposals));
        Files.write(judgmentsPath, JSON.writeValueAsBytes(judgments));
        Files.write(optionsPath, JSON.writeValueAsBytes(options));
        Path script = temp.resolve("oracle.py");
        Files.writeString(script, ORACLE, StandardCharsets.UTF_8);
        Process process = new ProcessBuilder("python3", script.toString(),
                REPOSITORY.resolve("tooling/validation").toString(),
                proposalsPath.toString(), judgmentsPath.toString(), optionsPath.toString())
                .start();
        byte[] stdout = process.getInputStream().readAllBytes();
        byte[] stderr = process.getErrorStream().readAllBytes();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("python oracle timed out");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("python oracle failed: "
                    + new String(stderr, StandardCharsets.UTF_8));
        }
        return stdout;
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

    private static final String ORACLE = """
            import json, sys
            sys.path.insert(0, sys.argv[1])
            from pkb001_evaluate import evaluate, build_decision_report
            proposals = json.load(open(sys.argv[2]))
            judgments = json.load(open(sys.argv[3]))
            options = json.load(open(sys.argv[4]))
            report = evaluate(proposals, judgments,
                              minimum_proposals=options['minimum_proposals'],
                              minimum_gold=options['minimum_gold'],
                              hard_failures=options.get('hard_failures'))
            wrap = options.get('wrap')
            if wrap:
                report = build_decision_report(wrap['report_id'],
                                               wrap['ground_truth_sha256'], report)
            sys.stdout.write(json.dumps(report, indent=2) + '\\n')
            """;
}

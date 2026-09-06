package com.featuredeliveryintelligence.fdi.validation.blindevaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Deterministic blinded evaluation for PKB-001 calibration arms. Ports the
 * observable behavior of the transitional Python consumer
 * {@code tooling/validation/pkb001_evaluate.py}: the Wilson interval with
 * z=1.959963984540054, the proposal-key validation vocabulary, judgment
 * sorting by (proposal_id, reviewer_id) before grouping, the two/three
 * reviewer adjudication, duplicate collapse by key with max SEVERITY
 * (SUPPORTED=0, DUPLICATE=1, PARTIALLY_SUPPORTED=2, UNSUPPORTED=3),
 * MERGE/SPLIT gold-match forcing UNSUPPORTED, per-arm metrics with the exact
 * field order, lexicographically sorted arms, and the decision logic (STOP on
 * failures, CONTINUE on sample_ok and applicable_pass, else REVISE).
 * {@link IllegalArgumentException} mirrors the Python {@code ValueError}
 * vocabulary; hostile non-JSON shapes (wrong node kinds where Python would
 * raise {@code TypeError}/{@code KeyError}) raise {@link IllegalStateException}
 * per the codebase convention. Counts stay integral and rates stay floating so
 * the Python int/float JSON distinction (including the integral median over an
 * odd number of integral review sums) is preserved by the renderer.
 */
public final class BlindEvaluation {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final double Z = 1.959963984540054;
    private static final Set<String> OUTCOMES = Set.of(
            "SUPPORTED", "PARTIALLY_SUPPORTED", "UNSUPPORTED", "DUPLICATE");
    private static final Map<String, Integer> SEVERITY = Map.of(
            "SUPPORTED", 0, "DUPLICATE", 1, "PARTIALLY_SUPPORTED", 2, "UNSUPPORTED", 3);
    private static final Set<String> ARMS = Set.of("R1", "R2", "R3", "F1");
    private static final Set<String> OPERATIONS = Set.of("CREATE", "MERGE", "SPLIT", "REVISE");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final String[] REQUIRED_PROPOSAL_FIELDS = {"proposal_id", "arm", "target_id",
            "relation_type", "operation", "gold_ids", "matched_gold_ids"};

    /** Ports {@code wilson_interval(successes, total)} with the frozen z value. */
    public static double[] wilsonInterval(long successes, long total) {
        if (total < 0 || successes < 0 || successes > total) {
            throw new IllegalArgumentException("invalid Wilson interval counts");
        }
        if (total == 0) {
            return new double[] {0.0, 0.0};
        }
        double proportion = (double) successes / (double) total;
        double denominator = 1 + Z * Z / total;
        double center = (proportion + Z * Z / (2.0 * total)) / denominator;
        double margin = Z * Math.sqrt(proportion * (1 - proportion) / total
                + Z * Z / (4.0 * total * total)) / denominator;
        return new double[] {Math.max(0.0, center - margin), Math.min(1.0, center + margin)};
    }

    /**
     * Ports {@code evaluate(proposals, judgments, minimum_proposals=30,
     * minimum_gold=10, hard_failures=None)}.
     */
    public ObjectNode evaluate(ArrayNode proposals, ArrayNode judgments,
            int minimumProposals, int minimumGold, List<String> hardFailures) {
        if (minimumProposals < 1 || minimumGold < 1) {
            throw new IllegalArgumentException("minimum sample bounds must be positive");
        }
        List<String> failures = new ArrayList<>(new TreeSet<>(hardFailures == null
                ? List.of() : hardFailures));

        List<ObjectNode> sortedJudgments = new ArrayList<>();
        for (JsonNode row : judgments) {
            if (row == null || !row.isObject()) {
                throw new IllegalStateException("judgment is not an object");
            }
            sortedJudgments.add((ObjectNode) row);
        }
        sortedJudgments.sort(Comparator
                .comparing((ObjectNode row) -> textOrEmpty(row.get("proposal_id")))
                .thenComparing(row -> textOrEmpty(row.get("reviewer_id"))));
        Map<JsonNode, List<ObjectNode>> judgmentGroups = new LinkedHashMap<>();
        for (ObjectNode row : sortedJudgments) {
            judgmentGroups.computeIfAbsent(row.get("proposal_id"), key -> new ArrayList<>()).add(row);
        }

        List<ObjectNode> sortedProposals = new ArrayList<>();
        for (JsonNode proposal : proposals) {
            if (proposal == null || !proposal.isObject()) {
                throw new IllegalStateException("proposal is not an object");
            }
            sortedProposals.add((ObjectNode) proposal);
        }
        sortedProposals.sort(Comparator.comparing(
                (ObjectNode proposal) -> textOrEmpty(proposal.get("proposal_id"))));

        Map<ProposalKey, List<ObjectNode>> grouped = new LinkedHashMap<>();
        for (ObjectNode proposal : sortedProposals) {
            grouped.computeIfAbsent(proposalKey(proposal), key -> new ArrayList<>()).add(proposal);
        }

        Map<String, List<ArmRecord>> byArm = new LinkedHashMap<>();
        List<ProposalKey> sortedKeys = new ArrayList<>(grouped.keySet());
        sortedKeys.sort(null);
        for (ProposalKey key : sortedKeys) {
            List<ObjectNode> duplicates = grouped.get(key);
            List<String> outcomes = new ArrayList<>();
            List<ObjectNode> allRows = new ArrayList<>();
            Set<String> allGold = new LinkedHashSet<>();
            Set<String> matchedGold = new LinkedHashSet<>();
            Seconds totalSeconds = Seconds.zero();
            for (ObjectNode proposal : duplicates) {
                JsonNode proposalId = proposal.get("proposal_id");
                List<ObjectNode> rows = judgmentGroups.getOrDefault(proposalId, List.of());
                String outcome = adjudicated(textOrEmpty(proposalId), rows);
                allRows.addAll(rows);
                Set<String> proposalGold = new LinkedHashSet<>(textList(proposal.get("gold_ids")));
                Set<String> proposalMatched =
                        new LinkedHashSet<>(textList(proposal.get("matched_gold_ids")));
                allGold.addAll(proposalGold);
                matchedGold.addAll(proposalMatched);
                if (OPERATIONS_GOLD_FORCED.contains(textOrEmpty(proposal.get("operation")))
                        && !proposalGold.equals(proposalMatched)) {
                    outcome = "UNSUPPORTED";
                }
                outcomes.add(outcome);
                for (ObjectNode row : rows) {
                    totalSeconds = totalSeconds.plus(seconds(row.get("active_review_seconds")));
                }
            }
            String outcome = outcomes.get(0);
            for (String candidate : outcomes) {
                if (SEVERITY.get(candidate) > SEVERITY.get(outcome)) {
                    outcome = candidate;
                }
            }
            boolean evidenceValid = true;
            for (ObjectNode row : allRows) {
                if (row.get("evidence_valid") == null
                        || !row.get("evidence_valid").isBoolean()
                        || !row.get("evidence_valid").asBoolean()) {
                    evidenceValid = false;
                }
            }
            byArm.computeIfAbsent(key.arm(), arm -> new ArrayList<>())
                    .add(new ArmRecord(outcome, allGold, evidenceValid, totalSeconds));
        }

        ArrayNode armMetrics = JSON.createArrayNode();
        List<String> arms = new ArrayList<>(byArm.keySet());
        arms.sort(null);
        boolean sampleOk = !byArm.isEmpty();
        List<ObjectNode> reverse = new ArrayList<>();
        List<ObjectNode> forward = new ArrayList<>();
        for (String arm : arms) {
            List<ArmRecord> records = byArm.get(arm);
            int total = records.size();
            int supported = 0;
            int partial = 0;
            int unsupported = 0;
            int evidenceValid = 0;
            Set<String> gold = new LinkedHashSet<>();
            List<Seconds> seconds = new ArrayList<>();
            for (ArmRecord record : records) {
                if ("SUPPORTED".equals(record.outcome())) {
                    supported++;
                } else if ("PARTIALLY_SUPPORTED".equals(record.outcome())) {
                    partial++;
                } else if ("UNSUPPORTED".equals(record.outcome())) {
                    unsupported++;
                }
                if (record.evidenceValid()) {
                    evidenceValid++;
                }
                gold.addAll(record.goldIds());
                seconds.add(record.reviewSeconds());
            }
            double[] wilson = wilsonInterval(supported, total);
            ObjectNode metrics = JSON.createObjectNode();
            metrics.put("arm", arm);
            metrics.put("proposal_count", total);
            metrics.put("gold_item_count", gold.size());
            metrics.put("supported_count", supported);
            metrics.put("partially_supported_count", partial);
            metrics.put("unsupported_count", unsupported);
            metrics.put("useful_rate", (double) supported / total);
            metrics.put("unsupported_rate", (double) unsupported / total);
            metrics.put("precision", (double) supported / total);
            metrics.put("evidence_validity", (double) evidenceValid / total);
            metrics.put("wilson_low", wilson[0]);
            metrics.put("wilson_high", wilson[1]);
            metrics.set("median_review_seconds", median(seconds));
            armMetrics.add(metrics);
            if (arm.startsWith("R")) {
                reverse.add(metrics);
            }
            if ("F1".equals(arm)) {
                forward.add(metrics);
            }
            if (total < minimumProposals || gold.size() < minimumGold) {
                sampleOk = false;
            }
        }

        boolean reversePass = false;
        for (ObjectNode row : reverse) {
            if (row.get("useful_rate").asDouble() >= 0.70
                    && row.get("unsupported_rate").asDouble() <= 0.10) {
                reversePass = true;
            }
        }
        reversePass = !reverse.isEmpty() && reversePass;
        boolean forwardPass = true;
        for (ObjectNode row : forward) {
            if (row.get("precision").asDouble() < 0.80
                    || row.get("evidence_validity").asDouble() != 1.0
                    || row.get("unsupported_count").asInt() != 0) {
                forwardPass = false;
            }
        }
        forwardPass = !forward.isEmpty() && forwardPass;
        boolean applicablePass = (reverse.isEmpty() || reversePass)
                && (forward.isEmpty() || forwardPass)
                && !(reverse.isEmpty() && forward.isEmpty());

        String decision;
        if (!failures.isEmpty()) {
            decision = "STOP";
        } else if (sampleOk && applicablePass) {
            decision = "CONTINUE";
        } else {
            decision = "REVISE";
        }
        ObjectNode report = JSON.createObjectNode();
        report.put("minimum_sample_satisfied", sampleOk);
        ArrayNode failuresNode = report.putArray("hard_gate_failures");
        failures.forEach(failuresNode::add);
        report.set("arm_metrics", armMetrics);
        report.put("decision", decision);
        report.put("claim_boundary", "CALIBRATION_ONLY_NOT_PRODUCTION_EVIDENCE");
        return report;
    }

    /** Ports {@code build_decision_report(report_id, ground_truth_sha256, evaluation)}. */
    public ObjectNode buildDecisionReport(String reportId, String groundTruthSha256,
            ObjectNode evaluation) {
        if (reportId == null || reportId.isEmpty()) {
            throw new IllegalArgumentException("report_id is required");
        }
        if (groundTruthSha256 == null || !SHA256.matcher(groundTruthSha256).matches()) {
            throw new IllegalArgumentException(
                    "ground truth SHA-256 must be 64 lowercase hex characters");
        }
        Set<String> required = Set.of("minimum_sample_satisfied", "hard_gate_failures",
                "arm_metrics", "decision", "claim_boundary");
        if (evaluation == null || !evaluation.isObject()
                || evaluation.size() != required.size()
                || !required.stream().allMatch(evaluation::has)) {
            throw new IllegalArgumentException("evaluation result has an invalid shape");
        }
        ObjectNode report = JSON.createObjectNode();
        report.put("report_id", reportId);
        report.put("ground_truth_sha256", groundTruthSha256);
        report.setAll(evaluation);
        return report;
    }

    private static final Set<String> OPERATIONS_GOLD_FORCED = Set.of("MERGE", "SPLIT");

    private record ProposalKey(String arm, JsonNode targetId, JsonNode relationType,
            String operation, List<String> goldIds) implements Comparable<ProposalKey> {
        @Override
        public int compareTo(ProposalKey other) {
            int result = arm.compareTo(other.arm);
            if (result != 0) {
                return result;
            }
            result = compareNodes(targetId, other.targetId);
            if (result != 0) {
                return result;
            }
            result = compareNodes(relationType, other.relationType);
            if (result != 0) {
                return result;
            }
            result = operation.compareTo(other.operation);
            if (result != 0) {
                return result;
            }
            int limit = Math.min(goldIds.size(), other.goldIds.size());
            for (int index = 0; index < limit; index++) {
                result = goldIds.get(index).compareTo(other.goldIds.get(index));
                if (result != 0) {
                    return result;
                }
            }
            return Integer.compare(goldIds.size(), other.goldIds.size());
        }
    }

    private static int compareNodes(JsonNode left, JsonNode right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.toString().compareTo(right.toString());
    }

    private record ArmRecord(String outcome, Set<String> goldIds, boolean evidenceValid,
            Seconds reviewSeconds) { }

    /** Integrality-tracking number mirroring the Python int/float distinction. */
    private record Seconds(boolean integral, long asLong, double asDouble) {
        static Seconds zero() {
            return new Seconds(true, 0, 0.0);
        }

        static Seconds of(JsonNode node) {
            if (node == null || node.isNull()) {
                return zero();
            }
            if (node.isIntegralNumber()) {
                return new Seconds(true, node.longValue(), node.doubleValue());
            }
            if (node.isFloatingPointNumber()) {
                return new Seconds(false, 0, node.doubleValue());
            }
            throw new IllegalStateException("active_review_seconds is not a number");
        }

        Seconds plus(Seconds other) {
            if (integral && other.integral) {
                return new Seconds(true, asLong + other.asLong, 0);
            }
            return new Seconds(false, 0, asDouble + other.asDouble);
        }

        double value() {
            return integral ? asLong : asDouble;
        }
    }

    private static JsonNode median(List<Seconds> values) {
        List<Seconds> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.comparingDouble(Seconds::value));
        int size = sorted.size();
        if (size % 2 == 1) {
            Seconds middle = sorted.get(size / 2);
            return middle.integral ? LongNode.valueOf(middle.asLong)
                    : DoubleNode.valueOf(middle.asDouble);
        }
        Seconds left = sorted.get(size / 2 - 1);
        Seconds right = sorted.get(size / 2);
        return DoubleNode.valueOf((left.value() + right.value()) / 2);
    }

    private static Seconds seconds(JsonNode node) {
        return Seconds.of(node);
    }

    private static ProposalKey proposalKey(ObjectNode proposal) {
        for (String field : REQUIRED_PROPOSAL_FIELDS) {
            if (!proposal.has(field)) {
                throw new IllegalArgumentException("proposal is missing required fields");
            }
        }
        JsonNode arm = proposal.get("arm");
        if (arm == null || !arm.isTextual() || !ARMS.contains(arm.asText())) {
            throw new IllegalArgumentException("invalid proposal arm");
        }
        JsonNode operation = proposal.get("operation");
        if (operation == null || !operation.isTextual()
                || !OPERATIONS.contains(operation.asText())) {
            throw new IllegalArgumentException("invalid proposal operation");
        }
        List<String> goldIds = new ArrayList<>(textList(proposal.get("gold_ids")));
        goldIds.sort(null);
        return new ProposalKey(arm.asText(), proposal.get("target_id"),
                proposal.get("relation_type"), operation.asText(), goldIds);
    }

    private static List<String> textList(JsonNode node) {
        if (node == null || !node.isArray()) {
            throw new IllegalStateException("gold identifier list is not an array");
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item == null || !item.isTextual()) {
                throw new IllegalStateException("gold identifier is not a string");
            }
            values.add(item.asText());
        }
        return values;
    }

    private static String adjudicated(String proposalId, List<ObjectNode> rows) {
        Set<JsonNode> reviewers = new LinkedHashSet<>();
        for (ObjectNode row : rows) {
            reviewers.add(row.get("reviewer_id"));
        }
        if (rows.size() < 2 || reviewers.size() < 2) {
            throw new IllegalArgumentException(
                    "two independent judgments are required for " + proposalId);
        }
        for (ObjectNode row : rows) {
            JsonNode outcome = row.get("outcome");
            if (outcome == null || !outcome.isTextual() || !OUTCOMES.contains(outcome.asText())) {
                throw new IllegalArgumentException("invalid judgment outcome for " + proposalId);
            }
        }
        String first = rows.get(0).get("outcome").asText();
        String second = rows.get(1).get("outcome").asText();
        if (first.equals(second)) {
            return first;
        }
        if (rows.size() < 3 || reviewers.size() < 3) {
            throw new IllegalArgumentException(
                    "third reviewer is required for disagreement on " + proposalId);
        }
        return rows.get(2).get("outcome").asText();
    }

    private static String textOrEmpty(JsonNode node) {
        return node == null || node.isNull() || !node.isTextual() ? "" : node.asText();
    }
}

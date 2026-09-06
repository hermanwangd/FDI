package com.featuredeliveryintelligence.fdi.validation.humanreviewpacket;

import com.featuredeliveryintelligence.fdi.validation.codebaseline.PythonJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the PKB-001 Product Team human review decision packet.
 *
 * <p>Ports the observable behavior of the transitional Python consumer
 * {@code tooling/validation/build_pkb001_human_review_packet.py} exactly: the
 * same eight sealed source paths, the same packet shape with Python dict
 * insertion order, the same {@code json.dumps(packet, indent=2)} rendering
 * (insertion order, {@code ensure_ascii=True} so every non-ASCII code point is
 * escaped as lowercase {@code \\uXXXX} with surrogate pairs above the BMP),
 * the same markdown template with hardcoded summary bullets, the same
 * sha256 source digests, and the same failure surface (missing or malformed
 * inputs propagate as exceptions).
 *
 * <p>Known deliberate deviations from the Python consumer, all outside the
 * byte-level output contract: none identified. The packet JSON writer is a
 * private reimplementation (see {@link #jsonBytes}) because the sibling
 * {@code BlindReview.jsonBytes} renders sorted keys with
 * {@code ensure_ascii=False}, which is a different writer.
 */
public final class HumanReviewPacket {
    private static final JsonNodeFactory NODE = JsonNodeFactory.instance;

    private static final Map<String, String> SOURCE_PATHS = sourcePaths();

    private HumanReviewPacket() { }

    /** The eight fixed repo-relative sealed source paths, in Python dict order. */
    public static Map<String, String> sourcePaths() {
        Map<String, String> paths = new LinkedHashMap<>();
        paths.put("blind_packet",
                "validation/pkb001/task6-blind-review/blind-review-packet.json");
        paths.put("reviewer_01",
                "validation/pkb001/task6-blind-review/judgment-workspaces/reviewer-01/judgment-template.json");
        paths.put("reviewer_02",
                "validation/pkb001/task6-blind-review/judgment-workspaces/reviewer-02/judgment-template.json");
        paths.put("pending_disagreements",
                "validation/pkb001/task7-evaluation/third-review-pending.json");
        paths.put("evaluation_report",
                "validation/pkb001/task7-evaluation/evaluation-report.json");
        paths.put("sealed_key",
                "validation/pkb001/task6-blind-review/sealed-blind-key.json");
        paths.put("forward_run",
                "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json");
        paths.put("evaluator_gold",
                "validation/pkb001/evaluator/petclinic-818c413/gold-mappings.json");
        return paths;
    }

    /** Reads one source file like Python {@code json.loads(path.read_text())}. */
    private static JsonNode readJson(Path root, String relativePath) throws IOException {
        return PythonJson.readTree(Files.readAllBytes(root.resolve(relativePath)));
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder hex = new StringBuilder();
            for (byte value : digest.digest(bytes)) {
                hex.append(Character.forDigit((value >> 4) & 0xF, 16));
                hex.append(Character.forDigit(value & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 unavailable", failure);
        }
    }

    /**
     * Builds the full packet object. JsonNode values are copied by reference
     * exactly where the Python consumer copies references, so integer versus
     * float number kinds stay as parsed.
     */
    public static ObjectNode buildPacket(Path root) throws IOException {
        Map<String, JsonNode> source = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : SOURCE_PATHS.entrySet()) {
            source.put(entry.getKey(), readJson(root, entry.getValue()));
        }

        Map<String, Map<String, JsonNode>> judgments = new LinkedHashMap<>();
        for (String reviewer : List.of("reviewer_01", "reviewer_02")) {
            Map<String, JsonNode> byBlindId = new LinkedHashMap<>();
            for (JsonNode row : requiredArray(source.get(reviewer).get("judgments"), "judgments")) {
                byBlindId.put(row.get("blind_id").asText(), row);
            }
            judgments.put(reviewer, byBlindId);
        }
        Map<String, JsonNode> pending = new LinkedHashMap<>();
        for (JsonNode row : requiredArray(
                source.get("pending_disagreements").get("items"), "items")) {
            pending.put(row.get("blind_id").asText(), row.get("reasons"));
        }
        Map<String, JsonNode> identities = new LinkedHashMap<>();
        for (JsonNode row : requiredArray(source.get("sealed_key").get("items"), "items")) {
            identities.put(row.get("blind_id").asText(), row);
        }
        Map<String, JsonNode> forwardResults = new LinkedHashMap<>();
        for (JsonNode row : requiredArray(
                source.get("forward_run").get("capability_results"), "capability_results")) {
            forwardResults.put(row.get("capability_id").asText(), row);
        }
        Map<String, JsonNode> goldMappings = new LinkedHashMap<>();
        for (JsonNode row : requiredArray(
                source.get("evaluator_gold").get("mappings"), "mappings")) {
            goldMappings.put(row.get("capability_id").asText(), row);
        }
        JsonNode evaluationForward = source.get("evaluation_report")
                .get("forward_expected_realization_comparison");
        Map<String, JsonNode> evaluationByCapability = new LinkedHashMap<>();
        for (JsonNode row : requiredArray(evaluationForward.get("by_capability"), "by_capability")) {
            evaluationByCapability.put(row.get("capability_id").asText(), row);
        }

        ArrayNode items = NODE.arrayNode();
        for (JsonNode candidate : requiredArray(
                source.get("blind_packet").get("items"), "items")) {
            String blindId = candidate.get("blind_id").asText();
            JsonNode identity = required(identities.get(blindId), blindId);
            String sourceArm = identity.get("source_arm").asText();

            ArrayNode reviewerSummaries = NODE.arrayNode();
            for (String reviewer : List.of("reviewer_01", "reviewer_02")) {
                JsonNode judgment = required(judgments.get(reviewer).get(blindId), blindId);
                ObjectNode summary = NODE.objectNode();
                summary.put("reviewer", reviewer.replace('_', '-'));
                summary.set("action", judgment.get("review_action"));
                summary.set("outcome", judgment.get("outcome"));
                summary.set("suggested_name", judgment.get("suggested_name"));
                summary.set("notes", judgment.get("reviewer_notes"));
                summary.set("limitations", judgment.get("limitations"));
                summary.set("unsupported_claims", judgment.get("unsupported_claims"));
                reviewerSummaries.add(summary);
            }

            ObjectNode item = NODE.objectNode();
            item.put("blind_id", blindId);
            item.put("source_arm", sourceArm);
            item.set("source_identifier", identity.get("source_identifier"));
            item.set("candidate_capability", candidate.get("candidate_capability"));
            item.set("candidate_basis", candidate.get("candidate_basis"));
            item.set("confidence_score", candidate.get("confidence_score"));
            ArrayNode componentReferences = NODE.arrayNode();
            for (JsonNode component : requiredArray(candidate.get("component_refs"), "component_refs")) {
                componentReferences.add(component.get("reference"));
            }
            item.set("component_references", componentReferences);
            item.put("needs_resolution", pending.containsKey(blindId));
            item.set("resolution_reasons", pending.containsKey(blindId)
                    ? pending.get(blindId) : NODE.arrayNode());
            item.set("evaluator_judgments", reviewerSummaries);
            item.put("proposal_only", "REVERSE".equals(sourceArm));
            item.putNull("forward_component_comparison");

            ObjectNode productTeamDecision = NODE.objectNode();
            productTeamDecision.putNull("action");
            productTeamDecision.putNull("approved_name");
            productTeamDecision.putNull("notes");
            item.set("product_team_decision", productTeamDecision);

            if ("FORWARD".equals(sourceArm)) {
                item.set("forward_component_comparison",
                        forwardComparison(identity, forwardResults, goldMappings,
                                evaluationByCapability));
            }
            items.add(item);
        }

        JsonNode pathComparison = evaluationForward.get("file_component_path_comparison");
        JsonNode nodeCoverage = evaluationForward.get("expected_graph_node_coverage");
        JsonNode exactComparison = evaluationForward
                .get("proposed_component_exact_graph_node_comparison");

        ObjectNode packet = NODE.objectNode();
        packet.put("schema_version", "pkb001.human-review-decision-packet.v1");
        packet.put("status", "PENDING_PRODUCT_TEAM_REVIEW");
        packet.set("current_prototype_decision", source.get("evaluation_report").get("decision"));
        packet.put("semantic_publication_allowed", false);
        packet.put("authority_statement",
                "Evaluator judgments are advisory. Only the Product Team may decide "
                        + "Product meaning; completing this packet does not publish semantics.");
        packet.set("allowed_product_team_actions",
                source.get("blind_packet").get("allowed_review_actions"));

        ObjectNode forwardComparison = NODE.objectNode();
        forwardComparison.put("interpretation",
                "The run generally found the correct code area, but did not precisely "
                        + "name the evaluator's expected realization nodes as proposed components.");
        forwardComparison.set("expected_component_path_recall",
                pathComparison.get("expected_component_path_recall"));
        forwardComparison.set("proposed_component_path_precision",
                pathComparison.get("proposed_component_path_precision"));
        ObjectNode coverage = NODE.objectNode();
        coverage.set("cited", nodeCoverage.get("expected_graph_nodes_cited"));
        coverage.set("expected", nodeCoverage.get("expected_graph_nodes"));
        coverage.set("rate", nodeCoverage.get("expected_graph_node_coverage_rate"));
        forwardComparison.set("expected_graph_node_coverage", coverage);
        ObjectNode exact = NODE.objectNode();
        exact.set("matched", exactComparison.get("proposed_component_exact_graph_node_matches"));
        exact.set("expected", exactComparison.get("expected_graph_nodes"));
        exact.set("rate", exactComparison.get("proposed_component_exact_graph_node_recall"));
        forwardComparison.set("exact_proposed_component_matches", exact);
        forwardComparison.set("limits", evaluationForward.get("comparison_limits"));
        packet.set("forward_comparison", forwardComparison);

        ObjectNode sourceDigests = NODE.objectNode();
        for (String path : SOURCE_PATHS.values()) {
            sourceDigests.put(path, sha256Hex(Files.readAllBytes(root.resolve(path))));
        }
        packet.set("source_digests", sourceDigests);
        packet.set("items", items);

        ObjectNode finalDecision = NODE.objectNode();
        finalDecision.putNull("reviewer_name");
        finalDecision.putNull("reviewed_at");
        finalDecision.putNull("prototype_decision");
        finalDecision.putNull("decision_rationale");
        finalDecision.put("semantic_publication_approval", false);
        packet.set("final_product_team_decision", finalDecision);
        return packet;
    }

    private static ObjectNode forwardComparison(JsonNode identity,
                                                Map<String, JsonNode> forwardResults,
                                                Map<String, JsonNode> goldMappings,
                                                Map<String, JsonNode> evaluationByCapability) {
        String capabilityId = identity.get("source_identifier").asText();
        JsonNode forward = required(forwardResults.get(capabilityId), capabilityId);
        JsonNode gold = required(goldMappings.get(capabilityId), capabilityId);
        JsonNode evaluated = required(evaluationByCapability.get(capabilityId), capabilityId);

        List<String> expectedIds = new ArrayList<>();
        for (JsonNode row : requiredArray(gold.get("expected_components"), "expected_components")) {
            expectedIds.add(row.get("graph_node_id").asText());
        }
        Set<String> citedIds = new LinkedHashSet<>();
        for (JsonNode row : requiredArray(forward.get("proposed_components"), "proposed_components")) {
            citedIds.add(row.get("graph_node_id").asText());
        }
        for (JsonNode row : requiredArray(forward.get("evidence_refs"), "evidence_refs")) {
            citedIds.add(row.get("graph_node_id").asText());
        }
        List<String> missingIds = new ArrayList<>();
        for (String nodeId : expectedIds) {
            if (!citedIds.contains(nodeId)) {
                missingIds.add(nodeId);
            }
        }

        ObjectNode comparison = NODE.objectNode();
        ArrayNode expectedComponents = NODE.arrayNode();
        for (JsonNode row : gold.get("expected_components")) {
            ObjectNode component = NODE.objectNode();
            component.set("graph_node_id", row.get("graph_node_id"));
            component.set("source_path", row.get("source_path"));
            component.set("source_location", row.get("source_location"));
            expectedComponents.add(component);
        }
        comparison.set("expected_components", expectedComponents);
        ArrayNode proposedComponents = NODE.arrayNode();
        for (JsonNode row : forward.get("proposed_components")) {
            ObjectNode component = NODE.objectNode();
            component.set("graph_node_id", row.get("graph_node_id"));
            component.set("source_location", row.get("source_location"));
            proposedComponents.add(component);
        }
        comparison.set("proposed_components", proposedComponents);
        ArrayNode supporting = NODE.arrayNode();
        for (JsonNode row : forward.get("evidence_refs")) {
            supporting.add(row.get("graph_node_id"));
        }
        comparison.set("supporting_evidence_node_ids", supporting);
        ArrayNode missing = NODE.arrayNode();
        missingIds.forEach(missing::add);
        comparison.set("missing_expected_node_ids", missing);
        comparison.set("expected_nodes_cited", evaluated.get("expected_graph_node_coverage")
                .get("expected_graph_nodes_cited"));
        comparison.set("expected_nodes", evaluated.get("expected_graph_node_coverage")
                .get("expected_graph_nodes"));
        comparison.set("exact_proposed_component_matches",
                evaluated.get("proposed_component_exact_graph_node_matches"));
        boolean unresolved = "UNRESOLVED".equals(textOrNull(forward.get("outcome")));
        comparison.put("difference_classification", unresolved
                ? "MISSING_EVIDENCE" : "GRANULARITY_OR_IDENTIFIER_MISMATCH");
        comparison.put("plain_language", unresolved
                ? "No component was proposed for the expected realization."
                : "The proposal found relevant files or nearby evidence nodes, "
                        + "but its formal components do not exactly identify the expected nodes.");
        return comparison;
    }

    /**
     * Renders the markdown exactly like the Python consumer's
     * {@code render_markdown}: template lines joined with {@code "\n"} whose
     * final element is empty, so the output ends with exactly one newline.
     */
    public static String renderMarkdown(JsonNode packet) {
        List<String> lines = new ArrayList<>();
        lines.add("# PKB-001 Human Review Decision Packet");
        lines.add("");
        lines.add("Status: **" + packet.get("status").asText() + "**");
        lines.add("Current prototype decision: **"
                + packet.get("current_prototype_decision").asText() + "**");
        lines.add("Semantic publication allowed: **false**");
        lines.add("");
        lines.add("## Product Team instructions");
        lines.add("");
        lines.add(packet.get("authority_statement").asText());
        lines.add("");
        lines.add("For each item, select one allowed action, provide the approved capability name when applicable, and record the rationale. Evaluator recommendations are evidence for review, not Product truth. A completed review does not by itself authorize semantic publication.");
        lines.add("");
        StringBuilder allowed = new StringBuilder("Allowed actions: ");
        List<String> actions = new ArrayList<>();
        packet.get("allowed_product_team_actions").forEach(
                action -> actions.add("`" + action.asText() + "`"));
        allowed.append(String.join(", ", actions));
        lines.add(allowed.toString());
        lines.add("");
        int needsResolution = 0;
        for (JsonNode item : packet.get("items")) {
            if (item.get("needs_resolution").asBoolean()) {
                needsResolution++;
            }
        }
        lines.add("Items requiring explicit disagreement resolution: **" + needsResolution + "/15**");
        lines.add("");
        lines.add("## Forward comparison context");
        lines.add("");
        lines.add("- Expected component path recall: 23/24 (95.8%)");
        lines.add("- Proposed component path precision: 21/25 (84.0%)");
        lines.add("- Expected graph-node coverage across components and supporting evidence: 17/24 (70.8%)");
        lines.add("- Exact proposed-component graph-node matches: 0/24");
        lines.add("");
        lines.add("Plain language: the run generally found the correct code area, but its formal components did not precisely identify the evaluator's expected method/entity nodes. File-path overlap and supporting evidence are useful, but neither is an exact proposed-component match.");
        lines.add("");
        lines.add("## Item decisions");
        lines.add("");

        for (JsonNode item : packet.get("items")) {
            lines.add("### " + item.get("blind_id").asText() + " — "
                    + item.get("candidate_capability").asText());
            lines.add("");
            lines.add("Candidate basis: " + item.get("candidate_basis").asText());
            lines.add("");
            lines.add("Confidence: `" + pythonNumberText(item.get("confidence_score")) + "`");
            lines.add("Resolution required: **" + (item.get("needs_resolution").asBoolean() ? "YES" : "NO") + "**");
            List<String> reasons = new ArrayList<>();
            item.get("resolution_reasons").forEach(reason -> reasons.add(reason.asText()));
            lines.add("Resolution reasons: " + (reasons.isEmpty() ? "none" : String.join(", ", reasons)));
            lines.add("");

            JsonNode comparison = item.get("forward_component_comparison");
            if (comparison == null || comparison.isNull()) {
                lines.add("Reverse proposal-only: this capability hypothesis is advisory and has no Forward expected-component comparison.");
                lines.add("");
            } else {
                lines.add("Expected components: "
                        + backtickedJoin(comparison.get("expected_components"), "graph_node_id"));
                lines.add("");
                lines.add("Proposed components: "
                        + backtickedJoinOrNone(comparison.get("proposed_components"), "graph_node_id"));
                lines.add("");
                lines.add("Supporting evidence nodes: "
                        + backtickedJoinOrNone(comparison.get("supporting_evidence_node_ids"), null));
                lines.add("");
                lines.add("Missing expected nodes: "
                        + backtickedJoinOrNone(comparison.get("missing_expected_node_ids"), null));
                lines.add("");
                lines.add("Difference classification: `"
                        + comparison.get("difference_classification").asText() + "` — "
                        + comparison.get("plain_language").asText());
                lines.add("");
            }

            for (JsonNode judgment : item.get("evaluator_judgments")) {
                JsonNode suggestedName = judgment.get("suggested_name");
                String suggested = suggestedName == null || suggestedName.isNull()
                        || suggestedName.asText().isEmpty()
                        ? "none" : suggestedName.asText();
                lines.add("- " + judgment.get("reviewer").asText() + ": `"
                        + judgment.get("action").asText() + "` / `"
                        + judgment.get("outcome").asText() + "`; suggested name: " + suggested);
                lines.add("  - Notes: " + judgment.get("notes").asText());
                List<String> claims = new ArrayList<>();
                judgment.get("unsupported_claims").forEach(claim -> claims.add(claim.asText()));
                lines.add("  - Unsupported claims: "
                        + (claims.isEmpty() ? "none" : String.join("; ", claims)));
            }

            lines.add("");
            lines.add("Product Team decision: **PENDING**");
            lines.add("");
            lines.add("- Action:");
            lines.add("- Approved name:");
            lines.add("- Rationale:");
            lines.add("");
        }

        lines.add("## Final Product Team decision");
        lines.add("");
        lines.add("- Reviewer name:");
        lines.add("- Reviewed at:");
        lines.add("- Prototype decision (`GO`, `REVISE`, or `STOP`):");
        lines.add("- Decision rationale:");
        lines.add("- Semantic publication approval: **false** (requires a separate explicit action)");
        lines.add("");
        return String.join("\n", lines);
    }

    /**
     * Formats a number like Python {@code str(number)}: integers render as the
     * plain integer; floats render like Python 3 {@code repr(float)}
     * (shortest round-trip digits, decimal notation for
     * {@code 1e-4 <= |x| < 1e16}, exponential notation with a signed,
     * at-least-two-digit exponent otherwise).
     */
    public static String pythonNumberText(JsonNode node) {
        if (node == null || node.isNull()) {
            return "None";
        }
        if (node.isIntegralNumber()) {
            return node.bigIntegerValue().toString();
        }
        return pythonFloat(node.doubleValue());
    }

    private static String backtickedJoin(JsonNode rows, String field) {
        List<String> values = new ArrayList<>();
        for (JsonNode row : rows) {
            values.add("`" + (field == null ? row.asText() : row.get(field).asText()) + "`");
        }
        return String.join(", ", values);
    }

    private static String backtickedJoinOrNone(JsonNode rows, String field) {
        String joined = backtickedJoin(rows, field);
        return joined.isEmpty() ? "none" : joined;
    }

    /**
     * Renders exactly like the Python consumer's packet writer:
     * {@code json.dumps(packet, indent=2) + '\n'} — insertion order (no key
     * sorting) and {@code ensure_ascii=True} (every code point above U+007E
     * is escaped as lowercase {@code \\uXXXX}, non-BMP code points as
     * surrogate pairs, matching CPython's {@code py_encode_basestring_ascii}).
     */
    public static byte[] jsonBytes(JsonNode value) {
        StringBuilder out = new StringBuilder();
        writeNode(out, value, 0);
        out.append('\n');
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void writeNode(StringBuilder out, JsonNode node, int indent) {
        if (node == null || node.isNull() || node instanceof MissingNode) {
            out.append("null");
        } else if (node.isTextual()) {
            writeStringAscii(out, node.textValue());
        } else if (node.isBoolean()) {
            out.append(node.booleanValue() ? "true" : "false");
        } else if (node.isIntegralNumber()) {
            out.append(node.bigIntegerValue().toString());
        } else if (node.isNumber()) {
            out.append(pythonFloat(node.doubleValue()));
        } else if (node.isArray()) {
            writeArray(out, node, indent);
        } else if (node.isObject()) {
            writeObject(out, node, indent);
        } else {
            throw new IllegalArgumentException("unsupported JSON value: " + node.getNodeType());
        }
    }

    private static void writeObject(StringBuilder out, JsonNode node, int indent) {
        if (node.isEmpty()) {
            out.append("{}");
            return;
        }
        out.append("{\n");
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            indent(out, indent + 1);
            writeStringAscii(out, field.getKey());
            out.append(": ");
            writeNode(out, field.getValue(), indent + 1);
            if (fields.hasNext()) {
                out.append(',');
            }
            out.append('\n');
        }
        indent(out, indent);
        out.append('}');
    }

    private static void writeArray(StringBuilder out, JsonNode node, int indent) {
        if (node.isEmpty()) {
            out.append("[]");
            return;
        }
        out.append("[\n");
        for (int index = 0; index < node.size(); index++) {
            indent(out, indent + 1);
            writeNode(out, node.get(index), indent + 1);
            if (index + 1 < node.size()) {
                out.append(',');
            }
            out.append('\n');
        }
        indent(out, indent);
        out.append(']');
    }

    private static void indent(StringBuilder out, int level) {
        out.append("  ".repeat(Math.max(0, level)));
    }

    /** CPython {@code py_encode_basestring_ascii}: escape all non-ASCII as lowercase {@code \\uXXXX}. */
    private static void writeStringAscii(StringBuilder out, String value) {
        out.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (character < 0x20) {
                        out.append(String.format("\\u%04x", (int) character));
                    } else if (character < 0x7f) {
                        out.append(character);
                    } else if (Character.isHighSurrogate(character)) {
                        int codePoint = Character.codePointAt(value, index);
                        if (codePoint > 0xffff) {
                            index += Character.charCount(codePoint) - 1;
                            int adjusted = codePoint - 0x10000;
                            out.append(String.format("\\u%04x\\u%04x",
                                    0xd800 | ((adjusted >> 10) & 0x3ff),
                                    0xdc00 | (adjusted & 0x3ff)));
                        } else {
                            out.append(String.format("\\u%04x", (int) character));
                        }
                    } else {
                        out.append(String.format("\\u%04x", (int) character));
                    }
                }
            }
        }
        out.append('"');
    }

    /**
     * Formats a double like Python 3 {@code repr(float)} (what
     * {@code json.dumps} and {@code str(float)} emit): shortest round-trip
     * digits, decimal notation for {@code 1e-4 <= |x| < 1e16}, exponential
     * notation with a signed, at-least-two-digit exponent otherwise.
     */
    static String pythonFloat(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("non-finite float is not valid JSON");
        }
        if (value == 0.0) {
            return Math.copySign(1.0, value) < 0 ? "-0.0" : "0.0";
        }
        // Double.toString is the same shortest round-trip digit sequence CPython
        // repr uses. Normalize it to digits d1..dn and X with
        // value = 0.d1..dn * 10^X, then apply CPython's notation rule:
        // decimal for -4 <= X-1 < 16, exponential "d.ddd e±XX" otherwise.
        String shortest = Double.toString(value);
        boolean negative = shortest.startsWith("-");
        String body = negative ? shortest.substring(1) : shortest;
        String digits;
        int exponent;
        int exponentAt = body.indexOf('E');
        if (exponentAt < 0) {
            exponentAt = body.indexOf('e');
        }
        if (exponentAt >= 0) {
            digits = body.substring(0, exponentAt).replace(".", "");
            exponent = Integer.parseInt(body.substring(exponentAt + 1)) + 1;
        } else {
            int point = body.indexOf('.');
            digits = body.replace(".", "");
            exponent = point < 0 ? body.length() : point;
            int leading = 0;
            while (leading < digits.length() && digits.charAt(leading) == '0') {
                leading++;
            }
            digits = digits.substring(leading);
            exponent -= leading;
        }
        int printedExponent = exponent - 1;
        String sign = negative ? "-" : "";
        if (printedExponent >= -4 && printedExponent < 16) {
            StringBuilder out = new StringBuilder(sign);
            if (exponent > 0) {
                if (digits.length() <= exponent) {
                    out.append(digits).append("0".repeat(exponent - digits.length())).append(".0");
                } else {
                    out.append(digits, 0, exponent).append('.').append(digits.substring(exponent));
                }
            } else {
                out.append("0.").append("0".repeat(-exponent)).append(digits);
            }
            return out.toString();
        }
        int significantEnd = digits.length();
        while (significantEnd > 1 && digits.charAt(significantEnd - 1) == '0') {
            significantEnd--;
        }
        StringBuilder mantissa = new StringBuilder();
        mantissa.append(digits.charAt(0));
        if (significantEnd > 1) {
            mantissa.append('.').append(digits, 1, significantEnd);
        }
        return sign + mantissa + "e" + (printedExponent < 0 ? "-" : "+")
                + String.format("%02d", Math.abs(printedExponent));
    }

    private static JsonNode required(JsonNode node, String name) {
        if (node == null) {
            throw new NullPointerException("required JSON field is missing: " + name);
        }
        return node;
    }

    private static ArrayNode requiredArray(JsonNode node, String name) {
        if (!(node instanceof ArrayNode array)) {
            throw new NullPointerException("required JSON array is missing: " + name);
        }
        return array;
    }

    private static String textOrNull(JsonNode node) {
        return node != null && node.isTextual() ? node.asText() : null;
    }
}

package com.featuredeliveryintelligence.fdi.validation.blindreview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic label/order blinding for PKB-001 Task-6 review material.
 *
 * <p>Ports the observable behavior of the transitional Python consumer
 * {@code tooling/validation/pkb001_blind_review.py} exactly: sealed-input
 * verification with the same fail-closed error vocabulary, SHA-256 blind
 * ordering, sorted indent-2 non-ASCII JSON rendering with trailing newline,
 * packet/key/manifest/workspace shapes, completed-judgment overwrite
 * protection, and the legacy {@code build_blind_packet} seam. This class
 * provides deterministic label/order blinding only:
 * {@code ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT}.
 */
public final class BlindReview {
    public static final String TASK6_DIR = "validation/pkb001/task6-blind-review";

    private static final String TASK6_BRIEF =
            ".superpowers/sdd/IMPLEMENTATION-PLAN/task-6-brief.md";
    private static final String FORWARD_ARTIFACT =
            "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json";
    private static final String FORWARD_MANIFEST =
            "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-manifest.json";
    private static final String FORWARD_WITNESS =
            "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-witness.json";
    private static final String REVERSE_ARTIFACT =
            "validation/pkb001/reverse-task5-pkb001_reverse_run/capability-hypotheses.json";
    private static final String REVERSE_MANIFEST =
            "validation/pkb001/reverse-task5-pkb001_reverse_run/manifest.json";
    private static final String REVERSE_WITNESS =
            "validation/pkb001/reverse-task5-pkb001_reverse_run/provenance-witness.json";
    private static final String GRAPH = "validation/pkb001/artifacts/petclinic-graph-818c413.json";
    private static final String JUDGMENT_SCHEMA =
            "validation/pkb001/schemas/evaluator-judgment-v0.1.schema.json";

    private static final String ORDER_SALT = "pkb001-task6-blind-order-v1\0";
    private static final List<String> ACTIONS =
            List.of("ACCEPT", "RENAME", "MERGE", "SPLIT", "REJECT", "ADD_MISSING");
    private static final List<String> OUTCOMES =
            List.of("SUPPORTED", "PARTIALLY_SUPPORTED", "UNSUPPORTED", "DUPLICATE");
    private static final List<String> JUDGMENT_DIMENSIONS = List.of(
            "evidence_validity", "usefulness", "unsupported_claims",
            "precision", "limitations", "active_review_seconds");
    private static final List<String> REVIEWER_WORKSPACES = List.of("reviewer-01", "reviewer-02");

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonNodeFactory NODE = JsonNodeFactory.instance;

    private BlindReview() {}

    // ------------------------------------------------------------------
    // Task-6 packet generation
    // ------------------------------------------------------------------

    public static Task6Packet buildTask6Packet(Path root) throws IOException {
        ObjectNode forward = readObject(root, FORWARD_ARTIFACT);
        ObjectNode forwardManifest = readObject(root, FORWARD_MANIFEST);
        ObjectNode forwardWitness = readObject(root, FORWARD_WITNESS);
        ObjectNode reverse = readObject(root, REVERSE_ARTIFACT);
        ObjectNode reverseManifest = readObject(root, REVERSE_MANIFEST);
        ObjectNode reverseWitness = readObject(root, REVERSE_WITNESS);
        verifyForwardRun(root, forward, forwardManifest, forwardWitness);
        verifyReverseRun(root, reverse, reverseManifest, reverseWitness);

        JsonNode forwardBinding = required(forward.get("bindings"), "bindings");
        JsonNode reverseBinding = required(reverse.get("source_binding"), "source_binding");
        require(Objects.equals(forwardBinding.get("source_commit_sha"),
                reverseBinding.get("source_commit_sha")), "runs use different source commits");
        require(Objects.equals(forwardBinding.get("graph_sha256"),
                reverseBinding.get("graph_sha256")), "runs use different graph digests");

        ObjectNode graph = readObject(root, GRAPH);
        Map<String, JsonNode> graphNodes = new LinkedHashMap<>();
        for (JsonNode node : requiredArray(graph.get("nodes"), "graph nodes")) {
            graphNodes.put(required(node.get("id"), "graph node id").asText(), node);
        }

        List<ObjectNode> candidates = new ArrayList<>();
        for (JsonNode result : requiredArray(forward.get("capability_results"), "capability_results")) {
            candidates.add(forwardCandidate(result));
        }
        for (JsonNode hypothesis : requiredArray(reverse.get("hypotheses"), "hypotheses")) {
            candidates.add(reverseCandidate(hypothesis, graphNodes));
        }
        candidates.sort(Comparator.comparing(candidate ->
                sha256Hex((ORDER_SALT + candidate.get("source_identifier").asText())
                        .getBytes(StandardCharsets.UTF_8))));

        ArrayNode packetItems = NODE.arrayNode();
        ArrayNode keyItems = NODE.arrayNode();
        for (int index = 0; index < candidates.size(); index++) {
            String blindId = String.format("BR-%03d", index + 1);
            ObjectNode candidate = candidates.get(index);
            ObjectNode item = NODE.objectNode();
            item.put("blind_id", blindId);
            item.put("candidate_capability", candidate.get("candidate_capability").asText());
            item.set("complete_realization_proposed", candidate.get("complete_realization_proposed"));
            item.set("component_refs", candidate.get("component_refs"));
            item.set("evidence_refs", candidate.get("evidence_refs"));
            item.set("confidence_score", candidate.get("confidence_score"));
            item.set("limitations", candidate.get("limitations"));
            item.set("candidate_basis", candidate.get("candidate_basis"));
            item.set("judgment", emptyJudgment());
            packetItems.add(item);

            ObjectNode keyItem = NODE.objectNode();
            keyItem.put("blind_id", blindId);
            keyItem.put("source_identifier", candidate.get("source_identifier").asText());
            keyItem.put("source_arm", candidate.get("source_arm").asText());
            keyItems.add(keyItem);
        }

        ObjectNode packet = NODE.objectNode();
        packet.put("schema_version", "pkb001.task6.blind-review-packet.v1");
        packet.put("packet_id", "pkb001-task6-blind-comparison-v1");
        packet.put("review_status", "AWAITING_JUDGMENTS");
        packet.set("allowed_outcomes", textArray(OUTCOMES));
        packet.set("allowed_review_actions", textArray(ACTIONS));
        packet.set("judgment_dimensions", textArray(JUDGMENT_DIMENSIONS));
        packet.set("empty_judgment", emptyJudgment());
        packet.set("items", packetItems);

        String packetDigest = sha256Hex(jsonBytes(packet));

        ObjectNode key = NODE.objectNode();
        key.put("schema_version", "pkb001.task6.sealed-blind-key.v1");
        key.put("key_id", "pkb001-task6-sealed-blind-key-v1");
        key.put("visibility", "SEALED_KEY_CUSTODIAN_ONLY");
        key.put("sealed_packet_sha256", packetDigest);
        key.put("item_count", keyItems.size());
        key.set("items", keyItems);

        List<String> inputs = List.of(
                TASK6_BRIEF, FORWARD_ARTIFACT, FORWARD_MANIFEST, FORWARD_WITNESS,
                REVERSE_ARTIFACT, REVERSE_MANIFEST, REVERSE_WITNESS, JUDGMENT_SCHEMA);
        ArrayNode inputDigests = NODE.arrayNode();
        for (String input : inputs) {
            ObjectNode entry = NODE.objectNode();
            entry.put("path", input);
            entry.put("sha256", sha256Hex(Files.readAllBytes(root.resolve(input))));
            inputDigests.add(entry);
        }

        ObjectNode sourceBindings = NODE.objectNode();
        sourceBindings.put("shared_source_commit_sha",
                forwardBinding.get("source_commit_sha").asText());
        sourceBindings.put("shared_graph_sha256", forwardBinding.get("graph_sha256").asText());
        sourceBindings.set("reverse_delivery_history_sha256",
                required(reverseBinding.get("delivery_history_sha256"), "delivery_history_sha256"));

        ObjectNode itemAccounting = NODE.objectNode();
        itemAccounting.put("forward_mapping_proposals", 9);
        itemAccounting.put("forward_unresolved_results", 1);
        itemAccounting.put("reverse_hypotheses", 5);
        itemAccounting.put("total_packet_items", packetItems.size());

        ObjectNode blinding = NODE.objectNode();
        blinding.put("scope", "DETERMINISTIC_LABEL_AND_ORDER_BLINDING");
        blinding.put("explicit_arm_labels_absent", true);
        blinding.put("source_identifiers_absent", true);
        blinding.put("content_level_arm_anonymity_claimed", false);
        blinding.put("limitation", "ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT");

        ObjectNode isolation = NODE.objectNode();
        isolation.put("packet_contains_explicit_arm_labels", false);
        isolation.put("packet_contains_source_identifiers", false);
        isolation.put("sealed_key_in_reviewer_workspaces", false);
        isolation.put("future_judgments_shared_between_reviewers", false);

        ObjectNode decisionBoundary = NODE.objectNode();
        decisionBoundary.put("judgments_fabricated", false);
        decisionBoundary.put("product_team_human_review_completed", false);
        decisionBoundary.put("final_go_revise_stop_decision_made", false);

        ObjectNode manifest = NODE.objectNode();
        manifest.put("schema_version", "pkb001.task6.blind-review-manifest.v1");
        manifest.put("packet_id", packet.get("packet_id").asText());
        manifest.set("source_bindings", sourceBindings);
        manifest.set("input_digests", inputDigests);
        manifest.put("packet_sha256", packetDigest);
        manifest.put("sealed_key_sha256", sha256Hex(jsonBytes(key)));
        manifest.set("item_accounting", itemAccounting);
        manifest.set("reviewer_workspaces", textArray(REVIEWER_WORKSPACES));
        manifest.set("blinding", blinding);
        manifest.set("isolation", isolation);
        manifest.set("decision_boundary", decisionBoundary);

        return new Task6Packet(packet, key, manifest);
    }

    public static ObjectNode writeTask6Artifacts(Path root) throws IOException {
        return writeTask6Artifacts(root, Path.of(TASK6_DIR));
    }

    public static ObjectNode writeTask6Artifacts(Path root, Path outputDir) throws IOException {
        Path destination = root.resolve(outputDir);
        protectExistingJudgments(destination);
        Task6Packet built = buildTask6Packet(root);
        ObjectNode packet = built.packet();
        ObjectNode key = built.key();
        ObjectNode manifest = built.manifest();

        Files.createDirectories(destination);
        byte[] packetBytes = jsonBytes(packet);
        Files.write(destination.resolve("blind-review-packet.json"), packetBytes);
        Files.write(destination.resolve("sealed-blind-key.json"), jsonBytes(key));
        Files.write(destination.resolve("manifest.json"), jsonBytes(manifest));
        Files.writeString(destination.resolve("reviewer-instructions.md"),
                reviewerInstructions(), StandardCharsets.UTF_8);

        for (JsonNode workspaceId : requiredArray(manifest.get("reviewer_workspaces"),
                "reviewer_workspaces")) {
            Path workspace = destination.resolve("judgment-workspaces")
                    .resolve(workspaceId.asText());
            Files.createDirectories(workspace);
            Files.write(workspace.resolve("packet-input.json"), packetBytes);
            Files.write(workspace.resolve("judgment-template.json"),
                    jsonBytes(reviewerTemplate(manifest.get("packet_sha256").asText(),
                            workspaceId.asText())));
        }
        return manifest;
    }

    public static String reviewerInstructions() {
        return """
                # PKB-001 deterministic label/order-blinded comparison instructions

                This packet removes explicit source-arm labels and deterministically obscures source ordering. It does not provide content-level arm anonymity: `ARM_INFERENCE_POSSIBLE_FROM_EVIDENCE_CONTENT`. Do not use evidence categories or values to infer an arm, and do not access the sealed identity key.

                For each item, record one frozen review action (`ACCEPT`, `RENAME`, `MERGE`, `SPLIT`, `REJECT`, or `ADD_MISSING`) and an evidence outcome. Record evidence validity, usefulness, unsupported claims, precision, limitations, and active review time. Leave a clear note when a claim exceeds the supplied evidence.

                Expected realization scoring is evaluator-only: it may compare a blinded item against separately sealed expected realizations for measurement. It must not expose those realizations to a Product Team meaning review and cannot create Product truth.

                Product meaning judgment is different. Only the human Product Team can finalize Product meaning, accepted terminology, boundaries, merges, splits, or publication. Upcoming AI evaluator contexts are `NON_HUMAN`; they can assist with evaluator-only scoring but cannot complete Product Team human review.

                Do not record a final GO / REVISE / STOP decision in either workspace.
                """;
    }

    public static ObjectNode reviewerTemplate(String packetDigest, String workspaceId) {
        ObjectNode reviewerContext = NODE.objectNode();
        reviewerContext.put("actor_type", "NON_HUMAN");
        reviewerContext.put("authority", "EVALUATOR_ONLY");
        reviewerContext.put("can_complete_product_team_review", false);

        ObjectNode reviewerIsolation = NODE.objectNode();
        reviewerIsolation.put("other_workspace_future_judgments_accessible", false);
        reviewerIsolation.put("sealed_key_accessible", false);
        reviewerIsolation.put("ground_truth_accessible_from_packet_workspace", false);

        ObjectNode entryTemplate = NODE.objectNode();
        entryTemplate.put("blind_id", "BR-###");
        entryTemplate.setAll(emptyJudgment());

        ObjectNode template = NODE.objectNode();
        template.put("schema_version", "pkb001.task6.blind-judgment-workspace.v1");
        template.put("workspace_id", workspaceId);
        template.put("packet_sha256", packetDigest);
        template.set("reviewer_context", reviewerContext);
        template.set("reviewer_isolation", reviewerIsolation);
        template.set("judgments", NODE.arrayNode());
        template.set("entry_template", entryTemplate);
        return template;
    }

    // ------------------------------------------------------------------
    // Legacy pre-Task-6 seam
    // ------------------------------------------------------------------

    public static LegacyBlindPacket buildBlindPacket(String runId, List<Map<String, Object>> outputs) {
        List<ObjectNode> proposals = new ArrayList<>();
        for (Map<String, Object> output : outputs) {
            for (Object proposalValue : asList(output.get("proposals"))) {
                Map<String, Object> proposal = asMap(proposalValue);
                ObjectNode merged = NODE.objectNode();
                proposal.forEach((name, value) -> merged.set(name, JSON.valueToTree(value)));
                merged.set("arm", JSON.valueToTree(
                        proposal.containsKey("arm") ? proposal.get("arm") : output.get("arm")));
                merged.put("source_kind", "CODE_BASELINE");
                proposals.add(merged);
            }
            for (Object mappingValue : asList(output.get("mappings"))) {
                Map<String, Object> mapping = asMap(mappingValue);
                ObjectNode merged = NODE.objectNode();
                mapping.forEach((name, value) -> merged.set(name, JSON.valueToTree(value)));
                merged.set("proposal_id", JSON.valueToTree(mapping.get("capability_id")));
                merged.set("label", JSON.valueToTree(mapping.get("capability_name")));
                merged.putNull("arm");
                merged.put("source_kind", "FORWARD_SKILL");
                proposals.add(merged);
            }
            for (Object hypothesisValue : asList(output.get("hypotheses"))) {
                Map<String, Object> hypothesis = asMap(hypothesisValue);
                ObjectNode merged = NODE.objectNode();
                hypothesis.forEach((name, value) -> merged.set(name, JSON.valueToTree(value)));
                merged.set("proposal_id", JSON.valueToTree(hypothesis.get("hypothesis_id")));
                merged.putNull("arm");
                merged.put("source_kind", "REVERSE_SKILL");
                proposals.add(merged);
            }
        }
        proposals.sort(Comparator.comparing(proposal ->
                sha256Hex((runId + "\0" + proposal.get("proposal_id").asText())
                        .getBytes(StandardCharsets.UTF_8))));

        ArrayNode packetItems = NODE.arrayNode();
        ArrayNode keyItems = NODE.arrayNode();
        for (int index = 0; index < proposals.size(); index++) {
            String blindId = String.format("BR-%03d", index + 1);
            ObjectNode proposal = proposals.get(index);
            ObjectNode item = NODE.objectNode();
            item.put("blind_id", blindId);
            item.set("candidate_capability", proposal.get("label"));
            item.set("component_refs", proposal.get("component_refs"));
            item.set("evidence_refs", proposal.get("evidence_refs"));
            item.set("limitations", proposal.get("limitations"));
            item.putNull("outcome");
            item.putNull("review_action");
            item.putNull("suggested_name");
            item.putNull("reviewer_notes");
            packetItems.add(item);

            ObjectNode keyItem = NODE.objectNode();
            keyItem.put("blind_id", blindId);
            keyItem.set("proposal_id", proposal.get("proposal_id"));
            keyItem.set("arm", proposal.get("arm"));
            keyItem.put("source_kind", proposal.get("source_kind").asText());
            keyItems.add(keyItem);
        }

        ObjectNode packet = NODE.objectNode();
        packet.put("packet_id", runId + "-blind-review-v1");
        packet.put("review_status", "AWAITING_HUMAN_INPUT");
        packet.put("instructions", "Review each candidate without consulting evaluator ground truth.");
        packet.set("allowed_outcomes", textArray(OUTCOMES));
        packet.set("allowed_review_actions", textArray(ACTIONS));
        packet.set("items", packetItems);

        ObjectNode key = NODE.objectNode();
        key.put("key_id", runId + "-blind-key-v1");
        key.put("visibility", "EVALUATOR_ONLY");
        key.set("items", keyItems);
        return new LegacyBlindPacket(packet, key);
    }

    // ------------------------------------------------------------------
    // Deterministic rendering and digests
    // ------------------------------------------------------------------

    public static String sha256(byte[] bytes) {
        return sha256Hex(bytes);
    }

    static String sha256Hex(byte[] bytes) {
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
     * Renders exactly like the Python consumer's {@code json_bytes}:
     * {@code json.dumps(value, indent=2, ensure_ascii=False, sort_keys=True) + '\n'}.
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
            writeString(out, node.textValue());
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
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        names.sort(BlindReview::compareCodePoints);
        out.append("{\n");
        for (int index = 0; index < names.size(); index++) {
            indent(out, indent + 1);
            writeString(out, names.get(index));
            out.append(": ");
            writeNode(out, node.get(names.get(index)), indent + 1);
            if (index + 1 < names.size()) {
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

    private static int compareCodePoints(String left, String right) {
        int leftIndex = 0;
        int rightIndex = 0;
        while (leftIndex < left.length() && rightIndex < right.length()) {
            int leftPoint = left.codePointAt(leftIndex);
            int rightPoint = right.codePointAt(rightIndex);
            if (leftPoint != rightPoint) {
                return Integer.compare(leftPoint, rightPoint);
            }
            leftIndex += Character.charCount(leftPoint);
            rightIndex += Character.charCount(rightPoint);
        }
        return Integer.compare(left.codePointCount(0, left.length()),
                right.codePointCount(0, right.length()));
    }

    private static void writeString(StringBuilder out, String value) {
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
                    } else {
                        out.append(character);
                    }
                }
            }
        }
        out.append('"');
    }

    /**
     * Formats a double like Python 3 {@code repr(float)} (what
     * {@code json.dumps} emits for floats): shortest round-trip digits,
     * decimal notation for {@code 1e-4 <= |x| < 1e16}, exponential notation
     * with a signed, at-least-two-digit exponent otherwise.
     */
    static String pythonFloat(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("non-finite float is not valid JSON");
        }
        double absolute = Math.abs(value);
        String shortest = Double.toString(value);
        boolean decimalRange = absolute == 0.0 || (absolute >= 1e-4 && absolute < 1e16);
        if (decimalRange) {
            if (shortest.indexOf('E') >= 0 || shortest.indexOf('e') >= 0) {
                return new BigDecimal(shortest).toPlainString();
            }
            return shortest;
        }
        String scientific = new BigDecimal(shortest).toString();
        int exponentAt = Math.max(scientific.indexOf('E'), scientific.indexOf('e'));
        String mantissa = scientific.substring(0, exponentAt);
        if (mantissa.endsWith(".0")) {
            mantissa = mantissa.substring(0, mantissa.length() - 2);
        }
        int exponent = Integer.parseInt(scientific.substring(exponentAt + 1));
        return mantissa + "e" + (exponent < 0 ? "-" : "+")
                + String.format("%02d", Math.abs(exponent));
    }

    // ------------------------------------------------------------------
    // Sealed-input verification
    // ------------------------------------------------------------------

    static void verifyForwardRun(Path root, ObjectNode artifact, ObjectNode manifest,
                                 ObjectNode witness) throws IOException {
        Path artifactPath = root.resolve(FORWARD_ARTIFACT);
        require(textOrNull(artifact.get("execution_kind")) != null
                && textOrNull(artifact.get("execution_kind")).equals("SKILL_EXECUTION"),
                "forward execution kind is not a skill execution");
        require(isTextEqual(artifact.get("run_status"), "COMPLETED"),
                "forward run is not completed");
        require(isTextEqual(artifact.get("authority"), "PROPOSAL_ONLY"),
                "forward run is not proposal-only");
        require(isTextEqual(manifest.get("authority"), "PROPOSAL_ONLY"),
                "forward manifest is not proposal-only");
        require(textOrNull(manifest.get("artifact_sha256")) != null
                && textOrNull(manifest.get("artifact_sha256"))
                        .equals(sha256Hex(Files.readAllBytes(artifactPath))),
                "forward artifact digest is not bound");
        require(textOrNull(witness.path("outputs").path("artifact").get("sha256")) != null
                && textOrNull(witness.path("outputs").path("artifact").get("sha256"))
                        .equals(sha256Hex(Files.readAllBytes(artifactPath))),
                "forward witness artifact digest is not bound");
        require(isFalse(artifact.path("input_policy").get("forbidden_inputs_accessed")),
                "forward artifact did not attest non-access");
        require(isFalse(manifest.get("forbidden_inputs_accessed")),
                "forward manifest did not attest non-access");
        require(isFalse(witness.get("forbidden_inputs_accessed")),
                "forward witness did not attest non-access");
        require(isTextEqual(witness.path("execution").get("authority"), "PROPOSAL_ONLY"),
                "forward witness authority differs");
        require(isTextEqual(witness.get("assurance_level"), "ATTESTATION_NOT_CRYPTOGRAPHIC_PROOF"),
                "forward witness assurance limit is missing");
        verifyRecordedInputs(root, manifest.get("visible_input_sha256"), "forward manifest");

        JsonNode bindings = artifact.get("bindings");
        require(Objects.equals(bindings == null ? null : bindings.get("source_commit_sha"),
                        manifest.get("source_commit_sha")),
                "forward source_commit_sha differs from manifest");
        require(Objects.equals(bindings == null ? null : bindings.get("graph_sha256"),
                        manifest.get("graph_sha256")),
                "forward graph_sha256 differs from manifest");

        ArrayNode results = requiredArray(artifact.get("capability_results"), "capability_results");
        int capabilityResultCount = manifest.get("capability_result_count") == null
                ? -1 : manifest.get("capability_result_count").asInt();
        require(results.size() == capabilityResultCount && capabilityResultCount == 10,
                "forward item count is not 10");
        int mappings = 0;
        int unresolved = 0;
        for (JsonNode item : results) {
            if (isTextEqual(item.get("outcome"), "MAPPING_PROPOSAL")) {
                mappings++;
            }
            if (isTextEqual(item.get("outcome"), "UNRESOLVED")) {
                unresolved++;
            }
        }
        require(mappings == 9, "forward mapping count is not 9");
        require(unresolved == 1, "forward unresolved count is not 1");
        for (JsonNode item : results) {
            require(isTextEqual(item.get("mapping_status"), "PROPOSAL_ONLY"),
                    "forward item authority is not proposal-only");
        }
    }

    static void verifyReverseRun(Path root, ObjectNode artifact, ObjectNode manifest,
                                 ObjectNode witness) throws IOException {
        Path artifactPath = root.resolve(REVERSE_ARTIFACT);
        require(isTextEqual(artifact.get("authority"), "PROPOSAL_ONLY"),
                "reverse run is not proposal-only");
        require(isTrue(artifact.get("human_review_required")),
                "reverse run does not require human review");
        require(isTextEqual(manifest.get("authority"), "PROPOSAL_ONLY"),
                "reverse manifest is not proposal-only");
        require(isTextEqual(witness.get("authority"), "PROPOSAL_ONLY"),
                "reverse witness is not proposal-only");
        require(isFalse(manifest.get("forbidden_inputs_accessed")),
                "reverse manifest did not attest non-access");
        require(isFalse(witness.get("forbidden_inputs_accessed")),
                "reverse witness did not attest non-access");
        require(isFalse(witness.get("product_semantics_visible")),
                "reverse witness permits Product Semantics");
        require(isFalse(witness.get("post_cutoff_knowledge_used")),
                "reverse witness permits post-cutoff knowledge");
        require(isTextEqual(witness.get("attestation_class"), "ATTESTATION_NOT_CRYPTOGRAPHIC_PROOF"),
                "reverse witness assurance limit is missing");
        JsonNode allowlist = manifest.get("visible_input_allowlist");
        verifyRecordedInputs(root, allowlist, "reverse manifest");

        Map<String, JsonNode> expectedDigests = new LinkedHashMap<>();
        if (allowlist != null) {
            for (JsonNode entry : allowlist) {
                expectedDigests.put(entry.get("path").asText(), entry.get("sha256"));
            }
        }
        JsonNode actualDigests = witness.get("allowed_input_digests");
        boolean digestsMatch = actualDigests != null && actualDigests.isObject()
                && actualDigests.size() == expectedDigests.size();
        if (digestsMatch) {
            for (Map.Entry<String, JsonNode> expected : expectedDigests.entrySet()) {
                if (!Objects.equals(expected.getValue(), actualDigests.get(expected.getKey()))) {
                    digestsMatch = false;
                    break;
                }
            }
        }
        require(digestsMatch, "reverse witness input digests differ from manifest");

        JsonNode primaryDigests = witness.get("primary_output_digests");
        require(primaryDigests != null && primaryDigests.isObject()
                        && Objects.equals(primaryDigests.get(REVERSE_ARTIFACT),
                                TextNode.valueOf(sha256Hex(Files.readAllBytes(artifactPath)))),
                "reverse witness artifact digest is not bound");
        require(isTextEqual(witness.get("manifest_sha256"),
                        sha256Hex(Files.readAllBytes(root.resolve(REVERSE_MANIFEST)))),
                "reverse witness manifest digest is not bound");

        ArrayNode hypotheses = requiredArray(artifact.get("hypotheses"), "hypotheses");
        require(hypotheses.size() == 5, "reverse hypothesis count is not 5");
        for (JsonNode item : hypotheses) {
            require(isTextEqual(item.get("authority"), "PROPOSAL_ONLY"),
                    "reverse item authority is not proposal-only");
        }
    }

    private static void verifyRecordedInputs(Path root, JsonNode entries, String label)
            throws IOException {
        if (entries == null) {
            return;
        }
        if (entries.isObject()) {
            List<String> names = new ArrayList<>();
            entries.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                verifyRecordedInput(root, name, entries.get(name), label);
            }
            return;
        }
        for (JsonNode entry : entries) {
            verifyRecordedInput(root, entry.get("path").asText(), entry.get("sha256"), label);
        }
    }

    private static void verifyRecordedInput(Path root, String relative, JsonNode expected,
                                            String label) throws IOException {
        Path path = root.resolve(relative);
        require(Files.isRegularFile(path), label + " input is missing: " + relative);
        require(expected != null && expected.isTextual()
                        && expected.asText().equals(sha256Hex(Files.readAllBytes(path))),
                label + " input digest mismatch: " + relative);
    }

    static void protectExistingJudgments(Path destination) throws IOException {
        for (String reviewer : REVIEWER_WORKSPACES) {
            Path path = destination.resolve("judgment-workspaces")
                    .resolve(reviewer).resolve("judgment-template.json");
            if (!Files.exists(path)) {
                continue;
            }
            JsonNode template;
            try {
                template = JSON.readTree(path.toFile());
            } catch (IOException failure) {
                throw new BlindReviewBindingException(
                        "cannot safely inspect existing judgment workspace " + path + ": "
                                + failure.getMessage(), failure);
            }
            if (template != null && !template.isObject()) {
                throw new BlindReviewBindingException(
                        "cannot safely inspect existing judgment workspace " + path + ": "
                                + "judgment workspace is not a JSON object");
            }
            if (template != null && isTruthy(template.get("judgments"))) {
                throw new BlindReviewBindingException(
                        "refusing to overwrite completed judgments; initialize a new "
                                + "version with an explicit --output-dir");
            }
        }
    }

    // ------------------------------------------------------------------
    // Candidates
    // ------------------------------------------------------------------

    private static ObjectNode forwardCandidate(JsonNode result) {
        ArrayNode components = NODE.arrayNode();
        JsonNode proposedComponents = result.get("proposed_components");
        if (proposedComponents != null) {
            for (JsonNode item : proposedComponents) {
                components.add(componentRecord(
                        item.get("graph_node_id").asText(),
                        item.get("source_location").asText(),
                        "CANDIDATE_COMPONENT"));
            }
        }
        if (components.isEmpty()) {
            JsonNode partial = result.get("evidence_refs").get(0);
            components.add(componentRecord(
                    partial.get("graph_node_id").asText(),
                    partial.get("source_location").asText(),
                    "INCOMPLETE_EVIDENCE"));
        }

        ArrayNode evidence = NODE.arrayNode();
        JsonNode evidenceRefs = result.get("evidence_refs");
        if (evidenceRefs != null) {
            for (JsonNode item : evidenceRefs) {
                evidence.add(evidenceRecord("STRUCTURAL",
                        item.get("graph_node_id").asText(), item.get("source_location").asText()));
            }
        }

        JsonNode basis = result.has("reasoning") ? result.get("reasoning")
                : (result.has("unresolved_reason") ? result.get("unresolved_reason")
                        : TextNode.valueOf(""));

        ObjectNode candidate = NODE.objectNode();
        candidate.put("source_identifier", result.get("capability_id").asText());
        candidate.put("source_arm", "FORWARD");
        candidate.put("candidate_capability", result.get("name").asText());
        candidate.put("complete_realization_proposed",
                isTextEqual(result.get("outcome"), "MAPPING_PROPOSAL"));
        candidate.set("component_refs", components);
        candidate.set("evidence_refs", evidence);
        candidate.set("confidence_score", result.get("confidence"));
        candidate.set("limitations", result.get("limitations"));
        candidate.set("candidate_basis", basis);
        return candidate;
    }

    private static ObjectNode reverseCandidate(JsonNode hypothesis, Map<String, JsonNode> graphNodes) {
        JsonNode refs = hypothesis.get("evidence_refs");
        ArrayNode components = NODE.arrayNode();
        ArrayNode evidence = NODE.arrayNode();
        for (JsonNode nodeId : requiredArray(refs.get("graph_node_ids"), "graph_node_ids")) {
            JsonNode node = graphNodes.get(nodeId.asText());
            components.add(componentRecord(nodeId.asText(),
                    node.get("source_location").asText(), "CANDIDATE_COMPONENT"));
            evidence.add(evidenceRecord("STRUCTURAL", nodeId.asText(),
                    node.get("source_location").asText()));
        }
        for (JsonNode edge : requiredArray(refs.get("graph_edges"), "graph_edges")) {
            evidence.add(evidenceRecord("STRUCTURAL",
                    edge.get("source").asText(),
                    graphNodes.get(edge.get("source").asText()).get("source_location").asText(),
                    edge.get("target").asText(),
                    edge.get("relation").asText()));
        }
        for (JsonNode commitSha : requiredArray(refs.get("commit_shas"), "commit_shas")) {
            evidence.add(evidenceRecord("DELIVERY", commitSha.asText()));
        }
        for (JsonNode number : requiredArray(refs.get("pull_request_numbers"), "pull_request_numbers")) {
            evidence.add(evidenceRecord("DELIVERY", number.asText()));
        }
        for (JsonNode change : requiredArray(refs.get("changed_path_refs"), "changed_path_refs")) {
            evidence.add(evidenceRecord("DELIVERY",
                    change.get("commit_sha").asText(), change.get("path").asText()));
        }

        ObjectNode candidate = NODE.objectNode();
        candidate.put("source_identifier", hypothesis.get("proposal_id").asText());
        candidate.put("source_arm", "REVERSE");
        candidate.put("candidate_capability", hypothesis.get("label").asText());
        candidate.put("complete_realization_proposed", true);
        candidate.set("component_refs", components);
        candidate.set("evidence_refs", evidence);
        candidate.set("confidence_score", hypothesis.get("confidence").get("score"));
        candidate.set("limitations", hypothesis.get("limitations"));
        candidate.set("candidate_basis", hypothesis.get("structural_delivery_convergence"));
        return candidate;
    }

    private static ObjectNode componentRecord(String reference, String sourceLocation,
                                              String evidenceState) {
        ObjectNode record = NODE.objectNode();
        record.put("reference", reference);
        record.put("source_location", sourceLocation);
        record.put("evidence_state", evidenceState);
        return record;
    }

    private static ObjectNode evidenceRecord(String category, String reference) {
        return evidenceRecord(category, reference, "", "", "");
    }

    private static ObjectNode evidenceRecord(String category, String reference,
                                             String sourceLocation) {
        return evidenceRecord(category, reference, sourceLocation, "", "");
    }

    private static ObjectNode evidenceRecord(String category, String reference,
                                             String sourceLocation, String relatedReference,
                                             String relation) {
        ObjectNode record = NODE.objectNode();
        record.put("evidence_category", category);
        record.put("reference", reference);
        record.put("source_location", sourceLocation);
        record.put("related_reference", relatedReference);
        record.put("relation", relation);
        record.put("availability", "EVIDENCED");
        return record;
    }

    // ------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------

    static ObjectNode emptyJudgment() {
        ObjectNode judgment = NODE.objectNode();
        judgment.putNull("outcome");
        judgment.putNull("review_action");
        judgment.putNull("suggested_name");
        judgment.putNull("evidence_validity");
        judgment.putNull("usefulness");
        judgment.putNull("unsupported_claims");
        judgment.putNull("precision");
        judgment.putNull("limitations");
        judgment.putNull("active_review_seconds");
        judgment.putNull("reviewer_notes");
        return judgment;
    }

    private static ArrayNode textArray(List<String> values) {
        ArrayNode array = NODE.arrayNode();
        values.forEach(array::add);
        return array;
    }

    private static ObjectNode readObject(Path root, String relative) throws IOException {
        JsonNode node = JSON.readTree(root.resolve(relative).toFile());
        if (!(node instanceof ObjectNode object)) {
            throw new IOException(relative + " is not a JSON object");
        }
        return object;
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

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new BlindReviewBindingException(message);
        }
    }

    private static boolean isTextEqual(JsonNode node, String expected) {
        return node != null && node.isTextual() && node.asText().equals(expected);
    }

    private static String textOrNull(JsonNode node) {
        return node != null && node.isTextual() ? node.asText() : null;
    }

    private static boolean isFalse(JsonNode node) {
        return node != null && node.isBoolean() && !node.booleanValue();
    }

    private static boolean isTrue(JsonNode node) {
        return node != null && node.isBoolean() && node.booleanValue();
    }

    private static boolean isTruthy(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return false;
        }
        if (node.isArray() || node.isObject()) {
            return node.size() > 0;
        }
        if (node.isTextual()) {
            return !node.asText().isEmpty();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isNumber()) {
            return node.decimalValue().signum() != 0;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new NullPointerException("expected a JSON-like map but got: " + value);
        }
        return (Map<String, Object>) map;
    }

    private static List<?> asList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new NullPointerException("expected a JSON-like list but got: " + value);
        }
        return list;
    }
}

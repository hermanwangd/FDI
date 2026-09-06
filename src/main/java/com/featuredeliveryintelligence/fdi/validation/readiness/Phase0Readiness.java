package com.featuredeliveryintelligence.fdi.validation.readiness;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Fail-closed PKB-001 Phase 0 readiness gate. Ports the observable behavior of
 * the transitional Python consumer {@code tooling/validation/pkb001_gate.py}:
 * the P0-01..P0-05 prerequisite checkers with their exact reason strings and
 * MISSING/MISMATCH/SATISFIED statuses, {@code _safe_file} (symlink rejection,
 * containment under the resolved root, regular file), the seal-vs-evidence
 * field matching of {@code _ground_truth} (reviewer role dict validation,
 * ordering dict, vocabulary set), readiness flags with PK_S1/PK_S2 evaluated
 * independently of prerequisite status, the review-state selection, and the
 * report key order {@code experiment, status, readiness_state,
 * readiness_flags, prerequisites, review_state}.
 * {@link IllegalArgumentException} mirrors the Python {@code ValueError}
 * vocabulary; file IO raises {@link IOException} like the Python
 * {@code OSError} surface.
 */
public final class Phase0Readiness {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern GIT_SHA = Pattern.compile("[0-9a-f]{40}");
    private static final Set<String> VOCABULARY = Set.of(
            "ACCEPT", "RENAME", "MERGE", "SPLIT", "REJECT", "ADD_MISSING");

    /**
     * Ports {@code evaluate_readiness(root, evidence)}. {@code root} is
     * resolved like the Python {@code Path(root).resolve()}; {@code evidence}
     * is the parsed evidence object (an empty object when no evidence file
     * was supplied).
     */
    public ObjectNode evaluate(Path root, JsonNode evidence) throws IOException {
        root = root.toRealPath();
        ObjectNode evidenceObject = evidence != null && evidence.isObject()
                ? (ObjectNode) evidence : JSON.createObjectNode();
        ArrayNode prerequisites = JSON.createArrayNode();
        ObjectNode productSemantics = productSemantics(root, evidenceObject.get("product_semantics"));
        ObjectNode graphify = graphify(evidenceObject.get("graphify"));
        ObjectNode skills = skills(root, evidenceObject.get("skills"));
        ObjectNode calibration = calibration(evidenceObject.get("calibration"));
        ObjectNode groundTruth = groundTruth(root, evidenceObject.get("ground_truth"));
        prerequisites.add(productSemantics);
        prerequisites.add(graphify);
        prerequisites.add(skills);
        prerequisites.add(calibration);
        prerequisites.add(groundTruth);

        boolean ready = true;
        for (JsonNode item : prerequisites) {
            if (!"SATISFIED".equals(item.get("status").asText())) {
                ready = false;
            }
        }
        ObjectNode skillsValue = evidenceObject.get("skills") != null
                && evidenceObject.get("skills").isObject()
                ? (ObjectNode) evidenceObject.get("skills") : JSON.createObjectNode();
        ObjectNode flags = JSON.createObjectNode();
        flags.set("PRODUCT_SEMANTICS_FROZEN", BooleanNode.valueOf(
                "SATISFIED".equals(productSemantics.get("status").asText())));
        flags.set("LIVE_GRAPHIFY_INTERFACE_VERIFIED", BooleanNode.valueOf(
                "SATISFIED".equals(graphify.get("status").asText())));
        flags.set("PK_S1_EXECUTION_READY", BooleanNode.valueOf(pkS1Ready(root, skillsValue)));
        flags.set("PK_S2_EXECUTION_READY", BooleanNode.valueOf(pkS2Ready(root, skillsValue)));
        flags.set("CALIBRATION_DATASET_FROZEN", BooleanNode.valueOf(
                "SATISFIED".equals(calibration.get("status").asText())));
        flags.set("GROUND_TRUTH_SEALED", BooleanNode.valueOf(
                "SATISFIED".equals(groundTruth.get("status").asText())));

        ObjectNode reviewState = JSON.createObjectNode();
        if (flags.get("GROUND_TRUTH_SEALED").asBoolean()) {
            reviewState.put("phase0_protocol_actors", "INDEPENDENT_AI_AGENT_CONTEXTS");
            reviewState.put("non_human_review_completed", true);
            reviewState.put("human_review_status", "PENDING_POST_GENERATION_SECTION_6");
        } else {
            reviewState.put("phase0_protocol_actors", "UNVERIFIED");
            reviewState.put("non_human_review_completed", false);
            reviewState.put("human_review_status", "UNVERIFIED");
        }

        ObjectNode report = JSON.createObjectNode();
        report.put("experiment", "PKB-001");
        report.put("status", ready ? "READY" : "BLOCKED");
        report.put("readiness_state", ready ? "READY" : "NOT_READY");
        report.set("readiness_flags", flags);
        report.set("prerequisites", prerequisites);
        report.set("review_state", reviewState);
        return report;
    }

    /** Ports {@code _safe_file}: containment under the resolved root, no symlink, regular file. */
    static Path safeFile(Path root, JsonNode relative) {
        if (relative == null || !relative.isTextual() || relative.asText().isEmpty()
                || Path.of(relative.asText()).isAbsolute()) {
            return null;
        }
        Path candidate = root.resolve(relative.asText());
        Path resolved;
        try {
            resolved = candidate.toRealPath();
        } catch (IOException | RuntimeException failure) {
            return null;
        }
        if (!resolved.startsWith(root)) {
            return null;
        }
        try {
            if (Files.isSymbolicLink(candidate) || !Files.isRegularFile(resolved)) {
                return null;
            }
        } catch (SecurityException failure) {
            return null;
        }
        return resolved;
    }

    /**
     * Ports {@code _safe_output}: the output must resolve inside the resolved
     * root and must not be a symlink.
     */
    public static Path safeOutput(Path root, Path requested) {
        Path candidate = requested.isAbsolute() ? requested : root.resolve(requested.toString());
        Path resolved = resolveLoose(candidate);
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("output path must remain inside repository root");
        }
        if (Files.isSymbolicLink(candidate)) {
            throw new IllegalArgumentException("output path must not be a symlink");
        }
        return resolved;
    }

    /**
     * Python {@code Path.resolve(strict=False)} equivalent: resolves symlinks
     * of the longest existing prefix and appends the normalized remainder.
     */
    public static Path resolveLoose(Path candidate) {
        Path normalized = candidate.toAbsolutePath().normalize();
        Path existing = normalized;
        java.util.Deque<String> tail = new java.util.ArrayDeque<>();
        while (existing != null && !Files.exists(existing)) {
            tail.addFirst(existing.getFileName().toString());
            existing = existing.getParent();
        }
        if (existing == null) {
            return normalized;
        }
        Path resolved = existing;
        if (!Files.exists(existing, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(existing) || Files.isRegularFile(existing)
                || Files.isDirectory(existing)) {
            try {
                resolved = existing.toRealPath();
            } catch (IOException | RuntimeException failure) {
                resolved = existing;
            }
        }
        for (String part : tail) {
            resolved = resolved.resolve(part);
        }
        return resolved;
    }

    private static ObjectNode item(String identifier, String status, String reason) {
        ObjectNode node = JSON.createObjectNode();
        node.put("id", identifier);
        node.put("status", status);
        node.put("reason", reason);
        return node;
    }

    private static ObjectNode productSemantics(Path root, JsonNode value) throws IOException {
        if (value == null || !value.isObject()) {
            return item("P0-01", "MISSING", "Product Semantics evidence is absent");
        }
        ObjectNode object = (ObjectNode) value;
        Path path = safeFile(root, object.get("path"));
        JsonNode digest = object.get("sha256");
        if (path == null || digest == null || !digest.isTextual()
                || !SHA256.matcher(digest.asText()).matches()) {
            return item("P0-01", "MISMATCH", "Product Semantics path or SHA-256 is invalid");
        }
        if (!sha256Hex(Files.readAllBytes(path)).equals(digest.asText())) {
            return item("P0-01", "MISMATCH", "Product Semantics SHA-256 does not match bytes");
        }
        boolean valid = textEquals(object.get("status"), "FROZEN")
                && textEquals(object.get("owner"), "PRODUCT_TEAM");
        return item("P0-01", valid ? "SATISFIED" : "MISMATCH",
                valid ? "Product Semantics frozen by Product Team"
                        : "Product Semantics is not frozen by Product Team");
    }

    private static ObjectNode graphify(JsonNode value) {
        if (value == null || !value.isObject()) {
            return item("P0-02", "MISSING", "Graphify evidence is absent");
        }
        ObjectNode object = (ObjectNode) value;
        String[] strings = {"runtime_identity", "runtime_version", "transport", "wire_version",
                "source_location_provenance"};
        String[] digests = {"graph_sha256", "input_policy_sha256"};
        JsonNode operations = object.get("supported_operations");
        JsonNode proof = object.get("structural_proof");
        JsonNode binding = object.get("snapshot_binding");
        boolean valid = textEquals(object.get("result"), "EXACTLY_BOUND")
                && object.get("queryable") != null && object.get("queryable").isBoolean()
                && object.get("queryable").asBoolean()
                && allStrings(object, strings, false)
                && allStrings(object, digests, true)
                && operations != null && operations.isArray() && !operations.isEmpty()
                && allElementsTextualNonEmpty(operations)
                && isTrue(object.get("exact_revision_opened"))
                && proof != null && proof.isObject()
                && isTrue(proof.get("node_query"))
                && isTrue(proof.get("path_query"))
                && binding != null && binding.isObject()
                && binding.get("requested_revision") != null
                && binding.get("requested_revision").isTextual()
                && GIT_SHA.matcher(binding.get("requested_revision").asText()).matches()
                && Objects.equals(binding.get("indexed_revision"), binding.get("requested_revision"));
        return item("P0-02", valid ? "SATISFIED" : "MISMATCH",
                valid ? "exact Graphify binding verified"
                        : "Graphify evidence is incomplete or invalid");
    }

    private static ObjectNode skills(Path root, JsonNode value) throws IOException {
        if (value == null || !value.isObject()) {
            return item("P0-03", "MISSING", "Skill evidence is absent");
        }
        ObjectNode object = (ObjectNode) value;
        boolean valid = pkS1Ready(root, object) && pkS2Ready(root, object);
        return item("P0-03", valid ? "SATISFIED" : "MISMATCH",
                valid ? "PK-S1 and PK-S2 materialized" : "PK-S1 or PK-S2 is unavailable");
    }

    static boolean pkS1Ready(Path root, ObjectNode value) {
        return safeFile(root, value.get("pk_s1_path")) != null
                && textEquals(value.get("pk_s1_registration"), "REGISTERED_NON_GOVERNING");
    }

    static boolean pkS2Ready(Path root, ObjectNode value) throws IOException {
        Path history = safeFile(root, value.get("delivery_history_path"));
        JsonNode digest = value.get("delivery_history_sha256");
        return safeFile(root, value.get("pk_s2_path")) != null
                && textEquals(value.get("pk_s2_registration"), "REGISTERED_NON_GOVERNING")
                && history != null && digest != null && digest.isTextual()
                && SHA256.matcher(digest.asText()).matches()
                && sha256Hex(Files.readAllBytes(history)).equals(digest.asText())
                && textEquals(value.get("delivery_history_status"), "FROZEN")
                && textEquals(value.get("post_cutoff_knowledge_policy"), "EXCLUDE_AFTER_CUTOFF");
    }

    private static ObjectNode calibration(JsonNode value) {
        if (value == null || !value.isObject()) {
            return item("P0-04", "MISSING", "calibration dataset evidence is absent");
        }
        ObjectNode object = (ObjectNode) value;
        boolean valid = textEquals(object.get("status"), "FROZEN")
                && textEquals(object.get("resource_policy_status"), "FROZEN")
                && textEquals(object.get("post_cutoff_knowledge_policy"), "EXCLUDE_AFTER_CUTOFF");
        return item("P0-04", valid ? "SATISFIED" : "MISMATCH",
                valid ? "calibration dataset frozen"
                        : "calibration or post-cutoff policy is incomplete");
    }

    private static ObjectNode groundTruth(Path root, JsonNode value) throws IOException {
        if (value == null || !value.isObject()) {
            return item("P0-05", "MISSING", "evaluator ground truth evidence is absent");
        }
        ObjectNode object = (ObjectNode) value;
        JsonNode reviewers = object.get("reviewers");
        JsonNode reviewerRoles = object.get("reviewer_roles");
        JsonNode ordering = object.get("creation_before_generation_ordering");
        Path gold = safeFile(root, object.get("gold_path"));
        Path sealPath = safeFile(root, object.get("seal_path"));
        JsonNode digest = object.get("gold_sha256");
        JsonNode sealDigest = object.get("seal_sha256");
        JsonNode seal = null;
        if (sealPath != null) {
            try {
                seal = com.featuredeliveryintelligence.fdi.validation.codebaseline
                        .PythonJson.readTree(Files.readAllBytes(sealPath));
            } catch (IOException failure) {
                seal = null;
            } catch (IllegalArgumentException failure) {
                // Python catches only OSError/JSONDecodeError when loading the
                // seal; a UnicodeDecodeError (a ValueError) propagates to the
                // CLI error surface instead of degrading to "no seal".
                if (failure.getMessage() != null
                        && failure.getMessage().startsWith("'utf-8' codec")) {
                    throw failure;
                }
                seal = null;
            }
        }
        boolean sealedFieldsMatch = seal != null && seal.isObject() && sealMatches((ObjectNode) seal, object);
        boolean validAgentContexts = validAgentContexts(reviewers, reviewerRoles);
        boolean validOrdering = expectedOrdering().equals(ordering);
        boolean valid = textEquals(object.get("status"), "SEALED")
                && gold != null
                && sealPath != null
                && sealedFieldsMatch
                && digest != null && digest.isTextual()
                && SHA256.matcher(digest.asText()).matches()
                && sha256Hex(Files.readAllBytes(gold)).equals(digest.asText())
                && sealDigest != null && sealDigest.isTextual()
                && SHA256.matcher(sealDigest.asText()).matches()
                && sha256Hex(Files.readAllBytes(sealPath)).equals(sealDigest.asText())
                && textEquals(object.get("isolation_status"), "VERIFIED")
                && textEquals(object.get("review_protocol_status"), "FROZEN")
                && validAgentContexts
                && validOrdering
                && isFalse(object.get("human_review_completed"))
                && isTrue(object.get("non_human_review_completed"))
                && textEquals(object.get("human_review_status"), "PENDING_POST_GENERATION_SECTION_6")
                && vocabularyMatches(object.get("judgment_vocabulary"));
        return item("P0-05", valid ? "SATISFIED" : "MISMATCH",
                valid ? "evaluator ground truth sealed"
                        : "ground truth, isolation, or review protocol is incomplete");
    }

    private static boolean sealMatches(ObjectNode seal, ObjectNode value) {
        String[] keys = {"status", "gold_path", "gold_sha256", "isolation_status",
                "review_protocol_status", "reviewers", "reviewer_roles",
                "judgment_vocabulary", "creation_before_generation_ordering",
                "human_review_completed", "non_human_review_completed",
                "human_review_status"};
        for (String key : keys) {
            if (!Objects.equals(seal.get(key), value.get(key))) {
                return false;
            }
        }
        return true;
    }

    private static boolean validAgentContexts(JsonNode reviewers, JsonNode reviewerRoles) {
        if (reviewers == null || !reviewers.isArray() || reviewers.isEmpty()) {
            return false;
        }
        Set<String> reviewerSet = new LinkedHashSet<>();
        for (JsonNode reviewer : reviewers) {
            if (reviewer == null || !reviewer.isTextual()
                    || !reviewer.asText().startsWith("agent-context:")) {
                return false;
            }
            reviewerSet.add(reviewer.asText());
        }
        if (reviewerSet.size() < 2) {
            return false;
        }
        if (reviewerRoles == null || !reviewerRoles.isObject()) {
            return false;
        }
        ObjectNode roles = (ObjectNode) reviewerRoles;
        if (roles.size() != reviewerSet.size()) {
            return false;
        }
        for (String reviewer : reviewerSet) {
            JsonNode role = roles.get(reviewer);
            if (role == null || !role.isObject()) {
                return false;
            }
            ObjectNode roleObject = (ObjectNode) role;
            if (!textEquals(roleObject.get("actor_type"), "AI_AGENT_CONTEXT")
                    || !Objects.equals(roleObject.get("context_id"), textNode(reviewer))
                    || !isTrue(roleObject.get("independent_context"))
                    || roleObject.get("role") == null
                    || !roleObject.get("role").isTextual()
                    || roleObject.get("role").asText().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static JsonNode textNode(String value) {
        return JSON.getNodeFactory().textNode(value);
    }

    private static ObjectNode expectedOrdering() {
        ObjectNode ordering = JSON.createObjectNode();
        ordering.put("status", "VERIFIED");
        ordering.put("rule", "SEALED_BEFORE_VALID_EXPERIMENT_GENERATION");
        ordering.put("valid_experiment_generation_started", false);
        return ordering;
    }

    private static boolean vocabularyMatches(JsonNode vocabulary) {
        if (vocabulary == null || !vocabulary.isArray()) {
            return false;
        }
        Set<String> values = new LinkedHashSet<>();
        for (JsonNode item : vocabulary) {
            if (item == null || !item.isTextual()) {
                return false;
            }
            values.add(item.asText());
        }
        return values.equals(VOCABULARY);
    }

    private static boolean allStrings(ObjectNode object, String[] keys, boolean digest) {
        for (String key : keys) {
            JsonNode value = object.get(key);
            if (value == null || !value.isTextual()
                    || (digest ? !SHA256.matcher(value.asText()).matches()
                    : value.asText().isEmpty())) {
                return false;
            }
        }
        return true;
    }

    private static boolean allElementsTextualNonEmpty(JsonNode array) {
        for (JsonNode element : array) {
            if (element == null || !element.isTextual() || element.asText().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    static boolean textEquals(JsonNode node, String expected) {
        return node != null && node.isTextual() && node.asText().equals(expected);
    }

    private static boolean isTrue(JsonNode node) {
        return node != null && node.isBoolean() && node.asBoolean();
    }

    private static boolean isFalse(JsonNode node) {
        return node != null && node.isBoolean() && !node.asBoolean();
    }

    static String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}

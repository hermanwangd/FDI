package com.featuredeliveryintelligence.fdi.validation.csi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class CsiRecommendationValidator {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> DISPOSITIONS = Set.of(
            "RECOMMENDED_NOT_SELECTED", "REJECTED", "SUPERSEDED");
    private static final Set<String> TAGS = Set.of("CODE", "DELIVERY", "PK", "MIXED", "UNKNOWN");
    private static final Set<String> KPI_STATUSES = Set.of("OBSERVED", "UNKNOWN", "N/A", "INSUFFICIENT_SAMPLE");
    private static final Pattern FULL_REVISION = Pattern.compile("[0-9a-f]{40}");
    private static final Set<String> AUTHORITY_FIELDS = Set.of(
            "dispatch_authorized", "implementation_authorized", "mutation_authorized",
            "product_truth", "closure_authorized");

    public CsiValidationReport validate(JsonNode record) {
        List<String> invalid = new ArrayList<>();
        List<String> blocked = new ArrayList<>();
        if (!(record instanceof ObjectNode object)) {
            return new CsiValidationReport("INVALID", "", "", List.of("record must be a JSON object"));
        }
        AUTHORITY_FIELDS.forEach(field -> {
            if (object.has(field)) invalid.add("authority-bearing field is prohibited: " + field);
        });
        String id = requiredText(object, "recommendation_id", invalid);
        allowedText(object, "disposition", DISPOSITIONS, invalid);
        allowedText(object, "tag", TAGS, invalid);
        String affected = requiredText(object, "affected_requirement", invalid);
        String insufficiency = requiredText(object, "insufficiency", invalid);
        String control = requiredText(object, "proposed_control", invalid);
        String route = requiredText(object, "revision_route", invalid);
        requiredText(object, "execution_identity", invalid);
        requiredText(object, "verdict_identity", invalid);
        validateCandidate(object, invalid);
        validateEvidence(object.path("origin_evidence"), invalid, blocked);
        validateHandoff(object.get("handoff"), invalid, blocked);
        validateKpis(object.path("affected_kpis"), invalid);

        String key = "";
        if (!affected.isBlank() && !insufficiency.isBlank() && !control.isBlank() && !route.isBlank()) {
            key = duplicateKey(affected, insufficiency, control, route);
            if (object.has("duplicate_key") && !key.equals(object.path("duplicate_key").asText())) {
                invalid.add("duplicate_key does not match canonical semantic fields");
            }
        }
        if (!invalid.isEmpty()) return new CsiValidationReport("INVALID", id, key, invalid);
        if (!blocked.isEmpty()) return new CsiValidationReport("BLOCKED", id, key, blocked);
        return new CsiValidationReport("VALID", id, key, List.of());
    }

    public String duplicateKey(String affectedRequirement, String insufficiency,
                               String proposedControl, String revisionRoute) {
        ObjectNode canonical = JSON.createObjectNode();
        canonical.put("affected_requirement", normalize(affectedRequirement));
        canonical.put("insufficiency", normalize(insufficiency));
        canonical.put("proposed_control", normalize(proposedControl));
        canonical.put("revision_route", normalize(revisionRoute));
        try {
            byte[] bytes = JSON.writeValueAsBytes(canonical);
            return hex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException | java.io.IOException failure) {
            throw new IllegalStateException("cannot compute CSI duplicate key", failure);
        }
    }

    private static void validateCandidate(ObjectNode object, List<String> invalid) {
        String value = requiredText(object, "candidate_revision", invalid);
        boolean explainedNotApplicable = value.startsWith("N/A: ") && value.length() > 5;
        if (!value.isBlank() && !explainedNotApplicable && !FULL_REVISION.matcher(value).matches()) {
            invalid.add("candidate_revision must be a full 40-character Git revision or N/A with reason");
        }
    }

    private static void validateEvidence(JsonNode evidence, List<String> invalid, List<String> blocked) {
        if (!evidence.isArray() || evidence.isEmpty()) {
            invalid.add("origin_evidence must be a non-empty array");
            return;
        }
        Set<String> identities = new HashSet<>();
        for (int i = 0; i < evidence.size(); i++) {
            JsonNode item = evidence.get(i);
            if (!item.isObject()) {
                invalid.add("origin_evidence[" + i + "] must be an object");
                continue;
            }
            String identity = text(item, "identity");
            if (identity.isBlank()) invalid.add("origin_evidence[" + i + "].identity is required");
            else if (!identities.add(identity)) invalid.add("origin_evidence identities must be unique");
            if (text(item, "origin_type").isBlank()) invalid.add("origin_evidence[" + i + "].origin_type is required");
            if (text(item, "durable_ref").isBlank()) blocked.add("origin_evidence[" + i + "].durable_ref is missing");
        }
    }

    private static void validateHandoff(JsonNode handoff, List<String> invalid, List<String> blocked) {
        if (handoff == null || handoff.isNull()) return;
        String gate = text(handoff, "gate");
        if (!Set.of("READY_FOR_REVIEW", "REVIEW_COMPLETE").contains(gate)) {
            invalid.add("handoff.gate must be READY_FOR_REVIEW or REVIEW_COMPLETE");
            return;
        }
        for (String field : List.of("base_revision", "candidate_revision", "envelope_identity",
                "command_log", "digest_manifest", "token_accounting")) {
            if (text(handoff, field).isBlank()) blocked.add(gate + " requires " + field);
        }
        if (!handoff.path("attempt_count").canConvertToInt() || handoff.path("attempt_count").asInt() < 1) {
            invalid.add("handoff.attempt_count must be at least 1");
        }
        if (!handoff.path("elapsed_ms").canConvertToLong() || handoff.path("elapsed_ms").asLong() < 0) {
            invalid.add("handoff.elapsed_ms must be non-negative");
        }
        if ("REVIEW_COMPLETE".equals(gate)) {
            for (String field : List.of("reviewer_identity", "reviewed_digest", "verdict", "limitations")) {
                if (text(handoff, field).isBlank()) blocked.add("REVIEW_COMPLETE requires " + field);
            }
            if (text(handoff, "receiver_readback").isBlank()) blocked.add("REVIEW_COMPLETE requires receiver_readback");
        }
    }

    private static void validateKpis(JsonNode samples, List<String> invalid) {
        if (!samples.isArray()) {
            invalid.add("affected_kpis must be an array");
            return;
        }
        for (int i = 0; i < samples.size(); i++) {
            JsonNode sample = samples.get(i);
            String prefix = "affected_kpis[" + i + "]";
            if (!sample.isObject()) { invalid.add(prefix + " must be an object"); continue; }
            String status = text(sample, "status");
            if (!KPI_STATUSES.contains(status)) invalid.add(prefix + ".status is invalid");
            for (String field : List.of("metric", "window_start", "window_end", "category", "size_band",
                    "measurement_revision", "source_evidence")) {
                if (text(sample, field).isBlank()) invalid.add(prefix + "." + field + " is required");
            }
            if (!sample.path("sample_count").canConvertToInt() || sample.path("sample_count").asInt() < 0) {
                invalid.add(prefix + ".sample_count must be non-negative");
            }
            if ("UNKNOWN".equals(status) && !sample.path("value").isNull()) {
                invalid.add(prefix + " UNKNOWN requires null value");
            }
            if ("N/A".equals(status) && text(sample, "reason").isBlank()) {
                invalid.add(prefix + " N/A requires reason");
            }
            if ("OBSERVED".equals(status) && sample.path("value").isNull()) {
                invalid.add(prefix + " OBSERVED requires value");
            }
            if ("INSUFFICIENT_SAMPLE".equals(status)
                    && !sample.path("baseline_identity").isNull()
                    && text(sample, "baseline_identity").isBlank()) {
                invalid.add(prefix + ".baseline_identity must be null or non-empty");
            }
        }
    }

    private static String requiredText(JsonNode object, String field, List<String> invalid) {
        String value = text(object, field);
        if (value.isBlank()) invalid.add(field + " is required");
        return value;
    }

    private static void allowedText(JsonNode object, String field, Set<String> allowed, List<String> invalid) {
        String value = requiredText(object, field, invalid);
        if (!value.isBlank() && !allowed.contains(value)) invalid.add(field + " has unsupported value: " + value);
    }

    private static String text(JsonNode object, String field) {
        JsonNode value = object == null ? null : object.get(field);
        return value != null && value.isTextual() ? value.textValue() : "";
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value.strip(), Normalizer.Form.NFC);
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }
}

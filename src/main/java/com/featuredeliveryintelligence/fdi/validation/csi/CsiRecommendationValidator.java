package com.featuredeliveryintelligence.fdi.validation.csi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Instant;
import java.time.format.DateTimeParseException;
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
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern PROVIDER_REF = Pattern.compile("provider:[a-zA-Z0-9._/-]+/[a-zA-Z0-9._:-]+");
    private static final Pattern REPOSITORY_REF = Pattern.compile("sha256:[0-9a-f]{64}:[^/\\s][^\\s]*");
    private static final Set<String> AUTHORITY_FIELDS = Set.of(
            "dispatch_authorized", "implementation_authorized", "mutation_authorized",
            "product_truth", "closure_authorized");

    public CsiValidationReport validate(JsonNode record) {
        return validate(record, null, null, null);
    }

    public CsiValidationReport validate(JsonNode record, JsonNode priorRecord) {
        return validate(record, priorRecord, null, null);
    }

    public CsiValidationReport validate(JsonNode record, JsonNode priorRecord,
                                        String expectedBaseRevision, String expectedCandidateRevision) {
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
        validateAppendOnlyEvidence(object, priorRecord, invalid);
        validateHandoff(object.get("handoff"), expectedBaseRevision, expectedCandidateRevision, invalid, blocked);
        validateKpis(object.path("affected_kpis"), invalid);
        if ("UNKNOWN".equals(object.path("tag").asText())
                && (!("RECOMMENDED_NOT_SELECTED".equals(object.path("disposition").asText()))
                || !("UNKNOWN".equals(object.path("revision_route").asText())))) {
            invalid.add("UNKNOWN tag must remain RECOMMENDED_NOT_SELECTED with revision_route UNKNOWN");
        }

        String key = "";
        if (!affected.isBlank() && !insufficiency.isBlank() && !control.isBlank() && !route.isBlank()) {
            key = duplicateKey(affected, insufficiency, control, route);
            if (!object.hasNonNull("duplicate_key") || object.path("duplicate_key").asText().isBlank()) {
                invalid.add("duplicate_key is required");
            } else if (!key.equals(object.path("duplicate_key").asText())) {
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
            String durableRef = text(item, "durable_ref");
            if (durableRef.isBlank()) blocked.add("origin_evidence[" + i + "].durable_ref is missing");
            else if (!isImmutableReference(durableRef)) {
                invalid.add("origin_evidence[" + i + "].durable_ref is not immutable");
            }
        }
    }

    private void validateAppendOnlyEvidence(JsonNode currentRecord, JsonNode priorRecord, List<String> invalid) {
        if (priorRecord == null) return;
        if (!priorRecord.isObject()
                || !text(priorRecord, "recommendation_id").equals(text(currentRecord, "recommendation_id"))
                || !text(priorRecord, "duplicate_key").equals(text(currentRecord, "duplicate_key"))) {
            invalid.add("prior record must have the same recommendation_id and duplicate_key");
            return;
        }
        String priorCanonicalKey = canonicalKey(priorRecord);
        if (!text(priorRecord, "duplicate_key").equals(priorCanonicalKey)) {
            invalid.add("prior duplicate_key does not match its semantic fields");
            return;
        }
        JsonNode current = currentRecord.path("origin_evidence");
        JsonNode prior = priorRecord.path("origin_evidence");
        validateArrayPrefix(current, prior, "origin_evidence", invalid);
        validateArrayPrefix(currentRecord.path("affected_kpis"), priorRecord.path("affected_kpis"),
                "affected_kpis", invalid);
    }

    private static void validateArrayPrefix(JsonNode current, JsonNode prior, String field,
                                            List<String> invalid) {
        if (!prior.isArray() || !current.isArray() || current.size() < prior.size()) {
            invalid.add(field + " must preserve the prior append-only prefix");
            return;
        }
        for (int i = 0; i < prior.size(); i++) {
            if (!prior.get(i).equals(current.get(i))) {
                invalid.add(field + " must preserve the prior append-only prefix");
                return;
            }
        }
    }

    private String canonicalKey(JsonNode record) {
        String affected = text(record, "affected_requirement");
        String insufficiency = text(record, "insufficiency");
        String control = text(record, "proposed_control");
        String route = text(record, "revision_route");
        if (affected.isBlank() || insufficiency.isBlank() || control.isBlank() || route.isBlank()) return "";
        return duplicateKey(affected, insufficiency, control, route);
    }

    private static void validateHandoff(JsonNode handoff, String expectedBaseRevision,
                                        String expectedCandidateRevision, List<String> invalid,
                                        List<String> blocked) {
        if (handoff == null || handoff.isNull()) return;
        String gate = text(handoff, "gate");
        if (!Set.of("READY_FOR_REVIEW", "REVIEW_COMPLETE").contains(gate)) {
            invalid.add("handoff.gate must be READY_FOR_REVIEW or REVIEW_COMPLETE");
            return;
        }
        for (String field : List.of("envelope_identity", "command_log", "digest_manifest", "token_accounting")) {
            if (text(handoff, field).isBlank()) blocked.add(gate + " requires " + field);
        }
        for (String field : List.of("base_revision", "candidate_revision")) {
            if (!FULL_REVISION.matcher(text(handoff, field)).matches()) {
                invalid.add("handoff." + field + " must be a full 40-character Git revision");
            }
        }
        if (expectedBaseRevision == null || expectedCandidateRevision == null) {
            invalid.add("handoff requires externally bound base and candidate revisions");
        } else {
            if (!expectedBaseRevision.equals(text(handoff, "base_revision"))) {
                invalid.add("handoff.base_revision does not match externally bound revision");
            }
            if (!expectedCandidateRevision.equals(text(handoff, "candidate_revision"))) {
                invalid.add("handoff.candidate_revision does not match externally bound revision");
            }
        }
        for (String field : List.of("command_log", "digest_manifest")) {
            String reference = text(handoff, field);
            if (!isImmutableReference(reference)) {
                invalid.add("handoff." + field + " is not immutable");
            }
        }
        if (!handoff.path("command_exit_status").canConvertToInt()) {
            invalid.add("handoff.command_exit_status must be an integer");
        }
        validateDigestArray(handoff.path("input_artifact_digests"), "handoff.input_artifact_digests", invalid);
        validateDigestArray(handoff.path("output_artifact_digests"), "handoff.output_artifact_digests", invalid);
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
        if (!samples.isArray() || samples.isEmpty()) {
            invalid.add("affected_kpis must be a non-empty array");
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
            for (String field : List.of("value", "numerator", "denominator", "baseline_identity")) {
                if (!sample.has(field)) invalid.add(prefix + "." + field + " field is required");
            }
            if (!sample.path("sample_count").canConvertToInt() || sample.path("sample_count").asInt() < 0) {
                invalid.add(prefix + ".sample_count must be non-negative");
            }
            if (!sample.path("minimum_sample_count").canConvertToInt()
                    || sample.path("minimum_sample_count").asInt() < 1) {
                invalid.add(prefix + ".minimum_sample_count must be at least 1");
            }
            validateKpiRevisionAndSource(sample, prefix, invalid);
            validateWindow(sample, prefix, invalid);
            boolean sparse = sample.path("sample_count").asInt() < sample.path("minimum_sample_count").asInt();
            boolean baselineMissing = sample.path("baseline_identity").isNull()
                    || text(sample, "baseline_identity").isBlank();
            if ((sparse || baselineMissing) && "OBSERVED".equals(status)) {
                invalid.add(prefix + " sparse or baseline-less sample must be INSUFFICIENT_SAMPLE");
            }
            if (Set.of("UNKNOWN", "N/A", "INSUFFICIENT_SAMPLE").contains(status)) {
                for (String field : List.of("value", "numerator", "denominator")) {
                    if (!sample.path(field).isNull()) invalid.add(prefix + " " + status + " requires null " + field);
                }
            }
            if ("N/A".equals(status) && text(sample, "reason").isBlank()) {
                invalid.add(prefix + " N/A requires reason");
            }
            if ("OBSERVED".equals(status)) {
                for (String field : List.of("value", "numerator", "denominator")) {
                    if (!sample.path(field).isNumber()) invalid.add(prefix + " OBSERVED requires numeric " + field);
                }
                if (sample.path("denominator").isNumber() && sample.path("denominator").asDouble() <= 0) {
                    invalid.add(prefix + ".denominator must be positive");
                }
            }
        }
    }

    private static void validateDigestArray(JsonNode values, String field, List<String> invalid) {
        if (!values.isArray() || values.isEmpty()) {
            invalid.add(field + " must be a non-empty digest array");
            return;
        }
        for (JsonNode value : values) {
            if (!value.isTextual() || !SHA256.matcher(value.asText()).matches()) {
                invalid.add(field + " entries must be SHA-256 digests");
                return;
            }
        }
    }

    private static void validateKpiRevisionAndSource(JsonNode sample, String prefix, List<String> invalid) {
        if (!FULL_REVISION.matcher(text(sample, "measurement_revision")).matches()) {
            invalid.add(prefix + ".measurement_revision must be a full 40-character Git revision");
        }
        String source = text(sample, "source_evidence");
        if (!isImmutableReference(source)) {
            invalid.add(prefix + ".source_evidence is not immutable");
        }
    }

    private static void validateWindow(JsonNode sample, String prefix, List<String> invalid) {
        try {
            Instant start = Instant.parse(text(sample, "window_start"));
            Instant end = Instant.parse(text(sample, "window_end"));
            if (start.isAfter(end)) invalid.add(prefix + " window_start must not follow window_end");
        } catch (DateTimeParseException failure) {
            invalid.add(prefix + " window must use ISO-8601 instants");
        }
    }

    private static boolean isImmutableReference(String reference) {
        if (PROVIDER_REF.matcher(reference).matches()) {
            Set<String> moving = Set.of("latest", "head", "main", "master", "trunk");
            for (String segment : reference.substring("provider:".length()).split("[/:]")) {
                if (moving.contains(segment.toLowerCase(java.util.Locale.ROOT))) return false;
            }
            return true;
        }
        if (!REPOSITORY_REF.matcher(reference).matches()) return false;
        String pathText = reference.substring("sha256:".length() + 64 + 1);
        try {
            java.nio.file.Path path = java.nio.file.Path.of(pathText);
            if (path.isAbsolute() || pathText.contains("\\")
                    || !path.normalize().toString().equals(pathText) || !path.iterator().hasNext()) return false;
            for (java.nio.file.Path part : path) {
                if (".".equals(part.toString()) || "..".equals(part.toString())) return false;
            }
            return true;
        } catch (RuntimeException failure) {
            return false;
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

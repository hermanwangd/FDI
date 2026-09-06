package com.featuredeliveryintelligence.fdi.validation.scenarioreview;

import com.featuredeliveryintelligence.fdi.validation.blindreview.BlindReview;
import com.featuredeliveryintelligence.fdi.validation.codebaseline.PythonJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Validates PKB-001 scenario proposals and renders immutable review surfaces.
 *
 * <p>Ports the observable behavior of the transitional Python consumer
 * {@code tooling/validation/pkb001_scenario_review.py} exactly: the same
 * fail-closed reason vocabulary, the same generation-input/binding/evidence
 * verification order, the same sorted indent-2 non-ASCII JSON rendering for
 * review outputs (via {@link BlindReview#jsonBytes}), the same Traditional
 * Chinese Markdown template, Python-exact {@code %.4f} confidence formatting
 * (round-half-even on the exact decimal expansion), Python
 * {@code datetime.fromisoformat} (CPython 3.9 grammar) timestamp parsing, and
 * Python deep-equality semantics for on-disk byte comparisons
 * ({@code 1 == 1.0}, key-order-insensitive objects, order-sensitive arrays).
 *
 * <p>Known adapter-level parity notes (probe evidence under
 * {@code .slice-work/scenario-review/}):
 * <ul>
 * <li>The checked-in schema's relative {@code $id}
 * ("scenario-proposal.schema.json") is rejected by networknt's
 * JsonSchemaFactory; it is removed from an in-memory copy before compiling
 * the validator. All {@code $ref}s in the schema are internal
 * {@code #/$defs/...} fragments, so this cannot change verdicts.</li>
 * <li>networknt 1.5.5 for Draft 2020-12 treats {@code format} as an
 * annotation by default, matching the Python runtime where the optional
 * {@code rfc3339_validator} package is absent (verified: {@code 'date-time'}
 * is not in {@code FormatChecker().checkers}); format assertions stay off.</li>
 * <li>JSON parsing delegates to {@link PythonJson} (CPython-exact error
 * surface) with a 10,000-digit number limit; CPython 3.9 has no such limit,
 * so a single JSON number longer than 10,000 digits is the one input class
 * where the Java surface can diverge (INPUT_JSON_INVALID where CPython would
 * parse). Never produced by any checked-in fixture.</li>
 * </ul>
 */
public final class ScenarioReview {
    public static final String SCHEMA_PATH =
            "validation/pkb001/schemas/scenario-proposal.schema.json";
    public static final String SKILL_PATH = "skills/pkb001/pk-scenario-proposal/SKILL.md";

    private static final List<String> INPUT_KINDS =
            List.of("GRAPHIFY_BINDING", "FROZEN_GRAPH", "DELIVERY_HISTORY", "SCENARIO_SKILL");
    private static final int MAX_INPUT_BYTES = 16 * 1024 * 1024;
    private static final int MAX_JSON_NODES = 100_000;
    private static final int MAX_JSON_DEPTH = 64;

    private static final Pattern TECHNICAL_TEXT = Pattern.compile(
            "graphify|provider[_ -]?node|source[_ -]?path|qualified[_ -]?symbol|"
                    + "evaluator[ _-]?gold|expected[ _-]?mapping|"
                    + "[A-Za-z0-9_.\\-/]+\\.(?:java|py|js|ts|html|mustache|jsp|xml|ya?ml)\\b|"
                    + "(?:^|[\\s`])(?:src|app|lib|templates?)/[A-Za-z0-9_.\\-/]+|"
                    + "(?:[A-Za-z_$][A-Za-z0-9_$]*\\.)+[A-Za-z_$][A-Za-z0-9_$]*|"
                    + "[A-Za-z_$][A-Za-z0-9_$]*\\s*\\(|"
                    + "[A-Z][A-Za-z0-9_$]*(?:Controller|Service|Repository|Entity|Config|Configuration|Template)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern JSON_POINTER = Pattern.compile(
            "^/(?:nodes|links|commits|pull_requests)/(?:0|[1-9][0-9]*)$");
    private static final Pattern DRIVE_LETTER = Pattern.compile("^[A-Za-z]:");

    private static final Set<String> FORBIDDEN_COMPONENTS = Set.of(
            "evaluator", "gold", "ground-truth", "ground_truth", "judgments", "judgement",
            "comparison", "post-generation", "post_generation", "accepted-semantics",
            "accepted_semantics", "forward-semantics", "forward_semantics", "human-review",
            "task6-blind-review", "task7-evaluation");

    private static final JsonNodeFactory NODE = JsonNodeFactory.instance;

    private ScenarioReview() { }

    // ------------------------------------------------------------------
    // Public API (ports validate_proposal / validate_review / render_review /
    // accepted_scenarios / write_review_outputs)
    // ------------------------------------------------------------------

    /** Validates generation output without allowing any human decision fields. */
    public static ObjectNode validateProposal(Path root, JsonNode proposal) {
        return validateProposal(root, proposal, null);
    }

    /** Validates generation output without allowing any human decision fields. */
    public static ObjectNode validateProposal(Path root, JsonNode proposal, Path proposalPath) {
        try {
            return validateDocument(root, proposal, proposalPath, true);
        } catch (ScenarioReviewException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new ScenarioReviewException(List.of("REQUEST_INVALID"));
        }
    }

    /** Validates a rendered or human-completed review against exact proposal binding. */
    public static ObjectNode validateReview(Path root, JsonNode review) {
        try {
            ObjectNode validated = validateDocument(root, review, null, false);
            verifyOriginalProposal(root, validated);
            return validated;
        } catch (ScenarioReviewException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new ScenarioReviewException(List.of("REQUEST_INVALID"));
        }
    }

    /** Deterministic review JSON and Markdown; never infers or edits semantic text. */
    public static ReviewRender renderReview(Path root, JsonNode proposal, Path proposalPath) {
        ObjectNode validated = validateProposal(root, proposal, proposalPath);
        ObjectNode review = proposal.deepCopy();
        review.put("artifact_kind", "SCENARIO_REVIEW_SURFACE");
        Path resolvedRoot = resolveRoot(root);
        Path resolvedProposal = resolveLoose(proposalPath);
        review.put("proposal_artifact_path",
                resolvedRoot.relativize(resolvedProposal).toString());
        review.put("proposal_sha256", proposalDigest(resolvedProposal));
        review.set("resolved_evidence", validated.get("resolved_evidence"));
        validateReview(resolvedRoot, review);
        return new ReviewRender(review, markdown(review));
    }

    /** Selects decision-confirmed behavior only; does not freeze or publish it. */
    public static ArrayNode acceptedScenarios(Path root, JsonNode review) {
        ObjectNode validated = validateReview(root, review);
        ArrayNode accepted = NODE.arrayNode();
        for (JsonNode capabilityValue : iterableObjects(
                getDefaultEmpty(validated, "capability_proposals"))) {
            ObjectNode capability = (ObjectNode) capabilityValue;
            JsonNode capabilityDecision = capability.get("decision");
            if (!decisionAccepts(capabilityDecision)) {
                continue;
            }
            for (JsonNode scenarioValue : iterableLenient(
                    getDefaultEmpty(capability, "scenarios"))) {
                ObjectNode scenario = (ObjectNode) scenarioValue;
                JsonNode decision = scenario.get("decision");
                if (!decisionAccepts(decision)) {
                    continue;
                }
                ObjectNode behavior = NODE.objectNode();
                behavior.put("capability_id", capability.get("capability_id").asText());
                behavior.put("scenario_id", scenario.get("scenario_id").asText());
                if ("EDIT".equals(decision.get("action").asText())) {
                    JsonNode replacement = decision.get("replacement_behavior");
                    if (replacement == null || !replacement.isObject()) {
                        throw new RequestInvalidMarker();
                    }
                    behavior.setAll((ObjectNode) replacement.deepCopy());
                } else {
                    for (String key : List.of("title", "given", "when", "then", "scope")) {
                        JsonNode value = scenario.get(key);
                        if (value == null) {
                            throw new RequestInvalidMarker();
                        }
                        behavior.set(key, value.deepCopy());
                    }
                }
                accepted.add(behavior);
            }
        }
        return accepted;
    }

    /** Exclusively publishes the review pair, rolling back if either destination collides. */
    public static OutputPaths writeReviewOutputs(Path root, Path proposalPath,
            Path jsonOutput, Path markdownOutput) {
        Path resolvedRoot = resolveRoot(root);
        Path safeProposal = safeOutput(resolvedRoot, proposalPath, false);
        Set<String> readReasons = new LinkedHashSet<>();
        byte[] data = readBounded(safeProposal, readReasons, "INPUT_FILE_INVALID");
        JsonNode proposal = loadJson(data, readReasons);
        if (proposal == null || !proposal.isObject()) {
            throw new ScenarioReviewException(
                    readReasons.isEmpty() ? List.of("INPUT_JSON_INVALID") : readReasons);
        }
        ReviewRender rendered = renderReview(resolvedRoot, proposal, safeProposal);
        Path jsonPath = safeOutput(resolvedRoot, jsonOutput, true);
        Path markdownPath = safeOutput(resolvedRoot, markdownOutput, true);
        if (jsonPath.equals(markdownPath)) {
            throw new ScenarioReviewException(List.of("OUTPUT_PATH_INVALID"));
        }
        if (Files.exists(jsonPath) || Files.exists(markdownPath)) {
            throw new ScenarioReviewException(List.of("OUTPUT_ALREADY_EXISTS"));
        }
        ensureSafeDirectory(resolvedRoot, parentRelative(resolvedRoot, jsonPath));
        ensureSafeDirectory(resolvedRoot, parentRelative(resolvedRoot, markdownPath));
        Path claimPath = reserveRun(resolvedRoot, rendered.review(), jsonPath, markdownPath);
        byte[] jsonBytes = BlindReview.jsonBytes(rendered.review());
        byte[] markdownBytes = rendered.markdown().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Path jsonStage = null;
        Path markdownStage = null;
        boolean jsonCreated = false;
        boolean markdownCreated = false;
        try {
            jsonStage = stage(jsonPath, jsonBytes);
            markdownStage = stage(markdownPath, markdownBytes);
            try {
                Files.createLink(jsonPath, jsonStage);
            } catch (FileAlreadyExistsException collision) {
                throw collision;
            }
            jsonCreated = true;
            Files.createLink(markdownPath, markdownStage);
            markdownCreated = true;
            readonly(jsonPath);
            readonly(markdownPath);
        } catch (FileAlreadyExistsException collision) {
            deleteQuietly(jsonCreated ? jsonPath : null);
            deleteQuietly(markdownCreated ? markdownPath : null);
            deleteQuietly(claimPath);
            throw new ScenarioReviewException(List.of("OUTPUT_ALREADY_EXISTS"));
        } catch (IOException | RuntimeException failure) {
            deleteQuietly(jsonCreated ? jsonPath : null);
            deleteQuietly(markdownCreated ? markdownPath : null);
            deleteQuietly(claimPath);
            throw new ScenarioReviewException(List.of("OUTPUT_WRITE_FAILED"));
        } finally {
            deleteQuietly(jsonStage);
            deleteQuietly(markdownStage);
        }
        return new OutputPaths(jsonPath, markdownPath);
    }

    public record ReviewRender(ObjectNode review, String markdown) { }

    public record OutputPaths(Path json, Path markdown) { }

    // ------------------------------------------------------------------
    // Document validation
    // ------------------------------------------------------------------

    private static ObjectNode validateDocument(Path root, JsonNode document,
            Path proposalPath, boolean generation) {
        Path resolvedRoot = resolveRoot(root);
        Set<String> reasons = new TreeSet<>();
        validateSnapshot(document, 0, new int[] {MAX_JSON_NODES});
        JsonNode snapshot = document.deepCopy();
        JsonSchema validator = schemaValidator(resolvedRoot, reasons);
        if (validator != null && !validator.validate(snapshot).isEmpty()) {
            reasons.add("SCHEMA_INVALID");
        }
        if (!snapshot.isObject()) {
            reasons.add("SCHEMA_INVALID");
            throw new ScenarioReviewException(reasons);
        }
        ObjectNode snapshotObject = (ObjectNode) snapshot;
        String expectedKind = generation ? "SCENARIO_PROPOSAL" : "SCENARIO_REVIEW_SURFACE";
        JsonNode artifactKind = snapshotObject.get("artifact_kind");
        if (artifactKind == null || !artifactKind.isTextual()
                || !artifactKind.asText().equals(expectedKind)) {
            reasons.add("ARTIFACT_KIND_INVALID");
        }
        if (proposalPath != null) {
            try {
                Path resolved = resolveStrict(proposalPath);
                if (resolved == null || !resolved.startsWith(resolvedRoot)) {
                    reasons.add("PROPOSAL_PATH_INVALID");
                } else {
                    Set<String> readReasons = new LinkedHashSet<>();
                    byte[] data = readBounded(resolved, readReasons, "INPUT_FILE_INVALID");
                    JsonNode onDisk = loadJson(data, readReasons);
                    reasons.addAll(readReasons);
                    if (!pythonDeepEquals(onDisk, snapshot)) {
                        reasons.add("PROPOSAL_BYTES_MISMATCH");
                    }
                }
            } catch (InvalidPathException failure) {
                reasons.add("PROPOSAL_PATH_INVALID");
            }
        }
        Map<String, InputBinding> byKind = verifyInputs(resolvedRoot, snapshotObject, reasons);
        Map<String, JsonNode> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, InputBinding> entry : byKind.entrySet()) {
            if (!entry.getKey().equals("SCENARIO_SKILL")) {
                parsed.put(entry.getKey(), loadJson(entry.getValue().data(), reasons));
            }
        }
        verifyBindings(snapshotObject, byKind, parsed, reasons);
        ObjectNode resolvedEvidence = verifyEvidence(snapshotObject, byKind, parsed, reasons);
        if (!generation && !pythonDeepEquals(snapshotObject.get("resolved_evidence"),
                resolvedEvidence)) {
            reasons.add("RESOLVED_EVIDENCE_MISMATCH");
        }
        verifySemantics(snapshotObject, reasons);
        verifyDecisions(snapshotObject, reasons, generation);
        if (!reasons.isEmpty()) {
            throw new ScenarioReviewException(reasons);
        }
        ObjectNode validated = snapshotObject.deepCopy();
        validated.set("resolved_evidence", resolvedEvidence);
        return validated;
    }

    private record InputBinding(JsonNode item, Path path, byte[] data, String digest) { }

    private static JsonSchema schemaValidator(Path root, Set<String> reasons) {
        Path path = resolveInput(root, SCHEMA_PATH, reasons);
        byte[] data = readBounded(path, reasons, "INPUT_FILE_INVALID");
        JsonNode schema = loadJson(data, reasons);
        if (schema == null || !schema.isObject()) {
            reasons.add("SCHEMA_DEFINITION_INVALID");
            return null;
        }
        try {
            // networknt rejects the schema's relative $id; all $refs are
            // internal fragments, so dropping it cannot change verdicts.
            ObjectNode schemaObject = ((ObjectNode) schema).deepCopy();
            schemaObject.remove("$id");
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                    .getSchema(schemaObject);
        } catch (Exception failure) {
            reasons.add("SCHEMA_DEFINITION_INVALID");
            return null;
        }
    }

    private static Map<String, InputBinding> verifyInputs(Path root, ObjectNode document,
            Set<String> reasons) {
        Map<String, InputBinding> byKind = new LinkedHashMap<>();
        JsonNode inputs = document.get("generation_inputs");
        if (inputs == null || !inputs.isArray()) {
            reasons.add("GENERATION_INPUT_SET_INVALID");
            return byKind;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (JsonNode item : inputs) {
            if (item != null && item.isObject()) {
                JsonNode kind = item.get("kind");
                if (kind != null && (kind.isObject() || kind.isArray())) {
                    throw new RequestInvalidMarker();
                }
                String key = kind != null && kind.isTextual() ? kind.asText()
                        : pythonStr(kind);
                counts.merge(key, 1, Integer::sum);
            }
        }
        Set<String> expected = new LinkedHashSet<>(INPUT_KINDS);
        if (!counts.keySet().equals(expected) || counts.values().stream().anyMatch(count -> count != 1)) {
            reasons.add("GENERATION_INPUT_SET_INVALID");
        }
        for (JsonNode item : inputs) {
            if (item == null || !item.isObject()) {
                reasons.add("GENERATION_INPUT_SET_INVALID");
                continue;
            }
            ObjectNode itemObject = (ObjectNode) item;
            JsonNode kindNode = itemObject.get("kind");
            String kind = kindNode != null && kindNode.isTextual() ? kindNode.asText() : null;
            if (kind == null || !INPUT_KINDS.contains(kind)) {
                reasons.add("GENERATION_INPUT_SET_INVALID");
                continue;
            }
            JsonNode pathNode = itemObject.get("path");
            String relative = pathNode != null && pathNode.isTextual() ? pathNode.asText() : null;
            if (forbiddenPath(relative)) {
                reasons.add("FORBIDDEN_GENERATION_INPUT");
                continue;
            }
            Path path = resolveInput(root, relative, reasons);
            byte[] data = readBounded(path, reasons, "INPUT_FILE_INVALID");
            String digest = data != null ? sha256Hex(data) : null;
            JsonNode claimed = itemObject.get("sha256");
            String claimedDigest = claimed != null && claimed.isTextual() ? claimed.asText() : null;
            if (claimedDigest == null || !Objects.equals(digest, claimedDigest)) {
                reasons.add("INPUT_DIGEST_MISMATCH");
            }
            byKind.put(kind, new InputBinding(item, path, data, digest));
        }
        InputBinding skill = byKind.get("SCENARIO_SKILL");
        JsonNode skillPath = skill == null ? null : skill.item().get("path");
        if (skillPath == null || !skillPath.isTextual()
                || !SKILL_PATH.equals(skillPath.asText())) {
            reasons.add("SCENARIO_SKILL_INVALID");
        }
        return byKind;
    }

    private static void verifyBindings(ObjectNode document, Map<String, InputBinding> byKind,
            Map<String, JsonNode> parsed, Set<String> reasons) {
        JsonNode revision = document.get("source_revision");
        JsonNode graphDigest = document.get("graph_sha256");
        JsonNode cutoff = document.get("history_cutoff");
        JsonNode binding = parsed.get("GRAPHIFY_BINDING");
        InputBinding graph = byKind.get("FROZEN_GRAPH");
        JsonNode history = parsed.get("DELIVERY_HISTORY");
        ObjectNode snapshot;
        if (binding == null || !binding.isObject()
                || !isTextEqual(binding.get("result"), "EXACTLY_BOUND")) {
            reasons.add("GRAPHIFY_BINDING_INVALID");
            snapshot = NODE.objectNode();
        } else {
            JsonNode snapshotBinding = binding.get("snapshot_binding");
            if (snapshotBinding != null && snapshotBinding.isObject()) {
                snapshot = (ObjectNode) snapshotBinding;
            } else {
                reasons.add("GRAPHIFY_BINDING_INVALID");
                snapshot = NODE.objectNode();
            }
        }
        if (!pythonValuesEqual(snapshot.get("requested_revision"), revision)
                || !pythonValuesEqual(snapshot.get("indexed_revision"), revision)) {
            reasons.add("REVISION_BINDING_MISMATCH");
        }
        JsonNode bindingGraph = binding != null && binding.isObject()
                ? binding.get("graph_sha256") : null;
        JsonNode graphEntryDigest = graph == null || graph.digest() == null
                ? null : NODE.textNode(graph.digest());
        if (!pythonValuesEqual(snapshot.get("graph_sha256"), graphDigest)
                || !pythonValuesEqual(bindingGraph, graphDigest)
                || !pythonValuesEqual(graphEntryDigest, graphDigest)) {
            reasons.add("GRAPH_BINDING_MISMATCH");
        }
        if (history == null || !history.isObject()
                || !isTextEqual(history.get("status"), "FROZEN")
                || !isTextEqual(history.get("post_cutoff_knowledge_policy"), "EXCLUDE_AFTER_CUTOFF")) {
            reasons.add("DELIVERY_HISTORY_BINDING_INVALID");
        } else {
            if (!pythonValuesEqual(history.get("source_commit_sha"), revision)) {
                reasons.add("REVISION_BINDING_MISMATCH");
            }
            if (!pythonValuesEqual(history.get("history_cutoff"), cutoff)) {
                reasons.add("HISTORY_CUTOFF_MISMATCH");
            }
            long commits = pythonLen(getDefaultEmpty((ObjectNode) history, "commits"));
            long pullRequests = pythonLen(getDefaultEmpty((ObjectNode) history, "pull_requests"));
            if (commits + pullRequests > 10_000) {
                reasons.add("INPUT_TOO_LARGE");
            }
            verifyHistoryCutoff((ObjectNode) history, cutoff, reasons);
        }
        JsonNode graphBody = parsed.get("FROZEN_GRAPH");
        if (graphBody != null && graphBody.isObject()) {
            if (pythonLen(getDefaultEmpty((ObjectNode) graphBody, "nodes"))
                    + pythonLen(getDefaultEmpty((ObjectNode) graphBody, "links")) > 10_000) {
                reasons.add("INPUT_TOO_LARGE");
            }
        } else {
            reasons.add("GRAPH_INPUT_INVALID");
        }
    }

    private static void verifyHistoryCutoff(ObjectNode history, JsonNode cutoff,
            Set<String> reasons) {
        Instant cutoffTime = parseTimestampNode(cutoff);
        if (cutoffTime == null) {
            reasons.add("DELIVERY_HISTORY_BINDING_INVALID");
            return;
        }
        for (JsonNode commit : iterableLenient(getDefaultEmpty(history, "commits"))) {
            Instant committedAt = commit != null && commit.isObject()
                    ? parseTimestampNode(commit.get("committed_at")) : null;
            if (committedAt == null) {
                reasons.add("DELIVERY_HISTORY_BINDING_INVALID");
            } else if (committedAt.isAfter(cutoffTime)) {
                reasons.add("POST_CUTOFF_HISTORY_ITEM");
            }
        }
        for (JsonNode pullRequest : iterableLenient(getDefaultEmpty(history, "pull_requests"))) {
            if (pullRequest == null || !pullRequest.isObject()) {
                reasons.add("DELIVERY_HISTORY_BINDING_INVALID");
                continue;
            }
            for (String field : List.of("created_at", "updated_at")) {
                Instant observedAt = parseTimestampNode(pullRequest.get(field));
                if (observedAt == null) {
                    reasons.add("DELIVERY_HISTORY_BINDING_INVALID");
                } else if (observedAt.isAfter(cutoffTime)) {
                    reasons.add("POST_CUTOFF_HISTORY_ITEM");
                }
            }
        }
    }

    private static ObjectNode verifyEvidence(ObjectNode document, Map<String, InputBinding> byKind,
            Map<String, JsonNode> parsed, Set<String> reasons) {
        ObjectNode resolved = NODE.objectNode();
        JsonNode catalog = document.get("evidence_catalog");
        if (catalog == null || !catalog.isArray()) {
            return resolved;
        }
        JsonNode availability = document.get("channel_availability");
        if (availability != null && !availability.isObject()) {
            throw new RequestInvalidMarker();
        }
        Map<String, JsonNode> inputForChannel = new LinkedHashMap<>();
        inputForChannel.put("STRUCTURAL", byKind.get("FROZEN_GRAPH") == null
                ? null : NODE.textNode("FROZEN_GRAPH"));
        inputForChannel.put("DELIVERY_HISTORY", byKind.get("DELIVERY_HISTORY") == null
                ? null : NODE.textNode("DELIVERY_HISTORY"));
        Set<JsonNode> seen = new LinkedHashSet<>();
        for (JsonNode referenceValue : catalog) {
            if (referenceValue == null || !referenceValue.isObject()) {
                continue;
            }
            ObjectNode reference = (ObjectNode) referenceValue;
            JsonNode evidenceId = reference.get("evidence_id");
            if (evidenceId != null && (evidenceId.isObject() || evidenceId.isArray())) {
                throw new RequestInvalidMarker();
            }
            if (containsIdentity(seen, evidenceId)) {
                reasons.add("DUPLICATE_EVIDENCE_ID");
            }
            seen.add(evidenceId);
            JsonNode channel = reference.get("channel");
            if (channel != null && !channel.isTextual()) {
                throw new RequestInvalidMarker();
            }
            JsonNode availabilityEntry = channel == null || availability == null
                    ? null : availability.get(channel.asText());
            JsonNode status = availabilityEntry != null && availabilityEntry.isObject()
                    ? availabilityEntry.get("status") : null;
            if (isTextEqual(status, "UNAVAILABLE")) {
                reasons.add("UNAVAILABLE_CHANNEL_CITED");
            }
            JsonNode kindNode = channel == null ? null : inputForChannel.get(channel.asText());
            String kind = kindNode != null && kindNode.isTextual() ? kindNode.asText() : null;
            InputBinding bound = kind == null ? null : byKind.get(kind);
            JsonNode boundPath = bound == null || bound.item() == null
                    ? null : bound.item().get("path");
            JsonNode boundDigest = bound == null || bound.digest() == null
                    ? null : NODE.textNode(bound.digest());
            JsonNode artifactPath = reference.get("artifact_path");
            JsonNode artifactDigest = reference.get("artifact_sha256");
            if (!pythonValuesEqual(artifactPath, boundPath)
                    || !pythonValuesEqual(artifactDigest, boundDigest)) {
                reasons.add("EVIDENCE_BINDING_MISMATCH");
                continue;
            }
            JsonNode pointer = reference.get("json_pointer");
            PointerTarget target = pointerValue(kind == null ? null : parsed.get(kind), pointer);
            if (target.reason() != null) {
                reasons.add(target.reason());
                continue;
            }
            String summary = evidenceSummary(channel == null ? null : channel.asText(),
                    pointer.asText(), target.value());
            if (summary == null || summary.isEmpty() || summary.contains("None")) {
                reasons.add("EVIDENCE_REFERENCE_UNRESOLVED");
                continue;
            }
            resolved.set(pythonStr(evidenceId), NODE.textNode(summary));
        }
        for (JsonNode capability : iterableObjects(
                getDefaultEmpty(document, "capability_proposals"))) {
            if (capability == null || !capability.isObject()) {
                continue;
            }
            verifyClaimRefs(capability.get("evidence_refs"), resolved, reasons);
            JsonNode scenarios = capability.get("scenarios");
            if (scenarios != null && scenarios.isArray()) {
                for (JsonNode scenario : scenarios) {
                    if (scenario != null && scenario.isObject()) {
                        verifyClaimRefs(scenario.get("evidence_refs"), resolved, reasons);
                    }
                }
            }
        }
        return resolved;
    }

    private static void verifyClaimRefs(JsonNode refs, ObjectNode resolved, Set<String> reasons) {
        if (refs == null || !refs.isArray() || refs.isEmpty()) {
            reasons.add("EMPTY_EVIDENCE_CLAIM");
            return;
        }
        for (JsonNode reference : refs) {
            if (reference != null && (reference.isObject() || reference.isArray())) {
                throw new RequestInvalidMarker();
            }
            if (reference == null || resolved.get(pythonStr(reference)) == null) {
                reasons.add("EVIDENCE_REFERENCE_UNRESOLVED");
            }
        }
    }

    private record PointerTarget(JsonNode value, String reason) { }

    private static PointerTarget pointerValue(JsonNode document, JsonNode pointer) {
        if (pointer == null || !pointer.isTextual()
                || !JSON_POINTER.matcher(pointer.asText()).matches()) {
            return new PointerTarget(null, "EVIDENCE_REFERENCE_NOT_ATOMIC");
        }
        String[] parts = pointer.asText().substring(1).split("/");
        String collection = parts[0];
        String index = parts[1];
        JsonNode rows = document != null && document.isObject()
                ? document.get(collection) : null;
        if (rows == null || !rows.isArray()
                || new BigInteger(index).compareTo(BigInteger.valueOf(rows.size())) >= 0) {
            return new PointerTarget(null, "EVIDENCE_REFERENCE_UNRESOLVED");
        }
        JsonNode value = rows.get(new BigInteger(index).intValue());
        if (value == null || !value.isObject()) {
            return new PointerTarget(null, "EVIDENCE_REFERENCE_UNRESOLVED");
        }
        return new PointerTarget(value, null);
    }

    private static String evidenceSummary(String channel, String pointer, JsonNode value) {
        String collection = pointer.split("/")[1];
        if ("STRUCTURAL".equals(channel) && "nodes".equals(collection)) {
            JsonNode label = pythonOr(value.get("label"), value.get("id"));
            JsonNode location = pythonOr(value.get("source_location"), value.get("source_file"));
            return "節點 " + pythonStr(label) + "（" + pythonStr(location) + "）";
        }
        if ("STRUCTURAL".equals(channel)) {
            JsonNode relation = pythonOr(value.get("relation"), NODE.textNode("structural link"));
            JsonNode location = pythonOr(value.get("source_location"), value.get("source_file"));
            if (location == null) {
                location = NODE.textNode("location unavailable");
            }
            return "關聯 " + pythonStr(relation) + "（" + pythonStr(location) + "）";
        }
        if ("commits".equals(collection)) {
            JsonNode commit = pythonOr(value.get("commit_sha"), NODE.textNode("unknown"));
            String commitText = truncate(pythonStr(commit), 12);
            JsonNode subject = pythonOr(value.get("subject"), NODE.textNode("subject unavailable"));
            return "提交 " + commitText + "：" + pythonStr(subject);
        }
        JsonNode number = pythonOr(value.get("number"), NODE.textNode("?"));
        JsonNode title = pythonOr(value.get("title"), NODE.textNode("title unavailable"));
        return "PR #" + pythonStr(number) + "：" + pythonStr(title);
    }

    private static void verifySemantics(ObjectNode document, Set<String> reasons) {
        for (JsonNode value : semanticStrings(document)) {
            if (value != null && value.isTextual() && containsTechnicalText(value.asText())) {
                reasons.add("TECHNICAL_IDENTIFIER_IN_BEHAVIOR");
                break;
            }
        }
        List<JsonNode> capabilityIds = new ArrayList<>();
        List<JsonNode> scenarioIds = new ArrayList<>();
        for (JsonNode capability : iterableObjects(
                getDefaultEmpty(document, "capability_proposals"))) {
            if (capability == null || !capability.isObject()) {
                continue;
            }
            checkHashable(capability.get("capability_id"));
            capabilityIds.add(capability.get("capability_id"));
            for (JsonNode scenario : iterableLenient(
                    getDefaultEmpty((ObjectNode) capability, "scenarios"))) {
                if (scenario != null && scenario.isObject()) {
                    checkHashable(scenario.get("scenario_id"));
                    scenarioIds.add(scenario.get("scenario_id"));
                }
            }
        }
        if (hasDuplicate(capabilityIds)) {
            reasons.add("DUPLICATE_CAPABILITY_ID");
        }
        if (hasDuplicate(scenarioIds)) {
            reasons.add("DUPLICATE_SCENARIO_ID");
        }
    }

    private static List<JsonNode> semanticStrings(ObjectNode document) {
        List<JsonNode> strings = new ArrayList<>();
        for (JsonNode capabilityValue : iterableObjects(
                getDefaultEmpty(document, "capability_proposals"))) {
            if (capabilityValue == null || !capabilityValue.isObject()) {
                continue;
            }
            ObjectNode capability = (ObjectNode) capabilityValue;
            strings.add(capability.get("title"));
            strings.add(capability.get("description"));
            for (String field : List.of("includes", "excludes", "non_goals")) {
                addAllStrings(strings, capability.get(field));
            }
            JsonNode capabilityDecision = capability.get("decision");
            if (capabilityDecision != null && capabilityDecision.isObject()) {
                JsonNode replacement = capabilityDecision.get("replacement_behavior");
                if (replacement != null && replacement.isObject()) {
                    strings.add(replacement.get("title"));
                    strings.add(replacement.get("description"));
                    for (String field : List.of("includes", "excludes", "non_goals")) {
                        addAllStrings(strings, replacement.get(field));
                    }
                }
            }
            for (JsonNode scenarioValue : iterableLenient(
                    getDefaultEmpty(capability, "scenarios"))) {
                if (scenarioValue == null || !scenarioValue.isObject()) {
                    continue;
                }
                ObjectNode scenario = (ObjectNode) scenarioValue;
                strings.add(scenario.get("title"));
                strings.add(scenario.get("when"));
                for (String field : List.of("given", "then")) {
                    addAllStrings(strings, scenario.get(field));
                }
                JsonNode scenarioDecision = scenario.get("decision");
                if (scenarioDecision != null && scenarioDecision.isObject()) {
                    JsonNode replacement = scenarioDecision.get("replacement_behavior");
                    if (replacement != null && replacement.isObject()) {
                        strings.add(replacement.get("title"));
                        strings.add(replacement.get("when"));
                        for (String field : List.of("given", "then")) {
                            addAllStrings(strings, replacement.get(field));
                        }
                    }
                }
            }
        }
        return strings;
    }

    private static void verifyDecisions(ObjectNode document, Set<String> reasons,
            boolean generation) {
        JsonNode proposalRevision = document.get("proposal_revision");
        JsonNode proposalDigest = document.get("proposal_sha256");
        for (JsonNode capabilityValue : iterableObjects(
                getDefaultEmpty(document, "capability_proposals"))) {
            if (capabilityValue == null || !capabilityValue.isObject()) {
                continue;
            }
            ObjectNode capability = (ObjectNode) capabilityValue;
            List<JsonNode> decisions = new ArrayList<>();
            decisions.add(capability.get("decision"));
            for (JsonNode scenario : iterableLenient(getDefaultEmpty(capability, "scenarios"))) {
                if (scenario != null && scenario.isObject()) {
                    decisions.add(scenario.get("decision"));
                }
            }
            for (JsonNode decision : decisions) {
                if (generation) {
                    if (decision != null && !decision.isNull()) {
                        reasons.add("GENERATED_DECISION_FORBIDDEN");
                    }
                    continue;
                }
                if (decision == null || decision.isNull()) {
                    continue;
                }
                if (!decision.isObject()
                        || !pythonValuesEqual(decision.get("proposal_revision"), proposalRevision)
                        || !pythonValuesEqual(decision.get("proposal_sha256"), proposalDigest)) {
                    reasons.add("DECISION_BINDING_MISMATCH");
                }
            }
        }
    }

    private static boolean decisionAccepts(JsonNode decision) {
        if (decision == null || !decision.isObject()) {
            return false;
        }
        if (isTextEqual(decision.get("action"), "ACCEPT")) {
            return true;
        }
        JsonNode confirmed = decision.get("edit_confirmed");
        return isTextEqual(decision.get("action"), "EDIT")
                && confirmed != null && confirmed.isBoolean() && confirmed.booleanValue();
    }

    private static void verifyOriginalProposal(Path root, ObjectNode review) {
        Path resolvedRoot = resolveRoot(root);
        Set<String> reasons = new TreeSet<>();
        JsonNode relative = review.get("proposal_artifact_path");
        String relativeText = relative != null && relative.isTextual() ? relative.asText() : null;
        Path path = resolveInput(resolvedRoot, relativeText, reasons);
        byte[] data = readBounded(path, reasons, "INPUT_FILE_INVALID");
        String digest = data != null ? sha256Hex(data) : null;
        JsonNode claimedDigest = review.get("proposal_sha256");
        if (claimedDigest == null || !claimedDigest.isTextual()
                || !Objects.equals(digest, claimedDigest.asText())) {
            reasons.add("PROPOSAL_DIGEST_MISMATCH");
        }
        JsonNode original = loadJson(data, reasons);
        if (original != null && original.isObject()) {
            ObjectNode validatedOriginal = validateProposal(resolvedRoot, original, path);
            ObjectNode expected = ((ObjectNode) original).deepCopy();
            expected.put("artifact_kind", "SCENARIO_REVIEW_SURFACE");
            expected.set("proposal_artifact_path", relative);
            expected.put("proposal_sha256", digest);
            expected.set("resolved_evidence", validatedOriginal.get("resolved_evidence"));
            if (!pythonDeepEquals(withoutDecisions(expected), withoutDecisions(review))) {
                reasons.add("ORIGINAL_PROPOSAL_MISMATCH");
            }
        } else {
            reasons.add("ORIGINAL_PROPOSAL_INVALID");
        }
        if (!reasons.isEmpty()) {
            throw new ScenarioReviewException(reasons);
        }
    }

    private static ObjectNode withoutDecisions(ObjectNode document) {
        ObjectNode snapshot = document.deepCopy();
        for (JsonNode capability : iterableObjects(
                getDefaultEmpty(snapshot, "capability_proposals"))) {
            if (capability == null || !capability.isObject()) {
                continue;
            }
            ((ObjectNode) capability).putNull("decision");
            for (JsonNode scenario : iterableLenient(getDefaultEmpty((ObjectNode) capability, "scenarios"))) {
                if (scenario != null && scenario.isObject()) {
                    ((ObjectNode) scenario).putNull("decision");
                }
            }
        }
        return snapshot;
    }

    // ------------------------------------------------------------------
    // Markdown rendering
    // ------------------------------------------------------------------

    private static String markdown(ObjectNode review) {
        List<String> lines = new ArrayList<>(List.of(
                "# PKB-001 情境提案個別審查 / Scenario Proposal Review",
                "",
                "- Run ID: `" + review.get("run_id").asText() + "`",
                "- Proposal revision: `" + review.get("proposal_revision").asText() + "`",
                "- Proposal SHA-256: `" + review.get("proposal_sha256").asText() + "`",
                "- Authority / status: `PROPOSAL_ONLY / UNREVIEWED`",
                "- Source revision: `" + review.get("source_revision").asText() + "`",
                "- Graph SHA-256: `" + review.get("graph_sha256").asText() + "`",
                "- Delivery cutoff: `" + review.get("history_cutoff").asText() + "`",
                "- Reviewer exposure: technical evidence is visible; content-level arm anonymity is not claimed.",
                "- Experiment limitation: `" + review.get("experiment_limitation").asText() + "`",
                "",
                "> 信心僅為未校準排序提示，不是校準後機率。每項證據只證明該觀察存在；請由 Human Reviewer 判斷推論是否成立。",
                ""));
        for (JsonNode capabilityValue : review.get("capability_proposals")) {
            ObjectNode capability = (ObjectNode) capabilityValue;
            lines.add("## 能力提案 / Capability Proposal — " + capability.get("capability_id").asText());
            lines.add("");
            lines.add("**" + capability.get("title").asText() + "**");
            lines.add("");
            lines.add(capability.get("description").asText());
            lines.add("");
            lines.add("包含 / Includes:");
            lines.add("");
            lines.add(bullets(capability.get("includes")));
            lines.add("");
            lines.add("排除 / Excludes:");
            lines.add("");
            lines.add(bullets(capability.get("excludes")));
            lines.add("");
            lines.add("非目標 / Non-goals:");
            lines.add("");
            lines.add(bullets(capability.get("non_goals")));
            lines.add("");
            lines.add("推論理由 / Inference rationale: " + capability.get("inference_rationale").asText());
            lines.add("");
            lines.add("Confidence: `" + format4f(capability.get("confidence").asDouble())
                    + "` (`UNCALIBRATED_RANKING_HINT`)");
            lines.add("");
            lines.add("限制 / Limitations:");
            lines.add("");
            lines.add(bullets(capability.get("limitations")));
            lines.add("");
            lines.add("證據 / Evidence:");
            lines.add("");
            lines.add(evidenceLines(review, capability.get("evidence_refs")));
            lines.add("");
            lines.add("能力決定 / Capability decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**");
            lines.add("");
            for (JsonNode scenarioValue : capability.get("scenarios")) {
                ObjectNode scenario = (ObjectNode) scenarioValue;
                lines.add("### 情境 / Scenario — " + scenario.get("scenario_id").asText());
                lines.add("");
                lines.add("**" + scenario.get("title").asText() + "** (`" + scenario.get("scope").asText() + "`)");
                lines.add("");
                lines.add("Given / 前提:");
                lines.add("");
                lines.add(bullets(scenario.get("given")));
                lines.add("");
                lines.add("When / 當: " + scenario.get("when").asText());
                lines.add("");
                lines.add("Then / 則:");
                lines.add("");
                lines.add(bullets(scenario.get("then")));
                lines.add("");
                lines.add("推論理由 / Inference rationale: " + scenario.get("inference_rationale").asText());
                lines.add("");
                lines.add("Confidence: `" + format4f(scenario.get("confidence").asDouble())
                        + "` (`UNCALIBRATED_RANKING_HINT`)");
                lines.add("");
                lines.add("限制 / Limitations:");
                lines.add("");
                lines.add(bullets(scenario.get("limitations")));
                lines.add("");
                lines.add("證據 / Evidence:");
                lines.add("");
                lines.add(evidenceLines(review, scenario.get("evidence_refs")));
                lines.add("");
                lines.add("情境決定 / Scenario decision: `ACCEPT / EDIT / REJECT` — **尚未填寫 / EMPTY**");
                lines.add("");
            }
        }
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            out.append(line).append('\n');
        }
        while (out.length() > 0 && Character.isWhitespace(out.charAt(out.length() - 1))) {
            out.setLength(out.length() - 1);
        }
        return out.toString() + "\n";
    }

    private static String bullets(JsonNode values) {
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                out.append('\n');
            }
            out.append("- ").append(values.get(index).asText());
        }
        return out.toString();
    }

    private static String evidenceLines(ObjectNode review, JsonNode refs) {
        JsonNode summaries = review.get("resolved_evidence");
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < refs.size(); index++) {
            if (index > 0) {
                out.append('\n');
            }
            String reference = refs.get(index).asText();
            JsonNode summary = summaries == null ? null : summaries.get(reference);
            if (summary == null) {
                throw new RequestInvalidMarker();
            }
            out.append("- `").append(reference).append("`: ").append(summary.asText());
        }
        return out.toString();
    }

    // ------------------------------------------------------------------
    // Exclusive output publication
    // ------------------------------------------------------------------

    private static Path safeOutput(Path root, Path value, boolean reviewOutput) {
        Path path = value;
        Path relative;
        if (path.isAbsolute()) {
            if (!path.normalize().startsWith(root)) {
                throw new ScenarioReviewException(List.of("OUTPUT_PATH_INVALID"));
            }
            relative = root.relativize(path.normalize());
        } else {
            relative = path;
        }
        String canonical = canonicalRelative(relative.toString().replace('\\', '/'));
        if (canonical == null) {
            throw new ScenarioReviewException(List.of("OUTPUT_PATH_INVALID"));
        }
        if (reviewOutput && !canonical.startsWith("validation/pkb001/")) {
            throw new ScenarioReviewException(List.of("OUTPUT_PATH_INVALID"));
        }
        Path candidate = root.resolve(canonical);
        String parentRelative = parentRelative(root, candidate);
        if (!parentRelative.isEmpty() && hasSymlinkComponent(root, parentRelative)) {
            throw new ScenarioReviewException(List.of("OUTPUT_PATH_SYMLINK"));
        }
        return candidate;
    }

    private static String parentRelative(Path root, Path candidate) {
        Path parent = candidate.getParent();
        if (parent == null) {
            return "";
        }
        Path relative = root.relativize(parent);
        String text = relative.toString().replace('\\', '/');
        return text.isEmpty() ? "" : text.equals(".") ? "" : text;
    }

    private static Path ensureSafeDirectory(Path root, String relative) {
        String canonical = canonicalRelative(relative);
        if (canonical == null) {
            throw new ScenarioReviewException(List.of("OUTPUT_PATH_INVALID"));
        }
        Path current = root;
        for (String component : canonical.split("/")) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) {
                throw new ScenarioReviewException(List.of("OUTPUT_PATH_SYMLINK"));
            }
            try {
                Files.createDirectory(current);
            } catch (FileAlreadyExistsException exists) {
                if (Files.isSymbolicLink(current)) {
                    throw new ScenarioReviewException(List.of("OUTPUT_PATH_SYMLINK"));
                }
                if (!Files.isDirectory(current)) {
                    throw new ScenarioReviewException(List.of("OUTPUT_PATH_INVALID"));
                }
            } catch (IOException | RuntimeException failure) {
                throw new ScenarioReviewException(List.of("OUTPUT_WRITE_FAILED"));
            }
        }
        return current;
    }

    private static Path reserveRun(Path root, ObjectNode review, Path jsonPath, Path markdownPath) {
        Path claimDir = ensureSafeDirectory(root, "validation/pkb001/scenario-review-runs");
        String claimName = sha256Hex(
                review.get("run_id").asText().getBytes(java.nio.charset.StandardCharsets.UTF_8))
                + ".claim.json";
        Path claimPath = claimDir.resolve(claimName);
        ObjectNode body = NODE.objectNode();
        body.put("run_id", review.get("run_id").asText());
        body.put("proposal_sha256", review.get("proposal_sha256").asText());
        body.put("json_output", root.relativize(jsonPath).toString().replace('\\', '/'));
        body.put("markdown_output", root.relativize(markdownPath).toString().replace('\\', '/'));
        byte[] bytes = (jsonDumpsAscii(body, 2) + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try {
            Files.createFile(claimPath);
        } catch (FileAlreadyExistsException collision) {
            throw new ScenarioReviewException(List.of("RUN_ID_ALREADY_EXISTS"));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        try {
            try (FileChannel channel = FileChannel.open(claimPath,
                    StandardOpenOption.WRITE)) {
                channel.write(java.nio.ByteBuffer.wrap(bytes));
                channel.force(true);
            }
            Files.setPosixFilePermissions(claimPath,
                    PosixFilePermissions.fromString("r--r--r--"));
        } catch (IOException | RuntimeException failure) {
            deleteQuietly(claimPath);
            if (failure instanceof UncheckedIOException unchecked) {
                throw unchecked;
            }
            throw new UncheckedIOException(new IOException(failure));
        }
        return claimPath;
    }

    private static Path stage(Path path, byte[] data) throws IOException {
        Files.createDirectories(path.getParent());
        Path staged = Files.createTempFile(path.getParent(), ".scenario-review-", null);
        try {
            try (FileChannel channel = FileChannel.open(staged,
                    StandardOpenOption.WRITE)) {
                channel.write(java.nio.ByteBuffer.wrap(data));
                channel.force(true);
            }
            return staged;
        } catch (IOException | RuntimeException failure) {
            deleteQuietly(staged);
            throw failure instanceof IOException io ? io : new IOException(failure);
        }
    }

    private static void readonly(Path path) throws IOException {
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("r--r--r--"));
    }

    // ------------------------------------------------------------------
    // Input loading and path safety primitives
    // ------------------------------------------------------------------

    private static void validateSnapshot(JsonNode value, int depth, int[] budget) {
        budget[0]--;
        if (budget[0] < 0 || depth > MAX_JSON_DEPTH) {
            throw new ScenarioReviewException(List.of("INPUT_TOO_LARGE"));
        }
        if (value == null || value.isNull()) {
            return;
        }
        if (value.isArray() || value.isObject()) {
            for (JsonNode child : value) {
                validateSnapshot(child, depth + 1, budget);
            }
            return;
        }
        if (value.isNumber()) {
            if (!value.isIntegralNumber() && !Double.isFinite(value.doubleValue())) {
                throw new ScenarioReviewException(List.of("NON_JSON_INPUT"));
            }
            return;
        }
        if (value.isTextual() || value.isBoolean()) {
            return;
        }
        throw new ScenarioReviewException(List.of("NON_JSON_INPUT"));
    }

    private static String canonicalRelative(String path) {
        if (path == null || path.isEmpty() || path.trim().isEmpty()
                || path.contains("\\") || path.contains("\0")) {
            return null;
        }
        if (path.startsWith("/") || DRIVE_LETTER.matcher(path).find()) {
            return null;
        }
        for (String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) {
                return null;
            }
        }
        return path;
    }

    private static boolean forbiddenPath(String path) {
        String canonical = canonicalRelative(path);
        if (canonical == null) {
            return false;
        }
        for (String component : canonical.split("/")) {
            if (FORBIDDEN_COMPONENTS.contains(component.toLowerCase(java.util.Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSymlinkComponent(Path root, String canonical) {
        Path current = root;
        for (String component : canonical.split("/")) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) {
                return true;
            }
        }
        return false;
    }

    private static Path resolveInput(Path root, String relative, Set<String> reasons) {
        String canonical = canonicalRelative(relative);
        if (canonical == null) {
            reasons.add("INPUT_PATH_INVALID");
            return null;
        }
        if (hasSymlinkComponent(root, canonical)) {
            reasons.add("INPUT_PATH_SYMLINK");
            return null;
        }
        Path candidate = root.resolve(canonical);
        Path resolved = resolveStrict(candidate);
        if (resolved == null || !resolved.startsWith(root) || !Files.isRegularFile(resolved)) {
            reasons.add("INPUT_FILE_INVALID");
            return null;
        }
        return resolved;
    }

    private static Path resolveStrict(Path candidate) {
        try {
            if (!Files.exists(candidate)) {
                return null;
            }
            return candidate.toRealPath();
        } catch (IOException | RuntimeException failure) {
            return null;
        }
    }

    private static Path resolveLoose(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException | RuntimeException failure) {
            try {
                return path.toAbsolutePath().normalize();
            } catch (RuntimeException nested) {
                throw new RequestInvalidMarker();
            }
        }
    }

    private static Path resolveRoot(Path root) {
        try {
            return root.toRealPath();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static byte[] readBounded(Path path, Set<String> reasons, String reason) {
        if (path == null) {
            return null;
        }
        try {
            if (Files.size(path) > MAX_INPUT_BYTES) {
                reasons.add("INPUT_TOO_LARGE");
                return null;
            }
            return Files.readAllBytes(path);
        } catch (IOException | RuntimeException failure) {
            reasons.add(reason);
            return null;
        }
    }

    private static JsonNode loadJson(byte[] data, Set<String> reasons) {
        if (data == null) {
            return null;
        }
        try {
            JsonNode value = PythonJson.readTree(data);
            validateSnapshot(value, 0, new int[] {MAX_JSON_NODES});
            return value;
        } catch (ScenarioReviewException failure) {
            reasons.addAll(failure.getReasons());
        } catch (Exception failure) {
            reasons.add("INPUT_JSON_INVALID");
        }
        return null;
    }

    private static String proposalDigest(Path proposalPath) {
        try {
            return sha256Hex(Files.readAllBytes(proposalPath));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    // ------------------------------------------------------------------
    // Python-exact primitives
    // ------------------------------------------------------------------

    static boolean containsTechnicalText(String text) {
        return TECHNICAL_TEXT.matcher(text).find();
    }

    /** Python {@code '%.4f' % value}: round-half-even on the exact decimal expansion. */
    static String format4f(double value) {
        if (Double.isNaN(value)) {
            return "nan";
        }
        if (Double.isInfinite(value)) {
            return value > 0 ? "inf" : "-inf";
        }
        if (value == 0.0) {
            return Math.copySign(1.0, value) < 0 ? "-0.0000" : "0.0000";
        }
        return new BigDecimal(value).setScale(4, RoundingMode.HALF_EVEN).toPlainString();
    }

    /**
     * Python {@code datetime.fromisoformat(value.replace("Z", "+00:00"))}
     * (CPython 3.9 grammar) with the consumer's tz-required rule: returns the
     * UTC instant, or {@code null} when Python would return {@code None}
     * (naive datetime or unparseable string).
     */
    static Instant parseTimestamp(String value) {
        if (value == null) {
            return null;
        }
        String s = value.replace("Z", "+00:00");
        Cursor cursor = new Cursor(s);
        Integer year = cursor.digits(4);
        if (year == null || cursor.peek() != '-') {
            return null;
        }
        cursor.pos++;
        Integer month = cursor.digits(2);
        if (month == null || cursor.peek() != '-') {
            return null;
        }
        cursor.pos++;
        Integer day = cursor.digits(2);
        if (day == null || year < 1) {
            return null;
        }
        if (cursor.pos >= cursor.text.length()) {
            return null;
        }
        char separator = cursor.text.charAt(cursor.pos);
        if (separator >= '0' && separator <= '9') {
            return null;
        }
        cursor.pos++;
        Integer hour = cursor.digits(2);
        if (hour == null || hour > 23) {
            return null;
        }
        int minute = 0;
        int second = 0;
        int micro = 0;
        if (cursor.peek() == ':') {
            cursor.pos++;
            Integer parsedMinute = cursor.digits(2);
            if (parsedMinute == null || parsedMinute > 59) {
                return null;
            }
            minute = parsedMinute;
            if (cursor.peek() == ':') {
                cursor.pos++;
                Integer parsedSecond = cursor.digits(2);
                if (parsedSecond == null || parsedSecond > 59) {
                    return null;
                }
                second = parsedSecond;
            }
        }
        if (cursor.peek() == '.') {
            cursor.pos++;
            Integer millis = cursor.tryDigits(3);
            if (millis != null) {
                micro = millis * 1000;
                cursor.pos += 3;
                Integer micros = cursor.tryDigits(3);
                if (micros != null) {
                    micro = millis * 1000 + micros;
                    cursor.pos += 3;
                }
            }
        }
        if (cursor.pos >= cursor.text.length()) {
            return null;
        }
        char signChar = cursor.text.charAt(cursor.pos);
        if (signChar != '+' && signChar != '-') {
            return null;
        }
        int sign = signChar == '-' ? -1 : 1;
        cursor.pos++;
        Integer tzHour = cursor.digits(2);
        if (tzHour == null || cursor.peek() != ':') {
            return null;
        }
        cursor.pos++;
        Integer tzMinute = cursor.digits(2);
        if (tzMinute == null) {
            return null;
        }
        int tzSecond = 0;
        long tzMicros = 0;
        if (cursor.peek() == ':') {
            cursor.pos++;
            Integer parsedTzSecond = cursor.digits(2);
            if (parsedTzSecond == null) {
                return null;
            }
            tzSecond = parsedTzSecond;
            if (cursor.peek() == '.') {
                cursor.pos++;
                Integer fraction = cursor.tryDigits(6);
                if (fraction == null) {
                    return null;
                }
                tzMicros = fraction;
                cursor.pos += 6;
            }
        }
        if (cursor.pos != cursor.text.length()) {
            return null;
        }
        long offsetMicros = sign * (tzHour * 3_600_000_000L + tzMinute * 60_000_000L
                + tzSecond * 1_000_000L + tzMicros);
        if (offsetMicros >= 24L * 3_600_000_000L || offsetMicros <= -24L * 3_600_000_000L) {
            return null;
        }
        try {
            LocalDateTime local = LocalDateTime.of(
                    year, month, day, hour, minute, second, micro * 1000);
            long localMicros = local.toEpochSecond(ZoneOffset.UTC) * 1_000_000L + micro;
            long instantMicros = localMicros - offsetMicros;
            long epochSecond = Math.floorDiv(instantMicros, 1_000_000L);
            long microPart = Math.floorMod(instantMicros, 1_000_000L);
            return Instant.ofEpochSecond(epochSecond, microPart * 1000L);
        } catch (RuntimeException failure) {
            return null;
        }
    }

    static Instant parseTimestampNode(JsonNode value) {
        if (value == null || !value.isTextual()) {
            return null;
        }
        return parseTimestamp(value.asText());
    }

    private static final class Cursor {
        final String text;
        int pos;

        Cursor(String text) {
            this.text = text;
        }

        char peek() {
            return pos < text.length() ? text.charAt(pos) : '\0';
        }

        Integer digits(int count) {
            Integer value = tryDigits(count);
            if (value == null) {
                return null;
            }
            pos += count;
            return value;
        }

        Integer tryDigits(int count) {
            if (pos + count > text.length()) {
                return null;
            }
            int value = 0;
            for (int index = 0; index < count; index++) {
                char character = text.charAt(pos + index);
                if (character < '0' || character > '9') {
                    return null;
                }
                value = value * 10 + (character - '0');
            }
            return value;
        }
    }

    /** Python {@code ==} for parsed JSON values ({@code 1 == 1.0}, key-order-free objects). */
    static boolean pythonDeepEquals(JsonNode left, JsonNode right) {
        if (left == null || left.isMissingNode() || left.isNull()) {
            return right == null || right.isMissingNode() || right.isNull();
        }
        if (right == null || right.isMissingNode() || right.isNull()) {
            return false;
        }
        if (left.isObject() && right.isObject()) {
            Set<String> leftNames = new LinkedHashSet<>();
            left.fieldNames().forEachRemaining(leftNames::add);
            Set<String> rightNames = new LinkedHashSet<>();
            right.fieldNames().forEachRemaining(rightNames::add);
            if (!leftNames.equals(rightNames)) {
                return false;
            }
            for (String name : leftNames) {
                if (!pythonDeepEquals(left.get(name), right.get(name))) {
                    return false;
                }
            }
            return true;
        }
        if (left.isArray() && right.isArray()) {
            if (left.size() != right.size()) {
                return false;
            }
            for (int index = 0; index < left.size(); index++) {
                if (!pythonDeepEquals(left.get(index), right.get(index))) {
                    return false;
                }
            }
            return true;
        }
        if (left.isBoolean() || right.isBoolean()) {
            if (left.isBoolean() && right.isBoolean()) {
                return left.booleanValue() == right.booleanValue();
            }
            JsonNode boolSide = left.isBoolean() ? left : right;
            JsonNode numberSide = left.isBoolean() ? right : left;
            if (!numberSide.isNumber()) {
                return false;
            }
            return new BigDecimal(boolSide.booleanValue() ? "1" : "0")
                    .compareTo(new BigDecimal(numberSide.asText())) == 0;
        }
        if (left.isNumber() && right.isNumber()) {
            try {
                return new BigDecimal(left.asText()).compareTo(new BigDecimal(right.asText())) == 0;
            } catch (NumberFormatException failure) {
                return left.asText().equals(right.asText());
            }
        }
        if (left.isTextual() && right.isTextual()) {
            return left.asText().equals(right.asText());
        }
        return false;
    }

    static boolean pythonValuesEqual(JsonNode left, JsonNode right) {
        return pythonDeepEquals(left, right);
    }

    /** Python {@code str()} for JSON scalars and containers (used by evidence summaries). */
    static String pythonStr(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return "None";
        }
        if (value.isBoolean()) {
            return value.booleanValue() ? "True" : "False";
        }
        if (value.isIntegralNumber()) {
            return value.bigIntegerValue().toString();
        }
        if (value.isNumber()) {
            return pythonFloatRepr(value.doubleValue());
        }
        if (value.isTextual()) {
            return value.asText();
        }
        return pythonRepr(value);
    }

    private static String pythonRepr(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return "None";
        }
        if (value.isBoolean()) {
            return value.booleanValue() ? "True" : "False";
        }
        if (value.isIntegralNumber()) {
            return value.bigIntegerValue().toString();
        }
        if (value.isNumber()) {
            return pythonFloatRepr(value.doubleValue());
        }
        if (value.isTextual()) {
            return pythonReprString(value.asText());
        }
        if (value.isArray()) {
            StringBuilder out = new StringBuilder("[");
            for (int index = 0; index < value.size(); index++) {
                if (index > 0) {
                    out.append(", ");
                }
                out.append(pythonRepr(value.get(index)));
            }
            return out.append(']').toString();
        }
        StringBuilder out = new StringBuilder("{");
        List<String> names = new ArrayList<>();
        value.fieldNames().forEachRemaining(names::add);
        for (int index = 0; index < names.size(); index++) {
            if (index > 0) {
                out.append(", ");
            }
            out.append(pythonReprString(names.get(index))).append(": ")
                    .append(pythonRepr(value.get(names.get(index))));
        }
        return out.append('}').toString();
    }

    private static String pythonReprString(String value) {
        boolean hasSingle = value.contains("'");
        boolean hasDouble = value.contains("\"");
        char quote = hasSingle && !hasDouble ? '"' : '\'';
        StringBuilder out = new StringBuilder();
        out.append(quote);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == quote || character == '\\') {
                out.append('\\').append(character);
            } else if (character == '\n') {
                out.append("\\n");
            } else if (character == '\r') {
                out.append("\\r");
            } else if (character == '\t') {
                out.append("\\t");
            } else if (character < 0x20 || character == 0x7f) {
                out.append(String.format("\\x%02x", (int) character));
            } else {
                out.append(character);
            }
        }
        return out.append(quote).toString();
    }

    /** Python 3 {@code repr(float)} for finite doubles. */
    private static String pythonFloatRepr(double value) {
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

    /** Python {@code json.dumps(value, ensure_ascii=True, sort_keys=True, indent=N)}. */
    static String jsonDumpsAscii(JsonNode value, int indent) {
        StringBuilder out = new StringBuilder();
        writeAscii(out, value, 0, indent);
        return out.toString();
    }

    private static void writeAscii(StringBuilder out, JsonNode node, int level, int indent) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            out.append("null");
        } else if (node.isTextual()) {
            writeAsciiString(out, node.asText());
        } else if (node.isBoolean()) {
            out.append(node.booleanValue() ? "true" : "false");
        } else if (node.isIntegralNumber()) {
            out.append(node.bigIntegerValue().toString());
        } else if (node.isNumber()) {
            out.append(pythonFloatRepr(node.doubleValue()));
        } else if (node.isArray()) {
            if (node.isEmpty()) {
                out.append("[]");
                return;
            }
            out.append("[\n");
            for (int index = 0; index < node.size(); index++) {
                out.append(" ".repeat(indent * (level + 1)));
                writeAscii(out, node.get(index), level + 1, indent);
                if (index + 1 < node.size()) {
                    out.append(',');
                }
                out.append('\n');
            }
            out.append(" ".repeat(indent * level)).append(']');
        } else if (node.isObject()) {
            if (node.isEmpty()) {
                out.append("{}");
                return;
            }
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            names.sort(String::compareTo);
            out.append("{\n");
            for (int index = 0; index < names.size(); index++) {
                out.append(" ".repeat(indent * (level + 1)));
                writeAsciiString(out, names.get(index));
                out.append(": ");
                writeAscii(out, node.get(names.get(index)), level + 1, indent);
                if (index + 1 < names.size()) {
                    out.append(',');
                }
                out.append('\n');
            }
            out.append(" ".repeat(indent * level)).append('}');
        }
    }

    private static void writeAsciiString(StringBuilder out, String value) {
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
                    } else if (character < 0x80) {
                        out.append(character);
                    } else if (Character.isHighSurrogate(character)
                            || Character.isLowSurrogate(character)) {
                        int codePoint;
                        if (Character.isHighSurrogate(character) && index + 1 < value.length()
                                && Character.isLowSurrogate(value.charAt(index + 1))) {
                            codePoint = Character.toCodePoint(character, value.charAt(index + 1));
                            index++;
                        } else {
                            codePoint = character;
                        }
                        out.append(String.format("\\u%04x\\u%04x",
                                (int) (char) (codePoint >> 10 | 0xd800),
                                (int) (char) (codePoint & 0x3ff | 0xdc00)));
                    } else {
                        out.append(String.format("\\u%04x", (int) character));
                    }
                }
            }
        }
        out.append('"');
    }

    // ------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------

    /** Python {@code document.get(name, [])}: absent keys default to empty. */
    private static JsonNode getDefaultEmpty(ObjectNode node, String name) {
        return node.has(name) ? node.get(name) : com.fasterxml.jackson.databind.node.MissingNode.getInstance();
    }

    /** Mirrors Python iteration over {@code document.get(name, [])}. */
    private static Iterable<JsonNode> iterableObjects(JsonNode value) {
        if (value == null || value.isMissingNode()) {
            return List.of();
        }
        if (value.isArray()) {
            return value;
        }
        if (value.isObject() || value.isTextual()) {
            return List.of();
        }
        throw new RequestInvalidMarker();
    }

    /** Mirrors Python iteration over a value already guarded to be list-like. */
    private static Iterable<JsonNode> iterableLenient(JsonNode value) {
        if (value == null || value.isMissingNode()) {
            return List.of();
        }
        if (value.isArray()) {
            return value;
        }
        if (value.isObject() || value.isTextual()) {
            return value;
        }
        throw new RequestInvalidMarker();
    }

    private static void addAllStrings(List<JsonNode> strings, JsonNode values) {
        if (values != null && values.isArray()) {
            for (JsonNode value : values) {
                strings.add(value);
            }
        }
    }

    private static JsonNode pythonOr(JsonNode primary, JsonNode fallback) {
        return pythonTruthy(primary) ? primary : fallback;
    }

    private static boolean pythonTruthy(JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return false;
        }
        if (value.isArray() || value.isObject()) {
            return value.size() > 0;
        }
        if (value.isTextual()) {
            return !value.asText().isEmpty();
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        if (value.isNumber()) {
            return value.decimalValue().signum() != 0;
        }
        return true;
    }

    private static long pythonLen(JsonNode value) {
        if (value == null || value.isMissingNode()) {
            return 0;
        }
        if (value.isArray() || value.isObject()) {
            return value.size();
        }
        if (value.isTextual()) {
            return value.asText().length();
        }
        throw new RequestInvalidMarker();
    }

    private static void checkHashable(JsonNode value) {
        if (value != null && (value.isObject() || value.isArray())) {
            throw new RequestInvalidMarker();
        }
    }

    private static boolean containsIdentity(Set<JsonNode> seen, JsonNode value) {
        for (JsonNode existing : seen) {
            if (pythonDeepEquals(existing, value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDuplicate(List<JsonNode> values) {
        for (int left = 0; left < values.size(); left++) {
            for (int right = left + 1; right < values.size(); right++) {
                if (pythonDeepEquals(values.get(left), values.get(right))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isTextEqual(JsonNode node, String expected) {
        return node != null && node.isTextual() && node.asText().equals(expected);
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
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

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Rollback is best-effort, as in the Python consumer.
        }
    }

    /** Mirrors the Python TypeError/KeyError/AttributeError escape hatch. */
    private static final class RequestInvalidMarker extends RuntimeException { }
}

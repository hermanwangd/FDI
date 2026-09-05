package com.featuredeliveryintelligence.fdi.validation.codebaseline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Deterministic PKB-001 code-baseline arm generator. Ports the observable
 * behavior of the transitional Python consumer {@code pkb001_code_baseline.py}:
 * the F1/R1/R2/R3 arm allowlist with exact input-category matching, the shared
 * full-commit-SHA identity binding across all inputs, the structure graph
 * SHA-256 validation (defaulting to {@code 0*64} when no structure input is
 * bound), parent-directory area classification with the {@code fdi} package
 * application rule, structure grouping by area with {@code (source_file, id)}
 * dedup, history grouping by area over {@code src/} changed paths with
 * per-SHA commit dedup, F1 frozen-semantics and R2 frozen-history checks, and
 * proposal assembly with sorted unique refs. Output is proposal-only.
 * {@link IllegalArgumentException} mirrors the Python {@code ValueError}
 * vocabulary; hostile non-JSON shapes raise {@link IllegalStateException},
 * mirroring the Python consumer's uncaught {@code TypeError}/{@code KeyError}.
 */
public final class CodeBaseline {
    static final Map<String, Set<String>> ALLOWED = Map.of(
            "R1", Set.of("structure"),
            "R2", Set.of("history"),
            "R3", Set.of("structure", "history"),
            "F1", Set.of("structure", "semantics"));
    private static final Pattern GIT_SHA = Pattern.compile("[0-9a-f]{40}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final String ZERO_SHA256 = "0".repeat(64);

    /**
     * Ports {@code generate_arm(arm, inputs)}. {@code inputs} values must be
     * JSON objects, like the Python dicts this API replaces.
     */
    public CodeBaselineResult generateArm(String arm, Map<String, JsonNode> inputs) {
        if (arm == null || !ALLOWED.containsKey(arm)
                || inputs == null || !inputs.keySet().equals(ALLOWED.get(arm))) {
            throw new IllegalArgumentException("input categories do not match arm allowlist");
        }
        Identity identity = identity(inputs);
        List<ObjectNode> proposals = new ArrayList<>();

        if ("F1".equals(arm)) {
            proposals.addAll(forwardProposals(inputs, identity));
        } else if ("R1".equals(arm)) {
            for (Map.Entry<String, List<ObjectNode>> area : filesByArea(inputs.get("structure")).entrySet()) {
                proposals.add(proposal(arm, area.getKey(), pyTitle(area.getKey()) + " structural responsibility",
                        componentRefs(area.getValue()), graphEvidenceRefs(area.getValue()), 0.45,
                        List.of("Structure alone does not establish Product meaning.")));
            }
        } else if ("R2".equals(arm)) {
            JsonNode history = requireObject(inputs.get("history"));
            if (!textEquals(history.get("status"), "FROZEN")
                    || !textEquals(history.get("post_cutoff_knowledge_policy"), "EXCLUDE_AFTER_CUTOFF")) {
                throw new IllegalArgumentException("R2 requires frozen cutoff-bounded history");
            }
            for (Map.Entry<String, List<ObjectNode>> area : historyByArea(history).entrySet()) {
                List<ObjectNode> commits = area.getValue();
                Set<String> paths = new TreeSet<>();
                for (ObjectNode commit : commits) {
                    for (JsonNode path : iterateElements(commit, "changed_paths")) {
                        if (!path.isTextual()) {
                            throw new IllegalStateException("changed path is not a string");
                        }
                        if (areaOf(path.asText()).equals(area.getKey())) {
                            paths.add(path.asText());
                        }
                    }
                }
                List<String> evidence = new ArrayList<>();
                for (ObjectNode commit : commits) {
                    evidence.add("git-commit:" + requireText(commit, "commit_sha"));
                }
                proposals.add(proposal(arm, area.getKey(), pyTitle(area.getKey()) + " delivery responsibility",
                        List.copyOf(paths), evidence, 0.4,
                        List.of("Delivery history is evidence, not Product truth.")));
            }
        } else {
            Map<String, List<ObjectNode>> structural = filesByArea(inputs.get("structure"));
            Map<String, List<ObjectNode>> historical = historyByArea(requireObject(inputs.get("history")));
            Set<String> areas = new TreeSet<>(structural.keySet());
            areas.retainAll(historical.keySet());
            for (String area : areas) {
                List<ObjectNode> nodes = structural.get(area);
                List<ObjectNode> commits = historical.get(area);
                List<String> evidence = new ArrayList<>();
                for (ObjectNode commit : commits) {
                    evidence.add("git-commit:" + requireText(commit, "commit_sha"));
                }
                evidence.addAll(graphEvidenceRefs(nodes));
                proposals.add(proposal(arm, area, pyTitle(area) + " implemented delivery capability",
                        componentRefs(nodes), evidence, 0.65,
                        List.of("Capability label requires Product Team review.")));
            }
        }

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("set_id", "pkb001-" + arm.toLowerCase(Locale.ROOT) + "-proposals-v1");
        result.put("run_id", "PKB001-" + arm + "-510a397");
        result.put("arm", arm);
        result.put("source_commit_sha", identity.sourceSha());
        result.put("graph_artifact_sha256", identity.graphSha());
        result.put("authority_status", "PROPOSAL_ONLY");
        ArrayNode proposalArray = result.putArray("proposals");
        proposals.forEach(proposalArray::add);
        return new CodeBaselineResult(result);
    }

    // ------------------------------------------------------------------
    // F1 forward arm
    // ------------------------------------------------------------------

    private List<ObjectNode> forwardProposals(Map<String, JsonNode> inputs, Identity identity) {
        JsonNode semantics = requireObject(inputs.get("semantics"));
        if (!textEquals(semantics.get("status"), "FROZEN")
                || !textEquals(semantics.get("owner"), "PRODUCT_TEAM")) {
            throw new IllegalArgumentException("F1 requires frozen Product Team semantics");
        }
        List<ObjectNode> allNodes = new ArrayList<>();
        filesByArea(inputs.get("structure")).values().forEach(allNodes::addAll);
        List<ObjectNode> proposals = new ArrayList<>();
        for (JsonNode capabilityNode : iterateElements(semantics, "capabilities")) {
            ObjectNode capability = requireObject(capabilityNode);
            Set<String> boundaries = new LinkedHashSet<>();
            for (JsonNode boundary : iterateElements(capability, "expected_realization_boundary")) {
                if (!boundary.isTextual()) {
                    throw new IllegalStateException("boundary is not a string");
                }
                boundaries.add(boundary.asText());
            }
            List<ObjectNode> matched = new ArrayList<>();
            for (ObjectNode node : allNodes) {
                String sourceFile = requireText(node, "source_file");
                for (String boundary : boundaries) {
                    if (sourceFile.startsWith(boundary)) {
                        matched.add(node);
                        break;
                    }
                }
            }
            List<String> limitations = matched.isEmpty()
                    ? List.of("No graph node was found inside the declared boundary.")
                    : List.of();
            proposals.add(proposal("F1", pyStr(requireField(capability, "capability_id")),
                    pyStr(requireField(capability, "name")), componentRefs(matched),
                    graphEvidenceRefs(matched), matched.isEmpty() ? 0.0 : 0.9, limitations));
        }
        return proposals;
    }

    // ------------------------------------------------------------------
    // Grouping and identity
    // ------------------------------------------------------------------

    /** Ports {@code _files_by_area}: area grouping with {@code (source_file, id)} dedup. */
    Map<String, List<ObjectNode>> filesByArea(JsonNode structure) {
        Map<String, List<ObjectNode>> grouped = new LinkedHashMap<>();
        Set<String> seen = new LinkedHashSet<>();
        for (JsonNode nodeNode : iterateElements(structure, "nodes")) {
            ObjectNode node = requireObject(nodeNode);
            JsonNode pathNode = node.get("source_file");
            JsonNode idNode = node.get("id");
            if (pathNode == null || !pathNode.isTextual() || pathNode.asText().isEmpty() || !truthy(idNode)) {
                continue;
            }
            String key = pathNode.asText() + "" + '\0' + idNode.toString();
            if (!seen.add(key)) {
                continue;
            }
            grouped.computeIfAbsent(areaOf(pathNode.asText()), area -> new ArrayList<>()).add(node);
        }
        Map<String, List<ObjectNode>> sorted = new LinkedHashMap<>();
        new TreeSet<>(grouped.keySet()).forEach(area -> {
            List<ObjectNode> rows = new ArrayList<>(grouped.get(area));
            rows.sort((left, right) -> {
                int byPath = requireText(left, "source_file").compareTo(requireText(right, "source_file"));
                return byPath != 0 ? byPath : pyStr(requireField(left, "id")).compareTo(pyStr(requireField(right, "id")));
            });
            sorted.put(area, rows);
        });
        return sorted;
    }

    /** Ports {@code _history_by_area}: {@code src/} area grouping with per-SHA commit dedup. */
    Map<String, List<ObjectNode>> historyByArea(JsonNode history) {
        Map<String, List<ObjectNode>> grouped = new LinkedHashMap<>();
        for (JsonNode commitNode : iterateElements(history, "commits")) {
            ObjectNode commit = requireObject(commitNode);
            Set<String> areas = new LinkedHashSet<>();
            for (JsonNode path : iterateElements(commit, "changed_paths")) {
                if (path.isTextual() && path.asText().startsWith("src/")) {
                    areas.add(areaOf(path.asText()));
                }
            }
            for (String area : areas) {
                grouped.computeIfAbsent(area, key -> new ArrayList<>()).add(commit);
            }
        }
        Map<String, List<ObjectNode>> sorted = new LinkedHashMap<>();
        new TreeSet<>(grouped.keySet()).forEach(area -> {
            Map<String, ObjectNode> bySha = new LinkedHashMap<>();
            grouped.get(area).forEach(row -> bySha.put(requireText(row, "commit_sha"), row));
            List<ObjectNode> rows = new ArrayList<>(bySha.values());
            rows.sort((left, right) -> requireText(left, "commit_sha").compareTo(requireText(right, "commit_sha")));
            sorted.put(area, rows);
        });
        return sorted;
    }

    /** Ports {@code _identity}: one shared full commit SHA across every input. */
    private Identity identity(Map<String, JsonNode> inputs) {
        List<JsonNode> revisions = new ArrayList<>();
        for (JsonNode value : inputs.values()) {
            JsonNode object = requireObject(value);
            JsonNode chosen = truthy(object.get("source_commit_sha"))
                    ? object.get("source_commit_sha") : object.get("applicable_source_commit_sha");
            if (revisions.stream().noneMatch(existing -> pythonEquals(existing, chosen))) {
                revisions.add(chosen);
            }
        }
        if (revisions.size() != 1) {
            throw new IllegalArgumentException("all inputs must bind the same full source commit SHA");
        }
        JsonNode revision = revisions.get(0);
        if (!revision.isTextual()) {
            throw new IllegalStateException("source commit SHA is not a string");
        }
        if (!GIT_SHA.matcher(revision.asText()).matches()) {
            throw new IllegalArgumentException("all inputs must bind the same full source commit SHA");
        }
        JsonNode structure = inputs.get("structure");
        String graphSha = ZERO_SHA256;
        if (structure != null) {
            ObjectNode structureObject = requireObject(structure);
            if (!structureObject.has("graph_sha256")) {
                graphSha = ZERO_SHA256;
            } else {
                JsonNode graphNode = structureObject.get("graph_sha256");
                if (!graphNode.isTextual()) {
                    throw new IllegalStateException("structure graph SHA-256 is not a string");
                }
                graphSha = graphNode.asText();
            }
        }
        if (!SHA256.matcher(graphSha).matches()) {
            throw new IllegalArgumentException("structure graph SHA-256 is invalid");
        }
        return new Identity(revision.asText(), graphSha);
    }

    /** Ports {@code _area}: PurePosixPath parts, the {@code fdi} package rule, parent name fallback. */
    static String areaOf(String path) {
        List<String> parts = pyParts(path);
        int fdi = parts.indexOf("fdi");
        if (fdi >= 0 && fdi + 1 < parts.size()) {
            String candidate = parts.get(fdi + 1);
            return pySuffix(candidate).isEmpty() ? candidate : "application";
        }
        if (parts.size() >= 2) {
            String parent = parts.get(parts.size() - 2);
            return parent.equals("/") ? "" : parent;
        }
        return "";
    }

    /** PurePosixPath-style normalization: split on '/', drop empties and '.', keep '..' and the root. */
    static List<String> pyParts(String path) {
        List<String> parts = new ArrayList<>();
        if (path.startsWith("/")) {
            parts.add("/");
        }
        for (String part : path.split("/", -1)) {
            if (!part.isEmpty() && !part.equals(".")) {
                parts.add(part);
            }
        }
        return parts;
    }

    /** Python {@code Path.suffix}: last dot, not at index 0, not the final character. */
    static String pySuffix(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 && dot < name.length() - 1 ? name.substring(dot) : "";
    }

    // ------------------------------------------------------------------
    // Proposal assembly
    // ------------------------------------------------------------------

    private ObjectNode proposal(String arm, String identifier, String label, List<String> components,
            List<String> evidence, double confidence, List<String> limitations) {
        TreeSet<String> componentRefs = new TreeSet<>(components);
        TreeSet<String> evidenceRefs = new TreeSet<>(evidence);
        ObjectNode proposal = JsonNodeFactory.instance.objectNode();
        proposal.put("proposal_id", arm + "-" + identifier);
        proposal.put("arm", arm);
        proposal.put("target_id", identifier);
        proposal.put("relation_type", "F1".equals(arm) ? "REALIZES" : "CAPABILITY_HYPOTHESIS");
        proposal.put("operation", "CREATE");
        proposal.put("label", label);
        ArrayNode componentArray = proposal.putArray("component_refs");
        componentRefs.forEach(componentArray::add);
        ArrayNode evidenceArray = proposal.putArray("evidence_refs");
        evidenceRefs.forEach(evidenceArray::add);
        proposal.put("confidence", confidence);
        ArrayNode limitationArray = proposal.putArray("limitations");
        limitations.forEach(limitationArray::add);
        proposal.put("authority_status", "PROPOSAL_ONLY");
        return proposal;
    }

    private static List<String> componentRefs(List<ObjectNode> nodes) {
        List<String> refs = new ArrayList<>();
        for (ObjectNode node : nodes) {
            refs.add(requireText(node, "source_file"));
        }
        return refs;
    }

    private static List<String> graphEvidenceRefs(List<ObjectNode> nodes) {
        List<String> refs = new ArrayList<>();
        for (ObjectNode node : nodes) {
            JsonNode location = node.get("source_location");
            refs.add("graph-node:" + pyStr(requireField(node, "id")) + "@"
                    + (location == null ? "UNKNOWN" : pyStr(location)));
        }
        return refs;
    }

    // ------------------------------------------------------------------
    // Python value semantics helpers
    // ------------------------------------------------------------------

    /** Python truthiness of a parsed JSON value. */
    static boolean truthy(JsonNode value) {
        if (value == null || value.isNull()) {
            return false;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isTextual()) {
            return !value.asText().isEmpty();
        }
        if (value.isNumber()) {
            return value.decimalValue().compareTo(BigDecimal.ZERO) != 0;
        }
        if (value.isArray() || value.isObject()) {
            return value.size() != 0;
        }
        return true;
    }

    /** Python {@code ==} on parsed JSON values: numbers and booleans compare numerically. */
    static boolean pythonEquals(JsonNode left, JsonNode right) {
        boolean leftNone = left == null || left.isNull();
        boolean rightNone = right == null || right.isNull();
        if (leftNone || rightNone) {
            return leftNone && rightNone;
        }
        if ((left.isNumber() || left.isBoolean()) && (right.isNumber() || right.isBoolean())) {
            BigDecimal leftDecimal = left.isBoolean()
                    ? BigDecimal.valueOf(left.asBoolean() ? 1 : 0) : left.decimalValue();
            BigDecimal rightDecimal = right.isBoolean()
                    ? BigDecimal.valueOf(right.asBoolean() ? 1 : 0) : right.decimalValue();
            return leftDecimal.compareTo(rightDecimal) == 0;
        }
        return left.equals(right);
    }

    /**
     * Iterates a JSON value like a Python {@code for} over the parsed value:
     * arrays by element, strings by character, objects by field name; a missing
     * field iterates empty like {@code .get(name, [])}; anything else (and
     * explicit null) raises {@link IllegalStateException} like a Python
     * {@code TypeError}.
     */
    static List<JsonNode> iterateElements(JsonNode container, String field) {
        JsonNode value = container.get(field);
        if (value == null) {
            return List.of();
        }
        return iterateValue(value);
    }

    static List<JsonNode> iterateValue(JsonNode value) {
        List<JsonNode> items = new ArrayList<>();
        if (value == null || value.isNull()) {
            throw new IllegalStateException("value is not iterable");
        }
        if (value.isArray()) {
            value.forEach(items::add);
            return items;
        }
        if (value.isTextual()) {
            for (int index = 0; index < value.asText().length(); index++) {
                items.add(JsonNodeFactory.instance.textNode(String.valueOf(value.asText().charAt(index))));
            }
            return items;
        }
        if (value.isObject()) {
            value.fieldNames().forEachRemaining(name -> items.add(JsonNodeFactory.instance.textNode(name)));
            return items;
        }
        throw new IllegalStateException("value is not iterable");
    }

    /** Python {@code str()} of a parsed JSON value for the scalar shapes this consumer stringifies. */
    static String pyStr(JsonNode value) {
        if (value == null || value.isNull()) {
            return "None";
        }
        if (value.isTextual()) {
            return value.asText();
        }
        if (value.isBoolean()) {
            return value.asBoolean() ? "True" : "False";
        }
        if (value.isIntegralNumber()) {
            return value.bigIntegerValue().toString();
        }
        if (value.isNumber()) {
            return CodeBaselineResult.pythonFloat(value.doubleValue());
        }
        throw new IllegalStateException("value is not a scalar string");
    }

    /** Python {@code str.title()}: letters after uncased characters uppercase, other letters lowercase. */
    static String pyTitle(String value) {
        StringBuilder out = new StringBuilder();
        boolean previousCased = false;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            offset += Character.charCount(codePoint);
            boolean cased = Character.isLetter(codePoint);
            if (cased) {
                String letter = new String(Character.toChars(codePoint));
                out.append(previousCased ? letter.toLowerCase(Locale.ROOT) : letter.toUpperCase(Locale.ROOT));
            } else {
                out.appendCodePoint(codePoint);
            }
            previousCased = cased;
        }
        return out.toString();
    }

    private static ObjectNode requireObject(JsonNode value) {
        if (value == null || !value.isObject()) {
            throw new IllegalStateException("value is not a JSON object");
        }
        return (ObjectNode) value;
    }

    private static JsonNode requireField(ObjectNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null) {
            throw new IllegalStateException("missing required field " + field);
        }
        return value;
    }

    private static String requireText(ObjectNode object, String field) {
        JsonNode value = requireField(object, field);
        if (!value.isTextual()) {
            throw new IllegalStateException("field " + field + " is not a string");
        }
        return value.asText();
    }

    private static boolean textEquals(JsonNode value, String expected) {
        return value != null && value.isTextual() && value.asText().equals(expected);
    }

    private record Identity(String sourceSha, String graphSha) { }
}

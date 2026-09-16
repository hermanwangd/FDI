package com.featuredeliveryintelligence.fdi.product.realization.contamination.v1;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Produces bounded diagnostic overlap evidence, never an independence or selection decision. */
public final class H2ExposureComparator {
    public static final int MAX_INPUT_BYTES = 4 * 1024 * 1024;
    private static final ObjectMapper JSON = new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build());
    private static final List<String> DIMENSIONS = List.of("REPOSITORY_IDENTITY", "REPOSITORY_LINEAGE",
            "FUNCTIONAL_CORPUS", "SCENARIO_SET", "TRUTH_SET", "SOURCE_CONTENT", "TEST_CONTENT");
    private static final Pattern TOKENS = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*|[0-9]+|[^\\s]", Pattern.UNICODE_CHARACTER_CLASS);

    /** Exact input bytes are validated and digest-bound before deterministic serialization. */
    public byte[] compare(byte[] bytes) throws Exception {
        if (bytes.length > MAX_INPUT_BYTES) throw new IllegalArgumentException("input exceeds 4 MiB");
        String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        JsonNode input = JSON.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(text);
        exactFields(input, Set.of("schemaVersion", "candidateId", "comparisons"));
        if (!"H2-COMPARISON-INPUT-001".equals(string(input, "schemaVersion"))) throw new IllegalArgumentException("invalid schemaVersion");
        String candidate = string(input, "candidateId");
        if (candidate.isBlank()) throw new IllegalArgumentException("candidateId must be nonblank");
        JsonNode comparisons = input.get("comparisons");
        if (!comparisons.isArray() || comparisons.size() > 64) throw new IllegalArgumentException("comparisons must be array of at most 64");
        Set<String> ids = new HashSet<>(), matches = new HashSet<>();
        ArrayNode results = JSON.createArrayNode();
        for (JsonNode pair : comparisons) {
            exactFields(pair, Set.of("id", "dimension", "left", "right"));
            String id = string(pair, "id"), dimension = string(pair, "dimension");
            if (id.isBlank() || !ids.add(id)) throw new IllegalArgumentException("blank or duplicate comparison id");
            if (!DIMENSIONS.contains(dimension)) throw new IllegalArgumentException("unknown dimension");
            String left = artifact(pair.get("left")), right = artifact(pair.get("right"));
            ObjectNode result = JSON.createObjectNode().put("id", id).put("dimension", dimension)
                    .put("leftSha256", string(pair.get("left"), "sha256"))
                    .put("rightSha256", string(pair.get("right"), "sha256"));
            String outcome = "UNKNOWN", reason = "NO_POSITIVE_OVERLAP_PROOF";
            if (!left.isEmpty() && left.equals(right)) {
                outcome = "MATCH"; reason = "EXACT_NONEMPTY_CONTENT_EQUALITY"; matches.add(dimension);
            } else if (!dimension.equals("REPOSITORY_IDENTITY") && !dimension.equals("REPOSITORY_LINEAGE")) {
                Set<List<String>> a = shingles(left), b = shingles(right);
                Set<List<String>> intersection = new HashSet<>(a); intersection.retainAll(b);
                Set<List<String>> union = new HashSet<>(a); union.addAll(b);
                result.put("intersection", intersection.size()).put("union", union.size());
                if (!a.isEmpty() && !b.isEmpty() && (long)intersection.size() * 5 >= (long)union.size() * 4)
                    reason = "NEAR_DUPLICATE_REVIEW_REQUIRED";
            }
            result.put("result", outcome).put("reason", reason); results.add(result);
        }
        ObjectNode output = JSON.createObjectNode().put("schemaVersion", "H2-COMPARISON-OUTPUT-001")
                .put("candidateId", candidate).put("inputSha256", hash(bytes))
                .put("eligibility", matches.isEmpty() ? "NOT_PROVEN_INDEPENDENT" : "INELIGIBLE")
                .put("selectionAuthorized", false).put("applicability", "SUPPLIED_EXTRACTS_ONLY");
        output.putObject("policy").put("id", "H2-EXACT-AND-SHINGLE-001").put("version", 1)
                .put("shingleTokens", 5).put("thresholdNumerator", 4).put("thresholdDenominator", 5);
        output.set("comparisons", results);
        ArrayNode dimensions = output.putArray("dimensions");
        for (String d : DIMENSIONS) dimensions.addObject().put("dimension", d).put("result", matches.contains(d) ? "MATCH" : "UNKNOWN");
        return (JSON.writeValueAsString(canonical(output)) + "\n").getBytes(StandardCharsets.UTF_8);
    }

    private static String artifact(JsonNode node) throws Exception {
        exactFields(node, Set.of("content", "sha256"));
        String content = string(node, "content"), digest = string(node, "sha256");
        // Reject escaped lone surrogates as well as malformed raw UTF-8: hashing must never replace content.
        byte[] bytes;
        var encoded = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).encode(java.nio.CharBuffer.wrap(content));
        bytes = new byte[encoded.remaining()]; encoded.get(bytes);
        if (bytes.length > 16384) throw new IllegalArgumentException("artifact exceeds 16 KiB");
        if (!digest.matches("[0-9a-f]{64}") || !digest.equals(hash(bytes))) throw new IllegalArgumentException("artifact digest mismatch");
        return content;
    }

    private static Set<List<String>> shingles(String content) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKENS.matcher(content);
        while (matcher.find()) {
            tokens.add(matcher.group());
            if (tokens.size() > 20000) throw new IllegalArgumentException("artifact token limit exceeded");
        }
        Set<List<String>> shingles = new HashSet<>();
        for (int i = 0; i + 5 <= tokens.size(); i++) shingles.add(List.copyOf(tokens.subList(i, i + 5)));
        return shingles;
    }

    private static String string(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) throw new IllegalArgumentException(field + " must be string");
        return value.textValue();
    }

    private static void exactFields(JsonNode node, Set<String> fields) {
        if (node == null || !node.isObject() || node.size() != fields.size()) throw new IllegalArgumentException("invalid object fields");
        node.fieldNames().forEachRemaining(key -> { if (!fields.contains(key)) throw new IllegalArgumentException("unknown field: " + key); });
    }

    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static JsonNode canonical(JsonNode node) {
        if (node.isObject()) {
            ObjectNode out = JSON.createObjectNode(); TreeSet<String> fields = new TreeSet<>(); node.fieldNames().forEachRemaining(fields::add);
            for (String field : fields) out.set(field, canonical(node.get(field)));
            return out;
        }
        if (node.isArray()) { ArrayNode out = JSON.createArrayNode(); node.forEach(value -> out.add(canonical(value))); return out; }
        return node;
    }
}

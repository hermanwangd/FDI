package com.featuredeliveryintelligence.fdi.reverse.input.structural;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseContractValidation;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import com.featuredeliveryintelligence.fdi.reverse.evidence.EvidenceChannelRecord;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceBundle;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceChannel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * Structural input adapter for the deterministic reverse experiment
 * (PKB-BL-009 Slice B / PKB-REVERSE-002). It normalizes only the accepted
 * frozen Graphify snapshot — the byte-exact export captured through
 * {@code CodeIntelligenceProvider} — into the Slice A {@code STRUCTURAL}
 * evidence channel.
 *
 * <p>The adapter is mechanical evidence plumbing, never semantic
 * interpretation: it verifies the pinned SHA-256 before use, requires the
 * snapshot revision to agree with the bound target revision, and emits the
 * provider's nodes and edges as observable structural observations. Provider
 * node ids, labels, source paths, locations, and both canonical and
 * extraction-time edge endpoints are preserved as observation metadata so
 * downstream citations remain traceable; the adapter makes no Product claims
 * and assigns no component roles.
 *
 * <p>Fail-closed with exactly one stable {@link ReverseFailure} code:
 * missing/unreadable input or malformed JSON ({@link ReverseFailure#MISSING_INPUT}),
 * digest mismatch on recompute ({@link ReverseFailure#DIGEST_MISMATCH}),
 * revision absence/disagreement ({@link ReverseFailure#REVISION_MISMATCH}),
 * absolute/traversal paths ({@link ReverseFailure#MALFORMED_PATH}),
 * duplicate provider node identities ({@link ReverseFailure#DUPLICATE_IDENTITY}),
 * and an unsupported snapshot schema version
 * ({@link ReverseFailure#UNSUPPORTED_SCHEMA_VERSION}).
 *
 * <p>Deterministic: nodes are emitted sorted by provider node id, edges by a
 * fixed relation/source/target comparator, statistics use sorted keys, and no
 * wall-clock, locale, HashMap-order, or randomness influences the output.
 */
public final class StructuralEvidenceAdapter {

    private static final ObjectMapper JSON = new ObjectMapper();

    private StructuralEvidenceAdapter() { }

    /**
     * Loads the bound snapshot under the given repository root and returns the
     * normalized {@code STRUCTURAL} evidence channel record.
     *
     * @param binding       exact snapshot binding (identity, revision, path, digest, provenance)
     * @param repositoryRoot readable repository root the input path resolves against
     * @return immutable, validated structural evidence channel record
     * @throws ReverseContractException with exactly one stable failure code on any refusal
     */
    public static EvidenceChannelRecord load(StructuralSnapshotBinding binding, Path repositoryRoot) {
        if (binding == null || repositoryRoot == null || !Files.isDirectory(repositoryRoot))
            throw new ReverseContractException(ReverseFailure.MISSING_INPUT, "repository root must be a readable directory");
        Path root = repositoryRoot.toAbsolutePath().normalize();
        Path input = resolveInsideRoot(root, binding.inputPath());
        if (!Files.isRegularFile(input))
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "structural snapshot is not a readable file: " + binding.inputPath());

        byte[] bytes = readAll(input, binding.inputPath());
        String recomputed = sha256(bytes);
        if (!recomputed.equals(binding.inputSha256()))
            throw new ReverseContractException(
                    ReverseFailure.DIGEST_MISMATCH,
                    "structural snapshot digest mismatch for " + binding.inputPath()
                            + ": bound " + binding.inputSha256() + " but recomputed " + recomputed);

        JsonNode document = parse(bytes, binding.inputPath());
        Snapshot snapshot = normalize(document, binding);

        ObjectNode observations = JSON.createObjectNode();
        observations.set("directed", JSON.getNodeFactory().booleanNode(snapshot.directed));
        observations.set("multigraph", JSON.getNodeFactory().booleanNode(snapshot.multigraph));
        observations.set("snapshot", snapshot.metadata);
        observations.set("nodes", snapshot.nodes);
        observations.set("links", snapshot.links);
        observations.set("statistics", snapshot.statistics);

        return new EvidenceChannelRecord(
                ReverseEvidenceChannel.STRUCTURAL,
                binding.repositoryId(),
                binding.targetRevision(),
                binding.inputPath(),
                binding.inputSha256(),
                ReverseEvidenceBundle.SUPPORTED_SCHEMA_VERSION,
                binding.providerId(),
                binding.provenance(),
                observations);
    }

    // ------------------------------------------------------------------
    // Parsing and normalization (all fail-closed, deterministic)
    // ------------------------------------------------------------------

    private static final class Snapshot {
        boolean directed;
        boolean multigraph;
        ObjectNode metadata;
        ArrayNode nodes;
        ArrayNode links;
        ObjectNode statistics;
    }

    private static Snapshot normalize(JsonNode document, StructuralSnapshotBinding binding) {
        Snapshot snapshot = new Snapshot();
        snapshot.directed = requiredBoolean(document, "directed");
        snapshot.multigraph = requiredBoolean(document, "multigraph");

        if (document.hasNonNull("schema_version")) {
            String schemaVersion = document.get("schema_version").isTextual()
                    ? document.get("schema_version").asText() : null;
            ReverseContractValidation.supportedSchemaVersion(
                    schemaVersion, ReverseEvidenceBundle.SUPPORTED_SCHEMA_VERSION);
        }

        JsonNode graph = requiredObject(document, "graph");
        String name = requiredText(graph, "name", "snapshot graph name");
        String sourceRevision = ReverseContractValidation.canonicalRevision(
                graph.hasNonNull("source_revision") && graph.get("source_revision").isTextual()
                        ? graph.get("source_revision").asText() : null);
        if (!sourceRevision.equals(binding.targetRevision()))
            throw new ReverseContractException(
                    ReverseFailure.REVISION_MISMATCH,
                    "snapshot source revision " + sourceRevision + " disagrees with the bound target revision "
                            + binding.targetRevision());

        ObjectNode metadata = JSON.createObjectNode();
        metadata.put("name", name);
        metadata.put("source_revision", sourceRevision);
        if (graph.hasNonNull("indexed_roots")) {
            JsonNode roots = graph.get("indexed_roots");
            if (!roots.isArray())
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT, "snapshot indexed_roots must be an array");
            ArrayNode orderedRoots = JSON.createArrayNode();
            List<String> rootList = new ArrayList<>();
            for (JsonNode root : roots) {
                if (!root.isTextual())
                    throw new ReverseContractException(
                            ReverseFailure.MISSING_INPUT, "snapshot indexed_roots must contain only strings");
                rootList.add(root.asText());
            }
            rootList.sort(String::compareTo);
            rootList.forEach(orderedRoots::add);
            metadata.set("indexed_roots", orderedRoots);
        }
        if (graph.hasNonNull("extraction")) {
            if (!graph.get("extraction").isTextual())
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT, "snapshot extraction metadata must be a string");
            metadata.put("extraction", graph.get("extraction").asText());
        }
        snapshot.metadata = metadata;

        List<ObjectNode> normalizedNodes = new ArrayList<>();
        Set<String> nodeIds = new HashSet<>();
        ArrayNode rawNodes = requiredArray(document, "nodes");
        for (JsonNode node : rawNodes) {
            if (!node.isObject())
                throw new ReverseContractException(ReverseFailure.MISSING_INPUT, "snapshot nodes must be objects");
            String id = requiredText(node, "id", "snapshot node id");
            if (!nodeIds.add(id))
                throw new ReverseContractException(
                        ReverseFailure.DUPLICATE_IDENTITY, "duplicate structural identity in snapshot: " + id);
            String label = requiredText(node, "label", "snapshot node label");
            String sourceFile = ReverseContractValidation.repositoryPath(
                    requiredText(node, "source_file", "snapshot node source_file for " + id),
                    "snapshot node source_file for " + id);
            String sourceLocation = requiredText(node, "source_location", "snapshot node source_location for " + id);

            ObjectNode normalized = JSON.createObjectNode();
            normalized.put("provider_node_id", id);
            normalized.put("label", label);
            normalized.put("kind", StructuralNodeKind.classify(label).name());
            if (node.hasNonNull("file_type")) {
                if (!node.get("file_type").isTextual())
                    throw new ReverseContractException(
                            ReverseFailure.MISSING_INPUT, "snapshot node file_type must be a string for " + id);
                normalized.put("file_type", node.get("file_type").asText());
            }
            normalized.put("source_file", sourceFile);
            normalized.put("source_location", sourceLocation);
            if (node.hasNonNull("community")) {
                if (!node.get("community").isIntegralNumber())
                    throw new ReverseContractException(
                            ReverseFailure.MISSING_INPUT, "snapshot node community must be an integer for " + id);
                normalized.put("community", node.get("community").intValue());
            }
            normalizedNodes.add(normalized);
        }
        if (normalizedNodes.isEmpty())
            throw new ReverseContractException(ReverseFailure.MISSING_INPUT, "snapshot contains no nodes");
        normalizedNodes.sort(Comparator.comparing(node -> node.get("provider_node_id").asText()));
        ArrayNode nodes = JSON.createArrayNode();
        normalizedNodes.forEach(nodes::add);
        snapshot.nodes = nodes;

        List<ObjectNode> normalizedLinks = new ArrayList<>();
        ArrayNode rawLinks = requiredArray(document, "links");
        for (JsonNode link : rawLinks) {
            if (!link.isObject())
                throw new ReverseContractException(ReverseFailure.MISSING_INPUT, "snapshot links must be objects");
            String relation = requiredText(link, "relation", "snapshot link relation");
            String source = requiredText(link, "source", "snapshot link source for " + relation);
            String target = requiredText(link, "target", "snapshot link target for " + relation);
            if (!nodeIds.contains(source) || !nodeIds.contains(target))
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT,
                        "snapshot link " + relation + " references an unknown node: " + source + " -> " + target);
            String sourceFile = ReverseContractValidation.repositoryPath(
                    requiredText(link, "source_file", "snapshot link source_file for " + relation),
                    "snapshot link source_file for " + relation);
            String sourceLocation = requiredText(link, "source_location", "snapshot link source_location for " + relation);

            ObjectNode normalized = JSON.createObjectNode();
            normalized.put("relation", relation);
            normalized.put("source", source);
            normalized.put("target", target);
            normalized.put("source_file", sourceFile);
            normalized.put("source_location", sourceLocation);
            if (link.hasNonNull("confidence")) {
                if (!link.get("confidence").isTextual())
                    throw new ReverseContractException(
                            ReverseFailure.MISSING_INPUT, "snapshot link confidence must be a string for " + relation);
                normalized.put("confidence", link.get("confidence").asText());
            }
            if (link.hasNonNull("weight")) {
                if (!link.get("weight").isNumber())
                    throw new ReverseContractException(
                            ReverseFailure.MISSING_INPUT, "snapshot link weight must be a number for " + relation);
                normalized.put("weight", link.get("weight").doubleValue());
            }
            if (link.hasNonNull("_src")) {
                if (!link.get("_src").isTextual())
                    throw new ReverseContractException(
                            ReverseFailure.MISSING_INPUT, "snapshot link _src must be a string for " + relation);
                normalized.put("extraction_source", link.get("_src").asText());
            }
            if (link.hasNonNull("_tgt")) {
                if (!link.get("_tgt").isTextual())
                    throw new ReverseContractException(
                            ReverseFailure.MISSING_INPUT, "snapshot link _tgt must be a string for " + relation);
                normalized.put("extraction_target", link.get("_tgt").asText());
            }
            normalizedLinks.add(normalized);
        }
        normalizedLinks.sort(LINK_ORDER);
        ArrayNode links = JSON.createArrayNode();
        normalizedLinks.forEach(links::add);
        snapshot.links = links;

        java.util.Map<StructuralNodeKind, Integer> kindCounts = new TreeMap<>();
        for (ObjectNode node : normalizedNodes) {
            StructuralNodeKind kind = StructuralNodeKind.valueOf(node.get("kind").asText());
            kindCounts.merge(kind, 1, Integer::sum);
        }
        java.util.Map<String, Integer> relationCounts = new TreeMap<>();
        for (ObjectNode link : normalizedLinks) {
            relationCounts.merge(link.get("relation").asText(), 1, Integer::sum);
        }
        ObjectNode statistics = JSON.createObjectNode();
        statistics.put("node_count", normalizedNodes.size());
        statistics.put("link_count", normalizedLinks.size());
        ObjectNode kindNode = JSON.createObjectNode();
        kindCounts.forEach((kind, count) -> kindNode.put(kind.name(), count));
        statistics.set("node_kind_counts", kindNode);
        ObjectNode relationNode = JSON.createObjectNode();
        relationCounts.forEach(relationNode::put);
        statistics.set("relation_counts", relationNode);
        snapshot.statistics = statistics;
        return snapshot;
    }

    private static final Comparator<ObjectNode> LINK_ORDER = Comparator
            .comparing((ObjectNode link) -> link.get("relation").asText())
            .thenComparing(link -> link.get("source").asText())
            .thenComparing(link -> link.get("target").asText())
            .thenComparing(link -> link.get("source_file").asText())
            .thenComparing(link -> link.get("source_location").asText());

    // ------------------------------------------------------------------
    // IO helpers (all fail-closed)
    // ------------------------------------------------------------------

    private static Path resolveInsideRoot(Path root, String repositoryRelativePath) {
        Path resolved = root.resolve(repositoryRelativePath).normalize();
        if (!resolved.startsWith(root))
            throw new ReverseContractException(
                    ReverseFailure.MALFORMED_PATH, "input path escapes the repository root: " + repositoryRelativePath);
        return resolved;
    }

    private static byte[] readAll(Path file, String repositoryRelativePath) {
        try (InputStream stream = Files.newInputStream(file)) {
            return stream.readAllBytes();
        } catch (IOException failure) {
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "structural snapshot is unreadable: " + repositoryRelativePath);
        }
    }

    private static JsonNode parse(byte[] bytes, String repositoryRelativePath) {
        try {
            return JSON.readTree(bytes);
        } catch (JsonProcessingException failure) {
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT,
                    "structural snapshot is not valid JSON: " + repositoryRelativePath);
        } catch (IOException failure) {
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT,
                    "structural snapshot could not be read as JSON: " + repositoryRelativePath);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is not available", failure);
        }
    }

    private static boolean requiredBoolean(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || !value.isBoolean())
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "snapshot field must be a boolean: " + field);
        return value.asBoolean();
    }

    private static JsonNode requiredObject(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || !value.isObject())
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "snapshot field must be an object: " + field);
        return value;
    }

    private static ArrayNode requiredArray(JsonNode object, String field) {
        JsonNode value = object.get(field);
        if (value == null || !value.isArray())
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "snapshot field must be an array: " + field);
        return (ArrayNode) value;
    }

    private static String requiredText(JsonNode object, String field, String what) {
        JsonNode value = object.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank())
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, what + " must not be blank");
        return value.asText();
    }
}

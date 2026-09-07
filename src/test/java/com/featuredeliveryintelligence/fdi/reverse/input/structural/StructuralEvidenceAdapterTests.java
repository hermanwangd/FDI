package com.featuredeliveryintelligence.fdi.reverse.input.structural;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import com.featuredeliveryintelligence.fdi.reverse.ReverseJson;
import com.featuredeliveryintelligence.fdi.reverse.evidence.EvidenceChannelRecord;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceBundle;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceChannel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract tests for the Slice B structural input adapter (PKB-BL-009 /
 * PKB-REVERSE-002). The adapter normalizes only the pinned Graphify snapshot
 * into the Slice A STRUCTURAL evidence channel, preserving provider node/edge
 * identities as traceable observation metadata and making no semantic claims.
 * Every fail-closed rule — missing/unreadable input, digest mismatch, malformed
 * JSON, unsupported schema version, escaped paths, duplicate identities, and
 * revision disagreement — is pinned here against the stable
 * {@link ReverseFailure} vocabulary.
 */
class StructuralEvidenceAdapterTests {

    private static final String REPOSITORY = "spring-petclinic";
    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String PINNED_INPUT =
            "validation/pkb001/reverse-pkb-bl009-petclinic-001/provider-discovery/graph-petclinic-818c413-main-and-test.json";
    private static final String PINNED_DIGEST =
            "5ff8454ae758c8b8ffb1e755ff7c2124efd233388f4f901d8d964b04977c27ca";
    private static final String PROVIDER = "graphify";
    private static final String PROVENANCE =
            "graphify 0.1.14 deterministic tree-sitter AST export captured through CodeIntelligenceProvider (MCP stdio); frozen snapshot pinned by SHA-256";

    @TempDir
    Path repositoryRoot;

    private static StructuralSnapshotBinding binding(String inputPath, String digest) {
        return new StructuralSnapshotBinding(REPOSITORY, REVISION, inputPath, digest, PROVIDER, PROVENANCE);
    }

    /** Minimal accepted-shape snapshot (schema version 1) used by hermetic negative and positive cases. */
    private static String minimalSnapshot(String revision) {
        return """
                {
                  "directed": false,
                  "multigraph": false,
                  "graph": {
                    "name": "minimal-bl009-test-graph",
                    "source_revision": "%s",
                    "indexed_roots": ["src/main/java"],
                    "extraction": "graphify deterministic tree-sitter AST (no semantic/LLM extraction)"
                  },
                  "nodes": [
                    {"label": "A.java", "file_type": "code", "source_file": "src/main/java/A.java", "source_location": "L1", "community": 1, "id": "a"},
                    {"label": "A", "file_type": "code", "source_file": "src/main/java/A.java", "source_location": "L3", "community": 1, "id": "a_a"},
                    {"label": ".m()", "file_type": "code", "source_file": "src/main/java/A.java", "source_location": "L5", "community": 1, "id": "a_a_m"}
                  ],
                  "links": [
                    {"relation": "contains", "confidence": "EXTRACTED", "source_file": "src/main/java/A.java", "source_location": "L3", "weight": 1.0, "_src": "a", "_tgt": "a_a", "source": "a", "target": "a_a"},
                    {"relation": "method", "confidence": "EXTRACTED", "source_file": "src/main/java/A.java", "source_location": "L5", "weight": 1.0, "_src": "a_a", "_tgt": "a_a_m", "source": "a_a", "target": "a_a_m"}
                  ]
                }
                """.formatted(revision);
    }

    private Path writeSnapshot(String relativePath, String content) throws IOException {
        Path file = repositoryRoot.resolve(relativePath).normalize();
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(file));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is not available", failure);
        }
    }

    private EvidenceChannelRecord load(Path file) throws IOException {
        return StructuralEvidenceAdapter.load(binding("snapshots/graph.json", sha256(file)), repositoryRoot);
    }

    // ------------------------------------------------------------------
    // Positive cases
    // ------------------------------------------------------------------

    @Test
    void loadsAcceptedPinnedSnapshotIntoStructuralChannel() throws IOException {
        Path root = Path.of("").toAbsolutePath().normalize();
        StructuralSnapshotBinding pinned = binding(PINNED_INPUT, PINNED_DIGEST);
        EvidenceChannelRecord record = StructuralEvidenceAdapter.load(pinned, root);

        assertThat(record.channel()).isEqualTo(ReverseEvidenceChannel.STRUCTURAL);
        assertThat(record.repositoryId()).isEqualTo(REPOSITORY);
        assertThat(record.canonicalRevision()).isEqualTo(REVISION);
        assertThat(record.inputPath()).isEqualTo(PINNED_INPUT);
        assertThat(record.inputSha256()).isEqualTo(PINNED_DIGEST);
        assertThat(record.schemaVersion()).isEqualTo(ReverseEvidenceBundle.SUPPORTED_SCHEMA_VERSION);
        assertThat(record.providerId()).isEqualTo(PROVIDER);
        assertThat(record.provenance()).isEqualTo(PROVENANCE);

        JsonNode observations = record.observations();
        assertThat(observations.at("/snapshot/name").asText()).isEqualTo("petclinic-818c413-bl009-test-discovery");
        assertThat(observations.at("/snapshot/source_revision").asText()).isEqualTo(REVISION);
        assertThat(observations.at("/statistics/node_count").asInt()).isEqualTo(282);
        assertThat(observations.at("/statistics/link_count").asInt()).isEqualTo(297);
        assertThat(observations.at("/statistics/node_kind_counts/FILE").asInt()).isEqualTo(45);
        assertThat(observations.at("/statistics/node_kind_counts/TYPE").asInt()).isEqualTo(50);
        assertThat(observations.at("/statistics/node_kind_counts/METHOD").asInt()).isEqualTo(187);
        assertThat(observations.at("/statistics/relation_counts/method").asInt()).isEqualTo(187);
        assertThat(observations.at("/statistics/relation_counts/contains").asInt()).isEqualTo(50);
        assertThat(observations.at("/statistics/relation_counts/imports").asInt()).isEqualTo(24);
        assertThat(observations.at("/statistics/relation_counts/calls").asInt()).isEqualTo(36);
    }

    @Test
    void preservesProviderIdentitiesAndSourceBindingsAsObservationMetadata() throws IOException {
        Path root = Path.of("").toAbsolutePath().normalize();
        EvidenceChannelRecord record = StructuralEvidenceAdapter.load(binding(PINNED_INPUT, PINNED_DIGEST), root);
        JsonNode observations = record.observations();

        // Nodes are deterministically ordered by provider node id and carry traceable source bindings.
        JsonNode nodes = observations.get("nodes");
        assertThat(nodes).hasSize(282);
        List<String> ids = new java.util.ArrayList<>();
        nodes.forEach(node -> ids.add(node.get("provider_node_id").asText()));
        assertThat(ids).isSorted();
        assertThat(ids).doesNotHaveDuplicates();

        JsonNode addPet = nodes.get(ids.indexOf("owner_owner_addpet"));
        assertThat(addPet.get("kind").asText()).isEqualTo("METHOD");
        assertThat(addPet.get("label").asText()).isEqualTo(".addPet()");
        assertThat(addPet.get("source_file").asText())
                .isEqualTo("src/main/java/org/springframework/samples/petclinic/owner/Owner.java");
        assertThat(addPet.get("source_location").asText()).isEqualTo("L97");

        // Containment and identity edges survive with both canonical and extraction-time endpoints.
        JsonNode links = observations.get("links");
        assertThat(links).hasSize(297);
        JsonNode contains = links.get(0);
        assertThat(contains.get("relation").asText()).isEqualTo("calls"); // sorted first by relation
        JsonNode containsEdge = null;
        for (JsonNode link : links) {
            if (link.get("relation").asText().equals("contains")
                    && link.get("source").asText().equals("owner")) {
                containsEdge = link;
                break;
            }
        }
        assertThat(containsEdge).isNotNull();
        assertThat(containsEdge.get("target").asText()).isEqualTo("owner_owner");
        assertThat(containsEdge.get("extraction_source").asText()).isNotBlank();
        assertThat(containsEdge.get("extraction_target").asText()).isNotBlank();
    }

    @Test
    void repeatedLoadsAreByteIdenticalUnderCanonicalSerialization() throws IOException {
        Path file = writeSnapshot("snapshots/graph.json", minimalSnapshot(REVISION));
        EvidenceChannelRecord first = load(file);
        EvidenceChannelRecord second = load(file);
        assertThat(ReverseJson.write(first.observations())).isEqualTo(ReverseJson.write(second.observations()));
    }

    @Test
    void normalizedChannelSatisfiesFullEvidenceBundleBindingAndDigestVerification() throws IOException {
        Path file = writeSnapshot("snapshots/graph.json", minimalSnapshot(REVISION));
        EvidenceChannelRecord structural = load(file);

        Files.writeString(repositoryRoot.resolve("snapshots/test-behavior.json"), "{}", StandardCharsets.UTF_8);
        Files.writeString(repositoryRoot.resolve("snapshots/delivery-history.json"), "{}", StandardCharsets.UTF_8);
        EvidenceChannelRecord testBehavior = new EvidenceChannelRecord(
                ReverseEvidenceChannel.TEST_BEHAVIOR, REPOSITORY, REVISION,
                "snapshots/test-behavior.json", sha256(repositoryRoot.resolve("snapshots/test-behavior.json")),
                ReverseEvidenceBundle.SUPPORTED_SCHEMA_VERSION, "java-extractor", "slice-c-fixture",
                JsonNodeFactory.instance.objectNode().put("kind", "TEST_BEHAVIOR"));
        EvidenceChannelRecord deliveryHistory = new EvidenceChannelRecord(
                ReverseEvidenceChannel.DELIVERY_HISTORY, REPOSITORY, REVISION,
                "snapshots/delivery-history.json", sha256(repositoryRoot.resolve("snapshots/delivery-history.json")),
                ReverseEvidenceBundle.SUPPORTED_SCHEMA_VERSION, "git-history", "slice-d-fixture",
                JsonNodeFactory.instance.objectNode().put("kind", "DELIVERY_HISTORY"));

        ReverseEvidenceBundle bundle = new ReverseEvidenceBundle(
                REPOSITORY, REVISION, ReverseEvidenceBundle.SUPPORTED_SCHEMA_VERSION,
                List.of(deliveryHistory, structural, testBehavior));
        ReverseEvidenceBundle.verifyInputDigests(bundle, repositoryRoot);
        assertThat(bundle.channel(ReverseEvidenceChannel.STRUCTURAL).inputSha256())
                .isEqualTo(structural.inputSha256());
    }

    @Test
    void classifiesNodesByStructuralShapeOnly() throws IOException {
        Path file = writeSnapshot("snapshots/graph.json", minimalSnapshot(REVISION));
        JsonNode nodes = load(file).observations().get("nodes");
        assertThat(nodes.get(0).get("kind").asText()).isEqualTo("FILE");
        assertThat(nodes.get(1).get("kind").asText()).isEqualTo("TYPE");
        assertThat(nodes.get(2).get("kind").asText()).isEqualTo("METHOD");
    }

    // ------------------------------------------------------------------
    // Fail-closed negative cases
    // ------------------------------------------------------------------

    @Test
    void missingInputFileFailsClosed() {
        assertThatThrownBy(() -> StructuralEvidenceAdapter.load(
                        binding("snapshots/absent.json", "1".repeat(64)), repositoryRoot))
                .isInstanceOf(ReverseContractException.class)
                .extracting(failure -> ((ReverseContractException) failure).failure())
                .isEqualTo(ReverseFailure.MISSING_INPUT);
    }

    @Test
    void directoryInsteadOfFileFailsClosed() throws IOException {
        Files.createDirectories(repositoryRoot.resolve("snapshots/dir"));
        assertThatThrownBy(() -> StructuralEvidenceAdapter.load(
                        binding("snapshots/dir", "1".repeat(64)), repositoryRoot))
                .isInstanceOf(ReverseContractException.class)
                .extracting(failure -> ((ReverseContractException) failure).failure())
                .isEqualTo(ReverseFailure.MISSING_INPUT);
    }

    @Test
    void digestMismatchOnRecomputeFailsClosed() throws IOException {
        Path file = writeSnapshot("snapshots/graph.json", minimalSnapshot(REVISION));
        assertThatThrownBy(() -> StructuralEvidenceAdapter.load(
                        binding("snapshots/graph.json", "1".repeat(64)), repositoryRoot))
                .isInstanceOf(ReverseContractException.class)
                .extracting(failure -> ((ReverseContractException) failure).failure())
                .isEqualTo(ReverseFailure.DIGEST_MISMATCH);
    }

    @Test
    void malformedJsonFailsClosed() throws IOException {
        Path file = writeSnapshot("snapshots/graph.json", "{ not json");
        assertThatThrownBy(() -> load(file))
                .isInstanceOf(ReverseContractException.class)
                .extracting(failure -> ((ReverseContractException) failure).failure())
                .isEqualTo(ReverseFailure.MISSING_INPUT);
    }

    @Test
    void nonObjectJsonFailsClosed() throws IOException {
        Path file = writeSnapshot("snapshots/graph.json", "[1, 2, 3]");
        assertThatThrownBy(() -> load(file))
                .isInstanceOf(ReverseContractException.class)
                .extracting(failure -> ((ReverseContractException) failure).failure())
                .isEqualTo(ReverseFailure.MISSING_INPUT);
    }

    @Test
    void unsupportedSchemaVersionFailsClosed() throws IOException {
        Path file = writeSnapshot("snapshots/graph.json",
                minimalSnapshot(REVISION).replaceFirst("\\{", "{\"schema_version\": \"2\","));
        assertThatThrownBy(() -> load(file))
                .isInstanceOf(ReverseContractException.class)
                .extracting(failure -> ((ReverseContractException) failure).failure())
                .isEqualTo(ReverseFailure.UNSUPPORTED_SCHEMA_VERSION);
    }

    @Test
    void absentSnapshotRevisionFailsClosed() throws IOException {
        Path file = writeSnapshot("snapshots/graph.json",
                minimalSnapshot(REVISION).replace("\"source_revision\": \"" + REVISION + "\",", ""));
        assertThatThrownBy(() -> load(file))
                .isInstanceOf(ReverseContractException.class)
                .extracting(failure -> ((ReverseContractException) failure).failure())
                .isEqualTo(ReverseFailure.MISSING_INPUT);
    }

    @Test
    void malformedSnapshotRevisionFailsClosed() throws IOException {
        Path file = writeSnapshot("snapshots/graph.json", minimalSnapshot("not-a-revision"));
        assertThatThrownBy(() -> load(file))
                .isInstanceOf(ReverseContractException.class)
                .extracting(failure -> ((ReverseContractException) failure).failure())
                .isEqualTo(ReverseFailure.REVISION_MISMATCH);
    }

    @Test
    void snapshotRevisionDisagreeingWithBoundTargetFailsClosed() throws IOException {
        Path file = writeSnapshot("snapshots/graph.json",
                minimalSnapshot("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
        assertThatThrownBy(() -> load(file))
                .isInstanceOf(ReverseContractException.class)
                .extracting(failure -> ((ReverseContractException) failure).failure())
                .isEqualTo(ReverseFailure.REVISION_MISMATCH);
    }

    @Test
    void absoluteNodeSourcePathFailsClosed() throws IOException {
        Path file = writeSnapshot("snapshots/graph.json",
                minimalSnapshot(REVISION).replace("\"src/main/java/A.java\"", "\"/etc/passwd\""));
        assertThatThrownBy(() -> load(file))
                .isInstanceOf(ReverseContractException.class)
                .extracting(failure -> ((ReverseContractException) failure).failure())
                .isEqualTo(ReverseFailure.MALFORMED_PATH);
    }

    @Test
    void traversalNodeSourcePathFailsClosed() throws IOException {
        Path file = writeSnapshot("snapshots/graph.json",
                minimalSnapshot(REVISION).replace("\"src/main/java/A.java\"", "\"../../outside/A.java\""));
        assertThatThrownBy(() -> load(file))
                .isInstanceOf(ReverseContractException.class)
                .extracting(failure -> ((ReverseContractException) failure).failure())
                .isEqualTo(ReverseFailure.MALFORMED_PATH);
    }

    @Test
    void duplicateStructuralIdentityFailsClosed() throws IOException {
        Path file = writeSnapshot("snapshots/graph.json",
                minimalSnapshot(REVISION).replace("\"id\": \"a_a_m\"", "\"id\": \"a_a\""));
        assertThatThrownBy(() -> load(file))
                .isInstanceOf(ReverseContractException.class)
                .extracting(failure -> ((ReverseContractException) failure).failure())
                .isEqualTo(ReverseFailure.DUPLICATE_IDENTITY);
    }

    @Test
    void linkWithUnknownEndpointFailsClosed() throws IOException {
        Path file = writeSnapshot("snapshots/graph.json",
                minimalSnapshot(REVISION).replace("\"target\": \"a_a_m\"", "\"target\": \"ghost\""));
        assertThatThrownBy(() -> load(file))
                .isInstanceOf(ReverseContractException.class)
                .extracting(failure -> ((ReverseContractException) failure).failure())
                .isEqualTo(ReverseFailure.MISSING_INPUT);
    }

    @Test
    void emptySnapshotNodesFailClosed() throws IOException {
        Path file = writeSnapshot("snapshots/graph.json",
                minimalSnapshot(REVISION).replaceAll(
                        "(?s)\\[\\s*\\{\"label\": \"A.java\".*?\"id\": \"a_a_m\"\\}\\s*\\]", "[]"));
        assertThatThrownBy(() -> load(file))
                .isInstanceOf(ReverseContractException.class)
                .extracting(failure -> ((ReverseContractException) failure).failure())
                .isEqualTo(ReverseFailure.MISSING_INPUT);
    }

    @Test
    void bindingWithEscapedInputPathFailsClosed() {
        assertThatThrownBy(() -> binding("../outside/graph.json", PINNED_DIGEST))
                .isInstanceOf(ReverseContractException.class)
                .extracting(failure -> ((ReverseContractException) failure).failure())
                .isEqualTo(ReverseFailure.MALFORMED_PATH);
    }

    @Test
    void bindingWithMalformedDigestFailsClosed() {
        assertThatThrownBy(() -> binding(PINNED_INPUT, "not-a-digest"))
                .isInstanceOf(ReverseContractException.class)
                .extracting(failure -> ((ReverseContractException) failure).failure())
                .isEqualTo(ReverseFailure.DIGEST_MISMATCH);
    }
}

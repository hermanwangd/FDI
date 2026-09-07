package com.featuredeliveryintelligence.fdi.reverse.input.testbehavior;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import com.featuredeliveryintelligence.fdi.reverse.ReverseJson;
import com.featuredeliveryintelligence.fdi.reverse.evidence.EvidenceChannelRecord;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceChannel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract tests for the Slice C test-behavior input adapter (PKB-BL-009 /
 * PKB-REVERSE-002). Pins the accepted-input load, the canonicalized immutable
 * observation payload, and every fail-closed refusal: missing or unreadable
 * inputs, digest mismatch on recompute, malformed JSON, unsupported schema
 * version, escaped/absolute/traversal paths, duplicate observation identities,
 * package/evidence revision and identity disagreement, and cross-file
 * contradictions. Every refusal must carry exactly one stable
 * {@link ReverseFailure} code.
 */
class TestBehaviorEvidenceAdapterTests {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String SCHEMA = "contracts/test-behavior-evidence.schema.json";
    private static final String PACKAGE_PATH = TestBehaviorEvidenceAdapter.ACCEPTED_PACKAGE_PATH;
    private static final String EVIDENCE_PATH = TestBehaviorEvidenceAdapter.ACCEPTED_EVIDENCE_PATH;

    @TempDir
    Path tempRoot;

    // ------------------------------------------------------------------
    // Positive cases
    // ------------------------------------------------------------------

    @Test
    void acceptedEvidenceLoadsIntoChannelContract() throws Exception {
        EvidenceChannelRecord record = TestBehaviorEvidenceAdapter.loadAccepted(repositoryRoot());

        assertThat(record.channel()).isEqualTo(ReverseEvidenceChannel.TEST_BEHAVIOR);
        assertThat(record.repositoryId()).isEqualTo("spring-petclinic");
        assertThat(record.canonicalRevision()).isEqualTo(REVISION);
        assertThat(record.inputPath()).isEqualTo(EVIDENCE_PATH);
        assertThat(record.inputSha256()).isEqualTo(TestBehaviorEvidenceAdapter.ACCEPTED_EVIDENCE_SHA256);
        assertThat(record.schemaVersion()).isEqualTo("1");
        assertThat(record.providerId()).isEqualTo("fdi-testbehavior-javaparser");
        assertThat(record.provenance())
                .contains("javaparser-test-behavior-extractor:1.0.0")
                .contains(PACKAGE_PATH)
                .contains(TestBehaviorEvidenceAdapter.ACCEPTED_PACKAGE_SHA256);
    }

    @Test
    void acceptedObservationsArePreservedWithUnresolvedEvidenceAndSourceLocations() throws Exception {
        JsonNode observations = TestBehaviorEvidenceAdapter.loadAccepted(repositoryRoot()).observations();

        assertThat(observations.path("schema_version").asText()).isEqualTo("1");
        assertThat(observations.path("repository_id").asText()).isEqualTo("spring-petclinic");
        assertThat(observations.path("canonical_revision").asText()).isEqualTo(REVISION);
        assertThat(observations.path("incomplete").asBoolean()).isTrue();

        JsonNode files = observations.path("test_files");
        assertThat(files.size()).isEqualTo(18);
        long methods = 0;
        long resolved = 0;
        java.util.Map<String, Long> unresolved = new java.util.TreeMap<>();
        for (JsonNode file : files) {
            assertThat(file.path("repository_relative_path").asText())
                    .startsWith("src/test/java/")
                    .doesNotStartWith("/")
                    .doesNotContain("..");
            for (JsonNode method : file.path("test_methods")) {
                methods++;
                JsonNode declaration = method.path("declaration_location");
                assertThat(declaration.path("line").asInt()).isPositive();
                assertThat(declaration.path("column").asInt()).isPositive();
                for (String channel : new String[] {"fixtures", "actions", "assertions"}) {
                    for (JsonNode observation : method.path(channel)) {
                        if (!observation.path("referenced_symbol").isNull()) resolved++;
                        assertThat(observation.path("location").path("repository_relative_path").asText())
                                .isEqualTo(file.path("repository_relative_path").asText());
                    }
                }
                for (JsonNode gap : method.path("unresolved_references")) {
                    unresolved.merge(gap.path("kind").asText(), 1L, Long::sum);
                }
            }
            for (JsonNode gap : file.path("unresolved_references")) {
                unresolved.merge(gap.path("kind").asText(), 1L, Long::sum);
            }
        }
        assertThat(methods).isEqualTo(76);
        assertThat(resolved).isEqualTo(144);
        assertThat(unresolved)
                .containsEntry("EXTERNAL_DEPENDENCY_NOT_RESOLVED", 849L)
                .containsEntry("MISSING_SOURCE", 42L);
    }

    @Test
    void observationsAreCanonicalAndByteStableAcrossLoads() throws Exception {
        EvidenceChannelRecord first = TestBehaviorEvidenceAdapter.loadAccepted(repositoryRoot());
        EvidenceChannelRecord second = TestBehaviorEvidenceAdapter.loadAccepted(repositoryRoot());

        byte[] firstBytes = ReverseJson.write(first.observations());
        byte[] secondBytes = ReverseJson.write(second.observations());
        assertThat(firstBytes).isEqualTo(secondBytes);

        // canonicalize sorts object keys at every depth: first-level keys and
        // the frozen input-digest keys must already be in sorted order
        List<String> topLevel = new ArrayList<>();
        first.observations().fieldNames().forEachRemaining(topLevel::add);
        assertThat(topLevel).isEqualTo(new TreeSet<>(topLevel).stream().toList());
        List<String> digestKeys = new ArrayList<>();
        first.observations().path("input_digests").fieldNames().forEachRemaining(digestKeys::add);
        assertThat(digestKeys).isEqualTo(new TreeSet<>(digestKeys).stream().toList());
    }

    @Test
    void observationsAreDefensivelyCopied() throws Exception {
        EvidenceChannelRecord record = TestBehaviorEvidenceAdapter.loadAccepted(repositoryRoot());
        int fileCount = record.observations().path("test_files").size();

        ((ObjectNode) record.observations()).remove("test_files");

        assertThat(record.observations().path("test_files").size()).isEqualTo(fileCount);
    }

    @Test
    void acceptedEvidenceLoadsFromCopiedRootByByteIdentity() throws Exception {
        writeInputs(tempRoot, realPackageBytes(), realEvidenceBytes());

        EvidenceChannelRecord record = TestBehaviorEvidenceAdapter.load(
                tempRoot, PACKAGE_PATH, sha256(realPackageBytes()), EVIDENCE_PATH, sha256(realEvidenceBytes()));

        assertThat(record.channel()).isEqualTo(ReverseEvidenceChannel.TEST_BEHAVIOR);
        assertThat(record.canonicalRevision()).isEqualTo(REVISION);
        assertThat(record.inputSha256()).isEqualTo(TestBehaviorEvidenceAdapter.ACCEPTED_EVIDENCE_SHA256);
    }

    // ------------------------------------------------------------------
    // Fail-closed negative cases — one stable ReverseFailure code each
    // ------------------------------------------------------------------

    @Test
    void missingPackageFileFailsClosed() throws Exception {
        writeSchema(tempRoot);
        writeEvidenceOnly(tempRoot, realEvidenceBytes());

        assertRefusal(() -> loadWithRealDigests(tempRoot), ReverseFailure.MISSING_INPUT);
    }

    @Test
    void missingEvidenceFileFailsClosed() throws Exception {
        writeSchema(tempRoot);
        write(tempRoot, PACKAGE_PATH, realPackageBytes());

        assertRefusal(() -> loadWithRealDigests(tempRoot), ReverseFailure.MISSING_INPUT);
    }

    @Test
    void corruptedPackageBytesFailClosedOnDigestRecompute() throws Exception {
        byte[] corrupted = corrupt(realPackageBytes());
        writeInputs(tempRoot, corrupted, realEvidenceBytes());

        assertRefusal(() -> loadWithRealDigests(tempRoot), ReverseFailure.DIGEST_MISMATCH);
    }

    @Test
    void corruptedEvidenceBytesFailClosedOnDigestRecompute() throws Exception {
        byte[] corrupted = corrupt(realEvidenceBytes());
        writeInputs(tempRoot, realPackageBytes(), corrupted);

        assertRefusal(() -> loadWithRealDigests(tempRoot), ReverseFailure.DIGEST_MISMATCH);
    }

    @Test
    void malformedPackageJsonFailsClosed() throws Exception {
        byte[] garbage = "{ not json".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        writeInputs(tempRoot, garbage, realEvidenceBytes());

        assertRefusal(
                () -> TestBehaviorEvidenceAdapter.load(
                        tempRoot, PACKAGE_PATH, sha256(garbage), EVIDENCE_PATH,
                        TestBehaviorEvidenceAdapter.ACCEPTED_EVIDENCE_SHA256),
                ReverseFailure.MISSING_INPUT);
    }

    @Test
    void malformedEvidenceJsonFailsClosed() throws Exception {
        byte[] garbage = "{ not json".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        writeInputs(tempRoot, realPackageBytes(), garbage);
        JsonNode packageDocument = JSON.readTree(realPackageBytes());
        ObjectNode bound = (ObjectNode) packageDocument.path("evidence");
        bound.put("sha256", sha256(garbage));
        bound.put("bytes", garbage.length);
        byte[] repackaged = JSON.writeValueAsBytes(packageDocument);
        write(tempRoot, PACKAGE_PATH, repackaged);

        assertRefusal(
                () -> TestBehaviorEvidenceAdapter.load(
                        tempRoot, PACKAGE_PATH, sha256(repackaged), EVIDENCE_PATH, sha256(garbage)),
                ReverseFailure.MISSING_INPUT);
    }

    @Test
    void unsupportedEvidenceSchemaVersionFailsClosed() throws Exception {
        ObjectNode evidence = (ObjectNode) JSON.readTree(realEvidenceBytes());
        evidence.put("schema_version", "2");
        byte[] changed = JSON.writeValueAsBytes(evidence);
        byte[] repackaged = rebindEvidencePackage(realPackageBytes(), changed);
        writeInputs(tempRoot, repackaged, changed);

        assertRefusal(
                () -> TestBehaviorEvidenceAdapter.load(
                        tempRoot, PACKAGE_PATH, sha256(repackaged), EVIDENCE_PATH, sha256(changed)),
                ReverseFailure.UNSUPPORTED_SCHEMA_VERSION);
    }

    @Test
    void absoluteEvidencePathFailsClosed() throws Exception {
        writeInputs(tempRoot, realPackageBytes(), realEvidenceBytes());

        assertRefusal(
                () -> TestBehaviorEvidenceAdapter.load(
                        tempRoot, PACKAGE_PATH, sha256(realPackageBytes()),
                        "/escaped/evidence.json", TestBehaviorEvidenceAdapter.ACCEPTED_EVIDENCE_SHA256),
                ReverseFailure.MALFORMED_PATH);
    }

    @Test
    void traversalPackagePathFailsClosed() throws Exception {
        writeInputs(tempRoot, realPackageBytes(), realEvidenceBytes());

        assertRefusal(
                () -> TestBehaviorEvidenceAdapter.load(
                        tempRoot, "../" + PACKAGE_PATH, sha256(realPackageBytes()),
                        EVIDENCE_PATH, TestBehaviorEvidenceAdapter.ACCEPTED_EVIDENCE_SHA256),
                ReverseFailure.MALFORMED_PATH);
    }

    @Test
    void duplicateObservationIdentityFailsClosed() throws Exception {
        ObjectNode evidence = (ObjectNode) JSON.readTree(realEvidenceBytes());
        JsonNode duplicate = evidence.path("test_files").get(0).deepCopy();
        ((com.fasterxml.jackson.databind.node.ArrayNode) evidence.path("test_files")).add(duplicate);
        byte[] changed = JSON.writeValueAsBytes(evidence);
        byte[] repackaged = rebindEvidencePackage(realPackageBytes(), changed);
        writeInputs(tempRoot, repackaged, changed);

        assertRefusal(
                () -> TestBehaviorEvidenceAdapter.load(
                        tempRoot, PACKAGE_PATH, sha256(repackaged), EVIDENCE_PATH, sha256(changed)),
                ReverseFailure.DUPLICATE_IDENTITY);
    }

    @Test
    void packageEvidenceRevisionDisagreementFailsClosed() throws Exception {
        ObjectNode packageDocument = (ObjectNode) JSON.readTree(realPackageBytes());
        ((ObjectNode) packageDocument.path("source_binding"))
                .put("canonical_revision", "0000000000000000000000000000000000000000");
        byte[] changed = JSON.writeValueAsBytes(packageDocument);
        writeInputs(tempRoot, changed, realEvidenceBytes());

        assertRefusal(
                () -> TestBehaviorEvidenceAdapter.load(
                        tempRoot, PACKAGE_PATH, sha256(changed), EVIDENCE_PATH,
                        TestBehaviorEvidenceAdapter.ACCEPTED_EVIDENCE_SHA256),
                ReverseFailure.REVISION_MISMATCH);
    }

    @Test
    void packageEvidenceRepositoryIdentityDisagreementFailsClosed() throws Exception {
        ObjectNode evidence = (ObjectNode) JSON.readTree(realEvidenceBytes());
        evidence.put("repository_id", "other-repository");
        byte[] changed = JSON.writeValueAsBytes(evidence);
        byte[] repackaged = rebindEvidencePackage(realPackageBytes(), changed);
        writeInputs(tempRoot, repackaged, changed);

        assertRefusal(
                () -> TestBehaviorEvidenceAdapter.load(
                        tempRoot, PACKAGE_PATH, sha256(repackaged), EVIDENCE_PATH, sha256(changed)),
                ReverseFailure.DUPLICATE_IDENTITY);
    }

    @Test
    void packageEvidenceProviderProvenanceDisagreementFailsClosed() throws Exception {
        ObjectNode packageDocument = (ObjectNode) JSON.readTree(realPackageBytes());
        ((ObjectNode) packageDocument.path("evidence")).put("provider_id", "other-provider");
        byte[] changed = JSON.writeValueAsBytes(packageDocument);
        writeInputs(tempRoot, changed, realEvidenceBytes());

        assertRefusal(
                () -> TestBehaviorEvidenceAdapter.load(
                        tempRoot, PACKAGE_PATH, sha256(changed), EVIDENCE_PATH,
                        TestBehaviorEvidenceAdapter.ACCEPTED_EVIDENCE_SHA256),
                ReverseFailure.DUPLICATE_IDENTITY);
    }

    @Test
    void packageBoundEvidenceDigestMismatchFailsClosed() throws Exception {
        ObjectNode packageDocument = (ObjectNode) JSON.readTree(realPackageBytes());
        ((ObjectNode) packageDocument.path("evidence"))
                .put("sha256", "0".repeat(64));
        byte[] changed = JSON.writeValueAsBytes(packageDocument);
        writeInputs(tempRoot, changed, realEvidenceBytes());

        assertRefusal(
                () -> TestBehaviorEvidenceAdapter.load(
                        tempRoot, PACKAGE_PATH, sha256(changed), EVIDENCE_PATH,
                        TestBehaviorEvidenceAdapter.ACCEPTED_EVIDENCE_SHA256),
                ReverseFailure.DIGEST_MISMATCH);
    }

    @Test
    void packageBoundEvidenceFileDisagreementFailsClosed() throws Exception {
        ObjectNode packageDocument = (ObjectNode) JSON.readTree(realPackageBytes());
        ((ObjectNode) packageDocument.path("evidence"))
                .put("file", "validation/pkb001/other-evidence.json");
        byte[] changed = JSON.writeValueAsBytes(packageDocument);
        writeInputs(tempRoot, changed, realEvidenceBytes());

        assertRefusal(
                () -> TestBehaviorEvidenceAdapter.load(
                        tempRoot, PACKAGE_PATH, sha256(changed), EVIDENCE_PATH,
                        TestBehaviorEvidenceAdapter.ACCEPTED_EVIDENCE_SHA256),
                ReverseFailure.MISSING_INPUT);
    }

    @Test
    void declaredTestMethodCountContradictionFailsClosed() throws Exception {
        ObjectNode packageDocument = (ObjectNode) JSON.readTree(realPackageBytes());
        ((ObjectNode) packageDocument.path("evidence")).put("test_methods", 75);
        byte[] changed = JSON.writeValueAsBytes(packageDocument);
        writeInputs(tempRoot, changed, realEvidenceBytes());

        assertRefusal(
                () -> TestBehaviorEvidenceAdapter.load(
                        tempRoot, PACKAGE_PATH, sha256(changed), EVIDENCE_PATH,
                        TestBehaviorEvidenceAdapter.ACCEPTED_EVIDENCE_SHA256),
                ReverseFailure.MISSING_INPUT);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static Path repositoryRoot() {
        return Path.of("").toAbsolutePath();
    }

    private static byte[] realPackageBytes() throws IOException {
        return Files.readAllBytes(repositoryRoot().resolve(PACKAGE_PATH));
    }

    private static byte[] realEvidenceBytes() throws IOException {
        return Files.readAllBytes(repositoryRoot().resolve(EVIDENCE_PATH));
    }

    /** Rewrites the package's evidence binding (digest + byte size) after an evidence mutation. */
    private static byte[] rebindEvidencePackage(byte[] packageBytes, byte[] evidenceBytes) throws IOException {
        ObjectNode packageDocument = (ObjectNode) JSON.readTree(packageBytes);
        ObjectNode bound = (ObjectNode) packageDocument.path("evidence");
        bound.put("sha256", sha256(evidenceBytes));
        bound.put("bytes", evidenceBytes.length);
        return JSON.writeValueAsBytes(packageDocument);
    }

    private static byte[] corrupt(byte[] bytes) {
        byte[] copy = bytes.clone();
        copy[copy.length / 2] ^= (byte) 0x01;
        return copy;
    }

    private static void loadWithRealDigests(Path root) throws IOException {
        TestBehaviorEvidenceAdapter.load(
                root, PACKAGE_PATH, TestBehaviorEvidenceAdapter.ACCEPTED_PACKAGE_SHA256,
                EVIDENCE_PATH, TestBehaviorEvidenceAdapter.ACCEPTED_EVIDENCE_SHA256);
    }

    private static void assertRefusal(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable, ReverseFailure code) {
        assertThatThrownBy(callable)
                .isInstanceOf(ReverseContractException.class)
                .satisfies(error -> assertThat(((ReverseContractException) error).failure()).isEqualTo(code));
    }

    private static void writeInputs(Path root, byte[] packageBytes, byte[] evidenceBytes) throws IOException {
        writeSchema(root);
        write(root, PACKAGE_PATH, packageBytes);
        writeEvidenceOnly(root, evidenceBytes);
    }

    private static void writeEvidenceOnly(Path root, byte[] evidenceBytes) throws IOException {
        write(root, EVIDENCE_PATH, evidenceBytes);
    }

    private static void writeSchema(Path root) throws IOException {
        write(root, SCHEMA, Files.readAllBytes(repositoryRoot().resolve(SCHEMA)));
    }

    private static void write(Path root, String relativePath, byte[] bytes) throws IOException {
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root.toAbsolutePath().normalize()))
            throw new IllegalArgumentException("test helper refuses escaped path: " + relativePath);
        Files.createDirectories(target.getParent());
        Files.write(target, bytes);
    }

    private static String sha256(byte[] bytes) {
        try {
            StringBuilder hex = new StringBuilder(64);
            for (byte value : MessageDigest.getInstance("SHA-256").digest(bytes)) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is not available", failure);
        }
    }
}

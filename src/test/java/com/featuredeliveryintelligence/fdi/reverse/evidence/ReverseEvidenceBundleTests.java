package com.featuredeliveryintelligence.fdi.reverse.evidence;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract tests for the provider-neutral reverse evidence bundle
 * (PKB-BL-009 Slice A). Every fail-closed binding rule — required channels,
 * duplicate identities, identity disagreement, digest, revision, path, and
 * schema-version validation plus digest recomputation — is pinned here.
 */
class ReverseEvidenceBundleTests {

    private static final String REPOSITORY = "spring-petclinic";
    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String DIGEST_STRUCTURAL = "1".repeat(64);
    private static final String DIGEST_TEST = "2".repeat(64);
    private static final String DIGEST_HISTORY = "3".repeat(64);

    private static EvidenceChannelRecord channel(
            ReverseEvidenceChannel channel, String repository, String revision, String path, String digest) {
        JsonNode observations = JsonNodeFactory.instance.objectNode().put("kind", channel.name());
        return new EvidenceChannelRecord(
                channel, repository, revision, path, digest,
                ReverseEvidenceBundle.SUPPORTED_SCHEMA_VERSION, "provider-" + channel.name().toLowerCase(),
                "slice-a-contract-test", observations);
    }

    private static List<EvidenceChannelRecord> allChannels() {
        return List.of(
                channel(ReverseEvidenceChannel.STRUCTURAL, REPOSITORY, REVISION, "validation/pkb001/graphify/snapshot.json", DIGEST_STRUCTURAL),
                channel(ReverseEvidenceChannel.TEST_BEHAVIOR, REPOSITORY, REVISION, "validation/pkb001/test-behavior/evidence.json", DIGEST_TEST),
                channel(ReverseEvidenceChannel.DELIVERY_HISTORY, REPOSITORY, REVISION, "validation/pkb001/datasets/petclinic-delivery-history.json", DIGEST_HISTORY));
    }

    private static ReverseEvidenceBundle bundle(List<EvidenceChannelRecord> channels) {
        return new ReverseEvidenceBundle(REPOSITORY, REVISION, ReverseEvidenceBundle.SUPPORTED_SCHEMA_VERSION, channels);
    }

    @Test
    void validBundleIsAcceptedInDeterministicChannelOrder() {
        List<EvidenceChannelRecord> shuffled = new ArrayList<>(allChannels());
        java.util.Collections.reverse(shuffled);
        ReverseEvidenceBundle accepted = bundle(shuffled);
        assertThat(accepted.channels()).extracting(EvidenceChannelRecord::channel).containsExactly(
                ReverseEvidenceChannel.STRUCTURAL,
                ReverseEvidenceChannel.TEST_BEHAVIOR,
                ReverseEvidenceChannel.DELIVERY_HISTORY);
        assertThat(accepted.channel(ReverseEvidenceChannel.TEST_BEHAVIOR).inputPath())
                .isEqualTo("validation/pkb001/test-behavior/evidence.json");
        assertThatThrownBy(() -> accepted.channels().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void missingRequiredChannelFailsClosed() {
        List<EvidenceChannelRecord> withoutHistory = allChannels().stream()
                .filter(record -> record.channel() != ReverseEvidenceChannel.DELIVERY_HISTORY)
                .toList();
        assertThatThrownBy(() -> bundle(withoutHistory))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.MISSING_INPUT);
    }

    @Test
    void duplicateChannelFailsClosed() {
        List<EvidenceChannelRecord> channels = new ArrayList<>(allChannels());
        channels.add(channel(ReverseEvidenceChannel.STRUCTURAL, REPOSITORY, REVISION, "validation/pkb001/graphify/other.json", DIGEST_STRUCTURAL));
        assertThatThrownBy(() -> bundle(channels))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.DUPLICATE_IDENTITY);
    }

    @Test
    void channelRevisionDisagreementFailsClosed() {
        List<EvidenceChannelRecord> channels = new ArrayList<>(allChannels());
        channels.set(1, channel(ReverseEvidenceChannel.TEST_BEHAVIOR, REPOSITORY,
                "a".repeat(40), "validation/pkb001/test-behavior/evidence.json", DIGEST_TEST));
        assertThatThrownBy(() -> bundle(channels))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.REVISION_MISMATCH);
    }

    @Test
    void channelRepositoryDisagreementFailsClosed() {
        List<EvidenceChannelRecord> channels = new ArrayList<>(allChannels());
        channels.set(0, channel(ReverseEvidenceChannel.STRUCTURAL, "other-repo", REVISION,
                "validation/pkb001/graphify/snapshot.json", DIGEST_STRUCTURAL));
        assertThatThrownBy(() -> bundle(channels))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.DUPLICATE_IDENTITY);
    }

    @Test
    void malformedBundleRevisionFailsClosed() {
        assertThatThrownBy(() -> new ReverseEvidenceBundle(
                REPOSITORY, "abc123", ReverseEvidenceBundle.SUPPORTED_SCHEMA_VERSION, allChannels()))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.REVISION_MISMATCH);
        assertThatThrownBy(() -> new ReverseEvidenceBundle(
                REPOSITORY, " ", ReverseEvidenceBundle.SUPPORTED_SCHEMA_VERSION, allChannels()))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.MISSING_INPUT);
    }

    @Test
    void escapedInputPathFailsClosed() {
        assertThatThrownBy(() -> channel(ReverseEvidenceChannel.STRUCTURAL, REPOSITORY, REVISION, "../outside.json", DIGEST_STRUCTURAL))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.MALFORMED_PATH);
        assertThatThrownBy(() -> channel(ReverseEvidenceChannel.STRUCTURAL, REPOSITORY, REVISION, "/abs/path.json", DIGEST_STRUCTURAL))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.MALFORMED_PATH);
        assertThatThrownBy(() -> channel(ReverseEvidenceChannel.STRUCTURAL, REPOSITORY, REVISION, "a//b.json", DIGEST_STRUCTURAL))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.MALFORMED_PATH);
    }

    @Test
    void malformedDigestFailsClosed() {
        assertThatThrownBy(() -> channel(ReverseEvidenceChannel.STRUCTURAL, REPOSITORY, REVISION, "validation/pkb001/graphify/snapshot.json", "abc"))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.DIGEST_MISMATCH);
    }

    @Test
    void unsupportedSchemaVersionFailsClosed() {
        JsonNode observations = JsonNodeFactory.instance.objectNode();
        assertThatThrownBy(() -> new EvidenceChannelRecord(
                ReverseEvidenceChannel.STRUCTURAL, REPOSITORY, REVISION,
                "validation/pkb001/graphify/snapshot.json", DIGEST_STRUCTURAL,
                "2", "provider", "slice-a-contract-test", observations))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.UNSUPPORTED_SCHEMA_VERSION);
    }

    @Test
    void absentObservationsFailClosed() {
        assertThatThrownBy(() -> new EvidenceChannelRecord(
                ReverseEvidenceChannel.STRUCTURAL, REPOSITORY, REVISION,
                "validation/pkb001/graphify/snapshot.json", DIGEST_STRUCTURAL,
                ReverseEvidenceBundle.SUPPORTED_SCHEMA_VERSION, "provider", "slice-a-contract-test", null))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.MISSING_INPUT);
    }

    @Test
    void observationsAreDeepCopiedInAndOut() {
        com.fasterxml.jackson.databind.node.ObjectNode payload =
                JsonNodeFactory.instance.objectNode().put("key", "original");
        EvidenceChannelRecord record = new EvidenceChannelRecord(
                ReverseEvidenceChannel.STRUCTURAL, REPOSITORY, REVISION,
                "validation/pkb001/graphify/snapshot.json", DIGEST_STRUCTURAL,
                ReverseEvidenceBundle.SUPPORTED_SCHEMA_VERSION, "provider", "slice-a-contract-test", payload);
        payload.put("key", "mutated");
        assertThat(record.observations().get("key").asText()).isEqualTo("original");
        ((com.fasterxml.jackson.databind.node.ObjectNode) record.observations()).put("key", "mutated-again");
        assertThat(record.observations().get("key").asText()).isEqualTo("original");
    }

    @Test
    void digestRecomputeMatchesBoundDigest(@TempDir Path repositoryRoot) throws Exception {
        String[] paths = {
                "validation/pkb001/graphify/snapshot.json",
                "validation/pkb001/test-behavior/evidence.json",
                "validation/pkb001/datasets/petclinic-delivery-history.json"};
        ReverseEvidenceChannel[] channelIds = ReverseEvidenceChannel.values();
        List<EvidenceChannelRecord> channels = new ArrayList<>();
        for (int index = 0; index < paths.length; index++) {
            Path input = repositoryRoot.resolve(paths[index]);
            Files.createDirectories(input.getParent());
            byte[] content = ("{\"channel\":\"" + channelIds[index].name() + "\"}").getBytes(StandardCharsets.UTF_8);
            Files.write(input, content);
            String recomputed = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
            channels.add(channel(channelIds[index], REPOSITORY, REVISION, paths[index], recomputed));
        }
        ReverseEvidenceBundle accepted = bundle(channels);

        ReverseEvidenceBundle.verifyInputDigests(accepted, repositoryRoot);
    }

    @Test
    void digestMismatchOnRecomputeFailsClosed(@TempDir Path repositoryRoot) throws Exception {
        Path input = repositoryRoot.resolve("validation/pkb001/graphify/snapshot.json");
        Files.createDirectories(input.getParent());
        Files.write(input, "tampered".getBytes(StandardCharsets.UTF_8));
        ReverseEvidenceBundle accepted = bundle(allChannels());
        assertThatThrownBy(() -> ReverseEvidenceBundle.verifyInputDigests(accepted, repositoryRoot))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.DIGEST_MISMATCH);
    }

    @Test
    void missingInputFileFailsClosed(@TempDir Path repositoryRoot) {
        ReverseEvidenceBundle accepted = bundle(allChannels());
        assertThatThrownBy(() -> ReverseEvidenceBundle.verifyInputDigests(accepted, repositoryRoot))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.MISSING_INPUT);
    }
}

package com.featuredeliveryintelligence.fdi.reverse.input.deliveryhistory;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import com.featuredeliveryintelligence.fdi.reverse.ReverseJson;
import com.featuredeliveryintelligence.fdi.reverse.evidence.EvidenceChannelRecord;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceBundle;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceChannel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fail-closed and determinism tests for the Slice D delivery-history input
 * adapter (PKB-BL-009). Covers the pinned dataset happy path plus every
 * negative case required by the execution envelope: missing/unreadable
 * input, digest mismatch, malformed JSON, unsupported schema version,
 * escaped/absolute/traversal paths, duplicate commit/episode/path
 * identities, and history revision disagreement.
 */
class DeliveryHistoryInputAdapterTests {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String REPOSITORY = "spring-petclinic";
    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String OTHER_REVISION = "0000000000000000000000000000000000000001";
    private static final String COMMIT_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String COMMIT_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String COMMIT_C = "cccccccccccccccccccccccccccccccccccccccc";

    @TempDir
    Path root;

    private Path writeDataset(JsonNode dataset) throws IOException {
        Path input = root.resolve(DeliveryHistoryInputAdapter.INPUT_PATH);
        Files.createDirectories(input.getParent());
        byte[] bytes = JSON.writeValueAsBytes(dataset);
        Files.write(input, bytes);
        return input;
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder hex = new StringBuilder(64);
            for (byte value : digest.digest(bytes)) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is not available", failure);
        }
    }

    private static ObjectNode validDataset() {
        ObjectNode dataset = JSON.createObjectNode();
        dataset.put("dataset_id", DeliveryHistoryInputAdapter.DATASET_ID);
        dataset.put("status", "FROZEN");
        dataset.put("source_commit_sha", REVISION);
        dataset.put("history_cutoff", "2026-08-26T10:57:54Z");
        dataset.put("post_cutoff_knowledge_policy", "EXCLUDE_AFTER_CUTOFF");
        dataset.put("evidence_boundary", "Git and pull-request evidence only; not Product truth.");
        ArrayNode limitations = dataset.putArray("limitations");
        limitations.add("z-limitation");
        limitations.add("a-limitation");
        ArrayNode commits = dataset.putArray("commits");
        commits.add(commit(COMMIT_B, "2020-02-02T02:02:02Z", "subject b", List.of("src/B.java", "src/A.java")));
        commits.add(commit(COMMIT_A, "2020-01-01T01:01:01Z", "subject a", List.of()));
        ArrayNode pullRequests = dataset.putArray("pull_requests");
        ObjectNode episode = pullRequests.addObject();
        episode.put("number", 7);
        episode.put("title", "Add feature");
        episode.put("state", "MERGED");
        episode.put("url", "https://github.com/spring-projects/spring-petclinic/pull/7");
        episode.put("created_at", "2020-03-03T03:03:03Z");
        episode.put("updated_at", "2020-03-04T03:03:03Z");
        episode.put("head_ref_oid", COMMIT_C);
        episode.put("merge_commit_sha", COMMIT_B);
        ArrayNode included = episode.putArray("included_commit_shas");
        included.add(COMMIT_B);
        included.add(COMMIT_A);
        return dataset;
    }

    private static ObjectNode commit(String sha, String committedAt, String subject, List<String> paths) {
        ObjectNode commit = JSON.createObjectNode();
        commit.put("commit_sha", sha);
        commit.put("committed_at", committedAt);
        commit.put("subject", subject);
        ArrayNode changedPaths = commit.putArray("changed_paths");
        paths.forEach(changedPaths::add);
        return commit;
    }

    private EvidenceChannelRecord loadValid() throws IOException {
        byte[] bytes = JSON.writeValueAsBytes(validDataset());
        writeDataset(validDataset());
        return DeliveryHistoryInputAdapter.load(root, REPOSITORY, REVISION, sha256(bytes));
    }

    private static void assertFailure(ReverseFailure expected, org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(ReverseContractException.class)
                .extracting(failure -> ((ReverseContractException) failure).failure())
                .isEqualTo(expected);
    }

    @Test
    void loadsPinnedDatasetAndBindsTraceableObservations() throws IOException {
        Path repositoryRoot = Path.of("").toAbsolutePath();
        Path input = repositoryRoot.resolve(DeliveryHistoryInputAdapter.INPUT_PATH);
        assertThat(input).isRegularFile();
        String digest = sha256(Files.readAllBytes(input));

        EvidenceChannelRecord record = DeliveryHistoryInputAdapter.load(
                repositoryRoot, REPOSITORY, REVISION, DeliveryHistoryInputAdapter.PINNED_INPUT_SHA256);

        assertThat(record.channel()).isEqualTo(ReverseEvidenceChannel.DELIVERY_HISTORY);
        assertThat(record.repositoryId()).isEqualTo(REPOSITORY);
        assertThat(record.canonicalRevision()).isEqualTo(REVISION);
        assertThat(record.inputPath()).isEqualTo(DeliveryHistoryInputAdapter.INPUT_PATH);
        assertThat(record.inputSha256()).isEqualTo(digest).isEqualTo(DeliveryHistoryInputAdapter.PINNED_INPUT_SHA256);
        assertThat(record.schemaVersion()).isEqualTo(ReverseEvidenceBundle.SUPPORTED_SCHEMA_VERSION);
        assertThat(record.providerId()).isEqualTo(DeliveryHistoryInputAdapter.PROVIDER_ID);
        assertThat(record.provenance()).isEqualTo(DeliveryHistoryInputAdapter.PROVENANCE);

        JsonNode observations = record.observations();
        assertThat(observations.get("schema_version").asText())
                .isEqualTo(DeliveryHistoryInputAdapter.DATASET_SCHEMA_VERSION);
        assertThat(observations.get("dataset_id").asText()).isEqualTo(DeliveryHistoryInputAdapter.DATASET_ID);
        assertThat(observations.get("source_commit_sha").asText()).isEqualTo(REVISION);
        assertThat(observations.get("commits").size()).isEqualTo(1042);
        assertThat(observations.get("changed_paths").size()).isEqualTo(6009);
        assertThat(observations.get("episodes").size()).isEqualTo(91);

        // Commit text is preserved verbatim as traceable historical evidence, never rewritten.
        JsonNode initialCommit = null;
        for (JsonNode node : observations.get("commits")) {
            if (node.get("commit_sha").asText().equals("349fdef18bcf688709f19097c140c69724905768"))
                initialCommit = node;
        }
        assertThat(initialCommit).isNotNull();
        assertThat(initialCommit.get("subject").asText()).isEqualTo("Initial commit");

        // Fixed ordering: commits by sha, episodes by number, changed paths by sha then path.
        List<String> shas = new ArrayList<>();
        observations.get("commits").forEach(node -> shas.add(node.get("commit_sha").asText()));
        assertThat(shas).isEqualTo(shas.stream().sorted().toList());
        List<Integer> episodeNumbers = new ArrayList<>();
        observations.get("episodes").forEach(node -> episodeNumbers.add(node.get("number").asInt()));
        assertThat(episodeNumbers).isEqualTo(episodeNumbers.stream().sorted().toList());
        List<String> pathOrder = new ArrayList<>();
        observations.get("changed_paths").forEach(node ->
                pathOrder.add(node.get("commit_sha").asText() + " " + node.get("path").asText()));
        assertThat(pathOrder).isEqualTo(pathOrder.stream().sorted().toList());
    }

    @Test
    void repeatedLoadsAreByteIdentical() throws IOException {
        EvidenceChannelRecord first = loadValid();
        EvidenceChannelRecord second = loadValid();
        assertThat(ReverseJson.write(first.observations())).isEqualTo(ReverseJson.write(second.observations()));
        // The bound payload is defensively copied: mutating a read copy changes nothing.
        ((ObjectNode) first.observations()).put("tampered", true);
        assertThat(new String(ReverseJson.write(second.observations()), java.nio.charset.StandardCharsets.UTF_8))
                .doesNotContain("tampered");
    }

    @Test
    void syntheticDatasetBindsCommitsPathsAndEpisodes() throws IOException {
        EvidenceChannelRecord record = loadValid();
        JsonNode observations = record.observations();

        // Source-order shuffles are normalized: commits arrive sorted by sha with sorted paths.
        JsonNode commitA = observations.get("commits").get(0);
        assertThat(commitA.get("commit_sha").asText()).isEqualTo(COMMIT_A);
        assertThat(commitA.get("changed_paths").size()).isZero();
        JsonNode commitB = observations.get("commits").get(1);
        assertThat(commitB.get("changed_paths").get(0).asText()).isEqualTo("src/A.java");
        assertThat(commitB.get("changed_paths").get(1).asText()).isEqualTo("src/B.java");

        assertThat(observations.get("changed_paths").size()).isEqualTo(2);
        assertThat(observations.get("changed_paths").get(0).get("commit_sha").asText()).isEqualTo(COMMIT_B);
        assertThat(observations.get("changed_paths").get(0).get("path").asText()).isEqualTo("src/A.java");

        JsonNode episode = observations.get("episodes").get(0);
        assertThat(episode.get("episode_id").asText()).isEqualTo("PR-7");
        assertThat(episode.get("title").asText()).isEqualTo("Add feature");
        assertThat(episode.get("included_commit_shas").get(0).asText()).isEqualTo(COMMIT_A);

        assertThat(observations.get("limitations").get(0).asText()).isEqualTo("a-limitation");
        assertThat(observations.get("limitations").get(1).asText()).isEqualTo("z-limitation");
    }

    @Test
    void missingInputFileFailsClosed() {
        assertFailure(ReverseFailure.MISSING_INPUT,
                () -> DeliveryHistoryInputAdapter.load(root, REPOSITORY, REVISION,
                        DeliveryHistoryInputAdapter.PINNED_INPUT_SHA256));
    }

    @Test
    void unreadableDirectoryFailsClosed() {
        assertFailure(ReverseFailure.MISSING_INPUT,
                () -> DeliveryHistoryInputAdapter.load(root.resolve("absent"), REPOSITORY, REVISION,
                        DeliveryHistoryInputAdapter.PINNED_INPUT_SHA256));
    }

    @Test
    void digestMismatchOnRecomputeFailsClosed() throws IOException {
        writeDataset(validDataset());
        String wrongDigest = "0".repeat(64);
        assertFailure(ReverseFailure.DIGEST_MISMATCH,
                () -> DeliveryHistoryInputAdapter.load(root, REPOSITORY, REVISION, wrongDigest));
    }

    @Test
    void malformedJsonFailsClosed() throws IOException {
        Path input = root.resolve(DeliveryHistoryInputAdapter.INPUT_PATH);
        Files.createDirectories(input.getParent());
        byte[] bytes = "{ not json".getBytes();
        Files.write(input, bytes);
        assertFailure(ReverseFailure.MISSING_INPUT,
                () -> DeliveryHistoryInputAdapter.load(root, REPOSITORY, REVISION, sha256(bytes)));
    }

    @Test
    void nonObjectJsonFailsClosed() throws IOException {
        byte[] bytes = "[1,2,3]".getBytes();
        writeDataset(JSON.readTree(bytes));
        assertFailure(ReverseFailure.MISSING_INPUT,
                () -> DeliveryHistoryInputAdapter.load(root, REPOSITORY, REVISION, sha256(bytes)));
    }

    @Test
    void unsupportedSchemaVersionFailsClosed() throws IOException {
        ObjectNode dataset = validDataset();
        dataset.put("dataset_id", "pkb001-delivery-history-v2");
        byte[] bytes = JSON.writeValueAsBytes(dataset);
        writeDataset(dataset);
        assertFailure(ReverseFailure.UNSUPPORTED_SCHEMA_VERSION,
                () -> DeliveryHistoryInputAdapter.load(root, REPOSITORY, REVISION, sha256(bytes)));
    }

    @Test
    void missingDatasetIdFailsClosed() throws IOException {
        ObjectNode dataset = validDataset();
        dataset.remove("dataset_id");
        byte[] bytes = JSON.writeValueAsBytes(dataset);
        writeDataset(dataset);
        assertFailure(ReverseFailure.MISSING_INPUT,
                () -> DeliveryHistoryInputAdapter.load(root, REPOSITORY, REVISION, sha256(bytes)));
    }

    @Test
    void unfrozenDatasetFailsClosed() throws IOException {
        ObjectNode dataset = validDataset();
        dataset.put("status", "DRAFT");
        byte[] bytes = JSON.writeValueAsBytes(dataset);
        writeDataset(dataset);
        assertFailure(ReverseFailure.MISSING_INPUT,
                () -> DeliveryHistoryInputAdapter.load(root, REPOSITORY, REVISION, sha256(bytes)));
    }

    @Test
    void historyRevisionDisagreementFailsClosed() throws IOException {
        ObjectNode dataset = validDataset();
        dataset.put("source_commit_sha", OTHER_REVISION);
        byte[] bytes = JSON.writeValueAsBytes(dataset);
        writeDataset(dataset);
        assertFailure(ReverseFailure.REVISION_MISMATCH,
                () -> DeliveryHistoryInputAdapter.load(root, REPOSITORY, REVISION, sha256(bytes)));
    }

    @Test
    void malformedHistoryRevisionFailsClosed() throws IOException {
        ObjectNode dataset = validDataset();
        dataset.put("source_commit_sha", "not-a-revision");
        byte[] bytes = JSON.writeValueAsBytes(dataset);
        writeDataset(dataset);
        assertFailure(ReverseFailure.REVISION_MISMATCH,
                () -> DeliveryHistoryInputAdapter.load(root, REPOSITORY, REVISION, sha256(bytes)));
    }

    @Test
    void malformedBoundRevisionFailsClosed() throws IOException {
        byte[] bytes = JSON.writeValueAsBytes(validDataset());
        writeDataset(validDataset());
        assertFailure(ReverseFailure.REVISION_MISMATCH,
                () -> DeliveryHistoryInputAdapter.load(root, REPOSITORY, "short", sha256(bytes)));
    }

    @Test
    void absoluteChangedPathFailsClosed() throws IOException {
        assertChangedPathFails("/etc/passwd");
    }

    @Test
    void traversalChangedPathFailsClosed() throws IOException {
        assertChangedPathFails("../outside.java");
    }

    @Test
    void backslashChangedPathFailsClosed() throws IOException {
        assertChangedPathFails("src\\evil.java");
    }

    private void assertChangedPathFails(String path) throws IOException {
        ObjectNode dataset = validDataset();
        ArrayNode changedPaths = (ArrayNode) dataset.get("commits").get(0).get("changed_paths");
        changedPaths.add(path);
        byte[] bytes = JSON.writeValueAsBytes(dataset);
        writeDataset(dataset);
        assertFailure(ReverseFailure.MALFORMED_PATH,
                () -> DeliveryHistoryInputAdapter.load(root, REPOSITORY, REVISION, sha256(bytes)));
    }

    @Test
    void duplicateCommitIdentityFailsClosed() throws IOException {
        ObjectNode dataset = validDataset();
        ArrayNode commits = (ArrayNode) dataset.get("commits");
        commits.add(commit(COMMIT_A, "2020-05-05T05:05:05Z", "duplicate", List.of()));
        byte[] bytes = JSON.writeValueAsBytes(dataset);
        writeDataset(dataset);
        assertFailure(ReverseFailure.DUPLICATE_IDENTITY,
                () -> DeliveryHistoryInputAdapter.load(root, REPOSITORY, REVISION, sha256(bytes)));
    }

    @Test
    void duplicateChangedPathIdentityFailsClosed() throws IOException {
        ObjectNode dataset = validDataset();
        ArrayNode changedPaths = (ArrayNode) dataset.get("commits").get(0).get("changed_paths");
        changedPaths.add("src/A.java");
        byte[] bytes = JSON.writeValueAsBytes(dataset);
        writeDataset(dataset);
        assertFailure(ReverseFailure.DUPLICATE_IDENTITY,
                () -> DeliveryHistoryInputAdapter.load(root, REPOSITORY, REVISION, sha256(bytes)));
    }

    @Test
    void duplicateEpisodeIdentityFailsClosed() throws IOException {
        ObjectNode dataset = validDataset();
        ArrayNode pullRequests = (ArrayNode) dataset.get("pull_requests");
        pullRequests.add(pullRequests.get(0).deepCopy());
        byte[] bytes = JSON.writeValueAsBytes(dataset);
        writeDataset(dataset);
        assertFailure(ReverseFailure.DUPLICATE_IDENTITY,
                () -> DeliveryHistoryInputAdapter.load(root, REPOSITORY, REVISION, sha256(bytes)));
    }

    @Test
    void episodeCitingAbsentCommitFailsClosed() throws IOException {
        ObjectNode dataset = validDataset();
        ArrayNode included = (ArrayNode) dataset.get("pull_requests").get(0).get("included_commit_shas");
        included.add("dddddddddddddddddddddddddddddddddddddddd");
        byte[] bytes = JSON.writeValueAsBytes(dataset);
        writeDataset(dataset);
        assertFailure(ReverseFailure.MISSING_INPUT,
                () -> DeliveryHistoryInputAdapter.load(root, REPOSITORY, REVISION, sha256(bytes)));
    }

    @Test
    void malformedTimestampFailsClosed() throws IOException {
        ObjectNode dataset = validDataset();
        ((ObjectNode) dataset.get("commits").get(0)).put("committed_at", "yesterday");
        byte[] bytes = JSON.writeValueAsBytes(dataset);
        writeDataset(dataset);
        assertFailure(ReverseFailure.MISSING_INPUT,
                () -> DeliveryHistoryInputAdapter.load(root, REPOSITORY, REVISION, sha256(bytes)));
    }
}

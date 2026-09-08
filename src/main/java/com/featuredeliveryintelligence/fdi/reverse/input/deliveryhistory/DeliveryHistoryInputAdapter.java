package com.featuredeliveryintelligence.fdi.reverse.input.deliveryhistory;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseContractValidation;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import com.featuredeliveryintelligence.fdi.reverse.ReverseJson;
import com.featuredeliveryintelligence.fdi.reverse.evidence.EvidenceChannelRecord;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceBundle;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceChannel;
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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Delivery-history input adapter (PKB-BL-009 Slice D). Loads the frozen Java
 * delivery-history reconstruction and binds its commits, changed paths, and
 * delivery episodes (merged pull requests) as normalized
 * {@link ReverseEvidenceChannel#DELIVERY_HISTORY} observations of the Slice A
 * reverse-evidence contract.
 *
 * <p>Commit subjects and pull-request titles are historical delivery
 * evidence, never Product truth: this adapter copies them verbatim into
 * traceable text fields and never interprets, classifies, or rewrites them
 * into semantic claims.
 *
 * <p>Determinism: observations are emitted with fixed ordering — commits by
 * commit sha, episodes by pull-request number, changed paths by commit sha
 * then path, included commits by sha, limitations alphabetically — and
 * canonicalized through {@link ReverseJson} so repeated loads of identical
 * bytes produce byte-identical documents. No wall-clock timestamps, locale
 * dependence, HashMap iteration-order exposure, or randomness is used.
 *
 * <p>Fail-closed, with exactly one stable {@link ReverseFailure} code per
 * refusal: a missing or unreadable input file or malformed JSON document is
 * {@link ReverseFailure#MISSING_INPUT}; a digest that fails recomputation is
 * {@link ReverseFailure#DIGEST_MISMATCH}; a malformed or disagreeing history
 * revision is {@link ReverseFailure#REVISION_MISMATCH}; an absolute,
 * escaped, backslash, or traversal changed path is
 * {@link ReverseFailure#MALFORMED_PATH}; duplicate commit, episode, or
 * changed-path identities (and episodes citing a commit absent from the
 * bound history) are {@link ReverseFailure#DUPLICATE_IDENTITY} /
 * {@link ReverseFailure#MISSING_INPUT}; an unsupported dataset schema
 * version is {@link ReverseFailure#UNSUPPORTED_SCHEMA_VERSION}.
 */
public final class DeliveryHistoryInputAdapter {

    /** Repository-relative location of the frozen delivery-history reconstruction. */
    public static final String INPUT_PATH = "validation/pkb001/datasets/petclinic-delivery-history.json";

    /** Frozen SHA-256 of {@link #INPUT_PATH} at acquisition time (verified before use). */
    public static final String PINNED_INPUT_SHA256 =
            "87b70439f69bb82cee0139b65108ecce6044aff1a4d1b5ce6324933acbeab2d1";

    /** Dataset identity this contract revision supports; the {@code -v1} suffix is the dataset schema version. */
    public static final String DATASET_ID = "pkb001-delivery-history-v1";

    /** Dataset schema version derived from {@link #DATASET_ID}. */
    public static final String DATASET_SCHEMA_VERSION = "1";

    /** Provider provenance recorded on the bound channel. */
    public static final String PROVIDER_ID = "fdi-delivery-history-reconstruction";

    /** Provenance narrative recorded on the bound channel. */
    public static final String PROVENANCE =
            "Java delivery-history reconstruction; Git/PR evidence bounded by history cutoff; "
                    + "historical evidence, not Product truth";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final DateTimeFormatter ISO_TIMESTAMP = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private DeliveryHistoryInputAdapter() { }

    /**
     * Loads the delivery-history reconstruction under the given repository
     * root, verifies its SHA-256 against {@code expectedInputSha256} before
     * parsing, validates the dataset fail-closed, and returns the bound
     * DELIVERY_HISTORY channel record. Use {@link #PINNED_INPUT_SHA256} for
     * the frozen Petclinic reconstruction.
     */
    public static EvidenceChannelRecord load(
            Path repositoryRoot, String repositoryId, String canonicalRevision, String expectedInputSha256) {
        if (repositoryRoot == null || !Files.isDirectory(repositoryRoot))
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "repository root must be a readable directory");
        String boundRepository = ReverseContractValidation.requiredText(repositoryId, "bundle repository id");
        String boundRevision = ReverseContractValidation.canonicalRevision(canonicalRevision);
        String boundDigest = ReverseContractValidation.sha256Hex(expectedInputSha256, "delivery-history input");
        Path root = repositoryRoot.toAbsolutePath().normalize();
        Path input = root.resolve(INPUT_PATH).normalize();
        if (!input.startsWith(root) || !Files.isRegularFile(input))
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "delivery-history input is not a readable file: " + INPUT_PATH);
        String recomputed = sha256(input);
        if (!recomputed.equals(boundDigest))
            throw new ReverseContractException(
                    ReverseFailure.DIGEST_MISMATCH,
                    "delivery-history digest mismatch for " + INPUT_PATH
                            + ": bound " + boundDigest + " but recomputed " + recomputed);
        JsonNode dataset = parse(input);
        ObjectNode observations = bindObservations(dataset, boundRevision);
        return new EvidenceChannelRecord(
                ReverseEvidenceChannel.DELIVERY_HISTORY,
                boundRepository,
                boundRevision,
                INPUT_PATH,
                boundDigest,
                ReverseEvidenceBundle.SUPPORTED_SCHEMA_VERSION,
                PROVIDER_ID,
                PROVENANCE,
                ReverseJson.canonicalize(observations));
    }

    private static JsonNode parse(Path input) {
        try {
            JsonNode dataset = JSON.readTree(Files.readAllBytes(input));
            if (dataset == null || !dataset.isObject())
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT, "delivery-history input must be a JSON object: " + INPUT_PATH);
            return dataset;
        } catch (IOException failure) {
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "delivery-history input is malformed JSON: " + INPUT_PATH);
        }
    }

    private static ObjectNode bindObservations(JsonNode dataset, String boundRevision) {
        String datasetId = text(dataset.get("dataset_id"), "dataset_id");
        if (!DATASET_ID.equals(datasetId))
            throw new ReverseContractException(
                    ReverseFailure.UNSUPPORTED_SCHEMA_VERSION,
                    "unsupported delivery-history dataset or schema version: " + datasetId
                            + " (supported: " + DATASET_ID + ")");
        String status = text(dataset.get("status"), "status");
        if (!"FROZEN".equals(status))
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "delivery-history dataset must be FROZEN but was: " + status);
        String sourceRevision = ReverseContractValidation.canonicalRevision(
                text(dataset.get("source_commit_sha"), "source_commit_sha"));
        if (!sourceRevision.equals(boundRevision))
            throw new ReverseContractException(
                    ReverseFailure.REVISION_MISMATCH,
                    "delivery-history source revision " + sourceRevision
                            + " disagrees with the bound target revision " + boundRevision);
        String historyCutoff = timestamp(dataset.get("history_cutoff"), "history_cutoff");
        String policy = text(dataset.get("post_cutoff_knowledge_policy"), "post_cutoff_knowledge_policy");
        String boundary = text(dataset.get("evidence_boundary"), "evidence_boundary");

        List<JsonNode> commits = elements(dataset.get("commits"), "commits");
        List<CommitObservation> commitObservations = new ArrayList<>();
        Set<String> seenCommits = new HashSet<>();
        for (JsonNode commit : commits) {
            String sha = ReverseContractValidation.canonicalRevision(
                    text(commit.get("commit_sha"), "commits[].commit_sha"));
            if (!seenCommits.add(sha))
                throw new ReverseContractException(
                        ReverseFailure.DUPLICATE_IDENTITY, "duplicate commit identity: " + sha);
            String committedAt = timestamp(commit.get("committed_at"), "commits[].committed_at");
            String subject = text(commit.get("subject"), "commits[].subject");
            List<String> changedPaths = new ArrayList<>();
            Set<String> seenPaths = new HashSet<>();
            for (JsonNode pathNode : elements(commit.get("changed_paths"), "commits[].changed_paths")) {
                String path = ReverseContractValidation.repositoryPath(
                        text(pathNode, "commits[].changed_paths[]"), "delivery-history changed path");
                if (!seenPaths.add(path))
                    throw new ReverseContractException(
                            ReverseFailure.DUPLICATE_IDENTITY,
                            "duplicate changed-path identity in commit " + sha + ": " + path);
                changedPaths.add(path);
            }
            changedPaths.sort(Comparator.naturalOrder());
            commitObservations.add(new CommitObservation(sha, committedAt, subject, changedPaths));
        }
        commitObservations.sort(Comparator.comparing(CommitObservation::commitSha));

        List<JsonNode> episodes = elements(dataset.get("pull_requests"), "pull_requests");
        List<EpisodeObservation> episodeObservations = new ArrayList<>();
        Set<Integer> seenEpisodes = new HashSet<>();
        for (JsonNode episode : episodes) {
            JsonNode numberNode = episode.get("number");
            if (numberNode == null || !numberNode.canConvertToInt() || !numberNode.isIntegralNumber())
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT, "pull_requests[].number must be an integer");
            int number = numberNode.asInt();
            if (number < 0 || !seenEpisodes.add(number))
                throw new ReverseContractException(
                        ReverseFailure.DUPLICATE_IDENTITY, "duplicate delivery-episode identity: PR-" + number);
            String state = text(episode.get("state"), "pull_requests[].state");
            String title = text(episode.get("title"), "pull_requests[].title");
            String url = text(episode.get("url"), "pull_requests[].url");
            String createdAt = timestamp(episode.get("created_at"), "pull_requests[].created_at");
            String updatedAt = timestamp(episode.get("updated_at"), "pull_requests[].updated_at");
            String headRefOid = ReverseContractValidation.canonicalRevision(
                    text(episode.get("head_ref_oid"), "pull_requests[].head_ref_oid"));
            String mergeCommitSha = ReverseContractValidation.canonicalRevision(
                    text(episode.get("merge_commit_sha"), "pull_requests[].merge_commit_sha"));
            List<String> included = new ArrayList<>();
            for (JsonNode shaNode : elements(episode.get("included_commit_shas"), "pull_requests[].included_commit_shas")) {
                String sha = ReverseContractValidation.canonicalRevision(
                        text(shaNode, "pull_requests[].included_commit_shas[]"));
                if (!seenCommits.contains(sha))
                    throw new ReverseContractException(
                            ReverseFailure.MISSING_INPUT,
                            "episode PR-" + number + " cites commit absent from the bound history: " + sha);
                included.add(sha);
            }
            included.sort(Comparator.naturalOrder());
            episodeObservations.add(new EpisodeObservation(
                    number, state, title, url, createdAt, updatedAt, headRefOid, mergeCommitSha, included));
        }
        episodeObservations.sort(Comparator.comparingInt(EpisodeObservation::number));

        ObjectNode observations = JSON.createObjectNode();
        observations.put("schema_version", DATASET_SCHEMA_VERSION);
        observations.put("dataset_id", DATASET_ID);
        observations.put("source_commit_sha", sourceRevision);
        observations.put("history_cutoff", historyCutoff);
        observations.put("post_cutoff_knowledge_policy", policy);
        observations.put("evidence_boundary", boundary);
        ArrayNode limitations = observations.putArray("limitations");
        List<String> sortedLimitations = new ArrayList<>();
        for (JsonNode limitation : elements(dataset.get("limitations"), "limitations"))
            sortedLimitations.add(text(limitation, "limitations[]"));
        sortedLimitations.sort(Comparator.naturalOrder());
        sortedLimitations.forEach(limitations::add);

        ArrayNode commitNodes = observations.putArray("commits");
        ArrayNode changedPathNodes = observations.putArray("changed_paths");
        for (CommitObservation commit : commitObservations) {
            ObjectNode node = commitNodes.addObject();
            node.put("commit_sha", commit.commitSha());
            node.put("committed_at", commit.committedAt());
            node.put("subject", commit.subject());
            ArrayNode paths = node.putArray("changed_paths");
            commit.changedPaths().forEach(paths::add);
            for (String path : commit.changedPaths()) {
                ObjectNode pathObservation = changedPathNodes.addObject();
                pathObservation.put("commit_sha", commit.commitSha());
                pathObservation.put("path", path);
            }
        }

        ArrayNode episodeNodes = observations.putArray("episodes");
        for (EpisodeObservation episode : episodeObservations) {
            ObjectNode node = episodeNodes.addObject();
            node.put("episode_id", "PR-" + episode.number());
            node.put("number", episode.number());
            node.put("state", episode.state());
            node.put("title", episode.title());
            node.put("url", episode.url());
            node.put("created_at", episode.createdAt());
            node.put("updated_at", episode.updatedAt());
            node.put("head_ref_oid", episode.headRefOid());
            node.put("merge_commit_sha", episode.mergeCommitSha());
            ArrayNode included = node.putArray("included_commit_shas");
            episode.includedCommitShas().forEach(included::add);
        }
        return observations;
    }

    private static List<JsonNode> elements(JsonNode node, String what) {
        if (node == null || !node.isArray())
            throw new ReverseContractException(ReverseFailure.MISSING_INPUT, what + " must be an array");
        List<JsonNode> elements = new ArrayList<>();
        node.forEach(elements::add);
        return elements;
    }

    private static String text(JsonNode node, String what) {
        if (node == null || !node.isTextual() || node.asText().isBlank())
            throw new ReverseContractException(ReverseFailure.MISSING_INPUT, what + " must not be blank");
        return node.asText();
    }

    private static String timestamp(JsonNode node, String what) {
        String value = text(node, what);
        try {
            OffsetDateTime.parse(value, ISO_TIMESTAMP);
        } catch (DateTimeParseException failure) {
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, what + " must be an ISO-8601 timestamp: " + value);
        }
        return value;
    }

    private static String sha256(Path file) {
        try (InputStream stream = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) digest.update(buffer, 0, read);
            StringBuilder hex = new StringBuilder(64);
            for (byte value : digest.digest()) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (IOException failure) {
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "delivery-history input is unreadable: " + file);
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is not available", failure);
        }
    }

    private record CommitObservation(
            String commitSha, String committedAt, String subject, List<String> changedPaths) { }

    private record EpisodeObservation(
            int number,
            String state,
            String title,
            String url,
            String createdAt,
            String updatedAt,
            String headRefOid,
            String mergeCommitSha,
            List<String> includedCommitShas) { }
}

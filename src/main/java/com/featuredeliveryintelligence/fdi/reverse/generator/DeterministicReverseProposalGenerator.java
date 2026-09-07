package com.featuredeliveryintelligence.fdi.reverse.generator;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import com.featuredeliveryintelligence.fdi.reverse.evidence.EvidenceChannelRecord;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceBundle;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceChannel;
import com.featuredeliveryintelligence.fdi.reverse.proposal.CapabilityProposal;
import com.featuredeliveryintelligence.fdi.reverse.proposal.EvidenceCitation;
import com.featuredeliveryintelligence.fdi.reverse.proposal.EvidenceGap;
import com.featuredeliveryintelligence.fdi.reverse.proposal.ProposalAuthority;
import com.featuredeliveryintelligence.fdi.reverse.proposal.ReverseProposalPackage;
import com.featuredeliveryintelligence.fdi.reverse.proposal.ReverseProposalProvider;
import com.featuredeliveryintelligence.fdi.reverse.proposal.ScenarioProposal;
import com.featuredeliveryintelligence.fdi.reverse.proposal.ScenarioStatus;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Deterministic Slice E proposal generator (PKB-BL-009 / PKB-REVERSE-002).
 * Consumes only the validated {@link ReverseEvidenceBundle} produced by the
 * Slice B/C/D input adapters and groups corroborated observations into
 * Capability and Behavior Scenario proposals; every observation that cannot
 * become a proposal is recorded as an explicit {@link EvidenceGap}.
 *
 * <p>Mechanical grouping rule: the repository-relative source path is the only
 * join key across channels. A path observed by at least two distinct channels
 * is corroborated. Corroborated test-behavior paths produce one
 * {@link CapabilityProposal} (wording derived mechanically from the recorded
 * test file stem) plus one {@link ScenarioProposal} per recorded test method
 * (wording derived mechanically from the recorded method name). Corroborated
 * paths without test-behavior observations, and every single-channel
 * observation, become explicit gaps. Delivery commit and episode observations
 * are delivery-channel aggregates and always remain gaps. The partition
 * invariant is exact: every normalized observation of the three channels is
 * either cited by a proposal or recorded as one gap, never both, never
 * neither.
 *
 * <p>Determinism: iteration uses sorted maps and lists only, identifiers are
 * assigned after sorting, no wall-clock, locale, HashMap-order, or randomness
 * influences the output; repeated generation over an identical bundle yields
 * an identical package and byte-identical serialization through
 * {@link com.featuredeliveryintelligence.fdi.reverse.ReverseJson}.
 *
 * <p>Isolation and authority: this generator reads only the bundle it is
 * given; it never touches accepted Product Semantics, evaluator gold, review
 * decisions, comparison output, or previous proposal outcomes, and every
 * emitted proposal is {@link ProposalAuthority#PROPOSAL_ONLY},
 * {@code semanticPublicationAllowed == false}, with scenarios
 * {@link ScenarioStatus#UNREVIEWED}.
 */
public final class DeterministicReverseProposalGenerator implements ReverseProposalProvider {

    /** Confidence base for a minimally corroborated (two-channel) proposal. */
    static final long BASE_CONFIDENCE = 5000L;
    /** Capability confidence increment per channel beyond the required two. */
    static final long CAPABILITY_CHANNEL_BONUS = 1500L;
    /** Scenario confidence increment per channel beyond the required two. */
    static final long SCENARIO_CHANNEL_BONUS = 1000L;

    static final String GIVEN_TEXT = "a recorded test exercises the system";
    static final String THEN_TEXT = "the recorded expectations hold";

    private static final String REASON_UNCORROBORATED =
            "observation is corroborated by no other evidence channel";
    private static final String REASON_NO_BEHAVIOR_SOURCE =
            "corroborated path has no test-behavior observations from which observable wording can be derived";
    private static final String REASON_NO_WORDING =
            "no observable wording could be derived mechanically from the recorded test method name";
    private static final String REASON_COMMIT_AGGREGATE =
            "commit observation is a delivery-channel aggregate and cannot be promoted without path corroboration";
    private static final String REASON_EPISODE_AGGREGATE =
            "episode observation is a delivery-channel aggregate and cannot be promoted without path corroboration";

    @Override
    public ReverseProposalPackage propose(ReverseEvidenceBundle evidence) {
        if (evidence == null)
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "evidence bundle must not be null");

        ChannelIndex structural = structuralIndex(evidence.channel(ReverseEvidenceChannel.STRUCTURAL));
        ChannelIndex testBehavior = testBehaviorIndex(evidence.channel(ReverseEvidenceChannel.TEST_BEHAVIOR));
        ChannelIndex delivery = deliveryIndex(evidence.channel(ReverseEvidenceChannel.DELIVERY_HISTORY));

        Set<String> paths = new TreeSet<>();
        paths.addAll(structural.paths());
        paths.addAll(testBehavior.paths());
        paths.addAll(delivery.paths());

        List<CapabilityProposal> capabilities = new ArrayList<>();
        List<ScenarioProposal> scenarios = new ArrayList<>();
        List<EvidenceGap> gaps = new ArrayList<>();

        int capabilitySequence = 0;
        int scenarioSequence = 0;
        for (String path : paths) {
            Set<ReverseEvidenceChannel> channels = channelsOf(structural, testBehavior, delivery, path);
            if (channels.size() < 2) {
                gapPathObservations(gaps, structural, testBehavior, delivery, path, REASON_UNCORROBORATED);
                continue;
            }
            if (!testBehavior.paths().contains(path)) {
                gapPathObservations(gaps, structural, testBehavior, delivery, path, REASON_NO_BEHAVIOR_SOURCE);
                continue;
            }
            String digest = testBehavior.digest();
            List<String> words = wordingWords(stripTestSuffix(fileStem(path)));
            if (words.isEmpty()) {
                gapPathObservations(gaps, structural, testBehavior, delivery, path, REASON_NO_BEHAVIOR_SOURCE);
                continue;
            }
            capabilitySequence++;
            String capabilityId = "HYP-CAPABILITY-" + sequence(capabilitySequence);
            List<EvidenceCitation> pathCitations = pathCitations(structural, testBehavior, delivery, path);
            int channelCount = channels.size();
            capabilities.add(new CapabilityProposal(
                    capabilityId,
                    capitalize(join(words)),
                    pathCitations,
                    evidence.canonicalRevision(),
                    contributingDigests(pathCitations),
                    "mechanically grouped from " + pathCitations.size() + " observations across "
                            + channelCount + " evidence channels for one corroborated repository path",
                    limitations(),
                    BASE_CONFIDENCE + CAPABILITY_CHANNEL_BONUS * (channelCount - 2),
                    ProposalAuthority.PROPOSAL_ONLY,
                    false));

            for (TestMethodRef method : testBehavior.methods(path)) {
                List<String> methodWords = wordingWords(method.methodName());
                if (methodWords.isEmpty()) {
                    gaps.add(new EvidenceGap(
                            ReverseEvidenceChannel.TEST_BEHAVIOR,
                            method.ref(),
                            digest,
                            REASON_NO_WORDING));
                    continue;
                }
                scenarioSequence++;
                List<EvidenceCitation> scenarioCitations = new ArrayList<>(pathCitations);
                scenarioCitations.add(new EvidenceCitation(
                        ReverseEvidenceChannel.TEST_BEHAVIOR, method.ref(), digest));
                scenarioCitations.sort(EvidenceCitation.stableOrder());
                scenarios.add(new ScenarioProposal(
                        "HYP-SCENARIO-" + sequence(scenarioSequence),
                        capabilityId,
                        capitalize(join(methodWords)),
                        GIVEN_TEXT,
                        join(methodWords),
                        THEN_TEXT,
                        scenarioCitations,
                        evidence.canonicalRevision(),
                        contributingDigests(scenarioCitations),
                        "mechanically derived from the recorded test method name and its corroborated path observations",
                        limitations(),
                        BASE_CONFIDENCE + SCENARIO_CHANNEL_BONUS * (channelCount - 2),
                        ScenarioStatus.UNREVIEWED,
                        ProposalAuthority.PROPOSAL_ONLY,
                        false));
            }
        }

        for (int commit = 0; commit < delivery.commitCount(); commit++) {
            gaps.add(new EvidenceGap(
                    ReverseEvidenceChannel.DELIVERY_HISTORY,
                    "/commits/" + commit,
                    delivery.digest(),
                    REASON_COMMIT_AGGREGATE));
        }
        for (int episode = 0; episode < delivery.episodeCount(); episode++) {
            gaps.add(new EvidenceGap(
                    ReverseEvidenceChannel.DELIVERY_HISTORY,
                    "/episodes/" + episode,
                    delivery.digest(),
                    REASON_EPISODE_AGGREGATE));
        }

        return new ReverseProposalPackage(
                evidence.repositoryId(), evidence.canonicalRevision(), capabilities, scenarios, gaps);
    }

    // ------------------------------------------------------------------
    // Observation indexing (fail-closed on malformed payloads)
    // ------------------------------------------------------------------

    /** Per-channel observation index keyed by repository-relative path. */
    static final class ChannelIndex {
        private final String digest;
        private final TreeMap<String, List<EvidenceObservation>> byPath = new TreeMap<>();
        private final TreeMap<String, List<TestMethodRef>> methods = new TreeMap<>();
        private int commitCount;
        private int episodeCount;

        ChannelIndex(String digest) {
            this.digest = digest;
        }

        String digest() { return digest; }

        Set<String> paths() { return byPath.keySet(); }

        List<EvidenceObservation> observations(String path) { return byPath.getOrDefault(path, List.of()); }

        List<TestMethodRef> methods(String path) {
            return methods.getOrDefault(path, List.of());
        }

        int commitCount() { return commitCount; }

        int episodeCount() { return episodeCount; }
    }

    /** One citable observation: stable JSON pointer plus owning path. */
    record EvidenceObservation(String path, String ref) { }

    /** One recorded test method: stable JSON pointer plus recorded name. */
    record TestMethodRef(String ref, String methodName) { }

    private static ChannelIndex structuralIndex(EvidenceChannelRecord record) {
        ChannelIndex index = new ChannelIndex(record.inputSha256());
        JsonNode nodes = record.observations().path("nodes");
        if (!nodes.isArray())
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "structural observations must contain a nodes array");
        for (int i = 0; i < nodes.size(); i++) {
            JsonNode node = nodes.get(i);
            JsonNode path = node.path("source_file");
            if (!path.isTextual() || path.asText().isBlank())
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT,
                        "structural node at /nodes/" + i + " must record a source_file");
            index.byPath
                    .computeIfAbsent(path.asText(), key -> new ArrayList<>())
                    .add(new EvidenceObservation(path.asText(), "/nodes/" + i));
        }
        return index;
    }

    private static ChannelIndex testBehaviorIndex(EvidenceChannelRecord record) {
        ChannelIndex index = new ChannelIndex(record.inputSha256());
        JsonNode files = record.observations().path("test_files");
        if (!files.isArray())
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "test-behavior observations must contain a test_files array");
        for (int file = 0; file < files.size(); file++) {
            JsonNode document = files.get(file);
            JsonNode path = document.path("repository_relative_path");
            if (!path.isTextual() || path.asText().isBlank())
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT,
                        "test file at /test_files/" + file + " must record a repository_relative_path");
            index.byPath
                    .computeIfAbsent(path.asText(), key -> new ArrayList<>())
                    .add(new EvidenceObservation(path.asText(), "/test_files/" + file));
            JsonNode testMethods = document.path("test_methods");
            if (!testMethods.isArray())
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT,
                        "test file at /test_files/" + file + " must record a test_methods array");
            List<TestMethodRef> refs = new ArrayList<>();
            for (int method = 0; method < testMethods.size(); method++) {
                JsonNode name = testMethods.get(method).path("method_name");
                if (!name.isTextual() || name.asText().isBlank())
                    throw new ReverseContractException(
                            ReverseFailure.MISSING_INPUT,
                            "test method at /test_files/" + file + "/test_methods/" + method
                                    + " must record a method_name");
                refs.add(new TestMethodRef(
                        "/test_files/" + file + "/test_methods/" + method, name.asText()));
            }
            index.methods.put(path.asText(), refs);
        }
        return index;
    }

    private static ChannelIndex deliveryIndex(EvidenceChannelRecord record) {
        ChannelIndex index = new ChannelIndex(record.inputSha256());
        JsonNode changedPaths = record.observations().path("changed_paths");
        if (!changedPaths.isArray())
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "delivery observations must contain a changed_paths array");
        for (int i = 0; i < changedPaths.size(); i++) {
            JsonNode path = changedPaths.get(i).path("path");
            if (!path.isTextual() || path.asText().isBlank())
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT,
                        "changed path at /changed_paths/" + i + " must record a path");
            index.byPath
                    .computeIfAbsent(path.asText(), key -> new ArrayList<>())
                    .add(new EvidenceObservation(path.asText(), "/changed_paths/" + i));
        }
        JsonNode commits = record.observations().path("commits");
        JsonNode episodes = record.observations().path("episodes");
        if (!commits.isArray() || !episodes.isArray())
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT,
                    "delivery observations must contain commits and episodes arrays");
        index.commitCount = commits.size();
        index.episodeCount = episodes.size();
        return index;
    }

    // ------------------------------------------------------------------
    // Grouping helpers
    // ------------------------------------------------------------------

    private static Set<ReverseEvidenceChannel> channelsOf(
            ChannelIndex structural, ChannelIndex testBehavior, ChannelIndex delivery, String path) {
        Set<ReverseEvidenceChannel> channels = EnumSet.noneOf(ReverseEvidenceChannel.class);
        if (structural.paths().contains(path)) channels.add(ReverseEvidenceChannel.STRUCTURAL);
        if (testBehavior.paths().contains(path)) channels.add(ReverseEvidenceChannel.TEST_BEHAVIOR);
        if (delivery.paths().contains(path)) channels.add(ReverseEvidenceChannel.DELIVERY_HISTORY);
        return channels;
    }

    private static void gapPathObservations(
            List<EvidenceGap> gaps,
            ChannelIndex structural,
            ChannelIndex testBehavior,
            ChannelIndex delivery,
            String path,
            String reason) {
        for (EvidenceObservation observation : structural.observations(path))
            gaps.add(new EvidenceGap(
                    ReverseEvidenceChannel.STRUCTURAL, observation.ref(), structural.digest(), reason));
        List<EvidenceObservation> testObservations = testBehavior.observations(path);
        if (testObservations != null) {
            for (EvidenceObservation observation : testObservations)
                gaps.add(new EvidenceGap(
                        ReverseEvidenceChannel.TEST_BEHAVIOR, observation.ref(), testBehavior.digest(), reason));
            for (TestMethodRef method : testBehavior.methods(path))
                gaps.add(new EvidenceGap(
                        ReverseEvidenceChannel.TEST_BEHAVIOR, method.ref(), testBehavior.digest(), reason));
        }
        for (EvidenceObservation observation : delivery.observations(path))
            gaps.add(new EvidenceGap(
                    ReverseEvidenceChannel.DELIVERY_HISTORY, observation.ref(), delivery.digest(), reason));
    }

    private static List<EvidenceCitation> pathCitations(
            ChannelIndex structural, ChannelIndex testBehavior, ChannelIndex delivery, String path) {
        List<EvidenceCitation> citations = new ArrayList<>();
        for (EvidenceObservation observation : structural.observations(path))
            citations.add(new EvidenceCitation(
                    ReverseEvidenceChannel.STRUCTURAL, observation.ref(), structural.digest()));
        for (EvidenceObservation observation : testBehavior.observations(path))
            citations.add(new EvidenceCitation(
                    ReverseEvidenceChannel.TEST_BEHAVIOR, observation.ref(), testBehavior.digest()));
        for (EvidenceObservation observation : delivery.observations(path))
            citations.add(new EvidenceCitation(
                    ReverseEvidenceChannel.DELIVERY_HISTORY, observation.ref(), delivery.digest()));
        citations.sort(EvidenceCitation.stableOrder());
        return citations;
    }

    private static List<String> contributingDigests(List<EvidenceCitation> citations) {
        Set<String> digests = new TreeSet<>();
        for (EvidenceCitation citation : citations) digests.add(citation.evidenceDigest());
        return List.copyOf(digests);
    }

    private static List<String> limitations() {
        return List.of(
                "reverse reconstruction consistency, not independent product validation",
                "wording is mechanically derived from recorded test names, not product semantics",
                "proposal only; awaits human reviewer review");
    }

    // ------------------------------------------------------------------
    // Mechanical wording derivation
    // ------------------------------------------------------------------

    /** File stem of a repository-relative path, without directory or extension. */
    static String fileStem(String path) {
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /** Drops one trailing JUnit-style {@code Tests} or {@code Test} suffix from a recorded file stem. */
    static String stripTestSuffix(String stem) {
        if (stem.endsWith("Tests") && stem.length() > "Tests".length()) return stem.substring(0, stem.length() - 5);
        if (stem.endsWith("Test") && stem.length() > "Test".length()) return stem.substring(0, stem.length() - 4);
        return stem;
    }

    /**
     * Splits one recorded Java identifier (test file stem or test method name)
     * into lowercase wording tokens: a leading {@code test} or {@code should}
     * prefix is dropped, then camelCase boundaries, underscores, hyphens,
     * whitespace, and letter/digit boundaries split the remainder. The rule is
     * purely mechanical and conservative; an empty result means no observable
     * wording could be derived.
     */
    static List<String> wordingWords(String identifier) {
        String text = identifier == null ? "" : identifier.strip();
        if (text.startsWith("test")) text = text.substring(4);
        else if (text.startsWith("should")) text = text.substring(6);
        String[] tokens = text.split(
                "(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Za-z])(?=[0-9])|(?<=[0-9])(?=[A-Za-z])|[^A-Za-z0-9]+");
        List<String> words = new ArrayList<>();
        for (String token : tokens) {
            if (!token.isBlank()) words.add(token.toLowerCase(Locale.ROOT));
        }
        return words;
    }

    private static String join(List<String> words) {
        return String.join(" ", words);
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String sequence(int value) {
        return String.format(Locale.ROOT, "%04d", value);
    }
}

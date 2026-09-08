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
import com.featuredeliveryintelligence.fdi.reverse.proposal.ScenarioProposal;
import com.featuredeliveryintelligence.fdi.reverse.proposal.ScenarioStatus;
import com.featuredeliveryintelligence.fdi.reverse.proposal.ScenarioTextValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests for the Slice E deterministic proposal generator over small
 * synthetic evidence bundles: corroboration grouping, gap partition, wording
 * derivation, authority flags, confidence, determinism, and fail-closed
 * payload validation.
 */
class DeterministicReverseProposalGeneratorTests {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String REPOSITORY = "synthetic-repo";
    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String STRUCTURAL_DIGEST = "1".repeat(64);
    private static final String TEST_DIGEST = "2".repeat(64);
    private static final String DELIVERY_DIGEST = "3".repeat(64);

    private static final String TEST_PATH = "src/test/java/x/OwnerControllerTests.java";
    private static final String MAIN_PATH = "src/main/java/x/OwnerController.java";
    private static final String ALONE_PATH = "src/main/java/x/Alone.java";
    private static final String README_PATH = "docs/README.md";

    private final DeterministicReverseProposalGenerator generator = new DeterministicReverseProposalGenerator();

    // ------------------------------------------------------------------
    // Synthetic bundle builders
    // ------------------------------------------------------------------

    private static EvidenceChannelRecord structuralChannel(JsonNode... nodes) {
        ObjectNode observations = JSON.createObjectNode();
        observations.set("nodes", JSON.valueToTree(List.of(nodes)));
        return channel(ReverseEvidenceChannel.STRUCTURAL, STRUCTURAL_DIGEST, observations);
    }

    private static JsonNode structuralNode(String sourceFile) {
        ObjectNode node = JSON.createObjectNode();
        node.put("provider_node_id", "node-" + sourceFile);
        node.put("label", "x.Label");
        node.put("kind", "TYPE");
        node.put("source_file", sourceFile);
        node.put("source_location", "1:1");
        return node;
    }

    private static EvidenceChannelRecord testBehaviorChannel(String path, String... methodNames) {
        return testBehaviorChannel(List.of(path), List.of(methodNames), List.of());
    }

    private static EvidenceChannelRecord testBehaviorChannel(
            List<String> paths, List<String> firstMethods, List<String> secondMethods) {
        ObjectNode observations = JSON.createObjectNode();
        var files = observations.putArray("test_files");
        for (int i = 0; i < paths.size(); i++) {
            ObjectNode file = files.addObject();
            file.put("repository_relative_path", paths.get(i));
            var methods = file.putArray("test_methods");
            List<String> names = i == 0 ? firstMethods : secondMethods;
            for (String methodName : names) {
                ObjectNode method = methods.addObject();
                method.put("method_name", methodName);
            }
        }
        return channel(ReverseEvidenceChannel.TEST_BEHAVIOR, TEST_DIGEST, observations);
    }

    private static EvidenceChannelRecord deliveryChannel(String... changedPaths) {
        ObjectNode observations = JSON.createObjectNode();
        var changed = observations.putArray("changed_paths");
        for (String path : changedPaths) {
            ObjectNode entry = changed.addObject();
            entry.put("commit_sha", REVISION);
            entry.put("path", path);
        }
        var commits = observations.putArray("commits");
        commits.addObject().put("commit_sha", REVISION);
        commits.addObject().put("commit_sha", REVISION);
        var episodes = observations.putArray("episodes");
        episodes.addObject().put("episode_id", "PR-1");
        return channel(ReverseEvidenceChannel.DELIVERY_HISTORY, DELIVERY_DIGEST, observations);
    }

    private static EvidenceChannelRecord channel(
            ReverseEvidenceChannel channel, String digest, ObjectNode observations) {
        return new EvidenceChannelRecord(
                channel, REPOSITORY, REVISION,
                "validation/" + channel.name().toLowerCase() + ".json",
                digest, "1", "synthetic-provider", "synthetic provenance", observations);
    }

    private static ReverseEvidenceBundle bundle(EvidenceChannelRecord... channels) {
        return new ReverseEvidenceBundle(REPOSITORY, REVISION, "1", List.of(channels));
    }

    /** The standard synthetic bundle: one two-channel test path, one corroborated main path without tests. */
    private static ReverseEvidenceBundle standardBundle() {
        return bundle(
                structuralChannel(
                        structuralNode(TEST_PATH),
                        structuralNode(MAIN_PATH),
                        structuralNode(ALONE_PATH)),
                testBehaviorChannel(TEST_PATH, "shouldFindOwnersByLastName", "test"),
                deliveryChannel(MAIN_PATH, README_PATH));
    }

    // ------------------------------------------------------------------
    // Grouping and wording
    // ------------------------------------------------------------------

    @Test
    void corroboratedTestPathBecomesCapabilityWithScenarios() {
        ReverseProposalPackage proposalPackage = generator.propose(standardBundle());

        assertEquals(1, proposalPackage.capabilities().size());
        CapabilityProposal capability = proposalPackage.capabilities().get(0);
        assertEquals("HYP-CAPABILITY-0001", capability.id());
        assertEquals("Owner controller", capability.title());
        assertEquals(REVISION, capability.sourceRevision());
        assertEquals(ProposalAuthority.PROPOSAL_ONLY, capability.authority());
        assertFalse(capability.semanticPublicationAllowed());
        assertEquals(5000L, capability.confidenceScale4());

        assertEquals(1, proposalPackage.scenarios().size());
        ScenarioProposal scenario = proposalPackage.scenarios().get(0);
        assertEquals("HYP-SCENARIO-0001", scenario.id());
        assertEquals(capability.id(), scenario.capabilityId());
        assertEquals("Find owners by last name", scenario.title());
        assertEquals("a recorded test exercises the system", scenario.given());
        assertEquals("find owners by last name", scenario.when());
        assertEquals("the recorded expectations hold", scenario.then());
        assertEquals(ScenarioStatus.UNREVIEWED, scenario.scenarioStatus());
        assertEquals(ProposalAuthority.PROPOSAL_ONLY, scenario.authority());
        assertFalse(scenario.semanticPublicationAllowed());
        assertEquals(5000L, scenario.confidenceScale4());
    }

    @Test
    void everyProposalCitesAtLeastTwoDistinctChannels() {
        ReverseProposalPackage proposalPackage = generator.propose(standardBundle());
        for (CapabilityProposal capability : proposalPackage.capabilities())
            assertTrue(distinctChannels(capability.citations()).size() >= 2,
                    "capability citations must span at least two channels: " + capability.id());
        for (ScenarioProposal scenario : proposalPackage.scenarios())
            assertTrue(distinctChannels(scenario.citations()).size() >= 2,
                    "scenario citations must span at least two channels: " + scenario.id());
    }

    @Test
    void contributingDigestsMatchCitedDigestsExactly() {
        ReverseProposalPackage proposalPackage = generator.propose(standardBundle());
        for (CapabilityProposal capability : proposalPackage.capabilities())
            assertEquals(citedDigests(capability.citations()), Set.copyOf(capability.contributingEvidenceDigests()));
        for (ScenarioProposal scenario : proposalPackage.scenarios())
            assertEquals(citedDigests(scenario.citations()), Set.copyOf(scenario.contributingEvidenceDigests()));
    }

    @Test
    void citationsAreInStableOrder() {
        ReverseProposalPackage proposalPackage = generator.propose(standardBundle());
        for (CapabilityProposal capability : proposalPackage.capabilities()) {
            List<EvidenceCitation> ordered = new ArrayList<>(capability.citations());
            ordered.sort(EvidenceCitation.stableOrder());
            assertEquals(ordered, capability.citations());
        }
    }

    @Test
    void proposalWordingIsFreeOfImplementationIdentifiers() {
        ReverseProposalPackage proposalPackage = generator.propose(standardBundle());
        for (CapabilityProposal capability : proposalPackage.capabilities())
            assertTrue(ScenarioTextValidator.validate(capability.title()).isEmpty(),
                    "capability title must be clean: " + capability.title());
        for (ScenarioProposal scenario : proposalPackage.scenarios()) {
            assertTrue(ScenarioTextValidator.validate(scenario.title()).isEmpty());
            assertTrue(ScenarioTextValidator.validate(scenario.given()).isEmpty());
            assertTrue(ScenarioTextValidator.validate(scenario.when()).isEmpty());
            assertTrue(ScenarioTextValidator.validate(scenario.then()).isEmpty());
        }
    }

    @Test
    void threeChannelCorroborationRaisesConfidenceDeterministically() {
        ReverseEvidenceBundle bundle = bundle(
                structuralChannel(structuralNode(TEST_PATH)),
                testBehaviorChannel(TEST_PATH, "shouldFindOwners"),
                deliveryChannel(TEST_PATH));
        ReverseProposalPackage proposalPackage = generator.propose(bundle);
        assertEquals(6500L, proposalPackage.capabilities().get(0).confidenceScale4());
        assertEquals(6000L, proposalPackage.scenarios().get(0).confidenceScale4());
    }

    @Test
    void identifiersFollowSortedPathOrderNotPayloadOrder() {
        String pathB = "src/test/java/x/BetaTests.java";
        String pathA = "src/test/java/x/AlphaTests.java";
        ReverseEvidenceBundle bundle = bundle(
                structuralChannel(structuralNode(pathB), structuralNode(pathA)),
                testBehaviorChannel(List.of(pathB, pathA), List.of("shouldDoBeta"), List.of("shouldDoAlpha")),
                deliveryChannel());
        ReverseProposalPackage proposalPackage = generator.propose(bundle);
        assertEquals(2, proposalPackage.capabilities().size());
        assertEquals("Alpha", proposalPackage.capabilities().get(0).title());
        assertEquals("HYP-CAPABILITY-0001", proposalPackage.capabilities().get(0).id());
        assertEquals("Beta", proposalPackage.capabilities().get(1).title());
        assertEquals("HYP-CAPABILITY-0002", proposalPackage.capabilities().get(1).id());
        List<String> scenarioIds = proposalPackage.scenarios().stream().map(ScenarioProposal::id).toList();
        assertIterableEquals(List.of("HYP-SCENARIO-0001", "HYP-SCENARIO-0002"), scenarioIds);
    }

    // ------------------------------------------------------------------
    // Evidence gap partition
    // ------------------------------------------------------------------

    @Test
    void uncitedObservationsBecomeExplicitGaps() {
        ReverseProposalPackage proposalPackage = generator.propose(standardBundle());

        // Structural nodes of the two non-proposal paths, the README changed
        // path, the wording-less method, two commits, one episode.
        assertEquals(8, proposalPackage.evidenceGaps().size());
        Set<String> gapRefs = new HashSet<>();
        for (EvidenceGap gap : proposalPackage.evidenceGaps()) gapRefs.add(gap.observationRef());
        assertEquals(Set.of(
                "/nodes/1",
                "/nodes/2",
                "/changed_paths/0",
                "/changed_paths/1",
                "/test_files/0/test_methods/1",
                "/commits/0",
                "/commits/1",
                "/episodes/0"), gapRefs);
    }

    @Test
    void corroboratedPathWithoutTestBehaviorStaysGap() {
        ReverseProposalPackage proposalPackage = generator.propose(standardBundle());
        long mainPathGaps = proposalPackage.evidenceGaps().stream()
                .filter(gap -> gap.observationRef().equals("/nodes/1")
                        || gap.observationRef().equals("/changed_paths/0"))
                .count();
        assertEquals(2L, mainPathGaps);
    }

    @Test
    void citationsAndGapsPartitionEveryObservationExactly() {
        ReverseEvidenceBundle bundle = standardBundle();
        ReverseProposalPackage proposalPackage = generator.propose(bundle);

        Set<String> all = new HashSet<>();
        JsonNode structural = bundle.channel(ReverseEvidenceChannel.STRUCTURAL).observations();
        for (int i = 0; i < structural.path("nodes").size(); i++) all.add("/nodes/" + i);
        JsonNode test = bundle.channel(ReverseEvidenceChannel.TEST_BEHAVIOR).observations();
        for (int f = 0; f < test.path("test_files").size(); f++) {
            all.add("/test_files/" + f);
            for (int m = 0; m < test.path("test_files").get(f).path("test_methods").size(); m++)
                all.add("/test_files/" + f + "/test_methods/" + m);
        }
        JsonNode delivery = bundle.channel(ReverseEvidenceChannel.DELIVERY_HISTORY).observations();
        for (int i = 0; i < delivery.path("changed_paths").size(); i++) all.add("/changed_paths/" + i);
        for (int i = 0; i < delivery.path("commits").size(); i++) all.add("/commits/" + i);
        for (int i = 0; i < delivery.path("episodes").size(); i++) all.add("/episodes/" + i);

        Set<String> cited = new HashSet<>();
        for (CapabilityProposal capability : proposalPackage.capabilities())
            for (EvidenceCitation citation : capability.citations()) cited.add(citation.observationRef());
        for (ScenarioProposal scenario : proposalPackage.scenarios())
            for (EvidenceCitation citation : scenario.citations()) cited.add(citation.observationRef());
        Set<String> gaps = new HashSet<>();
        for (EvidenceGap gap : proposalPackage.evidenceGaps()) {
            assertTrue(gaps.add(gap.observationRef()), "duplicate gap: " + gap.observationRef());
        }
        assertEquals(all.size(), cited.size() + gaps.size());
        for (String ref : cited) assertFalse(gaps.contains(ref), "observation both cited and gapped: " + ref);
        for (String ref : all) assertTrue(cited.contains(ref) || gaps.contains(ref), "unaccounted observation: " + ref);
    }

    // ------------------------------------------------------------------
    // Determinism
    // ------------------------------------------------------------------

    @Test
    void repeatedGenerationIsByteIdentical() {
        ReverseEvidenceBundle bundle = standardBundle();
        byte[] first = ReverseProposalPackageJson.write(generator.propose(bundle));
        byte[] second = ReverseProposalPackageJson.write(generator.propose(bundle));
        assertEquals(first.length, second.length);
        org.junit.jupiter.api.Assertions.assertArrayEquals(first, second);
    }

    @Test
    void generatorDoesNotMutateTheBundle() {
        ReverseEvidenceBundle bundle = standardBundle();
        String before = bundle.channel(ReverseEvidenceChannel.STRUCTURAL).observations().toString();
        generator.propose(bundle);
        assertEquals(before, bundle.channel(ReverseEvidenceChannel.STRUCTURAL).observations().toString());
    }

    // ------------------------------------------------------------------
    // Mechanical wording rules
    // ------------------------------------------------------------------

    @Test
    void wordingWordsStripPrefixesAndSplitIdentifiers() {
        assertEquals(List.of("find", "owners", "by", "last", "name"),
                DeterministicReverseProposalGenerator.wordingWords("shouldFindOwnersByLastName"));
        assertEquals(List.of("find", "all"),
                DeterministicReverseProposalGenerator.wordingWords("findAll"));
        assertEquals(List.of("init", "creation", "form"),
                DeterministicReverseProposalGenerator.wordingWords("initCreationForm"));
        assertEquals(List.of(), DeterministicReverseProposalGenerator.wordingWords("test"));
        assertEquals(List.of("owner", "controller"),
                DeterministicReverseProposalGenerator.wordingWords(
                        DeterministicReverseProposalGenerator.stripTestSuffix(
                                DeterministicReverseProposalGenerator.fileStem(TEST_PATH))));
    }

    // ------------------------------------------------------------------
    // Fail-closed payload validation
    // ------------------------------------------------------------------

    @Test
    void nullBundleFailsClosed() {
        ReverseContractException failure = assertThrows(ReverseContractException.class, () -> generator.propose(null));
        assertEquals(ReverseFailure.MISSING_INPUT, failure.failure());
    }

    @Test
    void malformedChannelPayloadsFailClosed() {
        ObjectNode nodesMissingSourceFile = JSON.createObjectNode();
        nodesMissingSourceFile.set("nodes", JSON.createArrayNode().add(JSON.createObjectNode()));
        assertEquals(ReverseFailure.MISSING_INPUT, assertThrows(ReverseContractException.class, () -> generator.propose(
                bundle(structuralChannelBad(nodesMissingSourceFile),
                        testBehaviorChannel(TEST_PATH, "shouldDoX"),
                        deliveryChannel(TEST_PATH)))).failure());
    }

    @Test
    void missingDeliveryCommitsArrayFailsClosed() {
        ObjectNode observations = JSON.createObjectNode();
        observations.set("changed_paths", JSON.createArrayNode());
        ReverseContractException failure = assertThrows(ReverseContractException.class, () -> generator.propose(
                bundle(
                        structuralChannel(structuralNode(TEST_PATH)),
                        testBehaviorChannel(TEST_PATH, "shouldDoX"),
                        channel(ReverseEvidenceChannel.DELIVERY_HISTORY, DELIVERY_DIGEST, observations))));
        assertEquals(ReverseFailure.MISSING_INPUT, failure.failure());
    }

    private static EvidenceChannelRecord structuralChannelBad(ObjectNode observations) {
        return channel(ReverseEvidenceChannel.STRUCTURAL, STRUCTURAL_DIGEST, observations);
    }

    private static Set<ReverseEvidenceChannel> distinctChannels(List<EvidenceCitation> citations) {
        Set<ReverseEvidenceChannel> channels = new TreeSet<>();
        for (EvidenceCitation citation : citations) channels.add(citation.channel());
        return channels;
    }

    private static Set<String> citedDigests(List<EvidenceCitation> citations) {
        Set<String> digests = new TreeSet<>();
        for (EvidenceCitation citation : citations) digests.add(citation.evidenceDigest());
        return digests;
    }
}

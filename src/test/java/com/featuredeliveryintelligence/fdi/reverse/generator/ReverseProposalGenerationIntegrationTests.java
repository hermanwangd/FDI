package com.featuredeliveryintelligence.fdi.reverse.generator;

import com.featuredeliveryintelligence.fdi.reverse.evidence.EvidenceChannelRecord;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceBundle;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceChannel;
import com.featuredeliveryintelligence.fdi.reverse.input.deliveryhistory.DeliveryHistoryInputAdapter;
import com.featuredeliveryintelligence.fdi.reverse.input.structural.StructuralEvidenceAdapter;
import com.featuredeliveryintelligence.fdi.reverse.input.structural.StructuralSnapshotBinding;
import com.featuredeliveryintelligence.fdi.reverse.input.testbehavior.TestBehaviorEvidenceAdapter;
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
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Slice E integration test over the real pinned Petclinic inputs: the three
 * accepted adapters build the bundle, the generator seals the proposal
 * package, every acceptance invariant of the execution envelope is checked,
 * and the serialized package validates against
 * {@code contracts/reverse-proposal.schema.json}.
 */
class ReverseProposalGenerationIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String REPOSITORY_ID = "spring-petclinic";
    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String GRAPH_PATH =
            "validation/pkb001/reverse-pkb-bl009-petclinic-001/provider-discovery/graph-petclinic-818c413-main-and-test.json";
    private static final String GRAPH_SHA256 =
            "5ff8454ae758c8b8ffb1e755ff7c2124efd233388f4f901d8d964b04977c27ca";

    private static final int EXPECTED_CAPABILITIES = 18;
    private static final int EXPECTED_SCENARIOS = 76;
    private static final int EXPECTED_GAPS = 7043;

    private final DeterministicReverseProposalGenerator generator = new DeterministicReverseProposalGenerator();

    private static ReverseEvidenceBundle acceptedBundle() {
        Path root = Path.of("").toAbsolutePath();
        EvidenceChannelRecord structural = StructuralEvidenceAdapter.load(
                new StructuralSnapshotBinding(
                        REPOSITORY_ID, REVISION, GRAPH_PATH, GRAPH_SHA256,
                        "graphify", "frozen Graphify snapshot via CodeIntelligenceProvider"),
                root);
        EvidenceChannelRecord testBehavior = TestBehaviorEvidenceAdapter.loadAccepted(root);
        EvidenceChannelRecord delivery = DeliveryHistoryInputAdapter.load(
                root, REPOSITORY_ID, REVISION, DeliveryHistoryInputAdapter.PINNED_INPUT_SHA256);
        ReverseEvidenceBundle bundle =
                new ReverseEvidenceBundle(REPOSITORY_ID, REVISION, "1", List.of(structural, testBehavior, delivery));
        ReverseEvidenceBundle.verifyInputDigests(bundle, root);
        return bundle;
    }

    @Test
    void sealedPackageOverAcceptedInputsSatisfiesEveryAcceptanceInvariant() {
        ReverseEvidenceBundle bundle = acceptedBundle();
        ReverseProposalPackage proposalPackage = generator.propose(bundle);

        assertEquals(REPOSITORY_ID, proposalPackage.repositoryId());
        assertEquals(REVISION, proposalPackage.canonicalRevision());
        assertEquals(EXPECTED_CAPABILITIES, proposalPackage.capabilities().size());
        assertEquals(EXPECTED_SCENARIOS, proposalPackage.scenarios().size());
        assertEquals(EXPECTED_GAPS, proposalPackage.evidenceGaps().size());

        for (CapabilityProposal capability : proposalPackage.capabilities()) {
            assertTrue(capability.id().matches("HYP-CAPABILITY-[0-9]{4}"));
            assertEquals(ProposalAuthority.PROPOSAL_ONLY, capability.authority());
            assertFalse(capability.semanticPublicationAllowed());
            assertEquals(REVISION, capability.sourceRevision());
            assertTrue(capability.confidenceScale4() >= 0 && capability.confidenceScale4() <= 10000);
            assertTrue(capability.citations().size() >= 3,
                    "every accepted capability is corroborated by all three channels: " + capability.id());
            assertTrue(ScenarioTextValidator.validate(capability.title()).isEmpty(),
                    "capability title must be identifier-free: " + capability.title());
        }
        for (ScenarioProposal scenario : proposalPackage.scenarios()) {
            assertTrue(scenario.id().matches("HYP-SCENARIO-[0-9]{4}"));
            assertEquals(ScenarioStatus.UNREVIEWED, scenario.scenarioStatus());
            assertEquals(ProposalAuthority.PROPOSAL_ONLY, scenario.authority());
            assertFalse(scenario.semanticPublicationAllowed());
            assertEquals(REVISION, scenario.sourceRevision());
            assertTrue(ScenarioTextValidator.validate(scenario.title()).isEmpty());
            assertTrue(ScenarioTextValidator.validate(scenario.given()).isEmpty());
            assertTrue(ScenarioTextValidator.validate(scenario.when()).isEmpty());
            assertTrue(ScenarioTextValidator.validate(scenario.then()).isEmpty());
            Set<String> channels = new TreeSet<>();
            for (EvidenceCitation citation : scenario.citations()) channels.add(citation.channel().name());
            assertTrue(channels.size() >= 2,
                    "every scenario cites at least two distinct evidence channels: " + scenario.id());
        }

        assertCitationsAndGapsPartitionAllObservations(bundle, proposalPackage);
    }

    @Test
    void repeatedSealingIsByteIdentical() {
        ReverseEvidenceBundle bundle = acceptedBundle();
        byte[] first = ReverseProposalPackageJson.write(generator.propose(bundle));
        byte[] second = ReverseProposalPackageJson.write(generator.propose(bundle));
        org.junit.jupiter.api.Assertions.assertArrayEquals(first, second);
    }

    @Test
    void serializedPackageValidatesAgainstTheProviderNeutralSchema() throws Exception {
        ReverseEvidenceBundle bundle = acceptedBundle();
        byte[] document = ReverseProposalPackageJson.write(generator.propose(bundle));

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // networknt requires an absolute base $id; the checked-in schema keeps
        // a relative one. All $refs are same-document fragments, so the
        // in-memory normalization cannot change validation outcomes.
        JsonNode definition = JSON.readTree(Files.readString(
                Path.of("").toAbsolutePath().resolve("contracts/reverse-proposal.schema.json")));
        ((com.fasterxml.jackson.databind.node.ObjectNode) definition)
                .put("$id", "urn:fdi:contracts/reverse-proposal.schema.json");
        JsonSchema schema = factory.getSchema(definition);
        Set<ValidationMessage> messages = schema.validate(JSON.readTree(document));
        assertTrue(messages.isEmpty(), () -> "schema validation failed: " + messages);

        JsonNode parsed = JSON.readTree(document);
        assertEquals("1", parsed.get("schema_version").asText());
        assertEquals(REVISION, parsed.get("canonical_revision").asText());
    }

    @Test
    void serializedPackageContainsNoWallClockFields() throws Exception {
        ReverseEvidenceBundle bundle = acceptedBundle();
        JsonNode document = JSON.readTree(ReverseProposalPackageJson.write(generator.propose(bundle)));
        Set<String> keys = new HashSet<>();
        collectKeys(document, keys);
        for (String key : keys)
            assertFalse(key.toLowerCase().contains("time") && !key.equals("history_cutoff"),
                    "sealed package must not contain wall-clock fields: " + key);
    }

    private static void collectKeys(JsonNode node, Set<String> keys) {
        if (node.isObject()) {
            node.fieldNames().forEachRemaining(key -> {
                keys.add(key);
                collectKeys(node.get(key), keys);
            });
        } else if (node.isArray()) {
            node.forEach(element -> collectKeys(element, keys));
        }
    }

    private static void assertCitationsAndGapsPartitionAllObservations(
            ReverseEvidenceBundle bundle, ReverseProposalPackage proposalPackage) {
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
            for (EvidenceCitation citation : capability.citations())
                cited.add(citation.observationRef());
        for (ScenarioProposal scenario : proposalPackage.scenarios())
            for (EvidenceCitation citation : scenario.citations())
                cited.add(citation.observationRef());
        Set<String> gaps = new HashSet<>();
        for (EvidenceGap gap : proposalPackage.evidenceGaps())
            assertTrue(gaps.add(gap.observationRef()), "duplicate gap: " + gap.observationRef());

        assertEquals(all.size(), cited.size() + gaps.size(),
                "every normalized observation must be cited or gapped exactly once");
        for (String ref : cited) assertFalse(gaps.contains(ref), "observation both cited and gapped: " + ref);
        for (String ref : all)
            assertTrue(cited.contains(ref) || gaps.contains(ref), "unaccounted observation: " + ref);
    }
}

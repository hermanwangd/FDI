package com.featuredeliveryintelligence.fdi.reverse.evaluation;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import com.featuredeliveryintelligence.fdi.reverse.evidence.EvidenceChannelRecord;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceBundle;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceChannel;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseComparisonReport.CapabilityMatch;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseComparisonReport.ComponentDiagnostics;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseEvaluatorTruth.TruthCapability;
import com.featuredeliveryintelligence.fdi.reverse.evaluation.ReverseEvaluatorTruth.TruthComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deterministic comparison math and fail-closed input binding for the
 * evaluator-only comparator, exercised over a small synthetic fixture.
 */
class ReverseProposalComparatorTests {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String REPOSITORY = "spring-petclinic";
    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String STRUCTURAL_DIGEST =
            "5ff8454ae758c8b8ffb1e755ff7c2124efd233388f4f901d8d964b04977c27ca";
    private static final String TEST_DIGEST =
            "082c621945ef063fec82adb3766348c0ecc888ba71a2f1ed34b147ebfa8bcc8a";
    private static final String DELIVERY_DIGEST =
            "87b70439f69bb82cee0139b65108ecce6044aff1a4d1b5ce6324933acbeab2d1";

    private static final String TEST_PATH =
            "src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java";

    @Test
    void comparisonReportsEveryDimensionSeparately() {
        ReverseComparisonReport report = ReverseProposalComparator.compare(
                sealedPackageBytes(), truth(), bundle());

        assertEquals(REPOSITORY, report.repositoryId());
        assertEquals(REVISION, report.canonicalRevision());
        assertEquals(64, report.proposalPackageSha256().length());

        CapabilityMatch match = report.capabilityMatch();
        assertEquals(1, match.proposedCapabilities());
        assertEquals(2, match.evaluatorCapabilities());
        assertEquals(1, match.matched());
        assertEquals(1.0, match.precision());
        assertEquals(0.5, match.recall());
        assertEquals("HYP-CAPABILITY-0001", match.matches().get(0).proposedId());
        assertEquals("PET-CAP-01", match.matches().get(0).goldId());
        assertEquals(0.5, match.matches().get(0).score());

        var coverage = report.scenarioPresentationCoverage();
        assertEquals(2, coverage.evaluatorCapabilities());
        assertEquals(1, coverage.coveredEvaluatorCapabilities());
        assertEquals(0.5, coverage.coverage());
        assertEquals("HYP-SCENARIO-0001",
                coverage.perCapability().get(0).presentingScenarioIds().get(0));

        var channels = report.evidenceChannelCoverage();
        assertEquals(3, channels.perChannel().size());
        for (var channel : channels.perChannel()) {
            assertEquals(1, channel.capabilitiesCiting());
            assertEquals(1, channel.scenariosCiting());
        }
        assertEquals(1, channels.allThreeChannels().capabilities());
        assertEquals(1, channels.allThreeChannels().scenarios());

        var unsupported = report.unsupportedProposalRate();
        assertEquals(0, unsupported.unsupportedProposals());
        assertEquals(1, unsupported.proposedCapabilities());
        assertEquals(0.0, unsupported.rate());

        ComponentDiagnostics overall = report.exactComponentDiagnostics().overall();
        assertEquals(0, overall.pathLevel().matched());
        assertEquals(2, overall.pathLevel().expected());
        assertEquals(0.0, overall.pathLevel().recall());
        assertEquals(1, report.exactComponentDiagnostics().perCapability().size());
        assertEquals(1, report.exactComponentDiagnostics().perCapability().get(0)
                .diagnostics().exactComponentLevel().expected());
        assertEquals(4, report.limitations().size());
    }

    @Test
    void unmatchedProposalsDriveUnsupportedRate() {
        ReverseComparisonReport report = ReverseProposalComparator.compare(
                sealedPackageBytes(), new ReverseEvaluatorTruth(
                        "set", "seal", ReverseEvaluatorTruth.GOLD_PATH, STRUCTURAL_DIGEST, REVISION,
                        List.of(new TruthCapability("PET-CAP-09", "Browse Veterinarians",
                                List.of(new TruthComponent(
                                        "src/main/java/org/springframework/samples/petclinic/vet/VetController.java",
                                        "VetController",
                                        "vetcontroller_vetcontroller_showvetlist"))))),
                bundle());

        CapabilityMatch match = report.capabilityMatch();
        assertEquals(0, match.matched());
        assertEquals(0.0, match.precision());
        assertEquals(0.0, match.recall());
        assertEquals(1, report.unsupportedProposalRate().unsupportedProposals());
        assertEquals(1.0, report.unsupportedProposalRate().rate());
        assertEquals(List.of("HYP-CAPABILITY-0001"),
                report.unsupportedProposalRate().unsupportedProposalIds());
        assertEquals(0, report.scenarioPresentationCoverage().coveredEvaluatorCapabilities());
    }

    @Test
    void evaluatorRevisionDisagreementFailsClosed() {
        ReverseEvaluatorTruth truth = new ReverseEvaluatorTruth(
                "set", "seal", ReverseEvaluatorTruth.GOLD_PATH, STRUCTURAL_DIGEST,
                "1111111111111111111111111111111111111111",
                List.of(new TruthCapability("PET-CAP-01", "Find Owners",
                        List.of(new TruthComponent(
                                "src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java",
                                "OwnerController",
                                "ownercontroller_ownercontroller_processfindform")))));
        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseProposalComparator.compare(sealedPackageBytes(), truth, bundle()));
        assertEquals(ReverseFailure.REVISION_MISMATCH, failure.failure());
    }

    @Test
    void citationDigestDisagreementFailsClosed() {
        ReverseEvidenceBundle bundle = bundle();
        String tampered = new String(sealedPackageBytes())
                .replace(STRUCTURAL_DIGEST,
                        "4db3b4fcac22c704f321e2a3bef20091741147984815addf3ebf77d88643e66d");
        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseProposalComparator.compare(tampered.getBytes(), truth(), bundle));
        assertEquals(ReverseFailure.DIGEST_MISMATCH, failure.failure());
    }

    @Test
    void unresolvableStructuralCitationFailsClosed() {
        String tampered = new String(sealedPackageBytes())
                .replace("/nodes/0", "/nodes/99");
        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseProposalComparator.compare(tampered.getBytes(), truth(), bundle()));
        assertEquals(ReverseFailure.MISSING_INPUT, failure.failure());
    }

    @Test
    void nonNodesStructuralCitationFailsClosed() {
        String tampered = new String(sealedPackageBytes())
                .replace("/nodes/0", "/files/0");
        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseProposalComparator.compare(tampered.getBytes(), truth(), bundle()));
        assertEquals(ReverseFailure.MISSING_INPUT, failure.failure());
    }

    @Test
    void sealedPackageWithBrokenAuthorityFailsBeforeComparison() {
        String tampered = new String(sealedPackageBytes())
                .replace("\"authority\": \"PROPOSAL_ONLY\"", "\"authority\": \"ACCEPTED\"");
        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseProposalComparator.compare(tampered.getBytes(), truth(), bundle()));
        assertEquals(ReverseFailure.AUTHORITY_VIOLATION, failure.failure());
    }

    @Test
    void reportSerializationIsByteStableAndDimensionSeparated() throws Exception {
        ReverseComparisonReport report = ReverseProposalComparator.compare(
                sealedPackageBytes(), truth(), bundle());
        byte[] first = ReverseComparisonReportJson.write(report);
        byte[] second = ReverseComparisonReportJson.write(report);
        org.junit.jupiter.api.Assertions.assertArrayEquals(first, second);

        ObjectNode document = (ObjectNode) JSON.createParser(first).readValueAsTree();
        for (String key : ReverseComparisonReportJson.dimensionKeys())
            assertTrue(document.has(key), "missing dimension: " + key);
        assertTrue(!document.has("aggregate_score") && !document.has("overall_score"),
                "no aggregate score may hide a failed dimension");
        assertEquals("EVALUATOR_COMPARISON_ONLY", document.get("authority").asText());
        assertTrue(document.get("semantic_publication_allowed").isBoolean()
                && !document.get("semantic_publication_allowed").asBoolean());
        assertEquals("PKB-BL-009-REVERSE-PROPOSAL-001-sliceF",
                document.get("comparison_set_id").asText());
    }

    // ------------------------------------------------------------------
    // Synthetic fixture
    // ------------------------------------------------------------------

    private static ReverseEvaluatorTruth truth() {
        return new ReverseEvaluatorTruth(
                "pkb001-petclinic-818c413-evaluator-v1",
                "pkb001-petclinic-818c413-ground-truth-v1",
                ReverseEvaluatorTruth.GOLD_PATH,
                "4d22799e4d7597e0bbc302c9db3cd0510f70cc946cb5de5909ded9c4b1b112d1",
                REVISION,
                List.of(
                        new TruthCapability("PET-CAP-01", "Find Owners",
                                List.of(new TruthComponent(
                                        "src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java",
                                        "OwnerController",
                                        "ownercontroller_ownercontroller_processfindform"))),
                        new TruthCapability("PET-CAP-08", "Browse Veterinarians",
                                List.of(new TruthComponent(
                                        "src/main/java/org/springframework/samples/petclinic/vet/VetController.java",
                                        "VetController",
                                        "vetcontroller_vetcontroller_showvetlist")))));
    }

    private static ReverseEvidenceBundle bundle() {
        ObjectNode structural = JSON.createObjectNode();
        var nodes = structural.putArray("nodes");
        ObjectNode fileNode = nodes.addObject();
        fileNode.put("source_file", TEST_PATH);
        fileNode.put("provider_node_id", "ownercontrollertests");
        ObjectNode methodNode = nodes.addObject();
        methodNode.put("source_file", TEST_PATH);
        methodNode.put("provider_node_id", "ownercontrollertests_ownercontrollertests_testfindowner");

        ObjectNode testBehavior = JSON.createObjectNode();
        var files = testBehavior.putArray("test_files");
        ObjectNode file = files.addObject();
        file.put("repository_relative_path", TEST_PATH);
        var methods = file.putArray("test_methods");
        methods.addObject().put("method_name", "testFindOwner");

        ObjectNode delivery = JSON.createObjectNode();
        var changedPaths = delivery.putArray("changed_paths");
        changedPaths.addObject().put("path", TEST_PATH);
        delivery.putArray("commits");
        delivery.putArray("episodes");

        return new ReverseEvidenceBundle(REPOSITORY, REVISION, "1", List.of(
                new EvidenceChannelRecord(ReverseEvidenceChannel.STRUCTURAL, REPOSITORY, REVISION,
                        "validation/pkb001/reverse-pkb-bl009-petclinic-001/provider-discovery/graph.json",
                        STRUCTURAL_DIGEST, "1", "graphify", "synthetic structural input", structural),
                new EvidenceChannelRecord(ReverseEvidenceChannel.TEST_BEHAVIOR, REPOSITORY, REVISION,
                        "validation/pkb001/reverse-pkb-bl009-petclinic-001/java-test-behavior/evidence.json",
                        TEST_DIGEST, "1", "java-extractor", "synthetic test-behavior input", testBehavior),
                new EvidenceChannelRecord(ReverseEvidenceChannel.DELIVERY_HISTORY, REPOSITORY, REVISION,
                        "validation/pkb001/datasets/petclinic-delivery-history.json",
                        DELIVERY_DIGEST, "1", "java-delivery", "synthetic delivery input", delivery)));
    }

    private static byte[] sealedPackageBytes() {
        String document = "{"
                + "\"schema_version\": \"1\","
                + "\"repository_id\": \"" + REPOSITORY + "\","
                + "\"canonical_revision\": \"" + REVISION + "\","
                + "\"capabilities\": [ {"
                + "\"id\": \"HYP-CAPABILITY-0001\","
                + "\"title\": \"Owner controller\","
                + "\"citations\": ["
                + citation("STRUCTURAL", "/nodes/0", STRUCTURAL_DIGEST) + ","
                + citation("STRUCTURAL", "/nodes/1", STRUCTURAL_DIGEST) + ","
                + citation("TEST_BEHAVIOR", "/test_files/0", TEST_DIGEST) + ","
                + citation("DELIVERY_HISTORY", "/changed_paths/0", DELIVERY_DIGEST)
                + "],"
                + "\"source_revision\": \"" + REVISION + "\","
                + "\"contributing_evidence_digests\": [\"" + STRUCTURAL_DIGEST + "\"],"
                + "\"inference_rationale\": \"synthetic\","
                + "\"limitations\": [\"synthetic\"],"
                + "\"confidence\": 8000,"
                + "\"authority\": \"PROPOSAL_ONLY\","
                + "\"semantic_publication_allowed\": false"
                + "} ],"
                + "\"scenarios\": [ {"
                + "\"id\": \"HYP-SCENARIO-0001\","
                + "\"capability_id\": \"HYP-CAPABILITY-0001\","
                + "\"title\": \"Find owner\","
                + "\"given\": \"a recorded test exercises the system\","
                + "\"when\": \"find owner\","
                + "\"then\": \"the recorded expectations hold\","
                + "\"citations\": ["
                + citation("STRUCTURAL", "/nodes/0", STRUCTURAL_DIGEST) + ","
                + citation("TEST_BEHAVIOR", "/test_files/0/test_methods/0", TEST_DIGEST) + ","
                + citation("DELIVERY_HISTORY", "/changed_paths/0", DELIVERY_DIGEST)
                + "],"
                + "\"source_revision\": \"" + REVISION + "\","
                + "\"contributing_evidence_digests\": [\"" + TEST_DIGEST + "\"],"
                + "\"inference_rationale\": \"synthetic\","
                + "\"limitations\": [\"synthetic\"],"
                + "\"confidence\": 7000,"
                + "\"scenario_status\": \"UNREVIEWED\","
                + "\"authority\": \"PROPOSAL_ONLY\","
                + "\"semantic_publication_allowed\": false"
                + "} ],"
                + "\"evidence_gaps\": []"
                + "}";
        return document.getBytes();
    }

    private static String citation(String channel, String ref, String digest) {
        return "{"
                + "\"channel\": \"" + channel + "\","
                + "\"observation_ref\": \"" + ref + "\","
                + "\"evidence_digest\": \"" + digest + "\""
                + "}";
    }
}

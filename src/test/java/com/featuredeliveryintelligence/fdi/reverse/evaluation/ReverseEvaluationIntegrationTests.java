package com.featuredeliveryintelligence.fdi.reverse.evaluation;

import com.featuredeliveryintelligence.fdi.reverse.evidence.EvidenceChannelRecord;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceBundle;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceChannel;
import com.featuredeliveryintelligence.fdi.reverse.generator.DeterministicReverseProposalGenerator;
import com.featuredeliveryintelligence.fdi.reverse.generator.ReverseProposalPackageJson;
import com.featuredeliveryintelligence.fdi.reverse.input.deliveryhistory.DeliveryHistoryInputAdapter;
import com.featuredeliveryintelligence.fdi.reverse.input.structural.StructuralEvidenceAdapter;
import com.featuredeliveryintelligence.fdi.reverse.input.structural.StructuralSnapshotBinding;
import com.featuredeliveryintelligence.fdi.reverse.input.testbehavior.TestBehaviorEvidenceAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Slice F evaluator-only comparison over the real pinned Petclinic inputs.
 * Replays the sealed A–E code to re-materialize the byte-identical sealed
 * proposal package, binds the frozen sealed evaluator surface, computes the
 * per-dimension comparison report, and maintains the immutable run-directory
 * artifacts: when the artifacts already exist they must match the regenerated
 * bytes exactly (byte-determinism replay check); when absent they are written.
 */
class ReverseEvaluationIntegrationTests {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String REPOSITORY_ID = "spring-petclinic";
    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String GRAPH_PATH =
            "validation/pkb001/reverse-pkb-bl009-petclinic-001/provider-discovery/graph-petclinic-818c413-main-and-test.json";
    private static final String GRAPH_SHA256 =
            "5ff8454ae758c8b8ffb1e755ff7c2124efd233388f4f901d8d964b04977c27ca";

    private static final Path RUN_DIRECTORY = Path.of(
            "validation/pkb001/reverse-pkb-bl009-petclinic-001/proposal-run");
    private static final Path PROPOSAL_PACKAGE_FILE = RUN_DIRECTORY.resolve("proposal-package.json");
    private static final Path COMPARISON_REPORT_FILE = RUN_DIRECTORY.resolve("comparison-report.json");

    private final DeterministicReverseProposalGenerator generator =
            new DeterministicReverseProposalGenerator();

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
    void sealedPackageIsMaterializedAndComparisonRunsAgainstTheFrozenEvaluatorSurface() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        ReverseEvidenceBundle bundle = acceptedBundle();
        byte[] sealedPackage = ReverseProposalPackageJson.write(generator.propose(bundle));

        ReverseEvaluatorTruth truth = ReverseEvaluatorTruth.load(root);
        ReverseComparisonReport report =
                ReverseProposalComparator.compare(sealedPackage, truth, bundle);

        assertEquals(REPOSITORY_ID, report.repositoryId());
        assertEquals(REVISION, report.canonicalRevision());
        assertEquals(18, report.capabilityMatch().proposedCapabilities());
        assertEquals(10, report.capabilityMatch().evaluatorCapabilities());
        assertEquals(18, report.evidenceChannelCoverage().allThreeChannels().capabilities());
        assertEquals(76, report.evidenceChannelCoverage().allThreeChannels().scenarios());
        for (var channel : report.evidenceChannelCoverage().perChannel()) {
            assertEquals(18, channel.capabilitiesCiting());
            assertEquals(76, channel.scenariosCiting());
        }
        assertEquals(report.capabilityMatch().matched(),
                report.capabilityMatch().proposedCapabilities()
                        - report.unsupportedProposalRate().unsupportedProposals());
        assertEquals(10, report.scenarioPresentationCoverage().perCapability().size());
        assertEquals(report.exactComponentDiagnostics().perCapability().size(),
                report.capabilityMatch().matched());
        assertTrue(report.capabilityMatch().matched() > 0,
                "the mechanical matching rule must match at least one evaluator capability");

        maintainArtifact(PROPOSAL_PACKAGE_FILE, sealedPackage);
        byte[] comparisonReport = ReverseComparisonReportJson.write(report);
        maintainArtifact(COMPARISON_REPORT_FILE, comparisonReport);

        // The committed comparison report must itself replay byte-identically.
        ReverseComparisonReport replayed =
                ReverseProposalComparator.compare(sealedPackage, truth, bundle);
        assertArrayEquals(Files.readAllBytes(COMPARISON_REPORT_FILE),
                ReverseComparisonReportJson.write(replayed));
    }

    @Test
    void comparisonReportKeepsDimensionsSeparateWithoutAggregateScore() throws Exception {
        ReverseEvidenceBundle bundle = acceptedBundle();
        byte[] sealedPackage = ReverseProposalPackageJson.write(generator.propose(bundle));
        ReverseComparisonReport report = ReverseProposalComparator.compare(
                sealedPackage, ReverseEvaluatorTruth.load(Path.of("").toAbsolutePath()), bundle);

        byte[] document = ReverseComparisonReportJson.write(report);
        assertArrayEquals(document, ReverseComparisonReportJson.write(report));

        JsonNode parsed = JSON.readTree(document);
        for (String key : ReverseComparisonReportJson.dimensionKeys())
            assertTrue(parsed.has(key), "missing dimension: " + key);
        Set<String> keys = new HashSet<>();
        collectKeys(parsed, keys);
        assertFalse(keys.contains("aggregate_score") || keys.contains("overall_score"),
                "no aggregate score may hide a failed dimension");
        for (String key : keys)
            assertFalse(key.toLowerCase().contains("time") && !key.equals("history_cutoff"),
                    "comparison report must not contain wall-clock fields: " + key);
        assertEquals("EVALUATOR_COMPARISON_ONLY", parsed.get("authority").asText());
        assertFalse(parsed.get("semantic_publication_allowed").asBoolean());
        assertEquals("1", parsed.get("schema_version").asText());
        assertTrue(parsed.get("limitations").size() >= 4);
    }

    /**
     * Writes the artifact when absent; when present, asserts the regenerated
     * bytes are identical so the committed artifact stays immutable and
     * byte-reproducible.
     */
    private static void maintainArtifact(Path relative, byte[] regenerated) throws Exception {
        Path absolute = Path.of("").toAbsolutePath().resolve(relative);
        if (Files.exists(absolute)) {
            assertArrayEquals(Files.readAllBytes(absolute), regenerated,
                    "committed artifact drifted from regenerated bytes: " + relative);
            return;
        }
        Files.createDirectories(absolute.getParent());
        Files.write(absolute, regenerated);
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
}

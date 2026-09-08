package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.ScenarioGroundedForwardMapper.ComponentRole;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SliceGEvaluatorComparisonTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String REV = "818c4136ea971c21674525f9053de0d9c7ad8cfe", SHA = "a".repeat(64);
    @TempDir Path temp;

    @Test void exactByteSealingPrecedesAnyEvaluatorAccess() throws Exception {
        copyTree(Path.of("."), temp);
        Path proposal = temp.resolve(SliceGEvaluatorComparison.PROPOSAL_PATH);
        Files.writeString(proposal, "\n", StandardOpenOption.APPEND);
        AtomicInteger accesses = new AtomicInteger();

        assertThrows(RuntimeContractException.class,
                () -> SliceGEvaluatorComparison.compare(temp, root -> {
                    accesses.incrementAndGet();
                    return SliceGEvaluatorComparison.loadEvaluatorTruth(root);
                }));
        assertEquals(0, accesses.get());
    }

    @Test void reportsEmptyProposalWithoutInventingFormalCredit() {
        var report = SliceGEvaluatorComparison.compare(Path.of("."),
                SliceGEvaluatorComparison::loadEvaluatorTruth);

        assertEquals("EVALUATOR_ONLY", report.authority());
        assertFalse(report.semanticPublicationAllowed());
        assertEquals(10, report.counts().scenarios());
        assertEquals(0, report.counts().mappingProposals());
        assertEquals(10, report.counts().unresolvedScenarios());
        assertEquals(0, report.counts().proposedComponents());
        assertEquals(24, report.exactComponent().expected());
        assertEquals(0, report.exactComponent().matched());
        assertEquals(0.0, report.exactComponent().recall());
        assertFalse(report.exactComponent().precisionDefined());
        assertNull(report.exactComponent().precision());
        assertEquals(0, report.directSymbolRecall().matched());
        assertEquals(0, report.expandedChainCoverage().matched());
        assertEquals(0, report.scenarioTraceCoverage().matched());
        assertEquals(891, report.unresolvedReferences().unresolved());
        assertEquals(1035, report.unresolvedReferences().total());
        assertEquals(0, report.traceCounts().direct());
        assertEquals(0, report.traceCounts().inferred());
        assertEquals(17, report.previousBaseline().graphNodeCovered());
        assertEquals(24, report.previousBaseline().graphNodeExpected());
    }

    @Test void evaluatorSealOrGoldMutationFailsClosed() throws Exception {
        for (String path : new String[]{SliceGEvaluatorComparison.GOLD_PATH, SliceGEvaluatorComparison.GOLD_SEAL_PATH,
                com.featuredeliveryintelligence.fdi.product.realization.evaluation.ProviderNeutralEvaluatorTruth.LEGACY_GOLD_PATH,
                com.featuredeliveryintelligence.fdi.product.realization.evaluation.ProviderNeutralEvaluatorTruth.LEGACY_SEAL_PATH}) {
            Path root = temp.resolve(Integer.toHexString(path.hashCode()));
            copyTree(Path.of("."), root);
            Files.writeString(root.resolve(path), "\n", StandardOpenOption.APPEND);
            assertThrows(RuntimeContractException.class,
                    () -> SliceGEvaluatorComparison.compare(root, SliceGEvaluatorComparison::loadEvaluatorTruth));
        }
    }

    @Test void reportSerializationIsDeterministic() throws Exception {
        var report = SliceGEvaluatorComparison.compare(Path.of("."), SliceGEvaluatorComparison::loadEvaluatorTruth);
        assertArrayEquals(SliceGEvaluatorComparison.toJson(report), SliceGEvaluatorComparison.toJson(report));
    }

    @Test void actualV04ShapeComputesDistinctFormalMetricsAndDeduplicates() {
        var controller = identity("src/main/java/example/OwnerController.java", "example.OwnerController.find");
        var repository = identity("src/main/java/example/OwnerRepository.java", "example.OwnerRepository.find");
        var mapping = mapping(controller, repository);
        ObjectNode first = scenario("S-1", mapping, controller, repository);
        ObjectNode second = first.deepCopy();
        second.with("mapping").put("scenarioId", "S-2");
        ObjectNode proposal = JSON.createObjectNode();
        ArrayNode scenarios = proposal.putArray("capabilities").addObject().putArray("scenarios");
        scenarios.add(first); scenarios.add(second);
        var expected = List.of(key(controller), key(repository),
                new SliceGEvaluatorComparison.Identity(REV, "src/main/java/example/Missing.java", "METHOD", "example.Missing#run"));

        var report = SliceGEvaluatorComparison.compute(proposal,
                new SliceGEvaluatorComparison.EvaluatorTruth("b".repeat(64), expected), "c".repeat(64));

        assertMetric(report.directSymbolRecall(), 1, 3, 1, true);
        assertMetric(report.expandedChainCoverage(), 2, 3, 2, true);
        assertMetric(report.exactComponent(), 1, 3, 1, true);
        assertMetric(report.scenarioTraceCoverage(), 2, 2, 2, true);
        assertEquals(1, report.counts().proposedComponents());
        assertEquals(2, report.traceCounts().direct());
        assertEquals(2, report.traceCounts().inferred());
    }

    @Test void supportingRoleAndProviderDiagnosticsNeverReceiveFormalCredit() {
        var expected = identity("src/main/java/example/OwnerController.java", "example.OwnerController.find");
        ObjectNode proposal = JSON.createObjectNode();
        ObjectNode scenario = proposal.putArray("capabilities").addObject().putArray("scenarios").addObject();
        ObjectNode mapping = scenario.putObject("mapping");
        mapping.put("scenarioId", "S-1").put("outcome", "UNRESOLVED");
        mapping.putArray("directProductionSymbols"); mapping.putArray("realizationChain");
        ObjectNode supporting = scenario.putArray("componentRoles").addObject();
        supporting.put("role", "SUPPORTING").put("providerNodeId", "diagnostic-that-looks-matching");
        supporting.set("component", JSON.valueToTree(expected));

        var report = SliceGEvaluatorComparison.compute(proposal,
                new SliceGEvaluatorComparison.EvaluatorTruth("b".repeat(64), List.of(key(expected))), "c".repeat(64));
        assertMetric(report.exactComponent(), 0, 1, 0, false);
        assertEquals(0, report.providerNativeDiagnostics().formalComponentCredit());
    }

    @Test void realV2StoredMethodIdentityMatchesV04HashQualifiedSymbol() {
        var expected = new SliceGEvaluatorComparison.Identity(REV,
                "src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java", "METHOD",
                "org.springframework.samples.petclinic.owner.OwnerController#processFindForm");
        var direct = identity("src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java",
                "org.springframework.samples.petclinic.owner.OwnerController#processFindForm");
        ObjectNode proposal = JSON.createObjectNode();
        proposal.putArray("capabilities").addObject().putArray("scenarios")
                .add(scenario("S-1", directOnlyMapping(direct), direct, direct));

        var report = SliceGEvaluatorComparison.compute(proposal,
                new SliceGEvaluatorComparison.EvaluatorTruth("b".repeat(64), List.of(expected)), "c".repeat(64));

        assertMetric(report.directSymbolRecall(), 1, 1, 1, true);
        assertMetric(report.exactComponent(), 1, 1, 1, true);
    }

    @Test void committedArtifactsReproduceByteExactly() throws Exception {
        Path output = temp.resolve("generated");
        SliceGEvaluatorComparisonGenerator.generate(Path.of("."), output);
        assertArrayEquals(Files.readAllBytes(Path.of(SliceGEvaluatorComparisonGenerator.REPORT_PATH)),
                Files.readAllBytes(output.resolve(SliceGEvaluatorComparisonGenerator.REPORT_PATH)));
        assertArrayEquals(Files.readAllBytes(Path.of(SliceGEvaluatorComparisonGenerator.EVIDENCE_PATH)),
                Files.readAllBytes(output.resolve(SliceGEvaluatorComparisonGenerator.EVIDENCE_PATH)));
    }

    private static void copyTree(Path source, Path target) throws Exception {
        for (String path : SliceGEvaluatorComparison.preEvaluatorPathsForTest()) {
            Path from = source.resolve(path), to = target.resolve(path);
            Files.createDirectories(to.getParent()); Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
        for (String path : new String[]{SliceGEvaluatorComparison.GOLD_PATH, SliceGEvaluatorComparison.GOLD_SEAL_PATH,
                com.featuredeliveryintelligence.fdi.product.realization.evaluation.ProviderNeutralEvaluatorTruth.LEGACY_GOLD_PATH,
                com.featuredeliveryintelligence.fdi.product.realization.evaluation.ProviderNeutralEvaluatorTruth.LEGACY_SEAL_PATH}) {
            Path from = source.resolve(path), to = target.resolve(path);
            Files.createDirectories(to.getParent()); Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static ScenarioMappingContractV04 mapping(ScenarioMappingContractV04.ComponentIdentity directIdentity,
                                                       ScenarioMappingContractV04.ComponentIdentity inferredIdentity) {
        var direct = new ScenarioMappingContractV04.DirectProductionSymbolEvidence("direct-1", "observation-1", directIdentity);
        var seed = new ScenarioMappingContractV04.SeedProvenance("seed-1", "direct-1", directIdentity);
        var edge = new ScenarioMappingContractV04.RelationshipEdge(1, directIdentity, inferredIdentity, "CALLS", "edge-1");
        var trace = new ScenarioMappingContractV04.RelationshipTrace("trace-1", REV, SHA, List.of(edge));
        return new ScenarioMappingContractV04(ScenarioMappingContractV04.SCHEMA_VERSION,
                ScenarioMappingContractV04.AUTHORITY, "C-1", "S-1", REV, SHA, SHA,
                ScenarioMappingContractV04.Outcome.MAPPING_PROPOSAL, ScenarioMappingContractV04.EvidenceStatus.COMPLETE,
                List.of(direct), List.of(seed), List.of(trace),
                List.of(new ScenarioMappingContractV04.RealizationChainStep(1, directIdentity,
                                ScenarioMappingContractV04.RelationshipBasis.DIRECT_TEST_REFERENCE, "seed-1", List.of("direct-1"), null),
                        new ScenarioMappingContractV04.RealizationChainStep(2, inferredIdentity,
                                ScenarioMappingContractV04.RelationshipBasis.GRAPHIFY_INFERRED, "seed-1", List.of(), "trace-1")),
                List.of(), List.of("bounded"));
    }

    private static ScenarioMappingContractV04 directOnlyMapping(ScenarioMappingContractV04.ComponentIdentity identity) {
        var direct = new ScenarioMappingContractV04.DirectProductionSymbolEvidence("direct-1", "observation-1", identity);
        var seed = new ScenarioMappingContractV04.SeedProvenance("seed-1", "direct-1", identity);
        return new ScenarioMappingContractV04(ScenarioMappingContractV04.SCHEMA_VERSION,
                ScenarioMappingContractV04.AUTHORITY, "C-1", "S-1", REV, SHA, SHA,
                ScenarioMappingContractV04.Outcome.MAPPING_PROPOSAL, ScenarioMappingContractV04.EvidenceStatus.COMPLETE,
                List.of(direct), List.of(seed), List.of(),
                List.of(new ScenarioMappingContractV04.RealizationChainStep(1, identity,
                        ScenarioMappingContractV04.RelationshipBasis.DIRECT_TEST_REFERENCE,
                        "seed-1", List.of("direct-1"), null)), List.of(), List.of("bounded"));
    }

    private static ObjectNode scenario(String id, ScenarioMappingContractV04 mapping,
                                       ScenarioMappingContractV04.ComponentIdentity primary,
                                       ScenarioMappingContractV04.ComponentIdentity supporting) {
        ObjectNode scenario = JSON.createObjectNode();
        scenario.set("mapping", JSON.valueToTree(mapping));
        scenario.with("mapping").put("scenarioId", id);
        ArrayNode roles = scenario.putArray("componentRoles");
        roles.add(JSON.valueToTree(new ComponentRole(primary, "PRIMARY")));
        roles.add(JSON.valueToTree(new ComponentRole(supporting, "SUPPORTING")));
        return scenario;
    }

    private static ScenarioMappingContractV04.ComponentIdentity identity(String path, String symbol) {
        return new ScenarioMappingContractV04.ComponentIdentity(REV, path, ScenarioMappingContractV04.Granularity.METHOD, symbol);
    }

    private static SliceGEvaluatorComparison.Identity key(ScenarioMappingContractV04.ComponentIdentity identity) {
        return new SliceGEvaluatorComparison.Identity(identity.sourceRevision(), identity.sourcePath(), identity.granularity().name(), identity.qualifiedSymbol());
    }

    private static void assertMetric(SliceGEvaluatorComparison.Metric metric, int matched, int expected,
                                     int proposed, boolean precisionDefined) {
        assertEquals(matched, metric.matched()); assertEquals(expected, metric.expected());
        assertEquals(proposed, metric.proposed()); assertEquals(precisionDefined, metric.precisionDefined());
    }
}

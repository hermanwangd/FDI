package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.ComponentIdentity;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.Granularity;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.RelationshipEdge;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.RelationshipTrace;
import com.featuredeliveryintelligence.fdi.product.realization.directtrace.SfBl002TestBehaviorEvidence;
import com.featuredeliveryintelligence.fdi.product.realization.graphexpansion.GraphifyProductionExpansion;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.SfBl002ScenarioMappingRun.Seal;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SfBl002ScenarioMappingRunTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String REV = SfBl002TestBehaviorEvidence.SOURCE_REVISION;
    private static final String AUTH = "d5aaa1d485f585c3b2b4162263f7e4dcc95033f11b7c4e6ad5b077b9da7a889e";
    @TempDir Path temp;

    @Test void frozenRunWritesOnlyProposalAndNonEvaluatorEvidenceManifest() throws Exception {
        Path output = temp.resolve("out");
        var result = SfBl002ScenarioMappingRun.generate(Path.of("."), output);

        assertEquals(5, result.capabilityCount());
        assertEquals(0, result.mappedScenarios());
        assertEquals(10, result.unresolvedScenarios());
        assertEquals(result.proposalSha256(), sha(Files.readAllBytes(
                output.resolve(SfBl002ScenarioMappingRun.PROPOSAL_PATH))));

        JsonNode proposal = JSON.readTree(output.resolve(SfBl002ScenarioMappingRun.PROPOSAL_PATH).toFile());
        assertEquals(REV, proposal.path("sourceRevision").asText());
        assertFalse(proposal.path("semanticPublicationAllowed").asBoolean());
        assertEquals(SfBl002ScenarioMappingRun.SEMANTICS_SHA256, proposal.path("semanticsSha256").asText());
        assertEquals(AUTH, proposal.path("authorizationSha256").asText());
        assertEquals(SfBl002ScenarioMappingRun.TEST_EVIDENCE_SHA256, proposal.path("testEvidenceSha256").asText());
        assertEquals(SfBl002ScenarioMappingRun.GRAPH_SHA256, proposal.path("graphSha256").asText());
        assertEquals(5, proposal.path("capabilities").size());
        int scenarios = 0;
        for (JsonNode capability : proposal.path("capabilities")) {
            for (JsonNode scenario : capability.path("scenarios")) {
                scenarios++;
                JsonNode mapping = scenario.path("mapping");
                assertEquals("UNRESOLVED", mapping.path("outcome").asText());
                assertEquals("INSUFFICIENT", mapping.path("evidenceStatus").asText());
                assertEquals("PROPOSAL_ONLY", mapping.path("authority").asText());
                assertTrue(mapping.path("realizationChain").isEmpty());
                assertFalse(mapping.path("evidenceGaps").isEmpty());
                assertTrue(scenario.path("componentRoles").isEmpty());
            }
        }
        assertEquals(10, scenarios);
        assertFalse(proposal.path("observedDirectEvidenceRefs").isEmpty());

        JsonNode evidence = JSON.readTree(output.resolve(SfBl002ScenarioMappingRun.EVIDENCE_PATH).toFile());
        assertFalse(evidence.path("semantic_publication_allowed").asBoolean());
        assertFalse(evidence.path("evaluator_inputs_accessed").asBoolean());
        assertEquals(result.proposalSha256(), evidence.path("output").path("sha256").asText());
        for (String path : SfBl002ScenarioMappingRun.sealedInputs().keySet()) {
            assertEquals(SfBl002ScenarioMappingRun.sealedInputs().get(path),
                    evidence.path("inputs").path(path).asText(), path);
        }
    }

    @Test void doubleRunIntoSeparateRootsIsByteIdentical() throws Exception {
        Path first = temp.resolve("first"), second = temp.resolve("second");
        var one = SfBl002ScenarioMappingRun.generate(Path.of("."), first);
        var two = SfBl002ScenarioMappingRun.generate(Path.of("."), second);
        assertEquals(one.proposalSha256(), two.proposalSha256());
        assertEquals(one.evidenceSha256(), two.evidenceSha256());
        assertArrayEquals(Files.readAllBytes(first.resolve(SfBl002ScenarioMappingRun.PROPOSAL_PATH)),
                Files.readAllBytes(second.resolve(SfBl002ScenarioMappingRun.PROPOSAL_PATH)));
        assertArrayEquals(Files.readAllBytes(first.resolve(SfBl002ScenarioMappingRun.EVIDENCE_PATH)),
                Files.readAllBytes(second.resolve(SfBl002ScenarioMappingRun.EVIDENCE_PATH)));
    }

    @Test void existingIdenticalOutputsAreReusedButChangedBytesFailClosed() throws Exception {
        Path output = temp.resolve("out");
        var first = SfBl002ScenarioMappingRun.generate(Path.of("."), output);
        var second = SfBl002ScenarioMappingRun.generate(Path.of("."), output);
        assertEquals(first.proposalSha256(), second.proposalSha256());
        Files.writeString(output.resolve(SfBl002ScenarioMappingRun.PROPOSAL_PATH), "\n", StandardOpenOption.APPEND);
        assertThrows(RuntimeContractException.class, () -> SfBl002ScenarioMappingRun.generate(Path.of("."), output));
    }

    @Test void sealedInputDigestMismatchFailsClosedBeforeComposition() throws Exception {
        Path root = temp.resolve("root");
        copySealedInputs(root);
        Files.writeString(root.resolve(SfBl002ScenarioMappingRun.ASSIGNMENTS_PATH),
                "\n", StandardOpenOption.APPEND);
        assertThrows(RuntimeContractException.class,
                () -> SfBl002ScenarioMappingRun.generate(root, temp.resolve("out")));
    }

    @Test void unknownAssignmentEvidenceFailsClosed() throws Exception {
        ObjectNode document = assignmentsDocument();
        record(document, 0).withArray("directEvidenceRefs").add("direct-test-reference:9999");
        var thrown = assertThrows(RuntimeContractException.class,
                () -> SfBl002ScenarioMappingRun.compose(seal(), document, loaded(), emptyExpansion()));
        assertTrue(thrown.getMessage().contains("unknown direct evidence"), thrown.getMessage());
    }

    @Test void mappedAndUnresolvedScenariosComposePrimaryOnlyFromDirectEvidence() throws Exception {
        ObjectNode document = assignmentsDocument();
        record(document, 0).withArray("directEvidenceRefs").add("direct-test-reference:0001");
        record(document, 1).withArray("gapRefs").add(loaded().gaps().get(0).observationRef());

        var composed = SfBl002ScenarioMappingRun.compose(seal(), document, loaded(), emptyExpansion());

        var first = find(composed, "HYP-SCENARIO-001");
        assertEquals("MAPPING_PROPOSAL", first.mapping().outcome().name());
        assertEquals("COMPLETE", first.mapping().evidenceStatus().name());
        assertEquals(1, first.mapping().realizationChain().size());
        assertEquals("PRIMARY", first.componentRoles().get(0).role());
        var second = find(composed, "HYP-SCENARIO-002");
        assertEquals("UNRESOLVED", second.mapping().outcome().name());
        assertFalse(second.mapping().evidenceGaps().isEmpty());
    }

    @Test void graphifyOutputBecomesSupportingOnlyThroughBoundRelationshipTrace() throws Exception {
        ObjectNode document = assignmentsDocument();
        record(document, 0).withArray("directEvidenceRefs").add("direct-test-reference:0001");
        var loaded = loaded();
        var provenance = loaded.observationsByRef().get("direct-test-reference:0001").seed();
        var seed = new GraphifyProductionExpansion.ProductionSeed(
                provenance.seedRef(), provenance.productionSeed(), "node-seed");
        var neighbour = new ComponentIdentity(REV,
                "src/main/java/org/springframework/samples/petclinic/vet/Vet.java",
                Granularity.METHOD, "org.springframework.samples.petclinic.vet.Vet#getSpecialties");
        var edge = new RelationshipEdge(1, seed.identity(), neighbour, "CALLS", "graph-edge-1");
        var trace = new RelationshipTrace("trace-1", REV, SfBl002ScenarioMappingRun.GRAPH_SHA256, List.of(edge));
        var expansionTrace = new GraphifyProductionExpansion.ExpandedSeedTrace(seed, "node-seed",
                List.of(new GraphifyProductionExpansion.InferredNeighbour(neighbour, "node-neighbour", trace)));
        var expansion = new GraphifyProductionExpansion.Result(REV, SfBl002ScenarioMappingRun.GRAPH_SHA256,
                SfBl002ScenarioMappingRun.EXPANSION_BOUNDS, List.of(expansionTrace));

        var composed = SfBl002ScenarioMappingRun.compose(seal(), document, loaded, expansion);

        var proposal = find(composed, "HYP-SCENARIO-001");
        assertEquals(2, proposal.mapping().realizationChain().size());
        assertEquals("GRAPHIFY_INFERRED",
                proposal.mapping().realizationChain().get(1).relationshipBasis().name());
        assertEquals(1, proposal.mapping().relationshipTraces().size());
        assertEquals(List.of("PRIMARY", "SUPPORTING"),
                proposal.componentRoles().stream().map(ScenarioGroundedForwardMapper.ComponentRole::role).toList());
    }

    @Test void unboundGraphifyRelationshipTraceFailsClosed() throws Exception {
        ObjectNode document = assignmentsDocument();
        record(document, 0).withArray("directEvidenceRefs").add("direct-test-reference:0001");
        var loaded = loaded();
        var provenance = loaded.observationsByRef().get("direct-test-reference:0001").seed();
        var seed = new GraphifyProductionExpansion.ProductionSeed(
                provenance.seedRef(), provenance.productionSeed(), "node-seed");
        var neighbour = new ComponentIdentity(REV,
                "src/main/java/org/springframework/samples/petclinic/vet/Vet.java",
                Granularity.METHOD, "org.springframework.samples.petclinic.vet.Vet#getSpecialties");
        var other = new ComponentIdentity(REV,
                "src/main/java/org/springframework/samples/petclinic/owner/Owner.java",
                Granularity.METHOD, "org.springframework.samples.petclinic.owner.Owner#getPets");
        var edge = new RelationshipEdge(1, seed.identity(), other, "CALLS", "graph-edge-1");
        var trace = new RelationshipTrace("trace-1", REV, SfBl002ScenarioMappingRun.GRAPH_SHA256, List.of(edge));
        var expansionTrace = new GraphifyProductionExpansion.ExpandedSeedTrace(seed, "node-seed",
                List.of(new GraphifyProductionExpansion.InferredNeighbour(neighbour, "node-neighbour", trace)));
        var expansion = new GraphifyProductionExpansion.Result(REV, SfBl002ScenarioMappingRun.GRAPH_SHA256,
                SfBl002ScenarioMappingRun.EXPANSION_BOUNDS, List.of(expansionTrace));

        assertThrows(RuntimeContractException.class,
                () -> SfBl002ScenarioMappingRun.compose(seal(), document, loaded, expansion));
    }

    @Test void evaluatorVocabularyAndPublicationFlagsFailClosed() throws Exception {
        ObjectNode document = assignmentsDocument();
        record(document, 0).put("scenarioId", "evaluator-gold-scenario");
        assertThrows(RuntimeContractException.class,
                () -> SfBl002ScenarioMappingRun.parseAndValidateAssignments(
                        JSON.writeValueAsBytes(document), seal(), frozen()));
        ObjectNode published = assignmentsDocument();
        published.put("semantic_publication_allowed", true);
        var thrown = assertThrows(RuntimeContractException.class,
                () -> SfBl002ScenarioMappingRun.parseAndValidateAssignments(
                        JSON.writeValueAsBytes(published), seal(), frozen()));
        assertTrue(thrown.getMessage().contains("semantic publication"), thrown.getMessage());
    }

    @Test void duplicateUnknownOrIncompleteScenarioCoverageFailsClosed() throws Exception {
        ObjectNode document = assignmentsDocument();
        ArrayNode records = document.withArray("assignments");
        records.add(records.get(0).deepCopy());
        assertThrows(RuntimeContractException.class,
                () -> SfBl002ScenarioMappingRun.parseAndValidateAssignments(
                        JSON.writeValueAsBytes(document), seal(), frozen()));

        ObjectNode unknown = assignmentsDocument();
        record(unknown, 0).put("scenarioId", "HYP-SCENARIO-UNKNOWN");
        assertThrows(RuntimeContractException.class,
                () -> SfBl002ScenarioMappingRun.parseAndValidateAssignments(
                        JSON.writeValueAsBytes(unknown), seal(), frozen()));

        ObjectNode incomplete = assignmentsDocument();
        incomplete.withArray("assignments").remove(0);
        var thrown = assertThrows(RuntimeContractException.class,
                () -> SfBl002ScenarioMappingRun.parseAndValidateAssignments(
                        JSON.writeValueAsBytes(incomplete), seal(), frozen()));
        assertTrue(thrown.getMessage().contains("exactly one record per frozen scenario"), thrown.getMessage());
    }

    @Test void mixedRevisionAssignmentRecordFailsClosedDuringComposition() throws Exception {
        ObjectNode document = assignmentsDocument();
        record(document, 0).put("sourceRevision", "1".repeat(40));
        assertThrows(RuntimeContractException.class,
                () -> SfBl002ScenarioMappingRun.compose(seal(), document, loaded(), emptyExpansion()));
    }

    private static ScenarioGroundedForwardMapper.ScenarioProposal find(
            ScenarioGroundedForwardMapper.Result composed, String scenarioId) {
        for (var capability : composed.capabilities()) for (var scenario : capability.scenarios()) {
            if (scenario.mapping().scenarioId().equals(scenarioId)) return scenario;
        }
        throw new AssertionError("scenario not found: " + scenarioId);
    }

    private static ObjectNode record(ObjectNode document, int index) {
        return (ObjectNode) document.withArray("assignments").get(index);
    }

    private static ObjectNode assignmentsDocument() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readAllBytes(Path.of(SfBl002ScenarioMappingRun.ASSIGNMENTS_PATH)));
    }

    private static Seal seal() {
        return new Seal(REV, SfBl002ScenarioMappingRun.SEMANTICS_SHA256, AUTH,
                SfBl002ScenarioMappingRun.TEST_EVIDENCE_SHA256, SfBl002ScenarioMappingRun.GRAPH_SHA256);
    }

    private static Set<List<String>> frozen() throws Exception {
        JsonNode semantics = JSON.readTree(Files.readAllBytes(Path.of(SfBl002ScenarioMappingRun.SEMANTICS_PATH)));
        return SfBl002ScenarioMappingRun.frozenScenarios(semantics);
    }

    private static SfBl002TestBehaviorEvidence.Loaded loaded() {
        return SfBl002TestBehaviorEvidence.load(Path.of("."), SfBl002ScenarioMappingRun.TEST_EVIDENCE_SHA256);
    }

    private static GraphifyProductionExpansion.Result emptyExpansion() {
        return new GraphifyProductionExpansion.Result(REV, SfBl002ScenarioMappingRun.GRAPH_SHA256,
                SfBl002ScenarioMappingRun.EXPANSION_BOUNDS, List.of());
    }

    private static String sha(byte[] bytes) { return ScenarioForwardRequestReader.sha256(bytes); }

    private static void copySealedInputs(Path root) throws Exception {
        Map<String, String> inputs = new java.util.LinkedHashMap<>(SfBl002ScenarioMappingRun.sealedInputs());
        for (String path : inputs.keySet()) {
            Path from = Path.of(".").resolve(path), to = root.resolve(path);
            Files.createDirectories(to.getParent());
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

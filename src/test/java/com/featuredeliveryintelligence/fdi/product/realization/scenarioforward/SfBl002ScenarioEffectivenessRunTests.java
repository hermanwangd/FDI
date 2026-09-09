package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.graphexpansion.GraphifyProductionExpansion.QueryBounds;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Slice B tests for the bounded Graphify mapping run. The frozen synthetic matcher fixture
 * (assignments-fixture-002.json) is shaped like slice A's scenario-observation-assignments-002
 * contract and is labelled synthetic test fixture, never production evidence. Sealed inputs are
 * read from the repository root; production proposal paths are exercised only under temp roots.
 */
class SfBl002ScenarioEffectivenessRunTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path FIXTURE = Path.of("src/test/resources/scenarioforward/sf-bl002/assignments-fixture-002.json");
    @TempDir Path temp;

    @Test void frozenRunMapsPrimarySeedsAndWritesProposalAndEvidence() throws Exception {
        Path output = temp.resolve("out");
        var result = generateWithFixture(output);

        assertEquals(10, result.scenarioCount());
        assertEquals(9, result.mappedScenarios());
        assertEquals(1, result.unresolvedScenarios());

        JsonNode proposal = read(output, SfBl002ScenarioEffectivenessRun.PROPOSAL_PATH);
        assertEquals("software-factory.sf-bl002-scenario-mapping-proposal.v0.2", proposal.path("schema_version").asText());
        assertEquals("SF-BL-002-SCENARIO-EFFECTIVENESS-004", proposal.path("execution_id").asText());
        assertEquals("PROPOSAL_ONLY", proposal.path("authority").asText());
        assertFalse(proposal.path("semantic_publication_allowed").asBoolean());
        assertEquals(SfBl002ScenarioEffectivenessRun.SOURCE_REVISION, proposal.path("source_revision").asText());

        JsonNode inputs = proposal.path("inputs");
        for (Map.Entry<String, String> sealed : SfBl002ScenarioEffectivenessRun.sealedInputs().entrySet()) {
            assertEquals(sealed.getValue(), inputs.path(sealed.getKey()).asText(), sealed.getKey());
        }
        assertEquals(sha(Files.readAllBytes(FIXTURE)),
                inputs.path(SfBl002ScenarioEffectivenessRun.ASSIGNMENTS_PATH).asText());

        JsonNode mapped = findScenario(proposal, "HYP-SCENARIO-005");
        assertEquals("MAPPING_PROPOSAL", mapped.path("outcome").asText());
        JsonNode primary = mapped.path("primary");
        assertEquals("direct-test-reference:0004", primary.path("evidenceRef").asText());
        assertEquals("owner_owner_getpets", primary.path("providerNodeId").asText());
        assertEquals("src/main/java/org/springframework/samples/petclinic/owner/Owner.java",
                primary.path("productionSymbol").path("sourcePath").asText());
        assertEquals(SfBl002ScenarioEffectivenessRun.SOURCE_REVISION,
                primary.path("productionSymbol").path("sourceRevision").asText());

        JsonNode supporting = mapped.path("supporting");
        Set<String> reached = new TreeSet<>();
        for (JsonNode node : supporting) {
            reached.add(node.path("providerNodeId").asText());
            assertFalse(node.path("formalPrimaryPrecisionCredit").asBoolean(),
                    "SUPPORTING must receive zero formal PRIMARY precision credit");
            assertTraceStartsAtSeedAndUsesRealGraphEdges(node, "owner_owner_getpets");
        }
        assertEquals(Set.of("owner_owner_addpet", "owner_owner_getpet", "owner_owner_addvisit"), reached);
        JsonNode addVisit = supporting.get(2);
        assertEquals("owner_owner_addvisit", addVisit.path("providerNodeId").asText());
        assertEquals(2, addVisit.path("relationshipTrace").path("edges").size());

        JsonNode evidence = read(output, SfBl002ScenarioEffectivenessRun.EVIDENCE_PATH);
        assertEquals("PROPOSAL_ONLY", evidence.path("authority").asText());
        assertFalse(evidence.path("semantic_publication_allowed").asBoolean());
        assertFalse(evidence.path("evaluator_inputs_accessed").asBoolean());
        assertEquals(result.proposalSha256(), evidence.path("output").path("sha256").asText());
        JsonNode runtime = evidence.path("provider_runtime");
        assertEquals("graphifyy", runtime.path("runtime_identity").asText());
        assertEquals("0.1.14", runtime.path("runtime_version").asText());
        assertEquals("MCP stdio", runtime.path("transport").asText());
        assertEquals("pkb001-graphify-live-818c413", runtime.path("verification_id").asText());
        assertEquals("EXACTLY_BOUND", runtime.path("result").asText());
    }

    @Test void deterministicDoubleRunProducesIdenticalBytes() throws Exception {
        Path first = temp.resolve("first"), second = temp.resolve("second");
        generateWithFixture(first);
        generateWithFixture(second);
        assertArrayEquals(Files.readAllBytes(first.resolve(SfBl002ScenarioEffectivenessRun.PROPOSAL_PATH)),
                Files.readAllBytes(second.resolve(SfBl002ScenarioEffectivenessRun.PROPOSAL_PATH)));
        assertArrayEquals(Files.readAllBytes(first.resolve(SfBl002ScenarioEffectivenessRun.EVIDENCE_PATH)),
                Files.readAllBytes(second.resolve(SfBl002ScenarioEffectivenessRun.EVIDENCE_PATH)));
    }

    @Test void noSeedScenarioStaysUnresolvedWithoutPrimaryOrSupporting() throws Exception {
        Path output = temp.resolve("out");
        generateWithFixture(output);
        JsonNode proposal = read(output, SfBl002ScenarioEffectivenessRun.PROPOSAL_PATH);
        JsonNode unresolved = findScenario(proposal, "HYP-SCENARIO-002");
        assertEquals("UNRESOLVED", unresolved.path("outcome").asText());
        assertTrue(unresolved.path("primary").isNull());
        assertEquals(0, unresolved.path("supporting").size());
    }

    @Test void unboundNeighbourRejectedWhenDepthBoundIsExceeded() throws Exception {
        QueryBounds shallow = new QueryBounds(1, 50, 50, 25, 100_000, 30_000);
        var thrown = assertThrows(RuntimeContractException.class,
                () -> generateWithFixture(temp.resolve("out"), fixtureManifest(), shallow));
        assertTrue(thrown.getMessage().contains("depth"), thrown.getMessage());
    }

    @Test void assignmentInputValidationFailsClosed() throws Exception {
        assertAssignmentRejected("schema_version", "wrong.schema");
        assertAssignmentRejected("authority", "ACCEPTED");
        assertAssignmentRejected("source_revision", "1".repeat(40));
        assertAssignmentRejected("intent_sha256", "0".repeat(64));
        assertAssignmentRejected("test_evidence_sha256", "0".repeat(64));
        ObjectNode publication = fixtureDocument();
        publication.put("semantic_publication_allowed", true);
        assertRejected(publication, "publication");
    }

    @Test void duplicateUnknownAndMissingScenarioRecordsFailClosed() throws Exception {
        ObjectNode document = fixtureDocument();
        ObjectNode clone = (ObjectNode) document.withArray("assignments").get(0).deepCopy();
        document.withArray("assignments").add(clone);
        assertRejected(document, "duplicate");

        ObjectNode unknown = fixtureDocument();
        ((ObjectNode) unknown.withArray("assignments").get(0)).put("scenarioId", "HYP-SCENARIO-999");
        assertRejected(unknown, null);

        ObjectNode missing = fixtureDocument();
        unknownMissing(missing);
        assertRejected(missing, null);
    }

    @Test void unknownPrimaryEvidenceRefFailsClosed() throws Exception {
        ObjectNode document = fixtureDocument();
        ((ObjectNode) document.withArray("assignments").get(0)).put("primaryEvidenceRef", "direct-test-reference:9999");
        assertRejected(document, "unknown");
    }

    @Test void evaluatorVocabularyInAssignmentFailsClosed() throws Exception {
        ObjectNode document = fixtureDocument();
        ((ObjectNode) document.withArray("assignments").get(0))
                .put("selectionRationale", "derived from evaluator gold mapping");
        assertRejected(document, "evaluator");
    }

    @Test void assignmentsManifestDigestMismatchFailsClosed() throws Exception {
        ObjectNode manifest = fixtureManifest();
        ((ObjectNode) manifest.path("artifact")).put("sha256", "0".repeat(64));
        var thrown = assertThrows(RuntimeContractException.class,
                () -> generateWithFixture(temp.resolve("out"), manifest, SfBl002ScenarioEffectivenessRun.EXPANSION_BOUNDS));
        assertTrue(thrown.getMessage().contains("manifest"), thrown.getMessage());
    }

    @Test void sealedInputDigestMismatchFailsClosedBeforeComposition() throws Exception {
        Path root = temp.resolve("root");
        copySealedInputs(root);
        Files.writeString(root.resolve(SfBl002ScenarioEffectivenessRun.INTENTS_PATH),
                "\n", StandardOpenOption.APPEND);
        var thrown = assertThrows(RuntimeContractException.class,
                () -> SfBl002ScenarioEffectivenessRun.generate(root, temp.resolve("out"), FIXTURE,
                        writeManifest(fixtureManifest()), SfBl002ScenarioEffectivenessRun.EXPANSION_BOUNDS));
        assertTrue(thrown.getMessage().contains("digest"), thrown.getMessage());
    }

    @Test void tamperedRuntimeEvidenceFailsClosed() throws Exception {
        Path root = temp.resolve("root");
        copySealedInputs(root);
        Files.writeString(root.resolve(SfBl002ScenarioEffectivenessRun.RUNTIME_EVIDENCE_PATH),
                "\n", StandardOpenOption.APPEND);
        assertThrows(RuntimeContractException.class,
                () -> SfBl002ScenarioEffectivenessRun.generate(root, temp.resolve("out"), FIXTURE,
                        writeManifest(fixtureManifest()), SfBl002ScenarioEffectivenessRun.EXPANSION_BOUNDS));
    }

    @Test void collisionWithChangedExistingBytesFailsClosed() throws Exception {
        Path output = temp.resolve("out");
        generateWithFixture(output);
        Files.writeString(output.resolve(SfBl002ScenarioEffectivenessRun.PROPOSAL_PATH), "{}");
        var thrown = assertThrows(RuntimeContractException.class, () -> generateWithFixture(output));
        assertTrue(thrown.getMessage().contains("collision"), thrown.getMessage());
    }

    @Test void idempotentRerunWithIdenticalBytesIsAccepted() throws Exception {
        Path output = temp.resolve("out");
        var first = generateWithFixture(output);
        var second = generateWithFixture(output);
        assertEquals(first.proposalSha256(), second.proposalSha256());
    }

    private void assertAssignmentRejected(String field, String value) throws Exception {
        ObjectNode document = fixtureDocument();
        document.put(field, value);
        assertRejected(document, null);
    }

    private void assertRejected(ObjectNode document, String messagePart) throws Exception {
        Path tampered = temp.resolve("tampered-" + Math.abs(document.hashCode()) + ".json");
        Files.createDirectories(tampered.getParent());
        Files.write(tampered, JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(document));
        var thrown = assertThrows(RuntimeContractException.class,
                () -> SfBl002ScenarioEffectivenessRun.generate(Path.of("."), temp.resolve("out-" + tampered.getFileName()),
                        tampered, writeManifest(manifestFor(tampered)),
                        SfBl002ScenarioEffectivenessRun.EXPANSION_BOUNDS));
        if (messagePart != null) assertTrue(thrown.getMessage().contains(messagePart), thrown.getMessage());
    }

    private void assertTraceStartsAtSeedAndUsesRealGraphEdges(JsonNode supporting, String seedNodeId) throws Exception {
        JsonNode edges = supporting.path("relationshipTrace").path("edges");
        assertFalse(edges.isEmpty());
        assertEquals(seedNodeId, edges.get(0).path("from").asText());
        Set<String> graphLinks = new TreeSet<>();
        JsonNode graph = JSON.readTree(Path.of(SfBl002ScenarioEffectivenessRun.GRAPH_PATH).toFile());
        for (JsonNode link : graph.path("links")) {
            graphLinks.add(link.path("source").asText() + "|" + link.path("target").asText()
                    + "|" + link.path("relation").asText());
        }
        String previous = seedNodeId;
        for (JsonNode edge : edges) {
            assertEquals(previous, edge.path("from").asText());
            assertTrue(graphLinks.contains(edge.path("from").asText() + "|" + edge.path("to").asText()
                    + "|" + edge.path("relationship").asText()), "invented edge: " + edge);
            previous = edge.path("to").asText();
        }
        assertEquals(previous, supporting.path("providerNodeId").asText());
    }

    private SfBl002ScenarioEffectivenessRun.Result generateWithFixture(Path output) throws Exception {
        return generateWithFixture(output, fixtureManifest(), SfBl002ScenarioEffectivenessRun.EXPANSION_BOUNDS);
    }

    private SfBl002ScenarioEffectivenessRun.Result generateWithFixture(Path output, ObjectNode manifest,
            QueryBounds bounds) throws Exception {
        return SfBl002ScenarioEffectivenessRun.generate(Path.of("."), output, FIXTURE, writeManifest(manifest), bounds);
    }

    private ObjectNode fixtureDocument() throws Exception {
        return (ObjectNode) JSON.readTree(Files.readAllBytes(FIXTURE));
    }

    private ObjectNode fixtureManifest() throws Exception {
        return manifestFor(FIXTURE);
    }

    private ObjectNode manifestFor(Path assignments) throws Exception {
        ObjectNode manifest = JSON.createObjectNode();
        manifest.put("schema_version", "software-factory.sf-bl002-artifact-manifest.v0.2");
        manifest.put("execution_id", "SF-BL-002-SCENARIO-EFFECTIVENESS-004");
        manifest.putObject("artifact")
                .put("path", SfBl002ScenarioEffectivenessRun.ASSIGNMENTS_PATH)
                .put("sha256", sha(Files.readAllBytes(assignments)));
        return manifest;
    }

    private Path writeManifest(ObjectNode manifest) throws Exception {
        Path path = temp.resolve("manifest-" + Math.abs(manifest.hashCode()) + ".json");
        Files.write(path, JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest));
        return path;
    }

    private static void unknownMissing(ObjectNode document) {
        List<JsonNode> kept = new ArrayList<>();
        document.withArray("assignments").forEach(node -> {
            if (!"HYP-SCENARIO-009".equals(node.path("scenarioId").asText())) kept.add(node);
        });
        document.remove("assignments");
        document.putArray("assignments").addAll(kept);
    }

    private static JsonNode findScenario(JsonNode proposal, String scenarioId) {
        for (JsonNode scenario : proposal.path("scenarios")) {
            if (scenarioId.equals(scenario.path("scenarioId").asText())) return scenario;
        }
        throw new AssertionError("scenario not found: " + scenarioId);
    }

    private static JsonNode read(Path output, String relative) throws Exception {
        return JSON.readTree(output.resolve(relative).toFile());
    }

    private static String sha(byte[] bytes) { return ScenarioForwardRequestReader.sha256(bytes); }

    private static void copySealedInputs(Path root) throws Exception {
        Map<String, String> inputs = new LinkedHashMap<>(SfBl002ScenarioEffectivenessRun.sealedInputs());
        for (String path : inputs.keySet()) {
            Path from = Path.of(".").resolve(path), to = root.resolve(path);
            Files.createDirectories(to.getParent());
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

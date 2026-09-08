package com.featuredeliveryintelligence.fdi.product.realization;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.*;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

class ScenarioMappingSchemaV04Tests {
    private static final ObjectMapper JSON = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    private static final JsonSchema SCHEMA;
    static { try { SCHEMA = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(
            JSON.readTree(Path.of("validation/pkb001/schemas/realization-mapping-v0.4.schema.json").toFile()));
    } catch (Exception failure) { throw new ExceptionInInitializerError(failure); } }

    @Test void javaJsonSchemaJavaRoundTripUsesActualRecord() throws Exception {
        var original = ScenarioMappingContractV04Tests.mapping();
        byte[] bytes = JSON.writeValueAsBytes(original); JsonNode tree = JSON.readTree(bytes);
        assertTrue(SCHEMA.validate(tree).isEmpty(), SCHEMA.validate(tree).toString());
        assertEquals(original, JSON.readValue(bytes, ScenarioMappingContractV04.class));
        assertEquals(ScenarioMappingContractV04.SCHEMA_VERSION, tree.path("schema_version").asText());
        assertEquals(ScenarioMappingContractV04.AUTHORITY, tree.path("authority").asText());
        assertEquals(ScenarioMappingContractV04Tests.SHA, tree.path("frozen_semantics_sha256").asText());
    }

    @Test void dotPrefixedRepositoryPathIsAcceptedByBothLayers() throws Exception {
        var id = new ScenarioMappingContractV04.ComponentIdentity(ScenarioMappingContractV04Tests.REV,
                ".github/workflows/build.yml", ScenarioMappingContractV04.Granularity.CONFIGURATION,
                "github.workflow.build");
        var direct = new ScenarioMappingContractV04.DirectProductionSymbolEvidence("direct-1", "observation-1", id);
        var seed = new ScenarioMappingContractV04.SeedProvenance("seed-1", "direct-1", id);
        var step = new ScenarioMappingContractV04.RealizationChainStep(1, id,
                ScenarioMappingContractV04.RelationshipBasis.DIRECT_TEST_REFERENCE, "seed-1", List.of("direct-1"), null);
        var mapping = new ScenarioMappingContractV04(ScenarioMappingContractV04.SCHEMA_VERSION,
                ScenarioMappingContractV04.AUTHORITY, "HYP-CAPABILITY-001", "HYP-SCENARIO-001",
                ScenarioMappingContractV04Tests.REV, ScenarioMappingContractV04Tests.SHA,
                ScenarioMappingContractV04Tests.SHA, ScenarioMappingContractV04.Outcome.MAPPING_PROPOSAL,
                ScenarioMappingContractV04.EvidenceStatus.COMPLETE, List.of(direct), List.of(seed), List.of(),
                List.of(step), List.of(), List.of("bounded"));
        ObjectNode value = JSON.valueToTree(mapping);
        assertTrue(SCHEMA.validate(value).isEmpty(), SCHEMA.validate(value).toString());
        assertTrue(acceptsJava(value));
    }

    @Test void sharedPathAndLeakageMutationsAreRejectedByBothLayers() {
        for (String path : ScenarioMappingContractV04Tests.badPaths()) {
            ObjectNode value = tree(); identity(value).put("source_path", path); assertBothReject(value, path);
        }
        for (String forbidden : ScenarioMappingContractV04Tests.forbiddenRefs()) {
            for (Consumer<ObjectNode> mutation : leakageMutations(forbidden)) {
                ObjectNode value = tree(); mutation.accept(value); assertBothReject(value, forbidden);
            }
        }
    }

    @Test void schemaAndJavaRejectUnsupportedBasisAndTraceBindingFailures() {
        ObjectNode basis = tree(); ((ObjectNode)basis.withArray("realization_chain").get(1)).put("relationship_basis", "SEMANTIC_GUESS");
        assertBothReject(basis, "basis");
        ObjectNode missing = tree(); missing.remove("relationship_traces"); assertBothReject(missing, "traces");
        ObjectNode wrong = tree(); ((ObjectNode)wrong.withArray("relationship_traces").get(0)).put("graph_sha256", "b".repeat(64));
        assertFalse(acceptsJava(wrong), "Java must enforce cross-document graph binding");
    }

    private static ObjectNode tree() { return JSON.valueToTree(ScenarioMappingContractV04Tests.mapping()); }
    private static ObjectNode identity(ObjectNode value) { return (ObjectNode)value.withArray("direct_production_symbols").get(0).path("production_symbol"); }
    private static List<Consumer<ObjectNode>> leakageMutations(String value) {
        return List.of(
                root -> root.put("capability_id", value), root -> root.put("scenario_id", value),
                root -> ((ObjectNode)root.withArray("direct_production_symbols").get(0)).put("evidence_ref", value),
                root -> ((ObjectNode)root.withArray("direct_production_symbols").get(0)).put("observation_ref", value),
                root -> ((ObjectNode)root.withArray("seeds").get(0)).put("seed_ref", value),
                root -> ((ObjectNode)root.withArray("seeds").get(0)).put("direct_evidence_ref", value),
                root -> ((ObjectNode)root.withArray("relationship_traces").get(0)).put("trace_id", value),
                root -> ((ObjectNode)root.withArray("relationship_traces").get(0).withArray("edges").get(0)).put("relationship_type", value),
                root -> ((ObjectNode)root.withArray("relationship_traces").get(0).withArray("edges").get(0)).put("evidence_ref", value),
                root -> ((ObjectNode)root.withArray("realization_chain").get(0)).put("seed_ref", value),
                root -> ((com.fasterxml.jackson.databind.node.ArrayNode)root.withArray("realization_chain").get(0)
                        .get("evidence_refs")).set(0, JSON.getNodeFactory().textNode(value)),
                root -> ((ObjectNode)root.withArray("realization_chain").get(1)).put("relationship_trace_ref", value),
                root -> root.withArray("limitations").set(0, JSON.getNodeFactory().textNode(value)));
    }
    private static void assertBothReject(ObjectNode value, String label) {
        assertFalse(SCHEMA.validate(value).isEmpty(), "schema accepted " + label);
        assertFalse(acceptsJava(value), "Java accepted " + label);
    }
    private static boolean acceptsJava(JsonNode value) {
        try { JSON.treeToValue(value, ScenarioMappingContractV04.class); return true; } catch (Exception ignored) { return false; }
    }
}

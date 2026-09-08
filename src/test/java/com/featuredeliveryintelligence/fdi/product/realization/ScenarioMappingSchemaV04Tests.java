package com.featuredeliveryintelligence.fdi.product.realization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioMappingSchemaV04Tests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path SCHEMA = Path.of("validation/pkb001/schemas/realization-mapping-v0.4.schema.json");

    @Test
    void schemaAcceptsVersionedProviderNeutralMappingAndRejectsMutations() throws Exception {
        var schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(JSON.readTree(SCHEMA.toFile()));
        ObjectNode valid = (ObjectNode) JSON.readTree("""
                {"schema_version":"pkb001.realization-mapping.v0.4","authority":"PROPOSAL_ONLY",
                 "capability_id":"HYP-CAPABILITY-001","scenario_id":"HYP-SCENARIO-001",
                 "source_revision":"818c4136ea971c21674525f9053de0d9c7ad8cfe","frozen_semantics_sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                 "direct_production_symbols":[{"evidence_ref":"d1","observation_ref":"o1","production_symbol":{"source_revision":"818c4136ea971c21674525f9053de0d9c7ad8cfe","source_path":"src/main/java/example/Owner.java","granularity":"METHOD","qualified_symbol":"example.Owner.find"}}],
                 "seeds":[{"seed_ref":"s1","direct_evidence_ref":"d1","production_seed":{"source_revision":"818c4136ea971c21674525f9053de0d9c7ad8cfe","source_path":"src/main/java/example/Owner.java","granularity":"METHOD","qualified_symbol":"example.Owner.find"}}],
                 "realization_chain":[{"order":1,"component":{"source_revision":"818c4136ea971c21674525f9053de0d9c7ad8cfe","source_path":"src/main/java/example/Owner.java","granularity":"METHOD","qualified_symbol":"example.Owner.find"},"relationship_basis":"DIRECT_TEST_REFERENCE","seed_ref":"s1","trace_refs":["d1"]}],
                 "limitations":["bounded"]}
                """);
        assertTrue(schema.validate(valid).isEmpty());

        ObjectNode badBasis = valid.deepCopy();
        ((ObjectNode) badBasis.withArray("realization_chain").get(0)).put("relationship_basis", "SEMANTIC_GUESS");
        assertFalse(schema.validate(badBasis).isEmpty());
        ObjectNode testPath = valid.deepCopy();
        ((ObjectNode) testPath.withArray("direct_production_symbols").get(0).path("production_symbol"))
                .put("source_path", "src/test/java/example/OwnerTests.java");
        assertFalse(schema.validate(testPath).isEmpty());
        ObjectNode leakage = valid.deepCopy();
        ((ObjectNode) leakage.withArray("direct_production_symbols").get(0)).put("observation_ref", "evaluator-gold");
        assertFalse(schema.validate(leakage).isEmpty());
    }
}

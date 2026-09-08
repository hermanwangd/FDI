package com.featuredeliveryintelligence.fdi.product.realization;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScenarioMappingContractV04Tests {
    private static final String REV = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String SEMANTICS = "a".repeat(64);

    @Test
    void acceptsOrderedDirectAndGraphifyProductionChain() {
        var controller = identity("src/main/java/example/OwnerController.java", "example.OwnerController.find");
        var repository = identity("src/main/java/example/OwnerRepository.java", "example.OwnerRepository.findByLastName");
        var direct = new ScenarioMappingContractV04.DirectProductionSymbolEvidence(
                "direct-1", "scenario-observation-1", controller);
        var seed = new ScenarioMappingContractV04.SeedProvenance("seed-1", "direct-1", controller);
        var chain = List.of(
                new ScenarioMappingContractV04.RealizationChainStep(1, controller,
                        ScenarioMappingContractV04.RelationshipBasis.DIRECT_TEST_REFERENCE,
                        "seed-1", List.of("direct-1")),
                new ScenarioMappingContractV04.RealizationChainStep(2, repository,
                        ScenarioMappingContractV04.RelationshipBasis.GRAPHIFY_INFERRED,
                        "seed-1", List.of("graph-path-1")));

        var contract = new ScenarioMappingContractV04("HYP-CAPABILITY-001", "HYP-SCENARIO-001",
                REV, SEMANTICS, List.of(direct), List.of(seed), chain, List.of("bounded expansion"));

        assertEquals("pkb001.realization-mapping.v0.4", contract.schemaVersion());
        assertEquals("PROPOSAL_ONLY", contract.authority());
        assertEquals(2, contract.realizationChain().size());
    }

    @Test
    void rejectsTestComponentsMissingIdentityFieldsAndDuplicateIdentity() {
        assertThrows(RuntimeContractException.class,
                () -> identity("src/test/java/example/OwnerControllerTests.java", "example.OwnerControllerTests.find"));
        assertThrows(RuntimeContractException.class,
                () -> new ScenarioMappingContractV04.ComponentIdentity(REV, " ", ScenarioMappingContractV04.Granularity.METHOD, "example.Owner.find"));
        assertThrows(RuntimeContractException.class,
                () -> new ScenarioMappingContractV04.ComponentIdentity(REV, "src/main/java/example/Owner.java", null, "example.Owner.find"));
        assertThrows(RuntimeContractException.class,
                () -> new ScenarioMappingContractV04.ComponentIdentity(REV, "src/main/java/example/Owner.java", ScenarioMappingContractV04.Granularity.METHOD, " "));
        var id = identity("src/main/java/example/Owner.java", "example.Owner.find");
        var direct = new ScenarioMappingContractV04.DirectProductionSymbolEvidence("d1", "o1", id);
        assertThrows(RuntimeContractException.class, () -> valid(List.of(direct,
                new ScenarioMappingContractV04.DirectProductionSymbolEvidence("d2", "o2", id)),
                List.of(new ScenarioMappingContractV04.SeedProvenance("s1", "d1", id)),
                List.of(step(1, id, "s1", ScenarioMappingContractV04.RelationshipBasis.DIRECT_TEST_REFERENCE, "d1"))));
    }

    @Test
    void rejectsUnboundSeedUnsupportedBasisAndInferredLinkWithoutTrace() {
        var id = identity("src/main/java/example/Owner.java", "example.Owner.find");
        var direct = new ScenarioMappingContractV04.DirectProductionSymbolEvidence("d1", "o1", id);
        assertThrows(RuntimeContractException.class, () -> valid(List.of(direct),
                List.of(new ScenarioMappingContractV04.SeedProvenance("s1", "missing", id)),
                List.of(step(1, id, "s1", ScenarioMappingContractV04.RelationshipBasis.DIRECT_TEST_REFERENCE, "d1"))));
        assertThrows(IllegalArgumentException.class,
                () -> ScenarioMappingContractV04.RelationshipBasis.valueOf("SEMANTIC_GUESS"));
        assertThrows(RuntimeContractException.class, () -> valid(List.of(direct),
                List.of(new ScenarioMappingContractV04.SeedProvenance("s1", "d1", id)),
                List.of(step(1, id, "s1", ScenarioMappingContractV04.RelationshipBasis.GRAPHIFY_INFERRED))));
    }

    @Test
    void rejectsEvaluatorLeakageBrokenOrderingAndSeedIdentityMismatch() {
        var id = identity("src/main/java/example/Owner.java", "example.Owner.find");
        var other = identity("src/main/java/example/Vet.java", "example.Vet.find");
        var direct = new ScenarioMappingContractV04.DirectProductionSymbolEvidence("d1", "o1", id);
        assertThrows(RuntimeContractException.class,
                () -> new ScenarioMappingContractV04.DirectProductionSymbolEvidence("d1", "evaluator-gold/mapping-1", id));
        assertThrows(RuntimeContractException.class, () -> valid(List.of(direct),
                List.of(new ScenarioMappingContractV04.SeedProvenance("s1", "d1", other)),
                List.of(step(1, other, "s1", ScenarioMappingContractV04.RelationshipBasis.DIRECT_TEST_REFERENCE, "d1"))));
        assertThrows(RuntimeContractException.class, () -> valid(List.of(direct),
                List.of(new ScenarioMappingContractV04.SeedProvenance("s1", "d1", id)),
                List.of(step(2, id, "s1", ScenarioMappingContractV04.RelationshipBasis.DIRECT_TEST_REFERENCE, "d1"))));
    }

    private static ScenarioMappingContractV04 valid(
            List<ScenarioMappingContractV04.DirectProductionSymbolEvidence> direct,
            List<ScenarioMappingContractV04.SeedProvenance> seeds,
            List<ScenarioMappingContractV04.RealizationChainStep> steps) {
        return new ScenarioMappingContractV04("HYP-CAPABILITY-001", "HYP-SCENARIO-001",
                REV, SEMANTICS, direct, seeds, steps, List.of("bounded"));
    }

    private static ScenarioMappingContractV04.ComponentIdentity identity(String path, String symbol) {
        return new ScenarioMappingContractV04.ComponentIdentity(REV, path, ScenarioMappingContractV04.Granularity.METHOD, symbol);
    }

    private static ScenarioMappingContractV04.RealizationChainStep step(int order,
            ScenarioMappingContractV04.ComponentIdentity identity, String seed,
            ScenarioMappingContractV04.RelationshipBasis basis, String... traces) {
        return new ScenarioMappingContractV04.RealizationChainStep(order, identity, basis, seed, List.of(traces));
    }
}

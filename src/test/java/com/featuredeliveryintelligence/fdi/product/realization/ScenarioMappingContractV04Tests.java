package com.featuredeliveryintelligence.fdi.product.realization;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ScenarioMappingContractV04Tests {
    static final String REV = "818c4136ea971c21674525f9053de0d9c7ad8cfe", SHA = "a".repeat(64);

    @Test void acceptsSupportedMappingAndHonestUnresolved() {
        assertEquals(2, mapping().realizationChain().size());
        var unresolved = new ScenarioMappingContractV04(ScenarioMappingContractV04.SCHEMA_VERSION,
                ScenarioMappingContractV04.AUTHORITY, "HYP-CAPABILITY-002", "HYP-SCENARIO-002", REV, SHA, SHA,
                ScenarioMappingContractV04.Outcome.UNRESOLVED, ScenarioMappingContractV04.EvidenceStatus.INSUFFICIENT,
                List.of(), List.of(), List.of(), List.of(), List.of("No production reference resolved"), List.of("bounded"));
        assertTrue(unresolved.realizationChain().isEmpty());
    }

    @Test void outcomeAndEvidenceStatusFailClosed() {
        var m = mapping();
        assertThrows(RuntimeContractException.class, () -> copy(m, ScenarioMappingContractV04.Outcome.UNRESOLVED,
                ScenarioMappingContractV04.EvidenceStatus.INSUFFICIENT, m.directProductionSymbols(), m.seeds(),
                m.relationshipTraces(), m.realizationChain(), List.of()));
        assertThrows(RuntimeContractException.class, () -> copy(m, m.outcome(), ScenarioMappingContractV04.EvidenceStatus.COMPLETE,
                m.directProductionSymbols(), m.seeds(), m.relationshipTraces(), m.realizationChain(), List.of("gap")));
        assertThrows(RuntimeContractException.class, () -> new ScenarioMappingContractV04(m.schemaVersion(), m.authority(),
                m.capabilityId(), m.scenarioId(), REV, SHA, SHA, ScenarioMappingContractV04.Outcome.UNRESOLVED,
                ScenarioMappingContractV04.EvidenceStatus.PARTIAL, List.of(), List.of(), List.of(), List.of(),
                List.of("gap"), List.of("bounded")));
    }

    @Test void inferredStepRequiresTypedBoundRevisionAndDigestTrace() {
        var m = mapping();
        assertThrows(RuntimeContractException.class, () -> copy(m, m.outcome(), m.evidenceStatus(),
                m.directProductionSymbols(), m.seeds(), List.of(), m.realizationChain(), List.of()));
        assertThrows(RuntimeContractException.class, () -> new ScenarioMappingContractV04.RelationshipTrace(
                "trace-1", "0".repeat(40), SHA, m.relationshipTraces().get(0).edges()));
        var inferred = m.realizationChain().get(1);
        var unbound = new ScenarioMappingContractV04.RealizationChainStep(2, inferred.component(), inferred.relationshipBasis(),
                inferred.seedRef(), List.of(), "trace-missing");
        assertThrows(RuntimeContractException.class, () -> copy(m, m.outcome(), m.evidenceStatus(),
                m.directProductionSymbols(), m.seeds(), m.relationshipTraces(), List.of(m.realizationChain().get(0), unbound), List.of()));
    }

    @Test void canonicalPathsAndProviderNeutralDuplicateAliasesFailClosed() {
        assertDoesNotThrow(() -> new ScenarioMappingContractV04.ComponentIdentity(REV,
                ".github/workflows/build.yml", ScenarioMappingContractV04.Granularity.CONFIGURATION,
                "github.workflow.build"));
        for (String path : badPaths()) assertThrows(RuntimeContractException.class,
                () -> identity(path, "example.A.find"), path);
        var m = mapping();
        var duplicate = new ScenarioMappingContractV04.DirectProductionSymbolEvidence("direct-2", "observation-2",
                m.directProductionSymbols().get(0).productionSymbol());
        assertThrows(RuntimeContractException.class, () -> copy(m, m.outcome(), m.evidenceStatus(),
                List.of(m.directProductionSymbols().get(0), duplicate), m.seeds(), m.relationshipTraces(), m.realizationChain(), List.of()));
    }

    @Test void directStepMustUseTheNamedSeedsIdentityAndEvidence() {
        var m = mapping();
        var other = identity("src/main/java/example/VetController.java", "example.VetController.find");
        var directB = new ScenarioMappingContractV04.DirectProductionSymbolEvidence("direct-2", "observation-2", other);
        var seedB = new ScenarioMappingContractV04.SeedProvenance("seed-2", "direct-2", other);
        var bypass = new ScenarioMappingContractV04.RealizationChainStep(1,
                m.directProductionSymbols().get(0).productionSymbol(),
                ScenarioMappingContractV04.RelationshipBasis.DIRECT_TEST_REFERENCE,
                "seed-2", List.of("direct-1"), null);
        assertThrows(RuntimeContractException.class, () -> copy(m, m.outcome(), m.evidenceStatus(),
                List.of(m.directProductionSymbols().get(0), directB), List.of(m.seeds().get(0), seedB),
                m.relationshipTraces(), List.of(bypass, m.realizationChain().get(1)), List.of()));
    }

    @Test void evaluatorLeakageUsesOnePolicyAcrossRelevantStrings() {
        for (String forbidden : forbiddenRefs()) {
            assertThrows(RuntimeContractException.class, () -> new ScenarioMappingContractV04.DirectProductionSymbolEvidence(
                    "direct", forbidden, identity("src/main/java/example/A.java", "example.A.find")), forbidden);
            assertThrows(RuntimeContractException.class, () -> new ScenarioMappingContractV04.SeedProvenance(
                    "seed", forbidden, identity("src/main/java/example/A.java", "example.A.find")), forbidden);
        }
    }

    static List<String> badPaths() { return List.of("./src/main/A.java", "src//main/A.java", " src/main/A.java",
            "src/main/A.java ", "/src/main/A.java", "src\\main\\A.java", "src/./main/A.java",
            "src/main/../A.java", "src/test/java/A.java", "SRC/TEST/java/A.java", "Src/Test/A.java",
            "test/A.java", "TEST/A.java", "tests/A.java", "Tests/A.java", "src/main/java/OwnerTEST.java",
            "src/main/java/OwnerTeSt.java", "src/main/java/OwnerTESTS.java"); }
    static List<String> forbiddenRefs() { return List.of("evaluator_gold", "evaluator-gold", "evaluator gold",
            "gold_mapping", "gold-mapping", "gold mapping", "ground_truth", "expected mapping"); }

    static ScenarioMappingContractV04 mapping() {
        var controller = identity("src/main/java/example/OwnerController.java", "example.OwnerController.find");
        var repository = identity("src/main/java/example/OwnerRepository.java", "example.OwnerRepository.find");
        var direct = new ScenarioMappingContractV04.DirectProductionSymbolEvidence("direct-1", "observation-1", controller);
        var seed = new ScenarioMappingContractV04.SeedProvenance("seed-1", "direct-1", controller);
        var edge = new ScenarioMappingContractV04.RelationshipEdge(1, controller, repository, "CALLS", "graph-edge-1");
        var trace = new ScenarioMappingContractV04.RelationshipTrace("trace-1", REV, SHA, List.of(edge));
        var steps = List.of(new ScenarioMappingContractV04.RealizationChainStep(1, controller,
                        ScenarioMappingContractV04.RelationshipBasis.DIRECT_TEST_REFERENCE, "seed-1", List.of("direct-1"), null),
                new ScenarioMappingContractV04.RealizationChainStep(2, repository,
                        ScenarioMappingContractV04.RelationshipBasis.GRAPHIFY_INFERRED, "seed-1", List.of(), "trace-1"));
        return new ScenarioMappingContractV04(ScenarioMappingContractV04.SCHEMA_VERSION, ScenarioMappingContractV04.AUTHORITY,
                "HYP-CAPABILITY-001", "HYP-SCENARIO-001", REV, SHA, SHA, ScenarioMappingContractV04.Outcome.MAPPING_PROPOSAL,
                ScenarioMappingContractV04.EvidenceStatus.COMPLETE, List.of(direct), List.of(seed), List.of(trace), steps,
                List.of(), List.of("bounded"));
    }

    static ScenarioMappingContractV04 copy(ScenarioMappingContractV04 m, ScenarioMappingContractV04.Outcome outcome,
            ScenarioMappingContractV04.EvidenceStatus status, List<ScenarioMappingContractV04.DirectProductionSymbolEvidence> direct,
            List<ScenarioMappingContractV04.SeedProvenance> seeds, List<ScenarioMappingContractV04.RelationshipTrace> traces,
            List<ScenarioMappingContractV04.RealizationChainStep> chain, List<String> gaps) {
        return new ScenarioMappingContractV04(m.schemaVersion(), m.authority(), m.capabilityId(), m.scenarioId(),
                m.sourceRevision(), m.graphSha256(), m.frozenSemanticsSha256(), outcome, status, direct, seeds, traces, chain, gaps, m.limitations());
    }

    static ScenarioMappingContractV04.ComponentIdentity identity(String path, String symbol) {
        return new ScenarioMappingContractV04.ComponentIdentity(REV, path, ScenarioMappingContractV04.Granularity.METHOD, symbol);
    }
}

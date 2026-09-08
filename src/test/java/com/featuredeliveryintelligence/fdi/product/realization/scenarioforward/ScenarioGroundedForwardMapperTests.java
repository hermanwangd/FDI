package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.*;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04;
import com.featuredeliveryintelligence.fdi.product.realization.directtrace.*;
import com.featuredeliveryintelligence.fdi.product.realization.graphexpansion.GraphifyProductionExpansion.*;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ScenarioGroundedForwardMapperTests {
    private static final String REV = "8".repeat(40);
    private static final String GRAPH = "a".repeat(64);
    private static final String SEMANTICS = "b".repeat(64);

    @Test
    void composesDirectAndInferredEvidenceWithoutMixingTheirBasis() {
        var direct = direct("obs-1", "e-1", "seed-1", component("Controller.java", "pet.Controller#save"));
        var inferred = component("Repository.java", "pet.Repository#save");
        var relationship = new RelationshipTrace("trace-1", REV, GRAPH, List.of(
                new RelationshipEdge(1, direct.seed().productionSeed(), inferred, "CALLS", "graph-edge-1")));
        var expansion = new Result(REV, GRAPH, new QueryBounds(1, 5, 5, 5, 1000, 1000), List.of(
                new ExpandedSeedTrace(new ProductionSeed("seed-1", direct.seed().productionSeed(), "node-1"), "node-1",
                        List.of(new InferredNeighbour(inferred, "node-2", relationship)))));

        var result = ScenarioGroundedForwardMapper.compose(input(List.of(direct), List.of(), expansion,
                List.of(new ScenarioGroundedForwardMapper.ScenarioAssignment("CAP-1", "SC-1", "obs-1", List.of("obs-1"),
                        List.of("trace-1"), List.of()))));

        assertThat(result.capabilities()).singleElement().satisfies(capability -> {
            assertThat(capability.capabilityId()).isEqualTo("CAP-1");
            assertThat(capability.scenarios()).singleElement().satisfies(proposal -> {
                var mapping = proposal.mapping();
                assertThat(mapping.outcome()).isEqualTo(Outcome.MAPPING_PROPOSAL);
                assertThat(mapping.evidenceStatus()).isEqualTo(EvidenceStatus.COMPLETE);
                assertThat(mapping.realizationChain()).extracting(RealizationChainStep::relationshipBasis)
                        .containsExactly(RelationshipBasis.DIRECT_TEST_REFERENCE, RelationshipBasis.GRAPHIFY_INFERRED);
                assertThat(mapping.realizationChain().get(0).evidenceRefs()).containsExactly("e-1");
                assertThat(mapping.realizationChain().get(1).relationshipTraceRef()).isEqualTo("trace-1");
                assertThat(proposal.componentRoles()).extracting(ScenarioGroundedForwardMapper.ComponentRole::role)
                        .containsExactly("PRIMARY", "SUPPORTING");
            });
        });
    }

    @Test
    void unsupportedScenarioIsUnresolvedAndCannotBorrowAnotherScenarioEvidence() {
        var direct = direct("obs-1", "e-1", "seed-1", component("Owner.java", "pet.Owner#save"));
        var expansion = new Result(REV, GRAPH, new QueryBounds(1, 5, 5, 5, 1000, 1000), List.of());
        var result = ScenarioGroundedForwardMapper.compose(input(List.of(direct), List.of(gap("gap-1")), expansion,
                List.of(new ScenarioGroundedForwardMapper.ScenarioAssignment("CAP-1", "SC-1", "obs-1", List.of("obs-1"), List.of(), List.of()),
                        new ScenarioGroundedForwardMapper.ScenarioAssignment("CAP-1", "SC-2", null, List.of(), List.of(), List.of("/test_files/0/gap-1")))));
        assertThat(result.capabilities().get(0).scenarios()).extracting(value -> value.mapping().outcome())
                .containsExactly(Outcome.MAPPING_PROPOSAL, Outcome.UNRESOLVED);
        assertThat(result.capabilities().get(0).scenarios().get(1).mapping().directProductionSymbols()).isEmpty();
    }

    @Test
    void rejectsDuplicateScenarioUnknownEvidenceCrossSeedTraceAndBindingMismatch() {
        var direct = direct("obs-1", "e-1", "seed-1", component("Owner.java", "pet.Owner#save"));
        var empty = new Result(REV, GRAPH, new QueryBounds(1, 5, 5, 5, 1000, 1000), List.of());
        var assignment = new ScenarioGroundedForwardMapper.ScenarioAssignment("CAP-1", "SC-1", "obs-1", List.of("obs-1"), List.of(), List.of());
        assertThatThrownBy(() -> ScenarioGroundedForwardMapper.compose(input(List.of(direct), List.of(), empty,
                List.of(assignment, assignment)))).isInstanceOf(RuntimeContractException.class).hasMessageContaining("duplicate scenario");
        assertThatThrownBy(() -> ScenarioGroundedForwardMapper.compose(input(List.of(direct), List.of(), empty,
                List.of(new ScenarioGroundedForwardMapper.ScenarioAssignment("CAP-1", "SC-1", "missing", List.of("missing"), List.of(), List.of())))))
                .isInstanceOf(RuntimeContractException.class).hasMessageContaining("unknown observation");
        assertThatThrownBy(() -> ScenarioGroundedForwardMapper.compose(new ScenarioGroundedForwardMapper.Input(
                "wrong", "FROZEN", "REVIEWED_EXPERIMENT_SEMANTICS", REV, SEMANTICS, "c".repeat(64), "d".repeat(64), GRAPH,
                List.of(direct), List.of(), empty, List.of(assignment))))
                .isInstanceOf(RuntimeContractException.class).hasMessageContaining("snapshot");

        var other = component("Other.java", "pet.Other#save");
        var crossTrace = new RelationshipTrace("cross", REV, GRAPH, List.of(
                new RelationshipEdge(1, other, component("Repo.java", "pet.Repo#save"), "CALLS", "edge")));
        var crossExpansion = new Result(REV, GRAPH, new QueryBounds(1, 5, 5, 5, 1000, 1000), List.of(
                new ExpandedSeedTrace(new ProductionSeed("other-seed", other, "other"), "other",
                        List.of(new InferredNeighbour(component("Repo.java", "pet.Repo#save"), "repo", crossTrace)))));
        assertThatThrownBy(() -> ScenarioGroundedForwardMapper.compose(input(List.of(direct), List.of(), crossExpansion,
                List.of(new ScenarioGroundedForwardMapper.ScenarioAssignment("CAP-1", "SC-1", "obs-1",
                        List.of("obs-1"), List.of("cross"), List.of())))))
                .isInstanceOf(RuntimeContractException.class).hasMessageContaining("scenario direct seed");
    }

    private static ScenarioGroundedForwardMapper.Input input(List<ResolvedDirectObservation> direct,
            List<UnresolvedDirectReferenceGap> gaps, Result expansion,
            List<ScenarioGroundedForwardMapper.ScenarioAssignment> assignments) {
        return new ScenarioGroundedForwardMapper.Input("pkb001-petclinic-reviewed-semantics-004", "FROZEN",
                "REVIEWED_EXPERIMENT_SEMANTICS", REV, SEMANTICS, "c".repeat(64), "d".repeat(64), GRAPH,
                direct, gaps, expansion, assignments);
    }

    private static ResolvedDirectObservation direct(String observation, String evidence, String seed, ComponentIdentity id) {
        var d = new DirectProductionSymbolEvidence(evidence, observation, id);
        return new ResolvedDirectObservation(d, new SeedProvenance(seed, evidence, id),
                new TraceSourceLocation("src/test/java/pet/Test.java", 1, 1));
    }

    private static UnresolvedDirectReferenceGap gap(String observation) {
        return new UnresolvedDirectReferenceGap("/test_files/0/" + observation, "unresolved expression",
                new TraceSourceLocation("src/test/java/pet/Test.java", 2, 1), "MISSING_SOURCE");
    }

    private static ComponentIdentity component(String file, String symbol) {
        return new ComponentIdentity(REV, "src/main/java/pet/" + file, Granularity.METHOD, symbol);
    }
}

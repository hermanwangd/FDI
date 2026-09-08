package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.*;
import com.featuredeliveryintelligence.fdi.product.realization.directtrace.*;
import com.featuredeliveryintelligence.fdi.product.realization.graphexpansion.GraphifyProductionExpansion.*;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class ScenarioGroundedForwardMapperTests {
    private static final String REV = "8".repeat(40), GRAPH = "a".repeat(64);

    @Test void currentContractCanOnlyEmitUnresolvedAndPreservesObservedEvidenceOutsideChains() {
        var result = ScenarioGroundedForwardMapper.compose(input(List.of(direct()),
                List.of(new ScenarioGroundedForwardMapper.ScenarioAssignment("CAP-1", "SC-1", List.of()))));
        var proposal = result.capabilities().get(0).scenarios().get(0);
        assertThat(proposal.mapping().outcome()).isEqualTo(Outcome.UNRESOLVED);
        assertThat(proposal.mapping().evidenceStatus()).isEqualTo(EvidenceStatus.INSUFFICIENT);
        assertThat(proposal.mapping().realizationChain()).isEmpty();
        assertThat(proposal.componentRoles()).isEmpty();
        assertThat(result.observedDirectEvidenceRefs()).containsExactly("e-1");
    }

    @Test void callerCannotInjectPrimaryOrFlipAConfirmationFlag() {
        String injected = """
                {"capabilityId":"CAP-1","scenarioId":"SC-1","gapRefs":[],
                 "coreBehaviorEvidenceConfirmed":true,
                 "primaryComponent":{"sourceRevision":"%s","sourcePath":"src/main/java/pet/Controller.java",
                 "granularity":"METHOD","qualifiedSymbol":"pet.Controller#save"}}
                """.formatted(REV);
        assertThatThrownBy(() -> new ObjectMapper().readValue(injected,
                ScenarioGroundedForwardMapper.ScenarioAssignment.class)).hasMessageContaining("Unrecognized field");
    }

    @Test void rejectsDuplicateScenarioUnknownGapAndBindingMismatch() {
        var assignment = new ScenarioGroundedForwardMapper.ScenarioAssignment("CAP-1", "SC-1", List.of());
        assertThatThrownBy(() -> ScenarioGroundedForwardMapper.compose(input(List.of(), List.of(assignment, assignment))))
                .isInstanceOf(RuntimeContractException.class).hasMessageContaining("duplicate scenario");
        assertThatThrownBy(() -> ScenarioGroundedForwardMapper.compose(input(List.of(), List.of(
                new ScenarioGroundedForwardMapper.ScenarioAssignment("CAP-1", "SC-1", List.of("missing"))))))
                .isInstanceOf(RuntimeContractException.class).hasMessageContaining("unknown evidence gap");
        var value = input(List.of(), List.of(assignment));
        assertThatThrownBy(() -> ScenarioGroundedForwardMapper.compose(new ScenarioGroundedForwardMapper.Input(
                "wrong", value.snapshotStatus(), value.snapshotAuthority(), value.sourceRevision(), value.semanticsSha256(),
                value.authorizationSha256(), value.testEvidenceSha256(), value.graphSha256(), List.of(), List.of(),
                value.expansion(), value.assignments())))
                .isInstanceOf(RuntimeContractException.class).hasMessageContaining("snapshot");
    }

    private static ScenarioGroundedForwardMapper.Input input(List<ResolvedDirectObservation> direct,
            List<ScenarioGroundedForwardMapper.ScenarioAssignment> assignments) {
        return new ScenarioGroundedForwardMapper.Input(ScenarioGroundedForwardMapper.SNAPSHOT, "FROZEN",
                "REVIEWED_EXPERIMENT_SEMANTICS", REV, "b".repeat(64), "c".repeat(64), "d".repeat(64), GRAPH,
                direct, List.of(), new Result(REV, GRAPH, new QueryBounds(1, 5, 5, 5, 1000, 1000), List.of()), assignments);
    }
    private static ResolvedDirectObservation direct() {
        var id = new ComponentIdentity(REV, "src/main/java/pet/Controller.java", Granularity.METHOD, "pet.Controller#save");
        var evidence = new DirectProductionSymbolEvidence("e-1", "obs-1", id);
        return new ResolvedDirectObservation(evidence, new SeedProvenance("seed-1", "e-1", id),
                new TraceSourceLocation("src/test/java/pet/Test.java", 1, 1));
    }
}

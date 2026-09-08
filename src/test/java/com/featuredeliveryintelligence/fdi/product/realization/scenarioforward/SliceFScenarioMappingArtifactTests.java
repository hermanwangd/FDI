package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SliceFScenarioMappingArtifactTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path OUTPUT = Path.of("validation/pkb001/scenario-forward/slice-f-scenario-mapping-proposal-001.json");

    @Test
    void artifactCoversTheExactFrozenScenarioSetAndPreservesProposalBoundary() throws Exception {
        ScenarioForwardPrototypeGenerator.main(new String[]{"."});
        JsonNode artifact = JSON.readTree(OUTPUT.toFile());
        JsonNode semantics = JSON.readTree(Path.of("validation/pkb001/scenario-review/"
                + "pkb001-scenarios-petclinic-818c413-20260905-01/accepted-semantics-004.json").toFile());
        Set<String> expected = new HashSet<>();
        Map<String, String> expectedParents = new HashMap<>();
        semantics.path("capabilities").forEach(capability -> capability.path("scenarios")
                .forEach(scenario -> { expected.add(scenario.path("scenario_id").asText());
                    expectedParents.put(scenario.path("scenario_id").asText(), capability.path("capability_id").asText()); }));
        Set<String> actual = new HashSet<>();
        int[] mapped = {0}; int[] unresolved = {0}; int[] direct = {0}; int[] inferred = {0};
        artifact.path("capabilities").forEach(capability -> capability.path("scenarios").forEach(proposal -> {
            JsonNode mapping = proposal.path("mapping"); actual.add(mapping.path("scenarioId").asText());
            assertThat(mapping.path("capabilityId").asText()).isEqualTo(expectedParents.get(mapping.path("scenarioId").asText()));
            if (mapping.path("outcome").asText().equals("MAPPING_PROPOSAL")) mapped[0]++; else unresolved[0]++;
            mapping.path("realizationChain").forEach(step -> {
                if (step.path("relationshipBasis").asText().equals("DIRECT_TEST_REFERENCE")) direct[0]++; else inferred[0]++;
            });
            if (mapping.path("outcome").asText().equals("MAPPING_PROPOSAL"))
                assertThat(proposal.path("componentRoles").findValuesAsText("role")).contains("PRIMARY");
        }));
        assertThat(artifact.path("capabilities")).hasSize(5);
        assertThat(actual).isEqualTo(expected).hasSize(10);
        assertThat(mapped[0]).isZero(); assertThat(unresolved[0]).isEqualTo(10);
        assertThat(direct[0]).isZero(); assertThat(inferred[0]).isZero();
        assertThat(artifact.path("observedDirectEvidenceRefs")).hasSize(144);
        assertThat(artifact.path("unresolvedDirectReferenceRefs")).hasSize(891);
        assertThat(artifact.path("semanticPublicationAllowed").asBoolean()).isFalse();
        assertThat(artifact.toString()).doesNotContain("Person#getLastName", "BaseEntity#getId");
    }
}

package com.featuredeliveryintelligence.fdi.product.realization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs the same contract fixtures as Python against the actual Java constructors. */
class ScenarioForwardParityTests {
    @TestFactory
    List<DynamicTest> sharedContractFixtures() throws Exception {
        var mapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        JsonNode fixtures = mapper.readTree(Path.of(
                "validation/pkb001/fixtures/scenario-forward-parity.json").toFile());
        assertTrue(fixtures.isArray(), "shared fixtures must be an array");
        List<DynamicTest> tests = new ArrayList<>();
        for (JsonNode fixture : fixtures) {
            tests.add(DynamicTest.dynamicTest(fixture.path("name").asText(), () -> {
                assertTrue(fixture.get("valid").isBoolean());
                boolean accepted = true;
                try {
                    mapper.treeToValue(fixture.get("result"), ScenarioRealizationProposal.class);
                } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException failure) {
                    accepted = false;
                }
                assertEquals(fixture.get("valid").booleanValue(), accepted,
                        "actual Java contract acceptance: " + fixture.path("name").asText());
            }));
        }
        assertTrue(tests.size() >= 3, "shared fixtures must exercise positive and negative contracts");
        return tests;
    }
}

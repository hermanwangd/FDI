package com.featuredeliveryintelligence.fdi.validation.scenarioforward;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScenarioForwardCharacterizationTests {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TestFactory
    List<DynamicTest> allSharedFixturesRetainTheirDecisionThroughGateContractPath() throws Exception {
        JsonNode fixtures = JSON.readTree(Path.of(
                "validation/pkb001/fixtures/scenario-forward-parity.json").toFile());
        byte[] schema = Files.readAllBytes(Path.of(ScenarioForwardGate.SCHEMA_PATH));
        List<DynamicTest> tests = new ArrayList<>();
        for (JsonNode fixture : fixtures) {
            tests.add(DynamicTest.dynamicTest(fixture.path("name").asText(), () -> {
                var proposal = JSON.createObjectNode()
                        .put("schema_version", "pkb001.realization-proposal.v0.3")
                        .put("authority", "PROPOSAL_ONLY")
                        .put("run_id", "fixture-only")
                        .put("source_revision", "a".repeat(40))
                        .put("graph_sha256", "b".repeat(64))
                        .put("semantics_sha256", "c".repeat(64));
                proposal.putArray("capability_results").add(fixture.get("result"));
                List<String> reasons = new ScenarioForwardGate().validateProposalContract(proposal, schema);
                List<String> expected = new ArrayList<>();
                fixture.withArray("expected_reasons").forEach(reason -> expected.add(reason.asText()));
                assertEquals(expected, reasons);
                assertEquals(fixture.path("valid").booleanValue(), reasons.isEmpty());
            }));
        }
        assertEquals(36, tests.size());
        return tests;
    }
}

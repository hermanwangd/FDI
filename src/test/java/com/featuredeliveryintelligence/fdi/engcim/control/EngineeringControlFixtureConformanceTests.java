package com.featuredeliveryintelligence.fdi.engcim.control;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * B1 adapter: fixture JSON is input to the real evaluator; expected JSON is
 * read only for an independent assertion after evaluation.
 */
class EngineeringControlFixtureConformanceTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String FIXTURE_ROOT = "/engcim/rc7b-v03/controls/";
    private static final List<String> CASES = List.of(
            "exact-binding-fresh",
            "exact-binding-stale",
            "evidence-integrity-valid",
            "independent-evaluation-producer-only",
            "execution-safety-protected-force-push",
            "finding-resolution-fresh",
            "finding-resolution-stale");

    @Test
    void fixtureInputsProduceIndependentlyAssertedResults() throws Exception {
        EngineeringControlEvaluator evaluator = new EngineeringControlEvaluator();
        List<ObjectNode> observed = new ArrayList<>();

        for (String caseRef : CASES) {
            JsonNode input = read(FIXTURE_ROOT + caseRef + ".input.json");
            JsonNode expected = read(FIXTURE_ROOT + caseRef + ".expected.json");
            String controlRef = required(input, "controlRef").textValue();
            JsonNode subject = input.get("subject");
            JsonNode evidence = input.get("evidence");

            EngineeringControlResult binding = null;
            if (EngineeringControlCatalog.FINDING_RESOLUTION.equals(controlRef)) {
                JsonNode bindingInput = evidence.get("bindingInput");
                binding = evaluator.evaluate(EngineeringControlCatalog.EXACT_BINDING,
                        bindingInput.get("subject"), bindingInput.get("evidence"));
                evidence = evidence.deepCopy();
                ((ObjectNode) evidence).remove("bindingInput");
                ((ObjectNode) evidence).put("bindingControlRef", binding.controlRef());
                ((ObjectNode) evidence).put("bindingOutcome", binding.outcome().name());
            }

            EngineeringControlResult actual = evaluator.evaluate(controlRef, subject, evidence);
            assertEquals(required(expected, "outcome").textValue(), actual.outcome().name(), caseRef);
            assertEquals(expected.get("reasonCodes"), JSON.valueToTree(actual.reasonCodes()), caseRef);

            ObjectNode record = JSON.createObjectNode();
            record.put("caseRef", caseRef);
            record.set("actual", JSON.valueToTree(actual));
            if (binding != null) record.set("bindingResult", JSON.valueToTree(binding));
            observed.add(record);
        }

        String outputDirectory = System.getProperty("engcim.observed.dir");
        if (outputDirectory != null && !outputDirectory.isBlank()) {
            Path directory = Path.of(outputDirectory);
            Files.createDirectories(directory);
            Path output = directory.resolve("raw-control-results.json");
            Files.writeString(output, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(observed));
        }
    }

    private static JsonNode read(String resource) throws IOException {
        try (InputStream stream = EngineeringControlFixtureConformanceTests.class
                .getResourceAsStream(resource)) {
            return JSON.readTree(Objects.requireNonNull(stream, resource));
        }
    }

    private static JsonNode required(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) throw new IllegalArgumentException("missing fixture field: " + field);
        return value;
    }
}

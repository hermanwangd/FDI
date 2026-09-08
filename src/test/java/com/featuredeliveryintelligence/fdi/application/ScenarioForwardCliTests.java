package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardGate;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequest;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioForwardCliTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path REPOSITORY = Path.of("").toAbsolutePath();

    @TempDir Path temp;

    @Test void handlesOnlyScenarioForwardCommand() {
        assertFalse(ScenarioForwardCli.handles(new String[0]));
        assertFalse(ScenarioForwardCli.handles(new String[]{"unrelated"}));
        assertTrue(ScenarioForwardCli.handles(new String[]{"scenario-forward-validate"}));
    }

    @Test void validRequestEmitsOneDeterministicContractValidReport() throws Exception {
        Fixture fixture = fixture("valid-cli-run");

        Result first = run(fixture.args());
        Result second = run(fixture.args());

        assertEquals(0, first.exitCode());
        assertEquals("", first.stderr());
        assertEquals(first.stdout(), second.stdout());
        assertEquals(1, first.stdout().lines().count());
        JsonNode report = JSON.readTree(first.stdout());
        assertEquals("CONTRACT_VALID", report.path("status").asText(), first.stdout());
        assertTrue(report.path("mappings").isEmpty());
        assertEquals(List.of("PRODUCT_SEMANTICS", "FROZEN_GRAPH", "PKS1_SKILL"),
                toTextList(report.path("generation_inputs"), "kind"));
    }

    @Test void blockedContractIsSuccessfulAndHasNoStackTrace() throws Exception {
        Fixture fixture = fixture("blocked-cli-run");
        ObjectNode request = (ObjectNode) JSON.readTree(fixture.request().toFile());
        ((ObjectNode) request.path("proposal")).put("graph_sha256", "0".repeat(64));
        Files.write(fixture.request(), JSON.writeValueAsBytes(request));

        Result result = run(fixture.args());

        assertEquals(0, result.exitCode());
        assertEquals("", result.stderr());
        assertFalse(result.stdout().contains("Exception"));
        assertEquals("BLOCKED", JSON.readTree(result.stdout()).path("status").asText());
    }

    @Test void malformedOptionsFailDeterministicallyWithoutStackTrace() {
        List<String[]> invalid = List.of(
                new String[]{"scenario-forward-validate"},
                new String[]{"scenario-forward-validate", "--root", temp.toString()},
                new String[]{"scenario-forward-validate", "--root", temp.toString(), "--root", temp.toString(), "--request", "r.json"},
                new String[]{"scenario-forward-validate", "--root", temp.toString(), "--unknown", "r.json"},
                new String[]{"scenario-forward-validate", "--root", " ", "--request", "r.json"},
                new String[]{"scenario-forward-validate", "--root", temp.toString(), "r.json", "--request"});

        for (String[] args : invalid) {
            Result first = run(args);
            Result second = run(args);
            assertNotEquals(0, first.exitCode(), List.of(args).toString());
            assertEquals("", first.stdout());
            assertEquals(first.stderr(), second.stderr());
            assertTrue(first.stderr().startsWith("scenario-forward-validate: INVALID_ARGUMENTS:"), first.stderr());
            assertFalse(first.stderr().contains("Exception"), first.stderr());
            assertFalse(first.stderr().contains("\tat "), first.stderr());
        }
    }

    @Test void applicationProcessReturnsStableExitCodesWithoutStartingSpring() throws Exception {
        Fixture fixture = fixture("process-cli-run");
        Process valid = javaProcess(fixture.args());
        assertEquals(0, valid.waitFor());
        String validOut = new String(valid.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String validErr = new String(valid.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals("CONTRACT_VALID", JSON.readTree(validOut).path("status").asText());
        assertEquals("", validErr);
        assertFalse(validOut.contains("Spring"));

        Process invalid = javaProcess(new String[]{"scenario-forward-validate", "--root", temp.toString()});
        assertEquals(2, invalid.waitFor());
        assertEquals("", new String(invalid.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        String invalidErr = new String(invalid.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(invalidErr.startsWith("scenario-forward-validate: INVALID_ARGUMENTS:"), invalidErr);
        assertFalse(invalidErr.contains("Exception"), invalidErr);
    }

    private Process javaProcess(String[] args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(FdiApplication.class.getName());
        command.addAll(List.of(args));
        return new ProcessBuilder(command).start();
    }

    private Result run(String[] args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exit = ScenarioForwardCli.run(args, new PrintStream(stdout), new PrintStream(stderr));
        return new Result(exit, stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
    }

    private Fixture fixture(String runId) throws Exception {
        Path root = temp.resolve(runId + "-root");
        Files.createDirectories(root);
        String folder = "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/";
        Map<String, String> paths = new LinkedHashMap<>();
        paths.put("PRODUCT_SEMANTICS", folder + "accepted-semantics-001.json");
        paths.put("ACCEPTANCE_MANIFEST", folder + "acceptance-manifest-001.json");
        paths.put("REVIEW_DECISIONS", folder + "review-decisions-001.json");
        paths.put("ORIGINAL_PROPOSAL", folder + "proposal.json");
        paths.put("GRAPHIFY_BINDING_EVIDENCE", "validation/pkb001/runtime/graphify-petclinic-live-evidence.json");
        paths.put("FROZEN_GRAPH", "validation/pkb001/artifacts/petclinic-graph-818c413.json");
        paths.put("PROPOSAL_SCHEMA", ScenarioForwardGate.SCHEMA_PATH);
        paths.put("PKS1_SKILL", ScenarioForwardGate.SKILL_PATH);
        List<ScenarioForwardRequest.BoundInput> inputs = new ArrayList<>();
        for (var entry : paths.entrySet()) {
            Path target = root.resolve(entry.getValue());
            Files.createDirectories(target.getParent());
            Files.copy(REPOSITORY.resolve(entry.getValue()), target, StandardCopyOption.REPLACE_EXISTING);
            inputs.add(new ScenarioForwardRequest.BoundInput(entry.getKey(), entry.getValue(),
                    ScenarioForwardRequestReader.sha256(Files.readAllBytes(target))));
        }
        git(root, "init", "-q");
        git(root, "-c", "user.name=Fixture", "-c", "user.email=fixture@example.test",
                "commit", "--allow-empty", "-qm", "fixture");

        ObjectNode semantics = (ObjectNode) JSON.readTree(root.resolve(paths.get("PRODUCT_SEMANTICS")).toFile());
        ObjectNode capability = (ObjectNode) semantics.withArray("capabilities").get(0);
        ObjectNode result = JSON.createObjectNode();
        result.put("capability_id", capability.path("capability_id").asText());
        result.put("source_revision", semantics.path("applicable_source_commit_sha").asText());
        result.put("outcome", "UNRESOLVED").put("evidence_status", "INSUFFICIENT");
        result.putArray("components");
        ArrayNode bound = result.putArray("bound_scenarios");
        ArrayNode traces = result.putArray("scenario_traces");
        for (JsonNode scenario : capability.withArray("scenarios")) {
            bound.addObject().put("scenario_id", scenario.path("scenario_id").asText())
                    .put("capability_id", capability.path("capability_id").asText());
            ObjectNode trace = traces.addObject().put("scenario_id", scenario.path("scenario_id").asText())
                    .put("capability_id", capability.path("capability_id").asText());
            ObjectNode step = trace.putArray("steps").addObject()
                    .put("behavioral_function", "Synthetic contract-only validation")
                    .put("state", "EVIDENCE_GAP");
            step.putArray("component_refs");
            step.putArray("evidence_refs");
            step.put("evidence_gap", "No mapping generation performed");
            step.putNull("not_applicable_reason");
        }
        result.putArray("limitations").add("Synthetic validation fixture, not a mapping result");
        Map<String, ScenarioForwardRequest.BoundInput> byKind = new LinkedHashMap<>();
        inputs.forEach(input -> byKind.put(input.kind(), input));
        ObjectNode proposal = JSON.createObjectNode().put("schema_version", "pkb001.realization-proposal.v0.3")
                .put("authority", "PROPOSAL_ONLY").put("run_id", runId)
                .put("source_revision", result.path("source_revision").asText())
                .put("graph_sha256", byKind.get("FROZEN_GRAPH").sha256())
                .put("semantics_sha256", byKind.get("PRODUCT_SEMANTICS").sha256());
        proposal.putArray("capability_results").add(result);
        ObjectNode request = JSON.createObjectNode();
        request.set("inputs", JSON.valueToTree(inputs));
        request.set("proposal", proposal);
        Path requestPath = root.resolve("request.json");
        Files.write(requestPath, JSON.writeValueAsBytes(request));
        return new Fixture(root, requestPath);
    }

    private static void git(Path root, String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-C", root.toString()));
        command.addAll(List.of(args));
        assertEquals(0, new ProcessBuilder(command).redirectErrorStream(true).start().waitFor());
    }

    private static List<String> toTextList(JsonNode array, String field) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.path(field).asText()));
        return values;
    }

    private record Result(int exitCode, String stdout, String stderr) {}
    private record Fixture(Path root, Path request) {
        String[] args() {
            return new String[]{"scenario-forward-validate", "--root", root.toString(), "--request", request.toString()};
        }
    }
}

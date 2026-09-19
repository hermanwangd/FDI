package com.featuredeliveryintelligence.fdi.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineeringControlCliTests {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path temp;

    @Test
    void controlEvalInvokesEvaluatorAndWritesEngineeringControlResultJson() throws Exception {
        Path subject = temp.resolve("subject.json");
        Path evidence = temp.resolve("evidence.json");
        Path output = temp.resolve("result.json");
        Files.writeString(subject, """
                {"subjectRef":"candidate:r1","revision":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}
                """, StandardCharsets.UTF_8);
        Files.writeString(evidence, """
                {"boundSubjectRef":"candidate:r1","boundRevision":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","evidenceRefs":["VER-1"]}
                """, StandardCharsets.UTF_8);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int status = EngineeringControlCli.run(new String[]{
                "control-eval", "--control", "CTRL-EXACT-BINDING-001",
                "--subject", subject.toString(), "--evidence", evidence.toString(),
                "--out", output.toString()},
                new PrintStream(stdout), new PrintStream(stderr));

        assertEquals(0, status, stderr.toString(StandardCharsets.UTF_8));
        JsonNode result = JSON.readTree(Files.readString(output));
        assertEquals("CTRL-EXACT-BINDING-001", result.path("controlRef").asText());
        assertEquals("SATISFIED", result.path("outcome").asText());
        assertTrue(result.path("resultRef").asText().startsWith("ECR-"));
    }

    @Test
    void controlGatePersistsBindingInputsResultsAndFailClosedDecision() throws Exception {
        Path bindings = temp.resolve("bindings.json");
        Path inputs = temp.resolve("inputs.json");
        Path output = temp.resolve("gate-output");
        Files.writeString(bindings, """
                [{"bindingRef":"BIND-1","scopeRef":"S05:r1","gateRef":"S05-delivery",
                  "controlRef":"CTRL-EXACT-BINDING-001","subjectRef":"candidate:r1","evidenceRefs":["EV-1"]}]
                """, StandardCharsets.UTF_8);
        Files.writeString(inputs, """
                {"BIND-1":{"subject":{"subjectRef":"candidate:r1","revision":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"},
                  "evidence":{"boundSubjectRef":"candidate:r1","boundRevision":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","evidenceRefs":["EV-1"]}}}
                """, StandardCharsets.UTF_8);

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int status = EngineeringControlCli.run(new String[]{
                "control-gate", "--gate", "S05-delivery", "--bindings", bindings.toString(),
                "--inputs", inputs.toString(), "--out", output.toString(), "--execution-ref", "RUN-1"},
                new PrintStream(stdout), new PrintStream(stderr));

        assertEquals(0, status, stderr.toString(StandardCharsets.UTF_8));
        JsonNode gate = JSON.readTree(stdout.toString(StandardCharsets.UTF_8));
        assertTrue(gate.path("proceed").asBoolean());
        assertEquals("RUN-1", gate.path("executionRef").asText());
        assertEquals(1, countFiles(output.resolve("bindings")));
        assertEquals(1, countFiles(output.resolve("results")));
        assertEquals(1, countFiles(output.resolve("resolved-inputs")));
        assertEquals(1, countFiles(output.resolve("gates")));
    }

    @Test
    void controlGateReturnsBlockedExitAndInconclusiveResultWhenEvidenceIsUnavailable() throws Exception {
        Path bindings = temp.resolve("bindings-missing.json");
        Path inputs = temp.resolve("inputs-missing.json");
        Path output = temp.resolve("gate-output-missing");
        Files.writeString(bindings, """
                [{"bindingRef":"BIND-2","scopeRef":"S05:r1","gateRef":"S05-delivery",
                  "controlRef":"CTRL-EXACT-BINDING-001","subjectRef":"candidate:r1","evidenceRefs":["EV-2"]}]
                """, StandardCharsets.UTF_8);
        Files.writeString(inputs, "{}", StandardCharsets.UTF_8);

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int status = EngineeringControlCli.run(new String[]{
                "control-gate", "--gate", "S05-delivery", "--bindings", bindings.toString(),
                "--inputs", inputs.toString(), "--out", output.toString(), "--execution-ref", "RUN-2"},
                new PrintStream(stdout), new PrintStream(stderr));

        assertEquals(1, status);
        assertTrue(JSON.readTree(stdout.toString(StandardCharsets.UTF_8)).path("proceed").asBoolean() == false);
        Path result;
        try (var files = Files.list(output.resolve("results"))) {
            result = files.findFirst().orElseThrow();
        }
        assertEquals("INCONCLUSIVE", JSON.readTree(Files.readString(result)).path("outcome").asText());
    }

    private static long countFiles(Path directory) throws Exception {
        try (var files = Files.list(directory)) {
            return files.count();
        }
    }
}

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
}

package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioSearchIntentProposalGeneratorTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String REV = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String SEM_SHA = ScenarioSearchIntentProposalGenerator.SEMANTICS_SHA256;
    private static final String MANIFEST_SHA = ScenarioSearchIntentProposalGenerator.ACCEPTANCE_MANIFEST_SHA256;
    private static final List<String> ACCEPTED = List.of(
            "HYP-SCENARIO-001", "HYP-SCENARIO-002", "HYP-SCENARIO-003", "HYP-SCENARIO-004",
            "HYP-SCENARIO-005", "HYP-SCENARIO-006", "HYP-SCENARIO-011", "HYP-SCENARIO-007",
            "HYP-SCENARIO-008", "HYP-SCENARIO-009");
    @TempDir Path temp;

    @Test void frozenRunEmitsExactlyOneProposalPerAcceptedScenario() throws Exception {
        Path output = temp.resolve("out");
        var result = ScenarioSearchIntentProposalGenerator.generate(Path.of("."), output);

        assertEquals(10, result.proposalCount());
        JsonNode artifact = JSON.readTree(output.resolve(ScenarioSearchIntentProposalGenerator.ARTIFACT_PATH).toFile());
        assertEquals("PROPOSAL_ONLY", artifact.path("authority").asText());
        assertFalse(artifact.path("semantic_publication_allowed").asBoolean());
        assertEquals(REV, artifact.path("source_revision").asText());
        assertEquals(SEM_SHA, artifact.path("semantics_sha256").asText());
        assertEquals(MANIFEST_SHA, artifact.path("acceptance_manifest_sha256").asText());
        assertEquals(ACCEPTED.size(), artifact.path("proposals").size());
        for (int i = 0; i < ACCEPTED.size(); i++) {
            assertEquals(ACCEPTED.get(i), artifact.path("proposals").get(i).path("scenarioId").asText());
        }
        JsonNode evidence = JSON.readTree(output.resolve(ScenarioSearchIntentProposalGenerator.EVIDENCE_PATH).toFile());
        assertEquals(result.artifactSha256(), evidence.path("artifact").path("sha256").asText());
        assertEquals(SEM_SHA, evidence.path("inputs")
                .path(ScenarioSearchIntentProposalGenerator.SEMANTICS_PATH).asText());
        assertEquals(MANIFEST_SHA, evidence.path("inputs")
                .path(ScenarioSearchIntentProposalGenerator.ACCEPTANCE_MANIFEST_PATH).asText());
    }

    @Test void everyProposalHasNonEmptyRetrievalFieldsAndExactDigestBinding() throws Exception {
        Path output = temp.resolve("out");
        ScenarioSearchIntentProposalGenerator.generate(Path.of("."), output);
        JsonNode artifact = JSON.readTree(output.resolve(ScenarioSearchIntentProposalGenerator.ARTIFACT_PATH).toFile());

        for (JsonNode record : artifact.path("proposals")) {
            String id = record.path("scenarioId").asText();
            assertTrue(record.path("capabilityId").asText().startsWith("HYP-CAPABILITY-"), id);
            assertFalse(record.path("action").asText().isBlank(), id);
            assertFalse(record.path("entity").asText().isBlank(), id);
            assertFalse(record.path("conditions").isEmpty(), id);
            assertFalse(record.path("aliases").isEmpty(), id);
            assertEquals("PROPOSAL_ONLY", record.path("authority").asText(), id);
            assertEquals(REV, record.path("sourceRevision").asText(), id);
            assertEquals(SEM_SHA, record.path("semanticsDigest").asText(), id);
            assertTrue(record.path("rationale").asText().contains("retrieval aid"), id);
        }
    }

    @Test void doubleRunIntoSeparateRootsIsByteIdenticalWithMatchingDigests() throws Exception {
        Path first = temp.resolve("first"), second = temp.resolve("second");
        var one = ScenarioSearchIntentProposalGenerator.generate(Path.of("."), first);
        var two = ScenarioSearchIntentProposalGenerator.generate(Path.of("."), second);
        assertEquals(one.artifactSha256(), two.artifactSha256());
        assertEquals(one.evidenceSha256(), two.evidenceSha256());
        assertArrayEquals(Files.readAllBytes(first.resolve(ScenarioSearchIntentProposalGenerator.ARTIFACT_PATH)),
                Files.readAllBytes(second.resolve(ScenarioSearchIntentProposalGenerator.ARTIFACT_PATH)));
        assertArrayEquals(Files.readAllBytes(first.resolve(ScenarioSearchIntentProposalGenerator.EVIDENCE_PATH)),
                Files.readAllBytes(second.resolve(ScenarioSearchIntentProposalGenerator.EVIDENCE_PATH)));
    }

    @Test void existingIdenticalOutputsAreReusedButChangedBytesFailClosed() throws Exception {
        Path output = temp.resolve("out");
        var first = ScenarioSearchIntentProposalGenerator.generate(Path.of("."), output);
        var second = ScenarioSearchIntentProposalGenerator.generate(Path.of("."), output);
        assertEquals(first.artifactSha256(), second.artifactSha256());
        Files.writeString(output.resolve(ScenarioSearchIntentProposalGenerator.ARTIFACT_PATH),
                "\n", StandardOpenOption.APPEND);
        assertThrows(RuntimeContractException.class,
                () -> ScenarioSearchIntentProposalGenerator.generate(Path.of("."), output));
    }

    @Test void sealedInputDigestMismatchFailsClosed() throws Exception {
        Path root = temp.resolve("root");
        copySealedInputs(root);
        Files.writeString(root.resolve(ScenarioSearchIntentProposalGenerator.SEMANTICS_PATH),
                "\n", StandardOpenOption.APPEND);
        assertThrows(RuntimeContractException.class,
                () -> ScenarioSearchIntentProposalGenerator.generate(root, temp.resolve("out")));
    }

    @Test void mechanicalKeywordMappingProducesDeterministicRetrievalTerms() {
        JsonNode scenario = scenario("HYP-SCENARIO-006", "拒絕同一飼主名下的重複寵物名稱",
                "一位飼主名下已有某個名稱的寵物。", "使用者嘗試為同一飼主新增相同名稱的另一筆寵物資料。",
                List.of("系統不完成造成同名的變更。"));

        ObjectNode record = ScenarioSearchIntentProposalGenerator.proposalRecord(
                "HYP-CAPABILITY-003", scenario, REV, SEM_SHA);

        assertEquals("REJECT", record.path("action").asText());
        assertEquals("PET", record.path("entity").asText());
        assertTrue(toList(record.path("conditions")).contains("duplicate-name-guard"));
        assertFalse(record.path("aliases").isEmpty());
    }

    @Test void scenarioTextWithoutAnyKnownKeywordFailsClosed() {
        JsonNode scenario = scenario("HYP-SCENARIO-999", "完全沒有已知關鍵字的情境描述",
                "沒有任何已知關鍵字。", "沒有任何已知關鍵字。", List.of("沒有任何已知關鍵字。"));
        assertThrows(RuntimeContractException.class, () -> ScenarioSearchIntentProposalGenerator
                .proposalRecord("HYP-CAPABILITY-001", scenario, REV, SEM_SHA));
    }

    @Test void mixedRevisionAndNonProposalAuthorityFailClosed() {
        JsonNode scenario = scenario("HYP-SCENARIO-001", "依姓氏找到一筆或多筆飼主資料",
                "系統中已有姓氏符合查詢條件的飼主資料。", "使用者以姓氏輸入查詢條件。",
                List.of("系統顯示符合條件的飼主。"));
        ObjectNode record = ScenarioSearchIntentProposalGenerator.proposalRecord(
                "HYP-CAPABILITY-001", scenario, REV, SEM_SHA);
        ScenarioSearchIntentProposalGenerator.validateProposalRecord(record, REV, SEM_SHA);

        record.put("sourceRevision", "1".repeat(40));
        assertThrows(RuntimeContractException.class, () -> ScenarioSearchIntentProposalGenerator
                .validateProposalRecord(record, REV, SEM_SHA));
        record.put("sourceRevision", REV);
        record.put("semanticsDigest", "2".repeat(64));
        assertThrows(RuntimeContractException.class, () -> ScenarioSearchIntentProposalGenerator
                .validateProposalRecord(record, REV, SEM_SHA));
        record.put("semanticsDigest", SEM_SHA);
        record.put("authority", "ACCEPTED_PRODUCT_KNOWLEDGE");
        assertThrows(RuntimeContractException.class, () -> ScenarioSearchIntentProposalGenerator
                .validateProposalRecord(record, REV, SEM_SHA));
        record.put("authority", "PROPOSAL_ONLY");
        record.put("action", "");
        assertThrows(RuntimeContractException.class, () -> ScenarioSearchIntentProposalGenerator
                .validateProposalRecord(record, REV, SEM_SHA));
    }

    @Test void evaluatorVocabularyInAnyEmittedFieldFailsClosed() {
        JsonNode scenario = scenario("HYP-SCENARIO-001", "依姓氏找到一筆或多筆飼主資料",
                "系統中已有姓氏符合查詢條件的飼主資料。", "使用者以姓氏輸入查詢條件。",
                List.of("系統顯示符合條件的飼主。"));
        assertThrows(RuntimeContractException.class, () -> ScenarioSearchIntentProposalGenerator
                .proposalRecord("evaluator-gold", scenario, REV, SEM_SHA));
        ObjectNode record = ScenarioSearchIntentProposalGenerator.proposalRecord(
                "HYP-CAPABILITY-001", scenario, REV, SEM_SHA);
        record.put("rationale", "derived from ground truth mapping");
        assertThrows(RuntimeContractException.class, () -> ScenarioSearchIntentProposalGenerator
                .validateProposalRecord(record, REV, SEM_SHA));
    }

    private static List<String> toList(JsonNode array) {
        return java.util.stream.StreamSupport.stream(array.spliterator(), false).map(JsonNode::asText).toList();
    }

    private static JsonNode scenario(String id, String title, String given, String when, List<String> then) {
        ObjectNode node = JSON.createObjectNode();
        node.put("scenario_id", id).put("title", title);
        node.putArray("given").add(given);
        node.put("when", when);
        var thenNode = node.putArray("then");
        then.forEach(thenNode::add);
        return node;
    }

    private static void copySealedInputs(Path root) throws Exception {
        for (String path : ScenarioSearchIntentProposalGenerator.sealedInputs().keySet()) {
            Path from = Path.of(".").resolve(path), to = root.resolve(path);
            Files.createDirectories(to.getParent());
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

package com.featuredeliveryintelligence.fdi.validation.scenarioforward;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ScenarioAcceptedSemanticsRevisionFourArtifactTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path PACKET = ROOT.resolve(
            "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01");

    @Test
    void revisionFourFreezesExactlyTheReviewedRevisionTwoSet() throws Exception {
        JsonNode semantics = read("accepted-semantics-004.json");
        JsonNode manifest = read("acceptance-manifest-004.json");
        JsonNode evidence = read("accepted-semantics-004-evidence.json");
        JsonNode authorization = read("freeze-authorization-binding-001.json");

        assertEquals("FROZEN", semantics.path("status").asText());
        assertEquals("HUMAN_REVIEWER", semantics.path("owner").asText());
        assertEquals("FROZEN", manifest.path("status").asText());
        assertEquals("HUMAN_REVIEWER", manifest.path("contract_owner_role").asText());
        assertEquals(0, manifest.path("remaining_pending_decisions").asInt());
        assertFalse(manifest.path("semantic_publication_allowed").asBoolean());
        assertFalse(manifest.path("product_truth_established").asBoolean());

        assertEquals(Set.of("HYP-CAPABILITY-001", "HYP-CAPABILITY-002", "HYP-CAPABILITY-003",
                "HYP-CAPABILITY-004", "HYP-CAPABILITY-005"), ids(semantics.path("capabilities"), "capability_id"));
        Set<String> scenarios = new HashSet<>();
        semantics.path("capabilities").forEach(capability ->
                scenarios.addAll(ids(capability.path("scenarios"), "scenario_id")));
        assertEquals(Set.of("HYP-SCENARIO-001", "HYP-SCENARIO-002", "HYP-SCENARIO-003",
                "HYP-SCENARIO-004", "HYP-SCENARIO-005", "HYP-SCENARIO-006",
                "HYP-SCENARIO-007", "HYP-SCENARIO-008", "HYP-SCENARIO-009",
                "HYP-SCENARIO-011"), scenarios);

        assertEquals(sha256("proposal-revision-002.json"), manifest.path("proposal_sha256").asText());
        assertArtifact(manifest.path("decision_artifact"), "review-decisions-004.json");
        assertArtifact(manifest.path("semantics_artifact"), "accepted-semantics-004.json");
        assertArtifact(manifest.path("authorization_artifact"), "freeze-authorization-binding-001.json");
        assertArtifact(authorization.path("source_authorization_artifact"),
                "ai-provisional-review-acceptance-001.json");
        assertArtifact(authorization.path("review_artifact"), "ai-provisional-review-001.json");
        assertArtifact(authorization.path("decision_artifact"), "review-decisions-004.json");
        assertEquals(2, authorization.path("proposal").path("revision").asInt());
        assertArtifact(authorization.path("proposal"), "proposal-revision-002.json");
        assertEquals(8, authorization.path("authorized_actions").size());
        assertArtifact(evidence.path("proposal"), "proposal-revision-002.json");
        assertArtifact(evidence.path("review"), "review-decisions-004.json");
        assertArtifact(evidence.path("manifest"), "acceptance-manifest-004.json");
        assertArtifact(evidence.path("semantics"), "accepted-semantics-004.json");
        assertEquals("CONTRACT_VALID", evidence.path("forward_gate").path("result").asText());
        assertFalse(evidence.path("authority").path("product_truth_established").asBoolean());
        assertFalse(evidence.path("authority").path("semantic_publication_allowed").asBoolean());
    }

    private static void assertArtifact(JsonNode binding, String name) throws Exception {
        assertEquals(ROOT.relativize(PACKET.resolve(name)).toString(), binding.path("path").asText());
        assertEquals(sha256(name), binding.path("sha256").asText());
    }

    private static Set<String> ids(JsonNode array, String field) {
        Set<String> ids = new HashSet<>();
        array.forEach(item -> ids.add(item.path(field).asText()));
        return ids;
    }

    private static JsonNode read(String name) throws Exception {
        return JSON.readTree(Files.readAllBytes(PACKET.resolve(name)));
    }

    private static String sha256(String name) throws Exception {
        StringBuilder result = new StringBuilder();
        for (byte value : MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(PACKET.resolve(name)))) {
            result.append(String.format("%02x", value));
        }
        return result.toString();
    }
}

package com.featuredeliveryintelligence.fdi.validation.scenarioreview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioReviewRevisionTwoArtifactTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path PACKET = ROOT.resolve(
            "validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01");

    @Test
    void revisionTwoAddsOnlySupplementalScenarioAndBindsEveryDecision() throws Exception {
        JsonNode original = read("proposal.json");
        Path proposalPath = PACKET.resolve("proposal-revision-002.json");
        JsonNode proposal = JSON.readTree(Files.readAllBytes(proposalPath));
        JsonNode review = read("review-decisions-004.json");

        assertEquals(1, original.path("proposal_revision").asInt());
        assertEquals(2, proposal.path("proposal_revision").asInt());
        assertEquals(2, review.path("proposal_revision").asInt());
        assertNotEquals(original.path("run_id").asText(), proposal.path("run_id").asText());
        assertEquals(original.path("generation_inputs"), proposal.path("generation_inputs"));

        ObjectNode expected = (ObjectNode) original.deepCopy();
        expected.put("proposal_revision", 2);
        expected.put("run_id", proposal.path("run_id").asText());
        ((ArrayNode) expected.path("capability_proposals").get(2).path("scenarios"))
                .add(proposal.path("capability_proposals").get(2).path("scenarios").get(2));
        assertEquals(expected, proposal, "only revision metadata and scenario 011 may change");

        JsonNode added = proposal.path("capability_proposals").get(2).path("scenarios").get(2);
        assertEquals("HYP-SCENARIO-011", added.path("scenario_id").asText());
        assertEquals("更新既有寵物資料", added.path("title").asText());

        String digest = sha256(Files.readAllBytes(proposalPath));
        assertEquals(digest, review.path("proposal_sha256").asText());
        List<JsonNode> decisions = allDecisions(review);
        assertEquals(17, decisions.size());
        for (JsonNode decision : decisions) {
            assertTrue(decision.path("reviewer_identity").isTextual());
            assertTrue(decision.path("reviewed_at").isTextual());
            assertTrue(decision.path("reason").isTextual());
            assertEquals(2, decision.path("proposal_revision").asInt());
            assertEquals(digest, decision.path("proposal_sha256").asText());
        }

        Set<String> accepted = new HashSet<>();
        review.path("capability_proposals").forEach(capability -> {
            if ("ACCEPT".equals(capability.path("decision").path("action").asText())) {
                capability.path("scenarios").forEach(scenario -> {
                    if ("ACCEPT".equals(scenario.path("decision").path("action").asText())) {
                        accepted.add(scenario.path("scenario_id").asText());
                    }
                });
            }
        });
        assertEquals(Set.of("HYP-SCENARIO-001", "HYP-SCENARIO-002", "HYP-SCENARIO-003",
                "HYP-SCENARIO-004", "HYP-SCENARIO-005", "HYP-SCENARIO-006",
                "HYP-SCENARIO-007", "HYP-SCENARIO-008", "HYP-SCENARIO-009",
                "HYP-SCENARIO-011"), accepted);
        assertTrue(!accepted.contains("HYP-SCENARIO-010"));
    }

    @Test
    void evidenceProvesHistoricalRenderBindingWithoutChangingCurrentSkill(@TempDir Path temp)
            throws Exception {
        JsonNode evidence = read("revision-002-render-evidence.json");
        JsonNode proposal = read("proposal-revision-002.json");
        assertEquals("PASS", evidence.path("status").asText());
        assertEquals("e4cd82a7f285f37d0a4b28766db4730ab1e4e7a73b42c7e77ad5e7625b081599",
                evidence.path("historical_skill_sha256").asText());
        assertEquals("341c264c660d12ef7d4b96dadf71de30239dc26f9adcda17d34cef15d9ec0de4",
                sha256(Files.readAllBytes(ROOT.resolve("skills/pkb001/pk-scenario-proposal/SKILL.md"))));
        assertEquals(0, evidence.path("java_cli_exit_code").asInt());
        assertEquals(sha256(Files.readAllBytes(PACKET.resolve("proposal.json"))),
                evidence.path("original_proposal_sha256").asText());
        assertEquals(sha256(Files.readAllBytes(PACKET.resolve("proposal-revision-002.json"))),
                evidence.path("proposal_sha256").asText());
        assertEquals(sha256(Files.readAllBytes(PACKET.resolve("review-revision-002.json"))),
                evidence.path("review_json_sha256").asText());
        assertEquals(sha256(Files.readAllBytes(PACKET.resolve("review-revision-002.md"))),
                evidence.path("review_markdown_sha256").asText());
        assertEquals(sha256(Files.readAllBytes(PACKET.resolve("review-decisions-004.json"))),
                evidence.path("review_decisions_sha256").asText());
        assertEquals("Supplemental proposal omitted confidence; revision 2 uses 0.0 to preserve "
                        + "conservative UNCALIBRATED_RANKING_HINT semantics.",
                evidence.path("scenario_011_confidence_basis").asText());
        assertEquals(0.0, proposal.path("capability_proposals").get(2).path("scenarios").get(2)
                .path("confidence").asDouble());

        copyToRoot(temp, ROOT.resolve("validation/pkb001/schemas/scenario-proposal.schema.json"),
                Path.of("validation/pkb001/schemas/scenario-proposal.schema.json"));
        Path isolatedProposal = copyToRoot(temp, PACKET.resolve("proposal-revision-002.json"),
                ROOT.relativize(PACKET.resolve("proposal-revision-002.json")));
        for (JsonNode input : proposal.path("generation_inputs")) {
            if (!"SCENARIO_SKILL".equals(input.path("kind").asText())) {
                Path relative = Path.of(input.path("path").asText());
                copyToRoot(temp, ROOT.resolve(relative), relative);
            }
        }
        Process historical = new ProcessBuilder("git", "show",
                "fc7b3046f8955138ec0fc7660667c63a5f17ccc9:skills/pkb001/pk-scenario-proposal/SKILL.md")
                .directory(ROOT.toFile()).start();
        byte[] historicalSkill = historical.getInputStream().readAllBytes();
        assertEquals(0, historical.waitFor());
        assertEquals(evidence.path("historical_skill_sha256").asText(), sha256(historicalSkill));
        Path isolatedSkill = temp.resolve("skills/pkb001/pk-scenario-proposal/SKILL.md");
        Files.createDirectories(isolatedSkill.getParent());
        Files.write(isolatedSkill, historicalSkill);

        ScenarioReview.ReviewRender rendered = ScenarioReview.renderReview(
                temp, proposal, isolatedProposal);
        assertEquals(read("review-revision-002.json"), rendered.review());
        assertEquals(Files.readString(PACKET.resolve("review-revision-002.md"),
                StandardCharsets.UTF_8), rendered.markdown());

        Set<String> accepted = new HashSet<>();
        ScenarioReview.acceptedScenarios(temp, read("review-decisions-004.json"))
                .forEach(item -> accepted.add(item.path("scenario_id").asText()));
        assertEquals(10, accepted.size());
        assertTrue(accepted.contains("HYP-SCENARIO-011"));
        assertTrue(!accepted.contains("HYP-SCENARIO-010"));
    }

    private static Path copyToRoot(Path root, Path source, Path relative) throws Exception {
        Path destination = root.resolve(relative);
        Files.createDirectories(destination.getParent());
        Files.copy(source, destination);
        return destination;
    }

    private static JsonNode read(String name) throws Exception {
        return JSON.readTree(Files.readAllBytes(PACKET.resolve(name)));
    }

    private static List<JsonNode> allDecisions(JsonNode review) {
        List<JsonNode> decisions = new ArrayList<>();
        review.path("capability_proposals").forEach(capability -> {
            decisions.add(capability.path("decision"));
            capability.path("scenarios").forEach(scenario -> decisions.add(scenario.path("decision")));
        });
        return decisions;
    }

    private static String sha256(byte[] bytes) throws Exception {
        StringBuilder result = new StringBuilder();
        for (byte value : MessageDigest.getInstance("SHA-256").digest(bytes)) {
            result.append(String.format("%02x", value));
        }
        return result.toString();
    }
}

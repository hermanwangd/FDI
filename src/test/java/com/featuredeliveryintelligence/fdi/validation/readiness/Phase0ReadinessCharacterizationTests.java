package com.featuredeliveryintelligence.fdi.validation.readiness;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ports the readiness-gate characterization corpus of
 * {@code tests/test_pkb001_phase0.py} and the seal mutation corpus of
 * {@code tests/test_pkb001_petclinic_evaluator_seal.py} to the Java
 * {@link Phase0Readiness} gate. Every rejected case from the Python corpus is
 * preserved without weakening.
 */
class Phase0ReadinessCharacterizationTests {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir Path temp;

    @Test
    void blocksWhenAnyPrerequisiteIsMissing() throws Exception {
        ObjectNode result = new Phase0Readiness().evaluate(temp, JSON.createObjectNode());

        assertEquals("BLOCKED", result.get("status").asText());
        assertEquals("NOT_READY", result.get("readiness_state").asText());
        ArrayNode prerequisites = (ArrayNode) result.get("prerequisites");
        assertEquals(5, prerequisites.size());
        for (int index = 0; index < 5; index++) {
            assertEquals("P0-0" + (index + 1), prerequisites.get(index).get("id").asText());
            assertEquals("MISSING", prerequisites.get(index).get("status").asText());
        }
        ObjectNode flags = (ObjectNode) result.get("readiness_flags");
        assertEquals(List.of("PRODUCT_SEMANTICS_FROZEN", "LIVE_GRAPHIFY_INTERFACE_VERIFIED",
                "PK_S1_EXECUTION_READY", "PK_S2_EXECUTION_READY", "CALIBRATION_DATASET_FROZEN",
                "GROUND_TRUTH_SEALED"), fieldNames(flags));
        for (JsonNode flag : flags) {
            assertFalse(flag.asBoolean());
        }
        assertEquals("UNVERIFIED", result.get("review_state").get("phase0_protocol_actors").asText());
    }

    @Test
    void rejectsUnfrozenProductSemantics() throws Exception {
        Path semantics = temp.resolve("product-semantics.json");
        Files.writeString(semantics, "{}");
        ObjectNode evidence = JSON.createObjectNode();
        ObjectNode productSemantics = evidence.putObject("product_semantics");
        productSemantics.put("status", "DRAFT");
        productSemantics.put("path", semantics.getFileName().toString());
        productSemantics.put("sha256", sha256(Files.readAllBytes(semantics)));

        ObjectNode result = new Phase0Readiness().evaluate(temp, evidence);

        assertEquals("MISMATCH", prerequisite(result, 0).get("status").asText());
        assertEquals("Product Semantics is not frozen by Product Team",
                prerequisite(result, 0).get("reason").asText());
    }

    @Test
    void isReadyOnlyWithAllVerifiedEvidence() throws Exception {
        ObjectNode result = new Phase0Readiness().evaluate(temp, fullEvidence());

        assertEquals("READY", result.get("status").asText());
        assertEquals("READY", result.get("readiness_state").asText());
        for (JsonNode item : result.get("prerequisites")) {
            assertEquals("SATISFIED", item.get("status").asText(), item.toString());
        }
        ObjectNode flags = (ObjectNode) result.get("readiness_flags");
        for (Map.Entry<String, JsonNode> flag : flags.properties()) {
            assertTrue(flag.getValue().asBoolean(), flag.getKey());
        }
        ObjectNode reviewState = (ObjectNode) result.get("review_state");
        assertEquals("INDEPENDENT_AI_AGENT_CONTEXTS",
                reviewState.get("phase0_protocol_actors").asText());
        assertTrue(reviewState.get("non_human_review_completed").asBoolean());
        assertEquals("PENDING_POST_GENERATION_SECTION_6",
                reviewState.get("human_review_status").asText());
    }

    @Test
    void rejectsUnboundGroundTruthDigest() throws Exception {
        ObjectNode evidence = JSON.createObjectNode();
        ObjectNode groundTruth = evidence.putObject("ground_truth");
        groundTruth.put("status", "SEALED");
        groundTruth.put("gold_sha256", "d".repeat(64));
        groundTruth.put("isolation_status", "VERIFIED");
        groundTruth.put("review_protocol_status", "FROZEN");
        ArrayNode reviewers = groundTruth.putArray("reviewers");
        reviewers.add("reviewer-a");
        reviewers.add("reviewer-b");
        groundTruth.set("judgment_vocabulary", vocabulary());

        ObjectNode result = new Phase0Readiness().evaluate(temp, evidence);

        assertEquals("MISMATCH", prerequisite(result, 4).get("status").asText());
        assertFalse(((ObjectNode) result.get("readiness_flags")).get("GROUND_TRUTH_SEALED").asBoolean());
    }

    @Test
    void rejectsGroundTruthWithoutMatchingSeal() throws Exception {
        Path gold = temp.resolve("gold.json");
        Files.writeString(gold, "{}");
        ObjectNode evidence = JSON.createObjectNode();
        ObjectNode groundTruth = evidence.putObject("ground_truth");
        groundTruth.put("status", "SEALED");
        groundTruth.put("gold_path", gold.getFileName().toString());
        groundTruth.put("gold_sha256", sha256(Files.readAllBytes(gold)));
        groundTruth.put("isolation_status", "VERIFIED");
        groundTruth.put("review_protocol_status", "FROZEN");
        ArrayNode reviewers = groundTruth.putArray("reviewers");
        reviewers.add("reviewer-a");
        reviewers.add("reviewer-b");
        groundTruth.set("judgment_vocabulary", vocabulary());

        ObjectNode result = new Phase0Readiness().evaluate(temp, evidence);

        assertEquals("MISMATCH", prerequisite(result, 4).get("status").asText());
    }

    @Test
    void rejectsUnregisteredSkillFiles() throws Exception {
        ObjectNode evidence = JSON.createObjectNode();
        ObjectNode skills = evidence.putObject("skills");
        for (String name : new String[] {"pk-s1", "pk-s2"}) {
            Path skill = temp.resolve(name).resolve("SKILL.md");
            Files.createDirectories(skill.getParent());
            Files.writeString(skill, "# Candidate only");
            skills.put(name.replace('-', '_') + "_path", name + "/SKILL.md");
        }

        ObjectNode result = new Phase0Readiness().evaluate(temp, evidence);

        assertEquals("MISMATCH", prerequisite(result, 2).get("status").asText());
        ObjectNode flags = (ObjectNode) result.get("readiness_flags");
        assertFalse(flags.get("PK_S1_EXECUTION_READY").asBoolean());
        assertFalse(flags.get("PK_S2_EXECUTION_READY").asBoolean());
    }

    @Test
    void pkS2RequiresFrozenDeliveryHistory() throws Exception {
        ObjectNode evidence = JSON.createObjectNode();
        ObjectNode skills = evidence.putObject("skills");
        for (String name : new String[] {"pk-s1", "pk-s2"}) {
            Path skill = temp.resolve(name).resolve("SKILL.md");
            Files.createDirectories(skill.getParent());
            Files.writeString(skill, "# Registered");
            skills.put(name.replace('-', '_') + "_path", name + "/SKILL.md");
        }
        skills.put("pk_s1_registration", "REGISTERED_NON_GOVERNING");
        skills.put("pk_s2_registration", "REGISTERED_NON_GOVERNING");

        ObjectNode result = new Phase0Readiness().evaluate(temp, evidence);

        ObjectNode flags = (ObjectNode) result.get("readiness_flags");
        assertTrue(flags.get("PK_S1_EXECUTION_READY").asBoolean());
        assertFalse(flags.get("PK_S2_EXECUTION_READY").asBoolean());
    }

    // --- Seal mutation corpus from tests/test_pkb001_petclinic_evaluator_seal.py ---

    @Test
    void rejectsProtocolActorNamesWithoutExplicitAgentContexts() throws Exception {
        SealFixture fixture = new SealFixture();
        ArrayNode reviewers = JSON.createArrayNode();
        reviewers.add("fresh-context-evaluator");
        reviewers.add("public-seam-integrity-verifier");
        fixture.seal.set("reviewers", reviewers);
        fixture.evidence.set("reviewers", reviewers);
        ObjectNode roles = JSON.createObjectNode();
        roles.put("fresh-context-evaluator", "EXPECTED_REALIZATION_AUTHOR");
        roles.put("public-seam-integrity-verifier", "DIGEST_ISOLATION_AND_RESOLUTION_VERIFIER");
        fixture.seal.set("reviewer_roles", roles);
        fixture.evidence.set("reviewer_roles", roles);
        fixture.rewriteSeal();

        assertGroundTruthRejected(fixture.evidence);
    }

    @Test
    void rejectsMissingReviewerRoles() throws Exception {
        SealFixture fixture = new SealFixture();
        fixture.seal.remove("reviewer_roles");
        fixture.evidence.remove("reviewer_roles");
        fixture.rewriteSeal();

        assertGroundTruthRejected(fixture.evidence);
    }

    @Test
    void rejectsDuplicateContextReviewerRoles() throws Exception {
        SealFixture fixture = new SealFixture();
        ObjectNode roles = (ObjectNode) fixture.evidence.get("reviewer_roles");
        List<String> identities = fieldNames(roles);
        ((ObjectNode) roles.get(identities.get(1))).put("context_id",
                ((ObjectNode) roles.get(identities.get(0))).get("context_id").asText());
        fixture.seal.set("reviewer_roles", JSON.readTree(roles.toString()));
        fixture.rewriteSeal();

        assertGroundTruthRejected(fixture.evidence);
    }

    @Test
    void rejectsNonIndependentReviewerRoles() throws Exception {
        SealFixture fixture = new SealFixture();
        ObjectNode roles = (ObjectNode) fixture.evidence.get("reviewer_roles");
        String first = fieldNames(roles).get(0);
        ((ObjectNode) roles.get(first)).put("independent_context", false);
        fixture.seal.set("reviewer_roles", JSON.readTree(roles.toString()));
        fixture.rewriteSeal();

        assertGroundTruthRejected(fixture.evidence);
    }

    @Test
    void rejectsAgentRolesThatDisagreeWithSeal() throws Exception {
        SealFixture fixture = new SealFixture();
        ObjectNode roles = (ObjectNode) fixture.evidence.get("reviewer_roles");
        String reviewer = fieldNames(roles).get(0);
        ((ObjectNode) roles.get(reviewer)).put("role", "ALTERNATE_AGENT_ROLE");

        assertGroundTruthRejected(fixture.evidence);
    }

    @Test
    void rejectsMissingCreationOrdering() throws Exception {
        SealFixture fixture = new SealFixture();
        fixture.seal.remove("creation_before_generation_ordering");
        fixture.evidence.remove("creation_before_generation_ordering");
        fixture.rewriteSeal();

        assertGroundTruthRejected(fixture.evidence);
    }

    @Test
    void rejectsWrongRuleCreationOrdering() throws Exception {
        SealFixture fixture = new SealFixture();
        ((ObjectNode) fixture.seal.get("creation_before_generation_ordering")).put("rule", "UNBOUND");
        ((ObjectNode) fixture.evidence.get("creation_before_generation_ordering")).put("rule", "UNBOUND");
        fixture.rewriteSeal();

        assertGroundTruthRejected(fixture.evidence);
    }

    @Test
    void rejectsCreationOrderingWhereGenerationStarted() throws Exception {
        SealFixture fixture = new SealFixture();
        ((ObjectNode) fixture.seal.get("creation_before_generation_ordering"))
                .put("valid_experiment_generation_started", true);
        ((ObjectNode) fixture.evidence.get("creation_before_generation_ordering"))
                .put("valid_experiment_generation_started", true);
        fixture.rewriteSeal();

        assertGroundTruthRejected(fixture.evidence);
    }

    @Test
    void rejectsCreationOrderingThatDisagreesWithSeal() throws Exception {
        SealFixture fixture = new SealFixture();
        ((ObjectNode) fixture.evidence.get("creation_before_generation_ordering"))
                .put("status", "MISMATCHED_EVIDENCE");

        assertGroundTruthRejected(fixture.evidence);
    }

    @Test
    void preservesHumanReviewAsPendingAfterGeneration() throws Exception {
        SealFixture humanTrue = new SealFixture();
        humanTrue.seal.put("human_review_completed", true);
        humanTrue.evidence.put("human_review_completed", true);
        humanTrue.rewriteSeal();
        assertGroundTruthRejected(humanTrue.evidence);

        SealFixture nonHumanFalse = new SealFixture();
        nonHumanFalse.seal.put("non_human_review_completed", false);
        nonHumanFalse.evidence.put("non_human_review_completed", false);
        nonHumanFalse.rewriteSeal();
        assertGroundTruthRejected(nonHumanFalse.evidence);

        SealFixture statusComplete = new SealFixture();
        statusComplete.seal.put("human_review_status", "COMPLETE");
        statusComplete.evidence.put("human_review_status", "COMPLETE");
        statusComplete.rewriteSeal();
        assertGroundTruthRejected(statusComplete.evidence);
    }

    private void assertGroundTruthRejected(ObjectNode evidence) throws Exception {
        ObjectNode wrapped = JSON.createObjectNode();
        wrapped.set("ground_truth", evidence);
        ObjectNode result = new Phase0Readiness().evaluate(temp, wrapped);
        assertEquals("MISMATCH", prerequisite(result, 4).get("status").asText(), result.toString());
        assertFalse(((ObjectNode) result.get("readiness_flags")).get("GROUND_TRUTH_SEALED").asBoolean());
    }

    // --- fixtures ---

    /** Ports the {@code full ready evidence} fixture of test_pkb001_phase0.py. */
    private ObjectNode fullEvidence() throws Exception {
        Path framework = temp.resolve("product-semantics.json");
        Files.writeString(framework, "{\"capabilities\": []}");
        for (String name : new String[] {"pk-s1", "pk-s2"}) {
            Path skill = temp.resolve(name).resolve("SKILL.md");
            Files.createDirectories(skill.getParent());
            Files.writeString(skill, "# " + name.toUpperCase());
        }
        Path history = temp.resolve("delivery-history.json");
        Files.writeString(history, "{\"episodes\": []}");
        Path gold = temp.resolve("gold-mappings.json");
        Files.writeString(gold, "{\"mappings\": []}");

        List<String> reviewers = List.of(
                "agent-context:expected-realization-author",
                "agent-context:seal-integrity-verifier");
        ObjectNode roles = JSON.createObjectNode();
        roles.set(reviewers.get(0), agentRole(reviewers.get(0), "EXPECTED_REALIZATION_AUTHOR"));
        roles.set(reviewers.get(1), agentRole(reviewers.get(1),
                "DIGEST_ISOLATION_AND_RESOLUTION_VERIFIER"));
        ObjectNode ordering = JSON.createObjectNode();
        ordering.put("status", "VERIFIED");
        ordering.put("rule", "SEALED_BEFORE_VALID_EXPERIMENT_GENERATION");
        ordering.put("valid_experiment_generation_started", false);

        ObjectNode seal = JSON.createObjectNode();
        seal.put("status", "SEALED");
        seal.put("gold_path", "gold-mappings.json");
        seal.put("gold_sha256", sha256(Files.readAllBytes(gold)));
        seal.put("isolation_status", "VERIFIED");
        seal.put("review_protocol_status", "FROZEN");
        ArrayNode sealReviewers = seal.putArray("reviewers");
        reviewers.forEach(sealReviewers::add);
        seal.set("reviewer_roles", roles);
        seal.set("judgment_vocabulary", vocabulary());
        seal.set("creation_before_generation_ordering", ordering);
        seal.put("human_review_completed", false);
        seal.put("non_human_review_completed", true);
        seal.put("human_review_status", "PENDING_POST_GENERATION_SECTION_6");
        Path sealPath = temp.resolve("ground-truth-seal.json");
        Files.writeString(sealPath, JSON.writeValueAsString(seal));

        ObjectNode evidence = JSON.createObjectNode();
        ObjectNode authority = evidence.putObject("product_semantics");
        authority.put("status", "FROZEN");
        authority.put("path", framework.getFileName().toString());
        authority.put("sha256", sha256(Files.readAllBytes(framework)));
        authority.put("owner", "PRODUCT_TEAM");

        ObjectNode graphify = evidence.putObject("graphify");
        graphify.put("result", "EXACTLY_BOUND");
        graphify.put("queryable", true);
        graphify.put("runtime_identity", "graphify-local");
        graphify.put("runtime_version", "1.0.0");
        graphify.put("transport", "MCP");
        graphify.put("wire_version", "mcp-1");
        ArrayNode operations = graphify.putArray("supported_operations");
        operations.add("native-node");
        operations.add("native-path");
        graphify.put("exact_revision_opened", true);
        graphify.put("source_location_provenance", "file:///frozen/repo");
        ObjectNode proof = graphify.putObject("structural_proof");
        proof.put("node_query", true);
        proof.put("path_query", true);
        ObjectNode binding = graphify.putObject("snapshot_binding");
        binding.put("requested_revision", "a".repeat(40));
        binding.put("indexed_revision", "a".repeat(40));
        graphify.put("graph_sha256", "b".repeat(64));
        graphify.put("input_policy_sha256", "c".repeat(64));

        ObjectNode skills = evidence.putObject("skills");
        skills.put("pk_s1_path", "pk-s1/SKILL.md");
        skills.put("pk_s2_path", "pk-s2/SKILL.md");
        skills.put("pk_s1_registration", "REGISTERED_NON_GOVERNING");
        skills.put("pk_s2_registration", "REGISTERED_NON_GOVERNING");
        skills.put("delivery_history_path", "delivery-history.json");
        skills.put("delivery_history_sha256", sha256(Files.readAllBytes(history)));
        skills.put("delivery_history_status", "FROZEN");
        skills.put("post_cutoff_knowledge_policy", "EXCLUDE_AFTER_CUTOFF");

        ObjectNode calibration = evidence.putObject("calibration");
        calibration.put("status", "FROZEN");
        calibration.put("resource_policy_status", "FROZEN");
        calibration.put("post_cutoff_knowledge_policy", "EXCLUDE_AFTER_CUTOFF");

        ObjectNode groundTruth = evidence.putObject("ground_truth");
        groundTruth.put("status", "SEALED");
        groundTruth.put("gold_path", "gold-mappings.json");
        groundTruth.put("gold_sha256", sha256(Files.readAllBytes(gold)));
        groundTruth.put("seal_path", "ground-truth-seal.json");
        groundTruth.put("seal_sha256", sha256(Files.readAllBytes(sealPath)));
        groundTruth.put("isolation_status", "VERIFIED");
        groundTruth.put("review_protocol_status", "FROZEN");
        ArrayNode evidenceReviewers = groundTruth.putArray("reviewers");
        reviewers.forEach(evidenceReviewers::add);
        groundTruth.set("reviewer_roles", roles);
        groundTruth.set("judgment_vocabulary", vocabulary());
        groundTruth.set("creation_before_generation_ordering", ordering);
        groundTruth.put("human_review_completed", false);
        groundTruth.put("non_human_review_completed", true);
        groundTruth.put("human_review_status", "PENDING_POST_GENERATION_SECTION_6");
        return evidence;
    }

    private ObjectNode agentRole(String reviewer, String role) {
        ObjectNode node = JSON.createObjectNode();
        node.put("actor_type", "AI_AGENT_CONTEXT");
        node.put("context_id", reviewer);
        node.put("independent_context", true);
        node.put("role", role);
        return node;
    }

    private ArrayNode vocabulary() {
        ArrayNode node = JSON.createArrayNode();
        node.add("ACCEPT");
        node.add("RENAME");
        node.add("MERGE");
        node.add("SPLIT");
        node.add("REJECT");
        node.add("ADD_MISSING");
        return node;
    }

    /** Ports the {@code ground_truth_fixture} of test_pkb001_petclinic_evaluator_seal.py. */
    private final class SealFixture {
        final ObjectNode seal;
        final ObjectNode evidence;
        final Path goldPath = temp.resolve("gold.json");
        final Path sealPath = temp.resolve("seal.json");

        SealFixture() throws Exception {
            Files.writeString(goldPath, "{}");
            List<String> reviewers = List.of(
                    "agent-context:expected-realization-author",
                    "agent-context:seal-integrity-verifier");
            ObjectNode roles = JSON.createObjectNode();
            roles.set(reviewers.get(0), agentRole(reviewers.get(0), "EXPECTED_REALIZATION_AUTHOR"));
            roles.set(reviewers.get(1), agentRole(reviewers.get(1),
                    "DIGEST_ISOLATION_AND_RESOLUTION_VERIFIER"));
            ObjectNode ordering = JSON.createObjectNode();
            ordering.put("status", "VERIFIED");
            ordering.put("rule", "SEALED_BEFORE_VALID_EXPERIMENT_GENERATION");
            ordering.put("valid_experiment_generation_started", false);
            seal = JSON.createObjectNode();
            seal.put("status", "SEALED");
            seal.put("gold_path", goldPath.getFileName().toString());
            seal.put("gold_sha256", sha256(Files.readAllBytes(goldPath)));
            seal.put("isolation_status", "VERIFIED");
            seal.put("review_protocol_status", "FROZEN");
            ArrayNode sealReviewers = seal.putArray("reviewers");
            reviewers.forEach(sealReviewers::add);
            seal.set("reviewer_roles", roles);
            seal.set("judgment_vocabulary", vocabulary());
            seal.set("creation_before_generation_ordering", ordering);
            seal.put("human_review_completed", false);
            seal.put("non_human_review_completed", true);
            seal.put("human_review_status", "PENDING_POST_GENERATION_SECTION_6");
            evidence = (ObjectNode) JSON.readTree(seal.toString());
            evidence.put("seal_path", sealPath.getFileName().toString());
            rewriteSeal();
        }

        void rewriteSeal() throws Exception {
            Files.writeString(sealPath, JSON.writeValueAsString(seal));
            evidence.put("seal_sha256", sha256(Files.readAllBytes(sealPath)));
        }
    }

    private static JsonNode prerequisite(ObjectNode result, int index) {
        return result.get("prerequisites").get(index);
    }

    private static List<String> fieldNames(ObjectNode node) {
        List<String> names = new java.util.ArrayList<>();
        Iterator<String> iterator = node.fieldNames();
        while (iterator.hasNext()) {
            names.add(iterator.next());
        }
        return names;
    }

    static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}

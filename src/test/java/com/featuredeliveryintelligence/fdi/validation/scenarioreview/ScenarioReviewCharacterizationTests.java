package com.featuredeliveryintelligence.fdi.validation.scenarioreview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ports the pinned characterization cases of
 * {@code tests/test_pkb001_scenario_review.py} to the Java engine: exact
 * binding verification, resolved evidence summaries, fail-closed reason
 * vocabulary, fail-closed renderer, decision binding/filtering, exclusive
 * output creation with rollback, and the Python-exact {@code %.4f}, deep
 * equality, and timestamp behaviors.
 */
public class ScenarioReviewCharacterizationTests {
    static final ObjectMapper JSON = new ObjectMapper();
    static final JsonNodeFactory NODE = JsonNodeFactory.instance;
    static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";

    @TempDir Path temp;

    // ------------------------------------------------------------------
    // Fixture (mirrors proposal_root in the Python suite)
    // ------------------------------------------------------------------

    public static final class Fixture {
        public Path root;
        public Path proposalPath;
        public ObjectNode proposal;
        public String graphDigest;
        public String bindingDigest;
        public String historyDigest;
        public String skillDigest;
    }

    public static Fixture buildFixture(Path root) throws IOException {
        Fixture fx = new Fixture();
        fx.root = root;
        Files.createDirectories(root.resolve("validation/pkb001/schemas"));
        Files.createDirectories(root.resolve("skills/pkb001/pk-scenario-proposal"));
        Files.write(root.resolve("validation/pkb001/schemas/scenario-proposal.schema.json"),
                Files.readAllBytes(Path.of("validation/pkb001/schemas/scenario-proposal.schema.json")));
        fx.skillDigest = write(root, "skills/pkb001/pk-scenario-proposal/SKILL.md",
                Files.readAllBytes(Path.of("skills/pkb001/pk-scenario-proposal/SKILL.md")));
        fx.graphDigest = write(root, "validation/pkb001/artifacts/graph.json", graphNode());
        fx.bindingDigest = write(root, "validation/pkb001/runtime/binding.json", bindingNode(fx.graphDigest));
        fx.historyDigest = write(root, "validation/pkb001/datasets/history.json", historyNode());
        fx.proposal = proposalNode(fx);
        fx.proposalPath = root.resolve("input/proposal.json");
        Files.createDirectories(fx.proposalPath.getParent());
        Files.write(fx.proposalPath, (JSON.writerWithDefaultPrettyPrinter()
                .writeValueAsString(fx.proposal) + "\n").getBytes(StandardCharsets.UTF_8));
        return fx;
    }

    static ObjectNode graphNode() {
        ObjectNode node = NODE.objectNode();
        node.put("id", "owner-controller");
        node.put("label", "OwnerController");
        node.put("source_file", "src/main/java/example/OwnerController.java");
        node.put("source_location", "class OwnerController");
        ObjectNode graph = NODE.objectNode();
        graph.putArray("nodes").add(node);
        graph.putArray("links");
        return graph;
    }

    static ObjectNode bindingNode(String graphDigest) {
        ObjectNode snapshot = NODE.objectNode();
        snapshot.put("requested_revision", REVISION);
        snapshot.put("indexed_revision", REVISION);
        snapshot.put("graph_sha256", graphDigest);
        ObjectNode binding = NODE.objectNode();
        binding.put("result", "EXACTLY_BOUND");
        binding.set("snapshot_binding", snapshot);
        binding.put("graph_sha256", graphDigest);
        return binding;
    }

    static ObjectNode historyNode() {
        ObjectNode commit = NODE.objectNode();
        commit.put("commit_sha", "b".repeat(40));
        commit.put("subject", "Add owner lookup behavior");
        commit.put("committed_at", "2020-01-02T03:04:05Z");
        commit.putArray("changed_paths").add("src/main/java/example/OwnerController.java");
        ObjectNode history = NODE.objectNode();
        history.put("status", "FROZEN");
        history.put("source_commit_sha", REVISION);
        history.put("history_cutoff", "2026-08-26T10:57:54Z");
        history.put("post_cutoff_knowledge_policy", "EXCLUDE_AFTER_CUTOFF");
        history.putArray("commits").add(commit);
        history.putArray("pull_requests");
        return history;
    }

    static ObjectNode scenarioNode() {
        ObjectNode scenario = NODE.objectNode();
        scenario.put("scenario_id", "HYP-SCENARIO-001");
        scenario.put("title", "依姓名尋找飼主");
        scenario.putArray("given").add("使用者已進入飼主搜尋流程。");
        scenario.put("when", "使用者提交完整或部分姓名。");
        scenario.putArray("then").add("系統顯示符合條件的飼主。");
        scenario.put("scope", "REQUIRED_ACCEPTANCE");
        scenario.putArray("evidence_refs").add("EV-G-001").add("EV-H-001");
        scenario.put("inference_rationale", "可解析的節點與提交主旨共同支持此行為假說。");
        scenario.put("confidence", 0.73);
        scenario.put("confidence_interpretation", "UNCALIBRATED_RANKING_HINT");
        scenario.putArray("limitations").add("無法從靜態結構確認所有空結果呈現細節。");
        scenario.putNull("decision");
        return scenario;
    }

    static ObjectNode capabilityNode() {
        ObjectNode capability = NODE.objectNode();
        capability.put("capability_id", "HYP-CAPABILITY-001");
        capability.put("title", "飼主管理");
        capability.put("description", "讓使用者依姓名尋找飼主。");
        capability.putArray("includes").add("依完整或部分姓名搜尋");
        capability.putArray("excludes").add("變更醫療紀錄");
        capability.putArray("non_goals").add("推斷未觀察到的授權規則");
        capability.putArray("evidence_refs").add("EV-G-001").add("EV-H-001");
        capability.put("inference_rationale", "結構與交付紀錄皆顯示飼主搜尋行為。");
        capability.put("confidence", 0.76);
        capability.put("confidence_interpretation", "UNCALIBRATED_RANKING_HINT");
        capability.putArray("limitations").add("靜態證據無法證明完整產品意圖。");
        capability.putNull("decision");
        capability.putArray("scenarios").add(scenarioNode());
        return capability;
    }

    static ObjectNode proposalNode(Fixture d) {
        ObjectNode proposal = NODE.objectNode();
        proposal.put("schema_version", "pkb001.scenario-proposal.v0.1");
        proposal.put("artifact_kind", "SCENARIO_PROPOSAL");
        proposal.put("run_id", "pkb001-scenario-review-test-001");
        proposal.put("proposal_revision", 1);
        proposal.put("authority", "PROPOSAL_ONLY");
        proposal.put("scenario_status", "UNREVIEWED");
        proposal.put("review_language", "zh-TW");
        proposal.put("source_revision", REVISION);
        proposal.put("graph_sha256", d.graphDigest);
        proposal.put("history_cutoff", "2026-08-26T10:57:54Z");
        ObjectNode context = NODE.objectNode();
        context.put("generator_role", "SCENARIO_PROPOSAL_AGENT");
        context.put("generator_identity", "isolated-generation-test");
        context.put("evaluator_gold_access", "PROHIBITED");
        context.put("accepted_forward_semantics_access", "PROHIBITED");
        context.put("post_generation_judgments_access", "PROHIBITED");
        proposal.set("generation_context", context);
        ObjectNode exposure = NODE.objectNode();
        exposure.put("technical_evidence_visible", true);
        exposure.put("content_level_arm_anonymity", "NOT_CLAIMED");
        proposal.set("reviewer_exposure", exposure);
        proposal.put("experiment_limitation",
                "RECONSTRUCTION_CONSISTENCY_NOT_INDEPENDENT_PRODUCT_VALIDATION");
        ArrayNode inputs = proposal.putArray("generation_inputs");
        inputs.add(inputNode("GRAPHIFY_BINDING", "validation/pkb001/runtime/binding.json", d.bindingDigest));
        inputs.add(inputNode("FROZEN_GRAPH", "validation/pkb001/artifacts/graph.json", d.graphDigest));
        inputs.add(inputNode("DELIVERY_HISTORY", "validation/pkb001/datasets/history.json", d.historyDigest));
        inputs.add(inputNode("SCENARIO_SKILL", "skills/pkb001/pk-scenario-proposal/SKILL.md", d.skillDigest));
        ObjectNode channels = NODE.objectNode();
        channels.set("STRUCTURAL", channelNode("AVAILABLE"));
        channels.set("DELIVERY_HISTORY", channelNode("AVAILABLE"));
        proposal.set("channel_availability", channels);
        ArrayNode catalog = proposal.putArray("evidence_catalog");
        catalog.add(evidenceNode("EV-G-001", "STRUCTURAL",
                "validation/pkb001/artifacts/graph.json", d.graphDigest, "/nodes/0"));
        catalog.add(evidenceNode("EV-H-001", "DELIVERY_HISTORY",
                "validation/pkb001/datasets/history.json", d.historyDigest, "/commits/0"));
        proposal.putArray("capability_proposals").add(capabilityNode());
        return proposal;
    }

    static ObjectNode inputNode(String kind, String path, String digest) {
        ObjectNode input = NODE.objectNode();
        input.put("kind", kind);
        input.put("path", path);
        input.put("sha256", digest);
        return input;
    }

    static ObjectNode channelNode(String status) {
        ObjectNode channel = NODE.objectNode();
        channel.put("status", status);
        channel.putNull("reason");
        return channel;
    }

    static ObjectNode evidenceNode(String id, String channel, String path, String digest, String pointer) {
        ObjectNode evidence = NODE.objectNode();
        evidence.put("evidence_id", id);
        evidence.put("channel", channel);
        evidence.put("artifact_path", path);
        evidence.put("artifact_sha256", digest);
        evidence.put("json_pointer", pointer);
        return evidence;
    }

    static String write(Path root, String relative, JsonNode value) throws IOException {
        return write(root, relative,
                (JSON.writeValueAsString(value) + "\n").getBytes(StandardCharsets.UTF_8));
    }

    static String write(Path root, String relative, byte[] data) throws IOException {
        Path path = root.resolve(relative);
        Files.createDirectories(path.getParent());
        Files.write(path, data);
        return sha256(data);
    }

    static String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder hex = new StringBuilder();
            for (byte value : digest.digest(data)) {
                hex.append(Character.forDigit((value >> 4) & 0xF, 16));
                hex.append(Character.forDigit(value & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private Fixture fixture() throws IOException {
        // Mirror pytest's resolved tmp_path: absolute paths passed to the
        // engine resolve lexically against the resolved root.
        return buildFixture(temp.toRealPath());
    }

    static ObjectNode deepCopy(ObjectNode node) {
        return node.deepCopy();
    }

    private void assertReason(Consumer<ObjectNode> mutation, String reason) throws IOException {
        Fixture fx = fixture();
        ObjectNode proposal = fx.proposal.deepCopy();
        mutation.accept(proposal);
        ScenarioReviewException caught = assertThrows(ScenarioReviewException.class,
                () -> ScenarioReview.validateProposal(fx.root, proposal, fx.proposalPath));
        assertTrue(caught.getReasons().contains(reason),
                "expected " + reason + " in " + caught.getReasons());
    }

    // ------------------------------------------------------------------
    // Pinned characterization cases
    // ------------------------------------------------------------------

    @Test
    void validatesExactBindingsAndResolvesNodeAndCommitSummaries() throws Exception {
        Fixture fx = fixture();
        ObjectNode validated = ScenarioReview.validateProposal(fx.root, fx.proposal, fx.proposalPath);
        assertEquals(REVISION, validated.get("source_revision").asText());
        JsonNode resolved = validated.get("resolved_evidence");
        assertEquals("節點 OwnerController（class OwnerController）", resolved.get("EV-G-001").asText());
        assertEquals("提交 " + "b".repeat(12) + "：Add owner lookup behavior",
                resolved.get("EV-H-001").asText());
    }

    @Test
    void rejectsAuthorityOverride() throws Exception {
        assertReason(p -> p.put("authority", "PRODUCT_TRUTH"), "SCHEMA_INVALID");
    }

    @Test
    void rejectsFrozenScenarioStatus() throws Exception {
        assertReason(p -> p.put("scenario_status", "FROZEN"), "SCHEMA_INVALID");
    }

    @Test
    void rejectsExtraGenerationInput() throws Exception {
        assertReason(p -> ((ArrayNode) p.get("generation_inputs")).add(inputNode(
                "EVALUATOR_GOLD", "gold.json", "a".repeat(64))), "GENERATION_INPUT_SET_INVALID");
    }

    @Test
    void rejectsForbiddenGenerationInputPath() throws Exception {
        assertReason(p -> ((ObjectNode) p.get("generation_inputs").get(0))
                .put("path", "validation/pkb001/evaluator/gold.json"), "FORBIDDEN_GENERATION_INPUT");
    }

    @Test
    void rejectsInputDigestMismatch() throws Exception {
        assertReason(p -> ((ObjectNode) p.get("generation_inputs").get(1))
                .put("sha256", "0".repeat(64)), "INPUT_DIGEST_MISMATCH");
    }

    @Test
    void rejectsGraphBindingMismatch() throws Exception {
        assertReason(p -> p.put("graph_sha256", "0".repeat(64)), "GRAPH_BINDING_MISMATCH");
    }

    @Test
    void rejectsRevisionBindingMismatch() throws Exception {
        assertReason(p -> p.put("source_revision", "a".repeat(40)), "REVISION_BINDING_MISMATCH");
    }

    @Test
    void rejectsHistoryCutoffMismatch() throws Exception {
        assertReason(p -> p.put("history_cutoff", "2026-08-27T00:00:00Z"), "HISTORY_CUTOFF_MISMATCH");
    }

    @Test
    void rejectsRootJsonPointer() throws Exception {
        assertReason(p -> ((ObjectNode) p.get("evidence_catalog").get(0))
                .put("json_pointer", "/"), "SCHEMA_INVALID");
    }

    @Test
    void rejectsUnresolvedJsonPointer() throws Exception {
        assertReason(p -> ((ObjectNode) p.get("evidence_catalog").get(0))
                .put("json_pointer", "/nodes/99"), "EVIDENCE_REFERENCE_UNRESOLVED");
    }

    @Test
    void rejectsNonAtomicJsonPointer() throws Exception {
        assertReason(p -> ((ObjectNode) p.get("evidence_catalog").get(1))
                .put("json_pointer", "/commits"), "EVIDENCE_REFERENCE_NOT_ATOMIC");
    }

    @Test
    void rejectsUnresolvedClaimReference() throws Exception {
        assertReason(p -> ((ObjectNode) p.get("capability_proposals").get(0).get("scenarios").get(0))
                .set("evidence_refs", NODE.arrayNode().add("EV-NOPE")), "EVIDENCE_REFERENCE_UNRESOLVED");
    }

    @Test
    void rejectsTechnicalTextInWhen() throws Exception {
        assertReason(p -> ((ObjectNode) p.get("capability_proposals").get(0).get("scenarios").get(0))
                .put("when", "呼叫 OwnerController.processFindForm()"),
                "TECHNICAL_IDENTIFIER_IN_BEHAVIOR");
    }

    @Test
    void rejectsTechnicalTextInDescription() throws Exception {
        assertReason(p -> ((ObjectNode) p.get("capability_proposals").get(0))
                .put("description", "讀取 src/main/App.java"), "TECHNICAL_IDENTIFIER_IN_BEHAVIOR");
    }

    @Test
    void rejectsOutOfRangeConfidence() throws Exception {
        assertReason(p -> ((ObjectNode) p.get("capability_proposals").get(0).get("scenarios").get(0))
                .put("confidence", 1.5), "SCHEMA_INVALID");
    }

    @Test
    void rejectsDecisionInGenerationProposal() throws Exception {
        assertReason(p -> {
            ObjectNode decision = NODE.objectNode();
            decision.put("action", "ACCEPT");
            decision.put("reviewer_identity", "agent");
            ((ObjectNode) p.get("capability_proposals").get(0).get("scenarios").get(0))
                    .set("decision", decision);
        }, "SCHEMA_INVALID");
    }

    @Test
    void obviousTechnicalIdentifiersAreRejectedFromBehavior() throws Exception {
        List<String> technical = List.of("OwnerController", "com.example.OwnerRepository",
                "foo()", "templates/owners/search.html");
        for (String text : technical) {
            assertReason(p -> ((ObjectNode) p.get("capability_proposals").get(0)
                    .get("scenarios").get(0)).put("when", text),
                    "TECHNICAL_IDENTIFIER_IN_BEHAVIOR");
        }
    }

    @Test
    void unavailableChannelIsExplicitAndCannotBeCited() throws Exception {
        Fixture fx = fixture();
        ObjectNode proposal = fx.proposal.deepCopy();
        ObjectNode channel = NODE.objectNode();
        channel.put("status", "UNAVAILABLE");
        channel.put("reason", "No pull-request provider was available");
        ((ObjectNode) proposal.get("channel_availability")).set("DELIVERY_HISTORY", channel);
        ArrayNode catalog = (ArrayNode) proposal.get("evidence_catalog");
        JsonNode structural = catalog.get(0);
        catalog.removeAll();
        catalog.add(structural);
        ObjectNode capability = (ObjectNode) proposal.get("capability_proposals").get(0);
        capability.set("evidence_refs", NODE.arrayNode().add("EV-G-001"));
        ((ObjectNode) capability.get("scenarios").get(0))
                .set("evidence_refs", NODE.arrayNode().add("EV-G-001"));
        assertEquals("UNAVAILABLE", ScenarioReview.validateProposal(fx.root, proposal)
                .get("channel_availability").get("DELIVERY_HISTORY").get("status").asText());

        // Re-add the history evidence entry; the channel is still UNAVAILABLE.
        ObjectNode historyEvidence = evidenceNode("EV-H-001", "DELIVERY_HISTORY",
                "validation/pkb001/datasets/history.json", fx.historyDigest, "/commits/0");
        catalog.add(historyEvidence);
        ScenarioReviewException caught = assertThrows(ScenarioReviewException.class,
                () -> ScenarioReview.validateProposal(fx.root, proposal, fx.proposalPath));
        assertTrue(caught.getReasons().contains("UNAVAILABLE_CHANNEL_CITED"), caught.getMessage());
    }

    @Test
    void rendererIsDeterministicBilingualAndLeavesDecisionsEmpty() throws Exception {
        Fixture fx = fixture();
        ScenarioReview.ReviewRender first = ScenarioReview.renderReview(
                fx.root, fx.proposal, fx.proposalPath);
        ScenarioReview.ReviewRender second = ScenarioReview.renderReview(
                fx.root, fx.proposal.deepCopy(), fx.proposalPath);
        assertEquals(first.review(), second.review());
        assertEquals(first.markdown(), second.markdown());
        assertEquals("SCENARIO_REVIEW_SURFACE", first.review().get("artifact_kind").asText());
        assertEquals(sha256(Files.readAllBytes(fx.proposalPath)),
                first.review().get("proposal_sha256").asText());
        assertTrue(first.review().get("capability_proposals").get(0).get("scenarios").get(0)
                .get("decision").isNull());
        String markdown = first.markdown();
        assertTrue(markdown.contains("## 能力提案 / Capability Proposal"));
        assertTrue(markdown.contains("### 情境 / Scenario"));
        assertTrue(markdown.contains("信心僅為未校準排序提示"));
        assertTrue(markdown.contains("節點 OwnerController"));
        assertTrue(markdown.contains("Add owner lookup behavior"));
        assertTrue(markdown.contains("ACCEPT / EDIT / REJECT"));
        assertTrue(markdown.contains("RECONSTRUCTION_CONSISTENCY_NOT_INDEPENDENT_PRODUCT_VALIDATION"));
        assertTrue(markdown.endsWith("\n"));
    }

    @Test
    void decisionsBindExactArtifactAndFilterRejectAndUnconfirmedEdit() throws Exception {
        Fixture fx = fixture();
        ScenarioReview.ReviewRender rendered = ScenarioReview.renderReview(
                fx.root, fx.proposal, fx.proposalPath);
        ObjectNode review = rendered.review().deepCopy();
        ObjectNode scenario = (ObjectNode) review.get("capability_proposals").get(0)
                .get("scenarios").get(0);
        ObjectNode base = decisionBase(review);

        ((ObjectNode) review.get("capability_proposals").get(0))
                .set("decision", withAction(base, "ACCEPT"));

        scenario.set("decision", withAction(base, "REJECT"));
        ScenarioReview.validateReview(fx.root, review);
        assertEquals(0, ScenarioReview.acceptedScenarios(fx.root, review).size());

        ObjectNode edit = withAction(base, "EDIT");
        edit.put("edit_confirmed", false);
        edit.set("replacement_behavior", replacementNode());
        scenario.set("decision", edit);
        ScenarioReview.validateReview(fx.root, review);
        assertEquals(0, ScenarioReview.acceptedScenarios(fx.root, review).size());

        edit.put("edit_confirmed", true);
        ArrayNode accepted = ScenarioReview.acceptedScenarios(fx.root, review);
        assertEquals("修改後情境", accepted.get(0).get("title").asText());
        assertEquals("HYP-CAPABILITY-001", accepted.get(0).get("capability_id").asText());
        assertEquals("HYP-SCENARIO-001", accepted.get(0).get("scenario_id").asText());
        assertEquals("REQUIRED_ACCEPTANCE", accepted.get(0).get("scope").asText());

        edit.put("proposal_sha256", "0".repeat(64));
        ScenarioReviewException caught = assertThrows(ScenarioReviewException.class,
                () -> ScenarioReview.validateReview(fx.root, review));
        assertTrue(caught.getReasons().contains("DECISION_BINDING_MISMATCH"), caught.getMessage());
    }

    static ObjectNode decisionBase(ObjectNode review) {
        ObjectNode base = NODE.objectNode();
        base.put("reviewer_identity", "human-reviewer");
        base.put("reviewed_at", "2026-09-05T01:02:03Z");
        base.put("reason", "逐項審查完成");
        base.put("proposal_revision", review.get("proposal_revision").asInt());
        base.put("proposal_sha256", review.get("proposal_sha256").asText());
        return base;
    }

    static ObjectNode withAction(ObjectNode base, String action) {
        ObjectNode decision = base.deepCopy();
        decision.put("action", action);
        return decision;
    }

    static ObjectNode replacementNode() {
        ObjectNode replacement = NODE.objectNode();
        replacement.put("title", "修改後情境");
        replacement.putArray("given").add("使用者已進入搜尋流程。");
        replacement.put("when", "使用者送出姓名。");
        replacement.putArray("then").add("系統顯示符合項目。");
        replacement.put("scope", "REQUIRED_ACCEPTANCE");
        return replacement;
    }

    @Test
    void rejectedOrUnreviewedCapabilityExcludesAcceptedChild() throws Exception {
        Fixture fx = fixture();
        ScenarioReview.ReviewRender rendered = ScenarioReview.renderReview(
                fx.root, fx.proposal, fx.proposalPath);
        ObjectNode review = rendered.review().deepCopy();
        ObjectNode capability = (ObjectNode) review.get("capability_proposals").get(0);
        ObjectNode scenario = (ObjectNode) capability.get("scenarios").get(0);
        ObjectNode base = decisionBase(review);
        scenario.set("decision", withAction(base, "ACCEPT"));
        assertEquals(0, ScenarioReview.acceptedScenarios(fx.root, review).size());
        capability.set("decision", withAction(base, "REJECT"));
        assertEquals(0, ScenarioReview.acceptedScenarios(fx.root, review).size());
        capability.set("decision", withAction(base, "ACCEPT"));
        ArrayNode accepted = ScenarioReview.acceptedScenarios(fx.root, review);
        assertEquals(1, accepted.size());
        assertEquals("HYP-SCENARIO-001", accepted.get(0).get("scenario_id").asText());
    }

    @Test
    void reviewRejectsTamperedBaseSemanticsEvenWhenDigestClaimIsUnchanged() throws Exception {
        Fixture fx = fixture();
        ObjectNode review = ScenarioReview.renderReview(fx.root, fx.proposal, fx.proposalPath)
                .review().deepCopy();
        ((ObjectNode) review.get("capability_proposals").get(0)).put("title", "竄改後能力");
        ScenarioReviewException caught = assertThrows(ScenarioReviewException.class,
                () -> ScenarioReview.validateReview(fx.root, review));
        assertTrue(caught.getReasons().contains("ORIGINAL_PROPOSAL_MISMATCH"), caught.getMessage());
    }

    @Test
    void acceptedScenariosValidatesBeforeFiltering() throws Exception {
        Fixture fx = fixture();
        ObjectNode review = ScenarioReview.renderReview(fx.root, fx.proposal, fx.proposalPath)
                .review().deepCopy();
        ObjectNode decision = NODE.objectNode();
        decision.put("action", "ACCEPT");
        ((ObjectNode) review.get("capability_proposals").get(0).get("scenarios").get(0))
                .set("decision", decision);
        assertThrows(ScenarioReviewException.class,
                () -> ScenarioReview.acceptedScenarios(fx.root, review));
    }

    @Test
    void futureHistoryItemsAreRejected() throws Exception {
        Fixture fx = fixture();
        ObjectNode proposal = fx.proposal.deepCopy();
        JsonNode historyInput = proposal.get("generation_inputs").get(2);
        Path historyPath = fx.root.resolve(historyInput.get("path").asText());
        ObjectNode history = (ObjectNode) JSON.readTree(Files.readAllBytes(historyPath));
        ((ObjectNode) history.get("commits").get(0)).put("committed_at", "2026-08-26T10:57:55Z");
        String digest = write(fx.root, historyInput.get("path").asText(), history);
        ((ObjectNode) historyInput).put("sha256", digest);
        ((ObjectNode) proposal.get("evidence_catalog").get(1)).put("artifact_sha256", digest);
        ScenarioReviewException caught = assertThrows(ScenarioReviewException.class,
                () -> ScenarioReview.validateProposal(fx.root, proposal));
        assertTrue(caught.getReasons().contains("POST_CUTOFF_HISTORY_ITEM"), caught.getMessage());
    }

    @Test
    void intermediateInputSymlinkIsRejected() throws Exception {
        Fixture fx = fixture();
        ObjectNode proposal = fx.proposal.deepCopy();
        Path real = fx.root.resolve("validation/pkb001/artifacts");
        Path alias = fx.root.resolve("validation/pkb001/graph-alias");
        Files.createSymbolicLink(alias, real);
        ((ObjectNode) proposal.get("generation_inputs").get(1))
                .put("path", "validation/pkb001/graph-alias/graph.json");
        ((ObjectNode) proposal.get("evidence_catalog").get(0))
                .put("artifact_path", "validation/pkb001/graph-alias/graph.json");
        ScenarioReviewException caught = assertThrows(ScenarioReviewException.class,
                () -> ScenarioReview.validateProposal(fx.root, proposal));
        assertTrue(caught.getReasons().contains("INPUT_PATH_SYMLINK"), caught.getMessage());
    }

    @Test
    void forbiddenInputIsNeverOpened() throws Exception {
        Fixture fx = fixture();
        ObjectNode proposal = fx.proposal.deepCopy();
        Path forbiddenPath = fx.root.resolve("validation/pkb001/evaluator/gold.json");
        Files.createDirectories(forbiddenPath.getParent());
        byte[] forbiddenBytes = "{\"nodes\": [], \"links\": []}\n".getBytes(StandardCharsets.UTF_8);
        Files.write(forbiddenPath, forbiddenBytes);
        String forbiddenDigest = sha256(forbiddenBytes);
        ((ObjectNode) proposal.get("generation_inputs").get(1))
                .put("path", "validation/pkb001/evaluator/gold.json");
        ((ObjectNode) proposal.get("generation_inputs").get(1))
                .put("sha256", forbiddenDigest);
        // Even though the digest matches, the forbidden path must fail closed first.
        ScenarioReviewException caught = assertThrows(ScenarioReviewException.class,
                () -> ScenarioReview.validateProposal(fx.root, proposal));
        assertTrue(caught.getReasons().contains("FORBIDDEN_GENERATION_INPUT"), caught.getMessage());
    }

    @Test
    void confirmedEditReplacementReceivesSemanticLint() throws Exception {
        Fixture fx = fixture();
        ObjectNode review = ScenarioReview.renderReview(fx.root, fx.proposal, fx.proposalPath)
                .review().deepCopy();
        ObjectNode base = decisionBase(review);
        ((ObjectNode) review.get("capability_proposals").get(0))
                .set("decision", withAction(base, "ACCEPT"));
        ObjectNode edit = withAction(base, "EDIT");
        edit.put("edit_confirmed", true);
        ObjectNode replacement = replacementNode();
        replacement.put("title", "Use SOURCE_PATH");
        edit.set("replacement_behavior", replacement);
        ((ObjectNode) review.get("capability_proposals").get(0).get("scenarios").get(0))
                .set("decision", edit);
        ScenarioReviewException caught = assertThrows(ScenarioReviewException.class,
                () -> ScenarioReview.validateReview(fx.root, review));
        assertTrue(caught.getReasons().contains("TECHNICAL_IDENTIFIER_IN_BEHAVIOR"),
                caught.getMessage());
    }

    @Test
    void malformedTopLevelAndChildrenFailClosed() throws Exception {
        Fixture fx = fixture();
        assertThrows(ScenarioReviewException.class, () -> ScenarioReview.validateProposal(
                fx.root, NODE.arrayNode()));
        assertThrows(ScenarioReviewException.class, () -> ScenarioReview.validateProposal(
                fx.root, NODE.nullNode()));
        assertThrows(ScenarioReviewException.class, () -> ScenarioReview.validateProposal(
                fx.root, NODE.arrayNode().add(NODE.nullNode())));
    }

    @Test
    void validationIsBounded() throws Exception {
        Fixture fx = fixture();
        ObjectNode proposal = fx.proposal.deepCopy();
        ArrayNode scenarios = (ArrayNode) proposal.get("capability_proposals").get(0)
                .get("scenarios");
        for (int index = 0; index < 100; index++) {
            scenarios.add(scenarioNode());
        }
        ScenarioReviewException caught = assertThrows(ScenarioReviewException.class,
                () -> ScenarioReview.validateProposal(fx.root, proposal, fx.proposalPath));
        assertTrue(caught.getReasons().contains("SCHEMA_INVALID"), caught.getMessage());
    }

    @Test
    void emptyReasonsBecomeDefaultValidationFailed() {
        ScenarioReviewException failure = new ScenarioReviewException(List.of());
        assertEquals(List.of("VALIDATION_FAILED"), failure.getReasons());
        assertEquals("VALIDATION_FAILED", failure.getMessage());
        ScenarioReviewException two = new ScenarioReviewException(List.of("B", "A", "B"));
        assertEquals(List.of("A", "B"), two.getReasons());
        assertEquals("A, B", two.getMessage());
    }

    // ------------------------------------------------------------------
    // Exclusive output creation
    // ------------------------------------------------------------------

    @Test
    void exclusiveCreationDuplicateRunAndNoPartialOrphan() throws Exception {
        Fixture fx = fixture();
        Path jsonOut = fx.root.resolve("validation/pkb001/reviews/review.json");
        Path mdOut = fx.root.resolve("validation/pkb001/reviews/review.md");
        ScenarioReview.writeReviewOutputs(fx.root, fx.proposalPath, jsonOut, mdOut);
        byte[] beforeJson = Files.readAllBytes(jsonOut);
        byte[] beforeMd = Files.readAllBytes(mdOut);

        Path otherJson = fx.root.resolve("validation/pkb001/reviews/other.json");
        Path otherMd = fx.root.resolve("validation/pkb001/reviews/other.md");
        ScenarioReviewException caught = assertThrows(ScenarioReviewException.class,
                () -> ScenarioReview.writeReviewOutputs(fx.root, fx.proposalPath, otherJson, otherMd));
        assertTrue(caught.getReasons().contains("RUN_ID_ALREADY_EXISTS"), caught.getMessage());
        assertFalse(Files.exists(otherJson));
        assertFalse(Files.exists(otherMd));
        assertEquals(new String(beforeJson, StandardCharsets.UTF_8),
                new String(Files.readAllBytes(jsonOut), StandardCharsets.UTF_8));
        assertEquals(new String(beforeMd, StandardCharsets.UTF_8),
                new String(Files.readAllBytes(mdOut), StandardCharsets.UTF_8));

        ObjectNode second = fx.proposal.deepCopy();
        second.put("run_id", "pkb001-scenario-review-test-002");
        Path secondProposalPath = fx.root.resolve("input/proposal-2.json");
        Files.write(secondProposalPath, (JSON.writerWithDefaultPrettyPrinter()
                .writeValueAsString(second) + "\n").getBytes(StandardCharsets.UTF_8));
        Path blockedMarkdown = fx.root.resolve("validation/pkb001/output/review.md");
        Files.createDirectories(blockedMarkdown.getParent());
        Files.writeString(blockedMarkdown, "keep me", StandardCharsets.UTF_8);
        ScenarioReviewException blocked = assertThrows(ScenarioReviewException.class,
                () -> ScenarioReview.writeReviewOutputs(fx.root, secondProposalPath,
                        fx.root.resolve("validation/pkb001/output/review.json"), blockedMarkdown));
        assertTrue(blocked.getReasons().contains("OUTPUT_ALREADY_EXISTS"), blocked.getMessage());
        assertFalse(Files.exists(fx.root.resolve("validation/pkb001/output/review.json")));
        assertEquals("keep me", Files.readString(blockedMarkdown, StandardCharsets.UTF_8));
        // The claim for run 002 must have been rolled back.
        assertTrue(Files.list(fx.root.resolve("validation/pkb001/scenario-review-runs"))
                .filter(path -> path.getFileName().toString().endsWith(".claim.json"))
                .count() == 1);
    }

    @Test
    void claimFileIsWrittenReadOnlyWithExactBody() throws Exception {
        Fixture fx = fixture();
        Path jsonOut = fx.root.resolve("validation/pkb001/reviews/review.json");
        Path mdOut = fx.root.resolve("validation/pkb001/reviews/review.md");
        ScenarioReview.writeReviewOutputs(fx.root, fx.proposalPath, jsonOut, mdOut);
        assertEquals("r--r--r--", java.nio.file.attribute.PosixFilePermissions.toString(
                Files.getPosixFilePermissions(jsonOut)));
        assertEquals("r--r--r--", java.nio.file.attribute.PosixFilePermissions.toString(
                Files.getPosixFilePermissions(mdOut)));
        String claimName = sha256("pkb001-scenario-review-test-001".getBytes(StandardCharsets.UTF_8))
                + ".claim.json";
        Path claim = fx.root.resolve("validation/pkb001/scenario-review-runs").resolve(claimName);
        assertTrue(Files.exists(claim));
        assertEquals("r--r--r--", java.nio.file.attribute.PosixFilePermissions.toString(
                Files.getPosixFilePermissions(claim)));
        String expected = "{\n  \"json_output\": \"validation/pkb001/reviews/review.json\",\n"
                + "  \"markdown_output\": \"validation/pkb001/reviews/review.md\",\n"
                + "  \"proposal_sha256\": \"" + sha256(Files.readAllBytes(fx.proposalPath)) + "\",\n"
                + "  \"run_id\": \"pkb001-scenario-review-test-001\"\n}\n";
        assertEquals(expected, Files.readString(claim, StandardCharsets.UTF_8));
        // No staged temp files remain.
        try (var stream = Files.list(fx.root.resolve("validation/pkb001/reviews"))) {
            assertTrue(stream.noneMatch(path -> path.getFileName().toString()
                    .startsWith(".scenario-review-")));
        }
    }

    @Test
    void outputWriteFailureRollsBackClaimAndCreatedFiles() throws Exception {
        Fixture fx = fixture();
        // Make the destination directory unwritable so staging the JSON temp
        // file fails with an I/O error after the run claim was reserved.
        Path reviews = fx.root.resolve("validation/pkb001/reviews");
        Files.createDirectories(reviews);
        Files.setPosixFilePermissions(reviews,
                java.nio.file.attribute.PosixFilePermissions.fromString("r-xr-xr-x"));
        try {
            ScenarioReviewException caught = assertThrows(ScenarioReviewException.class,
                    () -> ScenarioReview.writeReviewOutputs(fx.root, fx.proposalPath,
                            reviews.resolve("review.json"), reviews.resolve("review.md")));
            assertTrue(caught.getReasons().contains("OUTPUT_WRITE_FAILED"), caught.getMessage());
        } finally {
            Files.setPosixFilePermissions(reviews,
                    java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));
        }
        Path claimDir = fx.root.resolve("validation/pkb001/scenario-review-runs");
        try (var stream = Files.list(claimDir)) {
            assertEquals(0, stream.filter(path -> path.getFileName().toString()
                    .endsWith(".claim.json")).count());
        }
        assertFalse(Files.exists(reviews.resolve("review.json")));
        assertFalse(Files.exists(reviews.resolve("review.md")));
    }

    @Test
    void runClaimDirectoryCannotBeAnIntermediateSymlink() throws Exception {
        Fixture fx = fixture();
        Path realClaims = fx.root.resolve("validation/pkb001/real-claims");
        Files.createDirectories(realClaims);
        Files.createSymbolicLink(
                fx.root.resolve("validation/pkb001/scenario-review-runs"), realClaims);
        ScenarioReviewException caught = assertThrows(ScenarioReviewException.class,
                () -> ScenarioReview.writeReviewOutputs(fx.root, fx.proposalPath,
                        fx.root.resolve("validation/pkb001/reviews/review.json"),
                        fx.root.resolve("validation/pkb001/reviews/review.md")));
        assertTrue(caught.getReasons().contains("OUTPUT_PATH_SYMLINK"), caught.getMessage());
        try (var stream = Files.list(realClaims)) {
            assertEquals(0, stream.count());
        }
    }

    @Test
    void reviewOutputsRejectParentEscape() throws Exception {
        Fixture fx = fixture();
        ScenarioReviewException caught = assertThrows(ScenarioReviewException.class,
                () -> ScenarioReview.writeReviewOutputs(fx.root, fx.proposalPath,
                        Path.of("../escape.json"),
                        Path.of("validation/pkb001/reviews/review.md")));
        assertTrue(caught.getReasons().contains("OUTPUT_PATH_INVALID"), caught.getMessage());
    }

    @Test
    void reviewOutputsRejectNonReviewArea() throws Exception {
        Fixture fx = fixture();
        ScenarioReviewException caught = assertThrows(ScenarioReviewException.class,
                () -> ScenarioReview.writeReviewOutputs(fx.root, fx.proposalPath,
                        Path.of("outside/review.json"),
                        Path.of("validation/pkb001/reviews/review.md")));
        assertTrue(caught.getReasons().contains("OUTPUT_PATH_INVALID"), caught.getMessage());
    }

    @Test
    void reviewJsonBytesMatchPythonDumps() throws Exception {
        Fixture fx = fixture();
        ScenarioReview.ReviewRender rendered = ScenarioReview.renderReview(
                fx.root, fx.proposal, fx.proposalPath);
        byte[] javaBytes = com.featuredeliveryintelligence.fdi.validation.blindreview
                .BlindReview.jsonBytes(rendered.review());
        // Keys are sorted, indent is 2, non-ASCII is raw, trailing newline present.
        String text = new String(javaBytes, StandardCharsets.UTF_8);
        assertTrue(text.endsWith("\n"));
        assertTrue(text.contains("\"resolved_evidence\": {"));
        assertTrue(text.contains("節點 OwnerController（class OwnerController）"));
        // Round-trips as identical JSON.
        assertEquals(rendered.review(), JSON.readTree(javaBytes));
    }

    // ------------------------------------------------------------------
    // Python-exact primitives
    // ------------------------------------------------------------------

    @Test
    void format4fRoundsHalfEvenOnExactDecimalExpansion() {
        assertEquals("0.2812", ScenarioReview.format4f(0.28125));
        assertEquals("0.7188", ScenarioReview.format4f(0.71875));
        assertEquals("0.8438", ScenarioReview.format4f(0.84375));
        assertEquals("0.1562", ScenarioReview.format4f(0.15625));
        assertEquals("0.6700", ScenarioReview.format4f(0.67));
        assertEquals("0.9000", ScenarioReview.format4f(0.9));
        assertEquals("0.5000", ScenarioReview.format4f(0.5));
        assertEquals("2.5000", ScenarioReview.format4f(2.5));
        assertEquals("-0.2812", ScenarioReview.format4f(-0.28125));
        assertEquals("-0.0000", ScenarioReview.format4f(-0.0));
        assertEquals("0.0000", ScenarioReview.format4f(0.0));
        assertEquals("1.0000", ScenarioReview.format4f(0.99995));
        assertEquals("0.1235", ScenarioReview.format4f(0.12345));
        assertEquals("10000000000000000.0000", ScenarioReview.format4f(1e16));
        assertEquals("123456789.1234", ScenarioReview.format4f(123456789.12345));
        assertEquals("1.0001", ScenarioReview.format4f(1.00005));
        assertEquals("0.7600", ScenarioReview.format4f(0.76));
        assertEquals("0.7300", ScenarioReview.format4f(0.73));
    }

    @Test
    void pythonDeepEqualsIgnoresKeyOrderAndUnifiesIntAndFloat() throws Exception {
        JsonNode a = JSON.readTree("{\"x\": 1, \"y\": [1, 2.0]}");
        JsonNode b = JSON.readTree("{\"y\": [1.0, 2], \"x\": 1.0}");
        assertTrue(ScenarioReview.pythonDeepEquals(a, b));
        JsonNode c = JSON.readTree("{\"x\": 1, \"y\": [1, 2.1]}");
        assertFalse(ScenarioReview.pythonDeepEquals(a, c));
        JsonNode d = JSON.readTree("{\"x\": 1, \"y\": [2, 1]}");
        assertFalse(ScenarioReview.pythonDeepEquals(a, d));
        assertFalse(ScenarioReview.pythonDeepEquals(a, JSON.readTree("{\"x\": 1}")));
        assertFalse(ScenarioReview.pythonDeepEquals(a, JSON.readTree("null")));
        assertTrue(ScenarioReview.pythonDeepEquals(JSON.readTree("true"), JSON.readTree("1")));
        assertTrue(ScenarioReview.pythonDeepEquals(JSON.readTree("\"s\""), JSON.readTree("\"s\"")));
    }

    @Test
    void timestampsMirrorPythonFromIsoformat() {
        // Accept cases: value -> UTC epoch micros (verified against CPython 3.9).
        assertEquals(Instant.parse("2026-09-05T04:00:00Z"),
                ScenarioReview.parseTimestamp("2026-09-05T12:00:00+08:00"));
        assertEquals(Instant.parse("2026-09-05T12:00:00Z"),
                ScenarioReview.parseTimestamp("2026-09-05T12:00:00Z"));
        assertEquals(Instant.parse("2020-01-02T03:04:05Z"),
                ScenarioReview.parseTimestamp("2020-01-02T03:04:05Z"));
        assertEquals(Instant.parse("2026-09-05T12:00:00.123456Z"),
                ScenarioReview.parseTimestamp("2026-09-05T12:00:00.123456Z"));
        assertEquals(Instant.parse("2026-09-05T12:00:00.123Z"),
                ScenarioReview.parseTimestamp("2026-09-05T12:00:00.123Z"));
        assertEquals(Instant.parse("2026-09-05T12:00:00Z"),
                ScenarioReview.parseTimestamp("2026-09-05 12:00:00Z"));
        assertEquals(Instant.parse("2026-09-04T12:00:01Z"),
                ScenarioReview.parseTimestamp("2026-09-05T12:00:00+23:59:59"));
        assertEquals(Instant.parse("2026-09-04T12:00:00.000001Z"),
                ScenarioReview.parseTimestamp("2026-09-05T12:00:00+23:59:59.999999"));
        assertEquals(Instant.parse("2026-09-05T04:00:00Z"),
                ScenarioReview.parseTimestamp("2026-09-05T12:00:00.+08:00"));
        assertEquals(Instant.parse("2026-09-05T12:00:00Z"),
                ScenarioReview.parseTimestamp("2026-09-05X12:00:00Z"));
        // Reject cases.
        assertNull(ScenarioReview.parseTimestamp("2026-09-05T12:00:00"));
        assertNull(ScenarioReview.parseTimestamp("2026-09-05 12:00:00"));
        assertNull(ScenarioReview.parseTimestamp("2026-09-05T12:00:00+08"));
        assertNull(ScenarioReview.parseTimestamp("2026-09-05T12:00:00+0800"));
        assertNull(ScenarioReview.parseTimestamp("2026-09-05T12:00:00.5Z"));
        assertNull(ScenarioReview.parseTimestamp("2026-09-05T12:00:00.1234Z"));
        assertNull(ScenarioReview.parseTimestamp("2026-09-05T12:00:00.123456789Z"));
        assertNull(ScenarioReview.parseTimestamp("2026-13-05T12:00:00Z"));
        assertNull(ScenarioReview.parseTimestamp("2026-09-05T24:00:00Z"));
        assertNull(ScenarioReview.parseTimestamp("2026-09-05T12:00:60Z"));
        assertNull(ScenarioReview.parseTimestamp("2026-02-30T12:00:00Z"));
        assertNull(ScenarioReview.parseTimestamp("2021-02-29T12:00:00Z"));
        assertNull(ScenarioReview.parseTimestamp("2026-09-05T12:00:00+24:00"));
        assertNull(ScenarioReview.parseTimestamp("2026-09-05T12:00:00-23:59:60"));
        assertNull(ScenarioReview.parseTimestamp("2026-09-05T12:00:00z"));
        assertNull(ScenarioReview.parseTimestamp("0000-01-01T00:00:00Z"));
        assertNull(ScenarioReview.parseTimestamp("2026-09-0512:00:00Z"));
        assertNull(ScenarioReview.parseTimestamp("2026-09-05T12:00:00+08:00:00.123"));
        assertNull(ScenarioReview.parseTimestamp("2026-09-05T12:00:00Z junk"));
        assertNull(ScenarioReview.parseTimestamp(null));
    }

    @Test
    void technicalTextPatternMatchesPythonIgnoreCase() {
        // Reproduces the Python re.IGNORECASE battery (probe evidence under
        // .slice-work/scenario-review/probe_regex.py).
        String[] hits = {"OwnerController", "com.example.OwnerRepository", "foo()",
                "templates/owners/search.html", "呼叫 OwnerController.processFindForm()",
                "讀取 src/main/App.java", "source_path", "SOURCE PATH", "Graphify",
                "FooService", "aController", "templates/x", "app/config.yaml", "lib/util.js",
                "name(", "a.b.c", "e.g. foo", "foo.java5", "GrApHiFy", "main ("};
        String[] misses = {"normal chinese text", "PERFORMANCE", "src/", "srcx/main",
                "1.2.3", "v2.0", "3.5 mm", ".hidden", "()", "libs/x", "src", "lib/",
                "the Service", "STRASSE", "ß", "İ"};
        for (String text : hits) {
            assertTrue(ScenarioReview.containsTechnicalText(text), "expected hit: " + text);
        }
        for (String text : misses) {
            assertFalse(ScenarioReview.containsTechnicalText(text), "expected miss: " + text);
        }
    }

    @Test
    void parseTimestampRequiresString() {
        assertInstanceOf(Instant.class, ScenarioReview.parseTimestamp("2026-09-05T12:00:00Z"));
        assertNotNull(ScenarioReview.parseTimestamp("2026-09-05T12:00:00Z"));
    }
}

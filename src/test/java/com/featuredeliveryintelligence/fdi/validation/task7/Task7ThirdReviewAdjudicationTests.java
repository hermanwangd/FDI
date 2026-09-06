package com.featuredeliveryintelligence.fdi.validation.task7;

import com.featuredeliveryintelligence.fdi.application.Task7EvaluateCli;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Adjudication contract for the PKB-BL-004 third review: an optional
 * reviewer-03 workspace decides exactly the 11 frozen disagreements, fails
 * closed on missing / extra / duplicate / non-pending / malformed /
 * non-independent input, and never gains Human Reviewer authority. The
 * two-reviewer report remains byte-identical where reviewer-03 is absent.
 */
class Task7ThirdReviewAdjudicationTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String REVIEWER_03_DIR =
            Task7EvaluationRoots.TASK6 + "/judgment-workspaces/reviewer-03";
    private static final String REVIEWER_03_WORKSPACE = REVIEWER_03_DIR + "/judgment-template.json";
    private static final String REVIEWER_03_PACKET_INPUT = REVIEWER_03_DIR + "/packet-input.json";
    private static final List<String> PENDING_IDS = List.of(
            "BR-002", "BR-004", "BR-005", "BR-006", "BR-007", "BR-008",
            "BR-011", "BR-012", "BR-013", "BR-014", "BR-015");

    @TempDir Path temp;

    // ------------------------------------------------------------------
    // Positive contract
    // ------------------------------------------------------------------

    @Test
    void reviewer03DecidesExactlyThePendingDisagreements() throws Exception {
        Path root = adjudicatedRoot("valid");
        Task7Evaluation evaluation = new Task7Evaluation();
        JsonNode report = evaluation.evaluateRepository(root);

        assertEquals("REVISE", report.get("decision").asText());
        assertEquals("PENDING", report.get("human_product_team_review").get("status").asText());
        assertFalse(report.get("human_product_team_review")
                .get("semantic_publication_allowed").asBoolean());

        JsonNode adjudication = report.get("third_review_adjudication");
        assertEquals("ADJUDICATED", adjudication.get("status").asText());
        assertEquals("EVALUATOR_ONLY", adjudication.get("authority").asText());
        assertTrue(adjudication.get("independent_from_reviewer_01_and_reviewer_02").asBoolean());
        assertEquals(11, adjudication.get("item_count").asLong());
        assertEquals("reviewer-03", adjudication.get("reviewer_workspace_id").asText());
        assertEquals(REVIEWER_03_WORKSPACE, adjudication.get("reviewer_workspace_path").asText());

        ArrayNode resolutions = (ArrayNode) adjudication.get("resolutions");
        assertEquals(11, resolutions.size());
        List<String> resolutionIds = new ArrayList<>();
        for (JsonNode resolution : resolutions) {
            resolutionIds.add(resolution.get("blind_id").asText());
            // Final action/outcome follow the independent third judgment.
            JsonNode third = resolution.get("third_judgment");
            assertEquals(third.get("review_action").asText(),
                    resolution.get("final_review_action").asText());
            assertEquals(third.get("outcome").asText(), resolution.get("final_outcome").asText());
            assertFalse(resolution.get("reasons").isEmpty());
        }
        assertEquals(PENDING_IDS, resolutionIds);

        // The third judgment must be the synthesized reviewer-03 row, not a copy
        // of either original reviewer: BR-007 is decided SPLIT / UNSUPPORTED here.
        JsonNode br007 = resolutions.get(4);
        assertEquals("SPLIT", br007.get("final_review_action").asText());
        assertEquals("UNSUPPORTED", br007.get("final_outcome").asText());

        // Descriptive two-reviewer agreement stays auditable and unchanged.
        JsonNode baseReport = new Task7Evaluation().evaluateRepository(
                Task7EvaluationRoots.copyEvaluationRoot(temp.resolve("base")));
        assertArrayEquals(Task7Json.toReportBytes(baseReport.get("reviewer_agreement")),
                Task7Json.toReportBytes(report.get("reviewer_agreement")));
        assertArrayEquals(Task7Json.toReportBytes(baseReport.get("metrics")),
                Task7Json.toReportBytes(report.get("metrics")));
        assertArrayEquals(Task7Json.toReportBytes(baseReport.get("pending_third_review")),
                Task7Json.toReportBytes(report.get("pending_third_review")));
        assertArrayEquals(Task7Json.toReportBytes(baseReport.get("reverse_proposal_review_results")),
                Task7Json.toReportBytes(report.get("reverse_proposal_review_results")));
        assertFalse(baseReport.has("third_review_adjudication"));

        // The pending packet derivation still reproduces the committed artifact
        // bytes from an adjudicated report.
        byte[] committedPending = Files.readAllBytes(
                Task7EvaluationRoots.REPOSITORY.resolve(Task7EvaluationRoots.PENDING_RELATIVE));
        assertArrayEquals(committedPending,
                evaluation.toReportBytes(evaluation.buildThirdReviewPacket(report)));
    }

    @Test
    void thirdReviewResultPacketIsDeterministicAndEvaluatorOnly() throws Exception {
        Path root = adjudicatedRoot("result");
        Task7Evaluation evaluation = new Task7Evaluation();
        JsonNode report = evaluation.evaluateRepository(root);

        JsonNode result = evaluation.buildThirdReviewResult(report);
        assertEquals("pkb001.task7.third-review-result.v1", result.get("schema_version").asText());
        assertEquals("ADJUDICATED", result.get("status").asText());
        assertEquals("EVALUATOR_ONLY", result.get("authority").asText());
        assertFalse(result.get("semantic_publication_allowed").asBoolean());
        assertEquals("REVISE", result.get("decision").asText());
        assertEquals("BOUNDED_PROTOTYPE_DECISION_NO_SEMANTIC_PUBLICATION",
                result.get("decision_scope").asText());
        assertEquals(report.get("pre_unblinding_validation").get("packet_sha256").asText(),
                result.get("packet_sha256").asText());
        assertEquals("reviewer-03", result.get("reviewer_workspace_id").asText());
        assertEquals(11, result.get("item_count").asLong());
        assertEquals(report.get("third_review_adjudication").get("resolutions"),
                result.get("resolutions"));

        byte[] first = evaluation.toReportBytes(evaluation.buildThirdReviewResult(
                evaluation.evaluateRepository(root)));
        byte[] second = evaluation.toReportBytes(evaluation.buildThirdReviewResult(
                evaluation.evaluateRepository(root)));
        assertArrayEquals(first, second);
    }

    @Test
    void cliWritesResultPacketOnlyForAdjudicatedReport() throws Exception {
        Path root = adjudicatedRoot("cli");
        Path reportPath = temp.resolve("cli-report.json");
        Path resultPath = temp.resolve("cli-result.json");
        CliResult result = runCli("task7-evaluate",
                "--root", root.toString(),
                "--report", reportPath.toString(),
                "--result", resultPath.toString());
        assertEquals(0, result.exitCode(), result.stderr());
        assertEquals("", result.stderr());
        JsonNode packet = Task7Json.readTree(Files.readAllBytes(resultPath));
        assertEquals("pkb001.task7.third-review-result.v1", packet.get("schema_version").asText());
        assertEquals(11, packet.get("item_count").asLong());

        // Without a reviewer-03 workspace the --result contract fails closed.
        Path plainRoot = Task7EvaluationRoots.copyEvaluationRoot(temp.resolve("cli-plain"));
        Path mustNotExist = temp.resolve("cli-no-result.json");
        CliResult missing = runCli("task7-evaluate",
                "--root", plainRoot.toString(), "--result", mustNotExist.toString());
        assertEquals(2, missing.exitCode());
        assertFalse(Files.exists(mustNotExist));
    }

    // ------------------------------------------------------------------
    // Fail-closed negative cases
    // ------------------------------------------------------------------

    @Test
    void missingPendingJudgmentStopsClosed() throws Exception {
        Path root = adjudicatedRoot("missing");
        ObjectNode workspace = readReviewer03(root);
        ArrayNode rows = (ArrayNode) workspace.get("judgments");
        rows.remove(5); // BR-008
        writeReviewer03(root, workspace);
        assertThirdReviewStop(root, "must contain exactly 11 judgments");
    }

    @Test
    void extraNonPendingJudgmentStopsClosed() throws Exception {
        Path root = adjudicatedRoot("extra");
        ObjectNode workspace = readReviewer03(root);
        ((ArrayNode) workspace.get("judgments")).add(reviewer03Row("BR-001", "ACCEPT", "SUPPORTED"));
        writeReviewer03(root, workspace);
        assertThirdReviewStop(root, "must contain exactly 11 judgments");
    }

    @Test
    void duplicateJudgmentStopsClosed() throws Exception {
        Path root = adjudicatedRoot("duplicate");
        ObjectNode workspace = readReviewer03(root);
        ArrayNode rows = (ArrayNode) workspace.get("judgments");
        rows.set(3, reviewer03Row("BR-002", "ACCEPT", "SUPPORTED")); // replace BR-005 with a duplicate
        writeReviewer03(root, workspace);
        assertThirdReviewStop(root, "duplicate judgment for BR-002");
    }

    @Test
    void malformedJudgmentStopsClosed() throws Exception {
        Path root = adjudicatedRoot("malformed");
        ObjectNode workspace = readReviewer03(root);
        JsonNode row = workspace.get("judgments").get(2);
        ((ObjectNode) row).put("review_action", "MAYBE");
        writeReviewer03(root, workspace);
        assertThirdReviewStop(root, "incomplete or malformed judgment for BR-005");
    }

    @Test
    void emptyLimitationsStopsClosed() throws Exception {
        Path root = adjudicatedRoot("empty-limitations");
        ObjectNode workspace = readReviewer03(root);
        ((ObjectNode) workspace.get("judgments").get(0)).putArray("limitations");
        writeReviewer03(root, workspace);
        assertThirdReviewStop(root, "non-empty limitations for BR-002");
    }

    @Test
    void alteredPacketInputBindingStopsClosed() throws Exception {
        Path root = adjudicatedRoot("packet-input");
        // The pending packet is not the blind packet: rebinding to it must fail.
        Files.write(root.resolve(REVIEWER_03_PACKET_INPUT), Files.readAllBytes(
                Task7EvaluationRoots.REPOSITORY.resolve(Task7EvaluationRoots.PENDING_RELATIVE)));
        assertThirdReviewStop(root, REVIEWER_03_PACKET_INPUT + " digest does not match the sealed packet");
    }

    @Test
    void alteredWorkspacePacketDigestStopsClosed() throws Exception {
        Path root = adjudicatedRoot("packet-digest");
        ObjectNode workspace = readReviewer03(root);
        workspace.put("packet_sha256",
                "0".repeat(64));
        writeReviewer03(root, workspace);
        assertThirdReviewStop(root, "packet digest mismatch");
    }

    @Test
    void falseAuthorityAttestationStopsClosed() throws Exception {
        Path root = adjudicatedRoot("authority");
        ObjectNode workspace = readReviewer03(root);
        ((ObjectNode) workspace.get("reviewer_context"))
                .put("can_complete_product_team_review", true);
        writeReviewer03(root, workspace);
        assertThirdReviewStop(root, "claims invalid reviewer authority");
    }

    @Test
    void productTeamAuthorityStopsClosed() throws Exception {
        Path root = adjudicatedRoot("product-team");
        ObjectNode workspace = readReviewer03(root);
        ((ObjectNode) workspace.get("reviewer_context")).put("authority", "PRODUCT_TEAM");
        writeReviewer03(root, workspace);
        assertThirdReviewStop(root, "claims invalid reviewer authority");
    }

    @Test
    void missingIndependenceAttestationStopsClosed() throws Exception {
        Path root = adjudicatedRoot("independence");
        ObjectNode workspace = readReviewer03(root);
        ((ObjectNode) workspace.get("reviewer_context"))
                .put("independent_from_reviewer_01_and_reviewer_02", false);
        writeReviewer03(root, workspace);
        assertThirdReviewStop(root, "claims invalid reviewer authority");
    }

    @Test
    void sealedKeyAccessAttestationStopsClosed() throws Exception {
        Path root = adjudicatedRoot("sealed-key");
        ObjectNode workspace = readReviewer03(root);
        ((ObjectNode) workspace.get("reviewer_isolation")).put("sealed_key_accessible", true);
        writeReviewer03(root, workspace);
        assertThirdReviewStop(root, "does not preserve reviewer isolation");
    }

    @Test
    void colludingWorkspaceIdentityStopsClosed() throws Exception {
        Path root = adjudicatedRoot("collusion");
        ObjectNode workspace = readReviewer03(root);
        workspace.put("workspace_id", "reviewer-01");
        writeReviewer03(root, workspace);
        assertThirdReviewStop(root, "workspace_id must be reviewer-03");
    }

    @Test
    void unsupportedWorkspaceSchemaStopsClosed() throws Exception {
        Path root = adjudicatedRoot("schema");
        ObjectNode workspace = readReviewer03(root);
        workspace.put("schema_version", "pkb001.task6.blind-judgment-workspace.v2");
        writeReviewer03(root, workspace);
        assertThirdReviewStop(root, "unsupported schema_version");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Path adjudicatedRoot(String name) throws Exception {
        Path root = Task7EvaluationRoots.copyEvaluationRoot(temp.resolve(name));
        writeReviewer03(root, defaultReviewer03Workspace(root));
        return root;
    }

    private void assertThirdReviewStop(Path root, String detail) {
        Task7Evaluation evaluation = new Task7Evaluation();
        JsonNode report = evaluation.evaluateRepository(root);
        assertEquals("STOP", report.get("decision").asText());
        assertEquals("THIRD_REVIEW_ADJUDICATION", report.get("failure_stage").asText());
        assertEquals(2, report.get("documented_exit_code").asLong());
        assertFalse(report.get("semantic_publication_allowed").asBoolean());
        assertFalse(report.has("third_review_adjudication"));
        assertFalse(report.has("metrics"));
        assertTrue(report.get("stop_reasons").get(0).get("detail").asText().contains(detail),
                report.get("stop_reasons").get(0).get("detail").asText());
    }

    private static ObjectNode defaultReviewer03Workspace(Path root) throws Exception {
        ObjectNode workspace = JSON.createObjectNode();
        workspace.put("schema_version", "pkb001.task6.blind-judgment-workspace.v1");
        workspace.put("workspace_id", "reviewer-03");
        workspace.put("packet_sha256", Task7EvaluationRoots.sha256Hex(Files.readAllBytes(
                root.resolve(Task7EvaluationRoots.TASK6 + "/blind-review-packet.json"))));
        ObjectNode context = workspace.putObject("reviewer_context");
        context.put("actor_type", "NON_HUMAN");
        context.put("authority", "EVALUATOR_ONLY");
        context.put("can_complete_product_team_review", false);
        context.put("independent_from_reviewer_01_and_reviewer_02", true);
        ObjectNode isolation = workspace.putObject("reviewer_isolation");
        isolation.put("ground_truth_accessible_from_packet_workspace", false);
        isolation.put("other_workspace_future_judgments_accessible", false);
        isolation.put("sealed_key_accessible", false);
        ArrayNode judgments = workspace.putArray("judgments");
        for (int index = 0; index < PENDING_IDS.size(); index++) {
            String blindId = PENDING_IDS.get(index);
            if ("BR-007".equals(blindId)) {
                judgments.add(reviewer03Row(blindId, "SPLIT", "UNSUPPORTED"));
            } else {
                judgments.add(reviewer03Row(blindId, "ACCEPT", "SUPPORTED"));
            }
        }
        return workspace;
    }

    private static ObjectNode reviewer03Row(String blindId, String action, String outcome) {
        ObjectNode row = JSON.createObjectNode();
        row.put("active_review_seconds", 37);
        row.put("blind_id", blindId);
        row.put("evidence_validity", 0.8);
        ArrayNode limitations = row.putArray("limitations");
        limitations.add("Independent third judgment bounded to the supplied blind evidence.");
        row.put("outcome", outcome);
        row.put("precision", 0.8);
        row.put("review_action", action);
        row.put("reviewer_notes",
                "Decided from the blind packet content only; no reviewer-01/02 reasoning was accessed.");
        row.putNull("suggested_name");
        ArrayNode unsupported = row.putArray("unsupported_claims");
        unsupported.add("That structural evidence alone proves a complete realization.");
        row.put("usefulness", 0.8);
        return row;
    }

    private static void writeReviewer03(Path root, ObjectNode workspace) throws Exception {
        Path directory = root.resolve(REVIEWER_03_DIR);
        Files.createDirectories(directory);
        Files.write(root.resolve(REVIEWER_03_WORKSPACE), Task7Json.toReportBytes(workspace));
        Files.write(root.resolve(REVIEWER_03_PACKET_INPUT), Files.readAllBytes(
                root.resolve(Task7EvaluationRoots.TASK6 + "/blind-review-packet.json")));
    }

    private static ObjectNode readReviewer03(Path root) throws Exception {
        return (ObjectNode) Task7Json.readTree(Files.readAllBytes(
                root.resolve(REVIEWER_03_WORKSPACE)));
    }

    private record CliResult(int exitCode, String stdout, String stderr) { }

    private static CliResult runCli(String... args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = Task7EvaluateCli.run(args,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new CliResult(exitCode,
                stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
    }
}

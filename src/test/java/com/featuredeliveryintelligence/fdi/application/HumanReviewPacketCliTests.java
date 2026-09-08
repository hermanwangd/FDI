package com.featuredeliveryintelligence.fdi.application;

import com.featuredeliveryintelligence.fdi.validation.humanreviewpacket.HumanReviewPacket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ports the observable CLI contract of
 * {@code tooling/validation/build_pkb001_human_review_packet.py}: success writes
 * {@code human-review-decision-packet.json} and
 * {@code HUMAN-REVIEW-DECISION-PACKET.md} under
 * {@code validation/pkb001/human-review} relative to {@code --root} with no
 * stdout output and exit 0; a missing source file fails like a Python
 * traceback on stderr with exit 1. Byte parity of the written files against
 * the Python consumer is pinned by {@code HumanReviewPacketTests}; here the
 * files are compared against the direct {@link HumanReviewPacket} API.
 */
class HumanReviewPacketCliTests {
    @TempDir Path temp;

    @Test
    void handlesOnlyHumanReviewPacketBuildCommand() {
        assertFalse(HumanReviewPacketCli.handles(new String[0]));
        assertFalse(HumanReviewPacketCli.handles(new String[] {"unrelated"}));
        assertTrue(HumanReviewPacketCli.handles(new String[] {"human-review-packet-build"}));
    }

    @Test
    void cliWritesBothArtifactsAndExitsZero() throws Exception {
        Path root = writeFixtures();
        Result result = run(new String[] {"human-review-packet-build", "--root", root.toString()});

        assertEquals(0, result.exitCode());
        assertEquals("", result.stdout());

        Path outputDir = root.resolve("validation/pkb001/human-review");
        byte[] expectedJson = HumanReviewPacket.jsonBytes(HumanReviewPacket.buildPacket(root));
        byte[] expectedMarkdown = HumanReviewPacket.renderMarkdown(
                HumanReviewPacket.buildPacket(root)).getBytes(StandardCharsets.UTF_8);
        assertEquals(new String(expectedJson, StandardCharsets.UTF_8),
                new String(Files.readAllBytes(outputDir.resolve("human-review-decision-packet.json")),
                        StandardCharsets.UTF_8));
        assertEquals(new String(expectedMarkdown, StandardCharsets.UTF_8),
                new String(Files.readAllBytes(outputDir.resolve("HUMAN-REVIEW-DECISION-PACKET.md")),
                        StandardCharsets.UTF_8));
        // Files must be writable (Python default write_text permissions): no read-only bits.
        assertTrue(Files.isWritable(outputDir.resolve("human-review-decision-packet.json")));
        assertTrue(Files.isWritable(outputDir.resolve("HUMAN-REVIEW-DECISION-PACKET.md")));
    }

    @Test
    void cliAcceptsEqualsFormOption() throws Exception {
        Path root = writeFixtures();
        Result result = run(new String[] {"human-review-packet-build", "--root=" + root});
        assertEquals(0, result.exitCode());
        assertTrue(Files.exists(root.resolve("validation/pkb001/human-review/human-review-decision-packet.json")));
    }

    @Test
    void missingSourceFileFailsWithExitOneAndTracebackOnStderr() throws Exception {
        Path root = writeFixtures();
        Files.delete(root.resolve("validation/pkb001/task6-blind-review/sealed-blind-key.json"));
        Result result = run(new String[] {"human-review-packet-build", "--root", root.toString()});

        assertEquals(1, result.exitCode());
        assertEquals("", result.stdout());
        assertFalse(result.stderr().isEmpty());
    }

    @Test
    void unknownOptionFailsWithExitTwoLikeArgparse() {
        Result result = run(new String[] {"human-review-packet-build", "--bogus", "x"});
        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("INVALID_ARGUMENTS"));
    }

    @Test
    void defaultRootIsCurrentWorkingDirectory() {
        // The Python default is the repo root derived from __file__; the packaged
        // Java CLI has no file location, so it defaults to the current working
        // directory. Running from a directory without validation/pkb001 sources
        // must fail with exit 1 (missing source file), not silently succeed.
        Path emptyCwd = temp.resolve("empty-cwd");
        Result result = runWithUserDir(new String[] {"human-review-packet-build"}, emptyCwd);
        assertEquals(1, result.exitCode());
        assertFalse(result.stderr().isEmpty());
    }

    private Result run(String[] args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exit = HumanReviewPacketCli.run(args,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Result(exit, stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private Result runWithUserDir(String[] args, Path userDir) {
        String previous = System.getProperty("user.dir");
        System.setProperty("user.dir", userDir.toString());
        try {
            return run(args);
        } finally {
            System.setProperty("user.dir", previous);
        }
    }

    private Path writeFixtures() throws Exception {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("validation/pkb001/task6-blind-review/blind-review-packet.json",
                "{\"items\":[{\"blind_id\":\"BR-001\",\"candidate_capability\":\"Cap A\","
                        + "\"candidate_basis\":\"basis\",\"confidence_score\":0.9,"
                        + "\"component_refs\":[{\"reference\":\"n1\",\"source_location\":\"L1\"}]}],"
                        + "\"allowed_review_actions\":[\"ACCEPT\"]}");
        files.put("validation/pkb001/task6-blind-review/judgment-workspaces/reviewer-01/judgment-template.json",
                reviewerJudgments());
        files.put("validation/pkb001/task6-blind-review/judgment-workspaces/reviewer-02/judgment-template.json",
                reviewerJudgments());
        files.put("validation/pkb001/task7-evaluation/third-review-pending.json",
                "{\"items\":[]}");
        files.put("validation/pkb001/task7-evaluation/evaluation-report.json", evaluationReport());
        files.put("validation/pkb001/task6-blind-review/sealed-blind-key.json",
                "{\"items\":[{\"blind_id\":\"BR-001\",\"source_identifier\":\"PET-CAP-01\","
                        + "\"source_arm\":\"REVERSE\"}]}");
        files.put("validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json",
                "{\"capability_results\":[]}");
        files.put("validation/pkb001/evaluator/petclinic-818c413/gold-mappings.json",
                "{\"mappings\":[]}");

        Path root = temp.resolve("root");
        for (Map.Entry<String, String> file : files.entrySet()) {
            Path target = root.resolve(file.getKey());
            Files.createDirectories(target.getParent());
            Files.write(target, file.getValue().getBytes(StandardCharsets.UTF_8));
        }
        return root;
    }

    private String reviewerJudgments() {
        return "{\"judgments\":[{\"blind_id\":\"BR-001\",\"review_action\":\"ACCEPT\","
                + "\"outcome\":\"SUPPORTED\",\"suggested_name\":null,"
                + "\"reviewer_notes\":\"ok\",\"limitations\":[],\"unsupported_claims\":[]}]}";
    }

    private String evaluationReport() {
        return "{\"decision\":\"REVISE\",\"forward_expected_realization_comparison\":{"
                + "\"file_component_path_comparison\":{\"expected_component_path_recall\":1.0,"
                + "\"proposed_component_path_precision\":1.0},"
                + "\"expected_graph_node_coverage\":{\"expected_graph_nodes_cited\":1,"
                + "\"expected_graph_nodes\":1,\"expected_graph_node_coverage_rate\":1.0},"
                + "\"proposed_component_exact_graph_node_comparison\":{"
                + "\"proposed_component_exact_graph_node_matches\":0,\"expected_graph_nodes\":1,"
                + "\"proposed_component_exact_graph_node_recall\":0.0},"
                + "\"comparison_limits\":[\"L1\"],\"by_capability\":[]}}";
    }

    private record Result(int exitCode, String stdout, String stderr) { }
}

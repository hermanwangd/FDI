package com.featuredeliveryintelligence.fdi.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.featuredeliveryintelligence.fdi.validation.deliveryhistory.HistoryTestRepos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ports the CLI characterization cases of the transitional Python consumer
 * {@code tooling/validation/pkb001_history.py} to the packaged
 * {@code delivery-history-generate} command: required
 * {@code --repo/--source-sha/--cutoff/--prs/--output} arguments, deterministic
 * output artifact creation (including parent directories), the compact
 * {@code {"status": "ERROR", "error": ...}} JSON on stdout with exit 1 for
 * caught failures (OSError, ValueError, JSONDecodeError, CalledProcessError),
 * and exit 2 with usage on argument errors.
 */
class DeliveryHistoryCliTests {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir Path temp;

    @Test
    void handlesOnlyDeliveryHistoryGenerateCommand() {
        assertFalse(DeliveryHistoryCli.handles(new String[0]));
        assertFalse(DeliveryHistoryCli.handles(new String[] {"unrelated"}));
        assertTrue(DeliveryHistoryCli.handles(new String[] {"delivery-history-generate"}));
    }

    @Test
    void writesDeterministicFrozenHistoryAndCreatesParentDirectories() throws Exception {
        Path repo = HistoryTestRepos.init(temp.resolve("repo"));
        String c1 = HistoryTestRepos.commitAt(repo, "a.txt", "2026-01-01T10:00:00Z", "add a");
        String c2 = HistoryTestRepos.commitAt(repo, "b.txt", "2026-01-02T10:00:00Z", "add b");
        String c3 = HistoryTestRepos.commitAt(repo, "c.txt", "2026-01-04T10:00:00Z", "add c");
        Path prs = temp.resolve("prs.json");
        Files.writeString(prs, "[]\n", StandardCharsets.UTF_8);
        Path output = temp.resolve("nested/dir/result.json");

        Result result = run(new String[] {"delivery-history-generate",
                "--repo", repo.toString(),
                "--source-sha", c3,
                "--cutoff", "2026-01-03T00:00:00Z",
                "--prs", prs.toString(),
                "--output", output.toString()});

        assertEquals(0, result.exitCode());
        assertEquals("", result.stdout());
        assertEquals("", result.stderr());
        String written = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(written.endsWith("\n"), written);
        JsonNode tree = JSON.readTree(written);
        assertEquals("pkb001-delivery-history-v1", tree.get("dataset_id").asText());
        assertEquals("FROZEN", tree.get("status").asText());
        assertEquals(c3, tree.get("source_commit_sha").asText());
        assertEquals("EXCLUDE_AFTER_CUTOFF", tree.get("post_cutoff_knowledge_policy").asText());
        JsonNode commits = tree.get("commits");
        assertEquals(2, commits.size());
        assertEquals(c1, commits.get(0).get("commit_sha").asText());
        assertEquals("add a", commits.get(0).get("subject").asText());
        assertEquals(List.of("a.txt"), toList(commits.get(0).get("changed_paths")));
        assertEquals(c2, commits.get(1).get("commit_sha").asText());
        assertEquals(List.of("b.txt"), toList(commits.get(1).get("changed_paths")));
    }

    @Test
    void wrapsASinglePullRequestObjectIntoAList() throws Exception {
        Path repo = HistoryTestRepos.init(temp.resolve("repo"));
        String sha = HistoryTestRepos.commitAt(repo, "a.txt", "2026-01-01T10:00:00Z", "a");
        Path prs = temp.resolve("prs.json");
        Files.writeString(prs, "{\"number\": 9, \"title\": \"solo\", \"state\": \"OPEN\", \"url\": \"u\","
                + " \"createdAt\": \"2026-01-01T01:00:00Z\", \"updatedAt\": \"2026-01-01T02:00:00Z\","
                + " \"commits\": [{\"oid\": \"" + sha + "\"}]}\n", StandardCharsets.UTF_8);
        Path output = temp.resolve("out.json");

        Result result = run(new String[] {"delivery-history-generate",
                "--repo", repo.toString(),
                "--source-sha", sha,
                "--cutoff", "2026-01-02T00:00:00Z",
                "--prs", prs.toString(),
                "--output", output.toString()});

        assertEquals(0, result.exitCode());
        JsonNode tree = JSON.readTree(Files.readString(output, StandardCharsets.UTF_8));
        assertEquals(1, tree.get("pull_requests").size());
        assertEquals(9, tree.get("pull_requests").get(0).get("number").asInt());
        assertEquals(List.of(sha), toList(tree.get("pull_requests").get(0).get("included_commit_shas")));
    }

    @Test
    void treatsANullPullRequestDocumentAsNoPullRequests() throws Exception {
        Path repo = HistoryTestRepos.init(temp.resolve("repo"));
        String sha = HistoryTestRepos.commitAt(repo, "a.txt", "2026-01-01T10:00:00Z", "a");
        Path prs = temp.resolve("prs.json");
        Files.writeString(prs, "null\n", StandardCharsets.UTF_8);
        Path output = temp.resolve("out.json");

        Result result = run(new String[] {"delivery-history-generate",
                "--repo", repo.toString(),
                "--source-sha", sha,
                "--cutoff", "2026-01-02T00:00:00Z",
                "--prs", prs.toString(),
                "--output", output.toString()});

        assertEquals(0, result.exitCode());
        JsonNode tree = JSON.readTree(Files.readString(output, StandardCharsets.UTF_8));
        assertEquals(0, tree.get("pull_requests").size());
    }

    @Test
    void printsErrorJsonAndExitsOneForInvalidSourceSha() throws Exception {
        Path repo = HistoryTestRepos.init(temp.resolve("repo"));
        HistoryTestRepos.commitAt(repo, "a.txt", "2026-01-01T10:00:00Z", "a");
        Path prs = temp.resolve("prs.json");
        Files.writeString(prs, "[]\n", StandardCharsets.UTF_8);

        Result result = run(new String[] {"delivery-history-generate",
                "--repo", repo.toString(),
                "--source-sha", "abc123",
                "--cutoff", "2026-01-03T00:00:00Z",
                "--prs", prs.toString(),
                "--output", temp.resolve("out.json").toString()});

        assertEquals(1, result.exitCode());
        assertEquals("", result.stderr());
        assertEquals("{\"status\": \"ERROR\", \"error\": \"source SHA must be a full 40-character"
                + " lowercase Git SHA\"}" + System.lineSeparator(), result.stdout());
        assertFalse(Files.exists(temp.resolve("out.json")));
    }

    @Test
    void printsErrorJsonAndExitsOneForUnknownSourceSha() throws Exception {
        Path repo = HistoryTestRepos.init(temp.resolve("repo"));
        HistoryTestRepos.commitAt(repo, "a.txt", "2026-01-01T10:00:00Z", "a");
        Path prs = temp.resolve("prs.json");
        Files.writeString(prs, "[]\n", StandardCharsets.UTF_8);
        String unknown = "f".repeat(40);

        Result result = run(new String[] {"delivery-history-generate",
                "--repo", repo.toString(),
                "--source-sha", unknown,
                "--cutoff", "2026-01-03T00:00:00Z",
                "--prs", prs.toString(),
                "--output", temp.resolve("out.json").toString()});

        assertEquals(1, result.exitCode());
        assertEquals("{\"status\": \"ERROR\", \"error\": \"Command '['git', 'rev-parse', '--verify', '"
                + unknown + "^{commit}']' returned non-zero exit status 128.\"}"
                + System.lineSeparator(), result.stdout());
    }

    @Test
    void printsErrorJsonAndExitsOneForNaiveCutoff() throws Exception {
        Path repo = HistoryTestRepos.init(temp.resolve("repo"));
        String sha = HistoryTestRepos.commitAt(repo, "a.txt", "2026-01-01T10:00:00Z", "a");
        Path prs = temp.resolve("prs.json");
        Files.writeString(prs, "[]\n", StandardCharsets.UTF_8);

        Result result = run(new String[] {"delivery-history-generate",
                "--repo", repo.toString(),
                "--source-sha", sha,
                "--cutoff", "2026-01-03T00:00:00",
                "--prs", prs.toString(),
                "--output", temp.resolve("out.json").toString()});

        assertEquals(1, result.exitCode());
        assertEquals("{\"status\": \"ERROR\", \"error\": \"timestamp must include a timezone\"}"
                + System.lineSeparator(), result.stdout());
    }

    @Test
    void printsPythonStyleErrorJsonForMissingRepo() throws Exception {
        Path prs = temp.resolve("prs.json");
        Files.writeString(prs, "[]\n", StandardCharsets.UTF_8);
        Path missing = temp.resolve("no-such-repo");

        Result result = run(new String[] {"delivery-history-generate",
                "--repo", missing.toString(),
                "--source-sha", "a".repeat(40),
                "--cutoff", "2026-01-03T00:00:00Z",
                "--prs", prs.toString(),
                "--output", temp.resolve("out.json").toString()});

        assertEquals(1, result.exitCode());
        assertTrue(result.stdout().startsWith("{\"status\": \"ERROR\", \"error\": \"[Errno 2] No such file"
                + " or directory: '"), result.stdout());
        assertTrue(result.stdout().endsWith("no-such-repo'\"}" + System.lineSeparator()),
                result.stdout());
    }

    @Test
    void printsPythonStyleErrorJsonForRepoThatIsAFile() throws Exception {
        Path repo = HistoryTestRepos.init(temp.resolve("repo"));
        String sha = HistoryTestRepos.commitAt(repo, "a.txt", "2026-01-01T10:00:00Z", "a");
        Path notDir = temp.resolve("afile.txt");
        Files.writeString(notDir, "hi\n", StandardCharsets.UTF_8);
        Path prs = temp.resolve("prs.json");
        Files.writeString(prs, "[]\n", StandardCharsets.UTF_8);

        Result result = run(new String[] {"delivery-history-generate",
                "--repo", notDir.toString(),
                "--source-sha", sha,
                "--cutoff", "2026-01-03T00:00:00Z",
                "--prs", prs.toString(),
                "--output", temp.resolve("out.json").toString()});

        assertEquals(1, result.exitCode());
        assertEquals("{\"status\": \"ERROR\", \"error\": \"[Errno 20] Not a directory: PosixPath('"
                + notDir.toRealPath() + "')\"}" + System.lineSeparator(), result.stdout());
    }

    @Test
    void printsPythonStyleErrorJsonForMissingPullRequestFile() throws Exception {
        Path repo = HistoryTestRepos.init(temp.resolve("repo"));
        String sha = HistoryTestRepos.commitAt(repo, "a.txt", "2026-01-01T10:00:00Z", "a");

        Result result = run(new String[] {"delivery-history-generate",
                "--repo", repo.toString(),
                "--source-sha", sha,
                "--cutoff", "2026-01-03T00:00:00Z",
                "--prs", "missing-prs.json",
                "--output", temp.resolve("out.json").toString()});

        assertEquals(1, result.exitCode());
        assertEquals("{\"status\": \"ERROR\", \"error\": \"[Errno 2] No such file or directory:"
                + " 'missing-prs.json'\"}" + System.lineSeparator(), result.stdout());
    }

    @Test
    void printsPythonStyleErrorJsonForMalformedPullRequests() throws Exception {
        Path repo = HistoryTestRepos.init(temp.resolve("repo"));
        String sha = HistoryTestRepos.commitAt(repo, "a.txt", "2026-01-01T10:00:00Z", "a");
        Path prs = temp.resolve("prs.json");
        Files.writeString(prs, "not json\n", StandardCharsets.UTF_8);

        Result result = run(new String[] {"delivery-history-generate",
                "--repo", repo.toString(),
                "--source-sha", sha,
                "--cutoff", "2026-01-03T00:00:00Z",
                "--prs", prs.toString(),
                "--output", temp.resolve("out.json").toString()});

        assertEquals(1, result.exitCode());
        assertEquals("{\"status\": \"ERROR\", \"error\": \"Expecting value: line 1 column 1 (char 0)\"}"
                + System.lineSeparator(), result.stdout());
    }

    @Test
    void printsPythonStyleErrorJsonForNonUtf8PullRequests() throws Exception {
        Path repo = HistoryTestRepos.init(temp.resolve("repo"));
        String sha = HistoryTestRepos.commitAt(repo, "a.txt", "2026-01-01T10:00:00Z", "a");
        Path prs = temp.resolve("prs.json");
        Files.write(prs, new byte[] {(byte) 0xff, (byte) 0xfe});

        Result result = run(new String[] {"delivery-history-generate",
                "--repo", repo.toString(),
                "--source-sha", sha,
                "--cutoff", "2026-01-03T00:00:00Z",
                "--prs", prs.toString(),
                "--output", temp.resolve("out.json").toString()});

        assertEquals(1, result.exitCode());
        assertEquals("{\"status\": \"ERROR\", \"error\": \"'utf-8' codec can't decode byte 0xff"
                + " in position 0: invalid start byte\"}" + System.lineSeparator(), result.stdout());
    }

    @Test
    void exitsTwoWithUsageForMissingArguments() {
        Result result = run(new String[] {"delivery-history-generate",
                "--repo", temp.resolve("repo").toString()});

        assertEquals(2, result.exitCode());
        assertEquals("", result.stdout());
        assertTrue(result.stderr().contains("usage:"), result.stderr());
        assertTrue(result.stderr().contains("the following arguments are required: --source-sha, --cutoff,"
                + " --prs, --output"), result.stderr());
    }

    @Test
    void exitsTwoWithUsageForUnrecognizedArguments() {
        Result result = run(new String[] {"delivery-history-generate",
                "--repo", temp.resolve("repo").toString(),
                "--source-sha", "a".repeat(40),
                "--cutoff", "2026-01-03T00:00:00Z",
                "--prs", "prs.json",
                "--output", "out.json",
                "--bogus", "1"});

        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("unrecognized arguments"), result.stderr());
    }

    private static List<String> toList(JsonNode array) throws Exception {
        return JSON.readerForListOf(String.class).readValue(array.toString());
    }

    private static Result run(String[] args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = DeliveryHistoryCli.run(args,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Result(exitCode,
                stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
    }

    private record Result(int exitCode, String stdout, String stderr) { }
}

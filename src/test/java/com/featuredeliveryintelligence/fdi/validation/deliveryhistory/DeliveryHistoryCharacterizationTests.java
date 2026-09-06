package com.featuredeliveryintelligence.fdi.validation.deliveryhistory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ports the characterization cases of {@code tests/test_pkb_history.py} plus
 * the observed behavior of the transitional Python consumer
 * {@code tooling/validation/pkb001_history.py} to the Java {@link DeliveryHistory}
 * API: cutoff-bounded Git reconstruction over the exact source SHA, PR
 * metadata filtering against included commits and the cutoff, cutoff-inclusive
 * instant comparison, offset-timestamp comparison by instant, merge-commit
 * and empty-commit path behavior, deterministic rendering, and the Python
 * {@code ValueError} / {@code CalledProcessError} failure vocabulary.
 */
class DeliveryHistoryCharacterizationTests {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final DeliveryHistory history = new DeliveryHistory();

    @TempDir Path temp;

    @Test
    void reconstructsCutoffBoundedHistoryFromExactSourceSha() throws Exception {
        Path repo = HistoryTestRepos.init(temp.resolve("repo"));
        String c1 = HistoryTestRepos.commitAt(repo, "a.txt", "2026-01-01T10:00:00Z", "add a");
        String c2 = HistoryTestRepos.commitAt(repo, "b.txt", "2026-01-02T10:00:00Z", "add b");
        String c3 = HistoryTestRepos.commitAt(repo, "c.txt", "2026-01-04T10:00:00Z", "add c");

        JsonNode result = history.reconstruct(repo, c3, "2026-01-03T00:00:00Z", List.of());

        assertEquals(frozenHistory(c3, c1, c2, "[]"), render(result));
    }

    @Test
    void includesCommitCommittedExactlyAtTheCutoffInstant() throws Exception {
        Path repo = HistoryTestRepos.init(temp.resolve("repo"));
        String c1 = HistoryTestRepos.commitAt(repo, "a.txt", "2026-01-01T10:00:00Z", "add a");
        String c2 = HistoryTestRepos.commitAt(repo, "b.txt", "2026-01-02T10:00:00Z", "add b");
        String c3 = HistoryTestRepos.commitAt(repo, "c.txt", "2026-01-04T10:00:00Z", "add c");

        JsonNode result = history.reconstruct(repo, c3, "2026-01-02T10:00:00Z", List.of());

        assertEquals(List.of(c1, c2), shas(result));
    }

    @Test
    void filtersPullRequestsAgainstIncludedCommitsAndCutoff() throws Exception {
        Path repo = HistoryTestRepos.init(temp.resolve("repo"));
        String c1 = HistoryTestRepos.commitAt(repo, "a.txt", "2026-01-01T10:00:00Z", "add a");
        String c2 = HistoryTestRepos.commitAt(repo, "b.txt", "2026-01-02T10:00:00Z", "add b");
        String c3 = HistoryTestRepos.commitAt(repo, "c.txt", "2026-01-04T10:00:00Z", "add c");
        List<JsonNode> prs = List.of(
                pr("{\"number\": 2, \"title\": \"second\", \"state\": \"MERGED\","
                        + " \"url\": \"https://example.test/pr/2\","
                        + " \"createdAt\": \"2026-01-01T11:00:00Z\", \"updatedAt\": \"2026-01-02T09:00:00Z\","
                        + " \"headRefOid\": \"deadbeefdeadbeefdeadbeefdeadbeefdeadbeef\","
                        + " \"mergeCommit\": {\"oid\": \"" + c2 + "\"},"
                        + " \"commits\": [{\"oid\": \"" + c1 + "\"}, {\"oid\": \"" + c2 + "\"},"
                        + " {\"oid\": \"" + c2 + "\"}]}"),
                pr("{\"number\": 1, \"title\": \"late update\", \"state\": \"MERGED\","
                        + " \"url\": \"https://example.test/pr/1\","
                        + " \"createdAt\": \"2026-01-01T01:00:00Z\", \"updatedAt\": \"2026-01-04T00:00:00Z\","
                        + " \"commits\": [{\"oid\": \"" + c1 + "\"}]}"),
                pr("{\"number\": 3, \"title\": \"unknown commits\", \"state\": \"OPEN\","
                        + " \"url\": \"https://example.test/pr/3\","
                        + " \"createdAt\": \"2026-01-01T01:00:00Z\", \"updatedAt\": \"2026-01-02T01:00:00Z\","
                        + " \"commits\": [{\"oid\": \"eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee\"}]}"),
                JSON.readTree("\"not-a-dict\""),
                pr("{\"number\": 4, \"title\": \"missing updated\","
                        + " \"createdAt\": \"2026-01-01T01:00:00Z\"}"),
                pr("{\"number\": 5, \"title\": \"created after cutoff\", \"state\": \"OPEN\", \"url\": \"u\","
                        + " \"createdAt\": \"2026-01-03T01:00:00Z\", \"updatedAt\": \"2026-01-03T02:00:00Z\","
                        + " \"commits\": [{\"oid\": \"" + c1 + "\"}]}"),
                pr("{\"number\": 6, \"title\": \"post-cutoff commit link\", \"state\": \"MERGED\", \"url\": \"u6\","
                        + " \"createdAt\": \"2026-01-01T01:00:00Z\", \"updatedAt\": \"2026-01-02T01:00:00Z\","
                        + " \"commits\": [{\"oid\": \"" + c3 + "\"}]}"),
                pr("{\"number\": 0, \"title\": \"null fields\", \"state\": null, \"url\": null,"
                        + " \"createdAt\": \"2026-01-01T01:00:00Z\", \"updatedAt\": \"2026-01-02T01:00:00Z\","
                        + " \"commits\": [{\"oid\": \"" + c1 + "\"}]}"));

        JsonNode result = history.reconstruct(repo, c3, "2026-01-03T00:00:00Z", prs);

        String pullRequests = """
[
    {
      "number": 0,
      "title": "null fields",
      "state": null,
      "url": null,
      "created_at": "2026-01-01T01:00:00Z",
      "updated_at": "2026-01-02T01:00:00Z",
      "head_ref_oid": null,
      "merge_commit_sha": null,
      "included_commit_shas": [
        "@C1@"
      ]
    },
    {
      "number": 2,
      "title": "second",
      "state": "MERGED",
      "url": "https://example.test/pr/2",
      "created_at": "2026-01-01T11:00:00Z",
      "updated_at": "2026-01-02T09:00:00Z",
      "head_ref_oid": "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef",
      "merge_commit_sha": "@C2@",
      "included_commit_shas": [
        "@C2@",
        "@C1@"
      ]
    }
  ]""".replace("@C1@", c1).replace("@C2@", c2);
        assertEquals(frozenHistory(c3, c1, c2, pullRequests), render(result));
    }

    @Test
    void comparesOffsetTimestampsByInstantAndRendersThemVerbatim() throws Exception {
        Path repo = HistoryTestRepos.init(temp.resolve("repo"));
        String base = HistoryTestRepos.commitAt(repo, "base.txt", "2026-01-01T10:00:00+05:30", "offset date base");
        HistoryTestRepos.git(repo, "checkout", "-q", "-b", "feature");
        String feature = HistoryTestRepos.commitAt(repo, "feat.txt", "2026-01-02T10:00:00Z", "feature work");
        HistoryTestRepos.git(repo, "checkout", "-q", "-");
        HistoryTestRepos.commitAt(repo, "z.txt", "2026-01-03T10:00:00Z", "main z");
        HistoryTestRepos.gitAt(repo, "2026-01-04T10:00:00Z", "merge", "-q", "--no-ff", "feature", "-m", "merge feature");
        String merge = HistoryTestRepos.git(repo, "rev-parse", "HEAD");
        String tip = HistoryTestRepos.commitAt(repo, "tip.txt", "2026-01-05T10:00:00Z", "tip");

        // 2026-01-01T05:00:00Z is after 2026-01-01T10:00:00+05:30 (04:30 UTC).
        JsonNode result = history.reconstruct(repo, tip, "2026-01-01T05:00:00Z", List.of());

        assertEquals(List.of(base), shas(result));
        assertEquals("2026-01-01T10:00:00+05:30", result.get("commits").get(0).get("committed_at").asText());

        // The merge commit itself carries no changed paths, like the Python consumer.
        JsonNode full = history.reconstruct(repo, tip, "2026-01-10T00:00:00Z", List.of());
        JsonNode mergeRow = null;
        for (JsonNode commit : full.get("commits")) {
            if (commit.get("commit_sha").asText().equals(merge)) {
                mergeRow = commit;
            }
        }
        assertEquals(List.of(), toStringList(mergeRow.get("changed_paths")));
        assertEquals(5, full.get("commits").size());
        assertEquals(feature, full.get("commits").get(1).get("commit_sha").asText());
    }

    @Test
    void sortsChangedPathsAndKeepsEmptyDiffCommits() throws Exception {
        Path repo = HistoryTestRepos.init(temp.resolve("repo"));
        HistoryTestRepos.commitAt(repo, "z.txt", "2026-01-01T10:00:00Z", "z");
        HistoryTestRepos.gitAt(repo, "2026-01-02T10:00:00Z", "commit", "-qm", "empty", "--allow-empty");
        String empty = HistoryTestRepos.git(repo, "rev-parse", "HEAD");
        Files.writeString(repo.resolve("b.txt"), "b\n", StandardCharsets.UTF_8);
        Files.writeString(repo.resolve("a.txt"), "a\n", StandardCharsets.UTF_8);
        HistoryTestRepos.git(repo, "add", "a.txt", "b.txt");
        HistoryTestRepos.gitAt(repo, "2026-01-03T10:00:00Z", "commit", "-qm", "multi file");
        String tip = HistoryTestRepos.git(repo, "rev-parse", "HEAD");

        JsonNode result = history.reconstruct(repo, tip, "2026-01-10T00:00:00Z", List.of());

        assertEquals(empty, shas(result).get(1));
        assertEquals(List.of(), toStringList(result.get("commits").get(1).get("changed_paths")));
        assertEquals(List.of("a.txt", "b.txt"), toStringList(result.get("commits").get(2).get("changed_paths")));
        assertEquals(tip, shas(result).get(2));
    }

    @Test
    void rejectsShortOrUppercaseSourceSha() throws Exception {
        Path repo = HistoryTestRepos.init(temp.resolve("repo"));
        String sha = HistoryTestRepos.commitAt(repo, "a.txt", "2026-01-01T10:00:00Z", "a");

        IllegalArgumentException shortSha = assertThrows(IllegalArgumentException.class,
                () -> history.reconstruct(repo, "abc123", "2026-01-03T00:00:00Z", List.of()));
        assertEquals("source SHA must be a full 40-character lowercase Git SHA", shortSha.getMessage());
        IllegalArgumentException upperSha = assertThrows(IllegalArgumentException.class,
                () -> history.reconstruct(repo, sha.toUpperCase(), "2026-01-03T00:00:00Z", List.of()));
        assertEquals("source SHA must be a full 40-character lowercase Git SHA", upperSha.getMessage());
    }

    @Test
    void reportsUnknownSourceShaLikePythonCalledProcessError() throws Exception {
        Path repo = HistoryTestRepos.init(temp.resolve("repo"));
        HistoryTestRepos.commitAt(repo, "a.txt", "2026-01-01T10:00:00Z", "a");
        String unknown = "f".repeat(40);

        GitCommandException failure = assertThrows(GitCommandException.class,
                () -> history.reconstruct(repo, unknown, "2026-01-03T00:00:00Z", List.of()));
        assertEquals("Command '['git', 'rev-parse', '--verify', '" + unknown + "^{commit}']'"
                + " returned non-zero exit status 128.", failure.getMessage());
    }

    @Test
    void validatesTheCutoffTimestamp() throws Exception {
        Path repo = HistoryTestRepos.init(temp.resolve("repo"));
        String sha = HistoryTestRepos.commitAt(repo, "a.txt", "2026-01-01T10:00:00Z", "a");

        IllegalArgumentException naive = assertThrows(IllegalArgumentException.class,
                () -> history.reconstruct(repo, sha, "2026-01-03T00:00:00", List.of()));
        assertEquals("timestamp must include a timezone", naive.getMessage());
        IllegalArgumentException garbage = assertThrows(IllegalArgumentException.class,
                () -> history.reconstruct(repo, sha, "not-a-date", List.of()));
        assertEquals("Invalid isoformat string: 'not-a-date'", garbage.getMessage());
    }

    @Test
    void failsClosedWhenPullRequestTimestampIsUnparseable() throws Exception {
        Path repo = HistoryTestRepos.init(temp.resolve("repo"));
        String sha = HistoryTestRepos.commitAt(repo, "a.txt", "2026-01-01T10:00:00Z", "a");
        List<JsonNode> prs = List.of(pr("{\"number\": 1, \"title\": \"x\","
                + " \"createdAt\": \"garbage\", \"updatedAt\": \"2026-01-01T00:00:00Z\", \"commits\": []}"));

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> history.reconstruct(repo, sha, "2026-01-03T00:00:00Z", prs));
        assertEquals("Invalid isoformat string: 'garbage'", failure.getMessage());
    }

    @Test
    void resolvesTheRepoStrictly() {
        Path missing = temp.resolve("no-such-repo");

        NoSuchFileException failure = assertThrows(NoSuchFileException.class,
                () -> history.reconstruct(missing, "a".repeat(40), "2026-01-03T00:00:00Z", List.of()));
        assertTrue(failure.getFile().endsWith("no-such-repo"), failure.getFile());
    }

    private static JsonNode pr(String json) throws Exception {
        return JSON.readTree(json);
    }

    private static List<String> shas(JsonNode result) {
        List<String> shas = new ArrayList<>();
        result.get("commits").forEach(commit -> shas.add(commit.get("commit_sha").asText()));
        return shas;
    }

    private static List<String> toStringList(JsonNode array) throws Exception {
        return JSON.readerForListOf(String.class).readValue(array.toString());
    }

    private static String render(JsonNode result) {
        return new String(new DeliveryHistoryResult(result).toJsonBytes(), StandardCharsets.UTF_8);
    }

    private static String frozenHistory(String source, String c1, String c2, String pullRequests) {
        return """
                {
                  "dataset_id": "pkb001-delivery-history-v1",
                  "status": "FROZEN",
                  "source_commit_sha": "@SOURCE@",
                  "history_cutoff": "2026-01-03T00:00:00Z",
                  "post_cutoff_knowledge_policy": "EXCLUDE_AFTER_CUTOFF",
                  "commits": [
                    {
                      "commit_sha": "@C1@",
                      "committed_at": "2026-01-01T10:00:00Z",
                      "subject": "add a",
                      "changed_paths": [
                        "a.txt"
                      ]
                    },
                    {
                      "commit_sha": "@C2@",
                      "committed_at": "2026-01-02T10:00:00Z",
                      "subject": "add b",
                      "changed_paths": [
                        "b.txt"
                      ]
                    }
                  ],
                  "pull_requests": @PRS@,
                  "evidence_boundary": "Git and pull-request evidence only; not Product truth.",
                  "limitations": [
                    "Pull requests are included only when supplied metadata links them to an included commit.",
                    "Events created after the cutoff are excluded even when later repository state exposes them."
                  ]
                }
                """.replace("@SOURCE@", source).replace("@C1@", c1).replace("@C2@", c2).replace("@PRS@", pullRequests);
    }
}

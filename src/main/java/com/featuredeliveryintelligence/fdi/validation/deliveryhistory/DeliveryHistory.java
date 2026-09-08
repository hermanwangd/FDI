package com.featuredeliveryintelligence.fdi.validation.deliveryhistory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Deterministic PKB-001 delivery-history reconstruction. Ports the observable
 * behavior of the transitional Python consumer
 * {@code tooling/validation/pkb001_history.py}: strict repository resolution,
 * full 40-character lowercase source-SHA validation and exact-commit
 * resolution, cutoff-bounded {@code rev-list} reconstruction (commits at the
 * cutoff instant are included), sorted changed paths from root-aware
 * {@code diff-tree}, PR metadata filtering against included commits and the
 * cutoff with the Python sort order, and the frozen deterministic result
 * envelope. {@link IllegalArgumentException} mirrors the Python
 * {@code ValueError} vocabulary, {@link GitCommandException} mirrors the
 * caught {@code subprocess.CalledProcessError} text, {@link IOException}
 * mirrors {@code OSError}, and hostile non-iterable shapes raise
 * {@link IllegalStateException} like the Python consumer's uncaught
 * {@code TypeError}.
 */
public final class DeliveryHistory {
    private static final Pattern GIT_SHA = Pattern.compile("[0-9a-f]{40}");
    private static final JsonNodeFactory NODE = JsonNodeFactory.instance;

    /** Ports {@code reconstruct_history(repo, source_sha, cutoff, prs)}. */
    public JsonNode reconstruct(Path repo, String sourceSha, String cutoff, List<JsonNode> prs)
            throws IOException {
        Path resolved = repo.toRealPath();
        if (sourceSha == null || !GIT_SHA.matcher(sourceSha).matches()) {
            throw new IllegalArgumentException("source SHA must be a full 40-character lowercase Git SHA");
        }
        Instant cutoffInstant = PyTimestamp.parse(cutoff);
        String resolvedSha = git(resolved, "rev-parse", "--verify", sourceSha + "^{commit}");
        if (!resolvedSha.equals(sourceSha)) {
            throw new IllegalArgumentException("source SHA does not resolve to the requested commit");
        }

        ArrayNode commits = NODE.arrayNode();
        Set<String> includedShas = new HashSet<>();
        for (String commitSha : lines(git(resolved, "rev-list", "--reverse", sourceSha))) {
            String committedAt = git(resolved, "show", "-s", "--format=%cI", commitSha);
            if (PyTimestamp.parse(committedAt).compareTo(cutoffInstant) > 0) {
                continue;
            }
            Set<String> paths = new TreeSet<>();
            for (String path : lines(git(resolved, "diff-tree", "--root", "--no-commit-id", "--name-only",
                    "-r", commitSha))) {
                if (!path.isEmpty()) {
                    paths.add(path);
                }
            }
            ObjectNode commit = commits.addObject();
            commit.put("commit_sha", commitSha);
            commit.put("committed_at", committedAt);
            commit.put("subject", git(resolved, "show", "-s", "--format=%s", commitSha));
            ArrayNode changedPaths = commit.putArray("changed_paths");
            paths.forEach(changedPaths::add);
            includedShas.add(commitSha);
        }

        List<ObjectNode> pullRequests = new ArrayList<>();
        for (JsonNode pr : prs) {
            if (pr == null || !pr.isObject()) {
                continue;
            }
            JsonNode createdAt = pr.get("createdAt");
            JsonNode updatedAt = pr.get("updatedAt");
            if (createdAt == null || !createdAt.isTextual() || updatedAt == null || !updatedAt.isTextual()) {
                continue;
            }
            Set<String> matching = new TreeSet<>();
            for (JsonNode commit : iterateCommits(pr)) {
                if (commit.isObject()) {
                    JsonNode oid = commit.get("oid");
                    if (oid != null && oid.isTextual() && includedShas.contains(oid.asText())) {
                        matching.add(oid.asText());
                    }
                }
            }
            if (PyTimestamp.parse(createdAt.asText()).compareTo(cutoffInstant) > 0
                    || PyTimestamp.parse(updatedAt.asText()).compareTo(cutoffInstant) > 0
                    || matching.isEmpty()) {
                continue;
            }
            ObjectNode pullRequest = NODE.objectNode();
            pullRequest.set("number", orNull(pr.get("number")));
            pullRequest.set("title", orNull(pr.get("title")));
            pullRequest.set("state", orNull(pr.get("state")));
            pullRequest.set("url", orNull(pr.get("url")));
            pullRequest.put("created_at", createdAt.asText());
            pullRequest.put("updated_at", updatedAt.asText());
            pullRequest.set("head_ref_oid", orNull(pr.get("headRefOid")));
            JsonNode merge = pr.get("mergeCommit");
            pullRequest.set("merge_commit_sha",
                    merge != null && merge.isObject() ? orNull(merge.get("oid")) : NODE.nullNode());
            ArrayNode included = pullRequest.putArray("included_commit_shas");
            matching.forEach(included::add);
            pullRequests.add(pullRequest);
        }
        pullRequests.sort((left, right) -> pythonCompare(left.get("number"), right.get("number")));

        ObjectNode result = NODE.objectNode();
        result.put("dataset_id", "pkb001-delivery-history-v1");
        result.put("status", "FROZEN");
        result.put("source_commit_sha", sourceSha);
        result.put("history_cutoff", cutoff);
        result.put("post_cutoff_knowledge_policy", "EXCLUDE_AFTER_CUTOFF");
        result.set("commits", commits);
        ArrayNode pullRequestArray = result.putArray("pull_requests");
        pullRequests.forEach(pullRequestArray::add);
        result.put("evidence_boundary", "Git and pull-request evidence only; not Product truth.");
        ArrayNode limitations = result.putArray("limitations");
        limitations.add("Pull requests are included only when supplied metadata links them to an included commit.");
        limitations.add("Events created after the cutoff are excluded even when later repository state exposes them.");
        return result;
    }

    /** Ports the consumer's {@code _git} subprocess helper. */
    private static String git(Path repo, String... arguments) throws IOException {
        if (!Files.isDirectory(repo)) {
            // subprocess with a non-directory cwd raises NotADirectoryError
            // whose filename is the Path object, rendered as PosixPath('...').
            throw new NotDirectoryException(repo.toString());
        }
        List<String> command = new ArrayList<>();
        command.add("git");
        for (String argument : arguments) {
            command.add(argument);
        }
        Process process = new ProcessBuilder(command).directory(repo.toFile()).start();
        process.getOutputStream().close();
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        process.getErrorStream().readAllBytes();
        try {
            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("git timed out: " + String.join(" ", command));
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("git interrupted: " + String.join(" ", command), failure);
        }
        if (process.exitValue() != 0) {
            throw new GitCommandException("Command '" + pythonListRepr(command)
                    + "' returned non-zero exit status " + process.exitValue() + ".");
        }
        return stdout.strip();
    }

    /** CPython {@code list} {@code repr} of the argv list, like {@code CalledProcessError} prints it. */
    private static String pythonListRepr(List<String> command) {
        StringBuilder out = new StringBuilder("[");
        for (int index = 0; index < command.size(); index++) {
            out.append(index == 0 ? "" : ", ").append('\'').append(command.get(index)).append('\'');
        }
        return out.append(']').toString();
    }

    /** Python {@code str.splitlines()} on the stripped git stdout. */
    private static List<String> lines(String output) {
        if (output.isEmpty()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (String line : output.split("\n")) {
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
        return lines;
    }

    /** Python {@code for commit in pr.get('commits', [])}: arrays by element, strings by character. */
    private static List<JsonNode> iterateCommits(JsonNode pr) {
        JsonNode commits = pr.get("commits");
        if (commits == null) {
            return List.of();
        }
        if (commits.isNull() || (!commits.isArray() && !commits.isTextual())) {
            throw new IllegalStateException("value is not iterable");
        }
        List<JsonNode> items = new ArrayList<>();
        if (commits.isArray()) {
            commits.forEach(items::add);
        } else {
            String text = commits.asText();
            for (int index = 0; index < text.length(); index++) {
                items.add(NODE.textNode(String.valueOf(text.charAt(index))));
            }
        }
        return items;
    }

    /** Python {@code <} on parsed JSON values for the PR {@code number} sort key. */
    private static int pythonCompare(JsonNode left, JsonNode right) {
        boolean leftNone = left == null || left.isNull();
        boolean rightNone = right == null || right.isNull();
        if (leftNone || rightNone) {
            if (leftNone && rightNone) {
                return 0;
            }
            throw new IllegalStateException("'< not supported between instances of 'NoneType' and 'int'");
        }
        if (left.isNumber() && right.isNumber()) {
            return left.decimalValue().compareTo(right.decimalValue());
        }
        if (left.isTextual() && right.isTextual()) {
            return left.asText().compareTo(right.asText());
        }
        throw new IllegalStateException("'< not supported between instances of '"
                + pythonType(left) + "' and '" + pythonType(right) + "'");
    }

    private static String pythonType(JsonNode value) {
        if (value.isTextual()) {
            return "str";
        }
        if (value.isBoolean()) {
            return "bool";
        }
        if (value.isNumber()) {
            return "int";
        }
        if (value.isArray()) {
            return "list";
        }
        if (value.isObject()) {
            return "dict";
        }
        return "NoneType";
    }

    private static JsonNode orNull(JsonNode value) {
        return value == null ? NODE.nullNode() : value;
    }
}

package com.featuredeliveryintelligence.fdi.validation.deliveryhistory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Shared fixture builder for the delivery-history characterization and CLI
 * tests. Mirrors the {@code tmp_path} git repositories built inline by
 * {@code tests/test_pkb_history.py}: real temporary git repositories with
 * pinned author/committer identities and commit dates so every commit SHA is
 * deterministic.
 */
public final class HistoryTestRepos {
    private HistoryTestRepos() { }

    public static Path init(Path repo) throws IOException {
        Files.createDirectories(repo);
        git(repo, "init", "-q");
        git(repo, "config", "user.email", "test@example.com");
        git(repo, "config", "user.name", "Test");
        return repo;
    }

    /** Commits a single new file with pinned dates and returns the commit SHA. */
    public static String commitAt(Path repo, String file, String date, String message) throws IOException {
        Files.writeString(repo.resolve(file), file + "\n", StandardCharsets.UTF_8);
        git(repo, "add", file);
        gitAt(repo, date, "commit", "-qm", message);
        return git(repo, "rev-parse", "HEAD");
    }

    /** Runs git with pinned author/committer dates. */
    public static String gitAt(Path directory, String date, String... arguments) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("git");
        for (String argument : arguments) {
            command.add(argument);
        }
        ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile());
        if (date != null) {
            builder.environment().put("GIT_AUTHOR_DATE", date);
            builder.environment().put("GIT_COMMITTER_DATE", date);
        }
        Process process = builder.start();
        process.getOutputStream().close();
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        try {
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("git timed out: " + String.join(" ", command));
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("git interrupted: " + String.join(" ", command), failure);
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("git failed (" + process.exitValue() + "): "
                    + String.join(" ", command) + "\n" + stderr);
        }
        return stdout.strip();
    }

    /** Runs git and returns stripped stdout like the Python {@code git} test helper. */
    public static String git(Path directory, String... arguments) throws IOException {
        return gitAt(directory, null, arguments);
    }
}

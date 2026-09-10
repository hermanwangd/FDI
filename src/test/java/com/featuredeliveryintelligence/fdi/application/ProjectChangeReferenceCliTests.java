package com.featuredeliveryintelligence.fdi.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Packaged CLI for the SF-BL-004 change reference exporter (Task 4). Covers
 *  option parsing and stable exit codes (2 usage, 1 contract/runtime refusal,
 *  0 success), default bounds, mixed-file package generation, output-exists
 *  refusal, invalid revisions and ancestry, unsafe logical repository names
 *  (writer PATH_UNSAFE), and fail-closed secret handling that never leaks
 *  rejected bytes into diagnostics or the published package. */
class ProjectChangeReferenceCliTests {
    private static final String COMMAND = "project-change-reference-export";

    @TempDir Path temp;

    @Test
    void handlesOnlyProjectChangeReferenceExportCommand() {
        assertFalse(ProjectChangeReferenceCli.handles(new String[0]));
        assertFalse(ProjectChangeReferenceCli.handles(new String[]{"unrelated"}));
        assertTrue(ProjectChangeReferenceCli.handles(new String[]{COMMAND}));
        assertEquals(5, ProjectChangeReferenceCli.DEFAULT_CONTEXT_LINES);
        assertEquals(262144, ProjectChangeReferenceCli.DEFAULT_MAX_TEXT_BYTES);
    }

    @Test
    void usageErrorsExitTwoForDuplicateMissingUnknownAndNonNumericOptions() throws Exception {
        Path repo = repositoryWithMixedChanges("repo-a");
        String from = git(repo, "rev-parse", "HEAD~1");
        String to = git(repo, "rev-parse", "HEAD");

        assertEquals(2, run(new String[]{COMMAND, "--repository", repo.toString(), "--repository", repo.toString(),
                "--repository-name", "repo-a", "--from", from, "--to", to, "--output", output("dup").toString()}).exitCode());
        Result missingTo = run(new String[]{COMMAND, "--repository", repo.toString(), "--repository-name", "repo-a",
                "--from", from, "--output", output("missing").toString()});
        assertEquals(2, missingTo.exitCode());
        assertTrue(missingTo.stderr().contains("usage:"), missingTo.stderr());
        assertEquals(2, run(new String[]{COMMAND, "--bogus", "x"}).exitCode());
        assertEquals(2, run(new String[]{COMMAND, "--repository", repo.toString(), "--repository-name", "repo-a",
                "--from", from, "--to", to, "--output", output("nan").toString(), "--context-lines", "abc"}).exitCode());
    }

    @Test
    void exportsMixedFilePackageWithDefaultBounds() throws Exception {
        Path repo = repositoryWithMixedChanges("repo-b");
        String from = git(repo, "rev-parse", "HEAD~1");
        String to = git(repo, "rev-parse", "HEAD");
        Path out = output("mixed");

        Result result = run(new String[]{COMMAND, "--repository", repo.toString(), "--repository-name", "repo-b",
                "--from", from, "--to", to, "--output", out.toString()});

        assertEquals(0, result.exitCode(), result.stderr());
        assertTrue(Files.isDirectory(out));
        assertTrue(Files.isRegularFile(out.resolve("CHANGE-SUMMARY.md")));
        assertTrue(Files.isRegularFile(out.resolve("IMPORT-PROMPT.md")));
        assertTrue(Files.isRegularFile(out.resolve("manifest.sha256")));
        for (int n = 1; n <= 3; n++) {
            assertTrue(Files.isRegularFile(out.resolve("changes").resolve("CR-000" + n + ".md")));
        }
        String manifest = Files.readString(out.resolve("manifest.json"), StandardCharsets.UTF_8);
        assertTrue(manifest.contains("\"authority\":\"REFERENCE_ONLY\""), manifest);
        assertTrue(manifest.contains("\"automaticApplicationAllowed\":false"), manifest);
        assertTrue(manifest.contains("\"sharedBaseline\":\"NO_SHARED_BASELINE\""), manifest);
        assertTrue(manifest.contains("DO_NOT_APPLY_BLINDLY")
                || Files.readString(out.resolve("IMPORT-PROMPT.md"), StandardCharsets.UTF_8).contains("DO_NOT_APPLY_BLINDLY"));
        assertTrue(result.stdout().contains("records: 3"), result.stdout());
    }

    @Test
    void refusesExistingOutputAndInvalidRangeWithExitOne() throws Exception {
        Path repo = repositoryWithMixedChanges("repo-c");
        String from = git(repo, "rev-parse", "HEAD~1");
        String to = git(repo, "rev-parse", "HEAD");
        Path out = output("exists");
        assertTrue(out.toFile().mkdirs());

        Result refused = run(new String[]{COMMAND, "--repository", repo.toString(), "--repository-name", "repo-c",
                "--from", from, "--to", to, "--output", out.toString()});
        assertEquals(1, refused.exitCode());
        assertTrue(refused.stderr().contains("OUTPUT_EXISTS"), refused.stderr());

        Result badRevision = run(new String[]{COMMAND, "--repository", repo.toString(), "--repository-name", "repo-c",
                "--from", to.substring(0, 7), "--to", to, "--output", output("short").toString()});
        assertEquals(1, badRevision.exitCode());
        assertTrue(badRevision.stderr().contains("REVISION_INVALID"), badRevision.stderr());

        Result badAncestry = run(new String[]{COMMAND, "--repository", repo.toString(), "--repository-name", "repo-c",
                "--from", to, "--to", from, "--output", output("ancestry").toString()});
        assertEquals(1, badAncestry.exitCode());
        assertTrue(badAncestry.stderr().contains("ANCESTRY_INVALID"), badAncestry.stderr());
    }

    @Test
    void rejectsUnsafeRepositoryNameThroughWriterValidation() throws Exception {
        Path repo = repositoryWithMixedChanges("repo-d");
        String from = git(repo, "rev-parse", "HEAD~1");
        String to = git(repo, "rev-parse", "HEAD");

        Result result = run(new String[]{COMMAND, "--repository", repo.toString(), "--repository-name", "bad name",
                "--from", from, "--to", to, "--output", output("unsafe-name").toString()});

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("PATH_UNSAFE"), result.stderr());
        assertFalse(Files.exists(output("unsafe-name")));
    }

    @Test
    void keepsPathsWithSpacesAndShellMetacharactersLiteral() throws Exception {
        Path repo = repositoryWithMetacharPath();
        String from = git(repo, "rev-parse", "HEAD~1");
        String to = git(repo, "rev-parse", "HEAD");
        Path out = output("metachars");

        Result result = run(new String[]{COMMAND, "--repository", repo.toString(), "--repository-name", "repo-e",
                "--from", from, "--to", to, "--output", out.toString()});

        assertEquals(0, result.exitCode(), result.stderr());
        String manifest = Files.readString(out.resolve("manifest.json"), StandardCharsets.UTF_8);
        assertTrue(manifest.contains("space & $(touch meta).md"), manifest);
        assertFalse(Files.exists(repo.resolve("meta")), "git path must remain data, never a shell command");
    }

    @Test
    void failsClosedOnSecretsWithoutLeakingRejectedBytes() throws Exception {
        Path repo = repositoryWithSecret();
        String from = git(repo, "rev-parse", "HEAD~1");
        String to = git(repo, "rev-parse", "HEAD");
        String secret = "AKIAIOSFODNN7EXAMPLE";
        Path out = output("secret");
        assertTrue(out.toFile().mkdirs());
        Files.writeString(out.resolve(".keep"), "", StandardCharsets.UTF_8);

        Result refused = run(new String[]{COMMAND, "--repository", repo.toString(), "--repository-name", "repo-f",
                "--from", from, "--to", to, "--output", out.toString()});
        assertEquals(1, refused.exitCode());
        assertTrue(refused.stderr().contains("OUTPUT_EXISTS"), refused.stderr());
        assertFalse(refused.stderr().contains(secret), refused.stderr());

        Files.delete(out.resolve(".keep"));
        Files.delete(out);
        Result exported = run(new String[]{COMMAND, "--repository", repo.toString(), "--repository-name", "repo-f",
                "--from", from, "--to", to, "--output", out.toString()});
        assertEquals(0, exported.exitCode(), exported.stderr());
        assertTrue(exported.stdout().contains("excluded: 1"), exported.stdout());
        String manifest = Files.readString(out.resolve("manifest.json"), StandardCharsets.UTF_8);
        assertTrue(manifest.contains("SECRET_DETECTED"), manifest);
        try (Stream<Path> files = Files.walk(out)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                assertFalse(Files.readString(file, StandardCharsets.UTF_8).contains(secret),
                        "secret bytes leaked into " + file);
            }
        }
    }

    private Path output(String name) {
        try {
            Path parent = temp.resolve("out");
            Files.createDirectories(parent);
            return parent.resolve(name);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Path repositoryWithMixedChanges(String name) {
        Path repo = temp.resolve(name);
        write(repo, "src/App.java", "class App {\n    void run() {\n        System.out.println(\"old\");\n    }\n}\n");
        write(repo, "docs/guide.md", "# Guide\n\nold text\n");
        write(repo, "config/app.json", "{\"setting\": 1}\n");
        commitAll(repo, "base");
        write(repo, "src/App.java", "class App {\n    void run() {\n        System.out.println(\"new\");\n    }\n}\n");
        write(repo, "docs/guide.md", "# Guide\n\nnew text\n");
        write(repo, "config/app.json", "{\"setting\": 2}\n");
        commitAll(repo, "change");
        return repo;
    }

    private Path repositoryWithMetacharPath() {
        Path repo = temp.resolve("repo with space");
        write(repo, "notes dir/space & $(touch meta).md", "# Notes\n\nbefore\n");
        commitAll(repo, "base");
        write(repo, "notes dir/space & $(touch meta).md", "# Notes\n\nafter\n");
        commitAll(repo, "change");
        return repo;
    }

    private Path repositoryWithSecret() {
        Path repo = temp.resolve("repo-f");
        write(repo, "docs/readme.md", "# Readme\n\nok\n");
        commitAll(repo, "base");
        write(repo, "config/credentials.txt", "key material " + "AKIAIOSFODNN7EXAMPLE" + "\n");
        commitAll(repo, "add secret");
        return repo;
    }

    private static void write(Path repo, String relative, String content) {
        try {
            Path file = repo.resolve(relative);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void commitAll(Path repo, String message) {
        try {
            Files.createDirectories(repo);
            if (!Files.exists(repo.resolve(".git"))) {
                Process init = new ProcessBuilder("git", "init", "-q", repo.toString()).start();
                assertEquals(0, init.waitFor());
            }
            git(repo, "add", "-A");
            git(repo, "-c", "user.name=Export Tests", "-c", "user.email=tests@example.com", "commit", "-q", "-m", message);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String git(Path repo, String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-C", repo.toString()));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "git timed out: " + String.join(" ", args));
        assertEquals(0, process.exitValue(), "git failed: " + String.join(" ", args) + "\n" + output);
        return output.trim();
    }

    private static Result run(String[] args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = ProjectChangeReferenceCli.run(args,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new Result(exitCode,
                stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
    }

    private record Result(int exitCode, String stdout, String stderr) {
    }
}

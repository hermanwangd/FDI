package com.featuredeliveryintelligence.fdi.portability.changereference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Contract tests for the exact Git change source (SF-BL-004 Task 1): full-commit-ID
 *  enforcement, ancestry checks, NUL-safe add/modify/delete/rename parsing, committed
 *  blob reads, bounded diffs, timeout/output caps, sanitized fail-closed diagnostics. */
class GitChangeSourceTests {

    @TempDir Path root;

    private Path initRepo(String name) throws Exception {
        Path repo = Files.createDirectories(root.resolve(name));
        git(repo, "init", "-q", ".");
        git(repo, "config", "user.email", "tests@fdi.local");
        git(repo, "config", "user.name", "FDI Tests");
        git(repo, "config", "commit.gpgsign", "false");
        return repo;
    }

    private static String git(Path repo, String... args) throws Exception {
        List<String> command = new java.util.ArrayList<>(List.of("git", "-C", repo.toString()));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).start();
        String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        process.waitFor();
        return out;
    }

    private String commitAll(Path repo, String message) throws Exception {
        git(repo, "add", "-A");
        git(repo, "commit", "-q", "--no-gpg-sign", "-m", message);
        return git(repo, "rev-parse", "HEAD").trim();
    }

    private static void write(Path repo, String rel, String content) throws IOException {
        Path file = repo.resolve(rel);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static List<ChangedPath> changes(Path repo, String from, String to) {
        return new GitChangeSource().changes(new GitRange(repo, from, to), 2);
    }

    @Test
    void enumeratesAddModifyDeleteRenameWithNulSafePaths() throws Exception {
        Path repo = initRepo("ops");
        write(repo, "src/App.java", "class App {}\n");
        write(repo, "doc/note.md", "hello\n");
        write(repo, "old name.txt", "x\n");
        String from = commitAll(repo, "base");
        write(repo, "src/App.java", "class App { int x; }\n");
        Files.delete(repo.resolve("doc/note.md"));
        write(repo, "dir with space/new file.md", "added\n");
        Files.move(repo.resolve("old name.txt"), repo.resolve("renamed file.txt"));
        String to = commitAll(repo, "change");

        List<ChangedPath> changes = changes(repo, from, to);

        assertThat(changes).extracting(c -> c.operation() + ":" + (c.newPath() == null ? c.oldPath() : c.newPath()))
                .containsExactlyInAnyOrder("ADD:dir with space/new file.md", "MODIFY:src/App.java",
                        "DELETE:doc/note.md", "RENAME:renamed file.txt");
        ChangedPath rename = changes.stream().filter(c -> c.operation() == Operation.RENAME).findFirst().orElseThrow();
        assertThat(rename.oldPath()).isEqualTo("old name.txt");
        assertThat(rename.oldBytes()).isEqualTo("x\n".getBytes(StandardCharsets.UTF_8));
        assertThat(rename.newBytes()).isEqualTo("x\n".getBytes(StandardCharsets.UTF_8));
        ChangedPath delete = changes.stream().filter(c -> c.operation() == Operation.DELETE).findFirst().orElseThrow();
        assertThat(delete.newBytes()).isNull();
        ChangedPath add = changes.stream().filter(c -> c.operation() == Operation.ADD).findFirst().orElseThrow();
        assertThat(add.newBytes()).isEqualTo("added\n".getBytes(StandardCharsets.UTF_8));
        assertThat(add.unifiedDiff()).anyMatch(line -> line.startsWith("+added"));
        assertThat(delete.unifiedDiff()).anyMatch(line -> line.startsWith("-hello"));
        assertThat(rename.unifiedDiff()).anyMatch(line -> line.startsWith("rename from old name.txt"));
        assertThat(changes).allSatisfy(c -> {
            if (c.oldBlobId() != null) {
                assertThat(c.oldBlobId()).matches("[0-9a-f]{40}");
            }
            if (c.newBlobId() != null) {
                assertThat(c.newBlobId()).matches("[0-9a-f]{40}");
            }
        });
    }

    @Test
    void rejectsInvalidRevisionsAndNonAncestry() throws Exception {
        Path repo = initRepo("revisions");
        write(repo, "a.txt", "a\n");
        String first = commitAll(repo, "base");
        write(repo, "a.txt", "b\n");
        String second = commitAll(repo, "change");
        String tree = git(repo, "rev-parse", "HEAD^{tree}").trim();

        assertInvalid(repo, first.substring(0, 7), second);
        assertInvalid(repo, "HEAD", second);
        assertInvalid(repo, tree, second);
        assertInvalid(repo, "f".repeat(40), second);
        assertThat(changes(repo, first, first)).isEmpty();
        assertThatThrownBy(() -> changes(repo, second, first))
                .isInstanceOfSatisfying(GitChangeException.class,
                        e -> assertThat(e.code()).isEqualTo(FailureCode.ANCESTRY_INVALID));
    }

    private static void assertInvalid(Path repo, String from, String to) {
        assertThatThrownBy(() -> changes(repo, from, to))
                .isInstanceOfSatisfying(GitChangeException.class,
                        e -> assertThat(e.code()).isEqualTo(FailureCode.REVISION_INVALID));
    }

    @Test
    void failsClosedOnOutputCapWithSanitizedDiagnostics() throws Exception {
        Path repo = initRepo("secret-marker-repo-7f3d9c");
        write(repo, "big.txt", "y".repeat(4096) + "\n");
        String from = commitAll(repo, "base");
        write(repo, "big.txt", "z".repeat(4096) + "\n");
        String to = commitAll(repo, "change");

        GitChangeSource capped = new GitChangeSource("git", Duration.ofSeconds(15), 256);
        assertThatThrownBy(() -> capped.changes(new GitRange(repo, from, to), 2))
                .isInstanceOfSatisfying(GitChangeException.class, e -> {
                    assertThat(e.code()).isEqualTo(FailureCode.INPUT_LIMIT_EXCEEDED);
                    assertThat(e).hasMessageContaining("INPUT_LIMIT_EXCEEDED");
                    assertThat(e.getMessage()).doesNotContain(repo.toString());
                    assertThat(e.getMessage()).doesNotContain("y".repeat(64));
                });
    }

    @Test
    void failsClosedWhenGitTimesOut() throws Exception {
        Path repo = initRepo("timeout");
        write(repo, "a.txt", "a\n");
        String from = commitAll(repo, "base");
        write(repo, "a.txt", "b\n");
        String to = commitAll(repo, "change");
        Path fakeGit = root.resolve("slow-git");
        Files.writeString(fakeGit, "#!/bin/sh\ncase \" $* \" in *\" diff-tree \"*) exec sleep 60;; esac\nexec git \"$@\"\n");
        assertThat(fakeGit.toFile().setExecutable(true)).isTrue();

        GitChangeSource slow = new GitChangeSource(fakeGit.toString(), Duration.ofSeconds(5), 65536);
        assertThatThrownBy(() -> slow.changes(new GitRange(repo, from, to), 2))
                .isInstanceOfSatisfying(GitChangeException.class, e -> {
                    assertThat(e.code()).isEqualTo(FailureCode.GIT_COMMAND_FAILED);
                    assertThat(e).hasMessageContaining("timed out");
                });
    }
}

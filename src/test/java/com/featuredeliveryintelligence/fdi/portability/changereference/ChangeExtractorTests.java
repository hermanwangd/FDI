package com.featuredeliveryintelligence.fdi.portability.changereference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Contract tests for bounded excerpt extraction (SF-BL-004 Task 2): verified unified
 *  hunks against bound blobs, deterministic bounded context, Markdown enclosing headings,
 *  conservative Java declarations, stable JSON Pointers / configuration keys with text-hunk
 *  fallback, added-file completeness below the byte limit, truncation above it, and no
 *  unchanged whole-file leakage for modified or deleted files. */
class ChangeExtractorTests {

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

    private static String commitAll(Path repo, String message) throws Exception {
        git(repo, "add", "-A");
        git(repo, "commit", "-q", "--no-gpg-sign", "-m", message);
        return git(repo, "rev-parse", "HEAD").trim();
    }

    private static void write(Path repo, String rel, String content) throws IOException {
        Path file = repo.resolve(rel);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private ChangedPath singleChange(Path repo, String from, String to, int context) {
        List<ChangedPath> changes = new GitChangeSource().changes(new GitRange(repo, from, to), context);
        assertThat(changes).hasSize(1);
        return changes.get(0);
    }

    private ChangedPath docChange() throws Exception {
        Path repo = initRepo("docs");
        write(repo, "doc.md", "# Title\n\nintro\n\n## Auth\n\nalpha\nbeta\n\n## Other\n\ngamma\n");
        String from = commitAll(repo, "base");
        write(repo, "doc.md", "# Title\n\nintro\n\n## Auth\n\nalpha\nbeta\n\n## Other\n\ngamma changed\n");
        return singleChange(repo, from, commitAll(repo, "change"), 2);
    }

    private Path initJsonRepo() throws Exception {
        Path repo = initRepo("json");
        write(repo, "config.json", "{\"server\":{\"host\":\"a\",\"port\":8080},\"name\":\"x\"}\n");
        write(repo, "data.json", "{\"a\":1}\n");
        commitAll(repo, "base");
        return repo;
    }

    @Test
    void modifiedJavaYieldsVerifiedHunksWithBoundedContext() throws Exception {
        Path repo = initRepo("java");
        StringBuilder base = new StringBuilder("class App {\n");
        for (int i = 0; i < 16; i++) base.append("    int v").append(i).append(";\n");
        base.append("}\n");
        write(repo, "src/App.java", base.toString());
        String from = commitAll(repo, "base");
        write(repo, "src/App.java", base.toString().replace("int v3;", "int changed;"));
        String to = commitAll(repo, "change");

        List<ChangeExcerpt> excerpts = new TextChangeExtractor(2, 262144)
                .extract(singleChange(repo, from, to, 2));

        assertThat(excerpts).hasSize(1);
        ChangeExcerpt excerpt = excerpts.get(0);
        assertThat(excerpt.oldStart()).isEqualTo(5);
        assertThat(excerpt.oldEnd()).isEqualTo(5);
        assertThat(excerpt.newStart()).isEqualTo(5);
        assertThat(excerpt.newEnd()).isEqualTo(5);
        assertThat(excerpt.context()).contains("class App");
        assertThat(excerpt.before()).hasSizeLessThanOrEqualTo(2);
        assertThat(excerpt.after()).hasSizeLessThanOrEqualTo(2);
        assertThat(excerpt.truncated()).isFalse();
        assertThat(excerpt.unifiedDiff()).allMatch(l -> l.startsWith("+") || l.startsWith("-") || l.startsWith(" "));
        assertThat(excerpt.unifiedDiff().size()).isLessThan((int) base.toString().lines().count());
        assertThat(excerpt.before()).doesNotContain("int v3;");
    }

    @Test
    void markdownChangeFindsEnclosingHeading() throws Exception {
        List<ChangeExcerpt> excerpts = new TextChangeExtractor(2, 262144).extract(docChange());

        assertThat(excerpts).hasSize(1);
        assertThat(excerpts.get(0).context()).isEqualTo("## Other");
        assertThat(excerpts.get(0).after()).isEmpty();
    }

    @Test
    void extractionIsDeterministicAcrossRuns() throws Exception {
        ChangedPath change = docChange();
        TextChangeExtractor extractor = new TextChangeExtractor(2, 262144);

        assertThat(extractor.extract(change)).isEqualTo(extractor.extract(change));
    }

    @Test
    void addedFileIsCompleteBelowLimitAndTruncatedAbove() throws Exception {
        Path repo = initRepo("added");
        write(repo, "seed.txt", "seed\n");
        String from = commitAll(repo, "base");
        write(repo, "small.md", "# New\n\nfull content\n");
        String to = commitAll(repo, "change");
        ChangedPath small = singleChange(repo, from, to, 2);

        List<ChangeExcerpt> complete = new TextChangeExtractor(2, 262144).extract(small);
        assertThat(complete).hasSize(1);
        assertThat(complete.get(0).truncated()).isFalse();
        assertThat(complete.get(0).before()).isEmpty();
        assertThat(complete.get(0).newStart()).isEqualTo(1);
        assertThat(complete.get(0).newEnd()).isEqualTo(3);
        assertThat(complete.get(0).context()).isEqualTo("# New");

        write(repo, "big.txt", "line\n".repeat(2000));
        List<ChangeExcerpt> truncated = new TextChangeExtractor(2, 512)
                .extract(singleChange(repo, to, commitAll(repo, "bigger"), 2));
        assertThat(truncated).hasSize(1);
        assertThat(truncated.get(0).truncated()).isTrue();
        assertThat(truncated.get(0).newEnd()).isEqualTo(2000);
        assertThat(truncated.get(0).before()).isEmpty();
        assertThat(truncated.get(0).after()).isEmpty();
    }

    @Test
    void deletedAndRenamedFilesProduceHunksNotWholeFiles() throws Exception {
        Path repo = initRepo("delete");
        write(repo, "gone.txt", "one\ntwo\nthree\nfour\nfive\nsix\nseven\neight\n");
        write(repo, "keep.txt", "keep\n");
        String from = commitAll(repo, "base");
        Files.delete(repo.resolve("gone.txt"));
        List<ChangeExcerpt> excerpts = new TextChangeExtractor(2, 262144)
                .extract(singleChange(repo, from, commitAll(repo, "delete"), 2));

        assertThat(excerpts).isNotEmpty();
        assertThat(excerpts).allSatisfy(e -> {
            assertThat(e.newStart()).isEqualTo(0);
            assertThat(e.newEnd()).isEqualTo(0);
            assertThat(e.oldStart()).isGreaterThanOrEqualTo(1);
        });
        assertThat(excerpts.stream().mapToInt(e -> e.unifiedDiff().size()).sum())
                .isLessThan(8 + excerpts.size() * 2);

        Path repo2 = initRepo("rename");
        write(repo2, "before.txt", "alpha\nbeta\ngamma\n");
        String base2 = commitAll(repo2, "base");
        Files.move(repo2.resolve("before.txt"), repo2.resolve("after.txt"));
        assertThat(new TextChangeExtractor(2, 262144)
                .extract(singleChange(repo2, base2, commitAll(repo2, "rename"), 2))).isEmpty();
    }

    @Test
    void invalidUtf8ProducesNoExcerpt() {
        ChangedPath binary = new ChangedPath("a.bin", "a.bin", Operation.MODIFY, "b".repeat(40), "c".repeat(40),
                new byte[]{'a', (byte) 0xC3, (byte) 0x28}, new byte[]{'b', (byte) 0xC3, (byte) 0x28},
                List.of("@@ -1 +1 @@"));
        assertThat(new TextChangeExtractor(2, 262144).extract(binary)).isEmpty();
        assertThat(new StructuredChangeExtractor(new TextChangeExtractor(2, 262144)).extract(binary)).isEmpty();
    }

    @Test
    void hunkReconstructionFailureFailsClosed() {
        ChangedPath corrupt = new ChangedPath("a.txt", "a.txt", Operation.MODIFY, "b".repeat(40), "c".repeat(40),
                "a\nb\n".getBytes(StandardCharsets.UTF_8), "a\nB\n".getBytes(StandardCharsets.UTF_8),
                List.of("@@ -1,2 +1,2 @@", " a", "-b", "+X"));
        assertThatThrownBy(() -> new TextChangeExtractor(2, 262144).extract(corrupt))
                .isInstanceOfSatisfying(GitChangeException.class,
                        e -> assertThat(e.code()).isEqualTo(FailureCode.PACKAGE_VALIDATION_FAILED));
    }

    @Test
    void jsonChangeEmitsStableJsonPointerHint() throws Exception {
        Path repo = initJsonRepo();
        write(repo, "config.json", "{\"server\":{\"host\":\"a\",\"port\":9090},\"name\":\"x\"}\n");
        ChangedPath change = singleChange(repo, git(repo, "rev-parse", "HEAD").trim(), commitAll(repo, "change"), 2);

        List<ChangeExcerpt> excerpts = new StructuredChangeExtractor(new TextChangeExtractor(2, 262144))
                .extract(change);

        assertThat(excerpts).hasSize(1);
        assertThat(excerpts.get(0).context()).isEqualTo("/server/port");
    }

    @Test
    void jsonFormattingOnlyChangeProducesNoExcerpt() throws Exception {
        Path repo = initJsonRepo();
        String from = git(repo, "rev-parse", "HEAD").trim();
        write(repo, "config.json", "{\n  \"server\": {\n    \"host\": \"a\",\n    \"port\": 8080\n  },\n  \"name\": \"x\"\n}\n");
        ChangedPath change = singleChange(repo, from, commitAll(repo, "reformat"), 2);

        assertThat(new StructuredChangeExtractor(new TextChangeExtractor(2, 262144)).extract(change)).isEmpty();
    }

    @Test
    void brokenJsonFallsBackToTextHunks() throws Exception {
        Path repo = initJsonRepo();
        write(repo, "data.json", "{\"a\":2\n");
        ChangedPath change = singleChange(repo, git(repo, "rev-parse", "HEAD").trim(), commitAll(repo, "change"), 2);

        List<ChangeExcerpt> excerpts = new StructuredChangeExtractor(new TextChangeExtractor(2, 262144))
                .extract(change);

        assertThat(excerpts).hasSize(1);
        assertThat(excerpts.get(0).unifiedDiff()).anyMatch(l -> l.startsWith("-{"));
        assertThat(excerpts.get(0).context()).isNull();
    }

    @Test
    void propertiesChangeEmitsKeyHint() throws Exception {
        Path repo = initRepo("properties");
        write(repo, "app.properties", "timeout=30\nretries=3\n");
        String from = commitAll(repo, "base");
        write(repo, "app.properties", "timeout=60\nretries=3\n");
        String to = commitAll(repo, "change");

        List<ChangeExcerpt> excerpts = new StructuredChangeExtractor(new TextChangeExtractor(2, 262144))
                .extract(singleChange(repo, from, to, 2));

        assertThat(excerpts).hasSize(1);
        assertThat(excerpts.get(0).context()).isEqualTo("timeout");
    }
}

package com.featuredeliveryintelligence.fdi.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ports the CLI decisions of {@code tests/test_pkb001_task6_blind_packet.py}:
 * the packaged {@code blind-review-generate} command must preserve the Python
 * CLI's exit-code/stdout contract on a copied Task-6 root.
 */
class BlindReviewCliTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path REPOSITORY = Path.of("").toAbsolutePath();
    private static final String TASK6_DIR = "validation/pkb001/task6-blind-review";

    @TempDir Path temp;

    @Test
    void handlesOnlyBlindReviewCommand() {
        assertFalse(BlindReviewCli.handles(new String[0]));
        assertFalse(BlindReviewCli.handles(new String[]{"unrelated"}));
        assertTrue(BlindReviewCli.handles(new String[]{"blind-review-generate"}));
    }

    @Test
    void completedJudgmentsFailClosedWithoutTracebackOrMutation() throws Exception {
        Path root = copiedTask6Root();
        List<Path> judgmentPaths = List.of(
                root.resolve(TASK6_DIR).resolve("judgment-workspaces/reviewer-01/judgment-template.json"),
                root.resolve(TASK6_DIR).resolve("judgment-workspaces/reviewer-02/judgment-template.json"));
        List<byte[]> before = new ArrayList<>();
        for (Path path : judgmentPaths) {
            before.add(Files.readAllBytes(path));
        }

        Result result = run(new String[]{"blind-review-generate", "--root", root.toString()});

        assertEquals(2, result.exitCode());
        assertEquals("", result.stdout());
        assertTrue(result.stderr().contains("refusing to overwrite completed judgments"),
                result.stderr());
        assertFalse(result.stderr().contains("Exception"), result.stderr());
        assertFalse(result.stderr().contains("\tat "), result.stderr());
        for (int index = 0; index < judgmentPaths.size(); index++) {
            assertArrayEquals(before.get(index), Files.readAllBytes(judgmentPaths.get(index)));
        }
    }

    @Test
    void freshOutputDirectoryGeneratesDeterministicPacket() throws Exception {
        Path root = copiedTask6Root();
        String[] args = new String[]{"blind-review-generate", "--root", root.toString(),
                "--output-dir", "validation/pkb001/task6-cli-review"};

        Result first = run(args);
        Result second = runToNewDirectory(root, args);

        assertEquals(0, first.exitCode(), first.stderr());
        assertEquals("", first.stderr());
        assertEquals(first.stdout(), second.stdout());
        assertEquals(1, first.stdout().lines().count());
        JsonNode summary = JSON.readTree(first.stdout());
        assertEquals("pkb001-task6-blind-comparison-v1", summary.get("packet_id").asText());
        assertTrue(summary.get("packet_sha256").asText().matches("[0-9a-f]{64}"));
        // The regenerated packet must equal the sealed historical packet byte-for-byte.
        assertArrayEquals(Files.readAllBytes(root.resolve(
                        "validation/pkb001/task6-cli-review/blind-review-packet.json")),
                Files.readAllBytes(root.resolve(TASK6_DIR + "/blind-review-packet.json")));
    }

    @Test
    void malformedOptionsFailDeterministicallyWithoutStackTrace() {
        List<String[]> invalid = List.of(
                new String[]{"blind-review-generate", "--root"},
                new String[]{"blind-review-generate", "--root", temp.toString(), "--root", temp.toString()},
                new String[]{"blind-review-generate", "--unknown", "value"},
                new String[]{"blind-review-generate", "--root", " "},
                new String[]{"blind-review-generate", "positional"});

        for (String[] args : invalid) {
            Result first = run(args);
            Result second = run(args);
            assertNotEquals(0, first.exitCode(), List.of(args).toString());
            assertEquals("", first.stdout());
            assertEquals(first.stderr(), second.stderr());
            assertTrue(first.stderr().startsWith("blind-review-generate: INVALID_ARGUMENTS:"),
                    first.stderr());
            assertFalse(first.stderr().contains("Exception"), first.stderr());
            assertFalse(first.stderr().contains("\tat "), first.stderr());
        }
    }

    @Test
    void optionEqualsFormAndDefaultsAreAccepted() throws Exception {
        Path root = copiedTask6Root();
        Result explicit = run(new String[]{"blind-review-generate",
                "--root=" + root, "--output-dir=validation/pkb001/task6-cli-review"});
        assertEquals(0, explicit.exitCode(), explicit.stderr());

        // Default --output-dir points at the sealed copy, which refuses completed judgments.
        Result defaulted = run(new String[]{"blind-review-generate", "--root=" + root});
        assertEquals(2, defaulted.exitCode());
        assertTrue(defaulted.stderr().contains("refusing to overwrite completed judgments"));
    }

    @Test
    void applicationProcessReturnsStableExitCodesWithoutStartingSpring() throws Exception {
        Path root = copiedTask6Root();
        Process valid = javaProcess(new String[]{"blind-review-generate", "--root", root.toString(),
                "--output-dir", "validation/pkb001/task6-process-review"});
        assertEquals(0, valid.waitFor());
        String validOut = new String(valid.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String validErr = new String(valid.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals("pkb001-task6-blind-comparison-v1",
                JSON.readTree(validOut).get("packet_id").asText());
        assertEquals("", validErr);
        assertFalse(validOut.contains("Spring"));

        Process invalid = javaProcess(new String[]{"blind-review-generate", "--root", root.toString()});
        assertEquals(2, invalid.waitFor());
        assertEquals("", new String(invalid.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        String invalidErr = new String(invalid.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(invalidErr.contains("refusing to overwrite completed judgments"), invalidErr);
        assertFalse(invalidErr.contains("Exception"), invalidErr);
    }

    private Result runToNewDirectory(Path root, String[] args) throws Exception {
        // Regenerate against a second fresh output directory so the two runs do not collide.
        String[] second = new String[args.length];
        System.arraycopy(args, 0, second, 0, args.length);
        second[second.length - 1] = "validation/pkb001/task6-cli-review-2";
        Result result = run(second);
        assertArrayEquals(Files.readAllBytes(root.resolve("validation/pkb001/task6-cli-review/blind-review-packet.json")),
                Files.readAllBytes(root.resolve("validation/pkb001/task6-cli-review-2/blind-review-packet.json")));
        return result;
    }

    private Process javaProcess(String[] args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(FdiApplication.class.getName());
        command.addAll(List.of(args));
        return new ProcessBuilder(command).start();
    }

    private Result run(String[] args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exit = BlindReviewCli.run(args, new PrintStream(stdout), new PrintStream(stderr));
        return new Result(exit, stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private Path copiedTask6Root() throws Exception {
        Path target = temp.resolve("cli-root-" + java.util.UUID.randomUUID());
        List<String> paths = new ArrayList<>(List.of(
                ".superpowers/sdd/IMPLEMENTATION-PLAN/task-6-brief.md",
                "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json",
                "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-manifest.json",
                "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-witness.json",
                "validation/pkb001/artifacts/petclinic-graph-818c413.json",
                "validation/pkb001/reverse-task5-pkb001_reverse_run/capability-hypotheses.json",
                "validation/pkb001/reverse-task5-pkb001_reverse_run/manifest.json",
                "validation/pkb001/reverse-task5-pkb001_reverse_run/provenance-witness.json",
                "validation/pkb001/schemas/evaluator-judgment-v0.1.schema.json",
                TASK6_DIR));
        ObjectNode forwardManifest = (ObjectNode) JSON.readTree(REPOSITORY.resolve(
                "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-manifest.json").toFile());
        List<String> names = new ArrayList<>();
        forwardManifest.get("visible_input_sha256").fieldNames().forEachRemaining(names::add);
        paths.addAll(names);
        ObjectNode reverseManifest = (ObjectNode) JSON.readTree(REPOSITORY.resolve(
                "validation/pkb001/reverse-task5-pkb001_reverse_run/manifest.json").toFile());
        reverseManifest.get("visible_input_allowlist")
                .forEach(entry -> paths.add(entry.get("path").asText()));
        for (String relative : new LinkedHashSet<>(paths)) {
            Path source = REPOSITORY.resolve(relative);
            Path destination = target.resolve(relative);
            if (Files.isDirectory(source)) {
                copyTree(source, destination);
            } else {
                Files.createDirectories(destination.getParent());
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return target;
    }

    private static void copyTree(Path source, Path destination) throws Exception {
        try (var stream = Files.walk(source)) {
            for (Path path : stream.toList()) {
                Path relative = source.relativize(path);
                Path target = destination.resolve(relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private record Result(int exitCode, String stdout, String stderr) {}
}

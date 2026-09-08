package com.featuredeliveryintelligence.fdi.validation.task7;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Shared fixture builder for the Task 7 evaluation characterization and CLI
 * tests. Mirrors the {@code copied_evaluation_root} pytest fixture of
 * {@code tests/test_pkb001_task7_evaluation.py}: copies the sealed evaluation
 * inputs (Task 6 blind-review tree, bound forward/reverse runs, evaluator
 * gold) from the real repository into a temporary root that mutation tests
 * can damage without touching the immutable evidence.
 */
public final class Task7EvaluationRoots {
    public static final Path REPOSITORY = Path.of("").toAbsolutePath();
    public static final String TASK6 = "validation/pkb001/task6-blind-review";
    public static final String REPORT_RELATIVE =
            "validation/pkb001/task7-evaluation/evaluation-report.json";
    public static final String PENDING_RELATIVE =
            "validation/pkb001/task7-evaluation/third-review-pending.json";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> INPUT_PATHS = List.of(
            TASK6,
            "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413.json",
            "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-manifest.json",
            "validation/pkb001/artifacts/petclinic-pk-s1-forward-run-818c413-witness.json",
            "validation/pkb001/reverse-task5-pkb001_reverse_run/capability-hypotheses.json",
            "validation/pkb001/reverse-task5-pkb001_reverse_run/manifest.json",
            "validation/pkb001/reverse-task5-pkb001_reverse_run/provenance-witness.json",
            "validation/pkb001/evaluator/petclinic-818c413/gold-mappings.json",
            "validation/pkb001/evaluator/petclinic-818c413/ground-truth-seal.json");

    private Task7EvaluationRoots() { }

    public static Path copyEvaluationRoot(Path target) throws IOException {
        for (String relative : INPUT_PATHS) {
            Path source = REPOSITORY.resolve(relative);
            Path destination = target.resolve(relative);
            Files.createDirectories(destination.getParent() == null
                    ? destination : destination.getParent());
            if (Files.isDirectory(source)) {
                copyDirectory(source, destination);
            } else {
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return target;
    }

    private static void copyDirectory(Path source, Path destination) throws IOException {
        try (var stream = Files.walk(source)) {
            for (Path path : stream.toList()) {
                Path relative = source.relativize(path);
                Path targetPath = destination.resolve(relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * Removes the committed reviewer-03 workspace from a copied root so a
     * fixture models the two-reviewer state. The combined candidate commits
     * reviewer-03 into the repository, so every copied root contains it;
     * tests exercising the "reviewer-03 absent" contract must delete it.
     */
    public static void removeReviewer03Workspace(Path root) throws IOException {
        Path workspace = root.resolve(TASK6).resolve("judgment-workspaces/reviewer-03");
        if (!Files.exists(workspace)) {
            return;
        }
        try (var stream = Files.walk(workspace)) {
            for (Path path : stream.sorted((a, b) -> b.compareTo(a)).toList()) {
                Files.delete(path);
            }
        }
    }

    /** Drops the last judgment of reviewer-01, like the pre-unblinding pytest case. */
    public static void removeLastJudgment(Path root) throws IOException {
        Path judgment = root.resolve(TASK6)
                .resolve("judgment-workspaces/reviewer-01/judgment-template.json");
        JsonNode workspace = JSON.readTree(Files.readAllBytes(judgment));
        ArrayNode rows = (ArrayNode) workspace.get("judgments");
        rows.remove(rows.size() - 1);
        Files.writeString(judgment, JSON.writeValueAsString(workspace));
    }

    /** Appends one byte, like the bound-input digest-mutation pytest cases. */
    public static void appendByte(Path file) throws IOException {
        byte[] content = Files.readAllBytes(file);
        byte[] mutated = new byte[content.length + 1];
        System.arraycopy(content, 0, mutated, 0, content.length);
        mutated[content.length] = '\n';
        Files.write(file, mutated);
    }

    /** Rewrites the sealed key with an empty item list and repairs the manifest digest. */
    public static void emptySealedKeyAndRepairManifest(Path root) throws Exception {
        Path keyPath = root.resolve(TASK6).resolve("sealed-blind-key.json");
        JsonNode key = JSON.readTree(Files.readAllBytes(keyPath));
        ((com.fasterxml.jackson.databind.node.ObjectNode) key).putArray("items");
        Files.writeString(keyPath, JSON.writerWithDefaultPrettyPrinter()
                .writeValueAsString(key) + "\n");
        Path manifestPath = root.resolve(TASK6).resolve("manifest.json");
        JsonNode manifest = JSON.readTree(Files.readAllBytes(manifestPath));
        String digest = sha256Hex(Files.readAllBytes(keyPath));
        ((com.fasterxml.jackson.databind.node.ObjectNode) manifest)
                .put("sealed_key_sha256", digest);
        Files.writeString(manifestPath, JSON.writerWithDefaultPrettyPrinter()
                .writeValueAsString(manifest) + "\n");
    }

    static String sha256Hex(byte[] data) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        StringBuilder hex = new StringBuilder();
        for (byte value : digest.digest(data)) {
            hex.append(String.format("%02x", value));
        }
        return hex.toString();
    }
}

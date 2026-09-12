package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.SfBl002RouteEffectivenessRun;
import static com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.SfBl002RouteEffectivenessRun.*;

/** Exact-revision calibration producer entry point; never reads evaluator artifacts. */
public final class MethodCalibrationRun {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT, SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    private MethodCalibrationRun() { }
    public static void main(String[] args) throws Exception {
        run(args, false);
    }

    static void run(String[] args, boolean qualified) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("usage: <five-input-root> <exact-source-root> <new-output-root>");
        Path output = Path.of(args[2]).toAbsolutePath().normalize();
        if (Files.exists(output, LinkOption.NOFOLLOW_LINKS)) throw new IllegalArgumentException("OUTPUT_EXISTS");
        Path inputs = Path.of(args[0]).toRealPath();
        Path source = Path.of(args[1]).toRealPath();
        Map<String, String> sealed = sealed();
        verifyInputs(inputs, sealed);
        verifyCheckout(source);
        List<Path> files = verifySources(inputs, source);
        // PropertiesLauncher must execute the same sole JAR whose bytes bind the extractor.
        Path runtime = Path.of(System.getProperty("java.class.path")).toAbsolutePath().normalize();
        if (!runtime.toString().endsWith(".jar") || !Files.isRegularFile(runtime)
                || Files.size(runtime) > 128L * 1024 * 1024) throw new IllegalArgumentException("SINGLE_RUNTIME_JAR_REQUIRED");
        String jarSha = digest(runtime);
        var binding = new CalibrationProducer.Binding(SOURCE_REVISION, List.of("src/main/java"),
                List.of("src/test/java"), sha(JSON.writeValueAsBytes(sealed)), jarSha);
        Files.createDirectory(output);
        String start = Instant.now().toString();
        Path seedOutput = Files.createDirectory(output.resolve("seed"));
        SfBl002RouteEffectivenessRun.generate(inputs, source, seedOutput);
        var seeds = JSON.readTree(seedOutput.resolve(PROPOSAL_PATH).toFile());
        var index = new SourceMethodIndex(source, files);
        var baseline = CalibrationProducer.produce(binding, seeds, index, qualified);
        CalibrationProducer.Artifact improved;
        if (qualified) {
            var intents = JSON.readTree(inputs.resolve(INTENTS_PATH).toFile());
            var observations = JSON.readTree(seedOutput.resolve(OBSERVATIONS_PATH).toFile());
            var handlers = JSON.readTree(seedOutput.resolve(ROUTE_INDEX_PATH).toFile());
            var selected = new ArrayList<>(ScenarioEvidenceSelector.select(intents, observations, handlers, source));
            selected.addAll(RedirectEvidenceAssociation.select(selected, index, observations, handlers, source));
            List<String> scenarios = new ArrayList<>();
            intents.required("records").forEach(record -> scenarios.add(record.required("scenarioId").asText()));
            improved = QualifiedCalibrationProducer.produce(binding, scenarios, selected, index);
            write(output.resolve("selected-evidence.json"), selected);
        } else {
            improved = CalibrationProducer.produce(binding, seeds, index, true);
        }
        verifyInputs(inputs, sealed);
        verifySources(inputs, source);
        verifyCheckout(source);
        if (!jarSha.equals(digest(runtime))) throw new IllegalArgumentException("RUNTIME_CHANGED");
        write(output.resolve("baseline.json"), baseline);
        write(output.resolve("improved.json"), improved);
        Map<String, String> outputs = new TreeMap<>();
        try (var walk = Files.walk(output)) {
            for (Path path : walk.filter(Files::isRegularFile).sorted().toList())
                outputs.put(output.relativize(path).toString(), digest(path));
        }
        write(output.resolve("generation.json"), Map.of(
                "executionId", qualified ? "SF-BL-005-METHOD-QUALITY-006" : "SF-BL-005-METHOD-CALIBRATION-005", "datasetKind", "CALIBRATION",
                "binding", binding, "inputs", sealed, "outputs", outputs,
                "startedAt", start, "finishedAt", Instant.now().toString(),
                "authority", "PROPOSAL_ONLY", "expansion", Map.of("depth", qualified ? 3 : 2, "maxMethodsPerScenario", 64),
                "limitations", List.of("STATIC_CALL_NOT_OBSERVED_EXECUTION", "GRAPHIFY_SNAPSHOT_REUSED_NOT_REINDEXED",
                        "NO_EVALUATOR_INPUTS", "NO_UPSTREAM_TEST_EXECUTION", "TOP_LEVEL_SOURCE_DECLARATIONS_ONLY")));
    }

    static void verifyInputs(Path root, Map<String, String> expected) throws Exception {
        var seen = new HashSet<String>();
        try (var walk = Files.walk(root)) {
            for (Path path : walk.toList()) {
                if (Files.isSymbolicLink(path)) throw new IllegalArgumentException("SYMLINK_INPUT");
                if (Files.isDirectory(path)) continue;
                String relative = root.relativize(path).toString();
                if (!expected.containsKey(relative) || !Files.isRegularFile(path)
                        || Files.size(path) > 16L * 1024 * 1024) throw new IllegalArgumentException("UNEXPECTED_INPUT");
                if (!digest(path).equals(expected.get(relative))) throw new IllegalArgumentException("INPUT_DIGEST_MISMATCH");
                seen.add(relative);
            }
        }
        if (!seen.equals(expected.keySet())) throw new IllegalArgumentException("MISSING_INPUT");
    }

    private static List<Path> verifySources(Path inputs, Path source) throws Exception {
        var digests = JSON.readTree(inputs.resolve(TEST_EVIDENCE_PATH).toFile()).required("input_digests");
        List<Path> files = new ArrayList<>();
        var fields = digests.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            String relative = entry.getKey();
            if (!relative.matches("src/(main|test)/[A-Za-z0-9_./-]+\\.java")
                    || List.of(relative.split("/")).contains("..")) throw new IllegalArgumentException("SOURCE_PATH");
            Path path = source.resolve(relative);
            Path part = source;
            for (Path element : Path.of(relative)) {
                part = part.resolve(element);
                if (Files.isSymbolicLink(part)) throw new IllegalArgumentException("SOURCE_SYMLINK");
            }
            if (!Files.isRegularFile(path) || Files.size(path) > 1024 * 1024
                    || !digest(path).equals(entry.getValue().asText())) throw new IllegalArgumentException("SOURCE_CHANGED");
            if (relative.startsWith("src/main/java/")) files.add(path);
        }
        return files.stream().sorted().toList();
    }

    private static void verifyCheckout(Path source) throws Exception {
        if (!git(source, "rev-parse", "HEAD").trim().equals(SOURCE_REVISION)
                || !git(source, "status", "--porcelain", "--untracked-files=all").isBlank())
            throw new IllegalArgumentException("EXACT_CLEAN_SOURCE_REQUIRED");
    }

    private static String git(Path source, String... args) throws Exception {
        var command = new ArrayList<>(List.of("git", "-C", source.toString()));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalArgumentException("GIT_TIMEOUT");
        }
        byte[] bytes = process.getInputStream().readNBytes(65537);
        if (process.exitValue() != 0 || bytes.length > 65536) throw new IllegalArgumentException("GIT_FAILURE");
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static Map<String, String> sealed() {
        return new TreeMap<>(Map.of(INTENTS_PATH, INTENTS_SHA256, INTENT_ACCEPTANCE_PATH, INTENT_ACCEPTANCE_SHA256,
                TEST_EVIDENCE_PATH, TEST_EVIDENCE_SHA256, GRAPH_PATH, GRAPH_SHA256,
                RUNTIME_EVIDENCE_PATH, RUNTIME_EVIDENCE_SHA256));
    }

    private static String digest(Path file) throws Exception {
        var sha = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(file)) {
            byte[] buffer = new byte[65536];
            for (int count; (count = input.read(buffer)) != -1;) sha.update(buffer, 0, count);
        }
        return HexFormat.of().formatHex(sha.digest());
    }

    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static void write(Path path, Object value) throws Exception {
        Files.write(path, JSON.writeValueAsBytes(value), StandardOpenOption.CREATE_NEW);
    }
}

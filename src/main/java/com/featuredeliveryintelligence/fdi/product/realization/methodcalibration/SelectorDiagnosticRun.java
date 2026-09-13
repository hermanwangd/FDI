package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.LogicalType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Digest-pinned standalone selector diagnostic runner. Consumes only manifest-listed
 * bundle bytes, never live Git state; a sealed run proves nothing about downstream gates.
 */
public final class SelectorDiagnosticRun {
    @FunctionalInterface
    interface CheckedAction { void run() throws Exception; }
    static final String INPUT_SCHEMA = "SELECTOR-DIAGNOSTIC-INPUT-001";
    static final String OUTPUT_SCHEMA = "SELECTOR-DIAGNOSTIC-OUTPUT-001";
    static final String SEAL_SCHEMA = "SELECTOR-DIAGNOSTIC-SEAL-001";
    static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    static final long MAX_TOTAL_BYTES = 100L * 1024 * 1024;
    static final int MAX_FILES = 1000;
    static final long MAX_RUNTIME_BYTES = 128L * 1024 * 1024;

    record FileEntry(String path, String sha256) { }

    record Manifest(String executionId, String frameworkRevision, String sourceRevision,
                    List<FileEntry> intents, List<FileEntry> observations, List<FileEntry> handlers,
                    List<FileEntry> testFiles) { }

    private static final JsonMapper STRICT = JsonMapper.builder(JsonFactory.builder()
                    .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                    .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(64).build()).build())
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
                    DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES,
                    DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .withCoercionConfig(LogicalType.Textual, config -> config
                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
            .build();

    private static final ObjectMapper OUT = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT, SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    private SelectorDiagnosticRun() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException(
                "usage: <manifest> <manifest-sha256> <bundle-root> <new-output-directory>");
        run(Path.of(args[0]), args[1], Path.of(args[2]), Path.of(args[3]),
                requireRuntimeJar(System.getProperty("java.class.path")));
    }

    static Path requireRuntimeJar(String classPath) throws IOException {
        Path runtime = Path.of(classPath).toAbsolutePath().normalize();
        if (!runtime.toString().endsWith(".jar") || !Files.isRegularFile(runtime)
                || Files.size(runtime) > MAX_RUNTIME_BYTES) throw new IllegalArgumentException("SINGLE_RUNTIME_JAR_REQUIRED");
        return runtime;
    }

    static void run(Path manifestPath, String manifestSha256, Path bundleRoot, Path outputDirectory,
                    Path runtimeJar) throws Exception {
        run(manifestPath, manifestSha256, bundleRoot, outputDirectory, runtimeJar, () -> { });
    }

    static void run(Path manifestPath, String manifestSha256, Path bundleRoot, Path outputDirectory,
                    Path runtimeJar, CheckedAction afterSourceValidation) throws Exception {
        byte[] manifestBytes = readBounded(manifestPath, MAX_FILE_BYTES);
        require(digest(manifestBytes).equals(manifestSha256), "INVALID_MANIFEST_DIGEST");
        Manifest manifest = parseManifest(manifestBytes);
        Map<String, String> expected = validateManifest(manifest);
        Path bundle = bundleRoot.toAbsolutePath().normalize();
        validateBundleInventory(bundle, expected);
        afterSourceValidation.run();
        byte[] runtimeBytes = readBounded(runtimeJar, MAX_RUNTIME_BYTES);
        String runtimeSha = digest(runtimeBytes);

        Path output = outputDirectory.toAbsolutePath().normalize();
        require(!Files.exists(output, LinkOption.NOFOLLOW_LINKS), "OUTPUT_EXISTS");
        Path scratch = Files.createTempDirectory("selector-diagnostic-");
        try {
            copyVerifiedBundle(bundle, expected, scratch);
            Files.createDirectory(output);
            boolean sealed = false;
            try {
            Files.write(output.resolve("runtime.jar"), runtimeBytes);
            JsonNode intents = readTree(scratch.resolve(manifest.intents().get(0).path()));
            JsonNode observations = readTree(scratch.resolve(manifest.observations().get(0).path()));
            JsonNode handlers = readTree(scratch.resolve(manifest.handlers().get(0).path()));
            Set<String> testPaths = new LinkedHashSet<>();
            for (FileEntry entry : manifest.testFiles()) testPaths.add(entry.path());
            for (JsonNode observation : observations.required("observations")) {
                require(testPaths.contains(observation.required("testSourcePath").asText()),
                        "UNLISTED_OBSERVATION_SOURCE");
            }
            ScenarioEvidenceSelector.SelectionDiagnostics diagnostics =
                    ScenarioEvidenceSelector.selectWithDiagnostics(intents, observations, handlers, scratch);
            List<ScenarioEvidenceSelector.Seed> legacy =
                    ScenarioEvidenceSelector.select(intents, observations, handlers, scratch);
            require(legacy.equals(diagnostics.seeds()), "SEED_PARITY_FAILURE");
            write(output.resolve("diagnostics.json"), diagnosticsPayload(manifest, manifestSha256, expected,
                    diagnostics));
            Map<String, String> outputs = new TreeMap<>();
            outputs.put("diagnostics.json", digest(Files.readAllBytes(output.resolve("diagnostics.json"))));
            outputs.put("runtime.jar", runtimeSha);
            Map<String, Object> seal = new LinkedHashMap<>();
            seal.put("schema", SEAL_SCHEMA);
            seal.put("executionId", manifest.executionId());
            seal.put("manifestSha256", manifestSha256);
            seal.put("inputs", new TreeMap<>(expected));
            Map<String, Object> runtime = new LinkedHashMap<>();
            runtime.put("name", "runtime.jar");
            runtime.put("sha256", runtimeSha);
            runtime.put("bytes", runtimeBytes.length);
            seal.put("runtime", runtime);
            seal.put("outputs", outputs);
            seal.put("state", "SEALED");
            write(output.resolve("seal.json"), seal);
            sealed = true;
            System.out.println("SELECTOR-DIAGNOSTIC " + manifest.executionId()
                    + " scenarios=" + diagnostics.scenarios().size()
                    + " evaluated=" + diagnostics.scenarios().stream()
                            .mapToInt(ScenarioEvidenceSelector.ScenarioSelection::evaluated).sum()
                    + " accepted=" + diagnostics.seeds().size() + " SEALED");
            } catch (RuntimeException | IOException failure) {
                if (!sealed) markPartial(output, failure);
                throw failure;
            }
        } finally {
            deleteRecursively(scratch);
        }
    }

    private static Manifest parseManifest(byte[] bytes) {
        try {
            Manifest manifest = STRICT.readValue(bytes, Manifest.class);
            require(manifest != null, "INVALID_MANIFEST_SCHEMA");
            return manifest;
        } catch (IOException failure) {
            throw new IllegalArgumentException("INVALID_MANIFEST_SCHEMA", failure);
        }
    }

    private static Map<String, String> validateManifest(Manifest manifest) {
        require(manifest.executionId() != null && !manifest.executionId().isBlank(), "INVALID_EXECUTION_ID");
        for (String revision : List.of(manifest.frameworkRevision(), manifest.sourceRevision())) {
            require(revision != null && revision.matches("[a-f0-9]{40}"), "INVALID_REVISION");
        }
        require(manifest.intents() != null && manifest.observations() != null
                && manifest.handlers() != null, "MISSING_ROLE_FILE");
        require(manifest.intents().size() == 1 && manifest.observations().size() == 1
                && manifest.handlers().size() == 1, "ROLE_FILE_COUNT");
        Map<String, String> expected = new LinkedHashMap<>();
        Set<String> seen = new LinkedHashSet<>();
        index(expected, seen, manifest.intents(), false);
        index(expected, seen, manifest.observations(), false);
        index(expected, seen, manifest.handlers(), false);
        index(expected, seen, manifest.testFiles(), true);
        require(expected.size() <= MAX_FILES, "FILE_LIMIT_EXCEEDED");
        return expected;
    }

    private static void index(Map<String, String> expected, Set<String> seen, List<FileEntry> entries,
                              boolean testFile) {
        for (FileEntry entry : entries) {
            String path = entry.path();
            path(path);
            require(entry.sha256() != null && entry.sha256().matches("[a-f0-9]{64}"), "INVALID_DIGEST");
            if (testFile) require(path.startsWith("src/test/java/"), "TEST_PATH_PREFIX");
            else require(!path.contains("/"), "ROLE_PATH_NOT_AT_BUNDLE_ROOT");
            require(seen.add(path), "DUPLICATE_PATH");
            expected.put(path, entry.sha256());
        }
    }

    private static void validateBundleInventory(Path bundle, Map<String, String> expected) throws IOException {
        require(Files.isDirectory(bundle) && !Files.isSymbolicLink(bundle), "INVALID_BUNDLE_ROOT");
        Set<String> seen = new LinkedHashSet<>();
        long total = 0;
        try (var walk = Files.walk(bundle)) {
            for (Path path : walk.toList()) {
                if (Files.isSymbolicLink(path)) throw new IllegalArgumentException("SYMLINK_INPUT");
                if (Files.isDirectory(path)) continue;
                String relative = bundle.relativize(path).toString();
                require(Files.isRegularFile(path) && expected.containsKey(relative), "UNEXPECTED_INPUT");
                long size = Files.size(path);
                require(size <= MAX_FILE_BYTES, "INPUT_TOO_LARGE");
                total += size;
                require(total <= MAX_TOTAL_BYTES, "TOTAL_INPUT_TOO_LARGE");
                seen.add(relative);
            }
        }
        require(seen.equals(expected.keySet()), "MISSING_INPUT");
    }

    private static void copyVerifiedBundle(Path bundle, Map<String, String> expected, Path scratch)
            throws IOException {
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            byte[] bytes = readBounded(bundle.resolve(entry.getKey()), MAX_FILE_BYTES);
            require(digest(bytes).equals(entry.getValue()), "INPUT_DIGEST_MISMATCH");
            Path copy = scratch.resolve(entry.getKey());
            if (copy.getParent() != null) Files.createDirectories(copy.getParent());
            Files.write(copy, bytes);
        }
    }

    private static Map<String, Object> diagnosticsPayload(Manifest manifest, String manifestSha256,
            Map<String, String> inputs, ScenarioEvidenceSelector.SelectionDiagnostics diagnostics) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schema", OUTPUT_SCHEMA);
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("executionId", manifest.executionId());
        identity.put("frameworkRevision", manifest.frameworkRevision());
        identity.put("sourceRevision", manifest.sourceRevision());
        identity.put("manifestSha256", manifestSha256);
        payload.put("identity", identity);
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("files", inputs.size());
        counts.put("intents", manifest.intents().size());
        counts.put("observations", manifest.observations().size());
        counts.put("handlers", manifest.handlers().size());
        counts.put("testFiles", manifest.testFiles().size());
        payload.put("counts", counts);
        List<Map<String, Object>> scenarios = new ArrayList<>();
        Map<String, Integer> totals = zeroReasonCounts();
        int evaluated = 0;
        int accepted = 0;
        for (ScenarioEvidenceSelector.ScenarioSelection selection : diagnostics.scenarios()) {
            Map<String, Object> scenario = new LinkedHashMap<>();
            scenario.put("scenarioId", selection.scenarioId());
            scenario.put("evaluated", selection.evaluated());
            scenario.put("accepted", selection.accepted());
            scenario.put("rejected", selection.rejected());
            Map<String, Integer> reasons = zeroReasonCounts();
            List<Map<String, Object>> pairs = new ArrayList<>();
            for (ScenarioEvidenceSelector.PairDiagnostic pair : selection.pairs()) {
                reasons.merge(pair.reason().name(), 1, Integer::sum);
                totals.merge(pair.reason().name(), 1, Integer::sum);
                Map<String, Object> record = new LinkedHashMap<>();
                record.put("observationRef", pair.observationRef());
                record.put("reason", pair.reason().name());
                pairs.add(record);
            }
            scenario.put("reasonCounts", reasons);
            scenario.put("pairs", pairs);
            scenarios.add(scenario);
            evaluated += selection.evaluated();
            accepted += selection.accepted();
        }
        payload.put("scenarios", scenarios);
        Map<String, Object> selection = new LinkedHashMap<>();
        selection.put("evaluated", evaluated);
        selection.put("accepted", accepted);
        selection.put("rejected", evaluated - accepted);
        selection.put("reasonCounts", totals);
        List<Map<String, Object>> seeds = new ArrayList<>();
        for (ScenarioEvidenceSelector.Seed seed : diagnostics.seeds()) {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("scenarioId", seed.scenarioId());
            record.put("productionIdentity", seed.productionIdentity());
            record.put("observationRef", seed.observationRef());
            record.put("action", seed.action());
            record.put("conditions", seed.conditions());
            record.put("role", seed.role());
            seeds.add(record);
        }
        selection.put("seeds", seeds);
        selection.put("seedParity", true);
        payload.put("selection", selection);
        return payload;
    }

    private static Map<String, Integer> zeroReasonCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ScenarioEvidenceSelector.DiagnosticReason reason : ScenarioEvidenceSelector.DiagnosticReason.values()) {
            counts.put(reason.name(), 0);
        }
        return counts;
    }

    private static JsonNode readTree(Path path) throws IOException {
        return OUT.readTree(readBounded(path, MAX_FILE_BYTES));
    }

    private static byte[] readBounded(Path path, long max) throws IOException {
        try (var input = Files.newInputStream(path)) {
            byte[] bytes = input.readNBytes(Math.toIntExact(max) + 1);
            require(bytes.length <= max, "INPUT_TOO_LARGE");
            return bytes;
        }
    }

    private static void write(Path path, Object value) throws IOException {
        Files.write(path, OUT.writeValueAsBytes(value));
    }

    private static void markPartial(Path output, Exception failure) {
        try {
            Files.writeString(output.resolve("partial.txt"),
                    failure.getClass().getSimpleName() + ":" + failure.getMessage());
        } catch (IOException ignored) {
            // Best effort: absence of seal.json already marks the output incomplete.
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }

    static void path(String path) {
        require(path != null && !path.isBlank() && !path.contains("\\") && !path.contains(":")
                && !path.chars().anyMatch(Character::isISOControl), "INVALID_PATH");
        require(!Path.of(path).isAbsolute(), "ABSOLUTE_PATH");
        for (String segment : path.split("/", -1)) {
            require(!segment.isBlank() && !segment.equals(".") && !segment.equals(".."), "NONCANONICAL_PATH");
        }
    }

    static String digest(Path file) throws IOException {
        try (var input = Files.newInputStream(file)) {
            MessageDigest sha = sha();
            byte[] buffer = new byte[65536];
            for (int count; (count = input.read(buffer)) != -1;) sha.update(buffer, 0, count);
            return HexFormat.of().formatHex(sha.digest());
        }
    }

    static String digest(byte[] bytes) {
        return HexFormat.of().formatHex(sha().digest(bytes));
    }

    private static MessageDigest sha() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    static void require(boolean condition, String reason) {
        if (!condition) throw new IllegalArgumentException(reason);
    }
}

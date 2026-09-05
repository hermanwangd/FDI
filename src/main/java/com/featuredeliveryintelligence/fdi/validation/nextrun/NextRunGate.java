package com.featuredeliveryintelligence.fdi.validation.nextrun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * Fail-closed validation for a PKB-001 v0.2 proposal run request. Ports the
 * observable behavior of the transitional Python consumer
 * {@code pkb001_next_run_gate.py}: request snapshot with a hostile-node and
 * size budget, sha256-verified allowlisted generation inputs under a resolved
 * trusted root, forbidden-path token checks, the checked-in Draft 2020-12
 * proposal schema plus the layered component/evidence identity checks,
 * revision and graph-digest binding, and the committed {@code HEAD} run-ID
 * registry over {@code validation/pkb001} via bounded git subprocesses.
 * READY is not generation permission; every failure is BLOCKED with no mappings.
 */
public final class NextRunGate {
    public static final String SKILL_PATH = "skills/pkb001/pk-s1-product-realization-v0.2/SKILL.md";
    public static final String SCHEMA_PATH = "validation/pkb001/schemas/realization-proposal-v0.2.schema.json";

    private static final Set<String> REQUIRED_INPUT_KINDS =
            Set.of("PRODUCT_SEMANTICS", "GRAPHIFY_BINDING_EVIDENCE", "FROZEN_GRAPH", "PKS1_SKILL");
    private static final Set<String> FORBIDDEN_TOKENS =
            Set.of("evaluator", "gold", "judgments", "comparison", "evaluation", "task6", "task7");
    private static final Set<String> SYMBOL_GRANULARITIES = Set.of("TYPE", "METHOD", "TEMPLATE", "CONFIGURATION");
    private static final Set<String> NON_SYMBOL_GRANULARITIES = Set.of("REPOSITORY", "FILE");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final int MAX_REQUEST_NODES = 100_000;
    private static final int MAX_REQUEST_DEPTH = 64;
    private static final long GIT_TIMEOUT_MILLIS = 15_000;
    private static final long MAX_GIT_OUTPUT_BYTES = 16L * 1024 * 1024;
    private static final ObjectMapper JSON = new ObjectMapper();

    public NextRunReport validate(Path trustedRoot, JsonNode request) {
        try {
            Path rootAbsolute = trustedRoot.toAbsolutePath().normalize();
            JsonNode snapshot = snapshot(request, 0, new int[] {MAX_REQUEST_NODES});
            return validateResolved(rootAbsolute, snapshot);
        } catch (RuntimeException failure) {
            // This public trust boundary never exposes dependency, filesystem,
            // or hostile-state exceptions as a readiness success or traceback.
            return NextRunReport.blocked();
        }
    }

    // ------------------------------------------------------------------
    // Request snapshot (hostile containers, JSON-only values, size budget)
    // ------------------------------------------------------------------

    private static JsonNode snapshot(JsonNode node, int depth, int[] budget) {
        budget[0]--;
        if (budget[0] < 0 || depth > MAX_REQUEST_DEPTH) {
            throw new IllegalArgumentException("request is too large");
        }
        if (node == null) {
            return null;
        }
        if (node instanceof ObjectNode object) {
            ObjectNode copy = JsonNodeFactory.instance.objectNode();
            var fields = object.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                copy.set(field.getKey(), snapshot(field.getValue(), depth + 1, budget));
            }
            return copy;
        }
        if (node instanceof ArrayNode array) {
            ArrayNode copy = JsonNodeFactory.instance.arrayNode();
            for (JsonNode child : array) {
                copy.add(snapshot(child, depth + 1, budget));
            }
            return copy;
        }
        if (node.isTextual() || node.isBoolean() || node.isNull()) {
            return node;
        }
        if (node.isIntegralNumber()) {
            return node;
        }
        if (node.isFloatingPointNumber() && Double.isFinite(node.doubleValue())) {
            return node;
        }
        throw new IllegalArgumentException("request contains a non-JSON or hostile value");
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    private NextRunReport validateResolved(Path rootAbsolute, JsonNode request) {
        Path rootReal = realPathOr(rootAbsolute, rootAbsolute);
        Set<String> reasons = new TreeSet<>();
        if (!request.isObject()) {
            reasons.add("REQUEST_INVALID");
            request = JsonNodeFactory.instance.objectNode();
        }
        JsonNode inputsNode = request.get("generation_inputs");
        List<JsonNode> inputs = new ArrayList<>();
        if (inputsNode != null && inputsNode.isArray()) {
            inputsNode.forEach(inputs::add);
        } else {
            reasons.add("REQUIRED_INPUT_SET_INVALID");
        }
        List<JsonNode> safeItems = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (JsonNode item : inputs) {
            if (item == null || !item.isObject()) {
                reasons.add("INPUT_INVALID");
                reasons.add("GENERATION_INPUT_NOT_ALLOWLISTED");
                continue;
            }
            JsonNode kind = item.get("kind");
            if (kind == null || !kind.isTextual() || !REQUIRED_INPUT_KINDS.contains(kind.asText())) {
                reasons.add("GENERATION_INPUT_NOT_ALLOWLISTED");
                continue;
            }
            safeItems.add(item);
            counts.merge(kind.asText(), 1, Integer::sum);
        }
        for (String kind : REQUIRED_INPUT_KINDS) {
            if (counts.getOrDefault(kind, 0) != 1) {
                reasons.add("REQUIRED_INPUT_SET_INVALID");
                break;
            }
        }
        Map<String, byte[]> inputBytes = new LinkedHashMap<>();
        for (JsonNode item : safeItems) {
            if (forbiddenPath(item.get("path"))) {
                reasons.add("FORBIDDEN_GENERATION_INPUT");
            }
            byte[] data = readVerified(rootAbsolute, rootReal, item, reasons);
            if (counts.get(item.get("kind").asText()) == 1) {
                inputBytes.put(item.get("kind").asText(), data);
            }
        }
        Map<String, JsonNode> byKind = new LinkedHashMap<>();
        for (JsonNode item : safeItems) {
            if (counts.get(item.get("kind").asText()) == 1) {
                byKind.put(item.get("kind").asText(), item);
            }
        }
        JsonNode skill = byKind.getOrDefault("PKS1_SKILL", JsonNodeFactory.instance.objectNode());
        JsonNode skillPath = skill.get("path");
        if (!textEquals(skillPath, SKILL_PATH)) {
            reasons.add("SKILL_VERSION_NOT_SELECTED");
        } else if (inputBytes.get("PKS1_SKILL") == null) {
            reasons.add("SKILL_DIGEST_MISMATCH");
        }
        JsonNode semantics = loadJsonBytes(inputBytes.get("PRODUCT_SEMANTICS"), reasons);
        JsonNode binding = loadJsonBytes(inputBytes.get("GRAPHIFY_BINDING_EVIDENCE"), reasons);
        JsonNode graph = byKind.getOrDefault("FROZEN_GRAPH", JsonNodeFactory.instance.objectNode());
        if (!textEquals(semantics.get("status"), "FROZEN")) {
            reasons.add("PRODUCT_SEMANTICS_NOT_FROZEN");
        }
        if (!textEquals(semantics.get("owner"), "PRODUCT_TEAM")) {
            reasons.add("PRODUCT_SEMANTICS_OWNER_INVALID");
        }
        if (!textEquals(binding.get("result"), "EXACTLY_BOUND")) {
            reasons.add("GRAPHIFY_BINDING_INVALID");
        }
        JsonNode queryBounds = binding.get("query_bounds");
        if (queryBounds == null || !queryBounds.isObject() || queryBounds.isEmpty()) {
            reasons.add("GRAPHIFY_QUERY_BOUNDS_MISSING");
        }
        JsonNode proposal = request.get("proposal");
        JsonSchema validator = loadValidator(rootAbsolute, reasons);
        if (validator != null) {
            try {
                if (!validator.validate(proposal == null ? JsonNodeFactory.instance.nullNode() : proposal).isEmpty()) {
                    reasons.add("SCHEMA_INVALID");
                }
            } catch (RuntimeException failure) {
                reasons.add("SCHEMA_INVALID");
            }
        }
        JsonNode revision = proposalStructure(proposal, reasons);
        if (!valueEquals(semantics.get("applicable_source_commit_sha"), revision)
                || !valueEquals(binding.get("requested_revision"), revision)
                || !valueEquals(binding.get("indexed_revision"), revision)) {
            reasons.add("REVISION_BINDING_MISMATCH");
        }
        if (componentRevisionMismatch(proposal, revision)) {
            reasons.add("COMPONENT_REVISION_MISMATCH");
        }
        byte[] graphData = inputBytes.get("FROZEN_GRAPH");
        String verifiedGraph = graphData == null ? null : sha256Hex(graphData);
        if (verifiedGraph == null || !valueEquals(graph.get("sha256"), JsonNodeFactory.instance.textNode(verifiedGraph))) {
            reasons.add("FROZEN_GRAPH_DIGEST_MISMATCH");
        }
        boolean proposalGraphMismatch = proposal != null && proposal.isObject()
                && !valueEquals(proposal.get("graph_sha256"),
                        verifiedGraph == null ? null : JsonNodeFactory.instance.textNode(verifiedGraph));
        if (!valueEquals(binding.get("graph_sha256"),
                verifiedGraph == null ? null : JsonNodeFactory.instance.textNode(verifiedGraph))
                || proposalGraphMismatch) {
            reasons.add("GRAPH_BINDING_DIGEST_MISMATCH");
        }
        JsonNode runIdNode = proposal != null && proposal.isObject() ? proposal.get("run_id") : null;
        String runId = plainNonempty(runIdNode) ? runIdNode.asText() : null;
        if (runId == null) {
            reasons.add("RUN_ID_INVALID");
            return report(reasons, null, skill.get("path"), skill.get("sha256"));
        }
        if (committedRunIds(rootAbsolute, reasons).contains(runId)) {
            reasons.add("RUN_ID_ALREADY_EXISTS");
        }
        return report(reasons,
                JsonNodeFactory.instance.textNode(runId), skill.get("path"), skill.get("sha256"));
    }

    private static NextRunReport report(Set<String> reasons, JsonNode runId, JsonNode skillPath, JsonNode skillSha256) {
        return new NextRunReport(reasons.isEmpty() ? "READY" : "BLOCKED",
                List.copyOf(reasons), runId, skillPath, skillSha256);
    }

    private static boolean componentRevisionMismatch(JsonNode proposal, JsonNode revision) {
        if (proposal == null || !proposal.isObject()) {
            return false;
        }
        JsonNode results = proposal.get("capability_results");
        if (results == null || !results.isArray()) {
            return false;
        }
        for (JsonNode result : results) {
            if (result == null || !result.isObject()) {
                continue;
            }
            JsonNode components = result.get("components");
            if (components == null || !components.isArray()) {
                continue;
            }
            for (JsonNode component : components) {
                if (component != null && component.isObject()
                        && !valueEquals(component.get("source_revision"), revision)) {
                    return true;
                }
            }
        }
        return false;
    }

    private JsonNode proposalStructure(JsonNode proposal, Set<String> reasons) {
        if (proposal == null || !proposal.isObject()) {
            return null;
        }
        JsonNode revision = proposal.get("source_revision");
        JsonNode resultsValue = proposal.get("capability_results");
        List<JsonNode> results = new ArrayList<>();
        if (resultsValue != null && resultsValue.isArray()) {
            resultsValue.forEach(results::add);
        } else {
            reasons.add("SCHEMA_INVALID");
        }
        for (JsonNode result : results) {
            if (result == null || !result.isObject()) {
                continue;
            }
            JsonNode componentsValue = result.get("components");
            List<JsonNode> components = new ArrayList<>();
            if (componentsValue != null && componentsValue.isArray()) {
                componentsValue.forEach(components::add);
            } else {
                reasons.add("SCHEMA_INVALID");
            }
            if (components.stream().anyMatch(component -> !componentIdentityValid(component))) {
                reasons.add("COMPONENT_IDENTITY_INVALID");
            }
            JsonNode refsValue = result.get("evidence_refs");
            List<JsonNode> refs = new ArrayList<>();
            if (refsValue != null && refsValue.isArray()) {
                refsValue.forEach(refs::add);
            } else {
                reasons.add("SCHEMA_INVALID");
            }
            if (refs.stream().anyMatch(ref -> !evidenceIdentityValid(ref))) {
                reasons.add("COMPONENT_IDENTITY_INVALID");
            }
        }
        return revision;
    }

    private static boolean componentIdentityValid(JsonNode component) {
        if (component == null || !component.isObject()) {
            return false;
        }
        JsonNode granularity = component.get("granularity");
        JsonNode path = component.get("source_path");
        boolean pathValid;
        if (path != null && path.isTextual() && path.asText().equals(".")) {
            pathValid = textEquals(granularity, "REPOSITORY");
        } else {
            pathValid = canonicalRelative(path) != null;
        }
        boolean symbolValid;
        if (granularity != null && granularity.isTextual() && SYMBOL_GRANULARITIES.contains(granularity.asText())) {
            symbolValid = plainNonempty(component.get("qualified_symbol"));
        } else if (granularity != null && granularity.isTextual()
                && NON_SYMBOL_GRANULARITIES.contains(granularity.asText())) {
            symbolValid = component.get("qualified_symbol") != null && component.get("qualified_symbol").isTextual();
        } else {
            symbolValid = false;
        }
        return pathValid && symbolValid
                && plainNonempty(component.get("provider_node_id"))
                && plainNonempty(component.get("selection_reason"));
    }

    private static boolean evidenceIdentityValid(JsonNode ref) {
        return ref != null && ref.isObject()
                && canonicalRelative(ref.get("source_path")) != null
                && plainNonempty(ref.get("provider_node_id"))
                && plainNonempty(ref.get("source_location"));
    }

    // ------------------------------------------------------------------
    // Files, digests, and JSON loading
    // ------------------------------------------------------------------

    private static byte[] readVerified(Path rootAbsolute, Path rootReal, JsonNode item, Set<String> reasons) {
        Path path = resolvedFile(rootAbsolute, rootReal, item, reasons);
        JsonNode expected = item == null ? null : item.get("sha256");
        if (path == null || expected == null || !expected.isTextual()
                || !SHA256.matcher(expected.asText()).matches()) {
            reasons.add("INPUT_DIGEST_MISMATCH");
            return null;
        }
        byte[] data;
        try {
            data = Files.readAllBytes(path);
        } catch (IOException | RuntimeException failure) {
            reasons.add("INPUT_DIGEST_MISMATCH");
            return null;
        }
        if (!sha256Hex(data).equals(expected.asText())) {
            reasons.add("INPUT_DIGEST_MISMATCH");
            return null;
        }
        return data;
    }

    private static Path resolvedFile(Path rootAbsolute, Path rootReal, JsonNode item, Set<String> reasons) {
        String relative = canonicalRelative(item == null ? null : item.get("path"));
        if (relative == null) {
            reasons.add("INPUT_PATH_INVALID");
            return null;
        }
        Path candidate = rootAbsolute.resolve(relative);
        Path resolved;
        try {
            resolved = candidate.toRealPath();
        } catch (IOException | RuntimeException failure) {
            reasons.add("INPUT_FILE_INVALID");
            return null;
        }
        if (!resolved.startsWith(rootReal)) {
            reasons.add("INPUT_FILE_INVALID");
            return null;
        }
        try {
            if (Files.isSymbolicLink(candidate) || !Files.isRegularFile(resolved)) {
                reasons.add("INPUT_FILE_INVALID");
                return null;
            }
        } catch (SecurityException failure) {
            reasons.add("INPUT_FILE_INVALID");
            return null;
        }
        return resolved;
    }

    private static JsonNode loadJsonBytes(byte[] data, Set<String> reasons) {
        if (data == null) {
            return JsonNodeFactory.instance.objectNode();
        }
        try {
            JsonNode value = JSON.readTree(data);
            if (value == null || !value.isObject()) {
                reasons.add("INPUT_JSON_INVALID");
                return JsonNodeFactory.instance.objectNode();
            }
            return value;
        } catch (IOException | RuntimeException failure) {
            reasons.add("INPUT_JSON_INVALID");
            return JsonNodeFactory.instance.objectNode();
        }
    }

    private static JsonSchema loadValidator(Path rootAbsolute, Set<String> reasons) {
        try {
            byte[] schemaBytes = Files.readAllBytes(rootAbsolute.resolve(SCHEMA_PATH));
            JsonNode schema = JSON.readTree(schemaBytes);
            JsonNode id = schema == null ? null : schema.get("$id");
            // Python check_schema accepts a relative $id (a valid URI-reference);
            // networknt requires an absolute base URI. All $refs in this schema are
            // same-document fragments, so normalizing the in-memory $id cannot
            // change validation outcomes. The checked-in schema file is untouched.
            if (schema != null && schema.isObject() && id != null && id.isTextual()
                    && !id.asText().matches("^[A-Za-z][A-Za-z0-9+.-]*:.*")) {
                schema = schema.deepCopy();
                ((ObjectNode) schema).put("$id", "urn:fdi:" + SCHEMA_PATH);
            }
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schema);
        } catch (IOException | RuntimeException failure) {
            reasons.add("SCHEMA_DEFINITION_INVALID");
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Committed run-ID registry over git HEAD
    // ------------------------------------------------------------------

    private static Set<String> committedRunIds(Path root, Set<String> reasons) {
        byte[] listing = gitOutput(root,
                new String[] {"ls-tree", "-r", "-z", "--name-only", "HEAD", "--", "validation/pkb001"},
                "RUN_ID_REGISTRY_UNAVAILABLE", reasons);
        if (listing == null) {
            return Set.of();
        }
        Set<String> found = new HashSet<>();
        for (String relative : new String(listing, java.nio.charset.StandardCharsets.UTF_8).split("\0", -1)) {
            if (relative.isEmpty() || !relative.endsWith(".json")) {
                continue;
            }
            byte[] blob = gitOutput(root, new String[] {"show", "HEAD:" + relative},
                    "RUN_ID_REGISTRY_INVALID", reasons);
            if (blob == null) {
                return Set.of();
            }
            try {
                collectRunIds(JSON.readTree(blob), found);
            } catch (IOException | RuntimeException failure) {
                reasons.add("RUN_ID_REGISTRY_INVALID");
                return Set.of();
            }
        }
        return found;
    }

    private static void collectRunIds(JsonNode value, Set<String> found) {
        if (value != null && value.isObject()) {
            JsonNode runId = value.get("run_id");
            if (plainNonempty(runId)) {
                found.add(runId.asText());
            }
            value.forEach(child -> collectRunIds(child, found));
        } else if (value != null && value.isArray()) {
            value.forEach(child -> collectRunIds(child, found));
        }
    }

    /** Runs one bounded git subprocess; adds {@code failureCode} and returns null on any failure. */
    private static byte[] gitOutput(Path root, String[] arguments, String failureCode, Set<String> reasons) {
        Process process = null;
        Future<byte[]> drain = null;
        var executor = Executors.newSingleThreadExecutor(DAEMON_FACTORY);
        boolean completed = false;
        try {
            String[] command = new String[arguments.length + 1];
            command[0] = "git";
            System.arraycopy(arguments, 0, command, 1, arguments.length);
            process = new ProcessBuilder(command).directory(root.toFile())
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            Process started = process;
            drain = executor.submit(() -> drainBounded(started));
            if (!process.waitFor(GIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS) || process.exitValue() != 0) {
                reasons.add(failureCode);
                return null;
            }
            byte[] output = drain.get(GIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            if (output == null) {
                reasons.add(failureCode);
                return null;
            }
            completed = true;
            return output;
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException failure) {
            if (failure instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            reasons.add(failureCode);
            return null;
        } finally {
            if (!completed && process != null) {
                process.destroy();
                process.destroyForcibly();
                try {
                    process.getInputStream().close();
                } catch (IOException ignored) {
                    // best-effort cleanup
                }
            }
            if (drain != null && !drain.isDone()) {
                drain.cancel(true);
            }
            executor.shutdownNow();
        }
    }

    /** Drains stdout with a hard cap; overflow reports failure like an unreadable registry. */
    private static byte[] drainBounded(Process process) throws IOException {
        InputStream stream = process.getInputStream();
        ByteArrayOutputStream kept = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = stream.read(buffer)) != -1) {
            if (kept.size() + count > MAX_GIT_OUTPUT_BYTES) {
                return null;
            }
            kept.write(buffer, 0, count);
        }
        return kept.toByteArray();
    }

    private static final ThreadFactory DAEMON_FACTORY = runnable -> {
        Thread thread = new Thread(runnable, "next-run-gate-git-drain");
        thread.setDaemon(true);
        return thread;
    };

    // ------------------------------------------------------------------
    // Shared value helpers
    // ------------------------------------------------------------------

    /** Ports the Python {@code _canonical_relative} check, including NUL and all-whitespace rejection. */
    static String canonicalRelative(JsonNode value) {
        if (value == null || !value.isTextual()) {
            return null;
        }
        return canonicalRelative(value.asText());
    }

    public static String canonicalRelative(String text) {
        if (text == null || text.isEmpty() || text.isBlank()
                || text.indexOf('\\') >= 0 || text.indexOf('\0') >= 0
                || text.startsWith("/") || text.matches("^[A-Za-z]:.*")) {
            return null;
        }
        for (String part : text.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) {
                return null;
            }
        }
        return text;
    }

    /** Ports the Python forbidden-path token check; noncanonical paths are never forbidden. */
    static boolean forbiddenPath(JsonNode value) {
        String relative = canonicalRelative(value);
        if (relative == null) {
            return false;
        }
        for (String part : relative.split("/")) {
            List<String> tokens = new ArrayList<>();
            for (String token : part.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
                if (!token.isEmpty()) {
                    tokens.add(token);
                }
            }
            for (String token : tokens) {
                if (FORBIDDEN_TOKENS.contains(token)) {
                    return true;
                }
            }
            for (int index = 0; index + 1 < tokens.size(); index++) {
                if ((tokens.get(index).equals("human") && tokens.get(index + 1).equals("review"))
                        || (tokens.get(index).equals("post") && tokens.get(index + 1).equals("generation"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean plainNonempty(JsonNode value) {
        return value != null && value.isTextual() && !value.asText().isBlank();
    }

    private static boolean textEquals(JsonNode value, String expected) {
        return value != null && value.isTextual() && value.asText().equals(expected);
    }

    /** Python {@code !=} on parsed JSON values, treating Java null and JSON null as Python None. */
    static boolean valueEquals(JsonNode left, JsonNode right) {
        boolean leftNone = left == null || left.isNull();
        boolean rightNone = right == null || right.isNull();
        if (leftNone || rightNone) {
            return leftNone && rightNone;
        }
        if (left.isNumber() && right.isNumber()) {
            return left.decimalValue().compareTo(right.decimalValue()) == 0;
        }
        return left.equals(right);
    }

    private static Path realPathOr(Path path, Path fallback) {
        try {
            return path.toRealPath();
        } catch (IOException | RuntimeException failure) {
            return fallback;
        }
    }

    static String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}

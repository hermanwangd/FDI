package com.featuredeliveryintelligence.fdi.validation.nextrun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Shared fixture builder for the next-run gate characterization and CLI tests.
 * Mirrors the {@code root} and {@code valid_request} pytest fixtures of
 * {@code tests/test_pkb001_next_run_gate.py}: a real temporary git repository
 * with one committed run-ID and the checked-in v0.2 proposal schema.
 */
public final class GateTestRoots {
    public static final String SKILL_PATH = "skills/pkb001/pk-s1-product-realization-v0.2/SKILL.md";
    public static final String REVISION = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    public static final Path REPOSITORY = Path.of("").toAbsolutePath();
    public static final String SCHEMA_RELATIVE =
            "validation/pkb001/schemas/realization-proposal-v0.2.schema.json";
    private static final ObjectMapper JSON = new ObjectMapper();

    private GateTestRoots() { }

    public static Path newGateRoot(Path temp) throws IOException {
        git(temp, "init", "-q");
        git(temp, "config", "user.email", "gate@example.invalid");
        git(temp, "config", "user.name", "Gate Test");
        Path existing = temp.resolve("validation/pkb001/existing.json");
        Files.createDirectories(existing.getParent());
        Files.writeString(existing, "{\"run_id\":\"already-used\"}\n", StandardCharsets.UTF_8);
        git(temp, "add", "validation/pkb001/existing.json");
        git(temp, "commit", "-qm", "fixture");
        Path schemaTarget = temp.resolve(SCHEMA_RELATIVE);
        Files.createDirectories(schemaTarget.getParent());
        Files.copy(REPOSITORY.resolve(SCHEMA_RELATIVE), schemaTarget, StandardCopyOption.REPLACE_EXISTING);
        return temp;
    }

    public static void git(Path directory, String... arguments) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("git");
        for (String argument : arguments) {
            command.add(argument);
        }
        Process process = new ProcessBuilder(command).directory(directory.toFile())
                .redirectErrorStream(true).start();
        String output;
        try {
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("git timed out: " + String.join(" ", command));
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("git interrupted: " + String.join(" ", command), failure);
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException(
                    "git failed (" + process.exitValue() + "): " + String.join(" ", command) + "\n" + output);
        }
    }

    /** Writes JSON (or raw bytes) under the root and returns the {path, sha256} binding item. */
    public static ObjectNode write(Path root, String relative, JsonNode value) throws IOException {
        byte[] data = (JSON.writeValueAsString(value) + "\n").getBytes(StandardCharsets.UTF_8);
        return writeBytes(root, relative, data);
    }

    public static ObjectNode writeBytes(Path root, String relative, byte[] data) throws IOException {
        Path target = root.resolve(relative);
        Files.createDirectories(target.getParent());
        Files.write(target, data);
        ObjectNode item = JSON.createObjectNode();
        item.put("path", relative);
        item.put("sha256", NextRunGate.sha256Hex(data));
        return item;
    }

    /** Ports the {@code valid_request} pytest fixture. */
    public static ObjectNode validRequest(Path root) throws IOException {
        ObjectNode semantics = object();
        semantics.put("status", "FROZEN");
        semantics.put("owner", "PRODUCT_TEAM");
        semantics.put("applicable_source_commit_sha", REVISION);
        ObjectNode semanticsItem = write(root, "inputs/product-semantics.json", semantics);
        semantics = semanticsItem;
        ObjectNode graph = writeBytes(root, "inputs/graph.json", "{\"nodes\":[]}\n".getBytes(StandardCharsets.UTF_8));
        ObjectNode binding = object();
        binding.put("result", "EXACTLY_BOUND");
        binding.set("query_bounds", tree(java.util.Map.of("depth", 3)));
        binding.put("requested_revision", REVISION);
        binding.put("indexed_revision", REVISION);
        binding.put("graph_sha256", graph.get("sha256").asText());
        ObjectNode bindingItem = write(root, "inputs/binding.json", binding);
        binding = bindingItem;
        ObjectNode skill = writeBytes(root, SKILL_PATH, "next-run skill\n".getBytes(StandardCharsets.UTF_8));
        ArrayNode inputs = JSON.createArrayNode();
        inputs.add(withKind(semantics, "PRODUCT_SEMANTICS"));
        inputs.add(withKind(binding, "GRAPHIFY_BINDING_EVIDENCE"));
        inputs.add(withKind(graph, "FROZEN_GRAPH"));
        inputs.add(withKind(skill, "PKS1_SKILL"));

        ObjectNode component = JSON.createObjectNode();
        component.put("role", "PRIMARY");
        component.put("granularity", "TYPE");
        component.put("source_revision", REVISION);
        component.put("source_path", "src/App.java");
        component.put("qualified_symbol", "example.App");
        component.put("provider_node_id", "node-1");
        component.put("selection_reason", "Owns the behavior");
        ObjectNode evidence = JSON.createObjectNode();
        evidence.put("provider_node_id", "node-1");
        evidence.put("source_path", "src/App.java");
        evidence.put("source_location", "type example.App");
        ObjectNode result = JSON.createObjectNode();
        result.put("capability_id", "CAP-1");
        result.put("outcome", "MAPPING_PROPOSAL");
        result.set("components", JSON.createArrayNode().add(component));
        result.set("evidence_refs", JSON.createArrayNode().add(evidence));
        result.put("confidence", 0.8);
        result.set("limitations", JSON.createArrayNode().add("Static evidence only"));
        ObjectNode proposal = JSON.createObjectNode();
        proposal.put("schema_version", "pkb001.realization-proposal.v0.2");
        proposal.put("run_id", "new-run");
        proposal.put("authority", "PROPOSAL_ONLY");
        proposal.put("source_revision", REVISION);
        proposal.put("graph_sha256", graph.get("sha256").asText());
        proposal.set("capability_results", JSON.createArrayNode().add(result));

        ObjectNode request = JSON.createObjectNode();
        request.set("generation_inputs", inputs);
        request.set("proposal", proposal);
        return request;
    }

    public static ObjectNode withKind(ObjectNode item, String kind) {
        item.put("kind", kind);
        return item;
    }

    public static ObjectNode inputByKind(JsonNode request, String kind) {
        for (JsonNode item : request.withArray("generation_inputs")) {
            if (kind.equals(item.path("kind").asText())) {
                return (ObjectNode) item;
            }
        }
        throw new IllegalArgumentException("no input of kind " + kind);
    }

    public static void rewriteInput(Path root, ObjectNode item, JsonNode value) throws IOException {
        byte[] data = (JSON.writeValueAsString(value) + "\n").getBytes(StandardCharsets.UTF_8);
        Files.write(root.resolve(item.get("path").asText()), data);
        item.put("sha256", NextRunGate.sha256Hex(data));
    }

    public static void editJson(Path root, ObjectNode item, String key, JsonNode value) throws IOException {
        JsonNode body = JSON.readTree(Files.readAllBytes(root.resolve(item.get("path").asText())));
        ((ObjectNode) body).set(key, value);
        rewriteInput(root, item, body);
    }

    public static ObjectNode object() {
        return JSON.createObjectNode();
    }

    public static JsonNode tree(Object value) {
        return JSON.valueToTree(value);
    }
}

package com.featuredeliveryintelligence.fdi.validation.nextrun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ports the 80 non-CLI collected characterization cases of
 * {@code tests/test_pkb001_next_run_gate.py} to the Java {@link NextRunGate}.
 * The two CLI subprocess cases are ported in {@code NextRunGateCliTests}.
 * Every rejected case stays rejected; none were weakened for Java parity.
 */
class NextRunGateCharacterizationTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ZEROES = "0".repeat(64);
    private static final String BEES = "b".repeat(40);

    @TempDir Path temp;

    private Path root() throws Exception {
        return GateTestRoots.newGateRoot(temp);
    }

    @Test
    void v02ReadinessIsReady() throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        NextRunReport report = new NextRunGate().validate(root, request);
        JsonNode rendered = JSON.readTree(report.toJsonBytes());
        assertEquals("READY", rendered.get("status").asText());
        assertEquals(0, rendered.get("reasons").size());
        assertEquals(0, rendered.get("mappings").size());
        assertEquals("new-run", rendered.get("run_id").asText());
        assertEquals(GateTestRoots.SKILL_PATH, rendered.get("skill_path").asText());
        assertEquals(GateTestRoots.inputByKind(request, "PKS1_SKILL").get("sha256").asText(),
                rendered.get("skill_sha256").asText());
    }

    @Test
    void blockedReportRenderingMatchesPythonJsonDumps() {
        String expected = "{\n"
                + "  \"mappings\": [],\n"
                + "  \"reasons\": [\n"
                + "    \"REQUEST_INVALID\"\n"
                + "  ],\n"
                + "  \"run_id\": null,\n"
                + "  \"skill_path\": null,\n"
                + "  \"skill_sha256\": null,\n"
                + "  \"status\": \"BLOCKED\"\n"
                + "}\n";
        assertEquals(expected, new String(NextRunReport.blocked().toJsonBytes(), StandardCharsets.UTF_8));
    }

    static Stream<Arguments> mutations() {
        return Stream.of(
                Arguments.of("v1 selection", "SKILL_VERSION_NOT_SELECTED"),
                Arguments.of("skill digest mismatch", "SKILL_DIGEST_MISMATCH"),
                Arguments.of("forbidden input", "FORBIDDEN_GENERATION_INPUT"),
                Arguments.of("component revision mismatch", "COMPONENT_REVISION_MISMATCH"),
                Arguments.of("duplicate run_id", "RUN_ID_ALREADY_EXISTS"),
                Arguments.of("malformed schema", "SCHEMA_INVALID"),
                Arguments.of("empty inputs", "REQUIRED_INPUT_SET_INVALID"),
                Arguments.of("missing required kind", "REQUIRED_INPUT_SET_INVALID"),
                Arguments.of("duplicate required kind", "REQUIRED_INPUT_SET_INVALID"),
                Arguments.of("unknown kind", "GENERATION_INPUT_NOT_ALLOWLISTED"),
                Arguments.of("unfrozen semantics", "PRODUCT_SEMANTICS_NOT_FROZEN"),
                Arguments.of("wrong semantics owner", "PRODUCT_SEMANTICS_OWNER_INVALID"),
                Arguments.of("unbound Graphify evidence", "GRAPHIFY_BINDING_INVALID"),
                Arguments.of("missing query bounds", "GRAPHIFY_QUERY_BOUNDS_MISSING"),
                Arguments.of("applicable revision mismatch", "REVISION_BINDING_MISMATCH"),
                Arguments.of("requested revision mismatch", "REVISION_BINDING_MISMATCH"),
                Arguments.of("indexed revision mismatch", "REVISION_BINDING_MISMATCH"),
                Arguments.of("frozen graph digest mismatch", "FROZEN_GRAPH_DIGEST_MISMATCH"),
                Arguments.of("binding graph digest mismatch", "GRAPH_BINDING_DIGEST_MISMATCH"),
                Arguments.of("missing graph_sha256", "SCHEMA_INVALID"),
                Arguments.of("missing evidence_refs", "SCHEMA_INVALID"),
                Arguments.of("missing confidence", "SCHEMA_INVALID"),
                Arguments.of("missing limitations", "SCHEMA_INVALID"),
                Arguments.of("noncanonical path", "INPUT_PATH_INVALID"),
                Arguments.of("malformed request", "REQUEST_INVALID"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mutations")
    void mutationsAreBlockedWithoutMappings(String mutation, String reason) throws Exception {
        Path root = root();
        JsonNode request = mutate(root, GateTestRoots.validRequest(root), mutation);
        NextRunReport report = new NextRunGate().validate(root, request);
        assertEquals("BLOCKED", report.status());
        assertTrue(report.reasons().contains(reason), report.reasons().toString());
        assertTrue(new String(report.toJsonBytes(), StandardCharsets.UTF_8).contains("\"mappings\": []"));
    }

    /** Ports {@code _mutate} of the Python characterization suite. */
    private static JsonNode mutate(Path root, ObjectNode request, String mutation) throws Exception {
        if (mutation.equals("malformed request")) {
            return JsonNodeFactory.instance.arrayNode();
        }
        ObjectNode mutable = request.deepCopy();
        ArrayNode inputs = mutable.withArray("generation_inputs");
        ObjectNode proposal = (ObjectNode) mutable.get("proposal");
        switch (mutation) {
            case "v1 selection" -> inputByKind(mutable, "PKS1_SKILL")
                    .put("path", "skills/pkb001/pk-s1-product-realization/SKILL.md");
            case "skill digest mismatch" -> inputByKind(mutable, "PKS1_SKILL").put("sha256", ZEROES);
            case "forbidden input" -> inputByKind(mutable, "FROZEN_GRAPH")
                    .put("path", "validation/pkb001/evaluator/gold.json");
            case "component revision mismatch" -> firstComponent(proposal).put("source_revision", BEES);
            case "duplicate run_id" -> proposal.put("run_id", "already-used");
            case "malformed schema" -> proposal.put("authority", "PRODUCT_TRUTH");
            case "empty inputs" -> mutable.set("generation_inputs", JsonNodeFactory.instance.arrayNode());
            case "missing required kind" -> inputs.remove(inputs.size() - 1);
            case "duplicate required kind" -> inputs.add(inputs.get(0).deepCopy());
            case "unknown kind" -> {
                ObjectNode unknown = JsonNodeFactory.instance.objectNode();
                unknown.put("kind", "GOLD");
                unknown.put("path", "x");
                unknown.put("sha256", ZEROES);
                inputs.add(unknown);
            }
            case "unfrozen semantics" -> GateTestRoots.rewriteInput(root, inputByKind(mutable, "PRODUCT_SEMANTICS"),
                    frozenSemantics("DRAFT", "PRODUCT_TEAM"));
            case "wrong semantics owner" -> GateTestRoots.rewriteInput(root, inputByKind(mutable, "PRODUCT_SEMANTICS"),
                    frozenSemantics("FROZEN", "AGENT"));
            case "unbound Graphify evidence" -> GateTestRoots.editJson(root,
                    inputByKind(mutable, "GRAPHIFY_BINDING_EVIDENCE"),
                    "result", JsonNodeFactory.instance.textNode("PARTIAL"));
            case "missing query bounds" -> GateTestRoots.editJson(root,
                    inputByKind(mutable, "GRAPHIFY_BINDING_EVIDENCE"),
                    "query_bounds", JsonNodeFactory.instance.objectNode());
            case "applicable revision mismatch" -> GateTestRoots.editJson(root,
                    inputByKind(mutable, "PRODUCT_SEMANTICS"),
                    "applicable_source_commit_sha", JsonNodeFactory.instance.textNode(BEES));
            case "requested revision mismatch" -> GateTestRoots.editJson(root,
                    inputByKind(mutable, "GRAPHIFY_BINDING_EVIDENCE"),
                    "requested_revision", JsonNodeFactory.instance.textNode(BEES));
            case "indexed revision mismatch" -> GateTestRoots.editJson(root,
                    inputByKind(mutable, "GRAPHIFY_BINDING_EVIDENCE"),
                    "indexed_revision", JsonNodeFactory.instance.textNode(BEES));
            case "frozen graph digest mismatch" -> inputByKind(mutable, "FROZEN_GRAPH").put("sha256", ZEROES);
            case "binding graph digest mismatch" -> GateTestRoots.editJson(root,
                    inputByKind(mutable, "GRAPHIFY_BINDING_EVIDENCE"),
                    "graph_sha256", JsonNodeFactory.instance.textNode(ZEROES));
            case "missing graph_sha256" -> proposal.remove("graph_sha256");
            case "missing evidence_refs" -> firstResult(proposal).remove("evidence_refs");
            case "missing confidence" -> firstResult(proposal).remove("confidence");
            case "missing limitations" -> firstResult(proposal).remove("limitations");
            case "noncanonical path" -> inputByKind(mutable, "FROZEN_GRAPH")
                    .put("path", "inputs/../inputs/graph.json");
            default -> throw new IllegalArgumentException("unknown mutation " + mutation);
        }
        return mutable;
    }

    private static ObjectNode inputByKind(ObjectNode request, String kind) {
        return GateTestRoots.inputByKind(request, kind);
    }

    private static ObjectNode frozenSemantics(String status, String owner) {
        ObjectNode semantics = GateTestRoots.object();
        semantics.put("status", status);
        semantics.put("owner", owner);
        semantics.put("applicable_source_commit_sha", GateTestRoots.REVISION);
        return semantics;
    }

    private static ObjectNode firstResult(ObjectNode proposal) {
        return (ObjectNode) proposal.withArray("capability_results").get(0);
    }

    private static ObjectNode firstComponent(ObjectNode proposal) {
        return (ObjectNode) firstResult(proposal).withArray("components").get(0);
    }

    private static ObjectNode firstEvidence(ObjectNode proposal) {
        return (ObjectNode) firstResult(proposal).withArray("evidence_refs").get(0);
    }

    @Test
    void unresolvedRequiresNoComponents() throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        firstResult((ObjectNode) request.get("proposal")).put("outcome", "UNRESOLVED");
        assertTrue(new NextRunGate().validate(root, request).reasons().contains("SCHEMA_INVALID"));
    }

    @Test
    void forbiddenPathCheckNormalizesCaseAndComponents() throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        GateTestRoots.inputByKind(request, "FROZEN_GRAPH").put("path", "validation/PKB001/HUMAN-REVIEW/input.json");
        assertTrue(new NextRunGate().validate(root, request).reasons().contains("FORBIDDEN_GENERATION_INPUT"));
    }

    @Test
    void forbiddenPathCheckRejectsTaskPrefixedDirectory() throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        GateTestRoots.inputByKind(request, "FROZEN_GRAPH")
                .put("path", "validation/pkb001/task6-blind-review/input.json");
        assertTrue(new NextRunGate().validate(root, request).reasons().contains("FORBIDDEN_GENERATION_INPUT"));
    }

    @Test
    void checkedInSchemaDeclaresDraft202012() throws Exception {
        JsonNode schema = JSON.readTree(Files.readAllBytes(
                GateTestRoots.REPOSITORY.resolve(GateTestRoots.SCHEMA_RELATIVE)));
        assertEquals("https://json-schema.org/draft/2020-12/schema", schema.get("$schema").asText());
        assertEquals("realization-proposal-v0.2.schema.json", schema.get("$id").asText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"kind-array", "results-null", "components-null", "bad-inputs"})
    void jsonCompatibleHostileShapesFailClosed(String shape) throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        switch (shape) {
            case "kind-array" -> GateTestRoots.inputByKind(request, "PRODUCT_SEMANTICS").set("kind",
                    JsonNodeFactory.instance.arrayNode());
            case "results-null" -> ((ObjectNode) request.get("proposal")).set("capability_results",
                    JsonNodeFactory.instance.nullNode());
            case "components-null" -> firstResult((ObjectNode) request.get("proposal")).set("components",
                    JsonNodeFactory.instance.nullNode());
            case "bad-inputs" -> request.set("generation_inputs", JSON.valueToTree(List.of(
                    JsonNodeFactory.instance.nullNode(), JsonNodeFactory.instance.arrayNode(), "bad")));
            default -> throw new IllegalArgumentException(shape);
        }
        NextRunReport report = new NextRunGate().validate(root, request);
        assertEquals("BLOCKED", report.status());
        assertFalse(report.reasons().isEmpty(), report.toString());
        assertTrue(new String(report.toJsonBytes(), StandardCharsets.UTF_8).contains("\"mappings\": []"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "inputs/evaluator-gold.json",
            "inputs/comparison-results.json",
            "inputs/post-generation-results.json",
            "inputs/task7-results.json"})
    void compoundForbiddenPathComponentsAreBlocked(String path) throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        GateTestRoots.inputByKind(request, "FROZEN_GRAPH").put("path", path);
        assertTrue(new NextRunGate().validate(root, request).reasons().contains("FORBIDDEN_GENERATION_INPUT"));
    }

    @Test
    void forbiddenPathLogicDoesNotUseSubstrings() throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        ObjectNode graph = GateTestRoots.inputByKind(request, "FROZEN_GRAPH");
        ObjectNode safe = GateTestRoots.writeBytes(root, "inputs/golden-data.json", "safe\n".getBytes(StandardCharsets.UTF_8));
        graph.set("path", safe.get("path"));
        graph.set("sha256", safe.get("sha256"));
        NextRunReport report = new NextRunGate().validate(root, request);
        assertFalse(report.reasons().contains("FORBIDDEN_GENERATION_INPUT"), report.reasons().toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/src/App.java", "C:/src/App.java", "src\\App.java", "src//App.java",
            "src/./App.java", "src/../App.java", "src/App.java/", "."})
    void componentPathsMatchJavaCanonicalContract(String path) throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        firstComponent((ObjectNode) request.get("proposal")).put("source_path", path);
        assertTrue(new NextRunGate().validate(root, request).reasons().contains("COMPONENT_IDENTITY_INVALID"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/src/App.java", "C:src/App.java", "src\\App.java", "src//App.java", "src/../App.java", "."})
    void evidencePathsAreCanonical(String path) throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        firstEvidence((ObjectNode) request.get("proposal")).put("source_path", path);
        assertTrue(new NextRunGate().validate(root, request).reasons().contains("COMPONENT_IDENTITY_INVALID"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TYPE", "METHOD", "TEMPLATE", "CONFIGURATION"})
    void symbolGranularityRequiresQualifiedSymbol(String granularity) throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        ObjectNode component = firstComponent((ObjectNode) request.get("proposal"));
        component.put("granularity", granularity);
        component.put("qualified_symbol", " ");
        assertTrue(new NextRunGate().validate(root, request).reasons().contains("COMPONENT_IDENTITY_INVALID"));
    }

    @ParameterizedTest
    @MethodSource("nonSymbolGranularityCases")
    void nonSymbolGranularityAcceptsAnyStringSymbol(String granularity, String symbol) throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        ObjectNode component = firstComponent((ObjectNode) request.get("proposal"));
        component.put("granularity", granularity);
        component.put("qualified_symbol", symbol);
        component.put("source_path", "REPOSITORY".equals(granularity) ? "." : "src/App.java");
        assertEquals("READY", new NextRunGate().validate(root, request).status());
    }

    static Stream<Arguments> nonSymbolGranularityCases() {
        return Stream.of(
                Arguments.of("REPOSITORY", ""), Arguments.of("REPOSITORY", " "),
                Arguments.of("REPOSITORY", "example.Repository"),
                Arguments.of("FILE", ""), Arguments.of("FILE", " "), Arguments.of("FILE", "example.App"));
    }

    @Test
    void repositoryDotPathWithEmptySymbolIsValid() throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        ObjectNode component = firstComponent((ObjectNode) request.get("proposal"));
        component.put("granularity", "REPOSITORY");
        component.put("qualified_symbol", "");
        component.put("source_path", ".");
        assertEquals("READY", new NextRunGate().validate(root, request).status());
    }

    @ParameterizedTest
    @ValueSource(strings = {"source_path", "provider_node_id", "selection_reason"})
    void componentIdentityRejectsBlankRequiredStrings(String field) throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        firstComponent((ObjectNode) request.get("proposal")).put(field, " \t");
        assertTrue(new NextRunGate().validate(root, request).reasons().contains("COMPONENT_IDENTITY_INVALID"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"source_path", "provider_node_id"})
    void evidenceIdentityRejectsBlankRequiredStrings(String field) throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        firstEvidence((ObjectNode) request.get("proposal")).put(field, " \t");
        assertTrue(new NextRunGate().validate(root, request).reasons().contains("COMPONENT_IDENTITY_INVALID"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"delete", "mutate"})
    void committedRunIdsAreReadFromHead(String worktreeAction) throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        ((ObjectNode) request.get("proposal")).put("run_id", "already-used");
        Path existing = root.resolve("validation/pkb001/existing.json");
        if (worktreeAction.equals("delete")) {
            Files.delete(existing);
        } else {
            Files.writeString(existing, "{\"run_id\":\"changed-in-worktree\"}\n", StandardCharsets.UTF_8);
        }
        assertTrue(new NextRunGate().validate(root, request).reasons().contains("RUN_ID_ALREADY_EXISTS"));
    }

    @Test
    void malformedCommittedJsonFailsClosed() throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        Path bad = root.resolve("validation/pkb001/bad.json");
        Files.writeString(bad, "{not-json", StandardCharsets.UTF_8);
        GateTestRoots.git(root, "add", "validation/pkb001/bad.json");
        GateTestRoots.git(root, "commit", "-qm", "bad fixture");
        assertTrue(new NextRunGate().validate(root, request).reasons().contains("RUN_ID_REGISTRY_INVALID"));
    }

    @Test
    void malformedCheckedInSchemaFailsClosed() throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        Path schemaPath = root.resolve(GateTestRoots.SCHEMA_RELATIVE);
        ObjectNode schema = (ObjectNode) JSON.readTree(Files.readAllBytes(schemaPath));
        ((ObjectNode) schema.get("properties").get("run_id")).put("pattern", "[");
        Files.write(schemaPath, JSON.writeValueAsBytes(schema));
        NextRunReport report = new NextRunGate().validate(root, request);
        assertTrue(report.reasons().contains("SCHEMA_DEFINITION_INVALID"), report.reasons().toString());
        assertTrue(new String(report.toJsonBytes(), StandardCharsets.UTF_8).contains("\"mappings\": []"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"capability_id", "limitation"})
    void proposalRejectsBlankJavaRequiredText(String field) throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        ObjectNode result = firstResult((ObjectNode) request.get("proposal"));
        if (field.equals("capability_id")) {
            result.put("capability_id", " \t");
        } else {
            result.set("limitations", JSON.valueToTree(List.of(" \t")));
        }
        NextRunReport report = new NextRunGate().validate(root, request);
        assertEquals("BLOCKED", report.status());
        assertTrue(report.reasons().contains("SCHEMA_INVALID"), report.reasons().toString());
        assertTrue(new String(report.toJsonBytes(), StandardCharsets.UTF_8).contains("\"mappings\": []"));
    }

    @Test
    void evidenceSourceLocationRejectsWhitespaceOnly() throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        firstEvidence((ObjectNode) request.get("proposal")).put("source_location", " \t");
        NextRunReport report = new NextRunGate().validate(root, request);
        assertEquals("BLOCKED", report.status());
        assertTrue(report.reasons().contains("SCHEMA_INVALID"), report.reasons().toString());
        assertTrue(new String(report.toJsonBytes(), StandardCharsets.UTF_8).contains("\"mappings\": []"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"request", "proposal", "input", "results"})
    void hostileContainerSubclassesAreRejectedBeforeAccess(String location) throws Exception {
        Path root = root();
        ObjectNode request = GateTestRoots.validRequest(root);
        switch (location) {
            case "request" -> request = hostileObject();
            case "proposal" -> request.set("proposal", hostileObject());
            case "input" -> request.withArray("generation_inputs").set(0, hostileObject());
            case "results" -> ((ObjectNode) request.get("proposal")).set("capability_results", hostileArray());
            default -> throw new IllegalArgumentException(location);
        }
        NextRunReport report = new NextRunGate().validate(root, request);
        assertArrayEquals(NextRunReport.blocked().toJsonBytes(), report.toJsonBytes());
    }

    private static ObjectNode hostileObject() {
        return new ObjectNode(JsonNodeFactory.instance) {
            @Override
            public JsonNode get(String fieldName) {
                throw new RuntimeException("hostile get executed");
            }

            @Override
            public java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields() {
                throw new RuntimeException("hostile items executed");
            }
        };
    }

    private static ArrayNode hostileArray() {
        return new ArrayNode(JsonNodeFactory.instance) {
            @Override
            public java.util.Iterator<JsonNode> elements() {
                throw new RuntimeException("hostile list iteration executed");
            }
        };
    }
}

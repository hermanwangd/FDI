package com.featuredeliveryintelligence.fdi.validation.codebaseline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ports the 6 collected characterization cases of
 * {@code tests/test_pkb001_code_baseline.py} and the supplementary parity
 * probes (arm allowlist, identity binding, graph SHA defaulting, area
 * classification, dedup/sorting, R2 non-src path inclusion, R3 intersection,
 * unknown {@code source_location} fallback, byte-level rendering).
 */
class CodeBaselineCharacterizationTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonNodeFactory NODE = JsonNodeFactory.instance;
    private static final String SHA = "a".repeat(40);
    private static final String GRAPH_SHA = "b".repeat(64);

    private static ObjectNode structure() {
        ObjectNode structure = NODE.objectNode();
        structure.put("source_commit_sha", SHA);
        structure.put("graph_sha256", GRAPH_SHA);
        structure.putArray("nodes")
                .add(NODE.objectNode()
                        .put("id", "product-semantics")
                        .put("label", "ProductSemantics.java")
                        .put("source_file", "src/main/java/example/product/ProductSemantics.java")
                        .put("source_location", "L1"))
                .add(NODE.objectNode()
                        .put("id", "graphify-adapter")
                        .put("label", "GraphifyAdapter.java")
                        .put("source_file", "src/main/java/example/structural/GraphifyAdapter.java")
                        .put("source_location", "L1"));
        structure.putArray("links");
        return structure;
    }

    private static ObjectNode history(String... changedPaths) {
        ObjectNode history = NODE.objectNode();
        history.put("status", "FROZEN");
        history.put("source_commit_sha", SHA);
        history.put("history_cutoff", "2026-01-01T00:00:00Z");
        history.put("post_cutoff_knowledge_policy", "EXCLUDE_AFTER_CUTOFF");
        ObjectNode commit = NODE.objectNode();
        commit.put("commit_sha", "c".repeat(40));
        commit.put("subject", "add product validation");
        var changed = commit.putArray("changed_paths");
        for (String path : changedPaths) {
            changed.add(path);
        }
        history.putArray("commits").add(commit);
        return history;
    }

    private static Map<String, JsonNode> inputs(ObjectNode... values) {
        Map<String, JsonNode> inputs = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            inputs.put(values[index].get("text").asText(), values[index + 1]);
        }
        return inputs;
    }

    private static CodeBaselineResult generate(String arm, Map<String, JsonNode> inputs) {
        return new CodeBaseline().generateArm(arm, inputs);
    }

    @Test
    void forwardMapsFrozenCapabilityOnlyToInBoundaryGraphFiles() {
        ObjectNode semantics = NODE.objectNode();
        semantics.put("status", "FROZEN");
        semantics.put("owner", "PRODUCT_TEAM");
        semantics.put("applicable_source_commit_sha", SHA);
        ObjectNode capability = NODE.objectNode();
        capability.put("capability_id", "CAP-1");
        capability.put("name", "Product ownership");
        capability.put("description", "Human owned meaning");
        capability.putArray("expected_realization_boundary").add("src/main/java/example/product/");
        semantics.putArray("capabilities").add(capability);
        JsonNode result = generate("F1", inputs(text("structure"), structure(),
                text("semantics"), semantics)).result();

        assertEquals("PROPOSAL_ONLY", result.get("authority_status").asText());
        JsonNode proposal = result.get("proposals").get(0);
        assertEquals("CAP-1", proposal.get("target_id").asText());
        assertEquals("src/main/java/example/product/ProductSemantics.java",
                proposal.get("component_refs").get(0).asText());
        assertEquals(1, proposal.get("component_refs").size());
        assertEquals("graph-node:product-semantics@L1", proposal.get("evidence_refs").get(0).asText());
        assertEquals(1, proposal.get("evidence_refs").size());
    }

    @Test
    void reverseGenerationRejectsProductSemantics() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> generate("R1", inputs(text("structure"), structure(),
                        text("semantics"), NODE.objectNode())));
        assertEquals("input categories do not match arm allowlist", failure.getMessage());
    }

    @Test
    void r3RequiresStructuralAndDeliveryEvidenceToConverge() {
        JsonNode result = generate("R3", inputs(text("structure"), structure(),
                text("history"), history("src/main/java/example/product/ProductSemantics.java"))).result();
        assertEquals(1, result.get("proposals").size());
        JsonNode proposal = result.get("proposals").get(0);
        assertEquals("PROPOSAL_ONLY", proposal.get("authority_status").asText());
        assertEquals("git-commit:" + "c".repeat(40), proposal.get("evidence_refs").get(0).asText());
        assertEquals("graph-node:product-semantics@L1", proposal.get("evidence_refs").get(1).asText());
    }

    @Test
    void oneCommitCanSupplyDeliveryEvidenceToMultipleAreas() {
        JsonNode result = generate("R3", inputs(text("structure"), structure(),
                text("history"), history(
                        "src/main/java/example/product/ProductSemantics.java",
                        "src/main/java/example/structural/GraphifyAdapter.java"))).result();
        assertEquals("product", result.get("proposals").get(0).get("target_id").asText());
        assertEquals("structural", result.get("proposals").get(1).get("target_id").asText());
    }

    @Test
    void rootFdiJavaFilesAreGroupedAsApplicationNotFilenames() {
        JsonNode result = generate("R2", inputs(text("history"),
                history("src/main/java/example/fdi/Application.java"))).result();
        assertEquals("application", result.get("proposals").get(0).get("target_id").asText());
    }

    @Test
    void cliEquivalentStructureBindingIsHonored() {
        ObjectNode graph = structure();
        graph.remove("source_commit_sha");
        graph.remove("graph_sha256");
        graph.put("source_commit_sha", SHA);
        graph.put("graph_sha256", GRAPH_SHA);
        ObjectNode semantics = NODE.objectNode();
        semantics.put("status", "FROZEN");
        semantics.put("owner", "PRODUCT_TEAM");
        semantics.put("applicable_source_commit_sha", SHA);
        semantics.putArray("capabilities");
        JsonNode result = generate("F1", inputs(text("structure"), graph,
                text("semantics"), semantics)).result();
        assertEquals(SHA, result.get("source_commit_sha").asText());
        assertEquals(GRAPH_SHA, result.get("graph_artifact_sha256").asText());
        assertEquals(0, result.get("proposals").size());
    }

    // ---------------------------------------------------------------
    // Supplementary characterization probes
    // ---------------------------------------------------------------

    @Test
    void unknownArmIsRejectedLikeTheAllowlist() {
        assertThrows(IllegalArgumentException.class,
                () -> generate("X9", inputs(text("structure"), structure())));
    }

    @Test
    void identityRejectsMismatchedInputRevisions() {
        ObjectNode other = structure();
        other.put("source_commit_sha", "d".repeat(40));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> generate("R3", inputs(text("structure"), structure(),
                        text("history"), other)));
        assertEquals("all inputs must bind the same full source commit SHA", failure.getMessage());
    }

    @Test
    void identityRejectsNonFullShaValues() {
        ObjectNode shortSha = structure();
        shortSha.put("source_commit_sha", "abc");
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> generate("R1", inputs(text("structure"), shortSha)));
        assertEquals("all inputs must bind the same full source commit SHA", failure.getMessage());
    }

    @Test
    void identityAcceptsUppercaseHexApplicableSha() {
        // Python re.fullmatch('[0-9a-f]{40}') is lowercase-only; 'A'*40 must fail.
        ObjectNode semantics = NODE.objectNode();
        semantics.put("status", "FROZEN");
        semantics.put("owner", "PRODUCT_TEAM");
        semantics.put("applicable_source_commit_sha", "A".repeat(40));
        semantics.putArray("capabilities");
        ObjectNode graph = structure();
        graph.put("source_commit_sha", "A".repeat(40));
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> generate("F1", inputs(text("structure"), graph, text("semantics"), semantics)));
        assertEquals("all inputs must bind the same full source commit SHA", failure.getMessage());
    }

    @Test
    void r2WithoutStructureBindsZeroGraphSha() {
        JsonNode result = generate("R2", inputs(text("history"),
                history("src/main/java/example/product/ProductSemantics.java"))).result();
        assertEquals("0".repeat(64), result.get("graph_artifact_sha256").asText());
        assertEquals("pkb001-r2-proposals-v1", result.get("set_id").asText());
        assertEquals("PKB001-R2-510a397", result.get("run_id").asText());
    }

    @Test
    void invalidStructureGraphShaIsRejected() {
        ObjectNode graph = structure();
        graph.put("graph_sha256", "not-a-sha");
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> generate("R1", inputs(text("structure"), graph)));
        assertEquals("structure graph SHA-256 is invalid", failure.getMessage());
    }

    @Test
    void f1RejectsUnfrozenSemanticsAndWrongOwner() {
        ObjectNode unfrozen = NODE.objectNode();
        unfrozen.put("status", "DRAFT");
        unfrozen.put("owner", "PRODUCT_TEAM");
        unfrozen.put("applicable_source_commit_sha", SHA);
        unfrozen.putArray("capabilities");
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> generate("F1", inputs(text("structure"), structure(), text("semantics"), unfrozen)));
        assertEquals("F1 requires frozen Product Team semantics", failure.getMessage());

        ObjectNode wrongOwner = unfrozen.deepCopy();
        wrongOwner.put("status", "FROZEN");
        wrongOwner.put("owner", "AGENTS");
        assertThrows(IllegalArgumentException.class,
                () -> generate("F1", inputs(text("structure"), structure(), text("semantics"), wrongOwner)));
    }

    @Test
    void r2RejectsUnfrozenHistory() {
        ObjectNode history = history("src/main/java/example/product/ProductSemantics.java");
        history.put("status", "DRAFT");
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> generate("R2", inputs(text("history"), history)));
        assertEquals("R2 requires frozen cutoff-bounded history", failure.getMessage());
    }

    @Test
    void r2IncludesNonSrcPathsOfGroupedCommitsInComponentRefs() {
        ObjectNode history = NODE.objectNode();
        history.put("status", "FROZEN");
        history.put("source_commit_sha", SHA);
        history.put("history_cutoff", "2026-01-01T00:00:00Z");
        history.put("post_cutoff_knowledge_policy", "EXCLUDE_AFTER_CUTOFF");
        ObjectNode commit = NODE.objectNode();
        commit.put("commit_sha", "c".repeat(40));
        commit.put("subject", "cross-area change");
        commit.putArray("changed_paths")
                .add("src/main/java/example/product/A.java")
                .add("docs/product/README.md");
        history.putArray("commits").add(commit);
        JsonNode result = generate("R2", inputs(text("history"), history)).result();
        JsonNode proposal = result.get("proposals").get(0);
        assertEquals("docs/product/README.md", proposal.get("component_refs").get(0).asText());
        assertEquals("src/main/java/example/product/A.java",
                proposal.get("component_refs").get(1).asText());
    }

    @Test
    void duplicateNodesAreDeduplicatedAndRefsAreSorted() {
        ObjectNode graph = structure();
        graph.putArray("nodes")
                .add(NODE.objectNode()
                        .put("id", "zz-last")
                        .put("source_file", "src/main/java/example/product/ZZ.java")
                        .put("source_location", "L9"))
                .add(NODE.objectNode()
                        .put("id", "product-semantics")
                        .put("source_file", "src/main/java/example/product/ProductSemantics.java")
                        .put("source_location", "L1"));
        JsonNode result = generate("R1", inputs(text("structure"), graph)).result();
        JsonNode proposal = result.get("proposals").get(0);
        assertEquals(2, proposal.get("component_refs").size());
        assertEquals("src/main/java/example/product/ProductSemantics.java",
                proposal.get("component_refs").get(0).asText());
        assertEquals("graph-node:product-semantics@L1", proposal.get("evidence_refs").get(0).asText());
        assertEquals("graph-node:zz-last@L9", proposal.get("evidence_refs").get(1).asText());
    }

    @Test
    void unknownSourceLocationFallsBackToUnknownAndNullRendersLikePythonNone() {
        ObjectNode graph = structure();
        graph.putArray("nodes")
                .add(NODE.objectNode()
                        .put("id", "no-location")
                        .put("source_file", "src/main/java/example/structural/NoLocation.java"))
                .add(NODE.objectNode()
                        .put("id", "null-location")
                        .put("source_file", "src/main/java/example/structural/NullLocation.java")
                        .putNull("source_location"));
        JsonNode result = generate("R1", inputs(text("structure"), graph)).result();
        JsonNode proposal = result.get("proposals").get(0);
        assertTrue(proposal.get("evidence_refs").toString().contains("graph-node:no-location@UNKNOWN"));
        assertTrue(proposal.get("evidence_refs").toString().contains("graph-node:null-location@None"));
    }

    @Test
    void nodesWithoutIdOrSourceFileAreSkipped() {
        ObjectNode graph = NODE.objectNode();
        graph.put("source_commit_sha", SHA);
        graph.put("graph_sha256", GRAPH_SHA);
        graph.putArray("nodes")
                .add(NODE.objectNode()
                        .put("id", "no-path"))
                .add(NODE.objectNode()
                        .put("source_file", "src/main/java/example/product/NoId.java"))
                .add(NODE.objectNode()
                        .put("id", "")
                        .put("source_file", "src/main/java/example/product/EmptyId.java"))
                .add(NODE.objectNode()
                        .put("id", "valid-node")
                        .put("source_file", "src/main/java/example/product/Valid.java")
                        .put("source_location", "L2"));
        JsonNode result = generate("R1", inputs(text("structure"), graph)).result();
        assertEquals(1, result.get("proposals").size());
        assertEquals("graph-node:valid-node@L2",
                result.get("proposals").get(0).get("evidence_refs").get(0).asText());
    }

    @Test
    void renderingMatchesPythonDumpsByteForByte() throws Exception {
        ObjectNode semantics = NODE.objectNode();
        semantics.put("status", "FROZEN");
        semantics.put("owner", "PRODUCT_TEAM");
        semantics.put("applicable_source_commit_sha", SHA);
        ObjectNode capability = NODE.objectNode();
        capability.put("capability_id", "CAP-1");
        capability.put("name", "Product ownership");
        capability.putArray("expected_realization_boundary").add("src/main/java/example/product/");
        semantics.putArray("capabilities").add(capability);
        CodeBaselineResult result = generate("F1", inputs(text("structure"), structure(),
                text("semantics"), semantics));
        String expected = "{\n"
                + "  \"set_id\": \"pkb001-f1-proposals-v1\",\n"
                + "  \"run_id\": \"PKB001-F1-510a397\",\n"
                + "  \"arm\": \"F1\",\n"
                + "  \"source_commit_sha\": \"" + SHA + "\",\n"
                + "  \"graph_artifact_sha256\": \"" + GRAPH_SHA + "\",\n"
                + "  \"authority_status\": \"PROPOSAL_ONLY\",\n"
                + "  \"proposals\": [\n"
                + "    {\n"
                + "      \"proposal_id\": \"F1-CAP-1\",\n"
                + "      \"arm\": \"F1\",\n"
                + "      \"target_id\": \"CAP-1\",\n"
                + "      \"relation_type\": \"REALIZES\",\n"
                + "      \"operation\": \"CREATE\",\n"
                + "      \"label\": \"Product ownership\",\n"
                + "      \"component_refs\": [\n"
                + "        \"src/main/java/example/product/ProductSemantics.java\"\n"
                + "      ],\n"
                + "      \"evidence_refs\": [\n"
                + "        \"graph-node:product-semantics@L1\"\n"
                + "      ],\n"
                + "      \"confidence\": 0.9,\n"
                + "      \"limitations\": [],\n"
                + "      \"authority_status\": \"PROPOSAL_ONLY\"\n"
                + "    }\n"
                + "  ]\n"
                + "}\n";
        assertEquals(expected, new String(result.toJsonBytes(), StandardCharsets.UTF_8));
        // Sanity check against real Python json.dumps semantics for the same document.
        JsonNode reparsed = JSON.readTree(result.toJsonBytes());
        assertEquals(JSON.readTree(expected), reparsed);
    }

    @Test
    void unmatchedCapabilityRendersZeroConfidenceAndLimitation() {
        ObjectNode semantics = NODE.objectNode();
        semantics.put("status", "FROZEN");
        semantics.put("owner", "PRODUCT_TEAM");
        semantics.put("applicable_source_commit_sha", SHA);
        ObjectNode capability = NODE.objectNode();
        capability.put("capability_id", "CAP-9");
        capability.put("name", "Outside boundary");
        capability.putArray("expected_realization_boundary").add("src/main/java/example/nothing/");
        semantics.putArray("capabilities").add(capability);
        JsonNode proposal = generate("F1", inputs(text("structure"), structure(),
                text("semantics"), semantics)).result().get("proposals").get(0);
        assertEquals(0.0, proposal.get("confidence").asDouble());
        assertEquals("No graph node was found inside the declared boundary.",
                proposal.get("limitations").get(0).asText());
        assertEquals("[]", proposal.get("component_refs").toString());
    }

    private static ObjectNode text(String value) {
        return NODE.objectNode().put("text", value);
    }
}

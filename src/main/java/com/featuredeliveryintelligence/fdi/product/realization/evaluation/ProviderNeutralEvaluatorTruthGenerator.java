package com.featuredeliveryintelligence.fdi.product.realization.evaluation;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

import java.nio.file.*;
import java.util.*;

/** Deterministically migrates only the sealed legacy evaluator truth and its frozen graph. */
public final class ProviderNeutralEvaluatorTruthGenerator {
    public static final String GENERATOR_ID = "com.featuredeliveryintelligence.fdi.product.realization.evaluation.ProviderNeutralEvaluatorTruthGenerator:v1";
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private ProviderNeutralEvaluatorTruthGenerator() { }

    public static void main(String[] args) {
        if (args.length != 2) throw new IllegalArgumentException("usage: <input-root> <output-root>");
        generate(Path.of(args[0]), Path.of(args[1]));
    }

    public static void generate(Path root, Path outputRoot) {
        try {
            verifyInputs(root);
            JsonNode legacy = JSON.readTree(root.resolve(ProviderNeutralEvaluatorTruth.LEGACY_GOLD_PATH).toFile());
            JsonNode graph = JSON.readTree(root.resolve(ProviderNeutralEvaluatorTruth.GRAPH_PATH).toFile());
            Map<String,JsonNode> nodes = new HashMap<>();
            graph.path("nodes").forEach(node -> nodes.put(node.path("id").asText(), node));

            ObjectNode gold = JSON.createObjectNode();
            gold.put("mapping_set_id", "pkb001-petclinic-818c413-evaluator-v2");
            gold.put("status", "EVALUATOR_ONLY_FROZEN");
            gold.put("authority", "EVALUATOR_TRUTH_ONLY");
            gold.put("source_commit_sha", ProviderNeutralEvaluatorTruth.SOURCE_REVISION);
            gold.put("graph_path", ProviderNeutralEvaluatorTruth.GRAPH_PATH);
            gold.put("graph_sha256", ProviderNeutralEvaluatorTruth.GRAPH_SHA256);
            gold.put("legacy_gold_path", ProviderNeutralEvaluatorTruth.LEGACY_GOLD_PATH);
            gold.put("legacy_gold_sha256", ProviderNeutralEvaluatorTruth.LEGACY_GOLD_SHA256);
            ArrayNode mappings = gold.putArray("mappings");
            for (JsonNode legacyMapping : legacy.path("mappings")) {
                ObjectNode mapping = legacyMapping.deepCopy();
                for (JsonNode raw : mapping.path("expected_components")) {
                    ObjectNode component = (ObjectNode) raw;
                    JsonNode node = nodes.get(component.path("graph_node_id").asText());
                    require(node != null, "legacy component missing from frozen graph");
                    require(component.path("source_path").asText().equals(node.path("source_file").asText()), "legacy source path does not match graph");
                    require(component.path("source_location").asText().equals(node.path("source_location").asText()), "legacy source location does not match graph");
                    component.set("identity", identity(node));
                }
                mappings.add(mapping);
            }
            byte[] goldBytes = JSON.writeValueAsBytes(gold);
            String goldSha = com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader.sha256(goldBytes);

            ObjectNode seal = JSON.createObjectNode();
            seal.put("seal_id", "pkb001-petclinic-818c413-ground-truth-v2");
            seal.put("mapping_set_id", "pkb001-petclinic-818c413-evaluator-v2");
            seal.put("status", "SEALED");
            seal.put("gold_path", ProviderNeutralEvaluatorTruth.GOLD_PATH);
            seal.put("gold_sha256", goldSha);
            seal.put("legacy_gold_path", ProviderNeutralEvaluatorTruth.LEGACY_GOLD_PATH);
            seal.put("legacy_gold_sha256", ProviderNeutralEvaluatorTruth.LEGACY_GOLD_SHA256);
            seal.put("legacy_seal_path", ProviderNeutralEvaluatorTruth.LEGACY_SEAL_PATH);
            seal.put("legacy_seal_sha256", ProviderNeutralEvaluatorTruth.LEGACY_SEAL_SHA256);
            seal.put("graph_path", ProviderNeutralEvaluatorTruth.GRAPH_PATH);
            seal.put("graph_sha256", ProviderNeutralEvaluatorTruth.GRAPH_SHA256);
            seal.put("source_commit_sha", ProviderNeutralEvaluatorTruth.SOURCE_REVISION);
            seal.put("generator_identity", GENERATOR_ID);
            ObjectNode isolation = seal.putObject("isolation_controls");
            isolation.put("generation_access", "DENIED");
            isolation.put("product_knowledge_input", false);
            isolation.put("generation_artifact_input", false);

            write(outputRoot.resolve(ProviderNeutralEvaluatorTruth.GOLD_PATH), goldBytes);
            write(outputRoot.resolve(ProviderNeutralEvaluatorTruth.SEAL_PATH), JSON.writeValueAsBytes(seal));
        } catch (RuntimeContractException e) { throw e; }
        catch (Exception e) { throw new RuntimeContractException("cannot generate provider-neutral evaluator truth", e); }
    }

    private static void verifyInputs(Path root) throws Exception {
        require(ProviderNeutralEvaluatorTruth.LEGACY_GOLD_SHA256.equals(ProviderNeutralEvaluatorTruth.sha(root.resolve(ProviderNeutralEvaluatorTruth.LEGACY_GOLD_PATH))), "legacy gold digest mismatch");
        require(ProviderNeutralEvaluatorTruth.LEGACY_SEAL_SHA256.equals(ProviderNeutralEvaluatorTruth.sha(root.resolve(ProviderNeutralEvaluatorTruth.LEGACY_SEAL_PATH))), "legacy seal digest mismatch");
        require(ProviderNeutralEvaluatorTruth.GRAPH_SHA256.equals(ProviderNeutralEvaluatorTruth.sha(root.resolve(ProviderNeutralEvaluatorTruth.GRAPH_PATH))), "graph digest mismatch");
        JsonNode seal = JSON.readTree(root.resolve(ProviderNeutralEvaluatorTruth.LEGACY_SEAL_PATH).toFile());
        require("SEALED".equals(seal.path("status").asText()), "legacy seal status mismatch");
        require(ProviderNeutralEvaluatorTruth.SOURCE_REVISION.equals(seal.path("source_commit_sha").asText()), "legacy source mismatch");
        require("DENIED".equals(seal.path("isolation_controls").path("generation_access").asText()), "legacy generation isolation mismatch");
    }

    private static ObjectNode identity(JsonNode node) {
        String path = node.path("source_file").asText();
        String pkg = path.substring("src/main/java/".length(), path.lastIndexOf('/')).replace('/', '.');
        String owner = path.substring(path.lastIndexOf('/') + 1).replaceFirst("\\.java$", "");
        String label = node.path("label").asText();
        ObjectNode identity = JSON.createObjectNode().put("canonicalRevision", ProviderNeutralEvaluatorTruth.SOURCE_REVISION).put("sourcePath", path);
        if (label.startsWith(".")) {
            identity.put("granularity", "METHOD").put("qualifiedSymbol", pkg + "." + owner + "#" + label.substring(1).replaceFirst("\\(.*$", ""));
        } else identity.put("granularity", "TYPE").put("qualifiedSymbol", pkg + "." + label);
        return identity;
    }
    private static void write(Path path, byte[] bytes) throws Exception { Files.createDirectories(path.getParent()); Files.write(path, bytes); }
    private static void require(boolean condition, String message) { if (!condition) throw new RuntimeContractException(message); }
}

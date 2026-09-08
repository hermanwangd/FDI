package com.featuredeliveryintelligence.fdi.product.realization.evaluation;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader;
import com.fasterxml.jackson.databind.*;

import java.nio.file.*;
import java.util.*;

/** Immutable, sealed evaluator-only truth with provider-neutral formal identities. */
public record ProviderNeutralEvaluatorTruth(String goldSha256, List<Mapping> mappings) {
    public static final String SOURCE_REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    public static final String GRAPH_PATH = "validation/pkb001/artifacts/petclinic-graph-818c413.json";
    public static final String LEGACY_GOLD_PATH = "validation/pkb001/evaluator/petclinic-818c413/gold-mappings.json";
    public static final String LEGACY_SEAL_PATH = "validation/pkb001/evaluator/petclinic-818c413/ground-truth-seal.json";
    public static final String GOLD_PATH = "validation/pkb001/evaluator/petclinic-818c413/gold-mappings-v2.json";
    public static final String SEAL_PATH = "validation/pkb001/evaluator/petclinic-818c413/ground-truth-seal-v2.json";
    public static final String GRAPH_SHA256 = "e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e";
    public static final String LEGACY_GOLD_SHA256 = "4d22799e4d7597e0bbc302c9db3cd0510f70cc946cb5de5909ded9c4b1b112d1";
    public static final String LEGACY_SEAL_SHA256 = "7290fd4aec80cbdd5cea52b30f9da5323e455843948746fc53208eecf6e2a55a";
    public static final String SEAL_SHA256 = "2b343bc780b1f7a785bd5595ff932cb4dd447a859d3db955958b0215e7342ce0";
    private static final ObjectMapper JSON = new ObjectMapper();

    public ProviderNeutralEvaluatorTruth {
        mappings = List.copyOf(mappings);
    }

    public List<ExpectedComponent> expectedComponents() {
        return mappings.stream().flatMap(mapping -> mapping.expectedComponents().stream()).toList();
    }

    public static ProviderNeutralEvaluatorTruth load(Path root) {
        return loadWithSealSha(root, SEAL_SHA256);
    }

    static ProviderNeutralEvaluatorTruth loadWithSealSha(Path root, String expectedSealSha) {
        try {
            requireSha(root, GRAPH_PATH, GRAPH_SHA256, "graph");
            requireSha(root, LEGACY_GOLD_PATH, LEGACY_GOLD_SHA256, "legacy gold");
            requireSha(root, LEGACY_SEAL_PATH, LEGACY_SEAL_SHA256, "legacy seal");
            requireSha(root, SEAL_PATH, expectedSealSha, "v2 seal");
            JsonNode seal = JSON.readTree(root.resolve(SEAL_PATH).toFile());
            require("SEALED".equals(text(seal, "status")), "v2 seal status mismatch");
            require(GOLD_PATH.equals(text(seal, "gold_path")), "v2 gold path mismatch");
            require(LEGACY_GOLD_PATH.equals(text(seal, "legacy_gold_path")), "legacy gold path mismatch");
            require(LEGACY_SEAL_PATH.equals(text(seal, "legacy_seal_path")), "legacy seal path mismatch");
            require(GRAPH_PATH.equals(text(seal, "graph_path")), "graph path mismatch");
            require(SOURCE_REVISION.equals(text(seal, "source_commit_sha")), "source revision mismatch");
            require(GRAPH_SHA256.equals(text(seal, "graph_sha256")), "graph digest mismatch");
            require(LEGACY_GOLD_SHA256.equals(text(seal, "legacy_gold_sha256")), "legacy gold digest mismatch");
            require(LEGACY_SEAL_SHA256.equals(text(seal, "legacy_seal_sha256")), "legacy seal digest mismatch");
            require(ProviderNeutralEvaluatorTruthGenerator.GENERATOR_ID.equals(text(seal, "generator_identity")), "generator identity mismatch");
            require("DENIED".equals(seal.path("isolation_controls").path("generation_access").asText()), "generation isolation mismatch");
            String goldSha = sha(root.resolve(GOLD_PATH));
            require(goldSha.equals(text(seal, "gold_sha256")), "v2 gold digest mismatch");

            JsonNode gold = JSON.readTree(root.resolve(GOLD_PATH).toFile());
            require("EVALUATOR_ONLY_FROZEN".equals(text(gold, "status")), "v2 gold status mismatch");
            require(SOURCE_REVISION.equals(text(gold, "source_commit_sha")), "v2 source revision mismatch");
            require(GRAPH_SHA256.equals(text(gold, "graph_sha256")), "v2 graph digest mismatch");
            List<Mapping> mappings = new ArrayList<>();
            for (JsonNode mappingNode : gold.path("mappings")) {
                List<ExpectedComponent> components = new ArrayList<>();
                for (JsonNode component : mappingNode.path("expected_components")) {
                    JsonNode identity = component.path("identity");
                    String diagnosticPath = text(component, "source_path");
                    Identity normalized = new Identity(text(identity, "canonicalRevision"), text(identity, "sourcePath"),
                            text(identity, "granularity"), text(identity, "qualifiedSymbol"));
                    require(diagnosticPath.equals(normalized.sourcePath()), "diagnostic source path does not match provider-neutral identity");
                    components.add(new ExpectedComponent(text(component, "component_ref"), text(component, "graph_node_id"),
                            diagnosticPath, text(component, "source_location"), normalized));
                }
                mappings.add(new Mapping(text(mappingNode, "capability_id"), List.copyOf(components)));
            }
            var truth = new ProviderNeutralEvaluatorTruth(goldSha, mappings);
            require(truth.expectedComponents().size() == 24, "expected evaluator denominator mismatch");
            return truth;
        } catch (RuntimeContractException e) { throw e; }
        catch (Exception e) { throw new RuntimeContractException("cannot validate provider-neutral evaluator truth", e); }
    }

    private static void requireSha(Path root, String path, String expected, String label) throws Exception {
        require(expected.equals(sha(root.resolve(path))), label + " digest mismatch");
    }
    static String sha(Path path) throws Exception { return ScenarioForwardRequestReader.sha256(Files.readAllBytes(path)); }
    private static String text(JsonNode node, String field) { String value = node.path(field).asText(); require(!value.isBlank(), field + " is required"); return value; }
    private static void require(boolean condition, String message) { if (!condition) throw new RuntimeContractException(message); }

    public record Mapping(String capabilityId, List<ExpectedComponent> expectedComponents) {
        public Mapping { require(capabilityId != null && !capabilityId.isBlank(), "capability identity required"); expectedComponents = List.copyOf(expectedComponents); }
    }
    public record ExpectedComponent(String componentRef, String graphNodeId, String sourcePath, String sourceLocation, Identity identity) { }
    public record Identity(String canonicalRevision, String sourcePath, String granularity, String qualifiedSymbol) {
        public Identity {
            require(canonicalRevision != null && canonicalRevision.matches("[0-9a-f]{40}"), "canonical revision must be full lowercase Git SHA");
            require(canonicalPath(sourcePath), "source path must be canonical repository-relative production path");
            require(Set.of("TYPE", "METHOD").contains(granularity), "unsupported evaluator granularity");
            require(qualifiedSymbol != null && !qualifiedSymbol.isBlank(), "qualified symbol required");
        }
        private static boolean canonicalPath(String path) {
            if (path == null || !path.equals(path.strip()) || path.startsWith("/") || path.startsWith("./") || path.contains("\\") || path.contains("//")) return false;
            if (!path.startsWith("src/main/") || path.toLowerCase(Locale.ROOT).contains("/test/")) return false;
            return Arrays.stream(path.split("/", -1)).noneMatch(part -> part.isEmpty() || part.equals(".") || part.equals(".."));
        }
    }
}

package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.*;
import java.util.*;

/** Evaluator-only, fail-closed comparison of the immutable Slice F proposal. */
public final class SliceGEvaluatorComparison {
    static final String PROPOSAL_PATH = "validation/pkb001/scenario-forward/slice-f-scenario-mapping-proposal-001.json";
    static final String PROPOSAL_EVIDENCE_PATH = "validation/pkb001/scenario-forward/slice-f-scenario-mapping-evidence-001.json";
    static final String DIRECT_EVIDENCE_PATH = "validation/pkb001/scenario-forward/slice-d-direct-test-trace-evidence.json";
    static final String EXPANSION_EVIDENCE_PATH = "validation/pkb001/scenario-forward/slice-e-graphify-expansion-evidence.json";
    static final String GOLD_PATH = "validation/pkb001/evaluator/petclinic-818c413/gold-mappings.json";
    static final String GOLD_SEAL_PATH = "validation/pkb001/evaluator/petclinic-818c413/ground-truth-seal.json";
    private static final String SOURCE = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static final Map<String,String> PRE_EVALUATOR = new LinkedHashMap<>();
    static {
        PRE_EVALUATOR.put(PROPOSAL_PATH, "8c940fb94d3c86c80ff2c9555eb5d77b130f6d0d944593424fb7f2ae563535f2");
        PRE_EVALUATOR.put(PROPOSAL_EVIDENCE_PATH, "91daf7e121c8f0834d5c2353f6ce9e884a4c13d7615ceea26aa97ab306ded81b");
        PRE_EVALUATOR.put(DIRECT_EVIDENCE_PATH, "61f0c923d9fc8c5597a0d57d1106d6a055c51b9f8b06c597f47d9d7748f8d667");
        PRE_EVALUATOR.put(EXPANSION_EVIDENCE_PATH, "ac9f8ad48b2f4d919aa88f99827cd087aa506109b204b1bd2e22056c84520ed1");
        PRE_EVALUATOR.put("validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/accepted-semantics-004.json", "6c854c3d42c348d56720741b573ec88e5d6bd2dc38abb4753540ca23e8aaa9e3");
        PRE_EVALUATOR.put("validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/acceptance-manifest-004.json", "1b3fbbfd210c2c0d82d74a2579c1980d5de6b047fff56a08cc2b43097c76e2a9");
        PRE_EVALUATOR.put("validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/review-decisions-004.json", "98c46df1ffdf6ce57d37d6d320b644ac25778797b1ad786831f76d94e0d34f4f");
        PRE_EVALUATOR.put("validation/pkb001/scenario-review/pkb001-scenarios-petclinic-818c413-20260905-01/proposal-revision-002.json", "7f3e1cc546bf56c9e1f8883825bb27271561e3147b28389d3e4bfdd3d86f5be5");
        PRE_EVALUATOR.put("validation/pkb001/runtime/graphify-petclinic-live-evidence.json", "fd3b6729e720e33c89c87cb987748b17ee6cc4ac1fad2c09ddbf093ab39cd5f8");
        PRE_EVALUATOR.put("validation/pkb001/artifacts/petclinic-graph-818c413.json", "e1f6b1933c9529623b0ddd8b2d051349bf79b3f9baebe89c89c391c856bf629e");
        PRE_EVALUATOR.put("validation/pkb001/schemas/realization-proposal-v0.3.schema.json", "c2e9be62fb6be4d98d3f492abf6bdbdbe80e0fe1a4ea1c9c8afb9339bfbf13c7");
        PRE_EVALUATOR.put("skills/pkb001/pk-s1-product-realization-v0.3/SKILL.md", "e03fc502e5cfcb35f70a35102bd9094cc7aa4021171a74ff2c95e411f9982530");
        PRE_EVALUATOR.put("validation/pkb001/reverse-pkb-bl009-petclinic-001/java-test-behavior/evidence.json", "4db3b4fcac22c704f321e2a3bef20091741147984815addf3ebf77d88643e66d");
    }

    private SliceGEvaluatorComparison() { }

    @FunctionalInterface public interface EvaluatorAccess { EvaluatorTruth load(Path root); }

    public static Report compare(Path root, EvaluatorAccess evaluatorAccess) {
        try {
            // This phase must finish before the callback can expose any evaluator path or bytes.
            Map<String,String> sealed = sealGenerationInputs(root);
            JsonNode proposal = JSON.readTree(root.resolve(PROPOSAL_PATH).toFile());
            validateProposalBindings(proposal);

            EvaluatorTruth truth = Objects.requireNonNull(evaluatorAccess.load(root), "evaluator truth");
            return compute(proposal, truth, sealed.get(PROPOSAL_PATH));
        } catch (RuntimeContractException e) { throw e; }
        catch (Exception e) { throw new RuntimeContractException("Slice G comparison failed", e); }
    }

    static Report compute(JsonNode proposal, EvaluatorTruth truth, String proposalSha256) {
            int unresolved = 0, mappings = 0, direct = 0, inferred = 0;
            Set<String> scenarioIds = new TreeSet<>(), coveredScenarioIds = new TreeSet<>();
            Set<Identity> proposed = new TreeSet<>(), directSymbols = new TreeSet<>(), chainComponents = new TreeSet<>();
            for (JsonNode capability : proposal.path("capabilities")) for (JsonNode scenario : capability.path("scenarios")) {
                JsonNode mapping = scenario.path("mapping");
                String scenarioId = mapping.path("scenarioId").asText();
                if (scenarioId.isBlank() || !scenarioIds.add(scenarioId)) throw fail("duplicate or missing scenario identity");
                String outcome = mapping.path("outcome").asText();
                if ("UNRESOLVED".equals(outcome)) unresolved++;
                else if ("MAPPING_PROPOSAL".equals(outcome)) mappings++;
                else throw fail("unknown mapping outcome");
                for (JsonNode symbol : mapping.path("directProductionSymbols")) {
                    directSymbols.add(identity(symbol.path("productionSymbol")));
                }
                for (JsonNode step : mapping.path("realizationChain")) {
                    String basis = step.path("relationshipBasis").asText();
                    if ("GRAPHIFY_INFERRED".equals(basis)) inferred++;
                    else if ("DIRECT_TEST_REFERENCE".equals(basis)) direct++;
                    else throw fail("unknown realization chain relationshipBasis");
                    chainComponents.add(identity(step.path("component")));
                }
                if (!mapping.path("realizationChain").isEmpty()) coveredScenarioIds.add(scenarioId);
                for (JsonNode role : scenario.path("componentRoles")) {
                    String roleName = role.path("role").asText();
                    if ("PRIMARY".equals(roleName)) proposed.add(identity(role.path("component")));
                    else if (!"SUPPORTING".equals(roleName)) throw fail("unknown component role");
                }
            }
            List<Identity> expected = truth.expectedComponents();
            Metric exact = metric(matches(proposed, expected), expected.size(), proposed.size());
            Metric directRecall = metric(matches(directSymbols, expected), expected.size(), directSymbols.size());
            Metric expanded = metric(matches(chainComponents, expected), expected.size(), chainComponents.size());
            return new Report("pkb001.forward.slice-g-comparison.v1", "EVALUATOR_ONLY", false,
                    SOURCE, proposalSha256, truth.goldSha256(),
                    new Counts(scenarioIds.size(), mappings, unresolved, proposed.size()),
                    directRecall, expanded, exact, metric(coveredScenarioIds.size(), scenarioIds.size(), scenarioIds.size()),
                    new UnresolvedReferences(891, 1035, 891.0 / 1035.0), new TraceCounts(direct, inferred),
                    new PreviousBaseline(17, 24, 17.0 / 24.0, 0, 24),
                    new ProviderDiagnostics(0, "Provider-native node identifiers receive no formal component credit unless selected in the sealed proposal"),
                    List.of("Scenario-bound Graphify expansion is absent for this run",
                            "This is reconstruction consistency and evaluator reviewers may have repository exposure",
                            "Build targets Java 17; the local verification JDK may be newer",
                            "No threshold was selected from observed results; Product truth and publication remain false"));
    }

    private static Map<String,String> sealGenerationInputs(Path root) throws Exception {
        Map<String,String> sealed = new LinkedHashMap<>();
        for (var entry : PRE_EVALUATOR.entrySet()) {
            String actual = sha(root.resolve(entry.getKey()));
            if (!entry.getValue().equals(actual)) throw fail("pre-evaluator exact input digest mismatch: " + entry.getKey());
            sealed.put(entry.getKey(), actual);
        }
        return Collections.unmodifiableMap(sealed);
    }

    private static void validateProposalBindings(JsonNode proposal) {
        if (!SOURCE.equals(proposal.path("sourceRevision").asText())) throw fail("proposal source revision mismatch");
        if (proposal.path("semanticPublicationAllowed").asBoolean(true)) throw fail("proposal publication boundary violated");
        if (proposal.path("capabilities").size() != 5) throw fail("proposal capability count mismatch");
    }

    public static EvaluatorTruth loadEvaluatorTruth(Path root) {
        try {
            if (!"7290fd4aec80cbdd5cea52b30f9da5323e455843948746fc53208eecf6e2a55a".equals(sha(root.resolve(GOLD_SEAL_PATH))))
                throw fail("evaluator seal digest mismatch");
            JsonNode seal = JSON.readTree(root.resolve(GOLD_SEAL_PATH).toFile());
            if (!"SEALED".equals(seal.path("status").asText()) || !GOLD_PATH.equals(seal.path("gold_path").asText()))
                throw fail("evaluator seal invariant mismatch");
            if (!SOURCE.equals(seal.path("source_commit_sha").asText())) throw fail("evaluator seal source binding mismatch");
            String goldSha = sha(root.resolve(GOLD_PATH));
            if (!goldSha.equals(seal.path("gold_sha256").asText())) throw fail("evaluator gold digest mismatch");
            JsonNode gold = JSON.readTree(root.resolve(GOLD_PATH).toFile());
            if (!"EVALUATOR_ONLY_FROZEN".equals(gold.path("status").asText())) throw fail("evaluator gold status mismatch");
            if (!SOURCE.equals(gold.path("source_commit_sha").asText())) throw fail("evaluator source binding mismatch");
            if (!seal.path("graph_sha256").asText().equals(gold.path("graph_sha256").asText())) throw fail("evaluator graph binding mismatch");
            JsonNode graph = JSON.readTree(root.resolve("validation/pkb001/artifacts/petclinic-graph-818c413.json").toFile());
            Map<String,JsonNode> graphNodes = new HashMap<>();
            graph.path("nodes").forEach(node -> graphNodes.put(node.path("id").asText(), node));
            List<Identity> identities = new ArrayList<>();
            for (JsonNode mapping : gold.path("mappings")) for (JsonNode component : mapping.path("expected_components")) {
                JsonNode node = graphNodes.get(component.path("graph_node_id").asText());
                if (node == null) throw fail("evaluator component missing from bound graph");
                if (!component.path("source_path").asText().equals(node.path("source_file").asText()))
                    throw fail("evaluator component path does not match graph");
                identities.add(graphIdentity(node));
            }
            if (identities.size() != 24) throw fail("expected evaluator denominator mismatch");
            return new EvaluatorTruth(goldSha, List.copyOf(identities));
        } catch (RuntimeContractException e) { throw e; }
        catch (Exception e) { throw new RuntimeContractException("cannot validate evaluator truth", e); }
    }

    private static Identity identity(JsonNode node) {
        String path = node.path("sourcePath").asText();
        String symbol = node.path("qualifiedSymbol").asText();
        String granularity = node.path("granularity").asText();
        if (path.isBlank() || symbol.isBlank() || granularity.isBlank()) throw fail("provider-neutral component identity is incomplete");
        return new Identity(path, granularity, symbol);
    }
    static Identity graphIdentity(JsonNode node) {
        String path = node.path("source_file").asText();
        String file = path.substring(path.lastIndexOf('/') + 1).replaceFirst("\\.java$", "");
        String pkg = path.substring("src/main/java/".length(), path.lastIndexOf('/')).replace('/', '.');
        String label = node.path("label").asText();
        if (label.startsWith(".")) {
            String method = label.substring(1).replaceFirst("\\(.*$", "");
            return new Identity(path, "METHOD", pkg + "." + file + "#" + method);
        }
        return new Identity(path, "TYPE", pkg + "." + label);
    }
    private static int matches(Set<Identity> proposed, List<Identity> expected) {
        int matched = 0;
        for (Identity value : expected) if (proposed.contains(value)) matched++;
        return matched;
    }
    private static Metric metric(int matched, int expected, int proposed) {
        return new Metric(matched, expected, proposed, expected == 0 ? null : (double) matched / expected,
                proposed == 0 ? null : (double) matched / proposed, expected != 0, proposed != 0);
    }
    private static String sha(Path path) throws Exception { return ScenarioForwardRequestReader.sha256(Files.readAllBytes(path)); }
    private static RuntimeContractException fail(String message) { return new RuntimeContractException(message); }
    static List<String> preEvaluatorPathsForTest() { return List.copyOf(PRE_EVALUATOR.keySet()); }
    static Map<String,String> expectedGenerationInputDigests() { return Collections.unmodifiableMap(PRE_EVALUATOR); }
    public static byte[] toJson(Report report) throws Exception { return JSON.writeValueAsBytes(report); }

    public record Identity(String sourcePath, String granularity, String qualifiedSymbol) implements Comparable<Identity> {
        @Override public int compareTo(Identity other) {
            int path = sourcePath.compareTo(other.sourcePath);
            if (path != 0) return path;
            int kind = granularity.compareTo(other.granularity);
            return kind != 0 ? kind : qualifiedSymbol.compareTo(other.qualifiedSymbol);
        }
    }
    public record EvaluatorTruth(String goldSha256, List<Identity> expectedComponents) { }
    public record Metric(int matched, int expected, int proposed, Double recall, Double precision,
                         boolean recallDefined, boolean precisionDefined) { }
    public record Counts(int scenarios, int mappingProposals, int unresolvedScenarios, int proposedComponents) { }
    public record UnresolvedReferences(int unresolved, int total, double rate) { }
    public record TraceCounts(int direct, int inferred) { }
    public record PreviousBaseline(int graphNodeCovered, int graphNodeExpected, double graphNodeCoverage,
                                   int exactComponentMatched, int exactComponentExpected) { }
    public record ProviderDiagnostics(int formalComponentCredit, String separationRule) { }
    public record Report(String schemaVersion, String authority, boolean semanticPublicationAllowed,
                         String sourceRevision, String proposalSha256, String evaluatorGoldSha256,
                         Counts counts, Metric directSymbolRecall, Metric expandedChainCoverage,
                         Metric exactComponent, Metric scenarioTraceCoverage,
                         UnresolvedReferences unresolvedReferences, TraceCounts traceCounts,
                         PreviousBaseline previousBaseline, ProviderDiagnostics providerNativeDiagnostics,
                         List<String> limitations) { }
}

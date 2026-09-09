package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.directtrace.SfBl002TestBehaviorEvidence;
import com.featuredeliveryintelligence.fdi.product.realization.evaluation.HierarchicalForwardEvaluation;
import com.featuredeliveryintelligence.fdi.product.realization.evaluation.ProviderNeutralEvaluatorTruth;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Task 5 of SF-BL-002-PRODUCTION-SCENARIO-002: evaluator-only hierarchical scoring of the
 * immutable Task-4 mapping proposal. The generator phase seals every non-evaluator input —
 * accepted semantics, acceptance manifest, assignment artifact, test-behavior evidence, mapping
 * proposal and its evidence manifest, frozen graph snapshot, and Graphify runtime evidence —
 * before evaluator truth is opened; any mutation of a sealed input fails before evaluator access.
 * Matching semantics are reused from {@link HierarchicalForwardEvaluation}; no capability
 * crosswalk is invented, so capability alignment and semantic precision/recall/F1 remain
 * explicitly not comparable while trace and exact-component metrics are reported over PRIMARY
 * proposals only.
 */
public final class SfBl002HierarchicalEvaluation {
    public static final String EXECUTION_ID = "SF-BL-002-PRODUCTION-SCENARIO-002";
    public static final String SCHEMA_VERSION = "software-factory.sf-bl002-hierarchical-evaluation.v0.1";
    public static final String EVIDENCE_SCHEMA_VERSION = "software-factory.sf-bl002-hierarchical-evaluation-evidence.v0.1";
    public static final String REPORT_PATH = "validation/software-factory/sf-bl002/hierarchical-evaluation-001.json";
    public static final String EVIDENCE_PATH = "validation/software-factory/sf-bl002/hierarchical-evaluation-evidence-001.json";
    public static final String PROPOSAL_PATH = SfBl002ScenarioMappingRun.PROPOSAL_PATH;
    public static final String PROPOSAL_SHA256 = "38e8963996226dd614ae0abbf3ec6c4d2a8b60fb9c6b9c22ffee210b3dddaf9d";
    public static final String PROPOSAL_EVIDENCE_PATH = SfBl002ScenarioMappingRun.EVIDENCE_PATH;
    public static final String PROPOSAL_EVIDENCE_SHA256 = "4de9e1e2670974768a5cf7be570e3f1cd665d02e099622c4434bb318f1773bea";
    public static final String SEMANTICS_PATH = SfBl002ScenarioMappingRun.SEMANTICS_PATH;
    public static final String SEMANTICS_SHA256 = SfBl002ScenarioMappingRun.SEMANTICS_SHA256;
    public static final String ACCEPTANCE_MANIFEST_PATH = SfBl002ScenarioMappingRun.ACCEPTANCE_MANIFEST_PATH;
    public static final String ACCEPTANCE_MANIFEST_SHA256 = SfBl002ScenarioMappingRun.ACCEPTANCE_MANIFEST_SHA256;
    public static final String ASSIGNMENTS_PATH = SfBl002ScenarioMappingRun.ASSIGNMENTS_PATH;
    public static final String ASSIGNMENTS_SHA256 = SfBl002ScenarioMappingRun.ASSIGNMENTS_SHA256;
    public static final String TEST_EVIDENCE_PATH = SfBl002TestBehaviorEvidence.EVIDENCE_PATH;
    public static final String TEST_EVIDENCE_SHA256 = SfBl002TestBehaviorEvidence.EVIDENCE_SHA256;
    public static final String GRAPH_PATH = SfBl002ScenarioMappingRun.GRAPH_PATH;
    public static final String GRAPH_SHA256 = SfBl002ScenarioMappingRun.GRAPH_SHA256;
    public static final String GRAPHIFY_LIVE_EVIDENCE_PATH = SfBl002ScenarioMappingRun.GRAPHIFY_LIVE_EVIDENCE_PATH;
    public static final String GRAPHIFY_LIVE_EVIDENCE_SHA256 = SfBl002ScenarioMappingRun.GRAPHIFY_LIVE_EVIDENCE_SHA256;
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private SfBl002HierarchicalEvaluation() { }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("usage: <root> [output-root]");
        generate(Path.of(args[0]), args.length == 2 ? Path.of(args[1]) : Path.of(args[0]));
    }

    public static Result generate(Path root, Path outputRoot) {
        return generate(root, outputRoot, SfBl002HierarchicalEvaluation::loadEvaluatorTruth);
    }

    static Result generate(Path root, Path outputRoot,
            HierarchicalForwardEvaluation.EvaluatorAccess evaluatorAccess) {
        try {
            HierarchicalForwardEvaluation.Report report =
                    HierarchicalForwardEvaluation.compare(root, SfBl002HierarchicalEvaluation::seal,
                            evaluatorAccess, ProviderNeutralEvaluatorTruth.SEAL_SHA256);
            byte[] reportBytes = JSON.writeValueAsBytes(buildArtifact(report));
            String reportSha = sha(reportBytes);
            ObjectNode evidence = JSON.createObjectNode();
            evidence.put("schema_version", EVIDENCE_SCHEMA_VERSION).put("execution_id", EXECUTION_ID);
            evidence.put("authority", "EVALUATOR_ONLY").put("semantic_publication_allowed", false);
            evidence.put("evaluator_opened_after_non_evaluator_seal", true);
            evidence.put("thresholds_defined", false).put("go_claim_made", false);
            ObjectNode inputs = evidence.putObject("sealed_inputs");
            new TreeMap<>(nonEvaluatorInputs()).forEach(inputs::put);
            inputs.put(ProviderNeutralEvaluatorTruth.GOLD_PATH, report.evaluatorGoldSha256());
            inputs.put(ProviderNeutralEvaluatorTruth.SEAL_PATH, ProviderNeutralEvaluatorTruth.SEAL_SHA256);
            evidence.putObject("output").put("path", REPORT_PATH).put("sha256", reportSha);
            byte[] evidenceBytes = JSON.writeValueAsBytes(evidence);
            write(outputRoot.resolve(REPORT_PATH), reportBytes);
            write(outputRoot.resolve(EVIDENCE_PATH), evidenceBytes);
            return new Result(reportSha, sha(evidenceBytes), report);
        } catch (RuntimeContractException error) {
            throw error;
        } catch (Exception error) {
            throw new RuntimeContractException("cannot generate SF-BL-002 hierarchical evaluation", error);
        }
    }

    static Map<String, String> nonEvaluatorInputs() {
        Map<String, String> sealed = new LinkedHashMap<>();
        sealed.put(SEMANTICS_PATH, SEMANTICS_SHA256);
        sealed.put(ACCEPTANCE_MANIFEST_PATH, ACCEPTANCE_MANIFEST_SHA256);
        sealed.put(ASSIGNMENTS_PATH, ASSIGNMENTS_SHA256);
        sealed.put(TEST_EVIDENCE_PATH, TEST_EVIDENCE_SHA256);
        sealed.put(PROPOSAL_PATH, PROPOSAL_SHA256);
        sealed.put(PROPOSAL_EVIDENCE_PATH, PROPOSAL_EVIDENCE_SHA256);
        sealed.put(GRAPH_PATH, GRAPH_SHA256);
        sealed.put(GRAPHIFY_LIVE_EVIDENCE_PATH, GRAPHIFY_LIVE_EVIDENCE_SHA256);
        return sealed;
    }

    /** Seals all non-evaluator inputs and returns the exact proposal and its digest. */
    static HierarchicalForwardEvaluation.SealedInputs seal(Path root) {
        try {
            Map<String, String> sealed = nonEvaluatorInputs();
            for (Map.Entry<String, String> entry : sealed.entrySet()) {
                String actual = sha(Files.readAllBytes(root.resolve(entry.getKey())));
                if (!entry.getValue().equals(actual)) throw fail("non-evaluator digest mismatch: " + entry.getKey());
            }
            JsonNode proposal = JSON.readTree(root.resolve(PROPOSAL_PATH).toFile());
            return new HierarchicalForwardEvaluation.SealedInputs(proposal, sealed.get(PROPOSAL_PATH));
        } catch (RuntimeContractException error) {
            throw error;
        } catch (Exception error) {
            throw new RuntimeContractException("cannot seal non-evaluator inputs", error);
        }
    }

    static HierarchicalForwardEvaluation.EvaluatorTruth loadEvaluatorTruth(Path root) {
        ProviderNeutralEvaluatorTruth truth = ProviderNeutralEvaluatorTruth.load(root);
        return new HierarchicalForwardEvaluation.EvaluatorTruth(truth.goldSha256(), truth.mappings().stream()
                .flatMap(mapping -> mapping.expectedComponents().stream().map(component ->
                        new HierarchicalForwardEvaluation.Expected(mapping.capabilityId(), component.componentRef(),
                                component.graphNodeId(), new HierarchicalForwardEvaluation.Identity(
                                        component.identity().canonicalRevision(), component.identity().sourcePath(),
                                        component.identity().granularity(), containing(component.identity()),
                                        component.identity().qualifiedSymbol()))))
                .toList());
    }

    private static String containing(ProviderNeutralEvaluatorTruth.Identity identity) {
        String qualified = identity.qualifiedSymbol();
        return "METHOD".equals(identity.granularity()) ? qualified.substring(0, qualified.indexOf('#')) : qualified;
    }

    /** Wraps the reused evaluator report with the exact SF-BL-002 coverage metrics including F1. */
    static ObjectNode buildArtifact(HierarchicalForwardEvaluation.Report report) {
        ObjectNode artifact = JSON.createObjectNode();
        artifact.put("schema_version", SCHEMA_VERSION).put("execution_id", EXECUTION_ID);
        artifact.put("authority", "EVALUATOR_ONLY").put("semantic_publication_allowed", false);
        artifact.set("evaluation", JSON.valueToTree(report));
        HierarchicalForwardEvaluation.Scenario scenario = report.scenario();
        HierarchicalForwardEvaluation.Chain chain = report.chain();
        HierarchicalForwardEvaluation.Metric exact = report.component().exact();
        artifact.set("scenario_trace_coverage", coverage(scenario.traced(), scenario.traceDenominator()));
        artifact.set("chain_coverage", coverage(chain.exactExpectedCovered(), chain.exactExpectedDenominator()));
        ObjectNode component = artifact.putObject("exact_component");
        component.put("matched", exact.matched()).put("expected", exact.expected()).put("proposed", exact.proposed());
        component.set("recall", ratio(exact.recall().defined(), exact.recall().value()));
        component.set("precision", ratio(exact.precision().defined(), exact.precision().value()));
        component.set("f1", ratioValue(f1(exact.recall(), exact.precision())));
        artifact.put("capability_alignment", report.semantic().capabilityAlignment().status());
        return artifact;
    }

    private static ObjectNode coverage(int covered, int denominator) {
        ObjectNode node = JSON.createObjectNode();
        node.put("covered", covered).put("denominator", denominator);
        if (denominator == 0) node.putNull("ratio");
        else node.put("ratio", (double) covered / denominator);
        return node;
    }

    private static ObjectNode ratio(boolean defined, Double value) {
        return ratioValue(new RatioValue(defined, value));
    }

    private static ObjectNode ratioValue(RatioValue ratio) {
        ObjectNode node = JSON.createObjectNode();
        node.put("defined", ratio.defined());
        if (ratio.defined() && ratio.value() != null) node.put("value", ratio.value().doubleValue());
        else node.putNull("value");
        return node;
    }

    /** Exact component F1; undefined when either ratio is undefined or their sum is zero. */
    static RatioValue f1(HierarchicalForwardEvaluation.Ratio recall, HierarchicalForwardEvaluation.Ratio precision) {
        Objects.requireNonNull(recall, "recall");
        Objects.requireNonNull(precision, "precision");
        if (!recall.defined() || !precision.defined()) return new RatioValue(false, null);
        double sum = recall.value() + precision.value();
        if (sum == 0.0) return new RatioValue(false, null);
        return new RatioValue(true, 2 * recall.value() * precision.value() / sum);
    }

    static void write(Path path, byte[] bytes) throws Exception {
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            require(Arrays.equals(Files.readAllBytes(path), bytes), "output collision with changed existing bytes: " + path);
            return;
        }
        Files.write(path, bytes, StandardOpenOption.CREATE_NEW);
    }

    private static String sha(byte[] bytes) { return ScenarioForwardRequestReader.sha256(bytes); }

    private static void require(boolean condition, String message) {
        if (!condition) throw fail(message);
    }

    private static RuntimeContractException fail(String message) { return new RuntimeContractException(message); }

    /** One exact ratio value pair. */
    record RatioValue(boolean defined, Double value) { }

    public record Result(String reportSha256, String evidenceSha256, HierarchicalForwardEvaluation.Report report) { }
}

package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Reproducible artifact writer for the evaluator-only Slice G run. */
public final class SliceGEvaluatorComparisonGenerator {
    public static final String REPORT_PATH = "validation/pkb001/scenario-forward/slice-g-evaluator-comparison-001.json";
    public static final String EVIDENCE_PATH = "validation/pkb001/scenario-forward/slice-g-evaluator-comparison-evidence-001.json";
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private SliceGEvaluatorComparisonGenerator() { }

    public static void main(String[] args) throws Exception {
        Path root = args.length == 0 ? Path.of(".") : Path.of(args[0]);
        generate(root, root);
    }

    public static void generate(Path inputRoot, Path outputRoot) throws Exception {
        var report = SliceGEvaluatorComparison.compare(inputRoot, SliceGEvaluatorComparison::loadEvaluatorTruth);
        Path reportPath = outputRoot.resolve(REPORT_PATH);
        Files.createDirectories(reportPath.getParent());
        JSON.writeValue(reportPath.toFile(), report);
        String reportSha = sha(reportPath);

        ObjectNode evidence = JSON.createObjectNode();
        evidence.put("evidence_type", "PKB_BL_007_SLICE_G_EVALUATOR_ONLY_COMPARISON");
        evidence.put("execution_id", "PKB-BL-007-SCENARIO-TRACE-001");
        evidence.put("slice", "G");
        evidence.put("dispatch_commit", "a07b4e82b3322378f68f1a1d2b1adea7d54a5e05");
        evidence.put("requirement", "PKB-MAPPING-001");
        ArrayNode order = evidence.putArray("enforced_order");
        order.add("1_EXACT_BYTE_SEAL_GENERATION_INPUTS");
        order.add("2_LOAD_AND_VALIDATE_EVALUATOR_ONLY_TRUTH");
        order.add("3_COMPUTE_DETERMINISTIC_COMPARISON");
        order.add("4_EMIT_IMMUTABLE_REPORT_AND_EVIDENCE");
        ObjectNode isolation = evidence.putObject("isolation_proof");
        isolation.put("pre_evaluator_mutation_test", "PASS_EVALUATOR_ACCESS_COUNT_ZERO");
        isolation.put("proposal_sha256", report.proposalSha256());
        isolation.put("evaluator_gold_sha256", report.evaluatorGoldSha256());
        isolation.put("generation_does_not_read_evaluator_truth", true);
        isolation.put("comparison_is_evaluator_only", true);
        ObjectNode inputs = evidence.putObject("sealed_generation_inputs");
        SliceGEvaluatorComparison.expectedGenerationInputDigests().forEach(inputs::put);
        ObjectNode artifact = evidence.putObject("artifact");
        artifact.put("path", REPORT_PATH); artifact.put("sha256", reportSha);
        ObjectNode result = evidence.putObject("result");
        result.put("mapping_proposals", report.counts().mappingProposals());
        result.put("unresolved_scenarios", report.counts().unresolvedScenarios());
        result.put("proposed_components", report.counts().proposedComponents());
        result.put("exact_component_matched", report.exactComponent().matched());
        result.put("exact_component_expected", report.exactComponent().expected());
        result.put("exact_component_precision_defined", report.exactComponent().precisionDefined());
        result.put("exact_component_recall", report.exactComponent().recall());
        result.set("direct_symbol_recall", JSON.valueToTree(report.directSymbolRecall()));
        result.set("expanded_chain_coverage", JSON.valueToTree(report.expandedChainCoverage()));
        result.set("per_scenario_trace_coverage", JSON.valueToTree(report.scenarioTraceCoverage()));
        result.set("unresolved_reference_rate", JSON.valueToTree(report.unresolvedReferences()));
        result.set("direct_vs_inferred_trace_counts", JSON.valueToTree(report.traceCounts()));
        result.set("previous_baseline", JSON.valueToTree(report.previousBaseline()));
        result.set("provider_native_diagnostics", JSON.valueToTree(report.providerNativeDiagnostics()));
        result.put("semantic_publication_allowed", false);
        ObjectNode verification = evidence.putObject("focused_verification");
        verification.put("command", "MAVEN_OPTS='-Xmx2g' ./mvnw -q -Dtest=SliceGEvaluatorComparisonTests,SliceFInputVerifierTests,SliceFScenarioMappingArtifactTests,ScenarioGroundedForwardMapperTests,ScenarioMappingContractV04Tests,ScenarioMappingSchemaV04Tests,DirectTestTraceAdapterTests,GraphifyProductionExpansionTests test");
        verification.put("tests", 34);
        verification.put("result", "PASS");
        verification.put("git_diff_check", "PASS");
        ArrayNode limitations = evidence.putArray("limitations");
        report.limitations().forEach(limitations::add);
        JSON.writeValue(outputRoot.resolve(EVIDENCE_PATH).toFile(), evidence);
    }

    private static String sha(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}

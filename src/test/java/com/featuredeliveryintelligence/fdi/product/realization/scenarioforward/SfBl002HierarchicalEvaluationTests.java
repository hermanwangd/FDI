package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.evaluation.HierarchicalForwardEvaluation;
import com.featuredeliveryintelligence.fdi.product.realization.evaluation.ProviderNeutralEvaluatorTruth;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.SfBl002HierarchicalEvaluation.RatioValue;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SfBl002HierarchicalEvaluationTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    @TempDir Path temp;

    @Test void frozenRunReportsDescriptiveCoverageWithUndefinedPrecisionAndF1() throws Exception {
        Path output = temp.resolve("out");
        var result = SfBl002HierarchicalEvaluation.generate(Path.of("."), output);

        var report = result.report();
        assertEquals("pkb001.hierarchical-forward-evaluation.v1", report.schemaVersion());
        assertEquals("EVALUATOR_ONLY", report.authority());
        assertEquals(10, report.scenario().total());
        assertEquals(0, report.scenario().traced());
        assertEquals(10, report.scenario().traceDenominator());
        assertEquals(24, report.chain().exactExpectedDenominator());
        assertEquals(0, report.chain().exactExpectedCovered());
        assertEquals(0, report.component().exact().matched());
        assertEquals(24, report.component().exact().expected());
        assertEquals(0, report.component().exact().proposed());
        assertFalse(report.component().exact().precision().defined());
        assertTrue(report.component().exact().recall().defined());
        assertEquals(0.0, report.component().exact().recall().value());
        assertEquals("NOT_COMPARABLE_NO_SEALED_CROSSWALK", report.semantic().capabilityAlignment().status());

        JsonNode artifact = JSON.readTree(output.resolve(SfBl002HierarchicalEvaluation.REPORT_PATH).toFile());
        assertEquals("EVALUATOR_ONLY", artifact.path("authority").asText());
        assertFalse(artifact.path("semantic_publication_allowed").asBoolean());
        assertEquals(0.0, artifact.path("scenario_trace_coverage").path("ratio").asDouble());
        assertEquals(10, artifact.path("scenario_trace_coverage").path("denominator").asInt());
        assertEquals(0.0, artifact.path("chain_coverage").path("ratio").asDouble());
        assertEquals(24, artifact.path("chain_coverage").path("denominator").asInt());
        assertTrue(artifact.path("exact_component").path("precision").path("defined").asBoolean() == false);
        assertTrue(artifact.path("exact_component").path("f1").path("defined").asBoolean() == false);
        assertTrue(artifact.path("exact_component").path("f1").path("value").isNull());
        assertEquals("NOT_COMPARABLE_NO_SEALED_CROSSWALK", artifact.path("capability_alignment").asText());

        JsonNode evidence = JSON.readTree(output.resolve(SfBl002HierarchicalEvaluation.EVIDENCE_PATH).toFile());
        assertTrue(evidence.path("evaluator_opened_after_non_evaluator_seal").asBoolean());
        assertFalse(evidence.path("thresholds_defined").asBoolean());
        assertFalse(evidence.path("go_claim_made").asBoolean());
        assertEquals(result.reportSha256(), evidence.path("output").path("sha256").asText());
        assertEquals(ProviderNeutralEvaluatorTruth.SEAL_SHA256,
                evidence.path("sealed_inputs").path(ProviderNeutralEvaluatorTruth.SEAL_PATH).asText());
        assertEquals(ProviderNeutralEvaluatorTruth.GOLD_PATH.contains("gold-mappings-v2.json"), true);
    }

    @Test void mutatingAnySealedInputFailsBeforeEvaluatorAccess() throws Exception {
        for (String path : SfBl002HierarchicalEvaluation.nonEvaluatorInputs().keySet()) {
            Path root = temp.resolve(Integer.toHexString(path.hashCode()));
            copyInputs(root);
            Files.writeString(root.resolve(path), "\n", StandardOpenOption.APPEND);
            AtomicInteger accesses = new AtomicInteger();
            assertThrows(RuntimeContractException.class, () -> SfBl002HierarchicalEvaluation.generate(
                    root, temp.resolve("out-" + Integer.toHexString(path.hashCode())),
                    rootPath -> {
                        accesses.incrementAndGet();
                        return SfBl002HierarchicalEvaluation.loadEvaluatorTruth(rootPath);
                    }), path);
            assertEquals(0, accesses.get(), "evaluator opened for mutated input: " + path);
        }
    }

    @Test void evaluatorTruthOpensOnlyAfterNonEvaluatorSeal() throws Exception {
        Path root = temp.resolve("root");
        copyInputs(root);
        AtomicInteger accesses = new AtomicInteger();
        var result = SfBl002HierarchicalEvaluation.generate(root, temp.resolve("out"),
                rootPath -> {
                    accesses.incrementAndGet();
                    return SfBl002HierarchicalEvaluation.loadEvaluatorTruth(rootPath);
                });
        assertEquals(1, accesses.get());
        assertEquals(ProviderNeutralEvaluatorTruth.SEAL_SHA256, result.report().evaluatorSealSha256());
    }

    @Test void doubleRunIntoSeparateRootsIsByteIdentical() throws Exception {
        Path first = temp.resolve("first"), second = temp.resolve("second");
        var one = SfBl002HierarchicalEvaluation.generate(Path.of("."), first);
        var two = SfBl002HierarchicalEvaluation.generate(Path.of("."), second);
        assertEquals(one.reportSha256(), two.reportSha256());
        assertEquals(one.evidenceSha256(), two.evidenceSha256());
        assertArrayEquals(Files.readAllBytes(first.resolve(SfBl002HierarchicalEvaluation.REPORT_PATH)),
                Files.readAllBytes(second.resolve(SfBl002HierarchicalEvaluation.REPORT_PATH)));
        assertArrayEquals(Files.readAllBytes(first.resolve(SfBl002HierarchicalEvaluation.EVIDENCE_PATH)),
                Files.readAllBytes(second.resolve(SfBl002HierarchicalEvaluation.EVIDENCE_PATH)));
    }

    @Test void existingIdenticalOutputsAreReusedButChangedBytesFailClosed() throws Exception {
        Path output = temp.resolve("out");
        var first = SfBl002HierarchicalEvaluation.generate(Path.of("."), output);
        var second = SfBl002HierarchicalEvaluation.generate(Path.of("."), output);
        assertEquals(first.reportSha256(), second.reportSha256());
        Files.writeString(output.resolve(SfBl002HierarchicalEvaluation.REPORT_PATH),
                "\n", StandardOpenOption.APPEND);
        assertThrows(RuntimeContractException.class,
                () -> SfBl002HierarchicalEvaluation.generate(Path.of("."), output));
    }

    @Test void exactF1FollowsDefinedRatioRules() {
        var one = ratio(true, 1.0);
        var half = ratio(true, 0.5);
        var zero = ratio(true, 0.0);
        var undefined = ratio(false, null);
        RatioValue value = SfBl002HierarchicalEvaluation.f1(one, half);
        assertTrue(value.defined());
        assertEquals(2 * 1.0 * 0.5 / 1.5, value.value(), 1e-12);
        assertFalse(SfBl002HierarchicalEvaluation.f1(undefined, one).defined());
        assertFalse(SfBl002HierarchicalEvaluation.f1(one, undefined).defined());
        assertFalse(SfBl002HierarchicalEvaluation.f1(zero, zero).defined());
    }

    private static HierarchicalForwardEvaluation.Ratio ratio(boolean defined, Double value) {
        return new HierarchicalForwardEvaluation.Ratio(defined, value);
    }

    private static void copyInputs(Path root) throws Exception {
        for (String path : SfBl002HierarchicalEvaluation.nonEvaluatorInputs().keySet()) {
            copy(path, root);
        }
        for (String path : new String[]{ProviderNeutralEvaluatorTruth.GOLD_PATH, ProviderNeutralEvaluatorTruth.SEAL_PATH,
                ProviderNeutralEvaluatorTruth.LEGACY_GOLD_PATH, ProviderNeutralEvaluatorTruth.LEGACY_SEAL_PATH,
                ProviderNeutralEvaluatorTruth.GRAPH_PATH}) {
            copy(path, root);
        }
    }

    private static void copy(String path, Path root) throws Exception {
        Path from = Path.of(".").resolve(path), to = root.resolve(path);
        Files.createDirectories(to.getParent());
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    }
}

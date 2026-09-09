package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.evaluation.HierarchicalForwardEvaluation;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.SfBl002ScenarioEffectivenessEvaluation.RatioValue;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Slice C evaluator-only scoring tests. Every run binds only a synthetic mapping fixture and
 * temp-root synthetic upstream artifacts; evaluator truth is a test spy and the real
 * {@code ProviderNeutralEvaluatorTruth} gold/seal inputs are never opened.
 */
class SfBl002ScenarioEffectivenessEvaluationTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path FIXTURE = Path.of("src/test/resources/scenarioforward/sf-bl002/mapping-fixture-002.json");
    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String TYPE = "org.springframework.samples.petclinic.owner.";
    private static final String SYNTHETIC_GOLD_SHA = "1111111111111111111111111111111111111111111111111111111111111111";
    private static final String SYNTHETIC_SEAL_SHA = "2222222222222222222222222222222222222222222222222222222222222222";
    @TempDir Path temp;

    @Test void frozenSyntheticFixtureReportsExactPrimaryMetricsThresholdsAndDiagnostics() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        var result = generate(root, temp.resolve("out"), new AtomicInteger());

        var report = result.report();
        assertEquals("EVALUATOR_ONLY", report.authority());
        assertEquals(3, report.scenario().total());
        assertEquals(2, report.scenario().traced());
        assertEquals(2, report.chain().exactExpectedDenominator());
        assertEquals(1, report.chain().exactExpectedCovered());
        var exact = report.component().exact();
        assertEquals(1, exact.matched());
        assertEquals(2, exact.expected());
        assertEquals(2, exact.proposed());
        assertEquals(0.5, exact.recall().value(), 1e-12);
        assertEquals(0.5, exact.precision().value(), 1e-12);
        assertEquals(1, report.component().missing().size());
        assertEquals(1, report.component().extra().size());
        assertEquals("NOT_COMPARABLE_NO_SEALED_CROSSWALK", report.semantic().capabilityAlignment().status());
        assertFalse(report.semantic().productTruthScored());

        JsonNode artifact = JSON.readTree(temp.resolve("out").resolve(SfBl002ScenarioEffectivenessEvaluation.REPORT_PATH).toFile());
        assertEquals(SfBl002ScenarioEffectivenessEvaluation.SCHEMA_VERSION, artifact.path("schema_version").asText());
        assertEquals("EVALUATOR_ONLY", artifact.path("authority").asText());
        assertFalse(artifact.path("semantic_publication_allowed").asBoolean());
        assertEquals(2, artifact.path("scenario_trace_coverage").path("covered").asInt());
        assertEquals(3, artifact.path("scenario_trace_coverage").path("denominator").asInt());
        assertEquals(2.0 / 3.0, artifact.path("scenario_trace_coverage").path("ratio").asDouble(), 1e-12);
        assertEquals(0.5, artifact.path("chain_coverage").path("ratio").asDouble(), 1e-12);
        JsonNode f1 = artifact.path("exact_component").path("f1");
        assertTrue(f1.path("defined").asBoolean());
        assertEquals(0.5, f1.path("value").asDouble(), 1e-12);
        assertEquals("NOT_COMPARABLE_NO_SEALED_CROSSWALK", artifact.path("capability_alignment").asText());
        JsonNode thresholds = artifact.path("threshold_results");
        assertFalse(thresholds.path("enforced").asBoolean());
        assertTrue(thresholds.path("scenario_trace_coverage").path("met").asBoolean());
        assertTrue(thresholds.path("exact_chain_recall").path("met").asBoolean());
        assertFalse(thresholds.path("exact_primary_precision").path("met").asBoolean());
        assertEquals(0.5, thresholds.path("exact_primary_precision").path("observed").asDouble(), 1e-12);
        JsonNode supporting = artifact.path("supporting_overlap");
        assertEquals(1, supporting.path("supporting_count").asInt());
        assertEquals(0, supporting.path("exact_overlap").asInt());
        assertEquals(0, supporting.path("formal_credit").asInt());

        JsonNode evidence = JSON.readTree(temp.resolve("out").resolve(SfBl002ScenarioEffectivenessEvaluation.EVIDENCE_PATH).toFile());
        assertTrue(evidence.path("evaluator_opened_after_non_evaluator_seal").asBoolean());
        assertTrue(evidence.path("thresholds_recorded").asBoolean());
        assertFalse(evidence.path("go_claim_made").asBoolean());
        assertEquals(result.reportSha256(), evidence.path("output").path("sha256").asText());
        assertEquals(SYNTHETIC_GOLD_SHA, evidence.path("sealed_inputs").path("synthetic-evaluator-gold").asText());
    }

    @Test void mutatingAnyNonEvaluatorInputFailsBeforeEvaluatorAccess() throws Exception {
        List<String> synthetic = List.of(
                SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_PATH,
                SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_EVIDENCE_PATH,
                SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH,
                SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_MANIFEST_PATH);
        for (String path : SfBl002ScenarioEffectivenessEvaluation.nonEvaluatorInputs().keySet()) {
            assertSealedBeforeEvaluatorAccess(path);
        }
        for (String path : synthetic) {
            assertSealedBeforeEvaluatorAccess(path);
        }
    }

    private void assertSealedBeforeEvaluatorAccess(String path) throws Exception {
        Path root = temp.resolve(Integer.toHexString(path.hashCode()));
        buildRoot(root);
        if (path.equals(SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_MANIFEST_PATH)) {
            corruptDigest(root.resolve(path), "artifact");
        } else if (path.equals(SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_EVIDENCE_PATH)) {
            corruptDigest(root.resolve(path), "output");
        } else {
            Files.writeString(root.resolve(path), "\n", StandardOpenOption.APPEND);
        }
        AtomicInteger accesses = new AtomicInteger();
        assertThrows(RuntimeContractException.class,
                () -> generate(root, temp.resolve("out-" + Integer.toHexString(path.hashCode())), accesses));
        assertEquals(0, accesses.get(), "evaluator opened for mutated input: " + path);
    }

    private static void corruptDigest(Path file, String object) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(file.toFile());
        ((ObjectNode) doc.path(object)).put("sha256",
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
        JSON.writeValue(file.toFile(), doc);
    }

    private static void rebindMappingEvidence(Path root) throws Exception {
        Path evidencePath = root.resolve(SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_EVIDENCE_PATH);
        ObjectNode evidence = (ObjectNode) JSON.readTree(evidencePath.toFile());
        ((ObjectNode) evidence.path("output")).put("sha256", sha(Files.readAllBytes(
                root.resolve(SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_PATH))));
        JSON.writeValue(evidencePath.toFile(), evidence);
    }

    @Test void evaluatorTruthOpensExactlyOnceAfterNonEvaluatorSeal() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        AtomicInteger accesses = new AtomicInteger();
        var result = generate(root, temp.resolve("out"), accesses);
        assertEquals(1, accesses.get());
        assertEquals(SYNTHETIC_GOLD_SHA, result.report().evaluatorGoldSha256());
        assertEquals(SYNTHETIC_SEAL_SHA, result.report().evaluatorSealSha256());
    }

    @Test void undefinedF1IsReportedNotInventedWhenGoldPrimaryEmpty() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        var result = generate(root, temp.resolve("out"), new AtomicInteger(),
                new HierarchicalForwardEvaluation.EvaluatorTruth(SYNTHETIC_GOLD_SHA, List.of()));
        var exact = result.report().component().exact();
        assertFalse(exact.recall().defined());
        assertTrue(exact.precision().defined());
        JsonNode artifact = JSON.readTree(temp.resolve("out").resolve(SfBl002ScenarioEffectivenessEvaluation.REPORT_PATH).toFile());
        JsonNode f1 = artifact.path("exact_component").path("f1");
        assertFalse(f1.path("defined").asBoolean());
        assertTrue(f1.path("value").isNull());
        assertFalse(artifact.path("threshold_results").path("exact_primary_precision").path("met").asBoolean());
    }

    @Test void undefinedF1IsReportedNotInventedWhenNoPrimaryProposed() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        ObjectNode emptyPrimary = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_PATH).toFile());
        emptyPrimary.path("capabilities").forEach(capability -> capability.path("scenarios").forEach(scenario -> {
            ObjectNode mapping = (ObjectNode) scenario.path("mapping");
            mapping.put("outcome", "UNRESOLVED");
            ((ObjectNode) scenario).remove("componentRoles");
        }));
        JSON.writerWithDefaultPrettyPrinter().writeValue(
                root.resolve(SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_PATH).toFile(), emptyPrimary);
        rebindMappingEvidence(root);
        var result = generate(root, temp.resolve("out"), new AtomicInteger());
        var exact = result.report().component().exact();
        assertTrue(exact.recall().defined());
        assertFalse(exact.precision().defined());
        JsonNode artifact = JSON.readTree(temp.resolve("out").resolve(SfBl002ScenarioEffectivenessEvaluation.REPORT_PATH).toFile());
        assertFalse(artifact.path("exact_component").path("f1").path("defined").asBoolean());
        assertTrue(artifact.path("exact_component").path("f1").path("value").isNull());
    }

    @Test void doubleRunIntoSeparateRootsIsByteIdentical() throws Exception {
        Path first = temp.resolve("first"), second = temp.resolve("second");
        buildRoot(temp.resolve("root-one"));
        buildRoot(temp.resolve("root-two"));
        var one = generate(temp.resolve("root-one"), first, new AtomicInteger());
        var two = generate(temp.resolve("root-two"), second, new AtomicInteger());
        assertEquals(one.reportSha256(), two.reportSha256());
        assertEquals(one.evidenceSha256(), two.evidenceSha256());
        assertArrayEquals(Files.readAllBytes(first.resolve(SfBl002ScenarioEffectivenessEvaluation.REPORT_PATH)),
                Files.readAllBytes(second.resolve(SfBl002ScenarioEffectivenessEvaluation.REPORT_PATH)));
        assertArrayEquals(Files.readAllBytes(first.resolve(SfBl002ScenarioEffectivenessEvaluation.EVIDENCE_PATH)),
                Files.readAllBytes(second.resolve(SfBl002ScenarioEffectivenessEvaluation.EVIDENCE_PATH)));
    }

    @Test void existingIdenticalOutputsAreReusedButChangedBytesFailClosed() throws Exception {
        buildRoot(temp.resolve("root"));
        Path output = temp.resolve("out");
        var first = generate(temp.resolve("root"), output, new AtomicInteger());
        var second = generate(temp.resolve("root"), output, new AtomicInteger());
        assertEquals(first.reportSha256(), second.reportSha256());
        Files.writeString(output.resolve(SfBl002ScenarioEffectivenessEvaluation.REPORT_PATH),
                "\n", StandardOpenOption.APPEND);
        assertThrows(RuntimeContractException.class,
                () -> generate(temp.resolve("root"), output, new AtomicInteger()));
    }

    @Test void mappingProposalWholeDocumentValidationFailsClosed() throws Exception {
        for (String mutation : List.of("publication", "revision", "duplicate", "digest", "outcome", "vocabulary")) {
            Path root = temp.resolve(mutation);
            buildRoot(root);
            mutateMapping(root, mutation);
            AtomicInteger accesses = new AtomicInteger();
            assertThrows(RuntimeContractException.class,
                    () -> generate(root, temp.resolve("out-" + mutation), accesses), mutation);
            assertEquals(0, accesses.get(), "evaluator opened after mutation: " + mutation);
        }
    }

    private static void mutateMapping(Path root, String mutation) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_PATH).toFile());
        switch (mutation) {
            case "publication" -> doc.put("semantic_publication_allowed", true);
            case "revision" -> ((ObjectNode) doc.path("capabilities").get(0).path("scenarios").get(0)
                    .path("mapping")).put("sourceRevision", "0000000000000000000000000000000000000000");
            case "duplicate" -> {
                JsonNode first = doc.path("capabilities").get(0).path("scenarios").get(0);
                ((ObjectNode) doc.path("capabilities").get(0)).putArray("scenarios").add(first.deepCopy());
            }
            case "digest" -> doc.put("semanticsSha256",
                    "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
            case "outcome" -> ((ObjectNode) doc.path("capabilities").get(0).path("scenarios").get(0)
                    .path("mapping")).put("outcome", "PRODUCT_TRUTH");
            case "vocabulary" -> ((ObjectNode) doc.path("capabilities").get(0).path("scenarios").get(0)
                    .path("mapping")).put("scenarioId", "SCN-SYNTH-EVALUATOR-GOLD");
            default -> throw new IllegalStateException(mutation);
        }
        JSON.writerWithDefaultPrettyPrinter().writeValue(
                root.resolve(SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_PATH).toFile(), doc);
    }

    @Test void frozenFixtureAndSyntheticInputsCarryNoEvaluatorVocabulary() throws Exception {
        String fixture = Files.readString(FIXTURE);
        assertFalse(SfBl002ScenarioEffectivenessEvaluation.EVALUATOR_VOCABULARY.matcher(fixture).find(),
                "frozen mapping fixture embeds evaluator vocabulary");
        buildRoot(temp.resolve("root"));
        for (String path : new String[]{SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH,
                SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_MANIFEST_PATH,
                SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_EVIDENCE_PATH}) {
            assertFalse(SfBl002ScenarioEffectivenessEvaluation.EVALUATOR_VOCABULARY.matcher(
                    Files.readString(temp.resolve("root").resolve(path))).find(),
                    "synthetic upstream artifact embeds evaluator vocabulary: " + path);
        }
    }

    @Test void exactF1FollowsDefinedRatioRules() {
        var one = ratio(true, 1.0);
        var half = ratio(true, 0.5);
        var zero = ratio(true, 0.0);
        var undefined = ratio(false, null);
        RatioValue value = SfBl002ScenarioEffectivenessEvaluation.f1(one, half);
        assertTrue(value.defined());
        assertEquals(2 * 1.0 * 0.5 / 1.5, value.value(), 1e-12);
        assertFalse(SfBl002ScenarioEffectivenessEvaluation.f1(undefined, one).defined());
        assertFalse(SfBl002ScenarioEffectivenessEvaluation.f1(one, undefined).defined());
        assertFalse(SfBl002ScenarioEffectivenessEvaluation.f1(zero, zero).defined());
    }

    private static HierarchicalForwardEvaluation.Ratio ratio(boolean defined, Double value) {
        return new HierarchicalForwardEvaluation.Ratio(defined, value);
    }

    private SfBl002ScenarioEffectivenessEvaluation.Result generate(Path root, Path output, AtomicInteger accesses) {
        return generate(root, output, accesses, syntheticTruth());
    }

    private static SfBl002ScenarioEffectivenessEvaluation.Result generate(Path root, Path output,
            AtomicInteger accesses, HierarchicalForwardEvaluation.EvaluatorTruth truth) {
        return SfBl002ScenarioEffectivenessEvaluation.generate(root, output,
                rootPath -> {
                    accesses.incrementAndGet();
                    return truth;
                },
                SYNTHETIC_SEAL_SHA, "synthetic-evaluator-gold", "synthetic-evaluator-seal");
    }

    private static HierarchicalForwardEvaluation.EvaluatorTruth syntheticTruth() {
        return new HierarchicalForwardEvaluation.EvaluatorTruth(SYNTHETIC_GOLD_SHA, List.of(
                new HierarchicalForwardEvaluation.Expected("CAP-SYNTH-A", "REF-X", "node-x", identity(
                        "PetController.java", "PetController#processCreationForm")),
                new HierarchicalForwardEvaluation.Expected("CAP-SYNTH-B", "REF-Y", "node-y", identity(
                        "OwnerController.java", "OwnerController#processCreationForm"))));
    }

    private static HierarchicalForwardEvaluation.Identity identity(String file, String symbol) {
        return new HierarchicalForwardEvaluation.Identity(REVISION,
                "src/main/java/org/springframework/samples/petclinic/owner/" + file,
                "METHOD", TYPE + symbol.substring(0, symbol.indexOf('#')), TYPE + symbol);
    }

    /** Builds a complete temp root: real pinned inputs plus synthetic upstream artifacts and the frozen fixture. */
    private static void buildRoot(Path root) throws Exception {
        for (String path : SfBl002ScenarioEffectivenessEvaluation.nonEvaluatorInputs().keySet()) {
            Path to = root.resolve(path);
            Files.createDirectories(to.getParent());
            Files.copy(Path.of(".").resolve(path), to, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.createDirectories(root.resolve(SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_PATH).getParent());
        Files.copy(FIXTURE, root.resolve(SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_PATH),
                StandardCopyOption.REPLACE_EXISTING);
        String assignments = """
                {
                  "schema_version": "software-factory.sf-bl002-scenario-observation-assignments.v0.2",
                  "execution_id": "SF-BL-002-SCENARIO-MATCHER-002-SYNTHETIC-FIXTURE",
                  "authority": "PROPOSAL_ONLY",
                  "semantic_publication_allowed": false,
                  "source_revision": "%s",
                  "semantics_sha256": "%s",
                  "authorization_sha256": "%s",
                  "test_evidence_sha256": "%s",
                  "assignments": [
                    {"capabilityId": "CAP-SYNTH-A", "scenarioId": "SCN-SYNTH-101", "directEvidenceRefs": [], "gapRefs": []},
                    {"capabilityId": "CAP-SYNTH-B", "scenarioId": "SCN-SYNTH-102", "directEvidenceRefs": [], "gapRefs": []},
                    {"capabilityId": "CAP-SYNTH-C", "scenarioId": "SCN-SYNTH-103", "directEvidenceRefs": [], "gapRefs": []}
                  ]
                }
                """.formatted(REVISION,
                SfBl002ScenarioEffectivenessEvaluation.SEMANTICS_SHA256,
                SfBl002ScenarioEffectivenessEvaluation.AUTHORIZATION_SHA256,
                SfBl002ScenarioEffectivenessEvaluation.TEST_EVIDENCE_SHA256);
        Files.writeString(root.resolve(SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH), assignments);
        String manifest = """
                {
                  "schema_version": "software-factory.sf-bl002-artifact-manifest.v0.2",
                  "execution_id": "SF-BL-002-SCENARIO-MATCHER-002-SYNTHETIC-FIXTURE",
                  "artifact": {
                    "path": "%s",
                    "sha256": "%s"
                  },
                  "inputs": {
                    "semantics": "%s",
                    "authorization": "%s",
                    "test_evidence": "%s"
                  }
                }
                """.formatted(SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH,
                sha(Files.readAllBytes(root.resolve(SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH))),
                SfBl002ScenarioEffectivenessEvaluation.SEMANTICS_SHA256,
                SfBl002ScenarioEffectivenessEvaluation.AUTHORIZATION_SHA256,
                SfBl002ScenarioEffectivenessEvaluation.TEST_EVIDENCE_SHA256);
        Files.writeString(root.resolve(SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_MANIFEST_PATH), manifest);
        String mappingEvidence = """
                {
                  "schema_version": "software-factory.sf-bl002-scenario-mapping-evidence.v0.2",
                  "execution_id": "SF-BL-002-SCENARIO-MAPPING-002-SYNTHETIC-FIXTURE",
                  "authority": "PROPOSAL_ONLY",
                  "semantic_publication_allowed": false,
                  "output": {
                    "path": "%s",
                    "sha256": "%s"
                  }
                }
                """.formatted(SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_PATH,
                sha(Files.readAllBytes(root.resolve(SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_PATH))));
        Files.writeString(root.resolve(SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_EVIDENCE_PATH),
                mappingEvidence);
    }

    private static String sha(byte[] bytes) {
        return com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader.sha256(bytes);
    }
}

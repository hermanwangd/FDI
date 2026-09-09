package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.evaluation.HierarchicalForwardEvaluation;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.SfBl002ScenarioEffectivenessEvaluation.RatioValue;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Slice C evaluator-only scoring tests. Every run binds only a synthetic mapping fixture in the
 * real v0.2 producer shape and temp-root synthetic upstream artifacts; evaluator truth is a test
 * spy and the real {@code ProviderNeutralEvaluatorTruth} gold/seal inputs are never opened.
 */
class SfBl002ScenarioEffectivenessEvaluationTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path FIXTURE = Path.of("src/test/resources/scenarioforward/sf-bl002/mapping-fixture-002.json");
    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String TYPE = "org.springframework.samples.petclinic.owner.";
    private static final String RATIONALE_101 =
            "PRIMARY synthetic direct-test-reference:synth-0001 selects PetController#processCreationForm"
                    + " (frozen fixture stand-in, not production evidence)";
    private static final String RATIONALE_102 =
            "PRIMARY synthetic direct-test-reference:synth-0002 selects OwnerController#processFindFormSuccess"
                    + " (frozen fixture stand-in, not production evidence)";
    private static final String RATIONALE_103 =
            "no production identity accepted for this scenario; honest UNRESOLVED (frozen fixture stand-in)";
    private static final String SYNTHETIC_GOLD_SHA = "1111111111111111111111111111111111111111111111111111111111111111";
    private static final String SYNTHETIC_SEAL_SHA = "2222222222222222222222222222222222222222222222222222222222222222";
    @TempDir Path temp;

    @Test void frozenSyntheticFixtureReportsExactPrimaryMetricsThresholdsAndDiagnostics() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        var result = generate(root, temp.resolve("out"), new AtomicInteger());

        var report = result.report();
        assertEquals("EVALUATOR_ONLY", report.authority());
        assertEquals(10, report.scenario().total());
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
        assertEquals(10, artifact.path("scenario_trace_coverage").path("denominator").asInt());
        assertEquals(0.2, artifact.path("scenario_trace_coverage").path("ratio").asDouble(), 1e-12);
        assertEquals(0.5, artifact.path("chain_coverage").path("ratio").asDouble(), 1e-12);
        JsonNode f1 = artifact.path("exact_component").path("f1");
        assertTrue(f1.path("defined").asBoolean());
        assertEquals(0.5, f1.path("value").asDouble(), 1e-12);
        assertEquals("NOT_COMPARABLE_NO_SEALED_CROSSWALK", artifact.path("capability_alignment").asText());
        JsonNode thresholds = artifact.path("threshold_results");
        assertFalse(thresholds.path("enforced").asBoolean());
        assertFalse(thresholds.path("scenario_trace_coverage").path("met").asBoolean());
        assertEquals(0.2, thresholds.path("scenario_trace_coverage").path("observed").asDouble(), 1e-12);
        assertTrue(thresholds.path("exact_chain_recall").path("met").asBoolean());
        assertFalse(thresholds.path("exact_primary_precision").path("met").asBoolean());
        assertEquals(0.5, thresholds.path("exact_primary_precision").path("observed").asDouble(), 1e-12);
        JsonNode supporting = artifact.path("supporting_overlap");
        assertEquals(1, supporting.path("supporting_count").asInt());
        assertEquals(0, supporting.path("provider_node_overlap").asInt());
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

    /** Rebinds every digest the mapping evidence carries after a synthetic proposal/assignments edit. */
    private static void rebindMappingEvidence(Path root) throws Exception {
        Path evidencePath = root.resolve(SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_EVIDENCE_PATH);
        ObjectNode evidence = (ObjectNode) JSON.readTree(evidencePath.toFile());
        ObjectNode inputs = (ObjectNode) evidence.path("inputs");
        inputs.put(SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH, sha(Files.readAllBytes(
                root.resolve(SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH))));
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
        ObjectNode assignments = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH).toFile());
        assignments.path("assignments").forEach(record -> {
            ((ObjectNode) record).put("primaryEvidenceRef", "UNRESOLVED");
            ((ObjectNode) record).put("status", "UNRESOLVED");
        });
        JSON.writerWithDefaultPrettyPrinter().writeValue(
                root.resolve(SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH).toFile(), assignments);
        ObjectNode manifest = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_MANIFEST_PATH).toFile());
        ((ObjectNode) manifest.path("artifact")).put("sha256", sha(Files.readAllBytes(root.resolve(
                SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH))));
        JSON.writerWithDefaultPrettyPrinter().writeValue(
                root.resolve(SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_MANIFEST_PATH).toFile(), manifest);
        ObjectNode proposal = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_PATH).toFile());
        ((ObjectNode) proposal.path("inputs")).put(SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH,
                sha(Files.readAllBytes(root.resolve(SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH))));
        proposal.path("scenarios").forEach(scenario -> {
            ((ObjectNode) scenario).put("outcome", "UNRESOLVED");
            ((ObjectNode) scenario).putNull("primary");
        });
        JSON.writerWithDefaultPrettyPrinter().writeValue(
                root.resolve(SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_PATH).toFile(), proposal);
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
            rebindMappingEvidence(root);
            AtomicInteger accesses = new AtomicInteger();
            assertThrows(RuntimeContractException.class,
                    () -> generate(root, temp.resolve("out-" + mutation), accesses), mutation);
            assertEquals(0, accesses.get(), "evaluator opened after mutation: " + mutation);
        }
    }

    private static void mutateMapping(Path root, String mutation) throws Exception {
        Path proposalPath = root.resolve(SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_PATH);
        ObjectNode doc = (ObjectNode) JSON.readTree(proposalPath.toFile());
        switch (mutation) {
            case "publication" -> doc.put("semantic_publication_allowed", true);
            case "revision" -> ((ObjectNode) doc.path("scenarios").get(0).path("primary")
                    .path("productionSymbol")).put("sourceRevision",
                            "0000000000000000000000000000000000000000");
            case "duplicate" -> {
                JsonNode first = doc.path("scenarios").get(0);
                ((ArrayNode) doc.path("scenarios")).add(first.deepCopy());
            }
            case "digest" -> ((ObjectNode) doc.path("inputs")).put(
                    SfBl002ScenarioEffectivenessEvaluation.TEST_EVIDENCE_PATH,
                    "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
            case "outcome" -> ((ObjectNode) doc.path("scenarios").get(0)).put("outcome", "PRODUCT_TRUTH");
            case "vocabulary" -> ((ObjectNode) doc.path("scenarios").get(0))
                    .put("scenarioId", "SCN-SYNTH-EVALUATOR-GOLD");
            default -> throw new IllegalStateException(mutation);
        }
        JSON.writerWithDefaultPrettyPrinter().writeValue(proposalPath.toFile(), doc);
    }

    @Test void assignmentsWholeDocumentValidationFailsClosed() throws Exception {
        for (String mutation : List.of("legacyAuthorization", "digest", "status", "rationale")) {
            Path root = temp.resolve("assignments-" + mutation);
            buildRoot(root);
            mutateAssignments(root, mutation);
            AtomicInteger accesses = new AtomicInteger();
            assertThrows(RuntimeContractException.class,
                    () -> generate(root, temp.resolve("out-assignments-" + mutation), accesses), mutation);
            assertEquals(0, accesses.get(), "evaluator opened after assignment mutation: " + mutation);
        }
    }

    private static void mutateAssignments(Path root, String mutation) throws Exception {
        Path assignmentsPath = root.resolve(SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH);
        ObjectNode doc = (ObjectNode) JSON.readTree(assignmentsPath.toFile());
        switch (mutation) {
            case "legacyAuthorization" -> doc.put("authorization_sha256",
                    "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
            case "digest" -> doc.put("test_evidence_sha256",
                    "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
            case "status" -> ((ObjectNode) doc.path("assignments").get(0)).put("status", "UNRESOLVED");
            case "rationale" -> ((ObjectNode) doc.path("assignments").get(0)).put("selectionRationale",
                    "expected mapping per reviewer ground truth");
            default -> throw new IllegalStateException(mutation);
        }
        JSON.writerWithDefaultPrettyPrinter().writeValue(assignmentsPath.toFile(), doc);
    }

    @Test void derivedModelDeduplicatesSharedPrimaryComponentsWithinCapability() {
        ObjectNode proposal = syntheticSharedComponentProposal();
        ObjectNode derived = SfBl002ScenarioEffectivenessEvaluation.derivedModel(proposal, REVISION,
                Map.of(SfBl002ScenarioEffectivenessEvaluation.SEMANTICS_PATH,
                        SfBl002ScenarioEffectivenessEvaluation.SEMANTICS_SHA256),
                SfBl002ScenarioEffectivenessEvaluation.AUTHORIZATION_SHA256);
        JsonNode capability = derived.path("capabilities").get(0);
        assertEquals("CAP-DUP", capability.path("capabilityId").asText());
        assertEquals(2, capability.path("scenarios").size());
        int roles = 0, chainSteps = 0, traced = 0;
        for (JsonNode scenario : capability.path("scenarios")) {
            roles += scenario.path("componentRoles").size();
            chainSteps += scenario.path("mapping").path("realizationChain").size();
            if (!scenario.path("mapping").path("realizationChain").isEmpty()) traced++;
        }
        assertEquals(1, roles, "distinct component credited once per capability");
        assertEquals(1, chainSteps, "distinct chain component contributed once per capability");
        assertEquals(1, traced, "only the first scenario of a shared component is traced");
    }

    private static ObjectNode syntheticSharedComponentProposal() {
        ObjectNode proposal = JSON.createObjectNode();
        ArrayNode scenarios = proposal.putArray("scenarios");
        for (String scenarioId : List.of("SCN-DUP-1", "SCN-DUP-2")) {
            ObjectNode scenario = scenarios.addObject();
            scenario.put("capabilityId", "CAP-DUP").put("scenarioId", scenarioId);
            scenario.put("outcome", "MAPPING_PROPOSAL");
            ObjectNode primary = scenario.putObject("primary");
            primary.put("evidenceRef", "direct-test-reference:synth-dup");
            primary.put("providerNodeId", "node-dup");
            ObjectNode symbol = primary.putObject("productionSymbol");
            symbol.put("sourceRevision", REVISION);
            symbol.put("sourcePath", "src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java");
            symbol.put("granularity", "METHOD");
            symbol.put("qualifiedSymbol",
                    "org.springframework.samples.petclinic.owner.OwnerRepository#findByLastNameStartingWith");
        }
        return proposal;
    }

    @Test void frozenFixtureAndSyntheticInputsCarryNoEvaluatorVocabulary() throws Exception {
        assertNoEvaluatorVocabulary(JSON.readTree(FIXTURE.toFile()), "frozen mapping fixture");
        buildRoot(temp.resolve("root"));
        for (String path : new String[]{SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH,
                SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_MANIFEST_PATH,
                SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_EVIDENCE_PATH}) {
            assertNoEvaluatorVocabulary(JSON.readTree(temp.resolve("root").resolve(path).toFile()),
                    "synthetic upstream artifact: " + path);
        }
    }

    private static void assertNoEvaluatorVocabulary(JsonNode node, String label) {
        if (node.isTextual()) {
            assertFalse(SfBl002ScenarioEffectivenessEvaluation.EVALUATOR_VOCABULARY.matcher(node.asText()).find(),
                    label + " embeds evaluator vocabulary");
            return;
        }
        node.forEach(child -> assertNoEvaluatorVocabulary(child, label));
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
                new HierarchicalForwardEvaluation.Expected("HYP-CAPABILITY-001", "REF-X", "node-x", identity(
                        "PetController.java", "PetController#processCreationForm")),
                new HierarchicalForwardEvaluation.Expected("HYP-CAPABILITY-002", "REF-Y", "node-y", identity(
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
        Files.createDirectories(root.resolve(SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH).getParent());
        String assignments = """
                {
                  "schema_version": "software-factory.sf-bl002-scenario-observation-assignments.v0.2",
                  "execution_id": "SF-BL-002-SCENARIO-EFFECTIVENESS-004",
                  "authority": "PROPOSAL_ONLY",
                  "semantic_publication_allowed": false,
                  "source_revision": "%s",
                  "tokenization_policy": "CAMEL_CASE_LATIN_V1",
                  "semantics_sha256": "%s",
                  "acceptance_manifest_sha256": "%s",
                  "intent_sha256": "%s",
                  "intent_acceptance_sha256": "%s",
                  "test_evidence_sha256": "%s",
                  "assignments": [
                    {"capabilityId": "HYP-CAPABILITY-001", "scenarioId": "HYP-SCENARIO-001", "primaryEvidenceRef": "direct-test-reference:synth-0001", "status": "PRIMARY_ASSIGNED", "selectionRationale": "%s", "gapRefs": []},
                    {"capabilityId": "HYP-CAPABILITY-001", "scenarioId": "HYP-SCENARIO-002", "primaryEvidenceRef": "direct-test-reference:synth-0002", "status": "PRIMARY_ASSIGNED", "selectionRationale": "%s", "gapRefs": []},
                    {"capabilityId": "HYP-CAPABILITY-002", "scenarioId": "HYP-SCENARIO-003", "primaryEvidenceRef": "UNRESOLVED", "status": "UNRESOLVED", "selectionRationale": "%s", "gapRefs": ["/synthetic/gaps/0"]},
                    {"capabilityId": "HYP-CAPABILITY-002", "scenarioId": "HYP-SCENARIO-004", "primaryEvidenceRef": "UNRESOLVED", "status": "UNRESOLVED", "selectionRationale": "%s", "gapRefs": []},
                    {"capabilityId": "HYP-CAPABILITY-003", "scenarioId": "HYP-SCENARIO-005", "primaryEvidenceRef": "UNRESOLVED", "status": "UNRESOLVED", "selectionRationale": "%s", "gapRefs": []},
                    {"capabilityId": "HYP-CAPABILITY-003", "scenarioId": "HYP-SCENARIO-006", "primaryEvidenceRef": "UNRESOLVED", "status": "UNRESOLVED", "selectionRationale": "%s", "gapRefs": []},
                    {"capabilityId": "HYP-CAPABILITY-004", "scenarioId": "HYP-SCENARIO-007", "primaryEvidenceRef": "UNRESOLVED", "status": "UNRESOLVED", "selectionRationale": "%s", "gapRefs": []},
                    {"capabilityId": "HYP-CAPABILITY-004", "scenarioId": "HYP-SCENARIO-008", "primaryEvidenceRef": "UNRESOLVED", "status": "UNRESOLVED", "selectionRationale": "%s", "gapRefs": []},
                    {"capabilityId": "HYP-CAPABILITY-005", "scenarioId": "HYP-SCENARIO-009", "primaryEvidenceRef": "UNRESOLVED", "status": "UNRESOLVED", "selectionRationale": "%s", "gapRefs": []},
                    {"capabilityId": "HYP-CAPABILITY-003", "scenarioId": "HYP-SCENARIO-011", "primaryEvidenceRef": "UNRESOLVED", "status": "UNRESOLVED", "selectionRationale": "%s", "gapRefs": []}
                  ]
                }
                """.formatted(REVISION,
                SfBl002ScenarioEffectivenessEvaluation.SEMANTICS_SHA256,
                SfBl002ScenarioEffectivenessEvaluation.ACCEPTANCE_MANIFEST_SHA256,
                SfBl002ScenarioEffectivenessEvaluation.INTENTS_SHA256,
                SfBl002ScenarioEffectivenessEvaluation.INTENT_ACCEPTANCE_SHA256,
                SfBl002ScenarioEffectivenessEvaluation.TEST_EVIDENCE_SHA256,
                RATIONALE_101, RATIONALE_102, RATIONALE_103,
                RATIONALE_103, RATIONALE_103, RATIONALE_103, RATIONALE_103, RATIONALE_103, RATIONALE_103, RATIONALE_103);
        Files.writeString(root.resolve(SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH), assignments);
        String manifest = """
                {
                  "schema_version": "software-factory.sf-bl002-artifact-manifest.v0.2",
                  "execution_id": "SF-BL-002-SCENARIO-EFFECTIVENESS-004",
                  "artifact": {
                    "path": "%s",
                    "sha256": "%s"
                  },
                  "inputs": {
                    "semantics": "%s",
                    "acceptance_manifest": "%s",
                    "test_evidence": "%s"
                  }
                }
                """.formatted(SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH,
                sha(Files.readAllBytes(root.resolve(SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH))),
                SfBl002ScenarioEffectivenessEvaluation.SEMANTICS_SHA256,
                SfBl002ScenarioEffectivenessEvaluation.ACCEPTANCE_MANIFEST_SHA256,
                SfBl002ScenarioEffectivenessEvaluation.TEST_EVIDENCE_SHA256);
        Files.writeString(root.resolve(SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_MANIFEST_PATH), manifest);
        String fixture = Files.readString(FIXTURE).replace("__ASSIGNMENTS_SHA256__", sha(Files.readAllBytes(
                root.resolve(SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH))));
        Files.writeString(root.resolve(SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_PATH), fixture);
        String mappingEvidence = """
                {
                  "schema_version": "software-factory.sf-bl002-scenario-mapping-evidence.v0.2",
                  "execution_id": "SF-BL-002-SCENARIO-EFFECTIVENESS-004",
                  "authority": "PROPOSAL_ONLY",
                  "semantic_publication_allowed": false,
                  "evaluator_inputs_accessed": false,
                  "generation_method": "SfBl002ScenarioEffectivenessRun.generate",
                  "output": {
                    "path": "%s",
                    "sha256": "%s"
                  },
                  "inputs": {
                    "validation/pkb001/artifacts/petclinic-graph-818c413.json": "%s",
                    "validation/pkb001/runtime/graphify-petclinic-live-evidence.json": "%s",
                    "validation/software-factory/sf-bl002/accepted-scenario-search-intents-002.json": "%s",
                    "validation/software-factory/sf-bl002/scenario-search-intent-acceptance-manifest-002.json": "%s",
                    "validation/software-factory/sf-bl002/test-behavior-evidence.json": "%s",
                    "validation/software-factory/sf-bl002/scenario-observation-assignments-002.json": "%s"
                  }
                }
                """.formatted(SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_PATH,
                sha(Files.readAllBytes(root.resolve(SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_PATH))),
                SfBl002ScenarioEffectivenessEvaluation.GRAPH_SHA256,
                SfBl002ScenarioEffectivenessEvaluation.GRAPHIFY_LIVE_EVIDENCE_SHA256,
                SfBl002ScenarioEffectivenessEvaluation.INTENTS_SHA256,
                SfBl002ScenarioEffectivenessEvaluation.INTENT_ACCEPTANCE_SHA256,
                SfBl002ScenarioEffectivenessEvaluation.TEST_EVIDENCE_SHA256,
                sha(Files.readAllBytes(root.resolve(SfBl002ScenarioEffectivenessEvaluation.ASSIGNMENTS_PATH))));
        Files.writeString(root.resolve(SfBl002ScenarioEffectivenessEvaluation.MAPPING_PROPOSAL_EVIDENCE_PATH),
                mappingEvidence);
    }

    private static String sha(byte[] bytes) {
        return com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader.sha256(bytes);
    }
}

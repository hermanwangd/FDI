package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.evaluation.HierarchicalForwardEvaluation;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.SfBl002RouteEffectivenessEvaluation.RatioValue;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * W2C evaluator-independent proof revalidation tests. Every run binds synthetic {@code -003}
 * producer documents and the real pinned non-evaluator inputs in a temp root; evaluator truth is
 * a test spy and the real {@code ProviderNeutralEvaluatorTruth} gold/seal inputs are never
 * opened. No EVALUATOR_TRUTH content is used: the synthetic gold below is a stand-in fixture.
 */
class SfBl002RouteEffectivenessEvaluationTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String REVISION =
            com.featuredeliveryintelligence.fdi.product.realization.evaluation.ProviderNeutralEvaluatorTruth.SOURCE_REVISION;
    private static final String TYPE = "org.springframework.samples.petclinic.";
    private static final String OWNER = TYPE + "owner.";
    private static final String VET = TYPE + "vet.";
    private static final String SYNTHETIC_GOLD_SHA =
            "1111111111111111111111111111111111111111111111111111111111111111";
    private static final String SYNTHETIC_SEAL_SHA =
            "2222222222222222222222222222222222222222222222222222222222222222";
    private static final String DIGEST = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @TempDir Path temp;

    // ---------------------------------------------------------------- happy path

    @Test void syntheticWorldRevalidatesProofRecomputesCountsAndComputesGo() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        var result = generate(root, temp.resolve("out"), new AtomicInteger());

        assertEquals("GO", result.decision());
        assertEquals(10, result.traced());
        assertEquals(10, result.traceDenominator());
        assertEquals(8, result.proposed());
        assertEquals(8, result.matched());
        assertEquals(8, result.expected());

        JsonNode report = JSON.readTree(temp.resolve("out")
                .resolve(SfBl002RouteEffectivenessEvaluation.REPORT_PATH).toFile());
        assertEquals(SfBl002RouteEffectivenessEvaluation.SCHEMA_VERSION, report.path("schema_version").asText());
        assertEquals("EVALUATOR_ONLY", report.path("authority").asText());
        assertFalse(report.path("semantic_publication_allowed").asBoolean());
        assertEquals("NOT_COMPARABLE_NO_SEALED_CROSSWALK", report.path("capability_alignment").asText());
        assertEquals(0, report.path("failed_evidence_rules").size());
        assertEquals(10, report.path("scenario_trace_coverage").path("covered").asInt());
        assertEquals(10, report.path("scenario_trace_coverage").path("denominator").asInt());
        JsonNode exact = report.path("exact_component");
        assertEquals(1.0, exact.path("precision").path("value").asDouble(), 1e-12);
        assertEquals(1.0, exact.path("recall").path("value").asDouble(), 1e-12);
        assertEquals(1.0, exact.path("f1").path("value").asDouble(), 1e-12);
        JsonNode routes = report.path("route_resolution");
        assertEquals(7, routes.path("resolved").asInt());
        assertEquals(1, routes.path("unresolved").asInt());
        assertEquals(0, routes.path("ambiguous").asInt());
        assertEquals(8, routes.path("observations").asInt());
        assertEquals(5, routes.path("handlers").asInt());
        JsonNode proof = report.path("proof_counts");
        assertEquals(7, proof.path("exact_route_handler_valid").asInt());
        assertEquals(3, proof.path("direct_reference_valid").asInt());
        assertEquals(0, proof.path("graph_trace_diagnostic").asInt());
        JsonNode mapping = report.path("mapping_counts");
        assertEquals(10, mapping.path("mapping_proposals").asInt());
        assertEquals(0, mapping.path("unresolved").asInt());
        assertTrue(mapping.path("recomputed_from_revalidated_proof").asBoolean());
        JsonNode revalidation = report.path("proof_revalidation");
        assertTrue(revalidation.path("independent").asBoolean());
        assertFalse(revalidation.path("producer_credit_flags_trusted").asBoolean());
        assertFalse(revalidation.path("producer_claim_isolation_evidence").asBoolean());
        JsonNode thresholds = report.path("threshold_results");
        assertTrue(thresholds.path("enforced").asBoolean());
        assertTrue(thresholds.path("scenario_trace_coverage").path("met").asBoolean());
        assertTrue(thresholds.path("exact_component_precision").path("met").asBoolean());
        assertTrue(thresholds.path("exact_component_recall").path("met").asBoolean());
        assertTrue(thresholds.path("exact_component_f1").path("met").asBoolean());

        JsonNode evidence = JSON.readTree(temp.resolve("out")
                .resolve(SfBl002RouteEffectivenessEvaluation.EVIDENCE_PATH).toFile());
        assertEquals(SfBl002RouteEffectivenessEvaluation.EVIDENCE_SCHEMA_VERSION,
                evidence.path("schema_version").asText());
        assertTrue(evidence.path("evaluator_opened_after_non_evaluator_seal").asBoolean());
        assertTrue(evidence.path("thresholds_enforced").asBoolean());
        assertFalse(evidence.path("go_claim_made").asBoolean());
        assertEquals("GO", evidence.path("decision").asText());
        assertFalse(evidence.path("producer_evaluator_inputs_accessed_claim").asBoolean());
        assertFalse(evidence.path("producer_claim_trusted_as_isolation_evidence").asBoolean());
        assertTrue(evidence.path("independent_proof_revalidation").asBoolean());
        assertEquals(SYNTHETIC_GOLD_SHA, evidence.path("sealed_inputs").path("synthetic-evaluator-gold").asText());
        assertEquals(SYNTHETIC_SEAL_SHA, evidence.path("sealed_inputs").path("synthetic-evaluator-seal").asText());
        assertEquals(result.reportSha256(), evidence.path("output").path("sha256").asText());
    }

    // ------------------------------------------------------- forged / cross-scenario proof

    @Test void foreignScenarioObservationReuseReceivesNoCreditAndFailsRule() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        ObjectNode proposal = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile());
        componentFor(proposal, "HYP-SCENARIO-006").put("productionIdentity", OWNER + "PetController#processCreationForm");
        replaceEvidenceRefs(componentFor(proposal, "HYP-SCENARIO-006"), "obs-create-pet");
        writeProposalWithReboundEvidence(root, proposal);

        var result = generate(root, temp.resolve("out"), new AtomicInteger());
        JsonNode report = JSON.readTree(temp.resolve("out")
                .resolve(SfBl002RouteEffectivenessEvaluation.REPORT_PATH).toFile());
        assertTrue(hasRule(report, "HYP-SCENARIO-006", "CROSS_SCENARIO_PROOF_REUSED"),
                "foreign-scenario observation reuse must be reported");
        assertTrue(hasRule(report, "HYP-SCENARIO-006", "PROPOSAL_WITHOUT_VALID_PROOF"));
        assertEquals(9, report.path("scenario_trace_coverage").path("covered").asInt());
        assertEquals(1, report.path("mapping_counts").path("unresolved").asInt());
        // the shared PetController pair stays credited through HYP-SCENARIO-005's valid proof;
        // the forged HYP-SCENARIO-006 claim adds no credit of its own
        assertEquals(8, report.path("exact_component").path("proposed").asInt());
        assertEquals(8, report.path("exact_component").path("matched").asInt());
    }

    // -------------------------------------------- exact component capability identity

    /**
     * Production-shaped regression for REMEDIATION_W2C_EXACT_COMPONENT: the sealed accepted
     * intents speak the producer HYP-CAPABILITY vocabulary while the evaluator truth speaks the
     * PET-CAP vocabulary, with no sealed crosswalk between them. Exact credit must follow the
     * provider-neutral production identity (qualified symbol, canonical revision, source path),
     * not the producer capability string, or the real sealed run credits zero matches forever.
     */
    @Test void disjointCapabilityVocabularyStillCreditsIdentityAlignedClaims() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        var result = generate(root, temp.resolve("out"), new AtomicInteger(), petVocabularyTruth());

        assertEquals(8, result.proposed());
        assertEquals(8, result.matched());
        assertEquals(8, result.expected());
        assertEquals("GO", result.decision());
        JsonNode report = JSON.readTree(temp.resolve("out")
                .resolve(SfBl002RouteEffectivenessEvaluation.REPORT_PATH).toFile());
        JsonNode exact = report.path("exact_component");
        assertEquals(1.0, exact.path("precision").path("value").asDouble(), 1e-12);
        assertEquals(1.0, exact.path("recall").path("value").asDouble(), 1e-12);
        assertEquals(1.0, exact.path("f1").path("value").asDouble(), 1e-12);
        // capability alignment remains declared not comparable; nothing is inferred
        assertEquals("NOT_COMPARABLE_NO_SEALED_CROSSWALK", report.path("capability_alignment").asText());
    }

    /** A claimed symbol absent from evaluator truth receives no exact credit. */
    @Test void symbolAbsentFromTruthReceivesNoExactCredit() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        List<HierarchicalForwardEvaluation.Expected> expected = new ArrayList<>(petVocabularyTruth().expected());
        expected.removeIf(e -> e.identity().qualifiedSymbol().endsWith("VetController#showVetList"));
        var result = generate(root, temp.resolve("out"), new AtomicInteger(),
                new HierarchicalForwardEvaluation.EvaluatorTruth(SYNTHETIC_GOLD_SHA, expected));

        assertEquals(8, result.proposed());
        assertEquals(7, result.matched());
        assertEquals(7, result.expected());
        assertEquals("GO", result.decision());
        JsonNode report = JSON.readTree(temp.resolve("out")
                .resolve(SfBl002RouteEffectivenessEvaluation.REPORT_PATH).toFile());
        JsonNode exact = report.path("exact_component");
        assertEquals(1.0, exact.path("recall").path("value").asDouble(), 1e-12);
        assertEquals(7 / 8.0, exact.path("precision").path("value").asDouble(), 1e-12);
    }

    /** Truth whose qualified symbol matches but whose source path names another type is not an exact match. */
    @Test void truthEntryWithMismatchedSourcePathReceivesNoExactCredit() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        HierarchicalForwardEvaluation.EvaluatorTruth truth = petVocabularyTruth();
        List<HierarchicalForwardEvaluation.Expected> altered = new ArrayList<>();
        for (HierarchicalForwardEvaluation.Expected entry : truth.expected()) {
            if (entry.identity().qualifiedSymbol().endsWith("OwnerController#processFindForm")) {
                altered.add(new HierarchicalForwardEvaluation.Expected(entry.capabilityId(), entry.componentRef(),
                        entry.providerNodeId(), new HierarchicalForwardEvaluation.Identity(
                                entry.identity().sourceRevision(),
                                "src/main/java/org/springframework/samples/petclinic/vet/VetController.java",
                                entry.identity().granularity(), entry.identity().containingType(),
                                entry.identity().qualifiedSymbol())));
            } else {
                altered.add(entry);
            }
        }
        var result = generate(root, temp.resolve("out"), new AtomicInteger(),
                new HierarchicalForwardEvaluation.EvaluatorTruth(SYNTHETIC_GOLD_SHA, altered));

        assertEquals(8, result.proposed());
        assertEquals(7, result.matched());
    }

    /** Truth pinned at a different canonical revision is not an exact match for this sealed run. */
    @Test void truthEntryAtDifferentRevisionReceivesNoExactCredit() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        HierarchicalForwardEvaluation.EvaluatorTruth truth = petVocabularyTruth();
        List<HierarchicalForwardEvaluation.Expected> altered = new ArrayList<>();
        for (HierarchicalForwardEvaluation.Expected entry : truth.expected()) {
            if (entry.identity().qualifiedSymbol().endsWith("OwnerController#processFindForm")) {
                altered.add(new HierarchicalForwardEvaluation.Expected(entry.capabilityId(), entry.componentRef(),
                        entry.providerNodeId(), new HierarchicalForwardEvaluation.Identity(
                                "1111111111111111111111111111111111111111",
                                entry.identity().sourcePath(), entry.identity().granularity(),
                                entry.identity().containingType(), entry.identity().qualifiedSymbol())));
            } else {
                altered.add(entry);
            }
        }
        var result = generate(root, temp.resolve("out"), new AtomicInteger(),
                new HierarchicalForwardEvaluation.EvaluatorTruth(SYNTHETIC_GOLD_SHA, altered));

        assertEquals(8, result.proposed());
        assertEquals(7, result.matched());
    }

    @Test void unrelatedEvidenceRefPairingReceivesNoCreditAndFailsRule() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        ObjectNode proposal = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile());
        // keep the vet observation but claim an unrelated owner identity
        componentFor(proposal, "HYP-SCENARIO-009").put("productionIdentity", OWNER + "OwnerController#processFindForm");
        writeProposalWithReboundEvidence(root, proposal);

        var result = generate(root, temp.resolve("out"), new AtomicInteger());
        JsonNode report = JSON.readTree(temp.resolve("out")
                .resolve(SfBl002RouteEffectivenessEvaluation.REPORT_PATH).toFile());
        assertTrue(hasRule(report, "HYP-SCENARIO-009", "ROUTE_HANDLER_MISMATCH"));
        assertTrue(hasRule(report, "HYP-SCENARIO-009", "PROPOSAL_WITHOUT_VALID_PROOF"));
        assertEquals(9, report.path("scenario_trace_coverage").path("covered").asInt());
        assertEquals(7, report.path("exact_component").path("proposed").asInt());
        assertEquals(7, result.matched());
    }

    @Test void zeroValidComponentProposalIsNotAMappingProposalAndCountsRecompute() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        ObjectNode proposal = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile());
        replaceEvidenceRefs(componentFor(proposal, "HYP-SCENARIO-003"), "obs-does-not-exist");
        writeProposalWithReboundEvidence(root, proposal);

        var result = generate(root, temp.resolve("out"), new AtomicInteger());
        JsonNode report = JSON.readTree(temp.resolve("out")
                .resolve(SfBl002RouteEffectivenessEvaluation.REPORT_PATH).toFile());
        assertTrue(hasRule(report, "HYP-SCENARIO-003", "EVIDENCE_REF_UNRESOLVED"));
        assertTrue(hasRule(report, "HYP-SCENARIO-003", "PROPOSAL_WITHOUT_VALID_PROOF"));
        assertEquals(9, report.path("scenario_trace_coverage").path("covered").asInt());
        assertEquals(7, report.path("exact_component").path("proposed").asInt());
        assertEquals(1, report.path("mapping_counts").path("unresolved").asInt());
        assertTrue(report.path("mapping_counts").path("recomputed_from_revalidated_proof").asBoolean());
    }

    // ------------------------------------------------------------- direct reference proof

    @Test void directReferenceWithFewerThanTwoBehaviorSignalsReceivesNoCredit() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        ObjectNode proposal = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile());
        ObjectNode component = componentFor(proposal, "HYP-SCENARIO-011");
        component.put("productionIdentity", TYPE + "model.BaseEntity#setId");
        replaceEvidenceRefs(component,
                "direct-test-reference:src/test/java/org/springframework/samples/petclinic/vet/VetTests.java#serialization");
        writeProposalWithReboundEvidence(root, proposal);

        var result = generate(root, temp.resolve("out"), new AtomicInteger());
        JsonNode report = JSON.readTree(temp.resolve("out")
                .resolve(SfBl002RouteEffectivenessEvaluation.REPORT_PATH).toFile());
        assertTrue(hasRule(report, "HYP-SCENARIO-011", "DIRECT_REFERENCE_INSUFFICIENT_SIGNALS"));
        assertTrue(hasRule(report, "HYP-SCENARIO-011", "PROPOSAL_WITHOUT_VALID_PROOF"));
        assertEquals(9, report.path("scenario_trace_coverage").path("covered").asInt());
        assertEquals(7, report.path("exact_component").path("proposed").asInt());
    }

    @Test void crossScenarioDirectTestMethodReuseIsReportedAndReceivesNoCredit() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        ObjectNode proposal = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile());
        ObjectNode component = componentFor(proposal, "HYP-SCENARIO-011");
        component.put("productionIdentity", OWNER + "Owner#addPet");
        replaceEvidenceRefs(component,
                "direct-test-reference:src/test/java/org/springframework/samples/petclinic/owner/OwnerTests.java#addPetDoesNotAddDuplicatePet");
        writeProposalWithReboundEvidence(root, proposal);

        var result = generate(root, temp.resolve("out"), new AtomicInteger());
        JsonNode report = JSON.readTree(temp.resolve("out")
                .resolve(SfBl002RouteEffectivenessEvaluation.REPORT_PATH).toFile());
        assertTrue(hasRule(report, "HYP-SCENARIO-011", "CROSS_SCENARIO_PROOF_REUSED"));
        assertEquals(9, report.path("scenario_trace_coverage").path("covered").asInt());
        assertEquals(7, report.path("exact_component").path("proposed").asInt());
    }

    @Test void directReferenceWithoutMechanicalSymbolReferenceReceivesNoCredit() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        ObjectNode proposal = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile());
        ObjectNode component = componentFor(proposal, "HYP-SCENARIO-011");
        component.put("productionIdentity", OWNER + "Owner#getPet");
        replaceEvidenceRefs(component,
                "direct-test-reference:src/test/java/org/springframework/samples/petclinic/owner/OwnerTests.java#addPetAddsPersistedPet");
        writeProposalWithReboundEvidence(root, proposal);

        var result = generate(root, temp.resolve("out"), new AtomicInteger());
        JsonNode report = JSON.readTree(temp.resolve("out")
                .resolve(SfBl002RouteEffectivenessEvaluation.REPORT_PATH).toFile());
        assertTrue(hasRule(report, "HYP-SCENARIO-011", "DIRECT_REFERENCE_SYMBOL_MISMATCH"));
    }

    // ------------------------------------------------------------------ reject scenarios

    @Test void rejectScenarioWithoutSameTestNegativeEvidenceReceivesNoCredit() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        // Agreement passes through the handler method name, but the happy-path test method
        // carries no negative behavior evidence.
        addGuardHandlerAndObservation(root);
        ObjectNode proposal = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile());
        ObjectNode component = componentFor(proposal, "HYP-SCENARIO-006");
        component.put("productionIdentity", OWNER + "PetController#rejectDuplicatePet");
        replaceEvidenceRefs(component, "obs-pet-guard");
        writeProposalWithReboundEvidence(root, proposal);

        var result = generate(root, temp.resolve("out"), new AtomicInteger());
        JsonNode report = JSON.readTree(temp.resolve("out")
                .resolve(SfBl002RouteEffectivenessEvaluation.REPORT_PATH).toFile());
        assertTrue(hasRule(report, "HYP-SCENARIO-006", "REJECT_WITHOUT_NEGATIVE_EVIDENCE"));
        assertTrue(hasRule(report, "HYP-SCENARIO-006", "PROPOSAL_WITHOUT_VALID_PROOF"));
        assertEquals(9, report.path("scenario_trace_coverage").path("covered").asInt());
    }

    // ------------------------------------------------------------ graph trace diagnostic

    @Test void graphTraceSupportReceivesNoFormalCredit() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        ObjectNode proposal = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile());
        ObjectNode scenario = scenarioFor(proposal, "HYP-SCENARIO-009");
        ArrayNode components = (ArrayNode) scenario.path("components");
        ObjectNode diagnostic = components.addObject();
        diagnostic.put("role", "DIAGNOSTIC").put("evidenceStrength", "GRAPH_TRACE_SUPPORT")
                .put("productionIdentity", OWNER + "OwnerRepository#findByLastNameStartingWith")
                .put("relationshipTrace", "bounded graph edge, diagnostic only");
        diagnostic.putArray("evidenceRefs").add("obs-find");
        writeProposalWithReboundEvidence(root, proposal);

        var result = generate(root, temp.resolve("out"), new AtomicInteger());
        JsonNode report = JSON.readTree(temp.resolve("out")
                .resolve(SfBl002RouteEffectivenessEvaluation.REPORT_PATH).toFile());
        assertEquals(1, report.path("proof_counts").path("graph_trace_diagnostic").asInt());
        assertEquals(8, report.path("exact_component").path("proposed").asInt());
        assertEquals("GO", report.path("decision").asText());
    }

    @Test void graphTraceOnlyScenarioIsNotAMappingProposal() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        ObjectNode proposal = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile());
        ObjectNode scenario = scenarioFor(proposal, "HYP-SCENARIO-011");
        scenario.put("outcome", "MAPPING_PROPOSAL");
        ArrayNode components = (ArrayNode) scenario.path("components");
        components.removeAll();
        ObjectNode diagnostic = components.addObject();
        diagnostic.put("role", "DIAGNOSTIC").put("evidenceStrength", "GRAPH_TRACE_SUPPORT")
                .put("productionIdentity", OWNER + "Owner#getPet")
                .put("relationshipTrace", "bounded graph edge, diagnostic only");
        diagnostic.putArray("evidenceRefs").add("obs-find");
        writeProposalWithReboundEvidence(root, proposal);

        var result = generate(root, temp.resolve("out"), new AtomicInteger());
        JsonNode report = JSON.readTree(temp.resolve("out")
                .resolve(SfBl002RouteEffectivenessEvaluation.REPORT_PATH).toFile());
        assertTrue(hasRule(report, "HYP-SCENARIO-011", "PROPOSAL_WITHOUT_VALID_PROOF"));
        assertEquals(9, report.path("scenario_trace_coverage").path("covered").asInt());
        assertEquals(7, report.path("exact_component").path("proposed").asInt());
        assertEquals(1, report.path("mapping_counts").path("unresolved").asInt());
    }

    // --------------------------- REMEDIATION_W2C_SCORING_ROUTE_SEMANTICS regressions

    /**
     * Regression for finding 1: scoring pairs come only from components whose independent
     * revalidation succeeded. A same-scenario component whose proof fails stays in the
     * failed-rule evidence but never enters the proposed or matched scoring sets.
     */
    @Test void failedSameScenarioComponentIsAbsentFromScoringSetsButKeepsFailedRuleEvidence() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        ObjectNode proposal = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile());
        ArrayNode components = (ArrayNode) scenarioFor(proposal, "HYP-SCENARIO-009").path("components");
        ObjectNode invalid = components.addObject();
        invalid.put("role", "SECONDARY").put("evidenceStrength", "EXACT_ROUTE_HANDLER")
                .put("productionIdentity", OWNER + "OwnerController#processFindForm");
        invalid.putArray("evidenceRefs").add("obs-vets");
        writeProposalWithReboundEvidence(root, proposal);

        var result = generate(root, temp.resolve("out"), new AtomicInteger());
        JsonNode report = JSON.readTree(temp.resolve("out")
                .resolve(SfBl002RouteEffectivenessEvaluation.REPORT_PATH).toFile());
        assertTrue(hasRule(report, "HYP-SCENARIO-009", "ROUTE_HANDLER_MISMATCH"),
                "failed component evidence must be preserved");
        JsonNode exact = report.path("exact_component");
        assertEquals(8, exact.path("proposed").asInt(),
                "failed component pair must not inflate the proposed scoring set");
        assertEquals(8, exact.path("matched").asInt(),
                "failed component pair must not inflate the matched scoring set");
        assertEquals(8, exact.path("expected").asInt());
        assertEquals(1.0, exact.path("precision").path("value").asDouble(), 1e-12);
        assertEquals(10, report.path("scenario_trace_coverage").path("covered").asInt());
        assertEquals("GO", report.path("decision").asText());
    }

    /**
     * Regression for finding 2: the evaluator enforces the same structural route matching as
     * the approved W2B producer-side resolver. Concrete segments bind a single placeholder
     * handler, renamed placeholders still match, literal mismatches stay unresolved, malformed
     * braces never structurally match, and multiple structural matches stay ambiguous.
     */
    @Test void routeResolutionMatchesProducerStructuralSemantics() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        ObjectNode index = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH).toFile());
        ArrayNode handlers = (ArrayNode) index.path("handlers");
        handler(handlers, "h-parity-owner", "GET", "/owners/{ownerId}",
                OWNER + "OwnerController#findOwner", "OwnerController.java:70");
        handler(handlers, "h-parity-special", "GET", "/special/exact",
                OWNER + "OwnerController#special", "OwnerController.java:71");
        handler(handlers, "h-parity-broken-close", "GET", "/broken/{id}}",
                OWNER + "OwnerController#brokenClose", "OwnerController.java:72");
        handler(handlers, "h-parity-broken-open", "GET", "/broken/}{id}",
                OWNER + "OwnerController#brokenOpen", "OwnerController.java:73");
        handler(handlers, "h-parity-pets-a", "GET", "/pets/one/{petId}",
                OWNER + "PetController#showOneA", "PetController.java:74");
        handler(handlers, "h-parity-pets-b", "GET", "/pets/one/{id}",
                OWNER + "PetController#showOneB", "PetController.java:75");
        JSON.writeValue(root.resolve(SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH).toFile(), index);

        ObjectNode observations = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH).toFile());
        ArrayNode list = (ArrayNode) observations.path("observations");
        observation(list, "obs-parity-concrete", "GET", "/owners/1", "RouteParityTests",
                "concreteSegmentBindsSinglePlaceholderHandler");
        observation(list, "obs-parity-renamed", "GET", "/owners/{id}", "RouteParityTests",
                "renamedPlaceholderStillMatches");
        observation(list, "obs-parity-count", "GET", "/owners/2/extra", "RouteParityTests",
                "segmentCountMismatchStaysUnresolved");
        observation(list, "obs-parity-literal", "GET", "/special/wrong", "RouteParityTests",
                "literalMismatchStaysUnresolved");
        observation(list, "obs-parity-malformed", "GET", "/broken/9", "RouteParityTests",
                "malformedBracesNeverStructurallyMatch");
        observation(list, "obs-parity-ambiguous", "GET", "/pets/one/7", "RouteParityTests",
                "multipleStructuralMatchesStayAmbiguous");
        JSON.writeValue(root.resolve(SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH).toFile(), observations);
        rebindEvidence(root);

        generate(root, temp.resolve("out"), new AtomicInteger());
        JsonNode report = JSON.readTree(temp.resolve("out")
                .resolve(SfBl002RouteEffectivenessEvaluation.REPORT_PATH).toFile());
        JsonNode routes = report.path("route_resolution");
        assertEquals(14, routes.path("observations").asInt());
        assertEquals(11, routes.path("handlers").asInt());
        // resolved: the five untouched POST observations plus GET /vets, the concrete
        // /owners/1 observation, and the renamed-placeholder /owners/{id} observation
        // binding the single /owners/{ownerId} handler
        assertEquals(8, routes.path("resolved").asInt());
        // unresolved: /no/such/route, the segment-count mismatch, the literal mismatch,
        // and the malformed-brace handler observation
        assertEquals(4, routes.path("unresolved").asInt());
        // ambiguous: /owners/find now binds both its literal handler and /owners/{ownerId},
        // and /pets/one/7 binds both renamed-placeholder handlers
        assertEquals(2, routes.path("ambiguous").asInt());
    }

    /**
     * The pinned producer proposal carries {@code relationshipTrace: null} on its components;
     * the optional field is read with {@code JsonNode.asText()} toleration at the proposal
     * gate, so an explicit null must not abort the run and scoring stays exactly the
     * happy-path result.
     */
    @Test void explicitNullRelationshipTraceIsToleratedAndScoringIsUnchanged() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        ObjectNode proposal = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile());
        for (JsonNode scenario : proposal.path("scenarios")) {
            for (JsonNode component : scenario.path("components")) {
                ((ObjectNode) component).putNull("relationshipTrace");
            }
        }
        writeProposalWithReboundEvidence(root, proposal);

        var result = generate(root, temp.resolve("out"), new AtomicInteger());
        assertEquals("GO", result.decision());
        assertEquals(10, result.traced());
        assertEquals(8, result.proposed());
        assertEquals(8, result.matched());
    }

    // -------------------------------------------------------- fail-closed input handling

    @Test void mutatingAnySealedInputFailsBeforeEvaluatorAccess() throws Exception {
        List<String> all = new ArrayList<>(SfBl002RouteEffectivenessEvaluation.pinnedInputs().keySet());
        all.addAll(List.of(SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH,
                SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH,
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH,
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_EVIDENCE_PATH));
        for (String path : all) {
            Path root = temp.resolve("seal-" + Integer.toHexString(path.hashCode()));
            buildRoot(root);
            if (path.equals(SfBl002RouteEffectivenessEvaluation.PROPOSAL_EVIDENCE_PATH)) {
                corruptEvidenceOutputDigest(root.resolve(path));
            } else {
                Files.writeString(root.resolve(path), "\n", StandardOpenOption.APPEND);
            }
            AtomicInteger accesses = new AtomicInteger();
            assertThrows(RuntimeContractException.class,
                    () -> generate(root, temp.resolve("out-" + Integer.toHexString(path.hashCode())), accesses),
                    "mutation must fail closed: " + path);
            assertEquals(0, accesses.get(), "evaluator opened for mutated input: " + path);
        }
    }

    private static void corruptEvidenceOutputDigest(Path file) throws Exception {
        ObjectNode doc = (ObjectNode) JSON.readTree(file.toFile());
        ((ObjectNode) doc.path("output")).put("sha256",
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
        JSON.writeValue(file.toFile(), doc);
    }

    @Test void producerEvidenceClaimingEvaluatorAccessFailsClosed() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        ObjectNode evidence = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_EVIDENCE_PATH).toFile());
        evidence.put("evaluator_inputs_accessed", true);
        JSON.writeValue(root.resolve(SfBl002RouteEffectivenessEvaluation.PROPOSAL_EVIDENCE_PATH).toFile(), evidence);
        AtomicInteger accesses = new AtomicInteger();
        assertThrows(RuntimeContractException.class, () -> generate(root, temp.resolve("out"), accesses));
        assertEquals(0, accesses.get());
    }

    @Test void unknownDocumentShapesAndUnknownInputKeysFailClosed() throws Exception {
        Map<String, DocMutation> mutations = new LinkedHashMap<>();
        mutations.put("observationKey", (root, doc) -> doc.put("resolution", "RESOLVED"));
        mutations.put("indexKey", (root, doc) -> doc.put("ambiguity", "none"));
        mutations.put("proposalKey", (root, doc) -> scenarioFor(doc, "HYP-SCENARIO-001").put("capability", "X"));
        mutations.put("evidenceKey", (root, doc) -> doc.put("notes", "synthetic"));
        mutations.put("extraInputKey", (root, doc) ->
                ((ObjectNode) doc.path("inputs")).put("validation/extra.json", DIGEST));
        mutations.put("missingInputKey", (root, doc) ->
                ((ObjectNode) doc.path("inputs")).remove(
                        SfBl002RouteEffectivenessEvaluation.GRAPHIFY_LIVE_EVIDENCE_PATH));
        mutations.put("proposalAuthority", (root, doc) -> doc.put("authority", "PRODUCT_TRUTH"));
        mutations.put("proposalPublication", (root, doc) -> doc.put("semantic_publication_allowed", true));
        for (Map.Entry<String, DocMutation> mutation : mutations.entrySet()) {
            Path root = temp.resolve("shape-" + mutation.getKey());
            buildRoot(root);
            String path = documentPathFor(mutation.getKey());
            ObjectNode doc = (ObjectNode) JSON.readTree(root.resolve(path).toFile());
            mutation.getValue().mutate(root, doc);
            JSON.writeValue(root.resolve(path).toFile(), doc);
            rebindEvidence(root);
            AtomicInteger accesses = new AtomicInteger();
            assertThrows(RuntimeContractException.class,
                    () -> generate(root, temp.resolve("out-" + mutation.getKey()), accesses), mutation.getKey());
            assertEquals(0, accesses.get(), "evaluator opened after shape mutation: " + mutation.getKey());
        }
    }

    @Test void observationsDocumentWithPinnedProducerMetadataKeysPassesIntakeAndStillFailsClosed()
            throws Exception {
        Path accepted = temp.resolve("obs-intake-pinned");
        buildRoot(accepted);
        ObjectNode pinned = (ObjectNode) JSON.readTree(accepted.resolve(
                SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH).toFile());
        pinned.put("authority", "PROPOSAL_ONLY")
                .put("semantic_publication_allowed", false)
                .put("generation_method", "SfBl002RouteEffectivenessRun.generate");
        JSON.writeValue(accepted.resolve(SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH).toFile(), pinned);
        rebindEvidence(accepted);
        assertEquals("GO", generate(accepted, temp.resolve("out-obs-intake-pinned"), new AtomicInteger()).decision());

        Path unknownKey = temp.resolve("obs-intake-unknown-key");
        buildRoot(unknownKey);
        ObjectNode withUnknown = (ObjectNode) JSON.readTree(unknownKey.resolve(
                SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH).toFile());
        withUnknown.put("authority", "PROPOSAL_ONLY")
                .put("semantic_publication_allowed", false)
                .put("generation_method", "SfBl002RouteEffectivenessRun.generate")
                .put("provenance", "synthetic");
        JSON.writeValue(unknownKey.resolve(SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH).toFile(),
                withUnknown);
        rebindEvidence(unknownKey);
        RuntimeContractException unknownFailure = assertThrows(RuntimeContractException.class,
                () -> generate(unknownKey, temp.resolve("out-obs-intake-unknown-key"), new AtomicInteger()));
        assertTrue(unknownFailure.getMessage().contains("observations document carries unknown or missing keys"),
                "unknown 9th key must fail closed at observations intake: " + unknownFailure.getMessage());

        Path missingKey = temp.resolve("obs-intake-missing-key");
        buildRoot(missingKey);
        ObjectNode withMissing = (ObjectNode) JSON.readTree(missingKey.resolve(
                SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH).toFile());
        withMissing.remove("source_revision");
        JSON.writeValue(missingKey.resolve(SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH).toFile(),
                withMissing);
        rebindEvidence(missingKey);
        RuntimeContractException missingFailure = assertThrows(RuntimeContractException.class,
                () -> generate(missingKey, temp.resolve("out-obs-intake-missing-key"), new AtomicInteger()));
        assertTrue(missingFailure.getMessage().contains("observations document carries unknown or missing keys"),
                "missing required key must fail closed at observations intake: " + missingFailure.getMessage());
    }

    @Test void routeIndexDocumentWithPinnedProducerMetadataKeysPassesIntakeAndStillFailsClosed()
            throws Exception {
        Path accepted = temp.resolve("index-intake-pinned");
        buildRoot(accepted);
        ObjectNode pinned = (ObjectNode) JSON.readTree(accepted.resolve(
                SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH).toFile());
        pinned.put("authority", "PROPOSAL_ONLY")
                .put("semantic_publication_allowed", false)
                .put("generation_method", SfBl002RouteEffectivenessEvaluation.GENERATION_METHOD);
        JSON.writeValue(accepted.resolve(SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH).toFile(), pinned);
        rebindEvidence(accepted);
        assertEquals("GO", generate(accepted, temp.resolve("out-index-intake-pinned"), new AtomicInteger()).decision());

        Path unknownKey = temp.resolve("index-intake-unknown-key");
        buildRoot(unknownKey);
        ObjectNode withUnknown = (ObjectNode) JSON.readTree(unknownKey.resolve(
                SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH).toFile());
        withUnknown.put("provenance", "synthetic");
        JSON.writeValue(unknownKey.resolve(SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH).toFile(),
                withUnknown);
        rebindEvidence(unknownKey);
        RuntimeContractException unknownFailure = assertThrows(RuntimeContractException.class,
                () -> generate(unknownKey, temp.resolve("out-index-intake-unknown-key"), new AtomicInteger()));
        assertTrue(unknownFailure.getMessage().contains("route handler index carries unknown or missing keys"),
                "unknown 8th key must fail closed at route handler index intake: " + unknownFailure.getMessage());

        Path missingKey = temp.resolve("index-intake-missing-key");
        buildRoot(missingKey);
        ObjectNode withMissing = (ObjectNode) JSON.readTree(missingKey.resolve(
                SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH).toFile());
        withMissing.remove("source_revision");
        JSON.writeValue(missingKey.resolve(SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH).toFile(),
                withMissing);
        rebindEvidence(missingKey);
        RuntimeContractException missingFailure = assertThrows(RuntimeContractException.class,
                () -> generate(missingKey, temp.resolve("out-index-intake-missing-key"), new AtomicInteger()));
        assertTrue(missingFailure.getMessage().contains("route handler index carries unknown or missing keys"),
                "missing required key must fail closed at route handler index intake: " + missingFailure.getMessage());
    }

    @Test void proposalDocumentWithPinnedProducerMetadataKeysPassesIntakeAndStillFailsClosed()
            throws Exception {
        Path accepted = temp.resolve("proposal-intake-pinned");
        buildRoot(accepted);
        ObjectNode pinned = (ObjectNode) JSON.readTree(accepted.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile());
        pinned.put("generation_method", SfBl002RouteEffectivenessEvaluation.GENERATION_METHOD);
        JSON.writeValue(accepted.resolve(SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile(), pinned);
        rebindEvidence(accepted);
        assertEquals("GO", generate(accepted, temp.resolve("out-proposal-intake-pinned"), new AtomicInteger())
                .decision());

        Path unknownKey = temp.resolve("proposal-intake-unknown-key");
        buildRoot(unknownKey);
        ObjectNode withUnknown = (ObjectNode) JSON.readTree(unknownKey.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile());
        withUnknown.put("provenance", "synthetic");
        JSON.writeValue(unknownKey.resolve(SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile(), withUnknown);
        rebindEvidence(unknownKey);
        RuntimeContractException unknownFailure = assertThrows(RuntimeContractException.class,
                () -> generate(unknownKey, temp.resolve("out-proposal-intake-unknown-key"), new AtomicInteger()));
        assertTrue(unknownFailure.getMessage().contains("proposal set carries unknown or missing keys"),
                "unknown 11th key must fail closed at proposal intake: " + unknownFailure.getMessage());

        Path missingKey = temp.resolve("proposal-intake-missing-key");
        buildRoot(missingKey);
        ObjectNode withMissing = (ObjectNode) JSON.readTree(missingKey.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile());
        withMissing.remove("source_revision");
        JSON.writeValue(missingKey.resolve(SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile(), withMissing);
        rebindEvidence(missingKey);
        RuntimeContractException missingFailure = assertThrows(RuntimeContractException.class,
                () -> generate(missingKey, temp.resolve("out-proposal-intake-missing-key"), new AtomicInteger()));
        assertTrue(missingFailure.getMessage().contains("proposal set carries unknown or missing keys"),
                "missing required key must fail closed at proposal intake: " + missingFailure.getMessage());
    }

    @Test void proposalDocumentInPinnedThreeShapeWithoutCapabilityIdPassesProposalGate() throws Exception {
        Path root = temp.resolve("proposal-pinned-shape");
        buildRoot(root);
        ObjectNode proposal = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile());
        proposal.put("schema_version", "software-factory.sf-bl002-scenario-mapping-proposal.v0.3");
        for (JsonNode scenario : proposal.path("scenarios")) {
            ((ObjectNode) scenario).remove("capabilityId");
        }
        writeProposalWithReboundEvidence(root, proposal);
        assertEquals("GO", generate(root, temp.resolve("out-proposal-pinned-shape"), new AtomicInteger())
                .decision());
    }

    @Test void proposalScenarioWithMismatchedCapabilityIdFailsClosed() throws Exception {
        Path root = temp.resolve("proposal-capability-mismatch");
        buildRoot(root);
        ObjectNode proposal = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile());
        scenarioFor(proposal, "HYP-SCENARIO-001").put("capabilityId", "HYP-CAPABILITY-999");
        writeProposalWithReboundEvidence(root, proposal);
        AtomicInteger accesses = new AtomicInteger();
        RuntimeContractException failure = assertThrows(RuntimeContractException.class,
                () -> generate(root, temp.resolve("out-proposal-capability-mismatch"), accesses));
        assertTrue(failure.getMessage().contains("proposal capability does not match accepted intent"),
                "mismatched capabilityId must fail closed at proposal gate: " + failure.getMessage());
        assertEquals(0, accesses.get(), "evaluator opened after capabilityId mismatch");
    }

    @Test void proposalDocumentWithMismatchedSchemaLiteralFailsClosed() throws Exception {
        Path root = temp.resolve("proposal-schema-mismatch");
        buildRoot(root);
        ObjectNode proposal = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile());
        proposal.put("schema_version", "software-factory.sf-bl002-scenario-component-proposal-set.v0.3");
        writeProposalWithReboundEvidence(root, proposal);
        AtomicInteger accesses = new AtomicInteger();
        RuntimeContractException failure = assertThrows(RuntimeContractException.class,
                () -> generate(root, temp.resolve("out-proposal-schema-mismatch"), accesses));
        assertTrue(failure.getMessage().contains("proposal set schema mismatch"),
                "schema literal mismatch must fail closed at proposal gate: " + failure.getMessage());
        assertEquals(0, accesses.get(), "evaluator opened after schema mismatch");
    }

    @Test void proposalEvidenceDocumentWithPinnedProducerMetadataKeysPassesIntakeAndStillFailsClosed()
            throws Exception {
        Path accepted = temp.resolve("evidence-intake-pinned");
        buildRoot(accepted);
        ObjectNode pinned = (ObjectNode) JSON.readTree(accepted.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_EVIDENCE_PATH).toFile());
        pinned.put("java_runtime", System.getProperty("java.version"))
                .put("graphify_relationship_traces",
                        "diagnostic-only; no graph-backed component credit composed");
        pinned.putObject("source_verification")
                .put("checkout_revision", REVISION)
                .put("verified_input_files", 0);
        pinned.putObject("provider_runtime")
                .put("verification_id", "pkb001-graphify-live-synthetic")
                .put("result", "EXACTLY_BOUND");
        ArrayNode outputs = pinned.putArray("outputs");
        outputs.addObject().put("path", SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH)
                .put("sha256", sha(Files.readAllBytes(accepted.resolve(
                        SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH))));
        outputs.addObject().put("path", SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH)
                .put("sha256", sha(Files.readAllBytes(accepted.resolve(
                        SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH))));
        JSON.writeValue(accepted.resolve(SfBl002RouteEffectivenessEvaluation.PROPOSAL_EVIDENCE_PATH).toFile(),
                pinned);
        rebindEvidence(accepted);
        assertEquals("GO", generate(accepted, temp.resolve("out-evidence-intake-pinned"), new AtomicInteger())
                .decision());

        Path unknownKey = temp.resolve("evidence-intake-unknown-key");
        buildRoot(unknownKey);
        ObjectNode withUnknown = (ObjectNode) JSON.readTree(unknownKey.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_EVIDENCE_PATH).toFile());
        withUnknown.put("provenance", "synthetic");
        JSON.writeValue(unknownKey.resolve(SfBl002RouteEffectivenessEvaluation.PROPOSAL_EVIDENCE_PATH).toFile(),
                withUnknown);
        rebindEvidence(unknownKey);
        RuntimeContractException unknownFailure = assertThrows(RuntimeContractException.class,
                () -> generate(unknownKey, temp.resolve("out-evidence-intake-unknown-key"), new AtomicInteger()));
        assertTrue(unknownFailure.getMessage().contains("proposal evidence carries unknown or missing keys"),
                "unknown 14th key must fail closed at proposal evidence intake: " + unknownFailure.getMessage());

        Path missingKey = temp.resolve("evidence-intake-missing-key");
        buildRoot(missingKey);
        ObjectNode withMissing = (ObjectNode) JSON.readTree(missingKey.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_EVIDENCE_PATH).toFile());
        withMissing.remove("inputs");
        JSON.writeValue(missingKey.resolve(SfBl002RouteEffectivenessEvaluation.PROPOSAL_EVIDENCE_PATH).toFile(),
                withMissing);
        RuntimeContractException missingFailure = assertThrows(RuntimeContractException.class,
                () -> generate(missingKey, temp.resolve("out-evidence-intake-missing-key"), new AtomicInteger()));
        assertTrue(missingFailure.getMessage().contains("proposal evidence carries unknown or missing keys"),
                "missing required key must fail closed at proposal evidence intake: " + missingFailure.getMessage());
    }

    @Test void proposalEvidenceBindingGeneratedArtifactsViaOutputsPassesIntake() throws Exception {
        Path root = temp.resolve("evidence-outputs-bound");
        buildRoot(root);
        ObjectNode evidence = asymmetricEvidence(root);
        writeEvidence(root, evidence);
        AtomicInteger accesses = new AtomicInteger();
        assertEquals("GO", generate(root, temp.resolve("out-evidence-outputs-bound"), accesses).decision());
    }

    @Test void proposalEvidenceTamperedBindingDigestOnAnyBoundPathFailsClosed() throws Exception {
        List<String> bound = new ArrayList<>(SfBl002RouteEffectivenessEvaluation.pinnedInputs().keySet());
        bound.add(SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH);
        bound.add(SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH);
        for (String path : bound) {
            Path root = temp.resolve("evidence-tamper-" + Integer.toHexString(path.hashCode()));
            buildRoot(root);
            ObjectNode evidence = asymmetricEvidence(root);
            if (path.equals(SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH)
                    || path.equals(SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH)) {
                for (JsonNode entry : evidence.path("outputs")) {
                    if (path.equals(entry.path("path").asText())) {
                        ((ObjectNode) entry).put("sha256", DIGEST);
                    }
                }
            } else {
                ((ObjectNode) evidence.path("inputs")).put(path, DIGEST);
            }
            writeEvidence(root, evidence);
            AtomicInteger accesses = new AtomicInteger();
            assertThrows(RuntimeContractException.class,
                    () -> generate(root, temp.resolve("out-evidence-tamper-" + Integer.toHexString(path.hashCode())),
                            accesses),
                    "tampered binding digest must fail closed: " + path);
            assertEquals(0, accesses.get(), "evaluator opened after tampered binding: " + path);
        }
    }

    @Test void proposalEvidenceAsymmetricBindingsRemainFailClosed() throws Exception {
        Path unknown = temp.resolve("evidence-asym-unknown-key");
        buildRoot(unknown);
        ObjectNode unknownEvidence = asymmetricEvidence(unknown);
        ((ObjectNode) unknownEvidence.path("inputs")).put("validation/extra.json", DIGEST);
        writeEvidence(unknown, unknownEvidence);
        AtomicInteger accesses = new AtomicInteger();
        assertThrows(RuntimeContractException.class,
                () -> generate(unknown, temp.resolve("out-evidence-asym-unknown-key"), accesses));
        assertEquals(0, accesses.get(), "evaluator opened after unknown asymmetric input key");

        Path uncovered = temp.resolve("evidence-asym-uncovered");
        buildRoot(uncovered);
        ObjectNode uncoveredEvidence = asymmetricEvidence(uncovered);
        ArrayNode outputs = (ArrayNode) uncoveredEvidence.path("outputs");
        for (int i = outputs.size() - 1; i >= 0; i--) {
            if (SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH.equals(outputs.get(i).path("path").asText())) {
                outputs.remove(i);
            }
        }
        writeEvidence(uncovered, uncoveredEvidence);
        assertThrows(RuntimeContractException.class,
                () -> generate(uncovered, temp.resolve("out-evidence-asym-uncovered"), new AtomicInteger()));

        Path nonGenerated = temp.resolve("evidence-asym-nongenerated");
        buildRoot(nonGenerated);
        ObjectNode forgedEvidence = asymmetricEvidence(nonGenerated);
        ((ArrayNode) forgedEvidence.path("outputs")).addObject()
                .put("path", SfBl002RouteEffectivenessEvaluation.TEST_EVIDENCE_PATH)
                .put("sha256", SfBl002RouteEffectivenessEvaluation.TEST_EVIDENCE_SHA256);
        writeEvidence(nonGenerated, forgedEvidence);
        assertThrows(RuntimeContractException.class,
                () -> generate(nonGenerated, temp.resolve("out-evidence-asym-nongenerated"), new AtomicInteger()));

        Path entryKey = temp.resolve("evidence-asym-entry-key");
        buildRoot(entryKey);
        ObjectNode entryKeyEvidence = asymmetricEvidence(entryKey);
        ((ObjectNode) entryKeyEvidence.path("outputs").get(0)).put("note", "synthetic");
        writeEvidence(entryKey, entryKeyEvidence);
        assertThrows(RuntimeContractException.class,
                () -> generate(entryKey, temp.resolve("out-evidence-asym-entry-key"), new AtomicInteger()));

        Path duplicate = temp.resolve("evidence-asym-duplicate");
        buildRoot(duplicate);
        ObjectNode duplicateEvidence = asymmetricEvidence(duplicate);
        JsonNode firstBinding = duplicateEvidence.path("outputs").get(0);
        ((ArrayNode) duplicateEvidence.path("outputs")).add(firstBinding.deepCopy());
        writeEvidence(duplicate, duplicateEvidence);
        assertThrows(RuntimeContractException.class,
                () -> generate(duplicate, temp.resolve("out-evidence-asym-duplicate"), new AtomicInteger()));
    }

    /**
     * The pinned producer document binds its two generated route artifacts strictly and also
     * self-documents via outputs: the proposal with a digest-carrying self-entry and the
     * evidence document itself as a bare path. Self-referential entries are envelope-pinned
     * artifacts already digest-verified elsewhere, so they are tolerated (digest-checked when
     * a sha256 is present); every non-self, non-generated binding stays fail-closed.
     */
    @Test void proposalEvidenceSelfReferentialOutputsBindingsAreToleratedAndDigestChecked()
            throws Exception {
        Path four = temp.resolve("evidence-selfref-four-entry");
        buildRoot(four);
        ObjectNode evidence = asymmetricEvidence(four);
        ArrayNode outputs = (ArrayNode) evidence.path("outputs");
        outputs.addObject().put("path", SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH)
                .put("sha256", sha(Files.readAllBytes(four.resolve(
                        SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH))));
        outputs.addObject().put("path", SfBl002RouteEffectivenessEvaluation.PROPOSAL_EVIDENCE_PATH);
        writeEvidence(four, evidence);
        AtomicInteger accesses = new AtomicInteger();
        assertEquals("GO", generate(four, temp.resolve("out-evidence-selfref-four-entry"), accesses).decision());

        Path selfMismatch = temp.resolve("evidence-selfref-mismatch");
        buildRoot(selfMismatch);
        ObjectNode mismatchEvidence = asymmetricEvidence(selfMismatch);
        ((ArrayNode) mismatchEvidence.path("outputs")).addObject()
                .put("path", SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH)
                .put("sha256", DIGEST);
        writeEvidence(selfMismatch, mismatchEvidence);
        AtomicInteger mismatchAccesses = new AtomicInteger();
        RuntimeContractException mismatchFailure = assertThrows(RuntimeContractException.class,
                () -> generate(selfMismatch, temp.resolve("out-evidence-selfref-mismatch"), mismatchAccesses));
        assertTrue(mismatchFailure.getMessage().contains("proposal evidence outputs digest mismatch"),
                "self-entry digest mismatch must fail closed: " + mismatchFailure.getMessage());
        assertEquals(0, mismatchAccesses.get(), "evaluator opened after self-entry digest mismatch");

        Path duplicate = temp.resolve("evidence-selfref-duplicate");
        buildRoot(duplicate);
        ObjectNode duplicateEvidence = asymmetricEvidence(duplicate);
        JsonNode selfEntry = ((ArrayNode) duplicateEvidence.path("outputs")).addObject()
                .put("path", SfBl002RouteEffectivenessEvaluation.PROPOSAL_EVIDENCE_PATH);
        ((ArrayNode) duplicateEvidence.path("outputs")).add(selfEntry.deepCopy());
        writeEvidence(duplicate, duplicateEvidence);
        assertThrows(RuntimeContractException.class,
                () -> generate(duplicate, temp.resolve("out-evidence-selfref-duplicate"), new AtomicInteger()));

        Path entryKey = temp.resolve("evidence-selfref-entry-key");
        buildRoot(entryKey);
        ObjectNode entryKeyEvidence = asymmetricEvidence(entryKey);
        ((ArrayNode) entryKeyEvidence.path("outputs")).addObject()
                .put("path", SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH)
                .put("sha256", sha(Files.readAllBytes(entryKey.resolve(
                        SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH))))
                .put("note", "synthetic");
        writeEvidence(entryKey, entryKeyEvidence);
        assertThrows(RuntimeContractException.class,
                () -> generate(entryKey, temp.resolve("out-evidence-selfref-entry-key"), new AtomicInteger()));
    }

    /** Drops the two generated route artifacts from the evidence inputs map (generator perspective). */
    private static ObjectNode asymmetricEvidence(Path root) throws Exception {
        ObjectNode evidence = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_EVIDENCE_PATH).toFile());
        ObjectNode inputs = (ObjectNode) evidence.path("inputs");
        inputs.remove(SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH);
        inputs.remove(SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH);
        return evidence;
    }

    private static void writeEvidence(Path root, ObjectNode evidence) throws Exception {
        JSON.writeValue(root.resolve(SfBl002RouteEffectivenessEvaluation.PROPOSAL_EVIDENCE_PATH).toFile(),
                evidence);
    }

    @Test void evaluatorVocabularyInKeysAndValuesFailsClosed() throws Exception {
        Map<String, DocMutation> mutations = new LinkedHashMap<>();
        mutations.put("keyVocabulary", (root, doc) -> doc.put("evaluator_gold_hint", "forged"));
        mutations.put("roleValueVocabulary", (root, doc) ->
                componentFor(doc, "HYP-SCENARIO-001").put("role", "copied from gold mapping notes"));
        mutations.put("gapValueVocabulary", (root, doc) ->
                ((ArrayNode) scenarioFor(doc, "HYP-SCENARIO-002").path("gaps"))
                        .add("matches expected mapping per reviewer notes"));
        mutations.put("evidenceKeyVocabulary", (root, doc) -> doc.putObject("ground_truth_note"));
        for (Map.Entry<String, DocMutation> mutation : mutations.entrySet()) {
            Path root = temp.resolve("vocab-" + mutation.getKey());
            buildRoot(root);
            String path = documentPathFor(mutation.getKey());
            ObjectNode doc = (ObjectNode) JSON.readTree(root.resolve(path).toFile());
            mutation.getValue().mutate(root, doc);
            JSON.writeValue(root.resolve(path).toFile(), doc);
            rebindEvidence(root);
            AtomicInteger accesses = new AtomicInteger();
            assertThrows(RuntimeContractException.class,
                    () -> generate(root, temp.resolve("out-" + mutation.getKey()), accesses), mutation.getKey());
            assertEquals(0, accesses.get(), "evaluator opened after vocabulary injection: " + mutation.getKey());
        }
    }

    private static String documentPathFor(String mutation) {
        if (mutation.startsWith("observation") || mutation.startsWith("keyVocabulary")) {
            return SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH;
        }
        if (mutation.startsWith("index")) return SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH;
        if (mutation.startsWith("evidence")) return SfBl002RouteEffectivenessEvaluation.PROPOSAL_EVIDENCE_PATH;
        return SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH;
    }

    // --------------------------------------------------------------- thresholds / f1

    @Test void thresholdDirectionsAreStrictAndUndefinedRatiosFailTheirGate() {
        RatioValue full = new RatioValue(true, 1.0);
        assertEquals("GO", SfBl002RouteEffectivenessEvaluation.decision(
                new RatioValue(true, 0.6), new RatioValue(true, 0.70),
                new RatioValue(true, 0.0833333334), new RatioValue(true, 0.1290322582)));
        // boundary equality passes the inclusive gates
        assertEquals("GO", SfBl002RouteEffectivenessEvaluation.decision(
                new RatioValue(true, 0.6), new RatioValue(true, 0.70), full, full));
        // boundary equality fails the exclusive gates
        assertEquals("REVISE", SfBl002RouteEffectivenessEvaluation.decision(
                full, full, new RatioValue(true, 0.0833333333), full));
        assertEquals("REVISE", SfBl002RouteEffectivenessEvaluation.decision(
                full, full, full, new RatioValue(true, 0.1290322581)));
        // just below the inclusive gates
        assertEquals("REVISE", SfBl002RouteEffectivenessEvaluation.decision(
                new RatioValue(true, 0.5999999999), new RatioValue(true, 0.70), full, full));
        assertEquals("REVISE", SfBl002RouteEffectivenessEvaluation.decision(
                full, new RatioValue(true, 0.6999999999), full, full));
        // undefined ratios fail closed
        assertEquals("REVISE", SfBl002RouteEffectivenessEvaluation.decision(
                new RatioValue(false, null), full, full, full));
        assertEquals("REVISE", SfBl002RouteEffectivenessEvaluation.decision(
                full, new RatioValue(false, null), full, full));
        assertEquals("REVISE", SfBl002RouteEffectivenessEvaluation.decision(
                full, full, new RatioValue(false, null), full));
        assertEquals("REVISE", SfBl002RouteEffectivenessEvaluation.decision(
                full, full, full, new RatioValue(false, null)));
    }

    @Test void exactF1FollowsDefinedRatioRules() {
        var one = new RatioValue(true, 1.0);
        var half = new RatioValue(true, 0.5);
        var zero = new RatioValue(true, 0.0);
        var undefined = new RatioValue(false, null);
        var value = SfBl002RouteEffectivenessEvaluation.f1(one, half);
        assertTrue(value.defined());
        assertEquals(2 * 1.0 * 0.5 / 1.5, value.value(), 1e-12);
        assertFalse(SfBl002RouteEffectivenessEvaluation.f1(undefined, one).defined());
        assertFalse(SfBl002RouteEffectivenessEvaluation.f1(one, undefined).defined());
        assertFalse(SfBl002RouteEffectivenessEvaluation.f1(zero, zero).defined());
    }

    // ------------------------------------------------------- determinism and collision

    @Test void doubleRunIntoSeparateRootsIsByteIdentical() throws Exception {
        Path first = temp.resolve("first");
        Path second = temp.resolve("second");
        buildRoot(temp.resolve("root-one"));
        buildRoot(temp.resolve("root-two"));
        var one = generate(temp.resolve("root-one"), first, new AtomicInteger());
        var two = generate(temp.resolve("root-two"), second, new AtomicInteger());
        assertEquals(one.reportSha256(), two.reportSha256());
        assertEquals(one.evidenceSha256(), two.evidenceSha256());
        assertArrayEquals(Files.readAllBytes(first.resolve(SfBl002RouteEffectivenessEvaluation.REPORT_PATH)),
                Files.readAllBytes(second.resolve(SfBl002RouteEffectivenessEvaluation.REPORT_PATH)));
        assertArrayEquals(Files.readAllBytes(first.resolve(SfBl002RouteEffectivenessEvaluation.EVIDENCE_PATH)),
                Files.readAllBytes(second.resolve(SfBl002RouteEffectivenessEvaluation.EVIDENCE_PATH)));
    }

    @Test void existingIdenticalOutputsAreReusedButChangedBytesFailClosed() throws Exception {
        buildRoot(temp.resolve("root"));
        Path output = temp.resolve("out");
        var first = generate(temp.resolve("root"), output, new AtomicInteger());
        var second = generate(temp.resolve("root"), output, new AtomicInteger());
        assertEquals(first.reportSha256(), second.reportSha256());
        Files.writeString(output.resolve(SfBl002RouteEffectivenessEvaluation.REPORT_PATH),
                "\n", StandardOpenOption.APPEND);
        assertThrows(RuntimeContractException.class,
                () -> generate(temp.resolve("root"), output, new AtomicInteger()));
    }

    @Test void evaluatorTruthOpensExactlyOnceAfterNonEvaluatorSeal() throws Exception {
        Path root = temp.resolve("root");
        buildRoot(root);
        AtomicInteger accesses = new AtomicInteger();
        var result = generate(root, temp.resolve("out"), accesses);
        assertEquals(1, accesses.get());
        JsonNode evidence = JSON.readTree(temp.resolve("out")
                .resolve(SfBl002RouteEffectivenessEvaluation.EVIDENCE_PATH).toFile());
        assertEquals(SYNTHETIC_GOLD_SHA, evidence.path("sealed_inputs").path("synthetic-evaluator-gold").asText());
    }

    // ------------------------------------------------------------------ fixture wiring

    private SfBl002RouteEffectivenessEvaluation.Result generate(Path root, Path output, AtomicInteger accesses) {
        return generate(root, output, accesses, syntheticTruth());
    }

    private SfBl002RouteEffectivenessEvaluation.Result generate(Path root, Path output,
            AtomicInteger accesses, HierarchicalForwardEvaluation.EvaluatorTruth truth) {
        return SfBl002RouteEffectivenessEvaluation.generate(root, output, rootPath -> {
            accesses.incrementAndGet();
            return truth;
        }, SYNTHETIC_SEAL_SHA, "synthetic-evaluator-gold", "synthetic-evaluator-seal");
    }

    private static HierarchicalForwardEvaluation.EvaluatorTruth syntheticTruth() {
        List<HierarchicalForwardEvaluation.Expected> expected = List.of(
                expected("HYP-CAPABILITY-001", "OwnerController.java", OWNER + "OwnerController#processFindForm"),
                expected("HYP-CAPABILITY-001", "OwnerRepository.java",
                        OWNER + "OwnerRepository#findByLastNameStartingWith"),
                expected("HYP-CAPABILITY-002", "OwnerController.java", OWNER + "OwnerController#processCreationForm"),
                expected("HYP-CAPABILITY-002", "OwnerRepository.java", OWNER + "OwnerRepository#findById"),
                expected("HYP-CAPABILITY-003", "PetController.java", OWNER + "PetController#processCreationForm"),
                expected("HYP-CAPABILITY-003", "Owner.java", OWNER + "Owner#getPet"),
                expected("HYP-CAPABILITY-004", "VisitController.java", OWNER + "VisitController#processNewVisitForm"),
                expected("HYP-CAPABILITY-005", "VetController.java", VET + "VetController#showVetList"));
        return new HierarchicalForwardEvaluation.EvaluatorTruth(SYNTHETIC_GOLD_SHA, expected);
    }

    /**
     * Same identities as {@link #syntheticTruth()} but scoped under the evaluator-side PET-CAP
     * vocabulary, mirroring the production truth where the sealed accepted intents speak
     * HYP-CAPABILITY and the gold mappings speak PET-CAP with no sealed crosswalk.
     */
    private static HierarchicalForwardEvaluation.EvaluatorTruth petVocabularyTruth() {
        List<HierarchicalForwardEvaluation.Expected> expected = List.of(
                expected("PET-CAP-01", "OwnerController.java", OWNER + "OwnerController#processFindForm"),
                expected("PET-CAP-01", "OwnerRepository.java",
                        OWNER + "OwnerRepository#findByLastNameStartingWith"),
                expected("PET-CAP-02", "OwnerController.java", OWNER + "OwnerController#processCreationForm"),
                expected("PET-CAP-02", "OwnerRepository.java", OWNER + "OwnerRepository#findById"),
                expected("PET-CAP-03", "PetController.java", OWNER + "PetController#processCreationForm"),
                expected("PET-CAP-03", "Owner.java", OWNER + "Owner#getPet"),
                expected("PET-CAP-04", "VisitController.java", OWNER + "VisitController#processNewVisitForm"),
                expected("PET-CAP-10", "VetController.java", VET + "VetController#showVetList"));
        return new HierarchicalForwardEvaluation.EvaluatorTruth(SYNTHETIC_GOLD_SHA, expected);
    }

    private static HierarchicalForwardEvaluation.Expected expected(String capability, String file, String symbol) {
        String type = symbol.substring(0, symbol.indexOf('#'));
        return new HierarchicalForwardEvaluation.Expected(capability, "REF-" + symbol.hashCode(), "node-" + symbol,
                new HierarchicalForwardEvaluation.Identity(REVISION,
                        "src/main/java/org/springframework/samples/petclinic/owner/" + file, "METHOD", type, symbol));
    }

    private static boolean hasRule(JsonNode report, String scenarioId, String rule) {
        for (JsonNode node : report.path("failed_evidence_rules")) {
            if (scenarioId.equals(node.path("scenarioId").asText()) && rule.equals(node.path("rule").asText())) {
                return true;
            }
        }
        return false;
    }

    private static ObjectNode scenarioFor(ObjectNode proposal, String scenarioId) {
        for (JsonNode scenario : proposal.path("scenarios")) {
            if (scenarioId.equals(scenario.path("scenarioId").asText())) return (ObjectNode) scenario;
        }
        throw new IllegalArgumentException("no synthetic scenario " + scenarioId);
    }

    private static ObjectNode componentFor(ObjectNode proposal, String scenarioId) {
        JsonNode components = scenarioFor(proposal, scenarioId).path("components");
        return (ObjectNode) components.get(0);
    }

    private static void replaceEvidenceRefs(ObjectNode component, String... evidenceRefs) {
        ArrayNode refs = (ArrayNode) component.path("evidenceRefs");
        refs.removeAll();
        List<String> ordered = new ArrayList<>(List.of(evidenceRefs));
        ordered.sort(null);
        ordered.forEach(refs::add);
    }

    private void writeProposalWithReboundEvidence(Path root, ObjectNode proposal) throws Exception {
        JSON.writeValue(root.resolve(SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile(), proposal);
        rebindEvidence(root);
    }

    private static void rebindEvidence(Path root) throws Exception {
        List<String> generated = List.of(SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH,
                SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH);
        ObjectNode proposal = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile());
        for (String path : generated) {
            ((ObjectNode) proposal.path("inputs")).put(path, sha(Files.readAllBytes(root.resolve(path))));
        }
        JSON.writeValue(root.resolve(SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile(), proposal);
        ObjectNode evidence = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_EVIDENCE_PATH).toFile());
        ObjectNode inputs = (ObjectNode) evidence.path("inputs");
        for (String path : generated) {
            inputs.put(path, sha(Files.readAllBytes(root.resolve(path))));
        }
        for (JsonNode entry : evidence.path("outputs")) {
            String path = entry.path("path").asText();
            if (generated.contains(path)) {
                ((ObjectNode) entry).put("sha256", sha(Files.readAllBytes(root.resolve(path))));
            }
        }
        ((ObjectNode) evidence.path("output")).put("sha256", sha(Files.readAllBytes(root.resolve(
                SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH))));
        JSON.writeValue(root.resolve(SfBl002RouteEffectivenessEvaluation.PROPOSAL_EVIDENCE_PATH).toFile(), evidence);
    }

    /** Adds a synthetic guard handler plus a happy-path observation bound to it. */
    private static void addGuardHandlerAndObservation(Path root) throws Exception {
        ObjectNode index = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH).toFile());
        ObjectNode handler = ((ArrayNode) index.path("handlers")).addObject();
        handler.put("handlerRef", "h-pets-guard").put("normalizedRouteTemplate", "/owners/{ownerId}/pets/guard")
                .put("productionIdentity", OWNER + "PetController#rejectDuplicatePet")
                .put("sourceLocation", "PetController.java:99").put("sourceDigest", DIGEST);
        handler.putArray("httpMethods").add("POST");
        JSON.writeValue(root.resolve(SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH).toFile(), index);

        ObjectNode observations = (ObjectNode) JSON.readTree(root.resolve(
                SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH).toFile());
        ObjectNode observation = ((ArrayNode) observations.path("observations")).addObject();
        observation.put("observationRef", "obs-pet-guard")
                .put("testSourcePath", "src/test/java/org/springframework/samples/petclinic/owner/PetControllerTests.java")
                .put("testMethod", "processCreationFormSuccess")
                .put("sourceLocation", "PetControllerTests.java:88")
                .put("httpMethod", "POST").put("normalizedRouteTemplate", "/owners/{ownerId}/pets/guard")
                .put("extractionBasis", "MOCK_MVC_REQUEST_BUILDER");
        JSON.writeValue(root.resolve(SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH).toFile(), observations);
    }

    /** Builds a complete temp root: real pinned inputs plus synthetic {@code -003} producer documents. */
    private static void buildRoot(Path root) throws Exception {
        for (String path : SfBl002RouteEffectivenessEvaluation.pinnedInputs().keySet()) {
            Path to = root.resolve(path);
            Files.createDirectories(to.getParent());
            Files.copy(Path.of(".").resolve(path), to, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.createDirectories(root.resolve(SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH).getParent());
        JSON.writeValue(root.resolve(SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH).toFile(),
                observationsDocument());
        JSON.writeValue(root.resolve(SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH).toFile(),
                routeIndexDocument());
        JSON.writeValue(root.resolve(SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH).toFile(),
                proposalDocument(root));
        JSON.writeValue(root.resolve(SfBl002RouteEffectivenessEvaluation.PROPOSAL_EVIDENCE_PATH).toFile(),
                proposalEvidenceDocument(root));
    }

    private static ObjectNode observationsDocument() {
        ObjectNode document = JSON.createObjectNode();
        document.put("schema_version", SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_SCHEMA_VERSION)
                .put("execution_id", SfBl002RouteEffectivenessEvaluation.EXECUTION_ID)
                .put("authority", "PROPOSAL_ONLY")
                .put("semantic_publication_allowed", false)
                .put("source_revision", REVISION)
                .put("generation_method", "SfBl002RouteEffectivenessRun.generate");
        ArrayNode observations = document.putArray("observations");
        observation(observations, "obs-find", "GET", "/owners/find", "OwnerControllerTests", "processFindFormSuccess");
        observation(observations, "obs-create-owner", "POST", "/owners/new", "OwnerControllerTests",
                "processCreationFormSuccess");
        observation(observations, "obs-create-pet", "POST", "/owners/{ownerId}/pets/new", "PetControllerTests",
                "processCreationFormSuccess");
        observation(observations, "obs-reject-pet", "POST", "/owners/{ownerId}/pets/new", "PetControllerTests",
                "processCreationFormWithInvalidBirthDate");
        observation(observations, "obs-create-visit", "POST", "/owners/{ownerId}/pets/{petId}/visits/new",
                "VisitControllerTests", "processNewVisitFormSuccess");
        observation(observations, "obs-reject-visit", "POST", "/owners/{ownerId}/pets/{petId}/visits/new",
                "VisitControllerTests", "processNewVisitFormHasErrorsWhenVisitDateIsNotInFuture");
        observation(observations, "obs-vets", "GET", "/vets", "VetControllerTests", "showVetListHtml");
        observation(observations, "obs-gap", "GET", "/no/such/route", "VetControllerTests", "showResourcesVetList");
        document.putArray("gaps");
        return document;
    }

    private static void observation(ArrayNode observations, String ref, String method, String route,
            String testClass, String testMethod) {
        ObjectNode observation = observations.addObject();
        observation.put("observationRef", ref)
                .put("testSourcePath", "src/test/java/org/springframework/samples/petclinic/owner/" + testClass + ".java")
                .put("testMethod", testMethod)
                .put("sourceLocation", testClass + ".java:40")
                .put("httpMethod", method)
                .put("normalizedRouteTemplate", route)
                .put("extractionBasis", "MOCK_MVC_REQUEST_BUILDER");
    }

    private static ObjectNode routeIndexDocument() {
        ObjectNode document = JSON.createObjectNode();
        document.put("schema_version", SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_SCHEMA_VERSION)
                .put("execution_id", SfBl002RouteEffectivenessEvaluation.EXECUTION_ID)
                .put("authority", "PROPOSAL_ONLY")
                .put("semantic_publication_allowed", false)
                .put("source_revision", REVISION)
                .put("generation_method", SfBl002RouteEffectivenessEvaluation.GENERATION_METHOD);
        ArrayNode handlers = document.putArray("handlers");
        handler(handlers, "h-owners-find", "GET", "/owners/find", OWNER + "OwnerController#processFindForm",
                "OwnerController.java:42");
        handler(handlers, "h-owners-new", "POST", "/owners/new", OWNER + "OwnerController#processCreationForm",
                "OwnerController.java:61");
        handler(handlers, "h-pets-new", "POST", "/owners/{ownerId}/pets/new",
                OWNER + "PetController#processCreationForm", "PetController.java:55");
        handler(handlers, "h-visits-new", "POST", "/owners/{ownerId}/pets/{petId}/visits/new",
                OWNER + "VisitController#processNewVisitForm", "VisitController.java:58");
        handler(handlers, "h-vets", "GET", "/vets", VET + "VetController#showVetList", "VetController.java:35");
        return document;
    }

    private static void handler(ArrayNode handlers, String ref, String method, String route, String identity,
            String location) {
        ObjectNode handler = handlers.addObject();
        handler.put("handlerRef", ref).put("normalizedRouteTemplate", route)
                .put("productionIdentity", identity).put("sourceLocation", location).put("sourceDigest", DIGEST);
        handler.putArray("httpMethods").add(method);
    }

    private static ObjectNode proposalDocument(Path root) throws Exception {
        ObjectNode document = JSON.createObjectNode();
        document.put("schema_version", SfBl002RouteEffectivenessEvaluation.PROPOSAL_SCHEMA_VERSION)
                .put("execution_id", SfBl002RouteEffectivenessEvaluation.EXECUTION_ID)
                .put("authority", "PROPOSAL_ONLY")
                .put("semantic_publication_allowed", false)
                .put("source_revision", REVISION)
                .put("generation_method", SfBl002RouteEffectivenessEvaluation.GENERATION_METHOD);
        document.putArray("diagnostics");
        document.set("inputs", generationInputs(root));
        ArrayNode scenarios = document.putArray("scenarios");
        routeScenario(scenarios, "HYP-CAPABILITY-001", "HYP-SCENARIO-001", OWNER + "OwnerController#processFindForm",
                "obs-find");
        directScenario(scenarios, "HYP-CAPABILITY-001", "HYP-SCENARIO-002",
                OWNER + "OwnerRepository#findByLastNameStartingWith",
                "src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java#shouldFindOwnersByLastName");
        routeScenario(scenarios, "HYP-CAPABILITY-002", "HYP-SCENARIO-003",
                OWNER + "OwnerController#processCreationForm", "obs-create-owner");
        directScenario(scenarios, "HYP-CAPABILITY-002", "HYP-SCENARIO-004", OWNER + "OwnerRepository#findById",
                "src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java#shouldUpdateOwner");
        routeScenario(scenarios, "HYP-CAPABILITY-003", "HYP-SCENARIO-005", OWNER + "PetController#processCreationForm",
                "obs-create-pet");
        routeScenario(scenarios, "HYP-CAPABILITY-003", "HYP-SCENARIO-006", OWNER + "PetController#processCreationForm",
                "obs-reject-pet");
        routeScenario(scenarios, "HYP-CAPABILITY-004", "HYP-SCENARIO-007",
                OWNER + "VisitController#processNewVisitForm", "obs-create-visit");
        routeScenario(scenarios, "HYP-CAPABILITY-004", "HYP-SCENARIO-008",
                OWNER + "VisitController#processNewVisitForm", "obs-reject-visit");
        routeScenario(scenarios, "HYP-CAPABILITY-005", "HYP-SCENARIO-009", VET + "VetController#showVetList",
                "obs-vets");
        directScenario(scenarios, "HYP-CAPABILITY-003", "HYP-SCENARIO-011", OWNER + "Owner#getPet",
                "src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java#shouldUpdatePetName");
        return document;
    }

    private static void routeScenario(ArrayNode scenarios, String capability, String scenarioId, String identity,
            String observationRef) {
        scenario(scenarios, capability, scenarioId, "PRIMARY", "EXACT_ROUTE_HANDLER", identity, observationRef);
    }

    private static void directScenario(ArrayNode scenarios, String capability, String scenarioId, String identity,
            String testReference) {
        scenario(scenarios, capability, scenarioId, "PRIMARY", "DIRECT_PRODUCTION_REFERENCE", identity,
                "direct-test-reference:" + testReference);
    }

    private static void scenario(ArrayNode scenarios, String capability, String scenarioId, String role,
            String strength, String identity, String evidenceRef) {
        ObjectNode scenario = scenarios.addObject();
        scenario.put("capabilityId", capability).put("scenarioId", scenarioId).put("outcome", "MAPPING_PROPOSAL");
        ArrayNode components = scenario.putArray("components");
        ObjectNode component = components.addObject();
        component.put("role", role).put("evidenceStrength", strength).put("productionIdentity", identity);
        component.putArray("evidenceRefs").add(evidenceRef);
        scenario.putArray("gaps");
    }

    private static ObjectNode generationInputs(Path root) throws Exception {
        ObjectNode inputs = JSON.createObjectNode();
        inputs.put(SfBl002RouteEffectivenessEvaluation.INTENTS_PATH,
                SfBl002RouteEffectivenessEvaluation.INTENTS_SHA256);
        inputs.put(SfBl002RouteEffectivenessEvaluation.INTENT_ACCEPTANCE_PATH,
                SfBl002RouteEffectivenessEvaluation.INTENT_ACCEPTANCE_SHA256);
        inputs.put(SfBl002RouteEffectivenessEvaluation.TEST_EVIDENCE_PATH,
                SfBl002RouteEffectivenessEvaluation.TEST_EVIDENCE_SHA256);
        inputs.put(SfBl002RouteEffectivenessEvaluation.GRAPH_PATH,
                SfBl002RouteEffectivenessEvaluation.GRAPH_SHA256);
        inputs.put(SfBl002RouteEffectivenessEvaluation.GRAPHIFY_LIVE_EVIDENCE_PATH,
                SfBl002RouteEffectivenessEvaluation.GRAPHIFY_LIVE_EVIDENCE_SHA256);
        inputs.put(SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH, sha(Files.readAllBytes(root.resolve(
                SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH))));
        inputs.put(SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH, sha(Files.readAllBytes(root.resolve(
                SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH))));
        return inputs;
    }

    private static ObjectNode proposalEvidenceDocument(Path root) throws Exception {
        ObjectNode document = JSON.createObjectNode();
        document.put("schema_version", SfBl002RouteEffectivenessEvaluation.PROPOSAL_EVIDENCE_SCHEMA_VERSION)
                .put("execution_id", SfBl002RouteEffectivenessEvaluation.EXECUTION_ID)
                .put("authority", "PROPOSAL_ONLY")
                .put("semantic_publication_allowed", false)
                .put("evaluator_inputs_accessed", false)
                .put("generation_method", SfBl002RouteEffectivenessEvaluation.GENERATION_METHOD)
                .put("java_runtime", System.getProperty("java.version"))
                .put("graphify_relationship_traces",
                        "diagnostic-only; no graph-backed component credit composed");
        document.putObject("source_verification")
                .put("checkout_revision", REVISION)
                .put("verified_input_files", 0);
        document.putObject("provider_runtime")
                .put("verification_id", "pkb001-graphify-live-synthetic")
                .put("result", "EXACTLY_BOUND");
        ArrayNode outputs = document.putArray("outputs");
        outputs.addObject().put("path", SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH)
                .put("sha256", sha(Files.readAllBytes(root.resolve(
                        SfBl002RouteEffectivenessEvaluation.OBSERVATIONS_PATH))));
        outputs.addObject().put("path", SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH)
                .put("sha256", sha(Files.readAllBytes(root.resolve(
                        SfBl002RouteEffectivenessEvaluation.ROUTE_INDEX_PATH))));
        document.putObject("output").put("path", SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH)
                .put("sha256", sha(Files.readAllBytes(root.resolve(
                        SfBl002RouteEffectivenessEvaluation.PROPOSAL_PATH))));
        document.set("inputs", generationInputs(root));
        return document;
    }

    private static String sha(byte[] bytes) {
        return com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader.sha256(bytes);
    }

    private interface DocMutation {
        void mutate(Path root, ObjectNode document) throws Exception;
    }
}

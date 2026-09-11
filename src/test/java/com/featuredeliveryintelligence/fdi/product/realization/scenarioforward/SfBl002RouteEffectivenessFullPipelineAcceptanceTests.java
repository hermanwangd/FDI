package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.evaluation.HierarchicalForwardEvaluation;
import com.featuredeliveryintelligence.fdi.product.realization.evaluation.ProviderNeutralEvaluatorTruth;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * SF-BL-002-005 FULL_PIPELINE_ACCEPTANCE_TESTS (HERM-466, stage 32): current-contract
 * full-pipeline acceptance over the reviewed subject-identification candidate
 * ({@code ce8e078efd94e4f69a6e271c5e29786caf378252}) and the reviewed, byte-preserved
 * evaluator ({@code 57496334a8589699ed6029f202f5bd20d83dca79}).
 *
 * <p>The ACTUAL producer executes on an assembled synthetic repository root — fresh temp
 * roots carrying byte-copies of the five pinned non-evaluator inputs — against the
 * exact-revision Petclinic checkout {@code 818c4136ea971c21674525f9053de0d9c7ad8cfe},
 * resolving through {@code SFBL002_PETCLINIC_ROOT} (as bound in the execution envelope) or
 * a one-commit upstream fetch, and composes its complete four-document output set. The
 * unchanged output bytes then pass through the evaluator's non-evaluator sealing path and
 * the sealed evaluator comparison with synthetic truth loaded from
 * {@code src/test/resources/sfbl002-full-pipeline-acceptance/synthetic-truth.json}.
 * That fixture is injected in-process through the evaluator test seam, so it is physically
 * unavailable to the producer run; no evaluator truth under {@code validation/pkb001/evaluator/**}
 * is ever opened.
 *
 * <p>Coverage: complete four-document producer output (1); required/optional/null fields,
 * schema IDs/types, scenario/input references, and input/output digest bindings asserted
 * together with an aggregated mismatch inventory across independent negative cases before
 * any formal calibration (2, 5); current-contract decisions — supported positive mapping
 * retained, wrong operated subject and form-render GET claims demoted, REJECT PET honestly
 * UNRESOLVED, mixed valid+invalid components cannot borrow credit (3, 5); mutation
 * negatives — changed input bytes, missing references, forbidden evaluator-input-access
 * claims, output collisions, deterministic repeat in independent roots (5, 6, 7, 8).
 * Positive cases are preserved throughout: an all-UNRESOLVED producer output cannot
 * satisfy this acceptance.
 */
class SfBl002RouteEffectivenessFullPipelineAcceptanceTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String REVISION = ProviderNeutralEvaluatorTruth.SOURCE_REVISION;
    private static final Path FIXTURES = Path.of("src/test/resources/sfbl002-full-pipeline-acceptance");
    private static final String CHECKOUT_ENV = "SFBL002_PETCLINIC_ROOT";
    private static final String UPSTREAM = "https://github.com/spring-projects/spring-petclinic.git";
    private static final String TYPE = "org.springframework.samples.petclinic.";
    private static final String OWNER = TYPE + "owner.";
    private static final String VET = TYPE + "vet.";
    private static final String SF = "validation/software-factory/sf-bl002/";
    private static final List<String> GENERATED = List.of(
            SfBl002RouteEffectivenessRun.OBSERVATIONS_PATH,
            SfBl002RouteEffectivenessRun.ROUTE_INDEX_PATH,
            SfBl002RouteEffectivenessRun.PROPOSAL_PATH,
            SfBl002RouteEffectivenessRun.EVIDENCE_PATH);

    @TempDir Path temp;
    private static Path checkout;

    @BeforeAll
    static void resolveExactRevisionCheckout() throws Exception {
        String env = System.getenv(CHECKOUT_ENV);
        if (env != null && !env.isBlank()) {
            Path candidate = Path.of(env);
            assumeTrue(isExactRevisionCheckout(candidate),
                    CHECKOUT_ENV + " is not a git checkout at " + REVISION);
            checkout = candidate.toAbsolutePath().normalize();
            return;
        }
        Path fetched = Files.createTempDirectory("sfbl002-acceptance-petclinic-");
        try {
            runGit(fetched, "init", "-q");
            runGit(fetched, "remote", "add", "origin", UPSTREAM);
            runGit(fetched, "fetch", "-q", "--depth", "1", "origin", REVISION);
            runGit(fetched, "checkout", "-q", "FETCH_HEAD");
        } catch (Exception unavailable) {
            assumeTrue(false, "no exact-revision Petclinic checkout available (set " + CHECKOUT_ENV
                    + " or allow a one-commit fetch from " + UPSTREAM + "): " + unavailable.getMessage());
            return;
        }
        assumeTrue(isExactRevisionCheckout(fetched), "fetched checkout is not at " + REVISION);
        checkout = fetched;
    }

    private static boolean isExactRevisionCheckout(Path candidate) {
        try {
            Process process = new ProcessBuilder("git", "-C", candidate.toAbsolutePath().toString(),
                    "rev-parse", "HEAD").redirectErrorStream(true).start();
            String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return process.waitFor() == 0 && REVISION.equals(out);
        } catch (Exception error) {
            return false;
        }
    }

    // ---------------------------------------------------------- acceptance 1 + 2

    @Test void producerRunEmitsCompleteFourDocumentSetOverAssembledRoot() throws Exception {
        Path root = assembledRoot("producer-four-docs");
        var result = runProducer(root);

        assertEquals(10, result.scenarioCount());
        assertEquals(9, result.mappedScenarios());
        assertEquals(1, result.unresolvedScenarios());
        assertNotEquals("", result.observationsSha256());
        for (String artifact : GENERATED) {
            assertTrue(Files.isRegularFile(root.resolve(artifact)), "missing output " + artifact);
        }
        JsonNode evidence = read(root, SfBl002RouteEffectivenessRun.EVIDENCE_PATH);
        assertFalse(evidence.path("evaluator_inputs_accessed").asBoolean(),
                "generation must remain evaluator-blind");
        assertEquals(REVISION, evidence.path("source_verification").path("checkout_revision").asText());

        // The synthetic truth fixture must be physically outside the producer input path.
        Path truth = FIXTURES.resolve("synthetic-truth.json").toAbsolutePath().normalize();
        assertFalse(truth.startsWith(root.toAbsolutePath().normalize()),
                "truth fixture must not be placed on the producer input path");
        assertTrue(Files.isRegularFile(truth));
    }

    @Test void crossDocumentContractBindsFieldsReferencesAndDigestsTogether() throws Exception {
        Path root = assembledRoot("contract");
        runProducer(root);

        JsonNode intents = read(root, SfBl002RouteEffectivenessRun.INTENTS_PATH);
        JsonNode observations = read(root, SfBl002RouteEffectivenessRun.OBSERVATIONS_PATH);
        JsonNode routeIndex = read(root, SfBl002RouteEffectivenessRun.ROUTE_INDEX_PATH);
        JsonNode proposal = read(root, SfBl002RouteEffectivenessRun.PROPOSAL_PATH);
        JsonNode evidence = read(root, SfBl002RouteEffectivenessRun.EVIDENCE_PATH);
        JsonNode testEvidence = read(root, SfBl002RouteEffectivenessRun.TEST_EVIDENCE_PATH);

        // Required envelope fields and schema IDs on every generated document. The mapping
        // evidence document carries the verified checkout revision under source_verification
        // instead of a top-level source_revision.
        for (JsonNode document : List.of(observations, routeIndex, proposal)) {
            assertTrue(document.path("schema_version").asText().startsWith("software-factory.sf-bl002-"));
            assertEquals(SfBl002RouteEffectivenessRun.EXECUTION_ID, document.path("execution_id").asText());
            assertEquals("PROPOSAL_ONLY", document.path("authority").asText());
            assertFalse(document.path("semantic_publication_allowed").asBoolean());
            assertEquals(REVISION, document.path("source_revision").asText());
        }
        assertEquals(SfBl002RouteEffectivenessRun.EXECUTION_ID, evidence.path("execution_id").asText());
        assertEquals("PROPOSAL_ONLY", evidence.path("authority").asText());
        assertFalse(evidence.path("semantic_publication_allowed").asBoolean());
        assertEquals(REVISION,
                evidence.path("source_verification").path("checkout_revision").asText());
        assertEquals(SfBl002RouteEffectivenessRun.OBSERVATIONS_SCHEMA_VERSION,
                observations.path("schema_version").asText());
        assertEquals(SfBl002RouteEffectivenessRun.ROUTE_INDEX_SCHEMA_VERSION,
                routeIndex.path("schema_version").asText());
        assertEquals(SfBl002RouteEffectivenessRun.PROPOSAL_SCHEMA_VERSION,
                proposal.path("schema_version").asText());
        assertEquals(SfBl002RouteEffectivenessRun.EVIDENCE_SCHEMA_VERSION,
                evidence.path("schema_version").asText());
        assertTrue(observations.path("gaps").isArray(), "observations gaps must be an optional array");
        assertTrue(proposal.path("diagnostics").isArray());

        // Observation records: required fields, valid types, unique refs.
        Set<String> observationRefs = new LinkedHashSet<>();
        for (JsonNode observation : observations.path("observations")) {
            assertFalse(observation.path("observationRef").asText().isBlank());
            assertTrue(observationRefs.add(observation.path("observationRef").asText()));
            assertFalse(observation.path("testSourcePath").asText().isBlank());
            assertFalse(observation.path("testMethod").asText().isBlank());
            assertFalse(observation.path("httpMethod").asText().isBlank());
            assertFalse(observation.path("normalizedRouteTemplate").asText().isBlank());
            assertFalse(observation.path("sourceLocation").asText().isBlank());
        }

        // Route index records: required fields, digest shape, unique refs.
        Set<String> handlerRefs = new LinkedHashSet<>();
        for (JsonNode handler : routeIndex.path("handlers")) {
            assertTrue(handlerRefs.add(handler.path("handlerRef").asText()));
            assertFalse(handler.path("productionIdentity").asText().isBlank());
            assertTrue(handler.path("httpMethods").isArray()
                    && !handler.path("httpMethods").isEmpty());
            assertEquals(64, handler.path("sourceDigest").asText().length());
        }

        // Scenario/input references: every proposed scenario is an accepted intent; every
        // component evidenceRef resolves against the generated observations or the sealed
        // test-method index; explicit-null relationshipTrace stays tolerated.
        Set<String> intentIds = new LinkedHashSet<>();
        for (JsonNode record : intents.path("records")) intentIds.add(record.path("scenarioId").asText());
        Set<String> sealedMethodKeys = sealedMethodKeys(testEvidence);
        for (JsonNode scenario : proposal.path("scenarios")) {
            assertTrue(intentIds.contains(scenario.path("scenarioId").asText()),
                    "unaccepted scenario " + scenario.path("scenarioId").asText());
            for (JsonNode component : scenario.path("components")) {
                assertTrue(component.path("relationshipTrace").isNull(),
                        "graph relationship trace must stay diagnostic-only and null");
                for (JsonNode ref : component.path("evidenceRefs")) {
                    String evidenceRef = ref.asText();
                    boolean resolves = evidenceRef.startsWith("direct-test-reference:")
                            ? sealedMethodKeys.contains(
                                    evidenceRef.substring("direct-test-reference:".length()))
                            : observationRefs.contains(evidenceRef);
                    assertTrue(resolves, "evidenceRef does not resolve: " + evidenceRef);
                }
            }
        }

        // Input/output digest bindings across proposal, evidence, and file bytes.
        for (String path : SfBl002RouteEffectivenessEvaluation.pinnedInputs().keySet()) {
            String bound = proposal.path("inputs").path(path).asText();
            assertEquals(SfBl002RouteEffectivenessEvaluation.pinnedInputs().get(path), bound,
                    "proposal input binding diverges from the evaluator-pinned digest: " + path);
            assertEquals(sha(Files.readAllBytes(root.resolve(path))), bound,
                    "proposal input binding diverges from the root file bytes: " + path);
        }
        for (String path : List.of(SfBl002RouteEffectivenessRun.OBSERVATIONS_PATH,
                SfBl002RouteEffectivenessRun.ROUTE_INDEX_PATH)) {
            assertEquals(sha(Files.readAllBytes(root.resolve(path))),
                    proposal.path("inputs").path(path).asText(),
                    "proposal must bind " + path + " by raw-byte digest");
        }
        for (JsonNode entry : evidence.path("outputs")) {
            if (entry.hasNonNull("sha256")) {
                assertEquals(sha(Files.readAllBytes(root.resolve(entry.path("path").asText()))),
                        entry.path("sha256").asText(), "evidence output digest mismatch: "
                                + entry.path("path").asText());
            }
        }
        assertEquals(SfBl002RouteEffectivenessRun.PROPOSAL_PATH,
                evidence.path("output").path("path").asText());
        assertEquals(sha(Files.readAllBytes(root.resolve(SfBl002RouteEffectivenessRun.PROPOSAL_PATH))),
                evidence.path("output").path("sha256").asText(),
                "evidence must bind the proposal output by raw-byte digest");
        for (String path : SfBl002RouteEffectivenessEvaluation.pinnedInputs().keySet()) {
            assertEquals(proposal.path("inputs").path(path).asText(),
                    evidence.path("inputs").path(path).asText(),
                    "evidence input binding diverges from the proposal binding: " + path);
        }
        assertEquals(5, evidence.path("inputs").size(),
                "evidence inputs bind exactly the five pinned non-evaluator inputs");
    }

    // ---------------------------------------------------------- acceptance 3 + 5

    @Test void currentContractSubjectQualificationDecisionsAreRetained() throws Exception {
        Path root = assembledRoot("subject-decisions");
        runProducer(root);
        JsonNode proposal = read(root, SfBl002RouteEffectivenessRun.PROPOSAL_PATH);

        // Supported positive mapping retained.
        JsonNode find = scenario(proposal, "HYP-SCENARIO-001");
        assertEquals("MAPPING_PROPOSAL", find.path("outcome").asText());
        assertTrue(hasComponent(find, OWNER + "OwnerController#processFindForm"));

        // Wrong operated subject does not qualify: CREATE OWNER keeps the owner handler and
        // demotes the pet and visit handlers by the deterministic subject gap.
        JsonNode createOwner = scenario(proposal, "HYP-SCENARIO-003");
        assertTrue(hasComponent(createOwner, OWNER + "OwnerController#processCreationForm"));
        assertFalse(hasComponent(createOwner, OWNER + "PetController#processCreationForm"));
        assertFalse(hasComponent(createOwner, OWNER + "VisitController#processNewVisitForm"));
        assertTrue(hasGap(createOwner,
                "evidence-strength-gate:operated-subject-mismatch:"
                        + OWNER + "PetController#processCreationForm"));
        assertTrue(hasGap(createOwner,
                "evidence-strength-gate:operated-subject-mismatch:"
                        + OWNER + "VisitController#processNewVisitForm"));

        // Form-render GET never performs a mutating scenario action; the pet create handler
        // keeps its POST proof while the GET init handler is demoted.
        JsonNode createPet = scenario(proposal, "HYP-SCENARIO-005");
        assertTrue(hasComponent(createPet, OWNER + "PetController#processCreationForm"));
        assertFalse(hasComponent(createPet, OWNER + "PetController#initCreationForm"));
        assertTrue(hasGap(createPet, "evidence-strength-gate:action-http-method-conflict:"
                + OWNER + "PetController#initCreationForm"));
        assertTrue(hasGap(createPet, "evidence-strength-gate:operated-subject-mismatch:"
                + OWNER + "VisitController#processNewVisitForm"));

        // REJECT PET stays honestly UNRESOLVED; an all-UNRESOLVED run cannot satisfy acceptance.
        JsonNode rejectPet = scenario(proposal, "HYP-SCENARIO-006");
        assertEquals("UNRESOLVED", rejectPet.path("outcome").asText());
        assertEquals(0, rejectPet.path("components").size());
        int mapped = 0;
        for (JsonNode candidate : proposal.path("scenarios")) {
            if ("MAPPING_PROPOSAL".equals(candidate.path("outcome").asText())) mapped++;
        }
        assertEquals(9, mapped);
    }

    // ---------------------------------------------------------- acceptance 1 (evaluator leg)

    @Test void fullPipelineSealedEvaluatorWithSyntheticTruthComputesGo() throws Exception {
        Path root = assembledRoot("full-pipeline-go");
        runProducer(root);
        AtomicInteger truthAccesses = new AtomicInteger();
        var result = runEvaluator(root, root.resolve("out-eval"), truthAccesses);

        assertEquals("GO", result.decision());
        assertEquals(9, result.traced());
        assertEquals(10, result.traceDenominator());
        assertEquals(8, result.matched());
        assertEquals(8, result.proposed());
        assertEquals(8, result.expected());
        assertEquals(1, truthAccesses.get(),
                "evaluator truth must open exactly once, after the non-evaluator seal");

        JsonNode report = read(root.resolve("out-eval"), SfBl002RouteEffectivenessEvaluation.REPORT_PATH);
        assertEquals(SfBl002RouteEffectivenessEvaluation.SCHEMA_VERSION,
                report.path("schema_version").asText());
        assertEquals("EVALUATOR_ONLY", report.path("authority").asText());
        assertFalse(report.path("semantic_publication_allowed").asBoolean());
        assertEquals("NOT_COMPARABLE_NO_SEALED_CROSSWALK", report.path("capability_alignment").asText());
        assertEquals(0, report.path("failed_evidence_rules").size());
        assertEquals(9, report.path("scenario_trace_coverage").path("covered").asInt());
        assertEquals(10, report.path("scenario_trace_coverage").path("denominator").asInt());
        assertTrue(report.path("proof_revalidation").path("independent").asBoolean());
        assertFalse(report.path("proof_revalidation").path("producer_credit_flags_trusted").asBoolean());
        JsonNode thresholds = report.path("threshold_results");
        assertTrue(thresholds.path("enforced").asBoolean());
        assertTrue(thresholds.path("scenario_trace_coverage").path("met").asBoolean());
        assertTrue(thresholds.path("exact_component_precision").path("met").asBoolean());
        assertTrue(thresholds.path("exact_component_recall").path("met").asBoolean());
        assertTrue(thresholds.path("exact_component_f1").path("met").asBoolean());

        JsonNode evidence = read(root.resolve("out-eval"), SfBl002RouteEffectivenessEvaluation.EVIDENCE_PATH);
        assertTrue(evidence.path("evaluator_opened_after_non_evaluator_seal").asBoolean());
        assertTrue(evidence.path("thresholds_enforced").asBoolean());
        assertFalse(evidence.path("go_claim_made").asBoolean());
        assertEquals("GO", evidence.path("decision").asText());
        assertFalse(evidence.path("producer_evaluator_inputs_accessed_claim").asBoolean());
        assertFalse(evidence.path("producer_claim_trusted_as_isolation_evidence").asBoolean());
        assertTrue(evidence.path("independent_proof_revalidation").asBoolean());
        JsonNode fixture = JSON.readTree(FIXTURES.resolve("synthetic-truth.json").toFile());
        assertEquals(fixture.path("gold_sha256").asText(),
                evidence.path("sealed_inputs").path(fixture.path("gold_input_key").asText()).asText());
        assertEquals(fixture.path("seal_sha256").asText(),
                evidence.path("sealed_inputs").path(fixture.path("seal_input_key").asText()).asText());
        assertEquals(result.reportSha256(), evidence.path("output").path("sha256").asText());
    }

    // ------------------------------------------- acceptance 2 + 3 + 4 (aggregated negatives)

    @Test void negativeCasesAggregateMismatchInventoryBeforeFormalCalibration() throws Exception {
        List<String> inventory = new ArrayList<>();

        // Case 1: missing reference — a component evidenceRef that resolves to nothing
        // receives no credit and is reported; coverage recomputes honestly.
        Path missingRefRoot = assembledRoot("neg-missing-ref");
        runProducer(missingRefRoot);
        ObjectNode proposal = (ObjectNode) read(missingRefRoot, SfBl002RouteEffectivenessRun.PROPOSAL_PATH);
        ObjectNode firstComponent = (ObjectNode) scenario(proposal, "HYP-SCENARIO-001")
                .path("components").get(0);
        ArrayNode refs = (ArrayNode) firstComponent.path("evidenceRefs");
        refs.removeAll();
        refs.add("http-behavior-observation-does-not-exist");
        writeJson(missingRefRoot.resolve(SfBl002RouteEffectivenessRun.PROPOSAL_PATH), proposal);
        rebindProposalEvidence(missingRefRoot);
        var missing = runEvaluator(missingRefRoot, missingRefRoot.resolve("out"), new AtomicInteger());
        JsonNode missingReport = read(missingRefRoot.resolve("out"),
                SfBl002RouteEffectivenessEvaluation.REPORT_PATH);
        assertTrue(hasRule(missingReport, "HYP-SCENARIO-001", "EVIDENCE_REF_UNRESOLVED"));
        assertEquals(8, missing.traced());
        assertEquals(8, missing.matched());
        assertEquals(8, missing.proposed(),
                "the broken component adds no pair of its own; the shared FIND pair is still "
                        + "proposed through scenario HYP-SCENARIO-002");
        inventory.add("missing-evidence-reference:EVIDENCE_REF_UNRESOLVED:HYP-SCENARIO-001:coverage-8-of-10");

        // Case 2: mixed valid+invalid — a forged wrong-subject component pinned onto a
        // valid sibling scenario cannot borrow credit from the sibling's valid proof.
        Path forgedRoot = assembledRoot("neg-forged-mixed");
        runProducer(forgedRoot);
        ObjectNode forgedProposal = (ObjectNode) read(forgedRoot, SfBl002RouteEffectivenessRun.PROPOSAL_PATH);
        ObjectNode vetScenario = (ObjectNode) scenario(forgedProposal, "HYP-SCENARIO-009");
        JsonNode observations = read(forgedRoot, SfBl002RouteEffectivenessRun.OBSERVATIONS_PATH);
        String vetsObservation = null;
        for (JsonNode observation : observations.path("observations")) {
            if ("GET".equals(observation.path("httpMethod").asText())
                    && "/vets".equals(observation.path("normalizedRouteTemplate").asText())) {
                vetsObservation = observation.path("observationRef").asText();
            }
        }
        assertTrue(vetsObservation != null, "expected a GET /vets observation");
        ObjectNode forged = ((ArrayNode) vetScenario.path("components")).addObject();
        forged.put("role", "PRIMARY")
                .put("evidenceStrength", "EXACT_ROUTE_HANDLER")
                .put("productionIdentity", OWNER + "OwnerController#processFindForm");
        forged.putArray("evidenceRefs").add(vetsObservation);
        writeJson(forgedRoot.resolve(SfBl002RouteEffectivenessRun.PROPOSAL_PATH), forgedProposal);
        rebindProposalEvidence(forgedRoot);
        var forgedResult = runEvaluator(forgedRoot, forgedRoot.resolve("out"), new AtomicInteger());
        JsonNode forgedReport = read(forgedRoot.resolve("out"),
                SfBl002RouteEffectivenessEvaluation.REPORT_PATH);
        assertTrue(hasRule(forgedReport, "HYP-SCENARIO-009", "ROUTE_HANDLER_MISMATCH"));
        assertEquals(8, forgedResult.proposed(), "forged pair must not enter the scoring sets");
        assertEquals(8, forgedResult.matched(), "sibling valid components keep only their own credit");
        inventory.add("forged-wrong-subject-component:ROUTE_HANDLER_MISMATCH:HYP-SCENARIO-009:no-borrowed-credit");

        // Case 3: forbidden evaluator input access — a producer evidence document claiming
        // evaluator access fails the seal closed before any truth comparison.
        Path accessRoot = assembledRoot("neg-access-claim");
        runProducer(accessRoot);
        ObjectNode accessEvidence = (ObjectNode) read(accessRoot,
                SfBl002RouteEffectivenessRun.EVIDENCE_PATH);
        accessEvidence.put("evaluator_inputs_accessed", true);
        writeJson(accessRoot.resolve(SfBl002RouteEffectivenessRun.EVIDENCE_PATH), accessEvidence);
        var thrown = assertThrows(RuntimeContractException.class,
                () -> runEvaluator(accessRoot, accessRoot.resolve("out"), new AtomicInteger()));
        assertTrue(thrown.getMessage().contains("evaluator"), thrown.getMessage());
        inventory.add("evaluator-input-access-claim:seal-fails-closed:before-truth");

        // Case 4: unrelated synthetic truth entry — an expected identity that no proposed
        // component carries receives no exact credit; the unrelated entry only enlarges
        // the expected denominator.
        Path unrelatedRoot = assembledRoot("neg-unrelated-truth");
        runProducer(unrelatedRoot);
        HierarchicalForwardEvaluation.EvaluatorTruth augmented = syntheticTruth();
        List<HierarchicalForwardEvaluation.Expected> augmentedEntries =
                new ArrayList<>(augmented.expected());
        augmentedEntries.add(expected("PET-CAP-09", "src/main/java/org/springframework/samples/petclinic/vet/VetController.java",
                VET + "VetController#processFindForm"));
        var unrelated = runEvaluator(unrelatedRoot, unrelatedRoot.resolve("out"), new AtomicInteger(),
                new HierarchicalForwardEvaluation.EvaluatorTruth(augmented.goldSha256(), augmentedEntries));
        assertEquals(9, unrelated.expected());
        assertEquals(8, unrelated.matched());
        assertEquals("GO", unrelated.decision());
        inventory.add("unrelated-truth-entry:no-exact-credit:recall-8-of-9");

        // Acceptance 2: the mismatch inventory aggregates across independent cases before
        // any formal calibration attempt; every expected mismatch class is present exactly.
        assertEquals(Set.of(
                "missing-evidence-reference:EVIDENCE_REF_UNRESOLVED:HYP-SCENARIO-001:coverage-8-of-10",
                "forged-wrong-subject-component:ROUTE_HANDLER_MISMATCH:HYP-SCENARIO-009:no-borrowed-credit",
                "evaluator-input-access-claim:seal-fails-closed:before-truth",
                "unrelated-truth-entry:no-exact-credit:recall-8-of-9"), Set.copyOf(inventory),
                () -> "mismatch inventory incomplete: " + inventory);

        // Only after the inventory is aggregated does the formal calibration run.
        Path calibrationRoot = assembledRoot("calibration");
        runProducer(calibrationRoot);
        var calibration = runEvaluator(calibrationRoot, calibrationRoot.resolve("out"), new AtomicInteger());
        assertEquals("GO", calibration.decision());
    }

    // ---------------------------------------------------------- acceptance 4 (mutations)

    @Test void changedSealedInputBytesFailClosedBeforeComposition() throws Exception {
        Path root = assembledRoot("neg-input-bytes");
        Path intents = root.resolve(SfBl002RouteEffectivenessRun.INTENTS_PATH);
        byte[] bytes = Files.readAllBytes(intents);
        bytes[0] = bytes[0] == (byte) '{' ? (byte) '[' : (byte) '{';
        Files.write(intents, bytes);
        var thrown = assertThrows(RuntimeContractException.class, () -> runProducer(root));
        assertTrue(thrown.getMessage().contains("sealed input digest mismatch"), thrown.getMessage());
        assertFalse(Files.exists(root.resolve(SfBl002RouteEffectivenessRun.PROPOSAL_PATH)),
                "no artifact may be composed after a sealed input digest mismatch");
    }

    @Test void changedExistingOutputBytesFailClosedOnProducerRerun() throws Exception {
        Path root = assembledRoot("neg-output-collision");
        runProducer(root);
        Files.writeString(root.resolve(SfBl002RouteEffectivenessRun.ROUTE_INDEX_PATH), "{}");
        var thrown = assertThrows(RuntimeContractException.class, () -> runProducer(root));
        assertTrue(thrown.getMessage().contains("collision"), thrown.getMessage());
    }

    @Test void deterministicRepeatInIndependentRootsProducesIdenticalBytes() throws Exception {
        Path first = assembledRoot("repeat-first");
        Path second = assembledRoot("repeat-second");
        var firstResult = runProducer(first);
        var secondResult = runProducer(second);
        assertEquals(firstResult.proposalSha256(), secondResult.proposalSha256());
        assertEquals(firstResult.evidenceSha256(), secondResult.evidenceSha256());
        for (String artifact : GENERATED) {
            assertArrayEquals(Files.readAllBytes(first.resolve(artifact)),
                    Files.readAllBytes(second.resolve(artifact)), artifact);
        }
        var firstEval = runEvaluator(first, first.resolve("out-eval"), new AtomicInteger());
        var secondEval = runEvaluator(second, second.resolve("out-eval"), new AtomicInteger());
        assertEquals(firstEval.reportSha256(), secondEval.reportSha256());
        assertEquals(firstEval.evidenceSha256(), secondEval.evidenceSha256());
    }

    // ------------------------------------------------------------------ helpers

    /** Assembles a fresh synthetic repository root from the pinned non-evaluator inputs. */
    private Path assembledRoot(String name) throws Exception {
        Path root = temp.resolve(name);
        for (String path : SfBl002RouteEffectivenessEvaluation.pinnedInputs().keySet()) {
            Path to = root.resolve(path);
            Files.createDirectories(to.getParent());
            Files.copy(Path.of(".").resolve(path), to);
        }
        return root;
    }

    private SfBl002RouteEffectivenessRun.Result runProducer(Path root) {
        return SfBl002RouteEffectivenessRun.generate(root, checkout, root);
    }

    private SfBl002RouteEffectivenessEvaluation.Result runEvaluator(
            Path root, Path outputRoot, AtomicInteger truthAccesses) throws Exception {
        return runEvaluator(root, outputRoot, truthAccesses, syntheticTruth());
    }

    private SfBl002RouteEffectivenessEvaluation.Result runEvaluator(
            Path root, Path outputRoot, AtomicInteger truthAccesses,
            HierarchicalForwardEvaluation.EvaluatorTruth truth) {
        JsonNode fixture;
        try {
            fixture = JSON.readTree(FIXTURES.resolve("synthetic-truth.json").toFile());
        } catch (Exception error) {
            throw new IllegalStateException("cannot load synthetic truth fixture", error);
        }
        return SfBl002RouteEffectivenessEvaluation.generate(root, outputRoot, loadPath -> {
            truthAccesses.incrementAndGet();
            return truth;
        }, fixture.path("seal_sha256").asText(),
                fixture.path("gold_input_key").asText(), fixture.path("seal_input_key").asText());
    }

    private static HierarchicalForwardEvaluation.EvaluatorTruth syntheticTruth() throws Exception {
        JsonNode fixture = JSON.readTree(FIXTURES.resolve("synthetic-truth.json").toFile());
        List<HierarchicalForwardEvaluation.Expected> expected = new ArrayList<>();
        for (JsonNode entry : fixture.path("expected")) {
            JsonNode identity = entry.path("identity");
            expected.add(new HierarchicalForwardEvaluation.Expected(
                    entry.path("capabilityId").asText(),
                    entry.path("componentRef").asText(),
                    entry.path("providerNodeId").asText(),
                    new HierarchicalForwardEvaluation.Identity(
                            identity.path("sourceRevision").asText(),
                            identity.path("sourcePath").asText(),
                            identity.path("granularity").asText(),
                            identity.path("containingType").asText(),
                            identity.path("qualifiedSymbol").asText())));
        }
        return new HierarchicalForwardEvaluation.EvaluatorTruth(
                fixture.path("gold_sha256").asText(), expected);
    }

    private static HierarchicalForwardEvaluation.Expected expected(
            String capabilityId, String sourcePath, String qualifiedSymbol) {
        String containingType = qualifiedSymbol.substring(0, qualifiedSymbol.indexOf('#'));
        return new HierarchicalForwardEvaluation.Expected(capabilityId,
                "SYN-EXTRA-" + Integer.toHexString(qualifiedSymbol.hashCode()),
                "syn-extra-node-" + qualifiedSymbol,
                new HierarchicalForwardEvaluation.Identity(REVISION, sourcePath, "METHOD",
                        containingType, qualifiedSymbol));
    }

    /** Rebinds the producer evidence document after a proposal mutation, as a real producer rerun would. */
    private static void rebindProposalEvidence(Path root) throws Exception {
        byte[] proposalBytes = Files.readAllBytes(root.resolve(SfBl002RouteEffectivenessRun.PROPOSAL_PATH));
        ObjectNode evidence = (ObjectNode) read(root, SfBl002RouteEffectivenessRun.EVIDENCE_PATH);
        ((ObjectNode) evidence.path("output")).put("sha256", sha(proposalBytes));
        for (JsonNode entry : evidence.path("outputs")) {
            if (SfBl002RouteEffectivenessRun.PROPOSAL_PATH.equals(entry.path("path").asText())) {
                ((ObjectNode) entry).put("sha256", sha(proposalBytes));
            }
        }
        writeJson(root.resolve(SfBl002RouteEffectivenessRun.EVIDENCE_PATH), evidence);
    }

    private static Set<String> sealedMethodKeys(JsonNode testEvidence) {
        Set<String> keys = new LinkedHashSet<>();
        for (JsonNode file : testEvidence.path("test_files")) {
            String path = file.path("repository_relative_path").asText();
            for (JsonNode method : file.path("test_methods")) {
                keys.add(path + "#" + method.path("method_name").asText());
            }
        }
        return keys;
    }

    private static JsonNode scenario(JsonNode proposal, String scenarioId) {
        for (JsonNode scenario : proposal.path("scenarios")) {
            if (scenarioId.equals(scenario.path("scenarioId").asText())) return scenario;
        }
        throw new IllegalArgumentException("no scenario " + scenarioId);
    }

    private static boolean hasComponent(JsonNode scenario, String identity) {
        for (JsonNode component : scenario.path("components")) {
            if (identity.equals(component.path("productionIdentity").asText())) return true;
        }
        return false;
    }

    private static boolean hasGap(JsonNode scenario, String gap) {
        for (JsonNode candidate : scenario.path("gaps")) {
            if (gap.equals(candidate.asText())) return true;
        }
        return false;
    }

    private static boolean hasRule(JsonNode report, String scenarioId, String rule) {
        for (JsonNode node : report.path("failed_evidence_rules")) {
            if (scenarioId.equals(node.path("scenarioId").asText())
                    && rule.equals(node.path("rule").asText())) return true;
        }
        return false;
    }

    private static JsonNode read(Path root, String relative) throws Exception {
        return JSON.readTree(root.resolve(relative).toFile());
    }

    private static void writeJson(Path path, JsonNode document) throws Exception {
        JSON.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), document);
    }

    private static String sha(byte[] bytes) {
        return ScenarioForwardRequestReader.sha256(bytes);
    }

    private static void runGit(Path directory, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(directory.toAbsolutePath().toString());
        command.addAll(List.of(args));
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.environment().put("GIT_TERMINAL_PROMPT", "0");
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (process.waitFor() != 0) {
            throw new IllegalStateException("git " + String.join(" ", args) + " failed: " + output);
        }
    }
}

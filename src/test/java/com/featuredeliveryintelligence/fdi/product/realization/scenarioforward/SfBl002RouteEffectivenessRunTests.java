package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader;
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
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * W3_RUNNER tests for the evaluator-blind v0.3 generation runner. A synthetic
 * two-file Petclinic-style checkout lives in a temporary Git repository; the sealed
 * intents, acceptance manifest, test-behavior evidence, graph snapshot, and runtime
 * evidence are synthetic fixtures bound to that checkout's exact revision. Production
 * output paths are exercised only under temp roots; the repository evidence tree is
 * never touched. Evaluator truth is never read or synthesised.
 */
class SfBl002RouteEffectivenessRunTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CONTROLLER_PATH =
            "src/main/java/com/example/petclinic/owner/OwnerController.java";
    private static final String TEST_PATH =
            "src/test/java/com/example/petclinic/owner/OwnerControllerTests.java";
    private static final String SEMANTICS_SHA256 =
            "6c854c3d42c348d56720741b573ec88e5d6bd2dc38abb4753540ca23e8aaa9e3";
    @TempDir Path temp;

    @Test void frozenSyntheticRunWritesFourArtifactsWithRouteAndDirectProof() throws Exception {
        Path checkout = writeCheckout();
        String revision = gitRevision(checkout);
        Path root = writeSealedRoot(checkout, revision);
        Path output = temp.resolve("out");

        var result = generate(root, checkout, output);

        assertEquals(3, result.scenarioCount());
        assertEquals(3, result.mappedScenarios());
        assertEquals(0, result.unresolvedScenarios());
        assertTrue(result.resolvedRoutes() >= 3, "expected resolved route observations");
        assertTrue(result.unresolvedRoutes() >= 1, "GET /owners must stay honestly unresolved");
        assertEquals(0, result.ambiguousRoutes());

        JsonNode observations = read(output, SfBl002RouteEffectivenessRun.OBSERVATIONS_PATH);
        assertEquals("software-factory.sf-bl002-http-behavior-observations.v0.3",
                observations.path("schema_version").asText());
        assertEquals("SF-BL-002-ROUTE-EFFECTIVENESS-005", observations.path("execution_id").asText());
        assertFalse(observations.path("semantic_publication_allowed").asBoolean());
        assertEquals(revision, observations.path("source_revision").asText());
        assertTrue(observations.path("observations").size() >= 4);
        for (JsonNode observation : observations.path("observations")) {
            assertFalse(observation.path("observationRef").asText().isBlank());
            assertFalse(observation.path("normalizedRouteTemplate").asText().isBlank());
            assertFalse(observation.path("httpMethod").asText().isBlank());
        }

        JsonNode routeIndex = read(output, SfBl002RouteEffectivenessRun.ROUTE_INDEX_PATH);
        assertEquals("software-factory.sf-bl002-route-handler-index.v0.3",
                routeIndex.path("schema_version").asText());
        Set<String> identities = new TreeSet<>();
        for (JsonNode handler : routeIndex.path("handlers")) {
            identities.add(handler.path("productionIdentity").asText());
            assertEquals(64, handler.path("sourceDigest").asText().length());
        }
        assertEquals(Set.of(
                "com.example.petclinic.owner.OwnerController#initFindForm",
                "com.example.petclinic.owner.OwnerController#initCreationForm",
                "com.example.petclinic.owner.OwnerController#processCreationForm"), identities);

        JsonNode proposal = read(output, SfBl002RouteEffectivenessRun.PROPOSAL_PATH);
        assertEquals("software-factory.sf-bl002-scenario-mapping-proposal.v0.3",
                proposal.path("schema_version").asText());
        assertEquals("PROPOSAL_ONLY", proposal.path("authority").asText());
        assertFalse(proposal.path("semantic_publication_allowed").asBoolean());
        assertEquals(revision, proposal.path("source_revision").asText());
        for (Map.Entry<String, String> sealed : sealedMap(root).entrySet()) {
            assertEquals(sealed.getValue(), proposal.path("inputs").path(sealed.getKey()).asText(),
                    sealed.getKey());
        }
        assertEquals(sha(Files.readAllBytes(output.resolve(SfBl002RouteEffectivenessRun.OBSERVATIONS_PATH))),
                proposal.path("inputs").path(SfBl002RouteEffectivenessRun.OBSERVATIONS_PATH).asText(),
                "proposal must bind the observations output by raw-byte digest");
        assertEquals(sha(Files.readAllBytes(output.resolve(SfBl002RouteEffectivenessRun.ROUTE_INDEX_PATH))),
                proposal.path("inputs").path(SfBl002RouteEffectivenessRun.ROUTE_INDEX_PATH).asText(),
                "proposal must bind the route-index output by raw-byte digest");

        JsonNode find = findScenario(proposal, "HYP-SCENARIO-001");
        assertEquals("MAPPING_PROPOSAL", find.path("outcome").asText());
        JsonNode routeComponent = findComponent(find, "ROUTE_HANDLER", "EXACT_ROUTE_HANDLER",
                "com.example.petclinic.owner.OwnerController#initFindForm");
        assertFalse(routeComponent.path("evidenceRefs").isEmpty());
        assertTrue(routeComponent.path("relationshipTrace").isNull());
        JsonNode directComponent = findComponent(find, "DIRECT_REFERENCE", "DIRECT_PRODUCTION_REFERENCE",
                "com.example.petclinic.owner.OwnerRepository#findByLastNameStartingWith");
        assertFalse(directComponent.path("evidenceRefs").isEmpty());

        JsonNode create = findScenario(proposal, "HYP-SCENARIO-002");
        assertEquals("MAPPING_PROPOSAL", create.path("outcome").asText());
        findComponent(create, "ROUTE_HANDLER", "EXACT_ROUTE_HANDLER",
                "com.example.petclinic.owner.OwnerController#processCreationForm");

        JsonNode reject = findScenario(proposal, "HYP-SCENARIO-003");
        assertEquals("MAPPING_PROPOSAL", reject.path("outcome").asText());
        findComponent(reject, "ROUTE_HANDLER", "EXACT_ROUTE_HANDLER",
                "com.example.petclinic.owner.OwnerController#processCreationForm");

        JsonNode evidence = read(output, SfBl002RouteEffectivenessRun.EVIDENCE_PATH);
        assertEquals("software-factory.sf-bl002-scenario-mapping-evidence.v0.3",
                evidence.path("schema_version").asText());
        assertEquals("PROPOSAL_ONLY", evidence.path("authority").asText());
        assertFalse(evidence.path("semantic_publication_allowed").asBoolean());
        assertFalse(evidence.path("evaluator_inputs_accessed").asBoolean(),
                "generation must remain evaluator-blind");
        assertEquals(revision, evidence.path("source_verification").path("checkout_revision").asText());
        assertEquals(2, evidence.path("source_verification").path("verified_input_files").asInt());
        JsonNode outputs = evidence.path("outputs");
        assertEquals(4, outputs.size());
        assertEquals(result.proposalSha256(), outputs.get(2).path("sha256").asText());
        assertEquals(result.observationsSha256(), outputs.get(0).path("sha256").asText());
        assertEquals(result.routeIndexSha256(), outputs.get(1).path("sha256").asText());
        JsonNode proposalOutput = evidence.path("output");
        assertEquals(SfBl002RouteEffectivenessRun.PROPOSAL_PATH, proposalOutput.path("path").asText(),
                "mapping evidence must carry the evaluator-contract proposal output path");
        assertEquals(result.proposalSha256(), proposalOutput.path("sha256").asText(),
                "mapping evidence must bind the proposal output by raw-byte digest");
        assertEquals(sha(Files.readAllBytes(output.resolve(SfBl002RouteEffectivenessRun.PROPOSAL_PATH))),
                proposalOutput.path("sha256").asText(),
                "output digest must equal the SHA-256 of the run's own proposal output bytes");
        for (Map.Entry<String, String> sealed : sealedMap(root).entrySet()) {
            assertEquals(sealed.getValue(), evidence.path("inputs").path(sealed.getKey()).asText(),
                    sealed.getKey());
        }
        JsonNode runtime = evidence.path("provider_runtime");
        assertEquals("EXACTLY_BOUND", runtime.path("result").asText());
        assertEquals("graphifyy", runtime.path("runtime_identity").asText());
    }

    @Test void deterministicDoubleRunProducesIdenticalBytes() throws Exception {
        Path checkout = writeCheckout();
        String revision = gitRevision(checkout);
        Path root = writeSealedRoot(checkout, revision);
        Path first = temp.resolve("first"), second = temp.resolve("second");
        var firstResult = generate(root, checkout, first);
        var secondResult = generate(root, checkout, second);
        assertEquals(firstResult.proposalSha256(), secondResult.proposalSha256());
        for (String artifact : List.of(SfBl002RouteEffectivenessRun.OBSERVATIONS_PATH,
                SfBl002RouteEffectivenessRun.ROUTE_INDEX_PATH, SfBl002RouteEffectivenessRun.PROPOSAL_PATH,
                SfBl002RouteEffectivenessRun.EVIDENCE_PATH)) {
            assertArrayEquals(Files.readAllBytes(first.resolve(artifact)),
                    Files.readAllBytes(second.resolve(artifact)), artifact);
        }
    }

    @Test void idempotentRerunWithIdenticalBytesIsAccepted() throws Exception {
        Path checkout = writeCheckout();
        String revision = gitRevision(checkout);
        Path root = writeSealedRoot(checkout, revision);
        Path output = temp.resolve("out");
        var first = generate(root, checkout, output);
        var second = generate(root, checkout, output);
        assertEquals(first.proposalSha256(), second.proposalSha256());
        assertEquals(first.evidenceSha256(), second.evidenceSha256());
    }

    @Test void collisionWithChangedExistingBytesFailsClosed() throws Exception {
        Path checkout = writeCheckout();
        String revision = gitRevision(checkout);
        Path root = writeSealedRoot(checkout, revision);
        Path output = temp.resolve("out");
        generate(root, checkout, output);
        Files.writeString(output.resolve(SfBl002RouteEffectivenessRun.PROPOSAL_PATH), "{}");
        var thrown = assertThrows(RuntimeContractException.class,
                () -> generate(root, checkout, output));
        assertTrue(thrown.getMessage().contains("collision"), thrown.getMessage());
    }

    @Test void sealedInputDigestMismatchFailsClosedBeforeComposition() throws Exception {
        Path checkout = writeCheckout();
        String revision = gitRevision(checkout);
        Path root = writeSealedRoot(checkout, revision);
        Map<String, String> sealed = sealedMap(root);
        Files.writeString(root.resolve(SfBl002RouteEffectivenessRun.INTENTS_PATH),
                "\n", StandardOpenOption.APPEND);
        var thrown = assertThrows(RuntimeContractException.class,
                () -> SfBl002RouteEffectivenessRun.generate(root, checkout, temp.resolve("out"),
                        sealed, revision));
        assertTrue(thrown.getMessage().contains("digest"), thrown.getMessage());
    }

    @Test void checkoutNotAtBoundRevisionFailsClosed() throws Exception {
        Path checkout = writeCheckout();
        String wrongRevision = "0".repeat(40);
        Path root = writeSealedRoot(checkout, wrongRevision);
        var thrown = assertThrows(RuntimeContractException.class,
                () -> SfBl002RouteEffectivenessRun.generate(root, checkout, temp.resolve("out"),
                        sealedMap(root), wrongRevision));
        assertTrue(thrown.getMessage().contains("revision"), thrown.getMessage());
    }

    @Test void nonGitCheckoutFailsClosed() throws Exception {
        Path checkout = writeCheckout();
        String revision = gitRevision(checkout);
        Path root = writeSealedRoot(checkout, revision);
        Path notARepo = temp.resolve("not-a-repo");
        Files.createDirectories(notARepo);
        assertThrows(RuntimeContractException.class,
                () -> SfBl002RouteEffectivenessRun.generate(root, notARepo, temp.resolve("out"), sealedMap(root), revision));
    }

    @Test void consumedSourceFileDigestMismatchFailsClosed() throws Exception {
        Path checkout = writeCheckout();
        String revision = gitRevision(checkout);
        Path root = writeSealedRoot(checkout, revision);
        Files.writeString(checkout.resolve(CONTROLLER_PATH),
                "\n// tampered after sealing\n", StandardOpenOption.APPEND);
        var thrown = assertThrows(RuntimeContractException.class,
                () -> generate(root, checkout, temp.resolve("out")));
        assertTrue(thrown.getMessage().contains("digest mismatch"), thrown.getMessage());
    }

    @Test void missingConsumedSourceFileFailsClosed() throws Exception {
        Path checkout = writeCheckout();
        String revision = gitRevision(checkout);
        Path root = writeSealedRoot(checkout, revision);
        Files.delete(checkout.resolve(TEST_PATH));
        var thrown = assertThrows(RuntimeContractException.class,
                () -> generate(root, checkout, temp.resolve("out")));
        assertTrue(thrown.getMessage().contains("unavailable"), thrown.getMessage());
    }

    @Test void evaluatorVocabularyInIntentRecordFailsClosed() throws Exception {
        Path checkout = writeCheckout();
        String revision = gitRevision(checkout);
        Path root = writeSealedRoot(checkout, revision);
        ObjectNode intents = (ObjectNode) JSON.readTree(
                Files.readAllBytes(root.resolve(SfBl002RouteEffectivenessRun.INTENTS_PATH)));
        ((ObjectNode) intents.withArray("records").get(0)).put("entity", "evaluator gold mapping owner");
        writeJson(root.resolve(SfBl002RouteEffectivenessRun.INTENTS_PATH), intents);
        var thrown = assertThrows(RuntimeContractException.class,
                () -> generate(root, checkout, temp.resolve("out")));
        assertTrue(thrown.getMessage().contains("evaluator"), thrown.getMessage());
    }

    @Test void semanticPublicationRefusalViolationFailsClosed() throws Exception {
        Path checkout = writeCheckout();
        String revision = gitRevision(checkout);
        Path root = writeSealedRoot(checkout, revision);
        ObjectNode intents = (ObjectNode) JSON.readTree(
                Files.readAllBytes(root.resolve(SfBl002RouteEffectivenessRun.INTENTS_PATH)));
        intents.put("semantic_publication_allowed", true);
        writeJson(root.resolve(SfBl002RouteEffectivenessRun.INTENTS_PATH), intents);
        var thrown = assertThrows(RuntimeContractException.class,
                () -> generate(root, checkout, temp.resolve("out")));
        assertTrue(thrown.getMessage().contains("publication"), thrown.getMessage());
    }

    @Test void acceptanceRecordIdMismatchFailsClosed() throws Exception {
        Path checkout = writeCheckout();
        String revision = gitRevision(checkout);
        Path root = writeSealedRoot(checkout, revision);
        ObjectNode acceptance = (ObjectNode) JSON.readTree(
                Files.readAllBytes(root.resolve(SfBl002RouteEffectivenessRun.INTENT_ACCEPTANCE_PATH)));
        acceptance.withArray("accepted_record_ids").remove(0);
        writeJson(root.resolve(SfBl002RouteEffectivenessRun.INTENT_ACCEPTANCE_PATH), acceptance);
        var thrown = assertThrows(RuntimeContractException.class,
                () -> generate(root, checkout, temp.resolve("out")));
        assertTrue(thrown.getMessage().contains("acceptance"), thrown.getMessage());
    }

    @Test void generationNeverReadsEvaluatorTruth() throws Exception {
        Path checkout = writeCheckout();
        String revision = gitRevision(checkout);
        Path root = writeSealedRoot(checkout, revision);
        Path output = temp.resolve("out");
        generate(root, checkout, output);
        for (String artifact : List.of(SfBl002RouteEffectivenessRun.OBSERVATIONS_PATH,
                SfBl002RouteEffectivenessRun.ROUTE_INDEX_PATH, SfBl002RouteEffectivenessRun.PROPOSAL_PATH,
                SfBl002RouteEffectivenessRun.EVIDENCE_PATH)) {
            String content = Files.readString(output.resolve(artifact));
            assertFalse(content.contains("evaluator gold"), artifact);
            assertFalse(content.contains("ground-truth"), artifact);
            assertFalse(content.contains("expected-mapping"), artifact);
        }
        JsonNode evidence = read(output, SfBl002RouteEffectivenessRun.EVIDENCE_PATH);
        assertFalse(evidence.path("evaluator_inputs_accessed").asBoolean());
        Set<String> inputPaths = new TreeSet<>();
        evidence.path("inputs").properties().forEach(entry -> inputPaths.add(entry.getKey()));
        assertEquals(Set.of(
                SfBl002RouteEffectivenessRun.INTENTS_PATH,
                SfBl002RouteEffectivenessRun.INTENT_ACCEPTANCE_PATH,
                SfBl002RouteEffectivenessRun.TEST_EVIDENCE_PATH,
                SfBl002RouteEffectivenessRun.GRAPH_PATH,
                SfBl002RouteEffectivenessRun.RUNTIME_EVIDENCE_PATH), inputPaths);
    }

    @Test void proposalBindsDirectGenerationInputsByRawByteSha256() throws Exception {
        Path checkout = writeCheckout();
        String revision = gitRevision(checkout);
        Path root = writeSealedRoot(checkout, revision);
        Path output = temp.resolve("out");
        var result = generate(root, checkout, output);
        JsonNode proposal = read(output, SfBl002RouteEffectivenessRun.PROPOSAL_PATH);
        Set<String> inputPaths = new TreeSet<>();
        proposal.path("inputs").properties().forEach(entry -> inputPaths.add(entry.getKey()));
        assertEquals(Set.of(
                SfBl002RouteEffectivenessRun.INTENTS_PATH,
                SfBl002RouteEffectivenessRun.INTENT_ACCEPTANCE_PATH,
                SfBl002RouteEffectivenessRun.TEST_EVIDENCE_PATH,
                SfBl002RouteEffectivenessRun.GRAPH_PATH,
                SfBl002RouteEffectivenessRun.RUNTIME_EVIDENCE_PATH,
                SfBl002RouteEffectivenessRun.OBSERVATIONS_PATH,
                SfBl002RouteEffectivenessRun.ROUTE_INDEX_PATH), inputPaths);
        assertEquals(result.observationsSha256(), proposal.path("inputs")
                .path(SfBl002RouteEffectivenessRun.OBSERVATIONS_PATH).asText());
        assertEquals(result.routeIndexSha256(), proposal.path("inputs")
                .path(SfBl002RouteEffectivenessRun.ROUTE_INDEX_PATH).asText());
        assertEquals(sha(Files.readAllBytes(output.resolve(SfBl002RouteEffectivenessRun.OBSERVATIONS_PATH))),
                proposal.path("inputs").path(SfBl002RouteEffectivenessRun.OBSERVATIONS_PATH).asText());
        assertEquals(sha(Files.readAllBytes(output.resolve(SfBl002RouteEffectivenessRun.ROUTE_INDEX_PATH))),
                proposal.path("inputs").path(SfBl002RouteEffectivenessRun.ROUTE_INDEX_PATH).asText());
    }

    @Test void constructorDirectReferenceUsesSealedEvidenceSymbolNameVerbatim() throws Exception {
        Path checkout = writeCheckout();
        String revision = gitRevision(checkout);
        Path root = writeSealedRoot(checkout, revision);
        ObjectNode evidence = (ObjectNode) JSON.readTree(
                Files.readAllBytes(root.resolve(SfBl002RouteEffectivenessRun.TEST_EVIDENCE_PATH)));
        for (JsonNode method : evidence.path("test_files").get(0).path("test_methods")) {
            if (!"findOwnerByLastName".equals(method.path("method_name").asText())) continue;
            ((ArrayNode) method.path("actions")).insert(0, item(
                    "this.visits.save(new com.example.petclinic.owner.Visit())",
                    "com.example.petclinic.owner.Visit", "CONSTRUCTOR", "Visit",
                    "PRODUCTION_RECEIVER_SOURCE_ROOT"));
        }
        writeJson(root.resolve(SfBl002RouteEffectivenessRun.TEST_EVIDENCE_PATH), evidence);
        Path output = temp.resolve("out");

        generate(root, checkout, output);

        JsonNode proposal = read(output, SfBl002RouteEffectivenessRun.PROPOSAL_PATH);
        String proposalBytes = Files.readString(output.resolve(SfBl002RouteEffectivenessRun.PROPOSAL_PATH));
        assertFalse(proposalBytes.contains("<init>"),
                "direct references must keep the sealed evidence symbol naming verbatim");
        JsonNode find = findScenario(proposal, "HYP-SCENARIO-001");
        JsonNode constructorComponent = findComponent(find, "DIRECT_REFERENCE",
                "DIRECT_PRODUCTION_REFERENCE", "com.example.petclinic.owner.Visit#Visit");
        assertFalse(constructorComponent.path("evidenceRefs").isEmpty());
        assertEquals(List.of("direct-test-reference:" + TEST_PATH + "#findOwnerByLastName"),
                jsonList(constructorComponent.path("evidenceRefs")),
                "a direct-test-reference ref must carry the sealed test-method key "
                        + "so it resolves under the sealed test-method index");
    }

    @Test void directReferenceAbsentFromSealedEvidenceSymbolSetIsAbstained() throws Exception {
        Path checkout = writeCheckout();
        String revision = gitRevision(checkout);
        Path root = writeSealedRoot(checkout, revision);
        ObjectNode evidence = (ObjectNode) JSON.readTree(
                Files.readAllBytes(root.resolve(SfBl002RouteEffectivenessRun.TEST_EVIDENCE_PATH)));
        for (JsonNode method : evidence.path("test_files").get(0).path("test_methods")) {
            if (!"findOwnerByLastName".equals(method.path("method_name").asText())) continue;
            ObjectNode fixture = item("this.welcomeController.renderHomePage()",
                    "com.example.petclinic.system.WelcomeController", "METHOD", "renderHomePage",
                    "PRODUCTION_RECEIVER_SOURCE_ROOT");
            ((ArrayNode) method.path("fixtures")).add(fixture);
        }
        writeJson(root.resolve(SfBl002RouteEffectivenessRun.TEST_EVIDENCE_PATH), evidence);
        Path output = temp.resolve("out");

        generate(root, checkout, output);

        JsonNode proposal = read(output, SfBl002RouteEffectivenessRun.PROPOSAL_PATH);
        String proposalBytes = Files.readString(output.resolve(SfBl002RouteEffectivenessRun.PROPOSAL_PATH));
        assertFalse(proposalBytes.contains("com.example.petclinic.system.WelcomeController#renderHomePage"),
                "a direct reference not backed by the sealed evidence symbol set must be abstained, never claimed");
    }

    @Test void directReferenceEvidenceRefCarriesSealedTestMethodKey() throws Exception {
        Path checkout = writeCheckout();
        String revision = gitRevision(checkout);
        Path root = writeSealedRoot(checkout, revision);
        Path output = temp.resolve("out");

        generate(root, checkout, output);

        JsonNode proposal = read(output, SfBl002RouteEffectivenessRun.PROPOSAL_PATH);
        JsonNode find = findScenario(proposal, "HYP-SCENARIO-001");
        JsonNode directComponent = findComponent(find, "DIRECT_REFERENCE", "DIRECT_PRODUCTION_REFERENCE",
                "com.example.petclinic.owner.OwnerRepository#findByLastNameStartingWith");
        assertEquals(List.of("direct-test-reference:" + TEST_PATH + "#findOwnerByLastName"),
                jsonList(directComponent.path("evidenceRefs")),
                "every direct-test-reference suffix must be the sealed test-method key "
                        + "repository_relative_path#method_name so it resolves under the sealed index");
    }

    @Test void assertionOnlyDirectReferenceIsDemotedByEvidenceStrengthGate() throws Exception {
        Path checkout = writeCheckout();
        String revision = gitRevision(checkout);
        Path root = writeSealedRoot(checkout, revision);
        ObjectNode evidence = (ObjectNode) JSON.readTree(
                Files.readAllBytes(root.resolve(SfBl002RouteEffectivenessRun.TEST_EVIDENCE_PATH)));
        for (JsonNode method : evidence.path("test_files").get(0).path("test_methods")) {
            if (!"findOwnerByLastName".equals(method.path("method_name").asText())) continue;
            ArrayNode actions = (ArrayNode) method.path("actions");
            int symbolIndex = -1;
            for (int i = 0; i < actions.size(); i++) {
                if (actions.get(i).path("referenced_symbol").isObject()) {
                    symbolIndex = i;
                    break;
                }
            }
            JsonNode symbolItem = actions.remove(symbolIndex);
            ((ArrayNode) method.path("assertions")).add(symbolItem);
        }
        writeJson(root.resolve(SfBl002RouteEffectivenessRun.TEST_EVIDENCE_PATH), evidence);
        Path output = temp.resolve("out");

        generate(root, checkout, output);

        JsonNode proposal = read(output, SfBl002RouteEffectivenessRun.PROPOSAL_PATH);
        JsonNode find = findScenario(proposal, "HYP-SCENARIO-001");
        assertFalse(hasComponent(find, "DIRECT_REFERENCE", "DIRECT_PRODUCTION_REFERENCE",
                "com.example.petclinic.owner.OwnerRepository#findByLastNameStartingWith"),
                "a component the sealed actions do not back must never be proposed");
        assertTrue(hasGap(find, "evidence-strength-gate:symbol-not-backed-by-sealed-actions:"
                + "com.example.petclinic.owner.OwnerRepository#findByLastNameStartingWith"));
        assertEquals("MAPPING_PROPOSAL", find.path("outcome").asText(),
                "the surviving route-handler proof keeps the scenario proposed");
    }

    @Test void fixtureCarriedSignalDirectReferenceIsDemotedByEvidenceStrengthGate() throws Exception {
        Path checkout = writeCheckout();
        String revision = gitRevision(checkout);
        Path root = writeSealedRoot(checkout, revision);
        ObjectNode evidence = (ObjectNode) JSON.readTree(
                Files.readAllBytes(root.resolve(SfBl002RouteEffectivenessRun.TEST_EVIDENCE_PATH)));
        ObjectNode helper = JSON.createObjectNode();
        helper.put("method_name", "assistHelper");
        helper.putObject("declaration_location")
                .put("repository_relative_path", TEST_PATH).put("line", 30).put("column", 5);
        helper.putArray("fixtures").add(
                item("this.mockMvc.perform(get(\"/owners/find\")).andExpect(status().isOk())"));
        helper.putArray("actions").add(item("this.owners.fetch(\"Franklin\")",
                "com.example.petclinic.owner.OwnerRepository", "METHOD",
                "fetchByLastName", "PRODUCTION_RECEIVER_SOURCE_ROOT"));
        helper.putArray("assertions");
        helper.putArray("unresolved_references");
        ((ArrayNode) evidence.path("test_files").get(0).path("test_methods")).add(helper);
        writeJson(root.resolve(SfBl002RouteEffectivenessRun.TEST_EVIDENCE_PATH), evidence);
        Path output = temp.resolve("out");

        generate(root, checkout, output);

        JsonNode proposal = read(output, SfBl002RouteEffectivenessRun.PROPOSAL_PATH);
        JsonNode find = findScenario(proposal, "HYP-SCENARIO-001");
        assertFalse(hasComponent(find, "DIRECT_REFERENCE", "DIRECT_PRODUCTION_REFERENCE",
                "com.example.petclinic.owner.OwnerRepository#fetchByLastName"),
                "setup-only signals must not back a direct-reference claim; the sealed "
                        + "actions and assertions must carry the behavior evidence");
        assertTrue(hasGap(find, "evidence-strength-gate:insufficient-behavior-signals:"
                + "com.example.petclinic.owner.OwnerRepository#fetchByLastName"));
    }

    @Test void crossScenarioOwnedDirectReferenceIsDemotedFromEveryClaimingScenario() throws Exception {
        Path checkout = writeCheckout();
        String revision = gitRevision(checkout);
        Path root = writeSealedRoot(checkout, revision);
        ObjectNode evidence = (ObjectNode) JSON.readTree(
                Files.readAllBytes(root.resolve(SfBl002RouteEffectivenessRun.TEST_EVIDENCE_PATH)));
        for (JsonNode method : evidence.path("test_files").get(0).path("test_methods")) {
            if (!"findOwnerByLastName".equals(method.path("method_name").asText())) continue;
            ((ArrayNode) method.path("actions"))
                    .add(item("this.owners.add(new com.example.petclinic.owner.Owner())"));
        }
        writeJson(root.resolve(SfBl002RouteEffectivenessRun.TEST_EVIDENCE_PATH), evidence);
        Path output = temp.resolve("out");

        generate(root, checkout, output);

        JsonNode proposal = read(output, SfBl002RouteEffectivenessRun.PROPOSAL_PATH);
        String identity = "com.example.petclinic.owner.OwnerRepository#findByLastNameStartingWith";
        JsonNode find = findScenario(proposal, "HYP-SCENARIO-001");
        JsonNode create = findScenario(proposal, "HYP-SCENARIO-002");
        assertFalse(hasComponent(find, "DIRECT_REFERENCE", "DIRECT_PRODUCTION_REFERENCE", identity),
                "a method owned by two scenarios is not unique proof for the find scenario");
        assertFalse(hasComponent(create, "DIRECT_REFERENCE", "DIRECT_PRODUCTION_REFERENCE", identity),
                "a method owned by two scenarios is not unique proof for the create scenario");
        assertTrue(hasGap(find, "evidence-strength-gate:cross-scenario-proof-reused:" + identity));
        assertTrue(hasGap(create, "evidence-strength-gate:cross-scenario-proof-reused:" + identity));
    }

    @Test void crossScenarioRouteProofIsDemotedOnlyFromTheNonOwningScenario() throws Exception {
        Path checkout = writeGitCheckout("""
                package com.example.petclinic.owner;

                import org.springframework.stereotype.Controller;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;

                @Controller
                @RequestMapping("/owners")
                public class OwnerController {

                    @GetMapping("/find")
                    public String initFindForm() {
                        return "owners/findOwners";
                    }
                }
                """, """
                package com.example.petclinic.owner;

                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
                import org.springframework.test.web.servlet.MockMvc;

                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
                import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

                @WebMvcTest(OwnerController.class)
                class OwnerControllerTests {

                    @Autowired
                    MockMvc mockMvc;

                    void saveOwnerRecord() throws Exception {
                        this.mockMvc.perform(get("/owners/find")).andExpect(status().isOk());
                    }

                    void findOwnerForm() throws Exception {
                        this.mockMvc.perform(get("/owners/find")).andExpect(status().isOk());
                    }
                }
                """);
        String revision = gitRevision(checkout);
        Path root = writeCustomRoot(checkout, revision,
                List.of(
                        intentRecord("HYP-SCENARIO-001", "HYP-CAPABILITY-001", "UPDATE", "OWNER",
                                List.of(), List.of("update owner"), revision),
                        intentRecord("HYP-SCENARIO-002", "HYP-CAPABILITY-001", "FIND", "OWNER",
                                List.of(), List.of("find owner"), revision)),
                List.of("HYP-SCENARIO-001", "HYP-SCENARIO-002"),
                List.of(
                        testMethod("saveOwnerRecord",
                                List.of(item("this.mockMvc.perform(get(\"/owners/find\")).andExpect(status().isOk())")),
                                List.of(unresolved("get(\"/owners/find\")"))),
                        testMethod("findOwnerForm",
                                List.of(item("this.mockMvc.perform(get(\"/owners/find\")).andExpect(status().isOk())")),
                                List.of(unresolved("get(\"/owners/find\")")))));
        Path output = temp.resolve("out");

        generate(root, checkout, output);

        JsonNode proposal = read(output, SfBl002RouteEffectivenessRun.PROPOSAL_PATH);
        String identity = "com.example.petclinic.owner.OwnerController#initFindForm";
        JsonNode update = findScenario(proposal, "HYP-SCENARIO-001");
        JsonNode find = findScenario(proposal, "HYP-SCENARIO-002");
        assertFalse(hasComponent(update, "ROUTE_HANDLER", "EXACT_ROUTE_HANDLER", identity),
                "a route proof whose observation agrees with a different accepted scenario must not"
                        + " be proposed under the non-owning scenario");
        assertTrue(hasGap(update, "evidence-strength-gate:cross-scenario-proof-reused:" + identity));
        assertEquals("UNRESOLVED", update.path("outcome").asText(),
                "the non-owning scenario loses its only component and must emit UNRESOLVED");
        assertTrue(hasComponent(find, "ROUTE_HANDLER", "EXACT_ROUTE_HANDLER", identity),
                "the owning scenario keeps the same observation-backed proof");
        assertFalse(hasGap(find, "evidence-strength-gate:cross-scenario-proof-reused:" + identity));
        assertEquals("MAPPING_PROPOSAL", find.path("outcome").asText());
    }

    @Test void routeProofFailingEveryScenarioAgreementIsNotDemotedByReuseGate() throws Exception {
        Path checkout = writeGitCheckout("""
                package com.example.petclinic.owner;

                import org.springframework.stereotype.Controller;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;

                @Controller
                @RequestMapping("/owners")
                public class OwnerController {

                    @GetMapping("/find")
                    public String initFindForm() {
                        return "owners/findOwners";
                    }
                }
                """, """
                package com.example.petclinic.owner;

                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
                import org.springframework.test.web.servlet.MockMvc;

                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
                import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

                @WebMvcTest(OwnerController.class)
                class OwnerControllerTests {

                    @Autowired
                    MockMvc mockMvc;

                    void saveOwnerScreen() throws Exception {
                        this.mockMvc.perform(get("/owners/find")).andExpect(status().isOk());
                    }
                }
                """);
        String revision = gitRevision(checkout);
        Path root = writeCustomRoot(checkout, revision,
                List.of(intentRecord("HYP-SCENARIO-001", "HYP-CAPABILITY-001", "UPDATE", "OWNER",
                        List.of(), List.of("update owner"), revision)),
                List.of("HYP-SCENARIO-001"),
                List.of(testMethod("saveOwnerScreen",
                        List.of(item("this.mockMvc.perform(get(\"/owners/find\")).andExpect(status().isOk())")),
                        List.of(unresolved("get(\"/owners/find\")")))));
        Path output = temp.resolve("out");

        generate(root, checkout, output);

        JsonNode proposal = read(output, SfBl002RouteEffectivenessRun.PROPOSAL_PATH);
        String identity = "com.example.petclinic.owner.OwnerController#initFindForm";
        JsonNode update = findScenario(proposal, "HYP-SCENARIO-001");
        assertTrue(hasComponent(update, "ROUTE_HANDLER", "EXACT_ROUTE_HANDLER", identity),
                "a proof that agrees with no accepted scenario is not the cross-scenario-reuse"
                        + " condition and must stay proposed for the evaluator to diagnose");
        assertFalse(hasGap(update, "evidence-strength-gate:cross-scenario-proof-reused:" + identity));
        assertEquals("MAPPING_PROPOSAL", update.path("outcome").asText());
    }

    @Test void routeProofAgreeingWithClaimingScenarioSurvivesSharedReuse() throws Exception {
        Path checkout = writeGitCheckout("""
                package com.example.petclinic.owner;

                import org.springframework.stereotype.Controller;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestMapping;

                @Controller
                @RequestMapping("/owners")
                public class OwnerController {

                    @PostMapping("/new")
                    public String processCreationForm() {
                        return "redirect:/owners/1";
                    }
                }
                """, """
                package com.example.petclinic.owner;

                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
                import org.springframework.test.web.servlet.MockMvc;

                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
                import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

                @WebMvcTest(OwnerController.class)
                class OwnerControllerTests {

                    @Autowired
                    MockMvc mockMvc;

                    void createOwner() throws Exception {
                        this.mockMvc.perform(post("/owners/new")).andExpect(status().is3xxRedirection());
                    }
                }
                """);
        String revision = gitRevision(checkout);
        Path root = writeCustomRoot(checkout, revision,
                List.of(
                        intentRecord("HYP-SCENARIO-001", "HYP-CAPABILITY-001", "CREATE", "OWNER",
                                List.of(), List.of("create owner"), revision),
                        intentRecord("HYP-SCENARIO-002", "HYP-CAPABILITY-002", "CREATE", "OWNER",
                                List.of(), List.of("register owner"), revision)),
                List.of("HYP-SCENARIO-001", "HYP-SCENARIO-002"),
                List.of(testMethod("createOwner",
                        List.of(item("this.mockMvc.perform(post(\"/owners/new\")).andExpect(status().is3xxRedirection())")),
                        List.of(unresolved("post(\"/owners/new\")")))));
        Path output = temp.resolve("out");

        generate(root, checkout, output);

        JsonNode proposal = read(output, SfBl002RouteEffectivenessRun.PROPOSAL_PATH);
        String identity = "com.example.petclinic.owner.OwnerController#processCreationForm";
        for (String scenarioId : List.of("HYP-SCENARIO-001", "HYP-SCENARIO-002")) {
            JsonNode scenario = findScenario(proposal, scenarioId);
            assertTrue(hasComponent(scenario, "ROUTE_HANDLER", "EXACT_ROUTE_HANDLER", identity),
                    "an observation that agrees with the claiming scenario stays proposed even when"
                            + " the same proof also agrees with another accepted scenario: " + scenarioId);
            assertFalse(hasGap(scenario, "evidence-strength-gate:cross-scenario-proof-reused:" + identity));
            assertEquals("MAPPING_PROPOSAL", scenario.path("outcome").asText());
        }
    }

    private Path writeGitCheckout(String controllerSource, String testClassSource) throws Exception {
        Path checkout = temp.resolve("checkout-custom");
        write(checkout.resolve(CONTROLLER_PATH), controllerSource);
        write(checkout.resolve(TEST_PATH), testClassSource);
        runGit(checkout, "init", "-q");
        runGit(checkout, "add", "-A");
        runGit(checkout, "-c", "user.name=FDI Test", "-c", "user.email=fdi@example.invalid",
                "commit", "-q", "-m", "synthetic fixture");
        return checkout;
    }

    private Path writeCustomRoot(Path checkout, String revision, List<ObjectNode> intentRecords,
            List<String> acceptedIds, List<ObjectNode> testMethods) throws Exception {
        Path root = temp.resolve("root-custom-" + revision.substring(0, 8));
        Map<String, String> digests = new LinkedHashMap<>();
        digests.put(CONTROLLER_PATH, sha(Files.readAllBytes(checkout.resolve(CONTROLLER_PATH))));
        digests.put(TEST_PATH, sha(Files.readAllBytes(checkout.resolve(TEST_PATH))));

        ObjectNode intents = JSON.createObjectNode();
        intents.put("schema_version", "software-factory.sf-bl002-accepted-scenario-search-intents.v0.1");
        intents.put("status", "FROZEN");
        intents.put("authority", "ACCEPTED_RETRIEVAL_AID_ONLY");
        intents.put("semantic_publication_allowed", false);
        intents.put("source_revision", revision);
        intents.put("semantics_sha256", SEMANTICS_SHA256);
        intents.putArray("accepted_record_ids");
        intents.putArray("rejected_record_ids");
        ArrayNode records = intents.putArray("records");
        intentRecords.forEach(records::add);

        ObjectNode acceptance = JSON.createObjectNode();
        acceptance.put("schema_version",
                "software-factory.sf-bl002-scenario-search-intent-acceptance-manifest.v0.1");
        acceptance.put("status", "FROZEN");
        acceptance.put("source_revision", revision);
        acceptance.put("semantics_sha256", SEMANTICS_SHA256);
        acceptance.put("product_truth_established", false);
        acceptance.put("semantic_publication_allowed", false);
        acceptance.putObject("accepted_artifact")
                .put("path", SfBl002RouteEffectivenessRun.INTENTS_PATH)
                .put("sha256", sha(JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(intents)));
        ArrayNode accepted = acceptance.putArray("accepted_record_ids");
        acceptedIds.forEach(accepted::add);

        ObjectNode evidence = JSON.createObjectNode();
        evidence.put("schema_version", "1");
        evidence.put("repository_id", "spring-petclinic");
        evidence.put("canonical_revision", revision);
        evidence.putObject("provenance").put("provider_id", "fdi-testbehavior-javaparser");
        ObjectNode digestNode = evidence.putObject("input_digests");
        digests.forEach(digestNode::put);
        evidence.put("incomplete", false);
        evidence.putArray("diagnostics");
        ArrayNode testFiles = evidence.putArray("test_files");
        ObjectNode testFile = testFiles.addObject();
        testFile.put("repository_relative_path", TEST_PATH);
        testFile.put("input_digest", digests.get(TEST_PATH));
        testFile.put("test_class_name", "com.example.petclinic.owner.OwnerControllerTests");
        ArrayNode methods = testFile.putArray("test_methods");
        testMethods.forEach(methods::add);

        writeJson(root.resolve(SfBl002RouteEffectivenessRun.INTENTS_PATH), intents);
        writeJson(root.resolve(SfBl002RouteEffectivenessRun.INTENT_ACCEPTANCE_PATH), acceptance);
        writeJson(root.resolve(SfBl002RouteEffectivenessRun.TEST_EVIDENCE_PATH), evidence);
        writeJson(root.resolve(SfBl002RouteEffectivenessRun.GRAPH_PATH), graphDoc());
        writeJson(root.resolve(SfBl002RouteEffectivenessRun.RUNTIME_EVIDENCE_PATH), runtimeDoc());
        return root;
    }

    private ObjectNode graphDoc() {
        ObjectNode graph = JSON.createObjectNode();
        graph.putArray("nodes");
        graph.putArray("links");
        return graph;
    }

    private ObjectNode runtimeDoc() {
        ObjectNode runtime = JSON.createObjectNode();
        runtime.put("verification_id", "graphify-petclinic-live-818c413");
        runtime.put("captured_at", "2026-09-01T00:00:00Z");
        runtime.put("result", "EXACTLY_BOUND");
        runtime.put("queryable", true);
        runtime.put("exact_revision_opened", true);
        runtime.put("runtime_identity", "graphifyy");
        runtime.put("runtime_version", "0.1.14");
        runtime.put("runtime_python", "3.11.9");
        runtime.put("transport", "MCP stdio");
        runtime.put("mcp_version", "1.2.0");
        return runtime;
    }

    private Path writeCheckout() throws Exception {
        Path checkout = temp.resolve("checkout");
        write(checkout.resolve(CONTROLLER_PATH), """
                package com.example.petclinic.owner;

                import org.springframework.stereotype.Controller;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestMapping;

                @Controller
                @RequestMapping("/owners")
                public class OwnerController {

                    @GetMapping("/find")
                    public String initFindForm() {
                        return "owners/findOwners";
                    }

                    @GetMapping("/new")
                    public String initCreationForm() {
                        return "owners/createOrUpdateOwnerForm";
                    }

                    @PostMapping("/new")
                    public String processCreationForm() {
                        return "redirect:/owners/1";
                    }
                }
                """);
        write(checkout.resolve(TEST_PATH), """
                package com.example.petclinic.owner;

                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
                import org.springframework.test.web.servlet.MockMvc;

                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
                import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
                import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
                import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

                @WebMvcTest(OwnerController.class)
                class OwnerControllerTests {

                    @Autowired
                    MockMvc mockMvc;

                    void findOwnerForm() throws Exception {
                        this.mockMvc.perform(get("/owners/find")).andExpect(status().isOk());
                    }

                    void findOwnerByLastName() throws Exception {
                        this.owners.findByLastNameStartingWith("Franklin");
                        this.mockMvc.perform(get("/owners?page=1")).andExpect(view().name("owners/ownersList"));
                    }

                    void createOwner() throws Exception {
                        this.mockMvc.perform(post("/owners/new")).andExpect(status().is3xxRedirection());
                    }

                    void createOwnerWithErrors() throws Exception {
                        this.mockMvc.perform(post("/owners/new")).andExpect(model().attributeHasErrors("owner"));
                    }
                }
                """);
        runGit(checkout, "init", "-q");
        runGit(checkout, "add", "-A");
        runGit(checkout, "-c", "user.name=FDI Test", "-c", "user.email=fdi@example.invalid",
                "commit", "-q", "-m", "synthetic fixture");
        return checkout;
    }

    private Path writeSealedRoot(Path checkout, String revision) throws Exception {
        Path root = temp.resolve("root-" + revision.substring(0, 8));
        Map<String, String> digests = new LinkedHashMap<>();
        digests.put(CONTROLLER_PATH, sha(Files.readAllBytes(checkout.resolve(CONTROLLER_PATH))));
        digests.put(TEST_PATH, sha(Files.readAllBytes(checkout.resolve(TEST_PATH))));

        ObjectNode intents = JSON.createObjectNode();
        intents.put("schema_version", "software-factory.sf-bl002-accepted-scenario-search-intents.v0.1");
        intents.put("status", "FROZEN");
        intents.put("authority", "ACCEPTED_RETRIEVAL_AID_ONLY");
        intents.put("semantic_publication_allowed", false);
        intents.put("source_revision", revision);
        intents.put("semantics_sha256", SEMANTICS_SHA256);
        intents.putArray("accepted_record_ids");
        intents.putArray("rejected_record_ids");
        ArrayNode records = intents.putArray("records");
        records.add(intentRecord("HYP-SCENARIO-001", "HYP-CAPABILITY-001", "FIND", "OWNER",
                List.of("last-name-criteria"), List.of("find owner"), revision));
        records.add(intentRecord("HYP-SCENARIO-002", "HYP-CAPABILITY-001", "CREATE", "OWNER",
                List.of(), List.of("create owner"), revision));
        records.add(intentRecord("HYP-SCENARIO-003", "HYP-CAPABILITY-001", "REJECT", "OWNER",
                List.of("validation-error"), List.of("reject owner"), revision));

        ObjectNode acceptance = JSON.createObjectNode();
        acceptance.put("schema_version",
                "software-factory.sf-bl002-scenario-search-intent-acceptance-manifest.v0.1");
        acceptance.put("status", "FROZEN");
        acceptance.put("source_revision", revision);
        acceptance.put("semantics_sha256", SEMANTICS_SHA256);
        acceptance.put("product_truth_established", false);
        acceptance.put("semantic_publication_allowed", false);
        acceptance.putObject("accepted_artifact")
                .put("path", SfBl002RouteEffectivenessRun.INTENTS_PATH)
                .put("sha256", sha(JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(intents)));
        ArrayNode acceptedIds = acceptance.putArray("accepted_record_ids");
        acceptedIds.add("HYP-SCENARIO-001").add("HYP-SCENARIO-002").add("HYP-SCENARIO-003");

        ObjectNode evidence = JSON.createObjectNode();
        evidence.put("schema_version", "1");
        evidence.put("repository_id", "spring-petclinic");
        evidence.put("canonical_revision", revision);
        evidence.putObject("provenance").put("provider_id", "fdi-testbehavior-javaparser");
        ObjectNode digestNode = evidence.putObject("input_digests");
        digests.forEach(digestNode::put);
        evidence.put("incomplete", false);
        evidence.putArray("diagnostics");
        ArrayNode testFiles = evidence.putArray("test_files");
        ObjectNode testFile = testFiles.addObject();
        testFile.put("repository_relative_path", TEST_PATH);
        testFile.put("input_digest", digests.get(TEST_PATH));
        testFile.put("test_class_name", "com.example.petclinic.owner.OwnerControllerTests");
        ArrayNode methods = testFile.putArray("test_methods");
        methods.add(testMethod("findOwnerForm",
                List.of(item("this.mockMvc.perform(get(\"/owners/find\")).andExpect(status().isOk())")),
                List.of(unresolved("get(\"/owners/find\")"))));
        methods.add(testMethod("findOwnerByLastName",
                List.of(item("this.owners.findByLastNameStartingWith(\"Franklin\")",
                        "com.example.petclinic.owner.OwnerRepository", "METHOD",
                        "findByLastNameStartingWith", "PRODUCTION_RECEIVER_SOURCE_ROOT"),
                        item("this.mockMvc.perform(get(\"/owners?page=1\")).andExpect(view().name(\"owners/ownersList\"))")),
                List.of(unresolved("get(\"/owners?page=1\")"))));
        methods.add(testMethod("createOwner",
                List.of(item("this.mockMvc.perform(post(\"/owners/new\")).andExpect(status().is3xxRedirection())")),
                List.of(unresolved("post(\"/owners/new\")"))));
        methods.add(testMethod("createOwnerWithErrors",
                List.of(item("this.mockMvc.perform(post(\"/owners/new\")).andExpect(model().attributeHasErrors(\"owner\"))")),
                List.of(unresolved("model().attributeHasErrors(\"owner\")"))));

        ObjectNode graph = JSON.createObjectNode();
        graph.putArray("nodes");
        graph.putArray("links");

        ObjectNode runtime = JSON.createObjectNode();
        runtime.put("verification_id", "graphify-petclinic-live-818c413");
        runtime.put("captured_at", "2026-09-01T00:00:00Z");
        runtime.put("result", "EXACTLY_BOUND");
        runtime.put("queryable", true);
        runtime.put("exact_revision_opened", true);
        runtime.put("runtime_identity", "graphifyy");
        runtime.put("runtime_version", "0.1.14");
        runtime.put("runtime_python", "3.11.9");
        runtime.put("transport", "MCP stdio");
        runtime.put("mcp_version", "1.2.0");

        writeJson(root.resolve(SfBl002RouteEffectivenessRun.INTENTS_PATH), intents);
        writeJson(root.resolve(SfBl002RouteEffectivenessRun.INTENT_ACCEPTANCE_PATH), acceptance);
        writeJson(root.resolve(SfBl002RouteEffectivenessRun.TEST_EVIDENCE_PATH), evidence);
        writeJson(root.resolve(SfBl002RouteEffectivenessRun.GRAPH_PATH), graph);
        writeJson(root.resolve(SfBl002RouteEffectivenessRun.RUNTIME_EVIDENCE_PATH), runtime);
        return root;
    }

    private ObjectNode intentRecord(String scenarioId, String capabilityId, String action, String entity,
            List<String> conditions, List<String> aliases, String revision) {
        ObjectNode record = JSON.createObjectNode();
        record.put("scenarioId", scenarioId);
        record.put("capabilityId", capabilityId);
        record.put("action", action);
        record.put("entity", entity);
        ArrayNode conditionNode = record.putArray("conditions");
        conditions.forEach(conditionNode::add);
        ArrayNode aliasNode = record.putArray("aliases");
        aliases.forEach(aliasNode::add);
        record.put("semanticsDigest", SEMANTICS_SHA256);
        record.put("sourceRevision", revision);
        record.put("authority", "ACCEPTED_RETRIEVAL_AID_ONLY");
        return record;
    }

    private ObjectNode testMethod(String methodName, List<ObjectNode> items, List<ObjectNode> unresolved) {
        ObjectNode method = JSON.createObjectNode();
        method.put("method_name", methodName);
        method.putObject("declaration_location")
                .put("repository_relative_path", TEST_PATH).put("line", 20).put("column", 5);
        method.putArray("fixtures");
        ArrayNode actions = method.putArray("actions");
        items.forEach(actions::add);
        method.putArray("assertions");
        ArrayNode unresolvedNode = method.putArray("unresolved_references");
        unresolved.forEach(unresolvedNode::add);
        return method;
    }

    private ObjectNode item(String observedExpression) {
        return item(observedExpression, null, null, null, null);
    }

    private ObjectNode item(String observedExpression, String declaringType, String kind,
            String symbolName, String basis) {
        ObjectNode item = JSON.createObjectNode();
        item.put("kind", "ACTION");
        item.put("observed_expression", observedExpression);
        item.putObject("location")
                .put("repository_relative_path", TEST_PATH).put("line", 21).put("column", 9);
        if (declaringType != null) {
            item.putObject("referenced_symbol")
                    .put("kind", kind).put("declaring_type", declaringType)
                    .put("symbol_name", symbolName).put("basis", basis);
        }
        return item;
    }

    private ObjectNode unresolved(String referenceText) {
        ObjectNode item = JSON.createObjectNode();
        item.put("kind", "EXTERNAL_DEPENDENCY_NOT_RESOLVED");
        item.put("reference_text", referenceText);
        item.putObject("location")
                .put("repository_relative_path", TEST_PATH).put("line", 21).put("column", 9);
        return item;
    }

    private SfBl002RouteEffectivenessRun.Result generate(Path root, Path checkout, Path output)
            throws Exception {
        return SfBl002RouteEffectivenessRun.generate(root, checkout, output,
                sealedMap(root), gitRevision(checkout));
    }

    private static Map<String, String> sealedMap(Path root) throws Exception {
        Map<String, String> sealed = new LinkedHashMap<>();
        for (String path : List.of(SfBl002RouteEffectivenessRun.INTENTS_PATH,
                SfBl002RouteEffectivenessRun.INTENT_ACCEPTANCE_PATH,
                SfBl002RouteEffectivenessRun.TEST_EVIDENCE_PATH,
                SfBl002RouteEffectivenessRun.GRAPH_PATH,
                SfBl002RouteEffectivenessRun.RUNTIME_EVIDENCE_PATH)) {
            sealed.put(path, sha(Files.readAllBytes(root.resolve(path))));
        }
        return sealed;
    }

    private static JsonNode findScenario(JsonNode proposal, String scenarioId) {
        for (JsonNode scenario : proposal.path("scenarios")) {
            if (scenarioId.equals(scenario.path("scenarioId").asText())) return scenario;
        }
        throw new AssertionError("scenario not found: " + scenarioId);
    }

    private static JsonNode findComponent(JsonNode scenario, String role, String strength, String identity) {
        for (JsonNode component : scenario.path("components")) {
            if (role.equals(component.path("role").asText())
                    && strength.equals(component.path("evidenceStrength").asText())
                    && identity.equals(component.path("productionIdentity").asText())) {
                return component;
            }
        }
        throw new AssertionError("component not found: " + role + " " + strength + " " + identity);
    }

    private static boolean hasComponent(JsonNode scenario, String role, String strength, String identity) {
        for (JsonNode component : scenario.path("components")) {
            if (role.equals(component.path("role").asText())
                    && strength.equals(component.path("evidenceStrength").asText())
                    && identity.equals(component.path("productionIdentity").asText())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasGap(JsonNode scenario, String gap) {
        for (JsonNode node : scenario.path("gaps")) {
            if (gap.equals(node.asText())) return true;
        }
        return false;
    }

    private static List<String> jsonList(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.asText()));
        return values;
    }

    private static JsonNode read(Path output, String relative) throws Exception {
        return JSON.readTree(output.resolve(relative).toFile());
    }

    private static void write(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private static void writeJson(Path path, JsonNode document) throws Exception {
        Files.createDirectories(path.getParent());
        Files.write(path, JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(document));
    }

    private static String sha(byte[] bytes) { return ScenarioForwardRequestReader.sha256(bytes); }

    private static String gitRevision(Path repo) throws Exception {
        return runGit(repo, "rev-parse", "HEAD").trim();
    }

    private static String runGit(Path dir, String... args) throws Exception {
        String[] command = new String[args.length + 3];
        command[0] = "git";
        command[1] = "-C";
        command[2] = dir.toAbsolutePath().normalize().toString();
        System.arraycopy(args, 0, command, 3, args.length);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        int exit = process.waitFor();
        if (exit != 0) throw new IllegalStateException("git " + String.join(" ", args) + " failed: " + output);
        return output;
    }
}

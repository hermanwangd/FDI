package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.ComponentIdentity;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.DirectProductionSymbolEvidence;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.Granularity;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.SeedProvenance;
import com.featuredeliveryintelligence.fdi.product.realization.directtrace.ResolvedDirectObservation;
import com.featuredeliveryintelligence.fdi.product.realization.directtrace.SfBl002TestBehaviorEvidence;
import com.featuredeliveryintelligence.fdi.product.realization.directtrace.TraceSourceLocation;
import com.featuredeliveryintelligence.fdi.product.realization.directtrace.UnresolvedDirectReferenceGap;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.ScenarioObservationAssignmentGenerator.Candidate;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.ScenarioObservationAssignmentGenerator.EvidenceIndex;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioObservationAssignmentGeneratorTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String REV = SfBl002TestBehaviorEvidence.SOURCE_REVISION;
    private static final String SEM_SHA = ScenarioObservationAssignmentGenerator.SEMANTICS_SHA256;
    private static final String EV_SHA = SfBl002TestBehaviorEvidence.EVIDENCE_SHA256;
    @TempDir Path temp;

    @Test void frozenRunEmitsOneRecordPerScenarioAndNeverForceMaps() throws Exception {
        Path output = temp.resolve("out");
        var result = ScenarioObservationAssignmentGenerator.generate(Path.of("."), output);

        assertEquals(10, result.scenarioCount());
        assertEquals(0, result.assignedScenarioCount());
        JsonNode artifact = JSON.readTree(output.resolve(ScenarioObservationAssignmentGenerator.ARTIFACT_PATH).toFile());
        assertEquals("PROPOSAL_ONLY", artifact.path("authority").asText());
        assertFalse(artifact.path("semantic_publication_allowed").asBoolean());
        assertEquals(REV, artifact.path("source_revision").asText());
        assertEquals(SEM_SHA, artifact.path("semantics_sha256").asText());
        assertEquals(EV_SHA, artifact.path("test_evidence_sha256").asText());
        assertTrue(artifact.path("authorization_sha256").asText().matches("[0-9a-f]{64}"));
        assertEquals(10, artifact.path("assignments").size());
        for (JsonNode record : artifact.path("assignments")) {
            assertEquals("PROPOSAL_ONLY", record.path("authority").asText());
            assertEquals(REV, record.path("sourceRevision").asText());
            assertEquals(SEM_SHA, record.path("semanticsDigest").asText());
            assertEquals(EV_SHA, record.path("testEvidenceDigest").asText());
            assertTrue(record.path("directEvidenceRefs").isEmpty(), record.path("scenarioId").asText());
            assertTrue(record.path("gapRefs").isEmpty());
            assertTrue(record.path("selectionRationale").asText().contains("never force-mapped"));
        }
        JsonNode manifest = JSON.readTree(output.resolve(ScenarioObservationAssignmentGenerator.MANIFEST_PATH).toFile());
        assertEquals(result.artifactSha256(), manifest.path("artifact").path("sha256").asText());
        assertEquals(SEM_SHA, manifest.path("inputs").path(ScenarioObservationAssignmentGenerator.SEMANTICS_PATH).asText());
        assertEquals(EV_SHA, manifest.path("inputs").path(SfBl002TestBehaviorEvidence.EVIDENCE_PATH).asText());
    }

    @Test void doubleRunIntoSeparateRootsIsByteIdenticalWithMatchingDigests() throws Exception {
        Path first = temp.resolve("first"), second = temp.resolve("second");
        var one = ScenarioObservationAssignmentGenerator.generate(Path.of("."), first);
        var two = ScenarioObservationAssignmentGenerator.generate(Path.of("."), second);
        assertEquals(one.artifactSha256(), two.artifactSha256());
        assertEquals(one.manifestSha256(), two.manifestSha256());
        assertArrayEquals(Files.readAllBytes(first.resolve(ScenarioObservationAssignmentGenerator.ARTIFACT_PATH)),
                Files.readAllBytes(second.resolve(ScenarioObservationAssignmentGenerator.ARTIFACT_PATH)));
        assertArrayEquals(Files.readAllBytes(first.resolve(ScenarioObservationAssignmentGenerator.MANIFEST_PATH)),
                Files.readAllBytes(second.resolve(ScenarioObservationAssignmentGenerator.MANIFEST_PATH)));
    }

    @Test void existingIdenticalOutputsAreReusedButChangedBytesFailClosed() throws Exception {
        Path output = temp.resolve("out");
        var first = ScenarioObservationAssignmentGenerator.generate(Path.of("."), output);
        var second = ScenarioObservationAssignmentGenerator.generate(Path.of("."), output);
        assertEquals(first.artifactSha256(), second.artifactSha256());
        Files.writeString(output.resolve(ScenarioObservationAssignmentGenerator.ARTIFACT_PATH),
                "\n", StandardOpenOption.APPEND);
        assertThrows(RuntimeContractException.class,
                () -> ScenarioObservationAssignmentGenerator.generate(Path.of("."), output));
    }

    @Test void sealedInputDigestMismatchFailsClosed() throws Exception {
        Path root = temp.resolve("root");
        copySealedInputs(root);
        Files.writeString(root.resolve(ScenarioObservationAssignmentGenerator.SEMANTICS_PATH),
                "\n", StandardOpenOption.APPEND);
        assertThrows(RuntimeContractException.class,
                () -> ScenarioObservationAssignmentGenerator.generate(root, temp.resolve("out")));
    }

    @Test void mechanicalTokenOverlapSelectsOnlyExistingProductionEvidence() {
        var index = index(
                candidate("direct-test-reference:0001", "org.springframework.samples.petclinic.owner.OwnerRepository",
                        "findByLastName", "OwnerControllerTests", "testFind", Set.of(
                                "owner", "controller", "tests", "testfind", "ownerrepository", "find", "by", "last", "name")),
                candidate("direct-test-reference:0002", "org.springframework.samples.petclinic.vet.VetRepository",
                        "findAll", "VetControllerTests", "testVets", Set.of(
                                "vet", "controller", "tests", "testvets", "vetrepository", "all")));
        JsonNode scenario = scenario("HYP-SCENARIO-001", "依姓氏找到一筆或多筆飼主資料 Find owner by last name");

        ObjectNode record = ScenarioObservationAssignmentGenerator.assignmentRecord(
                "HYP-CAPABILITY-001", scenario, REV, SEM_SHA, EV_SHA, index);

        assertEquals(List.of("direct-test-reference:0001"), toList(record.path("directEvidenceRefs")));
        assertTrue(record.path("selectionRationale").asText().contains("direct-test-reference:0001"));
        assertTrue(record.path("selectionRationale").asText().contains("OwnerRepository#findByLastName"));
    }

    @Test void unassignableScenarioEmitsNoDirectRefsAndExplicitGapRationale() {
        var index = index(candidate("direct-test-reference:0001",
                "org.springframework.samples.petclinic.vet.VetRepository", "findAll",
                "VetControllerTests", "testVets", Set.of("vet", "find", "all")));
        JsonNode scenario = scenario("HYP-SCENARIO-009", "依姓氏找到一筆或多筆飼主資料");

        ObjectNode record = ScenarioObservationAssignmentGenerator.assignmentRecord(
                "HYP-CAPABILITY-001", scenario, REV, SEM_SHA, EV_SHA, index);

        assertTrue(record.path("directEvidenceRefs").isEmpty());
        assertTrue(record.path("gapRefs").isEmpty());
        assertTrue(record.path("selectionRationale").asText().contains("no defensible direct production evidence"));
    }

    @Test void duplicateAndUnknownAndMixedRevisionAndAuthoritySelectionsFailClosed() {
        var index = index(candidate("direct-test-reference:0001",
                "org.springframework.samples.petclinic.owner.OwnerRepository", "findByLastName",
                "OwnerControllerTests", "testFind", Set.of("owner", "find", "by", "last", "name")));
        JsonNode scenario = scenario("HYP-SCENARIO-001", "Find owner by last name");
        ObjectNode record = ScenarioObservationAssignmentGenerator.assignmentRecord(
                "HYP-CAPABILITY-001", scenario, REV, SEM_SHA, EV_SHA, index);
        ScenarioObservationAssignmentGenerator.validateAssignmentRecord(record, index, REV, SEM_SHA, EV_SHA);

        record.withArray("directEvidenceRefs").add("direct-test-reference:0001");
        assertThrows(RuntimeContractException.class, () -> ScenarioObservationAssignmentGenerator
                .validateAssignmentRecord(record, index, REV, SEM_SHA, EV_SHA));
        record.withArray("directEvidenceRefs").remove(1);
        record.withArray("directEvidenceRefs").add("direct-test-reference:9999");
        assertThrows(RuntimeContractException.class, () -> ScenarioObservationAssignmentGenerator
                .validateAssignmentRecord(record, index, REV, SEM_SHA, EV_SHA));
        record.withArray("directEvidenceRefs").remove(1);
        record.put("sourceRevision", "1".repeat(40));
        assertThrows(RuntimeContractException.class, () -> ScenarioObservationAssignmentGenerator
                .validateAssignmentRecord(record, index, REV, SEM_SHA, EV_SHA));
        record.put("sourceRevision", REV);
        record.put("semanticsDigest", "2".repeat(64));
        assertThrows(RuntimeContractException.class, () -> ScenarioObservationAssignmentGenerator
                .validateAssignmentRecord(record, index, REV, SEM_SHA, EV_SHA));
        record.put("semanticsDigest", SEM_SHA);
        record.put("authority", "ACCEPTED_PRODUCT_KNOWLEDGE");
        assertThrows(RuntimeContractException.class, () -> ScenarioObservationAssignmentGenerator
                .validateAssignmentRecord(record, index, REV, SEM_SHA, EV_SHA));
    }

    @Test void evaluatorVocabularyInIdentityOrRationaleFailsClosed() {
        var index = index(candidate("direct-test-reference:0001",
                "org.springframework.samples.petclinic.owner.OwnerRepository", "findByLastName",
                "OwnerControllerTests", "testFind", Set.of("owner", "find", "by", "last", "name")));
        JsonNode scenario = scenario("HYP-SCENARIO-001", "Find owner by last name");
        assertThrows(RuntimeContractException.class, () -> ScenarioObservationAssignmentGenerator
                .assignmentRecord("gold-mapping", scenario, REV, SEM_SHA, EV_SHA, index));
        assertThrows(RuntimeContractException.class, () -> ScenarioObservationAssignmentGenerator
                .assignmentRecord("HYP-CAPABILITY-001", scenario("ground truth", "Find owner"), REV, SEM_SHA, EV_SHA, index));
    }

    @Test void latinTokenExtractionSplitsCamelCaseAndIgnoresChineseText() {
        assertEquals(Set.of("owner", "controller", "tests", "find", "by", "last", "name"),
                ScenarioObservationAssignmentGenerator.latinTokens("OwnerControllerTests findByLastName"));
        assertTrue(ScenarioObservationAssignmentGenerator.latinTokens("依姓氏找到一筆或多筆飼主資料").isEmpty());
    }

    @Test void evidenceAdapterRejectsTestHelperExternalOnlyAndMixedRevisionSymbols() throws Exception {
        String testHelper = evidenceDocument("org.springframework.samples.petclinic.owner.OwnerControllerTests",
                "findByLastName", "IMPORTED_SOURCE_ROOT");
        assertThrows(RuntimeContractException.class,
                () -> SfBl002TestBehaviorEvidence.adapt(testHelper.getBytes(), Path.of(".")));
        String externalOnly = evidenceDocument("com.example.external.Client", "call", "EXTERNAL_DEPENDENCY_NOT_RESOLVED");
        assertThrows(RuntimeContractException.class,
                () -> SfBl002TestBehaviorEvidence.adapt(externalOnly.getBytes(), Path.of(".")));
        String mixedRevision = evidenceDocument("org.springframework.samples.petclinic.owner.OwnerRepository",
                "findByLastName", "PRODUCTION_RECEIVER_SOURCE_ROOT").replace(REV, "1".repeat(40));
        assertThrows(RuntimeContractException.class,
                () -> SfBl002TestBehaviorEvidence.adapt(mixedRevision.getBytes(), Path.of(".")));
    }

    @Test void evidenceAdapterRecoveredReceiverBasisResolvesToFrozenProductionPath() throws Exception {
        String document = evidenceDocument("org.springframework.samples.petclinic.owner.OwnerRepository",
                "findByLastName", "PRODUCTION_RECEIVER_SOURCE_ROOT");
        var loaded = SfBl002TestBehaviorEvidence.adapt(document.getBytes(), Path.of("."));
        assertEquals(1, loaded.observations().size());
        assertEquals("src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java",
                loaded.observations().get(0).directEvidence().productionSymbol().sourcePath());
        assertEquals("org.springframework.samples.petclinic.owner.OwnerRepository#findByLastName",
                loaded.observations().get(0).directEvidence().productionSymbol().qualifiedSymbol());
        assertEquals(ScenarioForwardRequestReader.sha256(document.getBytes()), loaded.evidenceSha256());
    }

    private static List<String> toList(JsonNode array) {
        return java.util.stream.StreamSupport.stream(array.spliterator(), false).map(JsonNode::asText).toList();
    }

    private static JsonNode scenario(String id, String title) {
        ObjectNode node = JSON.createObjectNode();
        node.put("scenario_id", id).put("title", title);
        node.putArray("given");
        node.put("when", title);
        node.putArray("then");
        return node;
    }

    private static Candidate candidate(String ref, String declaringType, String symbolName,
            String testClass, String testMethod, Set<String> tokens) {
        var identity = new ComponentIdentity(REV,
                "src/main/java/org/springframework/samples/petclinic/owner/" + simpleName(declaringType) + ".java",
                Granularity.METHOD, declaringType + "#" + symbolName);
        var evidence = new DirectProductionSymbolEvidence(ref, "/test_files/0/test_methods/0/actions/0", identity);
        var seed = new SeedProvenance("production-seed:" + ref.substring(ref.length() - 4), ref, identity);
        var observation = new ResolvedDirectObservation(evidence, seed,
                new TraceSourceLocation("src/test/java/org/springframework/samples/petclinic/owner/" + testClass + ".java", 1, 1));
        return new Candidate(observation, tokens, Set.of(), testClass, testMethod);
    }

    private static EvidenceIndex index(Candidate... candidates) {
        Map<String, Candidate> byRef = new LinkedHashMap<>();
        Map<String, ResolvedDirectObservation> observations = new LinkedHashMap<>();
        Map<String, UnresolvedDirectReferenceGap> gaps = new LinkedHashMap<>();
        for (Candidate candidate : candidates) {
            String ref = candidate.observation().directEvidence().evidenceRef();
            byRef.put(ref, candidate);
            observations.put(ref, candidate.observation());
        }
        return new EvidenceIndex(byRef, Map.of(), observations, gaps);
    }

    private static String simpleName(String qualified) {
        int dot = qualified.lastIndexOf('.');
        return dot < 0 ? qualified : qualified.substring(dot + 1);
    }

    private static String evidenceDocument(String declaringType, String symbolName, String basis) {
        return """
                {"schema_version":"1","provenance":{"provider_id":"fdi-testbehavior-javaparser"},
                 "repository_id":"spring-petclinic","canonical_revision":"%s",
                 "test_files":[{"repository_relative_path":"src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java",
                  "test_methods":[{"method_name":"testFind",
                   "fixtures":[],
                   "actions":[{"kind":"ACTION","observed_expression":"owners.find()",
                    "location":{"repository_relative_path":"src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java","line":10,"column":5},
                    "referenced_symbol":{"kind":"METHOD","declaring_type":"%s","symbol_name":"%s","basis":"%s"}}],
                   "assertions":[],
                   "unresolved_references":[]}],
                  "unresolved_references":[]}],
                 "incomplete":true,"diagnostics":[]}
                """.formatted(REV, declaringType, symbolName, basis);
    }

    private static void copySealedInputs(Path root) throws Exception {
        for (String path : ScenarioObservationAssignmentGenerator.sealedInputs().keySet()) {
            Path from = Path.of(".").resolve(path), to = root.resolve(path);
            Files.createDirectories(to.getParent());
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

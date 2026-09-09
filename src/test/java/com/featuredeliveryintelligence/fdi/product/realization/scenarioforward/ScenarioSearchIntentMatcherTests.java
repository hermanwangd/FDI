package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.ComponentIdentity;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.DirectProductionSymbolEvidence;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.Granularity;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.SeedProvenance;
import com.featuredeliveryintelligence.fdi.product.realization.directtrace.ResolvedDirectObservation;
import com.featuredeliveryintelligence.fdi.product.realization.directtrace.SfBl002TestBehaviorEvidence;
import com.featuredeliveryintelligence.fdi.product.realization.directtrace.TraceSourceLocation;
import com.featuredeliveryintelligence.fdi.product.realization.directtrace.UnresolvedDirectReferenceGap;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.ScenarioSearchIntentMatcher.Candidate;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.ScenarioSearchIntentMatcher.Digests;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.ScenarioSearchIntentMatcher.EvidenceIndex;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioSearchIntentMatcherTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String REV = SfBl002TestBehaviorEvidence.SOURCE_REVISION;
    private static final String SEM_SHA = ScenarioSearchIntentMatcher.SEMANTICS_SHA256;
    private static final String EV_SHA = SfBl002TestBehaviorEvidence.EVIDENCE_SHA256;
    private static final Digests DIGESTS = new Digests(SEM_SHA,
            ScenarioSearchIntentMatcher.ACCEPTANCE_MANIFEST_SHA256,
            ScenarioSearchIntentMatcher.INTENTS_SHA256,
            ScenarioSearchIntentMatcher.INTENT_ACCEPTANCE_SHA256, EV_SHA);
    @TempDir Path temp;

    @Test void camelCaseLatinV1PolicyIsNamedAndSplitsCamelCaseIdentifiers() {
        assertEquals("CAMEL_CASE_LATIN_V1", ScenarioSearchIntentMatcher.TOKENIZATION_POLICY);
        assertEquals(Set.of("owner", "controller", "tests", "find", "by", "last", "name"),
                ScenarioSearchIntentMatcher.latinTokens("OwnerControllerTests findByLastName"));
        assertEquals(Set.of("url", "parser", "value"),
                ScenarioSearchIntentMatcher.latinTokens("URLParserValue"));
        assertTrue(ScenarioSearchIntentMatcher.latinTokens("依姓氏找到一筆或多筆飼主資料").isEmpty());
        assertFalse(ScenarioSearchIntentMatcher.latinTokens("a I x 42").contains("a"));
        assertEquals(List.of("apple", "mango", "zebra"),
                List.copyOf(ScenarioSearchIntentMatcher.latinTokens("zebra Apple mango")));
    }

    @Test void frozenRunEmitsTenRecordsBindingEveryDigestAndRefusingPublication() throws Exception {
        Path output = temp.resolve("out");
        var result = ScenarioSearchIntentMatcher.generate(Path.of("."), output);
        assertEquals(10, result.scenarioCount());
        assertTrue(result.primaryAssignedCount() >= 6, "experimental trace threshold is 6/10");
        JsonNode artifact = JSON.readTree(output.resolve(ScenarioSearchIntentMatcher.ARTIFACT_PATH).toFile());
        assertEquals("PROPOSAL_ONLY", artifact.path("authority").asText());
        assertFalse(artifact.path("semantic_publication_allowed").asBoolean());
        assertEquals("CAMEL_CASE_LATIN_V1", artifact.path("tokenization_policy").asText());
        assertEquals(REV, artifact.path("source_revision").asText());
        assertEquals(SEM_SHA, artifact.path("semantics_sha256").asText());
        assertEquals(ScenarioSearchIntentMatcher.INTENTS_SHA256, artifact.path("intent_sha256").asText());
        assertEquals(ScenarioSearchIntentMatcher.INTENT_ACCEPTANCE_SHA256,
                artifact.path("intent_acceptance_sha256").asText());
        assertEquals(EV_SHA, artifact.path("test_evidence_sha256").asText());
        assertEquals(10, artifact.path("assignments").size());
        for (JsonNode record : artifact.path("assignments")) {
            assertEquals("PROPOSAL_ONLY", record.path("authority").asText());
            assertFalse(record.path("semantic_publication_allowed").asBoolean());
            assertEquals(REV, record.path("sourceRevision").asText());
            assertEquals(SEM_SHA, record.path("semanticsDigest").asText());
            assertEquals(ScenarioSearchIntentMatcher.INTENTS_SHA256, record.path("intentDigest").asText());
            assertEquals(ScenarioSearchIntentMatcher.INTENT_ACCEPTANCE_SHA256,
                    record.path("intentAcceptanceDigest").asText());
            assertEquals(EV_SHA, record.path("testEvidenceDigest").asText());
            assertTrue(record.path("status").asText().matches("PRIMARY_ASSIGNED|UNRESOLVED"));
            String status = record.path("status").asText();
            String primaryRef = record.path("primaryEvidenceRef").asText();
            assertFalse(primaryRef.isBlank());
            assertFalse(record.has("primaryEvidenceRefs"), "legacy array field must not be emitted");
            if (status.equals("UNRESOLVED")) {
                assertEquals("UNRESOLVED", primaryRef);
            } else {
                assertNotEquals("UNRESOLVED", primaryRef);
                assertTrue(primaryRef.startsWith("direct-test-reference:"));
            }
        }
        JsonNode manifest = JSON.readTree(output.resolve(ScenarioSearchIntentMatcher.MANIFEST_PATH).toFile());
        assertEquals("software-factory.sf-bl002-artifact-manifest.v0.2", manifest.path("schema_version").asText());
        assertEquals(result.artifactSha256(), manifest.path("artifact").path("sha256").asText());
        assertEquals(5, manifest.path("inputs").size());
        assertEquals(SEM_SHA, manifest.path("inputs").path(ScenarioSearchIntentMatcher.SEMANTICS_PATH).asText());
        assertEquals(EV_SHA, manifest.path("inputs").path(SfBl002TestBehaviorEvidence.EVIDENCE_PATH).asText());
    }

    @Test void doubleRunIntoSeparateRootsIsByteIdenticalWithMatchingDigests() throws Exception {
        Path first = temp.resolve("first"), second = temp.resolve("second");
        var one = ScenarioSearchIntentMatcher.generate(Path.of("."), first);
        var two = ScenarioSearchIntentMatcher.generate(Path.of("."), second);
        assertEquals(one.artifactSha256(), two.artifactSha256());
        assertArrayEquals(Files.readAllBytes(first.resolve(ScenarioSearchIntentMatcher.ARTIFACT_PATH)),
                Files.readAllBytes(second.resolve(ScenarioSearchIntentMatcher.ARTIFACT_PATH)));
        assertArrayEquals(Files.readAllBytes(first.resolve(ScenarioSearchIntentMatcher.MANIFEST_PATH)),
                Files.readAllBytes(second.resolve(ScenarioSearchIntentMatcher.MANIFEST_PATH)));
    }

    @Test void existingIdenticalOutputsAreReusedButChangedBytesRefuseCollision() throws Exception {
        Path output = temp.resolve("out");
        var first = ScenarioSearchIntentMatcher.generate(Path.of("."), output);
        assertEquals(first.artifactSha256(), ScenarioSearchIntentMatcher.generate(Path.of("."), output).artifactSha256());
        Files.writeString(output.resolve(ScenarioSearchIntentMatcher.ARTIFACT_PATH),
                "\n", StandardOpenOption.APPEND);
        assertThrows(RuntimeContractException.class,
                () -> ScenarioSearchIntentMatcher.generate(Path.of("."), output));
    }

    @Test void sealedInputDigestMismatchFailsClosed() throws Exception {
        Path root = temp.resolve("root");
        copySealedInputs(root);
        Files.writeString(root.resolve(ScenarioSearchIntentMatcher.INTENTS_PATH),
                "\n", StandardOpenOption.APPEND);
        assertThrows(RuntimeContractException.class,
                () -> ScenarioSearchIntentMatcher.generate(root, temp.resolve("out")));
    }

    @Test void wrongProposalArtifactDigestFailsClosed() throws Exception {
        Path root = temp.resolve("root");
        copySealedInputs(root);
        Files.writeString(root.resolve(ScenarioSearchIntentMatcher.PROPOSAL_PATH),
                "\n", StandardOpenOption.APPEND);
        assertThrows(RuntimeContractException.class,
                () -> ScenarioSearchIntentMatcher.generate(root, temp.resolve("out")));
    }

    @Test void wrongIntentAcceptanceOrTestEvidenceDigestFailsClosed() throws Exception {
        Path root = temp.resolve("root");
        copySealedInputs(root);
        Files.writeString(root.resolve(ScenarioSearchIntentMatcher.INTENT_ACCEPTANCE_PATH),
                "\n", StandardOpenOption.APPEND);
        assertThrows(RuntimeContractException.class,
                () -> ScenarioSearchIntentMatcher.generate(root, temp.resolve("out")));
        Path root2 = temp.resolve("root2");
        copySealedInputs(root2);
        Files.writeString(root2.resolve(SfBl002TestBehaviorEvidence.EVIDENCE_PATH),
                "\n", StandardOpenOption.APPEND);
        assertThrows(RuntimeContractException.class,
                () -> ScenarioSearchIntentMatcher.generate(root2, temp.resolve("out2")));
    }

    @Test void duplicateUnknownMixedRevisionAndNonProposalAuthorityRecordsFailClosed() throws Exception {
        JsonNode duplicate = JSON.readTree(Path.of(ScenarioSearchIntentMatcher.INTENTS_PATH).toFile());
        ObjectNode copy = (ObjectNode) duplicate.path("records").get(0).deepCopy();
        ((com.fasterxml.jackson.databind.node.ArrayNode) duplicate.path("records")).add(copy);
        assertThrows(RuntimeContractException.class, () -> ScenarioSearchIntentMatcher
                .validateAcceptedIntents(duplicate, REV, SEM_SHA, proposalSha()));

        JsonNode unknown = JSON.readTree(Path.of(ScenarioSearchIntentMatcher.INTENTS_PATH).toFile());
        ((ObjectNode) unknown.path("records").get(0)).put("scenarioId", "HYP-SCENARIO-999");
        assertThrows(RuntimeContractException.class, () -> ScenarioSearchIntentMatcher
                .validateAcceptedIntents(unknown, REV, SEM_SHA, proposalSha()));

        JsonNode mixedRevision = JSON.readTree(Path.of(ScenarioSearchIntentMatcher.INTENTS_PATH).toFile());
        ((ObjectNode) mixedRevision.path("records").get(0)).put("sourceRevision", "1".repeat(40));
        assertThrows(RuntimeContractException.class, () -> ScenarioSearchIntentMatcher
                .validateAcceptedIntents(mixedRevision, REV, SEM_SHA, proposalSha()));

        JsonNode authority = JSON.readTree(Path.of(ScenarioSearchIntentMatcher.INTENTS_PATH).toFile());
        ((ObjectNode) authority.path("records").get(0)).put("authority", "ACCEPTED_PRODUCT_KNOWLEDGE");
        assertThrows(RuntimeContractException.class, () -> ScenarioSearchIntentMatcher
                .validateAcceptedIntents(authority, REV, SEM_SHA, proposalSha()));
    }

    @Test void evaluatorVocabularyInAcceptedTermsFailsClosed() throws Exception {
        JsonNode intents = JSON.readTree(Path.of(ScenarioSearchIntentMatcher.INTENTS_PATH).toFile());
        ((ObjectNode) intents.path("records").get(0)).putArray("aliases").add("gold mapping helper");
        assertThrows(RuntimeContractException.class, () -> ScenarioSearchIntentMatcher
                .validateAcceptedIntents(intents, REV, SEM_SHA, proposalSha()));
    }

    @Test void evidenceBoundaryRejectsTestHelperExternalAndNonProductionSymbols() {
        String testHelper = evidenceDocument("org.springframework.samples.petclinic.owner.OwnerControllerTests",
                "findByLastName", "IMPORTED_SOURCE_ROOT");
        assertThrows(RuntimeContractException.class,
                () -> SfBl002TestBehaviorEvidence.adapt(testHelper.getBytes(), Path.of(".")));
        String external = evidenceDocument("com.example.external.Client", "call", "EXTERNAL_DEPENDENCY_NOT_RESOLVED");
        assertThrows(RuntimeContractException.class,
                () -> SfBl002TestBehaviorEvidence.adapt(external.getBytes(), Path.of(".")));
    }

    @Test void rankingIsDeterministicTieBrokenByEvidenceRefAscending() {
        var index = index(
                candidate("direct-test-reference:0002",
                        "org.springframework.samples.petclinic.vet.VetRepository", "findAll",
                        "VetControllerTests", "testVets", Set.of("vet", "all")),
                candidate("direct-test-reference:0001",
                        "org.springframework.samples.petclinic.owner.OwnerRepository", "findByLastName",
                        "OwnerControllerTests", "testFind", Set.of("owner", "find", "by", "last", "name")),
                candidate("direct-test-reference:0003",
                        "org.springframework.samples.petclinic.owner.OwnerRepository", "count",
                        "OwnerControllerTests", "testCount", Set.of("owner", "find", "by", "last", "name")));
        ObjectNode record = ScenarioSearchIntentMatcher.assignmentRecord("HYP-CAPABILITY-001",
                intent("HYP-CAPABILITY-001", "HYP-SCENARIO-001", "FIND", "OWNER",
                        List.of("last-name-criteria"), List.of("find owner")), REV, DIGESTS, index);
        assertEquals("direct-test-reference:0001", record.path("primaryEvidenceRef").asText());
        assertTrue(record.path("selectionRationale").asText().contains("matched terms [find, last, name, owner]"),
                "matched terms must keep deterministic sorted order");
    }

    @Test void onePrimaryPerProductionIdentityAndNoPositiveOverlapStaysUnresolved() {
        var index = index(
                candidate("direct-test-reference:0001",
                        "org.springframework.samples.petclinic.owner.OwnerRepository", "findByLastName",
                        "OwnerControllerTests", "testFind", Set.of("owner", "find", "by", "last", "name")),
                candidate("direct-test-reference:0002",
                        "org.springframework.samples.petclinic.owner.OwnerRepository", "findByLastName",
                        "PetControllerTests", "testFind", Set.of("owner", "find", "by", "last", "name")),
                candidate("direct-test-reference:0003",
                        "org.springframework.samples.petclinic.vet.VetRepository", "findAll",
                        "VetControllerTests", "testVets", Set.of("vet", "all")));
        ObjectNode dedup = ScenarioSearchIntentMatcher.assignmentRecord("HYP-CAPABILITY-001",
                intent("HYP-CAPABILITY-001", "HYP-SCENARIO-001", "FIND", "OWNER",
                        List.of("last-name-criteria"), List.of("find owner")), REV, DIGESTS, index);
        assertEquals("direct-test-reference:0001", dedup.path("primaryEvidenceRef").asText());

        ObjectNode unresolved = ScenarioSearchIntentMatcher.assignmentRecord("HYP-CAPABILITY-005",
                intent("HYP-CAPABILITY-005", "HYP-SCENARIO-009", "BROWSE", "VET",
                        List.of("specialty-listed"), List.of("browse vet")), REV, DIGESTS,
                index(candidate("direct-test-reference:0001",
                        "org.springframework.samples.petclinic.owner.OwnerRepository", "findByLastName",
                        "OwnerControllerTests", "testFind", Set.of("owner", "find", "by", "last", "name"))));
        assertEquals("UNRESOLVED", unresolved.path("status").asText());
        assertEquals("UNRESOLVED", unresolved.path("primaryEvidenceRef").asText());
        assertTrue(unresolved.path("selectionRationale").asText().contains("UNRESOLVED"));
    }

    @Test void legacyArrayShapedRecordIsRejectedFailClosed() {
        var index = index(candidate("direct-test-reference:0001",
                "org.springframework.samples.petclinic.owner.OwnerRepository", "findByLastName",
                "OwnerControllerTests", "testFind", Set.of("owner", "find", "by", "last", "name")));
        ObjectNode legacy = ScenarioSearchIntentMatcher.assignmentRecord("HYP-CAPABILITY-001",
                intent("HYP-CAPABILITY-001", "HYP-SCENARIO-001", "FIND", "OWNER",
                        List.of("last-name-criteria"), List.of("find owner")), REV, DIGESTS, index);
        legacy.remove("primaryEvidenceRef");
        var legacyArray = legacy.putArray("primaryEvidenceRefs");
        legacyArray.add("direct-test-reference:0001");
        assertThrows(RuntimeContractException.class,
                () -> ScenarioSearchIntentMatcher.validateAssignment(legacy, index, REV, DIGESTS));

        ObjectNode mismatched = ScenarioSearchIntentMatcher.assignmentRecord("HYP-CAPABILITY-001",
                intent("HYP-CAPABILITY-001", "HYP-SCENARIO-001", "FIND", "OWNER",
                        List.of("last-name-criteria"), List.of("find owner")), REV, DIGESTS, index);
        mismatched.put("primaryEvidenceRef", "UNRESOLVED");
        assertThrows(RuntimeContractException.class,
                () -> ScenarioSearchIntentMatcher.validateAssignment(mismatched, index, REV, DIGESTS));
    }

    @Test void immutablePolicyCollectionsRejectMutation() {
        assertThrows(UnsupportedOperationException.class,
                () -> ScenarioSearchIntentMatcher.termFields().add("rationale"));
        assertThrows(UnsupportedOperationException.class,
                () -> ScenarioSearchIntentMatcher.latinTokens("owner find").add("mutated"));
    }

    @Test void recordValidationBindsEveryDigestAndAuthorityFailClosed() {
        var index = index(candidate("direct-test-reference:0001",
                "org.springframework.samples.petclinic.owner.OwnerRepository", "findByLastName",
                "OwnerControllerTests", "testFind", Set.of("owner", "find", "by", "last", "name")));
        ObjectNode record = ScenarioSearchIntentMatcher.assignmentRecord("HYP-CAPABILITY-001",
                intent("HYP-CAPABILITY-001", "HYP-SCENARIO-001", "FIND", "OWNER",
                        List.of("last-name-criteria"), List.of("find owner")), REV, DIGESTS, index);
        ScenarioSearchIntentMatcher.validateAssignment(record, index, REV, DIGESTS);
        record.put("intentDigest", "2".repeat(64));
        assertThrows(RuntimeContractException.class,
                () -> ScenarioSearchIntentMatcher.validateAssignment(record, index, REV, DIGESTS));
        record.put("intentDigest", DIGESTS.intents());
        record.put("testEvidenceDigest", "3".repeat(64));
        assertThrows(RuntimeContractException.class,
                () -> ScenarioSearchIntentMatcher.validateAssignment(record, index, REV, DIGESTS));
        record.put("testEvidenceDigest", EV_SHA);
        record.put("status", "PRIMARY");
        assertThrows(RuntimeContractException.class,
                () -> ScenarioSearchIntentMatcher.validateAssignment(record, index, REV, DIGESTS));
    }

    private static JsonNode intent(String capabilityId, String scenarioId, String action, String entity,
            List<String> conditions, List<String> aliases) {
        ObjectNode node = JSON.createObjectNode();
        node.put("capabilityId", capabilityId).put("scenarioId", scenarioId);
        node.put("action", action).put("entity", entity);
        var conditionNodes = node.putArray("conditions");
        conditions.forEach(conditionNodes::add);
        var aliasNodes = node.putArray("aliases");
        aliases.forEach(aliasNodes::add);
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

    private static String proposalSha() throws Exception {
        return com.featuredeliveryintelligence.fdi.validation.scenarioforward.ScenarioForwardRequestReader.sha256(
                Files.readAllBytes(Path.of(ScenarioSearchIntentMatcher.PROPOSAL_PATH)));
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
        Map<String, String> inputs = new LinkedHashMap<>(ScenarioSearchIntentMatcher.sealedInputs());
        inputs.put(ScenarioSearchIntentMatcher.PROPOSAL_PATH, "");
        inputs.put(SfBl002TestBehaviorEvidence.EVIDENCE_PATH, "");
        for (String path : inputs.keySet()) {
            Path from = Path.of(".").resolve(path), to = root.resolve(path);
            Files.createDirectories(to.getParent());
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

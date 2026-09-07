package com.featuredeliveryintelligence.fdi.reverse.evaluation;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Fail-closed load invariants for the sealed proposal package as seen by the
 * evaluator-only comparison path.
 */
class ReverseSealedPackageTests {

    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String DIGEST_A =
            "5ff8454ae758c8b8ffb1e755ff7c2124efd233388f4f901d8d964b04977c27ca";
    private static final String DIGEST_B =
            "082c621945ef063fec82adb3766348c0ecc888ba71a2f1ed34b147ebfa8bcc8a";
    private static final String DIGEST_C =
            "87b70439f69bb82cee0139b65108ecce6044aff1a4d1b5ce6324933acbeab2d1";

    @Test
    void validSealedPackageLoads() {
        ReverseSealedPackage sealed = ReverseSealedPackage.load(validDocument().getBytes());
        assertEquals("spring-petclinic", sealed.repositoryId());
        assertEquals(REVISION, sealed.canonicalRevision());
        assertEquals(1, sealed.capabilities().size());
        assertEquals(1, sealed.scenarios().size());
        assertEquals(List.of("DELIVERY_HISTORY", "STRUCTURAL", "TEST_BEHAVIOR"),
                List.copyOf(sealed.capabilities().get(0).channels()));
    }

    @Test
    void emptyDocumentFailsClosed() {
        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseSealedPackage.load(new byte[0]));
        assertEquals(ReverseFailure.MISSING_INPUT, failure.failure());
    }

    @Test
    void unsupportedSchemaVersionFailsClosed() {
        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseSealedPackage.load(validDocument().replace("\"schema_version\": \"1\"",
                        "\"schema_version\": \"2\"").getBytes()));
        assertEquals(ReverseFailure.UNSUPPORTED_SCHEMA_VERSION, failure.failure());
    }

    @Test
    void nonProposalAuthorityFailsClosed() {
        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseSealedPackage.load(validDocument().replace(
                        "\"authority\": \"PROPOSAL_ONLY\"", "\"authority\": \"ACCEPTED\"").getBytes()));
        assertEquals(ReverseFailure.AUTHORITY_VIOLATION, failure.failure());
    }

    @Test
    void semanticPublicationAllowedFailsClosed() {
        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseSealedPackage.load(validDocument().replace(
                                "\"semantic_publication_allowed\": false",
                                "\"semantic_publication_allowed\": true")
                        .getBytes()));
        assertEquals(ReverseFailure.AUTHORITY_VIOLATION, failure.failure());
    }

    @Test
    void reviewedScenarioStatusFailsClosed() {
        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseSealedPackage.load(validDocument().replace(
                        "\"scenario_status\": \"UNREVIEWED\"",
                        "\"scenario_status\": \"ACCEPTED\"").getBytes()));
        assertEquals(ReverseFailure.AUTHORITY_VIOLATION, failure.failure());
    }

    @Test
    void scenarioReferencingUnknownCapabilityFailsClosed() {
        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseSealedPackage.load(validDocument().replace(
                        "\"capability_id\": \"HYP-CAPABILITY-0001\"",
                        "\"capability_id\": \"HYP-CAPABILITY-9999\"").getBytes()));
        assertEquals(ReverseFailure.MISSING_INPUT, failure.failure());
    }

    @Test
    void duplicateCapabilityIdentityFailsClosed() {
        String document = validDocument().replace(
                "\"capabilities\": [ " + capabilityJson(),
                "\"capabilities\": [ " + capabilityJson() + ", " + capabilityJson());
        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseSealedPackage.load(document.getBytes()));
        assertEquals(ReverseFailure.DUPLICATE_IDENTITY, failure.failure());
    }

    @Test
    void identifierBearingTitleFailsClosed() {
        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseSealedPackage.load(validDocument()
                        .replace("\"title\": \"Owner controller\"", "\"title\": \"ownerController\"")
                        .getBytes()));
        assertEquals(ReverseFailure.SCENARIO_TEXT_IDENTIFIER, failure.failure());
    }

    @Test
    void malformedCanonicalRevisionFailsClosed() {
        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseSealedPackage.load(validDocument().replace(REVISION, "not-a-revision")
                        .getBytes()));
        assertEquals(ReverseFailure.REVISION_MISMATCH, failure.failure());
    }

    @Test
    void malformedCitationDigestFailsClosed() {
        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseSealedPackage.load(validDocument().replace(DIGEST_A, "bogus").getBytes()));
        assertEquals(ReverseFailure.DIGEST_MISMATCH, failure.failure());
    }

    private static String validDocument() {
        return "{"
                + "\"schema_version\": \"1\","
                + "\"repository_id\": \"spring-petclinic\","
                + "\"canonical_revision\": \"" + REVISION + "\","
                + "\"capabilities\": [ " + capabilityJson() + " ],"
                + "\"scenarios\": [ " + scenarioJson() + " ],"
                + "\"evidence_gaps\": []"
                + "}";
    }

    private static String capabilityJson() {
        return "{"
                + "\"id\": \"HYP-CAPABILITY-0001\","
                + "\"title\": \"Owner controller\","
                + "\"citations\": ["
                + citationJson("STRUCTURAL", "/nodes/0", DIGEST_A) + ","
                + citationJson("TEST_BEHAVIOR", "/test_files/0", DIGEST_B) + ","
                + citationJson("DELIVERY_HISTORY", "/changed_paths/0", DIGEST_C)
                + "],"
                + "\"source_revision\": \"" + REVISION + "\","
                + "\"contributing_evidence_digests\": [\"" + DIGEST_A + "\"],"
                + "\"inference_rationale\": \"test rationale\","
                + "\"limitations\": [\"test limitation\"],"
                + "\"confidence\": 5000,"
                + "\"authority\": \"PROPOSAL_ONLY\","
                + "\"semantic_publication_allowed\": false"
                + "}";
    }

    private static String scenarioJson() {
        return "{"
                + "\"id\": \"HYP-SCENARIO-0001\","
                + "\"capability_id\": \"HYP-CAPABILITY-0001\","
                + "\"title\": \"Find owner\","
                + "\"given\": \"a recorded test exercises the system\","
                + "\"when\": \"find owner\","
                + "\"then\": \"the recorded expectations hold\","
                + "\"citations\": [ " + citationJson("TEST_BEHAVIOR", "/test_files/0/test_methods/0", DIGEST_B) + " ],"
                + "\"source_revision\": \"" + REVISION + "\","
                + "\"contributing_evidence_digests\": [\"" + DIGEST_B + "\"],"
                + "\"inference_rationale\": \"test rationale\","
                + "\"limitations\": [\"test limitation\"],"
                + "\"confidence\": 6000,"
                + "\"scenario_status\": \"UNREVIEWED\","
                + "\"authority\": \"PROPOSAL_ONLY\","
                + "\"semantic_publication_allowed\": false"
                + "}";
    }

    private static String citationJson(String channel, String ref, String digest) {
        return "{"
                + "\"channel\": \"" + channel + "\","
                + "\"observation_ref\": \"" + ref + "\","
                + "\"evidence_digest\": \"" + digest + "\""
                + "}";
    }
}

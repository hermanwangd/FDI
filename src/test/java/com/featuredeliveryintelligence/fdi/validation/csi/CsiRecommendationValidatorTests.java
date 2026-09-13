package com.featuredeliveryintelligence.fdi.validation.csi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsiRecommendationValidatorTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String REVISION = "a".repeat(40);

    @Test
    void evidenceObservationsDoNotChangeSemanticDuplicateKey() {
        ObjectNode first = validRecord("evidence-1", "validation/a.json");
        ObjectNode second = validRecord("evidence-2", "provider:review/run-2");

        CsiValidationReport firstReport = new CsiRecommendationValidator().validate(first);
        CsiValidationReport secondReport = new CsiRecommendationValidator().validate(second);

        assertEquals("VALID", firstReport.status());
        assertEquals(firstReport.duplicateKey(), secondReport.duplicateKey());
        assertEquals(64, firstReport.duplicateKey().length());
    }

    @Test
    void canonicalKeyPreservesInternalWhitespaceNegationAndNormalizesUnicodeAndOuterWhitespace() {
        ObjectNode composed = validRecord("evidence-1", "validation/a.json");
        composed.put("insufficiency", "  Caf\u00e9 is not safe\nfor arrays  ");
        ObjectNode decomposed = validRecord("evidence-2", "validation/b.json");
        decomposed.put("insufficiency", "Cafe\u0301 is not safe\nfor arrays");
        ObjectNode affirmative = decomposed.deepCopy();
        affirmative.put("insufficiency", "Caf\u00e9 is safe\nfor arrays");

        String key = new CsiRecommendationValidator().validate(composed).duplicateKey();
        assertEquals("d7f51e3a10449eabbcdcbbcf45831306ce6766fa34a0c79256077229326351e0", key);
        assertEquals(key, new CsiRecommendationValidator().validate(decomposed).duplicateKey());
        assertNotEquals(key, new CsiRecommendationValidator().validate(affirmative).duplicateKey());
    }

    @Test
    void missingDurableEvidenceIsBlockedWithoutInventingProvenance() {
        ObjectNode record = validRecord("HERM-518", null);

        CsiValidationReport report = new CsiRecommendationValidator().validate(record);

        assertEquals("BLOCKED", report.status());
        assertTrue(report.issues().contains("origin_evidence[0].durable_ref is missing"));
        assertFalse(report.duplicateKey().isBlank());
    }

    @Test
    void unknownClassificationAndAuthorityBearingFieldsFailClosed() {
        ObjectNode record = validRecord("evidence-1", "validation/a.json");
        record.put("tag", "SECURITY");
        record.put("dispatch_authorized", true);

        CsiValidationReport report = new CsiRecommendationValidator().validate(record);

        assertEquals("INVALID", report.status());
        assertTrue(report.issues().stream().anyMatch(v -> v.contains("tag")));
        assertTrue(report.issues().stream().anyMatch(v -> v.contains("authority-bearing")));
    }

    @Test
    void duplicateEvidenceIdentityAndAbbreviatedCandidateFailClosed() {
        ObjectNode record = validRecord("same", "validation/a.json");
        record.withArray("origin_evidence").add(evidence("same", "INDEPENDENT_REVIEW", "validation/b.json"));
        record.put("candidate_revision", "abc1234");

        CsiValidationReport report = new CsiRecommendationValidator().validate(record);

        assertEquals("INVALID", report.status());
        assertTrue(report.issues().contains("origin_evidence identities must be unique"));
        assertTrue(report.issues().contains("candidate_revision must be a full 40-character Git revision or N/A with reason"));
    }

    @Test
    void readyForReviewDoesNotRequireReceiverReadbackButReviewCompleteDoes() {
        ObjectNode ready = validRecord("evidence-1", "validation/a.json");
        ready.set("handoff", handoff("READY_FOR_REVIEW", false));
        assertEquals("VALID", new CsiRecommendationValidator().validate(ready).status());

        ObjectNode complete = validRecord("evidence-1", "validation/a.json");
        complete.set("handoff", handoff("REVIEW_COMPLETE", false));
        CsiValidationReport report = new CsiRecommendationValidator().validate(complete);
        assertEquals("BLOCKED", report.status());
        assertTrue(report.issues().contains("REVIEW_COMPLETE requires receiver_readback"));
    }

    @Test
    void kpiStatesRequireHonestFieldsAndComparableSamples() {
        ObjectNode record = validRecord("evidence-1", "validation/a.json");
        ArrayNode samples = record.putArray("affected_kpis");
        ObjectNode unknown = samples.addObject();
        unknown.put("metric", "recurrence").put("status", "UNKNOWN");
        unknown.putNull("value").putNull("numerator").putNull("denominator");
        unknown.put("sample_count", 0).put("window_start", "2026-09-13T00:00:00Z")
                .put("window_end", "2026-09-13T01:00:00Z").put("category", "DELIVERY")
                .put("size_band", "S").put("measurement_revision", REVISION)
                .put("source_evidence", "validation/kpi.json").putNull("baseline_identity");
        assertEquals("VALID", new CsiRecommendationValidator().validate(record).status());

        unknown.put("value", 0);
        CsiValidationReport invalid = new CsiRecommendationValidator().validate(record);
        assertEquals("INVALID", invalid.status());
        assertTrue(invalid.issues().contains("affected_kpis[0] UNKNOWN requires null value"));
    }

    @Test
    void fixedCanonicalRecordsValidateWithoutInventingMissingEvidence() throws Exception {
        for (int number = 1; number <= 4; number++) {
            ObjectNode record = (ObjectNode) JSON.readTree(java.nio.file.Files.readAllBytes(
                    java.nio.file.Path.of("validation/software-factory/sf-bl007/canonical/CSI-REC-%03d.json".formatted(number))));
            CsiValidationReport report = new CsiRecommendationValidator().validate(record);
            assertEquals(number <= 2 ? "VALID" : "BLOCKED", report.status(), "CSI-REC-%03d".formatted(number));
        }
    }

    private static ObjectNode validRecord(String evidenceId, String durableRef) {
        ObjectNode record = JSON.createObjectNode();
        record.put("recommendation_id", "CSI-REC-001");
        record.put("disposition", "RECOMMENDED_NOT_SELECTED");
        record.put("tag", "CODE");
        ArrayNode evidence = record.putArray("origin_evidence");
        evidence.add(evidence(evidenceId, "INDEPENDENT_REVIEW", durableRef));
        record.put("candidate_revision", REVISION);
        record.put("execution_identity", "N/A: finding arose in independent review");
        record.put("verdict_identity", "review/run-1:FAIL");
        record.put("affected_requirement", "EVID-001");
        record.put("insufficiency", "unsafe resolution");
        record.put("proposed_control", "fail closed before successful return");
        record.putArray("affected_kpis");
        record.put("revision_route", "SF-BL-005:T3");
        return record;
    }

    private static ObjectNode evidence(String id, String type, String durableRef) {
        ObjectNode evidence = JSON.createObjectNode();
        evidence.put("identity", id).put("origin_type", type);
        if (durableRef == null) evidence.putNull("durable_ref");
        else evidence.put("durable_ref", durableRef);
        return evidence;
    }

    private static ObjectNode handoff(String gate, boolean receiverReadback) {
        ObjectNode handoff = JSON.createObjectNode();
        handoff.put("gate", gate).put("base_revision", REVISION).put("candidate_revision", REVISION)
                .put("envelope_identity", "N/A: design-only handoff")
                .put("command_log", "validation/commands.log").put("digest_manifest", "validation/manifest.sha256")
                .put("attempt_count", 1).put("elapsed_ms", 10).put("token_accounting", "N/A: local tool did not expose tokens");
        if ("REVIEW_COMPLETE".equals(gate)) {
            handoff.put("reviewer_identity", "reviewer-1").put("reviewed_digest", "b".repeat(64))
                    .put("verdict", "PASS").put("limitations", "none");
            if (receiverReadback) handoff.put("receiver_readback", "reviewer confirmed digest");
        }
        return handoff;
    }
}

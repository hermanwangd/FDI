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
        ObjectNode first = validRecord("evidence-1", durable("validation/a.json"));
        ObjectNode second = validRecord("evidence-2", "provider:review/run-2");

        CsiValidationReport firstReport = new CsiRecommendationValidator().validate(first);
        CsiValidationReport secondReport = new CsiRecommendationValidator().validate(second);

        assertEquals("VALID", firstReport.status());
        assertEquals(firstReport.duplicateKey(), secondReport.duplicateKey());
        assertEquals(64, firstReport.duplicateKey().length());
    }

    @Test
    void canonicalKeyPreservesInternalWhitespaceNegationAndNormalizesUnicodeAndOuterWhitespace() {
        ObjectNode composed = validRecord("evidence-1", durable("validation/a.json"));
        composed.put("insufficiency", "  Caf\u00e9 is not safe\nfor arrays  ");
        ObjectNode decomposed = validRecord("evidence-2", durable("validation/b.json"));
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
    void proseDurableReferenceAndMissingDuplicateKeyFailClosed() {
        ObjectNode record = validRecord("evidence-1", "trust me");
        record.remove("duplicate_key");
        CsiValidationReport report = new CsiRecommendationValidator().validate(record);
        assertEquals("INVALID", report.status());
        assertTrue(report.issues().contains("duplicate_key is required"));
        assertTrue(report.issues().contains("origin_evidence[0].durable_ref is not immutable"));
    }

    @Test
    void unknownClassificationAndAuthorityBearingFieldsFailClosed() {
        ObjectNode record = validRecord("evidence-1", durable("validation/a.json"));
        record.put("tag", "SECURITY");
        record.put("dispatch_authorized", true);

        CsiValidationReport report = new CsiRecommendationValidator().validate(record);

        assertEquals("INVALID", report.status());
        assertTrue(report.issues().stream().anyMatch(v -> v.contains("tag")));
        assertTrue(report.issues().stream().anyMatch(v -> v.contains("authority-bearing")));
    }

    @Test
    void duplicateEvidenceIdentityAndAbbreviatedCandidateFailClosed() {
        ObjectNode record = validRecord("same", durable("validation/a.json"));
        record.withArray("origin_evidence").add(evidence("same", "INDEPENDENT_REVIEW", "validation/b.json"));
        record.put("candidate_revision", "abc1234");

        CsiValidationReport report = new CsiRecommendationValidator().validate(record);

        assertEquals("INVALID", report.status());
        assertTrue(report.issues().contains("origin_evidence identities must be unique"));
        assertTrue(report.issues().contains("candidate_revision must be a full 40-character Git revision or N/A with reason"));
    }

    @Test
    void readyForReviewDoesNotRequireReceiverReadbackButReviewCompleteDoes() {
        ObjectNode ready = validRecord("evidence-1", durable("validation/a.json"));
        ready.set("handoff", handoff("READY_FOR_REVIEW", false));
        assertEquals("VALID", new CsiRecommendationValidator().validate(ready).status());

        ObjectNode complete = validRecord("evidence-1", durable("validation/a.json"));
        complete.set("handoff", handoff("REVIEW_COMPLETE", false));
        CsiValidationReport report = new CsiRecommendationValidator().validate(complete);
        assertEquals("BLOCKED", report.status());
        assertTrue(report.issues().contains("REVIEW_COMPLETE requires receiver_readback"));
    }

    @Test
    void handoffRejectsMovingRevisionsAndMissingArtifactDigests() {
        ObjectNode record = validRecord("evidence-1", durable("validation/a.json"));
        ObjectNode handoff = handoff("READY_FOR_REVIEW", false);
        handoff.put("base_revision", "main");
        handoff.remove("input_artifact_digests");
        record.set("handoff", handoff);
        CsiValidationReport report = new CsiRecommendationValidator().validate(record);
        assertEquals("INVALID", report.status());
        assertTrue(report.issues().contains("handoff.base_revision must be a full 40-character Git revision"));
        assertTrue(report.issues().contains("handoff.input_artifact_digests must be a non-empty digest array"));
    }

    @Test
    void kpiStatesRequireHonestFieldsAndComparableSamples() {
        ObjectNode record = validRecord("evidence-1", durable("validation/a.json"));
        ArrayNode samples = record.putArray("affected_kpis");
        ObjectNode unknown = samples.addObject();
        unknown.put("metric", "recurrence").put("status", "UNKNOWN");
        unknown.putNull("value").putNull("numerator").putNull("denominator");
        unknown.put("sample_count", 0).put("minimum_sample_count", 2).put("window_start", "2026-09-13T00:00:00Z")
                .put("window_end", "2026-09-13T01:00:00Z").put("category", "DELIVERY")
                .put("size_band", "S").put("measurement_revision", REVISION)
                .put("source_evidence", durable("validation/kpi.json")).putNull("baseline_identity");
        assertEquals("VALID", new CsiRecommendationValidator().validate(record).status());

        unknown.put("value", 0);
        CsiValidationReport invalid = new CsiRecommendationValidator().validate(record);
        assertEquals("INVALID", invalid.status());
        assertTrue(invalid.issues().contains("affected_kpis[0] UNKNOWN requires null value"));
    }

    @Test
    void emptyKpisReversedWindowAndSparseObservedSampleFailClosed() {
        ObjectNode empty = validRecord("evidence-1", durable("validation/a.json"));
        empty.putArray("affected_kpis");
        assertEquals("INVALID", new CsiRecommendationValidator().validate(empty).status());

        ObjectNode record = validRecord("evidence-1", durable("validation/a.json"));
        ObjectNode sample = (ObjectNode) record.withArray("affected_kpis").get(0);
        sample.put("status", "OBSERVED").put("value", 1).put("numerator", 1).put("denominator", 1)
                .put("sample_count", 1).put("minimum_sample_count", 2)
                .put("window_start", "2026-09-14T00:00:00Z").put("window_end", "2026-09-13T00:00:00Z");
        sample.putNull("baseline_identity");
        CsiValidationReport report = new CsiRecommendationValidator().validate(record);
        assertEquals("INVALID", report.status());
        assertTrue(report.issues().contains("affected_kpis[0] window_start must not follow window_end"));
        assertTrue(report.issues().contains("affected_kpis[0] sparse or baseline-less sample must be INSUFFICIENT_SAMPLE"));
    }

    @Test
    void unknownTagMustRemainParkedWithoutConcreteRoute() {
        ObjectNode record = validRecord("evidence-1", durable("validation/a.json"));
        record.put("tag", "UNKNOWN").put("disposition", "REJECTED").put("revision_route", "SF-BL-007");
        record.put("duplicate_key", new CsiRecommendationValidator().duplicateKey(
                record.path("affected_requirement").asText(), record.path("insufficiency").asText(),
                record.path("proposed_control").asText(), record.path("revision_route").asText()));
        CsiValidationReport report = new CsiRecommendationValidator().validate(record);
        assertEquals("INVALID", report.status());
        assertTrue(report.issues().contains("UNKNOWN tag must remain RECOMMENDED_NOT_SELECTED with revision_route UNKNOWN"));
    }

    @Test
    void priorRecordComparisonRejectsEvidenceReplacementOrReordering() {
        ObjectNode prior = validRecord("evidence-1", durable("validation/a.json"));
        prior.withArray("origin_evidence").add(evidence("evidence-2", "DELIVERY", durable("validation/b.json")));
        ObjectNode current = prior.deepCopy();
        current.withArray("origin_evidence").remove(0);
        CsiValidationReport report = new CsiRecommendationValidator().validate(current, prior);
        assertEquals("INVALID", report.status());
        assertTrue(report.issues().contains("origin_evidence must preserve the prior append-only prefix"));
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
        ObjectNode sample = record.putArray("affected_kpis").addObject();
        sample.put("metric", "review_escape").put("status", "UNKNOWN");
        sample.putNull("value").putNull("numerator").putNull("denominator");
        sample.put("sample_count", 0).put("minimum_sample_count", 2)
                .put("window_start", "2026-09-13T00:00:00Z").put("window_end", "2026-09-13T01:00:00Z")
                .put("category", "CODE").put("size_band", "S").put("measurement_revision", REVISION)
                .put("source_evidence", durable("validation/kpi.json")).putNull("baseline_identity");
        record.put("revision_route", "SF-BL-005:T3");
        record.put("duplicate_key", new CsiRecommendationValidator().duplicateKey(
                record.path("affected_requirement").asText(), record.path("insufficiency").asText(),
                record.path("proposed_control").asText(), record.path("revision_route").asText()));
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
                .put("command_exit_status", 0).put("attempt_count", 1).put("elapsed_ms", 10)
                .put("token_accounting", "N/A: local tool did not expose tokens");
        handoff.putArray("input_artifact_digests").add("b".repeat(64));
        handoff.putArray("output_artifact_digests").add("c".repeat(64));
        if ("REVIEW_COMPLETE".equals(gate)) {
            handoff.put("reviewer_identity", "reviewer-1").put("reviewed_digest", "b".repeat(64))
                    .put("verdict", "PASS").put("limitations", "none");
            if (receiverReadback) handoff.put("receiver_readback", "reviewer confirmed digest");
        }
        return handoff;
    }

    private static String durable(String path) {
        return "sha256:" + "d".repeat(64) + ":" + path;
    }
}

package com.featuredeliveryintelligence.fdi.orchestration;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.featuredeliveryintelligence.fdi.application.Dev204Cli;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Local synthetic receipt assertions; none is a native acceptance receipt. */
class CurrentWorkPackageAuditTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String FREEZE = "docs/superpowers/plans/2026-10-08-current-work-package-qa-freeze-r2/";

    @Test void u01CurrentIndependentOrderedLedger() throws Exception { runCase("U01", null); }
    @Test void u02MissingApproval() throws Exception { runCase("U02", "APPROVAL_MISSING"); }
    @Test void u03ReviseDoesNotPermitAction() throws Exception {
        runCase("U03", "ACTION_NOT_APPROVED"); runReviewSupplement("U03");
        runNativeParentSupplement("U03");
    }
    @Test void u04SelfReviewCannotApprove() throws Exception { runCase("U04", "REVIEW_INDEPENDENCE_MISMATCH"); }
    @Test void u05ForeignMissionOrCase() throws Exception { runCase("U05", "PACKAGE_IDENTITY_MISMATCH"); }
    @Test void u06ForeignActorOrRole() throws Exception { runCase("U06", "ACTOR_ROLE_MISMATCH"); }
    @Test void u07NativeDeclaredAndDigestBindings() throws Exception {
        runCase("U07", "PLAN_BINDING_MISMATCH"); runNativeParentSupplement("U07");
        runPreparationSupplement("U07", "2026-10-08-a1-stored-byte-binding-freeze-r7");
    }
    @Test void u08EarlyOrAmbiguousAction() throws Exception { runCase("U08", "ACTION_ORDER_UNVERIFIED"); }
    @Test void u09ChangedInputOrMissingAnalysis() throws Exception { runCase("U09", "INPUT_OR_DEPENDENCY_MISMATCH"); }
    @Test void u10UnmetCondition() throws Exception { runCase("U10", "APPROVAL_CONDITIONS_UNRESOLVED"); }
    @Test void u11MissingOrTruncatedOperationTrace() throws Exception {
        runCase("U11", "ACTION_EVIDENCE_INCOMPLETE"); runReviewSupplement("U11");
        runPreparationSupplement("U11", "2026-10-08-a1-preparation-boundary-correction-r6");
        runPreparationSupplement("U11", "2026-10-08-audit-grant-raw-receipt-repair-r10");
    }
    @Test void u12ChangedActionNeedsNewReview() throws Exception { runCase("U12", "PACKAGE_IDENTITY_MISMATCH"); }
    @Test void u13ConcreteRetryAndReconciliationLedgers() throws Exception { runConcreteCase("U13"); }
    @Test void u14ConcreteScratchCompletionBoundaries() throws Exception {
        runConcreteCase("U14"); runPreparationSupplement("U14", "2026-10-08-a1-preparation-boundary-correction-r6");
        runPreparationSupplement("U14", "2026-10-08-audit-grant-raw-receipt-repair-r10");
    }
    @Test void u15ProtocolLabelCannotExemptSourceOrEscape() throws Exception {
        runCase("U15", "PROTOCOL_SCOPE_OR_KIND_MISMATCH");
        runPreparationSupplement("U15", "2026-10-08-a1-preparation-boundary-correction-r6");
        runPreparationSupplement("U15", "2026-10-08-audit-grant-raw-receipt-repair-r10");
    }
    @Test void u16OldOrSyntheticEvidenceCannotSubstitute() throws Exception {
        runCase("U16", "EVIDENCE_KIND_OR_COHORT_MISMATCH"); runReviewSupplement("U16");
    }

    @Test void c01SelectedReportAuditCombinesChecksReadOnly() throws Exception {
        ObjectNode report = report();
        report.put("work_package_amendment_selected", true);
        report.set("work_packages", base());
        ObjectNode before = report.deepCopy();
        var result = OrchestratorReportAudit.audit(report, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION);
        assertEquals("CLEAN", result.status(), result.findings().toString());
        assertTrue(result.facts().stream().anyMatch(f -> f.code().equals("CURRENT_WORK_PACKAGE_LEDGER_CHECKED")));
        assertEquals(before, report);
    }

    @Test void c02SelectedAmendmentRequiresLedger() throws Exception {
        for (boolean explicitNull : List.of(false, true)) {
            ObjectNode report = report();
            report.put("work_package_amendment_selected", true);
            if (explicitNull) report.putNull("work_packages");
            var result = OrchestratorReportAudit.audit(report, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION);
            assertTrue(codes(result).contains("WORK_PACKAGE_LEDGER_MISSING"));
            assertEquals(1, result.exitStatus());
        }
    }

    @Test void c03UnselectedLegacyReportKeepsOriginalBehavior() throws Exception {
        ObjectNode report = report();
        var prior = OrchestratorReportAudit.audit(report, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION);
        report.put("work_package_amendment_selected", false);
        report.putNull("work_packages");
        assertEquals(prior, OrchestratorReportAudit.audit(report, OrchestratorReportAudit.PublicationPhase.PREPUBLICATION));
        assertEquals("CLEAN", prior.status());
    }

    @Test void c04ParserFirstLinePreservedAndPassAloneDoesNotApprove() throws Exception {
        String overlay = Files.readString(root().resolve("engcim/swarm/instructions/ROLE-GUIDANCE.md"))
            + Files.readString(root().resolve("engcim/swarm/instructions/reviewer/source.json"));
        assertTrue(overlay.contains("判定：PASS（revision N）"));
        assertTrue(overlay.contains("planDecision: APPROVE|REVISE"));
        ObjectNode ledger = base();
        ObjectNode decision = (ObjectNode) ledger.withArray("decisions").get(0);
        decision.put("verdict_first_line", "判定：PASS（revision 2）");
        decision.put("decision", "");
        assertTrue(codes(audit(ledger)).contains("ACTION_NOT_APPROVED"));
        assertEquals(1, ledger.path("plans").get(0).path("native_revision").asInt());
        assertEquals(2, ledger.path("plans").get(0).path("declared_revision").asInt());
        runNativeParentSupplement("C04");
    }

    @Test void c05MissingBodyConflictingReceiptsAndAckOnlyDoNotApprove() throws Exception {
        ObjectNode missing = base();
        ((ObjectNode) missing.withArray("plans").get(0)).remove("stored_body");
        assertTrue(codes(audit(missing)).contains("PLAN_BINDING_MISMATCH"));
        ObjectNode conflicting = base();
        ObjectNode copy = conflicting.withArray("decisions").get(0).deepCopy();
        copy.put("decision", "REVISE");
        conflicting.withArray("decisions").add(copy);
        assertTrue(codes(audit(conflicting)).contains("CONFLICTING_RECEIPTS"));
        ObjectNode ack = base();
        ((ObjectNode) ack.withArray("decisions").get(0)).put("decision", "COMMENT_ACK");
        assertTrue(codes(audit(ack)).contains("ACTION_NOT_APPROVED"));
    }

    @Test void c06ExistingCliSerializesNonzeroAndRejectsMalformedLedger(@TempDir Path tmp) throws Exception {
        ObjectNode report = report();
        report.put("work_package_amendment_selected", true);
        Path file = tmp.resolve("evidence.json");
        Files.writeString(file, JSON.writeValueAsString(report));
        byte[] before = Files.readAllBytes(file);
        assertThrows(IllegalStateException.class, () -> Dev204Cli.handles(new String[] {
            "dev204-report-audit", "--phase", "prepublication", "--evidence-file", file.toString()}));
        assertArrayEquals(before, Files.readAllBytes(file));
        ObjectNode malformed = base();
        malformed.put("operations", "not-an-array");
        assertTrue(codes(audit(malformed)).contains("LEDGER_INVALID"));
        runReviewSupplement("C06");
    }

    private void runCase(String id, String expected) throws Exception {
        JsonNode entry = caseEntry(id);
        JsonNode mutation = entry.path("mutation");
        if (mutation.has("variants")) {
            for (JsonNode variant : mutation.path("variants")) checkMutation(variant, expected);
        } else checkMutation(mutation, expected);
    }

    private void checkMutation(JsonNode mutation, String expected) throws Exception {
        ObjectNode ledger = base();
        mutation.fields().forEachRemaining(e -> setPath(ledger, e.getKey(), e.getValue()));
        var result = audit(ledger);
        if (expected == null) assertEquals("CLEAN", result.status(), result.findings().toString());
        else {
            assertEquals("FINDINGS", result.status());
            assertTrue(codes(result).contains(expected), result.findings().toString());
        }
    }

    private void runConcreteCase(String id) throws Exception {
        for (JsonNode variant : caseEntry(id).path("variants")) {
            JsonNode expected = variant.path("expected");
            var result = audit(variant.path("input"));
            assertEquals(expected.path("local_audit_status").asText(), result.status(), variant.path("variant_id").asText());
            assertEquals(strings(expected.path("finding_codes")), codes(result), variant.path("variant_id").asText());
            Set<String> facts = result.facts().stream().map(OrchestratorReportAudit.Fact::code).collect(Collectors.toSet());
            assertTrue(facts.containsAll(strings(expected.path("required_fact_codes"))), result.facts().toString());
            assertFalse(facts.contains("ZERO_FILE_WRITES"));
        }
    }

    private void runReviewSupplement(String caseId) throws Exception {
        runSupplement(caseId, "supplemental-review-r3");
    }

    private void runNativeParentSupplement(String caseId) throws Exception {
        runSupplement(caseId, "supplemental-native-parent-r4");
    }

    private void runPreparationSupplement(String caseId, String directory) throws Exception {
        JsonNode supplement = JSON.readTree(root().resolve("docs/superpowers/plans/" + directory + "/fixtures/assertions.json").toFile());
        List<String> failures = new java.util.ArrayList<>();
        int assessed = 0;
        for (JsonNode variant : supplement.path("assertions")) {
            if (!variant.path("case_id").asText().equals(caseId)) continue;
            assessed++;
            var result = audit(variant.path("input"));
            String name = variant.path("variant_id").asText();
            JsonNode expected = variant.path("expected");
            Set<String> facts = result.facts().stream().map(OrchestratorReportAudit.Fact::code).collect(Collectors.toSet());
            System.out.println("PREPARATION_SUPPLEMENT " + name + " " + result.status() + " " + codes(result));
            if (!expected.path("local_audit_status").asText().equals(result.status())) failures.add(name + " status " + result.status());
            if (!strings(expected.path("finding_codes")).equals(codes(result))) failures.add(name + " findings " + codes(result));
            if (!facts.containsAll(strings(expected.path("required_fact_codes")))) failures.add(name + " missing facts " + facts);
            if (facts.stream().anyMatch(strings(expected.path("forbidden_fact_codes"))::contains)) failures.add(name + " forbidden facts " + facts);
        }
        assertTrue(assessed > 0, "Pinned case assertions must not be silently skipped");
        assertTrue(failures.isEmpty(), failures.toString());
    }

    private void runSupplement(String caseId, String directory) throws Exception {
        JsonNode supplement = JSON.readTree(root().resolve(
            "validation/rc10/current-work-package-candidate/" + directory + "/assertions.json").toFile());
        List<String> failures = new java.util.ArrayList<>();
        for (JsonNode variant : supplement.path("variants")) {
            if (!variant.path("case").asText().equals(caseId)) continue;
            var result = audit(variant.path("input"));
            String name = variant.path("variant").asText();
            System.out.println("CURRENT_PACKAGE_SUPPLEMENT " + directory + " " + caseId + " " + name + " " + result.status());
            if (!variant.path("status").asText().equals(result.status())) failures.add(name + " status " + result.status());
            if (!codes(result).containsAll(strings(variant.path("required_findings")))) failures.add(name + " findings " + codes(result));
            Set<String> facts = result.facts().stream().map(OrchestratorReportAudit.Fact::code).collect(Collectors.toSet());
            if (!facts.containsAll(strings(variant.path("required_facts")))) failures.add(name + " facts " + facts);
        }
        assertTrue(failures.isEmpty(), failures.toString());
    }

    private static OrchestratorReportAudit.AuditResult audit(JsonNode ledger) throws Exception {
        JsonNode before = ledger.deepCopy();
        OrchestratorReportAudit.AuditResult result;
        try {
            Class<?> helper = Class.forName("com.featuredeliveryintelligence.fdi.orchestration.CurrentWorkPackageAudit");
            result = (OrchestratorReportAudit.AuditResult) helper.getMethod("audit", JsonNode.class).invoke(null, ledger);
        } catch (ClassNotFoundException e) {
            return fail("CurrentWorkPackageAudit is absent: frozen current-package behavior is not implemented.");
        } catch (InvocationTargetException e) {
            throw new AssertionError("read-only audit threw instead of reporting supplied receipt findings", e.getCause());
        }
        assertEquals(before, ledger, "Audit must preserve supplied receipt nodes");
        System.out.println("CURRENT_PACKAGE_LOCAL " + result.status() + " " + result.findings());
        return result;
    }

    private static Set<String> codes(OrchestratorReportAudit.AuditResult result) {
        return result.findings().stream().map(OrchestratorReportAudit.Finding::code).collect(Collectors.toSet());
    }

    private static Set<String> strings(JsonNode values) {
        Set<String> result = new java.util.HashSet<>();
        values.forEach(v -> result.add(v.asText()));
        return result;
    }

    private static ObjectNode base() throws Exception { return fixtures().path("base").deepCopy(); }
    private static JsonNode fixtures() throws Exception {
        return JSON.readTree(root().resolve(FREEZE + "fixtures/ledger-fixtures.json").toFile());
    }
    private static JsonNode caseEntry(String id) throws Exception {
        for (JsonNode entry : fixtures().path("cases")) if (entry.path("id").asText().equals(id)) return entry;
        throw new AssertionError("Missing frozen case " + id);
    }
    private static ObjectNode report() throws Exception {
        var factory = OrchestratorReportAuditTest.class.getDeclaredMethod("validEvidence");
        factory.setAccessible(true);
        return (ObjectNode) factory.invoke(null);
    }
    private static Path root() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve(FREEZE + "FIXTURE-PINS.json"))) return p;
        }
        throw new AssertionError("Frozen r2 fixtures not found");
    }
    private static void setPath(ObjectNode node, String path, JsonNode value) {
        String[] keys = path.replace("[", ".").replace("]", "").split("\\.");
        JsonNode parent = node;
        for (int i = 0; i < keys.length - 1; i++) {
            parent = parent.isArray() ? parent.get(Integer.parseInt(keys[i])) : parent.get(keys[i]);
        }
        String key = keys[keys.length - 1];
        if (parent instanceof ArrayNode array) array.set(Integer.parseInt(key), value.deepCopy());
        else ((ObjectNode) parent).set(key, value.deepCopy());
    }
}

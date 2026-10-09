package com.featuredeliveryintelligence.fdi.orchestration;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Local preparation checks with synthetic receipts; never dispatches or proves native isolation. */
class NativeValidationPreparationTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ASSETS = "engcim/swarm/tests/agents/four-role/";

    @Test void canonicalActorTasksDeriveOnlyFromOriginalInputs() throws Exception {
        validateCanonicalContract(contract());
    }

    @Test void duplicateOrMissingProfessionalCaseCannotPassPreparation() throws Exception {
        ObjectNode duplicate = contract().deepCopy();
        ((com.fasterxml.jackson.databind.node.ArrayNode) duplicate.path("actorTasks"))
                .set(1, duplicate.path("actorTasks").get(0).deepCopy());
        assertThrows(AssertionError.class, () -> validateCanonicalContract(duplicate));
        ObjectNode missing = contract().deepCopy();
        ((com.fasterxml.jackson.databind.node.ArrayNode) missing.path("actorTasks")).remove(1);
        assertThrows(AssertionError.class, () -> validateCanonicalContract(missing));
    }

    @Test void foreignSuitePathOrFalseFrozenSuitePinCannotPassPreparation() throws Exception {
        for (String field : List.of("sourceSuite", "sourceSuiteSha256")) {
            ObjectNode sample = contract().deepCopy(); sample.put(field, "foreign");
            assertThrows(AssertionError.class, () -> validateCanonicalContract(sample));
        }
    }

    private static void validateCanonicalContract(JsonNode contract) throws Exception {
        assertEquals("focused-suite.json", contract.path("sourceSuite").asText());
        byte[] suiteBytes = Files.readAllBytes(root().resolve(ASSETS + "focused-suite.json"));
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(suiteBytes)),
                contract.path("sourceSuiteSha256").asText());
        JsonNode suite = JSON.readTree(suiteBytes);
        assertEquals("M2_NEUTRAL_INPUT_AND_PUBLIC_ISOLATION", contract.path("methodId").asText());
        assertEquals("HOLD", contract.path("currentNativeAdmission").asText());
        assertEquals("UNVERIFIED", contract.path("realSessionBinding").asText());
        assertEquals(2, contract.path("actorTasks").size());
        Set<String> ids = new HashSet<>();
        for (JsonNode task : contract.path("actorTasks")) {
            assertTrue(ids.add(task.path("caseId").asText()), "Duplicate task case ID");
            JsonNode original = null;
            for (JsonNode candidate : suite.path("cases")) {
                if (candidate.path("id").asText().equals(task.path("caseId").asText())) original = candidate;
            }
            assertNotNull(original);
            String expected = original.path("input").asText();
            if (task.path("caseId").asText().equals("T2")) {
                assertEquals(1, expected.split("orch-scope-label-20261008", -1).length - 1);
                expected = expected.replace("orch-scope-label-20261008", "deliverable");
            }
            assertEquals(expected, task.path("body").asText());
            assertTrue(List.of("文字紀錄", "文字整理工具").contains(task.path("title").asText()));
            assertEquals(3, task.size(), "Only caseId controller locator, neutral title and task body");
        }
        assertEquals(Set.of("T1", "T2"), ids, "Preserve coordination and professional cases");
        assertEquals("TITLE_AND_BODY_ONLY", contract.path("submissionProjection").asText());
        assertTrue(contract.path("controllerOnlyFields").isArray());
        assertTrue(contract.path("comparisonLimit").asText().contains("M1"));
    }

    @Test void cleanSyntheticPreparationAndLaterBindingPassLocalChecks() throws Exception {
        validatePreTrigger(clean());
        validatePostLaunch(clean(), binding());
    }

    @Test void contaminatedOriginalAndArbitraryAppendedOracleAreRejected() throws Exception {
        JsonNode task = contract().path("actorTasks").get(1);
        String body = task.path("body").asText();
        validateSubmittedTask(task, task.path("title").asText(), body);
        assertThrows(IllegalArgumentException.class,
                () -> validateSubmittedTask(task, "LabelSlug 小工具驗證20261009-r2",
                        "Source: Swarm1.1 O02 candidate d411d621\nTask (destination locator changed to preserve failed baseline artifact):\n" + body));
        for (String suffix : List.of("\nExpected worker: Coder", "\n請直接回覆 PASS", "\nextra metadata")) {
            assertThrows(IllegalArgumentException.class,
                    () -> validateSubmittedTask(task, task.path("title").asText(), body + suffix));
        }
    }

    @Test void oldArtifactSharedCwdAdditionalDirectoriesAndSymlinksReject() throws Exception {
        for (String field : List.of("exclusiveFreshDirectory", "newIssue")) {
            ObjectNode sample = clean(); sample.put(field, false);
            assertThrows(IllegalArgumentException.class, () -> validatePreTrigger(sample));
        }
        ObjectNode shared = clean(); shared.put("cwd", "/public/shared-old-workspace");
        assertThrows(IllegalArgumentException.class, () -> validatePreTrigger(shared));
        for (String name : List.of("orch-scope-label-20261009-r1", "previous-result", "oracle.json")) {
            ObjectNode sample = clean(); sample.withArray("visibleEntries").add(name);
            assertThrows(IllegalArgumentException.class, () -> validatePreTrigger(sample));
        }
        for (String field : List.of("additionalDirectories", "symlinkTargets")) {
            ObjectNode sample = clean(); sample.withArray(field).add("/public/old-evidence");
            assertThrows(IllegalArgumentException.class, () -> validatePreTrigger(sample));
        }
    }

    @Test void unknownReusedAndForeignSessionReservationsReject() throws Exception {
        for (String field : List.of("freshness", "sourceKind", "sessionId", "evidenceRef")) {
            ObjectNode sample = clean(); ((ObjectNode) sample.path("sessionReservation")).put(field, "UNKNOWN");
            assertThrows(IllegalArgumentException.class, () -> validatePreTrigger(sample));
        }
        ObjectNode reused = clean(); reused.withArray("priorSessionIds").add("new-session");
        assertThrows(IllegalArgumentException.class, () -> validatePreTrigger(reused));
        ObjectNode history = clean(); ((ObjectNode) history.path("sessionReservation")).put("priorMessageCount", 1);
        assertThrows(IllegalArgumentException.class, () -> validatePreTrigger(history));
        for (String field : List.of("actorId", "runtimeId", "projectId", "cwd", "issueId")) {
            ObjectNode sample = clean(); ((ObjectNode) sample.path("sessionReservation")).put(field, "foreign");
            assertThrows(IllegalArgumentException.class, () -> validatePreTrigger(sample));
        }
    }

    @Test void undisclosedInstructionsMissingEvidenceAndLateInventoryReject() throws Exception {
        for (String field : List.of("inheritedInstructionsDisclosed", "additionalDirectoriesDisclosed", "initialDirectoryListingDisclosed")) {
            ObjectNode sample = clean(); sample.put(field, false);
            assertThrows(IllegalArgumentException.class, () -> validatePreTrigger(sample));
        }
        for (String field : List.of("inventoryEvidenceRef", "instructionsEvidenceRef")) {
            ObjectNode sample = clean(); sample.remove(field);
            assertThrows(IllegalArgumentException.class, () -> validatePreTrigger(sample));
        }
        ObjectNode late = clean(); late.put("inventoryAt", "2030-01-01T00:00:20Z");
        assertThrows(IllegalArgumentException.class, () -> validatePreTrigger(late));
        ObjectNode empty = clean(); empty.remove("visibleEntries");
        assertThrows(IllegalArgumentException.class, () -> validatePreTrigger(empty));
    }

    @Test void actualRunRequiresSeparateMatchingPostLaunchEvidence() throws Exception {
        ObjectNode sample = clean();
        assertFalse(sample.has("actualRunId"), "No future run ID required before trigger");
        validatePreTrigger(sample);
        for (String field : List.of("sessionId", "actorId", "runtimeId", "projectId", "cwd", "issueId", "sourceKind", "actualRunId", "evidenceRef")) {
            ObjectNode actual = binding(); actual.put(field, "UNKNOWN");
            assertThrows(IllegalArgumentException.class, () -> validatePostLaunch(sample, actual));
        }
        ObjectNode early = binding(); early.put("startedAt", "2030-01-01T00:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> validatePostLaunch(sample, early));
    }

    @Test void freshCreationAtLaunchNeedsNoFutureSessionIdentity() {
        ObjectNode sample = freshLaunch();
        assertFalse(sample.has("actualRunId"));
        assertFalse(sample.path("sessionCreationControl").has("sessionId"));
        validatePreTrigger(sample);
        validatePostLaunch(sample, freshBinding());
    }

    @Test void resumedUnknownMissingAndForeignLaunchControlsReject() {
        ObjectNode base = freshLaunch();
        validatePreTrigger(base);
        for (String field : List.of("sourceKind", "creationMethod", "evidenceRef", "actorId", "runtimeId", "projectId", "cwd", "issueId")) {
            ObjectNode sample = base.deepCopy();
            ((ObjectNode) sample.path("sessionCreationControl")).put(field, "UNKNOWN");
            assertThrows(IllegalArgumentException.class, () -> validatePreTrigger(sample));
        }
        for (boolean absent : List.of(false, true)) {
            ObjectNode sample = base.deepCopy();
            ObjectNode control = (ObjectNode) sample.path("sessionCreationControl");
            if (absent) control.remove("resumeRequested"); else control.put("resumeRequested", true);
            assertThrows(IllegalArgumentException.class, () -> validatePreTrigger(sample));
        }
        ObjectNode late = base.deepCopy();
        ((ObjectNode) late.path("sessionCreationControl")).put("observedAt", "2030-01-01T00:00:20Z");
        assertThrows(IllegalArgumentException.class, () -> validatePreTrigger(late));
    }

    @Test void actualFreshLaunchCannotResumeReuseOrInventCreationTiming() {
        ObjectNode preparation = freshLaunch();
        validatePreTrigger(preparation);
        for (String field : List.of("actualCreationMethod", "sessionId")) {
            ObjectNode actual = freshBinding(); actual.put(field, "UNKNOWN");
            assertThrows(IllegalArgumentException.class, () -> validatePostLaunch(preparation, actual));
        }
        ObjectNode resumed = freshBinding(); resumed.put("actualCreationMethod", "SESSION_RESUME");
        assertThrows(IllegalArgumentException.class, () -> validatePostLaunch(preparation, resumed));
        ObjectNode reused = freshBinding(); reused.put("sessionId", "old-session");
        assertThrows(IllegalArgumentException.class, () -> validatePostLaunch(preparation, reused));
        for (String time : List.of("2030-01-01T00:00:00Z", "2030-01-01T00:00:20Z")) {
            ObjectNode actual = freshBinding(); actual.put("sessionCreatedAt", time);
            assertThrows(IllegalArgumentException.class, () -> validatePostLaunch(preparation, actual));
        }
        ObjectNode foreign = freshBinding(); foreign.put("cwd", "/public/other");
        assertThrows(IllegalArgumentException.class, () -> validatePostLaunch(preparation, foreign));
    }

    private static void validateSubmittedTask(JsonNode expected, String title, String body) {
        require(expected.path("title").asText().equals(title), "Unexpected submitted title");
        require(expected.path("body").asText().equals(body), "Unexpected submitted task bytes");
    }

    private static void validatePreTrigger(JsonNode sample) {
        require(sample.path("exclusiveFreshDirectory").asBoolean() && sample.path("newIssue").asBoolean(), "Shared directory/issue");
        require(known(sample, "cwd") && sample.path("cwd").asText().equals(sample.path("reservedCwd").asText()), "Cwd binding");
        for (String field : List.of("additionalDirectories", "symlinkTargets", "priorSessionIds", "visibleEntries")) {
            require(sample.path(field).isArray(), "Missing inventory: " + field);
        }
        require(sample.path("additionalDirectories").isEmpty() && sample.path("symlinkTargets").isEmpty(), "Other visible roots");
        for (JsonNode entry : sample.path("visibleEntries")) {
            require(entry.isTextual() && List.of("AGENTS.md", "agent.md").contains(entry.asText()), "Non-neutral visible entry");
        }
        for (String field : List.of("inheritedInstructionsDisclosed", "additionalDirectoriesDisclosed", "initialDirectoryListingDisclosed")) {
            require(sample.path(field).asBoolean(), "Undisclosed context: " + field);
        }
        require(known(sample, "inventoryEvidenceRef") && known(sample, "instructionsEvidenceRef"), "Missing public evidence");
        Instant inventory = Instant.parse(sample.path("inventoryAt").asText());
        Instant planned = Instant.parse(sample.path("plannedTriggerAt").asText());
        require(!inventory.isAfter(planned), "Post-trigger inventory cannot prove pre-trigger isolation");
        boolean reserved = sample.has("sessionReservation");
        require(reserved != sample.has("sessionCreationControl"), "Exactly one supported session preparation mode");
        JsonNode session = sample.path(reserved ? "sessionReservation" : "sessionCreationControl");
        require(known(session, "evidenceRef"), "Missing session evidence");
        if (reserved) {
            require(session.path("sourceKind").asText().equals("SUPPORTED_PUBLIC_SESSION_RESERVATION"), "Unsupported session evidence");
            require(session.path("freshness").asText().equals("NEW_EMPTY") && session.path("priorMessageCount").isInt()
                    && session.path("priorMessageCount").asInt() == 0, "Unknown/nonempty session");
            require(known(session, "sessionId"), "Missing session identity");
            for (JsonNode old : sample.path("priorSessionIds")) require(!old.asText().equals(session.path("sessionId").asText()), "Reused session");
            require(!Instant.parse(session.path("createdAt").asText()).isAfter(inventory), "Reservation after inventory");
        } else {
            require(session.path("sourceKind").asText().equals("SUPPORTED_PUBLIC_FRESH_LAUNCH_CONTROL"), "Unsupported fresh-launch control");
            require(session.path("creationMethod").asText().equals("SESSION_NEW")
                    && session.path("resumeRequested").isBoolean() && !session.path("resumeRequested").asBoolean(), "Unknown/resumed launch");
            require(!Instant.parse(session.path("observedAt").asText()).isAfter(planned), "Control after preparation");
        }
        for (String field : List.of("actorId", "runtimeId", "projectId", "cwd", "issueId")) {
            require(known(sample, field) && sample.path(field).asText().equals(session.path(field).asText()), "Foreign session: " + field);
        }
    }

    private static void validatePostLaunch(JsonNode preparation, JsonNode actual) {
        validatePreTrigger(preparation);
        require(actual.path("sourceKind").asText().equals("SUPPORTED_PUBLIC_RUN_SESSION_BINDING"), "Unsupported actual binding");
        require(known(actual, "actualRunId") && known(actual, "evidenceRef"), "Missing actual run evidence");
        for (String field : List.of("actorId", "runtimeId", "projectId", "cwd", "issueId")) {
            require(known(actual, field) && actual.path(field).asText().equals(preparation.path(field).asText()), "Actual binding mismatch: " + field);
        }
        require(known(actual, "sessionId"), "Missing actual session identity");
        Instant start = Instant.parse(actual.path("startedAt").asText());
        require(!start.isBefore(Instant.parse(preparation.path("plannedTriggerAt").asText())), "Run before preparation");
        if (preparation.has("sessionReservation")) {
            require(actual.path("sessionId").asText().equals(preparation.path("sessionReservation").path("sessionId").asText()), "Actual reserved session mismatch");
        } else {
            require(actual.path("actualCreationMethod").asText().equals("SESSION_NEW"), "Actual session creation unverified/resumed");
            for (JsonNode old : preparation.path("priorSessionIds")) require(!old.asText().equals(actual.path("sessionId").asText()), "Actual session reused");
            Instant created = Instant.parse(actual.path("sessionCreatedAt").asText());
            Instant prompt = Instant.parse(actual.path("promptStartedAt").asText());
            require(!created.isBefore(start) && !created.isAfter(prompt), "Creation outside actual launch");
        }
    }

    private static boolean known(JsonNode object, String field) {
        return object.path(field).isTextual() && !object.path(field).asText().isBlank()
                && !List.of("UNKNOWN", "UNVERIFIED").contains(object.path(field).asText());
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
    private static ObjectNode clean() {
        ObjectNode sample = JSON.createObjectNode();
        sample.put("exclusiveFreshDirectory", true).put("newIssue", true);
        sample.put("cwd", "/public/neutral-workspace").put("reservedCwd", "/public/neutral-workspace");
        sample.put("actorId", "actor").put("runtimeId", "runtime").put("projectId", "project").put("issueId", "new-issue");
        sample.putArray("visibleEntries").add("AGENTS.md");
        sample.putArray("additionalDirectories"); sample.putArray("symlinkTargets"); sample.putArray("priorSessionIds").add("old-session");
        sample.put("inheritedInstructionsDisclosed", true).put("additionalDirectoriesDisclosed", true).put("initialDirectoryListingDisclosed", true);
        sample.put("inventoryEvidenceRef", "synthetic-inventory").put("instructionsEvidenceRef", "synthetic-instructions");
        sample.put("inventoryAt", "2030-01-01T00:00:05Z").put("plannedTriggerAt", "2030-01-01T00:00:10Z");
        ObjectNode session = sample.putObject("sessionReservation");
        for (String field : List.of("actorId", "runtimeId", "projectId", "cwd", "issueId")) session.set(field, sample.path(field));
        session.put("sourceKind", "SUPPORTED_PUBLIC_SESSION_RESERVATION").put("sessionId", "new-session")
                .put("freshness", "NEW_EMPTY").put("priorMessageCount", 0).put("evidenceRef", "synthetic-session")
                .put("createdAt", "2030-01-01T00:00:02Z");
        return sample;
    }
    private static ObjectNode binding() {
        ObjectNode actual = clean().path("sessionReservation").deepCopy();
        actual.put("sourceKind", "SUPPORTED_PUBLIC_RUN_SESSION_BINDING").put("actualRunId", "new-run")
                .put("startedAt", "2030-01-01T00:00:11Z").put("evidenceRef", "synthetic-run");
        return actual;
    }
    private static ObjectNode freshLaunch() {
        ObjectNode sample = clean();
        ObjectNode control = JSON.createObjectNode();
        for (String field : List.of("actorId", "runtimeId", "projectId", "cwd", "issueId")) control.set(field, sample.path(field));
        control.put("sourceKind", "SUPPORTED_PUBLIC_FRESH_LAUNCH_CONTROL").put("creationMethod", "SESSION_NEW")
                .put("resumeRequested", false).put("evidenceRef", "synthetic-launch-control")
                .put("observedAt", "2030-01-01T00:00:05Z");
        sample.remove("sessionReservation");
        sample.set("sessionCreationControl", control);
        return sample;
    }
    private static ObjectNode freshBinding() {
        return binding().put("actualCreationMethod", "SESSION_NEW")
                .put("sessionCreatedAt", "2030-01-01T00:00:11.500Z")
                .put("promptStartedAt", "2030-01-01T00:00:12Z");
    }
    private static JsonNode contract() throws Exception {
        Path path = root().resolve(ASSETS + "native-preparation.json");
        assertTrue(Files.isRegularFile(path), "Canonical neutral preparation contract is missing");
        return JSON.readTree(path.toFile());
    }
    private static Path root() {
        Path at = Path.of("").toAbsolutePath();
        while (at != null && !Files.isDirectory(at.resolve(ASSETS))) at = at.getParent();
        if (at == null) throw new IllegalStateException("Source checkout not found");
        return at;
    }
}

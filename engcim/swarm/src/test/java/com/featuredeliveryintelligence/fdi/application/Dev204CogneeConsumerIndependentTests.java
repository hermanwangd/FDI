package com.featuredeliveryintelligence.fdi.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.featuredeliveryintelligence.fdi.orchestration.GovernedWorkspaceKnowledge;
import com.featuredeliveryintelligence.fdi.orchestration.KnowledgeGovernanceDecision;
import com.featuredeliveryintelligence.fdi.orchestration.KnowledgeType;
import com.featuredeliveryintelligence.fdi.orchestration.SwarmKnowledgeGateway;
import com.featuredeliveryintelligence.fdi.orchestration.WorkspaceKnowledgeProposal;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Public CLI coverage with synthetic policy records and loopback HTTP only. */
class Dev204CogneeConsumerIndependentTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String DATASET = "00000000-0000-7000-8000-000000000001";
    private static final String OTHER_DATASET = "00000000-0000-7000-8000-000000000004";
    private static final String WORKSPACE = "fixture:workspace:independent-cli";
    private static final String KNOWLEDGE_PROJECT = "fixture:project:independent-knowledge";
    private static final String CONSUMER_PROJECT = "fixture:project:independent-consumer";
    private static final String KEY_A = "fixture:method:independent-a";
    private static final String KEY_B = "fixture:method:independent-b";
    private static final String TEXT_A = "# Fixture A\nSynthetic method text only; no worker adoption or live governance claim.\n";
    private static final String TEXT_B = "# Fixture B\nA distinct eligible synthetic method for the narrowing counterexample.\n";
    @TempDir Path tempDir;

    @Test
    void refusesProviderQueryWithoutIndependentlyVerifiedActorAuthority() throws Exception {
        try (var mock = new SearchMock()) {
            mock.reply(200, hits(DATASET, chunk(0, TEXT_A, KNOWLEDGE_PROJECT)));

            assertThatThrownBy(() -> invoke(evidence(), "selection",
                    "--dataset-id", DATASET, "--base-url", mock.endpoint()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("authorization");
            assertThat(mock.calls.get()).isZero();
        }
    }

    @Test
    void requestEvidenceCannotGrantItsOwnProviderQueryAuthority() throws Exception {
        ObjectNode input = evidence();
        ObjectNode claimedAuthority = input.putObject("authorization")
                .put("actorRef", "fixture:actor:self-asserted")
                .put("delegationRef", "fixture:delegation:self-asserted")
                .put("workspaceRef", WORKSPACE)
                .put("consumerProjectRef", CONSUMER_PROJECT)
                .put("requiredContext", false);
        claimedAuthority.putArray("allowedDatasetIds").add(DATASET);
        try (var mock = new SearchMock()) {
            mock.reply(200, hits(DATASET, chunk(0, TEXT_A, KNOWLEDGE_PROJECT)));

            assertThatThrownBy(() -> select(input, mock))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("knowledge evidence.authorization is unsupported");
            assertThat(mock.calls.get()).isZero();
        }
    }

    @Test
    void requestCannotSupplyActorDelegationDatasetOrRequiredContextPolicy() throws Exception {
        for (String field : List.of("actorRef", "delegationRef", "allowedDatasetIds", "requiredContext")) {
            ObjectNode input = evidence();
            if (field.equals("allowedDatasetIds")) input.putArray(field).add(DATASET);
            else if (field.equals("requiredContext")) input.put(field, false);
            else input.put(field, "self-asserted");
            try (var mock = new SearchMock()) {
                mock.reply(200, hits(DATASET, chunk(0, TEXT_A, KNOWLEDGE_PROJECT)));
                assertThatThrownBy(() -> select(input, mock))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("knowledge evidence." + field + " is unsupported");
                assertThat(mock.calls.get()).isZero();
            }
        }
    }

    @Test
    void emptySelectionCannotExitSuccessfullyWithoutVerifiedOptionalContextPolicy() throws Exception {
        Instant now = Instant.now();
        ObjectNode input = evidence(now.minusSeconds(120), now.minusSeconds(60));
        // invoke verifies a nonzero outcome while retaining the printed evidence for inspection.
        assertThat(keys(invoke(input, "selection"))).isEmpty();
    }

    @Test
    void emptyGovernedSnapshotPreventsQueryEvenWithSyntheticClientComposition() throws Exception {
        ObjectNode input = evidence();
        ((ArrayNode) input.path("providerRead").path("entries")).removeAll();
        try (var mock = new SearchMock()) {
            mock.reply(200, hits(DATASET, chunk(0, TEXT_A, KNOWLEDGE_PROJECT)));
            JsonNode result = select(input, mock);
            assertThat(mock.calls.get()).isZero();
            assertThat(result.path("status").asText()).isEqualTo("NO_ELIGIBLE_RECORDS");
            assertThat(result.has("cogneeSelection")).isFalse();
        }
    }

    @Test
    void narrowsTwoEligibleRecordsToOneAndReplaysOriginalSelectionAfterExpiryWithMockClosed() throws Exception {
        Instant fetchedAt = Instant.now();
        Instant expiry = fetchedAt.plusSeconds(5);
        ObjectNode input = evidence(fetchedAt, expiry);
        JsonNode legacy = invoke(input, "selection");
        assertThat(keys(legacy)).containsExactly(KEY_A, KEY_B);
        try (var mock = new SearchMock()) {
            mock.reply(200, hits(DATASET, chunk(0, TEXT_A, KNOWLEDGE_PROJECT)));
            JsonNode selection = select(input, mock);

            assertThat(keys(selection)).containsExactly(KEY_A);
            assertThat(selection.path("status").asText()).isEqualTo("ELIGIBLE_CONTEXT");
            assertThat(selection.path("cogneeSelection").path("datasetId").asText()).isEqualTo(DATASET);
            assertThat(selection.path("cogneeSelection").path("candidateSearch").path("candidates")).hasSize(1);
            assertThat(selection.path("cogneeSelection").path("querySha256").asText()).matches("[0-9a-f]{64}");
            assertThat(selection.path("sourceSnapshotSha256").asText()).matches("[0-9a-f]{64}");
            assertThat(selection.path("selectionReceiptSha256").asText()).matches("[0-9a-f]{64}");
            assertThat(mock.request.get().path("top_k").asInt()).isEqualTo(5);
            assertThat(mock.request.get().path("search_type").asText()).isEqualTo("CHUNKS");
            assertThat(mock.request.get().path("dataset_ids").get(0).asText()).isEqualTo(DATASET);
            assertThat(mock.request.get().path("query").asText()).isEqualTo(input.path("cogneeQuery").asText());
            Instant evaluatedAt = Instant.parse(selection.path("context").path("evaluatedAt").asText());
            assertThat(evaluatedAt).isBefore(expiry);
            mock.close();
            long remaining = Duration.between(Instant.now(), expiry.plusMillis(50)).toMillis();
            if (remaining > 0) Thread.sleep(remaining);
            assertThat(Instant.now()).isAfter(expiry);

            JsonNode feedback = invoke(feedbackInput(input, selection, KEY_A), "feedback");

            assertThat(feedback.path("status").asText()).isEqualTo("FEEDBACK_BUILT");
            assertThat(feedback.path("feedback").path("recordKey").asText()).isEqualTo(KEY_A);
            assertThat(feedback.path("feedback").path("recordVersion").asInt()).isEqualTo(2);
            assertThat(feedback.path("feedback").path("disposition").asText()).isEqualTo("REJECTED");
            assertThat(feedback.path("selectionReceiptSha256")).isEqualTo(selection.path("selectionReceiptSha256"));
            assertThat(feedback.path("sourceSnapshotSha256")).isEqualTo(selection.path("sourceSnapshotSha256"));
            assertThat(mock.calls.get()).isEqualTo(1);
            // A fresh selection must use the current clock rather than the preserved feedback time.
            assertThat(keys(invoke(input, "selection"))).isEmpty();
        }
    }

    @Test
    void rejectsChangedSourceCandidateRevisionDatasetQueryAndSelectionContextWithoutRequery() throws Exception {
        ObjectNode input = evidence();
        try (var mock = new SearchMock()) {
            mock.reply(200, hits(DATASET, chunk(0, TEXT_A, KNOWLEDGE_PROJECT)));
            JsonNode selection = select(input, mock);
            mock.close();

            ObjectNode sourceChanged = feedbackInput(input, selection, KEY_A);
            ((ObjectNode) sourceChanged.path("providerRead").path("entries").get(1))
                    .put("providerRevision", "fixture:provider:tampered");
            assertThatThrownBy(() -> invoke(sourceChanged, "feedback"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("snapshot digest");

            ObjectNode revisionChanged = feedbackInput(input, selection, KEY_A);
            ((ObjectNode) revisionChanged.path("selectionReceipt").path("cogneeSelection")
                    .path("candidateSearch").path("candidates").get(0)).put("sourceRevision", 3);
            assertThatThrownBy(() -> invoke(revisionChanged, "feedback"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("candidate receipt digest");

            ObjectNode datasetChanged = feedbackInput(input, selection, KEY_A);
            ((ObjectNode) datasetChanged.path("selectionReceipt").path("cogneeSelection"))
                    .put("datasetId", OTHER_DATASET);
            assertThatThrownBy(() -> invoke(datasetChanged, "feedback"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("candidate receipt digest");

            ObjectNode queryChanged = feedbackInput(input, selection, KEY_A).put("cogneeQuery", "different query");
            assertThatThrownBy(() -> invoke(queryChanged, "feedback"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("query does not match");

            ObjectNode contextChanged = feedbackInput(input, selection, KEY_A);
            ((ObjectNode) contextChanged.path("selectionReceipt").path("context"))
                    .set("selected", input.path("providerRead").path("entries").deepCopy());
            assertThatThrownBy(() -> invoke(contextChanged, "feedback"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("selection receipt content");
            assertThat(mock.calls.get()).isEqualTo(1);
        }
    }

    @Test
    void noCogneeOptionsPreserveLegacySelectionShapeAndFeedbackOfEitherEligibleRecord() throws Exception {
        ObjectNode input = evidence();
        JsonNode selection = invoke(input, "selection");

        assertThat(keys(selection)).containsExactly(KEY_A, KEY_B);
        var fields = new ArrayList<String>();
        selection.fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder("phase", "status", "sourceSnapshotSha256", "context",
                "claimBoundary", "selectionReceiptSha256");
        assertThat(selection.path("claimBoundary").asText())
                .isEqualTo("FRESH_PROVIDER_READ_SELECTION_EVIDENCE_ONLY_NO_WRITE_NO_GOVERNANCE_DECISION");
        JsonNode feedback = invoke(feedbackInput(input, selection, KEY_B), "feedback");
        assertThat(feedback.path("status").asText()).isEqualTo("FEEDBACK_BUILT");
        assertThat(feedback.path("feedback").path("recordKey").asText()).isEqualTo(KEY_B);
        assertThat(feedback.path("selectionReceiptSha256")).isEqualTo(selection.path("selectionReceiptSha256"));
        assertThatThrownBy(() -> invoke(input, "selection", "--base-url", "http://127.0.0.1:1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("--dataset-id");
    }

    @Test
    void distinguishesEmptyPartialUnavailableAndForeignSourcesWhilePreservingFullGovernanceExclusions() throws Exception {
        ObjectNode input = evidence();
        try (var mock = new SearchMock()) {
            mock.reply(200, hits(DATASET));
            JsonNode empty = select(input, mock);
            assertUnqualified(empty, "NO_GOVERNED_COGNEE_MATCH", "SEARCHED");
            assertThat(empty.path("cogneeSelection").path("candidateSearch").path("rejections")).isEmpty();

            mock.reply(200, hits(DATASET, chunk(0, TEXT_A.substring(0, 11), KNOWLEDGE_PROJECT)));
            JsonNode partial = select(input, mock);
            assertUnqualified(partial, "NO_GOVERNED_COGNEE_MATCH", "SEARCHED");
            assertThat(partial.path("cogneeSelection").path("candidateSearch").path("rejections")).hasSize(1);

            mock.reply(503, JSON.createArrayNode());
            JsonNode unavailable = select(input, mock);
            assertUnqualified(unavailable, "COGNEE_UNAVAILABLE_UNQUALIFIED_CONTEXT", "UNAVAILABLE");
            assertThat(unavailable.path("cogneeSelection").path("unavailableReason").asText()).contains("HTTP 503");
            ObjectNode inconsistentUnavailable = feedbackInput(input, unavailable, KEY_A);
            ObjectNode trace = (ObjectNode) inconsistentUnavailable.path("selectionReceipt").path("cogneeSelection");
            ((ArrayNode) trace.path("candidateSearch").path("rejections")).addObject()
                    .put("documentId", "fixture:document:inconsistent")
                    .put("reason", "CONTENT_DIGEST_MISMATCH_OR_TRUNCATED_RESULT");
            ObjectNode traceBody = trace.deepCopy();
            traceBody.remove("candidateSearchSha256");
            trace.put("candidateSearchSha256", sha256(JSON.writeValueAsString(sortedCopy(traceBody))));
            assertThatThrownBy(() -> invoke(inconsistentUnavailable, "feedback"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unavailable");
            int callsBeforeMissingEndpoint = mock.calls.get();
            assertThatThrownBy(() -> invoke(input, "selection", "--dataset-id", DATASET))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("authorization");
            assertThat(mock.calls.get()).isEqualTo(callsBeforeMissingEndpoint);

            mock.reply(200, hits(OTHER_DATASET, chunk(0, TEXT_A, KNOWLEDGE_PROJECT)));
            assertThatThrownBy(() -> select(input, mock))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("dataset scope");
            mock.reply(200, hits(DATASET, chunk(0, TEXT_A, "fixture:project:foreign")));
            assertUnqualified(select(input, mock), "NO_GOVERNED_COGNEE_MATCH", "SEARCHED");

            mock.reply(200, hits(DATASET, chunk(0, TEXT_A, KNOWLEDGE_PROJECT)));
            ObjectNode duplicate = input.deepCopy();
            ArrayNode duplicateEntries = (ArrayNode) duplicate.path("providerRead").path("entries");
            duplicateEntries.add(((ObjectNode) duplicateEntries.get(0)).deepCopy()
                    .put("providerRevision", "fixture:provider:another"));
            JsonNode duplicateSelection = select(duplicate, mock);
            assertUnqualified(duplicateSelection, "NO_GOVERNED_COGNEE_MATCH", "SEARCHED");
            assertThat(duplicateSelection.path("context").path("excluded")).hasSize(2).allSatisfy(exclusion ->
                    assertThat(exclusion.path("reason").asText()).isEqualTo("DUPLICATE_RECORD_KEY"));

            ObjectNode missingGovernance = input.deepCopy();
            ((ObjectNode) missingGovernance.path("providerRead").path("entries").get(0)).remove("governance");
            JsonNode ungoverned = select(missingGovernance, mock);
            assertUnqualified(ungoverned, "NO_GOVERNED_COGNEE_MATCH", "SEARCHED");
            assertThat(ungoverned.path("context").path("excluded")).singleElement().satisfies(exclusion ->
                    assertThat(exclusion.path("reason").asText()).isEqualTo("GOVERNANCE_INCOMPLETE"));
        }
    }

    @Test
    void excludesFreshEnvelopeConflictsBeforeCogneeNarrowingWithoutChangingTheApprovedProposal() throws Exception {
        ObjectNode input = evidence();
        ObjectNode originalEntry = (ObjectNode) input.path("providerRead").path("entries").get(0);
        JsonNode originalProposal = originalEntry.path("proposal").deepCopy();
        JsonNode originalGovernance = originalEntry.path("governance").deepCopy();
        assertThat(originalProposal.path("conflictRefs")).isEmpty();
        try (var mock = new SearchMock()) {
            mock.reply(200, hits(DATASET, chunk(0, TEXT_A, KNOWLEDGE_PROJECT)));
            assertThat(keys(select(input, mock))).containsExactly(KEY_A);

            originalEntry.putArray("sourceConflictRefs").add("fixture:unresolved:record-envelope");
            JsonNode fullRead = invoke(input, "selection");
            assertThat(keys(fullRead)).containsExactly(KEY_B);
            assertThat(fullRead.path("context").path("excluded")).singleElement().satisfies(exclusion -> {
                assertThat(exclusion.path("recordKey").asText()).isEqualTo(KEY_A);
                assertThat(exclusion.path("reason").asText()).isEqualTo("UNRESOLVED_CONFLICT");
            });
            JsonNode narrowed = select(input, mock);
            assertUnqualified(narrowed, "NO_GOVERNED_COGNEE_MATCH", "SEARCHED");
            assertThat(narrowed.path("context").path("excluded")).isEqualTo(fullRead.path("context").path("excluded"));
            assertThat(originalEntry.path("proposal")).isEqualTo(originalProposal);
            assertThat(originalEntry.path("governance")).isEqualTo(originalGovernance);
            assertThat(originalEntry.path("proposalDigest")).isEqualTo(originalEntry.path("decisionProposalDigest"));
            assertThat(mock.calls.get()).isEqualTo(2);
        }
    }

    @Test
    void absentEmptyAndNullEnvelopeConflictFieldsKeepLegacyJsonSelectionAndOriginalFeedbackCompatible() throws Exception {
        for (String representation : List.of("absent", "empty", "null")) {
            ObjectNode input = evidence();
            for (JsonNode node : input.path("providerRead").path("entries")) {
                ObjectNode entry = (ObjectNode) node;
                if ("empty".equals(representation)) entry.putArray("sourceConflictRefs");
                if ("null".equals(representation)) entry.putNull("sourceConflictRefs");
            }
            JsonNode selection = invoke(input, "selection");
            assertThat(keys(selection)).as(representation).containsExactly(KEY_A, KEY_B);
            assertThat(selection.path("context").path("excluded")).as(representation).isEmpty();
            JsonNode feedback = invoke(feedbackInput(input, selection, KEY_B), "feedback");
            assertThat(feedback.path("status").asText()).as(representation).isEqualTo("FEEDBACK_BUILT");
            assertThat(feedback.path("sourceSnapshotSha256")).isEqualTo(selection.path("sourceSnapshotSha256"));
            assertThat(feedback.path("selectionReceiptSha256")).isEqualTo(selection.path("selectionReceiptSha256"));
        }
    }

    @Test
    void replaysLegacyTopTenReceiptWithoutTreatingHistoricalOptionalityAsAuthority() throws Exception {
        ObjectNode input = evidence();
        try (var mock = new SearchMock()) {
            mock.reply(200, hits(DATASET, chunk(0, TEXT_A, KNOWLEDGE_PROJECT)));
            ObjectNode legacy = (ObjectNode) select(input, mock);
            mock.close();
            ObjectNode trace = (ObjectNode) legacy.path("cogneeSelection");
            trace.put("topK", 10);
            trace.remove("candidateSearchSha256");
            trace.put("candidateSearchSha256", sha256(JSON.writeValueAsString(sortedCopy(trace))));
            ObjectNode snapshot = JSON.createObjectNode();
            snapshot.set("consumerRequest", input.path("consumerRequest"));
            snapshot.set("providerRead", input.path("providerRead"));
            snapshot.set("cogneeSelection", trace);
            legacy.put("sourceSnapshotSha256", sha256(JSON.writeValueAsString(sortedCopy(snapshot))));
            ((ObjectNode) legacy.path("context")).put("mayProceedWithoutKnowledge", true);
            legacy.remove("selectionReceiptSha256");
            legacy.put("selectionReceiptSha256", sha256(JSON.writeValueAsString(sortedCopy(legacy))));
            String digest = legacy.path("selectionReceiptSha256").asText();

            JsonNode replay = invoke(feedbackInput(input, legacy, KEY_A), "feedback");
            assertThat(replay.path("status").asText()).isEqualTo("FEEDBACK_BUILT");
            assertThat(replay.path("selectionReceiptSha256").asText()).isEqualTo(digest);
            assertThat(mock.calls.get()).isEqualTo(1);

            ((ObjectNode) legacy.path("context")).put("authorization", "invented");
            legacy.remove("selectionReceiptSha256");
            legacy.put("selectionReceiptSha256", sha256(JSON.writeValueAsString(sortedCopy(legacy))));
            assertThatThrownBy(() -> invoke(feedbackInput(input, legacy, KEY_A), "feedback"))
                    .hasMessageContaining("selection receipt content");
        }
    }

    @Test
    void replaysAnAuthenticReceiptProducedBeforeTheEnvelopeConflictFieldExisted() throws Exception {
        // Captured through FdiApplication.main using the fixed 627bc4a4 Gateway / 928452c6 Entry snapshot.
        // Rebuild the captured legacy JSON from its unchanged entries, then verify its original checksum.
        ObjectNode input = evidence(Instant.parse("2026-10-02T08:12:01.420Z"), Instant.parse("2026-10-02T09:12:01.420Z"));
        ObjectNode selection = JSON.createObjectNode().put("phase", "selection").put("status", "ELIGIBLE_CONTEXT")
                .put("sourceSnapshotSha256", "79033606898800478a81d216bde5db48bfca82af7956364a4dc9b18aa98bb597");
        ObjectNode context = selection.putObject("context");
        context.set("request", input.path("consumerRequest").deepCopy());
        context.put("readWorkspaceRef", WORKSPACE).put("readProjectRef", KNOWLEDGE_PROJECT)
                .put("fetchedAt", "2026-10-02T08:12:01.420Z").put("evaluatedAt", "2026-10-02T08:12:01.761305Z");
        context.set("selected", input.path("providerRead").path("entries").deepCopy());
        context.putArray("excluded");
        context.put("mayProceedWithoutKnowledge", true);
        selection.put("claimBoundary", "FRESH_PROVIDER_READ_SELECTION_EVIDENCE_ONLY_NO_WRITE_NO_GOVERNANCE_DECISION");
        String originalDigest = "6048a02e7cf4cd982c8432ae9316729b179d1b3bb7c2f3f13801537775e3c599";
        assertThat(sha256(JSON.writeValueAsString(sortedCopy(selection)))).isEqualTo(originalDigest);
        selection.put("selectionReceiptSha256", originalDigest);

        JsonNode feedback = invoke(feedbackInput(input, selection, KEY_A), "feedback");

        assertThat(feedback.path("status").asText()).isEqualTo("FEEDBACK_BUILT");
        assertThat(feedback.path("selectionReceiptSha256").asText()).isEqualTo(originalDigest);
    }

    @Test
    void rejectsEnvelopeConflictTamperingEvenAfterRecomputingSnapshotAndOuterReceiptChecksums() throws Exception {
        ObjectNode input = evidence();
        try (var mock = new SearchMock()) {
            mock.reply(200, hits(DATASET, chunk(0, TEXT_A, KNOWLEDGE_PROJECT)));
            JsonNode selection = select(input, mock);
            mock.close();
            for (int changedEntry : List.of(0, 1)) {
                ObjectNode altered = feedbackInput(input, selection, KEY_A);
                ((ObjectNode) altered.path("providerRead").path("entries").get(changedEntry))
                        .putArray("sourceConflictRefs").add("fixture:unresolved:tampered-envelope");
                assertThatThrownBy(() -> invoke(altered, "feedback"))
                        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("snapshot digest");

                ObjectNode receipt = (ObjectNode) altered.path("selectionReceipt");
                ObjectNode snapshot = JSON.createObjectNode();
                snapshot.set("consumerRequest", altered.path("consumerRequest"));
                snapshot.set("providerRead", altered.path("providerRead"));
                snapshot.set("cogneeSelection", receipt.path("cogneeSelection"));
                receipt.put("sourceSnapshotSha256", sha256(JSON.writeValueAsString(sortedCopy(snapshot))));
                ObjectNode receiptBody = receipt.deepCopy();
                receiptBody.remove("selectionReceiptSha256");
                receipt.put("selectionReceiptSha256", sha256(JSON.writeValueAsString(sortedCopy(receiptBody))));
                assertThatThrownBy(() -> invoke(altered, "feedback"))
                        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("selection receipt content");
            }
            assertThat(mock.calls.get()).isEqualTo(1);
        }
    }

    @Test
    void bodyIndexConflictQuarantinesTheOtherwiseEligibleIndexedKeyWithoutErasingEitherIdentity() throws Exception {
        Instant fetchedAt = Instant.now();
        String mismatchedId = "00000000-0000-7000-8000-00000000001a";
        String eligibleId = "00000000-0000-7000-8000-00000000001b";
        ObjectNode eligible = syntheticRawIssue(KEY_A, KEY_A, eligibleId, 211, fetchedAt, false);
        JsonNode control = invoke(syntheticRawEvidence(fetchedAt, eligible.deepCopy()), "selection");
        assertThat(keys(control)).containsExactly(KEY_A);
        assertThat(control.path("providerMappingExclusions")).isEmpty();

        ObjectNode mismatched = syntheticRawIssue(KEY_B, KEY_A, mismatchedId, 212, fetchedAt, false);
        ObjectNode input = syntheticRawEvidence(fetchedAt, mismatched, eligible);
        JsonNode selection = invoke(input, "selection");

        assertThat(keys(selection)).isEmpty();
        assertThat(selection.path("status").asText()).isEqualTo("NO_ELIGIBLE_RECORDS");
        assertThat(selection.path("context").path("mayProceedWithoutKnowledge").asBoolean()).isFalse();
        JsonNode exclusions = selection.path("providerMappingExclusions");
        assertThat(exclusions).hasSize(2);
        assertThat(exclusions).filteredOn(exclusion -> exclusion.path("issueRef").asText()
                .equals("multica:issue:" + mismatchedId)).singleElement().satisfies(exclusion -> {
                    assertThat(exclusion.path("recordKey").asText()).isEqualTo(KEY_B);
                    assertThat(exclusion.path("indexedRecordKey").asText()).isEqualTo(KEY_A);
                    assertThat(exclusion.path("reasonCode").asText())
                            .isIn("BODY_INDEX_MISMATCH", "DUPLICATE_RECORD_KEY");
                    assertThat(exclusion.path("sourceRowSha256").asText()).matches("[0-9a-f]{64}");
                });
        assertThat(exclusions).filteredOn(exclusion -> exclusion.path("issueRef").asText()
                .equals("multica:issue:" + eligibleId)).singleElement().satisfies(exclusion -> {
                    assertThat(exclusion.path("recordKey").asText()).isEqualTo(KEY_A);
                    assertThat(exclusion.path("reasonCode").asText()).isEqualTo("BODY_INDEX_MISMATCH");
                    assertThat(exclusion.path("sourceRowSha256").asText()).matches("[0-9a-f]{64}");
                });
    }

    @Test
    void changedMappingExcludedRawBytesInvalidateFeedbackWhileMappedEligibleEntriesStayIdentical() throws Exception {
        Instant fetchedAt = Instant.now();
        String eligibleId = "00000000-0000-7000-8000-00000000001c";
        String excludedId = "00000000-0000-7000-8000-00000000001d";
        ObjectNode input = syntheticRawEvidence(fetchedAt,
                syntheticRawIssue(KEY_A, KEY_A, eligibleId, 221, fetchedAt, false),
                syntheticRawIssue(KEY_B, KEY_B, excludedId, 222, fetchedAt, true));
        JsonNode selection = invoke(input, "selection");
        assertThat(keys(selection)).containsExactly(KEY_A);
        assertThat(selection.path("providerMappingStatus").asText()).isEqualTo("PARTIAL");
        assertThat(selection.path("providerMappingExclusions")).singleElement().satisfies(exclusion -> {
            assertThat(exclusion.path("issueRef").asText()).isEqualTo("multica:issue:" + excludedId);
            assertThat(exclusion.path("recordKey").asText()).isEqualTo(KEY_B);
            assertThat(exclusion.path("reasonCode").asText()).isEqualTo("UNSUPPORTED_KNOWLEDGE_TYPE");
            assertThat(exclusion.path("sourceRowSha256").asText()).matches("[0-9a-f]{64}");
        });
        assertThat(invoke(feedbackInput(input, selection, KEY_A), "feedback").path("status").asText())
                .isEqualTo("FEEDBACK_BUILT");

        ObjectNode changed = feedbackInput(input, selection, KEY_A);
        ArrayNode rawRows = (ArrayNode) changed.path("providerReadRaw").path("responses").get(0).path("issues");
        ObjectNode excluded = (ObjectNode) rawRows.get(1);
        excluded.put("description", excluded.path("description").asText()
                + "\nSynthetic test-only excluded-row bytes changed; the fenced record is unchanged.\n");
        assertThat(rawRows.get(0)).isEqualTo(input.path("providerReadRaw").path("responses").get(0)
                .path("issues").get(0));
        assertThat(changed.path("selectionReceipt")).isEqualTo(selection);

        ObjectNode changedSelectionInput = changed.deepCopy();
        changedSelectionInput.remove("selectionReceipt");
        changedSelectionInput.remove("consumerFeedback");
        JsonNode remapped = invoke(changedSelectionInput, "selection");
        assertThat(remapped.path("context").path("selected")).isEqualTo(selection.path("context").path("selected"));
        assertThat(remapped.path("sourceSnapshotSha256")).isNotEqualTo(selection.path("sourceSnapshotSha256"));
        assertThatThrownBy(() -> invoke(changed, "feedback"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("provider and candidate snapshot digest");
        assertThat(changed.path("selectionReceipt")).isEqualTo(selection);
    }

    private JsonNode select(ObjectNode input, SearchMock mock) throws Exception {
        return invoke(input, "selection", true, "--dataset-id", DATASET, "--base-url", mock.endpoint());
    }

    private JsonNode invoke(ObjectNode evidence, String phase, String... options) throws Exception {
        return invoke(evidence, phase, false, options);
    }

    private JsonNode invoke(ObjectNode evidence, String phase, boolean syntheticComposition,
            String... options) throws Exception {
        Path file = tempDir.resolve("evidence-" + phase + ".json");
        Files.writeString(file, JSON.writeValueAsString(evidence), StandardCharsets.UTF_8);
        var args = new ArrayList<>(List.of("dev204-knowledge-consume", "--evidence-file", file.toString(), "--phase", phase));
        args.addAll(List.of(options));
        var bytes = new ByteArrayOutputStream();
        PrintStream original = System.out;
        RuntimeException exit = null;
        try (var captured = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            try {
                if (syntheticComposition) {
                    // Local fixture wiring only. This is not a production actor or dataset grant.
                    Dev204Cli.handles(args.toArray(String[]::new), configured ->
                            new com.featuredeliveryintelligence.fdi.orchestration.CogneeSearchClient(
                                    java.net.URI.create(configured.get("--base-url"))));
                } else {
                    FdiApplication.main(args.toArray(String[]::new));
                }
            }
            catch (RuntimeException failure) { exit = failure; }
        } finally {
            System.setOut(original);
        }
        if (bytes.size() == 0 && exit != null) throw exit;
        JsonNode result = JSON.readTree(bytes.toString(StandardCharsets.UTF_8));
        assertThat(result).isNotNull();
        if ("selection".equals(phase) && result.path("context").path("selected").isEmpty()) {
            assertThat(exit).isInstanceOf(IllegalStateException.class).hasMessageContaining("optional-context policy");
        } else if (exit != null) {
            throw exit;
        }
        return result;
    }

    private static List<String> keys(JsonNode selection) {
        var keys = new ArrayList<String>();
        selection.path("context").path("selected").forEach(entry -> keys.add(entry.path("recordKey").asText()));
        return keys;
    }

    private static void assertUnqualified(JsonNode result, String status, String searchStatus) {
        assertThat(result.path("status").asText()).isEqualTo(status);
        assertThat(keys(result)).isEmpty();
        assertThat(result.path("context").path("mayProceedWithoutKnowledge").asBoolean()).isFalse();
        assertThat(result.path("cogneeSelection").path("searchStatus").asText()).isEqualTo(searchStatus);
    }

    private static ObjectNode feedbackInput(ObjectNode input, JsonNode selection, String key) {
        ObjectNode result = input.deepCopy();
        result.set("selectionReceipt", selection.deepCopy());
        var feedback = result.putObject("consumerFeedback").put("recordKey", key).put("recordVersion", 2)
                .put("disposition", "REJECTED").put("reason", "Synthetic receipt replay only; no actual worker method use.")
                .put("outcome", "UNASSESSED");
        feedback.putArray("outcomeEvidenceRefs");
        return result;
    }

    private static ObjectNode evidence() {
        Instant fetchedAt = Instant.now();
        return evidence(fetchedAt, fetchedAt.plusSeconds(3600));
    }

    private static ObjectNode evidence(Instant fetchedAt, Instant expiry) {
        var root = JSON.createObjectNode().put("cogneeQuery", "synthetic source A investigation");
        var request = root.putObject("consumerRequest").put("missionRef", "fixture:mission:independent-cli")
                .put("workspaceRef", WORKSPACE).put("knowledgeProjectRef", KNOWLEDGE_PROJECT)
                .put("consumerProjectRef", CONSUMER_PROJECT);
        request.putObject("currentRepositoryRevisions").put("fixture:repository:cli", "fixture:revision:r1");
        var read = root.putObject("providerRead").put("workspaceRef", WORKSPACE)
                .put("projectRef", KNOWLEDGE_PROJECT).put("fetchedAt", fetchedAt.toString());
        read.putArray("entries").add(entry(KEY_A, TEXT_A, fetchedAt, expiry)).add(entry(KEY_B, TEXT_B, fetchedAt, expiry));
        return root;
    }

    private static ObjectNode entry(String key, String text, Instant fetchedAt, Instant expiry) {
        var proposal = new WorkspaceKnowledgeProposal(key, WORKSPACE, List.of("fixture:source:" + key),
                KnowledgeType.PROCEDURAL, text, "Independent CLI fixture only", "Only the synthetic pinned repository",
                List.of("No live governance, worker use, or publication authority"), List.of("fixture:evidence:source"), List.of());
        var governance = new GovernedWorkspaceKnowledge(proposal, KnowledgeGovernanceDecision.APPROVED,
                "fixture:decision:" + key, "fixture:test-only-actor", "fixture:test-only-policy",
                fetchedAt.minusSeconds(120).toString(), List.of("fixture:evidence:synthetic-decision"));
        String digest = SwarmKnowledgeGateway.proposalDigest(proposal);
        var entry = JSON.createObjectNode().put("knowledgeRef", "fixture:knowledge:" + key)
                .put("providerRevision", "fixture:provider:r1").put("recordKey", key).put("recordVersion", 2)
                .put("proposalDigest", digest).put("decisionRecordKey", key).put("decisionRecordVersion", 2)
                .put("decisionProposalDigest", digest).put("lifecycle", "CURRENT")
                .put("observedAt", fetchedAt.minusSeconds(60).toString()).put("validUntil", expiry.toString())
                .put("visibility", "WORKSPACE_AUTHORIZED");
        entry.set("proposal", JSON.valueToTree(proposal));
        entry.set("governance", JSON.valueToTree(governance));
        entry.putObject("repositoryRevisions").put("fixture:repository:cli", "fixture:revision:r1");
        entry.putArray("applicableProjectRefs").add(CONSUMER_PROJECT);
        return entry;
    }

    private static ObjectNode syntheticRawEvidence(Instant fetchedAt, ObjectNode... issues) {
        ObjectNode input = evidence(fetchedAt, fetchedAt.plusSeconds(3600));
        input.remove("providerRead");
        ObjectNode revisions = (ObjectNode) input.path("consumerRequest").path("currentRepositoryRevisions");
        revisions.removeAll();
        revisions.put("github.com/example/independent-cli-synthetic", "fixture:revision:r1");
        ObjectNode page = input.putObject("providerReadRaw").put("fetchedAt", fetchedAt.toString())
                .putArray("responses").addObject();
        page.put("has_more", false).put("offset", 0).put("limit", 50).put("total", issues.length);
        ArrayNode rows = page.putArray("issues");
        for (ObjectNode issue : issues) rows.add(issue);
        return input;
    }

    /** Construct native-shaped synthetic input; this helper does not parse or implement production mapping. */
    private static ObjectNode syntheticRawIssue(
            String bodyKey, String indexKey, String issueId, int providerRevision,
            Instant fetchedAt, boolean unsupportedType) throws Exception {
        ObjectNode typed = entry(bodyKey, bodyKey.equals(KEY_A) ? TEXT_A : TEXT_B,
                fetchedAt, fetchedAt.plusSeconds(3600));
        ObjectNode proposal = ((ObjectNode) typed.path("proposal")).deepCopy();
        if (unsupportedType) proposal.put("knowledgeType", "synthetic_test_only_unknown_knowledge_type");
        String proposalDigest = sha256(JSON.writeValueAsString(sortedCopy(proposal)));
        JsonNode originalGovernance = typed.path("governance");
        ObjectNode body = JSON.createObjectNode().put("recordKind", "RC10_WORKSPACE_KNOWLEDGE")
                .put("schemaVersion", "0.1").put("recordKey", bodyKey).put("recordVersion", 2)
                .put("workspaceRef", WORKSPACE).put("projectRef", KNOWLEDGE_PROJECT);
        body.set("proposal", proposal);
        ObjectNode learning = body.putObject("learningSource")
                .put("learningSourceRef", "fixture:learning:synthetic-independent-raw")
                .put("workspaceRef", WORKSPACE).put("missionRef", "fixture:mission:independent-cli")
                .put("closureSummaryRef", "fixture:closure:synthetic-test-only");
        learning.putArray("subjectRefs").add("fixture:subject:synthetic-test-only");
        learning.putArray("sourceRefs").add("fixture:source:synthetic-test-only");
        learning.putArray("evidenceRefs").add("fixture:evidence:synthetic-test-only");
        body.putArray("applicableProjectRefs").add(CONSUMER_PROJECT);
        body.put("visibility", "WORKSPACE_AUTHORIZED");
        ObjectNode governance = body.putObject("governance").put("decision", "APPROVED")
                .put("decisionRef", originalGovernance.path("decisionRef").asText())
                .put("actorRef", originalGovernance.path("decidedBy").asText())
                .put("policyRef", originalGovernance.path("policyRef").asText())
                .put("decidedAt", originalGovernance.path("decidedAt").asText())
                .put("recordKey", bodyKey).put("recordVersion", 2).put("proposalDigest", proposalDigest);
        governance.set("evidenceRefs", originalGovernance.path("decisionEvidenceRefs").deepCopy());
        body.putObject("lifecycle").put("state", "CURRENT");
        ObjectNode freshness = body.putObject("freshness").put("observedAt", typed.path("observedAt").asText())
                .put("validUntil", typed.path("validUntil").asText());
        freshness.putArray("repositoryRevisions").addObject()
                .put("repository", "https://github.com/example/independent-cli-synthetic.git")
                .put("revision", "fixture:revision:r1");
        body.putArray("limitations").add("Synthetic test-only record; no real approval, provider call, or worker use.");
        body.putArray("conflictRefs");
        ObjectNode issue = JSON.createObjectNode().put("id", issueId)
                .put("identifier", "SYNTHETIC-RAW-" + providerRevision).put("workspace_id", WORKSPACE)
                .put("project_id", KNOWLEDGE_PROJECT).put("revision", providerRevision)
                .put("title", "Synthetic test-only independent native-row fixture")
                .put("description", "Synthetic test-only record; no real provider approval.\n```json\n"
                        + JSON.writeValueAsString(body) + "\n```\n");
        issue.putObject("metadata").put("rc10RecordKind", "RC10_WORKSPACE_KNOWLEDGE")
                .put("rc10RecordKey", indexKey).put("rc10RecordVersion", 2)
                .put("rc10Decision", "APPROVED").put("rc10Lifecycle", "CURRENT");
        return issue;
    }

    private static ObjectNode chunk(int index, String text, String project) throws Exception {
        var metadata = JSON.createObjectNode().put("sourceId", KEY_A).put("sourceRevision", 2)
                .put("providerRevision", "fixture:provider:r1").put("sourceRef", "fixture:source:" + KEY_A)
                .put("workspaceRef", WORKSPACE).put("projectRef", project).put("contentSha256", sha256(TEXT_A));
        return JSON.createObjectNode().put("document_id", "fixture:document:a").put("chunk_index", index)
                .put("type", "IndexSchema").put("version", 1).put("text", text).put("external_metadata", metadata.toString());
    }

    private static ArrayNode hits(String dataset, ObjectNode... chunks) {
        var hit = JSON.createObjectNode().put("dataset_id", dataset).put("dataset_name", "fixture:dataset:independent");
        var results = hit.putArray("search_result");
        for (var chunk : chunks) results.add(chunk);
        return JSON.createArrayNode().add(hit);
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static JsonNode sortedCopy(JsonNode value) {
        if (value.isObject()) {
            var names = new ArrayList<String>();
            value.fieldNames().forEachRemaining(names::add);
            names.sort(String::compareTo);
            var sorted = JSON.createObjectNode();
            names.forEach(name -> sorted.set(name, sortedCopy(value.get(name))));
            return sorted;
        }
        if (value.isArray()) {
            var sorted = JSON.createArrayNode();
            value.forEach(element -> sorted.add(sortedCopy(element)));
            return sorted;
        }
        return value.deepCopy();
    }

    private static final class SearchMock implements AutoCloseable {
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicInteger status = new AtomicInteger(200);
        private final AtomicReference<String> body = new AtomicReference<>("[]");
        private final AtomicReference<JsonNode> request = new AtomicReference<>();
        private final AtomicBoolean stopped = new AtomicBoolean();
        private final HttpServer server;

        private SearchMock() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/search", exchange -> {
                try {
                    calls.incrementAndGet();
                    request.set(JSON.readTree(exchange.getRequestBody()));
                    byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(status.get(), bytes.length);
                    exchange.getResponseBody().write(bytes);
                } finally {
                    exchange.close();
                }
            });
            server.start();
        }

        private void reply(int code, JsonNode response) {
            status.set(code);
            body.set(response.toString());
        }

        private String endpoint() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            if (stopped.compareAndSet(false, true)) server.stop(0);
        }
    }
}

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
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Dev204KnowledgeConsumerCliTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String WORKSPACE = "fixture:workspace:consumer-cli";
    private static final String KNOWLEDGE_PROJECT = "fixture:project:knowledge";
    private static final String CONSUMER_PROJECT = "fixture:project:consumer";
    private static final String MISSION = "fixture:mission:consumer-cli";
    private static final String REPOSITORY_KEY = "fixture:repository:consumer-cli";
    private static final String REVISION = "fixture:revision:one";
    private static final String RECORD_KEY = "fixture:method:consumer-cli";
    private static final String COGNEE_DATASET = "00000000-0000-7000-8000-000000000001";

    @TempDir Path tempDir;

    @Test
    void selectionReceiptCanBeReplayedIntoExactVersionBoundFeedback() throws Exception {
        ObjectNode selectionInput = evidence(true);
        JsonNode selection = invoke(selectionInput, "selection");

        assertThat(selection.path("phase").asText()).isEqualTo("selection");
        assertThat(selection.path("status").asText()).isEqualTo("ELIGIBLE_CONTEXT");
        assertThat(selection.path("context").path("evaluatedAt").asText()).isNotBlank();
        assertThat(selection.path("context").path("selected")).hasSize(1);
        assertThat(selection.path("claimBoundary").asText()).contains("NO_WRITE");
        assertThat(selection.path("selectionReceiptSha256").asText()).hasSize(64);

        ObjectNode feedbackInput = selectionInput.deepCopy();
        feedbackInput.set("selectionReceipt", selection.deepCopy());
        feedbackInput.set("consumerFeedback", JSON.createObjectNode()
                .put("recordKey", RECORD_KEY)
                .put("recordVersion", 1)
                .put("disposition", "ADOPTED")
                .put("reason", "Synthetic consumer actually used the selected method.")
                .put("actionRef", "fixture:action:cli")
                .put("resultRef", "fixture:result:cli")
                .put("outcome", "UNASSESSED")
                .set("outcomeEvidenceRefs", JSON.createArrayNode()));

        JsonNode result = invoke(feedbackInput, "feedback");
        JsonNode feedback = result.path("feedback");
        assertThat(result.path("status").asText()).isEqualTo("FEEDBACK_BUILT");
        assertThat(feedback.path("recordKey").asText()).isEqualTo(RECORD_KEY);
        assertThat(feedback.path("recordVersion").asInt()).isEqualTo(1);
        assertThat(feedback.path("providerRevision").asText()).isEqualTo("fixture:provider:r1");
        assertThat(feedback.path("decisionRef").asText()).isEqualTo("fixture:decision:consumer-cli");
        assertThat(feedback.path("actionRef").asText()).isEqualTo("fixture:action:cli");
        assertThat(feedback.path("resultRef").asText()).isEqualTo("fixture:result:cli");
        assertThat(result.path("selectionReceiptSha256").asText())
                .isEqualTo(selection.path("selectionReceiptSha256").asText());
        assertThat(result.path("claimBoundary").asText()).contains("NO_WRITE");
    }

    @Test
    void cogneeCandidatesPassThroughFullGovernedSelectionAndExactReceiptReplay() throws Exception {
        ObjectNode selectionInput = evidence(true).put("cogneeQuery", "same-case bounded source investigation");
        String content = "source-bound procedural context";
        ObjectNode metadata = JSON.createObjectNode()
                .put("sourceId", RECORD_KEY)
                .put("sourceRevision", 1)
                .put("providerRevision", "fixture:provider:r1")
                .put("sourceRef", selectionInput.path("providerRead").path("entries").get(0)
                        .path("proposal").path("sourceRefs").get(0).asText())
                .put("workspaceRef", WORKSPACE)
                .put("projectRef", KNOWLEDGE_PROJECT)
                .put("contentSha256", sha256(content));
        ObjectNode chunk = JSON.createObjectNode()
                .put("document_id", "fixture-document-consumer-cli")
                .put("chunk_index", 0)
                .put("type", "IndexSchema")
                .put("text", content)
                .put("external_metadata", JSON.writeValueAsString(metadata));
        JsonNode searchResponse = JSON.createArrayNode().add(JSON.createObjectNode()
                .put("dataset_id", COGNEE_DATASET)
                .set("search_result", JSON.createArrayNode().add(chunk)));
        var requestBody = new java.util.concurrent.atomic.AtomicReference<JsonNode>();
        var callCount = new java.util.concurrent.atomic.AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/search", exchange -> {
            callCount.incrementAndGet();
            requestBody.set(JSON.readTree(exchange.getRequestBody()));
            byte[] bytes = JSON.writeValueAsBytes(searchResponse);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            String endpoint = "http://127.0.0.1:" + server.getAddress().getPort();
            JsonNode selection = invoke(selectionInput, "selection",
                    "--dataset-id", COGNEE_DATASET, "--base-url", endpoint);

            assertThat(requestBody.get().path("dataset_ids").get(0).asText()).isEqualTo(COGNEE_DATASET);
            assertThat(requestBody.get().path("query").asText()).isEqualTo("same-case bounded source investigation");
            assertThat(requestBody.get().path("search_type").asText()).isEqualTo("CHUNKS");
            assertThat(selection.path("status").asText()).isEqualTo("ELIGIBLE_CONTEXT");
            assertThat(selection.path("context").path("selected")).hasSize(1);
            assertThat(selection.path("cogneeSelection").path("searchStatus").asText()).isEqualTo("SEARCHED");
            assertThat(selection.path("cogneeSelection").path("candidateSearch").path("candidates")).hasSize(1);
            assertThat(selection.path("claimBoundary").asText()).contains("FULL_GOVERNED_READ");

            ObjectNode feedbackInput = feedbackInput(selection, selectionInput);
            JsonNode feedback = invoke(feedbackInput, "feedback");
            assertThat(feedback.path("status").asText()).isEqualTo("FEEDBACK_BUILT");
            assertThat(feedback.path("selectionReceiptSha256").asText())
                    .isEqualTo(selection.path("selectionReceiptSha256").asText());
            assertThat(callCount.get()).isEqualTo(1);

            ObjectNode forged = feedbackInput(selection, selectionInput);
            ((ObjectNode) forged.path("selectionReceipt").path("cogneeSelection")
                    .path("candidateSearch").path("candidates").get(0)).put("sourceRevision", 2);
            assertThatThrownBy(() -> invoke(forged, "feedback"))
                    .hasMessageContaining("Cognee candidate receipt digest");
            assertThat(callCount.get()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void unavailableCogneePrintsEvidenceAndBlocksUnverifiedOptionalExit() throws Exception {
        ObjectNode selectionInput = evidence(true).put("cogneeQuery", "same-case bounded source investigation");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/search", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.start();
        try {
            String endpoint = "http://127.0.0.1:" + server.getAddress().getPort();
            JsonNode result = invoke(selectionInput, "selection",
                    "--dataset-id", COGNEE_DATASET, "--base-url", endpoint);

            assertThat(result.path("status").asText()).isEqualTo("COGNEE_UNAVAILABLE_UNQUALIFIED_CONTEXT");
            assertThat(result.path("context").path("selected")).isEmpty();
            assertThat(result.path("context").path("mayProceedWithoutKnowledge").asBoolean()).isFalse();
            assertThat(result.path("cogneeSelection").path("searchStatus").asText()).isEqualTo("UNAVAILABLE");
            assertThat(result.path("cogneeSelection").path("unavailableReason").asText()).contains("HTTP 503");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void feedbackRejectsChangedProviderSnapshotAndForgedSelectionContext() throws Exception {
        ObjectNode sourceEvidence = evidence(true);
        JsonNode selection = invoke(sourceEvidence, "selection");

        ObjectNode changedSnapshot = feedbackInput(selection, sourceEvidence);
        ObjectNode providerRead = (ObjectNode) changedSnapshot.get("providerRead");
        ArrayNode changedEntries = (ArrayNode) providerRead.get("entries");
        ((ObjectNode) changedEntries.get(0))
                .put("providerRevision", "fixture:provider:changed");
        assertThatThrownBy(() -> invoke(changedSnapshot, "feedback"))
                .hasMessageContaining("snapshot digest");

        ObjectNode forgedContext = feedbackInput(selection, sourceEvidence);
        ((ArrayNode) forgedContext.path("selectionReceipt").path("context").path("selected")).removeAll();
        assertThatThrownBy(() -> invoke(forgedContext, "feedback"))
                .hasMessageContaining("selection receipt content");

        ObjectNode forgedEvaluationTime = feedbackInput(selection, sourceEvidence);
        ObjectNode receipt = (ObjectNode) forgedEvaluationTime.get("selectionReceipt");
        ObjectNode context = (ObjectNode) receipt.get("context");
        context.put("evaluatedAt", Instant.parse(context.path("evaluatedAt").asText())
                .plusSeconds(1).truncatedTo(ChronoUnit.SECONDS).toString());
        assertThatThrownBy(() -> invoke(forgedEvaluationTime, "feedback"))
                .hasMessageContaining("selection receipt digest");
    }

    @Test
    void emptyKnowledgeReadPrintsEvidenceAndBlocksUnverifiedOptionalExit() throws Exception {
        JsonNode result = invoke(evidence(false), "selection");

        assertThat(result.path("status").asText()).isEqualTo("NO_ELIGIBLE_RECORDS");
        assertThat(result.path("context").path("selected")).isEmpty();
        assertThat(result.path("context").path("mayProceedWithoutKnowledge").asBoolean()).isFalse();
    }

    @Test
    void preservesOuterProviderConflictRefsWhenProposalConflictRefsAreEmpty() throws Exception {
        ObjectNode input = evidence(true);
        ((ObjectNode) input.path("providerRead").path("entries").get(0))
                .putArray("sourceConflictRefs").add("issue:fixture:unresolved-product-context");

        JsonNode result = invoke(input, "selection");

        assertThat(result.path("context").path("selected")).isEmpty();
        assertThat(result.path("context").path("excluded")).singleElement()
                .satisfies(exclusion -> {
                    assertThat(exclusion.path("recordKey").asText()).isEqualTo(RECORD_KEY);
                    assertThat(exclusion.path("reason").asText()).isEqualTo("UNRESOLVED_CONFLICT");
                });
    }

    @Test
    void providerRecordVersionsMustBeExactJsonIntegers() throws Exception {
        for (String field : List.of("recordVersion", "decisionRecordVersion")) {
            ObjectNode evidence = evidence(true);
            ((ObjectNode) ((ArrayNode) evidence.path("providerRead").path("entries")).get(0))
                    .put(field, 1.5);

            assertThatThrownBy(() -> invoke(evidence, "selection"))
                    .hasMessageContaining("providerRead.entries[0]." + field + " must be an integer");

            ObjectNode overflow = evidence(true);
            ((ObjectNode) ((ArrayNode) overflow.path("providerRead").path("entries")).get(0))
                    .put(field, (long) Integer.MAX_VALUE + 1L);
            assertThatThrownBy(() -> invoke(overflow, "selection"))
                    .hasMessageContaining("providerRead.entries[0]." + field + " exceeds the supported integer range");
        }
    }

    @Test
    void consumerFeedbackRecordVersionMustBeAnExactJsonInteger() throws Exception {
        ObjectNode sourceEvidence = evidence(true);
        JsonNode selection = invoke(sourceEvidence, "selection");
        ObjectNode invalidFeedback = feedbackInput(selection, sourceEvidence);
        ((ObjectNode) invalidFeedback.path("consumerFeedback")).put("recordVersion", 1.5);

        assertThatThrownBy(() -> invoke(invalidFeedback, "feedback"))
                .hasMessageContaining("recordVersion must be a positive integer");

        ObjectNode overflowFeedback = feedbackInput(selection, sourceEvidence);
        ((ObjectNode) overflowFeedback.path("consumerFeedback")).put("recordVersion", (long) Integer.MAX_VALUE + 1L);
        assertThatThrownBy(() -> invoke(overflowFeedback, "feedback"))
                .hasMessageContaining("recordVersion must be a positive integer");
    }

    @Test
    void mapsRawMulticaIssueRowsAndPreservesOuterConflictsAndRecordLimitations() throws Exception {
        ObjectNode input = rawEvidence();
        ObjectNode conflictEntry = typedEntryFor(RECORD_KEY + ":conflicted");
        ((ObjectNode) conflictEntry.path("proposal")).putArray("conflictRefs");
        ObjectNode availableEntry = typedEntryFor(RECORD_KEY + ":available");
        appendRawIssue(input, rawIssue(conflictEntry, "00000000-0000-7000-8000-000000000001",
                41, List.of("issue:fixture:outer-conflict"), true, false, List.of()));
        appendRawIssue(input, rawIssue(availableEntry, "00000000-0000-7000-8000-000000000004",
                42, List.of(), false, false, List.of("Record-level limitation from the provider.")));

        JsonNode result = invoke(input, "selection");

        assertThat(result.path("context").path("selected")).as(result.toPrettyString()).singleElement()
                .satisfies(selected -> {
                    assertThat(selected.path("recordKey").asText()).isEqualTo(RECORD_KEY + ":available");
                    assertThat(selected.path("providerRevision").asText()).isEqualTo("42");
                    assertThat(selected.path("repositoryRevisions").path("github.com/example/consumer-cli").asText())
                            .isEqualTo(REVISION);
                    assertThat(selected.path("sourceLimitations").get(0).asText())
                            .isEqualTo("Record-level limitation from the provider.");
                });
        assertThat(result.path("context").path("excluded")).singleElement()
                .satisfies(excluded -> {
                    assertThat(excluded.path("recordKey").asText()).isEqualTo(RECORD_KEY + ":conflicted");
                    assertThat(excluded.path("reason").asText()).isEqualTo("UNRESOLVED_CONFLICT");
                });
        assertThat(result.path("providerMappingStatus").asText()).isEqualTo("COMPLETE");
        assertThat(result.path("providerMappingExclusions")).isEmpty();
    }

    @Test
    void reportsMalformedRawRowsExplicitlyWhileKeepingOtherRowsAvailable() throws Exception {
        ObjectNode input = rawEvidence();
        appendRawIssue(input, rawIssue(typedEntryFor(RECORD_KEY + ":valid"),
                "00000000-0000-7000-8000-000000000006", 51, List.of(), false, false, List.of()));
        appendRawIssue(input, rawIssue(typedEntryFor(RECORD_KEY + ":malformed"),
                "00000000-0000-7000-8000-000000000008", 52, List.of(), false, true, List.of()));

        JsonNode result = invoke(input, "selection");

        assertThat(result.path("context").path("selected")).as(result.toPrettyString()).singleElement()
                .satisfies(selected -> assertThat(selected.path("recordKey").asText())
                        .isEqualTo(RECORD_KEY + ":valid"));
        assertThat(result.path("providerMappingStatus").asText()).isEqualTo("PARTIAL");
        assertThat(result.path("providerMappingExclusions")).singleElement()
                .satisfies(exclusion -> {
                    assertThat(exclusion.path("issueRef").asText())
                            .isEqualTo("multica:issue:00000000-0000-7000-8000-000000000008");
                    assertThat(exclusion.path("recordKey").asText()).isEqualTo(RECORD_KEY + ":malformed");
                    assertThat(exclusion.path("reasonCode").asText()).isEqualTo("UNSUPPORTED_KNOWLEDGE_TYPE");
                    assertThat(exclusion.path("sourceRowSha256").asText()).matches("[0-9a-f]{64}");
                });
    }

    @Test
    void rawProviderDuplicatesAreNotSilentlySelected() throws Exception {
        ObjectNode input = rawEvidence();
        ObjectNode duplicate = typedEntryFor(RECORD_KEY);
        appendRawIssue(input, rawIssue(duplicate, "00000000-0000-7000-8000-000000000009",
                61, List.of(), false, false, List.of()));
        appendRawIssue(input, rawIssue(duplicate, "00000000-0000-7000-8000-00000000000a",
                62, List.of(), false, false, List.of()));

        JsonNode result = invoke(input, "selection");

        assertThat(result.path("context").path("selected")).isEmpty();
        assertThat(result.path("context").path("excluded")).hasSize(2);
        assertThat(result.path("context").path("excluded")).allSatisfy(excluded ->
                assertThat(excluded.path("reason").asText()).isEqualTo("DUPLICATE_RECORD_KEY"));
    }

    @Test
    void malformedDuplicateQuarantinesTheOtherwiseMappableRecordWithTheSameKey() throws Exception {
        ObjectNode input = rawEvidence();
        ObjectNode sameKey = typedEntryFor(RECORD_KEY);
        appendRawIssue(input, rawIssue(sameKey, "00000000-0000-7000-8000-00000000000e",
                91, List.of(), false, false, List.of()));
        appendRawIssue(input, rawIssue(sameKey, "00000000-0000-7000-8000-000000000019",
                92, List.of(), false, true, List.of()));

        JsonNode result = invoke(input, "selection");

        assertThat(result.path("context").path("selected")).isEmpty();
        assertThat(result.path("providerMappingExclusions")).hasSize(2);
        assertThat(result.path("providerMappingExclusions")).allSatisfy(exclusion ->
                assertThat(exclusion.path("reasonCode").asText()).isEqualTo("DUPLICATE_RECORD_KEY"));
        assertThat(result.path("providerMappingExclusions")).allSatisfy(exclusion ->
                assertThat(exclusion.path("recordKey").asText()).isEqualTo(RECORD_KEY));
    }

    @Test
    void rawBodyAndIndexDisagreementExcludesTheRowWithoutTrustingEitherSide() throws Exception {
        ObjectNode input = rawEvidence();
        ObjectNode valid = rawIssue(typedEntryFor(RECORD_KEY + ":other"),
                "00000000-0000-7000-8000-00000000001e", 101, List.of(), false, false, List.of());
        appendRawIssue(input, valid);
        ObjectNode mismatch = rawIssue(typedEntryFor(RECORD_KEY + ":body"),
                "00000000-0000-7000-8000-000000000020", 102, List.of(), false, false, List.of());
        ((ObjectNode) mismatch.path("metadata")).put("rc10RecordKey", RECORD_KEY + ":index");
        appendRawIssue(input, mismatch);

        JsonNode result = invoke(input, "selection");

        assertThat(result.path("context").path("selected")).as(result.toPrettyString()).singleElement()
                .satisfies(selected -> assertThat(selected.path("recordKey").asText())
                        .isEqualTo(RECORD_KEY + ":other"));
        assertThat(result.path("providerMappingExclusions")).singleElement()
                .satisfies(exclusion -> {
                    assertThat(exclusion.path("reasonCode").asText()).isEqualTo("BODY_INDEX_MISMATCH");
                    assertThat(exclusion.path("recordKey").asText()).isEqualTo(RECORD_KEY + ":body");
                    assertThat(exclusion.path("indexedRecordKey").asText()).isEqualTo(RECORD_KEY + ":index");
                });
    }

    @Test
    void bodyIndexMismatchQuarantinesEveryRowSharingEitherDisputedRecordKey() throws Exception {
        ObjectNode input = rawEvidence();
        ObjectNode mismatched = rawIssue(typedEntryFor(RECORD_KEY + ":body"),
                "00000000-0000-7000-8000-000000000021", 103, List.of(), false, false, List.of());
        ((ObjectNode) mismatched.path("metadata")).put("rc10RecordKey", RECORD_KEY + ":index");
        appendRawIssue(input, mismatched);
        appendRawIssue(input, rawIssue(typedEntryFor(RECORD_KEY + ":body"),
                "00000000-0000-7000-8000-000000000022", 104, List.of(), false, false, List.of()));
        appendRawIssue(input, rawIssue(typedEntryFor(RECORD_KEY + ":index"),
                "00000000-0000-7000-8000-000000000023", 105, List.of(), false, false, List.of()));

        JsonNode result = invoke(input, "selection");

        assertThat(result.path("context").path("selected")).isEmpty();
        assertThat(result.path("providerMappingExclusions")).hasSize(3);
        assertThat(result.path("providerMappingExclusions")).allSatisfy(exclusion ->
                assertThat(exclusion.path("reasonCode").asText()).isEqualTo("BODY_INDEX_MISMATCH"));
    }

    @Test
    void mapsCompleteMultiPageRawProviderReadInProviderOrder() throws Exception {
        ObjectNode input = rawEvidence();
        ArrayNode responses = (ArrayNode) input.path("providerReadRaw").path("responses");
        responses.removeAll();
        responses.addObject()
                .put("has_more", true)
                .putArray("issues")
                .add(rawIssue(typedEntryFor(RECORD_KEY + ":page-1"),
                        "00000000-0000-7000-8000-000000000002", 121, List.of(), false, false, List.of()));
        ObjectNode firstPage = (ObjectNode) responses.get(0);
        firstPage.put("limit", 1).put("offset", 0).put("total", 2);
        responses.addObject()
                .put("has_more", false)
                .putArray("issues")
                .add(rawIssue(typedEntryFor(RECORD_KEY + ":page-2"),
                        "00000000-0000-7000-8000-000000000003", 122, List.of(), true, false, List.of()));
        ObjectNode secondPage = (ObjectNode) responses.get(1);
        secondPage.put("limit", 1).put("offset", 1).put("total", 2);

        JsonNode result = invoke(input, "selection");

        assertThat(result.path("context").path("selected"))
                .extracting(selected -> selected.path("recordKey").asText())
                .containsExactly(RECORD_KEY + ":page-1", RECORD_KEY + ":page-2");
        assertThat(result.path("providerMappingStatus").asText()).isEqualTo("COMPLETE");
    }

    @Test
    void rawProviderFeedbackReplayBindsTheWholeProviderResponse() throws Exception {
        ObjectNode source = rawEvidence();
        appendRawIssue(source, rawIssue(typedEntryFor(RECORD_KEY),
                "00000000-0000-7000-8000-00000000000b", 71, List.of(), true, false, List.of()));
        JsonNode selection = invoke(source, "selection");
        ObjectNode feedback = feedbackInput(selection, source);

        JsonNode result = invoke(feedback, "feedback");
        assertThat(result.path("status").asText()).isEqualTo("FEEDBACK_BUILT");

        ObjectNode changedRaw = feedback.deepCopy();
        ObjectNode issue = (ObjectNode) changedRaw.path("providerReadRaw").path("responses").get(0)
                .path("issues").get(0);
        issue.put("revision", 72);
        assertThatThrownBy(() -> invoke(changedRaw, "feedback"))
                .hasMessageContaining("provider and candidate snapshot digest");
    }

    @Test
    void rawProviderFeedbackReplayBindsRowsExcludedFromTypedMapping() throws Exception {
        ObjectNode source = rawEvidence();
        appendRawIssue(source, rawIssue(typedEntryFor(RECORD_KEY),
                "00000000-0000-7000-8000-00000000000b", 71, List.of(), true, false, List.of()));
        appendRawIssue(source, rawIssue(typedEntryFor(RECORD_KEY + ":unsupported"),
                "00000000-0000-7000-8000-00000000000d", 72, List.of(), true, true, List.of()));
        JsonNode selection = invoke(source, "selection");
        ObjectNode feedback = feedbackInput(selection, source);

        assertThat(selection.path("context").path("selected")).hasSize(1);
        assertThat(selection.path("providerMappingExclusions")).hasSize(1);
        assertThat(invoke(feedback, "feedback").path("status").asText()).isEqualTo("FEEDBACK_BUILT");

        ObjectNode changedExcludedRow = feedback.deepCopy();
        ObjectNode issue = (ObjectNode) changedExcludedRow.path("providerReadRaw").path("responses").get(0)
                .path("issues").get(1);
        issue.put("title", "changed excluded provider row; mapped entry is unchanged");
        assertThatThrownBy(() -> invoke(changedExcludedRow, "feedback"))
                .hasMessageContaining("provider and candidate snapshot digest");
    }

    @Test
    void rawProviderReadRejectsIncompletePagesAndCrossScopeRows() throws Exception {
        ObjectNode incomplete = rawEvidence();
        ObjectNode incompletePage = (ObjectNode) incomplete.path("providerReadRaw").path("responses").get(0);
        incompletePage.put("total", 2);
        incompletePage.put("has_more", true);
        assertThatThrownBy(() -> invoke(incomplete, "selection"))
                .hasMessageContaining("providerReadRaw.responses are incomplete");

        ObjectNode crossed = rawEvidence();
        ObjectNode wrongWorkspace = rawIssue(typedEntryFor(RECORD_KEY),
                "00000000-0000-7000-8000-00000000000d", 81, List.of(), false, false, List.of());
        wrongWorkspace.put("workspace_id", "fixture:other-workspace");
        appendRawIssue(crossed, wrongWorkspace);
        assertThatThrownBy(() -> invoke(crossed, "selection"))
                .hasMessageContaining("provider issue crossed the consumer workspace or knowledge project");
    }

    private ObjectNode feedbackInput(JsonNode selection, ObjectNode sourceEvidence) {
        ObjectNode input = sourceEvidence.deepCopy();
        input.set("selectionReceipt", selection.deepCopy());
        input.set("consumerFeedback", JSON.createObjectNode()
                .put("recordKey", RECORD_KEY)
                .put("recordVersion", 1)
                .put("disposition", "ADOPTED")
                .put("reason", "Synthetic consumer actually used the selected method.")
                .put("actionRef", "fixture:action:cli")
                .put("resultRef", "fixture:result:cli")
                .put("outcome", "UNASSESSED")
                .set("outcomeEvidenceRefs", JSON.createArrayNode()));
        return input;
    }

    private JsonNode invoke(ObjectNode evidence, String phase, String... extraOptions) throws Exception {
        Path input = tempDir.resolve("consumer-" + phase + ".json");
        Files.writeString(input, JSON.writeValueAsString(evidence), StandardCharsets.UTF_8);
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream previous = System.out;
        RuntimeException exit = null;
        List<String> arguments = new ArrayList<>(List.of(
                "dev204-knowledge-consume", "--evidence-file", input.toString(), "--phase", phase));
        arguments.addAll(List.of(extraOptions));
        try (PrintStream replacement = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            System.setOut(replacement);
            try {
                if (List.of(extraOptions).contains("--dataset-id")) {
                    // Synthetic loopback composition, not an actor authorization or production entry-point proof.
                    assertThat(Dev204Cli.handles(arguments.toArray(String[]::new), configured ->
                            new com.featuredeliveryintelligence.fdi.orchestration.CogneeSearchClient(
                                    java.net.URI.create(configured.get("--base-url"))))).isTrue();
                } else {
                    assertThat(Dev204Cli.handles(arguments.toArray(String[]::new))).isTrue();
                }
            }
            catch (RuntimeException failure) { exit = failure; }
        } finally {
            System.setOut(previous);
        }
        if (captured.size() == 0 && exit != null) throw exit;
        JsonNode result = JSON.readTree(captured.toString(StandardCharsets.UTF_8));
        assertThat(result).isNotNull();
        if ("selection".equals(phase) && result.path("context").path("selected").isEmpty()) {
            assertThat(exit).isInstanceOf(IllegalStateException.class).hasMessageContaining("optional-context policy");
        } else if (exit != null) {
            throw exit;
        }
        return result;
    }

    private static ObjectNode evidence(boolean includeEligibleEntry) {
        Instant fetchedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        ObjectNode root = JSON.createObjectNode();
        ObjectNode request = root.putObject("consumerRequest");
        request.put("missionRef", MISSION);
        request.put("workspaceRef", WORKSPACE);
        request.put("knowledgeProjectRef", KNOWLEDGE_PROJECT);
        request.put("consumerProjectRef", CONSUMER_PROJECT);
        request.putObject("currentRepositoryRevisions").put(REPOSITORY_KEY, REVISION);

        ObjectNode read = root.putObject("providerRead");
        read.put("workspaceRef", WORKSPACE);
        read.put("projectRef", KNOWLEDGE_PROJECT);
        read.put("fetchedAt", fetchedAt.toString());
        ArrayNode entries = read.putArray("entries");
        if (!includeEligibleEntry) return root;

        WorkspaceKnowledgeProposal proposal = new WorkspaceKnowledgeProposal(
                RECORD_KEY,
                WORKSPACE,
                List.of("fixture:source:method-v1"),
                KnowledgeType.PROCEDURAL,
                "Use the exact scoped fixture lookup before deciding the next action.",
                "consumer-cli-fixture",
                "Only for the pinned repository revision.",
                List.of("Local synthetic test fixture only."),
                List.of("fixture:evidence:proposal-v1"),
                List.of());
        String digest = SwarmKnowledgeGateway.proposalDigest(proposal);
        GovernedWorkspaceKnowledge governance = new GovernedWorkspaceKnowledge(
                proposal,
                KnowledgeGovernanceDecision.APPROVED,
                "fixture:decision:consumer-cli",
                "fixture:actor:policy-owner",
                "fixture:policy:consumer-cli",
                fetchedAt.minusSeconds(60).toString(),
                List.of("fixture:evidence:decision-v1"));

        ObjectNode entry = entries.addObject();
        entry.put("knowledgeRef", "fixture:knowledge:consumer-cli");
        entry.put("providerRevision", "fixture:provider:r1");
        entry.put("recordKey", RECORD_KEY);
        entry.put("recordVersion", 1);
        entry.set("proposal", JSON.valueToTree(proposal));
        entry.set("governance", JSON.valueToTree(governance));
        entry.put("proposalDigest", digest);
        entry.put("decisionRecordKey", RECORD_KEY);
        entry.put("decisionRecordVersion", 1);
        entry.put("decisionProposalDigest", digest);
        entry.put("lifecycle", "CURRENT");
        entry.put("observedAt", fetchedAt.minusSeconds(120).toString());
        entry.put("validUntil", fetchedAt.plusSeconds(3600).toString());
        entry.putObject("repositoryRevisions").put(REPOSITORY_KEY, REVISION);
        entry.putArray("applicableProjectRefs").add(CONSUMER_PROJECT);
        entry.put("visibility", "WORKSPACE_AUTHORIZED");
        return root;
    }

    private static ObjectNode rawEvidence() {
        ObjectNode root = evidence(false);
        root.remove("providerRead");
        ObjectNode request = (ObjectNode) root.path("consumerRequest");
        request.putObject("currentRepositoryRevisions")
                .put("github.com/example/consumer-cli", REVISION);
        ObjectNode raw = root.putObject("providerReadRaw");
        raw.put("fetchedAt", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
        ObjectNode response = raw.putArray("responses").addObject();
        response.put("has_more", false);
        response.putArray("issues");
        response.put("limit", 50);
        response.put("offset", 0);
        response.put("total", 0);
        return root;
    }

    private static ObjectNode typedEntryFor(String recordKey) throws Exception {
        ObjectNode entry = (ObjectNode) evidence(true).path("providerRead").path("entries").get(0).deepCopy();
        entry.put("recordKey", recordKey);
        ObjectNode proposal = (ObjectNode) entry.path("proposal");
        proposal.put("proposalRef", recordKey);
        WorkspaceKnowledgeProposal typedProposal = JSON.treeToValue(proposal, WorkspaceKnowledgeProposal.class);
        String digest = SwarmKnowledgeGateway.proposalDigest(typedProposal);
        ObjectNode governance = (ObjectNode) entry.path("governance");
        governance.set("proposal", proposal.deepCopy());
        entry.put("proposalDigest", digest);
        entry.put("decisionRecordKey", recordKey);
        entry.put("decisionProposalDigest", digest);
        return entry;
    }

    private static ObjectNode rawIssue(
            ObjectNode typedEntry,
            String issueId,
            int providerRevision,
            List<String> sourceConflicts,
            boolean stringRepositoryRevisions,
            boolean unsupportedKnowledgeType,
            List<String> recordLimitations) throws Exception {
        ObjectNode proposal = ((ObjectNode) typedEntry.path("proposal")).deepCopy();
        if (unsupportedKnowledgeType) proposal.put("knowledgeType", "operational_experience_lessons_learned");
        ObjectNode typedGovernance = (ObjectNode) typedEntry.path("governance");
        ObjectNode body = JSON.createObjectNode()
                .put("recordKind", "RC10_WORKSPACE_KNOWLEDGE")
                .put("schemaVersion", "0.1")
                .put("recordKey", typedEntry.path("recordKey").asText())
                .put("recordVersion", typedEntry.path("recordVersion").asInt())
                .put("workspaceRef", WORKSPACE)
                .put("projectRef", KNOWLEDGE_PROJECT);
        body.set("proposal", proposal);
        ObjectNode learningSource = JSON.createObjectNode()
                .put("learningSourceRef", "fixture:learning-source")
                .put("workspaceRef", WORKSPACE)
                .put("missionRef", MISSION)
                .put("closureSummaryRef", "fixture:closure");
        learningSource.set("subjectRefs", JSON.createArrayNode().add("fixture:subject"));
        learningSource.set("sourceRefs", JSON.createArrayNode().add("fixture:source"));
        learningSource.set("evidenceRefs", JSON.createArrayNode().add("fixture:evidence"));
        body.set("learningSource", learningSource);
        body.set("applicableProjectRefs", JSON.createArrayNode().add(CONSUMER_PROJECT));
        body.put("visibility", "WORKSPACE_AUTHORIZED");
        ObjectNode rawGovernance = JSON.createObjectNode()
                .put("decision", "APPROVED")
                .put("decisionRef", typedGovernance.path("decisionRef").asText())
                .put("actorRef", typedGovernance.path("decidedBy").asText())
                .put("decidedAt", typedGovernance.path("decidedAt").asText())
                .put("policyRef", typedGovernance.path("policyRef").asText());
        rawGovernance.set("evidenceRefs", typedGovernance.path("decisionEvidenceRefs").deepCopy());
        rawGovernance.put("recordKey", typedEntry.path("recordKey").asText())
                .put("recordVersion", typedEntry.path("recordVersion").asInt())
                .put("proposalDigest", typedEntry.path("proposalDigest").asText());
        body.set("governance", rawGovernance);
        body.set("lifecycle", JSON.createObjectNode().put("state", "CURRENT"));
        ObjectNode freshness = body.putObject("freshness");
        freshness.put("observedAt", typedEntry.path("observedAt").asText());
        freshness.put("validUntil", typedEntry.path("validUntil").asText());
        ArrayNode revisions = freshness.putArray("repositoryRevisions");
        if (stringRepositoryRevisions) {
            revisions.add("github.com/example/consumer-cli@" + REVISION);
        } else {
            revisions.addObject()
                    .put("repository", "https://github.com/example/consumer-cli.git")
                    .put("revision", REVISION);
        }
        body.set("limitations", JSON.valueToTree(recordLimitations));
        body.set("conflictRefs", JSON.valueToTree(sourceConflicts));

        ObjectNode issue = JSON.createObjectNode()
                .put("id", issueId)
                .put("identifier", "RC10VAL-" + providerRevision)
                .put("workspace_id", WORKSPACE)
                .put("project_id", KNOWLEDGE_PROJECT)
                .put("revision", providerRevision)
                .put("title", "RC10 workspace knowledge fixture")
                .put("description", "Authoritative record:\n```json\n" + JSON.writeValueAsString(body) + "\n```\n");
        issue.set("metadata", JSON.createObjectNode()
                .put("rc10RecordKind", "RC10_WORKSPACE_KNOWLEDGE")
                .put("rc10RecordKey", typedEntry.path("recordKey").asText())
                .put("rc10RecordVersion", typedEntry.path("recordVersion").asInt())
                .put("rc10Decision", "APPROVED")
                .put("rc10Lifecycle", "CURRENT"));
        return issue;
    }

    private static void appendRawIssue(ObjectNode evidence, ObjectNode issue) {
        ObjectNode response = (ObjectNode) evidence.path("providerReadRaw").path("responses").get(0);
        ((ArrayNode) response.path("issues")).add(issue);
        response.put("total", response.path("issues").size());
    }

    private static String sha256(String value) throws Exception {
        return java.util.HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}

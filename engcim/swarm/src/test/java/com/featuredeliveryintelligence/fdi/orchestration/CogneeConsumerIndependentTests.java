package com.featuredeliveryintelligence.fdi.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Independent public-API coverage; all HTTP responses are local synthetic fixtures. */
class CogneeConsumerIndependentTests {
    private static final String DATASET_ID = "00000000-0000-7000-8000-000000000007";
    private static final String DOCUMENT_ID = "00000000-0000-7000-8000-00000000001f";
    private static final String RECORD_KEY = "RC10-SAFE-ID-0025";
    private static final String SOURCE_REF = "classpath:rc10/phase2/methods/c5-run-attribution-v2.md";
    private static final String SOURCE_SHA = "f7aafb4412280cc706dec7f9314a84e7d92f84d51d8c5ded9c264bf97be532e2";
    private static final String SOURCE_TEXT = fixtureSource();
    // Boundaries and metadata shape from the saved, sanitized r1 provider response.
    private static final String FIRST_CHUNK = SOURCE_TEXT.substring(0, 1149);
    private static final String SECOND_CHUNK = SOURCE_TEXT.substring(1149);
    private static final String WORKSPACE = "fixture-workspace-a";
    private static final String KNOWLEDGE_PROJECT = "fixture-wk-a";
    private static final String CONSUMER_PROJECT = "fixture-chart-viewer";
    private static final Instant NOW = Instant.parse("2026-10-02T06:00:00Z");
    private static final Map<String, String> REVISIONS = Map.of("RC10-SAFE-ID-0008", "RC10-SAFE-ID-0008-rev-1");
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void reconstructsActualJsonStringMetadataFromOutOfOrderChunksWithoutUsingNodeVersion() throws Exception {
        assertThat(sha256(SOURCE_TEXT)).isEqualTo(SOURCE_SHA);
        try (var stub = new SearchStub()) {
            stub.reply(200, response(chunk(1, SECOND_CHUNK, metadata()), chunk(0, FIRST_CHUNK, metadata())));

            var candidates = client(stub).searchCandidates(DATASET_ID, "C5 run attribution v2", 5);

            assertThat(candidates).singleElement().satisfies(candidate -> {
                assertThat(candidate.datasetId()).isEqualTo(DATASET_ID);
                assertThat(candidate.documentId()).isEqualTo(DOCUMENT_ID);
                assertThat(candidate.sourceId()).isEqualTo(RECORD_KEY);
                assertThat(candidate.sourceRevision()).isEqualTo(2);
                assertThat(candidate.providerRevision()).isEqualTo("fixture-provider-rev-1");
                assertThat(candidate.sourceRef()).isEqualTo(SOURCE_REF);
                assertThat(candidate.workspaceRef()).isEqualTo("fixture-workspace-a");
                assertThat(candidate.projectRef()).isEqualTo("fixture-wk-a");
                assertThat(candidate.contentSha256()).isEqualTo(SOURCE_SHA);
                assertThat(candidate.chunkCount()).isEqualTo(2);
            });
            assertThat(stub.request.get().path("dataset_ids")).hasSize(1);
            assertThat(stub.request.get().path("dataset_ids").get(0).asText()).isEqualTo(DATASET_ID);
            assertThat(stub.request.get().path("search_type").asText()).isEqualTo("CHUNKS");
        }
    }

    @Test
    void rejectsMissingOrNonIntegralSourceRevisionDespiteAUsableNodeVersion() throws Exception {
        var missing = metadata();
        missing.remove("sourceRevision");
        for (var external : List.of(missing, metadata().put("sourceRevision", "2"),
                metadata().put("sourceRevision", 2.5))) {
            try (var stub = new SearchStub()) {
                var node = chunk(0, SOURCE_TEXT, external).put("version", 2);
                stub.reply(200, response(node));

                assertNoVerifiedCandidate(stub, 5);
            }
        }
    }

    @Test
    void rejectsConflictingSourceMetadataWithinOneDocument() throws Exception {
        for (var conflicting : List.of(metadata().put("projectRef", "fixture-workspace-knowledge"),
                metadata().put("sourceId", "fixture:method:another-source"),
                metadata().put("sourceRevision", 3))) {
            try (var stub = new SearchStub()) {
                stub.reply(200, response(chunk(0, FIRST_CHUNK, metadata()), chunk(1, SECOND_CHUNK, conflicting)));

                assertNoVerifiedCandidate(stub, 5);
            }
        }
    }

    @Test
    void rejectsDuplicateAndGappedIndexesAndPartialTopKInsteadOfInventingCompleteSources() throws Exception {
        try (var stub = new SearchStub()) {
            stub.reply(200, response(chunk(0, FIRST_CHUNK, metadata()), chunk(0, SECOND_CHUNK, metadata())));
            assertNoVerifiedCandidate(stub, 5);

            stub.reply(200, response(chunk(0, FIRST_CHUNK, metadata()), chunk(2, SECOND_CHUNK, metadata())));
            assertNoVerifiedCandidate(stub, 5);

            // The server answered 200, but top-k yielded only a prefix of this source.
            stub.reply(200, response(chunk(0, FIRST_CHUNK, metadata())));
            assertNoVerifiedCandidate(stub, 1);
        }
    }

    @Test
    void rejectsWellFormedButIncorrectWholeSourceDigest() throws Exception {
        try (var stub = new SearchStub()) {
            var wrongDigest = metadata().put("contentSha256", "0".repeat(64));
            stub.reply(200, response(chunk(1, SECOND_CHUNK, wrongDigest), chunk(0, FIRST_CHUNK, wrongDigest)));

            assertNoVerifiedCandidate(stub, 5);
        }
    }

    @Test
    void returnsEmptyForSuccessfulEmptySearchButDoesNotHideProviderUnavailability() throws Exception {
        try (var stub = new SearchStub()) {
            stub.reply(200, response());
            assertThat(client(stub).searchCandidates(DATASET_ID, "C5", 5)).isEmpty();
            stub.reply(200, mapper.createArrayNode());
            assertThat(client(stub).searchCandidates(DATASET_ID, "C5", 5)).isEmpty();

            stub.reply(503, mapper.createArrayNode());
            assertThatThrownBy(() -> client(stub).searchCandidates(DATASET_ID, "C5", 5))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("HTTP 503");
        }
    }

    @Test
    void cogneeNarrowingCannotHideDuplicateRecordKeysFromTheOriginalGovernedRead() {
        var matching = entry(2, "fixture-provider-rev-1", WORKSPACE, KnowledgeGovernanceDecision.APPROVED,
                NOW.plusSeconds(3600), REVISIONS);
        var otherProvider = entry(2, "fixture-provider-rev-other", WORKSPACE, KnowledgeGovernanceDecision.APPROVED,
                NOW.plusSeconds(3600), REVISIONS);
        var read = read(KNOWLEDGE_PROJECT, matching, otherProvider);
        var gateway = knowledgeGateway();
        var candidate = new CogneeSearchClient.CandidateDocument(DATASET_ID, DOCUMENT_ID, RECORD_KEY, 2,
                "fixture-provider-rev-1", SOURCE_REF, WORKSPACE, KNOWLEDGE_PROJECT, SOURCE_SHA, 2);

        var original = gateway.retrieveForConsumer(consumerRequest(), read);
        assertThat(original.selected()).isEmpty();
        assertThat(original.excluded()).hasSize(2).allSatisfy(exclusion ->
                assertThat(exclusion.reason()).isEqualTo(SwarmKnowledgeGateway.ContextExclusionReason.DUPLICATE_RECORD_KEY));

        var narrowed = gateway.retrieveForConsumer(consumerRequest(), read, DATASET_ID, List.of(candidate));

        assertThat(narrowed.selected()).isEmpty();
        assertThat(narrowed.excluded()).containsExactlyInAnyOrderElementsOf(original.excluded());
        assertThat(narrowed.mayProceedWithoutKnowledge()).isFalse();
    }

    @Test
    void passesOnlyTheExactSameScopeCurrentApprovedSyntheticEntryToTheMissionEnvelope() throws Exception {
        var matching = entry(2, "fixture-provider-rev-1", WORKSPACE, KnowledgeGovernanceDecision.APPROVED,
                NOW.plusSeconds(3600), REVISIONS);
        assertThat(matching.knowledgeRef()).isNotEqualTo(RECORD_KEY);
        assertThat(matching.proposalDigest()).isNotEqualTo(SOURCE_SHA);
        try (var stub = new SearchStub()) {
            stub.reply(200, response(chunk(1, SECOND_CHUNK, metadata()), chunk(0, FIRST_CHUNK, metadata())));

            var envelope = executeEnvelope(read(KNOWLEDGE_PROJECT, matching), stub);

            assertThat(envelope.eligibleKnowledge()).containsExactly(matching);
            assertThat(envelope.knowledgeContextStatus()).isEqualTo(MissionExecutionEnvelope.KnowledgeContextStatus.AVAILABLE);
            assertThat(envelope.currentRepositoryRevisions()).isEqualTo(REVISIONS);
            assertThat(envelope.missionRef()).isEqualTo("fixture:mission:cognee-independent");
        }
    }

    @Test
    void withdrawalDuringDerivedSearchBlocksHandoffAfterAuthoritativeReadback() throws Exception {
        var approved = entry(2, "fixture-provider-rev-1", WORKSPACE, KnowledgeGovernanceDecision.APPROVED,
                NOW.plusSeconds(3600), REVISIONS);
        var withdrawn = entry(2, "fixture-provider-rev-1", WORKSPACE, KnowledgeGovernanceDecision.DEFERRED,
                NOW.plusSeconds(3600), REVISIONS);
        var reads = new AtomicInteger();
        try (var stub = new SearchStub()) {
            stub.reply(200, response(chunk(0, SOURCE_TEXT, metadata())));
            executeEnvelope(() -> read(KNOWLEDGE_PROJECT,
                    reads.getAndIncrement() == 0 ? approved : withdrawn), stub, "NO_ELIGIBLE_RECORDS");
            assertThat(reads.get()).isEqualTo(2);
        }
    }

    @Test
    void excludesIneligibleOrNonmatchingEntriesAndBlocksUnverifiedOptionalDispatch() throws Exception {
        var approved = entry(2, "fixture-provider-rev-1", WORKSPACE, KnowledgeGovernanceDecision.APPROVED,
                NOW.plusSeconds(3600), REVISIONS);
        var variants = List.of(
                new ReadVariant("stale", read(KNOWLEDGE_PROJECT,
                        entry(2, "fixture-provider-rev-1", WORKSPACE, KnowledgeGovernanceDecision.APPROVED,
                                NOW.minusSeconds(1), REVISIONS))),
                new ReadVariant("DEFERRED", read(KNOWLEDGE_PROJECT,
                        entry(2, "fixture-provider-rev-1", WORKSPACE, KnowledgeGovernanceDecision.DEFERRED,
                                NOW.plusSeconds(3600), REVISIONS))),
                new ReadVariant("knowledge-project mismatch must not be aliased",
                        read("fixture-workspace-knowledge", approved)),
                new ReadVariant("wrong workspace", read(KNOWLEDGE_PROJECT,
                        entry(2, "fixture-provider-rev-1", "fixture-workspace-other", KnowledgeGovernanceDecision.APPROVED,
                                NOW.plusSeconds(3600), REVISIONS))),
                new ReadVariant("repository revision mismatch", read(KNOWLEDGE_PROJECT,
                        entry(2, "fixture-provider-rev-1", WORKSPACE, KnowledgeGovernanceDecision.APPROVED,
                                NOW.plusSeconds(3600), Map.of("RC10-SAFE-ID-0008", "RC10-SAFE-ID-0008-rev-old")))),
                new ReadVariant("external source revision differs from governed record version", read(KNOWLEDGE_PROJECT,
                        entry(3, "fixture-provider-rev-1", WORKSPACE, KnowledgeGovernanceDecision.APPROVED,
                                NOW.plusSeconds(3600), REVISIONS))));
        for (var variant : variants) {
            try (var stub = new SearchStub()) {
                stub.reply(200, response(chunk(0, FIRST_CHUNK, metadata()), chunk(1, SECOND_CHUNK, metadata())));

                executeEnvelope(variant.read(), stub, "NO_ELIGIBLE_RECORDS");
            }
        }
        try (var stub = new SearchStub()) {
            stub.reply(200, response(chunk(0, FIRST_CHUNK, metadata())));
            executeEnvelope(read(KNOWLEDGE_PROJECT, approved), stub, "NO_ELIGIBLE_RECORDS");

            stub.reply(503, mapper.createArrayNode());
            executeEnvelope(read(KNOWLEDGE_PROJECT, approved), stub, "PROVIDER_UNAVAILABLE");
        }
    }

    private SwarmKnowledgeGateway knowledgeGateway() {
        return new SwarmKnowledgeGateway(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private SwarmKnowledgeGateway.ConsumerRequest consumerRequest() {
        return new SwarmKnowledgeGateway.ConsumerRequest("fixture:mission:cognee-independent", WORKSPACE,
                KNOWLEDGE_PROJECT, CONSUMER_PROJECT, REVISIONS);
    }

    private WorkspaceKnowledgeRepository.ReadResult read(String project, WorkspaceKnowledgeRepository.Entry... entries) {
        return new WorkspaceKnowledgeRepository.ReadResult(WORKSPACE, project, NOW, List.of(entries));
    }

    private WorkspaceKnowledgeRepository.Entry entry(int version, String providerRevision, String workspace,
            KnowledgeGovernanceDecision decision, Instant validUntil, Map<String, String> revisions) {
        var proposal = new WorkspaceKnowledgeProposal(RECORD_KEY, workspace, List.of(SOURCE_REF), KnowledgeType.PROCEDURAL,
                SOURCE_TEXT, "Synthetic consumer integration only", "Explicit source identity match; no semantic use claim",
                List.of("Test-only; no live knowledge or publication authority"), List.of("fixture:evidence:source"), List.of());
        var governance = new GovernedWorkspaceKnowledge(proposal, decision, "fixture:decision:c5:v" + version,
                "fixture:test-only-actor", "fixture:test-only-policy", NOW.minusSeconds(120).toString(),
                List.of("fixture:evidence:synthetic-decision"));
        String digest = SwarmKnowledgeGateway.proposalDigest(proposal);
        return new WorkspaceKnowledgeRepository.Entry("fixture:knowledge:c5-run-attribution:v" + version,
                providerRevision, RECORD_KEY, version, proposal, governance, digest, RECORD_KEY, version, digest,
                "CURRENT", NOW.minusSeconds(60), validUntil, revisions, List.of(CONSUMER_PROJECT), "WORKSPACE_AUTHORIZED");
    }

    private MissionExecutionEnvelope executeEnvelope(WorkspaceKnowledgeRepository.ReadResult read, SearchStub stub) {
        return executeEnvelope(read, stub, null);
    }

    private MissionExecutionEnvelope executeEnvelope(WorkspaceKnowledgeRepository.ReadResult read, SearchStub stub,
            String blockedStatus) {
        return executeEnvelope(() -> read, stub, blockedStatus);
    }

    private MissionExecutionEnvelope executeEnvelope(
            java.util.function.Supplier<WorkspaceKnowledgeRepository.ReadResult> reads, SearchStub stub,
            String blockedStatus) {
        var captured = new AtomicReference<MissionExecutionEnvelope>();
        var dispatches = new AtomicInteger();
        WorkspaceKnowledgeRepository repository = new WorkspaceKnowledgeRepository() {
            @Override
            public void save(GovernedWorkspaceKnowledge knowledge) {
                throw new AssertionError("Independent consumer test must not write knowledge");
            }

            @Override
            public ReadResult findByWorkspace(String workspaceRef) {
                assertThat(workspaceRef).isEqualTo(WORKSPACE);
                return reads.get();
            }
        };
        RuntimeBindingPort runtime = envelope -> {
            captured.set(envelope);
            dispatches.incrementAndGet();
            return new BindingReceipt("fixture:runtime:loopback", "fixture:execution:local", "fixture:revision:r1",
                    "STARTED", List.of("fixture:evidence:captured-envelope"));
        };
        var mission = new Mission("fixture:mission:cognee-independent",
                new MissionRequest("fixture:request:cognee-independent", WORKSPACE, CONSUMER_PROJECT,
                        "Synthetic S05 consumer context", "C5 run attribution v2",
                        List.of("S05 only; no live writes"), List.of("Preserve exact governed eligibility"),
                        "fixture:revision:r1", REVISIONS), MissionState.READY);
        var gateway = new SwarmMissionGateway(runtime, knowledgeGateway(), repository,
                workspace -> new WorkspaceKnowledgeProjectRef(workspace, KNOWLEDGE_PROJECT, "WorkspaceKnowledge"),
                client(stub), DATASET_ID);
        if (blockedStatus != null) {
            assertThatThrownBy(() -> gateway.execute(mission))
                    .isInstanceOf(com.featuredeliveryintelligence.fdi.shared.RuntimeContractException.class)
                    .hasMessageContaining(blockedStatus).hasMessageContaining("optional-context policy");
            assertThat(dispatches.get()).isZero();
            assertThat(captured.get()).isNull();
            return null;
        }
        gateway.execute(mission);
        assertThat(dispatches.get()).isEqualTo(1);
        assertThat(captured.get()).isNotNull();
        return captured.get();
    }

    private record ReadVariant(String name, WorkspaceKnowledgeRepository.ReadResult read) {}

    private CogneeSearchClient client(SearchStub stub) {
        return new CogneeSearchClient(stub.uri());
    }

    private void assertNoVerifiedCandidate(SearchStub stub, int topK) {
        // Rejection may be an empty narrowing result or a source-contract exception.
        // Do not prescribe an exception subclass or mislabel partial CHUNKS as HTTP outage.
        try {
            assertThat(client(stub).searchCandidates(DATASET_ID, "C5", topK)).isEmpty();
        } catch (IllegalStateException rejectedSource) {
            assertThat(rejectedSource).hasMessageNotContaining("HTTP")
                    .hasMessageNotContaining("request failed")
                    .hasMessageNotContaining("timed out");
        }
    }

    private ObjectNode metadata() {
        var metadata = mapper.createObjectNode()
                .put("sourceId", RECORD_KEY)
                .put("sourceRevision", 2)
                .put("providerRevision", "fixture-provider-rev-1")
                .put("workspaceRef", "fixture-workspace-a")
                .put("projectRef", "fixture-wk-a")
                .put("sourceRef", SOURCE_REF)
                .put("contentSha256", SOURCE_SHA)
                .put("eligibleInFixture", true)
                .put("authorityBoundary", "test-fixture-only-not-governed-knowledge");
        metadata.putObject("_cognee").put("source_uri", "file:///app/c5-run-attribution-v2.md");
        return metadata;
    }

    private ObjectNode chunk(int index, String text, ObjectNode metadata) {
        return mapper.createObjectNode()
                .put("document_id", DOCUMENT_ID)
                .put("document_name", "c5-run-attribution-v2")
                .put("chunk_index", index)
                .put("type", "IndexSchema")
                .put("version", 1)
                .put("text", text)
                .put("external_metadata", metadata.toString());
    }

    private ArrayNode response(ObjectNode... chunks) {
        var hit = mapper.createObjectNode().put("dataset_id", DATASET_ID).put("dataset_name", "synthetic-fixture");
        var searchResult = hit.putArray("search_result");
        for (var chunk : chunks) searchResult.add(chunk);
        return mapper.createArrayNode().add(hit);
    }

    private static String fixtureSource() {
        try (InputStream source = CogneeConsumerIndependentTests.class.getClassLoader()
                .getResourceAsStream("rc10/phase2/methods/c5-run-attribution-v2.md")) {
            if (source == null) throw new IllegalStateException("Pinned synthetic source fixture is missing");
            return new String(source.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Pinned synthetic source fixture could not be read", e);
        }
    }

    private static String sha256(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private final class SearchStub implements AutoCloseable {
        private final AtomicReference<JsonNode> request = new AtomicReference<>();
        private final AtomicReference<String> body = new AtomicReference<>("[]");
        private final AtomicInteger status = new AtomicInteger(200);
        private final HttpServer server;

        private SearchStub() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/search", exchange -> {
                try {
                    request.set(mapper.readTree(exchange.getRequestBody()));
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

        private void reply(int status, JsonNode response) {
            this.status.set(status);
            body.set(response.toString());
        }

        private URI uri() {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}

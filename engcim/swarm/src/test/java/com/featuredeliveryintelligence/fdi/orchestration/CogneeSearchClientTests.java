package com.featuredeliveryintelligence.fdi.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.featuredeliveryintelligence.fdi.application.FdiApplication;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CogneeSearchClientTests {
    private static final String DATASET_ID = "00000000-0000-7000-8000-000000000001";
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void acceptsApprovedSupplementLimit() throws Exception {
        AtomicReference<JsonNode> sent = new AtomicReference<>();
        startServer(exchange -> {
            sent.set(mapper.readTree(exchange.getRequestBody()));
            respond(exchange, 200, "[]");
        });
        assertThat(new CogneeSearchClient(baseUri()).search(DATASET_ID, "bounded supplement", 15).isArray()).isTrue();
        assertThat(sent.get().path("top_k").asInt()).isEqualTo(15);
    }

    @Test
    void rejectsSearchAboveApprovedSupplementLimitBeforeNetworkIo() {
        var client = new CogneeSearchClient(URI.create("http://127.0.0.1:1"));
        assertThatThrownBy(() -> client.search(DATASET_ID, "bounded knowledge lookup", 16))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("15");
    }

    @Test
    void searchesOnlyThePinnedDatasetUsingRawChunkSearch() throws Exception {
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();
        startServer(exchange -> {
            assertThat(exchange.getRequestURI().getPath()).isEqualTo("/api/v1/search");
            requestBody.set(mapper.readTree(exchange.getRequestBody()));
            respond(exchange, 200, "[{\"search_result\":\"recordKey=fixture:method:one\",\"dataset_id\":\""
                    + DATASET_ID + "\",\"dataset_name\":\"fixture\"}]");
        });

        JsonNode response = new CogneeSearchClient(baseUri()).search(DATASET_ID, "report locator is missing", 5);

        assertThat(requestBody.get().path("dataset_ids").get(0).asText()).isEqualTo(DATASET_ID);
        assertThat(requestBody.get().path("search_type").asText()).isEqualTo("CHUNKS");
        assertThat(requestBody.get().path("query").asText()).isEqualTo("report locator is missing");
        assertThat(requestBody.get().path("top_k").asInt()).isEqualTo(5);
        assertThat(response.get(0).path("search_result").asText()).contains("fixture:method:one");
    }

    @Test
    void assemblesVersionedSourceCandidateFromOrderedCogneeChunks() throws Exception {
        String content = "# C5 method\n\nUse source task identity only after resolving the exact publishing run.\n";
        String contentSha256 = sha256(content);
        String response = candidateResponse(content, 2, contentSha256, 1);
        startServer(exchange -> respond(exchange, 200, response));

        List<CogneeSearchClient.CandidateDocument> candidates =
                new CogneeSearchClient(baseUri()).searchCandidates(DATASET_ID, "C5 publishing run", 5);

        assertThat(candidates).singleElement().satisfies(candidate -> {
            assertThat(candidate.datasetId()).isEqualTo(DATASET_ID);
            assertThat(candidate.documentId()).isEqualTo("fixture-document-1");
            assertThat(candidate.sourceId()).isEqualTo("RC10-SAFE-ID-0025");
            assertThat(candidate.sourceRevision()).isEqualTo(2);
            assertThat(candidate.providerRevision()).isEqualTo("fixture-provider-rev-1");
            assertThat(candidate.sourceRef()).isEqualTo("classpath:rc10/phase2/methods/c5-run-attribution-v2.md");
            assertThat(candidate.workspaceRef()).isEqualTo("fixture-workspace-a");
            assertThat(candidate.projectRef()).isEqualTo("fixture-wk-a");
            assertThat(candidate.contentSha256()).isEqualTo(contentSha256);
            assertThat(candidate.chunkCount()).isEqualTo(2);
        });
    }

    @Test
    void reportsContentDigestMismatchSeparatelyFromAnEmptySearch() throws Exception {
        String content = "# C5 method\n\nThe indexed source must remain content-pinned.\n";
        String response = candidateResponse(content, 2, "a".repeat(64), 1);
        startServer(exchange -> respond(exchange, 200, response));

        CogneeSearchClient.CandidateSearchResult result =
                new CogneeSearchClient(baseUri()).searchCandidateResult(DATASET_ID, "C5", 5);

        assertThat(result.candidates()).isEmpty();
        assertThat(result.rejections()).singleElement()
                .extracting(CogneeSearchClient.CandidateRejection::reason)
                .isEqualTo(CogneeSearchClient.CandidateRejectionReason.CONTENT_DIGEST_MISMATCH_OR_TRUNCATED_RESULT);
    }

    @Test
    void reportsMissingInitialChunkAsIndexGapWithoutClassifyingCogneeAsUnavailable() throws Exception {
        String content = "# C5 method\n\nThe indexed source must remain content-pinned.\n";
        ObjectNode hit = (ObjectNode) mapper.readTree(candidateResponse(content, 2, sha256(content), 1)).get(0);
        ((ArrayNode) hit.get("search_result")).remove(0);
        startServer(exchange -> respond(exchange, 200, mapper.createArrayNode().add(hit).toString()));

        CogneeSearchClient.CandidateSearchResult result =
                new CogneeSearchClient(baseUri()).searchCandidateResult(DATASET_ID, "C5", 5);

        assertThat(result.candidates()).isEmpty();
        assertThat(result.rejections()).singleElement()
                .extracting(CogneeSearchClient.CandidateRejection::reason)
                .isEqualTo(CogneeSearchClient.CandidateRejectionReason.CHUNK_INDEX_GAP);
    }

    @Test
    void reportsContiguousPartialPrefixAsDigestMismatchOrTruncatedResult() throws Exception {
        String content = "# C5 method\n\nThe indexed source must remain content-pinned.\n";
        ObjectNode hit = (ObjectNode) mapper.readTree(candidateResponse(content, 2, sha256(content), 1)).get(0);
        ((ArrayNode) hit.get("search_result")).remove(1);
        startServer(exchange -> respond(exchange, 200, mapper.createArrayNode().add(hit).toString()));

        CogneeSearchClient.CandidateSearchResult result =
                new CogneeSearchClient(baseUri()).searchCandidateResult(DATASET_ID, "C5", 5);

        assertThat(result.candidates()).isEmpty();
        assertThat(result.rejections()).singleElement()
                .extracting(CogneeSearchClient.CandidateRejection::reason)
                .isEqualTo(CogneeSearchClient.CandidateRejectionReason.CONTENT_DIGEST_MISMATCH_OR_TRUNCATED_RESULT);
    }

    @Test
    void sendsBearerAuthenticationWithoutExposingTheCredentialInTheResult() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        startServer(exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "[{\"search_result\":[],\"dataset_id\":\"" + DATASET_ID + "\"}]");
        });

        JsonNode response = new CogneeSearchClient(baseUri(), "test-01")
                .search(DATASET_ID, "query", 1);

        assertThat(authorization.get()).isEqualTo("Bearer test-01");
        assertThat(response.toString()).doesNotContain("test-01");
    }

    @Test
    void doesNotReflectBearerCredentialsFromProviderErrors() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        startServer(exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 401, "test-01 rejected");
        });

        assertThatThrownBy(() -> new CogneeSearchClient(baseUri(), "test-01")
                .search(DATASET_ID, "query", 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP 401")
                .hasMessageNotContaining("test-01");
        assertThat(authorization.get()).isEqualTo("Bearer test-01");
    }

    @Test
    void rejectsResultsThatDoNotEchoTheRequestedDataset() throws Exception {
        startServer(exchange -> respond(exchange, 200,
                "[{\"search_result\":\"foreign\",\"dataset_id\":\"00000000-0000-7000-8000-000000000004\"}]"));

        assertThatThrownBy(() -> new CogneeSearchClient(baseUri()).search(DATASET_ID, "query", 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dataset scope");
    }

    @Test
    void capsResponseBytesForTheLocalValidationCommand() throws Exception {
        String oversizedBody = "x".repeat(2 * 1024 * 1024 + 1);
        startServer(exchange -> respond(exchange, 413, oversizedBody));

        assertThatThrownBy(() -> new CogneeSearchClient(baseUri()).search(DATASET_ID, "query", 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP 413")
                .hasMessageContaining("2 MiB");
    }

    @Test
    void preservesProviderHttpErrorsInsteadOfReturningAnEmptySearch() throws Exception {
        startServer(exchange -> respond(exchange, 503, "provider unavailable"));

        assertThatThrownBy(() -> new CogneeSearchClient(baseUri()).search(DATASET_ID, "query", 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP 503");
    }

    @Test
    void enforcesRequestTimeoutAcrossResponseBody() throws Exception {
        Duration timeout = Duration.ofMillis(200);
        startServer(exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, 0);
            try (var output = exchange.getResponseBody()) {
                output.write("[".getBytes(StandardCharsets.UTF_8));
                output.flush();
                try {
                    Thread.sleep(500);
                    output.write("]".getBytes(StandardCharsets.UTF_8));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (java.io.IOException ignored) {
                    // The client may close the stream once its response deadline expires.
                }
            }
        });

        Instant startedAt = Instant.now();
        assertThatThrownBy(() -> new CogneeSearchClient(baseUri(), HttpClient.newHttpClient(), mapper, timeout)
                        .search(DATASET_ID, "query", 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("overall timeout");
        assertThat(Duration.between(startedAt, Instant.now()).toMillis()).isLessThan(1_500);
    }

    @Test
    void rejectsResponsesOutsideThePinnedResultShape() throws Exception {
        startServer(exchange -> respond(exchange, 200,
                "[{\"dataset_id\":\"" + DATASET_ID + "\"}]"));

        assertThatThrownBy(() -> new CogneeSearchClient(baseUri()).search(DATASET_ID, "query", 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SearchResult shape");
    }

    @Test
    void refusesNonLocalEndpointsForTheNonProductionSearchCommand() {
        assertThatThrownBy(() -> new CogneeSearchClient(URI.create("https://api.cognee.ai")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("loopback");
    }

    @Test
    void applicationEntryPointRunsTheReadOnlyCandidateSearch(@TempDir Path tempDir) throws Exception {
        startServer(exchange -> respond(exchange, 200,
                "[{\"search_result\":\"recordKey=fixture:method:one\",\"dataset_id\":\""
                        + DATASET_ID + "\",\"dataset_name\":\"fixture\"}]"));
        Path queryFile = tempDir.resolve("query.txt");
        Files.writeString(queryFile, "C5 report missing child locator\n");
        var originalOut = System.out;
        var capturedOut = new ByteArrayOutputStream();

        try (var captured = new java.io.PrintStream(capturedOut, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            FdiApplication.main(new String[] {
                "dev204-cognee-search",
                "--base-url", baseUri().toString(),
                "--dataset-id", DATASET_ID,
                "--query-file", queryFile.toString(),
                "--top-k", "3"
            });
        } finally {
            System.setOut(originalOut);
        }

        JsonNode output = mapper.readTree(capturedOut.toByteArray());
        assertThat(output.path("provider").asText()).isEqualTo("Cognee");
        assertThat(output.path("claimBoundary").asText())
                .isEqualTo("SEMANTIC_CANDIDATES_ONLY_NO_GOVERNANCE_DECISION");
        assertThat(output.path("isolationBoundary").asText())
                .isEqualTo("DEDICATED_SYNTHETIC_INSTANCE_ONLY_NO_TENANT_ISOLATION_CLAIM");
        assertThat(output.path("providerVersion").asText()).isEqualTo("UNVERIFIED_BY_HTTP_CLIENT");
        assertThat(output.path("results").get(0).path("dataset_id").asText()).isEqualTo(DATASET_ID);
    }

    private URI baseUri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    private String candidateResponse(String content, int sourceRevision, String contentSha256, int nodeVersion)
            throws Exception {
        ObjectNode metadata = mapper.createObjectNode()
                .put("sourceId", "RC10-SAFE-ID-0025")
                .put("sourceRevision", sourceRevision)
                .put("providerRevision", "fixture-provider-rev-1")
                .put("workspaceRef", "fixture-workspace-a")
                .put("projectRef", "fixture-wk-a")
                .put("sourceRef", "classpath:rc10/phase2/methods/c5-run-attribution-v2.md")
                .put("contentSha256", contentSha256);
        String serializedMetadata = mapper.writeValueAsString(metadata);
        int splitAt = content.length() / 2;
        ArrayNode chunks = mapper.createArrayNode();
        chunks.add(chunkNode("fixture-node-0", 0, content.substring(0, splitAt), serializedMetadata, nodeVersion));
        chunks.add(chunkNode("fixture-node-1", 1, content.substring(splitAt), serializedMetadata, nodeVersion));
        ObjectNode hit = mapper.createObjectNode()
                .put("dataset_id", DATASET_ID)
                .put("dataset_name", "fixture")
                .set("search_result", chunks);
        ArrayNode results = mapper.createArrayNode().add(hit);
        return mapper.writeValueAsString(results);
    }

    private ObjectNode chunkNode(String id, int chunkIndex, String text, String externalMetadata, int nodeVersion) {
        return mapper.createObjectNode()
                .put("id", id)
                .put("document_id", "fixture-document-1")
                .put("chunk_index", chunkIndex)
                .put("type", "IndexSchema")
                .put("version", nodeVersion)
                .put("text", text)
                .put("external_metadata", externalMetadata);
    }

    private static String sha256(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private void startServer(com.sun.net.httpserver.HttpHandler handler) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler);
        server.start();
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}

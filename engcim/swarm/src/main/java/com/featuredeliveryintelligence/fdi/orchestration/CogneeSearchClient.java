package com.featuredeliveryintelligence.fdi.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/** Read-only Cognee CHUNKS search for isolated non-production validation. */
public final class CogneeSearchClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_TOP_K = 15;
    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;

    private final URI searchUri;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String bearerToken;
    private final Duration requestTimeout;

    public CogneeSearchClient(URI baseUri) {
        this(baseUri, null);
    }

    public CogneeSearchClient(URI baseUri, String bearerToken) {
        this(baseUri,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build(),
                new ObjectMapper(),
                bearerToken,
                REQUEST_TIMEOUT);
    }

    CogneeSearchClient(URI baseUri, HttpClient httpClient, ObjectMapper mapper) {
        this(baseUri, httpClient, mapper, null, REQUEST_TIMEOUT);
    }

    CogneeSearchClient(URI baseUri, HttpClient httpClient, ObjectMapper mapper, Duration requestTimeout) {
        this(baseUri, httpClient, mapper, null, requestTimeout);
    }

    private CogneeSearchClient(
            URI baseUri, HttpClient httpClient, ObjectMapper mapper, String bearerToken, Duration requestTimeout) {
        this.searchUri = searchUri(requireLoopbackHttp(baseUri));
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient is required");
        this.mapper = Objects.requireNonNull(mapper, "mapper is required");
        this.bearerToken = validateBearerToken(bearerToken);
        this.requestTimeout = requirePositiveTimeout(requestTimeout);
    }

    public JsonNode search(String datasetId, String query, int topK) {
        return search(datasetId, query, topK, searchDeadlineNanos());
    }

    long searchDeadlineNanos() {
        return System.nanoTime() + Math.min(requestTimeout.toNanos(), REQUEST_TIMEOUT.toNanos());
    }

    private JsonNode search(String datasetId, String query, int topK, long deadlineNanos) {
        UUID datasetUuid = parseDatasetId(datasetId);
        if (query == null || query.isBlank()) throw new IllegalArgumentException("query is required");
        if (topK < 1 || topK > MAX_TOP_K) {
            throw new IllegalArgumentException("topK must be between 1 and " + MAX_TOP_K);
        }

        try {
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                throw new WorkspaceKnowledgeRepository.ProviderUnavailableException("Cognee search budget exhausted");
            }
            String requestBody = mapper.writeValueAsString(Map.of(
                    "query", query,
                    "search_type", "CHUNKS",
                    "dataset_ids", List.of(datasetUuid.toString()),
                    "top_k", topK));
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(searchUri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json");
            if (bearerToken != null) requestBuilder.header("Authorization", "Bearer " + bearerToken);
            HttpRequest request = requestBuilder
                    .timeout(Duration.ofNanos(Math.min(remainingNanos, requestTimeout.toNanos())))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<byte[]> response = sendBounded(request, deadlineNanos);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new WorkspaceKnowledgeRepository.ProviderUnavailableException(
                        "Cognee search failed with HTTP " + response.statusCode());
            }
            JsonNode results = mapper.readTree(new String(response.body(), StandardCharsets.UTF_8));
            validateDatasetScope(results, datasetUuid);
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cognee search was interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof WorkspaceKnowledgeRepository.ProviderUnavailableException unavailable) throw unavailable;
            if (cause instanceof ResponseLimitException tooLarge) {
                throw new WorkspaceKnowledgeRepository.ProviderUnavailableException(tooLarge.getMessage(), tooLarge);
            }
            if (cause instanceof HttpTimeoutException) {
                throw new WorkspaceKnowledgeRepository.ProviderUnavailableException("Cognee search exceeded overall timeout of "
                        + requestTimeout.toMillis() + " ms", cause);
            }
            throw new WorkspaceKnowledgeRepository.ProviderUnavailableException("Cognee search request failed", cause);
        } catch (IOException e) {
            throw new WorkspaceKnowledgeRepository.ProviderUnavailableException(
                    "Cognee search returned an unreadable response", e);
        }
    }

    /**
     * Returns source-bound documents for consumer narrowing. Cognee's IndexSchema.version is
     * deliberately ignored; sourceRevision is read only from the JSON external_metadata field.
     */
    public List<CandidateDocument> searchCandidates(String datasetId, String query, int topK) {
        return searchCandidateResult(datasetId, query, topK).candidates();
    }

    /** Returns validated candidates and explicit per-document reasons for omitted search results. */
    public CandidateSearchResult searchCandidateResult(String datasetId, String query, int topK) {
        return searchCandidateResult(datasetId, query, topK, searchDeadlineNanos());
    }

    CandidateSearchResult searchCandidateResult(String datasetId, String query, int topK, long deadlineNanos) {
        UUID datasetUuid = parseDatasetId(datasetId);
        JsonNode results = search(datasetUuid.toString(), query, topK, deadlineNanos);
        CandidateSearchResult candidates = candidateDocuments(datasetUuid, results);
        if (deadlineNanos - System.nanoTime() <= 0) {
            throw new WorkspaceKnowledgeRepository.ProviderUnavailableException("Cognee search budget exhausted during validation");
        }
        return candidates;
    }

    private CandidateSearchResult candidateDocuments(UUID datasetId, JsonNode results) {
        if (results == null || !results.isArray()) {
            throw candidateContractFailure("search response must be an array");
        }
        Map<String, List<JsonNode>> chunksByDocument = new LinkedHashMap<>();
        for (JsonNode hit : results) {
            JsonNode searchResult = hit.path("search_result");
            if (!searchResult.isArray()) {
                throw candidateContractFailure("CHUNKS search_result must be an array");
            }
            for (JsonNode chunk : searchResult) {
                if (!chunk.isObject()) throw candidateContractFailure("CHUNKS node must be an object");
                String documentId = requiredText(chunk, "document_id");
                chunksByDocument.computeIfAbsent(documentId, ignored -> new ArrayList<>()).add(chunk);
            }
        }

        var candidates = new ArrayList<CandidateDocument>();
        var rejections = new ArrayList<CandidateRejection>();
        for (Map.Entry<String, List<JsonNode>> document : chunksByDocument.entrySet()) {
            TreeMap<Integer, String> orderedText = new TreeMap<>();
            ExternalMetadata expectedMetadata = null;
            CandidateRejectionReason rejection = null;
            for (JsonNode chunk : document.getValue()) {
                if (!"IndexSchema".equals(requiredText(chunk, "type"))) {
                    throw candidateContractFailure("CHUNKS node type did not match the pinned IndexSchema shape");
                }
                JsonNode chunkIndexNode = chunk.get("chunk_index");
                if (chunkIndexNode == null || !chunkIndexNode.isIntegralNumber()
                        || !chunkIndexNode.canConvertToInt() || chunkIndexNode.asInt() < 0) {
                    throw candidateContractFailure("chunk_index must be a non-negative integer");
                }
                int chunkIndex = chunkIndexNode.asInt();
                String text = requiredText(chunk, "text");
                if (orderedText.putIfAbsent(chunkIndex, text) != null && rejection == null) {
                    rejection = CandidateRejectionReason.DUPLICATE_CHUNK_INDEX;
                }

                ExternalMetadata metadata = externalMetadata(chunk);
                if (expectedMetadata == null) expectedMetadata = metadata;
                else if (!expectedMetadata.equals(metadata) && rejection == null) {
                    rejection = CandidateRejectionReason.INCONSISTENT_SOURCE_METADATA;
                }
            }
            if (rejection != null) {
                rejections.add(new CandidateRejection(document.getKey(), rejection));
                continue;
            }
            if (expectedMetadata == null || orderedText.isEmpty()) {
                rejections.add(new CandidateRejection(document.getKey(), CandidateRejectionReason.NO_CHUNKS));
                continue;
            }
            StringBuilder content = new StringBuilder();
            boolean contiguous = true;
            for (int index = 0; index < orderedText.size(); index++) {
                String chunk = orderedText.get(index);
                if (chunk == null) {
                    contiguous = false;
                    break;
                }
                content.append(chunk);
            }
            if (!contiguous) {
                rejections.add(new CandidateRejection(
                        document.getKey(), CandidateRejectionReason.CHUNK_INDEX_GAP));
                continue;
            }
            String actualDigest = sha256(content.toString());
            if (!expectedMetadata.contentSha256().equals(actualDigest)) {
                // With the pinned response shape, a contiguous partial prefix and a full document paired with a
                // wrong digest are observationally identical. Keep that uncertainty explicit in the reason.
                rejections.add(new CandidateRejection(
                        document.getKey(), CandidateRejectionReason.CONTENT_DIGEST_MISMATCH_OR_TRUNCATED_RESULT));
                continue;
            }
            candidates.add(new CandidateDocument(
                    datasetId.toString(), document.getKey(), expectedMetadata.sourceId(),
                    expectedMetadata.sourceRevision(), expectedMetadata.providerRevision(),
                    expectedMetadata.sourceRef(), expectedMetadata.workspaceRef(), expectedMetadata.projectRef(),
                    expectedMetadata.contentSha256(), orderedText.size()));
        }
        return new CandidateSearchResult(candidates, rejections);
    }

    private ExternalMetadata externalMetadata(JsonNode chunk) {
        JsonNode rawMetadata = chunk.get("external_metadata");
        if (rawMetadata == null || !rawMetadata.isTextual() || rawMetadata.asText().isBlank()) {
            throw candidateContractFailure("external_metadata must be a JSON string");
        }
        try {
            JsonNode metadata = mapper.readTree(rawMetadata.asText());
            if (metadata == null || !metadata.isObject()) {
                throw candidateContractFailure("external_metadata JSON must be an object");
            }
            JsonNode revision = metadata.get("sourceRevision");
            if (revision == null || !revision.isIntegralNumber() || !revision.canConvertToInt() || revision.asInt() <= 0) {
                throw candidateContractFailure("external sourceRevision must be a positive integer");
            }
            String contentSha256 = requiredText(metadata, "contentSha256");
            if (!contentSha256.matches("[0-9a-f]{64}")) {
                throw candidateContractFailure("external contentSha256 must be a lowercase SHA-256 value");
            }
            return new ExternalMetadata(
                    requiredText(metadata, "sourceId"), revision.asInt(), requiredText(metadata, "providerRevision"),
                    requiredText(metadata, "sourceRef"), requiredText(metadata, "workspaceRef"),
                    requiredText(metadata, "projectRef"), contentSha256);
        } catch (IOException e) {
            throw candidateContractFailure("external_metadata was not valid JSON");
        }
    }

    private static String requiredText(JsonNode value, String field) {
        JsonNode node = value.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw candidateContractFailure(field + " is required");
        }
        return node.asText();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static WorkspaceKnowledgeRepository.ProviderUnavailableException candidateContractFailure(String reason) {
        return new WorkspaceKnowledgeRepository.ProviderUnavailableException(
                "Cognee source candidate could not be validated: " + reason);
    }

    public record CandidateDocument(
            String datasetId,
            String documentId,
            String sourceId,
            int sourceRevision,
            String providerRevision,
            String sourceRef,
            String workspaceRef,
            String projectRef,
            String contentSha256,
            int chunkCount) {
        public CandidateDocument {
            datasetId = parseDatasetId(datasetId).toString();
            requireNonBlank(documentId, "documentId");
            requireNonBlank(sourceId, "sourceId");
            if (sourceRevision <= 0) throw new IllegalArgumentException("sourceRevision must be positive");
            requireNonBlank(providerRevision, "providerRevision");
            requireNonBlank(sourceRef, "sourceRef");
            requireNonBlank(workspaceRef, "workspaceRef");
            requireNonBlank(projectRef, "projectRef");
            if (contentSha256 == null || !contentSha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("contentSha256 must be a lowercase SHA-256 value");
            }
            if (chunkCount <= 0) throw new IllegalArgumentException("chunkCount must be positive");
        }

        private static void requireNonBlank(String value, String field) {
            if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        }
    }

    public record CandidateSearchResult(List<CandidateDocument> candidates, List<CandidateRejection> rejections) {
        public CandidateSearchResult {
            candidates = List.copyOf(candidates == null ? List.of() : candidates);
            rejections = List.copyOf(rejections == null ? List.of() : rejections);
        }
    }

    public record CandidateRejection(String documentId, CandidateRejectionReason reason) {
        public CandidateRejection {
            if (documentId == null || documentId.isBlank()) throw new IllegalArgumentException("documentId is required");
            Objects.requireNonNull(reason, "reason is required");
        }
    }

    public enum CandidateRejectionReason {
        DUPLICATE_CHUNK_INDEX,
        INCONSISTENT_SOURCE_METADATA,
        NO_CHUNKS,
        CHUNK_INDEX_GAP,
        CONTENT_DIGEST_MISMATCH_OR_TRUNCATED_RESULT
    }

    private record ExternalMetadata(
            String sourceId,
            int sourceRevision,
            String providerRevision,
            String sourceRef,
            String workspaceRef,
            String projectRef,
            String contentSha256) {}

    private HttpResponse<byte[]> sendBounded(HttpRequest request, long deadlineNanos)
            throws InterruptedException, ExecutionException {
        if (deadlineNanos - System.nanoTime() <= 0) {
            throw new WorkspaceKnowledgeRepository.ProviderUnavailableException("Cognee search budget exhausted before HTTP");
        }
        AtomicReference<LimitedBodySubscriber> subscriberRef = new AtomicReference<>();
        CompletableFuture<HttpResponse<byte[]>> responseFuture = httpClient.sendAsync(request, responseInfo -> {
            LimitedBodySubscriber subscriber = new LimitedBodySubscriber(MAX_RESPONSE_BYTES, responseInfo.statusCode());
            subscriberRef.set(subscriber);
            return subscriber;
        });
        try {
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) throw new TimeoutException("request deadline elapsed before response wait");
            return responseFuture.get(remainingNanos, TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            cancel(responseFuture, subscriberRef.get());
            throw new WorkspaceKnowledgeRepository.ProviderUnavailableException("Cognee search exceeded overall timeout of "
                    + requestTimeout.toMillis() + " ms", e);
        } catch (InterruptedException e) {
            cancel(responseFuture, subscriberRef.get());
            throw e;
        } catch (ExecutionException e) {
            cancel(responseFuture, subscriberRef.get());
            throw e;
        }
    }

    private static void cancel(
            CompletableFuture<HttpResponse<byte[]>> responseFuture, LimitedBodySubscriber subscriber) {
        if (subscriber != null) subscriber.abort();
        responseFuture.cancel(true);
    }

    private static URI requireLoopbackHttp(URI baseUri) {
        if (baseUri == null
                || !"http".equalsIgnoreCase(baseUri.getScheme())
                || baseUri.getHost() == null
                || !("localhost".equalsIgnoreCase(baseUri.getHost())
                        || "127.0.0.1".equals(baseUri.getHost()))
                || baseUri.getUserInfo() != null
                || baseUri.getQuery() != null
                || baseUri.getFragment() != null
                || (baseUri.getPath() != null && !baseUri.getPath().isEmpty() && !"/".equals(baseUri.getPath()))) {
            throw new IllegalArgumentException("Cognee validation endpoint must be an unauthenticated loopback HTTP URL");
        }
        try {
            if (!InetAddress.getByName(baseUri.getHost()).isLoopbackAddress()) {
                throw new IllegalArgumentException("Cognee validation endpoint must resolve to loopback");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cognee validation endpoint must resolve to loopback", e);
        }
        return baseUri;
    }

    private static URI searchUri(URI baseUri) {
        String root = baseUri.toString().replaceAll("/+$", "");
        return URI.create(root + "/api/v1/search");
    }

    private static UUID parseDatasetId(String datasetId) {
        if (datasetId == null || datasetId.isBlank()) throw new IllegalArgumentException("datasetId is required");
        try {
            return UUID.fromString(datasetId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("datasetId must be a Cognee dataset UUID", e);
        }
    }

    private static String validateBearerToken(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) return null;
        if (bearerToken.chars().anyMatch(character -> character <= 32 || character >= 127)) {
            throw new IllegalArgumentException("Cognee bearer token must be a single header-safe value");
        }
        return bearerToken;
    }

    private static Duration requirePositiveTimeout(Duration timeout) {
        Objects.requireNonNull(timeout, "requestTimeout is required");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
        return timeout;
    }

    private static void validateDatasetScope(JsonNode results, UUID expectedDatasetId) {
        if (results == null || !results.isArray()) {
            throw new WorkspaceKnowledgeRepository.ProviderUnavailableException(
                    "Cognee search response did not match the pinned SearchResult shape/envelope");
        }
        for (JsonNode result : results) {
            if (!result.isObject() || !result.has("search_result")) {
                throw new WorkspaceKnowledgeRepository.ProviderUnavailableException(
                        "Cognee search response did not match the pinned SearchResult shape/envelope");
            }
            String actualDatasetId = result.path("dataset_id").asText("");
            if (!expectedDatasetId.toString().equalsIgnoreCase(actualDatasetId)) {
                throw new IllegalStateException("Cognee search response violated the requested dataset scope");
            }
        }
    }

    private static final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final int maxBytes;
        private final int statusCode;
        private final ByteArrayOutputStream received = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> body = new CompletableFuture<>();
        private volatile Flow.Subscription subscription;

        private LimitedBodySubscriber(int maxBytes, int statusCode) {
            this.maxBytes = maxBytes;
            this.statusCode = statusCode;
        }

        @Override
        public CompletionStage<byte[]> getBody() {
            return body;
        }

        @Override
        public void onSubscribe(Flow.Subscription nextSubscription) {
            if (subscription != null || body.isDone()) {
                nextSubscription.cancel();
                return;
            }
            subscription = nextSubscription;
            nextSubscription.request(1);
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            if (body.isDone()) return;
            for (ByteBuffer buffer : buffers) {
                int chunkSize = buffer.remaining();
                if ((long) received.size() + chunkSize > maxBytes) {
                    cancelSubscription();
                    body.completeExceptionally(new ResponseLimitException(
                            "Cognee search HTTP " + statusCode + " response exceeded the 2 MiB validation limit"));
                    return;
                }
                byte[] chunk = new byte[chunkSize];
                buffer.get(chunk);
                received.write(chunk, 0, chunk.length);
            }
            Flow.Subscription currentSubscription = subscription;
            if (currentSubscription != null && !body.isDone()) currentSubscription.request(1);
        }

        @Override
        public void onError(Throwable error) {
            body.completeExceptionally(error);
        }

        @Override
        public void onComplete() {
            body.complete(received.toByteArray());
        }

        private void abort() {
            cancelSubscription();
            body.cancel(true);
        }

        private void cancelSubscription() {
            Flow.Subscription currentSubscription = subscription;
            if (currentSubscription != null) currentSubscription.cancel();
        }
    }

    private static final class ResponseLimitException extends IllegalStateException {
        private ResponseLimitException(String message) {
            super(message);
        }
    }
}

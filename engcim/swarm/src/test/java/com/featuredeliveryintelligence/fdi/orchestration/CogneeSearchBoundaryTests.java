package com.featuredeliveryintelligence.fdi.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CogneeSearchBoundaryTests {
    private static final String DATASET_ID = "00000000-0000-7000-8000-00000000000c";
    private static final String OTHER_DATASET_ID = "00000000-0000-7000-8000-000000000005";
    private static final String TOKEN = "synthetic-token-never-for-a-live-server";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void validEmptyArrayIsAnExplicitEmptySearchResult() throws Exception {
        try (Fixture fixture = Fixture.respond(200, "[]")) {
            JsonNode response = new CogneeSearchClient(fixture.baseUri())
                    .search(DATASET_ID, "synthetic query", 7);

            assertTrue(response.isArray());
            assertEquals(0, response.size());
            RecordedRequest request = fixture.onlyRequest();
            assertEquals("/api/v1/search", request.path());
            assertEquals("POST", request.method());
            JsonNode requestJson = MAPPER.readTree(request.body());
            assertEquals("synthetic query", requestJson.path("query").asText());
            assertEquals("CHUNKS", requestJson.path("search_type").asText());
            assertEquals(DATASET_ID, requestJson.path("dataset_ids").get(0).asText());
            assertEquals(7, requestJson.path("top_k").asInt());
        }
    }

    @Test
    void structuredSearchResultPayloadIsAcceptedWithoutFlattening() throws Exception {
        String json = "[{\"search_result\":{\"text\":\"chunk\",\"score\":0.91,"
                + "\"metadata\":{\"source\":\"fixture\"}},\"dataset_id\":\""
                + DATASET_ID + "\",\"dataset_name\":\"isolated-fixture\"}]";
        try (Fixture fixture = Fixture.respond(200, json)) {
            JsonNode response = new CogneeSearchClient(fixture.baseUri())
                    .search(DATASET_ID, "structured response", 3);

            JsonNode result = response.get(0).path("search_result");
            assertTrue(result.isObject());
            assertEquals("chunk", result.path("text").asText());
            assertEquals(0.91, result.path("score").asDouble(), 0.00001);
            assertEquals("fixture", result.path("metadata").path("source").asText());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 404, 500})
    void nonSuccessStatusNeverBecomesAnApprovedEmptyResult(int status) throws Exception {
        try (Fixture fixture = Fixture.respond(status, "[]")) {
            IllegalStateException error = assertThrows(
                    IllegalStateException.class,
                    () -> new CogneeSearchClient(fixture.baseUri()).search(DATASET_ID, "q", 1));

            assertTrue(error.getMessage().contains("HTTP " + status));
            assertFalse(error.getMessage().contains("[]"));
            assertEquals(1, fixture.requestCount());
        }
    }

    @Test
    void unauthorizedWithoutBearerTokenRemainsAnExplicitFailure() throws Exception {
        try (Fixture fixture = Fixture.respond(401, "{\"detail\":\"authentication required\"}")) {
            IllegalStateException error = assertThrows(
                    IllegalStateException.class,
                    () -> new CogneeSearchClient(fixture.baseUri()).search(DATASET_ID, "q", 1));

            assertTrue(error.getMessage().contains("HTTP 401"));
            assertNull(fixture.onlyRequest().authorization());
        }
    }

    @Test
    void bearerTokenIsSentButNotIncludedInFailureText() throws Exception {
        try (Fixture fixture = Fixture.respond(403, "{\"detail\":\"synthetic rejection\"}")) {
            IllegalStateException error = assertThrows(
                    IllegalStateException.class,
                    () -> new CogneeSearchClient(fixture.baseUri(), TOKEN).search(DATASET_ID, "q", 1));

            assertEquals("Bearer " + TOKEN, fixture.onlyRequest().authorization());
            assertFalse(error.getMessage().contains(TOKEN));
            assertFalse(error.toString().contains(TOKEN));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "{}", "null", "\"not-an-array\""})
    void invalidJsonOrNonArrayEnvelopeIsRejected(String responseBody) throws Exception {
        try (Fixture fixture = Fixture.respond(200, responseBody)) {
            assertThrows(
                    IllegalStateException.class,
                    () -> new CogneeSearchClient(fixture.baseUri()).search(DATASET_ID, "q", 1));
        }
    }

    @Test
    void mixedDatasetEnvelopeIsRejectedAsAWhole() throws Exception {
        String body = "[{\"search_result\":\"first\",\"dataset_id\":\"" + DATASET_ID
                + "\"},{\"search_result\":\"foreign\",\"dataset_id\":\"" + OTHER_DATASET_ID + "\"}]";
        try (Fixture fixture = Fixture.respond(200, body)) {
            assertThrows(
                    IllegalStateException.class,
                    () -> new CogneeSearchClient(fixture.baseUri()).search(DATASET_ID, "q", 2));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "[{\"search_result\":\"missing dataset id\"}]",
        "[{\"search_result\":\"null dataset id\",\"dataset_id\":null}]",
        "[null]"
    })
    void missingNullOrMalformedDatasetEnvelopeIsRejected(String responseBody) throws Exception {
        try (Fixture fixture = Fixture.respond(200, responseBody)) {
            assertThrows(
                    IllegalStateException.class,
                    () -> new CogneeSearchClient(fixture.baseUri()).search(DATASET_ID, "q", 1));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://127.0.0.1",
        "http://192.0.2.10",
        "http://user:password@127.0.0.1",
        "http://127.0.0.1?dataset_id=masquerade",
        "http://127.0.0.1/api/v1/search",
        "http://127.0.0.1.evil"
    })
    void unsafeOrMasqueradingBaseUrlIsRejectedBeforeAnyRequest(String baseUrl) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CogneeSearchClient(URI.create(baseUrl)));
    }

    @Test
    void malformedSearchArgumentsAreRejectedBeforeDispatch() throws Exception {
        try (Fixture fixture = Fixture.respond(200, "[]")) {
            CogneeSearchClient client = new CogneeSearchClient(fixture.baseUri());

            assertThrows(IllegalArgumentException.class, () -> client.search("not-a-uuid", "q", 1));
            assertThrows(IllegalArgumentException.class, () -> client.search(DATASET_ID, "   ", 1));
            assertThrows(IllegalArgumentException.class, () -> client.search(DATASET_ID, "q", 0));
            assertThrows(IllegalArgumentException.class, () -> client.search(DATASET_ID, "q", 51));

            assertEquals(0, fixture.requestCount());
        }
    }

    @Test
    void responseOneByteOverTwoMibIsRejectedWithoutEchoingBody() throws Exception {
        byte[] oversizedBody = new byte[2 * 1024 * 1024 + 1];
        java.util.Arrays.fill(oversizedBody, (byte) 'x');
        try (Fixture fixture = Fixture.respond(200, oversizedBody)) {
            IllegalStateException error = assertThrows(
                    IllegalStateException.class,
                    () -> new CogneeSearchClient(fixture.baseUri()).search(DATASET_ID, "q", 1));

            assertTrue(error.getMessage().contains("2 MiB"));
            assertFalse(error.getMessage().contains("x".repeat(128)));
            assertEquals(1, fixture.requestCount());
        }
    }

    @Test
    void completeBodySucceedsWithinInjectedShortDeadline() throws Exception {
        try (Fixture fixture = Fixture.respond(200, "[]")) {
            JsonNode response = new CogneeSearchClient(
                            fixture.baseUri(),
                            HttpClient.newHttpClient(),
                            MAPPER,
                            Duration.ofSeconds(2))
                    .search(DATASET_ID, "short deadline positive", 1);

            assertTrue(response.isArray());
            assertEquals(0, response.size());
            fixture.onlyRequest();
        }
    }

    @Test
    @Timeout(value = 6, unit = TimeUnit.SECONDS)
    void injectedOverallDeadlineExpiresAfterHeadersAndBodyReadStart() throws Exception {
        CountDownLatch headersSent = new CountDownLatch(1);
        CountDownLatch initialBodyChunkFlushed = new CountDownLatch(1);
        CountDownLatch releaseRemainingBody = new CountDownLatch(1);
        AtomicLong headersSentAt = new AtomicLong();
        AtomicLong initialChunkFlushedAt = new AtomicLong();
        AtomicLong timeoutElapsedMillis = new AtomicLong();
        AtomicReference<RecordedRequest> timeoutRequest = new AtomicReference<>();

        Fixture fixture = Fixture.blockAfterInitialChunk(
                headersSent, initialBodyChunkFlushed, releaseRemainingBody, headersSentAt, initialChunkFlushedAt);
        long startedAt = System.nanoTime();
        boolean callerTerminated;
        try (fixture) {
            ExecutorService caller = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "cognee-boundary-timeout-call");
                thread.setDaemon(true);
                return thread;
            });
            Future<JsonNode> search = caller.submit(() -> new CogneeSearchClient(
                            fixture.baseUri(),
                            HttpClient.newHttpClient(),
                            MAPPER,
                            Duration.ofMillis(1200))
                    .search(DATASET_ID, "short deadline body timeout", 1));
            try {
                assertTrue(headersSent.await(2, TimeUnit.SECONDS), "fixture did not send response headers");
                assertTrue(initialBodyChunkFlushed.await(2, TimeUnit.SECONDS), "fixture did not start the body");
                long headerOffsetMillis = TimeUnit.NANOSECONDS.toMillis(headersSentAt.get() - startedAt);
                long bodyOffsetMillis = TimeUnit.NANOSECONDS.toMillis(initialChunkFlushedAt.get() - startedAt);
                assertTrue(headerOffsetMillis >= 0 && headerOffsetMillis < 900,
                        "headers must arrive well before the injected deadline");
                assertTrue(bodyOffsetMillis >= headerOffsetMillis && bodyOffsetMillis < 900,
                        "body read must start well before the injected deadline");

                ExecutionException timeout = assertThrows(
                        ExecutionException.class,
                        () -> search.get(3, TimeUnit.SECONDS));
                assertTrue(timeout.getCause() instanceof IllegalStateException);
                assertTrue(timeout.getCause().getMessage().contains("overall timeout"));
                long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
                timeoutElapsedMillis.set(elapsedMillis);
                assertTrue(elapsedMillis >= 1000 && elapsedMillis < 2500,
                        "overall timeout should bound the in-progress body read");
                timeoutRequest.set(fixture.onlyRequest());
                assertEquals("POST", timeoutRequest.get().method());
                assertEquals("/api/v1/search", timeoutRequest.get().path());
                assertEquals("short deadline body timeout",
                        MAPPER.readTree(timeoutRequest.get().body()).path("query").asText());
            } finally {
                releaseRemainingBody.countDown();
                caller.shutdownNow();
                callerTerminated = caller.awaitTermination(1, TimeUnit.SECONDS);
                assertTrue(callerTerminated, "search caller did not terminate");
            }
        }
        assertEquals(0, releaseRemainingBody.getCount());
        assertTrue(fixture.isCleanedUp(), "server/executor cleanup did not complete");
        System.out.println("BODY_TIMEOUT_PROBE request=" + timeoutRequest.get().method() + " "
                + timeoutRequest.get().path()
                + " headers_offset_ms=" + TimeUnit.NANOSECONDS.toMillis(headersSentAt.get() - startedAt)
                + " body_prefix_offset_ms=" + TimeUnit.NANOSECONDS.toMillis(initialChunkFlushedAt.get() - startedAt)
                + " outcome=overall-timeout elapsed_ms=" + timeoutElapsedMillis.get()
                + " body_gate_released=true caller_terminated=" + callerTerminated
                + " server_stopped=true fixture_executor_terminated=true");
    }

    private record RecordedRequest(String method, String path, String authorization, String body) {}

    @FunctionalInterface
    private interface ResponseWriter {
        void write(HttpExchange exchange) throws IOException;
    }

    private static final class Fixture implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor;
        private final AtomicInteger requests = new AtomicInteger();
        private final AtomicReference<RecordedRequest> lastRequest = new AtomicReference<>();
        private volatile boolean serverStopped;

        private Fixture(ResponseWriter writer) throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            executor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "cognee-search-boundary-fixture");
                thread.setDaemon(true);
                return thread;
            });
            server.setExecutor(executor);
            server.createContext("/", exchange -> {
                requests.incrementAndGet();
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                lastRequest.set(new RecordedRequest(
                        exchange.getRequestMethod(),
                        exchange.getRequestURI().getRawPath(),
                        exchange.getRequestHeaders().getFirst("Authorization"),
                        requestBody));
                writer.write(exchange);
            });
            server.start();
        }

        static Fixture respond(int status, String responseBody) throws IOException {
            return respond(status, responseBody.getBytes(StandardCharsets.UTF_8));
        }

        static Fixture respond(int status, byte[] responseBody) throws IOException {
            return new Fixture(exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, responseBody.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(responseBody);
                }
            });
        }

        static Fixture blockAfterInitialChunk(
                CountDownLatch headersSent,
                CountDownLatch initialBodyChunkFlushed,
                CountDownLatch releaseRemainingBody,
                AtomicLong headersSentAt,
                AtomicLong initialChunkFlushedAt)
                throws IOException {
            return new Fixture(exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, 0);
                headersSentAt.set(System.nanoTime());
                headersSent.countDown();
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write("[".getBytes(StandardCharsets.UTF_8));
                    output.flush();
                    initialChunkFlushedAt.set(System.nanoTime());
                    initialBodyChunkFlushed.countDown();
                    if (!releaseRemainingBody.await(4, TimeUnit.SECONDS)) {
                        throw new IOException("test did not release the remainder of the response body");
                    }
                    try {
                        output.write("]".getBytes(StandardCharsets.UTF_8));
                        output.flush();
                    } catch (IOException clientCancelledAfterDeadline) {
                        // Expected when the client cancels its bounded body subscription.
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException("body fixture was interrupted during cleanup", interrupted);
                } catch (IOException clientCancelledAfterDeadline) {
                    // Expected when cancellation closes the stream before try-with-resources exits.
                }
            });
        }

        URI baseUri() {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        }

        int requestCount() {
            return requests.get();
        }

        boolean isCleanedUp() {
            return serverStopped && executor.isTerminated();
        }

        RecordedRequest onlyRequest() {
            assertEquals(1, requestCount());
            RecordedRequest request = lastRequest.get();
            assertNotNull(request);
            return request;
        }

        @Override
        public void close() {
            server.stop(0);
            serverStopped = true;
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    throw new AssertionError("fixture executor did not terminate");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError("interrupted while waiting for fixture cleanup", interrupted);
            }
        }
    }
}

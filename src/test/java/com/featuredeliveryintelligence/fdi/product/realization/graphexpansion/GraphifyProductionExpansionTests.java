package com.featuredeliveryintelligence.fdi.product.realization.graphexpansion;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.ComponentIdentity;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.Granularity;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.structural.api.CodeIntelligenceProvider;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GraphifyProductionExpansionTests {
    private static final String REVISION = "8".repeat(40);
    private static final String GRAPH = "a".repeat(64);

    @Test
    void expandsFromProductionSeedAndOrdersAcyclicUniqueTraceDeterministically() {
        RecordingProvider provider = new RecordingProvider();
        provider.response = Map.of(
                "nodes", List.of(
                        node("n3", "src/main/java/pet/Repository.java", "pet.Repository"),
                        node("n1", "src/main/java/pet/Controller.java", "pet.Controller"),
                        node("n2", "src/main/java/pet/Service.java", "pet.Service")),
                "edges", List.of(
                        edge("n2", "n3", "CALLS", "edge-2"),
                        edge("n2", "n1", "CALLS", "cycle"),
                        edge("n1", "n2", "CALLS", "edge-1"),
                        edge("n1", "n2", "CALLS", "edge-1")),
                "paths", List.of());

        GraphifyProductionExpansion.Result result = new GraphifyProductionExpansion(provider).expand(
                new GraphifyProductionExpansion.Request(
                        REVISION, GRAPH, snapshot(),
                        List.of(new GraphifyProductionExpansion.ProductionSeed(
                                "seed-1", identity("src/main/java/pet/Controller.java", "pet.Controller"), "n1")),
                        new GraphifyProductionExpansion.QueryBounds(2, 10, 10, 5, 100_000, 2_000)));

        assertThat(provider.queries).singleElement().satisfies(query -> {
            assertThat(query).containsEntry("operation", "EXPAND")
                    .containsEntry("snapshot_id", "snap-1")
                    .containsEntry("seed_provider_node_id", "n1")
                    .containsEntry("max_depth", 2);
        });
        assertThat(result.traces()).singleElement().satisfies(trace -> {
            assertThat(trace.seed().seedRef()).isEqualTo("seed-1");
            assertThat(trace.directSeedProviderNodeId()).isEqualTo("n1");
            assertThat(trace.inferredNeighbours()).extracting(GraphifyProductionExpansion.InferredNeighbour::providerNodeId)
                    .containsExactly("n2", "n3");
            assertThat(trace.inferredNeighbours().get(0).relationshipTrace().edges())
                    .extracting(edge -> edge.evidenceRef()).containsExactly("edge-1");
            assertThat(trace.inferredNeighbours().get(1).relationshipTrace().edges())
                    .extracting(edge -> edge.evidenceRef()).containsExactly("edge-1", "edge-2");
        });
        assertThat(result.bounds()).isEqualTo(new GraphifyProductionExpansion.QueryBounds(2, 10, 10, 5, 100_000, 2_000));
    }

    @Test
    void missingOrMismatchedBindingsFailBeforeProviderCall() {
        RecordingProvider provider = new RecordingProvider();
        GraphifyProductionExpansion expansion = new GraphifyProductionExpansion(provider);
        var bounds = new GraphifyProductionExpansion.QueryBounds(1, 5, 5, 2, 10_000, 2_000);

        assertThatThrownBy(() -> expansion.expand(new GraphifyProductionExpansion.Request(
                REVISION, GRAPH, snapshot(), List.of(), bounds)))
                .isInstanceOf(RuntimeContractException.class).hasMessageContaining("seed");
        assertThatThrownBy(() -> expansion.expand(new GraphifyProductionExpansion.Request(
                "9".repeat(40), GRAPH, snapshot(), List.of(seed()), bounds)))
                .isInstanceOf(RuntimeContractException.class).hasMessageContaining("revision");
        Map<String, Object> wrongGraph = snapshot();
        wrongGraph.put("graph_sha256", "b".repeat(64));
        assertThatThrownBy(() -> expansion.expand(new GraphifyProductionExpansion.Request(
                REVISION, GRAPH, wrongGraph, List.of(seed()), bounds)))
                .isInstanceOf(RuntimeContractException.class).hasMessageContaining("graph digest");
        assertThat(provider.queries).isEmpty();
    }

    @Test
    void untraceableInferredEdgeFailsClosed() {
        RecordingProvider provider = new RecordingProvider();
        provider.response = Map.of(
                "nodes", List.of(node("n1", "src/main/java/pet/Controller.java", "pet.Controller"),
                        node("n2", "src/main/java/pet/Service.java", "pet.Service"),
                        node("n3", "src/main/java/pet/Other.java", "pet.Other")),
                "edges", List.of(edge("n2", "n3", "CALLS", "orphan")), "paths", List.of());

        assertThatThrownBy(() -> new GraphifyProductionExpansion(provider).expand(request()))
                .isInstanceOf(RuntimeContractException.class).hasMessageContaining("untraceable inferred link");
    }

    @Test
    void providerCannotReturnTestNodeOrMismatchedRevision() {
        RecordingProvider provider = new RecordingProvider();
        Map<String, Object> bad = node("n2", "src/test/java/pet/ServiceTests.java", "pet.ServiceTests");
        provider.response = Map.of("nodes", List.of(node("n1", "src/main/java/pet/Controller.java", "pet.Controller"), bad),
                "edges", List.of(edge("n1", "n2", "CALLS", "edge-1")), "paths", List.of());
        assertThatThrownBy(() -> new GraphifyProductionExpansion(provider).expand(request()))
                .isInstanceOf(RuntimeContractException.class).hasMessageContaining("production");
    }

    @Test
    void enforcesEveryProviderResponseBoundAndEnvelopePostcondition() {
        assertRejected(response(repeatedNodes(3), List.of(), List.of()), bounds(1, 2, 5, 5, 100_000, 2_000), "max_nodes");
        assertRejected(response(List.of(seedNode(), node("n2", "src/main/java/pet/S.java", "pet.S")),
                List.of(edge("n1", "n2", "A", "e1"), edge("n1", "n2", "B", "e2")), List.of()),
                bounds(1, 5, 1, 5, 100_000, 2_000), "max_edges");
        assertRejected(response(List.of(seedNode()), List.of(), List.of("p1", "p2")),
                bounds(1, 5, 5, 1, 100_000, 2_000), "max_paths");
        assertRejected(response(List.of(seedNode(), node("n2", "src/main/java/pet/S.java", "pet.S"),
                        node("n3", "src/main/java/pet/R.java", "pet.R")),
                        List.of(edge("n1", "n2", "A", "e1"), edge("n2", "n3", "B", "e2")), List.of()),
                bounds(1, 5, 5, 5, 100_000, 2_000), "max_depth");
        assertRejected(response(List.of(seedNode()), List.of(), List.of()),
                bounds(1, 5, 5, 5, 32, 2_000), "max_result_bytes");

        RecordingProvider timeout = providerWith(response(List.of(seedNode()), List.of(), List.of()));
        timeout.elapsedMillis = 2_001;
        assertThatThrownBy(() -> new GraphifyProductionExpansion(timeout).expand(request(bounds(1, 5, 5, 5, 100_000, 2_000))))
                .isInstanceOf(RuntimeContractException.class).hasMessageContaining("timeout");
        RecordingProvider truncated = providerWith(response(List.of(seedNode()), List.of(), List.of()));
        truncated.truncated = true;
        assertThatThrownBy(() -> new GraphifyProductionExpansion(truncated).expand(request(bounds(1, 5, 5, 5, 100_000, 2_000))))
                .isInstanceOf(RuntimeContractException.class).hasMessageContaining("truncated");
    }

    @Test
    void deeplyFreezesSnapshotSoExternalMutationCannotChangeBindingIdentity() {
        Map<String, Object> repository = new LinkedHashMap<>(Map.of(
                "repository_id", "petclinic", "canonical_revision", REVISION));
        List<Object> repositories = new ArrayList<>(List.of(repository));
        Map<String, Object> mutable = new LinkedHashMap<>(snapshot());
        mutable.put("repositories", repositories);
        var request = new GraphifyProductionExpansion.Request(REVISION, GRAPH, mutable, List.of(seed()),
                bounds(1, 5, 5, 5, 100_000, 2_000));
        repository.put("canonical_revision", "9".repeat(40));
        repositories.clear();
        mutable.put("graph_sha256", "b".repeat(64));

        RecordingProvider provider = providerWith(response(List.of(seedNode()), List.of(), List.of()));
        new GraphifyProductionExpansion(provider).expand(request);
        assertThat(((Map<?, ?>)((List<?>)request.snapshotRef().get("repositories")).get(0)).get("canonical_revision"))
                .isEqualTo(REVISION);
        assertThat(provider.snapshots.get(0).get("graph_sha256")).isEqualTo(GRAPH);
        assertThatThrownBy(() -> ((List<Object>)request.snapshotRef().get("repositories")).clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsIsolatedReturnedNodeButAllowsASeedWithNoNeighbours() {
        RecordingProvider isolated = providerWith(response(
                List.of(seedNode(), node("n9", "src/main/java/pet/Isolated.java", "pet.Isolated")),
                List.of(), List.of()));
        assertThatThrownBy(() -> new GraphifyProductionExpansion(isolated).expand(request()))
                .isInstanceOf(RuntimeContractException.class).hasMessageContaining("orphan provider node");

        RecordingProvider seedOnly = providerWith(response(List.of(seedNode()), List.of(), List.of()));
        GraphifyProductionExpansion.Result result = new GraphifyProductionExpansion(seedOnly).expand(request());
        assertThat(result.traces()).singleElement().satisfies(trace ->
                assertThat(trace.inferredNeighbours()).isEmpty());
    }

    private static GraphifyProductionExpansion.Request request() {
        return new GraphifyProductionExpansion.Request(REVISION, GRAPH, snapshot(), List.of(seed()),
                bounds(2, 10, 10, 5, 100_000, 2_000));
    }

    private static GraphifyProductionExpansion.Request request(GraphifyProductionExpansion.QueryBounds bounds) {
        return new GraphifyProductionExpansion.Request(REVISION, GRAPH, snapshot(), List.of(seed()), bounds);
    }

    private static GraphifyProductionExpansion.QueryBounds bounds(int depth, int nodes, int edges, int paths,
            int bytes, int timeoutMillis) {
        return new GraphifyProductionExpansion.QueryBounds(depth, nodes, edges, paths, bytes, timeoutMillis);
    }

    private static void assertRejected(Map<String, Object> response, GraphifyProductionExpansion.QueryBounds bounds,
            String message) {
        RecordingProvider provider = providerWith(response);
        assertThatThrownBy(() -> new GraphifyProductionExpansion(provider).expand(request(bounds)))
                .isInstanceOf(RuntimeContractException.class).hasMessageContaining(message);
    }

    private static RecordingProvider providerWith(Map<String, Object> response) {
        RecordingProvider provider = new RecordingProvider(); provider.response = response; return provider;
    }

    private static List<Map<String, Object>> repeatedNodes(int count) {
        List<Map<String, Object>> nodes = new ArrayList<>(); nodes.add(seedNode());
        for (int i = 2; i <= count; i++) nodes.add(node("n" + i, "src/main/java/pet/N" + i + ".java", "pet.N" + i));
        return nodes;
    }

    private static Map<String, Object> seedNode() {
        return node("n1", "src/main/java/pet/Controller.java", "pet.Controller");
    }

    private static Map<String, Object> response(List<?> nodes, List<?> edges, List<?> paths) {
        return Map.of("nodes", nodes, "edges", edges, "paths", paths);
    }

    private static GraphifyProductionExpansion.ProductionSeed seed() {
        return new GraphifyProductionExpansion.ProductionSeed(
                "seed-1", identity("src/main/java/pet/Controller.java", "pet.Controller"), "n1");
    }

    private static ComponentIdentity identity(String path, String symbol) {
        return new ComponentIdentity(REVISION, path, Granularity.TYPE, symbol);
    }

    private static Map<String, Object> snapshot() {
        return new LinkedHashMap<>(Map.of(
                "snapshot_id", "snap-1", "provider_scope_id", "scope-1", "provider_ref", "ref-1",
                "graph_sha256", GRAPH,
                "repositories", List.of(Map.of("repository_id", "petclinic", "canonical_revision", REVISION))));
    }

    private static Map<String, Object> node(String id, String path, String symbol) {
        return Map.of("provider_node_id", id, "source_revision", REVISION, "source_path", path,
                "granularity", "TYPE", "qualified_symbol", symbol);
    }

    private static Map<String, Object> edge(String from, String to, String type, String evidence) {
        return Map.of("from_provider_node_id", from, "to_provider_node_id", to,
                "relationship_type", type, "evidence_ref", evidence);
    }

    private static final class RecordingProvider implements CodeIntelligenceProvider {
        final List<Map<String, Object>> queries = new ArrayList<>();
        final List<Map<String, Object>> snapshots = new ArrayList<>();
        Map<String, Object> response = Map.of("nodes", List.of(), "edges", List.of(), "paths", List.of());
        long elapsedMillis = 1;
        boolean truncated;
        @Override public Map<String, Object> expand(Map<String, Object> query, Map<String, Object> snapshotRef) {
            queries.add(new LinkedHashMap<>(query)); snapshots.add(snapshotRef);
            Map<String, Object> result = new LinkedHashMap<>(response);
            result.put("query_id", query.get("query_id"));
            result.put("snapshot_id", snapshotRef.get("snapshot_id"));
            result.put("source_revision", REVISION);
            result.put("graph_sha256", GRAPH);
            result.put("elapsed_millis", elapsedMillis);
            result.put("timed_out", false);
            result.put("truncated", truncated);
            return result;
        }
        @Override public Map<String, Object> orient(Map<String, Object> a, Map<String, Object> b) { throw new AssertionError(); }
        @Override public Map<String, Object> find(Map<String, Object> a, Map<String, Object> b) { throw new AssertionError(); }
        @Override public Map<String, Object> trace(Map<String, Object> a, Map<String, Object> b) { throw new AssertionError(); }
        @Override public Map<String, Object> diff(Map<String, Object> a, Map<String, Object> b, Map<String, Object> c) { throw new AssertionError(); }
    }
}

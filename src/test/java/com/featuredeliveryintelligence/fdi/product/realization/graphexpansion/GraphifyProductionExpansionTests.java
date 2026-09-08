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
                        edge("n3", "n1", "CALLS", "cycle"),
                        edge("n1", "n2", "CALLS", "edge-1"),
                        edge("n1", "n2", "CALLS", "edge-1")),
                "paths", List.of());

        GraphifyProductionExpansion.Result result = new GraphifyProductionExpansion(provider).expand(
                new GraphifyProductionExpansion.Request(
                        REVISION, GRAPH, snapshot(),
                        List.of(new GraphifyProductionExpansion.ProductionSeed(
                                "seed-1", identity("src/main/java/pet/Controller.java", "pet.Controller"), "n1")),
                        new GraphifyProductionExpansion.QueryBounds(2, 10, 10, 5, 100_000)));

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
        assertThat(result.bounds()).isEqualTo(new GraphifyProductionExpansion.QueryBounds(2, 10, 10, 5, 100_000));
    }

    @Test
    void missingOrMismatchedBindingsFailBeforeProviderCall() {
        RecordingProvider provider = new RecordingProvider();
        GraphifyProductionExpansion expansion = new GraphifyProductionExpansion(provider);
        var bounds = new GraphifyProductionExpansion.QueryBounds(1, 5, 5, 2, 10_000);

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

    private static GraphifyProductionExpansion.Request request() {
        return new GraphifyProductionExpansion.Request(REVISION, GRAPH, snapshot(), List.of(seed()),
                new GraphifyProductionExpansion.QueryBounds(2, 10, 10, 5, 100_000));
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
        Map<String, Object> response = Map.of("nodes", List.of(), "edges", List.of(), "paths", List.of());
        @Override public Map<String, Object> expand(Map<String, Object> query, Map<String, Object> snapshotRef) {
            queries.add(new LinkedHashMap<>(query)); return response;
        }
        @Override public Map<String, Object> orient(Map<String, Object> a, Map<String, Object> b) { throw new AssertionError(); }
        @Override public Map<String, Object> find(Map<String, Object> a, Map<String, Object> b) { throw new AssertionError(); }
        @Override public Map<String, Object> trace(Map<String, Object> a, Map<String, Object> b) { throw new AssertionError(); }
        @Override public Map<String, Object> diff(Map<String, Object> a, Map<String, Object> b, Map<String, Object> c) { throw new AssertionError(); }
    }
}

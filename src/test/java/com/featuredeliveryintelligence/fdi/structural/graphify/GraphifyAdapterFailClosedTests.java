package com.featuredeliveryintelligence.fdi.structural.graphify;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fail-closed boundary tests for {@link GraphifyAdapter}, the in-process Java
 * boundary in front of the Graphify transport. A recording stub transport and
 * stub attestor stand in for the external runtime so every refusal path
 * (missing tool mapping, invalid snapshot descriptor, query/snapshot mismatch,
 * and result-bound enforcement) is pinned without a Graphify install and
 * without network access.
 */
class GraphifyAdapterFailClosedTests {

    /** Records invocations and returns a canned, bound-respecting result. */
    private static final class StubTransport implements GraphifyTransport {
        final List<Map<String, Object>> calls = new ArrayList<>();
        Map<String, Object> result = Map.of(
                "nodes", List.of(), "edges", List.of(), "paths", List.of());

        @Override
        public Map<String, Object> invoke(String tool, Map<String, Object> payload) {
            calls.add(Map.of("tool", tool, "payload", payload));
            return result;
        }
    }

    private static Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("snapshot_id", "snap-1");
        snapshot.put("provider_scope_id", "scope-9");
        snapshot.put("provider_ref", "ref-7");
        snapshot.put("repositories", List.of(Map.of(
                "repository_id", "repo-a",
                "canonical_revision", "a".repeat(40))));
        return snapshot;
    }

    private static Map<String, Object> query() {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("snapshot_id", "snap-1");
        query.put("operation", "FIND");
        query.put("max_depth", 3);
        query.put("max_nodes", 10);
        query.put("max_edges", 10);
        query.put("max_paths", 5);
        query.put("max_result_bytes", 100_000);
        return query;
    }

    private static GraphifyAdapter adapter(GraphifyTransport transport) {
        return new GraphifyAdapter(transport,
                snapshot -> Map.of("binding_state", "VERIFIED"),
                Map.of("FIND", "frozen_find_tool", "DIFF", "frozen_diff_tool",
                        "ORIENT", "frozen_orient_tool"),
                (operation, raw) -> raw);
    }

    @Test
    void missingToolMappingFailsClosedBeforeTransport() {
        StubTransport transport = new StubTransport();
        GraphifyAdapter adapter = new GraphifyAdapter(transport,
                snapshot -> Map.of("binding_state", "VERIFIED"),
                Map.of(), (operation, raw) -> raw);
        assertThatThrownBy(() -> adapter.find(query(), snapshot()))
                .isInstanceOf(RuntimeContractException.class)
                .hasMessageContaining("missing Graphify tool mapping: FIND");
        assertThat(transport.calls).isEmpty();
    }

    @Test
    void invalidSnapshotDescriptorFailsClosedBeforeTransport() {
        StubTransport transport = new StubTransport();
        Map<String, Object> broken = snapshot();
        broken.remove("provider_ref");
        assertThatThrownBy(() -> adapter(transport).find(query(), broken))
                .isInstanceOf(RuntimeContractException.class)
                .hasMessageContaining("provider_ref");
        assertThat(transport.calls).isEmpty();
    }

    @Test
    void querySnapshotMismatchFailsClosed() {
        StubTransport transport = new StubTransport();
        Map<String, Object> foreign = query();
        foreign.put("snapshot_id", "snap-2");
        assertThatThrownBy(() -> adapter(transport).find(foreign, snapshot()))
                .isInstanceOf(RuntimeContractException.class)
                .hasMessageContaining("query snapshot mismatch");
        assertThat(transport.calls).isEmpty();
    }

    @Test
    void nodeEdgeAndPathBoundsAreEnforced() {
        StubTransport transport = new StubTransport();
        transport.result = Map.of(
                "nodes", List.of("n1", "n2"),
                "edges", List.of(),
                "paths", List.of());
        Map<String, Object> tight = query();
        tight.put("max_nodes", 1);
        assertThatThrownBy(() -> adapter(transport).find(tight, snapshot()))
                .isInstanceOf(RuntimeContractException.class)
                .hasMessageContaining("max_nodes exceeded");

        StubTransport edges = new StubTransport();
        edges.result = Map.of(
                "nodes", List.of(), "edges", List.of("e1", "e2"), "paths", List.of());
        Map<String, Object> tightEdges = query();
        tightEdges.put("max_edges", 1);
        assertThatThrownBy(() -> adapter(edges).find(tightEdges, snapshot()))
                .isInstanceOf(RuntimeContractException.class)
                .hasMessageContaining("max_edges exceeded");

        StubTransport paths = new StubTransport();
        paths.result = Map.of(
                "nodes", List.of(), "edges", List.of(), "paths", List.of("p1", "p2"));
        Map<String, Object> tightPaths = query();
        tightPaths.put("max_paths", 1);
        assertThatThrownBy(() -> adapter(paths).find(tightPaths, snapshot()))
                .isInstanceOf(RuntimeContractException.class)
                .hasMessageContaining("max_paths exceeded");
    }

    @Test
    void maxResultBytesIsEnforced() {
        StubTransport transport = new StubTransport();
        transport.result = Map.of("nodes", List.of("x".repeat(500)));
        Map<String, Object> tight = query();
        tight.put("max_result_bytes", 16);
        assertThatThrownBy(() -> adapter(transport).find(tight, snapshot()))
                .isInstanceOf(RuntimeContractException.class)
                .hasMessageContaining("max_result_bytes exceeded");
    }

    @Test
    void payloadCarriesBindingGroupRefAndMappedTool() {
        StubTransport transport = new StubTransport();
        Map<String, Object> result = adapter(transport).find(query(), snapshot());
        assertThat(result).containsOnlyKeys("nodes", "edges", "paths");
        assertThat(transport.calls).hasSize(1);
        Map<String, Object> call = transport.calls.get(0);
        assertThat(call.get("tool")).isEqualTo("frozen_find_tool");
        Map<String, Object> payload = (Map<String, Object>) call.get("payload");
        assertThat(payload.get("binding")).isEqualTo(Map.of("binding_state", "VERIFIED"));
        assertThat(payload.get("group")).isEqualTo("scope-9");
        assertThat(payload.get("ref")).isEqualTo("ref-7");
        assertThat(payload.get("snapshot_id")).isEqualTo("snap-1");
    }

    @Test
    void diffAttestsBothSnapshots() {
        StubTransport transport = new StubTransport();
        adapter(transport).diff(query(), snapshot(), snapshot());
        assertThat(transport.calls).hasSize(1);
        Map<String, Object> payload = (Map<String, Object>) transport.calls.get(0).get("payload");
        assertThat(payload).containsKeys("before_binding", "after_binding");
        assertThat(transport.calls.get(0).get("tool")).isEqualTo("frozen_diff_tool");
    }

    @Test
    void orientSkipsQueryValidationButNotSnapshotValidation() {
        StubTransport transport = new StubTransport();
        Map<String, Object> minimal = Map.of("anything", true);
        adapter(transport).orient(minimal, snapshot());
        assertThat(transport.calls).hasSize(1);

        StubTransport unused = new StubTransport();
        Map<String, Object> broken = snapshot();
        broken.remove("snapshot_id");
        assertThatThrownBy(() -> adapter(unused).orient(minimal, broken))
                .isInstanceOf(RuntimeContractException.class)
                .hasMessageContaining("snapshot_id");
        assertThat(unused.calls).isEmpty();
    }
}

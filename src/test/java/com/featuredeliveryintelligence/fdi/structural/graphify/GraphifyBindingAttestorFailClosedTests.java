package com.featuredeliveryintelligence.fdi.structural.graphify;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fail-closed boundary tests for {@link GraphifyBindingAttestor}. A stub probe
 * stands in for the Graphify route so every refusal path (unqueryable route,
 * invalid freshness, duplicate or mismatched repository bindings, malformed
 * descriptors) is pinned without a Graphify install and without network
 * access. The revision-mismatch path itself is regression-covered in
 * {@code ReviewRegressionTests}; these tests pin the remaining gates and the
 * normalized attestation shape.
 */
class GraphifyBindingAttestorFailClosedTests {
    private static final String CANONICAL = "a".repeat(40);

    private static Map<String, Object> snapshot() {
        return Map.of(
                "snapshot_id", "snap-1",
                "provider_scope_id", "scope-9",
                "provider_ref", "ref-7",
                "repositories", List.of(Map.of(
                        "repository_id", "repo-a", "canonical_revision", CANONICAL)));
    }

    private static Map<String, Object> probeResponse() {
        return Map.of(
                "queryable", true,
                "runtime_version", "0.1.14",
                "wire_version", "MCP 2025-11-25",
                "repository_bindings", List.of(Map.of(
                        "repository_id", "repo-a",
                        "indexed_revision", CANONICAL,
                        "head_revision", "b".repeat(40))));
    }

    private static GraphifyBindingAttestor attestor(
            java.util.function.Function<Map<String, Object>, Map<String, Object>> probe) {
        return new GraphifyBindingAttestor(probe, "fdi-adapter-1");
    }

    @Test
    void unqueryableRouteFailsClosed() {
        Map<String, Object> response = new java.util.HashMap<>(probeResponse());
        response.put("queryable", false);
        assertThatThrownBy(() -> attestor(ignored -> response).attest(snapshot()))
                .isInstanceOf(RuntimeContractException.class)
                .hasMessageContaining("Graphify route is not queryable");
    }

    @Test
    void missingQueryableFlagFailsClosed() {
        Map<String, Object> response = new java.util.HashMap<>(probeResponse());
        response.remove("queryable");
        assertThatThrownBy(() -> attestor(ignored -> response).attest(snapshot()))
                .isInstanceOf(RuntimeContractException.class)
                .hasMessageContaining("Graphify route is not queryable");
    }

    @Test
    void invalidFreshnessFailsClosed() {
        Map<String, Object> response = new java.util.HashMap<>(probeResponse());
        response.put("freshness", "STALE");
        assertThatThrownBy(() -> attestor(ignored -> response).attest(snapshot()))
                .isInstanceOf(RuntimeContractException.class)
                .hasMessageContaining("invalid Graphify freshness: STALE");
    }

    @Test
    void supportedFreshnessValuesAreAccepted() {
        for (String freshness : List.of("LIVE_CURRENT", "FROZEN_INDEXED")) {
            Map<String, Object> response = new java.util.HashMap<>(probeResponse());
            response.put("freshness", freshness);
            Map<String, Object> attestation = attestor(ignored -> response).attest(snapshot());
            assertThat(attestation.get("freshness")).isEqualTo(freshness);
        }
    }

    @Test
    void duplicateRepositoryBindingFailsClosed() {
        Map<String, Object> response = new java.util.HashMap<>(probeResponse());
        response.put("repository_bindings", List.of(
                Map.of("repository_id", "repo-a", "indexed_revision", CANONICAL),
                Map.of("repository_id", "repo-a", "indexed_revision", CANONICAL)));
        assertThatThrownBy(() -> attestor(ignored -> response).attest(snapshot()))
                .isInstanceOf(RuntimeContractException.class)
                .hasMessageContaining("duplicate repository binding: repo-a");
    }

    @Test
    void unboundSnapshotRepositoryFailsClosed() {
        // The probe knows nothing about repo-a, so no binding can attest it.
        Map<String, Object> response = new java.util.HashMap<>(probeResponse());
        response.put("repository_bindings", List.of());
        assertThatThrownBy(() -> attestor(ignored -> response).attest(snapshot()))
                .isInstanceOf(RuntimeContractException.class)
                .hasMessageContaining("repository revision mismatch: repo-a");
    }

    @Test
    void extraUnrequestedBindingFailsClosed() {
        Map<String, Object> response = new java.util.HashMap<>(probeResponse());
        response.put("repository_bindings", List.of(
                Map.of("repository_id", "repo-a", "indexed_revision", CANONICAL),
                Map.of("repository_id", "repo-b", "indexed_revision", CANONICAL)));
        assertThatThrownBy(() -> attestor(ignored -> response).attest(snapshot()))
                .isInstanceOf(RuntimeContractException.class)
                .hasMessageContaining("repository binding set mismatch");
    }

    @Test
    void nonObjectRepositoryBindingFailsClosed() {
        Map<String, Object> response = new java.util.HashMap<>(probeResponse());
        response.put("repository_bindings", List.of("not-an-object"));
        assertThatThrownBy(() -> attestor(ignored -> response).attest(snapshot()))
                .isInstanceOf(RuntimeContractException.class)
                .hasMessageContaining("repository binding must be object");
    }

    @Test
    void attestationNormalizesVerifiedBinding() {
        Map<String, Object> attestation = attestor(ignored -> probeResponse()).attest(snapshot());
        assertThat(attestation.get("snapshot_id")).isEqualTo("snap-1");
        assertThat(attestation.get("binding_state")).isEqualTo("VERIFIED");
        assertThat(attestation.get("freshness")).isEqualTo("FROZEN_INDEXED");
        Map<String, Object> route = (Map<String, Object>) attestation.get("provider_route");
        assertThat(route).containsEntry("scope_id", "scope-9").containsEntry("ref", "ref-7");
        List<Map<String, Object>> repositories =
                (List<Map<String, Object>>) attestation.get("repositories");
        assertThat(repositories).hasSize(1);
        assertThat(repositories.get(0))
                .containsEntry("repository", "repo-a")
                .containsEntry("indexed_revision", CANONICAL)
                .containsEntry("queryable", true);
        Map<String, Object> runtime = (Map<String, Object>) attestation.get("provider_runtime");
        assertThat(runtime)
                .containsEntry("runtime_version", "0.1.14")
                .containsEntry("wire_version", "MCP 2025-11-25")
                .containsEntry("adapter_version", "fdi-adapter-1")
                .containsEntry("compatibility", "VERIFIED");
    }
}

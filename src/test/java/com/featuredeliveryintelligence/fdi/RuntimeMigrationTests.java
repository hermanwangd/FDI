package com.featuredeliveryintelligence.fdi;import com.featuredeliveryintelligence.fdi.application.RuntimeCapabilities;import com.featuredeliveryintelligence.fdi.validation.CanonicalBaseGate;import com.featuredeliveryintelligence.fdi.validation.Dev204Validation;import com.featuredeliveryintelligence.fdi.validation.VerificationAccounting;import com.featuredeliveryintelligence.fdi.feature.FeatureDiscovery;import com.featuredeliveryintelligence.fdi.feature.FeatureKnowledgePlan;import com.featuredeliveryintelligence.fdi.feature.RealizationTraversal;import com.featuredeliveryintelligence.fdi.product.ProductSemantics;import com.featuredeliveryintelligence.fdi.product.ProductKnowledgeMaintenance;import com.featuredeliveryintelligence.fdi.structural.graphify.GrafelAdapter;import com.featuredeliveryintelligence.fdi.structural.graphify.GrafelBindingAttestor;import com.featuredeliveryintelligence.fdi.structural.graphify.GrafelBindingEvidence;import com.featuredeliveryintelligence.fdi.structural.graphify.GrafelTransport;import com.featuredeliveryintelligence.fdi.structural.api.CodeIntelligenceProvider;import com.featuredeliveryintelligence.fdi.structural.api.SnapshotBindingAttestor;import com.featuredeliveryintelligence.fdi.structural.api.StructuralIntelligence;import com.featuredeliveryintelligence.fdi.structural.api.StructuralMaintenance;import com.featuredeliveryintelligence.fdi.shared.RuntimeMaps;import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class RuntimeMigrationTests {
    @Test
    void capabilityCompilationEnforcesRootBounds() {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("template_id", "structural"); template.put("capability", "CODE_INTELLIGENCE");
        template.put("allowed_modes", List.of("OPTIONAL", "REQUIRED")); template.put("may_promote_to_required", true);
        template.put("allowed_operations", List.of("FIND", "TRACE"));
        template.put("maximum_bounds", bounds(3, 100, 100, 10, 10_000));
        Map<String, Object> root = Map.of("skill_id", "skill", "skill_revision", "1", "runtime_capability_templates", List.of(template));
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("template_id", "structural"); item.put("capability", "CODE_INTELLIGENCE"); item.put("mode", "REQUIRED");
        item.put("operations", List.of("TRACE", "FIND", "FIND")); item.put("bounds", bounds(2, 50, 60, 5, 5_000));
        Map<String, Object> plan = Map.of("plan_id", "plan", "revision", 1, "runtime_capabilities", List.of(item));

        List<Map<String, Object>> compiled = RuntimeCapabilities.compile(root, plan);
        assertThat(compiled).singleElement().extracting(row -> row.get("operations")).isEqualTo(List.of("FIND", "TRACE"));

        item.put("bounds", bounds(4, 50, 60, 5, 5_000));
        assertThatThrownBy(() -> RuntimeCapabilities.compile(root, plan)).hasMessageContaining("exceeds");
    }

    @Test
    void grafelAdapterPassesExactRouteAndBounds() {
        List<Map<String, Object>> calls = new ArrayList<>();
        GrafelTransport transport = (tool, payload) -> { calls.add(Map.of("tool", tool, "payload", payload)); return Map.of("nodes", List.of(), "edges", List.of(), "paths", List.of()); };
        GrafelAdapter adapter = new GrafelAdapter(transport, snapshot -> Map.of("result", "EXACTLY_BOUND"));
        Map<String, Object> snapshot = snapshot();
        Map<String, Object> query = new LinkedHashMap<>(bounds(3, 10, 10, 5, 10_000));
        query.put("snapshot_id", "s"); query.put("operation", "TRACE"); query.put("query_id", "q");

        adapter.trace(query, snapshot);

        assertThat(calls).singleElement().satisfies(call -> {
            assertThat(call.get("tool")).isEqualTo("grafel_find_paths");
            @SuppressWarnings("unchecked") Map<String, Object> payload = (Map<String, Object>) call.get("payload");
            assertThat(payload).containsEntry("group", "g").containsEntry("ref", "r");
        });
    }

    @Test
    void verificationSummaryFailsClosedOnMismatchedResult() {
        Map<String, Object> pass = Map.of("result", "PASS", "passed", 2, "failed", 0);
        Map<String, Object> summary = VerificationAccounting.buildVerificationSummary("0.4.8.3", pass, pass, List.of("F001"));
        assertThat(summary).containsEntry("result", "PASS");
        summary.put("result", "FAIL");
        assertThatThrownBy(() -> VerificationAccounting.validateVerificationSummary(summary)).hasMessageContaining("mismatch");
    }

    private static Map<String, Object> snapshot() {
        return Map.of("snapshot_id", "s", "provider_scope_id", "g", "provider_ref", "r",
                "repositories", List.of(Map.of("repository_id", "repo", "canonical_revision", "a".repeat(40))));
    }

    private static Map<String, Object> bounds(int depth, int nodes, int edges, int paths, int bytes) {
        return new LinkedHashMap<>(Map.of("max_depth", depth, "max_nodes", nodes, "max_edges", edges,
                "max_paths", paths, "max_result_bytes", bytes));
    }
}

package com.featuredeliveryintelligence.fdi.structural.graphify;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.shared.RuntimeMaps;
import com.featuredeliveryintelligence.fdi.structural.api.StructuralIntelligence;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GraphifyBindingEvidence {
    private GraphifyBindingEvidence() {}

    public static Map<String, Object> build(
            Map<String, Object> snapshot,
            Map<String, Object> attestation,
            String productOrScenarioId,
            String capturedAt,
            List<String> limitations) {
        StructuralIntelligence.validateSnapshotRef(snapshot);
        if (!"VERIFIED".equals(attestation.get("binding_state"))) {
            throw new RuntimeContractException("snapshot binding must be VERIFIED");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> runtime = (Map<String, Object>) attestation.get("provider_runtime");
        if (runtime == null) {
            throw new RuntimeContractException("provider_runtime is required");
        }
        List<Map<String, Object>> sourceSnapshots = new ArrayList<>();
        for (Object raw : RuntimeMaps.list(snapshot, "repositories")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> repository = (Map<String, Object>) raw;
            sourceSnapshots.add(Map.of(
                    "repository", RuntimeMaps.requiredString(repository, "repository_id"),
                    "revision", RuntimeMaps.requiredString(repository, "canonical_revision")));
        }
        Map<String, Object> snapshotRef = new LinkedHashMap<>();
        snapshotRef.put("snapshot_id", RuntimeMaps.requiredString(snapshot, "snapshot_id"));
        snapshotRef.put("provider", Map.of(
                "name", "GRAPHIFY",
                "version", RuntimeMaps.requiredString(runtime, "runtime_version")));
        snapshotRef.put("adapter_version", RuntimeMaps.requiredString(runtime, "adapter_version"));
        snapshotRef.put("source_snapshots", sourceSnapshots);
        snapshotRef.put("created_at", capturedAt);

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("schema_version", "0.1");
        evidence.put("evidence_id", GraphifyBindingAttestor.stableId(
                "gbe", snapshot.get("snapshot_id") + "\0" + productOrScenarioId));
        evidence.put("product_or_scenario_id", productOrScenarioId);
        evidence.put("snapshot_ref", snapshotRef);
        evidence.put("snapshot_binding_attestation", RuntimeMaps.copy(attestation));
        evidence.put("captured_at", capturedAt);
        evidence.put("result", "EXACTLY_BOUND");
        evidence.put("limitations", limitations == null ? List.of() : List.copyOf(limitations));
        return evidence;
    }
}

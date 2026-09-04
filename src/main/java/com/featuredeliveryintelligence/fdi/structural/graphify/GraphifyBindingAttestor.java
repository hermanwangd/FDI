package com.featuredeliveryintelligence.fdi.structural.graphify;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.featuredeliveryintelligence.fdi.shared.RuntimeMaps;
import com.featuredeliveryintelligence.fdi.structural.api.SnapshotBindingAttestor;
import com.featuredeliveryintelligence.fdi.structural.api.StructuralIntelligence;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class GraphifyBindingAttestor implements SnapshotBindingAttestor {
    private final Function<Map<String, Object>, Map<String, Object>> probe;
    private final String adapterVersion;

    public GraphifyBindingAttestor(
            Function<Map<String, Object>, Map<String, Object>> probe, String adapterVersion) {
        this.probe = Objects.requireNonNull(probe);
        this.adapterVersion = Objects.requireNonNull(adapterVersion);
    }

    @Override
    public Map<String, Object> attest(Map<String, Object> snapshot) {
        StructuralIntelligence.validateSnapshotRef(snapshot);
        Map<String, Object> response = probe.apply(snapshot);
        if (!Boolean.TRUE.equals(response.get("queryable"))) {
            throw new RuntimeContractException("Graphify route is not queryable");
        }
        String runtime = RuntimeMaps.requiredString(response, "runtime_version");
        String wire = RuntimeMaps.requiredString(response, "wire_version");
        String freshness = response.getOrDefault("freshness", "FROZEN_INDEXED").toString();
        if (!List.of("LIVE_CURRENT", "FROZEN_INDEXED").contains(freshness)) {
            throw new RuntimeContractException("invalid Graphify freshness: " + freshness);
        }

        Map<String, Map<String, Object>> indexed = new HashMap<>();
        for (Object raw : RuntimeMaps.list(response, "repository_bindings")) {
            Map<String, Object> binding = map(raw);
            String id = RuntimeMaps.requiredString(binding, "repository_id");
            if (indexed.put(id, binding) != null) {
                throw new RuntimeContractException("duplicate repository binding: " + id);
            }
        }

        List<Map<String, Object>> repositories = new ArrayList<>();
        for (Object raw : RuntimeMaps.list(snapshot, "repositories")) {
            Map<String, Object> repository = map(raw);
            String id = RuntimeMaps.requiredString(repository, "repository_id");
            String canonical = RuntimeMaps.requiredString(repository, "canonical_revision").toLowerCase();
            Map<String, Object> binding = indexed.get(id);
            String indexedRevision = binding == null
                    ? ""
                    : RuntimeMaps.requiredString(binding, "indexed_revision").toLowerCase();
            if (!canonical.equals(indexedRevision)) {
                throw new RuntimeContractException("repository revision mismatch: " + id);
            }
            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("repository", id);
            normalized.put("indexed_revision", canonical);
            normalized.put("queryable", true);
            normalized.put("head_revision", binding.get("head_revision"));
            repositories.add(normalized);
        }
        if (indexed.size() != repositories.size()) {
            throw new RuntimeContractException("repository binding set mismatch");
        }

        String snapshotId = RuntimeMaps.requiredString(snapshot, "snapshot_id");
        Map<String, Object> attestation = new LinkedHashMap<>();
        attestation.put("snapshot_id", snapshotId);
        attestation.put("binding_state", "VERIFIED");
        attestation.put("provider_route", Map.of(
                "scope_id", RuntimeMaps.requiredString(snapshot, "provider_scope_id"),
                "ref", RuntimeMaps.requiredString(snapshot, "provider_ref")));
        attestation.put("freshness", freshness);
        attestation.put("repositories", repositories);
        attestation.put("attestation_id", stableId("sba", snapshotId));
        attestation.put("provider_runtime", Map.of(
                "runtime_version", runtime,
                "wire_version", wire,
                "adapter_version", adapterVersion,
                "compatibility", "VERIFIED"));
        return attestation;
    }

    static String stableId(String prefix, String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return prefix + ":" + HexFormat.of().formatHex(digest).substring(0, 24);
        } catch (Exception error) {
            throw new RuntimeContractException("cannot create Graphify evidence identifier", error);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?>)) {
            throw new RuntimeContractException("repository binding must be object");
        }
        return (Map<String, Object>) value;
    }
}

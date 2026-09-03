package com.featuredeliveryintelligence.fdi.structural.api;import com.featuredeliveryintelligence.fdi.shared.RuntimeMaps;import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public final class StructuralIntelligence {
    private static final Set<String> OPERATIONS = Set.of("ORIENT", "FIND", "EXPAND", "TRACE", "DIFF");
    private StructuralIntelligence() {}

    public static Map<String, Object> validateSnapshotRef(Map<String, Object> ref) {
        RuntimeMaps.requiredString(ref, "snapshot_id");
        RuntimeMaps.requiredString(ref, "provider_scope_id");
        RuntimeMaps.requiredString(ref, "provider_ref");
        List<Object> repositories = RuntimeMaps.list(ref, "repositories");
        if (repositories.isEmpty()) throw new RuntimeContractException("repositories must not be empty");
        for (Object item : repositories) {
            if (!(item instanceof Map<?, ?> raw)) throw new RuntimeContractException("repository must be an object");
            @SuppressWarnings("unchecked") Map<String, Object> repo = (Map<String, Object>) raw;
            RuntimeMaps.requiredString(repo, "repository_id");
            String revision = RuntimeMaps.requiredString(repo, "canonical_revision");
            if (!revision.matches("(?i)[0-9a-f]{40}|[0-9a-f]{64}"))
                throw new RuntimeContractException("canonical_revision must be a full Git object id");
        }
        return ref;
    }

    public static Map<String, Object> validateStructuralQuery(Map<String, Object> query, Map<String, Object> snapshot) {
        validateSnapshotRef(snapshot);
        if (!Objects.equals(query.get("snapshot_id"), snapshot.get("snapshot_id")))
            throw new RuntimeContractException("query snapshot mismatch");
        String operation = RuntimeMaps.requiredString(query, "operation");
        if (!OPERATIONS.contains(operation)) throw new RuntimeContractException("unsupported operation");
        for (String key : List.of("max_depth", "max_nodes", "max_edges", "max_paths", "max_result_bytes"))
            RuntimeMaps.requiredPositiveInt(query, key);
        return query;
    }

    public static Map<String, Object> normalizeObservations(List<Map<String, Object>> records,
            Map<String, Object> snapshot, Map<String, Object> query, boolean requirePathIds) {
        validateStructuralQuery(query, snapshot);
        int maxNodes = RuntimeMaps.requiredPositiveInt(query, "max_nodes");
        int maxEdges = RuntimeMaps.requiredPositiveInt(query, "max_edges");
        if (records.size() > maxEdges) throw new RuntimeContractException("max_edges exceeded");
        Set<String> nodes = new LinkedHashSet<>();
        List<Map<String, Object>> observations = new ArrayList<>();
        for (Map<String, Object> record : records) {
            String source = RuntimeMaps.requiredString(record, "source");
            String target = RuntimeMaps.requiredString(record, "target");
            String relation = RuntimeMaps.requiredString(record, "relation_type");
            nodes.add(source); nodes.add(target);
            if (requirePathIds && RuntimeMaps.list(record, "path_ids").isEmpty())
                throw new RuntimeContractException("path_ids are required");
            Map<String, Object> normalized = RuntimeMaps.copy(record);
            normalized.put("observation_id", stableId(snapshot.get("snapshot_id") + "\0" + source + "\0" + relation + "\0" + target));
            normalized.put("snapshot_id", snapshot.get("snapshot_id"));
            observations.add(normalized);
        }
        if (nodes.size() > maxNodes) throw new RuntimeContractException("max_nodes exceeded");
        observations.sort(Comparator.comparing(row -> row.get("observation_id").toString()));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schema_version", "0.1");
        result.put("snapshot_id", snapshot.get("snapshot_id"));
        result.put("query_id", query.get("query_id"));
        result.put("observations", observations);
        return result;
    }

    private static String stableId(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return "obs:" + HexFormat.of().formatHex(digest);
        } catch (Exception error) { throw new IllegalStateException(error); }
    }
}

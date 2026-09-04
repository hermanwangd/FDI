package com.featuredeliveryintelligence.fdi.structural.graphify;import com.featuredeliveryintelligence.fdi.structural.api.CodeIntelligenceProvider;import com.featuredeliveryintelligence.fdi.structural.api.SnapshotBindingAttestor;import com.featuredeliveryintelligence.fdi.structural.api.StructuralIntelligence;import com.featuredeliveryintelligence.fdi.structural.api.StructuralMaintenance;import com.featuredeliveryintelligence.fdi.shared.RuntimeMaps;import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.function.BiFunction;

public final class GraphifyAdapter implements CodeIntelligenceProvider {
    private final GraphifyTransport transport;
    private final SnapshotBindingAttestor attestor;
    private final Map<String, String> tools;
    private final BiFunction<String, Map<String, Object>, Map<String, Object>> responseMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GraphifyAdapter(GraphifyTransport transport, SnapshotBindingAttestor attestor,
            Map<String, String> tools, BiFunction<String, Map<String, Object>, Map<String, Object>> responseMapper) {
        this.transport = Objects.requireNonNull(transport);
        this.attestor = Objects.requireNonNull(attestor);
        this.tools = Map.copyOf(tools);
        this.responseMapper = Objects.requireNonNull(responseMapper);
    }

    @Override public Map<String, Object> orient(Map<String, Object> request, Map<String, Object> snapshot) {
        return invoke("ORIENT", request, snapshot);
    }
    @Override public Map<String, Object> find(Map<String, Object> query, Map<String, Object> snapshot) {
        return invoke("FIND", query, snapshot);
    }
    @Override public Map<String, Object> expand(Map<String, Object> query, Map<String, Object> snapshot) {
        return invoke("EXPAND", query, snapshot);
    }
    @Override public Map<String, Object> trace(Map<String, Object> query, Map<String, Object> snapshot) {
        return invoke("TRACE", query, snapshot);
    }
    @Override public Map<String, Object> diff(Map<String, Object> request, Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> payload = RuntimeMaps.copy(request);
        payload.put("before_binding", attestor.attest(StructuralIntelligence.validateSnapshotRef(before)));
        payload.put("after_binding", attestor.attest(StructuralIntelligence.validateSnapshotRef(after)));
        return mapped("DIFF", transport.invoke(tool("DIFF"), payload), request);
    }

    private Map<String, Object> invoke(String operation, Map<String, Object> query, Map<String, Object> snapshot) {
        StructuralIntelligence.validateSnapshotRef(snapshot);
        if (!"ORIENT".equals(operation)) StructuralIntelligence.validateStructuralQuery(query, snapshot);
        Map<String, Object> payload = RuntimeMaps.copy(query);
        payload.put("binding", attestor.attest(snapshot));
        payload.put("group", snapshot.get("provider_scope_id"));
        payload.put("ref", snapshot.get("provider_ref"));
        return mapped(operation, transport.invoke(tool(operation), payload), query);
    }

    private Map<String, Object> mapped(String operation, Map<String, Object> raw, Map<String, Object> query) {
        enforceBounds(raw, query);
        return responseMapper.apply(operation, raw);
    }

    private String tool(String operation) {
        String tool = tools.get(operation);
        if (tool == null || tool.isBlank()) throw new RuntimeContractException("missing Graphify tool mapping: " + operation);
        return tool;
    }

    private void enforceBounds(Map<String, Object> result, Map<String, Object> query) {
        for (String pair : List.of("nodes:max_nodes", "edges:max_edges", "paths:max_paths")) {
            String[] keys = pair.split(":");
            if (query.get(keys[1]) instanceof Number limit && RuntimeMaps.list(result, keys[0]).size() > limit.intValue())
                throw new RuntimeContractException(keys[1] + " exceeded");
        }
        if (query.get("max_result_bytes") instanceof Number limit) {
            try {
                if (objectMapper.writeValueAsBytes(result).length > limit.intValue())
                    throw new RuntimeContractException("max_result_bytes exceeded");
            } catch (RuntimeContractException error) { throw error; }
            catch (Exception error) { throw new RuntimeContractException("Graphify response is not serializable", error); }
        }
    }
}

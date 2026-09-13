package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/** Producer-facing serialization, intentionally separate from evaluator-only types. */
final class CalibrationProducer {
    record Binding(String sourceRevision, List<String> productionRoots, List<String> testRoots,
                   String inputSnapshotSha256, String extractorSha256) { }
    record Method(String sourceRevision, String path, String signature) { }
    record Pair(String scenarioId, Method method) { }
    record Edge(String scenarioId, Method from, Method to) { }
    record Claim(Pair pair, String role, String evidenceRef) { }
    record EdgeClaim(Edge edge, String evidenceRef) { }
    record Proposals(List<Claim> methods, List<EdgeClaim> edges,
                     List<String> unresolvedScenarios, List<String> unsupportedTypes) { }
    record Artifact(Binding binding, Proposals proposals) { }
    static Artifact produce(Binding binding, JsonNode seeds, SourceMethodIndex index, boolean expand) {
        Map<Pair, Claim> claims = new LinkedHashMap<>();
        Map<Edge, EdgeClaim> edges = new LinkedHashMap<>();
        List<String> unresolved = new ArrayList<>();
        for (JsonNode scenario : seeds.required("scenarios")) {
            String id = scenario.required("scenarioId").asText();
            var frontier = new LinkedHashSet<SourceMethodIndex.Method>();
            for (JsonNode component : scenario.required("components")) {
                String strength = component.required("evidenceStrength").asText();
                if (!List.of("EXACT_ROUTE_HANDLER", "DIRECT_PRODUCTION_REFERENCE").contains(strength)) continue;
                SourceMethodIndex.Method method = index.unique(component.required("productionIdentity").asText());
                if (method == null || component.required("evidenceRefs").isEmpty()) continue;
                String ref = "seed:" + component.get("evidenceRefs").get(0).asText();
                Pair pair = new Pair(id, bind(binding, method));
                claims.putIfAbsent(pair, new Claim(pair, component.required("role").asText(), ref));
                frontier.add(method);
            }
            if (frontier.isEmpty()) unresolved.add(id);
            var visited = new LinkedHashSet<>(frontier);
            for (int depth = 0; expand && depth < 2; depth++) {
                var next = new LinkedHashSet<SourceMethodIndex.Method>();
                for (SourceMethodIndex.Method from : frontier) {
                    for (SourceMethodIndex.Method to : index.calls(from)) {
                        if (from.equals(to)) continue;
                        String ref = "static-call:" + from.signature() + "->" + to.signature();
                        Pair pair = new Pair(id, bind(binding, to));
                        claims.putIfAbsent(pair, new Claim(pair, "STATIC_PRODUCTION_CALL", ref));
                        Edge edge = new Edge(id, bind(binding, from), pair.method());
                        edges.putIfAbsent(edge, new EdgeClaim(edge, ref));
                        if (visited.add(to)) next.add(to);
                        if (visited.size() > 64) throw new IllegalArgumentException("SCENARIO_EXPANSION_LIMIT");
                    }
                }
                frontier = next;
            }
        }
        return new Artifact(binding, new Proposals(List.copyOf(claims.values()), List.copyOf(edges.values()),
                List.copyOf(unresolved), List.of()));
    }

    private static Method bind(Binding binding, SourceMethodIndex.Method method) {
        return new Method(binding.sourceRevision(), method.path(), method.signature());
    }
}

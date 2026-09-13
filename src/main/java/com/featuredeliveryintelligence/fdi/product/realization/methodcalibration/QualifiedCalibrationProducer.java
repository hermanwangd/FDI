package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Set;
import static com.featuredeliveryintelligence.fdi.product.realization.methodcalibration.CalibrationProducer.*;

final class QualifiedCalibrationProducer {
    static CalibrationProducer.Artifact produce(CalibrationProducer.Binding binding, List<String> scenarios,
            List<ScenarioEvidenceSelector.Seed> seeds, SourceMethodIndex index) {
        return produce(binding, scenarios, seeds, index, null);
    }
    static CalibrationProducer.Artifact produce(CalibrationProducer.Binding binding, List<String> scenarios,
            List<ScenarioEvidenceSelector.Seed> seeds, SourceMethodIndex index, CandidateTrace trace) {
        return produce(binding, scenarios, seeds, index, trace, false);
    }
    static CalibrationProducer.Artifact produce(CalibrationProducer.Binding binding, List<String> scenarios,
            List<ScenarioEvidenceSelector.Seed> seeds, SourceMethodIndex index, CandidateTrace trace, boolean boxing) {
        return produce(binding, scenarios, seeds, index, trace, boxing, false);
    }
    static CalibrationProducer.Artifact produce(CalibrationProducer.Binding binding, List<String> scenarios,
            List<ScenarioEvidenceSelector.Seed> seeds, SourceMethodIndex index, CandidateTrace trace,
            boolean boxing, boolean verifiedJdkAncestry) {
        Set<Claim> claims = new LinkedHashSet<>();
        Set<EdgeClaim> edges = new LinkedHashSet<>();
        Set<String> resolved = new LinkedHashSet<>();
        var scenarioMethods = new HashMap<String, Set<SourceMethodIndex.Method>>();
        var calls = new QualifiedSourceCalls(index, boxing, verifiedJdkAncestry);
        for (var seed : seeds) {
            if (!scenarios.contains(seed.scenarioId())) throw new IllegalArgumentException("UNKNOWN_SCENARIO");
            var start = index.unique(seed.productionIdentity());
            if (start == null) {
                if (trace != null) trace.add(seed.scenarioId(), seed.observationRef(), 0, "UNRESOLVED",
                        "SEED_NOT_UNIQUELY_RESOLVED", null, null, seed.productionIdentity());
                continue;
            }
            resolved.add(seed.scenarioId());
            String seedRef = "qualified-seed:" + seed.observationRef();
            claims.add(new Claim(new Pair(seed.scenarioId(), bind(binding, start)), seed.role(), seedRef));
            if (trace != null) trace.add(seed.scenarioId(), seedRef, 0, "RETAINED", "RESOLVED_SEED", null, start, null);
            var visited = new LinkedHashSet<SourceMethodIndex.Method>();
            visited.add(start);
            var all = scenarioMethods.computeIfAbsent(seed.scenarioId(), key -> new LinkedHashSet<>());
            all.add(start);
            List<SourceMethodIndex.Method> frontier = List.of(start);
            for (int depth = 0; depth < 3; depth++) {
                int targetDepth = depth + 1;
                List<SourceMethodIndex.Method> next = new ArrayList<>();
                for (var from : frontier) for (var to : calls.calls(from, seed.action(), trace == null ? null
                        : observation -> trace.add(seed.scenarioId(), seedRef, targetDepth, observation.stage(),
                                observation.reason(), from, observation.target(), observation.callSite()))) {
                    if (from.equals(to)) continue;
                    all.add(to);
                    if (all.size() > 64) throw new IllegalArgumentException("SCENARIO_EXPANSION_LIMIT");
                    String ref = seedRef + "|qualified-call:" + from.signature() + "->" + to.signature();
                    var pair = new Pair(seed.scenarioId(), bind(binding, to));
                    claims.add(new Claim(pair, "STATIC_PRODUCTION_CALL", ref));
                    if (trace != null) trace.add(seed.scenarioId(), seedRef, targetDepth, "RETAINED",
                            "STATIC_PRODUCTION_CALL", from, to, ref);
                    edges.add(new EdgeClaim(new Edge(seed.scenarioId(), bind(binding, from), pair.method()), ref));
                    if (visited.add(to)) next.add(to);
                }
                frontier = next;
            }
            if (trace != null) for (var from : frontier)
                trace.add(seed.scenarioId(), seedRef, 3, "DEPTH_FRONTIER", "OUTGOING_CALLS_NOT_INSPECTED", from, null, null);
        }
        return new Artifact(binding, new Proposals(List.copyOf(claims), List.copyOf(edges),
                scenarios.stream().filter(id -> !resolved.contains(id)).distinct().toList(), List.of()));
    }
    private static Method bind(Binding binding, SourceMethodIndex.Method method) {
        return new Method(binding.sourceRevision(), method.path(), method.signature());
    }
}

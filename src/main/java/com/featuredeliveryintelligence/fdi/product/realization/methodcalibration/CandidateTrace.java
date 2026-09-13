package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import java.util.ArrayList;
import java.util.List;

/** Bounded producer-only observations; no gold labels or inferred FN diagnoses. */
final class CandidateTrace {
    static final int MAX_EVENTS = 100_000;
    record Event(String scenarioId, String seedRef, int depth, String stage, String reason,
                 CalibrationProducer.Method from, CalibrationProducer.Method target, String callSite) { }
    record Artifact(String schema, CalibrationProducer.Binding binding, String scope, List<Event> events) { }
    private final CalibrationProducer.Binding binding;
    private final List<Event> events = new ArrayList<>();

    CandidateTrace(CalibrationProducer.Binding binding) { this.binding = binding; }
    void add(String scenario, String seed, int depth, String stage, String reason,
             SourceMethodIndex.Method from, SourceMethodIndex.Method target, String callSite) {
        if (events.size() == MAX_EVENTS) throw new IllegalArgumentException("TRACE_EVENT_LIMIT");
        events.add(new Event(scenario, seed, depth, stage, reason, bind(from), bind(target), callSite));
    }
    List<Event> events() { return List.copyOf(events); }
    Artifact artifact() {
        return new Artifact("CANDIDATE-STAGE-TRACE-001", binding,
                "POST_SEED_SELECTION_NOT_FULL_RETRIEVAL_NOT_EVALUATOR_DIAGNOSIS", events());
    }
    private CalibrationProducer.Method bind(SourceMethodIndex.Method method) {
        return method == null ? null : new CalibrationProducer.Method(binding.sourceRevision(), method.path(), method.signature());
    }
}

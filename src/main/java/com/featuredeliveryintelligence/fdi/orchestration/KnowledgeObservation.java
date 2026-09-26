package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

/** Raw, evidence-bound observation; it is not governed WorkspaceKnowledge. */
public record KnowledgeObservation(
        String observationRef,
        String workspaceRef,
        String subjectRef,
        String sourceRef,
        String statement,
        List<String> evidenceRefs,
        ObservationState state) {
    public KnowledgeObservation {
        require(observationRef, "observationRef");
        require(workspaceRef, "workspaceRef");
        require(subjectRef, "subjectRef");
        require(sourceRef, "sourceRef");
        require(statement, "statement");
        if (state == null) throw new IllegalArgumentException("state is required");
        evidenceRefs = requiredRefs(evidenceRefs, "evidenceRefs");
    }

    private static List<String> requiredRefs(List<String> refs, String field) {
        if (refs == null || refs.isEmpty()) throw new IllegalArgumentException(field + " is required");
        return List.copyOf(refs);
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}

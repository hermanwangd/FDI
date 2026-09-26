package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

public record KnowledgeCorrelation(
        String workspaceRef,
        List<KnowledgeObservation> observations,
        List<KnowledgeConflict> conflicts) {
    public KnowledgeCorrelation {
        if (workspaceRef == null || workspaceRef.isBlank()) throw new IllegalArgumentException("workspaceRef is required");
        observations = List.copyOf(observations == null ? List.of() : observations);
        conflicts = List.copyOf(conflicts == null ? List.of() : conflicts);
        if (observations.isEmpty()) throw new IllegalArgumentException("observations are required");
    }
}

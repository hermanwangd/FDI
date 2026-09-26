package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

public record KnowledgeRoutingDecision(
        KnowledgeRoute route,
        String workspaceRef,
        String missionRef,
        List<String> sourceRefs,
        List<String> evidenceRefs,
        boolean reusable,
        String rationale) {
    public KnowledgeRoutingDecision {
        if (route == null) throw new IllegalArgumentException("route is required");
        sourceRefs = List.copyOf(sourceRefs == null ? List.of() : sourceRefs);
        evidenceRefs = List.copyOf(evidenceRefs == null ? List.of() : evidenceRefs);
        if (workspaceRef == null || workspaceRef.isBlank()) throw new IllegalArgumentException("workspaceRef is required");
        if (missionRef == null || missionRef.isBlank()) throw new IllegalArgumentException("missionRef is required");
        if (rationale == null || rationale.isBlank()) throw new IllegalArgumentException("rationale is required");
    }
}

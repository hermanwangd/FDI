package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

public record MissionClosureSummary(
        String closureSummaryRef,
        String missionRef,
        String workspaceRef,
        List<String> subjectRefs,
        List<String> sourceRefs,
        List<String> evidenceRefs) {
    public MissionClosureSummary {
        if (closureSummaryRef == null || closureSummaryRef.isBlank()) throw new IllegalArgumentException("closureSummaryRef is required");
        if (missionRef == null || missionRef.isBlank()) throw new IllegalArgumentException("missionRef is required");
        if (workspaceRef == null || workspaceRef.isBlank()) throw new IllegalArgumentException("workspaceRef is required");
        subjectRefs = requiredRefs(subjectRefs, "subjectRefs");
        sourceRefs = requiredRefs(sourceRefs, "sourceRefs");
        evidenceRefs = requiredRefs(evidenceRefs, "evidenceRefs");
    }

    private static List<String> requiredRefs(List<String> refs, String field) {
        if (refs == null || refs.isEmpty()) throw new IllegalArgumentException(field + " is required");
        return List.copyOf(refs);
    }
}

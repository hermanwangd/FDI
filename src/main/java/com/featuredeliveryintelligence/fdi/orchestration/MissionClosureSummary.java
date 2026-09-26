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
        subjectRefs = List.copyOf(subjectRefs == null ? List.of() : subjectRefs);
        sourceRefs = List.copyOf(sourceRefs == null ? List.of() : sourceRefs);
        evidenceRefs = List.copyOf(evidenceRefs == null ? List.of() : evidenceRefs);
    }
}

package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

/** Evidence-backed source for Swarm knowledge building; not WorkspaceKnowledge itself. */
public record MissionLearningSource(
        String learningSourceRef,
        String workspaceRef,
        String missionRef,
        String closureSummaryRef,
        List<String> subjectRefs,
        List<String> sourceRefs,
        List<String> evidenceRefs) {
    public MissionLearningSource {
        require(learningSourceRef, "learningSourceRef");
        require(workspaceRef, "workspaceRef");
        require(missionRef, "missionRef");
        require(closureSummaryRef, "closureSummaryRef");
        subjectRefs = requiredRefs(subjectRefs, "subjectRefs");
        sourceRefs = requiredRefs(sourceRefs, "sourceRefs");
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

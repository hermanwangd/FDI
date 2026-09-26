package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

/** External capture receipt proving where approved WorkspaceKnowledge was persisted. */
public record WorkspaceKnowledgeCaptureResult(
        String captureRef,
        String knowledgeRef,
        String workspaceRef,
        String projectRef,
        String proposalRef,
        List<String> sourceRefs,
        List<String> evidenceRefs,
        String capturedAt) {
    public WorkspaceKnowledgeCaptureResult {
        require(captureRef, "captureRef");
        require(knowledgeRef, "knowledgeRef");
        require(workspaceRef, "workspaceRef");
        require(projectRef, "projectRef");
        require(proposalRef, "proposalRef");
        sourceRefs = requiredRefs(sourceRefs, "sourceRefs");
        evidenceRefs = requiredRefs(evidenceRefs, "evidenceRefs");
        require(capturedAt, "capturedAt");
    }

    private static List<String> requiredRefs(List<String> refs, String field) {
        if (refs == null || refs.isEmpty()) throw new IllegalArgumentException(field + " is required");
        return List.copyOf(refs);
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}

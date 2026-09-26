package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

/** Product-truth candidate routed to existing Product Knowledge governance, not WorkspaceKnowledge. */
public record ProductKnowledgeProposal(
        String proposalRef,
        String workspaceRef,
        String missionRef,
        List<String> sourceRefs,
        List<String> evidenceRefs,
        String statement,
        String scope,
        List<String> limitations,
        boolean requiresProductGovernance) {
    public ProductKnowledgeProposal {
        if (proposalRef == null || proposalRef.isBlank()) throw new IllegalArgumentException("proposalRef is required");
        if (workspaceRef == null || workspaceRef.isBlank()) throw new IllegalArgumentException("workspaceRef is required");
        if (missionRef == null || missionRef.isBlank()) throw new IllegalArgumentException("missionRef is required");
        if (statement == null || statement.isBlank()) throw new IllegalArgumentException("statement is required");
        if (scope == null || scope.isBlank()) throw new IllegalArgumentException("scope is required");
        sourceRefs = requiredRefs(sourceRefs, "sourceRefs");
        evidenceRefs = requiredRefs(evidenceRefs, "evidenceRefs");
        limitations = List.copyOf(limitations == null ? List.of() : limitations);
    }

    private static List<String> requiredRefs(List<String> refs, String field) {
        if (refs == null || refs.isEmpty()) throw new IllegalArgumentException(field + " is required");
        return List.copyOf(refs);
    }
}

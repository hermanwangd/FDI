package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

/** Proposed WorkspaceKnowledge; governance/persistence remains outside this candidate contract. */
public record WorkspaceKnowledgeProposal(
        String proposalRef,
        String workspaceRef,
        List<String> sourceRefs,
        KnowledgeType knowledgeType,
        String statement,
        String scope,
        String applicability,
        String limitations,
        List<String> evidenceRefs,
        List<String> conflictRefs) {
    public WorkspaceKnowledgeProposal {
        require(proposalRef, "proposalRef");
        require(workspaceRef, "workspaceRef");
        if (knowledgeType == null) throw new IllegalArgumentException("knowledgeType is required");
        require(statement, "statement");
        require(scope, "scope");
        sourceRefs = requiredRefs(sourceRefs, "sourceRefs");
        evidenceRefs = requiredRefs(evidenceRefs, "evidenceRefs");
        conflictRefs = List.copyOf(conflictRefs == null ? List.of() : conflictRefs);
        applicability = applicability == null ? "" : applicability;
        limitations = limitations == null ? "" : limitations;
    }

    private static List<String> requiredRefs(List<String> refs, String field) {
        if (refs == null || refs.isEmpty()) throw new IllegalArgumentException(field + " is required");
        return List.copyOf(refs);
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}

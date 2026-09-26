package com.featuredeliveryintelligence.fdi.orchestration;

public record GovernedWorkspaceKnowledge(
        WorkspaceKnowledgeProposal proposal,
        KnowledgeGovernanceDecision decision,
        String decisionRef,
        String decidedBy) {
    public GovernedWorkspaceKnowledge {
        if (proposal == null) throw new IllegalArgumentException("proposal is required");
        if (decision == null) throw new IllegalArgumentException("decision is required");
        if (decisionRef == null || decisionRef.isBlank()) throw new IllegalArgumentException("decisionRef is required");
        if (decidedBy == null || decidedBy.isBlank()) throw new IllegalArgumentException("decidedBy is required");
    }
}

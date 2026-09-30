package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

public record GovernedWorkspaceKnowledge(
        WorkspaceKnowledgeProposal proposal,
        KnowledgeGovernanceDecision decision,
        String decisionRef,
        String decidedBy,
        String policyRef,
        String decidedAt,
        List<String> decisionEvidenceRefs) {
    public GovernedWorkspaceKnowledge {
        if (proposal == null) throw new IllegalArgumentException("proposal is required");
        if (decision == null) throw new IllegalArgumentException("decision is required");
        if (decisionRef == null || decisionRef.isBlank()) throw new IllegalArgumentException("decisionRef is required");
        if (decidedBy == null || decidedBy.isBlank()) throw new IllegalArgumentException("decidedBy is required");
        if (decidedAt == null || decidedAt.isBlank()) throw new IllegalArgumentException("decidedAt is required");
        if (decisionEvidenceRefs == null || decisionEvidenceRefs.isEmpty()
                || decisionEvidenceRefs.stream().anyMatch(ref -> ref == null || ref.isBlank())) {
            throw new IllegalArgumentException("decisionEvidenceRefs are required");
        }
        decisionEvidenceRefs = List.copyOf(decisionEvidenceRefs);
        if (decision != KnowledgeGovernanceDecision.DEFERRED && (policyRef == null || policyRef.isBlank())) {
            throw new IllegalArgumentException("policyRef is required for a governed WorkspaceKnowledge decision");
        }
    }
}

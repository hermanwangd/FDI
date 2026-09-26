package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Evidence-carrying result for one complete Swarm-owned WorkspaceKnowledge lifecycle. */
public record WorkspaceKnowledgeLifecycleResult(
        String workspaceRef,
        String missionLearningSourceRef,
        KnowledgeRoutingDecision routingDecision,
        WorkspaceKnowledgeProposal proposal,
        GovernedWorkspaceKnowledge governedKnowledge,
        Optional<WorkspaceKnowledgeCaptureResult> captureReceipt,
        List<WorkspaceKnowledgeProposal> retrievedKnowledge) {

    public WorkspaceKnowledgeLifecycleResult {
        require(workspaceRef, "workspaceRef");
        require(missionLearningSourceRef, "missionLearningSourceRef");
        routingDecision = Objects.requireNonNull(routingDecision, "routingDecision is required");
        proposal = Objects.requireNonNull(proposal, "proposal is required");
        governedKnowledge = Objects.requireNonNull(governedKnowledge, "governedKnowledge is required");
        captureReceipt = Objects.requireNonNull(captureReceipt, "captureReceipt is required");
        retrievedKnowledge = List.copyOf(retrievedKnowledge == null ? List.of() : retrievedKnowledge);
        if (!workspaceRef.equals(proposal.workspaceRef())) {
            throw new IllegalArgumentException("proposal workspaceRef does not match lifecycle workspaceRef");
        }
        if (!workspaceRef.equals(routingDecision.workspaceRef())) {
            throw new IllegalArgumentException("routing workspaceRef does not match lifecycle workspaceRef");
        }
        if (!retrievedKnowledge.stream().allMatch(item -> workspaceRef.equals(item.workspaceRef()))) {
            throw new IllegalArgumentException("retrieved knowledge crossed WorkspaceKnowledge boundaries");
        }
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}

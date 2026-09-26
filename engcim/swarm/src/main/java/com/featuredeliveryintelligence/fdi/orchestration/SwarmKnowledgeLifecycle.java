package com.featuredeliveryintelligence.fdi.orchestration;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The single Swarm-owned path from Mission learning material to governed,
 * workspace-scoped knowledge and later retrieval.
 */
public final class SwarmKnowledgeLifecycle {
    private final SwarmKnowledgeGateway gateway;
    private final WorkspaceKnowledgeRepository repository;

    public SwarmKnowledgeLifecycle(SwarmKnowledgeGateway gateway, WorkspaceKnowledgeRepository repository) {
        this.gateway = Objects.requireNonNull(gateway, "gateway is required");
        this.repository = Objects.requireNonNull(repository, "repository is required");
    }

    public WorkspaceKnowledgeLifecycleResult buildAndPersist(
            MissionLearningSource source,
            LearningCandidate candidate,
            String governanceActor) {
        Objects.requireNonNull(source, "source is required");
        KnowledgeRoutingDecision routing = gateway.route(source, candidate);
        if (routing.route() != KnowledgeRoute.WORKSPACE_SEMANTIC
                && routing.route() != KnowledgeRoute.WORKSPACE_PROCEDURAL) {
            throw new RuntimeContractException(
                    "only WorkspaceKnowledge routes may enter the WorkspaceKnowledge lifecycle: " + routing.route());
        }

        WorkspaceKnowledgeProposal proposal = gateway.propose(source, candidate);
        GovernedWorkspaceKnowledge governed = gateway.govern(
                proposal, KnowledgeGovernanceDecision.APPROVED, governanceActor);
        gateway.persist(governed, repository);
        List<WorkspaceKnowledgeProposal> retrieved = gateway.retrieve(source.workspaceRef(), repository);
        Optional<WorkspaceKnowledgeCaptureResult> capture = repository instanceof WorkspaceKnowledgeCaptureReceiptRepository receipts
                ? receipts.captureFor(proposal.proposalRef())
                : Optional.empty();

        return new WorkspaceKnowledgeLifecycleResult(
                source.workspaceRef(), source.learningSourceRef(), routing, proposal, governed, capture, retrieved);
    }
}

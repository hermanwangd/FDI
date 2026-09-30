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
            KnowledgeGovernanceDecision decision,
            String decisionRef,
            String governanceActor,
            String policyRef,
            String decidedAt,
            List<String> decisionEvidenceRefs) {
        Objects.requireNonNull(source, "source is required");
        Objects.requireNonNull(decision, "governance decision is required");
        KnowledgeRoutingDecision routing = gateway.route(source, candidate);
        if (routing.route() != KnowledgeRoute.WORKSPACE_SEMANTIC
                && routing.route() != KnowledgeRoute.WORKSPACE_PROCEDURAL) {
            throw new RuntimeContractException(
                    "only WorkspaceKnowledge routes may enter the WorkspaceKnowledge lifecycle: " + routing.route());
        }

        WorkspaceKnowledgeProposal proposal = gateway.propose(source, candidate);
        GovernedWorkspaceKnowledge governed = gateway.govern(
                proposal, decision, decisionRef, governanceActor, policyRef, decidedAt, decisionEvidenceRefs);
        if (decision != KnowledgeGovernanceDecision.APPROVED) {
            return new WorkspaceKnowledgeLifecycleResult(
                    source.workspaceRef(), source.learningSourceRef(), routing, proposal, governed,
                    Optional.empty(), List.of());
        }

        gateway.persist(governed, repository);
        WorkspaceKnowledgeRepository.ReadResult readback = gateway.readAfterWrite(source.workspaceRef(), repository);
        List<WorkspaceKnowledgeRepository.Entry> matching = readback.entries().stream()
                .filter(item -> proposal.proposalRef().equals(item.recordKey()))
                .toList();
        if (matching.size() != 1 || !proposal.equals(matching.get(0).proposal())) {
            throw new RuntimeContractException(
                    "WorkspaceKnowledge read-after-write did not return exactly one matching proposal: "
                            + proposal.proposalRef());
        }
        Optional<WorkspaceKnowledgeCaptureResult> capture = repository instanceof WorkspaceKnowledgeCaptureReceiptRepository receipts
                ? receipts.captureFor(proposal.proposalRef())
                : Optional.empty();

        return new WorkspaceKnowledgeLifecycleResult(
                source.workspaceRef(), source.learningSourceRef(), routing, proposal, governed, capture,
                readback.entries().stream().map(WorkspaceKnowledgeRepository.Entry::proposal).toList());
    }
}

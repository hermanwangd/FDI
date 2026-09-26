package com.featuredeliveryintelligence.fdi.orchestration;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import java.util.Objects;

/** Swarm Core facade for classification and proposal, without a new learning service. */
public final class SwarmKnowledgeGateway {
    public KnowledgeRoute classify(LearningCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate is required");
        if (candidate.route() == KnowledgeRoute.DIRECT_TKMS_PUBLICATION) {
            throw new RuntimeContractException("direct Mission/Swarm-to-tKMS publication is prohibited");
        }
        return candidate.route();
    }

    public WorkspaceKnowledgeProposal propose(MissionLearningSource source, LearningCandidate candidate) {
        return propose(source.workspaceRef(), source, candidate);
    }

    public WorkspaceKnowledgeProposal propose(
            String targetWorkspaceRef, MissionLearningSource source, LearningCandidate candidate) {
        Objects.requireNonNull(source, "source is required");
        if (targetWorkspaceRef == null || targetWorkspaceRef.isBlank()) {
            throw new IllegalArgumentException("targetWorkspaceRef is required");
        }
        if (!targetWorkspaceRef.equals(source.workspaceRef())) {
            throw new RuntimeContractException("learning source workspaceRef does not match target workspace");
        }
        KnowledgeRoute route = classify(candidate);
        if (route == KnowledgeRoute.PRODUCT_KNOWLEDGE_PROPOSAL) {
            throw new RuntimeContractException("Product truth must remain a Product Knowledge proposal");
        }
        KnowledgeType type = switch (route) {
            case WORKSPACE_SEMANTIC -> KnowledgeType.SEMANTIC;
            case WORKSPACE_PROCEDURAL -> KnowledgeType.PROCEDURAL;
            default -> throw new RuntimeContractException("route does not create WorkspaceKnowledge: " + route);
        };
        return new WorkspaceKnowledgeProposal(
                "proposal:" + source.learningSourceRef(),
                source.workspaceRef(),
                source.sourceRefs(),
                type,
                candidate.statement(),
                candidate.scope(),
                candidate.applicability(),
                candidate.limitations(),
                source.evidenceRefs(),
                candidate.conflictRefs());
    }
}

package com.featuredeliveryintelligence.fdi.orchestration;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic local repository used for contract tests; production storage remains governed. */
public final class InMemoryWorkspaceKnowledgeRepository implements WorkspaceKnowledgeRepository {
    private final Map<String, Map<String, WorkspaceKnowledgeProposal>> byWorkspace = new LinkedHashMap<>();

    @Override
    public void save(GovernedWorkspaceKnowledge knowledge) {
        if (knowledge == null || knowledge.decision() != KnowledgeGovernanceDecision.APPROVED) {
            throw new RuntimeContractException("only approved WorkspaceKnowledge may be persisted");
        }
        WorkspaceKnowledgeProposal proposal = knowledge.proposal();
        byWorkspace.computeIfAbsent(proposal.workspaceRef(), ignored -> new LinkedHashMap<>())
                .put(proposal.proposalRef(), proposal);
    }

    @Override
    public List<WorkspaceKnowledgeProposal> findByWorkspace(String workspaceRef) {
        if (workspaceRef == null || workspaceRef.isBlank()) throw new IllegalArgumentException("workspaceRef is required");
        return List.copyOf(new ArrayList<>(byWorkspace.getOrDefault(workspaceRef, Map.of()).values()));
    }
}

package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

/** Persistence/retrieval boundary for approved WorkspaceKnowledge only. */
public interface WorkspaceKnowledgeRepository {
    void save(GovernedWorkspaceKnowledge knowledge);

    List<WorkspaceKnowledgeProposal> findByWorkspace(String workspaceRef);
}

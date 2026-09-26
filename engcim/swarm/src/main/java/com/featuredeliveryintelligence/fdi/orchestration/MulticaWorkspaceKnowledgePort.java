package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

/**
 * Provider-neutral durable boundary for the external Multica WorkspaceKnowledge
 * project. CLI syntax and record primitives remain outside the ENGCIM semantic
 * layer and must be discovered/implemented by the runtime adapter.
 */
public interface MulticaWorkspaceKnowledgePort {
    WorkspaceKnowledgeCaptureResult persist(
            WorkspaceKnowledgeProjectRef project,
            GovernedWorkspaceKnowledge knowledge);

    List<WorkspaceKnowledgeProposal> retrieve(WorkspaceKnowledgeProjectRef project);
}

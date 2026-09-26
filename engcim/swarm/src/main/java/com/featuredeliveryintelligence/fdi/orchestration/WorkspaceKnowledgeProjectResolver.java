package com.featuredeliveryintelligence.fdi.orchestration;

/** Resolves the one logical WorkspaceKnowledge project for a Multica workspace. */
@FunctionalInterface
public interface WorkspaceKnowledgeProjectResolver {
    WorkspaceKnowledgeProjectRef resolve(String workspaceRef);
}

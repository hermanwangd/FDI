package com.featuredeliveryintelligence.fdi.orchestration;

/** Resolved Multica project target for one workspace's governed learning. */
public record WorkspaceKnowledgeProjectRef(
        String workspaceRef,
        String projectRef,
        String projectName) {
    public WorkspaceKnowledgeProjectRef {
        require(workspaceRef, "workspaceRef");
        require(projectRef, "projectRef");
        require(projectName, "projectName");
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}

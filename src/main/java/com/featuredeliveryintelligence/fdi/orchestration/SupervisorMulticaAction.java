package com.featuredeliveryintelligence.fdi.orchestration;

public record SupervisorMulticaAction(
        String actionRef,
        String workspaceRef,
        SupervisorMulticaActionType type,
        String details) {
    public SupervisorMulticaAction {
        if (actionRef == null || actionRef.isBlank()) throw new IllegalArgumentException("actionRef is required");
        if (workspaceRef == null || workspaceRef.isBlank()) throw new IllegalArgumentException("workspaceRef is required");
        if (type == null) throw new IllegalArgumentException("action type is required");
        details = details == null ? "" : details;
    }
}

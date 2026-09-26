package com.featuredeliveryintelligence.fdi.orchestration;

public record WorkspaceRuntimeSnapshot(
        String workspaceRef,
        WorkspaceRuntimeState state,
        String activeVersion,
        String previousVersion,
        long lifecycleRevision) { }

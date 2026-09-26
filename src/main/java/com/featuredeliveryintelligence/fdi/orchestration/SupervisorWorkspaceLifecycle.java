package com.featuredeliveryintelligence.fdi.orchestration;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import java.util.Objects;

/** Operational workspace lifecycle; it cannot submit an engineering Mission. */
public final class SupervisorWorkspaceLifecycle {
    private final String workspaceRef;
    private final SupervisorMulticaBoundary boundary;
    private WorkspaceRuntimeSnapshot snapshot;

    public SupervisorWorkspaceLifecycle(String workspaceRef, SupervisorMulticaBoundary boundary) {
        if (workspaceRef == null || workspaceRef.isBlank()) throw new IllegalArgumentException("workspaceRef is required");
        this.workspaceRef = workspaceRef;
        this.boundary = Objects.requireNonNull(boundary, "boundary is required");
        this.snapshot = new WorkspaceRuntimeSnapshot(workspaceRef, WorkspaceRuntimeState.UNQUALIFIED, "", "", 0);
    }

    public WorkspaceRuntimeSnapshot snapshot() { return snapshot; }

    public WorkspaceRuntimeSnapshot bootstrap(String version) {
        requireVersion(version);
        requireState(WorkspaceRuntimeState.UNQUALIFIED);
        operational(SupervisorMulticaActionType.WORKSPACE_QUALIFICATION, "qualify");
        operational(SupervisorMulticaActionType.APPROVED_ACTIVATION, version);
        snapshot = new WorkspaceRuntimeSnapshot(workspaceRef, WorkspaceRuntimeState.ACTIVE, version, "", snapshot.lifecycleRevision() + 1);
        return snapshot;
    }

    public WorkspaceRuntimeSnapshot upgrade(String version, boolean approved) {
        requireVersion(version);
        if (!approved) throw new RuntimeContractException("runtime upgrade requires approved activation");
        requireState(WorkspaceRuntimeState.ACTIVE);
        operational(SupervisorMulticaActionType.RUNTIME_VERSION, version);
        operational(SupervisorMulticaActionType.APPROVED_ACTIVATION, version);
        snapshot = new WorkspaceRuntimeSnapshot(workspaceRef, WorkspaceRuntimeState.ACTIVE, version,
                snapshot.activeVersion(), snapshot.lifecycleRevision() + 1);
        return snapshot;
    }

    public WorkspaceRuntimeSnapshot rollback() {
        requireState(WorkspaceRuntimeState.ACTIVE);
        if (snapshot.previousVersion().isBlank()) throw new RuntimeContractException("no previous runtime version is available");
        operational(SupervisorMulticaActionType.ROLLBACK, snapshot.previousVersion());
        snapshot = new WorkspaceRuntimeSnapshot(workspaceRef, WorkspaceRuntimeState.ACTIVE,
                snapshot.previousVersion(), snapshot.activeVersion(), snapshot.lifecycleRevision() + 1);
        return snapshot;
    }

    public WorkspaceRuntimeSnapshot pause() {
        requireState(WorkspaceRuntimeState.ACTIVE);
        operational(SupervisorMulticaActionType.PAUSE, "pause");
        snapshot = new WorkspaceRuntimeSnapshot(workspaceRef, WorkspaceRuntimeState.PAUSED,
                snapshot.activeVersion(), snapshot.previousVersion(), snapshot.lifecycleRevision() + 1);
        return snapshot;
    }

    public WorkspaceRuntimeSnapshot resume() {
        requireState(WorkspaceRuntimeState.PAUSED);
        operational(SupervisorMulticaActionType.RESUME, "resume");
        snapshot = new WorkspaceRuntimeSnapshot(workspaceRef, WorkspaceRuntimeState.ACTIVE,
                snapshot.activeVersion(), snapshot.previousVersion(), snapshot.lifecycleRevision() + 1);
        return snapshot;
    }

    private void operational(SupervisorMulticaActionType type, String details) {
        boundary.execute(new SupervisorMulticaAction(
                "workspace-runtime:" + workspaceRef + ":" + (snapshot.lifecycleRevision() + 1) + ":" + type,
                workspaceRef, type, details));
    }

    private void requireState(WorkspaceRuntimeState expected) {
        if (snapshot.state() != expected) throw new RuntimeContractException("workspace runtime must be " + expected);
    }

    private static void requireVersion(String version) {
        if (version == null || version.isBlank()) throw new IllegalArgumentException("runtime version is required");
    }
}

package com.featuredeliveryintelligence.fdi.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SupervisorLifecycleTests {
    @Test
    void bootstrapUpgradeRollbackAndPauseResumeUseOperationalMulticaOnly() {
        var actions = new ArrayList<SupervisorMulticaAction>();
        SupervisorMulticaBoundary boundary = new SupervisorMulticaBoundary(action -> {
            actions.add(action);
            return new OperationalReceipt(action.actionRef(), "OK");
        });
        SupervisorWorkspaceLifecycle lifecycle = new SupervisorWorkspaceLifecycle("workspace-a", boundary);

        assertThat(lifecycle.bootstrap("multica-1").state()).isEqualTo(WorkspaceRuntimeState.ACTIVE);
        assertThat(lifecycle.upgrade("multica-2", true).previousVersion()).isEqualTo("multica-1");
        assertThat(lifecycle.rollback().activeVersion()).isEqualTo("multica-1");
        assertThat(lifecycle.pause().state()).isEqualTo(WorkspaceRuntimeState.PAUSED);
        assertThat(lifecycle.resume().state()).isEqualTo(WorkspaceRuntimeState.ACTIVE);

        assertThat(actions).extracting(SupervisorMulticaAction::type)
                .containsExactly(
                        SupervisorMulticaActionType.WORKSPACE_QUALIFICATION,
                        SupervisorMulticaActionType.APPROVED_ACTIVATION,
                        SupervisorMulticaActionType.RUNTIME_VERSION,
                        SupervisorMulticaActionType.APPROVED_ACTIVATION,
                        SupervisorMulticaActionType.ROLLBACK,
                        SupervisorMulticaActionType.PAUSE,
                        SupervisorMulticaActionType.RESUME);
    }

    @Test
    void unapprovedUpgradeIsRejectedBeforeOperationalDispatch() {
        var actions = new ArrayList<SupervisorMulticaAction>();
        SupervisorWorkspaceLifecycle lifecycle = new SupervisorWorkspaceLifecycle("workspace-a",
                new SupervisorMulticaBoundary(action -> {
                    actions.add(action);
                    return new OperationalReceipt(action.actionRef(), "OK");
                }));
        lifecycle.bootstrap("multica-1");

        assertThatThrownBy(() -> lifecycle.upgrade("multica-2", false)).hasMessageContaining("approved");
        assertThat(actions).hasSize(2);
    }
}

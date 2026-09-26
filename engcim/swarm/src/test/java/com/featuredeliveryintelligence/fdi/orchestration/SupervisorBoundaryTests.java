package com.featuredeliveryintelligence.fdi.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class SupervisorBoundaryTests {
    @Test
    void T04_supervisorEngineeringDispatchIsRejected() {
        var calls = new ArrayList<SupervisorMulticaAction>();
        var boundary = new SupervisorMulticaBoundary(action -> {
            calls.add(action);
            return new OperationalReceipt(action.actionRef(), "OK");
        });

        assertThatThrownBy(() -> boundary.execute(new SupervisorMulticaAction(
                "action-1", "workspace-a", SupervisorMulticaActionType.ENGINEERING_EXECUTION, "edit code")))
                .hasMessageContaining("prohibited");
        assertThat(calls).isEmpty();
    }

    @Test
    void T05_supervisorOperationalMulticaActionIsAllowed() {
        var calls = new ArrayList<SupervisorMulticaAction>();
        var boundary = new SupervisorMulticaBoundary(action -> {
            calls.add(action);
            return new OperationalReceipt(action.actionRef(), "OK");
        });

        OperationalReceipt receipt = boundary.execute(new SupervisorMulticaAction(
                "action-2", "workspace-a", SupervisorMulticaActionType.HEALTH_STATUS, "read status"));

        assertThat(receipt.status()).isEqualTo("OK");
        assertThat(calls).singleElement().extracting(SupervisorMulticaAction::type)
                .isEqualTo(SupervisorMulticaActionType.HEALTH_STATUS);
    }

    @Test
    void supervisorCannotUseOperationalPortForEngineeringExecution() {
        assertThat(SupervisorMulticaActionType.values())
                .contains(SupervisorMulticaActionType.ENGINEERING_EXECUTION);
    }
}

package com.featuredeliveryintelligence.fdi.orchestration;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import java.util.Objects;

/** Claude Supervisor may use operational Multica controls, never engineering execution. */
public final class SupervisorMulticaBoundary {
    private final OperationalMulticaPort operationalPort;

    public SupervisorMulticaBoundary(OperationalMulticaPort operationalPort) {
        this.operationalPort = Objects.requireNonNull(operationalPort, "operationalPort is required");
    }

    public OperationalReceipt execute(SupervisorMulticaAction action) {
        Objects.requireNonNull(action, "action is required");
        if (action.type() == SupervisorMulticaActionType.ENGINEERING_EXECUTION) {
            throw new RuntimeContractException(
                    "Supervisor-to-Multica engineering execution is prohibited; submit a Mission to Swarm");
        }
        return operationalPort.execute(action);
    }
}

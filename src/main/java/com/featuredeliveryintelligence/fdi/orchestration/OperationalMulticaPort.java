package com.featuredeliveryintelligence.fdi.orchestration;

@FunctionalInterface
public interface OperationalMulticaPort {
    OperationalReceipt execute(SupervisorMulticaAction action);
}

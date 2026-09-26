package com.featuredeliveryintelligence.fdi.orchestration;

/** External Multica execution port. A live client is supplied by the execution environment. */
@FunctionalInterface
public interface MulticaExecutionPort {
    BindingReceipt execute(MissionExecutionEnvelope execution);
}

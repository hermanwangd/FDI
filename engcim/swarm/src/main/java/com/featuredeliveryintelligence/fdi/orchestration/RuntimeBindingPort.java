package com.featuredeliveryintelligence.fdi.orchestration;

/** Provider-neutral Runtime Binding port; a real Multica adapter belongs outside this contract. */
@FunctionalInterface
public interface RuntimeBindingPort {
    BindingReceipt execute(MissionExecutionEnvelope execution);
}

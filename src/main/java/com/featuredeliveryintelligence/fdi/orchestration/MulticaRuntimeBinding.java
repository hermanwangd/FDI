package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.Objects;

/** Runtime Binding adapter that is the only engineering path to a Multica execution port. */
public final class MulticaRuntimeBinding implements RuntimeBindingPort {
    private final String bindingRef;
    private final MulticaExecutionPort multica;

    public MulticaRuntimeBinding(String bindingRef, MulticaExecutionPort multica) {
        if (bindingRef == null || bindingRef.isBlank()) throw new IllegalArgumentException("bindingRef is required");
        this.bindingRef = bindingRef;
        this.multica = Objects.requireNonNull(multica, "multica is required");
    }

    @Override
    public BindingReceipt execute(MissionExecutionEnvelope execution) {
        BindingReceipt receipt = multica.execute(Objects.requireNonNull(execution, "execution is required"));
        if (!bindingRef.equals(receipt.runtimeBindingRef())) {
            throw new IllegalStateException("Multica receipt does not match Runtime Binding");
        }
        return receipt;
    }
}

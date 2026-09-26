package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

public record BindingReceipt(
        String runtimeBindingRef,
        String multicaExecutionRef,
        String executionRevision,
        String executionStatus,
        List<String> evidenceRefs) {
    public BindingReceipt {
        if (runtimeBindingRef == null || runtimeBindingRef.isBlank()) throw new IllegalArgumentException("runtimeBindingRef is required");
        if (multicaExecutionRef == null || multicaExecutionRef.isBlank()) throw new IllegalArgumentException("multicaExecutionRef is required");
        if (executionRevision == null || executionRevision.isBlank()) throw new IllegalArgumentException("executionRevision is required");
        if (executionStatus == null || executionStatus.isBlank()) throw new IllegalArgumentException("executionStatus is required");
        evidenceRefs = List.copyOf(evidenceRefs == null ? List.of() : evidenceRefs);
    }
}

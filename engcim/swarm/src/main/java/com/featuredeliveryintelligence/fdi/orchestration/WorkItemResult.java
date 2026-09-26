package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

/** Execution fact only. Verification and control outcomes use separate types. */
public record WorkItemResult(
        String missionRef,
        String requestRef,
        String workspaceRef,
        String runtimeBindingRef,
        String multicaExecutionRef,
        String executionRevision,
        String executionStatus,
        List<String> evidenceRefs) {
    public WorkItemResult {
        evidenceRefs = List.copyOf(evidenceRefs == null ? List.of() : evidenceRefs);
    }
}

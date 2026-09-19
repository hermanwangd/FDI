package com.featuredeliveryintelligence.fdi.engcim.control;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.List;

/**
 * v0.1 binding contract between Scenario evidence and one Engineering Control.
 * Runtime scheduling and issue state intentionally do not belong here.
 */
public record ControlEvidenceBinding(
        String bindingRef,
        String scopeRef,
        String gateRef,
        String controlRef,
        String subjectRef,
        List<String> evidenceRefs) {

    public ControlEvidenceBinding {
        require(bindingRef, "bindingRef");
        require(scopeRef, "scopeRef");
        require(gateRef, "gateRef");
        require(controlRef, "controlRef");
        require(subjectRef, "subjectRef");
        EngineeringControlCatalog.definition(controlRef);
        if (evidenceRefs == null || evidenceRefs.isEmpty()
                || evidenceRefs.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new RuntimeContractException("evidenceRefs must contain nonblank values");
        }
        evidenceRefs = List.copyOf(evidenceRefs);
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new RuntimeContractException(field + " is required");
        }
    }
}

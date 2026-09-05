package com.featuredeliveryintelligence.fdi.product.realization;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.List;

public record RealizationProposal(
        String capabilityId,
        Outcome outcome,
        String sourceRevision,
        List<RealizationComponent> components,
        List<String> limitations) {

    public enum Outcome {
        MAPPING_PROPOSAL,
        UNRESOLVED
    }

    public RealizationProposal {
        if (capabilityId == null || capabilityId.isBlank()) {
            throw new RuntimeContractException("capabilityId is required");
        }
        if (outcome == null) {
            throw new RuntimeContractException("outcome is required");
        }
        if (sourceRevision == null || !sourceRevision.matches("[0-9a-f]{40}")) {
            throw new RuntimeContractException("sourceRevision must be a full lowercase Git SHA");
        }
        if (components == null) {
            throw new RuntimeContractException("components are required");
        }
        if (limitations == null) {
            throw new RuntimeContractException("limitations are required");
        }

        boolean hasPrimary = false;
        for (RealizationComponent component : components) {
            if (component == null) {
                throw new RuntimeContractException("components cannot contain null elements");
            }
            if (!sourceRevision.equals(component.identity().sourceRevision())) {
                throw new RuntimeContractException("component revision mismatch");
            }
            hasPrimary |= component.role() == RealizationComponent.Role.PRIMARY;
        }
        if (limitations.isEmpty()) {
            throw new RuntimeContractException("limitations must not be empty");
        }
        for (String limitation : limitations) {
            if (limitation == null || limitation.isBlank()) {
                throw new RuntimeContractException("limitations must be nonblank");
            }
        }
        if (outcome == Outcome.MAPPING_PROPOSAL && !hasPrimary) {
            throw new RuntimeContractException("mapping proposal requires PRIMARY component");
        }
        if (outcome == Outcome.UNRESOLVED && !components.isEmpty()) {
            throw new RuntimeContractException("unresolved proposal cannot contain components");
        }

        components = List.copyOf(components);
        limitations = List.copyOf(limitations);
    }
}

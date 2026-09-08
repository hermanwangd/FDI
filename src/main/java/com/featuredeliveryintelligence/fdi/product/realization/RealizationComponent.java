package com.featuredeliveryintelligence.fdi.product.realization;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

public record RealizationComponent(
        Role role,
        StructuralComponentIdentity identity,
        String selectionReason) {

    public enum Role {
        PRIMARY,
        SUPPORTING
    }

    public RealizationComponent {
        if (role == null) {
            throw new RuntimeContractException("role is required");
        }
        if (identity == null) {
            throw new RuntimeContractException("identity is required");
        }
        if (selectionReason == null || selectionReason.isBlank()) {
            throw new RuntimeContractException("selectionReason is required");
        }
    }
}

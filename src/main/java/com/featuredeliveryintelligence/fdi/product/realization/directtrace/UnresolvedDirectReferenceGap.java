package com.featuredeliveryintelligence.fdi.product.realization.directtrace;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

/** An extractor-declared unresolved reference, retained without inference. */
public record UnresolvedDirectReferenceGap(
        String observationRef, String referenceText, TraceSourceLocation sourceLocation, String kind) {
    public UnresolvedDirectReferenceGap {
        if (observationRef == null || !observationRef.startsWith("/test_files/")
                || referenceText == null || referenceText.isBlank()
                || sourceLocation == null || kind == null || kind.isBlank()) {
            throw new RuntimeContractException("invalid unresolved direct-reference gap");
        }
    }
}

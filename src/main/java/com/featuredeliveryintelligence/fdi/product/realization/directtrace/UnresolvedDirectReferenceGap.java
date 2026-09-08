package com.featuredeliveryintelligence.fdi.product.realization.directtrace;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import java.util.regex.Pattern;

/** An extractor-declared unresolved reference, retained without inference. */
public record UnresolvedDirectReferenceGap(
        String observationRef, String referenceText, TraceSourceLocation sourceLocation, String kind) {
    private static final Pattern FORBIDDEN = Pattern.compile(
            "(?i)(evaluator(?:[ _/-]+gold)?|gold[ _-]+mapping|ground[ _-]+truth|expected[ _-]+mapping)");

    public UnresolvedDirectReferenceGap {
        if (observationRef == null || !observationRef.startsWith("/test_files/")
                || referenceText == null || referenceText.isBlank()
                || sourceLocation == null || kind == null || kind.isBlank()) {
            throw new RuntimeContractException("invalid unresolved direct-reference gap");
        }
        if (FORBIDDEN.matcher(referenceText).find() || FORBIDDEN.matcher(kind).find()) {
            throw new RuntimeContractException("unresolved gap contains evaluator-only vocabulary");
        }
    }
}

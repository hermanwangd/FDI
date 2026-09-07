package com.featuredeliveryintelligence.fdi.testbehavior.api;

/**
 * A reference observed in test source that the extractor could not resolve
 * against the configured source roots. Retained with its exact source
 * location as an explicit evidence gap.
 */
public record UnresolvedReference(String referenceText, SourceLocation location, UnresolvedKind kind) {
    public UnresolvedReference {
        referenceText = TestBehaviorValidation.required(
                referenceText, TestBehaviorErrorCode.INVALID_EVIDENCE, "unresolved reference text");
        if (location == null) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.INVALID_EVIDENCE, "unresolved reference location must not be null");
        if (kind == null) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.INVALID_EVIDENCE, "unresolved reference kind must not be null");
    }
}

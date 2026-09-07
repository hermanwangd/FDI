package com.featuredeliveryintelligence.fdi.testbehavior.api;

/**
 * A production symbol referenced from a test observation. Mechanical only:
 * the declaring type and symbol name come from source resolution against the
 * configured source roots, never from inference.
 */
public record ReferencedProductionSymbol(
        ReferenceKind kind,
        String declaringType,
        String symbolName,
        RelationshipBasis basis) {
    public ReferencedProductionSymbol {
        if (kind == null) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.INVALID_EVIDENCE, "reference kind must not be null");
        declaringType = TestBehaviorValidation.required(
                declaringType, TestBehaviorErrorCode.INVALID_EVIDENCE, "declaring type");
        symbolName = TestBehaviorValidation.required(
                symbolName, TestBehaviorErrorCode.INVALID_EVIDENCE, "symbol name");
        if (basis == null) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.INVALID_EVIDENCE, "relationship basis must not be null");
    }
}

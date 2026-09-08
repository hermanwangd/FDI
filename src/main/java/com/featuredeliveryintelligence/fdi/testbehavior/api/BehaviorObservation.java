package com.featuredeliveryintelligence.fdi.testbehavior.api;

import java.util.Optional;

/**
 * One mechanical observation inside a test method: the observed expression
 * text, its source location, and the production symbol it references when
 * resolution succeeded. When resolution did not succeed the observation stays
 * syntactic and the matching gap is recorded separately as an
 * {@link UnresolvedReference}; this record never carries an invented link.
 */
public record BehaviorObservation(
        BehaviorKind kind,
        String observedExpression,
        SourceLocation location,
        Optional<ReferencedProductionSymbol> referencedSymbol) {
    public BehaviorObservation {
        if (kind == null) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.INVALID_EVIDENCE, "behavior kind must not be null");
        observedExpression = TestBehaviorValidation.required(
                observedExpression, TestBehaviorErrorCode.INVALID_EVIDENCE, "observed expression");
        if (location == null) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.INVALID_EVIDENCE, "behavior observation location must not be null");
        referencedSymbol = referencedSymbol == null ? Optional.empty() : referencedSymbol;
    }
}

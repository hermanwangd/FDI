package com.featuredeliveryintelligence.fdi.testbehavior.api;

import java.util.Comparator;
import java.util.List;

/**
 * Mechanical observations for one {@code @Test}-bearing method: identity,
 * repository-relative declaration location, fixture/action/assertion
 * observations, and unresolved references. All lists are defensively copied
 * and deterministically ordered so repeated extraction over identical inputs
 * compares byte-equal.
 */
public record TestMethodObservation(
        String methodName,
        SourceLocation declarationLocation,
        List<BehaviorObservation> fixtures,
        List<BehaviorObservation> actions,
        List<BehaviorObservation> assertions,
        List<UnresolvedReference> unresolvedReferences) {
    public TestMethodObservation {
        methodName = TestBehaviorValidation.required(
                methodName, TestBehaviorErrorCode.INVALID_EVIDENCE, "test method name");
        if (declarationLocation == null) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.INVALID_EVIDENCE, "test method declaration location must not be null");
        fixtures = ordered(fixtures, "fixtures");
        actions = ordered(actions, "actions");
        assertions = ordered(assertions, "assertions");
        unresolvedReferences = ordered(unresolvedReferences, "unresolved references");
    }

    private static <T> List<T> ordered(List<T> values, String what) {
        if (values == null) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.INVALID_EVIDENCE, what + " must not be null");
        for (T value : values) if (value == null) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.INVALID_EVIDENCE, what + " must not contain null");
        return values.stream().sorted(Comparator.comparing(Object::toString)).toList();
    }
}

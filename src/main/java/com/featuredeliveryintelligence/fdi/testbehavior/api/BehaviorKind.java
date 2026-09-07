package com.featuredeliveryintelligence.fdi.testbehavior.api;

/**
 * Mechanical role of a behavior observation inside a test method.
 */
public enum BehaviorKind {
    /** Fixture or precondition evidence (for example a setup call). */
    FIXTURE,
    /** Exercised request, command, event, or method under test. */
    ACTION,
    /** Asserted result, error, state change, or invariant. */
    ASSERTION
}

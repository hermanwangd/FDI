package com.featuredeliveryintelligence.fdi.testbehavior.api;

/**
 * Mechanical basis on which a test-to-production relationship was observed.
 * The extractor records the basis; it never upgrades a basis into a stronger
 * claim and never fabricates a relationship edge.
 */
public enum RelationshipBasis {
    /** Resolved to a production type in the same package from a source root. */
    SAME_PACKAGE_SOURCE_ROOT,
    /** Resolved to a production type through an explicit import from a source root. */
    IMPORTED_SOURCE_ROOT
}

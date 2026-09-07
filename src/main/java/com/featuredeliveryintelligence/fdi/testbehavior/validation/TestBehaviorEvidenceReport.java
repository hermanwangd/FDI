package com.featuredeliveryintelligence.fdi.testbehavior.validation;

/**
 * Successful result of fail-closed evidence validation (PKB-BL-009 Slice C).
 * Carries only mechanical identity and count facts so callers can bind the
 * accepted evidence without re-parsing it. Any violation refuses validation
 * with a {@code TestBehaviorExtractionException} instead of producing a
 * report.
 */
public record TestBehaviorEvidenceReport(
        String schemaVersion,
        String canonicalRevision,
        int testFileCount,
        int testMethodCount,
        boolean incomplete) {

    public TestBehaviorEvidenceReport {
        if (schemaVersion == null || schemaVersion.isBlank())
            throw new IllegalArgumentException("schema version must not be blank");
        if (canonicalRevision == null || canonicalRevision.isBlank())
            throw new IllegalArgumentException("canonical revision must not be blank");
        if (testFileCount < 0 || testMethodCount < 0)
            throw new IllegalArgumentException("counts must not be negative");
    }
}

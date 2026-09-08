package com.featuredeliveryintelligence.fdi.testbehavior.api;

/**
 * Repository-relative source location of a mechanical observation.
 */
public record SourceLocation(String repositoryRelativePath, int line, int column) {
    public SourceLocation {
        repositoryRelativePath = TestBehaviorValidation.repositoryPath(
                repositoryRelativePath, TestBehaviorErrorCode.MALFORMED_REPOSITORY_PATH, "source location path");
        if (line < 1 || column < 1) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.INVALID_EVIDENCE, "source location line and column are 1-based: " + line + ":" + column);
    }
}

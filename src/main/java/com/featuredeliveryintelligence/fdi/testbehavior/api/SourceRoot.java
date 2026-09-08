package com.featuredeliveryintelligence.fdi.testbehavior.api;

/**
 * A repository source root offered to the extractor, relative to the bound
 * repository revision. Roots are restricted to in-repository locations;
 * absolute or traversal paths fail closed (PKB-BL-009: escaped source roots
 * fail closed).
 */
public record SourceRoot(SourceRootKind kind, String repositoryRelativePath) {
    public enum SourceRootKind {
        PRODUCTION,
        TEST
    }

    public SourceRoot {
        if (kind == null) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.MISSING_SOURCE_ROOT, "source root kind must not be null");
        repositoryRelativePath = TestBehaviorValidation.repositoryPath(
                repositoryRelativePath, TestBehaviorErrorCode.ESCAPED_SOURCE_ROOT, "source root path");
    }
}

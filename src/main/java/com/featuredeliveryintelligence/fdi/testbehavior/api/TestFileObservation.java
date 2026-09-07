package com.featuredeliveryintelligence.fdi.testbehavior.api;

import java.util.Comparator;
import java.util.List;

/**
 * Mechanical observations for one test file at the bound revision: identity,
 * frozen input digest, test class name, per-method observations, and any
 * unresolved references recorded at file scope. Test methods are
 * deterministically ordered by declaration location.
 */
public record TestFileObservation(
        String repositoryRelativePath,
        String inputDigest,
        String testClassName,
        boolean nestedContainer,
        List<TestMethodObservation> testMethods,
        List<UnresolvedReference> unresolvedReferences) {
    public TestFileObservation {
        repositoryRelativePath = TestBehaviorValidation.repositoryPath(
                repositoryRelativePath, TestBehaviorErrorCode.MALFORMED_REPOSITORY_PATH, "test file path");
        inputDigest = TestBehaviorValidation.hexDigest(inputDigest, repositoryRelativePath);
        testClassName = TestBehaviorValidation.required(
                testClassName, TestBehaviorErrorCode.INVALID_EVIDENCE, "test class name");
        testMethods = TestBehaviorValidation.requiredNonEmpty(
                testMethods, TestBehaviorErrorCode.INVALID_EVIDENCE, "test methods");
        testMethods = testMethods.stream()
                .sorted(Comparator.comparing(TestMethodObservation::declarationLocation,
                        Comparator.comparing(SourceLocation::repositoryRelativePath)
                                .thenComparingInt(SourceLocation::line)
                                .thenComparingInt(SourceLocation::column)))
                .toList();
        if (unresolvedReferences == null) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.INVALID_EVIDENCE, "unresolved references must not be null");
        unresolvedReferences = List.copyOf(unresolvedReferences);
    }
}

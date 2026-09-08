package com.featuredeliveryintelligence.fdi.testbehavior.api;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extraction result envelope. Binds provider provenance, exact source
 * revision, and frozen input digests to the per-file mechanical observations.
 * Test files are deterministically ordered by repository-relative path and
 * duplicate test identities fail closed. {@code incomplete} stays visible so
 * extraction limitations are never silently upgraded into complete evidence.
 */
public record TestBehaviorExtractionResult(
        ExtractionProvenance provenance,
        String repositoryId,
        String canonicalRevision,
        Map<String, String> inputDigests,
        List<TestFileObservation> testFiles,
        boolean incomplete,
        List<String> diagnostics) {
    public TestBehaviorExtractionResult {
        if (provenance == null) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.PROVIDER_BINDING_MISSING, "extraction provenance must not be null");
        repositoryId = TestBehaviorValidation.required(
                repositoryId, TestBehaviorErrorCode.PROVIDER_BINDING_MISSING, "repository id");
        canonicalRevision = TestBehaviorValidation.canonicalRevision(canonicalRevision);
        if (inputDigests == null || inputDigests.isEmpty())
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.DIGEST_MISMATCH, "frozen input digests must not be empty");
        inputDigests = Map.copyOf(inputDigests);
        testFiles = TestBehaviorValidation.requiredNonEmpty(
                testFiles, TestBehaviorErrorCode.INVALID_EVIDENCE, "test files");
        testFiles = testFiles.stream()
                .sorted(Comparator.comparing(TestFileObservation::repositoryRelativePath))
                .toList();
        Set<String> identities = new HashSet<>();
        for (TestFileObservation file : testFiles) {
            for (TestMethodObservation method : file.testMethods()) {
                String identity = file.repositoryRelativePath() + "#" + method.methodName();
                if (!identities.add(identity)) throw new TestBehaviorExtractionException(
                        TestBehaviorErrorCode.DUPLICATE_TEST_IDENTITY, "duplicate test identity: " + identity);
            }
        }
        if (diagnostics == null) throw new TestBehaviorExtractionException(
                TestBehaviorErrorCode.INVALID_EVIDENCE, "diagnostics must not be null");
        diagnostics = List.copyOf(diagnostics);
    }
}

package com.featuredeliveryintelligence.fdi.testbehavior.api;

import java.util.List;
import java.util.Map;

/**
 * Provider-neutral extraction request. Binds the exact source revision,
 * restricted source roots, frozen per-file input digests, and the evidence
 * schema version. Anything absent, inconsistent, or out of scope fails closed.
 */
public record TestBehaviorExtractionRequest(
        String repositoryId,
        String canonicalRevision,
        List<SourceRoot> sourceRoots,
        Map<String, String> inputDigests,
        String schemaVersion) {

    /** Schema version this contract revision supports. */
    public static final String SUPPORTED_SCHEMA_VERSION = "1";

    public TestBehaviorExtractionRequest {
        repositoryId = TestBehaviorValidation.required(
                repositoryId, TestBehaviorErrorCode.PROVIDER_BINDING_MISSING, "repository id");
        canonicalRevision = TestBehaviorValidation.canonicalRevision(canonicalRevision);
        sourceRoots = TestBehaviorValidation.requiredNonEmpty(
                sourceRoots, TestBehaviorErrorCode.MISSING_SOURCE_ROOT, "source roots");
        if (inputDigests == null || inputDigests.isEmpty())
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.DIGEST_MISMATCH, "frozen input digests must not be empty");
        for (Map.Entry<String, String> entry : inputDigests.entrySet()) {
            TestBehaviorValidation.repositoryPath(
                    entry.getKey(), TestBehaviorErrorCode.MALFORMED_REPOSITORY_PATH, "digest key");
            TestBehaviorValidation.hexDigest(entry.getValue(), entry.getKey());
        }
        inputDigests = Map.copyOf(inputDigests);
        schemaVersion = TestBehaviorValidation.supportedSchemaVersion(schemaVersion);
    }
}

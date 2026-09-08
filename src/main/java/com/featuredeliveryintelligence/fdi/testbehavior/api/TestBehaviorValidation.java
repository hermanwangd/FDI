package com.featuredeliveryintelligence.fdi.testbehavior.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared fail-closed validation for the test-behavior contract records. The
 * contract package stays records/interfaces only; this package-private helper
 * centralizes the mechanical checks so every record enforces the same rules.
 */
final class TestBehaviorValidation {
    private TestBehaviorValidation() {}

    static String required(String value, TestBehaviorErrorCode code, String what) {
        if (value == null || value.isBlank()) throw new TestBehaviorExtractionException(code, what + " must not be blank");
        return value;
    }

    static String canonicalRevision(String revision) {
        required(revision, TestBehaviorErrorCode.MISSING_SOURCE_REVISION, "canonical revision");
        if (!revision.matches("(?i)[0-9a-f]{40}|[0-9a-f]{64}"))
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.INVALID_SOURCE_REVISION, "canonical revision must be a full Git object id");
        return revision;
    }

    /** Repository-relative paths must be relative, normalized, and traversal-free. */
    static String repositoryPath(String path, TestBehaviorErrorCode code, String what) {
        required(path, code, what);
        if (path.startsWith("/") || path.startsWith("\\") || path.contains(":") || path.contains("\\"))
            throw new TestBehaviorExtractionException(code, what + " must be a repository-relative path: " + path);
        List<String> segments = new ArrayList<>(List.of(path.split("/")));
        if (segments.stream().anyMatch(s -> s.isEmpty() || s.equals(".") || s.equals("..")))
            throw new TestBehaviorExtractionException(code, what + " must be normalized and traversal-free: " + path);
        return path;
    }

    static <T> List<T> requiredNonEmpty(List<T> values, TestBehaviorErrorCode code, String what) {
        if (values == null || values.isEmpty())
            throw new TestBehaviorExtractionException(code, what + " must not be empty");
        for (T value : values) if (value == null) throw new TestBehaviorExtractionException(code, what + " must not contain null");
        return List.copyOf(values);
    }

    static String supportedSchemaVersion(String schemaVersion) {
        required(schemaVersion, TestBehaviorErrorCode.UNSUPPORTED_SCHEMA_VERSION, "schema version");
        if (!TestBehaviorExtractionRequest.SUPPORTED_SCHEMA_VERSION.equals(schemaVersion))
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.UNSUPPORTED_SCHEMA_VERSION,
                    "unsupported schema version: " + schemaVersion + " (supported: "
                            + TestBehaviorExtractionRequest.SUPPORTED_SCHEMA_VERSION + ")");
        return schemaVersion;
    }

    static String hexDigest(String digest, String key) {
        required(digest, TestBehaviorErrorCode.DIGEST_MISMATCH, "digest for " + key);
        if (!digest.toLowerCase(Locale.ROOT).matches("[0-9a-f]{32}|[0-9a-f]{40}|[0-9a-f]{64}"))
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.DIGEST_MISMATCH, "digest for " + key + " must be a hex digest");
        return digest;
    }
}

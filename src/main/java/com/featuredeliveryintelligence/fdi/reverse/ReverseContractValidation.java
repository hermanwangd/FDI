package com.featuredeliveryintelligence.fdi.reverse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared fail-closed validation for the reverse-proposal contract records.
 * Both {@code reverse.evidence} and {@code reverse.proposal} delegate to this
 * helper so every record enforces the same identity, digest, path, and schema
 * rules and raises the same {@link ReverseFailure} vocabulary.
 */
public final class ReverseContractValidation {
    private ReverseContractValidation() {}

    /** Non-blank text or {@link ReverseFailure#MISSING_INPUT}. */
    public static String requiredText(String value, String what) {
        if (value == null || value.isBlank())
            throw new ReverseContractException(ReverseFailure.MISSING_INPUT, what + " must not be blank");
        return value;
    }

    /**
     * Full Git object id (40 or 64 lowercase/uppercase hex), normalized to
     * lowercase. Absent revision is {@link ReverseFailure#MISSING_INPUT};
     * malformed revision is {@link ReverseFailure#REVISION_MISMATCH}.
     */
    public static String canonicalRevision(String revision) {
        if (revision == null || revision.isBlank())
            throw new ReverseContractException(ReverseFailure.MISSING_INPUT, "canonical revision must not be blank");
        if (!revision.matches("(?i)[0-9a-f]{40}|[0-9a-f]{64}"))
            throw new ReverseContractException(
                    ReverseFailure.REVISION_MISMATCH, "canonical revision must be a full Git object id: " + revision);
        return revision.toLowerCase(Locale.ROOT);
    }

    /** 64-hex SHA-256 digest, normalized to lowercase, or {@link ReverseFailure#DIGEST_MISMATCH}. */
    public static String sha256Hex(String digest, String what) {
        if (digest == null || digest.isBlank())
            throw new ReverseContractException(ReverseFailure.DIGEST_MISMATCH, "digest for " + what + " must not be blank");
        if (!digest.matches("(?i)[0-9a-f]{64}"))
            throw new ReverseContractException(
                    ReverseFailure.DIGEST_MISMATCH, "digest for " + what + " must be a SHA-256 hex digest");
        return digest.toLowerCase(Locale.ROOT);
    }

    /** Repository-relative path: relative, normalized, traversal-free, or {@link ReverseFailure#MALFORMED_PATH}. */
    public static String repositoryPath(String path, String what) {
        requiredText(path, what);
        if (path.startsWith("/") || path.startsWith("\\") || path.contains(":") || path.contains("\\"))
            throw new ReverseContractException(
                    ReverseFailure.MALFORMED_PATH, what + " must be a repository-relative path: " + path);
        List<String> segments = new ArrayList<>(List.of(path.split("/")));
        if (segments.stream().anyMatch(s -> s.isEmpty() || s.equals(".") || s.equals("..")))
            throw new ReverseContractException(
                    ReverseFailure.MALFORMED_PATH, what + " must be normalized and traversal-free: " + path);
        return path;
    }

    /** Exact schema-version match or {@link ReverseFailure#UNSUPPORTED_SCHEMA_VERSION}. */
    public static String supportedSchemaVersion(String schemaVersion, String supported) {
        if (schemaVersion == null || schemaVersion.isBlank() || !supported.equals(schemaVersion))
            throw new ReverseContractException(
                    ReverseFailure.UNSUPPORTED_SCHEMA_VERSION,
                    "unsupported schema version: " + schemaVersion + " (supported: " + supported + ")");
        return schemaVersion;
    }
}

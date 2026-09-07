package com.featuredeliveryintelligence.fdi.testbehavior.api;

/**
 * Stable error vocabulary for the provider-neutral test-behavior extractor
 * (PKB-BL-009 / PKB-REVERSE-002). Codes are mechanical and carry no Product
 * meaning; they exist so extraction, validation, and review can refer to one
 * fixed refusal taxonomy. Codes must never be renamed or reused for a
 * different semantics.
 */
public enum TestBehaviorErrorCode {
    /** Canonical source revision is absent. */
    MISSING_SOURCE_REVISION,
    /** Canonical source revision is not a full Git object id. */
    INVALID_SOURCE_REVISION,
    /** No source root was supplied for extraction. */
    MISSING_SOURCE_ROOT,
    /** A source root path escapes the repository (absolute or traversal). */
    ESCAPED_SOURCE_ROOT,
    /** A repository-relative path is blank, absolute, or contains traversal. */
    MALFORMED_REPOSITORY_PATH,
    /** A frozen input digest is absent or does not match the bound identity. */
    DIGEST_MISMATCH,
    /** Two extracted observations claim the same test identity. */
    DUPLICATE_TEST_IDENTITY,
    /** The evidence schema version is not supported by this contract. */
    UNSUPPORTED_SCHEMA_VERSION,
    /** Provider identity, provenance, or binding is absent. */
    PROVIDER_BINDING_MISSING,
    /** A mechanical observation is malformed or internally inconsistent. */
    INVALID_EVIDENCE
}

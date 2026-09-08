package com.featuredeliveryintelligence.fdi.reverse;

/**
 * Stable failure vocabulary for the deterministic reverse-proposal contract
 * (PKB-BL-009 / PKB-REVERSE-002, Slice A). Codes are mechanical and carry no
 * Product meaning; they exist so evidence binding, proposal validation, the
 * Slice E generator, and later evaluator comparison can refer to one fixed
 * refusal taxonomy. Codes must never be renamed or reused for different
 * semantics; later slices report refusal through this vocabulary only.
 */
public enum ReverseFailure {
    /** A required input (channel, text field, citation) is absent or blank. */
    MISSING_INPUT,
    /** A bound SHA-256 digest is absent, malformed, or fails recomputation. */
    DIGEST_MISMATCH,
    /** A canonical revision is absent or malformed, or channel-bound identities disagree on the revision. */
    REVISION_MISMATCH,
    /** A repository-relative input path is blank, absolute, non-normalized, or escapes via traversal. */
    MALFORMED_PATH,
    /** Two bound entities claim the same identity, or a channel repository identity disagrees with the bundle identity. */
    DUPLICATE_IDENTITY,
    /** The evidence or proposal schema version is not supported by this contract revision. */
    UNSUPPORTED_SCHEMA_VERSION,
    /** A single-channel observation cannot become a proposal; it must remain an explicit evidence gap (Slice E). */
    EVIDENCE_GAP,
    /** Proposal text, identifiers, citations, or digests cannot be traced to the bound evidence. */
    UNTRACEABLE_PROPOSAL,
    /** Generation consumed accepted Product Semantics, evaluator gold, review decisions, or comparison output. */
    EVALUATOR_LEAKAGE,
    /** A proposal claims authority beyond PROPOSAL_ONLY or requests semantic publication. */
    AUTHORITY_VIOLATION,
    /** Product-facing scenario or capability wording contains an implementation, provider, or evaluator identifier. */
    SCENARIO_TEXT_IDENTIFIER
}

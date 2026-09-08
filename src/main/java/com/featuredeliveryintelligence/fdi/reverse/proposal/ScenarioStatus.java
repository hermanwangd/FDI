package com.featuredeliveryintelligence.fdi.reverse.proposal;

/**
 * Review status of a generated behavior scenario proposal. Every generated
 * scenario starts {@link #UNREVIEWED}: only the sole Human Reviewer can
 * ACCEPT, EDIT, or REJECT it, and only accepted versions may enter frozen
 * Product Semantics. Contract records reject any other value with
 * {@code ReverseFailure.AUTHORITY_VIOLATION}.
 */
public enum ScenarioStatus {
    /** Freshly generated; awaits Human Reviewer decision. Proposal-only. */
    UNREVIEWED
}

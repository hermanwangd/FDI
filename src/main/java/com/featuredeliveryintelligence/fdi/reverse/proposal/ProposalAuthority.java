package com.featuredeliveryintelligence.fdi.reverse.proposal;

/**
 * Authority of every reverse-experiment output (PKB-REVERSE-002). Reverse
 * results are hypotheses, not Product truth: the only value this prototype
 * can produce is {@link #PROPOSAL_ONLY}. The enum is the stable extension
 * point; contract records reject any other value with
 * {@code ReverseFailure.AUTHORITY_VIOLATION}.
 */
public enum ProposalAuthority {
    /**
     * Proposal-only output. It cannot publish Product semantics, mark a
     * Capability or scenario accepted, or enter frozen experiment semantics
     * without Human Reviewer ACCEPT of this exact revision.
     */
    PROPOSAL_ONLY
}

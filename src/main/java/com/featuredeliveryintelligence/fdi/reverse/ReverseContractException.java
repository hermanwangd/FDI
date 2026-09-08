package com.featuredeliveryintelligence.fdi.reverse;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

/**
 * Typed fail-closed exception for the reverse-proposal contract. Every
 * refusal raised by {@code reverse.evidence} and {@code reverse.proposal}
 * validation carries exactly one {@link ReverseFailure} code so callers,
 * tests, and later slices can assert on the stable vocabulary instead of
 * message text.
 */
public final class ReverseContractException extends RuntimeContractException {

    private final ReverseFailure failure;

    public ReverseContractException(ReverseFailure failure, String message) {
        super(message);
        if (failure == null) throw new IllegalArgumentException("failure code must not be null");
        this.failure = failure;
    }

    /** Stable refusal code; never null. */
    public ReverseFailure failure() {
        return failure;
    }
}

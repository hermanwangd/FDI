package com.featuredeliveryintelligence.fdi.validation.liveverifier;

/**
 * Raised when live Graphify evidence cannot truthfully be marked
 * EXACTLY_BOUND. Ports the Python {@code VerificationFailure(RuntimeError)};
 * the CLI's handled-failure set maps it to NOT_BOUND evidence with exit 2.
 */
public final class VerificationFailure extends RuntimeException {
    public VerificationFailure(String message) {
        super(message);
    }
}

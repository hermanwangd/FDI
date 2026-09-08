package com.featuredeliveryintelligence.fdi.validation.blindreview;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

/**
 * Ports the Python consumer's {@code BindingError}: a source run cannot safely
 * enter the label/order-blinded comparison. The CLI maps this exception to
 * exit code 2 with the message on stderr, exactly like the Python CLI.
 */
public final class BlindReviewBindingException extends RuntimeContractException {
    public BlindReviewBindingException(String message) {
        super(message);
    }

    public BlindReviewBindingException(String message, Throwable cause) {
        super(message, cause);
    }
}

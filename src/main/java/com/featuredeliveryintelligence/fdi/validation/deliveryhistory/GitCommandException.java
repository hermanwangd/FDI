package com.featuredeliveryintelligence.fdi.validation.deliveryhistory;

/**
 * Mirrors the transitional Python consumer's caught
 * {@code subprocess.CalledProcessError}: the message is pre-formatted to the
 * CPython {@code str(CalledProcessError)} text so the packaged CLI can print
 * the same {@code {"status": "ERROR", ...}} JSON and exit 1.
 */
public final class GitCommandException extends RuntimeException {
    public GitCommandException(String message) {
        super(message);
    }
}

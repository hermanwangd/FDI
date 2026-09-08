package com.featuredeliveryintelligence.fdi.testbehavior.api;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

/**
 * Typed failure for the test-behavior extractor contract. Every fail-closed
 * refusal carries a stable {@link TestBehaviorErrorCode}; the exception type
 * stays a {@link RuntimeContractException} so existing framework catch
 * boundaries keep working.
 */
public class TestBehaviorExtractionException extends RuntimeContractException {
    private final TestBehaviorErrorCode code;

    public TestBehaviorExtractionException(TestBehaviorErrorCode code, String message) {
        super(message);
        if (code == null) throw new IllegalArgumentException("error code must not be null");
        this.code = code;
    }

    public TestBehaviorExtractionException(TestBehaviorErrorCode code, String message, Throwable cause) {
        super(message, cause);
        if (code == null) throw new IllegalArgumentException("error code must not be null");
        this.code = code;
    }

    public TestBehaviorErrorCode code() {
        return code;
    }
}

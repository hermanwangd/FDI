package com.featuredeliveryintelligence.fdi.orchestration;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

public final class ClarificationRequiredException extends RuntimeContractException {
    public ClarificationRequiredException(String message) { super(message); }
}

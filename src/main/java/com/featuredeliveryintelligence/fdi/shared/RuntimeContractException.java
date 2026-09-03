package com.featuredeliveryintelligence.fdi.shared;

public class RuntimeContractException extends IllegalArgumentException {
    public RuntimeContractException(String message) { super(message); }
    public RuntimeContractException(String message, Throwable cause) { super(message, cause); }
}

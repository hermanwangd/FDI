package com.featuredeliveryintelligence.fdi.runtime;

public class RuntimeContractException extends IllegalArgumentException {
    public RuntimeContractException(String message) { super(message); }
    public RuntimeContractException(String message, Throwable cause) { super(message, cause); }
}

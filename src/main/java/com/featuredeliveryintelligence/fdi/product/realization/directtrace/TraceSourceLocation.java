package com.featuredeliveryintelligence.fdi.product.realization.directtrace;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

/** Exact repository-relative location of a test-behavior observation or gap. */
public record TraceSourceLocation(String repositoryRelativePath, int line, int column) {
    public TraceSourceLocation {
        if (repositoryRelativePath == null || repositoryRelativePath.isBlank()
                || repositoryRelativePath.startsWith("/") || repositoryRelativePath.contains("\\")
                || repositoryRelativePath.contains("../") || line < 1 || column < 1) {
            throw new RuntimeContractException("invalid trace source location");
        }
    }
}

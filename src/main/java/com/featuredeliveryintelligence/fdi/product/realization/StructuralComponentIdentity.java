package com.featuredeliveryintelligence.fdi.product.realization;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.EnumSet;

public record StructuralComponentIdentity(
        String sourceRevision,
        String sourcePath,
        Granularity granularity,
        String qualifiedSymbol,
        String providerNodeId) {

    private static final EnumSet<Granularity> SYMBOL_LEVEL_GRANULARITIES = EnumSet.of(
            Granularity.TYPE,
            Granularity.METHOD,
            Granularity.TEMPLATE,
            Granularity.CONFIGURATION);

    public enum Granularity {
        REPOSITORY,
        FILE,
        TYPE,
        METHOD,
        TEMPLATE,
        CONFIGURATION
    }

    public StructuralComponentIdentity {
        if (sourceRevision == null || !sourceRevision.matches("[0-9a-f]{40}")) {
            throw new RuntimeContractException("sourceRevision must be a full lowercase Git SHA");
        }
        if (granularity == null) {
            throw new RuntimeContractException("granularity is required");
        }
        if (sourcePath == null
                || sourcePath.isBlank()
                || isNoncanonicalPath(sourcePath, granularity)) {
            throw new RuntimeContractException("sourcePath must be repository-relative");
        }
        if (SYMBOL_LEVEL_GRANULARITIES.contains(granularity)
                && (qualifiedSymbol == null || qualifiedSymbol.isBlank())) {
            throw new RuntimeContractException("qualifiedSymbol is required");
        }
        if (providerNodeId == null || providerNodeId.isBlank()) {
            throw new RuntimeContractException("providerNodeId is required");
        }
    }

    private static boolean isNoncanonicalPath(String sourcePath, Granularity granularity) {
        if (sourcePath.equals(".")) {
            return granularity != Granularity.REPOSITORY;
        }
        if (sourcePath.startsWith("/")
                || sourcePath.matches("^[A-Za-z]:.*")
                || sourcePath.contains("\\")) {
            return true;
        }
        for (String segment : sourcePath.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                return true;
            }
        }
        return false;
    }
}

package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

public record LearningCandidate(
        KnowledgeRoute route,
        String statement,
        String scope,
        String applicability,
        String limitations,
        List<String> conflictRefs) {
    public LearningCandidate {
        if (route == null) throw new IllegalArgumentException("route is required");
        if (statement == null || statement.isBlank()) throw new IllegalArgumentException("statement is required");
        if (scope == null || scope.isBlank()) throw new IllegalArgumentException("scope is required");
        applicability = applicability == null ? "" : applicability;
        limitations = limitations == null ? "" : limitations;
        conflictRefs = List.copyOf(conflictRefs == null ? List.of() : conflictRefs);
    }
}

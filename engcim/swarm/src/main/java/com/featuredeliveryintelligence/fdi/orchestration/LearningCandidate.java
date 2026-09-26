package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

public record LearningCandidate(
        String subjectRef,
        KnowledgeRoute route,
        String statement,
        String scope,
        String applicability,
        List<String> limitations,
        List<String> conflictRefs) {
    public LearningCandidate {
        if (subjectRef == null || subjectRef.isBlank()) throw new IllegalArgumentException("subjectRef is required");
        if (route == null) throw new IllegalArgumentException("route is required");
        if (statement == null || statement.isBlank()) throw new IllegalArgumentException("statement is required");
        if (scope == null || scope.isBlank()) throw new IllegalArgumentException("scope is required");
        applicability = applicability == null ? "" : applicability;
        limitations = List.copyOf(limitations == null ? List.of() : limitations);
        conflictRefs = List.copyOf(conflictRefs == null ? List.of() : conflictRefs);
    }

    public LearningCandidate(
            KnowledgeRoute route,
            String statement,
            String scope,
            String applicability,
            String limitation,
            List<String> conflictRefs) {
        this("subject:default", route, statement, scope, applicability,
                limitation == null || limitation.isBlank() ? List.of() : List.of(limitation), conflictRefs);
    }
}

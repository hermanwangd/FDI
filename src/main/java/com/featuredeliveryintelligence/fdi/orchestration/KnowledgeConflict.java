package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

public record KnowledgeConflict(
        String conflictRef,
        String workspaceRef,
        String subjectRef,
        List<String> observationRefs,
        List<String> statements) {
    public KnowledgeConflict {
        if (conflictRef == null || conflictRef.isBlank()) throw new IllegalArgumentException("conflictRef is required");
        if (workspaceRef == null || workspaceRef.isBlank()) throw new IllegalArgumentException("workspaceRef is required");
        if (subjectRef == null || subjectRef.isBlank()) throw new IllegalArgumentException("subjectRef is required");
        observationRefs = List.copyOf(observationRefs == null ? List.of() : observationRefs);
        statements = List.copyOf(statements == null ? List.of() : statements);
        if (observationRefs.size() < 2 || statements.size() < 2) throw new IllegalArgumentException("conflict needs two observations");
    }
}

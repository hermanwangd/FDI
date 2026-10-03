package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;
import java.util.Map;

/** Human-provided material retained before a Mission is formulated. */
public record MissionRequest(
        String requestRef,
        String workspaceRef,
        String projectRef,
        String scope,
        String goal,
        List<String> constraints,
        List<String> acceptanceCriteria,
        String requestedRevision,
        Map<String, String> currentRepositoryRevisions,
        KnowledgeContextRequirement knowledgeContextRequirement) {

    public enum KnowledgeContextRequirement { REQUIRED, OPTIONAL }

    public MissionRequest(
            String requestRef,
            String workspaceRef,
            String projectRef,
            String scope,
            String goal,
            List<String> constraints,
            List<String> acceptanceCriteria,
            String requestedRevision) {
        this(requestRef, workspaceRef, projectRef, scope, goal, constraints, acceptanceCriteria,
                requestedRevision, Map.of(), KnowledgeContextRequirement.OPTIONAL);
    }

    public MissionRequest(
            String requestRef,
            String workspaceRef,
            String projectRef,
            String scope,
            String goal,
            List<String> constraints,
            List<String> acceptanceCriteria,
            String requestedRevision,
            Map<String, String> currentRepositoryRevisions) {
        this(requestRef, workspaceRef, projectRef, scope, goal, constraints, acceptanceCriteria,
                requestedRevision, currentRepositoryRevisions, KnowledgeContextRequirement.OPTIONAL);
    }

    public MissionRequest {
        constraints = List.copyOf(constraints == null ? List.of() : constraints);
        acceptanceCriteria = List.copyOf(acceptanceCriteria == null ? List.of() : acceptanceCriteria);
        requestedRevision = requestedRevision == null ? "" : requestedRevision;
        currentRepositoryRevisions = Map.copyOf(
                currentRepositoryRevisions == null ? Map.of() : currentRepositoryRevisions);
        knowledgeContextRequirement = knowledgeContextRequirement == null
                ? KnowledgeContextRequirement.OPTIONAL
                : knowledgeContextRequirement;
    }

    public List<String> missingFields() {
        var missing = new java.util.ArrayList<String>();
        if (blank(requestRef)) missing.add("requestRef");
        if (blank(workspaceRef)) missing.add("workspaceRef");
        if (blank(projectRef)) missing.add("projectRef");
        if (blank(scope)) missing.add("scope");
        if (blank(goal)) missing.add("goal");
        if (acceptanceCriteria.isEmpty()) missing.add("acceptanceCriteria");
        return List.copyOf(missing);
    }

    public MissionRequest withRequestedRevision(String revision) {
        return new MissionRequest(requestRef, workspaceRef, projectRef, scope, goal,
                constraints, acceptanceCriteria, revision, currentRepositoryRevisions, knowledgeContextRequirement);
    }

    public MissionRequest withKnowledgeContextRequirement(KnowledgeContextRequirement requirement) {
        return new MissionRequest(requestRef, workspaceRef, projectRef, scope, goal,
                constraints, acceptanceCriteria, requestedRevision, currentRepositoryRevisions, requirement);
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}

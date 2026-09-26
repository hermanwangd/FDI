package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

/** Human-provided material retained before a Mission is formulated. */
public record MissionRequest(
        String requestRef,
        String workspaceRef,
        String projectRef,
        String scope,
        String goal,
        List<String> constraints,
        List<String> acceptanceCriteria,
        String requestedRevision) {

    public MissionRequest {
        constraints = List.copyOf(constraints == null ? List.of() : constraints);
        acceptanceCriteria = List.copyOf(acceptanceCriteria == null ? List.of() : acceptanceCriteria);
        requestedRevision = requestedRevision == null ? "" : requestedRevision;
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
                constraints, acceptanceCriteria, revision);
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}

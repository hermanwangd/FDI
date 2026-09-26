package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

/** Exact, immutable execution hand-off from Swarm Core to Runtime Binding. */
public record MissionExecutionEnvelope(
        String missionRef,
        String requestRef,
        String workspaceRef,
        String projectRef,
        String scope,
        String goal,
        List<String> constraints,
        List<String> acceptanceCriteria,
        String executionRevision) {

    public MissionExecutionEnvelope {
        require(missionRef, "missionRef");
        require(requestRef, "requestRef");
        require(workspaceRef, "workspaceRef");
        require(projectRef, "projectRef");
        require(scope, "scope");
        require(goal, "goal");
        require(executionRevision, "executionRevision");
        constraints = List.copyOf(constraints == null ? List.of() : constraints);
        acceptanceCriteria = requiredList(acceptanceCriteria, "acceptanceCriteria");
    }

    public static MissionExecutionEnvelope from(Mission mission) {
        MissionRequest request = mission.request();
        return new MissionExecutionEnvelope(
                mission.missionRef(), request.requestRef(), request.workspaceRef(), request.projectRef(),
                request.scope(), request.goal(), request.constraints(), request.acceptanceCriteria(),
                request.requestedRevision());
    }

    private static List<String> requiredList(List<String> values, String field) {
        if (values == null || values.isEmpty()) throw new IllegalArgumentException(field + " is required");
        return List.copyOf(values);
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}

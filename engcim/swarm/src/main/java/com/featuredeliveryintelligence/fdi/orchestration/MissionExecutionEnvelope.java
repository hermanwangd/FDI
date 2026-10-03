package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        String executionRevision,
        List<WorkspaceKnowledgeRepository.Entry> eligibleKnowledge,
        Map<String, String> currentRepositoryRevisions,
        KnowledgeContextStatus knowledgeContextStatus) {

    public MissionExecutionEnvelope(
            String missionRef,
            String requestRef,
            String workspaceRef,
            String projectRef,
            String scope,
            String goal,
            List<String> constraints,
            List<String> acceptanceCriteria,
            String executionRevision) {
        this(missionRef, requestRef, workspaceRef, projectRef, scope, goal, constraints, acceptanceCriteria,
                executionRevision, List.of(), Map.of(), KnowledgeContextStatus.NOT_CONFIGURED);
    }

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
        eligibleKnowledge = List.copyOf(eligibleKnowledge == null ? List.of() : eligibleKnowledge);
        currentRepositoryRevisions = Map.copyOf(
                currentRepositoryRevisions == null ? Map.of() : currentRepositoryRevisions);
        Objects.requireNonNull(knowledgeContextStatus, "knowledgeContextStatus is required");
        if (knowledgeContextStatus == KnowledgeContextStatus.AVAILABLE && eligibleKnowledge.isEmpty()) {
            throw new IllegalArgumentException("AVAILABLE knowledge context requires eligible records");
        }
        if (knowledgeContextStatus != KnowledgeContextStatus.AVAILABLE && !eligibleKnowledge.isEmpty()) {
            throw new IllegalArgumentException("non-available knowledge context cannot contain eligible records");
        }
    }

    public static MissionExecutionEnvelope from(Mission mission) {
        MissionRequest request = mission.request();
        return new MissionExecutionEnvelope(
                mission.missionRef(), request.requestRef(), request.workspaceRef(), request.projectRef(),
                request.scope(), request.goal(), request.constraints(), request.acceptanceCriteria(),
                request.requestedRevision(), List.of(), request.currentRepositoryRevisions(),
                KnowledgeContextStatus.NOT_CONFIGURED);
    }

    public MissionExecutionEnvelope withKnowledgeContext(
            SwarmKnowledgeGateway.EligibleConsumerContext context) {
        Objects.requireNonNull(context, "knowledge context is required");
        KnowledgeContextStatus status = context.selected().isEmpty()
                ? KnowledgeContextStatus.NO_ELIGIBLE_RECORDS
                : KnowledgeContextStatus.AVAILABLE;
        return withKnowledge(context.selected().stream().limit(3).toList(),
                context.request().currentRepositoryRevisions(), status);
    }

    public MissionExecutionEnvelope withKnowledgeProviderUnavailable(Map<String, String> repositoryRevisions) {
        return withKnowledge(List.of(), repositoryRevisions, KnowledgeContextStatus.PROVIDER_UNAVAILABLE);
    }

    private MissionExecutionEnvelope withKnowledge(
            List<WorkspaceKnowledgeRepository.Entry> selected,
            Map<String, String> repositoryRevisions,
            KnowledgeContextStatus status) {
        return new MissionExecutionEnvelope(missionRef, requestRef, workspaceRef, projectRef, scope, goal,
                constraints, acceptanceCriteria, executionRevision, selected, repositoryRevisions, status);
    }

    public enum KnowledgeContextStatus { NOT_CONFIGURED, AVAILABLE, NO_ELIGIBLE_RECORDS, PROVIDER_UNAVAILABLE }

    private static List<String> requiredList(List<String> values, String field) {
        if (values == null || values.isEmpty()) throw new IllegalArgumentException(field + " is required");
        return List.copyOf(values);
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}

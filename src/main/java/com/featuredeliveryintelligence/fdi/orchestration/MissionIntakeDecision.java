package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

public record MissionIntakeDecision(IntakeStatus status, Mission mission, List<String> clarificationFields) {
    public MissionIntakeDecision {
        if (status == null) throw new IllegalArgumentException("status is required");
        if (status == IntakeStatus.READY && mission == null) throw new IllegalArgumentException("ready Mission is required");
        if (status == IntakeStatus.CLARIFICATION_REQUIRED && (clarificationFields == null || clarificationFields.isEmpty())) {
            throw new IllegalArgumentException("clarification fields are required");
        }
        clarificationFields = List.copyOf(clarificationFields == null ? List.of() : clarificationFields);
    }

    public static MissionIntakeDecision ready(Mission mission) {
        return new MissionIntakeDecision(IntakeStatus.READY, mission, List.of());
    }

    public static MissionIntakeDecision clarificationRequired(List<String> fields) {
        return new MissionIntakeDecision(IntakeStatus.CLARIFICATION_REQUIRED, null, fields);
    }
}

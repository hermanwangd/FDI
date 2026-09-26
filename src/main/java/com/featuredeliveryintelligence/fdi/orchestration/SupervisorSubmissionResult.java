package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

public record SupervisorSubmissionResult(
        SupervisorSubmissionStatus status,
        Mission mission,
        WorkItemResult workItemResult,
        List<String> clarificationFields) {
    public SupervisorSubmissionResult {
        if (status == null) throw new IllegalArgumentException("status is required");
        if (status == SupervisorSubmissionStatus.DISPATCHED && (mission == null || workItemResult == null)) {
            throw new IllegalArgumentException("dispatched submission requires Mission and result");
        }
        if (status == SupervisorSubmissionStatus.CLARIFICATION_REQUIRED && (clarificationFields == null || clarificationFields.isEmpty())) {
            throw new IllegalArgumentException("clarification fields are required");
        }
        clarificationFields = List.copyOf(clarificationFields == null ? List.of() : clarificationFields);
    }
}

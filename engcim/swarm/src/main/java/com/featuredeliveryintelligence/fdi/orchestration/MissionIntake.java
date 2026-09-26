package com.featuredeliveryintelligence.fdi.orchestration;

/** Validates and formulates a Mission without performing engineering work. */
public final class MissionIntake {
    public MissionIntakeDecision assess(MissionRequest request) {
        if (request == null) throw new IllegalArgumentException("request is required");
        MissionRequest normalized = request.requestedRevision().isBlank()
                ? request.withRequestedRevision("request-revision:" + request.requestRef())
                : request;
        var missing = normalized.missingFields();
        if (!missing.isEmpty()) return MissionIntakeDecision.clarificationRequired(missing);
        return MissionIntakeDecision.ready(new Mission("mission:" + normalized.requestRef(), normalized, MissionState.READY));
    }

    public Mission formulate(MissionRequest request) {
        MissionIntakeDecision decision = assess(request);
        if (decision.status() == IntakeStatus.CLARIFICATION_REQUIRED) {
            throw new ClarificationRequiredException(
                    "Mission clarification required: " + String.join(", ", decision.clarificationFields()));
        }
        return decision.mission();
    }
}

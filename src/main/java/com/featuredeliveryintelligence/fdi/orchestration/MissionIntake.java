package com.featuredeliveryintelligence.fdi.orchestration;

/** Validates and formulates a Mission without performing engineering work. */
public final class MissionIntake {
    public Mission formulate(MissionRequest request) {
        var missing = request.missingFields();
        if (!missing.isEmpty()) {
            throw new ClarificationRequiredException("Mission clarification required: " + String.join(", ", missing));
        }
        return new Mission("mission:" + request.requestRef(), request, MissionState.READY);
    }
}

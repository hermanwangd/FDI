package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.Objects;

/** Execution/request instance; it is not an additional ENGCIM component. */
public record Mission(String missionRef, MissionRequest request, MissionState state) {
    public Mission {
        if (missionRef == null || missionRef.isBlank()) throw new IllegalArgumentException("missionRef is required");
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(state, "state is required");
    }
}

package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.Objects;

/** Existing Swarm Core integration facade, not a new ENGCIM component. */
public final class SwarmMissionGateway {
    private final RuntimeBindingPort runtimeBinding;

    public SwarmMissionGateway(RuntimeBindingPort runtimeBinding) {
        this.runtimeBinding = Objects.requireNonNull(runtimeBinding, "runtimeBinding is required");
    }

    public WorkItemResult execute(Mission mission) {
        Objects.requireNonNull(mission, "mission is required");
        MissionExecutionEnvelope execution = MissionExecutionEnvelope.from(mission);
        BindingReceipt receipt = runtimeBinding.execute(execution);
        return new WorkItemResult(
                mission.missionRef(),
                mission.request().requestRef(),
                mission.request().workspaceRef(),
                receipt.runtimeBindingRef(),
                receipt.multicaExecutionRef(),
                receipt.executionRevision(),
                receipt.executionStatus(),
                receipt.evidenceRefs());
    }
}

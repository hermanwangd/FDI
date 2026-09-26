package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** External Supervisor adapter: intake and closure only; Swarm owns engineering semantics. */
public final class ClaudeSupervisorGateway {
    private final MissionIntake intake;
    private final SwarmMissionGateway swarm;

    public ClaudeSupervisorGateway(MissionIntake intake, SwarmMissionGateway swarm) {
        this.intake = Objects.requireNonNull(intake, "intake is required");
        this.swarm = Objects.requireNonNull(swarm, "swarm is required");
    }

    public SupervisorSubmissionResult submit(MissionRequest request) {
        MissionIntakeDecision decision = intake.assess(request);
        if (decision.status() == IntakeStatus.CLARIFICATION_REQUIRED) {
            return new SupervisorSubmissionResult(
                    SupervisorSubmissionStatus.CLARIFICATION_REQUIRED, null, null, decision.clarificationFields());
        }
        Mission mission = decision.mission();
        return new SupervisorSubmissionResult(
                SupervisorSubmissionStatus.DISPATCHED, mission, swarm.execute(mission), List.of());
    }

    public MissionClosureSummary close(
            Mission mission,
            WorkItemResult workItemResult,
            VerificationResult verificationResult,
            ControlResult controlResult,
            List<String> subjectRefs,
            List<String> sourceRefs) {
        Objects.requireNonNull(mission, "mission is required");
        requireSameMission(mission, workItemResult.missionRef(), "WorkItemResult");
        requireSameMission(mission, verificationResult.missionRef(), "VerificationResult");
        requireSameMission(mission, controlResult.missionRef(), "ControlResult");
        if (!mission.request().workspaceRef().equals(workItemResult.workspaceRef())) {
            throw new IllegalArgumentException("WorkItemResult workspaceRef does not match Mission");
        }
        var evidenceRefs = new ArrayList<String>();
        evidenceRefs.addAll(workItemResult.evidenceRefs());
        evidenceRefs.addAll(verificationResult.evidenceRefs());
        return new MissionClosureSummary(
                "closure:" + mission.missionRef(), mission.missionRef(), mission.request().workspaceRef(),
                subjectRefs, sourceRefs, evidenceRefs.stream().distinct().toList());
    }

    private static void requireSameMission(Mission mission, String missionRef, String resultType) {
        if (!mission.missionRef().equals(missionRef)) {
            throw new IllegalArgumentException(resultType + " missionRef does not match Mission");
        }
    }
}

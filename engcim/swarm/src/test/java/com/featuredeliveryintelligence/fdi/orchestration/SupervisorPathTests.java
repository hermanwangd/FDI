package com.featuredeliveryintelligence.fdi.orchestration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SupervisorPathTests {
    @Test
    void completeHumanRequestIsSubmittedBySupervisorToSwarm() {
        var executions = new ArrayList<MissionExecutionEnvelope>();
        var binding = new MulticaRuntimeBinding("binding:multica", execution -> {
            executions.add(execution);
            return new BindingReceipt("binding:multica", "multica:1", execution.executionRevision(), "COMMITTED", List.of("evidence:1"));
        });
        var supervisor = new ClaudeSupervisorGateway(new MissionIntake(), new SwarmMissionGateway(binding));

        SupervisorSubmissionResult result = supervisor.submit(request());

        assertThat(result.status()).isEqualTo(SupervisorSubmissionStatus.DISPATCHED);
        assertThat(result.mission().request().goal()).isEqualTo(request().goal());
        assertThat(executions).singleElement().satisfies(execution -> {
            assertThat(execution.workspaceRef()).isEqualTo("workspace-a");
            assertThat(execution.acceptanceCriteria()).containsExactly("acceptance-1");
        });
    }

    @Test
    void materiallyIncompleteRequestReturnsTargetedClarificationWithoutDispatch() {
        var calls = new ArrayList<MissionExecutionEnvelope>();
        var supervisor = new ClaudeSupervisorGateway(
                new MissionIntake(), new SwarmMissionGateway(execution -> {
                    calls.add(execution);
                    return new BindingReceipt("binding:1", "multica:1", "rev-1", "COMMITTED", List.of("e:1"));
                }));

        SupervisorSubmissionResult result = supervisor.submit(
                new MissionRequest("req-incomplete", "workspace-a", "project-a", "scope", "", List.of("constraint"), List.of(), "rev-1"));

        assertThat(result.status()).isEqualTo(SupervisorSubmissionStatus.CLARIFICATION_REQUIRED);
        assertThat(result.clarificationFields()).containsExactly("goal", "acceptanceCriteria");
        assertThat(calls).isEmpty();
    }

    @Test
    void completeRequestWithoutConstraintsOrRevisionIsNotUnnecessarilyDelayed() {
        var executions = new ArrayList<MissionExecutionEnvelope>();
        ClaudeSupervisorGateway supervisor = new ClaudeSupervisorGateway(new MissionIntake(),
                new SwarmMissionGateway(execution -> {
                    executions.add(execution);
                    return new BindingReceipt("binding:1", "multica:1", execution.executionRevision(), "COMMITTED", List.of("e:1"));
                }));

        SupervisorSubmissionResult result = supervisor.submit(new MissionRequest(
                "req-no-extra", "workspace-a", "project-a", "scope", "goal", List.of(), List.of("acceptance"), ""));

        assertThat(result.status()).isEqualTo(SupervisorSubmissionStatus.DISPATCHED);
        assertThat(executions).singleElement().extracting(MissionExecutionEnvelope::executionRevision)
                .isEqualTo("request-revision:req-no-extra");
        assertThat(executions.get(0).constraints()).isEmpty();
    }

    @Test
    void supervisorClosureSummaryPreservesEvidenceBeforeLearningSource() {
        var binding = new MulticaRuntimeBinding("binding:1",
                execution -> new BindingReceipt("binding:1", "multica:1", execution.executionRevision(), "COMMITTED", List.of("evidence:work")));
        var supervisor = new ClaudeSupervisorGateway(new MissionIntake(), new SwarmMissionGateway(binding));
        SupervisorSubmissionResult submission = supervisor.submit(request());

        MissionClosureSummary summary = supervisor.close(
                submission.mission(), submission.workItemResult(),
                new VerificationResult(submission.mission().missionRef(), "PASS", List.of("evidence:verification")),
                new ControlResult(submission.mission().missionRef(), "SATISFIED", "control-rev-1"),
                List.of("subject:runtime"), List.of("source:mission"));
        MissionLearningSource source = MissionLearningSourceFactory.from(summary);

        assertThat(summary.evidenceRefs()).containsExactly("evidence:work", "evidence:verification");
        assertThat(source.workspaceRef()).isEqualTo("workspace-a");
        assertThat(source.missionRef()).isEqualTo(submission.mission().missionRef());
    }

    private static MissionRequest request() {
        return new MissionRequest("req-supervisor", "workspace-a", "project-a", "src/main",
                "Implement the requested change", List.of("preserve API"), List.of("acceptance-1"), "rev-1");
    }
}

package com.featuredeliveryintelligence.fdi.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MissionFlowTests {
    @Test
    void T01_completeRequestCrossesMissionSwarmBindingAndMultica() {
        var received = new ArrayList<MissionExecutionEnvelope>();
        RuntimeBindingPort binding = execution -> {
            received.add(execution);
            return new BindingReceipt("binding:multica", "multica:exec-1", "rev-17", "COMMITTED", List.of("evidence:1"));
        };

        Mission mission = new MissionIntake().formulate(request());
        WorkItemResult result = new SwarmMissionGateway(binding).execute(mission);

        assertThat(received).singleElement().satisfies(execution -> {
            assertThat(execution.missionRef()).isEqualTo(mission.missionRef());
            assertThat(execution.constraints()).isEqualTo(request().constraints());
            assertThat(execution.acceptanceCriteria()).isEqualTo(request().acceptanceCriteria());
        });
        assertThat(result.missionRef()).isEqualTo("mission:req-1");
        assertThat(result.workspaceRef()).isEqualTo("workspace-a");
        assertThat(result.runtimeBindingRef()).isEqualTo("binding:multica");
        assertThat(result.multicaExecutionRef()).isEqualTo("multica:exec-1");
    }

    @Test
    void T02_incompleteRequestRequiresClarificationAndDoesNotDispatch() {
        var calls = new ArrayList<Mission>();
        MissionRequest incomplete = new MissionRequest("req-2", "workspace-a", "project-a", "scope", "", List.of(), List.of(), "rev-1");

        assertThatThrownBy(() -> new MissionIntake().formulate(incomplete))
                .isInstanceOf(ClarificationRequiredException.class)
                .hasMessageContaining("goal")
                .hasMessageContaining("acceptanceCriteria");
        assertThat(calls).isEmpty();
    }

    @Test
    void T03_constraintsAndAcceptanceCriteriaArePreservedExactly() {
        MissionRequest request = request();
        Mission formulated = new MissionIntake().formulate(request);

        assertThat(formulated.request().constraints()).isEqualTo(request.constraints());
        assertThat(formulated.request().acceptanceCriteria()).isEqualTo(request.acceptanceCriteria());
        assertThat(formulated.request().requestedRevision()).isEqualTo("rev-17");
    }

    @Test
    void T06_bindingReceivesExactMissionIdentityAndRevision() {
        var seen = new ArrayList<MissionExecutionEnvelope>();
        RuntimeBindingPort binding = execution -> {
            seen.add(execution);
            return new BindingReceipt("binding:1", "multica:1", execution.executionRevision(), "COMMITTED", List.of("e:1"));
        };

        Mission mission = new MissionIntake().formulate(request());
        WorkItemResult result = new SwarmMissionGateway(binding).execute(mission);

        assertThat(seen.get(0).missionRef()).isEqualTo(mission.missionRef());
        assertThat(seen.get(0).workspaceRef()).isEqualTo("workspace-a");
        assertThat(seen.get(0).constraints()).containsExactly("no live dispatch");
        assertThat(seen.get(0).acceptanceCriteria()).containsExactly("T01 passes");
        assertThat(result.executionRevision()).isEqualTo("rev-17");
    }

    @Test
    void T12_resultTypesRemainDistinct() {
        assertThat(recordNames(WorkItemResult.class)).doesNotContain("verificationStatus", "controlStatus");
        assertThat(recordNames(VerificationResult.class)).contains("verificationStatus");
        assertThat(recordNames(ControlResult.class)).contains("controlStatus");

        var repository = new InMemoryWorkspaceKnowledgeRepository();
        var knowledgeGateway = new SwarmKnowledgeGateway();
        var lifecycle = new SwarmKnowledgeLifecycle(knowledgeGateway, repository);
        var supervisor = new ClaudeSupervisorGateway(new MissionIntake(),
                new SwarmMissionGateway(new MulticaRuntimeBinding("binding:multica",
                        execution -> new BindingReceipt(
                                "binding:multica", "multica:exec-1", execution.executionRevision(),
                                "COMMITTED", List.of("evidence:execution")))));
        SupervisorSubmissionResult submission = supervisor.submit(request());
        Mission mission = submission.mission();
        WorkItemResult execution = submission.workItemResult();
        var verification = new VerificationResult(
                mission.missionRef(), "FAILED", List.of("evidence:verification"));
        var control = new ControlResult(mission.missionRef(), "UNSATISFIED", "control:1");

        MissionClosureSummary closure = supervisor.close(
                mission, execution, verification, control,
                List.of("runtime-revision"), List.of("source:mission"));

        assertThat(submission.status()).isEqualTo(SupervisorSubmissionStatus.DISPATCHED);
        assertThat(execution.executionStatus()).isEqualTo("COMMITTED");
        assertThat(verification.verificationStatus()).isEqualTo("FAILED");
        assertThat(control.controlStatus()).isEqualTo("UNSATISFIED");
        assertThat(execution.missionRef()).isEqualTo(mission.missionRef());
        assertThat(execution.executionRevision()).isEqualTo(request().requestedRevision());
        assertThat(closure).isEqualTo(new MissionClosureSummary(
                "closure:" + mission.missionRef(), mission.missionRef(), request().workspaceRef(),
                List.of("runtime-revision"), List.of("source:mission"),
                List.of("evidence:execution", "evidence:verification")));
        // Closure carries linkage only; it cannot synthesize a final Human DONE outcome.
        assertThat(recordNames(MissionClosureSummary.class)).containsExactly(
                "closureSummaryRef", "missionRef", "workspaceRef", "subjectRefs", "sourceRefs", "evidenceRefs");
        assertThat(repository.findByWorkspace(mission.request().workspaceRef())).isEmpty();

        MissionLearningSource source = MissionLearningSourceFactory.from(closure);

        assertThat(source.missionRef()).isEqualTo(closure.missionRef());
        assertThat(source.closureSummaryRef()).isEqualTo(closure.closureSummaryRef());
        assertThat(source.workspaceRef()).isEqualTo(closure.workspaceRef());
        assertThat(source.subjectRefs()).isEqualTo(closure.subjectRefs());
        assertThat(source.sourceRefs()).isEqualTo(closure.sourceRefs());
        assertThat(source.evidenceRefs()).isEqualTo(closure.evidenceRefs());
        assertThat(repository.findByWorkspace(source.workspaceRef())).isEmpty();
        assertThat(knowledgeGateway.retrieve(source.workspaceRef(), repository)).isEmpty();

        var candidate = new LearningCandidate(
                "runtime-revision", KnowledgeRoute.WORKSPACE_SEMANTIC,
                "committed execution still requires verification and control", "runtime", "workspace-a",
                List.of(), List.of());
        WorkspaceKnowledgeLifecycleResult learning = lifecycle.buildAndPersist(
                source, candidate, KnowledgeGovernanceDecision.DEFERRED, "workspace-reviewer");

        assertThat(learning.missionLearningSourceRef()).isEqualTo(source.learningSourceRef());
        assertThat(learning.proposal().sourceRefs()).isEqualTo(source.sourceRefs());
        assertThat(learning.proposal().evidenceRefs()).isEqualTo(source.evidenceRefs());
        assertThat(learning.governedKnowledge().decision()).isEqualTo(KnowledgeGovernanceDecision.DEFERRED);
        assertThat(learning.captureReceipt()).isEmpty();
        assertThat(learning.retrievedKnowledge()).isEmpty();
        assertThat(knowledgeGateway.retrieve(source.workspaceRef(), repository)).isEmpty();
        assertThat(repository.findByWorkspace(source.workspaceRef())).isEmpty();
    }

    private static MissionRequest request() {
        return new MissionRequest("req-1", "workspace-a", "project-a", "src/main", "Implement RC10 boundary", List.of("no live dispatch"), List.of("T01 passes"), "rev-17");
    }

    private static List<String> recordNames(Class<?> type) {
        return java.util.Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
    }
}

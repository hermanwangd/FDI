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
        var received = new ArrayList<Mission>();
        RuntimeBindingPort binding = mission -> {
            received.add(mission);
            return new BindingReceipt("binding:multica", "multica:exec-1", "rev-17", "COMMITTED", List.of("evidence:1"));
        };

        Mission mission = new MissionIntake().formulate(request());
        WorkItemResult result = new SwarmMissionGateway(binding).execute(mission);

        assertThat(received).containsExactly(mission);
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
                .hasMessageContaining("constraints");
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
        var seen = new ArrayList<Mission>();
        RuntimeBindingPort binding = mission -> {
            seen.add(mission);
            return new BindingReceipt("binding:1", "multica:1", mission.request().requestedRevision(), "COMMITTED", List.of("e:1"));
        };

        Mission mission = new MissionIntake().formulate(request());
        WorkItemResult result = new SwarmMissionGateway(binding).execute(mission);

        assertThat(seen.get(0).missionRef()).isEqualTo(mission.missionRef());
        assertThat(seen.get(0).request().workspaceRef()).isEqualTo("workspace-a");
        assertThat(result.executionRevision()).isEqualTo("rev-17");
    }

    @Test
    void T12_resultTypesRemainDistinct() {
        assertThat(recordNames(WorkItemResult.class)).doesNotContain("verificationStatus", "controlStatus");
        assertThat(recordNames(VerificationResult.class)).contains("verificationStatus");
        assertThat(recordNames(ControlResult.class)).contains("controlStatus");
    }

    private static MissionRequest request() {
        return new MissionRequest("req-1", "workspace-a", "project-a", "src/main", "Implement RC10 boundary", List.of("no live dispatch"), List.of("T01 passes"), "rev-17");
    }

    private static List<String> recordNames(Class<?> type) {
        return java.util.Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
    }
}

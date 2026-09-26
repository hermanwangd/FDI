package com.featuredeliveryintelligence.fdi.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkspaceKnowledgeLifecycleTests {
    @Test
    void supervisorClosureFeedsSwarmKnowledgeLifecycleWithoutDirectSupervisorPersistence() {
        ClaudeSupervisorGateway supervisor = new ClaudeSupervisorGateway(
                new MissionIntake(),
                new SwarmMissionGateway(new MulticaRuntimeBinding(
                        "binding:multica",
                        execution -> new BindingReceipt(
                                "binding:multica", "multica:exec-1", execution.executionRevision(),
                                "COMMITTED", List.of("evidence:execution")))));
        SupervisorSubmissionResult submission = supervisor.submit(new MissionRequest(
                "request:1", "workspace-a", "project-a", "src/main", "verify runtime wiring",
                List.of("preserve attribution"), List.of("capture is evidence-bound"), "revision:1"));
        MissionClosureSummary closure = supervisor.close(
                submission.mission(),
                submission.workItemResult(),
                new VerificationResult(submission.mission().missionRef(), "PASS", List.of("evidence:verification")),
                new ControlResult(submission.mission().missionRef(), "SATISFIED", "control:1"),
                List.of("runtime-wiring"), List.of("source:mission"));
        MissionLearningSource source = MissionLearningSourceFactory.from(closure);
        WorkspaceKnowledgeLifecycleResult result = new SwarmKnowledgeLifecycle(
                new SwarmKnowledgeGateway(), new InMemoryWorkspaceKnowledgeRepository())
                .buildAndPersist(source, candidate(), "workspace-reviewer");

        assertThat(result.missionLearningSourceRef()).isEqualTo(source.learningSourceRef());
        assertThat(result.proposal().evidenceRefs())
                .containsExactly("evidence:execution", "evidence:verification");
        assertThat(result.retrievedKnowledge()).containsExactly(result.proposal());
    }

    @Test
    void completeMissionLearningSourceIsBuiltGovernedPersistedAndRetrieved() {
        SwarmKnowledgeGateway gateway = new SwarmKnowledgeGateway();
        MissionLearningSource source = source();
        WorkspaceKnowledgeProposal proposal = gateway.propose(source, candidate());
        GovernedWorkspaceKnowledge governed = gateway.govern(
                proposal, KnowledgeGovernanceDecision.APPROVED, "workspace-reviewer");
        List<WorkspaceKnowledgeProposal> externalStore = new ArrayList<>();
        WorkspaceKnowledgeProjectRef project = new WorkspaceKnowledgeProjectRef(
                "workspace-a", "workspace-knowledge-project", "WorkspaceKnowledge");
        MulticaWorkspaceKnowledgeRepository repository = new MulticaWorkspaceKnowledgeRepository(
                ignored -> project,
                new MulticaWorkspaceKnowledgePort() {
                    @Override
                    public WorkspaceKnowledgeCaptureResult persist(
                            WorkspaceKnowledgeProjectRef target,
                            GovernedWorkspaceKnowledge knowledge) {
                        externalStore.add(knowledge.proposal());
                        return new WorkspaceKnowledgeCaptureResult(
                                "capture:runtime", "knowledge:runtime", target.workspaceRef(), target.projectRef(),
                                knowledge.proposal().proposalRef(), knowledge.proposal().sourceRefs(),
                                knowledge.proposal().evidenceRefs(), "2026-09-26T04:05:22Z");
                    }

                    @Override
                    public List<WorkspaceKnowledgeProposal> retrieve(WorkspaceKnowledgeProjectRef target) {
                        return List.copyOf(externalStore);
                    }
                });

        WorkspaceKnowledgeLifecycleResult result = new SwarmKnowledgeLifecycle(gateway, repository)
                .buildAndPersist(source, candidate(), "workspace-reviewer");

        assertThat(result.routingDecision().route()).isEqualTo(KnowledgeRoute.WORKSPACE_SEMANTIC);
        assertThat(result.governedKnowledge().decision()).isEqualTo(KnowledgeGovernanceDecision.APPROVED);
        assertThat(result.captureReceipt()).get().extracting(WorkspaceKnowledgeCaptureResult::knowledgeRef)
                .isEqualTo("knowledge:runtime");
        assertThat(result.retrievedKnowledge()).containsExactly(result.proposal());
    }

    @Test
    void nonWorkspaceRouteCannotEnterWorkspaceKnowledgeLifecycle() {
        LearningCandidate candidate = new LearningCandidate(
                "runtime-revision", KnowledgeRoute.SKILL_IMPROVEMENT,
                "improve runtime reasoning", "skill", "reusable", List.of(), List.of());

        assertThatThrownBy(() -> new SwarmKnowledgeLifecycle(
                new SwarmKnowledgeGateway(), new InMemoryWorkspaceKnowledgeRepository())
                .buildAndPersist(source(), candidate, "workspace-reviewer"))
                .hasMessageContaining("only WorkspaceKnowledge routes");
    }

    private static MissionLearningSource source() {
        return new MissionLearningSource(
                "learning:mission-1", "workspace-a", "mission:1", "closure:1",
                List.of("runtime-revision"), List.of("source:1"), List.of("evidence:1"));
    }

    private static LearningCandidate candidate() {
        return new LearningCandidate(
                "runtime-revision", KnowledgeRoute.WORKSPACE_SEMANTIC,
                "verified runtime revision", "runtime", "workspace-a", List.of(), List.of());
    }
}

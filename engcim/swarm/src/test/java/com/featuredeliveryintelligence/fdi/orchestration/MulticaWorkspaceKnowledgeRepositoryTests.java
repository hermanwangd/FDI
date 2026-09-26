package com.featuredeliveryintelligence.fdi.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MulticaWorkspaceKnowledgeRepositoryTests {
    @Test
    void persistsApprovedKnowledgeThroughResolvedWorkspaceProjectAndReturnsCaptureReceipt() {
        SwarmKnowledgeGateway gateway = new SwarmKnowledgeGateway();
        WorkspaceKnowledgeProposal proposal = gateway.propose(source(), candidate());
        GovernedWorkspaceKnowledge governed = gateway.govern(proposal, KnowledgeGovernanceDecision.APPROVED, "workspace-reviewer");
        WorkspaceKnowledgeProjectRef project = new WorkspaceKnowledgeProjectRef(
                "workspace-a", "project-workspace-knowledge", "WorkspaceKnowledge");
        List<WorkspaceKnowledgeProposal> externalStore = new ArrayList<>();

        MulticaWorkspaceKnowledgePort port = new MulticaWorkspaceKnowledgePort() {
            @Override
            public WorkspaceKnowledgeCaptureResult persist(
                    WorkspaceKnowledgeProjectRef target,
                    GovernedWorkspaceKnowledge knowledge) {
                assertThat(target).isEqualTo(project);
                externalStore.add(knowledge.proposal());
                return new WorkspaceKnowledgeCaptureResult(
                        "capture:1", "knowledge:1", target.workspaceRef(), target.projectRef(),
                        knowledge.proposal().proposalRef(), knowledge.proposal().sourceRefs(),
                        knowledge.proposal().evidenceRefs(), "2026-09-26T04:05:22Z");
            }

            @Override
            public List<WorkspaceKnowledgeProposal> retrieve(WorkspaceKnowledgeProjectRef target) {
                assertThat(target).isEqualTo(project);
                return List.copyOf(externalStore);
            }
        };
        MulticaWorkspaceKnowledgeRepository repository = new MulticaWorkspaceKnowledgeRepository(
                ignored -> project, port);

        repository.save(governed);

        assertThat(repository.captureFor(proposal.proposalRef()))
                .get()
                .extracting(WorkspaceKnowledgeCaptureResult::knowledgeRef)
                .isEqualTo("knowledge:1");
        assertThat(repository.findByWorkspace("workspace-a")).containsExactly(proposal);
    }

    @Test
    void rejectsProjectResolvedForAnotherWorkspace() {
        MulticaWorkspaceKnowledgeRepository repository = new MulticaWorkspaceKnowledgeRepository(
                ignored -> new WorkspaceKnowledgeProjectRef(
                        "workspace-b", "project-b", "WorkspaceKnowledge"),
                new MulticaWorkspaceKnowledgePort() {
                    @Override
                    public WorkspaceKnowledgeCaptureResult persist(
                            WorkspaceKnowledgeProjectRef project,
                            GovernedWorkspaceKnowledge knowledge) {
                        throw new AssertionError("persistence must not be called");
                    }

                    @Override
                    public List<WorkspaceKnowledgeProposal> retrieve(WorkspaceKnowledgeProjectRef project) {
                        throw new AssertionError("retrieval must not be called");
                    }
                });

        assertThatThrownBy(() -> repository.findByWorkspace("workspace-a"))
                .hasMessageContaining("different workspace");
    }

    private static MissionLearningSource source() {
        return new MissionLearningSource("learning:1", "workspace-a", "mission:1", "closure:1",
                List.of("runtime-revision"), List.of("source:1"), List.of("evidence:1"));
    }

    private static LearningCandidate candidate() {
        return new LearningCandidate(
                "runtime-revision", KnowledgeRoute.WORKSPACE_SEMANTIC,
                "verified runtime revision", "runtime", "applicable to workspace-a", List.of(), List.of());
    }
}

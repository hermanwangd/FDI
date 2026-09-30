package com.featuredeliveryintelligence.fdi.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MulticaWorkspaceKnowledgeRepositoryTests {
    @Test
    void persistsApprovedKnowledgeThroughResolvedWorkspaceProjectAndReturnsCaptureReceipt() {
        SwarmKnowledgeGateway gateway = new SwarmKnowledgeGateway();
        WorkspaceKnowledgeProposal proposal = gateway.propose(source(), candidate());
        GovernedWorkspaceKnowledge governed = gateway.govern(
                proposal, KnowledgeGovernanceDecision.APPROVED, "test:decision:1", "workspace-reviewer",
                "test:policy:workspace-learning", "2026-09-28T00:00:00Z", proposal.evidenceRefs());
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
                                knowledge.proposal().proposalRef(), reverse(knowledge.proposal().sourceRefs()),
                                reverse(knowledge.proposal().evidenceRefs()), "2026-09-26T04:05:22Z");
            }

            @Override
            public WorkspaceKnowledgeRepository.ReadResult retrieve(WorkspaceKnowledgeProjectRef target) {
                assertThat(target).isEqualTo(project);
                List<WorkspaceKnowledgeRepository.Entry> entries = externalStore.stream()
                        .map(item -> new WorkspaceKnowledgeRepository.Entry(
                                "knowledge:" + item.proposalRef(), "readback-only", item.proposalRef(), 0,
                                item, null, "", "", 0, "", "UNKNOWN", null, null,
                                Map.of(), List.of(), ""))
                        .toList();
                return new WorkspaceKnowledgeRepository.ReadResult(
                        target.workspaceRef(), target.projectRef(), Instant.parse("2026-09-28T00:00:00Z"), entries);
            }
        };
        MulticaWorkspaceKnowledgeRepository repository = new MulticaWorkspaceKnowledgeRepository(
                ignored -> project, port);

        repository.save(governed);

        assertThat(repository.captureFor(proposal.proposalRef()))
                .get()
                .extracting(WorkspaceKnowledgeCaptureResult::knowledgeRef)
                .isEqualTo("knowledge:1");
        assertThat(repository.findByWorkspace("workspace-a").entries())
                .extracting(WorkspaceKnowledgeRepository.Entry::proposal).containsExactly(proposal);
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
                    public WorkspaceKnowledgeRepository.ReadResult retrieve(WorkspaceKnowledgeProjectRef project) {
                        throw new AssertionError("retrieval must not be called");
                    }
                });

        assertThatThrownBy(() -> repository.findByWorkspace("workspace-a"))
                .hasMessageContaining("different workspace");
    }

    @Test
    void rejectsCaptureReceiptWithMismatchedSourceOrEvidenceAttribution() {
        SwarmKnowledgeGateway gateway = new SwarmKnowledgeGateway();
        WorkspaceKnowledgeProposal proposal = gateway.propose(source(), candidate());
        GovernedWorkspaceKnowledge governed = gateway.govern(
                proposal, KnowledgeGovernanceDecision.APPROVED, "test:decision:1", "workspace-reviewer",
                "test:policy:workspace-learning", "2026-09-28T00:00:00Z", proposal.evidenceRefs());
        WorkspaceKnowledgeProjectRef project = new WorkspaceKnowledgeProjectRef(
                "workspace-a", "project-workspace-knowledge", "WorkspaceKnowledge");
        MulticaWorkspaceKnowledgeRepository repository = new MulticaWorkspaceKnowledgeRepository(
                ignored -> project,
                new MulticaWorkspaceKnowledgePort() {
                    @Override
                    public WorkspaceKnowledgeCaptureResult persist(
                            WorkspaceKnowledgeProjectRef target,
                            GovernedWorkspaceKnowledge knowledge) {
                        return new WorkspaceKnowledgeCaptureResult(
                                "capture:1", "knowledge:1", target.workspaceRef(), target.projectRef(),
                                knowledge.proposal().proposalRef(), knowledge.proposal().sourceRefs(),
                                List.of("evidence:unrelated"), "2026-09-26T04:05:22Z");
                    }

                    @Override
                    public WorkspaceKnowledgeRepository.ReadResult retrieve(WorkspaceKnowledgeProjectRef target) {
                        return new WorkspaceKnowledgeRepository.ReadResult(
                                target.workspaceRef(), target.projectRef(), Instant.parse("2026-09-28T00:00:00Z"), List.of());
                    }
                });

        assertThatThrownBy(() -> repository.save(governed))
                .hasMessageContaining("capture attribution")
                .hasMessageContaining("source/evidence");
    }

    private static MissionLearningSource source() {
        return new MissionLearningSource("learning:1", "workspace-a", "mission:1", "closure:1",
                List.of("runtime-revision"), List.of("source:1", "source:2"), List.of("evidence:1", "evidence:2"));
    }

    private static LearningCandidate candidate() {
        return new LearningCandidate(
                "runtime-revision", KnowledgeRoute.WORKSPACE_SEMANTIC,
                "verified runtime revision", "runtime", "applicable to workspace-a", List.of(), List.of());
    }

    private static List<String> reverse(List<String> refs) {
        var reversed = new ArrayList<>(refs);
        java.util.Collections.reverse(reversed);
        return reversed;
    }
}

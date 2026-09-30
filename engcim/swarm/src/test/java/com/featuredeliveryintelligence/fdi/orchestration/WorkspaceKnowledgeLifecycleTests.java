package com.featuredeliveryintelligence.fdi.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
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
        WorkspaceKnowledgeLifecycleResult result = build(
                new SwarmKnowledgeLifecycle(new SwarmKnowledgeGateway(), new InMemoryWorkspaceKnowledgeRepository()),
                source, candidate(), KnowledgeGovernanceDecision.APPROVED);

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
                proposal, KnowledgeGovernanceDecision.APPROVED, "test:decision:1", "workspace-reviewer",
                "test:policy:workspace-learning", "2026-09-28T00:00:00Z", proposal.evidenceRefs());
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
                    public WorkspaceKnowledgeRepository.ReadResult retrieve(WorkspaceKnowledgeProjectRef target) {
                        return readResult(target.workspaceRef(), target.projectRef(), externalStore);
                    }
                });

        WorkspaceKnowledgeLifecycleResult result = build(
                new SwarmKnowledgeLifecycle(gateway, repository), source, candidate(), KnowledgeGovernanceDecision.APPROVED);

        assertThat(result.routingDecision().route()).isEqualTo(KnowledgeRoute.WORKSPACE_SEMANTIC);
        assertThat(result.governedKnowledge().decision()).isEqualTo(KnowledgeGovernanceDecision.APPROVED);
        assertThat(result.captureReceipt()).get().extracting(WorkspaceKnowledgeCaptureResult::knowledgeRef)
                .isEqualTo("knowledge:runtime");
        assertThat(result.retrievedKnowledge()).containsExactly(result.proposal());
    }

    @Test
    void approvedKnowledgeRequiresExactReadAfterWriteMatch() {
        WorkspaceKnowledgeRepository repository = new WorkspaceKnowledgeRepository() {
            @Override
            public void save(GovernedWorkspaceKnowledge knowledge) {
                // Simulate a provider accepting the write without exposing the exact record on readback.
            }

            @Override
            public WorkspaceKnowledgeRepository.ReadResult findByWorkspace(String workspaceRef) {
                return readResult(workspaceRef, "test:knowledge-project", List.of());
            }
        };

        assertThatThrownBy(() -> new SwarmKnowledgeLifecycle(new SwarmKnowledgeGateway(), repository)
                .buildAndPersist(source(), candidate(), KnowledgeGovernanceDecision.APPROVED,
                        "test:decision:1", "workspace-reviewer", "test:policy:workspace-learning",
                        "2026-09-28T00:00:00Z", source().evidenceRefs()))
                .hasMessageContaining("read-after-write")
                .hasMessageContaining("proposal");
    }

    @Test
    void approvedKnowledgeRejectsReadbackWithSameRefButChangedProposal() {
        List<WorkspaceKnowledgeProposal> writes = new ArrayList<>();
        WorkspaceKnowledgeRepository repository = new WorkspaceKnowledgeRepository() {
            @Override
            public void save(GovernedWorkspaceKnowledge knowledge) {
                writes.add(knowledge.proposal());
            }

            @Override
            public WorkspaceKnowledgeRepository.ReadResult findByWorkspace(String workspaceRef) {
                WorkspaceKnowledgeProposal stored = writes.get(0);
                return readResult(workspaceRef, "test:knowledge-project", List.of(new WorkspaceKnowledgeProposal(
                        stored.proposalRef(), stored.workspaceRef(), stored.sourceRefs(), stored.knowledgeType(),
                        stored.statement() + " (changed)", stored.scope(), stored.applicability(),
                        stored.limitations(), stored.evidenceRefs(), stored.conflictRefs())));
            }
        };

        assertThatThrownBy(() -> build(
                new SwarmKnowledgeLifecycle(new SwarmKnowledgeGateway(), repository),
                source(), candidate(), KnowledgeGovernanceDecision.APPROVED))
                .hasMessageContaining("read-after-write")
                .hasMessageContaining("proposal");
    }

    @Test
    void approvedKnowledgeRejectsDuplicateReadbackForOneProposalRef() {
        List<WorkspaceKnowledgeProposal> writes = new ArrayList<>();
        WorkspaceKnowledgeRepository repository = new WorkspaceKnowledgeRepository() {
            @Override
            public void save(GovernedWorkspaceKnowledge knowledge) {
                writes.add(knowledge.proposal());
            }

            @Override
            public WorkspaceKnowledgeRepository.ReadResult findByWorkspace(String workspaceRef) {
                return readResult(workspaceRef, "test:knowledge-project", List.of(writes.get(0), writes.get(0)));
            }
        };

        assertThatThrownBy(() -> build(
                new SwarmKnowledgeLifecycle(new SwarmKnowledgeGateway(), repository),
                source(), candidate(), KnowledgeGovernanceDecision.APPROVED))
                .hasMessageContaining("read-after-write")
                .hasMessageContaining("proposal");
    }

    @Test
    void approvedKnowledgeRejectsForeignWorkspaceEntryInReadback() {
        List<WorkspaceKnowledgeProposal> writes = new ArrayList<>();
        WorkspaceKnowledgeProposal foreign = new WorkspaceKnowledgeProposal(
                "proposal:foreign", "workspace-b", List.of("repo@rev1"), KnowledgeType.SEMANTIC,
                "unrelated workspace method", "workspace-b", "repo@rev1", List.of(),
                List.of("evidence:foreign"), List.of());
        WorkspaceKnowledgeRepository repository = new WorkspaceKnowledgeRepository() {
            @Override
            public void save(GovernedWorkspaceKnowledge knowledge) {
                writes.add(knowledge.proposal());
            }

            @Override
            public WorkspaceKnowledgeRepository.ReadResult findByWorkspace(String workspaceRef) {
                return readResult(workspaceRef, "test:knowledge-project", List.of(writes.get(0), foreign));
            }
        };

        assertThatThrownBy(() -> build(
                new SwarmKnowledgeLifecycle(new SwarmKnowledgeGateway(), repository),
                source(), candidate(), KnowledgeGovernanceDecision.APPROVED))
                .isInstanceOf(RuntimeContractException.class)
                .hasMessageContaining("read-after-write")
                .hasMessageContaining("workspace boundaries");
    }

    @Test
    void nonWorkspaceRouteCannotEnterWorkspaceKnowledgeLifecycle() {
        LearningCandidate candidate = new LearningCandidate(
                "runtime-revision", KnowledgeRoute.SKILL_IMPROVEMENT,
                "improve runtime reasoning", "skill", "reusable", List.of(), List.of());

        assertThatThrownBy(() -> new SwarmKnowledgeLifecycle(
                new SwarmKnowledgeGateway(), new InMemoryWorkspaceKnowledgeRepository())
                .buildAndPersist(source(), candidate, KnowledgeGovernanceDecision.APPROVED,
                        "test:decision:1", "workspace-reviewer", "test:policy:workspace-learning",
                        "2026-09-28T00:00:00Z", source().evidenceRefs()))
                .hasMessageContaining("only WorkspaceKnowledge routes");
    }

    @Test
    void rejectedKnowledgeIsNotPersisted() {
        InMemoryWorkspaceKnowledgeRepository repository = new InMemoryWorkspaceKnowledgeRepository();
        SwarmKnowledgeLifecycle lifecycle = new SwarmKnowledgeLifecycle(new SwarmKnowledgeGateway(), repository);

        WorkspaceKnowledgeLifecycleResult result = build(
                lifecycle, source(), candidate(), KnowledgeGovernanceDecision.REJECTED);

        assertThat(result.governedKnowledge().decision()).isEqualTo(KnowledgeGovernanceDecision.REJECTED);
        assertThat(result.captureReceipt()).isEmpty();
        assertThat(result.retrievedKnowledge()).isEmpty();
        assertThat(repository.findByWorkspace("workspace-a").entries()).isEmpty();
    }

    @Test
    void deferredKnowledgeRemainsAProposalAndIsNotPersisted() {
        InMemoryWorkspaceKnowledgeRepository repository = new InMemoryWorkspaceKnowledgeRepository();
        SwarmKnowledgeLifecycle lifecycle = new SwarmKnowledgeLifecycle(new SwarmKnowledgeGateway(), repository);

        WorkspaceKnowledgeLifecycleResult result = build(
                lifecycle, source(), candidate(), KnowledgeGovernanceDecision.DEFERRED);

        assertThat(result.governedKnowledge().decision()).isEqualTo(KnowledgeGovernanceDecision.DEFERRED);
        assertThat(result.captureReceipt()).isEmpty();
        assertThat(repository.findByWorkspace("workspace-a").entries()).isEmpty();
    }

    @Test
    void conflictingProposalCannotBeApprovedThroughLifecycle() {
        InMemoryWorkspaceKnowledgeRepository repository = new InMemoryWorkspaceKnowledgeRepository();
        LearningCandidate conflicting = new LearningCandidate(
                "runtime-revision", KnowledgeRoute.WORKSPACE_SEMANTIC,
                "verified runtime revision", "runtime", "workspace-a", List.of(), List.of("conflict:source"));

        assertThatThrownBy(() -> new SwarmKnowledgeLifecycle(new SwarmKnowledgeGateway(), repository)
                .buildAndPersist(source(), conflicting, KnowledgeGovernanceDecision.APPROVED,
                        "test:decision:1", "workspace-reviewer", "test:policy:workspace-learning",
                        "2026-09-28T00:00:00Z", source().evidenceRefs()))
                .hasMessageContaining("conflicting WorkspaceKnowledge requires resolution");

        assertThat(repository.findByWorkspace("workspace-a").entries()).isEmpty();
    }

    @Test
    void missionHistoryCandidateIsRejectedBeforeWorkspacePersistence() {
        InMemoryWorkspaceKnowledgeRepository repository = new InMemoryWorkspaceKnowledgeRepository();
        LearningCandidate missionHistory = new LearningCandidate(
                "runtime-revision", KnowledgeRoute.MISSION_HISTORY,
                "one-off mission history", "mission", "workspace-a", List.of(), List.of());

        assertThatThrownBy(() -> new SwarmKnowledgeLifecycle(new SwarmKnowledgeGateway(), repository)
                .buildAndPersist(source(), missionHistory, KnowledgeGovernanceDecision.APPROVED,
                        "test:decision:1", "workspace-reviewer", "test:policy:workspace-learning",
                        "2026-09-28T00:00:00Z", source().evidenceRefs()))
                .hasMessageContaining("only WorkspaceKnowledge routes")
                .hasMessageContaining("MISSION_HISTORY");

        assertThat(repository.findByWorkspace("workspace-a").entries()).isEmpty();
    }

    private static WorkspaceKnowledgeRepository.ReadResult readResult(
            String workspaceRef, String projectRef, List<WorkspaceKnowledgeProposal> proposals) {
        List<WorkspaceKnowledgeRepository.Entry> entries = proposals.stream()
                .map(proposal -> new WorkspaceKnowledgeRepository.Entry(
                        "", "", proposal.proposalRef(), 0, proposal, null, "", "", 0, "", "UNKNOWN",
                        null, null, Map.of(), List.of(), ""))
                .toList();
        return new WorkspaceKnowledgeRepository.ReadResult(
                workspaceRef, projectRef, Instant.parse("2026-09-28T00:00:00Z"), entries);
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

    private static WorkspaceKnowledgeLifecycleResult build(
            SwarmKnowledgeLifecycle lifecycle,
            MissionLearningSource source,
            LearningCandidate candidate,
            KnowledgeGovernanceDecision decision) {
        return lifecycle.buildAndPersist(
                source, candidate, decision, "test:decision:1", "workspace-reviewer",
                decision == KnowledgeGovernanceDecision.DEFERRED ? "" : "test:policy:workspace-learning",
                "2026-09-28T00:00:00Z", source.evidenceRefs());
    }
}

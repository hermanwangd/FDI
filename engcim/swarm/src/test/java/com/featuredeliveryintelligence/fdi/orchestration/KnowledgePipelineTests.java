package com.featuredeliveryintelligence.fdi.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgePipelineTests {
    @Test
    void observationCorrelationAndConflictDetectionStayInSwarm() {
        SwarmKnowledgeGateway gateway = new SwarmKnowledgeGateway();
        MissionLearningSource source = source();
        KnowledgeObservation first = gateway.observe(source, candidate("verified runtime revision"));
        KnowledgeObservation second = new KnowledgeObservation(
                "observation:2", "workspace-a", "runtime-revision", "source:2", "different runtime revision", List.of("evidence:2"), ObservationState.PROVISIONAL);

        KnowledgeCorrelation correlation = gateway.correlate(List.of(first, second));

        assertThat(correlation.conflicts()).singleElement().satisfies(conflict -> {
            assertThat(conflict.subjectRef()).isEqualTo("runtime-revision");
            assertThat(conflict.observationRefs()).hasSize(2);
        });
    }

    @Test
    void unresolvedConflictCannotBeApprovedOrPersisted() {
        SwarmKnowledgeGateway gateway = new SwarmKnowledgeGateway();
        MissionLearningSource source = source();
        LearningCandidate candidate = candidate("verified runtime revision");
        KnowledgeObservation first = gateway.observe(source, candidate);
        KnowledgeObservation second = new KnowledgeObservation(
                "observation:2", "workspace-a", "runtime-revision", "source:2", "different runtime revision", List.of("evidence:2"), ObservationState.PROVISIONAL);
        WorkspaceKnowledgeProposal proposal = gateway.synthesize(
                "workspace-a", source, candidate, gateway.correlate(List.of(first, second)));

        assertThat(proposal.conflictRefs()).containsExactly("conflict:workspace-a:runtime-revision");
        assertThatThrownBy(() -> gateway.govern(
                proposal, KnowledgeGovernanceDecision.APPROVED, "test:decision:1", "reviewer",
                "test:policy:workspace-learning", "2026-09-28T00:00:00Z", proposal.evidenceRefs()))
                .hasMessageContaining("requires resolution");
    }

    @Test
    void approvedKnowledgeIsPersistedAndRetrievedOnlyForItsWorkspace() {
        SwarmKnowledgeGateway gateway = new SwarmKnowledgeGateway();
        WorkspaceKnowledgeProposal proposal = gateway.propose(source(), candidate("verified runtime revision"));
        GovernedWorkspaceKnowledge governed = gateway.govern(
                proposal, KnowledgeGovernanceDecision.APPROVED, "decision:17", "reviewer",
                "policy:workspace-learning", "2026-09-26T04:05:22Z", proposal.evidenceRefs());
        InMemoryWorkspaceKnowledgeRepository repository = new InMemoryWorkspaceKnowledgeRepository();

        gateway.persist(governed, repository);

        assertThat(gateway.readAfterWrite("workspace-a", repository).entries())
                .extracting(WorkspaceKnowledgeRepository.Entry::proposal).containsExactly(proposal);
        assertThat(gateway.readAfterWrite("workspace-b", repository).entries()).isEmpty();
    }

    @Test
    void approvedKnowledgeRequiresExplicitPolicyDecisionAndEvidence() {
        SwarmKnowledgeGateway gateway = new SwarmKnowledgeGateway();
        WorkspaceKnowledgeProposal proposal = gateway.propose(source(), candidate("verified runtime revision"));

        assertThatThrownBy(() -> gateway.govern(
                proposal, KnowledgeGovernanceDecision.APPROVED, "decision:17", "reviewer",
                "", "2026-09-26T04:05:22Z", proposal.evidenceRefs()))
                .hasMessageContaining("policyRef");
        assertThatThrownBy(() -> gateway.govern(
                proposal, KnowledgeGovernanceDecision.APPROVED, "decision:17", "reviewer",
                "policy:workspace-learning", "2026-09-26T04:05:22Z", List.of()))
                .hasMessageContaining("decisionEvidenceRefs");

        GovernedWorkspaceKnowledge governed = gateway.govern(
                proposal, KnowledgeGovernanceDecision.APPROVED, "decision:17", "reviewer",
                "policy:workspace-learning", "2026-09-26T04:05:22Z", proposal.evidenceRefs());

        assertThat(governed.decisionRef()).isEqualTo("decision:17");
        assertThat(governed.policyRef()).isEqualTo("policy:workspace-learning");
        assertThat(governed.decidedAt()).isEqualTo("2026-09-26T04:05:22Z");
        assertThat(governed.decisionEvidenceRefs()).containsExactlyElementsOf(proposal.evidenceRefs());
    }

    @Test
    void allNonTkmsDestinationsProduceTypedRoutingDecisions() {
        SwarmKnowledgeGateway gateway = new SwarmKnowledgeGateway();
        for (KnowledgeRoute route : KnowledgeRoute.values()) {
            if (route == KnowledgeRoute.DIRECT_TKMS_PUBLICATION) continue;
            KnowledgeRoutingDecision decision = gateway.route(source(), candidate(route, "route: " + route));
            assertThat(decision.route()).isEqualTo(route);
            if (route == KnowledgeRoute.MISSION_HISTORY) {
                assertThat(decision.reusable()).isFalse();
            }
            assertThat(decision.sourceRefs()).containsExactly("source:1");
            assertThat(decision.evidenceRefs()).containsExactly("evidence:1");
        }
    }

    private static MissionLearningSource source() {
        return new MissionLearningSource("learning:1", "workspace-a", "mission:1", "closure:1",
                List.of("runtime-revision"), List.of("source:1"), List.of("evidence:1"));
    }

    private static LearningCandidate candidate(String statement) {
        return candidate(KnowledgeRoute.WORKSPACE_SEMANTIC, statement);
    }

    private static LearningCandidate candidate(KnowledgeRoute route, String statement) {
        return new LearningCandidate("runtime-revision", route, statement, "runtime", "workspace-a", List.of("not live"), List.of());
    }
}

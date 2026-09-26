package com.featuredeliveryintelligence.fdi.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class LearningBoundaryTests {
    @Test
    void T07_closureAndEvidenceProduceMissionLearningSource() {
        MissionLearningSource source = MissionLearningSourceFactory.from(summary("workspace-a", "mission:req-1"));

        assertThat(source.learningSourceRef()).isEqualTo("learning:mission:req-1:closure-1");
        assertThat(source.workspaceRef()).isEqualTo("workspace-a");
        assertThat(source.missionRef()).isEqualTo("mission:req-1");
        assertThat(source.evidenceRefs()).containsExactly("evidence:1");
    }

    @Test
    void T08_missionLearningSourceProducesWorkspaceKnowledgeProposal() {
        MissionLearningSource source = MissionLearningSourceFactory.from(summary("workspace-a", "mission:req-1"));
        LearningCandidate candidate = new LearningCandidate(
                KnowledgeRoute.WORKSPACE_PROCEDURAL, "Use the verified binding revision", "workspace execution", "same runtime", "requires governance review", List.of());

        WorkspaceKnowledgeProposal proposal = new SwarmKnowledgeGateway().propose(source, candidate);

        assertThat(proposal.workspaceRef()).isEqualTo(source.workspaceRef());
        assertThat(proposal.knowledgeType()).isEqualTo(KnowledgeType.PROCEDURAL);
        assertThat(proposal.sourceRefs()).isEqualTo(source.sourceRefs());
        assertThat(proposal.evidenceRefs()).isEqualTo(source.evidenceRefs());
    }

    @Test
    void T09_crossWorkspaceLearningIsRejected() {
        MissionLearningSource source = MissionLearningSourceFactory.from(summary("workspace-b", "mission:req-2"));
        LearningCandidate candidate = new LearningCandidate(
                KnowledgeRoute.WORKSPACE_SEMANTIC, "Fact", "workspace", "workspace-b", "", List.of());

        assertThatThrownBy(() -> new SwarmKnowledgeGateway().propose("workspace-a", source, candidate))
                .hasMessageContaining("workspaceRef");
    }

    @Test
    void T10_productTruthCandidateDoesNotBecomeWorkspaceKnowledge() {
        MissionLearningSource source = MissionLearningSourceFactory.from(summary("workspace-a", "mission:req-1"));
        LearningCandidate candidate = new LearningCandidate(
                KnowledgeRoute.PRODUCT_KNOWLEDGE_PROPOSAL, "Candidate product fact", "product", "product review", "unverified", List.of());
        SwarmKnowledgeGateway gateway = new SwarmKnowledgeGateway();

        assertThat(gateway.classify(candidate)).isEqualTo(KnowledgeRoute.PRODUCT_KNOWLEDGE_PROPOSAL);
        assertThatThrownBy(() -> gateway.propose(source, candidate)).hasMessageContaining("Product Knowledge");
    }

    @Test
    void T11_directMissionOrSwarmToTkmsPublicationIsRejected() {
        LearningCandidate candidate = new LearningCandidate(
                KnowledgeRoute.DIRECT_TKMS_PUBLICATION, "Publish", "global", "none", "", List.of());

        assertThatThrownBy(() -> new SwarmKnowledgeGateway().classify(candidate))
                .hasMessageContaining("tKMS");
    }

    private static MissionClosureSummary summary(String workspaceRef, String missionRef) {
        return new MissionClosureSummary(
                "closure-1", missionRef, workspaceRef,
                List.of("subject:1"), List.of("source:1"), List.of("evidence:1"));
    }
}

package com.featuredeliveryintelligence.fdi.orchestration;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ENGCIM Swarm repository adapter for a durable external WorkspaceKnowledge
 * project. It owns no local knowledge store; the supplied Multica port owns the
 * external persistence primitive and supplies the read envelope.
 */
public final class MulticaWorkspaceKnowledgeRepository implements WorkspaceKnowledgeCaptureReceiptRepository {
    private final WorkspaceKnowledgeProjectResolver projectResolver;
    private final MulticaWorkspaceKnowledgePort multica;
    private final Map<String, WorkspaceKnowledgeCaptureResult> captures = new LinkedHashMap<>();

    public MulticaWorkspaceKnowledgeRepository(
            WorkspaceKnowledgeProjectResolver projectResolver,
            MulticaWorkspaceKnowledgePort multica) {
        this.projectResolver = Objects.requireNonNull(projectResolver, "projectResolver is required");
        this.multica = Objects.requireNonNull(multica, "multica is required");
    }

    @Override
    public void save(GovernedWorkspaceKnowledge knowledge) {
        Objects.requireNonNull(knowledge, "knowledge is required");
        if (knowledge.decision() != KnowledgeGovernanceDecision.APPROVED) {
            throw new RuntimeContractException("only approved WorkspaceKnowledge may be persisted");
        }
        WorkspaceKnowledgeProposal proposal = knowledge.proposal();
        WorkspaceKnowledgeProjectRef project = resolve(proposal.workspaceRef());
        WorkspaceKnowledgeCaptureResult capture = Objects.requireNonNull(
                multica.persist(project, knowledge), "Multica persistence returned no capture result");
        if (!proposal.workspaceRef().equals(capture.workspaceRef())
                || !project.projectRef().equals(capture.projectRef())
                || !proposal.proposalRef().equals(capture.proposalRef())) {
            throw new RuntimeContractException("WorkspaceKnowledge capture attribution does not match the proposal identity");
        }
        if (!sameRefs(proposal.sourceRefs(), capture.sourceRefs())
                || !sameRefs(proposal.evidenceRefs(), capture.evidenceRefs())) {
            throw new RuntimeContractException(
                    "WorkspaceKnowledge capture attribution source/evidence refs do not match the proposal");
        }
        captures.put(proposal.proposalRef(), capture);
    }

    @Override
    public WorkspaceKnowledgeRepository.ReadResult findByWorkspace(String workspaceRef) {
        WorkspaceKnowledgeProjectRef project = resolve(workspaceRef);
        return Objects.requireNonNull(multica.retrieve(project), "Multica retrieval returned no result");
    }

    @Override
    public java.util.Optional<WorkspaceKnowledgeCaptureResult> captureFor(String proposalRef) {
        if (proposalRef == null || proposalRef.isBlank()) throw new IllegalArgumentException("proposalRef is required");
        return java.util.Optional.ofNullable(captures.get(proposalRef));
    }

    private WorkspaceKnowledgeProjectRef resolve(String workspaceRef) {
        if (workspaceRef == null || workspaceRef.isBlank()) throw new IllegalArgumentException("workspaceRef is required");
        WorkspaceKnowledgeProjectRef project = Objects.requireNonNull(
                projectResolver.resolve(workspaceRef), "WorkspaceKnowledge project resolution returned no project");
        if (!workspaceRef.equals(project.workspaceRef())) {
            throw new RuntimeContractException("WorkspaceKnowledge project belongs to a different workspace");
        }
        return project;
    }

    private static boolean sameRefs(List<String> expected, List<String> actual) {
        // Receipt attribution preserves the reference multiset; provider ordering is not semantic here.
        return expected.size() == actual.size()
                && expected.stream().sorted().toList().equals(actual.stream().sorted().toList());
    }
}

package com.featuredeliveryintelligence.fdi.orchestration;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Test fixture for capture/readback contracts; it does not provide consumer-eligible provider evidence. */
public final class InMemoryWorkspaceKnowledgeRepository implements WorkspaceKnowledgeRepository {
    private static final Instant FETCHED_AT = Instant.parse("2026-09-28T00:00:00Z");
    private final Map<String, Map<String, GovernedWorkspaceKnowledge>> byWorkspace = new LinkedHashMap<>();

    @Override
    public void save(GovernedWorkspaceKnowledge knowledge) {
        if (knowledge == null || knowledge.decision() != KnowledgeGovernanceDecision.APPROVED) {
            throw new RuntimeContractException("only approved WorkspaceKnowledge may be persisted");
        }
        WorkspaceKnowledgeProposal proposal = knowledge.proposal();
        byWorkspace.computeIfAbsent(proposal.workspaceRef(), ignored -> new LinkedHashMap<>())
                .put(proposal.proposalRef(), knowledge);
    }

    @Override
    public ReadResult findByWorkspace(String workspaceRef) {
        if (workspaceRef == null || workspaceRef.isBlank()) throw new IllegalArgumentException("workspaceRef is required");
        List<Entry> entries = byWorkspace.getOrDefault(workspaceRef, Map.of()).values().stream()
                .map(InMemoryWorkspaceKnowledgeRepository::readbackEntry)
                .toList();
        return new ReadResult(workspaceRef, "in-memory:workspace-knowledge", FETCHED_AT, entries);
    }

    private static Entry readbackEntry(GovernedWorkspaceKnowledge governed) {
        WorkspaceKnowledgeProposal proposal = governed.proposal();
        String digest = SwarmKnowledgeGateway.proposalDigest(proposal);
        return new Entry(
                "in-memory:" + proposal.proposalRef(), "in-memory:readback-v1", proposal.proposalRef(), 1,
                proposal, governed, digest, proposal.proposalRef(), 1, digest, "CURRENT",
                FETCHED_AT, Instant.MAX, Map.of(), List.of(), "IN_MEMORY_TEST_ONLY");
    }
}

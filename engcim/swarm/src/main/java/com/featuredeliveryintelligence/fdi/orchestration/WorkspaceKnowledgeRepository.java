package com.featuredeliveryintelligence.fdi.orchestration;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** WorkspaceKnowledge persistence and fresh provider-read boundary. */
public interface WorkspaceKnowledgeRepository {
    void save(GovernedWorkspaceKnowledge knowledge);

    ReadResult findByWorkspace(String workspaceRef);

    /** Fresh provider read envelope; proposal content remains unchanged inside each entry. */
    record ReadResult(String workspaceRef, String projectRef, Instant fetchedAt, List<Entry> entries) {
        public ReadResult {
            require(workspaceRef, "workspaceRef");
            require(projectRef, "projectRef");
            if (fetchedAt == null) throw new IllegalArgumentException("fetchedAt is required");
            entries = List.copyOf(entries == null ? List.of() : entries);
        }
    }

    /** Provider evidence accompanying one proposal. Missing evidence is preserved for exclusion. */
    record Entry(
            String knowledgeRef,
            String providerRevision,
            String recordKey,
            int recordVersion,
            WorkspaceKnowledgeProposal proposal,
            GovernedWorkspaceKnowledge governance,
            String proposalDigest,
            String decisionRecordKey,
            int decisionRecordVersion,
            String decisionProposalDigest,
            String lifecycle,
            Instant observedAt,
            Instant validUntil,
            Map<String, String> repositoryRevisions,
            List<String> applicableProjectRefs,
            String visibility) {
        public Entry {
            repositoryRevisions = Map.copyOf(repositoryRevisions == null ? Map.of() : repositoryRevisions);
            applicableProjectRefs = List.copyOf(applicableProjectRefs == null ? List.of() : applicableProjectRefs);
        }
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}

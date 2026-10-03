package com.featuredeliveryintelligence.fdi.orchestration;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** WorkspaceKnowledge persistence and fresh provider-read boundary. */
public interface WorkspaceKnowledgeRepository {
    void save(GovernedWorkspaceKnowledge knowledge);

    ReadResult findByWorkspace(String workspaceRef);

    /** A provider outage is distinct from a successful empty read; optional knowledge may fall back explicitly. */
    class ProviderUnavailableException extends IllegalStateException {
        public ProviderUnavailableException(String message) { super(message); }

        public ProviderUnavailableException(String message, Throwable cause) { super(message, cause); }
    }

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
            String visibility,
            /** MultiCA record-envelope conflictRefs; kept separate from proposal conflictRefs and its digest. */
            @JsonInclude(JsonInclude.Include.NON_EMPTY)
            List<String> sourceConflictRefs,
            /** Record-level limitations outside the unchanged proposal and its digest. */
            @JsonInclude(JsonInclude.Include.NON_EMPTY)
            List<String> sourceLimitations) {
        public Entry {
            repositoryRevisions = Map.copyOf(repositoryRevisions == null ? Map.of() : repositoryRevisions);
            applicableProjectRefs = List.copyOf(applicableProjectRefs == null ? List.of() : applicableProjectRefs);
            sourceConflictRefs = List.copyOf(sourceConflictRefs == null ? List.of() : sourceConflictRefs);
            sourceLimitations = List.copyOf(sourceLimitations == null ? List.of() : sourceLimitations);
        }

        /** Backward-compatible constructor for providers that preserve envelope conflicts only. */
        public Entry(
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
                String visibility,
                List<String> sourceConflictRefs) {
            this(knowledgeRef, providerRevision, recordKey, recordVersion, proposal, governance, proposalDigest,
                    decisionRecordKey, decisionRecordVersion, decisionProposalDigest, lifecycle, observedAt, validUntil,
                    repositoryRevisions, applicableProjectRefs, visibility, sourceConflictRefs, List.of());
        }

        /** Backward-compatible constructor for providers that have no envelope-level conflict field. */
        public Entry(
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
            this(knowledgeRef, providerRevision, recordKey, recordVersion, proposal, governance, proposalDigest,
                    decisionRecordKey, decisionRecordVersion, decisionProposalDigest, lifecycle, observedAt, validUntil,
                    repositoryRevisions, applicableProjectRefs, visibility, List.of(), List.of());
        }
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}

package com.featuredeliveryintelligence.fdi.orchestration;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Swarm Core facade for extraction, synthesis, governance, routing, and retrieval. */
public final class SwarmKnowledgeGateway {
    private static final com.fasterxml.jackson.databind.ObjectMapper CANONICAL_JSON = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();
    private final Clock clock;

    public SwarmKnowledgeGateway() {
        this(Clock.systemUTC());
    }

    SwarmKnowledgeGateway(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public KnowledgeRoute classify(LearningCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate is required");
        if (candidate.route() == KnowledgeRoute.DIRECT_TKMS_PUBLICATION) {
            throw new RuntimeContractException("direct Mission/Swarm-to-tKMS publication is prohibited");
        }
        return candidate.route();
    }

    public KnowledgeRoutingDecision route(MissionLearningSource source, LearningCandidate candidate) {
        Objects.requireNonNull(source, "source is required");
        KnowledgeRoute route = classify(candidate);
        return new KnowledgeRoutingDecision(
                route,
                source.workspaceRef(),
                source.missionRef(),
                source.sourceRefs(),
                source.evidenceRefs(),
                route != KnowledgeRoute.MISSION_HISTORY,
                rationale(route));
    }

    public KnowledgeObservation observe(MissionLearningSource source, LearningCandidate candidate) {
        route(source, candidate);
        return new KnowledgeObservation(
                "observation:" + source.learningSourceRef() + ":" + candidate.subjectRef(),
                source.workspaceRef(),
                candidate.subjectRef(),
                source.sourceRefs().get(0),
                candidate.statement(),
                source.evidenceRefs(),
                ObservationState.PROVISIONAL);
    }

    public KnowledgeCorrelation correlate(List<KnowledgeObservation> observations) {
        if (observations == null || observations.isEmpty()) throw new IllegalArgumentException("observations are required");
        String workspaceRef = observations.get(0).workspaceRef();
        Map<String, List<KnowledgeObservation>> bySubject = new LinkedHashMap<>();
        for (KnowledgeObservation observation : observations) {
            if (!workspaceRef.equals(observation.workspaceRef())) {
                throw new RuntimeContractException("observations from different workspaces cannot be correlated");
            }
            bySubject.computeIfAbsent(observation.subjectRef(), ignored -> new ArrayList<>()).add(observation);
        }
        var conflicts = new ArrayList<KnowledgeConflict>();
        for (Map.Entry<String, List<KnowledgeObservation>> entry : bySubject.entrySet()) {
            var distinctStatements = entry.getValue().stream().map(KnowledgeObservation::statement).distinct().toList();
            if (distinctStatements.size() > 1) {
                conflicts.add(new KnowledgeConflict(
                        "conflict:" + workspaceRef + ":" + entry.getKey(),
                        workspaceRef,
                        entry.getKey(),
                        entry.getValue().stream().map(KnowledgeObservation::observationRef).toList(),
                        distinctStatements));
            }
        }
        return new KnowledgeCorrelation(workspaceRef, observations, conflicts);
    }

    public WorkspaceKnowledgeProposal propose(MissionLearningSource source, LearningCandidate candidate) {
        return propose(source.workspaceRef(), source, candidate);
    }

    public WorkspaceKnowledgeProposal propose(
            String targetWorkspaceRef, MissionLearningSource source, LearningCandidate candidate) {
        KnowledgeObservation observation = observe(source, candidate);
        return synthesize(targetWorkspaceRef, source, candidate, correlate(List.of(observation)));
    }

    public ProductKnowledgeProposal productKnowledgeProposal(
            MissionLearningSource source, LearningCandidate candidate) {
        if (route(source, candidate).route() != KnowledgeRoute.PRODUCT_KNOWLEDGE_PROPOSAL) {
            throw new RuntimeContractException("candidate is not routed to Product Knowledge");
        }
        return new ProductKnowledgeProposal(
                "product-proposal:" + source.learningSourceRef() + ":" + candidate.subjectRef(),
                source.workspaceRef(),
                source.missionRef(),
                source.sourceRefs(),
                source.evidenceRefs(),
                candidate.statement(),
                candidate.scope(),
                candidate.limitations(),
                true);
    }

    public WorkspaceKnowledgeProposal synthesize(
            String targetWorkspaceRef,
            MissionLearningSource source,
            LearningCandidate candidate,
            KnowledgeCorrelation correlation) {
        Objects.requireNonNull(source, "source is required");
        Objects.requireNonNull(correlation, "correlation is required");
        if (targetWorkspaceRef == null || targetWorkspaceRef.isBlank()) {
            throw new IllegalArgumentException("targetWorkspaceRef is required");
        }
        if (!targetWorkspaceRef.equals(source.workspaceRef()) || !targetWorkspaceRef.equals(correlation.workspaceRef())) {
            throw new RuntimeContractException("workspaceRef does not match synthesis target");
        }
        KnowledgeRoute route = route(source, candidate).route();
        if (route == KnowledgeRoute.PRODUCT_KNOWLEDGE_PROPOSAL) {
            throw new RuntimeContractException("Product truth must remain a Product Knowledge proposal");
        }
        KnowledgeType type = switch (route) {
            case WORKSPACE_SEMANTIC -> KnowledgeType.SEMANTIC;
            case WORKSPACE_PROCEDURAL -> KnowledgeType.PROCEDURAL;
            default -> throw new RuntimeContractException("route does not create WorkspaceKnowledge: " + route);
        };
        List<String> conflictRefs = new ArrayList<>(candidate.conflictRefs());
        correlation.conflicts().stream().map(KnowledgeConflict::conflictRef).forEach(conflictRefs::add);
        return new WorkspaceKnowledgeProposal(
                "proposal:" + source.learningSourceRef() + ":" + candidate.subjectRef(),
                source.workspaceRef(),
                source.sourceRefs(),
                type,
                candidate.statement(),
                candidate.scope(),
                candidate.applicability(),
                candidate.limitations(),
                source.evidenceRefs(),
                conflictRefs.stream().distinct().toList());
    }

    public GovernedWorkspaceKnowledge govern(
            WorkspaceKnowledgeProposal proposal,
            KnowledgeGovernanceDecision decision,
            String decisionRef,
            String actor,
            String policyRef,
            String decidedAt,
            List<String> decisionEvidenceRefs) {
        Objects.requireNonNull(proposal, "proposal is required");
        Objects.requireNonNull(decision, "decision is required");
        if (decision == KnowledgeGovernanceDecision.APPROVED && !proposal.conflictRefs().isEmpty()) {
            throw new RuntimeContractException("conflicting WorkspaceKnowledge requires resolution before approval");
        }
        return new GovernedWorkspaceKnowledge(
                proposal, decision, decisionRef, actor, policyRef, decidedAt, decisionEvidenceRefs);
    }

    public void persist(GovernedWorkspaceKnowledge knowledge, WorkspaceKnowledgeRepository repository) {
        Objects.requireNonNull(repository, "repository is required").save(knowledge);
    }

    /** Provider read used only by write-after-read verification; consumers must use retrieveForConsumer. */
    WorkspaceKnowledgeRepository.ReadResult readAfterWrite(
            String workspaceRef, WorkspaceKnowledgeRepository repository) {
        if (workspaceRef == null || workspaceRef.isBlank()) {
            throw new IllegalArgumentException("workspaceRef is required");
        }
        WorkspaceKnowledgeRepository provider = Objects.requireNonNull(repository, "repository is required");
        WorkspaceKnowledgeRepository.ReadResult read = Objects.requireNonNull(
                provider.findByWorkspace(workspaceRef), "WorkspaceKnowledge read-after-write returned no result");
        if (!workspaceRef.equals(read.workspaceRef())) {
            throw new RuntimeContractException("WorkspaceKnowledge read-after-write crossed workspace boundaries");
        }
        if (read.entries().stream().anyMatch(entry -> entry.proposal() == null)) {
            throw new RuntimeContractException("WorkspaceKnowledge read-after-write returned an entry without proposal content");
        }
        if (read.entries().stream().anyMatch(entry -> !workspaceRef.equals(entry.proposal().workspaceRef()))) {
            throw new RuntimeContractException("WorkspaceKnowledge read-after-write entry crossed workspace boundaries");
        }
        return read;
    }

    /**
     * Performs a provider read for this consumer invocation and returns only eligible context.
     * An empty selection is valid because optional WorkspaceKnowledge does not gate unrelated work.
     */
    public EligibleConsumerContext retrieveForConsumer(
            ConsumerRequest request, WorkspaceKnowledgeRepository repository) {
        Objects.requireNonNull(request, "request is required");
        WorkspaceKnowledgeRepository provider = Objects.requireNonNull(repository, "repository is required");
        WorkspaceKnowledgeRepository.ReadResult read = Objects.requireNonNull(
                provider.findByWorkspace(request.workspaceRef()), "WorkspaceKnowledge provider read returned no result");
        Instant evaluatedAt = clock.instant();
        var selected = new ArrayList<WorkspaceKnowledgeRepository.Entry>();
        var excluded = new ArrayList<ContextExclusion>();

        if (!request.workspaceRef().equals(read.workspaceRef())
                || !request.knowledgeProjectRef().equals(read.projectRef())) {
            excluded.add(new ContextExclusion("", read.projectRef(), ContextExclusionReason.READ_SCOPE_MISMATCH));
            return new EligibleConsumerContext(
                    request, read.workspaceRef(), read.projectRef(), read.fetchedAt(), selected, excluded);
        }

        Map<String, Long> recordsPerKey = read.entries().stream()
                .filter(entry -> entry.recordKey() != null && !entry.recordKey().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(
                        WorkspaceKnowledgeRepository.Entry::recordKey, java.util.stream.Collectors.counting()));
        for (WorkspaceKnowledgeRepository.Entry entry : read.entries()) {
            ContextExclusionReason reason = exclusionReason(request, read, entry, recordsPerKey, evaluatedAt);
            if (reason == null) selected.add(entry);
            else excluded.add(new ContextExclusion(entry.recordKey(), entry.knowledgeRef(), reason));
        }
        return new EligibleConsumerContext(
                request, read.workspaceRef(), read.projectRef(), read.fetchedAt(), selected, excluded);
    }

    /** Builds consumer feedback evidence; ADOPTED alone never marks the method effective. */
    public ConsumerFeedback buildConsumerFeedback(
            EligibleConsumerContext context,
            String recordKey,
            int recordVersion,
            ConsumerDisposition disposition,
            String reason,
            String actionRef,
            String resultRef,
            Outcome outcome,
            List<String> outcomeEvidenceRefs) {
        Objects.requireNonNull(context, "context is required");
        Objects.requireNonNull(disposition, "disposition is required");
        Objects.requireNonNull(outcome, "outcome is required");
        require(recordKey, "recordKey");
        if (recordVersion <= 0) throw new IllegalArgumentException("recordVersion must be positive");
        WorkspaceKnowledgeRepository.Entry selected = context.selected().stream()
                .filter(entry -> entry.recordKey().equals(recordKey) && entry.recordVersion() == recordVersion)
                .findFirst()
                .orElseThrow(() -> new RuntimeContractException("WorkspaceKnowledge record was not selected for this consumer"));
        if (disposition == ConsumerDisposition.ADOPTED) {
            require(reason, "adoption reason");
            require(actionRef, "actionRef");
            require(resultRef, "resultRef");
        } else {
            require(reason, "rejection reason");
            if (outcome != Outcome.UNASSESSED) {
                throw new RuntimeContractException("a rejected method has no effectiveness outcome");
            }
        }
        List<String> evidenceRefs = List.copyOf(outcomeEvidenceRefs == null ? List.of() : outcomeEvidenceRefs);
        if (outcome != Outcome.UNASSESSED && evidenceRefs.isEmpty()) {
            throw new IllegalArgumentException("outcomeEvidenceRefs are required for an assessed outcome");
        }
        return new ConsumerFeedback(
                context.request().missionRef(), context.request().workspaceRef(),
                context.readWorkspaceRef(), context.readProjectRef(), context.request().consumerProjectRef(), context.fetchedAt(),
                selected.recordKey(), selected.recordVersion(), selected.proposalDigest(),
                selected.knowledgeRef(), selected.providerRevision(), selected.governance().decisionRef(),
                disposition, reason, actionRef, resultRef, outcome, evidenceRefs);
    }

    public static String proposalDigest(WorkspaceKnowledgeProposal proposal) {
        Objects.requireNonNull(proposal, "proposal is required");
        try {
            byte[] canonicalJson = CANONICAL_JSON.writeValueAsBytes(proposal);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonicalJson));
        } catch (java.io.IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("could not calculate WorkspaceKnowledge proposal digest", exception);
        }
    }

    private static ContextExclusionReason exclusionReason(
            ConsumerRequest request,
            WorkspaceKnowledgeRepository.ReadResult read,
            WorkspaceKnowledgeRepository.Entry entry,
            Map<String, Long> recordsPerKey,
            Instant evaluatedAt) {
        if (entry.recordKey() == null || entry.recordKey().isBlank()
                || entry.recordVersion() <= 0 || entry.knowledgeRef() == null || entry.knowledgeRef().isBlank()
                || entry.providerRevision() == null || entry.providerRevision().isBlank()
                || entry.proposal() == null) return ContextExclusionReason.IDENTITY_INCOMPLETE;
        WorkspaceKnowledgeProposal proposal = entry.proposal();
        if (!entry.recordKey().equals(proposal.proposalRef())) return ContextExclusionReason.IDENTITY_MISMATCH;
        if (!request.workspaceRef().equals(proposal.workspaceRef())) return ContextExclusionReason.WRONG_WORKSPACE;
        if (recordsPerKey.getOrDefault(entry.recordKey(), 0L) > 1) return ContextExclusionReason.DUPLICATE_RECORD_KEY;
        if (!entry.applicableProjectRefs().contains(request.consumerProjectRef())) {
            return ContextExclusionReason.PROJECT_NOT_APPLICABLE;
        }
        if (!"WORKSPACE_AUTHORIZED".equals(entry.visibility())) return ContextExclusionReason.VISIBILITY_NOT_AUTHORIZED;
        if (entry.governance() == null
                || !proposal.equals(entry.governance().proposal())) return ContextExclusionReason.GOVERNANCE_INCOMPLETE;
        if (entry.governance().decision() != KnowledgeGovernanceDecision.APPROVED) {
            return ContextExclusionReason.NOT_APPROVED;
        }
        if (!"CURRENT".equals(entry.lifecycle())) return ContextExclusionReason.LIFECYCLE_NOT_CURRENT;
        if (!proposal.conflictRefs().isEmpty()) return ContextExclusionReason.UNRESOLVED_CONFLICT;
        if (entry.proposalDigest() == null || entry.proposalDigest().isBlank()) return ContextExclusionReason.DIGEST_MISSING;
        String recomputedDigest = proposalDigest(proposal);
        if (!entry.proposalDigest().equals(recomputedDigest)) return ContextExclusionReason.DIGEST_MISMATCH;
        if (!entry.recordKey().equals(entry.decisionRecordKey())
                || entry.recordVersion() != entry.decisionRecordVersion()
                || !entry.proposalDigest().equals(entry.decisionProposalDigest())) {
            return ContextExclusionReason.DECISION_BINDING_MISMATCH;
        }
        if (entry.governance().decisionRef().isBlank() || entry.governance().decidedBy().isBlank()
                || entry.governance().policyRef().isBlank() || entry.governance().decisionEvidenceRefs().isEmpty()) {
            return ContextExclusionReason.GOVERNANCE_INCOMPLETE;
        }
        if (entry.observedAt() == null || entry.validUntil() == null
                || entry.observedAt().isAfter(read.fetchedAt()) || entry.repositoryRevisions().isEmpty()) {
            return ContextExclusionReason.FRESHNESS_UNKNOWN;
        }
        if (entry.validUntil().isBefore(evaluatedAt)) return ContextExclusionReason.STALE;
        if (!entry.repositoryRevisions().equals(request.currentRepositoryRevisions())) {
            return ContextExclusionReason.SOURCE_REVISION_MISMATCH;
        }
        return null;
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }

    /** The revision map is the exact repository set relevant to this consumer request. */
    public record ConsumerRequest(
            String missionRef,
            String workspaceRef,
            String knowledgeProjectRef,
            String consumerProjectRef,
            Map<String, String> currentRepositoryRevisions) {
        public ConsumerRequest {
            require(missionRef, "missionRef");
            require(workspaceRef, "workspaceRef");
            require(knowledgeProjectRef, "knowledgeProjectRef");
            require(consumerProjectRef, "consumerProjectRef");
            currentRepositoryRevisions = Map.copyOf(currentRepositoryRevisions == null ? Map.of() : currentRepositoryRevisions);
        }
    }

    public static final class EligibleConsumerContext {
        private final ConsumerRequest request;
        private final String readWorkspaceRef;
        private final String readProjectRef;
        private final Instant fetchedAt;
        private final List<WorkspaceKnowledgeRepository.Entry> selected;
        private final List<ContextExclusion> excluded;

        private EligibleConsumerContext(
                ConsumerRequest request,
                String readWorkspaceRef,
                String readProjectRef,
                Instant fetchedAt,
                List<WorkspaceKnowledgeRepository.Entry> selected,
                List<ContextExclusion> excluded) {
            this.request = Objects.requireNonNull(request, "request is required");
            this.readWorkspaceRef = Objects.requireNonNull(readWorkspaceRef, "readWorkspaceRef is required");
            this.readProjectRef = Objects.requireNonNull(readProjectRef, "readProjectRef is required");
            this.fetchedAt = Objects.requireNonNull(fetchedAt, "fetchedAt is required");
            this.selected = List.copyOf(selected == null ? List.of() : selected);
            this.excluded = List.copyOf(excluded == null ? List.of() : excluded);
        }

        public ConsumerRequest request() { return request; }

        public String readWorkspaceRef() { return readWorkspaceRef; }

        public String readProjectRef() { return readProjectRef; }

        public Instant fetchedAt() { return fetchedAt; }

        public List<WorkspaceKnowledgeRepository.Entry> selected() { return selected; }

        public List<ContextExclusion> excluded() { return excluded; }

        public boolean mayProceedWithoutKnowledge() {
            return true;
        }
    }

    public record ContextExclusion(String recordKey, String knowledgeRef, ContextExclusionReason reason) {
        public ContextExclusion {
            Objects.requireNonNull(reason, "reason is required");
            recordKey = recordKey == null ? "" : recordKey;
            knowledgeRef = knowledgeRef == null ? "" : knowledgeRef;
        }
    }

    public enum ContextExclusionReason {
        READ_SCOPE_MISMATCH,
        IDENTITY_INCOMPLETE,
        IDENTITY_MISMATCH,
        WRONG_WORKSPACE,
        DUPLICATE_RECORD_KEY,
        PROJECT_NOT_APPLICABLE,
        VISIBILITY_NOT_AUTHORIZED,
        GOVERNANCE_INCOMPLETE,
        NOT_APPROVED,
        LIFECYCLE_NOT_CURRENT,
        UNRESOLVED_CONFLICT,
        DIGEST_MISSING,
        DIGEST_MISMATCH,
        DECISION_BINDING_MISMATCH,
        FRESHNESS_UNKNOWN,
        STALE,
        SOURCE_REVISION_MISMATCH
    }

    public enum ConsumerDisposition { ADOPTED, REJECTED }

    public enum Outcome { UNASSESSED, EFFECTIVE, INEFFECTIVE }

    public record ConsumerFeedback(
            String missionRef,
            String workspaceRef,
            String readWorkspaceRef,
            String readProjectRef,
            String consumerProjectRef,
            Instant fetchedAt,
            String recordKey,
            int recordVersion,
            String proposalDigest,
            String knowledgeRef,
            String providerRevision,
            String decisionRef,
            ConsumerDisposition disposition,
            String reason,
            String actionRef,
            String resultRef,
            Outcome outcome,
            List<String> outcomeEvidenceRefs) {
        public ConsumerFeedback {
            outcomeEvidenceRefs = List.copyOf(outcomeEvidenceRefs == null ? List.of() : outcomeEvidenceRefs);
            require(missionRef, "missionRef");
            require(workspaceRef, "workspaceRef");
            require(readWorkspaceRef, "readWorkspaceRef");
            require(readProjectRef, "readProjectRef");
            require(consumerProjectRef, "consumerProjectRef");
            Objects.requireNonNull(fetchedAt, "fetchedAt is required");
            require(recordKey, "recordKey");
            if (recordVersion <= 0) throw new IllegalArgumentException("recordVersion must be positive");
            require(proposalDigest, "proposalDigest");
            require(knowledgeRef, "knowledgeRef");
            require(providerRevision, "providerRevision");
            require(decisionRef, "decisionRef");
            Objects.requireNonNull(disposition, "disposition is required");
            Objects.requireNonNull(outcome, "outcome is required");
            if (disposition == ConsumerDisposition.ADOPTED) {
                require(reason, "adoption reason");
                require(actionRef, "actionRef");
                require(resultRef, "resultRef");
            } else {
                require(reason, "rejection reason");
                if (outcome != Outcome.UNASSESSED) {
                    throw new IllegalArgumentException("a rejected method has no effectiveness outcome");
                }
            }
            if (outcome != Outcome.UNASSESSED && outcomeEvidenceRefs.isEmpty()) {
                throw new IllegalArgumentException("outcomeEvidenceRefs are required for an assessed outcome");
            }
            if (outcomeEvidenceRefs.stream().anyMatch(ref -> ref == null || ref.isBlank())) {
                throw new IllegalArgumentException("outcomeEvidenceRefs cannot contain blank refs");
            }
        }
    }

    private static String rationale(KnowledgeRoute route) {
        return switch (route) {
            case WORKSPACE_SEMANTIC -> "reusable workspace fact";
            case WORKSPACE_PROCEDURAL -> "reusable workspace operating guidance";
            case SKILL_IMPROVEMENT -> "reusable engineering reasoning weakness";
            case CONTROL_IMPROVEMENT -> "deterministic governance or rule weakness";
            case SWARM_CORE_IMPROVEMENT -> "shared orchestration weakness";
            case RUNTIME_BINDING_IMPROVEMENT -> "ENGCIM to Multica translation weakness";
            case PRODUCT_KNOWLEDGE_PROPOSAL -> "candidate Product truth requires Product Knowledge governance";
            case MULTICA_PLATFORM_ISSUE -> "Multica-owned defect requires platform issue or proposal";
            case MISSION_HISTORY -> "one-off Mission history remains evidence/history";
            case DIRECT_TKMS_PUBLICATION -> "prohibited direct tKMS publication";
        };
    }
}

package com.featuredeliveryintelligence.fdi.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WorkspaceKnowledgeRetrievalTests {
    private static final Instant FETCHED_AT = Instant.parse("2026-09-29T00:00:00Z");
    private static final Instant VALID_UNTIL = Instant.parse("2026-09-30T00:00:00Z");
    private static final String COGNEE_DATASET = "00000000-0000-7000-8000-000000000001";

    @Test
    void missionHandsOffAtMostThreeQualifiedSourcesWithoutTruncatingSelectionReceipts() {
        var entries = java.util.stream.IntStream.range(0, 4).mapToObj(index -> entry(
                proposal("proposal-bounded-" + index, "workspace-a", List.of()),
                KnowledgeGovernanceDecision.APPROVED, "CURRENT", FETCHED_AT.minusSeconds(2), VALID_UNTIL,
                Map.of("repo", "rev-1"), List.of("project-consumer"), "WORKSPACE_AUTHORIZED")).toList();
        var read = new WorkspaceKnowledgeRepository.ReadResult(
                "workspace-a", "project-workspace-knowledge", FETCHED_AT, entries);
        var request = new SwarmKnowledgeGateway.ConsumerRequest(
                "mission:bounded", "workspace-a", "project-workspace-knowledge", "project-consumer", Map.of("repo", "rev-1"));
        var gateway = gateway();
        assertThat(gateway.retrieveForConsumer(request, read).selected()).hasSize(4);
        var context = gateway.retrieveForConsumer(request, read, COGNEE_DATASET,
                entries.stream().map(WorkspaceKnowledgeRetrievalTests::candidate).toList());
        assertThat(context.selected()).containsExactlyElementsOf(entries);
        var envelope = new MissionExecutionEnvelope("mission:bounded", "request", "workspace-a", "project-consumer",
                "scope", "goal", List.of(), List.of("qualified knowledge"), "revision").withKnowledgeContext(context);
        assertThat(envelope.eligibleKnowledge()).containsExactlyElementsOf(entries.subList(0, 3));
        assertThat(context.selected()).containsExactlyElementsOf(entries);
        assertThat(gateway.retrieveForConsumer(request, read, COGNEE_DATASET,
                List.of(candidate(entries.get(3)))).selected()).containsExactly(entries.get(3));
    }

    @Test
    void freshProviderReadSelectsOnlyBoundEligibleRecordsAndSeparatesUseFromEffectiveness() {
        WorkspaceKnowledgeProposal eligible = proposal("proposal-1", "workspace-a", List.of());
        WorkspaceKnowledgeProposal stale = proposal("proposal-stale", "workspace-a", List.of());
        WorkspaceKnowledgeProposal wrongWorkspace = proposal("proposal-wrong", "workspace-b", List.of());
        WorkspaceKnowledgeProposal conflicting = proposal("proposal-conflict", "workspace-a", List.of("conflict:1"));
        WorkspaceKnowledgeProposal rejected = proposal("proposal-rejected", "workspace-a", List.of());
        WorkspaceKnowledgeProposal badDigest = proposal("proposal-digest", "workspace-a", List.of());
        WorkspaceKnowledgeProposal unknownFreshness = proposal("proposal-unknown-freshness", "workspace-a", List.of());
        WorkspaceKnowledgeProposal wrongRevision = proposal("proposal-wrong-revision", "workspace-a", List.of());
        WorkspaceKnowledgeProposal missingRevision = proposal("proposal-missing-revision", "workspace-a", List.of());
        WorkspaceKnowledgeProposal duplicate = proposal("proposal-duplicate", "workspace-a", List.of());

        List<WorkspaceKnowledgeRepository.Entry> entries = List.of(
                entry(eligible, KnowledgeGovernanceDecision.APPROVED, "CURRENT", FETCHED_AT.minusSeconds(2), VALID_UNTIL,
                        Map.of("repo", "rev-1", "repo-secondary", "rev-2"),
                        List.of("project-consumer"), "WORKSPACE_AUTHORIZED"),
                entry(stale, KnowledgeGovernanceDecision.APPROVED, "CURRENT", FETCHED_AT.minusSeconds(2),
                        FETCHED_AT.minusSeconds(1), Map.of("repo", "rev-1"), List.of("project-consumer"), "WORKSPACE_AUTHORIZED"),
                entry(wrongWorkspace, KnowledgeGovernanceDecision.APPROVED, "CURRENT", FETCHED_AT.minusSeconds(2), VALID_UNTIL,
                        Map.of("repo", "rev-1"), List.of("project-consumer"), "WORKSPACE_AUTHORIZED"),
                entry(conflicting, KnowledgeGovernanceDecision.APPROVED, "CURRENT", FETCHED_AT.minusSeconds(2), VALID_UNTIL,
                        Map.of("repo", "rev-1"), List.of("project-consumer"), "WORKSPACE_AUTHORIZED"),
                entry(rejected, KnowledgeGovernanceDecision.REJECTED, "CURRENT", FETCHED_AT.minusSeconds(2), VALID_UNTIL,
                        Map.of("repo", "rev-1"), List.of("project-consumer"), "WORKSPACE_AUTHORIZED"),
                entry(badDigest, KnowledgeGovernanceDecision.APPROVED, "CURRENT", FETCHED_AT.minusSeconds(2), VALID_UNTIL,
                        Map.of("repo", "rev-1"), List.of("project-consumer"), "WORKSPACE_AUTHORIZED", "", "sha256:wrong"),
                entry(unknownFreshness, KnowledgeGovernanceDecision.APPROVED, "CURRENT", null, null,
                        Map.of(), List.of("project-consumer"), "WORKSPACE_AUTHORIZED"),
                entry(wrongRevision, KnowledgeGovernanceDecision.APPROVED, "CURRENT", FETCHED_AT.minusSeconds(2), VALID_UNTIL,
                        Map.of("repo", "rev-0", "repo-secondary", "rev-2"),
                        List.of("project-consumer"), "WORKSPACE_AUTHORIZED"),
                entry(missingRevision, KnowledgeGovernanceDecision.APPROVED, "CURRENT", FETCHED_AT.minusSeconds(2), VALID_UNTIL,
                        Map.of("repo", "rev-1"), List.of("project-consumer"), "WORKSPACE_AUTHORIZED"),
                entry(duplicate, KnowledgeGovernanceDecision.APPROVED, "CURRENT", FETCHED_AT.minusSeconds(2), VALID_UNTIL,
                        Map.of("repo", "rev-1"), List.of("project-consumer"), "WORKSPACE_AUTHORIZED"),
                entry(duplicate, KnowledgeGovernanceDecision.APPROVED, "CURRENT", FETCHED_AT.minusSeconds(2), VALID_UNTIL,
                        Map.of("repo", "rev-1"), List.of("project-consumer"), "WORKSPACE_AUTHORIZED", "provider:duplicate", ""));
        AtomicInteger providerReads = new AtomicInteger();
        WorkspaceKnowledgeRepository repository = repository(entries, providerReads);
        SwarmKnowledgeGateway gateway = gateway();
        SwarmKnowledgeGateway.ConsumerRequest request = new SwarmKnowledgeGateway.ConsumerRequest(
                "mission:later", "workspace-a", "project-workspace-knowledge", "project-consumer",
                Map.of("repo", "rev-1", "repo-secondary", "rev-2"));

        SwarmKnowledgeGateway.EligibleConsumerContext context = gateway.retrieveForConsumer(request, repository);

        assertThat(providerReads).hasValue(1);
        assertThat(context.selected()).extracting(item -> item.proposal().proposalRef()).containsExactly("proposal-1");
        assertThat(context.fetchedAt()).isEqualTo(FETCHED_AT);
        assertThat(context.selected().get(0).recordVersion()).isEqualTo(1);
        assertThat(context.selected().get(0).providerRevision()).isEqualTo("provider-revision:4");
        assertThat(context.selected().get(0).governance().decisionRef()).isEqualTo("decision:proposal-1");
        assertThat(context.selected().get(0).proposalDigest())
                .isEqualTo(SwarmKnowledgeGateway.proposalDigest(eligible));
        assertThat(context.excluded()).extracting(SwarmKnowledgeGateway.ContextExclusion::reason)
                .contains(
                        SwarmKnowledgeGateway.ContextExclusionReason.STALE,
                        SwarmKnowledgeGateway.ContextExclusionReason.WRONG_WORKSPACE,
                        SwarmKnowledgeGateway.ContextExclusionReason.UNRESOLVED_CONFLICT,
                        SwarmKnowledgeGateway.ContextExclusionReason.NOT_APPROVED,
                        SwarmKnowledgeGateway.ContextExclusionReason.DIGEST_MISMATCH,
                        SwarmKnowledgeGateway.ContextExclusionReason.FRESHNESS_UNKNOWN,
                        SwarmKnowledgeGateway.ContextExclusionReason.SOURCE_REVISION_MISMATCH);
        assertThat(context.excluded().stream()
                .filter(item -> item.reason() == SwarmKnowledgeGateway.ContextExclusionReason.DUPLICATE_RECORD_KEY))
                .hasSize(2);
        assertThat(context.excluded().stream()
                .filter(item -> item.reason() == SwarmKnowledgeGateway.ContextExclusionReason.SOURCE_REVISION_MISMATCH))
                .extracting(SwarmKnowledgeGateway.ContextExclusion::recordKey)
                .containsExactlyInAnyOrder("proposal-wrong-revision", "proposal-missing-revision");

        assertThatThrownBy(() -> gateway.buildConsumerFeedback(
                context, "proposal-1", 1,
                SwarmKnowledgeGateway.ConsumerDisposition.ADOPTED, "", "action:1", "result:1",
                SwarmKnowledgeGateway.Outcome.UNASSESSED, List.of()))
                .hasMessageContaining("adoption reason");

        SwarmKnowledgeGateway.ConsumerFeedback feedback = gateway.buildConsumerFeedback(
                context, "proposal-1", 1,
                SwarmKnowledgeGateway.ConsumerDisposition.ADOPTED, "applied the matching workflow", "action:1", "result:1",
                SwarmKnowledgeGateway.Outcome.UNASSESSED, List.of());

        assertThat(feedback.recordVersion()).isEqualTo(1);
        assertThat(feedback.reason()).isEqualTo("applied the matching workflow");
        assertThat(feedback.actionRef()).isEqualTo("action:1");
        assertThat(feedback.resultRef()).isEqualTo("result:1");
        assertThat(feedback.outcome()).isEqualTo(SwarmKnowledgeGateway.Outcome.UNASSESSED);
        assertThatThrownBy(() -> gateway.buildConsumerFeedback(
                context, "proposal-1", 1,
                SwarmKnowledgeGateway.ConsumerDisposition.ADOPTED, "applied the matching workflow", "action:1", "result:1",
                SwarmKnowledgeGateway.Outcome.EFFECTIVE, List.of()))
                .hasMessageContaining("outcomeEvidenceRefs");
        assertThatThrownBy(() -> gateway.buildConsumerFeedback(
                context, "proposal-stale", 1,
                SwarmKnowledgeGateway.ConsumerDisposition.ADOPTED, "would apply the matching workflow", "action:stale", "result:stale",
                SwarmKnowledgeGateway.Outcome.UNASSESSED, List.of()))
                .hasMessageContaining("was not selected");

        SwarmKnowledgeGateway.EligibleConsumerContext nextMissionContext = gateway.retrieveForConsumer(request, repository);
        assertThat(providerReads).hasValue(2);
        SwarmKnowledgeGateway.ConsumerFeedback rejectedUse = gateway.buildConsumerFeedback(
                nextMissionContext, "proposal-1", 1,
                SwarmKnowledgeGateway.ConsumerDisposition.REJECTED, "not applicable to this change", "", "",
                SwarmKnowledgeGateway.Outcome.UNASSESSED, List.of());
        assertThat(rejectedUse.disposition()).isEqualTo(SwarmKnowledgeGateway.ConsumerDisposition.REJECTED);
        assertThat(rejectedUse.reason()).isEqualTo("not applicable to this change");
        assertThat(rejectedUse.outcome()).isEqualTo(SwarmKnowledgeGateway.Outcome.UNASSESSED);
    }

    @Test
    void emptyProviderReadIsAValidContextAndDoesNotBlockTheConsumer() {
        AtomicInteger providerReads = new AtomicInteger();
        WorkspaceKnowledgeRepository repository = repository(List.of(), providerReads);

        SwarmKnowledgeGateway.EligibleConsumerContext context = gateway().retrieveForConsumer(
                new SwarmKnowledgeGateway.ConsumerRequest(
                        "mission:empty", "workspace-a", "project-workspace-knowledge", "project-consumer", Map.of()),
                repository);

        assertThat(providerReads).hasValue(1);
        assertThat(context.selected()).isEmpty();
        assertThat(context.mayProceedWithoutKnowledge()).isFalse();
    }

    @Test
    void cogneeSourceMatchNarrowsButDoesNotBypassFreshnessOrGovernanceChecks() {
        WorkspaceKnowledgeRepository.Entry approved = entry(
                proposal("proposal-cognee-approved", "workspace-a", List.of()),
                KnowledgeGovernanceDecision.APPROVED, "CURRENT", FETCHED_AT.minusSeconds(2), VALID_UNTIL,
                Map.of("repo", "rev-1"), List.of("project-consumer"), "WORKSPACE_AUTHORIZED");
        WorkspaceKnowledgeRepository.Entry stale = entry(
                proposal("proposal-cognee-stale", "workspace-a", List.of()),
                KnowledgeGovernanceDecision.APPROVED, "CURRENT", FETCHED_AT.minusSeconds(2), FETCHED_AT.minusSeconds(1),
                Map.of("repo", "rev-1"), List.of("project-consumer"), "WORKSPACE_AUTHORIZED");
        WorkspaceKnowledgeRepository.Entry deferred = entry(
                proposal("proposal-cognee-deferred", "workspace-a", List.of()),
                KnowledgeGovernanceDecision.DEFERRED, "CURRENT", FETCHED_AT.minusSeconds(2), VALID_UNTIL,
                Map.of("repo", "rev-1"), List.of("project-consumer"), "WORKSPACE_AUTHORIZED");
        var read = new WorkspaceKnowledgeRepository.ReadResult(
                "workspace-a", "project-workspace-knowledge", FETCHED_AT, List.of(approved, stale, deferred));
        var request = new SwarmKnowledgeGateway.ConsumerRequest(
                "mission:cognee-filter", "workspace-a", "project-workspace-knowledge", "project-consumer",
                Map.of("repo", "rev-1"));
        List<CogneeSearchClient.CandidateDocument> candidates = List.of(
                candidate(approved), candidate(stale), candidate(deferred));

        SwarmKnowledgeGateway.EligibleConsumerContext context = gateway().retrieveForConsumer(
                request, read, COGNEE_DATASET, candidates);

        assertThat(context.selected()).containsExactly(approved);
        assertThat(context.excluded()).extracting(SwarmKnowledgeGateway.ContextExclusion::recordKey)
                .containsExactlyInAnyOrder(stale.recordKey(), deferred.recordKey());
        assertThat(context.excluded()).extracting(SwarmKnowledgeGateway.ContextExclusion::reason)
                .containsExactlyInAnyOrder(
                        SwarmKnowledgeGateway.ContextExclusionReason.STALE,
                        SwarmKnowledgeGateway.ContextExclusionReason.NOT_APPROVED);
    }

    @Test
    void cogneeNarrowingCannotHideDuplicateRecordKeysFromTheOriginalProviderRead() {
        WorkspaceKnowledgeRepository.Entry exact = entry(
                proposal("proposal-cognee-duplicate", "workspace-a", List.of()),
                KnowledgeGovernanceDecision.APPROVED, "CURRENT", FETCHED_AT.minusSeconds(2), VALID_UNTIL,
                Map.of("repo", "rev-1"), List.of("project-consumer"), "WORKSPACE_AUTHORIZED");
        WorkspaceKnowledgeRepository.Entry conflictingVersion = new WorkspaceKnowledgeRepository.Entry(
                exact.knowledgeRef(), "provider-revision:other", exact.recordKey(), exact.recordVersion(), exact.proposal(),
                exact.governance(), exact.proposalDigest(), exact.decisionRecordKey(), exact.decisionRecordVersion(),
                exact.decisionProposalDigest(), exact.lifecycle(), exact.observedAt(), exact.validUntil(),
                exact.repositoryRevisions(), exact.applicableProjectRefs(), exact.visibility());
        var read = new WorkspaceKnowledgeRepository.ReadResult(
                "workspace-a", "project-workspace-knowledge", FETCHED_AT, List.of(exact, conflictingVersion));
        var request = new SwarmKnowledgeGateway.ConsumerRequest(
                "mission:cognee-duplicate", "workspace-a", "project-workspace-knowledge", "project-consumer",
                Map.of("repo", "rev-1"));

        SwarmKnowledgeGateway.EligibleConsumerContext context = gateway().retrieveForConsumer(
                request, read, COGNEE_DATASET, List.of(candidate(exact)));

        assertThat(context.selected()).isEmpty();
        assertThat(context.excluded()).hasSize(2)
                .extracting(SwarmKnowledgeGateway.ContextExclusion::reason)
                .containsOnly(SwarmKnowledgeGateway.ContextExclusionReason.DUPLICATE_RECORD_KEY);
    }

    @Test
    void entryExpiredSinceProviderFetchIsExcludedEvenWhenRepositoryRevisionsStillMatch() {
        Instant now = FETCHED_AT.plusSeconds(120);
        Instant fetchedAt = FETCHED_AT;
        Instant validUntil = FETCHED_AT.plusSeconds(60);
        WorkspaceKnowledgeProposal expired = proposal("proposal-expired-now", "workspace-a", List.of());
        WorkspaceKnowledgeRepository.Entry entry = entry(
                expired, KnowledgeGovernanceDecision.APPROVED, "CURRENT", fetchedAt.minusSeconds(10), validUntil,
                Map.of("repo", "rev-1"), List.of("project-consumer"), "WORKSPACE_AUTHORIZED");
        WorkspaceKnowledgeRepository repository = new WorkspaceKnowledgeRepository() {
            @Override
            public void save(GovernedWorkspaceKnowledge knowledge) {
                throw new AssertionError("read-only consumer must not persist WorkspaceKnowledge");
            }

            @Override
            public WorkspaceKnowledgeRepository.ReadResult findByWorkspace(String workspaceRef) {
                return new WorkspaceKnowledgeRepository.ReadResult(
                        "workspace-a", "project-workspace-knowledge", fetchedAt, List.of(entry));
            }
        };

        SwarmKnowledgeGateway.EligibleConsumerContext context = gatewayAt(now).retrieveForConsumer(
                new SwarmKnowledgeGateway.ConsumerRequest(
                        "mission:expired-now", "workspace-a", "project-workspace-knowledge", "project-consumer",
                        Map.of("repo", "rev-1")), repository);

        assertThat(context.selected()).isEmpty();
        assertThat(context.excluded()).singleElement()
                .extracting(SwarmKnowledgeGateway.ContextExclusion::reason)
                .isEqualTo(SwarmKnowledgeGateway.ContextExclusionReason.STALE);
    }

    @Test
    void feedbackReplaysOriginalSelectionTimeAfterMethodExpiresButFreshSelectionExcludesIt() {
        Instant selectedAt = FETCHED_AT.plusSeconds(30);
        Instant validUntil = FETCHED_AT.plusSeconds(60);
        Instant feedbackAt = FETCHED_AT.plusSeconds(120);
        WorkspaceKnowledgeProposal method = proposal("proposal-expired-after-selection", "workspace-a", List.of());
        WorkspaceKnowledgeRepository.Entry entry = entry(
                method, KnowledgeGovernanceDecision.APPROVED, "CURRENT", FETCHED_AT.minusSeconds(10), validUntil,
                Map.of("repo", "rev-1"), List.of("project-consumer"), "WORKSPACE_AUTHORIZED");
        WorkspaceKnowledgeRepository.ReadResult read = new WorkspaceKnowledgeRepository.ReadResult(
                "workspace-a", "project-workspace-knowledge", FETCHED_AT, List.of(entry));
        SwarmKnowledgeGateway.ConsumerRequest request = new SwarmKnowledgeGateway.ConsumerRequest(
                "mission:consumer-before-expiry", "workspace-a", "project-workspace-knowledge", "project-consumer",
                Map.of("repo", "rev-1"));

        SwarmKnowledgeGateway.EligibleConsumerContext original = gatewayAt(selectedAt).retrieveForConsumer(request, read);
        SwarmKnowledgeGateway afterExpiry = gatewayAt(feedbackAt);
        SwarmKnowledgeGateway.EligibleConsumerContext replayed =
                afterExpiry.replayConsumerSelection(request, read, original.evaluatedAt());
        SwarmKnowledgeGateway.ConsumerFeedback feedback = afterExpiry.buildConsumerFeedback(
                replayed, method.proposalRef(), 1,
                SwarmKnowledgeGateway.ConsumerDisposition.ADOPTED, "used while the receipt was eligible",
                "action:before-expiry", "result:after-expiry", SwarmKnowledgeGateway.Outcome.UNASSESSED, List.of());
        SwarmKnowledgeGateway.EligibleConsumerContext fresh = afterExpiry.retrieveForConsumer(request, read);

        assertThat(original.selected()).containsExactly(entry);
        assertThat(replayed.evaluatedAt()).isEqualTo(selectedAt);
        assertThat(replayed.selected()).containsExactly(entry);
        assertThat(feedback.actionRef()).isEqualTo("action:before-expiry");
        assertThat(fresh.selected()).isEmpty();
        assertThat(fresh.excluded()).singleElement()
                .extracting(SwarmKnowledgeGateway.ContextExclusion::reason)
                .isEqualTo(SwarmKnowledgeGateway.ContextExclusionReason.STALE);
    }

    @Test
    void mismatchedProviderWorkspaceOrProjectIsReportedAndNeverHandedOff() {
        WorkspaceKnowledgeRepository repository = new WorkspaceKnowledgeRepository() {
            @Override
            public void save(GovernedWorkspaceKnowledge knowledge) {
                throw new AssertionError("read-only consumer must not persist WorkspaceKnowledge");
            }

            @Override
            public WorkspaceKnowledgeRepository.ReadResult findByWorkspace(String workspaceRef) {
                return new WorkspaceKnowledgeRepository.ReadResult(
                        "workspace-other", "project-other", FETCHED_AT,
                        List.of(entry(proposal("proposal-cross-scope", "workspace-other", List.of()),
                                KnowledgeGovernanceDecision.APPROVED, "CURRENT", FETCHED_AT.minusSeconds(2), VALID_UNTIL,
                                Map.of("repo", "rev-1"), List.of("project-consumer"), "WORKSPACE_AUTHORIZED")));
            }
        };

        SwarmKnowledgeGateway.EligibleConsumerContext context = gateway().retrieveForConsumer(
                new SwarmKnowledgeGateway.ConsumerRequest(
                        "mission:cross-scope", "workspace-a", "project-workspace-knowledge", "project-consumer",
                        Map.of("repo", "rev-1")), repository);

        assertThat(context.selected()).isEmpty();
        assertThat(context.excluded()).singleElement()
                .extracting(SwarmKnowledgeGateway.ContextExclusion::reason)
                .isEqualTo(SwarmKnowledgeGateway.ContextExclusionReason.READ_SCOPE_MISMATCH);
        assertThat(context.mayProceedWithoutKnowledge()).isFalse();
    }

    @Test
    void proposalDigestUsesTheProfileCanonicalJsonEncoding() {
        WorkspaceKnowledgeProposal proposal = new WorkspaceKnowledgeProposal(
                "proposal-1", "workspace-a", List.of("repo@rev1"), KnowledgeType.SEMANTIC,
                "Use the supported entrypoint", "workspace-a", "repo@rev1", List.of(),
                List.of("issue:1"), List.of());

        assertThat(SwarmKnowledgeGateway.proposalDigest(proposal))
                .isEqualTo("7fbef635c4a43790440bef11352b7c62921a6bd4372ba6b63d99f6a68a65be5e");
    }

    private static WorkspaceKnowledgeRepository.Entry entry(
            WorkspaceKnowledgeProposal proposal,
            KnowledgeGovernanceDecision decision,
            String lifecycle,
            Instant observedAt,
            Instant validUntil,
            Map<String, String> repositoryRevisions,
            List<String> applicableProjects,
            String visibility) {
        return entry(proposal, decision, lifecycle, observedAt, validUntil, repositoryRevisions,
                applicableProjects, visibility, "", "");
    }

    private static WorkspaceKnowledgeRepository.Entry entry(
            WorkspaceKnowledgeProposal proposal,
            KnowledgeGovernanceDecision decision,
            String lifecycle,
            Instant observedAt,
            Instant validUntil,
            Map<String, String> repositoryRevisions,
            List<String> applicableProjects,
            String visibility,
            String providerKnowledgeRef,
            String digestOverride) {
        String digest = digestOverride.isBlank() ? SwarmKnowledgeGateway.proposalDigest(proposal) : digestOverride;
        GovernedWorkspaceKnowledge governance = new GovernedWorkspaceKnowledge(
                proposal, decision, "decision:" + proposal.proposalRef(), "actor:reviewer",
                "policy:workspace-learning", FETCHED_AT.toString(), List.of("decision-evidence:1"));
        return new WorkspaceKnowledgeRepository.Entry(
                providerKnowledgeRef.isBlank() ? "knowledge:" + proposal.proposalRef() : providerKnowledgeRef,
                "provider-revision:4", proposal.proposalRef(), 1, proposal, governance,
                digest, proposal.proposalRef(), 1, digest, lifecycle, observedAt, validUntil,
                repositoryRevisions, applicableProjects, visibility);
    }

    private static WorkspaceKnowledgeRepository repository(
            List<WorkspaceKnowledgeRepository.Entry> entries, AtomicInteger providerReads) {
        return new WorkspaceKnowledgeRepository() {
            @Override
            public void save(GovernedWorkspaceKnowledge knowledge) {
                throw new AssertionError("read-only consumer must not persist WorkspaceKnowledge");
            }

            @Override
            public WorkspaceKnowledgeRepository.ReadResult findByWorkspace(String workspaceRef) {
                providerReads.incrementAndGet();
                assertThat(workspaceRef).isEqualTo("workspace-a");
                return new WorkspaceKnowledgeRepository.ReadResult(
                        "workspace-a", "project-workspace-knowledge", FETCHED_AT, entries);
            }
        };
    }

    private static SwarmKnowledgeGateway gateway() {
        return gatewayAt(FETCHED_AT.plusSeconds(30));
    }

    private static SwarmKnowledgeGateway gatewayAt(Instant instant) {
        return new SwarmKnowledgeGateway(Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static WorkspaceKnowledgeProposal proposal(String ref, String workspaceRef, List<String> conflicts) {
        return new WorkspaceKnowledgeProposal(
                ref, workspaceRef, List.of("repo@rev1"), KnowledgeType.SEMANTIC,
                "Use the supported entrypoint", "workspace-a", "repo@rev1", List.of(), List.of("issue:1"), conflicts);
    }

    private static CogneeSearchClient.CandidateDocument candidate(WorkspaceKnowledgeRepository.Entry entry) {
        return new CogneeSearchClient.CandidateDocument(
                COGNEE_DATASET, "doc:" + entry.recordKey(), entry.recordKey(), entry.recordVersion(),
                entry.providerRevision(), entry.proposal().sourceRefs().get(0), "workspace-a",
                "project-workspace-knowledge", "0".repeat(64), 1);
    }
}

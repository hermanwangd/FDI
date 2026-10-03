package com.featuredeliveryintelligence.fdi.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Synthetic contract coverage: source qualification and a bounded handoff, without a relevance-rank claim. */
class CogneeHandoffOrderTests {
    private static final String DATASET = "00000000-0000-7000-8000-000000000007";
    private static final String WORKSPACE = "fixture:workspace:handoff-order";
    private static final String KNOWLEDGE_PROJECT = "fixture:project:knowledge";
    private static final String CONSUMER_PROJECT = "fixture:project:consumer";
    private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");
    private static final Map<String, String> REVISIONS = Map.of("fixture:repository", "fixture:revision:r1");
    private final SwarmKnowledgeGateway gateway = new SwarmKnowledgeGateway(Clock.fixed(NOW, ZoneOffset.UTC));
    private final SwarmKnowledgeGateway.ConsumerRequest request = new SwarmKnowledgeGateway.ConsumerRequest(
            "fixture:mission:handoff-order", WORKSPACE, KNOWLEDGE_PROJECT, CONSUMER_PROJECT, REVISIONS);

    @Test
    void moreThanThreeExactMatchesKeepProviderOrderWhenSearchOrderIsReversed() {
        var a = approved("a");
        var b = approved("b");
        var c = approved("c");
        var d = approved("d");
        var e = approved("e");
        var context = gateway.retrieveForConsumer(request, read(a, b, c, d, e), DATASET,
                List.of(candidate(e), candidate(d), candidate(c), candidate(b), candidate(a)));

        assertThat(context.selected()).containsExactly(a, b, c, d, e);
        assertThat(context.excluded()).isEmpty();
        var handoff = envelope(context);
        assertThat(handoff.eligibleKnowledge()).containsExactly(a, b, c);
        assertThat(handoff.knowledgeContextStatus()).isEqualTo(MissionExecutionEnvelope.KnowledgeContextStatus.AVAILABLE);
        assertThat(handoff.currentRepositoryRevisions()).isEqualTo(REVISIONS);
    }

    @Test
    void repeatedUnscoredHitsDoNotDuplicateSourcesOrGiveEqualAuthoritySourcesPriority() {
        var a = approved("a");
        var b = approved("b");
        var c = approved("c");
        var d = approved("d");
        var e = approved("e");
        // CandidateDocument has no score or rank. Repeated document hits are membership evidence only.
        var context = gateway.retrieveForConsumer(request, read(a, b, c, d, e), DATASET,
                List.of(candidate(e), candidate(e), candidate(d), candidate(c), candidate(b), candidate(a), candidate(a)));

        assertThat(context.selected()).containsExactly(a, b, c, d, e);
        assertThat(envelope(context).eligibleKnowledge()).containsExactly(a, b, c);
    }

    @Test
    void earlyHitsCannotPromoteExpiredUnapprovedConflictingOrDuplicateRecordsIntoTheCap() {
        var stale = entry("stale", "fixture:provider:stale", KnowledgeGovernanceDecision.APPROVED,
                NOW.minusSeconds(1), List.of());
        var unapproved = entry("unapproved", "fixture:provider:unapproved", KnowledgeGovernanceDecision.DEFERRED,
                NOW.plusSeconds(3600), List.of());
        var conflict = entry("conflict", "fixture:provider:conflict", KnowledgeGovernanceDecision.APPROVED,
                NOW.plusSeconds(3600), List.of("fixture:unresolved-conflict"));
        var duplicate = approved("duplicate");
        var otherDuplicate = entry("duplicate", "fixture:provider:other", KnowledgeGovernanceDecision.APPROVED,
                NOW.plusSeconds(3600), List.of());
        var a = approved("a");
        var b = approved("b");
        var c = approved("c");
        var d = approved("d");
        var read = read(stale, duplicate, a, unapproved, b, conflict, c, otherDuplicate, d);
        var original = gateway.retrieveForConsumer(request, read);
        // Only one duplicate is in the search results; both must still fail the full provider-read gate.
        var narrowed = gateway.retrieveForConsumer(request, read, DATASET,
                List.of(candidate(stale), candidate(unapproved), candidate(conflict), candidate(duplicate),
                        candidate(d), candidate(c), candidate(b), candidate(a)));

        assertThat(narrowed.selected()).containsExactly(a, b, c, d);
        assertThat(narrowed.excluded()).containsExactlyElementsOf(original.excluded());
        assertThat(narrowed.excluded()).extracting(SwarmKnowledgeGateway.ContextExclusion::reason)
                .containsExactly(SwarmKnowledgeGateway.ContextExclusionReason.STALE,
                        SwarmKnowledgeGateway.ContextExclusionReason.DUPLICATE_RECORD_KEY,
                        SwarmKnowledgeGateway.ContextExclusionReason.NOT_APPROVED,
                        SwarmKnowledgeGateway.ContextExclusionReason.UNRESOLVED_CONFLICT,
                        SwarmKnowledgeGateway.ContextExclusionReason.DUPLICATE_RECORD_KEY);
        assertThat(envelope(narrowed).eligibleKnowledge()).containsExactly(a, b, c);
    }

    @Test
    void wrongDatasetFailsTheWholeSelectionEvenWhenOtherHitsAreQualified() {
        var a = approved("a");
        var valid = candidate(a);
        var wrongDataset = new CogneeSearchClient.CandidateDocument(
                "00000000-0000-7000-8000-000000000008", valid.documentId(), valid.sourceId(),
                valid.sourceRevision(), valid.providerRevision(), valid.sourceRef(), valid.workspaceRef(),
                valid.projectRef(), valid.contentSha256(), valid.chunkCount());

        assertThatThrownBy(() -> gateway.retrieveForConsumer(request, read(a), DATASET, List.of(valid, wrongDataset)))
                .isInstanceOf(WorkspaceKnowledgeRepository.ProviderUnavailableException.class)
                .hasMessageContaining("configured dataset");
    }

    private WorkspaceKnowledgeRepository.Entry approved(String key) {
        return entry(key, "fixture:provider:" + key, KnowledgeGovernanceDecision.APPROVED,
                NOW.plusSeconds(3600), List.of());
    }

    private WorkspaceKnowledgeRepository.Entry entry(String key, String providerRevision,
            KnowledgeGovernanceDecision decision, Instant validUntil, List<String> conflicts) {
        String recordKey = "fixture:record:" + key;
        var proposal = new WorkspaceKnowledgeProposal(recordKey, WORKSPACE, List.of("fixture:source:" + key),
                KnowledgeType.PROCEDURAL, "Synthetic method " + key, "Synthetic handoff contract only",
                "Equal authority; no relevance score", List.of("Test-only; no publication authority"),
                List.of("fixture:evidence:source:" + key), List.of());
        var governance = new GovernedWorkspaceKnowledge(proposal, decision, "fixture:decision:" + key,
                "fixture:actor", "fixture:policy", NOW.minusSeconds(120).toString(),
                List.of("fixture:evidence:decision:" + key));
        String digest = SwarmKnowledgeGateway.proposalDigest(proposal);
        return new WorkspaceKnowledgeRepository.Entry("fixture:knowledge:" + key, providerRevision, recordKey, 1,
                proposal, governance, digest, recordKey, 1, digest, "CURRENT", NOW.minusSeconds(60), validUntil,
                REVISIONS, List.of(CONSUMER_PROJECT), "WORKSPACE_AUTHORIZED", conflicts);
    }

    private WorkspaceKnowledgeRepository.ReadResult read(WorkspaceKnowledgeRepository.Entry... entries) {
        return new WorkspaceKnowledgeRepository.ReadResult(WORKSPACE, KNOWLEDGE_PROJECT, NOW, List.of(entries));
    }

    private CogneeSearchClient.CandidateDocument candidate(WorkspaceKnowledgeRepository.Entry entry) {
        return new CogneeSearchClient.CandidateDocument(DATASET, "fixture:document:" + entry.recordKey(),
                entry.recordKey(), entry.recordVersion(), entry.providerRevision(), entry.proposal().sourceRefs().get(0),
                WORKSPACE, KNOWLEDGE_PROJECT, sourceDigest(entry.proposal().statement()), 1);
    }

    private String sourceDigest(String statement) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(statement.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new AssertionError(unavailable);
        }
    }

    private MissionExecutionEnvelope envelope(SwarmKnowledgeGateway.EligibleConsumerContext context) {
        return new MissionExecutionEnvelope(request.missionRef(), "fixture:request", WORKSPACE, CONSUMER_PROJECT,
                "Synthetic source-order contract", "Bound qualified handoff without a relevance claim", List.of(),
                List.of("Preserve qualifications and provider order"), "fixture:revision:r1").withKnowledgeContext(context);
    }
}

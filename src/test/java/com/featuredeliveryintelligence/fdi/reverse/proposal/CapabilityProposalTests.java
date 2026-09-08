package com.featuredeliveryintelligence.fdi.reverse.proposal;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceChannel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract tests for {@link CapabilityProposal} (PKB-BL-009 Slice A): stable
 * identifier shape, citation traceability, digest agreement, confidence
 * bounds, clean product-facing wording, and the proposal-only authority
 * boundary.
 */
class CapabilityProposalTests {

    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String DIGEST_STRUCTURAL = "1".repeat(64);
    private static final String DIGEST_TEST = "2".repeat(64);

    private static EvidenceCitation citation(ReverseEvidenceChannel channel, String ref, String digest) {
        return new EvidenceCitation(channel, ref, digest);
    }

    private static CapabilityProposal proposal(
            String id, String title, List<EvidenceCitation> citations, List<String> digests,
            long confidence, ProposalAuthority authority, boolean publicationAllowed) {
        return new CapabilityProposal(
                id, title, citations, REVISION, digests,
                "both channels observe the same owner registration behavior",
                List.of("limited to repository-visible behavior"), confidence, authority, publicationAllowed);
    }

    private static List<EvidenceCitation> corroboratedCitations() {
        return List.of(
                citation(ReverseEvidenceChannel.STRUCTURAL, "/observations/structural/0", DIGEST_STRUCTURAL),
                citation(ReverseEvidenceChannel.TEST_BEHAVIOR, "/observations/tests/0", DIGEST_TEST));
    }

    @Test
    void validProposalIsAcceptedWithDeterministicOrdering() {
        List<EvidenceCitation> reversed = new ArrayList<>(corroboratedCitations());
        java.util.Collections.reverse(reversed);
        CapabilityProposal accepted = proposal(
                "HYP-CAPABILITY-0001", "Manage pet owners", reversed,
                List.of(DIGEST_TEST, DIGEST_STRUCTURAL), 8000, ProposalAuthority.PROPOSAL_ONLY, false);
        assertThat(accepted.citations()).extracting(EvidenceCitation::channel).containsExactly(
                ReverseEvidenceChannel.STRUCTURAL, ReverseEvidenceChannel.TEST_BEHAVIOR);
        assertThat(accepted.contributingEvidenceDigests()).containsExactly(DIGEST_STRUCTURAL, DIGEST_TEST);
        assertThat(accepted.authority()).isEqualTo(ProposalAuthority.PROPOSAL_ONLY);
        assertThat(accepted.semanticPublicationAllowed()).isFalse();
        assertThatThrownBy(() -> accepted.citations().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void malformedIdentifierFailsClosed() {
        assertThatThrownBy(() -> proposal("HYP-SCENARIO-0001", "Manage pet owners", corroboratedCitations(),
                List.of(DIGEST_STRUCTURAL, DIGEST_TEST), 8000, ProposalAuthority.PROPOSAL_ONLY, false))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.UNTRACEABLE_PROPOSAL);
        assertThatThrownBy(() -> proposal("HYP-CAPABILITY-lowercase", "Manage pet owners", corroboratedCitations(),
                List.of(DIGEST_STRUCTURAL, DIGEST_TEST), 8000, ProposalAuthority.PROPOSAL_ONLY, false))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.UNTRACEABLE_PROPOSAL);
    }

    @Test
    void identifierInTitleFailsClosed() {
        assertThatThrownBy(() -> proposal("HYP-CAPABILITY-0001", "Manage owners via ownerController",
                corroboratedCitations(), List.of(DIGEST_STRUCTURAL, DIGEST_TEST), 8000,
                ProposalAuthority.PROPOSAL_ONLY, false))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.SCENARIO_TEXT_IDENTIFIER);
    }

    @Test
    void untraceableWithoutCitationsFailsClosed() {
        assertThatThrownBy(() -> proposal("HYP-CAPABILITY-0001", "Manage pet owners", List.of(),
                List.of(DIGEST_STRUCTURAL), 8000, ProposalAuthority.PROPOSAL_ONLY, false))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.UNTRACEABLE_PROPOSAL);
    }

    @Test
    void duplicateCitationFailsClosed() {
        EvidenceCitation duplicate = citation(ReverseEvidenceChannel.STRUCTURAL, "/observations/structural/0", DIGEST_STRUCTURAL);
        assertThatThrownBy(() -> proposal("HYP-CAPABILITY-0001", "Manage pet owners",
                List.of(duplicate, duplicate), List.of(DIGEST_STRUCTURAL), 8000,
                ProposalAuthority.PROPOSAL_ONLY, false))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.DUPLICATE_IDENTITY);
    }

    @Test
    void digestSetMustMatchCitedDigestsExactly() {
        assertThatThrownBy(() -> proposal("HYP-CAPABILITY-0001", "Manage pet owners",
                corroboratedCitations(), List.of(DIGEST_STRUCTURAL, "9".repeat(64)), 8000,
                ProposalAuthority.PROPOSAL_ONLY, false))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.UNTRACEABLE_PROPOSAL);
    }

    @Test
    void confidenceOutOfRangeFailsClosed() {
        assertThatThrownBy(() -> proposal("HYP-CAPABILITY-0001", "Manage pet owners", corroboratedCitations(),
                List.of(DIGEST_STRUCTURAL, DIGEST_TEST), 10001, ProposalAuthority.PROPOSAL_ONLY, false))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.MISSING_INPUT);
        assertThatThrownBy(() -> proposal("HYP-CAPABILITY-0001", "Manage pet owners", corroboratedCitations(),
                List.of(DIGEST_STRUCTURAL, DIGEST_TEST), -1, ProposalAuthority.PROPOSAL_ONLY, false))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.MISSING_INPUT);
    }

    @Test
    void semanticPublicationRequestFailsClosed() {
        assertThatThrownBy(() -> proposal("HYP-CAPABILITY-0001", "Manage pet owners", corroboratedCitations(),
                List.of(DIGEST_STRUCTURAL, DIGEST_TEST), 8000, ProposalAuthority.PROPOSAL_ONLY, true))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.AUTHORITY_VIOLATION);
    }

    @Test
    void nullAuthorityFailsClosed() {
        assertThatThrownBy(() -> proposal("HYP-CAPABILITY-0001", "Manage pet owners", corroboratedCitations(),
                List.of(DIGEST_STRUCTURAL, DIGEST_TEST), 8000, null, false))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.AUTHORITY_VIOLATION);
    }

    @Test
    void malformedSourceRevisionFailsClosed() {
        CapabilityProposal accepted = proposal("HYP-CAPABILITY-0001", "Manage pet owners",
                corroboratedCitations(), List.of(DIGEST_STRUCTURAL, DIGEST_TEST), 8000,
                ProposalAuthority.PROPOSAL_ONLY, false);
        assertThat(accepted.sourceRevision()).isEqualTo(REVISION);
        assertThatThrownBy(() -> new CapabilityProposal(
                "HYP-CAPABILITY-0001", "Manage pet owners", corroboratedCitations(), "abc",
                List.of(DIGEST_STRUCTURAL, DIGEST_TEST), "rationale", List.of(), 0,
                ProposalAuthority.PROPOSAL_ONLY, false))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.REVISION_MISMATCH);
    }
}

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
 * Contract tests for {@link ReverseProposalPackage} (PKB-BL-009 Slice A):
 * deterministic sorted ordering, duplicate refusal, scenario-to-capability
 * traceability, revision agreement, and gap handling.
 */
class ReverseProposalPackageTests {

    private static final String REPOSITORY = "spring-petclinic";
    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String DIGEST_STRUCTURAL = "1".repeat(64);
    private static final String DIGEST_TEST = "2".repeat(64);

    private static CapabilityProposal capability(String id) {
        return new CapabilityProposal(
                id, "Manage pet owners",
                List.of(
                        new EvidenceCitation(ReverseEvidenceChannel.STRUCTURAL, "/observations/structural/0", DIGEST_STRUCTURAL),
                        new EvidenceCitation(ReverseEvidenceChannel.TEST_BEHAVIOR, "/observations/tests/0", DIGEST_TEST)),
                REVISION, List.of(DIGEST_STRUCTURAL, DIGEST_TEST),
                "rationale", List.of(), 8000, ProposalAuthority.PROPOSAL_ONLY, false);
    }

    private static ScenarioProposal scenario(String id, String capabilityId, String sourceRevision) {
        return new ScenarioProposal(
                id, capabilityId, "List all registered owners",
                "given", "when", "then",
                List.of(new EvidenceCitation(ReverseEvidenceChannel.TEST_BEHAVIOR, "/observations/tests/0", DIGEST_TEST)),
                sourceRevision, List.of(DIGEST_TEST),
                "rationale", List.of(), 7000,
                ScenarioStatus.UNREVIEWED, ProposalAuthority.PROPOSAL_ONLY, false);
    }

    private static EvidenceGap gap(ReverseEvidenceChannel channel, String ref) {
        return new EvidenceGap(channel, ref, DIGEST_STRUCTURAL, "single-channel observation cannot be corroborated");
    }

    @Test
    void packageSortsAllListsByStableKeys() {
        List<CapabilityProposal> capabilities = new ArrayList<>(List.of(
                capability("HYP-CAPABILITY-0002"), capability("HYP-CAPABILITY-0001")));
        List<ScenarioProposal> scenarios = new ArrayList<>(List.of(
                scenario("HYP-SCENARIO-0002", "HYP-CAPABILITY-0001", REVISION),
                scenario("HYP-SCENARIO-0001", "HYP-CAPABILITY-0001", REVISION)));
        List<EvidenceGap> gaps = new ArrayList<>(List.of(
                gap(ReverseEvidenceChannel.TEST_BEHAVIOR, "/observations/tests/9"),
                gap(ReverseEvidenceChannel.STRUCTURAL, "/observations/structural/9")));

        ReverseProposalPackage pack = new ReverseProposalPackage(REPOSITORY, REVISION, capabilities, scenarios, gaps);

        assertThat(pack.capabilities()).extracting(CapabilityProposal::id)
                .containsExactly("HYP-CAPABILITY-0001", "HYP-CAPABILITY-0002");
        assertThat(pack.scenarios()).extracting(ScenarioProposal::id)
                .containsExactly("HYP-SCENARIO-0001", "HYP-SCENARIO-0002");
        assertThat(pack.evidenceGaps()).extracting(EvidenceGap::channel)
                .containsExactly(ReverseEvidenceChannel.STRUCTURAL, ReverseEvidenceChannel.TEST_BEHAVIOR);
        assertThatThrownBy(() -> pack.capabilities().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void emptyProposalsWithExplicitGapsAreReviewable() {
        ReverseProposalPackage pack = new ReverseProposalPackage(
                REPOSITORY, REVISION, List.of(), List.of(),
                List.of(gap(ReverseEvidenceChannel.DELIVERY_HISTORY, "/observations/history/0")));
        assertThat(pack.capabilities()).isEmpty();
        assertThat(pack.scenarios()).isEmpty();
        assertThat(pack.evidenceGaps()).hasSize(1);
    }

    @Test
    void scenarioWithoutParentCapabilityFailsClosed() {
        assertThatThrownBy(() -> new ReverseProposalPackage(
                REPOSITORY, REVISION, List.of(capability("HYP-CAPABILITY-0001")),
                List.of(scenario("HYP-SCENARIO-0001", "HYP-CAPABILITY-9999", REVISION)),
                List.of()))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.UNTRACEABLE_PROPOSAL);
    }

    @Test
    void duplicateCapabilityIdentityFailsClosed() {
        assertThatThrownBy(() -> new ReverseProposalPackage(
                REPOSITORY, REVISION, List.of(capability("HYP-CAPABILITY-0001"), capability("HYP-CAPABILITY-0001")),
                List.of(), List.of()))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.DUPLICATE_IDENTITY);
    }

    @Test
    void proposalRevisionDisagreementFailsClosed() {
        assertThatThrownBy(() -> new ReverseProposalPackage(
                REPOSITORY, REVISION, List.of(capability("HYP-CAPABILITY-0001")),
                List.of(scenario("HYP-SCENARIO-0001", "HYP-CAPABILITY-0001", "a".repeat(40))),
                List.of()))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.REVISION_MISMATCH);
    }

    @Test
    void duplicateGapFailsClosed() {
        EvidenceGap duplicate = gap(ReverseEvidenceChannel.STRUCTURAL, "/observations/structural/9");
        assertThatThrownBy(() -> new ReverseProposalPackage(
                REPOSITORY, REVISION, List.of(), List.of(), List.of(duplicate, duplicate)))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.DUPLICATE_IDENTITY);
    }

    @Test
    void nullListsFailClosed() {
        assertThatThrownBy(() -> new ReverseProposalPackage(REPOSITORY, REVISION, null, List.of(), List.of()))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.MISSING_INPUT);
    }
}

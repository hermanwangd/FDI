package com.featuredeliveryintelligence.fdi.reverse.proposal;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceChannel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract tests for {@link ScenarioProposal} (PKB-BL-009 Slice A): wording
 * isolation of given/when/then, scenario identifier shape, UNREVIEWED status,
 * and the proposal-only authority boundary.
 */
class ScenarioProposalTests {

    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String DIGEST_STRUCTURAL = "1".repeat(64);
    private static final String DIGEST_TEST = "2".repeat(64);

    private static ScenarioProposal scenario(String given, String when, String then) {
        return new ScenarioProposal(
                "HYP-SCENARIO-0001", "HYP-CAPABILITY-0001",
                "List all registered owners", given, when, then,
                List.of(
                        new EvidenceCitation(ReverseEvidenceChannel.STRUCTURAL, "/observations/structural/0", DIGEST_STRUCTURAL),
                        new EvidenceCitation(ReverseEvidenceChannel.TEST_BEHAVIOR, "/observations/tests/0", DIGEST_TEST)),
                REVISION, List.of(DIGEST_STRUCTURAL, DIGEST_TEST),
                "controller and integration tests exercise the owner list endpoint",
                List.of("test wording mirrors the repository example"), 7000,
                ScenarioStatus.UNREVIEWED, ProposalAuthority.PROPOSAL_ONLY, false);
    }

    @Test
    void validScenarioIsAcceptedUnreviewedAndProposalOnly() {
        ScenarioProposal accepted = scenario(
                "several owners are registered",
                "the owner list page is requested",
                "all registered owners are displayed");
        assertThat(accepted.scenarioStatus()).isEqualTo(ScenarioStatus.UNREVIEWED);
        assertThat(accepted.authority()).isEqualTo(ProposalAuthority.PROPOSAL_ONLY);
        assertThat(accepted.semanticPublicationAllowed()).isFalse();
        assertThat(accepted.citations()).hasSize(2);
    }

    @Test
    void implementationIdentifierInWordingFailsClosed() {
        assertThatThrownBy(() -> scenario(
                "an OwnerController.java exists",
                "the owner list page is requested",
                "all registered owners are displayed"))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.SCENARIO_TEXT_IDENTIFIER);
        assertThatThrownBy(() -> scenario(
                "several owners are registered",
                "findOwnerByLastName is invoked",
                "all registered owners are displayed"))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.SCENARIO_TEXT_IDENTIFIER);
        assertThatThrownBy(() -> scenario(
                "several owners are registered",
                "the owner list page is requested",
                "owners from src/test/java/owner appear"))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.SCENARIO_TEXT_IDENTIFIER);
    }

    @Test
    void malformedScenarioIdFailsClosed() {
        assertThatThrownBy(() -> new ScenarioProposal(
                "SCENARIO-0001", "HYP-CAPABILITY-0001",
                "List all registered owners", "given", "when", "then",
                List.of(new EvidenceCitation(ReverseEvidenceChannel.STRUCTURAL, "/o/0", DIGEST_STRUCTURAL)),
                REVISION, List.of(DIGEST_STRUCTURAL), "rationale", List.of(), 0,
                ScenarioStatus.UNREVIEWED, ProposalAuthority.PROPOSAL_ONLY, false))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.UNTRACEABLE_PROPOSAL);
    }

    @Test
    void semanticPublicationRequestFailsClosed() {
        assertThatThrownBy(() -> new ScenarioProposal(
                "HYP-SCENARIO-0001", "HYP-CAPABILITY-0001",
                "List all registered owners", "given", "when", "then",
                List.of(new EvidenceCitation(ReverseEvidenceChannel.STRUCTURAL, "/o/0", DIGEST_STRUCTURAL)),
                REVISION, List.of(DIGEST_STRUCTURAL), "rationale", List.of(), 0,
                ScenarioStatus.UNREVIEWED, ProposalAuthority.PROPOSAL_ONLY, true))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.AUTHORITY_VIOLATION);
    }

    @Test
    void blankWordingFailsClosed() {
        assertThatThrownBy(() -> scenario(" ", "when", "then"))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.MISSING_INPUT);
    }
}

package com.featuredeliveryintelligence.fdi.reverse.proposal;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceChannel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the mechanical, deterministic rule set of {@link ScenarioTextValidator}
 * (PKB-BL-009 Slice A): each rule fires on its identifier family with a stable
 * rule id, observable behavior wording passes, and {@code requireClean}
 * refuses with {@link ReverseFailure#SCENARIO_TEXT_IDENTIFIER}.
 */
class ScenarioTextValidatorTests {

    @Test
    void observableBehaviorWordingIsClean() {
        String text = "Given a registered owner, when the owner list is requested, then all owners are shown";
        assertThat(ScenarioTextValidator.validate(text)).isEmpty();
        assertThat(ScenarioTextValidator.requireClean(text, "scenario text")).isEqualTo(text);
    }

    @Test
    void sourcePathsAreRejected() {
        assertThat(ScenarioTextValidator.validate("loads src/main/java/owner/OwnerController.java"))
                .extracting(ScenarioTextValidator.Violation::ruleId)
                .contains(ScenarioTextValidator.R1_SOURCE_PATH);
        assertThat(ScenarioTextValidator.validate("uses Owner.java"))
                .extracting(ScenarioTextValidator.Violation::ruleId)
                .contains(ScenarioTextValidator.R1_SOURCE_PATH);
    }

    @Test
    void packageIdentifiersAreRejected() {
        assertThat(ScenarioTextValidator.validate("calls org.springframework.samples.petclinic.owner"))
                .extracting(ScenarioTextValidator.Violation::ruleId)
                .contains(ScenarioTextValidator.R2_PACKAGE_IDENTIFIER);
    }

    @Test
    void camelCaseIdentifiersAreRejected() {
        assertThat(ScenarioTextValidator.validate("invokes findOwnerByLastName."))
                .extracting(ScenarioTextValidator.Violation::ruleId)
                .contains(ScenarioTextValidator.R3_CAMELCASE_IDENTIFIER);
    }

    @Test
    void constantIdentifiersAreRejected() {
        assertThat(ScenarioTextValidator.validate("uses MAX_PAGE_SIZE"))
                .extracting(ScenarioTextValidator.Violation::ruleId)
                .contains(ScenarioTextValidator.R4_CONSTANT_IDENTIFIER);
    }

    @Test
    void providerNodeIdsAreRejected() {
        assertThat(ScenarioTextValidator.validate("node:123 and g:42"))
                .extracting(ScenarioTextValidator.Violation::ruleId)
                .containsOnly(ScenarioTextValidator.R5_PROVIDER_NODE_ID);
        assertThat(ScenarioTextValidator.validate("maps to ns::symbol"))
                .extracting(ScenarioTextValidator.Violation::ruleId)
                .contains(ScenarioTextValidator.R5_PROVIDER_NODE_ID);
    }

    @Test
    void evaluatorLabelsAreRejected() {
        assertThat(ScenarioTextValidator.validate("matches EVAL-OWNER-1 and the gold mapping for evaluator review"))
                .extracting(ScenarioTextValidator.Violation::ruleId)
                .contains(ScenarioTextValidator.R6_EVALUATOR_LABEL);
    }

    @Test
    void requireCleanFailsClosedWithStableCode() {
        assertThatThrownBy(() -> ScenarioTextValidator.requireClean("calls ownerController", "scenario title"))
                .isInstanceOf(ReverseContractException.class)
                .extracting(e -> ((ReverseContractException) e).failure())
                .isEqualTo(ReverseFailure.SCENARIO_TEXT_IDENTIFIER);
    }

    @Test
    void citationIdentifiersAreNotScenarioWordingConcerns() {
        EvidenceCitation citation = new EvidenceCitation(
                ReverseEvidenceChannel.STRUCTURAL, "/observations/0", "a".repeat(64));
        assertThat(citation.observationRef()).isEqualTo("/observations/0");
    }
}

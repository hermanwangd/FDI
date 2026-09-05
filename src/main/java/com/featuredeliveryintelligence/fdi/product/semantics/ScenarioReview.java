package com.featuredeliveryintelligence.fdi.product.semantics;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ScenarioReview {
    private ScenarioReview() {
    }

    public static Optional<ReviewedScenario> apply(
            ScenarioProposal proposal,
            String proposalDigest,
            ScenarioReviewDecision decision,
            ReviewedScenario.Status status) {
        if (proposal == null || decision == null || status == null) {
            throw new RuntimeContractException("proposal, decision, and status are required");
        }
        requireBinding(proposal.proposalRevision(), proposalDigest,
                decision.proposalRevision(), decision.proposalDigest());
        if (decision.action() == ScenarioReviewDecision.Action.REJECT
                || (decision.action() == ScenarioReviewDecision.Action.EDIT
                && !decision.editConfirmed())) {
            return Optional.empty();
        }
        BehaviorScenario behavior = decision.action() == ScenarioReviewDecision.Action.EDIT
                ? decision.replacementBehavior() : proposal.behavior();
        if (!behavior.capabilityId().equals(proposal.behavior().capabilityId())
                || !behavior.scenarioId().equals(proposal.behavior().scenarioId())) {
            throw new RuntimeContractException("edited scenario cannot change stable identifiers");
        }
        return Optional.of(new ReviewedScenario(
                proposal.behavior(), behavior, status,
                ReviewedScenario.Owner.HUMAN_REVIEWER, decision));
    }

    public static Optional<ReviewedCapability> apply(
            CapabilityProposal proposal,
            String proposalDigest,
            CapabilityReviewDecision decision,
            Map<String, ScenarioReviewDecision> scenarioDecisions,
            ReviewedCapability.Status status) {
        if (proposal == null || decision == null || scenarioDecisions == null || status == null) {
            throw new RuntimeContractException("proposal, decisions, and status are required");
        }
        requireBinding(proposal.proposalRevision(), proposalDigest,
                decision.proposalRevision(), decision.proposalDigest());
        if (decision.action() == CapabilityReviewDecision.Action.REJECT
                || (decision.action() == CapabilityReviewDecision.Action.EDIT
                && !decision.editConfirmed())) {
            return Optional.empty();
        }
        CapabilityBehavior behavior = decision.action() == CapabilityReviewDecision.Action.EDIT
                ? decision.replacementBehavior() : proposal.behavior();
        if (!behavior.capabilityId().equals(proposal.behavior().capabilityId())) {
            throw new RuntimeContractException("edited capability cannot change stable identifier");
        }
        ReviewedScenario.Status scenarioStatus = status == ReviewedCapability.Status.FROZEN
                ? ReviewedScenario.Status.FROZEN : ReviewedScenario.Status.DRAFT;
        List<ReviewedScenario> reviewedScenarios = new ArrayList<>();
        for (ScenarioProposal scenario : proposal.scenarios()) {
            ScenarioReviewDecision scenarioDecision = scenarioDecisions.get(
                    scenario.behavior().scenarioId());
            if (scenarioDecision != null) {
                apply(scenario, proposalDigest, scenarioDecision, scenarioStatus)
                        .ifPresent(reviewedScenarios::add);
            }
        }
        return Optional.of(new ReviewedCapability(
                proposal.behavior(), behavior, status, ReviewedCapability.Owner.HUMAN_REVIEWER,
                decision, reviewedScenarios));
    }

    public static List<ReviewedScenario> requireForwardEligibleScenarios(
            List<ReviewedScenario> scenarios) {
        List<ReviewedScenario> snapshot = List.copyOf(scenarios);
        if (snapshot.isEmpty() || !snapshot.stream().allMatch(ReviewedScenario::forwardEligible)) {
            throw new RuntimeContractException("only frozen Human Reviewer scenarios enter Forward inputs");
        }
        return snapshot;
    }

    public static ReviewedCapability requireForwardEligible(ReviewedCapability capability) {
        if (capability == null || !capability.forwardEligible()) {
            throw new RuntimeContractException("only frozen Human Reviewer capabilities enter Forward inputs");
        }
        return capability;
    }

    private static void requireBinding(
            int proposalRevision, String proposalDigest,
            int decisionRevision, String decisionDigest) {
        if (proposalDigest == null
                || !ScenarioContractValidation.DIGEST.matcher(proposalDigest).matches()) {
            throw new RuntimeContractException("proposalDigest must be a lowercase SHA-256");
        }
        if (proposalRevision != decisionRevision || !proposalDigest.equals(decisionDigest)) {
            throw new RuntimeContractException("decision does not bind the exact proposal revision and digest");
        }
    }
}

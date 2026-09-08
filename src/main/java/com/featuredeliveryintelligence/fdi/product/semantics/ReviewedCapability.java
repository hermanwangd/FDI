package com.featuredeliveryintelligence.fdi.product.semantics;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class ReviewedCapability {
    public enum Status { DRAFT, FROZEN }
    public enum Owner { HUMAN_REVIEWER }

    private final CapabilityBehavior proposalBehavior;
    private final CapabilityBehavior behavior;
    private final Status status;
    private final Owner owner;
    private final CapabilityReviewDecision decision;
    private final List<ReviewedScenario> scenarios;
    private final int proposalRevision;
    private final String proposalDigest;

    private ReviewedCapability(
            CapabilityBehavior proposalBehavior, CapabilityBehavior behavior, Status status,
            CapabilityReviewDecision decision, List<ReviewedScenario> scenarios,
            int proposalRevision, String proposalDigest) {
        this.proposalBehavior = proposalBehavior;
        this.behavior = behavior;
        this.status = status;
        this.owner = Owner.HUMAN_REVIEWER;
        this.decision = decision;
        this.scenarios = scenarios;
        this.proposalRevision = proposalRevision;
        this.proposalDigest = proposalDigest;
    }

    static ReviewedCapability create(
            CapabilityProposal proposal, CapabilityBehavior behavior,
            CapabilityReviewDecision decision, List<ReviewedScenario> scenarios,
            Status status, String proposalDigest) {
        if (proposal == null || behavior == null || decision == null
                || scenarios == null || status == null) {
            throw new RuntimeContractException(
                    "reviewed capability requires proposal and Human Reviewer approval provenance");
        }
        ScenarioReview.requireBinding(
                proposal.proposalRevision(), proposalDigest,
                decision.proposalRevision(), decision.proposalDigest());
        boolean acceptedOriginal = decision.action() == CapabilityReviewDecision.Action.ACCEPT
                && behavior.equals(proposal.behavior());
        boolean acceptedEdit = decision.action() == CapabilityReviewDecision.Action.EDIT
                && decision.editConfirmed()
                && behavior.equals(decision.replacementBehavior())
                && behavior.capabilityId().equals(proposal.behavior().capabilityId());
        if (!acceptedOriginal && !acceptedEdit) {
            throw new RuntimeContractException(
                    "reviewed capability requires ACCEPT or confirmed EDIT of the exact proposal");
        }
        List<ReviewedScenario> snapshot;
        try {
            snapshot = List.copyOf(scenarios);
        } catch (RuntimeException error) {
            throw new RuntimeContractException("reviewed scenarios must be immutable and nonnull", error);
        }
        ReviewedScenario.Status expectedScenarioStatus = status == Status.FROZEN
                ? ReviewedScenario.Status.FROZEN : ReviewedScenario.Status.DRAFT;
        var proposalScenarios = new HashMap<String, ScenarioProposal>();
        for (ScenarioProposal scenarioProposal : proposal.scenarios()) {
            if (proposalScenarios.put(
                    scenarioProposal.behavior().scenarioId(), scenarioProposal) != null) {
                throw new RuntimeContractException("capability proposal contains duplicate scenario identifiers");
            }
        }
        var scenarioIds = new HashSet<String>();
        for (ReviewedScenario scenario : snapshot) {
            ScenarioProposal source = proposalScenarios.get(
                    scenario.proposalBehavior().scenarioId());
            if (source == null
                    || !source.behavior().equals(scenario.proposalBehavior())
                    || !proposal.behavior().capabilityId().equals(
                    scenario.proposalBehavior().capabilityId())
                    || scenario.proposalRevision() != proposal.proposalRevision()
                    || !proposalDigest.equals(scenario.proposalDigest())
                    || scenario.status() != expectedScenarioStatus
                    || !scenarioIds.add(scenario.proposalBehavior().scenarioId())) {
                throw new RuntimeContractException(
                        "reviewed scenario does not belong to the exact capability proposal");
            }
        }
        return new ReviewedCapability(
                proposal.behavior(), behavior, status, decision, snapshot,
                proposal.proposalRevision(), proposalDigest);
    }

    public CapabilityBehavior proposalBehavior() { return proposalBehavior; }
    public CapabilityBehavior behavior() { return behavior; }
    public Status status() { return status; }
    public Owner owner() { return owner; }
    public CapabilityReviewDecision decision() { return decision; }
    public List<ReviewedScenario> scenarios() { return scenarios; }
    public int proposalRevision() { return proposalRevision; }
    public String proposalDigest() { return proposalDigest; }

    public boolean forwardEligible() {
        return status == Status.FROZEN
                && owner == Owner.HUMAN_REVIEWER
                && !scenarios.isEmpty()
                && scenarios.stream().allMatch(ReviewedScenario::forwardEligible);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ReviewedCapability that)) return false;
        return proposalRevision == that.proposalRevision
                && proposalBehavior.equals(that.proposalBehavior)
                && behavior.equals(that.behavior)
                && status == that.status
                && owner == that.owner
                && decision.equals(that.decision)
                && scenarios.equals(that.scenarios)
                && proposalDigest.equals(that.proposalDigest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(proposalBehavior, behavior, status, owner, decision,
                scenarios, proposalRevision, proposalDigest);
    }

    @Override
    public String toString() {
        return "ReviewedCapability[proposalBehavior=" + proposalBehavior
                + ", behavior=" + behavior + ", status=" + status + ", owner=" + owner
                + ", decision=" + decision + ", scenarios=" + scenarios
                + ", proposalRevision=" + proposalRevision
                + ", proposalDigest=" + proposalDigest + "]";
    }
}

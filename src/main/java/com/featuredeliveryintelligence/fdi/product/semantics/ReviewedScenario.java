package com.featuredeliveryintelligence.fdi.product.semantics;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.Objects;

public final class ReviewedScenario {
    public enum Status { DRAFT, FROZEN }
    public enum Owner { HUMAN_REVIEWER }

    private final BehaviorScenario proposalBehavior;
    private final BehaviorScenario behavior;
    private final Status status;
    private final Owner owner;
    private final ScenarioReviewDecision decision;
    private final int proposalRevision;
    private final String proposalDigest;

    private ReviewedScenario(
            BehaviorScenario proposalBehavior, BehaviorScenario behavior, Status status,
            ScenarioReviewDecision decision, int proposalRevision, String proposalDigest) {
        this.proposalBehavior = proposalBehavior;
        this.behavior = behavior;
        this.status = status;
        this.owner = Owner.HUMAN_REVIEWER;
        this.decision = decision;
        this.proposalRevision = proposalRevision;
        this.proposalDigest = proposalDigest;
    }

    static ReviewedScenario create(
            ScenarioProposal proposal, BehaviorScenario behavior,
            ScenarioReviewDecision decision, Status status, String proposalDigest) {
        if (proposal == null || behavior == null || decision == null || status == null) {
            throw new RuntimeContractException(
                    "reviewed scenario requires proposal and Human Reviewer approval provenance");
        }
        ScenarioReview.requireBinding(
                proposal.proposalRevision(), proposalDigest,
                decision.proposalRevision(), decision.proposalDigest());
        boolean acceptedOriginal = decision.action() == ScenarioReviewDecision.Action.ACCEPT
                && behavior.equals(proposal.behavior());
        boolean acceptedEdit = decision.action() == ScenarioReviewDecision.Action.EDIT
                && decision.editConfirmed()
                && behavior.equals(decision.replacementBehavior())
                && behavior.capabilityId().equals(proposal.behavior().capabilityId())
                && behavior.scenarioId().equals(proposal.behavior().scenarioId());
        if (!acceptedOriginal && !acceptedEdit) {
            throw new RuntimeContractException(
                    "reviewed scenario requires ACCEPT or confirmed EDIT of the exact proposal");
        }
        return new ReviewedScenario(
                proposal.behavior(), behavior, status, decision,
                proposal.proposalRevision(), proposalDigest);
    }

    public BehaviorScenario proposalBehavior() { return proposalBehavior; }
    public BehaviorScenario behavior() { return behavior; }
    public Status status() { return status; }
    public Owner owner() { return owner; }
    public ScenarioReviewDecision decision() { return decision; }
    public int proposalRevision() { return proposalRevision; }
    public String proposalDigest() { return proposalDigest; }

    boolean forwardEligible() {
        return status == Status.FROZEN && owner == Owner.HUMAN_REVIEWER;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ReviewedScenario that)) return false;
        return proposalRevision == that.proposalRevision
                && proposalBehavior.equals(that.proposalBehavior)
                && behavior.equals(that.behavior)
                && status == that.status
                && owner == that.owner
                && decision.equals(that.decision)
                && proposalDigest.equals(that.proposalDigest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(proposalBehavior, behavior, status, owner, decision,
                proposalRevision, proposalDigest);
    }

    @Override
    public String toString() {
        return "ReviewedScenario[proposalBehavior=" + proposalBehavior
                + ", behavior=" + behavior + ", status=" + status + ", owner=" + owner
                + ", decision=" + decision + ", proposalRevision=" + proposalRevision
                + ", proposalDigest=" + proposalDigest + "]";
    }
}

package com.featuredeliveryintelligence.fdi.product.semantics;

import java.util.List;

public record ReviewedCapability(
        CapabilityBehavior proposalBehavior,
        CapabilityBehavior behavior,
        Status status,
        Owner owner,
        CapabilityReviewDecision decision,
        List<ReviewedScenario> scenarios) {

    public enum Status {
        DRAFT,
        FROZEN
    }

    public enum Owner {
        HUMAN_REVIEWER
    }

    public ReviewedCapability {
        if (proposalBehavior == null || behavior == null || status == null
                || owner != Owner.HUMAN_REVIEWER || decision == null || scenarios == null) {
            throw new com.featuredeliveryintelligence.fdi.shared.RuntimeContractException(
                    "reviewed capability requires Human Reviewer approval provenance");
        }
        boolean acceptedOriginal = decision.action() == CapabilityReviewDecision.Action.ACCEPT
                && behavior.equals(proposalBehavior);
        boolean acceptedEdit = decision.action() == CapabilityReviewDecision.Action.EDIT
                && decision.editConfirmed()
                && behavior.equals(decision.replacementBehavior())
                && behavior.capabilityId().equals(proposalBehavior.capabilityId());
        if (!acceptedOriginal && !acceptedEdit) {
            throw new com.featuredeliveryintelligence.fdi.shared.RuntimeContractException(
                    "reviewed capability requires ACCEPT or confirmed EDIT");
        }
        try {
            scenarios = List.copyOf(scenarios);
        } catch (RuntimeException error) {
            throw new com.featuredeliveryintelligence.fdi.shared.RuntimeContractException(
                    "reviewed scenarios must be immutable and nonnull", error);
        }
    }

    public boolean forwardEligible() {
        return status == Status.FROZEN
                && owner == Owner.HUMAN_REVIEWER
                && !scenarios.isEmpty()
                && scenarios.stream().allMatch(ReviewedScenario::forwardEligible);
    }
}

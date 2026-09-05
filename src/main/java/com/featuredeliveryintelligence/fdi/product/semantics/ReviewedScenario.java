package com.featuredeliveryintelligence.fdi.product.semantics;

public record ReviewedScenario(
        BehaviorScenario proposalBehavior,
        BehaviorScenario behavior,
        Status status,
        Owner owner,
        ScenarioReviewDecision decision) {

    public enum Status {
        DRAFT,
        FROZEN
    }

    public enum Owner {
        HUMAN_REVIEWER
    }

    public ReviewedScenario {
        if (proposalBehavior == null || behavior == null || status == null
                || owner != Owner.HUMAN_REVIEWER || decision == null) {
            throw new com.featuredeliveryintelligence.fdi.shared.RuntimeContractException(
                    "reviewed scenario requires Human Reviewer approval provenance");
        }
        boolean acceptedOriginal = decision.action() == ScenarioReviewDecision.Action.ACCEPT
                && behavior.equals(proposalBehavior);
        boolean acceptedEdit = decision.action() == ScenarioReviewDecision.Action.EDIT
                && decision.editConfirmed()
                && behavior.equals(decision.replacementBehavior())
                && behavior.capabilityId().equals(proposalBehavior.capabilityId())
                && behavior.scenarioId().equals(proposalBehavior.scenarioId());
        if (!acceptedOriginal && !acceptedEdit) {
            throw new com.featuredeliveryintelligence.fdi.shared.RuntimeContractException(
                    "reviewed scenario requires ACCEPT or confirmed EDIT");
        }
    }

    public boolean forwardEligible() {
        return status == Status.FROZEN && owner == Owner.HUMAN_REVIEWER;
    }
}

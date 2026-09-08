package com.featuredeliveryintelligence.fdi.product.semantics;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.time.Instant;

public record CapabilityReviewDecision(
        Action action,
        String reviewerIdentity,
        Instant reviewedAt,
        String reason,
        int proposalRevision,
        String proposalDigest,
        CapabilityBehavior replacementBehavior,
        boolean editConfirmed) {

    public enum Action {
        ACCEPT,
        EDIT,
        REJECT
    }

    public CapabilityReviewDecision {
        ScenarioReviewDecision.validateCommon(action, reviewerIdentity, reviewedAt, reason,
                proposalRevision, proposalDigest);
        if (action == Action.EDIT && replacementBehavior == null) {
            throw new RuntimeContractException("EDIT requires replacementBehavior");
        }
        if (action != Action.EDIT && (replacementBehavior != null || editConfirmed)) {
            throw new RuntimeContractException("only EDIT may contain replacement behavior or confirmation");
        }
    }
}

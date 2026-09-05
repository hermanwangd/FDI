package com.featuredeliveryintelligence.fdi.product.semantics;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.time.Instant;

public record ScenarioReviewDecision(
        Action action,
        String reviewerIdentity,
        Instant reviewedAt,
        String reason,
        int proposalRevision,
        String proposalDigest,
        BehaviorScenario replacementBehavior,
        boolean editConfirmed) {

    public enum Action {
        ACCEPT,
        EDIT,
        REJECT
    }

    public ScenarioReviewDecision {
        validateCommon(action, reviewerIdentity, reviewedAt, reason,
                proposalRevision, proposalDigest);
        if (action == Action.EDIT && replacementBehavior == null) {
            throw new RuntimeContractException("EDIT requires replacementBehavior");
        }
        if (action != Action.EDIT && (replacementBehavior != null || editConfirmed)) {
            throw new RuntimeContractException("only EDIT may contain replacement behavior or confirmation");
        }
    }

    static void validateCommon(Object action, String reviewerIdentity, Instant reviewedAt,
            String reason, int proposalRevision, String proposalDigest) {
        if (action == null || reviewedAt == null) {
            throw new RuntimeContractException("action and reviewedAt are required");
        }
        ScenarioContractValidation.nonblank(reviewerIdentity, "reviewerIdentity");
        ScenarioContractValidation.nonblank(reason, "reason");
        if (proposalRevision < 1) {
            throw new RuntimeContractException("proposalRevision must be positive");
        }
        if (proposalDigest == null
                || !ScenarioContractValidation.DIGEST.matcher(proposalDigest).matches()) {
            throw new RuntimeContractException("proposalDigest must be a lowercase SHA-256");
        }
    }
}

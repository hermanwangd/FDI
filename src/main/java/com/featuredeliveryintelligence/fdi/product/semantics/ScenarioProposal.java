package com.featuredeliveryintelligence.fdi.product.semantics;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.time.Instant;
import java.util.List;

public record ScenarioProposal(
        Authority authority,
        Status status,
        int proposalRevision,
        String sourceRevision,
        String graphDigest,
        Instant historyCutoff,
        BehaviorScenario behavior,
        List<ScenarioEvidenceReference> evidenceReferences,
        String inferenceRationale,
        double confidence,
        ConfidenceInterpretation confidenceInterpretation,
        List<String> limitations) {

    public enum Authority {
        PROPOSAL_ONLY
    }

    public enum Status {
        UNREVIEWED
    }

    public enum ConfidenceInterpretation {
        UNCALIBRATED_RANKING_HINT
    }

    public ScenarioProposal {
        if (authority != Authority.PROPOSAL_ONLY) {
            throw new RuntimeContractException("authority must be PROPOSAL_ONLY");
        }
        if (status != Status.UNREVIEWED) {
            throw new RuntimeContractException("status must be UNREVIEWED");
        }
        if (proposalRevision < 1) {
            throw new RuntimeContractException("proposalRevision must be positive");
        }
        ScenarioContractValidation.binding(sourceRevision, graphDigest);
        if (historyCutoff == null) {
            throw new RuntimeContractException("historyCutoff is required");
        }
        if (behavior == null) {
            throw new RuntimeContractException("behavior is required");
        }
        evidenceReferences = ScenarioContractValidation.nonemptyList(
                evidenceReferences, "evidenceReferences");
        ScenarioContractValidation.nonblank(inferenceRationale, "inferenceRationale");
        if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1) {
            throw new RuntimeContractException("confidence must be finite and between 0 and 1");
        }
        if (confidenceInterpretation != ConfidenceInterpretation.UNCALIBRATED_RANKING_HINT) {
            throw new RuntimeContractException("confidence must be labeled UNCALIBRATED_RANKING_HINT");
        }
        limitations = ScenarioContractValidation.nonemptyStrings(limitations, "limitations");
    }
}

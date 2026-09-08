package com.featuredeliveryintelligence.fdi.product.semantics;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.time.Instant;
import java.util.List;

public record CapabilityProposal(
        ScenarioProposal.Authority authority,
        ScenarioProposal.Status status,
        int proposalRevision,
        String sourceRevision,
        String graphDigest,
        Instant historyCutoff,
        CapabilityBehavior behavior,
        List<ScenarioEvidenceReference> evidenceReferences,
        String inferenceRationale,
        double confidence,
        ScenarioProposal.ConfidenceInterpretation confidenceInterpretation,
        List<String> limitations,
        List<ScenarioProposal> scenarios) {

    public CapabilityProposal {
        if (authority != ScenarioProposal.Authority.PROPOSAL_ONLY) {
            throw new RuntimeContractException("authority must be PROPOSAL_ONLY");
        }
        if (status != ScenarioProposal.Status.UNREVIEWED) {
            throw new RuntimeContractException("status must be UNREVIEWED");
        }
        if (proposalRevision < 1) {
            throw new RuntimeContractException("proposalRevision must be positive");
        }
        ScenarioContractValidation.binding(sourceRevision, graphDigest);
        if (historyCutoff == null || behavior == null) {
            throw new RuntimeContractException("historyCutoff and behavior are required");
        }
        evidenceReferences = ScenarioContractValidation.nonemptyList(
                evidenceReferences, "evidenceReferences");
        ScenarioContractValidation.nonblank(inferenceRationale, "inferenceRationale");
        if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1) {
            throw new RuntimeContractException("confidence must be finite and between 0 and 1");
        }
        if (confidenceInterpretation != ScenarioProposal.ConfidenceInterpretation.UNCALIBRATED_RANKING_HINT) {
            throw new RuntimeContractException("confidence must be labeled UNCALIBRATED_RANKING_HINT");
        }
        limitations = ScenarioContractValidation.nonemptyStrings(limitations, "limitations");
        scenarios = ScenarioContractValidation.nonemptyList(scenarios, "scenarios");
        for (ScenarioProposal scenario : scenarios) {
            if (!behavior.capabilityId().equals(scenario.behavior().capabilityId())) {
                throw new RuntimeContractException("scenario capabilityId mismatch");
            }
            if (scenario.proposalRevision() != proposalRevision
                    || !scenario.sourceRevision().equals(sourceRevision)
                    || !scenario.graphDigest().equals(graphDigest)
                    || !scenario.historyCutoff().equals(historyCutoff)) {
                throw new RuntimeContractException("scenario proposal binding mismatch");
            }
        }
    }
}

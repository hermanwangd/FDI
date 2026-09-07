package com.featuredeliveryintelligence.fdi.reverse.proposal;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseContractValidation;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;

import java.util.List;

/**
 * A proposed Product Capability behavior scenario inferred from corroborated
 * evidence (PKB-REVERSE-002). Wording rules are strict: title and
 * given/when/then must describe externally observable actions and outcomes
 * only and are validated {@link ScenarioTextValidator#requireClean(String, String)
 * free of implementation identifiers}; the technical basis travels separately
 * in the {@link EvidenceCitation citations}. {@link #capabilityId()} must
 * reference a {@link CapabilityProposal} of the same package.
 *
 * <p>Every scenario is generated {@link ScenarioStatus#UNREVIEWED} with
 * {@link ProposalAuthority#PROPOSAL_ONLY} and
 * {@code semanticPublicationAllowed == false}; only a Human Reviewer decision
 * on this exact revision can change that, and confidence is a deterministic
 * ranking hint, never authority.
 */
public record ScenarioProposal(
        String id,
        String capabilityId,
        String title,
        String given,
        String when,
        String then,
        List<EvidenceCitation> citations,
        String sourceRevision,
        List<String> contributingEvidenceDigests,
        String inferenceRationale,
        List<String> limitations,
        long confidenceScale4,
        ScenarioStatus scenarioStatus,
        ProposalAuthority authority,
        boolean semanticPublicationAllowed) {

    public ScenarioProposal {
        id = ProposalValidation.proposalId(id, "HYP-SCENARIO", "scenario proposal");
        capabilityId = ReverseContractValidation.requiredText(capabilityId, "scenario proposal capability id");
        title = ScenarioTextValidator.requireClean(title, "scenario proposal title");
        given = ScenarioTextValidator.requireClean(given, "scenario proposal given");
        when = ScenarioTextValidator.requireClean(when, "scenario proposal when");
        then = ScenarioTextValidator.requireClean(then, "scenario proposal then");
        citations = ProposalValidation.citations(citations, "scenario proposal " + id);
        sourceRevision = ReverseContractValidation.canonicalRevision(sourceRevision);
        contributingEvidenceDigests =
                ProposalValidation.contributingDigests(contributingEvidenceDigests, citations, "scenario proposal " + id);
        inferenceRationale = ReverseContractValidation.requiredText(inferenceRationale, "scenario proposal inference rationale");
        limitations = ProposalValidation.limitations(limitations, "scenario proposal");
        confidenceScale4 = ProposalValidation.confidence(confidenceScale4, "scenario proposal");
        if (scenarioStatus != ScenarioStatus.UNREVIEWED)
            throw new ReverseContractException(
                    ReverseFailure.AUTHORITY_VIOLATION,
                    "scenario proposal status must be UNREVIEWED: " + scenarioStatus);
        authority = ProposalValidation.authority(authority, "scenario proposal");
        semanticPublicationAllowed =
                ProposalValidation.semanticPublicationBlocked(semanticPublicationAllowed, "scenario proposal");
    }
}

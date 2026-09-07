package com.featuredeliveryintelligence.fdi.reverse.proposal;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractValidation;

import java.util.List;

/**
 * A proposed Product Capability inferred from corroborated evidence
 * (PKB-REVERSE-002). A proposal is never Product truth: it carries
 * {@link ProposalAuthority#PROPOSAL_ONLY}, cannot request semantic
 * publication, and awaits Human Reviewer ACCEPT / EDIT / REJECT. Only the
 * Human Reviewer decides whether the evidence expresses Product meaning.
 *
 * <p>Traceability invariants enforced at construction: the identifier matches
 * {@code HYP-CAPABILITY-<stable-key>}; at least one {@link EvidenceCitation}
 * is present (the Slice E generator is required to demand corroboration
 * across channels and to leave single-channel observations as
 * {@link EvidenceGap}s); the contributing digest set equals the cited digest
 * set exactly; the title is validated
 * {@link ScenarioTextValidator#requireClean(String, String) free of
 * implementation identifiers}; and confidence is a scale-4 long
 * ({@link ProposalValidation#CONFIDENCE_SCALE} units of the 0–1 range) —
 * a deterministic ranking hint, explicitly not authority.
 */
public record CapabilityProposal(
        String id,
        String title,
        List<EvidenceCitation> citations,
        String sourceRevision,
        List<String> contributingEvidenceDigests,
        String inferenceRationale,
        List<String> limitations,
        long confidenceScale4,
        ProposalAuthority authority,
        boolean semanticPublicationAllowed) {

    public CapabilityProposal {
        id = ProposalValidation.proposalId(id, "HYP-CAPABILITY", "capability proposal");
        title = ScenarioTextValidator.requireClean(title, "capability proposal title");
        citations = ProposalValidation.citations(citations, "capability proposal " + id);
        sourceRevision = ReverseContractValidation.canonicalRevision(sourceRevision);
        contributingEvidenceDigests =
                ProposalValidation.contributingDigests(contributingEvidenceDigests, citations, "capability proposal " + id);
        inferenceRationale = ReverseContractValidation.requiredText(inferenceRationale, "capability proposal inference rationale");
        limitations = ProposalValidation.limitations(limitations, "capability proposal");
        confidenceScale4 = ProposalValidation.confidence(confidenceScale4, "capability proposal");
        authority = ProposalValidation.authority(authority, "capability proposal");
        semanticPublicationAllowed =
                ProposalValidation.semanticPublicationBlocked(semanticPublicationAllowed, "capability proposal");
    }
}

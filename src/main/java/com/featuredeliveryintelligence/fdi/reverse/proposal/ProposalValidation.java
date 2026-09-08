package com.featuredeliveryintelligence.fdi.reverse.proposal;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseContractValidation;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Shared fail-closed validation for {@link CapabilityProposal} and
 * {@link ScenarioProposal}. Centralizes the rules both records must enforce
 * identically: identifier shape, citation traceability, digest agreement,
 * confidence bounds, and the authority boundary.
 */
final class ProposalValidation {
    private ProposalValidation() {}

    /** Confidence is a scaled long: units of 1/10000 of the 0–1 range, so serialization is deterministic. */
    static final long CONFIDENCE_SCALE = 10_000L;

    /** Deterministic proposal identifier shape, e.g. HYP-CAPABILITY-0001. */
    static String proposalId(String id, String prefix, String what) {
        ReverseContractValidation.requiredText(id, what + " id");
        if (!id.matches(prefix + "-[0-9A-Z][0-9A-Z._-]*"))
            throw new ReverseContractException(
                    ReverseFailure.UNTRACEABLE_PROPOSAL,
                    what + " id must match " + prefix + "-<stable-key>: " + id);
        return id;
    }

    /** Citations sorted by {@link EvidenceCitation#stableOrder()}, duplicate-free, non-empty. */
    static List<EvidenceCitation> citations(List<EvidenceCitation> citations, String what) {
        if (citations == null || citations.isEmpty())
            throw new ReverseContractException(
                    ReverseFailure.UNTRACEABLE_PROPOSAL, what + " must cite at least one evidence observation");
        List<EvidenceCitation> ordered = new ArrayList<>(citations);
        for (EvidenceCitation citation : ordered)
            if (citation == null)
                throw new ReverseContractException(ReverseFailure.MISSING_INPUT, what + " citations must not contain null");
        ordered.sort(EvidenceCitation.stableOrder());
        for (int index = 1; index < ordered.size(); index++) {
            if (ordered.get(index - 1).equals(ordered.get(index)))
                throw new ReverseContractException(
                        ReverseFailure.DUPLICATE_IDENTITY, what + " cites the same observation twice: "
                                + ordered.get(index).observationRef());
        }
        return List.copyOf(ordered);
    }

    /**
     * Contributing digests sorted and deduplicated, and required to match the
     * citation digest set exactly: a proposal may not claim evidence it does
     * not cite, nor cite evidence it does not claim.
     */
    static List<String> contributingDigests(List<String> digests, List<EvidenceCitation> citations, String what) {
        if (digests == null || digests.isEmpty())
            throw new ReverseContractException(
                    ReverseFailure.UNTRACEABLE_PROPOSAL, what + " contributing evidence digests must not be empty");
        Set<String> normalized = new TreeSet<>();
        for (String digest : digests)
            normalized.add(ReverseContractValidation.sha256Hex(digest, what + " contributing evidence"));
        Set<String> cited = new TreeSet<>();
        for (EvidenceCitation citation : citations) cited.add(citation.evidenceDigest());
        if (!normalized.equals(cited))
            throw new ReverseContractException(
                    ReverseFailure.UNTRACEABLE_PROPOSAL,
                    what + " contributing evidence digests must match its cited digests exactly");
        return List.copyOf(new ArrayList<>(normalized));
    }

    static List<String> limitations(List<String> limitations, String what) {
        if (limitations == null)
            throw new ReverseContractException(ReverseFailure.MISSING_INPUT, what + " limitations must not be null");
        for (String limitation : limitations)
            ReverseContractValidation.requiredText(limitation, what + " limitation");
        return List.copyOf(limitations);
    }

    /** Confidence as a scale-4 long in [0, 10000]; a ranking hint, never authority. */
    static long confidence(long confidenceScale4, String what) {
        if (confidenceScale4 < 0 || confidenceScale4 > CONFIDENCE_SCALE)
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT,
                    what + " confidence must be within 0.." + CONFIDENCE_SCALE + ": " + confidenceScale4);
        return confidenceScale4;
    }

    static ProposalAuthority authority(ProposalAuthority authority, String what) {
        if (authority != ProposalAuthority.PROPOSAL_ONLY)
            throw new ReverseContractException(
                    ReverseFailure.AUTHORITY_VIOLATION, what + " authority must be PROPOSAL_ONLY: " + authority);
        return authority;
    }

    static boolean semanticPublicationBlocked(boolean semanticPublicationAllowed, String what) {
        if (semanticPublicationAllowed)
            throw new ReverseContractException(
                    ReverseFailure.AUTHORITY_VIOLATION,
                    what + " may never request semantic publication; generated output is proposal-only");
        return false;
    }
}

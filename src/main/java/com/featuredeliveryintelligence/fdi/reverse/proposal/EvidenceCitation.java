package com.featuredeliveryintelligence.fdi.reverse.proposal;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseContractValidation;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceChannel;

import java.util.Comparator;

/**
 * One evidence citation inside a proposal. It binds a proposal statement to a
 * single normalized observation of one bound evidence channel: the channel,
 * a stable reference into that channel's observation payload (for example a
 * JSON pointer into the normalized observation tree), and the exact SHA-256
 * digest of the channel input the observation came from.
 *
 * <p>Deterministic ordering: channel declaration order, then
 * {@code observationRef}, then {@code evidenceDigest} (see
 * {@link #stableOrder()}). Proposals and packages must hold citations in this
 * order so repeated generation over identical evidence is byte-identical.
 */
public record EvidenceCitation(
        ReverseEvidenceChannel channel,
        String observationRef,
        String evidenceDigest) {

    public EvidenceCitation {
        if (channel == null)
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "citation channel must not be null");
        observationRef = ReverseContractValidation.requiredText(observationRef, "citation observation reference");
        evidenceDigest = ReverseContractValidation.sha256Hex(evidenceDigest, "citation " + observationRef);
    }

    /** Deterministic citation order: channel, then observation reference, then digest. */
    public static Comparator<EvidenceCitation> stableOrder() {
        return Comparator.comparing((EvidenceCitation citation) -> citation.channel())
                .thenComparing(EvidenceCitation::observationRef)
                .thenComparing(EvidenceCitation::evidenceDigest);
    }
}

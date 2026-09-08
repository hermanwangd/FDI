package com.featuredeliveryintelligence.fdi.reverse.proposal;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseContractValidation;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceChannel;

import java.util.Comparator;

/**
 * A single-channel observation that cannot become a proposal. Per
 * PKB-REVERSE-002, proposals require corroborating evidence; an observation
 * supported by only one channel stays an explicit gap with its reason instead
 * of being promoted to a Capability or scenario. Attempting to promote a gap
 * to a proposal is {@code ReverseFailure.EVIDENCE_GAP} in the Slice E
 * generator, never silent success.
 *
 * <p>Deterministic ordering: channel, then {@code observationRef}, then
 * {@code evidenceDigest}, then {@code reason}.
 */
public record EvidenceGap(
        ReverseEvidenceChannel channel,
        String observationRef,
        String evidenceDigest,
        String reason) {

    public EvidenceGap {
        if (channel == null)
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "gap channel must not be null");
        observationRef = ReverseContractValidation.requiredText(observationRef, "gap observation reference");
        evidenceDigest = ReverseContractValidation.sha256Hex(evidenceDigest, "gap " + observationRef);
        reason = ReverseContractValidation.requiredText(reason, "gap reason");
    }

    /** Deterministic gap order: channel, then observation reference, then digest, then reason. */
    public static Comparator<EvidenceGap> stableOrder() {
        return Comparator.comparing((EvidenceGap gap) -> gap.channel())
                .thenComparing(EvidenceGap::observationRef)
                .thenComparing(EvidenceGap::evidenceDigest)
                .thenComparing(EvidenceGap::reason);
    }
}

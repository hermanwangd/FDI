package com.featuredeliveryintelligence.fdi.reverse.proposal;

import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceBundle;

/**
 * Provider-neutral capability for deterministic reverse-proposal generation
 * (PKB-BL-009 Slice E consumes this contract; slices B/C/D build the bundle).
 * An implementation groups corroborated structural, test-behavior, and
 * delivery-history observations into Capability and Behavior Scenario
 * proposals, and leaves single-channel observations as explicit
 * {@link EvidenceGap}s.
 *
 * <p>Determinism contract: implementations must return a package that is
 * identical (and byte-identical when serialized through {@code ReverseJson})
 * across repeated runs over an identical {@link ReverseEvidenceBundle}. That
 * forbids wall-clock timestamps, locale-dependent formatting, HashMap
 * iteration-order dependence, and any unseeded randomness. Ordering is fixed
 * by the contract: capabilities and scenarios sort by id, citations by
 * {@link EvidenceCitation#stableOrder()}, gaps by {@link EvidenceGap#stableOrder()}.
 *
 * <p>Isolation contract: implementations must not read accepted Product
 * Semantics, evaluator gold, review decisions, comparison output, or previous
 * proposal outcomes; doing so is {@code ReverseFailure.EVALUATOR_LEAKAGE}.
 * Output is proposal-only: every proposal carries
 * {@link ProposalAuthority#PROPOSAL_ONLY} and semantic publication stays
 * false. Java validates contracts, citations, provenance, and authority here;
 * it does not decide whether evidence is product-facing — that remains the
 * sole Human Reviewer's decision.
 */
public interface ReverseProposalProvider {

    /**
     * Generates the proposal package for a validated evidence bundle.
     * Implementations must be deterministic and single-threaded and must not
     * mutate the bundle; the parsed channel payloads are defensively copied.
     */
    ReverseProposalPackage propose(ReverseEvidenceBundle evidence);
}

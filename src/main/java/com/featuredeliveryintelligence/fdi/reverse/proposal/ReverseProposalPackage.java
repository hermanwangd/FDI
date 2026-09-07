package com.featuredeliveryintelligence.fdi.reverse.proposal;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseContractValidation;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable proposal package: the sealed output of one reverse-proposal
 * generation run (PKB-REVERSE-002). It binds one repository identity and
 * canonical revision to sorted, duplicate-free lists of
 * {@link CapabilityProposal}, {@link ScenarioProposal}, and {@link EvidenceGap}.
 *
 * <p>Deterministic ordering is part of the contract: capabilities sort by
 * id, scenarios sort by id, evidence gaps sort by
 * {@link EvidenceGap#stableOrder()}, and every citation list inside a
 * proposal sorts by {@link EvidenceCitation#stableOrder()}. Serialization
 * through {@code ReverseJson} is therefore byte-stable for equal packages,
 * and repeated generation over identical evidence must reproduce identical
 * packages.
 *
 * <p>Fail-closed validation: duplicate proposal ids, a scenario whose
 * {@code capabilityId} references no capability in this package, a proposal
 * whose {@code sourceRevision} disagrees with the package revision, or a
 * duplicate gap all raise {@link ReverseContractException} with the stable
 * {@link ReverseFailure} vocabulary. Packages may be empty of proposals when
 * everything is an evidence gap; that is an explicit, reviewable result.
 */
public record ReverseProposalPackage(
        String repositoryId,
        String canonicalRevision,
        List<CapabilityProposal> capabilities,
        List<ScenarioProposal> scenarios,
        List<EvidenceGap> evidenceGaps) {

    /** Schema version of the provider-neutral {@code contracts/reverse-proposal.schema.json} document. */
    public static final String SUPPORTED_SCHEMA_VERSION = "1";

    public ReverseProposalPackage {
        repositoryId = ReverseContractValidation.requiredText(repositoryId, "package repository id");
        canonicalRevision = ReverseContractValidation.canonicalRevision(canonicalRevision);
        capabilities = sortedCopy(capabilities, Comparator.comparing(CapabilityProposal::id), "capabilities");
        scenarios = sortedCopy(scenarios, Comparator.comparing(ScenarioProposal::id), "scenarios");
        evidenceGaps = sortedCopy(evidenceGaps, EvidenceGap.stableOrder(), "evidence gaps");
        Set<String> capabilityIds = new HashSet<>();
        for (CapabilityProposal capability : capabilities) capabilityIds.add(capability.id());
        for (ScenarioProposal scenario : scenarios) {
            if (!capabilityIds.contains(scenario.capabilityId()))
                throw new ReverseContractException(
                        ReverseFailure.UNTRACEABLE_PROPOSAL,
                        "scenario " + scenario.id() + " references capability " + scenario.capabilityId()
                                + " which is not part of this package");
            if (!scenario.sourceRevision().equals(canonicalRevision))
                throw new ReverseContractException(
                        ReverseFailure.REVISION_MISMATCH,
                        "scenario " + scenario.id() + " binds revision " + scenario.sourceRevision()
                                + " but the package binds " + canonicalRevision);
        }
        for (CapabilityProposal capability : capabilities) {
            if (!capability.sourceRevision().equals(canonicalRevision))
                throw new ReverseContractException(
                        ReverseFailure.REVISION_MISMATCH,
                        "capability " + capability.id() + " binds revision " + capability.sourceRevision()
                                + " but the package binds " + canonicalRevision);
        }
    }

    private static <T> List<T> sortedCopy(List<T> values, Comparator<T> order, String what) {
        if (values == null)
            throw new ReverseContractException(ReverseFailure.MISSING_INPUT, "package " + what + " must not be null");
        List<T> copy = new ArrayList<>(values);
        for (T value : copy)
            if (value == null)
                throw new ReverseContractException(ReverseFailure.MISSING_INPUT, "package " + what + " must not contain null");
        copy.sort(order);
        Set<T> duplicates = new HashSet<>();
        for (T value : copy) {
            if (!duplicates.add(value))
                throw new ReverseContractException(
                        ReverseFailure.DUPLICATE_IDENTITY, "duplicate identity in package " + what + ": " + value);
        }
        return List.copyOf(copy);
    }
}

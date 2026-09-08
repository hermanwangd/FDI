package com.featuredeliveryintelligence.fdi.product.realization.directtrace;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.ComponentIdentity;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.DirectProductionSymbolEvidence;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.SeedProvenance;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

/** Deterministic production seeds and explicit gaps derived from one bound extractor output. */
public record DirectTestTrace(
        String repositoryId,
        String sourceRevision,
        String inputPath,
        String inputSha256,
        List<DirectProductionSymbolEvidence> directEvidence,
        List<SeedProvenance> seeds,
        List<UnresolvedDirectReferenceGap> unresolvedGaps,
        List<ComponentIdentity> uniqueProductionComponents,
        List<TraceSourceLocation> uniqueObservationLocations) {
    public DirectTestTrace {
        if (repositoryId == null || repositoryId.isBlank() || sourceRevision == null
                || !sourceRevision.matches("[0-9a-f]{40}") || inputPath == null || inputPath.isBlank()
                || inputSha256 == null || !inputSha256.matches("[0-9a-f]{64}")) {
            throw new RuntimeContractException("invalid direct-test trace binding");
        }
        directEvidence = List.copyOf(directEvidence);
        seeds = List.copyOf(seeds);
        unresolvedGaps = List.copyOf(unresolvedGaps);
        uniqueProductionComponents = List.copyOf(uniqueProductionComponents);
        uniqueObservationLocations = List.copyOf(uniqueObservationLocations);
        if (directEvidence.size() != seeds.size()) {
            throw new RuntimeContractException("every direct observation must have one production seed");
        }
        Set<String> evidenceRefs = new HashSet<>();
        Set<String> seedRefs = new HashSet<>();
        for (int index = 0; index < directEvidence.size(); index++) {
            DirectProductionSymbolEvidence evidence = directEvidence.get(index);
            SeedProvenance seed = seeds.get(index);
            if (!evidenceRefs.add(evidence.evidenceRef()) || !seedRefs.add(seed.seedRef())) {
                throw new RuntimeContractException("duplicate direct evidence or seed identity");
            }
            if (!seed.directEvidenceRef().equals(evidence.evidenceRef())
                    || !seed.productionSeed().equals(evidence.productionSymbol())) {
                throw new RuntimeContractException("direct evidence must bind its exact production seed");
            }
        }
    }
}

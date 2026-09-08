package com.featuredeliveryintelligence.fdi.product.realization.directtrace;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.ComponentIdentity;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.DirectProductionSymbolEvidence;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.SeedProvenance;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Authoritative direct observations and explicit gaps derived from one bound extractor output. */
public record DirectTestTrace(
        String repositoryId,
        String sourceRevision,
        String inputPath,
        String inputSha256,
        List<ResolvedDirectObservation> resolvedObservations,
        List<UnresolvedDirectReferenceGap> unresolvedGaps) {
    private static final Pattern FORBIDDEN = Pattern.compile(
            "(?i)(evaluator(?:[ _/-]+gold)?|gold[ _-]+mapping|ground[ _-]+truth|expected[ _-]+mapping)");

    public DirectTestTrace {
        if (repositoryId == null || repositoryId.isBlank() || sourceRevision == null
                || !sourceRevision.matches("[0-9a-f]{40}") || inputPath == null || inputPath.isBlank()
                || inputSha256 == null || !inputSha256.matches("[0-9a-f]{64}")) {
            throw new RuntimeContractException("invalid direct-test trace binding");
        }
        if (FORBIDDEN.matcher(repositoryId).find() || FORBIDDEN.matcher(inputPath).find()) {
            throw new RuntimeContractException("direct-test trace binding contains evaluator-only vocabulary");
        }
        resolvedObservations = List.copyOf(resolvedObservations);
        unresolvedGaps = List.copyOf(unresolvedGaps);
        if (resolvedObservations.size() != 144 || unresolvedGaps.size() != 891) {
            throw new RuntimeContractException("accepted direct-test trace requires exactly 144 resolved and 891 unresolved observations");
        }
        Set<String> evidenceRefs = new HashSet<>();
        Set<String> seedRefs = new HashSet<>();
        Set<String> observationRefs = new HashSet<>();
        for (ResolvedDirectObservation observation : resolvedObservations) {
            if (!evidenceRefs.add(observation.directEvidence().evidenceRef())
                    || !seedRefs.add(observation.seed().seedRef())
                    || !observationRefs.add(observation.directEvidence().observationRef())) {
                throw new RuntimeContractException("duplicate direct evidence, seed, or observation identity");
            }
        }
        for (UnresolvedDirectReferenceGap gap : unresolvedGaps) {
            if (!observationRefs.add(gap.observationRef())) {
                throw new RuntimeContractException("duplicate resolved or unresolved observation identity");
            }
        }
    }

    public List<DirectProductionSymbolEvidence> directEvidence() {
        return resolvedObservations.stream().map(ResolvedDirectObservation::directEvidence).toList();
    }

    public List<SeedProvenance> seeds() {
        return resolvedObservations.stream().map(ResolvedDirectObservation::seed).toList();
    }

    public List<ComponentIdentity> uniqueProductionComponents() {
        return directEvidence().stream().map(DirectProductionSymbolEvidence::productionSymbol).distinct().toList();
    }

    public List<TraceSourceLocation> uniqueObservationLocations() {
        return resolvedObservations.stream().map(ResolvedDirectObservation::sourceLocation).distinct().toList();
    }
}

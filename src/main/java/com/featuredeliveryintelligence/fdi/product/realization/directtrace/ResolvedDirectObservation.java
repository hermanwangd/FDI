package com.featuredeliveryintelligence.fdi.product.realization.directtrace;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.DirectProductionSymbolEvidence;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.SeedProvenance;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

/** One authoritative extractor observation and its exact v0.4 direct-evidence/seed conversion. */
public record ResolvedDirectObservation(
        DirectProductionSymbolEvidence directEvidence,
        SeedProvenance seed,
        TraceSourceLocation sourceLocation) {
    public ResolvedDirectObservation {
        if (directEvidence == null || seed == null || sourceLocation == null
                || !seed.directEvidenceRef().equals(directEvidence.evidenceRef())
                || !seed.productionSeed().equals(directEvidence.productionSymbol())) {
            throw new RuntimeContractException("resolved observation must bind exact direct evidence, seed, and location");
        }
    }
}

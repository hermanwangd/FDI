package com.featuredeliveryintelligence.fdi.product.realization.route;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable contract for the bounded result of extracting HTTP behavior
 * observations from exact-revision test sources. Observations retain a stable,
 * byte-reproducible order; unsupported or ambiguous expressions are preserved
 * as explicit gaps, never silently dropped. Schema
 * {@code software-factory.sf-bl002-http-behavior-observations.v0.3}.
 */
public record HttpBehaviorExtractionResult(
        String sourceRevision,
        List<HttpBehaviorObservation> observations,
        List<String> gaps) {

    public static final String SCHEMA_VERSION = "software-factory.sf-bl002-http-behavior-observations.v0.3";

    public HttpBehaviorExtractionResult {
        if (sourceRevision == null || !sourceRevision.matches("[0-9a-f]{40}")) {
            throw new RuntimeContractException("sourceRevision must be a full lowercase Git SHA");
        }
        observations = copyObservations(observations);
        gaps = copyGaps(gaps);
    }

    private static List<HttpBehaviorObservation> copyObservations(List<HttpBehaviorObservation> observations) {
        if (observations == null) {
            throw new RuntimeContractException("observations must be non-null");
        }
        Set<String> refs = new HashSet<>();
        List<HttpBehaviorObservation> copy = new ArrayList<>(observations.size());
        for (HttpBehaviorObservation observation : observations) {
            if (observation == null) {
                throw new RuntimeContractException("observations must not contain null entries");
            }
            if (!refs.add(observation.observationRef())) {
                throw new RuntimeContractException("duplicate observationRef " + observation.observationRef());
            }
            copy.add(observation);
        }
        return List.copyOf(copy);
    }

    private static List<String> copyGaps(List<String> gaps) {
        if (gaps == null) {
            throw new RuntimeContractException("gaps must be non-null");
        }
        List<String> copy = new ArrayList<>(gaps.size());
        for (String gap : gaps) {
            if (gap == null || gap.isBlank()) {
                throw new RuntimeContractException("gaps must not contain blank entries");
            }
            copy.add(gap);
        }
        return List.copyOf(copy);
    }
}

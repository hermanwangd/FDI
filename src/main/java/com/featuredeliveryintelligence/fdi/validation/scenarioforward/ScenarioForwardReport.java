package com.featuredeliveryintelligence.fdi.validation.scenarioforward;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.TreeSet;

public record ScenarioForwardReport(
        Status status,
        List<String> reasons,
        List<Object> mappings,
        @JsonProperty("run_id") String runId,
        @JsonProperty("generation_inputs") List<GenerationInput> generationInputs) {

    public ScenarioForwardReport {
        if (status == null || reasons == null || mappings == null || generationInputs == null) {
            throw new RuntimeContractException("scenario forward report fields must not be null");
        }
        try {
            reasons = List.copyOf(new TreeSet<>(reasons));
            generationInputs = status == Status.BLOCKED ? List.of() : List.copyOf(generationInputs);
        } catch (NullPointerException failure) {
            throw new RuntimeContractException("scenario forward report lists must not contain null", failure);
        }
        mappings = List.of();
    }

    public enum Status { CONTRACT_VALID, BLOCKED }

    public record GenerationInput(String kind, String path, String sha256) {
        public GenerationInput {
            if (kind == null || path == null || sha256 == null) {
                throw new RuntimeContractException("generation input fields must not be null");
            }
        }
    }
}

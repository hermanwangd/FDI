package com.featuredeliveryintelligence.fdi.validation.scenarioforward;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record ScenarioForwardRequest(List<BoundInput> inputs, JsonNode proposal) {
    public ScenarioForwardRequest {
        if (inputs == null || proposal == null) {
            throw new RuntimeContractException("scenario forward request fields must not be null");
        }
        try {
            inputs = List.copyOf(inputs);
        } catch (NullPointerException failure) {
            throw new RuntimeContractException("scenario forward request inputs must not contain null", failure);
        }
        proposal = proposal.deepCopy();
    }

    @Override
    public JsonNode proposal() {
        return proposal.deepCopy();
    }

    public record BoundInput(String kind, String path, String sha256) {
        public BoundInput {
            if (kind == null || path == null || sha256 == null) {
                throw new RuntimeContractException("bound input fields must not be null");
            }
        }
    }
}

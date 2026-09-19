package com.featuredeliveryintelligence.fdi.engcim.control;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.List;

/** Immutable v0.3 description of one reusable Engineering Control. */
public record EngineeringControlDefinition(
        String controlRef,
        String name,
        String objective,
        List<String> subjectRequirements,
        List<String> evidenceRequirements,
        String predicate) {

    public EngineeringControlDefinition {
        require(controlRef, "controlRef");
        require(name, "name");
        require(objective, "objective");
        require(predicate, "predicate");
        subjectRequirements = copy(subjectRequirements, "subjectRequirements");
        evidenceRequirements = copy(evidenceRequirements, "evidenceRequirements");
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new RuntimeContractException(field + " is required");
        }
        return value;
    }

    private static List<String> copy(List<String> values, String field) {
        if (values == null || values.isEmpty() || values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new RuntimeContractException(field + " must contain nonblank values");
        }
        return List.copyOf(values);
    }
}

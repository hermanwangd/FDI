package com.featuredeliveryintelligence.fdi.engcim.control;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.List;

/** Scenario composition contract. Runtime scheduling state intentionally does not belong here. */
public record EngineeringScenarioDefinition(
        String scenarioRef,
        String name,
        String goal,
        List<String> inputRequirements,
        List<String> contextRequirements,
        List<String> skillRefs,
        List<String> controlRefs,
        List<String> outputRequirements,
        List<String> successCriteria) {

    public EngineeringScenarioDefinition {
        require(scenarioRef, "scenarioRef");
        require(name, "name");
        require(goal, "goal");
        inputRequirements = copy(inputRequirements, "inputRequirements");
        contextRequirements = copy(contextRequirements, "contextRequirements");
        skillRefs = copy(skillRefs, "skillRefs");
        controlRefs = copy(controlRefs, "controlRefs");
        outputRequirements = copy(outputRequirements, "outputRequirements");
        successCriteria = copy(successCriteria, "successCriteria");
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new RuntimeContractException(field + " is required");
        }
        return value;
    }

    private static List<String> copy(List<String> values, String field) {
        if (values == null || values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new RuntimeContractException(field + " must contain nonblank values");
        }
        return List.copyOf(values);
    }
}

package com.featuredeliveryintelligence.fdi.engcim.control;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.List;

/** Actual output of an Engineering Control evaluator; outcomes are not PASS/FAIL. */
public record EngineeringControlResult(
        String resultRef,
        String controlRef,
        List<String> subjectRefs,
        Outcome outcome,
        List<String> reasonCodes,
        List<String> evidenceRefs) {

    public enum Outcome {
        SATISFIED,
        UNSATISFIED,
        INCONCLUSIVE
    }

    public EngineeringControlResult {
        require(resultRef, "resultRef");
        require(controlRef, "controlRef");
        if (outcome == null) {
            throw new RuntimeContractException("outcome is required");
        }
        subjectRefs = copy(subjectRefs, "subjectRefs");
        reasonCodes = copy(reasonCodes, "reasonCodes");
        evidenceRefs = copy(evidenceRefs, "evidenceRefs");
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

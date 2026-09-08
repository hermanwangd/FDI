package com.featuredeliveryintelligence.fdi.product.semantics;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.List;

public record BehaviorScenario(
        String capabilityId,
        String scenarioId,
        String title,
        List<String> given,
        String when,
        List<String> then,
        Scope scope) {

    public enum Scope {
        REQUIRED_ACCEPTANCE,
        ILLUSTRATIVE
    }

    public BehaviorScenario {
        if (capabilityId == null
                || !ScenarioContractValidation.CAPABILITY_ID.matcher(capabilityId).matches()) {
            throw new RuntimeContractException("capabilityId must be a stable HYP-CAPABILITY-* identifier");
        }
        if (scenarioId == null
                || !ScenarioContractValidation.SCENARIO_ID.matcher(scenarioId).matches()) {
            throw new RuntimeContractException("scenarioId must be a stable HYP-SCENARIO-* identifier");
        }
        ScenarioContractValidation.semanticText(title, "title");
        given = ScenarioContractValidation.semanticTextList(given, "given");
        ScenarioContractValidation.semanticText(when, "when");
        then = ScenarioContractValidation.semanticTextList(then, "then");
        if (scope == null) {
            throw new RuntimeContractException("scope is required");
        }
    }
}

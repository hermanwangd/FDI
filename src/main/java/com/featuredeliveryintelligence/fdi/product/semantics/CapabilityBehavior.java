package com.featuredeliveryintelligence.fdi.product.semantics;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.List;

public record CapabilityBehavior(
        String capabilityId,
        String title,
        String description,
        List<String> includes,
        List<String> excludes,
        List<String> nonGoals) {

    public CapabilityBehavior {
        if (capabilityId == null
                || !ScenarioContractValidation.CAPABILITY_ID.matcher(capabilityId).matches()) {
            throw new RuntimeContractException("capabilityId must be a stable HYP-CAPABILITY-* identifier");
        }
        ScenarioContractValidation.semanticText(title, "title");
        ScenarioContractValidation.semanticText(description, "description");
        includes = ScenarioContractValidation.semanticTextList(includes, "includes");
        excludes = ScenarioContractValidation.semanticTextList(excludes, "excludes");
        nonGoals = ScenarioContractValidation.semanticTextList(nonGoals, "nonGoals");
    }
}

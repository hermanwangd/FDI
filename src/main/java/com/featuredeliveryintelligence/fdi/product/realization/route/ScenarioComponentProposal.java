package com.featuredeliveryintelligence.fdi.product.realization.route;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable contract for one scenario's ordered component proposals. A proposal
 * is {@link Outcome#MAPPING_PROPOSAL} only when at least one component carries
 * qualified proof ({@link EvidenceStrength#EXACT_ROUTE_HANDLER} or
 * {@link EvidenceStrength#DIRECT_PRODUCTION_REFERENCE}); diagnostic
 * {@link EvidenceStrength#GRAPH_TRACE_SUPPORT} never qualifies a scenario by
 * itself. Schema
 * {@code software-factory.sf-bl002-scenario-component-proposal.v0.3}.
 */
public record ScenarioComponentProposal(
        String scenarioId,
        Outcome outcome,
        List<Component> components,
        List<String> gaps) {

    public static final String SCHEMA_VERSION = "software-factory.sf-bl002-scenario-component-proposal.v0.3";

    public enum Outcome {
        MAPPING_PROPOSAL,
        UNRESOLVED
    }

    public enum EvidenceStrength {
        EXACT_ROUTE_HANDLER,
        DIRECT_PRODUCTION_REFERENCE,
        GRAPH_TRACE_SUPPORT
    }

    public record Component(
            String role,
            EvidenceStrength evidenceStrength,
            String productionIdentity,
            List<String> evidenceRefs,
            String relationshipTrace) {

        public Component {
            if (role == null || role.isBlank()) {
                throw new RuntimeContractException("role must be non-blank");
            }
            if (evidenceStrength == null) {
                throw new RuntimeContractException("evidenceStrength is required");
            }
            if (productionIdentity == null || productionIdentity.isBlank()
                    || !productionIdentity.contains("#")) {
                throw new RuntimeContractException("productionIdentity must qualify a production method");
            }
            evidenceRefs = copyEvidenceRefs(evidenceRefs);
            if (relationshipTrace != null && relationshipTrace.isBlank()) {
                throw new RuntimeContractException("relationshipTrace must be non-blank when present");
            }
        }

        private static List<String> copyEvidenceRefs(List<String> evidenceRefs) {
            if (evidenceRefs == null || evidenceRefs.isEmpty()) {
                throw new RuntimeContractException("evidenceRefs must be non-empty");
            }
            List<String> copy = new ArrayList<>(evidenceRefs.size());
            String previous = null;
            for (String evidenceRef : evidenceRefs) {
                if (evidenceRef == null || evidenceRef.isBlank()) {
                    throw new RuntimeContractException("evidenceRefs must not contain blank entries");
                }
                if (previous != null && evidenceRef.compareTo(previous) <= 0) {
                    throw new RuntimeContractException("evidenceRefs must be strictly ordered without duplicates");
                }
                copy.add(evidenceRef);
                previous = evidenceRef;
            }
            return List.copyOf(copy);
        }
    }

    public ScenarioComponentProposal {
        if (scenarioId == null || scenarioId.isBlank()) {
            throw new RuntimeContractException("scenarioId must be non-blank");
        }
        if (outcome == null) {
            throw new RuntimeContractException("outcome is required");
        }
        components = copyComponents(components);
        gaps = copyGaps(gaps);
        boolean qualified = components.stream().anyMatch(component ->
                component.evidenceStrength() != EvidenceStrength.GRAPH_TRACE_SUPPORT);
        if (outcome == Outcome.MAPPING_PROPOSAL && !qualified) {
            throw new RuntimeContractException(
                    "MAPPING_PROPOSAL requires at least one qualified component proof");
        }
        if (outcome == Outcome.UNRESOLVED && qualified) {
            throw new RuntimeContractException(
                    "UNRESOLVED cannot carry a qualified component proof");
        }
    }

    private static List<Component> copyComponents(List<Component> components) {
        if (components == null) {
            throw new RuntimeContractException("components must be non-null");
        }
        List<Component> copy = new ArrayList<>(components.size());
        for (Component component : components) {
            if (component == null) {
                throw new RuntimeContractException("components must not contain null entries");
            }
            copy.add(component);
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

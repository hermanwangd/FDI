package com.featuredeliveryintelligence.fdi.product.realization;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provider-neutral scenario mapping proposal. Evidence references and the bound scenario
 * set are caller-supplied assertions: this contract does not resolve graph evidence or
 * authenticate Human Reviewer approval. The frozen-input gate owns those checks.
 */
public record ScenarioRealizationProposal(
        String capabilityId,
        RealizationProposal.Outcome outcome,
        EvidenceStatus evidenceStatus,
        String sourceRevision,
        List<BoundScenario> boundScenarios,
        List<ComponentReference> components,
        List<ScenarioTrace> scenarioTraces,
        List<String> limitations) {

    public enum EvidenceStatus { COMPLETE, PARTIAL, INSUFFICIENT }
    public enum StepState { EVIDENCED, EVIDENCE_GAP, NOT_APPLICABLE }

    public String authority() { return "PROPOSAL_ONLY"; }

    public ScenarioRealizationProposal {
        required(capabilityId, "capabilityId");
        if (evidenceStatus == null) fail("evidenceStatus is required");
        boundScenarios = snapshot(boundScenarios, "boundScenarios", true);
        components = snapshot(components, "components", false);
        scenarioTraces = snapshot(scenarioTraces, "scenarioTraces", true);
        limitations = strings(limitations, "limitations", true, false);
        // Preserve the established outcome, role, revision and limitation contract.
        new RealizationProposal(capabilityId, outcome, sourceRevision,
                components.stream().map(ComponentReference::component).toList(), limitations);

        Set<String> expected = new HashSet<>();
        for (BoundScenario scenario : boundScenarios) {
            if (!capabilityId.equals(scenario.capabilityId())) fail("bound scenario parent mismatch");
            if (!expected.add(scenario.scenarioId())) fail("duplicate bound scenario");
        }
        Set<String> componentRefs = new HashSet<>();
        Set<NormalizedIdentity> identities = new HashSet<>();
        for (ComponentReference component : components) {
            if (!componentRefs.add(component.componentRef())) fail("duplicate component reference");
            if (!identities.add(NormalizedIdentity.of(component.component().identity())))
                fail("duplicate normalized component identity");
        }
        Set<String> seen = new HashSet<>();
        Set<String> used = new HashSet<>();
        for (ScenarioTrace trace : scenarioTraces) {
            if (!capabilityId.equals(trace.capabilityId())) fail("scenario trace parent mismatch");
            if (!expected.contains(trace.scenarioId())) fail("scenario is not in bound scenario set");
            if (!seen.add(trace.scenarioId())) fail("duplicate scenario trace");
            for (ChainStep step : trace.steps()) {
                if (evidenceStatus == EvidenceStatus.COMPLETE && step.state() == StepState.EVIDENCE_GAP)
                    fail("COMPLETE evidence cannot contain an evidence gap");
                for (String ref : step.componentRefs()) {
                    if (!componentRefs.contains(ref)) fail("dangling component reference");
                    used.add(ref);
                }
            }
        }
        if (!seen.equals(expected)) fail("every bound scenario requires a trace");
        if (!used.equals(componentRefs)) fail("every component requires scenario chain use");
    }

    /** Explicit scenario membership; deliberately carries no copied Product behavior. */
    public record BoundScenario(String scenarioId, String capabilityId) {
        public BoundScenario {
            required(scenarioId, "scenarioId");
            required(capabilityId, "capabilityId");
        }
    }

    /**
     * Direct method observations are declared separately from selected components.
     * A containing type or file needs an explanation when it replaces a declared method.
     * Completeness and truth of these observations require external evidence checks.
     */
    public record ComponentReference(
            String componentRef,
            RealizationComponent component,
            List<StructuralComponentIdentity> directlyEvidencedMethods,
            String containingComponentReason) {
        public ComponentReference {
            required(componentRef, "componentRef");
            if (component == null) fail("component is required");
            directlyEvidencedMethods = snapshot(directlyEvidencedMethods, "directlyEvidencedMethods", false);
            optional(containingComponentReason, "containingComponentReason");
            Set<NormalizedIdentity> seen = new HashSet<>();
            var selected = component.identity();
            for (var method : directlyEvidencedMethods) {
                if (method.granularity() != StructuralComponentIdentity.Granularity.METHOD)
                    fail("direct method evidence must identify a METHOD");
                if (!method.sourceRevision().equals(selected.sourceRevision())) fail("direct method revision mismatch");
                if (!seen.add(NormalizedIdentity.of(method))) fail("duplicate direct method evidence");
                boolean containingFile = selected.granularity() == StructuralComponentIdentity.Granularity.FILE;
                boolean containingType = selected.granularity() == StructuralComponentIdentity.Granularity.TYPE
                        && isContainingType(selected.qualifiedSymbol(), method.qualifiedSymbol());
                if (selected.sourcePath().equals(method.sourcePath()) && (containingFile || containingType))
                    required(containingComponentReason, "containingComponentReason for direct method replacement");
            }
        }
    }

    public record ScenarioTrace(String scenarioId, String capabilityId, List<ChainStep> steps) {
        public ScenarioTrace {
            required(scenarioId, "scenarioId");
            required(capabilityId, "capabilityId");
            steps = snapshot(steps, "steps", true);
        }
    }

    public record ChainStep(
            String behavioralFunction,
            StepState state,
            List<String> componentRefs,
            List<String> evidenceRefs,
            String evidenceGap,
            String notApplicableReason) {
        public String authority() { return "PROPOSAL_ONLY"; }

        public ChainStep {
            required(behavioralFunction, "behavioralFunction");
            if (state == null) fail("step state is required");
            componentRefs = strings(componentRefs, "componentRefs", false, true);
            evidenceRefs = strings(evidenceRefs, "evidenceRefs", state == StepState.EVIDENCED, true);
            optional(evidenceGap, "evidenceGap");
            optional(notApplicableReason, "notApplicableReason");
            if (state == StepState.EVIDENCE_GAP) required(evidenceGap, "evidenceGap");
            else if (evidenceGap != null) fail("evidenceGap belongs only to EVIDENCE_GAP");
            if (state == StepState.NOT_APPLICABLE) required(notApplicableReason, "notApplicableReason");
            else if (notApplicableReason != null) fail("notApplicableReason belongs only to NOT_APPLICABLE");
        }
    }

    // Qualified method symbols use their declaring type followed by a dot. Requiring
    // a single member segment avoids treating a nested type's method as its parent's.
    private static boolean isContainingType(String type, String method) {
        if (!method.startsWith(type + ".")) return false;
        String member = method.substring(type.length() + 1);
        int signature = member.indexOf('(');
        String name = signature < 0 ? member : member.substring(0, signature);
        return !name.isEmpty() && !name.contains(".");
    }

    private record NormalizedIdentity(String revision, String path,
            StructuralComponentIdentity.Granularity granularity, String symbol) {
        static NormalizedIdentity of(StructuralComponentIdentity identity) {
            return new NormalizedIdentity(identity.sourceRevision(), identity.sourcePath(),
                    identity.granularity(), identity.qualifiedSymbol());
        }
    }

    private static <T> List<T> snapshot(List<T> values, String field, boolean nonempty) {
        if (values == null) fail(field + " is required");
        var copy = new ArrayList<>(values);
        if (nonempty && copy.isEmpty()) fail(field + " must not be empty");
        if (copy.contains(null)) fail(field + " cannot contain null elements");
        return Collections.unmodifiableList(copy);
    }

    private static List<String> strings(List<String> values, String field, boolean nonempty, boolean unique) {
        var copy = snapshot(values, field, nonempty);
        Set<String> seen = new HashSet<>();
        for (String value : copy) {
            required(value, field + " element");
            if (unique && !seen.add(value)) fail(field + " contains duplicate references");
        }
        return copy;
    }

    private static void optional(String value, String field) {
        if (value != null) required(value, field);
    }

    private static void required(String value, String field) {
        if (value == null || value.isBlank()) fail(field + " must be nonblank");
    }

    private static void fail(String message) { throw new RuntimeContractException(message); }
}

package com.featuredeliveryintelligence.fdi.engcim.control;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Minimal runtime adapter for evaluating binding contracts at governed gates.
 * It has no Scenario scheduler, persistence store, or issue-status semantics.
 */
public final class ControlEvidenceBindingExecutor {
    private final EngineeringControlEvaluator evaluator;

    public ControlEvidenceBindingExecutor() {
        this(new EngineeringControlEvaluator());
    }

    ControlEvidenceBindingExecutor(EngineeringControlEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    /**
     * Resolves and evaluates every binding for one gate. A gate proceeds only
     * when every actual Control result is SATISFIED.
     */
    public GateDecision evaluateGate(String gateRef,
                                     List<ControlEvidenceBinding> bindings,
                                     EvidenceResolver resolver,
                                     ResultSink resultSink) {
        require(gateRef, "gateRef");
        if (bindings == null || bindings.isEmpty()) {
            throw new IllegalArgumentException("bindings must contain at least one binding");
        }
        if (resolver == null || resultSink == null) {
            throw new IllegalArgumentException("resolver and resultSink are required");
        }

        List<PersistedControlResult> persisted = new ArrayList<>();
        Set<String> reasons = new TreeSet<>();
        for (ControlEvidenceBinding binding : bindings) {
            if (!gateRef.equals(binding.gateRef())) {
                throw new IllegalArgumentException("binding gateRef does not match gate: " + binding.bindingRef());
            }
            ResolvedInputs resolved = resolveSafely(binding, resolver);
            JsonNode subject = resolved == null ? null : resolved.subject();
            JsonNode evidence = resolved == null ? null : resolved.evidence();
            if (!matchesBinding(binding, subject, evidence)) {
                subject = null;
                evidence = null;
            }

            EngineeringControlResult result = evaluator.evaluate(binding.controlRef(), subject, evidence);
            PersistedControlResult persistedResult = new PersistedControlResult(binding, result);
            resultSink.persist(persistedResult);
            persisted.add(persistedResult);
            if (result.outcome() == EngineeringControlResult.Outcome.UNSATISFIED) {
                reasons.add("UNSATISFIED_CONTROL_RESULT");
            } else if (result.outcome() == EngineeringControlResult.Outcome.INCONCLUSIVE) {
                reasons.add("INCONCLUSIVE_CONTROL_RESULT");
            }
        }

        List<String> resultRefs = persisted.stream()
                .map(item -> item.result().resultRef())
                .toList();
        return new GateDecision(gateRef, resultRefs, reasons.isEmpty(), List.copyOf(reasons));
    }

    private static ResolvedInputs resolveSafely(ControlEvidenceBinding binding, EvidenceResolver resolver) {
        try {
            return resolver.resolve(binding);
        } catch (RuntimeException ignored) {
            // A resolver failure is missing runtime evidence, never permission to continue.
            return null;
        }
    }

    private static boolean matchesBinding(ControlEvidenceBinding binding, JsonNode subject, JsonNode evidence) {
        if (subject == null || evidence == null || !subject.isObject() || !evidence.isObject()) return false;
        String subjectRef = text(subject, "subjectRef");
        if (subjectRef.isBlank()) subjectRef = text(subject, "currentSubjectRef");
        if (!binding.subjectRef().equals(subjectRef)) return false;
        Set<String> resolvedRefs = new LinkedHashSet<>();
        addArrayTexts(resolvedRefs, evidence, "evidenceRefs");
        for (String path : List.of("authorityDecision", "evaluation", "resolutionEvidence")) {
            String ref = text(evidence.get(path), "evidenceRef");
            if (!ref.isBlank()) resolvedRefs.add(ref);
        }
        JsonNode entries = evidence.get("evidence");
        if (entries != null && entries.isArray()) {
            for (JsonNode entry : entries) {
                String ref = text(entry, "ref");
                if (!ref.isBlank()) resolvedRefs.add(ref);
            }
        }
        return resolvedRefs.containsAll(binding.evidenceRefs());
    }

    private static void addArrayTexts(Set<String> target, JsonNode node, String field) {
        JsonNode values = node == null ? null : node.get(field);
        if (values == null || !values.isArray()) return;
        for (JsonNode value : values) if (value.isTextual() && !value.asText().isBlank()) target.add(value.asText());
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && value.isTextual() ? value.asText() : "";
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }

    @FunctionalInterface
    public interface EvidenceResolver {
        ResolvedInputs resolve(ControlEvidenceBinding binding);
    }

    @FunctionalInterface
    public interface ResultSink {
        void persist(PersistedControlResult result);
    }

    public record ResolvedInputs(JsonNode subject, JsonNode evidence) {
    }

    public record PersistedControlResult(ControlEvidenceBinding binding, EngineeringControlResult result) {
        public PersistedControlResult {
            if (binding == null || result == null) throw new IllegalArgumentException("binding and result are required");
        }
    }

    public record GateDecision(String gateRef, List<String> resultRefs, boolean proceed, List<String> reasonCodes) {
        public GateDecision {
            require(gateRef, "gateRef");
            resultRefs = List.copyOf(resultRefs);
            reasonCodes = List.copyOf(new TreeSet<>(reasonCodes));
        }
    }
}

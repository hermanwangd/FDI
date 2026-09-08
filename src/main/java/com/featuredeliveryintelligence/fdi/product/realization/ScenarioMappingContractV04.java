package com.featuredeliveryintelligence.fdi.product.realization;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Provider-neutral, proposal-only mapping contract for PK-S1 v0.4. */
public record ScenarioMappingContractV04(
        String capabilityId,
        String scenarioId,
        String sourceRevision,
        String frozenSemanticsSha256,
        List<DirectProductionSymbolEvidence> directProductionSymbols,
        List<SeedProvenance> seeds,
        List<RealizationChainStep> realizationChain,
        List<String> limitations) {

    public static final String SCHEMA_VERSION = "pkb001.realization-mapping.v0.4";
    public static final String AUTHORITY = "PROPOSAL_ONLY";

    public String schemaVersion() { return SCHEMA_VERSION; }
    public String authority() { return AUTHORITY; }

    public ScenarioMappingContractV04 {
        required(capabilityId, "capabilityId");
        required(scenarioId, "scenarioId");
        if (forbidden(capabilityId) || forbidden(scenarioId)) fail("evaluator identity is forbidden");
        revision(sourceRevision);
        if (frozenSemanticsSha256 == null || !frozenSemanticsSha256.matches("[0-9a-f]{64}")) {
            fail("frozenSemanticsSha256 must be a lowercase SHA-256");
        }
        directProductionSymbols = snapshot(directProductionSymbols, "directProductionSymbols", true);
        seeds = snapshot(seeds, "seeds", true);
        realizationChain = snapshot(realizationChain, "realizationChain", true);
        limitations = strings(limitations, "limitations", true);
        for (String limitation : limitations) if (forbidden(limitation)) fail("evaluator evidence is forbidden");

        Map<String, DirectProductionSymbolEvidence> evidenceByRef = new HashMap<>();
        Set<ComponentIdentity> identities = new HashSet<>();
        for (var evidence : directProductionSymbols) {
            if (!sourceRevision.equals(evidence.productionSymbol().sourceRevision())) fail("direct symbol revision mismatch");
            if (evidenceByRef.put(evidence.evidenceRef(), evidence) != null) fail("duplicate direct evidence reference");
            if (!identities.add(evidence.productionSymbol())) fail("duplicate component identity");
        }
        Map<String, SeedProvenance> seedByRef = new HashMap<>();
        for (var seed : seeds) {
            var evidence = evidenceByRef.get(seed.directEvidenceRef());
            if (evidence == null || !evidence.productionSymbol().equals(seed.productionSeed())) fail("unbound seed");
            if (seedByRef.put(seed.seedRef(), seed) != null) fail("duplicate seed reference");
        }
        for (int index = 0; index < realizationChain.size(); index++) {
            var step = realizationChain.get(index);
            if (step.order() != index + 1) fail("realization chain order must be contiguous from one");
            if (!sourceRevision.equals(step.component().sourceRevision())) fail("chain component revision mismatch");
            if (!seedByRef.containsKey(step.seedRef())) fail("unbound chain seed");
            if (step.relationshipBasis() == RelationshipBasis.DIRECT_TEST_REFERENCE
                    && step.traceRefs().stream().noneMatch(evidenceByRef::containsKey)) {
                fail("direct relationship requires direct-production-symbol evidence");
            }
            if (step.relationshipBasis() == RelationshipBasis.DIRECT_TEST_REFERENCE) {
                boolean exactDirectIdentity = step.traceRefs().stream().map(evidenceByRef::get)
                        .filter(java.util.Objects::nonNull)
                        .anyMatch(evidence -> evidence.productionSymbol().equals(step.component()));
                if (!exactDirectIdentity) fail("direct relationship identity must match its evidence");
            }
            if (step.relationshipBasis() == RelationshipBasis.GRAPHIFY_INFERRED && step.traceRefs().isEmpty()) {
                fail("Graphify inferred relationship requires a trace");
            }
        }
    }

    public enum RelationshipBasis { DIRECT_TEST_REFERENCE, GRAPHIFY_INFERRED }
    public enum Granularity { REPOSITORY, FILE, TYPE, METHOD, TEMPLATE, CONFIGURATION }

    /** Stable framework identity; provider node IDs remain diagnostics outside equality. */
    public record ComponentIdentity(String sourceRevision, String sourcePath, Granularity granularity,
                                    String qualifiedSymbol) {
        public ComponentIdentity {
            revision(sourceRevision);
            required(sourcePath, "sourcePath");
            if (granularity == null) fail("granularity is required");
            required(qualifiedSymbol, "qualifiedSymbol");
            if (sourcePath.startsWith("/") || sourcePath.contains("\\") || sourcePath.contains("..")
                    || sourcePath.matches("^[A-Za-z]:.*") || sourcePath.matches("(^|/)src/test(/|$).*")
                    || sourcePath.matches("(^|/)test(s)?(/|$).*") || sourcePath.matches(".*Test(s)?\\.java$")) {
                fail("component must be a canonical production path");
            }
        }
    }

    public record DirectProductionSymbolEvidence(String evidenceRef, String observationRef,
                                                  ComponentIdentity productionSymbol) {
        public DirectProductionSymbolEvidence {
            required(evidenceRef, "evidenceRef");
            required(observationRef, "observationRef");
            if (forbidden(observationRef)) fail("evaluator evidence is forbidden");
            if (productionSymbol == null) fail("productionSymbol is required");
        }
    }

    public record SeedProvenance(String seedRef, String directEvidenceRef, ComponentIdentity productionSeed) {
        public SeedProvenance {
            required(seedRef, "seedRef");
            required(directEvidenceRef, "directEvidenceRef");
            if (productionSeed == null) fail("productionSeed is required");
        }
    }

    public record RealizationChainStep(int order, ComponentIdentity component,
                                       RelationshipBasis relationshipBasis, String seedRef,
                                       List<String> traceRefs) {
        public RealizationChainStep {
            if (order < 1) fail("order must be positive");
            if (component == null) fail("component is required");
            if (relationshipBasis == null) fail("relationshipBasis is required");
            required(seedRef, "seedRef");
            traceRefs = strings(traceRefs, "traceRefs", false);
            for (String trace : traceRefs) if (forbidden(trace)) fail("evaluator evidence is forbidden");
        }
    }

    private static boolean forbidden(String value) {
        String normalized = value.toLowerCase(java.util.Locale.ROOT).replace('_', '-');
        return normalized.contains("evaluator") || normalized.contains("gold-mapping")
                || normalized.contains("ground-truth") || normalized.contains("expected-mapping");
    }

    private static void revision(String value) {
        if (value == null || !value.matches("[0-9a-f]{40}")) fail("sourceRevision must be a full lowercase Git SHA");
    }

    private static <T> List<T> snapshot(List<T> values, String field, boolean nonempty) {
        if (values == null) fail(field + " is required");
        var copy = new ArrayList<>(values);
        if (nonempty && copy.isEmpty()) fail(field + " must not be empty");
        if (copy.contains(null)) fail(field + " cannot contain null elements");
        return Collections.unmodifiableList(copy);
    }

    private static List<String> strings(List<String> values, String field, boolean nonempty) {
        var copy = snapshot(values, field, nonempty);
        Set<String> unique = new HashSet<>();
        for (String value : copy) {
            required(value, field + " element");
            if (!unique.add(value)) fail(field + " contains duplicates");
        }
        return copy;
    }

    private static void required(String value, String field) {
        if (value == null || value.isBlank()) fail(field + " is required");
    }

    private static void fail(String message) { throw new RuntimeContractException(message); }
}

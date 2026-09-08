package com.featuredeliveryintelligence.fdi.product.realization;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import java.util.*;
import java.util.regex.Pattern;

/** Executable provider-neutral, proposal-only mapping contract for PK-S1 v0.4. */
public record ScenarioMappingContractV04(
        String schemaVersion, String authority, String capabilityId, String scenarioId,
        String sourceRevision, String graphSha256, String frozenSemanticsSha256,
        Outcome outcome, EvidenceStatus evidenceStatus,
        List<DirectProductionSymbolEvidence> directProductionSymbols, List<SeedProvenance> seeds,
        List<RelationshipTrace> relationshipTraces, List<RealizationChainStep> realizationChain,
        List<String> evidenceGaps, List<String> limitations) {

    public static final String SCHEMA_VERSION = "pkb001.realization-mapping.v0.4";
    public static final String AUTHORITY = "PROPOSAL_ONLY";
    public enum Outcome { MAPPING_PROPOSAL, UNRESOLVED }
    public enum EvidenceStatus { COMPLETE, PARTIAL, INSUFFICIENT }
    public enum RelationshipBasis { DIRECT_TEST_REFERENCE, GRAPHIFY_INFERRED }
    public enum Granularity { REPOSITORY, FILE, TYPE, METHOD, TEMPLATE, CONFIGURATION }

    public ScenarioMappingContractV04 {
        if (!SCHEMA_VERSION.equals(schemaVersion)) fail("schemaVersion is not selected v0.4");
        if (!AUTHORITY.equals(authority)) fail("authority must be PROPOSAL_ONLY");
        safe(capabilityId, "capabilityId"); safe(scenarioId, "scenarioId"); revision(sourceRevision);
        digest(graphSha256, "graphSha256");
        digest(frozenSemanticsSha256, "frozenSemanticsSha256");
        if (outcome == null) fail("outcome is required");
        if (evidenceStatus == null) fail("evidenceStatus is required");
        directProductionSymbols = snapshot(directProductionSymbols, "directProductionSymbols");
        seeds = snapshot(seeds, "seeds"); relationshipTraces = snapshot(relationshipTraces, "relationshipTraces");
        realizationChain = snapshot(realizationChain, "realizationChain");
        evidenceGaps = safeStrings(evidenceGaps, "evidenceGaps"); limitations = safeStrings(limitations, "limitations");
        if (limitations.isEmpty()) fail("limitations must not be empty");

        if (outcome == Outcome.UNRESOLVED) {
            if (evidenceStatus != EvidenceStatus.INSUFFICIENT) fail("UNRESOLVED requires INSUFFICIENT evidence");
            if (!directProductionSymbols.isEmpty() || !seeds.isEmpty() || !relationshipTraces.isEmpty()
                    || !realizationChain.isEmpty()) fail("UNRESOLVED cannot claim realization evidence");
            if (evidenceGaps.isEmpty()) fail("UNRESOLVED requires an explicit evidence gap");
        } else {
            if (evidenceStatus == EvidenceStatus.INSUFFICIENT) fail("MAPPING_PROPOSAL cannot have INSUFFICIENT evidence");
            if (directProductionSymbols.isEmpty() || seeds.isEmpty() || realizationChain.isEmpty())
                fail("MAPPING_PROPOSAL requires direct production evidence, seeds, and a chain");
            if (evidenceStatus == EvidenceStatus.COMPLETE && !evidenceGaps.isEmpty()) fail("COMPLETE evidence cannot contain gaps");
            if (evidenceStatus == EvidenceStatus.PARTIAL && evidenceGaps.isEmpty()) fail("PARTIAL evidence requires a gap");
            validateMapping(sourceRevision, graphSha256, directProductionSymbols, seeds, relationshipTraces, realizationChain);
        }
    }

    private static void validateMapping(String revision, String graphDigest,
            List<DirectProductionSymbolEvidence> direct, List<SeedProvenance> seeds,
            List<RelationshipTrace> traces, List<RealizationChainStep> chain) {
        Map<String, DirectProductionSymbolEvidence> evidenceByRef = uniqueMap(direct,
                DirectProductionSymbolEvidence::evidenceRef, "direct evidence");
        Set<ComponentIdentity> identities = new HashSet<>();
        for (var evidence : direct) {
            sameRevision(revision, evidence.productionSymbol());
            if (!identities.add(evidence.productionSymbol())) fail("duplicate provider-neutral component identity");
        }
        Map<String, SeedProvenance> seedByRef = uniqueMap(seeds, SeedProvenance::seedRef, "seed");
        for (var seed : seeds) {
            var evidence = evidenceByRef.get(seed.directEvidenceRef());
            if (evidence == null || !evidence.productionSymbol().equals(seed.productionSeed())) fail("unbound seed");
            sameRevision(revision, seed.productionSeed());
        }
        Map<String, RelationshipTrace> traceByRef = uniqueMap(traces, RelationshipTrace::traceId, "relationship trace");
        for (var trace : traces) {
            if (!revision.equals(trace.sourceRevision()) || !graphDigest.equals(trace.graphSha256()))
                fail("relationship trace binding mismatch");
        }
        Set<String> usedTraces = new HashSet<>();
        for (int index = 0; index < chain.size(); index++) {
            var step = chain.get(index);
            if (step.order() != index + 1) fail("realization chain order must be contiguous from one");
            sameRevision(revision, step.component());
            var seed = seedByRef.get(step.seedRef()); if (seed == null) fail("unbound chain seed");
            if (step.relationshipBasis() == RelationshipBasis.DIRECT_TEST_REFERENCE) {
                if (step.relationshipTraceRef() != null) fail("direct relationship cannot cite a Graphify trace");
                boolean matched = step.evidenceRefs().stream().map(evidenceByRef::get).filter(Objects::nonNull)
                        .anyMatch(e -> e.productionSymbol().equals(step.component()));
                if (!matched) fail("direct relationship requires matching direct-production-symbol evidence");
            } else {
                if (!step.evidenceRefs().isEmpty()) fail("Graphify inferred relationship uses typed trace, not direct refs");
                var trace = traceByRef.get(step.relationshipTraceRef()); if (trace == null) fail("unbound relationship trace");
                if (!trace.startsAt(seed.productionSeed()) || !trace.endsAt(step.component()))
                    fail("relationship trace endpoints do not bind seed and inferred component");
                usedTraces.add(trace.traceId());
            }
        }
        if (!usedTraces.equals(traceByRef.keySet())) fail("unused relationship trace");
        if (chain.get(0).relationshipBasis() != RelationshipBasis.DIRECT_TEST_REFERENCE)
            fail("realization chain must start from a directly evidenced production seed");
    }

    /** Provider node IDs are intentionally absent from this durable identity. */
    public record ComponentIdentity(String sourceRevision, String sourcePath, Granularity granularity, String qualifiedSymbol) {
        public ComponentIdentity {
            revision(sourceRevision); safe(sourcePath, "sourcePath");
            if (granularity == null) fail("granularity is required"); safe(qualifiedSymbol, "qualifiedSymbol");
            if (!canonicalProductionPath(sourcePath)) fail("sourcePath must be canonical, repository-relative, and production-only");
        }
    }

    public record DirectProductionSymbolEvidence(String evidenceRef, String observationRef, ComponentIdentity productionSymbol) {
        public DirectProductionSymbolEvidence {
            safe(evidenceRef, "evidenceRef"); safe(observationRef, "observationRef");
            if (productionSymbol == null) fail("productionSymbol is required");
        }
    }
    public record SeedProvenance(String seedRef, String directEvidenceRef, ComponentIdentity productionSeed) {
        public SeedProvenance {
            safe(seedRef, "seedRef"); safe(directEvidenceRef, "directEvidenceRef");
            if (productionSeed == null) fail("productionSeed is required");
        }
    }
    public record RelationshipEdge(int order, ComponentIdentity from, ComponentIdentity to,
                                   String relationshipType, String evidenceRef) {
        public RelationshipEdge {
            if (order < 1) fail("edge order must be positive");
            if (from == null || to == null) fail("edge endpoints are required");
            safe(relationshipType, "relationshipType"); safe(evidenceRef, "evidenceRef");
        }
    }
    public record RelationshipTrace(String traceId, String sourceRevision, String graphSha256,
                                    List<RelationshipEdge> edges) {
        public RelationshipTrace {
            safe(traceId, "traceId"); revision(sourceRevision); digest(graphSha256, "graphSha256");
            edges = snapshot(edges, "edges"); if (edges.isEmpty()) fail("relationship trace requires edges");
            for (int i = 0; i < edges.size(); i++) {
                var edge = edges.get(i); if (edge.order() != i + 1) fail("edge order must be contiguous from one");
                sameRevision(sourceRevision, edge.from()); sameRevision(sourceRevision, edge.to());
                if (i > 0 && !edges.get(i - 1).to().equals(edge.from())) fail("relationship trace must be contiguous");
            }
        }
        boolean startsAt(ComponentIdentity identity) { return edges.get(0).from().equals(identity); }
        boolean endsAt(ComponentIdentity identity) { return edges.get(edges.size() - 1).to().equals(identity); }
    }
    public record RealizationChainStep(int order, ComponentIdentity component, RelationshipBasis relationshipBasis,
                                       String seedRef, List<String> evidenceRefs, String relationshipTraceRef) {
        public RealizationChainStep {
            if (order < 1) fail("order must be positive"); if (component == null) fail("component is required");
            if (relationshipBasis == null) fail("relationshipBasis is required"); safe(seedRef, "seedRef");
            evidenceRefs = safeStrings(evidenceRefs, "evidenceRefs");
            if (relationshipTraceRef != null) safe(relationshipTraceRef, "relationshipTraceRef");
        }
    }

    private static final Pattern FORBIDDEN = Pattern.compile(
            "(?i)(evaluator(?:[ _-]+gold)?|gold[ _-]+mapping|ground[ _-]+truth|expected[ _-]+mapping)");
    private static void safe(String value, String field) {
        if (value == null || value.isBlank()) fail(field + " is required");
        if (FORBIDDEN.matcher(value).find()) fail(field + " contains evaluator-only vocabulary");
    }
    private static boolean canonicalProductionPath(String path) {
        if (!path.equals(path.strip()) || path.startsWith("/") || path.startsWith("./") || path.endsWith("/")
                || path.contains("\\") || path.contains("//") || path.matches("^[A-Za-z]:.*")) return false;
        String[] parts = path.split("/", -1); for (String part : parts) if (part.isEmpty() || part.equals(".") || part.equals("..")) return false;
        String lower = path.toLowerCase(Locale.ROOT);
        return !lower.matches("(^|.*/)(src/test|tests?|test)(/.*|$)") && !path.matches(".*Tests?\\.java$");
    }
    private static void revision(String value) { if (value == null || !value.matches("[0-9a-f]{40}")) fail("full lowercase source revision required"); }
    private static void digest(String value, String field) { if (value == null || !value.matches("[0-9a-f]{64}")) fail(field + " must be lowercase SHA-256"); }
    private static void sameRevision(String revision, ComponentIdentity identity) { if (!revision.equals(identity.sourceRevision())) fail("component revision mismatch"); }
    private static <T> List<T> snapshot(List<T> values, String field) {
        if (values == null) fail(field + " is required"); var copy = new ArrayList<>(values);
        if (copy.contains(null)) fail(field + " cannot contain null"); return Collections.unmodifiableList(copy);
    }
    private static List<String> safeStrings(List<String> values, String field) {
        var copy = snapshot(values, field); Set<String> seen = new HashSet<>();
        for (String value : copy) { safe(value, field); if (!seen.add(value)) fail(field + " contains duplicates"); }
        return copy;
    }
    private static <T> Map<String,T> uniqueMap(List<T> values, java.util.function.Function<T,String> key, String field) {
        Map<String,T> result = new HashMap<>(); for (T value : values) if (result.put(key.apply(value), value) != null) fail("duplicate " + field + " identity"); return result;
    }
    private static void fail(String message) { throw new RuntimeContractException(message); }
}

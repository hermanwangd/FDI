package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.*;
import com.featuredeliveryintelligence.fdi.product.realization.directtrace.*;
import com.featuredeliveryintelligence.fdi.product.realization.graphexpansion.GraphifyProductionExpansion;
import com.featuredeliveryintelligence.fdi.product.realization.graphexpansion.GraphifyProductionExpansion.*;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Joins explicit scenario evidence assignments without deriving Product meaning from code structure. */
public final class ScenarioGroundedForwardMapper {
    public static final String SNAPSHOT = "pkb001-petclinic-reviewed-semantics-004";
    public static final String STATUS = "FROZEN";
    public static final String AUTHORITY = "REVIEWED_EXPERIMENT_SEMANTICS";

    private ScenarioGroundedForwardMapper() { }

    public static Result compose(Input input) {
        Objects.requireNonNull(input, "input");
        validateBindings(input);
        Map<String, ResolvedDirectObservation> observations = unique(input.directObservations(),
                value -> value.directEvidence().observationRef(), "observation");
        Map<String, UnresolvedDirectReferenceGap> gaps = unique(input.unresolvedGaps(),
                UnresolvedDirectReferenceGap::observationRef, "gap");
        Map<String, RelationshipTrace> traces = input.expansion().traces().stream()
                .flatMap(value -> value.inferredNeighbours().stream())
                .map(InferredNeighbour::relationshipTrace)
                .collect(Collectors.toMap(RelationshipTrace::traceId, Function.identity(), (a, b) -> {
                    throw failure("duplicate relationship trace");
                }, TreeMap::new));
        Map<ComponentIdentity, String> seedRefs = input.expansion().traces().stream()
                .collect(Collectors.toMap(value -> value.seed().identity(), value -> value.seed().seedRef(), (a, b) -> {
                    throw failure("duplicate expansion seed identity");
                }));

        Set<String> scenarios = new HashSet<>();
        List<ScenarioProposal> mappings = new ArrayList<>();
        for (ScenarioAssignment assignment : input.assignments()) {
            if (!scenarios.add(assignment.scenarioId())) throw failure("duplicate scenario assignment");
            ScenarioMappingContractV04 mapping = mapping(input, assignment, observations, gaps, traces, seedRefs);
            List<ComponentRole> roles = mapping.realizationChain().stream().map(RealizationChainStep::component).distinct()
                    .map(identity -> new ComponentRole(identity,
                            mapping.outcome() == Outcome.MAPPING_PROPOSAL && identity.equals(observations.get(assignment.primaryObservationRef()).directEvidence().productionSymbol())
                                    ? "PRIMARY" : "SUPPORTING"))
                    .toList();
            mappings.add(new ScenarioProposal(mapping, roles));
        }
        List<CapabilityProposal> capabilities = mappings.stream()
                .collect(Collectors.groupingBy(value -> value.mapping().capabilityId(), TreeMap::new, Collectors.toList()))
                .entrySet().stream().map(entry -> new CapabilityProposal(entry.getKey(), entry.getValue())).toList();
        return new Result(input.snapshotId(), input.semanticsSha256(), input.authorizationSha256(),
                input.sourceRevision(), input.testEvidenceSha256(), input.graphSha256(), capabilities,
                input.directObservations().stream().map(value -> value.directEvidence().evidenceRef()).sorted().toList(),
                input.unresolvedGaps().stream().map(UnresolvedDirectReferenceGap::observationRef).sorted().toList(), false);
    }

    private static ScenarioMappingContractV04 mapping(Input input, ScenarioAssignment assignment,
            Map<String, ResolvedDirectObservation> observations, Map<String, UnresolvedDirectReferenceGap> gaps,
            Map<String, RelationshipTrace> traces, Map<ComponentIdentity, String> expansionSeedRefs) {
        List<ResolvedDirectObservation> selected = assignment.observationRefs().stream().map(ref -> {
            var value = observations.get(ref); if (value == null) throw failure("unknown observation: " + ref); return value;
        }).sorted(Comparator.comparing(value -> value.directEvidence().evidenceRef())).toList();
        List<String> selectedGaps = assignment.gapRefs().stream().map(ref -> {
            var value = gaps.get(ref); if (value == null) throw failure("unknown evidence gap: " + ref);
            return "direct-gap:" + value.observationRef() + ":" + value.kind();
        }).sorted().toList();
        if (selected.isEmpty()) {
            if (assignment.primaryObservationRef() != null) throw failure("unresolved scenario cannot name a primary observation");
            List<String> unresolved = selectedGaps.isEmpty()
                    ? List.of("No scenario-bound production evidence was supplied") : selectedGaps;
            return new ScenarioMappingContractV04(ScenarioMappingContractV04.SCHEMA_VERSION,
                    ScenarioMappingContractV04.AUTHORITY, assignment.capabilityId(), assignment.scenarioId(),
                    input.sourceRevision(), input.graphSha256(), input.semanticsSha256(), Outcome.UNRESOLVED,
                    EvidenceStatus.INSUFFICIENT, List.of(), List.of(), List.of(), List.of(), unresolved,
                    List.of("Proposal-only reconstruction; unsupported behavior is not inferred"));
        }
        if (!assignment.coreBehaviorEvidenceConfirmed())
            throw failure("direct observation is not confirmed to perform scenario core behavior");
        if (assignment.primaryObservationRef() == null || selected.stream().noneMatch(value ->
                value.directEvidence().observationRef().equals(assignment.primaryObservationRef())))
            throw failure("mapping proposal requires a scenario-bound PRIMARY observation");
        List<DirectProductionSymbolEvidence> direct = selected.stream().map(ResolvedDirectObservation::directEvidence).toList();
        List<SeedProvenance> seeds = selected.stream().map(ResolvedDirectObservation::seed).toList();
        Map<ComponentIdentity, SeedProvenance> assignedSeeds = seeds.stream().collect(Collectors.toMap(
                SeedProvenance::productionSeed, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
        List<RelationshipTrace> selectedTraces = assignment.relationshipTraceRefs().stream().map(ref -> {
            var trace = traces.get(ref); if (trace == null) throw failure("unknown relationship trace: " + ref); return trace;
        }).sorted(Comparator.comparing(RelationshipTrace::traceId)).toList();
        for (RelationshipTrace trace : selectedTraces) {
            ComponentIdentity start = trace.edges().get(0).from();
            if (!assignedSeeds.containsKey(start) || !Objects.equals(expansionSeedRefs.get(start), assignedSeeds.get(start).seedRef()))
                throw failure("relationship trace is not bound to a scenario direct seed");
        }
        List<RealizationChainStep> chain = new ArrayList<>();
        for (ResolvedDirectObservation value : selected) chain.add(new RealizationChainStep(chain.size() + 1,
                value.seed().productionSeed(), RelationshipBasis.DIRECT_TEST_REFERENCE, value.seed().seedRef(),
                List.of(value.directEvidence().evidenceRef()), null));
        for (RelationshipTrace trace : selectedTraces) {
            ComponentIdentity start = trace.edges().get(0).from();
            chain.add(new RealizationChainStep(chain.size() + 1, trace.edges().get(trace.edges().size() - 1).to(),
                    RelationshipBasis.GRAPHIFY_INFERRED, assignedSeeds.get(start).seedRef(), List.of(), trace.traceId()));
        }
        List<String> evidenceGaps = new ArrayList<>(selectedGaps);
        if (selectedTraces.isEmpty()) evidenceGaps.add("No bounded Graphify relationship trace was assigned to this scenario");
        EvidenceStatus evidenceStatus = evidenceGaps.isEmpty() ? EvidenceStatus.COMPLETE : EvidenceStatus.PARTIAL;
        return new ScenarioMappingContractV04(ScenarioMappingContractV04.SCHEMA_VERSION,
                ScenarioMappingContractV04.AUTHORITY, assignment.capabilityId(), assignment.scenarioId(),
                input.sourceRevision(), input.graphSha256(), input.semanticsSha256(), Outcome.MAPPING_PROPOSAL,
                evidenceStatus, direct, seeds, selectedTraces, chain, evidenceGaps,
                List.of("Proposal-only reconstruction from explicit scenario evidence assignments"));
    }

    private static void validateBindings(Input input) {
        if (!SNAPSHOT.equals(input.snapshotId()) || !STATUS.equals(input.snapshotStatus())
                || !AUTHORITY.equals(input.snapshotAuthority())) throw failure("frozen semantics snapshot binding mismatch");
        if (!input.sourceRevision().matches("[0-9a-f]{40}")) throw failure("full source revision required");
        for (String digest : List.of(input.semanticsSha256(), input.authorizationSha256(), input.testEvidenceSha256(), input.graphSha256()))
            if (!digest.matches("[0-9a-f]{64}")) throw failure("exact lowercase digest required");
        if (!input.sourceRevision().equals(input.expansion().sourceRevision())
                || !input.graphSha256().equals(input.expansion().graphSha256())) throw failure("Graphify expansion binding mismatch");
    }

    private static <T> Map<String,T> unique(List<T> values, Function<T,String> key, String label) {
        Map<String,T> result = new HashMap<>();
        for (T value : values) if (result.put(key.apply(value), value) != null) throw failure("duplicate " + label);
        return result;
    }

    public record Input(String snapshotId, String snapshotStatus, String snapshotAuthority,
            String sourceRevision, String semanticsSha256, String authorizationSha256, String testEvidenceSha256,
            String graphSha256, List<ResolvedDirectObservation> directObservations,
            List<UnresolvedDirectReferenceGap> unresolvedGaps, GraphifyProductionExpansion.Result expansion,
            List<ScenarioAssignment> assignments) {
        public Input {
            directObservations = List.copyOf(directObservations); unresolvedGaps = List.copyOf(unresolvedGaps);
            assignments = List.copyOf(assignments); Objects.requireNonNull(expansion, "expansion");
        }
    }
    public record ScenarioAssignment(String capabilityId, String scenarioId, boolean coreBehaviorEvidenceConfirmed,
            String primaryObservationRef, List<String> observationRefs,
            List<String> relationshipTraceRefs, List<String> gapRefs) {
        public ScenarioAssignment {
            required(capabilityId); required(scenarioId); observationRefs = uniqueStrings(observationRefs);
            relationshipTraceRefs = uniqueStrings(relationshipTraceRefs); gapRefs = uniqueStrings(gapRefs);
        }
    }
    public record ComponentRole(ComponentIdentity component, String role) {
        public ComponentRole { Objects.requireNonNull(component, "component"); if (!Set.of("PRIMARY", "SUPPORTING").contains(role)) throw failure("invalid role"); }
    }
    public record ScenarioProposal(ScenarioMappingContractV04 mapping, List<ComponentRole> componentRoles) {
        public ScenarioProposal {
            Objects.requireNonNull(mapping, "mapping"); componentRoles = List.copyOf(componentRoles);
            long primary = componentRoles.stream().filter(value -> value.role().equals("PRIMARY")).count();
            if ((mapping.outcome() == Outcome.MAPPING_PROPOSAL && primary != 1)
                    || (mapping.outcome() == Outcome.UNRESOLVED && !componentRoles.isEmpty())) throw failure("outcome and PRIMARY role mismatch");
        }
    }
    public record CapabilityProposal(String capabilityId, List<ScenarioProposal> scenarios) {
        public CapabilityProposal { required(capabilityId); scenarios = List.copyOf(scenarios); }
        public String authority() { return ScenarioMappingContractV04.AUTHORITY; }
    }
    public record Result(String snapshotId, String semanticsSha256, String authorizationSha256,
            String sourceRevision, String testEvidenceSha256, String graphSha256,
            List<CapabilityProposal> capabilities, List<String> observedDirectEvidenceRefs,
            List<String> unresolvedDirectReferenceRefs,
            boolean semanticPublicationAllowed) {
        public Result { capabilities = List.copyOf(capabilities); observedDirectEvidenceRefs = List.copyOf(observedDirectEvidenceRefs);
            unresolvedDirectReferenceRefs = List.copyOf(unresolvedDirectReferenceRefs);
            if (semanticPublicationAllowed) throw failure("publication must remain false"); }
    }
    private static List<String> uniqueStrings(List<String> values) {
        List<String> copy = List.copyOf(values); if (new HashSet<>(copy).size() != copy.size()) throw failure("duplicate reference"); return copy;
    }
    private static void required(String value) { if (value == null || value.isBlank()) throw failure("required value missing"); }
    private static RuntimeContractException failure(String message) { return new RuntimeContractException(message); }
}

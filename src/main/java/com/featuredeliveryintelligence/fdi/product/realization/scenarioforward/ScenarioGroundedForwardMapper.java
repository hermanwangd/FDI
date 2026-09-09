package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04;
import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.*;
import com.featuredeliveryintelligence.fdi.product.realization.directtrace.*;
import com.featuredeliveryintelligence.fdi.product.realization.graphexpansion.GraphifyProductionExpansion;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Fail-closed composer of evidence-backed, proposal-only scenario realization mappings. */
public final class ScenarioGroundedForwardMapper {
    public static final String SNAPSHOT = "pkb001-petclinic-reviewed-semantics-004";
    public static final String STATUS = "FROZEN";
    public static final String AUTHORITY = "REVIEWED_EXPERIMENT_SEMANTICS";
    private ScenarioGroundedForwardMapper() { }

    public static Result compose(Input input) {
        Objects.requireNonNull(input, "input"); validateBindings(input);
        Map<String, UnresolvedDirectReferenceGap> gaps = unique(input.unresolvedGaps(), UnresolvedDirectReferenceGap::observationRef, "gap");
        Map<String, ResolvedDirectObservation> direct = unique(input.directObservations(),
                value -> value.directEvidence().evidenceRef(), "direct evidence");
        Map<String, GraphifyProductionExpansion.ExpandedSeedTrace> expansions = unique(
                input.expansion().traces(), value -> value.seed().seedRef(), "Graphify seed trace");
        Set<String> seen = new HashSet<>(); List<ScenarioProposal> proposals = new ArrayList<>();
        for (ScenarioAssignment assignment : input.assignments()) {
            if (!seen.add(assignment.scenarioId())) throw fail("duplicate scenario assignment");
            List<ResolvedDirectObservation> selectedDirect = assignment.directEvidenceRefs().stream().map(ref -> {
                var observation = direct.get(ref);
                if (observation == null) throw fail("unknown direct evidence: " + ref);
                return observation;
            }).sorted(Comparator.comparing(value -> value.directEvidence().evidenceRef())).toList();
            List<String> selectedGaps = assignment.gapRefs().stream().map(ref -> {
                var gap = gaps.get(ref); if (gap == null) throw fail("unknown evidence gap: " + ref);
                return "direct-gap:" + ref + ":" + gap.kind();
            }).sorted().toList();
            proposals.add(selectedDirect.isEmpty()
                    ? unresolved(input, assignment, selectedGaps)
                    : proposal(input, assignment, selectedDirect, selectedGaps, expansions));
        }
        List<CapabilityProposal> capabilities = proposals.stream().collect(Collectors.groupingBy(
                value -> value.mapping().capabilityId(), TreeMap::new, Collectors.toList())).entrySet().stream()
                .map(entry -> new CapabilityProposal(entry.getKey(), entry.getValue())).toList();
        return new Result(input.snapshotId(), input.semanticsSha256(), input.authorizationSha256(), input.sourceRevision(),
                input.testEvidenceSha256(), input.graphSha256(), capabilities,
                input.directObservations().stream().map(value -> value.directEvidence().evidenceRef()).sorted().toList(),
                input.unresolvedGaps().stream().map(UnresolvedDirectReferenceGap::observationRef).sorted().toList(), false);
    }

    private static ScenarioProposal unresolved(Input input, ScenarioAssignment assignment,
            List<String> selectedGaps) {
        List<String> gaps = selectedGaps.isEmpty()
                ? List.of("No authority-bound scenario core-behavior confirmation exists in the approved input set")
                : selectedGaps;
        var mapping = new ScenarioMappingContractV04(ScenarioMappingContractV04.SCHEMA_VERSION,
                ScenarioMappingContractV04.AUTHORITY, assignment.capabilityId(), assignment.scenarioId(),
                input.sourceRevision(), input.graphSha256(), input.semanticsSha256(), Outcome.UNRESOLVED,
                EvidenceStatus.INSUFFICIENT, List.of(), List.of(), List.of(), List.of(), gaps,
                List.of("Direct observations are retained as supporting evidence outside realization chains"));
        return new ScenarioProposal(mapping, List.of());
    }

    private static ScenarioProposal proposal(Input input, ScenarioAssignment assignment,
            List<ResolvedDirectObservation> selectedDirect, List<String> selectedGaps,
            Map<String, GraphifyProductionExpansion.ExpandedSeedTrace> expansions) {
        List<DirectProductionSymbolEvidence> evidence = selectedDirect.stream()
                .map(ResolvedDirectObservation::directEvidence).toList();
        List<SeedProvenance> seeds = selectedDirect.stream().map(ResolvedDirectObservation::seed).toList();
        List<RelationshipTrace> traces = new ArrayList<>();
        List<RealizationChainStep> chain = new ArrayList<>();
        List<ComponentRole> roles = new ArrayList<>();
        int order = 1;
        for (ResolvedDirectObservation observation : selectedDirect) {
            var directEvidence = observation.directEvidence();
            var seed = observation.seed();
            chain.add(new RealizationChainStep(order++, directEvidence.productionSymbol(),
                    RelationshipBasis.DIRECT_TEST_REFERENCE, seed.seedRef(),
                    List.of(directEvidence.evidenceRef()), null));
            roles.add(new ComponentRole(directEvidence.productionSymbol(), "PRIMARY"));
            var expansion = expansions.get(seed.seedRef());
            if (expansion == null) continue;
            for (var neighbour : expansion.inferredNeighbours()) {
                traces.add(neighbour.relationshipTrace());
                chain.add(new RealizationChainStep(order++, neighbour.identity(),
                        RelationshipBasis.GRAPHIFY_INFERRED, seed.seedRef(), List.of(),
                        neighbour.relationshipTrace().traceId()));
                roles.add(new ComponentRole(neighbour.identity(), "SUPPORTING"));
            }
        }
        EvidenceStatus status = selectedGaps.isEmpty() ? EvidenceStatus.COMPLETE : EvidenceStatus.PARTIAL;
        var mapping = new ScenarioMappingContractV04(ScenarioMappingContractV04.SCHEMA_VERSION,
                ScenarioMappingContractV04.AUTHORITY, assignment.capabilityId(), assignment.scenarioId(),
                input.sourceRevision(), input.graphSha256(), input.semanticsSha256(), Outcome.MAPPING_PROPOSAL,
                status, evidence, seeds, traces, chain, selectedGaps,
                List.of("Scenario selection creates a proposal only and cannot establish Product truth"));
        return new ScenarioProposal(mapping, roles);
    }
    private static void validateBindings(Input input) {
        if (!SNAPSHOT.equals(input.snapshotId()) || !STATUS.equals(input.snapshotStatus()) || !AUTHORITY.equals(input.snapshotAuthority()))
            throw fail("frozen semantics snapshot binding mismatch");
        if (!input.sourceRevision().matches("[0-9a-f]{40}")) throw fail("full source revision required");
        for (String digest : List.of(input.semanticsSha256(), input.authorizationSha256(), input.testEvidenceSha256(), input.graphSha256()))
            if (!digest.matches("[0-9a-f]{64}")) throw fail("exact lowercase digest required");
        if (!input.sourceRevision().equals(input.expansion().sourceRevision()) || !input.graphSha256().equals(input.expansion().graphSha256()))
            throw fail("Graphify expansion binding mismatch");
    }
    private static <T> Map<String,T> unique(List<T> values, Function<T,String> key, String label) {
        Map<String,T> result = new HashMap<>(); for (T value : values) if (result.put(key.apply(value), value) != null) throw fail("duplicate " + label); return result;
    }
    public record Input(String snapshotId, String snapshotStatus, String snapshotAuthority, String sourceRevision,
            String semanticsSha256, String authorizationSha256, String testEvidenceSha256, String graphSha256,
            List<ResolvedDirectObservation> directObservations, List<UnresolvedDirectReferenceGap> unresolvedGaps,
            GraphifyProductionExpansion.Result expansion, List<ScenarioAssignment> assignments) {
        public Input { directObservations = List.copyOf(directObservations); unresolvedGaps = List.copyOf(unresolvedGaps);
            assignments = List.copyOf(assignments); Objects.requireNonNull(expansion, "expansion"); }
    }
    /** Selects known evidence identities; components and roles cannot be injected by the caller. */
    public record ScenarioAssignment(String capabilityId, String scenarioId,
            List<String> directEvidenceRefs, List<String> gapRefs) {
        public ScenarioAssignment { required(capabilityId); required(scenarioId);
            directEvidenceRefs = uniqueStrings(directEvidenceRefs); gapRefs = uniqueStrings(gapRefs); }
    }
    public record ComponentRole(ComponentIdentity component, String role) { }
    public record ScenarioProposal(ScenarioMappingContractV04 mapping, List<ComponentRole> componentRoles) {
        public ScenarioProposal { Objects.requireNonNull(mapping); componentRoles = List.copyOf(componentRoles);
            if ((mapping.outcome() == Outcome.UNRESOLVED) != componentRoles.isEmpty())
                throw fail("component roles must exist exactly for mapping proposals"); }
    }
    public record CapabilityProposal(String capabilityId, List<ScenarioProposal> scenarios) {
        public CapabilityProposal { required(capabilityId); scenarios = List.copyOf(scenarios); }
        public String authority() { return ScenarioMappingContractV04.AUTHORITY; }
    }
    public record Result(String snapshotId, String semanticsSha256, String authorizationSha256, String sourceRevision,
            String testEvidenceSha256, String graphSha256, List<CapabilityProposal> capabilities,
            List<String> observedDirectEvidenceRefs, List<String> unresolvedDirectReferenceRefs,
            boolean semanticPublicationAllowed) {
        public Result { capabilities = List.copyOf(capabilities); observedDirectEvidenceRefs = List.copyOf(observedDirectEvidenceRefs);
            unresolvedDirectReferenceRefs = List.copyOf(unresolvedDirectReferenceRefs);
            if (semanticPublicationAllowed) throw fail("publication must remain false"); }
    }
    private static List<String> uniqueStrings(List<String> values) { List<String> copy = List.copyOf(values);
        if (new HashSet<>(copy).size() != copy.size()) throw fail("duplicate reference"); return copy; }
    private static void required(String value) { if (value == null || value.isBlank()) throw fail("required value missing"); }
    private static RuntimeContractException fail(String message) { return new RuntimeContractException(message); }
}

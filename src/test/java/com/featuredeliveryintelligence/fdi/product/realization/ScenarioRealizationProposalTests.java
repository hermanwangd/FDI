package com.featuredeliveryintelligence.fdi.product.realization;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static com.featuredeliveryintelligence.fdi.product.realization.ScenarioRealizationProposal.*;
import static org.junit.jupiter.api.Assertions.*;

class ScenarioRealizationProposalTests {
    static final String REV = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    static final String CAP = "HYP-CAPABILITY-001";
    static final List<BoundScenario> BOUND = List.of(new BoundScenario("HYP-SCENARIO-001", CAP), new BoundScenario("HYP-SCENARIO-002", CAP));
    static StructuralComponentIdentity identity(String symbol, StructuralComponentIdentity.Granularity kind) {
        return new StructuralComponentIdentity(REV, "src/Owner.java", kind, symbol, symbol);
    }
    static ComponentReference component(String ref) {
        return new ComponentReference(ref, new RealizationComponent(RealizationComponent.Role.PRIMARY,
                identity("example.Owner." + ref, StructuralComponentIdentity.Granularity.METHOD), "Performs core behavior"), List.of(), null);
    }
    static ChainStep evidenced(String ref) {
        return new ChainStep("Find matching owner", StepState.EVIDENCED, List.of(ref), List.of("evidence-1"), null, null);
    }
    static ScenarioTrace trace(String id, ChainStep... steps) { return new ScenarioTrace(id, CAP, List.of(steps)); }
    static List<ScenarioTrace> traces() { return List.of(trace("HYP-SCENARIO-001", evidenced("c1")), trace("HYP-SCENARIO-002", evidenced("c1"))); }
    static ScenarioRealizationProposal proposal(List<ComponentReference> components, List<ScenarioTrace> traces) {
        return new ScenarioRealizationProposal(CAP, RealizationProposal.Outcome.MAPPING_PROPOSAL, EvidenceStatus.PARTIAL,
                REV, BOUND, components, traces, List.of("Contract does not resolve evidence"));
    }
    @Test void preservesVariableLengthOrderAndBothScenarioIds() {
        var gap = new ChainStep("Render result", StepState.EVIDENCE_GAP, List.of(), List.of(), "UI unavailable", null);
        var na = new ChainStep("Persist changes", StepState.NOT_APPLICABLE, List.of(), List.of(), null, "Read only behavior");
        var p = proposal(List.of(component("c1")), List.of(trace("HYP-SCENARIO-001", evidenced("c1")), trace("HYP-SCENARIO-002", evidenced("c1"), gap, na)));
        assertEquals(List.of(evidenced("c1"), gap, na), p.scenarioTraces().get(1).steps());
        assertEquals("PROPOSAL_ONLY", p.authority());
        assertEquals("PROPOSAL_ONLY", na.authority());
    }
    @Test void outcomesAndEvidenceStatusAreIndependent() {
        for (var status : EvidenceStatus.values()) {
            assertEquals(status, new ScenarioRealizationProposal(CAP, RealizationProposal.Outcome.MAPPING_PROPOSAL,
                    status, REV, BOUND, List.of(component("c1")), traces(), List.of("bounded")).evidenceStatus());
            var gap = new ChainStep("Observed surrounding behavior", StepState.EVIDENCED, List.of(), List.of("e1"), null, null);
            assertEquals(status, new ScenarioRealizationProposal(CAP, RealizationProposal.Outcome.UNRESOLVED,
                    status, REV, BOUND, List.of(), List.of(trace("HYP-SCENARIO-001", gap), trace("HYP-SCENARIO-002", gap)), List.of("bounded")).evidenceStatus());
        }
    }
    @Test void completeEvidenceCannotContainAnExplicitGap() {
        var gap = new ChainStep("Render", StepState.EVIDENCE_GAP, List.of(), List.of(), "UI unavailable", null);
        assertThrows(RuntimeContractException.class, () -> new ScenarioRealizationProposal(CAP,
                RealizationProposal.Outcome.MAPPING_PROPOSAL, EvidenceStatus.COMPLETE, REV, BOUND,
                List.of(component("c1")), List.of(trace("HYP-SCENARIO-001", evidenced("c1")), trace("HYP-SCENARIO-002", gap)), List.of("bounded")));
    }
    @Test void rejectsDuplicateDanglingAndUnusedReferences() {
        assertThrows(RuntimeContractException.class, () -> proposal(List.of(component("c1"), component("c1")), traces()));
        assertThrows(RuntimeContractException.class, () -> proposal(List.of(component("c2")), traces()));
        assertThrows(RuntimeContractException.class, () -> proposal(List.of(component("c1"), component("c2")), traces()));
        assertThrows(RuntimeContractException.class, () -> new ChainStep("Find", StepState.EVIDENCED, List.of("c1", "c1"), List.of("e1"), null, null));
        var alias = new ComponentReference("alias", component("c1").component(), List.of(), null);
        assertThrows(RuntimeContractException.class, () -> proposal(List.of(component("c1"), alias), traces()));
    }
    @Test void rejectsMissingDuplicateUnknownAndCrossParentScenarios() {
        for (var ts : List.of(List.of(traces().get(0)), List.of(traces().get(0), traces().get(0)),
                List.of(trace("UNKNOWN", evidenced("c1")), traces().get(1)),
                List.of(new ScenarioTrace("HYP-SCENARIO-001", "OTHER", List.of(evidenced("c1"))), traces().get(1)))) {
            assertThrows(RuntimeContractException.class, () -> proposal(List.of(component("c1")), ts));
        }
        assertThrows(RuntimeContractException.class, () -> new ScenarioRealizationProposal(CAP, RealizationProposal.Outcome.MAPPING_PROPOSAL,
                EvidenceStatus.PARTIAL, REV, List.of(new BoundScenario("HYP-SCENARIO-001", "OTHER")), List.of(component("c1")), traces(), List.of("bounded")));
    }
    @Test void requiresStateSpecificEvidenceAndReasons() {
        assertThrows(RuntimeContractException.class, () -> new ChainStep("Find", StepState.EVIDENCED, List.of(), List.of(), null, null));
        assertThrows(RuntimeContractException.class, () -> new ChainStep("Find", StepState.EVIDENCE_GAP, List.of(), List.of(), " ", null));
        assertThrows(RuntimeContractException.class, () -> new ChainStep("Find", StepState.NOT_APPLICABLE, List.of(), List.of(), null, " "));
        assertThrows(RuntimeContractException.class, () -> new ScenarioTrace("HYP-SCENARIO-001", CAP, List.of()));
    }
    @Test void requiresPrimaryAndForbidsUnresolvedComponents() {
        var supporting = new ComponentReference("c1", new RealizationComponent(RealizationComponent.Role.SUPPORTING,
                component("c1").component().identity(), "Supplies data"), List.of(), null);
        assertThrows(RuntimeContractException.class, () -> proposal(List.of(supporting), traces()));
        assertThrows(RuntimeContractException.class, () -> new ScenarioRealizationProposal(CAP, RealizationProposal.Outcome.UNRESOLVED,
                EvidenceStatus.INSUFFICIENT, REV, BOUND, List.of(component("c1")), traces(), List.of("bounded")));
    }
    @Test void rejectsRevisionMismatch() {
        assertThrows(RuntimeContractException.class, () -> new ScenarioRealizationProposal(CAP, RealizationProposal.Outcome.MAPPING_PROPOSAL,
                EvidenceStatus.PARTIAL, "0".repeat(40), BOUND, List.of(component("c1")), traces(), List.of("bounded")));
    }
    @Test void containingTypeReplacementNeedsExplicitReasonWithoutUniversalMethodFirstRule() {
        var type = new RealizationComponent(RealizationComponent.Role.PRIMARY,
                identity("example.Owner", StructuralComponentIdentity.Granularity.TYPE), "Owns aggregate behavior");
        var direct = List.of(identity("example.Owner.find", StructuralComponentIdentity.Granularity.METHOD));
        assertThrows(RuntimeContractException.class, () -> new ComponentReference("c1", type, direct, null));
        assertDoesNotThrow(() -> new ComponentReference("c1", type, direct, "Aggregate lifecycle is the realization"));
        assertDoesNotThrow(() -> new ComponentReference("c1", type, List.of(), null));
        assertDoesNotThrow(() -> new ComponentReference("c1", type,
                List.of(identity("example.Other.find", StructuralComponentIdentity.Granularity.METHOD)), null));
    }
    @Test void rejectsNullsAndBlankRequiredValuesAsContractErrors() {
        assertThrows(RuntimeContractException.class, () -> proposal(null, traces()));
        assertThrows(RuntimeContractException.class, () -> proposal(Arrays.asList((ComponentReference) null), traces()));
        assertThrows(RuntimeContractException.class, () -> proposal(List.of(component("c1")), null));
        assertThrows(RuntimeContractException.class, () -> new BoundScenario(" ", CAP));
        assertThrows(RuntimeContractException.class, () -> new ComponentReference(" ", component("c1").component(), List.of(), null));
        assertThrows(RuntimeContractException.class, () -> new ChainStep("Find", null, List.of(), List.of(), null, null));
        assertThrows(RuntimeContractException.class, () -> new ChainStep("Find", StepState.EVIDENCED, List.of(), Arrays.asList((String) null), null, null));
    }
    @Test void snapshotsAllListsAndReturnsImmutableViews() {
        var refs = new ArrayList<>(List.of("c1"));
        var evidence = new ArrayList<>(List.of("e1"));
        var step = new ChainStep("Find", StepState.EVIDENCED, refs, evidence, null, null);
        var steps = new ArrayList<>(List.of(step));
        var trace = new ScenarioTrace("HYP-SCENARIO-001", CAP, steps);
        var components = new ArrayList<>(List.of(component("c1")));
        var bound = new ArrayList<>(BOUND);
        var traces = new ArrayList<>(List.of(trace, traces().get(1)));
        var limits = new ArrayList<>(List.of("bounded"));
        var p = new ScenarioRealizationProposal(CAP, RealizationProposal.Outcome.MAPPING_PROPOSAL,
                EvidenceStatus.PARTIAL, REV, bound, components, traces, limits);
        refs.clear(); evidence.clear(); steps.clear(); components.clear(); bound.clear(); traces.clear(); limits.clear();
        assertEquals(1, p.components().size()); assertEquals(2, p.boundScenarios().size());
        assertEquals(2, p.scenarioTraces().size()); assertEquals(List.of("c1"), step.componentRefs());
        assertEquals(List.of("e1"), step.evidenceRefs()); assertEquals(1, trace.steps().size());
        assertThrows(UnsupportedOperationException.class, () -> p.components().clear());
        assertThrows(UnsupportedOperationException.class, () -> p.boundScenarios().clear());
        assertThrows(UnsupportedOperationException.class, () -> p.scenarioTraces().clear());
        assertThrows(UnsupportedOperationException.class, () -> p.limitations().clear());
        assertThrows(UnsupportedOperationException.class, () -> step.componentRefs().clear());
        assertThrows(UnsupportedOperationException.class, () -> step.evidenceRefs().clear());
        assertThrows(UnsupportedOperationException.class, () -> trace.steps().clear());
    }
    @Test void validatesEveryTopLevelRequiredField() {
        for (String invalid : Arrays.asList(null, "", " ")) {
            assertThrows(RuntimeContractException.class, () -> new ScenarioRealizationProposal(invalid,
                    RealizationProposal.Outcome.MAPPING_PROPOSAL, EvidenceStatus.PARTIAL, REV, BOUND, List.of(component("c1")), traces(), List.of("bounded")));
        }
        for (String invalid : Arrays.asList(null, "short", REV.toUpperCase())) {
            assertThrows(RuntimeContractException.class, () -> new ScenarioRealizationProposal(CAP,
                    RealizationProposal.Outcome.MAPPING_PROPOSAL, EvidenceStatus.PARTIAL, invalid, BOUND, List.of(component("c1")), traces(), List.of("bounded")));
        }
        assertThrows(RuntimeContractException.class, () -> new ScenarioRealizationProposal(CAP,
                null, EvidenceStatus.PARTIAL, REV, BOUND, List.of(component("c1")), traces(), List.of("bounded")));
        assertThrows(RuntimeContractException.class, () -> new ScenarioRealizationProposal(CAP,
                RealizationProposal.Outcome.MAPPING_PROPOSAL, null, REV, BOUND, List.of(component("c1")), traces(), List.of("bounded")));
        for (List<BoundScenario> invalid : Arrays.asList(null, List.<BoundScenario>of(), Arrays.asList((BoundScenario) null), List.of(BOUND.get(0), BOUND.get(0)))) {
            assertThrows(RuntimeContractException.class, () -> new ScenarioRealizationProposal(CAP,
                    RealizationProposal.Outcome.MAPPING_PROPOSAL, EvidenceStatus.PARTIAL, REV, invalid, List.of(component("c1")), traces(), List.of("bounded")));
        }
        for (List<String> invalid : Arrays.asList(null, List.<String>of(), List.of(" "), Arrays.asList((String) null))) {
            assertThrows(RuntimeContractException.class, () -> new ScenarioRealizationProposal(CAP,
                    RealizationProposal.Outcome.MAPPING_PROPOSAL, EvidenceStatus.PARTIAL, REV, BOUND, List.of(component("c1")), traces(), invalid));
        }
    }
    @Test void rejectsConflictingStepMetadataAndMalformedNestedLists() {
        assertThrows(RuntimeContractException.class, () -> new ChainStep("Find", StepState.EVIDENCED, List.of(), List.of("e1"), "gap", null));
        assertThrows(RuntimeContractException.class, () -> new ChainStep("Find", StepState.EVIDENCE_GAP, List.of(), List.of(), "gap", "NA"));
        assertThrows(RuntimeContractException.class, () -> new ChainStep("Find", StepState.EVIDENCED, null, List.of("e1"), null, null));
        assertThrows(RuntimeContractException.class, () -> new ChainStep("Find", StepState.EVIDENCED, List.of(), null, null, null));
        assertThrows(RuntimeContractException.class, () -> new ChainStep("Find", StepState.EVIDENCED, List.of(), List.of("e1", "e1"), null, null));
        assertThrows(RuntimeContractException.class, () -> new ChainStep(" ", StepState.EVIDENCED, List.of(), List.of("e1"), null, null));
        assertThrows(RuntimeContractException.class, () -> new ScenarioTrace("s", CAP, null));
        assertThrows(RuntimeContractException.class, () -> new ScenarioTrace("s", CAP, Arrays.asList((ChainStep) null)));
        assertThrows(RuntimeContractException.class, () -> new ComponentReference("c", null, List.of(), null));
        assertThrows(RuntimeContractException.class, () -> new ComponentReference("c", component("c1").component(), null, null));
    }
    @Test void directMethodMetadataIsTypedRevisionBoundUniqueAndImmutable() {
        var original = component("c1").component();
        var method = original.identity();
        var differentRevision = new StructuralComponentIdentity("0".repeat(40), method.sourcePath(), method.granularity(), method.qualifiedSymbol(), method.providerNodeId());
        assertThrows(RuntimeContractException.class, () -> new ComponentReference("c", original, List.of(differentRevision), null));
        assertThrows(RuntimeContractException.class, () -> new ComponentReference("c", original, List.of(identity("example.Owner", StructuralComponentIdentity.Granularity.TYPE)), null));
        assertThrows(RuntimeContractException.class, () -> new ComponentReference("c", original, List.of(method, method), null));
        assertThrows(RuntimeContractException.class, () -> new ComponentReference("c", original, Arrays.asList((StructuralComponentIdentity) null), null));
        var mutable = new ArrayList<>(List.of(method));
        var reference = new ComponentReference("c", original, mutable, null);
        mutable.clear();
        assertEquals(List.of(method), reference.directlyEvidencedMethods());
        assertThrows(UnsupportedOperationException.class, () -> reference.directlyEvidencedMethods().clear());
    }
    @Test void normalizedDuplicateIdentityIgnoresProviderNodeAliases() {
        var original = component("c1").component();
        var id = original.identity();
        var alias = new ComponentReference("alias", new RealizationComponent(original.role(),
                new StructuralComponentIdentity(id.sourceRevision(), id.sourcePath(), id.granularity(), id.qualifiedSymbol(), "different-provider-node"), original.selectionReason()), List.of(), null);
        assertThrows(RuntimeContractException.class, () -> proposal(List.of(component("c1"), alias),
                List.of(trace("HYP-SCENARIO-001", evidenced("c1"), evidenced("alias")), traces().get(1))));
    }
    @Test void containingFileReplacementAlsoRequiresAnExplicitReason() {
        var file = new RealizationComponent(RealizationComponent.Role.PRIMARY,
                new StructuralComponentIdentity(REV, "src/Owner.java", StructuralComponentIdentity.Granularity.FILE, null, "file-node"), "Defines behavior");
        var methods = List.of(identity("example.Owner.find", StructuralComponentIdentity.Granularity.METHOD));
        assertThrows(RuntimeContractException.class, () -> new ComponentReference("file", file, methods, null));
        assertDoesNotThrow(() -> new ComponentReference("file", file, methods, "The whole file supplies the behavior"));
        assertDoesNotThrow(() -> new ComponentReference("file", file, List.of(), null));
    }
}

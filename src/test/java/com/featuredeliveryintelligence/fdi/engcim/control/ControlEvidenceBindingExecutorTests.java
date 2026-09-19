package com.featuredeliveryintelligence.fdi.engcim.control;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlEvidenceBindingExecutorTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String R1 = "a".repeat(40);
    private static final String R2 = "b".repeat(40);

    @Test
    void satisfiedControlIsPersistedAndAllowsTheGate() {
        ControlEvidenceBinding binding = binding("BIND-1", "GATE-1",
                EngineeringControlCatalog.EXACT_BINDING, "candidate:r1", "EV-1");
        List<ControlEvidenceBindingExecutor.PersistedControlResult> persisted = new ArrayList<>();
        ControlEvidenceBindingExecutor executor = new ControlEvidenceBindingExecutor();

        ControlEvidenceBindingExecutor.GateDecision decision = executor.evaluateGate(
                "GATE-1", List.of(binding), b -> new ControlEvidenceBindingExecutor.ResolvedInputs(
                        object().put("subjectRef", "candidate:r1").put("revision", R1),
                        evidence("candidate:r1", R1, "EV-1")),
                persisted::add);

        assertTrue(decision.proceed());
        assertEquals(1, persisted.size());
        assertEquals(EngineeringControlResult.Outcome.SATISFIED, persisted.get(0).result().outcome());
        assertEquals(persisted.get(0).result().resultRef(), decision.resultRefs().get(0));
    }

    @Test
    void inconclusiveResolutionBlocksTheGateAndStillPersistsTheResult() {
        ControlEvidenceBinding binding = binding("BIND-2", "GATE-2",
                EngineeringControlCatalog.EXACT_BINDING, "candidate:r1", "EV-2");
        List<ControlEvidenceBindingExecutor.PersistedControlResult> persisted = new ArrayList<>();

        ControlEvidenceBindingExecutor.GateDecision decision = new ControlEvidenceBindingExecutor().evaluateGate(
                "GATE-2", List.of(binding), ignored -> null, persisted::add);

        assertFalse(decision.proceed());
        assertEquals(List.of("INCONCLUSIVE_CONTROL_RESULT"), decision.reasonCodes());
        assertEquals(EngineeringControlResult.Outcome.INCONCLUSIVE, persisted.get(0).result().outcome());
    }

    @Test
    void oneRejectedControlBlocksTheWholeGateWithoutIssueStatusProjection() {
        ControlEvidenceBinding accepted = binding("BIND-3A", "GATE-3",
                EngineeringControlCatalog.EXACT_BINDING, "candidate:r1", "EV-3A");
        ControlEvidenceBinding stale = binding("BIND-3B", "GATE-3",
                EngineeringControlCatalog.EXACT_BINDING, "candidate:r2", "EV-3B");
        List<ControlEvidenceBindingExecutor.PersistedControlResult> persisted = new ArrayList<>();

        ControlEvidenceBindingExecutor.GateDecision decision = new ControlEvidenceBindingExecutor().evaluateGate(
                "GATE-3", List.of(accepted, stale), binding -> {
                    if (binding.bindingRef().endsWith("A")) {
                        return new ControlEvidenceBindingExecutor.ResolvedInputs(
                                object().put("subjectRef", "candidate:r1").put("revision", R1),
                                evidence("candidate:r1", R1, "EV-3A"));
                    }
                    return new ControlEvidenceBindingExecutor.ResolvedInputs(
                            object().put("subjectRef", "candidate:r2").put("revision", R2),
                            evidence("candidate:r1", R1, "EV-3B"));
                }, persisted::add);

        assertFalse(decision.proceed());
        assertEquals(2, persisted.size());
        assertEquals(EngineeringControlResult.Outcome.SATISFIED, persisted.get(0).result().outcome());
        assertEquals(EngineeringControlResult.Outcome.UNSATISFIED, persisted.get(1).result().outcome());
        assertTrue(decision.reasonCodes().contains("UNSATISFIED_CONTROL_RESULT"));
    }

    private static ControlEvidenceBinding binding(String ref, String gate, String control,
                                                   String subject, String evidenceRef) {
        return new ControlEvidenceBinding(ref, "scope:" + ref, gate, control, subject, List.of(evidenceRef));
    }

    private static ObjectNode object() {
        return JSON.createObjectNode();
    }

    private static ObjectNode evidence(String subject, String revision, String ref) {
        ObjectNode evidence = object()
                .put("boundSubjectRef", subject)
                .put("boundRevision", revision);
        evidence.putArray("evidenceRefs").add(ref);
        return evidence;
    }
}

package com.featuredeliveryintelligence.fdi.engcim.control;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ControlEvidenceBindingTests {
    @Test
    void bindingContainsOnlyTheContractFieldsAndCopiesEvidenceReferences() {
        ControlEvidenceBinding binding = new ControlEvidenceBinding(
                "BIND-1", "S05:r1", "S05-delivery", EngineeringControlCatalog.EXACT_BINDING,
                "candidate:r1", List.of("EV-1"));

        assertEquals("BIND-1", binding.bindingRef());
        assertEquals("S05:r1", binding.scopeRef());
        assertEquals("S05-delivery", binding.gateRef());
        assertEquals(EngineeringControlCatalog.EXACT_BINDING, binding.controlRef());
        assertEquals("candidate:r1", binding.subjectRef());
        assertEquals(List.of("EV-1"), binding.evidenceRefs());

        assertThrows(UnsupportedOperationException.class,
                () -> binding.evidenceRefs().add("EV-2"));

        assertEquals(List.of("bindingRef", "scopeRef", "gateRef", "controlRef", "subjectRef", "evidenceRefs"),
                Arrays.stream(ControlEvidenceBinding.class.getRecordComponents())
                        .map(RecordComponent::getName)
                        .toList());
    }

    @Test
    void bindingRejectsUnknownControlAndBlankContractValues() {
        assertThrows(IllegalArgumentException.class, () -> new ControlEvidenceBinding(
                "BIND-1", "SCOPE", "GATE", "CTRL-UNKNOWN", "candidate:r1", List.of("EV-1")));
        assertThrows(IllegalArgumentException.class, () -> new ControlEvidenceBinding(
                "", "SCOPE", "GATE", EngineeringControlCatalog.EXACT_BINDING,
                "candidate:r1", List.of("EV-1")));
        assertThrows(IllegalArgumentException.class, () -> new ControlEvidenceBinding(
                "BIND-1", "SCOPE", "GATE", EngineeringControlCatalog.EXACT_BINDING,
                "candidate:r1", List.of()));
    }
}

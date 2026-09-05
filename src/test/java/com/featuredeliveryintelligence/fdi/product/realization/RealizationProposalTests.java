package com.featuredeliveryintelligence.fdi.product.realization;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RealizationProposalTests {

    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String OTHER_REVISION = "0123456789abcdef0123456789abcdef01234567";

    @Test
    void acceptsMappingWithPrimaryAndSupportingComponents() {
        var primary = component(RealizationComponent.Role.PRIMARY, REVISION, "Direct behavior");
        var supporting = component(RealizationComponent.Role.SUPPORTING, REVISION, "Validation support");

        var proposal = new RealizationProposal("PET-CAP-01",
                RealizationProposal.Outcome.MAPPING_PROPOSAL, REVISION,
                List.of(primary, supporting), List.of("bounded evidence"));

        assertEquals(List.of(primary, supporting), proposal.components());
    }

    @Test
    void rejectsMappingWithOnlySupportingComponents() {
        var supporting = component(RealizationComponent.Role.SUPPORTING, REVISION, "Nearby type");
        assertThrows(RuntimeContractException.class, () -> new RealizationProposal(
                "PET-CAP-01", RealizationProposal.Outcome.MAPPING_PROPOSAL, REVISION,
                List.of(supporting), List.of("bounded evidence")));
    }

    @Test
    void acceptsUnresolvedWithNoComponents() {
        var proposal = new RealizationProposal("PET-CAP-10", RealizationProposal.Outcome.UNRESOLVED,
                REVISION, List.of(), List.of("template evidence unavailable"));
        assertEquals(List.of(), proposal.components());
    }

    @Test
    void rejectsUnresolvedWithComponents() {
        var primary = component(RealizationComponent.Role.PRIMARY, REVISION, "Direct behavior");
        assertThrows(RuntimeContractException.class, () -> new RealizationProposal(
                "PET-CAP-10", RealizationProposal.Outcome.UNRESOLVED, REVISION,
                List.of(primary), List.of("evidence unavailable")));
    }

    @Test
    void rejectsMixedRevisionComponents() {
        var primary = component(RealizationComponent.Role.PRIMARY, OTHER_REVISION, "Direct behavior");
        assertThrows(RuntimeContractException.class, () -> new RealizationProposal(
                "PET-CAP-01", RealizationProposal.Outcome.MAPPING_PROPOSAL, REVISION,
                List.of(primary), List.of("bounded evidence")));
    }

    @Test
    void rejectsInvalidSourceRevisions() {
        var primary = component(RealizationComponent.Role.PRIMARY, REVISION, "Direct behavior");
        for (String invalid : List.of("818c413", REVISION.toUpperCase())) {
            assertThrows(RuntimeContractException.class, () -> new RealizationProposal(
                    "PET-CAP-01", RealizationProposal.Outcome.MAPPING_PROPOSAL, invalid,
                    List.of(primary), List.of("bounded evidence")));
        }
    }

    @Test
    void rejectsBlankCapabilityAndNullOutcome() {
        var primary = component(RealizationComponent.Role.PRIMARY, REVISION, "Direct behavior");
        assertThrows(RuntimeContractException.class, () -> new RealizationProposal(
                " ", RealizationProposal.Outcome.MAPPING_PROPOSAL, REVISION,
                List.of(primary), List.of("bounded evidence")));
        assertThrows(RuntimeContractException.class, () -> new RealizationProposal(
                "PET-CAP-01", null, REVISION, List.of(primary), List.of("bounded evidence")));
    }

    @Test
    void rejectsNullListsAndNullElementsAsDomainErrors() {
        var primary = component(RealizationComponent.Role.PRIMARY, REVISION, "Direct behavior");
        assertThrows(RuntimeContractException.class, () -> new RealizationProposal(
                "PET-CAP-01", RealizationProposal.Outcome.MAPPING_PROPOSAL, REVISION,
                null, List.of("bounded evidence")));
        assertThrows(RuntimeContractException.class, () -> new RealizationProposal(
                "PET-CAP-01", RealizationProposal.Outcome.MAPPING_PROPOSAL, REVISION,
                List.of(primary), null));

        var componentsWithNull = new ArrayList<RealizationComponent>();
        componentsWithNull.add(null);
        var limitationsWithNull = new ArrayList<String>();
        limitationsWithNull.add(null);
        assertThrows(RuntimeContractException.class, () -> new RealizationProposal(
                "PET-CAP-01", RealizationProposal.Outcome.MAPPING_PROPOSAL, REVISION,
                componentsWithNull, List.of("bounded evidence")));
        assertThrows(RuntimeContractException.class, () -> new RealizationProposal(
                "PET-CAP-01", RealizationProposal.Outcome.MAPPING_PROPOSAL, REVISION,
                List.of(primary), limitationsWithNull));
    }

    @Test
    void rejectsEmptyAndBlankLimitations() {
        var primary = component(RealizationComponent.Role.PRIMARY, REVISION, "Direct behavior");
        assertThrows(RuntimeContractException.class, () -> new RealizationProposal(
                "PET-CAP-01", RealizationProposal.Outcome.MAPPING_PROPOSAL, REVISION,
                List.of(primary), List.of()));
        assertThrows(RuntimeContractException.class, () -> new RealizationProposal(
                "PET-CAP-01", RealizationProposal.Outcome.MAPPING_PROPOSAL, REVISION,
                List.of(primary), List.of(" ")));
    }

    @Test
    void componentRejectsNullRoleIdentityAndBlankReasonAsDomainErrors() {
        var identity = identity(REVISION);
        assertThrows(RuntimeContractException.class,
                () -> new RealizationComponent(null, identity, "Direct behavior"));
        assertThrows(RuntimeContractException.class,
                () -> new RealizationComponent(RealizationComponent.Role.PRIMARY, null, "Direct behavior"));
        assertThrows(RuntimeContractException.class,
                () -> new RealizationComponent(RealizationComponent.Role.PRIMARY, identity, " "));
    }

    @Test
    void defensivelyCopiesListsAndReturnsUnmodifiableAccessors() {
        var primary = component(RealizationComponent.Role.PRIMARY, REVISION, "Direct behavior");
        var components = new ArrayList<>(List.of(primary));
        var limitations = new ArrayList<>(List.of("bounded evidence"));
        var proposal = new RealizationProposal("PET-CAP-01",
                RealizationProposal.Outcome.MAPPING_PROPOSAL, REVISION, components, limitations);

        components.clear();
        limitations.set(0, "changed");
        assertEquals(List.of(primary), proposal.components());
        assertEquals(List.of("bounded evidence"), proposal.limitations());
        assertThrows(UnsupportedOperationException.class, () -> proposal.components().clear());
        assertThrows(UnsupportedOperationException.class, () -> proposal.limitations().add("new"));
    }

    private static RealizationComponent component(
            RealizationComponent.Role role, String revision, String reason) {
        return new RealizationComponent(role, identity(revision), reason);
    }

    private static StructuralComponentIdentity identity(String revision) {
        return new StructuralComponentIdentity(revision, "src/main/java/example/OwnerController.java",
                StructuralComponentIdentity.Granularity.METHOD,
                "example.OwnerController.processFindForm", "ownercontroller-processfindform");
    }
}

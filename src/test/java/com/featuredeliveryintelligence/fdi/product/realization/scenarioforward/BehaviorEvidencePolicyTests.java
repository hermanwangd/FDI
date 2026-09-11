package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.BehaviorEvidencePolicy.ActionFamily;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.BehaviorEvidencePolicy.BehaviorSignal;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.BehaviorEvidencePolicy.Decision;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.BehaviorEvidencePolicy.EvidenceOffer;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.BehaviorEvidencePolicy.ProofPath;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.BehaviorEvidencePolicy.ScenarioSignals;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Focused tests for the provider-neutral behavior evidence qualification policy
 * (SF-BL-002-ROUTE-EFFECTIVENESS-005 W3_POLICY_MAPPER). All scenario signals and
 * evidence surfaces are synthetic; evaluator truth, gold mappings, and Petclinic
 * component names are never used. The action-family table must stay
 * provider-neutral and an HTTP verb or one overlapping token must never qualify.
 */
class BehaviorEvidencePolicyTests {

    private static ScenarioSignals signals(String scenarioId, ActionFamily family, String entity,
            List<String> conditions, List<String> aliases) {
        return new ScenarioSignals(scenarioId, family, entity, conditions, aliases);
    }

    private static EvidenceOffer offer(ProofPath path, String identity, boolean uniqueBinding,
            List<String> surfaces, boolean negative) {
        return new EvidenceOffer(path, identity, uniqueBinding, surfaces, negative);
    }

    @Test
    void actionFamilyTableRecognizesCommonDeliveryVerbs() {
        assertEquals(Optional.of(ActionFamily.FIND), BehaviorEvidencePolicy.classifyAction("find"));
        assertEquals(Optional.of(ActionFamily.FIND), BehaviorEvidencePolicy.classifyAction("Search"));
        assertEquals(Optional.of(ActionFamily.BROWSE), BehaviorEvidencePolicy.classifyAction("browse"));
        assertEquals(Optional.of(ActionFamily.BROWSE), BehaviorEvidencePolicy.classifyAction("list"));
        assertEquals(Optional.of(ActionFamily.CREATE), BehaviorEvidencePolicy.classifyAction("create"));
        assertEquals(Optional.of(ActionFamily.CREATE), BehaviorEvidencePolicy.classifyAction("add"));
        assertEquals(Optional.of(ActionFamily.CREATE), BehaviorEvidencePolicy.classifyAction("insert"));
        assertEquals(Optional.of(ActionFamily.UPDATE), BehaviorEvidencePolicy.classifyAction("update"));
        assertEquals(Optional.of(ActionFamily.UPDATE), BehaviorEvidencePolicy.classifyAction("edit"));
        assertEquals(Optional.of(ActionFamily.REJECT), BehaviorEvidencePolicy.classifyAction("reject"));
        assertEquals(Optional.of(ActionFamily.REJECT), BehaviorEvidencePolicy.classifyAction("validate"));
        assertEquals(Optional.of(ActionFamily.REJECT), BehaviorEvidencePolicy.classifyAction("error"));
    }

    @Test
    void actionFamilyTableIsProviderNeutral() {
        for (String productName : List.of("owner", "pet", "vet", "visit", "OwnerController",
                "PetRepository", "Vet", "specialty")) {
            assertEquals(Optional.empty(), BehaviorEvidencePolicy.classifyAction(productName),
                    "table must not contain product component names: " + productName);
        }
    }

    @Test
    void httpVerbAloneNeverQualifiesAsActionFamily() {
        for (String verb : List.of("get", "post", "put", "patch", "delete", "head", "options")) {
            assertEquals(Optional.empty(), BehaviorEvidencePolicy.classifyAction(verb),
                    "HTTP verb must not map to an action family: " + verb);
        }
    }

    @Test
    void classifyActionIgnoresBlankAndUnknownTerms() {
        assertEquals(Optional.empty(), BehaviorEvidencePolicy.classifyAction(null));
        assertEquals(Optional.empty(), BehaviorEvidencePolicy.classifyAction(" "));
        assertEquals(Optional.empty(), BehaviorEvidencePolicy.classifyAction("teleport"));
    }

    @Test
    void entityAgreementAcceptsRouteControllerTypeAndAlias() {
        ScenarioSignals findOwner = signals("S1", ActionFamily.FIND, "OWNER", List.of(), List.of("find owner"));
        assertTrue(BehaviorEvidencePolicy.entityAgreement(findOwner, "/owners/find"));
        assertTrue(BehaviorEvidencePolicy.entityAgreement(findOwner, "samples.petclinic.owner.OwnerController#processFindForm"));
        assertTrue(BehaviorEvidencePolicy.entityAgreement(findOwner, "testFindOwnerByLastName"));
        assertFalse(BehaviorEvidencePolicy.entityAgreement(findOwner, "/vets"));
        assertFalse(BehaviorEvidencePolicy.entityAgreement(findOwner, "samples.petclinic.vet.VetController#showVet"));
    }

    @Test
    void entityAgreementDoesNotMatchInsideUnrelatedTokens() {
        ScenarioSignals createPet = signals("S2", ActionFamily.CREATE, "PET", List.of(), List.of("create pet"));
        // "petclinic" is one token and must not be read as the entity "pet".
        assertFalse(BehaviorEvidencePolicy.entityAgreement(createPet,
                "org.springframework.samples.petclinic.owner.OwnerControllerTests"));
        assertTrue(BehaviorEvidencePolicy.entityAgreement(createPet, "/owners/{ownerId}/pets"));
    }

    @Test
    void actionFamilyAgreementReadsTestAndHandlerMethodSurfaces() {
        ScenarioSignals findOwner = signals("S1", ActionFamily.FIND, "OWNER", List.of(), List.of());
        assertTrue(BehaviorEvidencePolicy.actionFamilyAgreement(findOwner, "testFindAllOwners"));
        assertTrue(BehaviorEvidencePolicy.actionFamilyAgreement(findOwner, "processFindForm"));
        assertFalse(BehaviorEvidencePolicy.actionFamilyAgreement(findOwner, "processCreationForm"));
        assertFalse(BehaviorEvidencePolicy.actionFamilyAgreement(findOwner, "POST"));
        assertFalse(BehaviorEvidencePolicy.actionFamilyAgreement(findOwner, "redirect:/owners/{ownerId}"));
    }

    @Test
    void conditionAgreementMatchesConditionTokensInEvidenceText() {
        ScenarioSignals rejectPet = signals("S3", ActionFamily.REJECT, "PET",
                List.of("duplicate-name-guard"), List.of("pet reject"));
        assertTrue(BehaviorEvidencePolicy.conditionAgreement(rejectPet, "assertThrows(DuplicateNameException)"));
        assertTrue(BehaviorEvidencePolicy.conditionAgreement(rejectPet, "duplicateNameGuardApplies"));
        assertFalse(BehaviorEvidencePolicy.conditionAgreement(rejectPet, "assertThat(results).isNotEmpty()"));
    }

    @Test
    void supportedSignalsCountsIndependentSignalsOnly() {
        ScenarioSignals findOwner = signals("S1", ActionFamily.FIND, "OWNER",
                List.of("last-name-criteria"), List.of("find owner"));
        assertEquals(Set.of(BehaviorSignal.ENTITY, BehaviorSignal.ACTION_FAMILY),
                BehaviorEvidencePolicy.supportedSignals(findOwner, List.of("testFindOwner")));
        assertEquals(Set.of(BehaviorSignal.ENTITY),
                BehaviorEvidencePolicy.supportedSignals(findOwner, List.of("owner", "/owners/1")));
        assertEquals(Set.of(BehaviorSignal.ENTITY, BehaviorSignal.CONDITION),
                BehaviorEvidencePolicy.supportedSignals(findOwner, List.of("owner last name")));
    }

    @Test
    void assignmentRequiresEntityPlusActionFamilyOrCondition() {
        ScenarioSignals findOwner = signals("S1", ActionFamily.FIND, "OWNER",
                List.of("last-name-criteria"), List.of("find owner"));
        assertTrue(BehaviorEvidencePolicy.isAssigned(findOwner, List.of("testFindOwner")));
        assertTrue(BehaviorEvidencePolicy.isAssigned(findOwner, List.of("ownerByLastName", "last")));
        assertFalse(BehaviorEvidencePolicy.isAssigned(findOwner, List.of("owner", "/owners")));
        assertFalse(BehaviorEvidencePolicy.isAssigned(findOwner, List.of("testFind")));
    }

    @Test
    void soleAssignmentHoldsWhenNoOtherScenarioClaimsTheMethod() {
        ScenarioSignals findOwner = signals("S1", ActionFamily.FIND, "OWNER",
                List.of("last-name-criteria"), List.of("find owner"));
        ScenarioSignals createOwner = signals("S2", ActionFamily.CREATE, "OWNER",
                List.of(), List.of("create owner"));
        ScenarioSignals rejectOwner = signals("S3", ActionFamily.REJECT, "OWNER",
                List.of("validation-error"), List.of("reject owner"));
        List<String> surfaces = List.of("testFindOwnerByLastName");
        assertTrue(BehaviorEvidencePolicy.isSolelyAssigned(findOwner, surfaces,
                List.of(findOwner, createOwner, rejectOwner)));
        assertTrue(BehaviorEvidencePolicy.isSolelyAssigned(findOwner, surfaces, List.of(findOwner)),
                "a scenario is solely assigned when no other accepted scenario claims the method");
    }

    @Test
    void soleAssignmentFailsWhenAnotherScenarioAlsoClaimsTheMethod() {
        ScenarioSignals findOwner = signals("S1", ActionFamily.FIND, "OWNER",
                List.of(), List.of("find owner"));
        ScenarioSignals findOwnerBroad = signals("S4", ActionFamily.FIND, "OWNER",
                List.of("last-name-criteria"), List.of());
        ScenarioSignals createOwner = signals("S2", ActionFamily.CREATE, "OWNER",
                List.of(), List.of("create owner"));
        List<String> surfaces = List.of("testFindOwnerByLastName");
        assertFalse(BehaviorEvidencePolicy.isSolelyAssigned(findOwner, surfaces,
                List.of(findOwner, findOwnerBroad)));
        assertFalse(BehaviorEvidencePolicy.isSolelyAssigned(findOwnerBroad, surfaces,
                List.of(findOwner, findOwnerBroad)));
        assertFalse(BehaviorEvidencePolicy.isSolelyAssigned(createOwner, surfaces,
                List.of(findOwner, createOwner)),
                "a scenario whose signals do not assign the method is never solely assigned");
    }

    @Test
    void soleAssignmentRejectsUnassignedSignalsAndNullScenarioLists() {
        ScenarioSignals findOwner = signals("S1", ActionFamily.FIND, "OWNER",
                List.of(), List.of("find owner"));
        ScenarioSignals createOwner = signals("S2", ActionFamily.CREATE, "OWNER",
                List.of(), List.of("create owner"));
        assertFalse(BehaviorEvidencePolicy.isSolelyAssigned(findOwner, List.of("/owners"),
                List.of(findOwner, createOwner)));
        assertThrows(RuntimeContractException.class, () -> BehaviorEvidencePolicy.isSolelyAssigned(
                findOwner, List.of("testFindOwner"), null));
        List<ScenarioSignals> withNull = new java.util.ArrayList<>(List.of(findOwner));
        withNull.add(null);
        assertThrows(RuntimeContractException.class, () -> BehaviorEvidencePolicy.isSolelyAssigned(
                findOwner, List.of("testFindOwner"), withNull));
    }

    @Test
    void negativeEvidenceSurfacesAreRecognized() {
        for (String surface : List.of("assertThrows(DuplicateKeyException.class)",
                "status().is4xxClientError", "model().attributeHasErrors(\"pet\")",
                "assertThat(rejected).isTrue()", "guardClauseFailed")) {
            assertTrue(BehaviorEvidencePolicy.isNegativeEvidenceSurface(surface), surface);
        }
        assertFalse(BehaviorEvidencePolicy.isNegativeEvidenceSurface("assertThat(results).hasSize(3)"));
        assertFalse(BehaviorEvidencePolicy.isNegativeEvidenceSurface("redirect:/owners/{ownerId}"));
    }

    @Test
    void exactRouteHandlerQualifiesWithUniqueBindingEntityAndFamily() {
        ScenarioSignals findOwner = signals("S1", ActionFamily.FIND, "OWNER", List.of(), List.of("find owner"));
        Decision decision = BehaviorEvidencePolicy.qualify(findOwner, offer(ProofPath.EXACT_ROUTE_HANDLER,
                "samples.OwnerController#processFindForm", true,
                List.of("src/test/java/samples/OwnerControllerTests.java", "testFindOwner", "/owners/find"),
                false));
        assertTrue(decision.qualified());
        assertEquals("", decision.reason());
    }

    @Test
    void exactRouteHandlerRejectsNonUniqueOrGuessedBinding() {
        ScenarioSignals findOwner = signals("S1", ActionFamily.FIND, "OWNER", List.of(), List.of("find owner"));
        Decision decision = BehaviorEvidencePolicy.qualify(findOwner, offer(ProofPath.EXACT_ROUTE_HANDLER,
                "samples.OwnerController#processFindForm", false,
                List.of("testFindOwner", "/owners/find"), false));
        assertFalse(decision.qualified());
        assertTrue(decision.reason().contains("unique"), decision.reason());
    }

    @Test
    void exactRouteHandlerRejectsMissingEntityAgreement() {
        ScenarioSignals findOwner = signals("S1", ActionFamily.FIND, "OWNER", List.of(), List.of("find owner"));
        Decision decision = BehaviorEvidencePolicy.qualify(findOwner, offer(ProofPath.EXACT_ROUTE_HANDLER,
                "samples.VetController#processFindForm", true,
                List.of("testFindVet", "/vets/find"), false));
        assertFalse(decision.qualified());
        assertTrue(decision.reason().contains("entity"), decision.reason());
    }

    @Test
    void exactRouteHandlerRejectsMissingActionFamilyAgreement() {
        // The -002 failure mode: a create scenario must not take Owner#getPet from the token "owner".
        ScenarioSignals createOwner = signals("S4", ActionFamily.CREATE, "OWNER", List.of(), List.of("create owner"));
        Decision decision = BehaviorEvidencePolicy.qualify(createOwner, offer(ProofPath.EXACT_ROUTE_HANDLER,
                "samples.OwnerController#processFindForm", true,
                List.of("testShowOwner", "/owners/find"), false));
        assertFalse(decision.qualified());
        assertTrue(decision.reason().contains("action-family"), decision.reason());
    }

    @Test
    void rejectScenarioRequiresSameTestNegativeEvidence() {
        ScenarioSignals rejectPet = signals("S3", ActionFamily.REJECT, "PET",
                List.of("duplicate-name-guard"), List.of("pet reject"));
        EvidenceOffer withoutNegative = offer(ProofPath.EXACT_ROUTE_HANDLER,
                "samples.PetController#validateNewPet", true,
                List.of("testRejectDuplicatePet", "/owners/{ownerId}/pets/new"), false);
        Decision abstain = BehaviorEvidencePolicy.qualify(rejectPet, withoutNegative);
        assertFalse(abstain.qualified());
        assertTrue(abstain.reason().contains("negative"), abstain.reason());

        EvidenceOffer withNegative = offer(ProofPath.EXACT_ROUTE_HANDLER,
                "samples.PetController#validateNewPet", true,
                List.of("testRejectDuplicatePet", "/owners/{ownerId}/pets/new",
                        "assertThrows(DuplicateNameException.class)"), true);
        assertTrue(BehaviorEvidencePolicy.qualify(rejectPet, withNegative).qualified());
    }

    @Test
    void directReferenceQualifiesOnlyFromAssignedTestMethod() {
        ScenarioSignals findOwner = signals("S1", ActionFamily.FIND, "OWNER",
                List.of("last-name-criteria"), List.of("find owner"));
        Decision assigned = BehaviorEvidencePolicy.qualify(findOwner, offer(ProofPath.DIRECT_PRODUCTION_REFERENCE,
                "samples.OwnerRepository#findByLastName", false,
                List.of("src/test/java/samples/OwnerControllerTests.java", "testFindOwner",
                        "owners.findByLastName(query)"), false));
        assertTrue(assigned.qualified());

        Decision singleTokenOnly = BehaviorEvidencePolicy.qualify(findOwner, offer(ProofPath.DIRECT_PRODUCTION_REFERENCE,
                "samples.OwnerRepository#findByLastName", false,
                List.of("src/test/java/samples/OwnerControllerTests.java", "owners", "data.sql loads owners"), false));
        assertFalse(singleTokenOnly.qualified());
        assertTrue(singleTokenOnly.reason().contains("assigned"), singleTokenOnly.reason());
    }

    @Test
    void directReferenceRequiresEntityAgreementOnReferencedProductionType() {
        ScenarioSignals findOwner = signals("S1", ActionFamily.FIND, "OWNER", List.of(), List.of("find owner"));
        Decision decision = BehaviorEvidencePolicy.qualify(findOwner, offer(ProofPath.DIRECT_PRODUCTION_REFERENCE,
                "samples.VetRepository#findAll", false,
                List.of("src/test/java/samples/VetControllerTests.java", "testFindVets",
                        "vets.findAll()"), false));
        assertFalse(decision.qualified());
        assertTrue(decision.reason().contains("entity"), decision.reason());
    }

    @Test
    void graphTraceSupportNeverQualifies() {
        ScenarioSignals findOwner = signals("S1", ActionFamily.FIND, "OWNER", List.of(), List.of("find owner"));
        Decision decision = BehaviorEvidencePolicy.qualify(findOwner, offer(ProofPath.GRAPH_TRACE_SUPPORT,
                "samples.OwnerRepository#findByLastName", false,
                List.of("testFindOwner", "/owners/find"), true));
        assertFalse(decision.qualified());
        assertTrue(decision.reason().contains("diagnostic"), decision.reason());
    }

    @Test
    void contractsValidateTheirInputs() {
        ScenarioSignals valid = signals("S1", ActionFamily.FIND, "OWNER", List.of(), List.of());
        assertThrows(RuntimeContractException.class, () -> signals(" ", ActionFamily.FIND, "OWNER", List.of(), List.of()));
        assertThrows(RuntimeContractException.class, () -> signals("S1", null, "OWNER", List.of(), List.of()));
        assertThrows(RuntimeContractException.class, () -> signals("S1", ActionFamily.FIND, " ", List.of(), List.of()));
        assertThrows(RuntimeContractException.class, () -> new ScenarioSignals("S1", ActionFamily.FIND, "OWNER",
                List.of(" "), List.of()));
        assertThrows(RuntimeContractException.class, () -> offer(ProofPath.EXACT_ROUTE_HANDLER, "no-method-qualifier",
                true, List.of("surface"), false));
        assertThrows(RuntimeContractException.class, () -> offer(ProofPath.EXACT_ROUTE_HANDLER,
                "Type#method", true, List.of(" "), false));
        assertThrows(RuntimeContractException.class, () -> new Decision(false, " "));
        assertThrows(RuntimeContractException.class, () -> new Decision(true, "qualified reasons are only for rejections"));
        assertThrows(RuntimeContractException.class,
                () -> BehaviorEvidencePolicy.supportedSignals(valid, null));
        assertThrows(RuntimeContractException.class,
                () -> BehaviorEvidencePolicy.supportedSignals(valid, Arrays.asList("ok", null)));
    }
}

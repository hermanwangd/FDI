package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorExtractionResult;
import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorObservation;
import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorObservation.HttpMethod;
import com.featuredeliveryintelligence.fdi.product.realization.route.ScenarioComponentProposal;
import com.featuredeliveryintelligence.fdi.product.realization.route.ScenarioComponentProposal.Component;
import com.featuredeliveryintelligence.fdi.product.realization.route.ScenarioComponentProposal.EvidenceStrength;
import com.featuredeliveryintelligence.fdi.product.realization.route.ScenarioComponentProposal.Outcome;
import com.featuredeliveryintelligence.fdi.product.realization.route.SpringRouteHandlerIndex;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.RouteAwareScenarioMapper.DirectProductionReference;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.RouteAwareScenarioMapper.GraphRelationshipTrace;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.RouteAwareScenarioMapper.MappingInput;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.RouteAwareScenarioMapper.MappingResult;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.RouteAwareScenarioMapper.ScenarioIntent;
import com.featuredeliveryintelligence.fdi.product.realization.scenarioforward.RouteAwareScenarioMapper.TestMethodBehavior;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Focused tests for the route-aware scenario mapper (SF-BL-002-ROUTE-EFFECTIVENESS-005
 * W3_POLICY_MAPPER). Every input is synthetic: controllers are written into a
 * temporary checkout, observations and references are constructed in memory, and
 * evaluator truth, gold mappings, and Petclinic component names are never used.
 * Abstention must be honest: weak token-only candidates, ambiguous routes, and
 * reject scenarios without same-test negative evidence never produce components.
 */
class RouteAwareScenarioMapperTests {

    @TempDir
    Path tempDir;
    private int checkouts;

    private static final String REVISION = "0123456789abcdef0123456789abcdef01234567";
    private static final String OWNER_CONTROLLER = """
            package samples.petclinic.owner;

            import org.springframework.stereotype.Controller;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestMapping;

            @Controller
            @RequestMapping("/owners")
            public class OwnerController {

                @GetMapping("/find")
                public String processFindForm() {
                    return "owners/list";
                }

                @PostMapping("/new")
                public String processCreationForm() {
                    return "redirect:/owners/{ownerId}";
                }

                @PostMapping("/{ownerId}/pets")
                public String validateNewPet() {
                    return "redirect:/owners/{ownerId}";
                }
            }
            """;
    private static final String AMBIGUOUS_ONE = """
            package samples.petclinic.owner;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class AmbiguousOneController {

                @GetMapping("/owners/ambiguous")
                public String first() {
                    return "first";
                }
            }
            """;
    private static final String AMBIGUOUS_TWO = """
            package samples.petclinic.owner;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class AmbiguousTwoController {

                @GetMapping("/owners/ambiguous")
                public String second() {
                    return "second";
                }
            }
            """;

    private static final String OVERLOADED_CONTROLLER = """
            package samples.petclinic.owner;

            import org.springframework.stereotype.Controller;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RequestMethod;

            @Controller
            @RequestMapping("/owners")
            public class OverloadedPetController {

                @RequestMapping(value = "/{ownerId}/pets/new", method = {RequestMethod.GET, RequestMethod.POST})
                public String processCreationForm() {
                    return "redirect:/owners/{ownerId}";
                }
            }
            """;
    private static final String DUPLICATE_ROUTE_ONE = """
            package samples.petclinic.owner;

            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class DuplicateRouteOneController {

                @PostMapping("/owners/submit")
                public String first() {
                    return "first";
                }
            }
            """;
    private static final String DUPLICATE_ROUTE_TWO = """
            package samples.petclinic.owner;

            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class DuplicateRouteTwoController {

                @PostMapping("/owners/submit")
                public String second() {
                    return "second";
                }
            }
            """;

    private Path checkout() throws Exception {
        Path checkout = tempDir.resolve("checkout-" + (++checkouts));
        Files.createDirectories(checkout);
        return checkout;
    }

    private static Path writeSource(Path checkout, String relative, String source) throws Exception {
        Path file = checkout.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, source, StandardCharsets.UTF_8);
        return file;
    }

    private static void commitAll(Path checkout) throws Exception {
        runGit(checkout, "init", "-q");
        runGit(checkout, "add", "-A");
        runGit(checkout, "-c", "user.name=FDI Test", "-c", "user.email=fdi@example.invalid",
                "commit", "-q", "--allow-empty", "-m", "fixture");
    }

    private static String runGit(Path dir, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(dir.toAbsolutePath().toString());
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("git " + String.join(" ", args) + " failed: " + output);
        }
        return output;
    }

    private SpringRouteHandlerIndex ownerIndex() throws Exception {
        Path checkout = checkout();
        writeSource(checkout, "src/main/java/samples/petclinic/owner/OwnerController.java", OWNER_CONTROLLER);
        commitAll(checkout);
        return SpringRouteHandlerIndex.build(checkout,
                List.of(Path.of("src/main/java/samples/petclinic/owner/OwnerController.java")));
    }

    private SpringRouteHandlerIndex overloadedIndex() throws Exception {
        Path checkout = checkout();
        writeSource(checkout, "src/main/java/samples/petclinic/owner/OverloadedPetController.java",
                OVERLOADED_CONTROLLER);
        commitAll(checkout);
        return SpringRouteHandlerIndex.build(checkout,
                List.of(Path.of("src/main/java/samples/petclinic/owner/OverloadedPetController.java")));
    }

    private static HttpBehaviorObservation observation(String ref, String testMethod, HttpMethod method, String route) {
        return new HttpBehaviorObservation(ref,
                "src/test/java/samples/petclinic/owner/OwnerControllerTests.java",
                testMethod, "src/test/java/samples/petclinic/owner/OwnerControllerTests.java:40",
                method, route, HttpBehaviorObservation.ExtractionBasis.MOCK_MVC_REQUEST_BUILDER);
    }

    private static HttpBehaviorExtractionResult extraction(HttpBehaviorObservation... observations) {
        return new HttpBehaviorExtractionResult(REVISION, List.of(observations), List.of());
    }

    private static ScenarioIntent intent(String scenarioId, String action, String entity,
            List<String> conditions, List<String> aliases) {
        return new ScenarioIntent(scenarioId, "CAP-1", action, entity, conditions, aliases);
    }

    private static MappingInput input(List<ScenarioIntent> intents, HttpBehaviorExtractionResult extraction,
            SpringRouteHandlerIndex index) {
        return new MappingInput(REVISION, intents, extraction, index,
                List.of(), List.of(), List.of());
    }

    @Test
    void exactRouteBindingProducesOrderedMappingProposal() throws Exception {
        ScenarioIntent findOwner = intent("HYP-SCENARIO-001", "FIND", "OWNER",
                List.of("last-name-criteria", "paged-results"), List.of("find owner"));
        MappingResult result = RouteAwareScenarioMapper.map(input(List.of(findOwner),
                extraction(observation("obs-find", "testFindOwner", HttpMethod.GET, "/owners/find")),
                ownerIndex()));

        assertEquals(RouteAwareScenarioMapper.AUTHORITY, result.authority());
        assertFalse(result.semanticPublicationAllowed());
        assertEquals(1, result.proposals().size());
        ScenarioComponentProposal proposal = result.proposals().get(0);
        assertEquals("HYP-SCENARIO-001", proposal.scenarioId());
        assertEquals(Outcome.MAPPING_PROPOSAL, proposal.outcome());
        assertEquals(1, proposal.components().size());
        Component component = proposal.components().get(0);
        assertEquals(RouteAwareScenarioMapper.ROLE_ROUTE_HANDLER, component.role());
        assertEquals(EvidenceStrength.EXACT_ROUTE_HANDLER, component.evidenceStrength());
        assertEquals("samples.petclinic.owner.OwnerController#processFindForm", component.productionIdentity());
        assertEquals(List.of("obs-find"), component.evidenceRefs());
        assertNull(component.relationshipTrace());
    }

    @Test
    void weakTokenOnlyCandidateAbstainsHonestly() throws Exception {
        // The -002 failure mode: a CREATE OWNER scenario must not capture GET /owners/find
        // merely because the route token "owners" overlaps.
        ScenarioIntent createOwner = intent("HYP-SCENARIO-003", "CREATE", "OWNER",
                List.of("contact-details", "post-operation-view"), List.of("create owner"));
        MappingResult result = RouteAwareScenarioMapper.map(input(List.of(createOwner),
                extraction(observation("obs-find", "testFindOwner", HttpMethod.GET, "/owners/find")),
                ownerIndex()));

        ScenarioComponentProposal proposal = result.proposals().get(0);
        assertEquals(Outcome.UNRESOLVED, proposal.outcome());
        assertEquals(List.of(), proposal.components());
        assertTrue(result.diagnostics().contains(
                "weak-token-only:HYP-SCENARIO-003:obs-find"), result.diagnostics().toString());
    }

    @Test
    void ambiguousRouteStaysAmbiguousAndIsNeverGuessed() throws Exception {
        Path checkout = checkout();
        writeSource(checkout, "src/main/java/samples/petclinic/owner/AmbiguousOneController.java", AMBIGUOUS_ONE);
        writeSource(checkout, "src/main/java/samples/petclinic/owner/AmbiguousTwoController.java", AMBIGUOUS_TWO);
        commitAll(checkout);
        SpringRouteHandlerIndex index = SpringRouteHandlerIndex.build(checkout, List.of(
                Path.of("src/main/java/samples/petclinic/owner/AmbiguousOneController.java"),
                Path.of("src/main/java/samples/petclinic/owner/AmbiguousTwoController.java")));

        ScenarioIntent findOwner = intent("HYP-SCENARIO-001", "FIND", "OWNER", List.of(), List.of("find owner"));
        MappingResult result = RouteAwareScenarioMapper.map(input(List.of(findOwner),
                extraction(observation("obs-ambiguous", "testAmbiguous", HttpMethod.GET, "/owners/ambiguous")),
                index));

        ScenarioComponentProposal proposal = result.proposals().get(0);
        assertEquals(Outcome.UNRESOLVED, proposal.outcome());
        assertEquals(List.of("ambiguous-route:obs-ambiguous"), proposal.gaps());
    }

    @Test
    void unresolvedRouteIsPreservedAsGap() throws Exception {
        ScenarioIntent findOwner = intent("HYP-SCENARIO-001", "FIND", "OWNER", List.of(), List.of("find owner"));
        MappingResult result = RouteAwareScenarioMapper.map(input(List.of(findOwner),
                extraction(observation("obs-missing", "testMissing", HttpMethod.GET, "/owners/missing")),
                ownerIndex()));

        ScenarioComponentProposal proposal = result.proposals().get(0);
        assertEquals(Outcome.UNRESOLVED, proposal.outcome());
        assertEquals(List.of("unresolved-route:obs-missing"), proposal.gaps());
    }

    @Test
    void overloadedHandlerProofResolvesUniquelyByMethodAndRoute() throws Exception {
        // The W4 round-6 failure mode: one identity is reachable through several
        // index entries (here GET and POST on the same route), so a proof must
        // bind the exact HTTP method plus the exact normalized route template to
        // resolve to exactly one handler identity.
        ScenarioIntent createPet = intent("HYP-SCENARIO-001", "CREATE", "PET",
                List.of(), List.of("create pet"));
        MappingResult result = RouteAwareScenarioMapper.map(input(List.of(createPet),
                extraction(observation("obs-create", "testCreatePet", HttpMethod.POST,
                        "/owners/{ownerId}/pets/new")),
                overloadedIndex()));

        ScenarioComponentProposal proposal = result.proposals().get(0);
        assertEquals(Outcome.MAPPING_PROPOSAL, proposal.outcome());
        assertEquals(1, proposal.components().size());
        Component component = proposal.components().get(0);
        assertEquals(RouteAwareScenarioMapper.ROLE_ROUTE_HANDLER, component.role());
        assertEquals(EvidenceStrength.EXACT_ROUTE_HANDLER, component.evidenceStrength());
        assertEquals("samples.petclinic.owner.OverloadedPetController#processCreationForm",
                component.productionIdentity());
        assertEquals(List.of("obs-create"), component.evidenceRefs());
        assertEquals(List.of(), proposal.gaps());
    }

    @Test
    void structurallyResolvingProofWithoutLiteralMatchAbstains() throws Exception {
        // Structural resolution binds a placeholder regardless of its name, so this
        // observation resolves to the handler; the emitted proof cannot re-resolve
        // by exact method plus exact template under the sealed index and must stay
        // UNRESOLVED instead of claiming the identity.
        ScenarioIntent createPet = intent("HYP-SCENARIO-001", "CREATE", "PET",
                List.of(), List.of("create pet"));
        MappingResult result = RouteAwareScenarioMapper.map(input(List.of(createPet),
                extraction(observation("obs-create", "testCreatePet", HttpMethod.POST,
                        "/owners/{variable}/pets/new")),
                overloadedIndex()));

        ScenarioComponentProposal proposal = result.proposals().get(0);
        assertEquals(Outcome.UNRESOLVED, proposal.outcome());
        assertEquals(List.of(), proposal.components());
        assertEquals(List.of("non-unique-route-proof:obs-create"), proposal.gaps());
    }

    @Test
    void routeProofResolvingToMultipleIndexEntriesNeverClaims() throws Exception {
        // Two handlers share one literal method-plus-route; the proof resolves to
        // more than one index entry, is non-unique, and never produces a component.
        Path checkout = checkout();
        writeSource(checkout, "src/main/java/samples/petclinic/owner/DuplicateRouteOneController.java",
                DUPLICATE_ROUTE_ONE);
        writeSource(checkout, "src/main/java/samples/petclinic/owner/DuplicateRouteTwoController.java",
                DUPLICATE_ROUTE_TWO);
        commitAll(checkout);
        SpringRouteHandlerIndex index = SpringRouteHandlerIndex.build(checkout, List.of(
                Path.of("src/main/java/samples/petclinic/owner/DuplicateRouteOneController.java"),
                Path.of("src/main/java/samples/petclinic/owner/DuplicateRouteTwoController.java")));

        ScenarioIntent createOwner = intent("HYP-SCENARIO-001", "CREATE", "OWNER",
                List.of(), List.of("create owner"));
        MappingResult result = RouteAwareScenarioMapper.map(input(List.of(createOwner),
                extraction(observation("obs-duplicate", "testCreateOwner", HttpMethod.POST, "/owners/submit")),
                index));

        ScenarioComponentProposal proposal = result.proposals().get(0);
        assertEquals(Outcome.UNRESOLVED, proposal.outcome());
        assertEquals(List.of(), proposal.components());
        assertEquals(List.of("ambiguous-route:obs-duplicate"), proposal.gaps());
    }

    @Test
    void directReferenceQualifiesFromAssignedTestMethod() throws Exception {
        ScenarioIntent findOwner = intent("HYP-SCENARIO-001", "FIND", "OWNER",
                List.of("last-name-criteria"), List.of("find owner"));
        DirectProductionReference reference = new DirectProductionReference("ref-find",
                "src/test/java/samples/petclinic/owner/OwnerControllerTests.java", "testFindOwner",
                "samples.petclinic.owner.OwnerRepository#findByLastName",
                List.of("owners.findByLastName(query)"));
        // No route observation: only the direct reference can qualify.
        MappingResult result = RouteAwareScenarioMapper.map(new MappingInput(REVISION, List.of(findOwner),
                extraction(), ownerIndex(), List.of(reference), List.of(), List.of()));

        assertEquals(1, result.proposals().size());
        ScenarioComponentProposal proposal = result.proposals().get(0);
        assertEquals(Outcome.MAPPING_PROPOSAL, proposal.outcome());
        Component component = proposal.components().get(0);
        assertEquals(RouteAwareScenarioMapper.ROLE_DIRECT_REFERENCE, component.role());
        assertEquals(EvidenceStrength.DIRECT_PRODUCTION_REFERENCE, component.evidenceStrength());
        assertEquals("samples.petclinic.owner.OwnerRepository#findByLastName", component.productionIdentity());
        assertEquals(List.of("ref-find"), component.evidenceRefs());
    }

    @Test
    void rejectScenarioRequiresSameTestNegativeEvidence() throws Exception {
        ScenarioIntent rejectPet = intent("HYP-SCENARIO-006", "REJECT", "PET",
                List.of("duplicate-name-guard"), List.of("pet reject"));
        HttpBehaviorObservation observation = observation("obs-reject-pet", "testRejectDuplicatePet",
                HttpMethod.POST, "/owners/{ownerId}/pets");

        MappingResult abstain = RouteAwareScenarioMapper.map(input(List.of(rejectPet),
                extraction(observation), ownerIndex()));
        assertEquals(Outcome.UNRESOLVED, abstain.proposals().get(0).outcome());
        assertEquals(List.of(), abstain.proposals().get(0).components());

        TestMethodBehavior negativeEvidence = new TestMethodBehavior(
                "src/test/java/samples/petclinic/owner/OwnerControllerTests.java", "testRejectDuplicatePet",
                List.of("assertThrows(DuplicateNameException.class)"));
        MappingResult qualified = RouteAwareScenarioMapper.map(new MappingInput(REVISION, List.of(rejectPet),
                extraction(observation), ownerIndex(), List.of(), List.of(negativeEvidence), List.of()));
        ScenarioComponentProposal proposal = qualified.proposals().get(0);
        assertEquals(Outcome.MAPPING_PROPOSAL, proposal.outcome());
        assertEquals("samples.petclinic.owner.OwnerController#validateNewPet",
                proposal.components().get(0).productionIdentity());
    }

    @Test
    void graphTraceStaysDiagnosticAndAttachesOnlyToQualifiedSources() throws Exception {
        ScenarioIntent findOwner = intent("HYP-SCENARIO-001", "FIND", "OWNER",
                List.of("last-name-criteria"), List.of("find owner"));
        ScenarioIntent browseVet = intent("HYP-SCENARIO-009", "BROWSE", "VET",
                List.of("paged-results"), List.of("browse vet"));
        GraphRelationshipTrace trace = new GraphRelationshipTrace("trace-1",
                "samples.petclinic.owner.OwnerController#processFindForm",
                "samples.petclinic.owner.OwnerRepository#findByLastName",
                List.of("CALLS"));

        MappingInput input = new MappingInput(REVISION, List.of(findOwner, browseVet),
                extraction(observation("obs-find", "testFindOwner", HttpMethod.GET, "/owners/find")),
                ownerIndex(), List.of(), List.of(), List.of(trace));
        MappingResult result = RouteAwareScenarioMapper.map(input);

        ScenarioComponentProposal ownerProposal = result.proposals().get(0);
        assertEquals(Outcome.MAPPING_PROPOSAL, ownerProposal.outcome());
        assertEquals(2, ownerProposal.components().size());
        Component traceComponent = ownerProposal.components().get(1);
        assertEquals(RouteAwareScenarioMapper.ROLE_GRAPH_TRACE, traceComponent.role());
        assertEquals(EvidenceStrength.GRAPH_TRACE_SUPPORT, traceComponent.evidenceStrength());
        assertEquals("samples.petclinic.owner.OwnerRepository#findByLastName", traceComponent.productionIdentity());
        assertEquals(List.of("trace-1"), traceComponent.evidenceRefs());
        assertTrue(traceComponent.relationshipTrace().contains("trace-1"));

        // The same trace must not create a component for a scenario without qualified proof.
        ScenarioComponentProposal vetProposal = result.proposals().get(1);
        assertEquals(Outcome.UNRESOLVED, vetProposal.outcome());
        assertEquals(List.of(), vetProposal.components());
    }

    @Test
    void graphTraceNeverDuplicatesAnIndependentlyQualifiedComponent() throws Exception {
        ScenarioIntent findOwner = intent("HYP-SCENARIO-001", "FIND", "OWNER",
                List.of("last-name-criteria"), List.of("find owner"));
        DirectProductionReference reference = new DirectProductionReference("ref-find",
                "src/test/java/samples/petclinic/owner/OwnerControllerTests.java", "testFindOwner",
                "samples.petclinic.owner.OwnerRepository#findByLastName",
                List.of("owners.findByLastName(query)"));
        GraphRelationshipTrace trace = new GraphRelationshipTrace("trace-1",
                "samples.petclinic.owner.OwnerController#processFindForm",
                "samples.petclinic.owner.OwnerRepository#findByLastName",
                List.of("CALLS"));
        MappingResult result = RouteAwareScenarioMapper.map(new MappingInput(REVISION, List.of(findOwner),
                extraction(observation("obs-find", "testFindOwner", HttpMethod.GET, "/owners/find")),
                ownerIndex(), List.of(reference), List.of(), List.of(trace)));

        List<Component> components = result.proposals().get(0).components();
        assertEquals(2, components.size());
        assertEquals(EvidenceStrength.EXACT_ROUTE_HANDLER, components.get(0).evidenceStrength());
        assertEquals(EvidenceStrength.DIRECT_PRODUCTION_REFERENCE, components.get(1).evidenceStrength());
        assertTrue(components.stream().noneMatch(c -> c.evidenceStrength() == EvidenceStrength.GRAPH_TRACE_SUPPORT));
    }

    @Test
    void componentsAreOrderedByProofStrengthThenIdentity() throws Exception {
        ScenarioIntent findOwner = intent("HYP-SCENARIO-001", "FIND", "OWNER",
                List.of("last-name-criteria"), List.of("find owner"));
        // Two direct references exercise deterministic identity ordering.
        DirectProductionReference refB = new DirectProductionReference("ref-b",
                "src/test/java/samples/petclinic/owner/OwnerControllerTests.java", "testFindOwner",
                "samples.petclinic.owner.OwnerRepository#findByLastName", List.of("owners.findByLastName(query)"));
        DirectProductionReference refA = new DirectProductionReference("ref-a",
                "src/test/java/samples/petclinic/owner/OwnerControllerTests.java", "testFindOwner",
                "samples.petclinic.owner.OwnerService#findOwner", List.of("owners.findOwner(id)"));

        MappingResult result = RouteAwareScenarioMapper.map(new MappingInput(REVISION, List.of(findOwner),
                extraction(), ownerIndex(), List.of(refB, refA), List.of(), List.of()));

        List<Component> components = result.proposals().get(0).components();
        assertEquals(2, components.size());
        assertEquals("samples.petclinic.owner.OwnerRepository#findByLastName", components.get(0).productionIdentity());
        assertEquals("samples.petclinic.owner.OwnerService#findOwner", components.get(1).productionIdentity());
    }

    @Test
    void inputValidationFailsClosed() throws Exception {
        ScenarioIntent findOwner = intent("HYP-SCENARIO-001", "FIND", "OWNER", List.of(), List.of("find owner"));
        HttpBehaviorExtractionResult extraction = extraction(
                observation("obs-find", "testFindOwner", HttpMethod.GET, "/owners/find"));
        SpringRouteHandlerIndex index = ownerIndex();
        MappingInput valid = input(List.of(findOwner), extraction, index);
        RouteAwareScenarioMapper.map(valid);

        assertThrows(RuntimeContractException.class, () -> new MappingInput("not-a-sha", List.of(findOwner),
                extraction, index, List.of(), List.of(), List.of()));
        // Revision mismatch between binding and observations fails closed.
        HttpBehaviorExtractionResult otherRevision = new HttpBehaviorExtractionResult(
                "89abcdef0123456789abcdef0123456789abcdef", List.of(), List.of());
        assertThrows(RuntimeContractException.class, () -> RouteAwareScenarioMapper.map(
                new MappingInput(REVISION, List.of(findOwner), otherRevision, index,
                        List.of(), List.of(), List.of())));
        // Duplicate scenario and duplicate reference fail closed.
        assertThrows(RuntimeContractException.class, () -> RouteAwareScenarioMapper.map(
                input(List.of(findOwner, findOwner), extraction, index)));
        DirectProductionReference duplicateRef = new DirectProductionReference("ref-dup",
                "src/test/java/samples/petclinic/owner/OwnerControllerTests.java", "testFindOwner",
                "samples.petclinic.owner.OwnerRepository#findByLastName", List.of("owners.findByLastName(query)"));
        assertThrows(RuntimeContractException.class, () -> RouteAwareScenarioMapper.map(new MappingInput(
                REVISION, List.of(findOwner), extraction, index,
                List.of(duplicateRef, duplicateRef), List.of(), List.of())));
        // Semantic publication and non-PROPOSAL_ONLY authority are refused.
        assertThrows(RuntimeContractException.class, () -> new MappingResult("PROPOSAL_ONLY", true,
                List.of(), List.of()));
        assertThrows(RuntimeContractException.class, () -> new MappingResult("PUBLISHED", false,
                List.of(), List.of()));
    }

    @Test
    void unsupportedActionTermIsContainedAndDoesNotAbortSupportedScenarios() throws Exception {
        // The CROSSREPO-REALWORLD-001 failure mode: an unsupported action such as
        // AUTHENTICATE becomes one honest UNRESOLVED proposal with a deterministic
        // gap and diagnostic, and the ordered loop continues so the supported
        // CREATE scenario maps unchanged.
        ScenarioIntent authenticate = intent("HYP-SCENARIO-100", "AUTHENTICATE", "USER",
                List.of("valid-credentials"), List.of("log in"));
        ScenarioIntent createPet = intent("HYP-SCENARIO-001", "CREATE", "PET",
                List.of(), List.of("create pet"));
        MappingResult result = RouteAwareScenarioMapper.map(input(List.of(authenticate, createPet),
                extraction(observation("obs-create", "testCreatePet", HttpMethod.POST,
                        "/owners/{ownerId}/pets/new")),
                overloadedIndex()));

        assertEquals(2, result.proposals().size());
        ScenarioComponentProposal unsupported = result.proposals().get(0);
        assertEquals("HYP-SCENARIO-100", unsupported.scenarioId());
        assertEquals(Outcome.UNRESOLVED, unsupported.outcome());
        assertEquals(List.of(), unsupported.components());
        assertEquals(List.of("unsupported-action-term:AUTHENTICATE"), unsupported.gaps());

        // Exactly one deterministic diagnostic; the unsupported scenario cannot
        // borrow evidence from the supported observation.
        assertEquals(List.of("unsupported-action:HYP-SCENARIO-100:AUTHENTICATE"), result.diagnostics());

        ScenarioComponentProposal supported = result.proposals().get(1);
        assertEquals("HYP-SCENARIO-001", supported.scenarioId());
        assertEquals(Outcome.MAPPING_PROPOSAL, supported.outcome());
        assertEquals(1, supported.components().size());
        Component component = supported.components().get(0);
        assertEquals(RouteAwareScenarioMapper.ROLE_ROUTE_HANDLER, component.role());
        assertEquals(EvidenceStrength.EXACT_ROUTE_HANDLER, component.evidenceStrength());
        assertEquals("samples.petclinic.owner.OverloadedPetController#processCreationForm",
                component.productionIdentity());
        assertEquals(List.of("obs-create"), component.evidenceRefs());
        assertEquals(List.of(), supported.gaps());
    }

    @Test
    void unsupportedActionTermBecomesUnresolvedProposalInsteadOfAborting() throws Exception {
        // Any absent ActionFamily (unrecognized single-token term or multi-token
        // term) no longer aborts the run: it is contained as one honest UNRESOLVED
        // proposal with a deterministic gap and diagnostic.
        ScenarioIntent unknown = intent("HYP-SCENARIO-099", "TELEPORT", "OWNER", List.of(), List.of());
        MappingResult result = RouteAwareScenarioMapper.map(
                input(List.of(unknown), extraction(), ownerIndex()));

        assertEquals(1, result.proposals().size());
        ScenarioComponentProposal proposal = result.proposals().get(0);
        assertEquals("HYP-SCENARIO-099", proposal.scenarioId());
        assertEquals(Outcome.UNRESOLVED, proposal.outcome());
        assertEquals(List.of(), proposal.components());
        assertEquals(List.of("unsupported-action-term:TELEPORT"), proposal.gaps());
        assertEquals(List.of("unsupported-action:HYP-SCENARIO-099:TELEPORT"), result.diagnostics());
    }

    @Test
    void emptyIntentSetProducesEmptyResult() throws Exception {
        MappingResult result = RouteAwareScenarioMapper.map(input(List.of(), extraction(), ownerIndex()));
        assertEquals(List.of(), result.proposals());
        assertEquals(List.of(), result.diagnostics());
        assertEquals(RouteAwareScenarioMapper.AUTHORITY, result.authority());
        assertFalse(result.semanticPublicationAllowed());
    }
}

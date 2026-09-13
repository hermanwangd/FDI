package com.featuredeliveryintelligence.fdi.product.realization.route;

import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorObservation.ExtractionBasis;
import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorObservation.HttpMethod;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteContractTests {

    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String DIGEST = "3c5da364196f1bbec17aabdbf2923c65f2bc0d90b2e9788bac8427219d554a3f";

    // --- HttpBehaviorObservation ---

    @Test
    void observationAcceptsMockMvcBasis() {
        var observation = observation("OBS-1", "/owners/{ownerId}", ExtractionBasis.MOCK_MVC_REQUEST_BUILDER);

        assertEquals(HttpMethod.GET, observation.httpMethod());
        assertEquals("/owners/{ownerId}", observation.normalizedRouteTemplate());
        assertEquals(ExtractionBasis.MOCK_MVC_REQUEST_BUILDER, observation.extractionBasis());
        assertEquals("software-factory.sf-bl002-http-behavior-observation.v0.3",
                HttpBehaviorObservation.SCHEMA_VERSION);
    }

    @Test
    void observationAcceptsRestTemplateBasis() {
        var observation = new HttpBehaviorObservation("OBS-2", "src/test/java/ApiIT.java", "getOwner",
                "ApiIT.java:31", HttpMethod.POST, "/owners", ExtractionBasis.REST_TEMPLATE_CALL);

        assertEquals(ExtractionBasis.REST_TEMPLATE_CALL, observation.extractionBasis());
    }

    @Test
    void observationRejectsBlankIdentityAndBlankTestMethod() {
        assertThrows(RuntimeContractException.class, () -> observation("", "/owners", ExtractionBasis.MOCK_MVC_REQUEST_BUILDER));
        assertThrows(RuntimeContractException.class, () -> observation("OBS-3", "/owners", null));
        assertThrows(RuntimeContractException.class, () -> new HttpBehaviorObservation("OBS-3", "src/T.java", " ",
                "T.java:1", HttpMethod.GET, "/owners", ExtractionBasis.MOCK_MVC_REQUEST_BUILDER));
    }

    @Test
    void observationRejectsUnsafeTestSourcePath() {
        for (String unsafe : List.of("/abs/T.java", "../T.java", "a/./T.java", "a\\T.java", "C:/T.java")) {
            assertThrows(RuntimeContractException.class, () -> new HttpBehaviorObservation("OBS-4", unsafe,
                    "m", "T.java:1", HttpMethod.GET, "/owners", ExtractionBasis.MOCK_MVC_REQUEST_BUILDER));
        }
    }

    @Test
    void observationRejectsInvalidMethodAndRoute() {
        assertThrows(RuntimeContractException.class, () -> new HttpBehaviorObservation("OBS-5", "src/T.java", "m",
                "T.java:1", null, "/owners", ExtractionBasis.MOCK_MVC_REQUEST_BUILDER));
        assertThrows(RuntimeContractException.class, () -> new HttpBehaviorObservation("OBS-5", "src/T.java", "m",
                "T.java:1", HttpMethod.GET, "owners", ExtractionBasis.MOCK_MVC_REQUEST_BUILDER));
        assertThrows(RuntimeContractException.class, () -> new HttpBehaviorObservation("OBS-5", "src/T.java", "m",
                "T.java:1", HttpMethod.GET, "/owners?active=true", ExtractionBasis.MOCK_MVC_REQUEST_BUILDER));
        assertThrows(RuntimeContractException.class, () -> new HttpBehaviorObservation("OBS-5", "src/T.java", "m",
                "T.java:1", HttpMethod.GET, "/owners//pets", ExtractionBasis.MOCK_MVC_REQUEST_BUILDER));
        assertThrows(RuntimeContractException.class, () -> new HttpBehaviorObservation("OBS-5", "src/T.java", "m",
                "T.java:1", HttpMethod.GET, "/owners/{", ExtractionBasis.MOCK_MVC_REQUEST_BUILDER));
        assertThrows(RuntimeContractException.class, () -> new HttpBehaviorObservation("OBS-5", "src/T.java", "m",
                "T.java:1", HttpMethod.GET, "/owners/{}", ExtractionBasis.MOCK_MVC_REQUEST_BUILDER));
    }

    // --- HttpBehaviorExtractionResult ---

    @Test
    void extractionResultPreservesOrderAndFreezesObservations() {
        var first = observation("OBS-10", "/owners", ExtractionBasis.MOCK_MVC_REQUEST_BUILDER);
        var second = observation("OBS-11", "/owners/{ownerId}/pets", ExtractionBasis.REST_TEMPLATE_CALL);

        var result = new HttpBehaviorExtractionResult(REVISION, List.of(first, second),
                List.of("OwnerControllerIT.java: dynamic route expression not statically recoverable"));

        assertEquals(List.of(first, second), result.observations());
        assertThrows(UnsupportedOperationException.class, () -> result.observations().add(first));
        assertEquals(1, result.gaps().size());
        assertEquals("software-factory.sf-bl002-http-behavior-observations.v0.3",
                HttpBehaviorExtractionResult.SCHEMA_VERSION);
    }

    @Test
    void extractionResultRejectsMalformedInput() {
        var observation = observation("OBS-12", "/owners", ExtractionBasis.MOCK_MVC_REQUEST_BUILDER);

        assertThrows(RuntimeContractException.class, () -> new HttpBehaviorExtractionResult(
                "not-a-sha", List.of(observation), List.of()));
        assertThrows(RuntimeContractException.class, () -> new HttpBehaviorExtractionResult(
                REVISION, null, List.of()));
        assertThrows(RuntimeContractException.class, () -> new HttpBehaviorExtractionResult(
                REVISION, List.of(observation, observation), List.of()));
        assertThrows(RuntimeContractException.class, () -> new HttpBehaviorExtractionResult(
                REVISION, java.util.Arrays.asList(observation, null), List.of()));
        assertThrows(RuntimeContractException.class, () -> new HttpBehaviorExtractionResult(
                REVISION, List.of(observation), java.util.Arrays.asList("gap", null)));
    }

    // --- RouteHandler ---

    @Test
    void handlerAcceptsComposedClassAndMethodMapping() {
        var handler = handler("HND-1", List.of(HttpMethod.GET), "/owners/{ownerId}",
                "com.example.OwnerController#showOwner");

        assertEquals(List.of(HttpMethod.GET), handler.httpMethods());
        assertEquals(DIGEST, handler.sourceDigest());
        assertEquals("software-factory.sf-bl002-route-handler.v0.3", RouteHandler.SCHEMA_VERSION);
    }

    @Test
    void handlerNormalizesMethodOrderAndRejectsDuplicates() {
        var handler = handler("HND-2", List.of(HttpMethod.POST, HttpMethod.GET), "/owners",
                "com.example.OwnerController#createOwner");

        assertEquals(List.of(HttpMethod.GET, HttpMethod.POST), handler.httpMethods());
        assertThrows(RuntimeContractException.class, () -> handler("HND-2", List.of(HttpMethod.GET, HttpMethod.GET),
                "/owners", "com.example.OwnerController#createOwner"));
    }

    @Test
    void handlerRejectsBlankIdentityUnsafeRouteInvalidDigestAndEmptyMethods() {
        assertThrows(RuntimeContractException.class, () -> handler(" ", List.of(HttpMethod.GET), "/owners",
                "com.example.OwnerController#m"));
        assertThrows(RuntimeContractException.class, () -> handler("HND-3", List.of(HttpMethod.GET), "owners",
                "com.example.OwnerController#m"));
        assertThrows(RuntimeContractException.class, () -> handler("HND-3", List.of(HttpMethod.GET), "/owners",
                "com.example.OwnerController#m", "not-a-digest"));
        assertThrows(RuntimeContractException.class, () -> handler("HND-3", List.of(), "/owners",
                "com.example.OwnerController#m"));
    }

    // --- RouteResolution ---

    @Test
    void uniqueExactMatchResolves() {
        var handler = handler("HND-10", List.of(HttpMethod.GET), "/owners/{ownerId}",
                "com.example.OwnerController#showOwner");

        var resolution = RouteResolution.resolved(HttpMethod.GET, "/owners/{ownerId}", handler);

        assertEquals(RouteResolution.Status.RESOLVED, resolution.status());
        assertEquals(List.of(handler), resolution.candidates());
    }

    @Test
    void zeroMatchesStayUnresolvedAndMultipleMatchesStayAmbiguous() {
        var unresolved = RouteResolution.unresolved(HttpMethod.GET, "/vets");
        assertEquals(RouteResolution.Status.UNRESOLVED, unresolved.status());
        assertEquals(List.of(), unresolved.candidates());

        var first = handler("HND-20", List.of(HttpMethod.GET), "/owners", "com.example.AController#list");
        var second = handler("HND-21", List.of(HttpMethod.GET), "/owners", "com.example.BController#list");
        var ambiguous = RouteResolution.ambiguous(HttpMethod.GET, "/owners", List.of(second, first));

        assertEquals(RouteResolution.Status.AMBIGUOUS, ambiguous.status());
        assertEquals(List.of(first, second), ambiguous.candidates(), "candidates must be ordered by handlerRef");
    }

    @Test
    void resolutionNeverGuessesAndRejectsInconsistentState() {
        var handler = handler("HND-30", List.of(HttpMethod.GET), "/owners", "com.example.OwnerController#list");

        assertThrows(RuntimeContractException.class, () -> RouteResolution.resolved(HttpMethod.GET, "/owners",
                List.of(handler, handler)));
        assertThrows(RuntimeContractException.class, () -> new RouteResolution(HttpMethod.GET, "/owners",
                RouteResolution.Status.AMBIGUOUS, List.of(handler)));
        assertThrows(RuntimeContractException.class, () -> new RouteResolution(HttpMethod.GET, "/owners",
                RouteResolution.Status.UNRESOLVED, List.of(handler)));
        assertThrows(RuntimeContractException.class, () -> RouteResolution.unresolved(null, "/owners"));
        assertThrows(RuntimeContractException.class, () -> RouteResolution.unresolved(HttpMethod.GET, "owners"));
    }

    @Test
    void candidateListsAreImmutable() {
        var first = handler("HND-40", List.of(HttpMethod.GET), "/owners", "com.example.OwnerController#list");
        var second = handler("HND-41", List.of(HttpMethod.GET), "/owners", "com.example.OtherController#list");
        var mutable = new ArrayList<>(List.of(second, first));
        var resolution = new RouteResolution(HttpMethod.GET, "/owners",
                RouteResolution.Status.AMBIGUOUS, mutable);
        mutable.add(handler("HND-42", List.of(HttpMethod.GET), "/owners", "com.example.ThirdController#list"));

        assertEquals(List.of(first, second), resolution.candidates());
        assertThrows(UnsupportedOperationException.class,
                () -> resolution.candidates().add(resolution.candidates().get(0)));
        assertTrue(resolution.toString().contains("AMBIGUOUS"));
    }

    // --- ScenarioComponentProposal ---

    @Test
    void proposalAcceptsQualifiedMappingWithExactRouteHandler() {
        var component = new ScenarioComponentProposal.Component("entry-point",
                ScenarioComponentProposal.EvidenceStrength.EXACT_ROUTE_HANDLER,
                "com.example.OwnerController#showOwner", List.of("HND-1", "OBS-1"), null);

        var proposal = new ScenarioComponentProposal("SCN-1",
                ScenarioComponentProposal.Outcome.MAPPING_PROPOSAL, List.of(component), List.of());

        assertEquals(ScenarioComponentProposal.Outcome.MAPPING_PROPOSAL, proposal.outcome());
        assertEquals(List.of(component), proposal.components());
        assertEquals("software-factory.sf-bl002-scenario-component-proposal.v0.3",
                ScenarioComponentProposal.SCHEMA_VERSION);
    }

    @Test
    void proposalUnresolvedCarriesGapsAndDiagnosticTracesOnly() {
        var trace = new ScenarioComponentProposal.Component("related-read",
                ScenarioComponentProposal.EvidenceStrength.GRAPH_TRACE_SUPPORT,
                "com.example.OwnerController#listOwners", List.of("EDGE-9"), "EDGE-9");

        var proposal = new ScenarioComponentProposal("SCN-2",
                ScenarioComponentProposal.Outcome.UNRESOLVED, List.of(trace),
                List.of("no direct production reference in the assigned tests"));

        assertEquals(ScenarioComponentProposal.Outcome.UNRESOLVED, proposal.outcome());
        assertEquals("no direct production reference in the assigned tests", proposal.gaps().get(0));
        assertEquals("EDGE-9", proposal.components().get(0).relationshipTrace());
    }

    @Test
    void proposalRejectsBlankIdentityNullOutcomeAndBlankComponentFields() {
        var component = qualifiedComponent("SCN-3");

        assertThrows(RuntimeContractException.class, () -> new ScenarioComponentProposal(" ",
                ScenarioComponentProposal.Outcome.MAPPING_PROPOSAL, List.of(component), List.of()));
        assertThrows(RuntimeContractException.class, () -> new ScenarioComponentProposal("SCN-3",
                null, List.of(component), List.of()));
        assertThrows(RuntimeContractException.class, () -> new ScenarioComponentProposal("SCN-3",
                ScenarioComponentProposal.Outcome.MAPPING_PROPOSAL, null, List.of()));
        assertThrows(RuntimeContractException.class, () -> new ScenarioComponentProposal("SCN-3",
                ScenarioComponentProposal.Outcome.UNRESOLVED, List.of(component), null));
        assertThrows(RuntimeContractException.class, () -> new ScenarioComponentProposal.Component(" ",
                ScenarioComponentProposal.EvidenceStrength.EXACT_ROUTE_HANDLER,
                "com.example.OwnerController#showOwner", List.of("OBS-1"), null));
        assertThrows(RuntimeContractException.class, () -> new ScenarioComponentProposal.Component("entry-point",
                null, "com.example.OwnerController#showOwner", List.of("OBS-1"), null));
        assertThrows(RuntimeContractException.class, () -> new ScenarioComponentProposal.Component("entry-point",
                ScenarioComponentProposal.EvidenceStrength.EXACT_ROUTE_HANDLER,
                "OwnerController", List.of("OBS-1"), null));
        assertThrows(RuntimeContractException.class, () -> new ScenarioComponentProposal.Component("entry-point",
                ScenarioComponentProposal.EvidenceStrength.EXACT_ROUTE_HANDLER,
                "com.example.OwnerController#showOwner", List.of(), null));
        assertThrows(RuntimeContractException.class, () -> new ScenarioComponentProposal.Component("entry-point",
                ScenarioComponentProposal.EvidenceStrength.EXACT_ROUTE_HANDLER,
                "com.example.OwnerController#showOwner", List.of("OBS-1"), " "));
    }

    @Test
    void proposalRejectsMappingProposalWithoutQualifiedComponents() {
        var traceOnly = new ScenarioComponentProposal.Component("related-read",
                ScenarioComponentProposal.EvidenceStrength.GRAPH_TRACE_SUPPORT,
                "com.example.OwnerController#listOwners", List.of("EDGE-9"), null);

        assertThrows(RuntimeContractException.class, () -> new ScenarioComponentProposal("SCN-4",
                ScenarioComponentProposal.Outcome.MAPPING_PROPOSAL, List.of(traceOnly), List.of()));
        assertThrows(RuntimeContractException.class, () -> new ScenarioComponentProposal("SCN-5",
                ScenarioComponentProposal.Outcome.MAPPING_PROPOSAL, List.of(), List.of()));
        assertThrows(RuntimeContractException.class, () -> new ScenarioComponentProposal("SCN-4",
                ScenarioComponentProposal.Outcome.UNRESOLVED,
                List.of(qualifiedComponent("SCN-4")), List.of()));
    }

    @Test
    void proposalRejectsUnorderedDuplicateOrMalformedEvidence() {
        assertThrows(RuntimeContractException.class, () -> new ScenarioComponentProposal.Component("entry-point",
                ScenarioComponentProposal.EvidenceStrength.EXACT_ROUTE_HANDLER,
                "com.example.OwnerController#showOwner", List.of("OBS-1", "OBS-1"), null));
        assertThrows(RuntimeContractException.class, () -> new ScenarioComponentProposal("SCN-6",
                ScenarioComponentProposal.Outcome.MAPPING_PROPOSAL,
                List.of(new ScenarioComponentProposal.Component("entry-point",
                        ScenarioComponentProposal.EvidenceStrength.EXACT_ROUTE_HANDLER,
                        "com.example.OwnerController#showOwner", List.of("HND-1", "HND-1"), null)),
                List.of()));
        assertThrows(RuntimeContractException.class, () -> qualifiedComponent("SCN-6",
                "OBS-2", "OBS-1"));
        assertThrows(RuntimeContractException.class, () -> qualifiedComponent("SCN-6", "OBS-1", " "));
        assertThrows(RuntimeContractException.class, () -> new ScenarioComponentProposal("SCN-6",
                ScenarioComponentProposal.Outcome.UNRESOLVED, List.of(), List.of("gap", " ")));
    }

    @Test
    void proposalListsAreImmutable() {
        var mutableComponents = new ArrayList<>(List.of(qualifiedComponent("SCN-7")));
        var mutableGaps = new ArrayList<>(List.of("diagnostic"));
        var proposal = new ScenarioComponentProposal("SCN-7",
                ScenarioComponentProposal.Outcome.MAPPING_PROPOSAL, mutableComponents, mutableGaps);
        mutableComponents.clear();
        mutableGaps.clear();

        assertEquals(1, proposal.components().size());
        assertEquals(1, proposal.gaps().size());
        assertThrows(UnsupportedOperationException.class, () -> proposal.components().add(proposal.components().get(0)));
        assertThrows(UnsupportedOperationException.class, () -> proposal.gaps().add("another"));
    }

    @Test
    void malformedBraceShapesAreRejectedExplicitlyEverywhere() {
        for (String malformed : List.of("/owners/{id}}", "/owners/}{id}", "/owners/{id}/{}}", "/owners/}")) {
            assertThrows(RuntimeContractException.class,
                    () -> observation("OBS-6", malformed, ExtractionBasis.MOCK_MVC_REQUEST_BUILDER), malformed);
            assertThrows(RuntimeContractException.class,
                    () -> RouteResolution.unresolved(HttpMethod.GET, malformed), malformed);
            assertThrows(RuntimeContractException.class,
                    () -> handler("HND-6", List.of(HttpMethod.GET), malformed, "com.example.OwnerController#m"),
                    malformed);
        }
    }

    @Test
    void canonicalAndNamedPlaceholderFormsAreBothValid() {
        var canonical = observation("OBS-7", "/owners/{variable}", ExtractionBasis.MOCK_MVC_REQUEST_BUILDER);
        var named = observation("OBS-8", "/owners/{ownerId}", ExtractionBasis.MOCK_MVC_REQUEST_BUILDER);

        assertEquals("/owners/{variable}", canonical.normalizedRouteTemplate());
        assertEquals("/owners/{ownerId}", named.normalizedRouteTemplate());
    }

    // --- Route fixtures ---

    @Test
    void fixtureFilesArePresentDeterministicAndPackaged() throws Exception {
        var controllerFirst = fixture("RouteFixtureController.java");
        var controllerSecond = fixture("RouteFixtureController.java");
        var tests = fixture("RouteFixtureTests.java");

        assertEquals(controllerFirst, controllerSecond, "fixture bytes must be deterministic");
        assertTrue(controllerFirst.contains("package scenarioforward.sfbl002.routeaware;"));
        assertTrue(tests.contains("package scenarioforward.sfbl002.routeaware;"));
    }

    @Test
    void fixtureCoversMappingCompositionQueriesAndPathVariables() {
        var controller = fixture("RouteFixtureController.java");
        var tests = fixture("RouteFixtureTests.java");

        assertTrue(controller.contains("@RequestMapping(\"/owners\")"),
                "class-level mapping must compose with method-level mappings");
        assertTrue(controller.contains("@GetMapping"), "composed GET mapping");
        assertTrue(controller.contains("@PostMapping"), "composed POST mapping");
        assertTrue(controller.contains("@PathVariable"), "path variable binding");
        assertTrue(controller.contains("@RequestParam"), "query parameter binding");
        assertTrue(tests.contains(".param("), "query assertions must be observable");
        assertTrue(tests.contains("\"/owners/{ownerId}\""), "normalized path-variable template");
    }

    @Test
    void fixtureCoversDynamicPathsAmbiguityPositiveBehaviorAndRejection() {
        var controller = fixture("RouteFixtureController.java");
        var tests = fixture("RouteFixtureTests.java");

        assertTrue(tests.contains("\"/pets/\" +"), "unsupported dynamic route expression");
        assertTrue(controller.contains("\"/owners/ambiguous\""), "ambiguous duplicate mapping");
        assertTrue(tests.contains("isOk()"), "positive behavior assertion");
        assertTrue(tests.contains("isBadRequest()"), "rejection requires same-test negative evidence");
    }

    private static String fixture(String name) {
        try (var stream = RouteContractTests.class
                .getResourceAsStream("/scenarioforward/sf-bl002/route-aware/" + name)) {
            if (stream == null) {
                throw new IllegalStateException("missing fixture " + name);
            }
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read fixture " + name, e);
        }
    }

    private static ScenarioComponentProposal.Component qualifiedComponent(String scenarioId) {
        return qualifiedComponent(scenarioId, "HND-1", "OBS-1");
    }

    private static ScenarioComponentProposal.Component qualifiedComponent(String scenarioId,
            String... evidenceRefs) {
        return new ScenarioComponentProposal.Component("entry-point",
                ScenarioComponentProposal.EvidenceStrength.EXACT_ROUTE_HANDLER,
                "com.example.OwnerController#showOwner", List.of(evidenceRefs), null);
    }

    private static HttpBehaviorObservation observation(String ref, String route, ExtractionBasis basis) {
        return new HttpBehaviorObservation(ref, "src/test/java/OwnerControllerTests.java", "testFind",
                "OwnerControllerTests.java:42", HttpMethod.GET, route, basis);
    }

    private static RouteHandler handler(String ref, List<HttpMethod> methods, String route, String identity) {
        return handler(ref, methods, route, identity, DIGEST);
    }

    private static RouteHandler handler(String ref, List<HttpMethod> methods, String route, String identity,
            String digest) {
        return new RouteHandler(ref, methods, route, identity, "OwnerController.java:33", digest);
    }
}

package com.featuredeliveryintelligence.fdi.testbehavior.http;

import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorExtractionResult;
import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorObservation;
import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorObservation.ExtractionBasis;
import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorObservation.HttpMethod;
import com.featuredeliveryintelligence.fdi.product.realization.route.RouteResolution;
import com.featuredeliveryintelligence.fdi.product.realization.route.SpringRouteHandlerIndex;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Focused contract and behavior tests for {@link HttpBehaviorObservationExtractor}.
 * Covers MockMvc request builders, RestTemplate calls, normalization, gaps,
 * provenance, deterministic ordering, fail-closed revision resolution, helper
 * classification negatives, and the W2A-to-W2B structural binding pipeline.
 */
class HttpBehaviorObservationExtractorTests {

    private static final Path PETCLINIC_FIXTURES = Path.of(
            "src/test/resources/testbehavior/petclinic-818c4136/fixtures");

    private final HttpBehaviorObservationExtractor extractor = new HttpBehaviorObservationExtractor();

    @TempDir
    Path checkout;

    @TempDir
    Path elsewhere;

    @Test
    void extractsMockMvcGetAndPostWithStaticRoutesAndStripsQuery() throws Exception {
        Path test = writeTestSource("OwnerApiTests.java", """
                package com.example;

                import org.junit.jupiter.api.Test;
                import org.springframework.test.web.servlet.MockMvc;

                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

                class OwnerApiTests {

                    @Autowired
                    MockMvc mockMvc;

                    @Test
                    void listOwners() throws Exception {
                        this.mockMvc.perform(get("/owners?page=1")).andExpect(status().isOk());
                    }

                    @Test
                    void createOwner() throws Exception {
                        this.mockMvc.perform(post("/owners/new"));
                    }
                }
                """);
        initGitRepo();

        HttpBehaviorExtractionResult result = extractor.extract(checkout, List.of(test));

        assertEquals(revisionOf(checkout), result.sourceRevision());
        assertEquals(List.of(), result.gaps());
        assertEquals(2, result.observations().size());

        HttpBehaviorObservation list = result.observations().get(0);
        assertEquals(HttpMethod.GET, list.httpMethod());
        assertEquals("/owners", list.normalizedRouteTemplate());
        assertEquals("listOwners", list.testMethod());
        assertEquals("src/test/java/com/example/OwnerApiTests.java", list.testSourcePath());
        assertEquals(ExtractionBasis.MOCK_MVC_REQUEST_BUILDER, list.extractionBasis());
        assertTrue(list.sourceLocation().startsWith("src/test/java/com/example/OwnerApiTests.java:"),
                "sourceLocation carries provenance, got " + list.sourceLocation());

        HttpBehaviorObservation create = result.observations().get(1);
        assertEquals(HttpMethod.POST, create.httpMethod());
        assertEquals("/owners/new", create.normalizedRouteTemplate());
        assertEquals("createOwner", create.testMethod());
        assertFalse(create.observationRef().isBlank());
        assertFalse(create.observationRef().equals(list.observationRef()));
    }

    @Test
    void preservesNamedPathVariablesAndIgnoresUriVariableArguments() throws Exception {
        Path test = writeTestSource("PetApiTests.java", """
                package com.example;

                import org.junit.jupiter.api.Test;

                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

                class PetApiTests {

                    @Test
                    void showPet() throws Exception {
                        this.mockMvc.perform(get("/owners/{ownerId}/pets/{petId}", TEST_OWNER_ID, 2));
                    }
                }
                """);
        initGitRepo();

        HttpBehaviorExtractionResult result = extractor.extract(checkout, List.of(test));

        assertEquals(1, result.observations().size());
        assertEquals("/owners/{ownerId}/pets/{petId}",
                result.observations().get(0).normalizedRouteTemplate());
        assertEquals(HttpMethod.GET, result.observations().get(0).httpMethod());
    }

    @Test
    void unwrapsChainedBuilderCallsAndQualifiedBuilders() throws Exception {
        Path test = writeTestSource("ChainedApiTests.java", """
                package com.example;

                import org.junit.jupiter.api.Test;
                import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

                class ChainedApiTests {

                    @Test
                    void chainedModifiers() throws Exception {
                        this.mockMvc.perform(get("/owners").param("page", "2").accept("application/json"));
                    }

                    @Test
                    void qualifiedBuilder() throws Exception {
                        this.mockMvc.perform(
                                MockMvcRequestBuilders.post("/owners/{ownerId}/edit", pathOwnerId)
                                    .flashAttr("owner", owner));
                    }
                }
                """);
        initGitRepo();

        HttpBehaviorExtractionResult result = extractor.extract(checkout, List.of(test));

        assertEquals(List.of(), result.gaps());
        assertEquals(2, result.observations().size());
        assertEquals("/owners", result.observations().get(0).normalizedRouteTemplate());
        assertEquals(HttpMethod.GET, result.observations().get(0).httpMethod());
        assertEquals("/owners/{ownerId}/edit", result.observations().get(1).normalizedRouteTemplate());
        assertEquals(HttpMethod.POST, result.observations().get(1).httpMethod());
    }

    @Test
    void extractsRestTemplateRequestEntityCalls() throws Exception {
        Path test = writeTestSource("IntegrationTests.java", """
                package com.example;

                import org.junit.jupiter.api.Test;
                import org.springframework.http.RequestEntity;
                import org.springframework.http.ResponseEntity;
                import org.springframework.web.client.RestTemplate;

                class IntegrationTests {

                    @Test
                    void getOwner() {
                        ResponseEntity<String> result = template.exchange(
                                RequestEntity.get("/owners/1").build(), String.class);
                    }

                    @Test
                    void createPet() {
                        ResponseEntity<String> result = template.postForEntity(
                                "/owners/" + ownerId + "/pets/new", body, String.class);
                    }
                }
                """);
        initGitRepo();

        HttpBehaviorExtractionResult result = extractor.extract(checkout, List.of(test));

        assertEquals(List.of(), result.gaps());
        assertEquals(2, result.observations().size());

        HttpBehaviorObservation get = result.observations().get(0);
        assertEquals(HttpMethod.GET, get.httpMethod());
        assertEquals("/owners/1", get.normalizedRouteTemplate());
        assertEquals(ExtractionBasis.REST_TEMPLATE_CALL, get.extractionBasis());
        assertEquals("getOwner", get.testMethod());

        HttpBehaviorObservation post = result.observations().get(1);
        assertEquals(HttpMethod.POST, post.httpMethod());
        assertEquals("/owners/{variable}/pets/new", post.normalizedRouteTemplate());
    }

    @Test
    void extractsRestTemplateExchangeWithExplicitHttpMethodAndAbsoluteUrl() throws Exception {
        Path test = writeTestSource("CrashIntegrationTests.java", """
                package com.example;

                import org.junit.jupiter.api.Test;
                import org.springframework.http.HttpMethod;
                import org.springframework.http.ResponseEntity;

                class CrashIntegrationTests {

                    @Test
                    void triggerException() {
                        ResponseEntity<String> resp = rest.exchange(
                                "http://localhost:" + port + "/oups", HttpMethod.GET, null, String.class);
                    }
                }
                """);
        initGitRepo();

        HttpBehaviorExtractionResult result = extractor.extract(checkout, List.of(test));

        assertEquals(List.of(), result.gaps());
        assertEquals(1, result.observations().size());
        assertEquals(HttpMethod.GET, result.observations().get(0).httpMethod());
        assertEquals("/oups", result.observations().get(0).normalizedRouteTemplate());
        assertEquals(ExtractionBasis.REST_TEMPLATE_CALL, result.observations().get(0).extractionBasis());
    }

    @Test
    void keepsRootRoute() throws Exception {
        Path test = writeTestSource("WelcomeTests.java", """
                package com.example;

                import org.junit.jupiter.api.Test;

                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

                class WelcomeTests {

                    @Test
                    void welcome() throws Exception {
                        this.mockMvc.perform(get("/"));
                    }
                }
                """);
        initGitRepo();

        HttpBehaviorExtractionResult result = extractor.extract(checkout, List.of(test));

        assertEquals(1, result.observations().size());
        assertEquals("/", result.observations().get(0).normalizedRouteTemplate());
    }

    @Test
    void recoversConcatenatedMockMvcRouteAsTemplate() throws Exception {
        Path test = writeTestSource("DynamicRouteTests.java", """
                package com.example;

                import org.junit.jupiter.api.Test;

                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

                class DynamicRouteTests {

                    @Test
                    void dynamicRoute() throws Exception {
                        int petId = 2;
                        this.mockMvc.perform(get("/pets/" + petId)).andExpect(status().isOk());
                    }
                }
                """);
        initGitRepo();

        HttpBehaviorExtractionResult result = extractor.extract(checkout, List.of(test));

        assertEquals(List.of(), result.gaps());
        assertEquals(1, result.observations().size());
        assertEquals("/pets/{variable}", result.observations().get(0).normalizedRouteTemplate());
    }

    @Test
    void canonicalizesDistinctOperandsToOneCanonicalForm() throws Exception {
        Path test = writeTestSource("CanonicalRouteTests.java", """
                package com.example;

                import org.junit.jupiter.api.Test;

                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

                class CanonicalRouteTests {

                    @Test
                    void byOwnerId() throws Exception {
                        this.mockMvc.perform(get("/owners/" + ownerId));
                    }

                    @Test
                    void byPetId() throws Exception {
                        this.mockMvc.perform(get("/owners/" + petId));
                    }
                }
                """);
        initGitRepo();

        HttpBehaviorExtractionResult result = extractor.extract(checkout, List.of(test));
        HttpBehaviorExtractionResult repeated = extractor.extract(checkout, List.of(test));

        assertEquals(List.of(), result.gaps());
        assertEquals(2, result.observations().size());
        // one canonical form per placeholder: distinct operands normalize identically
        assertEquals("/owners/{variable}", result.observations().get(0).normalizedRouteTemplate());
        assertEquals("/owners/{variable}", result.observations().get(1).normalizedRouteTemplate());
        assertEquals(result, repeated, "canonicalization is deterministic across runs");
    }

    @Test
    void recordsUnsupportedDynamicRoutesAsExplicitGaps() throws Exception {
        Path test = writeTestSource("UnsupportedTests.java", """
                package com.example;

                import org.junit.jupiter.api.Test;

                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

                class UnsupportedTests {

                    @Test
                    void unsupportedRoute(String route) throws Exception {
                        this.mockMvc.perform(get(route));
                    }

                    @Test
                    void supportedRoute() throws Exception {
                        this.mockMvc.perform(get("/owners"));
                    }
                }
                """);
        initGitRepo();

        HttpBehaviorExtractionResult result = extractor.extract(checkout, List.of(test));

        assertEquals(1, result.observations().size());
        assertEquals("/owners", result.observations().get(0).normalizedRouteTemplate());
        assertEquals(1, result.gaps().size());
        assertTrue(result.gaps().get(0).contains("src/test/java/com/example/UnsupportedTests.java"),
                "gap carries provenance, got " + result.gaps().get(0));
        assertTrue(result.gaps().get(0).contains("unsupported"),
                "gap names the unsupported expression, got " + result.gaps().get(0));
    }

    @Test
    void neverClassifiesUnqualifiedHelperCallsAndGapsUnsupportedFrameworkShapes() throws Exception {
        Path test = writeTestSource("HelperCallsTests.java", """
                package com.example;

                import org.junit.jupiter.api.Test;
                import org.springframework.http.HttpMethod;
                import org.springframework.web.client.RestTemplate;

                class HelperCallsTests {

                    RestTemplate restTemplate = new RestTemplate();
                    RouteHelper helper = new RouteHelper();
                    HttpMethod dynamicMethod = HttpMethod.POST;

                    @Test
                    void helperLikeCalls() {
                        getForObject("/self");
                        exchange("/also-self", dynamicMethod);
                        helper.perform("/helpers");
                        helper.exchange("/helpers/exchange", dynamicMethod);
                        restTemplate.getForObject("/owners/1", String.class);
                    }
                }
                """);
        initGitRepo();

        HttpBehaviorExtractionResult result = extractor.extract(checkout, List.of(test));

        // only the scoped RestTemplate call is an observation; helper calls
        // that merely share a framework method name are never classified
        assertEquals(1, result.observations().size());
        HttpBehaviorObservation observation = result.observations().get(0);
        assertEquals(HttpMethod.GET, observation.httpMethod());
        assertEquals("/owners/1", observation.normalizedRouteTemplate());
        assertEquals(ExtractionBasis.REST_TEMPLATE_CALL, observation.extractionBasis());

        // scoped framework-shaped calls that are not statically recoverable
        // stay explicit gaps
        assertEquals(2, result.gaps().size());
        assertTrue(result.gaps().stream().anyMatch(gap -> gap.contains("perform argument")),
                "scoped non-builder perform stays a gap, got " + result.gaps());
        assertTrue(result.gaps().stream().anyMatch(gap -> gap.contains("non-statically-recoverable HttpMethod")),
                "scoped exchange with dynamic method stays a gap, got " + result.gaps());
    }

    @Test
    void rejectsTestFileOutsideCheckout() throws Exception {
        initGitRepo();
        Path external = elsewhere.resolve("ExternalTests.java");
        Files.writeString(external, """
                package com.example;

                class ExternalTests {
                }
                """, StandardCharsets.UTF_8);

        RuntimeContractException failure = assertThrows(RuntimeContractException.class,
                () -> extractor.extract(checkout, List.of(external)));
        assertTrue(failure.getMessage().contains("outside the checkout"),
                "failure names the containment problem, got " + failure.getMessage());
    }

    @Test
    void rejectsSymlinkEscapedTestFile() throws Exception {
        initGitRepo();
        Path outside = elsewhere.resolve("SecretTests.java");
        Files.writeString(outside, """
                package com.example;

                class SecretTests {
                }
                """, StandardCharsets.UTF_8);

        Path link = checkout.resolve("src/test/java/com/example/LinkedTests.java");
        Files.createDirectories(link.getParent());
        Files.createSymbolicLink(link, outside);

        RuntimeContractException failure = assertThrows(RuntimeContractException.class,
                () -> extractor.extract(checkout, List.of(link)));
        assertTrue(failure.getMessage().contains("symbolic link"),
                "failure names the symlink escape, got " + failure.getMessage());
    }

    @Test
    void rejectsUntrackedTestFile() throws Exception {
        Path tracked = writeTestSource("TrackedTests.java", """
                package com.example;

                class TrackedTests {
                }
                """);
        initGitRepo();
        Path untracked = writeTestSource("UntrackedTests.java", """
                package com.example;

                class UntrackedTests {
                }
                """);

        RuntimeContractException failure = assertThrows(RuntimeContractException.class,
                () -> extractor.extract(checkout, List.of(untracked)));
        assertTrue(failure.getMessage().contains("not tracked"),
                "failure names the tracking problem, got " + failure.getMessage());

        // the tracked sibling still extracts cleanly
        assertEquals(0, extractor.extract(checkout, List.of(tracked)).observations().size());
    }

    @Test
    void rejectsDirtyTestFile() throws Exception {
        Path test = writeTestSource("DirtyTests.java", """
                package com.example;

                class DirtyTests {
                }
                """);
        initGitRepo();
        Files.writeString(test, """
                package com.example;

                class DirtyTests {
                    // modified after commit
                }
                """, StandardCharsets.UTF_8);

        RuntimeContractException failure = assertThrows(RuntimeContractException.class,
                () -> extractor.extract(checkout, List.of(test)));
        assertTrue(failure.getMessage().contains("not clean"),
                "failure names the cleanliness problem, got " + failure.getMessage());
    }

    @Test
    void extractorOutputBindsStructurallyAgainstHandlerIndex() throws Exception {
        Path production = checkout.resolve("src/main/java/samples/routeaware/FixtureController.java");
        Files.createDirectories(production.getParent());
        Files.writeString(production, """
                package samples.routeaware;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/owners")
                public class FixtureController {

                    @GetMapping("/{ownerId}")
                    public String showOwner() {
                        return "owner";
                    }

                    @GetMapping("/{ownerId}/pets/{petId}")
                    public String showPet() {
                        return "pet";
                    }

                    @PostMapping("/new")
                    public String createOwner() {
                        return "created";
                    }
                }
                """, StandardCharsets.UTF_8);
        Path test = checkout.resolve("src/test/java/samples/routeaware/FixtureControllerTests.java");
        Files.createDirectories(test.getParent());
        Files.writeString(test, """
                package samples.routeaware;

                import org.junit.jupiter.api.Test;

                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

                class FixtureControllerTests {

                    @Test
                    void showConcreteOwner() throws Exception {
                        this.mockMvc.perform(get("/owners/1"));
                    }

                    @Test
                    void showTemplatedOwner() throws Exception {
                        this.mockMvc.perform(get("/owners/{ownerId}", TEST_OWNER_ID));
                    }

                    @Test
                    void showDynamicOwner() throws Exception {
                        this.mockMvc.perform(get("/owners/" + ownerId));
                    }

                    @Test
                    void createOwner() throws Exception {
                        this.mockMvc.perform(post("/owners/new"));
                    }
                }
                """, StandardCharsets.UTF_8);
        initGitRepo();

        HttpBehaviorExtractionResult extraction = extractor.extract(checkout, List.of(test));
        assertEquals(List.of(), extraction.gaps());
        assertEquals(4, extraction.observations().size());

        SpringRouteHandlerIndex index = SpringRouteHandlerIndex.build(checkout,
                List.of(Path.of("src/main/java/samples/routeaware/FixtureController.java")));
        assertEquals(extraction.sourceRevision(), index.sourceRevision(),
                "observations and handlers bind at the same verified revision");

        // every observation form — concrete segment, declared placeholder, and
        // canonicalized {variable} — binds the same single handler structurally
        RouteResolution concrete = resolveObservation(index, extraction, "/owners/1");
        assertEquals(RouteResolution.Status.RESOLVED, concrete.status());
        assertEquals("samples.routeaware.FixtureController#showOwner",
                concrete.candidates().get(0).productionIdentity());

        RouteResolution templated = resolveObservation(index, extraction, "/owners/{ownerId}");
        assertEquals(RouteResolution.Status.RESOLVED, templated.status());
        assertEquals(concrete.candidates(), templated.candidates());

        RouteResolution dynamic = resolveObservation(index, extraction, "/owners/{variable}");
        assertEquals(RouteResolution.Status.RESOLVED, dynamic.status());
        assertEquals(concrete.candidates(), dynamic.candidates());

        RouteResolution created = resolveObservation(index, extraction, "/owners/new");
        assertEquals(RouteResolution.Status.RESOLVED, created.status());
        assertEquals("samples.routeaware.FixtureController#createOwner",
                created.candidates().get(0).productionIdentity());
    }

    @Test
    void orderingIsStableAndIndependentOfInputOrder() throws Exception {
        Path b = writeTestSource("BTests.java", """
                package com.example;

                import org.junit.jupiter.api.Test;

                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

                class BTests {

                    @Test
                    void b() throws Exception {
                        this.mockMvc.perform(get("/b"));
                    }
                }
                """);
        Path a = writeTestSource("ATests.java", """
                package com.example;

                import org.junit.jupiter.api.Test;

                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

                class ATests {

                    @Test
                    void a() throws Exception {
                        this.mockMvc.perform(get("/a"));
                    }
                }
                """);
        initGitRepo();

        HttpBehaviorExtractionResult forward = extractor.extract(checkout, List.of(a, b));
        HttpBehaviorExtractionResult reversed = extractor.extract(checkout, List.of(b, a));

        assertEquals(forward, reversed);
        assertEquals(2, forward.observations().size());
        assertEquals("/a", forward.observations().get(0).normalizedRouteTemplate());
        assertEquals("src/test/java/com/example/ATests.java",
                forward.observations().get(0).testSourcePath());
        assertEquals("/b", forward.observations().get(1).normalizedRouteTemplate());
    }

    @Test
    void failsClosedWhenCheckoutIsNotAnExactGitRevision() throws Exception {
        Path test = writeTestSource("OrphanTests.java", """
                package com.example;

                class OrphanTests {
                }
                """);

        RuntimeContractException notARepo = assertThrows(RuntimeContractException.class,
                () -> extractor.extract(checkout, List.of(test)));
        assertTrue(notARepo.getMessage().contains("revision"),
                "failure names the revision problem, got " + notARepo.getMessage());
    }

    @Test
    void failsClosedWhenTestFileIsMissing() throws Exception {
        initGitRepo();
        Path missing = checkout.resolve("src/test/java/com/example/MissingTests.java");

        assertThrows(RuntimeContractException.class,
                () -> extractor.extract(checkout, List.of(missing)));
    }

    @Test
    void extractsFrozenPetclinicFixtureSetWithoutGaps() throws Exception {
        Path sourceRoot = checkout.resolve("petclinic");
        copyTree(PETCLINIC_FIXTURES, sourceRoot);
        initGitRepo();
        List<Path> testFiles;
        try (Stream<Path> walk = Files.walk(sourceRoot)) {
            testFiles = walk.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.getFileName().toString().endsWith("Tests.java"))
                    .sorted()
                    .toList();
        }

        HttpBehaviorExtractionResult result = extractor.extract(checkout, testFiles);

        assertEquals(List.of(), result.gaps());
        assertEquals(43, result.observations().size(),
                "frozen 818c413 MockMvc and RestTemplate observation count");
        assertObservation(result, "org/springframework/samples/petclinic/system/WelcomeControllerTests.java",
                HttpMethod.GET, "/");
        assertObservation(result, "org/springframework/samples/petclinic/vet/VetControllerTests.java",
                HttpMethod.GET, "/vets");
        assertObservation(result, "org/springframework/samples/petclinic/vet/VetControllerTests.java",
                HttpMethod.GET, "/vets.html");
        assertObservation(result, "org/springframework/samples/petclinic/owner/OwnerControllerTests.java",
                HttpMethod.GET, "/owners/new");
        assertObservation(result, "org/springframework/samples/petclinic/owner/OwnerControllerTests.java",
                HttpMethod.POST, "/owners/new");
        assertObservation(result, "org/springframework/samples/petclinic/owner/OwnerControllerTests.java",
                HttpMethod.GET, "/owners/find");
        assertObservation(result, "org/springframework/samples/petclinic/owner/OwnerControllerTests.java",
                HttpMethod.GET, "/owners");
        assertObservation(result, "org/springframework/samples/petclinic/owner/OwnerControllerTests.java",
                HttpMethod.GET, "/owners/{ownerId}");
        assertObservation(result, "org/springframework/samples/petclinic/owner/OwnerControllerTests.java",
                HttpMethod.GET, "/owners/{ownerId}/edit");
        assertObservation(result, "org/springframework/samples/petclinic/owner/OwnerControllerTests.java",
                HttpMethod.POST, "/owners/{ownerId}/edit");
        assertObservation(result, "org/springframework/samples/petclinic/owner/PetControllerTests.java",
                HttpMethod.GET, "/owners/{ownerId}/pets/new");
        assertObservation(result, "org/springframework/samples/petclinic/owner/PetControllerTests.java",
                HttpMethod.POST, "/owners/{ownerId}/pets/{petId}/edit");
        assertObservation(result, "org/springframework/samples/petclinic/owner/VisitControllerTests.java",
                HttpMethod.GET, "/owners/{ownerId}/pets/{petId}/visits/new");
        assertObservation(result, "org/springframework/samples/petclinic/owner/VisitControllerTests.java",
                HttpMethod.POST, "/owners/{ownerId}/pets/{petId}/visits/new");
        assertObservation(result, "org/springframework/samples/petclinic/PetClinicIntegrationTests.java",
                HttpMethod.GET, "/owners/1", ExtractionBasis.REST_TEMPLATE_CALL);
        assertObservation(result, "org/springframework/samples/petclinic/PetClinicIntegrationTests.java",
                HttpMethod.GET, "/owners", ExtractionBasis.REST_TEMPLATE_CALL);
        assertObservation(result, "org/springframework/samples/petclinic/PetClinicConcurrencyTests.java",
                HttpMethod.POST, "/owners/{variable}/pets/new", ExtractionBasis.REST_TEMPLATE_CALL);
        assertObservation(result, "org/springframework/samples/petclinic/system/CrashControllerIntegrationTests.java",
                HttpMethod.GET, "/oups", ExtractionBasis.REST_TEMPLATE_CALL);

        List<String> refs = result.observations().stream().map(HttpBehaviorObservation::observationRef).toList();
        assertEquals(refs.size(), refs.stream().distinct().count(), "observationRef uniqueness");
    }

    private RouteResolution resolveObservation(SpringRouteHandlerIndex index,
            HttpBehaviorExtractionResult extraction, String route) {
        HttpBehaviorObservation observation = extraction.observations().stream()
                .filter(candidate -> candidate.normalizedRouteTemplate().equals(route))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing observation for " + route));
        return index.resolve(observation.httpMethod().name(), observation.normalizedRouteTemplate());
    }

    private void assertObservation(HttpBehaviorExtractionResult result, String expectedPath,
            HttpMethod method, String route) {
        assertObservation(result, expectedPath, method, route, null);
    }

    private void assertObservation(HttpBehaviorExtractionResult result, String expectedPath,
            HttpMethod method, String route, ExtractionBasis basis) {
        boolean found = result.observations().stream().anyMatch(observation ->
                observation.testSourcePath().equals("petclinic/src/test/java/" + expectedPath)
                        && observation.httpMethod() == method
                        && observation.normalizedRouteTemplate().equals(route)
                        && (basis == null || observation.extractionBasis() == basis));
        assertTrue(found, "expected observation " + method + " " + route + " in " + expectedPath
                + " but observations were " + result.observations());
    }

    private Path writeTestSource(String fileName, String source) throws IOException {
        Path dir = checkout.resolve("src/test/java/com/example");
        Files.createDirectories(dir);
        Path file = dir.resolve(fileName);
        Files.writeString(file, source, StandardCharsets.UTF_8);
        return file;
    }

    private void initGitRepo() throws IOException {
        runGit(checkout, "init", "-q");
        runGit(checkout, "add", "-A");
        runGit(checkout, "-c", "user.name=FDI Test", "-c", "user.email=fdi@example.invalid",
                "commit", "-q", "--allow-empty", "-m", "fixture");
    }

    private static String revisionOf(Path repo) throws IOException {
        return runGit(repo, "rev-parse", "HEAD").trim();
    }

    private static String runGit(Path dir, String... args) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(dir.toAbsolutePath().toString());
        command.addAll(List.of(args));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process;
        try {
            process = builder.start();
        }
        catch (IOException e) {
            throw new IOException("git is required to resolve the exact source revision", e);
        }
        String output;
        try (java.io.InputStream stream = process.getInputStream()) {
            output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        try {
            int exit = process.waitFor();
            if (exit != 0) {
                throw new IOException("git " + String.join(" ", args) + " failed: " + output);
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("git " + String.join(" ", args) + " was interrupted", e);
        }
        return output;
    }

    private static void copyTree(Path source, Path target) throws IOException {
        try (Stream<Path> walk = Files.walk(source)) {
            for (Path path : walk.toList()) {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                }
                else {
                    Files.copy(path, destination);
                }
            }
        }
    }
}

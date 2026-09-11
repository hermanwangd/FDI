package com.featuredeliveryintelligence.fdi.product.realization.route;

import com.featuredeliveryintelligence.fdi.product.realization.route.HttpBehaviorObservation.HttpMethod;
import com.featuredeliveryintelligence.fdi.product.realization.route.RouteResolution.Status;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Focused tests for the deterministic class-plus-method Spring route-handler
 * index (SF-BL-002 Task 2B). All inputs are synthetic sources written and
 * committed into a temporary git checkout so the build is revision-bound; the
 * W1B fixture is an additional read-only parse input.
 */
class SpringRouteHandlerIndexTests {

    @TempDir
    Path tempDir;

    private static final String OWNER_CONTROLLER = """
            package samples.petclinic.owner;

            import org.springframework.stereotype.Controller;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RequestMethod;

            @Controller
            @RequestMapping("/owners")
            public class OwnerController {

                @GetMapping
                public String listOwners() {
                    return "owners/list";
                }

                @GetMapping({"/{ownerId}"})
                public String showOwner() {
                    return "owners/details";
                }

                @PostMapping("/new")
                public String createOwner() {
                    return "redirect:/owners/{ownerId}";
                }

                @RequestMapping(value = "/{ownerId}/edit", method = RequestMethod.PUT)
                public String updateOwner() {
                    return "owners/form";
                }
            }
            """;

    private static final String OWNER_ID_ALIAS_CONTROLLER = """
            package samples.petclinic.owner;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class OwnerAliasController {

                @GetMapping("/owners/{petId}")
                public String showByPet() {
                    return "alias";
                }
            }
            """;

    private static final String HELPER_NAMED_CONTROLLER = """
            package samples.petclinic.helper;

            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class HelperNamedController {

                public String getForObject(String path) {
                    return path;
                }

                public String exchange(String path) {
                    return path;
                }

                public String perform(String path) {
                    return path;
                }
            }
            """;

    private static final String VET_CONTROLLER = """
            package samples.petclinic.vet;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class VetController {

                @GetMapping({ "/vets", "/vets.html" })
                public String showVetList() {
                    return "vets/vetList";
                }
            }
            """;

    private static final String DUPLICATE_MAPPING_CONTROLLERS = """
            package samples.petclinic.owner;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class FirstAmbiguousController {

                @GetMapping("/owners/ambiguous")
                public String first() {
                    return "first";
                }
            }
            """;

    private static final String DUPLICATE_MAPPING_ALT_CONTROLLER = """
            package samples.petclinic.owner;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class SecondAmbiguousController {

                @GetMapping("/owners/ambiguous")
                public String second() {
                    return "second";
                }
            }
            """;

    private static final String UNCONSTRAINED_CONTROLLER = """
            package samples.petclinic.system;

            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class PingController {

                @RequestMapping("/ping")
                public String ping() {
                    return "pong";
                }
            }
            """;

    private static final String NON_LITERAL_AND_NON_CONTROLLER = """
            package samples.petclinic.config;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            public class NotAController {

                @GetMapping("/uncounted/route")
                public String uncounted() {
                    return "uncounted";
                }
            }

            @RestController
            public class ConfigController {

                private static final String BASE = "/config";

                @GetMapping(BASE + "/list")
                public String list() {
                    return "list";
                }

                @GetMapping("/config/explicit")
                public String explicit() {
                    return "explicit";
                }
            }
            """;

    @Test
    void buildResolvesComposedClassAndMethodMapping() throws Exception {
        Path checkout = checkout();
        Path source = writeSource(checkout, "src/main/java/samples/petclinic/owner/OwnerController.java",
                OWNER_CONTROLLER);
        commitAll(checkout);

        SpringRouteHandlerIndex index = SpringRouteHandlerIndex.build(checkout,
                List.of(Path.of("src/main/java/samples/petclinic/owner/OwnerController.java")));

        RouteResolution resolution = index.resolve("GET", "/owners/{ownerId}");
        assertEquals(Status.RESOLVED, resolution.status());
        assertEquals(HttpMethod.GET, resolution.httpMethod());
        assertEquals("/owners/{ownerId}", resolution.normalizedRouteTemplate());

        RouteHandler handler = resolution.candidates().get(0);
        assertEquals(List.of(HttpMethod.GET), handler.httpMethods());
        assertEquals("/owners/{ownerId}", handler.normalizedRouteTemplate());
        assertEquals("samples.petclinic.owner.OwnerController#showOwner", handler.productionIdentity());
        assertEquals("samples.petclinic.owner.OwnerController#showOwner GET /owners/{ownerId}", handler.handlerRef());
        int expectedLine = lineOf(OWNER_CONTROLLER, "@GetMapping({\"/{ownerId}\"})");
        assertEquals("src/main/java/samples/petclinic/owner/OwnerController.java:" + expectedLine,
                handler.sourceLocation());
        assertEquals(sha256(Files.readAllBytes(source)), handler.sourceDigest());
        assertEquals(revisionOf(checkout), index.sourceRevision(),
                "the index is bound to the exact verified HEAD revision");
        assertEquals("software-factory.sf-bl002-route-handler.v0.3", RouteHandler.SCHEMA_VERSION);
    }

    @Test
    void buildIndexesArrayMappingsAndPrefixOnlyMethods() throws Exception {
        Path checkout = checkout();
        writeSource(checkout, "src/main/java/samples/petclinic/owner/OwnerController.java", OWNER_CONTROLLER);
        writeSource(checkout, "src/main/java/samples/petclinic/vet/VetController.java", VET_CONTROLLER);
        commitAll(checkout);

        SpringRouteHandlerIndex index = SpringRouteHandlerIndex.build(checkout, List.of(
                Path.of("src/main/java/samples/petclinic/owner/OwnerController.java"),
                Path.of("src/main/java/samples/petclinic/vet/VetController.java")));

        // class-level prefix only (@GetMapping without a value)
        assertEquals(Status.RESOLVED, index.resolve("GET", "/owners").status());
        // String array value expands to one handler per element
        assertEquals(Status.RESOLVED, index.resolve("GET", "/vets").status());
        assertEquals(Status.RESOLVED, index.resolve("GET", "/vets.html").status());
        assertEquals("samples.petclinic.vet.VetController#showVetList GET /vets.html",
                index.resolve("GET", "/vets.html").candidates().get(0).handlerRef());
        // POST mapping
        assertEquals(Status.RESOLVED, index.resolve("POST", "/owners/new").status());
        // a two-segment observation never matches single-segment handlers
        assertEquals(Status.UNRESOLVED, index.resolve("GET", "/vets/anything").status());
        // a concrete segment binds the placeholder handler structurally
        RouteResolution concrete = index.resolve("GET", "/owners/42");
        assertEquals(Status.RESOLVED, concrete.status());
        assertEquals("samples.petclinic.owner.OwnerController#showOwner",
                concrete.candidates().get(0).productionIdentity());
    }

    @Test
    void requestMappingWithExplicitMethodsResolvesEachListedMethodOnly() throws Exception {
        Path checkout = checkout();
        writeSource(checkout, "src/main/java/samples/petclinic/owner/OwnerController.java", OWNER_CONTROLLER);
        commitAll(checkout);

        SpringRouteHandlerIndex index = SpringRouteHandlerIndex.build(checkout,
                List.of(Path.of("src/main/java/samples/petclinic/owner/OwnerController.java")));

        RouteResolution put = index.resolve("PUT", "/owners/{ownerId}/edit");
        assertEquals(Status.RESOLVED, put.status());
        assertEquals(List.of(HttpMethod.PUT), put.candidates().get(0).httpMethods());
        // an unlisted method never resolves: the binding is exact, never guessed
        assertEquals(Status.UNRESOLVED, index.resolve("POST", "/owners/{ownerId}/edit").status());
        // Spring's implicit HEAD-for-GET behavior is deliberately not modeled
        assertEquals(Status.UNRESOLVED, index.resolve("HEAD", "/owners").status());
    }

    @Test
    void placeholderSegmentsBindExactlyOneObservedSegment() throws Exception {
        Path checkout = checkout();
        writeSource(checkout, "src/main/java/samples/petclinic/owner/OwnerController.java", OWNER_CONTROLLER);
        commitAll(checkout);

        SpringRouteHandlerIndex index = SpringRouteHandlerIndex.build(checkout,
                List.of(Path.of("src/main/java/samples/petclinic/owner/OwnerController.java")));

        // a concrete segment, a canonicalized {variable} segment, and the
        // handler's own placeholder name all bind the same single handler
        RouteResolution concrete = index.resolve("GET", "/owners/42");
        assertEquals(Status.RESOLVED, concrete.status());
        assertEquals("samples.petclinic.owner.OwnerController#showOwner",
                concrete.candidates().get(0).productionIdentity());

        RouteResolution canonical = index.resolve("GET", "/owners/{variable}");
        assertEquals(Status.RESOLVED, canonical.status());
        assertEquals(concrete.candidates(), canonical.candidates(),
                "placeholder names never affect structural binding");

        RouteResolution named = index.resolve("GET", "/owners/{ownerId}");
        assertEquals(Status.RESOLVED, named.status());
        assertEquals(concrete.candidates(), named.candidates());

        // segment-count equality is required: no single-segment swallowing
        assertEquals(Status.UNRESOLVED, index.resolve("GET", "/owners/42/pets").status());
        // a literal handler segment never binds a different literal segment
        assertEquals(Status.UNRESOLVED, index.resolve("POST", "/owners/43").status());
        // a placeholder handler segment still binds any single non-empty segment
        assertEquals(Status.RESOLVED, index.resolve("PUT", "/owners/anything/edit").status());
    }

    @Test
    void multipleStructuralMatchesStayAmbiguous() throws Exception {
        Path checkout = checkout();
        writeSource(checkout, "src/main/java/samples/petclinic/owner/OwnerController.java", OWNER_CONTROLLER);
        writeSource(checkout, "src/main/java/samples/petclinic/owner/OwnerAliasController.java",
                OWNER_ID_ALIAS_CONTROLLER);
        commitAll(checkout);

        SpringRouteHandlerIndex index = SpringRouteHandlerIndex.build(checkout, List.of(
                Path.of("src/main/java/samples/petclinic/owner/OwnerController.java"),
                Path.of("src/main/java/samples/petclinic/owner/OwnerAliasController.java")));

        // two placeholder handlers with different names both structurally
        // match the observation; there is no ranking and no fuzzy fallback
        RouteResolution resolution = index.resolve("GET", "/owners/42");
        assertEquals(Status.AMBIGUOUS, resolution.status());
        assertEquals(2, resolution.candidates().size());
        assertEquals("samples.petclinic.owner.OwnerAliasController#showByPet GET /owners/{petId}",
                resolution.candidates().get(0).handlerRef());
        assertEquals("samples.petclinic.owner.OwnerController#showOwner GET /owners/{ownerId}",
                resolution.candidates().get(1).handlerRef());
    }

    @Test
    void unconstrainedRequestMappingMatchesEveryHttpMethod() throws Exception {
        Path checkout = checkout();
        writeSource(checkout, "src/main/java/samples/petclinic/system/PingController.java",
                UNCONSTRAINED_CONTROLLER);
        commitAll(checkout);

        SpringRouteHandlerIndex index = SpringRouteHandlerIndex.build(checkout,
                List.of(Path.of("src/main/java/samples/petclinic/system/PingController.java")));

        for (HttpMethod method : HttpMethod.values()) {
            RouteResolution resolution = index.resolve(method.name(), "/ping");
            assertEquals(Status.RESOLVED, resolution.status(), method.name());
            assertEquals(List.of(HttpMethod.values()), resolution.candidates().get(0).httpMethods());
        }
        assertEquals("samples.petclinic.system.PingController#ping "
                        + "GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS /ping",
                index.resolve("GET", "/ping").candidates().get(0).handlerRef());
    }

    @Test
    void resolveReturnsUnresolvedForZeroMatchesAndEmptyIndex() throws Exception {
        Path checkout = checkout();
        writeSource(checkout, "src/main/java/samples/petclinic/owner/OwnerController.java", OWNER_CONTROLLER);
        commitAll(checkout);

        SpringRouteHandlerIndex empty = SpringRouteHandlerIndex.build(checkout, List.of());
        RouteResolution emptyResolution = empty.resolve("GET", "/owners");
        assertEquals(Status.UNRESOLVED, emptyResolution.status());
        assertTrue(emptyResolution.candidates().isEmpty());
        assertTrue(empty.handlers().isEmpty());

        SpringRouteHandlerIndex index = SpringRouteHandlerIndex.build(checkout,
                List.of(Path.of("src/main/java/samples/petclinic/owner/OwnerController.java")));
        RouteResolution resolution = index.resolve("GET", "/no/such/route");
        assertEquals(Status.UNRESOLVED, resolution.status());
        assertTrue(resolution.candidates().isEmpty());
    }

    @Test
    void resolveReturnsAmbiguousForDuplicateMappings() throws Exception {
        Path checkout = checkout();
        writeSource(checkout, "src/main/java/samples/petclinic/owner/FirstAmbiguousController.java",
                DUPLICATE_MAPPING_CONTROLLERS);
        writeSource(checkout, "src/main/java/samples/petclinic/owner/SecondAmbiguousController.java",
                DUPLICATE_MAPPING_ALT_CONTROLLER);
        commitAll(checkout);

        SpringRouteHandlerIndex index = SpringRouteHandlerIndex.build(checkout, List.of(
                Path.of("src/main/java/samples/petclinic/owner/FirstAmbiguousController.java"),
                Path.of("src/main/java/samples/petclinic/owner/SecondAmbiguousController.java")));

        RouteResolution resolution = index.resolve("GET", "/owners/ambiguous");
        assertEquals(Status.AMBIGUOUS, resolution.status());
        assertEquals(2, resolution.candidates().size());
        // candidates stay ordered by handlerRef, independent of file order
        assertEquals("samples.petclinic.owner.FirstAmbiguousController#first GET /owners/ambiguous",
                resolution.candidates().get(0).handlerRef());
        assertEquals("samples.petclinic.owner.SecondAmbiguousController#second GET /owners/ambiguous",
                resolution.candidates().get(1).handlerRef());
    }

    @Test
    void buildSkipsNonControllerClassesAndNonLiteralMappingValues() throws Exception {
        Path checkout = checkout();
        writeSource(checkout, "src/main/java/samples/petclinic/config/MixedController.java",
                NON_LITERAL_AND_NON_CONTROLLER);
        commitAll(checkout);

        SpringRouteHandlerIndex index = SpringRouteHandlerIndex.build(checkout,
                List.of(Path.of("src/main/java/samples/petclinic/config/MixedController.java")));

        // no @Controller/@RestController stereotype: never indexed
        assertEquals(Status.UNRESOLVED, index.resolve("GET", "/uncounted/route").status());
        // a route value that is not a literal in the annotation is not statically
        // recoverable and stays an explicit gap instead of a guessed handler
        assertEquals(Status.UNRESOLVED, index.resolve("GET", "/config/list").status());
        // the literal sibling mapping of the same controller is indexed
        RouteResolution resolution = index.resolve("GET", "/config/explicit");
        assertEquals(Status.RESOLVED, resolution.status());
        assertEquals("samples.petclinic.config.ConfigController#explicit", resolution.candidates().get(0).productionIdentity());
    }

    @Test
    void helperMethodsNamedLikeFrameworkCallsAreNeverIndexed() throws Exception {
        Path checkout = checkout();
        writeSource(checkout, "src/main/java/samples/petclinic/helper/HelperNamedController.java",
                HELPER_NAMED_CONTROLLER);
        commitAll(checkout);

        SpringRouteHandlerIndex index = SpringRouteHandlerIndex.build(checkout,
                List.of(Path.of("src/main/java/samples/petclinic/helper/HelperNamedController.java")));

        // methods are only indexed through mapping annotations; helper methods
        // merely named getForObject, exchange, or perform are never handlers
        assertTrue(index.handlers().isEmpty());
        assertEquals(Status.UNRESOLVED, index.resolve("GET", "/owners").status());
    }

    @Test
    void buildRejectsUnsafeAndNonCanonicalInputs() throws Exception {
        Path checkout = checkout();
        Path source = writeSource(checkout, "src/main/java/samples/petclinic/owner/OwnerController.java",
                OWNER_CONTROLLER);
        commitAll(checkout);
        Path outside = writeFile(tempDir.resolve("outside.java"), OWNER_CONTROLLER);

        assertThrows(RuntimeContractException.class, () -> SpringRouteHandlerIndex.build(null, List.of()));
        assertThrows(RuntimeContractException.class, () -> SpringRouteHandlerIndex.build(source, List.of()));
        assertThrows(RuntimeContractException.class,
                () -> SpringRouteHandlerIndex.build(checkout, null));
        assertThrows(RuntimeContractException.class,
                () -> SpringRouteHandlerIndex.build(checkout, java.util.Arrays.asList((Path) null)));
        assertThrows(RuntimeContractException.class,
                () -> SpringRouteHandlerIndex.build(checkout, List.of(Path.of("missing/Nowhere.java"))));
        assertThrows(RuntimeContractException.class,
                () -> SpringRouteHandlerIndex.build(checkout, List.of(outside)));
        assertThrows(RuntimeContractException.class, () -> SpringRouteHandlerIndex.build(checkout,
                List.of(Path.of("src/main/java/samples/petclinic/owner/OwnerController.java"),
                        Path.of("src/main/java/samples/petclinic/owner/OwnerController.java"))));
        Path readme = writeFile(checkout.resolve("README.md"), "not java");
        assertThrows(RuntimeContractException.class,
                () -> SpringRouteHandlerIndex.build(checkout, List.of(readme)));
    }

    @Test
    void buildRejectsNonGitCheckout() throws Exception {
        Path plain = Files.createDirectories(tempDir.resolve("plain"));

        RuntimeContractException failure = assertThrows(RuntimeContractException.class,
                () -> SpringRouteHandlerIndex.build(plain, List.of()));
        assertTrue(failure.getMessage().contains("revision"),
                "failure names the revision problem, got " + failure.getMessage());
    }

    @Test
    void buildRejectsUntrackedAndDirtyProductionFiles() throws Exception {
        Path checkout = checkout();
        writeSource(checkout, "src/main/java/samples/petclinic/owner/OwnerController.java", OWNER_CONTROLLER);
        commitAll(checkout);

        SpringRouteHandlerIndex verified = SpringRouteHandlerIndex.build(checkout,
                List.of(Path.of("src/main/java/samples/petclinic/owner/OwnerController.java")));
        assertEquals(revisionOf(checkout), verified.sourceRevision());

        // an untracked production file is rejected before any handler is emitted
        Path untracked = writeSource(checkout, "src/main/java/samples/petclinic/vet/VetController.java",
                VET_CONTROLLER);
        RuntimeContractException untrackedFailure = assertThrows(RuntimeContractException.class,
                () -> SpringRouteHandlerIndex.build(checkout,
                        List.of(Path.of("src/main/java/samples/petclinic/vet/VetController.java"))));
        assertTrue(untrackedFailure.getMessage().contains("not tracked"),
                "failure names the tracking problem, got " + untrackedFailure.getMessage());

        // a dirty (modified after commit) production file is rejected
        Path owner = checkout.resolve("src/main/java/samples/petclinic/owner/OwnerController.java");
        Files.writeString(owner, OWNER_CONTROLLER + "\n// dirty\n", StandardCharsets.UTF_8);
        RuntimeContractException dirtyFailure = assertThrows(RuntimeContractException.class,
                () -> SpringRouteHandlerIndex.build(checkout,
                        List.of(Path.of("src/main/java/samples/petclinic/owner/OwnerController.java"))));
        assertTrue(dirtyFailure.getMessage().contains("not clean"),
                "failure names the cleanliness problem, got " + dirtyFailure.getMessage());

        // committing restores trustworthiness
        commitAll(checkout);
        assertEquals(Status.RESOLVED, SpringRouteHandlerIndex.build(checkout,
                List.of(Path.of("src/main/java/samples/petclinic/vet/VetController.java")))
                .resolve("GET", "/vets").status());
    }

    @Test
    void buildRejectsSymlinkEscapedProductionFile() throws Exception {
        Path checkout = checkout();
        Path outside = writeFile(tempDir.resolve("outside/SecretController.java"), OWNER_CONTROLLER);
        commitAll(checkout);

        Path link = checkout.resolve("src/main/java/samples/petclinic/owner/LinkedController.java");
        Files.createDirectories(link.getParent());
        Files.createSymbolicLink(link, outside);

        RuntimeContractException failure = assertThrows(RuntimeContractException.class,
                () -> SpringRouteHandlerIndex.build(checkout,
                        List.of(Path.of("src/main/java/samples/petclinic/owner/LinkedController.java"))));
        assertTrue(failure.getMessage().contains("symbolic link"),
                "failure names the symlink escape, got " + failure.getMessage());
    }

    @Test
    void buildFailsClosedOnUnparsableProductionSource() throws Exception {
        Path checkout = checkout();
        writeSource(checkout, "src/main/java/samples/petclinic/broken/BrokenController.java",
                "this is not valid java at all {{{");
        commitAll(checkout);

        assertThrows(RuntimeContractException.class, () -> SpringRouteHandlerIndex.build(checkout,
                List.of(Path.of("src/main/java/samples/petclinic/broken/BrokenController.java"))));
    }

    @Test
    void buildIsDeterministicAcrossInputFileOrder() throws Exception {
        Path checkout = checkout();
        Path ownerRel = Path.of("src/main/java/samples/petclinic/owner/OwnerController.java");
        Path vetRel = Path.of("src/main/java/samples/petclinic/vet/VetController.java");
        writeSource(checkout, ownerRel.toString(), OWNER_CONTROLLER);
        writeSource(checkout, vetRel.toString(), VET_CONTROLLER);
        commitAll(checkout);

        SpringRouteHandlerIndex forward = SpringRouteHandlerIndex.build(checkout, List.of(ownerRel, vetRel));
        SpringRouteHandlerIndex reverse = SpringRouteHandlerIndex.build(checkout, List.of(vetRel, ownerRel));

        assertEquals(forward.handlers(), reverse.handlers());
        List<String> refs = forward.handlers().stream().map(RouteHandler::handlerRef).toList();
        assertEquals(refs.stream().sorted().toList(), refs, "handlers are ordered by handlerRef");
        assertEquals(6, forward.handlers().size());
    }

    @Test
    void resolveRejectsInvalidMethodAndRoute() throws Exception {
        Path checkout = checkout();
        writeSource(checkout, "src/main/java/samples/petclinic/owner/OwnerController.java", OWNER_CONTROLLER);
        commitAll(checkout);
        SpringRouteHandlerIndex index = SpringRouteHandlerIndex.build(checkout,
                List.of(Path.of("src/main/java/samples/petclinic/owner/OwnerController.java")));

        assertThrows(RuntimeContractException.class, () -> index.resolve(null, "/owners"));
        assertThrows(RuntimeContractException.class, () -> index.resolve(" ", "/owners"));
        assertThrows(RuntimeContractException.class, () -> index.resolve("FETCH", "/owners"));
        assertThrows(RuntimeContractException.class, () -> index.resolve("GET", null));
        assertThrows(RuntimeContractException.class, () -> index.resolve("GET", "owners"));
        assertThrows(RuntimeContractException.class, () -> index.resolve("GET", "/owners?active=true"));
        assertThrows(RuntimeContractException.class, () -> index.resolve("GET", "/owners//pets"));
        assertThrows(RuntimeContractException.class, () -> index.resolve("GET", "/owners/{"));
        assertThrows(RuntimeContractException.class, () -> index.resolve("GET", "/owners/}"));
        // malformed placeholder shapes are rejected explicitly, never silently matched
        assertThrows(RuntimeContractException.class, () -> index.resolve("GET", "/owners/{id}}"));
        assertThrows(RuntimeContractException.class, () -> index.resolve("GET", "/owners/}{id}"));
        // method parsing is case-insensitive
        assertEquals(Status.RESOLVED, index.resolve("get", "/owners").status());
    }

    @Test
    void indexesW1BRouteFixtureExactly() throws Exception {
        Path checkout = checkout();
        Path fixtureTarget = checkout.resolve("src/main/java/scenarioforward/sfbl002/routeaware/RouteFixtureController.java");
        Files.createDirectories(fixtureTarget.getParent());
        Files.copy(Path.of("src/test/resources/scenarioforward/sf-bl002/route-aware/RouteFixtureController.java"),
                fixtureTarget);
        commitAll(checkout);

        SpringRouteHandlerIndex index = SpringRouteHandlerIndex.build(checkout,
                List.of(Path.of("src/main/java/scenarioforward/sfbl002/routeaware/RouteFixtureController.java")));

        RouteResolution list = index.resolve("GET", "/owners");
        assertEquals(Status.RESOLVED, list.status());
        assertEquals("scenarioforward.sfbl002.routeaware.RouteFixtureController#listOwners",
                list.candidates().get(0).productionIdentity());

        assertEquals(Status.RESOLVED, index.resolve("GET", "/owners/{ownerId}").status());
        assertEquals(Status.RESOLVED, index.resolve("POST", "/owners").status());
        assertEquals(Status.RESOLVED, index.resolve("GET", "/owners/{ownerId}/pets/{petId}").status());

        RouteResolution ambiguous = index.resolve("GET", "/owners/ambiguous");
        assertEquals(Status.AMBIGUOUS, ambiguous.status());
        // three handlers bind structurally: the two literal /owners/ambiguous
        // mappings and the placeholder /owners/{ownerId} mapping; candidates
        // stay ordered by handlerRef
        assertEquals(3, ambiguous.candidates().size());
        assertEquals("scenarioforward.sfbl002.routeaware.AmbiguousFixtureAltController#secondAmbiguousMapping",
                ambiguous.candidates().get(0).productionIdentity());
        assertEquals("scenarioforward.sfbl002.routeaware.AmbiguousFixtureController#firstAmbiguousMapping",
                ambiguous.candidates().get(1).productionIdentity());
        assertEquals("scenarioforward.sfbl002.routeaware.RouteFixtureController#showOwner",
                ambiguous.candidates().get(2).productionIdentity());
    }

    // --- helpers ---

    private Path checkout() throws Exception {
        Path checkout = Files.createDirectories(tempDir.resolve("checkout"));
        runGit(checkout, "init", "-q");
        runGit(checkout, "add", "-A");
        runGit(checkout, "-c", "user.name=FDI Test", "-c", "user.email=fdi@example.invalid",
                "commit", "-q", "--allow-empty", "-m", "fixture");
        return checkout;
    }

    private void commitAll(Path checkout) throws Exception {
        runGit(checkout, "add", "-A");
        runGit(checkout, "-c", "user.name=FDI Test", "-c", "user.email=fdi@example.invalid",
                "commit", "-q", "--allow-empty", "-m", "fixture");
    }

    private static String revisionOf(Path repo) throws Exception {
        return runGit(repo, "rev-parse", "HEAD").trim();
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

    private Path writeSource(Path checkout, String relativePath, String source) throws Exception {
        return writeFile(checkout.resolve(relativePath), source);
    }

    private static Path writeFile(Path target, String content) throws Exception {
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
        return target;
    }

    private static int lineOf(String source, String needle) {
        int idx = source.indexOf(needle);
        assertTrue(idx >= 0, "needle must be present: " + needle);
        return source.substring(0, idx).split("\n", -1).length;
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

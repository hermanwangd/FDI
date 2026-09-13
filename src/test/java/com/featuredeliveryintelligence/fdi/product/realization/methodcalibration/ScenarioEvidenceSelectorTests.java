package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ScenarioEvidenceSelectorTests {
    @TempDir Path root;
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void diagnosticsRejectExpansionAboveLimitBeforeAllocatingPairs() throws Exception {
        var input = JSON.createObjectNode();
        var rows = input.putArray("observations");
        var row = observations("unused").required("observations").get(0);
        for (int i = 0; i < 100001; i++) rows.add(row);
        var failure = assertThrows(IllegalArgumentException.class, () ->
                ScenarioEvidenceSelector.selectWithDiagnostics(intent("S", "CREATE", "[]"),
                        input, JSON.readTree("{\"handlers\":[]}"), root));
        assertEquals("DIAGNOSTIC_PAIR_LIMIT_EXCEEDED", failure.getMessage());
        assertTrue(ScenarioEvidenceSelector.select(intent("S", "CREATE", "[]"),
                input, JSON.readTree("{\"handlers\":[]}"), root).isEmpty());
    }

    @Test void diagnosticsAllowExactlyLimitPairs() throws Exception {
        var input = JSON.createObjectNode();
        var rows = input.putArray("observations");
        var row = observations("unused").required("observations").get(0);
        for (int i = 0; i < 100000; i++) rows.add(row);
        var result = ScenarioEvidenceSelector.selectWithDiagnostics(intent("S", "CREATE", "[]"),
                input, JSON.readTree("{\"handlers\":[]}"), root);
        assertEquals(100000, result.scenarios().get(0).rejected());
        assertEquals(100000, result.scenarios().get(0).pairs().size());
    }

    @Test void legacyRejectedPairsDoNotRequireDiagnosticIdentity() throws Exception {
        var input = JSON.readTree("{\"observations\":[{\"httpMethod\":\"POST\"}]}");
        assertTrue(ScenarioEvidenceSelector.select(intent("S", "CREATE", "[]"),
                input, JSON.readTree("{\"handlers\":[]}"), root).isEmpty());
    }

    @Test void sameRouteErrorTestCannotSeedSuccessfulCreation() throws Exception {
        Files.writeString(root.resolve("OrderControllerTests.java"), """
                class OrderControllerTests {
                  void ok() { post("/orders/new").param("name","a").andExpect(is3xxRedirection()); }
                  void bad() { post("/orders/new").andExpect(attributeHasFieldErrors("order","name")); }
                }
                """);
        var selected = ScenarioEvidenceSelector.select(intent("S-a", "CREATE", "[]"),
                observations("ok", "bad"), handlers(), root);
        assertEquals(List.of("obs-ok"), selected.stream().map(ScenarioEvidenceSelector.Seed::observationRef).toList());
        assertEquals("S-a", selected.get(0).scenarioId());
        assertEquals("S-renamed", ScenarioEvidenceSelector.select(intent("S-renamed", "CREATE", "[]"),
                observations("ok", "bad"), handlers(), root).get(0).scenarioId());
    }

    @Test void duplicateRejectionNeedsSpecificErrorEvidence() throws Exception {
        Files.writeString(root.resolve("OrderControllerTests.java"), """
                class OrderControllerTests {
                  void dup() { post("/orders/new").andExpect(attributeHasFieldErrorCode("order","name","duplicate")); }
                  void bad() { post("/orders/new").andExpect(attributeHasFieldErrors("order","name")); }
                }
                """);
        var selected = ScenarioEvidenceSelector.select(intent("S", "REJECT", "[\"duplicate-name-guard\"]"),
                observations("dup", "bad"), handlers(), root);
        assertEquals(List.of("obs-dup"), selected.stream().map(ScenarioEvidenceSelector.Seed::observationRef).toList());
    }

    @Test void diagnosticReasonMatrixCoversEveryTaxonomyEntry() throws Exception {
        Files.writeString(root.resolve("OrderControllerTests.java"), """
                class OrderControllerTests {
                  void ok() { post("/orders/new").param("name","a").andExpect(is3xxRedirection()); }
                  void bad() { post("/orders/new").andExpect(attributeHasFieldErrors("order","name")); }
                  void mixed() { post("/orders/new"); get("/health").andExpect(isOk()); }
                  void standalone() { post("/orders/new"); isOk(); }
                  void deferred() { Runnable later = () -> post("/orders/new").andExpect(isOk()); }
                }
                """);
        var observations = JSON.createObjectNode();
        var array = observations.putArray("observations");
        array.addObject().put("observationRef", "obs-ok").put("testSourcePath", "OrderControllerTests.java")
                .put("testMethod", "ok").put("httpMethod", "POST").put("normalizedRouteTemplate", "/orders/new");
        array.addObject().put("observationRef", "obs-absent").put("testSourcePath", "OrderControllerTests.java")
                .put("testMethod", "ok").put("httpMethod", "POST").put("normalizedRouteTemplate", "/no/such/route");
        array.addObject().put("observationRef", "obs-mixed").put("testSourcePath", "OrderControllerTests.java")
                .put("testMethod", "mixed").put("httpMethod", "POST").put("normalizedRouteTemplate", "/orders/new");
        array.addObject().put("observationRef", "obs-standalone").put("testSourcePath", "OrderControllerTests.java")
                .put("testMethod", "standalone").put("httpMethod", "POST").put("normalizedRouteTemplate", "/orders/new");
        array.addObject().put("observationRef", "obs-deferred").put("testSourcePath", "OrderControllerTests.java")
                .put("testMethod", "deferred").put("httpMethod", "POST").put("normalizedRouteTemplate", "/orders/new");
        array.addObject().put("observationRef", "obs-missing").put("testSourcePath", "OrderControllerTests.java")
                .put("testMethod", "doesNotExist").put("httpMethod", "POST").put("normalizedRouteTemplate", "/orders/new");
        array.addObject().put("observationRef", "obs-bad").put("testSourcePath", "OrderControllerTests.java")
                .put("testMethod", "bad").put("httpMethod", "POST").put("normalizedRouteTemplate", "/orders/new");
        var diagnostics = ScenarioEvidenceSelector.selectWithDiagnostics(intent("S", "CREATE", "[]"), observations,
                handlers(), root);
        assertEquals(List.of("obs-ok", "obs-absent", "obs-mixed", "obs-standalone", "obs-deferred", "obs-missing",
                "obs-bad"), diagnostics.scenarios().get(0).pairs().stream()
                .map(ScenarioEvidenceSelector.PairDiagnostic::observationRef).toList());
        assertEquals(List.of(ScenarioEvidenceSelector.DiagnosticReason.ACCEPTED,
                ScenarioEvidenceSelector.DiagnosticReason.ROUTE_ABSENT,
                ScenarioEvidenceSelector.DiagnosticReason.REQUEST_AMBIGUOUS,
                ScenarioEvidenceSelector.DiagnosticReason.UNSUPPORTED_ASSERTION_DIALECT,
                ScenarioEvidenceSelector.DiagnosticReason.REQUEST_AMBIGUOUS,
                ScenarioEvidenceSelector.DiagnosticReason.TEST_IDENTITY,
                ScenarioEvidenceSelector.DiagnosticReason.ASSERTION_POLARITY),
                diagnostics.scenarios().get(0).pairs().stream()
                        .map(ScenarioEvidenceSelector.PairDiagnostic::reason).toList());
        assertEquals(7, diagnostics.scenarios().get(0).evaluated());
        assertEquals(1, diagnostics.scenarios().get(0).accepted());
        assertEquals(6, diagnostics.scenarios().get(0).rejected());
        assertEquals(List.of("obs-ok"), diagnostics.seeds().stream()
                .map(ScenarioEvidenceSelector.Seed::observationRef).toList());
    }

    @Test void routeAmbiguityEntityAndActionMismatchAreDistinctWithDeterministicPrecedence() throws Exception {
        Files.writeString(root.resolve("OrderControllerTests.java"), """
                class OrderControllerTests {
                  void ok() { post("/orders/new").param("name","a").andExpect(is3xxRedirection()); }
                }
                """);
        var ambiguousHandlers = JSON.readTree("""
                {"handlers":[
                {"productionIdentity":"demo.OrderController#create","httpMethods":["POST"],
                 "normalizedRouteTemplate":"/orders/new"},
                {"productionIdentity":"demo.OrderController#duplicate","httpMethods":["POST"],
                 "normalizedRouteTemplate":"/orders/new"}]}
                """);
        assertEquals(ScenarioEvidenceSelector.DiagnosticReason.ROUTE_AMBIGUOUS, firstReason(
                ScenarioEvidenceSelector.selectWithDiagnostics(intent("S", "CREATE", "[]"),
                        observations("ok"), ambiguousHandlers, root)));
        // Entity mismatch precedes action mismatch when both fail.
        assertEquals(ScenarioEvidenceSelector.DiagnosticReason.ENTITY_MISMATCH, firstReason(
                ScenarioEvidenceSelector.selectWithDiagnostics(JSON.readTree("""
                        {"records":[{"scenarioId":"S","entity":"CUSTOMER","action":"CREATE","conditions":[]}]}
                        """), observations("ok"), handlers(), root)));
        // Wrong action on a matching route/entity.
        assertEquals(ScenarioEvidenceSelector.DiagnosticReason.ACTION_MISMATCH, firstReason(
                ScenarioEvidenceSelector.selectWithDiagnostics(intent("S", "FIND", "[]"),
                        observations("ok"), handlers(), root)));
    }

    @Test void polarityAndConditionRejectionsAreNotRelabeledAsDialectFailures() throws Exception {
        Files.writeString(root.resolve("OrderControllerTests.java"), """
                class OrderControllerTests {
                  void ok() { post("/orders/new").param("name","a").andExpect(is3xxRedirection()); }
                  void bad() { post("/orders/new").andExpect(attributeHasFieldErrors("order","name")); }
                  void dup() { post("/orders/new").andExpect(attributeHasFieldErrorCode("order","name","duplicate")); }
                  void browseNoPage() { get("/owners").andExpect(isOk()); }
                  void browsePaged() { get("/owners?page=1").andExpect(isOk()); }
                }
                """);
        var ownersHandlers = JSON.readTree("""
                {"handlers":[
                {"productionIdentity":"demo.OrderController#create","httpMethods":["POST"],
                 "normalizedRouteTemplate":"/orders/new"},
                {"productionIdentity":"demo.OrderController#listOwners","httpMethods":["GET"],
                 "normalizedRouteTemplate":"/owners"}]}
                """);
        var reject = ScenarioEvidenceSelector.selectWithDiagnostics(
                intent("S", "REJECT", "[\"duplicate-name-guard\"]"),
                observations("ok", "bad", "dup"), handlers(), root);
        assertEquals(List.of(ScenarioEvidenceSelector.DiagnosticReason.ASSERTION_POLARITY,
                ScenarioEvidenceSelector.DiagnosticReason.UNMET_CONDITION,
                ScenarioEvidenceSelector.DiagnosticReason.ACCEPTED), reject.scenarios().get(0).pairs().stream()
                .map(ScenarioEvidenceSelector.PairDiagnostic::reason).toList());
        var browseObservations = JSON.createObjectNode();
        var array = browseObservations.putArray("observations");
        for (String name : List.of("browseNoPage", "browsePaged")) array.addObject()
                .put("observationRef", "obs-" + name).put("testSourcePath", "OrderControllerTests.java")
                .put("testMethod", name).put("httpMethod", "GET").put("normalizedRouteTemplate", "/owners");
        var browse = ScenarioEvidenceSelector.selectWithDiagnostics(intent("S", "BROWSE", "[\"paged-results\"]"),
                browseObservations, ownersHandlers, root);
        assertEquals(List.of(ScenarioEvidenceSelector.DiagnosticReason.UNMET_CONDITION,
                ScenarioEvidenceSelector.DiagnosticReason.ACCEPTED), browse.scenarios().get(0).pairs().stream()
                .map(ScenarioEvidenceSelector.PairDiagnostic::reason).toList());
    }

    @Test void seedOrderAndContentAreIdenticalWithDiagnosticsOnOrOff() throws Exception {
        Files.writeString(root.resolve("OrderControllerTests.java"), """
                class OrderControllerTests {
                  void ok() { post("/orders/new").param("name","a").andExpect(is3xxRedirection()); }
                  void bad() { post("/orders/new").andExpect(attributeHasFieldErrors("order","name")); }
                  void dup() { post("/orders/new").andExpect(attributeHasFieldErrorCode("order","name","duplicate")); }
                }
                """);
        var intents = JSON.readTree("""
                {"records":[
                 {"scenarioId":"S-1","entity":"ORDER","action":"CREATE","conditions":[]},
                 {"scenarioId":"S-2","entity":"ORDER","action":"REJECT","conditions":["duplicate-name-guard"]}]}
                """);
        var observations = JSON.readTree("""
                {"observations":[
                 {"observationRef":"obs-ok","testSourcePath":"OrderControllerTests.java","testMethod":"ok",
                  "httpMethod":"POST","normalizedRouteTemplate":"/orders/new"},
                 {"observationRef":"obs-bad","testSourcePath":"OrderControllerTests.java","testMethod":"bad",
                  "httpMethod":"POST","normalizedRouteTemplate":"/orders/new"},
                 {"observationRef":"obs-dup","testSourcePath":"OrderControllerTests.java","testMethod":"dup",
                  "httpMethod":"POST","normalizedRouteTemplate":"/orders/new"}]}
                """);
        var plain = ScenarioEvidenceSelector.select(intents, observations, handlers(), root);
        var withDiagnostics = ScenarioEvidenceSelector.selectWithDiagnostics(intents, observations, handlers(), root);
        assertEquals(plain, withDiagnostics.seeds());
        assertEquals(plain.stream().map(ScenarioEvidenceSelector.Seed::toString).toList(),
                withDiagnostics.seeds().stream().map(ScenarioEvidenceSelector.Seed::toString).toList());
        assertEquals(List.of("S-1", "S-2"), withDiagnostics.seeds().stream()
                .map(ScenarioEvidenceSelector.Seed::scenarioId).toList());
        assertEquals(6, withDiagnostics.scenarios().stream()
                .mapToInt(ScenarioEvidenceSelector.ScenarioSelection::evaluated).sum());
    }

    @Test void scenariosWithoutObservationsAreExplicitlyRepresented() throws Exception {
        var diagnostics = ScenarioEvidenceSelector.selectWithDiagnostics(intent("S", "CREATE", "[]"),
                JSON.readTree("{\"observations\":[]}"), handlers(), root);
        assertEquals(1, diagnostics.scenarios().size());
        var scenario = diagnostics.scenarios().get(0);
        assertEquals("S", scenario.scenarioId());
        assertEquals(0, scenario.evaluated());
        assertEquals(0, scenario.accepted());
        assertEquals(0, scenario.rejected());
        assertTrue(scenario.pairs().isEmpty());
        assertTrue(diagnostics.seeds().isEmpty());
    }

    @Test void malformedInputStillThrowsAndIsNotCountedAsRejection() throws Exception {
        Files.writeString(root.resolve("BrokenTests.java"), "this is not java {{{");
        var brokenObservation = JSON.readTree("""
                {"observations":[
                 {"observationRef":"obs-broken","testSourcePath":"BrokenTests.java","testMethod":"ok",
                  "httpMethod":"POST","normalizedRouteTemplate":"/orders/new"}]}
                """);
        assertThrows(IllegalArgumentException.class, () -> ScenarioEvidenceSelector.selectWithDiagnostics(
                intent("S", "CREATE", "[]"), brokenObservation, handlers(), root));
        assertThrows(IllegalArgumentException.class, () -> ScenarioEvidenceSelector.select(
                intent("S", "CREATE", "[]"), brokenObservation, handlers(), root));
        assertThrows(Exception.class, () -> ScenarioEvidenceSelector.selectWithDiagnostics(
                JSON.readTree("{\"records\":[{\"scenarioId\":\"S\",\"entity\":\"ORDER\",\"action\":\"CREATE\"}]}"),
                observations("ok"), handlers(), root));
    }

    private static ScenarioEvidenceSelector.DiagnosticReason firstReason(
            ScenarioEvidenceSelector.SelectionDiagnostics diagnostics) {
        return diagnostics.scenarios().get(0).pairs().get(0).reason();
    }

    private static com.fasterxml.jackson.databind.JsonNode intent(String id, String action, String conditions) throws Exception {
        return JSON.readTree("{\"records\":[{\"scenarioId\":\"" + id + "\",\"entity\":\"ORDER\",\"action\":\"" + action
                + "\",\"conditions\":" + conditions + "}]}");
    }
    @Test void unrelatedRequestCannotSupplyPositiveAssertion() throws Exception {
        Files.writeString(root.resolve("OrderControllerTests.java"), """
                class OrderControllerTests {
                  void mixed() { post("/orders/new"); get("/health").andExpect(isOk()); }
                }
                """);
        assertTrue(ScenarioEvidenceSelector.select(intent("S", "CREATE", "[]"),
                observations("mixed"), handlers(), root).isEmpty());
    }
    @Test void standaloneOrDeferredAssertionsAreNotResponseEvidence() throws Exception {
        Files.writeString(root.resolve("OrderControllerTests.java"), """
                class OrderControllerTests {
                  void standalone() { post("/orders/new"); isOk(); }
                  void deferred() { Runnable later = () -> post("/orders/new").andExpect(isOk()); }
                }
                """);
        assertTrue(ScenarioEvidenceSelector.select(intent("S", "CREATE", "[]"),
                observations("standalone", "deferred"), handlers(), root).isEmpty());
    }
    @Test void unrelatedPagingLiteralIsNotRequestEvidence() {
        var test = ScenarioEvidenceSelector.parser().parse("""
                class Tests { void browse(){String unrelated="currentPage";
                mock.perform(get("/items")).andExpect(status().isOk());} }
                """).getResult().orElseThrow().findFirst(com.github.javaparser.ast.body.MethodDeclaration.class).orElseThrow();
        assertFalse(ScenarioEvidenceSelector.qualifies(test,"BROWSE",List.of("paged-results")));
    }
    @Test void queryEvidenceDistinguishesEmptyNonemptyAndNormalizedInput() {
        for (String value : List.of("", "   ", "Smith", " Smith ")) {
            var test = ScenarioEvidenceSelector.parser().parse("class Tests {void search(){mock.perform(get(\"/items\")"
                    + ".param(\"lastName\",\"" + value + "\")).andExpect(status().isOk());}}")
                    .getResult().orElseThrow().findFirst(com.github.javaparser.ast.body.MethodDeclaration.class).orElseThrow();
            assertEquals(!value.isBlank(), ScenarioEvidenceSelector.qualifies(test,"FIND",List.of("last-name-criteria")));
            assertEquals(value.equals(" Smith "), ScenarioEvidenceSelector.qualifies(test,"FIND",
                    List.of("last-name-criteria","normalized-input")));
        }
    }
    @Test void pagingQueryOnBoundRequestIsSupported() {
        var test = ScenarioEvidenceSelector.parser().parse("""
                class Tests {void browse(){mock.perform(get("/items?page=1")).andExpect(status().isOk());}}
                """).getResult().orElseThrow().findFirst(com.github.javaparser.ast.body.MethodDeclaration.class).orElseThrow();
        assertTrue(ScenarioEvidenceSelector.qualifies(test,"BROWSE",List.of("paged-results")));
    }
    private static com.fasterxml.jackson.databind.JsonNode observations(String... names) {
        var root = JSON.createObjectNode(); var array = root.putArray("observations");
        for (String name : names) array.addObject().put("observationRef", "obs-" + name)
                .put("testSourcePath", "OrderControllerTests.java").put("testMethod", name)
                .put("httpMethod", "POST").put("normalizedRouteTemplate", "/orders/new");
        return root;
    }
    private static com.fasterxml.jackson.databind.JsonNode handlers() throws Exception {
        return JSON.readTree("""
                {"handlers":[{"productionIdentity":"demo.OrderController#create",
                "httpMethods":["POST"],"normalizedRouteTemplate":"/orders/new"}]}
                """);
    }
}

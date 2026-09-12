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

package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RedirectEvidenceAssociationTests {
    @TempDir Path root;
    @Test void requiresExplicitRedirectUniqueHandlerAndIndependentSuccessfulGet() throws Exception {
        Path file = root.resolve("Controller.java");
        Files.writeString(file, "class Controller { String create(){return \"redirect:/items/{id}\";} void show(){} }");
        Files.writeString(root.resolve("Tests.java"), "class Tests { void showTest(){ mock.perform(get(\"/items/1\")).andExpect(status().isOk()); } }");
        var index = new SourceMethodIndex(root, List.of(file));
        var json = new ObjectMapper();
        var observations = json.readTree("""
                {"observations":[{"httpMethod":"GET","normalizedRouteTemplate":"/items/{id}",
                "testSourcePath":"Tests.java","testMethod":"showTest","observationRef":"get-1"}]}
                """);
        var handlers = json.readTree("""
                {"handlers":[{"httpMethods":["GET"],"normalizedRouteTemplate":"/items/{id}","productionIdentity":"Controller#show"},
                {"httpMethods":["GET"],"normalizedRouteTemplate":"/items/new","productionIdentity":"Controller#create"}]}
                """);
        var seed = new ScenarioEvidenceSelector.Seed("s", "Controller#create", "post-1", "CREATE", List.of("post-operation-view"));
        var result = RedirectEvidenceAssociation.select(List.of(seed), index, observations, handlers, root);
        assertEquals(1, result.size());
        assertEquals("Controller#show", result.get(0).productionIdentity());
        assertEquals("REDIRECT_TARGET_ASSOCIATION", result.get(0).role());
        assertTrue(result.get(0).observationRef().contains("get-1"));
        assertTrue(result.get(0).observationRef().contains("post-1"));
        Files.writeString(file, "class Controller { Errors errors; String create(){if(errors.hasErrors()) return \"redirect:/items/{id}\"; return \"view\";} void show(){} }");
        assertTrue(RedirectEvidenceAssociation.select(List.of(seed), new SourceMethodIndex(root,List.of(file)), observations, handlers, root).isEmpty());
        Files.writeString(file, "class Controller { String create(){java.util.function.Supplier<String> later=()->{return \"redirect:/items/{id}\";};return \"view\";} void show(){} }");
        assertTrue(RedirectEvidenceAssociation.select(List.of(seed), new SourceMethodIndex(root,List.of(file)), observations, handlers, root).isEmpty());
        var wrong = observations.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) wrong.get("observations").get(0))
                .put("normalizedRouteTemplate", "/items/new");
        assertTrue(RedirectEvidenceAssociation.select(List.of(seed), index, wrong, handlers, root).isEmpty());
        assertTrue(RedirectEvidenceAssociation.select(List.of(seed), index,
                json.readTree("{\"observations\":[]}"), handlers, root).isEmpty());
        Files.writeString(file, "class Controller { String create(){return \"redirect:/items/\" + \"archive/all\";} void show(){} }");
        var multiSegment = new SourceMethodIndex(root, List.of(file));
        assertTrue(RedirectEvidenceAssociation.select(List.of(seed), multiSegment, observations, handlers, root).isEmpty());
        ((com.fasterxml.jackson.databind.node.ArrayNode) handlers.get("handlers")).add(handlers.get("handlers").get(0).deepCopy());
        assertTrue(RedirectEvidenceAssociation.select(List.of(seed), index, observations, handlers, root).isEmpty());
    }
}

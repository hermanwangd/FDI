package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CalibrationProducerTests {
    @TempDir Path root;
    @Test void baselineAndExpandedShareBindingButOnlyExpansionAddsCalls() throws Exception {
        Path file = root.resolve("Service.java");
        Files.writeString(file, "package demo; class Service { void run() { helper(); } void helper() {} }");
        var index = new SourceMethodIndex(root, List.of(file));
        var seeds = new ObjectMapper().readTree("""
                {"scenarios":[{"scenarioId":"S1","components":[{
                "productionIdentity":"demo.Service#run","role":"ROUTE_HANDLER",
                "evidenceStrength":"EXACT_ROUTE_HANDLER","evidenceRefs":["http-1"]}]},
                {"scenarioId":"S2","components":[]}]}
                """);
        var binding = new CalibrationProducer.Binding("a".repeat(40), List.of("src/main/java"),
                List.of("src/test/java"), "b".repeat(64), "c".repeat(64));
        var base = CalibrationProducer.produce(binding, seeds, index, false);
        var improved = CalibrationProducer.produce(binding, seeds, index, true);
        assertEquals(base.binding(), improved.binding());
        assertEquals(1, base.proposals().methods().size());
        assertEquals(2, improved.proposals().methods().size());
        assertEquals(1, improved.proposals().edges().size());
        assertEquals(List.of("S2"), improved.proposals().unresolvedScenarios());
        assertEquals(improved, CalibrationProducer.produce(binding, seeds, index, true));
    }

    @Test void graphDiagnosticsCannotBecomeSeeds() throws Exception {
        Path file = root.resolve("Service.java");
        Files.writeString(file, "package demo; class Service { void run() {} }");
        var seeds = new ObjectMapper().readTree("""
                {"scenarios":[{"scenarioId":"S1","components":[{
                "productionIdentity":"demo.Service#run","role":"GRAPH_TRACE_SUPPORT",
                "evidenceStrength":"GRAPH_DIAGNOSTIC","evidenceRefs":["graph-1"]}]}]}
                """);
        var binding = new CalibrationProducer.Binding("a".repeat(40), List.of("src/main/java"),
                List.of("src/test/java"), "b".repeat(64), "c".repeat(64));
        var result = CalibrationProducer.produce(binding, seeds, new SourceMethodIndex(root, List.of(file)), true);
        assertTrue(result.proposals().methods().isEmpty());
        assertEquals(List.of("S1"), result.proposals().unresolvedScenarios());
    }
}

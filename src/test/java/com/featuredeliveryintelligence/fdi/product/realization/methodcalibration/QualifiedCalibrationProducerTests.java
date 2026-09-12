package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class QualifiedCalibrationProducerTests {
    @TempDir Path root;
    @Test void qualifiedSeedsExpandWithAuditableReferencesAndKeepUnresolved() throws Exception {
        Path file = root.resolve("Controller.java");
        Files.writeString(file, "package demo; class Controller { void create() { save(); } void save() {} }");
        var index = new SourceMethodIndex(root, List.of(file));
        var binding = new CalibrationProducer.Binding("a".repeat(40), List.of("src/main/java"),
                List.of("src/test/java"), "b".repeat(64), "c".repeat(64));
        var seeds = List.of(new ScenarioEvidenceSelector.Seed("s1", "demo.Controller#create", "obs-1", "CREATE", List.of()),
                new ScenarioEvidenceSelector.Seed("s1", "demo.Controller#create", "obs-2", "CREATE", List.of()));
        var result = QualifiedCalibrationProducer.produce(binding, List.of("s1", "s2"), seeds, index);
        assertEquals(List.of("s2"), result.proposals().unresolvedScenarios());
        assertEquals(2, result.proposals().methods().stream().map(CalibrationProducer.Claim::pair).distinct().count());
        assertTrue(result.proposals().methods().stream().anyMatch(c -> c.evidenceRef().contains("obs-1")));
        assertTrue(result.proposals().methods().stream().anyMatch(c -> c.evidenceRef().contains("obs-2")));
        assertFalse(result.proposals().edges().isEmpty());
        assertEquals(result, QualifiedCalibrationProducer.produce(binding, List.of("s1", "s2"), seeds, index));
    }
}

package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CandidateTraceTests {
    @TempDir Path root;
    private final CalibrationProducer.Binding binding = new CalibrationProducer.Binding("a".repeat(40),
            List.of("src/main/java"), List.of("src/test/java"), "b".repeat(64), "c".repeat(64));

    @Test void tracePreservesProposalsAndDistinguishesFiltersFromUnknownTargets() throws Exception {
        var index = index("""
                package demo;
                class Controller {
                    String name;
                    void create() { save(); getName(); unknown(); }
                    void save() {}
                    String getName() { return name; }
                }
                """);
        var seeds = seeds();
        var original = QualifiedCalibrationProducer.produce(binding, List.of("s1", "s2"), seeds, index);
        var trace = new CandidateTrace(binding);
        var traced = QualifiedCalibrationProducer.produce(binding, List.of("s1", "s2"), seeds, index, trace);
        assertEquals(original, traced);
        assertTrue(trace.events().stream().anyMatch(e -> e.reason().equals("TRIVIAL_ACCESSOR")
                && e.target().signature().equals("demo.Controller#getName()")));
        assertTrue(trace.events().stream().anyMatch(e -> e.reason().equals("TARGET_NOT_UNIQUELY_RESOLVED")
                && e.target() == null));
        assertTrue(trace.events().stream().anyMatch(e -> e.stage().equals("RETAINED")
                && e.target().signature().equals("demo.Controller#save()")));
        var again = new CandidateTrace(binding);
        QualifiedCalibrationProducer.produce(binding, List.of("s1", "s2"), seeds, index, again);
        assertEquals(trace.events(), again.events());
    }

    @Test void depthBoundaryRecordsUnexpandedFrontierNotAnInventedMissingTarget() throws Exception {
        var index = index("package demo; class Controller { void create(){a();} void a(){b();} "
                + "void b(){c();} void c(){d();} void d(){} }");
        var trace = new CandidateTrace(binding);
        var result = QualifiedCalibrationProducer.produce(binding, List.of("s1"), seeds(), index, trace);
        assertFalse(result.proposals().methods().stream().anyMatch(c -> c.pair().method().signature().endsWith("#d()")));
        var boundary = trace.events().stream().filter(e -> e.stage().equals("DEPTH_FRONTIER")).toList();
        assertEquals(1, boundary.size());
        assertEquals("demo.Controller#c()", boundary.get(0).from().signature());
        assertNull(boundary.get(0).target());
        assertEquals(3, boundary.get(0).depth());
    }

    @Test void missingSeedIsNotLabeledAsAResolvedCandidate() throws Exception {
        var index = index("package demo; class Controller {} ");
        var trace = new CandidateTrace(binding);
        QualifiedCalibrationProducer.produce(binding, List.of("s1"), seeds(), index, trace);
        assertEquals(1, trace.events().size());
        assertEquals("SEED_NOT_UNIQUELY_RESOLVED", trace.events().get(0).reason());
        assertNull(trace.events().get(0).target());
    }

    private SourceMethodIndex index(String source) throws Exception {
        Path path = root.resolve("Controller.java");
        Files.writeString(path, source);
        return new SourceMethodIndex(root, List.of(path));
    }
    @Test void exceedingMethodBudgetFailsRatherThanReturningTruncatedProposals() throws Exception {
        StringBuilder calls = new StringBuilder();
        StringBuilder methods = new StringBuilder();
        for (int i = 0; i < 65; i++) {
            calls.append("m").append(i).append("();");
            methods.append("void m").append(i).append("(){}");
        }
        var index = index("package demo; class Controller { void create(){" + calls + "}" + methods + "}");
        var trace = new CandidateTrace(binding);
        assertEquals("SCENARIO_EXPANSION_LIMIT", assertThrows(IllegalArgumentException.class,
                () -> QualifiedCalibrationProducer.produce(binding, List.of("s1"), seeds(), index, trace)).getMessage());
        assertTrue(trace.events().stream().noneMatch(e -> e.stage().equals("DEPTH_FRONTIER")));
    }

    @Test void traceIsBoundedAndSnapshotsAreImmutable() {
        var trace = new CandidateTrace(binding);
        var snapshot = trace.events();
        for (int i = 0; i < CandidateTrace.MAX_EVENTS; i++)
            trace.add("s1", "seed", 0, "UNRESOLVED", "SEED_NOT_UNIQUELY_RESOLVED", null, null, "method");
        assertTrue(snapshot.isEmpty());
        assertEquals("TRACE_EVENT_LIMIT", assertThrows(IllegalArgumentException.class,
                () -> trace.add("s1", "seed", 0, "UNRESOLVED", "reason", null, null, "method")).getMessage());
        assertThrows(UnsupportedOperationException.class, () -> trace.events().clear());
    }
    private List<ScenarioEvidenceSelector.Seed> seeds() {
        return List.of(new ScenarioEvidenceSelector.Seed("s1", "demo.Controller#create", "obs-1", "CREATE", List.of()));
    }
}

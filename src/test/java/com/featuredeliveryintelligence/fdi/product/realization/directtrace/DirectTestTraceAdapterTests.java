package com.featuredeliveryintelligence.fdi.product.realization.directtrace;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.Granularity;
import com.featuredeliveryintelligence.fdi.reverse.evidence.EvidenceChannelRecord;
import com.featuredeliveryintelligence.fdi.reverse.input.testbehavior.TestBehaviorEvidenceAdapter;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DirectTestTraceAdapterTests {

    private static final Path ROOT = Path.of("").toAbsolutePath();

    @Test
    void acceptedExtractorEvidenceProducesAllDirectObservationsAndGapsDeterministically() {
        EvidenceChannelRecord input = TestBehaviorEvidenceAdapter.loadAccepted(ROOT);

        DirectTestTrace first = DirectTestTraceAdapter.adapt(ROOT, input);
        DirectTestTrace second = DirectTestTraceAdapter.adapt(ROOT, input);

        assertEquals(first, second);
        assertEquals("818c4136ea971c21674525f9053de0d9c7ad8cfe", first.sourceRevision());
        assertEquals(144, first.directEvidence().size());
        assertEquals(891, first.unresolvedGaps().size());
        assertEquals(29, first.uniqueProductionComponents().size());
        assertEquals(139, first.uniqueObservationLocations().size());
        assertTrue(first.directEvidence().stream().allMatch(e ->
                e.productionSymbol().granularity() == Granularity.METHOD
                        && e.productionSymbol().sourcePath().startsWith("src/main/java/")
                        && !e.productionSymbol().sourcePath().contains("/test/")));
        assertTrue(first.directEvidence().stream().allMatch(e ->
                e.evidenceRef().startsWith("direct-test-reference:")
                        && e.observationRef().startsWith("/test_files/")));
        for (int index = 0; index < first.directEvidence().size(); index++) {
            assertEquals(first.directEvidence().get(index).evidenceRef(),
                    first.seeds().get(index).directEvidenceRef());
            assertEquals(first.directEvidence().get(index).productionSymbol(),
                    first.seeds().get(index).productionSeed());
        }
        assertTrue(first.unresolvedGaps().stream().allMatch(g ->
                g.observationRef().startsWith("/test_files/")
                        && g.sourceLocation().repositoryRelativePath().startsWith("src/test/")));
    }

    @Test
    void constructorObservationUsesMethodGranularityAndStableQualifiedIdentity() {
        DirectTestTrace trace = DirectTestTraceAdapter.adapt(ROOT, TestBehaviorEvidenceAdapter.loadAccepted(ROOT));

        var constructor = trace.directEvidence().stream()
                .filter(e -> e.productionSymbol().qualifiedSymbol().endsWith("#<init>"))
                .findFirst().orElseThrow();

        assertEquals("src/main/java/org/springframework/samples/petclinic/owner/Visit.java",
                constructor.productionSymbol().sourcePath());
        assertEquals(Granularity.METHOD, constructor.productionSymbol().granularity());
        assertEquals("org.springframework.samples.petclinic.owner.Visit#<init>",
                constructor.productionSymbol().qualifiedSymbol());
    }

    @Test
    void rejectsRevisionMismatchAndTestNodeLeakage() {
        EvidenceChannelRecord accepted = TestBehaviorEvidenceAdapter.loadAccepted(ROOT);
        JsonNode observations = accepted.observations();

        ((com.fasterxml.jackson.databind.node.ObjectNode) observations)
                .put("canonical_revision", "0000000000000000000000000000000000000000");
        EvidenceChannelRecord mismatched = copyWith(accepted, observations);
        assertThrows(RuntimeContractException.class, () -> DirectTestTraceAdapter.adapt(ROOT, mismatched));

        observations = accepted.observations();
        var symbol = firstResolvedSymbol(observations);
        symbol.put("declaring_type", "src.test.java.EscapedTest");
        EvidenceChannelRecord leaked = copyWith(accepted, observations);
        assertThrows(RuntimeContractException.class, () -> DirectTestTraceAdapter.adapt(ROOT, leaked));
    }

    @Test
    void rejectsEvaluatorPathAndGoldVocabularyMutations() {
        EvidenceChannelRecord accepted = TestBehaviorEvidenceAdapter.loadAccepted(ROOT);
        EvidenceChannelRecord evaluatorPath = new EvidenceChannelRecord(accepted.channel(), accepted.repositoryId(),
                accepted.canonicalRevision(), "validation/evaluator/gold-mapping.json", accepted.inputSha256(),
                accepted.schemaVersion(), accepted.providerId(), accepted.provenance(), accepted.observations());
        assertThrows(RuntimeContractException.class, () -> DirectTestTraceAdapter.adapt(ROOT, evaluatorPath));

        JsonNode observations = accepted.observations();
        var gap = (com.fasterxml.jackson.databind.node.ObjectNode) observations.path("test_files").get(0)
                .path("test_methods").get(0).path("unresolved_references").get(0);
        gap.put("reference_text", "evaluator gold mapping");
        assertThrows(RuntimeContractException.class,
                () -> DirectTestTraceAdapter.adapt(ROOT, copyWith(accepted, observations)));
    }

    @Test
    void resolvesNestedTypesFromTheDeclaringSourceInsteadOfGuessing(@TempDir Path temporaryRoot) throws Exception {
        Path sourceRoot = temporaryRoot.resolve("src/main/java");
        Path outer = sourceRoot.resolve("example/Outer.java");
        Files.createDirectories(outer.getParent());
        Files.writeString(outer, "package example; public class Outer { static class Inner {} }");

        assertEquals("src/main/java/example/Outer.java",
                ProductionSourcePathResolver.from(sourceRoot).resolve("example.Outer.Inner"));
        assertThrows(RuntimeContractException.class,
                () -> ProductionSourcePathResolver.from(sourceRoot).resolve("example.Outer.Missing"));
    }

    @Test
    void derivedCollectionsCannotBeSuppliedOrMadeStale() {
        DirectTestTrace trace = DirectTestTraceAdapter.adapt(ROOT, TestBehaviorEvidenceAdapter.loadAccepted(ROOT));

        assertThrows(UnsupportedOperationException.class, () -> trace.resolvedObservations().clear());
        assertEquals(trace.resolvedObservations().stream().map(ResolvedDirectObservation::directEvidence).toList(),
                trace.directEvidence());
        assertEquals(trace.directEvidence().stream().map(e -> e.productionSymbol()).distinct().toList(),
                trace.uniqueProductionComponents());
    }

    private static com.fasterxml.jackson.databind.node.ObjectNode firstResolvedSymbol(JsonNode observations) {
        for (JsonNode file : observations.path("test_files")) {
            for (JsonNode method : file.path("test_methods")) {
                for (String group : java.util.List.of("fixtures", "actions", "assertions")) {
                    for (JsonNode observation : method.path(group)) {
                        if (observation.path("referenced_symbol").isObject()) {
                            return (com.fasterxml.jackson.databind.node.ObjectNode) observation.path("referenced_symbol");
                        }
                    }
                }
            }
        }
        throw new AssertionError("accepted fixture has no resolved symbol");
    }

    private static EvidenceChannelRecord copyWith(EvidenceChannelRecord source, JsonNode observations) {
        return new EvidenceChannelRecord(source.channel(), source.repositoryId(), source.canonicalRevision(),
                source.inputPath(), source.inputSha256(), source.schemaVersion(), source.providerId(),
                source.provenance(), observations);
    }
}

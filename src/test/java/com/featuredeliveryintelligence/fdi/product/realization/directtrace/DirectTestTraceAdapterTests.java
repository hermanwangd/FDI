package com.featuredeliveryintelligence.fdi.product.realization.directtrace;

import com.featuredeliveryintelligence.fdi.product.realization.ScenarioMappingContractV04.Granularity;
import com.featuredeliveryintelligence.fdi.reverse.evidence.EvidenceChannelRecord;
import com.featuredeliveryintelligence.fdi.reverse.input.testbehavior.TestBehaviorEvidenceAdapter;
import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DirectTestTraceAdapterTests {

    private static final Path ROOT = Path.of("").toAbsolutePath();

    @Test
    void acceptedExtractorEvidenceProducesAllDirectObservationsAndGapsDeterministically() {
        EvidenceChannelRecord input = TestBehaviorEvidenceAdapter.loadAccepted(ROOT);

        DirectTestTrace first = DirectTestTraceAdapter.adapt(input);
        DirectTestTrace second = DirectTestTraceAdapter.adapt(input);

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
        DirectTestTrace trace = DirectTestTraceAdapter.adapt(TestBehaviorEvidenceAdapter.loadAccepted(ROOT));

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
        assertThrows(RuntimeContractException.class, () -> DirectTestTraceAdapter.adapt(mismatched));

        observations = accepted.observations();
        var symbol = firstResolvedSymbol(observations);
        symbol.put("declaring_type", "src.test.java.EscapedTest");
        EvidenceChannelRecord leaked = copyWith(accepted, observations);
        assertThrows(RuntimeContractException.class, () -> DirectTestTraceAdapter.adapt(leaked));
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

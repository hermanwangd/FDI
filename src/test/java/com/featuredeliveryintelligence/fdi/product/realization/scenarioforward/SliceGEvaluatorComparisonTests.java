package com.featuredeliveryintelligence.fdi.product.realization.scenarioforward;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SliceGEvaluatorComparisonTests {
    @TempDir Path temp;

    @Test void exactByteSealingPrecedesAnyEvaluatorAccess() throws Exception {
        copyTree(Path.of("."), temp);
        Path proposal = temp.resolve(SliceGEvaluatorComparison.PROPOSAL_PATH);
        Files.writeString(proposal, "\n", StandardOpenOption.APPEND);
        AtomicInteger accesses = new AtomicInteger();

        assertThrows(RuntimeContractException.class,
                () -> SliceGEvaluatorComparison.compare(temp, root -> {
                    accesses.incrementAndGet();
                    return SliceGEvaluatorComparison.loadEvaluatorTruth(root);
                }));
        assertEquals(0, accesses.get());
    }

    @Test void reportsEmptyProposalWithoutInventingFormalCredit() {
        var report = SliceGEvaluatorComparison.compare(Path.of("."),
                SliceGEvaluatorComparison::loadEvaluatorTruth);

        assertEquals("EVALUATOR_ONLY", report.authority());
        assertFalse(report.semanticPublicationAllowed());
        assertEquals(10, report.counts().scenarios());
        assertEquals(0, report.counts().mappingProposals());
        assertEquals(10, report.counts().unresolvedScenarios());
        assertEquals(0, report.counts().proposedComponents());
        assertEquals(24, report.exactComponent().expected());
        assertEquals(0, report.exactComponent().matched());
        assertEquals(0.0, report.exactComponent().recall());
        assertFalse(report.exactComponent().precisionDefined());
        assertNull(report.exactComponent().precision());
        assertEquals(0, report.directSymbolRecall().matched());
        assertEquals(0, report.expandedChainCoverage().matched());
        assertEquals(0, report.scenarioTraceCoverage().matched());
        assertEquals(891, report.unresolvedReferences().unresolved());
        assertEquals(1035, report.unresolvedReferences().total());
        assertEquals(0, report.traceCounts().direct());
        assertEquals(0, report.traceCounts().inferred());
        assertEquals(17, report.previousBaseline().graphNodeCovered());
        assertEquals(24, report.previousBaseline().graphNodeExpected());
    }

    @Test void evaluatorSealOrGoldMutationFailsClosed() throws Exception {
        for (String path : new String[]{SliceGEvaluatorComparison.GOLD_PATH, SliceGEvaluatorComparison.GOLD_SEAL_PATH}) {
            Path root = temp.resolve(path.endsWith("gold-mappings.json") ? "gold" : "seal");
            copyTree(Path.of("."), root);
            Files.writeString(root.resolve(path), "\n", StandardOpenOption.APPEND);
            assertThrows(RuntimeContractException.class,
                    () -> SliceGEvaluatorComparison.compare(root, SliceGEvaluatorComparison::loadEvaluatorTruth));
        }
    }

    @Test void reportSerializationIsDeterministic() throws Exception {
        var report = SliceGEvaluatorComparison.compare(Path.of("."), SliceGEvaluatorComparison::loadEvaluatorTruth);
        assertArrayEquals(SliceGEvaluatorComparison.toJson(report), SliceGEvaluatorComparison.toJson(report));
    }

    @Test void committedArtifactsReproduceByteExactly() throws Exception {
        Path output = temp.resolve("generated");
        SliceGEvaluatorComparisonGenerator.generate(Path.of("."), output);
        assertArrayEquals(Files.readAllBytes(Path.of(SliceGEvaluatorComparisonGenerator.REPORT_PATH)),
                Files.readAllBytes(output.resolve(SliceGEvaluatorComparisonGenerator.REPORT_PATH)));
        assertArrayEquals(Files.readAllBytes(Path.of(SliceGEvaluatorComparisonGenerator.EVIDENCE_PATH)),
                Files.readAllBytes(output.resolve(SliceGEvaluatorComparisonGenerator.EVIDENCE_PATH)));
    }

    private static void copyTree(Path source, Path target) throws Exception {
        for (String path : SliceGEvaluatorComparison.preEvaluatorPathsForTest()) {
            Path from = source.resolve(path), to = target.resolve(path);
            Files.createDirectories(to.getParent()); Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
        for (String path : new String[]{SliceGEvaluatorComparison.GOLD_PATH, SliceGEvaluatorComparison.GOLD_SEAL_PATH}) {
            Path from = source.resolve(path), to = target.resolve(path);
            Files.createDirectories(to.getParent()); Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

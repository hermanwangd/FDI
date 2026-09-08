package com.featuredeliveryintelligence.fdi.product.realization.evaluation;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProviderNeutralEvaluatorTruthTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    @TempDir Path temp;

    @Test void migratesExactlyTwentyFourComponentsAndPreservesProvenanceAndDuplicates() {
        var truth = ProviderNeutralEvaluatorTruth.load(Path.of("."));
        assertEquals(24, truth.expectedComponents().size());
        assertEquals(24, truth.mappings().stream().mapToInt(m -> m.expectedComponents().size()).sum());
        assertTrue(truth.expectedComponents().stream().allMatch(c -> c.componentRef().startsWith("graph-node:")
                && !c.graphNodeId().isBlank() && !c.sourcePath().isBlank() && !c.sourceLocation().isBlank()));
        assertEquals(2, truth.expectedComponents().stream()
                .filter(c -> c.identity().qualifiedSymbol().equals("org.springframework.samples.petclinic.owner.Owner"))
                .count());
    }

    @Test void normalizesMethodsWithHashAndTypesWithQualifiedName() {
        var truth = ProviderNeutralEvaluatorTruth.load(Path.of("."));
        var method = truth.expectedComponents().stream().filter(c -> c.graphNodeId().contains("processfindform")).findFirst().orElseThrow();
        var type = truth.expectedComponents().stream().filter(c -> c.graphNodeId().equals("owner_owner")).findFirst().orElseThrow();
        assertEquals("METHOD", method.identity().granularity());
        assertEquals("org.springframework.samples.petclinic.owner.OwnerController#processFindForm", method.identity().qualifiedSymbol());
        assertEquals("TYPE", type.identity().granularity());
        assertEquals("org.springframework.samples.petclinic.owner.Owner", type.identity().qualifiedSymbol());
    }

    @Test void identityRequiresCanonicalRevisionAndRepositoryRelativeProductionPath() {
        assertThrows(RuntimeContractException.class, () -> new ProviderNeutralEvaluatorTruth.Identity("bad", "src/main/java/A.java", "TYPE", "A"));
        for (String path : List.of("/tmp/A.java", "../A.java", "src\\main\\A.java", "src/test/java/A.java"))
            assertThrows(RuntimeContractException.class, () -> new ProviderNeutralEvaluatorTruth.Identity("a".repeat(40), path, "TYPE", "A"));
    }

    @Test void generatorReproducesCommittedArtifactsByteExactlyAndLeavesLegacyUnchanged() throws Exception {
        byte[] legacyGold = Files.readAllBytes(Path.of(ProviderNeutralEvaluatorTruth.LEGACY_GOLD_PATH));
        byte[] legacySeal = Files.readAllBytes(Path.of(ProviderNeutralEvaluatorTruth.LEGACY_SEAL_PATH));
        ProviderNeutralEvaluatorTruthGenerator.generate(Path.of("."), temp);
        assertArrayEquals(Files.readAllBytes(Path.of(ProviderNeutralEvaluatorTruth.GOLD_PATH)), Files.readAllBytes(temp.resolve(ProviderNeutralEvaluatorTruth.GOLD_PATH)));
        assertArrayEquals(Files.readAllBytes(Path.of(ProviderNeutralEvaluatorTruth.SEAL_PATH)), Files.readAllBytes(temp.resolve(ProviderNeutralEvaluatorTruth.SEAL_PATH)));
        assertArrayEquals(legacyGold, Files.readAllBytes(Path.of(ProviderNeutralEvaluatorTruth.LEGACY_GOLD_PATH)));
        assertArrayEquals(legacySeal, Files.readAllBytes(Path.of(ProviderNeutralEvaluatorTruth.LEGACY_SEAL_PATH)));
    }

    @Test void failsClosedForEveryBoundInputAndInvariant() throws Exception {
        for (String path : List.of(ProviderNeutralEvaluatorTruth.GOLD_PATH, ProviderNeutralEvaluatorTruth.SEAL_PATH,
                ProviderNeutralEvaluatorTruth.LEGACY_GOLD_PATH, ProviderNeutralEvaluatorTruth.LEGACY_SEAL_PATH,
                ProviderNeutralEvaluatorTruth.GRAPH_PATH)) {
            Path root = temp.resolve(path.replace('/', '_'));
            copyInputs(root);
            Files.writeString(root.resolve(path), " ", StandardOpenOption.APPEND);
            assertThrows(RuntimeContractException.class, () -> ProviderNeutralEvaluatorTruth.load(root), path);
        }
        Path root = temp.resolve("invariants"); copyInputs(root);
        JsonNode gold = JSON.readTree(root.resolve(ProviderNeutralEvaluatorTruth.GOLD_PATH).toFile());
        ((com.fasterxml.jackson.databind.node.ObjectNode) gold).put("status", "DRAFT");
        Files.write(root.resolve(ProviderNeutralEvaluatorTruth.GOLD_PATH), JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(gold));
        assertThrows(RuntimeContractException.class, () -> ProviderNeutralEvaluatorTruth.load(root));
    }

    private static void copyInputs(Path root) throws Exception {
        for (String path : List.of(ProviderNeutralEvaluatorTruth.GOLD_PATH, ProviderNeutralEvaluatorTruth.SEAL_PATH,
                ProviderNeutralEvaluatorTruth.LEGACY_GOLD_PATH, ProviderNeutralEvaluatorTruth.LEGACY_SEAL_PATH,
                ProviderNeutralEvaluatorTruth.GRAPH_PATH)) {
            Path to = root.resolve(path); Files.createDirectories(to.getParent()); Files.copy(Path.of(path), to);
        }
    }
}

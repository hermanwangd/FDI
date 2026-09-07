package com.featuredeliveryintelligence.fdi.reverse.evaluation;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Seal and isolation invariants for the frozen evaluator surface. Positive
 * cases read the real sealed Petclinic evaluator truth; negative cases replay
 * tampered copies in a temporary directory.
 */
class ReverseEvaluatorTruthTests {

    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path temp;

    @Test
    void sealedEvaluatorSurfaceLoadsWithExpectedIdentity() {
        ReverseEvaluatorTruth truth = ReverseEvaluatorTruth.load(Path.of("").toAbsolutePath());
        assertEquals("pkb001-petclinic-818c413-evaluator-v1", truth.mappingSetId());
        assertEquals("pkb001-petclinic-818c413-ground-truth-v1", truth.sealId());
        assertEquals("818c4136ea971c21674525f9053de0d9c7ad8cfe", truth.sourceCommitSha());
        assertEquals("4d22799e4d7597e0bbc302c9db3cd0510f70cc946cb5de5909ded9c4b1b112d1",
                truth.goldSha256());
        assertEquals(10, truth.capabilities().size());
        int components = truth.capabilities().stream()
                .mapToInt(capability -> capability.components().size())
                .sum();
        assertEquals(24, components);
        assertEquals("OwnerController",
                truth.capabilities().get(0).components().get(0).containingType());
    }

    @Test
    void tamperedGoldFailsClosedAgainstTheSeal() throws Exception {
        Path root = copyEvaluatorSurface();
        Path gold = root.resolve(ReverseEvaluatorTruth.GOLD_PATH);
        ObjectNode document = (ObjectNode) JSON.readTree(Files.readAllBytes(gold));
        document.put("status", "EVALUATOR_ONLY_FROZEN");
        Files.write(gold, JSON.writeValueAsBytes(document));

        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseEvaluatorTruth.load(root));
        assertEquals(ReverseFailure.DIGEST_MISMATCH, failure.failure());
    }

    @Test
    void unsealedSurfaceFailsClosed() throws Exception {
        Path root = copyEvaluatorSurface();
        Path seal = root.resolve(ReverseEvaluatorTruth.SEAL_PATH);
        ObjectNode document = (ObjectNode) JSON.readTree(Files.readAllBytes(seal));
        document.put("status", "OPEN");
        Files.write(seal, JSON.writeValueAsBytes(document));

        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseEvaluatorTruth.load(root));
        assertEquals(ReverseFailure.EVALUATOR_LEAKAGE, failure.failure());
    }

    @Test
    void unverifiedIsolationFailsClosed() throws Exception {
        Path root = copyEvaluatorSurface();
        Path seal = root.resolve(ReverseEvaluatorTruth.SEAL_PATH);
        ObjectNode document = (ObjectNode) JSON.readTree(Files.readAllBytes(seal));
        document.put("isolation_status", "UNVERIFIED");
        Files.write(seal, JSON.writeValueAsBytes(document));

        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseEvaluatorTruth.load(root));
        assertEquals(ReverseFailure.EVALUATOR_LEAKAGE, failure.failure());
    }

    @Test
    void generationAccessAllowedFailsClosed() throws Exception {
        Path root = copyEvaluatorSurface();
        Path seal = root.resolve(ReverseEvaluatorTruth.SEAL_PATH);
        ObjectNode document = (ObjectNode) JSON.readTree(Files.readAllBytes(seal));
        ((ObjectNode) document.path("isolation_controls")).put("generation_access", "ALLOWED");
        Files.write(seal, JSON.writeValueAsBytes(document));

        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseEvaluatorTruth.load(root));
        assertEquals(ReverseFailure.EVALUATOR_LEAKAGE, failure.failure());
    }

    @Test
    void mappingSetDisagreementFailsClosed() throws Exception {
        Path root = copyEvaluatorSurface();
        Path gold = root.resolve(ReverseEvaluatorTruth.GOLD_PATH);
        ObjectNode document = (ObjectNode) JSON.readTree(Files.readAllBytes(gold));
        document.put("mapping_set_id", "pkb001-petclinic-818c413-evaluator-v2");
        Files.write(gold, JSON.writeValueAsBytes(document));

        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseEvaluatorTruth.load(root));
        assertEquals(ReverseFailure.DIGEST_MISMATCH, failure.failure());
    }

    @Test
    void missingGoldFileFailsClosed() throws Exception {
        Path root = copyEvaluatorSurface();
        Files.delete(root.resolve(ReverseEvaluatorTruth.GOLD_PATH));

        ReverseContractException failure = assertThrows(ReverseContractException.class,
                () -> ReverseEvaluatorTruth.load(root));
        assertEquals(ReverseFailure.MISSING_INPUT, failure.failure());
    }

    @Test
    void wordingTokensAreMechanical() {
        assertEquals(List.of("find", "owner"),
                ReverseProposalComparator.wordingTokens("Find Owners"));
        assertEquals(List.of("maintain", "owner", "detail"),
                ReverseProposalComparator.wordingTokens("Maintain Owner Details"));
        assertEquals(List.of("specialty"),
                ReverseProposalComparator.wordingTokens("specialties"));
        assertEquals(List.of("browse", "veterinarian"),
                ReverseProposalComparator.wordingTokens("Browse Veterinarians"));
        assertTrue(ReverseProposalComparator.wordingTokens("").isEmpty());
    }

    /** Copies the sealed evaluator surface into the temporary directory, preserving relative paths. */
    private Path copyEvaluatorSurface() throws Exception {
        Path root = temp.resolve("repo");
        for (String relative : List.of(ReverseEvaluatorTruth.GOLD_PATH, ReverseEvaluatorTruth.SEAL_PATH)) {
            Path source = Path.of("").toAbsolutePath().resolve(relative);
            Path target = root.resolve(relative);
            Files.createDirectories(target.getParent());
            Files.copy(source, target);
        }
        return root;
    }
}

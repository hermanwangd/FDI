package com.featuredeliveryintelligence.fdi.product.realization.methodpair;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static com.featuredeliveryintelligence.fdi.product.realization.methodpair.MethodPairData.*;
import static com.featuredeliveryintelligence.fdi.product.realization.methodpair.MethodPairScorerTests.*;
import static org.assertj.core.api.Assertions.*;

class MethodPairComparisonRunnerTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    @TempDir Path root;

    @Test
    void comparesBoundInputsDeterministicallyWithoutClaimingExperimentalGo() throws Exception {
        Fixture f = fixture();
        f.run();
        var report = JSON.readTree(f.output.toFile());
        assertThat(report.at("/improved/truePositives").asInt()).isEqualTo(1);
        assertThat(report.at("/baseline/falseNegatives").asInt()).isEqualTo(1);
        assertThat(report.at("/baseline/precision").isNull()).isTrue();
        assertThat(report.path("experimentDecision").asText()).isEqualTo("NOT_RUN");
        assertThat(report.path("readiness").asText()).isEqualTo("SCORING_MECHANICS_ONLY");
        assertThat(report.path("manifestSha256").asText()).isEqualTo(f.pin());
        Path second = root.resolve("second.json");
        new MethodPairComparisonRunner().compare(f.manifest, f.pin(), second);
        assertThat(Files.readAllBytes(second)).isEqualTo(Files.readAllBytes(f.output));
    }

    @Test
    void refusesOverwritingEvenAnIdenticalResult() throws Exception {
        Fixture f = fixture();
        f.run();
        byte[] before = Files.readAllBytes(f.output);
        assertThatThrownBy(f::run).isInstanceOf(java.nio.file.FileAlreadyExistsException.class);
        assertThat(Files.readAllBytes(f.output)).isEqualTo(before);
    }

    @Test
    void refusesUnpinnedOrChangedManifestAndChangedProducerBytes() throws Exception {
        Fixture f = fixture();
        assertThatThrownBy(() -> new MethodPairComparisonRunner().compare(f.manifest, "0".repeat(64), f.output))
                .isInstanceOf(IllegalArgumentException.class);
        Files.writeString(root.resolve("improved.json"), "{}");
        assertThatThrownBy(f::run).isInstanceOf(IllegalArgumentException.class);
        assertThat(f.output).doesNotExist();
    }

    @Test
    void refusesMismatchedProducerBindingAndProofCandidateDigest() throws Exception {
        Fixture f = fixture();
        ObjectNode improved = (ObjectNode) JSON.readTree(root.resolve("improved.json").toFile());
        ((ObjectNode) improved.get("binding")).put("sourceRevision", "c".repeat(40));
        f.replace("improved", improved);
        assertThatThrownBy(f::run).isInstanceOf(IllegalArgumentException.class);
        f = fixture();
        ObjectNode ledger = (ObjectNode) JSON.readTree(root.resolve("proofs.json").toFile());
        ((ObjectNode) ledger.get("improved")).put("proposalsSha256", DIGEST);
        f.replace("proofs", ledger);
        assertThatThrownBy(f::run).isInstanceOf(IllegalArgumentException.class);
        assertThat(f.output).doesNotExist();
    }

    @Test
    void refusesUnknownMissingNullDuplicateAndCoercedFields() throws Exception {
        for (String malformed : List.of("{}", "null",
                "{\"schemaVersion\":\"x\",\"schemaVersion\":\"y\"}")) {
            Fixture f = fixture();
            Files.writeString(f.manifest, malformed);
            assertThatThrownBy(f::run).isInstanceOf(IllegalArgumentException.class);
        }
        for (String field : List.of("producerProofValid", "selectedScenarios")) {
            Fixture f = fixture();
            ObjectNode manifest = (ObjectNode) JSON.readTree(f.manifest.toFile());
            manifest.put(field, true);
            Files.write(f.manifest, JSON.writeValueAsBytes(manifest));
            assertThatThrownBy(f::run).isInstanceOf(IllegalArgumentException.class);
        }
        Fixture f = fixture();
        ObjectNode truth = (ObjectNode) JSON.readTree(root.resolve("truth.json").toFile());
        truth.putArray("selectedScenarios").add(42);
        f.replace("truth", truth);
        assertThatThrownBy(f::run).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTraversalSymlinkAndOversizedInputs() throws Exception {
        for (String path : List.of("../outside.json", "/tmp/input.json", "./truth.json", "a/../truth.json")) {
            Fixture f = fixture();
            ObjectNode manifest = (ObjectNode) JSON.readTree(f.manifest.toFile());
            ((ObjectNode) manifest.get("truth")).put("path", path);
            Files.write(f.manifest, JSON.writeValueAsBytes(manifest));
            assertThatThrownBy(f::run).isInstanceOf(IllegalArgumentException.class);
        }
        Fixture f = fixture();
        Path link = root.resolve("linked.json");
        Files.createSymbolicLink(link, root.resolve("truth.json"));
        ObjectNode manifest = (ObjectNode) JSON.readTree(f.manifest.toFile());
        ((ObjectNode) manifest.get("truth")).put("path", "linked.json");
        Files.write(f.manifest, JSON.writeValueAsBytes(manifest));
        assertThatThrownBy(f::run).isInstanceOf(IllegalArgumentException.class);
        f = fixture();
        byte[] large = new byte[2 * 1024 * 1024 + 1];
        Files.write(root.resolve("truth.json"), large);
        f.refreshRef("truth");
        assertThatThrownBy(f::run).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsHoldoutAndInconsistentGoldInsteadOfProducingScores() throws Exception {
        Fixture f = fixture();
        ObjectNode manifest = (ObjectNode) JSON.readTree(f.manifest.toFile());
        manifest.put("datasetKind", "HOLDOUT");
        Files.write(f.manifest, JSON.writeValueAsBytes(manifest));
        assertThatThrownBy(f::run).isInstanceOf(IllegalArgumentException.class);
        f = fixture();
        Pair pair = new Pair("s1", A);
        f.replace("truth", truth(List.of("s1"), List.of(pair, pair), List.of()));
        assertThatThrownBy(f::run).isInstanceOf(IllegalArgumentException.class);
        f.replace("truth", truth(List.of("s1"), List.of(pair),
                List.of(new Chain("s1", List.of(B), List.of()))));
        assertThatThrownBy(f::run).isInstanceOf(IllegalArgumentException.class);
        assertThat(f.output).doesNotExist();
    }

    @Test
    void rejectsUnresolvedWithClaimsAndMalformedMethodSignatures() throws Exception {
        Fixture f = fixture();
        Pair pair = new Pair("s1", A);
        f.replace("improved", Map.of("binding", binding(), "proposals",
                proposals(List.of(claim(pair, "e")), List.of("s1"), List.of())));
        assertThatThrownBy(f::run).isInstanceOf(IllegalArgumentException.class);
        f = fixture();
        Method noOverloadIdentity = new Method(REVISION, A.path(), "demo.Service#first");
        f.replace("truth", truth(List.of("s1"), List.of(new Pair("s1", noOverloadIdentity)), List.of()));
        assertThatThrownBy(f::run).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void proofForAnotherEvidenceReferenceCannotMakeAClaimTruePositive() throws Exception {
        Fixture f = fixture();
        ObjectNode ledger = (ObjectNode) JSON.readTree(root.resolve("proofs.json").toFile());
        ((ObjectNode) ledger.at("/improved/methods/0")).put("evidenceRef", "other-proof");
        f.replace("proofs", ledger);
        f.run();
        var report = JSON.readTree(f.output.toFile());
        assertThat(report.at("/improved/truePositives").asInt()).isZero();
        assertThat(report.at("/improved/falsePositives").asInt()).isEqualTo(1);
        assertThat(report.at("/improved/falseNegatives").asInt()).isEqualTo(1);
    }

    @Test
    void numericEvidenceReferencesCannotBeSilentlyCoercedToStrings() throws Exception {
        Fixture f = fixture();
        ObjectNode improved = (ObjectNode) JSON.readTree(root.resolve("improved.json").toFile());
        ((ObjectNode) improved.at("/proposals/methods/0")).put("evidenceRef", 42);
        f.replace("improved", improved);
        ObjectNode ledger = (ObjectNode) JSON.readTree(root.resolve("proofs.json").toFile());
        ((ObjectNode) ledger.at("/improved/methods/0")).put("evidenceRef", 42);
        ((ObjectNode) ledger.get("improved")).put("proposalsSha256", sha(Files.readAllBytes(root.resolve("improved.json"))));
        f.replace("proofs", ledger);
        assertThatThrownBy(f::run).isInstanceOf(IllegalArgumentException.class);
        assertThat(f.output).doesNotExist();
    }

    @Test
    void realApplicationEntrypointWritesBoundComparisonWithoutStartingSpring() throws Exception {
        Fixture f = fixture();
        Path log = root.resolve("cli.log");
        var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin/java").toString(),
                "-Xmx256m", "-cp", System.getProperty("java.class.path"),
                "com.featuredeliveryintelligence.fdi.application.FdiApplication", "method-pair-compare",
                "--manifest", f.manifest.toString(), "--manifest-sha256", f.pin(), "--output", f.output.toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertThat(process.waitFor(20, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(process.exitValue()).withFailMessage(Files.readString(log)).isZero();
            assertThat(Files.readString(log)).contains("SCORING_MECHANICS_ONLY").doesNotContain("Starting FdiApplication");
            assertThat(JSON.readTree(f.output.toFile()).at("/improved/truePositives").asInt()).isEqualTo(1);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }

    @Test
    void legitimateEvaluatorMethodNamesAreNotProducerVocabularyViolations() throws Exception {
        Fixture f = fixture();
        Method method = new Method(REVISION, "backend/src/main/java/demo/Evaluator.java", "demo.Evaluator#score(int)");
        Pair pair = new Pair("s1", method);
        f.replace("improved", Map.of("binding", binding(), "proposals",
                proposals(List.of(claim(pair, "e")), List.of(), List.of())));
        f.replace("truth", truth(List.of("s1"), List.of(pair),
                List.of(new Chain("s1", List.of(method), List.of()))));
        f.replace("proofs", Map.of(
                "baseline", new Proofs(sha(Files.readAllBytes(root.resolve("baseline.json"))), List.of(), List.of()),
                "improved", new Proofs(sha(Files.readAllBytes(root.resolve("improved.json"))),
                        List.of(new MethodProof(pair, "e")), List.of())));
        f.run();
        assertThat(JSON.readTree(f.output.toFile()).at("/improved/truePositives").asInt()).isEqualTo(1);
    }

    @Test
    void structuralIdentityValidationStillRefusesTestsUnsafePathsAndInvalidRevisions() throws Exception {
        for (Method method : List.of(
                new Method(REVISION, "backend/src/test/java/demo/Service.java", A.signature()),
                new Method(REVISION, "backend/src/main/java/demo/ServiceTest.java", A.signature()),
                new Method(REVISION, " backend/src/main/java/demo/Service.java", A.signature()),
                new Method(REVISION, "backend/src/main/java/demo/../Service.java", A.signature()),
                new Method(REVISION, "backend\\src\\main\\java\\Service.java", A.signature()),
                new Method("not-a-revision", A.path(), A.signature()))) {
            Fixture f = fixture();
            f.replace("truth", truth(List.of("s1"), List.of(new Pair("s1", method)), List.of()));
            assertThatThrownBy(f::run).isInstanceOf(IllegalArgumentException.class);
            assertThat(f.output).doesNotExist();
        }
    }

    private Map<String, Object> binding() {
        return Map.of("sourceRevision", REVISION, "productionRoots", List.of("backend/src/main/java"),
                "testRoots", List.of("backend/src/test/java"), "inputSnapshotSha256", DIGEST,
                "extractorSha256", "c".repeat(64));
    }

    private Fixture fixture() throws Exception {
        Pair pair = new Pair("s1", A);
        Files.write(root.resolve("baseline.json"), JSON.writeValueAsBytes(Map.of("binding", binding(),
                "proposals", proposals(List.of(), List.of("s1"), List.of()))));
        Files.write(root.resolve("improved.json"), JSON.writeValueAsBytes(Map.of("binding", binding(),
                "proposals", proposals(List.of(claim(pair, "e")), List.of(), List.of()))));
        Files.write(root.resolve("truth.json"), JSON.writeValueAsBytes(truth(List.of("s1"), List.of(pair),
                List.of(new Chain("s1", List.of(A), List.of())))));
        Files.write(root.resolve("proofs.json"), JSON.writeValueAsBytes(Map.of(
                "baseline", new Proofs(sha(Files.readAllBytes(root.resolve("baseline.json"))), List.of(), List.of()),
                "improved", new Proofs(sha(Files.readAllBytes(root.resolve("improved.json"))),
                        List.of(new MethodProof(pair, "e")), List.of()))));
        ObjectNode manifest = JSON.createObjectNode();
        manifest.put("schemaVersion", CONTRACT).put("datasetKind", "SYNTHETIC");
        manifest.set("binding", JSON.valueToTree(binding()));
        for (String key : List.of("baseline", "improved", "truth", "proofs")) {
            manifest.set(key, JSON.valueToTree(Map.of("path", key + ".json",
                    "sha256", sha(Files.readAllBytes(root.resolve(key + ".json"))))));
        }
        Path path = root.resolve("manifest.json");
        Files.write(path, JSON.writeValueAsBytes(manifest));
        return new Fixture(path, root.resolve("result.json"));
    }

    private record Fixture(Path manifest, Path output) {
        String pin() throws Exception { return sha(Files.readAllBytes(manifest)); }
        void run() throws Exception { new MethodPairComparisonRunner().compare(manifest, pin(), output); }
        void replace(String key, Object value) throws Exception {
            Files.write(manifest.resolveSibling(key + ".json"), JSON.writeValueAsBytes(value));
            refreshRef(key);
        }
        void refreshRef(String key) throws Exception {
            ObjectNode node = (ObjectNode) JSON.readTree(manifest.toFile());
            ((ObjectNode) node.get(key)).put("sha256", sha(Files.readAllBytes(manifest.resolveSibling(key + ".json"))));
            Files.write(manifest, JSON.writeValueAsBytes(node));
        }
    }

    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}

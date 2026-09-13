package com.featuredeliveryintelligence.fdi.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.featuredeliveryintelligence.fdi.product.realization.formalholdout.v1.FormalHoldoutConformanceScorer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FormalHoldoutScoreCliTests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final FormalHoldoutConformanceScorer scorer = new FormalHoldoutConformanceScorer();
    @TempDir Path temp;

    @Test
    void canonicalIdentityIncludesSignatureAndRejectsNonErasedGeneric() throws Exception {
        JsonNode valid = score("CANONICAL_IDENTITY", """
            {"candidate":{"fullyQualifiedDeclaringType":"invalid.example.Service","methodName":"run","parameterTypes":["int","byte[]"],"repositorySnapshotSha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","returnType":"void","scenarioId":"S-1"}}
            """, "POSITIVE");
        assertThat(valid.path("result").asText()).isEqualTo("VALID");
        assertThat(valid.path("canonicalOutputs").path(0).path("pairDigest").asText()).matches("[0-9a-f]{64}");
        assertThat(valid.path("normalizedParameterTypes")).containsExactly(JSON.getNodeFactory().textNode("int"), JSON.getNodeFactory().textNode("byte[]"));

        JsonNode invalid = score("CANONICAL_IDENTITY", """
            {"candidates":[{"fullyQualifiedDeclaringType":"invalid.example.Service","methodName":"run","parameterTypes":["java.util.List"],"repositorySnapshotSha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","returnType":"void","scenarioId":"S-1"},{"fullyQualifiedDeclaringType":"invalid.example.Service","methodName":"run","parameterTypes":["java.util.List<java.lang.String>"],"repositorySnapshotSha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","returnType":"void","scenarioId":"S-1"}]}
            """, "NEGATIVE");
        assertThat(invalid.path("result").asText()).isEqualTo("MIXED");
        assertThat(invalid.path("canonicalOutputs").path(1).path("result").asText()).isEqualTo("INVALID_NON_ERASED_GENERIC");

        JsonNode strayClose = score("CANONICAL_IDENTITY", """
            {"candidate":{"fullyQualifiedDeclaringType":"invalid.example.Service","methodName":"run","parameterTypes":["java.util.List>"],"repositorySnapshotSha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","returnType":"void","scenarioId":"S-1"}}
            """, "NEGATIVE");
        assertThat(strayClose.path("canonicalOutputs").path(0).path("result").asText()).isEqualTo("INVALID_NON_ERASED_GENERIC");

        JsonNode badConstructor = score("CANONICAL_IDENTITY", """
            {"candidate":{"fullyQualifiedDeclaringType":"invalid.example.Service","methodName":"<init>","parameterTypes":[],"repositorySnapshotSha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","returnType":"invalid.example.Service","scenarioId":"S-1"}}
            """, "NEGATIVE");
        assertThat(badConstructor.path("canonicalOutputs").path(0).path("result").asText()).isEqualTo("INVALID_CONSTRUCTOR_SIGNATURE");
        assertThat(badConstructor.path("result").asText()).isEqualTo("INVALID");
        assertThat(badConstructor.has("relation")).isFalse();
        assertThat(valid.path("canonicalOutputs")).hasSize(1);
    }

    @Test
    void occurrenceScoringCountsFirstMatchAndDuplicateOccurrences() throws Exception {
        JsonNode oracle = score("OCCURRENCE_SCORING", """
            {"goldPairDigests":["a"],"proposalOccurrences":[{"disposition":"VALID","occurrenceIndex":0,"pairDigest":"a"},{"disposition":"VALID","occurrenceIndex":1,"pairDigest":"a"},{"disposition":"VALID","occurrenceIndex":2,"pairDigest":"b"}]}
            """, "POSITIVE");
        assertThat(oracle.path("counts").toString()).isEqualTo("{\"duplicateCount\":1,\"fn\":0,\"fp\":2,\"tp\":1}");

        JsonNode invalid = score("OCCURRENCE_SCORING", """
            {"goldPairDigests":["a"],"proposalOccurrences":[{"disposition":"VALID","occurrenceIndex":1,"pairDigest":"a"}]}
            """, "NEGATIVE");
        assertThat(invalid.path("result").asText()).isEqualTo("INVALID");
        assertThat(invalid.has("counts")).isFalse();
        assertThat(invalid.path("reasonCodes")).containsExactly(JSON.getNodeFactory().textNode("NONCONTIGUOUS_OCCURRENCE_INDEX"));
    }

    @Test
    void dispositionDistinguishesScoredMismatchFromInvalidProof() throws Exception {
        String occurrence = """
            "occurrence":{"facets":{"action":"MATCH","ambiguity":"UNAMBIGUOUS","assertionRole":"MATCH","businessCondition":"MATCH","entity":"FAIL","evidenceSufficiency":"SUFFICIENT","polarity":"MATCH"},"occurrenceId":"S-1#0","pairDigest":"a"}
            """;
        JsonNode mismatch = score("DISPOSITION_EVIDENCE", "{\"goldPairDigest\":\"a\"," + occurrence + ",\"proofs\":[{\"occurrenceId\":\"S-1#0\",\"pairDigest\":\"a\",\"proofDigest\":\"b\"}],\"sealedProofDigest\":\"b\"}", "NEGATIVE");
        assertThat(mismatch.path("result").asText()).isEqualTo("VALID");
        assertThat(mismatch.path("reasonCodes")).containsExactly(JSON.getNodeFactory().textNode("ENTITY_MISMATCH"));
        assertThat(mismatch.path("counts").path("fp").asInt()).isEqualTo(1);

        JsonNode missingProof = score("DISPOSITION_EVIDENCE", "{\"goldPairDigest\":\"a\"," + occurrence + ",\"proofs\":[],\"sealedProofDigest\":\"b\"}", "NEGATIVE");
        assertThat(missingProof.path("result").asText()).isEqualTo("INVALID");
        assertThat(missingProof.has("counts")).isFalse();
        assertThat(missingProof.path("reasonCodes")).containsExactly(JSON.getNodeFactory().textNode("MISSING_PROOF"));

        String validFacets = occurrence.replace("\"entity\":\"FAIL\"", "\"entity\":\"MATCH\"").replace("\"pairDigest\":\"a\"", "\"pairDigest\":\"b\"");
        JsonNode wrongPair = score("DISPOSITION_EVIDENCE", "{\"goldPairDigest\":\"a\"," + validFacets + ",\"proofs\":[{\"occurrenceId\":\"S-1#0\",\"pairDigest\":\"b\",\"proofDigest\":\"p\"}],\"sealedProofDigest\":\"p\"}", "NEGATIVE");
        assertThat(wrongPair.path("result").asText()).isEqualTo("VALID");
        assertThat(wrongPair.path("reasonCodes")).containsExactly(JSON.getNodeFactory().textNode("PAIR_MISMATCH"));
        assertThat(wrongPair.path("counts").toString()).isEqualTo("{\"duplicateCount\":0,\"fn\":1,\"fp\":1,\"tp\":0}");
    }

    @Test
    void provenanceAcceptsBoundEvidenceAndRejectsForeignRepository() throws Exception {
        String base = "\"artifactSha256\":\"d\",\"coverageStrata\":[\"UNIT\"],\"expectedArtifactSha256\":\"d\",\"pairRepositorySnapshotSha256\":\"a\",\"repositorySnapshotSha256\":\"a\",\"scenarioId\":\"S-1\",\"sourceProvenanceSha256\":\"c\",\"testProvenanceSha256\":\"b\",\"truthDisposition\":\"SEALED\"";
        assertThat(score("PROVENANCE_INTEGRITY", "{" + base + ",\"foreignRepositoryReference\":false}", "POSITIVE").path("result").asText()).isEqualTo("VALID");
        JsonNode invalid = score("PROVENANCE_INTEGRITY", "{" + base + ",\"foreignRepositoryReference\":true}", "NEGATIVE");
        assertThat(invalid.path("reasonCodes")).containsExactly(JSON.getNodeFactory().textNode("CROSS_REPOSITORY_REFERENCE"));
    }

    @Test
    void emptyProposalAndExplicitAbstentionRetainDenominators() throws Exception {
        JsonNode empty = score("EMPTY_AND_ABSTENTION", "{\"goldCount\":2,\"proposalOccurrences\":[],\"scenarioCount\":1}", "NEGATIVE");
        assertThat(empty.path("result").asText()).isEqualTo("INVALID");
        assertThat(empty.path("precision").isNull()).isTrue();
        assertThat(empty.path("counts").path("fn").asInt()).isEqualTo(2);
        JsonNode abstained = score("EMPTY_AND_ABSTENTION", "{\"abstention\":\"UNSUPPORTED\",\"goldCount\":2,\"proposalOccurrences\":[],\"scenarioCount\":1}", "NEGATIVE");
        assertThat(abstained.path("result").asText()).isEqualTo("VALID");
        assertThat(abstained.path("scenarioDenominator").asInt()).isEqualTo(1);
    }

    @Test
    void chainRequiresCanonicalAdjacentTruthAndMatchingProposalEdges() throws Exception {
        String good = "{\"chainRequired\":true,\"orderedGoldPairDigests\":[\"a\",\"b\",\"c\"],\"proposalEdges\":[[\"a\",\"b\"],[\"b\",\"c\"]],\"proposalPairDigests\":[\"a\",\"b\",\"c\"],\"truthEdges\":[[\"a\",\"b\"],[\"b\",\"c\"]]}";
        assertThat(score("CHAIN_SCORING", good, "POSITIVE").path("chainComplete").asBoolean()).isTrue();
        String reversed = good.replace("[[\"a\",\"b\"],[\"b\",\"c\"]]}", "[[\"b\",\"a\"],[\"b\",\"c\"]]}");
        JsonNode invalid = score("CHAIN_SCORING", reversed, "NEGATIVE");
        assertThat(invalid.path("result").asText()).isEqualTo("INVALID");
        assertThat(invalid.path("reasonCodes")).containsExactly(JSON.getNodeFactory().textNode("REVERSED_TRUTH_EDGE"));

        String duplicate = good.replace("\"proposalPairDigests\":[\"a\",\"b\",\"c\"]", "\"proposalPairDigests\":[\"a\",\"a\",\"b\",\"c\"]");
        JsonNode duplicateOracle = score("CHAIN_SCORING", duplicate, "NEGATIVE");
        assertThat(duplicateOracle.path("counts").toString()).isEqualTo("{\"duplicateCount\":1,\"fn\":0,\"fp\":1,\"tp\":3}");
        String extraEdge = good.replace("[\"b\",\"c\"]],\"proposalPairDigests\"", "[\"b\",\"c\"],[\"a\",\"c\"]],\"proposalPairDigests\"");
        JsonNode edgeOracle = score("CHAIN_SCORING", extraEdge, "NEGATIVE");
        assertThat(edgeOracle.path("result").asText()).isEqualTo("VALID");
        assertThat(edgeOracle.path("chainComplete").asBoolean()).isFalse();
    }

    @Test
    void repositoryDecisionUsesStrictThresholdsAndCannotMaskNullMetric() throws Exception {
        JsonNode boundary = score("REPOSITORY_DECISION", "{\"repositories\":[{\"fn\":1,\"fp\":1,\"repositoryId\":\"r1\",\"tp\":4}]}", "NEGATIVE");
        assertThat(boundary.path("repositories").path(0).path("precision").asText()).isEqualTo("0.800000000000");
        assertThat(boundary.path("repositories").path(0).path("strictPass").asBoolean()).isFalse();
        JsonNode noProposal = score("REPOSITORY_DECISION", "{\"repositories\":[{\"fn\":2,\"fp\":0,\"repositoryId\":\"r1\",\"tp\":0}]}", "NEGATIVE");
        assertThat(noProposal.path("result").asText()).isEqualTo("INVALID");
        assertThat(noProposal.path("aggregate").path("microPrecision").isNull()).isTrue();

        JsonNode noGold = score("REPOSITORY_DECISION", "{\"repositories\":[{\"fn\":0,\"fp\":2,\"repositoryId\":\"r1\",\"tp\":0}]}", "NEGATIVE");
        assertThat(noGold.path("repositories").path(0).path("recall").isNull()).isTrue();
        assertThat(noGold.path("repositories").path(0).path("recallReason").asText()).isEqualTo("NO_GOLD_PAIRS");
        assertThat(noGold.path("aggregate").path("macroRecall").isNull()).isTrue();
        assertThat(noGold.path("aggregate").path("macroRecallReason").asText()).isEqualTo("NO_GOLD_PAIRS");
        assertThat(noGold.path("aggregate").path("microRecall").isNull()).isTrue();
        assertThat(noGold.path("aggregate").path("microRecallReason").asText()).isEqualTo("NO_GOLD_PAIRS");

        JsonNode rawMacro = score("REPOSITORY_DECISION", "{\"repositories\":[{\"fn\":10,\"fp\":5,\"repositoryId\":\"r1\",\"tp\":90},{\"fn\":2,\"fp\":2,\"repositoryId\":\"r2\",\"tp\":4}]}", "NEGATIVE");
        assertThat(rawMacro.path("aggregate").path("macroRecall").asText()).isEqualTo("0.783333333333");

        assertThatThrownBy(() -> score("REPOSITORY_DECISION", "{\"repositories\":[]}", "NEGATIVE")).hasMessageContaining("nonempty");
        assertThatThrownBy(() -> score("REPOSITORY_DECISION", "{\"repositories\":[{\"fn\":-1,\"fp\":0,\"repositoryId\":\"r1\",\"tp\":1}]}", "NEGATIVE")).hasMessageContaining("nonnegative");
        assertThatThrownBy(() -> score("REPOSITORY_DECISION", "{\"repositories\":[{\"fn\":0,\"fp\":0,\"repositoryId\":\"r1\",\"tp\":1},{\"fn\":0,\"fp\":0,\"repositoryId\":\"r1\",\"tp\":1}]}", "NEGATIVE")).hasMessageContaining("duplicate repositoryId");

        JsonNode large = score("REPOSITORY_DECISION", "{\"repositories\":[{\"fn\":9223372036854775807,\"fp\":9223372036854775807,\"repositoryId\":\"r1\",\"tp\":9223372036854775807},{\"fn\":9223372036854775807,\"fp\":9223372036854775807,\"repositoryId\":\"r2\",\"tp\":9223372036854775807}]}", "NEGATIVE");
        assertThat(large.path("aggregate").path("microPrecision").asText()).isEqualTo("0.500000000000");
        assertThat(large.path("repositories").path(0).path("tp").bigIntegerValue()).isEqualTo(new java.math.BigInteger("9223372036854775807"));
    }

    @Test
    void wilsonUsesDecimalBoundaryAndRejectsImpossibleCounts() throws Exception {
        JsonNode valid = score("WILSON_INTERVAL", "{\"n\":10,\"precision\":50,\"rounding\":\"HALF_EVEN\",\"scale\":12,\"x\":8,\"zDecimal\":\"1.959963984540054\"}", "POSITIVE");
        assertThat(valid.path("lower").asText()).isEqualTo("0.490162471537");
        assertThat(valid.path("upper").asText()).isEqualTo("0.943317848546");
        JsonNode invalid = score("WILSON_INTERVAL", "{\"n\":10,\"precision\":50,\"rounding\":\"HALF_EVEN\",\"scale\":12,\"x\":11,\"zDecimal\":\"1.959963984540054\"}", "NEGATIVE");
        assertThat(invalid.path("reasonCodes")).containsExactly(JSON.getNodeFactory().textNode("INVALID_COUNTS"));
        assertThatThrownBy(() -> score("WILSON_INTERVAL", "{\"n\":10,\"precision\":34,\"rounding\":\"HALF_UP\",\"scale\":9,\"x\":8,\"zDecimal\":\"2\"}", "NEGATIVE")).hasMessageContaining("Wilson parameters");
    }

    @Test
    void parityHashesBytesAndFailsOnAnyRunMismatch() throws Exception {
        String matched = "{\"goldenBytes\":\"x\\n\",\"javaRun1Bytes\":\"x\\n\",\"javaRun2Bytes\":\"x\\n\",\"pythonRun1Bytes\":\"x\\n\",\"pythonRun2Bytes\":\"x\\n\"}";
        assertThat(score("DETERMINISM_AND_PARITY", matched, "POSITIVE").path("result").asText()).isEqualTo("PASS");
        JsonNode failed = score("DETERMINISM_AND_PARITY", matched.replace("\"pythonRun2Bytes\":\"x\\n\"", "\"pythonRun2Bytes\":\"y\\n\""), "NEGATIVE");
        assertThat(failed.path("result").asText()).isEqualTo("FAIL");
        assertThat(failed.path("byteIdentical").asBoolean()).isFalse();
    }

    @Test
    void cliWritesCanonicalContractAndFailsClosedWhenOutputExists() throws Exception {
        byte[] input = caseJson("WILSON_INTERVAL", "{\"n\":1,\"precision\":50,\"rounding\":\"HALF_EVEN\",\"scale\":12,\"x\":1,\"zDecimal\":\"1.959963984540054\"}").getBytes(StandardCharsets.UTF_8);
        Path inputs = Files.createDirectory(temp.resolve("inputs")); Files.write(inputs.resolve("v.json"), input);
        String digest = FormalHoldoutConformanceScorer.sha256(input);
        Path manifest = temp.resolve("manifest.json");
        Files.writeString(manifest, manifestJson("V1", "v.json", digest, "WILSON_INTERVAL", "POSITIVE"));
        Path output = temp.resolve("result.json");
        runCli(manifest, inputs, output);
        assertThat(Files.readString(output)).endsWith("\n");
        JsonNode root = JSON.readTree(output.toFile());
        assertThat(root.fieldNames()).toIterable().containsExactly("manifestSha256", "results", "schemaVersion");
        assertThat(root.path("results").path(0).fieldNames()).toIterable().containsExactly("oracle", "vectorId");
        assertThatThrownBy(() -> runCli(manifest, inputs, output)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("absent");
    }

    @Test
    void cliRejectsMalformedJsonSchemaPathsSymlinksAndDigestMismatch() throws Exception {
        Path inputs = Files.createDirectory(temp.resolve("strict-inputs"));
        byte[] good = caseJson("WILSON_INTERVAL", "{\"n\":1,\"precision\":50,\"rounding\":\"HALF_EVEN\",\"scale\":12,\"x\":1,\"zDecimal\":\"1.959963984540054\"}").getBytes(StandardCharsets.UTF_8);
        Files.write(inputs.resolve("good.json"), good);

        assertCliInputRejected(inputs, "{\"given\":{},\"given\":{},\"operation\":\"WILSON_INTERVAL\"}", "duplicate");
        assertCliInputRejected(inputs, caseJson("WILSON_INTERVAL", "{\"n\":1,\"precision\":50,\"rounding\":\"HALF_EVEN\",\"scale\":12,\"x\":1,\"zDecimal\":\"1.959963984540054\",\"unknown\":true}"), "unknown");
        assertCliInputRejected(inputs, "{\"given\":null,\"operation\":\"WILSON_INTERVAL\"}", "null");
        assertCliInputRejected(inputs, caseJson("WILSON_INTERVAL", "{\"n\":1,\"x\":1}"), "missing");
        assertCliInputRejected(inputs, caseJson("WILSON_INTERVAL", "{\"n\":1,\"x\":null,\"zDecimal\":\"1.959963984540054\",\"precision\":50,\"rounding\":\"HALF_EVEN\",\"scale\":12}"), "null");
        assertCliInputRejected(inputs, caseJson("WILSON_INTERVAL", "{\"n\":1,\"precision\":50,\"rounding\":\"HALF_EVEN\",\"scale\":12,\"x\":1,\"zDecimal\":\"1.959963984540054\"}") + " true", "trailing");

        Path escapeManifest = writeManifest("../outside.json", FormalHoldoutConformanceScorer.sha256(good));
        assertThatThrownBy(() -> runCli(escapeManifest, inputs, temp.resolve("escape-output.json"))).hasMessageContaining("escapes root");

        Path linked = inputs.resolve("linked.json");
        Files.createSymbolicLink(linked, inputs.resolve("good.json"));
        Path linkManifest = writeManifest("linked.json", FormalHoldoutConformanceScorer.sha256(good));
        assertThatThrownBy(() -> runCli(linkManifest, inputs, temp.resolve("link-output.json"))).hasMessageContaining("symlink");

        Path digestManifest = writeManifest("good.json", "0".repeat(64));
        assertThatThrownBy(() -> runCli(digestManifest, inputs, temp.resolve("digest-output.json"))).hasMessageContaining("digest mismatch");

        Path realManifest = writeManifest("good.json", FormalHoldoutConformanceScorer.sha256(good)); Path linkedManifest=temp.resolve("linked-manifest.json");Files.createSymbolicLink(linkedManifest,realManifest);
        assertThatThrownBy(() -> runCli(linkedManifest,inputs,temp.resolve("linked-manifest-output.json"))).hasMessageContaining("manifest symlink");

        Path realInputs = Files.createDirectory(temp.resolve("real-inputs")); Files.write(realInputs.resolve("good.json"), good);
        Path inputRootLink = temp.resolve("input-root-link"); Files.createSymbolicLink(inputRootLink, realInputs);
        Path rootLinkManifest = writeManifest("good.json", FormalHoldoutConformanceScorer.sha256(good));
        assertThatThrownBy(() -> runCli(rootLinkManifest, inputRootLink, temp.resolve("root-link-output.json"))).hasMessageContaining("symlink");

        Path realOutputParent = Files.createDirectory(temp.resolve("real-output")); Path outputParentLink = temp.resolve("output-link"); Files.createSymbolicLink(outputParentLink, realOutputParent);
        assertThatThrownBy(() -> runCli(rootLinkManifest, inputs, outputParentLink.resolve("out.json"))).hasMessageContaining("symlink");
    }

    @Test
    void manifestRequiresUniqueIdsAndExactNestedCoverage() throws Exception {
        Path inputs = Files.createDirectory(temp.resolve("manifest-inputs"));
        byte[] input = caseJson("WILSON_INTERVAL", "{\"n\":1,\"precision\":50,\"rounding\":\"HALF_EVEN\",\"scale\":12,\"x\":1,\"zDecimal\":\"1.959963984540054\"}").getBytes(StandardCharsets.UTF_8);
        Files.write(inputs.resolve("v.json"), input); String sha = FormalHoldoutConformanceScorer.sha256(input);
        String vector = vectorJson("V1", "v.json", sha, "WILSON_INTERVAL", "POSITIVE");
        Path duplicate = temp.resolve("duplicate-manifest.json"); Files.writeString(duplicate, "{\"schemaVersion\":\"SFBL005-FORMAL-SCORER-VECTOR-MANIFEST-001\",\"vectors\":["+vector+","+vector+"]}");
        assertThatThrownBy(() -> runCli(duplicate, inputs, temp.resolve("duplicate-result.json"))).hasMessageContaining("duplicate vector id");
        Path badPolarity = temp.resolve("polarity-manifest.json"); Files.writeString(badPolarity, manifestJson("V1","v.json",sha,"WILSON_INTERVAL","MAYBE"));
        assertThatThrownBy(() -> runCli(badPolarity, inputs, temp.resolve("polarity-result.json"))).hasMessageContaining("polarity");
        Path nestedUnknown = temp.resolve("nested-manifest.json"); Files.writeString(nestedUnknown, manifestJson("V1","v.json",sha,"WILSON_INTERVAL","POSITIVE").replace("\"polarity\":\"POSITIVE\"", "\"polarity\":\"POSITIVE\",\"extra\":1"));
        assertThatThrownBy(() -> runCli(nestedUnknown, inputs, temp.resolve("nested-result.json"))).hasMessageContaining("unknown coverage field");
        Path wrongSchema = temp.resolve("wrong-schema.json"); Files.writeString(wrongSchema, manifestJson("V1","v.json",sha,"WILSON_INTERVAL","POSITIVE").replace("MANIFEST-001","MANIFEST-999"));
        assertThatThrownBy(() -> runCli(wrongSchema, inputs, temp.resolve("wrong-schema-out.json"))).hasMessageContaining("schemaVersion");
        Path wrongRule = temp.resolve("wrong-rule.json"); Files.writeString(wrongRule, manifestJson("V1","v.json",sha,"CHAIN_SCORING","POSITIVE"));
        assertThatThrownBy(() -> runCli(wrongRule, inputs, temp.resolve("wrong-rule-out.json"))).hasMessageContaining("ruleId");
        Path numericRule = temp.resolve("numeric-rule.json"); Files.writeString(numericRule, manifestJson("V1","v.json",sha,"WILSON_INTERVAL","POSITIVE").replace("\"ruleId\":\"WILSON_INTERVAL\"", "\"ruleId\":1"));
        assertThatThrownBy(() -> runCli(numericRule, inputs, temp.resolve("numeric-rule-out.json"))).hasMessageContaining("ruleId");
        Path numericPath = temp.resolve("numeric-path.json"); Files.writeString(numericPath, manifestJson("V1","v.json",sha,"WILSON_INTERVAL","POSITIVE").replace("\"path\":\"v.json\"", "\"path\":1"));
        assertThatThrownBy(() -> runCli(numericPath, inputs, temp.resolve("numeric-path-out.json"))).hasMessageContaining("path");
    }

    @Test
    void caseMetadataMustBeExactAndBoundToManifestIdentityAndOrder() throws Exception {
        Path inputs=Files.createDirectory(temp.resolve("metadata-inputs"));
        String valid=caseJson("WILSON_INTERVAL","{\"n\":1,\"precision\":50,\"rounding\":\"HALF_EVEN\",\"scale\":12,\"x\":1,\"zDecimal\":\"1.959963984540054\"}");
        assertMetadataRejected(inputs,valid.replace("\"contract\":\"developer-test\"","\"contract\":1"),"contract");
        assertMetadataRejected(inputs,valid.replace("conformance.invalid","wrong.invalid"),"namespace");
        assertMetadataRejected(inputs,valid.replace("\"caseOrdinal\":1","\"caseOrdinal\":2"),"caseOrdinal");
        assertMetadataRejected(inputs,valid.replace("\"vectorId\":\"V1\"","\"vectorId\":\"OTHER\""),"vectorId");
        assertMetadataRejected(inputs,valid.replace("SFBL005-FORMAL-SCORER-CONFORMANCE-CASE-001","BAD"),"schemaVersion");
        assertThatThrownBy(() -> scorer.score(JSON.readTree("{\"given\":{},\"operation\":\"WILSON_INTERVAL\"}"),"NEGATIVE")).hasMessageContaining("caseOrdinal");
    }

    @Test
    void ownedJavaSourcesDoNotReadOracleFixtureDirectories() throws Exception {
        Path main = Path.of("src/main/java/com/featuredeliveryintelligence/fdi");
        Path test = Path.of("src/test/java/com/featuredeliveryintelligence/fdi/application/FormalHoldoutScoreCliTests.java");
        String forbiddenResolve = ".resolve(\"ex" + "pected\")";
        String forbiddenPath = "conformance/ex" + "pected";
        try (var files = Files.walk(main)) {
            assertThat(files.filter(Files::isRegularFile).map(this::read).noneMatch(s -> s.contains(forbiddenResolve) || s.contains(forbiddenPath))).isTrue();
        }
        String testSource = Files.readString(test);
        assertThat(testSource).doesNotContain(forbiddenResolve, forbiddenPath);
    }

    @Test
    void everyOperationRejectsMalformedNestedTypesAndShapes() {
        assertMalformed("CANONICAL_IDENTITY", "{\"candidate\":{\"fullyQualifiedDeclaringType\":1,\"methodName\":\"m\",\"parameterTypes\":[],\"repositorySnapshotSha256\":\"a\",\"returnType\":\"void\",\"scenarioId\":\"s\"}}", "fullyQualifiedDeclaringType");
        assertMalformed("OCCURRENCE_SCORING", "{\"goldPairDigests\":\"a\",\"proposalOccurrences\":[]}", "goldPairDigests");
        assertMalformed("DISPOSITION_EVIDENCE", "{\"goldPairDigest\":\"a\",\"occurrence\":[],\"proofs\":[],\"sealedProofDigest\":\"b\"}", "occurrence");
        assertMalformed("PROVENANCE_INTEGRITY", "{\"artifactSha256\":\"d\",\"coverageStrata\":{},\"expectedArtifactSha256\":\"d\",\"foreignRepositoryReference\":false,\"pairRepositorySnapshotSha256\":\"a\",\"repositorySnapshotSha256\":\"a\",\"scenarioId\":\"s\",\"sourceProvenanceSha256\":\"c\",\"testProvenanceSha256\":\"b\",\"truthDisposition\":\"SEALED\"}", "coverageStrata");
        assertMalformed("EMPTY_AND_ABSTENTION", "{\"goldCount\":\"2\",\"proposalOccurrences\":[],\"scenarioCount\":1}", "goldCount");
        assertMalformed("CHAIN_SCORING", "{\"chainRequired\":true,\"orderedGoldPairDigests\":[\"a\",\"b\"],\"proposalEdges\":[[\"a\"]],\"proposalPairDigests\":[\"a\",\"b\"],\"truthEdges\":[[\"a\",\"b\"]]}", "proposalEdges");
        assertMalformed("REPOSITORY_DECISION", "{\"repositories\":[{\"fn\":0.5,\"fp\":0,\"repositoryId\":\"r\",\"tp\":1}]}", "fn");
        assertMalformed("WILSON_INTERVAL", "{\"n\":10.0,\"precision\":50,\"rounding\":\"HALF_EVEN\",\"scale\":12,\"x\":8,\"zDecimal\":\"1.959963984540054\"}", "n");
        assertMalformed("DETERMINISM_AND_PARITY", "{\"goldenBytes\":null,\"javaRun1Bytes\":\"x\",\"javaRun2Bytes\":\"x\",\"pythonRun1Bytes\":\"x\",\"pythonRun2Bytes\":\"x\"}", "goldenBytes");
    }

    private JsonNode score(String operation, String given, String polarity) throws Exception {
        return scorer.score(JSON.readTree(caseJson(operation, given)), polarity);
    }
    private static String caseJson(String operation, String given) { return "{\"caseOrdinal\":1,\"contract\":\"developer-test\",\"given\":" + given + ",\"namespace\":\"conformance.invalid\",\"operation\":\"" + operation + "\",\"schemaVersion\":\"SFBL005-FORMAL-SCORER-CONFORMANCE-CASE-001\",\"vectorId\":\"V1\"}"; }
    private static void runCli(Path manifest, Path inputs, Path output) throws Exception { FormalHoldoutScoreCli.main(new String[]{"--manifest",manifest.toString(),"--inputs-root",inputs.toString(),"--output",output.toString()}); }
    private Path writeManifest(String inputPath, String digest) throws Exception {
        Path manifest = Files.createTempFile(temp, "manifest-", ".json");
        Files.writeString(manifest, manifestJson("V1", inputPath, digest, "WILSON_INTERVAL", "POSITIVE"));
        return manifest;
    }
    private static String manifestJson(String id,String path,String sha,String rule,String polarity){return "{\"schemaVersion\":\"SFBL005-FORMAL-SCORER-VECTOR-MANIFEST-001\",\"vectors\":["+vectorJson(id,path,sha,rule,polarity)+"]}";}
    private static String vectorJson(String id,String path,String sha,String rule,String polarity){return "{\"coverage\":[{\"polarity\":\""+polarity+"\",\"ruleId\":\""+rule+"\"}],\"expected\":{\"path\":\"unused.json\",\"sha256\":\""+"e".repeat(64)+"\"},\"id\":\""+id+"\",\"input\":{\"path\":\""+path+"\",\"sha256\":\""+sha+"\"}}";}
    private void assertCliInputRejected(Path inputs, String content, String message) throws Exception {
        Path input = inputs.resolve("bad-" + message + ".json"); Files.writeString(input, content);
        Path manifest = writeManifest(input.getFileName().toString(), FormalHoldoutConformanceScorer.sha256(Files.readAllBytes(input)));
        assertThatThrownBy(() -> runCli(manifest, inputs, temp.resolve("out-" + message + ".json"))).hasMessageContaining(message);
    }
    private void assertMetadataRejected(Path inputs,String content,String field)throws Exception{Path input=inputs.resolve("bad-"+field+".json");Files.writeString(input,content);Path manifest=temp.resolve("meta-"+field+".json");Files.writeString(manifest,manifestJson("V1",input.getFileName().toString(),FormalHoldoutConformanceScorer.sha256(Files.readAllBytes(input)),"WILSON_INTERVAL","NEGATIVE"));assertThatThrownBy(()->runCli(manifest,inputs,temp.resolve("meta-out-"+field+".json"))).hasMessageContaining(field);}
    private String read(Path p) { try { return Files.readString(p); } catch (Exception e) { throw new IllegalStateException(e); } }
    private void assertMalformed(String operation,String given,String field){assertThatThrownBy(() -> score(operation,given,"NEGATIVE")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(field);}
}

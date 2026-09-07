package com.featuredeliveryintelligence.fdi.testbehavior.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract tests for the provider-neutral test-behavior request and result
 * envelope (PKB-BL-009 Slice A). Every fail-closed rule in the contract is
 * pinned here so later slices inherit a stable vocabulary.
 */
class TestBehaviorContractTests {
    private static final String REVISION = "a".repeat(40);
    private static final String DIGEST = "b".repeat(64);

    private static TestBehaviorExtractionRequest request(
            String revision, List<SourceRoot> roots, Map<String, String> digests, String schemaVersion) {
        return new TestBehaviorExtractionRequest("repo-a", revision, roots, digests, schemaVersion);
    }

    @Test
    void validRequestIsAcceptedAndDefensivelyCopied() {
        List<SourceRoot> roots = List.of(new SourceRoot(SourceRoot.SourceRootKind.TEST, "src/test/java"));
        TestBehaviorExtractionRequest accepted = request(REVISION, roots, Map.of("src/test/java/A.java", DIGEST), "1");
        assertThat(accepted.canonicalRevision()).isEqualTo(REVISION);
        assertThat(accepted.sourceRoots()).containsExactlyElementsOf(roots);
        assertThatThrownBy(() -> accepted.sourceRoots().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> accepted.inputDigests().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void missingCanonicalRevisionFailsClosed() {
        assertThatThrownBy(() -> request(" ", roots(), digests(), "1"))
                .isInstanceOf(TestBehaviorExtractionException.class)
                .extracting(e -> ((TestBehaviorExtractionException) e).code())
                .isEqualTo(TestBehaviorErrorCode.MISSING_SOURCE_REVISION);
    }

    @Test
    void shortRevisionFailsClosed() {
        assertThatThrownBy(() -> request("a".repeat(7), roots(), digests(), "1"))
                .isInstanceOf(TestBehaviorExtractionException.class)
                .extracting(e -> ((TestBehaviorExtractionException) e).code())
                .isEqualTo(TestBehaviorErrorCode.INVALID_SOURCE_REVISION);
    }

    @Test
    void emptySourceRootsFailClosed() {
        assertThatThrownBy(() -> request(REVISION, List.of(), digests(), "1"))
                .isInstanceOf(TestBehaviorExtractionException.class)
                .extracting(e -> ((TestBehaviorExtractionException) e).code())
                .isEqualTo(TestBehaviorErrorCode.MISSING_SOURCE_ROOT);
    }

    @Test
    void escapedSourceRootFailsClosed() {
        assertThatThrownBy(() -> new SourceRoot(SourceRoot.SourceRootKind.TEST, "src/test/../../etc"))
                .isInstanceOf(TestBehaviorExtractionException.class)
                .extracting(e -> ((TestBehaviorExtractionException) e).code())
                .isEqualTo(TestBehaviorErrorCode.ESCAPED_SOURCE_ROOT);
        assertThatThrownBy(() -> new SourceRoot(SourceRoot.SourceRootKind.PRODUCTION, "/abs/path"))
                .isInstanceOf(TestBehaviorExtractionException.class)
                .extracting(e -> ((TestBehaviorExtractionException) e).code())
                .isEqualTo(TestBehaviorErrorCode.ESCAPED_SOURCE_ROOT);
        assertThatThrownBy(() -> new SourceRoot(SourceRoot.SourceRootKind.TEST, "src\\test"))
                .isInstanceOf(TestBehaviorExtractionException.class)
                .extracting(e -> ((TestBehaviorExtractionException) e).code())
                .isEqualTo(TestBehaviorErrorCode.ESCAPED_SOURCE_ROOT);
    }

    @Test
    void unsupportedSchemaVersionFailsClosed() {
        assertThatThrownBy(() -> request(REVISION, roots(), digests(), "2"))
                .isInstanceOf(TestBehaviorExtractionException.class)
                .extracting(e -> ((TestBehaviorExtractionException) e).code())
                .isEqualTo(TestBehaviorErrorCode.UNSUPPORTED_SCHEMA_VERSION);
    }

    @Test
    void missingOrMalformedDigestFailsClosed() {
        assertThatThrownBy(() -> request(REVISION, roots(), Map.of(), "1"))
                .isInstanceOf(TestBehaviorExtractionException.class)
                .extracting(e -> ((TestBehaviorExtractionException) e).code())
                .isEqualTo(TestBehaviorErrorCode.DIGEST_MISMATCH);
        assertThatThrownBy(() -> request(REVISION, roots(), Map.of("src/test/java/A.java", "not-hex"), "1"))
                .isInstanceOf(TestBehaviorExtractionException.class)
                .extracting(e -> ((TestBehaviorExtractionException) e).code())
                .isEqualTo(TestBehaviorErrorCode.DIGEST_MISMATCH);
        assertThatThrownBy(() -> request(REVISION, roots(), Map.of("../outside", DIGEST), "1"))
                .isInstanceOf(TestBehaviorExtractionException.class)
                .extracting(e -> ((TestBehaviorExtractionException) e).code())
                .isEqualTo(TestBehaviorErrorCode.MALFORMED_REPOSITORY_PATH);
    }

    @Test
    void sourceLocationIsOneBasedAndRepositoryRelative() {
        SourceLocation location = new SourceLocation("src/test/java/A.java", 12, 5);
        assertThat(location.line()).isEqualTo(12);
        assertThatThrownBy(() -> new SourceLocation("src/test/java/A.java", 0, 1))
                .isInstanceOf(TestBehaviorExtractionException.class)
                .hasFieldOrPropertyWithValue("code", TestBehaviorErrorCode.INVALID_EVIDENCE);
        assertThatThrownBy(() -> new SourceLocation("a/./b.java", 1, 1))
                .isInstanceOf(TestBehaviorExtractionException.class)
                .hasFieldOrPropertyWithValue("code", TestBehaviorErrorCode.MALFORMED_REPOSITORY_PATH);
    }

    @Test
    void resultSortsFilesAndMethodsDeterministically() {
        TestBehaviorExtractionResult result = resultWith(
                file("src/test/java/Z.java", method("zLast", 30), method("zFirst", 10)),
                file("src/test/java/A.java", method("only", 5)));
        assertThat(result.testFiles()).extracting(TestFileObservation::repositoryRelativePath)
                .containsExactly("src/test/java/A.java", "src/test/java/Z.java");
        assertThat(result.testFiles().get(1).testMethods()).extracting(TestMethodObservation::methodName)
                .containsExactly("zFirst", "zLast");
    }

    @Test
    void duplicateTestIdentityFailsClosed() {
        assertThatThrownBy(() -> resultWith(
                file("src/test/java/A.java", method("same", 5), method("same", 20))))
                .isInstanceOf(TestBehaviorExtractionException.class)
                .hasFieldOrPropertyWithValue("code", TestBehaviorErrorCode.DUPLICATE_TEST_IDENTITY);
    }

    @Test
    void unresolvedReferenceRetainsLocationAndKind() {
        UnresolvedReference gap = new UnresolvedReference(
                "com.example.External.call()",
                new SourceLocation("src/test/java/A.java", 9, 9),
                UnresolvedKind.EXTERNAL_DEPENDENCY_NOT_RESOLVED);
        assertThat(gap.kind()).isEqualTo(UnresolvedKind.EXTERNAL_DEPENDENCY_NOT_RESOLVED);
        assertThat(gap.location().line()).isEqualTo(9);
    }

    private static List<SourceRoot> roots() {
        return List.of(new SourceRoot(SourceRoot.SourceRootKind.TEST, "src/test/java"));
    }

    private static Map<String, String> digests() {
        return Map.of("src/test/java/A.java", DIGEST);
    }

    private static TestMethodObservation method(String name, int line) {
        return new TestMethodObservation(
                name,
                new SourceLocation("src/test/java/A.java", line, 3),
                List.of(),
                List.of(new BehaviorObservation(
                        BehaviorKind.ACTION,
                        "ownerService.findOwner(1)",
                        new SourceLocation("src/test/java/A.java", line + 1, 9),
                        java.util.Optional.of(new ReferencedProductionSymbol(
                                ReferenceKind.METHOD,
                                "org.springframework.samples.petclinic.owner.OwnerService",
                                "findOwner",
                                RelationshipBasis.IMPORTED_SOURCE_ROOT)))),
                List.of(),
                List.of());
    }

    private static TestFileObservation file(String path, TestMethodObservation... methods) {
        return new TestFileObservation(
                path, DIGEST, path.substring(path.lastIndexOf('/') + 1).replace(".java", ""),
                false, List.of(methods), List.of());
    }

    private static TestBehaviorExtractionResult resultWith(TestFileObservation... files) {
        return new TestBehaviorExtractionResult(
                new ExtractionProvenance("java-test-behavior", "JavaTestBehaviorExtractor", "1"),
                "repo-a", REVISION, digests(), List.of(files), false, List.of());
    }
}

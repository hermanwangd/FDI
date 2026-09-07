package com.featuredeliveryintelligence.fdi.testbehavior.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorErrorCode;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorExtractionException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Focused fail-closed tests for the provider-neutral test-behavior evidence
 * validator (PKB-BL-009 Slice C). Every refusal must reuse the Slice A
 * {@link TestBehaviorErrorCode} vocabulary and never produce a partial
 * acceptance report.
 */
class TestBehaviorEvidenceValidatorTests {

    private static final Path SCHEMA = Path.of("").toAbsolutePath()
            .resolve(TestBehaviorEvidenceValidator.DEFAULT_SCHEMA_PATH);
    private static final TestBehaviorEvidenceValidator VALIDATOR =
            TestBehaviorEvidenceValidator.fromRepositoryRoot(Path.of("").toAbsolutePath());

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String REVISION = "818c4136ea971c21674525f9053de0d9c7ad8cfe";
    private static final String DIGEST = "216a0bb06fa0ad2adde6139b2d4a800b4ab7cd6d57b9cd209f5fdfadb852bb01";
    private static final String FILE = "src/test/java/example/ExampleTests.java";

    // ------------------------------------------------------------------
    // Builders
    // ------------------------------------------------------------------

    private static ObjectNode validEvidence() {
        ObjectNode document = MAPPER.createObjectNode();
        document.put("schema_version", TestBehaviorEvidenceValidator.SUPPORTED_SCHEMA_VERSION);
        ObjectNode provenance = document.putObject("provenance");
        provenance.put("provider_id", "fdi.testbehavior");
        provenance.put("extractor_name", "java-test-behavior-extractor");
        provenance.put("extractor_version", "1");
        document.put("repository_id", "spring-petclinic");
        document.put("canonical_revision", REVISION);
        document.putObject("input_digests").put(FILE, DIGEST);
        ObjectNode root = document.putArray("source_roots").addObject();
        root.put("kind", "TEST");
        root.put("repository_relative_path", "src/test/java");
        document.putArray("test_files").add(testFile(FILE, "exampleTest", 20, 5));
        document.put("incomplete", false);
        document.putArray("diagnostics");
        return document;
    }

    private static ObjectNode testFile(String path, String methodName, int line, int column) {
        ObjectNode file = MAPPER.createObjectNode();
        file.put("repository_relative_path", path);
        file.put("input_digest", DIGEST);
        file.put("test_class_name", "ExampleTests");
        file.put("nested_container", false);
        file.putArray("test_methods").add(testMethod(path, methodName, line, column));
        file.putArray("unresolved_references");
        return file;
    }

    private static ObjectNode testMethod(String path, String methodName, int line, int column) {
        ObjectNode method = MAPPER.createObjectNode();
        method.put("method_name", methodName);
        method.set("declaration_location", location(path, line, column));
        ObjectNode action = method.putArray("actions").addObject();
        action.put("kind", "ACTION");
        action.put("observed_expression", "controller.find(\"Betty\")");
        action.set("location", location(path, line + 1, 9));
        action.putNull("referenced_symbol");
        method.putArray("fixtures");
        method.putArray("assertions");
        method.putArray("unresolved_references");
        return method;
    }

    private static ObjectNode location(String path, int line, int column) {
        ObjectNode location = MAPPER.createObjectNode();
        location.put("repository_relative_path", path);
        location.put("line", line);
        location.put("column", column);
        return location;
    }

    private static void assertRefusal(Runnable call, TestBehaviorErrorCode code) {
        assertThatThrownBy(call::run)
                .isInstanceOf(TestBehaviorExtractionException.class)
                .extracting(failure -> ((TestBehaviorExtractionException) failure).code())
                .isEqualTo(code);
    }

    // ------------------------------------------------------------------
    // Acceptance
    // ------------------------------------------------------------------

    @Test
    void validEvidenceProducesMechanicalReport() throws Exception {
        TestBehaviorEvidenceReport report = VALIDATOR.validate(MAPPER.writeValueAsBytes(validEvidence()));
        assertThat(report.schemaVersion()).isEqualTo("1");
        assertThat(report.canonicalRevision()).isEqualTo(REVISION);
        assertThat(report.testFileCount()).isEqualTo(1);
        assertThat(report.testMethodCount()).isEqualTo(1);
        assertThat(report.incomplete()).isFalse();
    }

    @Test
    void validEvidenceAcceptsUnresolvedReferencesAndSyntacticObservations() {
        ObjectNode document = validEvidence();
        ObjectNode method = (ObjectNode) document.get("test_files").get(0).get("test_methods").get(0);
        ObjectNode unresolved = method.putArray("unresolved_references").addObject();
        unresolved.put("reference_text", "assertThrows");
        unresolved.set("location", location(FILE, 22, 9));
        unresolved.put("kind", "EXTERNAL_DEPENDENCY_NOT_RESOLVED");
        TestBehaviorEvidenceReport report = VALIDATOR.validate(document);
        assertThat(report.testMethodCount()).isEqualTo(1);
    }

    @Test
    void missingSchemaFileIsDeploymentFailureNotEvidenceRefusal() {
        assertThatThrownBy(() -> new TestBehaviorEvidenceValidator(Path.of("does-not-exist.schema.json")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void schemaFileItselfIsValidDraft2020() {
        assertThat(SCHEMA).exists();
    }

    // ------------------------------------------------------------------
    // Document-level refusals
    // ------------------------------------------------------------------

    @Test
    void malformedJsonBytesRefuseClosed() {
        assertRefusal(() -> VALIDATOR.validate("{not json".getBytes()), TestBehaviorErrorCode.INVALID_EVIDENCE);
    }

    @Test
    void nonObjectDocumentRefusesClosed() {
        assertRefusal(() -> VALIDATOR.validate(MAPPER.createArrayNode()), TestBehaviorErrorCode.INVALID_EVIDENCE);
    }

    @Test
    void missingSchemaVersionRefusesClosed() {
        ObjectNode document = validEvidence();
        document.remove("schema_version");
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.UNSUPPORTED_SCHEMA_VERSION);
    }

    @Test
    void unsupportedSchemaVersionRefusesClosed() {
        ObjectNode document = validEvidence();
        document.put("schema_version", "2");
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.UNSUPPORTED_SCHEMA_VERSION);
    }

    @Test
    void structuralSchemaViolationRefusesClosed() {
        ObjectNode document = validEvidence();
        document.remove("test_files");
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.INVALID_EVIDENCE);
    }

    @Test
    void missingProviderProvenanceRefusesClosed() {
        ObjectNode document = validEvidence();
        ((ObjectNode) document.get("provenance")).remove("provider_id");
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.PROVIDER_BINDING_MISSING);
    }

    @Test
    void missingCanonicalRevisionRefusesClosed() {
        ObjectNode document = validEvidence();
        document.remove("canonical_revision");
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.MISSING_SOURCE_REVISION);
    }

    @Test
    void shortCanonicalRevisionRefusesClosed() {
        ObjectNode document = validEvidence();
        document.put("canonical_revision", "818c413");
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.INVALID_SOURCE_REVISION);
    }

    // ------------------------------------------------------------------
    // Digest, path, and source-root refusals
    // ------------------------------------------------------------------

    @Test
    void emptyInputDigestsRefuseClosed() {
        ObjectNode document = validEvidence();
        document.putObject("input_digests");
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.DIGEST_MISMATCH);
    }

    @Test
    void nonHexInputDigestRefusesClosed() {
        ObjectNode document = validEvidence();
        ((ObjectNode) document.get("input_digests")).put(FILE, "not-a-hex-digest");
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.DIGEST_MISMATCH);
    }

    @Test
    void traversalDigestKeyRefusesClosed() {
        ObjectNode document = validEvidence();
        ((ObjectNode) document.get("input_digests")).put("../escaped.txt", DIGEST);
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.MALFORMED_REPOSITORY_PATH);
    }

    @Test
    void emptySourceRootsRefuseClosed() {
        ObjectNode document = validEvidence();
        document.putArray("source_roots");
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.MISSING_SOURCE_ROOT);
    }

    @Test
    void absoluteSourceRootRefusesClosed() {
        ObjectNode document = validEvidence();
        ((ObjectNode) document.get("source_roots").get(0)).put("repository_relative_path", "/etc/passwd");
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.ESCAPED_SOURCE_ROOT);
    }

    @Test
    void traversalSourceRootRefusesClosed() {
        ObjectNode document = validEvidence();
        ((ObjectNode) document.get("source_roots").get(0))
                .put("repository_relative_path", "src/test/../../escaped");
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.ESCAPED_SOURCE_ROOT);
    }

    // ------------------------------------------------------------------
    // Identity and determinism refusals
    // ------------------------------------------------------------------

    @Test
    void duplicatePathMethodIdentityRefusesClosed() {
        ObjectNode document = validEvidence();
        ArrayNode methods = (ArrayNode) document.get("test_files").get(0).get("test_methods");
        methods.add(testMethod(FILE, "exampleTest", 30, 5));
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.DUPLICATE_TEST_IDENTITY);
    }

    @Test
    void duplicateTestFileRefusesClosed() {
        ObjectNode document = validEvidence();
        ((ArrayNode) document.get("test_files")).add(testFile(FILE, "otherTest", 30, 5));
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.DUPLICATE_TEST_IDENTITY);
    }

    @Test
    void testFileWithoutFrozenDigestRefusesClosed() {
        ObjectNode document = validEvidence();
        ((ObjectNode) document.get("input_digests")).remove(FILE);
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.DIGEST_MISMATCH);
    }

    @Test
    void nonHexFileInputDigestRefusesClosed() {
        ObjectNode document = validEvidence();
        ObjectNode file = (ObjectNode) document.get("test_files").get(0);
        file.put("input_digest", "zzz");
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.DIGEST_MISMATCH);
    }

    @Test
    void declarationLocationInOtherFileRefusesClosed() {
        ObjectNode document = validEvidence();
        ObjectNode method = (ObjectNode) document.get("test_files").get(0).get("test_methods").get(0);
        method.set("declaration_location", location("src/test/java/other/OtherTests.java", 20, 5));
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.INVALID_EVIDENCE);
    }

    @Test
    void malformedTestFilePathRefusesClosed() {
        ObjectNode document = validEvidence();
        ObjectNode file = (ObjectNode) document.get("test_files").get(0);
        file.put("repository_relative_path", "/absolute/ExampleTests.java");
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.MALFORMED_REPOSITORY_PATH);
    }

    @Test
    void unsortedTestFilesRefuseClosed() {
        ObjectNode document = validEvidence();
        ArrayNode files = (ArrayNode) document.get("test_files");
        files.add(testFile("src/test/java/b/BTests.java", "bTest", 10, 5));
        files.add(testFile("src/test/java/a/ATests.java", "aTest", 10, 5));
        ObjectNode digests = (ObjectNode) document.get("input_digests");
        digests.put("src/test/java/b/BTests.java", DIGEST);
        digests.put("src/test/java/a/ATests.java", DIGEST);
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.INVALID_EVIDENCE);
    }

    @Test
    void unsortedTestMethodsRefuseClosed() {
        ObjectNode document = validEvidence();
        ArrayNode methods = (ArrayNode) document.get("test_files").get(0).get("test_methods");
        methods.add(testMethod(FILE, "laterTest", 5, 5));
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.INVALID_EVIDENCE);
    }

    @Test
    void unsupportedBehaviorKindRefusesClosedThroughSchema() {
        ObjectNode document = validEvidence();
        ObjectNode action = (ObjectNode) document.get("test_files").get(0)
                .get("test_methods").get(0).get("actions").get(0);
        action.put("kind", "SPECULATION");
        assertRefusal(() -> VALIDATOR.validate(document), TestBehaviorErrorCode.INVALID_EVIDENCE);
    }
}

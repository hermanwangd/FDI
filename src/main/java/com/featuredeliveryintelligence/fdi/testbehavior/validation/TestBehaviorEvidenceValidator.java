package com.featuredeliveryintelligence.fdi.testbehavior.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorErrorCode;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorExtractionException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fail-closed validator for provider-neutral repository test-behavior
 * evidence documents (PKB-BL-009 Slice C / PKB-REVERSE-002). Validates the
 * checked-in {@code contracts/test-behavior-evidence.schema.json} plus the
 * layered mechanical rules the schema cannot express: schema-version
 * support, revision and digest identity, canonical repository paths,
 * escaped source roots, duplicate {@code path#method} identities,
 * same-file location identity, frozen-digest binding per test file, and
 * deterministic ordering. Every refusal reuses the Slice A
 * {@link TestBehaviorErrorCode} vocabulary. Validation is read-only and
 * deterministic: identical documents produce identical reports or identical
 * refusals.
 */
public final class TestBehaviorEvidenceValidator {

    /** Schema version this validator supports; matches the Slice A request contract. */
    public static final String SUPPORTED_SCHEMA_VERSION = "1";

    /** Repository-relative location of the checked-in evidence schema. */
    public static final String DEFAULT_SCHEMA_PATH = "contracts/test-behavior-evidence.schema.json";

    private static final long MAX_EVIDENCE_BYTES = 64L * 1024 * 1024;
    private static final int MAX_REPORTED_SCHEMA_VIOLATIONS = 10;
    private static final int MAX_TEST_FILES = 100_000;
    private static final int MAX_TEST_METHODS = 1_000_000;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final JsonSchema schema;

    /** Builds a validator from an explicit schema file; an absent, unreadable, or malformed schema is a deployment failure, not an evidence refusal. */
    public TestBehaviorEvidenceValidator(Path schemaFile) {
        this.schema = loadSchema(schemaFile);
    }

    /** Builds a validator resolving {@value #DEFAULT_SCHEMA_PATH} under the given repository root. */
    public static TestBehaviorEvidenceValidator fromRepositoryRoot(Path repositoryRoot) {
        return new TestBehaviorEvidenceValidator(repositoryRoot.resolve(DEFAULT_SCHEMA_PATH));
    }

    private static JsonSchema loadSchema(Path schemaFile) {
        try {
            byte[] bytes = Files.readAllBytes(schemaFile);
            JsonNode definition = JSON.readTree(bytes);
            if (definition == null || !definition.isObject()) {
                throw new IllegalStateException("evidence schema is not a JSON object: " + schemaFile);
            }
            // networknt requires an absolute base $id while the checked-in schema
            // keeps a relative one (a valid URI-reference, as python jsonschema
            // accepts). All $refs are same-document fragments, so normalizing the
            // in-memory $id cannot change validation outcomes.
            JsonNode id = definition.get("$id");
            if (id != null && id.isTextual() && !id.asText().matches("^[A-Za-z][A-Za-z0-9+.-]*:.*")) {
                definition = definition.deepCopy();
                ((com.fasterxml.jackson.databind.node.ObjectNode) definition)
                        .put("$id", "urn:fdi:" + DEFAULT_SCHEMA_PATH);
            }
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(definition);
        } catch (IOException | RuntimeException failure) {
            throw new IllegalStateException("evidence schema is unreadable or malformed: " + schemaFile, failure);
        }
    }

    /** Parses and fully validates one evidence document; any violation refuses with a stable Slice A error code. */
    public TestBehaviorEvidenceReport validate(byte[] evidenceBytes) {
        if (evidenceBytes == null) {
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.INVALID_EVIDENCE, "evidence document must not be null");
        }
        if (evidenceBytes.length == 0 || evidenceBytes.length > MAX_EVIDENCE_BYTES) {
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.INVALID_EVIDENCE,
                    "evidence document size is outside the supported bounds: " + evidenceBytes.length + " bytes");
        }
        final JsonNode document;
        try {
            document = JSON.readTree(evidenceBytes);
        } catch (IOException | RuntimeException failure) {
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.INVALID_EVIDENCE, "evidence document is not valid JSON", failure);
        }
        return validate(document);
    }

    /** Fully validates one parsed evidence document; any violation refuses with a stable Slice A error code. */
    public TestBehaviorEvidenceReport validate(JsonNode document) {
        if (document == null || !document.isObject()) {
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.INVALID_EVIDENCE, "evidence document must be a JSON object");
        }
        String schemaVersion = supportedSchemaVersion(document);
        validateAuthority(document);
        String canonicalRevision = validateRevision(document);
        validateInputDigests(document);
        validateSourceRoots(document);
        int testFileCount = 0;
        int testMethodCount = 0;
        for (JsonNode file : requiredArray(document, "test_files", TestBehaviorErrorCode.INVALID_EVIDENCE)) {
            testFileCount++;
            if (testFileCount > MAX_TEST_FILES) {
                throw new TestBehaviorExtractionException(
                        TestBehaviorErrorCode.INVALID_EVIDENCE, "evidence exceeds the supported test file count");
            }
            for (JsonNode method : requiredArray(file, "test_methods", TestBehaviorErrorCode.INVALID_EVIDENCE)) {
                testMethodCount++;
                if (testMethodCount > MAX_TEST_METHODS) {
                    throw new TestBehaviorExtractionException(
                            TestBehaviorErrorCode.INVALID_EVIDENCE, "evidence exceeds the supported test method count");
                }
            }
        }
        validateFileIdentities(document);
        validateOrdering(document);
        validateStructure(document);
        return new TestBehaviorEvidenceReport(
                schemaVersion,
                canonicalRevision,
                testFileCount,
                testMethodCount,
                document.path("incomplete").asBoolean());
    }

    // ------------------------------------------------------------------
    // Layered mechanical checks (beyond the JSON Schema structure)
    // ------------------------------------------------------------------

    private static String supportedSchemaVersion(JsonNode document) {
        JsonNode value = document.get("schema_version");
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.UNSUPPORTED_SCHEMA_VERSION, "schema_version is missing or blank");
        }
        if (!SUPPORTED_SCHEMA_VERSION.equals(value.asText())) {
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.UNSUPPORTED_SCHEMA_VERSION,
                    "unsupported schema version: " + value.asText() + " (supported: " + SUPPORTED_SCHEMA_VERSION + ")");
        }
        return value.asText();
    }

    private void validateStructure(JsonNode document) {
        List<String> violations;
        try {
            violations = schema.validate(document).stream()
                    .map(message -> message.getMessage())
                    .sorted()
                    .toList();
        } catch (RuntimeException failure) {
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.INVALID_EVIDENCE, "evidence schema validation failed", failure);
        }
        if (!violations.isEmpty()) {
            String detail = String.join("; ", violations.subList(0, Math.min(violations.size(), MAX_REPORTED_SCHEMA_VIOLATIONS)));
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.INVALID_EVIDENCE,
                    "evidence document violates the schema (" + violations.size() + " violation(s)): " + detail);
        }
    }

    private static void validateAuthority(JsonNode document) {
        JsonNode provenance = document.get("provenance");
        if (provenance == null || !provenance.isObject()) {
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.PROVIDER_BINDING_MISSING, "provenance is missing");
        }
        for (String field : List.of("provider_id", "extractor_name", "extractor_version")) {
            JsonNode value = provenance.get(field);
            if (value == null || !value.isTextual() || value.asText().isBlank()) {
                throw new TestBehaviorExtractionException(
                        TestBehaviorErrorCode.PROVIDER_BINDING_MISSING, "provenance " + field + " is missing or blank");
            }
        }
        JsonNode repositoryId = document.get("repository_id");
        if (repositoryId == null || !repositoryId.isTextual() || repositoryId.asText().isBlank()) {
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.PROVIDER_BINDING_MISSING, "repository_id is missing or blank");
        }
    }

    private static String validateRevision(JsonNode document) {
        JsonNode value = document.get("canonical_revision");
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.MISSING_SOURCE_REVISION, "canonical_revision is missing or blank");
        }
        if (!value.asText().matches("(?i)[0-9a-f]{40}|[0-9a-f]{64}")) {
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.INVALID_SOURCE_REVISION,
                    "canonical_revision must be a full Git object id: " + value.asText());
        }
        return value.asText();
    }

    private static void validateInputDigests(JsonNode document) {
        JsonNode digests = document.get("input_digests");
        if (digests == null || !digests.isObject() || digests.isEmpty()) {
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.DIGEST_MISMATCH, "frozen input_digests must not be empty");
        }
        List<String> keys = new ArrayList<>();
        digests.properties().forEach(entry -> keys.add(entry.getKey()));
        keys.sort(String::compareTo);
        for (String key : keys) {
            if (canonicalRelative(key) == null) {
                throw new TestBehaviorExtractionException(
                        TestBehaviorErrorCode.MALFORMED_REPOSITORY_PATH,
                        "input digest key must be a canonical repository-relative path: " + key);
            }
            JsonNode value = digests.get(key);
            if (value == null || !value.isTextual() || !isHexDigest(value.asText())) {
                throw new TestBehaviorExtractionException(
                        TestBehaviorErrorCode.DIGEST_MISMATCH, "input digest for " + key + " must be a hex digest");
            }
        }
    }

    private static void validateSourceRoots(JsonNode document) {
        List<JsonNode> roots = requiredArray(document, "source_roots", TestBehaviorErrorCode.MISSING_SOURCE_ROOT);
        if (roots.isEmpty()) {
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.MISSING_SOURCE_ROOT, "at least one source root is required");
        }
        for (JsonNode root : roots) {
            JsonNode kind = root.get("kind");
            if (kind == null || !kind.isTextual()
                    || (!kind.asText().equals("PRODUCTION") && !kind.asText().equals("TEST"))) {
                throw new TestBehaviorExtractionException(
                        TestBehaviorErrorCode.MISSING_SOURCE_ROOT,
                        "source root kind must be PRODUCTION or TEST: " + (kind == null ? null : kind.asText()));
            }
            JsonNode path = root.get("repository_relative_path");
            if (path == null || !path.isTextual() || canonicalRelative(path.asText()) == null) {
                throw new TestBehaviorExtractionException(
                        TestBehaviorErrorCode.ESCAPED_SOURCE_ROOT,
                        "source root path escapes the repository or is malformed: " + (path == null ? null : path.asText()));
            }
        }
    }

    /** Duplicate file paths and duplicate {@code path#method} identities fail closed. */
    private static void validateFileIdentities(JsonNode document) {
        JsonNode digests = document.get("input_digests");
        java.util.Set<String> filePaths = new java.util.TreeSet<>();
        java.util.Set<String> identities = new java.util.TreeSet<>();
        for (JsonNode file : requiredArray(document, "test_files", TestBehaviorErrorCode.INVALID_EVIDENCE)) {
            String path = file.path("repository_relative_path").asText();
            if (canonicalRelative(path) == null) {
                throw new TestBehaviorExtractionException(
                        TestBehaviorErrorCode.MALFORMED_REPOSITORY_PATH,
                        "test file path must be a canonical repository-relative path: " + path);
            }
            if (!filePaths.add(path)) {
                throw new TestBehaviorExtractionException(
                        TestBehaviorErrorCode.DUPLICATE_TEST_IDENTITY, "duplicate test file identity: " + path);
            }
            if (!digests.has(path)) {
                throw new TestBehaviorExtractionException(
                        TestBehaviorErrorCode.DIGEST_MISMATCH, "no frozen input digest bound for test file: " + path);
            }
            JsonNode inputDigest = file.get("input_digest");
            if (inputDigest == null || !inputDigest.isTextual() || !isHexDigest(inputDigest.asText())) {
                throw new TestBehaviorExtractionException(
                        TestBehaviorErrorCode.DIGEST_MISMATCH, "input_digest for " + path + " must be a hex digest");
            }
            validateSameFileLocations(path, file);
            for (JsonNode method : requiredArray(file, "test_methods", TestBehaviorErrorCode.INVALID_EVIDENCE)) {
                String identity = path + "#" + method.path("method_name").asText();
                if (!identities.add(identity)) {
                    throw new TestBehaviorExtractionException(
                            TestBehaviorErrorCode.DUPLICATE_TEST_IDENTITY, "duplicate test identity: " + identity);
                }
            }
        }
    }

    /** Every nested location belongs to the containing test file; anything else is an identity mismatch. */
    private static void validateSameFileLocations(String filePath, JsonNode file) {
        for (JsonNode method : requiredArray(file, "test_methods", TestBehaviorErrorCode.INVALID_EVIDENCE)) {
            requireSameFile(filePath, method.path("declaration_location"), "declaration location");
            for (String channel : List.of("fixtures", "actions", "assertions")) {
                for (JsonNode observation : requiredArray(method, channel, TestBehaviorErrorCode.INVALID_EVIDENCE)) {
                    requireSameFile(filePath, observation.path("location"), channel + " observation location");
                }
            }
            for (JsonNode unresolved : requiredArray(method, "unresolved_references", TestBehaviorErrorCode.INVALID_EVIDENCE)) {
                requireSameFile(filePath, unresolved.path("location"), "unresolved reference location");
            }
        }
        for (JsonNode unresolved : requiredArray(file, "unresolved_references", TestBehaviorErrorCode.INVALID_EVIDENCE)) {
            requireSameFile(filePath, unresolved.path("location"), "file unresolved reference location");
        }
    }

    private static void requireSameFile(String filePath, JsonNode location, String what) {
        String locationPath = location.path("repository_relative_path").asText();
        if (!filePath.equals(locationPath)) {
            throw new TestBehaviorExtractionException(
                    TestBehaviorErrorCode.INVALID_EVIDENCE,
                    what + " identity mismatch: " + locationPath + " is outside " + filePath);
        }
    }

    /** Test files and methods must be deterministically ordered as the contract records order them. */
    private static void validateOrdering(JsonNode document) {
        List<JsonNode> files = requiredArray(document, "test_files", TestBehaviorErrorCode.INVALID_EVIDENCE);
        String previousPath = null;
        for (JsonNode file : files) {
            String path = file.path("repository_relative_path").asText();
            if (previousPath != null && previousPath.compareTo(path) >= 0) {
                throw new TestBehaviorExtractionException(
                        TestBehaviorErrorCode.INVALID_EVIDENCE,
                        "test files are not ordered by repository-relative path: " + previousPath + " then " + path);
            }
            previousPath = path;
            JsonNode previousLocation = null;
            for (JsonNode method : requiredArray(file, "test_methods", TestBehaviorErrorCode.INVALID_EVIDENCE)) {
                JsonNode location = method.path("declaration_location");
                if (previousLocation != null && compareLocations(previousLocation, location) > 0) {
                    throw new TestBehaviorExtractionException(
                            TestBehaviorErrorCode.INVALID_EVIDENCE,
                            "test methods in " + path + " are not ordered by declaration location");
                }
                previousLocation = location;
            }
        }
    }

    private static int compareLocations(JsonNode left, JsonNode right) {
        int line = Integer.compare(left.path("line").asInt(), right.path("line").asInt());
        return line != 0 ? line : Integer.compare(left.path("column").asInt(), right.path("column").asInt());
    }

    // ------------------------------------------------------------------
    // Shared helpers
    // ------------------------------------------------------------------

    private static List<JsonNode> requiredArray(JsonNode node, String field, TestBehaviorErrorCode code) {
        JsonNode value = node.get(field);
        if (value == null || !value.isArray()) {
            throw new TestBehaviorExtractionException(code, field + " must be an array");
        }
        List<JsonNode> items = new ArrayList<>();
        value.forEach(items::add);
        return items;
    }

    private static boolean isHexDigest(String value) {
        return value.toLowerCase(Locale.ROOT).matches("[0-9a-f]{32}|[0-9a-f]{40}|[0-9a-f]{64}");
    }

    /** Same canonical repository-relative path rule as the Slice A contract and the next-run gate. */
    static String canonicalRelative(String text) {
        if (text == null || text.isEmpty() || text.isBlank()
                || text.indexOf('\\') >= 0 || text.indexOf('\0') >= 0
                || text.startsWith("/") || text.matches("^[A-Za-z]:.*")) {
            return null;
        }
        for (String part : text.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) {
                return null;
            }
        }
        return text;
    }
}

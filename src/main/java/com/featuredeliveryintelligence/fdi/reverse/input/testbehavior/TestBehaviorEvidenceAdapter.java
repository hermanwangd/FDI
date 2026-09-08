package com.featuredeliveryintelligence.fdi.reverse.input.testbehavior;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseContractValidation;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import com.featuredeliveryintelligence.fdi.reverse.ReverseJson;
import com.featuredeliveryintelligence.fdi.reverse.evidence.EvidenceChannelRecord;
import com.featuredeliveryintelligence.fdi.reverse.evidence.ReverseEvidenceChannel;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorErrorCode;
import com.featuredeliveryintelligence.fdi.testbehavior.api.TestBehaviorExtractionException;
import com.featuredeliveryintelligence.fdi.testbehavior.validation.TestBehaviorEvidenceValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Slice C test-behavior input adapter (PKB-BL-009 / PKB-REVERSE-002). Imports
 * the accepted Java extractor evidence — the immutable evidence package
 * envelope and the extractor evidence document it pins — into the Slice A
 * {@link EvidenceChannelRecord} contract for the {@code TEST_BEHAVIOR} channel.
 *
 * <p>Both input files are verified before use: each is read from the
 * repository root through a traversal-free repository-relative path, its
 * SHA-256 is recomputed and compared with the caller-bound digest, and the
 * evidence document is structurally validated by the already-reviewed
 * {@link TestBehaviorEvidenceValidator}. The package envelope and the evidence
 * document must then agree on repository identity, canonical revision,
 * provider provenance, schema version, evidence file binding, byte size, and
 * observation counts; any contradiction fails closed.
 *
 * <p>Resolved and unresolved observations are preserved exactly as recorded —
 * nothing is dropped, resolved, or re-ordered — and every source location
 * stays in the document as observation metadata so downstream citations remain
 * traceable. The observation payload is canonicalized through
 * {@link ReverseJson#canonicalize} so repeated loads of the same inputs expose
 * byte-identical, key-sorted documents: no wall-clock, locale, HashMap-order,
 * or randomness dependence. Test-behavior evidence is historical evidence,
 * not Product truth.
 *
 * <p>Every refusal raises {@link ReverseContractException} with exactly one
 * stable {@link ReverseFailure} code.
 */
public final class TestBehaviorEvidenceAdapter {

    /** Repository-relative path of the accepted immutable evidence package envelope. */
    public static final String ACCEPTED_PACKAGE_PATH =
            "validation/pkb001/reverse-pkb-bl009-petclinic-001/java-test-behavior/evidence-package.json";

    /** Frozen SHA-256 of {@link #ACCEPTED_PACKAGE_PATH}. */
    public static final String ACCEPTED_PACKAGE_SHA256 =
            "082c621945ef063fec82adb3766348c0ecc888ba71a2f1ed34b147ebfa8bcc8a";

    /** Repository-relative path of the accepted extractor evidence document. */
    public static final String ACCEPTED_EVIDENCE_PATH =
            "validation/pkb001/reverse-pkb-bl009-petclinic-001/java-test-behavior/evidence.json";

    /** Frozen SHA-256 of {@link #ACCEPTED_EVIDENCE_PATH}. */
    public static final String ACCEPTED_EVIDENCE_SHA256 =
            "4db3b4fcac22c704f321e2a3bef20091741147984815addf3ebf77d88643e66d";

    private static final ObjectMapper JSON = new ObjectMapper();

    private TestBehaviorEvidenceAdapter() { }

    /**
     * Loads the accepted Petclinic test-behavior evidence into one
     * {@code TEST_BEHAVIOR} channel record, verifying both frozen digests.
     */
    public static EvidenceChannelRecord loadAccepted(Path repositoryRoot) {
        return load(
                repositoryRoot,
                ACCEPTED_PACKAGE_PATH, ACCEPTED_PACKAGE_SHA256,
                ACCEPTED_EVIDENCE_PATH, ACCEPTED_EVIDENCE_SHA256);
    }

    /**
     * Loads one evidence package envelope and the evidence document it pins
     * into one {@code TEST_BEHAVIOR} channel record. Fail-closed at every
     * step; see the class documentation for the checked identities.
     */
    public static EvidenceChannelRecord load(
            Path repositoryRoot,
            String packagePath, String packageSha256,
            String evidencePath, String evidenceSha256) {
        Path root = requireRepositoryRoot(repositoryRoot);
        String packageRel = ReverseContractValidation.repositoryPath(packagePath, "evidence package path");
        String evidenceRel = ReverseContractValidation.repositoryPath(evidencePath, "test-behavior evidence path");
        String packageDigest =
                ReverseContractValidation.sha256Hex(packageSha256, "evidence package " + packageRel);
        String evidenceDigest =
                ReverseContractValidation.sha256Hex(evidenceSha256, "test-behavior evidence " + evidenceRel);

        byte[] packageBytes = readVerified(root, packageRel, packageDigest);
        byte[] evidenceBytes = readVerified(root, evidenceRel, evidenceDigest);

        JsonNode packageDocument = parseObject(packageBytes, "evidence package " + packageRel);
        JsonNode evidenceDocument = parseObject(evidenceBytes, "test-behavior evidence " + evidenceRel);

        validateEvidenceStructure(root, evidenceRel, evidenceBytes);
        JsonNode sourceBinding = requiredObject(packageDocument, "source_binding", packageRel);
        JsonNode packageEvidence = requiredObject(packageDocument, "evidence", packageRel);
        String packageRepository = ReverseContractValidation.requiredText(
                textField(sourceBinding, "repository_id", "source_binding"), "package source repository id");
        String packageRevision = ReverseContractValidation.canonicalRevision(
                textField(sourceBinding, "canonical_revision", "source_binding"));
        String packageProvider = ReverseContractValidation.requiredText(
                textField(packageEvidence, "provider_id", "evidence"), "package evidence provider id");
        String packageFile = ReverseContractValidation.repositoryPath(
                textField(packageEvidence, "file", "evidence"), "package evidence file");
        String packageFileDigest = ReverseContractValidation.sha256Hex(
                textField(packageEvidence, "sha256", "evidence"), "package evidence sha256");
        String packageSchemaVersion = ReverseContractValidation.requiredText(
                textField(packageEvidence, "schema_version", "evidence"), "package evidence schema version");
        long packageBytesBound = longField(packageEvidence, "bytes", "evidence");
        long packageTestFiles = longField(packageEvidence, "test_files", "evidence");
        long packageTestMethods = longField(packageEvidence, "test_methods", "evidence");

        // ---- package/evidence agreement: any contradiction fails closed ----
        if (!packageFile.equals(evidenceRel))
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT,
                    "package/evidence contradiction: package binds evidence file " + packageFile
                            + " but " + evidenceRel + " was loaded");
        if (packageBytesBound != evidenceBytes.length)
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT,
                    "package/evidence contradiction: package binds " + packageBytesBound
                            + " evidence bytes but " + evidenceRel + " is " + evidenceBytes.length + " bytes");
        if (!packageFileDigest.equals(evidenceDigest))
            throw new ReverseContractException(
                    ReverseFailure.DIGEST_MISMATCH,
                    "package binds evidence digest " + packageFileDigest + " but " + evidenceRel
                            + " recomputes to " + evidenceDigest);
        if (!packageRepository.equals(evidenceDocument.path("repository_id").asText()))
            throw new ReverseContractException(
                    ReverseFailure.DUPLICATE_IDENTITY,
                    "package/evidence repository identity disagreement: package binds " + packageRepository
                            + " but evidence records " + evidenceDocument.path("repository_id").asText());
        if (!packageRevision.equalsIgnoreCase(evidenceDocument.path("canonical_revision").asText()))
            throw new ReverseContractException(
                    ReverseFailure.REVISION_MISMATCH,
                    "package/evidence revision disagreement: package binds " + packageRevision
                            + " but evidence binds " + evidenceDocument.path("canonical_revision").asText());
        if (!packageProvider.equals(evidenceDocument.path("provenance").path("provider_id").asText()))
            throw new ReverseContractException(
                    ReverseFailure.DUPLICATE_IDENTITY,
                    "package/evidence provider provenance disagreement: package binds " + packageProvider
                            + " but evidence records "
                            + evidenceDocument.path("provenance").path("provider_id").asText());
        if (!packageSchemaVersion.equals(evidenceDocument.path("schema_version").asText()))
            throw new ReverseContractException(
                    ReverseFailure.UNSUPPORTED_SCHEMA_VERSION,
                    "package/evidence schema-version disagreement: package binds " + packageSchemaVersion
                            + " but evidence records " + evidenceDocument.path("schema_version").asText());

        JsonNode files = evidenceDocument.path("test_files");
        long fileCount = files.size();
        if (packageTestFiles != fileCount)
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT,
                    "package/evidence contradiction: package declares " + packageTestFiles
                            + " test files but evidence records " + fileCount);
        long methods = 0;
        for (JsonNode file : files) methods += file.path("test_methods").size();
        if (packageTestMethods != methods)
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT,
                    "package/evidence contradiction: package declares " + packageTestMethods
                            + " test methods but evidence records " + methods);

        // ---- bind the accepted observations into the Slice A contract ----
        String providerId = evidenceDocument.path("provenance").path("provider_id").asText();
        String provenance = evidenceDocument.path("provenance").path("extractor_name").asText()
                + ":" + evidenceDocument.path("provenance").path("extractor_version").asText()
                + " via " + packageRel + "@" + packageDigest;
        return new EvidenceChannelRecord(
                ReverseEvidenceChannel.TEST_BEHAVIOR,
                evidenceDocument.path("repository_id").asText(),
                evidenceDocument.path("canonical_revision").asText().toLowerCase(Locale.ROOT),
                evidenceRel,
                evidenceDigest,
                evidenceDocument.path("schema_version").asText(),
                providerId,
                provenance,
                ReverseJson.canonicalize(evidenceDocument));
    }

    private static Path requireRepositoryRoot(Path repositoryRoot) {
        if (repositoryRoot == null || !Files.isDirectory(repositoryRoot))
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "repository root must be a readable directory");
        return repositoryRoot.toAbsolutePath().normalize();
    }

    /** Reads one bound input, refusing escaped paths, missing files, and digest mismatches. */
    private static byte[] readVerified(Path root, String relativePath, String boundDigest) {
        Path input = root.resolve(relativePath).normalize();
        if (!input.startsWith(root) || !Files.isRegularFile(input))
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "input is not a readable file: " + relativePath);
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(input);
        } catch (IOException failure) {
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "input is unreadable: " + relativePath);
        }
        String recomputed = sha256(bytes);
        if (!recomputed.equals(boundDigest))
            throw new ReverseContractException(
                    ReverseFailure.DIGEST_MISMATCH,
                    "digest mismatch for " + relativePath + ": bound " + boundDigest
                            + " but recomputed " + recomputed);
        return bytes;
    }

    private static JsonNode parseObject(byte[] bytes, String what) {
        final JsonNode document;
        try {
            document = JSON.readTree(bytes);
        } catch (IOException | RuntimeException failure) {
            throw new ReverseContractException(ReverseFailure.MISSING_INPUT, what + " is not valid JSON");
        }
        if (document == null || !document.isObject())
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, what + " must be a JSON object");
        return document;
    }

    /** Runs the already-reviewed structural validator; every refusal maps to the Slice A vocabulary. */
    private static void validateEvidenceStructure(Path root, String evidenceRel, byte[] evidenceBytes) {
        try {
            TestBehaviorEvidenceValidator.fromRepositoryRoot(root).validate(evidenceBytes);
        } catch (TestBehaviorExtractionException failure) {
            throw new ReverseContractException(
                    mapFailure(failure.code()), evidenceRel + ": " + failure.getMessage());
        }
    }

    /** Deterministic one-to-one mapping from the extractor refusal vocabulary to {@link ReverseFailure}. */
    private static ReverseFailure mapFailure(TestBehaviorErrorCode code) {
        return switch (code) {
            case MISSING_SOURCE_REVISION, MISSING_SOURCE_ROOT, PROVIDER_BINDING_MISSING, INVALID_EVIDENCE ->
                    ReverseFailure.MISSING_INPUT;
            case INVALID_SOURCE_REVISION -> ReverseFailure.REVISION_MISMATCH;
            case ESCAPED_SOURCE_ROOT, MALFORMED_REPOSITORY_PATH -> ReverseFailure.MALFORMED_PATH;
            case DIGEST_MISMATCH -> ReverseFailure.DIGEST_MISMATCH;
            case DUPLICATE_TEST_IDENTITY -> ReverseFailure.DUPLICATE_IDENTITY;
            case UNSUPPORTED_SCHEMA_VERSION -> ReverseFailure.UNSUPPORTED_SCHEMA_VERSION;
        };
    }

    private static JsonNode requiredObject(JsonNode parent, String field, String what) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isObject())
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, what + " is missing " + field + " object");
        return value;
    }

    private static String textField(JsonNode parent, String field, String section) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank())
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, section + "." + field + " is missing or blank");
        return value.asText();
    }

    private static long longField(JsonNode parent, String field, String section) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isIntegralNumber() || value.asLong() < 0)
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, section + "." + field + " is missing or not a non-negative integer");
        return value.asLong();
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder hex = new StringBuilder(64);
            for (byte value : digest.digest(bytes)) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is not available", failure);
        }
    }
}

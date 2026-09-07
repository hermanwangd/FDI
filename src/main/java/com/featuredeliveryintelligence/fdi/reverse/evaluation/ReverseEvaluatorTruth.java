package com.featuredeliveryintelligence.fdi.reverse.evaluation;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseContractValidation;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Frozen evaluator surface for evaluator-only comparison (PKB-BL-009 slice F).
 * Binds the sealed evaluator gold mappings and their ground-truth seal,
 * verifies seal integrity and evaluator-only isolation status, and exposes
 * evaluator capabilities strictly as comparison input.
 *
 * <p>This class is reachable only from the evaluation path after the proposal
 * package is sealed. It fails closed when the gold digest disagrees with the
 * seal, when the seal is not {@code SEALED}, when generation isolation is not
 * {@code DENIED}/verified, or when the gold document does not declare
 * {@code EVALUATOR_ONLY_FROZEN} / {@code EVALUATOR_TRUTH_ONLY} authority.
 * Comparison output derived from this surface must never be consumed by
 * proposal generation.
 *
 * @param mappingSetId evaluator mapping set identity, agreed by gold and seal
 * @param sealId ground-truth seal identity
 * @param goldPath repository-relative path of the frozen gold mappings
 * @param goldSha256 SHA-256 of the gold document, verified against the seal
 * @param sourceCommitSha exact source revision the evaluator truth belongs to
 * @param capabilities evaluator capabilities with exact expected components
 */
public record ReverseEvaluatorTruth(
        String mappingSetId,
        String sealId,
        String goldPath,
        String goldSha256,
        String sourceCommitSha,
        List<TruthCapability> capabilities) {

    /** Repository-relative path of the frozen evaluator gold mappings. */
    public static final String GOLD_PATH =
            "validation/pkb001/evaluator/petclinic-818c413/gold-mappings.json";
    /** Repository-relative path of the ground-truth seal. */
    public static final String SEAL_PATH =
            "validation/pkb001/evaluator/petclinic-818c413/ground-truth-seal.json";

    private static final ObjectMapper JSON = new ObjectMapper();

    /** One evaluator expected component as a normalized identity row. */
    public record TruthComponent(String sourcePath, String containingType, String qualifiedSymbol) {

        public TruthComponent {
            sourcePath = ReverseContractValidation.repositoryPath(sourcePath, "evaluator component source path");
            containingType = ReverseContractValidation.requiredText(
                    containingType, "evaluator component containing type");
            qualifiedSymbol = ReverseContractValidation.requiredText(
                    qualifiedSymbol, "evaluator component qualified symbol");
        }
    }

    /** One evaluator capability with its exact expected components. */
    public record TruthCapability(String capabilityId, String capabilityName, List<TruthComponent> components) {

        public TruthCapability {
            capabilityId = ReverseContractValidation.requiredText(capabilityId, "evaluator capability id");
            capabilityName = ReverseContractValidation.requiredText(
                    capabilityName, "evaluator capability name");
            components = List.copyOf(components == null ? List.of() : components);
        }
    }

    public ReverseEvaluatorTruth {
        mappingSetId = ReverseContractValidation.requiredText(mappingSetId, "evaluator mapping set id");
        sealId = ReverseContractValidation.requiredText(sealId, "evaluator seal id");
        goldPath = ReverseContractValidation.repositoryPath(goldPath, "evaluator gold path");
        goldSha256 = ReverseContractValidation.sha256Hex(goldSha256, "evaluator gold document");
        sourceCommitSha = ReverseContractValidation.canonicalRevision(sourceCommitSha);
        capabilities = List.copyOf(capabilities == null ? List.of() : capabilities);
        if (capabilities.isEmpty())
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "evaluator surface must contain at least one capability");
    }

    /**
     * Loads the frozen evaluator surface from the repository root and verifies
     * it end to end: seal status, isolation controls, gold authority, gold
     * digest against the seal, mapping-set identity agreement, and evaluator
     * component shape. Any deviation fails closed.
     */
    public static ReverseEvaluatorTruth load(Path repositoryRoot) {
        if (repositoryRoot == null)
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "repository root must not be null");

        JsonNode seal = readObject(repositoryRoot.resolve(SEAL_PATH), "ground-truth seal");
        String sealId = ReverseContractValidation.requiredText(
                seal.path("seal_id").isTextual() ? seal.get("seal_id").asText() : null,
                "ground-truth seal id");
        if (!"SEALED".equals(text(seal, "status", "ground-truth seal status")))
            throw new ReverseContractException(
                    ReverseFailure.EVALUATOR_LEAKAGE,
                    "evaluator surface seal status must be SEALED but was " + seal.path("status"));
        if (!"VERIFIED".equals(text(seal, "isolation_status", "ground-truth seal isolation status")))
            throw new ReverseContractException(
                    ReverseFailure.EVALUATOR_LEAKAGE,
                    "evaluator surface isolation must be VERIFIED but was " + seal.path("isolation_status"));
        JsonNode isolationControls = seal.path("isolation_controls");
        if (!isolationControls.isObject()
                || !"DENIED".equals(text(isolationControls, "generation_access", "seal generation access")))
            throw new ReverseContractException(
                    ReverseFailure.EVALUATOR_LEAKAGE,
                    "evaluator surface generation access must remain DENIED");
        String sealMappingSetId = ReverseContractValidation.requiredText(
                seal.path("mapping_set_id").isTextual() ? seal.get("mapping_set_id").asText() : null,
                "ground-truth seal mapping set id");
        String sealGoldSha256 = ReverseContractValidation.sha256Hex(
                seal.path("gold_sha256").isTextual() ? seal.get("gold_sha256").asText() : null,
                "ground-truth seal gold digest");
        String sealSourceCommit = ReverseContractValidation.canonicalRevision(
                seal.path("source_commit_sha").isTextual() ? seal.get("source_commit_sha").asText() : null);

        Path goldFile = repositoryRoot.resolve(GOLD_PATH);
        byte[] goldBytes = readBytes(goldFile, "evaluator gold mappings");
        String goldSha256 = sha256Hex(goldBytes);
        if (!sealGoldSha256.equals(goldSha256))
            throw new ReverseContractException(
                    ReverseFailure.DIGEST_MISMATCH,
                    "evaluator gold digest disagrees with the seal: sealed " + sealGoldSha256
                            + " but recomputed " + goldSha256);

        JsonNode gold = readObject(goldFile, "evaluator gold mappings");
        if (!"EVALUATOR_ONLY_FROZEN".equals(text(gold, "status", "evaluator gold status")))
            throw new ReverseContractException(
                    ReverseFailure.EVALUATOR_LEAKAGE,
                    "evaluator gold status must be EVALUATOR_ONLY_FROZEN but was " + gold.path("status"));
        if (!"EVALUATOR_TRUTH_ONLY".equals(text(gold, "authority", "evaluator gold authority")))
            throw new ReverseContractException(
                    ReverseFailure.EVALUATOR_LEAKAGE,
                    "evaluator gold authority must be EVALUATOR_TRUTH_ONLY but was " + gold.path("authority"));
        String goldMappingSetId = ReverseContractValidation.requiredText(
                gold.path("mapping_set_id").isTextual() ? gold.get("mapping_set_id").asText() : null,
                "evaluator gold mapping set id");
        if (!sealMappingSetId.equals(goldMappingSetId))
            throw new ReverseContractException(
                    ReverseFailure.DIGEST_MISMATCH,
                    "evaluator gold mapping set " + goldMappingSetId
                            + " disagrees with sealed mapping set " + sealMappingSetId);
        String goldSourceCommit = ReverseContractValidation.canonicalRevision(
                gold.path("source_commit_sha").isTextual() ? gold.get("source_commit_sha").asText() : null);
        if (!sealSourceCommit.equals(goldSourceCommit))
            throw new ReverseContractException(
                    ReverseFailure.REVISION_MISMATCH,
                    "evaluator gold source revision disagrees with the sealed source revision");

        JsonNode mappings = gold.path("mappings");
        if (!mappings.isArray() || mappings.isEmpty())
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "evaluator gold must contain a non-empty mappings array");

        List<TruthCapability> capabilities = new ArrayList<>();
        for (int index = 0; index < mappings.size(); index++) {
            JsonNode mapping = mappings.get(index);
            String where = "evaluator mapping at /mappings/" + index;
            if (!mapping.isObject())
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT, where + " must be an object");
            String capabilityId = ReverseContractValidation.requiredText(
                    mapping.path("capability_id").isTextual() ? mapping.get("capability_id").asText() : null,
                    where + " capability_id");
            String capabilityName = ReverseContractValidation.requiredText(
                    mapping.path("capability_name").isTextual() ? mapping.get("capability_name").asText() : null,
                    where + " capability_name");
            JsonNode components = mapping.path("expected_components");
            if (!components.isArray() || components.isEmpty())
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT,
                        where + " must contain a non-empty expected_components array");
            List<TruthComponent> expected = new ArrayList<>();
            for (int component = 0; component < components.size(); component++) {
                String componentWhere = where + "/expected_components/" + component;
                JsonNode node = components.get(component);
                if (!node.isObject())
                    throw new ReverseContractException(
                            ReverseFailure.MISSING_INPUT, componentWhere + " must be an object");
                String sourcePath = ReverseContractValidation.repositoryPath(
                        node.path("source_path").isTextual() ? node.get("source_path").asText() : null,
                        componentWhere + " source_path");
                String graphNodeId = ReverseContractValidation.requiredText(
                        node.path("graph_node_id").isTextual() ? node.get("graph_node_id").asText() : null,
                        componentWhere + " graph_node_id");
                if (!node.path("source_location").isTextual()
                        || node.get("source_location").asText().isBlank())
                    throw new ReverseContractException(
                            ReverseFailure.MISSING_INPUT, componentWhere + " must record a source_location");
                expected.add(new TruthComponent(sourcePath, fileStem(sourcePath), graphNodeId));
            }
            capabilities.add(new TruthCapability(capabilityId, capabilityName, expected));
        }

        return new ReverseEvaluatorTruth(
                goldMappingSetId, sealId, GOLD_PATH, goldSha256, goldSourceCommit, capabilities);
    }

    private static JsonNode readObject(Path file, String what) {
        byte[] bytes = readBytes(file, what);
        try {
            JsonNode node = JSON.readTree(bytes);
            if (!node.isObject())
                throw new ReverseContractException(
                        ReverseFailure.MISSING_INPUT, what + " must be a JSON object at " + file);
            return node;
        } catch (IOException failure) {
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, what + " must be parseable JSON at " + file);
        }
    }

    private static byte[] readBytes(Path file, String what) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException failure) {
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, what + " is not readable at " + file);
        }
    }

    private static String text(JsonNode node, String field, String what) {
        JsonNode value = node.path(field);
        if (!value.isTextual())
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, what + " must be a plain string");
        return value.asText();
    }

    /** File stem of a repository-relative path, without directory or extension. */
    static String fileStem(String path) {
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder hex = new StringBuilder();
            for (byte value : digest.digest(bytes)) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is not available", failure);
        }
    }
}

package com.featuredeliveryintelligence.fdi.reverse.evidence;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractException;
import com.featuredeliveryintelligence.fdi.reverse.ReverseContractValidation;
import com.featuredeliveryintelligence.fdi.reverse.ReverseFailure;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;

/**
 * One bound evidence channel of a {@link ReverseEvidenceBundle}. The record
 * pins the exact repository identity, canonical revision, repository-relative
 * input path, frozen SHA-256 digest, schema version, and provider provenance
 * of one evidence input, plus its parsed observation payload.
 *
 * <p>The {@code observations} payload is opaque to this contract: STRUCTURAL,
 * TEST_BEHAVIOR, and DELIVERY_HISTORY adapters (slices B/C/D) place their own
 * normalized observation trees here without changing this record. The payload
 * is deep-copied on construction and on read so a bundle stays immutable.
 */
public record EvidenceChannelRecord(
        ReverseEvidenceChannel channel,
        String repositoryId,
        String canonicalRevision,
        String inputPath,
        String inputSha256,
        String schemaVersion,
        String providerId,
        String provenance,
        JsonNode observations) {

    public EvidenceChannelRecord {
        if (channel == null)
            throw new ReverseContractException(ReverseFailure.MISSING_INPUT, "evidence channel id must not be null");
        repositoryId = ReverseContractValidation.requiredText(repositoryId, "channel repository id");
        canonicalRevision = ReverseContractValidation.canonicalRevision(canonicalRevision);
        inputPath = ReverseContractValidation.repositoryPath(inputPath, "channel input path");
        inputSha256 = ReverseContractValidation.sha256Hex(inputSha256, "channel input " + inputPath);
        schemaVersion = ReverseContractValidation.supportedSchemaVersion(
                schemaVersion, ReverseEvidenceBundle.SUPPORTED_SCHEMA_VERSION);
        providerId = ReverseContractValidation.requiredText(providerId, "channel provider id");
        provenance = ReverseContractValidation.requiredText(provenance, "channel provenance");
        if (observations == null || observations instanceof MissingNode)
            throw new ReverseContractException(
                    ReverseFailure.MISSING_INPUT, "channel observations must not be absent: " + channel);
        observations = observations.deepCopy();
    }

    /** Defensive copy: callers cannot mutate the parsed observation payload through the accessor. */
    @Override
    public JsonNode observations() {
        return observations.deepCopy();
    }
}

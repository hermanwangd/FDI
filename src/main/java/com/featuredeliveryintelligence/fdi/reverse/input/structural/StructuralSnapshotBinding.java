package com.featuredeliveryintelligence.fdi.reverse.input.structural;

import com.featuredeliveryintelligence.fdi.reverse.ReverseContractValidation;

/**
 * Exact binding of the one accepted Graphify structural snapshot for the
 * reverse experiment (PKB-BL-009 Slice B). The binding pins the analyzed
 * repository identity, the bound target revision the snapshot must agree
 * with, the repository-relative snapshot path, the frozen SHA-256 digest,
 * and the external provider provenance. All fields are validated at
 * construction with the shared fail-closed {@link com.featuredeliveryintelligence.fdi.reverse.ReverseFailure}
 * vocabulary; file presence, digest recomputation, and revision agreement
 * are verified separately by the adapter so binding and verification
 * identities stay distinct.
 *
 * @param repositoryId   analyzed repository identity shared by all evidence channels
 * @param targetRevision full Git object id the snapshot revision must equal
 * @param inputPath      repository-relative, traversal-free snapshot path
 * @param inputSha256    frozen SHA-256 of the snapshot bytes
 * @param providerId     external structural provider identity (Graphify)
 * @param provenance     free-text provider provenance recorded on the channel
 */
public record StructuralSnapshotBinding(
        String repositoryId,
        String targetRevision,
        String inputPath,
        String inputSha256,
        String providerId,
        String provenance) {

    public StructuralSnapshotBinding {
        repositoryId = ReverseContractValidation.requiredText(repositoryId, "structural binding repository id");
        targetRevision = ReverseContractValidation.canonicalRevision(targetRevision);
        inputPath = ReverseContractValidation.repositoryPath(inputPath, "structural snapshot input path");
        inputSha256 = ReverseContractValidation.sha256Hex(inputSha256, "structural snapshot " + inputPath);
        providerId = ReverseContractValidation.requiredText(providerId, "structural provider id");
        provenance = ReverseContractValidation.requiredText(provenance, "structural provenance");
    }
}

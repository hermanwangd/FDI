package com.featuredeliveryintelligence.fdi.product.semantics;

import com.featuredeliveryintelligence.fdi.shared.RuntimeContractException;

import java.util.regex.Pattern;

public record ScenarioEvidenceReference(
        String evidenceId,
        Channel channel,
        String artifactPath,
        String artifactDigest,
        String jsonPointer) {

    private static final Pattern EVIDENCE_ID = Pattern.compile("EV-[A-Z0-9][A-Z0-9-]*");
    private static final Pattern ATOMIC_POINTER = Pattern.compile(
            "/(?:nodes|links|commits|pull_requests)/(?:0|[1-9][0-9]*)");

    public enum Channel {
        STRUCTURAL,
        DELIVERY_HISTORY
    }

    public ScenarioEvidenceReference {
        if (evidenceId == null || !EVIDENCE_ID.matcher(evidenceId).matches()) {
            throw new RuntimeContractException("evidenceId must be a stable EV-* identifier");
        }
        if (channel == null) {
            throw new RuntimeContractException("channel is required");
        }
        if (!ScenarioContractValidation.canonicalRelativePath(artifactPath)) {
            throw new RuntimeContractException("artifactPath must be repository-relative");
        }
        if (artifactDigest == null || !ScenarioContractValidation.DIGEST.matcher(artifactDigest).matches()) {
            throw new RuntimeContractException("artifactDigest must be a lowercase SHA-256");
        }
        if (jsonPointer == null || !ATOMIC_POINTER.matcher(jsonPointer).matches()) {
            throw new RuntimeContractException("jsonPointer must identify one node, link, commit, or pull request");
        }
    }
}

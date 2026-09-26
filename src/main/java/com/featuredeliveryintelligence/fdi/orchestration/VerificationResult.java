package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.List;

/** Verification outcome; it is deliberately not part of WorkItemResult. */
public record VerificationResult(String missionRef, String verificationStatus, List<String> evidenceRefs) {
    public VerificationResult { evidenceRefs = List.copyOf(evidenceRefs == null ? List.of() : evidenceRefs); }
}

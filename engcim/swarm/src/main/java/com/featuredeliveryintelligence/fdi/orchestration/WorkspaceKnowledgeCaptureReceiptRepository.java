package com.featuredeliveryintelligence.fdi.orchestration;

import java.util.Optional;

/** Optional extension for repositories that can expose an external capture receipt. */
public interface WorkspaceKnowledgeCaptureReceiptRepository extends WorkspaceKnowledgeRepository {
    Optional<WorkspaceKnowledgeCaptureResult> captureFor(String proposalRef);
}

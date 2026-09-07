package com.featuredeliveryintelligence.fdi.reverse.evidence;

/**
 * Evidence channels of the deterministic reverse experiment
 * (PKB-REVERSE-002). Every channel supplies observations, never Product
 * semantics. Channel order is declaration order and is the deterministic
 * iteration order for bundle validation and serialization.
 */
public enum ReverseEvidenceChannel {
    /** Structural observations from the frozen Graphify snapshot (Slice B). */
    STRUCTURAL,
    /** Mechanical test-behavior observations from the Java extractor (Slice C). */
    TEST_BEHAVIOR,
    /** Delivery reconstruction observations from Git/PR/history (Slice D). */
    DELIVERY_HISTORY
}

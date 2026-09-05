package com.featuredeliveryintelligence.fdi.validation.blindreview;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Legacy pre-Task-6 seam result: the blind packet and its evaluator-only key.
 */
public record LegacyBlindPacket(ObjectNode packet, ObjectNode key) {
    public LegacyBlindPacket {
        packet = packet.deepCopy();
        key = key.deepCopy();
    }
}

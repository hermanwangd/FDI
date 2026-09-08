package com.featuredeliveryintelligence.fdi.validation.blindreview;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Immutable Task-6 blind-review triple: the public packet, the sealed identity
 * key, and the non-recursive manifest.
 */
public record Task6Packet(ObjectNode packet, ObjectNode key, ObjectNode manifest) {
    public Task6Packet {
        packet = packet.deepCopy();
        key = key.deepCopy();
        manifest = manifest.deepCopy();
    }
}

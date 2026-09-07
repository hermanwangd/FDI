package com.featuredeliveryintelligence.fdi.reverse;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Byte-stable JSON serialization entry point for the reverse-proposal
 * contract (PKB-BL-009). Every later slice (evidence adapters, proposal
 * generator, evaluator comparison, CLI) must serialize through this helper so
 * that repeated serialization of equal documents is byte-identical:
 *
 * <ul>
 *   <li>object keys are emitted in sorted order at every nesting depth
 *       ({@link #canonicalize(JsonNode)}), independent of construction order;</li>
 *   <li>fixed indentation (two spaces, LF newlines) via a shared pretty printer;</li>
 *   <li>no wall-clock timestamps, locale-dependent formatting, or
 *       HashMap iteration-order dependence anywhere in the emitted document.</li>
 * </ul>
 */
public final class ReverseJson {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final DefaultPrettyPrinter PRETTY_PRINTER =
            new DefaultPrettyPrinter().withArrayIndenter(new DefaultIndenter("  ", "\n"));

    private ReverseJson() { }

    /** Serializes a document to bytes with sorted keys at every depth; repeated calls are byte-identical. */
    public static byte[] write(JsonNode document) {
        if (document == null) throw new IllegalArgumentException("document must not be null");
        try {
            return JSON.writerWithDefaultPrettyPrinter()
                    .with(PRETTY_PRINTER)
                    .writeValueAsBytes(canonicalize(document));
        } catch (IOException failure) {
            throw new IllegalStateException("reverse JSON serialization failed", failure);
        }
    }

    /** Deep copy with every object's fields re-ordered by sorted key; value nodes are returned as-is. */
    public static JsonNode canonicalize(JsonNode node) {
        if (node instanceof ObjectNode object) {
            ObjectNode copy = JSON.createObjectNode();
            Map<String, JsonNode> sorted = new TreeMap<>();
            object.fields().forEachRemaining(entry -> sorted.put(entry.getKey(), canonicalize(entry.getValue())));
            sorted.forEach(copy::set);
            return copy;
        }
        if (node instanceof ArrayNode array) {
            ArrayNode copy = JSON.createArrayNode();
            array.forEach(element -> copy.add(canonicalize(element)));
            return copy;
        }
        return node;
    }
}

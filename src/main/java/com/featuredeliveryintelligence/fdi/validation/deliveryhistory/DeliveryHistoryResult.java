package com.featuredeliveryintelligence.fdi.validation.deliveryhistory;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

/**
 * Deterministic delivery-history result. Renders byte-for-byte like the
 * Python CLI's {@code json.dumps(result, indent=2) + "\n"}: insertion order
 * (no key sorting), two-space indents, {@code ": "} key separator, default
 * {@code ensure_ascii=True} string escaping, Python integer and float
 * rendering, and CPython {@code NaN}/{@code Infinity} tokens for the
 * non-finite numbers accepted from PR metadata.
 */
public record DeliveryHistoryResult(JsonNode result) {

    public byte[] toJsonBytes() {
        StringBuilder out = new StringBuilder();
        render(out, result, 0);
        out.append('\n');
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void render(StringBuilder out, JsonNode value, int indent) {
        if (value == null || value.isNull()) {
            out.append("null");
        } else if (value.isTextual()) {
            out.append('"').append(escape(value.asText())).append('"');
        } else if (value.isBoolean()) {
            out.append(value.asBoolean() ? "true" : "false");
        } else if (value.isNumber()) {
            out.append(number(value));
        } else if (value.isArray()) {
            if (value.isEmpty()) {
                out.append("[]");
                return;
            }
            out.append("[\n");
            for (int index = 0; index < value.size(); index++) {
                out.append(" ".repeat(indent + 2));
                render(out, value.get(index), indent + 2);
                out.append(index + 1 < value.size() ? ",\n" : "\n");
            }
            out.append(" ".repeat(indent)).append(']');
        } else if (value.isObject()) {
            if (value.isEmpty()) {
                out.append("{}");
                return;
            }
            out.append("{\n");
            Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
            int index = 0;
            int size = value.size();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                out.append(" ".repeat(indent + 2)).append('"').append(escape(field.getKey())).append("\": ");
                render(out, field.getValue(), indent + 2);
                out.append(index + 1 < size ? ",\n" : "\n");
                index++;
            }
            out.append(" ".repeat(indent)).append('}');
        } else {
            out.append("null");
        }
    }

    private static String number(JsonNode value) {
        if (value.isIntegralNumber()) {
            return value.bigIntegerValue().toString();
        }
        double number = value.doubleValue();
        if (Double.isNaN(number)) {
            return "NaN";
        }
        if (Double.isInfinite(number)) {
            return number > 0 ? "Infinity" : "-Infinity";
        }
        if (number == Math.rint(number) && Math.abs(number) < 1e16) {
            return (long) number + ".0";
        }
        return Double.toString(number);
    }

    /** Python {@code json.dumps} string escaping with {@code ensure_ascii=True}. */
    public static String escape(String value) {
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (current < 0x20) {
                        out.append(String.format("\\u%04x", (int) current));
                    } else if (current < 0x80) {
                        out.append(current);
                    } else if (Character.isHighSurrogate(current) && index + 1 < value.length()
                            && Character.isLowSurrogate(value.charAt(index + 1))) {
                        out.append(String.format("\\u%04x\\u%04x",
                                (int) current, (int) value.charAt(++index)));
                    } else {
                        out.append(String.format("\\u%04x", (int) current));
                    }
                }
            }
        }
        return out.toString();
    }
}

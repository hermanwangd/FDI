package com.featuredeliveryintelligence.fdi.validation.nextrun;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Locale;

/**
 * Deterministic next-run gate report. Ports the public report shape of the
 * transitional Python consumer: {@code status}, sorted {@code reasons},
 * always-empty {@code mappings}, and the raw {@code run_id},
 * {@code skill_path}, and {@code skill_sha256} values (null when absent).
 * {@link #toJsonBytes()} renders byte-for-byte like the Python CLI's
 * {@code json.dumps(report, indent=2, sort_keys=True) + "\n"} with default
 * {@code ensure_ascii=True} escaping.
 */
public record NextRunReport(
        String status,
        List<String> reasons,
        JsonNode runId,
        JsonNode skillPath,
        JsonNode skillSha256) {

    public static NextRunReport blocked() {
        return new NextRunReport("BLOCKED", List.of("REQUEST_INVALID"), null, null, null);
    }

    public byte[] toJsonBytes() {
        StringBuilder out = new StringBuilder();
        out.append("{\n");
        out.append("  \"mappings\": [],\n");
        if (reasons.isEmpty()) {
            out.append("  \"reasons\": [],\n");
        } else {
            out.append("  \"reasons\": [\n");
            for (int index = 0; index < reasons.size(); index++) {
                out.append("    \"").append(escape(reasons.get(index))).append('"');
                out.append(index + 1 < reasons.size() ? ",\n" : "\n");
            }
            out.append("  ],\n");
        }
        out.append("  \"run_id\": ").append(renderValue(runId)).append(",\n");
        out.append("  \"skill_path\": ").append(renderValue(skillPath)).append(",\n");
        out.append("  \"skill_sha256\": ").append(renderValue(skillSha256)).append(",\n");
        out.append("  \"status\": \"").append(status).append("\"\n");
        out.append("}\n");
        return out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Python {@code json.dumps} string escaping with {@code ensure_ascii=True}. */
    static String escape(String value) {
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

    /** Renders the report value types the snapshot allows; null renders as Python {@code None}. */
    private static String renderValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return "null";
        }
        if (value.isTextual()) {
            return "\"" + escape(value.asText()) + "\"";
        }
        if (value.isBoolean()) {
            return value.asBoolean() ? "true" : "false";
        }
        if (value.isIntegralNumber()) {
            return value.bigIntegerValue().toString();
        }
        if (value.isNumber()) {
            return String.format(Locale.ROOT, "%s", value.decimalValue().toPlainString());
        }
        return "null";
    }
}

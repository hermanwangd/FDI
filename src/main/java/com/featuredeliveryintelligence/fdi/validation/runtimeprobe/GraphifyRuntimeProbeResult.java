package com.featuredeliveryintelligence.fdi.validation.runtimeprobe;

import java.util.List;
import java.util.Locale;

/**
 * Deterministic Graphify runtime-probe report. Ports the public result shape
 * of the transitional Python {@code inspect_runtime}: {@code runtime_found},
 * {@code runtime_path}, {@code runtime_sha256}, {@code verification_status},
 * {@code supported_operations}, the always-empty {@code api_assumptions}, and
 * the four identity fields that exist only after descriptor validation.
 * {@link #toJsonBytes()} renders byte-for-byte like the Python CLI's
 * {@code json.dumps(result, indent=2) + "\n"} with default
 * {@code ensure_ascii=True} escaping and the dict insertion order of the
 * Python consumer.
 */
public record GraphifyRuntimeProbeResult(
        boolean runtimeFound,
        String runtimePath,
        String runtimeSha256,
        String verificationStatus,
        List<String> supportedOperations,
        List<String> apiAssumptions,
        String runtimeIdentity,
        String runtimeVersion,
        String transport,
        String wireVersion) {

    public GraphifyRuntimeProbeResult {
        supportedOperations = List.copyOf(supportedOperations);
        apiAssumptions = List.copyOf(apiAssumptions);
    }

    public byte[] toJsonBytes() {
        StringBuilder out = new StringBuilder();
        out.append("{\n");
        out.append("  \"runtime_found\": ").append(runtimeFound).append(",\n");
        out.append("  \"runtime_path\": ").append(renderValue(runtimePath)).append(",\n");
        out.append("  \"runtime_sha256\": ").append(renderValue(runtimeSha256)).append(",\n");
        out.append("  \"verification_status\": \"").append(escape(verificationStatus)).append("\",\n");
        out.append("  \"supported_operations\": ").append(renderList(supportedOperations)).append(",\n");
        out.append("  \"api_assumptions\": ").append(renderList(apiAssumptions)).append(",\n");
        if (runtimeIdentity != null) {
            out.append("  \"runtime_identity\": \"").append(escape(runtimeIdentity)).append("\",\n");
            out.append("  \"runtime_version\": \"").append(escape(runtimeVersion)).append("\",\n");
            out.append("  \"transport\": \"").append(escape(transport)).append("\",\n");
            out.append("  \"wire_version\": \"").append(escape(wireVersion)).append("\"\n");
        } else {
            out.setLength(out.length() - 2);
            out.append('\n');
        }
        out.append("}\n");
        return out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String renderList(List<String> values) {
        if (values.isEmpty()) {
            return "[]";
        }
        StringBuilder out = new StringBuilder("[\n");
        for (int index = 0; index < values.size(); index++) {
            out.append("    \"").append(escape(values.get(index))).append('"');
            out.append(index + 1 < values.size() ? ",\n" : "\n");
        }
        out.append("  ]");
        return out.toString();
    }

    private static String renderValue(String value) {
        return value == null ? "null" : "\"" + escape(value) + "\"";
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
                        out.append(String.format(Locale.ROOT, "\\u%04x", (int) current));
                    }
                }
            }
        }
        return out.toString();
    }
}

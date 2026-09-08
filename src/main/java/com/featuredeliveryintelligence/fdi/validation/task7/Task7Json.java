package com.featuredeliveryintelligence.fdi.validation.task7;

import com.featuredeliveryintelligence.fdi.validation.codebaseline.PythonJson;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Python-compatible JSON surface for the Task 7 evaluation. Parsing delegates
 * to {@link PythonJson} (the transitional Python consumer's observable
 * failure surface). Rendering reproduces CPython
 * {@code json.dumps(value, indent=2, sort_keys=True) + "\n"} with default
 * {@code ensure_ascii=True} escaping: sorted object keys (code-point order),
 * two-space indents, {@code ": "} key separator, empty containers on one
 * line, and Python {@code repr(float)} number formatting.
 */
public final class Task7Json {

    private Task7Json() { }

    public static JsonNode readTree(byte[] data) {
        return PythonJson.readTree(data);
    }

    public static byte[] toReportBytes(JsonNode report) {
        StringBuilder out = new StringBuilder();
        render(out, report, 0);
        out.append('\n');
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Renders like CPython {@code json.dumps(value, sort_keys=True)} (no indent). */
    public static String compact(JsonNode value) {
        StringBuilder out = new StringBuilder();
        renderCompact(out, value);
        return out.toString();
    }

    private static void render(StringBuilder out, JsonNode value, int indent) {
        if (value == null || value.isNull()) {
            out.append("null");
        } else if (value.isTextual()) {
            out.append('"').append(escape(value.asText())).append('"');
        } else if (value.isBoolean()) {
            out.append(value.asBoolean() ? "true" : "false");
        } else if (value.isNumber()) {
            out.append(value.isFloatingPointNumber()
                    ? pythonFloat(value.doubleValue()) : value.bigIntegerValue().toString());
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
            List<Map.Entry<String, JsonNode>> fields = new ArrayList<>();
            value.fields().forEachRemaining(fields::add);
            fields.sort(Comparator.comparing(Map.Entry::getKey, Task7Json::compareCodePoints));
            for (int index = 0; index < fields.size(); index++) {
                Map.Entry<String, JsonNode> field = fields.get(index);
                out.append(" ".repeat(indent + 2)).append('"').append(escape(field.getKey())).append("\": ");
                render(out, field.getValue(), indent + 2);
                out.append(index + 1 < fields.size() ? ",\n" : "\n");
            }
            out.append(" ".repeat(indent)).append('}');
        } else {
            out.append("null");
        }
    }

    private static void renderCompact(StringBuilder out, JsonNode value) {
        if (value == null || value.isNull()) {
            out.append("null");
        } else if (value.isTextual()) {
            out.append('"').append(escape(value.asText())).append('"');
        } else if (value.isBoolean()) {
            out.append(value.asBoolean() ? "true" : "false");
        } else if (value.isNumber()) {
            out.append(value.isFloatingPointNumber()
                    ? pythonFloat(value.doubleValue()) : value.bigIntegerValue().toString());
        } else if (value.isArray()) {
            out.append('[');
            for (int index = 0; index < value.size(); index++) {
                renderCompact(out, value.get(index));
                if (index + 1 < value.size()) {
                    out.append(", ");
                }
            }
            out.append(']');
        } else if (value.isObject()) {
            out.append('{');
            List<Map.Entry<String, JsonNode>> fields = new ArrayList<>();
            value.fields().forEachRemaining(fields::add);
            fields.sort(Comparator.comparing(Map.Entry::getKey, Task7Json::compareCodePoints));
            for (int index = 0; index < fields.size(); index++) {
                Map.Entry<String, JsonNode> field = fields.get(index);
                out.append('"').append(escape(field.getKey())).append("\": ");
                renderCompact(out, field.getValue());
                if (index + 1 < fields.size()) {
                    out.append(", ");
                }
            }
            out.append('}');
        } else {
            out.append("null");
        }
    }

    /** CPython orders strings by Unicode code point, not by UTF-16 unit. */
    private static int compareCodePoints(String left, String right) {
        Iterator<Integer> a = left.codePoints().iterator();
        Iterator<Integer> b = right.codePoints().iterator();
        while (a.hasNext() && b.hasNext()) {
            int value = Integer.compare(a.next(), b.next());
            if (value != 0) {
                return value;
            }
        }
        return a.hasNext() ? 1 : b.hasNext() ? -1 : 0;
    }

    /**
     * Python {@code repr(float)} for finite values and the {@code json.dumps}
     * {@code NaN}/{@code Infinity} tokens: shortest correctly-rounded digit
     * string (ties-to-even) with Python's fixed-versus-scientific layout
     * (scientific when the decimal exponent of the first significant digit is
     * below -4 or at least 16).
     */
    static String pythonFloat(double value) {
        if (Double.isNaN(value)) {
            return "NaN";
        }
        if (value == Double.POSITIVE_INFINITY) {
            return "Infinity";
        }
        if (value == Double.NEGATIVE_INFINITY) {
            return "-Infinity";
        }
        if (value == 0.0) {
            return Math.copySign(1.0, value) < 0 ? "-0.0" : "0.0";
        }
        boolean negative = value < 0;
        double magnitude = Math.abs(value);
        BigInteger unscaled = null;
        int scale = 0;
        for (int significant = 1; significant <= 17; significant++) {
            BigDecimal candidate = new BigDecimal(magnitude,
                    new MathContext(significant, RoundingMode.HALF_EVEN));
            if (candidate.doubleValue() == magnitude) {
                String raw = candidate.unscaledValue().toString();
                int trailingZeros = 0;
                while (raw.length() - trailingZeros > 1
                        && raw.charAt(raw.length() - 1 - trailingZeros) == '0') {
                    trailingZeros++;
                }
                unscaled = new BigInteger(raw.substring(0, raw.length() - trailingZeros));
                scale = candidate.scale() - trailingZeros;
                break;
            }
        }
        if (unscaled == null) {
            throw new IllegalStateException("no shortest decimal for " + value);
        }
        String digits = unscaled.toString();
        // value = digits * 10^-scale = 0.digits * 10^exponent
        int exponent = digits.length() - scale;
        int firstDigitExponent = exponent - 1;
        StringBuilder out = new StringBuilder();
        if (negative) {
            out.append('-');
        }
        if (firstDigitExponent < -4 || firstDigitExponent >= 16) {
            out.append(digits.charAt(0));
            if (digits.length() > 1) {
                out.append('.').append(digits, 1, digits.length());
            }
            out.append('e').append(firstDigitExponent < 0 ? '-' : '+');
            String exponentText = Integer.toString(Math.abs(firstDigitExponent));
            if (exponentText.length() < 2) {
                out.append('0');
            }
            out.append(exponentText);
        } else if (exponent >= digits.length()) {
            out.append(digits).append("0".repeat(exponent - digits.length())).append(".0");
        } else if (exponent > 0) {
            out.append(digits, 0, exponent).append('.').append(digits.substring(exponent));
        } else {
            out.append("0.").append("0".repeat(-exponent)).append(digits);
        }
        return out.toString();
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

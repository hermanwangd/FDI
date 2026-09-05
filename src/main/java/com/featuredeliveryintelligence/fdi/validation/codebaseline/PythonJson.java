package com.featuredeliveryintelligence.fdi.validation.codebaseline;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Strict JSON parsing with the transitional Python consumer's observable
 * failure surface. Mirrors {@code json.loads(Path.read_text())}: UTF-8 is
 * decoded strictly (invalid bytes raise the CPython codec message), content
 * after the first value raises {@code Extra data}, empty or whitespace-only
 * input raises {@code Expecting value}, {@code NaN}/{@code Infinity} are
 * accepted like CPython, and parse failures raise an
 * {@link IllegalArgumentException} whose message is byte-identical to the
 * Python {@code json.JSONDecodeError} text, including line, column, and
 * character offset. Positions count Unicode code points, like CPython.
 */
public final class PythonJson {
    private static final StreamReadConstraints CONSTRAINTS = StreamReadConstraints.builder()
            .maxNestingDepth(1_000)
            .maxStringLength(16 * 1024 * 1024)
            .maxNumberLength(10_000)
            .build();
    private static final JsonFactory FACTORY = JsonFactory.builder()
            .streamReadConstraints(CONSTRAINTS)
            .build();
    private static final ObjectMapper JSON = new ObjectMapper(FACTORY)
            .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature());

    private PythonJson() { }

    public static JsonNode readTree(byte[] data) {
        String text = decodeUtf8(data);
        try (JsonParser parser = FACTORY.createParser(text)) {
            JsonNode value = JSON.readTree(parser);
            if (value == null && parser.currentToken() == null) {
                throw error("Expecting value", firstNonWhitespace(text, 0), text);
            }
            try {
                if (parser.nextToken() != null) {
                    throw error("Extra data", locationOffset(parser.getTokenLocation(), text), text);
                }
            } catch (JsonProcessingException trailing) {
                // Any garbage after a complete first value is Python "Extra
                // data" at the start of the trailing token.
                int errPos = locationOffset(trailing.getLocation(), text);
                String original = trailing.getOriginalMessage();
                int pos = original.contains("Unrecognized token")
                        || original.contains("number exponent") || original.contains("in null")
                        || original.contains("No digit following sign") || original.contains("Leading zeroes")
                        || original.contains("Expected space separating root-level values")
                        ? tokenStart(text, errPos) : errPos;
                throw error("Extra data", pos, text);
            }
            return value;
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(pythonMessage(text, failure));
        } catch (IOException impossible) {
            throw new IllegalStateException("string parser failed", impossible);
        }
    }

    /** Python {@code Path.read_text()} equivalent: strict UTF-8 with CPython codec error messages. */
    private static String decodeUtf8(byte[] data) {
        int index = 0;
        while (index < data.length) {
            int lead = data[index] & 0xff;
            if (lead < 0x80) {
                index++;
                continue;
            }
            if (lead < 0xc2 || lead > 0xf4) {
                throw codecError(data, index, index, "invalid start byte");
            }
            int needed = lead < 0xe0 ? 1 : lead < 0xf0 ? 2 : 3;
            int end = index + 1;
            while (end < data.length && end - index - 1 < needed && (data[end] & 0xc0) == 0x80) {
                end++;
            }
            int have = end - index - 1;
            // For f4/ed leads the decisive byte is the first continuation, which
            // decides whether the sequence is in the surrogate/above-max range.
            boolean surrogate = lead == 0xed && have >= 1 && (data[index + 1] & 0xff) >= 0xa0;
            boolean aboveMax = lead == 0xf4 && have >= 1 && (data[index + 1] & 0xff) > 0x8f;
            if (surrogate || aboveMax) {
                throw codecError(data, index, index, "invalid continuation byte");
            }
            if (have == needed) {
                index = end;
                continue;
            }
            if (end == data.length) {
                throw codecError(data, index, data.length - 1, "unexpected end of data");
            }
            throw codecError(data, index, end - 1, "invalid continuation byte");
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    private static IllegalArgumentException codecError(byte[] data, int start, int endInclusive, String reason) {
        if (start == endInclusive) {
            return new IllegalArgumentException("'utf-8' codec can't decode byte 0x"
                    + String.format("%02x", data[start] & 0xff) + " in position " + start + ": " + reason);
        }
        return new IllegalArgumentException("'utf-8' codec can't decode bytes in position "
                + start + "-" + endInclusive + ": " + reason);
    }

    /** Maps a first-value Jackson parse failure to the exact CPython {@code JSONDecodeError} text. */
    static String pythonMessage(String text, JsonProcessingException failure) {
        String original = failure.getOriginalMessage();
        int errPos = locationOffset(failure.getLocation(), text);
        String message;
        int pos = errPos;
        if (original.contains("in field name") || original.contains("in VALUE_STRING")
                || original.contains("closing quote for a string value")) {
            message = "Unterminated string starting at";
            pos = stringStart(text, errPos);
        } else if (original.contains("expected close marker") && original.contains("end-of-input")) {
            message = "Expecting ',' delimiter";
        } else if (original.contains("Unexpected close marker")) {
            message = "Expecting value";
        } else if (original.contains("within/between Object entries")
                || original.contains("within/between Array entries")) {
            message = "Expecting value";
        } else if (original.contains("was expecting double-quote to start field name")) {
            message = "Expecting property name enclosed in double quotes";
        } else if (original.contains("was expecting a colon")) {
            message = "Expecting ':' delimiter";
        } else if (original.contains("was expecting comma")) {
            message = "Expecting ',' delimiter";
        } else if (original.contains("Unrecognized character escape")) {
            message = "Invalid \\escape";
            pos = Math.max(0, errPos - 1);
        } else if (original.contains("hex-digit for character escape")) {
            message = "Invalid \\uXXXX escape";
            pos = unicodeEscapeStart(text, errPos);
        } else if (original.contains("Illegal unquoted character") || original.contains("CTRL-CHAR")) {
            message = "Invalid control character at";
        } else if (original.contains("Leading zeroes") || original.contains("Trailing token")
                || original.contains("Expected space separating root-level values")) {
            message = "Extra data";
        } else if (original.contains("Unrecognized token") || original.contains("number exponent")
                || original.contains("No digit following sign") || original.contains("in null")) {
            int start = tokenStart(text, errPos);
            int end = literalPrefixEnd(text, start);
            if (end == start) {
                end = numberPrefixEnd(text, start);
            }
            if (end > start) {
                message = "Extra data";
                pos = end;
            } else {
                message = "Expecting value";
                pos = start;
            }
        } else {
            message = "Expecting value";
        }
        return formatError(message, pos, text);
    }

    private static IllegalArgumentException error(String message, int pos, String text) {
        return new IllegalArgumentException(formatError(message, pos, text));
    }

    private static String formatError(String message, int pos, String text) {
        int line = 1;
        int lineStart = 0;
        for (int index = 0; index < pos && index < text.length(); index++) {
            if (text.charAt(index) == '\n') {
                line++;
                lineStart = index + 1;
            }
        }
        int column = pos - lineStart + 1;
        return message + ": line " + line + " column " + column + " (char " + pos + ")";
    }

    /** Char offset of a Jackson location (1-based line/column, code-point counted). */
    static int locationOffset(JsonLocation location, String text) {
        if (location == null) {
            return 0;
        }
        long line = location.getLineNr();
        long column = location.getColumnNr();
        if (line <= 1) {
            return Math.toIntExact(Math.max(0, column - 1));
        }
        int currentLine = 1;
        int offset = 0;
        while (currentLine < line && offset < text.length()) {
            if (text.charAt(offset) == '\n') {
                currentLine++;
            }
            offset++;
        }
        return Math.min(text.length(), offset + Math.toIntExact(Math.max(0, column - 1)));
    }

    private static int firstNonWhitespace(String text, int from) {
        int index = from;
        while (index < text.length() && isJsonWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private static boolean isJsonWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    /** Scans back from {@code errPos} to the start of the current token, like CPython's scanner start. */
    static int tokenStart(String text, int errPos) {
        int index = Math.min(errPos, text.length()) - 1;
        while (index >= 0) {
            char c = text.charAt(index);
            if (isJsonWhitespace(c) || c == ',' || c == ':' || c == '[' || c == ']'
                    || c == '{' || c == '}' || c == '"' || c == '\\') {
                break;
            }
            index--;
        }
        return index + 1;
    }

    /** End of the longest Python JSON literal starting at {@code start}, or {@code start}. */
    static int literalPrefixEnd(String text, int start) {
        for (String literal : new String[] {"-Infinity", "Infinity", "false", "true", "null", "NaN"}) {
            if (text.startsWith(literal, start)) {
                return start + literal.length();
            }
        }
        return start;
    }

    /** End of the longest prefix matching CPython's JSON number regex, or {@code start} when no match. */
    static int numberPrefixEnd(String text, int start) {
        int length = text.length();
        int index = start;
        if (index < length && text.charAt(index) == '-') {
            index++;
        }
        if (index < length && text.charAt(index) == '0') {
            index++;
        } else if (index < length && text.charAt(index) >= '1' && text.charAt(index) <= '9') {
            while (index < length && Character.isDigit(text.charAt(index))) {
                index++;
            }
        } else {
            return start;
        }
        if (index + 1 < length && text.charAt(index) == '.'
                && Character.isDigit(text.charAt(index + 1))) {
            index += 2;
            while (index < length && Character.isDigit(text.charAt(index))) {
                index++;
            }
        }
        int beforeExponent = index;
        if (index < length && (text.charAt(index) == 'e' || text.charAt(index) == 'E')) {
            int cursor = index + 1;
            if (cursor < length && (text.charAt(cursor) == '-' || text.charAt(cursor) == '+')) {
                cursor++;
            }
            if (cursor < length && Character.isDigit(text.charAt(cursor))) {
                while (cursor < length && Character.isDigit(text.charAt(cursor))) {
                    cursor++;
                }
                index = cursor;
            } else {
                index = beforeExponent;
            }
        }
        return index;
    }

    /** Finds the opening quote of the string being scanned at {@code errPos}. */
    static int stringStart(String text, int errPos) {
        int index = Math.min(errPos, text.length()) - 1;
        while (index >= 0) {
            if (text.charAt(index) == '"' && !isEscaped(text, index)) {
                return index;
            }
            index--;
        }
        return 0;
    }

    private static boolean isEscaped(String text, int quote) {
        int backslashes = 0;
        int index = quote - 1;
        while (index >= 0 && text.charAt(index) == '\\') {
            backslashes++;
            index--;
        }
        return backslashes % 2 == 1;
    }

    private static int unicodeEscapeStart(String text, int errPos) {
        int slash = text.lastIndexOf("\\u", Math.min(errPos, text.length()));
        return slash >= 0 ? slash + 1 : Math.max(0, errPos - 1);
    }
}

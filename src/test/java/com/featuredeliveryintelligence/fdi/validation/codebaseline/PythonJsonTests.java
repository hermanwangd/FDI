package com.featuredeliveryintelligence.fdi.validation.codebaseline;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterization battery for {@link PythonJson}. Every expected message was
 * collected from the transitional Python consumer's actual
 * {@code json.loads(Path.read_text())} failure surface (CPython JSONDecodeError
 * and UnicodeDecodeError texts), so the Java CLI's ERROR JSON stdout stays
 * byte-identical for malformed input files.
 */
class PythonJsonTests {
    private static final Map<String, String> JSON_ERRORS = new LinkedHashMap<>();

    static {
        JSON_ERRORS.put("{not json",
                "Expecting property name enclosed in double quotes: line 1 column 2 (char 1)");
        JSON_ERRORS.put("", "Expecting value: line 1 column 1 (char 0)");
        JSON_ERRORS.put("   ", "Expecting value: line 1 column 4 (char 3)");
        JSON_ERRORS.put("[1,", "Expecting value: line 1 column 4 (char 3)");
        JSON_ERRORS.put("{\"a\" 1}", "Expecting ':' delimiter: line 1 column 6 (char 5)");
        JSON_ERRORS.put("\"abc", "Unterminated string starting at: line 1 column 1 (char 0)");
        JSON_ERRORS.put("{\"a\":1,}",
                "Expecting property name enclosed in double quotes: line 1 column 8 (char 7)");
        JSON_ERRORS.put("tru", "Expecting value: line 1 column 1 (char 0)");
        JSON_ERRORS.put("{\"a\":\"\\q\"}", "Invalid \\escape: line 1 column 7 (char 6)");
        JSON_ERRORS.put("\"a\u0001b\"", "Invalid control character at: line 1 column 3 (char 2)");
        JSON_ERRORS.put("{} {}", "Extra data: line 1 column 4 (char 3)");
        JSON_ERRORS.put("[1 2]", "Expecting ',' delimiter: line 1 column 4 (char 3)");
        JSON_ERRORS.put("{\"a\":}", "Expecting value: line 1 column 6 (char 5)");
        JSON_ERRORS.put("nul", "Expecting value: line 1 column 1 (char 0)");
        JSON_ERRORS.put("'single'", "Expecting value: line 1 column 1 (char 0)");
        JSON_ERRORS.put("01", "Extra data: line 1 column 2 (char 1)");
        JSON_ERRORS.put("-", "Expecting value: line 1 column 1 (char 0)");
        JSON_ERRORS.put("\"\\u12\"", "Invalid \\uXXXX escape: line 1 column 3 (char 2)");
        JSON_ERRORS.put("[}", "Expecting value: line 1 column 2 (char 1)");
        JSON_ERRORS.put("{\"a\":1", "Expecting ',' delimiter: line 1 column 7 (char 6)");
        JSON_ERRORS.put("{\"a", "Unterminated string starting at: line 1 column 2 (char 1)");
        JSON_ERRORS.put("{\"a\":", "Expecting value: line 1 column 6 (char 5)");
        JSON_ERRORS.put("[\"x\"", "Expecting ',' delimiter: line 1 column 5 (char 4)");
        JSON_ERRORS.put("[1", "Expecting ',' delimiter: line 1 column 3 (char 2)");
        JSON_ERRORS.put("  tru", "Expecting value: line 1 column 3 (char 2)");
        JSON_ERRORS.put("{\n \"a\":1,\n}",
                "Expecting property name enclosed in double quotes: line 3 column 1 (char 10)");
        JSON_ERRORS.put("[1,\n]", "Expecting value: line 2 column 1 (char 4)");
        JSON_ERRORS.put("{\"a\":1} x", "Extra data: line 1 column 9 (char 8)");
        JSON_ERRORS.put("{\"a\":1} true", "Extra data: line 1 column 9 (char 8)");
        JSON_ERRORS.put("{} 01", "Extra data: line 1 column 4 (char 3)");
        JSON_ERRORS.put("{\"a\": \"\\u12\"}", "Invalid \\uXXXX escape: line 1 column 9 (char 8)");
        JSON_ERRORS.put("truee", "Extra data: line 1 column 5 (char 4)");
        JSON_ERRORS.put("123abc", "Extra data: line 1 column 4 (char 3)");
        JSON_ERRORS.put("-5x", "Extra data: line 1 column 3 (char 2)");
        JSON_ERRORS.put("1.2.3", "Extra data: line 1 column 4 (char 3)");
        JSON_ERRORS.put("nullx", "Extra data: line 1 column 5 (char 4)");
        JSON_ERRORS.put("NaNx", "Extra data: line 1 column 4 (char 3)");
        JSON_ERRORS.put("-Infinityz", "Extra data: line 1 column 10 (char 9)");
        JSON_ERRORS.put("1e", "Extra data: line 1 column 2 (char 1)");
        JSON_ERRORS.put("1e+", "Extra data: line 1 column 2 (char 1)");
        JSON_ERRORS.put("0x1", "Extra data: line 1 column 2 (char 1)");
        JSON_ERRORS.put("{} truee", "Extra data: line 1 column 4 (char 3)");
        JSON_ERRORS.put("{} 1e", "Extra data: line 1 column 4 (char 3)");
        JSON_ERRORS.put("{} ]", "Extra data: line 1 column 4 (char 3)");
        JSON_ERRORS.put("{} tru", "Extra data: line 1 column 4 (char 3)");
        JSON_ERRORS.put("{} -5x", "Extra data: line 1 column 4 (char 3)");
        JSON_ERRORS.put("falsey", "Extra data: line 1 column 6 (char 5)");
        JSON_ERRORS.put("Infinityx", "Extra data: line 1 column 9 (char 8)");
        JSON_ERRORS.put("[\"\u2603\",]", "Expecting value: line 1 column 6 (char 5)");
    }

    private static final Map<String, String> CODEC_ERRORS = new LinkedHashMap<>();

    static {
        CODEC_ERRORS.put("\"\u0080\"",
                "'utf-8' codec can't decode byte 0x80 in position 1: invalid start byte");
        CODEC_ERRORS.put("\"\u00c3",
                "'utf-8' codec can't decode byte 0xc3 in position 1: unexpected end of data");
        CODEC_ERRORS.put("\"\u00c3\"",
                "'utf-8' codec can't decode byte 0xc3 in position 1: invalid continuation byte");
        CODEC_ERRORS.put("\"\u00c3(\"",
                "'utf-8' codec can't decode byte 0xc3 in position 1: invalid continuation byte");
        CODEC_ERRORS.put("\"\u00e2\u0082",
                "'utf-8' codec can't decode bytes in position 1-2: unexpected end of data");
        CODEC_ERRORS.put("\"\u00ff\"",
                "'utf-8' codec can't decode byte 0xff in position 1: invalid start byte");
        CODEC_ERRORS.put("\u00ed\u00a0\u0080",
                "'utf-8' codec can't decode byte 0xed in position 0: invalid continuation byte");
        CODEC_ERRORS.put("\u00f4\u0090\u0080\u0080",
                "'utf-8' codec can't decode byte 0xf4 in position 0: invalid continuation byte");
        CODEC_ERRORS.put("\"\u00e2\u0082x\"",
                "'utf-8' codec can't decode bytes in position 1-2: invalid continuation byte");
    }

    @Test
    void jsonDecodeErrorsMatchPythonMessages() {
        JSON_ERRORS.forEach((input, expected) -> {
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> PythonJson.readTree(input.getBytes(StandardCharsets.UTF_8)),
                    "input: " + repr(input));
            assertEquals(expected, failure.getMessage(), "input: " + repr(input));
        });
    }

    @Test
    void utf8DecodeErrorsMatchPythonMessages() {
        CODEC_ERRORS.forEach((input, expected) -> {
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> PythonJson.readTree(input.getBytes(StandardCharsets.ISO_8859_1)),
                    "input: " + repr(input));
            assertEquals(expected, failure.getMessage(), "input: " + repr(input));
        });
    }

    @Test
    void validPythonAcceptedInputsParse() throws Exception {
        assertEquals("NaN", PythonJson.readTree("NaN".getBytes(StandardCharsets.UTF_8)).asText());
        assertEquals("Infinity",
                PythonJson.readTree("Infinity".getBytes(StandardCharsets.UTF_8)).asText());
        assertEquals("-Infinity",
                PythonJson.readTree("-Infinity".getBytes(StandardCharsets.UTF_8)).asText());
        JsonNode withNan = PythonJson.readTree("{\"a\": NaN}".getBytes(StandardCharsets.UTF_8));
        assertTrue(withNan.get("a").isDouble());
        // Lone surrogate escapes are accepted like CPython.
        assertEquals("\ud800",
                PythonJson.readTree("\"\\ud800\"".getBytes(StandardCharsets.UTF_8)).asText());
        // Trailing whitespace is fine.
        assertEquals(1, PythonJson.readTree("{\"a\":1}   \n".getBytes(StandardCharsets.UTF_8)).size());
        // Duplicate keys keep the last value like CPython.
        assertEquals(2, PythonJson.readTree("{\"x\":1,\"x\":2}".getBytes(StandardCharsets.UTF_8))
                .get("x").asInt());
        // Unicode content and astral characters parse.
        assertEquals("\ud83d\ude00", PythonJson
                .readTree("{\"s\": \"\ud83d\ude00\"}".getBytes(StandardCharsets.UTF_8))
                .get("s").asText());
        // Python-arbitrary-precision integers keep their digits.
        String big = "12345678901234567890123456789012345678901234567890";
        assertEquals(big, PythonJson.readTree(big.getBytes(StandardCharsets.UTF_8)).asText());
    }

    @Test
    void validInputsDoNotThrow() throws Exception {
        assertFalse(PythonJson.readTree("{\"a\":[1,2,3]}".getBytes(StandardCharsets.UTF_8)).isEmpty());
    }

    private static String repr(String value) {
        return "'" + value.replace("\n", "\\n") + "'";
    }
}

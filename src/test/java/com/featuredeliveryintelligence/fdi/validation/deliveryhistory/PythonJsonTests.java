package com.featuredeliveryintelligence.fdi.validation.deliveryhistory;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterization tests for {@link PythonJson}, the strict JSON reader that
 * mirrors {@code json.loads(Path.read_text())} as exercised through the
 * transitional Python consumer's {@code --prs} input: CPython decode-error
 * messages, {@code Expecting value} on empty input, {@code Extra data} after
 * the first value, CPython {@code JSONDecodeError} text with line, column, and
 * character offset, and {@code NaN}/{@code Infinity} acceptance like CPython.
 */
class PythonJsonTests {

    @Test
    void parsesSingleValuesLikePythonJsonLoads() {
        JsonNode array = PythonJson.readTree(bytes("[1, 2]"));
        assertEquals(2, array.size());
        assertEquals(1, array.get(0).asInt());
        assertEquals(2, array.get(1).asInt());
        JsonNode object = PythonJson.readTree(bytes("{\"a\": 1}"));
        assertEquals(1, object.get("a").asInt());
        assertEquals("text", PythonJson.readTree(bytes("\"text\"")).asText());
        assertTrue(PythonJson.readTree(bytes("null")).isNull());
        assertTrue(PythonJson.readTree(bytes("true")).asBoolean());
    }

    @Test
    void acceptsNonNumericConstantsLikeCpython() {
        assertTrue(PythonJson.readTree(bytes("NaN")).isNumber());
        assertTrue(PythonJson.readTree(bytes("[Infinity, -Infinity]")).get(0).isNumber());
    }

    @Test
    void rejectsInvalidUtf8WithTheCpythonCodecMessage() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> PythonJson.readTree(new byte[] {(byte) 0xff, (byte) 0xfe}));
        assertEquals("'utf-8' codec can't decode byte 0xff in position 0: invalid start byte",
                failure.getMessage());
    }

    @Test
    void reportsEmptyAndWhitespaceOnlyInputAsExpectingValue() {
        assertDecodeError("", "Expecting value: line 1 column 1 (char 0)");
        assertDecodeError("   \n ", "Expecting value: line 2 column 2 (char 5)");
    }

    @Test
    void reportsTrailingContentAsExtraData() {
        assertDecodeError("{\"a\":1} x", "Extra data: line 1 column 9 (char 8)");
        assertDecodeError("[1]\n2", "Extra data: line 2 column 1 (char 4)");
    }

    @Test
    void reportsObjectAndArrayDelimiterErrorsLikeCpython() {
        assertDecodeError("{\"a\":1,}", "Expecting property name enclosed in double quotes:"
                + " line 1 column 8 (char 7)");
        assertDecodeError("{'a':1}", "Expecting property name enclosed in double quotes:"
                + " line 1 column 2 (char 1)");
        assertDecodeError("{\"a\" 1}", "Expecting ':' delimiter: line 1 column 6 (char 5)");
        assertDecodeError("{\"a\":1 \"b\":2}", "Expecting ',' delimiter: line 1 column 8 (char 7)");
    }

    @Test
    void reportsStringErrorsLikeCpython() {
        assertDecodeError("[\"a\\q\"]", "Invalid \\escape: line 1 column 4 (char 3)");
        assertDecodeError("[\"a\\u12\"]", "Invalid \\uXXXX escape: line 1 column 5 (char 4)");
        assertDecodeError("[\"abc", "Unterminated string starting at: line 1 column 2 (char 1)");
    }

    @Test
    void reportsBareValuesLikeCpython() {
        assertDecodeError("not json", "Expecting value: line 1 column 1 (char 0)");
    }

    private static void assertDecodeError(String input, String message) {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> PythonJson.readTree(bytes(input)));
        assertEquals(message, failure.getMessage());
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}

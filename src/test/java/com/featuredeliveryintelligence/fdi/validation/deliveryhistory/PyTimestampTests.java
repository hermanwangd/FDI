package com.featuredeliveryintelligence.fdi.validation.deliveryhistory;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Characterization tests for {@link PyTimestamp}, the port of the Python 3.9
 * {@code datetime.fromisoformat} grammar the transitional consumer applies to
 * the cutoff and PR timestamps (after its {@code 'Z' → '+00:00'} replacement):
 * fixed-position date, any single-character separator, optional seconds,
 * exactly 3- or 6-digit fractions, {@code ±HH:MM[:SS]} offsets, naive-timestamp
 * rejection, CPython range-check messages, and 24-hour offset rejection.
 */
class PyTimestampTests {
    @Test
    void parsesZuluAndOffsetTimestamps() {
        assertEquals(Instant.parse("2026-01-03T00:00:00Z"), PyTimestamp.parse("2026-01-03T00:00:00Z"));
        assertEquals(Instant.parse("2026-01-03T00:00:00Z"), PyTimestamp.parse("2026-01-03T00:00:00+00:00"));
        assertEquals(Instant.parse("2026-01-02T18:30:00Z"), PyTimestamp.parse("2026-01-03T00:00:00+05:30"));
        assertEquals(Instant.parse("2026-01-03T00:00:00Z"), PyTimestamp.parse("2026-01-03T00:00:00-00:00"));
        assertEquals(Instant.parse("2026-01-03T00:00:00.250Z"), PyTimestamp.parse("2026-01-03T00:00:00.250+00:00"));
        assertEquals(Instant.parse("2026-01-03T00:00:00.000001Z"),
                PyTimestamp.parse("2026-01-03T00:00:00.000001+00:00"));
        assertEquals(Instant.parse("2026-01-02T23:59:30Z"), PyTimestamp.parse("2026-01-03T00:00:00+00:00:30"));
        assertEquals(Instant.parse("2026-01-02T23:00:00Z"), PyTimestamp.parse("2026-01-03T00:00:00+00:60"));
    }

    @Test
    void acceptsAnySingleCharacterSeparator() {
        assertEquals(Instant.parse("2026-01-03T00:00:00Z"), PyTimestamp.parse("2026-01-03 00:00:00+00:00"));
        assertEquals(Instant.parse("2026-01-03T00:00:00Z"), PyTimestamp.parse("2026-01-03X00:00:00+00:00"));
    }

    @Test
    void replacesEveryZLikeThePythonConsumer() {
        assertEquals(Instant.parse("2026-01-03T00:00:00Z"), PyTimestamp.parse("2026-01-03T00:00:00Z"));
        IllegalArgumentException doubled = assertThrows(IllegalArgumentException.class,
                () -> PyTimestamp.parse("2026-01-03T00:00:00+00:00Z"));
        assertEquals("Invalid isoformat string: '2026-01-03T00:00:00+00:00+00:00'", doubled.getMessage());
    }

    @Test
    void rejectsNaiveTimestampsWithTheConsumerMessage() {
        IllegalArgumentException dateOnly = assertThrows(IllegalArgumentException.class,
                () -> PyTimestamp.parse("2026-01-03"));
        assertEquals("timestamp must include a timezone", dateOnly.getMessage());
        IllegalArgumentException noOffset = assertThrows(IllegalArgumentException.class,
                () -> PyTimestamp.parse("2026-01-03T00:00:00"));
        assertEquals("timestamp must include a timezone", noOffset.getMessage());
    }

    @Test
    void rejectsMalformedTimestampsLikePython39() {
        assertInvalid("not-a-date");
        assertInvalid("00:00:00+00:00");
        assertInvalid("2026-01-03T00:00:00.5+00:00");
        assertInvalid("2026-01-03T00:00:00.1234+00:00");
        assertInvalid("2026-01-03T00:00:00.123456789+00:00");
        assertInvalid("2026-01-03T00:00:00+0000");
    }

    @Test
    void reportsRangeViolationsLikeThePythonConstructor() {
        assertInvalid("2026-13-03T00:00:00+00:00", "month must be in 1..12");
        assertInvalid("2026-02-30T00:00:00+00:00", "day is out of range for month");
        assertInvalid("2026-01-03T24:00:00+00:00", "hour must be in 0..23");
        assertInvalid("2026-01-03T00:60:00+00:00", "minute must be in 0..59");
        assertInvalid("2026-01-03T00:00:60+00:00", "second must be in 0..59");
        assertInvalid("2026-01-03T00:00:00+24:00", "offset must be a timedelta strictly between"
                + " -timedelta(hours=24) and timedelta(hours=24), not datetime.timedelta(days=1).");
        assertInvalid("2026-01-03T00:00:00-24:00", "offset must be a timedelta strictly between"
                + " -timedelta(hours=24) and timedelta(hours=24), not datetime.timedelta(days=-1).");
    }

    private static void assertInvalid(String value) {
        assertInvalid(value, "Invalid isoformat string: '" + value.replace("Z", "+00:00") + "'");
    }

    private static void assertInvalid(String value, String message) {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> PyTimestamp.parse(value));
        assertEquals(message, failure.getMessage());
    }
}

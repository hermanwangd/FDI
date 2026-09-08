package com.featuredeliveryintelligence.fdi.validation.deliveryhistory;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Port of the transitional Python consumer's {@code _instant}: CPython 3.9
 * {@code datetime.fromisoformat} applied after the consumer's
 * {@code value.replace('Z', '+00:00')}. The 3.9 grammar is fixed-position
 * {@code YYYY-MM-DD}, any single-character date/time separator,
 * {@code HH:MM[:SS[.fff|.ffffff]]]}, and an optional {@code ±HH:MM[:SS]}
 * offset; a missing offset is rejected with the consumer's
 * {@code 'timestamp must include a timezone'}, malformed strings with
 * {@code "Invalid isoformat string: '<replaced value>'"}, and range
 * violations with the CPython {@code datetime}/{@code timedelta} constructor
 * messages. Comparison semantics are instant-based, like aware
 * {@code datetime} comparison.
 */
public final class PyTimestamp {
    private PyTimestamp() { }

    /** Parses like the Python consumer's {@code _instant} and returns the instant. */
    public static Instant parse(String value) {
        String replaced = value.replace("Z", "+00:00");
        if (replaced.length() < 10 || replaced.charAt(4) != '-' || replaced.charAt(7) != '-') {
            throw invalid(replaced);
        }
        int year = digits(replaced, 0, 4);
        int month = digits(replaced, 5, 7);
        int day = digits(replaced, 8, 10);
        if (replaced.length() == 10) {
            throw new IllegalArgumentException("timestamp must include a timezone");
        }
        int position = 11;
        if (replaced.length() < position + 5 || replaced.charAt(position + 2) != ':') {
            throw invalid(replaced);
        }
        int hour = digits(replaced, position, position + 2);
        int minute = digits(replaced, position + 3, position + 5);
        position += 5;
        int second = 0;
        int nano = 0;
        if (position < replaced.length() && replaced.charAt(position) == ':') {
            if (replaced.length() < position + 3) {
                throw invalid(replaced);
            }
            second = digits(replaced, position + 1, position + 3);
            position += 3;
        }
        if (position < replaced.length() && replaced.charAt(position) == '.') {
            int start = ++position;
            while (position < replaced.length() && Character.isDigit(replaced.charAt(position))) {
                position++;
            }
            int count = position - start;
            if (count != 3 && count != 6) {
                throw invalid(replaced);
            }
            int fraction = Integer.parseInt(replaced.substring(start, position));
            nano = count == 3 ? fraction * 1_000_000 : fraction * 1_000;
        }
        long offsetSeconds = 0;
        boolean offset = false;
        if (position < replaced.length()) {
            char sign = replaced.charAt(position);
            if ((sign != '+' && sign != '-') || replaced.length() < position + 6
                    || replaced.charAt(position + 3) != ':') {
                throw invalid(replaced);
            }
            long hours = digits(replaced, position + 1, position + 3);
            long minutes = digits(replaced, position + 4, position + 6);
            offsetSeconds = hours * 3600 + minutes * 60;
            position += 6;
            if (position < replaced.length() && replaced.charAt(position) == ':') {
                if (replaced.length() < position + 3) {
                    throw invalid(replaced);
                }
                offsetSeconds += digits(replaced, position + 1, position + 3);
                position += 3;
            }
            if (position != replaced.length()) {
                throw invalid(replaced);
            }
            if (sign == '-') {
                offsetSeconds = -offsetSeconds;
            }
            offset = true;
        }
        if (!offset) {
            throw new IllegalArgumentException("timestamp must include a timezone");
        }
        if (year < 1 || year > 9999) {
            throw new IllegalArgumentException("year " + year + " is out of range");
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be in 1..12");
        }
        if (day < 1 || day > lengthOfMonth(year, month)) {
            throw new IllegalArgumentException("day is out of range for month");
        }
        if (hour > 23) {
            throw new IllegalArgumentException("hour must be in 0..23");
        }
        if (minute > 59) {
            throw new IllegalArgumentException("minute must be in 0..59");
        }
        if (second > 59) {
            throw new IllegalArgumentException("second must be in 0..59");
        }
        if (Math.abs(offsetSeconds) >= 86400) {
            throw new IllegalArgumentException("offset must be a timedelta strictly between"
                    + " -timedelta(hours=24) and timedelta(hours=24), not "
                    + timedeltaRepr(offsetSeconds) + ".");
        }
        long epochSecond = LocalDate.of(year, month, day).toEpochDay() * 86400
                + hour * 3600L + minute * 60L + second - offsetSeconds;
        return Instant.ofEpochSecond(epochSecond, nano);
    }

    private static IllegalArgumentException invalid(String replaced) {
        return new IllegalArgumentException("Invalid isoformat string: '" + replaced + "'");
    }

    private static int digits(String value, int start, int end) {
        if (end > value.length()) {
            throw invalid(value);
        }
        for (int index = start; index < end; index++) {
            if (!Character.isDigit(value.charAt(index))) {
                throw invalid(value);
            }
        }
        return Integer.parseInt(value.substring(start, end));
    }

    private static int lengthOfMonth(int year, int month) {
        return switch (month) {
            case 2 -> (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) ? 29 : 28;
            case 4, 6, 9, 11 -> 30;
            default -> 31;
        };
    }

    /** CPython {@code repr(timedelta)} for whole-second offsets. */
    private static String timedeltaRepr(long offsetSeconds) {
        long days = Math.floorDiv(offsetSeconds, 86400);
        long seconds = Math.floorMod(offsetSeconds, 86400);
        StringBuilder out = new StringBuilder("datetime.timedelta(days=").append(days);
        if (seconds != 0) {
            out.append(", seconds=").append(seconds);
        }
        return out.append(')').toString();
    }
}

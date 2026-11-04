package io.github.piotr_janczukowicz.util;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Date-time formatting and parsing helpers.
 *
 * <p>Converts {@link Instant} values to ISO-8601 UTC text ({@code Z} offset), not a local time zone. Also converts RFC
 * 1123 text, the form used by HTTP dates, to and from {@link Instant}.
 */
public final class DateTimeUtils {

    /** Epoch second of {@code 0000-01-01T00:00:00Z}, the earliest instant RFC 1123 text can print. */
    private static final long MIN_SECOND = -62_167_219_200L;

    /** Epoch second of {@code 9999-12-31T23:59:59Z}, the latest instant RFC 1123 text can print. */
    private static final long MAX_SECOND = 253_402_300_799L;

    private DateTimeUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Formats {@code instant} as ISO-8601 UTC, truncated to whole seconds.
     *
     * <p>Nanos are discarded with {@link ChronoUnit#SECONDS} (not rounded), then formatted with
     * {@link DateTimeFormatter#ISO_INSTANT}. For example {@code 2024-01-01T00:00:00.123456789Z} becomes
     * {@code 2024-01-01T00:00:00Z}.
     *
     * @param instant instant to format
     * @return ISO-8601 UTC timestamp with second precision
     * @throws NullPointerException if {@code instant} is {@code null}
     */
    public static String toIso(final Instant instant) {
        requireNonNull(instant, "instant");

        return DateTimeFormatter.ISO_INSTANT.format(instant.truncatedTo(ChronoUnit.SECONDS));
    }

    /**
     * Parses RFC 1123 text, such as {@code Tue, 3 Jun 2008 11:05:30 GMT}, with
     * {@link DateTimeFormatter#RFC_1123_DATE_TIME}.
     *
     * <p>Any zone offset the formatter accepts is applied, so the result is the same point in time in UTC. RFC 850 and
     * asctime forms are not accepted.
     *
     * @param value text to parse
     * @return the parsed instant, or empty if {@code value} is not RFC 1123 text
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static Optional<Instant> parseRfc1123(final String value) {
        requireNonNull(value, "value");

        Optional<Instant> parsed;
        try {
            parsed = Optional.of(ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toInstant());
        } catch (final DateTimeParseException notRfc1123) {
            parsed = Optional.empty();
        }
        return parsed;
    }

    /**
     * Formats {@code instant} as RFC 1123 text in UTC, such as {@code Tue, 3 Jun 2008 11:05:30 GMT}.
     *
     * <p>The output is {@link DateTimeFormatter#RFC_1123_DATE_TIME} applied to {@code instant} at
     * {@link ZoneOffset#UTC}. The day of the month is not zero-padded, and fractional seconds are dropped.
     *
     * @param instant instant to format
     * @return RFC 1123 text in UTC
     * @throws NullPointerException if {@code instant} is {@code null}
     * @throws IllegalArgumentException if {@code instant} falls outside the years 0000 to 9999, which RFC 1123 text
     *     cannot represent
     */
    public static String toRfc1123(final Instant instant) {
        requireNonNull(instant, "instant");
        if (instant.getEpochSecond() < MIN_SECOND || instant.getEpochSecond() > MAX_SECOND) {
            throw new IllegalArgumentException("instant must be between 0000-01-01T00:00:00Z and 9999-12-31T23:59:59Z");
        }

        return DateTimeFormatter.RFC_1123_DATE_TIME.format(instant.atOffset(ZoneOffset.UTC));
    }
}

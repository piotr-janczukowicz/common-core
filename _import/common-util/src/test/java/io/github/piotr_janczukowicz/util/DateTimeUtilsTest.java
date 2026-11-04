package io.github.piotr_janczukowicz.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DateTimeUtilsTest {

    @Test
    void constructor_shouldThrow_whenInstantiatedViaReflection() throws ReflectiveOperationException {
        UtilityClassAssertions.assertNotInstantiable(DateTimeUtils.class);
    }

    @Test
    void toIso_shouldReturnUtcSeconds_whenInstantHasNanos() {
        final Instant instant = Instant.parse("2024-01-01T00:00:00.123456789Z");

        final String formatted = DateTimeUtils.toIso(instant);

        assertThat(formatted).isEqualTo("2024-01-01T00:00:00Z");
    }

    @Test
    void toIso_shouldReturnUtcSeconds_whenAlreadyWholeSeconds() {
        final Instant instant = Instant.parse("2023-01-01T00:00:00Z");

        final String formatted = DateTimeUtils.toIso(instant);

        assertThat(formatted).isEqualTo("2023-01-01T00:00:00Z");
    }

    @Test
    void toIso_shouldThrow_whenInstantNull() {
        final Instant instant = null;

        final Throwable thrown = catchThrowable(() -> DateTimeUtils.toIso(instant));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("instant must not be null");
    }

    @Test
    void parseRfc1123_shouldReturnInstant_whenGmtDate() {
        final Optional<Instant> parsed = DateTimeUtils.parseRfc1123("Tue, 3 Jun 2008 11:05:30 GMT");

        assertThat(parsed).contains(Instant.parse("2008-06-03T11:05:30Z"));
    }

    @Test
    void parseRfc1123_shouldReturnSamePointInUtc_whenNonGmtOffset() {
        final Optional<Instant> parsed = DateTimeUtils.parseRfc1123("Tue, 3 Jun 2008 13:05:30 +0200");

        assertThat(parsed).contains(Instant.parse("2008-06-03T11:05:30Z"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not a date", "Sunday, 06-Nov-94 08:49:37 GMT", "Sun Nov  6 08:49:37 1994"})
    void parseRfc1123_shouldReturnEmpty_whenTextNotRfc1123(final String text) {
        final Optional<Instant> parsed = DateTimeUtils.parseRfc1123(text);

        assertThat(parsed).isEmpty();
    }

    @Test
    void parseRfc1123_shouldThrowNullPointerException_whenValueNull() {
        final String value = null;

        final Throwable thrown = catchThrowable(() -> DateTimeUtils.parseRfc1123(value));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("value must not be null");
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "2008-06-03T11:05:30Z|Tue, 3 Jun 2008 11:05:30 GMT",
                "2008-06-03T11:05:30.987654321Z|Tue, 3 Jun 2008 11:05:30 GMT",
                "9999-12-31T23:59:59Z|Fri, 31 Dec 9999 23:59:59 GMT",
                "9999-12-31T23:59:59.999999999Z|Fri, 31 Dec 9999 23:59:59 GMT",
                "0000-01-01T00:00:00Z|Sat, 1 Jan 0000 00:00:00 GMT"
            })
    void toRfc1123_shouldFormatInUtc_whenInstantGiven(final String text, final String expected) {
        final Instant instant = Instant.parse(text);

        final String formatted = DateTimeUtils.toRfc1123(instant);

        assertThat(formatted).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"+10000-01-01T00:00:00Z", "-0001-12-31T23:59:59Z", "+1000000000-12-31T23:59:59Z"})
    void toRfc1123_shouldThrowIllegalArgumentException_whenYearOutsideRfc1123Range(final String text) {
        final Instant instant = Instant.parse(text);

        final Throwable thrown = catchThrowable(() -> DateTimeUtils.toRfc1123(instant));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("instant must be between 0000-01-01T00:00:00Z and 9999-12-31T23:59:59Z");
    }

    @Test
    void toRfc1123_shouldThrowIllegalArgumentException_whenInstantMax() {
        final Instant instant = Instant.MAX;

        final Throwable thrown = catchThrowable(() -> DateTimeUtils.toRfc1123(instant));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseRfc1123_shouldReturnEmpty_whenDayOfWeekDoesNotMatchDate() {
        final Optional<Instant> parsed = DateTimeUtils.parseRfc1123("Mon, 3 Jun 2008 11:05:30 GMT");

        assertThat(parsed).isEmpty();
    }

    @Test
    void toRfc1123_shouldMatchRfc1123Formatter_whenInstantGiven() {
        final Instant instant = Instant.parse("2024-02-29T23:59:59Z");

        final String formatted = DateTimeUtils.toRfc1123(instant);

        assertThat(formatted).isEqualTo(DateTimeFormatter.RFC_1123_DATE_TIME.format(instant.atOffset(ZoneOffset.UTC)));
    }

    @Test
    void toRfc1123_shouldThrowNullPointerException_whenInstantNull() {
        final Instant instant = null;

        final Throwable thrown = catchThrowable(() -> DateTimeUtils.toRfc1123(instant));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("instant must not be null");
    }

    @Test
    void parseRfc1123_shouldReturnOriginalInstant_whenRoundTripped() {
        final Instant instant = Instant.parse("2024-02-29T23:59:59Z");

        final String text = DateTimeUtils.toRfc1123(instant);

        final Optional<Instant> parsed = DateTimeUtils.parseRfc1123(text);

        assertThat(parsed).contains(instant);
    }
}

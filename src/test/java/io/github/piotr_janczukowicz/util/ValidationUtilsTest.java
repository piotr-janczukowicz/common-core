package io.github.piotr_janczukowicz.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ValidationUtilsTest {

    @Test
    void constructor_shouldThrow_whenInstantiatedViaReflection() throws ReflectiveOperationException {
        UtilityClassAssertions.assertNotInstantiable(ValidationUtils.class);
    }

    @ParameterizedTest
    @MethodSource("nonPositiveDurations")
    void isPositive_shouldReturnFalse_whenNullOrNotPositive(final Duration duration) {
        final boolean positive = ValidationUtils.isPositive(duration);

        assertThat(positive).isFalse();
    }

    @Test
    void isPositive_shouldReturnTrue_whenDurationPositive() {
        final Duration duration = Duration.ofMillis(1);

        final boolean positive = ValidationUtils.isPositive(duration);

        assertThat(positive).isTrue();
    }

    @ParameterizedTest
    @MethodSource("nonPositiveDurations")
    void isNonPositive_shouldReturnTrue_whenNullOrNotPositive(final Duration duration) {
        final boolean nonPositive = ValidationUtils.isNonPositive(duration);

        assertThat(nonPositive).isTrue();
    }

    @Test
    void isNonPositive_shouldReturnFalse_whenDurationPositive() {
        final Duration duration = Duration.ofMillis(1);

        final boolean nonPositive = ValidationUtils.isNonPositive(duration);

        assertThat(nonPositive).isFalse();
    }

    @Test
    void requireAtMost_shouldReturnValue_whenWithinMaximum() {
        final int value = 100;
        final int maximum = 100;
        final String fieldName = "perPage";

        final int result = ValidationUtils.requireAtMost(value, maximum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireAtMost_shouldThrow_whenValueExceedsMaximum() {
        final int value = 101;
        final int maximum = 100;
        final String fieldName = "perPage";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireAtMost(value, maximum, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("perPage must be at most 100");
    }

    @Test
    void requireAtLeast_shouldReturnValue_whenWithinMinimum() {
        final double value = 0.0;
        final double minimum = 0.0;
        final String fieldName = "percentage";

        final double result = ValidationUtils.requireAtLeast(value, minimum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireAtLeast_shouldThrow_whenValueBelowMinimum() {
        final double value = -1.0;
        final double minimum = 0.0;
        final String fieldName = "percentage";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireAtLeast(value, minimum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("percentage must be finite and at least 0.0");
    }

    @Test
    void requireAtMost_shouldReturnValue_whenWithinMaximumForDouble() {
        final double value = 100.0;
        final double maximum = 100.0;
        final String fieldName = "percentage";

        final double result = ValidationUtils.requireAtMost(value, maximum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireAtMost_shouldThrow_whenDoubleValueExceedsMaximum() {
        final double value = 101.0;
        final double maximum = 100.0;
        final String fieldName = "percentage";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireAtMost(value, maximum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("percentage must be finite and at most 100.0");
    }

    @Test
    void requireAtMost_shouldThrow_whenDoubleValueIsNaN() {
        final double value = Double.NaN;
        final double maximum = 100.0;
        final String fieldName = "percentage";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireAtMost(value, maximum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("percentage must be finite and at most 100.0");
    }

    @Test
    void requireAtMost_shouldThrow_whenDoubleValueIsInfinite() {
        final double value = Double.POSITIVE_INFINITY;
        final double maximum = 100.0;
        final String fieldName = "percentage";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireAtMost(value, maximum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("percentage must be finite and at most 100.0");
    }

    @Test
    void requireBetween_shouldReturnValue_whenWithinBounds() {
        final double value = 50.0;
        final double minimum = 0.0;
        final double maximum = 100.0;
        final String fieldName = "percentage";

        final double result = ValidationUtils.requireBetween(value, minimum, maximum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireBetween_shouldThrow_whenValueBelowMinimum() {
        final double value = -1.0;
        final double minimum = 0.0;
        final double maximum = 100.0;
        final String fieldName = "percentage";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireBetween(value, minimum, maximum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("percentage must be finite and between 0.0 and 100.0");
    }

    @Test
    void requireBetween_shouldThrow_whenValueAboveMaximum() {
        final double value = 101.0;
        final double minimum = 0.0;
        final double maximum = 100.0;
        final String fieldName = "percentage";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireBetween(value, minimum, maximum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("percentage must be finite and between 0.0 and 100.0");
    }

    @Test
    void requireBetween_shouldThrow_whenValueIsNaN() {
        final double value = Double.NaN;
        final double minimum = 0.0;
        final double maximum = 100.0;
        final String fieldName = "percentage";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireBetween(value, minimum, maximum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("percentage must be finite and between 0.0 and 100.0");
    }

    @Test
    void requireBetween_shouldThrow_whenValueIsInfinite() {
        final double value = Double.POSITIVE_INFINITY;
        final double minimum = 0.0;
        final double maximum = 100.0;
        final String fieldName = "percentage";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireBetween(value, minimum, maximum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("percentage must be finite and between 0.0 and 100.0");
    }

    @Test
    void requireNonNull_shouldReturnValue_whenNonNull() {
        final String value = "token";
        final String fieldName = "token";

        final String result = ValidationUtils.requireNonNull(value, fieldName);

        assertThat(result).isSameAs(value);
    }

    @Test
    void requireNonNull_shouldThrow_whenNull() {
        final String value = null;
        final String fieldName = "token";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireNonNull(value, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage(fieldName + " must not be null");
    }

    @Test
    void requireNotBlank_shouldReturnValue_whenNonBlank() {
        final String value = "token";
        final String fieldName = "token";

        final String result = ValidationUtils.requireNotBlank(value, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireNotBlank_shouldThrow_whenNull() {
        final String value = null;
        final String fieldName = "token";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireNotBlank(value, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("token must not be null");
    }

    @Test
    void requireNotBlank_shouldThrow_whenBlank() {
        final String value = "  ";
        final String fieldName = "token";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireNotBlank(value, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("token must not be blank");
    }

    @Test
    void requireSizeAtMost_shouldReturnValue_whenWithinMaximum() {
        final List<String> values = List.of("a", "b");
        final int maximum = 2;
        final String fieldName = "items";

        final List<String> result = ValidationUtils.requireSizeAtMost(values, maximum, fieldName);

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeAtMost_shouldThrow_whenSizeExceedsMaximum() {
        final List<String> values = List.of("a", "b", "c");
        final int maximum = 2;
        final String fieldName = "items";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeAtMost(values, maximum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items must have at most 2 elements");
    }

    @Test
    void requireSizeAtLeast_shouldReturnValue_whenWithinMinimum() {
        final List<String> values = List.of("a", "b");
        final int minimum = 2;
        final String fieldName = "items";

        final List<String> result = ValidationUtils.requireSizeAtLeast(values, minimum, fieldName);

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeAtLeast_shouldThrow_whenSizeBelowMinimum() {
        final List<String> values = List.of("a");
        final int minimum = 2;
        final String fieldName = "items";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeAtLeast(values, minimum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items must have at least 2 elements");
    }

    @Test
    void requireSizeExactly_shouldReturnValue_whenSizeMatches() {
        final List<String> values = List.of("a");
        final int size = 1;
        final String fieldName = "items";

        final List<String> result = ValidationUtils.requireSizeExactly(values, size, fieldName);

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeExactly_shouldThrow_whenNull() {
        final List<String> values = null;
        final int size = 1;
        final String fieldName = "items";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeExactly(values, size, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("items must not be null");
    }

    @Test
    void requireSizeExactly_shouldThrow_whenSizeDiffers() {
        final List<String> values = List.of("a", "b");
        final int size = 1;
        final String fieldName = "items";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeExactly(values, size, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items must have exactly 1 elements");
    }

    @Test
    void requireSizeBetween_shouldReturnValue_whenWithinBounds() {
        final List<String> values = List.of("a", "b");
        final int minimum = 2;
        final int maximum = 100;
        final String fieldName = "items";

        final List<String> result = ValidationUtils.requireSizeBetween(values, minimum, maximum, fieldName);

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeBetween_shouldThrow_whenNull() {
        final List<String> values = null;
        final int minimum = 2;
        final int maximum = 100;
        final String fieldName = "items";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireSizeBetween(values, minimum, maximum, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("items must not be null");
    }

    @Test
    void requireSizeBetween_shouldThrow_whenCollectionBoundsInverted() {
        final List<String> values = List.of("a", "b");
        final int minimum = 5;
        final int maximum = 2;
        final String fieldName = "items";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireSizeBetween(values, minimum, maximum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items has bounds in the wrong order: minimum 5 is greater than maximum 2");
    }

    @Test
    void requireSizeBetween_shouldThrow_whenMapBoundsInverted() {
        final Map<String, String> values = Map.of("a", "1");
        final int minimum = 5;
        final int maximum = 2;
        final String fieldName = "entries";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireSizeBetween(values, minimum, maximum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("entries has bounds in the wrong order: minimum 5 is greater than maximum 2");
    }

    @Test
    void requireAtLeast_shouldThrow_whenDoubleMinimumIsPositiveInfinity() {
        final double value = 5.0;
        final double minimum = Double.POSITIVE_INFINITY;
        final String fieldName = "ratio";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireAtLeast(value, minimum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ratio must be finite and at least Infinity");
    }

    @Test
    void requireAtLeast_shouldReturnValue_whenDoubleValueIsNegativeZeroAndMinimumPositiveZero() {
        final double value = -0.0;
        final double minimum = 0.0;
        final String fieldName = "ratio";

        final double result = ValidationUtils.requireAtLeast(value, minimum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireSizeBetween_shouldThrow_whenSizeBelowMinimum() {
        final List<String> values = List.of("a");
        final int minimum = 2;
        final int maximum = 100;
        final String fieldName = "items";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireSizeBetween(values, minimum, maximum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items must have between 2 and 100 elements");
    }

    @Test
    void requireSizeBetween_shouldThrow_whenSizeExceedsMaximum() {
        final List<String> values = List.of("a", "b", "c");
        final int minimum = 1;
        final int maximum = 2;
        final String fieldName = "items";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireSizeBetween(values, minimum, maximum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items must have between 1 and 2 elements");
    }

    @Test
    void requireMaxLength_shouldReturnValue_whenWithinMaximum() {
        final String value = "hi";
        final int maximum = 2;
        final String fieldName = "summary";

        final String result = ValidationUtils.requireMaxLength(value, maximum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireMaxLength_shouldThrow_whenLongerThanMaximum() {
        final String value = "hey";
        final int maximum = 2;
        final String fieldName = "summary";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireMaxLength(value, maximum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("summary must have length at most 2");
    }

    @Test
    void requireMaxLengthIfPresent_shouldReturnValue_whenWithinMaximum() {
        final String value = "hi";
        final int maximum = 2;
        final String fieldName = "summary";

        final String result = ValidationUtils.requireMaxLengthIfPresent(value, maximum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireMaxLengthIfPresent_shouldThrow_whenLongerThanMaximum() {
        final String value = "hey";
        final int maximum = 2;
        final String fieldName = "summary";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireMaxLengthIfPresent(value, maximum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("summary must have length at most 2");
    }

    @Test
    void requireMatches_shouldReturnValue_whenPatternMatches() {
        final String value = "ssh-rsa AAA";
        final Pattern pattern = Pattern.compile("^ssh-rsa ");
        final String fieldName = "key";

        final String result = ValidationUtils.requireMatches(value, pattern, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireMatches_shouldThrow_whenPatternDoesNotMatch() {
        final String value = "not-a-key";
        final Pattern pattern = Pattern.compile("^ssh-rsa ");
        final String fieldName = "key";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireMatches(value, pattern, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("key must match the required pattern");
    }

    @Test
    void requirePositive_shouldReturnDuration_whenPositive() {
        final Duration duration = Duration.ofMillis(1);
        final String fieldName = "timeout";

        final Duration result = ValidationUtils.requirePositive(duration, fieldName);

        assertThat(result).isSameAs(duration);
    }

    @Test
    void requirePositive_shouldThrow_whenNull() {
        final Duration duration = null;
        final String fieldName = "timeout";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requirePositive(duration, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("timeout must not be null");
    }

    @ParameterizedTest
    @MethodSource("zeroAndNegativeDurations")
    void requirePositive_shouldThrow_whenZeroOrNegative(final Duration duration) {
        final String fieldName = "timeout";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requirePositive(duration, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("timeout must be positive");
    }

    @ParameterizedTest
    @MethodSource("zeroAndPositiveDurations")
    void requireNonNegative_shouldReturnDuration_whenZeroOrPositive(final Duration duration) {
        final String fieldName = "delay";

        final Duration result = ValidationUtils.requireNonNegative(duration, fieldName);

        assertThat(result).isEqualTo(duration);
    }

    @Test
    void requireNonNegative_shouldThrow_whenNull() {
        final Duration duration = null;
        final String fieldName = "delay";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireNonNegative(duration, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("delay must not be null");
    }

    @Test
    void requireNonNegative_shouldThrow_whenNegative() {
        final Duration duration = Duration.ofSeconds(-1);
        final String fieldName = "delay";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireNonNegative(duration, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("delay must not be negative");
    }

    @Test
    void requireAtMost_shouldReturnDuration_whenDurationWithinMaximum() {
        final Duration value = Duration.ofSeconds(2);
        final Duration maximum = Duration.ofSeconds(2);
        final String fieldName = "wait";

        final Duration result = ValidationUtils.requireAtMost(value, maximum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireAtMost_shouldThrow_whenDurationExceedsMaximum() {
        final Duration value = Duration.ofSeconds(3);
        final Duration maximum = Duration.ofSeconds(2);
        final String fieldName = "wait";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireAtMost(value, maximum, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("wait must be at most PT2S");
    }

    @Test
    void requireAtMost_shouldThrow_whenDurationNull() {
        final Duration value = null;
        final Duration maximum = Duration.ZERO;
        final String fieldName = "wait";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireAtMost(value, maximum, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("wait must not be null");
    }

    @Test
    void requireAtMost_shouldThrow_whenMaximumDurationNull() {
        final Duration value = Duration.ZERO;
        final Duration maximum = null;
        final String fieldName = "wait";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireAtMost(value, maximum, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("maximum must not be null");
    }

    @Test
    void requireAtLeast_shouldReturnInt_whenWithinMinimum() {
        final int value = 2;
        final int minimum = 2;
        final String fieldName = "count";

        final int result = ValidationUtils.requireAtLeast(value, minimum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireAtLeast_shouldThrow_whenIntBelowMinimum() {
        final int value = 1;
        final int minimum = 2;
        final String fieldName = "count";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireAtLeast(value, minimum, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("count must be at least 2");
    }

    @Test
    void requireAtLeast_shouldThrow_whenDoubleValueIsNaN() {
        final double value = Double.NaN;
        final double minimum = 0.0;
        final String fieldName = "percentage";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireAtLeast(value, minimum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("percentage must be finite and at least 0.0");
    }

    @Test
    void requireAtLeast_shouldThrow_whenDoubleValueIsInfinite() {
        final double value = Double.NEGATIVE_INFINITY;
        final double minimum = 0.0;
        final String fieldName = "percentage";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireAtLeast(value, minimum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("percentage must be finite and at least 0.0");
    }

    @Test
    void requireAtLeast_shouldReturnValue_whenDurationAtLeastMinimum() {
        final Duration value = Duration.ofSeconds(2);
        final Duration minimum = Duration.ofSeconds(2);
        final String fieldName = "wait";

        final Duration result = ValidationUtils.requireAtLeast(value, minimum, fieldName);

        assertThat(result).isSameAs(value);
    }

    @Test
    void requireAtLeast_shouldThrow_whenDurationBelowMinimum() {
        final Duration value = Duration.ofSeconds(1);
        final Duration minimum = Duration.ofSeconds(2);
        final String fieldName = "wait";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireAtLeast(value, minimum, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("wait must be at least PT2S");
    }

    @Test
    void requireAtLeast_shouldThrow_whenDurationNull() {
        final Duration value = null;
        final Duration minimum = Duration.ZERO;
        final String fieldName = "wait";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireAtLeast(value, minimum, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("wait must not be null");
    }

    @Test
    void requireAtLeast_shouldThrow_whenMinimumDurationNull() {
        final Duration value = Duration.ZERO;
        final Duration minimum = null;
        final String fieldName = "wait";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireAtLeast(value, minimum, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("minimum must not be null");
    }

    @Test
    void requireAtLeast_shouldThrow_whenDoubleMinimumIsNaN() {
        final double value = 1.0;
        final double minimum = Double.NaN;
        final String fieldName = "ratio";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireAtLeast(value, minimum, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("ratio has a NaN minimum");
    }

    @Test
    void requireAtMost_shouldThrow_whenDoubleMaximumIsNaN() {
        final double value = 1.0;
        final double maximum = Double.NaN;
        final String fieldName = "ratio";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireAtMost(value, maximum, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("ratio has a NaN maximum");
    }

    @Test
    void requireBetween_shouldThrow_whenIntBoundsSwapped() {
        final int value = 5;
        final String fieldName = "count";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireBetween(value, 10, 1, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("count has bounds in the wrong order: minimum 10 is greater than maximum 1");
    }

    @Test
    void requireBetween_shouldThrow_whenLongBoundsSwapped() {
        final long value = 5L;
        final String fieldName = "count";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireBetween(value, 10L, 1L, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("count has bounds in the wrong order: minimum 10 is greater than maximum 1");
    }

    @Test
    void requireBetween_shouldThrow_whenDoubleBoundsSwapped() {
        final double value = 5.0;
        final String fieldName = "ratio";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireBetween(value, 10.0, 1.0, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ratio has bounds in the wrong order: minimum 10.0 is greater than maximum 1.0");
    }

    @Test
    void requireBetween_shouldThrow_whenDoubleMinimumIsNaN() {
        final double value = 5.0;
        final String fieldName = "ratio";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireBetween(value, Double.NaN, 10.0, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("ratio has a NaN minimum");
    }

    @Test
    void requireBetween_shouldThrow_whenDoubleMaximumIsNaN() {
        final double value = 5.0;
        final String fieldName = "ratio";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireBetween(value, 1.0, Double.NaN, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("ratio has a NaN maximum");
    }

    @Test
    void requireFullMatch_shouldReturnValue_whenWholeValueMatches() {
        final String value = "abc";
        final Pattern pattern = Pattern.compile("[a-z]+");
        final String fieldName = "code";

        final String result = ValidationUtils.requireFullMatch(value, pattern, fieldName);

        assertThat(result).isSameAs(value);
    }

    @Test
    void requireFullMatch_shouldThrow_whenOnlyPartOfValueMatches() {
        final String value = "abc; DROP";
        final Pattern pattern = Pattern.compile("[a-z]+");
        final String fieldName = "code";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireFullMatch(value, pattern, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("code must match the required pattern");
    }

    @Test
    void requireFullMatchIfPresent_shouldReturnValue_whenWholeValueMatches() {
        final String value = "abc";
        final Pattern pattern = Pattern.compile("[a-z]+");
        final String fieldName = "code";

        final String result = ValidationUtils.requireFullMatchIfPresent(value, pattern, fieldName);

        assertThat(result).isSameAs(value);
    }

    @Test
    void requireFullMatchIfPresent_shouldThrow_whenOnlyPartOfValueMatches() {
        final String value = "abc; DROP";
        final Pattern pattern = Pattern.compile("[a-z]+");
        final String fieldName = "code";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireFullMatchIfPresent(value, pattern, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("code must match the required pattern");
    }

    @Test
    void requireFullMatchIfPresent_shouldReturnNull_whenValueNull() {
        final String value = null;
        final Pattern pattern = Pattern.compile("[a-z]+");
        final String fieldName = "code";

        final String result = ValidationUtils.requireFullMatchIfPresent(value, pattern, fieldName);

        assertThat(result).isNull();
    }

    @Test
    void requireFullMatch_shouldThrow_whenValueNull() {
        final String value = null;
        final Pattern pattern = Pattern.compile("[a-z]+");
        final String fieldName = "code";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireFullMatch(value, pattern, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("code must not be null");
    }

    @Test
    void requireFullMatch_shouldThrow_whenPatternNull() {
        final String value = "abc";
        final Pattern pattern = null;
        final String fieldName = "code";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireFullMatch(value, pattern, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("pattern must not be null");
    }

    @Test
    void requireNonEmpty_shouldReturnValue_whenNonEmpty() {
        final List<String> values = List.of("a");
        final String fieldName = "items";

        final List<String> result = ValidationUtils.requireNonEmpty(values, fieldName);

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireNonEmpty_shouldThrow_whenEmpty() {
        final List<String> values = List.of();
        final String fieldName = "items";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireNonEmpty(values, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("items must not be empty");
    }

    @Test
    void requireNonEmpty_shouldThrow_whenNull() {
        final List<String> values = null;
        final String fieldName = "items";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireNonEmpty(values, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("items must not be null");
    }

    @Test
    void requireSizeAtMost_shouldThrow_whenNull() {
        final List<String> values = null;
        final int maximum = 2;
        final String fieldName = "items";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeAtMost(values, maximum, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("items must not be null");
    }

    @Test
    void requireSizeAtLeast_shouldThrow_whenNull() {
        final List<String> values = null;
        final int minimum = 1;
        final String fieldName = "items";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeAtLeast(values, minimum, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("items must not be null");
    }

    @Test
    void requireUniqueItems_shouldReturnValue_whenUnique() {
        final List<String> values = List.of("a", "b");
        final String fieldName = "items";

        final List<String> result = ValidationUtils.requireUniqueItems(values, fieldName);

        assertThat(result).isSameAs(values);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void requireUniqueItems_shouldReturnValue_whenFewerThanTwoItems(final int size) {
        final List<String> values = Collections.nCopies(size, "a");
        final String fieldName = "items";

        final List<String> result = ValidationUtils.requireUniqueItems(values, fieldName);

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireUniqueItems_shouldThrow_whenDuplicates() {
        final List<String> values = List.of("a", "a");
        final String fieldName = "items";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireUniqueItems(values, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("items must not contain duplicates");
    }

    @Test
    void requireUniqueItems_shouldThrow_whenNullElementsDuplicate() {
        final List<String> values = Arrays.asList(null, null);
        final String fieldName = "items";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireUniqueItems(values, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("items must not contain duplicates");
    }

    @Test
    void requireUniqueItems_shouldReturnValue_whenOnlyOneNullElement() {
        final List<String> values = Arrays.asList("a", null);
        final String fieldName = "items";

        final List<String> result = ValidationUtils.requireUniqueItems(values, fieldName);

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireUniqueItems_shouldThrow_whenNull() {
        final List<String> values = null;
        final String fieldName = "items";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireUniqueItems(values, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("items must not be null");
    }

    @Test
    void requireNonEmpty_shouldReturnMap_whenNonEmpty() {
        final Map<String, String> values = Map.of("a", "1");
        final String fieldName = "props";

        final Map<String, String> result = ValidationUtils.requireNonEmpty(values, fieldName);

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireNonEmpty_shouldThrow_whenMapEmpty() {
        final Map<String, String> values = Map.of();
        final String fieldName = "props";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireNonEmpty(values, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("props must not be empty");
    }

    @Test
    void requireNonEmpty_shouldThrow_whenMapNull() {
        final Map<String, String> values = null;
        final String fieldName = "props";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireNonEmpty(values, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("props must not be null");
    }

    @Test
    void requireSizeAtMost_shouldReturnMap_whenWithinMaximum() {
        final Map<String, String> values = Map.of("a", "1");
        final int maximum = 1;
        final String fieldName = "props";

        final Map<String, String> result = ValidationUtils.requireSizeAtMost(values, maximum, fieldName);

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeAtMost_shouldThrow_whenMapSizeExceedsMaximum() {
        final Map<String, String> values = Map.of("a", "1", "b", "2");
        final int maximum = 1;
        final String fieldName = "props";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeAtMost(values, maximum, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("props must have at most 1 entries");
    }

    @Test
    void requireSizeAtMost_shouldThrow_whenMapNull() {
        final Map<String, String> values = null;
        final int maximum = 1;
        final String fieldName = "props";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeAtMost(values, maximum, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("props must not be null");
    }

    @Test
    void requireSizeAtLeast_shouldReturnMap_whenWithinMinimum() {
        final Map<String, String> values = Map.of("a", "1", "b", "2");
        final int minimum = 2;
        final String fieldName = "props";

        final Map<String, String> result = ValidationUtils.requireSizeAtLeast(values, minimum, fieldName);

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeAtLeast_shouldThrow_whenMapSizeBelowMinimum() {
        final Map<String, String> values = Map.of("a", "1");
        final int minimum = 2;
        final String fieldName = "props";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeAtLeast(values, minimum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("props must have at least 2 entries");
    }

    @Test
    void requireSizeAtLeast_shouldThrow_whenMapNull() {
        final Map<String, String> values = null;
        final int minimum = 1;
        final String fieldName = "props";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeAtLeast(values, minimum, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("props must not be null");
    }

    @Test
    void requireSizeExactly_shouldReturnMap_whenSizeMatches() {
        final Map<String, String> values = Map.of("a", "1");
        final int size = 1;
        final String fieldName = "props";

        final Map<String, String> result = ValidationUtils.requireSizeExactly(values, size, fieldName);

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeExactly_shouldThrow_whenMapNull() {
        final Map<String, String> values = null;
        final int size = 1;
        final String fieldName = "props";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeExactly(values, size, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("props must not be null");
    }

    @Test
    void requireSizeExactly_shouldThrow_whenMapSizeDiffers() {
        final Map<String, String> values = Map.of("a", "1", "b", "2");
        final int size = 1;
        final String fieldName = "props";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeExactly(values, size, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("props must have exactly 1 entries");
    }

    @Test
    void requireSizeBetween_shouldReturnMap_whenWithinBounds() {
        final Map<String, String> values = Map.of("a", "1", "b", "2");
        final int minimum = 2;
        final int maximum = 100;
        final String fieldName = "props";

        final Map<String, String> result = ValidationUtils.requireSizeBetween(values, minimum, maximum, fieldName);

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeBetween_shouldThrow_whenMapNull() {
        final Map<String, String> values = null;
        final int minimum = 2;
        final int maximum = 100;
        final String fieldName = "props";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireSizeBetween(values, minimum, maximum, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("props must not be null");
    }

    @Test
    void requireSizeBetween_shouldThrow_whenMapSizeBelowMinimum() {
        final Map<String, String> values = Map.of("a", "1");
        final int minimum = 2;
        final int maximum = 100;
        final String fieldName = "props";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireSizeBetween(values, minimum, maximum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("props must have between 2 and 100 entries");
    }

    @Test
    void requireSizeBetween_shouldThrow_whenMapSizeExceedsMaximum() {
        final Map<String, String> values = Map.of("a", "1", "b", "2", "c", "3");
        final int minimum = 1;
        final int maximum = 2;
        final String fieldName = "props";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireSizeBetween(values, minimum, maximum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("props must have between 1 and 2 entries");
    }

    @Test
    void requireMaxLengthIfPresent_shouldReturnNull_whenValueNull() {
        final String value = null;
        final int maximum = 2;
        final String fieldName = "summary";

        final String result = ValidationUtils.requireMaxLengthIfPresent(value, maximum, fieldName);

        assertThat(result).isNull();
    }

    @Test
    void requireMinLength_shouldReturnValue_whenWithinMinimum() {
        final String value = "hi";
        final int minLength = 2;
        final String fieldName = "summary";

        final String result = ValidationUtils.requireMinLength(value, minLength, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireMinLength_shouldThrow_whenShorterThanMinimum() {
        final String value = "h";
        final int minLength = 2;
        final String fieldName = "summary";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireMinLength(value, minLength, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("summary must have length at least 2");
    }

    @Test
    void requireMinLength_shouldThrow_whenValueNull() {
        final String value = null;
        final int minLength = 1;
        final String fieldName = "summary";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireMinLength(value, minLength, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("summary must not be null");
    }

    @Test
    void requireMinLengthIfPresent_shouldReturnValue_whenWithinMinimum() {
        final String value = "hi";
        final int minLength = 2;
        final String fieldName = "summary";

        final String result = ValidationUtils.requireMinLengthIfPresent(value, minLength, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireMinLengthIfPresent_shouldThrow_whenShorterThanMinimum() {
        final String value = "h";
        final int minLength = 2;
        final String fieldName = "summary";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireMinLengthIfPresent(value, minLength, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("summary must have length at least 2");
    }

    @Test
    void requireMinLengthIfPresent_shouldReturnNull_whenValueNull() {
        final String value = null;
        final int minLength = 2;
        final String fieldName = "summary";

        final String result = ValidationUtils.requireMinLengthIfPresent(value, minLength, fieldName);

        assertThat(result).isNull();
    }

    @Test
    void requireMatchesIfPresent_shouldReturnValue_whenPatternMatches() {
        final String value = "ssh-rsa AAA";
        final Pattern pattern = Pattern.compile("^ssh-rsa ");
        final String fieldName = "key";

        final String result = ValidationUtils.requireMatchesIfPresent(value, pattern, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireMatchesIfPresent_shouldThrow_whenPatternDoesNotMatch() {
        final String value = "not-a-key";
        final Pattern pattern = Pattern.compile("^ssh-rsa ");
        final String fieldName = "key";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireMatchesIfPresent(value, pattern, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("key must match the required pattern");
    }

    @Test
    void requireMatchesIfPresent_shouldReturnNull_whenValueNull() {
        final String value = null;
        final Pattern pattern = Pattern.compile("^ssh-rsa ");
        final String fieldName = "key";

        final String result = ValidationUtils.requireMatchesIfPresent(value, pattern, fieldName);

        assertThat(result).isNull();
    }

    @Test
    void requireMatchesIfPresent_shouldReturnNull_whenValueNullAndPatternNull() {
        final String value = null;
        final Pattern pattern = null;
        final String fieldName = "key";

        final String result = ValidationUtils.requireMatchesIfPresent(value, pattern, fieldName);

        assertThat(result).isNull();
    }

    @Test
    void requireNotBlankIfPresent_shouldReturnValue_whenNonBlank() {
        final String value = "token";
        final String fieldName = "token";

        final String result = ValidationUtils.requireNotBlankIfPresent(value, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireNotBlankIfPresent_shouldThrow_whenBlank() {
        final String value = "  ";
        final String fieldName = "token";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireNotBlankIfPresent(value, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("token must not be blank");
    }

    @Test
    void requireNotBlankIfPresent_shouldReturnNull_whenValueNull() {
        final String value = null;
        final String fieldName = "token";

        final String result = ValidationUtils.requireNotBlankIfPresent(value, fieldName);

        assertThat(result).isNull();
    }

    @Test
    void requireAtLeast_shouldReturnLong_whenWithinMinimum() {
        final long value = 2L;
        final long minimum = 2L;
        final String fieldName = "id";

        final long result = ValidationUtils.requireAtLeast(value, minimum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireAtLeast_shouldThrow_whenLongBelowMinimum() {
        final long value = 1L;
        final long minimum = 2L;
        final String fieldName = "id";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireAtLeast(value, minimum, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("id must be at least 2");
    }

    @Test
    void requireAtMost_shouldReturnLong_whenWithinMaximum() {
        final long value = 100L;
        final long maximum = 100L;
        final String fieldName = "id";

        final long result = ValidationUtils.requireAtMost(value, maximum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireAtMost_shouldThrow_whenLongExceedsMaximum() {
        final long value = 101L;
        final long maximum = 100L;
        final String fieldName = "id";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireAtMost(value, maximum, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("id must be at most 100");
    }

    @Test
    void requireBetween_shouldReturnInt_whenWithinBounds() {
        final int value = 50;
        final int minimum = 0;
        final int maximum = 100;
        final String fieldName = "count";

        final int result = ValidationUtils.requireBetween(value, minimum, maximum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireBetween_shouldThrow_whenIntBelowMinimum() {
        final int value = -1;
        final int minimum = 0;
        final int maximum = 100;
        final String fieldName = "count";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireBetween(value, minimum, maximum, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("count must be between 0 and 100");
    }

    @Test
    void requireBetween_shouldThrow_whenIntAboveMaximum() {
        final int value = 101;
        final int minimum = 0;
        final int maximum = 100;
        final String fieldName = "count";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireBetween(value, minimum, maximum, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("count must be between 0 and 100");
    }

    @Test
    void requireBetween_shouldReturnLong_whenWithinBounds() {
        final long value = 50L;
        final long minimum = 0L;
        final long maximum = 100L;
        final String fieldName = "id";

        final long result = ValidationUtils.requireBetween(value, minimum, maximum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireBetween_shouldThrow_whenLongBelowMinimum() {
        final long value = -1L;
        final long minimum = 0L;
        final long maximum = 100L;
        final String fieldName = "id";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireBetween(value, minimum, maximum, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("id must be between 0 and 100");
    }

    @Test
    void requireBetween_shouldThrow_whenLongAboveMaximum() {
        final long value = 101L;
        final long minimum = 0L;
        final long maximum = 100L;
        final String fieldName = "id";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireBetween(value, minimum, maximum, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("id must be between 0 and 100");
    }

    @Test
    void requireAtLeastIfPresent_shouldReturnValue_whenIntegerWithinMinimum() {
        final Integer value = 2;
        final int minimum = 2;
        final String fieldName = "count";

        final Integer result = ValidationUtils.requireAtLeastIfPresent(value, minimum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireAtLeastIfPresent_shouldThrow_whenIntegerBelowMinimum() {
        final Integer value = 1;
        final int minimum = 2;
        final String fieldName = "count";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireAtLeastIfPresent(value, minimum, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("count must be at least 2");
    }

    @Test
    void requireAtLeastIfPresent_shouldReturnNull_whenIntegerNull() {
        final Integer value = null;
        final int minimum = 2;
        final String fieldName = "count";

        final Integer result = ValidationUtils.requireAtLeastIfPresent(value, minimum, fieldName);

        assertThat(result).isNull();
    }

    @Test
    void requireAtLeastIfPresent_shouldReturnValue_whenLongWithinMinimum() {
        final Long value = 2L;
        final long minimum = 2L;
        final String fieldName = "id";

        final Long result = ValidationUtils.requireAtLeastIfPresent(value, minimum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireAtLeastIfPresent_shouldThrow_whenLongBelowMinimum() {
        final Long value = 1L;
        final long minimum = 2L;
        final String fieldName = "id";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireAtLeastIfPresent(value, minimum, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("id must be at least 2");
    }

    @Test
    void requireAtLeastIfPresent_shouldReturnNull_whenLongNull() {
        final Long value = null;
        final long minimum = 2L;
        final String fieldName = "id";

        final Long result = ValidationUtils.requireAtLeastIfPresent(value, minimum, fieldName);

        assertThat(result).isNull();
    }

    @Test
    void requireAtLeastIfPresent_shouldReturnValue_whenDoubleWithinMinimum() {
        final Double value = 0.0;
        final double minimum = 0.0;
        final String fieldName = "percentage";

        final Double result = ValidationUtils.requireAtLeastIfPresent(value, minimum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireAtLeastIfPresent_shouldThrow_whenDoubleBelowMinimum() {
        final Double value = -1.0;
        final double minimum = 0.0;
        final String fieldName = "percentage";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireAtLeastIfPresent(value, minimum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("percentage must be finite and at least 0.0");
    }

    @Test
    void requireAtLeastIfPresent_shouldReturnNull_whenDoubleNull() {
        final Double value = null;
        final double minimum = 0.0;
        final String fieldName = "percentage";

        final Double result = ValidationUtils.requireAtLeastIfPresent(value, minimum, fieldName);

        assertThat(result).isNull();
    }

    @Test
    void requireAtMostIfPresent_shouldReturnValue_whenIntegerWithinMaximum() {
        final Integer value = 100;
        final int maximum = 100;
        final String fieldName = "count";

        final Integer result = ValidationUtils.requireAtMostIfPresent(value, maximum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireAtMostIfPresent_shouldThrow_whenIntegerExceedsMaximum() {
        final Integer value = 101;
        final int maximum = 100;
        final String fieldName = "count";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireAtMostIfPresent(value, maximum, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("count must be at most 100");
    }

    @Test
    void requireAtMostIfPresent_shouldReturnNull_whenIntegerNull() {
        final Integer value = null;
        final int maximum = 100;
        final String fieldName = "count";

        final Integer result = ValidationUtils.requireAtMostIfPresent(value, maximum, fieldName);

        assertThat(result).isNull();
    }

    @Test
    void requireAtMostIfPresent_shouldReturnValue_whenLongWithinMaximum() {
        final Long value = 100L;
        final long maximum = 100L;
        final String fieldName = "id";

        final Long result = ValidationUtils.requireAtMostIfPresent(value, maximum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireAtMostIfPresent_shouldThrow_whenLongExceedsMaximum() {
        final Long value = 101L;
        final long maximum = 100L;
        final String fieldName = "id";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireAtMostIfPresent(value, maximum, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("id must be at most 100");
    }

    @Test
    void requireAtMostIfPresent_shouldReturnNull_whenLongNull() {
        final Long value = null;
        final long maximum = 100L;
        final String fieldName = "id";

        final Long result = ValidationUtils.requireAtMostIfPresent(value, maximum, fieldName);

        assertThat(result).isNull();
    }

    @Test
    void requireAtMostIfPresent_shouldReturnValue_whenDoubleWithinMaximum() {
        final Double value = 100.0;
        final double maximum = 100.0;
        final String fieldName = "percentage";

        final Double result = ValidationUtils.requireAtMostIfPresent(value, maximum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireAtMostIfPresent_shouldThrow_whenDoubleExceedsMaximum() {
        final Double value = 101.0;
        final double maximum = 100.0;
        final String fieldName = "percentage";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireAtMostIfPresent(value, maximum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("percentage must be finite and at most 100.0");
    }

    @Test
    void requireAtMostIfPresent_shouldReturnNull_whenDoubleNull() {
        final Double value = null;
        final double maximum = 100.0;
        final String fieldName = "percentage";

        final Double result = ValidationUtils.requireAtMostIfPresent(value, maximum, fieldName);

        assertThat(result).isNull();
    }

    @Test
    void requireBetweenIfPresent_shouldReturnValue_whenIntegerWithinBounds() {
        final Integer value = 50;
        final int minimum = 0;
        final int maximum = 100;
        final String fieldName = "count";

        final Integer result = ValidationUtils.requireBetweenIfPresent(value, minimum, maximum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireBetweenIfPresent_shouldThrow_whenIntegerOutOfBounds() {
        final Integer value = 101;
        final int minimum = 0;
        final int maximum = 100;
        final String fieldName = "count";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireBetweenIfPresent(value, minimum, maximum, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("count must be between 0 and 100");
    }

    @Test
    void requireBetweenIfPresent_shouldReturnNull_whenIntegerNull() {
        final Integer value = null;
        final int minimum = 0;
        final int maximum = 100;
        final String fieldName = "count";

        final Integer result = ValidationUtils.requireBetweenIfPresent(value, minimum, maximum, fieldName);

        assertThat(result).isNull();
    }

    @Test
    void requireBetweenIfPresent_shouldReturnValue_whenLongWithinBounds() {
        final Long value = 50L;
        final long minimum = 0L;
        final long maximum = 100L;
        final String fieldName = "id";

        final Long result = ValidationUtils.requireBetweenIfPresent(value, minimum, maximum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireBetweenIfPresent_shouldThrow_whenLongOutOfBounds() {
        final Long value = -1L;
        final long minimum = 0L;
        final long maximum = 100L;
        final String fieldName = "id";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireBetweenIfPresent(value, minimum, maximum, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("id must be between 0 and 100");
    }

    @Test
    void requireBetweenIfPresent_shouldReturnNull_whenLongNull() {
        final Long value = null;
        final long minimum = 0L;
        final long maximum = 100L;
        final String fieldName = "id";

        final Long result = ValidationUtils.requireBetweenIfPresent(value, minimum, maximum, fieldName);

        assertThat(result).isNull();
    }

    @Test
    void requireBetweenIfPresent_shouldReturnValue_whenDoubleWithinBounds() {
        final Double value = 50.0;
        final double minimum = 0.0;
        final double maximum = 100.0;
        final String fieldName = "percentage";

        final Double result = ValidationUtils.requireBetweenIfPresent(value, minimum, maximum, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @Test
    void requireBetweenIfPresent_shouldThrow_whenDoubleOutOfBounds() {
        final Double value = 101.0;
        final double minimum = 0.0;
        final double maximum = 100.0;
        final String fieldName = "percentage";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireBetweenIfPresent(value, minimum, maximum, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("percentage must be finite and between 0.0 and 100.0");
    }

    @Test
    void requireBetweenIfPresent_shouldReturnNull_whenDoubleNull() {
        final Double value = null;
        final double minimum = 0.0;
        final double maximum = 100.0;
        final String fieldName = "percentage";

        final Double result = ValidationUtils.requireBetweenIfPresent(value, minimum, maximum, fieldName);

        assertThat(result).isNull();
    }

    @Test
    void requireMatches_shouldThrow_whenValueNull() {
        final String value = null;
        final Pattern pattern = Pattern.compile("^ssh-rsa ");
        final String fieldName = "key";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireMatches(value, pattern, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("key must not be null");
    }

    @Test
    void requireMatches_shouldThrow_whenPatternNull() {
        final String value = "ssh-rsa AAA";
        final Pattern pattern = null;
        final String fieldName = "key";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireMatches(value, pattern, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("pattern must not be null");
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,true"})
    void requireExactlyOne_shouldNotThrow_whenExactlyOneOfTwoPresent(final boolean first, final boolean second) {
        final String fieldName = "options";

        assertThatCode(() -> ValidationUtils.requireExactlyOne(present(first), present(second), fieldName))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,true"})
    void requireExactlyOne_shouldThrow_whenNoneOrBothOfTwoPresent(final boolean first, final boolean second) {
        final String fieldName = "options";

        final Object firstValue = present(first);
        final Object secondValue = present(second);

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireExactlyOne(firstValue, secondValue, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("options must have exactly one value present");
    }

    @ParameterizedTest
    @CsvSource({"true,false,false", "false,true,false", "false,false,true"})
    void requireExactlyOne_shouldNotThrow_whenExactlyOneOfThreePresent(
            final boolean first, final boolean second, final boolean third) {
        final String fieldName = "options";

        assertThatCode(() ->
                        ValidationUtils.requireExactlyOne(present(first), present(second), present(third), fieldName))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"false,false,false", "true,true,false", "true,false,true", "false,true,true", "true,true,true"})
    void requireExactlyOne_shouldThrow_whenNoneOrManyOfThreePresent(
            final boolean first, final boolean second, final boolean third) {
        final String fieldName = "options";

        final Object firstValue = present(first);
        final Object secondValue = present(second);
        final Object thirdValue = present(third);

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireExactlyOne(firstValue, secondValue, thirdValue, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("options must have exactly one value present");
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true"})
    void requireAtMostOne_shouldNotThrow_whenAtMostOneOfTwoPresent(final boolean first, final boolean second) {
        final String fieldName = "options";

        assertThatCode(() -> ValidationUtils.requireAtMostOne(present(first), present(second), fieldName))
                .doesNotThrowAnyException();
    }

    @Test
    void requireAtMostOne_shouldThrow_whenBothOfTwoPresent() {
        final String fieldName = "options";

        final Object firstValue = present(true);
        final Object secondValue = present(true);

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireAtMostOne(firstValue, secondValue, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("options must have at most one value present");
    }

    @ParameterizedTest
    @CsvSource({"false,false,false", "true,false,false", "false,true,false", "false,false,true"})
    void requireAtMostOne_shouldNotThrow_whenAtMostOneOfThreePresent(
            final boolean first, final boolean second, final boolean third) {
        final String fieldName = "options";

        assertThatCode(() ->
                        ValidationUtils.requireAtMostOne(present(first), present(second), present(third), fieldName))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"true,true,false", "true,false,true", "false,true,true", "true,true,true"})
    void requireAtMostOne_shouldThrow_whenManyOfThreePresent(
            final boolean first, final boolean second, final boolean third) {
        final String fieldName = "options";

        final Object firstValue = present(first);
        final Object secondValue = present(second);
        final Object thirdValue = present(third);

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireAtMostOne(firstValue, secondValue, thirdValue, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("options must have at most one value present");
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,true", "true,true"})
    void requireAtLeastOne_shouldNotThrow_whenAnyOfTwoPresent(final boolean first, final boolean second) {
        final String fieldName = "options";

        assertThatCode(() -> ValidationUtils.requireAtLeastOne(present(first), present(second), fieldName))
                .doesNotThrowAnyException();
    }

    @Test
    void requireAtLeastOne_shouldThrow_whenNoneOfTwoPresent() {
        final String fieldName = "options";

        final Object firstValue = present(false);
        final Object secondValue = present(false);

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireAtLeastOne(firstValue, secondValue, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("options must have at least one value present");
    }

    @ParameterizedTest
    @CsvSource({
        "true,false,false",
        "false,true,false",
        "false,false,true",
        "true,true,false",
        "true,false,true",
        "false,true,true",
        "true,true,true"
    })
    void requireAtLeastOne_shouldNotThrow_whenAnyOfThreePresent(
            final boolean first, final boolean second, final boolean third) {
        final String fieldName = "options";

        assertThatCode(() ->
                        ValidationUtils.requireAtLeastOne(present(first), present(second), present(third), fieldName))
                .doesNotThrowAnyException();
    }

    @Test
    void requireAtLeastOne_shouldThrow_whenNoneOfThreePresent() {
        final String fieldName = "options";

        final Object firstValue = present(false);
        final Object secondValue = present(false);
        final Object thirdValue = present(false);

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireAtLeastOne(firstValue, secondValue, thirdValue, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("options must have at least one value present");
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,true"})
    void requireBothOrNeither_shouldNotThrow_whenBothOrNeitherPresent(final boolean first, final boolean second) {
        final String fieldName = "options";

        assertThatCode(() -> ValidationUtils.requireBothOrNeither(present(first), present(second), fieldName))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,true"})
    void requireBothOrNeither_shouldThrow_whenOnlyOnePresent(final boolean first, final boolean second) {
        final String fieldName = "options";

        final Object firstValue = present(first);
        final Object secondValue = present(second);

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireBothOrNeither(firstValue, secondValue, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("options must have both values present or both absent");
    }

    @Test
    void requireExactlyOne_shouldCountAsPresent_whenFalseBooleanPassed() {
        final String fieldName = "options";

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireExactlyOne(Boolean.FALSE, Boolean.FALSE, fieldName));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("options must have exactly one value present");
    }

    @Test
    void requirePositive_shouldReturnValue_whenIntPositive() {
        final int value = 1;
        final String fieldName = "value";

        final int result = ValidationUtils.requirePositive(value, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void requirePositive_shouldThrow_whenIntZeroOrNegative(final int value) {
        final String fieldName = "value";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requirePositive(value, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("value must be positive");
    }

    @Test
    void requirePositive_shouldReturnValue_whenLongPositive() {
        final long value = 1L;
        final String fieldName = "value";

        final long result = ValidationUtils.requirePositive(value, fieldName);

        assertThat(result).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void requirePositive_shouldThrow_whenLongZeroOrNegative(final long value) {
        final String fieldName = "value";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requirePositive(value, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("value must be positive");
    }

    @Test
    void requireNonEmpty_shouldReturnSameArray_whenArrayNonEmpty() {
        final String[] values = {"a"};
        final String fieldName = "values";

        final String[] result = ValidationUtils.requireNonEmpty(values, fieldName);

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireNonEmpty_shouldThrow_whenArrayEmpty() {
        final String[] values = {};
        final String fieldName = "values";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireNonEmpty(values, fieldName));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("values must not be empty");
    }

    @Test
    void requireNonEmpty_shouldThrow_whenArrayNull() {
        final String[] values = null;
        final String fieldName = "values";

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireNonEmpty(values, fieldName));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("values must not be null");
    }

    private static @Nullable Object present(final boolean present) {
        return present ? "value" : null;
    }

    private static Stream<Duration> nonPositiveDurations() {
        return Stream.of(null, Duration.ZERO, Duration.ofSeconds(-1));
    }

    private static Stream<Duration> zeroAndNegativeDurations() {
        return Stream.of(Duration.ZERO, Duration.ofSeconds(-1));
    }

    private static Stream<Duration> zeroAndPositiveDurations() {
        return Stream.of(Duration.ZERO, Duration.ofMillis(1));
    }
}

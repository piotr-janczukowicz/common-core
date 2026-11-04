package io.github.piotr_janczukowicz.util;

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Argument validation helpers.
 *
 * <p>Fail-fast checks for method arguments and configuration. Each {@code require*} method either returns the checked
 * value (so the result can be assigned) or throws; the exceptions are the {@code requireExactlyOne},
 * {@code requireAtMostOne}, {@code requireAtLeastOne} and {@code requireBothOrNeither} group checks, which return
 * nothing. {@code fieldName} names the checked value; the exception text is built from it, for example {@code "timeout
 * must be positive"}. {@code null} is rejected with {@link NullPointerException}; other violations use
 * {@link IllegalArgumentException}. Numeric bounds are inclusive unless a method says otherwise.
 */
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.GodClass", "PMD.CyclomaticComplexity", "PMD.TooManyMethods"})
public final class ValidationUtils {

    private static final int ONE_PRESENT = 1;
    private static final String MINIMUM = "minimum";
    private static final String MAXIMUM = "maximum";
    private static final String ELEMENTS = " elements";
    private static final String ENTRIES = " entries";
    private static final String MUST_BE_AT_LEAST = "must be at least ";
    private static final String MUST_BE_AT_MOST = "must be at most ";
    private static final String MUST_BE_POSITIVE = "must be positive";
    private static final String MUST_NOT_BE_EMPTY = "must not be empty";

    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Requires {@code value} to be non-null.
     *
     * @param value value to check
     * @param fieldName name of the checked value, used in the exception message
     * @param <T> value type
     * @return {@code value}
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static <T> T requireNonNull(@Nullable final T value, final String fieldName) {
        return Objects.requireNonNull(value, () -> fieldName + " must not be null");
    }

    /**
     * Requires {@code value} to be non-null and not blank.
     *
     * <p>Blank follows {@link String#isBlank()}: empty or only Unicode whitespace. Surrounding whitespace is not
     * stripped; a value that contains non-whitespace still passes even if it also has spaces.
     *
     * @param value value to check
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is blank
     */
    public static String requireNotBlank(@Nullable final String value, final String fieldName) {
        final String nonNull = requireNonNull(value, fieldName);
        if (nonNull.isBlank()) {
            throw invalid(fieldName, "must not be blank");
        }
        return nonNull;
    }

    /**
     * Requires {@code value} to be not blank when it is non-null.
     *
     * <p>{@code null} is allowed and returned unchanged. A present value is checked with
     * {@link #requireNotBlank(String, String)}.
     *
     * @param value value to check, or {@code null}
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value} is blank
     */
    public static @Nullable String requireNotBlankIfPresent(@Nullable final String value, final String fieldName) {
        if (value != null) {
            requireNotBlank(value, fieldName);
        }
        return value;
    }

    /**
     * Requires {@code duration} to be strictly positive.
     *
     * @param duration duration to check
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code duration}
     * @throws NullPointerException if {@code duration} is {@code null}
     * @throws IllegalArgumentException if {@code duration} is negative or zero
     */
    public static Duration requirePositive(final Duration duration, final String fieldName) {
        requireNonNull(duration, fieldName);

        if (!duration.isPositive()) {
            throw invalid(fieldName, MUST_BE_POSITIVE);
        }
        return duration;
    }

    /**
     * Requires {@code value} to be strictly positive.
     *
     * @param value value to check
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value} is zero or negative
     */
    public static int requirePositive(final int value, final String fieldName) {
        if (value <= 0) {
            throw invalid(fieldName, MUST_BE_POSITIVE);
        }
        return value;
    }

    /**
     * Requires {@code value} to be strictly positive.
     *
     * @param value value to check
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value} is zero or negative
     */
    public static long requirePositive(final long value, final String fieldName) {
        if (value <= 0) {
            throw invalid(fieldName, MUST_BE_POSITIVE);
        }
        return value;
    }

    /**
     * Requires {@code duration} to be zero or positive.
     *
     * @param duration duration to check
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code duration}
     * @throws NullPointerException if {@code duration} is {@code null}
     * @throws IllegalArgumentException if {@code duration} is negative
     */
    public static Duration requireNonNegative(final Duration duration, final String fieldName) {
        requireNonNull(duration, fieldName);

        if (duration.isNegative()) {
            throw invalid(fieldName, "must not be negative");
        }
        return duration;
    }

    /**
     * Requires {@code value} to be at least {@code minimum}.
     *
     * @param value value to check
     * @param minimum inclusive lower bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value} is below {@code minimum}
     */
    public static int requireAtLeast(final int value, final int minimum, final String fieldName) {
        if (value < minimum) {
            throw invalid(fieldName, MUST_BE_AT_LEAST + minimum);
        }
        return value;
    }

    /**
     * Requires {@code value} to be at least {@code minimum}.
     *
     * @param value value to check
     * @param minimum inclusive lower bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value} is below {@code minimum}
     */
    public static long requireAtLeast(final long value, final long minimum, final String fieldName) {
        if (value < minimum) {
            throw invalid(fieldName, MUST_BE_AT_LEAST + minimum);
        }
        return value;
    }

    /**
     * Requires {@code value} to be finite and at least {@code minimum}.
     *
     * <p>The lower bound is inclusive. {@link Double#NaN}, infinities, and values below {@code minimum} fail. Finite is
     * checked before the bound so a NaN does not compare as in-range.
     *
     * @param value value to check
     * @param minimum inclusive lower bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code minimum} is NaN, or {@code value} is non-finite or below
     *     {@code minimum}
     */
    public static double requireAtLeast(final double value, final double minimum, final String fieldName) {
        requireNotNaN(minimum, MINIMUM, fieldName);

        if (!Double.isFinite(value) || value < minimum) {
            throw invalid(fieldName, "must be finite and at least " + minimum);
        }
        return value;
    }

    /**
     * Requires {@code value} to be at least {@code minimum}.
     *
     * @param value duration to check
     * @param minimum inclusive lower bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws NullPointerException if {@code value} or {@code minimum} is {@code null}
     * @throws IllegalArgumentException if {@code value} is below {@code minimum}
     */
    public static Duration requireAtLeast(final Duration value, final Duration minimum, final String fieldName) {
        requireNonNull(value, fieldName);
        requireNonNull(minimum, MINIMUM);

        if (value.compareTo(minimum) < 0) {
            throw invalid(fieldName, MUST_BE_AT_LEAST + minimum);
        }
        return value;
    }

    /**
     * Requires {@code value} to be at least {@code minimum} when it is non-null.
     *
     * <p>{@code null} is allowed and returned unchanged. A present value is checked with {@link #requireAtLeast(int,
     * int, String)}.
     *
     * @param value value to check, or {@code null}
     * @param minimum inclusive lower bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value} is below {@code minimum}
     */
    public static @Nullable Integer requireAtLeastIfPresent(
            @Nullable final Integer value, final int minimum, final String fieldName) {
        if (value != null) {
            requireAtLeast(value, minimum, fieldName);
        }
        return value;
    }

    /**
     * Requires {@code value} to be at least {@code minimum} when it is non-null.
     *
     * <p>{@code null} is allowed and returned unchanged. A present value is checked with {@link #requireAtLeast(long,
     * long, String)}.
     *
     * @param value value to check, or {@code null}
     * @param minimum inclusive lower bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value} is below {@code minimum}
     */
    public static @Nullable Long requireAtLeastIfPresent(
            @Nullable final Long value, final long minimum, final String fieldName) {
        if (value != null) {
            requireAtLeast(value, minimum, fieldName);
        }
        return value;
    }

    /**
     * Requires {@code value} to be finite and at least {@code minimum} when it is non-null.
     *
     * <p>{@code null} is allowed and returned unchanged. A present value is checked with {@link #requireAtLeast(double,
     * double, String)}.
     *
     * @param value value to check, or {@code null}
     * @param minimum inclusive lower bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value} is non-finite or below {@code minimum}
     */
    public static @Nullable Double requireAtLeastIfPresent(
            @Nullable final Double value, final double minimum, final String fieldName) {
        if (value != null) {
            requireAtLeast(value, minimum, fieldName);
        }
        return value;
    }

    /**
     * Requires {@code value} to be at most {@code maximum}.
     *
     * @param value value to check
     * @param maximum inclusive upper bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value} is above {@code maximum}
     */
    public static int requireAtMost(final int value, final int maximum, final String fieldName) {
        if (value > maximum) {
            throw invalid(fieldName, MUST_BE_AT_MOST + maximum);
        }
        return value;
    }

    /**
     * Requires {@code value} to be at most {@code maximum}.
     *
     * @param value value to check
     * @param maximum inclusive upper bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value} is above {@code maximum}
     */
    public static long requireAtMost(final long value, final long maximum, final String fieldName) {
        if (value > maximum) {
            throw invalid(fieldName, MUST_BE_AT_MOST + maximum);
        }
        return value;
    }

    /**
     * Requires {@code value} to be finite and at most {@code maximum}.
     *
     * @param value value to check
     * @param maximum inclusive upper bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code maximum} is NaN, or {@code value} is non-finite or above
     *     {@code maximum}
     */
    public static double requireAtMost(final double value, final double maximum, final String fieldName) {
        requireNotNaN(maximum, MAXIMUM, fieldName);

        if (!Double.isFinite(value) || value > maximum) {
            throw invalid(fieldName, "must be finite and at most " + maximum);
        }
        return value;
    }

    /**
     * Requires {@code value} to be at most {@code maximum}.
     *
     * @param value duration to check
     * @param maximum inclusive upper bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws NullPointerException if {@code value} or {@code maximum} is {@code null}
     * @throws IllegalArgumentException if {@code value} is above {@code maximum}
     */
    public static Duration requireAtMost(final Duration value, final Duration maximum, final String fieldName) {
        requireNonNull(value, fieldName);
        requireNonNull(maximum, MAXIMUM);

        if (value.compareTo(maximum) > 0) {
            throw invalid(fieldName, MUST_BE_AT_MOST + maximum);
        }
        return value;
    }

    /**
     * Requires {@code value} to be at most {@code maximum} when it is non-null.
     *
     * <p>{@code null} is allowed and returned unchanged. A present value is checked with {@link #requireAtMost(int,
     * int, String)}.
     *
     * @param value value to check, or {@code null}
     * @param maximum inclusive upper bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value} is above {@code maximum}
     */
    public static @Nullable Integer requireAtMostIfPresent(
            @Nullable final Integer value, final int maximum, final String fieldName) {
        if (value != null) {
            requireAtMost(value, maximum, fieldName);
        }
        return value;
    }

    /**
     * Requires {@code value} to be at most {@code maximum} when it is non-null.
     *
     * <p>{@code null} is allowed and returned unchanged. A present value is checked with {@link #requireAtMost(long,
     * long, String)}.
     *
     * @param value value to check, or {@code null}
     * @param maximum inclusive upper bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value} is above {@code maximum}
     */
    public static @Nullable Long requireAtMostIfPresent(
            @Nullable final Long value, final long maximum, final String fieldName) {
        if (value != null) {
            requireAtMost(value, maximum, fieldName);
        }
        return value;
    }

    /**
     * Requires {@code value} to be finite and at most {@code maximum} when it is non-null.
     *
     * <p>{@code null} is allowed and returned unchanged. A present value is checked with {@link #requireAtMost(double,
     * double, String)}.
     *
     * @param value value to check, or {@code null}
     * @param maximum inclusive upper bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value} is non-finite or above {@code maximum}
     */
    public static @Nullable Double requireAtMostIfPresent(
            @Nullable final Double value, final double maximum, final String fieldName) {
        if (value != null) {
            requireAtMost(value, maximum, fieldName);
        }
        return value;
    }

    /**
     * Requires {@code value} to be within {@code minimum} and {@code maximum}, inclusive.
     *
     * @param value value to check
     * @param minimum inclusive lower bound
     * @param maximum inclusive upper bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code minimum} is above {@code maximum}, or {@code value} is outside the
     *     range
     */
    public static int requireBetween(final int value, final int minimum, final int maximum, final String fieldName) {
        if (minimum > maximum) {
            throw invertedBounds(fieldName, minimum, maximum);
        }

        if (value < minimum || value > maximum) {
            throw invalid(fieldName, "must be " + between(minimum, maximum));
        }
        return value;
    }

    /**
     * Requires {@code value} to be within {@code minimum} and {@code maximum}, inclusive.
     *
     * @param value value to check
     * @param minimum inclusive lower bound
     * @param maximum inclusive upper bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code minimum} is above {@code maximum}, or {@code value} is outside the
     *     range
     */
    public static long requireBetween(
            final long value, final long minimum, final long maximum, final String fieldName) {
        if (minimum > maximum) {
            throw invertedBounds(fieldName, minimum, maximum);
        }

        if (value < minimum || value > maximum) {
            throw invalid(fieldName, "must be " + between(minimum, maximum));
        }
        return value;
    }

    /**
     * Requires {@code value} to be finite and within {@code minimum} and {@code maximum}, inclusive.
     *
     * @param value value to check
     * @param minimum inclusive lower bound
     * @param maximum inclusive upper bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if a bound is NaN, {@code minimum} is above {@code maximum}, or {@code value} is
     *     non-finite or outside the range
     */
    public static double requireBetween(
            final double value, final double minimum, final double maximum, final String fieldName) {
        requireNotNaN(minimum, MINIMUM, fieldName);
        requireNotNaN(maximum, MAXIMUM, fieldName);
        if (minimum > maximum) {
            throw invertedBounds(fieldName, minimum, maximum);
        }

        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw invalid(fieldName, "must be finite and " + between(minimum, maximum));
        }
        return value;
    }

    /**
     * Requires {@code value} to be within {@code minimum} and {@code maximum}, inclusive, when it is non-null.
     *
     * <p>{@code null} is allowed and returned unchanged. A present value is checked with {@link #requireBetween(int,
     * int, int, String)}.
     *
     * @param value value to check, or {@code null}
     * @param minimum inclusive lower bound
     * @param maximum inclusive upper bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value} is outside the range
     */
    public static @Nullable Integer requireBetweenIfPresent(
            @Nullable final Integer value, final int minimum, final int maximum, final String fieldName) {
        if (value != null) {
            requireBetween(value, minimum, maximum, fieldName);
        }
        return value;
    }

    /**
     * Requires {@code value} to be within {@code minimum} and {@code maximum}, inclusive, when it is non-null.
     *
     * <p>{@code null} is allowed and returned unchanged. A present value is checked with {@link #requireBetween(long,
     * long, long, String)}.
     *
     * @param value value to check, or {@code null}
     * @param minimum inclusive lower bound
     * @param maximum inclusive upper bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value} is outside the range
     */
    public static @Nullable Long requireBetweenIfPresent(
            @Nullable final Long value, final long minimum, final long maximum, final String fieldName) {
        if (value != null) {
            requireBetween(value, minimum, maximum, fieldName);
        }
        return value;
    }

    /**
     * Requires {@code value} to be finite and within {@code minimum} and {@code maximum}, inclusive, when it is
     * non-null.
     *
     * <p>{@code null} is allowed and returned unchanged. A present value is checked with {@link #requireBetween(double,
     * double, double, String)}.
     *
     * @param value value to check, or {@code null}
     * @param minimum inclusive lower bound
     * @param maximum inclusive upper bound
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value} is non-finite or outside the range
     */
    public static @Nullable Double requireBetweenIfPresent(
            @Nullable final Double value, final double minimum, final double maximum, final String fieldName) {
        if (value != null) {
            requireBetween(value, minimum, maximum, fieldName);
        }
        return value;
    }

    /**
     * Requires {@code values} to be non-null and non-empty.
     *
     * @param values collection to check
     * @param fieldName name of the checked value, used in the exception message
     * @param <T> collection type
     * @return {@code values}
     * @throws NullPointerException if {@code values} is {@code null}
     * @throws IllegalArgumentException if {@code values} is empty
     */
    public static <T extends Collection<?>> T requireNonEmpty(final T values, final String fieldName) {
        requireNonNull(values, fieldName);

        if (values.isEmpty()) {
            throw invalid(fieldName, MUST_NOT_BE_EMPTY);
        }
        return values;
    }

    /**
     * Requires {@code map} to be non-null and non-empty.
     *
     * @param map map to check
     * @param fieldName name of the checked value, used in the exception message
     * @param <K> key type
     * @param <V> value type
     * @return {@code map}
     * @throws NullPointerException if {@code map} is {@code null}
     * @throws IllegalArgumentException if {@code map} is empty
     */
    public static <K, V> Map<K, V> requireNonEmpty(final Map<K, V> map, final String fieldName) {
        requireNonNull(map, fieldName);

        if (map.isEmpty()) {
            throw invalid(fieldName, MUST_NOT_BE_EMPTY);
        }
        return map;
    }

    /**
     * Requires {@code values} to be non-null and non-empty.
     *
     * @param values array to check
     * @param fieldName name of the checked value, used in the exception message
     * @param <T> element type
     * @return {@code values}
     * @throws NullPointerException if {@code values} is {@code null}
     * @throws IllegalArgumentException if {@code values} has length zero
     */
    public static <T> T[] requireNonEmpty(final T[] values, final String fieldName) {
        requireNonNull(values, fieldName);

        if (values.length == 0) {
            throw invalid(fieldName, MUST_NOT_BE_EMPTY);
        }
        return values;
    }

    /**
     * Requires {@code values} to contain at most {@code max} elements.
     *
     * @param values collection to check
     * @param max inclusive maximum size
     * @param fieldName name of the checked value, used in the exception message
     * @param <T> collection type
     * @return {@code values}
     * @throws NullPointerException if {@code values} is {@code null}
     * @throws IllegalArgumentException if {@code values} has more than {@code max} elements
     */
    public static <T extends Collection<?>> T requireSizeAtMost(final T values, final int max, final String fieldName) {
        requireNonNull(values, fieldName);

        if (values.size() > max) {
            throw invalid(fieldName, "must have at most " + max + ELEMENTS);
        }
        return values;
    }

    /**
     * Requires {@code map} to contain at most {@code max} entries.
     *
     * @param map map to check
     * @param max inclusive maximum size
     * @param fieldName name of the checked value, used in the exception message
     * @param <K> key type
     * @param <V> value type
     * @return {@code map}
     * @throws NullPointerException if {@code map} is {@code null}
     * @throws IllegalArgumentException if {@code map} has more than {@code max} entries
     */
    public static <K, V> Map<K, V> requireSizeAtMost(final Map<K, V> map, final int max, final String fieldName) {
        requireNonNull(map, fieldName);

        if (map.size() > max) {
            throw invalid(fieldName, "must have at most " + max + ENTRIES);
        }
        return map;
    }

    /**
     * Requires {@code values} to contain at least {@code min} elements.
     *
     * @param values collection to check
     * @param min inclusive minimum size
     * @param fieldName name of the checked value, used in the exception message
     * @param <T> collection type
     * @return {@code values}
     * @throws NullPointerException if {@code values} is {@code null}
     * @throws IllegalArgumentException if {@code values} has fewer than {@code min} elements
     */
    public static <T extends Collection<?>> T requireSizeAtLeast(
            final T values, final int min, final String fieldName) {
        requireNonNull(values, fieldName);

        if (values.size() < min) {
            throw invalid(fieldName, "must have at least " + min + ELEMENTS);
        }
        return values;
    }

    /**
     * Requires {@code map} to contain at least {@code min} entries.
     *
     * @param map map to check
     * @param min inclusive minimum size
     * @param fieldName name of the checked value, used in the exception message
     * @param <K> key type
     * @param <V> value type
     * @return {@code map}
     * @throws NullPointerException if {@code map} is {@code null}
     * @throws IllegalArgumentException if {@code map} has fewer than {@code min} entries
     */
    public static <K, V> Map<K, V> requireSizeAtLeast(final Map<K, V> map, final int min, final String fieldName) {
        requireNonNull(map, fieldName);

        if (map.size() < min) {
            throw invalid(fieldName, "must have at least " + min + ENTRIES);
        }
        return map;
    }

    /**
     * Requires {@code values} to contain exactly {@code size} elements.
     *
     * @param values collection to check
     * @param size required size
     * @param fieldName name of the checked value, used in the exception message
     * @param <T> collection type
     * @return {@code values}
     * @throws NullPointerException if {@code values} is {@code null}
     * @throws IllegalArgumentException if {@code values} does not have {@code size} elements
     */
    public static <T extends Collection<?>> T requireSizeExactly(
            final T values, final int size, final String fieldName) {
        requireNonNull(values, fieldName);

        if (values.size() != size) {
            throw invalid(fieldName, "must have exactly " + size + ELEMENTS);
        }
        return values;
    }

    /**
     * Requires {@code map} to contain exactly {@code size} entries.
     *
     * @param map map to check
     * @param size required size
     * @param fieldName name of the checked value, used in the exception message
     * @param <K> key type
     * @param <V> value type
     * @return {@code map}
     * @throws NullPointerException if {@code map} is {@code null}
     * @throws IllegalArgumentException if {@code map} does not have {@code size} entries
     */
    public static <K, V> Map<K, V> requireSizeExactly(final Map<K, V> map, final int size, final String fieldName) {
        requireNonNull(map, fieldName);

        if (map.size() != size) {
            throw invalid(fieldName, "must have exactly " + size + ENTRIES);
        }
        return map;
    }

    /**
     * Requires {@code values} to contain between {@code min} and {@code max} elements, inclusive.
     *
     * @param values collection to check
     * @param min inclusive minimum size
     * @param max inclusive maximum size
     * @param fieldName name of the checked value, used in the exception message
     * @param <T> collection type
     * @return {@code values}
     * @throws NullPointerException if {@code values} is {@code null}
     * @throws IllegalArgumentException if {@code min} is above {@code max}, or size is outside {@code min}..{@code max}
     */
    public static <T extends Collection<?>> T requireSizeBetween(
            final T values, final int min, final int max, final String fieldName) {
        requireNonNull(values, fieldName);
        if (min > max) {
            throw invertedBounds(fieldName, min, max);
        }

        if (values.size() < min || values.size() > max) {
            throw invalid(fieldName, "must have " + between(min, max) + ELEMENTS);
        }
        return values;
    }

    /**
     * Requires {@code map} to contain between {@code min} and {@code max} entries, inclusive.
     *
     * @param map map to check
     * @param min inclusive minimum size
     * @param max inclusive maximum size
     * @param fieldName name of the checked value, used in the exception message
     * @param <K> key type
     * @param <V> value type
     * @return {@code map}
     * @throws NullPointerException if {@code map} is {@code null}
     * @throws IllegalArgumentException if {@code min} is above {@code max}, or size is outside {@code min}..{@code max}
     */
    public static <K, V> Map<K, V> requireSizeBetween(
            final Map<K, V> map, final int min, final int max, final String fieldName) {
        requireNonNull(map, fieldName);
        if (min > max) {
            throw invertedBounds(fieldName, min, max);
        }

        if (map.size() < min || map.size() > max) {
            throw invalid(fieldName, "must have " + between(min, max) + ENTRIES);
        }
        return map;
    }

    /**
     * Requires {@code values} to contain no duplicate elements.
     *
     * <p>Uniqueness follows {@link Object#equals(Object)} and {@link Object#hashCode()} via a {@link HashSet}. Element
     * order is not changed. Two {@code null} elements count as duplicates.
     *
     * @param values collection to check
     * @param fieldName name of the checked value, used in the exception message
     * @param <T> collection type
     * @return {@code values}
     * @throws NullPointerException if {@code values} is {@code null}
     * @throws IllegalArgumentException if {@code values} contains duplicates
     */
    public static <T extends Collection<?>> T requireUniqueItems(final T values, final String fieldName) {
        requireNonNull(values, fieldName);

        if (values.size() > 1 && values.size() != new HashSet<>(values).size()) {
            throw invalid(fieldName, "must not contain duplicates");
        }
        return values;
    }

    /**
     * Requires {@code value} to have length at most {@code max}.
     *
     * <p>Length is {@link String#length()} (UTF-16 code units), not Unicode code points. The maximum is inclusive.
     *
     * @param value string to check
     * @param max inclusive maximum length
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is longer than {@code max}
     */
    public static String requireMaxLength(final String value, final int max, final String fieldName) {
        requireNonNull(value, fieldName);

        if (value.length() > max) {
            throw invalid(fieldName, "must have length at most " + max);
        }
        return value;
    }

    /**
     * Requires {@code value} to have length at most {@code max} when it is non-null.
     *
     * <p>{@code null} is allowed and returned unchanged. A present value is checked with
     * {@link #requireMaxLength(String, int, String)}.
     *
     * @param value string to check, or {@code null}
     * @param max inclusive maximum length
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value} is longer than {@code max}
     */
    public static @Nullable String requireMaxLengthIfPresent(
            @Nullable final String value, final int max, final String fieldName) {
        if (value != null) {
            requireMaxLength(value, max, fieldName);
        }
        return value;
    }

    /**
     * Requires {@code value} to have length at least {@code min}.
     *
     * <p>Length is {@link String#length()} (UTF-16 code units), not Unicode code points. The minimum is inclusive.
     *
     * @param value string to check
     * @param min inclusive minimum length
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is shorter than {@code min}
     */
    public static String requireMinLength(final String value, final int min, final String fieldName) {
        requireNonNull(value, fieldName);

        if (value.length() < min) {
            throw invalid(fieldName, "must have length at least " + min);
        }
        return value;
    }

    /**
     * Requires {@code value} to have length at least {@code min} when it is non-null.
     *
     * <p>{@code null} is allowed and returned unchanged. A present value is checked with
     * {@link #requireMinLength(String, int, String)}.
     *
     * @param value string to check, or {@code null}
     * @param min inclusive minimum length
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value} is shorter than {@code min}
     */
    public static @Nullable String requireMinLengthIfPresent(
            @Nullable final String value, final int min, final String fieldName) {
        if (value != null) {
            requireMinLength(value, min, fieldName);
        }
        return value;
    }

    /**
     * Requires {@code value} to match {@code pattern}.
     *
     * <p>Matching uses {@link java.util.regex.Matcher#find()}, so a substring match succeeds; the pattern does not have
     * to cover the entire string unless it is anchored. Use {@code ^} and {@code $} when the whole value must match.
     *
     * @param value string to check
     * @param pattern regex applied with {@link java.util.regex.Matcher#find()}
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws NullPointerException if {@code value} or {@code pattern} is {@code null}
     * @throws IllegalArgumentException if {@code value} does not match {@code pattern}
     */
    public static String requireMatches(final String value, final Pattern pattern, final String fieldName) {
        requireNonNull(value, fieldName);
        requireNonNull(pattern, "pattern");

        if (!pattern.matcher(value).find()) {
            throw invalid(fieldName, "must match the required pattern");
        }
        return value;
    }

    /**
     * Requires the whole of {@code value} to match {@code pattern}.
     *
     * <p>Matching uses {@link java.util.regex.Matcher#matches()}, so the pattern must cover the entire string.
     *
     * @param value string to check
     * @param pattern regex applied with {@link java.util.regex.Matcher#matches()}
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws NullPointerException if {@code value} or {@code pattern} is {@code null}
     * @throws IllegalArgumentException if {@code value} does not match {@code pattern} as a whole
     */
    public static String requireFullMatch(final String value, final Pattern pattern, final String fieldName) {
        requireNonNull(value, fieldName);
        requireNonNull(pattern, "pattern");

        if (!pattern.matcher(value).matches()) {
            throw invalid(fieldName, "must match the required pattern");
        }
        return value;
    }

    /**
     * Requires the whole of {@code value} to match {@code pattern} when it is non-null.
     *
     * <p>{@code null} is allowed and returned unchanged. A present value is checked with
     * {@link #requireFullMatch(String, Pattern, String)}.
     *
     * @param value string to check, or {@code null}
     * @param pattern regex applied with {@link java.util.regex.Matcher#matches()}
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws NullPointerException if {@code value} is present and {@code pattern} is {@code null}
     * @throws IllegalArgumentException if {@code value} does not match {@code pattern} as a whole
     */
    public static @Nullable String requireFullMatchIfPresent(
            @Nullable final String value, final Pattern pattern, final String fieldName) {
        if (value != null) {
            requireFullMatch(value, pattern, fieldName);
        }
        return value;
    }

    /**
     * Requires {@code value} to match {@code pattern} when it is non-null.
     *
     * <p>{@code null} is allowed and returned unchanged. A present value is checked with {@link #requireMatches(String,
     * Pattern, String)}.
     *
     * @param value string to check, or {@code null}
     * @param pattern regex applied with {@link java.util.regex.Matcher#find()}
     * @param fieldName name of the checked value, used in the exception message
     * @return {@code value}
     * @throws NullPointerException if {@code value} is present and {@code pattern} is {@code null}
     * @throws IllegalArgumentException if {@code value} does not match {@code pattern}
     */
    public static @Nullable String requireMatchesIfPresent(
            @Nullable final String value, final Pattern pattern, final String fieldName) {
        if (value != null) {
            requireMatches(value, pattern, fieldName);
        }
        return value;
    }

    /**
     * Requires exactly one of two values to be present.
     *
     * <p>A value is present when it is non-null.
     *
     * @param first first value, or {@code null}
     * @param second second value, or {@code null}
     * @param fieldName name of the checked group, used in the exception message
     * @throws IllegalArgumentException if none or both values are present
     */
    public static void requireExactlyOne(
            @Nullable final Object first, @Nullable final Object second, final String fieldName) {
        if (countPresent(first, second) != ONE_PRESENT) {
            throw invalid(fieldName, "must have exactly one value present");
        }
    }

    /**
     * Requires exactly one of three values to be present.
     *
     * <p>A value is present when it is non-null.
     *
     * @param first first value, or {@code null}
     * @param second second value, or {@code null}
     * @param third third value, or {@code null}
     * @param fieldName name of the checked group, used in the exception message
     * @throws IllegalArgumentException if none or more than one value is present
     */
    public static void requireExactlyOne(
            @Nullable final Object first,
            @Nullable final Object second,
            @Nullable final Object third,
            final String fieldName) {
        if (countPresent(first, second, third) != ONE_PRESENT) {
            throw invalid(fieldName, "must have exactly one value present");
        }
    }

    /**
     * Requires at most one of two values to be present.
     *
     * <p>A value is present when it is non-null.
     *
     * @param first first value, or {@code null}
     * @param second second value, or {@code null}
     * @param fieldName name of the checked group, used in the exception message
     * @throws IllegalArgumentException if both values are present
     */
    public static void requireAtMostOne(
            @Nullable final Object first, @Nullable final Object second, final String fieldName) {
        if (countPresent(first, second) > ONE_PRESENT) {
            throw invalid(fieldName, "must have at most one value present");
        }
    }

    /**
     * Requires at most one of three values to be present.
     *
     * <p>A value is present when it is non-null.
     *
     * @param first first value, or {@code null}
     * @param second second value, or {@code null}
     * @param third third value, or {@code null}
     * @param fieldName name of the checked group, used in the exception message
     * @throws IllegalArgumentException if more than one value is present
     */
    public static void requireAtMostOne(
            @Nullable final Object first,
            @Nullable final Object second,
            @Nullable final Object third,
            final String fieldName) {
        if (countPresent(first, second, third) > ONE_PRESENT) {
            throw invalid(fieldName, "must have at most one value present");
        }
    }

    /**
     * Requires at least one of two values to be present.
     *
     * <p>A value is present when it is non-null.
     *
     * @param first first value, or {@code null}
     * @param second second value, or {@code null}
     * @param fieldName name of the checked group, used in the exception message
     * @throws IllegalArgumentException if neither value is present
     */
    public static void requireAtLeastOne(
            @Nullable final Object first, @Nullable final Object second, final String fieldName) {
        if (countPresent(first, second) == 0) {
            throw invalid(fieldName, "must have at least one value present");
        }
    }

    /**
     * Requires at least one of three values to be present.
     *
     * <p>A value is present when it is non-null.
     *
     * @param first first value, or {@code null}
     * @param second second value, or {@code null}
     * @param third third value, or {@code null}
     * @param fieldName name of the checked group, used in the exception message
     * @throws IllegalArgumentException if no value is present
     */
    public static void requireAtLeastOne(
            @Nullable final Object first,
            @Nullable final Object second,
            @Nullable final Object third,
            final String fieldName) {
        if (countPresent(first, second, third) == 0) {
            throw invalid(fieldName, "must have at least one value present");
        }
    }

    /**
     * Requires two values to be either both present or both absent.
     *
     * <p>A value is present when it is non-null.
     *
     * @param first first value, or {@code null}
     * @param second second value, or {@code null}
     * @param fieldName name of the checked group, used in the exception message
     * @throws IllegalArgumentException if exactly one value is present
     */
    public static void requireBothOrNeither(
            @Nullable final Object first, @Nullable final Object second, final String fieldName) {
        if (countPresent(first, second) == ONE_PRESENT) {
            throw invalid(fieldName, "must have both values present or both absent");
        }
    }

    /**
     * Returns {@code true} if {@code duration} is {@code null}, negative, or zero.
     *
     * @param duration duration to inspect, or {@code null}
     * @return {@code true} if {@code duration} is missing or not positive
     */
    public static boolean isNonPositive(@Nullable final Duration duration) {
        return duration == null || duration.isNegative() || duration.isZero();
    }

    /**
     * Returns {@code true} if {@code duration} is non-null and strictly positive.
     *
     * @param duration duration to inspect, or {@code null}
     * @return {@code true} if {@code duration} is positive
     */
    public static boolean isPositive(@Nullable final Duration duration) {
        return duration != null && !duration.isNegative() && !duration.isZero();
    }

    private static String between(final Object minimum, final Object maximum) {
        return "between " + minimum + " and " + maximum;
    }

    private static void requireNotNaN(final double bound, final String name, final String fieldName) {
        if (Double.isNaN(bound)) {
            throw invalid(fieldName, "has a NaN " + name);
        }
    }

    private static IllegalArgumentException invertedBounds(
            final String fieldName, final Object minimum, final Object maximum) {
        return invalid(
                fieldName, "has bounds in the wrong order: minimum " + minimum + " is greater than maximum " + maximum);
    }

    private static IllegalArgumentException invalid(final String fieldName, final String rule) {
        return new IllegalArgumentException(fieldName + " " + rule);
    }

    private static int countPresent(@Nullable final Object... values) {
        int present = 0;
        for (final Object value : values) {
            if (value != null) {
                present++;
            }
        }
        return present;
    }
}

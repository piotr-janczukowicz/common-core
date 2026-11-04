package io.github.piotr_janczukowicz.util;

import org.jspecify.annotations.Nullable;

/**
 * JDK-only string helpers with the same semantics as the blank checks of Apache Commons Lang 3.
 *
 * <p>Whitespace means {@link Character#isWhitespace(int)}: tab, line feed, carriage return, vertical tab, form feed and
 * Unicode space separators such as U+2003 are whitespace. No-break spaces (U+00A0, U+2007, U+202F) are not, so they are
 * neither blank nor stripped. {@link String#trim()} is deliberately not used because it only removes characters up to
 * U+0020.
 */
public final class StringUtils {

    private StringUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Tells whether {@code value} is {@code null}, empty, or made only of whitespace.
     *
     * <p>A {@link String} is checked with {@link String#isBlank()}; any other {@link CharSequence} is checked code
     * point by code point with {@link Character#isWhitespace(int)}.
     *
     * @param value text to check, may be {@code null}
     * @return {@code true} if {@code value} is {@code null} or contains only whitespace
     */
    public static boolean isBlank(@Nullable final CharSequence value) {
        final boolean blank;
        if (value == null) {
            blank = true;
        } else if (value instanceof final String string) {
            blank = string.isBlank();
        } else {
            blank = value.codePoints().allMatch(Character::isWhitespace);
        }
        return blank;
    }

    /**
     * Tells whether {@code value} contains at least one non-whitespace character.
     *
     * @param value text to check, may be {@code null}
     * @return {@code true} if {@code value} is not {@code null} and not blank
     * @see #isBlank(CharSequence)
     */
    public static boolean isNotBlank(@Nullable final CharSequence value) {
        return !isBlank(value);
    }

    /**
     * Removes leading and trailing whitespace and maps the result to {@code null} when nothing remains.
     *
     * <p>Whitespace is defined by {@link String#strip()}, not by {@link String#trim()}.
     *
     * @param value text to strip, may be {@code null}
     * @return the stripped text, or {@code null} if {@code value} is {@code null} or empty after stripping
     */
    public static @Nullable String stripToNull(@Nullable final String value) {
        final String stripped = value == null ? "" : value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}

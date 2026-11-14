package io.github.piotr_janczukowicz.util;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requirePositive;

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

    /**
     * Replaces every ISO control character of {@code text} with {@code replacement}, so that text from a server cannot
     * forge a log line or inject an escape sequence when it reaches a log or an exception message.
     *
     * <p>An ISO control character is one for which {@link Character#isISOControl(char)} is {@code true}: the C0
     * controls (such as line feed, carriage return and escape), the delete character and the C1 controls. Every other
     * character, including other whitespace, is kept. Each {@code char} is checked on its own, so a surrogate pair
     * passes through unchanged.
     *
     * @param text text to clean
     * @param replacement character that takes the place of each control character
     * @return {@code text} itself if it holds no control character, otherwise a copy with each one replaced
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static String replaceControlCharacters(final String text, final char replacement) {
        requireNonNull(text, "text");
        final int length = text.length();
        int index = 0;
        while (index < length && !Character.isISOControl(text.charAt(index))) {
            index++;
        }
        if (index == length) {
            return text;
        }
        final char[] cleaned = text.toCharArray();
        for (; index < length; index++) {
            if (Character.isISOControl(cleaned[index])) {
                cleaned[index] = replacement;
            }
        }
        return new String(cleaned);
    }

    /**
     * Cuts {@code text} to at most {@code maxChars} characters, never inside a surrogate pair: when the cut would fall
     * between the two halves of a pair, the whole pair is dropped.
     *
     * @param text text to cut
     * @param maxChars the most characters to keep, positive
     * @return {@code text} itself if it fits, otherwise its first characters, which may be one fewer than
     *     {@code maxChars}
     * @throws NullPointerException if {@code text} is {@code null}
     * @throws IllegalArgumentException if {@code maxChars} is not positive
     */
    public static String limited(final String text, final int maxChars) {
        requireNonNull(text, "text");
        requirePositive(maxChars, "maxChars");
        if (text.length() <= maxChars) {
            return text;
        }
        final int end = Character.isHighSurrogate(text.charAt(maxChars - 1)) ? maxChars - 1 : maxChars;
        return text.substring(0, end);
    }
}

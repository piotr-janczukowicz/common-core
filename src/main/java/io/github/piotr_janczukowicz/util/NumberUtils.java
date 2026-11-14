package io.github.piotr_janczukowicz.util;

import java.util.OptionalInt;
import java.util.OptionalLong;
import org.jspecify.annotations.Nullable;

/**
 * Lenient number parsing for text that comes from outside, such as a response header.
 *
 * <p>Surrounding whitespace is ignored. A number is an optional {@code -} followed by ASCII digits: a leading {@code +}
 * and digits of other scripts are refused. Text that is absent, blank, or not such a number gives an empty result
 * instead of an exception, so a caller can treat a bad header as a missing one.
 */
public final class NumberUtils {

    private NumberUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Parses {@code value} as a decimal {@code int}, after {@link String#strip() stripping} it.
     *
     * @param value text to parse, or {@code null}
     * @return the number, or empty if {@code value} is {@code null}, blank, not a whole ASCII decimal number, or out of
     *     range
     */
    public static OptionalInt parseInt(@Nullable final String value) {
        final String text = asciiDecimalOrNull(value);
        if (text == null) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(text));
        } catch (final NumberFormatException notAnInt) {
            return OptionalInt.empty();
        }
    }

    /**
     * Parses {@code value} as a decimal {@code long}, after {@link String#strip() stripping} it.
     *
     * @param value text to parse, or {@code null}
     * @return the number, or empty if {@code value} is {@code null}, blank, not a whole ASCII decimal number, or out of
     *     range
     */
    public static OptionalLong parseLong(@Nullable final String value) {
        final String text = asciiDecimalOrNull(value);
        if (text == null) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(Long.parseLong(text));
        } catch (final NumberFormatException notALong) {
            return OptionalLong.empty();
        }
    }

    private static @Nullable String asciiDecimalOrNull(@Nullable final String value) {
        if (value == null) {
            return null;
        }
        final String text = value.strip();
        final int start = text.startsWith("-") ? 1 : 0;
        if (text.length() == start) {
            return null;
        }
        for (int index = start; index < text.length(); index++) {
            final char character = text.charAt(index);
            if (character < '0' || character > '9') {
                return null;
            }
        }
        return text;
    }
}

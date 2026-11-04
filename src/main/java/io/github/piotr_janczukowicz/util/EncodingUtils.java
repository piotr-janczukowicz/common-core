package io.github.piotr_janczukowicz.util;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import java.util.Base64;

/**
 * Encoding and decoding helpers.
 *
 * <p>Failures are reported with a caller-supplied message and never chain the underlying decoder exception, because its
 * text can echo part of the input.
 */
public final class EncodingUtils {

    private EncodingUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Decodes {@code value} with the standard {@link Base64} decoder.
     *
     * <p>The decoder exception is deliberately not attached as the cause: its text can echo a character of the input,
     * which may be a secret.
     *
     * @param value Base64 text to decode
     * @param message exception text
     * @return the decoded bytes
     * @throws NullPointerException if {@code value} or {@code message} is {@code null}
     * @throws IllegalArgumentException if {@code message} is blank, or {@code value} is not valid Base64
     */
    @SuppressWarnings("PMD.AvoidThrowingNewInstanceOfSameException")
    public static byte[] decodeBase64(final String value, final String message) {
        requireNonNull(value, "value");
        requireNotBlank(message, "message");

        try {
            return Base64.getDecoder().decode(value);
        } catch (final IllegalArgumentException ignored) {
            throw new IllegalArgumentException(message);
        }
    }
}

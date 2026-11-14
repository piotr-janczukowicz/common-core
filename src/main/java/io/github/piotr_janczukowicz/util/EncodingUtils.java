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

    /**
     * Encodes {@code bytes} with the standard {@link Base64} encoder, with padding and without line breaks.
     *
     * @param bytes bytes to encode
     * @return the Base64 text
     * @throws NullPointerException if {@code bytes} is {@code null}
     */
    public static String encodeBase64(final byte[] bytes) {
        requireNonNull(bytes, "bytes");

        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Decodes Base64 {@code value} after removing its line breaks, such as the wrapped text GitHub returns.
     *
     * <p>Only line feeds and carriage returns are removed; any other character that is not Base64 fails as in
     * {@link #decodeBase64(String, String)}, which this method calls.
     *
     * @param value Base64 text, which may be wrapped over several lines
     * @param message exception text
     * @return the decoded bytes
     * @throws NullPointerException if {@code value} or {@code message} is {@code null}
     * @throws IllegalArgumentException if {@code message} is blank, or {@code value} is not valid Base64
     */
    public static byte[] decodeWrappedBase64(final String value, final String message) {
        requireNonNull(value, "value");

        return decodeBase64(value.replace("\n", "").replace("\r", ""), message);
    }
}

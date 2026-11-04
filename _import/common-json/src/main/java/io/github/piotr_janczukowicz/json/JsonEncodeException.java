package io.github.piotr_janczukowicz.json;

import java.io.Serial;

/**
 * A value that could not be written as JSON, for example because of a cyclic structure or a type the codec cannot
 * serialize.
 *
 * <p>Unchecked, like the rest of {@link JsonCodec}, so callers can catch this one type for every encode failure. The
 * underlying codec exception is the cause.
 */
public class JsonEncodeException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception for an unserializable value.
     *
     * @param message what could not be encoded
     * @param cause codec exception
     */
    public JsonEncodeException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

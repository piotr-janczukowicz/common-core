package io.github.piotr_janczukowicz.json;

import java.io.Serial;

/**
 * A JSON payload that could not be decoded: malformed JSON, or JSON that does not fit the target type.
 *
 * <p>Unchecked, like the rest of {@link JsonCodec}, so callers can catch this one type for every decode failure. The
 * underlying codec exception is the cause. A payload that is the JSON literal {@code null} is reported as an
 * {@link IllegalStateException} instead, because the JSON is valid and only has no value to return.
 *
 * <p>The message never quotes values from the payload, but it may name the field path, which can include field or map
 * key names taken from the payload. The cause may quote more: the codec's own exception can contain the offending
 * value. Do not log the cause chain of a payload that carries secrets.
 */
public class JsonDecodeException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception for an undecodable payload.
     *
     * @param message what could not be decoded, without the payload text
     * @param cause codec exception
     */
    public JsonDecodeException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

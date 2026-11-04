package io.github.piotr_janczukowicz.json;

/**
 * Reads and writes JSON as UTF-8 bytes.
 *
 * <p>This interface is intentionally open (not sealed) so that other projects can implement it, with the conventions of
 * the service they talk to.
 *
 * <p>The {@link Class} decode overload reads a raw type. The {@link JsonType} overload preserves generic type
 * arguments. Invalid JSON or a type mismatch fails with a {@link JsonDecodeException}; a body that is the JSON literal
 * {@code null} fails with an {@link IllegalStateException}. A decode never returns {@code null}.
 * {@link #encode(Object)} writes a value as JSON and fails with a {@link JsonEncodeException} when it cannot.
 */
public interface JsonCodec {

    /**
     * Decodes {@code json} as the raw type {@code type}.
     *
     * @param json JSON payload
     * @param type target class
     * @param <T> result type
     * @return value of {@code type}
     * @throws NullPointerException if {@code json} or {@code type} is {@code null}
     * @throws JsonDecodeException if {@code json} is malformed or does not fit {@code type}
     * @throws IllegalStateException if the JSON body is {@code null}, because a decode never returns {@code null}
     */
    <T> T decode(byte[] json, Class<T> type);

    /**
     * Decodes {@code json} as {@code type}, including generic type arguments.
     *
     * @param json JSON payload
     * @param type target type, including type arguments
     * @param <T> result type
     * @return value of {@code type}
     * @throws NullPointerException if {@code json} or {@code type} is {@code null}
     * @throws JsonDecodeException if {@code json} is malformed or does not fit {@code type}
     * @throws IllegalStateException if the JSON body is {@code null}, because a decode never returns {@code null}
     */
    <T> T decode(byte[] json, JsonType<T> type);

    /**
     * Encodes {@code value} as UTF-8 JSON bytes.
     *
     * @param value value to write
     * @return UTF-8 JSON bytes
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws JsonEncodeException if {@code value} cannot be written as JSON
     */
    byte[] encode(Object value);
}

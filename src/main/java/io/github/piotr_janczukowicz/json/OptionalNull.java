package io.github.piotr_janczukowicz.json;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Three-state JSON field: omit when the wrapper is {@code null}, write JSON {@code null} when {@link #ofNull()}, or
 * write the wrapped value when {@link #of(Object)}.
 *
 * @param <T> value type
 */
public final class OptionalNull<T> {

    private final @Nullable T value;

    private OptionalNull(final @Nullable T value) {
        this.value = value;
    }

    /**
     * JSON value to write, including {@code null}.
     *
     * @return wrapped value
     */
    public @Nullable T value() {
        return value;
    }

    /**
     * Present non-null value.
     *
     * @param value value to write
     * @param <T> value type
     * @return wrapper
     * @throws NullPointerException if {@code value} is {@code null} (use {@link #ofNull()} for JSON null)
     */
    public static <T> OptionalNull<T> of(final T value) {
        return new OptionalNull<>(requireNonNull(value, "value"));
    }

    /**
     * Explicit JSON {@code null}.
     *
     * @param <T> value type
     * @return wrapper
     */
    public static <T> OptionalNull<T> ofNull() {
        return new OptionalNull<>(null);
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        return this == other || other instanceof final OptionalNull<?> wrapper && Objects.equals(value, wrapper.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    /**
     * Describes the wrapper by its value; {@code OptionalNull[null]} is the explicit JSON {@code null}.
     *
     * @return text with the wrapped value
     */
    @Override
    public String toString() {
        return "OptionalNull[" + value + "]";
    }
}

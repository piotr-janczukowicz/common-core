package io.github.piotr_janczukowicz.json;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.lang.reflect.Type;
import org.jspecify.annotations.Nullable;

/**
 * Library-owned decode target for generic JSON types.
 *
 * @param <T> Java type to decode
 */
@SuppressWarnings("java:S2326") // phantom type parameter: carries the decode result type for callers
public final class JsonType<T> {

    private final Type javaType;

    JsonType(final Type javaType) {
        this.javaType = requireNonNull(javaType, "javaType");
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        return this == other || other instanceof final JsonType<?> that && javaType.equals(that.javaType);
    }

    @Override
    public int hashCode() {
        return javaType.hashCode();
    }

    @Override
    public String toString() {
        return "JsonType(" + javaType.getTypeName() + ")";
    }

    /**
     * Java type to decode into.
     *
     * @return type, including type arguments
     */
    public Type javaType() {
        return javaType;
    }
}

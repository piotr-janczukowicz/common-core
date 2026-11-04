package io.github.piotr_janczukowicz.json.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.piotr_janczukowicz.json.OptionalNull;
import org.jspecify.annotations.Nullable;

/**
 * Jackson mixin for {@link OptionalNull} so the Jackson-free {@code json} package carries no Jackson annotations.
 *
 * <p>{@code ALWAYS} keeps an explicit {@link OptionalNull#ofNull()} written as JSON {@code null}; a Java {@code null}
 * wrapper is still omitted by the enclosing type's {@code NON_NULL} inclusion. Pair with
 * {@link OptionalNullDeserializer}.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public interface OptionalNullMixin {

    /**
     * JSON value to write, including {@code null}.
     *
     * @return wrapped value
     */
    @JsonValue
    @Nullable Object value();
}

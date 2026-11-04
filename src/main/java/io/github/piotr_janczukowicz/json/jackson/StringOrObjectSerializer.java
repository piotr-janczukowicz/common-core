package io.github.piotr_janczukowicz.json.jackson;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/** Writes the scalar text of a string-or-object union when there is one, and otherwise its details object. */
final class StringOrObjectSerializer<T, D> extends StdSerializer<T> {

    private final Function<T, @Nullable String> scalar;
    private final Function<T, @Nullable D> details;

    StringOrObjectSerializer(
            final Class<T> type, final Function<T, @Nullable String> scalar, final Function<T, @Nullable D> details) {
        super(type);
        this.scalar = scalar;
        this.details = details;
    }

    @Override
    public void serialize(final T value, final JsonGenerator generator, final SerializationContext context) {
        final String text = scalar.apply(value);
        if (text != null) {
            generator.writeString(text);
            return;
        }
        context.writeValue(generator, requireNonNull(details.apply(value), "details"));
    }
}

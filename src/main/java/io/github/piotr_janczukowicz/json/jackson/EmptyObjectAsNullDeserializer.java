package io.github.piotr_janczukowicz.json.jackson;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Reads a property marked {@link io.github.piotr_janczukowicz.json.EmptyObjectAsNull}: an object with no properties and
 * JSON {@code null} are {@code null}, any other value is read by the deserializer of the property's declared type.
 */
final class EmptyObjectAsNullDeserializer extends ValueDeserializer<Object> {

    private final @Nullable ValueDeserializer<Object> delegate;

    EmptyObjectAsNullDeserializer() {
        this(null);
    }

    private EmptyObjectAsNullDeserializer(final @Nullable ValueDeserializer<Object> delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public ValueDeserializer<?> createContextual(final DeserializationContext context, final BeanProperty property) {
        return new EmptyObjectAsNullDeserializer(context.findContextualValueDeserializer(property.getType(), property));
    }

    @Override
    public @Nullable Object deserialize(final JsonParser parser, final DeserializationContext context) {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            return delegate().deserialize(parser, context);
        }
        return BufferedJson.readUnlessEmptyObject(parser, context, delegate());
    }

    @Override
    public @Nullable Object getNullValue(final DeserializationContext context) {
        return null;
    }

    private ValueDeserializer<Object> delegate() {
        return Objects.requireNonNull(delegate, "The deserializer was used before it was made contextual");
    }
}

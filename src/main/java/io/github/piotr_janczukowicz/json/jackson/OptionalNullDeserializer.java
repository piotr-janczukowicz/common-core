package io.github.piotr_janczukowicz.json.jackson;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.json.OptionalNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;

/** Decodes {@link OptionalNull} from JSON null or a present value; absent properties stay Java {@code null}. */
public final class OptionalNullDeserializer extends StdDeserializer<OptionalNull<?>> {

    private final @Nullable ValueDeserializer<Object> valueDeserializer;

    /** Deserializer used before Jackson supplies the value type. */
    public OptionalNullDeserializer() {
        super(OptionalNull.class);
        this.valueDeserializer = null;
    }

    private OptionalNullDeserializer(final ValueDeserializer<Object> valueDeserializer) {
        super(OptionalNull.class);
        this.valueDeserializer = requireNonNull(valueDeserializer, "valueDeserializer");
    }

    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public ValueDeserializer<?> createContextual(
            final DeserializationContext context, final @Nullable BeanProperty property) {
        final JavaType wrapperType = property == null ? context.getContextualType() : property.getType();
        if (wrapperType == null) {
            throw DatabindException.from(context, "Cannot resolve the value type of an OptionalNull without a type");
        }
        final JavaType contentType = wrapperType.containedTypeOrUnknown(0);
        return new OptionalNullDeserializer(context.findContextualValueDeserializer(contentType, property));
    }

    @Override
    public OptionalNull<?> deserialize(final JsonParser parser, final DeserializationContext context) {
        if (valueDeserializer == null) {
            throw new IllegalStateException("OptionalNullDeserializer has no value type: it was not contextualized");
        }
        final Object value = valueDeserializer.deserialize(parser, context);
        // A value the inner deserializer reads as null, such as an enum constant this library does not know, must not
        // become an OptionalNull holding null: a PATCH would send that null and clear the field on the server.
        if (value == null) {
            throw DatabindException.from(parser, "Unknown value inside OptionalNull; refusing to read it as null");
        }
        return OptionalNull.of(value);
    }

    @Override
    public Object getNullValue(final DeserializationContext context) {
        return OptionalNull.ofNull();
    }

    @Override
    public @Nullable Object getAbsentValue(final DeserializationContext context) {
        return null;
    }
}

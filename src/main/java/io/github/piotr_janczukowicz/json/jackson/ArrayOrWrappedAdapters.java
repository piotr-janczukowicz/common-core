package io.github.piotr_janczukowicz.json.jackson;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Registers Jackson adapters for a named object property holding {@code string[]} versus a bare {@code string[]} value.
 */
public final class ArrayOrWrappedAdapters {

    private ArrayOrWrappedAdapters() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Registers a serializer and a deserializer for a value that is either a bare string array or an object holding the
     * array in one property.
     *
     * @param module module to add to
     * @param type union type
     * @param wrappedProperty name of the property that holds the array in the object form
     * @param arrayValue the list of a bare-array value, or {@code null} when the value is wrapped
     * @param wrappedValue the list of a wrapped value, or {@code null} when the value is a bare array
     * @param ofArray builds the union from a bare array
     * @param ofWrapped builds the union from the wrapped list
     * @param <T> union type
     */
    public static <T> void register(
            final SimpleModule module,
            final Class<T> type,
            final String wrappedProperty,
            final Function<T, @Nullable List<String>> arrayValue,
            final Function<T, @Nullable List<String>> wrappedValue,
            final Function<List<String>, T> ofArray,
            final Function<List<String>, T> ofWrapped) {
        requireNonNull(module, "module");
        requireNonNull(type, "type");
        requireNonNull(wrappedProperty, "wrappedProperty");
        requireNonNull(arrayValue, "arrayValue");
        requireNonNull(wrappedValue, "wrappedValue");
        requireNonNull(ofArray, "ofArray");
        requireNonNull(ofWrapped, "ofWrapped");
        module.addSerializer(type, new Serializer<>(type, wrappedProperty, arrayValue, wrappedValue));
        module.addDeserializer(type, new Deserializer<>(type, wrappedProperty, ofArray, ofWrapped));
    }

    private static final class Serializer<T> extends StdSerializer<T> {

        private final String wrappedProperty;
        private final Function<T, @Nullable List<String>> arrayValue;
        private final Function<T, @Nullable List<String>> wrappedValue;

        private Serializer(
                final Class<T> type,
                final String wrappedProperty,
                final Function<T, @Nullable List<String>> arrayValue,
                final Function<T, @Nullable List<String>> wrappedValue) {
            super(type);
            this.wrappedProperty = wrappedProperty;
            this.arrayValue = arrayValue;
            this.wrappedValue = wrappedValue;
        }

        @Override
        public void serialize(final T value, final JsonGenerator generator, final SerializationContext context) {
            final List<String> array = arrayValue.apply(value);
            if (array != null) {
                context.writeValue(generator, array);
                return;
            }
            context.writeValue(
                    generator, Map.of(wrappedProperty, requireNonNull(wrappedValue.apply(value), wrappedProperty)));
        }
    }

    private static final class Deserializer<T> extends StdDeserializer<T> {

        private final String wrappedProperty;
        private final Function<List<String>, T> ofArray;
        private final Function<List<String>, T> ofWrapped;

        private Deserializer(
                final Class<T> type,
                final String wrappedProperty,
                final Function<List<String>, T> ofArray,
                final Function<List<String>, T> ofWrapped) {
            super(type);
            this.wrappedProperty = wrappedProperty;
            this.ofArray = ofArray;
            this.ofWrapped = ofWrapped;
        }

        @Override
        @SuppressWarnings("PMD.LawOfDemeter")
        public T deserialize(final JsonParser parser, final DeserializationContext context) {
            final JavaType listType = context.getTypeFactory().constructCollectionType(List.class, String.class);
            if (parser.currentToken() == JsonToken.START_ARRAY) {
                return ofArray.apply(context.readValue(parser, listType));
            }
            try (BufferedJson json = BufferedJson.capture(parser, context)) {
                final List<String> values = json.readField(wrappedProperty, listType);
                if (values == null) {
                    throw json.mismatch("Expected a JSON array or an object with the field " + wrappedProperty);
                }
                return ofWrapped.apply(values);
            }
        }
    }
}

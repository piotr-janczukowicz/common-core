package io.github.piotr_janczukowicz.json;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.json.jackson.OptionalNullDeserializer;
import io.github.piotr_janczukowicz.json.jackson.OptionalNullMixin;
import java.util.function.Consumer;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * Jackson-backed {@link JsonCodec} for tests: snake_case names, unknown properties ignored, and {@link OptionalNull}
 * registered. {@link #with(Consumer)} adds the adapters a test needs.
 */
public final class JsonTestCodec implements JsonCodec {

    private static final JsonTestCodec DEFAULTS = with(module -> {});

    private final JsonMapper mapper;

    private JsonTestCodec(final JsonMapper mapper) {
        this.mapper = mapper;
    }

    public static JsonTestCodec defaults() {
        return DEFAULTS;
    }

    public static JsonTestCodec with(final Consumer<SimpleModule> customizer) {
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(OptionalNull.class, new OptionalNullDeserializer());
        module.setMixInAnnotation(OptionalNull.class, OptionalNullMixin.class);
        customizer.accept(module);
        return new JsonTestCodec(JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .addModule(module)
                .build());
    }

    @Override
    public <T> T decode(final byte[] json, final Class<T> type) {
        requireNonNull(json, "json");
        requireNonNull(type, "type");
        try {
            return requireValue(mapper.readValue(json, type), type.getName());
        } catch (final JacksonException e) {
            throw new JsonDecodeException("Cannot decode JSON as " + type.getName(), e);
        }
    }

    @Override
    public <T> T decode(final byte[] json, final JsonType<T> type) {
        requireNonNull(json, "json");
        requireNonNull(type, "type");
        try {
            final T value = mapper.readValue(json, mapper.constructType(type.javaType()));
            return requireValue(value, type.javaType().getTypeName());
        } catch (final JacksonException e) {
            throw new JsonDecodeException(
                    "Cannot decode JSON as " + type.javaType().getTypeName(), e);
        }
    }

    @Override
    public byte[] encode(final Object value) {
        requireNonNull(value, "value");
        try {
            return mapper.writeValueAsBytes(value);
        } catch (final JacksonException e) {
            throw new JsonEncodeException("Cannot encode " + value.getClass().getName(), e);
        }
    }

    private static <T> T requireValue(final T value, final String typeName) {
        if (value == null) {
            throw new IllegalStateException("JSON body is null, expected a value of type " + typeName);
        }
        return value;
    }
}

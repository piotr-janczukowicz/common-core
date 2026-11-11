package io.github.piotr_janczukowicz.json.jackson;

import io.github.piotr_janczukowicz.json.jackson.module.immutable.ImmutableCollectionsModule;
import java.time.Instant;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.type.LogicalType;

/** The Jackson settings that the JSON clients built on this library share. */
public final class JsonMappers {

    private JsonMappers() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * A mapper builder for APIs whose JSON uses {@code snake_case} names and that add properties over time. A caller
     * adds the modules of its own models and builds the mapper once: a {@link JsonMapper} is thread-safe and costly to
     * create.
     *
     * <p>The mapper reads {@code snake_case} names as the camelCase components of a record; ignores properties the type
     * does not have; refuses a duplicate key and a fractional number for an integer; reads an unknown enum constant as
     * {@code null}; reads a single value where an array is expected; converts a JSON number or boolean to a
     * {@link String}, and a JSON string to a collection of one element; decodes collections as immutable ones; and
     * reads {@link Instant} values with {@link FastInstantDeserializer}. Dates are written as text, not as timestamps.
     *
     * @return a new builder with these settings
     */
    public static JsonMapper.Builder snakeCase() {
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .withCoercionConfig(LogicalType.Textual, cfg -> {
                    cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.TryConvert);
                    cfg.setCoercion(CoercionInputShape.Float, CoercionAction.TryConvert);
                    cfg.setCoercion(CoercionInputShape.Boolean, CoercionAction.TryConvert);
                })
                .withCoercionConfig(
                        LogicalType.Collection,
                        cfg -> cfg.setCoercion(CoercionInputShape.String, CoercionAction.TryConvert))
                .addModule(new ImmutableCollectionsModule())
                .addModule(
                        new SimpleModule("FastInstant").addDeserializer(Instant.class, new FastInstantDeserializer()));
    }
}

package io.github.piotr_janczukowicz.json.jackson;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.json.jackson.module.immutable.ImmutableCollectionsModule;
import io.github.piotr_janczukowicz.json.jackson.module.optionalnull.OptionalNullModule;
import io.github.piotr_janczukowicz.json.jackson.module.valueunion.ValueUnionModule;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JacksonModule;
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
     * {@code null}; reads a value whose external type id property is missing as {@code null}, so a
     * sibling-discriminated union (Jackson's {@code EXTERNAL_PROPERTY}) accepts a body that has the discriminator but
     * not the value; reads a single value where an array is expected; converts a JSON number or boolean to a
     * {@link String}, and a JSON string to a collection of one element; decodes collections as immutable ones; reads
     * and writes {@link io.github.piotr_janczukowicz.json.OptionalNull} through {@link OptionalNullModule}; reads
     * {@link io.github.piotr_janczukowicz.json.ValueUnion} types through {@link ValueUnionModule}; reads and writes
     * {@link io.github.piotr_janczukowicz.json.EpochInstant} properties through it; and reads {@link Instant} values
     * with {@link FastInstantDeserializer}. Dates are written as text, not as timestamps.
     *
     * @return a new builder with these settings
     */
    public static JsonMapper.Builder snakeCase() {
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .disable(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY)
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
                .addModule(new OptionalNullModule())
                .addModule(new ValueUnionModule())
                .addModule(
                        new SimpleModule("FastInstant").addDeserializer(Instant.class, new FastInstantDeserializer()));
    }

    /**
     * The Jackson modules of {@code providers}, in order, for a client that finds the modules of its models with a
     * {@link java.util.ServiceLoader}: the caller keeps the {@code ServiceLoader.load} call, because the {@code uses}
     * clause of its module descriptor belongs to it, and passes the result to {@link JsonMapper.Builder#addModules}.
     *
     * @param providers the providers, such as a {@code ServiceLoader}
     * @param toModule reads the module of one provider
     * @param <P> type of a provider
     * @return an unmodifiable list with the module of each provider
     * @throws NullPointerException if an argument is {@code null}, or {@code toModule} returns {@code null}
     */
    public static <P> List<JacksonModule> modulesOf(
            final Iterable<? extends P> providers, final Function<? super P, ? extends JacksonModule> toModule) {
        requireNonNull(providers, "providers");
        requireNonNull(toModule, "toModule");
        final List<JacksonModule> modules = new ArrayList<>();
        for (final P provider : providers) {
            modules.add(requireNonNull(toModule.apply(provider), "module"));
        }
        return Collections.unmodifiableList(modules);
    }
}

package io.github.piotr_janczukowicz.json.jackson;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.module.SimpleModule;

/** Registers Jackson adapters for {@code oneOf} string-or-object values. */
public final class StringOrObjectAdapters {

    private StringOrObjectAdapters() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Registers a serializer that writes the scalar text when there is one and otherwise the details object, and a
     * deserializer that reads a JSON string as the scalar and anything else as the details.
     *
     * @param module module to add to
     * @param type union type
     * @param scalar text of a union value, or {@code null} when it holds details
     * @param details details of a union value, or {@code null} when it holds a scalar
     * @param ofScalar builds the union from text
     * @param ofDetails builds the union from details
     * @param detailsType type to read the details as
     * @param <T> union type
     * @param <D> details type
     */
    public static <T, D> void register(
            final SimpleModule module,
            final Class<T> type,
            final Function<T, @Nullable String> scalar,
            final Function<T, @Nullable D> details,
            final Function<String, T> ofScalar,
            final Function<D, T> ofDetails,
            final Class<D> detailsType) {
        register(
                module,
                type,
                scalar,
                details,
                ofScalar,
                ofDetails,
                (parser, context) -> context.readValue(parser, detailsType));
    }

    /**
     * Same as {@link #register(SimpleModule, Class, Function, Function, Function, Function, Class)}, but reads the
     * details as a generic type.
     *
     * @param module module to add to
     * @param type union type
     * @param scalar text of a union value, or {@code null} when it holds details
     * @param details details of a union value, or {@code null} when it holds a scalar
     * @param ofScalar builds the union from text
     * @param ofDetails builds the union from details
     * @param detailsType generic type to read the details as
     * @param <T> union type
     * @param <D> details type
     */
    public static <T, D> void register(
            final SimpleModule module,
            final Class<T> type,
            final Function<T, @Nullable String> scalar,
            final Function<T, @Nullable D> details,
            final Function<String, T> ofScalar,
            final Function<D, T> ofDetails,
            final TypeReference<D> detailsType) {
        register(
                module,
                type,
                scalar,
                details,
                ofScalar,
                ofDetails,
                (parser, context) -> context.readValue(parser, detailsType));
    }

    private static <T, D> void register(
            final SimpleModule module,
            final Class<T> type,
            final Function<T, @Nullable String> scalar,
            final Function<T, @Nullable D> details,
            final Function<String, T> ofScalar,
            final Function<D, T> ofDetails,
            final StringOrObjectDeserializer.DetailsReader<D> detailsReader) {
        requireNonNull(module, "module");
        requireNonNull(type, "type");
        requireNonNull(scalar, "scalar");
        requireNonNull(details, "details");
        requireNonNull(ofScalar, "ofScalar");
        requireNonNull(ofDetails, "ofDetails");
        requireNonNull(detailsReader, "detailsReader");
        module.addSerializer(type, new StringOrObjectSerializer<>(type, scalar, details));
        module.addDeserializer(type, new StringOrObjectDeserializer<>(type, ofScalar, ofDetails, detailsReader));
    }
}

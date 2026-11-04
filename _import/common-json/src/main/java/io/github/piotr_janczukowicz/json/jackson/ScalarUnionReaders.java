package io.github.piotr_janczukowicz.json.jackson;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;

/** Reads scalar unions (JSON string, number, optional boolean or null, string array) into a factory-built value. */
public final class ScalarUnionReaders {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    // Jackson limits the digits of a number but not its exponent, and a value such as 1e999999999 would make a later
    // toPlainString or toBigInteger allocate gigabytes.
    private static final int MAX_NUMBER_SCALE = 1000;

    private ScalarUnionReaders() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Like {@link #read}, but a JSON {@code null} fails too.
     *
     * @param parser parser on the value's first token
     * @param context deserialization context
     * @param factories how each JSON kind becomes a {@code T}
     * @param <T> result type
     * @return the built value, never {@code null}
     * @throws DatabindException if the token kind is not accepted, with {@link Factories#unknownTokenMessage()}
     */
    public static <T> T readRequired(
            final JsonParser parser, final DeserializationContext context, final Factories<T> factories) {
        final T value = read(parser, context, factories);
        if (value == null) {
            throw DatabindException.from(parser, factories.unknownTokenMessage());
        }
        return value;
    }

    /**
     * Builds a value from a JSON string, number, boolean, string array, or {@code null}, using the matching factory.
     *
     * @param parser parser on the value's first token
     * @param context deserialization context, used to read the string array
     * @param factories how each JSON kind becomes a {@code T}
     * @param <T> result type
     * @return the built value, or {@code null} for a JSON {@code null} when {@link Factories#acceptNull()}
     * @throws DatabindException if the token kind is not accepted, with {@link Factories#unknownTokenMessage()}, or a
     *     number has an exponent beyond 1000 decimal places
     */
    public static <T> @Nullable T read(
            final JsonParser parser, final DeserializationContext context, final Factories<T> factories) {
        final JsonToken token = parser.currentToken();
        return switch (token) {
            case VALUE_NULL -> readNull(parser, factories);
            case VALUE_STRING -> factories.text().apply(parser.getString());
            case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> readNumber(parser, factories);
            case VALUE_TRUE, VALUE_FALSE -> readFlag(parser, factories);
            case START_ARRAY -> factories.texts().apply(context.readValue(parser, STRING_LIST));
            case null, default -> throw DatabindException.from(parser, factories.unknownTokenMessage());
        };
    }

    private static <T> @Nullable T readNull(final JsonParser parser, final Factories<T> factories) {
        if (factories.acceptNull()) {
            return null;
        }
        throw DatabindException.from(parser, factories.unknownTokenMessage());
    }

    private static <T> T readNumber(final JsonParser parser, final Factories<T> factories) {
        final BigDecimal number = parser.getDecimalValue();
        if (!hasReasonableExponent(number)) {
            throw DatabindException.from(parser, "number exponent is out of range");
        }
        return factories.number().apply(number);
    }

    private static <T> T readFlag(final JsonParser parser, final Factories<T> factories) {
        final FlagMapper<T> flag = factories.flag();
        if (flag == null) {
            throw DatabindException.from(parser, factories.unknownTokenMessage());
        }
        return flag.apply(parser.getBooleanValue());
    }

    private static boolean hasReasonableExponent(final BigDecimal number) {
        final int scale = number.scale();
        return scale <= MAX_NUMBER_SCALE && scale >= -MAX_NUMBER_SCALE;
    }

    /**
     * One factory per accepted JSON kind.
     *
     * @param text builds from a JSON string
     * @param number builds from a JSON number, which is read exactly as a {@link BigDecimal}
     * @param flag builds from a JSON boolean, or {@code null} when booleans are not accepted
     * @param texts builds from a JSON array of strings
     * @param acceptNull whether a JSON {@code null} reads as {@code null} instead of failing
     * @param unknownTokenMessage message of the failure for a token kind that is not accepted
     * @param <T> result type
     */
    public record Factories<T>(
            Function<String, T> text,
            Function<BigDecimal, T> number,
            @Nullable FlagMapper<T> flag,
            Function<List<String>, T> texts,
            boolean acceptNull,
            String unknownTokenMessage) {

        // A missing factory or message fails here, not on the first decode.
        public Factories {
            requireNonNull(text, "text");
            requireNonNull(number, "number");
            requireNonNull(texts, "texts");
            requireNonNull(unknownTokenMessage, "unknownTokenMessage");
        }
    }

    /**
     * Builds a value from a JSON boolean.
     *
     * @param <T> result type
     */
    @FunctionalInterface
    public interface FlagMapper<T> {

        /**
         * Builds the value.
         *
         * @param value the JSON boolean
         * @return built value
         */
        T apply(boolean value);
    }
}

package io.github.piotr_janczukowicz.json.jackson;

import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;

/** Checks on the token a parser is positioned on. */
public final class JsonTokens {

    private JsonTokens() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Requires the parser's token to be {@code expected}.
     *
     * @param parser parser the token comes from, which locates the failure
     * @param actual token the parser is on, or {@code null} at the end of input
     * @param expected token the caller needs
     * @param message exception text
     * @throws DatabindException if {@code actual} is not {@code expected}
     */
    public static void requireToken(
            final JsonParser parser, final @Nullable JsonToken actual, final JsonToken expected, final String message) {
        if (actual != expected) {
            throw DatabindException.from(parser, message);
        }
    }

    /**
     * Binds the object the parser is on to {@code type}, unless it is empty, without buffering it.
     *
     * <p>To spot an empty object the parser is advanced, so a non-empty object is bound from its first property name,
     * not from {@code START_OBJECT}. That works for bean and map targets, but not for a target whose deserializer needs
     * the whole object, such as one that uses {@link BufferedJson}, {@link PolymorphicObjectAdapters}, or
     * {@link StringOrObjectAdapters}. For those, use {@link BufferedJson#isEmptyObject()} and then
     * {@link BufferedJson#read(Class)}.
     *
     * @param parser parser positioned on the value; it ends after the value
     * @param context context used for the read
     * @param type target class
     * @param <T> result type
     * @return bound value, or {@code null} when the value is an object with no properties
     */
    public static <T> @Nullable T readUnlessEmptyObject(
            final JsonParser parser, final DeserializationContext context, final Class<T> type) {
        if (parser.currentToken() == JsonToken.START_OBJECT && parser.nextToken() == JsonToken.END_OBJECT) {
            return null;
        }
        return context.readValue(parser, type);
    }
}

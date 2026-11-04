package io.github.piotr_janczukowicz.json.jackson;

import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.util.TokenBuffer;

/**
 * One JSON value copied as tokens so a deserializer can inspect a field and then bind a Java type without building a
 * {@link tools.jackson.databind.JsonNode} tree.
 */
public final class BufferedJson implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BufferedJson.class);

    private final TokenBuffer buffer;
    private final DeserializationContext context;

    private BufferedJson(final TokenBuffer buffer, final DeserializationContext context) {
        this.buffer = buffer;
        this.context = context;
    }

    /** Releases the buffered tokens. */
    @Override
    public void close() {
        buffer.close();
    }

    /**
     * Copies the value at the parser's current token, including all of its children, so it can be read several times.
     *
     * @param parser parser positioned on the first token of the value; it ends after the value
     * @param context context used for later reads
     * @return buffered value; the caller must close it
     */
    public static BufferedJson capture(final JsonParser parser, final DeserializationContext context) {
        final TokenBuffer buffer = TokenBuffer.forBuffering(parser, context);
        buffer.copyCurrentStructure(parser);
        return new BufferedJson(buffer, context);
    }

    /**
     * Whether the value is an object with no properties.
     *
     * @return {@code true} for an empty object; {@code false} for any other value
     */
    public boolean isEmptyObject() {
        try (JsonParser parser = replay()) {
            return parser.currentToken() == JsonToken.START_OBJECT && parser.nextToken() == JsonToken.END_OBJECT;
        }
    }

    /**
     * Whether the value is an object with a top-level property {@code field}, even when it is {@code null}.
     *
     * @param field property name
     * @return {@code true} if present
     */
    public boolean hasField(final String field) {
        try (JsonParser parser = replay()) {
            return seek(parser, field);
        }
    }

    /**
     * Whether the value is an object with a top-level property {@code field} that is not the JSON literal {@code null}.
     *
     * @param field property name
     * @return {@code true} if present and not {@code null}
     */
    public boolean hasNonNull(final String field) {
        try (JsonParser parser = replay()) {
            return seek(parser, field) && parser.currentToken() != JsonToken.VALUE_NULL;
        }
    }

    /**
     * Text of a top-level property.
     *
     * @param field property name
     * @return the value as text (numbers and booleans in their JSON form), or {@code null} when the property is missing
     *     or {@code null}
     */
    public @Nullable String text(final String field) {
        try (JsonParser parser = replay()) {
            return fieldText(parser, field);
        }
    }

    /**
     * Text of a top-level property that must be present.
     *
     * @param field property name
     * @return the value as text
     * @throws DatabindException if the property is missing or {@code null}, or is an object or an array
     */
    public String requiredText(final String field) {
        final String value = text(field);
        if (value != null) {
            return value;
        }
        if (hasNonNull(field)) {
            throw mismatch("Field " + field + " must be a string, a number or a boolean");
        }
        throw missing(field);
    }

    /**
     * Enum constant named by the text of a top-level property that must be present, for a discriminator such as
     * {@code type}. A value the enum does not know is reported as a {@link DatabindException}, like any other value of
     * the wrong shape, so decoding through a mapper fails with the usual decode exception.
     *
     * @param field property name
     * @param fromWire converts the wire text to the constant, throwing {@link IllegalArgumentException} for an unknown
     *     value
     * @param <E> enum type
     * @return the constant
     * @throws DatabindException if the property is missing or {@code null}, or {@code fromWire} rejects its value
     */
    public <E> E requiredWire(final String field, final Function<String, E> fromWire) {
        final String value = requiredText(field);
        try {
            return fromWire.apply(value);
        } catch (final IllegalArgumentException unknown) {
            LOG.atDebug()
                    .setMessage("Rejected the value of field {}: {}")
                    .addArgument(field)
                    .addArgument(unknown.getMessage())
                    .log();
            throw DatabindException.from(context, unknown.getMessage(), unknown);
        }
    }

    /**
     * Integer value of a top-level property that must be present.
     *
     * @param field property name
     * @return the value
     * @throws DatabindException if the property is missing or {@code null}
     */
    public long requiredLong(final String field) {
        try (JsonParser parser = replay()) {
            return fieldLong(parser, field);
        }
    }

    /**
     * Binds the whole buffered value.
     *
     * @param type target class
     * @param <T> result type
     * @return bound value
     */
    public <T> T read(final Class<T> type) {
        try (JsonParser parser = replay()) {
            return context.readValue(parser, type);
        }
    }

    /**
     * Binds a top-level property.
     *
     * @param field property name
     * @param type target class
     * @param <T> result type
     * @return bound value, or {@code null} when the property is missing or {@code null}
     */
    public <T> @Nullable T readField(final String field, final Class<T> type) {
        return readLocated(field, parser -> context.readValue(parser, type));
    }

    /**
     * Binds a top-level property to a type with type arguments.
     *
     * @param field property name
     * @param type target type
     * @param <T> result type
     * @return bound value, or {@code null} when the property is missing or {@code null}
     */
    public <T> @Nullable T readField(final String field, final JavaType type) {
        return readLocated(field, parser -> context.readValue(parser, type));
    }

    /**
     * Binds a top-level property that must be present.
     *
     * @param field property name
     * @param type target class
     * @param <T> result type
     * @return bound value
     * @throws DatabindException if the property is missing or {@code null}
     */
    public <T> T readRequiredField(final String field, final Class<T> type) {
        final T value = readField(field, type);
        if (value == null) {
            throw missing(field);
        }
        return value;
    }

    /**
     * Binds {@code type} after writing {@code value} for {@code field}, replacing a missing, null, or existing value.
     * The buffered value is not changed.
     *
     * @param field property name to set
     * @param value text to write
     * @param type target class
     * @param <T> result type
     * @return bound value
     * @throws DatabindException if the buffered value is not an object
     */
    public <T> T readReplacingText(final String field, final String value, final Class<T> type) {
        try (JsonParser parser = replay();
                TokenBuffer rewritten = TokenBuffer.forBuffering(parser, context)) {
            if (parser.currentToken() != JsonToken.START_OBJECT) {
                throw mismatch("Expected an object");
            }
            rewritten.writeStartObject();
            boolean wrote = false;
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                final String name = parser.currentName();
                parser.nextToken();
                if (field.equals(name)) {
                    rewritten.writeStringProperty(name, value);
                    parser.skipChildren();
                    wrote = true;
                } else {
                    rewritten.writeName(name);
                    rewritten.copyCurrentStructure(parser);
                }
            }
            if (!wrote) {
                rewritten.writeStringProperty(field, value);
            }
            rewritten.writeEndObject();
            try (JsonParser bound = rewritten.asParserOnFirstToken(context)) {
                return context.readValue(bound, type);
            }
        }
    }

    /**
     * A failure for a value that does not have the expected shape. It is a {@link DatabindException}, so decoding
     * through a mapper reports it like any other malformed input, also when this value is the root of the document.
     *
     * @param message what is wrong with the value
     * @return the exception to throw
     */
    public DatabindException mismatch(final String message) {
        return DatabindException.from(context, message);
    }

    /**
     * Position of the first of {@code fields} that is a top-level property with a value other than {@code null}, found
     * in a single pass. Only the first occurrence of each property counts, as in {@link #hasNonNull(String)}.
     *
     * @param fields property names in priority order
     * @return index into {@code fields}, or {@code -1} when the value is not an object or none qualifies
     */
    int firstNonNull(final List<String> fields) {
        try (JsonParser parser = replay()) {
            return indexOfFirstNonNull(parser, fields);
        }
    }

    private <T> @Nullable T readLocated(final String field, final Function<JsonParser, T> reader) {
        try (JsonParser parser = replay()) {
            return located(parser, field, reader);
        }
    }

    private static @Nullable String fieldText(final JsonParser parser, final String field) {
        if (!seek(parser, field) || parser.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        return parser.getValueAsString();
    }

    private long fieldLong(final JsonParser parser, final String field) {
        if (!seek(parser, field) || parser.currentToken() == JsonToken.VALUE_NULL) {
            throw missing(field);
        }
        if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
            throw mismatch("Field " + field + " must be an integer");
        }
        return parser.getLongValue();
    }

    private static <T> @Nullable T located(
            final JsonParser parser, final String field, final Function<JsonParser, T> reader) {
        if (!seek(parser, field) || parser.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        return reader.apply(parser);
    }

    private JsonParser replay() {
        return buffer.asParserOnFirstToken(context);
    }

    private static int indexOfFirstNonNull(final JsonParser parser, final List<String> fields) {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            return -1;
        }
        final boolean[] seen = new boolean[fields.size()];
        int best = -1;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            final int index = fields.indexOf(parser.currentName());
            final boolean fresh = index >= 0 && !seen[index];
            if (fresh) {
                seen[index] = true;
            }
            if (parser.nextToken() != JsonToken.VALUE_NULL && fresh && (best < 0 || index < best)) {
                best = index;
                if (best == 0) {
                    return best;
                }
            }
            parser.skipChildren();
        }
        return best;
    }

    private static boolean seek(final JsonParser parser, final String field) {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            return false;
        }
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            final String name = parser.currentName();
            parser.nextToken();
            if (field.equals(name)) {
                return true;
            }
            parser.skipChildren();
        }
        return false;
    }

    private DatabindException missing(final String field) {
        return mismatch("Missing required field " + field);
    }
}

package io.github.piotr_janczukowicz.json.jackson.module.valueunion;

import io.github.piotr_janczukowicz.json.EpochUnit;
import io.github.piotr_janczukowicz.json.jackson.FastInstantDeserializer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Reads a property marked {@link io.github.piotr_janczukowicz.json.EpochInstant}: an integer count of the unit, a
 * number with a zero fraction, or ISO-8601 text; JSON {@code null} is {@code null}.
 */
final class EpochInstantDeserializer extends ValueDeserializer<Instant> {

    private static final int MAX_DIGITS = 30;
    private static final String RANGE = " is outside the range of an Instant";

    private final EpochUnit unit;
    private final FastInstantDeserializer text = new FastInstantDeserializer();

    EpochInstantDeserializer(final EpochUnit unit) {
        super();
        this.unit = unit;
    }

    @Override
    public Instant deserialize(final JsonParser parser, final DeserializationContext context) {
        final JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_NUMBER_INT) {
            return toInstant(parser.getBigIntegerValue(), parser);
        }
        if (token == JsonToken.VALUE_NUMBER_FLOAT) {
            return toInstant(wholeNumber(parser.getDecimalValue(), parser), parser);
        }
        if (token == JsonToken.VALUE_STRING) {
            return text.deserialize(parser, context);
        }
        throw refuse(parser, "cannot be read from " + token);
    }

    @Override
    public @Nullable Object getNullValue(final DeserializationContext context) {
        return null;
    }

    private BigInteger wholeNumber(final BigDecimal number, final JsonParser parser) {
        if (number.precision() - number.scale() > MAX_DIGITS) {
            throw refuse(parser, number + RANGE);
        }
        try {
            return number.toBigIntegerExact();
        } catch (ArithmeticException e) {
            throw MismatchedInputException.from(
                            parser, Instant.class, "Epoch " + unit + " " + number + " has a fraction")
                    .withCause(e);
        }
    }

    private Instant toInstant(final BigInteger count, final JsonParser parser) {
        try {
            final long value = count.longValueExact();
            return unit == EpochUnit.SECONDS ? Instant.ofEpochSecond(value) : Instant.ofEpochMilli(value);
        } catch (ArithmeticException | DateTimeException e) {
            throw MismatchedInputException.from(parser, Instant.class, "Epoch " + unit + " " + count + RANGE)
                    .withCause(e);
        }
    }

    private MismatchedInputException refuse(final JsonParser parser, final String reason) {
        return MismatchedInputException.from(parser, Instant.class, "Epoch " + unit + " " + reason);
    }
}

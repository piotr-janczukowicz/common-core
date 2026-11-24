package io.github.piotr_janczukowicz.json.jackson.module.valueunion;

import io.github.piotr_janczukowicz.json.ValueUnion;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueInstantiator;
import tools.jackson.databind.deser.std.StdDeserializer;

/**
 * Reads a {@link ValueUnion}: picks the branch class from the value's first token and lets that class read the value.
 *
 * <p>Two branch kinds are read here instead of by Jackson, because the mapper's coercions would change them: a branch
 * created from a {@link BigDecimal} gets the number exactly, in plain notation ({@code 1e10} is {@code 10000000000}),
 * and a branch created from a collection of strings takes JSON strings only (the mapper would read {@code [1]} as
 * {@code ["1"]}).
 */
final class ValueUnionDeserializer extends StdDeserializer<Object> {

    // Jackson limits the digits of a number but not its exponent, and a value such as 1e999999999 would make a later
    // toPlainString or toBigInteger allocate gigabytes.
    private static final int MAX_NUMBER_SCALE = 1000;

    private final transient ValueUnion union;
    private final String unknownTokenMessage;
    private final String itemMessage;

    ValueUnionDeserializer(final Class<?> type, final ValueUnion union) {
        super(type);
        this.union = union;
        this.unknownTokenMessage = type.getSimpleName() + " takes " + takenTokens(union);
        this.itemMessage = type.getSimpleName() + " list items are strings";
    }

    @Override
    public Object deserialize(final JsonParser parser, final DeserializationContext context) {
        final Class<?> branch = branchOf(parser);
        if (branch == Void.class) {
            throw DatabindException.from(parser, unknownTokenMessage);
        }
        final ValueDeserializer<Object> deserializer = context.findRootValueDeserializer(context.constructType(branch));
        if (deserializer instanceof ValueInstantiator.Gettable gettable) {
            return readDelegating(parser, context, deserializer, gettable.getValueInstantiator());
        }
        return deserializer.deserialize(parser, context);
    }

    private Class<?> branchOf(final JsonParser parser) {
        final Class<?> branch =
                switch (parser.currentToken()) {
                    case VALUE_STRING -> union.string();
                    case VALUE_NUMBER_INT -> integerBranch(parser);
                    case VALUE_NUMBER_FLOAT -> union.number();
                    case VALUE_TRUE, VALUE_FALSE -> union.flag();
                    case START_ARRAY -> union.array();
                    case START_OBJECT -> union.object();
                    case null, default -> Void.class;
                };
        return branch == Void.class ? union.unknown() : branch;
    }

    @SuppressWarnings("PMD.LawOfDemeter")
    private Class<?> integerBranch(final JsonParser parser) {
        final boolean fitsLong = parser.getNumberType() != JsonParser.NumberType.BIG_INTEGER;
        return fitsLong && union.integer() != Void.class ? union.integer() : union.number();
    }

    private Object readDelegating(
            final JsonParser parser,
            final DeserializationContext context,
            final ValueDeserializer<Object> deserializer,
            final ValueInstantiator instantiator) {
        final DeserializationConfig config = context.getConfig();
        final JsonToken token = parser.currentToken();
        if (token.isNumeric()
                && instantiator.canCreateUsingDelegate()
                && instantiator.getDelegateType(config).hasRawClass(BigDecimal.class)) {
            return instantiator.createUsingDelegate(context, plainDecimal(parser));
        }
        if (token == JsonToken.START_ARRAY
                && instantiator.canCreateUsingArrayDelegate()
                && isStringCollection(instantiator.getArrayDelegateType(config))) {
            return instantiator.createUsingArrayDelegate(context, readStrings(parser));
        }
        return deserializer.deserialize(parser, context);
    }

    @SuppressWarnings("PMD.LawOfDemeter")
    private static BigDecimal plainDecimal(final JsonParser parser) {
        final BigDecimal number = parser.getDecimalValue();
        final int scale = number.scale();
        if (scale > MAX_NUMBER_SCALE || scale < -MAX_NUMBER_SCALE) {
            throw DatabindException.from(parser, "number exponent is out of range");
        }
        return new BigDecimal(number.toPlainString());
    }

    private static boolean isStringCollection(final JavaType delegate) {
        return delegate.isCollectionLikeType() && delegate.getContentType().hasRawClass(String.class);
    }

    private List<String> readStrings(final JsonParser parser) {
        final List<String> items = new ArrayList<>();
        JsonToken token = parser.nextToken();
        while (token != JsonToken.END_ARRAY) {
            if (token != JsonToken.VALUE_STRING) {
                throw DatabindException.from(parser, itemMessage);
            }
            items.add(parser.getString());
            token = parser.nextToken();
        }
        return items;
    }

    private static String takenTokens(final ValueUnion union) {
        final List<String> words = new ArrayList<>();
        addIfGiven(words, union.string(), "a string");
        addIfGiven(words, union.integer(), "an integer");
        addIfGiven(words, union.number(), "a number");
        addIfGiven(words, union.flag(), "a boolean");
        addIfGiven(words, union.array(), "an array");
        addIfGiven(words, union.object(), "an object");
        if (words.isEmpty()) {
            return "no value";
        }
        final String last = words.removeLast();
        return words.isEmpty() ? last : String.join(", ", words) + " or " + last;
    }

    private static void addIfGiven(final List<String> words, final Class<?> branch, final String word) {
        if (branch != Void.class) {
            words.add(word);
        }
    }
}

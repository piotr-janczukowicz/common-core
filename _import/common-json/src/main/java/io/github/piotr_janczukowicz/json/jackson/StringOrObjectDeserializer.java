package io.github.piotr_janczukowicz.json.jackson;

import java.util.function.Function;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

/** Reads a JSON string as the scalar of a string-or-object union and anything else as its details. */
final class StringOrObjectDeserializer<T, D> extends StdDeserializer<T> {

    private final Function<String, T> ofScalar;
    private final Function<D, T> ofDetails;
    private final DetailsReader<D> detailsReader;

    StringOrObjectDeserializer(
            final Class<T> type,
            final Function<String, T> ofScalar,
            final Function<D, T> ofDetails,
            final DetailsReader<D> detailsReader) {
        super(type);
        this.ofScalar = ofScalar;
        this.ofDetails = ofDetails;
        this.detailsReader = detailsReader;
    }

    @Override
    public T deserialize(final JsonParser parser, final DeserializationContext context) {
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
            return ofScalar.apply(parser.getString());
        }
        return ofDetails.apply(detailsReader.read(parser, context));
    }

    /** Reads the details object of a union value. */
    @FunctionalInterface
    interface DetailsReader<D> {
        D read(JsonParser parser, DeserializationContext context);
    }
}

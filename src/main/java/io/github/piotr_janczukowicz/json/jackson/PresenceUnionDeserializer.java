package io.github.piotr_janczukowicz.json.jackson;

import io.github.piotr_janczukowicz.json.PresenceUnion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

/**
 * Reads a {@link PresenceUnion}: buffers the object, picks the branch by the first of its names that is a key (document
 * order), else the fallback when the object has every name the fallback requires, else the Unknown, and lets that class
 * read the object.
 */
final class PresenceUnionDeserializer extends StdDeserializer<Object> {

    private final Map<String, Class<?>> branches;
    private final Class<?> fallback;
    private final List<String> fallbackRequires;
    private final Class<?> unknown;
    private final String objectMessage;
    private final String noBranchMessage;

    PresenceUnionDeserializer(final Class<?> type, final PresenceUnion union) {
        super(type);
        this.branches = keys(type, union);
        this.fallback = union.fallback();
        this.fallbackRequires = List.of(union.fallbackRequires());
        this.unknown = union.unknown();
        this.objectMessage = type.getSimpleName() + " takes an object";
        this.noBranchMessage = type.getSimpleName() + " matches no branch: the object has none of its keys"
                + (fallback == Void.class ? "" : " and not every property of " + fallback.getSimpleName());
    }

    @Override
    public Object deserialize(final JsonParser parser, final DeserializationContext context) {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            throw DatabindException.from(parser, objectMessage);
        }
        try (BufferedJson json = BufferedJson.capture(parser, context)) {
            final Class<?> branch = branchOf(json.names());
            if (branch == Void.class) {
                throw json.mismatch(noBranchMessage);
            }
            return json.read(branch);
        }
    }

    private Class<?> branchOf(final Set<String> names) {
        for (final String name : names) {
            final Class<?> branch = branches.get(name);
            if (branch != null) {
                return branch;
            }
        }
        if (fallback != Void.class && names.containsAll(fallbackRequires)) {
            return fallback;
        }
        return unknown;
    }

    private static Map<String, Class<?>> keys(final Class<?> type, final PresenceUnion union) {
        final Map<String, Class<?>> found = new HashMap<>();
        for (final PresenceUnion.Key key : union.keys()) {
            if (found.put(key.name(), key.type()) != null) {
                throw new IllegalArgumentException(type.getName() + " names the key " + key.name() + " twice");
            }
        }
        return found;
    }
}

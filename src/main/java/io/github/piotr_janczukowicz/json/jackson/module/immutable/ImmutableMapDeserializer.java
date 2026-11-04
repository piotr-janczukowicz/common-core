package io.github.piotr_janczukowicz.json.jackson.module.immutable;

import java.util.Map;
import java.util.Set;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Wraps Jackson's own map deserializer so element types are preserved, then makes the result an unmodifiable map.
 *
 * <p>Entries with a null value are kept. JSON {@code null} and absent properties are left to Jackson, so they stay
 * {@code null}; an empty-value coercion becomes an empty immutable map.
 */
final class ImmutableMapDeserializer extends ImmutableCollectionDeserializer {

    private static final Set<Class<?>> REQUESTED_TYPES = Set.of(Map.class);

    ImmutableMapDeserializer(final ValueDeserializer<?> delegatee) {
        super(delegatee, REQUESTED_TYPES);
    }

    @Override
    public Object deserialize(final JsonParser parser, final DeserializationContext context) {
        final Map<?, ?> source = (Map<?, ?>) _delegatee.deserialize(parser, context);
        return ImmutableCopies.map(source);
    }

    @Override
    public Object getEmptyValue(final DeserializationContext context) {
        return Map.of();
    }

    @Override
    protected ValueDeserializer<?> newDelegatingInstance(final ValueDeserializer<?> newDelegatee) {
        return new ImmutableMapDeserializer(newDelegatee);
    }
}

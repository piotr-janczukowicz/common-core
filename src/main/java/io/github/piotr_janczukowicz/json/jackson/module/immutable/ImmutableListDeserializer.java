package io.github.piotr_janczukowicz.json.jackson.module.immutable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Wraps Jackson's own list deserializer so element types are preserved, then makes the result an unmodifiable list.
 *
 * <p>Null elements are kept. JSON {@code null} and absent properties are left to Jackson, so they stay {@code null}; an
 * empty-value coercion becomes an empty immutable list.
 */
final class ImmutableListDeserializer extends ImmutableCollectionDeserializer {

    private static final Set<Class<?>> REQUESTED_TYPES = Set.of(List.class, Collection.class);

    ImmutableListDeserializer(final ValueDeserializer<?> delegatee) {
        super(delegatee, REQUESTED_TYPES);
    }

    @Override
    public Object deserialize(final JsonParser parser, final DeserializationContext context) {
        final Collection<?> source = (Collection<?>) _delegatee.deserialize(parser, context);
        return ImmutableCopies.list(source);
    }

    @Override
    public Object getEmptyValue(final DeserializationContext context) {
        return List.of();
    }

    @Override
    protected ValueDeserializer<?> newDelegatingInstance(final ValueDeserializer<?> newDelegatee) {
        return new ImmutableListDeserializer(newDelegatee);
    }
}

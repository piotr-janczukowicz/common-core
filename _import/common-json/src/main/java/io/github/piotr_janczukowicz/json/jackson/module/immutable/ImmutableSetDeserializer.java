package io.github.piotr_janczukowicz.json.jackson.module.immutable;

import java.util.Collection;
import java.util.Set;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Wraps Jackson's own set deserializer so element types are preserved, then makes the result an unmodifiable set.
 *
 * <p>Null elements are kept. JSON {@code null} and absent properties are left to Jackson, so they stay {@code null}; an
 * empty-value coercion becomes an empty immutable set.
 */
final class ImmutableSetDeserializer extends ImmutableCollectionDeserializer {

    private static final Set<Class<?>> REQUESTED_TYPES = Set.of(Set.class);

    ImmutableSetDeserializer(final ValueDeserializer<?> delegatee) {
        super(delegatee, REQUESTED_TYPES);
    }

    @Override
    public Object deserialize(final JsonParser parser, final DeserializationContext context) {
        final Collection<?> source = (Collection<?>) _delegatee.deserialize(parser, context);
        return ImmutableCopies.set(source);
    }

    @Override
    public Object getEmptyValue(final DeserializationContext context) {
        return Set.of();
    }

    @Override
    protected ValueDeserializer<?> newDelegatingInstance(final ValueDeserializer<?> newDelegatee) {
        return new ImmutableSetDeserializer(newDelegatee);
    }
}

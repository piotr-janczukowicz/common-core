package io.github.piotr_janczukowicz.json.jackson.module.immutable;

import java.util.Set;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.DelegatingDeserializer;

/**
 * Base for deserializers that make a Jackson-built collection immutable, but only when the requested type is one of the
 * given interface types.
 *
 * <p>Jackson maps an interface such as {@code List} to a default implementation such as {@code ArrayList} before it
 * calls a deserializer modifier, so the declared type is only known once the deserializer is contextualized. When a
 * caller declared a concrete type such as {@code ArrayList}, the wrapped Jackson deserializer is used unchanged.
 */
abstract class ImmutableCollectionDeserializer extends DelegatingDeserializer {

    private final Set<Class<?>> requestedTypes;

    ImmutableCollectionDeserializer(final ValueDeserializer<?> delegatee, final Set<Class<?>> requestedTypes) {
        super(delegatee);
        this.requestedTypes = requestedTypes;
    }

    /**
     * Keeps this wrapper when the declared type is one of the requested interface types, otherwise unwraps to the
     * contextualized Jackson deserializer.
     *
     * @param context deserialization context holding the declared type
     * @param property property being deserialized, or {@code null} for a root value
     * @return this wrapper (contextualized) or the plain Jackson deserializer
     */
    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public ValueDeserializer<?> createContextual(
            final DeserializationContext context, final @Nullable BeanProperty property) {
        final DelegatingDeserializer contextual = (DelegatingDeserializer) super.createContextual(context, property);
        if (requestedTypes.contains(context.getContextualType().getRawClass())) {
            return contextual;
        }
        return contextual.getDelegatee();
    }
}

package io.github.piotr_janczukowicz.json.jackson.module.immutable;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.type.CollectionType;
import tools.jackson.databind.type.MapType;

/**
 * Wraps the default deserializers for {@link List}, {@link Collection}, {@link Set}, and {@link Map} so they return
 * immutable collections.
 *
 * <p>Jackson replaces these interfaces with default implementations ({@code ArrayList}, {@code HashSet},
 * {@code EnumSet}, {@code LinkedHashMap}) before calling this modifier, so those are the types wrapped here; the
 * wrapper itself decides from the declared type whether to stay in place. This type does not hold collection data.
 */
final class ImmutableCollectionsModifier extends ValueDeserializerModifier {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public ValueDeserializer<?> modifyCollectionDeserializer(
            final DeserializationConfig config,
            final CollectionType type,
            final BeanDescription.Supplier beanDescription,
            final ValueDeserializer<?> deserializer) {
        final Class<?> rawClass = type.getRawClass();
        if (rawClass == ArrayList.class) {
            return new ImmutableListDeserializer(deserializer);
        }
        if (rawClass == HashSet.class || rawClass == EnumSet.class) {
            return new ImmutableSetDeserializer(deserializer);
        }
        return deserializer;
    }

    @Override
    public ValueDeserializer<?> modifyMapDeserializer(
            final DeserializationConfig config,
            final MapType type,
            final BeanDescription.Supplier beanDescription,
            final ValueDeserializer<?> deserializer) {
        if (type.getRawClass() == LinkedHashMap.class) {
            return new ImmutableMapDeserializer(deserializer);
        }
        return deserializer;
    }
}

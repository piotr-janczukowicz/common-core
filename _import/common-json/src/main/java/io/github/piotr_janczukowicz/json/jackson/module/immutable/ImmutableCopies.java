package io.github.piotr_janczukowicz.json.jackson.module.immutable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unmodifiable views of Jackson-built collections that keep null elements and null map values.
 *
 * <p>The sources are collections Jackson has just built and nobody else holds, so they are wrapped without a copy.
 * Lists and maps keep the order of the JSON; the order of a set is unspecified.
 */
final class ImmutableCopies {

    private ImmutableCopies() {
        throw new UnsupportedOperationException("Utility class");
    }

    static <E> List<E> list(final Collection<? extends E> source) {
        if (source.isEmpty()) {
            return List.of();
        }
        if (source instanceof List<?>) {
            @SuppressWarnings("unchecked")
            final List<E> list = (List<E>) source;
            return Collections.unmodifiableList(list);
        }
        return Collections.unmodifiableList(new ArrayList<>(source));
    }

    static <E> Set<E> set(final Collection<? extends E> source) {
        if (source.isEmpty()) {
            return Set.of();
        }
        if (source instanceof Set<?>) {
            @SuppressWarnings("unchecked")
            final Set<E> set = (Set<E>) source;
            return Collections.unmodifiableSet(set);
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }

    static <K, V> Map<K, V> map(final Map<? extends K, ? extends V> source) {
        if (source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(source);
    }
}

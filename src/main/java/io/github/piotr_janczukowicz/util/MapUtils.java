package io.github.piotr_janczukowicz.util;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Map helpers: immutable copies of a map, which may be required or absent. */
public final class MapUtils {

    private MapUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Copies a required map into an unmodifiable map.
     *
     * @param values the map to copy
     * @param name name of {@code values}, used in the exception message
     * @param <K> key type
     * @param <V> value type
     * @return an unmodifiable copy of {@code values}
     * @throws NullPointerException if {@code values} is {@code null}, or has a {@code null} key or value
     */
    public static <K, V> Map<K, V> copyOf(final Map<K, V> values, final String name) {
        return Map.copyOf(requireNonNull(values, name));
    }

    /**
     * Copies {@code values} into an unmodifiable map, and keeps an absent map absent.
     *
     * @param values the map to copy, or {@code null}
     * @param <K> key type
     * @param <V> value type
     * @return an unmodifiable copy of {@code values}, or {@code null} if {@code values} is {@code null}
     * @throws NullPointerException if {@code values} has a {@code null} key or value
     */
    @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
    public static <K, V> @Nullable Map<K, V> copyOrNull(@Nullable final Map<K, V> values) {
        if (values == null) {
            return null;
        }
        return Map.copyOf(values);
    }

    /**
     * Copies {@code values} into an unmodifiable map, and turns an absent map into an empty one.
     *
     * @param values the map to copy, or {@code null}
     * @param <K> key type
     * @param <V> value type
     * @return an unmodifiable copy of {@code values}, or an empty map if {@code values} is {@code null}
     * @throws NullPointerException if {@code values} has a {@code null} key or value
     */
    public static <K, V> Map<K, V> copyOrEmpty(@Nullable final Map<K, V> values) {
        if (values == null) {
            return Map.of();
        }
        return Map.copyOf(values);
    }

    /**
     * Turns an absent map into an empty one, without copying a present one.
     *
     * @param values the map, or {@code null}
     * @param <K> key type
     * @param <V> value type
     * @return {@code values} itself, or an empty map if {@code values} is {@code null}
     */
    public static <K, V> Map<K, V> orEmpty(@Nullable final Map<K, V> values) {
        if (values == null) {
            return Map.of();
        }
        return values;
    }
}

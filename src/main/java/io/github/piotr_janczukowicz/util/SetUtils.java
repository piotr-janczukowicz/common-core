package io.github.piotr_janczukowicz.util;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Set helpers: immutable copies of a set, which may be required or absent. */
public final class SetUtils {

    private SetUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Copies a required set into an unmodifiable set.
     *
     * @param values the set to copy
     * @param name name of {@code values}, used in the exception message
     * @param <T> element type
     * @return an unmodifiable copy of {@code values}
     * @throws NullPointerException if {@code values} is {@code null}, or has a {@code null} element
     */
    public static <T> Set<T> copyOf(final Set<T> values, final String name) {
        return Set.copyOf(requireNonNull(values, name));
    }

    /**
     * Copies {@code values} into an unmodifiable set, and keeps an absent set absent.
     *
     * @param values the set to copy, or {@code null}
     * @param <T> element type
     * @return an unmodifiable copy of {@code values}, or {@code null} if {@code values} is {@code null}
     * @throws NullPointerException if {@code values} has a {@code null} element
     */
    @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
    public static <T> @Nullable Set<T> copyOrNull(@Nullable final Set<T> values) {
        if (values == null) {
            return null;
        }
        return Set.copyOf(values);
    }

    /**
     * Copies {@code values} into an unmodifiable set, and turns an absent set into an empty one.
     *
     * @param values the set to copy, or {@code null}
     * @param <T> element type
     * @return an unmodifiable copy of {@code values}, or an empty set if {@code values} is {@code null}
     * @throws NullPointerException if {@code values} has a {@code null} element
     */
    public static <T> Set<T> copyOrEmpty(@Nullable final Set<T> values) {
        if (values == null) {
            return Set.of();
        }
        return Set.copyOf(values);
    }

    /**
     * Turns an absent set into an empty one, without copying a present one.
     *
     * @param values the set, or {@code null}
     * @param <T> element type
     * @return {@code values} itself, or an empty set if {@code values} is {@code null}
     */
    public static <T> Set<T> orEmpty(@Nullable final Set<T> values) {
        if (values == null) {
            return Set.of();
        }
        return values;
    }
}
